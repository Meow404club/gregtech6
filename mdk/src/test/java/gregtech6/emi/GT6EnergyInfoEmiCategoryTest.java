/**
 * Offline guard suite for task energy-page-emi-twin: the EMI leg of the energy-source
 * page. Four nails per the card: ① the category registration pin — all TEN carriers get
 * page recipes, unconditionally (an empty run would silently never open the screen), ②
 * the DOUBLE-HOOK contract pin — every page's getInputs AND getOutputs return the
 * carrier's pseudo stack (the E1 red-proof fix: EMI's displayRecipes reads byOutput
 * only, an INPUT-only mount is a silent no-op) and supportsRecipeTree is false, ③ the
 * paging pin — the packer chunks families at 24 machine cells and never exceeds the
 * canvas budget (synthetic census-independent inputs), ④ the click seam pin — consume
 * cells carry the bound click, produce/convert cells and the header are hover-only.
 * The plugin retirement (EmiInfoRecipe energy instances gone) is pinned at the bytecode
 * layer, the same strategy as GT6EmiPluginTest.
 *
 * <p>Offline posture = the GT6EnergyCensusTest form: the minimal vanilla bootstrap, the
 * recipe-map generation per test, NO item resolution (the census is path-level data,
 * the item suppliers stay lazy; addWidgets is never invoked offline).
 */
package gregtech6.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import dev.emi.emi.api.stack.EmiStack;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.jei.GT6EnergyCensus;
import gregtech6.jei.GT6RecipeMapIcons;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMaterialItems;

class GT6EnergyInfoEmiCategoryTest {

	/** The minimal vanilla offline bootstrap (the GT6EnergyCensusTest form). */
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

	private static List<GT6EnergyCensus.Machine> machines(int aCount) {
		List<GT6EnergyCensus.Machine> rMachines = new ArrayList<>();
		for (int i = 0; i < aCount; i++) rMachines.add(new GT6EnergyCensus.Machine("machine_" + i, () -> null));
		return rMachines;
	}

	private static List<GT6EnergyCensus.Consumer> consumers(int aMachineCount) {
		// one census record (map may be null — the record never dereferences it offline)
		// whose workstation list carries aMachineCount path-level entries
		List<GT6RecipeMapIcons.Workstation> tWorkstations = new ArrayList<>();
		for (int i = 0; i < aMachineCount; i++) tWorkstations.add(new GT6RecipeMapIcons.Workstation("ws_" + i, () -> null));
		return List.of(new GT6EnergyCensus.Consumer(null, tWorkstations));
	}

	// -------------------------------------------------------------------
	// Nail ①: the ten-carrier registration census
	// -------------------------------------------------------------------

	@Test
	void everyCarrierGetsPagesUnconditionally() {
		GT6RecipeMaps.init();
		List<TagData> tCarriers = GT6EnergyCensus.carriers();
		assertEquals(10, tCarriers.size(), "the nine pinned carriers + STEAM, the tenth page carrier");
		List<GT6EnergyInfoEmiRecipe> tAll = new ArrayList<>();
		for (TagData tCarrier : tCarriers) {
			List<GT6EnergyInfoEmiRecipe> tPages = GT6EnergyInfoEmiCategory.pagesOf(tCarrier);
			assertFalse(tPages.isEmpty(), tCarrier.mName + " must register at least one page — an empty run silently never opens");
			tAll.addAll(tPages);
		}
		assertTrue(tAll.size() >= 10, "ten carrier pages at minimum (the big families run longer)");
	}

	@Test
	void emptyProduceCarriersRenderTheEmptyStateOnPageOne() {
		GT6RecipeMaps.init();
		// KU/CU/LU/MU: no ported producer declares them — the legal empty families
		for (TagData tCarrier : List.of(TD.Energy.KU, TD.Energy.CU, TD.Energy.LU, TD.Energy.MU)) {
			GT6EnergyInfoEmiRecipe tPage = GT6EnergyInfoEmiCategory.pagesOf(tCarrier).get(0);
			assertTrue(tPage.sections().stream()
					.anyMatch(tSection -> tSection.mKind() == GT6EnergyInfoEmiRecipe.Section.Kind.PRODUCE_EMPTY),
					tCarrier.mName + " produces nothing ported — the empty state row must render");
		}
	}

	// -------------------------------------------------------------------
	// Nail ②: the double-hook contract (the E1 red-proof fix)
	// -------------------------------------------------------------------

