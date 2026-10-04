package gregtech6.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.multiblock.GTLargeBoilerBlock;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.GT6HeatExchangerBlockEntity;
import gregtech6.tileentity.multiblocks.GTGasTurbineBlockEntity;
import gregtech6.tileentity.multiblocks.GTLargeDynamoBlockEntity;
import gregtech6.tileentity.multiblocks.GTSteamTurbineBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

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
 * <p><b>Growth = data batches (research.r11-mbpreview-batches).</b> First version = the
 * Coke Oven; batch A (task mbpreview-data-a-thermal) appended the thermal pair — the five
 * Large Boiler tiers (upstream Loader_MultiTileEntities.java:1248-1252) and the eight
 * Large Crucible tiers (:1270-1277), rows built off the port's own registration ladders
 * ({@link GTMultiBlocks#LARGE_BOILER_ROWS} / {@link GT6Crucibles#CRUCIBLE_ROWS} — the
 * upstream line order lives in exactly one place). Later batches tail-append their
 * family loop inside {@link #buildEntries()} after the previous batch's; the census test
 * re-pins the full name list per batch. Predicate-only cells (no {@code partBlock}) still
 * have no general display ruling — the boiler row is NOT one: its binding is
 * predicate-shaped, so the row re-stamps the display identity from its own part list
 * ({@link #withDisplayBlocks}) before tabling it; the per-machine ruling for genuinely
 * predicate-only machines belongs to the data card that tables them (research.
 * multiblock-preview risk column).
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
	 * declaration (the pattern-checker red line — never a transcribed copy). NO text
	 * description: the 3D preview speaks for itself (task mbpreview-shell-replicate —
	 * the former description face retired with the text-info pages).
	 */
	public record Entry(String name, Supplier<Item> item, Supplier<GTMultiBlockPattern> pattern) {

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
	 * The table. Every row's pattern supplier builds a THROWAWAY TileEntity —
	 * {@code getStructurePattern()} is a pure lazy declaration (no level access), the
	 * cheapest honest reuse of the one binding — and the suppliers stay lazy so the
	 * offline census never touches a registry.
	 */
	private static final List<Entry> ENTRIES = buildEntries();

	/**
	 * The row builder — the data-batch tail-append point (the batch doctrine, class doc):
	 * each batch adds its family loop BELOW the previous batch's, rows in the upstream
	 * registration order (the port's ladder lists are the upstream line order), and the
	 * census test re-pins the full name list per batch.
	 */
	private static List<Entry> buildEntries() {
		List<Entry> tRows = new ArrayList<>();
		tRows.add(new Entry("multiblock_coke_oven", GTMultiBlocks.COKE_OVEN_ITEM,
				() -> new TileEntityCokeOven(BlockPos.ZERO, Blocks.AIR.defaultBlockState()).getStructurePattern()));
		// --- batch A (mbpreview-data-a-thermal): the thermal pair -----------------------
		// the five Large Boiler tiers (upstream Loader_MultiTileEntities.java:1248-1252):
		// the throwaway BE carries its tier wall through the VARIANT STATE (the
		// construction-time-state doctrine — getWallBlock reads the row off the
		// GTLargeBoilerBlock state, TileEntityLargeBoiler.java:256-262); the binding is
		// predicate-shaped so the display identity re-stamps from the row's own parts
		for (GTMultiBlocks.LargeBoilerRow tRow : GTMultiBlocks.LARGE_BOILER_ROWS) {
			tRows.add(new Entry(tRow.path(), GTMultiBlocks.LARGE_BOILER_ITEMS_BY_PATH.get(tRow.path()),
					() -> boilerPreview(GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH.get(tRow.path()).get())));
		}
		// the eight Large Crucible tiers (upstream :1270-1277): the binding carries the
		// wall form directly (formingPart — zero re-stamp); the throwaway BE carries its
		// row's wall through the getWallBlock override (the level-gated production read,
		// TileEntityCrucible.java:294-299, answers a never-placed BE the fixture — the
		// override drives the SAME wallBlockOf resolution the placed controller does)
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.get(tRow.path()),
					() -> crucibleOf(tRow).getStructurePattern()));
		}
		// --- batch B (mbpreview-data-b-energy): the energy family -----------------------
		// the three converter families share the ONE GTMultiBlockConverter binding
		// (GTMultiBlockConverter.java:395 — the facing-rotated 3x3x4 shell): the throwaway
		// BE carries its TIER through the VARIANT STATE (the batch-A boiler doctrine —
		// getWallBlock reads the row's Dense Wall off the state, GTSteamTurbineBlockEntity
		// :47-53 / GTGasTurbineBlockEntity :100-106 / GTLargeDynamoBlockEntity :43-49) and
		// the binding is formingPart, so the wall identity rides the cells — zero re-stamp
		for (GT6Turbines.SteamTurbineRow tRow : GT6Turbines.STEAM_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Turbines.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTSteamTurbineBlockEntity(BlockPos.ZERO,
							GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		for (GT6Turbines.GasTurbineRow tRow : GT6Turbines.GAS_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6Turbines.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTGasTurbineBlockEntity(BlockPos.ZERO,
							GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		for (GT6DynamoHousings.DynamoRow tRow : GT6DynamoHousings.DYNAMO_ROWS) {
			tRows.add(new Entry(tRow.path(), GT6DynamoHousings.ITEMS_BY_PATH.get(tRow.path()),
					() -> new GTLargeDynamoBlockEntity(BlockPos.ZERO,
							GT6DynamoHousings.BLOCKS_BY_PATH.get(tRow.path()).get().defaultBlockState()).getStructurePattern()));
		}
		// the Implosion Compressor (upstream :1228) — the binding carries the shell form
		// directly (formingPart over the getPartBlock hook, TileEntityImplosionCompressor
		// :131-142; state-blind like the Coke Oven collect)
		tRows.add(new Entry("implosion_compressor", GTMultiBlocks.IMPLOSION_COMPRESSOR_ITEM,
				() -> new TileEntityImplosionCompressor(BlockPos.ZERO,
						Blocks.AIR.defaultBlockState()).getStructurePattern()));
		// the Large Heat Exchanger (upstream :1245) — the binding is predicate-shaped
		// (part() cells, GT6HeatExchangerBlockEntity:266-285), so the row re-stamps the
		// display identity from its own part list (the boiler form); the throwaway BE
		// carries the real carrier state so getWallBlock answers the Dense Tungsten Wall
		tRows.add(new Entry("large_heat_exchanger", GT6HeatExchangers.HEAT_EXCHANGER_ITEM,
				GT6MultiblockPreviews::heatExchangerPreview));
		// --- batch C (processing) tail-appends here; then D1/D2 (special) ---------------
		return List.copyOf(tRows);
	}

	/**
	 * The heat exchanger row's display pattern: the BE binding (predicate-only) re-stamped
	 * with the Dense Tungsten Wall + the Heat Transmitter (the two identities the
	 * predicates judge — zero transcription of the geometry).
	 */
	private static GTMultiBlockPattern heatExchangerPreview() {
		return withDisplayBlocks(
				new GT6HeatExchangerBlockEntity(BlockPos.ZERO,
						GT6HeatExchangers.HEAT_EXCHANGER_BLOCK.get().defaultBlockState()).getStructurePattern(),
				GTMultiBlocks.WALL_BLOCKS_BY_PATH.get("dense_wall_tungsten").get(),
				GTMultiBlocks.HEAT_TRANSMITTER.get());
	}

	/**
	 * The boiler row's display pattern: the BE binding (display-only — the structure
	 * check rides the hand loop, TileEntityLargeBoiler.java:287-331) re-stamped with the
	 * variant's own wall block + the heat transmitter (the two identities the predicates
	 * judge — zero transcription of the geometry).
	 */
	private static GTMultiBlockPattern boilerPreview(GTLargeBoilerBlock aBlock) {
		return withDisplayBlocks(
				new TileEntityLargeBoiler(BlockPos.ZERO, aBlock.defaultBlockState()).getStructurePattern(),
				aBlock.wallBlock(), GTMultiBlocks.HEAT_TRANSMITTER.get());
	}

	/** The throwaway crucible BE carrying its row's wall (the getWallBlock override above). */
	private static TileEntityCrucible crucibleOf(GT6Crucibles.CrucibleRow aRow) {
		return new TileEntityCrucible(BlockPos.ZERO, Blocks.AIR.defaultBlockState()) {
			@Override
			protected Block getWallBlock() {
				return GT6Crucibles.wallBlockOf(aRow);
			}
		};
	}

	/**
	 * The display-identity stamp for predicate-shaped bindings: cells declared with
	 * {@code part(predicate)} carry {@code partBlock == null} (the seam draws nothing for
	 * them), so each cell takes the FIRST part candidate its predicate accepts — the
	 * judgement and the stamp share one identity. {@code formingPart} cells pass through
	 * untouched (their partBlock is already the identity); an unresolvable cell keeps its
	 * predicate shape (the seam's draw-nothing ruling).
	 */
	static GTMultiBlockPattern withDisplayBlocks(GTMultiBlockPattern aPattern, Block... aParts) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			Block tBlock = tCell.partBlock;
			if (tBlock == null && !tCell.isHollow()) {
				for (Block tPart : aParts) {
					if (tCell.predicate.test(tPart.defaultBlockState())) {
						tBlock = tPart;
						break;
					}
				}
			}
			if (tCell.isHollow()) tBuilder.hollow(tCell.x, tCell.y, tCell.z, tCell.predicate);
			else if (tBlock != null) tBuilder.formingPart(tCell.x, tCell.y, tCell.z, tBlock, tCell.usage, tCell.design);
			else tBuilder.part(tCell.x, tCell.y, tCell.z, tCell.predicate);
		}
		return tBuilder.build();
	}

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
