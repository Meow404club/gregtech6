/**
 * Offline guard tests for task r6-29-34a (GitHub #29a): the per-map category icon table —
 * every VISIBLE map carries a tabled machine item unless it is on the four-map
 * DECLARED-empty furnace-fallback whitelist (the guard the card's "零兜底或仅白名单兜底"
 * clause turns into a census pin), the whitelist is closed against the live census, and
 * the icon resolution never returns an empty stack. The Forge item registry does not
 * exist offline, so resolution rides the {@code sResolver} fixture seam (the
 * GT6RecipeMapJsonLoader.sItemResolver convention); the title-key formula the table's
 * consumers share is pinned here too.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.RecipeMap;

class GT6RecipeMapIconsTest extends GTRecipesOfflineTestBase {

	private static final net.minecraft.world.item.Item STUB_ITEM = Items.IRON_INGOT;

	@BeforeAll
	static void bootMaterials() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		GT6RecipeMapIcons.sResolver = tSupplier -> STUB_ITEM; // offline fixture — restored per test
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
		GT6RecipeMapIcons.sResolver = java.util.function.Supplier::get; // never leak the stub into other tests
	}

	@Test
	void everyVisibleMapCarriesATabledIconOrIsWhitelisted() {
		GT6RecipeMaps.init();
		int tTabled = 0;
		Set<String> tVisible = new TreeSet<>();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			tVisible.add(tMap.mNameInternal);
			if (GT6RecipeMapIcons.FURNACE_FALLBACK.contains(tMap.mNameInternal)) continue;
			assertTrue(GT6RecipeMapIcons.has(tMap),
					tMap.mNameInternal + " is visible but carries no icon table row and no whitelist entry");
			tTabled++;
		}
		assertEquals(72, tVisible.size(), "the batch-2 census must stay stable under this guard");
		// the whitelist is EXACTLY the four declared-empty maps (GT6RecipeMapJsonLoader's
		// zero-row-stock set) — a wider fallback is the #29a regression this card fixes
		assertTrue(tVisible.containsAll(GT6RecipeMapIcons.FURNACE_FALLBACK),
				"every whitelist entry must be a visible map (dead whitelist rows are silent drift)");
		assertEquals(4, GT6RecipeMapIcons.FURNACE_FALLBACK.size());
		assertEquals(68, tTabled, "72 visible - 4 whitelist = 68 tabled machine icons");
	}

	@Test
	void iconResolutionNeverYieldsAnEmptyStack() {
		GT6RecipeMaps.init();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			if (!GT6RecipeMapViewerMeta.visibleToViewers(tMap)) continue;
			assertFalse(GT6RecipeMapIcons.iconOf(tMap).isEmpty(),
					tMap.mNameInternal + " resolved to an empty icon stack");
		}
	}

	@Test
	void whitelistMapsResolveToTheUpstreamFurnaceFallbackWithoutTouchingTheTable() {
		GT6RecipeMaps.init();
		// the whitelist path must NOT consult the item seam (it is the faithful
		// NEI_RecipeMap.init :82 lit-furnace default, not a tabled row)
		GT6RecipeMapIcons.sResolver = tSupplier -> { throw new AssertionError("whitelist icons must not resolve table rows"); };
		try {
			RecipeMap tMicrowave = RecipeMap.RECIPE_MAPS.get("gt.recipe.microwave");
			ItemStack tIcon = GT6RecipeMapIcons.iconOf(tMicrowave);
			assertFalse(GT6RecipeMapIcons.has(tMicrowave));
			assertEquals(Items.FURNACE, tIcon.getItem(),
					GT6RecipeMapIcons.FURNACE_FALLBACK + " rides the upstream lit-furnace default");
		} finally {
			GT6RecipeMapIcons.sResolver = tSupplier -> STUB_ITEM;
		}
	}

	@Test
	void titleKeyFormulaPinned() {
		GT6RecipeMaps.init();
		assertEquals("gt6.jei.recipe_map.cokeoven", GT6RecipeMapViewerMeta.titleKey(GT6RecipeMaps.COKE_OVEN));
		assertEquals("gt6.jei.recipe_map.anvil_bend", GT6RecipeMapViewerMeta.titleKey(GT6RecipeMaps.ANVIL_BEND),
				"dots fold to underscores");
		assertEquals("gt6.jei.recipe_map.fuels_engine", GT6RecipeMapViewerMeta.titleKey(GT6RecipeMaps.ENGINE_FUELS));
		assertEquals("gt6.jei.recipe_map.furnace", GT6RecipeMapViewerMeta.titleKey(GT6RecipeMaps.FURNACE),
				"the mc.recipe. prefix folds the same way");
	}
}
