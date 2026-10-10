/*
 * Offline pinned tests for task anvil-shape-models: the anvil family's six-box
 * upstream silhouette (the distill-tower-face-3d regression posture — the placeholder
 * must stay dead). The pre-card models were a SINGLE full-cube element (the
 * tex-placeholder-audit era stand-in), which is the "砧还是没有模型" symptom; the card
 * transcribes the upstream setBlockBounds2 pass table — MultiTileEntityAnvil.java
 * :326-349, passes 0-5 — into the 0..16 model px space (the CS.java:492-513 PX_P/PX_N
 * block units ×16). Pinned:
 * <ul>
 * <li>every row model carries EXACTLY the six boxes (pass order + per-box coordinates
 *     verbatim, the wire-card 对照表 form) — the retired single full-cube element form
 *     fails every leg of this pin;</li>
 * <li>no cullface anywhere (the guard walls sit flush with the block boundary —
 *     culled faces would vanish against opaque neighbours; the grindstone sub-cube
 *     form) and the tint seats keep the material-mc-e split (the two finished rows
 *     untinted over the vanilla smooth faces, the 33 ladder rows tintindex 0 over the
 *     borrowed grayscale);</li>
 * <li>the blockstate rotationY table is the anvil-shape-models考据, NOT the furnace
 *     front convention: the long axis runs PERPENDICULAR to the facing (the
 *     :397-399 collision box confirms x[4,12] over the full z band for X-facings) and
 *     the guard pair is asymmetric, so the four facings are four rotations of the one
 *     canonical SIDE_X_NEG (WEST) form — NORTH→270, SOUTH→90, EAST→180, WEST→0. The
 *     furnace anchor (vanilla blockstate facing=east → y=90 with the front on the
 *     model's NORTH face, orientable_with_bottom "north": "#front") fixes
 *     Rot(y=90): (x,z)→(16−z,x); mapping the canonical WEST form onto the four
 *     upstream facing geometries (the tall/low guard swap distinguishes the
 *     ±axis arms) yields exactly this table;</li>
 * <li>the item models parent the row block models (the 3D silhouette rides the
 *     parent chain — the acceptance ② leg).</li>
 * </ul>
 * Declared omissions (not pinned): the anvil-borne item boxes (passes 6-7, the
 * mShapeA/B :350-376 nine-form table) are per-NBT dynamic — the static model is the
 * EMPTY anvil; the pass-5 ALONG_AXIS faces answer BI.nei() upstream (:388) — that
 * glyph art is the GT6AnvilNeiModel wrap's domain, the skirt rides the body texture.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GT6AnvilShapePinTest {

	/** facing → rotationY — the crosswise-long-axis table (NOT the furnace front table). */
	private static final Map<String, Integer> ROT_Y = Map.of(
			"north", 270, "south", 90, "east", 180, "west", 0);

	/** The vanilla face key set every silhouette box renders. */
	private static final Set<String> FACES = Set.of("down", "up", "north", "south", "west", "east");

	/**
	 * The six-box silhouette — the upstream setBlockBounds2 pass table
	 * (MultiTileEntityAnvil.java:326-349) in 0..16 model px, canonical SIDE_X_NEG arm
	 * ({x1,y1,z1,x2,y2,z2}; the PX_P/PX_N block units ×16).
	 */
	private static final float[][] BOXES = {
			{4, 0, 2, 12, 4, 14},            // pass 0 :326 the base
			{6, 4, 4, 10, 8, 12},            // pass 1 :327 the waist
			{4, 8, 1, 12, 12, 15},           // pass 2 :328 the plate
			{5, 8, 15, 11, 12, 16},          // pass 3 :331 the tall guard (the +Z end)
			{4, 9, 0, 12, 11, 1},            // pass 4 :338 the low guard (the −Z end)
			{3.9984F, 0, 2, 12.0016F, 2, 4}, // pass 5 :345 the skirt (the 0.0001F eps ×16)
	};

	@BeforeAll
	static void bootOffline() {
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline
		}
	}

	private static InputStream resource(String aPath) {
		return GT6AnvilShapePinTest.class.getClassLoader().getResourceAsStream(aPath);
	}

	private static JsonObject json(String aPath) throws IOException {
		try (InputStream tStream = resource(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ------------------------------------------------------------------
	// the six-box silhouette
	// ------------------------------------------------------------------

	@Test
	public void everyAnvilRowShipsTheSixBoxUpstreamSilhouette() throws IOException {
		assertEquals(35, gregtech6.registry.GT6Anvils.ROWS.size(), "the anvil census stays 35");
		for (var tRow : gregtech6.registry.GT6Anvils.ROWS) {
			String tPath = tRow.path();
			boolean tLegacy = "stone_anvil".equals(tPath) || "blackstone_anvil".equals(tPath);
			JsonObject tModel = json("assets/gt6/models/block/" + tPath + ".json");
			assertEquals("minecraft:block/block", tModel.get("parent").getAsString(), tPath + ": the block/block parent");
			var tElements = tModel.getAsJsonArray("elements");
			assertEquals(6, tElements.size(), tPath + ": the six-box silhouette — the retired single full-cube placeholder must stay dead");
			for (int i = 0; i < 6; i++) {
				JsonObject tEl = tElements.get(i).getAsJsonObject();
				var tFrom = tEl.getAsJsonArray("from");
				var tTo = tEl.getAsJsonArray("to");
				for (int a = 0; a < 3; a++) {
					assertEquals(BOXES[i][a], tFrom.get(a).getAsFloat(),
							tPath + " pass " + i + " from[" + a + "] = the upstream :"
									+ (326 + i) + " transcription");
					assertEquals(BOXES[i][a + 3], tTo.get(a).getAsFloat(),
							tPath + " pass " + i + " to[" + a + "] = the upstream :"
									+ (326 + i) + " transcription");
				}
				// the retired placeholder shape stays dead: no full-cube element, no cullface
				boolean tFullCube = tFrom.get(0).getAsFloat() == 0 && tFrom.get(1).getAsFloat() == 0
						&& tFrom.get(2).getAsFloat() == 0 && tTo.get(0).getAsFloat() == 16
						&& tTo.get(1).getAsFloat() == 16 && tTo.get(2).getAsFloat() == 16;
				assertFalse(tFullCube, tPath + " pass " + i + ": the full-cube placeholder is retired");
				JsonObject tFaces = tEl.getAsJsonObject("faces");
				assertEquals(FACES, tFaces.keySet(), tPath + " pass " + i + ": all six face directions");
				for (String tFace : FACES) {
					JsonObject tFaceObj = tFaces.getAsJsonObject(tFace);
					assertEquals("#all", tFaceObj.get("texture").getAsString(),
							tPath + " pass " + i + " face " + tFace + ": the single smooth-set art");
					assertNull(tFaceObj.get("cullface"),
							tPath + " pass " + i + " face " + tFace + ": the boundary-flush guards must not cull");
					JsonElement tTint = tFaceObj.get("tintindex");
					if (tLegacy) {
						assertNull(tTint, tPath + " pass " + i + " face " + tFace + ": the finished row is untinted");
					} else {
						assertNotNull(tTint, tPath + " pass " + i + " face " + tFace + ": the ladder row carries the seat");
						assertEquals(0, tTint.getAsInt(), tPath + " pass " + i + " face " + tFace + ": seat 0");
					}
				}
			}
			String tAll = tModel.getAsJsonObject("textures").get("all").getAsString();
			if ("stone_anvil".equals(tPath)) {
				assertEquals("minecraft:block/smooth_stone", tAll, "stone anvil keeps the SET_STONE face");
			} else if ("blackstone_anvil".equals(tPath)) {
				assertEquals("minecraft:block/blackstone", tAll, "blackstone anvil keeps the vanilla face");
			} else {
				assertTrue(tAll.startsWith("gt6:"), tPath + ": the ladder row rides the borrowed grayscale");
			}
		}
	}

	// ------------------------------------------------------------------
	// the blockstate rotation table
	// ------------------------------------------------------------------

	@Test
	public void blockstatesRotateTheCrosswiseLongAxisPerFacing() throws IOException {
		for (var tRow : gregtech6.registry.GT6Anvils.ROWS) {
			JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
			assertEquals(4, tVariants.size(), tRow.path() + ": exactly the 4 facing variants");
			for (var tFacing : ROT_Y.keySet()) {
				JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
				assertEquals("gt6:block/" + tRow.path(), tVariant.get("model").getAsString(),
						tRow.path() + " facing=" + tFacing + ": the row model");
				assertEquals(ROT_Y.get(tFacing).intValue(),
						tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
						tRow.path() + " facing=" + tFacing + ": the crosswise rotationY table (NOT the furnace front table)");
			}
		}
	}

	// ------------------------------------------------------------------
	// the item parent chain
	// ------------------------------------------------------------------

	@Test
	public void itemModelsParentTheRowBlockModel() throws IOException {
		for (var tRow : gregtech6.registry.GT6Anvils.ROWS) {
			JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
			assertEquals("gt6:block/" + tRow.path(), tItem.get("parent").getAsString(),
					tRow.path() + ": the item parents the row model (the 3D silhouette chain)");
		}
	}
}
