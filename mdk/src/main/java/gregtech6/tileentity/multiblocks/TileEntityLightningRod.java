package gregtech6.tileentity.multiblocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GTMultiBlocks;

/**
 * 1.20.1 counterpart of the GT6 Lightning Rod multiblock — task p24-lightning-rod, ported
 * from gregtech/tileentity/multiblocks/MultiTileEntityLightningRod.java (:50-197) as the
 * canon-FIRST variable-length multiblock (research.p24-r-variable-length: the only
 * variable-formation machine in the whole GT6 census). A 3x3x5 base (Tungsten Walls /
 * Large Niobium-Titanium Coils alternating, the controller at the bottom-centre) plus a
 * 1x1 pillar of Lightning Rod blocks whose length is DISCOVERED at runtime by the
 * unbounded while probe — a hand-written {@link #checkStructure2} exactly because the
 * pattern DSL cannot express existence probes (GTMultiBlockPattern.java:77-81 ruling):
 * {@link #getStructurePattern()} stays null, never bound (decision family-seam; the int
 * variant has zero consumers at card-1 time, the size-seam pilot is pooled).
 *
 * <p><b>The structure (:72-86 verbatim)</b>: {@code mSize = 0} reset FIRST (:74), then the
 * five 3x3 layers — y+0 walls (:77), y+1 coils (:78), y+2 walls (:79), y+3 coils (:80),
 * y+4 walls (:81), every cell design 0 mode NOTHING — then, ONLY when the base holds, the
 * pillar probe (:84): {@code while (checkAndSetTargetOffset(this, 0, 5+mSize, 0, rod...))}
 * counts consecutive rod blocks above the base centre. mSize == 0 still forms (the probe
 * rides the tSuccess gate, :84). The pillar cells are unreachable by the SET scaffold: the
 * ±1 clicked-neighbour door (ITileEntityMultiBlockController.java:138-139) keeps every
 * pillar cell |dy| &gt;= 2 from any click — the form-set decision keeps the walk VERBATIM,
 * zero special-casing.
 *
 * <p><b>The unloaded pre-gate (decision probe-unloaded)</b>: the base FOUR corner columns
 * are {@code isLoaded}-checked before anything runs (the TileEntityLargeBoiler :289-290
 * form); unloaded corners keep the LAST verdict ({@code return mStructureOkay}, :329
 * counterpart) and never force a chunk load. The pillar column shares the controller's own
 * chunk column (full-height 16x384x16 chunks) — zero extra probing, the upstream
 * 1.7.10 while naturally stopping in unloaded terrain folds away.
 *
 * <p><b>mSize, the four consumers (research card)</b>: ① {@link #isInsideStructure} :119
 * verbatim (the 3x3 box below y+5, the 1x1 column up to y+mSize+4); ② the strike gate
 * :132 — {@code mSize > 0 && yCoord + mSize >= 100} (the ABSOLUTE altitude, kept verbatim
 * across the 256→384 height change — decision height-gate, the declared deviation is the
 * more generous 219 headroom to build top) with the probability
 * {@code rng(1000000) < min(100, mSize)}; ③ the 256 m interference dilution :133-135 via
 * the static {@link #ALL_LIGHTNING_RODS} table (non-structure-sharing, pure probability);
 * ④ the re-scan cadence :127 — {@code mSize < 100 && aTimer % 1200 == 300 && rain}, the
 * forced-reset arm that re-discovers newly placed rod blocks (the checkStructure caching
 * template rides the ported base :145-154 verbatim, the force flag IS the :127 argument).
 * mSize stays a BYTE verbatim (the 127 wrap self-stop is upstream-homomorphic — decision
 * height-gate); it is NOT persisted, the onTickFirst forced check re-derives it.
 *
 * <p><b>The energy face (:128-129 + :183-192)</b>: formed machines with
 * {@code mEnergy >= 32768} push {@code 16 A x 32768 EU} out the BOTTOM via
 * {@link ITileEntityEnergy.Util#emitEnergyToSide} (the adjacency seam — the port's
 * neighbour-lookup externalization), then pay {@code used * 32768}. Sub-packet energy is
 * TRASHED to 0 and the strike gate takes over (the :130-132 else form). The capacitor
 * interface half (ITileEntityEnergyDataCapacitor, :185-190 getEnergyStored/getEnergyCapacity)
 * is the cut ADR-D1 subsystem (the interface note — no port surface), while the NETWORK
 * face ports verbatim: output recommended 2048 / min 1024 / max 4096, and the :184
 * {@code isEnergyEmittingTo → isEnergyType(..., F)} quirk that reports NOT-emitting to
 * conductors (the tick emission bypasses it, exactly like upstream).
 *
 * <p><b>The strike (:132-148)</b>: all four gates pure (mSize/y/rng/weather), the rng
 * through the {@link #rng} seam (the boiler form) so the offline tests drive every branch;
 * the interference count walks {@link #ALL_LIGHTNING_RODS} for same-world rods within
 * 256 m x/z with mSize &gt; 0; the sky-clearance loop (:137-142) walks
 * {@code yCoord + mSize + 5} up to {@code level.getHeight()} (the 256→384 same-construct
 * "world top" call); the strike itself spawns a vanilla {@link LightningBolt} with
 * {@code setVisualOnly(false)} (the :144 addWeatherEffect counterpart — modern lightning
 * is a regular entity, the declared substitution) at {@code yCoord + mSize + 4} through
 * {@link #spawnStrike} (the offline seam: the tests record, never constructing an Entity),
 * and the charge lands as {@code mEnergy = mCapacity}.
 *
 * <p><b>The capacity (decision capacity)</b>: {@code 18000 * VREC[6] = 18000 * 32768 =
 * 589,824,000 EU} per strike — the Loader row's NBT_CAPACITY
 * (Loader_MultiTileEntities.java:1282, VREC[6] = CS.java:149) as a COMPILE-TIME constant,
 * not NBT (the upstream read-only NBT default folds away: :61 read with no :66-69 write
 * back). Only {@code mEnergy} persists (:68).
 *
 * <p><b>The static table (three lifecycle points, the :157-178 port)</b>: server
 * onTickFirst registers (the :157-159 + :163-166 validate pair folds into the ported
 * first-tick), setRemoved and onChunkUnloaded de-register (the :169-172 invalidate +
 * :175-178 onChunkUnload pair). The list is single-server-thread (the interface note).
 *
 * <p>Cuts: the placement sides (:180-181 SIDE_BOTTOM/SIDES_BOTTOM — the ported controller
 * blocks carry no placement-face gate, the CokeOven/Boiler convention), canDrop :194 (no
 * inventory), the F3-H tooltip channel (the 9 structure keys live in lang, the tooltip
 * face rides the cut addToolTips channel like every prior machine).
 */
