package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import org.junit.jupiter.api.Test;

import gregtech6.block.GTOvenBlock;
import gregtech6.covers.covers.CoverRedstoneConductorIN;
import gregtech6.covers.covers.CoverRedstoneConductorOUT;

/**
 * The redstone conductor pair acceptance tables (task p10-cover-conductor-redstone).
 * Offline and exhaustive, upstream CoverRedstoneConductorOUT.java:36-57 as the mother:
 *
 * <ul>
 * <li>the IN marker is zero behaviour — it never emits (the weak/strong exits pass the
 *     machine default) and never overrides the incoming world read;</li>
 * <li>the OUT emission truth table — weak = bind4(value lane), the machine default
 *     argument IGNORED (:37-40); the strong exit is NOT overridden (only :36-57 is
 *     behaviour, the strong default passes through);</li>
 * <li>no IN marker → 0, one IN marker → its world feed, several → the MAXIMUM (:51),
 *     recomputed on every block update in both directions;</li>
 * <li>onCoverPlaced self-refresh (:42-46) — the quiet mount already lands the value;</li>
 * <li>the value write count — CoverData.value's change gate only, the direct-write
 *     deviation (the DELAYED_BLOCK_UPDATES queue is not ported);</li>
 * <li>the server-side write gate (the emitter-:151 shape) — a level-less host never
 *     writes;</li>
 * <li>the GTOvenBlock dispatch seam (neighborChanged → covers().onBlockUpdate(), the
 *     upstream 06Covers :382 counterpart) — the wire is fed through the seam.</li>
 * </ul>
 */
public class CoverRedstoneConductorTest extends GTCoverTestBase {

	private static final byte OUT_FACE = 5; // EAST — the fixed emission face
	private static final byte IN_A = (byte) Direction.NORTH.get3DDataValue(); // 2
	private static final byte IN_B = (byte) Direction.SOUTH.get3DDataValue(); // 3

	/** A stub level whose per-position neighbour feeds the test drives (the multi-IN max needs two different feeds). */
	public static class FeedLevel extends gregtech6.tileentity.machines.GTMachinesOfflineTestBase.MachineLevel {
		final Map<BlockPos, Integer> mFeeds = new HashMap<>();

		public FeedLevel() {
			super(new TestRecipeManager());
		}

		@Override
		public int getSignal(BlockPos aPos, Direction aDir) {
			return mFeeds.getOrDefault(aPos, 0);
		}
	}

	/** A probe counting the CoverData-triggered neighbour updates (the emitter-test double). */
	static class CountingProbe extends TileEntityOvenCoverProbe {
		public int mBlockUpdates;
		public FeedLevel mFeedLevel = new FeedLevel();

		CountingProbe() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		CountingProbe leveled() {
			setLevel(mFeedLevel);
			return this;
		}

		@Override
		public void sendBlockUpdateFromCover() {
			mBlockUpdates++;
		}
	}

