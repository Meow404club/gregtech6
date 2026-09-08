package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The W1 trio pour acceptance (task p26-w1-sifter-compressor-wiremill, the
 * {@link GT6RecipesShCLTest} offline shape): the sifter row0 pours over the vanilla
 * resolvers, the compressor/wiremill template walks pour over a synthetic
 * (prefix, material) → item resolver, and the generation reset re-pours (the pour flag
 * retires WITH the maps, the ADR-P18 poison-state guard).
 */
public class GT6KineticRecipesPourTest extends GTRecipesOfflineTestBase {

	private static final Item SYNTH = Items.IRON_INGOT;

	private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;
	private static java.util.function.Function<java.util.function.Supplier<Item>, Item> sDefaultVanillaResolver;

	@BeforeAll
	static void buildSyntheticUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		sDefaultMaterialResolver = GT6RecipesSifter.sMaterialItemResolver;
		sDefaultVanillaResolver = GT6RecipesSifter.sVanillaItemResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesSifter.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesSifter.sVanillaItemResolver = sDefaultVanillaResolver;
		GT6RecipesCompressor.sMaterialItemResolver = GT6RecipesSifter.sMaterialItemResolver;
		GT6RecipesWiremill.sMaterialItemResolver = GT6RecipesSifter.sMaterialItemResolver;
		GT6RecipeMaps.reset();
		GT6RecipesSifter.resetForTest();
		GT6RecipesCompressor.resetForTest();
		GT6RecipesWiremill.resetForTest();
	}

	private static void useSyntheticResolver() {
		GT6RecipesSifter.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTH;
		GT6RecipesCompressor.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTH;
		GT6RecipesWiremill.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTH;
	}

	/** The :224 grass row0 — the one all-vanilla sifter pour. */
	@Test
	void sifterPoursTheGrassRow0() {
		GT6RecipesSifter.load();
		assertNotNull(GT6RecipeMaps.SIFTING);
		assertEquals(1, GT6RecipeMaps.SIFTING.mRecipeList.size(), "the row0 pour — every other upstream arm is the declared pool");
		Recipe tRow = GT6RecipeMaps.SIFTING.mRecipeList.iterator().next();
		assertEquals(Items.GRASS_BLOCK, tRow.mInputs[0].getItem(), "the :224 grass-block input");
		assertEquals(5, tRow.mOutputs.length, "coarse dirt + three vanilla seeds + the beetroot identity");
		assertEquals(Items.COARSE_DIRT, tRow.mOutputs[0].getItem(), "the 1.7.10 dirt meta-1 identity");
		assertEquals(Items.BEETROOT_SEEDS, tRow.mOutputs[4].getItem(), "the EtFu beet-seed identity");
		assertEquals(5, tRow.mChances.length, "the chances prefix-trim rides the kept outputs");
		assertEquals(144, tRow.mDuration, "the :224 duration column");
		assertTrue(GT6RecipesSifter.SKIPPED_UPSTREAM.stream().anyMatch(aNote -> aNote.contains(":71")),
				"the audit finding stays declared: the Furnace:71/83/89 rows are RoC-compat-gated");
	}

	/** The compressor template walk pours both arms + the vanilla fixed rows. */
	@Test
	void compressorPoursBothArmsAndTheVanillaRows() {
		useSyntheticResolver();
		GT6RecipesCompressor.load();
		assertNotNull(GT6RecipeMaps.COMPRESSOR);
		assertTrue(GT6RecipeMaps.COMPRESSOR.mRecipeList.size() > 100, "the plate/dense walk x the registration order pours wide");
		long tFixed = GT6RecipeMaps.COMPRESSOR.mRecipeList.stream().filter(aRecipe -> aRecipe.mInputs[0].getItem() == Blocks.SAND.asItem()).count();
		assertEquals(1, tFixed, "the :654 sand→sandstone vanilla row is in the pour");
		// the easy arm: fixed 16-tick dust rows; the hard arm: getCosts(256) durations
		long tDustRows = GT6RecipeMaps.COMPRESSOR.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == SYNTH && aRecipe.mInputs[0].getCount() == 1).count();
		assertTrue(tDustRows >= 2, "the :217/:226 dust→plateGem arms both pour");
	}

	/** The wiremill template walk pours the stick/stickLong arms; the easy arm rides the fixed durations. */
	@Test
	void wiremillPoursTheStickArms() {
		useSyntheticResolver();
		GT6RecipesWiremill.load();
		assertNotNull(GT6RecipeMaps.WIREMILL);
		assertTrue(GT6RecipeMaps.WIREMILL.mRecipeList.size() > 50, "the :287-295 walk x the registration order pours");
		// the arm census, derived from the same gate the buildRecipe applies (the exact math):
		// easy rows carry the fixed durations 8/16/16/16, hard rows the getCosts(128) ≥ 128
		int tEasyStick = 0, tHardStick = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() != OP.stick) continue;
			OreDictMaterial tMaterial = tPair.material();
			boolean tEasy = tMaterial.contains(gregapi.data.TD.Processing.FURNACE) || tMaterial.contains(gregapi.data.TD.Properties.SOFT);
			if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER) || tMaterial.contains(gregapi.data.TD.Compounds.COATED)
					|| !tMaterial.contains(gregapi.data.TD.Processing.SMITHABLE)) continue;
			if (tEasy) tEasyStick++; else tHardStick++;
		}
		long tFixedEight = GT6RecipeMaps.WIREMILL.mRecipeList.stream().filter(aRecipe -> aRecipe.mDuration == 8).count();
		assertEquals(tEasyStick, tFixedEight, "every easy-arm stick material pours exactly one :292 8-tick row (only the :292 arm carries duration 8)");
		long tComputed = GT6RecipeMaps.WIREMILL.mRecipeList.stream().filter(aRecipe -> aRecipe.mDuration > 16).count();
		assertTrue(tComputed >= 2L * tHardStick,
				"every hard-arm stick/stickLong material pours its :287/:288 getCosts rows (durations ≥ 128 > 16); the ingot/compressed walks add more");
	}

	/** The generation reset re-pours: the pour flags retire WITH the maps (ADR-P18). */
	@Test
	void generationResetRepoursTheTrio() {
		useSyntheticResolver();
		GT6RecipesSifter.load();
		GT6RecipesCompressor.load();
		GT6RecipesWiremill.load();
		int tSifter = GT6RecipeMaps.SIFTING.mRecipeList.size();
		int tCompressor = GT6RecipeMaps.COMPRESSOR.mRecipeList.size();
		int tWiremill = GT6RecipeMaps.WIREMILL.mRecipeList.size();
		assertTrue(tSifter >= 1 && tCompressor > 0 && tWiremill > 0);
		GT6RecipeMaps.reset(); // the loader hooks retire the pour flags with the maps
		GT6RecipesSifter.load();
		GT6RecipesCompressor.load();
		GT6RecipesWiremill.load();
		assertEquals(tSifter, GT6RecipeMaps.SIFTING.mRecipeList.size(), "the fresh generation re-pours the sifter row");
		assertEquals(tCompressor, GT6RecipeMaps.COMPRESSOR.mRecipeList.size(), "the fresh generation re-pours the compressor tables");
		assertEquals(tWiremill, GT6RecipeMaps.WIREMILL.mRecipeList.size(), "the fresh generation re-pours the wiremill templates");
	}
}
