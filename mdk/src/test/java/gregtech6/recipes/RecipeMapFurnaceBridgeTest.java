package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Acceptance ③ (offline equivalent): the RecipeManager bridge hits vanilla
 * smelting semantics — the genuine vanilla sand→glass smelting recipe JSON is
 * loaded through the real RecipeManager.apply() pipeline (the same protected
 * entry point the reload listener uses) and the RecipeMapFurnace bridge must
 * resolve it into a GT Recipe with mEUt=16 / mDuration=16
 * (RecipeMapFurnace.java:54/:154 semantics).
 *
 * <p>Server-level RCON verification of the same query happens with the W2
 * p4-machine-oven acceptance (the RCON command file belongs to that card's
 * files scope).
 */
class RecipeMapFurnaceBridgeTest extends GTRecipesOfflineTestBase {

	/** The genuine vanilla data/minecraft/recipes/glass.json (1.20.1). */
	private static final String VANILLA_GLASS_RECIPE_JSON =
			"{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:sand\"},\"result\":\"minecraft:glass\",\"experience\":0.1,\"cookingtime\":200}";

	private static final ResourceLocation GLASS_RECIPE_ID = new ResourceLocation("minecraft:glass");

	@BeforeEach
	void loadVanillaRecipe() {
		TestRecipeManager tManager = new TestRecipeManager();
		JsonObject tJson = new Gson().fromJson(VANILLA_GLASS_RECIPE_JSON, JsonObject.class);
		Map<ResourceLocation, JsonElement> tMap = new HashMap<>();
		tMap.put(GLASS_RECIPE_ID, tJson);
		tManager.load(tMap);
		mLevel = new MinimalLevel(tManager);
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	private MinimalLevel mLevel;

	@Test
	void recipeManagerActuallyLoadedTheRecipe() {
		ItemStack tSand = new ItemStack(Items.SAND);
		ItemStack tResult = RecipeMapFurnace.getSmeltingResult(mLevel, tSand);
		assertNotNull(tResult, "vanilla RecipeManager must resolve sand");
		assertEquals(Items.GLASS, tResult.getItem());
	}

	@Test
	void bridgeHitsSandToGlassWithFurnaceSemantics() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FURNACE);

		ItemStack tSand = new ItemStack(Items.SAND, 8);
		Recipe tRecipe = GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, null, tSand);

		assertNotNull(tRecipe, "findRecipe must hit the vanilla smelting query");
		assertEquals(16, tRecipe.mEUt, "upstream :154 semantics: mEUt=16");
		assertEquals(16, tRecipe.mDuration, "upstream :154 semantics: mDuration=16");
		assertEquals(Items.GLASS, tRecipe.mOutputs[0].getItem());
		assertEquals(1, tRecipe.mOutputs[0].getCount());
		assertEquals(Items.SAND, tRecipe.mInputs[0].getItem());
		assertEquals(1, tRecipe.mInputs[0].getCount(), "upstream :154: input normalized to amount 1");
		assertFalse(tRecipe.mCanBeBuffered, "generated recipes are not bufferable (upstream :154 first arg F)");
		assertEquals(0, tRecipe.mFluidInputs.length);
		assertEquals(0, tRecipe.mFluidOutputs.length);
	}

	@Test
	void bridgeIsLookupOnly() {
		GT6RecipeMaps.init();
		ItemStack tSand = new ItemStack(Items.SAND, 8);

		Recipe tRecipe = GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, null, tSand);
		assertNotNull(tRecipe);
		assertEquals(8, tSand.getCount(), "findRecipe must never consume the input");

		// The machine consumes via the pinned contract (upstream :725/:738 two-stage).
		assertTrue(tRecipe.isRecipeInputEqual(true, false, (FluidStack[])null, tSand));
		assertEquals(7, tSand.getCount());
	}

	@Test
	void lastRecipeFastPathReturnsCachedRecipe() {
		GT6RecipeMaps.init();
		ItemStack tSand = new ItemStack(Items.SAND, 8);

		Recipe tFirst = GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, null, tSand);
		assertNotNull(tFirst);
		Recipe tCached = GT6RecipeMaps.FURNACE.findRecipe(mLevel, tFirst, 64, null, null, new ItemStack(Items.SAND, 4));
		assertSame(tFirst, tCached, "upstream :53: the passed last recipe is returned when it still matches");
	}

	@Test
	void bridgeReturnsNullForNonSmeltablesAndEmptyInputs() {
		GT6RecipeMaps.init();
		assertNull(GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, null));
		assertNull(GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, null, new ItemStack[1]));
		assertNull(GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, null, new ItemStack(Items.DIAMOND_SWORD)), "swords are not smeltable");
		assertFalse(GT6RecipeMaps.FURNACE.containsInput(mLevel, new ItemStack(Items.DIAMOND_SWORD)));
		assertTrue(GT6RecipeMaps.FURNACE.containsInput(mLevel, new ItemStack(Items.SAND)));
	}

	@Test
	void bridgeRejectsFluidsButIgnoresTheirContent() {
		// The trimmed furnace map generates recipes with empty fluid arrays; fluids
		// passed by the machine are irrelevant to the vanilla smelting query.
		GT6RecipeMaps.init();
		FluidStack[] tFluids = {new FluidStack(Fluids.WATER, 1000)};
		Recipe tRecipe = GT6RecipeMaps.FURNACE.findRecipe(mLevel, null, 64, null, tFluids, new ItemStack(Items.SAND, 4));
		assertNotNull(tRecipe);
		assertEquals(1000, tFluids[0].getAmount(), "lookup must not drain machine fluids");
	}

	@Test
	void emptyRecipeManagerYieldsNoRecipe() {
		GT6RecipeMaps.init();
		MinimalLevel tEmptyLevel = new MinimalLevel(new RecipeManager());
		assertNull(GT6RecipeMaps.FURNACE.findRecipe(tEmptyLevel, null, 64, null, null, new ItemStack(Items.SAND, 4)));
		assertNull(RecipeMapFurnace.getSmeltingResult(tEmptyLevel, new ItemStack(Items.SAND)));
	}
}
