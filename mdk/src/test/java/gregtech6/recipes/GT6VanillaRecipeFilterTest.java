/**
 * Copyright (c) 2025 GregTech-6 Team
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
//? if forge {
// (1.20.1: no RecipeHolder — the id rides Recipe.getId() directly.)
//?} else {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}

import org.junit.jupiter.api.Test;

/**
 * Offline pins for the vanilla recipe removal channel: the 6-id whitelist table,
 * the exact-match filter (targets removed, everything else untouched) and the
 * idempotence of a repeated pass — exercised against a real RecipeManager double
 * through the same {@code getRecipes()}/{@code replaceRecipes()} seam the event
 * handlers hit.
 */
public class GT6VanillaRecipeFilterTest extends GTRecipesOfflineTestBase {

	/** The whitelist pin: exactly the six upstream removal ids, nothing else. */
	@Test
	public void removalTablePinsTheSixUpstreamIds() {
		assertEquals(
				List.of("minecraft:cake", "minecraft:cookie", "minecraft:golden_apple",
						"minecraft:golden_carrot", "minecraft:milk_bucket", "minecraft:water_bucket"),
				GT6VanillaRecipeFilter.REMOVAL_IDS.stream().map(ResourceLocation::toString).sorted().collect(Collectors.toList()),
				"the removal table drifted from the upstream CR.delate/CR.remove face");
		// the golden_apple × 2 guard: the NORMAL apple is in, the enchanted row
		// (Loader_Recipes_Vanilla.java:472) is a declared absence — no vanilla
		// enchanted_golden_apple recipe exists on either leg (vanilla jars,
		// data/minecraft/recipes[/recipe]/ — see the filter class doc).
		assertTrue(GT6VanillaRecipeFilter.shouldRemove(mc("golden_apple")));
		assertFalse(GT6VanillaRecipeFilter.shouldRemove(mc("enchanted_golden_apple")));
		// and the whitelisted ids must be exact — no prefix/predicate fuzzing.
		assertFalse(GT6VanillaRecipeFilter.shouldRemove(mc("cake_base")));
		assertFalse(GT6VanillaRecipeFilter.shouldRemove(mc("cookie_cutting")));
	}

	/** Whitelisted targets leave the graph; every non-target survives untouched. */
	@Test
	public void filterRemovesOnlyTheWhitelistedIds() {
		RecipeManager tManager = new TestRecipeManager();
		// the two bucket ids ship no vanilla recipe (declared no-op rows) — seeding
		// them here proves the exact-match contract still strips them if a datapack
		// ever adds one; crafting_table/furnace are the untouched-witness row.
		seed(tManager, mc("cake"), mc("water_bucket"), mc("milk_bucket"), mc("cookie"),
				mc("golden_apple"), mc("golden_carrot"), mc("crafting_table"), mc("furnace"));
		assertEquals(8, tManager.getRecipes().size(), "the fixture seeded the full graph");

		GT6VanillaRecipeFilter.apply(tManager);

		// liveIds is sorted, so this is a deterministic order-sensitive pin.
		assertEquals(List.of(mc("crafting_table"), mc("furnace")), liveIds(tManager),
				"exactly the six whitelisted ids leave the graph, the rest survive");
	}

	/** A second pass is a no-op: nothing left to remove, no duplicate-id throw, same graph. */
	@Test
	public void repeatedApplyIsIdempotent() {
		RecipeManager tManager = new TestRecipeManager();
		seed(tManager, mc("cake"), mc("cookie"), mc("golden_carrot"), mc("crafting_table"));

		GT6VanillaRecipeFilter.apply(tManager);
		List<ResourceLocation> tAfterFirst = liveIds(tManager);

		GT6VanillaRecipeFilter.apply(tManager);

		assertEquals(tAfterFirst, liveIds(tManager),
				"the second pass must not change the already-filtered graph");
		assertEquals(1, liveIds(tManager).size(), "only the non-target survives both passes");
	}

	private static ResourceLocation mc(String aPath) {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("minecraft", aPath);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("minecraft", aPath);
		*///?}
	}

	/** Seeds the manager through the same public replaceRecipes rebuild the filter itself uses. */
	private static void seed(RecipeManager aManager, ResourceLocation... aIds) {
		//? if forge {
		List<Recipe<?>> tRecipes = new ArrayList<>();
		for (ResourceLocation tId : aIds) {
			tRecipes.add(new FakeRecipe(tId));
		}
		aManager.replaceRecipes(tRecipes);
		//?} else {
		/*List<RecipeHolder<?>> tRecipes = new ArrayList<>();
		for (ResourceLocation tId : aIds) {
			tRecipes.add(new RecipeHolder<>(tId, new FakeRecipe()));
		}
		aManager.replaceRecipes(tRecipes);
		*///?}
	}

	/** Reads back the live id set through the same live view the filter reads. */
	private static List<ResourceLocation> liveIds(RecipeManager aManager) {
		//? if forge {
		return aManager.getRecipes().stream().map(aRecipe -> aRecipe.getId()).sorted().collect(Collectors.toList());
		//?} else {
		/*return aManager.getRecipes().stream().map(aHolder -> aHolder.id()).sorted().collect(Collectors.toList());
		*///?}
	}

	/**
	 * A recipe double good enough for the replaceRecipes rebuild (which reads only
	 * the id and the type) — the matcher/serializer faces stay inert.
	 */
	//? if forge {
	private static final class FakeRecipe implements Recipe<net.minecraft.world.Container> {
		private final ResourceLocation mId;

		FakeRecipe(ResourceLocation aId) { mId = aId; }

		@Override public ResourceLocation getId() { return mId; }

		@Override public boolean matches(net.minecraft.world.Container aContainer, net.minecraft.world.level.Level aLevel) { return false; }

		@Override public net.minecraft.world.item.ItemStack assemble(net.minecraft.world.Container aContainer, net.minecraft.core.RegistryAccess aAccess) { return net.minecraft.world.item.ItemStack.EMPTY; }

		@Override public net.minecraft.world.item.ItemStack getResultItem(net.minecraft.core.RegistryAccess aAccess) { return net.minecraft.world.item.ItemStack.EMPTY; }

		@Override public boolean canCraftInDimensions(int aWidth, int aHeight) { return false; }

		@Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SHAPELESS_RECIPE; }

		@Override public RecipeType<?> getType() { return RecipeType.CRAFTING; }
	}
	//?} else {
	/*// 21.1: the id rides the RecipeHolder wrapper (no getId override here) and the
	// input bound is RecipeInput.
	private static final class FakeRecipe implements Recipe<net.minecraft.world.item.crafting.RecipeInput> {
		@Override public boolean matches(net.minecraft.world.item.crafting.RecipeInput aInput, net.minecraft.world.level.Level aLevel) { return false; }

		@Override public net.minecraft.world.item.ItemStack assemble(net.minecraft.world.item.crafting.RecipeInput aInput, net.minecraft.core.HolderLookup.Provider aRegistries) { return net.minecraft.world.item.ItemStack.EMPTY; }

		@Override public net.minecraft.world.item.ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider aRegistries) { return net.minecraft.world.item.ItemStack.EMPTY; }

		@Override public boolean canCraftInDimensions(int aWidth, int aHeight) { return false; }

		@Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SHAPELESS_RECIPE; }

		@Override public RecipeType<?> getType() { return RecipeType.CRAFTING; }
	}
	*///?}
}
