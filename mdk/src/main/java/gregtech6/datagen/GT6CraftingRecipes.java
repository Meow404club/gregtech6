package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

//? if forge {
import net.minecraftforge.common.Tags;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
*///?}

import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6SprayCans;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTGrassBlocks;

/**
 * The GT6 vanilla-crafting datagen home — task p24-tool-system spec ③, the FIRST
 * RecipeProvider of the port (decisions.p24-tool-system-recipe-provider-first: single
 * file, {@code //?} forks confined to the constructor and the {@code buildRecipes}
 * signature, the builder chain shared; the tags-foundation/tool-family cards take over
 * and append bands). Upstream anchor: the empty spray can is CRAFTED
 * (MultiItemRandomTools.java:240 — pattern {@code "Rf"}/{@code "Cs"} over the oredict
 * keys {@code R=dust} redstone, {@code f=craftingToolFile}, {@code C=plateCurved Sn},
 * {@code s=craftingToolSaw}); the port translates every key onto the self-owned item
 * tags of {@link GT6ItemTags} (the tag-strategy ruling — four keys, zero bare items),
 * result {@code gt6:spray_can_empty} ({@link GT6SprayCans#SPRAY_CAN_EMPTY}, the p22
 * depletion-swap target — the crafted can is what the colour cans deplete INTO, so
 * this recipe closes the p22 "v1 acquisition" note's crafting cut). Task
 * p25-food-can-row0 appends the food-can second row itself ({@code "fh"}/{@code "oP"},
 * :239 — {@link #foodCanEmptyBuilder}) plus the bending-cylinder self-craft
 * ({@code "sfh"}/{@code "III"}, Loader_Tools.java:313 — {@link #bendingCylinderSmallBuilder}),
 * the row that consumes the hammer/file/saw trio through its own tool letters.
 *
 * <p>Leg split (the decision's pinned lines): 1.20.1 —
 * {@code RecipeProvider(PackOutput)} (RecipeProvider.java:81) + abstract
 * {@code buildRecipes(Consumer<FinishedRecipe>)} (:108) + {@code save(Consumer,
 * ResourceLocation)} (ShapedRecipeBuilder.java:102); 21.1 —
 * {@code RecipeProvider(PackOutput, CompletableFuture<HolderLookup.Provider>)}
 * (RecipeProvider.java:70) + overridable {@code buildRecipes(RecipeOutput)} (:127) +
 * {@code save(RecipeOutput, ResourceLocation)} (ShapedRecipeBuilder.java:108). The
 * shared chain calls are name-identical on both legs: {@code shaped(RecipeCategory,
 * ItemLike)} (:45/:47), {@code define(char, TagKey)} (:53/:59), {@code pattern(String)}
 * (:72/:78), {@code unlockedBy(String, has(TagKey))} (:81/:87 — the {@code has(TagKey)}
 * helper returns the leg-native criterion type, :580/:709). The registration site in
 * {@link GT6DataGenerators} stays single-source because BOTH legs construct through the
 * two-argument form (the forge leg ignores the lookup future).
 */
public class GT6CraftingRecipes extends RecipeProvider {

