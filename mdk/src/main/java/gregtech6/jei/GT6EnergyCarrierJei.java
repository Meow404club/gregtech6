package gregtech6.jei;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;

import gregapi.code.TagData;

/**
 * The energy-carrier pseudo ingredient face of the JEI leg (task viewer-energy-jump-gear):
 * the nine accepted-energy carriers ({@link GT6RecipeMapViewerMeta#pinnedEnergyCarriers},
 * the r6-30 phase-2 design) enter JEI as ONE custom {@link IIngredientType} over the
 * {@link TagData} singleton — the jump target the gear port opens. The registration
 * carries an EMPTY {@code allIngredients} list, so the carriers never join the ingredient
 * list / search index — they exist only as the info pages
 * ({@code GT6JeiPlugin.registerIngredients}) and the focus targets the gear port jumps to.
 * Their on-screen visibility is the r6-30-flagged POC face, field_test's to verify.
 *
 * <p>Both pinned JEI generations (15.62 forge / 19.52 neoforge) carry the identical
 * abstract faces this file implements — {@code IIngredientType.getIngredientClass} +
 * {@code IIngredientHelper} (getIngredientType/getDisplayName/getUniqueId(V, UidContext)/
 * getResourceLocation/copyIngredient/getErrorInfo) + {@code IIngredientRenderer}
 * (render(GuiGraphics, T)/getTooltip(T, TooltipFlag)) — read off the harvested API sources,
 * zero stonecutter fork. The {@code new ResourceLocation} sites keep paren-free arguments
 * so the neoforge-leg ctor swap can digest them (mdk/stonecutter.gradle.kts regex table).
 */
public final class GT6EnergyCarrierJei {

	private GT6EnergyCarrierJei() {}

	/** The singleton type; the uid is pinned literal (JEI serializes ingredient types by it). */
	public static final CarrierType TYPE = new CarrierType();

	/** The type face: {@code TagData} class + the stable uid. */
	public static final class CarrierType implements IIngredientType<TagData> {
		@Override
		public Class<? extends TagData> getIngredientClass() {
			return TagData.class;
		}

		@Override
		public String getUid() {
			return "gt6_energy_carrier";
		}
	}

	/** The carrier's {@code gt6:energy/<code>} id tail, paren-free for the ctor swap. */
	private static String idPath(TagData aCarrier) {
		return "energy/" + GT6RecipeMapViewerMeta.energyTypeShortCode(aCarrier).toLowerCase(Locale.ROOT);
	}

	/** The helper face: identity by the TagData singleton, name = the short code's plain form. */
	static final IIngredientHelper<TagData> HELPER = new IIngredientHelper<>() {
		@Override
		public IIngredientType<TagData> getIngredientType() {
			return TYPE;
		}

		@Override
		public String getDisplayName(TagData aCarrier) {
			return GT6RecipeMapViewerMeta.energyTypeShortCode(aCarrier);
		}

		@Override
		public String getUniqueId(TagData aCarrier, UidContext aContext) {
			return "gt6:" + idPath(aCarrier);
		}

		@Override
		public ResourceLocation getResourceLocation(TagData aCarrier) {
			String tPath = idPath(aCarrier); // paren-free local — the ctor swap's regex forbids parens in the arg
			return ResourceLocation.fromNamespaceAndPath("gt6", tPath);
		}

		@Override
		public TagData copyIngredient(TagData aCarrier) {
			return aCarrier; // immutable singleton — the copy IS the identity
		}

		@Override
		public String getErrorInfo(TagData aCarrier) {
			return "energy carrier " + GT6RecipeMapViewerMeta.energyTypeShortCode(aCarrier);
		}
	};

	/**
	 * The renderer face: the colored short code (the shared {@link GT6RecipeMapViewerMeta#energyUnit}
	 * face — the same component the cost lines draw) centered in the 16x16 ingredient space.
	 */
	static final IIngredientRenderer<TagData> RENDERER = new IIngredientRenderer<>() {
		@Override
		public void render(GuiGraphics aGuiGraphics, TagData aCarrier) {
			aGuiGraphics.drawCenteredString(Minecraft.getInstance().font, GT6RecipeMapViewerMeta.energyUnit(aCarrier), 8, 4, 0xFFFFFFFF);
		}

		@Override
		public List<Component> getTooltip(TagData aCarrier, TooltipFlag aFlag) {
			return List.of(GT6RecipeMapViewerMeta.energyUnit(aCarrier));
		}
	};
}
