package gregtech6.tileentity.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.registry.GT6LongDistPipes;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6LongDistanceFluidPipeBlockEntity offline tests (task p35-long-distance-pipes): the
 * fill-only window over the temperature gate (the :200/:216
 * {@code FL.temperature <= mTemperature} arm — the offline rating is set directly, the
 * water 300 K vs a sub-300 rating is the refusal arm), the drain refusal (:206-225),
 * the tank-query forwarding (:227-233), the no-link / stopped refusals (:119/:133) and
 * the NBT face (:65-83 — the rating rides the throughput key, the upstream :80 debug
 * write kept).
 *
 * <p>Offline harness (the item-endpoint test form): the {@code setTargetOverride} seam
 * replaces the wire BFS (no level), the {@code setRemoteOverride} seam injects the
 * remote tank behind the target's BACK (no level query).
 */
public class GT6LongDistanceFluidPipeBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6LongDistanceFluidPipeBlockEntity> sType;
	static final BlockPos POS = new BlockPos(10, 4, 5);
	static final BlockPos FAR = new BlockPos(110, 4, 5);

	private static GT6LongDistanceFluidPipeBlockEntity endpoint() {
		return new GT6LongDistanceFluidPipeBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/** The recording remote tank (the drum behind the target's BACK). */
	public static class StubTank implements IFluidHandler {
		FluidStack held = FluidStack.EMPTY;
		int fills = 0;

		@Override
		public int getTanks() {return 1;}

		@Override
		public FluidStack getFluidInTank(int aTank) {return held;}

		@Override
		public int getTankCapacity(int aTank) {return 64000;}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}

		@Override
		public int fill(FluidStack aStack, FluidAction aAction) {
			if (!held.isEmpty()) return 0;
			fills++;
			if (aAction.execute()) held = aStack.copy();
			return aStack.getAmount();
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;}
	}

	/** The rig: sender filling through the target, the stub tank behind the target's BACK. */
	private static StubTank wire(GT6LongDistanceFluidPipeBlockEntity aSender, GT6LongDistanceFluidPipeBlockEntity aTarget) {
		StubTank tRemote = new StubTank();
		aSender.setTargetOverride(aTarget);
		aSender.setRemoteOverride(tRemote);
		return tRemote;
	}

	@BeforeAll
	static void buildOfflineFixture() {
		BlockEntityType<GT6LongDistanceFluidPipeBlockEntity>[] tHolder = (BlockEntityType<GT6LongDistanceFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6LongDistanceFluidPipeBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	@Test
	public void waterTemperatureReadsTheAttributes() {
		assertEquals(300, GT6LongDistanceFluidPipeBlockEntity.temperatureOf(new FluidStack(Fluids.WATER, 1000)),
				"the vanilla water attributes = the 300 K default band");
		assertEquals(300, GT6LongDistanceFluidPipeBlockEntity.temperatureOf(FluidStack.EMPTY),
				"the empty stack = the FL.DEF_ENV_TEMP default (:798)");
	}

	@Test
	public void fillForwardsThroughTheWindowWhenWithinTheRating() {
		GT6LongDistanceFluidPipeBlockEntity tSender = endpoint();
		GT6LongDistanceFluidPipeBlockEntity tTarget = new GT6LongDistanceFluidPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		StubTank tRemote = wire(tSender, tTarget);
		tSender.mTemperature = 1943; // the SS wire blob rating (the :148 scan column, set directly offline)

		assertEquals(1000, tSender.window().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				"300 K water inside the 1943 K rating — the whole stack lands remotely");
		assertEquals(1, tRemote.fills);
		assertEquals(1000, tRemote.held.getAmount(), "the remote drum holds it");
		assertEquals(1, tSender.window().getTanks(), "the tank query forwards (:227-233)");
		assertEquals(1000, tSender.window().getFluidInTank(0).getAmount());
		assertEquals(64000, tSender.window().getTankCapacity(0));
		assertTrue(tSender.window().isFluidValid(0, new FluidStack(Fluids.WATER, 1000)), "canFill through the gate (:215-221)");
	}

	@Test
	public void hotterThanTheRatingRefusesTheFill() {
		GT6LongDistanceFluidPipeBlockEntity tSender = endpoint();
		GT6LongDistanceFluidPipeBlockEntity tTarget = new GT6LongDistanceFluidPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		StubTank tRemote = wire(tSender, tTarget);
		tSender.mTemperature = 299; // the rating set BELOW the water's 300 K — the offline refusal arm

		assertEquals(0, tSender.window().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				":200 — a fluid over the rating refuses");
		assertEquals(0, tRemote.fills, "the remote tank is untouched");
	}

	@Test
	public void drainIsRefusedAlways() {
		GT6LongDistanceFluidPipeBlockEntity tSender = endpoint();
		GT6LongDistanceFluidPipeBlockEntity tTarget = new GT6LongDistanceFluidPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		StubTank tRemote = wire(tSender, tTarget);
		tSender.mTemperature = 1943;
		tSender.window().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

		assertTrue(tSender.window().drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
				":211-213 — fill-only, the max-drain arm returns EMPTY");
		assertTrue(tSender.window().drain(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE).isEmpty(),
				":207-209 — the resource arm returns EMPTY");
		assertEquals(1000, tRemote.held.getAmount(), "the remote content stays");
	}

	@Test
	public void noLinkRefusesTheFill() {
		GT6LongDistanceFluidPipeBlockEntity tSender = endpoint(); // no override: the offline scan self-seeds
		assertEquals(0, tSender.window().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				":133 — the self-seed is no target");
		assertFalse(tSender.checkTarget());
		assertEquals(0, tSender.mTemperature, "the scan re-seeds the rating (:144)");
	}

	@Test
	public void stoppedEndpointRefusesTheFill() {
		GT6LongDistanceFluidPipeBlockEntity tSender = endpoint();
		GT6LongDistanceFluidPipeBlockEntity tTarget = new GT6LongDistanceFluidPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		wire(tSender, tTarget);
		tSender.mStopped = true; // the soft-hammer channel (:119)
		assertEquals(0, tSender.window().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE), ":119 — the stopped guard");
	}

	@Test
	public void nbtCarriesThePositionAndRatingButNotTheLinkPair() {
		GT6LongDistanceFluidPipeBlockEntity tSender = endpoint();
		GT6LongDistanceFluidPipeBlockEntity tTarget = new GT6LongDistanceFluidPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		wire(tSender, tTarget);
		assertTrue(tSender.checkTarget());
		tSender.mTemperature = 1943;

		CompoundTag tNBT = tSender.saveWithoutMetadata();
		GT6LongDistanceFluidPipeBlockEntity tRevived = endpoint();
		tRevived.load(tNBT);

		assertEquals(FAR, tRevived.mTargetPos, "the persisted position");
		assertNull(tRevived.mTarget, "the link pair is transient (:61)");
		assertNull(tRevived.mSender);
		assertEquals(1943, tRevived.mTemperature, "the rating rides the throughput key (the :80 debug face)");
		assertTrue(tNBT.getBoolean(GT6LongDistanceFluidPipeBlockEntity.NBT_TARGET), "the live-link flag rides the NBT");
		assertEquals(GT6LongDistPipes.pathOf(0), "long_dist_pipe_0", "the pathOf form");
	}
}
