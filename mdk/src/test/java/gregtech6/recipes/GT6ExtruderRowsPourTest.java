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
 * (the r11c 52 representative rows + the task recipe-b5-extruder-statics append of 175
 * rows = 227) and the item-id faces are the acceptance. The per-family tallies below
 * partition the FULL shipped stock on the (EUt, duration, count) signatures and carry
 * their b5 additions inline (the b5 card owns the row-level decomposition pins in
 * GT6RecipeMapDataB5ExtruderStaticsRowsPourTest).
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
	 * The census: 227 rows — the r11c 52 representative rows (8 metal Shape_Extruder +
	 * 8 metal Shape_SimpleEx + 6+6 stone stonetypes + 6+6 BlockStones + 6+6 Blackstone)
	 * plus the task recipe-b5-extruder-statics append of 175 rows (138 module-domain
	 * literals + 9 walk representatives + 28 Handlers forging representatives).
	 */
	@Test
	public void theShippedStockPoursTheFullRepresentativeCensus() throws Exception {
		pourShipped();
		assertEquals(227, GT6RecipeMaps.EXTRUDER.mRecipeList.size(),
				"the r11c 52 + the b5 175 (138 module literals + 9 walk + 28 Handlers)");
		assertEquals(227, GT6RecipeMapJsonLoader.pouredCount("extruder"), "the loader keyed the pour under the extruder map key");
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
		Map<String, Integer> tExpected = new java.util.TreeMap<>();
		tExpected.put("96EU 185t x1", 6);   // :750 shovel + b5 Handlers:741/:742/:743/:745/:764 (the 1-in 1-out statements)
		tExpected.put("96EU 185t x2", 2);   // b5 :744 rod / :749 casing
		tExpected.put("96EU 185t x4", 2);   // b5 :747 ring / :768 foil
		tExpected.put("96EU 185t x8", 2);   // b5 :746 bolt / :769 wireFine
		tExpected.put("96EU 185t x9", 1);   // b5 :767 plateTiny
		tExpected.put("96EU 369t x1", 4);   // :756/:757/:758 sword/hoe/saw + :761 file
		tExpected.put("96EU 554t x1", 2);   // :759/:760 pickaxe/axe
		tExpected.put("96EU 738t x1", 1);   // b5 :763 gear (4 ingots in)
		tExpected.put("96EU 1107t x1", 1);  // :762 hammer
		tExpected.put("96EU 1661t x1", 1);  // b5 :765 block (9 ingots in)
		tExpected.put("96EU 216t x1", 1);   // b5 walk :292 Zr cell (the melting-point column)
		assertEquals(tExpected, tTally, "the forging column over the 23 96-EUt rows");
	}

	/**
	 * The stone families (RM.java stonetypes + BlockStones): 16 EUt / 32 t everywhere,
	 * the block + mold -> head-count column (9 shovel / 4 sword / 4 hoe / 3 pickaxe /
	 * 3 axe / 1 hammer) over the two block representatives x the Shape/SimpleEx twins,
	 * plus the b5 32-t rows (the module-domain rod pairs x3 domains, the W-meta
	 * stone/cobblestone 7-statement families, the x2-out W-meta rods).
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
				"32t x2", 4,   // b5 W-meta rods (:139/:140 + :170/:171, both domains x both twins)
				"32t x1", 38), // RM.java:417/:430 + BlockStones:300/:315 hammers (4) + the b5 x1 columns
				tTally, "the 32-t column over the 62 stone rows");
	}

	/**
	 * The shared 16-EUt units column (64 t per output-U): the metal SimpleEx mirror
	 * (Handlers:783-795: 1/2/2/2/3/3/2/6 ingots -> 64/128/128/128/192/192/96/384 t) plus
	 * the Blackstone rows (Loader_Recipes_Extruder:51-58 + :66-73: the same 64/128/128/
	 * 192/192/384 ladder, count-1 heads, both mold families) — 20 r11c rows merged — plus
	 * the b5 rows: the module domains (18+30+30), the W-meta stone/cobblestone 14 per
	 * domain, the 8 walk rows, the 14 Handlers SimpleEx mirrors (see the class doc).
	 */
	@Test
	public void theUnitsColumnCoversTheSimpleExMirrorAndTheBlackstoneRows() throws Exception {
		pourShipped();
		Map<String, Integer> tTally = new java.util.TreeMap<>();
		for (Recipe tRow : GT6RecipeMaps.EXTRUDER.mRecipeList) {
			if (tRow.mEUt != 16L || tRow.mDuration == 32L) continue;
			tTally.merge(tRow.mDuration + "t x" + tRow.mOutputs[0].getCount(), 1, Integer::sum);
		}
		Map<String, Integer> tExpected = new java.util.TreeMap<>();
		tExpected.put("8t x8", 4);     // b5 W-meta bolts (:141/:172 x both twins)
		tExpected.put("16t x1", 6);    // b5 module bolts (:48/:63 + :79/:94 + :110/:125)
		tExpected.put("64t x1", 62);   // the r11c shovels (:783 + :51/:66) + the b5 module/walk/Handlers x1 columns
		tExpected.put("64t x2", 2);    // b5 :777 rod / :782 casing mirrors
		tExpected.put("64t x3", 2);    // b5 walk :254/:255 glass cells (chemtube x3)
		tExpected.put("64t x4", 2);    // b5 :780 ring / :801 foil mirrors
		tExpected.put("64t x8", 2);    // b5 :779 bolt / :802 wireFine mirrors
		tExpected.put("64t x9", 1);    // b5 :800 plateTiny mirror
		tExpected.put("96t x1", 9);    // r11c :794 file + the b5 W-meta pickaxe/axe pairs
		tExpected.put("128t x1", 19);  // r11c sword/hoe/saw column + the b5 module sword/hoe pairs
		tExpected.put("192t x1", 18);  // r11c pickaxe/axe column + the b5 module pickaxe/axe + W-meta hammers
		tExpected.put("256t x1", 7);   // b5 module gears + the Handlers:796 gear mirror
		tExpected.put("384t x1", 7);   // r11c hammer column + the b5 module hammers
		tExpected.put("576t x1", 1);   // b5 Handlers:798 block mirror (9U in)
		assertEquals(tExpected, tTally, "the units column over the 142 merged rows");
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
