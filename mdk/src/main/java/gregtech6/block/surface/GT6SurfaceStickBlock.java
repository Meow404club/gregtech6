package gregtech6.block.surface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The GT6 surface stick (task p30-w6-rocks-sticks) — ONE block for the upstream MTE 32756
 * (WorldgenSticks.java:65 places it bare, MultiTileEntityStick: no NBT, hardness 0.25 :189,
 * no collision :177, fire 300/300 :190-191). Same attach/pickup/micro-slab behaviour as the
 * rock (the :72-99 rows are the MultiTileEntityRock rows re-run for sticks), so this is a
 * subclass carrying the bar selection shape, the flammable face and the wood sound/map
 * colour.
 *
 * <p>Declared deviation (research row "stick 群系名子串"): the upstream getDefaultStick
 * biome-substring ladder (MultiTileEntityStick.java:112-153, ~25 wood materials + the
 * 3/16 Dead/Mossy/Rotten roll) is NOT materialised — the first batch drops the vanilla
 * stick (research verdict "首批可收敛 vanilla stick"); the wood band lands as loot-table
 * biome conditions when the material-stick item face is consumed (no 25 blocks — the card
 * pin).
 */
public class GT6SurfaceStickBlock extends GT6SurfaceRockBlock {

	/**
	 * Task r3-stick-shape-random (GitHub #12) — the bar-exact selection shapes, replacing
	 * the inherited rock pebble boxes: upstream GetSelectedBoundingBoxFromPool rides the
	 * per-instance visual box (MultiTileEntityStick.java:176), so the wireframe must be the
	 * lying 12x2x2 bar of the pinned default pose (the :53 bounds 2..14 x 7..9), not the
	 * 8x3x8 pebble the stick used to inherit. Each facing is the default bar carried
	 * through the same rotation the blockstate FACING dispatch applies; the render-side
	 * weighted slide/rotation variants (GT6BlockStates addSurfaceBand) overhang these
	 * bounds by at most 2px — the envelope union would be the old full pelt again, so the
	 * exact default pose wins (upstream slides 0..14px per instance, far wider). The
	 * collision side stays empty ({@code noCollission()}, the upstream :177 null).
	 */
	private static final VoxelShape BAR_SHAPE_DOWN = Block.box(2, 0, 7, 14, 2, 9);
	private static final VoxelShape BAR_SHAPE_UP = Block.box(2, 14, 7, 14, 16, 9);
	private static final VoxelShape BAR_SHAPE_NORTH = Block.box(2, 7, 14, 14, 9, 16);
	private static final VoxelShape BAR_SHAPE_SOUTH = Block.box(2, 7, 0, 14, 9, 2);
	private static final VoxelShape BAR_SHAPE_WEST = Block.box(7, 0, 2, 9, 2, 14);
	private static final VoxelShape BAR_SHAPE_EAST = Block.box(7, 0, 2, 9, 2, 14);

	public GT6SurfaceStickBlock(Properties aProperties) {
		super(aProperties.sound(SoundType.WOOD).mapColor(MapColor.WOOD), null);
	}

	/** The bar-exact override (see the shape javadoc above); the rock table stays in the superclass. */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return switch (aState.getValue(FACING)) {
			case DOWN -> BAR_SHAPE_DOWN;
			case UP -> BAR_SHAPE_UP;
			case NORTH -> BAR_SHAPE_NORTH;
			case SOUTH -> BAR_SHAPE_SOUTH;
			case WEST -> BAR_SHAPE_WEST;
			case EAST -> BAR_SHAPE_EAST;
		};
	}

	/** MultiTileEntityStick.java:190-191 verbatim — the IForgeBlock 4-arg faces (the GTTankValveBlock form, both legs). */
	@Override
	public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return 300;
	}

	@Override
	public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return 300;
	}
}
