package gregtech6.block.surface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
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
	 * Task debt-issue12-shape-follow-tilt (GitHub #12 residual): the selection box now
	 * FOLLOWS the render variant — {@link GT6SurfaceVariants#pick} replays the
	 * renderer's position-seeded draw (the chain pinned in that class's javadoc) over
	 * the stick table, and the variant's raw box rides the same FACING x-rotation +
	 * arm y-rotation the blockstate dispatch emits (upstream
	 * GetSelectedBoundingBoxFromPool rides the per-instance visual box,
	 * MultiTileEntityStick.java:176). The exact boxes: the two centered arms and the
	 * two slide tiers are bar-EXACT (the r3-stick-shape-random bar-exact face kept);
	 * the four tilt variants carry the declared ENVELOPE deviation — a ±22.5/45
	 * tilted bar's quads are not axis-aligned and VoxelShape cannot express them, so
	 * the wireframe is the conservative axis-aligned box over the tilted bar
	 * (overhang ~1.5px per side, GT6SurfaceVariants.Stick javadoc). A null-pos call
	 * falls back to the default centered bar; the C2-era six-facing pins move WITH this
	 * card — the old table carried NORTH/SOUTH transposed against the emitted dispatch
	 * (the wireframe sat on the wrong side of the block for the wall facings; the pin
	 * test passed tautologically against the same wrong table — see
	 * GT6SurfaceVariants.rotate for the vanilla end_rod anchor that settles it).
	 * Collision stays empty ({@code noCollission()}, the upstream :177 null).
	 */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		GT6SurfaceVariants.Stick tVariant = aPos == null ? GT6SurfaceVariants.Stick.CENTERED_X
				: GT6SurfaceVariants.pick(TABLE, aState, aPos);
		return GT6SurfaceVariants.shapeOf(tVariant, aState.getValue(FACING));
	}

	/** The cached variant table ({@link Enum#values} clones per call; getShape is a raytrace-hot path). */
	private static final GT6SurfaceVariants.Stick[] TABLE = GT6SurfaceVariants.Stick.values();

	public GT6SurfaceStickBlock(Properties aProperties) {
		super(aProperties.sound(SoundType.WOOD).mapColor(MapColor.WOOD), null);
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
