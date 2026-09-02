package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.AbstractCoverDefault;
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerCovers;

/**
 * The cover controller acceptance tables (task p11-cover-controllers). Offline and
 * exhaustive, upstream CoverControllerCovers.java:39-108 as the mother:
 *
 * <ul>
 * <li>the EQUALITY truth table (:106-108) — {@code bind1(feed) == bit0} over the
 *     full feed scale x both polarity bits, the exact inverse of the machine-
 *     controller inequality (the anti-cross cross-claim runs against
 *     {@link CoverControllerAutoRedstone} too);</li>
 * <li>the three arms (:41-55) driving {@link CoverData#setStopped} — the
 *     block-wide COVER stop flag (not the machine's setStateOnOff) — on both
 *     mount edges and on the server tick, with the onStoppedUpdate broadcast
 *     reaching the other covers on the block;</li>
 * <li>the cross-face relay (:58-91) — clicks and tool clicks on the controller's
 *     face resolve the nine-grid region through {@code UT6.getSideWrenching} and
 *     forward to the cover on THAT face; the self-face/centre case runs the own
 *     screwdriver toggle (which re-derives the stop flag, :80); a foreign
 *     aSideClicked or a coverless target face relays nothing;</li>
 * <li>the OPOS anti-flip — the incoming read is the cover face's neighbour, never
 *     the opposite face's (the P9 redstone-hooks precedent);</li>
 * <li>no placement gate (the plain attachment family — every coverable host
 *     admits it) and the attachment flags + sprite (:35-40/:104).</li>
 * </ul>
 */
public class CoverControllerCoversTest extends GTCoverTestBase {

	private static final byte FACE = (byte) Direction.UP.get3DDataValue(); // 1
	/** The west face — getSideWrenching(UP, 0.1, 0.5, 0.5) resolves there (hitX < 0.25, hitZ mid). */
	private static final byte WEST = (byte) Direction.WEST.get3DDataValue(); // 4

	/** The relay subject — a plain cover that records every forwarded interaction. */
	static class RelaySpy extends AbstractCoverDefault {
		int mRight, mLeft, mTool, mStoppedUpdates, mRemoved;
		byte mLastToolSide = -1, mLastToolClicked = -1, mLastClickSide = -1, mLastClickClicked = -1;

		@Override public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
			mRight++;
			mLastClickSide = aCoverSide;
			mLastClickClicked = aSideClicked;
			return true;
		}

