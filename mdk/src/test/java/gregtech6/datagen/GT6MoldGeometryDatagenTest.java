/**
 * Offline pin for task r7-mold-geometry (GitHub #40/#41 card 2) — the mold BLOCKS carry
 * the upstream shape geometry: the 5x5 {@code gt.mold} bitmap rendered as the 1px
 * full-footprint floor + one 2.4x3x2.4px cell per lit bit (the MOLD_BOUNDS render-pass
 * 18-42 geometry, MultiTileEntityMold.java:459-509), the selection/collision shapes
 * riding the same mask ({@link GT6Molds#shapeOf}), and the formed BlockItem parenting
 * the block model (the 3D inventory shape for free).
 *
 * <p>Three pins (the card face): (a) the 30 ceramic masks vs the Loader
 * _MultiTileEntities.java:391-420 smelting literals transcribed HERE independently —
 * the transcription is the card's lifeline, so the reference table must not import the
 * registry's own copy; (b) the committed model JSON element census = popcount + floor
 * with the per-bit coordinates on the 2..14px / 2.4px grid; (c) the selection shape
 * covers exactly the lit cells (empty grid cells unselectable), collision = selection.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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

	/** (b) The committed model JSON: popcount elements + the floor, on the 2.4px grid. */
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
			int tBits = Integer.bitCount(tMask);
			assertEquals(tBits + 1, tElements.size(),
					tRow.path() + ": floor + one element per lit bit");
			// element 0 = the full-footprint 1px floor (MOLD_BOUNDS[1])
			JsonArray tFrom = tElements.get(0).getAsJsonObject().getAsJsonArray("from");
			JsonArray tTo = tElements.get(0).getAsJsonObject().getAsJsonArray("to");
			assertEquals(0.0, tFrom.get(0).getAsDouble(), 1e-9, tRow.path() + " floor x0");
			assertEquals(0.0, tFrom.get(1).getAsDouble(), 1e-9, tRow.path() + " floor y0");
			assertEquals(0.0, tFrom.get(2).getAsDouble(), 1e-9, tRow.path() + " floor z0");
			assertEquals(16.0, tTo.get(0).getAsDouble(), 1e-9, tRow.path() + " floor x1");
			assertEquals(1.0, tTo.get(1).getAsDouble(), 1e-9, tRow.path() + " floor y1 (PX_N[15])");
			assertEquals(16.0, tTo.get(2).getAsDouble(), 1e-9, tRow.path() + " floor z1");
			// elements 1..n = the lit cells in bit order i: cell (xcol=i/5, zrow=i%5),
			// the MOLD_BOUNDS[18+i] walk — independent double math, 1e-3 slack for the
			// 2.4px float print
			int tElement = 1;
			for (int i = 0; i < 25; i++) {
				if ((tMask & (1 << i)) == 0) continue;
				JsonObject tCell = tElements.get(tElement).getAsJsonObject();
				double[] tE = {gridLine(i / 5), 0.0, gridLine(i % 5), gridLine(i / 5 + 1), 3.0, gridLine(i % 5 + 1)};
				JsonArray tCFrom = tCell.getAsJsonArray("from");
				JsonArray tCTo = tCell.getAsJsonArray("to");
				double[] tA = {tCFrom.get(0).getAsDouble(), tCFrom.get(1).getAsDouble(), tCFrom.get(2).getAsDouble(),
						tCTo.get(0).getAsDouble(), tCTo.get(1).getAsDouble(), tCTo.get(2).getAsDouble()};
				for (int k = 0; k < 6; k++) {
					assertEquals(tE[k], tA[k], 1e-3, tRow.path() + " cell bit " + i + " coord " + k);
				}
				tElement++;
			}
		}
	}

	/** (b2) The blockstate face: one variant per row pointing at the bitmap model. */
	@Test
	void moldBlockstatesPointAtTheBitmapModels() throws Exception {
		JsonObject tState = generatedJson("assets/gt6/blockstates/mold_ceramic_ingot.json");
		JsonObject tVariant = tState.getAsJsonObject("variants").getAsJsonObject("");
		assertEquals("gt6:block/mold_ceramic_ingot", tVariant.get("model").getAsString(),
				"the ingot blockstate variant rides the bitmap model");
	}

	/** The coverage probe: does any AABB of the shape contain the px point? */
	private static boolean covers(List<net.minecraft.world.phys.AABB> aBoxes, double aX, double aY, double aZ) {
		net.minecraft.world.phys.Vec3 tPoint = new net.minecraft.world.phys.Vec3(aX / 16.0, aY / 16.0, aZ / 16.0);
		return aBoxes.stream().anyMatch(tBox -> tBox.intersects(new net.minecraft.world.phys.AABB(tPoint, tPoint)));
	}

	/** (c) The selection shape covers exactly the lit cells; collision = selection. */
	@Test
	void moldSelectionAndCollisionFollowTheBitmap() {
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
		java.util.function.ToIntFunction<String> tMaskOf = aPath -> {
			for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
				if (tRow.path().equals(aPath)) return tRow.preCarvedShape();
			}
			return GT6Molds.CERAMIC_BLANK_ROW.preCarvedShape();
		};
		for (String tPath : new String[] {"mold_ceramic", "mold_ceramic_ingot", "mold_ceramic_nugget",
				"mold_ceramic_gear", "mold_ceramic_long_rod"}) {
			GT6Molds.MoldBlock tBlock = new GT6Molds.MoldBlock(
					new GT6Molds.MoldRow(tPath, () -> null, 1.0F, tMaskOf.applyAsInt(tPath)),
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
			int tMask = tMaskOf.applyAsInt(tPath);
			List<net.minecraft.world.phys.AABB> tBoxes = tBlock
					.getShape(tBlock.defaultBlockState(), null, null, null).toAabbs();
			// every lit cell's centre (x/z +1.2px inside, y 1.5px) is selectable
			for (int i = 0; i < 25; i++) {
				double tCx = gridLine(i / 5) + 1.2, tCz = gridLine(i % 5) + 1.2;
				boolean tLit = (tMask & (1 << i)) != 0;
				assertEquals(tLit, covers(tBoxes, tCx, 1.5, tCz),
						tPath + " cell bit " + i + " (" + (i / 5) + "," + (i % 5) + ") selection = mask");
				// collision = selection, cell for cell
				List<net.minecraft.world.phys.AABB> tCollide = tBlock
						.getCollisionShape(tBlock.defaultBlockState(), null, null, null).toAabbs();
				assertEquals(tLit, covers(tCollide, tCx, 1.5, tCz),
						tPath + " cell bit " + i + " collision = mask");
			}
			// the floor band under an empty cell stays selectable (the 1px plate)
			assertTrue(covers(tBoxes, 8, 0.5, 8), tPath + " floor centre selectable");
			assertSame(tBlock.getShape(tBlock.defaultBlockState(), null, null, null),
					tBlock.getCollisionShape(tBlock.defaultBlockState(), null, null, null),
					tPath + ": collision IS the selection shape (the card SPEC)");
		}
		// the blank: exactly the floor — no cell anywhere
		GT6Molds.MoldBlock tBlank = new GT6Molds.MoldBlock(
				new GT6Molds.MoldRow("mold_ceramic", () -> null, 1.0F, 0),
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
		List<net.minecraft.world.phys.AABB> tBlankBoxes = tBlank
				.getShape(tBlank.defaultBlockState(), null, null, null).toAabbs();
		assertEquals(1, tBlankBoxes.size(), "the blank selects as the bare floor");
		net.minecraft.world.phys.AABB tFloor = tBlankBoxes.get(0);
		assertEquals(0.0, tFloor.minX, 1e-9, "blank floor x0");
		assertEquals(1.0 / 16.0, tFloor.maxY, 1e-9, "blank floor 1px tall");
		assertEquals(1.0, tFloor.maxX, 1e-9, "blank full footprint");
	}
}
