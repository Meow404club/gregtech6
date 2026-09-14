package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase01Root;

/**
 * 1.20.1 counterpart of gregapi/tileentity/multiblocks/TileEntityBase11MultiBlockConverter
 * (:48-160, task p29-w3-turbine-dynamo ①) — the energy-in → energy-out conversion base the
 * Large Turbines (steam/gas) and the Large Dynamo ride. The consumers of this base are the
 * card's twelve controllers; the family behaviour lives on the block-carrier ROWS
 * ({@link gregtech6.registry.GT6Turbines} / {@link gregtech6.registry.GT6DynamoHousings}),
 * exactly like every prior registration-row family (the GT6Boilers/GTLargeBoilerBlock form).
 *
 * <h2>The conversion core (TE_Behavior_Energy_Converter.doConversion :61-94, the constant
 * shape of these three families)</h2>
 *
 * The upstream Base11 wires the four TE_Behavior objects (Stats in/out + Capacitor +
 * Converter, :74-83); the P28 dynamo precedent carries the same four flat as fields
 * (GT6DynamoBlockEntity), and this base does the same:
 * <ul>
 * <li><b>Capacitor</b> = NBT_INPUT × 2 (Base11:76), {@link #capacity()}.</li>
 * <li><b>Input gate</b> ({@link #doInject}, the Stats.doInject :56-66 verbatim — the P28
 *     core's already-ported shape): an oversize packet (|size| &gt; 2×NBT_INPUT) consumes
 *     the WHOLE offer and strikes the ladder; a full capacitor answers 0 (the source-side
 *     refund); whole packets only.</li>
 * <li><b>Overload ladder</b> (Base11:145-153 over Base10:140-148): 100 soft strikes clear
 *     the capacitor, strike 101+ arms {@code overcharge} (the Root explosion family).</li>
 * <li><b>The door</b> (Converter:62/:64): {@code tOutput = units(storage, inRec, outRec, F)}
 *     (the UT.java:1682 floor, re-declared here — the P28 core's copy is package-private in
 *     the energy package and this base deliberately does not widen it); emit requires
 *     {@code tOutput >= NBT_OUTPUT/2}. {@code tOutput > NBT_OUTPUT*2} is unreachable for a
 *     capped capacitor (storage ≤ 2×in ⇒ tOutput ≤ 2×out = outMax) — EXCEPT the gas
 *     turbine's fuel bank: the :121 ceil ceiling (tMax = ceil(deficit/power)) lets the last
 *     parallel overshoot the window, banking storage past the capacitor size, and the
 *     conversion door then rounds past outMax. That is the LIVE LIMIT_CONSUMPTION case
 *     (the :70 cap holds the emitted packet at outMax; the gas rows carry the flag for
 *     exactly this), while the :72 overload arm stays the defensive never-observed pair
 *     (the :108-115 "hacking my own code" pre-clamp handles the true stockpiles).</li>
 * <li><b>The emit arm</b> (:84-89 — all three families emit non-size-irrelevant types,
 *     RU/EU): ONE packet of size tOutput per tick, pushed at {@link #getEmittingSide()} OF
 *     THE EMITTING PROXY ({@link #getEmittingTileEntity()} — the far-end structure plate,
 *     the Base11:123/:124 abstract pair LargeDynamo.java:102-103 implements), NOT at the
 *     controller. mMultiplier = 1, mFactor = 1, aNegative = F (Base11:127 constants — no
 *     NBT_MULTIPLIER / negative-output row in this family), aMode = 0.</li>
 * <li><b>WASTE_ENERGY</b> (the two :81/:87/:92 arms): waste rows (steam turbine, dynamo)
 *     do NOT deduct the capacitor on emit (the input side already paid) and VENT
 *     2×NBT_INPUT every tick — the bucket emptied and refilled each tick, the P28 core's
 *     funnel semantics. The gas row is waste=F: the emit deducts
 *     {@code units(used × tOutput, outRec, inRec, T)} (:87) and nothing vents.</li>
 * <li><b>The acceptance formula</b> (Base11:156 verbatim):
 *     {@code (aTheoretical || (!mStopped && (mWasteEnergy || mEmits == mCanEmit)))} —
 *     the steady-state gate that makes the waste=F row stop charging while its emit door
 *     is closed.</li>
 * </ul>
 *
 * <h2>The structure (the facing-rotated 3x3x4 shell)</h2>
 *
 * Both families share the upstream box (LargeTurbineSteam.java:70-101 ≡
 * LargeDynamo.java:53-72): 3x3x4 along the facing axis, the controller centred on the
 * FRONT layer (d = 0), the far plate 3 behind it carrying the ONLY_ENERGY_OUT port. The
 * cells are declared per facing ({@link #structurePattern}, centre-relative over
 * {@link GTMultiBlockPattern#cellOffset}) and the check rides the shared checker (the Coke
 * Oven pilot form, TileEntityCokeOven.java:144-150): the pattern IS the check. The
 * per-cell (part, design, usage) triples are the upstream loop's constants:
 * <ul>
 * <li><b>Steam turbine</b> (LargeTurbineSteam.java:85-97): far centre = design 3 +
 *     ONLY_ENERGY_OUT; the controller layer bottom cell = ONLY_FLUID, the rest of the
 *     layer = ONLY_FLUID_IN (the middle fluid column); every other wall cell is the bottom
 *     row ONLY_FLUID_OUT, the rest NOTHING.</li>
 * <li><b>Large Dynamo</b> (LargeDynamo.java:68): the two middle layers (d = 1..2) are the
 *     18 Large Copper Coils (18040); the two 3x3 end plates are the row's dense wall; the
 *     far centre = design 2 + ONLY_ENERGY_OUT, everything else NOTHING.</li>
 * </ul>
 *
 * <h2>Cropped with declaration</h2>
 * The steam turbine's FLUID path (LargeTurbineSteam.java:46-58/:142-162 — the steam tank
 * pair, the half-split buffer tick, the STEAM_PER_WATER 170 distilled-water counter, the
 * plunger) is the fluid-domain remainder: this card's spec consumes STEAM as the
 * TD.Energy.STEAM packet type over the energy network (the card's STEAM-source dial
 * fixture), so the base carries packets only.
 * {@code ponytail:} the distilled-water byproduct and the tank fill face return with the
 * fluid-attachment card that wires the frontal ONLY_FLUID column to a real tank — the
 * usage masks above are already the structure half of that seam.
 * The activity trinary (TE_Behavior_Active_Trinary) folds to the mActive boolean (the P28
 * core's form); the client minecart sound (:115) is the pool; the LH tooltip stack is the
 * item-lore surface not ported here.
 *
 * <p>KJS face (task card): this base is REGISTRATION-surface (12 controllers over three
 * BETs) + the FM.Gas datapack map (the gas rows are the direct-fill anchor); no KubeJS
 * special face.
 */
