package gregtech6.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;

import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.concurrent.CompletableFuture;

import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Molds;

/**
 * The crucible-chain datagen home (task crucible-physics-smeltery spec ⑦). Since task
 * ops-datagen-lang-order the provider band folds into {@link GT6DataGenerators#onGatherData}
 * — this class is NO LONGER a {@code @Mod.EventBusSubscriber}: three self-contained
 * GatherDataEvent subscribers ordered themselves by the annotation-scan lottery and the
 * per-family full-file lang writers made the last one win (the p30 card's live repro: an
 * incremental recompile flipped the scan order and a cache-cold runData landed en_us.json
 * 66 keys short). All four faces are datagen-native; ZERO hand-written JSON:
 * <ul>
 * <li><b>blockstates</b>: per crucible block NINE variants over the
 *     {@link GT6Crucibles.CrucibleBlock#LIQUID_LEVEL} int property (0..8, the
 *     mDisplayedHeight :298 census bucketed) — level 0 the open-top BOWL shell (the
 *     upstream 6-pass setBlockBounds2 render verbatim, MultiTileEntitySmeltery.java:596-606:
 *     four 2px walls full height + the 2px floor, the {@link #bodyTexture} faces), levels
 *     1..8 the bowl shell + the content box whose top rides the upstream h/292.571428
 *     height formula (:603, the top-face-only gate :616, the molten-indicator texture).</li>
 * <li><b>item models</b>: the BlockItems parent the bowl-shell block model.</li>
 * <li><b>lang</b>: the four composed display keys (the GT6Crucibles/GT6Molds getName
 *     carriers).</li>
 * <li><b>crafting</b>: the Stone Smeltery (8 cobblestone — the upstream id-1000 opening
 *     row) and the Stone Mold (7 cobblestone — the card face), the tier-a crafting JSON
 *     universe (KJS: the naturally moddable face).</li>
 * </ul>
 */
public final class GT6CrucibleDatagen {

	private GT6CrucibleDatagen() {}

	/**
	 * The provider band {@link GT6DataGenerators} appends to ITS listener — package-visible,
	 * deliberately NOT a {@code @SubscribeEvent}. The Lang face is NOT registered here: the
	 * chain GT6EnUs ← this Lang ← GT6MoldDatagen.Lang replays the full base table through the
	 * super calls, so the ONE registered tail ({@link GT6MoldDatagen.Lang}) writes the
	 * complete en_us.json; any extra registered writer would re-open the last-writer lottery.
	 */
	static void appendProviders(GatherDataEvent aEvent) {
		aEvent.getGenerator().addProvider(true,
				new Provider(aEvent.getGenerator().getPackOutput(), aEvent.getExistingFileHelper()));
		//? if forge {
		// the crafting face is the 1.20.1-forge runData surface this card drives; the 21.1
		// datagen flow (RecipeOutput) has no FinishedRecipe and stays the card-B future
		aEvent.getGenerator().addProvider(true,
				new Recipes(aEvent.getGenerator().getPackOutput(), aEvent.getLookupProvider()));
		//?}
	}

	// ------------------------------------------------------------------------------------
	// blockstates + models
	// ------------------------------------------------------------------------------------

	/**
	 * The texture-set folders whose {@code block_solid} grayscale the port borrowed verbatim
	 * (assets/gt6/textures/block/materialicons/&lt;set&gt;/block_solid.png — the prefixblock-render
	 * borrow). Upstream ships a blockSolid.png for EVERY texture set, and the byte census of the
	 * upstream tree says the art is shared: 39 of the 41 sets carry the SAME file (md5 75286903,
	 * the {@code copy_into_*.bat} spreads), only STONE and BRICK differ (they share a second
	 * file, md5 43496774, which the port borrowed under brick/). So a set with its own borrow
	 * references that folder and every remaining set — including the ones the port never
	 * borrowed (GLASS, GEM_*, NETHERSTAR, the gas/fluid/plasma faces, SET_NONE) — references the
	 * byte-identical rough/ art. Task crucible-solid-face-matrix: the crucible holds ANY
	 * material, so the mapping is TOTAL — the former four-family loud ISE retires because
	 * upstream itself never throws either: the setless default IS a set
	 * (OreDictMaterial.java:252 {@code mTextureSetsBlock = TextureSet.SET_NONE[0].mList}).
	 */
	private static final java.util.Set<String> BORROWED_BLOCK_SOLID = java.util.Set.of(
			"brick", "copper", "cube", "diamond", "dull", "fiery", "fine", "food", "lapis",
			"leaf", "lignite", "magnetic", "metallic", "quartz", "rad", "redstone", "rough",
			"rubber", "ruby", "shiny", "space", "wood");

