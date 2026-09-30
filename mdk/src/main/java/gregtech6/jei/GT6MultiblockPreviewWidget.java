package gregtech6.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import brachy.modularui.drawable.schema.MapSchema;
import brachy.modularui.utils.Alignment;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ItemDisplayWidget;
import brachy.modularui.widgets.SchemaWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;

/**
 * The multiblock structure preview page (task multiblock-preview-infra) — the port's
 * simplified {@code MultiblockPreviewWidget}: a 3D SchemaWidget over a virtual
 * {@link MapSchema} level filled from the machine's {@link GTMultiBlockPattern}, a
 * material-count column, and the description line (the former text-info page body, folded
 * in). Both viewer legs embed the SAME widget — the JEI category and the EMI wrapper are
 * thin holders around this constructor (the GTCEu shared-widget shape,
 * MultiblockInfoEmiCategory.java:68 / MultiblockInfoJeiCategory.java:46).
 *
 * <p><b>v1 cuts, deliberately</b> (the card's "砍" list, GTCEu parity deferred): no
 * click-to-select-cell with per-predicate swap menus (their SelectionInfo +
 * selectedBlockHandler), no layer-filter button (SchemaWidget.LayerButton), no
 * in-world hologram preview button (PatternPreviewRenderer), no
 * {@code BlockHighlight} green frame (it renders ray-trace hits — nothing ray-traces
 * here). Drag-rotate / scroll-zoom stay (SchemaWidget defaults) — free value.
 *
 * <p>Construction happens ONLY inside a live viewer page (the JEI category's
 * wrapperFunction and the EMI wrapper's supplier are lazy), where the vendored ModularUI
 * schema stack is at home — the offline tests pin the widget's INPUTS at the
 * {@link GT6MultiblockPreviews} model seam instead.
 */
public class GT6MultiblockPreviewWidget extends ParentWidget<GT6MultiblockPreviewWidget> {

	/** The description strip's height budget (scale-0.5 lines) under the 3D view. */
	private static final int DESCRIPTION_HEIGHT = 40;
	/** The material column's per-slot cell (18 = the standard item-slot box). */
	private static final int PART_CELL = 18;

	public GT6MultiblockPreviewWidget(GT6MultiblockPreviews.Entry aEntry, int aWidth, int aHeight) {
		Map<BlockPos, BlockState> tBlocks = GT6MultiblockPreviews.structureBlocks(
				aEntry.pattern().get(), aEntry.controllerBlock(), GT6MultiblockPreviews.DISPLAY_FACING);

		int tViewWidth = aWidth - PART_CELL - 4;
		SchemaWidget tView = new SchemaWidget(new MapSchema(tBlocks).createRenderer())
				.enableAllInteraction(true)
				.size(tViewWidth, aHeight - DESCRIPTION_HEIGHT);

		TextWidget<?> tText = new TextWidget<>(aEntry.description())
				.scale(0.5f)
				.width(tViewWidth)
				.textAlign(Alignment.TopLeft);

		// Flow.children takes Iterable<IWidget> — the material widgets ride the base type
		List<brachy.modularui.api.widget.IWidget> tParts = new ArrayList<>();
		for (Map.Entry<Block, Integer> tCount : GT6MultiblockPreviews
				.materialCounts(aEntry.pattern().get(), aEntry.controllerBlock(), GT6MultiblockPreviews.DISPLAY_FACING)
				.entrySet()) {
			tParts.add(new ItemDisplayWidget()
					.item(new ItemStack(tCount.getKey(), tCount.getValue()))
					.displayAmount(true));
		}

		this.coverChildren().padding(2)
				.child(Flow.row().coverChildren()
						.child(Flow.col().coverChildren()
								.child(tView)
								.child(tText))
						.child(Flow.col().wrap()
								.coverChildrenWidth(PART_CELL)
								.height(aHeight)
								.childPadding(1)
								.children(tParts)));
	}
}
