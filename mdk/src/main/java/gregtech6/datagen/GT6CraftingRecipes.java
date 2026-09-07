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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

//? if forge {
import net.minecraftforge.common.Tags;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
*///?}

import gregtech6.registry.GT6SprayCans;
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
 * this recipe closes the p22 "v1 acquisition" note's crafting cut). The food-can
 * second row ({@code "fh"}/{@code "oP"}, :239) stays POOLED — it needs hammer +
 * bendingcylinder_small, neither item exists in this port yet.
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
		for (GrassRecipeRow tRow : grassRecipeBuilders()) {
			tRow.builder().save(aConsumer, tRow.id());
		}
	}
	//?} else {
	/*@Override
	protected void buildRecipes(net.minecraft.data.recipes.RecipeOutput aOutput) {
		sprayCanEmptyBuilder().save(aOutput, SPRAY_CAN_EMPTY_ID);
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
}
