package gregtech6.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.registry.GTWireSpecs;

/**
 * The Long Distance Electric Wire block (task p35-energy-tail-machines) — the 1.20.1
 * counterpart of {@code BlockLongDistWire} (63 lines, BlockBaseMachineUpdate): a PLAIN
 * block (no BE — upstream stores only {@code mTiers[aMeta]}), one block per upstream
 * meta 0..15, the tier byte the row's only data column. The scan face of the Long
 * Distance Transformer: the BFS matches the block INSTANCE, so each meta is its own
 * registered block (upstream matched block+meta; the port keeps the same wire-cable
 * split by registering 16 blocks over one class).
 *
 * <p>Upstream columns (Loader_Blocks.java:160 registration + BlockLongDistWire):
 * tier bytes {4,4,5,6,6,6,6,6,7,7,7,7,8,8,8,8}, throughput {@code VMAX[tier]} with
 * UNLIMITED amperage and {@code 0.125} EU/m loss (the loss lives in the transformer's
 * doInject, {@code aSize - max(64, mDistance/8)}), hardness = the iron block's,
 * resistance 15, cloth sounds, flammable 150/150, harvest = cutter level 3 (the cutter
 * tool gate is the tool-tag pool; the block drops itself).
 *
 * <p>NOTE for the follow-up p35-long-distance-pipes card: this is the ELECTRIC wire
 * family (the transformer scan object). The ITEM/FLUID pipes are the separate upstream
 * block {@code BlocksGT.LongDistPipe01} (BlockLongDistPipe, Loader_Blocks.java:177) —
 * a different class, registered by the pipes card, NOT scanned by this transformer.
 */
public class GT6LongDistWireBlock extends Block {

	/** The upstream visual: a thin post (BlockLongDistWire bounds fold — the PX_P[6..10] column box); full-cube collision is the render-pool alternative. */
	public static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

	/** The wire's voltage-tier byte (the VMAX[tier] throughput seat; the VN[tier] display word). */
	private final byte mTier;

	public GT6LongDistWireBlock(int aTier) {
		super(Properties.of().strength(3.0F, 15.0F).sound(SoundType.WOOL));
		mTier = (byte) aTier;
	}

	/** The voltage-tier byte (the row's only data column). */
	public byte tier() {
		return mTier;
	}

	/** The throughput = VMAX[tier] EU (the UNLIMITED-amperage packet size cap). */
	public long throughput() {
		return GTWireSpecs.VMAX[mTier];
	}

	@Override
	public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, net.minecraft.core.Direction aDirection) {
		return 150; // BlockLongDistWire :54
	}

	@Override
	public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, net.minecraft.core.Direction aDirection) {
		return 150; // :55
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}
}
