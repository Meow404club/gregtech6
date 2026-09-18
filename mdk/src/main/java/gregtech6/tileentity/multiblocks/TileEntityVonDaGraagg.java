package gregtech6.tileentity.multiblocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.registry.GTMultiBlocks;

/**
 * The Von da Graagg multiblock controller (task p31-graagg) — the 1.20.1/1.21.1 port of
 * gregtech/tileentity/multiblocks/MultiTileEntityVonDaGraagg.java over
 * {@link TileEntityBase10MultiBlockBase} (Loader_MultiTileEntities.java:1280: meta 17996,
 * "Von da Graagg Generator", MT.SteelGalvanized, hardness 6.0 == resistance 6.0,
 * NBT_TEXTURE "vondagraagg", NBT_ENERGY_ACCEPTED TD.Energy.EU). A "Generator" in name
 * only: an EU-CONSUMING mob-suppression tower (the upstream tooltip :99 "Prevents Mob
 * Spawns except on Mossy Cobblestone, Range depends on Input").
 *
 * <p><b>The structure (:66-93 verbatim)</b>, CONTROLLER-anchored (upstream walks straight
 * off xCoord/yCoord/zCoord with no facing displacement — the crucible anchor, so
 * {@link #patternWalkFacing()} returns the ZERO-OFFSET facing like
 * TileEntityCrucible.java:245):
 * <ul>
 * <li>the cornerless 5x5x2 base (41 Dense Galvanized Steel Walls + the controller as the
 *     bottom-centre cell): every (i, j) of [-2, 2] with {@code |i*j| < 4} at BOTH dy 0 and
 *     dy 1 — 21 cells per layer, the four |i|=|j|=2 corners cut by the product gate —
 *     walked in the upstream :71-74 interleave (cell (i, 0, j) then (i, 1, j) per (i, j));
 *     every cell usage {@link MultiBlockPartBlockEntity#ONLY_ENERGY_IN}, design 0 (the
 *     :72-73 call columns);</li>
 * <li>the 5m coil pole: (0, +2..+6, 0) Large Copper Coils (:75-79, 5 cells, NOTHING);</li>
 * <li>the top box of Dense Steel Walls (:81-89): (0, +7, 0) + the y+6 cornerless ring
 *     (8 cells) + the y+5 and y+7 crosses ({@code i*j == 0}, 4+4 cells) — 17 cells,
 *     NOTHING.</li>
 * </ul>
 * All three part ids (18028/18040/18029) are EXISTING GTMultiBlocks rows — zero new part
 * blocks (the p29-w3-nbtdesign-parts census).
 *
 * <p><b>The behavior (:141-152)</b>: every server tick
 * {@code mCurrentRange = bind8(mStructureOkay ? min(mEnergy, 4096) / 16 : 0)} — the pure
 * seam {@link #updateRange}; the byte-bind ceiling is 255 (UT.java:1560 — the "256m" of
 * the tooltip/gibbl faces is display-only, the LIVE cap is 255) and the range DRAINS
 * {@code 4096 EU/t} unconditionally afterwards (:150 — an unformed tower bleeds dry
 * verbatim). The stored energy persists (NBT_ENERGY, the :56 field); the accepted type is
 * the EU registration column read-only (final, the Lightning Rod mEnergyTypeEmitted form).
 *
 * <p><b>The energy face (:167-178)</b>: a pure EU capacitor-demand sink —
 * {@link #doInject} accumulates unconditionally and reports the whole packet used
 * (:170), {@link #getEnergyDemanded} reports {@code 4096 - mEnergy} (:171), the input
 * window is recommended 2048 / min 256 / max 4096 (:172-174 — the min is a LITERAL 256,
 * not the Root's Rec/2 default). The capacitor reporting half
 * (getEnergyStored/getEnergyCapacity :175-176, isEnergyCapacitorType :169) is the cut
 * ADR-D1 subsystem (the Lightning Rod precedent — no port surface); the gibbl/progress
 * display faces (:162-165) and the magnifying-glass read (:155-157) ride the cut
 * addToolTips channel.
 *
 * <p><b>The suppression face (:129-139)</b>: the upstream rides
 * {@code GT_API_Proxy.MOB_SPAWN_INHIBITORS} + {@code LivingSpawnEvent.CheckSpawn}, which
 * has NO modern counterpart (Forge 1.20.1 deleted it — the surviving
 * MobSpawnEvent.PositionCheck covers natural attempts only, and /summon fires no
 * spawn-check event at all). The port rides {@code EntityJoinLevelEvent} (the only
 * universal join gate, both loaders) through {@link GTGraaggSpawnListener}:
 * server-side, freshly-spawned ({@code !loadedFromDisk}) Mobs only — covers all
 * join-path spawns incl. spawner/egg/breeding, while chunk-reload re-joins pass
 * (upstream CheckSpawn covered natural attempts only; declared deviation,
 * coordinator-approved task p31-graagg ruling). {@link #inhibitsSpawn} is the :130-138
 * port: range 0 / wrong dimension / outside the Chebyshev x-z radius passes, then the
 * ±5 vertical scan at the spawn column exempts mossy cobblestone — the vanilla block AND
 * the GT stone MCOBL variant (upstream {@code BlockStones.MCOBL} meta 2, BlockStones.java:73).
 * The registration table {@link #ALL_GRAAGGS} rides the Lightning Rod three-lifecycle
 * form (onTickFirst add, setRemoved/onChunkUnloaded remove).
 */
