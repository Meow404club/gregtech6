package gregtech6.tileentity.energy.generators;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GT6MagicAbsorberBlock;
import gregtech6.registry.GT6MagicAbsorbers;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Magic Field Absorber (task p32-magic-absorber) — the 1.20.1 counterpart of
 * {@code MultiTileEntityMagicFieldAbsorber} (:46-121, upstream id 10180, the
 * Loader_MultiTileEntities.java:1005 "Magical Energy Production" row): a single-block
 * generator with NO fuel and NO inventory — it reads the trophy sitting on its TOP face
 * every re-check and pushes the trophy's energy out its FACING face:
 * <ul>
 * <li><b>Dragon Egg</b> on top (:85-86) → {@code mOutput = 64}, type QU;</li>
 * <li><b>any vanilla skull/head</b> on top (:87-88, the single 1.7.10 {@code Blocks.skull}
 *     block = every head type standing AND wall-mounted in metadata form — the modern
 *     port walks the seven standing/wall block pairs) → {@code mOutput = 1}, type TU
 *     (the upstream quip: "I can't foresee this getting OP as heck. XD");</li>
 * <li>anything else → the absorber sits idle (mActive = false, the :82 reset).</li>
 * </ul>
 *
 * <p><b>The emission arm is upstream :101-108 verbatim</b>: the KU ±alternating branch
 * (:102-103) is UNREACHABLE here — its only writer was the Twilight Forest trophy arm —
 * and both live types ride {@link TD.Energy#ALL_SIZE_IRRELEVANT} (TD.java:218 carries QU
 * and TU alike), so the emission is {@code emit(type, 1, mOutput)} = mOutput packets of
 * size 1 per tick (the branch pair itself is kept verbatim for the upstream shape).
 *
 * <h2>Cropped with declaration</h2>
 * <ul>
 * <li><b>The TF trophy arm (:89-97)</b> — the task-card cut: {@code IL.TF_Trophy} is a
 *     Twilight Forest dependency and the port carries no TF face. KU/QU/HU/LU/CU trophy
 *     energies are therefore unreachable; the user can reinstate the arm on request
 *     (the known-deviation note).</li>
 * <li><b>The KU ±alternating emit branch (:102-103)</b> — its only writer was the cut TF
 *     arm (no KU consumers exist in the port either, the task card's 不做-KU-consumers
 *     clause); the branch is not even written.</li>
 * <li><b>The mActive client visual</b> (:98/:113-114 + the :140-158
 *     colored/overlay/overlay_active texture trichotomy) — the static-face datagen (the
 *     laser/dynamo W2-render posture: the activity visual rides the W2 render card,
 *     {@code mActive} stays a server-side flag the stat command reads).</li>
 * <li><b>The ITileEntityRunningActively / ITileEntitySwitchableOnOff interfaces</b>
 *     (:123-127) — no port consumer (the dynamo crop precedent); {@code mStopped} is kept
 *     with its NBT persistence, the toggle channel is the pool.</li>
 * <li><b>The wrench rotation / placement-side quirk</b> (:128-129,
 *     SIDES_BOTTOM_HORIZONTAL) — the block carries the six-way FACING property with the
 *     upstream default SIDE_BOTTOM (DOWN); placement always lands DOWN (the canonical
 *     posture: the absorber sits on the consumer), {@code /setblock} rotates, the
 *     horizontal-click placement form of upstream :73 is not reproduced.</li>
 * <li><b>The mOutput tooltip face</b> (:66-74) — plain BlockItem, no tooltip attach
 *     channel in the port (the laser-domain block rows' identical crop).</li>
 * </ul>
 *
 * <p><b>The metering face</b> (gt.last_out, NOT upstream): the cumulative ACCEPTED mass —
 * {@code emitEnergyToNetwork} returns the packets consumers actually took (the Root gate
 * counts white-burned offers as used, the p32-qu-laser live lesson), and this BE books
 * {@code tUsed × |size|} only when that return is positive. The /gt6magicabsorber
 * stat|reset command reads/resets it — the GT6LaserConverterBlockEntity accounting form.
 */
public class GT6MagicAbsorberBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.NBT_STOPPED/NBT_ACTIVE, :54-55/:61-62). */
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_ACTIVE = "gt.active";
	/** The metering key (the bridge/laser accounting form, NOT upstream). */
	public static final String NBT_LAST_OUT = "gt.last_out";

	/** The soft-hammer stop (upstream :47 {@code mStopped = F}; the toggle channel is the pool). */
	public boolean mStopped = false;
	/** The running flag (upstream :47 {@code mActive = F}; :82/:86/:88 set, the stat face). */
	public boolean mActive = false;
	/** The re-check trigger (upstream :47 {@code mCheck = T}: probe on the first tick). */
	public boolean mCheck = true;

	/** The per-tick output (upstream :48 default 64; :83/:86/:88 rewrite per probe). */
	public long mOutput = 64;

	/** The emitted type (upstream :49 default TU; :86/:88 rewrite per probe). */
	public TagData mEnergyTypeEmitted = TD.Energy.TU;

	/** The cumulative accepted-emitted mass (the metering face, NOT upstream). */
	public long mLastOut = 0;

	/** The tick the metering window opened at (the rate denominator base, NOT upstream). */
	private long mMeterBase = 0;

	/** The vanilla head family (the single 1.7.10 Blocks.skull block split into 14 modern blocks). */
	public static final Set<Block> SKULL_FAMILY = Set.of(
			Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL,
			Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL,
			Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD,
			Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD,
			Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD,
			Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD,
			Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD);

	/** The runtime facing mirror (byte, GT6 side order == Direction.get3DDataValue; FRONT = the output face). */
	public byte mFacing = 0; // DOWN — the upstream getDefaultSide() = SIDE_BOTTOM

	/** BET factory for BlockEntityType.Builder.of — the registry path entry point. */
	public GT6MagicAbsorberBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — the registry factory and the offline (test) entry point. */
	public GT6MagicAbsorberBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6MagicAbsorbers.MAGIC_ABSORBER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "magic_absorber"; // the BET registry path twin (GT6MagicAbsorbers.MAGIC_ABSORBER_BE)
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :77-111, the TF arm and the KU branch declared away)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide || mStopped) return; // :78
		syncFacingFromState(); // the /setblock path: the state is the authority (the dynamo form)
		if (mCheck || mBlockUpdated || aTimer % 600 == 5) { // :79
			boolean tActive = mActive;
			mCheck = false; // :81
			if (hasLevel()) {
				probe(getLevel().getBlockState(getBlockPos().above()).getBlock()); // :82-97 via :84
			} else {
				probe(null); // the offline fixture posture: no level, no trophy
			}
			if (tActive != mActive) updateClientData(); // :98
		}
		if (mActive) emitOnce(); // :101-109
	}

	/**
	 * The :82-97 trophy classification (minus the :89-97 TF arm, the task-card cut): the
	 * probe seam the offline tests drive directly (a Level is unconstructible offline).
	 * Resets mActive/mOutput to the idle :82-83 defaults first, then re-arms per trophy.
	 */
	public void probe(@Nullable Block aBlockAbove) {
		mActive = false; // :82
		mOutput = 64; // :83
		if (aBlockAbove == Blocks.DRAGON_EGG) { // :85
			mActive = true; mOutput = 64; mEnergyTypeEmitted = TD.Energy.QU; // :86
		} else if (aBlockAbove != null && SKULL_FAMILY.contains(aBlockAbove)) { // :87
			mActive = true; mOutput = 1; mEnergyTypeEmitted = TD.Energy.TU; // :88
		}
	}

	/**
	 * The :101-108 emission arm, split out so the offline tests drive it directly (the
	 * GTEnergySourceBlockEntity.emitOnce form). The KU ±branch (:102-103) is not written:
	 * its only writer was the cut TF arm. Books the ACCEPTED mass on gt.last_out.
	 */
	void emitOnce() {
		long tSize, tAmount;
		if (TD.Energy.ALL_SIZE_IRRELEVANT.contains(mEnergyTypeEmitted)) { // :104
			tSize = 1; tAmount = mOutput; // :105
		} else {
			tSize = mOutput; tAmount = 1; // :107
		}
		long tUsed = ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyTypeEmitted, tSize, tAmount, this, adjacency());
		if (tUsed > 0) mLastOut += tUsed * Math.abs(tSize); // the accepted-mass meter (NOT upstream)
	}

	/** The live accounting reset arm (the /gt6magicabsorber reset window form). */
	public void resetAccounting() {
		mLastOut = 0;
		mMeterBase = getTimer();
	}

	/** The metering window's tick count (the rate denominator; the RCON stat face). */
	public long meterWindowTicks() {
		return Math.max(1, getTimer() - mMeterBase);
	}

	/** The accepted-mass rate over the window (delivered/ticks — exact while emitting every tick). */
	public long meterRate() {
		return mLastOut / meterWindowTicks();
	}

	/** The state-authority facing mirror (the dynamo syncFacingFromState form, six-way here). */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6MagicAbsorberBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GT6MagicAbsorberBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the energy-source rig D1 form)
	// ---------------------------------------------------------------------------

	private @Nullable IEnergyAdjacency mAdjacencyOverride = null;

	/** The offline test seam for the emit side. */
	public void setAdjacencyOverrideForTest(@Nullable IEnergyAdjacency aAdjacency) {
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
	// the energy faces (upstream :116-121 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting; // :116 — the emitter answer, any type
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// :117 — the FACING face only, the live activity on the non-theoretical probe
		// (the Root :714 form: aTheoretical || mActive, with the mStopped gate folded in)
		return aSide == mFacing && isEnergyType(aEnergyType, aSide, true) && (aTheoretical || (mActive && !mStopped));
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return false; // a pure source (the :116 answer to the accepting probe is constant false)
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		return 0;
	}

	@Override
	public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		return 0;
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return mOutput; // :118
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return mOutput; // :119
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return mOutput; // :120
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 0;
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return 0;
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return 0;
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.ALL_GT; // :121 — the display face names the whole GT family
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return 0;
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		return 0;
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream :52-63 + the metering key)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_ACTIVE, mActive); // :61
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :62
		aNBT.putLong(NBT_LAST_OUT, mLastOut);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE); // :55
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :54
		if (aNBT.contains(NBT_LAST_OUT, Tag.TAG_ANY_NUMERIC)) mLastOut = Math.max(0, aNBT.getLong(NBT_LAST_OUT));
	}

	/** The stat-face word of the emitted type (the bridge shortType form, the two live types). */
	public static String shortType(TagData aType) {
		if (aType == TD.Energy.QU) return "QU";
		if (aType == TD.Energy.TU) return "TU";
		return aType.toString();
	}
}
