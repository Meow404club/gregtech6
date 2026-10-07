package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.ANY;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2-residual-maps row-stock pour test (the six B2 residual maps: juicer,
 * roasting, lightning, cryomixer, loom, unboxinator). RECOMPUTABILITY: the walks were
 * generated from a one-shot live registration dump (the zero-transcription method) and
 * this test is the durable half — the loom walk recomputes from the LIVE ANY.Iron /
 * ANY.Steel groups and the live registration, and fails the moment the frozen snapshot
 * trails or outruns it. The juice walk pins its 37 carriers against the live GTFluids
 * spec lookups. CENSUS (file rows = seated + poured): juicer 22 (6 + 16), roasting 15
 * (5 corrected + 10), lightning 8 (1 + 7), cryomixer 62 (1 + 37 + 24), loom 35 (1 + 26
 * + 8 recipe-data-b1 statics), unboxinator 11312 (2 + 19 recipe-data-b1 halves + 11291 the recipe-b7 unbox walk).
 *
 * <p>THE recipe-data-b1 SEAM CORRECTIONS ride this class too (the seat XIII
 * re-adjudication against the 1.7.10 sources): the Chem:217/:218 salt legs and the
 * Chem:271 Adamantine leg were re-poured at the upstream full-dust OM.dust ladders
 * (K2S/Na2S U*3 = dust x3 -> U*7 = dust x7; Adamantine U*7 = dust x7 -> Ad U*3 = dust
 * x3) — they carried tiny/small mis-ladders; the loom seated :744 row corrected to the
 * upstream 16t; and the shared files grew by the recipe-data-b1 rows (the census bump).
 *
 * <p>THE ROASTING GAS-UNIT CORRECTION rides this class too: MT.X.gas(N) = N x 1000 mB / U
 * (Loader_Fluids.java:660 -> FL.java:1080 AmountPerUnit 1000 -> FL.java:1124
 * STATE_GASEOUS bind -> OreDictMaterial.java:1315-1319 units(aAmount, mGasUnit=U, native)).
 * The seated Boudouard rows were corrected from the 432-family numbers (the liquid-arm
 * U=144 calibration misapplied to the gas accessor) to the 3000-family truth, and the
 * {@link GT6RoastingRowsPourTest} pins were updated in the same commit (the declared
 * out-of-boundary edit, the squeezer-compensation precedent).
 */
public class GT6RecipeMapDataB2ResidualRowsPourTest extends GTRecipesOfflineTestBase {

	/** The per-file row census (the seated rows, the recipe-data-b1 shared-file rows, the
	 * recipe-b4 flower/fruit band (juicer +13 BlockFlowersA/B rows, loom +16 :736 dyed rows)
	 * and the smoke-juice-squeeze-vanilla-crops residue band (juicer +16 the :841-:853/:855/:856/:874 rows)). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"juicer", 51, "roasting", 15, "lightning", 8, "cryomixer", 62, "loom", 51, "unboxinator", 11312);

	/** The frozen FRUIT_JUICE walk (FL.java:187-224, the 37 members; Juice :186 is NOT one). */
	private static final String[] JUICES = {"kiwijuice", "juicelime", "juicelemon", "juiceorange", "persimmonjuice",
			"melonjuice", "currantjuice", "raspberryjuice", "blackberryjuice", "blueberryjuice", "gooseberryjuice",
			"strawberryjuice", "juiceplum", "juicepeach", "juiceelderberry", "hellderberryjuice", "juicegrapefruit",
			"juiceapricot", "juicepear", "grapejuice", "grc_grapewine0", "juiceredgrape", "juicewhitegrape",
			"juiceapple", "grc_applecider0", "juicepineapple", "juicebanana", "juicecherry", "juicecranberry",
			"cactusfruitjuice", "mangojuice", "pomegranatejuice", "starfruitjuice", "papayajuice", "figjuice",
			"coconutmilk", "datejuice"};

	/** The port waters walk (the FL.waters members). */
	private static final String[] WATERS = {"minecraft:water", "gt6:spdew", "gt6:mnwtr", "gt6:distilled_water"};

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
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

