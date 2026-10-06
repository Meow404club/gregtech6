package gregtech6.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;

import gregtech6.worldgen.GT6OreGenInfoLayout;
import gregtech6.worldgen.OreDistributionInfo;

/**
 * The JEI face of the ore-generation distribution page (task debt-ore-gen-display) — ONE
 * category serving every {@link OreDistributionInfo.Entry}, the thin wrapper of the B-lite
 * ruling: all geometry, text and stack resolution live on the shared
 * {@link GT6OreGenInfoLayout} seam, the same functions the EMI twin
 * (gregtech6.emi.GT6OreGenInfoEmiRecipe) renders from. Structure precedent: the batch1
 * generic RM category (GT6RecipeMapJeiCategory) and the GTCEu ore-vein category
 * (GTOreVeinInfoCategory — one RecipeType, one row per definition).
 *
 * <p>The U-reachability arm is the GTCEu addInvisibleIngredients trick
 * (GTOreVeinInfoCategory.java:51-53): every ore block item the material owns rides as an
 * INVISIBLE output, so U on any of them opens this page while the drawn face stays the
 * single representative slot. The catalyst face (the surface-rock's rockGt pebble) is
 * registered by the plugin ({@link GT6JeiPlugin#registerRecipeCatalysts}); with no drawn
 * icon ({@link #getIcon}, the batch1 null form) JEI falls back to that catalyst.
 *
 * <p>Loader-neutral common API only — {@code addInvisibleIngredients} and
 * {@code addRecipeCatalysts} are identical on the JEI 15.x (1.20.1) and 19.x (1.21.1)
 * generations (IRecipeLayoutBuilder.java:117 and IRecipeCatalystRegistration.java:41/43 in
 * both harvested API sources), so this wrapper compiles fork-free on both legs.
 */
public class GT6OreGenInfoJeiCategory implements IRecipeCategory<OreDistributionInfo.Entry> {

	/** {@code gt6:ore_gen_info} — the JEI uid mirrors the EMI category id one-to-one. */
	public final RecipeType<OreDistributionInfo.Entry> mRecipeType;

	public GT6OreGenInfoJeiCategory() {
		//? if forge {
		mRecipeType = new RecipeType<>(new ResourceLocation("gt6", "ore_gen_info"), OreDistributionInfo.Entry.class);
		//?} else {
		/*mRecipeType = new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", "ore_gen_info"), OreDistributionInfo.Entry.class);
		 *///?}
	}

	@Override
	public RecipeType<OreDistributionInfo.Entry> getRecipeType() {
		return mRecipeType;
	}

	@Override
	public Component getTitle() {
		return GT6OreGenInfoLayout.title();
	}

	@Override
	public int getWidth() {
		return GT6OreGenInfoLayout.WIDTH;
	}

	@Override
	public int getHeight() {
		return GT6OreGenInfoLayout.categoryHeight();
	}

	/** No icon: JEI falls back to the category's catalyst (the rockGt pebble) — the batch1 null form. */
	@Override
	public IDrawable getIcon() {
		return null;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, OreDistributionInfo.Entry aEntry, IFocusGroup aFocuses) {
		ItemStack tRep = GT6OreGenInfoLayout.representative(aEntry);
		if (!tRep.isEmpty()) aBuilder.addOutputSlot(GT6OreGenInfoLayout.SLOT_X, GT6OreGenInfoLayout.SLOT_Y).addItemStack(tRep);
		// the invisible arm: every variant U-searches to this page (the GTCEu form, role OUTPUT)
		aBuilder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
				.addItemStacks(GT6OreGenInfoLayout.variantStacks(aEntry));
	}

	/** The draw leg: the dims row beside the slot, then the sectioned rows — all from the shared seam, zero local layout. */
	@Override
	public void draw(OreDistributionInfo.Entry aEntry, IRecipeSlotsView aRecipeSlotsView,
			GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		var tFont = Minecraft.getInstance().font;
		aGuiGraphics.drawString(tFont, GT6OreGenInfoLayout.name(aEntry), GT6OreGenInfoLayout.TEXT_X,
				GT6OreGenInfoLayout.NAME_Y, 0xFF000000);
		aGuiGraphics.drawString(tFont, GT6OreGenInfoLayout.dimsRow(aEntry), GT6OreGenInfoLayout.DIMS_X,
				GT6OreGenInfoLayout.DIMS_Y, 0xFF000000);
		for (GT6OreGenInfoLayout.Row tRow : GT6OreGenInfoLayout.rows(aEntry)) {
			aGuiGraphics.drawString(tFont, tRow.component(), tRow.x(), tRow.y(), 0xFF000000);
		}
	}
}
