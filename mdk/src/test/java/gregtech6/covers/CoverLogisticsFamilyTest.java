package gregtech6.covers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.covers.covers.logistics.AbstractCoverLogisticsDisplay;
import gregtech6.covers.covers.logistics.AbstractCoverLogisticsFiltered;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPULogic;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidExport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericDump;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericExport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericImport;
import gregtech6.covers.covers.logistics.CoverLogisticsItemExport;
import gregtech6.covers.covers.logistics.CoverLogisticsItemStorage;
import gregtech6.tileentity.connectors.GTLogisticsWireBlockEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The p33 logistics cover family offline acceptance (task p33-logistics-covers-12):
 * the priority bit lanes (upstream AbstractCoverAttachmentLogistics :59-81), the filtered
 * bus lanes, and the Dump exclusion live through the Core scan (the :479-494 protected
 * set minus-arm driven by COVER-mounted endpoints, not the seeded set of the lv3 test).
 */
public class CoverLogisticsFamilyTest extends GTCoverTestBase {

	/** The wire host probe — the offline wire BE (the ICoverableTE composition target). */
	static BlockEntityType<GTLogisticsWireBlockEntity> sWireType;

	static final BlockPos WIRE_POS = new BlockPos(5, 5, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildWireFixture() {
		sWireType = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTLogisticsWireBlockEntity(sWireType, aPos, aState),
				Blocks.BRICKS).build(null);
	}

	/** A wire host with a mounted cover face (the composition store drives it). */
	static GTLogisticsWireBlockEntity wireWith(ICover aCover, byte aSide) {
		return wireWith(Items.BRICKS, aCover, aSide);
	}

	/** The registry-backed form — the id lane carries the item's registry id so the load re-resolves. */
	static GTLogisticsWireBlockEntity wireWith(net.minecraft.world.item.Item aItem, ICover aCover, byte aSide) {
		GTLogisticsWireBlockEntity tWire = sWireType.create(WIRE_POS, Blocks.BRICKS.defaultBlockState());
		tWire.mCovers = new CoverData(tWire);
		tWire.mCovers.set(aSide, new ItemStack(aItem)); // the id lane = the registry id, the behaviour = the registry cover
		tWire.mCovers.mBehaviours[aSide] = aCover; // the behaviour swap (the NBT test asserts the re-resolve)
		return tWire;
	}

	// ------------------------------------------------------------------
	// the priority bit lanes (upstream :59-81)
	// ------------------------------------------------------------------

