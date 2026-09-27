package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The declared-byproducts cross-table census (task debt-byproducts-data, acceptance ①): the
 * {@link OreByproductInfo} declared face pinned against the MT table, then held against the
 * DERIVED face — the byproduct edges the live recipe rows actually carry (the
 * MaterialTreeBuilder.ByproductEdge rule, test-local because the builder itself is card A
 * in-flight on another branch; the rule is one line: a row whose input carries X and whose
 * output carries Y != X yields the edge X -> Y).
 *
 * <p><b>The honest split</b> (ruling 2026-09-26-debt-material-tree): the declared face keeps
 * every upstream relation even where no recipe row produces it yet — the byproduct OUTPUT rows
 * (Sifter tiny dusts, the Loader_OreProcessing.java:331-333 tiers) ride the pooled DUST_ORE
 * ore-block cards, so over today's pour the sampled intersections are EMPTY and the
 * declared-only differences are the FULL lists. That is the point: 差集不消灭. When the pooled
 * cards land, these intersection assertions flip red on purpose — the expected-signal pattern
 * (the material-tree-a crushedPurified precedent).
 *
 * <p><b>The derived leg here</b> is the Crusher ore-chain census (GT6RecipesOreChain.load()
 * with one identity-carrying probe item per referenced (prefix, material) pair — the
 * GT6RecipeMapHashIndexTest probeItem pattern, hoisted once per class); the crushing redirects
 * (Fe -> Fe2O3, W -> OREMATS.Scheelite) are the derived byproduct edges that exist TODAY.
 */
class OreByproductInfoTest extends GTRecipesOfflineTestBase {

	/** One lazily created probe item per referenced (prefix, material) pair — the identity seam, offline. */
	private static final Map<PrefixMaterial, Item> PREFIX_ITEMS = new HashMap<>();
	private static int sNextProbeId = 0;
	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultResolver;

	@BeforeAll
	static void bootUniverseAndOpenRegistry() {
		GTMaterialItems.initMaterials(); // the offline material universe (the ShCL convention)
		sDefaultResolver = GT6RecipesOreChain.sMaterialItemResolver;
		openOfflineItemRegistry(); // the three-lock walk, ONCE for the whole class
	}

	@BeforeEach
	void pourCrusherCensus() {
		GT6RecipesOreChain.sMaterialItemResolver = OreByproductInfoTest::prefixItem;
		// self-grounding (ADR-P18): this test's pour must be THIS resolver's generation
		GT6RecipeMaps.reset();
		GT6RecipesOreChain.load();
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesOreChain.sMaterialItemResolver = sDefaultResolver;
		GT6RecipeMaps.reset();
		GT6RecipesOreChain.resetForTest();
	}

	// ------------------------------------------------------------------
	// declared face: the four pinned materials (upstream MT.java table, ported verbatim)
	// ------------------------------------------------------------------

	/** The pinned declared lists, with the repo table line carried in the assert messages. */
	@Test
	void declaredFacePinsFourMaterials() {
		// Fe2O3 — keyed via the Fe crushing redirect (MT.java:2885), list from MT.java:3820
		OreByproductInfo.Entry tFe2O3 = OreByproductInfo.of(MT.Fe2O3);
		assertNotNull(tFe2O3, "Fe2O3 must be keyed: every Fe ore registration redirects to it (:311)");
		assertEquals(MT.Fe2O3, tFe2O3.material());
		assertEquals(List.of(MT.OREMATS.Ilmenite, MT.OREMATS.GraniticMineralSand, MT.MnO2, MT.ClayRed), tFe2O3.byproducts(), "MT.java:3820 order verbatim");

		// Sphalerite — self-target ore, list from MT.java:3809
		OreByproductInfo.Entry tSphalerite = OreByproductInfo.of(MT.OREMATS.Sphalerite);
		assertNotNull(tSphalerite);
		assertEquals(List.of(MT.Cd, MT.Ga, MT.Zn, MT.OREMATS.Kesterite, MT.Se, MT.In), tSphalerite.byproducts(), "MT.java:3809 order verbatim");

		// Pitchblende — the first table row, MT.java:3780
		OreByproductInfo.Entry tPitchblende = OreByproductInfo.of(MT.OREMATS.Pitchblende);
		assertNotNull(tPitchblende);
		assertEquals(List.of(MT.Pb, MT.Ra, MT.RareEarth, MT.Th), tPitchblende.byproducts(), "MT.java:3780 order verbatim");

		// Cu — the plain-metal face, MT.java:3827
		OreByproductInfo.Entry tCu = OreByproductInfo.of(MT.Cu);
		assertNotNull(tCu);
		assertEquals(List.of(MT.OREMATS.Cobaltite, MT.Au, MT.Ni, MT.OREMATS.Malachite, MT.As), tCu.byproducts(), "MT.java:3827 order verbatim");
	}

