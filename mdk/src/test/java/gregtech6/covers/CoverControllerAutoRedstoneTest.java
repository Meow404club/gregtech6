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
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerCovers;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The auto redstone machine switch acceptance tables (task p11-cover-controllers).
 * Offline and exhaustive, upstream AbstractCoverAttachmentController.java:32-62 +
 * CoverControllerAutoRedstone.java:41-72 as the mother:
 *
 * <ul>
 * <li>the five inlined arms — the same shape as the P10
 *     {@link CoverControllerRedstoneTest} tables, re-run against the auto switch;</li>
 * <li>the auto-arm truth table (:70-72) — the FULL 4x2x2 cross of
 *     {@code (mActive, mSuccessful)} x {@code bit0} x {@code feed}: the auto arm
 *     {@code active && !successful} holds the machine ON through a signal drop
 *     ("lets it finish"), the redstone arm {@code bind1(feed) != bit0} is the P10
 *     inequality and must stay intact for the three non-auto machine states;</li>
 * <li>the anti-cross claim — the same (feed, bit0) row answered by the cover
 *     controller's EQUALITY (:106-108) is the exact inverse of the P10 inequality
 *     wherever the auto arm is quiet (the two formulas must never swap);</li>
 * <li>the OPOS anti-flip — {@code getRedstoneIncoming(UP)} reads the UP neighbour
 *     (AbstractCoverDefault :78 {@code pos.relative(face)}), never the opposite
 *     face's neighbour (the P9 redstone-hooks precedent);</li>
 * <li>the screwdriver toggle (:44-48), the cut arms and the attachment flags.</li>
 * </ul>
 */
public class CoverControllerAutoRedstoneTest extends GTCoverTestBase {

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

	/** An oven probe on a feed-driven stub level (the P10 switch-probe shape). */
	static class AutoProbe extends TileEntityOvenCoverProbe {
		final CoverRedstoneConductorTest.FeedLevel mSwitchLevel = new CoverRedstoneConductorTest.FeedLevel();

		AutoProbe() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		AutoProbe leveled() {
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

	/** A fake BlockGetter holding exactly one block entity (the P10 dispatch fixture). */
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
	// arm 1 — the placement gate (AbstractCoverAttachmentController :33)
	// ---------------------------------------------------------------------------

	@Test
	public void placementGateRefusesNonSwitchableHosts() {
		CoverControllerAutoRedstone tSwitch = new CoverControllerAutoRedstone();
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(tOven instanceof ITileEntitySwitchableOnOff, "the oven carries the switchable interface");
		assertTrue(install(tOven, tSwitch), "the install lands on the oven");
		assertFalse(tSwitch.interceptCoverPlacement(FACE, tOven.getCovers(), null), ":33 — a switchable host admits the switch");
		PlainCoverableBE tPlain = new PlainCoverableBE();
		CoverRegistry.put(COVER_ITEMS[FACE], tSwitch);
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true),
				":33 — a non-switchable host refuses the switch");
		assertNull(tPlain.getCovers(), "the refused install left no store behind");
	}

	// ---------------------------------------------------------------------------
	// the auto-arm truth table (:70-72) — the full 4x2x2 cross
	// ---------------------------------------------------------------------------

