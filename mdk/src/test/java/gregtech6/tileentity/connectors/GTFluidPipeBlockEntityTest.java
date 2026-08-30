package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTFluidPipeBlockEntity offline tests (task p4-fluid-pipes spec ③): NBT persistence,
 * the fillable/drainable side gates, the anti-backflow record surface and the side-wrapped
 * handler (spec ⑤). The world-level distribute equalisation (pressure difference, divup
 * split, pipe-before-tank, leftover push) needs live neighbours — the card prescribes it
 * as the RCON command acceptance (two pipes, one fill, split + backflow-bit assertions).
 */
public class GTFluidPipeBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTFluidPipeBlockEntity> sType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTFluidPipeBlockEntity>[] tHolder = (BlockEntityType<GTFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTFluidPipeBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];
	}

	private static GTFluidPipeBlockEntity connectedPipe() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tPipe.mConnections = 63; // all sides pass the canEmit/canAccept gates
		return tPipe;
	}

	@Test
	public void defaultCapacityFallsBackForVanillaBlocks() {
		// the block-tier capacity read needs a GTFluidPipeBlock block state (registration
		// supplies 50/300 for the wood tiers); a vanilla block gets the upstream default
		GTFluidPipeBlockEntity tPipe = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(1000, tPipe.mCapacity, "upstream :75 default mCapacity = 1000");
		assertEquals(1, tPipe.mTanks.length, "W1 ships the single-tank wood tiers");
		assertEquals(1000, tPipe.mTanks[0].capacity());
		assertEquals(0, tPipe.mTanks[0].mIndex);
	}

	@Test
	public void nbtRoundTripCarriesTanksMasksAndTransferred() {
		GTFluidPipeBlockEntity tPipe = connectedPipe();
		tPipe.mTanks[0].fill(new FluidStack(Fluids.WATER, 150), FluidAction.EXECUTE);
		tPipe.onFilledFrom((byte)2, tPipe.mTanks[0]);
		tPipe.mTransferredAmount = 90;

		CompoundTag tSaved = tPipe.saveWithoutMetadata();
		assertTrue(tSaved.contains("tank.0", Tag.TAG_COMPOUND));
		assertEquals(TileEntityBase09Connector.SBIT[2], tSaved.getByte("last.0"), "side 2 recorded as its SBIT value");

		GTFluidPipeBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(150, tBack.mTanks[0].amount());
		assertEquals(TileEntityBase09Connector.SBIT[2], tBack.mLastReceivedFrom[0], "upstream :121 — the source mask persists");
		assertEquals(90, tBack.mTransferredAmount);
		assertEquals("fluid_pipe", tSaved.getString("te_name"));
	}

	@Test
	public void fillableTankLookupHonoursSideGatesAndFluidIdentity() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS, Blocks.STONE.defaultBlockState());
		// unconnected sides reject both fill and drain lookups (upstream :464/:472)
		assertNull(tPipe.getFluidTankFillable((byte)0, new FluidStack(Fluids.WATER, 10)));
		assertNull(tPipe.getFluidTankDrainable((byte)0, new FluidStack(Fluids.WATER, 10)));

		tPipe.mConnections = 63;
		assertNotNull(tPipe.getFluidTankFillable((byte)0, new FluidStack(Fluids.WATER, 10)),
				"connected empty tank is fillable (upstream :466)");
		tPipe.mTanks[0].fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		assertSame(tPipe.mTanks[0], tPipe.getFluidTankFillable((byte)0, new FluidStack(Fluids.WATER, 10)),
				"the containing tank wins the lookup (upstream :465)");
		assertNull(tPipe.getFluidTankFillable((byte)0, new FluidStack(Fluids.LAVA, 10)),
				"single-fluid rule through the pipe surface (:465/:466 both miss for lava)");
		assertNotNull(tPipe.getFluidTankDrainable((byte)1, new FluidStack(Fluids.WATER, 10)));
		assertNull(tPipe.getFluidTankDrainable((byte)1, new FluidStack(Fluids.LAVA, 10)));
	}

	@Test
	public void sideHandlerRecordsBackflowBitsOnExecutedFills() {
		GTFluidPipeBlockEntity tPipe = connectedPipe();
		SideFluidHandler tHandler = new SideFluidHandler(tPipe, (byte)3);

		assertEquals(1, tHandler.getTanks());
		assertEquals(1000, tHandler.getTankCapacity(0));
		assertTrue(tHandler.isFluidValid(0, new FluidStack(Fluids.WATER, 1)));

		assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE));
		assertEquals(100, tPipe.mTanks[0].amount());
		assertEquals(TileEntityBase09Connector.SBIT[3], tPipe.mLastReceivedFrom[0] & TileEntityBase09Connector.SBIT[3],
				"upstream :487 — mLastReceivedFrom |= SBIT[side] on executed fill");

		// simulate leaves the mask alone
		tHandler.fill(new FluidStack(Fluids.WATER, 50), FluidAction.SIMULATE);
		assertEquals(TileEntityBase09Connector.SBIT[3], tPipe.mLastReceivedFrom[0] & TileEntityBase09Connector.SBIT[3]);

		FluidStack tDrained = tHandler.drain(60, FluidAction.EXECUTE);
		assertEquals(60, tDrained.getAmount());
		assertEquals(40, tPipe.mTanks[0].amount());
	}

	@Test
	public void unconnectedSidesRejectHandlerTraffic() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS, Blocks.STONE.defaultBlockState()); // mConnections = 0
		SideFluidHandler tHandler = new SideFluidHandler(tPipe, (byte)0);
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"canAcceptFluidsFrom gate (upstream :464)");
		assertEquals(FluidStack.EMPTY, tHandler.drain(100, FluidAction.EXECUTE),
				"canEmitFluidsTo gate (upstream :472)");
	}

	@Test
	public void fillRejectionTableForMarkedOutputFaces() {
		// task p5-pipe-flow-semantics acceptance ② — the SideFluidHandler.fill static reject:
		// an arrow-marked face is a pump outlet and refuses the external back-fill; the gate
		// lives in the wrapper (never in canAcceptFluidsFrom/getFluidTankFillable, which the
		// pipe-to-pipe equalisation receiver shares) and precedes onFilledFrom
		FluidStack tWater = new FluidStack(Fluids.WATER, 100);

		// mask == 0 → every side accepts (the GT6 default; the reject is arrow-scoped)
		GTFluidPipeBlockEntity tMaskless = connectedPipe();
		assertEquals(100, new SideFluidHandler(tMaskless, (byte)2).fill(tWater, FluidAction.EXECUTE),
				"mask == 0 accepts the fill on any side");

		// mask != 0 → the arrow face statically refuses, action-independently
		GTFluidPipeBlockEntity tPipe = connectedPipe();
		tPipe.toggleOutput((byte)2);
		SideFluidHandler tMarked = new SideFluidHandler(tPipe, (byte)2);
		assertEquals(0, tMarked.fill(tWater, FluidAction.EXECUTE), "arrow-marked face refuses the external fill");
		assertEquals(0, tMarked.fill(tWater, FluidAction.SIMULATE), "the reject is action-independent");
		assertEquals(0, tPipe.mTanks[0].amount(), "nothing entered the tank");
		assertEquals(0, tPipe.mLastReceivedFrom[0],
				"a rejected fill records no mLastReceivedFrom bit — the static reject precedes onFilledFrom");

		// unmarked faces keep accepting under the same non-zero mask and still record their bit
		assertEquals(100, new SideFluidHandler(tPipe, (byte)3).fill(tWater, FluidAction.EXECUTE),
				"an unmarked face accepts under the same non-zero mask");
		assertEquals(TileEntityBase09Connector.SBIT[3], tPipe.mLastReceivedFrom[0] & TileEntityBase09Connector.SBIT[3],
				"accepted fills still record their source side (upstream :487)");
		assertEquals(0, tPipe.mLastReceivedFrom[0] & TileEntityBase09Connector.SBIT[2], "the marked face's bit stays clear");

		// the side-less handler (-1) is unaffected by the mask (isOutputFace bounds-checks)
		assertEquals(100, new SideFluidHandler(tPipe, (byte)-1).fill(tWater, FluidAction.EXECUTE),
				"mSide == -1 bypasses the arrow reject");

		// unconnected faces still reject through the untouched canAcceptFluidsFrom gate
		GTFluidPipeBlockEntity tHalfOpen = connectedPipe();
		tHalfOpen.toggleOutput((byte)2);
		tHalfOpen.disconnect((byte)4, true);
		assertEquals(0, new SideFluidHandler(tHalfOpen, (byte)4).fill(tWater, FluidAction.EXECUTE),
				"an unconnected face still rejects via canAcceptFluidsFrom — the original gate is untouched");
	}

	@Test
	public void divupIsCeilingDivision() {
		assertEquals(50, GTFluidPipeBlockEntity.divup(100, 2));
		assertEquals(34, GTFluidPipeBlockEntity.divup(100, 3), "upstream UT.java:1697");
		assertEquals(1, GTFluidPipeBlockEntity.divup(1, 2));
		assertEquals(0, GTFluidPipeBlockEntity.divup(0, 4));
	}
}
