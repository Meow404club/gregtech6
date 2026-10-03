package gregtech6.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.multiblock.GTLargeBoilerBlock;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6Logistics;
import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.GT6HeatExchangerBlockEntity;
import gregtech6.tileentity.multiblocks.GTGasTurbineBlockEntity;
import gregtech6.tileentity.multiblocks.GTLargeDynamoBlockEntity;
import gregtech6.tileentity.multiblocks.GTSteamTurbineBlockEntity;
import gregtech6.tileentity.multiblocks.GTTankValveBlockEntity;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;
import gregtech6.tileentity.multiblocks.TileEntityFusionReactor;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;
import gregtech6.tileentity.multiblocks.TileEntityVonDaGraagg;

/**
 * The multiblock preview registry (task multiblock-preview-infra): ONE static
 * item→pattern table — the {@link GT6RecipeMapIcons} hand-curated-row precedent — plus the
 * viewer-neutral model seam (pattern → schema blocks / material counts) that BOTH viewer
 * legs consume. The GTCEu Modern shape this modernizes is
 * {@code MultiblockInfoEmiCategory.registerDisplays} (machines-filter loop) collapsed into
 * this port's explicit table, because the port's multiblocks bind their shape on the
 * TileEntity ({@code getStructurePattern()}) instead of a definition registry — the table
 * row IS the machine census (a data card appends rows; every viewer face picks them up
 * automatically).
 *
 * <p><b>Growth = data batches (research.r11-mbpreview-batches).</b> First version = the
 * Coke Oven; batch A (task mbpreview-data-a-thermal) appended the thermal pair — the five
 * Large Boiler tiers (upstream Loader_MultiTileEntities.java:1248-1252) and the eight
 * Large Crucible tiers (:1270-1277), rows built off the port's own registration ladders
 * ({@link GTMultiBlocks#LARGE_BOILER_ROWS} / {@link GT6Crucibles#CRUCIBLE_ROWS} — the
 * upstream line order lives in exactly one place). Later batches tail-append their
 * family loop inside {@link #buildEntries()} after the previous batch's; the census test
 * re-pins the full name list per batch. Predicate-only cells (no {@code partBlock}) still
 * have no general display ruling — the boiler row is NOT one: its binding is
 * predicate-shaped, so the row re-stamps the display identity from its own part list
 * ({@link #withDisplayBlocks}) before tabling it; the per-machine ruling for genuinely
 * predicate-only machines belongs to the data card that tables them (research.
 * multiblock-preview risk column).
 *
 * <p><b>Batch D2 (task mbpreview-data-d2-authored) — the authored shapes + two user
 * rulings.</b> The four no-pattern machines join as TABLE-SIDE display declarations
 * (the bedrockDrillShape doctrine — the BE bindings stay null and untouched): the
 * massfab/fusion/logistics quota cores and the variable-length rod pillar are exactly
 * what the declarative API does not express, so the shapes transcribe the upstream
 * walks at the display layer. Ruling A1: {@link Entry#controllerCell} — the seam paints
 * the controller on the row's TRUE seat (upstream-evidenced per machine, the census
 * test's pin table), falling back to the anchor law when undeclared; six already-tabled
 * machines were retro-declared (the crucible/HEX/Graagg/drill seats were mispainted,
 * the implosion/tower seats were already right and are now explicit). Ruling ③ (the
 * r11 Q3 degrade): the predicate quota cells render their FIRST candidate
 * ({@link #withDisplayBlocks}) and the tooltip lists them all
 * ({@link #candidatePaths}/{@link #cellCandidateBlocks}).
 *
 * <p>Offline-test contract: loading this class never touches a registry (the
 * {@code Supplier}s are lazy), so the census test runs bare-JVM; the model seam is pure
 * (fixture patterns over vanilla blocks) — the SAME functions the widget feeds
 * {@code MapSchema} with, asserted offline at the layer that needs no client.
 */
public final class GT6MultiblockPreviews {

	/**
	 * The display facing (north = 2, {@code Direction.NORTH.get3DDataValue()}) — the
	 * anchor arithmetic's rotation input. The GT6 anchor convention ("Main Block centered
	 * on Side and facing outwards") puts the CONTROLLER cell at centre-relative
	 * {@code -anchorOffset(facing)}; rendering one canonical facing keeps the page
	 * deterministic (no rotation buttons in v1).
	 */
	public static final byte DISPLAY_FACING = 2;

	/** The preview page size both viewer legs declare (the GTCEu 200x180 page shape). */
	public static final int PAGE_WIDTH = 200;
	public static final int PAGE_HEIGHT = 180;

	/** The category uid path — {@code gt6:multiblock_preview} on both legs (JEI RecipeType + EMI category id). */
	public static final String UID_PATH = "multiblock_preview";

	/** The shared category title key — the producer half lives in GT6EnUs/GT6ZhCn. */
	public static final String TITLE_KEY = "gt6.jei.multiblock_preview";