		@Override public boolean onCoverClickedLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
			mLeft++;
			mLastClickSide = aCoverSide;
			mLastClickClicked = aSideClicked;
			return true;
		}

		@Override public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
			mTool++;
			mLastToolSide = aCoverSide;
			mLastToolClicked = aSideClicked;
			return 12345;
		}

		@Override public void onStoppedUpdate(byte aCoverSide, CoverData aData, boolean aStopped) {
			mStoppedUpdates++;
		}

		@Override public void onCoverRemove(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
			super.onCoverRemove(aCoverSide, aData, aPlayer);
			mRemoved++;
		}
	}

	/** The oven probe on a feed-driven stub level (the P10 switch-probe shape). */
	static class ControllerProbe extends TileEntityOvenCoverProbe {
		final CoverRedstoneConductorTest.FeedLevel mSwitchLevel = new CoverRedstoneConductorTest.FeedLevel();

		ControllerProbe() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		ControllerProbe leveled() {
			setLevel(mSwitchLevel);
			return this;
		}

		void feed(int aSignal) {
			mSwitchLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(FACE)), aSignal);
		}
	}

	/** Installs aCover on the controller's own face (the real placement path). */
	private static boolean install(TileEntityOvenCoverProbe aOven, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[FACE], aCover);
		return aOven.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true);
	}

	/** Installs the spy on the WEST face, west of the controller face. */
	private static RelaySpy installSpy(ControllerProbe aOven) {
		RelaySpy tSpy = new RelaySpy();
		CoverRegistry.put(COVER_ITEMS[WEST], tSpy);
		aOven.setCoverItem(WEST, new ItemStack(COVER_ITEMS[WEST]), null, false, true);
		return tSpy;
	}

	// ---------------------------------------------------------------------------
	// the EQUALITY truth table (:106-108) + the anti-cross claim
	// ---------------------------------------------------------------------------

	@Test
	public void equalityTruthTableIsTheExactInverseOfTheMachineControllers() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		CoverControllerCovers tController = (CoverControllerCovers) tOven.getCovers().mBehaviours[FACE];
		CoverControllerAutoRedstone tMachineSwitch = new CoverControllerAutoRedstone();
		CoverData tData = tOven.getCovers();
		for (int tFeed : new int[] {0, 1, 3, 15}) {
			tOven.feed(tFeed);
			for (int tBit = 0; tBit < 2; tBit++) {
				tData.mValues[FACE] = (short) tBit;
				boolean tEq = (tFeed != 0 ? 1 : 0) == tBit;
				assertEquals(tEq, tController.getStateOnOff(FACE, tData), "feed " + tFeed + " bit " + tBit + ": bind1(feed) == bit");
				// the machine-controller family answers the SAME store row with the
				// inequality (the anti-cross claim, on the exact same inputs)
				assertEquals(!tEq, tMachineSwitch.getStateOnOff(FACE, tData), "feed " + tFeed + " bit " + tBit + ": the machine switch answers the inverse");
			}
		}
		// the machine lane must NOT leak into the cover controller's answer (the two
		// families sit on different semantic axes: setStateOnOff vs setStopped)
		tOven.mActive = true;
		tOven.mSuccessful = false;
		tOven.feed(0);
		tData.mValues[FACE] = 0;
		assertTrue(tController.getStateOnOff(FACE, tData), "the cover controller ignores the machine state (quiet face, bit clear → stopped)");
	}

	// ---------------------------------------------------------------------------
	// the three arms (:41-55) drive CoverData.setStopped
	// ---------------------------------------------------------------------------

	@Test
	public void mountArmDerivesTheStopFlagFromTheFace() {
		// quiet face, bit clear: covers work when Input is ON — no input → STOPPED
		ControllerProbe tQuiet = new ControllerProbe().leveled();
		assertTrue(install(tQuiet, new CoverControllerCovers()), "install accepted");
		assertTrue(tQuiet.getCovers().mStopped, ":47-50 — a quiet face stops the covers on this block");
		// loud face, bit clear: covers work → the stop flag clears
		ControllerProbe tLoud = new ControllerProbe().leveled();
		tLoud.feed(15);
		assertTrue(install(tLoud, new CoverControllerCovers()), "install accepted");
		assertFalse(tLoud.getCovers().mStopped, "a loud face lets the covers work");
	}

	@Test
	public void tickArmPollsOnTheServerOnlyAndBroadcastsTheChange() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		RelaySpy tSpy = installSpy(tOven);
		CoverData tData = tOven.getCovers();
		assertTrue(tData.mStopped, "quiet face — the mount stopped the covers");
		int tBefore = tSpy.mStoppedUpdates;
		// the signal arrives: the server poll clears the flag (covers work again)
		tOven.feed(15);
		tData.tickPre(10, true, false, false);
		assertFalse(tData.mStopped, ":53-55 — the server poll re-derived the flag from the loud face");
		assertEquals(tBefore + 1, tSpy.mStoppedUpdates, "the change broadcast reached the neighbour cover once");
		// no change → no broadcast (setStopped is a no-op on the current state)
		tData.tickPre(11, true, false, false);
		assertEquals(tBefore + 1, tSpy.mStoppedUpdates, "an unchanged flag does not re-broadcast");
		// the client arm never drives
		tOven.feed(0);
		tData.tickPre(12, false, false, false);
		assertFalse(tData.mStopped, "the client-side tick does not touch the stop flag");
		// the signal leaves — the next server poll stops the covers again
		tData.tickPre(13, true, false, false);
		assertTrue(tData.mStopped, "the poll stops the covers once the face goes quiet");
		assertEquals(tBefore + 2, tSpy.mStoppedUpdates, "the second change broadcast reached the neighbour cover");
	}

	@Test
	public void removalArmReDerivesTheFlagOnTheWayOut() {
		// loud face (covers work, flag clear): poison the flag away from the formula,
		// then dismantle — the removal arm (:41-44) re-derives the formula value and
		// the correction broadcasts once to the neighbour cover (the store itself is
		// dissolved by the dismantle, the broadcast is the observable)
		ControllerProbe tOven = new ControllerProbe().leveled();
		tOven.feed(15);
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		RelaySpy tSpy = installSpy(tOven);
		assertFalse(tOven.getCovers().mStopped, "sanity: loud face — covers work");
		tOven.getCovers().setStopped(true); // the poison (its own broadcast lands before `before`)
		int tBefore = tSpy.mStoppedUpdates;
		assertTrue(tOven.setCoverItem(FACE, ItemStack.EMPTY, null, false, true), "dismantle accepted");
		assertEquals(tBefore + 1, tSpy.mStoppedUpdates, ":41-44 — the removal arm re-derived the flag and broadcast the correction");
	}

	// ---------------------------------------------------------------------------
	// the cross-face relay (:58-91)
	// ---------------------------------------------------------------------------

	@Test
	public void toolClickOnTheNineGridRegionRelaysToThatFaceCover() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		RelaySpy tSpy = installSpy(tOven);
		// hitX < 0.25 with a mid hitZ on the UP face → the WEST face (the parse table)
		assertEquals(WEST, gregtech6.util.UT6.getSideWrenching(FACE, 0.1F, 0.5F, 0.5F), "the nine-grid parse lands on WEST");
		assertEquals(12345, tOven.getCovers().mBehaviours[FACE].onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 7, null, false, FACE, 0.1F, 0.5F, 0.5F), ":89 — the tool click forwards the spy's answer");
		assertEquals(1, tSpy.mTool, "exactly one relayed tool click");
		assertEquals(WEST, tSpy.mLastToolSide, ":89 — the spy is addressed as the WEST cover");
		assertEquals(FACE, tSpy.mLastToolClicked, ":89 — aSideClicked stays the clicked (UP) face");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the relay did NOT touch the controller's own value lane");
	}

	@Test
	public void clicksOnTheNineGridRegionRelayToThatFaceCover() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		RelaySpy tSpy = installSpy(tOven);
		assertTrue(tOven.getCovers().mBehaviours[FACE].onCoverClickedRight(FACE, tOven.getCovers(), null, FACE, 0.1F, 0.5F, 0.5F), ":61 — the right click forwards the spy's true");
		assertEquals(1, tSpy.mRight, "exactly one relayed right click");
		assertEquals(WEST, tSpy.mLastClickSide, ":61 — the spy is addressed as the WEST cover");
		assertEquals(FACE, tSpy.mLastClickClicked, ":61 — aSideClicked stays the clicked face");
		assertTrue(tOven.getCovers().mBehaviours[FACE].onCoverClickedLeft(FACE, tOven.getCovers(), null, FACE, 0.1F, 0.5F, 0.5F), ":69 — the left click forwards the spy's true");
		assertEquals(1, tSpy.mLeft, "exactly one relayed left click");
	}

	@Test
	public void selfFaceCentreClickRunsTheOwnToggleNotTheRelay() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		RelaySpy tSpy = installSpy(tOven);
		// centre hit → getSideWrenching resolves to the controller's own face
		assertEquals(FACE, gregtech6.util.UT6.getSideWrenching(FACE, 0.5F, 0.5F, 0.5F), "the centre hit is the self face");
		// quiet face + fresh mount: the flag sits stopped; the toggle flips to
		// "Covers work when Input is OFF" — the quiet face then WORKS the covers
		assertTrue(tOven.getCovers().mStopped, "sanity: quiet + bit clear → stopped");
		assertEquals(1000, tOven.getCovers().mBehaviours[FACE].onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":81 — the toggle damage");
		assertEquals(1, tOven.getCovers().mValues[FACE] & 1, ":78 — bit 0 set");
		assertFalse(tOven.getCovers().mStopped, ":80 — the toggle re-derived the flag (quiet face + inverse arm → covers work)");
		assertEquals(0, tSpy.mTool, "the self-face tool click never reached the spy");
		// toggle back: the quiet face stops the covers again
		assertEquals(1000, tOven.getCovers().mBehaviours[FACE].onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":81 again");
		assertTrue(tOven.getCovers().mStopped, ":80 — back to the default arm, quiet → stopped");
		// non-screwdriver self-face clicks answer 0 (the magnifyingglass cut)
		assertEquals(0, tOven.getCovers().mBehaviours[FACE].onToolClick(FACE, tOven.getCovers(), "magnifyingglass", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "the magnifyingglass arm is cut");
	}

	@Test
	public void foreignFaceClicksAndCoverlessTargetsRelayNothing() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		ICover tController = tOven.getCovers().mBehaviours[FACE];
		// a coverless target face: the nine-grid region lands on WEST, nothing there
		assertEquals(0, tController.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.1F, 0.5F, 0.5F), ":90 — a coverless target answers 0");
		assertFalse(tController.onCoverClickedRight(FACE, tOven.getCovers(), null, FACE, 0.1F, 0.5F, 0.5F), ":62 — a coverless target answers false");
		// a foreign aSideClicked (the DOWN face, no controller there): the tool click
		// takes the :76 conservative SELF-face branch (`aSideClicked != aCoverSide`)
		// — the own screwdriver toggle runs, never a relay; the click relays
		// short-circuit false (the click pair has no own-face behaviour)
		assertEquals(1000, tController.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) Direction.DOWN.get3DDataValue(), 0.1F, 0.5F, 0.5F), ":77-81 — the foreign face takes the own-toggle branch");
		assertEquals(1, tOven.getCovers().mValues[FACE] & 1, "the own toggle ran on the foreign-face click");
		assertEquals(1000, tController.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) Direction.DOWN.get3DDataValue(), 0.1F, 0.5F, 0.5F), "toggling back through the same branch");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the lane is back to clear");
		assertFalse(tController.onCoverClickedLeft(FACE, tOven.getCovers(), null, (byte) Direction.DOWN.get3DDataValue(), 0.1F, 0.5F, 0.5F), ":68 — the left click short-circuits false");
		// now install the spy and re-check: the foreign face still relays nothing
		RelaySpy tSpy = installSpy(tOven);
		assertEquals(1000, tController.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) Direction.DOWN.get3DDataValue(), 0.1F, 0.5F, 0.5F), "the foreign face keeps taking the own-toggle branch");
		tController.onToolClick(FACE, tOven.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) Direction.DOWN.get3DDataValue(), 0.1F, 0.5F, 0.5F);
		assertEquals(0, tSpy.mTool, "the spy saw nothing");
		assertEquals(0, tOven.getCovers().mValues[FACE] & 1, "the lane ends clear");
	}

	// ---------------------------------------------------------------------------
	// the OPOS anti-flip: the incoming read is THIS face's neighbour
	// ---------------------------------------------------------------------------

	@Test
	public void incomingReadIsTheCoverFaceNotTheOppositeFace() {
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(install(tOven, new CoverControllerCovers()), "install accepted");
		CoverData tData = tOven.getCovers();
		tData.mValues[FACE] = 0;
		// signal on the OPPOSITE face's neighbour (DOWN for an UP cover) must NOT read
		tOven.mSwitchLevel.mFeeds.put(COVER_POS.relative(Direction.DOWN), 15);
		assertEquals(0, tOven.getRedstoneIncoming(FACE), "the OPOS feed is invisible to the UP face");
		assertTrue(((CoverControllerCovers) tData.mBehaviours[FACE]).getStateOnOff(FACE, tData), "no signal → the equality holds → the covers stay STOPPED");
		// signal on the COVER face's neighbour reads
		tOven.mSwitchLevel.mFeeds.put(COVER_POS.relative(Direction.UP), 15);
		assertEquals(15, tOven.getRedstoneIncoming(FACE), "the cover-face feed reads");
		assertFalse(((CoverControllerCovers) tData.mBehaviours[FACE]).getStateOnOff(FACE, tData), "signal → the equality breaks → the covers work");
	}

	// ---------------------------------------------------------------------------
	// no placement gate + the attachment flags + the sprite (:104)
	// ---------------------------------------------------------------------------

	@Test
	public void everyCoverableHostAdmitsItAndFlagsAndSprite() {
		CoverControllerCovers tController = new CoverControllerCovers();
		ControllerProbe tOven = new ControllerProbe().leveled();
		assertTrue(tController.interceptCoverPlacement(FACE, tOven.getCovers(), null) == false, "the attachment family has NO placement gate — the default admits");
		assertTrue(install(tOven, tController), "the install lands");
		ICover tCover = tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		assertFalse(tCover.interceptClickLeft(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":37 — the left click falls through");
		assertFalse(tCover.interceptClickRight(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":38 — the right click falls through");
		assertFalse(tCover.isOpaque(FACE, tData), ":39 — the plate is non-opaque");
		assertFalse(tCover.isSealable(FACE, tData), ":40 — the plate is non-sealable");
		assertEquals("gt6:block/cover_switch/circuit", tCover.getCoverTextureSurface(FACE, tData).toString(), ":104 — the cover controller art");
		assertEquals(tCover.getCoverTextureSurface(FACE, tData), tCover.getCoverTextureAttachment(FACE, tData, FACE));
		assertEquals(tCover.getCoverTextureSurface(FACE, tData), tCover.getCoverTextureHolder(FACE, tData, FACE));
	}
}