public class TileEntityLightningRod extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy {

	/** The NBT key (the W3 generator spelling convention; only mEnergy persists, upstream :68). */
	public static final String NBT_ENERGY = "gt.energy";

	/** VREC[6] (CS.java:149) — the EU per packet and per amp, the Lightning Rod's tier. */
	public static final long VOLTAGE = 32768;
	/** The 16-amp offer (upstream :129 literal). */
	public static final long AMPS = 16;
	/** 18000 * VREC[6] = 589,824,000 EU per strike (Loader_MultiTileEntities.java:1282 NBT_CAPACITY, decision capacity). */
	public static final long CAPACITY = 18000 * VOLTAGE;
	/** The strike-probability per-gate constant :132 (rng(1000000) &lt; min(100, mSize)). */
	public static final int STRIKE_RNG_SCALE = 1000000;
	/** The absolute-altitude tip gate :132/:95 (kept verbatim across the height change, decision height-gate). */
	public static final int TIP_MIN_Y = 100;
	/** The interference radius :134 (|dx| &lt; 256 && |dz| &lt; 256). */
	public static final int INTERFERENCE_RADIUS = 256;
	/** The re-scan cadence :127 (mSize &lt; 100 && aTimer % 1200 == 300 && rain). */
	public static final int RESCAN_PERIOD = 1200;
	/** The byte-length cap of the re-scan arm :127 (mSize &lt; 100; the byte wrap self-stop is upstream-homomorphic). */
	public static final int RESCAN_SIZE_LIMIT = 100;

	/** The GT6 side byte for DOWN (CS.java:671 SIDE_BOTTOM == Direction.get3DDataValue, the boiler table pin). */
	public static final byte SIDE_BOTTOM = 0;

	/** The stored charge (:51, NBT_ENERGY :68). */
	public long mEnergy = 0;
	/**
	 * The charge ceiling (:51) — the decision-capacity field form (the boiler :179 default
	 * shape): the compile-time constant, never NBT, no re-derivation (the single Loader row
	 * pins it; upstream's :61 NBT read had no :66-69 write-back).
	 */
	public final long mCapacity = CAPACITY;
	/** The probed pillar length (:52, NOT persisted — the onTickFirst forced check re-derives). */
	public byte mSize = 0;
	/** The emitted energy type (:53; the registration NBT_ENERGY_EMITTED row value, TD.Energy.EU). */
	public final TagData mEnergyTypeEmitted = TD.Energy.EU;

	/** The 256 m interference table (:55) — registration/de-registration rides the three lifecycle points below. */
	public static final List<TileEntityLightningRod> ALL_LIGHTNING_RODS = new ArrayList<>();

