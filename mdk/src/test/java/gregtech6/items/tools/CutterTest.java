package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.covers.CoverData;
import gregtech6.covers.CoverRegistry;
import gregtech6.covers.TileEntityOvenCoverProbe;
import gregtech6.covers.covers.AbstractCoverDefault;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;
import gregtech6.util.UT6;

/**
 * The cutter offline tests (task p10-tool-cutter acceptance, offline half): the
 * CUTTER_ID parity with the emitter's strong-gate string (the emitter file itself stays
 * zero-diff), the nine-grid wiring (the resolution rides the already-tested
 * {@link UT6#getSideWrenching} — the full six-face × nine-region table is
 * UT6SideWrenchingTest's pin, this test pins that the cutter CALLS it), the
 * connected?disconnect:connect toggle (upstream TileEntityBase09Connector:70-79, the
 * :76 10000/0 returns and the connections before/after flip) and the durability
 * mapping. The live give/place/cut/re-cut chain rides the RCON acceptance
 * (/gt6tool cut + /gt6wire stat).
 */
public class CutterTest extends GTOfflineTestBase {

	/** Test hook (the GTWireBlockEntityTest.TestWire shape — the mod-Item wall does not cover BEs). */
	public static class TestWire extends GTWireBlockEntity {
		public TestWire(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
	}

	/**
	 * The toggle-test level: no block entities anywhere, AIR at the connect target (so
	 * the base connect handshake takes the upstream :141 open-end branch) and BRICKS at
	 * the wire's own position (no CONNECTIONS property — onConnectionChange skips the
	 * BlockState write, the GTWireBlockEntityTest premise).
	 */
	public static class CutterLevel extends GTMachinesOfflineTestBase.MachineLevel {
		public final BlockPos mAirPos;

		public CutterLevel(BlockPos aAirPos) {
			super(new GTRecipesOfflineTestBase.TestRecipeManager());
			mAirPos = aAirPos;
		}

		@Override
		public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos aPos) {
			return null;
		}

		@Override
		public BlockState getBlockState(BlockPos aPos) {
			return aPos.equals(mAirPos) ? Blocks.AIR.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
		}
	}

	static BlockEntityType<TestWire> sWireType;
	static BlockEntityType<TileEntityOvenCoverProbe> sProbeType;