	/**
	 * The material smooth body texture (task 40-41-mold-assets, GitHub #41 — the former
	 * flat andesite/cobble placeholder for every row). The upstream body face is the
	 * material's {@code getTextureSmooth()} — the texture set's blockSolid icon
	 * (OreDictMaterial.java:983-990 → :974-976 → BlockTextureDefault.java:143-150
	 * {@code mTextureSetsBlock.get(OP.blockSolid.mIconIndexBlock)}; the icon itself is
	 * {@code materialicons/<SET>/<file>}, TextureSet.java:63/:78). Dispatch by the
	 * material's first block texture-set name (MT.setTextures seeds it from the SET_*
	 * constants, MT.java:225-230):
	 * <ul>
	 * <li>the Stone ROW rides vanilla smooth stone — the recorded #40-41 declaration
	 *     deviation, kept (the four-family regression pins);</li>
	 * <li>the STONE SET (Lava/Obsidian/Bedrock/the stone() family, MT.java:2052/:2395/:2396)
	 *     rides the brick borrow — upstream STONE/blockSolid is byte-identical to
	 *     BRICK/blockSolid (md5 43496774), the port borrowed that art once;</li>
	 * <li>the 21 further borrowed sets reference their own folder;</li>
	 * <li>every other set name (and the setless default) references the shared
	 *     grayscale via rough/ — byte-identical to what upstream registers for them.</li>
	 * </ul>
	 * Upstream multiplies the grayscale art with the material colour at runtime (the
	 * {@code mRGBaSolid} pass of {@code getTextureSmooth(mRGBaSolid, F)} :980-987) — task
	 * debt-material-tint closed the former un-tinted deviation: the tinted rows carry
	 * tintindex 0 on the body faces and {@code GT6MoldTintListener} answers the
	 * {@link #bodyTinted} material's mRGBaSolid. Returns the FULLY-QUALIFIED
	 * {@code ns:path} (the vanilla row carries its explicit {@code minecraft:} — the
	 * callers parse it, they must not {@code modLoc} it again).
	 */
	static String bodyTexture(gregapi.oredict.OreDictMaterial aMaterial) {
		if (aMaterial == gregapi.data.MT.Stone) return "minecraft:block/smooth_stone";
		java.util.List<String> tSets = aMaterial.mTextureSetsBlock;
		String tSet = (tSets.isEmpty() ? "NONE" : tSets.get(0)).toLowerCase(java.util.Locale.ROOT);
		if (tSet.equals("stone")) tSet = "brick"; // the STONE set ships the BRICK art byte-for-byte
		if (BORROWED_BLOCK_SOLID.contains(tSet)) return "gt6:block/materialicons/" + tSet + "/block_solid";
		return "gt6:block/materialicons/rough/block_solid";
	}

	/**
	 * Whether the body face needs the material tint (task debt-material-tint): the
	 * borrowed grayscale materialicons art is multiplied with the mRGBaSolid colour, the
	 * vanilla smooth-stone row is a FINISHED texture — a second multiply would dirty it
	 * (the recorded declaration deviation, kept). Derived from {@link #bodyTexture}'s own
	 * namespace so the tint rule can never drift from the texture mapping.
	 */
	public static boolean bodyTinted(gregapi.oredict.OreDictMaterial aMaterial) {
		return !bodyTexture(aMaterial).startsWith("minecraft:");
	}

	/**
	 * A fully-qualified {@code ns:path} string into the ResourceLocation the model builder
	 * wants — {@code fromNamespaceAndPath} is the leg-neutral form (the GTOreBakedModel
	 * spriteOf precedent); the 1.20.1 single-arg ctor does not exist on the 21.1 leg.
	 * Public since task crucible-large-ber: the BER sprite lookup shares it (the
	 * ContentFace texture strings are its product).
	 */
	public static ResourceLocation loc(String aQualified) {
		int tColon = aQualified.indexOf(':');
		return ResourceLocation.fromNamespaceAndPath(aQualified.substring(0, tColon), aQualified.substring(tColon + 1));
	}

