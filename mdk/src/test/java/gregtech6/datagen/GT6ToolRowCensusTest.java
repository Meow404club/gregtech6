package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import net.minecraft.world.level.block.SoundType;

import gregapi.data.MT;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6MaterialTestSupport;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The tool-domain row census (task material-mc-e-tool-anvil-rows — the mc-E card's
 * domain pin, the GT6FaucetRowCensusTest posture): the FULL anvil ladder rides the
 * Loader_MultiTileEntities.java:2185-2219 columns verbatim (material / NBT_DURABILITY /
 * the aUtil carrier column, whose port proxy is the row's sound family) and the three
 * kitchen TABLE variants join the four-pot family (the :2174/:2176/:2178 rows — the
 * carrier columns are the registration literals reviewed verbatim against the loader
 * lines; the generated-tree faces pin their registration surfaces below, the offline
 * forge leg never binds the RegistryObjects, the census-posture ruling).
 *
 * <p>The negative ledger the craft walk carries: an anvil row's recipe skips when its
 * carrier item ({@code OP.stone}/{@code OP.ingot} dual) is absent from the port
 * universe — {@link #theGeneratedRecipeSetCarriesTheResolvableRows} walks the generated
 * recipe set and pins the ledger's shape (the vanilla-carried pair can never skip).
 */
public class GT6ToolRowCensusTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		GT6MaterialTestSupport.materials();
	}

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6ToolRowCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tStream,
					java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static boolean generatedExists(String aPath) {
		try (InputStream tStream = GT6ToolRowCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream != null;
		} catch (Exception aE) {
			return false;
		}
	}

	// -------------------------------------------------------------------
	// the anvil ladder against the Loader columns
	// -------------------------------------------------------------------

	/** The full 35-row ladder: identity + the durability rungs at every seat (the :2185-2219 walk). */
	@Test
	public void theAnvilLadderRidesTheLoaderColumns() {
		assertEquals(35, GT6Anvils.ROWS.size(), "the Loader:2185-2219 projection");
		List<String> tSeen = new ArrayList<>();
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) {
			tSeen.add(tRow.path());
			assertTrue(tRow.durability() > 0, "the durability column on " + tRow.path());
		}
		assertEquals(35, tSeen.stream().distinct().count(), "paths unique");
		assertSame(MT.Stone, GT6Anvils.ROWS.get(0).material().get());
		assertEquals(10000L, GT6Anvils.ROWS.get(0).durability());
		assertEquals("stone_anvil", GT6Anvils.ROWS.get(0).path());
		assertSame(MT.STONES.Blackstone, GT6Anvils.ROWS.get(1).material().get());
		assertEquals(100000L, GT6Anvils.ROWS.get(1).durability());
		assertSame(MT.STONES.GraniteBlack, GT6Anvils.ROWS.get(2).material().get(), ":2187");
		assertSame(MT.STONES.GraniteRed, GT6Anvils.ROWS.get(3).material().get(), ":2188");
		assertSame(MT.Pb, GT6Anvils.ROWS.get(4).material().get(), ":2189");
		assertEquals(800000L, GT6Anvils.ROWS.get(4).durability(), "the lead rung");
		assertSame(MT.Bronze, GT6Anvils.ROWS.get(5).material().get(), ":2190");
		assertSame(MT.ArsenicCopper, GT6Anvils.ROWS.get(6).material().get(), ":2191");
		assertSame(MT.ArsenicBronze, GT6Anvils.ROWS.get(7).material().get(), ":2192");
		assertSame(MT.Syrmorite, GT6Anvils.ROWS.get(8).material().get(), ":2193");
		assertSame(MT.IronWood, GT6Anvils.ROWS.get(9).material().get(), ":2194");
		assertEquals(7500000L, GT6Anvils.ROWS.get(9).durability(), "the ironwood rung");
		assertSame(MT.Steel, GT6Anvils.ROWS.get(10).material().get(), ":2195 — the ANY.Steel identity");
		assertSame(MT.Desh, GT6Anvils.ROWS.get(11).material().get(), ":2196");
		assertSame(MT.Efrine, GT6Anvils.ROWS.get(12).material().get(), ":2197");
		assertSame(MT.Thaumium, GT6Anvils.ROWS.get(13).material().get(), ":2198");
		assertSame(MT.Manasteel, GT6Anvils.ROWS.get(14).material().get(), ":2199");
		assertSame(MT.BlackSteel, GT6Anvils.ROWS.get(15).material().get(), ":2200 — the ANY.BlackSteel identity");
		assertSame(MT.BlueSteel, GT6Anvils.ROWS.get(16).material().get(), ":2201");
		assertSame(MT.RedSteel, GT6Anvils.ROWS.get(17).material().get(), ":2202");
		assertSame(MT.VanadiumSteel, GT6Anvils.ROWS.get(18).material().get(), ":2203");
		assertSame(MT.Octine, GT6Anvils.ROWS.get(19).material().get(), ":2204");
		assertSame(MT.FierySteel, GT6Anvils.ROWS.get(20).material().get(), ":2205");
		assertSame(MT.TungstenAlloy, GT6Anvils.ROWS.get(21).material().get(), ":2206");
		assertSame(MT.Ti, GT6Anvils.ROWS.get(22).material().get(), ":2207");
		assertSame(MT.Netherite, GT6Anvils.ROWS.get(23).material().get(), ":2208");
		assertSame(MT.Terrasteel, GT6Anvils.ROWS.get(24).material().get(), ":2209");
		assertSame(MT.VoidMetal, GT6Anvils.ROWS.get(25).material().get(), ":2210");
		assertSame(MT.TitaniumGold, GT6Anvils.ROWS.get(26).material().get(), ":2211");
		assertSame(MT.TungstenSteel, GT6Anvils.ROWS.get(27).material().get(), ":2212");
		assertSame(MT.Tungsten, GT6Anvils.ROWS.get(28).material().get(), ":2213 — the ANY.W identity");
		assertSame(MT.Ir, GT6Anvils.ROWS.get(29).material().get(), ":2214");
		assertSame(MT.GaiaSpirit, GT6Anvils.ROWS.get(30).material().get(), ":2215");
		assertSame(MT.Ad, GT6Anvils.ROWS.get(31).material().get(), ":2216");
		assertSame(MT.Draconium, GT6Anvils.ROWS.get(32).material().get(), ":2217");
		assertSame(MT.DraconiumAwakened, GT6Anvils.ROWS.get(33).material().get(), ":2218");
		assertSame(MT.Infinity, GT6Anvils.ROWS.get(34).material().get(), ":2219");
		assertEquals(1000000000000000L, GT6Anvils.ROWS.get(34).durability(), "the infinity rung — a quadrillion");
	}

	/** The aUtil column: STONE = the 4 stone-carrier rows, WOOD = the Ironwood singleton, METAL = the 30 ingot rungs. */
	@Test
	public void theSoundColumnIsTheUtilCarrierProxy() {
		Set<String> tWood = new TreeSet<>(), tStone = new TreeSet<>();
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) {
			if (GT6Anvils.isWoodBand(tRow)) tWood.add(tRow.path());
			else if (tRow.sound() == SoundType.STONE) tStone.add(tRow.path());
		}
		assertEquals(Set.of("iron_wood_anvil"), tWood, "the single :2194 aUtilWood rung (the axe band)");
		assertEquals(Set.of("stone_anvil", "blackstone_anvil", "granite_black_anvil", "granite_red_anvil"),
				tStone, "the aUtilStone carrier rows (the :2185-2188 head)");
		assertEquals(30, GT6Anvils.ROWS.size() - tWood.size() - tStone.size(), "the aUtilMetal rungs (the pickaxe band)");
	}

	// -------------------------------------------------------------------
	// the generated tree (recipes / blockstates / loot / tags / lang)
	// -------------------------------------------------------------------

	/**
	 * The craft walk's survivors + the negative ledger: the generated recipe set carries
	 * exactly the rows whose carrier item resolves; the vanilla-carried pair (stone →
	 * {@code minecraft:stone}, blackstone → {@code minecraft:blackstone}) can never skip.
	 */
	@Test
	public void theGeneratedRecipeSetCarriesTheResolvableRows() throws Exception {
		Set<String> tRecipes = new TreeSet<>();
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) {
			if (generatedExists("data/gt6/recipes/" + tRow.path() + ".json")) tRecipes.add(tRow.path());
		}
		assertTrue(tRecipes.contains("stone_anvil") && tRecipes.contains("blackstone_anvil"),
				"the vanilla-carried rows always land: " + tRecipes);
		JsonObject tBronze = generatedJson("data/gt6/recipes/bronze_anvil.json");
		assertEquals("gt6:bronze_anvil", tBronze.getAsJsonObject("result").get("item").getAsString());
		assertEquals("RRR", tBronze.getAsJsonArray("pattern").get(0).getAsString());
		for (String tAbsent : GT6Anvils.ROWS.stream().map(GT6Anvils.AnvilRow::path)
				.filter(tPath -> !tRecipes.contains(tPath)).toList()) {
			assertTrue(!tAbsent.equals("stone_anvil") && !tAbsent.equals("blackstone_anvil"),
					"the vanilla-carried row " + tAbsent + " must never skip");
		}
	}

	/** The registration surfaces: 35 anvil blockstates + item models + loot self-drops, the three table blockstates + item models. */
	@Test
	public void theGeneratedTreeCarriesTheRegistrationSurfaces() {
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) {
			assertTrue(generatedExists("assets/gt6/blockstates/" + tRow.path() + ".json"), tRow.path() + " blockstate");
			assertTrue(generatedExists("assets/gt6/models/item/" + tRow.path() + ".json"), tRow.path() + " item model");
			assertTrue(generatedExists("data/gt6/loot_tables/blocks/" + tRow.path() + ".json"), tRow.path() + " loot");
		}
		for (String tTable : new String[] {"bathing_pot_table_wood", "bathing_pot_table_steel", "mixing_bowl_table"}) {
			assertTrue(generatedExists("assets/gt6/blockstates/" + tTable + ".json"), tTable + " blockstate");
			assertTrue(generatedExists("assets/gt6/models/item/" + tTable + ".json"), tTable + " item model");
			assertTrue(generatedExists("data/gt6/recipes/" + tTable + ".json"), tTable + " recipe");
		}
	}

	/** The mineable tags carry the walk: 34 anvils + the steel/bowl tables on pickaxe, the ironwood rung + the wood table on axe. */
	@Test
	public void theMineableTagsCarryTheBandWalk() throws Exception {
		JsonObject tPickaxe = generatedJson("data/minecraft/tags/blocks/mineable/pickaxe.json");
		JsonObject tAxe = generatedJson("data/minecraft/tags/blocks/mineable/axe.json");
		Set<String> tPick = new TreeSet<>(), tAxeSet = new TreeSet<>();
		// the value rows come in both shapes since the G1 dyed-panel wave (plain "gt6:x"
		// strings and {"id": "gt6:x", "required": false} optionals) — normalize to the id
		tPickaxe.getAsJsonArray("values").forEach(t -> tPick.add(
				t.isJsonObject() ? t.getAsJsonObject().get("id").getAsString() : t.getAsString()));
		tAxe.getAsJsonArray("values").forEach(t -> tAxeSet.add(
				t.isJsonObject() ? t.getAsJsonObject().get("id").getAsString() : t.getAsString()));
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) {
			String tId = "gt6:" + tRow.path();
			if (GT6Anvils.isWoodBand(tRow)) {
				assertTrue(tAxeSet.contains(tId), "the axe band carries " + tId);
			} else {
				assertTrue(tPick.contains(tId), "the pickaxe band carries " + tId);
			}
		}
		assertTrue(tPick.contains("gt6:bathing_pot_table_steel"), "the :2176 aUtilMetal table");
		assertTrue(tPick.contains("gt6:mixing_bowl_table"), "the :2178 aUtilStone table");
		assertTrue(tAxeSet.contains("gt6:bathing_pot_table_wood"), "the :2174 aUtilWood table");
	}

	/** The lang faces: en = mNameLocal + " Anvil" (the :2185-2219 name column), zh = the dump mte faces. */
	@Test
	public void theLangFacesCarryTheLadder() throws Exception {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) {
			String tKey = "block.gt6." + tRow.path();
			assertTrue(tEn.has(tKey), "en face " + tKey);
			assertEquals(tRow.material().get().mNameLocal + " Anvil", tEn.get(tKey).getAsString(),
					"the name column on " + tKey);
			assertTrue(tZh.has(tKey), "zh face " + tKey);
			assertTrue(tZh.get(tKey).getAsString().endsWith("砧"), "the zh dump face on " + tKey);
		}
		assertEquals("Stone Anvil", tEn.get("block.gt6.stone_anvil").getAsString());
		assertEquals("石头砧", tZh.get("block.gt6.stone_anvil").getAsString());
		assertEquals("无尽砧", tZh.get("block.gt6.infinity_anvil").getAsString());
		assertEquals("Wooden Bathing Pot Table", tEn.get("block.gt6.bathing_pot_table_wood").getAsString());
		assertEquals("木质浸洗桌", tZh.get("block.gt6.bathing_pot_table_wood").getAsString());
		assertEquals("浸洗桌", tZh.get("block.gt6.bathing_pot_table_steel").getAsString());
		assertEquals("搅拌桌", tZh.get("block.gt6.mixing_bowl_table").getAsString());
	}
}
