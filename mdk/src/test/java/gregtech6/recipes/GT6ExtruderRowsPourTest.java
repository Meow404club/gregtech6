package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Task toolhead-r11c-extruder-heads — the extruder tool-head row pour test: the shipped
 * {@code data/gt6/recipe_maps/extruder.json} pours through the real
 * {@link GT6RecipeMapJsonLoader} seam into {@code GT6RecipeMaps.EXTRUDER}. The census
 * (52 representative rows over the 8 walk families) and the item-id faces are the
 * acceptance.
 *
 * <p><b>Signature groups</b> (the offline stand-in resolver erases item identity, so the
 * per-family pins partition on the (EUt, duration, count) signature, which is disjoint):
 * the 96-EUt forging rows are metal-Shape-only; the 32-t rows are stone-family-only;
 * the remaining 16-EUt units column is SHARED by the metal SimpleEx mirror and the
 * Blackstone rows (the upstream Blackstone durations follow the same units formula) —
 * those two families pin as one merged tally, the per-row attribution lives in the
 * JSON row comments.
 *
 * <p><b>HERMETIC (the r11e lesson)</b>: the neo junit FML boot statically pours the
 * GT6Recipes* suite into the boot generation, and a bare {@code init()} would no-op
 * onto it — the census would eat the boot residue. So {@code reset()} retires the
 * generation BEFORE {@code init()} builds the fresh one, every test.
 */
