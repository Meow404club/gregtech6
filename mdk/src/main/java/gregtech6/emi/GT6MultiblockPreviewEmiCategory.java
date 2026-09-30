package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import brachy.modularui.integration.emi.recipe.ModularUIEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import gregtech6.jei.GT6MultiblockPreviewWidget;
import gregtech6.jei.GT6MultiblockPreviews;

/**
 * The multiblock preview category of the EMI leg (task multiblock-preview-infra) — the
 * native twin of {@link gregtech6.jei.GT6MultiblockPreviewJeiCategory}, the GTCEu
 * {@code MultiblockInfoEmiCategory} shape (EmiRecipeCategory + a
 * {@link ModularUIEmiRecipe} wrapper whose page is the shared
 * {@link GT6MultiblockPreviewWidget}; MultiblockInfoEmiCategory.java:40-129). One
 * category, one wrapper per {@link GT6MultiblockPreviews} table row.
 *
 * <p>The U-navigation faces (the GTCEu data bridge): {@code getOutputs()} = the controller
 * item (U on a coke oven lands here), {@code getInputs()} = the material counts (U on a
 * Coke Oven Brick reaches the page too); the workstation arm registers the controller
 * item (the JEMI red-line twin of the JEI catalysts). THE JEMI RED LINE (GT6EmiPlugin
 * class doc): every gt6 JEI face ships with its native EMI twin in the same card — the
 * text EmiInfoRecipe this category replaces was tier-b face 1; the preview category is
 * its modern replacement, landing on both legs together.
 *
 * <p>Not loaded outside a live EMI session: the class links the vendored ModularUI and
 * EMI APIs, both compile-only — the offline guard test reads its BYTECODE (annotation/
 * bridge/uid strings), never the class.
 */
public final class GT6MultiblockPreviewEmiCategory extends EmiRecipeCategory {

	/** The singleton category (the GTCEu CATEGORY shape) — icon = the first table row's controller item. */
	public static final GT6MultiblockPreviewEmiCategory CATEGORY = new GT6MultiblockPreviewEmiCategory();

	private GT6MultiblockPreviewEmiCategory() {
		super(id(), EmiStack.of(new ItemStack(GT6MultiblockPreviews.entries().get(0).item().get())));
	}

	/** {@code gt6:multiblock_preview} — mirrors the JEI category uid one-to-one (the JEMI skip key). */
	public static ResourceLocation id() {
		//? if forge {
		return new ResourceLocation("gt6", GT6MultiblockPreviews.UID_PATH);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", GT6MultiblockPreviews.UID_PATH);
		 *///?}
	}

	/** The wrapper recipe id: {@code gt6:multiblock_preview/<registry name>} — one per table row. */
	public static ResourceLocation recipeId(GT6MultiblockPreviews.Entry aEntry) {
		//? if forge {
		return new ResourceLocation("gt6", GT6MultiblockPreviews.UID_PATH + "/" + aEntry.name());
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", GT6MultiblockPreviews.UID_PATH + "/" + aEntry.name());
		 *///?}
	}

	@Override
	public Component getName() {
		return Component.translatable(GT6MultiblockPreviews.TITLE_KEY);
	}

	/**
	 * One preview page (the GTCEu MultiblockInfoEmiWrapper): the shared widget supplier,
	 * explicit page bounds (skips the lazy size calculation entirely — the page is
	 * fixed-size by construction), material counts as inputs, the controller item as the
	 * sole output. {@code supportsRecipeTree} stays false: material counts are a
	 * shopping list, not craftable tree nodes.
	 */
	public static final class PreviewEmiRecipe extends ModularUIEmiRecipe {

		private final GT6MultiblockPreviews.Entry mEntry;
		private final List<EmiIngredient> mInputs = new ArrayList<>();

		public PreviewEmiRecipe(GT6MultiblockPreviews.Entry aEntry) {
			super(recipeId(aEntry), GT6MultiblockPreviews.PAGE_WIDTH, GT6MultiblockPreviews.PAGE_HEIGHT,
					() -> new GT6MultiblockPreviewWidget(aEntry, GT6MultiblockPreviews.PAGE_WIDTH,
							GT6MultiblockPreviews.PAGE_HEIGHT));
			mEntry = aEntry;
			Map<Block, Integer> tCounts = GT6MultiblockPreviews.materialCounts(
					aEntry.pattern().get(), aEntry.controllerBlock(), GT6MultiblockPreviews.DISPLAY_FACING);
			tCounts.forEach((tBlock, tCount) -> mInputs.add(EmiStack.of(new ItemStack(tBlock, tCount))));
		}

		@Override
		public EmiRecipeCategory getCategory() {
			return CATEGORY;
		}

		@Override
		public List<EmiIngredient> getInputs() {
			return mInputs;
		}

		@Override
		public List<EmiStack> getOutputs() {
			return List.of(EmiStack.of(new ItemStack(mEntry.item().get())));
		}

		@Override
		public boolean supportsRecipeTree() {
			return false;
		}
	}
}
