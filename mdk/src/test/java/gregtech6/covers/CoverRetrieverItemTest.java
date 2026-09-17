package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


import net.minecraft.core.BlockPos;import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverRetrieverItem;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.tileentity.connectors.SideItemHandler;

/**
 * CoverRetrieverItem offline tests (task p31-retriever-cover): the placement gate
 * (CoverRetrieverItem.java:50 — ticking item pipes only), the disjunctive pull trigger
 * (:61), the filtered pull through the synthetic pipe network with the path-prefix
 * counter payment (:72-74), the inverted mode (:95), the capacity-window stop and the
 * covered-face item gate (:138-139). The live RCON chain (acceptance ④) covers the
 * world-level scenario.
 */
public class CoverRetrieverItemTest extends GTCoverTestBase {

	static BlockEntityType<GTItemPipeBlockEntity> sPipeType;

	static final BlockPos PIPE_POS = new BlockPos(2, 2, 2);
	/** The covered face of the host pipe (Direction 2 = NORTH) — the pull target side. */
	static final byte COVER_SIDE = 2;
	/** The network face: the source pipe hangs here. */
	static final byte NET_SIDE = 3;
	/** The source container face on the source pipe. */
	static final byte SOURCE_SIDE = 4;

	CoverRetrieverItem mCover = new CoverRetrieverItem();

