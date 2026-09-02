package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverConveyor;
import gregtech6.covers.covers.CoverRobotArm;

/**
 * The conveyor + robot arm acceptance tables (task p11-cover-conveyor-robotarm), all
 * offline: the 512&gt;&gt;i timing-tier truth table (PERIOD semantics — tier 0 beats once
 * per 512 ticks, tier 9 every tick), the screwdriver/monkeywrench tool tables, the
 * one-way item gates, the GTItemMover consumption-point alignment (the direction split
 * and the arm's fixed-slot quadrant table over handler doubles), the oven decorator
 * consumption of the gates (the p10 wrapper loop, capability-free) and the value/visual
 * NBT round-trip. The live capability views and the real per-beat transfer are the RCON
 * chain's job (ForgeCapabilities cannot class-init offline — the
 * TestMachineBlockEntityNBTTest:71 precedent; a real neighbour needs a level). The
 * offline mounts therefore ride the setCoverItem force path (the force install skips the
 * placement intercepts, ICoverableTE :147-158); the non-force gate is pinned separately.
 */
public class CoverConveyorRobotArmTest extends GTCoverTestBase {

	static BlockEntityType<TileEntityOvenCoverProbe> sProbeType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildProbeFixture() {
		// the base @BeforeAll already bootstrapped; a probe BET over the vanilla fixture block
		// (the machines-base precedent — the registries are frozen after boot)
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now
		}
		BlockEntityType<TileEntityOvenCoverProbe>[] tHolder = (BlockEntityType<TileEntityOvenCoverProbe>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOvenCoverProbe(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sProbeType = tHolder[0];
	}

	static TileEntityOvenCoverProbe probe() {
		return new TileEntityOvenCoverProbe(sProbeType, COVER_POS, Blocks.BRICKS.defaultBlockState());
	}

	/**
	 * Mounts the cover on the probe's east face (5) via the FORCE path (see class javadoc)
	 * and returns the host.
	 */
	static TileEntityOvenCoverProbe mount(ICover aCover, byte aSide) {
		TileEntityOvenCoverProbe tProbe = probe();
		CoverRegistry.put(Items.BRICK, aCover);
		assertTrue(tProbe.setCoverItem(aSide, new ItemStack(Items.BRICK), null, true, true), "the force mount installs the cover");
		return tProbe;
	}

	private static ItemStack stone(int aCount) {
		return new ItemStack(Items.STONE, aCount);
	}

	private static ItemStack dirt(int aCount) {
		return new ItemStack(Items.DIRT, aCount);
	}

	private static ItemStackHandler handler(int aSlots, ItemStack... aStacks) {
		ItemStackHandler tHandler = new ItemStackHandler(aSlots);
		for (int i = 0; i < aStacks.length; i++) {
			tHandler.setStackInSlot(i, aStacks[i]);
		}
		return tHandler;
	}

	// ---------------------------------------------------------------------------
	// the 512>>i timing tiers — PERIODS, not caps (MultiItemTechnological.java:51/:53
	// pass 512>>i into the ctor whose mTiming only feeds SERVER_TIME % mTiming at
	// CoverConveyor.java:66 / CoverRobotArm.java:84)
	// ---------------------------------------------------------------------------

	@Test
	public void timingTierTableIsTheUpstream512DividedDown() {
		assertEquals(10, CoverConveyor.TIMING_TIERS.length, "ten conveyor tiers (metas 12040+i)");
		for (int i = 0; i < 10; i++) {
			assertEquals(512 >> i, CoverConveyor.TIMING_TIERS[i], "tier " + i + " period = 512>>i");
			assertEquals(512 >> i, new CoverConveyor(512 >> i).mTiming, "the ctor keeps the period verbatim");
		}
		assertEquals(1, new CoverConveyor(0).mTiming, "the ctor floors at 1 (upstream Math.max(1, aTiming))");
		assertEquals(100, new CoverConveyor(100).mTiming, "a custom period passes through");
	}

	@Test
	public void beatIsThePeriodModuloForBothCovers() {
		// tier 0 = 512 ticks: only the multiples of 512 beat
		assertTrue(CoverConveyor.isBeat(512, 0));
		assertTrue(CoverConveyor.isBeat(512, 512));
		assertTrue(CoverConveyor.isBeat(512, 15360));
		assertFalse(CoverConveyor.isBeat(512, 1));
		assertFalse(CoverConveyor.isBeat(512, 511));
		assertFalse(CoverConveyor.isBeat(512, 256), "half the period is NOT a beat — a period, not a per-tick cap");
		// tier 9 = 1 tick: every tick beats
		assertTrue(CoverConveyor.isBeat(1, 0));
		assertTrue(CoverConveyor.isBeat(1, 1));
		assertTrue(CoverConveyor.isBeat(1, 987654321L));
		// tier 8 = 2 ticks: the even ticks
		assertTrue(CoverConveyor.isBeat(2, 100));
		assertFalse(CoverConveyor.isBeat(2, 101));
		// the arm runs the same formula on the same table
		assertEquals(CoverConveyor.isBeat(512, 511), CoverRobotArm.isBeat(512, 511));
		assertEquals(CoverConveyor.isBeat(1, 0), CoverRobotArm.isBeat(1, 0));
		assertFalse(CoverRobotArm.isBeat(64, 63));
		assertTrue(CoverRobotArm.isBeat(64, 128));
	}

	// ---------------------------------------------------------------------------
	// the tool tables
	// ---------------------------------------------------------------------------

	@Test
	public void conveyorScrewdriverTogglesTheDirectionAndDamagesTheTool() {
		TileEntityOvenCoverProbe tProbe = mount(new CoverConveyor(4), (byte) 5);
		CoverData tData = tProbe.getCovers();
		CoverConveyor tConveyor = (CoverConveyor) tData.mBehaviours[5];

		assertEquals(1000, tConveyor.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F),
				"the upstream screwdriver damage");
		assertEquals(1, tData.mVisuals[5], "out (0) → in (1)");
		assertEquals(1000, tConveyor.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 900, null, false, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(0, tData.mVisuals[5], "in (1) → out (0) — the toggle flips both ways");
		assertEquals(0, tConveyor.onToolClick((byte) 5, tData, ICover.TOOL_CROWBAR, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F), "the crowbar is not the conveyor's tool");
	}

	@Test
	public void armToolsStepTheSlotAndFlipTheDirection() {
		TileEntityOvenCoverProbe tProbe = mount(new CoverRobotArm(4), (byte) 5);
		CoverData tData = tProbe.getCovers();
		CoverRobotArm tArm = (CoverRobotArm) tData.mBehaviours[5];

		// the screwdriver steps the slot address by +1 (200 = the upstream tool damage)
		assertEquals(200, tArm.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(1, tData.mValues[5], "0 → 1 (put into slot 1)");
		assertEquals(200, tArm.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 1000, null, true, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(0, tData.mValues[5], "sneaking inverts the step");
		// below zero the encoding is TAKE from slot -1-v
		tArm.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 1000, null, true, (byte) 5, 0.5F, 0.5F, 0.5F);
		assertEquals(-1, tData.mValues[5], "0 → -1 = take from slot 0");
		assertEquals(200, tArm.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 1000, null, true, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(-2, tData.mValues[5], "-1 → -2 = take from slot 1");

		// the step clamps at the short range (upstream UT.Code.bind(MIN, MAX, step), UT.java:1544-1545)
		assertEquals(Short.MAX_VALUE, CoverRobotArm.stepSlot(Short.MAX_VALUE, false), "clamped, not wrapped");
		assertEquals(Short.MIN_VALUE, CoverRobotArm.stepSlot(Short.MIN_VALUE, true), "clamped, not wrapped");
		assertEquals((short) -4, CoverRobotArm.stepSlot((short) -5, false));

		// the monkeywrench flips the direction lane (1000 = the upstream tool damage)
		assertEquals(1000, tArm.onToolClick((byte) 5, tData, CoverRobotArm.TOOL_MONKEYWRENCH, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(1, tData.mVisuals[5], "out (0) → in (1)");
		assertEquals(1000, tArm.onToolClick((byte) 5, tData, CoverRobotArm.TOOL_MONKEYWRENCH, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(0, tData.mVisuals[5], "in (1) → out (0)");
		assertEquals(0, tArm.onToolClick((byte) 5, tData, ICover.TOOL_CROWBAR, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F), "the crowbar is not the arm's tool");
	}

	// ---------------------------------------------------------------------------
	// the one-way item gates (upstream :89-90 / :115-116 verbatim)
	// ---------------------------------------------------------------------------

	@Test
	public void conveyorAndArmOneWayGateTables() {
		TileEntityOvenCoverProbe tConveyorProbe = mount(new CoverConveyor(4), (byte) 5);
		CoverData tConveyorData = tConveyorProbe.getCovers();
		CoverConveyor tConveyor = (CoverConveyor) tConveyorData.mBehaviours[5];

		ItemStack tStack = stone(1);
		assertTrue(tConveyor.interceptItemInsert((byte) 5, tConveyorData, 0, tStack, (byte) 5), "the out-face refuses incoming items");
		assertFalse(tConveyor.interceptItemExtract((byte) 5, tConveyorData, 0, tStack, (byte) 5), "the out-face lets its own extract through the gate");
		assertFalse(tConveyor.interceptItemInsert((byte) 5, tConveyorData, 0, tStack, (byte) 2), "the gate only binds its own face");
		assertFalse(tConveyor.interceptItemExtract((byte) 5, tConveyorData, 0, tStack, (byte) 2));
		tConveyorData.visual((byte) 5, (short) 1);
		assertFalse(tConveyor.interceptItemInsert((byte) 5, tConveyorData, 0, tStack, (byte) 5), "the in-face takes items in");
		assertTrue(tConveyor.interceptItemExtract((byte) 5, tConveyorData, 0, tStack, (byte) 5), "the in-face refuses outgoing items");

		// the arm's gate pair is verbatim the same (:115-116)
		TileEntityOvenCoverProbe tArmProbe = mount(new CoverRobotArm(4), (byte) 5);
		CoverData tArmData = tArmProbe.getCovers();
		CoverRobotArm tArm = (CoverRobotArm) tArmData.mBehaviours[5];
		assertTrue(tArm.interceptItemInsert((byte) 5, tArmData, 0, tStack, (byte) 5));
		assertFalse(tArm.interceptItemExtract((byte) 5, tArmData, 0, tStack, (byte) 5));
		tArmData.visual((byte) 5, (short) 1);
		assertFalse(tArm.interceptItemInsert((byte) 5, tArmData, 0, tStack, (byte) 5));
		assertTrue(tArm.interceptItemExtract((byte) 5, tArmData, 0, tStack, (byte) 5));
	}

	@Test
	public void ovenDecoratorHonoursTheConveyorOneWayGate() {
		// the p10 wrapper loop, capability-free: the side-aware decorator runs the queried
		// face's cover gates BEFORE the inner inventory (TileEntityOven.newCoverGatedHandler)
		TileEntityOvenCoverProbe tProbe = mount(new CoverConveyor(4), (byte) 5); // east face, visual 0 = out
		tProbe.getInventory().setStackInSlot(0, stone(10));

		ItemStack tRefused = tProbe.newCoverGatedHandler(Direction.EAST).insertItem(0, dirt(3), false);
		assertEquals(3, tRefused.getCount(), "the out-face refuses the insert through the decorator");
		assertEquals(10, tProbe.getInventory().getStackInSlot(0).getCount(), "nothing entered the oven");

		ItemStack tDrawn = tProbe.newCoverGatedHandler(Direction.EAST).extractItem(0, 5, false);
		assertEquals(5, tDrawn.getCount(), "the out-face lets its own extract through the decorator");
		assertEquals(5, tProbe.getInventory().getStackInSlot(0).getCount());

		// visual 1 (in): the gate flips
		tProbe.getCovers().visual((byte) 5, (short) 1);
		ItemStack tAccepted = tProbe.newCoverGatedHandler(Direction.EAST).insertItem(2, dirt(3), false);
		assertTrue(tAccepted.isEmpty(), "the in-face accepts the insert (into the empty slot 2)");
		assertEquals(3, tProbe.getInventory().getStackInSlot(2).getCount());
		assertTrue(tProbe.newCoverGatedHandler(Direction.EAST).extractItem(0, 5, false).isEmpty(), "the in-face refuses the extract through the decorator");

		// an uncovered face is unobstructed by the east-face cover
		ItemStack tFree = tProbe.newCoverGatedHandler(Direction.UP).extractItem(0, 5, false);
		assertEquals(5, tFree.getCount(), "the UP face answers for its own (absent) cover — unobstructed");
	}

	// ---------------------------------------------------------------------------
	// the GTItemMover consumption-point alignment over handler doubles
	// ---------------------------------------------------------------------------

	@Test
	public void conveyorDirectionSplitsOverTheTwoViews() {
		// :67-71 — visual 0 = host gives, visual 1 = host takes
		ItemStackHandler tHost = handler(2, stone(20));
		ItemStackHandler tNeighbour = handler(2);

		CoverConveyor.moveByDirection((short) 0, tHost, tNeighbour); // the offline seam drives the direction split
		assertEquals(0, tHost.getStackInSlot(0).getCount(), "visual 0 moves OUT of the host");
		assertEquals(20, tNeighbour.getStackInSlot(0).getCount(), "visual 0 lands in the neighbour");

		CoverConveyor.moveByDirection((short) 1, handler(2), tNeighbour);
		assertEquals(0, tNeighbour.getStackInSlot(0).getCount(), "visual 1 moves OUT of the neighbour");

		// one stack per beat: the first-match single transfer (GTItemMover.move contract)
		ItemStackHandler tTwoStacks = handler(3, stone(10), dirt(10));
		ItemStackHandler tSink = handler(3);
		CoverConveyor.moveByDirection((short) 0, tTwoStacks, tSink);
		assertEquals(0, tTwoStacks.getStackInSlot(0).getCount(), "the first source slot drained");
		assertEquals(10, tTwoStacks.getStackInSlot(1).getCount(), "the second source slot is the NEXT beat's business");
		assertEquals(10, tSink.getStackInSlot(0).getCount());
	}

	@Test
	public void armQuadrantTableRoutesFixedSlotAndDirection() {
		// :85-97 — the four quadrants over the value (slot) and visual (direction) lanes

		// take (v<0) visual 0: the FIXED slot is on the HOST, side-less; the neighbour scans
		CoverRobotArm.SideViews tHost = new CoverRobotArm.SideViews(
				handler(3, stone(20), dirt(20), stone(5)),
				handler(3, stone(20), dirt(20), stone(5)));
		CoverRobotArm.SideViews tNeighbour = new CoverRobotArm.SideViews(handler(3), handler(3));
		CoverRobotArm.armTransfer((short) -3, (short) 0, tHost, tNeighbour);
		assertEquals(0, tHost.sideLess().getStackInSlot(2).getCount(), "took from the HOST slot -1-v = 2");
		assertEquals(5, tNeighbour.normal().getStackInSlot(0).getCount(), "the neighbour received");
		assertEquals(5, tHost.normal().getStackInSlot(2).getCount(), "the normal view is a separate lane — the fixed half used the side-less one");

		// take (v<0) visual 1: the fixed slot is on the NEIGHBOUR, side-less; the host scans
		CoverRobotArm.SideViews tHost2 = new CoverRobotArm.SideViews(handler(3, stone(20)), handler(3, stone(20)));
		ItemStackHandler tNeighbour2SideLess = handler(3);
		tNeighbour2SideLess.setStackInSlot(1, dirt(7));
		CoverRobotArm.SideViews tNeighbour2 = new CoverRobotArm.SideViews(handler(3), tNeighbour2SideLess);
		CoverRobotArm.armTransfer((short) -2, (short) 1, tHost2, tNeighbour2);
		assertEquals(0, tNeighbour2.sideLess().getStackInSlot(1).getCount(), "took from the NEIGHBOUR slot -1-v = 1");
		assertEquals(7, tHost2.normal().getStackInSlot(1).getCount(), "the host received (slot 0 holds foreign stone, the first fit is slot 1)");
		assertTrue(tNeighbour2.normal().getStackInSlot(1).isEmpty(), "the neighbour's normal lane is untouched by the side-less take");

		// put (v>=0) visual 0: the FIXED slot is on the NEIGHBOUR, side-less; the host scans
		CoverRobotArm.SideViews tHost3 = new CoverRobotArm.SideViews(handler(3, stone(9)), handler(3, stone(9)));
		CoverRobotArm.SideViews tNeighbour3 = new CoverRobotArm.SideViews(handler(3), handler(3));
		CoverRobotArm.armTransfer((short) 1, (short) 0, tHost3, tNeighbour3);
		assertEquals(9, tNeighbour3.sideLess().getStackInSlot(1).getCount(), "put into the NEIGHBOUR slot v = 1");
		assertEquals(0, tHost3.normal().getStackInSlot(0).getCount(), "the host gave");
		assertEquals(9, tHost3.sideLess().getStackInSlot(0).getCount(), "the host's side-less lane is untouched by the normal-view scan");

		// put (v>=0) visual 1: the fixed slot is on the HOST, side-less; the neighbour scans
		CoverRobotArm.SideViews tHost4 = new CoverRobotArm.SideViews(handler(3), handler(3));
		CoverRobotArm.SideViews tNeighbour4 = new CoverRobotArm.SideViews(handler(3, dirt(4)), handler(3, dirt(4)));
		CoverRobotArm.armTransfer((short) 2, (short) 1, tHost4, tNeighbour4);
		assertEquals(4, tHost4.sideLess().getStackInSlot(2).getCount(), "put into the HOST slot v = 2");
		assertEquals(0, tNeighbour4.normal().getStackInSlot(0).getCount(), "the neighbour gave");
		assertEquals(4, tNeighbour4.sideLess().getStackInSlot(0).getCount(), "the neighbour's side-less lane is untouched by the normal-view scan");
	}

	@Test
	public void armFixedSlotOutOfRangeMovesNothingAndZeroMeansPut() {
		// ST.java:514/:517/:538/:544 via GTItemMover — the fixed slot is bounds-checked
		CoverRobotArm.SideViews tHost = new CoverRobotArm.SideViews(handler(3, stone(20)), handler(3, stone(20)));
		CoverRobotArm.SideViews tNeighbour = new CoverRobotArm.SideViews(handler(3), handler(3));

		// v = -99 → take slot 98 on a 3-slot handler
		CoverRobotArm.armTransfer((short) -99, (short) 0, tHost, tNeighbour);
		assertEquals(20, tHost.sideLess().getStackInSlot(0).getCount(), "nothing moved");
		assertEquals(0, tNeighbour.normal().getStackInSlot(0).getCount());

		// v = 99 → put into slot 99
		CoverRobotArm.armTransfer((short) 99, (short) 0, tHost, tNeighbour);
		assertEquals(20, tHost.normal().getStackInSlot(0).getCount(), "nothing moved");
		for (int i = 0; i < 3; i++) assertTrue(tNeighbour.sideLess().getStackInSlot(i).isEmpty());

		// v = 0 is a PUT into slot 0 — the take encoding starts at -1
		CoverRobotArm.armTransfer((short) 0, (short) 0, tHost, tNeighbour);
		assertEquals(0, tHost.normal().getStackInSlot(0).getCount(), "v=0 puts: the host gave");
		assertEquals(20, tNeighbour.sideLess().getStackInSlot(0).getCount(), "the stone landed in the neighbour slot 0");
	}

	// ---------------------------------------------------------------------------
	// the tick guards (the offline level-less no-op) and the placement gate
	// ---------------------------------------------------------------------------

	@Test
	public void tickWithoutALevelIsAHarmlessNoOp() {
		TileEntityOvenCoverProbe tProbe = mount(new CoverConveyor(1), (byte) 5);
		tProbe.getInventory().setStackInSlot(0, stone(10));
		// no level → no views → the beat fires and moves nothing (offline guard, the pump precedent)
		tProbe.getCovers().tickPre(1, true, false, false);
		assertEquals(10, tProbe.getInventory().getStackInSlot(0).getCount(), "the conveyor beat without a level moves nothing");

		TileEntityOvenCoverProbe tArmProbe = mount(new CoverRobotArm(1), (byte) 5);
		tArmProbe.getInventory().setStackInSlot(0, stone(10));
		tArmProbe.getCovers().tickPre(1, true, false, false);
		assertEquals(10, tArmProbe.getInventory().getStackInSlot(0).getCount(), "the arm beat without a level moves nothing");
	}

	@Test
	public void placementGateRefusesAHostWithoutAnItemSurface() {
		// upstream :41 canTick() && instanceof IInventory — the port maps the IInventory half
		// onto the item-handler surface; offline the level guard dominates (the live
		// acceptance — the oven mounts, an itemless host would not — is the RCON chain's
		// business, which installs through the non-force path)
		CoverConveyor tConveyor = new CoverConveyor(4);
		TileEntityOvenCoverProbe tOven = probe();
		CoverData tData = CoverRegistry.coverdata(tOven, null);
		assertTrue(tConveyor.interceptCoverPlacement((byte) 5, tData, null), "no level → no view → the gate refuses");
		CoverRobotArm tArm = new CoverRobotArm(4);
		assertTrue(tArm.interceptCoverPlacement((byte) 5, tData, null), "the arm runs the same gate");
	}

	// ---------------------------------------------------------------------------
	// the NBT round-trip (needsVisualsSaved + the value lane)
	// ---------------------------------------------------------------------------

	@Test
	public void conveyorDirectionRoundTripsThroughTheCoversNbt() {
		TileEntityOvenCoverProbe tProbe = mount(new CoverConveyor(4), (byte) 5);
		tProbe.getCovers().visual((byte) 5, (short) 1); // the screwdriver flipped it to IN

		CompoundTag tSaved = new CompoundTag();
		tProbe.writeCoversToNBT(tSaved);
		CompoundTag tCoversTag = tSaved.getCompound(ICoverableTE.NBT_COVERS);
		assertEquals(1, tCoversTag.getShort(CoverData.VISUAL_KEYS[5]), "needsVisualsSaved=true keeps the direction in the save (CoverData :87)");

		// rehydration: a fresh store over the saved compound restores the behaviour + the lane
		TileEntityOvenCoverProbe tBack = probe();
		CoverData tBackData = CoverRegistry.coverdata(tBack, tCoversTag);
		assertNotNull(tBackData.mBehaviours[5], "the conveyor re-resolves from the id lane");
		assertEquals(1, tBackData.mVisuals[5], "the direction survives the round-trip");
		assertTrue(tBackData.mBehaviours[5].needsVisualsSaved((byte) 5, tBackData));
	}

	@Test
	public void armSlotAndDirectionRoundTripThroughTheCoversNbt() {
		TileEntityOvenCoverProbe tProbe = mount(new CoverRobotArm(4), (byte) 5);
		CoverData tData = tProbe.getCovers();
		tData.visual((byte) 5, (short) 1); // monkeywrench → in
		tData.value((byte) 5, (short) 3); // screwdriver → put into slot 3

		CompoundTag tSaved = new CompoundTag();
		tProbe.writeCoversToNBT(tSaved);
		CompoundTag tCoversTag = tSaved.getCompound(ICoverableTE.NBT_COVERS);
		assertEquals(1, tCoversTag.getShort(CoverData.VISUAL_KEYS[5]), "the direction lane");
		assertEquals(3, tCoversTag.getShort(CoverData.VALUE_KEYS[5]), "the slot lane persists unconditionally (CoverData :85)");

		// the value lane of side 5 rides key "5" — a neighbouring side's key must not appear
		assertFalse(tCoversTag.contains(CoverData.VALUE_KEYS[4]));

		TileEntityOvenCoverProbe tBack = probe();
		CoverData tBackData = CoverRegistry.coverdata(tBack, tCoversTag);
		assertNotNull(tBackData.mBehaviours[5], "the arm re-resolves from the id lane");
		assertEquals(1, tBackData.mVisuals[5], "the direction survives");
		assertEquals(3, tBackData.mValues[5], "the slot address survives");
		assertNull(tBackData.mBehaviours[0], "the other faces stay empty");
	}

	@Test
	public void coverTexturesFollowTheVisualLane() {
		TileEntityOvenCoverProbe tProbe = mount(new CoverConveyor(4), (byte) 5);
		CoverData tData = tProbe.getCovers();
		assertEquals(CoverConveyor.CONVEYOR_OUT_SPRITE, tData.mBehaviours[5].getCoverTextureSurface((byte) 5, tData));
		tData.visual((byte) 5, (short) 1);
		assertEquals(CoverConveyor.CONVEYOR_IN_SPRITE, tData.mBehaviours[5].getCoverTextureSurface((byte) 5, tData));

		TileEntityOvenCoverProbe tArmProbe = mount(new CoverRobotArm(4), (byte) 5);
		assertEquals(CoverRobotArm.ROBOT_ARM_OUT_SPRITE, tArmProbe.getCovers().mBehaviours[5].getCoverTextureSurface((byte) 5, tArmProbe.getCovers()));
	}
}
