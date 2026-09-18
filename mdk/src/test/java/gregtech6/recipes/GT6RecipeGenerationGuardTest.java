package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The generation-reset guard (ADR-P18 staticinit poison fix, task p18-staticinit-generation-reset).
 *
 * <p><b>Behavioral canary</b> — "a bare {@link GT6RecipeMaps#reset()} (no loader
 * {@code resetForTest}) must retire the loader pour-flags with the maps, so a following
 * {@code load()} truly re-pours". This is the poison state itself: on the unfixed baseline
 * reset() cleared the 11 map fields but left every loader's private {@code sLoaded} set, so
 * the second {@code load()} silently early-returned and these canaries died on a null map
 * (NPE). They are kept self-grounding (an explicit paired reset + resetForTest at the head)
 * so the FIRST bare reset below is always the poison constructor, on both legs and both
 * boot states.
 *
 * <p><b>Structural pin</b> — every poison-capable loader (each holds a private static
 * poured flag) must register its {@code resetForTest} into the GT6RecipeMaps generation at
 * static-init. The hook ledger IS the poison-capable census: a loader that gains a static
 * pour-flag without registering turns this test red.
 */
class GT6RecipeGenerationGuardTest extends GTRecipesOfflineTestBase {

	/**
	 * The poison-capable census (ADR-P18 EVIDENCE): every loader holding a private static
	 * poured flag. This array is the live ledger — a new loader that gains a pour-flag must
	 * join here AND static-init register its resetForTest, or the structural pin below stays red.
	 */
	private static final String[] POISON_CAPABLE_LOADERS = {
			"gregtech6.recipes.GT6RecipesDistillery",
			"gregtech6.recipes.GT6RecipesDrying",
			"gregtech6.recipes.GT6RecipesBurnFuels",
			"gregtech6.recipes.GT6RecipesEngineFuels",
			"gregtech6.recipes.GT6RecipesCokeOven",
			"gregtech6.recipes.GT6RecipesOreChain",
			"gregtech6.recipes.GT6RecipesShCL",
			"gregtech6.recipes.GT6RecipesStoneChisel",
			"gregtech6.recipes.GT6RecipesCanner", // task p24-canner-machine — the refill pour joins the ledger
			"gregtech6.recipes.GT6RecipeMapJsonLoader", // task p26-tier-b-rm-json-loader — the JSON subset tracker joins the ledger
			"gregtech6.recipes.GT6RecipesMixer", // task p26-c-foam-fluid-refill — the C-Foam rock/Pd pour joins the ledger
			"gregtech6.recipes.GT6RecipesSifter", // task p26-w1-sifter-compressor-wiremill — the W1 trio joins the ledger
			"gregtech6.recipes.GT6RecipesCompressor",
			"gregtech6.recipes.GT6RecipesWiremill",
			"gregtech6.recipes.GT6RecipesExtruder", // task p26-w1-press-extruder-molds — the plate/rod pour joins the ledger
			"gregtech6.recipes.GT6RecipesPress", // task p26-w1-press-extruder-molds — the declared-empty pour joins the ledger
			"gregtech6.recipes.GT6RecipesBath", // task p26-kitchen-pot-bowl — the RM.Bath wood-oil pour joins the ledger
			"gregtech6.recipes.GT6RecipesAnvil", // task p28-c-anvil — the anvil grinding/bending pour joins the ledger
			"gregtech6.recipes.GT6RecipesWelder", // task p29-w3-nbtdesign-parts — the 22 welder wall rows join the ledger
			"gregtech6.recipes.GT6RecipesImplosion", // task p31-implosion — the Implosion Compressor 4-tier pour joins the ledger
			"gregtech6.recipes.GT6RecipesBees", // task p31-bees-lv1 — the 20+20 bee comb pour joins the ledger
		};

	private static Function<String, Fluid> sDefaultFluidResolver;
	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;

	@BeforeAll
	static void captureDefaults() {
		GTMaterialItems.initMaterials(); // the offline material universe (the OreChain test precedent)
		sDefaultFluidResolver = GT6RecipesEngineFuels.sFluidResolver;
		sDefaultMaterialResolver = GT6RecipesOreChain.sMaterialItemResolver;
	}

	@AfterEach
	void restoreSeams() {
		GT6RecipesEngineFuels.sFluidResolver = sDefaultFluidResolver;
		GT6RecipesOreChain.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest();
		GT6RecipesOreChain.resetForTest();
	}

	/** The EngineFuels canary: bare reset() → load() must re-pour the seven rows. */
	@Test
	void engineFuelsLoadRepoursAfterABareMapReset() {
		GT6RecipesEngineFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipeMaps.reset();
		GT6RecipesEngineFuels.resetForTest(); // self-grounding: boot state and prior classes never feed this test
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "the fixture pour (p12 pin: seven FM.Engine rows)");

		GT6RecipeMaps.reset(); // the BARE reset — no resetForTest: the shared-static poison constructor
		assertNull(GT6RecipeMaps.ENGINE_FUELS, "the bare reset dropped the map generation");
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(),
				"load() must truly re-pour after a bare reset() — the pour-flag must retire WITH the generation "
				+ "(unfixed baseline: sLoaded survives, load() silently early-returns, this line NPEs on the null map)");
	}

	/** The OreChain canary (the GT6RecipesOreChainTest stub-resolver form): bare reset() → load() must re-pour. */
	@Test
	void oreChainLoadRepoursAfterABareMapReset() {
		GT6RecipesOreChain.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.BRICK; // everything resolves
		GT6RecipeMaps.reset();
		GT6RecipesOreChain.resetForTest(); // self-grounding
		GT6RecipesOreChain.load();
		int tFirst = GT6RecipeMaps.CRUSHER.mRecipeList.size();
		assertTrue(tFirst > 0, "the stub-resolver pour");

		GT6RecipeMaps.reset(); // the BARE reset — the poison constructor
		assertNull(GT6RecipeMaps.CRUSHER, "the bare reset dropped the map generation");
		GT6RecipesOreChain.load();
		assertNotNull(GT6RecipeMaps.CRUSHER,
				"load() must re-init and re-pour after a bare reset() — the pour-flag must retire WITH the generation "
				+ "(unfixed baseline: sLoaded survives, load() silently early-returns, CRUSHER stays null here)");
		assertEquals(tFirst, GT6RecipeMaps.CRUSHER.mRecipeList.size(), "the second generation pours the same census");
	}

	/**
	 * The structural pin: all seven poison-capable loaders register a generation hook. The
	 * static initializer IS the registration seam, so touching the class is all it takes —
	 * no reflection into private state. A loader that gains a static pour-flag without
	 * registering (or a new poison-capable loader missing from the census) turns this red.
	 */
	@Test
	void everyPoisonCapableLoaderRegistersAGenerationResetHook() throws ClassNotFoundException {
		for (String tLoader : POISON_CAPABLE_LOADERS) {
			Class.forName(tLoader);
		}
		assertEquals(POISON_CAPABLE_LOADERS.length, GT6RecipeMaps.generationResetHooks().size(),
				"the hook ledger must equal the poison-capable loader census — every poured-flag loader must "
				+ "static-init register its resetForTest into the GT6RecipeMaps generation (ADR-P18)");
	}
}