	/** Reads the shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB2ResidualRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theSixFilesPourTheirFullCensusesWithZeroSkips() throws Exception {
		for (Map.Entry<String, Integer> tFile : CENSUS.entrySet()) {
			pourShipped(tFile.getKey());
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tFile.getKey());
			assertNotNull(tMap, tFile.getKey() + " resolves");
			assertEquals(tFile.getValue().intValue(), GT6RecipeMapJsonLoader.pouredCount(tFile.getKey()),
					tFile.getKey() + ": the tracker mirrors the file census (a smaller number = WARN-skipped rows)");
			assertEquals(tFile.getValue().intValue(), tMap.mRecipeList.size(),
					tFile.getKey() + ": the map holds the census");
		}
	}

	/** The map-key seams: each file keys its own RM (no cutter/sawing-style shared-map split on this card). */
	@Test
	public void theSixKeysResolveToTheirOwnMaps() {
		assertSame(GT6RecipeMaps.JUICER, GT6RecipeMapJsonLoader.mapFor("juicer"));
		assertSame(GT6RecipeMaps.ROASTING, GT6RecipeMapJsonLoader.mapFor("roasting"));
		assertSame(GT6RecipeMaps.LIGHTNING, GT6RecipeMapJsonLoader.mapFor("lightning"));
		assertSame(GT6RecipeMaps.CRYO_MIXER, GT6RecipeMapJsonLoader.mapFor("cryomixer"));
		assertSame(GT6RecipeMaps.LOOM, GT6RecipeMapJsonLoader.mapFor("loom"));
		assertSame(GT6RecipeMaps.UNBOXINATOR, GT6RecipeMapJsonLoader.mapFor("unboxinator"));
	}

	private static void assertSame(Object aExpected, Object aActual) {
		org.junit.jupiter.api.Assertions.assertSame(aExpected, aActual);
	}

	/** The file-head declaration pins: the census anchors, the blocked faces, the negatives, the mappings. */
	@Test
	public void theFileHeadsKeepTheirDeclarations() throws Exception {
		String tJuicer = headOf("juicer");
		assertTrue(tJuicer.contains("Loader_Recipes_Vanilla.java:841-874"), "the vanilla run anchor");
		assertTrue(tJuicer.contains("Loader_Recipes_Crops.java"), "the crops anchor");
		assertTrue(tJuicer.contains("Loader_Recipes_Food.java"), "the food anchor");
		assertTrue(tJuicer.contains("Loader_Recipes_Other.java:185"), "the other anchor");
		assertTrue(tJuicer.contains("gemChipped/gemFlawed Ice"), "the unregistered gem forms declared");
		assertTrue(tJuicer.contains("ALREADY SEATED"), "the seated rows declared");
		assertTrue(tJuicer.toLowerCase(java.util.Locale.ROOT).contains("blocked"), "the blocked class declared");

		String tRoasting = headOf("roasting");
		assertTrue(tRoasting.contains("53 stations"), "the roasting census anchor");
		assertTrue(tRoasting.contains("DECLARED CORRECTION"), "the gas-unit correction is declared");
		assertTrue(tRoasting.contains("1000 mB / U"), "the gas-unit face is stated");
		assertTrue(tRoasting.contains("netherair/enderair"), "the AIR walk members are stated");
		assertTrue(tRoasting.contains("SO2"), "the SO2 pooled blockage is declared");

		String tLightning = headOf("lightning");
		assertTrue(tLightning.contains("Other:930"), "the NetherStar anchor");
		assertTrue(tLightning.contains("Ores:339"), "the Dolamide anchor");
		assertTrue(tLightning.contains("RULING-EXTERNAL"), "the absorption ruling declared");
		assertTrue(tLightning.contains("Chem:217"), "the salt legs anchor");
		assertTrue(tLightning.contains("Chem:271"), "the Adamantine anchor");
		assertTrue(tLightning.contains("Other:638"), "the Certus anchor");
		assertTrue(tLightning.contains("H2O2"), "the H2O2 blocker declared");
		assertTrue(tLightning.contains("nitricoxide"), "the NO blocker declared");
		assertTrue(tLightning.contains("FL.Ender_TE"), "the TE-gated true negative declared");
		assertTrue(tLightning.toLowerCase(java.util.Locale.ROOT).contains("selector"), "the ST.tag drop declared");

		String tCryo = headOf("cryomixer");
		assertTrue(tCryo.contains("Loader_Recipes_Food.java:829"), "the juice walk anchor");
		assertTrue(tCryo.contains("exactly 37"), "the FRUIT_JUICE member census");
		assertTrue(tCryo.contains("Other:233-238"), "the Cryotheum absorption anchor");
		assertTrue(tCryo.contains("water/spdew/mnwtr/distilled_water"), "the waters walk members");
		assertTrue(tCryo.contains("MultiItemFood:848-849"), "the seated java band declared");

		String tLoom = headOf("loom");
		assertTrue(tLoom.contains("Loader_Recipes_Vanilla.java:751"), "the horse-armor anchor");
		assertTrue(tLoom.contains(":755"), "the saddle anchor");
		assertTrue(tLoom.contains(":763-:766"), "the chain anchor");
		assertTrue(tLoom.contains("plateGem arm"), "the Enori special declared");
		assertTrue(tLoom.contains("Dye_Materials"), "the dye block declared");
		assertTrue(tLoom.contains("PLANTS"), "the plants block declared");
		assertTrue(tLoom.toLowerCase(java.util.Locale.ROOT).contains("selector"), "the ST.tag drop declared");

		String tUnbox = headOf("unboxinator");
		assertTrue(tUnbox.contains("Loader_Recipes_Vanilla.java:965"), "the bookshelf anchor");
		assertTrue(tUnbox.contains("minecraft:oak_planks"), "the original identity anchor on record");
		assertTrue(tUnbox.contains("-> gt6:plank_wood") && tUnbox.contains("plank-mapping-sweep"),
				"the sweep re-pour target and card are declared");
		assertTrue(tUnbox.contains("Loader_Woods.java:74"), "the IL.Plank provenance");
		assertTrue(tUnbox.contains("addFakeRecipe"), "the loot faces declared not poured");
	}

