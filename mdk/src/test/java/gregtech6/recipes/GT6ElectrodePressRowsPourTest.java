package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

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
 * The press-electrodes press pour test (the GT6ExplosivesPressRowsPourTest posture): the
 * Forestry electrode rows of MultiItemTechnological.java:502-543 pour through the real
 * {@link GT6RecipeMapJsonLoader} seam and the verbatim spot checks read the shipped row
 * objects directly.
 *
 * <p>Row accounting (the acceptance 89 to 178 census): the registration face is the MIT
 * :488-500 electrode thirteen (task press-electrodes, {@code GT6Electrodes}); the rows are
 * 31 upstream statements whose five material walks (ANY.Cu :502/:521, ANY.Iron :506/:525,
 * ANY.Diamond :509/:528, ANY.Rubber :513/:532, ANY.Emerald :515/:534) expand over the
 * PORT-universe subset of each group, folded by input item-truth (a member rides only when
 * the port registers both its stick and its bolt — the b-series fold; the GTMaterialItems
 * live dump 2026-10-04):
 * <ul>
 * <li>ANY.Cu = {AnnealedCopper, Copper} (2);</li>
 * <li>ANY.Iron = {CastIron, Iron, Enori, WroughtIron, Knightmetal, MeteoricIron,
 *     MeteoricSteel, Steel, PigIron, Meteorite, IronCompressed} (11);</li>
 * <li>ANY.Diamond = 12 members, all item-true; ANY.Rubber = {Rubber} (1); ANY.Emerald =
 *     8 members, all item-true.</li>
 * </ul>
 * Block 1 (:502-519, output x1): 2+1+1+11+1+12+1+1+1+8+1+1+1 = 42. Block 2 (:521-539,
 * output x2): 41 + the two Ender legs (:538 dust, :539 gem) = 43. Block 3 (:540-543, the
 * Ender dust/gem x4 legs) = 4. Total 42+43+4 = 89 new rows, 89+89 = 178.
 *
 * <p>All rows ride the addRecipeX(T, 16, 64, ...) columns: 16 EUt / 64 t verbatim.
 */
public class GT6ElectrodePressRowsPourTest extends GTRecipesOfflineTestBase {

