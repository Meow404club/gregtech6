package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.Test;

/**
 * The eight-phase tick dispatcher of TileEntityBase03TicksAndSync (upstream :111-165),
 * verified offline (level == null behaves as server side, upstream isServerSide :177).
 */
public class TileEntityBase03DispatchTest extends GTOfflineTestBase {

	/** Records every phase the dispatcher enters. */
	private static class RecordingBE extends TileEntityBase03TicksAndSync {
		final List<String> mPhases = new ArrayList<>();
		boolean mThrowInOnTick = false;
		boolean mForceCheck = false;

		/**
		 * 21.1 BlockEntity ctor validates its type/state pair (validateBlockState →
		 * getType().isValid(), javap 21.1.249), so the fixture binds a synthetic BET over
		 * the vanilla stone block instead of the pre-21.1 nulls (task p15-m4-test-infra).
		 * The supplier is stored, never invoked — no init cycle with TEST_TYPE.
		 */
		private static final BlockEntityType<RecordingBE> TEST_TYPE = BlockEntityType.Builder
				.of((aPos, aState) -> new RecordingBE(true), Blocks.STONE).build(null);

		RecordingBE(boolean aTicking) {
			super(aTicking, TEST_TYPE, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
		}

		@Override
		public String getTileEntityName() {
			return "test_recording";
		}

		@Override
		public void onTickFirst(boolean aIsServerSide) { mPhases.add("first"); }

		@Override
		public void onTickStart(long aTimer, boolean aIsServerSide) { mPhases.add("start:" + aTimer); }

		@Override
		public void onTick(long aTimer, boolean aIsServerSide) {
			mPhases.add("tick:" + aTimer);
			if (mThrowInOnTick) throw new IllegalStateException("boom");
		}

		@Override
		public boolean onTickCheck(long aTimer) { return mForceCheck; }

		@Override
		public void onTickChecked(long aTimer) { mPhases.add("checked:" + aTimer); }

		@Override
		public void onTickEnd(long aTimer, boolean aIsServerSide) { mPhases.add("end:" + aTimer); }

		@Override
		public void onTickFailed(long aTimer, boolean aIsServerSide) { mPhases.add("failed:" + aTimer); }

		@Override
		public void sendClientData() { mPhases.add("send"); }
	}

	@Test
	public void firstTickRunsOnTickFirstOnceAndAdvancesTimer() {
		RecordingBE tBe = new RecordingBE(true);

		tBe.updateEntity();
		assertTrue(tBe.mPhases.contains("first"));
		assertEquals(List.of("start:0", "tick:1", "end:1"), tBe.mPhases.stream().filter(p -> !p.equals("first")).toList(),
				"first tick: onTickStart sees mTimer 0 (upstream :120), onTick sees 1 (incremented in the 02 core, upstream 02:146)");
		assertEquals(1, tBe.getTimer());
		assertFalse(tBe.mIsRunningTick, "mIsRunningTick must be cleared when the tick ends (upstream :140)");

		tBe.mPhases.clear();
		tBe.updateEntity();
		assertFalse(tBe.mPhases.contains("first"), "onTickFirst is the very first tick only (upstream :115)");
		assertEquals(2, tBe.getTimer());
	}

	@Test
	public void syncGateOpensAtTimerThreeLikeUpstream() {
		RecordingBE tBe = new RecordingBE(true);
		tBe.updateClientData(); // upstream :88

		tBe.updateEntity();
		tBe.updateEntity();
		assertFalse(tBe.mPhases.contains("send"), "upstream :123 gates sync on mTimer > 2 — ticks 1 and 2 must not send");

		tBe.updateEntity(); // mTimer == 3 after increment
		assertTrue(tBe.mPhases.contains("send"));
		assertTrue(tBe.mPhases.contains("checked:3"));

		tBe.mPhases.clear();
		tBe.updateEntity(); // flag consumed, no check -> no send
		assertFalse(tBe.mPhases.contains("send"));

		tBe.mForceCheck = true;
		tBe.updateEntity(); // onTickCheck true -> send again
		assertTrue(tBe.mPhases.contains("send"));
	}

	@Test
	public void throwableFallbackRunsOnTickFailedAndSetsError() {
		RecordingBE tBe = new RecordingBE(true);
		tBe.mThrowInOnTick = true;

		tBe.updateEntity(); // must not propagate (upstream :130-139)

		assertTrue(tBe.mPhases.contains("failed:1"), "onTickFailed runs with the timer value of the failing tick");
		assertTrue(tBe.ERROR_MESSAGE.startsWith("Serverside: "), "offline BEs are server side (upstream :132)");
		assertTrue(tBe.ERROR_MESSAGE.contains("boom"));
		assertFalse(tBe.mIsRunningTick);
	}

	@Test
	public void canUpdateMirrorsUpstreamSemantics() {
		// upstream :440 = mIsTicking && mShouldRefresh
		assertTrue(new RecordingBE(true).canUpdate());
		assertFalse(new RecordingBE(false).canUpdate(), "notick chain: TileEntityBase01Root(false) never ticks");

		RecordingBE tStopped = new RecordingBE(true);
		tStopped.mShouldRefresh = false;
		assertFalse(tStopped.canUpdate(), "machines can stop ticking by flipping mShouldRefresh");
	}

	@Test
	public void onTickResetChecksClearsBlockUpdatedFlag() {
		RecordingBE tBe = new RecordingBE(true);
		tBe.markBlockUpdated();
		assertTrue(tBe.mBlockUpdated);
		tBe.updateEntity();
		assertFalse(tBe.mBlockUpdated, "upstream :158-159 resets the change-detection flags every tick");
	}
}
