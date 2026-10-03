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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6Turbines;
import gregtech6.tileentity.multiblocks.GT6HeatExchangerBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlockConverter;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

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
		// Compressor (:1228) and the Large Heat Exchanger (:1245). The pin FAILS until
		// the batch lands (the red→green drill).
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
				"large_heat_exchanger"),
				tNames, "the row census: coke oven + thermal 13 + energy 14, upstream order");
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
	public void cruciblePreviewPinsTheDesign0WallForm() {
		// the crucible binding IS the forming declaration (acceptance ②): 24 formingPart
		// walls in three rings + the fail-not-clear hollow pair — zero re-stamp needed.
		// design 0 on this base; the in-flight mb-formed-crucible-wall re-pins it to 4.
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
			assertEquals(0, tCell.design, "design 0 on this base");
		}
		assertEquals(24, tWalls, "three 8-cell rings (26 cells total with the hollow pair)");
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(24, tFill.size(), "every wall renders");
		assertFalse(tFill.containsKey(BlockPos.ZERO),
				"the bottom centre — the real controller seat — is never declared (the upstream pattern)");
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
		// centre (the +1-Y bake, (0,1,0)); the controller paints the anchor cell (0,0,-1)
		// over a shell wall — the crucible legacy ① seam ruling, generalization is D2's
		GTMultiBlockPattern tPattern = new PreviewImplosion(BlockPos.ZERO, Blocks.BRICKS.defaultBlockState())
				.getStructurePattern();
		Map<BlockPos, BlockState> tFill = GT6MultiblockPreviews.structureBlocks(
				tPattern, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
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
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(17, tFill.size(), "8 walls + 8 transmitters + the centre wall");
		Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
				tStamped, CONTROLLER, GT6MultiblockPreviews.DISPLAY_FACING);
		assertEquals(Integer.valueOf(8), tCounts.get(Blocks.BRICKS), "9 walls minus the anchor-painted controller");
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