	@Test
	public void screwdriverCyclesThePriorityBits() {
		ICover tCover = CoverLogisticsItemExport.INSTANCE; // usePriorities=T, useTargetStackSize=T
		GTLogisticsWireBlockEntity tWire = wireWith(tCover, (byte) 0);

		tCover.onToolClick((byte) 0, tWire.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0); // :59
		assertEquals(1, tWire.mCovers.mValues[0] & 3, "the low bits cycle 0->1 (generic)");
		tCover.onToolClick((byte) 0, tWire.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(2, tWire.mCovers.mValues[0] & 3, "1->2 (semi)");
		tCover.onToolClick((byte) 0, tWire.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(3, tWire.mCovers.mValues[0] & 3, "2->3 (filtered)");
		tCover.onToolClick((byte) 0, tWire.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(0, tWire.mCovers.mValues[0] & 3, "3->0 wraps (unmodified)");
		assertEquals(10000, tCover.onToolClick((byte) 0, tWire.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0) % 10001, "damage 10000");
	}

	@Test
	public void cutterCyclesTheTargetStacksizeBits() {
		ICover tCover = CoverLogisticsItemExport.INSTANCE;
		GTLogisticsWireBlockEntity tWire = wireWith(tCover, (byte) 0);

		tCover.onToolClick((byte) 0, tWire.mCovers, AbstractCoverAttachmentLogistics.TOOL_CUTTER, 0, null, false, (byte) 1, 0, 0, 0); // :71
		assertEquals(1, (tWire.mCovers.mValues[0] >> 2) & 127, "the stacksize lane 0->1");
		tCover.onToolClick((byte) 0, tWire.mCovers, AbstractCoverAttachmentLogistics.TOOL_CUTTER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(2, (tWire.mCovers.mValues[0] >> 2) & 127, "1->2");
		// the priority bits survive the cutter write (the masked :72 form)
		tWire.mCovers.value((byte) 0, (short) 3);
		tCover.onToolClick((byte) 0, tWire.mCovers, AbstractCoverAttachmentLogistics.TOOL_CUTTER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(3, tWire.mCovers.mValues[0] & 3, "the priority bits survive");
		assertEquals(1, (tWire.mCovers.mValues[0] >> 2) & 127, "the stacksize lane wrapped 64->1 over the priority write");
	}

	@Test
	public void dumpAndDisplayRefuseTheValueLanes() {
		GTLogisticsWireBlockEntity tDump = wireWith(CoverLogisticsGenericDump.INSTANCE, (byte) 0);
		CoverLogisticsGenericDump.INSTANCE.onToolClick((byte) 0, tDump.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(0, tDump.mCovers.mValues[0], "the dump has no priority lane (upstream :37)");

		GTLogisticsWireBlockEntity tDisplay = wireWith(CoverLogisticsDisplayCPULogic.INSTANCE, (byte) 0);
		long tDamage = CoverLogisticsDisplayCPULogic.INSTANCE.onToolClick((byte) 0, tDisplay.mCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 1, 0, 0, 0);
		assertEquals(0, tDamage, "the display answers 0 — no priority lane (upstream :54)");
		assertEquals(0, tDisplay.mCovers.mValues[0]);
	}

	// ------------------------------------------------------------------
	// the display lane arithmetic (upstream :301-318) + the redstone face
	// ------------------------------------------------------------------

	@Test
	public void displayArithmeticPins() {
		assertEquals(0, AbstractCoverLogisticsDisplay.displayVisual(0, 27), "unused");
		assertEquals(10, AbstractCoverLogisticsDisplay.displayVisual(27, 27), "saturated");
		assertEquals(1, AbstractCoverLogisticsDisplay.displayVisual(1, 27), "one op used of 27 ((27-1)*9/27=8 -> 9-8=1)");
				assertEquals(0, AbstractCoverLogisticsDisplay.displayValue(0, 27));
		assertEquals(15, AbstractCoverLogisticsDisplay.displayValue(27, 27));
		
	}

	@Test
	public void displayValueLaneIsTheRedstoneLevel() {
		GTLogisticsWireBlockEntity tDisplay = wireWith(CoverLogisticsDisplayCPULogic.INSTANCE, (byte) 0);
		tDisplay.mCovers.value((byte) 0, (short) 10);
		assertEquals(10, CoverLogisticsDisplayCPULogic.INSTANCE.getRedstoneOutStrong((byte) 0, tDisplay.mCovers, (byte) 0), "the value lane IS the redstone out");
		tDisplay.mCovers.value((byte) 0, (short) 200);
		assertEquals(15, CoverLogisticsDisplayCPULogic.INSTANCE.getRedstoneOutWeak((byte) 0, tDisplay.mCovers, (byte) 0), "bind4 clamps");
	}

	// ------------------------------------------------------------------
	// the filtered lane write/clear faces
	// ------------------------------------------------------------------

	@Test
	public void filteredItemBusFilterSetAndClear() {
		CoverLogisticsItemExport tCover = CoverLogisticsItemExport.INSTANCE;
		GTLogisticsWireBlockEntity tWire = wireWith(tCover, (byte) 0);
		assertNull(tCover.filterItemOf(tWire.mCovers, (byte) 0), "no filter initially");

		// the offline set arm — the pure lane writer (the player click needs the live entity system)
		tWire.mCovers.mNBTs[0] = gregtech6.covers.covers.CoverFilterItem.filterTagKeyOf(new ItemStack(Items.IRON_INGOT), tCover.filterKey);
		ItemStack tFilter = tCover.filterItemOf(tWire.mCovers, (byte) 0);
		assertNotNull(tFilter);
		assertEquals(Items.IRON_INGOT, tFilter.getItem());

		tCover.onToolClick((byte) 0, tWire.mCovers, AbstractCoverLogisticsFiltered.TOOL_SOFTHAMMER, 0, null, false, (byte) 1, 0, 0, 0); // :57
		assertNull(tCover.filterItemOf(tWire.mCovers, (byte) 0), "the softhammer clears the KEY");
	}

	@Test
	public void placementGateCoversTheWholeFamily() {
		// the family gate is the inherited :40 — every cover instance routes through it
		ICover[] tFamily = {
				CoverLogisticsDisplayCPULogic.INSTANCE, CoverLogisticsGenericDump.INSTANCE,
				CoverLogisticsItemExport.INSTANCE, CoverLogisticsGenericExport.INSTANCE,
				CoverLogisticsFluidExport.INSTANCE};
		for (ICover tCover : tFamily) {
			assertTrue(tCover.interceptCoverPlacement((byte) 0, new CoverData(bareOven()), null),
					tCover.getClass().getSimpleName() + " refuses a non-member host");
		}
	}

	// ------------------------------------------------------------------
	// the wire host composition (the p33 host face)
	// ------------------------------------------------------------------

	@Test
	public void wireHostCarriesCoversThroughNBT() {
		// the behaviour re-resolves through the registry item-id map on load — register the
		// real cover on a test item first (the GTCoverTestBase offline mount form)
		CoverRegistry.put(Items.GOLD_INGOT, CoverLogisticsItemStorage.INSTANCE);
		GTLogisticsWireBlockEntity tWire = wireWith(Items.GOLD_INGOT, CoverLogisticsItemStorage.INSTANCE, (byte) 3);
		tWire.mCovers.value((byte) 3, (short) 5); // priority 1 + stacksize 1

		CompoundTag tTag = tWire.saveWithoutMetadata(); // the 03 public save face (saveAdditional is protected)
		assertTrue(tTag.contains("covers"), "the covers tag rode the wire save");

		GTLogisticsWireBlockEntity tRestored = sWireType.create(WIRE_POS, Blocks.BRICKS.defaultBlockState());
		tRestored.load(tTag);
		assertNotNull(tRestored.getCovers(), "the store rehydrated");
		assertTrue(tRestored.getCovers().mBehaviours[3] instanceof CoverLogisticsItemStorage, "the behaviour re-resolved");
		assertEquals(5, tRestored.getCovers().mValues[3], "the value lane round-tripped");
	}

	@Test
	public void wireHostCanLogisticsUnaffectedByCovers() {
		GTLogisticsWireBlockEntity tWire = wireWith(CoverLogisticsItemExport.INSTANCE, (byte) 0);
		assertTrue(tWire.canLogistics((byte) 6), "the SIDE_ANY family answer stays");
		assertFalse(tWire.canLogistics((byte) 1), "an unconnected side still refuses (the :47 arm)");
	}
}