	private static final int THE_PRESS_CENSUS = 178; // 89 baseline + 89 electrode rows
	private static final int THE_ELECTRODE_ROWS = 89;

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
		InputStream tStream = GT6ElectrodePressRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "press"), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: the 89 baseline rows + the 89 electrode rows, zero skips. */
	@Test
	public void thePressFilePoursTheElectrodeCensusWithZeroSkips() throws Exception {
		pourShippedPress();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("press");
		assertNotNull(tMap, "press resolves");
		assertEquals(THE_PRESS_CENSUS, tMap.mRecipeList.size(), "press: the map holds the census");
		assertEquals(THE_PRESS_CENSUS, GT6RecipeMapJsonLoader.pouredCount("press"),
				"press: the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** Exactly 89 electrode rows ride the file, cited per upstream statement. */
	@Test
	public void theFileCitesExactlyTheEightyNineElectrodeRows() throws Exception {
		JsonArray tRows = pourShippedPress();
		int tElectrodes = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith("MultiItemTechnological.java:")) {
				tElectrodes++;
			}
		}
		assertEquals(THE_ELECTRODE_ROWS, tElectrodes, "42 x1 legs + 43 x2 legs + 4 x4 Ender legs");
	}

	/** The per-statement walk accounting (the class doc table), each cell pinned against the file. */
	@Test
	public void theWalkAccountingMatchesTheDeclaredPortUniverse() throws Exception {
		JsonArray tRows = pourShippedPress();
		Map<String, Integer> tPerStatement = new java.util.LinkedHashMap<>(Map.ofEntries(
				Map.entry(":503", 2), Map.entry(":504", 1), Map.entry(":505", 1), // the Cu walk + Sn + Bronze
				Map.entry(":507", 11), Map.entry(":508", 1), // the Iron walk + Au
				Map.entry(":510", 12), Map.entry(":511", 1), Map.entry(":512", 1), // the Diamond walk + Obsidian + Blaze
				Map.entry(":514", 1), Map.entry(":516", 8), // the Rubber walk + the Emerald walk
				Map.entry(":517", 1), Map.entry(":518", 1), Map.entry(":519", 1), // Apatite + Lapis + Ender x1
				Map.entry(":522", 2), Map.entry(":523", 1), Map.entry(":524", 1), // x2 legs: Cu walk + Sn + Bronze
				Map.entry(":526", 11), Map.entry(":527", 1), // Iron walk + Au
				Map.entry(":529", 12), Map.entry(":530", 1), Map.entry(":531", 1), // Diamond walk + Obsidian + Blaze
				Map.entry(":533", 1), Map.entry(":535", 8), // Rubber walk + Emerald walk
				Map.entry(":536", 1), Map.entry(":537", 1), // Apatite + Lapis
				Map.entry(":538", 1), Map.entry(":539", 1), // Ender x2, the dust and gem legs
				Map.entry(":540", 1), Map.entry(":541", 1), Map.entry(":542", 1), Map.entry(":543", 1) // Ender x4 quartet
		));
		for (Map.Entry<String, Integer> tCell : tPerStatement.entrySet()) {
			long tCount = 0;
			for (JsonElement tElement : tRows) {
				JsonObject tRow = tElement.getAsJsonObject();
				if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith("MultiItemTechnological.java" + tCell.getKey() + " ")) {
					tCount++;
				}
			}
			assertEquals(tCell.getValue().longValue(), tCount, "MultiItemTechnological.java" + tCell.getKey() + " expands to the declared port-universe rows");
		}
	}

	/**
	 * Spot check 1 — the ANY.Cu walk xAnnealedCopper, the first group member (:503): 2x
	 * annealed-copper stick + 2x bolt + 2x small redstone dust -> 1x copper electrode at
	 * 64 t / 16 EUt.
	 */
	@Test
	public void copperWalkAnnealedCopperLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "MultiItemTechnological.java:503 — Electrode (Copper), Cu walk xAnnealedCopper");
		assertEquals(64, tRow.get("duration").getAsLong(), "the addRecipeX duration column");
		assertEquals(16, tRow.get("eut").getAsLong(), "the addRecipeX eut column");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(3, tInputs.size());
		assertEquals("gt6:stick_annealed_copper", slotId(tInputs.get(0)), "OP.stick.mat(tMat, 2)");
		assertEquals(2, slotCount(tInputs.get(0)));
		assertEquals("gt6:bolt_annealed_copper", slotId(tInputs.get(1)), "OP.bolt.mat(tMat, 2)");
		assertEquals(2, slotCount(tInputs.get(1)));
		assertEquals("gt6:dust_small_redstone", slotId(tInputs.get(2)), "OP.dustSmall.mat(MT.Redstone, 2)");
		assertEquals(2, slotCount(tInputs.get(2)));
		assertEquals("gt6:electrode_fr_copper", slotId(tRow.getAsJsonArray("outputs").get(0)), "IL.Electrode_FR_Copper.get(1)");
		assertEquals(1, slotCount(tRow.getAsJsonArray("outputs").get(0)));
	}

	/**
	 * Spot check 2 — the ANY.Iron walk xCastIron, the first group member (:507): 2x
	 * cast-iron stick + 2x bolt + 2x small redstone dust -> 1x iron electrode.
	 */
	@Test
	public void ironWalkCastIronLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "MultiItemTechnological.java:507 — Electrode (Iron), Iron walk xCastIron");
		assertEquals("gt6:stick_cast_iron", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals("gt6:bolt_cast_iron", slotId(tRow.getAsJsonArray("inputs").get(1)));
		assertEquals("gt6:electrode_fr_iron", slotId(tRow.getAsJsonArray("outputs").get(0)), "IL.Electrode_FR_Iron.get(1)");
	}

	/**
	 * Spot check 3 — the Ender electrode x1 leg (:519): the STICK-LESS form, 5x small
	 * endstone dust + 2x small ender-eye dust -> 1x ender electrode (two inputs, the only
	 * block-1 leg without the stick/bolt frame).
	 */
	@Test
	public void enderSingleLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "MultiItemTechnological.java:519 — Electrode (Ender)");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals(2, tInputs.size(), "the stick/bolt-less Ender form");
		assertEquals("gt6:dust_small_endstone", slotId(tInputs.get(0)), "OP.dustSmall.mat(MT.Endstone, 5)");
		assertEquals(5, slotCount(tInputs.get(0)));
		assertEquals("gt6:dust_small_ender_eye", slotId(tInputs.get(1)), "OP.dustSmall.mat(MT.EnderEye, 2)");
		assertEquals(2, slotCount(tInputs.get(1)));
		assertEquals("gt6:electrode_fr_ender", slotId(tRow.getAsJsonArray("outputs").get(0)), "IL.Electrode_FR_Ender.get(1)");
	}

	/**
	 * Spot check 4 — the doubled Cu leg xAnnealedCopper (:522): 4x stick + 4x bolt + 1x
	 * full redstone dust -> 2x copper electrodes (the x2 output block).
	 */
	@Test
	public void doubledCopperLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "MultiItemTechnological.java:522 — Electrode (Copper), Cu walk xAnnealedCopper");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals("gt6:stick_annealed_copper", slotId(tInputs.get(0)));
		assertEquals(4, slotCount(tInputs.get(0)), "the doubled stick amount");
		assertEquals("gt6:bolt_annealed_copper", slotId(tInputs.get(1)));
		assertEquals(4, slotCount(tInputs.get(1)), "the doubled bolt amount");
		assertEquals("gt6:dust_redstone", slotId(tInputs.get(2)), "OP.dust.mat(MT.Redstone, 1) — the FULL dust");
		assertEquals(1, slotCount(tInputs.get(2)));
		assertEquals("gt6:electrode_fr_copper", slotId(tRow.getAsJsonArray("outputs").get(0)));
		assertEquals(2, slotCount(tRow.getAsJsonArray("outputs").get(0)), "IL.Electrode_FR_Copper.get(2)");
	}

	/**
	 * Spot check 5 — the Ender x2 gem leg (:539): 10x small endstone dust + 1x ender-eye
	 * GEM -> 2x ender electrodes (the dust-leg :538 gem-leg :539 pair shares the inputs'
	 * small-dust frame).
	 */
	@Test
	public void enderDoubledGemLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "MultiItemTechnological.java:539 — Electrode (Ender)");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals("gt6:dust_small_endstone", slotId(tInputs.get(0)));
		assertEquals(10, slotCount(tInputs.get(0)), "OP.dustSmall.mat(MT.Endstone, 10)");
		assertEquals("gt6:gem_ender_eye", slotId(tInputs.get(1)), "OP.gem.mat(MT.EnderEye, 1)");
		assertEquals(1, slotCount(tInputs.get(1)));
		assertEquals(2, slotCount(tRow.getAsJsonArray("outputs").get(0)), "IL.Electrode_FR_Ender.get(2)");
	}

	/**
	 * Spot check 6 — the Ender x4 vanilla-block leg (:543): 5x minecraft:end_stone + 2x
	 * ender-eye gem -> 4x ender electrodes (ST.make(Blocks.end_stone, 5, W), the vanilla
	 * identity face).
	 */
	@Test
	public void enderVanillaBlockLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShippedPress();
		JsonObject tRow = findRow(tRows, "MultiItemTechnological.java:543 — Electrode (Ender)");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals("minecraft:end_stone", slotId(tInputs.get(0)), "ST.make(Blocks.end_stone, 5, W)");
		assertEquals(5, slotCount(tInputs.get(0)));
		assertEquals("gt6:gem_ender_eye", slotId(tInputs.get(1)));
		assertEquals(2, slotCount(tInputs.get(1)));
		assertEquals(4, slotCount(tRow.getAsJsonArray("outputs").get(0)), "IL.Electrode_FR_Ender.get(4)");
	}

	/**
	 * The id universe: every gt6: id in the electrode rows rides the declared material-item
	 * set (the walk members' stick/bolt frames + the redstone/endstone/ender-eye consumable
	 * faces) + the thirteen output ids; the only non-gt6 id is minecraft:end_stone.
	 */
	@Test
	public void theElectrodeRowIdsRideTheDeclaredUniverse() throws Exception {
		Set<String> tUniverse = new java.util.HashSet<>();
		String[] tMembers = {"annealed_copper", "copper", "tin", "bronze", "cast_iron", "iron", "enori",
				"wrought_iron", "knightmetal", "meteoric_iron", "meteoric_steel", "steel", "pig_iron",
				"meteorite", "iron_compressed", "gold", "diamond", "blue_diamond", "green_diamond",
				"purple_diamond", "diamantine", "red_diamond", "yellow_diamond", "pink_diamond",
				"diamond_industrial", "mana_diamond", "elven_dragonstone", "gravitite", "obsidian",
				"blaze", "rubber", "emerald", "maxixe", "emeradic", "aquamarine", "morganite",
				"heliodor", "goshenite", "bixbite", "apatite", "lapis"};
		for (String tMember : tMembers) {
			tUniverse.add("gt6:stick_" + tMember);
			tUniverse.add("gt6:bolt_" + tMember);
		}
		tUniverse.add("gt6:dust_small_redstone");
		tUniverse.add("gt6:dust_redstone");
		tUniverse.add("gt6:dust_small_endstone");
		tUniverse.add("gt6:dust_endstone");
		tUniverse.add("gt6:dust_small_ender_eye");
		tUniverse.add("gt6:dust_ender_eye");
		tUniverse.add("gt6:gem_ender_eye");
		for (String tPath : new String[] {"electrode_fr_copper", "electrode_fr_tin", "electrode_fr_bronze",
				"electrode_fr_iron", "electrode_fr_gold", "electrode_fr_diamond", "electrode_fr_obsidian",
				"electrode_fr_blaze", "electrode_fr_rubber", "electrode_fr_emerald", "electrode_fr_apatite",
				"electrode_fr_lapis", "electrode_fr_ender"}) {
			tUniverse.add("gt6:" + tPath);
		}
		JsonArray tRows = pourShippedPress();
		Set<String> tSeen = new java.util.HashSet<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment") || !tRow.get("comment").getAsString().startsWith("MultiItemTechnological.java:")) continue;
			for (JsonElement tSlot : tRow.getAsJsonArray("inputs")) {
				String tId = slotId(tSlot);
				if (tId.startsWith("gt6:")) tSeen.add(tId);
				else assertEquals("minecraft:end_stone", tId, "the only vanilla face is the endstone block");
			}
			for (JsonElement tSlot : tRow.getAsJsonArray("outputs")) tSeen.add(slotId(tSlot));
		}
		Set<String> tEscapees = new java.util.HashSet<>(tSeen);
		tEscapees.removeAll(tUniverse);
		assertTrue(tEscapees.isEmpty(), "electrode-row ids outside the declared universe: " + tEscapees);
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
