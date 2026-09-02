package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverShutter;

/**
 * The shutter + item filter covers (task p11-cover-shutter-filter) — the offline
 * half of the acceptance: the shutter's four-intercept truth table (upstream
 * CoverShutter :82-85, closed = {@code (visual == 0) == mStopped}) across both
 * directions and the fluid pair, the filter's whitelist/blacklist/empty/stopped
 * truth table (CoverFilterItem :115-127) with the NBT-insensitive match, the filter
 * lane round-trip through the CoverData {@code s}-{@code x} compounds, and the
 * eight-hook dispatch face through the host gates AND the side-aware oven decorator
 * (the p10 framework the two covers consume). The player-held right-click arm of
 * the filter set (:89-112) needs the live entity system — its pure core
 * ({@link CoverFilterItem#filterTagFor}) and the null-player skip are covered here;
 * the held-stack wiring is runtime/RCON surface.
 */
public class CoverShutterFilterTest extends GTCoverTestBase {

	/** Installs the given cover behaviour on the face (re-binds the fixture item first-wins). */
	static void mount(TileEntityOvenCoverProbe aOven, byte aSide, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[aSide], aCover);
		assertTrue(aOven.setCoverItem(aSide, new ItemStack(COVER_ITEMS[aSide]), null, false, true), "the fixture cover installs");
		assertTrue(aOven.isCovered(aSide), "the face carries the cover under test");
	}

	static ItemStack stack(net.minecraft.world.item.Item aItem, int aCount) {
		return new ItemStack(aItem, aCount);
	}

	// ---------------------------------------------------------------------------
	// shutter — the :82-85 closed truth table, both item directions
	// ---------------------------------------------------------------------------

	@Test
	void shutterTruthTableOnBothDirections() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new CoverShutter());

		// normal plate (visual 0), covers running: open
		assertTrue(tOven.canInsertItem((byte) 1, 0, stack(Items.IRON_INGOT, 8)), ":82 — (0==0)==false → the insert passes");
		assertTrue(tOven.canExtractItem((byte) 1, 0, stack(Items.IRON_INGOT, 1)), ":83 — the extract passes");

		// a controller stops the covers: the normal plate closes
		tOven.getCovers().setStopped(true);
		assertFalse(tOven.canInsertItem((byte) 1, 0, stack(Items.IRON_INGOT, 8)), ":82 — (0==0)==true → the insert is refused");
		assertFalse(tOven.canExtractItem((byte) 1, 0, stack(Items.IRON_INGOT, 1)), ":83 — the extract is refused");

		// the screwdriver flips to the inverted plate: closed while running, open while stopped
		tOven.getCovers().setStopped(false);
		assertEquals(1000L, tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 1, false), ":62 — the tool damage");
		assertEquals(1, tOven.getCovers().mVisuals[1], ":53 — the visual lane flipped to inverted");
		assertFalse(tOven.canInsertItem((byte) 1, 0, stack(Items.IRON_INGOT, 8)), ":82 — (0==1)==false → inverted refuses while running");
		assertFalse(tOven.canExtractItem((byte) 1, 0, stack(Items.IRON_INGOT, 1)), ":83 — same for the extract");
		tOven.getCovers().setStopped(true);
		assertTrue(tOven.canInsertItem((byte) 1, 0, stack(Items.IRON_INGOT, 8)), ":82 — (0==1)==true → the inverted plate opens on the stop");
		assertTrue(tOven.canExtractItem((byte) 1, 0, stack(Items.IRON_INGOT, 1)), ":83 — same for the extract");
	}

	@Test
	void shutterGatesItsOwnFaceOnly() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 0, new CoverShutter());
		tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 0, false); // inverted → closed while running

		for (byte tSide = 1; tSide < 6; tSide++) {
			assertTrue(tOven.canInsertItem(tSide, 0, stack(Items.IRON_INGOT, 8)), "face " + tSide + " passes: the DOWN shutter never gates it");
			assertTrue(tOven.canExtractItem(tSide, 0, stack(Items.IRON_INGOT, 1)));
		}
		assertFalse(tOven.canInsertItem((byte) 0, 0, stack(Items.IRON_INGOT, 8)), "the covered face stays closed");
	}

	@Test
	void shutterFluidPairFollowsTheSameTruthTable() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 2, new CoverShutter());
		FluidStack tWater = new FluidStack(Fluids.WATER, 1000);

		assertFalse(tOven.interceptFluidFill((byte) 2, tWater), ":84 — normal plate, running → the fill passes");
		assertFalse(tOven.interceptFluidDrain((byte) 2, tWater), ":85 — the drain passes");

		tOven.getCovers().setStopped(true);
		assertTrue(tOven.interceptFluidFill((byte) 2, tWater), ":84 — the closed gate refuses the fill");
		assertTrue(tOven.interceptFluidDrain((byte) 2, tWater), ":85 — the closed gate refuses the drain");

		tOven.getCovers().setStopped(false);
		tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 2, false); // inverted
		assertTrue(tOven.interceptFluidFill((byte) 2, tWater), ":84 — inverted refuses while running");
		assertTrue(tOven.interceptFluidDrain((byte) 2, tWater), ":85 — same for the drain");
	}

	@Test
	void shutterVisualsSurviveTheSave() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new CoverShutter());
		tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 1, false); // visual 1
		assertTrue(tOven.getCovers().mVisuals[1] != 0, "the inverted state is on the lane");

		// the needsVisualsSaved gate: the visual persists even on the visuals-stripped save
		CompoundTag tStripped = tOven.getCovers().writeToNBT(new CompoundTag(), false);
		assertEquals(1, tStripped.getShort(CoverData.VISUAL_KEYS[1]), "needsVisualsSaved=T keeps the state on the :87 gate");

		TileEntityOvenCoverProbe tReloaded = leveledOven();
		tReloaded.setCovers(CoverRegistry.coverdata(tReloaded, tStripped));
		assertTrue(tReloaded.isCovered((byte) 1), "the behaviour rehydrates from the id lane");
		assertTrue(tReloaded.getCovers().mBehaviours[1] instanceof CoverShutter, "the shutter rebinds");
		assertEquals(1, tReloaded.getCovers().mVisuals[1], "the inverted state round-trips");
		assertFalse(tReloaded.canInsertItem((byte) 1, 0, stack(Items.IRON_INGOT, 8)), "the rehydrated gate still refuses while running");
	}

	// ---------------------------------------------------------------------------
	// filter — the :115-127 whitelist/blacklist/empty/stopped truth table
	// ---------------------------------------------------------------------------

	@Test
	void filterEmptyFilterTruthTable() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new CoverFilterItem());
		ItemStack tAnything = stack(Items.IRON_INGOT, 8);

		// whitelist mode (visual 0) with an EMPTY filter: refuse everything
		assertNull(tOven.getCovers().mNBTs[1], "the fresh filter carries no lane data");
		assertFalse(tOven.canInsertItem((byte) 1, 0, tAnything), ":118 — an empty whitelist refuses everything");
		assertFalse(tOven.canExtractItem((byte) 1, 0, tAnything), ":125 — same for the extract");

		// blacklist mode (visual 1) with an EMPTY filter: admit everything
		assertEquals(1000L, tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 1, false));
		assertEquals(1, tOven.getCovers().mVisuals[1], "the mode flipped to blacklist");
		assertTrue(tOven.canInsertItem((byte) 1, 0, tAnything), ":118 — an empty blacklist admits everything");
		assertTrue(tOven.canExtractItem((byte) 1, 0, tAnything), ":125 — same for the extract");

		// a stopped controller refuses everything BEFORE the polarity (:117/:124)
		tOven.getCovers().setStopped(true);
		assertFalse(tOven.canInsertItem((byte) 1, 0, tAnything), ":117 — the stop precedes the blacklist admit");
		assertFalse(tOven.canExtractItem((byte) 1, 0, tAnything), ":124 — same for the extract");
	}

	@Test
	void filterSetFilterGatesByPolarityOnBothDirections() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 3, new CoverFilterItem());
		tOven.getCovers().mNBTs[3] = CoverFilterItem.filterTagFor(stack(Items.IRON_INGOT, 1)); // the whitelist item

		// whitelist: the filter item passes, everything else is refused (insert and extract)
		assertTrue(tOven.canInsertItem((byte) 3, 0, stack(Items.IRON_INGOT, 8)), ":119 — the whitelist admits its item");
		assertFalse(tOven.canInsertItem((byte) 3, 0, stack(Items.SAND, 8)), ":119 — the whitelist refuses the rest");
		assertTrue(tOven.canExtractItem((byte) 3, 0, stack(Items.IRON_INGOT, 1)), ":126 — the whitelist admits its item");
		assertFalse(tOven.canExtractItem((byte) 3, 0, stack(Items.SAND, 1)), ":126 — the whitelist refuses the rest");

		// blacklist: exactly inverted
		assertEquals(1000L, tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 3, false));
		assertFalse(tOven.canInsertItem((byte) 3, 0, stack(Items.IRON_INGOT, 8)), ":119 — the blacklist refuses its item");
		assertTrue(tOven.canInsertItem((byte) 3, 0, stack(Items.SAND, 8)), ":119 — the blacklist admits the rest");
		assertFalse(tOven.canExtractItem((byte) 3, 0, stack(Items.IRON_INGOT, 1)), ":126 — the blacklist refuses its item");
		assertTrue(tOven.canExtractItem((byte) 3, 0, stack(Items.SAND, 1)), ":126 — the blacklist admits the rest");
	}

	@Test
	void filterMatchIsNbtAndCountInsensitive() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 2, new CoverFilterItem());
		tOven.getCovers().mNBTs[2] = CoverFilterItem.filterTagFor(stack(Items.IRON_INGOT, 1));

		// NOT NBT sensitive (upstream ST.equal(filter, stack, T)): the display-name
		// component noise on the offered stack never changes the verdict
		ItemStack tTagged = stack(Items.IRON_INGOT, 8);
		tTagged.getOrCreateTag().putBoolean("gt6_test_marker", true);
		assertTrue(CoverFilterItem.matches(tOven.getCovers(), (byte) 2, tTagged), "the match ignores the stack tag");
		// count-insensitive: a 1-count filter matches a 64-count offer
		assertTrue(CoverFilterItem.matches(tOven.getCovers(), (byte) 2, stack(Items.IRON_INGOT, 64)));
		assertFalse(CoverFilterItem.matches(tOven.getCovers(), (byte) 2, stack(Items.GOLD_INGOT, 1)), "a different item still refuses");
	}

	@Test
	void filterScrewdriverAndSoftHammerLanes() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 4, new CoverFilterItem());
		tOven.getCovers().mNBTs[4] = CoverFilterItem.filterTagFor(stack(Items.IRON_INGOT, 1));

		// the soft hammer clears the filter (upstream :63-66) — the mode stays, the item goes
		assertEquals(10000L, tOven.onCoverToolClick(CoverFilterItem.TOOL_SOFTHAMMER, null, ItemStack.EMPTY, (byte) 4, false));
		assertFalse(tOven.getCovers().mNBTs[4].contains(CoverFilterItem.FILTER_KEY, Tag.TAG_COMPOUND), ":64 — the filter key is gone, the mode lane survives");
		assertFalse(tOven.canInsertItem((byte) 4, 0, stack(Items.IRON_INGOT, 1)), "back to the empty-whitelist refuse-all");
		// clearing an empty lane is a no-op that still costs the hammer
		tOven.getCovers().mNBTs[4] = null;
		assertEquals(10000L, tOven.onCoverToolClick(CoverFilterItem.TOOL_SOFTHAMMER, null, ItemStack.EMPTY, (byte) 4, false), "the null lane never NPEs");
	}

	@Test
	void filterLaneRoundTripsThroughTheCoverDataNBT() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 0, new CoverFilterItem());
		tOven.getCovers().mNBTs[0] = CoverFilterItem.filterTagFor(stack(Items.SAND, 1));
		tOven.getCovers().mVisuals[0] = 1; // blacklist

		CompoundTag tSaved = tOven.getCovers().writeToNBT(new CompoundTag(), true);
		assertTrue(tSaved.getCompound(CoverData.NBT_KEYS[0]).contains(CoverFilterItem.FILTER_KEY, Tag.TAG_COMPOUND),
				"the gt.filter.item payload rides the s-x lane compound (:86)");
		assertEquals(1, tSaved.getShort(CoverData.VISUAL_KEYS[0]), "the blacklist mode rides the :87 gate");

		TileEntityOvenCoverProbe tReloaded = leveledOven();
		tReloaded.setCovers(CoverRegistry.coverdata(tReloaded, tSaved));
		assertTrue(tReloaded.getCovers().mBehaviours[0] instanceof CoverFilterItem, "the filter rebinds");
		assertTrue(tReloaded.canInsertItem((byte) 0, 0, stack(Items.GRAVEL, 4)), "the rehydrated blacklist admits the rest");
		assertFalse(tReloaded.canInsertItem((byte) 0, 0, stack(Items.SAND, 4)), "the rehydrated blacklist refuses its item");
	}

	@Test
	void filterRightClickArmSkipsWithoutAPlayerAndConsumesTheClick() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 5, new CoverFilterItem());
		// the null-player call (the offline/RCON double) consumes the click but writes nothing
		assertTrue(tOven.getCovers().mBehaviours[5].onCoverClickedRight((byte) 5, tOven.getCovers(), null, (byte) 5, 0.5F, 0.5F, 0.5F),
				":111 — the click is consumed unconditionally");
		assertNull(tOven.getCovers().mNBTs[5], ":90 — no player, no lane write");

		// the pure core of the set: the tag shape under the verbatim upstream key
		CompoundTag tLane = CoverFilterItem.filterTagFor(stack(Items.IRON_INGOT, 64));
		assertTrue(tLane.contains(CoverFilterItem.FILTER_KEY, Tag.TAG_COMPOUND), "the upstream key gt.filter.item");
		ItemStack tSaved = ItemStack.of(tLane.getCompound(CoverFilterItem.FILTER_KEY));
		assertEquals(Items.IRON_INGOT, tSaved.getItem(), "the identity write (the meta cycle is the declared deviation collapse)");
		assertEquals(1, tSaved.getCount(), "a 1-count filter stack (upstream ST.make(item, 1, meta))");
	}

	// ---------------------------------------------------------------------------
	// the eight-hook dispatch face — the host gates AND the side-aware decorator
	// ---------------------------------------------------------------------------

	@Test
	void wrapperRefusesThroughAClosedShutterAndPassesThroughAnOpenOne() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new CoverShutter()); // visual 0, running → OPEN

		IItemHandler tUp = tOven.newCoverGatedHandler(Direction.UP);
		assertTrue(tUp.insertItem(0, stack(Items.IRON_INGOT, 8), false).isEmpty(), "the open shutter passes the insert");
		assertEquals(8, tOven.getInventory().getStackInSlot(0).getCount(), "the loop closes through the decorator");

		tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 1, false); // inverted → CLOSED while running
		assertEquals(4, tUp.insertItem(0, stack(Items.IRON_INGOT, 4), false).getCount(), "the closed shutter returns the offered stack whole");
		assertEquals(8, tOven.getInventory().getStackInSlot(0).getCount(), "the gate fired before the inner inventory");
		assertTrue(tUp.extractItem(0, 8, false).isEmpty(), "the closed shutter seals the extract");

		// the neighbour face without a cover stays transferable (the side-aware decorator keys on the face)
		IItemHandler tNorth = tOven.newCoverGatedHandler(Direction.NORTH);
		assertTrue(tNorth.insertItem(1, stack(Items.GOLD_INGOT, 2), false).isEmpty(), "face A's shutter never gates face B");
	}

	@Test
	void wrapperRunsTheFilterVerdictThroughTheDecorator() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		mount(tOven, (byte) 1, new CoverFilterItem()); // whitelist, empty

		IItemHandler tUp = tOven.newCoverGatedHandler(Direction.UP);
		assertEquals(4, tUp.insertItem(0, stack(Items.IRON_INGOT, 4), false).getCount(), "the empty whitelist refuses the insert at the decorator");
		assertTrue(tOven.getInventory().getStackInSlot(0).isEmpty());

		tOven.getCovers().mNBTs[1] = CoverFilterItem.filterTagFor(stack(Items.IRON_INGOT, 1));
		assertTrue(tUp.insertItem(0, stack(Items.IRON_INGOT, 4), false).isEmpty(), "the whitelist lets its item through the decorator");
		assertEquals(4, tOven.getInventory().getStackInSlot(0).getCount());
		assertEquals(4, tUp.extractItem(0, 4, false).getCount(), "the whitelist lets its item back out");
		assertEquals(4, tUp.insertItem(0, stack(Items.SAND, 4), false).getCount(), "a non-filter item is refused");

		tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, (byte) 1, false); // blacklist
		assertTrue(tUp.insertItem(0, stack(Items.SAND, 4), false).isEmpty(), "the blacklist admits the rest");
		assertEquals(4, tUp.insertItem(0, stack(Items.IRON_INGOT, 4), false).getCount(), "the blacklist refuses its item");
	}
}
