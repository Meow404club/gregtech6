/**
 * Offline pin for task r7-mold-geometry (GitHub #40/#41 card 2) as amended by
 * r9-41-mold-invert-fix (issue #41) — the mold BLOCKS carry the upstream CONCAVE shape
 * geometry: a chisel strike SETS a bit (MultiTileEntityMold.java:328-335) and the render
 * gate :537 skips the lit cells, so bit=1 = carved out. The committed model JSON is the
 * 1px full-footprint floor + the four 2px walls + one 2.4x3x2.4px element per UNLIT bit
 * (the lit bit = a 2px-deep recess over the floor = the negative/cavity form), the
 * selection/collision shapes riding the upstream boxes (MultiTileEntityMold.java
 * :559-560), and the formed BlockItem parenting the block model (the 3D inventory shape
 * for free).
 *
 * <p>Three pins (the card face): (a) the 30 ceramic masks vs the Loader
 * _MultiTileEntities.java:391-420 smelting literals transcribed HERE independently —
 * the transcription is the card's lifeline, so the reference table must not import the
 * registry's own copy; (b) the committed model JSON element census = 5 + (25 − popcount)
 * — plate (all 25 carved) = the bare 5-element dish, blank = 30, every cell element y
 * 0..3 (nails the retired convex minY=1/maxY=4 forms); (c) the upstream selection box
 * (full 16x16x3px footprint) and collision box (12x12x2px inner cavity).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tools.TileEntityMold;

public class GT6MoldGeometryDatagenTest extends GTOfflineTestBase {

	/**
	 * The upstream reference table (Loader_MultiTileEntities.java:391-420, verbatim
	 * {@code gt.mold} literals, row order) — transcribed HERE, not imported from
	 * {@link GT6Molds#CERAMIC_ROWS}, so a transcription slip in either copy fails loud.
	 */
	private static final List<Map.Entry<String, Integer>> UPSTREAM = List.of(
			Map.entry("mold_ceramic_ingot",          0b0_01110_01110_01110_01110_01110),
			Map.entry("mold_ceramic_billet",         0b0_01100_11110_11110_01100_00000),
			Map.entry("mold_ceramic_chunk",          0b0_11000_11000_00000_00000_00000),
			Map.entry("mold_ceramic_plate",          0b0_11111_11111_11111_11111_11111),
			Map.entry("mold_ceramic_tiny_plate",     0b0_00000_01110_01110_01110_00000),
			Map.entry("mold_ceramic_bolt",           0b0_00000_00000_00100_00100_00000),
			Map.entry("mold_ceramic_rod",            0b0_00000_00000_11111_00000_00000),
			Map.entry("mold_ceramic_long_rod",       0b0_10000_01000_00100_00010_00001),
			Map.entry("mold_ceramic_item_casing",    0b0_11101_11101_11101_00001_11100),
			Map.entry("mold_ceramic_ring",           0b0_00000_01110_01010_01110_00000),
			Map.entry("mold_ceramic_gear",           0b0_10101_01110_11011_01110_10101),
			Map.entry("mold_ceramic_small_gear",     0b0_01010_11111_01010_11111_01010),
			Map.entry("mold_ceramic_sword",          0b0_00100_01110_01110_01110_01110),
			Map.entry("mold_ceramic_pickaxe",        0b0_00000_01110_10001_00000_00000),
			Map.entry("mold_ceramic_spade",          0b0_01110_01110_01110_01010_00000),
			Map.entry("mold_ceramic_shovel",         0b0_00100_01110_01110_01110_00000),
			Map.entry("mold_ceramic_universal_spade",0b0_00100_01110_01100_01110_00000),
			Map.entry("mold_ceramic_axe",            0b0_00000_01110_01110_01000_00000),
			Map.entry("mold_ceramic_double_axe",     0b0_00000_11111_11111_10001_00000),
			Map.entry("mold_ceramic_saw",            0b0_00000_11111_11111_00000_00000),
			Map.entry("mold_ceramic_hammer",         0b0_01110_01110_01010_01110_01110),
			Map.entry("mold_ceramic_file",           0b0_01110_01110_01110_00100_00100),
			Map.entry("mold_ceramic_screwdriver",    0b0_00000_00100_00100_00100_00100),
			Map.entry("mold_ceramic_chisel",         0b0_01110_00100_00100_00100_00100),
			Map.entry("mold_ceramic_arrow",          0b0_00000_00100_00100_01110_00000),
			Map.entry("mold_ceramic_hoe",            0b0_00000_00110_01110_00000_00000),
			Map.entry("mold_ceramic_sense",          0b0_00000_01111_11111_00000_00000),
			Map.entry("mold_ceramic_plow",           0b0_11111_11111_11111_11111_00100),
			Map.entry("mold_ceramic_builderwand",    0b0_00000_00100_11111_01110_01010),
			Map.entry("mold_ceramic_nugget",         0b0_00000_00000_00100_00000_00000));

	/** The row walk of the datagen band: the blank first, then the 30 shapes (GT6MoldDatagen order). */
	private static List<GT6Molds.MoldRow> rows() {
		return java.util.stream.Stream.concat(java.util.stream.Stream.of(GT6Molds.CERAMIC_BLANK_ROW),
				GT6Molds.CERAMIC_ROWS.stream()).toList();
	}

	/** The independent grid math (the MOLD_BOUNDS window): grid line c in px. */
	private static double gridLine(int c) {
		return 2.0 + 12.0 * c / 5.0;
	}

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6MoldGeometryDatagenTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertTrue(tStream != null, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	/** (a) The registry masks vs the upstream literals — per-row, per-bit. */
	@Test
	void moldMasksMatchTheUpstreamLiterals() {
		assertEquals(30, GT6Molds.CERAMIC_ROWS.size(), "the 30 pre-carved ceramic shapes");
		assertEquals(0, GT6Molds.CERAMIC_BLANK_ROW.preCarvedShape(), "the blank ships shape 0 (Loader:352)");
		// the stone rung's pre-carve = the ingot bar at shift 0 (the :688-694 loop form:
		// B[0]|B[1]|B[2] per group of 5 = 00111 in every 5-bit group)
		assertEquals(0b0_00111_00111_00111_00111_00111, TileEntityMold.ingotShape(0),
				"ingotShape(0) is the B[i+r*5] loop literal");
		assertEquals(TileEntityMold.ingotShape(0), GT6Molds.ROWS.get(0).preCarvedShape(),
				"the stone mold ships the ingot bar");
		for (int i = 0; i < UPSTREAM.size(); i++) {
			GT6Molds.MoldRow tRow = GT6Molds.CERAMIC_ROWS.get(i);
			assertEquals(UPSTREAM.get(i).getKey(), tRow.path(), "row " + i + " order (the Loader walk)");
			assertEquals(UPSTREAM.get(i).getValue().intValue(), tRow.preCarvedShape(),
					tRow.path() + ": the gt.mold literal, bit for bit (Loader:"
							+ (391 + i) + ")");
			assertTrue(tRow.preCarvedShape() >= 0 && tRow.preCarvedShape() <= TileEntityMold.SHAPE_MASK,
					tRow.path() + ": 25-bit mask");
		}
	}

	/**
	 * (b) The committed model JSON: the 5+(25−popcount) elements — floor + 4 walls + one
	 * element per UNLIT (carved-out) bit, on the 2.4px grid, every cell y 0..3.
	 */
	@Test
	void moldModelsFollowTheBitmap() throws Exception {
		assertFalse(rows().isEmpty(), "the row walk broke — never pass vacuously");
		for (GT6Molds.MoldRow tRow : rows()) {
			int tMask = tRow.preCarvedShape();
			JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + ".json");
			assertEquals("minecraft:block/block", tModel.get("parent").getAsString(),
					tRow.path() + ": the display-transform parent");
			assertTrue(tModel.getAsJsonObject("textures").has("body"),
					tRow.path() + ": the body texture key");
			JsonArray tElements = tModel.getAsJsonArray("elements");
			int tCarved = 25 - Integer.bitCount(tMask);
			assertEquals(tCarved + 5, tElements.size(),
					tRow.path() + ": floor + 4 walls + one element per carved (unlit) bit");
			// element 0 = the full-footprint 1px floor (MOLD_BOUNDS[1])
			JsonArray tFrom = tElements.get(0).getAsJsonObject().getAsJsonArray("from");
			JsonArray tTo = tElements.get(0).getAsJsonObject().getAsJsonArray("to");
			assertEquals(0.0, tFrom.get(0).getAsDouble(), 1e-9, tRow.path() + " floor x0");
			assertEquals(0.0, tFrom.get(1).getAsDouble(), 1e-9, tRow.path() + " floor y0");
			assertEquals(0.0, tFrom.get(2).getAsDouble(), 1e-9, tRow.path() + " floor z0");
			assertEquals(16.0, tTo.get(0).getAsDouble(), 1e-9, tRow.path() + " floor x1");
			assertEquals(1.0, tTo.get(1).getAsDouble(), 1e-9, tRow.path() + " floor y1 (PX_N[15])");
			assertEquals(16.0, tTo.get(2).getAsDouble(), 1e-9, tRow.path() + " floor z1");
			// elements 1..4 = the four 2px-thick 4px-tall walls (MOLD_BOUNDS[2..5]),
			// E/S/W/N emission order
			double[][] tWalls = {
					{14, 0, 0, 16, 4, 16}, // east, outward cullface
					{0, 0, 14, 16, 4, 16}, // south
					{0, 0, 0, 2, 4, 16},   // west
					{0, 0, 0, 16, 4, 2}};  // north
			for (int w = 0; w < 4; w++) {
				double[] tA = coords(tElements.get(1 + w).getAsJsonObject());
				for (int k = 0; k < 6; k++) {
					assertEquals(tWalls[w][k], tA[k], 1e-9, tRow.path() + " wall " + w + " coord " + k);
				}
			}
			// elements 5..n = the UNLIT cells standing as the 3px surface (the lit bit =
			// carved out), in bit order i: cell (xcol=i/5, zrow=i%5), the MOLD_BOUNDS[18+i]
			// walk — independent double math, 1e-3 slack for the 2.4px float print
			int tElement = 5;
			for (int i = 0; i < 25; i++) {
				if ((tMask & (1 << i)) != 0) continue;
				double[] tE = {gridLine(i / 5), 0.0, gridLine(i % 5), gridLine(i / 5 + 1), 3.0, gridLine(i % 5 + 1)};
				double[] tA = coords(tElements.get(tElement).getAsJsonObject());
				for (int k = 0; k < 6; k++) {
					assertEquals(tE[k], tA[k], 1e-3, tRow.path() + " cell bit " + i + " coord " + k);
				}
				tElement++;
			}
		}
		// the extremes, nailed by name: the all-carved plate = the bare 5-element dish,
		// the untouched blank = 30 (25 cells + 5)
		assertEquals(5, generatedJson("assets/gt6/models/block/mold_ceramic_plate.json")
				.getAsJsonArray("elements").size(), "the plate molds as the plain dish (25 carved)");
		assertEquals(30, generatedJson("assets/gt6/models/block/mold_ceramic.json")
				.getAsJsonArray("elements").size(), "the blank keeps all 25 surface cells");
	}

	/** The from/to sextet of one model element. */
	private static double[] coords(JsonObject aElement) {
		JsonArray tFrom = aElement.getAsJsonArray("from");
		JsonArray tTo = aElement.getAsJsonArray("to");
		return new double[] {tFrom.get(0).getAsDouble(), tFrom.get(1).getAsDouble(), tFrom.get(2).getAsDouble(),
				tTo.get(0).getAsDouble(), tTo.get(1).getAsDouble(), tTo.get(2).getAsDouble()};
	}

	/** (b2) The blockstate face: one variant per row pointing at the bitmap model. */
	@Test
	void moldBlockstatesPointAtTheBitmapModels() throws Exception {
		JsonObject tState = generatedJson("assets/gt6/blockstates/mold_ceramic_ingot.json");
		JsonObject tVariant = tState.getAsJsonObject("variants").getAsJsonObject("");
		assertEquals("gt6:block/mold_ceramic_ingot", tVariant.get("model").getAsString(),
				"the ingot blockstate variant rides the bitmap model");
	}

	/**
	 * (c) The upstream selection/collision pair (MultiTileEntityMold.java:559-560),
	 * shape-independent: selection = the full 16x16x3px footprint, collision = the
	 * 12x12x2px inner cavity. The retired r7 mask-shaped selection died with issue #41 —
	 * after the polarity flip the cavities are the holes, and a mask-shaped selection left
	 * the mold un-clickable in its own pit.
	 */
	@Test
	void moldSelectionAndCollisionAreTheUpstreamBoxes() {
		// offline Block construction needs the block registry temporarily unfrozen (the
		// GTWireContactDamageTest / GT6SurfaceBlocksTest form)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		// the two mask extremes (all-carved plate, untouched blank) — every row rides the
		// same two static boxes
		for (String tPath : new String[] {"mold_ceramic", "mold_ceramic_plate"}) {
			GT6Molds.MoldBlock tBlock = new GT6Molds.MoldBlock(
					new GT6Molds.MoldRow(tPath, () -> null, 1.0F, 0),
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
			List<net.minecraft.world.phys.AABB> tSel = tBlock
					.getShape(tBlock.defaultBlockState(), null, null, null).toAabbs();
			assertEquals(1, tSel.size(), tPath + ": selection is one full-footprint box");
			assertBox(tSel.get(0), 0, 0, 0, 16, 3, 16, tPath + " selection (:560)");
			List<net.minecraft.world.phys.AABB> tCol = tBlock
					.getCollisionShape(tBlock.defaultBlockState(), null, null, null).toAabbs();
			assertEquals(1, tCol.size(), tPath + ": collision is one inner-cavity box");
			assertBox(tCol.get(0), 2, 0, 2, 14, 2, 14, tPath + " collision (:559)");
			assertNotSame(tBlock.getShape(tBlock.defaultBlockState(), null, null, null),
					tBlock.getCollisionShape(tBlock.defaultBlockState(), null, null, null),
					tPath + ": the two upstream boxes are distinct shapes");
		}
	}

	/** The 0..1-normalized AABB pin (VoxelShape.toAabbs space, the r3-stick precedent). */
	private static void assertBox(net.minecraft.world.phys.AABB aBox, double aX0, double aY0, double aZ0,
			double aX1, double aY1, double aZ1, String aLabel) {
		assertEquals(aX0 / 16.0, aBox.minX, 1e-9, aLabel + " minX");
		assertEquals(aY0 / 16.0, aBox.minY, 1e-9, aLabel + " minY");
		assertEquals(aZ0 / 16.0, aBox.minZ, 1e-9, aLabel + " minZ");
		assertEquals(aX1 / 16.0, aBox.maxX, 1e-9, aLabel + " maxX");
		assertEquals(aY1 / 16.0, aBox.maxY, 1e-9, aLabel + " maxY");
		assertEquals(aZ1 / 16.0, aBox.maxZ, 1e-9, aLabel + " maxZ");
	}
}
