package gregtech6.block.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The GT6 PATH block — {@code gt.block.paths} (upstream BlockPath, Loader_Blocks.java:83,
 * task material-mc-g2-decor-misc). The upstream 16-meta soil ladder folds to ONE block:
 * meta 0 = the vanilla-dirt path is the only member with a home in the single-mod port —
 * metas 1-11 (Aether/BoP/EB foreign dirts, the :145-155 IL rows) are foreign-identity TRUE
 * NEGATIVES and metas 12-15 are byte-equal generic "Path" rows of meta 0 (identical drops
 * and icons in the port universe) — the T5b/bale-pool declaration shape.
 *
 * <p>The behaviour face, upstream verbatim: the 15px flat height (BlockPath.java:137
 * PIXELS_NEG[1] — the vanilla dirt-path height), the walk-speed x1.1 multiplication
 * (:188-192, the ice-below arm needs the stacked-path face = the isHalfBlock arm that
 * rides the slab-sweep defer), {@code canCreatureSpawn = F} (:225 — the 1.20.1
 * isValidSpawn false), {@code canSilkHarvest = F} (:226), the drops = vanilla dirt
 * (:143-158, fortune ignored — the GT6GrassBlock loot reading), shovel tool level 0
 * (:229-230), hardness = vanilla grass x2 (:231) = 1.2, resistance = grass x1.5 (:232)
 * = 0.9, flammable like dirt (no arm). The light-opacity WATER face (:228) is the
 * 1.7.10 sealable plumbing the port does not carry.
 */
public class GT6PathBlock extends Block {

	/** The display name ({@code gt.block.paths.0} = "Path"/草径, the dump face). */
	public static final String BLOCK_NAME_KEY = "gt6.path.block";

	/** The 15px flat height (upstream PIXELS_NEG[1], BlockPath.java:137). */
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

	public GT6PathBlock(BlockBehaviour.Properties aProperties) {
		super(aProperties);
	}

	/** The upstream numbers (BlockPath.java:231-232): grass hardness x2, resistance x1.5;
	 * the spawn gate rides the predicate (the :225 canCreatureSpawn = F face). */
	public static BlockBehaviour.Properties decorProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.2F, 0.9F)
				.sound(SoundType.GRASS)
				.isValidSpawn((aState, aLevel, aPos, aType) -> false);
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	/** Upstream canCreatureSpawn = F (:225) — the Properties predicate route (the 1.20.1/21.1
	 * common 4-arg lambda shape; the 21.1 BlockBehaviour carries no overridable method). */
	//? if forge {
	@Override
	public boolean isPathfindable(BlockState aState, BlockGetter aLevel, BlockPos aPos, PathComputationType aType) {
	//?} else {
	/*// 21.1 dropped the BlockGetter argument (the PathfindingContext refactor)
	public boolean isPathfindable(BlockState aState, PathComputationType aType) {
	 *///?}
		return false;
	}

	/** The upstream walk face (BlockPath.java:188-192): the x1.1 boost (x1.05 stacked arm deferred). */
	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		if ((aEntity.getDeltaMovement().x != 0 || aEntity.getDeltaMovement().z != 0)
				&& !aEntity.isInWater() && !aEntity.isShiftKeyDown()) {
			aEntity.setDeltaMovement(aEntity.getDeltaMovement().x * 1.1, aEntity.getDeltaMovement().y,
					aEntity.getDeltaMovement().z * 1.1);
		}
		super.stepOn(aLevel, aPos, aState, aEntity);
	}

	/** The upstream drops face rides the loot table (dirt, no silk arm — the grass-loot reading). */
	@Override
	public boolean canSurvive(BlockState aState, LevelReader aLevel, BlockPos aPos) {
		return canSupportRigidBlock(aLevel, aPos.below());
	}
}
