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
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
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
	public void tableCensusBatchAThermalAppendsThirteenRows() {
		// the batch-A face (task mbpreview-data-a-thermal): the coke oven + the THERMAL
		// pair appended in upstream registration order — the 5 boiler tiers
		// (Loader_MultiTileEntities.java:1248-1252) then the 8 crucible tiers
		// (:1270-1277). The pin FAILS until the batch lands (the red→green drill);
		// every later data batch re-pins the tail (the tail-append doctrine).
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
				"crucible_adamantium"),
				tNames, "the row census: coke oven + the batch-A thermal 13, upstream order");
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