	static final BlockPos WIRE_POS = new BlockPos(3, 4, 5);
	static final BlockPos COVER_POS = new BlockPos(2, 2, 3);

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		@SuppressWarnings("unchecked")
		BlockEntityType<TestWire>[] tWireHolder = (BlockEntityType<TestWire>[]) new BlockEntityType<?>[1];
		tWireHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestWire(tWireHolder[0], aPos, aState), Blocks.STONE).build(null);
		sWireType = tWireHolder[0];
		sProbeType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOvenCoverProbe(sProbeType, aPos, aState),
				Blocks.BRICKS).build(null);
		// NOTE: the GTCutterItem instance itself is NOT constructed — a mod Item cannot be
		// built in this bootstrapped-and-frozen JVM (Item.java:61 intrusive holder); the
		// classifier pins through the static seam, the live item rides the RCON chain.
	}

	@BeforeEach
	void putCoverFixtures() {
		CoverRegistry.reset();
		CoverRegistry.put(Items.BRICK, new CoverRedstoneEmitter());
	}

	@AfterEach
	void clearCoverFixtures() {
		CoverRegistry.reset();
	}

	/** A wire on the AIR-target level (connects towards north/2). */
	private static TestWire connectableWire() {
		TestWire tWire = sWireType.create(WIRE_POS, Blocks.STONE.defaultBlockState());
		tWire.setLevel(new CutterLevel(WIRE_POS.north()));
		return tWire;
	}

	@Test
	void cutterIdIsParityPinnedWithTheEmitter() {
		assertEquals("gt6_cutter", GT6ToolActions.CUTTER.name(), "the ADR-pinned action name");
		assertEquals("cutter", GT6ToolActions.CUTTER_ID, "the upstream CS.TOOL_cutter dispatch id");
		assertEquals("cutter", CoverRedstoneEmitter.TOOL_CUTTER,
				"PARITY — the emitter strong-gate string and the cutter id are the same upstream constant");
		assertEquals(GT6ToolActions.CUTTER_ID, CoverRedstoneEmitter.TOOL_CUTTER, "the literal forms agree");
		assertEquals(512, GTCutterItem.DURABILITY_POINTS, "the single steel tier");
		assertEquals(10000, GTCutterItem.TOOL_DAMAGE_PER_CUT, "the upstream :76 return");
	}

	@Test
	void classifierExposesCutterOnly() {
		assertTrue(GTCutterItem.classifies(GT6ToolActions.CUTTER), "the cutter action classifies");
		assertFalse(GTCutterItem.classifies(ToolActions.HOE_DIG), "RED LINE — never a hoe: the three wrench predicates must not fire");
		assertFalse(GTCutterItem.classifies(GT6ToolActions.CROWBAR), "the cutter is not a crowbar");
	}

	@Test
	void nineGridResolutionRidesGetSideWrenching() {
		// the wiring pin: every sampled click resolves exactly as the UT6 function the
		// p4 card already table-tested (UT6SideWrenchingTest owns the full 6×9 table)
		BlockPos tPos = WIRE_POS;
		for (byte tFace = 0; tFace < 6; tFace++) {
			for (float[] tHit : new float[][] {{0.5F, 0.5F}, {0.1F, 0.5F}, {0.9F, 0.5F}, {0.5F, 0.1F},
					{0.5F, 0.9F}, {0.1F, 0.1F}, {0.9F, 0.9F}, {0.25F, 0.75F}}) {
				// the synthetic world-space hit: pos origin + the in-face offsets
				Vec3 tLocation = new Vec3(tPos.getX() + tHit[0], tPos.getY() + tHit[1], tPos.getZ() + 0.5);
				byte tResolved = GTCutterItem.targetSide(tFace, tLocation, tPos);
				byte tExpected = UT6.getSideWrenching(tFace, tHit[0], tHit[1], 0.5F);
				assertEquals(tExpected, tResolved, "face " + tFace + " hit " + tHit[0] + "/" + tHit[1]);
			}
		}
		// the spot shapes: centre hit = the clicked face itself, edge = the adjacent face,
		// corner = the OPOS fallback (upstream :73 semantics)
		Vec3 tCentre = new Vec3(tPos.getX() + 0.5, tPos.getY() + 0.5, tPos.getZ() + 0.5);
		assertEquals(2, GTCutterItem.targetSide((byte) 2, tCentre, tPos), "centre hit keeps the clicked face");
		assertEquals(4, GTCutterItem.targetSide((byte) 2,
				new Vec3(tPos.getX() + 0.1, tPos.getY() + 0.5, tPos.getZ() + 0.5), tPos), "x edge picks west");
		assertEquals(UT6.OPOS[2], GTCutterItem.targetSide((byte) 2,
				new Vec3(tPos.getX() + 0.1, tPos.getY() + 0.1, tPos.getZ() + 0.5), tPos), "corner falls onto OPOS");
	}

	@Test
	void wireToggleConnectsThenDisconnectsThenReconnects() {
		TestWire tWire = connectableWire();
		assertFalse(tWire.connected((byte) 2), "fresh wire starts unconnected");
		assertEquals(0, tWire.getConnections(), "mask 0 before");

		// cut 1 — the connect half (:76, not connected → connect → 10000)
		assertEquals(10000, GTCutterItem.cutterToolClick(tWire, (byte) 2), "the :76 connect return");
		assertTrue(tWire.connected((byte) 2), "connections after cut 1: the bit is set");
		assertEquals(4, tWire.getConnections(), "SBIT[2] = 4");

		// cut 2 — the disconnect half (:76, connected → disconnect → 10000)
		assertEquals(10000, GTCutterItem.cutterToolClick(tWire, (byte) 2), "the :76 disconnect return");
		assertFalse(tWire.connected((byte) 2), "connections after cut 2: the bit is cleared");
		assertEquals(0, tWire.getConnections(), "mask back to 0");

		// cut 3 — re-connect: the same handshake, the player-visible reconnect
		assertEquals(10000, GTCutterItem.cutterToolClick(tWire, (byte) 2), "the re-connect return");
		assertTrue(tWire.connected((byte) 2), "connections after cut 3: reconnected");
	}

	@Test
	void wireToggleIntoASolidNeighbourFailsWithZero() {
		// BRICKS at the cut target (the AIR slot sits on the far side) — the base connect
		// handshake takes neither the connector branch nor the :141 air/liquid branch, the
		// :76 outcome is 0 and the mask stays
		TestWire tWire = sWireType.create(WIRE_POS, Blocks.STONE.defaultBlockState());
		tWire.setLevel(new CutterLevel(WIRE_POS.south()));
		assertEquals(0, GTCutterItem.cutterToolClick(tWire, (byte) 2), "a solid neighbour denies the connect");
		assertFalse(tWire.connected((byte) 2), "no bit appeared");
		assertEquals(0, tWire.getConnections(), "mask unchanged");
	}

	@Test
	void coverRelayTogglesTheEmitterStrongGate() {
		TileEntityOvenCoverProbe tOven = new TileEntityOvenCoverProbe(sProbeType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		tOven.setLevel(new GTMachinesOfflineTestBase.MachineLevel(new GTRecipesOfflineTestBase.TestRecipeManager()));
		assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.BRICK), null, true, false), "install accepted");

		int tStrongBefore = tOven.getCovers().mValues[4];
		// a vanilla stick — a hoe-class carrier would fire the :247 dismantle substitute
		// instead of the relay (the CrowbarTest.crowbarIdPathDismantles premise)
		ItemStack tCarrier = new ItemStack(Items.STICK);

		// the relay arm: onCoverToolClick("cutter") → ICover.onToolClick → the emitter :43
		assertEquals(1000, GTCutterItem.cutterToolClick(tOven, null, tCarrier, (byte) 4, false),
				"the emitter relay return (CoverRedstoneEmitter :45)");
		assertEquals(tStrongBefore == 0 ? 1 : 0, tOven.getCovers().mValues[4], "the strong gate flipped (mValues ^ B[0])");

		// the second cut flips it back — and the 1000-unit return stays below one vanilla
		// durability point (the declared sub-point ruling), the carrier pays nothing
		assertEquals(1000, GTCutterItem.cutterToolClick(tOven, null, tCarrier, (byte) 4, false), "the toggle returns");
		assertEquals(tStrongBefore, tOven.getCovers().mValues[4], "the strong gate flipped back");
		assertEquals(0, tCarrier.getDamageValue(), "the 1000-unit relay return pays no durability point");
	}

	@Test
	void durabilitySeamPinsThePointMapping() {
		// the declared mapping: one vanilla point per full 10000 upstream units. The
		// physical hurtAndBreak payment (needs a server-side LivingEntity) is the RCON
		// chain's cutterDamage=1/512 assertion; here the vanilla hurt() seam is pinned on
		// a vanilla item (the CrowbarTest.durabilityMappingOnePointPerDismantle shape).
		assertEquals(10000, GTCutterItem.TOOL_DAMAGE_PER_CUT);
		assertEquals(512, GTCutterItem.DURABILITY_POINTS);
		ItemStack tStack = new ItemStack(Items.WOODEN_HOE);
		tStack.hurt(1, RandomSource.create(), null); // the return value is "broke", not "applied"
		assertEquals(1, tStack.getDamageValue(), "one payment = one damage value");
		assertFalse(tStack.isEmpty());
		ItemStack tDying = new ItemStack(Items.WOODEN_HOE);
		tDying.setDamageValue(tDying.getMaxDamage() - 1);
		assertTrue(tDying.hurt(1, RandomSource.create(), null), "the final unit reports the break");
	}

	/**
	 * The known_bugs 2026-09-01 #1 counting stub (p11-cutter-payperpoint): the cover arm
	 * through the CONTEXT overload must call payPerPoint exactly ONCE. Upstream ruling —
	 * the single payment sits at the item layer (Behavior_Tool.java:63 aggregates the
	 * IBlockToolable.Util.onToolClick chain and pays once), while the host relay
	 * TileEntityBase06Covers.onToolClick returns raw units from every arm (:151/:159/:162)
	 * without paying. The stub cover answers with a FULL point's worth (10000 units); the
	 * {@link GTCutterItem#sPayPerPointCalls} counter is the call count, player-independent
	 * (the offline Player wall — FluidType registry boot — blocks the hurtAndBreak route,
	 * the same reason CrowbarTest routes the physical payment to RCON).
	 */
	@Test
	void contextCoverArmPaysExactlyOncePerClick() {
		CoverRegistry.put(Items.BRICK, new StubCover()); // replaces the @BeforeEach emitter on the same key
		TileEntityOvenCoverProbe tOven = new TileEntityOvenCoverProbe(sProbeType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		CoverClickLevel tLevel = new CoverClickLevel(tOven);
		assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.BRICK), null, true, false), "install accepted");
		ItemStack tCarrier = new ItemStack(Items.WOODEN_HOE);
		UseOnContext tContext = new TestContext(tLevel, null, InteractionHand.MAIN_HAND, tCarrier,
				new BlockHitResult(new Vec3(2.0, 2.5, 3.25), Direction.WEST, COVER_POS, false));
		int tCountBefore = GTCutterItem.sPayPerPointCalls;

		// click 1 — one payPerPoint call for the whole cover-arm click
		assertEquals(10000, GTCutterItem.cutterToolClick(tContext), "the stub relay return");
		assertEquals(tCountBefore + 1, GTCutterItem.sPayPerPointCalls,
				"payPerPoint fired ONCE for the cover-arm click (the double-charge seam would read +2)");

		// click 2 — the count accumulates one per click, proving per-click singleness
		assertEquals(10000, GTCutterItem.cutterToolClick(tContext), "the stub relay return again");
		assertEquals(tCountBefore + 2, GTCutterItem.sPayPerPointCalls,
				"one call per click — the double-charge seam would read +4 here");
	}

	/**
	 * The observable-behaviour regression half of the same bug: the emitter's 1000-unit
	 * relay return stays below one point through the SAME context seam, so the carrier
	 * pays nothing — the pre-fix and post-fix observables are identical (the double call
	 * rounded to zero twice before; the single call rounds to zero once now).
	 */
	@Test
	void contextEmitterRelayStillPaysNothingBelowOnePoint() {
		TileEntityOvenCoverProbe tOven = new TileEntityOvenCoverProbe(sProbeType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		CoverClickLevel tLevel = new CoverClickLevel(tOven);
		assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.BRICK), null, true, false), "install accepted");
		ItemStack tCarrier = new ItemStack(Items.WOODEN_HOE);
		UseOnContext tContext = new TestContext(tLevel, null, InteractionHand.MAIN_HAND, tCarrier,
				new BlockHitResult(new Vec3(2.0, 2.5, 3.25), Direction.WEST, COVER_POS, false));
		int tCountBefore = GTCutterItem.sPayPerPointCalls;

		assertEquals(1000, GTCutterItem.cutterToolClick(tContext), "the emitter relay return (CoverRedstoneEmitter :45)");
		assertEquals(tCountBefore + 1, GTCutterItem.sPayPerPointCalls, "the single outer pay call");
		assertEquals(0, tCarrier.getDamageValue(), "1000 < 10000 → zero points, the pre-fix observable is unchanged");
	}

	// ------------------------------------------------------------------ fixtures

	/**
	 * The 10000-unit relay stub — an {@link AbstractCoverDefault} that answers the cutter
	 * with one full vanilla point worth of upstream units (the counting-stub carrier).
	 */
	public static class StubCover extends AbstractCoverDefault {
		@Override
		public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability,
				Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
			return GTCutterItem.TOOL_DAMAGE_PER_CUT;
		}
	}

	/** The context level — a MachineLevel that yields the covered probe at {@link #COVER_POS}. */
	public static class CoverClickLevel extends GTMachinesOfflineTestBase.MachineLevel {
		public final TileEntityOvenCoverProbe mProbe;

		public CoverClickLevel(TileEntityOvenCoverProbe aProbe) {
			super(new GTRecipesOfflineTestBase.TestRecipeManager());
			mProbe = aProbe;
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return aPos.equals(COVER_POS) ? mProbe : null;
		}
	}

	/**
	 * The context double — exposes the protected five-argument UseOnContext constructor
	 * (the public one derives the level and the held stack from a real player, which the
	 * offline JVM has no workable double for) so the tests can drive the dispatch seam
	 * with a nullable player.
	 */
	public static class TestContext extends UseOnContext {
		public TestContext(Level aLevel, @Nullable Player aPlayer, InteractionHand aHand, ItemStack aStack, BlockHitResult aHit) {
			super(aLevel, aPlayer, aHand, aStack, aHit);
		}
	}
}