	@Test
	public void autoArmTruthTableFourMachineStatesBothPolarities() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		CoverControllerAutoRedstone tSwitch = (CoverControllerAutoRedstone) tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		for (int tActive = 0; tActive < 2; tActive++) {
			for (int tSuccessful = 0; tSuccessful < 2; tSuccessful++) {
				// the machine-state lane is the cover's read-only consumption (the
				// declared mapping of MultiTileEntityBasicMachine.java:1025-1026)
				tOven.mActive = tActive != 0;
				tOven.mSuccessful = tSuccessful != 0;
				boolean tAuto = tActive != 0 && tSuccessful == 0;
				for (int tFeed : new int[] {0, 15}) {
					tOven.feed(tFeed);
					for (int tBit = 0; tBit < 2; tBit++) {
						tData.mValues[FACE] = (short) tBit;
						boolean tExpected = tAuto || ((tFeed != 0 ? 1 : 0) != tBit);
						assertEquals(tExpected, tSwitch.getStateOnOff(FACE, tData),
								"active=" + tActive + " successful=" + tSuccessful + " feed=" + tFeed + " bit=" + tBit
										+ ": (active&&!successful) || (feed != bit)");
					}
				}
			}
		}
	}

	/**
	 * The auto arm's exact sentence: a machine actively running WITHOUT having
	 * produced stays ON through a quiet face — on BOTH polarities — and the same
	 * arm goes quiet the moment the process produces (or never ran), handing the
	 * decision back to the redstone arm (the P10 inequality).
	 */
	@Test
	public void autoArmHoldsOnThroughTheSignalDropAndHandsBack() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		CoverData tData = tOven.getCovers();
		// quiet face, mid-process: the auto arm runs the machine on the default polarity
		tOven.feed(0);
		tData.mValues[FACE] = 0;
		tOven.mActive = true;
		tOven.mSuccessful = false;
		assertTrue(((CoverControllerAutoRedstone) tOven.getCovers().mBehaviours[FACE]).getStateOnOff(FACE, tData), ":71 — mid-process holds ON (lets it finish)");
		tOven.getCovers().tickPre(1, true, false, false);
		assertTrue(tOven.getStateOnOff(), "the poll arm delivered the hold");
		// ...and on the inverse polarity (the auto arm ORs, it does not care about the bit)
		tData.mValues[FACE] = 1;
		tOven.getCovers().tickPre(2, true, false, false);
		assertTrue(tOven.getStateOnOff(), "the hold survives the polarity toggle");
		// the process produced: the auto arm goes quiet — and with the bit CLEARED
		// again the quiet face's redstone arm takes over and stops the machine
		tData.mValues[FACE] = 0;
		tOven.mSuccessful = true;
		tOven.getCovers().tickPre(3, true, false, false);
		assertFalse(tOven.getStateOnOff(), ":71 — produced, the redstone arm takes over (bit clear: stop on quiet)");
		// machine inactive again: plain redstone-switch semantics
		tOven.mActive = false;
		tOven.mSuccessful = false;
		tOven.getCovers().tickPre(4, true, false, false);
		assertFalse(tOven.getStateOnOff(), "inactive — the quiet face keeps the machine stopped");
		// signal arrives: runs again (the P10 arm, untouched)
		tOven.feed(15);
		tOven.getCovers().tickPre(5, true, false, false);
		assertTrue(tOven.getStateOnOff(), "the signal runs the stopped machine");
	}

	// ---------------------------------------------------------------------------
	// the redstone arm keeps the P10 inequality — and the anti-cross claim
	// ---------------------------------------------------------------------------

	@Test
	public void redstoneArmStaysTheP10InequalityAndNeverSwapsWithTheCoverController() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		CoverControllerAutoRedstone tAuto = (CoverControllerAutoRedstone) tOven.getCovers().mBehaviours[FACE];
		// the anti-cross subjects: the cover controller's EQUALITY lives on the SAME store
		CoverControllerCovers tCovers = new CoverControllerCovers();
		CoverData tData = tOven.getCovers();
		tOven.mActive = false; // the auto arm quiet — the two formulas face off raw
		for (int tFeed : new int[] {0, 1, 3, 15}) {
			tOven.feed(tFeed);
			for (int tBit = 0; tBit < 2; tBit++) {
				tData.mValues[FACE] = (short) tBit;
				boolean tIneq = (tFeed != 0 ? 1 : 0) != tBit;
				boolean tEq = (tFeed != 0 ? 1 : 0) == tBit;
				assertEquals(tIneq, tAuto.getStateOnOff(FACE, tData), "feed " + tFeed + " bit " + tBit + ": auto switch == P10 inequality");
				assertEquals(tEq, tCovers.getStateOnOff(FACE, tData), "feed " + tFeed + " bit " + tBit + ": cover controller == equality");
				// the exact-inverse cross-claim (trivially the four feed x bit rows)
				assertEquals(!tIneq, tEq, "feed " + tFeed + " bit " + tBit + ": equality == !inequality");
			}
		}
	}

	// ---------------------------------------------------------------------------
	// arm 3 — the mount sync reads the machine state too (:46-49)
	// ---------------------------------------------------------------------------

	@Test
	public void placedArmDerivesFromFaceAndMachineState() {
		// mid-process on a quiet face: the mount itself holds the machine ON
		AutoProbe tMid = new AutoProbe().leveled();
		tMid.mActive = true;
		tMid.mSuccessful = false;
		assertTrue(install(tMid, new CoverControllerAutoRedstone()), "install accepted");
		assertTrue(tMid.getStateOnOff(), ":46-49 — a mid-process quiet mount is ON");
		// an idle machine on a quiet face: the fresh mount stops it (the P10 default)
		AutoProbe tIdle = new AutoProbe().leveled();
		assertTrue(install(tIdle, new CoverControllerAutoRedstone()), "install accepted");
		assertFalse(tIdle.getStateOnOff(), "an idle quiet mount is OFF");
		// a loud face runs the idle machine (the P10 default, regression-kept)
		AutoProbe tLoud = new AutoProbe().leveled();
		tLoud.feed(15);
		assertTrue(install(tLoud, new CoverControllerAutoRedstone()), "install accepted");
		assertTrue(tLoud.getStateOnOff(), "signal on a bit-0-clear mount means ON");
	}

	// ---------------------------------------------------------------------------
	// arm 3a — the load sync re-derives from the persisted polarity + machine state
	// ---------------------------------------------------------------------------

	@Test
	public void loadArmReDerivesAndThePolarityBitSurvives() {
		AutoProbe tLive = new AutoProbe().leveled();
		tLive.feed(15);
		assertTrue(install(tLive, new CoverControllerAutoRedstone()), "install accepted");
		assertTrue(tLive.getStateOnOff(), "sanity: runs on signal with the default polarity");
		assertEquals(1000, tLive.getCovers().mBehaviours[FACE].onToolClick(FACE, tLive.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":47 — the toggle damage");
		tLive.getCovers().onBlockUpdate();
		assertFalse(tLive.getStateOnOff(), "sanity: the set bit inverts to stop-on-signal");
		CompoundTag tNBT = new CompoundTag();
		tLive.writeCoversToNBT(tNBT);
		// restart: a fresh oven rehydrates — the auto arm keeps the mid-process hold
		AutoProbe tRestored = new AutoProbe().leveled();
		tRestored.mActive = true;
		tRestored.mSuccessful = false;
		tRestored.readCoversFromNBT(tNBT);
		assertEquals(1, tRestored.getCovers().mValues[FACE] & 1, "the polarity bit survives the save");
		assertTrue(tRestored.getStateOnOff(), ":41-44 — the rehydrated cover holds the mid-process machine ON (auto arm)");
		// process ends: the auto arm folds away and the saved INVERSE bit re-takes
		// the machine — the loud face stops it, the quiet face would run it again
		tRestored.mActive = false;
		tRestored.feed(15);
		tRestored.getCovers().onBlockUpdate();
		assertFalse(tRestored.getStateOnOff(), "the auto arm handed back: set bit + loud face → stop");
		tRestored.feed(0);
		tRestored.getCovers().onBlockUpdate();
		assertTrue(tRestored.getStateOnOff(), "the persisted inverse bit runs the quiet machine (the P10 arm, regression)");
	}

	// ---------------------------------------------------------------------------
	// arm 4 — the block-update drive (:52-54, the P10 seam)
	// ---------------------------------------------------------------------------

	@Test
	public void blockUpdateArmDrivesBothWaysThroughTheDispatchSeam() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		assertFalse(tOven.getStateOnOff(), "quiet face — stopped");
		tOven.feed(15);
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tOven), COVER_POS);
		assertTrue(tOven.getStateOnOff(), ":52-54 — the seam delivered the update and the machine runs");
		tOven.feed(0);
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tOven), COVER_POS);
		assertFalse(tOven.getStateOnOff(), "the seam delivers the stop as well");
		tOven.feed(15);
		assertTrue(tOven.getCovers().onBlockUpdate(), "the CoverData dispatch returns true");
		assertTrue(tOven.getStateOnOff(), "the direct dispatch drives the same arm");
	}

	// ---------------------------------------------------------------------------
	// arm 5 — the server tick poll is the auto arm's pulse (:57-59)
	// ---------------------------------------------------------------------------

	@Test
	public void tickArmPollsOnTheServerOnly() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		tOven.mActive = true;
		tOven.mSuccessful = false;
		tOven.feed(0);
		// a stale stopped state heals on the next server tick (the auto arm, quiet face)
		tOven.mStopped = true;
		tOven.getCovers().tickPre(10, true, false, false);
		assertTrue(tOven.getStateOnOff(), ":57-59 — the server poll re-derived ON from the mid-process state");
		// the client arm never drives
		tOven.mStopped = true;
		tOven.getCovers().tickPre(11, false, false, false);
		assertFalse(tOven.getStateOnOff(), "the client-side tick is not the boss of the machine state");
		// the process ends — the next server poll stops the machine
		tOven.mActive = false;
		tOven.getCovers().tickPre(12, true, false, false);
		assertFalse(tOven.getStateOnOff(), "the poll stops the machine once the process ended");
	}

	// ---------------------------------------------------------------------------
	// arm 2 — the removal reset (:36-39)
	// ---------------------------------------------------------------------------

	@Test
	public void removalArmResetsTheMachineToOnRegardless() {
		// the mid-process hold: the removal still frees the machine
		AutoProbe tMid = new AutoProbe().leveled();
		tMid.mActive = true;
		tMid.mSuccessful = false;
		assertTrue(install(tMid, new CoverControllerAutoRedstone()), "install accepted");
		assertTrue(tMid.getStateOnOff(), "sanity: the mid-process hold");
		assertTrue(tMid.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertTrue(tMid.getStateOnOff(), ":36-39 — the removal resets the machine to ON");
		// the quiet case as well
		AutoProbe tQuiet = new AutoProbe().leveled();
		assertTrue(install(tQuiet, new CoverControllerAutoRedstone()), "install accepted");
		assertFalse(tQuiet.getStateOnOff(), "sanity: quiet face — stopped");
		assertTrue(tQuiet.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertTrue(tQuiet.getStateOnOff(), ":36-39 — the quiet removal resets to ON as well");
	}

	// ---------------------------------------------------------------------------
	// the screwdriver toggle (:44-48) + the cut arms
	// ---------------------------------------------------------------------------

	@Test
	public void screwdriverToggleFlipsThePolarityBitAndDamageIs1000() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		ICover tSwitch = tOven.getCovers().mBehaviours[FACE];
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the fresh mount carries a clear bit");
		assertEquals(1000, tSwitch.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":47 — the toggle damage");
		assertEquals(1, tOven.getCovers().mValues[FACE] & 1, ":45 — bit 0 set");
		assertEquals(1000, tSwitch.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":47 again");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, ":45 — bit 0 cleared back");
		assertEquals(0, tSwitch.onToolClick(FACE, tOven.getCovers(), "cutter", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "non-screwdriver ids answer 0");
		assertEquals(0, tSwitch.onToolClick(FACE, tOven.getCovers(), "magnifyingglass", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "the magnifyingglass arm is cut");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the refused tools left the lane alone");
	}

	// ---------------------------------------------------------------------------
	// the OPOS anti-flip: the incoming read is THIS face's neighbour, never OPOS
	// ---------------------------------------------------------------------------

	@Test
	public void incomingReadIsTheCoverFaceNotTheOppositeFace() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		CoverData tData = tOven.getCovers();
		tData.mValues[FACE] = 0;
		tOven.mActive = false;
		// signal on the OPPOSITE face's neighbour (DOWN for an UP cover) must NOT read
		tOven.mSwitchLevel.mFeeds.put(COVER_POS.relative(Direction.DOWN), 15);
		assertEquals(0, tOven.getRedstoneIncoming(FACE), "the OPOS feed is invisible to the UP face");
		assertFalse(((CoverControllerAutoRedstone) tOven.getCovers().mBehaviours[FACE]).getStateOnOff(FACE, tData), "no signal means OFF (bit clear)");
		// signal on the COVER face's neighbour reads
		tOven.mSwitchLevel.mFeeds.put(COVER_POS.relative(Direction.UP), 15);
		assertEquals(15, tOven.getRedstoneIncoming(FACE), "the cover-face feed reads");
		assertTrue(((CoverControllerAutoRedstone) tOven.getCovers().mBehaviours[FACE]).getStateOnOff(FACE, tData), "signal means ON (bit clear)");
	}

	// ---------------------------------------------------------------------------
	// the inlined attachment flags (:35-40) + the sprite mapping (:67)
	// ---------------------------------------------------------------------------

	@Test
	public void attachmentFlagsAndSpriteMapping() {
		AutoProbe tOven = new AutoProbe().leveled();
		assertTrue(install(tOven, new CoverControllerAutoRedstone()), "install accepted");
		ICover tSwitch = tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		assertFalse(tSwitch.interceptClickLeft(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":37 — the left click falls through");
		assertFalse(tSwitch.interceptClickRight(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":38 — the right click falls through");
		assertFalse(tSwitch.isOpaque(FACE, tData), ":39 — the plate is non-opaque");
		assertFalse(tSwitch.isSealable(FACE, tData), ":40 — the plate is non-sealable");
		assertEquals("gt6:block/auto_redstone_switch/circuit", tSwitch.getCoverTextureSurface(FACE, tData).toString(), ":67 — the auto switch art");
		assertEquals(tSwitch.getCoverTextureSurface(FACE, tData), tSwitch.getCoverTextureAttachment(FACE, tData, FACE));
		assertEquals(tSwitch.getCoverTextureSurface(FACE, tData), tSwitch.getCoverTextureHolder(FACE, tData, FACE));
	}
}
