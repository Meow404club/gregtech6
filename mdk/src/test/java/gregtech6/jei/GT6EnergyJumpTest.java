/**
 * Offline pin suite for task viewer-energy-jump-gear: the gear-spot energy jump port on
 * both viewer legs. The POC discipline (the research card ④) puts the pseudo-carrier
 * VISIBILITY and the real-click behaviour in the user's field_test; the offline face is
 * the API-wiring correctness — the port position (the baked-art fold), the carrier/key
 * census (the single-formula closure over the live ENERGY_BY_MAP transcription), the JEI
 * extras wiring (handler+widget on carrier maps, nothing on GU maps), the JEI click
 * contract (mouse-down simulates, mouse-up executes, keys refused — IJeiInputHandler's
 * documented contract) and the EMI widget's click target (constructor-injected consumer,
 * so no EMI static runtime is touched).
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import dev.emi.emi.api.widget.Bounds;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.datagen.GT6EnUs;
import gregtech6.datagen.GT6ZhCn;
import gregtech6.emi.GT6EnergyCarrierEmiStack;
import gregtech6.emi.GT6RecipeMapEmiRecipe;
import gregtech6.registry.GTMaterialItems;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

class GT6EnergyJumpTest {

	/** The minimal vanilla offline bootstrap (the GT6RecipeMapEmiCategoryTest form). */
	@BeforeAll
	static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials();
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	/**
	 * The icon-table resolver stub (the GT6RecipeMapEmiCategoryTest posture, per-test):
	 * the EMI row-wiring pin constructs the memoized category, whose ctor resolves the
	 * per-map icon through Forge RegistryObjects — dead in a bare JVM.
	 */
	@org.junit.jupiter.api.BeforeEach
	void stubIconResolver() {
		gregtech6.jei.GT6RecipeMapIcons.sResolver = tSupplier -> net.minecraft.world.item.Items.IRON_INGOT;
	}

	@org.junit.jupiter.api.AfterAll
	static void restoreIconResolver() {
		gregtech6.jei.GT6RecipeMapIcons.sResolver = java.util.function.Supplier::get;
	}

	// -------------------------------------------------------------------
	// The seam pins: position, census, key formula, long names
	// -------------------------------------------------------------------

	@Test
	void gearPortPinsTheBakedArtSpotFoldedToPanelCoordinates() {
		// the machine-GUI constant: the machine skins' printed special slot — the 22x22
		// gear cell every sheet draws at (77,60) (upstream 1.7.10 Default.png and the
		// amazawa redraw agree; its crop IS gui/parts/slot_special_22x22.png). The first
		// cut's plate-corner decoration spot (152,83) was a self-invented slot — rolled
		// back (composed-ui-energy-slot-and-parts, the user ruling 复用齿轮).
		assertArrayEquals(new int[] {77, 60}, GT6RecipeMapViewerMeta.GEAR_POS_GUI);
		// the panel fold: (77,60) - (5,7) = (72,53)
		assertArrayEquals(new int[] {72, 53}, GT6RecipeMapViewerMeta.viewerGearPos());
		assertEquals(22, GT6RecipeMapViewerMeta.GEAR_SIZE, "the port hit box is the full 22x22 gear-slot cell");
	}

	/** The jitter-free array assertion (JUnit's array equals has no import-free shorthand here). */
	private static void assertArrayEquals(int[] aExpected, int[] aActual) {
		org.junit.jupiter.api.Assertions.assertArrayEquals(aExpected, aActual);
	}

	@Test
	void gearPortNeverOverlapsSlotsTextOrFluidRows() {
		int[] tPos = GT6RecipeMapViewerMeta.viewerGearPos();
		// inside the 166x140 category
		assertTrue(tPos[0] >= 0 && tPos[1] >= 0
				&& tPos[0] + GT6RecipeMapViewerMeta.GEAR_SIZE <= GT6RecipeMapViewerMeta.CATEGORY_WIDTH
				&& tPos[1] + GT6RecipeMapViewerMeta.GEAR_SIZE <= GT6RecipeMapViewerMeta.CATEGORY_HEIGHT,
				"the port rect stays inside the category rect");
		// the item slots: the input grid ends at GUI x71 (panel 66) and the output grid
		// starts at GUI x107 (panel 102); the gear slot lives in the middle column
		// (panel x72..94), 2D-disjoint from BOTH grids at every case row — the deepest
		// rows (fourth 61-row inputs panel x48..66, fluid outputs x102..156) all clear it
		assertTrue(tPos[0] >= 66 && tPos[0] + GT6RecipeMapViewerMeta.GEAR_SIZE <= 102,
				"the port's x-range rides the empty middle column between the two grids");
		// the text band starts at y77, x10 — the port ends at y75, strictly above it
		assertTrue(tPos[1] + GT6RecipeMapViewerMeta.GEAR_SIZE <= GT6RecipeMapViewerMeta.TEXT_BASE_Y,
				"the port clears the text band's top edge");
	}

	@Test
	void carrierCensusIsExactlyTheNinePinnedCarriers() {
		// the live ENERGY_BY_MAP transcription (pinned at 51 maps by GT6RecipeMapEnergySplitTest)
		// uses exactly nine carriers — the jump faces' whole world
		List<TagData> tCarriers = GT6RecipeMapViewerMeta.pinnedEnergyCarriers();
		assertEquals(9, tCarriers.size(), "EU/RU/KU/HU/CU/LU/MU/QU/TU — the nine the registration rows use");
		List<String> tCodes = tCarriers.stream().map(GT6RecipeMapViewerMeta::energyTypeShortCode).toList();
		assertEquals(List.of("CU", "EU", "HU", "KU", "LU", "MU", "QU", "RU", "TU"), tCodes,
				"short-code-sorted determinism — the registration walk and the census share one order");
	}

	@Test
	void energyInfoKeyFormulaAndTheUpstreamLongNames() {
		assertEquals("gt6.jei.info.energy.eu", GT6RecipeMapViewerMeta.energyInfoKey(TD.Energy.EU));
		assertEquals("gt6.jei.info.energy.tu", GT6RecipeMapViewerMeta.energyInfoKey(TD.Energy.TU));
		// the upstream TD.java createTagData 3rd args, verbatim transcription
		assertEquals("Electric Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.EU));
		assertEquals("Rotation Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.RU));
		assertEquals("Kinetic Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.KU));
		assertEquals("Heat Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.HU));
		assertEquals("Cryo Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.CU));
		assertEquals("Light Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.LU));
		assertEquals("Magnetic Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.MU));
		assertEquals("Quantum Energy", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.QU));
		assertEquals("Time", GT6RecipeMapViewerMeta.energyLongName(TD.Energy.TU));
		assertNull(GT6RecipeMapViewerMeta.energyLongName(TD.Energy.STEAM), "a non-pinned carrier has no long-name face");
	}

	// -------------------------------------------------------------------
	// The lang census: both providers carry the hint + all nine bodies
	// -------------------------------------------------------------------

	@Test
	void bothLangFacesCoverTheHintAndEveryCarrierKey() {
		GT6RecipeMaps.init(); // the provider walks read the live maps for the title band
		Map<String, String> tEn = recordTranslations(false);
		Map<String, String> tZh = recordTranslations(true);
		assertNotNull(tEn.get(GT6RecipeMapViewerMeta.ENERGY_JUMP_HINT_KEY), "the en hint face");
		assertNotNull(tZh.get(GT6RecipeMapViewerMeta.ENERGY_JUMP_HINT_KEY), "the zh hint face");
		for (TagData tCarrier : GT6RecipeMapViewerMeta.pinnedEnergyCarriers()) {
			String tKey = GT6RecipeMapViewerMeta.energyInfoKey(tCarrier);
			String tEnBody = tEn.get(tKey);
			assertNotNull(tEnBody, tKey + " missing from the en face — the info page would render the raw key");
			assertTrue(tEnBody.startsWith(GT6RecipeMapViewerMeta.energyLongName(tCarrier)),
					tKey + " en body opens with the upstream long name");
			assertNotNull(tZh.get(tKey), tKey + " missing from the zh face");
			assertFalse(tZh.get(tKey).isBlank(), tKey + " carries a blank zh face");
		}
		// the units faces: EU-denominated for eight carriers, Ticks for TU (the upstream doc)
		assertEquals("Time. Units are Ticks.", tEn.get(GT6RecipeMapViewerMeta.energyInfoKey(TD.Energy.TU)));
		assertEquals("Electric Energy. Units are IndustrialCraft EU.", tEn.get(GT6RecipeMapViewerMeta.energyInfoKey(TD.Energy.EU)));
	}

	/** The GT6RecipeMapLangTest recording posture — capture every add() without a datagen run. */
	private static Map<String, String> recordTranslations(boolean aZh) {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp", aZh ? "gt6energyjump-zh" : "gt6energyjump-en"));
		if (aZh) {
			new GT6ZhCn(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					tEntries.put(aKey, aValue);
				}

				void run() {
					this.addTranslations(); // the cross-package protected seam
				}
			}.run();
		} else {
			new GT6EnUs(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					tEntries.put(aKey, aValue);
				}

				void run() {
					this.addTranslations();
				}
			}.run();
		}
		return tEntries;
	}

	// -------------------------------------------------------------------
	// The JEI leg: extras wiring census + the click contract
	// -------------------------------------------------------------------

	@Test
	void jeiExtrasWiringCensusPortOnlyOnCarrierMaps() {
		GT6RecipeMaps.init();
		int tCarrierMaps = 0, tGuMaps = 0;
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			RecordingExtras tBuilder = new RecordingExtras();
			new GT6RecipeMapJeiCategory(tMap, null).createRecipeExtras(tBuilder, null, null);
			if (GT6RecipeMapViewerMeta.energyOf(tMap) != null) {
				tCarrierMaps++;
				assertEquals(1, tBuilder.mInputHandlers.size(), tMap.mNameInternal + " carries exactly one input handler");
				assertSame(tBuilder.mWidgets.get(0), tBuilder.mInputHandlers.get(0),
						tMap.mNameInternal + " registers ONE GearJumpFace as both widget and handler");
				GT6RecipeMapJeiCategory.GearJumpFace tFace = (GT6RecipeMapJeiCategory.GearJumpFace) tBuilder.mInputHandlers.get(0);
				assertEquals(new ScreenRectangle(72, 53, 22, 22), tFace.getArea(),
						tMap.mNameInternal + "'s port rides the folded gear rect");
			} else {
				tGuMaps++;
				assertEquals(0, tBuilder.mInputHandlers.size(), tMap.mNameInternal + " (GU) draws no port");
				assertEquals(0, tBuilder.mWidgets.size(), tMap.mNameInternal + " (GU) draws no port widget");
			}
		}
		assertEquals(51, tCarrierMaps, "the #30a carrier-map census (the ENERGY_BY_MAP transcription)");
		assertEquals(24, tGuMaps, "the GU remainder: 75 visible - 51 carrier maps (5 mixed + 19 carrier-less;"
				+ " the crucible pair joined the carrier-less set in crucible-viewer-page,"
				+ " the nanofab joined in recipe-b6b — its rows poured but no machine row to transcribe a carrier from)");
	}

	@Test
	void jeiClickContractMouseDownSimulatesMouseUpExecutesKeysRefused() {
		GT6RecipeMaps.init();
		RecordingExtras tBuilder = new RecordingExtras();
		new GT6RecipeMapJeiCategory(GT6RecipeMaps.LATHE, null).createRecipeExtras(tBuilder, null, null);
		GT6RecipeMapJeiCategory.GearJumpFace tFace = (GT6RecipeMapJeiCategory.GearJumpFace) tBuilder.mInputHandlers.get(0);

		// mouse-down: simulate — claim the click, execute nothing (no runtime touched)
		InputConstants.Key tMouse = InputConstants.Type.MOUSE.getOrCreate(0);
		assertTrue(tFace.handleInput(0, 0, new Input(tMouse, true)), "mouse-down simulates: the port can handle it");
		// mouse-up: execute — sRuntime is null offline, the guarded jump no-ops without crashing
		assertTrue(tFace.handleInput(0, 0, new Input(tMouse, false)), "mouse-up executes: handled");
		// keys are not the port's face — only mouse clicks jump
		InputConstants.Key tKey = InputConstants.Type.KEYSYM.getOrCreate(256 /* escape */);
		assertFalse(tFace.handleInput(0, 0, new Input(tKey, false)), "keyboard input is refused");
	}

	/** The minimal {@code IJeiUserInput} double (NonExtendable interface, pure value face). */
	private record Input(InputConstants.Key key, boolean simulate) implements mezz.jei.api.gui.inputs.IJeiUserInput {
		@Override
		public InputConstants.Key getKey() {
			return key;
		}

		@Override
		public int getModifiers() {
			return 0;
		}

		@Override
		public boolean isSimulate() {
			return simulate;
		}

		@Override
		public boolean is(net.minecraft.client.KeyMapping aMapping) {
			return false;
		}

		@Override
		public boolean is(mezz.jei.api.runtime.IJeiKeyMapping aMapping) {
			return false;
		}
	}

	/**
	 * The extras-builder double: records the two registration faces the port uses; every
	 * other method throws — the category must never touch them.
	 */
	private static final class RecordingExtras implements mezz.jei.api.gui.widgets.IRecipeExtrasBuilder {
		final List<mezz.jei.api.gui.widgets.IRecipeWidget> mWidgets = new ArrayList<>();
		final List<mezz.jei.api.gui.inputs.IJeiInputHandler> mInputHandlers = new ArrayList<>();

		@Override
		public void addWidget(mezz.jei.api.gui.widgets.IRecipeWidget aWidget) {
			mWidgets.add(aWidget);
		}

		@Override
		public void addInputHandler(mezz.jei.api.gui.inputs.IJeiInputHandler aHandler) {
			mInputHandlers.add(aHandler);
		}

		@Override
		public mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView getRecipeSlots() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addDrawable(mezz.jei.api.gui.drawable.IDrawable aDrawable, int aX, int aY) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.placement.IPlaceable<?> addDrawable(mezz.jei.api.gui.drawable.IDrawable aDrawable) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IDrawableWidget addDrawableWidget(mezz.jei.api.gui.drawable.IDrawable aDrawable) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IDrawableWidget addTooltipArea(int aX, int aY, int aWidth, int aHeight) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addSlottedWidget(mezz.jei.api.gui.widgets.ISlottedRecipeWidget aWidget, List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> aSlots) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addGuiEventListener(mezz.jei.api.gui.inputs.IJeiGuiEventListener aListener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IScrollBoxWidget addScrollBoxWidget(int aWidth, int aHeight, int aX, int aY) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IScrollGridWidget addScrollGridWidget(List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> aSlots, int aColumns, int aRows) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.placement.IPlaceable<?> addRecipeArrow() {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IDrawableWidget addRecipeArrowWidget() {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.placement.IPlaceable<?> addRecipePlusSign() {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IDrawableWidget addRecipePlusSignWidget() {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.placement.IPlaceable<?> addAnimatedRecipeArrow(int aTicksPerCycle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IDrawableWidget addAnimatedRecipeArrowWidget(int aTicksPerCycle) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.placement.IPlaceable<?> addAnimatedRecipeFlame(int aCookTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.IDrawableWidget addAnimatedRecipeFlameWidget(int aCookTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public mezz.jei.api.gui.widgets.ITextWidget addText(List<net.minecraft.network.chat.FormattedText> aText, int aMaxWidth, int aMaxHeight) {
			throw new UnsupportedOperationException();
		}
	}

	// -------------------------------------------------------------------
	// The EMI leg: the widget's click target + the pseudo stack identity
	// -------------------------------------------------------------------

	@Test
	void emiWidgetClickTargetPinsTheInjectedJump() {
		List<TagData> tJumps = new ArrayList<>();
		GT6RecipeMapEmiRecipe.GearJumpWidget tWidget = new GT6RecipeMapEmiRecipe.GearJumpWidget(TD.Energy.EU, tJumps::add);
		// the same folded gear rect the JEI twin's getArea returns
		assertEquals(new Bounds(72, 53, 22, 22), tWidget.getBounds());
		// outside: refused, no jump
		assertFalse(tWidget.mouseClicked(0, 0, 0), "a click off the gear rect is refused");
		assertTrue(tJumps.isEmpty(), "no jump fired off-rect");
		// inside: the injected consumer receives exactly the widget's carrier
		assertTrue(tWidget.mouseClicked(72 + 11, 53 + 11, 0), "the click inside executes");
		assertEquals(List.of(TD.Energy.EU), tJumps, "the click target IS the carrier");
		// the affordance: one tooltip line over the rect
		assertEquals(1, tWidget.getTooltip(72, 53).size());
	}

	@Test
	void emiPseudoStackIsKeyedOnTheCarrierSingleton() {
		GT6EnergyCarrierEmiStack tStack = GT6EnergyCarrierEmiStack.of(TD.Energy.RU);
		assertSame(tStack, GT6EnergyCarrierEmiStack.of(TD.Energy.RU), "one shared instance per carrier");
		assertFalse(tStack.isEmpty(), "the pseudo stack is a real stack (never dropped as empty)");
		assertSame(TD.Energy.RU, tStack.getKey(), "the key IS the TagData singleton — the recipe-index face");
		assertSame(tStack, tStack.copy(), "immutable — the copy IS the identity");
		// the name face: the shared short code (the colored component's plain string)
		assertEquals("RU", tStack.getName().getString(), "the name is the short code literal (styled)");
	}

	@Test
	void emiRowWiringAddsTheGearWidgetOnlyOnCarrierMaps() {
		GT6RecipeMaps.init();
		// LATHE (RU): the widget rides third — right after the two backdrop textures
		RecordingHolder tHolder = new RecordingHolder();
		new GT6RecipeMapEmiRecipe(GT6RecipeMaps.LATHE, row(), GT6RecipeMapEmiCategoryForTest(GT6RecipeMaps.LATHE), 0).addWidgets(tHolder);
		assertEquals(2, tHolder.mTextures.size(), "the two backdrop textures lead");
		assertTrue(tHolder.mAll.get(2) instanceof GT6RecipeMapEmiRecipe.GearJumpWidget,
				"the gear port rides third on a carrier map (the art's z face)");
		assertEquals(new Bounds(72, 53, 22, 22), tHolder.mAll.get(2).getBounds());
		// MORTAR (hand tool, no carrier): no port — the gear stays decoration
		RecordingHolder tMortarHolder = new RecordingHolder();
		new GT6RecipeMapEmiRecipe(GT6RecipeMaps.MORTAR, row(), GT6RecipeMapEmiCategoryForTest(GT6RecipeMaps.MORTAR), 0).addWidgets(tMortarHolder);
		for (dev.emi.emi.api.widget.Widget tWidget : tMortarHolder.mAll) {
			assertFalse(tWidget instanceof GT6RecipeMapEmiRecipe.GearJumpWidget,
					"MORTAR (GU) carries no jump port — 无载体图不画");
		}
	}

	private static Recipe row() {
		return new Recipe(true,
				new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)},
				new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_NUGGET)},
				null, null, 400, 32, 0);
	}

	private static gregtech6.emi.GT6RecipeMapEmiCategory GT6RecipeMapEmiCategoryForTest(RecipeMap aMap) {
		return gregtech6.emi.GT6RecipeMapEmiCategory.CATEGORIES.apply(aMap);
	}

	/** The recording WidgetHolder double (the GT6RecipeMapEmiCategoryTest shape). */
	private static final class RecordingHolder implements dev.emi.emi.api.widget.WidgetHolder {
		final List<dev.emi.emi.api.widget.TextureWidget> mTextures = new ArrayList<>();
		/** The add order across ALL widget kinds (the z-order face). */
		final List<dev.emi.emi.api.widget.Widget> mAll = new ArrayList<>();

		@Override
		public int getWidth() {
			return 166;
		}

		@Override
		public int getHeight() {
			return 140;
		}

		@Override
		public <T extends dev.emi.emi.api.widget.Widget> T add(T aWidget) {
			mAll.add(aWidget);
			if (aWidget instanceof dev.emi.emi.api.widget.TextureWidget tTexture) mTextures.add(tTexture);
			return aWidget;
		}
	}
}