public class TileEntityVonDaGraagg extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy {

	/** The NBT key (the W3 generator spelling convention — only mEnergy persists, upstream :56). */
	public static final String NBT_ENERGY = "gt.energy";

	/**
	 * The live range NBT mirror — upstream did NOT persist mCurrentRange; the port saves it
	 * as the ops/RCON read face (the headless machines' data-get convention). Re-derived
	 * every server tick, so a stale saved value self-corrects.
	 */
	public static final String NBT_RANGE = "gt.range";

	/** The energy window cap AND the per-tick drain (:149-150 — min(mEnergy, 4096) and -= 4096). */
	public static final long CAP = 4096;
	/** The range divisor (:149 — min(mEnergy, 4096) / 16). */
	public static final long RANGE_DIVISOR = 16;
	/** The byte-bind ceiling of UT.Code.bind8 (UT.java:1560) — the LIVE range cap (256 is display-only). */
	public static final int RANGE_CAP = 255;
	/** The input window (:172-174 literals — the min is NOT the Root Rec/2 default). */
	public static final long INPUT_RECOMMENDED = 2048, INPUT_MIN = 256, INPUT_MAX = 4096;
	/** The mossy-exemption vertical scan half-width (:133 — for i in [-5, 5]). */
	public static final int EXEMPTION_SCAN = 5;

	/** The accepted energy type (the :1280 registration column — read-only, final). */
	public final TagData mEnergyTypeAccepted = TD.Energy.EU;

	/** The stored energy (:56). Persists. */
	public long mEnergy = 0;
	/** The live suppression radius in blocks (:56; recomputed every server tick, NOT persisted). */
	public int mCurrentRange = 0;

