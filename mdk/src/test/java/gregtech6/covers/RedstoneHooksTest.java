package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.Test;

import gregtech6.block.GTOvenBlock;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.util.UT6;

/**
 * The cover redstone framework truth tables (task p9-redstone-hooks, ADR
 * 2026-09-01-p9-redstone-hooks acceptance ⑤). Offline and exhaustive — the OPOS
 * direction fold is the one irreversible mistake this card can make, so the emission
 * side each hook receives is asserted for all six query directions, plus the explicit
 * front/back oven case and the Block-carrier bridge (GTOvenBlock.getSignal /
 * getDirectSignal). The incoming read pins the two branches (cover override vs the
 * plain world pass-through) and the SIDES_INVALID six-face maximum with its early 15.
 */
public class RedstoneHooksTest extends GTCoverTestBase {

	/** Upstream side order == Direction.get3DDataValue(): DOWN, UP, NORTH, SOUTH, WEST, EAST. */
	private static final Direction[] SIDES = Direction.values();

	/** A cover that records every redstone hook invocation (the C-card emitter's test double). */
	static class RecordingCover extends CoverTextureSimple {
		final byte mIn, mWeak, mStrong;
		final List<Byte> mInSides = new ArrayList<>();
		final List<Byte> mWeakSides = new ArrayList<>();
		final List<Byte> mStrongSides = new ArrayList<>();
		final List<Byte> mWeakDefaults = new ArrayList<>();
		final List<Byte> mStrongDefaults = new ArrayList<>();
		final List<String> mStoppedEvents = new ArrayList<>();

		RecordingCover(byte aIn, byte aWeak, byte aStrong) {
			super(TEST_SPRITE);
			mIn = aIn;
			mWeak = aWeak;
			mStrong = aStrong;
		}

		@Override public byte getRedstoneIn(byte aCoverSide, CoverData aData) {mInSides.add(aCoverSide); return mIn;}
		@Override public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {mWeakSides.add(aCoverSide); mWeakDefaults.add(aDefaultRedstone); return mWeak;}
		@Override public byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {mStrongSides.add(aCoverSide); mStrongDefaults.add(aDefaultRedstone); return mStrong;}
		@Override public void onStoppedUpdate(byte aCoverSide, CoverData aData, boolean aStopped) {mStoppedEvents.add(aCoverSide + ":" + aStopped);}
	}

	/** A stub level whose neighbour signal the test drives (the getIndirectPowerLevelTo counterpart). */
	public static class RedstoneLevel extends gregtech6.tileentity.machines.GTMachinesOfflineTestBase.MachineLevel {
		int mSignal = 0;
		final List<Direction> mQueriedDirs = new ArrayList<>();
		final List<BlockPos> mQueriedPos = new ArrayList<>();

		public RedstoneLevel() {
			super(new TestRecipeManager());
		}

		@Override
		public int getSignal(BlockPos aPos, Direction aDir) {
			mQueriedDirs.add(aDir);
			mQueriedPos.add(aPos);
			return mSignal;
		}
	}

	/** An oven probe on a redstone-driven stub level. */
	private TileEntityOvenCoverProbe redstoneOven() {
		TileEntityOvenCoverProbe tOven = bareOven();
		tOven.setLevel(new RedstoneLevel());
		return tOven;
	}

	private RedstoneLevel level(TileEntityOvenCoverProbe aOven) {
		return (RedstoneLevel) aOven.getLevel();
	}

