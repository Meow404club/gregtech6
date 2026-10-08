package gregtech6.tileentity.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * The nozzle pair offline truth tables (task nozzle-function acceptance — the
 * material-mc-f-attachment-rows declared cut, now ported):
 *
 * <ul>
 * <li>the drain chain (upstream MultiTileEntityFluidNozzle.java:71-129) — VOIDING item
 *     first (:76-80); the tap's MIRROR gate, gases ONLY + the acid door (:81-82); the
 *     empty hand consumes the click (:114-115; the XP/mob orb half is level-bound — the
 *     spawn needs a ServerLevel, the TapFunnelTest precedent leaves it un-exercised
 *     offline); the held container fills and the tank pays what landed (:117-124);</li>
 * <li>the cap chain (MultiTileEntityFluidCapNozzle.java:68-93) — the held gas pours
 *     when it ALL fits (:76-82), the handler form lands first and pays what landed
 *     (:83-87);</li>
 * <li>the negatives — empty tank, non-gas fluids, acids on a non-proof row, no
 *     container on the facing side, the un-faced neighbour never consulted.</li>
 * </ul>
 *
 * <p>Offline harness notes (the TapFunnelTest record): level-less fixtures take the
 * SERVER branch, the adjacency override seam wires the fakes, the gas verdict is the
 * injected name list (vanilla water as the only live offline fluid), the held-item
 * probes are overridden seams, and the cap half is forced through the
 * {@code isCapNozzle} override (a stone state carries no family row). One fixture pair
 * also drives the REAL barrel BE — the tap/funnel faces the nozzle calls are the very
 * interfaces {@link TileEntityBase08Barrel} already ships, the no-new-arm claim.
 */
public class GT6NozzleFunctionTest extends GTOfflineTestBase {

	static BlockEntityType<GTNozzleBlockEntity> sNozzleType;
	static BlockEntityType<GTBarrelBlockEntity> sBarrelType;
	static BlockEntityType<FakeGasTankBE> sFakeTankType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<GTNozzleBlockEntity>[] tNozzles = (BlockEntityType<GTNozzleBlockEntity>[]) new BlockEntityType<?>[1];
		tNozzles[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTNozzleBlockEntity(tNozzles[0], aPos, aState), Blocks.STONE).build(null);
		sNozzleType = tNozzles[0];
		BlockEntityType<GTBarrelBlockEntity>[] tBarrels = (BlockEntityType<GTBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tBarrels[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTBarrelBlockEntity(tBarrels[0], aPos, aState), Blocks.STONE).build(null);
		sBarrelType = tBarrels[0];
		sFakeTankType = BlockEntityType.Builder.of((aPos, aState) -> new FakeGasTankBE(aPos, 0), Blocks.STONE).build(null);
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

	/** A level-less DRAIN nozzle facing NORTH with the fake wired as its mount. */
	private static GTNozzleBlockEntity nozzle(BlockEntity aMount) {
		GTNozzleBlockEntity tNozzle = new GTNozzleBlockEntity(sNozzleType, POS, Blocks.STONE.defaultBlockState());
		tNozzle.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tNozzle.mAdjacentOverride = aMount;
		return tNozzle;
	}

	/** A level-less CAP nozzle (the family seam forced — a stone state carries no row). */
	private static GTNozzleBlockEntity capNozzle(BlockEntity aMount, final FluidStack aProbedHeld) {
		GTNozzleBlockEntity tNozzle = new GTNozzleBlockEntity(sNozzleType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			public boolean isCapNozzle() {return true;}

			@Override
			protected FluidStack probeHeldFluid(ItemStack aHeld) {return aProbedHeld;}
		};
		tNozzle.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tNozzle.mAdjacentOverride = aMount;
		return tNozzle;
	}

	/** The fake mounted container: both faces over a counter-backed tank (the TapFunnelTest fake form). */
	public static class FakeGasTankBE extends BlockEntity implements TapAccessible, FunnelAccessible {
		public final AtomicLong tank = new AtomicLong();
		public long space = 4000;

		FakeGasTankBE(BlockPos aPos, long aAmount) {
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

	// ---------------------------------------------------------------------------
	// the drain chain (upstream MultiTileEntityFluidNozzle.java:71-129)
	// ---------------------------------------------------------------------------

	@Test
	public void nozzleVoidingItemDrainsAllAndTrashes() {
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 8000);
		GTNozzleBlockEntity tNozzle = nozzle(tMount);
		GTTapBlockEntity.VOIDING_ITEMS.add(Items.STICK);

		String tReport = tNozzle.activate(null, (byte)2, new ItemStack(Items.STICK));

		assertTrue(tReport.contains("voided 8000"), tReport);
		assertEquals(0, tMount.tank.get(), "the VOIDING click trashes the whole tank (:76-80)");
	}

	@Test
	public void nozzleDrawsGasesOnly() {
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 8000);
		GTNozzleBlockEntity tNozzle = nozzle(tMount);

		// water is no gas offline — the mirror gate refuses BEFORE anything drains (:82)
		String tReport = tNozzle.activate(null, (byte)2, null);
		assertTrue(tReport.contains("refused a non-gas"), tReport);
		assertEquals(8000, tMount.tank.get(), "nothing drains on the non-gas refusal (:82)");

		// the injected gas list lets the same water through the gate
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		String tOk = tNozzle.activate(null, (byte)2, null);
		assertTrue(tOk.contains("consumed"), tOk);
		assertEquals(8000, tMount.tank.get(), "the empty hand never drains without a consumer (:114-115)");
	}

	@Test
	public void nozzleRefusesAcidUnlessAcidProof() {
		GTAttachmentSmallBlockEntity.Categories.ACIDS.add(waterKey());
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey()); // past the gas door
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 8000);
		GTNozzleBlockEntity tNozzle = nozzle(tMount);