	/** The recipe id — the result path, the vanilla naming convention (gt6:spray_can_empty). */
	public static final ResourceLocation SPRAY_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "spray_can_empty");

	/**
	 * The tool-family recipe ids (task p25-tool-hammer-wrench spec ⑤) — the result-path
	 * vanilla naming convention; the two hammer routes cannot share the result's own id,
	 * so the route names the suffix (the vanilla two-recipe-per-result precedent shape).
	 */
	public static final ResourceLocation HAMMER_STONE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hammer_stone");
	public static final ResourceLocation HAMMER_INGOTS_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "hammer_ingots");
	public static final ResourceLocation WRENCH_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "wrench");
	/** The bending-cylinder self-craft row (task p25-food-can-row0 spec ②) — the result-path convention. */
	public static final ResourceLocation BENDING_CYLINDER_SMALL_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "bending_cylinder_small");
	/** The empty-food-can crafting row (task p25-food-can-row0 spec ③, MultiItemRandomTools.java:239). */
	public static final ResourceLocation FOOD_CAN_EMPTY_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "food_can_empty");
	/** The plate-mold crafting row (task p26-w1-press-extruder-molds, MultiItemTechnological.java:247 stroke). */
	public static final ResourceLocation SHAPE_EXTRUDER_PLATE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_extruder_plate");
	/** The rod-mold crafting row (task p26-w1-press-extruder-molds, MultiItemTechnological.java:221 stroke). */
	public static final ResourceLocation SHAPE_EXTRUDER_ROD_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "shape_extruder_rod");
	/** The LARGE Steel Crucible crafting row (task p26-crucible-multiblock SPEC ⑦). */
	public static final ResourceLocation LARGE_STEEL_CRUCIBLE_ID = new ResourceLocation(GT6DataGenerators.MOD_ID, "large_steel_crucible");

	public GT6CraftingRecipes(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider) {
		//? if forge {
		super(aOutput);
		//?} else {
		/*super(aOutput, aLookupProvider);
		*///?}
	}

	//? if forge {
	@Override
	protected void buildRecipes(Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
		sprayCanEmptyBuilder().save(aConsumer, SPRAY_CAN_EMPTY_ID);
		hammerFromStoneBuilder().save(aConsumer, HAMMER_STONE_ID);
		hammerFromIngotsBuilder().save(aConsumer, HAMMER_INGOTS_ID);
		wrenchBuilder().save(aConsumer, WRENCH_ID);
		bendingCylinderSmallBuilder().save(aConsumer, BENDING_CYLINDER_SMALL_ID);
		foodCanEmptyBuilder().save(aConsumer, FOOD_CAN_EMPTY_ID);
		shapeExtruderPlateBuilder().save(aConsumer, SHAPE_EXTRUDER_PLATE_ID);
		shapeExtruderRodBuilder().save(aConsumer, SHAPE_EXTRUDER_ROD_ID);
		largeSteelCrucibleBuilder().save(aConsumer, LARGE_STEEL_CRUCIBLE_ID);
		for (GrassRecipeRow tRow : grassRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
	}
	//?} else {
	/*@Override
	protected void buildRecipes(net.minecraft.data.recipes.RecipeOutput aOutput) {
		sprayCanEmptyBuilder().save(aOutput, SPRAY_CAN_EMPTY_ID);
		hammerFromStoneBuilder().save(aOutput, HAMMER_STONE_ID);
		hammerFromIngotsBuilder().save(aOutput, HAMMER_INGOTS_ID);
		wrenchBuilder().save(aOutput, WRENCH_ID);
		bendingCylinderSmallBuilder().save(aOutput, BENDING_CYLINDER_SMALL_ID);
		foodCanEmptyBuilder().save(aOutput, FOOD_CAN_EMPTY_ID);
		shapeExtruderPlateBuilder().save(aOutput, SHAPE_EXTRUDER_PLATE_ID);
		shapeExtruderRodBuilder().save(aOutput, SHAPE_EXTRUDER_ROD_ID);
		largeSteelCrucibleBuilder().save(aOutput, LARGE_STEEL_CRUCIBLE_ID);
		for (GrassRecipeRow tRow : grassRecipeBuilders()) {
			tRow.builder().save(aOutput, tRow.id());
		}
	}
	*///?}

	/** One staged recipe: the shared builder + the id its save face writes (the save type is the one leg split). */
	private record GrassRecipeRow(ShapelessRecipeBuilder builder, ResourceLocation id) {}

	/** The forward recipe id of variant i — the result path (the vanilla naming convention). */
	public static ResourceLocation grassRecipeId(int aVariant) {
		String tPath = GTGrassBlocks.PATHS.get(aVariant); // a local so the two-arg RL ctor args stay bare identifiers (the swap-table regex note)
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/** The reverse recipe id of variant i — the variant path + the {@code _reverse} suffix. */
	public static ResourceLocation grassReverseRecipeId(int aVariant) {
		String tPath = GTGrassBlocks.PATHS.get(aVariant) + "_reverse"; // the same bare-identifier discipline
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The grass dye band (task p24-grass-block) — the upstream BlockGrass.java:72-80
	 * registrations as 12 shapeless rows, ONLY the save face forked (the ctor rule):
	 * <ul>
	 * <li><b>forward ×6</b>: 8 vanilla grass BLOCKS (the literal {@code Items.GRASS_BLOCK}
	 * — the 1.20.1 name of what 1.7.10 called {@code Blocks.grass}; no generic grass tag
	 * exists, the census verdict) + 1 dye → 8 of the variant item (BlockGrass.java:75-80,
	 * count 8 on the result). The dye input is the PLATFORM dye tag per variant colour —
	 * {@code Tags.Items.DYES_<COLOR>} (forge Tags.java:226-241 {@code DyeColor.getTag()}
	 * face; the NeoForge constant is the same name over the {@code c:} namespace, the
	 * import fork above carries the whole fork surface). No bare dye item anywhere.</li>
	 * <li><b>reverse ×6</b>: 1 variant item → 1 vanilla grass block (the upstream
	 * {@code RM.generify} + {@code CR.shapeless(ST.make(Blocks.grass, 1, 0), new
	 * Object[] {this})} pair, :72-73 — the generify face has no modern equivalent, the
	 * shapeless row IS the portable half, the shapeless id notes it).</li>
	 * </ul>
	 * Row order = the {@link GTGrassBlocks#VARIANTS} order (forward, reverse alternating
	 * would be fine — each row saves under its own id; the appender order is variant-major
	 * forward-first, deterministic output).
	 *
	 * <p>NOT this card: the Bath dye-fluid rows (Loader_Recipes_Other.java:467-472 — the
	 * Canner/fluid domain) and the Sifting table (Loader_Recipes_Ores.java:225 — pooled,
	 * coarse_dirt conversion + cross-mod bait cut).
	 */
	private List<GrassRecipeRow> grassRecipeBuilders() {
		List<GrassRecipeRow> rRows = new ArrayList<>(GTGrassBlocks.VARIANTS.size() * 2);
		for (int i = 0; i < GTGrassBlocks.VARIANTS.size(); i++) {
			Item tVariantItem = GTGrassBlocks.ITEMS.get(i).get();
			// forward: 8 vanilla grass + 1 dye → 8 variant items (BlockGrass.java:75-80)
			rRows.add(new GrassRecipeRow(ShapelessRecipeBuilder
					.shapeless(RecipeCategory.DECORATIONS, tVariantItem, 8)
					.requires(Items.GRASS_BLOCK, 8)
					.requires(dyeTagOf(GTGrassBlocks.VARIANTS.get(i).dyeIndex()))
					.unlockedBy("has_grass_block", has(Items.GRASS_BLOCK)),
					grassRecipeId(i)));
			// reverse: 1 variant item → 1 vanilla grass block (BlockGrass.java:72-73)
			rRows.add(new GrassRecipeRow(ShapelessRecipeBuilder
					.shapeless(RecipeCategory.DECORATIONS, Items.GRASS_BLOCK)
					.requires(tVariantItem)
					.unlockedBy("has_gt6_grass", has(tVariantItem)),
					grassReverseRecipeId(i)));
		}
		return rRows;
	}

	/** The platform dye tag of a GT6 spray dye index (the six grass-effective colours). */
	private static TagKey<Item> dyeTagOf(byte aDyeIndex) {
		return switch (aDyeIndex) {
			case 2 -> Tags.Items.DYES_GREEN; // variant 0 (Behavior_Spray_Color.java:154)
			case 10 -> Tags.Items.DYES_LIME; // variant 1 (:155)
			case 0 -> Tags.Items.DYES_BLACK; // variant 2 (:156)
			case 7 -> Tags.Items.DYES_LIGHT_GRAY; // variant 3 (:157)
			case 11 -> Tags.Items.DYES_YELLOW; // variant 4 (:158)
			case 3 -> Tags.Items.DYES_BROWN; // variant 5 (:159)
			default -> throw new IllegalArgumentException("not a grass dye: " + aDyeIndex);
		};
	}

	/**
	 * The shared builder chain — the upstream MultiItemRandomTools.java:240 shape with
	 * every key on a {@link GT6ItemTags} tag. NOT the save face: the save call stays in
	 * the {@code buildRecipes} forks because the sink type is the one leg split the
	 * builder chain cannot paper over.
	 */
	private ShapedRecipeBuilder sprayCanEmptyBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6SprayCans.SPRAY_CAN_EMPTY.get())
				.pattern("Rf")
				.pattern("Cs")
				.define('R', GT6ItemTags.REDSTONE_DUSTS)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('C', GT6ItemTags.PLATE_CURVED_TIN)
				.define('s', GT6ItemTags.TOOLS_SAW)
				.unlockedBy("has_redstone_dust", has(GT6ItemTags.REDSTONE_DUSTS));
	}

	/**
	 * The hammer STONE route (task p25-tool-hammer-wrench spec ⑤α) — the upstream
	 * {@code "XX "}/{@code "XXS"}/{@code "XX "} row (Loader_Tools.java:277/:285,
	 * {@code CR.DEF_MIR}) with the input translated: upstream 'X' walks
	 * {@code rockGt.dat(tRock)} over the stone/pebble material universe, the port keys
	 * the WHOLE input face on the vanilla {@code #minecraft:stone_tool_materials} tag
	 * ({@link ItemTags#STONE_TOOL_MATERIALS}) — the cobblestone/blackstone/
	 * cobbled_deepslate trio IS the portable form of the rockGt early-game face.
	 * Double-source probe (the card's不许猜 duty): 1.20.1 decompile ItemTags.java:83;
	 * 1.21.1 client jar {@code data/minecraft/tags/item/stone_tool_materials.json} +
	 * mojmap VanillaRecipeProvider usage — single-source, ZERO leg fork. 'S' stays the
	 * vanilla stick (upstream {@code tHandle[1]}, the handle-family pool cut). Vanilla
	 * shaped recipes auto-match the horizontal mirror, which IS the {@code CR.DEF_MIR}
	 * semantics. Result 1x {@code gt6:hammer}.
	 */
	private ShapedRecipeBuilder hammerFromStoneBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.HAMMER.get())
				.pattern("XX ")
				.pattern("XXS")
				.pattern("XX ")
				.define('X', ItemTags.STONE_TOOL_MATERIALS)
				.define('S', Items.STICK)
				.unlockedBy("has_stone_tool_materials", has(ItemTags.STONE_TOOL_MATERIALS));
	}

	/**
	 * The hammer METAL route (task p25-tool-hammer-wrench spec ⑤β) — the upstream
	 * {@code "II "}/{@code "IIh"}/{@code "II "} row (Loader_Tools.java:327, the
	 * OreProcessing_Tool material loop flattened to ONE tag-keyed row): 'I' = the
	 * ecosystem generic ingots tag ({@code Tags.Items.INGOTS} — forge:ingots on 1.20.1,
	 * c:ingots on 21.1; the per-material loop folds onto the whole tag), 'h' =
	 * {@code #gt6:tools/hard_hammer} (the craftingToolHardHammer snake). The in-grid
	 * hammer pays one durability point per craft and rides along (the container-item
	 * channel, GT6FileItem.craftRemaining via GTHammerItem). Result 1x
	 * {@code gt6:hammer} — zero dead items (the hammer is consumed by its own making
	 * route's h-key just as upstream intended).
	 */
	private ShapedRecipeBuilder hammerFromIngotsBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.HAMMER.get())
				.pattern("II ")
				.pattern("IIh")
				.pattern("II ")
				.define('I', Tags.Items.INGOTS)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_ingot", has(Tags.Items.INGOTS));
	}

	/**
	 * The wrench self-craft row (task p25-tool-hammer-wrench spec ⑤γ) — the upstream
	 * {@code "PhP"}/{@code " P "}/{@code " P "} row (Loader_Tools.java:310): 'P' =
	 * the steel plate platform tag ({@code GT6ItemTags.materialTag(PLATES_FAMILY,
	 * "steel")} — the forge:plates/steel form the plate family band already emits),
	 * 'h' = {@code #gt6:tools/hard_hammer} (the hammer MUST exist first — the
	 * dependency direction the card's merge order pins). The in-grid hammer pays one
	 * point and rides along. Result 1x {@code gt6:wrench} — the wrench's ONLY crafting
	 * row (the upstream consumption face is ≈zero, the research card's ripgrep verdict;
	 * the machine-dismantle interaction pool stays out of this card).
	 */
	private ShapedRecipeBuilder wrenchBuilder() {
		TagKey<Item> tSteelPlates = GT6ItemTags.materialTag(GT6ItemTags.PLATES_FAMILY, "steel");
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.WRENCH.get())
				.pattern("PhP")
				.pattern(" P ")
				.pattern(" P ")
				.define('P', tSteelPlates)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.unlockedBy("has_steel_plate", has(tSteelPlates));
	}

	/**
	 * The bending-cylinder SELF-CRAFT row (task p25-food-can-row0 spec ②) — the upstream
	 * {"sfh", "III"} row (Loader_Tools.java:313, the OreProcessing_Tool material loop
	 * flattened to ONE tag-keyed row, the hammer-ingots-route precedent; the per-material
	 * {@code typemin(2)} gate folds onto the whole INGOTS tag): 's' = #gt6:tools/saw,
	 * 'f' = #gt6:tools/file, 'h' = #gt6:tools/hard_hammer (the CR.java:200/201/211 tool
	 * alphabet — the three p25 tools this row CONSUMES in-grid, the live consumption
	 * chain), 'I' = the ecosystem generic ingots tag ({@code Tags.Items.INGOTS}; upstream
	 * walks {@code ingot.dat(tMat)} per metal — {@code setMaterialAmount(3*U)} = the
	 * 3-ingot row). Each tool pays one durability point per craft and rides along (the
	 * container-item channel via Recipe.getRemainingItems). Result 1x
	 * {@code gt6:bending_cylinder_small}.
	 */
	private ShapedRecipeBuilder bendingCylinderSmallBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GT6Tools.BENDING_CYLINDER_SMALL.get())
				.pattern("sfh")
				.pattern("III")
				.define('s', GT6ItemTags.TOOLS_SAW)
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('I', Tags.Items.INGOTS)
				.unlockedBy("has_ingot", has(Tags.Items.INGOTS));
	}

	/**
	 * The empty-food-can crafting row (task p25-food-can-row0 spec ③) — the upstream
	 * {"fh", "oP"} row VERBATIM (MultiItemRandomTools.java:239, CR.DEF_NCC): 'f' =
	 * {@code #gt6:tools/file} (the CR.java:200 craftingToolFile letter), 'h' =
	 * {@code #gt6:tools/hard_hammer} (:201), 'o' = {@code #gt6:tools/bending_cylinder_small}
	 * (:207 — the OreDictToolNames.bendingcylindersmall letter, the bending cylinder IS a
	 * crafting ingredient here exactly like upstream), 'P' = {@code OP.plateCurved.dat(
	 * MT.TinAlloy)} → the existing {@code #gt6:plate_curved_tin} material tag (the
	 * GTMaterialItems plate_curved_tin item, the spray-can row's 'C' key precedent).
	 * Result 1x {@code gt6:food_can_empty} — the canning machine's consumable input.
	 */
	/**
	 * The LARGE Steel Crucible crafting row (task p26-crucible-multiblock SPEC ⑦) — the
	 * upstream "hMy" row (Loader_MultiTileEntities.java:1270, 'M' = the wall item 18009
	 * 对位) with the declared port deviation that the soldering-tool family is not ported
	 * yet: the row runs "hM" ('h' = the hard-hammer tool tag, the hammerFromIngotsBuilder
	 * key; 'M' = gt6:crucible_steel_wall). The in-grid hammer pays one durability point
	 * and rides along (the container-item channel). Result 1x the controller block item.
	 */
	private ShapedRecipeBuilder largeSteelCrucibleBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,
				gregtech6.registry.GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.get("crucible_steel").get())
				.pattern("hM")
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('M', gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM.get())
				.unlockedBy("has_crucible_wall", has(gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM.get()));
	}

	private ShapedRecipeBuilder foodCanEmptyBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6FoodCans.FOOD_CAN_EMPTY.get())
				.pattern("fh")
				.pattern("oP")
				.define('f', GT6ItemTags.TOOLS_FILE)
				.define('h', GT6ItemTags.TOOLS_HARD_HAMMER)
				.define('o', GT6ItemTags.TOOLS_BENDING_CYLINDER_SMALL)
				.define('P', GT6ItemTags.PLATE_CURVED_TIN)
				.unlockedBy("has_plate_curved_tin", has(GT6ItemTags.PLATE_CURVED_TIN));
	}

	/**
	 * The plate-mold crafting row (task p26-w1-press-extruder-molds) — the upstream
	 * {@code "x  ", " P ", "   "} stroke VERBATIM (MultiItemTechnological.java:247, the
	 * plate-identity file position): 'x' = {@code #gt6:tools/file} (the CR.java:200
	 * craftingToolFile letter), 'P' = {@link GT6ItemTags#EXTRUDER_SHAPE_BASE} (the declared
	 * row0 flattening — upstream chains Plate←Foil+file through the pooled Empty/Foil
	 * intermediates; the port keys both row0 molds on the chain root's tungsten-carbide
	 * plate face, one file stroke per mold identity). Result 1x plate mold — the RM.Extruder
	 * plate row's shaping tool (RM.java:405).
	 */
	private ShapedRecipeBuilder shapeExtruderPlateBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get())
				.pattern("x  ")
				.pattern(" P ")
				.pattern("   ")
				.define('x', GT6ItemTags.TOOLS_FILE)
				.define('P', GT6ItemTags.EXTRUDER_SHAPE_BASE)
				.unlockedBy("has_shape_base", has(GT6ItemTags.EXTRUDER_SHAPE_BASE));
	}

	/**
	 * The rod-mold crafting row (task p26-w1-press-extruder-molds) — the upstream
	 * {@code "   ", " Px", "   "} stroke VERBATIM (MultiItemTechnological.java:221, the
	 * rod-identity file position; the flattened base ingredient per the plate-mold doc).
	 * Result 1x rod mold — the RM.Extruder rod row's shaping tool (RM.java:407).
	 */
	private ShapedRecipeBuilder shapeExtruderRodBuilder() {
		return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get())
				.pattern("   ")
				.pattern(" Px")
				.pattern("   ")
				.define('x', GT6ItemTags.TOOLS_FILE)
				.define('P', GT6ItemTags.EXTRUDER_SHAPE_BASE)
				.unlockedBy("has_shape_base", has(GT6ItemTags.EXTRUDER_SHAPE_BASE));
	}
}
