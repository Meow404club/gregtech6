package gregtech6.datagen;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraftforge.data.event.GatherDataEvent;

import gregtech6.registry.GT6Foods;

/**
 * The meat smelting datagen (task food-meat-recipes) — the {@link GT6CupDatagen} form
 * (the card-owned provider band appended by {@link GT6DataGenerators}, the
 * DataProvider-direct shape). ONE row, and its existence is a DECLARED upstream bug
 * transcription:
 *
 * <p><b>DECLARED — the MultiItemFood.java:574 copy-paste line.</b> Upstream
 * {@code RM.add_smelting(IL.Food_DogMeat_Raw.get(1), IL.Food_DogMeat_Cooked.get(1), F,
 * T, F)} sits immediately after the MULE registration pair (:572-:573, between mule and
 * donkey) and is a character-for-character duplicate of the dogmeat line :560 — the
 * argument should have been {@code IL.Food_Mule_Raw → IL.Food_Mule_Cooked}. The
 * consequence is native upstream state: mule meat has NO smelting path while dogmeat's
 * line registers twice. This port transcribes the line VERBATIM (dogmeat_raw →
 * dogmeat_cooked, 照灌禁修正): correcting it to mule would be a content change, which
 * needs an explicit upstream-fix/content decision card. The mule smelting absence is
 * pinned by GT6MeatDatagenTest.
 *
 * <p>The row face is the standard smelting band (200 ticks, 0 xp — the GT6CupDatagen
 * :2094 face). The crafting face is the forge-leg runData surface (the {@code //? if
 * forge} gating and the tools/datagen_tree_check.py canonical registration; the KJS face
 * is the plain vanilla JSON, naturally moddable).
 */
public final class GT6MeatDatagen {

	private GT6MeatDatagen() {}

	/**
	 * The provider band {@link GT6DataGenerators} appends to ITS listener — package-visible,
	 * deliberately NOT a {@code @SubscribeEvent} (the GT6CupDatagen.appendProviders form).
	 */
	static void appendProviders(GatherDataEvent aEvent) {
		//? if forge {
		// the crafting face is the 1.20.1-forge runData surface this card drives; the 21.1
		// datagen flow (RecipeOutput) has no FinishedRecipe and stays the card-B future
		aEvent.getGenerator().addProvider(true,
				new Recipes(aEvent.getGenerator().getPackOutput(), aEvent.getLookupProvider()));
		//?}
	}

	//? if forge {

	/** The single-row provider (the GT6CupDatagen.Recipes DataProvider-direct form). */
	public static final class Recipes implements net.minecraft.data.DataProvider {

		private final PackOutput mOutput;
		/** The 21.1 run feeds saveStable the registries lookup; the 1.20.1 leg ignores it. */
		private final CompletableFuture<net.minecraft.core.HolderLookup.Provider> mLookup;

		public Recipes(PackOutput aOutput, CompletableFuture<net.minecraft.core.HolderLookup.Provider> aLookup) {
			mOutput = aOutput;
			mLookup = aLookup;
		}

		/** Unique provider name (the vanilla DataGenerator duplicate check). */
		@Override
		public String getName() {
			return "Recipes: gt6:meat_smelting";
		}

		@Override
		public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
			PackOutput.PathProvider tRecipePaths = mOutput.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
			java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
			java.util.Set<ResourceLocation> tSeen = new HashSet<>();
			build(tFinished -> {
				if (!tSeen.add(tFinished.getId())) throw new IllegalStateException("Duplicate recipe " + tFinished.getId());
				tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, GT6ForeignRowConvergence.converged(tFinished).serializeRecipe(), tRecipePaths.json(tFinished.getId())));
				// the unlock-advancement write stopped here (2026-10-03 user ruling, remember id1359:
				// JEI/EMI ubiquitous, the vanilla recipe book is dead weight) — the builders keep their
				// unlockedBy criteria (the builder API needs them); the JSON is simply never saved.
			});
			return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture<?>[0]));
		}

		/** The has() helper (the 1.20.1 unlockedBy criterion shape). */
		private static net.minecraft.advancements.CriterionTriggerInstance has(net.minecraft.world.level.ItemLike aItem) {
			return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(aItem);
		}

		/**
		 * The one row — MultiItemFood.java:574 VERBATIM (the :560 duplicate, the DECLARED
		 * bug transcription; do NOT "fix" it to mule here, see the class doc).
		 */
		private void build(Consumer<net.minecraft.data.recipes.FinishedRecipe> aOutput) {
			Item tRaw = item("food_dogmeat_raw");
			Item tCooked = item("food_dogmeat_cooked");
			// :574 — RM.add_smelting(Food_DogMeat_Raw → Food_DogMeat_Cooked), the standard 200t/0xp face
			SimpleCookingRecipeBuilder.smelting(Ingredient.of(tRaw), RecipeCategory.MISC, tCooked, 0.0F, 200)
					.unlockedBy("has_food_dogmeat_raw", has(tRaw))
					.save(aOutput, id("smelt_food_dogmeat"));
		}

		/** The FoodRow item walk (the FOODS table is the single registration face). */
		private static Item item(String aId) {
			for (int i = 0; i < GT6Foods.FOOD_ROWS.size(); i++)
				if (GT6Foods.FOOD_ROWS.get(i).id().equals(aId)) return GT6Foods.FOODS.get(i).get();
			throw new IllegalStateException("unknown FoodRow id: " + aId);
		}

		private static ResourceLocation id(String aPath) {
			return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
		}
	}

	//?}

}
