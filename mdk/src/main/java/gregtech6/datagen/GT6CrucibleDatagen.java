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
 *     height formula (:603, the top-face-only gate :616, the molten art).</li>
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
	 * The texture-set folders whose {@code molten} grayscale the port borrowed verbatim
	 * (assets/gt6/textures/block/materialicons/&lt;set&gt;/molten.png + the shared
	 * .png.mcmeta — task r11b-crucible-molten-art). Upstream ships a molten icon for EVERY
	 * texture set ({@code TextureSet.addToAll(MD.GT.mID, F, "molten")}, GT_API.java:157)
	 * and the byte census of the upstream tree says the art comes in THREE distinct files
	 * plus ONE shared animation mcmeta (the sha256 ledger in assets/README.md): the
	 * standard 16x320 animated strip (27 sets), the soft 16x512 family (10 sets) and the
	 * RAD singleton. The upstream GAS/PLASMA sets carry a fourth static art the port has
	 * no folder for, and FLUID rides the soft-family bytes — those set names fall through
	 * to the byte-different rough/ art (the declared deviation). Total like
	 * {@link #bodyTexture}: the setless default IS a set (OreDictMaterial.java:252).
	 */
	private static final java.util.Set<String> BORROWED_MOLTEN = java.util.Set.of(
			"brick", "copper", "cube", "cube_shiny", "diamond", "dull", "emerald", "fiery", "fine", "flint",
			"food", "gem_horizontal", "gem_vertical", "glass", "hex", "lapis", "leaf", "lignite", "magnetic",
			"metallic", "netherstar", "none", "opal", "paper", "powder", "prismarine", "quartz", "rad",
			"redstone", "rough", "rubber", "ruby", "sand", "shards", "shiny", "space", "stone", "wood");

	/**
	 * The molten content art — the upstream {@code getTextureMolten} texture source
	 * (OreDictMaterial.java:990-999, {@code IconsGT.INDEX_BLOCK_MOLTEN}): the material's
	 * first block texture-set folder's {@code molten} grayscale, byte-identical to what
	 * upstream registers for that set ({@link #BORROWED_MOLTEN}); the gas/plasma/fluid
	 * sets and anything unknown ride the shared rough/ borrow. Fully-qualified like
	 * {@link #bodyTexture}. Public so the tests pin the dispatch the three content
	 * consumers (bowl tint / large-crucible BER / Jade bar) all draw through.
	 */
	public static String moltenTexture(gregapi.oredict.OreDictMaterial aMaterial) {
		java.util.List<String> tSets = aMaterial.mTextureSetsBlock;
		String tSet = (tSets.isEmpty() ? "none" : tSets.get(0)).toLowerCase(java.util.Locale.ROOT);
		return "gt6:block/materialicons/" + (BORROWED_MOLTEN.contains(tSet) ? tSet : "rough") + "/molten";
	}

	/**
	 * The content render face — the seam one level up (task crucible-bowl-model, the
	 * 2026-09-30 colour ruling): the sprite + the opaque ARGB tint a crucible content face
	 * renders with. The SOLID arm is the upstream crucible solid face verbatim (the
	 * {@link #bodyTexture} blockSolid icon + the {@link #bodyTinted} mRGBaSolid tint — the
	 * exact faces the bowl shell renders); the MOLTEN arm is the molten face (the
	 * {@link #moltenTexture} per-set grayscale + the material mRGBaLiquid — the upstream
	 * {@code getTextureMolten} shape verbatim, OreDictMaterial.java:990-999; task
	 * r11b-crucible-molten-art retired the flat smeltery_content placeholder). The
	 * jade-tankbar and the large-crucible BER cards consume THIS dispatch instead of
	 * re-deriving sprite/colour math; {@code -1} tint = the sprite renders as-is.
	 */
	public record ContentFace(String texture, int tintARGB) {}

	/** The dispatch itself — loud on an unmapped material ({@link #bodyTexture} rule). */
	public static ContentFace contentFace(gregapi.oredict.OreDictMaterial aMaterial, boolean aMolten) {
		if (aMolten) return new ContentFace(moltenTexture(aMaterial), argb(aMaterial.mRGBaLiquid));
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
			// task material-mc-c-crucible-rows — the two new single-shell families:
			for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.BASIN_ROWS) {
				addBasin(tRow, GT6Crucibles.BASIN_BLOCKS_BY_PATH.get(tRow.path()).get());
			}
			for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.CROSSING_ROWS) {
				addCrossing(tRow, GT6Crucibles.CROSSING_BLOCKS_BY_PATH.get(tRow.path()).get());
			}
		}

		/**
		 * One basin: the static open VAT shell (the MultiTileEntityBasin.java:100-104 render
		 * passes 0-4 verbatim — four 1px full-height walls + the 1px floor, px units) + the
		 * BlockItem parent. tintindex 0 on every face of the grayscale-borrow rows (the
		 * GT6MoldTintListener seat). The molten content face (upstream pass 5) is the
		 * declared mold-family render cut — the port molds render no charge either.
		 */
		private void addBasin(GT6Crucibles.SmelteryRow aRow, Block aBlock) {
			gregapi.oredict.OreDictMaterial tMaterial = aRow.material().get();
			boolean tTinted = bodyTinted(tMaterial);
			BlockModelBuilder tModel = models().getBuilder("block/" + aRow.path())
					.parent(models().getExistingFile(mcLoc("block/block")))
					.texture("all", loc(bodyTexture(tMaterial)))
					.texture("particle", "#all");
			// passes 0-3 — the 1px full-height walls
			float[][] tWalls = {{0, 0, 0, 1, 16, 16}, {15, 0, 0, 16, 16, 16}, {0, 0, 0, 16, 16, 1}, {0, 0, 15, 16, 16, 16}};
			for (float[] tBox : tWalls) addBox(tModel, tBox, tTinted);
			// pass 4 — the 1px floor
			addBox(tModel, new float[] {0, 0, 0, 16, 1, 16}, tTinted);
			getVariantBuilder(aBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
			itemModels().withExistingParent(aRow.path(), modLoc("block/" + aRow.path()));
		}

		/**
		 * One crossing: the 11-box bridge (the MultiTileEntityCrossing.java setBlockBounds2
		 * passes 0-10 verbatim — the two 1px crossing strips + the eight rim posts) + the
		 * BlockItem parent. Faces carry no cullface: the non-full cross renders every quad
		 * (66 per block — the noOcclusion posture makes them all visible anyway).
		 */
		private void addCrossing(GT6Crucibles.SmelteryRow aRow, Block aBlock) {
			gregapi.oredict.OreDictMaterial tMaterial = aRow.material().get();
			boolean tTinted = bodyTinted(tMaterial);
			BlockModelBuilder tModel = models().getBuilder("block/" + aRow.path())
					.parent(models().getExistingFile(mcLoc("block/block")))
					.texture("all", loc(bodyTexture(tMaterial)))
					.texture("particle", "#all");
			for (float[] tBox : CROSSING_MODEL_BOXES) addBox(tModel, tBox, tTinted);
			getVariantBuilder(aBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
			itemModels().withExistingParent(aRow.path(), modLoc("block/" + aRow.path()));
		}

		/** The crossing element boxes, px units (passes 0-2 the strips, 3-10 the rim ring). */
		private static final float[][] CROSSING_MODEL_BOXES = {
				{6, 1, 0, 10, 2, 16}, {0, 1, 6, 6, 2, 10}, {10, 1, 6, 16, 2, 10},
				{5, 2, 0, 6, 6, 5}, {5, 2, 11, 6, 6, 16}, {0, 2, 5, 6, 6, 6}, {10, 2, 5, 16, 6, 6},
				{10, 2, 0, 11, 6, 5}, {10, 2, 11, 11, 6, 16}, {0, 2, 10, 6, 6, 11}, {10, 2, 10, 11, 6, 11}};

		/** One box element: all six faces #all, tintindex 0 on the tinted rows (the basin/crossing shell face). */
		private static void addBox(BlockModelBuilder aModel, float[] aBox, boolean aTinted) {
			var tElement = aModel.element()
					.from(aBox[0], aBox[1], aBox[2]).to(aBox[3], aBox[4], aBox[5]);
			for (net.minecraft.core.Direction tDir : net.minecraft.core.Direction.values()) {
				var tFace = tElement.face(tDir).texture("#all");
				if (aTinted) tFace.tintindex(0);
				tFace.end();
			}
			tElement.end();
		}

		/**
		 * One crucible: the upstream open-top BOWL (MultiTileEntitySmeltery.java:596-619) over
		 * the 9 LIQUID_LEVEL × 2 MOLTEN variants + the BlockItem parent. Level 0 = the bowl
		 * shell (the {@link #bodyTexture} faces, tintindex 0 on the grayscale-borrow rows —
		 * task debt-material-tint); levels 1..8 = the shell + the content box, its TOP face
		 * the phase art at the upstream height (:603 {@code 0.125F + h/292.571428F} block
		 * units, the bucket L carrying the census floor {@code h = L*255/8}; the :616
		 * top-face-only gate): molten=true the molten art, molten=false the body art (task
		 * crucible-render-followup — the cooled-charge face, the upstream art choice by the
		 * mDisplayedFluid validity :587-591, the gray-NULL placeholder retired per the user
		 * ruling "solid = the material's own colour").
		 */
		private void addCrucible(GT6Crucibles.SmelteryRow aRow, Block aBlock) {
			String tEmpty = "block/" + aRow.path() + "_empty";
			gregapi.oredict.OreDictMaterial tMaterial = aRow.material().get();
			ModelFile tEmptyModel = bowlModel(tEmpty, bodyTexture(tMaterial), bodyTinted(tMaterial));
			ModelFile[] tFilledModels = new ModelFile[9];
			ModelFile[] tSolidModels = new ModelFile[9];
			boolean tTinted = bodyTinted(tMaterial);
			for (int tLevel = 1; tLevel <= 8; tLevel++) {
				// the r11a-crucible-filled-shell fix: vanilla BlockModel.getElements (1.20.1
				// :104-105) NEVER merges the parent chain once the child carries its own
				// elements — the former "candle idiom" parent+one-element form made the game
				// drop the bowl shell (floating content panel). The child re-writes the FULL
				// set (the shell verbatim + the content box); the parent stays for the
				// texture map only. The content texture is the ROW material's molten art
				// (task r11b-crucible-molten-art — the tintindex-1 seat still recolours live
				// by the displayed material; the borrowed art is shared by 31/41 upstream
				// sets, so the static bake stays upstream-faithful)
				float tTop = 2.0F + (tLevel * 255.0F / 8.0F) / 292.571428F * 16.0F;
				BlockModelBuilder tBuilder = models().getBuilder("block/" + aRow.path() + "_filled_" + tLevel)
						.parent(tEmptyModel)
						.texture("content", loc(moltenTexture(tMaterial)));
				addShellElements(tBuilder, tTinted);
				// the :616 gate — top face only; tintindex 1 = the content seat (task
				// crucible-large-ber): GT6MoldTintListener answers the BE's synced displayed
				// material through the ContentFace dispatch (the same colour the large-crucible
				// BER renders); tintindex 0 stays the shell's material body seat
				var tElement = tBuilder.element()
						.from(0.0F, 2.0F, 0.0F).to(16.0F, tTop, 16.0F);
				tElement.face(Direction.UP).texture("#content").tintindex(1).end();
				tFilledModels[tLevel] = tElement.end();
				// the cooled twin (task crucible-render-followup): the same geometry, the
				// content seat on the BODY art — recoloured live by the synced lightest
				// content's mRGBaSolid (the tint listener's solid arm)
				BlockModelBuilder tSolidBuilder = models().getBuilder("block/" + aRow.path() + "_filled_" + tLevel + "_solid")
						.parent(tEmptyModel);
				addShellElements(tSolidBuilder, tTinted);
				var tSolidElement = tSolidBuilder.element()
						.from(0.0F, 2.0F, 0.0F).to(16.0F, tTop, 16.0F);
				tSolidElement.face(Direction.UP).texture("#all").tintindex(1).end();
				tSolidModels[tLevel] = tSolidElement.end();
			}
			getVariantBuilder(aBlock).forAllStates(aState -> {
				int tLevel = aState.getValue(GT6Crucibles.CrucibleBlock.LIQUID_LEVEL);
				ModelFile tModel = tLevel == 0 ? tEmptyModel
						: aState.getValue(GT6Crucibles.CrucibleBlock.MOLTEN) ? tFilledModels[tLevel] : tSolidModels[tLevel];
				return ConfiguredModel.builder().modelFile(tModel).build();
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
			// the 39 smeltery rungs (task material-mc-c-crucible-rows): the display column IS
			// the en value (the four former literals were its first four entries verbatim)
			for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
				add("gt6.row.crucible.display." + tRow.path(), tRow.display());
			}
			// the basin/crossing composed templates + the shared material words (the ACT
			// :1797 live-mNameLocal form — addRowMatUnit dedups, the first writer wins, so
			// the hopper/wall words already emitted by the base set stay untouched)
			add(GT6Crucibles.BASIN_DISPLAY_KEY, "Basin (%s)");
			add(GT6Crucibles.CROSSING_DISPLAY_KEY, "Crucible Crossing (%s)");
			for (GT6Crucibles.CrucibleMaterial tMat : GT6Crucibles.MATERIALS) {
				addRowMatUnit("gt6.row.mat." + tMat.slug(), tMat.mt().mNameLocal);
			}
			// the raw clay items (the port Clay→Ceramic rename family, the clay_crucible_raw form)
			add("item.gt6.clay_crucible_raw", "Ceramic Crucible (Raw)");
			add("item.gt6.basin_ceramic_raw", "Ceramic Basin (Raw)");
			add("item.gt6.crossing_ceramic_raw", "Ceramic Crossing (Raw)");
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
				// the unlock-advancement write stopped here (2026-10-03 user ruling, remember id1359):
				// JEI/EMI ubiquitous, the vanilla recipe book is dead weight.
			java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
			java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
			build(tFinished -> {
				if (!tSeen.add(tFinished.getId())) throw new IllegalStateException("Duplicate recipe " + tFinished.getId());
				tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, GT6ForeignRowConvergence.converged(tFinished).serializeRecipe(), tRecipePaths.json(tFinished.getId())));
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
								java.util.Optional.of(GT6ForeignRowConvergence.conditioned(aRecipe, tRegistries, aConditions)),
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

			// task material-mc-c-crucible-rows — the :251-292/:425-466/:471-513 craft walks.
			// The ingredient resolves through GTMaterialItems.get (the faucet-card resolvable
			// gate: a pair without a port item path emits NO recipe — the OP.stone/ANY-gem
			// rows mostly CUT, the mod-stones forever). The tool marks drop to empty (the
			// mold-card form), so the smeltery rings keep 7, the basins 5, the crossings 5
			// items — the upstream pattern shapes with h/y/w cells blanked. smeltery_stone
			// keeps its recorded 8-cobblestone deviation row above; the ceramic rows ride
			// the raw-clay chains below.
			for (GT6Crucibles.CrucibleMaterial tMat : GT6Crucibles.MATERIALS) {
				Item tIngredientItem = craftIngredientItem(tMat);
				if (tIngredientItem == null) continue;
				char tKey = tMat.craft() == GT6Crucibles.CrucibleMaterial.CraftKind.PLATE_SELF
						|| tMat.craft() == GT6Crucibles.CrucibleMaterial.CraftKind.PLATE_GRAPHENE ? 'P' : 'B';
				Item tSmeltery = GT6Crucibles.ITEMS_BY_PATH.get(tMat.pathOf("smeltery")).get();
				if (!"stone".equals(tMat.slug())) { // the opening row's cobblestone deviation stands
					ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tSmeltery)
							.pattern(tKey + " " + tKey).pattern(tKey + " " + tKey)
							.pattern("" + tKey + tKey + tKey)
							.define(tKey, tIngredientItem)
							.unlockedBy("has_ingredient", has(tIngredientItem))
							.save(aOutput, id(tMat.pathOf("smeltery")));
				}
				Item tBasin = GT6Crucibles.BASIN_ITEMS_BY_PATH.get(tMat.pathOf("basin")).get();
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tBasin)
						.pattern(tKey + " " + tKey).pattern(tKey + " " + tKey).pattern(" " + tKey + " ")
						.define(tKey, tIngredientItem)
						.unlockedBy("has_ingredient", has(tIngredientItem))
						.save(aOutput, id(tMat.pathOf("basin")));
				Item tCrossing = GT6Crucibles.CROSSING_ITEMS_BY_PATH.get(tMat.pathOf("crossing")).get();
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tCrossing)
						.pattern(" " + tKey + " ").pattern("" + tKey + tKey + tKey).pattern(" " + tKey + " ")
						.define(tKey, tIngredientItem)
						.unlockedBy("has_ingredient", has(tIngredientItem))
						.save(aOutput, id(tMat.pathOf("crossing")));
			}

			// the ceramic basin/crossing raw chains (:430/:477 — the U*5 pair, the
			// clay_crucible_raw chain form: shaped raw, shapeless reclaim, furnace hardening)
			for (String tFamily : new String[] {"basin", "crossing"}) {
				Item tRaw = "basin".equals(tFamily) ? GT6Crucibles.BASIN_CERAMIC_RAW.get() : GT6Crucibles.CROSSING_CERAMIC_RAW.get();
				ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tRaw)
						.pattern("C C").pattern("C C").pattern(" C ")
						.define('C', Items.CLAY_BALL)
						.unlockedBy("has_clay", has(Items.CLAY_BALL))
						.save(aOutput, id(tFamily + "_ceramic_raw"));
				ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 5)
						.requires(tRaw)
						.unlockedBy("has_raw", has(tRaw))
						.save(aOutput, id(tFamily + "_ceramic_raw_reclaim"));
				Item tFormed = "basin".equals(tFamily)
						? GT6Crucibles.BASIN_ITEMS_BY_PATH.get("basin_ceramic").get()
						: GT6Crucibles.CROSSING_ITEMS_BY_PATH.get("crossing_ceramic").get();
				SimpleCookingRecipeBuilder.smelting(net.minecraft.world.item.crafting.Ingredient.of(tRaw), RecipeCategory.MISC, tFormed, 0.0F, 200)
						.unlockedBy("has_raw", has(tRaw))
						.save(aOutput, id("smelt_" + tFamily + "_ceramic"));
			}
		}

		/**
		 * The resolvable gate (the faucet-card form): the row's craft ingredient, or null =
		 * the pair has no port item path and emits NO recipe JSON.
		 */
		private static @javax.annotation.Nullable Item craftIngredientItem(GT6Crucibles.CrucibleMaterial aMat) {
			var tItem = switch (aMat.craft()) {
				case STONE -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.stone, aMat.mt());
				case GEM -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.gem, aMat.mt());
				case PLATE_SELF -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, aMat.mt());
				case PLATE_GRAPHENE -> gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.plate, gregapi.data.MT.Graphene);
				case NONE -> null;
			};
			return tItem == null ? null : tItem.get();
		}

		private static ResourceLocation id(String aPath) {
			return new ResourceLocation("gt6", aPath);
		}
	}

	//?}

}
