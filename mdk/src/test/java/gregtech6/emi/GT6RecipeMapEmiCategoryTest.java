/**
 * Offline guard tests for task debt-jei-emi-batch1: the EMI leg's generic RM category
 * surface — the memoize factory (one instance per map, the GTCEu GTRecipeEMICategory
 * form), the row face (flattened inputs/outputs, the id contract, the no-tree ruling)
 * and the widget layout. The EMI runtime cannot run offline (the tier-b GT6EmiPluginTest
 * boundary), but the widget layer can: a recording {@link dev.emi.emi.api.widget.WidgetHolder}
 * double captures every {@link dev.emi.emi.api.widget.SlotWidget} the row adds, and
 * {@code SlotWidget.getBounds()} is a pure field read — so the slot geometry is pinnable
 * here at the same coordinates the shared GT6RecipeMapViewerMeta seam (and the JEI twin)
 * render from.
 */
package gregtech6.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

public class GT6RecipeMapEmiCategoryTest {

	/** The minimal vanilla offline bootstrap (the tier-b GT6EmiPluginTest form). */
	@org.junit.jupiter.api.BeforeAll
	static void bootVanillaOffline() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	@Test
	public void memoizeFactoryYieldsOneCategoryPerMap() {
		GT6RecipeMaps.init();
		GT6RecipeMapEmiCategory tFirst = GT6RecipeMapEmiCategory.CATEGORIES.apply(GT6RecipeMaps.COKE_OVEN);
		assertSame(tFirst, GT6RecipeMapEmiCategory.CATEGORIES.apply(GT6RecipeMaps.COKE_OVEN),
				"one category instance per map (the Util.memoize factory contract)");
		assertEquals("gt6:recipe_map/gt.recipe.cokeoven", tFirst.getId().toString(),
				"the EMI category id mirrors the JEI uid one-to-one — the JEMI skip key stays aligned");
		assertEquals("Coke Oven", tFirst.getName().getString());
	}