	/**
	 * One table row: the registry name doubles as the offline census key and the recipe-id
	 * suffix; the item is the controller's BlockItem (the U anchor); the pattern supplier
	 * re-reads the machine's own binding so the page and the server check share ONE
	 * declaration (the pattern-checker red line — never a transcribed copy). NO text
	 * description: the 3D preview speaks for itself (task mbpreview-shell-replicate —
	 * the former description face retired with the text-info pages).
	 *
	 * <p><b>{@code controllerCell} — user ruling A1 (task mbpreview-data-d2-authored).</b>
	 * The controller-anchored display: the cell the seam paints the CONTROLLER block on.
	 * Upstream patterns never mark their controller cell, so the seam's anchor law
	 * ({@code -anchorOffset}) was GUESSING — and guessed wrong wherever the true seat
	 * deviates (the zero-offset Graagg/drill walks, the undeclared crucible/HEX bottom
	 * centres, the 2-cells-behind-the-face fusion/massfab/logistics cores). A declared
	 * cell overrides the guess — and paints ADDITIVELY when the upstream never declares
	 * the seat at all; {@code null} keeps the anchor-law fallback (the rows whose seat
	 * IS {@code -anchor}: the coke oven, the converters, the towers, the tank valves).
	 * Per-row seat evidence rides the census test.
	 */
	public record Entry(String name, Supplier<Item> item, Supplier<GTMultiBlockPattern> pattern,
			@Nullable BlockPos controllerCell) {

		/** The anchor-law form — the fallback every pre-D2 row keeps (no seat override). */
		public Entry(String name, Supplier<Item> item, Supplier<GTMultiBlockPattern> pattern) {
			this(name, item, pattern, null);
		}

		/**
		 * The controller {@link Block} — the seat cell is painted with it (the GT6
		 * pattern marks every structural cell with the part block, the controller cell
		 * included — the research card's coke-oven special case, generalized to the table:
		 * whichever cell the seat ruling lands on IS the controller for every GT6 shape).
		 */
		@Nullable
		public Block controllerBlock() {
			return item().get() instanceof BlockItem tBlockItem ? tBlockItem.getBlock() : null;
		}
	}

	/**
	 * The table. Every row's pattern supplier builds a THROWAWAY TileEntity —
	 * {@code getStructurePattern()} is a pure lazy declaration (no level access), the
	 * cheapest honest reuse of the one binding — and the suppliers stay lazy so the
	 * offline census never touches a registry.
	 */
	private static final List<Entry> ENTRIES = buildEntries();

