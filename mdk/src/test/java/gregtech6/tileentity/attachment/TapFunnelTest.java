package gregtech6.tileentity.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.attachment.GTFunnelBlockEntity.FunnelAccessible;
import gregtech6.tileentity.attachment.GTTapBlockEntity.TapAccessible;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The tap/funnel offline truth tables (task p12-tap-funnel-attachment acceptance a):
 *
 * <ul>
 * <li>the tap priority chain — VOIDING item drains-all-and-trashes (:82-86); the
 *     simulate probe refuses gases and acids before anything executes (:87-88); the
 *     334/667/1000 cauldron tier table (:94-104, the {@code cauldronFillPlan} pure
 *     form); the tap-to-tap one-step nesting (:110-118); the held-container fill with
 *     the source paying EXACTLY what landed (:164-171);</li>
 * <li>the funnel chain — the simulate-then-execute pour (:76-77), the empty-container
 *     give (:79-80), the gas refusal (:73), the acid gate with the acid-proof override;</li>
 * <li>the barrel hooks — {@code tapDrain}/{@code funnelFill} drain and fill the tank
 *     directly (upstream :269-276, the unsealed declared form), and the attachment-face
 *     mFacing gate: only the neighbour the attachment FACES is ever touched.</li>
 * </ul>
 *
 * <p>Offline-harness notes (the GTCrankBlockEntityTest record): the level-less fixture
 * takes the SERVER branch, the adjacency override seam ({@code mAdjacentOverride}, the
 * crank {@code setAdjacencyOverride} precedent) wires the fakes in, the gas/acid
 * verdicts are driven through the declared-minimal NAME LISTS (vanilla water being the
 * only live fluid offline — the tests inject its key and restore the lists per test),
 * and the capability-dependent probe seams are overridden, NOT dispatched (the
 * CoverItemInterceptTest ruling: getCapability dispatch is the RCON gate; the held
 * fluid probe rides the {@code probeHeldFluid} override and the :83-87 handler pour is
 * covered through its pure seam).
 */
public class TapFunnelTest extends GTOfflineTestBase {

	static BlockEntityType<GTTapBlockEntity> sTapType;
	static BlockEntityType<GTFunnelBlockEntity> sFunnelType;
	static BlockEntityType<GTBarrelBlockEntity> sBarrelType;
	static BlockEntityType<FakeTankBE> sFakeTankType;
	static BlockEntityType<FakeFillableBE> sFakeFillableType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<GTTapBlockEntity>[] tTaps = (BlockEntityType<GTTapBlockEntity>[]) new BlockEntityType<?>[1];
		tTaps[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTTapBlockEntity(tTaps[0], aPos, aState), Blocks.STONE).build(null);
		sTapType = tTaps[0];
		BlockEntityType<GTFunnelBlockEntity>[] tFunnels = (BlockEntityType<GTFunnelBlockEntity>[]) new BlockEntityType<?>[1];
		tFunnels[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTFunnelBlockEntity(tFunnels[0], aPos, aState), Blocks.STONE).build(null);
		sFunnelType = tFunnels[0];
		BlockEntityType<GTBarrelBlockEntity>[] tBarrels = (BlockEntityType<GTBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tBarrels[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTBarrelBlockEntity(tBarrels[0], aPos, aState), Blocks.STONE).build(null);
		sBarrelType = tBarrels[0];
		// 21.1 BlockEntity ctor validates the type/state pair (validateBlockState →
		// getType().isValid), so the fakes bind real BETs over the vanilla stone state
		// too — the suppliers are stored, never invoked (task p15-m4-test-infra-2).
		sFakeTankType = BlockEntityType.Builder.of((aPos, aState) -> new FakeTankBE(aPos, 0), Blocks.STONE).build(null);
		sFakeFillableType = BlockEntityType.Builder.of((aPos, aState) -> new FakeFillableBE(aPos), Blocks.STONE).build(null);
	}

	@AfterEach
	void restoreCategoryLists() {
		GTAttachmentSmallBlockEntity.Categories.GASES.clear();
		GTAttachmentSmallBlockEntity.Categories.ACIDS.clear();
		GTTapBlockEntity.VOIDING_ITEMS.clear();
	}

	/** The water key, the only live offline fluid — the name-list injection carrier. */
	private static String waterKey() {
		return GTAttachmentSmallBlockEntity.fluidKey(new FluidStack(Fluids.WATER, 1));
	}

