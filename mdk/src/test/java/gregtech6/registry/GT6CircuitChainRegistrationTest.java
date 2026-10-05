/*
 * Offline tests for task circuit-chain-items: the GT6CircuitChain registration home — the
 * 53 circuit synthesis-chain intermediates (MultiItemTechnological.java:546-770): the
 * empty plate + 6 wirings + 8 plates + 11 parts + 19 boards + 4 crystal circuits + 5
 * crystal processors, the "有物无方" face of the 7 circuit carriers (GT6Batteries).
 * Task circuit-chain-recipes joins the three gap circuits (30311/30313/30315) for 56.
 *
 * <p>The GT6ElectrodesRegistrationTest posture: the items are NOT constructible in this
 * bootstrapped-and-frozen JVM (the mod-Item intrusive-holder wall), so the assertion
 * surface is the registry-wiring data ({@code DeferredRegister.getEntries()} /
 * {@code RegistryObject.getId()}), the datagen JSON existence + verbatim values (the
 * models, both lang faces), the borrowed sprite files, and the generated circuit-tag
 * cascade — read off the classpath, working-directory independent.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class GT6CircuitChainRegistrationTest {

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/** Both DRs target the vanilla item registry (the GT6Electrodes shape). */
	@Test
	public void registryKeyIsTheVanillaItemRegistry() {
		assertEquals(Registries.ITEM, GT6CircuitChain.ITEMS.getRegistryKey());
	}

	/**
	 * The 56 chain items in upstream meta order (MultiItemTechnological.java:546-770), ids
	 * the snake of the IL field names (the GT6FoodCans ruling), family columns
	 * 8/6/11/19/4+5 + the card-2 gap circuits (task circuit-chain-recipes: the 30311/
	 * 30313/30315 Bath outputs whose materials live in the port tree — the 8 BC circuits
	 * 30380-30387 stay CUT, BuildCraft-gated upstream).
	 */
	@Test
	public void chainRowsAreTheUpstreamFiftyThree() {
		assertEquals(56, GT6CircuitChain.ROWS.size(), "MultiItemTechnological.java:546-770 + the three gap circuits = 56");
		assertEquals(8, GT6CircuitChain.PLATES, "plate census: Empty+Cu+Au+Pt+Magic+Enderium+Signalum+HSLA");
		assertEquals(6, GT6CircuitChain.WIRES, "wiring census: Cu+Au+Pt+Magic+Enderium+Signalum");
		assertEquals(11, GT6CircuitChain.PARTS, "part census: 6 tiers + Magic/Enderium/Signalum + EnderPearl/EnderEye");
		assertEquals(19, GT6CircuitChain.BOARDS, "board census: 6 tiers + Magic/Enderium/Signalum + BC×8 + HSLA/PowerModule");
		assertEquals(9, GT6CircuitChain.CRYSTALS, "crystal census: 4 crystal circuits + socket + 4 processors");
		assertEquals(3, GT6CircuitChain.GAP_CIRCUITS, "gap circuits: Magic/Enderium/Signalum (the 8 BC = CUT)");

		int[] tMetas = {30000, 30001, 30002, 30003, 30004, 30005, 30006, 30011, 30012, 30013, 30014, 30015, 30016,
				30099, 30101, 30102, 30103, 30104, 30105, 30106, 30111, 30113, 30115, 30198, 30199,
				30201, 30202, 30203, 30204, 30205, 30206, 30211, 30213, 30215,
				30280, 30281, 30282, 30283, 30284, 30285, 30286, 30287, 30298, 30299,
				30311, 30313, 30315,
				30401, 30402, 30403, 30404, 30500, 30501, 30502, 30503, 30504};
		assertEquals(GT6CircuitChain.ROWS.size(), tMetas.length, "the meta parity column matches the row walk");
		for (int i = 0; i < tMetas.length; i++) {
			assertEquals(tMetas[i], GT6CircuitChain.ROWS.get(i).metaId(), "row " + i + " rides the upstream meta order");
		}
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		GT6CircuitChain.ITEMS.getEntries().forEach(tEntry -> tIds.add(tEntry.getKey().location()));
		Set<ResourceLocation> tExpected = new LinkedHashSet<>();
		for (GT6CircuitChain.ChainRow tRow : GT6CircuitChain.ROWS) tExpected.add(rl(tRow.path()));
		assertEquals(tExpected, tIds, "the register carries exactly the 53 chain items");
	}

	/**
	 * The datagen JSON existence + verbatim values: the 53 item models (single layer0, the
	 * plain-item form) + both lang faces carrying the row display columns verbatim (the
	 * en MIT:546-770 addItem wordings, the zh tmp/gregtech.lang dump rows 10575-10730)
	 * + the 53 borrowed sprites shipping.
	 */
	@Test
	public void datagenFacesShipUpstreamVerbatim() throws Exception {
		for (GT6CircuitChain.ChainRow tRow : GT6CircuitChain.ROWS) {
			JsonObject tModel = readJson("assets/gt6/models/item/" + tRow.path() + ".json");
			JsonObject tTextures = tModel.getAsJsonObject("textures");
			assertTrue(tTextures.has("layer0"), "the chain model carries the body layer (" + tRow.path() + ")");
			assertTrue(classpathHas("assets/gt6/textures/item/circuit_chain/" + tRow.path() + ".png"),
					"the borrowed upstream sprite must ship (gt.multiitem.technological/" + tRow.metaId() + ".png)");
		}

		JsonObject tEn = readJson("assets/gt6/lang/en_us.json");
		JsonObject tZh = readJson("assets/gt6/lang/zh_cn.json");
		for (GT6CircuitChain.ChainRow tRow : GT6CircuitChain.ROWS) {
			String tKey = "item.gt6." + tRow.path();
			assertEquals(tRow.enName(), tEn.get(tKey).getAsString(), "MIT:" + tRow.metaId() + " addItem name");
			assertEquals(tRow.enTooltip(), tEn.get(tKey + ".tooltip").getAsString(), "MIT:" + tRow.metaId() + " subtitle");
			assertEquals(tRow.zhName(), tZh.get(tKey).getAsString(), "dump " + tRow.metaId() + " zh name");
			assertEquals(tRow.zhTooltip(), tZh.get(tKey + ".tooltip").getAsString(), "dump " + tRow.metaId() + " zh tooltip");
		}
	}

	/**
	 * The tier fallback chain (the card's UNKNOWN ④ — the three-state declaration, state
	 * (b) amended at review 2026-10-05): (a) upstream INTENT = a full cascade (the re-reg
	 * chain LoaderOreDictReRegistrations.java:375-383 re-registers every
	 * {@code gt:circuitN} member onto {@code gt:circuitN-1}); (b) upstream ACTUAL = also
	 * the full cascade — the earlier "write-only map" archaeology missed the event path:
	 * 1.7.10 Forge OreDictionary.registerOre posts OreRegisterEvent on every
	 * non-duplicate registration (forge-1.7.10 OreDictionary.java:561) and OreDictManager
	 * .java:383-385 reads {@code mReRegistrationMappings} on each such event, so the
	 * ascending chain-add makes every rung's immediate copy (addReRegistration:212)
	 * transitively fall through the previously added mappings; (c) the PORT = the full
	 * monotone cascade — faithful to BOTH the intent and the actual: each carrier joins
	 * its own tag AND every lower-tier tag, i.e.
	 * {@code #gt6:circuitN = all carriers of tier >= N} (GT6ItemTags.addBatteryTags).
	 * Clipped at [0..6] — the [7..9] Quantum rungs are the p24 census CUT.
	 */
	@Test
	public void tierFallbackChainCascadesDownward() throws Exception {
		String[][] tExpected = { // per tag: circuit0..circuit6 member paths, high tiers cascade down
				{"circuit_primitive", "circuit_basic", "circuit_good", "circuit_advanced", "circuit_elite", "circuit_master", "circuit_ultimate"},
				{"circuit_basic", "circuit_good", "circuit_advanced", "circuit_elite", "circuit_master", "circuit_ultimate"},
				{"circuit_good", "circuit_advanced", "circuit_elite", "circuit_master", "circuit_ultimate"},
				{"circuit_advanced", "circuit_elite", "circuit_master", "circuit_ultimate"},
				{"circuit_elite", "circuit_master", "circuit_ultimate"},
				{"circuit_master", "circuit_ultimate"},
				{"circuit_ultimate"}};
		// both generated-tree legs (forge tags/items, 1.21.1 tags/item — the same provider)
		for (String tDir : new String[] {"tags/item", "tags/items"}) {
			for (int tTag = 0; tTag <= 6; tTag++) {
				JsonObject tFile = readJson("data/gt6/" + tDir + "/circuit" + tTag + ".json");
				Set<String> tValues = new LinkedHashSet<>();
				for (var tElement : tFile.getAsJsonArray("values")) {
					tValues.add(tElement.getAsString());
				}
				Set<String> tExpectedIds = new LinkedHashSet<>();
				for (String tPath : tExpected[tTag]) tExpectedIds.add("gt6:" + tPath);
				assertEquals(tExpectedIds, tValues, tDir + "/circuit" + tTag + ": the cascade face of the upstream re-reg chain");
			}
		}
	}

	private static JsonObject readJson(String aResource) throws Exception {
		InputStream tStream = GT6CircuitChainRegistrationTest.class.getClassLoader().getResourceAsStream(aResource);
		assertTrue(tStream != null, "the resource " + aResource + " must ship in the generated tree");
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static boolean classpathHas(String aResource) {
		return GT6CircuitChainRegistrationTest.class.getClassLoader().getResourceAsStream(aResource) != null;
	}
}
