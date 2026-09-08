package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel;

/**
 * The pipe-ownership offline tests (task p24-pipe-owner acceptance ①): the
 * {@link GTFluidPipeBlockEntity#allowInteraction(UUID)} truth table (five arms — the
 * upstream TileEntityBase03TicksAndSync:106-108 shape over UUIDs, zero Player/Entity
 * constructed), the placement owner record (upstream 10ConnectorRendered:148-150), the
 * gt.ownable/gt.owner NBT round-trip (the vanilla putUUID int-array form), the two
 * neighbour gates (placement support-side, upstream 09Connector:86; toggle target-side,
 * :75) plus the toggle self gate (the 06Covers:141 counterpart), and the static
 * break-gate seam ({@link GTFluidPipeBlockEntity#ownerDestroyProgress} — deny 0.0F /
 * allow passes through, the upstream 01Root:941-943 counterpart).
 *
 * <p>All arms drive the BE methods directly on the {@link MultiBlockLevel} stub — no
 * Block, no Player, no Entity (the offline-UUID discipline the predicate itself serves).
 */
public class GTPipeOwnerTest extends GTOfflineTestBase {

	static BlockEntityType<GTFluidPipeBlockEntity> sType;
	static final BlockPos POS_A = new BlockPos(2, 3, 4);

	/** Fixed identities — deterministic, no Player construction. */
	static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	static final UUID FOREIGN = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