	private static String headOf(String aKey) throws Exception {
		InputStream tStream = GT6RecipeMapDataB2ResidualRowsPourTest.class
				.getResourceAsStream("/data/gt6/recipe_maps/" + aKey + ".json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue(tDoc.has("comment"), aKey + ": the file head carries a comment member");
		return tDoc.get("comment").getAsString();
	}

	/** The loom walk recompute — the heart: the LIVE ANY.Iron/ANY.Steel groups over the live registration. */
	@Test
	public void theLoomWalkMatchesTheLiveRecompute() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tReg = registrationUnion();
		JsonArray tRows = pourShipped("loom");
		Set<String> tShipped = new HashSet<>();
		int tHorse = 0, tSaddle = 0, tChain = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue; // the seated smoke row
			String tComment = tRow.get("comment").getAsString();
			String tOut = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
			boolean tWalk;
			if (tComment.contains(":751")) { tHorse++; tWalk = true; }
			else if (tComment.contains(":755")) { tSaddle++; tWalk = true; }
			else if (tOut.startsWith("minecraft:chainmail_")) { tChain++; tWalk = true; }
			else tWalk = false; // the recipe-data-b1 statics (:745/:746/:752/:753/:757-:760) sit outside the walk parity
			if (tWalk) tShipped.add(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString()
					+ ">" + tOut);
		}
		// the live walk: :751 over ANY.Iron (plate or the Enori plateGem arm), :755/:763-766 over ANY.Steel
		Set<String> tExpected = new HashSet<>();
		int tLiveHorse = 0, tLiveSaddle = 0, tLiveChain = 0;
		for (OreDictMaterial tMat : ANY.Iron.mToThis) {
			boolean tPlate = tReg.contains("plate|" + tMat.mNameInternal);
			boolean tPlateGem = tReg.contains("plateGem|" + tMat.mNameInternal);
			if (!tPlate && !tPlateGem) continue;
			tLiveHorse++;
			tExpected.add("minecraft:leather>minecraft:iron_horse_armor");
		}
		for (OreDictMaterial tMat : ANY.Steel.mToThis) {
			if (!tReg.contains("ring|" + tMat.mNameInternal) || !tReg.contains("stick|" + tMat.mNameInternal)) continue;
			tLiveSaddle++;
			tExpected.add("minecraft:leather>minecraft:saddle");
			for (String tPiece : new String[] {"chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots"}) {
				tLiveChain++;
				tExpected.add("gt6:" + GTMaterialItems.itemIdOf(OP.ring, tMat) + ">minecraft:" + tPiece);
			}
		}
		assertEquals(11, tLiveHorse, "the live ANY.Iron plate walk");
		assertEquals(3, tLiveSaddle, "the live ANY.Steel saddle walk");
		assertEquals(12, tLiveChain, "the live ANY.Steel chain walk");
		assertEquals(tLiveHorse, tHorse, "the shipped horse-armor rows match the live walk");
		assertEquals(tLiveSaddle, tSaddle, "the shipped saddle rows match the live walk");
		assertEquals(tLiveChain, tChain, "the shipped chain rows match the live walk");
		Set<String> tShippedOnly = new HashSet<>(tShipped);
		tShippedOnly.removeAll(tExpected);
		assertTrue(tShippedOnly.isEmpty(), "no walk row outside the live walks: " + tShippedOnly);
	}

	/** The verbatim spot checks — the juicer ice/poison/sunflower faces and the OM.dust ladder byproducts. */
	@Test
	public void theJuicerRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("juicer");
		assertRow(findRow(tRows, "minecraft:sunflower"), "gt6:sunfloweroil:75>minecraft:yellow_dye:2", 16, 16); // review-seat: dye:11 = DYE_INDEX_Yellow, the :816 squeezer twin mapping
		assertRow(findRow(tRows, "minecraft:packed_ice"), "gt6:ice:2000", 128, 16);
		assertRow(findRow(tRows, "minecraft:snowball"), "gt6:ice:250", 64, 16);
		assertRow(findRow(tRows, "gt6:dust_tiny_ice"), "gt6:ice:111", 64, 16);
		assertRow(findRow(tRows, "gt6:gem_ice"), "gt6:ice:1000", 64, 16);
		// :872 spider eye -> OM.dust(MeatRaw, U2): the ladder lands on dustSmall x2 (OM.java:460-468)
		assertRow(findRow(tRows, "minecraft:spider_eye"), "gt6:potion.poison:125>gt6:dust_small_meat_raw:2", 16, 16);
		// :873 pufferfish -> Potion_Poison_2 = potion.poison.strong + OM.dust(FishRaw, U) = dust x1
		assertRow(findRow(tRows, "minecraft:pufferfish"), "gt6:potion.poison.strong:125>gt6:dust_fish_raw:1", 32, 16);
		// the :860/:861 chipped/flawed gem forms have NO rows (the unregistered gem walk, declared)
		for (String tForm : new String[] {"gt6:gem_chipped_ice", "gt6:gem_flawed_ice"}) {
			for (JsonElement tElement : tRows) {
				for (JsonElement tSlot : tElement.getAsJsonObject().getAsJsonArray("inputs")) {
					assertTrue(!tForm.equals(tSlot.getAsJsonObject().get("item").getAsString()),
							tForm + " stays absent (declared)");
				}
			}
		}
	}

	/** The roasting verbatim face: the corrected Boudouard pair, one oxide ladder row, one combustion row. */
	@Test
	public void theRoastingRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("roasting");
		// :398 carbon Boudouard — the corrected gas face (3000 -> 4000, the 1000 mB/U conversion)
		JsonObject tCarbon = findRowByFluidOut(tRows, "gt6:dust_carbon", "gt6:carbonmonoxide");
		assertEquals(3000, tCarbon.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				":398 3U CO2 at the gas-native face");
		assertEquals(4000, tCarbon.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(), ":398 4U CO");
		// :435 combustion — the literal O2 input, the 3U CO2 output
		JsonObject tCombustion = findRowByFluidOut(tRows, "gt6:dust_carbon", "gt6:carbondioxide");
		assertEquals(2000, tCombustion.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				":435 the literal mB O2 form");
		assertEquals(3000, tCombustion.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(), ":435 3U CO2");
		// :424 oxide — the OM.dust ladder (V2O5 7*U2 -> dustSmall x14)
		JsonObject tVanadium = findRow(tRows, "gt6:dust_vanadium");
		assertEquals(2500, tVanadium.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(), ":424 O2 2500");
		assertEquals("gt6:dust_small_vanadium_pentoxide", tVanadium.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(14, tVanadium.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt(), "the ladder count");
		// the oxide census on the JSON rows (five, dur 128)
		int tOxides = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("outputs") && tRow.has("fluidInputs") && !tRow.has("fluidOutputs") && tRow.get("duration").getAsInt() == 128) tOxides++;
		}
		assertEquals(5, tOxides, "the five oxide rows");
	}

	/** The lightning verbatim face: the NetherStar legs, the Dolamide chances, the salt/Adamantine/Certus absorptions. */
	@Test
	public void theLightningRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("lightning");
		// Other:930 — the blockSolid Netherite input is live in the port universe
		JsonObject tNether = findRow(tRows, "gt6:block_solid_netherite");
		JsonArray tFluids = tNether.getAsJsonArray("fluidInputs");
		assertEquals("gt6:enderpearl_molten", tFluids.get(0).getAsJsonObject().get("fluid").getAsString(), "FL.Ender carrier");
		assertEquals(576, tFluids.get(0).getAsJsonObject().get("amount").getAsInt(), "L*4 = 4 x CS.L(144)");
		assertEquals(5000, tFluids.get(1).getAsJsonObject().get("amount").getAsInt(), "the soulsandoil leg");
		assertRow(tNether, "gt6:gem_nether_star:1", 512, 512);
		// Ores:339 — the chance-8000 leg
		JsonObject tDolamide = findRow(tRows, "gt6:crushed_purified_dolamide");
		assertEquals(8000, tDolamide.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsInt(),
				"the new long[] {8000} chance");
		assertEquals(18, tDolamide.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt(), ":339 x18");
		// Chem:217 — the salt leg (the OM.dust ladders verbatim: K2S U*3 = dust x3 -> K2SO4 U*7 = dust x7;
		// the tiny/small mis-ladder corrected by task recipe-data-b1 against Loader_Recipes_Chem.java:217)
		JsonObject tSalt = findRow(tRows, "gt6:dust_potassium_sulfide");
		assertEquals(3, tSalt.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt(), "OM.dust(K2S, U*3) = dust x3");
		assertEquals(4000, tSalt.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(), "the O2 leg");
		assertEquals("gt6:dust_potassium_sulfate", tSalt.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(7, tSalt.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		// Chem:271 — the Adamantine leg: OM.dust(U*7) = dust x7 -> O gas OUT 4000 + Ad OM.dust(U*3) = dust x3 (corrected)
		JsonObject tAdamant = findRow(tRows, "gt6:dust_adamantine");
		assertEquals(7, tAdamant.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt(), "OM.dust(Adamantine, U*7) = dust x7");
		assertEquals("gt6:oxygen", tAdamant.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
		assertEquals(4000, tAdamant.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"MT.O.gas(U*4) = 4000 mB (the Reikygen-native 1000 mB/U face)");
		assertEquals("gt6:dust_adamantium", tAdamant.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(3, tAdamant.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		// Other:638 — the Certus charge
		assertRow(findRow(tRows, "gt6:gem_certus_quartz"), "gt6:gem_charged_certus_quartz:1", 2048, 16);
	}

	/** The cryomixer verbatim face: the juice walk row shape, the 37-member set, the Cryotheum forms. */
	@Test
	public void theCryomixerRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("cryomixer");
		Set<String> tJuiceSet = new HashSet<>();
		int tCryoSmall = 0, tCryoBig = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("comment")) continue;
			String tComment = tRow.get("comment").getAsString();
			if (tComment.contains("FRUIT_JUICE")) {
				tJuiceSet.add(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
				assertEquals("gt6:sweettea", tRow.getAsJsonArray("fluidInputs").get(1).getAsJsonObject().get("fluid").getAsString(),
						"the sweet tea leg");
				assertEquals(125, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt());
				assertEquals("gt6:icetea", tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
				assertEquals(250, tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
			} else if (tComment.contains("small form")) {
				tCryoSmall++;
				assertEquals(250, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
						"FL.mul(tWater, 1, 4, T) = 250");
				assertEquals("gt6:dust_cryotheum", tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
				assertEquals(2, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
				assertEquals(32, tRow.get("duration").getAsInt());
			} else if (tComment.contains("big form")) {
				tCryoBig++;
				assertEquals(1000, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt());
				assertEquals(8, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
				assertEquals(128, tRow.get("duration").getAsInt());
			}
		}
		assertEquals(37, tJuiceSet.size(), "the FRUIT_JUICE member census");
		Set<String> tExpected = new HashSet<>();
		for (String tJuice : JUICES) tExpected.add("gt6:" + tJuice);
		assertEquals(tExpected, tJuiceSet, "the juice carriers, set-equal both directions");
		assertEquals(12, tCryoSmall, "the three small-form stations x the four waters members");
		assertEquals(12, tCryoBig, "the three big-form stations x the four waters members");
		// every juice id resolves against the LIVE fluid universe
		for (String tJuice : JUICES) {
			assertTrue(fluidRegistered(tJuice), "the live carrier for gt6:" + tJuice);
		}
	}

	/** The loom verbatim face: the horse-armor/saddle/chain row shapes incl. the Enori plateGem arm. */
	@Test
	public void theLoomRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("loom");
		JsonObject tCastIron = findRowByAnyInput(tRows, "gt6:plate_cast_iron");
		assertEquals("minecraft:iron_horse_armor", tCastIron.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(64, tCastIron.get("eut").getAsInt(), ":751 eut 64");
		assertEquals(128, tCastIron.get("duration").getAsInt(), ":751 dur 128");
		assertEquals(8, tCastIron.getAsJsonArray("inputs").get(1).getAsJsonObject().get("count").getAsInt(), "plate x8");
		JsonObject tEnori = findRowByAnyInput(tRows, "gt6:plate_gem_enori");
		assertEquals("minecraft:iron_horse_armor", tEnori.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString(),
				"the :751 ternary plateGem arm on Enori");
		JsonObject tSaddle = findRowByAnyInput(tRows, "gt6:ring_knightmetal");
		assertEquals("minecraft:saddle", tSaddle.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(3, tSaddle.getAsJsonArray("inputs").get(2).getAsJsonObject().get("count").getAsInt(), "stick x3");
		JsonObject tChain = findRow(tRows, "gt6:ring_meteoric_steel", "minecraft:chainmail_chestplate");
		assertEquals(8, tChain.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt(), ":764 ring x8");
		assertEquals(96, tChain.get("eut").getAsInt(), ":763-766 eut 96");
	}

	/** The unboxinator verbatim face: the bookshelf row, its IL.Plank leg re-poured onto gt6:plank_wood (task plank-mapping-sweep). */
	@Test
	public void theUnboxinatorRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("unboxinator");
		JsonObject tBookshelf = findRow(tRows, "minecraft:bookshelf");
		assertEquals(16, tBookshelf.get("duration").getAsInt());
		assertEquals(16, tBookshelf.get("eut").getAsInt());
		assertEquals("minecraft:book", tBookshelf.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(3, tBookshelf.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals("gt6:plank_wood", tBookshelf.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(),
				"the absorbed IL.Plank identity anchor (the wood-planks-register carrier)");
		assertEquals(3, tBookshelf.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsInt());
	}

	/**
	 * The id faces: every gt6 ITEM id across the six files lives in GTMaterialItems ∪
	 * GTMaterialBlocks (the pipe families GT/GTItemPipes contribute no member ids on this
	 * card — declared); every minecraft: id sits in the vanilla whitelist.
	 */
	@Test
	public void everyItemIdTheFilesReferenceIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		// task planks-blockification — the plank prefix items retired; the plank face is the
		// GT6WoodDict BlockItem rows (the ONE plank authority, the GT6TreeBlocks cube family),
		// so the 17 gt6 family ids join the universe (the unboxinator plank rows pour onto it)
		for (gregtech6.registry.GT6WoodDict.PlankEntry tPlank : gregtech6.registry.GT6WoodDict.GT6_ROWS) {
			tUniverse.add("gt6:" + tPlank.id());
		}
		// the flower band items are plain BlockItems, not material-prefix ids (the b4 BlockFlowers rows)
		for (var tFlower : gregtech6.registry.GT6SurfaceBlocks.FLOWER_ITEMS) {
			tUniverse.add("gt6:" + tFlower.getId().getPath());
		}
		tUniverse.addAll(vanillaWhitelist());
		// the bee-comb flat item (not a material-prefix id; the b2b1 COMB_SPECS universe precedent)
		tUniverse.add("gt6:comb_honey");
		// the vanilla-alias foodside family (the smoke-card remains legs ride gt6:remains_plant/remains_fruit)
		for (gregtech6.registry.GT6FoodsideItems.SideRow tRow : gregtech6.registry.GT6FoodsideItems.ROWS) {
			tUniverse.add("gt6:" + tRow.id());
		}
		Set<String> tMissing = new HashSet<>();
		for (String tFile : CENSUS.keySet()) {
			for (JsonElement tElement : pourShipped(tFile)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (!tUniverse.contains(tId)) tMissing.add(tFile + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered item ids (these rows would WARN-skip live): " + tMissing);
	}

	/** The fluid faces: every gt6 FLUID id across the six files resolves in the GTFluids spec lookups. */
	@Test
	public void everyFluidIdTheFilesReferenceIsRegistered() throws Exception {
		Set<String> tMissing = new HashSet<>();
		for (String tFile : CENSUS.keySet()) {
			for (JsonElement tElement : pourShipped(tFile)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"fluidInputs", "fluidOutputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
						if (tId.startsWith("minecraft:")) continue; // the vanilla carriers resolve live
						if (!fluidRegistered(tId.substring("gt6:".length()))) tMissing.add(tFile + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------------ helpers

	private static Set<String> registrationUnion() {
		Set<String> rReg = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			rReg.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			rReg.add(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal);
		}
		return rReg;
	}

	/** The offline fluid-universe lookup: the union of the GTFluids spec-table lookups (the sawing shape, widened
	 * with the two dye compose families the b4 loom/juicer bands reference). */
	private static boolean fluidRegistered(String aPath) {
		if (aPath.startsWith("dye_watermixed_") || aPath.startsWith("dye_flower_")) return true; // the GTFluids DyeFluid compose walk
		return gregtech6.fluid.GTFluids.aquaSpec(aPath) != null || gregtech6.fluid.GTFluids.engineSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.chemicalSpec(aPath) != null || gregtech6.fluid.GTFluids.simpleLiquidSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.lubricantSpec(aPath) != null || gregtech6.fluid.GTFluids.foodSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodB1Spec(aPath) != null || gregtech6.fluid.GTFluids.foodB2Spec(aPath) != null
				|| gregtech6.fluid.GTFluids.foodTailSpec(aPath) != null || gregtech6.fluid.GTFluids.hotSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.closureSpec(aPath) != null || gregtech6.fluid.GTFluids.honeySpec(aPath) != null
				|| gregtech6.fluid.GTFluids.beeRowSpec(aPath) != null || gregtech6.fluid.GTFluids.quSpec(aPath) != null
				|| gregtech6.fluid.GTFluids.namingSpec(aPath) != null;
	}

	private static JsonObject findRow(JsonArray aRows, String aFirstItem) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aFirstItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())) return tRow;
		}
		throw new AssertionError("no row keyed on " + aFirstItem);
	}

	private static JsonObject findRow(JsonArray aRows, String aFirstItem, String aOutput) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!rowHasInput(tRow, aFirstItem) || !tRow.has("outputs")) continue;
			for (JsonElement tOut : tRow.getAsJsonArray("outputs")) {
				if (aOutput.equals(tOut.getAsJsonObject().get("item").getAsString())) return tRow;
			}
		}
		throw new AssertionError("no row " + aFirstItem + " -> " + aOutput);
	}

	/** Row lookup by ANY input slot (the loom rows key on the plate/ring leg, not the leather head). */
	private static JsonObject findRowByAnyInput(JsonArray aRows, String aItem) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (rowHasInput(tRow, aItem)) return tRow;
		}
		throw new AssertionError("no row with input " + aItem);
	}

	private static boolean rowHasInput(JsonObject aRow, String aItem) {
		for (JsonElement tSlot : aRow.getAsJsonArray("inputs")) {
			if (aItem.equals(tSlot.getAsJsonObject().get("item").getAsString())) return true;
		}
		return false;
	}

	/** Row lookup by input item + a fluid OUTPUT face (the Boudouard/combustion split). */
	private static JsonObject findRowByFluidOut(JsonArray aRows, String aItem, String aFluid) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!rowHasInput(tRow, aItem) || !tRow.has("fluidOutputs")) continue;
			for (JsonElement tOut : tRow.getAsJsonArray("fluidOutputs")) {
				if (aFluid.equals(tOut.getAsJsonObject().get("fluid").getAsString())) return tRow;
			}
		}
		throw new AssertionError("no row " + aItem + " -> fluid " + aFluid);
	}

	/** Full-row pin: the output slot tail (">id:count" style segments), duration, eut. */
	private static void assertRow(JsonObject aRow, String aTail, long aDuration, long aEut) {
		StringBuilder tTail = new StringBuilder();
		if (aRow.has("fluidOutputs")) {
			for (JsonElement tOut : aRow.getAsJsonArray("fluidOutputs")) {
				JsonObject tSlot = tOut.getAsJsonObject();
				tTail.append(tSlot.get("fluid").getAsString()).append(':').append(tSlot.get("amount").getAsInt()).append('>');
			}
		}
		if (aRow.has("outputs")) {
			for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
				JsonObject tSlot = tOut.getAsJsonObject();
				tTail.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt()).append('>');
			}
		}
		assertEquals(aTail + ">", tTail.toString(), "the output slot tail");
		assertEquals(aDuration, aRow.get("duration").getAsLong(), "the duration");
		assertEquals(aEut, aRow.get("eut").getAsLong(), "the eut");
	}

	/** The vanilla whitelist — exactly the minecraft: ids the six files reference. */
	private static Set<String> vanillaWhitelist() {
		Set<String> rSet = new HashSet<>();
		for (String tId : new String[] {
				// juicer
				"sunflower", "yellow_dye", "ice", "packed_ice", "snowball", "snow",
				"red_mushroom", "poisonous_potato", "spider_eye", "pufferfish",
				"slime_ball", "wheat_seeds", "melon_seeds", "beetroot_seeds", "pumpkin_seeds",
					// the smoke-card residue band: the :841-:856/:874 vanilla-crops inputs
					"poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
					"white_tulip", "pink_tulip", "oxeye_daisy", "dandelion", "lilac", "peony",
					"rose_bush", "cactus", "sugar_cane", "ink_sac",
					// the b4 band: the Tungstus IL.Dye_Cactus face + the :736 dyed-wool outputs
					"green_dye", "white_wool", "orange_wool", "magenta_wool", "light_blue_wool", "yellow_wool",
					"lime_wool", "pink_wool", "gray_wool", "light_gray_wool", "cyan_wool", "purple_wool",
					"blue_wool", "brown_wool", "green_wool", "red_wool", "black_wool",
					// loom + unboxinator + lightning smoke
					"leather", "iron_horse_armor", "saddle",
					"chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots",
					// "oak_planks" left at plank-mapping-sweep: the bookshelf unbox's IL.Plank leg
					// re-poured onto gt6:plank_wood, which rides the registration union above
					"string", "bookshelf", "book",
				"quartz", "glowstone_dust", "water", "prismarine_crystals",
				// cryomixer smoke
				"clay_ball", "snow_block", "map", "paper", "compass",
				// recipe-data-b1 statics (the loom deferred statics + the unbox halves;
				// all live vanilla ids — the whitelist guards against transcription typos)
				"cobweb", "sugar_cane", "golden_horse_armor", "diamond_horse_armor",
				"leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
				"chest_minecart", "furnace_minecart", "hopper_minecart", "tnt_minecart", "minecart",
				"chest", "furnace", "hopper", "tnt", "stick",
				"iron_sword", "golden_sword", "diamond_sword",
				"iron_pickaxe", "golden_pickaxe", "diamond_pickaxe",
				"iron_shovel", "golden_shovel", "diamond_shovel",
				"iron_axe", "golden_axe", "diamond_axe",
				"iron_hoe", "golden_hoe", "diamond_hoe"}) {
			rSet.add("minecraft:" + tId);
		}
		return rSet;
	}
}