	/**
	 * The molten-content face (the script-generated placeholder PNG, the p2 pipeline — the
	 * one flat-orange sprite; the per-material liquid colour rides {@link #contentFace}'s
	 * molten arm for the consumers that can tint). Fully-qualified like {@link #bodyTexture}.
	 */
	private static final String CONTENT_TEXTURE = "gt6:block/smeltery_content";

	/**
	 * The content render face — the seam one level up (task crucible-bowl-model, the
	 * 2026-09-30 colour ruling): the sprite + the opaque ARGB tint a crucible content face
	 * renders with. The SOLID arm is the upstream crucible solid face verbatim (the
	 * {@link #bodyTexture} blockSolid icon + the {@link #bodyTinted} mRGBaSolid tint — the
	 * exact faces the bowl shell renders); the MOLTEN arm is the molten face (the
	 * smeltery_content sprite + the material mRGBaLiquid — the upstream
	 * {@code getTextureMolten} liquid colour, OreDictMaterial.java:997-998). The
	 * jade-tankbar and the large-crucible BER cards consume THIS dispatch instead of
	 * re-deriving sprite/colour math; {@code -1} tint = the sprite renders as-is.
	 */
	public record ContentFace(String texture, int tintARGB) {}

	/** The dispatch itself — loud on an unmapped material ({@link #bodyTexture} rule). */
	public static ContentFace contentFace(gregapi.oredict.OreDictMaterial aMaterial, boolean aMolten) {
		if (aMolten) return new ContentFace(CONTENT_TEXTURE, argb(aMaterial.mRGBaLiquid));
		return new ContentFace(bodyTexture(aMaterial), bodyTinted(aMaterial) ? argb(aMaterial.mRGBaSolid) : -1);
	}

