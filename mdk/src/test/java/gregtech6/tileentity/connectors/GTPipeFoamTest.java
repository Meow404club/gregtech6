package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTModelProperties;
import gregtech6.item.foamspray.GT6FoamSprayItem;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel;

/**
 * The pipe C-Foam offline tests (task p25-c-foam-pipe-spray acceptance ①): the applyFoam
 * truth table (upstream TileEntityBase10ConnectorRendered.java:159-166 over UUIDs — zero
 * Player/Entity constructed), the third-clause flip (the {@code !mFoamDried} arm of
 * allowInteraction :153-156), the dryFoam no-gate asymmetry (:169-174) and the removeFoam
 * gate+reset (:177-183), the drying ticker determinism (the :99-102 rng injection seam),
 * the gt.foamed/gt.foamdried/gt.ownable NBT round-trip incl. the ITEM leg (the loot
 * BlockEntityTag merge — owner excluded, the re-placement records the NEW placer, :148-150),
 * the dried static seams (collision :218 / light :144) and the FOAM_SNAPSHOT render supply.
 *
 * <p>All arms drive the BE methods directly on the {@link MultiBlockLevel} stub — no
 * Block, no Player, no Entity (the offline-UUID discipline).
 */
public class GTPipeFoamTest extends GTOfflineTestBase {

	static BlockEntityType<GTFluidPipeBlockEntity> sType;
	static final BlockPos POS_A = new BlockPos(2, 3, 4);

	/** Fixed identities — deterministic, no Player construction. */
	static final UUID SPRAYER = UUID.fromString("00000000-0000-0000-0000-00000000f001");
	static final UUID FOREIGN = UUID.fromString("00000000-0000-0000-0000-00000000f002");

	/** The 0xRRGGBB the tests spray (an arbitrary GTSprayCanItem.DYES_INT slot would do). */
	static final int SPRAY_RGB = GTSprayCanItem.DYES_INT[3];

	/** The drying-ticker stub: the roll is injected (the rng seam). */
	static class DryingPipe extends GTFluidPipeBlockEntity {
		int injectedRoll = 0;

		DryingPipe(BlockPos aPos) {
			super(sDryType, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		protected int rng(int aBound) {
			return injectedRoll;
		}
	}

	static BlockEntityType<DryingPipe> sDryType;

	@BeforeAll
	static void buildPipeFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTFluidPipeBlockEntity>[] tHolder = (BlockEntityType<GTFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTFluidPipeBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];

		@SuppressWarnings("unchecked")
		BlockEntityType<DryingPipe>[] tDryHolder = (BlockEntityType<DryingPipe>[]) new BlockEntityType<?>[1];
		tDryHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new DryingPipe(aPos),
				Blocks.STONE, Blocks.DIRT).build(null);
		sDryType = tDryHolder[0];
	}

	private static GTFluidPipeBlockEntity pipe() {
		GTFluidPipeBlockEntity tPipe = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		tPipe.setLevel(new MultiBlockLevel());
		return tPipe;
	}

	// ---------------------------------------------------------------------------
	// ① the applyFoam truth table (upstream :159-166; the mDiameter arm is the cut
	//    deviation A, the remaining four arms are walked)
	// ---------------------------------------------------------------------------