public abstract class GTMultiBlockConverter extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy {

	/** The upstream NBT keys (the P28 core carriers; the CS.NBT_* names stay upstream). */
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_CAPACITOR = "gt.capacitor";
	/** The row config mirror (the upstream registration NBT the block carrier replaces) — persisted for the /data RCON readback. */
	public static final String NBT_INPUT = "gt.input";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_ENERGY_ACCEPTED = "gt.energy.accepted";
	public static final String NBT_ENERGY_EMITTED = "gt.energy.emitted";
	public static final String NBT_WASTE_ENERGY = "gt.waste_energy";
	public static final String NBT_LIMIT_CONSUMPTION = "gt.limit_consumption";
	/** The RCON observability pair (the p28-c-dynamo-family-be handoff's W3 accounting channel): the last injected packet size, the last conversion door readout, the last delivered packet size. Live diagnostics — re-derived from zero on reload. */
	public static final String NBT_LAST_IN = "gt.last_in";
	public static final String NBT_LAST_CONVERTED = "gt.last_converted";
	public static final String NBT_LAST_OUT = "gt.last_out";

	/** The row's NBT_INPUT column (the packet window centre; 12288 for the steam T1 row, Loader :1254). */
	public long mInput = 16;
	/** The row's NBT_OUTPUT column (4096 for the steam T1 row, Loader :1254). */
	public long mOutput = 8;
	/** The accepted energy type (NBT_ENERGY_ACCEPTED — STEAM/RU/HU per row). */
	public TagData mInputType = gregapi.data.TD.Energy.EU;
	/** The emitted energy type (NBT_ENERGY_EMITTED — RU/EU per row). */
	public TagData mOutputType = gregapi.data.TD.Energy.EU;
	/** The row's NBT_WASTE_ENERGY (T for the steam turbine and dynamo rows, F for gas). */
	public boolean mWasteEnergy = false;
	/** The row's NBT_LIMIT_CONSUMPTION (T for the gas rows only). */
	public boolean mLimitConsumption = false;