	/** The redirect keying: Fe itself gets NO page (upstream keys the crushing target, :311). */
	@Test
	void redirectTargetsOwnThePages() {
		assertNull(OreByproductInfo.of(MT.Fe), "Fe has no page of its own — the Fe ores show the Fe2O3 page (:311)");
		assertNull(OreByproductInfo.of(MT.W), "W likewise shows the OREMATS.Scheelite page (MT.java:2888)");
	}

	/** The first-wins dedup (:306/:336): Nq_528 AND Nq_522 both crush into Nq — ONE Nq entry. */
	@Test
	void firstWinsDedupOnSharedCrushingTargets() {
		assertEquals(MT.Nq, GT6RecipesOreChain.crushingTarget(MT.Nq_528), "sanity: Nq_528 crushes into Nq (MT.java:2893)");
		assertEquals(MT.Nq, GT6RecipesOreChain.crushingTarget(MT.Nq_522), "sanity: Nq_522 too (:2894)");
		long tNqEntries = OreByproductInfo.entries().stream().filter(tEntry -> tEntry.material() == MT.Nq).count();
		assertEquals(1, tNqEntries, "two ore materials sharing one crushing target -> one declared entry (:336-338)");
	}

	/** Structural invariants: every entry keyed on a crushing target, byproducts non-empty, alias guards. */
	@Test
	void structuralInvariants() {
		List<OreByproductInfo.Entry> tEntries = OreByproductInfo.entries();
		assertFalse(tEntries.isEmpty());
		assertTrue(tEntries.size() >= 30, "the 309-line ore table keys far more than 30 faces (floor pin, not exact — the universe grows)");
		Set<OreDictMaterial> tTargets = new LinkedHashSet<>();
		for (OreDictMaterial tOre : GT6RecipesOreChain.expandOreMaterials()) tTargets.add(GT6RecipesOreChain.crushingTarget(tOre));
		for (OreByproductInfo.Entry tEntry : tEntries) {
			assertTrue(tTargets.contains(tEntry.material()), tEntry.material().mNameInternal + " must be a crushing target of the ore universe");
			assertFalse(tEntry.byproducts().isEmpty(), "the upstream :336 empty-list gate");
		}
		assertNull(OreByproductInfo.of(null));
	}

	// ------------------------------------------------------------------
	// the cross-table: declared face vs derived face (intersection / difference, as-is)
	// ------------------------------------------------------------------

	/**
	 * The acceptance cross-table: for the four pinned materials the derived intersection is
	 * EMPTY today (the byproduct OUTPUT rows ride the pooled DUST_ORE cards) and the
	 * declared-only difference is the FULL declared list — upstream semantics preserved, never
	 * trimmed to what the maps happen to contain. Expected-signal: flips when pooled cards land.
	 */
	@Test
	void declaredVsDerivedCrossTable() {
		Map<OreDictMaterial, Set<OreDictMaterial>> tDerived = derivedByproductEdges();
		assertFalse(tDerived.isEmpty(), "the crusher pour must carry the redirect edges");

		Map<OreDictMaterial, List<OreDictMaterial>> tDeclared = Map.of(
				MT.Fe2O3, List.of(MT.OREMATS.Ilmenite, MT.OREMATS.GraniticMineralSand, MT.MnO2, MT.ClayRed),
				MT.OREMATS.Sphalerite, List.of(MT.Cd, MT.Ga, MT.Zn, MT.OREMATS.Kesterite, MT.Se, MT.In),
				MT.OREMATS.Pitchblende, List.of(MT.Pb, MT.Ra, MT.RareEarth, MT.Th),
				MT.Cu, List.of(MT.OREMATS.Cobaltite, MT.Au, MT.Ni, MT.OREMATS.Malachite, MT.As));

		for (Map.Entry<OreDictMaterial, List<OreDictMaterial>> tRow : tDeclared.entrySet()) {
			OreDictMaterial tMaterial = tRow.getKey();
			Set<OreDictMaterial> tDerivedOf = tDerived.getOrDefault(tMaterial, Set.of());
			Set<OreDictMaterial> tIntersection = new LinkedHashSet<>(tRow.getValue());
			tIntersection.retainAll(tDerivedOf);
			assertTrue(tIntersection.isEmpty(), tMaterial.mNameInternal + ": no poured row outputs a declared byproduct of it yet (pooled DUST_ORE cards pending — expected signal)");
			assertEquals(tRow.getValue(), OreByproductInfo.of(tMaterial).byproducts(), tMaterial.mNameInternal + ": the declared-only difference stays the FULL list (差集不消灭)");
		}
	}

