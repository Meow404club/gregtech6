package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTItemPipes.ItemPipeRow;
import gregtech6.registry.GTItemPipes.ItemPipeVariant;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTItemPipeBlockEntity offline tests (task p26-pipe-item acceptance ②): the stepSize/
 * invSize parameter axis (the variant table MultiTileEntityPipeItem.java:77-82 over the
 * loader bases :1823-1825), the one-way latch truth table (:268), the monkeywrench
 * face-disable cycle (:128-153), the capacity window (:243-251), the scanPipes distance
 * order (ITileEntityItemPipe.Util :77-101 over the adjacency seam) and the NBT round
 * trip. The live container-to-container transfer is the RCON chain's (acceptance ③).
 */
public class GTItemPipeBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTItemPipeBlockEntity> sType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTItemPipeBlockEntity>[] tHolder = (BlockEntityType<GTItemPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTItemPipeBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];
	}

	private static GTItemPipeBlockEntity pipe() {
		GTItemPipeBlockEntity tPipe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tPipe.mConnections = 63; // all sides pass the latch/connect gates
		return tPipe;
	}

	private static ItemStack stone(int aCount) {
		return new ItemStack(Items.STONE, aCount);
	}

	// ---------------------------------------------------------------------------
	// the parameter axis (acceptance ② "stepSize 两列表" / spec ① "32768→64+1→512 两列")
	// ---------------------------------------------------------------------------

	@Test
	public void variantAxisReproducesTheUpstreamTable() {
		// MultiTileEntityPipeItem.java:77-82 — the six variant rows over one material base
		long tBase = 32768; // :1823 — the Brass line's aStepSize
		int tInv = 1;       // :1823 — the Brass line's aInvSize
		long[] tSteps = {tBase, tBase / 2, tBase / 4, tBase * 100, tBase * 50, tBase * 25};
		int[] tInvs = {1, 2, 4, 1, 2, 4};
		ItemPipeVariant[] tVariants = ItemPipeVariant.values();
		for (int i = 0; i < tVariants.length; i++) {
			assertEquals(tSteps[i], tVariants[i].stepSizeOf(tBase), tVariants[i] + " stepSize");
			assertEquals(tInvs[i], tVariants[i].invSizeOf(tInv), tVariants[i] + " invSize");
		}
		// the whole first batch — three materials, all at the 32768/1 base (:1823-1825), 18 rows
		assertEquals(18, GTItemPipes.ROWS.size());
		for (ItemPipeRow tRow : GTItemPipes.ROWS) {
			assertEquals(tRow.variant().stepSizeOf(32768), tRow.stepSize(), tRow.path() + " stepSize");
			assertEquals(tRow.variant().invSizeOf(1), tRow.invSize(), tRow.path() + " invSize");
		}
		// the metaId table: the three addItemPipes bases at the +2..+7 offsets (:77-82)
		int[][] tExpectedIds = {{25002, 25003, 25004, 25005, 25006, 25007},
				{25027, 25028, 25029, 25030, 25031, 25032},
				{25052, 25053, 25054, 25055, 25056, 25057}};
		for (int tMat = 0; tMat < 3; tMat++) {
			for (int tVar = 0; tVar < 6; tVar++) {
				assertEquals(tExpectedIds[tMat][tVar], GTItemPipes.ROWS.get(tMat * 6 + tVar).metaId(),
						GTItemPipes.ROWS.get(tMat * 6 + tVar).path() + " metaId");
			}
		}
		// the id paths (the arch ruling: material first, the wood_fluid_pipe_small order)
		assertEquals("brass_item_pipe_medium", GTItemPipes.ROWS.get(0).path());
		assertEquals("brass_item_pipe_restrictive_huge", GTItemPipes.ROWS.get(5).path());
		assertEquals("constantan_item_pipe_medium", GTItemPipes.ROWS.get(6).path());
		assertEquals("cobalt_brass_item_pipe_medium", GTItemPipes.ROWS.get(12).path());
	}

	@Test
	public void offlineFixtureFallsBackToTheLoaderBase() {
		GTItemPipeBlockEntity tPipe = pipe();
		assertEquals(32768, tPipe.mStepSize, "the vanilla-block fallback = the :1823 base stepSize");
		assertEquals(1, tPipe.mInventory.getSlots(), "the :1823 base invSize");
		assertTrue(tPipe.mBlocking, "upstream :70 — every loader line passes aBlocking = T");
		assertEquals(GTItemPipeBlockEntity.SIDE_UNDEFINED, tPipe.mLastReceivedFrom, ":69 — a fresh pipe never latched");
	}

	// ---------------------------------------------------------------------------
	// the one-way latch (:268)
	// ---------------------------------------------------------------------------

	@Test
	public void latchArmsOnFirstFillAndFreezesWhileHolding() {
		GTItemPipeBlockEntity tPipe = pipe();
		// the empty pipe arms the latch to the inserting side
		assertTrue(tPipe.canInsertItem(0, stone(1), (byte)2), ":268 — an empty pipe accepts the first insert");
		assertEquals((byte)2, tPipe.mLastReceivedFrom, "the latch arms to side 2");
		// while the slot holds, nobody inserts — not even the latched side
		tPipe.mInventory.setStackInSlot(0, stone(64));
		assertFalse(tPipe.canInsertItem(0, stone(1), (byte)2), ":268 — !slotHas(aSlot) blocks the latched side too");
	}

	@Test
	public void latchSideGateHoldsAcrossMultiSlotPipes() {
		// a huge-pipe shape (invSize 4): an empty SLOT on a foreign side is still refused —
		// the side half of the latch is orthogonal to the slot half
		GTItemPipeBlockEntity tWide = pipe();
		tWide.mInventory = new GTItemStackHandler(4, tWide::setChanged);
		assertTrue(tWide.canInsertItem(0, stone(1), (byte)2));
		assertEquals((byte)2, tWide.mLastReceivedFrom);
		tWide.mInventory.setStackInSlot(0, stone(64));
		assertFalse(tWide.canInsertItem(1, stone(1), (byte)3), ":268 — mLastReceivedFrom(2) != side 3 refuses the empty slot");
		// upstream :268 checks !slotHas(aSlot) per slot while the latch freezes the SIDE —
		// so the latched side keeps filling further empty slots
		assertTrue(tWide.canInsertItem(1, stone(1), (byte)2), "the latched side fills further empty slots");
		assertTrue(tWide.canInsertItem(2, stone(1), (byte)2));
		assertFalse(tWide.canInsertItem(1, stone(1), (byte)5), "a second foreign side still refuses");
	}

	@Test
	public void latchGatesUnconnectedAndDisabledFaces() {
		GTItemPipeBlockEntity tPipe = pipe();
		tPipe.mConnections = 0; // nothing connected
		assertFalse(tPipe.canInsertItem(0, stone(1), (byte)2), ":268 — !connected refuses before the latch");
		tPipe.mConnections = 63;
		tPipe.mDisabledInputs |= TileEntityBase09Connector.SBIT[2];
		assertFalse(tPipe.canInsertItem(0, stone(1), (byte)2), ":268 — the input-disabled face refuses");
		assertTrue(tPipe.canInsertItem(0, stone(1), (byte)3), "a connected enabled face arms the latch instead");
		assertEquals((byte)3, tPipe.mLastReceivedFrom);
		// the side-less external query never inserts (connected(-1) is false)
		assertFalse(tPipe.canInsertItem(0, stone(1), (byte)-1));
	}

	@Test
	public void extractGatesFollowUpstream() {
		GTItemPipeBlockEntity tPipe = pipe();
		tPipe.mInventory.setStackInSlot(0, stone(8));
		assertTrue(tPipe.canExtractItem(0, tPipe.mInventory.getStackInSlot(0), (byte)2), ":269 — connected extracts");
		assertTrue(tPipe.canExtractItem(0, tPipe.mInventory.getStackInSlot(0), (byte)-1), ":269 — SIDES_INVALID extracts");
		tPipe.mConnections = 0;
		assertFalse(tPipe.canExtractItem(0, tPipe.mInventory.getStackInSlot(0), (byte)2), ":269 — unconnected refuses");
	}

	@Test
	public void latchReleasesWithTheLastItemOnTheTickGate() {
		GTItemPipeBlockEntity tPipe = pipe();
		tPipe.mInventory.setStackInSlot(0, stone(4));
		tPipe.mLastReceivedFrom = 2;
		tPipe.oLastReceivedFrom = 2;
		// drain the pipe, then cross the 10t round: upstream :218-219 release + echo
		tPipe.mInventory.setStackInSlot(0, ItemStack.EMPTY);
		for (int i = 0; i < 12; i++) tPipe.updateEntity();
		assertEquals(GTItemPipeBlockEntity.SIDE_UNDEFINED, tPipe.mLastReceivedFrom, "upstream :218 — an empty pipe releases the latch");
		assertEquals(GTItemPipeBlockEntity.SIDE_UNDEFINED, tPipe.oLastReceivedFrom, "upstream :219 — the echo follows");
	}

	// ---------------------------------------------------------------------------
	// the monkeywrench cycle (:128-153) — the verbatim arms, the effective truth table
	// ---------------------------------------------------------------------------

	@Test
	public void monkeyWrenchCycleTruthTable() {
		GTItemPipeBlockEntity tPipe = pipe(); // level-less: the two-pipe refusal arm is dormant here (RCON arm)
		int tBit = TileEntityBase09Connector.SBIT[4];
		// the upstream four-arm cycle (:134-148) — each click advances one step
		assertTrue(tPipe.monkeyWrench((byte)4)); // (0,0) -> (0,1) emit-off
		assertEquals(0, tPipe.mDisabledInputs, "inputs untouched");
		assertEquals(tBit, tPipe.mDisabledOutputs & tBit, "outputs disabled");
		assertTrue(tPipe.monkeyWrench((byte)4)); // (0,1) -> (1,0) accept-off
		assertEquals(tBit, tPipe.mDisabledInputs & tBit, "inputs disabled");
		assertEquals(0, tPipe.mDisabledOutputs & tBit, "outputs re-enabled");
		assertTrue(tPipe.monkeyWrench((byte)4)); // (1,0) -> (1,1) both-off
		assertEquals(tBit, tPipe.mDisabledInputs & tBit);
		assertEquals(tBit, tPipe.mDisabledOutputs & tBit, "both disabled");
		assertTrue(tPipe.monkeyWrench((byte)4)); // (1,1) -> (0,0) normal
		assertEquals(0, tPipe.mDisabledInputs, "back to the plain pipe");
		assertEquals(0, tPipe.mDisabledOutputs);
		// out-of-range side: no-op
		assertFalse(tPipe.monkeyWrench((byte)6));
	}

	@Test
	public void disabledOutputsBlockTheEmitGate() {
		GTItemPipeBlockEntity tPipe = pipe();
		tPipe.mLastReceivedFrom = 2; // latched to side 2
		// :272 — the anti-ping-pong arm: no emit back out the latched side
		assertFalse(tPipe.canEmitItemsTo((byte)2, tPipe), ":272 — aSender == this && aSide == mLastReceivedFrom refuses");
		assertTrue(tPipe.canEmitItemsTo((byte)3, tPipe), ":272 — the other faces emit");
		assertTrue(tPipe.canEmitItemsTo((byte)2, new Object()), "a foreign sender is not the ping-pong shape");
		// :225 — the disabled output face refuses before canEmitItemsTo (insertItemStackIntoTileEntity)
		tPipe.mDisabledOutputs |= TileEntityBase09Connector.SBIT[3];
		assertFalse(tPipe.insertItemStackIntoTileEntity(new Object(), (byte)3), ":225 — the disabled output face short-circuits (level-less no-target)");
		// :273 — accept = connected
		assertTrue(tPipe.canAcceptItemsFrom((byte)3, null));
		tPipe.mConnections = 0;
		assertFalse(tPipe.canAcceptItemsFrom((byte)3, null));
	}

	// ---------------------------------------------------------------------------
	// capacity (:243-251)
	// ---------------------------------------------------------------------------

	@Test
	public void capacityWindowIsInvSizePerWindow() {
		GTItemPipeBlockEntity tPipe = pipe(); // invSize 1
		assertTrue(tPipe.pipeCapacityCheck(), ":246 — mTransferredItems <= 0 always passes");
		assertFalse(tPipe.incrementTransferCounter(1), ":243/:246 — one item spends the invSize=1 budget (1 < 1 false)");
		assertFalse(tPipe.incrementTransferCounter(1), ":243 — beyond budget");
		// a 4-slot shape carries 4 per window
		GTItemPipeBlockEntity tWide = pipe();
		tWide.mInventory = new GTItemStackHandler(4, tWide::setChanged);
		assertTrue(tWide.incrementTransferCounter(1), "1 < 4");
		assertTrue(tWide.incrementTransferCounter(1), "2 < 4");
		assertTrue(tWide.incrementTransferCounter(1), "3 < 4");
		assertFalse(tWide.incrementTransferCounter(1), "4 < 4 is false — the invSize=4 budget spent");
	}

	// ---------------------------------------------------------------------------
	// scanPipes (:77-101) — the distance order over the adjacency seam
	// ---------------------------------------------------------------------------

	/** A synthetic network node: fixed stepSize, wired adjacency, all faces open. */
	private static final class TestPipe extends GTItemPipeBlockEntity {
		final GTItemPipeBlockEntity[] tNeighbors = new GTItemPipeBlockEntity[6];

		TestPipe() {
			super(sType, POS, Blocks.STONE.defaultBlockState());
			mConnections = 63;
		}

		@Override
		protected GTItemPipeBlockEntity adjacentItemPipe(byte aSide) {
			return tNeighbors[aSide];
		}
	}

	@Test
	public void scanPipesWalksAscendingDistanceAndRelaxesToTheMinimum() {
		TestPipe tSource = new TestPipe();
		TestPipe tNear = new TestPipe();
		TestPipe tFar = new TestPipe();
		TestPipe tBackdoor = new TestPipe();
		// near is a heavy hop (a restrictive-pipe stand-in, upstream :80 stepSize weight);
		// tBackdoor sits behind BOTH near (expensive) and far (cheap) — the min-distance
		// memo (:80/:81) must relax to the cheap path
		tSource.mStepSize = 8192;  // upstream :78 — the source adds its own stepSize first
		tNear.mStepSize = 65536;
		tFar.mStepSize = 32768;
		tBackdoor.mStepSize = 32768;
		tSource.tNeighbors[1] = tNear;
		tSource.tNeighbors[2] = tFar;
		tNear.tNeighbors[0] = tSource;
		tNear.tNeighbors[3] = tBackdoor;
		tFar.tNeighbors[4] = tBackdoor;
		tBackdoor.tNeighbors[3] = tNear;
		tBackdoor.tNeighbors[4] = tFar;

		Map<GTItemPipeBlockEntity, Long> tScan = GTItemPipeBlockEntity.scanPipes(tSource, new LinkedHashMap<>(), 0, false);
		assertEquals(4, tScan.size(), "all four reachable pipes");
		assertEquals(Long.valueOf(8192), tScan.get(tSource), "8192+0 — the source's own stepSize");
		assertEquals(Long.valueOf(40960), tScan.get(tFar), "8192+32768");
		assertEquals(Long.valueOf(73728), tScan.get(tNear), "8192+65536");
		assertEquals(Long.valueOf(73728), tScan.get(tBackdoor), "min(8192+65536+32768, 8192+32768+32768) = the FAR path wins");

		List<GTItemPipeBlockEntity> tOrder = GTItemPipeBlockEntity.pipesAscending(tScan);
		assertEquals(tSource, tOrder.get(0), "the source itself heads the scan (:201)");
		assertEquals(tFar, tOrder.get(1));
		assertEquals(tNear, tOrder.get(2), "tie 73728 keeps insertion order");
		assertEquals(tBackdoor, tOrder.get(3));
	}

	@Test
	public void scanPipesHonoursTheCapacityGateAndFaceMasksOrthogonality() {
		TestPipe tSource = new TestPipe();
		TestPipe tSpent = new TestPipe();
		tSource.mStepSize = 8192;
		tSpent.mStepSize = 8192;
		tSpent.mTransferredItems = 1; // the invSize=1 budget spent → pipeCapacityCheck false
		tSource.tNeighbors[0] = tSpent;
		tSpent.tNeighbors[0] = tSource;
		Map<GTItemPipeBlockEntity, Long> tScan = GTItemPipeBlockEntity.scanPipes(tSource, new LinkedHashMap<>(), 0, false);
		assertEquals(1, tScan.size(), ":80 — a pipe at capacity is neither a target nor routed through");
		// aIgnoreCapacity = T (the :80 first arm) includes it
		assertEquals(2, GTItemPipeBlockEntity.scanPipes(tSource, new LinkedHashMap<>(), 0, true).size());
		// the face-disable masks are NOT routing gates (the :91/:93 gates are connected-only)
		tSpent.mTransferredItems = 0;
		tSpent.mDisabledOutputs = 63;
		tSpent.mDisabledInputs = 63;
		assertEquals(2, GTItemPipeBlockEntity.scanPipes(tSource, new LinkedHashMap<>(), 0, false).size(),
				"routing ignores the monkeywrench masks — the transfer enforces them (:225/:268)");
	}

	// ---------------------------------------------------------------------------
	// NBT round trip (:86-95/:105-112)
	// ---------------------------------------------------------------------------

	@Test
	public void nbtRoundTripCarriesLatchMasksCounterAndInventory() {
		GTItemPipeBlockEntity tPipe = pipe();
		tPipe.mLastReceivedFrom = 3;
		tPipe.oLastReceivedFrom = 3;
		tPipe.mDisabledInputs = 0x08;
		tPipe.mDisabledOutputs = 0x10;
		tPipe.mTransferredItems = 42;
		tPipe.mInventory.setStackInSlot(0, stone(7));

		CompoundTag tSaved = tPipe.saveWithoutMetadata();
		assertEquals((byte)3, tSaved.getByte("mlast"));
		assertEquals((byte)3, tSaved.getByte("olast"));
		assertEquals((byte)0x08, tSaved.getByte("inputs"));
		assertEquals((byte)0x10, tSaved.getByte("outputs"));
		assertEquals(42, tSaved.getLong("transferred"));
		assertTrue(tSaved.contains("inventory", Tag.TAG_COMPOUND));
		assertEquals("item_pipe", tSaved.getString("te_name"));

		GTItemPipeBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals((byte)3, tBack.mLastReceivedFrom);
		assertEquals((byte)0x08, tBack.mDisabledInputs);
		assertEquals((byte)0x10, tBack.mDisabledOutputs);
		assertEquals(42, tBack.mTransferredItems);
		assertEquals(7, tBack.mInventory.getStackInSlot(0).getCount(), "the inventory rides the NBT");
	}

	@Test
	public void teNameMatchesTheBetPath() {
		assertEquals("item_pipe", pipe().getTileEntityName(), "upstream :291 — the BET registry path mirrors it");
	}
}
