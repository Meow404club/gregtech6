/**
 * Copyright (c) 2026 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Loader_OreProcessing.java:351 DUST_ORE row family (task debt-ore-purified-edge): the
 * pour walk, the pure planner and the pinned row shape. The upstream archaeology this pins:
 *
 * <ul>
 *   <li>the consumer machine is {@code RM.Sifting} (the Sifter) — NOT the Crusher; the
 *       Crusher's {@code RecipeMapHandlerCrushing:127-136} loop is the PREFIX-level
 *       {@code mPrefix.mByProducts} face, a different mechanism that stays dormant here;</li>
 *   <li>the listener family is :196-197 — ORE-tagged prefixes minus
 *       bedrock/poor/small/rich/normal; the crushed family carries
 *       {@code ORE_PROCESSING_BASED} (OP.java:135-140 setMaterialStats), NOT the ORE tag,
 *       so it never fires — the DUST_ORE gate :351 leaves exactly the five sand-family
 *       ore-BLOCK prefixes (OP.java:115-119);</li>
 *   <li>4 of the 5 have port families (gravel/sand/redsand/mud); oreStrangesand has none —
 *       the declared SKIPPED_UPSTREAM remainder, never an invented family;</li>
 *   <li>every axis material is self-crushing (the twelve MT.java:2884-2895 redirecters are
 *       off-axis), so every row is the same-material ore-block → crushedPurified hop —
 *       upstream has NO crushed → crushedPurified edge anywhere (the CrushedOres feed the
 *       Shredder/Mortar dust legs instead).</li>
 * </ul>
 *
 * <p>The offline fixture is the MaterialPrefixItem probe walk (the
 * GT6RecipeMapHashIndexTest.probeItem three-lock precedent hoisted per-class by
 * MaterialTreeBuilderTest): every (prefix, material) resolves to a registered probe so the
 * pour covers the full 4 x 53 walk offline.
 */
class GT6SifterDustOreRowsTest extends GTRecipesOfflineTestBase {

	private static final Map<PrefixMaterial, Item> PREFIX_PROBES = new LinkedHashMap<>();
	private static int sNextProbeId = 0;
	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultResolver;

	@BeforeAll
	static void bootUniverseAndOpenRegistry() {
		GTMaterialItems.initMaterials(); // the offline material universe (the ShCL convention)
		sDefaultResolver = GT6RecipesSifter.sMaterialItemResolver;
		openOfflineItemRegistry(); // the probeItem three-lock walk, ONCE for the whole class
	}

	/** The (prefix, material) -> probe-item resolver the sifter seam gets armed with.
	 * The gt6 namespace is load-bearing: a minecraft-namespaced probe would grow the
	 * frozen-vanilla pool GT6RecipesCokeOvenTest's synthetic universe re-deals on. */
	private static Item probeItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null; // the loaders' null-pair drop semantics
		return PREFIX_PROBES.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, new net.minecraft.resources.ResourceLocation("gt6", "dust_ore_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The offline item-registry unlock (GT6RecipeMapHashIndexTest.probeItem, hoisted once per class). */
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

	@BeforeEach
	void armProbeResolverAndFreshGeneration() {
		GT6RecipesSifter.sMaterialItemResolver = GT6SifterDustOreRowsTest::probeItem;
		GT6RecipeMaps.reset();
	}

	@AfterEach
	void restoreResolverAndDropGeneration() {
		GT6RecipesSifter.sMaterialItemResolver = sDefaultResolver;
		GT6RecipeMaps.reset();
		GT6RecipesSifter.resetForTest();
	}

	// ------------------------------------------------------------------
	// the family gate (acceptance ①: the walk's upstream 对照)
	// ------------------------------------------------------------------

	/** The DUST_ORE gate lands exactly the four port families; oreStrangesand stays family-less. */
	@Test
	void dustOreFamiliesAreTheFourPortFamilies() {
		List<OreDictPrefix> tPrefixes = GT6RecipesSifter.dustOreFamilies().stream().map(GT6OreBlocks.OreFamily::prefix).toList();
		assertEquals(4, tPrefixes.size(), "gravel + sand + redsand + mud — the DUST_ORE-tagged port families");
		assertTrue(tPrefixes.contains(OP.oreGravel) && tPrefixes.contains(OP.oreSand) && tPrefixes.contains(OP.oreRedSand) && tPrefixes.contains(OP.oreMud),
				"the four families by prefix, not by hardcoded name");
		for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
			if (tFamily.prefix() == OP.oreStrangesand) throw new AssertionError("oreStrangesand must have NO port family (the declared zero-row remainder)");
		}
	}

	// ------------------------------------------------------------------
	// the pure planner (:302/:311/:314/:328-343)
	// ------------------------------------------------------------------

