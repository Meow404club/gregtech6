package gregtech6.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.mojang.blaze3d.platform.InputConstants;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.widget.IGuiAction;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.SchemaRenderer;
import brachy.modularui.drawable.schema.BlockHighlight;
import brachy.modularui.drawable.schema.MapSchema;
import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.integration.recipeviewer.RecipeViewerSlotWidget;
import brachy.modularui.utils.Color;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ItemDisplayWidget;
import brachy.modularui.widgets.SchemaWidget;
import brachy.modularui.widgets.dynamic.DynamicHandler;
import brachy.modularui.widgets.dynamic.DynamicWidget;
import brachy.modularui.widgets.layout.Flow;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The multiblock structure preview page (task multiblock-preview-infra, shell replicated
 * in task mbpreview-shell-replicate) — the GT6 port of GTCEu Modern's
 * {@code MultiblockPreviewWidget} (MultiblockPreviewWidget.java, 499 lines) over the
 * port's fixed-size data seam: a ray-tracing 3D {@link SchemaRenderer} over a virtual
 * {@link MapSchema} level filled from the machine's {@link GTMultiBlockPattern}, the
 * {@link BlockHighlight} green frame on the traced cell, left-click cell selection, the
 * y-level layer filter ({@link SchemaWidget.LayerButton}, the vendored MUI's own state
 * machine), and the viewer-recognized material column. Both viewer legs embed the SAME
 * widget — the JEI category and the EMI wrapper are thin holders around this constructor
 * (the GTCEu shared-widget shape, MultiblockInfoEmiCategory.java:68 /
 * MultiblockInfoJeiCategory.java:46).
 *
 * <p><b>The GT6 degrades, declared</b> (the card's mapping table): no slice/size sliders
 * — GT6 patterns are fixed-size immutable cell lists ({@code ITileEntityMultiBlockController}
 * :57-59, no consumer for repeats); no per-predicate swap menu on selection — a GT6 cell
 * carries a bare {@code Predicate} with no candidate list, so the selected cell simply
 * NAMES its block (the {@code partBlock} enumeration face, GTMultiBlockPattern.java:134);
 * no in-world preview button — the port's hologram is a separate card (P12 renderer, id331).
 * Drag-rotate / scroll-zoom stay (SchemaWidget defaults) — free value.
 *
 * <p>Construction happens ONLY inside a live viewer page (the JEI category's
 * wrapperFunction and the EMI wrapper's supplier are lazy), where the vendored ModularUI
 * schema stack is at home — the offline tests pin the widget's INPUTS at the
 * {@link GT6MultiblockPreviews} model seam and the widget's STRUCTURE at the bytecode
 * layer instead.
 */
public class GT6MultiblockPreviewWidget extends ParentWidget<GT6MultiblockPreviewWidget> {

	/** The top strip's height: the selection display cell and the layer filter button. */
	private static final int TOP_STRIP = 20;
	/** The material column's per-slot cell (18 = the standard item-slot box). */
	private static final int PART_CELL = 18;

	/** The left-clicked cell — the GTCEu SelectionInfo, degraded to (pos, state). */
	private SelectionInfo selectionInfo = SelectionInfo.empty();
	/** Rebuilds the selection display when a click lands (the GTCEu selectedBlockHandler). */
	private final DynamicHandler selectedBlockHandler = new DynamicHandler();

	public GT6MultiblockPreviewWidget(GT6MultiblockPreviews.Entry aEntry, int aWidth, int aHeight) {
		// the pattern captured once — the tooltip's candidate lookups ride the SAME
		// instance (the fusion page is 887 cells; per-frame supplier calls would churn)
		GTMultiBlockPattern tPattern = aEntry.pattern().get();
		Map<BlockPos, BlockState> tBlocks = GT6MultiblockPreviews.structureBlocks(
				tPattern, aEntry.controllerBlock(), GT6MultiblockPreviews.DISPLAY_FACING, aEntry.controllerCell());

		// the ray-tracing renderer with the green frame — the GTCEu renderer face
		// (MultiblockPreviewWidget.java:102-103, verbatim color/thickness)
		SchemaRenderer tRenderer = new SchemaRenderer(new MapSchema(tBlocks))
				.highlightRenderer(new BlockHighlight(Color.withAlpha(Color.GREEN.brighter(1), 0.9f), 1 / 32f));

		// the 3D view: the former 40px description strip's budget goes back to it
		int tViewWidth = aWidth - PART_CELL - 4;
		SchemaWidget tView = tRenderer.asWidget()
				.listenGuiAction((IGuiAction.MouseReleased) (tContext, tButton) -> {
					// left-click select: the GTCEu setBlockOnClick (:105-117), reading the
					// port's own fill map instead of a cloned stack
					if (tButton != InputConstants.MOUSE_BUTTON_LEFT) return false;
					BlockHitResult tHit = tRenderer.lastRayTrace();
					if (tHit == null || tHit.getType() != HitResult.Type.BLOCK) return false;
					BlockState tState = tBlocks.get(tHit.getBlockPos());
					if (tState == null) return false;
					this.selectionInfo = SelectionInfo.of(tHit, tState);
					this.selectedBlockHandler.notifyUpdate();
					return true;
				})
				.tooltipDynamic(tText -> {
					// hover names the traced cell (the GTCEu tooltipDynamic :164-174, the
					// port's degrade: the map lookup replaces getCloneItemStack+player)
					BlockHitResult tHit = tRenderer.lastRayTrace();
					if (tHit != null && tHit.getType() == HitResult.Type.BLOCK) {
						BlockState tState = tBlocks.get(tHit.getBlockPos());
						if (tState != null) {
							tText.addFromItem(new ItemStack(tState.getBlock()));
							// the predicate quota cells list their REMAINING candidates
							// (the r11 Q3 degrade ruling: first candidate renders, the
							// rest join the tooltip — GT6MultiblockPreviews D2 face)
							for (Block tCandidate : GT6MultiblockPreviews.cellCandidateBlocks(aEntry.name(),
									GT6MultiblockPreviews.cellAt(tPattern, tHit.getBlockPos()))) {
								if (!tState.is(tCandidate)) tText.addFromItem(new ItemStack(tCandidate));
							}
						}
					}
				})
				.tooltipAutoUpdate(true)
				.size(tViewWidth, aHeight - TOP_STRIP - 4);

		// the y-level filter — the vendored LayerButton state machine (ALL → layers → ALL,
		// left = expand up / right = cut down); its ctor installs the render filter
		int tMinY = tBlocks.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
		int tMaxY = tBlocks.keySet().stream().mapToInt(BlockPos::getY).max().orElse(0);
		SchemaWidget.LayerButton tLayerFilter = new SchemaWidget.LayerButton(tRenderer, tMinY, tMaxY).size(TOP_STRIP, TOP_STRIP);

		// the selection display (the GTCEu "selected_block" DynamicWidget :231-234): the
		// degrade shows WHAT THE CELL IS — a fixed GT6 structure has no candidates to swap
		this.selectedBlockHandler.widgetProvider(() -> {
			ItemStack tSelected = this.selectionInfo.stack();
			if (tSelected.isEmpty()) return null;
			return new ItemDisplayWidget()
					.item(tSelected)
					.tooltip(tText -> tText.addFromItem(tSelected));
		});

		// the material column — viewer-recognized OUTPUT-role slots (the GTCEu
		// parts face :119-134, U-hover works on the listed materials)
		List<IWidget> tParts = new ArrayList<>();
		for (Map.Entry<Block, Integer> tCount : GT6MultiblockPreviews
				.materialCounts(tPattern, aEntry.controllerBlock(), GT6MultiblockPreviews.DISPLAY_FACING,
						aEntry.controllerCell())
				.entrySet()) {
			tParts.add(tMaterialSlot(new ItemStack(tCount.getKey(), tCount.getValue())));
		}

		this.coverChildren().padding(2)
				.child(Flow.col().coverChildren()
						.child(Flow.row().coverChildren()
								.childPadding(2)
								.child(new DynamicWidget<>()
										.coverChildrenWidth(TOP_STRIP)
										.coverChildrenHeight(TOP_STRIP)
										.clientOnlyHandler(this.selectedBlockHandler))
								.child(tLayerFilter))
						.child(Flow.row().coverChildren()
								.child(tView)
								.child(Flow.col().wrap()
										.coverChildrenWidth(PART_CELL)
										.height(aHeight - TOP_STRIP - 4)
										.childPadding(1)
										.children(tParts))));
	}

	/**
	 * The material slot factory — the ONE per-leg fork in the shell: the vendored fork's
	 * slot API differs by leg (forge 15.x {@code create(Class)} carries the ingredient
	 * class with a DUMMY fallback, forgeMain RecipeViewerSlotWidget.java:69; the 1.21.1
	 * create is erased, neoforgeMain :35) — everything after {@code create} is the shared
	 * GTCEu chain (OUTPUT role, no slot background, 16px, item tooltip).
	 */
	private static IWidget tMaterialSlot(ItemStack aStack) {
		//? if forge {
		return RecipeViewerSlotWidget.create(ItemStack.class)
		//?} else {
		/*return RecipeViewerSlotWidget.create()
		 *///?}
				.recipeSlotRole(RecipeSlotRole.OUTPUT)
				.value(aStack)
				.background(IDrawable.EMPTY)
				.size(16)
				.margin(1)
				.tooltip(tText -> tText.addFromItem(aStack));
	}

	/** The clicked cell — the GTCEu SelectionInfo (MultiblockPreviewWidget.java:477-498), degraded. */
	private record SelectionInfo(BlockPos pos, BlockState state) {

		static SelectionInfo empty() {
			return new SelectionInfo(BlockPos.ZERO, null);
		}

		static SelectionInfo of(BlockHitResult aHit, BlockState aState) {
			return new SelectionInfo(aHit.getBlockPos(), aState);
		}

		/** The cell's block as an item — empty when the cell has no item form. */
		ItemStack stack() {
			Block tBlock = state == null ? null : state.getBlock();
			return tBlock == null || tBlock.asItem() == net.minecraft.world.item.Items.AIR
					? ItemStack.EMPTY
					: new ItemStack(tBlock);
		}
	}
}
