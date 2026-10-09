package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Task robotics-chain — the Boxinator row-stock pour (the GT6RecipeMapDataB1RowsPourTest
 * fixture posture): the 10 tip-packing rows pour through the real
 * {@link GT6RecipeMapJsonLoader} seam appended to the shipped boxinator.json
 * (MultiItemRandomTools.java:503-512), zero skips, and the verbatim spot checks read the
 * shipped row objects directly. The upstream {@code IL.Robot_Tip_*.get(0)} size-0 input
 * (the never-consumed tip template) is carried at count 1 — the consume-time face rides
 * {@code Recipe.sNotConsumable} (the id478 mold archaeology shape), so the JSON face
 * stays the loader's count-1 schema.
 */
public class GT6RoboticsRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The upstream census: 29 seated rows (GT6_Main:350 map row + the landed cards' walks) + this card's 10 (:503-512) + the recipe-b7 pack walk 12319 (the seat14 rebase roll: the same-key ratchet takes the semantic union). */
	private static final int BOXINATOR_CENSUS = 12358;

	/**
	 * The 10 rows in upstream order: tip kind, the output token count, the input leg
	 * (the :509 Rubber row walks {@code OP.nugget.dat(MT.Rubber)}, every other row
	 * {@code OP.plateTiny.dat(MT.Steel)}).
	 */
	private static final String[][] ROWS = {
			{"wrench"     ,  "2", "gt6:plate_tiny_steel"},
			{"screwdriver", "32", "gt6:plate_tiny_steel"},
			{"saw"        , "32", "gt6:plate_tiny_steel"},
			{"hammer"     ,  "2", "gt6:plate_tiny_steel"},
			{"cutter"     ,  "4", "gt6:plate_tiny_steel"},
			{"chisel"     ,  "9", "gt6:plate_tiny_steel"},
			{"rubber"     ,  "4", "gt6:nugget_rubber"   },
			{"blade"      , "32", "gt6:plate_tiny_steel"},
			{"drill"      , "32", "gt6:plate_tiny_steel"},
			{"file"       ,  "9", "gt6:plate_tiny_steel"},
	};

	@BeforeEach
	void freshGeneration() {
		// the reset FIRST (the toolhead-r11e hermetic form: the neoforge junit FML boot runs
		// the whole static pour suite at modloading — a bare init() no-ops there)
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId ->
				"gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK; // identity stand-ins
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped boxinator.json verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/boxinator.json";
		InputStream tStream = GT6RoboticsRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", "boxinator"), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the whole file pours — 29 seated + this card's 10 + the recipe-b7 pack walk 12319, zero WARN-skipped rows. */
	@Test
	public void boxinatorPoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("boxinator");
		assertNotNull(tMap, "boxinator resolves");
		assertEquals(BOXINATOR_CENSUS, tMap.mRecipeList.size(), "boxinator: the map holds the census");
		assertEquals(BOXINATOR_CENSUS, GT6RecipeMapJsonLoader.pouredCount("boxinator"),
				"boxinator: the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The verbatim face: the 10 appended rows are the :503-:512 transcription (input leg, tip, token count, duration = 16xN, eut 16). */
	@Test
	public void theTenAppendedRowsAreTheUpstreamTranscription() throws Exception {
		JsonArray tRows = pourShipped();
		assertEquals(BOXINATOR_CENSUS, tRows.size(), "the file holds the seated rows plus this card's ten");
		for (int i = 0; i < ROWS.length; i++) {
			// the seat14 rebase roll: the recipe-b7 pack walk appends AFTER this card's rows, so the
			// old last-ten positional slice now lands in the walk — the row is picked by its upstream
			// line anchor instead (the b7 card's own content-pin medicine, the duration-32 face form)
			JsonObject tRow = null;
			for (JsonElement tElement : tRows) {
				JsonObject tCandidate = tElement.getAsJsonObject();
				if (tCandidate.has("comment") && tCandidate.get("comment").getAsString().contains("MultiItemRandomTools.java:" + (503 + i))) { tRow = tCandidate; break; }
			}
			String tKind = ROWS[i][0];
			int tCount = Integer.parseInt(ROWS[i][1]);
			String tAnchor = "the :5" + String.format("%02d", 3 + i) + " row (Robot_Tip_" + tKind + ")";
			assertNotNull(tRow, tAnchor + ": the row must exist in the shipped file");

			JsonArray tInputs = tRow.getAsJsonArray("inputs");
			assertEquals(2, tInputs.size(), tAnchor + ": the plateTiny/nugget leg + the tip template");
			JsonObject tLeg = tInputs.get(0).getAsJsonObject();
			assertEquals(ROWS[i][2], tLeg.get("item").getAsString(), tAnchor + ": the input leg");
			assertEquals(1, tLeg.get("count").getAsInt(), tAnchor + ": the leg rides at count 1");
			JsonObject tTip = tInputs.get(1).getAsJsonObject();
			assertEquals("gt6:robot_arm_" + tKind + "_tip", tTip.get("item").getAsString(), tAnchor + ": the tip template");
			assertEquals(1, tTip.get("count").getAsInt(), tAnchor + ": the tip rides at count 1 (the size-0 face is the consume-time sNotConsumable seam)");

			JsonArray tOutputs = tRow.getAsJsonArray("outputs");
			assertEquals(1, tOutputs.size(), tAnchor + ": single output");
			JsonObject tToken = tOutputs.get(0).getAsJsonObject();
			assertEquals("gt6:single_use_" + tKind, tToken.get("item").getAsString(), tAnchor + ": the token output");
			assertEquals(tCount, tToken.get("count").getAsInt(), tAnchor + ": the token count");

			assertEquals(16L * tCount, tRow.get("duration").getAsLong(), tAnchor + ": duration = 16xN");
			assertEquals(16L, tRow.get("eut").getAsLong(), tAnchor + ": eut 16");
			assertTrue(tRow.has("comment") && tRow.get("comment").getAsString().contains("MultiItemRandomTools.java"),
					tAnchor + ": the upstream line anchor rides the comment");
		}
	}
}
