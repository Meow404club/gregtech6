package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextureWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregtech6.jei.GT6RecipeMapViewerMeta;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * One RM row's EMI face (task debt-jei-emi-batch1) — the hand-flattened inputs/outputs
 * form of the GTCEu GTEmiRecipe precedent (:22-26 each row wraps its category and
 * flattens the GT ingredient model), here flattening the port's {@link Recipe} arrays:
 * item inputs (with the never-consumed tooltip), item outputs (with the chance tooltip),
 * fluid inputs/outputs as native {@link EmiStack}s. The slots and the cost text land at
 * exactly the coordinates the shared {@link GT6RecipeMapViewerMeta} computes — the same
 * geometry the JEI twin renders (NEI_RecipeMap layout switch translation).
 *
 * <p>Id stability: the port's rows carry no registry id, so the id is the per-map sorted
 * index ({@code gt6:recipe_map/<internal>/<index>}); the sort is a simplified port of the
 * NEI sortRecipes key (NEI_RecipeMap.java:475-514 EUt-first) — EUt, then duration, then
 * the first input's string, then the first output's. ponytail: near-duplicate rows that
 * tie on all four keys may swap ids across sessions; cosmetic (bookmark instability at
 * worst), upgrade to registry-identity keys if a row universe ever grows duplicates.
 */
public class GT6RecipeMapEmiRecipe implements EmiRecipe {

	public final RecipeMap mMap;
	public final Recipe mRow;
	public final GT6RecipeMapEmiCategory mCategory;
	private final ResourceLocation mId;
	private final List<EmiIngredient> mInputs;
	private final List<EmiStack> mOutputs;
	/** The map's accepted-energy carrier ({@code null} = GU — no gear port, the decoration stays dead). */
	private final gregapi.code.TagData mEnergyCarrier;