	/** The planner pins: self-crushing targets, the multiplier 1, and the tiny tiers with fallbacks. */
	@Test
	void planPinsTargetsMultiplierAndTinyTiers() {
		GT6OreBlocks.OreFamily tGravel = GT6RecipesSifter.dustOreFamilies().get(0); // FAMILIES walk order: gravel first
		assertSame(OP.oreGravel, tGravel.prefix(), "the walk order sanity — gravel is the first DUST_ORE family");

		// Cu — the plain-metal face, MT.java:3827 first three byproducts
		GT6RecipesSifter.DustOrePlan tCu = GT6RecipesSifter.planDustOre(tGravel, MT.Cu);
		assertSame(MT.Cu, tCu.outMaterial(), "Cu is self-crushing (the twelve MT.java:2884-2895 redirecters are all off-axis)");
		assertEquals(1, tCu.multiplier(), "bindStack(mOreMultiplier x mOreProcessingMultiplier) = 1 for every axis material");
		assertEquals(List.of(MT.OREMATS.Cobaltite, MT.Au, MT.Ni), tCu.tinyMaterials(), "the :331-333 first-three tiers, MT.java:3827 order");

		// Fe2O3 — self too (Fe redirects TO Fe2O3; Fe2O3 itself never redirects), MT.java:3820
		GT6RecipesSifter.DustOrePlan tFe2O3 = GT6RecipesSifter.planDustOre(tGravel, MT.Fe2O3);
		assertSame(MT.Fe2O3, tFe2O3.outMaterial());
		assertEquals(List.of(MT.OREMATS.Ilmenite, MT.OREMATS.GraniticMineralSand, MT.MnO2), tFe2O3.tinyMaterials(), "MT.java:3820 order");

		// Garnierite — TWO declared byproducts (MT.java:3955): the :342-343 repeat-last pad
		GT6RecipesSifter.DustOrePlan tGarnierite = GT6RecipesSifter.planDustOre(tGravel, MT.OREMATS.Garnierite);
		assertEquals(List.of(MT.Ni, MT.OREMATS.Sperrylite, MT.OREMATS.Sperrylite), tGarnierite.tinyMaterials(),
				"two tiers + the tertiary = secondary fallback (:343)");

		// Amber — ZERO declared byproducts: the :341 self fallback fills all three tiers
		GT6RecipesSifter.DustOrePlan tAmber = GT6RecipesSifter.planDustOre(tGravel, MT.Amber);
		assertEquals(List.of(MT.Amber, MT.Amber, MT.Amber), tAmber.tinyMaterials(),
				"an empty mByProducts list pads to the material itself (:341-342)");
	}

	// ------------------------------------------------------------------
	// the pour (acceptance ①: 行数与材质循环覆盖报告)
	// ------------------------------------------------------------------

	/** 4 families x the 157-material axis + the grass row0 — the census pin. */
	@Test
	void pourWalkCoversFourFamiliesByTheWholeAxis() {
		GT6RecipesSifter.load();
		int tWalk = GT6RecipesSifter.dustOreFamilies().size() * GT6OreBlocks.materialAxis().size();
		assertEquals(628, tWalk, "4 families x the 157-material axis (GT6OreBlocksRegistrationTest's pinned axis)");
		assertEquals(1 + tWalk, GT6RecipeMaps.SIFTING.mRecipeList.size(), "the grass row0 + the whole DUST_ORE walk pours under the probe resolver");
	}

	/** The Cu row shape verbatim: 2x crushedPurified @10000 + 3 tiny dusts @1500/1000/500, 16 EUt, 256 ticks. */
	@Test
	void copperRowShapePinned() {
		GT6RecipesSifter.load();
		Item tGravelCu = probeItem(OP.oreGravel, MT.Cu);
		Recipe tRow = null;
		for (Recipe tCandidate : GT6RecipeMaps.SIFTING.mRecipeList) {
			if (tCandidate.mInputs[0].getItem() == tGravelCu) {tRow = tCandidate; break;}
		}
		assertNotNull(tRow, "the oreGravel.Cu row is in the pour");
		assertEquals(1, tRow.mInputs[0].getCount(), "1x the ore block (the :197 listener's registered stack)");
		assertEquals(5, tRow.mOutputs.length, "the two main outputs + the three tiny tiers");
		assertSame(OP.crushedPurified, prefixOf(tRow.mOutputs[0].getItem()), "slot 0: crushedPurified (:351 first main output)");
		assertSame(OP.crushedPurified, prefixOf(tRow.mOutputs[1].getItem()), "slot 1: the second 10000 main output (:351)");
		assertSame(MT.Cu, materialOf(tRow.mOutputs[0].getItem()));
		assertEquals(1, tRow.mOutputs[0].getCount(), "the multiplier-1 count");
		assertSame(OP.dustTiny, prefixOf(tRow.mOutputs[2].getItem()), "slot 2: the primary tiny (:331)");
		assertSame(MT.OREMATS.Cobaltite, materialOf(tRow.mOutputs[2].getItem()));
		assertSame(MT.Au, materialOf(tRow.mOutputs[3].getItem()), "slot 3: the secondary tiny (:332)");
		assertSame(MT.Ni, materialOf(tRow.mOutputs[4].getItem()), "slot 4: the tertiary tiny (:333)");
		assertEquals(10000, tRow.mChances[0]); assertEquals(10000, tRow.mChances[1]);
		assertEquals(1500, tRow.mChances[2]); assertEquals(1000, tRow.mChances[3]); assertEquals(500, tRow.mChances[4]);
		assertEquals(256, tRow.mDuration, "the :351 duration column");
		assertEquals(16, tRow.mEUt, "the :351 eUt column");
	}

	private static OreDictPrefix prefixOf(Item aItem) {
		return ((MaterialPrefixItem) aItem).prefix;
	}

	private static OreDictMaterial materialOf(Item aItem) {
		return ((MaterialPrefixItem) aItem).material;
	}
}
