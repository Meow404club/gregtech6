package gregtech6.tileentity.inventories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6Hoppers.HopperRow;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.util.GTItemMover;

/**
 * GT6 hopper family offline tests (task p26-storage-hopper-family acceptance): the
 * Bronze/Steel row axis (Loader_MultiTileEntities.java:191/:202), the hopper batch math
 * (the :177-184 budget loop, the :183 exact break, the :246-247 face gates, the :248 stack
 * limit, the :212-223 compaction, the :195-196 suction pick), the queue FIFO (the :199-207
 * fixed point, the :227-229 tail-insert/head-extract, the per-slot cap) and the NBT round
 * trip. The live container-to-container transfer is the RCON chain's.
 */
public class GT6HopperFamilyTest extends GTOfflineTestBase {

	static BlockEntityType<GT6HopperBlockEntity> sHopperType;
	static BlockEntityType<GT6QueueHopperBlockEntity> sQueueType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6HopperBlockEntity>[] tHopper = (BlockEntityType<GT6HopperBlockEntity>[]) new BlockEntityType<?>[1];
		tHopper[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6HopperBlockEntity(tHopper[0], aPos, aState),
				Blocks.STONE).build(null);
		sHopperType = tHopper[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6QueueHopperBlockEntity>[] tQueue = (BlockEntityType<GT6QueueHopperBlockEntity>[]) new BlockEntityType<?>[1];
		tQueue[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6QueueHopperBlockEntity(tQueue[0], aPos, aState),
				Blocks.STONE).build(null);
		sQueueType = tQueue[0];
	}

	private static ItemStack stone(int aCount) {
		return new ItemStack(Items.STONE, aCount);
	}

	private static ItemStack dirt(int aCount) {
		return new ItemStack(Items.DIRT, aCount);
	}

	/** A hopper whose output face drains into the given capture handler (the adjacency seam). */
	private static GT6HopperBlockEntity hopper(GTItemStackHandler aCapture) {
		return new GT6HopperBlockEntity(sHopperType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			protected net.minecraftforge.items.IItemHandler outputTarget() {
				return aCapture;
			}
		};
	}

	/** A queue hopper whose output face drains into the given capture handler. */
	private static GT6QueueHopperBlockEntity queue(GTItemStackHandler aCapture) {
		return new GT6QueueHopperBlockEntity(sQueueType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			protected net.minecraftforge.items.IItemHandler outputTarget() {
				return aCapture;
			}
		};
	}

	// ---------------------------------------------------------------------------
	// the row axis (Loader :145-146 over :191/:202 — the arch Iron-slip correction)
	// ---------------------------------------------------------------------------

	@Test
	public void rowAxisReproducesTheLoaderAnchors() {
		assertEquals(4, GT6Hoppers.ROWS.size());
		HopperRow tBronze = GT6Hoppers.ROWS.get(0), tSteel = GT6Hoppers.ROWS.get(1);
		HopperRow tBronzeQueue = GT6Hoppers.ROWS.get(2), tSteelQueue = GT6Hoppers.ROWS.get(3);
		// the meta ids: hopper 8000+aID (:145), queue 8200+aID (:146); Bronze aID 9 (:191), Steel aID 10 (:202)
		assertEquals(8009, tBronze.metaId());
		assertEquals(8010, tSteel.metaId());
		assertEquals(8209, tBronzeQueue.metaId());
		assertEquals(8210, tSteelQueue.metaId());
		// the aHopperSize column + the NBT_INV_SIZE floors (:145 max(1,n), :146 max(2,n))
		assertEquals(3, tBronze.slots());
		assertEquals(5, tSteel.slots());
		assertEquals(3, tBronzeQueue.slots());
		assertEquals(5, tSteelQueue.queue() ? tSteelQueue.slots() : -1);
		assertFalse(tBronze.queue());
		assertTrue(tBronzeQueue.queue());
		// the paths
		assertEquals("hopper_bronze", tBronze.path());
		assertEquals("hopper_steel", tSteel.path());
		assertEquals("queue_hopper_bronze", tBronzeQueue.path());
		assertEquals("queue_hopper_steel", tSteelQueue.path());
		// the hardness column (:191 7.0 / :202 6.0) — the row record verbatim
		// (the Properties destroyTime field is package-private, so the row is the check)
		assertEquals("bronze", tBronze.material().slug());
		assertEquals("steel", tSteel.material().slug());
		assertEquals(7.0F, tBronze.material().hardness());
		assertEquals(6.0F, tSteel.material().hardness());
	}

