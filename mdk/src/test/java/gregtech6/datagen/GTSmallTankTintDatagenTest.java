package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Offline pin for task small-tank-colored-tint — the SEAT half over the committed
 * generated tree: the colored-band faces of the four shared empty models carry
 * {@code tintindex 0} exactly, the 0.01 overlay shells and the filled children's
 * smeltery_content fluid boxes carry NO tintindex (the P22 decal split — upstream
 * BlockTextureDefault(overlay)/BlockTextureFluid carry no mRGBa, GasCylinder :141 /
 * Cell :42-44 / Cup :68-73 / Jug :71-76). The families share ONE model family per card,
 * so the four files census every row. The COLOUR half (the carriers + the fRGBaSolid
 * literals) is pinned in the package-private dispatch's own driver
 * GTMachinePaintTintTest.smallTankFamiliesRideTheCombinedDispatch.
 */
class GTSmallTankTintDatagenTest {

	/** One committed generated-tree JSON as an object (the GT6CellDatagenTest classpath form). */
	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GTSmallTankTintDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** Walks every face of every element; returns [tinted, untinted] counts and asserts the seat split. */
	private static int[] censusModel(String aPath, String aModelName) throws Exception {
		JsonObject tModel = generatedJson(aPath);
		assertTrue(tModel.has("elements"), aModelName + " is an element model");
		int tTinted = 0, tUntinted = 0;
		for (JsonElement tElement : tModel.getAsJsonArray("elements")) {
			for (Map.Entry<String, JsonElement> tFace : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
				JsonObject tFaceObj = tFace.getValue().getAsJsonObject();
				String tTexture = tFaceObj.get("texture").getAsString();
				if (tTexture.startsWith("#overlay_")) {
					assertFalse(tFaceObj.has("tintindex"),
							aModelName + " face " + tFace.getKey() + " rides the overlay band — no tint seat (the P22 split)");
					tUntinted++;
				} else {
					assertEquals(0, tFaceObj.get("tintindex").getAsInt(),
							aModelName + " face " + tFace.getKey() + " rides the colored band — the tintindex-0 seat");
					tTinted++;
				}
			}
		}
		return new int[] {tTinted, tUntinted};
	}

	/** The colored band IS the seat: every non-overlay face tintindex 0, every overlay face bare — counts exact. */
	@Test
	void theColoredBandsAreTheTintindexZeroSeats() throws Exception {
		// gas cylinder: 5 colored elements (3 bell bodies + 2 all-barometer arms, the :141
		// aRenderPass>2 tint) + 5 overlay shells = 30 tinted / 30 bare
		int[] tGas = censusModel("assets/gt6/models/block/barometer_gas_cylinder.json", "barometer_gas_cylinder");
		assertEquals(30, tGas[0], "the gas cylinder's 5 colored elements are 30 seated faces");
		assertEquals(30, tGas[1], "the gas cylinder's 5 overlay shells are 30 bare faces");
		// cell: the shell + the insides box colored, both overlay twins bare = 12 / 12
		int[] tCell = censusModel("assets/gt6/models/block/cell_container_empty.json", "cell_container_empty");
		assertEquals(12, tCell[0], "the cell's shell + insides are 12 seated faces (the :42-44 mRGBa passes)");
		assertEquals(12, tCell[1], "the cell's overlay twins are 12 bare faces");
		// cup: 4 walls + the bottom slab colored, their overlay twins bare = 30 / 30
		int[] tCup = censusModel("assets/gt6/models/block/porcelain_cup_empty.json", "porcelain_cup_empty");
		assertEquals(30, tCup[0], "the cup's walls + slab are 30 seated faces (the :68-73 mRGBa passes)");
		assertEquals(30, tCup[1], "the cup's overlay twins are 30 bare faces");
		// jug: the same wall+slab grammar over the jug geometry = 30 / 30
		int[] tJug = censusModel("assets/gt6/models/block/ceramic_jug_empty.json", "ceramic_jug_empty");
		assertEquals(30, tJug[0], "the jug's walls + body slab are 30 seated faces (the :71-76 mRGBa passes)");
		assertEquals(30, tJug[1], "the jug's overlay twins are 30 bare faces");
	}

	/** The filled children's smeltery_content fluid boxes stay untinted (upstream BlockTextureFluid carries no mRGBa). */
	@Test
	void theFilledFluidBoxesStayUntinted() throws Exception {
		// every filled child carries the FULL shell+fluid set in itself now — the cup rides
		// the r11a-crucible-filled-shell fix (in main), the cell + jug ride the
		// filled-shell-family-2 fix (vanilla getElements never merges the parent once the
		// child carries elements); the surviving invariant: the #content faces never tint,
		// and the fluid box is the only #content consumer
		Set<String> tFullFilled = Set.of(
				"assets/gt6/models/block/cell_container_filled_3.json",
				"assets/gt6/models/block/ceramic_jug_filled_3.json",
				"assets/gt6/models/block/porcelain_cup_filled_3.json");
		for (String tPath : tFullFilled) {
			JsonObject tModel = generatedJson(tPath);
			assertTrue(tModel.get("parent").getAsString().endsWith("_empty"),
					tPath + " parents the seated shell (the texture map)");
			int tContentFaces = 0;
			for (JsonElement tElement : tModel.getAsJsonArray("elements")) {
				for (Map.Entry<String, JsonElement> tFace : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
					if ("#content".equals(tFace.getValue().getAsJsonObject().get("texture").getAsString())) {
						tContentFaces++;
						assertFalse(tFace.getValue().getAsJsonObject().has("tintindex"),
								tPath + " face " + tFace.getKey() + " — the fluid box never tints");
					}
				}
			}
			assertEquals(6, tContentFaces, tPath + ": the fluid box (six faces) is the only #content consumer");
		}
	}
}
