package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverControllerAuto;
import gregtech6.covers.covers.CoverControllerAutoTimer;
import gregtech6.covers.covers.CoverControllerDisplay;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The controller-family acceptance tables (task p35-covers-display-scale-6) — the
 * automatic machine switch (:33-46) + the auto reboot switch (:34-56) + the machine
 * status display (:43-124), upstream CoverControllerAuto/CoverControllerAutoTimer/
 * CoverControllerDisplay as the mothers, on the real oven probe:
 *
 * <ul>
 * <li>the placement gates — the auto switch needs machine-form + switchable, the timer
 *     the base switchable gate, the display machine-form OR switchable; a plain
 *     coverable host refuses all three (the interop contrast);</li>
 * <li>the auto switch truth table (:44) — {@code possible || active} over the oven
 *     lanes, driven both ways through the base block-update arm, the removal release
 *     (:36-39) and the load re-derive (:41-44);</li>
 * <li>the timer clock (:48-55) — {@code active || timer % mTime >= mTime - 10}, the
 *     11-tick floor, the always-true mount answer, the five ladder durations;</li>
 * <li>the display build (:61-71) — the four capability markers + the four lamp bits,
 *     the empty-tickPre override (:56-58), the chisel style cycle (:47-53), the switch
 *     face (:74-83) both styles and both halves, the style base sprites (:85);</li>
 * <li>the attachment flags (:35-40) shared by the whole family.</li>
 * </ul>
 */
public class CoverDisplayScaleControllerTest extends GTCoverTestBase {

	private static final byte FACE = (byte) net.minecraft.core.Direction.UP.get3DDataValue(); // 1

