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
 * The explosives-chain press pour test (the GT6RecipeMapDataB1RowsPourTest posture): the
 * dynamite/boomstick rows of Loader_Recipes_Other.java:641-655 pour through the real
 * {@link GT6RecipeMapJsonLoader} seam and the verbatim spot checks read the shipped row
 * objects directly.
 *
 * <p>Row accounting (the acceptance 65 to 89 census): the ANY.SiO2 walk is FOUR statements
 * (:642 dust/string, :643 dust/fiber, :644 blockDust/string, :645 blockDust/fiber) over the
 * PORT-universe subset of the group — {SiO2, NetherQuartz, CertusQuartz, Quartzite}, the
 * press.json header's declared expansion — so 4 x 4 = 16 rows, plus the four Dynamite rows
 * (:647-650) and the four Dynamite_Strong rows (:652-655) = 24 new rows.
 *
 * <p>The {@code ST.tag(1)}/{@code ST.tag(2)} selector legs are DROPPED (the loom.json
 * precedent, declared deviation): the JSON v1 row schema carries no damage/NBT faces and the
 * configured Integrated Circuit is not expressible as a plain item id. Dropping the selector
 * does not collide the Dynamite and Strong rows — the dust amounts differ (1 vs 2), so each
 * row still answers uniquely without the circuit.
 *
 * <p>The bullet-casing rows (:657-668) stay POOLED (the card-scope boundary): they consume
 * the Shape_Press molds not-consumably, a face the JSON v1 row schema does not carry either.
 */
public class GT6ExplosivesPressRowsPourTest extends GTRecipesOfflineTestBase {

	private static final int THE_PRESS_CENSUS = 178; // 89 with the explosives rows + 89 press-electrodes rows (MultiItemTechnological.java:502-543, task press-electrodes)

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		// the hermetic bracket (task toolhead-r11e-press-mortar): the reset FIRST — the
		// neoforge junit FML boot runs the whole static pour suite at modloading, so a bare
		// init() no-ops there and the census would count boot residue
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

	/** Reads the shipped press file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShippedPress() throws Exception {
		String tPath = "/data/gt6/recipe_maps/press.json";
		InputStream tStream = GT6ExplosivesPressRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", "press"), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the 65 baseline rows + the 24 explosives rows + the 89 electrode rows, zero skips. */
	@Test
	public void thePressFilePoursTheExplosivesCensusWithZeroSkips() throws Exception {
		pourShippedPress();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("press");
		assertNotNull(tMap, "press resolves");
		assertEquals(THE_PRESS_CENSUS, tMap.mRecipeList.size(), "press: the map holds the census");
		assertEquals(THE_PRESS_CENSUS, GT6RecipeMapJsonLoader.pouredCount("press"),
				"press: the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The 24 explosives rows ride the file, cited per upstream statement. */
	@Test
	public void theFileCitesExactlyTheTwentyFourExplosiveRows() throws Exception {
		JsonArray tRows = pourShippedPress();
		int tExplosives = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue;
			String tComment = tRow.get("comment").getAsString();
			if (tComment.startsWith("Loader_Recipes_Other.java:64") || tComment.startsWith("Loader_Recipes_Other.java:65")) {
				tExplosives++;
			}
		}
		assertEquals(24, tExplosives, "16 ANY.SiO2 walk rows + 4 Dynamite + 4 Dynamite_Strong");
	}

	/**
	 * Spot check 1 — the ANY.SiO2 dust/string leg xSiO2 (Loader_Recipes_Other.java:642, the
	 * SiO2 member iteration): 2x SiO2 dust + 2x Gunpowder dust + 1x string -> 1x Boomstick
	 * at 16 t / 16 EUt, the selector-less form of the dropped ST.tag face.
	 */
	@Test
	public void boomstickDustStringLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Other.java:642 — Boomstick, dust leg xSiO2");
		assertEquals(16, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong(), "the addRecipeX eut column");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(3, tInputs.size(), "2x SiO2 dust + 2x Gunpowder dust + 1x string (the ST.tag leg dropped)");
		assertEquals("gt6:dust_silicon_dioxide", slotId(tInputs.get(0)));
		assertEquals(2, slotCount(tInputs.get(0)));
		assertEquals("gt6:dust_gunpowder", slotId(tInputs.get(1)));
		assertEquals(2, slotCount(tInputs.get(1)));
		assertEquals("minecraft:string", slotId(tInputs.get(2)), "ST.make(Items.string, 1, W)");
		assertEquals(1, slotCount(tInputs.get(2)));
		assertEquals("gt6:boomstick", slotId(tRow.getAsJsonArray("outputs").get(0)), "IL.Boomstick.get(1)");
		assertEquals(1, slotCount(tRow.getAsJsonArray("outputs").get(0)));
	}

