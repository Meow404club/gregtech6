package gregtech6.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import java.util.function.Consumer;

import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GT6Molds;

/**
 * The card-B datagen home (task p26-crucible-mold-faucet, the self-contained
 * GatherDataEvent subscriber — GT6DataGenerators/GT6CrucibleDatagen stay untouched).
 * ZERO hand-written JSON:
 * <ul>
 * <li><b>blockstates</b>: the 31 ceramic mold rows (blank + 30 pre-carved shapes) and
 *     the 2 faucet rows as placeholder cubes — the mold cube (the GT6CrucibleDatagen
 *     cobble placeholder style) and the faucet cube_all (the p12 addAttachments
 *     single-model-over-all-facings form, the oriented thin plate is the render pool).</li>
 * <li><b>item models</b>: the formed molds and faucets parent their block models; the
 *     31 raw clay items ride {@code item/generated} over the vanilla clay texture.</li>
 * <li><b>lang</b>: the composed display keys (en_us; the zh_cn walk rides the
 *     committed-tsv pipeline, this card adds none).</li>
 * <li><b>recipes</b>: the tier-a JSON faces — the VANILLA furnace hardening
 *     (raw → formed, the Loader_MultiTileEntities.java:391-420 RM.add_smelting family;
 *     the declared port deviation: one recipe per shape because the vanilla smelting
 *     JSON cannot emit item NBT), the clay handcraft chain (the blank :127 — the rolling
 *     pin is not ported, the declared clay-only form — and the vanilla donation set
 *     :136-153; the GT tool-head prefix loops :175-215 are the declared defer), and the
 *     two faucet crafts (:300 stone 3-stone; :305 ceramic raw → furnace).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GT6DataGenerators.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MoldDatagen {

	private GT6MoldDatagen() {}

	@SubscribeEvent
	public static void onGatherData(GatherDataEvent aEvent) {
		aEvent.getGenerator().addProvider(true,
				new Provider(aEvent.getGenerator().getPackOutput(), aEvent.getExistingFileHelper()));
		aEvent.getGenerator().addProvider(true,
				new Lang(aEvent.getGenerator().getPackOutput()));
		// the lookup provider rides along for the 21.1 saveStable face (the forge leg ignores it)
		aEvent.getGenerator().addProvider(true,
				new Recipes(aEvent.getGenerator().getPackOutput(), aEvent.getLookupProvider()));
	}

	// ------------------------------------------------------------------------------------
	// blockstates + models
	// ------------------------------------------------------------------------------------

	/** The mold placeholder body (the GT6CrucibleDatagen cobble placeholder style). */
	private static final String MOLD_TEXTURE = "block/stones/andesite/cobble";
	/** The faucet placeholder body (the andesite cobble; the oriented plate is the render pool). */
	private static final String FAUCET_TEXTURE = "block/stones/andesite/cobble";

	/** The blockstate/item-model provider. */
		public static final class Provider extends BlockStateProvider {

			public Provider(PackOutput aOutput, ExistingFileHelper aHelper) {
				super(aOutput, GT6DataGenerators.MOD_ID, aHelper);
			}

			/**
			 * Unique provider name — the vanilla BlockStateProvider default is
			 * {@code "Block States: gt6"} and the DataGenerator rejects a duplicate
			 * ("Duplicate provider: Block States: gt6", the GT6CrucibleDatagen
			 * Provider#getName precedent); this card's spec keeps
			 * GT6DataGenerators/GT6BlockStates untouched.
			 */
			@Override
			public String getName() {
				return "Block States: gt6:mold";
			}

			@Override
			protected void registerStatesAndModels() {
				// the ceramic molds: the blank + 30 shapes (the stone rung rides GT6CrucibleDatagen)
				registerMoldModels(GT6Molds.CERAMIC_BLANK_ROW);
				for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
					registerMoldModels(tRow);
				}
				// the faucets: cube_all placeholder over every FACING (the p12 addAttachments form)
				for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
					Block tBlock = GT6Molds.FAUCET_BLOCKS_BY_PATH.get(tRow.path()).get();
					simpleBlock(tBlock, models().cubeAll(tRow.path(), modLoc(FAUCET_TEXTURE)));
					itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
				}
			}

			/** One mold row: the placeholder cube blockstate + the formed item model + the raw clay sprite. */
			private void registerMoldModels(GT6Molds.MoldRow tRow) {
				Block tBlock = GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get();
				ModelFile tModel = models().cubeAll("block/" + tRow.path(), modLoc(MOLD_TEXTURE));
				simpleBlock(tBlock, tModel);
				itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
				// the paired raw clay item — a flat generated sprite over the VANILLA clay
				// texture (the explicit minecraft: namespace; ExistingFileHelper validates
				// layer0 against the known packs and gt6:block/clay does not exist)
				itemModels().withExistingParent(tRow.path() + "_raw", "item/generated")
						.texture("layer0", "minecraft:block/clay");
			}
		}

	// ------------------------------------------------------------------------------------
	// lang (en_us)
	// ------------------------------------------------------------------------------------

	/** The composed display keys. */
	public static final class Lang extends LanguageProvider {

		public Lang(PackOutput aOutput) {
			super(aOutput, GT6DataGenerators.MOD_ID, "en_us");
		}

		/** Unique provider name (the GT6EnUs collision; see {@link Provider#getName}). */
		@Override
		public String getName() {
			return "Language Provider: gt6:mold[en_us]";
		}

		@Override
		protected void addTranslations() {
			add("gt6.row.mold.display.mold_ceramic", "Ceramic Mold");
			add("item.gt6.mold_ceramic_raw", "Ceramic Mold (Raw)");
			for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
				String tName = shapeName(tRow.path());
				add("gt6.row.mold.display." + tRow.path(), "Ceramic " + tName + " Mold");
				add("item.gt6." + tRow.path() + "_raw", "Ceramic " + tName + " Mold (Raw)");
			}
			add("gt6.row.faucet.display", "%s Crucible Faucet");
			add("gt6.row.faucet.mat.stone", "Stone");
			add("gt6.row.faucet.mat.ceramic", "Ceramic");
			add("item.gt6.faucet_ceramic_raw", "Ceramic Crucible Faucet (Raw)");
		}

		/** {@code mold_ceramic_tiny_plate} → {@code Tiny Plate}. */
		private static String shapeName(String aPath) {
			String tTail = aPath.substring("mold_ceramic_".length());
			String[] tWords = tTail.split("_");
			StringBuilder r = new StringBuilder();
			for (String tWord : tWords) {
				if (r.length() > 0) r.append(' ');
				r.append(Character.toUpperCase(tWord.charAt(0))).append(tWord.substring(1));
			}
			return r.toString();
		}
	}

	// ------------------------------------------------------------------------------------
	// recipes (the tier-a JSON face)
	// ------------------------------------------------------------------------------------

	/**
	 * The handcrafts + the vanilla furnace hardening family. Implements DataProvider
	 * DIRECTLY instead of extending RecipeProvider — {@code RecipeProvider.getName()} is
	 * final on BOTH legs (1.20.1 sources :613-615, 21.1 sources :747) and returns a
	 * constant, so a second RecipeProvider would trip the vanilla "Duplicate provider"
	 * check against GT6CraftingRecipes (the GT6CrucibleDatagen.Recipes posture). Each
	 * leg's run() mirrors its vanilla RecipeProvider.run shape; the builder chains and
	 * the staged-row dispatch stay leg-neutral — ONLY the save consumer type and the
	 * has() criterion wrapper are forked.
	 */
	public static final class Recipes implements net.minecraft.data.DataProvider {

		private final PackOutput mOutput;
		/** The 21.1 run feeds saveStable the registries lookup; the 1.20.1 leg ignores it. */
		private final CompletableFuture<net.minecraft.core.HolderLookup.Provider> mLookup;

		public Recipes(PackOutput aOutput, CompletableFuture<net.minecraft.core.HolderLookup.Provider> aLookup) {
			mOutput = aOutput;
			mLookup = aLookup;
		}

		/** Unique provider name (the GT6CrucibleDatagen "Recipes: gt6:crucible" neighbour). */
		@Override
		public String getName() {
			return "Recipes: gt6:mold";
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

		/** The staged-row dispatch over the leg-typed save consumer. */
		//? if forge {
		private void build(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aOutput) {
			for (StagedRecipe tRow : stagedRows()) {
				if (tRow.shaped() != null) tRow.shaped().save(aOutput, tRow.id());
				else if (tRow.shapeless() != null) tRow.shapeless().save(aOutput, tRow.id());
				else tRow.smelting().save(aOutput, tRow.id());
			}
		}
		//?} else {
		/*private void build(net.minecraft.data.recipes.RecipeOutput aOutput) {
			for (StagedRecipe tRow : stagedRows()) {
				if (tRow.shaped() != null) tRow.shaped().save(aOutput, tRow.id());
				else if (tRow.shapeless() != null) tRow.shapeless().save(aOutput, tRow.id());
				else tRow.smelting().save(aOutput, tRow.id());
			}
		}
		*///?}

		/**
		 * One staged recipe: the leg-neutral builders + the id its save face writes (the
		 * GrassRecipeRow form — the save type is the one leg split; exactly one builder
		 * arm is set per row).
		 */
		private record StagedRecipe(ShapedRecipeBuilder shaped, ShapelessRecipeBuilder shapeless,
				SimpleCookingRecipeBuilder smelting, ResourceLocation id) {}

		/**
		 * The whole card-B recipe band, leg-neutral (MultiItemRandomTools.java:125-215 +
		 * Loader_MultiTileEntities.java:300/:305/:352/:391-420). ONLY the save face is
		 * forked (the GT6CraftingRecipes constructor rule: the builder chains are
		 * leg-identical, the consumer type is not).
		 */
		private static java.util.List<StagedRecipe> stagedRows() {
			java.util.List<StagedRecipe> rRows = new java.util.ArrayList<>();

			// the blank raw mold (MultiItemRandomTools.java:127, the rolling pin is not
			// ported — the declared clay-only form, 5 clay = the U*5 ceramic amount)
			Item tBlankRaw = GT6Molds.rawItemByPath("mold_ceramic");
			rRows.add(shaped(id("mold_ceramic_raw"), ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tBlankRaw)
					.pattern("C C")
					.pattern("CCC")
					.define('C', Items.CLAY_BALL)
					.unlockedBy("has_clay", has(Items.CLAY_BALL))));

			// the vanilla donation set (:136-153): blank raw + the shape's exemplar → shaped raw
			donation(rRows, "mold_ceramic_ingot", "from_brick", Items.BRICK);
			donation(rRows, "mold_ceramic_plate", "from_glass_pane", Items.GLASS_PANE);
			donationTag(rRows, "mold_ceramic_plate", "from_planks", ItemTags.PLANKS);
			donation(rRows, "mold_ceramic_arrow", "from_flint", Items.FLINT);
			donation(rRows, "mold_ceramic_arrow", "from_arrow", Items.ARROW);
			donation(rRows, "mold_ceramic_sword", "from_wooden_sword", Items.WOODEN_SWORD);
			donation(rRows, "mold_ceramic_sword", "from_stone_sword", Items.STONE_SWORD);
			donation(rRows, "mold_ceramic_pickaxe", "from_wooden_pickaxe", Items.WOODEN_PICKAXE);
			donation(rRows, "mold_ceramic_pickaxe", "from_stone_pickaxe", Items.STONE_PICKAXE);
			donation(rRows, "mold_ceramic_shovel", "from_wooden_shovel", Items.WOODEN_SHOVEL);
			donation(rRows, "mold_ceramic_shovel", "from_stone_shovel", Items.STONE_SHOVEL);
			donation(rRows, "mold_ceramic_axe", "from_wooden_axe", Items.WOODEN_AXE);
			donation(rRows, "mold_ceramic_axe", "from_stone_axe", Items.STONE_AXE);
			donation(rRows, "mold_ceramic_hoe", "from_wooden_hoe", Items.WOODEN_HOE);
			donation(rRows, "mold_ceramic_hoe", "from_stone_hoe", Items.WOODEN_HOE);
			donation(rRows, "mold_ceramic_file", "from_two_glass_panes", Items.GLASS_PANE, Items.GLASS_PANE);
			donationTag(rRows, "mold_ceramic_file", "from_two_planks", ItemTags.PLANKS, ItemTags.PLANKS);

			// the stone faucet (Loader:300, "h y","B B"," B " — the tool marks are the port cut)
			Item tStoneFaucet = GT6Molds.FAUCET_ITEMS_BY_PATH.get("faucet_stone").get();
			rRows.add(shaped(id("faucet_stone"), ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tStoneFaucet)
					.pattern("B B")
					.pattern(" B ")
					.define('B', Items.STONE)
					.unlockedBy("has_stone", has(Items.STONE))));

			// the ceramic faucet raw (Loader:305 craft, "C C","kCR" minus the tool marks)
			rRows.add(shaped(id("faucet_ceramic_raw"), ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6Molds.FAUCET_CERAMIC_RAW.get())
					.pattern("C C")
					.pattern(" C ")
					.define('C', Items.CLAY_BALL)
					.unlockedBy("has_clay", has(Items.CLAY_BALL))));

			// the vanilla furnace hardening family (:352/:391-420/:305) — raw → formed
			rRows.add(smeltingRow(id("smelt_mold_ceramic"), smeltingBuilder(tBlankRaw, GT6Molds.ITEMS_BY_PATH.get("mold_ceramic").get())));
			for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
				rRows.add(smeltingRow(id("smelt_" + tRow.path()),
						smeltingBuilder(GT6Molds.rawItemByPath(tRow.path()), GT6Molds.ITEMS_BY_PATH.get(tRow.path()).get())));
			}
			rRows.add(smeltingRow(id("smelt_faucet_ceramic"),
					smeltingBuilder(GT6Molds.FAUCET_CERAMIC_RAW.get(), GT6Molds.FAUCET_ITEMS_BY_PATH.get("faucet_ceramic").get())));

			return rRows;
		}

		/** The shape-kept factory trio — exactly one arm non-null, the save face types the call. */
		private static StagedRecipe shaped(ResourceLocation aId, ShapedRecipeBuilder aBuilder) { return new StagedRecipe(aBuilder, null, null, aId); }
		private static StagedRecipe shapelessRow(ResourceLocation aId, ShapelessRecipeBuilder aBuilder) { return new StagedRecipe(null, aBuilder, null, aId); }
		private static StagedRecipe smeltingRow(ResourceLocation aId, SimpleCookingRecipeBuilder aBuilder) { return new StagedRecipe(null, null, aBuilder, aId); }

		/** One vanilla donation recipe: blank raw + exemplars → the shaped raw mold. */
		private static void donation(java.util.List<StagedRecipe> aRows, String aFormedPath, String aIdTail, Item... aExemplars) {
			ShapelessRecipeBuilder tBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, GT6Molds.rawItemByPath(aFormedPath))
					.requires(GT6Molds.rawItemByPath("mold_ceramic"))
					.unlockedBy("has_blank", has(GT6Molds.rawItemByPath("mold_ceramic")));
			for (Item tExemplar : aExemplars) tBuilder.requires(tExemplar);
			aRows.add(shapelessRow(id("mold_" + aFormedPath.substring("mold_ceramic_".length()) + "_raw_" + aIdTail), tBuilder));
		}

		/** The tag-ingredient donation overload (the planks family). */
		private static void donationTag(java.util.List<StagedRecipe> aRows, String aFormedPath, String aIdTail, net.minecraft.tags.TagKey<Item>... aExemplars) {
			ShapelessRecipeBuilder tBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, GT6Molds.rawItemByPath(aFormedPath))
					.requires(GT6Molds.rawItemByPath("mold_ceramic"))
					.unlockedBy("has_blank", has(GT6Molds.rawItemByPath("mold_ceramic")));
			for (net.minecraft.tags.TagKey<Item> tTag : aExemplars) tBuilder.requires(tTag);
			aRows.add(shapelessRow(id("mold_" + aFormedPath.substring("mold_ceramic_".length()) + "_raw_" + aIdTail), tBuilder));
		}

		/** One furnace row builder: the raw clay item hardens into the formed block item (the RM.add_smelting family).
		 * The unlock criterion is mandatory — vanilla ensureValid rejects a recipe with no
		 * advancement trigger ("No way of obtaining recipe", SimpleCookingRecipeBuilder:109). */
		private static SimpleCookingRecipeBuilder smeltingBuilder(Item aRaw, Item aFormed) {
			return SimpleCookingRecipeBuilder.smelting(net.minecraft.world.item.crafting.Ingredient.of(aRaw), RecipeCategory.MISC, aFormed, 0.0F, 200)
					.unlockedBy("has_raw", has(aRaw));
		}

		private static ResourceLocation id(String aPath) {
			return new ResourceLocation("gt6", aPath);
		}
	}
}
