package gregtech6.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;

import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.concurrent.CompletableFuture;

import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Molds;

/**
 * The crucible-chain datagen home (task p26-crucible-physics-smeltery spec ⑦, the
 * self-contained GatherDataEvent subscriber — GT6DataGenerators stays untouched). All
 * four faces are datagen-native; ZERO hand-written JSON:
 * <ul>
 * <li><b>blockstates</b>: per crucible block NINE variants over the
 *     {@link GT6Crucibles.CrucibleBlock#LIQUID_LEVEL} int property (0..8, the
 *     mDisplayedHeight :298 census bucketed) — level 0 the cobble cube, levels 1..8 the
 *     "filled" model over the molten-indicator texture. Declared deviation: the fill
 *     HEIGHT within the cube (the upstream 6-pass setBlockBounds2 render, :596-606) is
 *     the defer pool — element-based per-level models need the render card; the
 *     property + variants face this card promised is delivered.</li>
 * <li><b>item models</b>: the BlockItems parent their block models.</li>
 * <li><b>lang</b>: the four composed display keys (the GT6Crucibles/GT6Molds getName
 *     carriers).</li>
 * <li><b>crafting</b>: the Stone Smeltery (8 cobblestone — the upstream id-1000 opening
 *     row) and the Stone Mold (7 cobblestone — the card face), the tier-a crafting JSON
 *     universe (KJS: the naturally moddable face).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GT6DataGenerators.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CrucibleDatagen {

	private GT6CrucibleDatagen() {}

	@SubscribeEvent
	public static void onGatherData(GatherDataEvent aEvent) {
		aEvent.getGenerator().addProvider(true,
				new Provider(aEvent.getGenerator().getPackOutput(), aEvent.getExistingFileHelper()));
		aEvent.getGenerator().addProvider(true,
				new Lang(aEvent.getGenerator().getPackOutput()));
		// task p24-tool-system ③ posture: the recipes ride the two-arg RecipeProvider
		// ctor on both legs (the forge leg ignores the lookup provider)
		aEvent.getGenerator().addProvider(true,
				new Recipes(aEvent.getGenerator().getPackOutput(), aEvent.getLookupProvider()));
	}

	// ------------------------------------------------------------------------------------
	// blockstates + models
	// ------------------------------------------------------------------------------------

	/** The cobble body texture (the andesite cobble of the borrowed stones universe). */
	private static final String BODY_TEXTURE = "block/stones/andesite/cobble";
	/** The molten-content indicator (the script-generated placeholder PNG, the p2 pipeline). */
	private static final String CONTENT_TEXTURE = "block/smeltery_content";

	/** The blockstate/item-model provider. */
	public static final class Provider extends BlockStateProvider {

		public Provider(PackOutput aOutput, ExistingFileHelper aHelper) {
			super(aOutput, GT6DataGenerators.MOD_ID, aHelper);
		}

		/**
		 * Unique provider name — the vanilla DataGenerator rejects two providers with the
		 * same name ("Duplicate provider: Block States: gt6", the GT6BlockStates collision)
		 * and this card's spec keeps GT6DataGenerators/GT6BlockStates untouched.
		 */
		@Override
		public String getName() {
			return "Block States: gt6:crucible";
		}

		@Override
		protected void registerStatesAndModels() {
			for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
				Block tBlock = GT6Crucibles.BLOCKS_BY_PATH.get(tRow.path()).get();
				addCrucible(tBlock, tRow.path());
			}
			for (GT6Molds.MoldRow tRow : GT6Molds.ROWS) {
				Block tBlock = GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get();
				addSimpleCube(tBlock, "block/" + tRow.path());
			}
		}

		/** One crucible: 9 LIQUID_LEVEL variants + the BlockItem parent. */
		private void addCrucible(Block aBlock, String aPath) {
			String tEmpty = "block/" + aPath + "_empty";
			String tFilled = "block/" + aPath + "_filled";
			ModelFile tEmptyModel = models().cubeAll(tEmpty, modLoc(BODY_TEXTURE));
			ModelFile tFilledModel = models().cubeAll(tFilled, modLoc(CONTENT_TEXTURE));
			getVariantBuilder(aBlock).forAllStates(aState -> {
				int tLevel = aState.getValue(GT6Crucibles.CrucibleBlock.LIQUID_LEVEL);
				return ConfiguredModel.builder().modelFile(tLevel == 0 ? tEmptyModel : tFilledModel).build();
			});
			itemModels().withExistingParent(aPath, modLoc(tEmpty));
		}

		/** One plain cube carrier (the mold blocks). */
		private void addSimpleCube(Block aBlock, String aModelName) {
			ModelFile tModel = models().cubeAll(aModelName, modLoc(BODY_TEXTURE));
			simpleBlock(aBlock, tModel);
			itemModels().withExistingParent(aModelName.substring("block/".length()), modLoc(aModelName));
		}
	}

	// ------------------------------------------------------------------------------------
	// lang (en_us; the zh_cn walk rides its committed-tsv pipeline, the card adds none)
	// ------------------------------------------------------------------------------------

	/**
	 * The four display keys (the GT6Crucibles/GT6Molds getName carriers). Subclasses the
	 * GT6EnUs provider on purpose: a standalone LanguageProvider would clobber
	 * en_us.json (LanguageProvider.finish rewrites the whole file — the later-registered
	 * provider would wipe every GT6EnUs key), so this provider replays the full base
	 * translation set plus the four crucible keys.
	 */
	public static final class Lang extends GT6EnUs {

		public Lang(PackOutput aOutput) {
			super(aOutput);
		}

		/** Unique provider name (the GT6EnUs collision, see {@link Provider#getName}). */
		@Override
		public String getName() {
			return "Language Provider: gt6:crucible[en_us]";
		}

		@Override
		protected void addTranslations() {
			super.addTranslations(); // the full base set — the file must stay complete
			add("gt6.row.crucible.display.smeltery_stone", "Stone Smeltery");
			add("gt6.row.crucible.display.smeltery_bronze", "Bronze Smeltery");
			add("gt6.row.crucible.display.smeltery_steel", "Steel Smeltery");
			for (GT6Molds.MoldRow tRow : GT6Molds.ROWS) {
				add("gt6.row.mold.display." + tRow.path(), "Stone Mold");
			}
		}
	}

	// ------------------------------------------------------------------------------------
	// crafting (the tier-a JSON face)
	// ------------------------------------------------------------------------------------

	/**
	 * The two handcrafts: 8 cobblestone → Stone Smeltery (the upstream opening row), 7 →
	 * Stone Mold. Implements DataProvider DIRECTLY instead of extending RecipeProvider:
	 * {@code RecipeProvider.getName() final} on BOTH legs (1.20.1 sources jar :461, the
	 * 21.1 neoforge-21.1.249 sources RecipeProvider.java:747), so a second recipe provider
	 * would trip the vanilla "Duplicate provider" check against GT6CraftingRecipes and
	 * files_scope keeps that file untouched. Each leg's run() mirrors its vanilla
	 * RecipeProvider.run shape: 1.20.1 = serializeRecipe() + saveStable through the
	 * DATA_PACK path providers; 1.21.1 = the RecipeOutput accept face (RecipeProvider.java
	 * :81-113 of the 21.1 sources — CONDITIONAL_CODEC + WithConditions over the
	 * createRegistryElementsPathProvider paths).
	 */
	public static final class Recipes implements net.minecraft.data.DataProvider {

		private final PackOutput mOutput;
		/** The 21.1 run feeds saveStable the registries lookup; the 1.20.1 leg ignores it. */
		private final CompletableFuture<net.minecraft.core.HolderLookup.Provider> mLookup;

		public Recipes(PackOutput aOutput, CompletableFuture<net.minecraft.core.HolderLookup.Provider> aLookup) {
			mOutput = aOutput;
			mLookup = aLookup;
		}

		/** Unique provider name (see {@link Provider#getName}). */
		@Override
		public String getName() {
			return "Recipes: gt6:crucible";
		}

		//? if forge {
		@Override
		public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
			PackOutput.PathProvider tRecipePaths = mOutput.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
			PackOutput.PathProvider tAdvancementPaths = mOutput.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
			java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
			java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
			build(tFinished -> {
				if (!tSeen.add(tFinished.getId())) throw new IllegalStateException("Duplicate recipe " + tFinished.getId());
				tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tFinished.serializeRecipe(), tRecipePaths.json(tFinished.getId())));
				com.google.gson.JsonObject tAdvancement = tFinished.serializeAdvancement();
				if (tAdvancement != null) {
					tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tAdvancement, tAdvancementPaths.json(tFinished.getAdvancementId())));
				}
			});
			return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture[0]));
		}
		//?} else {
		/*@Override
		public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
			PackOutput.PathProvider tRecipePaths = mOutput.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.RECIPE);
			PackOutput.PathProvider tAdvancementPaths = mOutput.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.ADVANCEMENT);
			return mLookup.thenCompose(tRegistries -> {
				java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
				java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
				build(new net.minecraft.data.recipes.RecipeOutput() {
					@Override
					public void accept(ResourceLocation aId, net.minecraft.world.item.crafting.Recipe<?> aRecipe,
							net.minecraft.advancements.AdvancementHolder aAdvancement,
							net.neoforged.neoforge.common.conditions.ICondition... aConditions) {
						if (!tSeen.add(aId)) throw new IllegalStateException("Duplicate recipe " + aId);
						tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tRegistries,
								net.minecraft.world.item.crafting.Recipe.CONDITIONAL_CODEC,
								java.util.Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(aRecipe, aConditions)),
								tRecipePaths.json(aId)));
						if (aAdvancement != null) {
							tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tRegistries,
									net.minecraft.advancements.Advancement.CONDITIONAL_CODEC,
									java.util.Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(aAdvancement.value(), aConditions)),
									tAdvancementPaths.json(aAdvancement.id())));
						}
					}

					@Override
					public net.minecraft.advancements.Advancement.Builder advancement() {
						return net.minecraft.advancements.Advancement.Builder.recipeAdvancement()
								.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
					}
				});
				return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture[0]));
			});
		}
		*///?}

		/** The has() helper — the one criterion shape is leg-split (1.20.1 unlockedBy takes the bare TriggerInstance, 1.21.1 a Criterion wrapper). */
		//? if forge {
		private static net.minecraft.advancements.CriterionTriggerInstance has(net.minecraft.world.level.ItemLike aItem) {
			return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(aItem);
		}
		//?} else {
		/*private static net.minecraft.advancements.Criterion<?> has(net.minecraft.world.level.ItemLike aItem) {
			return net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.createCriterion(
					new net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance(
							java.util.Optional.empty(),
							net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.Slots.ANY,
							java.util.List.of(net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(aItem).build())));
		}
		*///?}

		/** The two shaped rows (the leg-neutral builder chain over the forked save consumer). */
		//? if forge {
		private void build(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aOutput) {
		//?} else {
		/*private void build(net.minecraft.data.recipes.RecipeOutput aOutput) {
		 *///?}
			Item tStoneSmeltery = GT6Crucibles.ITEMS_BY_PATH.get("smeltery_stone").get();
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tStoneSmeltery)
					.pattern("BBB")
					.pattern("B B")
					.pattern("BBB")
					.define('B', Items.COBBLESTONE)
					.unlockedBy("has_cobblestone", has(Items.COBBLESTONE))
					.save(aOutput, id("smeltery_stone"));
			Item tStoneMold = GT6Molds.ITEMS_BY_PATH.get("mold_stone").get();
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tStoneMold)
					.pattern("B B")
					.pattern("B B")
					.pattern("BBB")
					.define('B', Items.COBBLESTONE)
					.unlockedBy("has_cobblestone", has(Items.COBBLESTONE))
					.save(aOutput, id("mold_stone"));
		}

		private static ResourceLocation id(String aPath) {
			return new ResourceLocation("gt6", aPath);
		}
	}

}