	@BeforeAll
	static void buildPipeFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTFluidPipeBlockEntity>[] tHolder = (BlockEntityType<GTFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTFluidPipeBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];
	}

	private static GTFluidPipeBlockEntity place(MultiBlockLevel aLevel, BlockPos aPos) {
		GTFluidPipeBlockEntity tPipe = sType.create(aPos, Blocks.STONE.defaultBlockState());
		tPipe.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.STONE.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tPipe);
		return tPipe;
	}

	/** The locked form: ownable + owner set in one step. Task p25-c-foam-pipe-spray: a
	 * locked pipe IS a dried owned foam — the third clause {@code !mFoamDried} of upstream
	 * 10ConnectorRendered:153-156 is the ONLY lock arming (an undried owned pipe passes
	 * everyone), so the fixture sets the dried foam too. */
	private static GTFluidPipeBlockEntity locked(MultiBlockLevel aLevel, BlockPos aPos, UUID aOwner) {
		GTFluidPipeBlockEntity tPipe = place(aLevel, aPos);
		tPipe.mOwnable = true;
		tPipe.mOwner = aOwner;
		tPipe.mFoamDried = true; // the p25 third clause — the dried foam arms the lock
		return tPipe;
	}

	// ---------------------------------------------------------------------------
	// ① the predicate truth table (upstream 03TicksAndSync:106-108 over UUIDs)
	// ---------------------------------------------------------------------------

	@Test
	public void allowInteractionTruthTable() {
		GTFluidPipeBlockEntity tPipe = place(new MultiBlockLevel(), POS_A);

		// arm 1: ownable=false short-circuits — everyone passes (the default pipe, upstream
		// 10ConnectorRendered:154-156 with !mOwnable opening)
		tPipe.mOwnable = false;
		tPipe.mOwner = null;
		assertTrue(tPipe.allowInteraction(null));
		assertTrue(tPipe.allowInteraction(FOREIGN));
		assertTrue(tPipe.allowInteraction(OWNER));

		// arm 2: ownable=true, owner set, foam DRIED, null identity → deny (the console is
		// nobody — the upstream :107 `aEntity != null` arm). Task p25-c-foam-pipe-spray:
		// the lock arms only through the dried foam (the third clause) — an undried owned
		// pipe passes everyone (see GTPipeFoamTest.thirdClauseFlipsOnDrying).
		tPipe.mOwnable = true;
		tPipe.mOwner = OWNER;
		tPipe.mFoamDried = true;
		assertFalse(tPipe.allowInteraction(null));
		tPipe.mFoamDried = false;
		assertTrue(tPipe.allowInteraction(null), "undried owned foam: the third clause bypasses the owner half (p25)");

		// arm 3: ownable=true, owner=null → everyone passes (upstream :107 arm 1 — the
		// null-owner pipe is unowned even while ownable is persisted)
		tPipe.mOwner = null;
		assertTrue(tPipe.allowInteraction(null));
		assertTrue(tPipe.allowInteraction(FOREIGN));

		// arm 4: ownable=true, owner match → allow (the dried form of the locked pipe)
		tPipe.mOwner = OWNER;
		tPipe.mFoamDried = true;
		assertTrue(tPipe.allowInteraction(OWNER));

		// arm 5: ownable=true, dried, foreign identity → deny
		assertFalse(tPipe.allowInteraction(FOREIGN));

		// the defaults of a fresh BE are the unlocked plain pipe
		GTFluidPipeBlockEntity tFresh = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		assertFalse(tFresh.mOwnable);
		assertNull(tFresh.mOwner);
		assertTrue(tFresh.allowInteraction(null));
	}

	// ---------------------------------------------------------------------------
	// ② the placement owner record (upstream 10ConnectorRendered:148-150 verbatim form)
	// ---------------------------------------------------------------------------

	@Test
	public void placementRecordsOwnerOnlyWhenOwnable() {
		// the upstream shape: `if (mOwnable && aPlayer != null) mOwner = ...` — a not-ownable
		// pipe NEVER records (the live behaviour stays the upstream plain pipe)
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tPlain = place(tLevel, POS_A);
		tPlain.onPlaced((byte)1, OWNER);
		assertFalse(tPlain.mOwnable);
		assertNull(tPlain.mOwner, "ownable=false → the placement never records an owner (upstream :148-150)");

		// ownable=true + identity → recorded
		GTFluidPipeBlockEntity tOwned = place(tLevel, POS_A.east());
		tOwned.mOwnable = true;
		tOwned.onPlaced((byte)1, OWNER);
		assertEquals(OWNER, tOwned.mOwner, "ownable=true + identity → the owner is recorded");

		// ownable=true + null identity (console placement) → not recorded
		GTFluidPipeBlockEntity tConsole = place(tLevel, POS_A.east(2));
		tConsole.mOwnable = true;
		tConsole.onPlaced((byte)1, null);
		assertNull(tConsole.mOwner, "ownable=true + null identity → nothing recorded (aPlayer != null arm)");

		// the record happens BEFORE the support gate — a denied placement still records the
		// owner (upstream order: 10ConnectorRendered:148-150 runs ahead of 09Connector:86)
		GTFluidPipeBlockEntity tSupport = locked(tLevel, POS_A.north(), FOREIGN);
		GTFluidPipeBlockEntity tDenied = place(tLevel, POS_A);
		tDenied.mOwnable = true;
		// clicked face 3 (SOUTH) → support side OPOS[3]=2 (NORTH) → the FOREIGN-locked
		// support denies the null... the OWNER identity: FOREIGN's pipe rejects OWNER
		tDenied.onPlaced((byte)3, OWNER);
		assertEquals(OWNER, tDenied.mOwner, "the owner record precedes the :86 gate — a denial does not undo it");
		assertFalse(tSupport.allowInteraction(OWNER), "sanity: the FOREIGN-owned support rejects OWNER");
	}

	// ---------------------------------------------------------------------------
	// ③ the NBT round-trip (gt.ownable / gt.owner, CS.java:1167-1168 verbatim keys)
	// ---------------------------------------------------------------------------

	@Test
	public void ownershipNbtRoundTrips() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());

		// fresh pipe: ownable persists as false (upstream :77 setBoolean unconditional),
		// owner is ABSENT (upstream :78 `if (mOwner != null)` non-null guard)
		CompoundTag tFresh = tPipe.saveWithoutMetadata();
		assertTrue(tFresh.contains(GTFluidPipeBlockEntity.NBT_OWNABLE, Tag.TAG_ANY_NUMERIC),
				"gt.ownable always written, the upstream :77 setBoolean form");
		assertFalse(tFresh.getBoolean(GTFluidPipeBlockEntity.NBT_OWNABLE));
		assertFalse(tFresh.hasUUID(GTFluidPipeBlockEntity.NBT_OWNER),
				"gt.owner absent on an unowned pipe — the upstream :78 non-null guard");

		// locked pipe: both keys round-trip, the UUID through the vanilla int-array form
		tPipe.mOwnable = true;
		tPipe.mOwner = OWNER;
		CompoundTag tSaved = tPipe.saveWithoutMetadata();
		assertTrue(tSaved.getBoolean(GTFluidPipeBlockEntity.NBT_OWNABLE));
		assertTrue(tSaved.hasUUID(GTFluidPipeBlockEntity.NBT_OWNER));
		assertEquals(Tag.TAG_INT_ARRAY, tSaved.getTagType(GTFluidPipeBlockEntity.NBT_OWNER),
				"the vanilla putUUID storage form (NbtUtils.createUUID = IntArrayTag, both legs)");
		assertEquals(OWNER, tSaved.getUUID(GTFluidPipeBlockEntity.NBT_OWNER));

		GTFluidPipeBlockEntity tBack = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertTrue(tBack.mOwnable, "gt.ownable reads back");
		assertEquals(OWNER, tBack.mOwner, "gt.owner reads back through hasUUID/getUUID");

		// the hasUUID read guard: an ownable tag without an owner key loads owner=null
		CompoundTag tNoOwner = tPipe.saveWithoutMetadata();
		tNoOwner.remove(GTFluidPipeBlockEntity.NBT_OWNER);
		GTFluidPipeBlockEntity tPartial = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		tPartial.load(tNoOwner);
		assertTrue(tPartial.mOwnable);
		assertNull(tPartial.mOwner, "the hasUUID guard — a missing owner key never fabricates a UUID");

		// the ownable=0 reset form clears the owner on save (the removeFoam reset form)
		tPipe.mOwnable = false;
		tPipe.mOwner = null;
		CompoundTag tReset = tPipe.saveWithoutMetadata();
		assertFalse(tReset.getBoolean(GTFluidPipeBlockEntity.NBT_OWNABLE));
		assertFalse(tReset.hasUUID(GTFluidPipeBlockEntity.NBT_OWNER));
	}

	// ---------------------------------------------------------------------------
	// ④ the placement support-side neighbour gate (upstream 09Connector:86 return T)
	// ---------------------------------------------------------------------------

	@Test
	public void placementDeniesLockedSupportNeighbour() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// a locked support pipe west of A; A is placed "against" its EAST face (clicked face
		// 5) → the support side is OPOS[5]=4=WEST — exactly the upstream :85-86 probe
		GTFluidPipeBlockEntity tSupport = locked(tLevel, POS_A.west(), OWNER);
		tSupport.mConnections = TileEntityBase09Connector.SBIT[1]; // a pre-existing bit, untouched by the denial

		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);
		tPipe.onPlaced((byte)5, null); // console identity = null = foreign

		assertEquals(0, tPipe.getConnections(),
				"the locked support denies the first connect — all six sides stay 0 (upstream :86 return T)");
		assertFalse(tPipe.connected((byte)4));
		assertEquals(TileEntityBase09Connector.SBIT[1], tSupport.getConnections(),
				"the support's own bits are untouched by the denial");

		// the owner of the support gets through — same geometry, identity = the support's owner
		GTFluidPipeBlockEntity tPipe2 = place(tLevel, POS_A);
		tPipe2.onPlaced((byte)5, OWNER);
		assertTrue(tPipe2.connected((byte)4), "the support's owner connects on placement (allowInteraction opens)");
		assertTrue(tSupport.connected((byte)5), "the symmetric handshake ran — the support carries the reciprocal bit");
	}

	@Test
	public void placementAgainstUnlockedSupportStillConnects() {
		// the zero-regression arm: default (ownable=false) support pipes keep the plain
		// placement behaviour byte for byte
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tSupport = place(tLevel, POS_A.west());
		GTFluidPipeBlockEntity tPipe = place(tLevel, POS_A);
		tPipe.onPlaced((byte)5, null);
		assertTrue(tPipe.connected((byte)4), "an unlocked support connects exactly as before");
		assertTrue(tSupport.connected((byte)5));
	}

	// ---------------------------------------------------------------------------
	// ⑤ the toggle gates: self (the 06Covers:141 counterpart) + target-side neighbour
	//    (upstream 09Connector:75, connect AND disconnect arms)
	// ---------------------------------------------------------------------------

	@Test
	public void toggleSelfGateDeniesNonOwner() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tPipe = locked(tLevel, POS_A, OWNER);

		// null identity (the console / a stranger) → rejected, nothing flips
		assertFalse(tPipe.toggleConnection((byte)1, null), "the locked pipe rejects the toggle itself");
		assertFalse(tPipe.connected((byte)1));

		// the owner gets through into the same open end
		assertTrue(tPipe.toggleConnection((byte)1, OWNER));
		assertTrue(tPipe.connected((byte)1));

		// the default pipe stays ungated (zero regression arm)
		GTFluidPipeBlockEntity tPlain = place(tLevel, POS_A.east());
		assertTrue(tPlain.toggleConnection((byte)1), "ownable=false → the legacy byte-only entry behaves as before");
		assertTrue(tPlain.connected((byte)1));
	}

	@Test
	public void toggleNeighborGateCoversConnectAndDisconnect() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		GTFluidPipeBlockEntity tA = place(tLevel, POS_A);
		GTFluidPipeBlockEntity tB = locked(tLevel, POS_A.west(), OWNER);

		// DISCONNECT arm: A↔B pre-connected, the locked B denies A's null toggle on that side
		assertTrue(tA.connect((byte)4, true), "the locked neighbour only gates the TOOL toggle, not connect() itself");
		assertTrue(tB.connected((byte)5));
		assertFalse(tA.toggleConnection((byte)4, null), "the locked target-side neighbour denies the disconnect arm");
		assertTrue(tA.connected((byte)4), "nothing flipped");
		assertTrue(tB.connected((byte)5), "the handshake partner is untouched");

		// the neighbour's owner gets through — disconnect arm
		assertTrue(tA.toggleConnection((byte)4, OWNER), "the neighbour's owner toggles the disconnect");
		assertFalse(tA.connected((byte)4));
		assertFalse(tB.connected((byte)5), "the mirrored handshake ran");

		// CONNECT arm: the same locked neighbour denies the null toggle again
		assertFalse(tA.toggleConnection((byte)4, null), "the locked target-side neighbour denies the connect arm");
		assertFalse(tA.connected((byte)4));

		// and the owner's connect arm passes
		assertTrue(tA.toggleConnection((byte)4, OWNER));
		assertTrue(tA.connected((byte)4));
		assertTrue(tB.connected((byte)5));

		// a foreign identity is denied on both arms too
		assertFalse(tA.toggleConnection((byte)4, FOREIGN));
		assertTrue(tA.connected((byte)4), "the foreign denial does not flip the existing connection");
	}

	// ---------------------------------------------------------------------------
	// ⑥ the static break-gate seam (deny → 0.0F / allow → the super value unchanged;
	//    the upstream 01Root:941-943 getPlayerRelativeBlockHardness counterpart)
	// ---------------------------------------------------------------------------

	@Test
	public void ownerDestroyProgressSeamTruthTable() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		float tSuper = 0.75F; // the caller-computed super progress stands in for the real one

		// a non-pipe position (no BE / foreign BE) never gates
		assertEquals(tSuper, GTFluidPipeBlockEntity.ownerDestroyProgress(null, tSuper, null), 1e-9F);
		assertEquals(tSuper, GTFluidPipeBlockEntity.ownerDestroyProgress(null, tSuper, FOREIGN), 1e-9F);

		// the default pipe passes the super value through for any identity
		tPipe.mOwnable = false;
		tPipe.mOwner = null;
		assertEquals(tSuper, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, tSuper, null), 1e-9F);
		assertEquals(tSuper, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, tSuper, FOREIGN), 1e-9F);

		// locked: deny = 0.0F (progress never accrues — the upstream :943 `: 0` arm).
		// Task p25-c-foam-pipe-spray: the lock arms through the dried foam (the third clause).
		tPipe.mOwnable = true;
		tPipe.mOwner = OWNER;
		tPipe.mFoamDried = true;
		assertEquals(0.0F, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, tSuper, null), 1e-9F,
				"locked × null identity → 0.0F");
		assertEquals(0.0F, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, tSuper, FOREIGN), 1e-9F,
				"locked × foreign → 0.0F");

		// locked: the owner and the null-owner pipe pass through
		assertEquals(tSuper, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, tSuper, OWNER), 1e-9F,
				"locked × owner → the super value unchanged");
		tPipe.mOwner = null;
		assertEquals(tSuper, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, tSuper, null), 1e-9F,
				"ownable with a null owner → everyone breaks");
	}
}
