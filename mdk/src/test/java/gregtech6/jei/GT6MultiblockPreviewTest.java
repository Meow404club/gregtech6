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
 * GTMultiBlockPattern.cellOffset table.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.emi.GT6EmiPlugin;
import gregtech6.emi.GT6MultiblockPreviewEmiCategory;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GTRecipesOfflineTestBase;

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
	public void tableCensusFirstVersionIsCokeOvenOnly() {
		// the card face: first version = the Coke Oven row; data cards append rows and this
		// pin deliberately FAILS — the census is meant to be re-pinned per machine
		assertEquals(1, GT6MultiblockPreviews.entries().size(), "first version: exactly one preview row");
		GT6MultiblockPreviews.Entry tEntry = GT6MultiblockPreviews.entries().get(0);
		assertEquals("multiblock_coke_oven", tEntry.name(), "the row is the coke oven (registry-name census key)");
		assertEquals("gt6.jei.multiblock_preview", GT6MultiblockPreviews.TITLE_KEY, "category title key pinned");
		assertEquals("multiblock_preview", GT6MultiblockPreviews.UID_PATH, "category uid path pinned");
		assertEquals(2, GT6MultiblockPreviews.DISPLAY_FACING, "display facing = north (Direction.get3DDataValue)");
		assertEquals(200, GT6MultiblockPreviews.PAGE_WIDTH);
		assertEquals(180, GT6MultiblockPreviews.PAGE_HEIGHT);
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
