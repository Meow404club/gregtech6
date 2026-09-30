/**
 * Offline guard tests for task #30a (GitHub #30 phase 1): the per-map accepted-energy
 * column of the shared viewer metadata — the census classification (every visible map is
 * either pinned to one carrier or declared GU), the registration-row pin sample, the
 * mixed-carrier GU fallback (the conflict list), the colored short-code unit lines and
 * the GU zero-change guard. All faces ride {@link GT6RecipeMapViewerMeta}, the seam both
 * viewer legs render from, so pinning here pins JEI and EMI at the one shared place.
 *
 * <p>Evidence base: the machine registration rows (GTMachines.java single-block families,
 * GT6LargeMachines LargeMachineRow rows, GT6Distillation TowerRows, the single-carrier
 * special TileEntities), the upstream colors TD.java:81-144 + LH.java:658+ and the short
 * codes GT6MachineProvider.energyTypeShortCode (pinned by its own jade test).
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

class GT6RecipeMapEnergySplitTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	// -------------------------------------------------------------------
	// The census classification: pinned + declared-GU covers the visible set exactly
	// -------------------------------------------------------------------

	/** The declared GU faces — mixed-carrier maps first (the #30a conflict list), then the carrier-less ones. */
	private static final Set<String> GU_MIXED = Set.of(
			// the conflict list: every pair upstream-faithful (small KU/RU vs large/EU variant)
			"gt.recipe.crusher",   // KU small GTMachines:438-440 vs RU large GT6LargeMachines:1238
			"gt.recipe.squeezer",  // KU :2940 vs RU large :1240
			"gt.recipe.sifter",    // KU :949 vs EU electricsifter :1689
			"gt.recipe.mixer",     // RU :1631/:1234 vs EU electricmixer :1652
			"gt.recipe.loom");     // RU :4280 vs EU electricloom :1673

	private static final Set<String> GU_CARRIER_LESS = Set.of(
			// hand tools (no energy face), fuel burners and display maps
			"gt.recipe.anvil", "gt.recipe.anvil.bend",   // GT6AnvilBlockEntity hammer face
			"gt.recipe.juicer",                          // hand crank
			"gt.recipe.mortar", "gt.recipe.hammer",      // hand tools (rm-six-maps)
			"gt.recipe.microwave", "gt.recipe.cooker", "gt.recipe.toolhead", // declared-empty faces
			"gt.recipe.cokeoven",                        // solid-fuel burner
			"gt.recipe.implosioncompressor",             // explosives, no energy input
			"gt.recipe.bedrockorelist",                  // the RM.java:153 display map
			"gt.recipe.catalyticcracking", "gt.recipe.steamcracking", // no machine registered yet
			// the fuel maps: the emitting carrier varies per machine (boiler HU / engine RU / turbine …)
			"gt.recipe.fuels.burn", "gt.recipe.fuels.engine", "gt.recipe.fuels.fluidbed",
			"gt.recipe.fuels.gas", "gt.recipe.fuels.hot");

	@Test
	void censusEveryVisibleMapClassified() {
		GT6RecipeMaps.init();
		int tPinned = 0;
		List<String> tUnclassified = new ArrayList<>();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			if (GT6RecipeMapViewerMeta.energyOf(tMap) != null) tPinned++;
			else if (!GU_MIXED.contains(tMap.mNameInternal) && !GU_CARRIER_LESS.contains(tMap.mNameInternal))
				tUnclassified.add(tMap.mNameInternal);
		}
		assertEquals(List.of(), tUnclassified, "every visible map must be pinned or declared GU");
		assertEquals(49, tPinned, "the pinned-carrier count (72 visible - 5 mixed - 18 carrier-less)");
		// the mixed set is never pinned — the GU fallback IS the ruling
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			if (GU_MIXED.contains(tMap.mNameInternal))
				assertNull(GT6RecipeMapViewerMeta.energyOf(tMap), tMap.mNameInternal + " is mixed-carrier and must stay GU");
		}
	}

	// -------------------------------------------------------------------
	// The registration-row pin sample (8+ maps, the card faces)
	// -------------------------------------------------------------------

	@Test
	void energyColumnPinsFromRegistrationRows() {
		GT6RecipeMaps.init();
		// SHREDDER=RU (the card pin): small family GTMachines:425-426 + large :1239 agree;
		// the p28 ULV EU rows are the port-side tier extension, outside this transcription
		assertEquals(TD.Energy.RU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.SHREDDER));
		assertEquals(TD.Energy.RU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.LATHE));
		// CRUSHER stays GU (the sweep correction): small KU :438-440 vs large RU :1238
		assertNull(GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.CRUSHER));
		// the HU sample (the card's "Oven" face has no map — the oven runs the excluded
		// FURNACE map, TileEntityOven:274): the dryer family HU, GTMachines:514
		assertEquals(TD.Energy.HU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.DRYING));
		assertEquals(TD.Energy.HU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE));
		// KU, EU, QU, CU: one carrier per family column
		assertEquals(TD.Energy.KU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.COMPRESSOR));
		assertEquals(TD.Energy.EU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.CANNER));
		assertEquals(TD.Energy.QU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.MASSFAB));
		assertEquals(TD.Energy.CU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.CRYO_DISTILLATION_TOWER));
		// FUSION: the reactor's accepted face is TU (TileEntityFusionReactor:142)
		assertEquals(TD.Energy.TU, GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.FUSION));
	}

	// -------------------------------------------------------------------
	// The colored short-code unit lines (the JEI/EMI shared render face)
	// -------------------------------------------------------------------

	private static Recipe row(long aEUt, long aDuration, long aSpecialValue) {
		return new Recipe(true, new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
				new ItemStack[]{new ItemStack(Items.IRON_NUGGET)}, null, null, aDuration, aEUt, aSpecialValue);
	}

	private static TranslatableContents contents(Component aLine) {
		return (TranslatableContents) aLine.getContents();
	}

	@Test
	void costLinesUseUnitKeysWithColoredShortCode() {
		GT6RecipeMaps.init();
		// SHREDDER (RU): Costs/Usage/Tier ride the _unit keys with the styled code as arg 2
		var tLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.SHREDDER, row(32, 400, 0));
		var tCosts = contents(tLines.get(0));
		assertEquals("gt6.jei.cost.costs_unit", tCosts.getKey());
		assertEquals(12800L, tCosts.getArgs()[0]);
		assertUnit(tCosts.getArgs()[1], "RU", ChatFormatting.GREEN);
		var tUsage = contents(tLines.get(1));
		assertEquals("gt6.jei.cost.usage_unit", tUsage.getKey());
		assertUnit(tUsage.getArgs()[1], "RU", ChatFormatting.GREEN);
		var tTier = contents(tLines.get(2));
		assertEquals("gt6.jei.cost.tier_unit", tTier.getKey());
		assertUnit(tTier.getArgs()[1], "RU", ChatFormatting.GREEN);
		// Power carries no unit (the amperage face) and Time is untouched
		assertEquals("gt6.jei.cost.power", contents(tLines.get(3)).getKey());
		assertEquals("gt6.jei.cost.time", contents(tLines.get(4)).getKey());
		// negative EUt = the Gain/Output face, same unit column
		var tGainLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.SHREDDER, row(-16, 600, 0));
		assertEquals("gt6.jei.cost.gain_unit", contents(tGainLines.get(0)).getKey());
		assertUnit(contents(tGainLines.get(0)).getArgs()[1], "RU", ChatFormatting.GREEN);
		assertEquals("gt6.jei.cost.output_unit", contents(tGainLines.get(1)).getKey());
		// the colors are the upstream TD.java:81-144 LH.Chat transcription
		assertUnit(contents(GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.CANNER, row(32, 20, 0)).get(0)).getArgs()[1],
				"EU", ChatFormatting.BLUE);
		assertUnit(contents(GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COMPRESSOR, row(32, 20, 0)).get(0)).getArgs()[1],
				"KU", ChatFormatting.DARK_GREEN);
		assertUnit(contents(GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.DRYING, row(32, 20, 0)).get(0)).getArgs()[1],
				"HU", ChatFormatting.RED);
		assertUnit(contents(GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.MASSFAB, row(32, 20, 0)).get(0)).getArgs()[1],
				"QU", ChatFormatting.DARK_PURPLE);
		assertUnit(contents(GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.FUSION, row(32, 20, 0)).get(0)).getArgs()[1],
				"TU", ChatFormatting.DARK_BLUE);
	}

	/** The second arg is the styled short-code component: text + ChatFormatting color. */
	private static void assertUnit(Object aArg, String aCode, ChatFormatting aColor) {
		assertTrue(aArg instanceof Component, "the unit arg is a Component");
		Component tUnit = (Component) aArg;
		assertEquals(aCode, tUnit.getString(), "the short code");
		assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(aColor),
				tUnit.getStyle().getColor(), "the upstream LH.Chat color");
	}

	@Test
	void fusionSpecialValueLineStaysOnTheLuStartFace() {
		GT6RecipeMaps.init();
		// FUSION is TU-pinned for the run cost, but the Start special keeps its own LU post
		var tLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.FUSION, row(8, 200, 131072));
		var tSpecial = contents(tLines.get(5));
		assertEquals("gt6.jei.cost.start", tSpecial.getKey());
		assertEquals(131072L, tSpecial.getArgs()[0]);
		assertEquals(" LU", tSpecial.getArgs()[1]);
	}

	// -------------------------------------------------------------------
	// The GU zero-change guard: mixed and carrier-less maps keep the issues #29/#34a face
	// -------------------------------------------------------------------

	@Test
	void guMapsKeepTheUnsplitCostFace() {
		GT6RecipeMaps.init();
		// MIXER: the mixed-carrier conflict-list face — GU keys, one arg, no unit
		var tLines = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.MIXER, row(32, 400, 0));
		assertEquals("gt6.jei.cost.costs", contents(tLines.get(0)).getKey());
		assertArrayArgs(new Object[]{12800L}, contents(tLines.get(0)).getArgs());
		assertEquals("gt6.jei.cost.usage", contents(tLines.get(1)).getKey());
		assertEquals("gt6.jei.cost.tier", contents(tLines.get(2)).getKey());
		// COKE_OVEN: the carrier-less face (the issues #29/#34a pin shape, byte-identical)
		var tCoke = GT6RecipeMapViewerMeta.costLines(GT6RecipeMaps.COKE_OVEN, row(32, 400, 0));
		assertEquals("gt6.jei.cost.costs", contents(tCoke.get(0)).getKey());
		assertArrayArgs(new Object[]{12800L}, contents(tCoke.get(0)).getArgs());
		assertNull(GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.COKE_OVEN));
		// the fuel maps stay GU (the emitting carrier varies per machine)
		assertNull(GT6RecipeMapViewerMeta.energyOf(GT6RecipeMaps.ENGINE_FUELS));
	}

	/** The one-arg GU face: exactly the number, no trailing unit component. */
	private static void assertArrayArgs(Object[] aExpected, Object[] aActual) {
		org.junit.jupiter.api.Assertions.assertArrayEquals(aExpected, aActual);
	}

	// -------------------------------------------------------------------
	// The short-code formatter seam (the jade reuse the card pinned)
	// -------------------------------------------------------------------

	@Test
	void shortCodesRideTheJadeFormatter() {
		assertEquals("RU", GT6RecipeMapViewerMeta.energyUnit(TD.Energy.RU).getString());
		assertEquals("KU", GT6RecipeMapViewerMeta.energyUnit(TD.Energy.KU).getString());
		assertEquals("HU", GT6RecipeMapViewerMeta.energyUnit(TD.Energy.HU).getString());
		assertEquals("EU", GT6RecipeMapViewerMeta.energyUnit(TD.Energy.EU).getString());
		assertEquals("TU", GT6RecipeMapViewerMeta.energyUnit(TD.Energy.TU).getString());
		// the styled component renders through both legs' text pipes (Component face)
		assertTrue(GT6RecipeMapViewerMeta.energyUnit(TD.Energy.RU) instanceof Component);
	}

	// -------------------------------------------------------------------
	// The mixed-set membership stays stable across the visible census
	// -------------------------------------------------------------------

	@Test
	void guSetsAreAllVisible() {
		GT6RecipeMaps.init();
		Set<String> tVisible = new TreeSet<>();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) tVisible.add(tMap.mNameInternal);
		for (String tName : GU_MIXED) assertTrue(tVisible.contains(tName), tName + " must stay visible (GU fallback, not excluded)");
		for (String tName : GU_CARRIER_LESS) assertTrue(tVisible.contains(tName), tName + " must stay visible (carrier-less GU)");
	}
}