	/**
	 * Spot check 2 — the ANY.SiO2 blockDust/fiber leg xQuartzite (:645, the last group
	 * member): 2x Quartzite block dust + 2x Gunpowder block dust + 9x plant fiber -> 9x
	 * Boomstick at 144 t / 16 EUt.
	 */
	@Test
	public void boomstickBlockDustFiberLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Other.java:645 — Boomstick, blockDust leg xQuartzite");
		assertEquals(144, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(3, tInputs.size());
		assertEquals("gt6:block_dust_quartzite", slotId(tInputs.get(0)));
		assertEquals(2, slotCount(tInputs.get(0)));
		assertEquals("gt6:block_dust_gunpowder", slotId(tInputs.get(1)));
		assertEquals(2, slotCount(tInputs.get(1)));
		assertEquals("gt6:plant_gt_fiber_wood", slotId(tInputs.get(2)),
				"plantGtFiber.mRegisteredPrefixItems.get(0) — the Wood fiber, the registered-first representative");
		assertEquals(9, slotCount(tInputs.get(2)));
		assertEquals("gt6:boomstick", slotId(tRow.getAsJsonArray("outputs").get(0)));
		assertEquals(9, slotCount(tRow.getAsJsonArray("outputs").get(0)));
	}

	/**
	 * Spot check 3 — the Dynamite dust/string leg (:647): 1x Dynamite dust + 1x string ->
	 * 1x Dynamite at 16 t / 16 EUt; the ST.tag(1) selector leg dropped (the loom precedent).
	 */
	@Test
	public void dynamiteDustStringLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Other.java:647 — Dynamite, dust leg");
		assertEquals(16, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(2, tInputs.size(), "the ST.tag(1) selector leg is DROPPED, not a third slot");
		assertEquals("gt6:dust_dynamite", slotId(tInputs.get(0)), "dust.mat(MT.Dynamite, 1)");
		assertEquals(1, slotCount(tInputs.get(0)));
		assertEquals("minecraft:string", slotId(tInputs.get(1)));
		assertEquals(1, slotCount(tInputs.get(1)));
		assertEquals("gt6:dynamite", slotId(tRow.getAsJsonArray("outputs").get(0)), "IL.Dynamite.get(1)");
		assertEquals(1, slotCount(tRow.getAsJsonArray("outputs").get(0)));
	}

	/**
	 * Spot check 4 — the Dynamite_Strong blockDust/string leg (:654): 2x Dynamite block
	 * dust + 9x string -> 9x Strong Dynamite at 576 t / 16 EUt; the doubled dust amount (2
	 * vs the Dynamite rows' 1) keeps the two selector-less families non-colliding.
	 */
	@Test
	public void dynamiteStrongBlockDustStringLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Other.java:654 — Dynamite_Strong, blockDust leg");
		assertEquals(576, tRow.get("duration").getAsLong(), "144 x 4 (the addRecipeX duration column)");
		assertEquals(16, tRow.get("eut").getAsLong());
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(2, tInputs.size());
		assertEquals("gt6:block_dust_dynamite", slotId(tInputs.get(0)));
		assertEquals(2, slotCount(tInputs.get(0)), "the doubled Dynamite amount — the ST.tag(2) semantic stand-in");
		assertEquals("minecraft:string", slotId(tInputs.get(1)));
		assertEquals(9, slotCount(tInputs.get(1)));
		assertEquals("gt6:dynamite_strong", slotId(tRow.getAsJsonArray("outputs").get(0)), "IL.Dynamite_Strong.get(9)");
		assertEquals(9, slotCount(tRow.getAsJsonArray("outputs").get(0)));
	}

	/**
	 * The pool boundary: the bullet-casing rows (:657-668) stay pooled — no row cites them
	 * (the B1 test's :254-259 pool pin form). The molds are registered by this card; the
	 * not-consumable press rows are the bullet-card face.
	 */
	@Test
	public void theBulletCasingRowsStayPooled() throws Exception {
		JsonArray tRows = pourShippedPress();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tComment = tRow.has("comment") ? tRow.get("comment").getAsString() : "";
			assertTrue(!tComment.contains("Shape_Press_Bullet_Casing") && !tComment.contains("bulletGt"),
					"the bullet-casing press rows stay pooled (the card-scope boundary; the bullet card reclaims them)");
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Finds the one row whose comment starts with the given source citation. */
	private static JsonObject findRow(JsonArray aRows, String aCommentPrefix) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(aCommentPrefix)) return tRow;
		}
		throw new AssertionError("no row cited " + aCommentPrefix);
	}

	private static String slotId(JsonElement aSlot) {
		JsonObject tSlot = aSlot.getAsJsonObject();
		return tSlot.has("item") ? tSlot.get("item").getAsString() : tSlot.get("fluid").getAsString();
	}

	private static int slotCount(JsonElement aSlot) {
		return aSlot.getAsJsonObject().has("count") ? aSlot.getAsJsonObject().get("count").getAsInt() : 1;
	}
}
