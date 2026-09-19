package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

/**
 * The Matter Fabricator disintegration walk offline acceptance (task p31-massfab): the
 * Loader_Recipes_Other.java:969-987 element filter and the :971-981 unit/block arm
 * template — the iron constants (26p/30n → 7340032 ticks, 26 mB charged + 30 mB neutral,
 * eUt 1), the ×9 block arm, the proton-less arm skip (the :971 {@code NF} halves over
 * hydrogen 1p/0n), the {@code mat() → null} drop and the generation-tracked pour.
 *
 * <p>Fixture posture = the GT6RecipesImplosionTest convention: the vanilla registry is
 * frozen offline, so the material item seam answers vanilla stand-ins and the matter
 * fluids answer water/lava.
 */
public class GT6RecipesMassfabTest extends GTRecipesOfflineTestBase {

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;
	private static java.util.function.Function<String, Fluid> sDefaultMatterResolver;

	/** The offline fixture resolver: every walked arm prefix answers IRON_INGOT (identity), everything else null. */
	public static final BiFunction<OreDictPrefix, OreDictMaterial, Item> FIXTURE_RESOLVER = (aPrefix, aMaterial) ->
			GT6RecipesMassfab.unitPrefixes().contains(aPrefix) || GT6RecipesMassfab.blockPrefixes().contains(aPrefix)
					? Items.IRON_INGOT : null;

	/** The offline fixture matter fluids: charged → LAVA, neutral → WATER (identity is all the mechanics compare). */
	public static final java.util.function.Function<String, Fluid> FIXTURE_MATTER = aHalf ->
			aHalf.equals("charged") ? Fluids.LAVA : Fluids.WATER;

	@BeforeAll
	static void captureDefaults() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the offline material universe — OP prefixes AND the MT walk (the
		// GT6RecipeGenerationGuardTest form; bare MT.init() leaves OP.* null and the
		// result depends on the JVM class order, the neo-leg 7/9 red lesson)
		gregtech6.registry.GTMaterialItems.initMaterials();
		sDefaultMaterialResolver = GT6RecipesMassfab.sMaterialItemResolver;
		sDefaultMatterResolver = GT6RecipesMassfab.sMatterFluidResolver;
	}

	@BeforeEach
	void freshSeams() {
		// the offline registry is frozen: every walked arm resolves through the fixture
		// (the live GTMaterialItems seam answers null offline — no item registration)
		GT6RecipesMassfab.sMaterialItemResolver = FIXTURE_RESOLVER;
		GT6RecipesMassfab.sMatterFluidResolver = FIXTURE_MATTER;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesMassfab.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesMassfab.sMatterFluidResolver = sDefaultMatterResolver;
		GT6RecipeMaps.reset();
		GT6RecipesMassfab.resetForTest();
	}

	@AfterAll
	static void restoreEverything() {
		GT6RecipesMassfab.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesMassfab.sMatterFluidResolver = sDefaultMatterResolver;
	}

	// ------------------------------------------------------------------
	// the :969 element filter and the arm prefixes
	// ------------------------------------------------------------------