	public GT6RecipeMapEmiRecipe(RecipeMap aMap, Recipe aRow, GT6RecipeMapEmiCategory aCategory, int aSortedIndex) {
		mMap = aMap;
		mRow = aRow;
		mCategory = aCategory;
		mEnergyCarrier = gregtech6.jei.GT6RecipeMapViewerMeta.energyOf(aMap);
		//? if forge {
		mId = ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal + "/" + aSortedIndex);
		//?} else {
		/*mId = ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal + "/" + aSortedIndex);
		 *///?}
		mInputs = new ArrayList<>();
		for (int i = 0; i < Math.min(aRow.mInputs.length, aMap.mInputItemsCount); i++) {
			if (aRow.mInputs[i] != null && !aRow.mInputs[i].isEmpty()) mInputs.add(EmiStack.of(aRow.mInputs[i]));
		}
		for (int i = 0; i < Math.min(aRow.mFluidInputs.length, aMap.mInputFluidCount); i++) {
			if (aRow.mFluidInputs[i] != null && !aRow.mFluidInputs[i].isEmpty())
				mInputs.add(EmiStack.of(aRow.mFluidInputs[i].getFluid(), aRow.mFluidInputs[i].getAmount()));
		}
		mOutputs = new ArrayList<>();
		for (int i = 0; i < Math.min(aRow.mOutputs.length, aMap.mOutputItemsCount); i++) {
			if (aRow.mOutputs[i] != null && !aRow.mOutputs[i].isEmpty()) mOutputs.add(EmiStack.of(aRow.mOutputs[i]));
		}
		for (int i = 0; i < Math.min(aRow.mFluidOutputs.length, aMap.mOutputFluidCount); i++) {
			if (aRow.mFluidOutputs[i] != null && !aRow.mFluidOutputs[i].isEmpty())
				mOutputs.add(EmiStack.of(aRow.mFluidOutputs[i].getFluid(), aRow.mFluidOutputs[i].getAmount()));
		}
	}

	@Override
	public GT6RecipeMapEmiCategory getCategory() {
		return mCategory;
	}

	@Override
	public ResourceLocation getId() {
		return mId;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return mInputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return mOutputs;
	}

	@Override
	public int getDisplayWidth() {
		return GT6RecipeMapViewerMeta.CATEGORY_WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return GT6RecipeMapViewerMeta.CATEGORY_HEIGHT;
	}

	/** No recipe tree this card: the transfer/ghost face is batch 4 (the card's 不做 clause). */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		// the composed page (task composed-ui-energy-slot-and-parts, the user ruling 配方页
		// 渲染切拼接 UI+删除独立机器 GUI 贴图) — render order = add order: the grey
		// machines/NEI.png plate (a 256x256 canvas, the 7-arg TextureWidget form), then the
		// two machine-skin furniture cells as part crops (the arrow cell, the special-slot
		// gear cell) via the 11-arg form — the parts are standalone small PNGs, the texture
		// dims are declared explicitly (emi-1.20.1 TextureWidget 11-arg ctor, xplat source).
		// The per-map machine GUI band is RETIRED: its baked slot frames doubled the code
		// slots' (贴图槽+代码槽叠加), so the frames now come only from the SlotWidgets
		// below (each draws its own back). The furniture positions are the meta's folded
		// exits — the same pixels the JEI twin blits.
		int[] tPlate = GT6RecipeMapViewerMeta.PLATE_CROP;
		aWidgets.add(new TextureWidget(GT6RecipeMapViewerMeta.PLATE_TEXTURE, 0, 0, tPlate[2], tPlate[3], tPlate[0], tPlate[1]));
		addPart(aWidgets, gregtech6.gui.machines.GT6GuiParts.ARROW_OUTLINE, GT6RecipeMapViewerMeta.viewerArrowPos());
		addPart(aWidgets, gregtech6.gui.machines.GT6GuiParts.SLOT_SPECIAL, GT6RecipeMapViewerMeta.viewerGearPos());
		// The gear-spot jump port (task viewer-energy-jump-gear, the user ruling): carrier
		// maps get the click widget fourth — right after the plate + the two furniture
		// parts (render order = add order, the art's z face), riding the special-slot
		// gear cell drawn above. GU maps
		// (mEnergyCarrier null) add nothing — the gear stays decoration (无载体图不画).
		// The jump consumer is injected so the offline tests can pin the click target
		// without touching EmiApi's static runtime.
		if (mEnergyCarrier != null) {
			aWidgets.add(new GearJumpWidget(mEnergyCarrier, GT6EmiPlugin::displayEnergyCarrierInfo));
		}
		// NO hand-drawn machine item on the plate's gear spot (task viewer-icon-retire-gu-pin,
		// the user ruling): EMI renders the workstation list itself (RecipeScreen.java:203-217,
		// fed by GT6EmiPlugin:157 addWorkstation) — the retired SlotWidget draw was an
		// over-generalization of upstream NEI_RecipeMap.java:278's rare non-empty
		// mRecipeMachineList branch. The category tab icon stays (ctor, the #29a face).
		for (int i = 0; i < Math.min(mRow.mInputs.length, mMap.mInputItemsCount); i++) {
			if (mRow.mInputs[i] == null || mRow.mInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerInputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			SlotWidget tSlot = aWidgets.add(new SlotWidget(EmiStack.of(mRow.mInputs[i]), tPos[0], tPos[1]));
			if (GT6RecipeMapViewerMeta.notConsumable(mRow.mInputs[i]))
				tSlot.appendTooltip(Component.translatable(GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY));
		}
		for (int i = 0; i < Math.min(mRow.mOutputs.length, mMap.mOutputItemsCount); i++) {
			if (mRow.mOutputs[i] == null || mRow.mOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerOutputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			// issue #34: NO .large(true) — EMI's large form is a 26x26 box anchored at the
			// passed coordinate (SlotWidget.getBounds output branch), which on the 18px
			// output pitch overlaps each neighbour by 8px and pushes a 3rd slot past the
			// 166-wide category. Upstream NEI drew faithful 18px slots — same as the JEI twin.
			SlotWidget tSlot = aWidgets.add(new SlotWidget(EmiStack.of(mRow.mOutputs[i]), tPos[0], tPos[1]));
			Component tChance = GT6RecipeMapViewerMeta.chanceLine(GT6RecipeMapViewerMeta.outputChance(mRow, i), mRow.mOutputs[i].getCount());
			if (tChance != null) tSlot.appendTooltip(tChance);
		}
		for (int i = 0; i < Math.min(mRow.mFluidInputs.length, mMap.mInputFluidCount); i++) {
			if (mRow.mFluidInputs[i] == null || mRow.mFluidInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidInputPos(i);
			aWidgets.add(new SlotWidget(EmiStack.of(mRow.mFluidInputs[i].getFluid(), mRow.mFluidInputs[i].getAmount()), tPos[0], tPos[1]));
		}
		for (int i = 0; i < Math.min(mRow.mFluidOutputs.length, mMap.mOutputFluidCount); i++) {
			if (mRow.mFluidOutputs[i] == null || mRow.mFluidOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidOutputPos(i);
			aWidgets.add(new SlotWidget(EmiStack.of(mRow.mFluidOutputs[i].getFluid(), mRow.mFluidOutputs[i].getAmount()), tPos[0], tPos[1]));
		}
		// drawExtras (:680-717 verbatim arithmetic) at the shared panel-system text band.
		int tY = GT6RecipeMapViewerMeta.TEXT_BASE_Y;
		for (Component tLine : GT6RecipeMapViewerMeta.costLines(mMap, mRow)) {
			aWidgets.addText(tLine, GT6RecipeMapViewerMeta.TEXT_X, tY, 0xFF000000, false);
			tY += GT6RecipeMapViewerMeta.TEXT_LINE_HEIGHT;
		}
	}

	/** One furniture part at its folded viewer position — the standalone-PNG TextureWidget (own texture dims declared). */
	private static void addPart(WidgetHolder aWidgets, gregtech6.gui.machines.GT6GuiParts.GuiPart aPart, int[] aPos) {
		aWidgets.add(new TextureWidget(aPart.texture(), aPos[0], aPos[1],
				aPart.width(), aPart.height(), 0, 0, aPart.width(), aPart.height(), aPart.width(), aPart.height()));
	}

	/**
	 * The gear-spot jump port of the EMI leg (task viewer-energy-jump-gear): a no-draw
	 * {@link dev.emi.emi.api.widget.Widget} over the folded gear rect — the art is the
	 * special-slot part drawn just above in {@link #addWidgets}, the widget only carries
	 * the hit box, the hint tooltip
	 * and the click. EMI's RecipeScreen routes mouse clicks to every non-{@code SlotWidget}
	 * widget whose bounds contain the cursor (RecipeScreen.java:429-436, the emi 1.1.24
	 * source) — pressed on mouse-down, no simulate/up split. The jump consumer is
	 * constructor-injected (production: {@link GT6EmiPlugin#displayEnergyCarrierInfo}) so
	 * the offline pin can drive clicks without EMI's static runtime.
	 */
	public static final class GearJumpWidget extends dev.emi.emi.api.widget.Widget {

		private final gregapi.code.TagData mCarrier;
		private final java.util.function.Consumer<gregapi.code.TagData> mJump;

		public GearJumpWidget(gregapi.code.TagData aCarrier, java.util.function.Consumer<gregapi.code.TagData> aJump) {
			mCarrier = aCarrier;
			mJump = aJump;
		}

		/** The folded gear rect — the same face the JEI twin's {@code IJeiInputHandler.getArea} returns. */
		@Override
		public dev.emi.emi.api.widget.Bounds getBounds() {
			int[] tPos = GT6RecipeMapViewerMeta.viewerGearPos();
			return new dev.emi.emi.api.widget.Bounds(tPos[0], tPos[1],
					GT6RecipeMapViewerMeta.GEAR_SIZE, GT6RecipeMapViewerMeta.GEAR_SIZE);
		}

		/** No draw — the gear art is the special-slot part added ahead of this widget. */
		@Override
		public void render(net.minecraft.client.gui.GuiGraphics aDraw, int aMouseX, int aMouseY, float aDelta) {
		}

		@Override
		public List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> getTooltip(int aMouseX, int aMouseY) {
			return List.of(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(
					Component.translatable(GT6RecipeMapViewerMeta.ENERGY_JUMP_HINT_KEY).getVisualOrderText()));
		}

		@Override
		public boolean mouseClicked(int aMouseX, int aMouseY, int aButton) {
			if (!getBounds().contains(aMouseX, aMouseY)) return false;
			mJump.accept(mCarrier);
			return true;
		}
	}
}