	/**
	 * The row builder — the data-batch tail-append point (the batch doctrine, class doc):
	 * each batch adds its family loop BELOW the previous batch's, rows in the upstream
	 * registration order (the port's ladder lists are the upstream line order), and the
	 * census test re-pins the full name list per batch.
	 */
	private static List<Entry> buildEntries() {
		List<Entry> tRows = new ArrayList<>();
		tRows.add(new Entry("multiblock_coke_oven", GTMultiBlocks.COKE_OVEN_ITEM,
				() -> new TileEntityCokeOven(BlockPos.ZERO, Blocks.AIR.defaultBlockState()).getStructurePattern()));
		// --- batch A (mbpreview-data-a-thermal): the thermal pair -----------------------
		// the five Large Boiler tiers (upstream Loader_MultiTileEntities.java:1248-1252):
		// the throwaway BE carries its tier wall through the VARIANT STATE (the
		// construction-time-state doctrine — getWallBlock reads the row off the
		// GTLargeBoilerBlock state, TileEntityLargeBoiler.java:256-262); the binding is
		// predicate-shaped so the display identity re-stamps from the row's own parts
		for (GTMultiBlocks.LargeBoilerRow tRow : GTMultiBlocks.LARGE_BOILER_ROWS) {
			tRows.add(new Entry(tRow.path(), GTMultiBlocks.LARGE_BOILER_ITEMS_BY_PATH.get(tRow.path()),
					() -> boilerPreview(GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH.get(tRow.path()).get())));
		}
		// the eight Large Crucible tiers (upstream :1270-1277): the binding carries the
		// wall form directly (formingPart — zero re-stamp); the throwaway BE carries its
		// row's wall through the getWallBlock override (the level-gated production read,
		// TileEntityCrucible.java:294-299, answers a never-placed BE the fixture — the
		// override drives the SAME wallBlockOf resolution the placed controller does).
		// D2 seat (ruling A1): the walk SKIPS the bottom centre (MultiTileEntityCrucible
		// :118) — "Main at Bottom-Center" (:140) — the undeclared seat (0,0,0) paints
		// additively; the anchor law's (0,0,-1) guess was a ring wall
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.get(tRow.path()),
					() -> crucibleOf(tRow).getStructurePattern(), BlockPos.ZERO));
		}
		// --- batch B (mbpreview-data-b-energy): the energy family -----------------------
		// the three converter families share the ONE GTMultiBlockConverter binding
		// (GTMultiBlockConverter.java:395 — the facing-rotated 3x3x4 shell): the throwaway
		// BE carries its TIER through the VARIANT STATE (the batch-A boiler doctrine —
		// getWallBlock reads the row's Dense Wall off the state, GTSteamTurbineBlockEntity
		// :47-53 / GTGasTurbineBlockEntity :100-106 / GTLargeDynamoBlockEntity :43-49) and
		// the binding is formingPart, so the wall identity rides the cells — zero re-stamp
		for (GT6Turbines.SteamTurbineRow tRow : GT6Turbines.STEAM_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Turbines.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTSteamTurbineBlockEntity(BlockPos.ZERO,
							GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		for (GT6Turbines.GasTurbineRow tRow : GT6Turbines.GAS_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Turbines.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTGasTurbineBlockEntity(BlockPos.ZERO,
							GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		for (GT6DynamoHousings.DynamoRow tRow : GT6DynamoHousings.DYNAMO_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6DynamoHousings.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTLargeDynamoBlockEntity(BlockPos.ZERO,
							GT6DynamoHousings.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		// the Implosion Compressor (upstream :1228) — the binding carries the shell form
		// directly (formingPart over the getPartBlock hook, TileEntityImplosionCompressor
		// :131-142; state-blind like the Coke Oven collect). D2 seat: the shell cube
		// centres on controller + anchor + up (upstream :48) — the controller IS the
		// front-bottom-centre shell cell (0,0,-1), "Side-Bottom" (:69): the anchor law
		// already lands it, now declared (the B-batch legacy-① suspicion dissolved)
		tRows.add(new Entry("implosion_compressor", GTMultiBlocks.IMPLOSION_COMPRESSOR_ITEM,
				() -> new TileEntityImplosionCompressor(BlockPos.ZERO,
						Blocks.AIR.defaultBlockState()).getStructurePattern(), new BlockPos(0, 0, -1)));
		// the Large Heat Exchanger (upstream :1245) — the binding is predicate-shaped
		// (part() cells, GT6HeatExchangerBlockEntity:266-285), so the row re-stamps the
		// display identity from its own part list (the boiler form); the throwaway BE
		// carries the real carrier state so getWallBlock answers the Dense Tungsten Wall.
		// D2 seat: the zero-offset walk skips the y0 centre (upstream :85/:89-96 —
		// "with Main inside" :125): the undeclared seat (0,0,0) paints additively
		tRows.add(new Entry("large_heat_exchanger", GT6HeatExchangers.HEAT_EXCHANGER_ITEM,
				GT6MultiblockPreviews::heatExchangerPreview, BlockPos.ZERO));
		// --- batch C (mbpreview-data-c-processing): the processing family ---------------
		// the twelve W3 large machines (upstream Loader_MultiTileEntities.java:1229-1240):
		// ONE loop off the registration ladder (GT6LargeMachines.ROWS IS the upstream line
		// order); the throwaway BE carries its row through the BLOCK STATE (the registry
		// constructor resolves it, GTLargeMachineBlock.row()) and the binding is
		// formingPart over the row's own wall/inner/base paths (StructureKind.build) —
		// zero re-stamp
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) {
			tRows.add(new Entry(tRow.path(), GT6LargeMachines.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GT6LargeMachines.GTLargeMachineBlockEntity(BlockPos.ZERO,
							GT6LargeMachines.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		// the two distillation towers (upstream :1226-1227, the verbatim-clone Cryo shape):
		// the binding is formingPart with the per-facing hole column (GT6Distillation
		// getStructurePattern :372) — zero re-stamp. D2 seat: the pattern frame is the
		// centre one cell behind the facing (upstream :53) and the walk DECLARES the
		// controller seat as a 18102 part cell — the anchor law (0,0,-1) is exactly it,
		// now declared
		for (GT6Distillation.TowerRow tRow : GT6Distillation.ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Distillation.TOWER_ITEMS_BY_PATH.get(tRow.path()),
					() -> new GT6Distillation.TileEntityDistillationTower(BlockPos.ZERO,
							GT6Distillation.TOWER_BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern(),
					new BlockPos(0, 0, -1)));
		}
		// --- batch D1 (mbpreview-data-d1-special): the special family --------------------
		// the Bedrock Drill (upstream :1283): the BE binding stays NULL (the
		// existence-probe seam — the y-5 floor is TERRAIN the declared pattern cannot
		// judge, TileEntityBedrockDrill:179-181), so the row carries the DISPLAY
		// declaration: the hand check's geometry (:87-117) with the y-5 probe declared as
		// the floor's bedrock identity; the part identities are the SAME anyPartBlock
		// resolutions the BE hooks make (getDrillHeadBlock :159-161 / getWallBlock :164-166).
		// D2 seat: the zero-offset walk's y0 loop includes the controller's own cell
		// (:87-117 checkAndSetTargetOffset) — seat (0,0,0), declared
		tRows.add(new Entry("bedrock_drill", GTMultiBlocks.BEDROCK_DRILL_ITEM,
				GT6MultiblockPreviews::bedrockDrillPreview, BlockPos.ZERO));
		// the Von da Graagg (upstream :1280): the binding carries the shape (the
		// cornerless 5x5x2 base + the 5m coil pole + the top box, the zero-offset
		// controller anchor — TileEntityVonDaGraagg:173-197): direct collect. D2 seat:
		// the ZERO-OFFSET walk (upstream :66-93) declares the controller's own base cell
		// — seat (0,0,0); the anchor law's (0,0,-1) guess was a base wall
		tRows.add(new Entry("von_da_graagg", GTMultiBlocks.VON_DA_GRAAGG_ITEM,
				() -> new TileEntityVonDaGraagg(BlockPos.ZERO, Blocks.AIR.defaultBlockState()).getStructurePattern(),
				BlockPos.ZERO));
		// the 25 Tank Main Valves (upstream :1195-1222 — GT6Tanks.ROWS IS the upstream
		// line order): ONE loop off the registration ladder; the binding is the
		// radius-parametrized shell (GTTankValveBlockEntity getStructurePattern — the
		// -(r-1)*OFF frame shift lands the valve seat on the anchor law), formingPart
		// over the row's own wall — zero re-stamp
		for (GT6Tanks.TankValveRow tRow : GT6Tanks.ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Tanks.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTTankValveBlockEntity(BlockPos.ZERO,
							GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		// --- batch D2 (mbpreview-data-d2-authored): the authored shapes ------------------
		// the four no-pattern machines: their server checks are hand-written walks whose
		// semantics the declarative pattern API deliberately does not express (the N-of-M
		// quota cores, the mActive-conditional ring design, the unbounded rod probe — the
		// TileEntityMassfab/TileEntityFusionReactor class docs, GTMultiBlockPattern quota
		// case ②/probe case ③), so the BE bindings stay null and untouched and the display
		// shapes are authored HERE over the same walks (the bedrockDrillShape doctrine).
		// The predicate cores carry the r11 Q3 degrade: the quota cells declare the full
		// candidate set, the render stamps the FIRST candidate (withDisplayBlocks) and the
		// tooltip lists them all (candidatePaths/cellCandidatePaths).
		// upstream registration order: :1241 massfab / :1242 fusion / :1281 logistics / :1282 rod
		tRows.add(new Entry("large_massfab", GTMultiBlocks.MASSFAB_ITEM,
				GT6MultiblockPreviews::massfabPreview, new BlockPos(0, 0, -2)));
		tRows.add(new Entry("fusion_reactor", GTMultiBlocks.FUSION_REACTOR_ITEM,
				GT6MultiblockPreviews::fusionPreview, new BlockPos(0, 0, -2)));
		tRows.add(new Entry("logistics_core", GT6Logistics.LOGISTICS_CORE_ITEM,
				GT6MultiblockPreviews::logisticsCorePreview, new BlockPos(0, 0, -2)));
		tRows.add(new Entry("multiblock_lightning_rod", GTMultiBlocks.LIGHTNING_ROD_ITEM,
				GT6MultiblockPreviews::lightningRodPreview, BlockPos.ZERO));
		return List.copyOf(tRows);
	}

	// ---------------------------------------------------------------------------
	// the batch-D2 authored shapes (the bedrockDrillShape doctrine: package-private,
	// identity-parameterized — the offline tests drive the SAME functions the rows
	// compose; the part identities at the row edge are the SAME anyPartBlock
	// resolutions the BE hooks make)
	// ---------------------------------------------------------------------------

	/** The part block by registration path — the row edge's lazy identity resolution (the drill preview form). */
	private static Block part(String aPath) {
		return GTMultiBlocks.anyPartBlock(aPath);
	}

	/**
	 * The Matter Fabricator display shape (upstream checkStructure2 :45-224, the port
	 * TileEntityMassfab class doc arithmetic): 150 cells = six 5x5 layers — dy0/dy4 the
	 * full Dense Lead Walls (:50-74/:154-178), dy1..dy3 the 16-wall ring + the inner 3x3
	 * Osmium Coils (:82-94/:108-121/:134-147) with the exact centre the fail-not-clear
	 * AIR (:114 — the checker hollow), dy5 the 16 vents ring (:180-195), the centre
	 * Versatile PU (:197) and the 8-cell Control/Conversion 4+4 quota ring (:201-217)
	 * declared as the candidates predicate. The 8 quota cells carry the r11 Q3 degrade:
	 * first candidate renders, the tooltip lists both.
	 */
	static GTMultiBlockPattern massfabShape(Block aWall, Block aCoil, Block aVent, Block aVersatile, List<Block> aQuota) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
			tBuilder.formingPart(i, 0, j, aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
			tBuilder.formingPart(i, 4, j, aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
			boolean tInner = Math.abs(i) <= 1 && Math.abs(j) <= 1;
			boolean tCentre = i == 0 && j == 0;
			for (int tY = 1; tY <= 3; tY++) {
				if (tCentre && tY == 2) tBuilder.hollow(i, tY, j, GTMultiBlockPattern.AIR); // :114
				else if (tInner) tBuilder.formingPart(i, tY, j, aCoil, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
				else tBuilder.formingPart(i, tY, j, aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
			}
			if (Math.abs(i) == 2 || Math.abs(j) == 2) tBuilder.formingPart(i, 5, j, aVent, MultiBlockPartBlockEntity.NOTHING, 0);
			else if (tCentre) tBuilder.formingPart(i, 5, j, aVersatile, MultiBlockPartBlockEntity.NOTHING, 0);
			else tBuilder.part(i, 5, j, GTMultiBlockPattern.anyOf(aQuota.toArray(new Block[0])));
		}
		return tBuilder.build();
	}

	/** The row's massfab shape over the BE hooks' identities; the quota ring stamps its first candidate. */
	private static GTMultiBlockPattern massfabPreview() {
		return withDisplayBlocks(
				massfabShape(part("dense_wall_lead"), part("large_osmium_coil"), part("ventilation_unit"),
						part("processor_unit_versatile"), candidateBlocks("large_massfab")),
				candidateBlocks("large_massfab").toArray(new Block[0]));
	}

	/**
	 * The Fusion Reactor display shape (upstream checkStructure2 :47-126): 887 cells —
	 * the 5x5x5 core walk (125: the 27 Quadcore-PU quota cells declared as the
	 * candidates predicate — the :56-64 per-type fallback chain can't be a forming
	 * identity — + the 48 Galvanized Steel shell cells (:65-66, the axis tips included)
	 * + the 50 vents (:68)) + the four orthogonal arms minus the facing one (:74-89 —
	 * at the canonical north display the -z arm is the dropped one) + the OCTAGONS base
	 * (:91-125, the masks verbatim off the port BE's own public table): 216 glass +
	 * 360 glass/coil + 180 glass/coil/stainless. The ring declares the IDLE design 5
	 * (the active flip 5↔6 is the BE's refreshStructureOnActiveStateChange concern — a
	 * display declaration picks one state). Geometry rides TileEntityFusionReactor's
	 * pure helpers (shellKind/isRingOutTip/OCTAGONS) — zero transcription.
	 */
	static GTMultiBlockPattern fusionShape(Block aWall, Block aGlass, Block aSs, Block aCoil, Block aVent, List<Block> aPus) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			int tKind = TileEntityFusionReactor.shellKind(i, j, k);
			if (tKind == -1) tBuilder.part(i, j, k, GTMultiBlockPattern.anyOf(aPus.toArray(new Block[0]))); // :56-64
			else if (tKind == 0) tBuilder.formingPart(i, j, k, aWall, MultiBlockPartBlockEntity.NOTHING, 0); // :65-66
			else tBuilder.formingPart(i, j, k, aVent, MultiBlockPartBlockEntity.NOTHING, 0); // :68
		}
		// the arms (:74-89), facing 2 (north) skips the -z pair
		tBuilder.formingPart(-3, 0, 0, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		tBuilder.formingPart(-4, 0, 0, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		tBuilder.formingPart(3, 0, 0, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		tBuilder.formingPart(4, 0, 0, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		tBuilder.formingPart(0, 0, 3, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		tBuilder.formingPart(0, 0, 4, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		for (int i = 0; i < 19; i++) for (int j = 0; j < 19; j++) {
			int tX = i - 9, tZ = j - 9; // :91 the ring origin, re-based to the core centre
			if (TileEntityFusionReactor.OCTAGONS[0][i][j]) {
				tBuilder.formingPart(tX, -1, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0); // :95
				if (TileEntityFusionReactor.isRingOutTip(i, j)) // :96-97
					tBuilder.formingPart(tX, 0, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, 2);
				else tBuilder.formingPart(tX, 0, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 5); // :99 idle
				tBuilder.formingPart(tX, 1, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0); // :101
			}
			if (TileEntityFusionReactor.OCTAGONS[1][i][j]) { // :104-112
				tBuilder.formingPart(tX, -2, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0);
				tBuilder.formingPart(tX, -1, tZ, aGlass, MultiBlockPartBlockEntity.NOTHING, 0);
				tBuilder.formingPart(tX, 0, tZ, aCoil, MultiBlockPartBlockEntity.NOTHING, 0);
				tBuilder.formingPart(tX, 1, tZ, aGlass, MultiBlockPartBlockEntity.NOTHING, 0);
				tBuilder.formingPart(tX, 2, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0);
			}
			if (TileEntityFusionReactor.OCTAGONS[2][i][j]) { // :115-123
				tBuilder.formingPart(tX, -2, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0);
				tBuilder.formingPart(tX, -1, tZ, aCoil, MultiBlockPartBlockEntity.NOTHING, 0);
				tBuilder.formingPart(tX, 0, tZ, aSs, MultiBlockPartBlockEntity.NOTHING, 0);
				tBuilder.formingPart(tX, 1, tZ, aCoil, MultiBlockPartBlockEntity.NOTHING, 0);
				tBuilder.formingPart(tX, 2, tZ, aGlass, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0);
			}
		}
		return tBuilder.build();
	}

	/** The row's fusion shape over the BE hooks' identities; the PU core stamps its first candidate (the idle page). */
	private static GTMultiBlockPattern fusionPreview() {
		return withDisplayBlocks(
				fusionShape(part("machine_wall_galvanized_steel"), part("machine_wall_tungstensteel"),
						part("machine_wall_stainless_steel"), part("large_iridium_coil"), part("ventilation_unit"),
						candidateBlocks("fusion_reactor")),
				candidateBlocks("fusion_reactor").toArray(new Block[0]));
	}

	/**
	 * The Logistics Core display shape (upstream checkStructure2 :109-147): 125 cells —
	 * the 27 CPU cells of d²&lt;4 declared as the candidates predicate (:119-133: any
	 * processor mix plus the :132 wall cheapskate arm, which rides the list tail), the
	 * 44 Galvanized Steel cells of d²&gt;6 (:137-138, logistics+energy faces) and the 54
	 * vents between (:139-140 — the controller seat (0,0,-2) front-centre included).
	 */
	static GTMultiBlockPattern logisticsCoreShape(Block aWall, Block aVent, List<Block> aCpus) {
		List<Block> tCoreCandidates = new ArrayList<>(aCpus);
		tCoreCandidates.add(aWall); // :132-133 — the wall substitution, last candidate
		java.util.function.Predicate<BlockState> tCore = GTMultiBlockPattern.anyOf(tCoreCandidates.toArray(new Block[0]));
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			int tD2 = i * i + j * j + k * k;
			if (tD2 < 4) tBuilder.part(i, j, k, tCore);
			else if (tD2 > 6) tBuilder.formingPart(i, j, k, aWall,
					MultiBlockPartBlockEntity.ONLY_LOGISTICS & MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 0);
			else tBuilder.formingPart(i, j, k, aVent, MultiBlockPartBlockEntity.ONLY_LOGISTICS, 0);
		}
		return tBuilder.build();
	}

	/** The row's logistics core shape over the BE hooks' identities; the CPU core stamps its first candidate. */
	private static GTMultiBlockPattern logisticsCorePreview() {
		return withDisplayBlocks(
				logisticsCoreShape(part("machine_wall_galvanized_steel"), part("ventilation_unit"),
						candidateBlocks("logistics_core").subList(0, 5)), // the five CPUs; the wall rides the shape's own tail
				candidateBlocks("logistics_core").toArray(new Block[0]));
	}

	/**
	 * The Lightning Rod display shape (upstream checkStructure2 :72-86): the five 3x3
	 * layers (walls/coils/walls/coils/walls, :77-81) plus ONE pillar block at (0,5,0) —
	 * the MINIMAL representative of the unbounded {@code while (mSize)} probe (:84):
	 * fixed slices cannot carry a variable-length column (the r11 non-binding ruling —
	 * this row is the APPROXIMATE declaration, mSize==1 of the "optimum 100m" tooltip),
	 * the pillar cell declares the 18104 identity so the page reads as a rod.
	 */
	static GTMultiBlockPattern lightningRodShape(Block aWall, Block aCoil, Block aRod) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
			tBuilder.formingPart(i, 0, j, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
			tBuilder.formingPart(i, 1, j, aCoil, MultiBlockPartBlockEntity.NOTHING, 0);
			tBuilder.formingPart(i, 2, j, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
			tBuilder.formingPart(i, 3, j, aCoil, MultiBlockPartBlockEntity.NOTHING, 0);
			tBuilder.formingPart(i, 4, j, aWall, MultiBlockPartBlockEntity.NOTHING, 0);
		}
		tBuilder.formingPart(0, 5, 0, aRod, MultiBlockPartBlockEntity.NOTHING, 0); // :84, the 1m representative
		return tBuilder.build();
	}

	/** The row's rod shape over the BE hooks' identities (the port getWallBlock/getCoilBlock/getRodBlock resolutions). */
	private static GTMultiBlockPattern lightningRodPreview() {
		return lightningRodShape(part("machine_wall_tungsten"), part("niobium_titanium_coil"), part("lightning_rod"));
	}

	/**
	 * The heat exchanger row's display pattern: the BE binding (predicate-only) re-stamped
	 * with the Dense Tungsten Wall + the Heat Transmitter (the two identities the
	 * predicates judge — zero transcription of the geometry).
	 */
	private static GTMultiBlockPattern heatExchangerPreview() {
		return withDisplayBlocks(
				new GT6HeatExchangerBlockEntity(BlockPos.ZERO,
						GT6HeatExchangers.HEAT_EXCHANGER_BLOCK.get().defaultBlockState()).getStructurePattern(),
				GTMultiBlocks.WALL_BLOCKS_BY_PATH.get("dense_wall_tungsten").get(),
				GTMultiBlocks.HEAT_TRANSMITTER.get());
	}

	/**
	 * The boiler row's display pattern: the BE binding (display-only — the structure
	 * check rides the hand loop, TileEntityLargeBoiler.java:287-331) re-stamped with the
	 * variant's own wall block + the heat transmitter (the two identities the predicates
	 * judge — zero transcription of the geometry).
	 */
	private static GTMultiBlockPattern boilerPreview(GTLargeBoilerBlock aBlock) {
		return withDisplayBlocks(
				new TileEntityLargeBoiler(BlockPos.ZERO, aBlock.defaultBlockState()).getStructurePattern(),
				aBlock.wallBlock(), GTMultiBlocks.HEAT_TRANSMITTER.get());
	}

	/** The throwaway crucible BE carrying its row's wall (the getWallBlock override above). */
	private static TileEntityCrucible crucibleOf(GT6Crucibles.CrucibleRow aRow) {
		return new TileEntityCrucible(BlockPos.ZERO, Blocks.AIR.defaultBlockState()) {
			@Override
			protected Block getWallBlock() {
				return GT6Crucibles.wallBlockOf(aRow);
			}
		};
	}

	/**
	 * The Bedrock Drill row's display pattern: the existence-probe machine's BE binding
	 * stays null (TileEntityBedrockDrill:179-181 — the y-5 floor is terrain the declared
	 * pattern cannot judge), so the geometry is declared HERE as display data over the
	 * hand check's walk (:87-117). Controller-anchored walk (the Graagg/crucible anchor
	 * family): the seam paints the controller on the anchor cell — the legacy-① ruling,
	 * generalization is D2's.
	 */
	private static GTMultiBlockPattern bedrockDrillPreview() {
		return bedrockDrillShape(
				GTMultiBlocks.anyPartBlock("bedrock_drill_head"),   // the getDrillHeadBlock hook's resolution
				GTMultiBlocks.anyPartBlock("dense_wall_titanium")); // the getWallBlock hook's resolution
	}

	/**
	 * The drill shape over injectable part identities (the offline-test seam — the SAME
	 * function the row composes, the batch-C doctrine): 54 cells = the y-5 probe floor as
	 * the bedrock display declaration (the ore face of the probe renders as the floor's
	 * base skin — one display identity), then the four machine layers with the hand
	 * check's {@code (usage, design)} triples verbatim.
	 */
	static GTMultiBlockPattern bedrockDrillShape(Block aHead, Block aWall) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
			tBuilder.formingPart(i, -5, j, Blocks.BEDROCK, MultiBlockPartBlockEntity.NOTHING, 0); // :88-107 the probe, display-declared
			tBuilder.formingPart(i, -4, j, aHead, MultiBlockPartBlockEntity.NOTHING, 0);            // :109
			tBuilder.formingPart(i, -3, j, aWall, MultiBlockPartBlockEntity.ONLY_FLUID_IN, 0);      // :110
			tBuilder.formingPart(i, -2, j, aWall, MultiBlockPartBlockEntity.ONLY_FLUID_IN, 0);      // :111
			if ((i == 0) != (j == 0)) tBuilder.formingPart(i, -1, j, aWall, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 3); // :113 the energy ring
			else tBuilder.formingPart(i, -1, j, aWall, MultiBlockPartBlockEntity.ONLY_FLUID_IN, 0); // :115
			tBuilder.formingPart(i, 0, j, aWall, MultiBlockPartBlockEntity.ONLY_FLUID_IN, 0);       // :117 (the centre is the controller self-cell)
		}
		return tBuilder.build();
	}

	/**
	 * The display-identity stamp for predicate-shaped bindings: cells declared with
	 * {@code part(predicate)} carry {@code partBlock == null} (the seam draws nothing for
	 * them), so each cell takes the FIRST part candidate its predicate accepts — the
	 * judgement and the stamp share one identity. {@code formingPart} cells pass through
	 * untouched (their partBlock is already the identity); an unresolvable cell keeps its
	 * predicate shape (the seam's draw-nothing ruling).
	 */
	static GTMultiBlockPattern withDisplayBlocks(GTMultiBlockPattern aPattern, Block... aParts) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			Block tBlock = tCell.partBlock;
			if (tBlock == null && !tCell.isHollow()) {
				for (Block tPart : aParts) {
					if (tCell.predicate.test(tPart.defaultBlockState())) {
						tBlock = tPart;
						break;
					}
				}
			}
			if (tCell.isHollow()) tBuilder.hollow(tCell.x, tCell.y, tCell.z, tCell.predicate);
			else if (tBlock != null) tBuilder.formingPart(tCell.x, tCell.y, tCell.z, tBlock, tCell.usage, tCell.design);
			else tBuilder.part(tCell.x, tCell.y, tCell.z, tCell.predicate);
		}
		return tBuilder.build();
	}

	/** The live rows — one per previewed machine (the data cards' growth point). */
	public static List<Entry> entries() {
		return ENTRIES;
	}

	/**
	 * The schema fill: the pattern's structural cells as {@code (centre-relative pos →
	 * state)} — hollow markers stay air, the CONTROLLER rides its seat (the ruling-A1
	 * cell when declared, the anchor law otherwise — see {@link Entry#controllerCell}),
	 * forming cells their {@code partBlock}. Predicate-only cells draw NOTHING unless
	 * the row stamped them first ({@link #withDisplayBlocks} — the D2 quota rows do:
	 * the first candidate becomes the identity; the unstamped keep the draw-nothing
	 * ruling). Insertion-ordered: the cell declaration order (the upstream loop order)
	 * survives for stable rendering and material listing.
	 */
	/**
	 * The schema fill, anchor-law form (the legacy default — the seat guessed at
	 * {@code -anchorOffset}); the seat-declaring rows ride the
	 * {@link #structureBlocks(Entry, byte)} overload.
	 */
	public static Map<BlockPos, BlockState> structureBlocks(GTMultiBlockPattern aPattern, @Nullable Block aController, byte aFacing) {
		return structureBlocks(aPattern, aController, aFacing, null);
	}

	/**
	 * The schema fill, seat-declared form (user ruling A1): {@code aControllerCell} (a
	 * row's {@link Entry#controllerCell}) paints the controller on THAT cell — overriding
	 * the anchor law's guess and painting ADDITIVELY when the upstream never declares the
	 * seat (the crucible/HEX bottom centres); {@code null} falls back to the anchor law.
	 */
	public static Map<BlockPos, BlockState> structureBlocks(GTMultiBlockPattern aPattern, @Nullable Block aController,
			byte aFacing, @Nullable BlockPos aControllerCell) {
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(aFacing);
		Map<BlockPos, BlockState> tMap = new LinkedHashMap<>();
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			if (tCell.isHollow()) continue;
			boolean tIsController = aControllerCell != null
					? tCell.x == aControllerCell.getX() && tCell.y == aControllerCell.getY() && tCell.z == aControllerCell.getZ()
					: tCell.x == -tAnchor[0] && tCell.y == -tAnchor[1] && tCell.z == -tAnchor[2];
			Block tBlock = tIsController ? aController : tCell.partBlock;
			if (tBlock == null) continue;
			tMap.put(new BlockPos(tCell.x, tCell.y, tCell.z), tBlock.defaultBlockState());
		}
		// the undeclared-seat arm: the true seat is a hole in the upstream declaration
		// (the crucible :118 / HEX :89-96 centre skips) — the controller fills it
		if (aControllerCell != null && aController != null) tMap.putIfAbsent(aControllerCell, aController.defaultBlockState());
		return tMap;
	}

	/** The row-driven fill — the viewer legs' entry point (the seat rides the row). */
	public static Map<BlockPos, BlockState> structureBlocks(Entry aEntry, byte aFacing) {
		return structureBlocks(aEntry.pattern().get(), aEntry.controllerBlock(), aFacing, aEntry.controllerCell());
	}

	/**
	 * The material list: block → count over the schema fill — the "what do I need to
	 * build this" column (the GTCEu initializeContainedBlocks face). The controller
	 * counts as its own material (1x) — the seat cell IS one controller.
	 */
	public static Map<Block, Integer> materialCounts(GTMultiBlockPattern aPattern, @Nullable Block aController, byte aFacing) {
		return materialCounts(aPattern, aController, aFacing, null);
	}

	/** The {@link #structureBlocks(GTMultiBlockPattern, Block, byte, BlockPos)} seat-declared twin. */
	public static Map<Block, Integer> materialCounts(GTMultiBlockPattern aPattern, @Nullable Block aController,
			byte aFacing, @Nullable BlockPos aControllerCell) {
		Map<Block, Integer> tCounts = new LinkedHashMap<>();
		for (BlockState tState : structureBlocks(aPattern, aController, aFacing, aControllerCell).values()) {
			tCounts.merge(tState.getBlock(), 1, Integer::sum);
		}
		return tCounts;
	}

	/** The row-driven material list — the viewer legs' entry point (the seat rides the row). */
	public static Map<Block, Integer> materialCounts(Entry aEntry, byte aFacing) {
		Map<Block, Integer> tCounts = new LinkedHashMap<>();
		for (BlockState tState : structureBlocks(aEntry, aFacing).values()) {
			tCounts.merge(tState.getBlock(), 1, Integer::sum);
		}
		return tCounts;
	}

	// ---------------------------------------------------------------------------
	// the predicate-cell display face (task mbpreview-data-d2-authored, the r11 Q3
	// degrade ruling): the quota cores render their FIRST candidate and the full
	// candidate list rides the tooltip — the paths are the honest data (the upstream
	// part ids the walk accepts), resolved to blocks lazily at the viewer edge.
	// ---------------------------------------------------------------------------

	/**
	 * The predicate cells' candidate list per row — the upstream part paths the check
	 * accepts at those cells, first candidate = the rendered identity. Empty for every
	 * row without predicate cells. Sources: the fusion core's per-type quota
	 * (MultiTileEntityFusionReactor :56-64, ≥3 versatile/≥12 logic/≥12 control), the
	 * massfab ring's 4+4 quota (MultiTileEntityMatterFabricator :201-217) and the
	 * logistics core's free combination (MultiTileEntityLogisticsCore :119-133 — the
	 * wall substitution rides last, the :132 cheapskate arm).
	 */
	public static List<String> candidatePaths(String aRowName) {
		return switch (aRowName) {
			case "fusion_reactor" -> List.of("processor_unit_versatile", "processor_unit_logic", "processor_unit_control");
			case "large_massfab" -> List.of("processor_unit_control", "processor_unit_conversion");
			case "logistics_core" -> List.of("processor_unit_versatile", "processor_unit_logic", "processor_unit_control",
					"processor_unit_storage", "processor_unit_conversion", "machine_wall_galvanized_steel");
			default -> List.of();
		};
	}

	/** The candidate paths resolved to the live part blocks (the tooltip's item face; lazy — frozen registry). */
	public static List<Block> candidateBlocks(String aRowName) {
		List<Block> tBlocks = new ArrayList<>();
		for (String tPath : candidatePaths(aRowName)) tBlocks.add(GTMultiBlocks.anyPartBlock(tPath));
		return tBlocks;
	}

	/** The pattern cell at a structure-centre-relative position, or null. */
	@Nullable
	public static GTMultiBlockPattern.Cell cellAt(GTMultiBlockPattern aPattern, BlockPos aPos) {
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			if (tCell.x == aPos.getX() && tCell.y == aPos.getY() && tCell.z == aPos.getZ()) return tCell;
		}
		return null;
	}

	/**
	 * The tooltip candidates of ONE cell (the degrade ruling's data half): the row's list
	 * when the cell is a predicate structural cell ({@code partBlock == null}, not
	 * hollow — the stamped render is its first candidate), empty for forming/hollow/
	 * unknown cells so plain walls never advertise foreign candidates.
	 */
	public static List<String> cellCandidatePaths(String aRowName, @Nullable GTMultiBlockPattern.Cell aCell) {
		if (aCell == null || aCell.isHollow() || aCell.forms()) return List.of();
		return candidatePaths(aRowName);
	}

	/** The {@link #cellCandidatePaths} list resolved to the live part blocks (the tooltip face). */
	public static List<Block> cellCandidateBlocks(String aRowName, @Nullable GTMultiBlockPattern.Cell aCell) {
		List<Block> tBlocks = new ArrayList<>();
		for (String tPath : cellCandidatePaths(aRowName, aCell)) tBlocks.add(GTMultiBlocks.anyPartBlock(tPath));
		return tBlocks;
	}

	private GT6MultiblockPreviews() { }
}
