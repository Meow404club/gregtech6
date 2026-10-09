package gregtech6.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import gregtech6.worldgen.GT6OreGenInfoLayout;
import gregtech6.worldgen.OreDistributionInfo;

/**
 * The EMI face of the ore-generation distribution page (task debt-ore-gen-display) — the
 * native twin of {@code gregtech6.jei.GT6OreGenInfoJeiCategory} (the JEMI red line: a gt6
 * namespace face exists on both viewers or EMI+JEI users lose it), the one-category
 * singleton form of the GTCEu precedent (GTBedrockFluidEmiCategory.CATEGORY). All geometry
 * and text come from the shared {@link GT6OreGenInfoLayout} seam. The id
 * {@code gt6:ore_gen_info} mirrors the JEI uid one-to-one.
 *
 * <p>The icon is the catalyst stack (the surface-rock's rockGt pebble — {@link
 * GT6OreGenInfoLayout#catalystStack()}), the GTCEu workstation-image form; offline
 * (registries unfired) it degrades to a stone fallback purely so the constructor has a
 * non-empty icon — the fallback can never render in-game where the pebble is registered.
 */
public final class GT6OreGenInfoEmiCategory extends EmiRecipeCategory {

	/** The one category instance (the GTBedrockFluidEmiCategory.CATEGORY form). */
	public static final GT6OreGenInfoEmiCategory CATEGORY = new GT6OreGenInfoEmiCategory();

	private GT6OreGenInfoEmiCategory() {
		super(idOf(), iconOf());
	}

	/** {@code gt6:ore_gen_info} — mirrors the JEI category uid one-to-one (the JEMI skip key). */
	public static ResourceLocation idOf() {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", "ore_gen_info");
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "ore_gen_info");
		 *///?}
	}

	private static EmiStack iconOf() {
		ItemStack tCatalyst = GT6OreGenInfoLayout.catalystStack();
		return tCatalyst.isEmpty() ? EmiStack.of(Items.STONE) : EmiStack.of(tCatalyst);
	}

	/** The category + rows + workstation registration (GTCEu GTBedrockFluidEmiCategory shape). */
	public static void register(EmiRegistry aRegistry) {
		aRegistry.addCategory(CATEGORY);
		int tIndex = 0;
		for (OreDistributionInfo.Entry tEntry : OreDistributionInfo.entries()) {
			aRegistry.addRecipe(new GT6OreGenInfoEmiRecipe(tEntry, tIndex++));
		}
		ItemStack tCatalyst = GT6OreGenInfoLayout.catalystStack();
		if (!tCatalyst.isEmpty()) aRegistry.addWorkstation(CATEGORY, EmiStack.of(tCatalyst));
	}

	@Override
	public Component getName() {
		return GT6OreGenInfoLayout.title();
	}
}