	@Test
	void everyPageDoubleHooksThePseudoCarrier() {
		GT6RecipeMaps.init();
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			for (GT6EnergyInfoEmiRecipe tPage : GT6EnergyInfoEmiCategory.pagesOf(tCarrier)) {
				List<EmiStack> tOutputs = tPage.getOutputs();
				assertEquals(1, tPage.getInputs().size(), tCarrier.mName + ": the INPUT half carries the pseudo carrier");
				assertEquals(1, tOutputs.size(), tCarrier.mName + ": the OUTPUT half carries the pseudo carrier (byOutput mount)");
				assertSame(tPage.getInputs().get(0), tOutputs.get(0),
						tCarrier.mName + ": the two halves share ONE stack instance (the EmiInfoRecipe mirror)");
				assertSame(tCarrier, ((GT6EnergyCarrierEmiStack) tOutputs.get(0)).carrier(),
						tCarrier.mName + ": the mounted stack is THIS carrier's pseudo stack");
				assertSame(tOutputs.get(0), ((GT6EnergyCarrierEmiStack) tOutputs.get(0)).copy(),
						tCarrier.mName + ": copy() is identity — registration and click-time lookup unify on one key");
				assertFalse(tPage.supportsRecipeTree(), tCarrier.mName + ": pseudo carriers never enter the recipe tree");
			}
		}
	}

	@Test
	void categoryAndPagesCarryTheApprovedCanvas() {
		GT6RecipeMaps.init();
		GT6EnergyInfoEmiRecipe tPage = GT6EnergyInfoEmiCategory.pagesOf(TD.Energy.EU).get(0);
		assertSame(GT6EnergyInfoEmiCategory.CATEGORY, tPage.getCategory(), "one category singleton");
		assertEquals(GT6EnergyInfoEmiRecipe.WIDTH, tPage.getDisplayWidth(), "the approved 202 canvas");
		assertEquals(GT6EnergyInfoEmiRecipe.HEIGHT, tPage.getDisplayHeight(), "the approved ~206 canvas");
		assertEquals("gt6:energy_info", GT6EnergyInfoEmiCategory.idOf().toString(), "the category uid (the JEMI skip key face)");
	}

	// -------------------------------------------------------------------
	// Nail ③: the paging packer (synthetic, census-independent)
	// -------------------------------------------------------------------

	@Test
	void familiesChunkAtTwentyFourMachineCells() {
		// 25 produce machines → chunk 0 rides page 0, the 1-machine remainder page 1
		List<GT6EnergyInfoEmiRecipe> tPages = GT6EnergyInfoEmiCategory.pagesOf(
				TD.Energy.EU, machines(25), List.of(), List.of());
		assertEquals(2, tPages.size(), "25 machines spill past the 24-cell chunk onto a second page");
		assertEquals(24, tPages.get(0).sections().get(0).mCells().size(), "page 0: the full chunk");
		assertEquals(1, tPages.get(1).sections().get(0).mCells().size(), "page 1: the remainder");
	}

	@Test
	void consumeCountsMachinesNotMaps() {
		// one census record (one map) with 30 workstations = 30 machine cells = 2 pages
		List<GT6EnergyInfoEmiRecipe> tPages = GT6EnergyInfoEmiCategory.pagesOf(
				TD.Energy.EU, List.of(), consumers(30), List.of());
		assertEquals(2, tPages.size(), "the grid unit is the MACHINE (the design's 台), not the map");
	}

	@Test
	void pageOneReproducesTheApprovedWireframeWhenEverythingFits() {
		// 8 produce + 16 consume + 5 convert — every first chunk fits one page
		List<GT6EnergyCensus.Converter> tConverters = List.of(
				new GT6EnergyCensus.Converter(new GT6EnergyCensus.Machine("dynamo", () -> null), TD.Energy.RU, TD.Energy.EU));
		List<GT6EnergyInfoEmiRecipe> tPages = GT6EnergyInfoEmiCategory.pagesOf(
				TD.Energy.EU, machines(8), consumers(16), tConverters);
		assertEquals(1, tPages.size(), "small families: the single approved wireframe page");
		List<GT6EnergyInfoEmiRecipe.Section> tSections = tPages.get(0).sections();
		assertEquals(List.of(
				GT6EnergyInfoEmiRecipe.Section.Kind.PRODUCE,
				GT6EnergyInfoEmiRecipe.Section.Kind.CONSUME,
				GT6EnergyInfoEmiRecipe.Section.Kind.CONVERT,
				GT6EnergyInfoEmiRecipe.Section.Kind.TRANSFER),
				tSections.stream().map(GT6EnergyInfoEmiRecipe.Section::mKind).toList(),
				"page 1 = header + produce + consume + converters + transfer (the approved IA)");
	}

	@Test
	void thePackerNeverExceedsTheCanvasBudget() {
		GT6RecipeMaps.init();
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			for (GT6EnergyInfoEmiRecipe tPage : GT6EnergyInfoEmiCategory.pagesOf(tCarrier)) {
				int tStack = GT6EnergyInfoEmiRecipe.BODY_BUDGET;
				for (GT6EnergyInfoEmiRecipe.Section tSection : tPage.sections()) {
					tStack += tSection.height();
				}
				assertTrue(tStack <= GT6EnergyInfoEmiRecipe.HEIGHT,
						tCarrier.mName + ": the packed sections must fit the " + GT6EnergyInfoEmiRecipe.HEIGHT + "px canvas");
			}
		}
	}

	// -------------------------------------------------------------------
	// Nail ④: the click seam
	// -------------------------------------------------------------------

	@Test
	void consumeCellsCarryTheBoundClickAndHoverCellsRefuse() {
		List<Integer> tClicks = new ArrayList<>();
		EmiStack tStack = EmiStack.EMPTY;
		GT6EnergyInfoEmiRecipe.MachineIconWidget tClickable = new GT6EnergyInfoEmiRecipe.MachineIconWidget(
				tStack, 4, 28, List.of(net.minecraft.network.chat.Component.literal("Shredder")), () -> tClicks.add(1));
		assertTrue(tClickable.mouseClicked(4 + 9, 28 + 9, 0), "the click inside a consume cell executes");
		assertEquals(List.of(1), tClicks, "the click target is the cell's bound jump");
		assertFalse(tClickable.mouseClicked(0, 0, 0), "a click off the cell is refused");

		GT6EnergyInfoEmiRecipe.MachineIconWidget tHoverOnly = new GT6EnergyInfoEmiRecipe.MachineIconWidget(
				tStack, 4, 28, List.of(net.minecraft.network.chat.Component.literal("Engine")), null);
		assertFalse(tHoverOnly.mouseClicked(4 + 9, 28 + 9, 0), "produce/convert/header cells are hover-only");
		assertTrue(tHoverOnly.getTooltip(4, 28).size() == 1, "the hover line(s) still render");
	}

	// -------------------------------------------------------------------
	// the plugin retirement (bytecode layer, the GT6EmiPluginTest strategy)
	// -------------------------------------------------------------------

	@Test
	void thePluginRetiresTheInfoRecipeFaceForTheNewCategory() throws Exception {
		try (java.io.InputStream tIn = GT6EmiPlugin.class.getResourceAsStream("GT6EmiPlugin.class")) {
			assertNotNull(tIn, "plugin class resource not found on the test classpath");
			String tBytes = new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertFalse(tBytes.contains("EmiInfoRecipe"),
					"the EmiInfoRecipe energy instances must be retired (task energy-page-emi-twin)");
			assertTrue(tBytes.contains("GT6EnergyInfoEmiCategory"),
					"the plugin must register the energy-source category twin");
		}
	}

	@Test
	void theReferencedLangKeysAreTheE3ContractSet() {
		// the key strings are the E3 card's predefined set — a typo here would render raw
		// keys on the page; the tsv rows themselves are written by the lang wave only
		assertEquals("gt6.viewer.energy.group.generators", GT6EnergyInfoEmiRecipe.GROUP_GENERATORS_KEY);
		assertEquals("gt6.viewer.energy.group.processors", GT6EnergyInfoEmiRecipe.GROUP_PROCESSORS_KEY);
		assertEquals("gt6.viewer.energy.group.converters", GT6EnergyInfoEmiRecipe.GROUP_CONVERTERS_KEY);
		assertEquals("gt6.viewer.energy.transfer", GT6EnergyInfoEmiRecipe.TRANSFER_KEY);
		assertEquals("gt6.viewer.energy.empty", GT6EnergyInfoEmiRecipe.EMPTY_KEY);
	}
}
