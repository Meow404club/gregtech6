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
import static org.junit.jupiter.api.Assertions.assertNull;
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
		// GearJumpWidget rides after the plate + the furniture crops + the three slot-frame
		// pads (task viewer-recipe-slot-frames), right before the slots (the art's z face).
		assertEquals(6, tHolder.mOtherWidgets, "the Lathe row's five drawExtras lines + the gear-port widget");
		// the Lathe map declares 1 input + 2 outputs (no fluids) → 3 slot-frame pads join
		// the plate + the two furniture crops (task viewer-recipe-slot-frames)
		assertEquals(6, tHolder.mTextures.size(), "plate + two furniture crops + the 3 declared slot-frame pads");
		assertTrue(tHolder.mAll.get(6) instanceof GT6RecipeMapEmiRecipe.GearJumpWidget,
				"the gear-port jump widget rides after the plate + furniture + the 3 slot-frame pads");
		assertEquals(new Bounds(72, 53, 22, 22), tHolder.mAll.get(6).getBounds(),
				"the port covers the folded gear-slot art (77,60)-(5,7)=(72,53), 22x22 form");
	}

	/**
	 * The composed-page lead (task 34-viewer-gui-bg, GitHub #34; composed per
	 * composed-ui-energy-slot-and-parts): the FIRST widgets added are the grey NEI plate
	 * and the two machine-skin furniture part crops — the arrow cell and the special-slot
	 * gear cell (render order = add order, so these must lead the z stack). The per-map
	 * machine band is RETIRED (its baked slot frames doubled the code slots'). The plate
	 * keeps the upstream drawBackground quadruple (NEI_RecipeMap.java:632 folded to the
	 * panel system) anchored at (0,0); the parts are standalone small PNGs at u/v 0 with
	 * their own dims declared. TextureWidget's ctor is pure field assignment — fully
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

		// the plate + the two furniture part crops lead; the Lathe map's 3 slot-frame pads
		// (1 in + 2 out, task viewer-recipe-slot-frames) follow them — the lead is unchanged
		assertEquals(6, tHolder.mTextures.size(), "plate + arrow + gear lead, the 3 declared slot-frame pads follow");
		var tPlate = tHolder.mTextures.get(0);
		var tArrow = tHolder.mTextures.get(1);
		var tGear = tHolder.mTextures.get(2);
		// plate: NEI.png crop (5,12,166,140) at (0,0) — the layer under everything (the
		// re-anchored v; pre-fix (5,16,166,140), the +4 task viewer-row-headroom shift)
		assertEquals(new Bounds(0, 0, 166, 140), tPlate.getBounds(), "the plate fills the 166x140 category");
		assertEquals("gt6:textures/gui/machines/nei.png", textureOf(tPlate).toString());
		assertEquals(5, uOf(tPlate));
		assertEquals(12, vOf(tPlate));
		// arrow cell: the skin print at GUI (78,24) → panel (73,17), the standalone part
		assertEquals(new Bounds(73, 17, 20, 18), tArrow.getBounds(), "the arrow cell rides its folded exit");
		assertEquals("gt6:textures/gui/parts/arrow_outline_20x18.png", textureOf(tArrow).toString());
		assertEquals(0, uOf(tArrow));
		assertEquals(0, vOf(tArrow));
		// gear slot: the skin print at GUI (77,60) → panel (72,53), the jump's art
		assertEquals(new Bounds(72, 53, 22, 22), tGear.getBounds(), "the gear slot rides its folded exit");
		assertEquals("gt6:textures/gui/parts/slot_special_22x22.png", textureOf(tGear).toString());
		assertEquals(0, uOf(tGear));
		assertEquals(0, vOf(tGear));
	}

	/**
	 * The GT6 GUI slot-base pads (task viewer-recipe-slot-frames, user 反馈⑦): the EMI
	 * native slot back is not the GT6 GUI look and EMPTY declared slots rendered as bare
	 * holes. The fix's three faces, all offline-pinnable on the Lathe map (declares
	 * 1 input + 2 outputs, no fluids — its second output slot is the empty-slot case):
	 * <ol>
	 * <li>EVERY declared slot position gets a {@code slot_frame_18x18} TextureWidget pad
	 *     at the folded shared-seam coordinate — including positions the row leaves
	 *     empty, which now render as slots, not holes;</li>
	 * <li>a non-empty row stack keeps its SlotWidget at the SAME coordinate as its pad
	 *     (the overlay relation — pad below, stack above, render order = add order);</li>
	 * <li>the SlotWidgets ride {@code drawBack(false)} — EMI's native widgets.png back
	 *     (SlotWidget.drawBackground's default arm, emi xplat SlotWidget.java:168-185)
	 *     must not stack over the GT6 pad (the field is protected → reflection, the same
	 *     posture as the u/v pins).</li>
	 * </ol>
	 */
	@Test
	public void slotFramePadsPadEveryDeclaredSlotAndEmptySlotsRenderToo() throws Exception {
		GT6RecipeMaps.init();
		RecipeMap tLathe = GT6RecipeMaps.LATHE;
		// the row carries 1 input + 1 output; the map declares 2 outputs — declared
		// output slot 1 stays empty and must still render its pad
		Recipe tRow = new Recipe(true,
				new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET)},
				null, null, 400, 32, 0);
		RecordingHolder tHolder = new RecordingHolder();
		new GT6RecipeMapEmiRecipe(tLathe, tRow, GT6RecipeMapEmiCategory.CATEGORIES.apply(tLathe), 0).addWidgets(tHolder);

		// 1. the three pads lead after the furniture, at the shared coordinates
		assertEquals(6, tHolder.mTextures.size(), "plate + arrow + gear + 3 slot-frame pads");
		for (int i = 0; i < 3; i++) {
			TextureWidget tPad = tHolder.mTextures.get(3 + i);
			int[] tPos = i == 0 ? GT6RecipeMapViewerMeta.viewerInputPos(0, tLathe)
					: GT6RecipeMapViewerMeta.viewerOutputPos(i - 1, tLathe);
			assertEquals(new Bounds(tPos[0], tPos[1], 18, 18), tPad.getBounds(),
					"pad " + i + " covers the declared slot at the shared coordinate " + tPos[0] + "," + tPos[1]);
			assertEquals("gt6:textures/gui/parts/slot_frame_18x18.png", textureOf(tPad).toString());
			assertEquals(0, uOf(tPad));
			assertEquals(0, vOf(tPad));
		}
		// pads lead the slots — every pad index precedes every slot index (z = add order)
		int tFirstSlotIndex = tHolder.mAll.indexOf(tHolder.mSlots.get(0));
		assertTrue(tFirstSlotIndex > tHolder.mAll.lastIndexOf(tHolder.mTextures.get(5)),
				"the pads render under the stacks (render order = add order)");

		// 2. the empty declared slot: pad present, NO SlotWidget on top of it
		int[] tEmpty = GT6RecipeMapViewerMeta.viewerOutputPos(1, tLathe);
		assertNull(slotAt(tHolder, tEmpty[0], tEmpty[1]), "the empty declared slot carries no interactive widget");
		// 2b. the non-empty slot: the SlotWidget sits at the SAME coordinate as its pad
		int[] tFilled = GT6RecipeMapViewerMeta.viewerOutputPos(0, tLathe);
		SlotWidget tOverlay = slotAt(tHolder, tFilled[0], tFilled[1]);
		assertTrue(tOverlay != null, "the filled slot's widget rides the pad's coordinate");

		// 3. every slot rides drawBack(false) — the GT6 pad IS the base
		for (SlotWidget tSlot : tHolder.mSlots)
			assertFalse(drawBackOf(tSlot), "EMI's native slot back is disabled — the GT6 pad is the base");
	}

	/**
	 * The fluid pads (task viewer-recipe-slot-frames): BATH declares 6/6 items + 1/3
	 * fluids — every fluid position gets the {@code slot_fluid_18x19} droplet cell (the
	 * GTBasicMachineMUI fluid-seat convention: the widget box stays 18x18, the 1px lip
	 * rides), and the row's empty fluid slots (1 of 3 declared outputs) still render
	 * their pad. The slot widgets stay the faithful 18x18 form (the issue #34 pin above
	 * re-runs on the same BATH row).
	 */
	@Test
	public void fluidSlotsWearTheDropletCellPadIncludingTheEmptyOnes() throws Exception {
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

		// 3 furniture + 12 item pads (6 in + 6 out) + 4 fluid pads (1 in + 3 out) = 19
		assertEquals(19, tHolder.mTextures.size(), "plate + arrow + gear + 16 declared slot pads");
		int tFirstFluidPad = 3 + tBath.mInputItemsCount + tBath.mOutputItemsCount;
		// the first fluid pad: the droplet cell at the folded fluid-input seat (48,56)
		TextureWidget tPad = tHolder.mTextures.get(tFirstFluidPad);
		int[] tFluidIn = GT6RecipeMapViewerMeta.viewerFluidInputPos(0);
		assertEquals(new Bounds(tFluidIn[0], tFluidIn[1], 18, 19), tPad.getBounds(),
				"the fluid pad is the 18x19 droplet cell at the shared fluid coordinate");
		assertEquals("gt6:textures/gui/parts/slot_fluid_18x19.png", textureOf(tPad).toString());
		// the three fluid-output pads: the row fills only the first — pads 1 and 2 are
		// the empty declared slots, and ALL THREE carry the droplet cell
		for (int i = 0; i < 3; i++) {
			TextureWidget tOutPad = tHolder.mTextures.get(tFirstFluidPad + 1 + i);
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidOutputPos(i);
			assertEquals(new Bounds(tPos[0], tPos[1], 18, 19), tOutPad.getBounds(),
					"fluid output pad " + i + " (empty or not) rides the shared coordinate");
			assertEquals("gt6:textures/gui/parts/slot_fluid_18x19.png", textureOf(tOutPad).toString());
		}
		// the filled fluid-output slot still carries its widget; the two empty ones don't
		int[] tFilled = GT6RecipeMapViewerMeta.viewerFluidOutputPos(0);
		assertTrue(slotAt(tHolder, tFilled[0], tFilled[1]) != null, "the filled fluid output keeps its widget");
		int[] tEmpty = GT6RecipeMapViewerMeta.viewerFluidOutputPos(1);
		assertNull(slotAt(tHolder, tEmpty[0], tEmpty[1]), "the empty declared fluid output carries no interactive widget");
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

		// 1. the map scan is row-independent (75 = the 83-map census minus the 4 excluded
		// minus the 5 NEI-disallowed plus the 1 overlap — the b6b trio joined, Nanofab visible)
		assertEquals(75, gregtech6.jei.GT6RecipeMapViewerMeta.visibleMaps().size());

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

	/**
	 * {@code SlotWidget.drawBack} is protected (the emi api face) — the task
	 * viewer-recipe-slot-frames pin reads it by reflection, the same posture as the
	 * TextureWidget u/v reads below.
	 */
	private static boolean drawBackOf(SlotWidget aSlot) throws Exception {
		var tField = SlotWidget.class.getDeclaredField("drawBack");
		tField.setAccessible(true);
		return tField.getBoolean(aSlot);
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