	/** The real install path against a GIVEN oven (the truth tables mutate its lanes). */
	private static boolean installOn(TileEntityOvenCoverProbe aOven, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[FACE], aCover);
		return aOven.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true);
	}

	// ---------------------------------------------------------------------------
	// the placement gates — the family interop contrast
	// ---------------------------------------------------------------------------

	@Test
	public void placementGatesAcrossTheFamily() {
		// the oven is machine-form + switchable — all three admit (the real setCoverItem path)
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven instanceof ITileEntitySwitchableOnOff, "sanity: the oven IS switchable");
		assertTrue(installOn(tOven, new CoverControllerAuto()), ":34 — the oven admits the auto switch");
		tOven.setCoverItem(FACE, ItemStack.EMPTY, null, true, false);
		assertTrue(installOn(tOven, new CoverControllerAutoTimer(1200)), ":33 — the oven admits the timer");
		tOven.setCoverItem(FACE, ItemStack.EMPTY, null, true, false);
		assertTrue(installOn(tOven, new CoverControllerDisplay()), ":44 — the oven admits the display");
		tOven.setCoverItem(FACE, ItemStack.EMPTY, null, true, false);
		// a plain coverable (non machine-form, non-switchable) host refuses all three
		PlainProbe tPlain = new PlainProbe();
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true) && tPlain.getCovers() != null,
				"the plain host refuses every controller of this family");
		assertNull(tPlain.getCovers(), "the refused installs left no store behind");
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverControllerDisplay());
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true), ":44 — the plain host refuses the display");
		assertNull(tPlain.getCovers(), "no store behind the refused display");
	}

	/** A coverable non-machine host — the gate refusal subject (the P10 test's shape). */
	static class PlainProbe extends net.minecraft.world.level.block.entity.BlockEntity implements ICoverableTE {
		private CoverData mCovers;

		PlainProbe() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		@Override public CoverData getCovers() {return mCovers;}
		@Override public void setCovers(CoverData aData) {mCovers = aData;}
	}

	// ---------------------------------------------------------------------------
	// the auto switch — the :44 truth table + the base arms
	// ---------------------------------------------------------------------------

	@Test
	public void autoSwitchTruthTablePossibleOrActive() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerAuto()), "install accepted");
		// the mount arm re-derives: idle oven — possible=false, active=false -> OFF
		assertFalse(tOven.getStateOnOff(), ":44 idle — the mount stopped the machine");
		CoverControllerAuto tAuto = (CoverControllerAuto) tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		// a recipe becomes usable -> possible=true -> ON (the block-update arm drives it)
		tOven.mCouldUseRecipe = true;
		tData.onBlockUpdate();
		assertTrue(tOven.getStateOnOff(), ":44 — possible runs the machine");
		tOven.mCouldUseRecipe = false;
		tData.onBlockUpdate();
		assertFalse(tOven.getStateOnOff(), "the possible drop stops it again");
		// the active lane alone also runs it (the :44 second arm)
		tOven.mActive = true;
		tData.onBlockUpdate();
		assertTrue(tAuto.getStateOnOff(FACE, tData), ":44 — active runs the machine");
		// the max-progress half of the possible lane
		tOven.mActive = false;
		tOven.mMaxProgress = 100;
		assertTrue(tAuto.getStateOnOff(FACE, tData), ":1023 — mMaxProgress > 0 is possible");
		tOven.mMaxProgress = 0;
		assertFalse(tAuto.getStateOnOff(FACE, tData), "all quiet again");
	}

	@Test
	public void autoSwitchRemovalReleasesAndLoadReDerives() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerAuto()), "install accepted");
		assertFalse(tOven.getStateOnOff(), "the idle mount stopped the machine");
		assertTrue(tOven.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertTrue(tOven.getStateOnOff(), ":36-39 — the removal releases the machine to ON");
		// the load arm: save the mounted cover, rehydrate on a fresh oven — the
		// mCouldUseRecipe lane is a RUNTIME probe flag (not persisted), so the rehydrate
		// re-derives OFF from the fresh oven's quiet lanes and re-stops the machine
		TileEntityOvenCoverProbe tLive = bareOven();
		assertTrue(installOn(tLive, new CoverControllerAuto()), "install accepted");
		net.minecraft.nbt.CompoundTag tNBT = new net.minecraft.nbt.CompoundTag();
		tLive.writeCoversToNBT(tNBT);
		TileEntityOvenCoverProbe tRestored = leveledOven();
		tRestored.readCoversFromNBT(tNBT);
		assertFalse(tRestored.getStateOnOff(), ":41-44 — the rehydrated cover re-derived OFF from the quiet lanes");
		// the live lane flip then drives the rehydrated cover both ways
		tRestored.mCouldUseRecipe = true;
		tRestored.getCovers().onBlockUpdate();
		assertTrue(tRestored.getStateOnOff(), ":52-54 — the possible lane runs the rehydrated machine");
		tRestored.mCouldUseRecipe = false;
		tRestored.getCovers().onBlockUpdate();
		assertFalse(tRestored.getStateOnOff(), "and stops it again when the lane quiets");
	}

	// ---------------------------------------------------------------------------
	// the auto reboot switch — the timer clock
	// ---------------------------------------------------------------------------

	@Test
	public void autoTimerClockFormulaAndLadder() {
		// the five ladder durations, upstream MultiItemTechnological.java:68-72 verbatim
		assertEquals(5, CoverControllerAutoTimer.TIMER_TIMES.length);
		assertEquals(1200, CoverControllerAutoTimer.TIMER_TIMES[0]);
		assertEquals(36000, CoverControllerAutoTimer.TIMER_TIMES[4]);
		// the 11-tick floor (:39)
		assertEquals(11, new CoverControllerAutoTimer(0).mTime);
		assertEquals(11, new CoverControllerAutoTimer(5).mTime);
		assertEquals(1200, new CoverControllerAutoTimer(1200).mTime);
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerAutoTimer(1200)), "install accepted");
		CoverControllerAutoTimer tTimer = (CoverControllerAutoTimer) tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		// mid-cycle: not active, timer % mTime < mTime - 10 -> the machine is held OFF
		tOven.getCovers().tickPre(500, true, false, false);
		assertFalse(tOven.getStateOnOff(), ":49 mid-cycle — the idle machine is held OFF");
		// the reboot window: timer % mTime >= mTime - 10 -> the machine pulses ON
		tData.tickPre(1195, true, false, false);
		assertTrue(tOven.getStateOnOff(), ":49 — the 10-tick reboot window pulses ON");
		tData.tickPre(1199, true, false, false);
		assertTrue(tOven.getStateOnOff(), "the window end");
		tData.tickPre(1200, true, false, false);
		assertFalse(tOven.getStateOnOff(), "the next cycle holds OFF again");
		// an actively running machine is NEVER pulsed
		tOven.mActive = true;
		tData.tickPre(500, true, false, false);
		assertTrue(tOven.getStateOnOff(), ":49 — the running machine stays ON");
		tOven.mActive = false;
		// the client tick never drives
		tOven.mStopped = true;
		tData.tickPre(1195, false, false, false);
		assertFalse(tOven.getStateOnOff(), "the client-side tick is not the boss");
		// :53-55 — the mount/load answer is always true
		assertTrue(tTimer.getStateOnOff(FACE, tData), ":54 — the timer answers true");
		// the removal release (the controller base arm)
		assertTrue(tOven.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertTrue(tOven.getStateOnOff(), ":36-39 — the removal releases the machine");
	}

	// ---------------------------------------------------------------------------
	// the machine status display — the visual build, the chisel, the switch face
	// ---------------------------------------------------------------------------

	@Test
	public void displayVisualBuildMarksCapabilitiesAndLamps() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerDisplay()), "install accepted");
		CoverControllerDisplay tDisplay = (CoverControllerDisplay) tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		// the fresh oven: all four capability markers (32+64+128+256 = 480), no lamps —
		// the oven is idle (nothing possible/passive/active) and not stopped (ON lamp 8)
		tData.tickPost(10, true, false, false);
		assertEquals(480 | 8, tData.mVisuals[FACE] & 1023, ":61-71 — markers 480 + the ON lamp");
		// a usable recipe lights the possible lamp (B[0])
		tOven.mCouldUseRecipe = true;
		tData.tickPost(11, true, false, false);
		assertEquals(480 | 8 | 1, tData.mVisuals[FACE] & 1023, "the possible lamp lights");
		// the passive lamp rides mRunning (B[6] marker + B[1])
		tOven.mRunning = true;
		tData.tickPost(12, true, false, false);
		assertEquals(480 | 8 | 1 | 2, tData.mVisuals[FACE] & 1023, "the passive lamp lights");
		// the active lamp rides mActive (B[7] marker + B[2])
		tOven.mActive = true;
		tData.tickPost(13, true, false, false);
		assertEquals(480 | 8 | 1 | 2 | 4, tData.mVisuals[FACE] & 1023, "the active lamp lights");
		// the OFF machine clears the ON lamp (B[8] marker + B[3])
		tOven.mActive = false;
		tOven.mRunning = false;
		tOven.mCouldUseRecipe = false;
		tOven.mStopped = true;
		tData.tickPost(14, true, false, false);
		assertEquals(480, tData.mVisuals[FACE] & 1023, "the OFF machine clears every lamp");
		// :56-58 — the tickPre override never drives the machine (a poisoned latch survives)
		tOven.mStopped = true;
		tData.tickPre(15, true, false, false);
		assertFalse(tOven.getStateOnOff(), "the empty tickPre left the machine alone");
		assertTrue(tDisplay.needsVisualsSaved(FACE, tData), "the display base :30 — the lane saves");
	}

	@Test
	public void displayChiselCyclesTheStyleBits() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerDisplay()), "install accepted");
		ICover tDisplay = tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		assertEquals(0, tData.mVisuals[FACE] >>> 10, "the fresh mount carries style 0");
		assertEquals(100, tDisplay.onToolClick(FACE, tData, CoverControllerDisplay.TOOL_CHISEL, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":50 — the chisel damage");
		assertEquals(1, tData.mVisuals[FACE] >>> 10, ":49 — the style bits cycled to 1");
		assertEquals(100, tDisplay.onToolClick(FACE, tData, CoverControllerDisplay.TOOL_CHISEL, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":50 again");
		assertEquals(0, tData.mVisuals[FACE] >>> 10, "the style cycled back (mod 2)");
		// non-chisel ids answer 0
		assertEquals(0, tDisplay.onToolClick(FACE, tData, ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "the display does not answer the screwdriver");
	}

	@Test
	public void displaySwitchFaceTogglesByStyleAndHalf() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerDisplay()), "install accepted");
		CoverControllerDisplay tDisplay = (CoverControllerDisplay) tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		// style 0 (bottom art): the TOP QUARTER strip (y >= PX_N[4]=0.75) is the switch —
		// side 1 (UP) maps (aHitX, aHitZ) to the art coords (the :1737 row)
		assertTrue(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.9F, 0.9F), ":78 — the strip click (y >= 0.75) toggles");
		assertFalse(tOven.getStateOnOff(), "the toggle stopped the machine");
		assertTrue(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.9F, 0.9F), "the second click toggles back");
		assertTrue(tOven.getStateOnOff(), "the machine runs again");
		// BELOW the strip the bottom art is NOT the switch — y 0.5 is the readout half,
		// upstream :78 (y >= PX_N[4]) does not answer it
		assertFalse(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.5F, 0.5F), ":78 — y 0.5 < 0.75 falls through");
		assertFalse(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.1F, 0.1F), ":78 — y < 0.25 falls through");
		// the left side is NOT the switch either
		assertFalse(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.3F, 0.9F, 0.9F), ":78 — x < 0.625 falls through");
		// style 1 (top art): the BOTTOM QUARTER strip (y <= PX_P[4]=0.25) is the switch
		tData.mVisuals[FACE] = (short) ((tData.mVisuals[FACE] & 1023) | (1 << 10));
		assertTrue(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.1F, 0.1F), ":79 — the strip click (y <= 0.25) toggles on style 1");
		assertFalse(tOven.getStateOnOff(), "the style-1 toggle stopped the machine");
		assertFalse(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.9F, 0.9F), ":79 — y 0.9 > 0.25 falls through on style 1");
		assertFalse(tDisplay.onCoverClickedRight(FACE, tData, null, FACE, 0.8F, 0.5F, 0.5F), ":79 — y 0.5 > 0.25 falls through on style 1");
		// a click on a DIFFERENT side never toggles
		tData.mVisuals[FACE] = 0;
		assertFalse(tDisplay.onCoverClickedRight(FACE, tData, null, (byte) 0, 0.8F, 0.5F, 0.5F), ":75 — aSide != aSideClicked falls through");
	}

	@Test
	public void displayTexturePicksTheStyleBase() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverControllerDisplay()), "install accepted");
		ICover tDisplay = tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		assertEquals("gt6:block/status_display/bottom/base", tDisplay.getCoverTextureSurface(FACE, tData).toString(), ":116 — style 0 is the bottom base");
		tData.mVisuals[FACE] = (short) (1 << 10);
		assertEquals("gt6:block/status_display/top/base", tDisplay.getCoverTextureSurface(FACE, tData).toString(), ":117 — style 1 is the top base");
		tData.mVisuals[FACE] = (short) ((2 << 10) | 5); // mod 2 wraps to style 0, low bits ride along
		assertEquals("gt6:block/status_display/bottom/base", tDisplay.getCoverTextureSurface(FACE, tData).toString(), ":85 — the style folds mod 2");
	}
}
