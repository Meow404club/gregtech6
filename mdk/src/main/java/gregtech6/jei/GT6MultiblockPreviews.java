package gregtech6.jei;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * The multiblock preview registry (task multiblock-preview-infra): ONE static
 * item→pattern table — the {@link GT6RecipeMapIcons} hand-curated-row precedent — plus the
 * viewer-neutral model seam (pattern → schema blocks / material counts) that BOTH viewer
 * legs consume. The GTCEu Modern shape this modernizes is
 * {@code MultiblockInfoEmiCategory.registerDisplays} (machines-filter loop) collapsed into
 * this port's explicit table, because the port's multiblocks bind their shape on the
 * TileEntity ({@code getStructurePattern()}) instead of a definition registry — the table
 * row IS the machine census (a data card appends rows; every viewer face picks them up
 * automatically).
 *
 * <p><b>First version = Coke Oven only</b> (the card face): its pattern is the complete
 * forming declaration (26 {@code formingPart} bricks + the hollow centre,
 * TileEntityCokeOven.java:119-130), so the 3D page renders fully. The other 10+ machines
 * with bound patterns join through later data cards — predicate-only cells (no
 * {@code partBlock}) have no representative block and each machine needs its own display
 * ruling (research.multiblock-preview risk column).
 *
 * <p>Offline-test contract: loading this class never touches a registry (the
 * {@code Supplier}s are lazy), so the census test runs bare-JVM; the model seam is pure
 * (fixture patterns over vanilla blocks) — the SAME functions the widget feeds
 * {@code MapSchema} with, asserted offline at the layer that needs no client.
 */
public final class GT6MultiblockPreviews {

	/**
	 * The display facing (north = 2, {@code Direction.NORTH.get3DDataValue()}) — the
	 * anchor arithmetic's rotation input. The GT6 anchor convention ("Main Block centered
	 * on Side and facing outwards") puts the CONTROLLER cell at centre-relative
	 * {@code -anchorOffset(facing)}; rendering one canonical facing keeps the page
	 * deterministic (no rotation buttons in v1).
	 */
	public static final byte DISPLAY_FACING = 2;

	/** The preview page size both viewer legs declare (the GTCEu 200x180 page shape). */
	public static final int PAGE_WIDTH = 200;
	public static final int PAGE_HEIGHT = 180;

	/** The category uid path — {@code gt6:multiblock_preview} on both legs (JEI RecipeType + EMI category id). */
	public static final String UID_PATH = "multiblock_preview";

	/** The shared category title key — the producer half lives in GT6EnUs/GT6ZhCn. */
	public static final String TITLE_KEY = "gt6.jei.multiblock_preview";

	/**
	 * One table row: the registry name doubles as the offline census key and the recipe-id
	 * suffix; the item is the controller's BlockItem (the U anchor); the pattern supplier
	 * re-reads the machine's own binding so the page and the server check share ONE
	 * declaration (the pattern-checker red line — never a transcribed copy).
	 */
	public record Entry(String name, Supplier<Item> item, Supplier<GTMultiBlockPattern> pattern,
			Component description) {

		/**
		 * The controller {@link Block} — the anchor cell is painted with it (the GT6
		 * pattern marks every structural cell with the part block, the controller cell
		 * included — the research card's coke-oven special case, generalized to the table:
		 * whichever cell sits at the anchor IS the controller for every GT6 shape).
		 */
		@Nullable
		public Block controllerBlock() {
			return item().get() instanceof BlockItem tBlockItem ? tBlockItem.getBlock() : null;
		}
	}

	/**
	 * The table. The coke-oven row's pattern supplier builds a THROWAWAY TileEntity —
	 * {@code getStructurePattern()} is a pure lazy declaration (no level access,
	 * TileEntityCokeOven.java:119-130), the cheapest honest reuse of the one binding.
	 * The description is the shared info-page text (task debt-emi-tier-b seam) folded
	 * into the page — the text-info pages this card replaces.
	 */
	private static final List<Entry> ENTRIES = List.of(
			new Entry("multiblock_coke_oven", GTMultiBlocks.COKE_OVEN_ITEM,
					() -> new TileEntityCokeOven(BlockPos.ZERO, Blocks.AIR.defaultBlockState()).getStructurePattern(),
					GT6RecipeViewerText.cokeOvenInfo()));

	/** The live rows — one per previewed machine (the data cards' growth point). */
	public static List<Entry> entries() {
		return ENTRIES;
	}

	/**
	 * The schema fill: the pattern's structural cells as {@code (centre-relative pos →
	 * state)} — hollow markers stay air, the anchor cell carries the CONTROLLER block
	 * (see {@link Entry#controllerBlock}), forming cells their {@code partBlock}.
	 * Predicate-only cells (no partBlock, not the anchor) draw NOTHING in v1 — the
	 * per-machine display ruling belongs to the data card that tables the machine
	 * (ponytail: revisit only when the first predicate-only machine joins the table).
	 * Insertion-ordered: the cell declaration order (the upstream loop order) survives
	 * for stable rendering and material listing.
	 */
	public static Map<BlockPos, BlockState> structureBlocks(GTMultiBlockPattern aPattern, @Nullable Block aController, byte aFacing) {
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(aFacing);
		Map<BlockPos, BlockState> tMap = new LinkedHashMap<>();
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			if (tCell.isHollow()) continue;
			boolean tIsController = tCell.x == -tAnchor[0] && tCell.y == -tAnchor[1] && tCell.z == -tAnchor[2];
			Block tBlock = tIsController ? aController : tCell.partBlock;
			if (tBlock == null) continue;
			tMap.put(new BlockPos(tCell.x, tCell.y, tCell.z), tBlock.defaultBlockState());
		}
		return tMap;
	}

	/**
	 * The material list: block → count over the schema fill — the "what do I need to
	 * build this" column (the GTCEu initializeContainedBlocks face). The controller
	 * counts as its own material (1x) — the anchor cell IS one controller.
	 */
	public static Map<Block, Integer> materialCounts(GTMultiBlockPattern aPattern, @Nullable Block aController, byte aFacing) {
		Map<Block, Integer> tCounts = new LinkedHashMap<>();
		for (BlockState tState : structureBlocks(aPattern, aController, aFacing).values()) {
			tCounts.merge(tState.getBlock(), 1, Integer::sum);
		}
		return tCounts;
	}

	private GT6MultiblockPreviews() { }
}
