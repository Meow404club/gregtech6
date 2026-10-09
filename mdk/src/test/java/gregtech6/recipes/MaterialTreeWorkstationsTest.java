package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeWorkstations;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The material-tree workstation census (task debt-material-tree-c acceptance): the
 * catalyst/workstation set over the LIVE ore-chain pour, with the same prefix-identity
 * probe fixture as {@link MaterialTreeDisplayTest} (one MaterialPrefixItem per referenced
 * pair, lazily registered into the offline-opened vanilla registry; the four ore-chain
 * loaders poured exactly as card B pours them).
 *
 * <ul>
 * <li><b>Chain census</b>: the maps displayed on the tree's edges over the live pour are
 *     exactly the four ore-chain machines — Crusher (GT6RecipesOreChain), Shredder
 *     (ShCL terminal legs), Sifter (the LANDED :351 DUST_ORE walk), Anvil (selfcrush
 *     legs) — and the seam's table covers EXACTLY that set (a new displayed map without
 *     a table entry fails here: the deliberate discovery face).</li>
 * <li><b>Offline degrade</b>: with the mod registries unfired, every representative
 *     handle is unbound, so {@code workstationStacks} is EMPTY — the
 *     GT6OreGenInfoLayout.catalystStack degrade shape; the live client fills the stacks.</li>
 * <li><b>Hook faces (bytecode layer)</b>: the JEI plugin's compiled constant pool must
 *     reference the workstation seam (its catalyst arm) and the EMI plugin must carry
 *     {@code addWorkstation} — the one face no offline recording double can exercise,
 *     the GT6OreGenInfoJeiCategoryTest.invisibleMountingArmIsInTheBytecode precedent.</li>
 * </ul>
 */
class MaterialTreeWorkstationsTest extends GTRecipesOfflineTestBase {

	/** One lazily created probe item per referenced (prefix, material) pair — the identity seam, offline. */
	private static final Map<PrefixMaterial, Item> PREFIX_ITEMS = new HashMap<>();
	private static int sNextProbeId = 0;

	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	private static <T> void capture(java.util.function.Supplier<T> aGetter, java.util.function.Consumer<T> aSetter) {
		T tDefault = aGetter.get();
		sSeamRestores.add(() -> aSetter.accept(tDefault));
	}

	@BeforeAll
	static void bootUniverseAndOpenRegistry() {
		GTMaterialItems.initMaterials(); // the offline material universe (the ShCL convention)
		openOfflineItemRegistry();
	}

