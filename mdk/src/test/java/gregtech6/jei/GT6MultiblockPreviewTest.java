/**
 * Offline guard tests for task multiblock-preview-infra: the pattern→schema model seam
 * (the fill map and material counts the preview widget feeds MapSchema with), the static
 * item→pattern table's census, and the viewer plugin swap (the text-info pages replaced by
 * the preview category on both legs). The widget object itself and the two category
 * classes are viewer-embed faces (they link the compile-only ModularUI/JEI/EMI stacks and
 * construct virtual levels) — never class-loaded offline; their pins ride the bytecode
 * layer, the same strategy as GT6JeiPluginTest/GT6EmiPluginTest. In-game rendering across
 * the JEI-only / EMI-only / both-installed matrix stays with the card's field_test gate.
 *
 * <p>The fill fixture re-declares the Coke Oven shape over vanilla blocks (26
 * formingPart bricks + the hollow centre — the TestCokeOven binding, TileEntityCokeOven
 * :119-130) because the LIVE pattern supplier rides the frozen mod registry (unreachable
 * offline); the live pattern's equality with this shape is the multiblock tests' pin
 * (GTMultiBlockStructureCheckerFormSeamTest), the north-anchor arithmetic is the pure
 * GTMultiBlockPattern.cellOffset table. The batch-A thermal rows pin their family shapes
 * the same way: fixture-block stand-ins over the REAL TileEntityLargeBoiler /
 * TileEntityCrucible bindings (the PatternBoiler/TestCrucible recipe from the multiblock
 * tests) — the per-row wiring itself is the census pin above.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.emi.GT6EmiPlugin;
import gregtech6.emi.GT6MultiblockPreviewEmiCategory;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GT6Turbines;
import gregtech6.tileentity.multiblocks.GT6HeatExchangerBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlockConverter;
import gregtech6.tileentity.multiblocks.GTTankValveBlockEntity;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;
import gregtech6.tileentity.multiblocks.TileEntityVonDaGraagg;

public class GT6MultiblockPreviewTest extends GTRecipesOfflineTestBase {

	/** The fixture controller (the real row rides the frozen registry — the class doc). */
	private static final Block CONTROLLER = Blocks.FURNACE;

	/** The coke-oven-shape fixture: 26 formingPart cells + the hollow centre, north-facing display. */
	private static GTMultiBlockPattern cokeOvenShape() {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			tBuilder.formingPart(i, j, k, Blocks.BRICKS, 0, 0);
		}
		tBuilder.hollow(0, 0, 0, GTMultiBlockPattern.AIR);
		return tBuilder.build();
	}

	@Test
	public void anchorCellPaintsTheControllerBlock() {
		// north = facing 2 → anchor (0,0,+1) → the controller occupies the centre-relative
		// cell (0,0,-1): the research card's "26 pattern cells, one is really the controller"
		// coke-oven special case, generalized to the anchor arithmetic
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				cokeOvenShape(), CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(26, tFill.size(), "26 structural cells render (25 bricks + the controller)");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(0, 0, -1)),
				"the north-anchor cell is the controller, not a brick");
		assertEquals(Blocks.BRICKS.defaultBlockState(), tFill.get(new BlockPos(0, 0, 1)),
				"the opposite anchor side stays a part block");
		assertFalse(tFill.containsKey(BlockPos.ZERO), "the hollow centre never renders");
	}

	@Test
	public void anchorFollowsTheFacingTable() {
		// facing byte 3 (south) mirrors the anchor: the controller cell flips to (0,0,+1)
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(cokeOvenShape(), CONTROLLER, (byte) 3);
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(0, 0, 1)),
				"the facing table drives the anchor cell, not a hardcoded north");
	}

	@Test
	public void materialCountsPinTheShoppingList() {
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				cokeOvenShape(), CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(2, tCounts.size(), "controller + one part block");
		assertEquals(Integer.valueOf(25), tCounts.get(Blocks.BRICKS), "25 bricks");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER), "the controller counts as its own material");
	}

	@Test
	public void predicateOnlyCellsDrawNothing() {
		// a declaration-only cell (no forming expectation, not the anchor) — v1 renders
		// nothing for it (the data-card display ruling, GT6MultiblockPreviews javadoc)
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder()
				.formingPart(0, 0, 1, Blocks.BRICKS, 0, 0)
				.part(1, 0, 0, GTMultiBlockPattern.is(Blocks.IRON_BLOCK))
				.build();
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(1, tFill.size(), "the forming cell renders");
		assertFalse(tFill.containsKey(new BlockPos(1, 0, 0)), "the predicate-only cell is skipped in v1");
	}

	@Test
	public void tableCensusPinsTheFullRowLadder() {
		// the data-batch face (the tail-append doctrine — re-pinned per batch): the coke
		// oven + batch A's THERMAL pair (the 5 boiler tiers Loader_MultiTileEntities
		// :1248-1252, the 8 crucible tiers :1270-1277) + batch B's ENERGY fourteen (task
		// mbpreview-data-b-energy): the 4 steam turbine tiers (:1254-1257), the 4 gas
		// turbine tiers (:1264-1267), the 4 dynamo housings (:1259-1262), the Implosion
		// Compressor (:1228) and the Large Heat Exchanger (:1245) + batch C's PROCESSING
		// fourteen (task mbpreview-data-c-processing): the W3 twelve in the upstream
		// :1229-1240 line order and the two distillation towers (:1226-1227) + batch D1's
		// SPECIAL twenty-seven (task mbpreview-data-d1-special): the Bedrock Drill
		// (:1283) and the Von da Graagg (:1280), then the 25 Tank Main Valves in the
		// GT6Tanks.ROWS order — the upstream :1195-1222 registration order verbatim + batch
		// D2's AUTHORED four (task mbpreview-data-d2-authored): the no-pattern machines —
		// Large Matter Fabricator (:1241), Fusion Reactor (:1242), Logistics Core (:1281)
		// and the Lightning Rod (:1282) — the display shapes authored table-side over the
		// upstream hand walks (the quota/mActive/probe cases the declarative API
		// deliberately does not express). The pin FAILS until the batch lands (the
		// red→green drill).
		List<String> tNames = GT6MultiblockPreviews.entries().stream()
				.map(GT6MultiblockPreviews.Entry::name).toList();
		assertEquals(List.of(
				"multiblock_coke_oven",
				// the boiler ladder — the upstream :1248-1252 line order
				"large_boiler_stainless_steel", "large_boiler_invar", "large_boiler_titanium",
				"large_boiler_tungstensteel", "large_boiler_adamantium",
				// the crucible ladder — the upstream :1270-1277 line order
				"crucible_steel", "crucible_stainless_steel", "crucible_invar", "crucible_titanium",
				"crucible_tungstensteel", "crucible_tungsten", "crucible_tantalum_hafnium_carbide",
				"crucible_adamantium",
				// batch B (energy): the converter shell families + the two direct collects
				"steam_turbine_magnalium", "steam_turbine_trinitanium",
				"steam_turbine_graphene", "steam_turbine_vibramantium",
				"gas_turbine_magnalium", "gas_turbine_trinitanium",
				"gas_turbine_graphene", "gas_turbine_vibramantium",
				"large_dynamo_stainless_steel", "large_dynamo_titanium",
				"large_dynamo_tungstensteel", "large_dynamo_adamantium",
				"implosion_compressor",
				"large_heat_exchanger",
				// batch C (processing): the W3 twelve — the upstream :1229-1240 line order —
				// then the two distillation towers (:1226-1227)
				"large_centrifuge", "large_electrolyzer", "large_coagulator", "large_autoclave",
				"large_bath", "large_batch_mixer", "large_fermenter", "large_electric_oven",
				"large_sluice", "large_crusher", "large_shredder", "large_squeezer",
				"distillation_tower", "cryo_distillation_tower",
				// batch D1 (special): the two controllers, then the 25 valves —
				// GT6Tanks.ROWS IS the upstream :1195-1222 line order
				"bedrock_drill", "von_da_graagg",
				"tank_wood",
				// the plain small 3x3x3 six — :1196-1201
				"tank_small_stainless_steel", "tank_small_invar", "tank_small_titanium",
				"tank_small_tungstensteel", "tank_small_tungsten", "tank_small_adamantium",
				// the dense small 3x3x3 six — :1203-1208
				"tank_small_dense_stainless_steel", "tank_small_dense_invar", "tank_small_dense_titanium",
				"tank_small_dense_tungstensteel", "tank_small_dense_tungsten", "tank_small_dense_adamantium",
				// the plain large 5x5x5 six — :1210-1215
				"tank_large_stainless_steel", "tank_large_invar", "tank_large_titanium",
				"tank_large_tungstensteel", "tank_large_tungsten", "tank_large_adamantium",
				// the dense large 5x5x5 six — :1217-1222
				"tank_large_dense_stainless_steel", "tank_large_dense_invar", "tank_large_dense_titanium",
				"tank_large_dense_tungstensteel", "tank_large_dense_tungsten", "tank_large_dense_adamantium",
				// batch D2 (authored): the four no-pattern machines — the upstream
				// :1241/:1242/:1281/:1282 registration line order
				"large_massfab", "fusion_reactor", "logistics_core", "multiblock_lightning_rod"),
				tNames, "the row census: coke oven + thermal 13 + energy 14 + processing 14 + special 27 + authored 4, upstream order");
		// the wiring face the census CAN see offline: every row carries its suppliers
		// (lazy handles — resolving them rides the live registry, the class doc)
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			assertNotNull(tEntry.item(), tEntry.name() + " carries an item supplier (the map-key wiring)");
			assertNotNull(tEntry.pattern(), tEntry.name() + " carries a pattern supplier");
		}
		assertEquals("gt6.jei.multiblock_preview", GT6MultiblockPreviews.TITLE_KEY,
				"category title key pinned (the ONE text survivor — the category name, kept)");
		assertEquals("multiblock_preview", GT6MultiblockPreviews.UID_PATH, "category uid path pinned");
		assertEquals(2, GT6MultiblockPreviews.DISPLAY_FACING, "display facing = north (Direction.get3DDataValue)");
		assertEquals(200, GT6MultiblockPreviews.PAGE_WIDTH);
		assertEquals(180, GT6MultiblockPreviews.PAGE_HEIGHT);
	}

	// ------------------------------------------------------------------
	// the batch-A thermal fixtures (the PatternBoiler/TestCrucible recipe —
	// the frozen registry keeps the live wall/transmitter handles out of the
	// bare-JVM reach, so the fixture blocks stand in for the tier identities)
	// ------------------------------------------------------------------

	static BlockEntityType<PreviewBoiler> sBoilerType;
	static BlockEntityType<PreviewCrucible> sCrucibleType;
	static BlockEntityType<PreviewConverter> sConverterType;
	static BlockEntityType<PreviewImplosion> sImplosionType;
	static BlockEntityType<PreviewHeatExchanger> sHexType;
	static BlockEntityType<PreviewTower> sTowerType;

	/** The offline boiler — fixture wall/transmitter over the REAL pattern binding. */
	public static final class PreviewBoiler extends TileEntityLargeBoiler {
		PreviewBoiler(BlockPos aPos, BlockState aState) {
			super(sBoilerType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	/** The offline crucible — fixture wall over the REAL pattern binding. */
	public static final class PreviewCrucible extends TileEntityCrucible {
		PreviewCrucible(BlockPos aPos, BlockState aState) {
			super(sCrucibleType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
	}

	// ------------------------------------------------------------------
	// the batch-B energy fixtures (the same recipe): the converter shell
	// (the three families differ only in the shape switches — fluid column /
	// coil segment / far design), the implosion compressor (the getPartBlock
	// hook, TileEntityImplosionCompressorTest precedent) and the heat
	// exchanger (wall+transmitter hooks, the GT6HeatExchangerTest form)
	// ------------------------------------------------------------------

	/** The offline converter — fixture wall/coil over the REAL binding, shape switches as fields. */
	public static final class PreviewConverter extends GTMultiBlockConverter {
		private final boolean mFluidColumn, mCoilSegment;
		private final int mFarDesign;
		PreviewConverter(BlockPos aPos, BlockState aState, boolean aFluidColumn, boolean aCoilSegment, int aFarDesign) {
			super(sConverterType, aPos, aState);
			mFluidColumn = aFluidColumn;
			mCoilSegment = aCoilSegment;
			mFarDesign = aFarDesign;
		}
		@Override
		public String getTileEntityName() {
			return "multiblock_steam_turbine"; // the BET name face the abstract root demands (the offline stand-in)
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getCoilBlock() {
			return Blocks.IRON_BLOCK;
		}
		@Override
		protected boolean fluidColumn() {
			return mFluidColumn;
		}
		@Override
		protected boolean coilSegment() {
			return mCoilSegment;
		}
		@Override
		protected int farPlateDesign() {
			return mFarDesign;
		}
	}

	/** The offline implosion compressor — fixture part block over the REAL pattern binding. */
	public static final class PreviewImplosion extends TileEntityImplosionCompressor {
		PreviewImplosion(BlockPos aPos, BlockState aState) {
			super(sImplosionType, aPos, aState);
		}
		@Override
		protected Block getPartBlock() {
			return Blocks.BRICKS;
		}
	}

	/** The offline heat exchanger — fixture wall/transmitter over the REAL (predicate) binding. */
	public static final class PreviewHeatExchanger extends GT6HeatExchangerBlockEntity {
		PreviewHeatExchanger(BlockPos aPos, BlockState aState) {
			super(sHexType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	// ------------------------------------------------------------------
	// the batch-C processing fixtures (the same recipe): the distillation
	// tower over the REAL :372 binding (the fixture part identities ride the
	// two protected hooks — the GT6DistillationTowerTest form). The W3 twelve
	// need no BE fixture: their family shapes ride the public
	// StructureKind.build with the fixture part identities (the
	// GT6LargeMachineRowTest form — the SAME functions the binding composes)
	// ------------------------------------------------------------------

	/** The offline distillation tower — fixture part identities over the REAL binding. */
	public static final class PreviewTower extends GT6Distillation.TileEntityDistillationTower {
		PreviewTower(BlockPos aPos, BlockState aState) {
			super(sTowerType, aPos, aState);
		}
		@Override
		protected Block getTransmitterBlock() {
			return Blocks.STONE;
		}
		@Override
		protected Block getPartBlock() {
			return Blocks.BRICKS;
		}
	}

	// ------------------------------------------------------------------
	// the batch-D1 special fixtures (the same recipe): the tank valve over
	// the REAL radius-parametrized binding (the fixture radius/wall ride
	// the two overrides — the GT6TankValveFamilyTest.TestTank form)
	// ------------------------------------------------------------------

	static BlockEntityType<PreviewTank> sTankValveType;

	/** The offline tank valve — fixture wall + explicit radius over the REAL binding. */
	public static final class PreviewTank extends GTTankValveBlockEntity {
		private final int mRadius;
		PreviewTank(int aRadius, BlockPos aPos, BlockState aState) {
			super(sTankValveType, aPos, aState);
			mRadius = aRadius;
		}
		@Override
		public int radius() {
			return mRadius;
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
	}

	static BlockEntityType<PreviewGraagg> sGraaggType;

	/** The offline Von da Graagg — fixture identities over the REAL zero-offset binding. */
	public static final class PreviewGraagg extends TileEntityVonDaGraagg {
		PreviewGraagg(BlockPos aPos, BlockState aState) {
			super(sGraaggType, aPos, aState);
		}
		@Override
		protected Block getBaseWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getCoilBlock() {
			return Blocks.IRON_BLOCK;
		}
		@Override
		protected Block getTopWallBlock() {
			return Blocks.STONE;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildPreviewFixtures() {
		// the base @BeforeAll booted + reopened the BET write window (GTRecipesOfflineTestBase)
		BlockEntityType<PreviewBoiler>[] tBoiler = (BlockEntityType<PreviewBoiler>[]) new BlockEntityType<?>[1];
		tBoiler[0] = BlockEntityType.Builder.of(PreviewBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sBoilerType = tBoiler[0];
		BlockEntityType<PreviewCrucible>[] tCrucible = (BlockEntityType<PreviewCrucible>[]) new BlockEntityType<?>[1];
		tCrucible[0] = BlockEntityType.Builder.of(PreviewCrucible::new, Blocks.BRICKS).build(null);
		sCrucibleType = tCrucible[0];
		BlockEntityType<PreviewConverter>[] tConverter = (BlockEntityType<PreviewConverter>[]) new BlockEntityType<?>[1];
		tConverter[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new PreviewConverter(aPos, aState, true, false, 3),
				Blocks.BRICKS, Blocks.IRON_BLOCK, Blocks.STONE).build(null);
		sConverterType = tConverter[0];
		BlockEntityType<PreviewImplosion>[] tImplosion = (BlockEntityType<PreviewImplosion>[]) new BlockEntityType<?>[1];
		tImplosion[0] = BlockEntityType.Builder.of(PreviewImplosion::new, Blocks.BRICKS).build(null);
		sImplosionType = tImplosion[0];
		BlockEntityType<PreviewHeatExchanger>[] tHex = (BlockEntityType<PreviewHeatExchanger>[]) new BlockEntityType<?>[1];
		tHex[0] = BlockEntityType.Builder.of(PreviewHeatExchanger::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sHexType = tHex[0];
		BlockEntityType<PreviewTower>[] tTower = (BlockEntityType<PreviewTower>[]) new BlockEntityType<?>[1];
		tTower[0] = BlockEntityType.Builder.of(PreviewTower::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sTowerType = tTower[0];
		BlockEntityType<PreviewTank>[] tTankValve = (BlockEntityType<PreviewTank>[]) new BlockEntityType<?>[1];
		tTankValve[0] = BlockEntityType.Builder.of((aPos, aState) -> new PreviewTank(1, aPos, aState), Blocks.BRICKS).build(null);
		sTankValveType = tTankValve[0];
		BlockEntityType<PreviewGraagg>[] tGraagg = (BlockEntityType<PreviewGraagg>[]) new BlockEntityType<?>[1];
		tGraagg[0] = BlockEntityType.Builder.of(PreviewGraagg::new, Blocks.BRICKS, Blocks.IRON_BLOCK, Blocks.STONE).build(null);
		sGraaggType = tGraagg[0];
	}

	@Test
	public void boilerPreviewStampsTheWallAndTransmitterBlocks() {
		// the boiler binding judges by is(block) predicates with partBlock == null
		// (TileEntityLargeBoiler getStructurePattern — the BE is display-innocent, the
		// structure check rides the hand loop), so the row re-stamps the display identity
		// from its OWN part list before the seam renders: 35 cells = 9 transmitters + 25
		// walls + the anchor controller (the middle-ring cell behind the anchor)
		GTMultiBlockPattern tRaw = new PreviewBoiler(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		for (GTMultiBlockPattern.Cell tCell : tRaw.cells()) {
			if (!tCell.isHollow()) assertNull(tCell.partBlock, "the raw binding is predicate-only");
		}
		GTMultiBlockPattern tStamped = GT6MultiblockPreviews.withDisplayBlocks(tRaw, Blocks.BRICKS, Blocks.STONE);
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(35, tFill.size(), "the 3x3 bottom + 3x3 middle + two rings + top centre, one relabeled the controller");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(0, 0, -1)),
				"the anchor law paints the controller on the front middle-ring cell");
		assertEquals(Blocks.BRICKS.defaultBlockState(), tFill.get(BlockPos.ZERO),
				"the anchor cell itself stays a wall (the boiler controller is NOT the structure centre)");
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(3, tCounts.size(), "wall + transmitter + controller");
		assertEquals(Integer.valueOf(25), tCounts.get(Blocks.BRICKS), "26 walls minus the controller cell");
		assertEquals(Integer.valueOf(9), tCounts.get(Blocks.STONE), "the 3x3 heat-transmitter floor");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER));
	}

	@Test
	public void cruciblePreviewPinsTheFormedWallForm() {
		// the crucible binding IS the forming declaration (acceptance ②): 24 formingPart
		// walls in three rings + the fail-not-clear hollow pair — zero re-stamp needed.
		// the pattern declares the formed design 4 directly — mb-formed-crucible-wall
		// (merge 39c107bf6) landed: TileEntityCrucible.FORMED_WALL_DESIGN, the net effect
		// of the upstream two-pass check (:119-121 writes 0, :124-128 repaints 4).
		GTMultiBlockPattern tPattern = new PreviewCrucible(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		int tWalls = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.isHollow()) {
				assertTrue(tCell.y == 1 || tCell.y == 2, "the hollow pair is the centre column at y +1/+2");
				continue;
			}
			tWalls++;
			assertSame(Blocks.BRICKS, tCell.partBlock, "the crucible-wall form: one uniform tier wall");
			assertTrue(tCell.forms(), "every wall cell carries the forming expectation");
			assertEquals(4, tCell.design, "the formed wall skin — mb-formed-crucible-wall (39c107bf6) declares TileEntityCrucible.FORMED_WALL_DESIGN");
		}
		assertEquals(24, tWalls, "three 8-cell rings (26 cells total with the hollow pair)");
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, BlockPos.ZERO);
		assertEquals(25, tFill.size(), "every wall renders + the additive controller on the undeclared seat");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(BlockPos.ZERO),
				"the bottom centre — the real controller seat (upstream :118/:140) — paints additively (the D2 ruling)");
		assertSame(Blocks.BRICKS, tFill.get(new BlockPos(0, 0, -1)).getBlock(),
				"the anchor law's (0,0,-1) ring wall is back to being a wall");
		assertFalse(GT6MultiblockPreviews.materialCounts(tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING)
				.isEmpty(), "the shopping list renders");
	}

	@Test
	public void everyBatchARowRendersShapeAndShoppingList() {
		// the per-row acceptance: both thermal ladders table a shape the seam renders
		// (the live suppliers ride the frozen registry — the class doc — so the family
		// shapes stand in over the fixture blocks, the SAME functions the row composes)
		GTMultiBlockPattern tBoilerShape = GT6MultiblockPreviews.withDisplayBlocks(
				new PreviewBoiler(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState()).getStructurePattern(),
				Blocks.BRICKS, Blocks.STONE);
		GTMultiBlockPattern tCrucibleShape = new PreviewCrucible(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		int tBoiler = 0, tCrucible = 0;
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			if (tEntry.name().startsWith("large_boiler_")) {
				tBoiler++;
				assertFalse(GT6MultiblockPreviews.structureBlocks(tBoilerShape, CONTROLLER,
						GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tEntry.name() + " renders a shape");
				assertFalse(GT6MultiblockPreviews.materialCounts(tBoilerShape, CONTROLLER,
						GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tEntry.name() + " renders a shopping list");
			} else if (tEntry.name().startsWith("crucible_")) {
				tCrucible++;
				assertFalse(GT6MultiblockPreviews.structureBlocks(tCrucibleShape, CONTROLLER,
						GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tEntry.name() + " renders a shape");
				assertFalse(GT6MultiblockPreviews.materialCounts(tCrucibleShape, CONTROLLER,
						GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tEntry.name() + " renders a shopping list");
			}
		}
		assertEquals(5, tBoiler, "the five boiler tiers table the boiler shape");
		assertEquals(8, tCrucible, "the eight crucible tiers table the crucible shape");
	}

	// ------------------------------------------------------------------
	// the batch-B energy pins (task mbpreview-data-b-energy): the three
	// converter shell families (the tier wall carried through the binding),
	// the facing-dependent four-way spot check, the implosion shell and the
	// heat exchanger's predicate re-stamp
	// ------------------------------------------------------------------

	@Test
	public void converterTierWallLaddersFollowTheUpstreamDesignColumn() {
		// acceptance ② — the mTurbineWalls face: the tier wall identity per family, pinned
		// on the port registration ladders. The upstream NBT_DESIGN column is the same
		// Dense Wall set for all three families, in the :1254-1267 line order
		// (18022 SS / 18026 Ti / 18023 TungstenSteel / 18025 Ad); the ladders ARE the
		// upstream line order in exactly one place (the batch doctrine)
		List<String> tLadder = List.of("dense_wall_stainless_steel", "dense_wall_titanium",
				"dense_wall_tungstensteel", "dense_wall_adamantium");
		assertEquals(tLadder, GT6Turbines.STEAM_ROWS.stream()
				.map(GT6Turbines.SteamTurbineRow::wallPath).toList(), "the steam tier walls");
		assertEquals(tLadder, GT6Turbines.GAS_ROWS.stream()
				.map(GT6Turbines.GasTurbineRow::wallPath).toList(), "the gas tier walls");
		assertEquals(tLadder, GT6DynamoHousings.DYNAMO_ROWS.stream()
				.map(GT6DynamoHousings.DynamoRow::wallPath).toList(), "the dynamo tier walls");
	}

	@Test
	public void converterBindingCarriesTheWallIdentityAndTheFarPort() {
		// the converter binding is formingPart (the crucible form, NOT the boiler's
		// predicate shape — GTMultiBlockConverter.structurePattern :437-475): the tier
		// wall rides the cells, zero re-stamp. The far plate centre is the single
		// ONLY_ENERGY_OUT port at the far design (3 for the turbines)
		GTMultiBlockPattern tPattern = steamFixture().getStructurePattern();
		int tEnergyOut = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.isHollow(), "the turbine shell declares every cell");
			assertSame(Blocks.BRICKS, tCell.partBlock, "the tier wall identity rides the binding");
			assertTrue(tCell.forms(), "every cell carries the forming expectation");
			if (tCell.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_OUT) tEnergyOut++;
		}
		assertEquals(1, tEnergyOut, "the far plate centre is the single energy-out port");
	}

	@Test
	public void dynamoBindingPinsTheCoilSegment() {
		// the dynamo shape over the same binding: the two middle layers are the 18 Large
		// Copper Coils (LargeDynamo.java:68), the two 3x3 end plates the row wall, the
		// far centre the design-2 energy-out port
		GTMultiBlockPattern tPattern = new PreviewConverter(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState(),
				false, true, 2).getStructurePattern();
		int tCoils = 0, tWalls = 0, tEnergyOut = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.partBlock == Blocks.IRON_BLOCK) tCoils++;
			else if (tCell.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_OUT) tEnergyOut++;
			else tWalls++;
		}
		assertEquals(18, tCoils, "the two middle layers are the coil segment");
		assertEquals(17, tWalls, "the two end plates minus the anchor cell (painted controller)");
		assertEquals(1, tEnergyOut, "the far centre, design 2");
	}

	@Test
	public void converterPatternRotatesThroughTheFourFacings() {
		// acceptance ③ — the facing-dependent spot check, one turbine: the converter
		// binding declares its cells PER FACING (structurePattern rotates the 3x3x4
		// shell, the controller layer frontmost at +1 front, the far plate at -2 front),
		// so every facing must table a consistent anchor: the controller paints the
		// -anchorOffset cell and the energy-out far centre sits collinear OPPOSITE
		// (-2x the controller vector — the back-hole direction follows the facing)
		for (byte tFacing : new byte[] {2, 3, 4, 5}) {
			GTMultiBlockPattern tPattern = steamFixture(tFacing).getStructurePattern();
			Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
					tPattern, CONTROLLER, tFacing);
			assertEquals(36, tFill.size(), "facing " + tFacing + ": the 3x3x4 shell renders (35 walls + the controller)");
			int[] tAnchor = GTMultiBlockPattern.anchorOffset(tFacing);
			BlockPos tControllerCell = new BlockPos(-tAnchor[0], -tAnchor[1], -tAnchor[2]);
			assertEquals(CONTROLLER.defaultBlockState(), tFill.get(tControllerCell),
					"facing " + tFacing + ": the anchor law paints the controller on the front centre cell");
			BlockPos tFarCell = new BlockPos(-2 * tControllerCell.getX(), -2 * tControllerCell.getY(),
					-2 * tControllerCell.getZ());
			int tFarFound = 0;
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
				if (tCell.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_OUT) {
					tFarFound++;
					assertEquals(new BlockPos(tCell.x, tCell.y, tCell.z), tFarCell,
							"facing " + tFacing + ": the far plate is opposite the controller layer");
				}
			}
			assertEquals(1, tFarFound, "facing " + tFacing + ": exactly one energy-out port");
			assertFalse(GT6MultiblockPreviews.materialCounts(tPattern, CONTROLLER, tFacing).isEmpty(),
					"facing " + tFacing + ": the shopping list renders");
		}
	}

	@Test
	public void implosionPreviewRendersTheShellAroundTheHollowCentre() {
		// the :1228 direct collect — the 26 dense-wall cells + the fail-not-clear hollow
		// centre (the +1-Y bake, (0,1,0)); the controller paints the front-bottom-centre
		// shell cell (0,0,-1) — the anchor law IS the true seat (upstream :48/:69), the
		// D2 row declares it explicitly
		GTMultiBlockPattern tPattern = new PreviewImplosion(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -1));
		assertEquals(26, tFill.size(), "25 walls + the controller painted on the anchor cell");
		assertFalse(tFill.containsKey(new BlockPos(0, 1, 0)), "the hollow centre never renders");
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(Integer.valueOf(25), tCounts.get(Blocks.BRICKS), "the 26-cell shell minus the anchor cell");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER), "the controller counts as its own material");
	}

	@Test
	public void heatExchangerPreviewRestampsThePredicateBinding() {
		// the :1245 collect — the binding is predicate-shaped (part() cells,
		// GT6HeatExchangerBlockEntity:266-285): the boiler form, the row re-stamps the
		// display identity from its own part list (wall + transmitter). 17 cells = the
		// y0 wall ring + the transmitter ring + the centre wall
		GTMultiBlockPattern tRaw = new PreviewHeatExchanger(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		for (GTMultiBlockPattern.Cell tCell : tRaw.cells()) {
			assertNull(tCell.partBlock, "the raw binding is predicate-only");
		}
		GTMultiBlockPattern tStamped = GT6MultiblockPreviews.withDisplayBlocks(tRaw, Blocks.BRICKS, Blocks.STONE);
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, BlockPos.ZERO);
		assertEquals(18, tFill.size(), "8 walls + 8 transmitters + the centre wall + the additive controller");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(BlockPos.ZERO),
				"the undeclared bottom-centre seat paints additively (upstream :89-96/:125, the D2 ruling)");
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, BlockPos.ZERO);
		assertEquals(Integer.valueOf(9), tCounts.get(Blocks.BRICKS), "the full 9-wall ring — no controller painted over");
		assertEquals(Integer.valueOf(8), tCounts.get(Blocks.STONE), "the transmitter ring");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER));
	}

	@Test
	public void everyBatchBRowRendersShapeAndShoppingList() {
		// the per-row acceptance: the energy fourteen table shapes the seam renders —
		// the three converter families over their fixtures (the live suppliers ride the
		// frozen registry, the class doc), the implosion shell and the re-stamped HEX
		GTMultiBlockPattern tTurbine = steamFixture().getStructurePattern();
		GTMultiBlockPattern tDynamo = new PreviewConverter(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState(),
				false, true, 2).getStructurePattern();
		GTMultiBlockPattern tImplosion = new PreviewImplosion(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		GTMultiBlockPattern tHex = GT6MultiblockPreviews.withDisplayBlocks(
				new PreviewHeatExchanger(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState()).getStructurePattern(),
				Blocks.BRICKS, Blocks.STONE);
		int tSteam = 0, tGas = 0, tDynamos = 0, tImplosions = 0, tHexs = 0;
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			GTMultiBlockPattern tShape;
			if (tEntry.name().startsWith("steam_turbine_") || tEntry.name().startsWith("gas_turbine_")) {
				tShape = tTurbine;
				if (tEntry.name().startsWith("steam_turbine_")) tSteam++; else tGas++;
			} else if (tEntry.name().startsWith("large_dynamo_")) {
				tShape = tDynamo;
				tDynamos++;
			} else if (tEntry.name().equals("implosion_compressor")) {
				tShape = tImplosion;
				tImplosions++;
			} else if (tEntry.name().equals("large_heat_exchanger")) {
				tShape = tHex;
				tHexs++;
			} else continue;
			assertFalse(GT6MultiblockPreviews.structureBlocks(tShape, CONTROLLER,
					GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tEntry.name() + " renders a shape");
			assertFalse(GT6MultiblockPreviews.materialCounts(tShape, CONTROLLER,
					GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tEntry.name() + " renders a shopping list");
		}
		assertEquals(4, tSteam, "the four steam tiers table the turbine shape");
		assertEquals(4, tGas, "the four gas tiers table the turbine shape");
		assertEquals(4, tDynamos, "the four dynamo housings table the dynamo shape");
		assertEquals(1, tImplosions, "the implosion compressor tables its shell");
		assertEquals(1, tHexs, "the heat exchanger tables its rings");
	}

	/** The offline steam turbine fixture — the fluid-column shape at the display facing (north). */
	private static PreviewConverter steamFixture() {
		return steamFixture(GT6MultiblockPreviews.DISPLAY_FACING);
	}

	/** The offline steam turbine fixture at an explicit facing (the public mFacing field, base default 2). */
	private static PreviewConverter steamFixture(byte aFacing) {
		PreviewConverter tFixture = new PreviewConverter(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState(),
				true, false, 3);
		tFixture.mFacing = aFacing;
		return tFixture;
	}

	// ------------------------------------------------------------------
	// the batch-C processing pins (task mbpreview-data-c-processing): the
	// ROWS anti-drift pin, the twelve W3 identity anchors over the REAL
	// StructureKind builds, and the distillation tower's binding + facing
	// hole spot check
	// ------------------------------------------------------------------

	@Test
	public void processingRowLadderMirrorsTheRegistrationRows() {
		// acceptance ② — the anti-handwriting-drift pin: the table's batch-C segment IS
		// the ladder traversal (GT6LargeMachines.ROWS = the upstream :1229-1240 line
		// order, GT6Distillation.ROWS = :1226-1227), not a transcribed list — a hand-
		// written row in the table (or a typo) breaks the mirror, the census pin's
		// hand-written list breaks with it
		List<String> tLadder = new ArrayList<>();
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) tLadder.add(tRow.path());
		for (GT6Distillation.TowerRow tRow : GT6Distillation.ROWS) tLadder.add(tRow.path());
		List<String> tNames = GT6MultiblockPreviews.entries().stream()
				.map(GT6MultiblockPreviews.Entry::name).toList();
		assertTrue(tNames.size() >= tLadder.size(), "the table carries the batch-C segment");
		// located by the segment head since batch D1 tail-appends after it (the segment
		// must stay CONTIGUOUS and ladder-ordered — the anti-handwriting-drift semantics)
		int tStart = tNames.indexOf(tLadder.get(0));
		assertTrue(tStart >= 0, "the batch-C segment sits in the table");
		assertEquals(tLadder, tNames.subList(tStart, tStart + tLadder.size()),
				"the batch-C segment mirrors the registration ladder traversal");
		assertEquals(12, GT6LargeMachines.ROWS.size(), "the W3 twelve");
		assertEquals(2, GT6Distillation.ROWS.size(), "the two towers");
	}

	@Test
	public void everyProcessingMachineCarriesItsWallAndSpecialIdentity() {
		// acceptance ③ — per-machine identity anchors: each of the twelve builds its REAL
		// family geometry (StructureKind.build — the same functions the binding composes;
		// fixture part identities stand in for the frozen-registry handles, the class
		// doc): the wall identity renders for ALL twelve, and each row's DECLARED special
		// blocks ride along — the Fermenter's transmitter base, the Oven's coil ring, the
		// Sluice trough parts, the Crusher/Shredder wheels/blades (the innerPath rows)
		int tAnchored = 0, tInner = 0, tBase = 0;
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) {
			Block tWall = Blocks.BRICKS;
			Block tInnerBlock = tRow.innerPath() != null ? Blocks.IRON_BLOCK : tWall;
			Block tBaseBlock = tRow.basePath() != null ? Blocks.STONE : null;
			GTMultiBlockPattern tPattern = tRow.structure().build(tRow, GT6MultiblockPreviews.DISPLAY_FACING,
					tWall, tInnerBlock, tInnerBlock, tBaseBlock);
			Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(tPattern, CONTROLLER,
					GT6MultiblockPreviews.DISPLAY_FACING);
			assertFalse(tCounts.isEmpty(), tRow.path() + " renders a shopping list");
			assertTrue(tCounts.getOrDefault(tWall, 0) > 0, tRow.path() + " carries its wall identity");
			if (tRow.innerPath() != null) {
				assertTrue(tCounts.getOrDefault(Blocks.IRON_BLOCK, 0) > 0, tRow.path() + " carries its inner part");
				tInner++;
			}
			if (tRow.basePath() != null) {
				assertTrue(tCounts.getOrDefault(Blocks.STONE, 0) > 0, tRow.path() + " carries its transmitter base");
				tBase++;
			}
			tAnchored++;
		}
		assertEquals(12, tAnchored, "the W3 twelve each anchored");
		assertEquals(4, tInner, "the inner-part rows: Oven / Sluice / Crusher / Shredder");
		assertEquals(1, tBase, "the transmitter-base row: the Fermenter");
	}

	@Test
	public void distillationTowerPreviewPinsTheBindingAndTheFacingHole() {
		// the :1226-1227 direct collects — the binding is formingPart (81 declared cells:
		// 9 transmitters at y-1 + 72 column cells at y0..y7, GT6Distillation :372), zero
		// re-stamp. The facing spot-check (the research risk face): the design-1 hole
		// column rides the +anchor direction, the anchor law paints the controller on the
		// -anchor cell — collinear opposite, the hole follows the facing. (The D2 row
		// declares that same cell — (0,0,-1) in the canonical north frame; the facing
		// loop here rides the 3-arg anchor law, which is the facing-rotated form of it.)
		PreviewTower tFixture = towerFixture();
		GTMultiBlockPattern tPattern = tFixture.getStructurePattern();
		assertEquals(81, tPattern.cells().size(), "9 transmitters + 72 column cells");
		int tHoles = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.isHollow(), "the tower declares every cell");
			assertNotNull(tCell.partBlock, "the binding is formingPart (zero re-stamp)");
			if (tCell.design == 1) tHoles++;
		}
		assertEquals(8, tHoles, "the back-centre hole column, one cell per y");
		for (byte tFacing : new byte[] {2, 3, 4, 5}) {
			PreviewTower tRotated = towerFixture();
			tRotated.mFacing = tFacing;
			GTMultiBlockPattern tRotatedPattern = tRotated.getStructurePattern();
			Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(tRotatedPattern, CONTROLLER, tFacing);
			assertEquals(81, tFill.size(), "facing " + tFacing + ": the full column renders");
			int[] tAnchor = GTMultiBlockPattern.anchorOffset(tFacing);
			assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(-tAnchor[0], -tAnchor[1], -tAnchor[2])),
					"facing " + tFacing + ": the anchor law paints the controller");
			int tHoleAtAnchor = 0;
			for (GTMultiBlockPattern.Cell tCell : tRotatedPattern.cells()) {
				if (tCell.design == 1) {
					assertEquals(tAnchor[0], tCell.x, "facing " + tFacing + ": the hole column rides +anchor x");
					assertEquals(tAnchor[2], tCell.z, "facing " + tFacing + ": the hole column rides +anchor z");
					tHoleAtAnchor++;
				}
			}
			assertEquals(8, tHoleAtAnchor, "facing " + tFacing + ": the hole column follows the facing");
			assertFalse(GT6MultiblockPreviews.materialCounts(tRotatedPattern, CONTROLLER, tFacing).isEmpty(),
					"facing " + tFacing + ": the shopping list renders");
		}
	}

	/** The offline distillation tower fixture at the display facing (north). */
	private static PreviewTower towerFixture() {
		PreviewTower tFixture = new PreviewTower(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState());
		tFixture.mFacing = GT6MultiblockPreviews.DISPLAY_FACING;
		return tFixture;
	}

	// ------------------------------------------------------------------
	// the batch-D1 special pins (task mbpreview-data-d1-special): the tank
	// valve's radius-parametrized binding — the 5x5x5 shell declaration
	// replacing the retired :405 null ruling — and the 25-row family
	// coverage over the GT6Tanks registration ladder
	// ------------------------------------------------------------------

	@Test
	public void tankValveRadius2PatternPinsTheShellAroundTheHollow() {
		// acceptance ② — the OLD :405 ruling returned null for radius 2 (the
		// "distance-1 anchor" reading); the radius parametrization declares the shell in
		// the frame shift -(r-1)*OFF[facing] — the frame that walks the checker's
		// cellOffset contract (GTMultiBlockStructureChecker worldCell) AND puts the valve
		// seat exactly on the anchor law: the seat's pattern cell collapses to OFF[facing]
		// = -anchorOffset for EVERY radius and facing. 125 cells = the 5x5x5 shell (98
		// formingPart walls, the valve seat included — the self-cell arm) around the
		// inner 3x3x3 hollow (27, the check's :66 i*i<=1 gate)
		GTMultiBlockPattern tPattern = new PreviewTank(2, BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		assertNotNull(tPattern, "the 5x5x5 binds a pattern (the :405 null is retired)");
		int tForming = 0, tHollow = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.isHollow()) {
				tHollow++;
				assertTrue(Math.abs(tCell.x) <= 1 && Math.abs(tCell.y) <= 1 && tCell.z >= 0 && tCell.z <= 2,
						"the hollow rides the frame shift: the inner 3x3x3 moved +1 z at the north display");
				continue;
			}
			tForming++;
			assertSame(Blocks.BRICKS, tCell.partBlock, "the row wall identity rides the cells (formingPart, zero re-stamp)");
			assertTrue(tCell.forms(), "every shell cell carries the forming expectation");
			assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID, tCell.usage, "the wall usage is ONLY_FLUID (the :69 mode)");
			assertEquals(0, tCell.design, "the design argument is the literal 0 (:69)");
		}
		assertEquals(125, tPattern.cells().size(), "the full (2r+1)^3 declaration");
		assertEquals(98, tForming, "the 5x5x5 shell: 125 - 27 hollow");
		assertEquals(27, tHollow, "the inner 3x3x3 hollow");
		// the frame spot check: the checker walks world = valve + cell - OFF[facing]; the
		// hand check's top-centre shell target sits at valve + (0,2,+2) for the north
		// facing (centre + (0,2,0), centre = valve + 2 z) — pattern cell (0,2,+1)
		assertArrayEquals(new int[] {0, 2, 2}, GTMultiBlockPattern.cellOffset((byte) 2, 0, 2, 1),
				"the frame shift -(r-1)*OFF lands the shell on the hand check's cells");
		// the display face: the anchor law paints the CONTROLLER on the valve seat
		// (0,0,-1) — the front shell face centre, exactly where the valve sits
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(98, tFill.size(), "every shell cell renders (the valve seat relabeled the controller)");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(0, 0, -1)),
				"the anchor law lands on the valve seat for radius 2 too");
		assertFalse(tFill.containsKey(new BlockPos(0, 0, 1)), "the hollow centre (the frame-shifted one) never renders");
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(Integer.valueOf(97), tCounts.get(Blocks.BRICKS), "the 98 shell walls minus the valve-seat cell");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER));
	}

	@Test
	public void tankValveRadius1PatternStaysTheUpstreamLoop() {
		// the r=1 regression: the parametrization collapses to the upstream :66 loop
		// (26 formingPart walls + the hollow centre — byte-identical shape, 27 cells)
		GTMultiBlockPattern tPattern = new PreviewTank(1, BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		assertNotNull(tPattern, "the 3x3x3 keeps its binding");
		assertEquals(27, tPattern.cells().size(), "26 forming cells + the hollow centre (the GT6TankValveFamilyTest pin)");
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(26, tFill.size(), "26 walls render, one relabeled the controller");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(0, 0, -1)),
				"the valve seat rides the anchor law unchanged");
	}

	@Test
	public void everyTankValveRowRendersItsShellAndShoppingList() {
		// the per-row acceptance over the GT6Tanks registration ladder (the batch-A/B/C
		// family doctrine): all 25 rows table a shape the seam renders — the 13 size-3
		// rows the 27-cell shell, the 12 size-5 rows the 125-cell shell — and the table
		// tail segment mirrors the ladder traversal (the anti-handwriting-drift pin)
		assertEquals(25, GT6Tanks.ROWS.size(), "the 25-valve registration face (:1195-1222)");
		int tSmall = 0, tLarge = 0;
		List<String> tTankNames = GT6MultiblockPreviews.entries().stream()
				.map(GT6MultiblockPreviews.Entry::name)
				.filter(tName -> tName.startsWith("tank_")).toList();
		assertEquals(GT6Tanks.ROWS.stream().map(GT6Tanks.TankValveRow::path).toList(), tTankNames,
				"the tank segment mirrors the registration ladder traversal");
		for (GT6Tanks.TankValveRow tRow : GT6Tanks.ROWS) {
			int tRadius = tRow.size() / 2;
			GTMultiBlockPattern tShape = new PreviewTank(tRadius, BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
					.getStructurePattern();
			assertNotNull(tShape, tRow.path() + " binds its shell");
			assertEquals(tRadius == 1 ? 27 : 125, tShape.cells().size(), tRow.path() + " declares the full shell");
			assertFalse(GT6MultiblockPreviews.structureBlocks(tShape, CONTROLLER,
					GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tRow.path() + " renders a shape");
			assertFalse(GT6MultiblockPreviews.materialCounts(tShape, CONTROLLER,
					GT6MultiblockPreviews.DISPLAY_FACING).isEmpty(), tRow.path() + " renders a shopping list");
			if (tRadius == 1) tSmall++; else tLarge++;
		}
		assertEquals(13, tSmall, "wood + 6 plain small + 6 dense small ride radius 1");
		assertEquals(12, tLarge, "6 plain large + 6 dense large ride radius 2");
	}

	@Test
	public void bedrockDrillPreviewPinsTheWallHeadAndProbeFloor() {
		// acceptance ③ — the :1283 row's display declaration over the null BE binding
		// (the existence-probe seam, TileEntityBedrockDrill:179-181): 54 cells = the y-5
		// probe floor (9 bedrock) + the y-4 drill-head layer (9) + the four wall layers
		// y-3..y0 (36) — the SAME function the row composes (the fixture identities ride
		// the parameters, the batch-C doctrine)
		GTMultiBlockPattern tPattern = GT6MultiblockPreviews.bedrockDrillShape(Blocks.BRICKS, Blocks.STONE);
		int tFloor = 0, tHeads = 0, tWalls = 0, tEnergy = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.y == -5) {
				tFloor++;
				assertSame(Blocks.BEDROCK, tCell.partBlock, "the y-5 probe floor declares the bedrock identity");
			} else if (tCell.y == -4) {
				tHeads++;
				assertSame(Blocks.BRICKS, tCell.partBlock, "the drill-head identity rides y-4");
			} else {
				tWalls++;
				assertSame(Blocks.STONE, tCell.partBlock, "the Dense Titanium Wall identity rides y-3..y0");
			}
			if (tCell.y == -1 && (tCell.x == 0) != (tCell.z == 0)) {
				tEnergy++;
				assertEquals(3, tCell.design, "the energy ring carries design 3 (the :113 column)");
				assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tCell.usage, "the edge cells are the ONLY_ENERGY_IN faces");
			}
		}
		assertEquals(54, tPattern.cells().size(), "9 floor + 9 heads + 36 walls");
		assertEquals(9, tFloor, "the 3x3 probe floor");
		assertEquals(9, tHeads, "the 3x3 drill-head layer");
		assertEquals(36, tWalls, "the four wall layers");
		assertEquals(4, tEnergy, "the XOR ring: the four edge cells");
		assertFalse(GT6MultiblockPreviews.structureBlocks(tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING)
				.isEmpty(), "the shape renders");
		assertFalse(GT6MultiblockPreviews.materialCounts(tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING)
				.isEmpty(), "the shopping list renders");
	}

	@Test
	public void graaggPreviewPinsTheCornerlessBaseAndThePole() {
		// acceptance ③ — the :1280 direct collect (TileEntityVonDaGraagg:173, the
		// zero-offset controller anchor): 64 cells = the cornerless 5x5x2 base (21 cells
		// x 2 layers, the |i*j|<4 gate) ALL ONLY_ENERGY_IN + the 5m coil pole (5, NOTHING)
		// + the 17-cell top box (1 centre + the y+6 cornerless ring 8 + the y+5/y+7
		// crosses 4+4)
		GTMultiBlockPattern tPattern = new PreviewGraagg(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		int tBase = 0, tCoils = 0, tTop = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.isHollow(), "the Graagg declares every cell");
			assertNotNull(tCell.partBlock, "the binding is formingPart (zero re-stamp)");
			if (tCell.partBlock == Blocks.BRICKS) {
				tBase++;
				assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tCell.usage, "the base walls are ONLY_ENERGY_IN (the :72-73 columns)");
			} else if (tCell.partBlock == Blocks.IRON_BLOCK) {
				tCoils++;
				assertEquals(0, tCell.x, "the pole is the centre column");
				assertTrue(tCell.y >= 2 && tCell.y <= 6, "the pole spans y+2..y+6");
			} else {
				tTop++;
			}
		}
		assertEquals(42, tBase, "the cornerless 5x5x2 base: 21 cells x 2 layers");
		assertEquals(5, tCoils, "the 5m coil pole");
		assertEquals(17, tTop, "the top box: 1 centre + 8 ring + 4+4 crosses");
		assertEquals(64, tPattern.cells().size(), "42 + 5 + 17");
		// the corner gate: neither base layer declares a |i|=|j|=2 corner
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.y <= 1 && Math.abs(tCell.x) == 2 && Math.abs(tCell.z) == 2, "the base corners are cut (|i*j|<4)");
		}
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, BlockPos.ZERO);
		assertEquals(64, tFill.size(), "every declared cell renders");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(BlockPos.ZERO),
				"the controller paints its zero-offset base-centre seat (upstream :66-93, the D2 ruling)");
		assertSame(Blocks.BRICKS, tFill.get(new BlockPos(0, 0, -1)).getBlock(),
				"the anchor law's (0,0,-1) stays a base wall");
		assertFalse(GT6MultiblockPreviews.materialCounts(tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING)
				.isEmpty(), "the shopping list renders");
	}

	@Test
	public void controllerSeatsPinTheUpstreamTrueCells() {
		// task mbpreview-data-d2-authored, user ruling A1: the seam paints the controller
		// on the row's TRUE controller cell, per-machine upstream evidence (the anchor law
		// (0,0,-1) stays the FALLBACK for the rows whose seat IS -anchor). Pinned over the
		// fixture shapes with the rows' declared cells (the red commit drove these same
		// assertions through the seat-less 3-arg seam and they failed for the four
		// mispainted machines).
		byte tFacing = GT6MultiblockPreviews.DISPLAY_FACING;
		// the crucible (8 tiers): the walk SKIPS the bottom centre (MultiTileEntityCrucible
		// :118 `if (i != 0 || j != 0)`) — "Main at Bottom-Center" (:140): seat (0,0,0),
		// painted ADDITIVELY (the pattern never declares it)
		Map<BlockPos, BlockState> tCrucible = GT6MultiblockPreviews.structureBlocks(
				new PreviewCrucible(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState()).getStructurePattern(),
				CONTROLLER, tFacing, BlockPos.ZERO);
		assertEquals(CONTROLLER.defaultBlockState(), tCrucible.get(BlockPos.ZERO),
				"the crucible controller paints its bottom-centre seat (upstream :118/:140)");
		assertEquals(25, tCrucible.size(), "24 walls + the additive controller (the old anchor paint is gone)");
		assertSame(Blocks.BRICKS.defaultBlockState().getBlock(), tCrucible.get(new BlockPos(0, 0, -1)).getBlock(),
				"the anchor law's (0,0,-1) ring wall is back to being a wall");
		// the heat exchanger: the y0 ring declares 8, the centre stays undeclared
		// (MultiTileEntityLargeHeatExchanger :89-96, zero-offset walk :85) — "with Main
		// inside" (:125): seat (0,0,0), controller-relative frame, additive
		Map<BlockPos, BlockState> tHex = GT6MultiblockPreviews.structureBlocks(
				GT6MultiblockPreviews.withDisplayBlocks(
						new PreviewHeatExchanger(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState()).getStructurePattern(),
						Blocks.BRICKS, Blocks.STONE),
				CONTROLLER, tFacing, BlockPos.ZERO);
		assertEquals(CONTROLLER.defaultBlockState(), tHex.get(BlockPos.ZERO),
				"the HEX controller paints its undeclared bottom-centre seat (upstream :89-96/:125)");
		assertEquals(18, tHex.size(), "8 walls + 8 transmitters + the centre wall + the additive controller");
		// the implosion compressor: the shell cube centres on controller + anchor + up
		// (:48 tY = yCoord+1) — "Main Block centered on Side-Bottom" (:69): the controller
		// IS the front-bottom-centre shell cell = pattern cell (0,0,-1) — the anchor law
		// already lands it (the B-batch legacy-① suspicion dissolves: no seat change)
		Map<BlockPos, BlockState> tImplosion = GT6MultiblockPreviews.structureBlocks(
				new PreviewImplosion(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState()).getStructurePattern(),
				CONTROLLER, tFacing, new BlockPos(0, 0, -1));
		assertEquals(CONTROLLER.defaultBlockState(), tImplosion.get(new BlockPos(0, 0, -1)),
				"the implosion seat (0,0,-1) IS the anchor law (upstream :48/:69)");
		assertEquals(26, tImplosion.size(), "the declared seat paints over its shell wall — no additive cell");
		// the distillation towers: the pattern frame is the centre one cell behind the
		// facing (upstream :53, port centre()) and the walk DECLARES the controller seat
		// as a 18102 part cell — the anchor law (0,0,-1) is exactly it, now declared
		// (the canonical north form of the per-facing cell)
		Map<BlockPos, BlockState> tTower = GT6MultiblockPreviews.structureBlocks(
				towerFixture().getStructurePattern(), CONTROLLER, tFacing, new BlockPos(0, 0, -1));
		assertEquals(CONTROLLER.defaultBlockState(), tTower.get(new BlockPos(0, 0, -1)),
				"the tower seat (0,0,-1) IS the anchor law (upstream :53, the declared part cell)");
		// the Von da Graagg: the ZERO-OFFSET walk (upstream :66-93 — no facing
		// displacement, the port patternWalkFacing 0) — the cornerless base loop declares
		// the controller's own cell: seat (0,0,0), NOT the anchor law's (0,0,-1)
		Map<BlockPos, BlockState> tGraagg = GT6MultiblockPreviews.structureBlocks(
				new PreviewGraagg(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState()).getStructurePattern(),
				CONTROLLER, tFacing, BlockPos.ZERO);
		assertEquals(CONTROLLER.defaultBlockState(), tGraagg.get(BlockPos.ZERO),
				"the Graagg controller paints its zero-offset seat (upstream :66-93)");
		assertSame(Blocks.BRICKS.defaultBlockState().getBlock(), tGraagg.get(new BlockPos(0, 0, -1)).getBlock(),
				"the anchor law's (0,0,-1) stays a base wall for the Graagg");
		// the bedrock drill: the ZERO-OFFSET walk (upstream :87-117
		// checkAndSetTargetOffset) — the y0 loop includes the controller's own cell, the
		// display declaration carries it: seat (0,0,0)
		Map<BlockPos, BlockState> tDrill = GT6MultiblockPreviews.structureBlocks(
				GT6MultiblockPreviews.bedrockDrillShape(Blocks.BRICKS, Blocks.STONE), CONTROLLER, tFacing, BlockPos.ZERO);
		assertEquals(CONTROLLER.defaultBlockState(), tDrill.get(BlockPos.ZERO),
				"the drill controller paints its zero-offset self-cell (upstream :87-117)");
	}

	@Test
	public void rowSeatDeclarationsPinTheUpstreamTrueCells() {
		// the D2 seat TABLE on the rows themselves (acceptance ②): the declared cell per
		// machine — the six retro rows and the four authored ones; every other row keeps
		// the anchor-law null. Upstream anchors: crucible MultiTileEntityCrucible :118
		// (skip)+:140; HEX :89-96+:125; implosion :48+:69; tower :53; Graagg :66-93;
		// drill :87-117; massfab :46 (the corner arithmetic, seat = front-bottom-centre);
		// fusion :48 (the 2x-anchor core, seat = the front axis tip); logistics :110;
		// rod :76-84 (zero-offset, "Main at Center" :89)
		Map<String, BlockPos> tSeats = new java.util.LinkedHashMap<>();
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			tSeats.put(tEntry.name(), tEntry.controllerCell());
		}
		BlockPos tOrigin = BlockPos.ZERO;
		BlockPos tFront = new BlockPos(0, 0, -1);
		BlockPos tCoreFront = new BlockPos(0, 0, -2);
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) {
			assertEquals(tOrigin, tSeats.get(tRow.path()), tRow.path() + ": the bottom-centre seat");
		}
		assertEquals(tFront, tSeats.get("implosion_compressor"), "the implosion front-bottom-centre shell cell");
		assertEquals(tOrigin, tSeats.get("large_heat_exchanger"), "the HEX undeclared bottom centre");
		assertEquals(tFront, tSeats.get("distillation_tower"), "the tower's declared part cell at -anchor");
		assertEquals(tFront, tSeats.get("cryo_distillation_tower"), "the cryo twin");
		assertEquals(tOrigin, tSeats.get("von_da_graagg"), "the Graagg zero-offset base centre");
		assertEquals(tOrigin, tSeats.get("bedrock_drill"), "the drill zero-offset self-cell");
		assertEquals(tCoreFront, tSeats.get("large_massfab"), "the massfab front-bottom-centre of the 5x5x5");
		assertEquals(tCoreFront, tSeats.get("fusion_reactor"), "the fusion front axis-tip shell cell");
		assertEquals(tCoreFront, tSeats.get("logistics_core"), "the logistics front-centre vent cell");
		assertEquals(tOrigin, tSeats.get("multiblock_lightning_rod"), "the rod bottom-centre (zero-offset)");
		// the anchor-law rows: null keeps the fallback (spot representatives per family)
		assertNull(tSeats.get("multiblock_coke_oven"), "the coke oven seat IS -anchor (the anchor law)");
		assertNull(tSeats.get("large_boiler_stainless_steel"), "the boiler keeps the anchor law");
		assertNull(tSeats.get("steam_turbine_magnalium"), "the converters keep the anchor law");
		assertNull(tSeats.get("tank_wood"), "the tank valves keep the anchor law (the seat law by construction)");
		int tDeclared = 0;
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			if (tEntry.controllerCell() != null) tDeclared++;
		}
		assertEquals(18, tDeclared, "8 crucibles + implosion + HEX + 2 towers + Graagg + drill + the D2 four");
	}

	// ------------------------------------------------------------------
	// the batch-D2 authored pins (task mbpreview-data-d2-authored): the
	// three quota-machine display shapes (the cell ledgers counted
	// item-by-item against the upstream walks), the rod's minimal
	// representative, and the predicate-candidate display face
	// ------------------------------------------------------------------

	/** The fusion fixture identities (distinct vanilla blocks per upstream part id). */
	private static GTMultiBlockPattern fusionFixture() {
		return GT6MultiblockPreviews.fusionShape(Blocks.IRON_BLOCK, Blocks.BRICKS, Blocks.STONE,
				Blocks.GLOWSTONE, Blocks.OBSIDIAN, List.of(Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK));
	}

	@Test
	public void fusionShapePinsTheCellLedger() {
		// acceptance ③ — the :47-126 walk transcribed: 887 cells = the 5x5x5 core (27 PU
		// predicate + 48 walls incl. the (0,0,-2) front axis tip + 50 vents) + the 6 arms
		// (the facing -z pair dropped, :74-89) + the OCTAGONS base (216 glass + 360
		// glass/coil + 180 glass/coil/stainless — TileEntityFusionReactor.OCTAGONS verbatim)
		GTMultiBlockPattern tPattern = fusionFixture();
		int tPu = 0, tWall = 0, tVent = 0, tGlass = 0, tCoil = 0, tSs = 0, tArms = 0;
		int tOutTips = 0, tInRing = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.isHollow(), "the fusion declares every cell");
			Block tBlock = tCell.partBlock;
			if (tBlock == null) {
				tPu++;
				assertTrue(tCell.predicate.test(Blocks.GOLD_BLOCK.defaultBlockState())
						&& tCell.predicate.test(Blocks.LAPIS_BLOCK.defaultBlockState())
						&& tCell.predicate.test(Blocks.REDSTONE_BLOCK.defaultBlockState()),
						"the PU core cells accept all three quadcores (the :56-64 chain)");
				continue;
			}
			if (tCell.y == 0 && ((Math.abs(tCell.x) == 3 || Math.abs(tCell.x) == 4) && tCell.z == 0
					|| (Math.abs(tCell.z) == 3 || Math.abs(tCell.z) == 4) && tCell.x == 0)) tArms++; // the ±3/±4 arms
			if (tBlock == Blocks.IRON_BLOCK) tWall++;
			else if (tBlock == Blocks.OBSIDIAN) tVent++;
			else if (tBlock == Blocks.BRICKS) tGlass++;
			else if (tBlock == Blocks.GLOWSTONE) tCoil++;
			else if (tBlock == Blocks.STONE) tSs++;
			if (tCell.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_OUT) {
				tOutTips++;
				assertEquals(2, tCell.design, "the ring tips are the design-2 OUT faces (:97)");
			} else if (tCell.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_IN && tBlock == Blocks.BRICKS) {
				tInRing++;
				assertEquals(5, tCell.design, "the idle ring input design (:99, the mActive flip is the BE's)");
			}
		}
		assertEquals(887, tPattern.cells().size(), "the full reactor: 125 core + 6 arms + 756 base");
		assertEquals(27, tPu, "the d²<4 quadcore quota cells");
		assertEquals(54, tWall, "48 shell (axis tips included) + 6 arms = the 53 blocks + the controller seat");
		assertEquals(50, tVent, "the ventilation shell (the :68 fifty)");
		assertEquals(576, tGlass, "the 216 + 288 + 72 Tungstensteel 'glass' (the :195 tooltip ledger)");
		assertEquals(144, tCoil, "the 72 + 72 Iridium coils (:196)");
		assertEquals(36, tSs, "the Stainless Steel wall ring (:196)");
		assertEquals(6, tArms, "the ±3/±4 arms minus the facing pair");
		assertEquals(4, tOutTips, "the four orthogonal ring tips");
		assertEquals(68, tInRing, "the 72-ring minus its four tips");
		// the dropped facing arm: no cells at (0,0,-3)/(0,0,-4); the back pair declared
		assertNull(GT6MultiblockPreviews.cellAt(tPattern, new BlockPos(0, 0, -3)),
				"the facing -z arm is the skipped one (:74-89 at north)");
		assertNull(GT6MultiblockPreviews.cellAt(tPattern, new BlockPos(0, 0, -4)), "the facing arm's far cell too");
		assertNotNull(GT6MultiblockPreviews.cellAt(tPattern, new BlockPos(0, 0, 4)), "the back +z arm rides");
	}

	@Test
	public void fusionPreviewRendersTheStampedLedgerAndTheFrontSeat() {
		// the row's composition: withDisplayBlocks stamps the PU core to its FIRST
		// candidate (the r11 Q3 degrade), the seat (0,0,-2) paints the controller over
		// the front axis-tip wall — the ledger closes to the :195-197 tooltip counts
		GTMultiBlockPattern tStamped = GT6MultiblockPreviews.withDisplayBlocks(fusionFixture(),
				Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK);
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2));
		assertEquals(887, tFill.size(), "every declared cell renders");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(new BlockPos(0, 0, -2)),
				"the controller paints the front axis tip — the 2x-anchor seat (upstream :48)");
		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), tFill.get(BlockPos.ZERO),
				"the core centre renders the FIRST quadcore candidate");
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2));
		assertEquals(Integer.valueOf(53), tCounts.get(Blocks.IRON_BLOCK), "the 54 declared walls minus the seat");
		assertEquals(Integer.valueOf(50), tCounts.get(Blocks.OBSIDIAN), "the fifty vents");
		assertEquals(Integer.valueOf(576), tCounts.get(Blocks.BRICKS), "the glass ledger");
		assertEquals(Integer.valueOf(144), tCounts.get(Blocks.GLOWSTONE), "the coil ledger");
		assertEquals(Integer.valueOf(36), tCounts.get(Blocks.STONE), "the stainless ledger");
		assertEquals(Integer.valueOf(27), tCounts.get(Blocks.GOLD_BLOCK), "the core renders as 27 first candidates");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER));
	}

	@Test
	public void massfabShapePinsTheCellLedger() {
		// acceptance ③ — the :45-224 walk: 150 cells = 98 Dense Lead Walls + 26 Osmium
		// Coils + the fail-not-clear AIR centre + 16 vents + 1 versatile + the 8-cell
		// 4+4 quota ring (the port class doc's own arithmetic; the upstream :227 tooltip
		// says 97 — its own miscount, the checks walk 98)
		GTMultiBlockPattern tPattern = GT6MultiblockPreviews.massfabShape(Blocks.IRON_BLOCK, Blocks.GLOWSTONE,
				Blocks.OBSIDIAN, Blocks.GOLD_BLOCK, List.of(Blocks.REDSTONE_BLOCK, Blocks.LAPIS_BLOCK));
		int tWall = 0, tCoil = 0, tVent = 0, tVersatile = 0, tQuota = 0, tHollow = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.isHollow()) {
				tHollow++;
				assertEquals(new BlockPos(0, 2, 0), new BlockPos(tCell.x, tCell.y, tCell.z),
						"the exact centre must stay air (:114)");
				continue;
			}
			if (tCell.partBlock == Blocks.IRON_BLOCK) tWall++;
			else if (tCell.partBlock == Blocks.GLOWSTONE) tCoil++;
			else if (tCell.partBlock == Blocks.OBSIDIAN) tVent++;
			else if (tCell.partBlock == Blocks.GOLD_BLOCK) tVersatile++;
			else {
				tQuota++;
				assertEquals(5, tCell.y, "the quota ring rides the top layer");
				assertTrue(Math.abs(tCell.x) <= 1 && Math.abs(tCell.z) <= 1, "the inner 3x3 minus the centre");
			}
		}
		assertEquals(150, tPattern.cells().size(), "98 + 26 + 1 air + 16 + 1 + 8");
		assertEquals(98, tWall, "the lead walls (25+16+16+16+25)");
		assertEquals(26, tCoil, "the osmium coils (9+8+9)");
		assertEquals(16, tVent, "the 5x5 ring's 16 vents (:180-195)");
		assertEquals(1, tVersatile, "the centre versatile PU (:197)");
		assertEquals(8, tQuota, "the Control/Conversion quota ring (:201-217)");
		assertEquals(1, tHollow, "the air centre");
		// the stamped render: the seat (0,0,-2) paints the controller over the
		// front-bottom-centre wall, the quota ring stamps its first candidate
		GTMultiBlockPattern tStamped = GT6MultiblockPreviews.withDisplayBlocks(tPattern,
				Blocks.REDSTONE_BLOCK, Blocks.LAPIS_BLOCK);
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2));
		assertEquals(149, GT6MultiblockPreviews.structureBlocks(tStamped, CONTROLLER,
				GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2)).size(), "149 render (the air stays air)");
		assertEquals(Integer.valueOf(97), tCounts.get(Blocks.IRON_BLOCK), "98 walls minus the seat cell");
		assertEquals(Integer.valueOf(26), tCounts.get(Blocks.GLOWSTONE));
		assertEquals(Integer.valueOf(16), tCounts.get(Blocks.OBSIDIAN));
		assertEquals(Integer.valueOf(1), tCounts.get(Blocks.GOLD_BLOCK));
		assertEquals(Integer.valueOf(8), tCounts.get(Blocks.REDSTONE_BLOCK), "the ring stamps the FIRST candidate");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER), "the front-bottom-centre seat");
	}

	@Test
	public void logisticsCoreShapePinsTheCellLedger() {
		// acceptance ③ — the :109-147 walk: 125 cells = the 27 CPU predicate cells of
		// d²<4 (:119-133, the six candidates incl. the wall cheapskate arm) + the 44
		// walls of d²>6 (:137-138) + the 54 vents between (:139-140, the seat included)
		GTMultiBlockPattern tPattern = GT6MultiblockPreviews.logisticsCoreShape(Blocks.IRON_BLOCK, Blocks.OBSIDIAN,
				List.of(Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.DIAMOND_BLOCK,
						Blocks.EMERALD_BLOCK));
		int tPu = 0, tWall = 0, tVent = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.isHollow(), "the core declares every cell");
			if (tCell.partBlock == null) {
				tPu++;
				assertTrue(tCell.predicate.test(Blocks.IRON_BLOCK.defaultBlockState()),
						"the wall substitution is the last candidate (:132-133)");
				continue;
			}
			if (tCell.partBlock == Blocks.IRON_BLOCK) {
				tWall++;
				assertEquals(MultiBlockPartBlockEntity.ONLY_LOGISTICS & MultiBlockPartBlockEntity.ONLY_ENERGY_IN,
						tCell.usage, "the shell faces (:138)");
			} else {
				tVent++;
				assertEquals(MultiBlockPartBlockEntity.ONLY_LOGISTICS, tCell.usage, "the inner faces (:140)");
			}
		}
		assertEquals(125, tPattern.cells().size(), "27 + 44 + 54");
		assertEquals(27, tPu, "the d²<4 core");
		assertEquals(44, tWall, "the d²>6 shell");
		assertEquals(54, tVent, "the between shell — the 53 blocks + the controller seat");
		// the stamped render: the front-centre vent cell (0,0,-2) is the seat
		GTMultiBlockPattern tStamped = GT6MultiblockPreviews.withDisplayBlocks(tPattern,
				Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK);
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2));
		assertEquals(125, GT6MultiblockPreviews.structureBlocks(tStamped, CONTROLLER,
				GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2)).size(), "every cell renders");
		assertEquals(Integer.valueOf(53), tCounts.get(Blocks.OBSIDIAN), "54 vents minus the seat (the :150 tooltip)");
		assertEquals(Integer.valueOf(44), tCounts.get(Blocks.IRON_BLOCK), "the walls (the :150 tooltip)");
		assertEquals(Integer.valueOf(27), tCounts.get(Blocks.GOLD_BLOCK), "the core renders as 27 versatile");
		assertEquals(Integer.valueOf(1), tCounts.get(CONTROLLER));
	}

	@Test
	public void lightningRodShapePinsTheMinimalRepresentative() {
		// the :72-86 walk: the five 3x3 layers (walls/coils alternating, the bottom
		// centre IS the controller — zero-offset) + ONE pillar block as the minimal
		// representative of the unbounded while probe (the r11 non-binding ruling's
		// approximate declaration — javadoc'd on the shape)
		GTMultiBlockPattern tPattern = GT6MultiblockPreviews.lightningRodShape(
				Blocks.IRON_BLOCK, Blocks.GLOWSTONE, Blocks.STONE);
		int tWall = 0, tCoil = 0, tRod = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			assertFalse(tCell.isHollow(), "the rod declares every cell");
			if (tCell.partBlock == Blocks.IRON_BLOCK) tWall++;
			else if (tCell.partBlock == Blocks.GLOWSTONE) tCoil++;
			else {
				tRod++;
				assertEquals(new BlockPos(0, 5, 0), new BlockPos(tCell.x, tCell.y, tCell.z),
						"the 1m pillar at the base centre top (:84)");
			}
		}
		assertEquals(46, tPattern.cells().size(), "27 walls + 18 coils + 1 pillar");
		assertEquals(27, tWall, "the y0/y2/y4 wall layers");
		assertEquals(18, tCoil, "the y1/y3 coil layers");
		assertEquals(1, tRod, "the minimal pillar");
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, BlockPos.ZERO);
		assertEquals(46, tFill.size(), "every cell renders, the seat painted over");
		assertEquals(CONTROLLER.defaultBlockState(), tFill.get(BlockPos.ZERO),
				"the bottom-centre wall cell is the controller seat (upstream :76-84/:89)");
	}

	@Test
	public void candidateFacesPinTheQuotaListsAndTheTooltipCellGate() {
		// acceptance ④ — the r11 Q3 degrade data: the predicate rows carry their full
		// candidate path lists (first = the rendered identity); the mechanism gates the
		// tooltip to the predicate cells only
		assertEquals(List.of("processor_unit_versatile", "processor_unit_logic", "processor_unit_control"),
				GT6MultiblockPreviews.candidatePaths("fusion_reactor"),
				"the fusion core quota (≥3V/≥12L/≥12C — the :56-64 chain)");
		assertEquals(List.of("processor_unit_control", "processor_unit_conversion"),
				GT6MultiblockPreviews.candidatePaths("large_massfab"), "the massfab 4+4 ring (:201-217)");
		assertEquals(List.of("processor_unit_versatile", "processor_unit_logic", "processor_unit_control",
				"processor_unit_storage", "processor_unit_conversion", "machine_wall_galvanized_steel"),
				GT6MultiblockPreviews.candidatePaths("logistics_core"),
				"the free combination + the wall cheapskate last (:119-133)");
		assertTrue(GT6MultiblockPreviews.candidatePaths("multiblock_coke_oven").isEmpty(),
				"rows without predicate cells list nothing");
		// the tooltip gate: a fusion PU cell lists, a forming wall does not, a hollow
		// cell does not, an unknown cell does not
		GTMultiBlockPattern tPattern = fusionFixture();
		GTMultiBlockPattern.Cell tPu = GT6MultiblockPreviews.cellAt(tPattern, BlockPos.ZERO);
		assertNotNull(tPu, "the core centre cell");
		assertEquals(3, GT6MultiblockPreviews.cellCandidatePaths("fusion_reactor", tPu).size(),
				"the predicate cell lists its candidates");
		GTMultiBlockPattern.Cell tWall = GT6MultiblockPreviews.cellAt(tPattern, new BlockPos(0, 0, -2));
		assertTrue(GT6MultiblockPreviews.cellCandidatePaths("fusion_reactor", tWall).isEmpty(),
				"the forming seat cell lists nothing");
		assertTrue(GT6MultiblockPreviews.cellCandidatePaths("fusion_reactor", null).isEmpty(),
				"an unknown cell lists nothing");
		GTMultiBlockPattern tMassfab = GT6MultiblockPreviews.massfabShape(Blocks.IRON_BLOCK, Blocks.GLOWSTONE,
				Blocks.OBSIDIAN, Blocks.GOLD_BLOCK, List.of(Blocks.REDSTONE_BLOCK, Blocks.LAPIS_BLOCK));
		assertTrue(GT6MultiblockPreviews.cellCandidatePaths("large_massfab",
				GT6MultiblockPreviews.cellAt(tMassfab, new BlockPos(0, 2, 0))).isEmpty(),
				"the hollow air centre lists nothing");
	}

	@Test
	public void previewWidgetPinsTheReplicatedShell() throws Exception {
		// the mbpreview-shell-replicate face, pinned at the bytecode layer (the widget
		// links the compile-only ModularUI client stack — never class-loaded offline, the
		// class doc): the ray-tracing renderer + green frame, the left-click selection
		// callback, the vendored LayerButton state machine, the viewer-recognized material
		// slots — and the retired description strip GONE (the 40px give-back to the 3D view)
		try (java.io.InputStream tWidget = GT6MultiblockPreviewWidget.class
				.getResourceAsStream("GT6MultiblockPreviewWidget.class");
			 java.io.InputStream tTable = GT6MultiblockPreviews.class
					 .getResourceAsStream("GT6MultiblockPreviews.class")) {
			assertNotNull(tWidget, "preview widget class resource");
			String tWidgetBytes = new String(tWidget.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertNotNull(tTable, "preview table class resource");
			String tTableBytes = new String(tTable.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertTrue(tWidgetBytes.contains("brachy/modularui/drawable/SchemaRenderer"),
					"the 3D view rides the ray-tracing SchemaRenderer (the GTCEu renderer face)");
			assertTrue(tWidgetBytes.contains("brachy/modularui/drawable/schema/BlockHighlight"),
					"the green selection frame is wired (the GTCEu highlightRenderer face)");
			// the centering seed (task mbpreview-centering-gtceu-replica): the GTCEu
			// reference algorithm's positioning segment carries into the compiled artifact —
			// camera().setPosAndLookAt(0, 0, -10, schema center) — the MultiblockPreviewWidget
			// .java:160-161 face (the seed + the getFocus center source, the bytecode layer)
			assertTrue(tWidgetBytes.contains("setPosAndLookAt") && tWidgetBytes.contains("getFocus"),
					"the GTCEu centering seed (camera.setPosAndLookAt over the schema focus) is wired");
			assertTrue(tWidgetBytes.contains("listenGuiAction") && tWidgetBytes.contains("lastRayTrace"),
					"left-click selection reads the traced hit (the GTCEu setBlockOnClick face)");
			assertTrue(tWidgetBytes.contains("brachy/modularui/widgets/SchemaWidget$LayerButton"),
					"the y-level filter is the vendored LayerButton state machine");
			assertTrue(tWidgetBytes.contains("brachy/modularui/integration/recipeviewer/RecipeViewerSlotWidget"),
					"the material column is viewer-recognized slots (the GTCEu parts face)");
			assertTrue(tWidgetBytes.contains("brachy/modularui/widgets/dynamic/DynamicWidget"),
					"the selected-block display is the GTCEu dynamic rebuild face");
			assertFalse(tWidgetBytes.contains("brachy/modularui/widgets/TextWidget"),
					"the description strip is retired — no TextWidget in the page");
			assertFalse(tTableBytes.contains("description"),
					"the table row carries NO text face (name+item+pattern only)");
		}
	}

	/**
	 * The centering nail (task mbpreview-centering-gtceu-replica): the GTCEu reference
	 * positioning algorithm, driven END TO END over the port's real schema fills with the
	 * VENDORED {@code Camera} (the class that actually runs — pure math, no client stack:
	 * Camera.java imports only {@code Mth} + joml, so it loads offline through the
	 * {@code gt6.modularui.classes} seam, the GT6MultiblockPreviewEmiInputTest mechanism
	 * upgraded from byte-reading to a child-first URLClassLoader).
	 *
	 * <p>The reference chain, verbatim anchors: the seed
	 * {@code camera().setPosAndLookAt(0, 0, -10, getCenter())}
	 * (MultiblockPreviewWidget.java:160-161); the bounds — non-air min/max, center =
	 * {@code BlockPosUtil.getCenterF(min, max)} (MapSchema.java:36-51, BlockPosUtil.java
	 * :59-62); the derived yaw/pitch/dist (Camera.java:45-53); the projection that turns
	 * dist+lookAt into the centered picture (BaseSchemaRenderer.java:490-527); the
	 * runtime draw re-seeding lookAt = focus + offset EVERY frame (SchemaWidget.java
	 * :47-52) — the invariant the seed and the draw SHARE: the camera always anchors the
	 * structure's bounding-box center, which is what puts the structure in the panel's
	 * middle instead of hugging a corner.
	 */
	@Test
	public void previewCenteringPinsTheGtceuCameraSeed() throws Exception {
		String tClasses = System.getProperty("gt6.modularui.classes");
		assertNotNull(tClasses, "gt6.modularui.classes system property (the build scripts feed it)");
		java.net.URLClassLoader tMui = new java.net.URLClassLoader(
				new java.net.URL[] { java.nio.file.Paths.get(tClasses).toUri().toURL() },
				getClass().getClassLoader());
		Class<?> tCamera = Class.forName("brachy.modularui.drawable.schema.Camera", true, tMui);
		Class<?> tVec = Class.forName("org.joml.Vector3f", true, tMui);
		Object tSeed = tCamera.getConstructor().newInstance();
		java.lang.reflect.Method tSetPosAndLookAt = tCamera.getMethod("setPosAndLookAt",
				float.class, float.class, float.class, tVec);
		java.lang.reflect.Method tLookAt = tCamera.getMethod("lookAt");
		java.lang.reflect.Method tDist = tCamera.getMethod("dist");

		// the center the way MapSchema computes it (MapSchema.java:36-51): the non-air
		// fill's bounding-box midpoint (the port's structureBlocks IS the MapSchema input)
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				cokeOvenShape(), CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		float[] tCenter = bboxCenter(tFill);
		assertArrayEquals(new float[] {0f, 0f, 0f}, tCenter, 1e-6f,
				"the anchor-symmetric coke oven centers ON the anchor — the bounds center is the origin");

		// the seed (MultiblockPreviewWidget.java:160-161): look at the center from
		// (0, 0, -10) — the camera derives yaw/pitch and dist = 10 from it
		Object tCenterVec = tVec.getConstructor(float.class, float.class, float.class)
				.newInstance(tCenter[0], tCenter[1], tCenter[2]);
		tSetPosAndLookAt.invoke(tSeed, 0f, 0f, -10f, tCenterVec);
		Object tAnchored = tLookAt.invoke(tSeed);
		for (int i = 0; i < 3; i++) {
			assertEquals(tCenter[i], (float) tVec.getMethod(vecGetter(i)).invoke(tAnchored), 1e-6f,
					"the seed anchors the camera ON the fill's bounds center (axis " + i + ")");
		}
		assertEquals(10f, (float) tDist.invoke(tSeed), 1e-6f, "the seed distance is 10 (the :160 literal)");

		// the runtime draw invariant (SchemaWidget.java:47-52): every frame re-seeds
		// lookAt = getFocus() + offset with the widget's own scale/yaw/pitch — the SAME
		// center, so drag state can never leave the structure off the panel's middle
		java.lang.reflect.Method tSetLookAtAndAngle = tCamera.getMethod("setLookAtAndAngle",
				float.class, float.class, float.class, float.class, float.class, float.class);
		tSetLookAtAndAngle.invoke(tSeed, tCenter[0], tCenter[1], tCenter[2], 10f, 0f,
				(float) (Math.PI / 4));
		Object tRedrawn = tLookAt.invoke(tSeed);
		for (int i = 0; i < 3; i++) {
			assertEquals(tCenter[i], (float) tVec.getMethod(vecGetter(i)).invoke(tRedrawn), 1e-6f,
					"the per-frame draw re-seed keeps the camera ON the center (axis " + i + ")");
		}

		// the authored-shape spot check (the massfab): the upstream walk is x/z symmetric
		// but y-offset (layers y0..y5) — the bounds center follows the FILL, exactly the
		// reference semantics (MapSchema.java:36-51), NOT an assumed (0,0,0)
		GTMultiBlockPattern tMassfab = GT6MultiblockPreviews.massfabShape(Blocks.IRON_BLOCK, Blocks.GLOWSTONE,
				Blocks.OBSIDIAN, Blocks.GOLD_BLOCK, List.of(Blocks.REDSTONE_BLOCK, Blocks.LAPIS_BLOCK));
		float[] tMassCenter = bboxCenter(GT6MultiblockPreviews.structureBlocks(
				tMassfab, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING, new BlockPos(0, 0, -2)));
		assertArrayEquals(new float[] {0f, 2.5f, 0f}, tMassCenter, 1e-6f,
				"the massfab bounds center rides the fill (y0..y5 → 2.5), the reference arithmetic");
	}

	/** The bounding-box midpoint of a schema fill — the MapSchema.java:36-51 center arithmetic. */
	private static float[] bboxCenter(Map<BlockPos, BlockState> aFill) {
		int tMinX = Integer.MAX_VALUE, tMinY = Integer.MAX_VALUE, tMinZ = Integer.MAX_VALUE;
		int tMaxX = Integer.MIN_VALUE, tMaxY = Integer.MIN_VALUE, tMaxZ = Integer.MIN_VALUE;
		for (BlockPos tPos : aFill.keySet()) {
			tMinX = Math.min(tMinX, tPos.getX()); tMaxX = Math.max(tMaxX, tPos.getX());
			tMinY = Math.min(tMinY, tPos.getY()); tMaxY = Math.max(tMaxY, tPos.getY());
			tMinZ = Math.min(tMinZ, tPos.getZ()); tMaxZ = Math.max(tMaxZ, tPos.getZ());
		}
		return new float[] {(tMaxX - tMinX) / 2.0f + tMinX, (tMaxY - tMinY) / 2.0f + tMinY,
				(tMaxZ - tMinZ) / 2.0f + tMinZ};
	}

	/** joml Vector3f's component accessor per axis (x()/y()/z() — the 1.10-line names). */
	private static String vecGetter(int aAxis) {
		return new String[] {"x", "y", "z"}[aAxis];
	}

	@Test
	public void infoPagesRemovedFromBothPlugins() throws Exception {
		// the swap census: neither plugin may still carry the retired COKE-OVEN text-info
		// faces — read at the bytecode layer (the same layer the detection-contract tests
		// read). Narrowed at the seat-IX rebase adjudication (task viewer-energy-jump-gear):
		// the retirement targeted the replaced face, not the API — the energy-carrier
		// info pages legitimately ride addIngredientInfo/EmiInfoRecipe (user ruling: the
		// gear port jumps to per-carrier "how is this produced" pages).
		try (java.io.InputStream tJei = GT6JeiPlugin.class.getResourceAsStream("GT6JeiPlugin.class");
			 java.io.InputStream tEmi = GT6EmiPlugin.class.getResourceAsStream("GT6EmiPlugin.class")) {
			assertNotNull(tJei, "JEI plugin class resource");
			assertNotNull(tEmi, "EMI plugin class resource");
			String tJeiBytes = new String(tJei.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			String tEmiBytes = new String(tEmi.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertFalse(tJeiBytes.contains("cokeOvenInfo"),
					"the JEI coke-oven text-info page must be gone (replaced by the preview category)");
			assertFalse(tEmiBytes.contains("registerCokeOvenInfo"),
					"the EMI coke-oven text-info page must be gone (replaced by the preview category)");
			assertTrue(tJeiBytes.contains("GT6MultiblockPreviewJeiCategory"),
					"the JEI leg must register the preview category");
			assertTrue(tEmiBytes.contains("GT6MultiblockPreviewEmiCategory"),
					"the EMI leg must register the preview category");
		}
	}

	@Test
	public void categoryClassesPinTheirBridgesAndUid() throws Exception {
		// the two category classes are never loaded offline (compile-only stacks) — pin
		// them at the bytecode layer: the uid literal and the per-leg viewer bridge
		try (java.io.InputStream tJei = GT6MultiblockPreviewJeiCategory.class.getResourceAsStream("GT6MultiblockPreviewJeiCategory.class");
			 java.io.InputStream tEmi = GT6MultiblockPreviewEmiCategory.class.getResourceAsStream("GT6MultiblockPreviewEmiCategory.class")) {
			assertNotNull(tJei, "JEI preview category class resource");
			assertNotNull(tEmi, "EMI preview category class resource");
			String tJeiBytes = new String(tJei.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			String tEmiBytes = new String(tEmi.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertTrue(tJeiBytes.contains("multiblock_preview"), "JEI category uid literal");
			assertTrue(tEmiBytes.contains("multiblock_preview"), "EMI category uid literal");
			assertTrue(tJeiBytes.contains("GT6MultiblockPreviewWidget"),
					"the JEI category embeds the shared preview widget");
			// the EMI widget supplier lives on the NESTED wrapper (its own class file):
			// the outer category class only pins the EMI bridge + uid
			try (java.io.InputStream tEmiWrapper = GT6MultiblockPreviewEmiCategory.class
					.getResourceAsStream("GT6MultiblockPreviewEmiCategory$PreviewEmiRecipe.class")) {
				assertNotNull(tEmiWrapper, "EMI preview wrapper class resource");
				String tWrapperBytes = new String(tEmiWrapper.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
				assertTrue(tWrapperBytes.contains("GT6MultiblockPreviewWidget"),
						"the EMI wrapper embeds the shared preview widget");
				assertTrue(tWrapperBytes.contains("brachy/modularui/integration/emi/recipe/ModularUIEmiRecipe"),
						"the EMI wrapper extends the vendored ModularUI EMI bridge (both legs)");
			}
			//? if forge {
			assertTrue(tJeiBytes.contains("ModularUIJeiCategory"),
					"the forge leg extends the vendored ModularUI JEI bridge (JEI 15.x face)");
			//?} else {
			/*assertTrue(tJeiBytes.contains("ModularUIRecipeCategory"),
					"the neoforge leg extends the vendored ModularUI JEI bridge (JEI 19.x face)");
			*///?}
		}
	}
}