	/** Mounts the given cover behaviour on {@code aSide} (registry re-put + forced install). */
	private static void mount(TileEntityOvenCoverProbe aOven, byte aSide, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[aSide], aCover);
		assertTrue(aOven.setCoverItem(aSide, new ItemStack(COVER_ITEMS[aSide]), null, true, false), "install accepted");
	}

	/** A fake BlockGetter holding exactly one block entity (the RedstoneHooksTest dispatch fixture). */
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
	// the IN marker is zero behaviour (upstream CoverRedstoneConductorIN :30-36)
	// ---------------------------------------------------------------------------

	@Test
	public void inMarkerNeverEmitsAndNeverOverridesTheIncomingRead() {
		TileEntityOvenCoverProbe tOven = new CountingProbe().leveled();
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		CoverData tData = tOven.getCovers();
		ICover tMarker = tData.mBehaviours[IN_A];
		// the emission side passes the machine default through (AbstractCoverDefault :79/:80 — zero override)
		assertEquals(7, tMarker.getRedstoneOutWeak(IN_A, tData, (byte) 7), "the marker never emits weak");
		assertEquals(9, tMarker.getRedstoneOutStrong(IN_A, tData, (byte) 9), "the marker never emits strong");
		// the incoming side stays the plain world read (no getRedstoneIn override — the
		// whole OUT mechanism depends on this pass-through)
		RedstoneHooksTest.RedstoneLevel tLevel = new RedstoneHooksTest.RedstoneLevel();
		tLevel.mSignal = 7;
		tOven.setLevel(tLevel);
		assertEquals(7, tOven.getRedstoneIncoming(IN_A), "the marker face reads the world");
		assertEquals(1, tLevel.mQueriedDirs.size(), "the world query actually ran (no override sat in front of it)");
	}

	// ---------------------------------------------------------------------------
	// the OUT emission truth table (:37-40) — weak = bind4(value lane), default-blind
	// ---------------------------------------------------------------------------

	@Test
	public void outEmissionTruthTableWeakLaneDefaultBlindStrongPassesThrough() {
		TileEntityOvenCoverProbe tOven = new CountingProbe().leveled();
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		CoverData tData = tOven.getCovers();
		ICover tOut = tData.mBehaviours[OUT_FACE];
		for (int tValue = 0; tValue <= 15; tValue++) {
			tData.value(OUT_FACE, (short) tValue, false);
			// weak = the value lane, the machine default argument is IGNORED (feed 9)
			assertEquals(tValue, tOut.getRedstoneOutWeak(OUT_FACE, tData, (byte) 9),
					"lane " + tValue + ": the weak emission reads the value lane, never the machine default");
			// the strong exit is NOT overridden (:36-57 is the whole behaviour surface)
			assertEquals(9, tOut.getRedstoneOutStrong(OUT_FACE, tData, (byte) 9),
					"lane " + tValue + ": the strong default passes through");
			// and the host exit carries the lane answer on the OPOS fold
			byte tQuerySide = (byte) Direction.values()[OUT_FACE].getOpposite().get3DDataValue();
			assertEquals(tValue, tOven.getRedstoneOutWeak(tQuerySide, 0), "the host weak exit reads the lane");
		}
		// the lane is bind4-clamped on the read (UT.Code.bind4) — an out-of-domain lane
		// value still reads on the redstone scale (the emitter-test clamp arm)
		tData.mValues[OUT_FACE] = 200;
		assertEquals(15, tOut.getRedstoneOutWeak(OUT_FACE, tData, (byte) 0), "bind4(200) == 15");
		tData.mValues[OUT_FACE] = -3;
		assertEquals(0, tOut.getRedstoneOutWeak(OUT_FACE, tData, (byte) 0), "bind4(-3) == 0");
	}

	// ---------------------------------------------------------------------------
	// the scan (:48-57) — no IN → 0, one IN → its feed, several → the max, both ways
	// ---------------------------------------------------------------------------

	@Test
	public void noInMarkerMeansZeroEvenWithLoudBareFaces() {
		CountingProbe tOven = new CountingProbe().leveled();
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		// every bare face is loud — but only IN markers count
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tSide != OUT_FACE) tOven.mFeedLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(tSide)), 15);
		}
		tOven.getCovers().onBlockUpdate();
		assertEquals(0, tOven.getCovers().mValues[OUT_FACE], ":51 — bare faces never contribute, no marker no signal");
	}

	@Test
	public void singleInMarkerFeedsTheOutFaceBothWays() {
		CountingProbe tOven = new CountingProbe().leveled();
		BlockPos tFeedPos = COVER_POS.relative(Direction.from3DDataValue(IN_A));
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 15);
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		assertEquals(15, tOven.getCovers().mValues[OUT_FACE], "the marker feed lands in the value lane");
		// the feed drops — every block update recomputes (:49-56 is stateless per call)
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 6);
		tOven.getCovers().onBlockUpdate();
		assertEquals(6, tOven.getCovers().mValues[OUT_FACE], "the lane follows the feed down");
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 15);
		tOven.getCovers().onBlockUpdate();
		assertEquals(15, tOven.getCovers().mValues[OUT_FACE], "and back up");
	}

	@Test
	public void multipleInMarkersTakeTheMaximum() {
		CountingProbe tOven = new CountingProbe().leveled();
		tOven.mFeedLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(IN_A)), 3);
		tOven.mFeedLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(IN_B)), 9);
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		mount(tOven, IN_B, new CoverRedstoneConductorIN());
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		assertEquals(9, tOven.getCovers().mValues[OUT_FACE], ":51 — Math.max over the marker faces");
		// the quieter face rising above re-wins the max
		tOven.mFeedLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(IN_A)), 11);
		tOven.getCovers().onBlockUpdate();
		assertEquals(11, tOven.getCovers().mValues[OUT_FACE], "the max recomputes over ALL marker faces");
	}

	// ---------------------------------------------------------------------------
	// the self-refresh (:42-46) — the quiet mount already lands the value
	// ---------------------------------------------------------------------------

	@Test
	public void placingTheOutCoverSelfRefreshes() {
		CountingProbe tOven = new CountingProbe().leveled();
		tOven.mFeedLevel.mFeeds.put(COVER_POS.relative(Direction.from3DDataValue(IN_A)), 12);
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		// the mount helper passes aBlockUpdate=false — onCoverPlaced refreshes regardless (:43-46)
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		assertEquals(12, tOven.getCovers().mValues[OUT_FACE], ":43-46 — the placed cover scanned the markers itself");
	}

	// ---------------------------------------------------------------------------
	// the value write count — the direct-write deviation's change gate
	// ---------------------------------------------------------------------------

	@Test
	public void valueWritesCountOnlyOnChange() {
		CountingProbe tOven = new CountingProbe().leveled();
		BlockPos tFeedPos = COVER_POS.relative(Direction.from3DDataValue(IN_A));
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		assertEquals(0, tOven.getCovers().mValues[OUT_FACE]);
		assertEquals(0, tOven.mBlockUpdates, "a quiet world never wrote (0 == 0)");
		// feed 15 → one write; re-fire → none; feed 0 → one write
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 15);
		tOven.getCovers().onBlockUpdate();
		assertEquals(15, tOven.getCovers().mValues[OUT_FACE]);
		assertEquals(1, tOven.mBlockUpdates, "0 → 15 fired exactly one sendBlockUpdateFromCover");
		tOven.getCovers().onBlockUpdate();
		assertEquals(1, tOven.mBlockUpdates, "15 == 15 — the CoverData change gate held");
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 0);
		tOven.getCovers().onBlockUpdate();
		assertEquals(0, tOven.getCovers().mValues[OUT_FACE]);
		assertEquals(2, tOven.mBlockUpdates, "15 → 0 fired the second write");
	}

	// ---------------------------------------------------------------------------
	// the server-side write gate (the emitter-:151 shape)
	// ---------------------------------------------------------------------------

	@Test
	public void valueWriteIsServerGated() {
		CountingProbe tOven = new CountingProbe(); // NO level — isServerSideTE() is false
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		tOven.getCovers().mValues[OUT_FACE] = 5; // a poisoned lane to make the gate observable
		tOven.getCovers().onBlockUpdate();
		assertEquals(5, tOven.getCovers().mValues[OUT_FACE], "a client-side (level-less) host never writes the lane");
		assertEquals(0, tOven.mBlockUpdates, "and never fires the neighbour refresh");
	}

	// ---------------------------------------------------------------------------
	// the GTOvenBlock dispatch seam (neighborChanged → covers().onBlockUpdate())
	// ---------------------------------------------------------------------------

	@Test
	public void ovenDispatchSeamFeedsTheWire() {
		CountingProbe tOven = new CountingProbe().leveled();
		BlockPos tFeedPos = COVER_POS.relative(Direction.from3DDataValue(IN_A));
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 4);
		mount(tOven, IN_A, new CoverRedstoneConductorIN());
		mount(tOven, OUT_FACE, new CoverRedstoneConductorOUT());
		assertEquals(4, tOven.getCovers().mValues[OUT_FACE]);
		// the upstream 06Covers :382 counterpart — a neighbour change re-drives the scan
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 13);
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tOven), COVER_POS);
		assertEquals(13, tOven.getCovers().mValues[OUT_FACE], "the seam delivered the block update to the cover");
		tOven.mFeedLevel.mFeeds.put(tFeedPos, 4);
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tOven), COVER_POS);
		assertEquals(4, tOven.getCovers().mValues[OUT_FACE], "and delivers it both ways");
	}

	@Test
	public void dispatchSeamIgnoresCoverlessAndNonCoverableHosts() {
		// a coverable host with no covers — the hasCovers gate short-circuits
		CountingProbe tBare = new CountingProbe().leveled();
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(tBare), COVER_POS);
		assertEquals(0, tBare.mBlockUpdates, "no covers, no dispatch work");
		// a non-coverable BE — the instanceof gate short-circuits
		GTOvenBlock.dispatchCoverBlockUpdate(fakeLevel(null), COVER_POS);
	}

	// ---------------------------------------------------------------------------
	// the inlined attachment flags (:35-40) + the sprite mapping (:59-63)
	// ---------------------------------------------------------------------------

	@Test
	public void attachmentFlagsAndSpriteMapping() {
		TileEntityOvenCoverProbe tOven = new CountingProbe().leveled();
		CoverRedstoneConductorIN tIn = new CoverRedstoneConductorIN();
		CoverRedstoneConductorOUT tOut = new CoverRedstoneConductorOUT();
		CoverData tData = tOven.getCovers();
		for (ICover tCover : new ICover[] {tIn, tOut}) {
			assertFalse(tCover.interceptClickLeft(OUT_FACE, tData, null, OUT_FACE, 0.5F, 0.5F, 0.5F), ":37 — the left click falls through");
			assertFalse(tCover.interceptClickRight(OUT_FACE, tData, null, OUT_FACE, 0.5F, 0.5F, 0.5F), ":38 — the right click falls through");
			assertFalse(tCover.isOpaque(OUT_FACE, tData), ":39 — attachment plates are non-opaque");
			assertFalse(tCover.isSealable(OUT_FACE, tData), ":40 — attachment plates are non-sealable");
			assertFalse(tCover.needsVisualsSaved(OUT_FACE, tData), "the visuals lane is unused — only the value lane persists");
		}
		assertEquals("gt6:block/redstone_conductor/in", tIn.getCoverTextureSurface(OUT_FACE, tData).toString(), ":35 — the marker art");
		assertEquals("gt6:block/redstone_conductor/out", tOut.getCoverTextureSurface(OUT_FACE, tData).toString(), ":63 — the conductor art");
		// the attachment/holder faces fold into the same sprite (AbstractCoverDefault :71-72)
		assertEquals(tIn.getCoverTextureSurface(OUT_FACE, tData), tIn.getCoverTextureAttachment(OUT_FACE, tData, IN_A));
		assertEquals(tOut.getCoverTextureSurface(OUT_FACE, tData), tOut.getCoverTextureHolder(OUT_FACE, tData, IN_A));
	}
}