	@Test
	void theElementFilterWalksTheNucleateElements() {
		assertTrue(GT6RecipesMassfab.elements().size() > 100, "the element walk covers the periodic table bulk");
		assertTrue(GT6RecipesMassfab.elements().contains(MT.Fe), "iron (26p/30n, ELEMENT) rides the walk");
		assertFalse(GT6RecipesMassfab.elements().contains(MT.Steel), "the Steel ALLOY is not an ELEMENT (the :969 filter)");
		for (OreDictMaterial tMaterial : GT6RecipesMassfab.elements()) {
			assertTrue(tMaterial.mNeutrons + tMaterial.mProtons > 0, "every walked material carries nucleons");
			assertTrue(tMaterial.contains(gregapi.data.TD.Atomic.ELEMENT), "every walked material carries the ELEMENT tag");
			assertFalse(tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER), "antimatter never walks (the :969 filter)");
		}
	}

	@Test
	void theArmPrefixTablesPinTheUpstreamOrder() {
		assertEquals(5, GT6RecipesMassfab.unitPrefixes().size());
		assertEquals(5, GT6RecipesMassfab.blockPrefixes().size());
		// :971-975 then :977-981, upstream order verbatim
		assertEquals(OP.dust     , GT6RecipesMassfab.unitPrefixes().get(0));
		assertEquals(OP.ingot    , GT6RecipesMassfab.unitPrefixes().get(1));
		assertEquals(OP.plate    , GT6RecipesMassfab.unitPrefixes().get(2));
		assertEquals(OP.plateGem , GT6RecipesMassfab.unitPrefixes().get(3));
		assertEquals(OP.gem      , GT6RecipesMassfab.unitPrefixes().get(4));
		assertEquals(OP.blockDust    , GT6RecipesMassfab.blockPrefixes().get(0));
		assertEquals(OP.blockIngot   , GT6RecipesMassfab.blockPrefixes().get(1));
		assertEquals(OP.blockPlate   , GT6RecipesMassfab.blockPrefixes().get(2));
		assertEquals(OP.blockPlateGem, GT6RecipesMassfab.blockPrefixes().get(3));
		assertEquals(OP.blockGem     , GT6RecipesMassfab.blockPrefixes().get(4));
	}

	// ------------------------------------------------------------------
	// the :971-981 arm template — the iron constants and the multipliers
	// ------------------------------------------------------------------

	@Test
	void theIronUnitArmCarriesTheDisintegrationConstants() {
		Recipe tRow = GT6RecipesMassfab.buildRecipe(MT.Fe, OP.dust, 1);
		assertNotNull(tRow, "the iron dust arm resolves");
		assertEquals(1L, tRow.mEUt, "the disintegration voltage 1 (:971)");
		assertEquals(7340032L, tRow.mDuration, "(26 protons + 30 neutrons) × 131072 — the :971 formula (the qu-a smoke-row constants)");
		assertEquals(1, tRow.mInputs.length, "one item input");
		assertEquals(0, tRow.mOutputs.length, "zero item outputs — matter only");
		assertEquals(2, tRow.mFluidOutputs.length, "the two matter carriers out");
		assertEquals(26, tRow.mFluidOutputs[0].getAmount(), "1 mB = 1 proton: Fe charges to 26 mB");
		assertEquals(30, tRow.mFluidOutputs[1].getAmount(), "1 mB = 1 neutron: Fe neutrals to 30 mB");
	}

	@Test
	void theIronBlockArmScalesByNine() {
		Recipe tRow = GT6RecipesMassfab.buildRecipe(MT.Fe, OP.blockDust, 9);
		assertNotNull(tRow, "the iron blockDust arm resolves");
		assertEquals(7340032L * 9, tRow.mDuration, "the :977 ×9 duration");
		assertEquals(26 * 9, tRow.mFluidOutputs[0].getAmount(), "the :977 ×9 proton output");
		assertEquals(30 * 9, tRow.mFluidOutputs[1].getAmount(), "the :977 ×9 neutron output");
	}

	@Test
	void theProtonlessAndNeutronlessArmsSkipTheirHalf() {
		// hydrogen = 1 proton / 0 neutrons (MT.java:943 upstream form) — the :971 NF arms
		Recipe tRow = GT6RecipesMassfab.buildRecipe(MT.H, OP.dust, 1);
		assertNotNull(tRow, "the hydrogen arm resolves");
		assertEquals(1, tRow.mFluidOutputs.length, "hydrogen carries ONLY the charged half (mNeutrons < 1 → NF)");
		assertEquals(1, tRow.mFluidOutputs[0].getAmount(), "1 mB = the lone proton");
		assertEquals(131072L, tRow.mDuration, "(0 + 1) × 131072");
	}

	@Test
	void anUnresolvablePrefixItemDropsTheRow() {
		GT6RecipesMassfab.sMaterialItemResolver = (aPrefix, aMaterial) -> null; // the mat() → null shape
		assertNull(GT6RecipesMassfab.buildRecipe(MT.Fe, OP.dust, 1), "the upstream silent drop");
		assertNull(GT6RecipesMassfab.buildRecipe(MT.Fe, OP.gem, 1), "any arm drops");
	}

	// ------------------------------------------------------------------
	// the generation-tracked pour
	// ------------------------------------------------------------------

	@Test
	void thePourFillsTheMassfabMapWithTheWalkedArms() {
		assertFalse(GT6RecipesMassfab.elements().isEmpty());
		GT6RecipesMassfab.load();
		// every element × 10 arms (the fixture resolver answers every walked prefix) —
		// the map row count IS the walked arm count
		assertEquals(GT6RecipesMassfab.elements().size() * 10, GT6RecipeMaps.MASSFAB.mRecipeList.size(),
				"the pour covers every element's ten arms");
		// the Fe row constants survive the pour
		Recipe tFeRow = null;
		for (Recipe tRow : GT6RecipeMaps.MASSFAB.mRecipeList) {
			if (tRow.mDuration == 7340032L) {tFeRow = tRow; break;}
		}
		assertNotNull(tFeRow, "the iron 26p/30n row is in the map");
		assertEquals(26, tFeRow.mFluidOutputs[0].getAmount());
		// idempotent — the generation flag refuses a second pour
		GT6RecipesMassfab.load();
		assertEquals(GT6RecipesMassfab.elements().size() * 10, GT6RecipeMaps.MASSFAB.mRecipeList.size(), "the second load() is a no-op");
	}

	@Test
	void theIronRowDrivesTheMassfabRecipeLookup() {
		GT6RecipesMassfab.load();
		Recipe tFound = GT6RecipeMaps.MASSFAB.findRecipe(null, 2097152, null, null, new ItemStack[] {new ItemStack(Items.IRON_INGOT, 1), ItemStack.EMPTY, ItemStack.EMPTY});
		assertNotNull(tFound, "the map lookup answers the iron stand-in inside the :1241 input window");
		assertEquals(1L, tFound.mEUt);
	}
}