	@BeforeAll
	static void buildPipeFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTItemPipeBlockEntity>[] tHolder = (BlockEntityType<GTItemPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> (GTItemPipeBlockEntity) new TestPipe(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sPipeType = tHolder[0];
	}

	@BeforeEach
	void putRetrieverFixture() {
		CoverRegistry.put(Items.DIAMOND, mCover); // GTCoverTestBase.reset ran first (super @BeforeEach)
	}

	@AfterEach
	void clearRetrieverFixture() {
		CoverRegistry.reset();
	}

	/** A synthetic pipe with wired pipe- and container-adjacency (the GTItemPipeBlockEntityTest.TestPipe shape). */
	static final class TestPipe extends GTItemPipeBlockEntity {
		final GTItemPipeBlockEntity[] tPipes = new GTItemPipeBlockEntity[6];
		final IItemHandler[] tContainers = new IItemHandler[6];

		TestPipe(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
			mConnections = 63;
		}

		void wire(GTItemPipeBlockEntity aPipe, byte aSide, IItemHandler aHandler) {
			((TestPipe) aPipe).tContainers[aSide] = aHandler;
		}

		void wireSlot(GTItemPipeBlockEntity aPipe, byte aSide, ItemStack aStack) {
			ItemStackHandler tHandler = new ItemStackHandler(1);
			tHandler.setStackInSlot(0, aStack);
			wire(aPipe, aSide, tHandler);
		}

		@Override
		protected GTItemPipeBlockEntity adjacentItemPipe(byte aSide) {
			return tPipes[aSide];
		}

		@Override
		public IItemHandler adjacentInventoryOf(GTItemPipeBlockEntity aPipe, byte aSide) {
			// the stub wires per-TestPipe arrays — the receiver may differ from aPipe (the
			// cover asks the host to walk ANY scanned pipe)
			return ((TestPipe) aPipe).tContainers[aSide];
		}
	}

	private static ItemStack stone(int aCount) {
		return new ItemStack(Items.STONE, aCount);
	}

	private static ItemStack dirt(int aCount) {
		return new ItemStack(Items.DIRT, aCount);
	}

	/** host --net(NET_SIDE)-- pipe --source(SOURCE_SIDE): container; host --COVER_SIDE: target container. */
	private TestPipe host() {
		TestPipe tHost = new TestPipe(sPipeType, PIPE_POS, Blocks.STONE.defaultBlockState());
		tHost.wireSlot(tHost, COVER_SIDE, ItemStack.EMPTY);
		return tHost;
	}

	private TestPipe twoPipeNetwork(TestPipe aHost) {
		TestPipe tPipe = new TestPipe(sPipeType, PIPE_POS.south(), Blocks.STONE.defaultBlockState());
		aHost.tPipes[NET_SIDE] = tPipe;
		tPipe.tPipes[0] = aHost; // scanPipes walks the back face for the connector-type intersection
		return tPipe;
	}

	private void install(TestPipe aHost) {
		assertTrue(aHost.setCoverItem(COVER_SIDE, new ItemStack(Items.DIAMOND), null, false, true),
				":50 — a ticking item pipe admits the retriever");
	}

	/** Arms the fast trigger (the mValues lane — the onStoppedUpdate/controller path) and runs one cover tick. */
	private void drive(TestPipe aHost, long aTimer) {
		CoverData tData = aHost.getCovers();
		if (tData.mValues[COVER_SIDE] == 0) tData.value(COVER_SIDE, (short) 1); // upstream :55-57
		tData.tickPre(aTimer, true, false, false);
	}

	// ---------------------------------------------------------------------------
	// the placement gate (:50)
	// ---------------------------------------------------------------------------

	@Test
	public void mountsOnTickingPipesAndRefusesNonPipes() {
		TestPipe tHost = host();
		install(tHost);
		assertTrue(tHost.isCovered(COVER_SIDE));
		assertSame(mCover, tHost.getCovers().mBehaviours[COVER_SIDE]);

		// a non-pipe coverable host (the oven probe) refuses
		TileEntityOvenCoverProbe tOven = bareOven();
		assertTrue(mCover.interceptCoverPlacement(COVER_SIDE, CoverRegistry.coverdata(tOven, null), null),
				":50 — non-pipe hosts refuse");
		assertFalse(mCover.interceptCoverPlacement(COVER_SIDE, CoverRegistry.coverdata(tHost, null), null),
				":50 — the ticking item pipe admits");
	}

	// ---------------------------------------------------------------------------
	// the pull round (:61-78)
	// ---------------------------------------------------------------------------

	@Test
	public void filteredPullMovesTheMatchingStackAlongThePath() {
		TestPipe tHost = host();
		TestPipe tPipe = twoPipeNetwork(tHost);
		ItemStackHandler tSource = new ItemStackHandler(2);
		tSource.setStackInSlot(0, stone(10));
		tSource.setStackInSlot(1, dirt(10));
		tPipe.wire(tPipe, SOURCE_SIDE, tSource);
		ItemStackHandler tTarget = (ItemStackHandler) tHost.tContainers[COVER_SIDE];
		install(tHost);
		tHost.getCovers().mNBTs[COVER_SIDE] = CoverFilterItem.filterTagFor(stone(1));

		drive(tHost, 0); // the mValues fast path — the phase gate is moot

		assertEquals(10, tTarget.getStackInSlot(0).getCount(), ":72 — the filter stack moved");
		assertSame(Items.STONE, tTarget.getStackInSlot(0).getItem());
		assertEquals(0, tSource.getStackInSlot(0).getCount(), "the source slot drained");
		assertEquals(10, tSource.getStackInSlot(1).getCount(), ":467 — the non-matching stack stayed");
		assertEquals(1, tHost.mTransferredItems, ":73 — the host pays the path prefix");
		assertEquals(1, tPipe.mTransferredItems, ":73 — the scanned pipe pays");
		assertEquals(0, tHost.getCovers().mValues[COVER_SIDE], ":62 — the value lane cleared");
	}

	@Test
	public void secondTriggerWithoutValueLaneDoesNothing() {
		TestPipe tHost = host();
		TestPipe tPipe = twoPipeNetwork(tHost);
		ItemStackHandler tSource = new ItemStackHandler(2);
		tSource.setStackInSlot(0, stone(10));
		tSource.setStackInSlot(1, stone(10));
		tPipe.wire(tPipe, SOURCE_SIDE, tSource);
		ItemStackHandler tTarget = (ItemStackHandler) tHost.tContainers[COVER_SIDE];
		install(tHost);

		drive(tHost, 0); // pulls one stack
		assertEquals(10, tTarget.getStackInSlot(0).getCount());

		// the value lane cleared (:62) and timer 1 misses the phase (:61 first arm) — no pull
		tHost.getCovers().tickPre(1, true, false, false);
		assertEquals(10, tTarget.getStackInSlot(0).getCount(), ":61 — the disjunctive trigger gates the retry");

		// the phase gate fires on its own (:61 second arm — SERVER_TIME % 20 == 15); the
		// invSize=1 window from the first pull is spent, so reset it first (upstream :193)
		tHost.mTransferredItems = 0;
		tHost.getCovers().tickPre(CoverRetrieverItem.TRIGGER_PHASE, true, false, false);
		assertEquals(20, tTarget.getStackInSlot(0).getCount(), "the periodic pull ran");
	}

	@Test
	public void invertedModePullsEverythingButTheFilter() {
		TestPipe tHost = host();
		TestPipe tPipe = twoPipeNetwork(tHost);
		ItemStackHandler tSource = new ItemStackHandler(2);
		tSource.setStackInSlot(0, stone(10));
		tSource.setStackInSlot(1, dirt(10));
		tPipe.wire(tPipe, SOURCE_SIDE, tSource);
		ItemStackHandler tTarget = (ItemStackHandler) tHost.tContainers[COVER_SIDE];
		install(tHost);
		tHost.getCovers().mNBTs[COVER_SIDE] = CoverFilterItem.filterTagFor(stone(1));

		assertEquals(1000, mCover.onToolClick(COVER_SIDE, tHost.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, COVER_SIDE, 0.5F, 0.5F, 0.5F),
				":97 — the screwdriver damage");
		assertEquals(1, tHost.getCovers().mVisuals[COVER_SIDE], ":95 — the visual lane flipped");
		assertTrue(mCover.needsVisualsSaved(COVER_SIDE, tHost.getCovers()), ":143 — the invert marker persists");

		drive(tHost, 0);

		assertSame(Items.DIRT, tTarget.getStackInSlot(0).getItem(), ":72 — the inverted pull takes the NON-matching stack");
		assertEquals(10, tSource.getStackInSlot(0).getCount(), "the matching stack stayed");
		assertEquals(10000, mCover.onToolClick(COVER_SIDE, tHost.getCovers(), CoverRetrieverItem.TOOL_SOFTHAMMER, 0, null, false, COVER_SIDE, 0.5F, 0.5F, 0.5F),
				":101 — the soft hammer damage");
		assertNull(tHost.getCovers().mNBTs[COVER_SIDE], ":100 — the whole lane cleared");
	}

	@Test
	public void capacityWindowStopsThePull() {
		TestPipe tHost = host(); // the vanilla-block fixture: invSize 1 → the 1-per-window budget
		TestPipe tPipe = twoPipeNetwork(tHost);
		ItemStackHandler tSource = new ItemStackHandler(2);
		tSource.setStackInSlot(0, stone(10));
		tSource.setStackInSlot(1, stone(10));
		tPipe.wire(tPipe, SOURCE_SIDE, tSource);
		ItemStackHandler tTarget = (ItemStackHandler) tHost.tContainers[COVER_SIDE];
		install(tHost);

		drive(tHost, 0);
		assertEquals(10, tTarget.getStackInSlot(0).getCount(), "the first pull ran");
		assertEquals(1, tHost.mTransferredItems, ":73");

		drive(tHost, 0); // re-arms the value lane, but the capacity gate (:61 pipeCapacityCheck) refuses
		assertEquals(10, tTarget.getStackInSlot(0).getCount(), ":246 — the invSize=1 window is spent");
		assertEquals(1, tHost.mTransferredItems, "no counter growth on the refused round");

		tHost.mTransferredItems = 0; // the 20-tick window reset (upstream :193)
		drive(tHost, 0);
		assertEquals(20, tTarget.getStackInSlot(0).getCount(), "the reset window pulls again");
	}

	@Test
	public void missingTargetOrFilterDegradeGracefully() {
		// no target container on the covered face — the trimmed ST.put fallback keeps the pull silent
		TestPipe tBare = new TestPipe(sPipeType, PIPE_POS, Blocks.STONE.defaultBlockState());
		TestPipe tPipe = twoPipeNetwork(tBare);
		tPipe.wireSlot(tPipe, SOURCE_SIDE, stone(10));
		install(tBare);
		tBare.getCovers().mNBTs[COVER_SIDE] = CoverFilterItem.filterTagFor(stone(1));
		drive(tBare, 0);
		assertEquals(10, ((ItemStackHandler) tPipe.tContainers[SOURCE_SIDE]).getStackInSlot(0).getCount(),
				"no target — nothing pulled, nothing lost");
		assertEquals(0, tBare.mTransferredItems, "no counter payment without a move");

		// an unset filter lane pulls anything (the null-filter gate)
		TestPipe tNoFilter = host();
		TestPipe tPipe2 = twoPipeNetwork(tNoFilter);
		tPipe2.wireSlot(tPipe2, SOURCE_SIDE, dirt(10));
		ItemStackHandler tTarget2 = (ItemStackHandler) tNoFilter.tContainers[COVER_SIDE];
		install(tNoFilter);
		drive(tNoFilter, 0);
		assertEquals(10, tTarget2.getStackInSlot(0).getCount(), "null filter = no gate");
	}

	// ---------------------------------------------------------------------------
	// the covered-face item gate (:138-139) through the SideItemHandler dispatch
	// ---------------------------------------------------------------------------

	@Test
	public void coveredFaceRefusesForeignItemTraffic() {
		TestPipe tHost = host();
		install(tHost);

		// the covered face refuses inserts (the retriever plate is the face's answer)
		ItemStack tRest = new SideItemHandler(tHost, COVER_SIDE).insertItem(0, stone(8), false);
		assertEquals(8, tRest.getCount(), ":138 — the covered face refuses the insert");

		// a plain face still behaves like the bare pipe (the empty pipe latches to the insert)
		ItemStack tRest2 = new SideItemHandler(tHost, (byte) 5).insertItem(0, stone(4), false);
		assertTrue(tRest2.isEmpty(), "a bare face inserts as usual");
		assertEquals(4, tHost.mInventory.getStackInSlot(0).getCount());
	}

	@Test
	public void storeDissolvesOnCoverRemoval() {
		// the offline BE has no level (isServerSideTE false) — the crowbar-with-drop arm is
		// the RCON chain's (live /gt6cover dismantle); here the removal path dissolves the store
		TestPipe tHost = host();
		install(tHost);
		assertTrue(tHost.setCoverItem(COVER_SIDE, ItemStack.EMPTY, null, false, true), "the removal accepted");
		assertNull(tHost.getCovers(), ":313-317 — an all-empty store dissolves to null");
	}
}