	/** The capacitor energy in input units (upstream mStorage.mEnergy). */
	public long mStorage = 0;
	/** The soft-hammer stop (Base11:49; the toggle channel is the pool). */
	public boolean mStopped = false;
	/** The explosion-prevention strikes (Base11:50, the :145-153 ladder counter). */
	public int mExplosionPrevention = 0;
	/** The last conversion emitted (upstream mConverter.mEmitsEnergy :66/:82/:88). */
	public boolean mActive = false;
	/** The :64 door readout (upstream mConverter.mCanEmitEnergy). */
	public boolean mCanEmitEnergy = false;
	/** The acceptance gate's stale mirror (the mEmitsEnergy == mCanEmitEnergy term reads the LAST completed tick's pair — mActive alone would blur the closed-door read). */
	private boolean mLastTickEmits = false;

	/** The RCON accounting triple (see NBT_LAST_*). */
	public long mLastIn = 0, mLastConverted = 0, mLastOut = 0;

	/** The cached per-facing structure pattern (rebuilt on a facing flip). */
	@Nullable
	private GTMultiBlockPattern mPattern;
	/** The facing the cached pattern was built for. */
	private byte mPatternFacing = -1;

	/** The BET-factory constructor (the registry row config is injected by the subclass after super). */
	protected GTMultiBlockConverter(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	/** The row-config injection point (the block carrier + the offline test seam, the applyTier form). */
	protected void applyRow(long aInput, long aOutput, TagData aInputType, TagData aOutputType, boolean aWaste, boolean aLimit) {
		mInput = aInput;
		mOutput = aOutput;
		mInputType = aInputType;
		mOutputType = aOutputType;
		mWasteEnergy = aWaste;
		mLimitConsumption = aLimit;
	}

	// ---------------------------------------------------------------------------
	// the tick + conversion (Base11:107-133 over Converter:61-94, aMode=0, mult=1)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (!aIsServerSide) return; // the :114-116 client branch is the minecart-sound pool
		syncFacingFromState();
		if (checkStructure(false)) doConversion(aTimer); // Base11:110-113 — convert only while formed
	}

	/** The state is the authority on the /setblock path (the GT6DynamoBlockEntity syncFacingFromState form). */
	protected void syncFacingFromState() {
		BlockState tState = getBlockState();
		if (tState.hasProperty(FACING)) {
			mFacing = (byte) tState.getValue(FACING).get3DDataValue();
		}
	}

	/** Base11:126-133 — the door, the proxy emit, the waste pair; the gas turbine overrides the fuel half upstream of this. */
	protected void doConversion(long aTimer) {
		long tOutput = units(mStorage, mInput, mOutput, false); // Converter:62 — the UT.Code.units floor direction
		mCanEmitEnergy = tOutput >= mOutput / 2; // :64
		mActive = false; // :66
		if (mCanEmitEnergy) {
			if (tOutput > mOutput * 2) { // :68 — unreachable for a capped capacitor (declared), the defensive pair
				if (mLimitConsumption) {
					tOutput = mOutput * 2; // :70 — LIMIT_CONSUMPTION caps at the out window
				} else if (aTimer > 2) {
					overload(mStorage, mOutputType); // :72 → Base11:129 — the ladder arms overcharge
					mStorage = 0; // Base11:131
					return;
				} else {
					mStorage = 0; // :74 — the chunkload grace (aTimer <= 2)
					return;
				}
			}
			long tUsed = emitThroughProxy(tOutput); // :85 — the size-carrying branch, ONE packet
			if (tUsed > 0) { // :86
				mActive = true; // :88
				mLastOut = tOutput;
				if (!mWasteEnergy) mStorage -= units(tUsed * tOutput, mOutput, mInput, true); // :87 — waste=F deducts
			}
		}
		mLastConverted = tOutput; // the door readout (the RCON channel; a no-emit tick still converted)
		if (mWasteEnergy) mStorage = Math.max(0, mStorage - mInput * 2); // :92, aMode = 0 — the funnel vent
		mLastTickEmits = mActive;
	}