	/**
	 * The derived-only direction: the redirect edges Fe -> Fe2O3 and W -> OREMATS.Scheelite
	 * exist as actual crusher rows while Fe/W own no declared page — the two faces disagree
	 * honestly, exactly as upstream's face split does.
	 */
	@Test
	void redirectEdgesAreDerivedOnly() {
		Map<OreDictMaterial, Set<OreDictMaterial>> tDerived = derivedByproductEdges();
		assertTrue(tDerived.getOrDefault(MT.Fe, Set.of()).contains(MT.Fe2O3), "crusher: oreRaw.Fe -> crushed.Fe2O3 (GT6RecipesOreChain :2885 redirect)");
		assertTrue(tDerived.getOrDefault(MT.W, Set.of()).contains(MT.OREMATS.Scheelite), "crusher: oreRaw.W -> crushed.Scheelite (:2888)");
		assertNull(OreByproductInfo.of(MT.Fe), "and Fe still has no declared page — the derived face carries relations the declared face does not");
	}

	// ------------------------------------------------------------------
	// the test-local derived face (the ByproductEdge rule, one line) + probe fixtures
	// ------------------------------------------------------------------

	/**
	 * The derived byproduct edges over everything the maps actually contain (the ByproductEdge
	 * rule of MaterialTreeBuilder, test-local): input carries X, output carries Y != X.
	 */
	private static Map<OreDictMaterial, Set<OreDictMaterial>> derivedByproductEdges() {
		Map<OreDictMaterial, Set<OreDictMaterial>> rEdges = new LinkedHashMap<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			for (Recipe tRow : tMap.mRecipeList) {
				Set<OreDictMaterial> tInputs = materialsOf(tRow.mInputs);
				Set<OreDictMaterial> tOutputs = materialsOf(tRow.mOutputs);
				if (tInputs.isEmpty()) continue; // rows without a material-identity input leg carry no edge
				for (OreDictMaterial tIn : tInputs) for (OreDictMaterial tOut : tOutputs) {
					if (tIn != tOut) rEdges.computeIfAbsent(tIn, aKey -> new LinkedHashSet<>()).add(tOut);
				}
			}
		}
		return rEdges;
	}

	/** The material legs of a stack array — only MaterialPrefixItem legs carry the identity seam. */
	private static Set<OreDictMaterial> materialsOf(ItemStack[] aStacks) {
		Set<OreDictMaterial> rMaterials = new LinkedHashSet<>();
		if (aStacks == null) return rMaterials;
		for (ItemStack tStack : aStacks) {
			if (tStack != null && !tStack.isEmpty() && tStack.getItem() instanceof MaterialPrefixItem tPrefixItem) rMaterials.add(tPrefixItem.material);
		}
		return rMaterials;
	}

	/** The (prefix, material) -> probe-item resolver the material seam gets armed with. */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null; // the loaders' null-pair drop semantics
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, "byproduct_probe_" + sNextProbeId++,
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The offline item-registry unlock (GT6RecipeMapHashIndexTest.probeItem, hoisted to once-per-class). */
	private static void openOfflineItemRegistry() {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// three locks must open (the FileSawTest walk): the vanilla frozen flag, the
			// delegate ForgeRegistry.isFrozen, the NamespacedWrapper.locked register gate
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
		// 21.1: the plain vanilla DefaultedMappedRegistry — a single frozen flag
		java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
		throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
	}

	/** getDeclaredField along the superclass chain (the FileSawTest helper, mirrored). */
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
}
