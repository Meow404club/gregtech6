package gregtech6.datagen;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.minecraftforge.data.event.GatherDataEvent;

import gregapi.data.OP;
import gregtech6.registry.GT6GasCylinders;
import gregtech6.registry.GTMaterialItems;

/**
 * The gas cylinder crafting datagen (task small-tank-gas-cylinder) — the
 * GT6MeasuringPotDatagen form (the card-owned provider band appended by
 * {@link GT6DataGenerators}, DataProvider-direct). Four rows, the Loader
 * :2101-2104 registration-line recipe tails verbatim:
 * <ul>
 * <li><b>shaped</b> {@code "RCR"/"BCh"/"TPd"} — 'R' = {@code OP.ring.dat(aMat)},
 *     'C' = {@code OP.plateCurved.dat(aMat)}, 'B' = {@code OP.round.dat(aMat)},
 *     'T' = {@code OP.screw.dat(aMat)}, 'P' = {@code OP.plate.dat(aMat)} (the
 *     GTMaterialItems concrete items — no tag families exist for these prefixes),
 *     and the tool marks 'h' = {@code #gt6:tools/hard_hammer} +
 *     'd' = {@code #gt6:tools/screwdriver} (the measuring-pot k/R tag-define form;
 *     the in-grid tools pay one point and ride along). All five material letters
 *     resolve through GTMaterialItems concrete items — no tag families exist for
 *     these prefixes, and one resolution shape for all five beats the hopper row's
 *     mixed tag+item form.
 * </ul>
 *
 * <p>The crafting face is the forge-leg runData surface (the GT6MeasuringPotDatagen
 * Recipes form — {@code //? if forge} gated, the canonical-only files declared in
 * tools/datagen_tree_check.py FORGE_GATED_ONLY_CANONICAL; the KJS face is the plain
 * vanilla JSON, naturally moddable).
 */
public final class GT6GasCylinderDatagen {

	private GT6GasCylinderDatagen() {}

	/**
	 * The provider band {@link GT6DataGenerators} appends to ITS listener — package-visible,
	 * deliberately NOT a {@code @SubscribeEvent} (the GT6CrucibleDatagen.appendProviders
	 * form; the lang chain is untouched — the cylinder keys ride GT6EnUs/GT6ZhCn directly).
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

	/** The four-row provider (the GT6MeasuringPotDatagen.Recipes DataProvider-direct form). */
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
			return "Recipes: gt6:gas_cylinders";
		}

		@Override
		public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
			PackOutput.PathProvider tRecipePaths = mOutput.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
			java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
			java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
			build(tFinished -> {
				if (!tSeen.add(tFinished.getId())) throw new IllegalStateException("Duplicate recipe " + tFinished.getId());
				tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tFinished.serializeRecipe(), tRecipePaths.json(tFinished.getId())));
				// the unlock-advancement write stopped here (2026-10-03 user ruling, remember id1359:
				// JEI/EMI ubiquitous, the vanilla recipe book is dead weight) — the builders keep their
				// unlockedBy criteria (the builder API needs them); the JSON is simply never saved.
			});
			return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture[0]));
		}

		/** The has() helper (the 1.20.1 unlockedBy criterion shape). */
		private static net.minecraft.advancements.CriterionTriggerInstance has(Item aItem) {
			return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(aItem);
		}

		/** The four rows (the :2101-2104 inline "RCR"/"BCh"/"TPd" grid per material). */
		private void build(Consumer<net.minecraft.data.recipes.FinishedRecipe> aOutput) {
			for (GT6GasCylinders.GasCylinderRow tRow : GT6GasCylinders.ROWS) {
				Item tCylinder = GT6GasCylinders.BLOCKS_IN_ORDER.get(GT6GasCylinders.ROWS.indexOf(tRow)).get().asItem();
				gregapi.oredict.OreDictMaterial tMat = tRow.material().get();
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tCylinder)
						.pattern("RCR")
						.pattern("BCh")
						.pattern("TPd")
						.define('R', GTMaterialItems.get(OP.ring, tMat).get())
						.define('C', GTMaterialItems.get(OP.plateCurved, tMat).get())
						.define('B', GTMaterialItems.get(OP.round, tMat).get())
						.define('T', GTMaterialItems.get(OP.screw, tMat).get())
						.define('P', GTMaterialItems.get(OP.plate, tMat).get())
						.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
						.define('d', GT6ItemTags.TOOLS_SCREWDRIVER)
						.unlockedBy("has_ring", has(GTMaterialItems.get(OP.ring, tMat).get()))
						.save(aOutput, id(tRow.path()));
			}
		}

		private static ResourceLocation id(String aPath) {
			return new ResourceLocation("gt6", aPath);
		}
	}

	//?}

}