	/** The 0xFFRRGGBB pack (the {@code GT6MoldTintListener.materialTintARGB} form). */
	private static int argb(short[] aRGBa) {
		return 0xFF000000 | (aRGBa[0] << 16) | (aRGBa[1] << 8) | aRGBa[2];
	}

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
				addCrucible(tRow, tBlock);
			}
			// the mold_stone row moved to GT6MoldDatagen (issue #41: stone and ceramic molds
			// share the one concave MTE design 1072 — the flat-cube placeholder retired)
		}

		/**
		 * One crucible: the upstream open-top BOWL (MultiTileEntitySmeltery.java:596-619) over
		 * the 9 LIQUID_LEVEL variants + the BlockItem parent. Level 0 = the bowl shell (the
		 * {@link #bodyTexture} faces, tintindex 0 on the grayscale-borrow rows — task
		 * debt-material-tint); levels 1..8 = the shell + the content box, its TOP face the
		 * molten-indicator sprite at the upstream height (:603
		 * {@code 0.125F + h/292.571428F} block units, the bucket L carrying the census floor
		 * {@code h = L*255/8}; the :616 top-face-only gate).
		 */
		private void addCrucible(GT6Crucibles.SmelteryRow aRow, Block aBlock) {
			String tEmpty = "block/" + aRow.path() + "_empty";
			gregapi.oredict.OreDictMaterial tMaterial = aRow.material().get();
			ModelFile tEmptyModel = bowlModel(tEmpty, bodyTexture(tMaterial), bodyTinted(tMaterial));
			ModelFile[] tFilledModels = new ModelFile[9];
			boolean tTinted = bodyTinted(tMaterial);
			for (int tLevel = 1; tLevel <= 8; tLevel++) {
				// the r11a-crucible-filled-shell fix: vanilla BlockModel.getElements (1.20.1
				// :104-105) NEVER merges the parent chain once the child carries its own
				// elements — the former "candle idiom" parent+one-element form made the game
				// drop the bowl shell (floating content panel). The child re-writes the FULL
				// set (the shell verbatim + the content box); the parent stays for the
				// texture map only.
				float tTop = 2.0F + (tLevel * 255.0F / 8.0F) / 292.571428F * 16.0F;
				BlockModelBuilder tBuilder = models().getBuilder("block/" + aRow.path() + "_filled_" + tLevel)
						.parent(tEmptyModel)
						.texture("content", loc(CONTENT_TEXTURE));
				addShellElements(tBuilder, tTinted);
				// the :616 gate — top face only; tintindex 1 = the content seat (task
				// crucible-large-ber): GT6MoldTintListener answers the BE's synced displayed
				// material through the ContentFace dispatch (the same colour the large-crucible
				// BER renders); tintindex 0 stays the shell's material body seat
				var tElement = tBuilder.element()
						.from(0.0F, 2.0F, 0.0F).to(16.0F, tTop, 16.0F);
				tElement.face(Direction.UP).texture("#content").tintindex(1).end();
				tFilledModels[tLevel] = tElement.end();
			}
			getVariantBuilder(aBlock).forAllStates(aState -> {
				int tLevel = aState.getValue(GT6Crucibles.CrucibleBlock.LIQUID_LEVEL);
				return ConfiguredModel.builder().modelFile(tLevel == 0 ? tEmptyModel : tFilledModels[tLevel]).build();
			});
			itemModels().withExistingParent(aRow.path(), modLoc(tEmpty));
		}

		/**
		 * The open-top bowl shell (the upstream setBlockBounds2 passes 0-4 verbatim,
		 * MultiTileEntitySmeltery.java:596-606 — four 2px walls full height + the 2px floor,
		 * px units). Faces follow the getTexture2 null-gate (:610-619) so no two faces of the
		 * shell are coplanar: the X-walls (passes 0/2) render west/east/up, the Z-walls
		 * (passes 1/3) north/south/up, the floor (pass 4) up+down — the corners ride the
		 * full-length wall spans, exactly upstream. The outer faces carry cullface (the
		 * cubeAll convention), the interior/rim faces none.
		 */
		private ModelFile bowlModel(String aName, String aBodyTexture, boolean aTinted) {
			BlockModelBuilder tModel = models().getBuilder(aName)
					.parent(models().getExistingFile(mcLoc("block/block")))
					.texture("all", loc(aBodyTexture))
					.texture("particle", "#all");
			addShellElements(tModel, aTinted);
			return tModel;
		}

		/**
		 * The shell elements — the upstream setBlockBounds2 passes 0-4 verbatim
		 * (MultiTileEntitySmeltery.java:596-606, four 2px walls full height + the 2px floor,
		 * px units) — written onto ANY builder: the {@code _empty} model and every
		 * {@code _filled_N} child (the r11a fix — the child must carry the full element set
		 * itself, vanilla getElements never merges the parent). Faces follow the getTexture2
		 * null-gate (:610-619) so no two faces of the shell are coplanar: the X-walls
		 * (passes 0/2) render west/east/up, the Z-walls (passes 1/3) north/south/up, the
		 * floor (pass 4) up+down — the corners ride the full-length wall spans, exactly
		 * upstream. The outer faces carry cullface (the cubeAll convention), the
		 * interior/rim faces none.
		 */
		private static void addShellElements(BlockModelBuilder tModel, boolean aTinted) {
			// pass 0 — the west wall (x 0..2); pass 2 — the east wall (x 14..16)
			for (float tX : new float[] {0.0F, 14.0F}) {
				var tElement = tModel.element().from(tX, 0.0F, 0.0F).to(tX + 2.0F, 16.0F, 16.0F);
				tElement.face(tX == 0.0F ? Direction.WEST : Direction.EAST)
						.texture("#all").cullface(tX == 0.0F ? Direction.WEST : Direction.EAST).end();
				tElement.face(tX == 0.0F ? Direction.EAST : Direction.WEST).texture("#all").end();
				tElement.face(Direction.UP).texture("#all").end();
				if (aTinted) tElement.faces((aDir, aFace) -> aFace.tintindex(0));
				tElement.end();
			}
			// pass 1 — the north wall (z 0..2); pass 3 — the south wall (z 14..16)
			for (float tZ : new float[] {0.0F, 14.0F}) {
				var tElement = tModel.element().from(0.0F, 0.0F, tZ).to(16.0F, 16.0F, tZ + 2.0F);
				tElement.face(tZ == 0.0F ? Direction.NORTH : Direction.SOUTH)
						.texture("#all").cullface(tZ == 0.0F ? Direction.NORTH : Direction.SOUTH).end();
				tElement.face(tZ == 0.0F ? Direction.SOUTH : Direction.NORTH).texture("#all").end();
				tElement.face(Direction.UP).texture("#all").end();
				if (aTinted) tElement.faces((aDir, aFace) -> aFace.tintindex(0));
				tElement.end();
			}
			// pass 4 — the 2px floor (y 0..2): up + down, the :615 SIDES_VERTICAL gate
			var tFloor = tModel.element().from(0.0F, 0.0F, 0.0F).to(16.0F, 2.0F, 16.0F);
			tFloor.face(Direction.UP).texture("#all").end();
			tFloor.face(Direction.DOWN).texture("#all").cullface(Direction.DOWN).end();
			if (aTinted) tFloor.faces((aDir, aFace) -> aFace.tintindex(0));
			tFloor.end();
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
	 * translation set plus the four crucible keys. Since task ops-datagen-lang-order this
	 * class is a CHAIN LINK only — never registered itself; {@link GT6MoldDatagen.Lang}, the
	 * chain tail, is the single registered en_us writer.
	 */
	public static class Lang extends GT6EnUs {

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
			add("gt6.row.crucible.display.smeltery_ceramic", "Ceramic Smeltery");
			add("gt6.row.crucible.display.smeltery_bronze", "Bronze Smeltery");
			add("gt6.row.crucible.display.smeltery_steel", "Steel Smeltery");
			// the raw clay crucible item (issue #45 C2 — the upstream "Clay Crucible" raw,
			// MultiItemRandomTools.java:113; the Clay→Ceramic rename rides the port family
			// convention, the faucet raw precedent)
			add("item.gt6.clay_crucible_raw", "Ceramic Crucible (Raw)");
			// the stone rung this card registered (the loop over GT6Molds.ROWS degenerated when
			// the mold card grew the 30 ceramic rows — their display keys belong to
			// GT6MoldDatagen.Lang, which chains BELOW this provider and relabels them properly;
			// labelling every ceramic row "Stone Mold" here would win whenever this listener
			// registers last — the sensors-core merge review)
			add("gt6.row.mold.display.mold_stone", "Stone Mold");
		}
	}

	// ------------------------------------------------------------------------------------
	// crafting (the tier-a JSON face)
	// ------------------------------------------------------------------------------------

	//? if forge {

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

			// the clay crucible chain (issue #45 C2):
			// - the shaped raw (MultiItemRandomTools.java:125 "CkC"/"CRC"/"CCC" — the
			//   k=knife/R=rollingpin tool marks are the port cut, the GT6MoldDatagen family
			//   form: the tool slots drop to empty, 7 clay = the U*7 amount, :113)
			// - the reverse shapeless (:113 — the raw un-molds back to 7 clay balls)
			// - the furnace hardening tail (Loader_MultiTileEntities.java:256
			//   RM.add_smelting — raw → the smeltery_ceramic item, the row's only acquisition)
			Item tClayCrucibleRaw = GT6Crucibles.CLAY_CRUCIBLE_RAW.get();
			ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tClayCrucibleRaw)
					.pattern("C C")
					.pattern("C C")
					.pattern("CCC")
					.define('C', Items.CLAY_BALL)
					.unlockedBy("has_clay", has(Items.CLAY_BALL))
					.save(aOutput, id("clay_crucible_raw"));
			ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 7)
					.requires(tClayCrucibleRaw)
					.unlockedBy("has_raw", has(tClayCrucibleRaw))
					.save(aOutput, id("clay_crucible_raw_reclaim"));
			Item tCeramicSmeltery = GT6Crucibles.ITEMS_BY_PATH.get("smeltery_ceramic").get();
			SimpleCookingRecipeBuilder.smelting(net.minecraft.world.item.crafting.Ingredient.of(tClayCrucibleRaw), RecipeCategory.MISC, tCeramicSmeltery, 0.0F, 200)
					.unlockedBy("has_raw", has(tClayCrucibleRaw))
					.save(aOutput, id("smelt_smeltery_ceramic"));
		}

		private static ResourceLocation id(String aPath) {
			return new ResourceLocation("gt6", aPath);
		}
	}

	//?}

}
