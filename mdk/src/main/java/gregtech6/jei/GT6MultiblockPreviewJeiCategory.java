package gregtech6.jei;

import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;

//? if forge {
import brachy.modularui.integration.jei.recipe.ModularUIJeiCategory;
//?} else {
/*import brachy.modularui.integration.jei.recipe.ModularUIRecipeCategory;
*///?}

/**
 * The multiblock preview category of the JEI leg (task multiblock-preview-infra) — the
 * native twin of {@link gregtech6.emi.GT6MultiblockPreviewEmiCategory}, the GTCEu
 * {@code MultiblockInfoJeiCategory} shape (MultiblockInfoJeiCategory.java:36-59): a
 * ModularUI viewer bridge category whose page is the shared
 * {@link GT6MultiblockPreviewWidget}, one row per {@link GT6MultiblockPreviews} table
 * entry. U on a controller item (the registered catalyst) or on a listed material opens
 * the page.
 *
 * <p>The vendored ModularUI fork ships TWO viewer bridges, one per leg — the forge leg
 * (JEI 15.x) exposes {@code ModularUIJeiCategory} (abstract getMaxWidth/getMaxHeight/
 * setupRecipeIngredients, final getWidth/getHeight) while the neoforge leg (JEI 19.x)
 * exposes {@code ModularUIRecipeCategory} (no abstracts, default getWidth/getHeight over
 * a nullable background) — the extends clause and the size/lookup faces below are this
 * file's one leg fork, everything else is shared. Neither bridge is ever class-loaded
 * offline (compile-only dependency): the guard test pins this file at the bytecode layer.
 */
//? if forge {
public class GT6MultiblockPreviewJeiCategory extends ModularUIJeiCategory<GT6MultiblockPreviewJeiCategory.Wrapper> {
//?} else {
/*public class GT6MultiblockPreviewJeiCategory extends ModularUIRecipeCategory<GT6MultiblockPreviewJeiCategory.Wrapper> {
*///?}

	/** {@code gt6:multiblock_preview} — mirrors the EMI category id one-to-one (the JEMI skip key). */
	public static final RecipeType<Wrapper> RECIPE_TYPE = recipeType();

	/** One preview page: the table row plus its stable recipe id ({@code gt6:multiblock_preview/<name>}). */
	public record Wrapper(GT6MultiblockPreviews.Entry entry, ResourceLocation id) {}

	private final IDrawable mIcon;

	public GT6MultiblockPreviewJeiCategory(IGuiHelper aGuiHelper) {
		super(tWrapper -> new GT6MultiblockPreviewWidget(tWrapper.entry(), GT6MultiblockPreviews.PAGE_WIDTH,
				GT6MultiblockPreviews.PAGE_HEIGHT), tWrapper -> tWrapper.id());
		mIcon = aGuiHelper.createDrawableItemStack(
				new ItemStack(GT6MultiblockPreviews.entries().get(0).item().get()));
	}

	private static RecipeType<Wrapper> recipeType() {
		//? if forge {
		return new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", GT6MultiblockPreviews.UID_PATH), Wrapper.class);
		//?} else {
		/*return new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", GT6MultiblockPreviews.UID_PATH), Wrapper.class);
		 *///?}
	}

	/** One wrapper per table row (the GTCEu registerRecipes loop over the definition registry). */
	public static Wrapper wrapperOf(GT6MultiblockPreviews.Entry aEntry) {
		//? if forge {
		return new Wrapper(aEntry, ResourceLocation.fromNamespaceAndPath("gt6", GT6MultiblockPreviews.UID_PATH + "/" + aEntry.name()));
		//?} else {
		/*return new Wrapper(aEntry, ResourceLocation.fromNamespaceAndPath("gt6",
				GT6MultiblockPreviews.UID_PATH + "/" + aEntry.name()));
		 *///?}
	}

	@Override
	public RecipeType<Wrapper> getRecipeType() {
		return RECIPE_TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(GT6MultiblockPreviews.TITLE_KEY);
	}

	@Override
	public IDrawable getIcon() {
		return mIcon;
	}

	@Override
	public ResourceLocation getRegistryName(Wrapper aRecipe) {
		return aRecipe.id();
	}

	//? if forge {
	/** The bridge's fixed page face — the same bounds the EMI wrapper declares. */
	@Override
	public int getMaxWidth() {
		return GT6MultiblockPreviews.PAGE_WIDTH;
	}

	@Override
	public int getMaxHeight() {
		return GT6MultiblockPreviews.PAGE_HEIGHT;
	}

	/**
	 * The lookup-only layout (the bridge's abstract, consumed when JEI builds its search
	 * index — no display geometry): the controller item as the output arm and the
	 * material counts as inputs, so U on EITHER lands on the page. The in-page slots are
	 * the embedded widget's business (the bridge's setRecipe handles them).
	 */
	@Override
	public void setupRecipeIngredients(IRecipeLayoutBuilder aBuilder, Wrapper aRecipe, IFocusGroup aFocuses) {
		ItemStack tController = new ItemStack(aRecipe.entry().item().get());
		aBuilder.addOutputSlot(0, 0).addItemStack(tController);
		for (Map.Entry<net.minecraft.world.level.block.Block, Integer> tCount : GT6MultiblockPreviews
				.materialCounts(aRecipe.entry().pattern().get(), aRecipe.entry().controllerBlock(),
						GT6MultiblockPreviews.DISPLAY_FACING, aRecipe.entry().controllerCell())
				.entrySet()) {
			aBuilder.addInputSlot(0, 0).addItemStack(new ItemStack(tCount.getKey(), tCount.getValue()));
		}
	}
	//?} else {
	/*// JEI 19.x: the background is null by default, so the page size must be declared
	// directly (the forge leg reaches the same face through getMaxWidth/getMaxHeight —
	// the bridge marks its getWidth/getHeight final there).
	@Override
	public int getWidth() {
		return GT6MultiblockPreviews.PAGE_WIDTH;
	}

	@Override
	public int getHeight() {
		return GT6MultiblockPreviews.PAGE_HEIGHT;
	}
	*///?}
}