	/**
	 * The emit through the far-end plate (the :85 proxy form): ONE packet of
	 * {@code aSize} at {@link #getEmittingSide()} of the resolving proxy BE. The adjacency
	 * resolves from the PROXY (the plate's neighbour behind it receives — the energy exits
	 * the BACK wall of the structure).
	 */
	protected long emitThroughProxy(long aSize) {
		BlockEntity tEmitter = getEmittingTileEntity();
		IEnergyAdjacency tAdjacency = aSide -> {
			if (!tEmitter.hasLevel()) return null;
			BlockEntity tNeighbor = tEmitter.getLevel().getBlockEntity(tEmitter.getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
		return ITileEntityEnergy.Util.emitEnergyToSide(mOutputType, getEmittingSide(), aSize, 1, tEmitter, tAdjacency);
	}

	/**
	 * The emitting proxy (LargeDynamo.java:102 verbatim shape): the far-end cell at
	 * OPOS[mFacing] distance 3 — the ONLY_ENERGY_OUT plate; a dead/non-GT/absent BE falls
	 * back to this controller. The GT-TE gate mirrors the upstream
	 * {@code instanceof ITileEntityUnloadable} (vanilla BEs never proxied; the Root chain
	 * is the port's marker).
	 */
	public BlockEntity getEmittingTileEntity() {
		if (hasLevel()) {
			BlockEntity tPlate = getLevel().getBlockEntity(
					getBlockPos().relative(Direction.from3DDataValue(opposite(mFacing)), 3));
			if (tPlate instanceof TileEntityBase01Root && !tPlate.isRemoved()) return tPlate;
		}
		return this;
	}

	/** The emit side (LargeDynamo.java:103): OPOS[mFacing] — out the BACK, through the far plate. */
	public byte getEmittingSide() {
		return opposite(mFacing);
	}

	/** The input face (LargeDynamo.java:104): the FRONT. */
	public boolean isInput(byte aSide) {
		return aSide == mFacing;
	}

	/** The output face (LargeDynamo.java:105): the BACK (the proxy's emit side). */
	public boolean isOutput(byte aSide) {
		return aSide == opposite(mFacing);
	}

	/** The GT6 side-order opposite (CS OPOS; the P28 core form). */
	public static byte opposite(byte aSide) {
		return (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	/** The overload ladder (Base11:145-153 verbatim): 100 soft strikes, then overcharge. */
	protected void overload(long aSize, TagData aEnergyType) {
		if (mExplosionPrevention < 100) {
			mExplosionPrevention++;
			mStorage = 0;
		} else {
			overcharge(aSize, aEnergyType); // the Root explosion family (offline: log-only)
		}
	}

	/**
	 * Upstream {@code UT.Code.units} (UT.java:1682 verbatim) — re-declared from the P28
	 * core (its copy is package-private in the energy package; both cite the same upstream
	 * line, the wave-card wall: copy the direction, never reinvent the remainder behavior).
	 */
	static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
		if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
		return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}

	// ---------------------------------------------------------------------------
	// the input (Base11:135-143 over Stats.doInject :56-66 — the P28 core shape)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aSize == 0 || !isEnergyAcceptingFrom(aEnergyType, aSide, false)) return 0;
		if (aDoInject) mLastIn = aSize; // the RCON channel (upstream writes mNegativeInput here; these rows carry no negative carrier)
		long tAbs = Math.abs(aSize);
		if (tAbs > mInput * 2) { // the Stats oversize leg :57-61 — consumes ALL
			if (aDoInject) overload(tAbs, aEnergyType); // Base11:138-141
			return aAmount;
		}
		if (mStorage >= capacity()) return 0; // Stats :62 — the full gate: 0, the source refunds
		long tEnergy = Math.min(capacity() - mStorage, tAbs * aAmount); // Stats :63
		long tConsumed = Math.min(aAmount, (tEnergy / tAbs) + (tEnergy % tAbs != 0 ? 1 : 0)); // Stats :63
		if (aDoInject) mStorage += tConsumed * tAbs; // Stats :64
		return tConsumed;
	}

	/** The capacitor capacity = NBT_INPUT × 2 (Base11:76). */
	public long capacity() {
		return mInput * 2;
	}

	// ---------------------------------------------------------------------------
	// the faces (Base11:155-160)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting ? aEnergyType == mOutputType : aEnergyType == mInputType; // :155
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// :156 verbatim — the middle term is the steady-state gate (the waste=F gas row
		// stops charging while its emit door is closed)
		return (aTheoretical || (!mStopped && (mWasteEnergy || mLastTickEmits == mCanEmitEnergy)))
				&& isInput(aSide) && isEnergyType(aEnergyType, aSide, false);
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isOutput(aSide) && isEnergyType(aEnergyType, aSide, true); // :157
	}

	// the size bands (Base11:76/:77, the P28 core's type-guarded form)

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		if (aEnergyType != mInputType) return 0;
		return mInput <= 16 ? 1 : mInput / 2; // Base11:77, takesAnyLowerSize() = F
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == mInputType ? mInput : 0;
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == mInputType ? mInput * 2 : 0;
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return aEnergyType == mOutputType ? mOutput / 2 : 0;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == mOutputType ? mOutput : 0;
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == mOutputType ? mOutput * 2 : 0;
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
		return java.util.Arrays.asList(mInputType, mOutputType); // Base11:159 — both converter halves
	}

