package gregtech6.tileentity.energy;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.registries.RegistryObject;

import gregapi.code.TagData;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GTGearBoxBlock;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Custom GearBox — task p12-gearbox-transformer spec 1,
 * ported from gregtech/tileentity/energy/transformers/MultiTileEntityGearBox.java
 * (:58-441) as the RU split/direction block of the fluid-engine chain (the total ADR
 * 2026-09-02-p12-fluid-engine-chain ⑥). With the axle family (main 93b5913) this closes
 * the RU transmission face: source → axle → GEARBOX → machines.
 *
 * <p>The connection mask (upstream {@code mAxleGear} :61, the :142-153 monkey-wrench
 * structure built verbatim, the TOOL interactions themselves are the p12-gear-items pool
 * card): bits 0-5 = a gear mounted on that face, bits 6-7 = a through-axle on the axis
 * (1 = X, 2 = Y, 3 = Z — the CS {@code AXIS_XYZ} indexing, CS.java:756-761, the same
 * encoding the monkey-wrench rows :145-147 write). Persistence rides the upstream
 * {@code gt.connection} byte (CS.java:1194, the readFromNBT2 :68 unsignB chain).
 *
 * <p>{@link #checkGears} (upstream :271-310 verbatim): 0/1 gears always work, a corner
 * pair always works, an opposite-pair pair works only on its own through-axle, 3-4 gears
 * refuse the D/Omikron shape (both gears of the axle axis) and the triangle interlock
 * (all three axes used), 5-6 gears never work.
 *
 * <p>Input (upstream {@code doInject} :347-410, reached through the Root
 * doEnergyInjection gate → the {@code doInject} hook exactly like upstream :717):
 * <ul>
 * <li>overspeed ({@code |aSpeed| > mMaxThroughPut}, the row rating = VMAX[tier],
 *     :357-370) EXPLODES the gears: scrap 9+rng(27) of the material + tCount-1 gears
 *     drop, the mask zeroes, the block SURVIVES ({@code mGearsWork = checkGears()} :368
 *     with 0 gears = true). Declared port deviation: the gear ITEM does not exist yet
 *     (the p12-gear-items pool card), so the gear leg drops the block item itself; the
 *     scrap leg rides the runtime material-item lookup and skips silently when the
 *     (prefix, material) pair is not registered. The {@code mTimer < 10} grace (:358)
 *     and the SFX (audio pool) are kept out of the drop path.</li>
 * <li>a free through-axle is ALWAYS a passthrough (:372-375): the packet crosses to the
 *     opposite face untouched (speed sign included) — the axle-family recursion form.</li>
 * <li>multi-input per tick takes the LOWEST speed and ADDS the power (:393-394).</li>
 * <li>a second input whose gear physics disagree with the first one's JAMS the box
 *     (:381-389): zero output, zero explosion, the jam gate closes the faces
 *     ({@code isEnergyAcceptingFrom} :413) until the masks change.</li>
 * </ul>
 *
 * <p>Output (upstream {@code onTick2} :188-216): while power remains, round-robin from
 * {@code mOrder}, each face gets at most {@code max(1, power/3)} packets per pass (the
 * geometric comment is upstream :192 — three output sides max). The emitted SPEED SIGN is
 * the gear physics of {@link #getRotations} (:227-268): bit set = +speed (clockwise),
 * bit clear = -speed — the RU negative-sign IS the counterclockwise direction semantic
 * (TD.java:74-84), consumed verbatim here; adjacent gears counter-rotate, the
 * through-axle faces co-rotate. The {@code mIgnorePower} ladder (:401/:403) refuses fresh
 * input while last tick's power is still queued, so a backed-up box does not waste power.
 *
 * <p>Cropped with declaration: the paint half of TileEntityBase07Paintable (no RGBa in
 * this port), the tool-click family (:104-179 — wrench/monkeywrench/softhammer are the
 * pool card's interaction surface, the tachometer readout is pooled to
 * {@code /gt6engine stat}, the config channel is the {@code /gt6engine gearbox} direct
 * mask write — the P9 "acceptance channel is not the upstream player semantics" ruling),
 * the render family (:328-344 — one shared static texture, the rotation overlay is the
 * render pool), {@code IMTE_GetOreDictItemData} (:437-440), and the client :223 sound.
 */
public class GTGearBoxBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.java:1241/:1194/:1314). */
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_CONNECTION = "gt.connection";
	public static final String NBT_INPUT = "gt.input";

	/** The upstream field defaults (:59-62), mMaxThroughPut overridden by the wood row = VMAX[0]. */
	public static final long DEFAULT_MAX_THROUGHPUT = 64;

	/** The speed rating (upstream mMaxThroughPut :60, the row's NBT_INPUT = VMAX[0] = 16, Loader :1669). */
	public long mMaxThroughPut = GT6Kinetics.GEARBOX_MAX_THROUGHPUT;

	/** The interlock state (upstream :59): jammed / a gear was used this tick / the gears mesh. */
	public boolean mJammed = false, mUsedGear = false, mGearsWork = true;

	/** The live accumulators (upstream :60): the accepted tick packet and the tachometer readout. */
	public long mCurrentSpeed = 0, mCurrentPower = 0, mTransferredLast = 0;

	/**
	 * The connection mask (upstream mAxleGear :61): bits 0-5 gears + bits 6-7 the axle
	 * axis (1 = X, 2 = Y, 3 = Z). An int carries the upstream unsigned-short form.
	 */
	public int mAxleGear = 0;

	/** The per-tick bookkeeping bytes (upstream :62). */
	public byte mInputtedSides = 0, oInputtedSides = 0, mOrder = 0, mRotationData = 0, oRotationData = 0, mIgnorePower = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTGearBoxBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the shared registry type at runtime, tests pass an offline-built BET.
	 */
	public GTGearBoxBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.GEARBOX_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gearbox"; // BET registry path mirrors it (GTBlockEntities.GEARBOX_BE)
	}

	// ---------------------------------------------------------------------------
	// the side algebra (the CS lookup tables this port needs, CS.java:598-761)
	// ---------------------------------------------------------------------------

	/** CS FACE_CONNECTED[aSide][aMask] (:598) — the plain bit test. */
	public static boolean faceConnected(byte aSide, int aMask) {
		return aSide >= 0 && aSide < 6 && (aMask & (1 << aSide)) != 0;
	}

	/** CS FACE_CONNECTION_COUNT (:609). */
	public static int faceConnectionCount(int aMask) {
		return Integer.bitCount(aMask & 63);
	}

	/**
	 * CS AXIS_XYZ (CS.java:756-761) over the gearbox axle encoding (1 = X, 2 = Y, 3 = Z —
	 * SIDES_AXIS_X {4,5} / SIDES_AXIS_Y {0,1} / SIDES_AXIS_Z {2,3}, CS.java:704-706).
	 */
	public static boolean axisXYZ(int aAxis, byte aSide) {
		return switch (aAxis) {
			case 1 -> aSide >= 4;
			case 2 -> aSide <= 1;
			case 3 -> aSide == 2 || aSide == 3;
			default -> false;
		};
	}

	/** CS ALL_SIDES_VALID_BUT_AXIS[aSide] (CS.java:676) as a six-bit mask. */
	public static int sidesValidButAxis(byte aSide) {
		return switch (aSide) {
			case 0, 1 -> 0b111100;
			case 2, 3 -> 0b110011;
			default -> 0b001111;
		};
	}

	/** The axle axis field of the mask (bits 6-7). */
	public int axleAxis() {
		return (mAxleGear >> 6) & 3;
	}

	/** The GT6 side order opposite (CS OPOS, CS.java:620; byte side == Direction.get3DDataValue). */
	public static byte opposite(byte aSide) {
		return (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	// ---------------------------------------------------------------------------
	// the topology check (upstream checkGears :271-310 verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * Only ever called whenever the Axle or Gears change or when the Gearbox TileEntity is
	 * loaded (upstream :270).
	 */
	public boolean checkGears() {
		// Just in case something broke during setting up the Gearbox. (upstream :272-275)
		mIgnorePower = 0;
		mCurrentSpeed = mCurrentPower = 0;
		// Check if the Gearbox actually works properly. (upstream :276)
		switch (faceConnectionCount(mAxleGear)) {
			case 0:
				// Just prevents the Error Tooltip from popping up. (upstream :278-280)
				return true;
			case 1:
				// Always Rotates when connected, nothing stops it from doing so. (upstream :281-283)
				return true;
			case 2:
				// Corner Gears will always work. (upstream :284-286)
				if ((mAxleGear & 48) != 48 && (mAxleGear & 3) != 3 && (mAxleGear & 12) != 12) return true;
				// But also Rotate when both Gears are on the same Axle. (upstream :287-293)
				switch (axleAxis()) {
					case 1: return (mAxleGear & 48) != 0;
					case 2: return (mAxleGear & 3) != 0;
					case 3: return (mAxleGear & 12) != 0;
				}
				return false;
			case 3: case 4:
				// Make sure the Axle wont screw the Setup over by forming a D or Omikron Shape. (upstream :294-300)
				switch (axleAxis()) {
					case 1: if ((mAxleGear & 48) == 48) return false; break;
					case 2: if ((mAxleGear & 3) == 3) return false; break;
					case 3: if ((mAxleGear & 12) == 12) return false; break;
				}
				// Triangle interlocked Gears do ofcourse not work! (upstream :301-306)
				int tAxisUsed = 0;
				if ((mAxleGear & 48) != 0) tAxisUsed++;
				if ((mAxleGear & 3) != 0) tAxisUsed++;
				if ((mAxleGear & 12) != 0) tAxisUsed++;
				return tAxisUsed < 3;
		}
		// 5 Gears never work, same for 6 Gears (upstream :308-309)
		return false;
	}

	// ---------------------------------------------------------------------------
	// the gear direction physics (upstream getRotations :227-268 verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * The rotation bitmask for an input arriving on {@code aSide} with sign
	 * {@code aNegative} (counterclockwise): bit set = +speed (clockwise), bit clear =
	 * -speed, bit 6 = running. Adjacent gears counter-rotate, the through-axle faces
	 * co-rotate — the negative-sign RU semantics the consumers read verbatim.
	 */
	public byte getRotations(byte aSide, boolean aNegative) {
		// Nothing is interlocked properly so no functionality here. (upstream :228-229)
		if (!mGearsWork) return 0;
		// The Gear on the Input Side needs the correct direction set. (upstream :230-231)
		int rRotationData = aNegative ? (1 << aSide) : 0;
		// There is an Axle along this Axis. (upstream :232-254)
		if (axisXYZ(axleAxis(), aSide)) {
			// Make whatever is on the other side of the Axle rotate the same direction the Axle does.
			if (!aNegative) rRotationData |= (1 << opposite(aSide));
			// Gear on Input Side.
			if (faceConnected(aSide, mAxleGear)) {
				// All adjacent Gears need to rotate the opposite direction of this Gear.
				if (!aNegative) for (byte tSide = 0; tSide < 6; tSide++)
					if ((sidesValidButAxis(aSide) & (1 << tSide)) != 0 && faceConnected(tSide, mAxleGear)) rRotationData |= (1 << tSide);
				// Clear unused Values to make sure that it can be compared properly.
				return (byte) ((rRotationData & mAxleGear & 63) | 64);
			}
			// Gear on Throughput Side.
			if (faceConnected(opposite(aSide), mAxleGear)) {
				// Make adjacent Gears rotate according to the Gear on the opposite Side.
				if (aNegative) for (byte tSide = 0; tSide < 6; tSide++)
					if ((sidesValidButAxis(aSide) & (1 << tSide)) != 0 && faceConnected(tSide, mAxleGear)) rRotationData |= (1 << tSide);
				return (byte) ((rRotationData & mAxleGear & 63) | 64);
			}
			// There are no Gears on that Axle — the Passthrough takes over before this gets called
			// (upstream :250-253; returning the current data so nothing breaks too badly).
			return mRotationData;
		}
		// Axle not involved. (upstream :255-263)
		if (faceConnected(aSide, mAxleGear)) {
			// The Gear on opposite Sides of the Gearbox rotates the opposite direction.
			if (aNegative) rRotationData |= (1 << opposite(aSide));
			// All adjacent Gears need to rotate the opposite direction of this Gear.
			if (!aNegative) for (byte tSide = 0; tSide < 6; tSide++)
				if ((sidesValidButAxis(aSide) & (1 << tSide)) != 0 && faceConnected(tSide, mAxleGear)) rRotationData |= (1 << tSide);
			return (byte) ((rRotationData & mAxleGear & 63) | 64);
		}
		// This Facing is not even connected so nothing to do here (upstream :264-267).
		return mRotationData;
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :182-225 server branch)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the :222 client branch is the sound pool
		if (mJammed || !mGearsWork) mCurrentPower = 0; // upstream :184

		mTransferredLast = Math.abs(mCurrentPower * mCurrentSpeed); // upstream :186

		if (mUsedGear && mCurrentPower > 0) { // upstream :188
			boolean temp = true;
			while (temp) {
				temp = false;
				// Due to Geometry, there can only ever be up to 3 Output Sides at once. (upstream :192-193)
				long tUsable = Math.max(1, mCurrentPower / 3);
				for (byte i = 0; i < 6; i++) {
					byte tSide = (byte) ((mOrder + i) % 6);
					if (faceConnected(tSide, mInputtedSides)) continue; // upstream :196 — never into the input face
					EnergyTarget tTarget = adjacency().adjacent(tSide);
					long tUsed = 0;
					if (faceConnected(tSide, mAxleGear)) {
						// upstream :197-203 — the gear face emits by its own rotation bit
						tUsed = tTarget == null ? 0 : ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.RU, tTarget.side(),
								(mRotationData & (1 << tSide)) != 0 ? +mCurrentSpeed : -mCurrentSpeed, tUsable, this, tTarget.receiver());
					} else if (axisXYZ(axleAxis(), tSide) && faceConnected(opposite(tSide), mAxleGear)) {
						// upstream :204-210 — the axle face emits by the OPPOSITE face's rotation bit
						tUsed = tTarget == null ? 0 : ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.RU, tTarget.side(),
								(mRotationData & (1 << opposite(tSide))) == 0 ? +mCurrentSpeed : -mCurrentSpeed, tUsable, this, tTarget.receiver());
					}
					if (tUsed > 0) {
						mCurrentPower -= tUsed;
						if (mCurrentPower <= 0) {
							temp = false;
							break;
						}
						temp = true;
					}
				}
				if (++mOrder >= 6) mOrder = 0; // upstream :214
			}
		}

		mTransferredLast -= Math.abs(mCurrentPower * mCurrentSpeed); // upstream :217 — what actually left
		if (!mUsedGear) mRotationData &= ~64; // upstream :218 — clear the running bit when idle
		oInputtedSides = mInputtedSides; // upstream :219
		mInputtedSides = 0; // upstream :220
		mUsedGear = false; // upstream :221
	}

	// ---------------------------------------------------------------------------
	// the input (upstream doInject :347-410 verbatim, the Root doInject hook)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSpeed, long aPower, boolean aDoInject) {
		if (!isEnergyType(aEnergyType, aSide, false)) return 0; // upstream :348
		if (!axisXYZ(axleAxis(), aSide) && !faceConnected(aSide, mAxleGear)) return 0; // upstream :349
		if (!aDoInject) return mIgnorePower == 0 ? aPower : 0; // upstream :350 (the theoretical probe)

		// Received Input from this Side. (upstream :352-353)
		mInputtedSides |= (byte) (1 << aSide);

		long tSpeed = Math.abs(aSpeed); // upstream :355

		if (tSpeed > mMaxThroughPut) { // upstream :357-370 — the gears EXPLODE
			if (getTimer() < 10) return aPower; // upstream :358 (the load grace)
			breakGears();
			return aPower;
		}

		// Free Axle means it is always a Passthrough. (upstream :372-375)
		if (axisXYZ(axleAxis(), aSide) && !faceConnected(aSide, mAxleGear) && !faceConnected(opposite(aSide), mAxleGear)) {
			EnergyTarget tTarget = adjacency().adjacent(opposite(aSide));
			return tTarget == null ? 0 : ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.RU, tTarget.side(), aSpeed, aPower, this, tTarget.receiver());
		}

		// Just void all power if the Gearbox is not set up properly. (upstream :377-378)
		if (!mGearsWork) return aPower;

		// There already has been at least one Input during this Tick. Add more Power. (upstream :380-396)
		if (mUsedGear) {
			byte tRotationData = getRotations(aSide, aSpeed < 0);
			if (tRotationData != mRotationData) {
				// Gears are jamming! (upstream :383-389 — zero output, zero explosion)
				mRotationData = 0;
				mJammed = true;
				return aPower;
			}
			// If ignoring further Inputs, keep the old values. (upstream :390-391)
			if (mIgnorePower != 0) return 0;
			// Just take the lowest Speed available. (upstream :392-395)
			mCurrentSpeed = Math.min(tSpeed, mCurrentSpeed);
			mCurrentPower += aPower;
			return aPower;
		}
		// There was no Input during this Tick yet. (upstream :397-409)
		if ((mRotationData = getRotations(aSide, aSpeed < 0)) != 0) {
			mUsedGear = true;
			// Still had leftover Power from last time. Start ignoring Input in order to not waste Power.
			if (mCurrentPower > 0) mIgnorePower++; else mIgnorePower = 0; // upstream :401
			if (mIgnorePower != 0) return 0;
			// Set Maximum Speed and current Power.
			mCurrentSpeed = tSpeed;
			mCurrentPower = aPower;
			return aPower;
		}
		return 0;
	}

	/**
	 * The gear explosion (upstream :359-368 minus the SFX): scrap + the gear leg drop,
	 * the mask zeroes, the block SURVIVES with {@code mGearsWork = checkGears()}.
	 */
	private void breakGears() {
		int tCount = faceConnectionCount(mAxleGear); // upstream :360
		if (tCount > 0) dropGearDebris(tCount); // upstream :361-365 (the sound rides the audio pool)
		mAxleGear = 0; // upstream :366
		updateClientData(); // upstream :367
		mGearsWork = checkGears(); // upstream :368
	}

	/**
	 * The drop legs (upstream :362-364, the ST.drop form): scrap 9+rng(27) of the row
	 * material through the runtime material-item lookup (silently absent when the
	 * (prefix, material) item is not registered), and the gear leg tCount-1 — the gear
	 * ITEM does not exist yet, so the block item itself drops (the declared
	 * p12-gear-items-pool substitution, the task card ruling).
	 */
	private void dropGearDebris(int aGearCount) {
		if (!hasLevel() || isClientSide()) return;
		BlockPos tPos = getBlockPos();
		Item tScrap = null;
		RegistryObject<Item> tScrapItem = GTMaterialItems.get(OP.scrapGt, MT.WoodTreated);
		if (tScrapItem != null && tScrapItem.isPresent()) tScrap = tScrapItem.get();
		int tScrapCount = 9 + getLevel().random.nextInt(27); // upstream 9+rng(27)
		if (tScrap != null) spawnDrop(new ItemStack(tScrap, tScrapCount), tPos);
		if (aGearCount > 1) spawnDrop(new ItemStack(getBlockState().getBlock(), aGearCount - 1), tPos); // the gear leg → the block item (declared)
	}

	private void spawnDrop(ItemStack aStack, BlockPos aPos) {
		if (aStack.isEmpty()) return;
		getLevel().addFreshEntity(new ItemEntity(getLevel(), aPos.getX() + 0.5, aPos.getY() + 0.5, aPos.getZ() + 0.5, aStack));
	}

	// ---------------------------------------------------------------------------
	// the config channel (the /gt6engine gearbox direct write — no upstream line,
	// the P9 "acceptance channel is not the upstream player semantics" ruling)
	// ---------------------------------------------------------------------------

	/**
	 * The RCON mask write (the monkey-wrench :142-153 data structure without the tool):
	 * gears = bits 0-5, axle axis = 1/2/3 (0 = none). The tool path clears the jam and
	 * re-checks (:148-149) — so does this.
	 */
	public void setMasks(int aGearMask, int aAxleAxis) {
		mAxleGear = (aGearMask & 63) | ((aAxleAxis & 3) << 6);
		mJammed = false; // upstream :148
		mGearsWork = checkGears(); // upstream :149
		setChanged();
		updateClientData();
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the crank/rig D1 form)
	// ---------------------------------------------------------------------------

	/** The offline test seam (the rig/crank mAdjacencyOverride form). */
	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	private IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	// ---------------------------------------------------------------------------
	// the energy face family (upstream :412-421 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return TD.Energy.RU == aEnergyType; // upstream :412
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return (aTheoretical || !mJammed) && isEnergyType(aEnergyType, aSide, false); // upstream :413
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, true); // upstream :414 (the Root default shape — the real output walk happens in the tick loop)
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return 0; // upstream :415
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return mMaxThroughPut / 2; // upstream :416
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return mMaxThroughPut; // upstream :417
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 0; // upstream :418
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return mMaxThroughPut / 2; // upstream :419
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return mMaxThroughPut; // upstream :420
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.RU.AS_LIST; // upstream :421
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2/writeToNBT2 :64-71)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STOPPED, mJammed); // upstream :76
		aNBT.putByte(NBT_CONNECTION, (byte) mAxleGear); // upstream :77
		aNBT.putLong(NBT_INPUT, mMaxThroughPut); // the row rating (the readFromNBT2 :69 round trip)
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mJammed = aNBT.getBoolean(NBT_STOPPED); // upstream :67
		if (aNBT.contains(NBT_CONNECTION, Tag.TAG_ANY_NUMERIC)) mAxleGear = aNBT.getByte(NBT_CONNECTION) & 0xFF; // upstream :68 (the unsignB chain)
		if (aNBT.contains(NBT_INPUT, Tag.TAG_ANY_NUMERIC)) mMaxThroughPut = Math.max(1, aNBT.getLong(NBT_INPUT)); // upstream :69
		mGearsWork = checkGears(); // upstream :70
	}
}
