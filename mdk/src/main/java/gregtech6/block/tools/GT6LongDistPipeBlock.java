package gregtech6.block.tools;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.tileentity.inventories.GT6LongDistanceItemPipeBlockEntity;
import gregtech6.tileentity.tank.GT6LongDistanceFluidPipeBlockEntity;

/**
 * The Long Distance Pipe block (task p35-long-distance-pipes) — the 1.20.1 counterpart
 * of {@code BlockLongDistPipe} (49 lines, BlockBaseMachineUpdate): a PLAIN block (no BE
 * — upstream stores only {@code mTemperatures[aMeta]}), one block per upstream meta
 * 0..15 over one class (the GT6LongDistWireBlock family form), the temperature rating
 * the row's only data column. Meta 0 is the ITEM pipeline ({@code -1} K — the
 * {@code >= 0 → return} arm of the item scan, MultiTileEntityLongDistancePipelineItem
 * :141), metas 1..4 the FLUID pipelines (the material melting points, Loader_Blocks
 * :179), metas 5..15 the dead {@code 0} rows (both families refuse them).
 *
 * <p>The passive scan medium of the two pipeline endpoints: the BFS matches the block
 * INSTANCE (the upstream block+meta match split over 16 registered blocks — the
 * GT6LongDistWireBlock precedent). Hardness = the iron block's 3.0, resistance 20
 * (the :45-:48 columns), metal sounds (the soundTypeMetal :36).
 *
 * <p>The machine-block-update invalidation (BlockBaseMachineUpdate :46-47 +
 * ITileEntityMachineBlockUpdateable.Util.stepToUpdateMachine :126-143): place/remove
 * floods the wire network (same-instance blocks conduct) and resets every pipeline
 * endpoint it touches, so the endpoints re-scan on the next lazy access. The flood is
 * chunk-load-guarded (the p35 cross-dim POC R1 ruling — no synchronous chunk loads,
 * the {@code mIgnoreUnloadedChunks} arm collapses to a halt).
 */
public class GT6LongDistPipeBlock extends Block {

	/** The upstream visual: a thin post (the LONG_DIST_PIPES_01 iconset is the render pool; the wire-family column box). */
	public static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

	/** The temperature rating in K (meta 0 = -1 the item family, metas 1..4 the fluid ratings, metas 5..15 = 0). */
	private final long mTemperatureK;

	public GT6LongDistPipeBlock(long aTemperatureK) {
		super(Properties.of().strength(3.0F, 20.0F).sound(SoundType.METAL));
		mTemperatureK = aTemperatureK;
	}

	/** The row's temperature rating (the scan column; the fluid fill gate reads it through the scan). */
	public long temperatureK() {
		return mTemperatureK;
	}

	@Override
	public VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	// ---------------------------------------------------------------------------
	// the machine-block-update invalidation (BlockBaseMachineUpdate :46-47)
	// ---------------------------------------------------------------------------

	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aIsMoving) {
		super.onPlace(aState, aLevel, aPos, aOldState, aIsMoving);
		floodInvalidate(aLevel, aPos);
	}

	@Override
	public void onRemove(BlockState aState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
		floodInvalidate(aLevel, aPos);
		super.onRemove(aState, aLevel, aPos, aNewState, aIsMoving);
	}

	/** A BFS node: the position plus the walk order (the upstream {@code aSet.size() < 5} count arm). */
	private record FloodNode(BlockPos pos, int order) {}

	/**
	 * The {@code stepToUpdateMachine} walk (:126-143): from the changed block, the wire
	 * blocks (the registered machine blocks) conduct indefinitely, the reached pipeline
	 * endpoints reset and conduct, other blocks conduct only while the walk is young
	 * (the first five visited — the radius that reaches the endpoints sitting just off
	 * the network). Unloaded chunks halt the walk (the POC R1 isLoaded guard — upstream
	 * loaded chunks on demand through the {@code mIgnoreUnloadedChunks = F} scan window;
	 * the port never loads).
	 */
	private void floodInvalidate(Level aLevel, BlockPos aPos) {
		if (aLevel.isClientSide) return;
		Set<BlockPos> tSeen = new HashSet<>();
		Deque<FloodNode> tToCheck = new ArrayDeque<>();
		tSeen.add(aPos);
		tToCheck.add(new FloodNode(aPos, 0));
		while (!tToCheck.isEmpty()) {
			FloodNode tNode = tToCheck.poll();
			if (!aLevel.isLoaded(tNode.pos())) continue; // the POC R1 guard — no synchronous chunk load
			BlockState tState = aLevel.getBlockState(tNode.pos());
			boolean tConducts = tState.getBlock() instanceof GT6LongDistPipeBlock;
			BlockEntity tTileEntity = aLevel.getBlockEntity(tNode.pos());
			if (tTileEntity instanceof GT6LongDistanceItemPipeBlockEntity tItem) {
				tItem.invalidateLink();
				tConducts = true;
			}
			if (tTileEntity instanceof GT6LongDistanceFluidPipeBlockEntity tFluid) {
				tFluid.invalidateLink();
				tConducts = true;
			}
			if (!tConducts && tNode.order() >= 5) continue; // the :131 count arm
			for (Direction tDir : Direction.values()) {
				BlockPos tNext = tNode.pos().relative(tDir);
				if (tSeen.add(tNext)) tToCheck.add(new FloodNode(tNext, tNode.order() + 1));
			}
		}
	}
}