	/** Mounts the given cover behaviour on {@code aSide} (registry re-put + forced install). */
	private void mount(TileEntityOvenCoverProbe aOven, byte aSide, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[aSide], aCover);
		assertTrue(aOven.setCoverItem(aSide, new ItemStack(COVER_ITEMS[aSide]), null, true, false), "install accepted");
	}

	// ---------------------------------------------------------------------------
	// incoming — the covered-face override vs the bare-face pass-through (:409-424)
	// ---------------------------------------------------------------------------

	@Test
	void incomingPlainFacePassesWorldThrough() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		RedstoneLevel tLevel = level(tOven);
		tLevel.mSignal = 7;
		assertEquals(7, tOven.getRedstoneIncoming((byte) Direction.NORTH.get3DDataValue()), "bare face reads the world");
		// the Root :587 shape — the neighbour at pos.relative(face) is queried along the face
		assertEquals(List.of(Direction.NORTH), tLevel.mQueriedDirs);
		assertEquals(List.of(COVER_POS.relative(Direction.NORTH)), tLevel.mQueriedPos);
		// the default AbstractCoverDefault read passes the same world query through
		mount(tOven, (byte) 3, new RecordingCover((byte) 0, (byte) 0, (byte) 0));
		tLevel.mQueriedDirs.clear();
		tLevel.mQueriedPos.clear();
		assertEquals(7, tOven.getRedstoneIncoming((byte) Direction.NORTH.get3DDataValue()), "the SOUTH cover does not answer the NORTH face");
		assertEquals(1, tLevel.mQueriedDirs.size(), "the bare-face branch stays a pure world read");
	}

	@Test
	void incomingCoveredFaceOverridesWorldQuery() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		RedstoneLevel tLevel = level(tOven);
		tLevel.mSignal = 7;
		RecordingCover tCover = new RecordingCover((byte) 9, (byte) 0, (byte) 0);
		mount(tOven, (byte) Direction.NORTH.get3DDataValue(), tCover);
		assertEquals(9, tOven.getRedstoneIncoming((byte) Direction.NORTH.get3DDataValue()), ":423 — the cover answer wins");
		assertEquals(List.of((byte) Direction.NORTH.get3DDataValue()), tCover.mInSides, "the cover sees its own face");
		assertTrue(tLevel.mQueriedDirs.isEmpty(), ":417 — the world query is overridden away");
	}

	@Test
	void incomingInvalidSideFoldsToSixFaceMaxWithEarlyFifteen() {
		// cover-free: the Root :580-585 degenerate case
		TileEntityOvenCoverProbe tOven = redstoneOven();
		RedstoneLevel tLevel = level(tOven);
		tLevel.mSignal = 5;
		assertEquals(5, tOven.getRedstoneIncoming((byte) -1), "the six-face maximum");
		tLevel.mSignal = 0;
		tLevel.mQueriedDirs.clear();
		assertEquals(0, tOven.getRedstoneIncoming((byte) -1), "all-quiet world");
		assertEquals(6, tLevel.mQueriedDirs.size(), "no early exit below 15 — all six faces read");

		// cover-aware: the :411-422 form — a 15 ends the scan early
		TileEntityOvenCoverProbe tCovered = redstoneOven();
		RedstoneLevel tCoveredLevel = level(tCovered);
		RecordingCover tCover = new RecordingCover((byte) 15, (byte) 0, (byte) 0);
		mount(tCovered, (byte) Direction.NORTH.get3DDataValue(), tCover);
		assertEquals(15, tCovered.getRedstoneIncoming((byte) -1), ":419 — the early 15 exit");
		assertEquals(List.of((byte) Direction.NORTH.get3DDataValue()), tCover.mInSides, "the scan stopped at the 15 face");
		assertEquals(2, tCoveredLevel.mQueriedDirs.size(), "faces 3..5 are never queried after the early exit");
	}

	@Test
	void incomingWithoutLevelReadsZero() {
		TileEntityOvenCoverProbe tOven = bareOven();
		assertEquals(0, tOven.getRedstoneIncoming((byte) Direction.NORTH.get3DDataValue()), "upstream Root :578");
		assertEquals(0, tOven.getRedstoneIncoming((byte) -1));
	}

	// ---------------------------------------------------------------------------
	// outgoing — the OPOS fold truth table (:427-438), the direction-reversal guard
	// ---------------------------------------------------------------------------

	@Test
	void outgoingOposTruthTableAllSixDirections() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		for (Direction tQuery : SIDES) {
			byte tExpectedFace = UT6.OPOS[tQuery.get3DDataValue()];
			RecordingCover tCover = new RecordingCover((byte) 0, (byte) 13, (byte) 11);
			mount(tOven, tExpectedFace, tCover);
			assertEquals(13, tOven.getRedstoneOutWeak((byte) tQuery.get3DDataValue(), 4),
					"querying from " + tQuery + " must reach the " + Direction.from3DDataValue(tExpectedFace) + " cover (weak)");
			assertEquals(List.of(tExpectedFace), tCover.mWeakSides, "the weak hook sees the emission face, not the query side");
			assertEquals(11, tOven.getRedstoneOutStrong((byte) tQuery.get3DDataValue(), 4),
					"querying from " + tQuery + " must reach the emission-face cover (strong)");
			assertEquals(List.of(tExpectedFace), tCover.mStrongSides);
			assertEquals(List.of((byte) 4), tCover.mWeakDefaults, "the machine default rides in (:429)");
		}
	}

	@Test
	void outgoingUncoveredFacesPassTheMachineDefaultThrough() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		assertEquals(4, tOven.getRedstoneOutWeak((byte) Direction.NORTH.get3DDataValue(), 4), ":430 — no cover, the emission passes");
		assertEquals(0, tOven.getRedstoneOutStrong((byte) Direction.SOUTH.get3DDataValue(), 0), ":437 — same shape");
		// the cover branch clamps the machine default to the redstone scale (UT.Code.bind4)
		RecordingCover tCover = new RecordingCover((byte) 0, (byte) 13, (byte) 11);
		mount(tOven, (byte) Direction.SOUTH.get3DDataValue(), tCover);
		assertEquals(13, tOven.getRedstoneOutWeak((byte) Direction.NORTH.get3DDataValue(), 200));
		assertEquals(List.of((byte) 15), tCover.mWeakDefaults, "bind4(200) == 15");
	}

	@Test
	void outgoingFrontBackOposSemanticsOnTheOven() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		assertTrue(tOven.setFrontFacing((byte) 3), "front = SOUTH (side 3)");
		RecordingCover tCover = new RecordingCover((byte) 0, (byte) 13, (byte) 11);
		mount(tOven, (byte) 3, tCover);
		// a receiver SOUTH of the machine (the front) queries along NORTH: the fold lands on the front cover
		assertEquals(13, tOven.getRedstoneOutWeak((byte) Direction.NORTH.get3DDataValue(), 0), "the front face emits toward its own side");
		assertEquals(11, tOven.getRedstoneOutStrong((byte) Direction.NORTH.get3DDataValue(), 0));
		// a receiver NORTH of the machine queries along SOUTH: OPOS lands on the bare back face
		assertEquals(0, tOven.getRedstoneOutWeak((byte) Direction.SOUTH.get3DDataValue(), 0), "the back stays quiet");
		assertEquals(0, tOven.getRedstoneOutStrong((byte) Direction.SOUTH.get3DDataValue(), 0));
	}

	// ---------------------------------------------------------------------------
	// the AbstractCoverDefault defaults (:78-80) — plain covers are transparent
	// ---------------------------------------------------------------------------

	@Test
	void abstractCoverDefaultRedstoneDefaults() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		RedstoneLevel tLevel = level(tOven);
		tLevel.mSignal = 7;
		mount(tOven, (byte) Direction.NORTH.get3DDataValue(), testCover());
		ICover tCover = tOven.getCovers().mBehaviours[Direction.NORTH.get3DDataValue()];
		CoverData tData = tOven.getCovers();
		assertEquals(7, tCover.getRedstoneIn((byte) Direction.NORTH.get3DDataValue(), tData), ":78 — the world read passes through");
		assertEquals(List.of(COVER_POS.relative(Direction.NORTH)), level(tOven).mQueriedPos);
		assertEquals(5, tCover.getRedstoneOutWeak((byte) Direction.NORTH.get3DDataValue(), tData, (byte) 5), ":79 — the default passes the machine value");
		assertEquals(5, tCover.getRedstoneOutStrong((byte) Direction.NORTH.get3DDataValue(), tData, (byte) 5), ":80 — same shape");
	}

	// ---------------------------------------------------------------------------
	// the GTOvenBlock carrier bridge — the vanilla query lands on the ICoverableTE exit
	// ---------------------------------------------------------------------------

	/** A fake BlockGetter holding exactly one block entity at the cover position. */
	private static BlockGetter fakeLevel(BlockEntity aBE) {
		return new BlockGetter() {
			@Override public BlockState getBlockState(BlockPos aPos) {return net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState();}
			@Override public net.minecraft.world.level.material.FluidState getFluidState(BlockPos aPos) {return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();}
			@Override public BlockEntity getBlockEntity(BlockPos aPos) {return aBE;}
			@Override public int getHeight() {return 384;}
			@Override public int getMinBuildHeight() {return -64;}
			@Override public int getMaxBuildHeight() {return 320;}
		};
	}

	@Test
	void ovenBlockBridgeCarriesTheCoverEmission() {
		// the bridge body drives the exits exactly as the Block override would (the block
		// instance itself is not constructible post-bootstrap — TileEntityOvenFacingTest
		// precedent; the live dispatch is covered by the RCON chain)
		TileEntityOvenCoverProbe tOven = redstoneOven();
		RecordingCover tCover = new RecordingCover((byte) 0, (byte) 13, (byte) 11);
		// the query along NORTH folds (OPOS) onto the SOUTH emission face — mount the cover there
		byte tEmissionFace = UT6.OPOS[Direction.NORTH.get3DDataValue()];
		assertEquals((byte) Direction.SOUTH.get3DDataValue(), tEmissionFace, "the fold pins NORTH-query → SOUTH-face");
		mount(tOven, tEmissionFace, tCover);
		BlockGetter tLevel = fakeLevel(tOven);
		assertEquals(13, GTOvenBlock.bridgeSignal(tLevel, COVER_POS, Direction.NORTH, 0, false), "the bridge reaches the weak exit");
		assertEquals(11, GTOvenBlock.bridgeSignal(tLevel, COVER_POS, Direction.NORTH, 0, true), "the bridge reaches the strong exit");
		// the query along SOUTH folds onto the bare NORTH face: the machine default passes through
		assertEquals(0, GTOvenBlock.bridgeSignal(tLevel, COVER_POS, Direction.SOUTH, 0, false));
		assertEquals(0, GTOvenBlock.bridgeSignal(tLevel, COVER_POS, Direction.SOUTH, 0, true));
		// a plain block-entity-free carrier stays vanilla-quiet
		BlockGetter tEmpty = fakeLevel(null);
		assertEquals(0, GTOvenBlock.bridgeSignal(tEmpty, COVER_POS, Direction.NORTH, 0, false));
		assertEquals(0, GTOvenBlock.bridgeSignal(tEmpty, COVER_POS, Direction.NORTH, 0, true));
	}

	// ---------------------------------------------------------------------------
	// the mStopped broadcast contract rides CoverData untouched (the zero-diff claim)
	// ---------------------------------------------------------------------------

	@Test
	void stoppedBroadcastContractRidesCoverData() {
		TileEntityOvenCoverProbe tOven = redstoneOven();
		RecordingCover tCover = new RecordingCover((byte) 0, (byte) 0, (byte) 0);
		mount(tOven, (byte) Direction.NORTH.get3DDataValue(), tCover);
		assertTrue(tOven.getCovers().setStopped(true), "the change fires");
		assertEquals(List.of(Direction.NORTH.get3DDataValue() + ":true"), tCover.mStoppedEvents, "onStoppedUpdate reaches the cover");
		assertTrue(tOven.getCovers().writeToNBT().getBoolean("y"), "the y key persists (CoverData zero-diff recheck)");
		assertTrue(tOven.getCovers().setStopped(false), "the release fires too");
		assertEquals(List.of(Direction.NORTH.get3DDataValue() + ":true", Direction.NORTH.get3DDataValue() + ":false"), tCover.mStoppedEvents);
	}
}
