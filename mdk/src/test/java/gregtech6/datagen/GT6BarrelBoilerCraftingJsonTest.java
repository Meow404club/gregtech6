package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The barrel + boiler obtainability pins (task crafting-barrels-boilers — the
 * recipe-bidirectional-census P1 card, the GT6MachineWallCraftingJsonTest shape): the
 * upstream registration tails ship verbatim under {@code data/gt6/recipes/}:
 * <ul>
 * <li>barrel/barrel_wood.json — Loader_MultiTileEntities.java:2140 "rGs","PSP","PSP"
 *     ('r' = the CR.java:355 softhammer auto-bind, 'G' = OD.itemGlue folded to the slime
 *     ball — no glue item in the port universe, the bottlecrate precedent, 's' = the saw
 *     auto-bind, 'P' = plate_wood_treated, 'S' = stick_long_iron);</li>
 * <li>barrel/barrel_metal.json — :2151 Bronze Drum " h ","PSP","PSP" (plate_curved_bronze
 *     + stick_long_bronze + the hard hammer);</li>
 * <li>boiler/&lt;path&gt;.json ×26 — :553-565 " P ","PwP","PhP" over plate_double and
 *     :567-579 the same grid over plateDense (the census "plateDouble form" note is the
 *     standard band; the Strong band rides plateDense verbatim upstream).</li>
 * </ul>
 * DECLARED CUT (negative pin): :2150 Plastic Canister carries NO recipe tail upstream —
 * no barrel_plastic recipe ships, by upstream fidelity, NOT a census gap.
 */
public class GT6BarrelBoilerCraftingJsonTest {

	@BeforeAll
	static void boot() {
		try { net.minecraft.SharedConstants.tryDetectVersion(); } catch (Throwable ignored) {}
		try { net.minecraft.server.Bootstrap.bootStrap(); } catch (Throwable ignored) {}
	}

	/** The :2140 Wooden Barrel — the "rGs","PSP","PSP" grid verbatim. */
	@Test
	public void theWoodenBarrelShipsTheUpstream2140Grid() throws Exception {
		JsonObject tJson = generated("barrel/barrel_wood");
		assertEquals("rGs", tJson.getAsJsonArray("pattern").get(0).getAsString(), ":2140 row 1");
		assertEquals("PSP", tJson.getAsJsonArray("pattern").get(1).getAsString(), ":2140 row 2");
		assertEquals("PSP", tJson.getAsJsonArray("pattern").get(2).getAsString(), ":2140 row 3");
		JsonObject tKey = tJson.getAsJsonObject("key");
		assertEquals("gt6:tools/soft_hammer", tKey.getAsJsonObject("r").get("tag").getAsString(), ":2140 'r' = the softhammer auto-bind");
		assertEquals("minecraft:slime_ball", tKey.getAsJsonObject("G").get("item").getAsString(), ":2140 'G' = the itemGlue fold");
		assertEquals("gt6:tools/saw", tKey.getAsJsonObject("s").get("tag").getAsString(), ":2140 's' = the saw auto-bind");
		assertEquals("gt6:plate_wood_treated", tKey.getAsJsonObject("P").get("item").getAsString(), ":2140 'P' = OP.plate.dat(MT.WoodTreated)");
		assertEquals("gt6:stick_long_iron", tKey.getAsJsonObject("S").get("item").getAsString(), ":2140 'S' = OP.stickLong.dat(ANY.Iron)");
		assertEquals("gt6:barrel_wood", tJson.getAsJsonObject("result").get("item").getAsString(), ":2140 the result is the barrel itself");
	}

	/** The :2151 Bronze Drum — the " h ","PSP","PSP" grid verbatim. */
	@Test
	public void theBronzeDrumShipsTheUpstream2151Grid() throws Exception {
		JsonObject tJson = generated("barrel/barrel_metal");
		assertEquals(" h ", tJson.getAsJsonArray("pattern").get(0).getAsString(), ":2151 row 1");
		assertEquals("PSP", tJson.getAsJsonArray("pattern").get(1).getAsString(), ":2151 row 2");
		assertEquals("PSP", tJson.getAsJsonArray("pattern").get(2).getAsString(), ":2151 row 3");
		JsonObject tKey = tJson.getAsJsonObject("key");
		assertEquals("gt6:tools/hard_hammer", tKey.getAsJsonObject("h").get("tag").getAsString(), ":2151 'h' = the hard-hammer letter");
		assertEquals("gt6:plate_curved_bronze", tKey.getAsJsonObject("P").get("item").getAsString(), ":2151 'P' = OP.plateCurved.dat(MT.Bronze)");
		assertEquals("gt6:stick_long_bronze", tKey.getAsJsonObject("S").get("item").getAsString(), ":2151 'S' = OP.stickLong.dat(MT.Bronze)");
		assertEquals("gt6:barrel_metal", tJson.getAsJsonObject("result").get("item").getAsString(), ":2151 the result is the drum itself");
	}

	/** The :2150 Plastic Canister CUT — the upstream row carries no recipe tail. */
	@Test
	public void thePlasticCanisterShipsNoRecipe() throws Exception {
		assertNull(resourceOrNull("data/gt6/recipes/barrel/barrel_plastic.json"),
				":2150 the declared CUT — no recipe ships for the plastic canister");
	}

	/** All 26 boiler rows ship the " P ","PwP","PhP" grid: standard = plateDouble, Strong = plateDense. */
	@Test
	public void theTwentySixBoilersShipTheUpstreamGrids() throws Exception {
		for (var tRow : gregtech6.registry.GT6Boilers.allRows()) {
			JsonObject tJson = generated("boiler/" + tRow.path());
			assertEquals(" P ", tJson.getAsJsonArray("pattern").get(0).getAsString(), tRow.path() + " row 1");
			assertEquals("PwP", tJson.getAsJsonArray("pattern").get(1).getAsString(), tRow.path() + " row 2");
			assertEquals("PhP", tJson.getAsJsonArray("pattern").get(2).getAsString(), tRow.path() + " row 3");
			JsonObject tKey = tJson.getAsJsonObject("key");
			String tPlateId = "gt6:plate_" + (tRow.strong() ? "dense_" : "double_") + tRow.material().slug();
			assertEquals(tPlateId, tKey.getAsJsonObject("P").get("item").getAsString(),
					tRow.path() + ": the " + (tRow.strong() ? ":567-579 plateDense" : ":553-565 plateDouble") + " column");
			assertEquals("gt6:tools/wrench", tKey.getAsJsonObject("w").get("tag").getAsString(), tRow.path() + ": the 'w' wrench auto-bind");
			assertEquals("gt6:tools/hard_hammer", tKey.getAsJsonObject("h").get("tag").getAsString(), tRow.path() + ": the 'h' hard-hammer auto-bind");
			assertEquals("gt6:" + tRow.path(), tJson.getAsJsonObject("result").get("item").getAsString(), tRow.path() + ": the result is the boiler itself");
		}
	}

	/** The census ratchet: the builder walks exactly the 26 registration rows (a dropped row fails here, not silently). */
	@Test
	public void theBoilerCensusStaysTwentySix() {
		assertEquals(26, gregtech6.registry.GT6Boilers.allRows().size(), "the :553-579 census stays 26 (13 standard + 13 Strong)");
	}

	private static JsonObject generated(String aId) throws Exception {
		byte[] tBytes = resourceOrNull("data/gt6/recipes/" + aId + ".json");
		assertNotNull(tBytes, "data/gt6/recipes/" + aId + ".json rides the generated-resources classpath");
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6BarrelBoilerCraftingJsonTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
