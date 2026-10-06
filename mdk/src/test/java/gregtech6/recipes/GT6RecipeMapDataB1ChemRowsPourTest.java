package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * The recipe-b1-chem-domain row-stock pour test (the GT6RecipeMapDataB1RowsPourTest
 * fixture posture): the seven Chem-domain files — mixerchem (the MIXER Chem face),
 * roastingchem (the ROASTING SO2 face), melterchem/smelterchem (the Chem ice/snow
 * faces), cryodistillationtowerchem (the dimension-air face), injectorchem (the
 * thorium-salt face) and the rewritten autoclave (the Bayer face, the kelp forgery
 * DELETED) — pour through the real {@link GT6RecipeMapJsonLoader} seam with zero
 * skips, the verbatim spot checks read the shipped row objects directly (the
 * transcription artifact IS the assertion surface), and the live-id face proves every
 * gt6 item id exists in the registration walks' universe.
 *
 * <p><b>The accounting</b> (Loader_Recipes_Chem.java static rows, the D-段 gap ledger of
 * research.r11-recipe-coverage): MIXER Chem face = 22 FL.waters templates x4 (:37-64) +
 * the ANY.SiO2 walk x22 (:100) + 79 fixed rows (:103-109/:110-120/:130-139/:142-149/
 * :154-155/:157-158/:160-164) + the ANY.Fe walk x5 of 8 (:169-171) + the UF6 chain
 * (:175-178) + the OXYGEN walk (:223-226) + the AIR walk x2 (:229-232) + :267/:268 +
 * :385-386 = 189. ROASTING SO2 face = 14 sulfide + 5 S/Blaze over OXYGEN x1 (:407-441)
 * and AIR x2 (:445-465) = 57. MELTER/SMELTER = 10 of the 13-row ice/snow faces each.
 * CRYO DT = 3 of 3 (:363-365; the plain-air row :363 rides the registered gt6:air,
 * task cryo-distillery-air-rewire). INJECTOR = 5
 * (:389-394). AUTOCLAVE = 12 (:274-285).
 *
 * <p><b>Declared skips</b> (upstream rows with unregistered port carriers — the file
 * headers carry the same declaration): mixer :140-141/:156 H2S, :150 hematite molten,
 * :151/:266 Glycerol/Glyceryl/NitroFuel, :153 Reikygen, :179-181 the UF4/Ca/U molten
 * legs, :380-381 biodiesel, the ANY.Fe members WroughtIron/CastIron/IronCompressed
 * (no port dust item); melter/smelter gemChipped/gemFlawed legs (:483-484/:498-499).
 * The HEATMIXER face (:101-102/:121-128/:159/:166/:174) is the RM.java:75 PURE ALIAS —
 * upstream {@code HeatMixer = Mixer}, so the ten stations ride the MIXER map through
 * this very mixerchem key (the task recipe-b6b alias flip; the old "pools until a
 * map-registration card" judgment is void — there is no HeatMixer map and never was
 * one; the stations' own data pour is a data card's content and the mixerchem.json
 * header pool-note retires with it). The DISTILLATION_TOWER Chem rows (:350-360)
 * pour nothing new: the biomass pair needs the Glycerol carrier and the
 * Heavy2/HotCrude/Light2 oils are unregistered (six oil faces shipped earlier).
 */
