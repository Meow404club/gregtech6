/*
 * Offline tests for task explosives-chain: the GT6Explosives dynamite trio + the
 * GT6PressMolds bullet-casing trio registration homes (the GT6ExtruderMoldsTest posture
 * over the pure table + registry-wiring data).
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the registry-wiring data
 * ({@code DeferredRegister.getEntries()} / {@code RegistryObject.getId()}), the tint seam
 * over the initialized material system (GTMaterialItems.initMaterials — the B1 pour-test
 * boot face), and the datagen JSON existence + verbatim values (the crafting rows, the
 * models, both lang faces — read off the classpath, working-directory independent).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

public class GT6ExplosivesRegistrationTest {

	/** The offline boot BEFORE the first registry-home touch (the GT6ExtruderMoldsTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.registry.GTMaterialItems.initMaterials(); // the tint seam resolves MT.Orange/Red/Purple offline
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/** Both DRs target the vanilla item registry (the GT6ExtruderMolds shape). */
	@Test
	public void registryKeysAreTheVanillaItemRegistry() {
		assertEquals(Registries.ITEM, GT6Explosives.ITEMS.getRegistryKey());
		assertEquals(Registries.ITEM, GT6PressMolds.ITEMS.getRegistryKey());
	}

	/**
	 * The dynamite trio in upstream meta order (Loader_MultiTileEntities.java:2236 Boomstick
	 * 32104 MT.Orange, :2237 Dynamite 32713 MT.Red, :2238 Strong Dynamite 32712 MT.Purple),
	 * ids the snake of the IL field names (IL.java:469).
	 */
	@Test
	public void dynamiteRowsAreTheUpstreamTrio() {
		assertEquals(3, GT6Explosives.ROWS.size());
		assertEquals(rl("boomstick"), GT6Explosives.ITEMS_BY_PATH.get("boomstick").getId(), "meta 32104");
		assertEquals(rl("dynamite"), GT6Explosives.ITEMS_BY_PATH.get("dynamite").getId(), "meta 32713");
		assertEquals(rl("dynamite_strong"), GT6Explosives.ITEMS_BY_PATH.get("dynamite_strong").getId(), "meta 32712");
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		GT6Explosives.ITEMS.getEntries().forEach(tEntry -> tIds.add(tEntry.getKey().location()));
		assertEquals(Set.of(rl("boomstick"), rl("dynamite"), rl("dynamite_strong")), tIds,
				"the register carries exactly the dynamite trio");
	}

	/**
	 * The press-mold trio in upstream meta order (MultiItemTechnological.java:352-354, metas
	 * 10896-10898), ids the snake of the IL field names (IL.java:232).
	 */
	@Test
	public void pressMoldRowsAreTheUpstreamTrio() {
		assertEquals(3, GT6PressMolds.MOLDS.size());
		assertEquals(rl("shape_press_bullet_casing_small"), GT6PressMolds.SHAPE_PRESS_BULLET_CASING_SMALL.getId(), "meta 10896");
		assertEquals(rl("shape_press_bullet_casing_medium"), GT6PressMolds.SHAPE_PRESS_BULLET_CASING_MEDIUM.getId(), "meta 10897");
		assertEquals(rl("shape_press_bullet_casing_large"), GT6PressMolds.SHAPE_PRESS_BULLET_CASING_LARGE.getId(), "meta 10898");
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		GT6PressMolds.ITEMS.getEntries().forEach(tEntry -> tIds.add(tEntry.getKey().location()));
		assertEquals(Set.of(rl("shape_press_bullet_casing_small"), rl("shape_press_bullet_casing_medium"),
				rl("shape_press_bullet_casing_large")), tIds, "the register carries exactly the mold trio");
	}

	/**
	 * The tint seam answers the upstream :2236-2238 material assignment (Orange 255/128/0,
	 * Red 255/0/0, Purple 128/0/128) on tint index 0 and stays inert elsewhere (the overlay
	 * layer and unknown paths render as-is).
	 */
	@Test
	public void tintSeamAnswersTheUpstreamMaterialColours() {
		assertEquals(0xFFFF8000, GT6Explosives.tintARGBByPath("boomstick", 0), "MT.Orange mRGBaSolid");
		assertEquals(0xFFFF0000, GT6Explosives.tintARGBByPath("dynamite", 0), "MT.Red mRGBaSolid");
		assertEquals(0xFF800080, GT6Explosives.tintARGBByPath("dynamite_strong", 0), "MT.Purple mRGBaSolid");
		assertEquals(-1, GT6Explosives.tintARGBByPath("boomstick", 1), "the overlay layer renders as-is");
		assertEquals(-1, GT6Explosives.tintARGBByPath("not_a_dynamite", 0), "unknown paths stay un-tinted");
		assertEquals(-1, GT6Explosives.tintARGBByPath(null, 0));
	}

	/**
	 * The datagen JSON existence: the three crafting rows + the six item models + both lang
	 * faces ship in the generated tree; the crafting stroke is the upstream :356-358 verbatim
	 * ("TPT"/"dyh"/"SPS", the steel representative keys + the tool keys) and the lang values
	 * are the upstream registration wordings (the :2236-2238 aRegistry.add names and the
	 * :352-354 MIT names, en; the tmp/gregtech.lang dump rows, zh).
	 */
	@Test
	public void datagenFacesShipUpstreamVerbatim() throws Exception {
		for (String tMold : new String[] {"small", "medium", "large"}) {
			JsonObject tCraft = readJson("data/gt6/recipe/shape_press_bullet_casing_" + tMold + ".json");
			assertEquals(3, tCraft.getAsJsonArray("pattern").size(), "the :356-358 stroke is three rows");
			assertEquals("TPT", tCraft.getAsJsonArray("pattern").get(0).getAsString());
			assertEquals("dyh", tCraft.getAsJsonArray("pattern").get(1).getAsString());
			assertEquals("SPS", tCraft.getAsJsonArray("pattern").get(2).getAsString());
			JsonObject tKey = tCraft.getAsJsonObject("key");
			assertEquals("gt6:screw_steel", tKey.getAsJsonObject("T").get("item").getAsString(),
					"the MT.Steel representative of the upstream ANY.Steel screw frame");
			assertEquals("gt6:stick_steel", tKey.getAsJsonObject("S").get("item").getAsString());
			assertEquals("gt6:tools/screwdriver", tKey.getAsJsonObject("d").get("tag").getAsString(), "the CR.java:342 letter");
			assertEquals("gt6:chisel", tKey.getAsJsonObject("y").get("item").getAsString(), "the CR.java:360 letter (no tools/chisel tag at baseline)");
			assertEquals("gt6:tools/hard_hammer", tKey.getAsJsonObject("h").get("tag").getAsString(), "the CR.java:346 letter");
		}
		assertEquals("gt6:plate_double_steel", readJson("data/gt6/recipe/shape_press_bullet_casing_small.json")
				.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(), ":356 plateDouble");
		assertEquals("gt6:plate_triple_steel", readJson("data/gt6/recipe/shape_press_bullet_casing_medium.json")
				.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(), ":357 plateTriple");
		assertEquals("gt6:plate_quadruple_steel", readJson("data/gt6/recipe/shape_press_bullet_casing_large.json")
				.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(), ":358 plateQuadruple");

		for (String tMold : new String[] {"small", "medium", "large"}) {
			assertTrue(classpathHas("assets/gt6/models/item/shape_press_bullet_casing_" + tMold + ".json"),
					"the press-mold item model must ship");
		}
		for (String tDynamite : new String[] {"boomstick", "dynamite", "dynamite_strong"}) {
			JsonObject tModel = readJson("assets/gt6/models/item/" + tDynamite + ".json");
			assertTrue(tModel.getAsJsonObject("textures").has("layer0") && tModel.getAsJsonObject("textures").has("layer1"),
					"the dynamite model carries the body + overlay layers (" + tDynamite + ")");
		}

		JsonObject tEn = readJson("assets/gt6/lang/en_us.json");
		assertEquals("Boomstick", tEn.get("item.gt6.boomstick").getAsString(), ":2236 aRegistry.add name");
		assertEquals("Dynamite", tEn.get("item.gt6.dynamite").getAsString(), ":2237 aRegistry.add name");
		assertEquals("Strong Dynamite", tEn.get("item.gt6.dynamite_strong").getAsString(), ":2238 aRegistry.add name");
		assertEquals("Bullet Casing Mold (Small)", tEn.get("item.gt6.shape_press_bullet_casing_small").getAsString(), "MIT:352");
		assertEquals("Bullet Casing Mold (Medium)", tEn.get("item.gt6.shape_press_bullet_casing_medium").getAsString(), "MIT:353");
		assertEquals("Bullet Casing Mold (Large)", tEn.get("item.gt6.shape_press_bullet_casing_large").getAsString(), "MIT:354");

		JsonObject tZh = readJson("assets/gt6/lang/zh_cn.json");
		assertEquals("\u706b\u836f\u68d2", tZh.get("item.gt6.boomstick").getAsString(), "dump gt.multitileentity.32104 (:13125)");
		assertEquals("\u952f\u672b\u70b8\u836f", tZh.get("item.gt6.dynamite").getAsString(), "dump gt.multitileentity.32713 (:13534)");
		assertEquals("\u5f3a\u5316\u77ff\u7528\u96f7\u7ba1", tZh.get("item.gt6.dynamite_strong").getAsString(), "dump gt.multitileentity.32712 (:13533)");
		assertEquals("\u5f39\u58f3\u6a21\u5177 (\u5c0f\u578b)", tZh.get("item.gt6.shape_press_bullet_casing_small").getAsString(), "dump 10896 (:10337)");
		assertEquals("\u5f39\u58f3\u6a21\u5177 (\u4e2d\u578b)", tZh.get("item.gt6.shape_press_bullet_casing_medium").getAsString(), "dump 10897 (:10339)");
		assertEquals("\u5f39\u58f3\u6a21\u5177 (\u5927\u578b)", tZh.get("item.gt6.shape_press_bullet_casing_large").getAsString(), "dump 10898 (:10341)");
	}

	private static JsonObject readJson(String aResource) throws Exception {
		InputStream tStream = GT6ExplosivesRegistrationTest.class.getClassLoader().getResourceAsStream(aResource);
		assertTrue(tStream != null, "the resource " + aResource + " must ship in the generated tree");
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static boolean classpathHas(String aResource) {
		return GT6ExplosivesRegistrationTest.class.getClassLoader().getResourceAsStream(aResource) != null;
	}
}