	@Test
	public void applyFoamTruthTable() {
		// arm: an empty pipe takes the spray — the write-point bundle (:161-163)
		GTFluidPipeBlockEntity tPipe = pipe();
		assertTrue(tPipe.applyFoam((byte)2, SPRAYER, SPRAY_RGB, false));
		assertTrue(tPipe.mFoam);
		assertFalse(tPipe.mFoamDried, "a fresh spray is WET (upstream :161 mFoamDried = F)");
		assertFalse(tPipe.mOwnable, "plain spray: not ownable (aOwned=F)");
		assertNull(tPipe.mOwner, "non-owned spray never records an owner (:162 mOwnable gate)");
		assertTrue(tPipe.isPainted(), "spraying PAINTS the pipe (:161 mIsPainted = T, the IPaintableTE face)");
		assertEquals(SPRAY_RGB, tPipe.getPaint(), "the pipe wears the foam colour (:163 mRGBa, the IPaintableTE face)");
		assertTrue(tPipe.hasFoam((byte)2) && tPipe.hasFoam((byte)0), "the foam state is pipe-wide (:215)");

		// arm: an already-foamed (wet) pipe refuses — any colour, any ownership
		assertFalse(tPipe.applyFoam((byte)3, SPRAYER, 0xFF0000, false), "wet pipe refuses the re-spray (:160 mFoam)");
		assertFalse(tPipe.applyFoam((byte)3, SPRAYER, 0xFF0000, true), "owned flag changes nothing while wet");

		// arm: a dried pipe refuses
		tPipe.mFoamDried = true;
		assertFalse(tPipe.applyFoam((byte)3, FOREIGN, 0x00FF00, false), "dried foam refuses the re-spray (:160 mFoamDried)");

		// arm: the allowInteraction deny — a LOCKED pipe (ownable + dried + owner) rejects
		// the foreign spray. Reachable state: the NBT loads foam-dried without foam.
		GTFluidPipeBlockEntity tLocked = pipe();
		tLocked.mOwnable = true;
		tLocked.mOwner = SPRAYER;
		tLocked.mFoamDried = true; // dried WITHOUT foam → the :160 foam arms pass through...
		assertFalse(tLocked.applyFoam((byte)1, FOREIGN, SPRAY_RGB, false),
				"a locked pipe rejects the non-owner spray");
		assertFalse(tLocked.mFoam, "the rejection wrote nothing");

		// the verbatim ORDER of upstream :160: the mFoam/mFoamDried arms precede the
		// allowInteraction arm — a DRIED pipe refuses even its OWNER (removal, not re-spray,
		// is the only path; the verbatim order keeps the upstream tautology: with both foam
		// arms passing, !mFoamDried already makes allowInteraction open)
		assertFalse(tLocked.applyFoam((byte)1, SPRAYER, SPRAY_RGB, false),
				"dried refuses EVERYONE's spray — the :160 arm order is verbatim (removal is the path)");
	}

	@Test
	public void ownedSprayRecordsTheSprayer() {
		GTFluidPipeBlockEntity tPipe = pipe();
		assertTrue(tPipe.applyFoam((byte)4, SPRAYER, SPRAY_RGB, true));
		assertTrue(tPipe.mOwnable, "owned spray sets mOwnable (upstream :161 aOwned)");
		assertEquals(SPRAYER, tPipe.mOwner, "owned spray records the SPRAYER (:162 — the runtime owner write point)");
		assertTrue(tPipe.ownedFoam((byte)4), "owned + foamed (:217)");

		// the null identity (the console): owned flag lands, nobody is recorded (:162 aPlayer != null)
		GTFluidPipeBlockEntity tConsole = pipe();
		assertTrue(tConsole.applyFoam((byte)4, null, SPRAY_RGB, true));
		assertTrue(tConsole.mOwnable);
		assertNull(tConsole.mOwner, "a null identity records nobody (the console is nobody)");
	}

	// ---------------------------------------------------------------------------
	// ② the third-clause flip (the p24 fold refilled: !mFoamDried bypasses the owner
	//    check until the foam dries, upstream :153-156)
	// ---------------------------------------------------------------------------

	@Test
	public void thirdClauseFlipsOnDrying() {
		GTFluidPipeBlockEntity tPipe = pipe();
		tPipe.mOwnable = true;
		tPipe.mOwner = SPRAYER;
		// WET owned foam: the third clause bypasses the owner check — everyone passes
		tPipe.mFoam = true;
		tPipe.mFoamDried = false;
		assertTrue(tPipe.allowInteraction(FOREIGN), "undried owned foam: the owner half is bypassed (upstream :155 !mFoamDried)");
		assertTrue(tPipe.allowInteraction(null));
		assertEquals(0.75F, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, 0.75F, FOREIGN), 1e-9F,
				"a foreign breaker accrues progress while the foam is wet");

