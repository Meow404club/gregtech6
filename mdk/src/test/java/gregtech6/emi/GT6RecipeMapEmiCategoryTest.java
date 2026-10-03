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
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextureWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregtech6.jei.GT6RecipeMapViewerMeta;
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

	/**
	 * The issues #29/#34a icon table resolves Forge RegistryObjects, which do not exist in a
	 * bare JVM — the sResolver fixture seam (GT6RecipeMapJsonLoader.sItemResolver
	 * convention). Stubbed PER TEST (a @BeforeAll-set stub combined with an @AfterEach
	 * reset wiped it mid-class — the memoize-cache test passed and its successors died),
	 * restored once after the class so no stub leaks into the next fork class.
	 */
	@org.junit.jupiter.api.BeforeEach
	void stubIconResolver() {
		gregtech6.jei.GT6RecipeMapIcons.sResolver = tSupplier -> net.minecraft.world.item.Items.IRON_INGOT;
	}

	@org.junit.jupiter.api.AfterAll
	static void restoreIconResolver() {
		gregtech6.jei.GT6RecipeMapIcons.sResolver = java.util.function.Supplier::get;
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
		// issues #29/#34a: the name face is the shared translatable title key (the oregen
		// precedent — the bare-JVM getString() rides the Language fallback, pin the key)
		var tName = tFirst.getName().getContents();
		assertTrue(tName instanceof net.minecraft.network.chat.contents.TranslatableContents, "getName is translatable, not literal");
		assertEquals("gt6.jei.recipe_map.cokeoven", ((net.minecraft.network.chat.contents.TranslatableContents) tName).getKey());
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
	 * the shared-seam coordinates FOLDED to the panel system (task 34-viewer-gui-bg,
	 * the re-anchored -(5,7) sOffset fold living in the meta exits — task
	 * viewer-row-headroom, pre-fix -(5,11)) — Lathe 1 in / 2 out (no fluids):
	 * the NEI-GUI switch says in0 (53,25), out0/1 (107,25)/(125,25) → the viewer sees
	 * (48,18)/(102,18)/(120,18), and the cost text starts at the panel band y77.
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

		// the row's two slots (the gear-spot machine icon was RETIRED by task
		// viewer-icon-retire-gu-pin — EMI renders workstations itself, RecipeScreen:203-217;
		// the spot returned as the CLICKABLE jump port, task viewer-energy-jump-gear, pinned
		// in full in GT6EnergyJumpTest)
		assertEquals(2, tHolder.mSlots.size(), "one input slot + one output slot");
		assertSlot(tHolder.mSlots.get(0), 48, 18);
		assertSlot(tHolder.mSlots.get(1), 102, 18);
		// the drawExtras face rides as five text widgets (Costs/Usage/Tier/Power/Time —
		// the line CONTENT is pinned by the JEI-side test's costLines asserts, the shared
		// seam; TextWidget.getBounds is client-bound so only the count is assertable here)
		// PLUS the gear-port jump widget — LATHE is a RU carrier map, so the no-draw
		// GearJumpWidget rides third, right after the two backdrop textures (the art's z face).
		assertEquals(6, tHolder.mOtherWidgets, "the Lathe row's five drawExtras lines + the gear-port widget");
		assertEquals(2, tHolder.mTextures.size(), "the two backdrop textures lead the stack (z order pinned below)");
		assertTrue(tHolder.mAll.get(2) instanceof GT6RecipeMapEmiRecipe.GearJumpWidget,
				"the gear-port jump widget rides third, right after the two backdrops");
		assertEquals(new Bounds(147, 76, 18, 18), tHolder.mAll.get(2).getBounds(),
				"the port covers the folded gear art (152,83)-(5,7)=(147,76), 18px form");
	}

	/**
	 * The backdrop composite (task 34-viewer-gui-bg, GitHub #34): the FIRST two widgets
	 * added are the grey NEI plate and the per-map machine band (render order = add
	 * order, so these must lead the z stack), cropped at exactly the upstream
	 * drawBackground quadruples (NEI_RecipeMap.java:632/:634 folded to the panel system)
	 * and anchored at (0,0). TextureWidget's ctor is pure field assignment — fully
	 * assertable offline; its u/v fields are protected, so the pin reads them by
	 * reflection (the bounds face is public).
	 */
	@Test
	public void backdropTexturesLeadTheWidgetStackWithTheUpstreamCrops() throws Exception {
		GT6RecipeMaps.init();
		RecipeMap tLathe = GT6RecipeMaps.LATHE;
		Recipe tRow = new Recipe(true,
				new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET)},
				null, null, 400, 32, 0);
		RecordingHolder tHolder = new RecordingHolder();
		new GT6RecipeMapEmiRecipe(tLathe, tRow, GT6RecipeMapEmiCategory.CATEGORIES.apply(tLathe), 0).addWidgets(tHolder);

		assertEquals(2, tHolder.mTextures.size(), "exactly two backdrop texture widgets");
		var tPlate = tHolder.mTextures.get(0);
		var tBand = tHolder.mTextures.get(1);
		// plate: NEI.png crop (5,12,166,140) at (0,0) — the layer under everything (the
		// re-anchored v; pre-fix (5,16,166,140), the +4 task viewer-row-headroom shift)
		assertEquals(new Bounds(0, 0, 166, 140), tPlate.getBounds(), "the plate fills the 166x140 category");
		assertEquals("gt6:textures/gui/machines/nei.png", textureOf(tPlate).toString());
		assertEquals(5, uOf(tPlate));
		assertEquals(12, vOf(tPlate));
		// band: the per-map machine GUI (mGUIPath → lathe.png) crop (5,7,166,75) at (0,0)
		// — the re-anchored v with h extended to keep the same texture-row-81 bottom edge
		// (pre-fix (5,11,166,71))
		assertEquals(new Bounds(0, 0, 166, 75), tBand.getBounds(), "the machine band rides the plate's top");
		assertEquals("gt6:textures/gui/machines/lathe.png", textureOf(tBand).toString());
		assertEquals(5, uOf(tBand));
		assertEquals(7, vOf(tBand));
	}

	// The two gear-spot machine-icon tests (the bare-item z pin, the furnace-fallback
	// skip guard) were RETIRED with the face itself by task viewer-icon-retire-gu-pin —
	// EMI renders the workstation column itself (RecipeScreen.java:203-217), so there is
	// no gear-spot widget left to pin. The zero-residual census lives on
	// GT6RecipeMapViewerMetaTest.machineIconExitsAreFullyRetiredZeroResidualCensus.

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
		assertEquals(74, gregtech6.jei.GT6RecipeMapViewerMeta.visibleMaps().size());

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

	/**
	 * Issue #34 regression guard: the EMI slots are the NEI-faithful 18px form. EMI's
	 * {@code SlotWidget.large(true)} switches getBounds() to a 26x26 box anchored at the
	 * passed coordinate (its output branch — NOT centered), which on the 18px output
	 * pitch overlaps each neighbour by 8px — never again. getBounds() is a pure field
	 * read (the class doc), so the whole geometry pins offline. The BATH row exercises
	 * BOTH former large sites at once: 3 item outputs AND a fluid output. Since
	 * 34-viewer-gui-bg the lookups go through the meta's VIEWER exits (the
	 * panel-system coordinates the slots actually draw at).
	 */
	@Test
	public void outputSlotsKeepTheFaithful18pxGeometry() {
		GT6RecipeMaps.init();
		RecipeMap tBath = GT6RecipeMaps.BATH;
		Recipe tRow = new Recipe(true,
				new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.REDSTONE)},
				new FluidStack[]{new FluidStack(Fluids.WATER, 1000)},
				new FluidStack[]{new FluidStack(Fluids.LAVA, 500)},
				400, 32, 0);
		RecordingHolder tHolder = new RecordingHolder();
		new GT6RecipeMapEmiRecipe(tBath, tRow, GT6RecipeMapEmiCategory.CATEGORIES.apply(tBath), 0).addWidgets(tHolder);

		// every slot — item in/out AND fluid in/out — is the small 18x18 form; the large
		// 26x26 box (the old fluid-output line rode it too) is gone for good
		for (SlotWidget tSlot : tHolder.mSlots) {
			assertEquals(18, tSlot.getBounds().width(), "issue #34: slots stay the small 18px form (width)");
			assertEquals(18, tSlot.getBounds().height(), "issue #34: slots stay the small 18px form (height)");
		}
		// the three item outputs sit at the folded shared-seam coordinates (the GUI-system
		// 107/125/143 → panel 102/120/138), 18px apart, none spilling past the 166-wide
		// category
		int tPrevX = -1;
		for (int i = 0; i < 3; i++) {
			int[] tPos = GT6RecipeMapViewerMeta.viewerOutputPos(i, tBath);
			SlotWidget tSlot = slotAt(tHolder, tPos[0], tPos[1]);
			assertTrue(tSlot != null, "output slot " + i + " drawn at the shared coordinate " + tPos[0] + "," + tPos[1]);
			if (i > 0) assertTrue(tSlot.getBounds().x() - tPrevX >= 18, "the 18px pitch — adjacent output slots never overlap");
			assertTrue(tSlot.getBounds().x() + tSlot.getBounds().width() <= GT6RecipeMapViewerMeta.CATEGORY_WIDTH,
					"output slot " + i + " stays inside the 166-wide category");
			tPrevX = tSlot.getBounds().x();
		}
		// the BATH fold spot (the acceptance card's抽验): a 4-6-slot map with >3 fluids
		// puts input row 0 at GUI y16 → panel y9 (16−7, the re-anchored fold), and the
		// fluid input at GUI (53,63) → panel (48,56)
		int[] tFluid = GT6RecipeMapViewerMeta.viewerFluidInputPos(0);
		assertTrue(slotAt(tHolder, tFluid[0], tFluid[1]) != null,
				"the fluid input drawn at the folded " + tFluid[0] + "," + tFluid[1]);
	}

	/** The one slot drawn at the given coordinate (the holder carries a handful of slots). */
	private static SlotWidget slotAt(RecordingHolder aHolder, int aX, int aY) {
		for (SlotWidget tSlot : aHolder.mSlots)
			if (tSlot.getBounds().x() == aX && tSlot.getBounds().y() == aY) return tSlot;
		return null;
	}

	private static void assertSlot(SlotWidget aSlot, int aX, int aY) {
		Bounds tBounds = aSlot.getBounds();
		assertEquals(aX, tBounds.x(), "slot x — the shared NEI-switch coordinate, folded to the panel system");
		assertEquals(aY, tBounds.y(), "slot y — the shared NEI-switch coordinate, folded to the panel system");
	}

	// the TextureWidget geometry/UV fields are protected and the test sits in another
	// package — the pin reads them by reflection (ctor purity is what makes this legal).
	private static net.minecraft.resources.ResourceLocation textureOf(TextureWidget aWidget) throws Exception {
		var tField = TextureWidget.class.getDeclaredField("texture");
		tField.setAccessible(true);
		return (net.minecraft.resources.ResourceLocation) tField.get(aWidget);
	}

	private static int intField(TextureWidget aWidget, String aName) throws Exception {
		var tField = TextureWidget.class.getDeclaredField(aName);
		tField.setAccessible(true);
		return tField.getInt(aWidget);
	}

	private static int uOf(TextureWidget aWidget) throws Exception {
		return intField(aWidget, "u");
	}

	private static int vOf(TextureWidget aWidget) throws Exception {
		return intField(aWidget, "v");
	}

	/** The recording double: {@code add} is the one funnel every addSlot/addText default lands in. */
	private static final class RecordingHolder implements WidgetHolder {
		final List<SlotWidget> mSlots = new ArrayList<>();
		final List<TextureWidget> mTextures = new ArrayList<>();
		/** The add order across ALL widget kinds (the z-order face — render order = add order). */
		final List<Widget> mAll = new ArrayList<>();
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
			mAll.add(aWidget);
			if (aWidget instanceof SlotWidget tSlot) mSlots.add(tSlot);
			else if (aWidget instanceof TextureWidget tTexture) mTextures.add(tTexture);
			else mOtherWidgets++;
			return aWidget;
		}
	}
}