	/** The (prefix, material) -> probe-item resolver armed into the four ore-chain loaders.
	 * The gt6 namespace is load-bearing: a minecraft-namespaced probe would grow the
	 * frozen-vanilla pool GT6RecipesCokeOvenTest's synthetic universe re-deals on. */
	private static Item prefixItem(OreDictPrefix aPrefix, gregapi.oredict.OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "mtree_ws_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The offline item-registry unlock (MaterialTreeDisplayTest.openOfflineItemRegistry, mirrored). */
	private static void openOfflineItemRegistry() {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
		java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
		throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
	}

	/** getDeclaredField along the superclass chain (MaterialTreeDisplayTest helper, mirrored). */
	private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// walk up
			}
		}
		throw new NoSuchFieldException(aName);
	}

	@BeforeEach
	void armSeamsAndPourOreChain() {
		GT6RecipeMaps.reset(); // deterministic slate regardless of sibling-class order (the isolation lesson)
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeWorkstationsTest::prefixItem;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeWorkstationsTest::prefixItem;
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeWorkstationsTest::prefixItem;
		GT6RecipesSifter.sMaterialItemResolver = MaterialTreeWorkstationsTest::prefixItem;
		GT6RecipesOreChain.load(); // the Crusher ore chain
		GT6RecipesShCL.load(); // the Shredder terminal legs
		GT6RecipesAnvil.load(); // the Anvil selfcrush + mortar rows
		GT6RecipesSifter.load(); // the LANDED :351 DUST_ORE walk
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	// ------------------------------------------------------------------
	// the chain census
	// ------------------------------------------------------------------

	/** The displays over the live pour with the probe-item resolver (the B-card buildAll overload). */
	private static List<MaterialTreeDisplay> probeDisplays() {
		return MaterialTreeDisplay.buildAll(MaterialTreeBuilder.build(), MaterialTreeWorkstationsTest::prefixItem);
	}

	@Test
	public void displayedChainMapsAreExactlyTheFourOreChainMachines() {
		Set<String> tDisplayed = MaterialTreeWorkstations.displayedMapNames(probeDisplays());
		Set<String> tExpected = new TreeSet<>(Set.of(
				"gt.recipe.crusher", "gt.recipe.shredder", "gt.recipe.sifter", "gt.recipe.anvil"));
		assertEquals(tExpected, new TreeSet<>(tDisplayed),
				"the maps displayed on the material tree edges drifted — update BOTH this census and the workstation table together");
	}

	@Test
	public void workstationTableCoversExactlyTheDisplayedChain() {
		Set<String> tDisplayed = MaterialTreeWorkstations.displayedMapNames(probeDisplays());
		assertTrue(tDisplayed.stream().allMatch(MaterialTreeWorkstations.tableNames()::contains),
				"a displayed chain map has NO workstation table entry — clicking its machine never reaches the page");
		// the table is exact, not a superset: untabled families must join via a census change
		assertEquals(new TreeSet<>(Set.of("gt.recipe.crusher", "gt.recipe.shredder", "gt.recipe.sifter", "gt.recipe.anvil")),
				new TreeSet<>(MaterialTreeWorkstations.tableNames()));
	}

	@Test
	public void workstationStacksResolvePerLegHarness() {
		// the representative handles resolve per-leg offline (the ore-gen card's harness
		// fork: forge never fires RegisterEvent → unbound → EMPTY; the neo moddev harness
		// DOES fire registration → the real stacks resolve — the live-content proof)
		List<net.minecraft.world.item.ItemStack> tStacks = MaterialTreeWorkstations.workstationStacks(probeDisplays());
		assertNotNull(tStacks, "the seam must return a list, never null");
		//? if forge {
		assertTrue(tStacks.isEmpty(), "unbound handles must resolve EMPTY and drop out offline: " + tStacks);
		//?} else {
		/*assertEquals(4, tStacks.size(), "one representative per chain map, TABLE order");
		assertEquals(List.of("gt6:crusher", "gt6:shredder", "gt6:sifter", "gt6:stone_anvil"),
				tStacks.stream().map(tStack -> BuiltInRegistries.ITEM.getKey(tStack.getItem()).toString()).toList(),
				"the table resolves exactly the four tier-0 chain machines");
		*///?}
		// and the guards: null displays / no displays are empty too, not errors
		assertTrue(MaterialTreeWorkstations.workstationStacks(null).isEmpty());
		assertTrue(MaterialTreeWorkstations.workstationStacks(List.of()).isEmpty());
	}

	// ------------------------------------------------------------------
	// the hook faces (bytecode layer — the offline recording double cannot run JEI/EMI)
	// ------------------------------------------------------------------

	/** The constant pool carries the method/class names exactly what loses the face if the call is dropped. */
	private static String classBytes(Class<?> aClass) throws Exception {
		try (java.io.InputStream in = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(in, aClass.getSimpleName() + " class resource not found on the test classpath");
			return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}

	@Test
	public void jeiPluginCatalystArmIsInTheBytecode() throws Exception {
		String tBytes = classBytes(gregtech6.jei.GT6JeiPlugin.class);
		assertTrue(tBytes.contains("addRecipeCatalysts"), "the JEI catalyst registration call is missing");
		assertTrue(tBytes.contains("MaterialTreeWorkstations"),
				"the material-tree catalyst arm is missing — clicking a chain machine never reaches the page in JEI");
	}

	@Test
	public void emiPluginWorkstationArmIsInTheBytecode() throws Exception {
		String tBytes = classBytes(gregtech6.emi.GT6EmiPlugin.class);
		assertTrue(tBytes.contains("addWorkstation"), "the EMI workstation registration call is missing");
		assertTrue(tBytes.contains("MaterialTreeWorkstations"),
				"the material-tree workstation arm is missing — clicking a chain machine never reaches the page in EMI");
	}
}