	// pull-based extraction stays the Root default (0) — pure push sources, the P28 posture

	// ---------------------------------------------------------------------------
	// the structure (the per-facing declared pattern; the checker IS the check)
	// ---------------------------------------------------------------------------

	/** The row's wall block (the dense wall the shell is built from — the block-carrier row read, the boiler getWallBlock form). */
	protected abstract Block getWallBlock();

	/** The Large Copper Coil block (the dynamo middle segment; 18040, the card ① registration). */
	protected Block getCoilBlock() {
		Block tCoil = GTMultiBlocks.anyPartBlock("large_copper_coil");
		return tCoil != null ? tCoil : GTMultiBlocks.COKE_OVEN_BRICKS.get(); // the defensive default, unreachable in production
	}

	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mPattern == null || mPatternFacing != mFacing) {
			mPattern = structurePattern(mFacing, getWallBlock(), getCoilBlock(),
					fluidColumn(), coilSegment(), farPlateDesign());
			mPatternFacing = mFacing;
		}
		return mPattern;
	}

	/** Whether the controller layer is the turbine's middle fluid column (the dynamo answers false — all NOTHING). */
	protected boolean fluidColumn() {
		return false;
	}

	/** Whether the two middle layers are the Large Copper Coil segment (the dynamo answers true). */
	protected boolean coilSegment() {
		return false;
	}

	/** The far-plate design (the upstream aDesign argument: 3 for the turbines, 2 for the dynamo). */
	protected int farPlateDesign() {
		return 3;
	}

	/**
	 * The facing-rotated 3x3x4 shell, centre-relative. The cell coordinate over depth
	 * {@code d} (0..3; d = 0 the controller layer, d = 3 the far plate) and the
	 * perpendicular offsets {@code (p1, p2)} (±1) is
	 * {@code (1−d)·front + p1·axis1 + p2·axis2} — the inverse of the
	 * {@code worldOffset = cell − OFF[facing]} arithmetic for the upstream loop's world
	 * walk ({@code world = controller + d·(−OFF) + p·perp}, LargeTurbineSteam.java:71-96;
	 * the perpendicular axes are the two world axes the facing is not on). The usage table
	 * folds the two families:
	 * <ul>
	 * <li>far centre — the wall block, {@code ONLY_ENERGY_OUT}, the far design;</li>
	 * <li>coil layers (the dynamo) — the coil block, NOTHING;</li>
	 * <li>the controller layer (the turbine) — the wall, bottom = ONLY_FLUID / rest =
	 *     ONLY_FLUID_IN (the middle fluid column);</li>
	 * <li>every other wall cell — the turbine: bottom row ONLY_FLUID_OUT / rest NOTHING
	 *     (LargeTurbineSteam.java:93); the dynamo: NOTHING (LargeDynamo.java:68).</li>
	 * </ul>
	 */
	public static GTMultiBlockPattern structurePattern(byte aFacing, Block aWall, Block aCoil,
			boolean aFluidColumn, boolean aCoilSegment, int aFarDesign) {
		Direction tFront = Direction.from3DDataValue(aFacing);
		Direction tAxis1 = tFront.getAxis() == Direction.Axis.X
				? Direction.fromAxisAndDirection(Direction.Axis.Z, Direction.AxisDirection.POSITIVE)
				: Direction.fromAxisAndDirection(Direction.Axis.X, Direction.AxisDirection.POSITIVE);
		Direction tAxis2 = Direction.UP; // the perpendicular plane of a horizontal facing = the other horizontal axis + Y (the upstream y box, tMinY = yCoord-1)

		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int tD = 0; tD <= 3; tD++) for (int tP1 = -1; tP1 <= 1; tP1++) for (int tP2 = -1; tP2 <= 1; tP2++) {
			boolean tFarCentre = tD == 3 && tP1 == 0 && tP2 == 0;
			boolean tCoilLayer = aCoilSegment && (tD == 1 || tD == 2);
			int tX = (1 - tD) * tFront.getStepX() + tP1 * tAxis1.getStepX() + tP2 * tAxis2.getStepX();
			int tY = (1 - tD) * tFront.getStepY() + tP1 * tAxis1.getStepY() + tP2 * tAxis2.getStepY();
			int tZ = (1 - tD) * tFront.getStepZ() + tP1 * tAxis1.getStepZ() + tP2 * tAxis2.getStepZ();
			Block tPart;
			int tUsage, tDesign;
			if (tFarCentre) {
				tPart = aWall; // the plate is a WALL block carrying the port (the upstream pair)
				tUsage = MultiBlockPartBlockEntity.ONLY_ENERGY_OUT;
				tDesign = aFarDesign;
			} else if (tCoilLayer) {
				tPart = aCoil;
				tUsage = MultiBlockPartBlockEntity.NOTHING;
				tDesign = 0;
			} else if (tD == 0 && aFluidColumn) {
				// the controller layer — the turbine's middle fluid column (LargeTurbineSteam:90-91)
				tPart = aWall;
				tUsage = tY == -1 ? MultiBlockPartBlockEntity.ONLY_FLUID : MultiBlockPartBlockEntity.ONLY_FLUID_IN;
				tDesign = 0;
			} else {
				tPart = aWall;
				tUsage = aFluidColumn && tY == -1 ? MultiBlockPartBlockEntity.ONLY_FLUID_OUT : MultiBlockPartBlockEntity.NOTHING;
				tDesign = 0;
			}
			tBuilder.formingPart(tX, tY, tZ, tPart, tUsage, tDesign);
		}
		return tBuilder.build();
	}

	/** The structure-box query (the coke-oven-generic form over the pattern bounds + the centre anchor). */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		GTMultiBlockPattern tPattern = getStructurePattern();
		if (tPattern == null) return false;
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing);
		int tCx = getBlockPos().getX() + tAnchor[0], tCy = getBlockPos().getY() + tAnchor[1], tCz = getBlockPos().getZ() + tAnchor[2];
		return aX >= tCx + tPattern.minX() && aX <= tCx + tPattern.maxX()
				&& aY >= tCy + tPattern.minY() && aY <= tCy + tPattern.maxY()
				&& aZ >= tCz + tPattern.minZ() && aZ <= tCz + tPattern.maxZ();
	}

	/** The check rides the shared checker (the Coke Oven pilot form, TileEntityCokeOven.java:144-150). */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay;
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				this, mFacing, aCoordinates, aPlayer, aInventory);
		if (tVerdict.unloaded) return mStructureOkay; // :59 — unloaded cells keep the last verdict
		return tVerdict.formed;
	}

	// ---------------------------------------------------------------------------
	// NBT
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STOPPED, mStopped);
		aNBT.putLong(NBT_CAPACITOR, mStorage);
		aNBT.putLong(NBT_INPUT, mInput);
		aNBT.putLong(NBT_OUTPUT, mOutput);
		aNBT.putString(NBT_ENERGY_ACCEPTED, mInputType.mName);
		aNBT.putString(NBT_ENERGY_EMITTED, mOutputType.mName);
		aNBT.putBoolean(NBT_WASTE_ENERGY, mWasteEnergy);
		aNBT.putBoolean(NBT_LIMIT_CONSUMPTION, mLimitConsumption);
		aNBT.putLong(NBT_LAST_IN, mLastIn);
		aNBT.putLong(NBT_LAST_CONVERTED, mLastConverted);
		aNBT.putLong(NBT_LAST_OUT, mLastOut);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED);
		if (aNBT.contains(NBT_CAPACITOR, Tag.TAG_ANY_NUMERIC)) mStorage = Math.max(0, aNBT.getLong(NBT_CAPACITOR));
		if (aNBT.contains(NBT_LAST_IN, Tag.TAG_ANY_NUMERIC)) mLastIn = aNBT.getLong(NBT_LAST_IN);
		if (aNBT.contains(NBT_LAST_CONVERTED, Tag.TAG_ANY_NUMERIC)) mLastConverted = aNBT.getLong(NBT_LAST_CONVERTED);
		if (aNBT.contains(NBT_LAST_OUT, Tag.TAG_ANY_NUMERIC)) mLastOut = aNBT.getLong(NBT_LAST_OUT);
		// the config columns do NOT read back (the block carrier is the authority, the boiler
		// row form); they persist only for the /data RCON readback
	}

	/** A facing flip breaks the pattern cache (the centre-relative cells rotate). */
	@Override
	public void onFacingChange(byte aPreviousFacing) {
		super.onFacingChange(aPreviousFacing);
		mPattern = null;
	}
}
