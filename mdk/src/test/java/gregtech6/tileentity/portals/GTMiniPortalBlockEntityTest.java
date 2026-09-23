package gregtech6.tileentity.portals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Portals;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The miniature portal offline pins (task p35-portals-mini-nether-end) — the pairing
 * kernel (×8/128² Nether, ×128/512² End, the Y tie-break, the dead-entry skip), the
 * redstone/comparator promote + 20-tick watchdog state machine, the mActive two-channel
 * sync face and the pair-list lifecycle (upstream MultiTileEntityMiniPortal.java anchors
 * per assertion). Level-less BEs are server-side by the Root convention
 * (TileEntityBase01Root.isServerSide :187), which is exactly the relay's operating side.
 */
public class GTMiniPortalBlockEntityTest {

	static BlockEntityType<GTMiniPortalNetherBlockEntity> sNetherType;
	static BlockEntityType<GTMiniPortalEndBlockEntity> sEndType;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure expected offline; registries ready (GTOfflineTestBase)
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		@SuppressWarnings("unchecked")
		BlockEntityType<GTMiniPortalNetherBlockEntity>[] tNether = (BlockEntityType<GTMiniPortalNetherBlockEntity>[]) new BlockEntityType<?>[1];
		tNether[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTMiniPortalNetherBlockEntity(tNether[0], aPos, aState), Blocks.STONE).build(null);
		sNetherType = tNether[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<GTMiniPortalEndBlockEntity>[] tEnd = (BlockEntityType<GTMiniPortalEndBlockEntity>[]) new BlockEntityType<?>[1];
		tEnd[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTMiniPortalEndBlockEntity(tEnd[0], aPos, aState), Blocks.STONE).build(null);
		sEndType = tEnd[0];
	}

	static GTMiniPortalNetherBlockEntity nether(int aX, int aY, int aZ) {
		return new GTMiniPortalNetherBlockEntity(sNetherType, new BlockPos(aX, aY, aZ), Blocks.STONE.defaultBlockState());
	}

