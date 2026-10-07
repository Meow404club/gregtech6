package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Turbines;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The powertrain row census (task material-mc-d-powertrain-rows — the mc-D card's
 * domain pin, the GT6KineticsTabCensusTest posture over the row-table VALUES): the
 * four ladders ride their Loader kinetic-section columns verbatim and the axle tail
 * rows close the census gap. Table-level (name+number, the AXLE_SPECS no-MT ruling —
 * the construct event never fires on the forge leg's bare JVM) + two classpath JSON
 * spot pins (the generated-resources convention, the GT6DieselCraftingJsonTest form).
 *
 * <p>The census erratum the card carries (the main-session ruling): the upstream
 * "Transformer Gearbox / Custom Gearbox" 13-row families are 12 NEW rows + 1 SEATED
 * wooden singleton each (GT6Kinetics.TRANSFORMER_ROTATION / GEARBOX, the Loader
 * :1668/:1669 rows ported by gearbox-transformer) — the tables below deliberately do
 * NOT duplicate the wood rows. The BE pool declaration (the A-case card): the row
 * VALUES are the data layer, the converter/motor behaviour (TileEntityBase11Bipolar /
 * Base11Motor) is the kinetics-BE functional card's surface.
 */
public class GT6PowertrainRowCensusTest extends GTOfflineTestBase {

	/** The axle tail rows: the Trinaquadalloy/Adamantium specs ride VMAX[8]/[9] with the doubling bandwidth ladders. */
	@Test
	public void theAxleTailRowsCloseTheCensusGap() {
		assertEquals(13, GT6Kinetics.AXLE_SPECS.size(), "the Loader kinetic material rows (:1662-:1763)");
		GT6Kinetics.AxleSpec tTrina = GT6Kinetics.AXLE_SPECS.get(11);
		assertEquals("trinaquadalloy", tTrina.material());
		assertEquals("Trinaquadalloy", tTrina.matDisplay());
		assertEquals(8, tTrina.tier());
		assertEquals(1048576L, GT6Kinetics.axleSpeed(tTrina), "VMAX[8]");
		assertEquals(java.util.List.of(256, 512, 1024, 2048), java.util.Arrays.stream(tTrina.bandwidth()).boxed().toList(),
				"the :1752-:1755 bandwidth ladder (each rung doubles the previous material)");
		GT6Kinetics.AxleSpec tAd = GT6Kinetics.AXLE_SPECS.get(12);
		assertEquals("adamantium", tAd.material());
		assertEquals("Adamantium", tAd.matDisplay());
		assertEquals(9, tAd.tier());
		assertEquals(4194304L, GT6Kinetics.axleSpeed(tAd), "VMAX[9]");
		assertEquals(java.util.List.of(512, 1024, 2048, 4096), java.util.Arrays.stream(tAd.bandwidth()).boxed().toList(),
				"the :1760-:1763 bandwidth ladder");
	}

	/** The 13 rotation-engine rows: V[t] → V[t]/2, the wood row first (aWooden), the early-lubricant split at Steel. */
	@Test
	public void theRotationEnginesRideTheBipolarColumns() {
		assertEquals(13, GT6Kinetics.ROTATION_ENGINES.size(), "the :1667 wood row + the :1676-:1764 metal rows");
		GT6Kinetics.RotationEngineRow tWood = GT6Kinetics.ROTATION_ENGINES.get(0);
		assertEquals("rotation_engine_wood_treated", tWood.path());
		assertTrue(tWood.wooden(), "the :1667 aWooden row");
		assertEquals(8, tWood.inputSpeed(), "V[0]");
		assertEquals(4, tWood.outputSpeed(), "V[0]/2 — the RU→KU halving");
		assertTrue(tWood.earlyLubricant(), "the wood row rides OD.itemLubricantEarly");
		for (int i = 1; i < GT6Kinetics.ROTATION_ENGINES.size(); i++) {
			GT6Kinetics.RotationEngineRow tRow = GT6Kinetics.ROTATION_ENGINES.get(i);
			assertEquals(tRow.inputSpeed() / 2, tRow.outputSpeed(), "V[t]/2 on " + tRow.path());
			// the early→regular lubricant split rides the Steel row (:1708 OD.itemLubricant)
			assertEquals(i >= 5, !tRow.earlyLubricant(), "the lubricant column on " + tRow.path());
		}
		assertEquals(512, GT6Kinetics.ROTATION_ENGINES.get(6).inputSpeed(), "Ti V[3]");
		assertEquals(1048576, GT6Kinetics.ROTATION_ENGINES.get(12).outputSpeed(), "Adamantium V[9]/2");
	}

	/** The 12 metal transformer-gearbox rows: output = input/4 (the ÷4-speed ×4-power law); the wood row stays the seated singleton. */
	@Test
	public void theTransformerGearboxesHalveSpeedByFour() {
		assertEquals(12, GT6Kinetics.TRANSFORMER_GEARBOXES.size(), "the :1677-:1765 metal rows (the wood row = the seated TRANSFORMER_ROTATION singleton)");
		assertTrue(GT6Kinetics.TRANSFORMER_GEARBOXES.stream().noneMatch(GT6Kinetics.TransformerGearboxRow::wooden),
				"no wood row in the metal table — the negative pin (mc-C/P1' form)");
		for (GT6Kinetics.TransformerGearboxRow tRow : GT6Kinetics.TRANSFORMER_GEARBOXES) {
			assertEquals(tRow.inputSpeed() / 4, tRow.outputSpeed(), "V[t]→V[t-1] = ÷4 on " + tRow.path());
		}
		assertEquals(32, GT6Kinetics.TRANSFORMER_GEARBOXES.get(0).inputSpeed(), "Bronze V[1]");
		assertEquals(8, GT6Kinetics.TRANSFORMER_GEARBOXES.get(0).outputSpeed(), "Bronze V[0]");
		assertEquals(2097152, GT6Kinetics.TRANSFORMER_GEARBOXES.get(11).inputSpeed(), "Adamantium V[9]");
	}

	/** The 12 metal custom-gearbox rows: the VMAX[t] throughput rating; the wood row stays the seated singleton. */
	@Test
	public void theCustomGearboxesRideTheVmaxRating() {
		assertEquals(12, GT6Kinetics.CUSTOM_GEARBOXES.size(), "the :1678-:1766 metal rows (the wood row = the seated GEARBOX singleton)");
		assertTrue(GT6Kinetics.CUSTOM_GEARBOXES.stream().noneMatch(GT6Kinetics.CustomGearboxRow::wooden),
				"no wood row in the metal table — the negative pin");
		// the tier column: Bronze/Brass/AsCu/AsBr ride VMAX[1], then Steel..Adamantium double per row
		int[] tTiers = {1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		for (int i = 0; i < GT6Kinetics.CUSTOM_GEARBOXES.size(); i++) {
			GT6Kinetics.CustomGearboxRow tRow = GT6Kinetics.CUSTOM_GEARBOXES.get(i);
			assertEquals(GT6Kinetics.VMAX[tTiers[i]], tRow.maxThroughput(), "VMAX[tier] on " + tRow.path());
		}
	}

	/** The 15 small steam turbine rows: the :794-:811 inputs/outputs verbatim over the Kinetic_T body tiers. */
	@Test
	public void theSmallSteamTurbinesRideTheTurbineColumns() {
		assertEquals(15, GT6Kinetics.STEAM_TURBINES.size(), "the :794-:811 rows");
		// the Kinetic_T body tier (NBT_MATERIAL): T[1]=Bronze x3, T[2]=Steel x5, T[3]=Ti x4, T[4]=Tungstensteel x3
		for (int i = 0; i < 15; i++) {
			String tBody = GT6Kinetics.STEAM_TURBINES.get(i).bodySlug();
			assertEquals(i < 3 ? "bronze" : i < 8 ? "steel" : i < 12 ? "titanium" : "tungstensteel",
					tBody, "the body tier on row " + i);
		}
		assertEquals(48, GT6Kinetics.STEAM_TURBINES.get(0).inputEU(), "Bronze 24*STEAM_PER_EU (STEAM_PER_EU=2, CS.java:240)");
		assertEquals(16, GT6Kinetics.STEAM_TURBINES.get(0).output(), "the Bronze NBT_OUTPUT");
		assertEquals(6144, GT6Kinetics.STEAM_TURBINES.get(14).inputEU(), "Graphene 3072*2");
		assertEquals(2048, GT6Kinetics.STEAM_TURBINES.get(14).output(), "the Graphene NBT_OUTPUT");
		// the path prefix is small_steam_turbine_* — the steam_turbine_* paths are the LARGE
		// turbine main-housing controllers (GT6Turbines.STEAM_ROWS), the 21.1 duplicate-key wall
		for (GT6Kinetics.SteamTurbineRow tRow : GT6Kinetics.STEAM_TURBINES) {
			assertTrue(tRow.path().startsWith("small_steam_turbine_"), tRow.path());
			assertNull(GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()), "no collision with the large-turbine controller paths");
		}
	}

	/** The registration maps fill 1:1 with the tables (the static-init face the forge leg can read without the registry). */
	@Test
	public void theRegistrationMapsFillWithTheirTables() {
		assertEquals(13, GT6Kinetics.ROTATION_ENGINE_ITEMS.size());
		assertEquals(12, GT6Kinetics.TRANSFORMER_GEARBOX_ITEMS.size());
		assertEquals(12, GT6Kinetics.CUSTOM_GEARBOX_ITEMS.size());
		assertEquals(15, GT6Kinetics.STEAM_TURBINE_ITEMS.size());
		assertEquals(52, GT6Kinetics.ROTATION_ENGINE_ITEMS.size() + GT6Kinetics.TRANSFORMER_GEARBOX_ITEMS.size()
				+ GT6Kinetics.CUSTOM_GEARBOX_ITEMS.size() + GT6Kinetics.STEAM_TURBINE_ITEMS.size(),
				"the 52 new powertrain rows (the census erratum: 60 new + 2 seated singletons = the 62-row census gap)");
	}

	/** The recipe spot pins: the wood rotation-engine grid and one SST row verbatim off the classpath (the diesel JSON form). */
	@Test
	public void theRecipeJsonsCarryTheLoaderGrids() throws Exception {
		JsonObject tWood = generated("rotation_engine_wood_treated");
		assertEquals(3, tWood.getAsJsonArray("pattern").size(), "the :1667 three-row grid");
		assertEquals("[\"PSP\",\"wAL\",\"GAG\"]", tWood.getAsJsonArray("pattern").toString(),
				"the :1667 grid verbatim (P=plate, S=gearGtSmall, G=gearGt, w=wrench, A=axle, L=lube)");
		assertEquals("gt6:lubricant_bucket", keyOf(tWood, 'L'), "the single lubricant carrier");
		assertEquals("gt6:axle_wood_treated_medium", keyOf(tWood, 'A'), "the :1667 aRegistry.getItem(24801) shape");
		JsonObject tTurbine = generated("small_steam_turbine_graphene");
		assertEquals("[\"TwT\",\"GSG\",\"TMT\"]", tTurbine.getAsJsonArray("pattern").toString(), "the :811 grid verbatim");
		JsonObject tResult = tTurbine.getAsJsonObject("result");
		assertEquals("gt6:small_steam_turbine_graphene", tResult.get("item").getAsString(), "the block item result");
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6PowertrainRowCensusTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** The 1.20.1 key face: each symbol resolves to a {"item": ...} object (the faucet-card JSON lesson). */
	private static String keyOf(JsonObject aRecipe, char aKey) {
		return aRecipe.getAsJsonObject("key").getAsJsonObject(Character.toString(aKey)).get("item").getAsString();
	}
}