public class GT6RecipeMapDataB1ChemRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-key census of this card: file key -> expected poured rows (zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"mixerchem", 189,                // the MIXER Chem face (see the class doc accounting)
			"roastingchem", 57,              // the SO2 face: 19 OXYGEN-leg + 19x2 AIR-leg
			"melterchem", 10,                // Chem:480-492 minus the seated ice row and the 2 gem skips
			"smelterchem", 10,               // Chem:495-507, the melter face mirrored
			"cryodistillationtowerchem", 3,  // :363-365 (the plain-air row rides the registered gt6:air)
			"injectorchem", 5,               // :389-394
			"autoclave", 12);                // :274-285 — the kelp forgery REPLACED by the real Bayer face

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

	/** Reads one shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB1ChemRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyChemFaceFilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The mixer chem face rides its OWN key beside the Food face — both subsets coexist on the MIXER map (the sawing 双文件一 map form). */
	@Test
	public void theMixerChemKeyJoinsTheFoodFaceOnTheMixerMap() throws Exception {
		pourShipped("mixerchem");
		assertEquals(189, GT6RecipeMapJsonLoader.pouredCount("mixerchem"), "the chem subset tracker");
		assertEquals(GT6RecipeMaps.MIXER, GT6RecipeMapJsonLoader.mapFor("mixerchem"), "the MIXER map instance");
	}

	/** Verbatim 1 — the FL.waters walk base row (Chem:38, the Saltwater half-unit leg): every one of the four port waters pours it. */
	@Test
	public void theSaltWaterWalkPoursAllFourPortWaters() throws Exception {
		JsonArray tRows = pourShipped("mixerchem");
		int tSeen = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.get("comment").getAsString().endsWith("Chem:38")) continue;
			tSeen++;
			assertEquals(16, tRow.get("duration").getAsLong());
			assertEquals(16, tRow.get("eut").getAsLong());
			assertEquals("gt6:dust_small_salt", tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString(), "OM.dust(NaCl, U4)");
			assertEquals(750, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(), "FL.mul(tWater, 3, 4, T)");
			assertEquals(1000, tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt(), "MT.SaltWater.liquid(U*1)");
		}
		assertEquals(4, tSeen, "the FL.waters walk = water + mnwtr + distilled_water + spdew");
	}

	/** Verbatim 2 — the Ca + HCl row (Chem:130, the Q3 error-mode anchor): H2 2U gas + CaCl2 3U dust at 80 t / 16 EUt. */
	@Test
	public void theCalciumHydrochloricRowIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("mixerchem"), "RM.Mixer Chem:130");
		assertEquals(80, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		assertEquals("gt6:dust_calcium", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals("gt6:hydrochloricacid", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(4000, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "MT.HCl.fluid(U*4)");
		assertEquals("gt6:hydrogen", slotId(tRow.getAsJsonArray("fluidOutputs").get(0)));
		assertEquals(2000, slotAmount(tRow.getAsJsonArray("fluidOutputs").get(0)), "MT.H.gas(U*2)");
		assertEquals("gt6:dust_calcium_chloride", slotId(tRow.getAsJsonArray("outputs").get(0)));
		assertEquals(3, slotCount(tRow.getAsJsonArray("outputs").get(0)), "OM.dust(CaCl2, U*3)");
	}

	/** Verbatim 3 — the UF6 chain tail (Chem:175): UF4 5U + F 2U -> UF6 7U gas at 112 t. */
	@Test
	public void theUf6ChainRowIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("mixerchem"), "RM.Mixer Chem:175");
		assertEquals(112, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		assertEquals("gt6:dust_uranium_tetrafluoride", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals(5, slotCount(tRow.getAsJsonArray("inputs").get(0)), "OM.dust(UF4, U*5)");
		assertEquals("gt6:fluorine", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(2000, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "MT.F.gas(U*2)");
		assertEquals("gt6:uraniumhexafluoride", slotId(tRow.getAsJsonArray("fluidOutputs").get(0)));
		assertEquals(7000, slotAmount(tRow.getAsJsonArray("fluidOutputs").get(0)), "MT.UF6.gas(U*7)");
	}

	/** Verbatim 4 — the Fe walk close (Chem:171): dust iron + 8 ferric -> 9 ferrous chloride at 144 t. */
	@Test
	public void theIronWalkCloseRowIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("mixerchem"), "RM.Mixer ANY.Fe walk x5 Chem:171 [iron]");
		assertEquals(144, tRow.get("duration").getAsLong());
		assertEquals("gt6:dust_iron", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals("gt6:dust_ferric_chloride", slotId(tRow.getAsJsonArray("inputs").get(1)));
		assertEquals(8, slotCount(tRow.getAsJsonArray("inputs").get(1)), "OM.dust(FeCl3, U*8)");
		assertEquals("gt6:dust_ferrous_chloride", slotId(tRow.getAsJsonArray("outputs").get(0)));
		assertEquals(9, slotCount(tRow.getAsJsonArray("outputs").get(0)), "OM.dust(FeCl2, U*9)");
	}

	/** Verbatim 5 — the Pyrite oxygen-leg roast (Chem:408): O2 1834, SO2 6*U3, the 5*U6 hematite OM ladder = 60 div72 dust. */
	@Test
	public void thePyriteOxygenRoastIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("roastingchem"), "RM.Roasting Chem:407-441 OXYGEN walk x1 [dust_pyrite]");
		assertEquals(512, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
		assertEquals("gt6:dust_pyrite", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals("gt6:oxygen", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(1834, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "FL.make(tOxygen, 1834)");
		assertEquals("gt6:sulfurdioxide", slotId(tRow.getAsJsonArray("fluidOutputs").get(0)));
		assertEquals(2000, slotAmount(tRow.getAsJsonArray("fluidOutputs").get(0)), "MT.SO2.gas(6*U3)");
		assertEquals("gt6:dust_div72_hematite", slotId(tRow.getAsJsonArray("outputs").get(0)), "OM.dust(Fe2O3, 5*U6)");
		assertEquals(60, slotCount(tRow.getAsJsonArray("outputs").get(0)), "the OM.java:460-468 ladder at 5/6 U");
		assertFalse(tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().has("chance"), "the OXYGEN legs are deterministic");
	}

	/** Verbatim 6 — the Pyrite AIR-leg twin carries the tChances {8000} on its item outputs (Chem:446). */
	@Test
	public void thePyriteAirRoastCarriesTheUpstreamChances() throws Exception {
		JsonObject tRow = findRow(pourShipped("roastingchem"), "RM.Roasting Chem:445-459 AIR walk x2 chances 8000 [dust_pyrite] [gt6:netherair]");
		assertEquals(512, tRow.get("duration").getAsLong());
		assertEquals(8000, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "FL.make(tAir, 8000)");
		assertEquals(8000, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsLong(), "the tChances literal");
	}

	/** Verbatim 7 — the Bayer KOH small-batch anchor (Chem:274): steam 48000 + DistW 1050 and the {10000,5000,5000} chances. */
	@Test
	public void theBayerKohSmallBatchRowIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("autoclave"), "RM.Autoclave Bayer Chem:274 [potassium]");
		assertEquals(1500, tRow.get("duration").getAsLong());
		assertEquals(0, tRow.get("eut").getAsLong(), "eut 0 — the NBT_NO_CONSTANT_POWER face");
		assertEquals("gt6:dust_bauxite", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals("gt6:dust_small_potassium_hydroxide", slotId(tRow.getAsJsonArray("inputs").get(1)));
		assertEquals(6, slotCount(tRow.getAsJsonArray("inputs").get(1)), "OP.dustSmall.mat(KOH, 6)");
		assertEquals("gt6:steam", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(48000, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "FL.Steam.make(48000)");
		assertEquals("gt6:distilled_water", slotId(tRow.getAsJsonArray("fluidOutputs").get(0)));
		assertEquals(1050, slotAmount(tRow.getAsJsonArray("fluidOutputs").get(0)), "FL.DistW.make(300+750)");
		JsonArray tOuts = tRow.getAsJsonArray("outputs");
		assertEquals(3, tOuts.size(), "KAlO2 + Ilmenite tiny + Rutile tiny");
		assertEquals(10000, tOuts.get(0).getAsJsonObject().get("chance").getAsLong(), "new long[] {10000, 5000, 5000}[0]");
		assertEquals(5000, tOuts.get(1).getAsJsonObject().get("chance").getAsLong(), "[1]");
		assertEquals(5000, tOuts.get(2).getAsJsonObject().get("chance").getAsLong(), "[2]");
		assertEquals("gt6:dust_potassium_aluminate", slotId(tOuts.get(0)));
		assertEquals(2, slotCount(tOuts.get(0)));
		assertEquals("gt6:crushed_centrifuged_tiny_ilmenite", slotId(tOuts.get(1)));
		assertEquals(2, slotCount(tOuts.get(1)));
		assertEquals("gt6:crushed_centrifuged_tiny_rutile", slotId(tOuts.get(2)));
		assertEquals(1, slotCount(tOuts.get(2)));
	}

	/** Verbatim 8 — the thorium-salt large-batch leg (Chem:389): the 20736 mB molten-carrier literal both ways at 1152 t / 64 EUt. */
	@Test
	public void theThoriumSaltLargeBatchRowIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("injectorchem"), "RM.Injector Chem:389");
		assertEquals(1152, tRow.get("duration").getAsLong());
		assertEquals(64, tRow.get("eut").getAsLong());
		assertEquals("gt6:dust_thorium", slotId(tRow.getAsJsonArray("inputs").get(0)));
		assertEquals("gt6:lithium_chloride_molten", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(20736, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "MT.LiCl.liquid(U*144) at 144 mB/U");
		assertEquals("gt6:thoriumsalt", slotId(tRow.getAsJsonArray("fluidOutputs").get(0)));
		assertEquals(20736, slotAmount(tRow.getAsJsonArray("fluidOutputs").get(0)), "FL.Thorium_Salt.make(20736)");
	}

	/** Verbatim 9 — the Ender Air cryo split (Chem:365): the integer-floor gas ladder and the {6000} chance on the ash dust. */
	@Test
	public void theEnderAirCryoRowIsUpstreamVerbatim() throws Exception {
		JsonObject tRow = findRow(pourShipped("cryodistillationtowerchem"), "RM.CryoDT Chem:365 Ender Air");
		assertEquals(64, tRow.get("duration").getAsLong());
		assertEquals(64, tRow.get("eut").getAsLong());
		assertEquals("gt6:enderair", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(200, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)));
		JsonArray tFouts = tRow.getAsJsonArray("fluidOutputs");
		assertEquals(6, tFouts.size(), "N + O + CO2 + Kr + Xe + Rn");
		assertEquals(142, tFouts.get(0).getAsJsonObject().get("amount").getAsInt(), "MT.N.gas(U7) — the long floor");
		assertEquals(50, tFouts.get(1).getAsJsonObject().get("amount").getAsInt(), "MT.O.gas(U20)");
		assertEquals(10, tFouts.get(2).getAsJsonObject().get("amount").getAsInt(), "MT.CO2.gas(U100)");
		assertEquals(1, tFouts.get(3).getAsJsonObject().get("amount").getAsInt(), "MT.Kr.gas(U1000)");
		assertEquals(6000, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsLong(), "new long[] {6000}");
		assertEquals("gt6:dust_tiny_ashes", slotId(tRow.getAsJsonArray("outputs").get(0)), "OP.dustTiny.mat(MT.Ice/Ash, 1)");
	}

	/** Verbatim 10 — the plain-air cryo split (Chem:363, the flipped no-plain-air pin): FL.Air 200 in, the He/Ne/Ar ladder and the {9000} chance on the ash dust. */
	@Test
	public void thePlainAirCryoRowIsUpstreamVerbatim() throws Exception {
		assertTrue(rowsText("cryodistillationtowerchem.json").contains("gt6:air\""),
				":363 — the plain-air row ships on the registered gt6:air carrier (task cryo-distillery-air-rewire)");
		JsonObject tRow = findRow(pourShipped("cryodistillationtowerchem"), "RM.CryoDT Chem:363 Plain Air");
		assertEquals(64, tRow.get("duration").getAsLong());
		assertEquals(64, tRow.get("eut").getAsLong());
		assertEquals("gt6:air", slotId(tRow.getAsJsonArray("fluidInputs").get(0)));
		assertEquals(200, slotAmount(tRow.getAsJsonArray("fluidInputs").get(0)), "FL.Air.make(200)");
		JsonArray tFouts = tRow.getAsJsonArray("fluidOutputs");
		assertEquals(6, tFouts.size(), "N + O + CO2 + He + Ne + Ar");
		assertEquals("gt6:nitrogen", slotId(tFouts.get(0)));
		assertEquals(142, slotAmount(tFouts.get(0)), "MT.N.gas(U7) — the long floor");
		assertEquals("gt6:oxygen", slotId(tFouts.get(1)));
		assertEquals(50, slotAmount(tFouts.get(1)), "MT.O.gas(U20)");
		assertEquals("gt6:carbondioxide", slotId(tFouts.get(2)));
		assertEquals(10, slotAmount(tFouts.get(2)), "MT.CO2.gas(U100)");
		assertEquals("gt6:helium", slotId(tFouts.get(3)));
		assertEquals(1, slotAmount(tFouts.get(3)), "MT.He.gas(U1000)");
		assertEquals("gt6:neon", slotId(tFouts.get(4)));
		assertEquals(1, slotAmount(tFouts.get(4)), "MT.Ne.gas(U1000)");
		assertEquals("gt6:argon", slotId(tFouts.get(5)));
		assertEquals(1, slotAmount(tFouts.get(5)), "MT.Ar.gas(U1000)");
		assertEquals(9000, tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("chance").getAsLong(), "new long[] {9000}");
		assertEquals("gt6:dust_tiny_ashes", slotId(tRow.getAsJsonArray("outputs").get(0)), "OP.dustTiny.mat(MT.Ice, 1) — the file's seated Ice/Ash dust leg");
	}

	/** The kelp census: the forgery is GONE from autoclave.json (the user bug-report anchor), the file is the 12-row Bayer face. */
	@Test
	public void theKelpForgeryRowIsDeletedAndTheBayerFaceShips() throws Exception {
		JsonArray tRows = pourShipped("autoclave");
		for (JsonElement tElement : tRows) {
			assertFalse(tElement.toString().contains("kelp"), "the port-original kelp placeholder has no 1.7.10 upstream basis — deleted");
		}
		assertEquals(12, tRows.size(), "the 12 Bayer rows (:274-285) replace it");
		int tSodium = 0, tPotassium = 0;
		for (JsonElement tElement : pourShipped("autoclave")) {
			String tComment = tElement.getAsJsonObject().get("comment").getAsString();
			if (tComment.contains("[sodium]")) tSodium++;
			if (tComment.contains("[potassium]")) tPotassium++;
		}
		assertEquals(6, tSodium, ":280-285");
		assertEquals(6, tPotassium, ":274-279");
	}

	/** The declared-skip pins: the unregistered-carrier faces ride NO shipped row (the file headers carry the declarations). */
	@Test
	public void theDeclaredSkipsShipNoRows() throws Exception {
		assertFalse(rowsText("mixerchem.json").contains("hydrogensulfide"), ":140-141/:156 — no H2S carrier");
		assertFalse(rowsText("mixerchem.json").contains("glycerol"), ":151 — no Glycerol carrier");
		assertFalse(rowsText("mixerchem.json").contains("nitrofuel"), ":266 — no NitroFuel carrier");
		assertFalse(rowsText("mixerchem.json").contains("reikygen"), ":153 — the RotaryCraft oxygen alias is absent");
		assertFalse(rowsText("mixerchem.json").contains("biodiesel"), ":380-381 — no biodiesel carrier");
	}

	/**
	 * The HeatMixer ALIAS pin (the task recipe-b6b flip, replacing the old no-mention
	 * assertion): upstream RM.java:75 {@code HeatMixer = Mixer} is the pure-alias row, so
	 * the ten Chem stations (:101-102/:121-128/:159/:166/:174) own no map — they ride the
	 * MIXER map instance through this very mixerchem key (zero dedicated Java face, the
	 * JsonLoader POURABLE carries no heatmixer key on purpose). The old "the HEATMIXER
	 * face pools until a map-registration card" judgment is VOID.
	 */
	@Test
	public void theHeatMixerAliasRidesTheMixerMap() {
		assertSame(GT6RecipeMaps.MIXER, GT6RecipeMapJsonLoader.mapFor("mixerchem"),
				"RM.java:75 HeatMixer = Mixer: the Chem HeatMixer face pours through the mixerchem key onto MIXER");
		assertNull(GT6RecipeMapJsonLoader.mapFor("heatmixer"),
				"the alias form: mapFor has no heatmixer case — the stations need none");
	}

	/**
	 * The live-id face, offline: every gt6 item id the seven files reference must be a member
	 * of the registration walks' id universe (GTMaterialItems + GTMaterialBlocks
	 * registrationOrder over the initialized material system — the B1 test :206-234 pattern).
	 */
	@Test
	public void everyGt6IdTheChemFaceFilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		java.util.Set<String> tUniverse = new java.util.HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		assertTrue(tUniverse.contains("gt6:dust_coal"), "the id universe built (" + tUniverse.size() + " ids)");

		java.util.Set<String> tMissing = new java.util.TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB1ChemRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						assertTrue(tId.startsWith("gt6:") || tId.startsWith("minecraft:"), "namespace-less item id (a live pour would hit minecraft:): " + tKey + ": " + tId);
						if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tKey + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------------ helpers

	/** The row payload of one shipped file, as text (the _header declaration member excluded — it cites the skips by name). */
	private static String rowsText(String aName) throws Exception {
		InputStream tStream = GT6RecipeMapDataB1ChemRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aName);
		assertNotNull(tStream, aName);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		return tDoc.getAsJsonArray("recipes").toString();
	}

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

	private static int slotAmount(JsonElement aSlot) {
		return aSlot.getAsJsonObject().get("amount").getAsInt();
	}
}
