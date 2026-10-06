/**
 * Offline pin for task debt-material-tint — the #40-41 declared-deviation rework.
 * The borrowed grayscale materialicons blockSolid bodies (the
 * {@code GT6CrucibleDatagen.bodyTexture} rows) now carry tintindex 0 on every body face,
 * and {@code GT6MoldTintListener} multiplies the row material's {@code mRGBaSolid} over
 * them (the upstream getTextureSmooth(mRGBaSolid, F) colour semantics,
 * OreDictMaterial.java:980-987). The vanilla smooth-stone rows (mold_stone,
 * smeltery_stone_empty) are FINISHED textures — a second multiply would
 * dirty them, so they stay un-tinted (the recorded declaration shortcut); the
 * faucet_stone row rides the same three-element tint seat as its ceramic sibling since
 * task tap-funnel-model-audit — its inertness is the listener dispatch (the finished-
 * texture branch answers -1), not the model. Since task
 * crucible-large-ber the filled content boxes carry tintindex 1 — the per-BE seat the
 * GT6MoldTintListener index-1 arm answers at runtime; the raw clay items stay un-tinted.
 *
 * <p>Reads the committed generated tree on the classpath (the
 * {@link GT6MoldAssetDatagenTest} form — no datagen run) and derives the universe from
 * the registration rows, so a new ceramic row joins the pin automatically.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.registry.GT6MaterialTestSupport;

public class GT6MoldTintDatagenTest extends GTOfflineTestBase {

	/** The six face keys every cube/stamp element serializes. */
	private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

	/**
	 * The mRGBaSolid tints — pinned literally from the MT.java static-block rows (Ceramic
	 * 220/130/70, Bronze 210/130/60, Steel 130/130/130), NOT derived from production code
	 * (a tautology pins nothing). Steel's setRGBaLiquid tail recolours only the LIQUID row;
	 * the solid stays the grey.
	 */
	private static final int CERAMIC_TINT = 0xFFDC8246;
	private static final int BRONZE_TINT  = 0xFFD2823C;
	private static final int STEEL_TINT   = 0xFF828282;

	@BeforeAll
	static void bootMaterials() {
		// the row materials resolve through MT.init (the GTMachinePaintTintTest shape —
		// the bare JVM leaves the MT statics null)
		GT6MaterialTestSupport.materials(); // the hermetic bracket (task hermetic-pour-tests)
	}

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6MoldTintDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	private static void assertEveryFaceTinted(JsonObject aElement, String aLabel) {
		JsonObject tFaces = aElement.getAsJsonObject("faces");
		for (String tFace : FACES) {
			assertTrue(tFaces.has(tFace), aLabel + ": the " + tFace + " face exists");
			JsonElement tTint = tFaces.getAsJsonObject(tFace).get("tintindex");
			assertNotNull(tTint, aLabel + ": the " + tFace + " face carries a tintindex");
			assertEquals(0, tTint.getAsInt(), aLabel + ": the " + tFace + " face tintindex is 0");
		}
	}

	/**
	 * The stamp-element form: every face PRESENT carries tintindex 0, with a face-count
	 * floor so the walk cannot go vacuous. The mold rim posts (MOLD_BOUNDS[6..17]) carry
	 * only their four horizontal faces by the upstream texture gate
	 * (MultiTileEntityMold.java getTexture2 :522-533 — no up/down, whose y6/y4 planes
	 * would coplanar z-fight the cap bottom / wall top), so the strict six-face cube
	 * assertion above cannot ride the stamp walk.
	 */
	private static void assertEveryPresentFaceTinted(JsonObject aElement, String aLabel) {
		JsonObject tFaces = aElement.getAsJsonObject("faces");
		assertTrue(tFaces.size() >= 4, aLabel + ": at least the four horizontal faces exist");
		for (String tFace : FACES) {
			if (!tFaces.has(tFace)) continue;
			JsonElement tTint = tFaces.getAsJsonObject(tFace).get("tintindex");
			assertNotNull(tTint, aLabel + ": the " + tFace + " face carries a tintindex");
			assertEquals(0, tTint.getAsInt(), aLabel + ": the " + tFace + " face tintindex is 0");
		}
	}

	private static void assertNoTintAnywhere(String aModelPath, JsonObject aModel, String aLabel) {
		assertFalse(aModel.toString().contains("tintindex"),
				aLabel + " (" + aModelPath + "): the finished/vanilla row stays un-tinted");
	}

	/** All 31 ceramic mold stamps: every element face tintindex 0 (the grayscale rough borrow). */
	@Test
	void everyCeramicMoldFaceCarriesTintIndexZero() throws Exception {
		List<GT6Molds.MoldRow> tRows = new ArrayList<>(GT6Molds.CERAMIC_ROWS);
		tRows.add(GT6Molds.CERAMIC_BLANK_ROW);
		assertEquals(31, tRows.size(), "the ceramic universe walk broke — never pass vacuously");
		for (GT6Molds.MoldRow tRow : tRows) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + ".json");
			for (JsonElement tElement : tModel.getAsJsonArray("elements")) {
				assertEveryPresentFaceTinted(tElement.getAsJsonObject(), tRow.path());
			}
		}
	}

	/** The stone mold rides the vanilla smooth stone — zero tintindex anywhere (geometry intact). */
	@Test
	void theStoneMoldStaysUnTinted() throws Exception {
		JsonObject tModel = generatedJson("assets/gt6/models/block/mold_stone.json");
		assertTrue(tModel.has("elements"), "the stone stamp keeps the bitmap geometry");
		assertNoTintAnywhere("assets/gt6/models/block/mold_stone.json", tModel, "mold_stone");
	}

	/** The grayscale smeltery empties tint (the bowl shell, every face); the stone bowl and every filled content box do not. */
	@Test
	void theGrayscaleSmelteryEmptiesCarryTheTintAndStoneDoesNot() throws Exception {
		for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
			JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + "_empty.json");
			if (tRow.path().endsWith("_stone")) {
				assertNoTintAnywhere(tRow.path() + "_empty", tModel, tRow.path());
			} else {
				assertEquals(5, tModel.getAsJsonArray("elements").size(), tRow.path() + ": the bowl shell (4 walls + floor)");
				// the bowl walls carry 2-3 faces each (the :610-619 null-gate) — every PRESENT
				// face tints, no face-count floor (that floor is the mold-stamp walk's shape)
				for (JsonElement tElement : tModel.getAsJsonArray("elements")) {
					JsonObject tFaces = tElement.getAsJsonObject().getAsJsonObject("faces");
					assertTrue(tFaces.size() >= 2, tRow.path() + "_empty: the element renders faces");
					for (String tFace : FACES) {
						if (!tFaces.has(tFace)) continue;
						assertEquals(0, tFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
								tRow.path() + "_empty: the " + tFace + " face carries tintindex 0");
					}
				}
			}
			// the per-level content boxes (task crucible-bowl-model, upgraded task
			// crucible-large-ber): the content material is per-BE data the static model
			// cannot know — the up face rides tintindex 1 and the GT6MoldTintListener
			// index-1 arm answers the synced displayed material at runtime. Since
			// r11a-crucible-filled-shell the filled child carries the FULL shell+content
			// element set (vanilla getElements never merges the parent), so the content box
			// is located by its #content up face, not by the append-era index 0.
			for (int tLevel = 1; tLevel <= 8; tLevel++) {
				JsonObject tFilled = generatedJson("assets/gt6/models/block/" + tRow.path() + "_filled_" + tLevel + ".json");
				JsonObject tContentUp = null;
				for (JsonElement tElement : tFilled.getAsJsonArray("elements")) {
					JsonObject tFaces = tElement.getAsJsonObject().getAsJsonObject("faces");
					if (tFaces.has("up") && "#content".equals(tFaces.getAsJsonObject("up").get("texture").getAsString())) {
						tContentUp = tFaces.getAsJsonObject("up");
					}
				}
				assertNotNull(tContentUp, tRow.path() + "_filled_" + tLevel + ": the content box element");
				assertEquals(1, tContentUp.get("tintindex").getAsInt(),
						tRow.path() + "_filled_" + tLevel + ": the content seat is tintindex 1");
			}
		}
	}

	/**
	 * The faucet stack (task tap-funnel-model-audit re-formed it from the cube_all
	 * placeholder): BOTH rows ride the one three-element builder with tintindex 0 on
	 * every face — the stone row's inertness is the LISTENER's property (its dispatch
	 * answers -1, pinned at materialTintPinsMatchTheUpstreamRGBa), not the model's.
	 */
	@Test
	void theFaucetStacksSeatTheTintAndStoneDispatchesInert() throws Exception {
		JsonObject tCeramic = generatedJson("assets/gt6/models/block/faucet_ceramic.json");
		assertEquals(3, tCeramic.getAsJsonArray("elements").size(), "faucet_ceramic: the three-pass stack");
		for (JsonElement tElement : tCeramic.getAsJsonArray("elements")) {
			assertEveryFaceTinted(tElement.getAsJsonObject(), "faucet_ceramic");
		}
		JsonObject tStone = generatedJson("assets/gt6/models/block/faucet_stone.json");
		assertEquals(3, tStone.getAsJsonArray("elements").size(), "faucet_stone: the same three-pass stack");
		for (JsonElement tElement : tStone.getAsJsonArray("elements")) {
			assertEveryFaceTinted(tElement.getAsJsonObject(), "faucet_stone");
		}
		// the stone row's inertness lives in the dispatch (the finished-texture branch),
		// already pinned in materialTintPinsMatchTheUpstreamRGBa — not in the model
	}

	/** The tint rule mirrors bodyTexture's own vanilla branch (the single mapping source). */
	@Test
	void bodyTintedMirrorsTheVanillaNamespaceBranch() {
		assertFalse(GT6CrucibleDatagen.bodyTinted(gregapi.data.MT.Stone), "stone rides the vanilla finished texture");
		assertTrue(GT6CrucibleDatagen.bodyTinted(gregapi.data.MT.Ceramic), "ceramic rides the rough borrow");
		assertTrue(GT6CrucibleDatagen.bodyTinted(gregapi.data.MT.Bronze), "bronze rides the copper borrow");
		assertTrue(GT6CrucibleDatagen.bodyTinted(gregapi.data.MT.Steel), "steel rides the metallic borrow");
	}

	/** The handler colours = the mRGBaSolid pack, the upstream getTextureSmooth multiply seat. */
	@Test
	void materialTintPinsMatchTheUpstreamRGBa() {
		assertEquals(CERAMIC_TINT, gregtech6.client.GT6MoldTintListener.materialTintARGB(gregapi.data.MT.Ceramic, 0));
		assertEquals(BRONZE_TINT, gregtech6.client.GT6MoldTintListener.materialTintARGB(gregapi.data.MT.Bronze, 0));
		assertEquals(STEEL_TINT, gregtech6.client.GT6MoldTintListener.materialTintARGB(gregapi.data.MT.Steel, 0));
		assertEquals(-1, gregtech6.client.GT6MoldTintListener.materialTintARGB(gregapi.data.MT.Stone, 0),
				"the vanilla smooth-stone row answers no-tint");
		assertEquals(-1, gregtech6.client.GT6MoldTintListener.materialTintARGB(gregapi.data.MT.Ceramic, 1),
				"a foreign tint index answers no-tint");
		assertEquals(-1, gregtech6.client.GT6MoldTintListener.materialTintARGB(null, 0),
				"a null material answers no-tint");
	}
}
