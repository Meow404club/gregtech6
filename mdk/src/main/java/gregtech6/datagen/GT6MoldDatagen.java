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

import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

import gregtech6.registry.GT6Molds;

/**
 * The card-B datagen home (task crucible-mold-faucet). Since task
 * ops-datagen-lang-order the provider band folds into {@link GT6DataGenerators#onGatherData}
 * — this class is NO LONGER a {@code @Mod.EventBusSubscriber}: three self-contained
 * GatherDataEvent subscribers ordered themselves by the annotation-scan lottery and the
 * per-family full-file lang writers made the last one win (the p30 card's live repro: an
 * incremental recompile flipped the scan order and a cache-cold runData landed en_us.json
 * 66 keys short).
 * ZERO hand-written JSON:
 * <ul>
 * <li><b>blockstates</b>: the 32 mold rows (stone + the 31 ceramic — blank + 30
 *     pre-carved shapes, the one upstream MTE design 1072, Loader:347/:352/:391-420) as
 *     the 5x5 bitmap stamp models (issue #41: the CONCAVE mold — the 1px floor + the four
 *     2px walls + the 12 wall-top rim handles (MOLD_BOUNDS[6..17]) + one 2.4x3x2.4px
 *     element per UNLIT bit, the lit bit = chiseled out,
 *     MultiTileEntityMold.java:328-335/:537) riding the MATERIAL SMOOTH body texture
 *     ({@link GT6CrucibleDatagen#bodyTexture}, task 40-41-mold-assets — the former
 *     flat andesite/cobble placeholder is gone), and the 2 faucet rows as material smooth
 *     body cubes (the p12 addAttachments single-model-over-all-facings form, the oriented
 *     thin plate is the render pool).</li>
 * <li><b>item models</b>: the formed molds and faucets parent their block models; the
 *     31 raw clay items ride {@code item/generated} over their OWN borrowed upstream
 *     icon ({@code item/<path>_raw}, the gt.multiitem.randomtools 900-929/991 borrows,
 *     assets/README.md — task 40-41-mold-assets; the former shared vanilla clay
 *     sprite made every shape look identical, GitHub #40).</li>
 * <li><b>lang</b>: the composed display keys (en_us; the zh_cn walk rides the
 *     committed-tsv pipeline, this card adds none).</li>
 * <li><b>recipes</b>: the tier-a JSON faces — the VANILLA furnace hardening
 *     (raw → formed, the Loader_MultiTileEntities.java:391-420 RM.add_smelting family;
 *     the declared port deviation: one recipe per shape because the vanilla smelting
 *     JSON cannot emit item NBT), the clay handcraft chain (the blank :127 and the
 *     faucet :128 rows now carry the upstream {@code k=knife}/{@code R=rollingpin}
 *     tool marks as the {@code #gt6:tools/knife}/{@code #gt6:tools/rolling_pin} tag
 *     defines, issue #45 C1 — the p24 tool seams take the 1-damage craft toll — and
 *     the vanilla donation set :136-153; the GT tool-head prefix loops :175-215 are
 *     the declared defer), and the two faucet crafts (:300 stone 3-stone; :305
 *     ceramic raw → furnace).</li>
 * </ul>
 */
public final class GT6MoldDatagen {

	private GT6MoldDatagen() {}

	/**
	 * The provider band {@link GT6DataGenerators} appends to ITS listener — package-visible,
	 * deliberately NOT a {@code @SubscribeEvent}. The Lang face is registered ONCE, by
	 * {@link GT6DataGenerators} itself (this chain tail is the single en_us writer); only the
	 * blockstate/model and recipe providers ride here.
	 */
	static void appendProviders(GatherDataEvent aEvent) {
		aEvent.getGenerator().addProvider(true,
				new Provider(aEvent.getGenerator().getPackOutput(), aEvent.getExistingFileHelper()));
		// the lookup provider rides along for the 21.1 saveStable face (the forge leg ignores it)
		aEvent.getGenerator().addProvider(true,
				new Recipes(aEvent.getGenerator().getPackOutput(), aEvent.getLookupProvider()));
	}