	static GTMiniPortalEndBlockEntity end(int aX, int aY, int aZ) {
		return new GTMiniPortalEndBlockEntity(sEndType, new BlockPos(aX, aY, aZ), Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------- pairing kernel

	@Test
	public void netherPairingUsesTheX8Factor() {
		// the RCON-chain geometry: OW (448,65,350) ↔ Nether (56,·,44) — dx=448-448=0, dz=350-352=-2
		GTMiniPortalBlockEntity tNetherSide = nether(56, 65, 44);
		List<GTMiniPortalBlockEntity> tList = List.of(tNetherSide);
		assertEquals(tNetherSide, GTMiniPortalBlockEntity.nearestPortal(tList, new BlockPos(448, 65, 350), 8, 128 * 128, true),
				"the ×8 coordinate arithmetic pairs the mirrored sites (Nether.java:73)");
	}

	@Test
	public void netherPairingStopsAtThe128mBoundary() {
		// self z rides 352 = 44*8 so dz folds to 0 and dx alone drives the boundary
		// exactly ON the tolerance: dist² == 128² with no prior best wins through the
		// upstream tie-break arm (Nether.java:78 `mTarget == null` disjunct)
		GTMiniPortalBlockEntity tOnBoundary = nether(40, 65, 44); // dx = 448-320 = 128, dz = 0 → 16384
		assertEquals(tOnBoundary, GTMiniPortalBlockEntity.nearestPortal(List.of(tOnBoundary), new BlockPos(448, 65, 352), 8, 128 * 128, true),
				"the boundary candidate is the upstream null-arm acceptance");
		// one rod beyond: dx = 136 → 18496 > 16384 → rejected
		GTMiniPortalBlockEntity tBeyond = nether(39, 65, 44);
		assertNull(GTMiniPortalBlockEntity.nearestPortal(List.of(tBeyond), new BlockPos(448, 65, 352), 8, 128 * 128, true),
				"beyond the 128 m margin nothing pairs (Nether.java:71 initial bound)");
	}

	@Test
	public void endPairingUsesTheX128Factor() {
		// OW (512,65,384) ↔ End (4,·,3): dx=512-512=0, dz=384-384=0
		GTMiniPortalBlockEntity tEndSide = end(4, 65, 3);
		assertEquals(tEndSide, GTMiniPortalBlockEntity.nearestPortal(List.of(tEndSide), new BlockPos(512, 65, 384), 128, 512 * 512, true),
				"the ×128 coordinate arithmetic pairs the mirrored sites (End.java:69)");
		// exactly ON the tolerance: End (0,·,3) → dx=512 → 262144 == 512² → the null-arm acceptance
		GTMiniPortalBlockEntity tOnBoundary = end(0, 65, 3);
		assertEquals(tOnBoundary, GTMiniPortalBlockEntity.nearestPortal(List.of(tOnBoundary), new BlockPos(512, 65, 384), 128, 512 * 512, true),
				"the 512 m boundary is the acceptance edge (End.java:67 initial bound)");
		// one End block further: dx=640 → 409600 > 512² → rejected
		GTMiniPortalBlockEntity tBeyond = end(-1, 65, 3);
		assertNull(GTMiniPortalBlockEntity.nearestPortal(List.of(tBeyond), new BlockPos(512, 65, 384), 128, 512 * 512, true),
				"beyond the 512 m margin nothing pairs");
	}

	@Test
	public void netherMirrorDirectionCarriesTheFactorOnSelf() {
		// the Nether branch (Nether.java:85): the factor hits SELF, the OW coord is raw —
		// self (56,65,44), candidate (448,65,352): dx = 448-448 = 0, dz = 352-352 = 0
		GTMiniPortalBlockEntity tOwSide = nether(448, 65, 352);
		assertEquals(tOwSide, GTMiniPortalBlockEntity.nearestPortal(List.of(tOwSide), new BlockPos(56, 65, 44), 8, 128 * 128, false),
				"the mirrored arm pairs the same geometry (the RCON expect-13 lesson)");
		GTMiniPortalBlockEntity tWrong = nether(448, 65, 350); // dz = 350 - 44*8 = -2 → dist² = 4, still within
		assertEquals(tWrong, GTMiniPortalBlockEntity.nearestPortal(List.of(tWrong), new BlockPos(56, 65, 44), 8, 128 * 128, false));
	}

	@Test
	public void equalDistancePicksTheCloserY() {
		// both candidates at dist² = 0 (XZ 128,0): the Y tie-break picks the nearer one
		GTMiniPortalBlockEntity tFarY = nether(16, 70, 0);
		GTMiniPortalBlockEntity tNearY = nether(16, 64, 0);
		List<GTMiniPortalBlockEntity> tList = new ArrayList<>();
		tList.add(tFarY);
		tList.add(tNearY);
		assertEquals(tNearY, GTMiniPortalBlockEntity.nearestPortal(tList, new BlockPos(128, 65, 0), 8, 128 * 128, true),
				"the equal-distance tie-break is Y proximity (Nether.java:78)");
	}

	@Test
	public void removedCandidatesNeverPair() {
		GTMiniPortalBlockEntity tDead = nether(56, 65, 44);
		tDead.setRemoved(); // isRemoved() ↔ upstream isDead (the POC R3 mapping)
		assertNull(GTMiniPortalBlockEntity.nearestPortal(List.of(tDead), new BlockPos(448, 65, 350), 8, 128 * 128, true),
				"the dead-entry skip (upstream `!tTarget.isDead()`, Nether.java:72)");
	}

	// ------------------------------------ the promote + watchdog state machine

	@Test
	public void inboundRelayPromotesThenWatchdogDecaysAfter20Ticks() {
		GTMiniPortalNetherBlockEntity tPortal = nether(0, 64, 0);
		tPortal.mActive = true;
		tPortal.xRedstone[2] = 5; // the cross-dimension relay write (upstream onTick :177)
		tPortal.onTickStart(1, true);
		assertEquals(5, tPortal.mRedstone[2], "the promote arm copies xRedstone → mRedstone (upstream :120)");
		assertEquals(-1, tPortal.xRedstone[2], "the sentinel resets (upstream :123)");
		assertEquals(0, tPortal.wRedstone[2], "the watchdog resets on a fresh value (upstream :124)");
		for (int tTick = 2; tTick <= 21; tTick++) tPortal.onTickStart(tTick, true);
		assertEquals(5, tPortal.mRedstone[2], "20 ticks without a fresh relay write hold the value (upstream :126-133)");
		tPortal.onTickStart(22, true);
		assertEquals(0, tPortal.mRedstone[2], "the 20-tick watchdog decays to zero (upstream :127-130)");
		assertTrue(tPortal.mDoesBlockUpdate, "the decay arms a block update (upstream causeBlockUpdate :129)");
	}

	@Test
	public void comparatorRelayRidesTheSameMachine() {
		GTMiniPortalEndBlockEntity tPortal = end(0, 64, 0);
		tPortal.mActive = true;
		tPortal.xComparator[5] = 15;
		tPortal.onTickStart(1, true);
		assertEquals(15, tPortal.mComparator[5], "the comparator promote (upstream :137-140)");
		assertEquals(0, tPortal.mRedstone[5], "the redstone buffer stays untouched");
	}

	@Test
	public void inactivePortalEmitsNothing() {
		GTMiniPortalNetherBlockEntity tPortal = nether(0, 64, 0);
		tPortal.mRedstone[2] = 9;
		tPortal.mComparator[2] = 9;
		tPortal.onTickStart(1, true);
		assertEquals(0, tPortal.mRedstone[2], "the inactive arm zeroes the redstone emission (upstream :154-157)");
		assertEquals(0, tPortal.mComparator[2], "the inactive arm zeroes the comparator output (upstream :158-161)");
	}

	@Test
	public void theRescanBeatFiresOnTimerMod100Equals5AndOnTargetDeath() {
		CountingNether tPortal = new CountingNether(new BlockPos(0, 64, 0));
		GTMiniPortalNetherBlockEntity tOther = nether(56, 65, 44);
		tPortal.mActive = true;
		tPortal.onTick(104, true);
		assertEquals(0, tPortal.mScans, "104 % 100 = 4 — the beat holds (upstream :173 `aTimer % 100 == 5`)");
		tPortal.onTick(105, true);
		assertEquals(1, tPortal.mScans, "105 % 100 = 5 — the null-target rescan beat fires");
		tPortal.mTarget = tOther;
		tPortal.onTick(205, true);
		assertEquals(1, tPortal.mScans, "a live target skips the beat (upstream `mTarget.isDead()` false arm)");
		tOther.setRemoved();
		tPortal.onTick(206, true);
		assertEquals(2, tPortal.mScans, "a removed target rescans IMMEDIATELY (upstream :173, the POC isRemoved() mapping)");
	}

	/** The findTargetPortal counter probe (the rescan-beat pin). */
	static final class CountingNether extends GTMiniPortalNetherBlockEntity {
		int mScans = 0;

		CountingNether(BlockPos aPos) {
			super(sNetherType, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public void findTargetPortal() {
			mScans++;
		}
	}

	// --------------------------------------------------- the sync + NBT face

	@Test
	public void mActiveRidesBothVanillaSyncChannels() {
		GTMiniPortalNetherBlockEntity tPortal = nether(0, 64, 0);
		tPortal.setPortalActive();
		assertTrue(tPortal.mActive, "the activation flipped the flag (upstream :183)");
		CompoundTag tTag = tPortal.getUpdateTag(); // = saveWithoutMetadata → the chunk-data channel
		assertTrue(tTag.contains(GTMiniPortalBlockEntity.NBT_ACTIVE), "the chunk-data channel carries gt.active (upstream :79)");
		assertTrue(tTag.getBoolean(GTMiniPortalBlockEntity.NBT_ACTIVE));
		assertNotNull(tPortal.getUpdatePacket(), "the block-update channel is armed (ClientboundBlockEntityDataPacket)");
		GTMiniPortalNetherBlockEntity tMirror = nether(0, 64, 0);
		tMirror.load(tTag); // the client landing (both channels converge on load)
		assertTrue(tMirror.mActive, "the client mirror rehydrates mActive (upstream receiveDataByte :249-257)");
	}

	// ------------------------------------------------- the registry lifecycle

	@Test
	public void pairListMembershipSurvivesLikeTheUpstreamTables() {
		GTMiniPortalNetherBlockEntity tA = nether(0, 64, 0);
		GTMiniPortalNetherBlockEntity tB = nether(56, 65, 44);
		try {
			GTMiniPortalNetherBlockEntity.sListWorldSide.add(tA);
			GTMiniPortalNetherBlockEntity.sListNetherSide.add(tB);
			assertEquals(1, GTMiniPortalNetherBlockEntity.sListWorldSide.size());
			GT6Portals.clearPairLists(); // the ServerStarted/Stopped face (upstream :209-210)
			assertTrue(GTMiniPortalNetherBlockEntity.sListWorldSide.isEmpty());
			assertTrue(GTMiniPortalNetherBlockEntity.sListNetherSide.isEmpty());
			assertTrue(GTMiniPortalEndBlockEntity.sListEndSide.isEmpty());
		} finally {
			GT6Portals.clearPairLists();
		}
	}

	@Test
	public void disableClearsTheTargetRelayBuffers() {
		GTMiniPortalNetherBlockEntity tA = nether(0, 64, 0);
		GTMiniPortalNetherBlockEntity tB = nether(56, 65, 44);
		tA.mTarget = tB;
		tA.mRedstone[2] = 7;
		tA.mComparator[2] = 7;
		tB.xRedstone[3] = 9; // OPOS[2] = 3 — the buffer A writes on its own tick
		tA.disableThisPortal();
		assertFalse(tA.mActive, "the disable deactivates (upstream :214)");
		assertEquals(0, tA.mRedstone[2], "the own emission zeroes (upstream :216)");
		assertEquals(0, tB.xRedstone[3], "the target's inbound relay buffer zeroes (upstream :219-220)");
		assertNull(tA.mTarget, "the target reference drops (upstream :224)");
	}

	@Test
	public void removalRescansWhoeverPointedHere() {
		GTMiniPortalNetherBlockEntity tA = nether(0, 64, 0);
		GTMiniPortalNetherBlockEntity tB = nether(56, 65, 44);
		try {
			GTMiniPortalNetherBlockEntity.sListWorldSide.add(tA);
			GTMiniPortalNetherBlockEntity.sListNetherSide.add(tB);
			tB.mTarget = tA;
			tA.removeThisPortalFromLists(); // upstream :186-189
			assertNull(tB.mTarget, "the pointer re-scans (level-less the rescan lands null — upstream :187)");
			assertFalse(GTMiniPortalNetherBlockEntity.sListWorldSide.contains(tA), "the membership drops");
		} finally {
			GT6Portals.clearPairLists();
		}
	}

	@Test
	public void levellessPortalStillAnswersTheDelegationContract() {
		GTMiniPortalNetherBlockEntity tPortal = nether(0, 64, 0);
		assertNull(tPortal.delegateAdjacent((byte) 2), "no level → no delegate (the guarded adjacency, POC R1)");
		assertNull(tPortal.delegateAdjacent((byte) 9), "the invalid side folds to null");
		assertFalse(tPortal.isEnergyAcceptingFrom(null, (byte) 2, false), "no level → no energy face");
	}
}