	/** A level-less tap facing NORTH with the fake wired as its mount. */
	private static GTTapBlockEntity tap(BlockEntity aMount) {
		GTTapBlockEntity tTap = new GTTapBlockEntity(sTapType, POS, Blocks.STONE.defaultBlockState());
		tTap.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tTap.mAdjacentOverride = aMount;
		return tTap;
	}

	/** A level-less funnel facing NORTH with the fake wired as its mount and the held-fluid probe injected. */
	private static GTFunnelBlockEntity funnel(BlockEntity aMount, final FluidStack aProbedHeld) {
		GTFunnelBlockEntity tFunnel = new GTFunnelBlockEntity(sFunnelType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			protected FluidStack probeHeldFluid(ItemStack aHeld) {
				return aProbedHeld;
			}
		};
		tFunnel.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tFunnel.mAdjacentOverride = aMount;
		return tFunnel;
	}

	/**
	 * The fake mounted container: a BlockEntity (so the adjacency seam types cleanly)
	 * carrying both faces over a counter-backed tank.
	 */
	public static class FakeTankBE extends BlockEntity implements TapAccessible, FunnelAccessible {
		public final AtomicLong tank = new AtomicLong();
		public long space = 4000;

		FakeTankBE(BlockPos aPos, long aAmount) {
			super(sFakeTankType, aPos, Blocks.STONE.defaultBlockState());
			tank.set(aAmount);
		}

		@Override
		public FluidStack tapDrain(byte aSide, int aMaxDrain, boolean aDoDrain) {
			if (tank.get() <= 0) return null;
			long tAmount = Math.min(aMaxDrain, tank.get());
			if (!aDoDrain) return new FluidStack(Fluids.WATER, (int)tAmount);
			tank.addAndGet(-tAmount);
			return new FluidStack(Fluids.WATER, (int)tAmount);
		}

		@Override
		public int funnelFill(byte aSide, FluidStack aFluid, boolean aDoFill) {
			long tRoom = Math.max(0, space - tank.get());
			int tAccepted = (int)Math.min(aFluid.getAmount(), tRoom);
			if (aDoFill) tank.addAndGet(tAccepted);
			return tAccepted;
		}
	}

	/** The tap-to-tap target fake: a bounded {@code tapFill} sink. */
	public static class FakeFillableBE extends BlockEntity implements GTTapBlockEntity.TapFillable {
		public long space = 250;

		FakeFillableBE(BlockPos aPos) {
			super(sFakeFillableType, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public int tapFill(byte aSide, FluidStack aFluid, boolean aDoFill) {
			int tAccepted = (int)Math.min(aFluid.getAmount(), space);
			if (aDoFill) space -= tAccepted;
			return tAccepted;
		}
	}

	/** A recording {@link IFluidHandlerItem} fake — the held container of the :164-171 chain. */
	public static class RecordingHandler implements IFluidHandlerItem {
		public final AtomicLong content = new AtomicLong();
		private final int capacity;

		RecordingHandler(int aCapacity, long aContent) {
			capacity = aCapacity;
			content.set(aContent);
		}

		@Override
		public ItemStack getContainer() {
			return content.get() > 0 ? new ItemStack(Items.WATER_BUCKET) : new ItemStack(Items.BUCKET);
		}

		@Override
		public int getTanks() {return 1;}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			return content.get() <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, (int)content.get());
		}

