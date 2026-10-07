package gregtech6.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import gregapi.code.TagData;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The generic RM recipe category of the JEI leg (task debt-jei-emi-batch1) — ONE class
 * serving every visible map, the modern counterpart of the upstream single 718-line
 * NEI_RecipeMap handler that served every mNEIAllowed map (NEI_GT_API_Config.java:62
 * registered one handler per map over this same single class). The layout, the cost text
 * and the tooltips all delegate to the shared {@link GT6RecipeMapViewerMeta} seam — the
 * EMI twin renders the same geometry from the same functions, the card's
 * "布局/EU 文案/tooltip 双 viewer 共享" clause.
 *
 * <p>Consumed faces (JEI 15.x = 1.20.1-forge and 19.x = 1.21.1-neoforge, both read from
 * the harvested API sources, identical on this surface — the jei-tool-output-tint
 * dual-node precedent): {@code IRecipeCategory<T>} (getRecipeType/getTitle/getWidth/
 * getHeight/setRecipe/draw), {@code IRecipeLayoutBuilder.addInputSlot(x,y)/addOutputSlot
 * (x,y)}, {@code IRecipeSlotBuilder.addItemStack/addFluidStack(Fluid,long)/
 * addRichTooltipCallback} — the loader-neutral common API only, pinned by the
 * GT6JeiPluginTest bytecode guard's jurisdiction.
 *
 * <p>Slot semantics over the port's {@link Recipe} row (the NEI display fields):
 * item inputs up to the map's mInputItemsCount (NEI probed getRepresentativeInput per
 * slot — the port's mInputs array IS the row's slot list), item outputs with the chance
 * tooltip, fluid inputs/outputs as native fluids (the bucket containerization is the
 * dropped column, GT6RecipeMapViewerMeta class doc), and the never-consumed tooltip
 * riding {@link Recipe#sNotConsumable} (the size-0 marker port).
 */
public class GT6RecipeMapJeiCategory implements IRecipeCategory<Recipe> {

	/** {@code gt6:recipe_map/<internal>} — the JEI-side uid mirrors the EMI category id one-to-one. */
	public final RecipeType<Recipe> mRecipeType;
	public final RecipeMap mMap;
	/** The per-map machine icon (task issues #29/#34a) — built by the plugin from {@link GT6RecipeMapIcons}. */
	private final IDrawable mIcon;

	public GT6RecipeMapJeiCategory(RecipeMap aMap, IDrawable aIcon) {
		mMap = aMap;
		mRecipeType = recipeTypeOf(aMap);
		mIcon = aIcon;
	}

	/**
	 * The per-map {@link RecipeType} formula — ONE construction shared by the registration
	 * (the ctor above) and the runtime jump face ({@link GT6JeiPlugin#openRecipeMapPage},
	 * task debt-jei-emi-batch4). {@code RecipeType.equals} compares uid + recipe class
	 * (RecipeType.java 15.x, identical in 19.x), so the type rebuilt at click time resolves
	 * to the registered category without stashing instances.
	 */
	public static RecipeType<Recipe> recipeTypeOf(RecipeMap aMap) {
		//? if forge {
		return new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal), Recipe.class);
		//?} else {
		/*return new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal), Recipe.class);
		 *///?}
	}

	@Override
	public RecipeType<Recipe> getRecipeType() {
		return mRecipeType;
	}

	/**
	 * The per-map category title (task issues #29/#34a, GitHub #29b): the shared
	 * {@link GT6RecipeMapViewerMeta#titleKey} formula — translatable, so every locale
	 * resolves its own face (the ctor's English {@code mNameLocal} stays the en_us value,
	 * produced by the GT6EnUs datagen walk).
	 */
	@Override
	public Component getTitle() {
		return Component.translatable(GT6RecipeMapViewerMeta.titleKey(mMap));
	}

	@Override
	public int getWidth() {
		return GT6RecipeMapViewerMeta.CATEGORY_WIDTH;
	}

	@Override
	public int getHeight() {
		return GT6RecipeMapViewerMeta.CATEGORY_HEIGHT;
	}

	/**
	 * Per-map machine icon (task issues #29/#34a, GitHub #29a): the representative machine
	 * BlockItem from the shared {@link GT6RecipeMapIcons} table — the plugin builds the
	 * drawable via {@code IGuiHelper.createDrawableItemStack} and hands it to the ctor.
	 * The batch-2 DEFER adjudication is superseded by that table.
	 */
	@Override
	public IDrawable getIcon() {
		return mIcon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, Recipe aRecipe, IFocusGroup aFocuses) {
		// all four loops consume the meta's VIEWER exits — the re-anchored -(5,7) fold to
		// panel coordinates happened once inside GT6RecipeMapViewerMeta, never here.
		int tInputs = Math.min(aRecipe.mInputs.length, mMap.mInputItemsCount);
		for (int i = 0; i < tInputs; i++) {
			ItemStack tStack = aRecipe.mInputs[i];
			if (tStack == null || tStack.isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerInputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			var tSlot = aBuilder.addInputSlot(tPos[0], tPos[1]).addItemStack(tStack.copy());
			if (GT6RecipeMapViewerMeta.notConsumable(tStack)) tSlot.addRichTooltipCallback(notConsumedTooltip());
		}
		int tOutputs = Math.min(aRecipe.mOutputs.length, mMap.mOutputItemsCount);
		for (int i = 0; i < tOutputs; i++) {
			ItemStack tStack = aRecipe.mOutputs[i];
			if (tStack == null || tStack.isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerOutputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			net.minecraft.network.chat.Component tChance = GT6RecipeMapViewerMeta.chanceLine(GT6RecipeMapViewerMeta.outputChance(aRecipe, i), tStack.getCount());
			var tSlot = aBuilder.addOutputSlot(tPos[0], tPos[1]).addItemStack(tStack.copy());
			if (tChance != null) tSlot.addRichTooltipCallback(staticTooltip(tChance));
		}
		int tFluids = Math.min(aRecipe.mFluidInputs.length, mMap.mInputFluidCount);
		for (int i = 0; i < tFluids; i++) {
			if (aRecipe.mFluidInputs[i] == null || aRecipe.mFluidInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidInputPos(i);
			aBuilder.addInputSlot(tPos[0], tPos[1])
					.addFluidStack(aRecipe.mFluidInputs[i].getFluid(), aRecipe.mFluidInputs[i].getAmount());
		}
		int tFluidOuts = Math.min(aRecipe.mFluidOutputs.length, mMap.mOutputFluidCount);
		for (int i = 0; i < tFluidOuts; i++) {
			if (aRecipe.mFluidOutputs[i] == null || aRecipe.mFluidOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidOutputPos(i);
			aBuilder.addOutputSlot(tPos[0], tPos[1])
					.addFluidStack(aRecipe.mFluidOutputs[i].getFluid(), aRecipe.mFluidOutputs[i].getAmount());
		}
	}

	/**
	 * The gear-spot jump port (task viewer-energy-jump-gear, the user ruling): carrier maps
	 * get ONE {@link GearJumpFace} registered as BOTH an {@code IRecipeWidget} (the hover
	 * tooltip affordance over the baked gear art) and an {@code IJeiInputHandler} (the
	 * click contract — mouse-down simulates, mouse-up executes, IJeiInputHandler.java:36-49).
	 * GU maps register nothing: the gear stays decoration (无载体图不画). Both pins are
	 * @since 15.9.0 on the forge leg / 19.6.0 on the neo leg — under both pinned stacks.
	 */
	@Override
	public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder aBuilder, Recipe aRecipe, IFocusGroup aFocuses) {
		TagData tCarrier = GT6RecipeMapViewerMeta.energyOf(mMap);
		if (tCarrier == null) return;
		GearJumpFace tGear = new GearJumpFace(tCarrier);
		aBuilder.addWidget(tGear);
		aBuilder.addInputHandler(tGear);
	}

	/**
	 * The port itself: the folded gear rect, the hint tooltip and the jump through
	 * {@link GT6JeiPlugin#openEnergyCarrierInfo} (guarded — offline/pre-init no-op). The
	 * mouse-coordinate params are ignored: JEI only routes inputs whose position is inside
	 * {@link #getArea} (the handler contract). Package-private: the offline pin drives the
	 * click contract directly (the GT6JeiPluginTest posture).
	 */
	static final class GearJumpFace implements mezz.jei.api.gui.widgets.IRecipeWidget, mezz.jei.api.gui.inputs.IJeiInputHandler {

		private final TagData mCarrier;

		GearJumpFace(TagData aCarrier) {
			mCarrier = aCarrier;
		}

		@Override
		public net.minecraft.client.gui.navigation.ScreenRectangle getArea() {
			int[] tPos = GT6RecipeMapViewerMeta.viewerGearPos();
			return new net.minecraft.client.gui.navigation.ScreenRectangle(tPos[0], tPos[1],
					GT6RecipeMapViewerMeta.GEAR_SIZE, GT6RecipeMapViewerMeta.GEAR_SIZE);
		}

		@Override
		public net.minecraft.client.gui.navigation.ScreenPosition getPosition() {
			int[] tPos = GT6RecipeMapViewerMeta.viewerGearPos();
			return new net.minecraft.client.gui.navigation.ScreenPosition(tPos[0], tPos[1]);
		}

		@Override
		public net.minecraft.client.gui.navigation.ScreenRectangle getScreenRectangle() {
			return getArea();
		}

		@Override
		public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder aTooltip, double aMouseX, double aMouseY) {
			aTooltip.add(Component.translatable(GT6RecipeMapViewerMeta.ENERGY_JUMP_HINT_KEY));
		}

		@Override
		public boolean handleInput(double aMouseX, double aMouseY, mezz.jei.api.gui.inputs.IJeiUserInput aInput) {
			if (aInput.getKey().getType() != com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) return false;
			if (aInput.isSimulate()) return true; // mouse-down: this click can be handled (no action)
			GT6JeiPlugin.openEnergyCarrierInfo(mCarrier); // mouse-up: execute
			return true;
		}
	}

	/**
	 * The composed page (task composed-ui-energy-slot-and-parts, the user ruling 配方页
	 * 渲染切拼接 UI+删除独立机器 GUI 贴图): the grey {@code machines/NEI.png} plate, then
	 * the two machine-skin furniture cells as part crops — the progress-arrow cell and
	 * the special-slot gear cell (the energy jump's art, drawn whether or not THIS map
	 * carries a port: the gear slot is the skins' universal decor). The per-map machine
	 * GUI band is RETIRED here — its baked slot frames doubled the code slots'
	 * (贴图槽+代码槽叠加), so the frames now come only from JEI's own RecipeSlots. The
	 * part blits use the full {@code blit(location, x, y, w, h, u, v, uW, vH, tW, tH)}
	 * form — the parts are standalone small PNGs, not 256x256 canvases. The plate is
	 * drawn at (0,0) via the 6-int form: it IS a 256x256 canvas.
	 */
	@Override
	public void draw(Recipe aRecipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView aRecipeSlotsView,
			net.minecraft.client.gui.GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		int[] tPlate = GT6RecipeMapViewerMeta.PLATE_CROP;
		aGuiGraphics.blit(GT6RecipeMapViewerMeta.PLATE_TEXTURE, 0, 0, tPlate[0], tPlate[1], tPlate[2], tPlate[3]);
		blitPart(aGuiGraphics, gregtech6.gui.machines.GT6GuiParts.ARROW_OUTLINE, GT6RecipeMapViewerMeta.viewerArrowPos());
		blitPart(aGuiGraphics, gregtech6.gui.machines.GT6GuiParts.SLOT_SPECIAL, GT6RecipeMapViewerMeta.viewerGearPos());
		// NO hand-drawn machine item on the plate's gear spot (task viewer-icon-retire-gu-pin,
		// the user ruling): EMI renders the workstation list itself (RecipeScreen.java:203-217)
		// and JEI renders the catalyst column itself (RecipesGui.java:635-636 → RecipeCatalysts,
		// left side, hover/click) — both fed by GT6JeiPlugin:164-167/GT6EmiPlugin:157. Upstream
		// NEI_RecipeMap.java:278 only drew for the rare non-empty mRecipeMachineList, so the
		// per-map draw was an over-generalization; the retired exits lived on the meta.
		// drawExtras (NEI_RecipeMap.drawExtras :680-717 verbatim arithmetic): the
		// Costs/Usage/Tier/Power/Time/Special lines at the shared panel-system text band —
		// NEI's fixed 0xFF000000 ink and x10 kept.
		int tY = GT6RecipeMapViewerMeta.TEXT_BASE_Y;
		for (net.minecraft.network.chat.Component tLine : GT6RecipeMapViewerMeta.costLines(mMap, aRecipe)) {
			aGuiGraphics.drawString(Minecraft.getInstance().font, tLine, GT6RecipeMapViewerMeta.TEXT_X, tY, 0xFF000000);
			tY += GT6RecipeMapViewerMeta.TEXT_LINE_HEIGHT;
		}
	}

	/** One furniture part at its folded viewer position — the standalone-PNG blit (own texture dims declared). */
	private static void blitPart(net.minecraft.client.gui.GuiGraphics aGuiGraphics,
			gregtech6.gui.machines.GT6GuiParts.GuiPart aPart, int[] aPos) {
		aGuiGraphics.blit(aPart.texture(), aPos[0], aPos[1], aPart.width(), aPart.height(),
				0.0F, 0.0F, aPart.width(), aPart.height(), aPart.width(), aPart.height());
	}

	private static IRecipeSlotRichTooltipCallback staticTooltip(net.minecraft.network.chat.Component aLine) {
		return (aView, aTooltip) -> aTooltip.add(aLine);
	}

	private static IRecipeSlotRichTooltipCallback notConsumedTooltip() {
		return staticTooltip(net.minecraft.network.chat.Component.translatable(GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY));
	}
}
