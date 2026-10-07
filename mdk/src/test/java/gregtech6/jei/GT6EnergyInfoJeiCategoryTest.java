/**
 * Offline pin suite for task energy-page-jei-leg (the E3 JEI leg of the energy-source-page
 * wave): the category's page packing and its frozen faces. The E1 POC put the runtime
 * focus behaviour in field_test; the offline face is the structural contract — every
 * carrier (the ten, STEAM included) opens at least one page recipe, page zero always
 * carries the demoted-header + all-family section order, overflow continues on further
 * page recipes of the SAME carrier with every cell inside the canvas, the packer is
 * deterministic, the row-tier line rides the GTMachines window tables, and the E5 lang
 * keys exist on BOTH locale faces (the frozen five + the declared sixth). The E1
 * reachability arm (the carrier INPUT slot) is pinned at the bytecode layer — the one
 * face no offline recording double can exercise (the GT6OreGenInfoJeiCategoryTest
 * invisible-mounting precedent).
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.datagen.GT6EnUs;
import gregtech6.datagen.GT6ZhCn;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;
import gregtech6.recipes.GT6RecipeMaps;

class GT6EnergyInfoJeiCategoryTest {

	/** The minimal vanilla offline bootstrap (the GT6EnergyJumpTest posture). */
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

	@BeforeEach
	void freshMaps() {
		GT6RecipeMaps.init(); // the packer's census walk reads the live maps (the census contract)
	}

	@AfterEach
	void dropMaps() {
		GT6RecipeMaps.reset();
	}

	// -------------------------------------------------------------------
	// the page census: ten carriers, at least one page each, header slot face
	// -------------------------------------------------------------------

	@Test
	public void everyCarrierOpensAtLeastOnePageRecipe() {
		List<TagData> tCarriers = GT6EnergyCensus.carriers();
		assertEquals(10, tCarriers.size(), "the nine pinned carriers plus STEAM — the tenth page");
		for (TagData tCarrier : tCarriers) {
			List<GT6EnergyInfoJeiCategory.Page> tPages = GT6EnergyInfoJeiCategory.pagesOf(tCarrier);
			assertTrue(tPages.size() >= 1, tCarrier.mName + " opens at least one page — the empty-carrier clause"
					+ " (a carrier without a recipe silently never opens, the E1 risk-2 face)");
			for (int i = 0; i < tPages.size(); i++) {
				assertEquals(tCarrier, tPages.get(i).carrier(), "every page rides its carrier");
				assertEquals(i, tPages.get(i).number(), "page numbers run 0..n per carrier");
			}
		}
	}

	@Test
	public void thePackerIsDeterministic() {
		// the structural projection (not Page.equals — the cells carry live item suppliers,
		// whose lambda identities never compare equal across walks)
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			assertEquals(project(GT6EnergyInfoJeiCategory.pagesOf(tCarrier)),
					project(GT6EnergyInfoJeiCategory.pagesOf(tCarrier)),
					tCarrier.mName + " packs identically on every walk (the registration-order face)");
		}
	}

	private static List<String> project(List<GT6EnergyInfoJeiCategory.Page> aPages) {
		List<String> rLines = new java.util.ArrayList<>();
		for (GT6EnergyInfoJeiCategory.Page tPage : aPages) {
			rLines.add(tPage.carrier().mName + "#" + tPage.number() + (tPage.produceEmpty() ? "!" : ""));
			for (GT6EnergyInfoJeiCategory.Section tSection : tPage.sections()) {
				rLines.add(tSection.key() + "@" + tSection.headerY());
				for (GT6EnergyInfoJeiCategory.Cell tCell : tSection.cells())
					rLines.add(tCell.x() + "," + tCell.y() + " " + tCell.path()
							+ (tCell.map() == null ? "" : ">" + tCell.map().mNameInternal)
							+ (tCell.from() == null ? "" : "~" + tCell.from().mName + "->" + tCell.to().mName));
			}
		}
		return rLines;
	}

	@Test
	public void pageZeroCarriesTheApprovedSectionOrder() {
		List<String> tApproved = List.of(GT6EnergyInfoJeiCategory.GROUP_GENERATORS_KEY,
				GT6EnergyInfoJeiCategory.GROUP_PROCESSORS_KEY, GT6EnergyInfoJeiCategory.GROUP_CONVERTERS_KEY);
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			List<String> tKeys = GT6EnergyInfoJeiCategory.pagesOf(tCarrier).get(0).sections().stream()
					.map(GT6EnergyInfoJeiCategory.Section::key).toList();
			// page zero's sections form a subsequence of the approved order (产生 → 消费 → 转换器)
			int tCursor = 0;
			for (String tKey : tKeys) {
				int tNext = tApproved.indexOf(tKey);
				assertTrue(tNext >= tCursor, tCarrier.mName + " page-0 sections follow the 产生/消费/转换器 order");
				tCursor = tNext;
			}
			assertTrue(tKeys.contains(GT6EnergyInfoJeiCategory.GROUP_GENERATORS_KEY),
					tCarrier.mName + " page zero always opens with the generators header (the 空态 clause's home)");
		}
	}

	@Test
	public void emptyProducersFlagMatchesTheCensus() {
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			boolean tEmpty = GT6EnergyCensus.familiesOf(tCarrier).produce().isEmpty();
			for (GT6EnergyInfoJeiCategory.Page tPage : GT6EnergyInfoJeiCategory.pagesOf(tCarrier))
				assertEquals(tEmpty, tPage.produceEmpty(),
						tCarrier.mName + " pages carry the census's produce-empty verdict (CU/LU/MU/KU = true)");
		}
		assertTrue(GT6EnergyInfoJeiCategory.pagesOf(TD.Energy.CU).get(0).produceEmpty(),
				"CU (the 空态 exemplar) carries the empty-produce flag");
	}

	@Test
	public void overflowContinuesOnTheSameCarrierInsideTheCanvas() {
		int tOverflowingCarriers = 0;
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			List<GT6EnergyInfoJeiCategory.Page> tPages = GT6EnergyInfoJeiCategory.pagesOf(tCarrier);
			if (tPages.size() > 1) tOverflowingCarriers++;
			for (GT6EnergyInfoJeiCategory.Page tPage : tPages) {
				for (GT6EnergyInfoJeiCategory.Section tSection : tPage.sections()) {
					assertTrue(tSection.headerY() >= 26 && tSection.headerY() <= 194,
							tCarrier.mName + " page " + tPage.number() + " header inside the content band");
					for (GT6EnergyInfoJeiCategory.Cell tCell : tSection.cells()) {
						assertTrue(tCell.x() >= 0 && tCell.x() + 16 <= GT6EnergyInfoJeiCategory.WIDTH
								&& tCell.y() >= 26 && tCell.y() + 16 <= 194,
								tCarrier.mName + " page " + tPage.number() + " cell (" + tCell.path()
										+ ") stays inside the 202x206 canvas");
					}
				}
			}
		}
		assertTrue(tOverflowingCarriers >= 1, "at least one carrier overflows into a second page recipe"
				+ " (the approved 翻页 semantics — native page arrows, not a scroll box)");
		// EU fits one canvas (8 grid rows + 2 headers = 164px ≤ 168) — the E2 census's EU face
		// is exactly the 24+19 split that motivated the 8×3 grid; HU (98 burning-box rows)
		// paginates deep — the overflow mechanism's stress face
		assertTrue(GT6EnergyInfoJeiCategory.pagesOf(TD.Energy.HU).size() >= 3, "HU paginates past two pages");
	}

	@Test
	public void consumersFlattenOneCellPerMachinePath() {
		Set<String> tPaths = new HashSet<>();
		int tCells = 0;
		for (GT6EnergyInfoJeiCategory.Page tPage : GT6EnergyInfoJeiCategory.pagesOf(TD.Energy.EU))
			for (GT6EnergyInfoJeiCategory.Section tSection : tPage.sections())
				for (GT6EnergyInfoJeiCategory.Cell tCell : tSection.cells()) {
					if (tCell.map() == null) continue;
					tCells++;
					assertTrue(tPaths.add(tCell.path()),
							"the consumer grid dedupes by machine path (first owning map wins — " + tCell.path() + ")");
				}
		// the census walk flattened and deduped — the packer may neither drop nor duplicate a machine
		Set<String> tCensusPaths = new HashSet<>();
		for (GT6EnergyCensus.Consumer tConsumer : GT6EnergyCensus.consumersOf(TD.Energy.EU))
			for (GT6RecipeMapIcons.Workstation tWorkstation : tConsumer.workstations())
				tCensusPaths.add(tWorkstation.path());
		assertEquals(tCensusPaths.size(), tCells, "the EU consumer grid = the census, path-deduped ("
				+ tCensusPaths.size() + " machines — the wireframe's (43 台) band)");
		assertTrue(tCells >= 40, "the EU consume face stays the multi-machine census");
	}

	@Test
	public void convertersCarryTheirFromToPair() {
		GT6EnergyCensus.Families tFamilies = GT6EnergyCensus.familiesOf(TD.Energy.RU);
		org.junit.jupiter.api.Assertions.assertFalse(tFamilies.convert().isEmpty(), "RU converts (RU→EU dynamos)");
		TagData tFrom = tFamilies.convert().get(0).from();
		assertEquals(TD.Energy.RU, tFrom, "the RU page's converter rows key on the INPUT carrier");
	}

	// -------------------------------------------------------------------
	// the E1 reachability arm + the registration walk, at the bytecode layer
	// -------------------------------------------------------------------

	@Test
	public void theCarrierInputSlotArmIsInTheBytecode() throws Exception {
		try (java.io.InputStream in = GT6EnergyInfoJeiCategory.class.getResourceAsStream("GT6EnergyInfoJeiCategory.class")) {
			assertNotNull(in, "category class resource not found on the test classpath");
			String tBytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertTrue(tBytes.contains("addInputSlot"),
					"the carrier INPUT slot call is missing — show(focus) would never resolve this category");
			assertTrue(tBytes.contains("addIngredient"),
					"the typed pseudo-carrier mounting is missing from the slot builder chain");
		}
	}

	@Test
	public void thePluginWalkRegistersEveryCarriersPages() {
		// the registerEnergyInfoPages walk, replayed offline: ten carriers → their pages
		int tTotal = 0;
		Set<TagData> tSeen = new HashSet<>();
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			List<GT6EnergyInfoJeiCategory.Page> tPages = GT6EnergyInfoJeiCategory.pagesOf(tCarrier);
			tTotal += tPages.size();
			tSeen.add(tPages.get(0).carrier());
		}
		assertEquals(GT6EnergyCensus.carriers().size(), tSeen.size(), "every carrier registers");
		assertTrue(tTotal >= 10, "the registration row count is the ten-carrier pages (got " + tTotal + ")");
	}

	// -------------------------------------------------------------------
	// the hover faces: the TIER_INPUTS window line
	// -------------------------------------------------------------------

	@Test
	public void tierLineRidesTheRowWindows() {
		assertEquals("LV 16–64 KU", GT6EnergyInfoJeiCategory.tierLine("compressor"),
				"the compressor T1 row: TIER_NAMES[0+1] over TIER_INPUTS[0], the row's own KU carrier");
		assertEquals("ULV 4–16 EU", GT6EnergyInfoJeiCategory.tierLine("canner_ulv"),
				"the ULV rung: the explicit ULV_TIER_INPUTS window");
		// the large machines carry their own declared window (the W3 twelve)
		GT6LargeMachines.LargeMachineRow tLarge = GT6LargeMachines.ROWS.get(0);
		String tExpected = tLarge.nbtInputMin() + "–" + tLarge.nbtInputMax() + " "
				+ GT6RecipeMapViewerMeta.energyTypeShortCode(tLarge.energyType());
		assertEquals(tExpected, GT6EnergyInfoJeiCategory.tierLine(tLarge.path()),
				"the large machine's window is the row's own min–max (no TIER_INPUTS fabrication)");
		// the tier ladders (no MachineRow) carry no fabricated tier
		assertNull(GT6EnergyInfoJeiCategory.tierLine(GTMachines.SHREDDER_ITEM.getId().getPath()),
				"the shredder ladder has no row — the tier line is absent, never invented");
	}

	// -------------------------------------------------------------------
	// the E5 lang census: both faces carry the frozen five + the declared sixth
	// -------------------------------------------------------------------

	@Test
	public void bothLangFacesCoverTheFrozenKeySet() {
		Map<String, String> tEn = recordTranslations(false);
		Map<String, String> tZh = recordTranslations(true);
		List<String> tKeys = List.of(GT6EnergyInfoJeiCategory.GROUP_GENERATORS_KEY,
				GT6EnergyInfoJeiCategory.GROUP_PROCESSORS_KEY, GT6EnergyInfoJeiCategory.GROUP_CONVERTERS_KEY,
				GT6EnergyInfoJeiCategory.TRANSFER_KEY, GT6EnergyInfoJeiCategory.EMPTY_KEY,
				GT6EnergyInfoJeiCategory.EMITS_KEY);
		for (String tKey : tKeys) {
			assertNotNull(tEn.get(tKey), tKey + " missing from the en face");
			assertNotNull(tZh.get(tKey), tKey + " missing from the zh face (the tsv direct band)");
			assertTrue(!tEn.get(tKey).isBlank() && !tZh.get(tKey).isBlank(), tKey + " carries blank faces");
		}
		// the value faces: the transfer legend maps carriers to transports; the emit line takes the code
		assertTrue(tEn.get(GT6EnergyInfoJeiCategory.TRANSFER_KEY).contains("→"), "the transfer legend's arrows");
		assertTrue(tZh.get(GT6EnergyInfoJeiCategory.TRANSFER_KEY).contains("线缆"), "the zh transfer legend");
		assertTrue(tEn.get(GT6EnergyInfoJeiCategory.EMITS_KEY).contains("%s"), "the emit line's code slot");
		assertEquals("发射 %s", tZh.get(GT6EnergyInfoJeiCategory.EMITS_KEY), "the zh emit line verbatim");
		assertEquals("尚无已移植的产生机器", tZh.get(GT6EnergyInfoJeiCategory.EMPTY_KEY), "the zh 空态 line verbatim");
	}

	/** The GT6EnergyJumpTest recording posture — capture every add() without a datagen run. */
	private static Map<String, String> recordTranslations(boolean aZh) {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp", aZh ? "gt6energyinfo-zh" : "gt6energyinfo-en"));
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
	// the demoted header body: the STEAM page drives on the slot alone
	// -------------------------------------------------------------------

	@Test
	public void theHeaderBodyLineSplitsFromTheInfoKeyFace() {
		// offline the translatable falls back to the key — the split still yields the name line
		String[] tLines = GT6EnergyInfoJeiCategory.infoBodyLines(TD.Energy.EU);
		assertNotNull(tLines, "the EU header body rides the demoted info key");
		assertNotNull(tLines[0]);
		assertNull(GT6EnergyInfoJeiCategory.infoBodyLines(TD.Energy.STEAM),
				"STEAM has no info body — the tenth page header-drives on the colored slot alone");
	}
}