	// ------------------------------------------------------------------------------------
	// blockstates + models
	// ------------------------------------------------------------------------------------

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
					// the stone rung: the SAME mold geometry (Loader:347 — stone and ceramic
					// molds are the one MTE design 1072; the former addSimpleCube flat-cube
					// placeholder retires here, the debt-mold-stone-visual-sync ride)
					for (GT6Molds.MoldRow tRow : GT6Molds.ROWS) {
						Block tBlock = GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get();
						simpleBlock(tBlock, moldModel(tRow, tRow.preCarvedShape()));
						itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
					}
					// the ceramic molds: the blank + 30 shapes
					registerMoldModels(GT6Molds.CERAMIC_BLANK_ROW);
				for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
					registerMoldModels(tRow);
				}
				// the faucets: cube_all over every FACING (the p12 addAttachments form), the
				// material smooth body (task 40-41-mold-assets — the former flat cobble
				// placeholder is gone); the grayscale-borrow rows carry tintindex 0 (task
				// debt-material-tint), the vanilla smooth-stone row stays the finished
				// texture (a second multiply would dirty it — the recorded declaration shortcut)
				for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
					Block tBlock = GT6Molds.FAUCET_BLOCKS_BY_PATH.get(tRow.path()).get();
					ResourceLocation tBody = GT6CrucibleDatagen.loc(GT6CrucibleDatagen.bodyTexture(tRow.material().get()));
					simpleBlock(tBlock, GT6CrucibleDatagen.bodyTinted(tRow.material().get())
							? tintedCubeAll(tRow.path(), tBody)
							: models().cubeAll(tRow.path(), tBody));
					itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
				}
			}

			/**
			 * The one-element tinted cube (the GT6OreBlockStates.tintedCubeAll:161-171 idiom,
			 * local copy — FILES_SCOPE keeps the shared providers untouched): every face
			 * tintindex 0 so {@code GT6MoldTintListener} multiplies the material mRGBaSolid
			 * over the grayscale borrow.
			 */
			private ModelFile tintedCubeAll(String aName, ResourceLocation aTexture) {
				BlockModelBuilder tModel = models().getBuilder("block/" + aName)
						.parent(models().getExistingFile(new ResourceLocation("minecraft", "block/block")))
						.texture("all", aTexture)
						.texture("particle", "#all");
				tModel.element()
						.from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
						.allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
						.end();
				return tModel;
			}

			/**
			 * One mold row: the 5x5 bitmap-stamp blockstate over the material smooth body
			 * texture (the mold-geometry geometry × 40-41-mold-assets bodyTexture
			 * stitch — the upstream getTextureSmooth face, the former cobble placeholder is
			 * gone) + the formed item model + the raw clay item as a flat generated sprite
			 * over ITS OWN borrowed upstream icon ({@code item/<path>_raw}, the
			 * gt.multiitem.randomtools 900-929/991 borrows — task 40-41-mold-assets,
			 * GitHub #40; the former shared vanilla clay sprite made all 31 shapes look
			 * identical). The icon files are named after the raw item ids, so the layer0
			 * mapping needs no shape table.
			 */
			private void registerMoldModels(GT6Molds.MoldRow tRow) {
				Block tBlock = GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get();
				simpleBlock(tBlock, moldModel(tRow, tRow.preCarvedShape()));
				itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
				itemModels().withExistingParent(tRow.path() + "_raw", "item/generated")
						.texture("layer0", modLoc("item/" + tRow.path() + "_raw"));
			}

			/**
			 * The 5x5 bitmap stamp model (mold-geometry geometry, issue #41 polarity):
			 * the MOLD is CONCAVE — a chisel strike SETS a bit (MultiTileEntityMold.java
			 * :328-335) and the render gate :537 skips exactly the lit cells, so bit=1 =
			 * carved out. The elements: the 1px full-footprint floor, the four 2px-thick
			 * 4px-tall walls (MOLD_BOUNDS[2..5]), the 12 rim handle boxes (MOLD_BOUNDS
			 * [6..17] — two posts + a spanning cap per side, the upstream wall-top trim),
			 * then one 2.4x3x2.4px element per UNLIT bit standing as the 3px surface — a
			 * lit bit stays a 2px-deep recess over the floor. The block/block parent
			 * carries ONLY the display transforms (the
			 * portal-frame precedent — the standalone element model would strip them from
			 * the BlockItem GUI/hand rendering); the formed item model parents this model,
			 * so the BlockItem inventory face IS the 3D shape for free. The grayscale-borrow
			 * body faces carry tintindex 0 (task debt-material-tint — the upstream
			 * getTextureSmooth mRGBaSolid multiply, :980-987); the vanilla smooth-stone row
			 * stays un-tinted (a finished texture, the recorded declaration shortcut).
			 */
			private ModelFile moldModel(GT6Molds.MoldRow aRow, int aShape) {
				boolean tTint = GT6CrucibleDatagen.bodyTinted(aRow.material().get());
				ResourceLocation tBody = GT6CrucibleDatagen.loc(GT6CrucibleDatagen.bodyTexture(aRow.material().get()));
				BlockModelBuilder tModel = models().getBuilder("block/" + aRow.path())
						.parent(models().getExistingFile(new ResourceLocation("minecraft", "block/block")))
						.texture("particle", tBody)
						.texture("body", tBody);
				// the floor: cullface everywhere but UP — its top sits at y=1, not the
				// boundary, and a cullface there would eat the plate under a solid neighbour
				tModel.element().from(0, 0, 0).to(16, 1, 16)
						.allFaces((aDir, aFace) -> {
							aFace.texture("#body");
							if (tTint) aFace.tintindex(0);
							if (aDir != net.minecraft.core.Direction.UP) aFace.cullface(aDir);
						}).end();
				// the four walls (MOLD_BOUNDS[2..5], 2px thick, y 0..4, the outward face
				// culled): the cavity rim the r7 version lacked
				moldWall(tModel, 14, 0, 16, 16, net.minecraft.core.Direction.EAST, tTint);
				moldWall(tModel, 0, 14, 16, 16, net.minecraft.core.Direction.SOUTH, tTint);
				moldWall(tModel, 0, 0, 2, 16, net.minecraft.core.Direction.WEST, tTint);
				moldWall(tModel, 0, 0, 16, 2, net.minecraft.core.Direction.NORTH, tTint);
				// the rim handles (MOLD_BOUNDS[6..17], MultiTileEntityMold.java:467-478):
				// per side two posts y 4..6 + the spanning cap y 6..7 — the wall-top trim.
				// The texture gate getTexture2 :522-533 renders the caps (passes 8/11/14/17)
				// on all six faces but the posts (6/7/9/10/12/13/15/16) on the four
				// HORIZONTAL ones only — an emitted post up face would coplanar z-fight the
				// cap bottom, a post down face the wall top. The outward boundary face rides
				// the wall cullface; pure decoration, selection/collision stay the :559-560
				// boxes.
				for (int tRim = 0; tRim < RIM_HANDLES.length; tRim++) {
					int[] tBox = RIM_HANDLES[tRim];
					net.minecraft.core.Direction tOutward = RIM_OUTWARD[tRim / 3];
					boolean tCap = tRim % 3 == 2;
					var tElement = tModel.element()
							.from(tBox[0], tBox[1], tBox[2]).to(tBox[3], tBox[4], tBox[5]);
					for (net.minecraft.core.Direction tDir : net.minecraft.core.Direction.values()) {
						if (!tCap && tDir.getAxis() == net.minecraft.core.Direction.Axis.Y) continue;
						var tFace = tElement.face(tDir).texture("#body");
						if (tTint) tFace.tintindex(0);
						if (tDir == tOutward) tFace.cullface(tDir);
						tFace.end();
					}
					tElement.end();
				}
				// the unlit cells stand as the surface; the carved (lit) cells stay open
				for (int i = 0; i < 25; i++) {
					if ((aShape & (1 << i)) == 0) {
						tModel.element()
								.from(GT6Molds.cellLo(i / 5), 0, GT6Molds.cellLo(i % 5))
								.to(GT6Molds.cellHi(i / 5), 3, GT6Molds.cellHi(i % 5))
								.allFaces((aDir, aFace) -> {
									aFace.texture("#body");
									if (tTint) aFace.tintindex(0);
								}).end();
					}
				}
				return tModel;
			}

			/** One mold wall: x aX0..aX1 / z aZ0..aZ1, 4px tall, the outward face culled. */
			private void moldWall(BlockModelBuilder aModel, int aX0, int aZ0, int aX1, int aZ1, net.minecraft.core.Direction aOutward, boolean aTint) {
				aModel.element().from(aX0, 0, aZ0).to(aX1, 4, aZ1)
						.allFaces((aDir, aFace) -> {
							aFace.texture("#body");
							if (aTint) aFace.tintindex(0);
							if (aDir == aOutward) aFace.cullface(aDir);
						}).end();
			}

			/**
			 * The rim handle boxes, MOLD_BOUNDS[6..17] verbatim in px (x0,y0,z0 → x1,y1,z1)
			 * — four sides × (post, post, cap), MultiTileEntityMold.java:467-478.
			 */
			private static final int[][] RIM_HANDLES = {
					{6, 4, 0, 7, 6, 2}, {9, 4, 0, 10, 6, 2}, {6, 6, 0, 10, 7, 2},       // north
					{6, 4, 14, 7, 6, 16}, {9, 4, 14, 10, 6, 16}, {6, 6, 14, 10, 7, 16}, // south
					{0, 4, 6, 2, 6, 7}, {0, 4, 9, 2, 6, 10}, {0, 6, 6, 2, 7, 10},       // west
					{14, 4, 6, 16, 6, 7}, {14, 4, 9, 16, 6, 10}, {14, 6, 6, 16, 7, 10}}; // east

			/** The outward boundary face per {@link #RIM_HANDLES} group. */
			private static final net.minecraft.core.Direction[] RIM_OUTWARD = {
					net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
					net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST};
		}

	// ------------------------------------------------------------------------------------
	// lang (en_us)
	// ------------------------------------------------------------------------------------

	/**
	 * The composed display keys. Chains BELOW {@link GT6CrucibleDatagen.Lang} (which chains
	 * below {@link GT6EnUs}): the tail replays the FULL base set through
	 * {@code super.addTranslations()}. Since task ops-datagen-lang-order this tail is the
	 * SINGLE registered en_us writer (GT6DataGenerators registers it at a fixed position), so
	 * the file stays complete with no second writer to race — the pre-p30 form registered this
	 * AND the two chained providers as separate subscribers and the annotation-scan order
	 * decided which full-file write landed last (the sensors-core merge gate caught the
	 * standalone-provider ancestor of this class wiping the 2701-key table down to 66 keys;
	 * the t4 re-gate lost the 66 ceramic rows the same way).
	 */
	public static final class Lang extends GT6CrucibleDatagen.Lang {

		public Lang(PackOutput aOutput) {
			super(aOutput);
		}

		/** Unique provider name (the GT6EnUs collision; see {@link Provider#getName}). */
		@Override
		public String getName() {
			return "Language Provider: gt6:mold[en_us]";
		}

		@Override
		protected void addTranslations() {
			super.addTranslations(); // base + crucible + stone-mold set — the file must stay complete
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
			// the unlock-advancement write stopped here (2026-10-03 user ruling, remember id1359):
			// JEI/EMI ubiquitous, the vanilla recipe book is dead weight.
			java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
			java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
			build(tFinished -> {
				if (!tSeen.add(tFinished.getId())) throw new IllegalStateException("Duplicate recipe " + tFinished.getId());
				tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tFinished.serializeRecipe(), tRecipePaths.json(tFinished.getId())));
			});
			return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture[0]));
		}
		//?} else {
		/*@Override
		public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
			PackOutput.PathProvider tRecipePaths = mOutput.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.RECIPE);
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

			// the blank raw mold (MultiItemRandomTools.java:127 "C C","CCC","k R" VERBATIM —
			// the third row IS the tool marks: 'k' knife + 'R' rolling pin, re-expanded in
			// issue #45 C1 as the #gt6:tools/knife + #gt6:tools/rolling_pin tag defines
			// (the p24 tool seams take the 1-damage craft toll); 5 clay = the U*5 mass)
			Item tBlankRaw = GT6Molds.rawItemByPath("mold_ceramic");
			rRows.add(shaped(id("mold_ceramic_raw"), ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tBlankRaw)
					.pattern("C C")
					.pattern("CCC")
					.pattern("k R")
					.define('C', Items.CLAY_BALL)
					.define('k', GT6ItemTags.TOOLS_KNIFE)
					.define('R', GT6ItemTags.TOOLS_ROLLING_PIN)
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

			// the ceramic faucet raw (Loader:305 craft, "C C","kCR" — the tool marks
			// re-expanded issue #45 C1, same tag-define face as the blank mold row)
			rRows.add(shaped(id("faucet_ceramic_raw"), ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6Molds.FAUCET_CERAMIC_RAW.get())
					.pattern("C C")
					.pattern("kCR")
					.define('C', Items.CLAY_BALL)
					.define('k', GT6ItemTags.TOOLS_KNIFE)
					.define('R', GT6ItemTags.TOOLS_ROLLING_PIN)
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
