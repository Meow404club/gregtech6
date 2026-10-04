/*
 * Offline tests for task press-electrodes: the GT6Electrodes registration home — the
 * Forestry electrode thirteen (MultiItemTechnological.java:488-500, metas 29987-29999),
 * the press-row unlock subset the RM.Press electrode band (:502-543) consumes.
 *
 * <p>The GT6ExplosivesRegistrationTest posture: the items are NOT constructible in this
 * bootstrapped-and-frozen JVM (the mod-Item intrusive-holder wall), so the assertion
 * surface is the registry-wiring data ({@code DeferredRegister.getEntries()} /
 * {@code RegistryObject.getId()}), the datagen JSON existence + verbatim values (the
 * models, both lang faces), and the borrowed sprite files — read off the classpath,
 * working-directory independent.
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

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

public class GT6ElectrodesRegistrationTest {

	/** The upstream meta order (MIT:488 Copper .. :500 Ender) — the ids snake of the IL fields. */
	private static final String[] PATHS = {
			"electrode_fr_copper", "electrode_fr_tin", "electrode_fr_bronze", "electrode_fr_iron",
			"electrode_fr_gold", "electrode_fr_diamond", "electrode_fr_obsidian", "electrode_fr_blaze",
			"electrode_fr_rubber", "electrode_fr_emerald", "electrode_fr_apatite", "electrode_fr_lapis",
			"electrode_fr_ender"};

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/** Both DRs target the vanilla item registry (the GT6Explosives shape). */
	@Test
	public void registryKeyIsTheVanillaItemRegistry() {
		assertEquals(Registries.ITEM, GT6Electrodes.ITEMS.getRegistryKey());
	}

	/**
	 * The thirteen electrodes in upstream meta order (MultiItemTechnological.java:488-500),
	 * ids the snake of the IL field names (IL.java:472-484, the GT6FoodCans ruling).
	 */
	@Test
	public void electrodeRowsAreTheUpstreamThirteen() {
		assertEquals(PATHS.length, GT6Electrodes.ROWS.size(), "MultiItemTechnological.java:488-500 registers exactly thirteen");
		for (int i = 0; i < PATHS.length; i++) {
			assertEquals(rl(PATHS[i]), GT6Electrodes.ROWS.get(i).item().getId(), "row " + i + " rides the upstream meta " + (29987 + i) + " order");
		}
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		GT6Electrodes.ITEMS.getEntries().forEach(tEntry -> tIds.add(tEntry.getKey().location()));
		Set<ResourceLocation> tExpected = new LinkedHashSet<>();
		for (String tPath : PATHS) tExpected.add(rl(tPath));
		assertEquals(tExpected, tIds, "the register carries exactly the electrode thirteen");
	}

	/**
	 * The datagen JSON existence + verbatim values: the thirteen item models (single layer0,
	 * the plain-item form — no tint seam) + both lang faces with the upstream registration
	 * wordings (the MIT:488-500 addItem names + the "Needs Glass Tube" subtitle; the
	 * tmp/gregtech.lang dump rows 10579-10604, zh 电子管元件 + 需要玻璃外壳 tooltip).
	 */
	@Test
	public void datagenFacesShipUpstreamVerbatim() throws Exception {
		String[] tEnNames = {"Electrode (Copper)", "Electrode (Tin)", "Electrode (Bronze)", "Electrode (Iron)",
				"Electrode (Gold)", "Electrode (Diamond)", "Electrode (Obsidian)", "Electrode (Blaze)",
				"Electrode (Rubber)", "Electrode (Emerald)", "Electrode (Apatite)", "Electrode (Lapis)",
				"Electrode (Ender)"};
		String[] tZhNames = {"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u94dc)", "\u7535\u5b50\u7ba1\u5143\u4ef6 (\u9521)",
				"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u9752\u94dc)", "\u7535\u5b50\u7ba1\u5143\u4ef6 (\u94c1)",
				"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u91d1)", "\u7535\u5b50\u7ba1\u5143\u4ef6 (\u94bb\u77f3)",
				"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u9ed1\u66dc\u77f3)", "\u7535\u5b50\u7ba1\u5143\u4ef6 (\u70c8\u7130)",
				"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u6a61\u80f6)", "\u7535\u5b50\u7ba1\u5143\u4ef6 (\u7eff\u5b9d\u77f3)",
				"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u78f7\u7070\u77f3)", "\u7535\u5b50\u7ba1\u5143\u4ef6 (\u9752\u91d1\u77f3)",
				"\u7535\u5b50\u7ba1\u5143\u4ef6 (\u672b\u5f71)"};
		String tZhTooltip = "\u9700\u8981\u73bb\u7483\u5916\u58f3";
		for (int i = 0; i < PATHS.length; i++) {
			JsonObject tModel = readJson("assets/gt6/models/item/" + PATHS[i] + ".json");
			JsonObject tTextures = tModel.getAsJsonObject("textures");
			assertTrue(tTextures.has("layer0"), "the electrode model carries the body layer (" + PATHS[i] + ")");
			assertTrue(classpathHas("assets/gt6/textures/item/electrode/" + PATHS[i] + ".png"),
					"the borrowed upstream sprite must ship (gt.multiitem.technological/" + (29987 + i) + ".png)");
		}

		JsonObject tEn = readJson("assets/gt6/lang/en_us.json");
		JsonObject tZh = readJson("assets/gt6/lang/zh_cn.json");
		for (int i = 0; i < PATHS.length; i++) {
			assertEquals(tEnNames[i], tEn.get("item.gt6." + PATHS[i]).getAsString(), "MIT:" + (488 + i) + " addItem name");
			assertEquals("Needs Glass Tube", tEn.get("item.gt6." + PATHS[i] + ".tooltip").getAsString(),
					"MIT:" + (488 + i) + " subtitle");
			assertEquals(tZhNames[i], tZh.get("item.gt6." + PATHS[i]).getAsString(), "dump 299" + (87 + i) + " zh name");
			assertEquals(tZhTooltip, tZh.get("item.gt6." + PATHS[i] + ".tooltip").getAsString(), "dump zh tooltip");
		}
	}

	private static JsonObject readJson(String aResource) throws Exception {
		InputStream tStream = GT6ElectrodesRegistrationTest.class.getClassLoader().getResourceAsStream(aResource);
		assertTrue(tStream != null, "the resource " + aResource + " must ship in the generated tree");
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static boolean classpathHas(String aResource) {
		return GT6ElectrodesRegistrationTest.class.getClassLoader().getResourceAsStream(aResource) != null;
	}
}