		@Override
		public int getTankCapacity(int aTank) {return aTank == 0 ? capacity : 0;}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {return aTank == 0;}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			long tSpace = capacity - content.get();
			int tFilled = (int)Math.min(aResource.getAmount(), tSpace);
			if (aAction.execute()) content.addAndGet(tFilled);
			return tFilled;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			int tDrained = (int)Math.min(aMaxDrain, content.get());
			if (aAction.execute()) content.addAndGet(-tDrained);
			return tDrained <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, tDrained);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			return drain(aResource.getAmount(), aAction);
		}
	}

	// ---------------------------------------------------------------------------
	// the tap chain (upstream MultiTileEntityFluidTap.onBlockActivated3 :77-176)
	// ---------------------------------------------------------------------------

	@Test
	public void tapVoidingItemDrainsAllAndTrashes() {
		FakeTankBE tMount = new FakeTankBE(POS.north(), 8000);
		GTTapBlockEntity tTap = tap(tMount);
		GTTapBlockEntity.VOIDING_ITEMS.add(Items.STICK);

		String tReport = tTap.activate(null, (byte)2, new ItemStack(Items.STICK));

		assertTrue(tReport.contains("voided 8000"), tReport);
		assertEquals(0, tMount.tank.get(), "the VOIDING click trashes the whole tank (:82-86)");
	}

	@Test
	public void tapSimulateRefusesGasBeforeAnyExecution() {
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		FakeTankBE tMount = new FakeTankBE(POS.north(), 8000);
		GTTapBlockEntity tTap = tap(tMount);

		String tReport = tTap.activate(null, (byte)2, null);

		assertTrue(tReport.contains("refused a gas"), tReport);
		assertEquals(8000, tMount.tank.get(), "the gas refusal rides the SIMULATE probe — nothing drains (:87-88)");
	}

	@Test
	public void tapSimulateRefusesAcidUnlessTapIsAcidProof() {
		GTAttachmentSmallBlockEntity.Categories.ACIDS.add(waterKey());
		FakeTankBE tMount = new FakeTankBE(POS.north(), 8000);
		GTTapBlockEntity tTap = tap(tMount);

		String tReport = tTap.activate(null, (byte)2, null);
		assertTrue(tReport.contains("refused an acid"), tReport);
		assertEquals(8000, tMount.tank.get(), "the acid refusal happens before any drain (:88)");

		// an acid-proof tap (the Stainless/Tungsten/Adamantium rows) lets the acid through;
		// the empty hand with no consumer below consumes the click with no further effect (:162)
		tTap.mAcidProofNbt = true;
		tTap.mAcidProofFromNbt = true;
		assertTrue(tTap.isAcidProof());
		String tOk = tTap.activate(null, (byte)2, null);
		assertTrue(tOk.contains("consumed"), tOk);
		assertEquals(8000, tMount.tank.get(), "the empty hand never drains without a consumer (:162)");
	}

	@Test
	public void cauldronTierTable334_667_1000() {
		// the :94-104 table over the 1.20.1 levels — {drain, delta} or null
		assertNull(GTTapBlockEntity.cauldronFillPlan(0, false, 8000), "non-water is no plan");
		assertNull(GTTapBlockEntity.cauldronFillPlan(3, true, 8000), "a full cauldron is no plan (:94 tMeta<3)");
		assertNull(GTTapBlockEntity.cauldronFillPlan(0, true, 333), "under one tier is no plan (:94 >=334)");
		// :95 — 1000 available on an EMPTY cauldron takes the full tier
		assertPlan(GTTapBlockEntity.cauldronFillPlan(0, true, 8000), 1000, 3);
		assertPlan(GTTapBlockEntity.cauldronFillPlan(0, true, 1000), 1000, 3);
		// :98 — 667 available with the cauldron at most one tier climbs two
		assertPlan(GTTapBlockEntity.cauldronFillPlan(1, true, 8000), 667, 2);
		assertPlan(GTTapBlockEntity.cauldronFillPlan(1, true, 667), 667, 2);
		// :101 — 500 >= 334 falls to the catch-all one-tier plan (the upstream outer guard is >=334)
		assertPlan(GTTapBlockEntity.cauldronFillPlan(1, true, 500), 334, 1);
		// :101 — the catch-all one-tier plan
		assertPlan(GTTapBlockEntity.cauldronFillPlan(2, true, 334), 334, 1);
		assertPlan(GTTapBlockEntity.cauldronFillPlan(2, true, 8000), 334, 1);
		assertNull(GTTapBlockEntity.cauldronFillPlan(2, true, 200), "under 334 nothing pours");
	}

	private static void assertPlan(int[] aActual, int aDrain, int aDelta) {
		assertNotNull(aActual);
		assertEquals(aDrain, aActual[0], "drain amount");
		assertEquals(aDelta, aActual[1], "level delta");
	}

	@Test
	public void tapToTapMovesExactlyWhatTheTargetAccepted() {
		// the :110-118 nesting: the target executes first, the source pays its number
		FakeFillableBE tTarget = new FakeFillableBE(POS.below());
		tTarget.space = 100; // the target accepts less than the 250 portion
		FakeTankBE tSource = new FakeTankBE(POS.north(), 8000);
		GTTapBlockEntity tTap = tap(tSource);

		String tReport = tTap.tapToTap(tSource, (byte)3, new FluidStack(Fluids.WATER, 8000), tTarget);

		assertTrue(tReport.contains("moved 100"), tReport);
		assertEquals(7900, tSource.tank.get(), "the source pays EXACTLY what the target accepted (:114)");

		// the portion bound: lava 1000, everything else 250 (the material-bridge cut, :113)
		assertEquals(250, GTTapBlockEntity.tapToTapPortion(new FluidStack(Fluids.WATER, 8000)));
		assertEquals(1000, GTTapBlockEntity.tapToTapPortion(new FluidStack(Fluids.LAVA, 8000)));
	}

	@Test
	public void tapHeldContainerFillsAndSourcePaysExactlyWhatLanded() {
		// the :164-171 pair over its pure seams — fill the container, pay the difference
		FakeTankBE tSource = new FakeTankBE(POS.north(), 8000);
		RecordingHandler tHandler = new RecordingHandler(1000, 400);
		ItemStack tHeld = new ItemStack(Items.BUCKET);

		int tFilled = GTTapBlockEntity.fillHeldContainer(tHandler, new FluidStack(Fluids.WATER, 8000));

		assertEquals(600, tFilled, "the container takes its remaining space (:165 FL.fill)");
		assertEquals(1000, tHandler.content.get());
		GTTapBlockEntity.giveFilledContainer(null, tHandler, tHeld);
		assertTrue(tHeld.isEmpty(), "the spent container item is consumed (:168 aStack.stackSize--)");
		assertEquals(Items.WATER_BUCKET, tHandler.getContainer().getItem(), "the filled container identity (:169 ST.give)");

		// the :166 executed pay — the source drains by aFluid.amount - tNewFluid.amount
		FluidStack tPaid = tSource.tapDrain((byte)3, tFilled, true);
		assertEquals(600, tPaid.getAmount());
		assertEquals(7400, tSource.tank.get());
	}

	// ---------------------------------------------------------------------------
	// the funnel chain (upstream MultiTileEntityFluidFunnel.onBlockActivated3 :68-93)
	// ---------------------------------------------------------------------------

	@Test
	public void funnelSimulateGateThenExecutePourWithEmptyContainerBack() {
		FakeTankBE tMount = new FakeTankBE(POS.north(), 0);
		GTFunnelBlockEntity tFunnel = funnel(tMount, new FluidStack(Fluids.WATER, 1000));
		ItemStack tHeld = new ItemStack(Items.WATER_BUCKET);

		String tReport = tFunnel.activate(null, (byte)2, tHeld);

		assertTrue(tReport.contains("poured 1000"), tReport);
		assertEquals(1000, tMount.tank.get(), "the executed pour lands (:77)");
		assertTrue(tHeld.isEmpty(), "the spent container is consumed (:79)");
	}

	@Test
	public void funnelRefusesGasOnSimulate() {
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		FakeTankBE tMount = new FakeTankBE(POS.north(), 0);
		GTFunnelBlockEntity tFunnel = funnel(tMount, new FluidStack(Fluids.WATER, 1000));

		String tReport = tFunnel.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET));

		assertTrue(tReport.contains("refused a gas"), tReport);
		assertEquals(0, tMount.tank.get(), "nothing pours (:73)");
	}

	@Test
	public void funnelRefusesAcidUnlessFunnelIsAcidProof() {
		GTAttachmentSmallBlockEntity.Categories.ACIDS.add(waterKey());
		FakeTankBE tMount = new FakeTankBE(POS.north(), 0);
		GTFunnelBlockEntity tFunnel = funnel(tMount, new FluidStack(Fluids.WATER, 1000));

		String tReport = tFunnel.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET));
		assertTrue(tReport.contains("refused an acid"), tReport);
		assertEquals(0, tMount.tank.get());

		tFunnel.mAcidProofNbt = true;
		tFunnel.mAcidProofFromNbt = true;
		assertTrue(tFunnel.isAcidProof());
		String tOk = tFunnel.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET));
		assertTrue(tOk.contains("poured 1000"), tOk);
		assertEquals(1000, tMount.tank.get());
	}

	@Test
	public void funnelPartialSpaceFallsToHandlerFormAndPaysWhatLanded() {
		// :76 fails when the probe cannot take the WHOLE content (400 < 1000), so the
		// :83-87 handler form is the only pour: land first, drain the held by the number
		FakeTankBE tMount = new FakeTankBE(POS.north(), 3600); // 400 of space
		GTFunnelBlockEntity tFunnel = new GTFunnelBlockEntity(sFunnelType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			protected FluidStack probeHeldFluid(ItemStack aHeld) {
				return new FluidStack(Fluids.WATER, 1000);
			}

			@Override
			protected IFluidHandlerItem heldItemHandler(ItemStack aHeld) {
				return tRecording;
			}
		};
		tFunnel.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tFunnel.mAdjacentOverride = tMount;
		tRecording = new RecordingHandler(1000, 1000);

		String tReport = tFunnel.activate(null, (byte)2, new ItemStack(Items.BUCKET));

		assertTrue(tReport.contains("drained 400"), tReport);
		assertEquals(4000, tMount.tank.get(), "the executed pour landed the 400 (:85)");
		assertEquals(600, tRecording.content.get(), "the held container drains by EXACTLY what landed (:85)");
	}

	/** Assigned before the anonymous override fires (the fixture needs the instance first). */
	private RecordingHandler tRecording;

	// ---------------------------------------------------------------------------
	// the barrel hooks (upstream TileEntityBase08Barrel :269-276) + the mFacing gate
	// ---------------------------------------------------------------------------

	@Test
	public void barrelTapDrainAndFunnelFillMoveTheTankDirectly() {
		TileEntityBase08Barrel tBarrel = barrel(8000);

		// tapDrain: the probe answers, the executed drain pays
		FluidStack tProbed = tBarrel.tapDrain((byte)2, 300, false);
		assertEquals(300, tProbed.getAmount());
		assertEquals(8000, tBarrel.mTank.amount(), "the probe never drains (:274 aDoDrain=F)");
		FluidStack tDrained = tBarrel.tapDrain((byte)2, 300, true);
		assertEquals(300, tDrained.getAmount());
		assertEquals(7700, tBarrel.mTank.amount(), "the executed tapDrain (:274-276)");

		// funnelFill: the probe answers, the executed pour lands
		assertEquals(300, tBarrel.funnelFill((byte)2, new FluidStack(Fluids.WATER, 300), false));
		assertEquals(7700, tBarrel.mTank.amount(), "the probe never fills (:270 aDoFill=F)");
		assertEquals(300, tBarrel.funnelFill((byte)2, new FluidStack(Fluids.WATER, 300), true));
		assertEquals(8000, tBarrel.mTank.amount(), "the executed funnelFill (:269-271)");

		// the Integer.MAX_VALUE all-drain of the tap probe rides the bindInt seam
		FluidStack tAll = tBarrel.tapDrain((byte)2, Integer.MAX_VALUE, true);
		assertEquals(8000, tAll.getAmount());
		assertEquals(0, tBarrel.mTank.amount(), "the :87 Integer.MAX_VALUE probe drains everything when executed");
		assertTrue(tBarrel.tapDrain((byte)2, 100, false).isEmpty(), "an empty tank probes nothing — the FluidTankGT.EMPTY form (the upstream NF, declared)");
	}

	@Test
	public void attachmentFaceGateOnlyTheFacedNeighbourIsConsumed() {
		// the mFacing gate: the tap wired to the NORTH mount must never touch the SOUTH fake
		// (the upstream getAdjacentTileEntity(mFacing) single look, :79/:74)
		FakeTankBE tNorth = new FakeTankBE(POS.north(), 8000);
		FakeTankBE tSouth = new FakeTankBE(POS.south(), 8000);
		GTTapBlockEntity tTap = tap(tNorth);
		GTTapBlockEntity.VOIDING_ITEMS.add(Items.STICK);

		String tReport = tTap.activate(null, (byte)2, new ItemStack(Items.STICK)); // VOIDING the faced mount

		assertTrue(tReport.contains("voided 8000"), tReport);
		assertEquals(0, tNorth.tank.get());
		assertEquals(8000, tSouth.tank.get(), "the un-faced neighbour is never consulted (the mFacing gate)");

		// the same gate on the funnel side
		FakeTankBE tFunnelTarget = new FakeTankBE(POS.north(), 0);
		GTFunnelBlockEntity tFunnel = funnel(tFunnelTarget, new FluidStack(Fluids.WATER, 1000));
		String tFunnelReport = tFunnel.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET));
		assertTrue(tFunnelReport.contains("poured 1000"), tFunnelReport);
		assertEquals(8000, tSouth.tank.get(), "the funnel never touches the un-faced neighbour");
	}

	/** A level-less barrel fixture with 8000 L of water (the TileEntityBase08BarrelTest fixture form). */
	private static TileEntityBase08Barrel barrel(long aWater) {
		GTBarrelBlockEntity tBarrel = new GTBarrelBlockEntity(sBarrelType, POS, Blocks.STONE.defaultBlockState());
		if (aWater > 0) tBarrel.mTank.fill(new FluidStack(Fluids.WATER, FluidTankGT.bindInt(aWater)), FluidAction.EXECUTE);
		return tBarrel;
	}
}
