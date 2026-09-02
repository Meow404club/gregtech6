package gregtech6.tileentity.energy;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GTAxleBlock;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Axle — task p12-axle-family, ported from
 * gregapi/tileentity/connectors/MultiTileEntityAxle.java (:55-179) as the ONLY cross-block
 * RU carrier of the rotation system (ADR 2026-09-02-p12-rotation-carrier): RU has NO wire
 * family (upstream WireElectric hard-locks EU, WireElectric.java:180/:184), the axle
 * forwards packets by adjacency recursion, and there is NO network object — every tick the
 * source pushes the packet through the whole chain (the Create KineticNetwork/stress-net
 * shape is the studied counter-example, NOT ported; research tmp.research.p12-engine-family
 * q4).
 *
 * <p>The transfer recursion (upstream {@code transferRotations} :105-117, verbatim): a
 * packet arriving on {@code aSide} leaves on the opposite face ({@code OPOS[aSide]}); the
 * neighbor there is either another axle (recursion, gated on its
 * {@code isEnergyAcceptingFrom(RU, side, F)}) or any other receiver
 * ({@code ITileEntityEnergy.Util.insertEnergyInto} — the root Util handshake), and the
 * {@code aAlreadyPassed} set kills loops. The idle gate (upstream :112): while
 * {@code oRotationDir == 0} — the previous tick had no rotation — the axle spins idle and
 * does NOT forward (the packet is booked as consumed by the spin-up, upstream returns
 * {@code aPower} there). A face with no reachable receiver books zero (upstream :114), so
 * the caller's packet comes back unconsumed.
 *
 * <p>Zero loss (upstream :159 {@code getEnergyLossPerMeter} = 0): the recursion forwards
 * the packet UNTOUCHED — the ITileEntityEnergyDataConductor marker surface of upstream is
 * not ported (the p7 ADR ruling 1), its one live semantic (no per-meter loss) lives in
 * exactly this pass-through.
 *
 * <p>Overspeed/bandwidth pop-off (upstream {@code addToEnergyTransferred} :119-131): the
 * bookkeeping happens FIRST, then {@code |aSpeed| > mSpeed || mTransferredPower > mPower}
 * breaks the axle — and the caller is handed back the ORIGINAL power (upstream
 * {@code return aOriginalPower} :128, the "original amount returned to the caller" form),
 * while an unbroken axle returns only what its downstream actually consumed. The physical
 * break is DEFERRED to this axle's own next tick ({@link #mBreakPending} then
 * {@code level.destroyBlock(pos, true)} — the drop-loot path, NOT an onRemove override):
 * the root ITileEntityEnergy interface note pins destructive reactions out of the packet
 * path ("do not set Blocks... just set a flag to do it in your own next tick"). Upstream
 * pops off synchronously inside the injection stack (TileEntityBase04 popOff :173-177 =
 * drop + setToAir) and plays SFX.MC_BREAK :126 — the sound rides the audio pool (no seam
 * in this port). "Yes Rotation Speed only becomes a problem when it is actually being
 * transferred, if the Axle just Rotates Idle then it can spin at ludicrous Speeds"
 * (upstream :123-124).
 *
 * <p>Connectivity (upstream {@code canConnect} :134-137 verbatim): the neighbor must be an
 * ITileEntityEnergy that accepts OR emits RU (theoretical probe) — EU/KU wires and machines
 * refuse. The straight-line rule of the upstream ConnectorStraight base
 * (TileEntityBase11ConnectorStraight.java:63-66 "Makes sure the Axles are going actually
 * straight") is carried by the AXIS BlockState: the two faces on the axis are the connected
 * faces ({@link #connected}), all perpendicular faces refuse — the declared port-ism of the
 * task card (upstream mFacing six-way placement → 1.20.1 straight axis three-way, the
 * ConnectorStraight line semantics need exactly one axis).
 *
 * <p>Ratings (task spec ③, the Loader kinetic section rows): mSpeed = NBT_PIPESIZE =
 * VMAX[tier] (CS.java:150) and mPower = NBT_PIPEBANDWIDTH — Wood 1/2/4/8 at VMAX[0]=16,
 * Bronze-band 2/4/8/16 at VMAX[1]=64, ... down to Trinitanium 128/256/512/1024 at
 * VMAX[7]=262144 (Loader_MultiTileEntities.java:1663-1666/:1672-1675 and the material rows
 * through :1744); the four diameters PX_P 6/9/12/16 (CS.java:492). The table itself lives
 * in {@link gregtech6.registry.GT6Kinetics#AXLE_SPECS}. The tachometer tool readout
 * (upstream onToolClick2 :82-87, {@code mTransferredLast + " RU/t"}) is pooled to the
 * {@code /gt6engine stat} channel (the task card ruling — no tool item in this port yet).
 *
 * <p>Cropped with declaration: ITileEntityProgress (:35/:166-167 — no progress surface in
 * this port), IMTE_GetDebugInfo (:169 — the F3 debug seam), the texture/rotation render
 * family (:171-172 — static placeholder texture, the animated spin icons are a render-pool
 * item), and the upstream NBT_FLAMMABILITY row half (burning axles are a later card).
 */
public class GTAxleBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.java:1198-1199/:1224). */
	public static final String NBT_ACTIVE_DATA = "gt.active.data";
	public static final String NBT_PIPESIZE = "gt.pipesize";
	public static final String NBT_PIPEBANDWIDTH = "gt.pipebandwidth";

	/** The upstream field defaults (:56 — mPower = 1, mSpeed = 32). */
	public static final long DEFAULT_SPEED = 32;
	public static final long DEFAULT_POWER = 1;

	/** The speed rating (upstream mSpeed :56, NBT_PIPESIZE at registration = VMAX[tier]). */
	public long mSpeed = DEFAULT_SPEED;

	/** The bandwidth rating in packets/tick (upstream mPower :56, NBT_PIPEBANDWIDTH). */
	public long mPower = DEFAULT_POWER;

	/** The rotation-direction visual/phase byte (upstream mRotationDir :57, the :111 math). */
	public byte mRotationDir = 0;

	/** The previous-tick rotation byte (upstream oRotationDir :57, the :112 idle gate). */
	public byte oRotationDir = 0;

	/** The transfer bookkeeping (upstream :56): last-tick RU/t readout + the live accumulators. */
	public long mTransferredPower = 0, mTransferredSpeed = 0, mTransferredEnergy = 0, mTransferredLast = 0;

	/**
	 * The deferred pop-off flag (the root interface note: no setBlock inside the packet
	 * path) — consumed at the head of the next server tick by {@link #popOffNow()}.
	 */
	public boolean mBreakPending = false;

	/** The BE axis mirror (the BlockState AXIS is the authority, the crank facing-mirror form). */
	public Direction.Axis mAxis = Direction.Axis.X;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTAxleBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET. The ratings come
	 * from the placed row (the 1.20.1 carrier of the upstream registration NBT: the block
	 * instance carries spec + size → mSpeed = VMAX[tier], mPower = bandwidth[sizeIndex]);
	 * a non-axle state (the offline fixture) keeps the upstream :56 field defaults, and a
	 * saved NBT overrides both (the readFromNBT2 :63-64 chain).
	 */
	public GTAxleBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.AXLE_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTAxleBlock tAxle) {
			mSpeed = Math.max(1, GT6Kinetics.axleSpeed(tAxle.spec));
			mPower = Math.max(1, tAxle.spec.bandwidth()[tAxle.sizeIndex]);
		}
	}

	@Override
	public String getTileEntityName() {
		return "axle"; // BET registry path mirrors it (GTBlockEntities.AXLE_BE)
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :90-98 server branch + :100-101 checks)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncAxisFromState();
		if (mBreakPending) { // the deferred pop-off (the interface note defer rule)
			mBreakPending = false;
			popOffNow();
			return;
		}
		if (mTransferredSpeed == 0 && aTimer > 5) mRotationDir = 0; // upstream :94
		mTransferredLast = mTransferredEnergy; // upstream :95 — the tachometer readout (/gt6engine stat)
		mTransferredEnergy = mTransferredSpeed = mTransferredPower = 0; // upstream :96
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oRotationDir = mRotationDir; // upstream :101 — the idle gate lags one tick
	}

	/** The physical break (upstream popOff, TileEntityBase04:173-177 = drop + setToAir), the loot path. */
	private void popOffNow() {
		if (!hasLevel() || getLevel().isClientSide()) return;
		getLevel().destroyBlock(getBlockPos(), true);
	}

	// ---------------------------------------------------------------------------
	// the transfer recursion (upstream :105-117 verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * The packet walk (upstream :105-117): forwards along the axis, recurses into axles,
	 * hands non-axle receivers to the root Util handshake, returns what was consumed
	 * downstream — or the ORIGINAL power when this axle breaks (upstream :128).
	 */
	public long transferRotations(byte aSide, long aSpeed, long aPower, long aChannel, HashSetNoNulls<BlockEntity> aAlreadyPassed) {
		if (mTimer < 1) return 0; // upstream :106 (the BE not through its first tick yet)

		// upstream :108-111 — the direction math (negative side = ?1:2, positive = ?2:1)
		mRotationDir = (byte) (aSpeed < 0 ? 1 + (aSide & 1) : 2 - (aSide & 1));
		if (oRotationDir == 0) return addToEnergyTransferred(aSpeed, aPower, aPower); // upstream :112 (idle spin-up, no forward)

		byte tOpposite = opposite(aSide);
		if (!canEmitEnergyTo(tOpposite)) return addToEnergyTransferred(aSpeed, aPower, 0); // upstream :114 (dead face = unconsumed)

		EnergyTarget tTarget = adjacency().adjacent(tOpposite);
		if (tTarget == null) return addToEnergyTransferred(aSpeed, aPower, 0); // the null neighbor rides the NoNulls add=false branch of :116

		BlockEntity tNeighbor = (BlockEntity) tTarget.receiver();
		byte tSideInto = tTarget.side();
		if (!aAlreadyPassed.add(tNeighbor)) return addToEnergyTransferred(aSpeed, aPower, 0); // upstream :116 loop kill

		if (tNeighbor instanceof GTAxleBlockEntity tAxle) {
			long tForwarded = tAxle.isEnergyAcceptingFrom(TD.Energy.RU, tSideInto, false)
					? tAxle.transferRotations(tSideInto, aSpeed, aPower, aChannel, aAlreadyPassed)
					: 0;
			return addToEnergyTransferred(aSpeed, aPower, tForwarded);
		}
		long tConsumed = ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.RU, tSideInto, aSpeed, aPower, this, tNeighbor);
		return addToEnergyTransferred(aSpeed, aPower, tConsumed);
	}

	/**
	 * The bookkeeping + break gate (upstream :119-131 verbatim, minus the SFX): books the
	 * transfer, then breaks on overspeed/bandwidth excess — returning the ORIGINAL power to
	 * the caller (:128) instead of the forwarded amount.
	 */
	public long addToEnergyTransferred(long aSpeed, long aOriginalPower, long aPower) {
		mTransferredSpeed += aSpeed;
		mTransferredPower += aPower;
		mTransferredEnergy += Math.abs(aSpeed * aPower);
		// Yes Rotation Speed only becomes a problem when it is actually being transferred,
		// If the Axle just Rotates Idle then it can spin at ludicrous Speeds. (upstream :123-124)
		if (Math.abs(aSpeed) > mSpeed || mTransferredPower > mPower) {
			mBreakPending = true; // upstream :126-127, deferred per the interface note (the SFX is the audio pool)
			return aOriginalPower;
		}
		return aPower;
	}

	// ---------------------------------------------------------------------------
	// connectivity (upstream :134-137 + :163-164 over the AXIS faces)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream {@code canConnect} :134-137 verbatim: the neighbor must accept OR emit RU on
	 * the face towards us (theoretical probe) — {@code aSide} is the NEIGHBOR's own side
	 * (the delegator.mSideOfTileEntity of the upstream signature).
	 */
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		if (aNeighbor instanceof ITileEntityEnergy tEnergy)
			return tEnergy.isEnergyAcceptingFrom(TD.Energy.RU, aSide, true)
					|| tEnergy.isEnergyEmittingTo(TD.Energy.RU, aSide, true);
		return false;
	}

	/** The connected faces of the straight axis (upstream connected(aSide), the mConnections bit replacement). */
	public boolean connected(byte aSide) {
		return Direction.from3DDataValue(aSide).getAxis() == mAxis;
	}

	public boolean canEmitEnergyTo(byte aSide) {
		return connected(aSide); // upstream :163
	}

	public boolean canAcceptEnergyFrom(byte aSide) {
		return connected(aSide); // upstream :164
	}

	// ---------------------------------------------------------------------------
	// the energy face family (upstream :139-151 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEnergyType == TD.Energy.RU; // upstream :139
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.RU.AS_LIST; // upstream :140
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, true) && canEmitEnergyTo(aSide); // upstream :142
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, false) && canAcceptEnergyFrom(aSide); // upstream :143
	}

	@Override
	public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		return 0; // upstream :144
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		return aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)
				? aDoInject
						? transferRotations(aSide, aSize, aAmount, -1, new HashSetNoNulls<>(false, this))
						: aAmount
				: 0; // upstream :145
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return mSpeed; // upstream :146
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return 0; // upstream :147
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return mSpeed; // upstream :148
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return mSpeed; // upstream :149
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 0; // upstream :150
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return mSpeed; // upstream :151
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the crank/rig D1 form) + the axis mirror
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

	/** The GT6 side order opposite (CS OPOS; byte side == Direction.get3DDataValue). */
	public static byte opposite(byte aSide) {
		return (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	/**
	 * The /setblock RCON path and the axis mirror: the state is the authority, the BE
	 * re-syncs at each tick head (the crank syncFacingFromState form). Keyed on the PROPERTY
	 * (not the block class) so the offline test can drive it with a vanilla log state
	 * carrying {@link BlockStateProperties#AXIS}; a state without the property leaves the
	 * mirror alone.
	 */
	void syncAxisFromState() {
		if (getBlockState().hasProperty(GTAxleBlock.AXIS)) {
			mAxis = getBlockState().getValue(GTAxleBlock.AXIS);
		}
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2/writeToNBT2 :60-71)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_ACTIVE_DATA, mRotationDir); // upstream :70
		aNBT.putLong(NBT_PIPESIZE, mSpeed);
		aNBT.putLong(NBT_PIPEBANDWIDTH, mPower);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ACTIVE_DATA, Tag.TAG_ANY_NUMERIC)) mRotationDir = aNBT.getByte(NBT_ACTIVE_DATA); // upstream :62
		if (aNBT.contains(NBT_PIPESIZE, Tag.TAG_ANY_NUMERIC)) mSpeed = Math.max(1, aNBT.getLong(NBT_PIPESIZE)); // upstream :63
		if (aNBT.contains(NBT_PIPEBANDWIDTH, Tag.TAG_ANY_NUMERIC)) mPower = Math.max(1, aNBT.getLong(NBT_PIPEBANDWIDTH)); // upstream :64
	}
}
