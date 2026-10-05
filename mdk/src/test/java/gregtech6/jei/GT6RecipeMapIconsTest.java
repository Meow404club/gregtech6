/**
 * Offline guard tests for task issues #29/#34a (GitHub #29a): the per-map category icon table —
 * every VISIBLE map carries a tabled machine item unless it is on the no-port-machine
 * furnace-fallback whitelist (the guard the card's "零兜底或仅白名单兜底"
 * clause turns into a census pin), the whitelist is closed against the live census, and
 * the icon resolution never returns an empty stack. The Forge item registry does not
 * exist offline, so resolution rides the {@code sResolver} fixture seam (the
 * GT6RecipeMapJsonLoader.sItemResolver convention); the title-key formula the table's
 * consumers share is pinned here too.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.registries.RegistryObject;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Mortars;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6Turbines;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.registry.GTMachines;

class GT6RecipeMapIconsTest extends GTRecipesOfflineTestBase {

	private static final net.minecraft.world.item.Item STUB_ITEM = Items.IRON_INGOT;

	// no static vanilla-item field: a <clinit> Items dereference runs before the base
	// bootStrap and poisons the worker JVM for every later suite (the run-order lottery)

	@BeforeAll
	static void bootMaterials() {
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	@BeforeEach
	void stubIconResolver() {
		// stubbed PER TEST (the GT6RecipeMapIcon — the EmiCategory test's lesson: a
		// @BeforeAll-set stub combined with an @AfterEach restore dies on method-order
		// lottery — any test running after the first wipe would resolve real RegistryObjects)
		GT6RecipeMapIcons.sResolver = tSupplier -> STUB_ITEM;
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
		GT6RecipeMapIcons.sResolver = java.util.function.Supplier::get; // never leak the stub into other test classes
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
		assertEquals(75, tVisible.size(), "the batch-2 census + crucible pair + recipe-b6b's nanofab must stay stable under this guard");
		// the whitelist is EXACTLY the no-port-machine maps (the declared-empty trio +
		// the nanofab, whose rows landed in recipe-b6b but whose machine block is still
		// pending) — a wider fallback is the #29a regression this card fixes
		assertTrue(tVisible.containsAll(GT6RecipeMapIcons.FURNACE_FALLBACK),
				"every whitelist entry must be a visible map (dead whitelist rows are silent drift)");
		assertEquals(4, GT6RecipeMapIcons.FURNACE_FALLBACK.size());
		assertEquals(71, tTabled, "75 visible - 4 whitelist = 71 tabled machine icons (the crucible pair joined via the Smeltery family; nanofab rides the whitelist until its machine card lands)");
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
			GT6RecipeMapIcons.sResolver = tSupplier -> Items.IRON_INGOT;
		}
	}

	/** The mortar-family card's wiring: the map left the whitelist and tables its registration-order head. */
	@Test
	void theMortarMapTablesItsRegistrationHeadInsteadOfTheFurnaceFallback() {
		GT6RecipeMaps.init();
		RecipeMap tMortar = RecipeMap.RECIPE_MAPS.get("gt.recipe.mortar");
		assertFalse(GT6RecipeMapIcons.FURNACE_FALLBACK.contains("gt.recipe.mortar"),
				"the mortar machine now exists in the port — the furnace-fallback whitelist row is retired");
		assertTrue(GT6RecipeMapIcons.has(tMortar), "the icon table carries the mortar row (the workstation walk face)");
		assertEquals(Items.IRON_INGOT, GT6RecipeMapIcons.iconOf(tMortar).getItem(),
				"the mortar icon resolves through the table (the first() head = mortar_steel live, the stub offline)");
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

	// -------------------------------------------------------------------------
	// r11-emi-workstation-full — the reverse-index census
	// -------------------------------------------------------------------------

	/**
	 * The test-side re-derivation of the whole machine walk, straight off the same
	 * registration tables the production build() consumes — the independent arithmetic the
	 * "数量与机器注册对账" clause pins. Same sources, own loops: a dropped family, a
	 * mispaired item table or a wrong key on the production side shows up as a path-list
	 * mismatch here.
	 */
	private static java.util.Map<String, java.util.List<String>> expectedPaths() {
		java.util.Map<String, java.util.List<String>> r = new java.util.TreeMap<>();
		java.util.function.BiConsumer<String, String> add = (aMap, aPath) -> r.computeIfAbsent(aMap, k -> new ArrayList<>()).add(aPath);
		java.util.function.BiConsumer<java.util.List<GTBasicMachineBlock.MachineRow>, java.util.Map<String, RegistryObject<Item>>> walk =
				(aRows, aItems) -> {
					for (GTBasicMachineBlock.MachineRow tRow : aRows) add.accept(tRow.recipes().get().mNameInternal, tRow.path());
				};
		// the multiblock controllers
		add.accept("gt.recipe.cokeoven", "multiblock_coke_oven");
		add.accept("gt.recipe.implosioncompressor", "implosion_compressor");
		add.accept("gt.recipe.massfab", "large_massfab");
		add.accept("gt.recipe.fusionreactor", "fusion_reactor");
		add.accept("gt.recipe.bedrockorelist", "bedrock_drill");
		// the row-less tier ladders
		for (String tPath : new String[]{"shredder", "shredder_t2", "shredder_t3", "shredder_t4", "shredder_ulv"}) add.accept("gt.recipe.shredder", tPath);
		for (String tPath : new String[]{"crusher", "crusher_t2", "crusher_t3", "crusher_t4", "crusher_ulv"}) add.accept("gt.recipe.crusher", tPath);
		for (String tPath : new String[]{"lathe", "lathe_t2", "lathe_t3", "lathe_t4"}) add.accept("gt.recipe.lathe", tPath);
		// the row-carried families (the same pairing the registration loops run)
		walk.accept(GTMachines.DRYER_ROWS, GTMachines.DRYER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CANNER_ROWS, GTMachines.CANNER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CANNER_ULV_ROWS, GTMachines.CANNER_ITEMS_BY_PATH);
		walk.accept(GTMachines.PRESS_ROWS, GTMachines.PRESS_ITEMS_BY_PATH);
		walk.accept(GTMachines.EXTRUDER_ROWS, GTMachines.EXTRUDER_ITEMS_BY_PATH);
		walk.accept(GTMachines.SIFTER_ROWS, GTMachines.SIFTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.SIFTER_ULV_ROWS, GTMachines.SIFTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.COMPRESSOR_ROWS, GTMachines.COMPRESSOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.WIREMILL_ROWS, GTMachines.WIREMILL_ITEMS_BY_PATH);
		walk.accept(GTMachines.WIREMILL_ULV_ROWS, GTMachines.WIREMILL_ITEMS_BY_PATH);
		walk.accept(GTMachines.ROLLINGMILL_ROWS, GTMachines.ROLLINGMILL_ITEMS_BY_PATH);
		walk.accept(GTMachines.ROLLINGMILL_RU_ROWS, GTMachines.ROLLINGMILL_ITEMS_BY_PATH);
		walk.accept(GTMachines.ROLL_BENDER_ROWS, GTMachines.ROLLBENDER_ITEMS_BY_PATH);
		walk.accept(GTMachines.ROLL_FORMER_ROWS, GTMachines.ROLLFORMER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CLUSTER_MILL_ROWS, GTMachines.CLUSTERMILL_ITEMS_BY_PATH);
		walk.accept(GTMachines.MIXER_ROWS, GTMachines.MIXER_ITEMS_BY_PATH);
		walk.accept(GTMachines.ELECTRIC_MIXER_ROWS, GTMachines.ELECTRIC_MIXER_ITEMS_BY_PATH);
		walk.accept(GTMachines.LOOM_ROWS, GTMachines.LOOM_ITEMS_BY_PATH);
		walk.accept(GTMachines.ELECTRIC_LOOM_ROWS, GTMachines.ELECTRIC_LOOM_ITEMS_BY_PATH);
		walk.accept(GTMachines.ELECTRIC_SIFTER_ROWS, GTMachines.ELECTRIC_SIFTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.BOXINATOR_ROWS, GTMachines.BOXINATOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.UNBOXINATOR_ROWS, GTMachines.UNBOXINATOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.FERMENTER_ROWS, GTMachines.FERMENTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.POLARIZER_ROWS, GTMachines.POLARIZER_ITEMS_BY_PATH);
		walk.accept(GTMachines.MAGNETIC_SEPARATOR_ROWS, GTMachines.MAGNETIC_SEPARATOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.LASER_ENGRAVER_ROWS, GTMachines.LASER_ENGRAVER_ITEMS_BY_PATH);
		walk.accept(GTMachines.LASER_WELDER_ROWS, GTMachines.LASER_WELDER_ITEMS_BY_PATH);
		walk.accept(GTMachines.FREEZER_ROWS, GTMachines.FREEZER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CRYO_MIXER_ROWS, GTMachines.CRYO_MIXER_ITEMS_BY_PATH);
		walk.accept(GTMachines.MASSFAB_SMALL_ROWS, GTMachines.MASSFAB_SMALL_ITEMS_BY_PATH);
		walk.accept(GTMachines.MOLECULAR_SCANNER_ROWS, GTMachines.MOLECULAR_SCANNER_ITEMS_BY_PATH);
		walk.accept(GTMachines.REPLICATOR_ROWS, GTMachines.REPLICATOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.DISTILLERY_ROWS, GTMachines.DISTILLERY_ITEMS_BY_PATH);
		walk.accept(GTMachines.BUZZSAW_ROWS, GTMachines.BUZZSAW_ITEMS_BY_PATH);
		walk.accept(GTMachines.SQUEEZER_ROWS, GTMachines.SQUEEZER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CENTRIFUGE_ROWS, GTMachines.CENTRIFUGE_ITEMS_BY_PATH);
		walk.accept(GTMachines.SLUICE_ROWS, GTMachines.SLUICE_ITEMS_BY_PATH);
		walk.accept(GTMachines.SANDING_ROWS, GTMachines.SANDING_ITEMS_BY_PATH);
		walk.accept(GTMachines.PRESSURE_WASHER_ROWS, GTMachines.PRESSURE_WASHER_ITEMS_BY_PATH);
		walk.accept(GTMachines.AUTOCRAFTER_ROWS, GTMachines.AUTOCRAFTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.LIGHTNING_ROWS, GTMachines.LIGHTNING_ITEMS_BY_PATH);
		walk.accept(GTMachines.LAMINATOR_ROWS, GTMachines.LAMINATOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.ELECTROLYZER_ROWS, GTMachines.ELECTROLYZER_ITEMS_BY_PATH);
		walk.accept(GTMachines.INJECTOR_ROWS, GTMachines.INJECTOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.PRINTER_ROWS, GTMachines.PRINTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.SCANNER_VISUALS_ROWS, GTMachines.SCANNER_VISUALS_ITEMS_BY_PATH);
		walk.accept(GTMachines.SLICER_ROWS, GTMachines.SLICER_ITEMS_BY_PATH);
		walk.accept(GTMachines.STEAM_CRACKER_ROWS, GTMachines.STEAM_CRACKER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CATALYTIC_CRACKER_ROWS, GTMachines.CATALYTIC_CRACKER_ITEMS_BY_PATH);
		walk.accept(GTMachines.COAGULATOR_ROWS, GTMachines.COAGULATOR_ITEMS_BY_PATH);
		walk.accept(GTMachines.GENERIFIER_ROWS, GTMachines.GENERIFIER_ITEMS_BY_PATH);
		walk.accept(GTMachines.BATH_ROWS, GTMachines.BATH_ITEMS_BY_PATH);
		walk.accept(GTMachines.AUTOCLAVE_ROWS, GTMachines.AUTOCLAVE_ITEMS_BY_PATH);
		walk.accept(GTMachines.SMELTER_ROWS, GTMachines.SMELTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.MELTER_ROWS, GTMachines.MELTER_ITEMS_BY_PATH);
		walk.accept(GTMachines.ROASTING_ROWS, GTMachines.ROASTING_ITEMS_BY_PATH);
		walk.accept(GTMachines.BUMBLELYZER_ROWS, GTMachines.BUMBLELYZER_ITEMS_BY_PATH);
		walk.accept(GTMachines.CRYSTALLISATION_ROWS, GTMachines.CRYSTALLISATION_ITEMS_BY_PATH);
		walk.accept(GTMachines.BURNER_MIXER_ROWS, GTMachines.BURNER_MIXER_ITEMS_BY_PATH);
		walk.accept(GTMachines.PLANTALYZER_ROWS, GTMachines.PLANTALYZER_ITEMS_BY_PATH);
		// the twelve W3 large machines
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) add.accept(tRow.recipes().get().mNameInternal, tRow.path());
		// the towers
		for (GT6Distillation.TowerRow tRow : GT6Distillation.ROWS)
			add.accept(tRow.cryo() ? "gt.recipe.cryodistillationtower" : "gt.recipe.distillationtower", tRow.path());
		// the anvils + tool/kitchen faces
		for (GT6Anvils.AnvilRow tRow : GT6Anvils.ROWS) add.accept("gt.recipe.anvil", tRow.path());
		add.accept("gt.recipe.juicer", "juicer");
		add.accept("gt.recipe.anvil.bend", "bending_cylinder");
		add.accept("gt.recipe.hammer", "hammer");
		// the mortar family (task mortar-family — the hand-tool face joins the tabled
		// machines; the keySet order IS the registration order, the same source the
		// production walk reads)
		for (String tPath : GT6Mortars.ITEMS_BY_PATH.keySet()) add.accept("gt.recipe.mortar", tPath);
		// the fuel faces (diesels ride the same spec-driven derivation as production — the
		// DIESEL_ITEMS map fills at the mod-bus event, not at class-init, so offline the
		// expected list is the spec names and the supplier stays unresolved under the stub)
		for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS)
			add.accept("gt.recipe.fuels.engine", GT6Kinetics.dieselName(tSpec.material()));
		for (GT6Turbines.GasTurbineRow tRow : GT6Turbines.GAS_ROWS) add.accept("gt.recipe.fuels.gas", tRow.path());
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows()) {
			String tMap = tRow.family() == GT6BurningBoxes.Family.LIQUID || tRow.family() == GT6BurningBoxes.Family.GAS
					? "gt.recipe.fuels.burn"
					: tRow.family() == GT6BurningBoxes.Family.FLUIDBED ? "gt.recipe.fuels.fluidbed" : null;
			if (tMap != null) add.accept(tMap, tRow.path());
		}
		add.accept("gt.recipe.fuels.hot", "large_heat_exchanger");
		// the crucible pair (crucible-viewer-page): the Smeltery family's lowest registered
		// rung, both maps share the representative — the mirror of the production build()'s
		// tail entries (the ITEMS_BY_PATH LinkedHashMap order)
		add.accept("gt.recipe.cruciblesmelting", GT6Crucibles.ITEMS_BY_PATH.keySet().iterator().next());
		add.accept("gt.recipe.cruciblealloying", GT6Crucibles.ITEMS_BY_PATH.keySet().iterator().next());
		return r;
	}

	@Test
	void workstationIndexReconcilesWithTheMachineRegistration() {
		GT6RecipeMaps.init();
		java.util.Map<String, java.util.List<String>> tExpected = expectedPaths();
		// two-way key closure: the production walk covers exactly the machines the
		// registration tables carry — no dropped family, no phantom map
		for (java.util.Map.Entry<String, java.util.List<String>> tEntry : tExpected.entrySet()) {
			RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(tEntry.getKey());
			assertNotNull(tMap, "the walk keyed " + tEntry.getKey() + " but the map registry has no such map");
			java.util.List<String> tActual = new ArrayList<>();
			for (GT6RecipeMapIcons.Workstation tWs : GT6RecipeMapIcons.workstationsOf(tMap)) tActual.add(tWs.path());
			assertEquals(tEntry.getValue(), tActual, "machine list mismatch for " + tEntry.getKey());
		}
		assertEquals(tExpected.keySet(), productionKeys(),
				"the production index must carry exactly the maps the registration walks derive");
	}

	/** The production index's key set (the two-way closure's other half). */
	private static java.util.Set<String> productionKeys() {
		java.util.Set<String> r = new TreeSet<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			if (!GT6RecipeMapIcons.workstationsOf(tMap).isEmpty()) r.add(tMap.mNameInternal);
		}
		return r;
	}

	@Test
	void representativeMapsCarryTheirFullMachineListsInRegistrationOrder() {
		GT6RecipeMaps.init();
		// shredder: the four row-less tiers + the ULV rung + the W3 large machine (6)
		assertEquals(java.util.List.of("shredder", "shredder_t2", "shredder_t3", "shredder_t4", "shredder_ulv", "large_shredder"),
				pathsOf("gt.recipe.shredder"));
		// bath: the single TU bath + the large bathing vat
		assertEquals(java.util.List.of("bath", "large_bath"), pathsOf("gt.recipe.bath"));
		// coke oven: the one controller (upstream's list has exactly the controller too)
		assertEquals(java.util.List.of("multiblock_coke_oven"), pathsOf("gt.recipe.cokeoven"));
		// the representative-compat faces: the FIRST entry is the machine the retired hand
		// table tabled (large massfab before the small ladder; the ULV rollingmill rung
		// registers first; the manual bronze mixer before the electric ladder)
		assertEquals("large_massfab", pathsOf("gt.recipe.massfab").get(0));
		assertEquals("rollingmill", pathsOf("gt.recipe.rollingmill").get(0));
		assertEquals("mixer", pathsOf("gt.recipe.mixer").get(0));
		assertEquals("sifter", pathsOf("gt.recipe.sifter").get(0));
	}

	/** The registration paths of one map's workstation list (the census face). */
	private static java.util.List<String> pathsOf(String aMapName) {
		RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(aMapName);
		assertNotNull(tMap, aMapName + " must be a registered map");
		java.util.List<String> r = new ArrayList<>();
		for (GT6RecipeMapIcons.Workstation tWs : GT6RecipeMapIcons.workstationsOf(tMap)) r.add(tWs.path());
		return r;
	}

	@Test
	void iconOfResolvesExactlyTheFirstWorkstation() {
		GT6RecipeMaps.init();
		// the category-icon face must stay cheap: iconOf resolves ONE entry (the first),
		// never the whole list — pin the resolver-invocation count
		int[] tCalls = {0};
		GT6RecipeMapIcons.sResolver = tSupplier -> { tCalls[0]++; return STUB_ITEM; };
		try {
			RecipeMap tShredder = RecipeMap.RECIPE_MAPS.get("gt.recipe.shredder");
			int tListSize = GT6RecipeMapIcons.workstationsOf(tShredder).size();
			assertTrue(tListSize >= 6, "the shredder list must carry the full ladder");
			GT6RecipeMapIcons.iconOf(tShredder);
			assertEquals(1, tCalls[0], "iconOf must resolve only the FIRST workstation, not the list");
		} finally {
			GT6RecipeMapIcons.sResolver = tSupplier -> STUB_ITEM;
		}
	}
}