	// ---------------------------------------------------------------------------
	// the offline seams (the boiler/engine form: rng + adjacency + the strike spawn)
	// ---------------------------------------------------------------------------

	/** The live rng (:132/:135 — the world Random; the generator rng seam pattern, the boiler :200-213 form). */
	protected int rng(int aRange) {
		if (aRange <= 0) return 0;
		if (mRngOverride != null) return mRngOverride.getAsInt();
		return hasLevel() ? getLevel().random.nextInt(aRange) : 0;
	}

	@Nullable
	private IntSupplier mRngOverride = null;

	/** The offline rng seam. */
	public void setRngOverride(@Nullable IntSupplier aRng) {
		mRngOverride = aRng;
	}

	@Nullable
	private IEnergyAdjacency mAdjacencyOverride = null;

	/** The offline adjacency seam (the GTSteamEngineBlockEntity :434 form). */
	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	/** The bottom-face neighbour resolution (the engine :438-449 form, side hard-wired to the :129 emission face). */
	private IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (!(tNeighbor instanceof ITileEntityEnergy)) return null;
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	/**
	 * The strike spawn (:144 {@code addWeatherEffect(new EntityLightningBolt(...))}) — a
	 * vanilla {@link LightningBolt} with {@code setVisualOnly(false)} (vanilla
	 * LightningBolt.java:49; modern lightning is a regular entity, the declared
	 * substitution for addWeatherEffect). The offline seam: the tests override to record —
	 * no Entity is ever constructed offline (the offline-test discipline).
	 */
	protected void spawnStrike(int aX, int aY, int aZ) {
		Level tLevel = getLevel();
		LightningBolt tBolt = EntityType.LIGHTNING_BOLT.create(tLevel);
		if (tBolt == null || tLevel == null) return;
		tBolt.moveTo(aX, aY, aZ);
		tBolt.setVisualOnly(false);
		tLevel.addFreshEntity(tBolt);
	}

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + block-carrier shape)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public TileEntityLightningRod(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime.
	 */
	public TileEntityLightningRod(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTMultiBlocks.LIGHTNING_ROD_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_lightning_rod"; // BET registry path mirrors it
	}

	/** The Tungsten Wall part block (upstream part id 18004, Loader :1151). */
	protected Block getWallBlock() {
		return GTMultiBlocks.lightningRodPartBlock("machine_wall_tungsten");
	}

	/** The Large Niobium-Titanium Coil part block (upstream part id 18041, Loader :1168). */
	protected Block getCoilBlock() {
		return GTMultiBlocks.lightningRodPartBlock("niobium_titanium_coil");
	}

	/** The Lightning Rod pillar block (upstream part id 18104, Loader :1179). */
	protected Block getRodBlock() {
		return GTMultiBlocks.lightningRodPartBlock("lightning_rod");
	}

	// ---------------------------------------------------------------------------
	// the structure (:72-86 verbatim + the probe-unloaded pre-gate)
	// ---------------------------------------------------------------------------

	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // the pre-gate needs the level (the boiler :288 form)
		BlockPos tPos = getBlockPos();
		// decision probe-unloaded — the base FOUR corner columns (the boiler :289-290 form;
		// full-height chunk columns, so the corners cover the whole 3x3x5 base). Unloaded
		// keeps the last verdict; nothing here ever force-loads a chunk.
		if (!getLevel().isLoaded(new BlockPos(tPos.getX() - 1, tPos.getY(), tPos.getZ() - 1))
				|| !getLevel().isLoaded(new BlockPos(tPos.getX() + 1, tPos.getY(), tPos.getZ() - 1))
				|| !getLevel().isLoaded(new BlockPos(tPos.getX() - 1, tPos.getY(), tPos.getZ() + 1))
				|| !getLevel().isLoaded(new BlockPos(tPos.getX() + 1, tPos.getY(), tPos.getZ() + 1))) {
			return mStructureOkay;
		}

		boolean tSuccess = true;
		mSize = 0; // :74

		// :76-82 — the 3x3x5 base, walls / coils alternating (the controller's own cell at
		// (0,0,0) passes through the checkAndSetTarget self-cell arm :49)
		for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 0, j, getWallBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 1, j, getCoilBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 2, j, getWallBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 3, j, getCoilBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 4, j, getWallBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
		}

		// :84 — the pillar probe, verbatim: runs ONLY on a held base, counts consecutive rod
		// blocks; mSize == 0 still forms. The SET triple is dead here by the ±1 door (the
		// pillar sits |dy| >= 2 from any click) — the form-set decision, zero special-casing.
		if (tSuccess) while (ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, 0, 5 + mSize, 0, getRodBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) mSize++;
		return tSuccess;
	}

	/** Upstream :119 verbatim — the 3x3 box below y+5, then the 1x1 column to y+mSize+4. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tPos = getBlockPos();
		return aY >= tPos.getY() && aX >= tPos.getX() - 1 && aZ >= tPos.getZ() - 1
				&& aX <= tPos.getX() + 1 && aZ <= tPos.getZ() + 1
				&& (aY < tPos.getY() + 5 || (aX == tPos.getX() && aZ == tPos.getZ() && aY <= tPos.getY() + mSize + 4));
	}

	// the declared structure pattern stays NULL (the interface default): the unbounded
	// pillar probe is NOT expressible in the pattern DSL (GTMultiBlockPattern.java:77-81)
	// and never binds one (decision family-seam; checker form short-circuits at the null).

	// ---------------------------------------------------------------------------
	// the tick (:122-154 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide || !hasLevel()) return;

		// :126-127 — the forced re-scan arm: the variable rod length needs forcing while it
		// can still grow, because newly placed rod blocks cause no multiblock updates.
		if (checkStructure(mSize < RESCAN_SIZE_LIMIT && aTimer % RESCAN_PERIOD == 300
				&& (getLevel().isRaining() || getLevel().isThundering()))) {
			if (mEnergy >= VOLTAGE) {
				// :128-129 — the 16 A x 32768 EU push out the bottom, then pay for what moved
				mEnergy -= Math.max(1, ITileEntityEnergy.Util.emitEnergyToSide(mEnergyTypeEmitted, SIDE_BOTTOM, VOLTAGE, AMPS, this, adjacency())) * VOLTAGE;
			} else {
				mEnergy = 0; // :131 — the sub-packet remainder never accumulates
				// :132 — the four strike gates (mSize / absolute tip altitude / probability /
				// weather), all rng through the seam
				if (mSize > 0 && tY() + mSize >= TIP_MIN_Y && rng(STRIKE_RNG_SCALE) < Math.min(100, mSize)
						&& (getLevel().isThundering() || (getLevel().isRaining() && rng(10) == 0))) {
					// :133-135 — the 256 m interference dilution over the static table
					int tCount = 1;
					for (TileEntityLightningRod tRod : ALL_LIGHTNING_RODS) {
						if (tRod != this && tRod.mSize > 0 && tRod.getLevel() == getLevel()
								&& Math.abs(tRod.tX() - tX()) < INTERFERENCE_RADIUS
								&& Math.abs(tRod.tZ() - tZ()) < INTERFERENCE_RADIUS) tCount++;
					}
					if (rng(tCount) == 0) { // :135
						// :136-142 — the sky-clearance loop up to the world top
						boolean temp = true;
						for (int i = tY() + mSize + 5, j = getLevel().getHeight(); i < j; i++) {
							if (!getLevel().getBlockState(new BlockPos(tX(), i, tZ())).isAir()) {
								temp = false;
								break;
							}
						}
						if (temp) { // :143
							spawnStrike(tX(), tY() + mSize + 4, tZ()); // :144 (the setVisualOnly(false) bolt)
							mEnergy = mCapacity; // :145
						}
					}
				}
			}
		} else {
			mEnergy = 0; // :151 — a broken structure bleeds dry
		}
	}

	// the :157-178 static-table lifecycle, three points (the card port note: upstream's
	// validate()+onTickFirst2 registration pair folds into the ported first-tick hook)
	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide && !ALL_LIGHTNING_RODS.contains(this)) ALL_LIGHTNING_RODS.add(this); // :157-159
	}

	@Override
	public void setRemoved() {
		if (isServerSide()) ALL_LIGHTNING_RODS.remove(this); // :169-172 invalidate
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		if (isServerSide()) ALL_LIGHTNING_RODS.remove(this); // :175-178
		super.onChunkUnloaded();
	}

	// the placement sides (:180-181) ride the ported controller-block convention: no
	// placement-face gate (the CokeOven/Boiler form) — declared cut.

	// ---------------------------------------------------------------------------
	// the energy face (:183-192 verbatim, the capacitor half cut per ADR-D1)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == mEnergyTypeEmitted; // :183
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, false); // :184 VERBATIM — always false (the network-facing quirk; the :129 tick emission bypasses it)
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return 2048; // :186
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return 1024; // :187
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return 4096; // :188
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeEmitted.AS_LIST; // :191
	}

	// the :185/:189-192 capacitor reporting is the cut ADR-D1 subsystem (no port surface)

	// ---------------------------------------------------------------------------
	// NBT (:57-69 — only mEnergy persists; mCapacity is the compile-time constant, mSize
	// re-derives on the onTickFirst forced check)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :68
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :60
	}

	// coord shorthands (the 1.7.10 xCoord/yCoord/zCoord reads the verbatim lines cite)
	private int tX() { return getBlockPos().getX(); }
	private int tY() { return getBlockPos().getY(); }
	private int tZ() { return getBlockPos().getZ(); }
}