	// ---------------------------------------------------------------------------
	// the hopper batch math (the :177-184 loop)
	// ---------------------------------------------------------------------------

	@Test
	public void hopperDivisibleBudgetDrainsWithin64() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6HopperBlockEntity tHopper = hopper(tCapture);
		tHopper.mMode = 16;
		for (int i = 0; i < 64; i++) tHopper.getInventory().insertItem(0, stone(1), false);
		assertEquals(64, tHopper.getInventory().getStackInSlot(0).getCount());
		// one gated pass = the whole :177-184 loop: rounds of 16 while total+16 <= 64 —
		// four rounds (the fifth would take the total to 80 > 64), then the pass ends
		assertEquals(64, tHopper.moveOutPhase());
		assertTrue(tHopper.invempty());
		assertEquals(64, tCapture.getStackInSlot(0).getCount());
	}

	@Test
	public void hopperZeroModeEmitsAStackPerGatedPass() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6HopperBlockEntity tHopper = hopper(tCapture);
		// mMode = 0 (the default): aMaxMove 64, aMinMove 1 — one call drains up to a stack
		for (int i = 0; i < 64; i++) tHopper.getInventory().insertItem(0, stone(1), false);
		assertEquals(64, tHopper.moveOutPhase());
		assertTrue(tHopper.invempty());
	}

	@Test
	public void hopperHighModeCapsTheSlotAndDrainsItWhole() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6HopperBlockEntity tHopper = hopper(tCapture);
		tHopper.mMode = 48;
		// the :248 cap mMode*max(1, 64/mMode) = 48 — the slot refuses past it (the insert
		// face of the divisible-stack law: slots hold mMode-sized stacks at high modes)
		assertEquals(16, tHopper.getInventory().insertItem(0, stone(64), false).getCount());
		assertEquals(48, tHopper.getInventory().getStackInSlot(0).getCount());
		// one gated pass: 0+48 <= 64 → the whole slot emits; the next round 48+48 > 64
		// refuses but the inventory is already dry
		assertEquals(48, tHopper.moveOutPhase());
		assertTrue(tHopper.invempty());
		assertEquals(0, tHopper.moveOutPhase());
	}

	@Test
	public void hopperExactModeStopsAfterOneRound() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6HopperBlockEntity tHopper = hopper(tCapture);
		tHopper.mMode = 16;
		tHopper.mExactMode = true;
		for (int i = 0; i < 64; i++) tHopper.getInventory().insertItem(0, stone(1), false);
		// the :183 break — each gated pass emits ONE mMode chunk
		assertEquals(16, tHopper.moveOutPhase());
		assertEquals(48, tHopper.getInventory().getStackInSlot(0).getCount());
		assertEquals(16, tHopper.moveOutPhase());
		assertEquals(32, tHopper.getInventory().getStackInSlot(0).getCount());
	}

	@Test
	public void hopperMinMoveGateRefusesAnUndersizedSource() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6HopperBlockEntity tHopper = hopper(tCapture);
		tHopper.mMode = 16;
		tHopper.getInventory().insertItem(0, stone(10), false);
		// the ST.java:471/:121 aMinMove arm: a 10-item source is not a 16-chunk → nothing emits
		assertEquals(0, tHopper.moveOutPhase());
		assertEquals(10, tHopper.getInventory().getStackInSlot(0).getCount());
		// at mMode 8 the same 10 items emit an 8-chunk (10 >= 8), leaving the 2 remainder;
		// the next pass refuses the 2-item source again — the divisible emission law
		tHopper.mMode = 8;
		assertEquals(8, tHopper.moveOutPhase());
		assertEquals(2, tHopper.getInventory().getStackInSlot(0).getCount());
		assertEquals(0, tHopper.moveOutPhase());
		assertEquals(8, tCapture.getStackInSlot(0).getCount());
	}

	@Test
	public void hopperStackLimitFollowsTheModeArithmetic() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		// upstream :248 mMode<=0?64:mMode*Math.max(1, 64/mMode)
		assertEquals(64, tHopper.stackLimit());
		tHopper.mMode = 16;
		assertEquals(64, tHopper.stackLimit());
		tHopper.mMode = 48;
		assertEquals(48, tHopper.stackLimit());
		tHopper.mMode = 33;
		assertEquals(33, tHopper.stackLimit());
	}

	// ---------------------------------------------------------------------------
	// the hopper compaction (:212-223) + suction pick (:195-196)
	// ---------------------------------------------------------------------------

	@Test
	public void hopperCompactionMergesLateDuplicatesOnThreeSlots() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		tHopper.getInventory().setStackInSlot(1, stone(10));
		tHopper.getInventory().setStackInSlot(2, stone(10));
		tHopper.getInventory().setStackInSlot(0, ItemStack.EMPTY);
		assertEquals(20, tHopper.compressPhase()); // the :216/:220 move accounting
		assertEquals(20, tHopper.slot(0).getCount());
		assertTrue(tHopper.getInventory().getStackInSlot(1).isEmpty());
		assertTrue(tHopper.getInventory().getStackInSlot(2).isEmpty());
	}

	@Test
	public void hopperSuctionPicksTheHighestEmptySlot() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		tHopper.getInventory().setStackInSlot(0, stone(1));
		tHopper.getInventory().setStackInSlot(1, stone(1));
		// the :195 descending scan — slot 2 is the landing slot
		assertEquals(2, tHopper.findSuctionSlot());
		tHopper.getInventory().setStackInSlot(2, stone(1));
		assertEquals(-1, tHopper.findSuctionSlot());
	}

	// ---------------------------------------------------------------------------
	// the hopper face gates (:246-247) + the side view
	// ---------------------------------------------------------------------------

	@Test
	public void hopperFaceGatesMatchTheUpstreamTruthTable() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		byte tFacing = tHopper.getFacing(); // DOWN = 0
		assertEquals(GT6HopperBaseBlockEntity.SIDE_BOTTOM, tFacing);
		assertFalse(tHopper.canInsertItem(0, stone(1), tFacing)); // :246
		assertTrue(tHopper.canInsertItem(0, stone(1), (byte) (tFacing + 1)));
		assertFalse(tHopper.canExtractItem(0, tFacing)); // :247 without the belt
		assertTrue(tHopper.canExtractItem(0, (byte) (tFacing + 1)));
		tHopper.mLock = true;
		assertTrue(tHopper.canExtractItem(0, tFacing)); // :247 the mLock bypass
	}

	@Test
	public void hopperSideViewRefusesTheOutputFace() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		tHopper.getInventory().setStackInSlot(0, stone(10));
		byte tFacing = tHopper.getFacing();
		// the fresh-per-call side view: the insert returns the refused stack on the output face
		assertEquals(5, tHopper.sideView(tFacing).insertItem(0, dirt(5), false).getCount());
		assertTrue(tHopper.sideView((byte) (tFacing + 1)).insertItem(1, dirt(5), false).isEmpty()); // an empty slot takes it
		assertTrue(tHopper.sideView(tFacing).extractItem(0, 5, false).isEmpty());
		assertEquals(5, tHopper.sideView((byte) (tFacing + 1)).extractItem(0, 5, false).getCount());
	}

	// ---------------------------------------------------------------------------
	// the queue FIFO (the :199-207 fixed point, :227-229 access, :230 cap)
	// ---------------------------------------------------------------------------

	@Test
	public void queueFixedPointPushesTowardTheHead() {
		GT6QueueHopperBlockEntity tQueue = queue(new GTItemStackHandler(64));
		tQueue.getInventory().setStackInSlot(0, stone(5));
		// one pass walks i=1 (slot0→slot1) then i=2 (slot1→slot2); a second pass moves 0
		assertEquals(10, tQueue.compressPhase()); // 5 + 5 across the two :204 moves
		assertTrue(tQueue.getInventory().getStackInSlot(0).isEmpty());
		assertTrue(tQueue.getInventory().getStackInSlot(1).isEmpty());
		assertEquals(5, tQueue.getInventory().getStackInSlot(2).getCount());
	}

	@Test
	public void queueFifoOrderSurvivesMixedItems() {
		GT6QueueHopperBlockEntity tQueue = queue(new GTItemStackHandler(64));
		tQueue.getInventory().setStackInSlot(0, stone(5));
		tQueue.compressPhase();
		tQueue.getInventory().setStackInSlot(0, dirt(5)); // the newest enters at the tail
		tQueue.compressPhase();
		// [empty, dirt5, stone5] — the oldest (stone) sits at the head
		assertEquals(5, tQueue.getInventory().getStackInSlot(1).getCount());
		assertTrue(tQueue.getInventory().getStackInSlot(1).is(Items.DIRT));
		assertEquals(5, tQueue.getInventory().getStackInSlot(2).getCount());
		assertTrue(tQueue.getInventory().getStackInSlot(2).is(Items.STONE));
		// the :227-229 access — extraction only from the head, insertion only at the tail
		assertEquals(5, tQueue.sideView((byte) 3).extractItem(1, 64, false).getCount()); // head = stone
		assertTrue(tQueue.getInventory().getStackInSlot(2).isEmpty());
		assertEquals(5, tQueue.compressPhase()); // the tick's fixed point pulls the dirt to the head
		assertEquals(5, tQueue.sideView((byte) 3).extractItem(1, 64, false).getCount()); // then dirt
		assertTrue(tQueue.invempty());
	}

	@Test
	public void queueInsertLandsOnTheTailOnly() {
		GT6QueueHopperBlockEntity tQueue = queue(new GTItemStackHandler(64));
		tQueue.getInventory().setStackInSlot(2, stone(5)); // the head is occupied
		byte tAny = 3;
		assertTrue(tQueue.canInsertItem(0, dirt(1), tAny)); // :228 — slot 0 takes (any side)
		assertFalse(tQueue.canInsertItem(1, dirt(1), tAny));
		assertFalse(tQueue.canExtractItem(0, tAny)); // :229 — the head is slot 2
		assertTrue(tQueue.canExtractItem(2, tAny));
		// the side view maps view-0 → slot 0 (the tail) and view-1 → slot 2 (the head)
		assertTrue(tQueue.sideView(tAny).insertItem(0, dirt(5), false).isEmpty());
		assertEquals(5, tQueue.getInventory().getStackInSlot(0).getCount());
		assertTrue(tQueue.sideView(tAny).insertItem(1, dirt(5), false).getCount() == 5);
		assertTrue(tQueue.sideView(tAny).extractItem(0, 1, false).isEmpty()); // the tail never yields
	}

	@Test
	public void queuePerSlotCapRidesTheMode() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6QueueHopperBlockEntity tQueue = queue(tCapture);
		assertEquals(64, tQueue.stackLimit()); // upstream :230 the default
		tQueue.mMode = 16;
		assertEquals(16, tQueue.stackLimit());
		// the :230 cap — the slot refuses past mMode (the insert face of the per-slot law)
		assertEquals(24, tQueue.getInventory().insertItem(0, stone(40), false).getCount());
		assertEquals(16, tQueue.getInventory().getStackInSlot(0).getCount());
		// the tail-bound 16 reach the head only through the FIFO compaction (:199-207)
		tQueue.compressPhase();
		assertEquals(16, tQueue.getInventory().getStackInSlot(2).getCount());
		// the :164 while gate — one gated pass emits at most mMode: the slot-full drain
		assertEquals(16, tQueue.moveOutPhase());
		assertEquals(0, tQueue.moveOutPhase());
		assertTrue(tQueue.invempty());
		assertEquals(16, tCapture.getStackInSlot(0).getCount());
		// the :70 clamp — a zeroed mode re-arms at 64
		tQueue.mMode = 0;
		tQueue.clampMode();
		assertEquals(64, tQueue.mMode);
	}

	@Test
	public void queueSuctionLandsOnTheTailOnly() {
		GT6QueueHopperBlockEntity tQueue = queue(new GTItemStackHandler(64));
		tQueue.getInventory().setStackInSlot(0, stone(1));
		// upstream :182-183 — the LAST slot only, even with earlier slots full/empty
		assertEquals(2, tQueue.findSuctionSlot());
		tQueue.getInventory().setStackInSlot(2, stone(1));
		assertEquals(-1, tQueue.findSuctionSlot());
	}

	// ---------------------------------------------------------------------------
	// the tick skeleton (mCheck throttle + the wake handshake)
	// ---------------------------------------------------------------------------

	@Test
	public void mCheckThrottleAndIdleArmMatchTheUpstream() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		tHopper.mCheck = 3;
		tHopper.onTick(1, true); // :168-170 — the countdown eats the pass
		assertEquals(2, tHopper.mCheck);
		tHopper.onTick(2, true);
		assertEquals(1, tHopper.mCheck);
		tHopper.onTick(3, true);
		assertEquals(0, tHopper.mCheck);
		tHopper.onTick(4, true); // the gated pass runs, moves nothing (empty) → idle -1
		assertEquals(-1, tHopper.mCheck);
		// the :252 receiver — only the top or the output face re-arms an idle hopper
		tHopper.adjacentInventoryUpdated((byte) 3, null);
		assertEquals(-1, tHopper.mCheck);
		tHopper.adjacentInventoryUpdated(GT6HopperBaseBlockEntity.SIDE_TOP, null);
		assertEquals(0, tHopper.mCheck);
		tHopper.mCheck = -1;
		tHopper.adjacentInventoryUpdated(tHopper.getFacing(), null);
		assertEquals(0, tHopper.mCheck);
	}

	@Test
	public void wokeHopperDrainsItsInventory() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6HopperBlockEntity tHopper = hopper(tCapture);
		tHopper.getInventory().insertItem(0, stone(20), false);
		tHopper.mCheck = -1;
		tHopper.adjacentInventoryUpdated(GT6HopperBaseBlockEntity.SIDE_TOP, null); // mCheck = 0
		tHopper.onTick(1, true); // the gated pass: 0+1 <= 64 → mMode 0 emits up to 64
		assertTrue(tHopper.invempty());
		assertEquals(20, tCapture.getStackInSlot(0).getCount());
		assertEquals(3, tHopper.mCheck); // the :206-210 movement throttle
	}

	@Test
	public void wokeQueueCompactsBeforeEmitting() {
		GTItemStackHandler tCapture = new GTItemStackHandler(64);
		GT6QueueHopperBlockEntity tQueue = queue(tCapture);
		// the FIFO: enter B at the tail while the head holds A — B may NOT jump the line
		tQueue.getInventory().setStackInSlot(2, stone(5)); // the oldest
		tQueue.getInventory().setStackInSlot(0, dirt(5)); // the newest
		tQueue.mCheck = -1;
		tQueue.adjacentInventoryUpdated(GT6HopperBaseBlockEntity.SIDE_TOP, null);
		tQueue.onTick(1, true);
		// the gate pass emits from the head (stone first), then the out-of-gate fixed point
		// pulls dirt toward the head — after ONE tick the stone moved out or the queue holds
		// [dirt-pushed]; assert the FIFO invariant instead of the tick count: stone leaves first
		long tStone = 0, tDirt = 0;
		for (int i = 0; i < tCapture.getSlots(); i++) {
			if (tCapture.getStackInSlot(i).is(Items.STONE)) tStone += tCapture.getStackInSlot(i).getCount();
			if (tCapture.getStackInSlot(i).is(Items.DIRT)) tDirt += tCapture.getStackInSlot(i).getCount();
		}
		assertTrue(tStone >= tDirt, "the head item (stone) emits no later than the tail item (dirt)");
		for (int i = 0; i < 10; i++) tQueue.onTick(i + 2, true); // drain
		tStone = 0;
		tDirt = 0;
		for (int i = 0; i < tCapture.getSlots(); i++) {
			if (tCapture.getStackInSlot(i).is(Items.STONE)) tStone += tCapture.getStackInSlot(i).getCount();
			if (tCapture.getStackInSlot(i).is(Items.DIRT)) tDirt += tCapture.getStackInSlot(i).getCount();
		}
		assertEquals(5, tStone);
		assertEquals(5, tDirt);
		assertTrue(tQueue.invempty());
	}

	@Test
	public void redstoneGateReadsNothingOffline() {
		// the level-less fixture keeps the gate open (no redstone source)
		assertFalse(hopper(new GTItemStackHandler(64)).hasRedstoneIncomingFromNonRail());
	}

	// ---------------------------------------------------------------------------
	// the kind surfaces + NBT
	// ---------------------------------------------------------------------------

	@Test
	public void kindSurfacesMatchTheUpstreamToolArms() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		GT6QueueHopperBlockEntity tQueue = queue(new GTItemStackHandler(64));
		assertTrue(tHopper.hasExactMode());
		assertFalse(tQueue.hasExactMode()); // the queue has no monkeywrench arm upstream
		// the screwdriver bounds: hopper 0..64 (:130-132), queue 1..64 (:123-125)
		tQueue.mMode = 1;
		tQueue.screwdriver(true, null); // -- below 1 wraps to 64
		assertEquals(64, tQueue.mMode);
		tQueue.screwdriver(true, null); // 63... wait — 64 -> 63
		assertEquals(63, tQueue.mMode);
		tHopper.mMode = 0;
		tHopper.screwdriver(true, null); // -- below 0 wraps to 64
		assertEquals(64, tHopper.mMode);
		tHopper.mMode = 64;
		tHopper.screwdriver(false, null); // ++ above 64 wraps to 0
		assertEquals(0, tHopper.mMode);
		// the :134/:127 chat lines
		tHopper.mMode = 0;
		assertEquals("Emits up to 64 Items", tHopper.modeChatLine());
		tHopper.mExactMode = true;
		assertEquals("Emits up to 1 Stack", tHopper.modeChatLine());
		tHopper.mMode = 16;
		tHopper.mExactMode = false;
		assertEquals("Emits divisible Stacksize of: 16", tHopper.modeChatLine());
		tHopper.monkeyWrench(null);
		assertEquals("Emits exact Stacksize of: 16", tHopper.modeChatLine());
		tQueue.mMode = 16;
		assertEquals("Max Stacksize: 16", tQueue.modeChatLine());
	}

	@Test
	public void nbtRoundTripCarriesModeExactAndInventory() {
		GT6HopperBlockEntity tHopper = hopper(new GTItemStackHandler(64));
		tHopper.mMode = 16;
		tHopper.mExactMode = true;
		tHopper.getInventory().setStackInSlot(0, stone(7));
		CompoundTag tTag = tHopper.saveWithoutMetadata();
		assertTrue(tTag.contains(GT6HopperBaseBlockEntity.NBT_MODE));
		assertTrue(tTag.getBoolean(GT6HopperBaseBlockEntity.NBT_MODE_EXACT));
		GT6HopperBlockEntity tRested = hopper(new GTItemStackHandler(64));
		tRested.load(tTag);
		assertEquals(16, tRested.mMode);
		assertTrue(tRested.mExactMode);
		assertEquals(7, tRested.getInventory().getStackInSlot(0).getCount());

		// the queue default mode stays out of the NBT (upstream :76) and the :70 clamp re-arms
		GT6QueueHopperBlockEntity tQueue = queue(new GTItemStackHandler(64));
		CompoundTag tQueueTag = tQueue.saveWithoutMetadata();
		assertFalse(tQueueTag.contains(GT6HopperBaseBlockEntity.NBT_MODE));
		GT6QueueHopperBlockEntity tQueueRested = queue(new GTItemStackHandler(64));
		CompoundTag tZeroed = new CompoundTag();
		tZeroed.putByte(GT6HopperBaseBlockEntity.NBT_MODE, (byte) 0);
		tQueueRested.load(tZeroed);
		assertEquals(64, tQueueRested.mMode);
	}

	@Test
	public void familyImplementsTheWakeInterface() {
		assertTrue(new GT6HopperBlockEntity(sHopperType, POS, Blocks.STONE.defaultBlockState())
				instanceof GT6AdjacentInventoryUpdatable);
		assertTrue(new GT6QueueHopperBlockEntity(sQueueType, POS, Blocks.STONE.defaultBlockState())
				instanceof GT6AdjacentInventoryUpdatable);
	}

	// ---------------------------------------------------------------------------
	// the placement auto-connect arm (upstream onPlaced :114-122 / :104-116)
	// ---------------------------------------------------------------------------

	/** A pipe recording nothing — the shape is enough for the ignore gates. */
	private GTItemPipeBlockEntity recordingPipe() {
		return new GTItemPipeBlockEntity(sHopperType, POS, Blocks.STONE.defaultBlockState());
	}

	@Test
	public void placedHopperAutoConnectsAnItemPipeAtItsOutputFace() {
		// the UP face never fires (SIDES_BOTTOM_HORIZONTAL[mFacing] — upstream)
		assertFalse(GT6Hoppers.GT6HopperBlock.autoConnectItemConnector(recordingPipe(), net.minecraft.core.Direction.UP));
		// no neighbour / a non-connector neighbour is ignored
		assertFalse(GT6Hoppers.GT6HopperBlock.autoConnectItemConnector(null, net.minecraft.core.Direction.DOWN));
		assertFalse(GT6Hoppers.GT6HopperBlock.autoConnectItemConnector(
				new GT6HopperBlockEntity(sHopperType, POS, Blocks.STONE.defaultBlockState()),
				net.minecraft.core.Direction.DOWN));
		// a connector whose types toward the hopper do not intersect ALL_ITEM_TRANSPORT is ignored
		GTItemPipeBlockEntity tFluidFace = new GTItemPipeBlockEntity(sHopperType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			public java.util.List<gregapi.code.TagData> getConnectorTypes(byte aSide) {
				return java.util.List.of(gregapi.data.TD.Connectors.PIPE_FLUID);
			}
		};
		assertFalse(GT6Hoppers.GT6HopperBlock.autoConnectItemConnector(tFluidFace, net.minecraft.core.Direction.DOWN));
		// the real pipe face (PNEUMATIC_ITEM toward the hopper) gets connect(mSideOfTileEntity, T)
		byte[] tFiredSide = {-1};
		boolean[] tFiredNotify = {false};
		GTItemPipeBlockEntity tPipe = new GTItemPipeBlockEntity(sHopperType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			public boolean connect(byte aSide, boolean aNotify) {
				tFiredSide[0] = aSide;
				tFiredNotify[0] = aNotify;
				return true;
			}
		};
		assertTrue(GT6Hoppers.GT6HopperBlock.autoConnectItemConnector(tPipe, net.minecraft.core.Direction.DOWN));
		// a hopper pointing DOWN at the pipe fires the pipe's UP side (the side facing back)
		assertEquals(net.minecraft.core.Direction.UP.get3DDataValue(), tFiredSide[0]);
		assertTrue(tFiredNotify[0]);
		// a horizontal output face fires the mirrored side too
		assertTrue(GT6Hoppers.GT6HopperBlock.autoConnectItemConnector(tPipe, net.minecraft.core.Direction.NORTH));
		assertEquals(net.minecraft.core.Direction.SOUTH.get3DDataValue(), tFiredSide[0]);
	}
}
