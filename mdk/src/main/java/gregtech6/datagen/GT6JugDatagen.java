package gregtech6.datagen;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.minecraftforge.data.event.GatherDataEvent;

import gregtech6.registry.GT6Jugs;

/**
 * The Ceramic Jug crafting datagen (task small-tank-jug) — the
 * {@link GT6CupDatagen} form (the card-owned provider band appended by
 * {@link GT6DataGenerators}, the DataProvider-direct shape). Three rows, the clay
 * chain closure:
 * <ul>
 * <li><b>shaped</b> {@code gt6:clay_jug} — the upstream MultiItemRandomTools
 *     :133 row {@code "kCR"/"C C"/"CCC"} (C = the clay ball ×6 = the U*6
 *     OreDictItemData, k = the knife tool mark, R = the rolling-pin tool mark); the
 *     port form keeps the tool MARKS as the {@code #gt6:tools/knife} /
 *     {@code #gt6:tools/rolling_pin} tag defines (the clay-band law — the
 *     GT6MeasuringPotDatagen k/R form).</li>
 * <li><b>shapeless reverse</b> {@code gt6:clay_jug_reverse} — the upstream
 *     :120 tail: one raw → 6 clay balls.</li>
 * <li><b>smelt</b> {@code gt6:smelt_clay_jug} — the Loader_MultiTileEntities
 *     :2095 tail {@code RM.add_smelting(Ceramic_Jug_Raw → Ceramic_Jug)}, the raw
 *     hardening into the block item.</li>
 * </ul>
 *
 * <p>The crafting face is the forge-leg runData surface (the GT6CupDatagen
 * {@code //? if forge} gating and the tools/datagen_tree_check.py FORGE_GATED_ONLY_CANONICAL
 * registration; the KJS face is the plain vanilla JSON, naturally moddable).
 */
public final class GT6JugDatagen {

	private GT6JugDatagen() {}

	/**
	 * The provider band {@link GT6DataGenerators} appends to ITS listener — package-visible,
	 * deliberately NOT a {@code @SubscribeEvent} (the GT6CupDatagen.appendProviders
	 * form; the lang chain is untouched — the jug keys ride GT6EnUs/GT6ZhCn directly).
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

	/** The three-row provider (the GT6CupDatagen.Recipes DataProvider-direct form). */
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
			return "Recipes: gt6:ceramic_jug";
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
		private static net.minecraft.advancements.CriterionTriggerInstance has(ItemLike aItem) {
			return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(aItem);
		}

		/** The three rows (the leg-neutral builder chain). */
		private void build(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aOutput) {
			Item tRaw = GT6Jugs.CLAY_JUG_RAW.get();
			Item tJug = GT6Jugs.CERAMIC_JUG_ITEM.get();
			// :133 — "kCR"/"C C"/"CCC" (C clay, k knife mark, R rolling-pin mark): 6 clay + both tool marks
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tRaw)
					.pattern("kCR")
					.pattern("C C")
					.pattern("CCC")
					.define('C', Items.CLAY_BALL)
					.define('k', GT6ItemTags.TOOLS_KNIFE)
					.define('R', GT6ItemTags.TOOLS_ROLLING_PIN)
					.unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
					.save(aOutput, id("clay_jug"));
			// :120 — the reverse shapeless tail: one raw → 6 clay balls
			ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 6)
					.requires(tRaw)
					.unlockedBy("has_clay_jug", has(tRaw))
					.save(aOutput, id("clay_jug_reverse"));
			// Loader :2095 — RM.add_smelting(Ceramic_Jug_Raw → Ceramic_Jug), the :2177-band standard 200t/0xp face
			SimpleCookingRecipeBuilder.smelting(Ingredient.of(tRaw), RecipeCategory.MISC, tJug, 0.0F, 200)
					.unlockedBy("has_clay_jug", has(tRaw))
					.save(aOutput, id("smelt_clay_jug"));
		}

		private static ResourceLocation id(String aPath) {
			return new ResourceLocation("gt6", aPath);
		}
	}

	//?}

}