		String tReport = tNozzle.activate(null, (byte)2, null);
		assertTrue(tReport.contains("refused an acid"), tReport);
		assertEquals(8000, tMount.tank.get(), "the acid refusal happens before any drain (:82)");

		tNozzle.mAcidProofNbt = true;
		tNozzle.mAcidProofFromNbt = true;
		String tOk = tNozzle.activate(null, (byte)2, null);
		assertTrue(tOk.contains("consumed"), tOk);
		assertEquals(8000, tMount.tank.get(), "the acid-proof row passes the door, the empty hand still drains nothing");
	}

	@Test
	public void nozzleHeldContainerFillsAndTankPaysWhatLanded() {
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 8000);
		GTNozzleBlockEntity tNozzle = new GTNozzleBlockEntity(sNozzleType, POS, Blocks.STONE.defaultBlockState()) {
			private final TapFunnelTest.RecordingHandler tHandler = new TapFunnelTest.RecordingHandler(1000, 400);

			@Override
			protected IFluidHandlerItem heldItemHandler(ItemStack aHeld) {return tHandler;}
		};
		tNozzle.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tNozzle.mAdjacentOverride = tMount;
		ItemStack tHeld = new ItemStack(Items.BUCKET);

		String tReport = tNozzle.activate(null, (byte)2, tHeld);

		assertTrue(tReport.contains("filled 600"), tReport);
		assertEquals(7400, tMount.tank.get(), "the tank pays EXACTLY what the container took (:119-121)");
		assertTrue(tHeld.isEmpty(), "the spent container is consumed (:121 stackSize-- / the 1.20.1 shrink)");
	}

	@Test
	public void nozzleNegativesEmptyTankAndNoContainer() {
		// empty tank → the probe answers nothing (:81)
		GTNozzleBlockEntity tNozzle = nozzle(new FakeGasTankBE(POS.north(), 0));
		assertTrue(tNozzle.activate(null, (byte)2, null).contains("nothing to draw"));

		// no BE on the facing side at all (:74 instanceof guard)
		GTNozzleBlockEntity tAlone = nozzle(null);
		assertTrue(tAlone.activate(null, (byte)2, null).contains("no tap-accessible container"));
	}

	@Test
	public void nozzleDrainsTheRealBarrelThroughItsShippedFace() {
		// the integration pin: the real barrel BE's TapAccessible face serves the nozzle
		// with NO new arm — the no-new-container-BE-surface claim, end to end
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		GTBarrelBlockEntity tBarrel = new GTBarrelBlockEntity(sBarrelType, POS.north(), Blocks.STONE.defaultBlockState());
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 8000), FluidAction.EXECUTE);
		GTNozzleBlockEntity tNozzle = nozzle(tBarrel);
		GTTapBlockEntity.VOIDING_ITEMS.add(Items.STICK);

		String tReport = tNozzle.activate(null, (byte)2, new ItemStack(Items.STICK));

		assertTrue(tReport.contains("voided 8000"), tReport);
		assertEquals(0, tBarrel.mTank.amount(), "the real barrel paid the whole tank through tapDrain (Root :936 identity)");
	}

	// ---------------------------------------------------------------------------
	// the cap-fill chain (upstream MultiTileEntityFluidCapNozzle.java:68-93)
	// ---------------------------------------------------------------------------

	@Test
	public void capNozzlePoursHeldGasWhenItAllFits() {
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 0);
		GTNozzleBlockEntity tNozzle = capNozzle(tMount, new FluidStack(Fluids.WATER, 1000));
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		ItemStack tHeld = new ItemStack(Items.BUCKET);

		String tReport = tNozzle.activate(null, (byte)2, tHeld);

		assertTrue(tReport.contains("filled 1000 L of " + waterKey()), tReport);
		assertEquals(1000, tMount.tank.get(), "the executed pour lands (:77)");
		assertTrue(tHeld.isEmpty(), "the spent container is consumed (:79)");
	}

	@Test
	public void capNozzleRefusesNonGasAndAcids() {
		// non-gas content → the mirror gate refuses (:73)
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 0);
		GTNozzleBlockEntity tNozzle = capNozzle(tMount, new FluidStack(Fluids.WATER, 1000));
		assertTrue(tNozzle.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET)).contains("refused a non-gas"));
		assertEquals(0, tMount.tank.get(), "nothing pours on the non-gas refusal (:73)");

		// acid gas on a non-proof row → the acid door refuses; the proof row passes
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		GTAttachmentSmallBlockEntity.Categories.ACIDS.add(waterKey());
		assertTrue(tNozzle.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET)).contains("refused an acid"));
		tNozzle.mAcidProofNbt = true;
		tNozzle.mAcidProofFromNbt = true;
		assertTrue(tNozzle.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET)).contains("filled 1000"));
		assertEquals(1000, tMount.tank.get());
	}

	@Test
	public void capNozzleNegativesNothingHeldAndNoContainer() {
		// nothing held → the no-op (:70-71)
		GTNozzleBlockEntity tNozzle = capNozzle(new FakeGasTankBE(POS.north(), 0), new FluidStack(Fluids.WATER, 1000));
		assertTrue(tNozzle.activate(null, (byte)2, null).contains("nothing held"));

		// held but empty of fluid (:72) — the probe seam answers null for the fluid-less item
		GTNozzleBlockEntity tEmpty = capNozzle(new FakeGasTankBE(POS.north(), 0), null);
		assertTrue(tEmpty.activate(null, (byte)2, new ItemStack(Items.STICK)).contains("carries no fluid"));

		// gas in hand but no FunnelAccessible on the facing side (:74-75)
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		GTNozzleBlockEntity tAlone = capNozzle(null, new FluidStack(Fluids.WATER, 1000));
		assertTrue(tAlone.activate(null, (byte)2, new ItemStack(Items.BUCKET)).contains("no funnel-accessible container"));
	}

	@Test
	public void capNozzlePartialSpaceFallsToHandlerFormAndPaysWhatLanded() {
		// :76 fails when the probe cannot take the WHOLE content (400 < 1000), so the
		// :83-87 handler form is the only pour: land first, drain the held by the number
		FakeGasTankBE tMount = new FakeGasTankBE(POS.north(), 3600); // 400 of space
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		GTNozzleBlockEntity tNozzle = new GTNozzleBlockEntity(sNozzleType, POS, Blocks.STONE.defaultBlockState()) {
			private final TapFunnelTest.RecordingHandler tHandler = new TapFunnelTest.RecordingHandler(1000, 1000);

			@Override
			public boolean isCapNozzle() {return true;}

			@Override
			protected FluidStack probeHeldFluid(ItemStack aHeld) {return new FluidStack(Fluids.WATER, 1000);}

			@Override
			protected IFluidHandlerItem heldItemHandler(ItemStack aHeld) {return tHandler;}
		};
		tNozzle.mFacing = (byte)Direction.NORTH.get3DDataValue();
		tNozzle.mAdjacentOverride = tMount;

		String tReport = tNozzle.activate(null, (byte)2, new ItemStack(Items.BUCKET));

		assertTrue(tReport.contains("drained 400"), tReport);
		assertEquals(4000, tMount.tank.get(), "the executed pour landed the 400 (:85)");
	}

	@Test
	public void theOneClassDispatchesBothChainsByItsFamily() {
		// the one-class-two-BETs dispatch: the default fixture (no family row) runs the
		// drain half, the forced cap fixture runs the fill half — same activate head
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		GTNozzleBlockEntity tDrain = nozzle(null);
		assertTrue(tDrain.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET)).contains("no tap-accessible container"),
				"the default mount answers the drain chain");
		GTNozzleBlockEntity tCap = capNozzle(null, new FluidStack(Fluids.WATER, 1000));
		assertTrue(tCap.activate(null, (byte)2, new ItemStack(Items.BUCKET)).contains("no funnel-accessible container"),
				"the forced cap mount answers the fill chain");
	}

	@Test
	public void attachmentFaceGateOnlyTheFacedNeighbourIsConsumed() {
		// the mFacing gate on both halves: only the neighbour the attachment FACES moves
		FakeGasTankBE tNorth = new FakeGasTankBE(POS.north(), 8000);
		FakeGasTankBE tSouth = new FakeGasTankBE(POS.south(), 8000);
		GTNozzleBlockEntity tNozzle = nozzle(tNorth);
		GTTapBlockEntity.VOIDING_ITEMS.add(Items.STICK);

		assertTrue(tNozzle.activate(null, (byte)2, new ItemStack(Items.STICK)).contains("voided 8000"));
		assertEquals(0, tNorth.tank.get());
		assertEquals(8000, tSouth.tank.get(), "the un-faced neighbour is never consulted");

		FakeGasTankBE tCapTarget = new FakeGasTankBE(POS.north(), 0);
		GTAttachmentSmallBlockEntity.Categories.GASES.add(waterKey());
		GTNozzleBlockEntity tCap = capNozzle(tCapTarget, new FluidStack(Fluids.WATER, 1000));
		assertTrue(tCap.activate(null, (byte)2, new ItemStack(Items.WATER_BUCKET)).contains("filled 1000"));
		assertEquals(8000, tSouth.tank.get(), "the cap never touches the un-faced neighbour");
	}
}
