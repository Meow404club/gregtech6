package gregtech6.block.surface;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregapi.oredict.OreDictMaterial;

/**
 * The GT6 surface rock (task p30-w6-rocks-sticks) — the research winner's borrowed shape:
 * GTCEu Modern {@code SurfaceRockBlock.java:44-187} (FACING 6-state + micro box + canSurvive
 * sturdy attach + right-click pickup) per-material, replacing the upstream MTE 32757
 * (MultiTileEntityRock: no-tick TE, hardness 0.25 :250, light opacity 0 :248, random
 * 1-4/16 micro box :63-67, right-click pickup :143-148, support-lost drop :151-163).
 *
 * <p>Declared deviations (research.p30-w6-surface-rock deviations_to_declare, all
 * GTCEu-shared):
 * <ul>
 * <li><b>texture sampling</b> — upstream samples the block below (MultiTileEntityRock
 *     :196-232 getRenderPasses); a 1.20.1 static model cannot read neighbour textures, so
 *     the block renders ONE shared grayscale model tinted with the material RGB (GTCEu
 *     SurfaceRockModelGenerator + tintedBlockColor :154-161 same deviation).</li>
 * <li><b>default-rock computation</b> — upstream computes the dimension/biome default rock
 *     at break time (MultiTileEntityRock.java:169-187); here the three first-batch rocks
 *     carry FIXED loot identities (per-pair blocks over the zero-material-NBT-port ruling,
 *     research.p27-nbt-creative-tab-design), the per-biome face rides the loot-table
 *     condition seam (deferred with the material-stick/biome band).</li>
 * <li><b>random micro box</b> — upstream randomises the 1-4/16 box per placement NBT; the
 *     shared model pins one fixed micro box (GTCEu same).</li>
 * <li><b>liquid-adjacent drop</b> — the upstream onNeighborBlockChange liquid arm (:158-162)
 *     needs a block update only on neighbour changes (a plain-block neighbourChanged has no
 *     fluid-update hook without ticking); GTCEu carries the same cut (:134-141 support
 *     check only).</li>
 * </ul>
 *
 * <p>No BlockItem: the upstream rock is never obtainable as a block (right-click collects
 * the carried item, MultiTileEntityRock :143-148) — the loot table is the only item path.
 */
public class GT6SurfaceRockBlock extends Block {

	/** GTCEu SurfaceRockBlock.java:46 — the full 6-direction attach facing, default DOWN. */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/**
	 * DOWN/UP tightened to the model's 8x3x8 micro box (task p38-issue1-4, GitHub #1 —
	 * the MultiTileEntityRock.java:58 default envelope PX_P[4]..PX_N[4] centered, the 3px
	 * p30 pebble height): the selection box rides the same bounds as the visual, the
	 * upstream GetSelectedBoundingBoxFromPool face (MultiTileEntityRock.java:237 —
	 * {@code box(mMinX, 0, mMinZ, mMaxX, mMaxY, mMaxZ)}); the GTCEu :48-53 12/16 slab
	 * stood 2px proud per side over the shrunk visual. The four WALL shapes stay GTCEu
	 * :48-53 verbatim (their visuals ride the y-only variant-rotation quirk,
	 * GT6BlockStates addSurfaceBand). The stick subclass inherits DOWN/UP (its 12x2x2
	 * bar is a closer fit than the old slab; the bar-exact shape stays a pool candidate).
	 */
	protected static final VoxelShape SHAPE_DOWN = Block.box(4, 0, 4, 12, 3, 12);
	protected static final VoxelShape SHAPE_UP = Block.box(4, 13, 4, 12, 16, 12);
	protected static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 0, 14, 14, 3);
	protected static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 13, 14, 14, 16);
	protected static final VoxelShape SHAPE_WEST = Block.box(0, 2, 2, 3, 14, 14);
	protected static final VoxelShape SHAPE_EAST = Block.box(13, 2, 2, 16, 14, 14);

	/** The tint material (GTCEu :55-60); null on the material-less stick subclass (no tint face). */
	@Nullable
	protected final OreDictMaterial material;

	public GT6SurfaceRockBlock(Properties aProperties, @Nullable OreDictMaterial aMaterial) {
		super(aProperties);
		this.material = aMaterial;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN)); // GTCEu :62
	}

	/** The client tint (GTCEu tintedBlockColor :154-161): the material's solid RGB, -1 = no tint. */
	public int tintARGB() {
		if (material == null) return -1;
		return 0xFF000000 | (material.mRGBaSolid[0] << 16) | (material.mRGBaSolid[1] << 8) | material.mRGBaSolid[2];
	}

	/**
	 * Upstream onBlockActivated2 (:143-148) right-click pickup in the GTCEu :93 form:
	 * destroy with drops — the loot table carries the items (the exact-1 upstream give
	 * rides the loot count minimum; the fortune band stays for the break path).
	 */
	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(the GTExampleChestBlock fork precedent).
	 *///?}
		if (aLevel.isClientSide()) return InteractionResult.SUCCESS;
		return aLevel.destroyBlock(aPos, true, aPlayer) ? InteractionResult.SUCCESS : InteractionResult.PASS; // GTCEu :93-96
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return switch (aState.getValue(FACING)) { // GTCEu :101-110
			case DOWN -> SHAPE_DOWN;
			case UP -> SHAPE_UP;
			case NORTH -> SHAPE_NORTH;
			case SOUTH -> SHAPE_SOUTH;
			case WEST -> SHAPE_WEST;
			case EAST -> SHAPE_EAST;
		};
	}

	/** GTCEu :126-131 — the attach face must be sturdy (upstream :153 isSideSolid check). */
	@Override
	public boolean canSurvive(BlockState aState, LevelReader aLevel, BlockPos aPos) {
		Direction tFacing = aState.getValue(FACING);
		BlockPos tAttached = aPos.relative(tFacing);
		return aLevel.getBlockState(tAttached).isFaceSturdy(aLevel, tAttached, tFacing.getOpposite());
	}

	/** GTCEu :134-141 — break off (no drops: the pickup-only deco loses nothing; vanilla flower form). */
	@Override
	public void neighborChanged(BlockState aState, Level aLevel, BlockPos aPos, Block aNeighborBlock, BlockPos aNeighborPos, boolean aMovedByPiston) {
		super.neighborChanged(aState, aLevel, aPos, aNeighborBlock, aNeighborPos, aMovedByPiston);
		if (!canSurvive(aState, aLevel, aPos)) {
			Block.updateOrDestroy(aState, Blocks.AIR.defaultBlockState(), aLevel, aPos, Block.UPDATE_ALL);
		}
	}

	/** GTCEu :145-151 — the placement facing = the nearest look axis (worldgen places DOWN directly). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		return defaultBlockState().setValue(FACING, aContext.getNearestLookingVerticalDirection());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		aBuilder.add(FACING); // GTCEu :174-177
	}

	/**
	 * The composed display name (the GTStoneBlock B-wave canon): ONE
	 * {@code gt6.surface.rock} template with the {@code gt6.material.<snake>} small-unit
	 * slot — per-pair naming is a modern necessity (one block id per material needs
	 * distinct names; the upstream MTE name was the generic "Rock"). The material-less
	 * stick subclass composes its own single key instead.
	 */
	@Override
	public MutableComponent getName() {
		if (material == null) return Component.translatable("block.gt6.surface_stick");
		return Component.translatable("gt6.surface.rock",
				Component.translatable("gt6.material." + gregtech6.item.MaterialPrefixItem.snakeCase(material.mNameInternal)));
	}
}
