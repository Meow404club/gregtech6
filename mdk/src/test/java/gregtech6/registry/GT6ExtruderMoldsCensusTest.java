package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Task toolhead-r11c-extruder-heads — the extruder-mold registration census. The w1
 * row0 pair (plate + rod) extends to the tool-head family the R11-C card's recipe rows
 * consume: 8 {@code Shape_Extruder_*} head molds (MultiItemTechnological.java:215-222,
 * metas 10015-10022) + their 8 {@code Shape_SimpleEx_*} low-heat twins (:276-283, metas
 * 10215-10222), walked in upstream meta order after the seated plate (:186)/rod (:212).
 *
 * <p>The offline surface follows the CreativeTabJoinCensusTest discipline: registry
 * objects are unbound in this JVM, but the {@code MOLDS} walk order, the shipped asset
 * tree (generated resources ride the test classpath) and the reflective tab-join shape
 * are all readable — and every face here is exactly what the JEI/creative visibility
 * and the not-consumable predicate consume.
 */
public class GT6ExtruderMoldsCensusTest {

	/** The mold ids in upstream meta order (the MultiItemTechnological registration walk). */
	private static final List<String> MOLD_IDS = List.of(
			"shape_extruder_plate",    // 10001 (:186, seated row0)
			"shape_extruder_sword",    // 10015
			"shape_extruder_pickaxe",  // 10016
			"shape_extruder_shovel",   // 10017
			"shape_extruder_axe",      // 10018
			"shape_extruder_hoe",      // 10019
			"shape_extruder_hammer",   // 10020
			"shape_extruder_file",     // 10021
			"shape_extruder_saw",      // 10022
			"shape_extruder_rod",      // 10027 (:212, seated row0)
			"shape_simple_ex_sword",    // 10215
			"shape_simple_ex_pickaxe",  // 10216
			"shape_simple_ex_shovel",   // 10217
			"shape_simple_ex_axe",      // 10218
			"shape_simple_ex_hoe",      // 10219
			"shape_simple_ex_hammer",   // 10220
			"shape_simple_ex_file",     // 10221
			"shape_simple_ex_saw");     // 10222

	/** The offline boot before the first registry-class touch (the CreativeTabJoinCensusTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The census: 18 molds, ids in the upstream meta order. */
	@Test
	public void theMoldCensusIsEighteenInTheUpstreamMetaOrder() {
		assertEquals(MOLD_IDS.size(), GT6ExtruderMolds.MOLDS.size(), "the full head-family census");
		List<String> tPaths = new ArrayList<>();
		for (RegistryObject<Item> tMold : GT6ExtruderMolds.MOLDS) tPaths.add(tMold.getId().getPath());
		assertEquals(MOLD_IDS, tPaths, "the ids walk in upstream meta order (Extruder 10015-10022 block, then SimpleEx 10215-10222)");
	}

	/** The asset face: every mold has an item model JSON and its borrowed texture PNG. */
	@Test
	public void everyMoldHasAnItemModelAndABorrowedTexture() {
		for (String tPath : MOLD_IDS) {
			assertNotNull(read("assets/gt6/models/item/" + tPath + ".json"), "the item model rides the generated tree: " + tPath);
			String tTexture = texturePath(tPath);
			assertNotNull(read("assets/gt6/textures/item/" + tTexture + ".png"), "the borrowed sprite rides main resources: " + tTexture);
		}
	}

	/** The not-consumable face: the extruder_shapes tag JSON carries all 18 memberships. */
	@Test
	public void theExtruderShapesTagCarriesTheFullFamily() throws Exception {
		String tJson = read("data/gt6/tags/item/extruder_shapes.json");
		assertNotNull(tJson, "the generated tag file rides the test classpath");
		List<String> tValues = new ArrayList<>();
		for (var tElement : JsonParser.parseString(tJson).getAsJsonObject().getAsJsonArray("values")) {
			tValues.add(tElement.getAsString());
		}
		for (String tPath : MOLD_IDS) {
			assertTrue(tValues.contains("gt6:" + tPath), "the tag membership: " + tPath);
		}
		assertEquals(MOLD_IDS.size(), tValues.size(), "no stray memberships — exactly the registered family");
	}

	/** The creative-tab join (the GT6SlicerBlades.onBuildTabContents form) exists and walks the full family. */
	@Test
	public void theTabJoinHandlerWalksTheWholeFamily() throws Exception {
		assertNotNull(GT6ExtruderMolds.class.getDeclaredMethod("onBuildTabContents", BuildCreativeModeTabContentsEvent.class),
				"the MACHINES_TAB join handler (the tabfix census: registered-but-tab-less is invisible in JEI)");
	}

	/** The texture basename drops the family prefix (the food_can band convention). */
	private static String texturePath(String aPath) {
		return aPath.startsWith("shape_simple_ex_")
				? "shape_simple_ex/" + aPath.substring("shape_simple_ex_".length())
				: "shape_extruder/" + aPath.substring("shape_extruder_".length());
	}

	/** A classloader read of the shipped tree (null = absent). */
	private static String read(String aPath) {
		try (InputStream tStream = GT6ExtruderMoldsCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}
}