public class GT6ExtruderRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The item ids the loader asked for during the pour, "namespace:path" (the id-face assertion set). */
	private final Set<String> mRequestedItems = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // hermetic: retire the boot generation (and its residue) FIRST
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		mRequestedItems.clear();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			mRequestedItems.add(aId.getNamespace() + ":" + aId.getPath());
			// identity stand-ins per namespace — the mechanics compare shapes only
			return "gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK;
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped true-row file verbatim and pours it under its map key. */
	private void pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/extruder.json";
		try (InputStream tStream = GT6ExtruderRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "extruder"), JsonParser.parseString(tJson)));
		}
	}

	/**
	 * The census: 52 representative rows — 8 metal Shape_Extruder (Handlers:750-762 head
	 * subset) + 8 metal Shape_SimpleEx (:783-795 head subset) + 6+6 stone stonetypes
	 * (RM.java:410-417 + the :423-430 SimpleEx mirror) + 6+6 BlockStones (:293-300 +
	 * :308-315) + 6+6 Blackstone (Loader_Recipes_Extruder:51-58 + :66-73).
	 */
	@Test
	public void theShippedStockPoursTheFullRepresentativeCensus() throws Exception {
		pourShipped();
		assertEquals(52, GT6RecipeMaps.EXTRUDER.mRecipeList.size(),
				"8 metal Shape + 8 metal SimpleEx + 6+6 stone + 6+6 BlockStones + 6+6 Blackstone");
		assertEquals(52, GT6RecipeMapJsonLoader.pouredCount("extruder"), "the loader keyed the pour under the extruder map key");
	}

	/**
	 * The metal Shape_Extruder forging column (Handlers:750-762): 96 EUt at the Iron
	 * representative's melting-point duration (RecipeMapHandlerPrefixForging.getCosts =
	 * max(16, 1 + |dT * weight| / (75 * 96)), dT = 1811-293, Iron 7.874 g/cm3): 1 ingot
	 * -> 185 t (shovel), 2 -> 369 t (sword/hoe/saw + the 1.5U file), 3 -> 554 t
	 * (pickaxe/axe), 6 -> 1107 t (hammer), one head out each.
	 */
	@Test
	public void theForgingRowsCarryTheIronMeltingPointColumns() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		for (Recipe tRow : GT6RecipeMaps.EXTRUDER.mRecipeList) {
			if (tRow.mEUt != 96L) continue;
			tTally.merge(tRow.mEUt + "EU " + tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		assertEquals(Map.of(
				"96EU 185t x1", 1,   // :750 raw shovel
				"96EU 369t x1", 4,   // :756/:757/:758 sword/hoe/saw + :761 file (2 ingots in, 1.5U head)
				"96EU 554t x1", 2,   // :759/:760 pickaxe/axe
				"96EU 1107t x1", 1), // :762 hammer
				tTally, "the forging column over the 8 metal Shape rows");
	}

	/**
	 * The stone families (RM.java stonetypes + BlockStones): 16 EUt / 32 t everywhere,
	 * the block + mold -> head-count column (9 shovel / 4 sword / 4 hoe / 3 pickaxe /
	 * 3 axe / 1 hammer) over the two block representatives x the Shape/SimpleEx twins.
	 */
	@Test
	public void theStoneFamiliesCarryTheUpstreamColumns() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		for (Recipe tRow : GT6RecipeMaps.EXTRUDER.mRecipeList) {
			if (tRow.mDuration != 32L) continue; // 32 t is stone-family-only (the metal and Blackstone columns run 64+)
			tTally.merge(tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		assertEquals(Map.of(
				"32t x9", 4,   // RM.java:410/:423 + BlockStones:293/:308 — shovel, stone + granite reps
				"32t x4", 8,   // sword + hoe, both reps x both twins
				"32t x3", 8,   // pickaxe + axe, both reps x both twins
				"32t x1", 4),  // RM.java:417/:430 + BlockStones:300/:315 — hammer
				tTally, "the 32-t column over the 24 stone rows");
	}

	/**
	 * The shared 16-EUt units column (64 t per output-U): the metal SimpleEx mirror
	 * (Handlers:783-795: 1/2/2/2/3/3/2/6 ingots -> 64/128/128/128/192/192/96/384 t) plus
	 * the Blackstone rows (Loader_Recipes_Extruder:51-58 + :66-73: the same 64/128/128/
	 * 192/192/384 ladder, count-1 heads, both mold families) — 20 rows merged (see the
	 * class doc).
	 */
	@Test
	public void theUnitsColumnCoversTheSimpleExMirrorAndTheBlackstoneRows() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		for (Recipe tRow : GT6RecipeMaps.EXTRUDER.mRecipeList) {
			if (tRow.mEUt != 16L || tRow.mDuration == 32L) continue;
			tTally.merge(tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		assertEquals(Map.of(
				"64t x1", 3,    // :783 SimpleEx shovel + :51/:66 Blackstone shovel twins
				"96t x1", 1,    // :794 SimpleEx file (1.5U)
				"128t x1", 7,   // :789/:790/:791 sword/hoe/saw + :52-53/:67-68 Blackstone sword/hoe
				"192t x1", 6,   // :792/:793 pickaxe/axe + :54-55/:69-70 Blackstone pickaxe/axe
				"384t x1", 3),  // :795 hammer + :58/:73 Blackstone hammer twins
				tTally, "the units column over the 20 merged rows");
	}

	/**
	 * The id faces: the mold slots, the Iron metal walk, the stone representatives and
	 * their head sets.
	 */
	@Test
	public void theIdFacesCoverTheMoldsAndTheRepresentatives() throws Exception {
		pourShipped();
		for (String tMold : new String[] {"gt6:shape_extruder_shovel", "gt6:shape_extruder_sword", "gt6:shape_extruder_hoe",
				"gt6:shape_extruder_saw", "gt6:shape_extruder_pickaxe", "gt6:shape_extruder_axe", "gt6:shape_extruder_file",
				"gt6:shape_extruder_hammer", "gt6:shape_simple_ex_shovel", "gt6:shape_simple_ex_sword", "gt6:shape_simple_ex_hoe",
				"gt6:shape_simple_ex_saw", "gt6:shape_simple_ex_pickaxe", "gt6:shape_simple_ex_axe", "gt6:shape_simple_ex_file",
				"gt6:shape_simple_ex_hammer"}) {
			assertTrue(mRequestedItems.contains(tMold), "the mold slot face: " + tMold);
		}
		for (String tMetal : new String[] {"gt6:ingot_iron", "gt6:tool_head_raw_shovel_iron", "gt6:tool_head_raw_sword_iron",
				"gt6:tool_head_raw_hoe_iron", "gt6:tool_head_raw_saw_iron", "gt6:tool_head_raw_pickaxe_iron",
				"gt6:tool_head_raw_axe_iron", "gt6:tool_head_file_iron", "gt6:tool_head_hammer_iron"}) {
			assertTrue(mRequestedItems.contains(tMetal), "the metal walk face: " + tMetal);
		}
		for (String tStone : new String[] {"minecraft:stone", "gt6:tool_head_raw_shovel_stone", "gt6:tool_head_hammer_stone",
				"gt6:granite", "gt6:tool_head_raw_sword_granite", "minecraft:blackstone", "gt6:tool_head_raw_pickaxe_blackstone",
				"gt6:tool_head_hammer_blackstone"}) {
			assertTrue(mRequestedItems.contains(tStone), "the stone representative face: " + tStone);
		}
	}
}