	/** The suppression table (:146 MOB_SPAWN_INHIBITORS port) — the Lightning Rod three-lifecycle form. */
	public static final List<TileEntityVonDaGraagg> ALL_GRAAGGS = new ArrayList<>();

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + the offline test seam, the Lightning Rod form)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public TileEntityVonDaGraagg(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the frozen-registry seam). */
	public TileEntityVonDaGraagg(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTMultiBlocks.VON_DA_GRAAGG_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_von_da_graagg"; // BET registry path mirrors it
	}

	/** The base wall (upstream part id 18028, Loader :1162 family — Dense Galvanized Steel Wall). */
	protected Block getBaseWallBlock() {
		return GTMultiBlocks.anyPartBlock("dense_wall_galvanized_steel");
	}

	/** The coil pole block (upstream part id 18040, Loader :1170 — Large Copper Coil). */
	protected Block getCoilBlock() {
		return GTMultiBlocks.anyPartBlock("large_copper_coil");
	}

	/** The top-box block (upstream part id 18029, Loader :1163 — Dense Steel Wall). */
	protected Block getTopWallBlock() {
		return GTMultiBlocks.anyPartBlock("dense_wall_steel");
	}

	// ---------------------------------------------------------------------------
	// the structure (:66-93 verbatim — controller-anchored, the crucible anchor)
	// ---------------------------------------------------------------------------

	@Override
	public byte patternWalkFacing() {
		return 0; // the OFF[facing] tables' zero entry — the upstream walk has no facing displacement
	}

	/** The lazy structure pattern (the implosion lazy-override shape). */
	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			Block tBase = getBaseWallBlock(), tCoil = getCoilBlock(), tTop = getTopWallBlock();
			// :71-74 — the cornerless 5x5x2 base, (i, 0, j) then (i, 1, j) per (i, j) interleave
			for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (Math.abs(i * j) < 4) {
				tBuilder.formingPart(i, 0, j, tBase, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 0);
				tBuilder.formingPart(i, 1, j, tBase, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 0);
			}
			// :75-79 — the 5m coil pole
			for (int h = 2; h <= 6; h++) tBuilder.formingPart(0, h, 0, tCoil, MultiBlockPartBlockEntity.NOTHING, 0);
			// :81 — the box top centre
			tBuilder.formingPart(0, 7, 0, tTop, MultiBlockPartBlockEntity.NOTHING, 0);
			// :82-89 — the y+6 cornerless ring, then the y+5 / y+7 crosses (i*j == 0)
			for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) if (i != 0 || j != 0) {
				tBuilder.formingPart(i, 6, j, tTop, MultiBlockPartBlockEntity.NOTHING, 0);
				if (i * j == 0) {
					tBuilder.formingPart(i, 5, j, tTop, MultiBlockPartBlockEntity.NOTHING, 0);
					tBuilder.formingPart(i, 7, j, tTop, MultiBlockPartBlockEntity.NOTHING, 0);
				}
			}
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	/** Upstream :66-93 through the shared checker (the implosion :150-157 form). */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay;
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				this, patternWalkFacing(), aCoordinates, aPlayer, aInventory);
		if (tVerdict.unloaded) return mStructureOkay; // :92
		return tVerdict.formed;
	}

	/** Upstream :114-117 verbatim — the controller-anchored box (x/z ±2, y .. y+8). */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tPos = getBlockPos();
		return aX >= tPos.getX() - 2 && aY >= tPos.getY() && aZ >= tPos.getZ() - 2
				&& aX <= tPos.getX() + 2 && aY <= tPos.getY() + 8 && aZ <= tPos.getZ() + 2;
	}

	// ---------------------------------------------------------------------------
	// the tick (:141-152) and the suppression table lifecycle
	// ---------------------------------------------------------------------------

	/** The pure range formula (:149) — bind8(mStructureOkay ? min(mEnergy, CAP) / 16 : 0). */
	static int updateRange(long aEnergy, boolean aStructureOkay) {
		if (!aStructureOkay) return 0;
		return (int)Math.min(RANGE_CAP, Math.max(0, Math.min(aEnergy, CAP) / RANGE_DIVISOR));
	}

	/** The pure drain (:150) — 4096/t, floored at zero. */
	static long drain(long aEnergy) {
		return Math.max(0, aEnergy - CAP);
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide) return;
		mCurrentRange = updateRange(mEnergy, mStructureOkay); // :149
		mEnergy = drain(mEnergy); // :150
	}

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide && !ALL_GRAAGGS.contains(this)) ALL_GRAAGGS.add(this); // :145-148
	}

	@Override
	public void setRemoved() {
		if (isServerSide()) ALL_GRAAGGS.remove(this); // :120/:125 unregister
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		if (isServerSide()) ALL_GRAAGGS.remove(this); // :125 onCoordinateChange pair
		super.onChunkUnloaded();
	}

	// ---------------------------------------------------------------------------
	// the suppression face (:129-139)
	// ---------------------------------------------------------------------------

	/**
	 * The suppression decision over the whole live table — the listener adapter seam.
	 * {@code aLoadedFromDisk} is the chunk-reload pass: re-joins from disk are NEVER
	 * suppressed (the coordinator acceptance arm — no chunk-reload apocalypse).
	 */
	public static boolean shouldSuppress(Level aLevel, BlockPos aPos, boolean aLoadedFromDisk) {
		if (aLoadedFromDisk) return false;
		if (ALL_GRAAGGS.isEmpty()) return false;
		for (TileEntityVonDaGraagg tGraagg : ALL_GRAAGGS) {
			if (tGraagg.inhibitsSpawn(aLevel, aPos)) return true;
		}
		return false;
	}

	/** Upstream :130-138 verbatim — dimension gate, Chebyshev x-z radius, the mossy column exemption. */
	public boolean inhibitsSpawn(Level aWorld, BlockPos aPos) {
		if (getLevel() == null || mCurrentRange <= 0 || aWorld != getLevel()
				|| Math.abs(aPos.getX() - getBlockPos().getX()) > mCurrentRange
				|| Math.abs(aPos.getZ() - getBlockPos().getZ()) > mCurrentRange) return false; // :131
		// :132-137 — allow spawns on Mossy Cobblestone (vanilla block OR the GT stone MCOBL variant)
		for (int i = -EXEMPTION_SCAN; i <= EXEMPTION_SCAN; i++) {
			BlockState tState = aWorld.getBlockState(aPos.offset(0, i, 0));
			if (tState.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE)) return false;
			if (tState.getBlock() instanceof GTStoneBlock tStone && tStone.variant == StoneVariant.MCOBL) return false;
		}
		return true; // :138
	}

	// ---------------------------------------------------------------------------
	// the energy face (:167-178 — the capacitor/display half is the cut ADR-D1 subsystem)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :167
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :177
	}

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aDoInject) mEnergy += Math.abs(aAmount * aSize); // :170 — accepts the whole packet (the wires demand-gate via getEnergyDemanded)
		return aAmount;
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return CAP - mEnergy; // :171
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return INPUT_RECOMMENDED; // :172
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return INPUT_MIN; // :173 — the literal, NOT the Root Rec/2 default
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return INPUT_MAX; // :174
	}

	// ---------------------------------------------------------------------------
	// NBT (:56 — only mEnergy persists; mCurrentRange re-derives on the first tick)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy);
		aNBT.putInt(NBT_RANGE, mCurrentRange); // the ops face — see NBT_RANGE
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY);
		if (aNBT.contains(NBT_RANGE, Tag.TAG_ANY_NUMERIC)) mCurrentRange = aNBT.getInt(NBT_RANGE);
	}

	// the placement sides (:159-160 SIDE_FRONT/SIDES_BOTTOM) ride the ported
	// controller-block convention: no placement-face gate (the CokeOven/Boiler form).
	// The :96-99 structure tooltip keys and the :102-111 addToolTips face ride the cut
	// tooltip channel (the Lightning Rod precedent); the :96-99 strings live in the
	// upstream lang dump, not the port's lang table (the structure keys are cut with it).
	// The controller crafting row "CSC"/"PMP"/"CEC" is CUT — the GTMultiBlocks ledger.
	// The :119-120 mHasToAddToList lazy-registration flag folds into the onTickFirst add
	// (the Lightning Rod lifecycle precedent); the :122-127 onCoordinateChange remove
	// folds into setRemoved/onChunkUnloaded (the set-stable contains() guard).
}
