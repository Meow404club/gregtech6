package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import org.junit.jupiter.api.Test;

import gregtech6.block.GTOvenBlock;
import gregtech6.covers.covers.CoverControllerRedstone;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The redstone machine switch acceptance tables (task p10-cover-controller-redstone).
 * Offline and exhaustive, upstream AbstractCoverAttachmentController.java:32-62 +
 * CoverControllerRedstone.java:39-70 as the mother:
 *
 * <ul>
 * <li>the five inlined arms — the placement gate refuses non-switchable hosts (:33),
 *     the removal arm resets the machine to ON (:36-39), the mount and load arms
 *     re-derive the state from the face (:41-49), the block-update arm drives both
 *     ways (:52-54, through the conductor card's GTOvenBlock dispatch seam) and the
 *     tick arm polls server-side only (:57-59);</li>
 * <li>the polarity truth table (:68-70) — bit 0 clear = runs while the face sees
 *     signal, bit 0 set (the screwdriver toggle) = the inverse 有信号停/无信号跑;
 *     bind1 clamps the whole 0..15 scale onto the 0/1 logic level;</li>
 * <li>the screwdriver toggle (:42-46) — bit 0 flips, 1000 tool damage, non-screwdriver
 *     ids answered with 0 (the magnifyingglass/host-relay cut declaration);</li>
 * <li>the oven latch idempotence — setStateOnOff is a no-op on the current state, so
 *     the poll arms never flap;</li>
 * <li>the attachment flag inlining + the sprite mapping (:35-40/:65).</li>
 * </ul>
 */
public class CoverControllerRedstoneTest extends GTCoverTestBase {

	private static final byte FACE = (byte) Direction.UP.get3DDataValue(); // 1

	/** A coverable but NOT switchable host — the placement gate's refusal subject. */
	static class PlainCoverableBE extends BlockEntity implements ICoverableTE {
		private @Nullable CoverData mCovers;

		PlainCoverableBE() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		@Override public CoverData getCovers() {return mCovers;}
		@Override public void setCovers(@Nullable CoverData aData) {mCovers = aData;}
	}

	/** An oven probe on a feed-driven stub level (the conductor pair's FeedLevel fixture). */
	static class SwitchProbe extends TileEntityOvenCoverProbe {
		final CoverRedstoneConductorTest.FeedLevel mSwitchLevel = new CoverRedstoneConductorTest.FeedLevel();

		SwitchProbe() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		SwitchProbe leveled() {
			setLevel(mSwitchLevel);
			return this;
		}

		void feed(int aSignal) {
			mSwitchLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(FACE)), aSignal);
		}
	}

	/** The real install path (force = F — the placement gate is part of the arms under test). */
	private static boolean install(TileEntityOvenCoverProbe aOven, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[FACE], aCover);
		return aOven.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true);
	}

	/** A fake BlockGetter holding exactly one block entity (the conductor-test dispatch fixture). */
	private static BlockGetter fakeLevel(BlockEntity aBE) {
		return new BlockGetter() {
			@Override public BlockState getBlockState(BlockPos aPos) {return Blocks.BRICKS.defaultBlockState();}
			@Override public FluidState getFluidState(BlockPos aPos) {return Fluids.EMPTY.defaultFluidState();}
			@Override public BlockEntity getBlockEntity(BlockPos aPos) {return aBE;}
			@Override public int getHeight() {return 384;}
			@Override public int getMinBuildHeight() {return -64;}
			@Override public int getMaxBuildHeight() {return 320;}
		};
	}

	// ---------------------------------------------------------------------------
	// arm 1 — the placement gate (:33)
	// ---------------------------------------------------------------------------

	@Test
	public void placementGateRefusesNonSwitchableHosts() {
		CoverControllerRedstone tSwitch = new CoverControllerRedstone();
		// the oven IS switchable (the one-line implements is what unlocked the gate)
		TileEntityOvenCoverProbe tOven = new SwitchProbe().leveled();
		assertTrue(tOven instanceof ITileEntitySwitchableOnOff, "the oven carries the switchable interface");
		assertTrue(install(tOven, tSwitch), "the install lands on the oven");
		assertFalse(tSwitch.interceptCoverPlacement(FACE, tOven.getCovers(), null), ":33 — a switchable host admits the switch");
		// a coverable-but-plain host refuses, store stays dissolved
		PlainCoverableBE tPlain = new PlainCoverableBE();
		CoverRegistry.put(COVER_ITEMS[FACE], tSwitch);
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true),
				":33 — a non-switchable host refuses the switch");
		assertNull(tPlain.getCovers(), "the refused install left no store behind");
	}

	// ---------------------------------------------------------------------------
	// arm 3b — the mount sync (:46-49)
	// ---------------------------------------------------------------------------

	@Test
	public void placedArmDerivesTheStateFromTheFace() {
		// quiet face: the mount stops the machine (default polarity — runs only on signal)
		SwitchProbe tQuiet = new SwitchProbe().leveled();
		assertTrue(install(tQuiet, new CoverControllerRedstone()), "install accepted");
		assertFalse(tQuiet.getStateOnOff(), ":46-49 — no signal on a bit-0-clear mount means OFF");
		assertTrue(tQuiet.mStopped, "the manual latch holds the machine stopped");
		// loud face: the mount runs the machine
		SwitchProbe tLoud = new SwitchProbe().leveled();
		tLoud.feed(15);
		assertTrue(install(tLoud, new CoverControllerRedstone()), "install accepted");
		assertTrue(tLoud.getStateOnOff(), ":46-49 — signal on a bit-0-clear mount means ON");
	}

	// ---------------------------------------------------------------------------
	// arm 3a — the load sync (:41-44, the readCoversFromNBT rehydrate path)
	// ---------------------------------------------------------------------------

	@Test
	public void loadArmReDerivesTheStateAndKeepsThePolarityBit() {
		// live: loud face, polarity toggled to the inverse arm (bit 0 set), then saved
		SwitchProbe tLive = new SwitchProbe().leveled();
		tLive.feed(15);
		assertTrue(install(tLive, new CoverControllerRedstone()), "install accepted");
		assertTrue(tLive.getStateOnOff(), "sanity: runs on signal with the default polarity");
		assertEquals(1000, tLive.getCovers().mBehaviours[FACE].onToolClick(FACE, tLive.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":45 — the toggle damage");
		tLive.getCovers().onBlockUpdate();
		assertFalse(tLive.getStateOnOff(), "sanity: the set bit inverts to stop-on-signal");
		CompoundTag tNBT = new CompoundTag();
		tLive.writeCoversToNBT(tNBT);
		// restart: a fresh oven rehydrates — the load arm re-derives the state AND the bit
		SwitchProbe tRestored = new SwitchProbe().leveled();
		tRestored.feed(15);
		tRestored.readCoversFromNBT(tNBT);
		assertEquals(1, tRestored.getCovers().mValues[FACE] & 1, "the polarity bit survives the save");
		assertFalse(tRestored.getStateOnOff(), ":41-44 — the rehydrated cover re-stopped the loud machine (set bit)");
		// and with the feed quiet the same store runs the machine
		tRestored.feed(0);
		tRestored.getCovers().onBlockUpdate();
		assertTrue(tRestored.getStateOnOff(), "the persisted inverse polarity runs the quiet machine");
	}

	// ---------------------------------------------------------------------------
	// arm 4 — the block-update drive (:52-54, both ways, through the dispatch seam)
	// ---------------------------------------------------------------------------

	@Test
	public void blockUpdateArmDrivesBothWaysThroughTheDispatchSeam() {
		SwitchProbe tOven = new SwitchProbe().leveled();
		assertTrue(install(tOven, new CoverControllerRedstone()), "install accepted");
		assertFalse(tOven.getStateOnOff(), "quiet face — stopped");
		// the signal arrives: the neighbour change rides the conductor card's seam
		tOven.feed(15);
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tOven), COVER_POS);
		assertTrue(tOven.getStateOnOff(), ":52-54 — the seam delivered the update and the machine runs");
		// the signal leaves: same seam, other direction
		tOven.feed(0);
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tOven), COVER_POS);
		assertFalse(tOven.getStateOnOff(), "the seam delivers the stop as well");
		// the direct CoverData dispatch (a coverless neighbour update shape) is equivalent
		tOven.feed(15);
		assertTrue(tOven.getCovers().onBlockUpdate(), "the CoverData dispatch returns true");
		assertTrue(tOven.getStateOnOff(), "the direct dispatch drives the same arm");
	}

	// ---------------------------------------------------------------------------
	// arm 5 — the server tick poll (:57-59, server-side only)
	// ---------------------------------------------------------------------------

	@Test
	public void tickArmPollsOnTheServerOnly() {
		SwitchProbe tOven = new SwitchProbe().leveled();
		assertTrue(install(tOven, new CoverControllerRedstone()), "install accepted");
		tOven.feed(15);
		// a stale stopped state heals on the next server tick
		tOven.mStopped = true;
		tOven.getCovers().tickPre(10, true, false, false);
		assertTrue(tOven.getStateOnOff(), ":57-59 — the server poll re-derived ON from the loud face");
		// the client arm never drives: a poisoned state survives a client-side tick
		tOven.mStopped = true;
		tOven.getCovers().tickPre(11, false, false, false);
		assertFalse(tOven.getStateOnOff(), "the client-side tick is not the boss of the machine state");
		// the feed drops — the next server poll stops the machine
		tOven.feed(0);
		tOven.getCovers().tickPre(12, true, false, false);
		assertFalse(tOven.getStateOnOff(), "the poll stops the machine once the face goes quiet");
	}

	// ---------------------------------------------------------------------------
	// arm 2 — the removal reset (:36-39)
	// ---------------------------------------------------------------------------

	@Test
	public void removalArmResetsTheMachineToOnRegardlessOfTheFace() {
		// the loud case: stopped by the persisted inverse arm, the removal frees the machine
		SwitchProbe tLoud = new SwitchProbe().leveled();
		tLoud.feed(15);
		assertTrue(install(tLoud, new CoverControllerRedstone()), "install accepted");
		assertEquals(1000, tLoud.getCovers().mBehaviours[FACE].onToolClick(FACE, tLoud.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":45");
		tLoud.getCovers().onBlockUpdate();
		assertFalse(tLoud.getStateOnOff(), "sanity: stop-on-signal arm engaged");
		assertTrue(tLoud.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertTrue(tLoud.getStateOnOff(), ":36-39 — the removal resets the machine to ON");
		// the quiet case: the machine sits stopped, the removal still frees it
		SwitchProbe tQuiet = new SwitchProbe().leveled();
		assertTrue(install(tQuiet, new CoverControllerRedstone()), "install accepted");
		assertFalse(tQuiet.getStateOnOff(), "sanity: quiet face — stopped");
		assertTrue(tQuiet.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertTrue(tQuiet.getStateOnOff(), ":36-39 — the quiet removal resets to ON as well");
	}

	// ---------------------------------------------------------------------------
	// the polarity truth table (:68-70) — the whole 0..15 scale folds onto 0/1
	// ---------------------------------------------------------------------------

	@Test
	public void polarityTruthTableBothArms() {
		SwitchProbe tOven = new SwitchProbe().leveled();
		assertTrue(install(tOven, new CoverControllerRedstone()), "install accepted");
		CoverControllerRedstone tSwitch = (CoverControllerRedstone) tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		for (int tFeed : new int[] {0, 1, 3, 15}) {
			tOven.feed(tFeed); // the face feed under this row
			// bit 0 CLEAR — the fresh-mount arm: runs while the face sees signal
			tData.mValues[FACE] = 0;
			assertEquals(tFeed != 0 ? 1 : 0, tSwitch.getStateOnOff(FACE, tData) ? 1 : 0,
					"feed " + tFeed + ", bit clear: getStateOnOff == (feed != 0)");
			// bit 0 SET — the screwdriver arm: 有信号停/无信号跑
			tData.mValues[FACE] = 1;
			assertEquals(tFeed == 0 ? 1 : 0, tSwitch.getStateOnOff(FACE, tData) ? 1 : 0,
					"feed " + tFeed + ", bit set: getStateOnOff == (feed == 0)");
		}
		// bind1 folds the scale — feeds 1, 3 and 15 are the same answer (the table above
		// already pins it); the machine side rides the idempotent latch, no flapping
		tData.mValues[FACE] = 0;
		tOven.feed(3);
		tOven.getCovers().onBlockUpdate();
		assertTrue(tOven.getStateOnOff(), "feed 3 runs the machine");
		tOven.getCovers().onBlockUpdate();
		assertTrue(tOven.getStateOnOff(), "the re-poll is idempotent (setStateOnOff is a no-op on the current state)");
	}

	// ---------------------------------------------------------------------------
	// the screwdriver toggle (:42-46) + the cut arms
	// ---------------------------------------------------------------------------

	@Test
	public void screwdriverToggleFlipsThePolarityBitAndDamageIs1000() {
		SwitchProbe tOven = new SwitchProbe().leveled();
		assertTrue(install(tOven, new CoverControllerRedstone()), "install accepted");
		ICover tSwitch = tOven.getCovers().mBehaviours[FACE];
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the fresh mount carries a clear bit");
		assertEquals(1000, tSwitch.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":45 — the toggle damage");
		assertEquals(1, tOven.getCovers().mValues[FACE] & 1, ":43 — bit 0 set");
		assertEquals(1000, tSwitch.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":45 again");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, ":43 — bit 0 cleared back");
		// non-screwdriver ids answer 0 and never touch the lane (the cut-arms declaration)
		assertEquals(0, tSwitch.onToolClick(FACE, tOven.getCovers(), "cutter", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "the emitter's cutter id is NOT answered");
		assertEquals(0, tSwitch.onToolClick(FACE, tOven.getCovers(), "magnifyingglass", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "the magnifyingglass arm is cut");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the refused tools left the lane alone");
	}

	// ---------------------------------------------------------------------------
	// the inlined attachment flags (:35-40) + the sprite mapping (:65)
	// ---------------------------------------------------------------------------

	@Test
	public void attachmentFlagsAndSpriteMapping() {
		SwitchProbe tOven = new SwitchProbe().leveled();
		assertTrue(install(tOven, new CoverControllerRedstone()), "install accepted");
		ICover tSwitch = tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		assertFalse(tSwitch.interceptClickLeft(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":37 — the left click falls through to the host");
		assertFalse(tSwitch.interceptClickRight(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":38 — the right click falls through to the host GUI");
		assertFalse(tSwitch.isOpaque(FACE, tData), ":39 — the plate is non-opaque");
		assertFalse(tSwitch.isSealable(FACE, tData), ":40 — the plate is non-sealable");
		assertFalse(tSwitch.needsVisualsSaved(FACE, tData), "the visuals lane is unused — the polarity rides the value lane");
		assertEquals("gt6:block/redstone_switch/circuit", tSwitch.getCoverTextureSurface(FACE, tData).toString(), ":65 — the switch art");
		// the attachment/holder faces fold into the same sprite (AbstractCoverDefault :71-72)
		assertEquals(tSwitch.getCoverTextureSurface(FACE, tData), tSwitch.getCoverTextureAttachment(FACE, tData, FACE));
		assertEquals(tSwitch.getCoverTextureSurface(FACE, tData), tSwitch.getCoverTextureHolder(FACE, tData, FACE));
	}
}