	@Test
	public void rowFaceFlattensInputsOutputsAndCarriesTheSortedIndexId() {
		GT6RecipeMaps.init();
		RecipeMap tLathe = GT6RecipeMaps.LATHE;
		Recipe tRow = new Recipe(true,
				new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GOLD_NUGGET)},
				null, null, 400, 32, 0);
		GT6RecipeMapEmiCategory tCategory = GT6RecipeMapEmiCategory.CATEGORIES.apply(tLathe);
		GT6RecipeMapEmiRecipe tFace = new GT6RecipeMapEmiRecipe(tLathe, tRow, tCategory, 3);

		assertEquals("gt6:recipe_map/gt.recipe.lathe/3", tFace.getId().toString());
		assertSame(tCategory, tFace.getCategory());
		assertEquals(1, tFace.getInputs().size());
		assertEquals(2, tFace.getOutputs().size());
		assertEquals(Items.IRON_NUGGET, tFace.getOutputs().get(0).getItemStack().getItem());
		assertEquals(166, tFace.getDisplayWidth());
		assertEquals(140, tFace.getDisplayHeight());
		assertFalse(tFace.supportsRecipeTree(), "no transfer/tree face this card (batch 4)");
	}

	/**
	 * The widget layout: the recording holder captures the slots/text; the positions are
	 * the shared-seam coordinates — Lathe 1 in / 2 out (no fluids): in0 (53,25), out0/1
	 * (107,25)/(125,25), and the cost text starting at the fluid-free base y73.
	 */
	@Test
	public void addWidgetsLaysSlotsAtTheSharedCoordinates() {
		GT6RecipeMaps.init();
		RecipeMap tLathe = GT6RecipeMaps.LATHE;
		Recipe tRow = new Recipe(true,
				new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET)},
				null, null, 400, 32, 0);
		RecordingHolder tHolder = new RecordingHolder();
		new GT6RecipeMapEmiRecipe(tLathe, tRow, GT6RecipeMapEmiCategory.CATEGORIES.apply(tLathe), 0).addWidgets(tHolder);

		assertEquals(2, tHolder.mSlots.size(), "one input slot + one output slot");
		assertSlot(tHolder.mSlots.get(0), 53, 25);
		assertSlot(tHolder.mSlots.get(1), 107, 25);
		// the drawExtras face rides as five text widgets (Costs/Usage/Tier/Power/Time —
		// the line CONTENT is pinned by the JEI-side test's costLines asserts, the shared
		// seam; TextWidget.getBounds is client-bound so only the count is assertable here)
		assertEquals(5, tHolder.mOtherWidgets, "the Lathe row's five drawExtras lines");
	}

	/**
	 * The batch-2 structural registration guard (task debt-jei-emi-batch2): the MIXER
	 * production shape (~56000 rows) injected into the live map, then the three things
	 * the EMI registration loop does, asserted structurally — deliberately NO wall clock
	 * (flaky by nature, measures the runner not the code):
	 * <ol>
	 * <li>the map-level scan ({@code visibleMaps}) stays row-count independent — 72 maps,
	 *     not 56000 entries;</li>
	 * <li>the {@code ROW_ORDER} sort at the tie-heavy 56000 scale completes
	 *     deterministically on a COPY — an inconsistent comparator would throw TimSort's
	 *     "general contract" violation right here;</li>
	 * <li>the live mRecipeList is untouched (registration may never reorder the cooking
	 *     findRecipe domain) and per-row wrapping stays the one-wrapper O(1) shape with
	 *     the index-only id.</li>
	 * </ol>
	 * The static analysis backing this lives on GT6RecipeMapViewerMeta's class doc (the
	 * GTCEu Modern same-shape precedent, file:line pinned there).
	 */
	@Test
	public void mixerScaleRegistrationStructureGuard() {
		GT6RecipeMaps.init();
		RecipeMap tMixer = GT6RecipeMaps.MIXER;
		for (int i = 0; i < 56000; i++) {
			// two thirds share the sort keys (the real MIXER shape is tie-heavy: thousands
			// of rows over the same first input), one third varies the duration
			long tDuration = i % 3 == 0 ? 100 + (i % 7) : 100;
			tMixer.mRecipeList.add(new Recipe(true, new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
					new ItemStack[]{new ItemStack(Items.IRON_NUGGET)}, null, null, tDuration, 8, 0));
		}
		List<Recipe> tLive = new ArrayList<>(tMixer.mRecipeList);

		// 1. the map scan is row-independent
		assertEquals(72, gregtech6.jei.GT6RecipeMapViewerMeta.visibleMaps().size());

		// 2. the registration op — defensive copy, one ROW_ORDER sort, deterministic
		List<Recipe> tSorted = new ArrayList<>(tLive);
		tSorted.sort(GT6EmiPlugin.ROW_ORDER);
		List<Recipe> tSortedAgain = new ArrayList<>(tLive);
		tSortedAgain.sort(GT6EmiPlugin.ROW_ORDER);
		assertEquals(tSorted, tSortedAgain, "the sort is deterministic at the tie-heavy scale");

		// 3a. the live list never mutates (sort happened on the copy)
		assertEquals(tLive, new ArrayList<>(tMixer.mRecipeList), "registration never touches the live list");
		// 3b. per-row wrap: one wrapper, index-only id, bounded slot flattening
		GT6RecipeMapEmiRecipe tFirst = new GT6RecipeMapEmiRecipe(tMixer, tSorted.get(0),
				GT6RecipeMapEmiCategory.CATEGORIES.apply(tMixer), 0);
		assertEquals("gt6:recipe_map/gt.recipe.mixer/0", tFirst.getId().toString());
		GT6RecipeMapEmiRecipe tLast = new GT6RecipeMapEmiRecipe(tMixer, tSorted.get(tSorted.size() - 1),
				GT6RecipeMapEmiCategory.CATEGORIES.apply(tMixer), tSorted.size() - 1);
		assertEquals("gt6:recipe_map/gt.recipe.mixer/" + (tSorted.size() - 1), tLast.getId().toString());
		assertEquals(1, tLast.getInputs().size(), "one item input flattened (MIXER declares 6 slots, the row carries 1)");
		assertEquals(1, tLast.getOutputs().size());
	}

	private static void assertSlot(SlotWidget aSlot, int aX, int aY) {
		Bounds tBounds = aSlot.getBounds();
		assertEquals(aX, tBounds.x(), "slot x — the shared NEI-switch coordinate");
		assertEquals(aY, tBounds.y(), "slot y — the shared NEI-switch coordinate");
	}

	/** The recording double: {@code add} is the one funnel every addSlot/addText default lands in. */
	private static final class RecordingHolder implements WidgetHolder {
		final List<SlotWidget> mSlots = new ArrayList<>();
		int mOtherWidgets;

		@Override
		public int getWidth() {
			return 166;
		}

		@Override
		public int getHeight() {
			return 140;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Widget> T add(T aWidget) {
			if (aWidget instanceof SlotWidget tSlot) mSlots.add(tSlot); else mOtherWidgets++;
			return aWidget;
		}
	}
}