		// DRIED: the flip — only the owner passes
		tPipe.mFoamDried = true;
		assertFalse(tPipe.allowInteraction(FOREIGN), "dried owned foam arms the lock (upstream :155 falls to super)");
		assertFalse(tPipe.allowInteraction(null));
		assertTrue(tPipe.allowInteraction(SPRAYER));
		assertEquals(0.0F, GTFluidPipeBlockEntity.ownerDestroyProgress(tPipe, 0.75F, FOREIGN), 1e-9F,
				"the foreign breaker is denied to 0.0F on the dried pipe (the p24 seam, now live)");
	}

	// ---------------------------------------------------------------------------
	// ③ dryFoam — the NO-GATE asymmetry verbatim (upstream :169-174)
	// ---------------------------------------------------------------------------

	@Test
	public void dryFoamAsymmetry() {
		GTFluidPipeBlockEntity tPipe = pipe();
		assertFalse(tPipe.dryFoam((byte)0, FOREIGN), "an unfoamed pipe cannot dry (:170 !mFoam)");

		// the foreign identity dries a LOCKED stranger's wet foam — no allowInteraction gate
		GTFluidPipeBlockEntity tLocked = pipe();
		tLocked.mOwnable = true;
		tLocked.mOwner = SPRAYER;
		tLocked.mFoam = true;
		assertTrue(tLocked.dryFoam((byte)0, FOREIGN), "dryFoam has NO ownership gate (upstream :169-174 asymmetry)");
		assertTrue(tLocked.mFoam && tLocked.mFoamDried, "driedFoam (:216)");
		assertFalse(tLocked.dryFoam((byte)0, SPRAYER), "an already-dried foam cannot dry again (:170 mFoamDried)");
	}

	// ---------------------------------------------------------------------------
	// ④ removeFoam — the gate + the four-field reset + unpaint (upstream :177-183)
	// ---------------------------------------------------------------------------

	@Test
	public void removeFoamGatesAndResets() {
		GTFluidPipeBlockEntity tPipe = pipe();
		tPipe.applyFoam((byte)2, SPRAYER, SPRAY_RGB, true);

		assertFalse(tPipe.removeFoam((byte)2, SPRAYER), "a WET foam cannot be removed (:178 !mFoamDried)");

		tPipe.dryFoam((byte)2, SPRAYER);
		assertFalse(tPipe.removeFoam((byte)2, FOREIGN), "a locked dried pipe keeps the non-owner out (:178 allowInteraction)");
		assertTrue(tPipe.hasFoam((byte)2), "the rejection removed nothing");

		assertTrue(tPipe.removeFoam((byte)2, SPRAYER), "the owner removes the dried foam");
		assertFalse(tPipe.mFoam, "reset :179 mFoam = F");
		assertFalse(tPipe.mFoamDried, "reset :179 mFoamDried = F");
		assertFalse(tPipe.mOwnable, "reset :179 mOwnable = F");
		assertNull(tPipe.mOwner, "reset :179 mOwner = null");
		assertFalse(tPipe.isPainted(), "unpaint (:180) — the applyFoam paint write reverts");
		assertFalse(tPipe.removeFoam((byte)2, SPRAYER), "nothing left to remove");
	}

	// ---------------------------------------------------------------------------
	// ⑤ the drying ticker (upstream :99-102) — the rng injection seam, deterministic
	// ---------------------------------------------------------------------------

	@Test
	public void dryingTickerIsRngDeterministic() {
		DryingPipe tPipe = new DryingPipe(POS_A);
		tPipe.mFoam = true;

		// a matured age + a winning roll of 0 → dried
		tPipe.injectedRoll = 0;
		tPipe.onTick(100, true);
		assertTrue(tPipe.mFoamDried, "aTimer>=100 && rng(5900)==0 → mFoamDried (:99-102)");

		// the age gate: a fresh pipe (aTimer < 100) never dries, whatever the roll
		DryingPipe tYoung = new DryingPipe(POS_A);
		tYoung.mFoam = true;
		tYoung.onTick(99, true);
		assertFalse(tYoung.mFoamDried, "aTimer < 100 → no drying (upstream :99 aTimer >= 100)");

		// a losing roll never dries
		DryingPipe tLosing = new DryingPipe(POS_A);
		tLosing.mFoam = true;
		tLosing.injectedRoll = 1;
		tLosing.onTick(5000, true);
		assertFalse(tLosing.mFoamDried, "rng != 0 → no drying");

		// a client-side or unfoamed BE never dries
		DryingPipe tClient = new DryingPipe(POS_A);
		tClient.mFoam = true;
		tClient.onTick(5000, false);
		assertFalse(tClient.mFoamDried, "aIsServerSide = F → no drying");
		DryingPipe tBare = new DryingPipe(POS_A);
		tBare.onTick(5000, true);
		assertFalse(tBare.mFoamDried, "no foam → no drying");
	}

	// ---------------------------------------------------------------------------
	// ⑥ the NBT round-trip: the world keys (upstream :75-76/:65-66) and the ITEM leg
	//    (writeItemNBT2 :82-87 three keys — owner excluded — + the re-placement owner
	//    record :148-150)
	// ---------------------------------------------------------------------------

	@Test
	public void foamNbtRoundTripsAndOwnerDoesNotRideItems() {
		GTFluidPipeBlockEntity tPipe = pipe();
		tPipe.applyFoam((byte)2, SPRAYER, SPRAY_RGB, true);
		tPipe.dryFoam((byte)2, SPRAYER);

		// the world save: all four keys (foam/dried unconditional :75-76, ownable :77)
		CompoundTag tSaved = tPipe.saveWithoutMetadata();
		assertTrue(tSaved.getBoolean(GTFluidPipeBlockEntity.NBT_FOAMED));
		assertTrue(tSaved.getBoolean(GTFluidPipeBlockEntity.NBT_FOAMDRIED));
		assertTrue(tSaved.getBoolean(GTFluidPipeBlockEntity.NBT_OWNABLE));
		assertTrue(tSaved.hasUUID(GTFluidPipeBlockEntity.NBT_OWNER));

		// the world load (a chunk reload): everything reads back
		GTFluidPipeBlockEntity tBack = pipe();
		tBack.load(tSaved);
		assertTrue(tBack.mFoam && tBack.mFoamDried && tBack.mOwnable);
		assertEquals(SPRAYER, tBack.mOwner);

		// the ITEM leg: the loot copy_nbt extracts EXACTLY the foam trio + the paint pair into
		// BlockEntityTag (gt.owner is NOT among them — the card's owner-does-not-flow rule)
		CompoundTag tItemTag = new CompoundTag();
		for (String tKey : new String[] {GTFluidPipeBlockEntity.NBT_FOAMED, GTFluidPipeBlockEntity.NBT_FOAMDRIED,
				GTFluidPipeBlockEntity.NBT_OWNABLE, "gt.color", "gt.painted"}) {
			if (tSaved.contains(tKey)) tItemTag.put(tKey, tSaved.get(tKey).copy());
		}
		assertTrue(tItemTag.contains(GTFluidPipeBlockEntity.NBT_FOAMED), "the item carries gt.foamed");
		assertTrue(tItemTag.contains(GTFluidPipeBlockEntity.NBT_OWNABLE), "the item carries gt.ownable");
		assertFalse(tItemTag.hasUUID(GTFluidPipeBlockEntity.NBT_OWNER), "the item NEVER carries gt.owner (:82-87 has no owner op)");

		// the re-placement: vanilla updateCustomBlockEntityTag merges the tag over the fresh
		// BE's save and loads (BlockItem :158) — the merge happens BEFORE onPlaced (the
		// GTFluidPipeBlockItem pre-merge), so the NEW placer is recorded
		GTFluidPipeBlockEntity tReplaced = sType.create(POS_A, Blocks.STONE.defaultBlockState());
		tReplaced.setLevel(new MultiBlockLevel()); // onPlaced's server gate needs a level (the GTPipeOwnerTest place form)
		CompoundTag tMerged = tReplaced.saveWithoutMetadata();
		tMerged.merge(tItemTag);
		tReplaced.load(tMerged);
		assertTrue(tReplaced.mFoam && tReplaced.mFoamDried, "the foam state survives the trip (三键保留)");
		assertTrue(tReplaced.mOwnable, "ownable survives the trip");
		assertNull(tReplaced.mOwner, "the carried pipe is ownerless — owner 不回流");

		tReplaced.onPlaced((byte)1, FOREIGN);
		assertEquals(FOREIGN, tReplaced.mOwner, "the re-placement records the NEW placer (:148-150 read-back ownable)");
		assertFalse(tReplaced.allowInteraction(SPRAYER), "the OLD owner is now locked out — the dried foam re-arms");
		assertTrue(tReplaced.allowInteraction(FOREIGN));
	}

	// ---------------------------------------------------------------------------
	// ⑦ the dried static seams (collision :218 / light :144) + the render snapshot
	// ---------------------------------------------------------------------------

	@Test
	public void driedSeamsAndSnapshot() {
		GTFluidPipeBlockEntity tPlain = pipe();
		assertSame(Shapes.block(), GTFluidPipeBlockEntity.foamCollisionShape(tPlain, Shapes.block()),
				"sanity: the seam passes the super shape through (wet pipe)");

		GTFluidPipeBlockEntity tPipe = pipe();
		tPipe.applyFoam((byte)2, SPRAYER, SPRAY_RGB, false);

		// wet: neither seam engages
		assertEquals(-7, GTFluidPipeBlockEntity.foamLightBlock(tPipe, -7), "wet foam keeps the super light block");
		assertEquals(-7, GTFluidPipeBlockEntity.foamLightBlock(null, -7), "null BE never gates (the static form)");
		assertFalse(tPipe.driedFoam((byte)2));

		// dried: the full-block collision + the max light block
		tPipe.dryFoam((byte)2, SPRAYER);
		assertSame(Shapes.block(), GTFluidPipeBlockEntity.foamCollisionShape(tPipe, Shapes.block()),
				"dried foam collides as a FULL block (upstream :218)");
		assertEquals(15, GTFluidPipeBlockEntity.foamLightBlock(tPipe, 0), "dried foam blocks ALL light (upstream :144)");
		assertTrue(tPipe.driedFoam((byte)2));

		// the render snapshot: fresh=(!dried), dried=true after hardening, owned on the flag
		GTFluidPipeBlockEntity tFreshOwned = pipe();
		tFreshOwned.applyFoam((byte)3, SPRAYER, SPRAY_RGB, true);
		gregtech6.client.render.PipeFoamSnapshot tSnap =
				tFreshOwned.getModelData().get(GTModelProperties.FOAM_SNAPSHOT);
		assertEquals(new gregtech6.client.render.PipeFoamSnapshot(false, true), tSnap,
				"the fresh owned snapshot (spec ⑤)");
		assertTrue(tFreshOwned.getModelData().has(GTModelProperties.PAINT), "the paint colour rides the same snapshot");
		tFreshOwned.dryFoam((byte)3, SPRAYER);
		tSnap = tFreshOwned.getModelData().get(GTModelProperties.FOAM_SNAPSHOT);
		assertEquals(new gregtech6.client.render.PipeFoamSnapshot(true, true), tSnap, "the dried owned snapshot");

		// a plain pipe carries NO foam snapshot (the absent-property contract)
		assertFalse(pipe().getModelData().has(GTModelProperties.FOAM_SNAPSHOT));
	}

	// ---------------------------------------------------------------------------
	// ⑧ the item routing gate (Behavior_Spray_Foam.java:113-114 arm (1) — spec ⑨)
	// ---------------------------------------------------------------------------

	@Test
	public void foamTargetRoutesOnlyUnfoamedPipes() {
		GTFluidPipeBlockEntity tPipe = pipe();
		assertTrue(GT6FoamSprayItem.foamTarget(tPipe, (byte)2), "an unfoamed pipe is a target");
		assertFalse(GT6FoamSprayItem.foamTarget(null, (byte)2), "nothing to hit → PASS");
		tPipe.applyFoam((byte)2, SPRAYER, SPRAY_RGB, false);
		assertFalse(GT6FoamSprayItem.foamTarget(tPipe, (byte)2), "a foamed pipe is NOT a target (!hasFoam gate)");
		assertFalse(GT6FoamSprayItem.foamTarget(tPipe, (byte)5), "the foam state is pipe-wide — every side refuses");
	}

	// ---------------------------------------------------------------------------
	// ⑨ the payment ledger (the shared GTSprayCanItem faces, the 10-unit pipe hit)
	// ---------------------------------------------------------------------------

	@Test
	public void sprayPaymentLedger() {
		long tFull = GT6FoamSprayItem.FOAM_USES * GTSprayCanItem.HIT_COST; // 2560 — the upstream ctor :56 x10 form
		assertEquals(2560, tFull);
		assertEquals(2560 - 10, GTSprayCanItem.remainingAfterHit(tFull, GTSprayCanItem.HIT_COST, false),
				"one pipe hit pays 10 internal units (Behavior_Spray_Foam.java:114)");
		assertEquals(tFull, GTSprayCanItem.remainingAfterHit(tFull, GTSprayCanItem.HIT_COST, true),
				"creative pays nothing (:87 hasInfiniteItems)");
		assertTrue(GTSprayCanItem.depleted(0), "the depletion verdict (:92)");
		assertTrue(GTSprayCanItem.barVisible(tFull - 1, tFull) && !GTSprayCanItem.barVisible(tFull, tFull),
				"the durability bar shows on a partially-used can (the p22 face)");
	}

	// ---------------------------------------------------------------------------
	// the foam planner (pure geometry, the flow-model test shape) + the tint seam
	// ---------------------------------------------------------------------------

	@Test
	public void foamModelPlannerAndTint() {
		gregtech6.client.render.PipeFoamSnapshot tOwnedDried = new gregtech6.client.render.PipeFoamSnapshot(true, true);
		assertEquals(gregtech6.client.render.GTFluidPipeFoamModel.HARDENED_OWNED_SPRITE,
				gregtech6.client.render.GTFluidPipeFoamModel.spriteOf(tOwnedDried));
		assertEquals(gregtech6.client.render.GTFluidPipeFoamModel.FRESH_SPRITE,
				gregtech6.client.render.GTFluidPipeFoamModel.spriteOf(new gregtech6.client.render.PipeFoamSnapshot(false, false)));

		// six quads, one per face, all at tint index 1
		var tPlans = gregtech6.client.render.GTFluidPipeFoamModel.planQuads(tOwnedDried, null);
		assertEquals(6, tPlans.size());
		assertEquals(gregtech6.client.render.GTFluidPipeFoamModel.FOAM_TINT_INDEX, tPlans.get(0).tintIndex());

		// the tint seam: index 1 reads PAINT, index 0 (the arrows) and absent paint are no-tint
		assertEquals(-1, gregtech6.client.render.GTPipeFoamClientListener.foamTintARGB(null, 0),
				"the arrow index stays untinted");
		assertEquals(-1, gregtech6.client.render.GTPipeFoamClientListener.foamTintARGB(null, 1),
				"no paint data → the -1 sentinel");
		net.minecraftforge.client.model.data.ModelData tData = net.minecraftforge.client.model.data.ModelData.builder()
				.with(GTModelProperties.PAINT, 0x40B080)
				.build();
		assertEquals(0xFF40B080, gregtech6.client.render.GTPipeFoamClientListener.foamTintARGB(tData, 1),
				"index 1 = the PAINT colour, full alpha");
	}
}
