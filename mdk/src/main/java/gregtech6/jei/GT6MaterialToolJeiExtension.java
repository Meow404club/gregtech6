package gregtech6.jei;

import java.util.List;

//? if forge {
import net.minecraft.resources.ResourceLocation;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import gregtech6.items.tools.GT6MaterialToolRecipe;

/**
 * The gt6:material_tool crafting-category extension (task r3-jei-tool-output-tint,
 * GitHub #6 round 3) — the b' ruling face (custom category / subtypes / result
 * components were ruled out, research.issues-r3-tool-jei). JEI renders the vanilla
 * crafting category's output slot from the bare {@code getResultItem} stack
 * (CraftingCategoryExtension.java:26-30 in JEI 15.x / :21-26 in 19.x), so every
 * material row shows the identity-less Steel fallback although {@code assemble}
 * stamps every real craft. This extension swaps the output for the row's stamped
 * representative stack ({@link GT6MaterialToolRecipe#stampedDisplayResult()}) —
 * JEI's own firework extension is the NBT-dependent-output precedent
 * (FireworkStarRecipeCategoryExtension.java:65,199). Same category, same search
 * behaviour, no subtypes.
 *
 * <p>LEG-FORKED FILE, the two JEI generations carry the extension API differently:
 * <ul>
 * <li><b>15.x (1.20.1 forge)</b> — {@code IExtendableRecipeCategory.addCategoryExtension
 *     (Class, Function)} (IVanillaCategoryExtensionRegistration.java:16,30): the
 *     extension WRAPS the recipe instance. It also carries the shaped layout: JEI's
 *     Forge RecipeHelper reads width/height only off an {@code IShapedRecipe}
 *     (RecipeHelper.java:30-43) and this leg's wrapper class deliberately does not
 *     implement it — so {@code getWidth}/{@code getHeight} delegate the pattern dims
 *     here, fixing the shapeless-looking single-row layout.</li>
 * <li><b>19.x (1.21.1 neoforge)</b> — {@code IExtendableCraftingRecipeCategory
 *     .addExtension(Class, singleton)} (IExtendableCraftingRecipeCategory.java:24):
 *     extensions are stateless singletons, the recipe rides in every call
 *     (ICraftingCategoryExtension.java:41, RecipeHolder-wrapped). Width/height need
 *     NO override on this leg: the 19.x default dispatch reads them off
 *     {@code ShapedRecipe} (CraftingCategoryExtension.java:36-45) and this leg's
 *     GT6MaterialToolRecipe extends ShapedRecipe.</li>
 * </ul>
 * Both legs stay strictly on the loader-neutral common API ({@code mezz.jei.api}),
 * the same boundary the GT6JeiPluginTest bytecode guard pins for the plugin class.
 */
//? if forge {
public class GT6MaterialToolJeiExtension implements ICraftingCategoryExtension {

	private final GT6MaterialToolRecipe mRecipe;

	/** The 15.x factory shape: one extension instance per recipe (JEI caches per recipe). */
	public GT6MaterialToolJeiExtension(GT6MaterialToolRecipe aRecipe) {
		mRecipe = aRecipe;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, ICraftingGridHelper aCraftingGridHelper, IFocusGroup aFocuses) {
		// The default CraftingCategoryExtension layout, with the one fix: the output is the
		// row's stamped representative stack, not the bare getResultItem.
		aCraftingGridHelper.createAndSetOutputs(aBuilder, List.of(mRecipe.stampedDisplayResult()));
		aCraftingGridHelper.createAndSetIngredients(aBuilder, mRecipe.getIngredients(), getWidth(), getHeight());
	}

	@Override
	public ResourceLocation getRegistryName() {
		return mRecipe.getId();
	}

	@Override
	public int getWidth() {
		return mRecipe.recipeWidth();
	}

	@Override
	public int getHeight() {
		return mRecipe.recipeHeight();
	}
}
//?} else {
/*public class GT6MaterialToolJeiExtension implements ICraftingCategoryExtension<GT6MaterialToolRecipe> {

	// The 19.x stateless shape: JEI receives this one singleton at registration.
	public static final GT6MaterialToolJeiExtension INSTANCE = new GT6MaterialToolJeiExtension();

	private GT6MaterialToolJeiExtension() { }

	@Override
	public void setRecipe(RecipeHolder<GT6MaterialToolRecipe> aRecipeHolder, IRecipeLayoutBuilder aBuilder, ICraftingGridHelper aCraftingGridHelper, IFocusGroup aFocuses) {
		GT6MaterialToolRecipe tRecipe = aRecipeHolder.value();
		// The default layout, with the one fix: the output is the row's stamped representative
		// stack; the shaped dims come straight off the ShapedRecipe superclass.
		aCraftingGridHelper.createAndSetOutputs(aBuilder, List.of(tRecipe.stampedDisplayResult()));
		aCraftingGridHelper.createAndSetIngredients(aBuilder, tRecipe.getIngredients(), tRecipe.getWidth(), tRecipe.getHeight());
	}
}
*///?}
