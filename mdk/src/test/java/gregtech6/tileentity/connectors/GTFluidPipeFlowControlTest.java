package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel;

/**
 * The flow-control offline tests (task p4-pipe-flow-control acceptance ①): the ioMask
 * NBT round-trip, the isOutputFace/externalPushAllowed gate truth tables (ioMask bit x
 * connected; task p5-pipe-flow-semantics adds the mask==0 GT6-default row), the toggle
 * entries, the unmarked-external-face distribute skip, and the onPlaced support/
 * back-connect semantics on a stub level.
 *
 * <p>Offline stub adaptation of the onPlaced table (the card's "supported by an adjacent
 * fluid tank" row): a
 * live fluid-handler neighbour needs {@code ForgeCapabilities.FLUID_HANDLER}, which does
 * not class-initialise offline (the multiblock card finding) — the support-side
 * semantics (OPOS flip + exactly one side connected + the rest untouched) are asserted
 * against a PIPE neighbour instead (same connect path through the connector type
 * intersection); the fluid-handler acceptance runs live via /gt6pipe place (RCON).
 */
public class GTFluidPipeFlowControlTest {

	static BlockEntityType<GTFluidPipeBlockEntity> sType;
	static final BlockPos POS_A = new BlockPos(2, 3, 4);

	@BeforeAll
	static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		@SuppressWarnings("unchecked")
		BlockEntityType<GTFluidPipeBlockEntity>[] tHolder = (BlockEntityType<GTFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTFluidPipeBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];
	}

	private static GTFluidPipeBlockEntity place(MultiBlockLevel aLevel, BlockPos aPos) {
		GTFluidPipeBlockEntity tPipe = sType.create(aPos, Blocks.STONE.defaultBlockState());
		tPipe.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.STONE.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tPipe);
		return tPipe;
	}

	// ---------------------------------------------------------------------------
	// ioMask NBT round-trip (saveWithoutMetadata seam)
	// ---------------------------------------------------------------------------

	@Test
	public void ioMaskRoundTripsThroughNbt() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		tPipe.mIoMask = 0;
		assertTrue(tPipe.saveWithoutMetadata().contains("ioMask", Tag.TAG_ANY_NUMERIC),
				"mask 0 still writes the plain key — byte form, no sparsity special case");

		tPipe.toggleOutput((byte)2);
		tPipe.toggleOutput((byte)5);
		assertEquals((byte)(4 | 32), tPipe.getIoMask(), "two independent arrows set");

		CompoundTag tSaved = tPipe.saveWithoutMetadata();
		assertEquals((byte)(4 | 32), tSaved.getByte("ioMask"), "plain key ioMask, byte bit0-5");

		GTFluidPipeBlockEntity tBack = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals((byte)(4 | 32), tBack.getIoMask(), "bit0-5 fidelity across the round trip");
		assertFalse(tBack.isOutputFace((byte)2), "mask alone does not make an output face — the connection gate is separate");
	}

	@Test
	public void ioMaskClampsToSixBitsOnLoad() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		CompoundTag tSaved = tPipe.saveWithoutMetadata();
		tSaved.putByte("ioMask", (byte)127); // bit6+ garbage
		tPipe.load(tSaved);
		assertEquals(63, tPipe.getIoMask(), "the mConnections form clamp on read");
	}

	// ---------------------------------------------------------------------------
	// isOutputFace gate truth table (ioMask bit x connected)
	// ---------------------------------------------------------------------------

	@Test
	public void isOutputFaceTruthTable() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());

		// neither bit nor connection
		assertFalse(tPipe.isOutputFace((byte)0), "unmarked + unconnected");
		// bit only
		tPipe.mIoMask = TileEntityBase09Connector.SBIT[0];
		assertFalse(tPipe.isOutputFace((byte)0), "marked but unconnected — the arrow gates at runtime");
		// connection only
		tPipe.mIoMask = 0;
		tPipe.mConnections = TileEntityBase09Connector.SBIT[0];
		assertFalse(tPipe.isOutputFace((byte)0), "connected but unmarked");
		// both
		tPipe.mIoMask = TileEntityBase09Connector.SBIT[0];
		assertTrue(tPipe.isOutputFace((byte)0), "marked AND connected");
		// other faces stay closed
		assertFalse(tPipe.isOutputFace((byte)1), "another face with neither bit nor connection");
		// out-of-range sides are false, not an array fault
		assertFalse(tPipe.isOutputFace((byte)6));
		assertFalse(tPipe.isOutputFace((byte)-1));
	}

	@Test
	public void externalPushAllowedTruthTable() {
		// task p5-pipe-flow-semantics acceptance ① — the corrected distribute gate (the p4
		// hard gate left a mask==0 pipe pushing nowhere, off the GT6 default)
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());

		// mask == 0 → the GT6 default: every valid face pushes, connected or not (the
		// connection gate itself stays with canEmitFluidsTo, the distribute loop :380)
		tPipe.mIoMask = 0;
		tPipe.mConnections = 0;
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertTrue(tPipe.externalPushAllowed(tSide), "mask == 0 → GT6 default all-faces push (side " + tSide + ")");
		}

		// mask != 0 → the arrow bit AND the connection decide
		tPipe.mConnections = TileEntityBase09Connector.SBIT[0];
		tPipe.mIoMask = TileEntityBase09Connector.SBIT[0];
		assertTrue(tPipe.externalPushAllowed((byte)0), "marked AND connected");
		assertFalse(tPipe.externalPushAllowed((byte)1), "unmarked face under a non-zero mask");
		tPipe.mIoMask = TileEntityBase09Connector.SBIT[1];
		assertFalse(tPipe.externalPushAllowed((byte)0), "connected but the arrow sits on another face under a non-zero mask");
		tPipe.mIoMask = TileEntityBase09Connector.SBIT[0];
		tPipe.mConnections = 0;
		assertFalse(tPipe.externalPushAllowed((byte)0), "marked but unconnected — the arrow gates at runtime");
		// out-of-range sides carry no bit under a non-zero mask (isOutputFace bounds check)
		assertFalse(tPipe.externalPushAllowed((byte)6));
		assertFalse(tPipe.externalPushAllowed((byte)-1));
	}

	@Test
	public void distributeSkipsUnmarkedExternalFacesUnderANonZeroMask() {
		// the offline-safe half of the spec ① gate: with a NON-ZERO mask, an external
		// (non-pipe) BE neighbour without the arrow never reaches the capability probe —
		// the round must be a silent no-op (no push, no crash). The mask==0 probe half (the
		// GT6 default all-faces push) executes the live ForgeCapabilities lookup, which
		// cannot class-init offline (MultiBlockPartBlockEntityTest:91) — that half is RCON
		// territory (p5-pipe-flow-semantics acceptance ②/③).
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);
		BlockPos tPosSign = POS_A.relative(Direction.EAST);
		tLevel.mStates.put(tPosSign, Blocks.OAK_SIGN.defaultBlockState());
		tLevel.mBlockEntities.put(tPosSign, new SignBlockEntity(tPosSign, Blocks.OAK_SIGN.defaultBlockState()));

		tPipe.mConnections = 63; // connected everywhere
		tPipe.mTanks[0].fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		tPipe.mIoMask = TileEntityBase09Connector.SBIT[1]; // a non-zero mask, arrow NOT on the EAST sign face (5)

		// six passes cover every phase offset ((timer + offset) % 5 == 0 fires once)
		for (int i = 0; i < 6; i++) tPipe.updateEntity();

		assertEquals(0, tPipe.mTransferredAmount, "no output arrow on the external face → no external push");
		assertEquals(100, tPipe.mTanks[0].amount(), "the fluid stays in the pipe");
	}

	// ---------------------------------------------------------------------------
	// toggle entries (the use-path and command share them)
	// ---------------------------------------------------------------------------

	@Test
	public void toggleOutputIsAnXorPerSide() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		assertTrue(tPipe.toggleOutput((byte)3));
		assertEquals(TileEntityBase09Connector.SBIT[3], tPipe.getIoMask());
		assertTrue(tPipe.toggleOutput((byte)3));
		assertEquals(0, tPipe.getIoMask(), "the flip is an XOR — marked → unmarked");
		// other bits untouched, several faces can be marked at once
		tPipe.toggleOutput((byte)0);
		tPipe.toggleOutput((byte)5);
		tPipe.toggleOutput((byte)0);
		assertEquals(TileEntityBase09Connector.SBIT[5], tPipe.getIoMask());
		// invalid sides are rejected without touching the mask
		assertFalse(tPipe.toggleOutput((byte)6));
		assertFalse(tPipe.toggleOutput((byte)-1));
		assertEquals(TileEntityBase09Connector.SBIT[5], tPipe.getIoMask());
		// clear drops everything
		tPipe.clearOutputs();
		assertEquals(0, tPipe.getIoMask());
		tPipe.clearOutputs(); // idempotent
		assertEquals(0, tPipe.getIoMask());
	}

	@Test
	public void toggleConnectionFlipsPerSideThroughTheOpenEnd() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);

		// connect into air (side 1 = UP, the stub above is air) — the open end always yields
		assertTrue(tPipe.toggleConnection((byte)1), "open-end connect into air (upstream :141)");
		assertTrue(tPipe.connected((byte)1));
		// the flip disconnects again
		assertTrue(tPipe.toggleConnection((byte)1), "connected → disconnect returns true");
		assertFalse(tPipe.connected((byte)1));
		// invalid sides
		assertFalse(tPipe.toggleConnection((byte)6));
		assertFalse(tPipe.toggleConnection((byte)-1));
	}

	// ---------------------------------------------------------------------------
	// onPlaced (upstream TileEntityBase09Connector :82-96)
	// ---------------------------------------------------------------------------

	@Test
	public void onPlacedConnectsExactlyTheSupportSide() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// the "support": a neighbouring pipe at POS_A.relative(WEST) (already part of a net)
		GTFluidPipeBlockEntity tSupport = place(tLevel, POS_A.relative(Direction.WEST));
		tSupport.mConnections = TileEntityBase09Connector.SBIT[1]; // some pre-existing connection, untouched by the placement

		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);
		// clickedFace = EAST (5): the support sits at POS_A.relative(OPOS[5]=4=WEST)
		tPipe.onPlaced((byte)5);

		assertTrue(tPipe.connected((byte)4), "the OPOS-flipped support side (WEST) is connected");
		assertEquals(TileEntityBase09Connector.SBIT[4], tPipe.getConnections(),
				"exactly one side connected — the placement never fans out");
		// the symmetric handshake reached the support through the connector types
		assertTrue(tSupport.connected((byte)5), "the support carries the reciprocal bit");
		// the support's own pre-existing connection survived
		assertTrue(tSupport.connected((byte)1), "placement does not disturb neighbours");
	}

	@Test
	public void onPlacedAgainstABlankBlockConnectsNothing() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// a plain stone support (no BE, not air, not liquid) at the west side
		tLevel.mStates.put(POS_A.relative(Direction.WEST), Blocks.STONE.defaultBlockState());

		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);
		tPipe.onPlaced((byte)5); // clickedFace EAST → support side WEST → connect fails on the stone

		assertEquals(0, tPipe.getConnections(), "a support that is neither pipe nor handler nor air/liquid fails the connect — all six sides stay 0");
	}

	@Test
	public void onPlacedBackConnectsNeighboursAlreadyPointingHere() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// neighbour pipe B west of A, pre-connected towards A's spot (its EAST side, 5)
		GTFluidPipeBlockEntity tPipeB = place(tLevel, POS_A.relative(Direction.WEST));
		assertTrue(tPipeB.connect((byte)5, true), "B opens an end toward A's (still empty) spot");
		assertTrue(tPipeB.connected((byte)5));

		GTFluidPipeBlockEntity tPipeA = place(tLevel, POS_A);
		// support side = DOWN into air (clickedFace UP=1 → OPOS[1]=0 → connect DOWN into air)
		tPipeA.onPlaced((byte)1);

		assertTrue(tPipeA.connected((byte)0), "the support-side connect into the air open end");
		assertTrue(tPipeA.connected((byte)4), "the back-connect loop picked up B already pointing here (upstream :88-93)");
		assertTrue(tPipeB.connected((byte)5), "B keeps its reciprocal bit");
		// the support side stays too — the two connects are independent
		assertEquals((byte)(TileEntityBase09Connector.SBIT[0] | TileEntityBase09Connector.SBIT[4]), tPipeA.getConnections());
	}

	@Test
	public void onPlacedIgnoresInvalidClickedFaces() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);
		tPipe.onPlaced((byte)6);
		tPipe.onPlaced((byte)-1);
		assertEquals(0, tPipe.getConnections(), "invalid clicked faces are a no-op");
		// the OPOS index table stays upstream verbatim
		assertEquals(4, gregtech6.util.UT6.OPOS[5], "OPOS[5]=4 — the EAST clicked face touches the WEST pipe side");
	}
}
