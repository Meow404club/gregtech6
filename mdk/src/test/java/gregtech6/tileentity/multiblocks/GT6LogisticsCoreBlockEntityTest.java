package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.tileentity.logistics.ITileEntityLogisticsStorage;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.multiblocks.GT6LogisticsCoreBlockEntity.LogisticsData;

/**
 * The Logistics Core offline acceptance (task p32-logistics-lv3, the massfab fixture
 * posture): the :109-147 structure walk over the stub world (the 44/53/27 cell census,
 * the CPU pool arithmetic, the cheapskate wall substitute, the wrong-part rejection),
 * the :437-444 BFS (cubic radius, tier registration), the :451-500 routing order
 * (defrag Filtered-before-Semi), the EU accounting (:525/:565) and the :680-687 energy
 * window. The live RCON chain (p32_logistics_core) carries the wire-mediated BFS and
 * the tank endpoints; these tables pin the decision functions.
 *
 * <p>Fixture posture: the seven structure roles bind to vanilla stand-ins (walls BRICKS,
 * vents GOLD_BLOCK, versatile DIAMOND_BLOCK, logic EMERALD_BLOCK, control LAPIS_BLOCK,
 * storage IRON_BLOCK, conversion COAL_BLOCK); the offline capability wall (the
 * CapabilityToken:28 transformer note, MultiBlockPartBlockEntity doc) is routed around
 * by the {@link TestCore} handler-map override of the resolution seam.
 */
public class GT6LogisticsCoreBlockEntityTest extends GTMultiBlocksOfflineTestBase {

	/** The controller cell for this suite — away from the shared C1/C2 arbitration fixtures. */
	static final BlockPos CORE_POS = new BlockPos(300, 64, 300);
	/** Facing north (2) → the centre sits at z+2 (the anchor IS -OFF). */
	static final BlockPos CENTER = new BlockPos(300, 64, 302);

	static BlockEntityType<TestCore> sCoreType;
	static BlockEntityType<MultiBlockPartBlockEntity> sCorePartType;

	/** The concrete test core — the stand-in roles + the offline handler map. */
	public static final class TestCore extends GT6LogisticsCoreBlockEntity {
		public final Map<BlockEntity, IFluidHandler> mFluidHandlers = new IdentityHashMap<>();
		public final Map<BlockEntity, IItemHandler> mItemHandlers = new IdentityHashMap<>();

		public TestCore(BlockPos aPos, BlockState aState) {
			super(sCoreType, aPos, aState);
		}

		public TestCore(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override protected Block getWallBlock() { return Blocks.BRICKS; }
		@Override protected Block getVentBlock() { return Blocks.GOLD_BLOCK; }
		@Override protected Block getVersatileBlock() { return Blocks.DIAMOND_BLOCK; }
		@Override protected Block getLogicBlock() { return Blocks.EMERALD_BLOCK; }
		@Override protected Block getControlBlock() { return Blocks.LAPIS_BLOCK; }
		@Override protected Block getStorageBlock() { return Blocks.IRON_BLOCK; }
		@Override protected Block getConversionBlock() { return Blocks.COAL_BLOCK; }

		@Override
		public IFluidHandler fluidHandlerOf(BlockEntity aTarget) {
			return mFluidHandlers.get(aTarget);
		}

		@Override
		public IItemHandler itemHandlerOf(BlockEntity aTarget) {
			return mItemHandlers.get(aTarget);
		}
	}

	/** The storage-endpoint fixture — the upstream barrel face (:285-288) over a fixture tank. */
	public static class TestTankEndpoint extends BlockEntity implements ITileEntityLogistics, ITileEntityLogisticsStorage {
		public final FluidTankGT mTank = new FluidTankGT(100000);
		public int mPriorityOverride = -1;
		@Nullable public ItemStack mItemFilter = null;
		public int mPriorityItem = 0;

		TestTankEndpoint(BlockPos aPos) {
			super(sCorePartType, aPos, Blocks.BRICKS.defaultBlockState());
		}

		@Override public boolean canLogistics(byte aSide) { return true; }
		@Override public int getLogisticsPriorityFluid() { return mPriorityOverride >= 0 ? mPriorityOverride : (mTank.isEmpty() ? 1 : 2); }
		@Override public int getLogisticsPriorityItem() { return mPriorityItem; }
		@Override public Fluid getLogisticsFilterFluid() { return mTank.fluid() == null ? null : mTank.fluid().getFluid(); }
		@Nullable @Override public ItemStack getLogisticsFilterItem() { return mItemFilter; }
	}

	/** The dump-sink fixture — a bare logistics member with an item handler (the cover stand-in). */
	public static class TestDumpSink extends BlockEntity implements ITileEntityLogistics {
		public final ItemStackHandler mItems = new ItemStackHandler(9);

		TestDumpSink(BlockPos aPos) {
			super(sCorePartType, aPos, Blocks.BRICKS.defaultBlockState());
		}

		@Override public boolean canLogistics(byte aSide) { return true; }
	}

	/** A minimal IFluidHandler over a {@link FluidTankGT} (the BarrelFluidHandler shape, tank-only). */
	static final class TankHandler implements IFluidHandler {
		final FluidTankGT mTank;

		TankHandler(FluidTankGT aTank) {
			mTank = aTank;
		}

		@Override public int getTanks() { return 1; }
		@Override public FluidStack getFluidInTank(int aTank) { return mTank.fluid(); }
		@Override public int getTankCapacity(int aTank) { return (int)Math.min(mTank.getCapacity(), Integer.MAX_VALUE); }
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) { return true; }

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			return mTank.fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			return mTank.drain(aMaxDrain, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (mTank.fluid() == null || aResource == null || mTank.fluid().getFluid() != aResource.getFluid()) return null;
			return mTank.drain(aResource.getAmount(), aAction);
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildCoreFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		Block[] tStandIns = {Blocks.BRICKS, Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK,
				Blocks.LAPIS_BLOCK, Blocks.IRON_BLOCK, Blocks.COAL_BLOCK};
		BlockEntityType<TestCore>[] tHolder = (BlockEntityType<TestCore>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestCore::new, tStandIns).build(null);
		sCoreType = tHolder[0];
		BlockEntityType<MultiBlockPartBlockEntity>[] tPartHolder = (BlockEntityType<MultiBlockPartBlockEntity>[]) new BlockEntityType<?>[1];
		tPartHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new MultiBlockPartBlockEntity(tPartHolder[0], aPos, aState), tStandIns).build(null);
		sCorePartType = tPartHolder[0];
	}

	private static TestCore placeCore(MultiBlockLevel aLevel) {
		TestCore tCore = sCoreType.create(CORE_POS, Blocks.BRICKS.defaultBlockState());
		tCore.setLevel(aLevel);
		tCore.mFacing = 2; // north — the centre sits at z+2
		aLevel.mStates.put(CORE_POS, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(CORE_POS, tCore);
		return tCore;
	}

	private static TestTankEndpoint placeTank(MultiBlockLevel aLevel, BlockPos aPos, int aPriority) {
		TestTankEndpoint tTank = new TestTankEndpoint(aPos);
		tTank.mPriorityOverride = aPriority;
		tTank.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tTank);
		return tTank;
	}

	// ------------------------------------------------------------------
	// the :109-147 structure walk
	// ------------------------------------------------------------------

	/** The full greenfield scaffold stock: 44 walls + 53 vents + 23 versatile + 1 of each specialist = 122 items (125 cells minus the controller's own vent cell). */
	private static SimpleContainer fullStock() {
		return new SimpleContainer(
				new ItemStack(Blocks.BRICKS, 44),
				new ItemStack(Blocks.GOLD_BLOCK, 53),
				new ItemStack(Blocks.DIAMOND_BLOCK, 23),
				new ItemStack(Blocks.EMERALD_BLOCK, 1),
				new ItemStack(Blocks.LAPIS_BLOCK, 1),
				new ItemStack(Blocks.IRON_BLOCK, 1),
				new ItemStack(Blocks.COAL_BLOCK, 1));
	}

	@Test
	public void greenfieldScaffoldFormsAndCountsTheCpuPools() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = (aPos, aState) -> sCorePartType.create(aPos, aState);
		TestCore tCore = placeCore(tLevel);
		SimpleContainer tStock = fullStock();

		// pass A places and consumes (the Util stale-reference quirk), pass B binds the cells
		tCore.checkStructure2(null, null, tStock);
		assertTrue(tCore.checkStructure2(null, null, tStock), "the greenfield scaffold forms");
		for (int i = 0; i < tStock.getContainerSize(); i++) {
			assertEquals(0, tStock.getItem(i).getCount(), "stack " + i + " fully consumed");
		}

		// the CPU pool arithmetic: 23 versatile (+1 each) + one specialist (+4 each)
		assertEquals(27, tCore.mCPU_Logic, "23 versatile + 4 logic");
		assertEquals(27, tCore.mCPU_Control, "23 versatile + 4 control");
		assertEquals(27, tCore.mCPU_Storage, "23 versatile + 4 storage");
		assertEquals(27, tCore.mCPU_Conversion, "23 versatile + 4 conversion");
		assertTrue(tCore.checkStructure(true), "the cached verdict flips formed");
	}

	@Test
	public void wallSubstituteAndWrongPartArms() {
		// the cheapskate arm (:132): a wall in a CPU cell is legal — 27 walls in the core, no CPUs, fails :144
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = (aPos, aState) -> sCorePartType.create(aPos, aState);
		TestCore tCore = placeCore(tLevel);
		SimpleContainer tAllWalls = new SimpleContainer(
				new ItemStack(Blocks.BRICKS, 71), // 44 frame + 27 core substitutes
				new ItemStack(Blocks.GOLD_BLOCK, 53));
		tCore.checkStructure2(null, null, tAllWalls);
		assertFalse(tCore.checkStructure2(null, null, tAllWalls), "the :144 pool gate refuses the all-wall core");

		// the wrong-part rejection: one outer wall replaced by dirt
		MultiBlockLevel tLevel2 = new MultiBlockLevel();
		tLevel2.mBeFactory = (aPos, aState) -> sCorePartType.create(aPos, aState);
		TestCore tCore2 = placeCore(tLevel2);
		SimpleContainer tStock = fullStock();
		tCore2.checkStructure2(null, null, tStock);
		tCore2.checkStructure2(null, null, tStock);
		// the foreign block lands straight in the maps — the BE factory would mint a part BE
		// over dirt, which the 1.21.1 validateBlockState rejects at setBlock time
		BlockPos tWrong = new BlockPos(302, 64, 300); // the (2,0,-2) wall cell
		tLevel2.mStates.put(tWrong, Blocks.DIRT.defaultBlockState());
		tLevel2.mBlockEntities.remove(tWrong);
		assertFalse(tCore2.checkStructure2(null, null, null), "a foreign block in the frame rejects");
	}

	@Test
	public void partModesAndStructureBox() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = (aPos, aState) -> sCorePartType.create(aPos, aState);
		TestCore tCore = placeCore(tLevel);
		SimpleContainer tStock = fullStock();
		tCore.checkStructure2(null, null, tStock);
		tCore.checkStructure2(null, null, tStock);

		// the outer wall at centre+(2,0,-2) carries ONLY_LOGISTICS & ONLY_ENERGY_IN (:138)
		MultiBlockPartBlockEntity tWall = (MultiBlockPartBlockEntity)tLevel.getBlockEntity(new BlockPos(302, 64, 300));
		assertEquals(MultiBlockPartBlockEntity.ONLY_LOGISTICS & MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tWall.mMode, "the wall mode");
		// the vent at centre+(1,0,-2) carries ONLY_LOGISTICS (:140)
		MultiBlockPartBlockEntity tVent = (MultiBlockPartBlockEntity)tLevel.getBlockEntity(new BlockPos(301, 64, 300));
		assertEquals(MultiBlockPartBlockEntity.ONLY_LOGISTICS, tVent.mMode, "the vent mode");
		// the CPU cell at the centre carries NOTHING (:119)
		MultiBlockPartBlockEntity tCPU = (MultiBlockPartBlockEntity)tLevel.getBlockEntity(CENTER);
		assertEquals(MultiBlockPartBlockEntity.NOTHING, tCPU.mMode, "the CPU mode");

		// :180-183 — the box ±2 around the centre
		assertFalse(tCore.isInsideStructure(CENTER.getX() - 3, CENTER.getY(), CENTER.getZ()));
		assertTrue(tCore.isInsideStructure(CENTER.getX() - 2, CENTER.getY(), CENTER.getZ()));
		assertTrue(tCore.isInsideStructure(CORE_POS.getX(), CORE_POS.getY(), CORE_POS.getZ()));
		assertFalse(tCore.isInsideStructure(CENTER.getX(), CENTER.getY() + 3, CENTER.getZ()));
	}

	// ------------------------------------------------------------------
	// the :437-444 BFS + :277-294 tier registration
	// ------------------------------------------------------------------

	@Test
	public void bfsRegistersTiersWithinTheCubicRadius() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mCPU_Logic = 1; tCore.mCPU_Control = 1; tCore.mCPU_Storage = 1; tCore.mCPU_Conversion = 1;
		tCore.mEnergy = 10000;

		TestTankEndpoint tA = placeTank(tLevel, new BlockPos(300, 64, 299), 1); // generic, dist 3
		TestTankEndpoint tB = placeTank(tLevel, new BlockPos(300, 63, 299), 3); // filtered, dist 3 via A
		TestTankEndpoint tC = placeTank(tLevel, new BlockPos(300, 62, 299), 2); // semi, dist 3 via B
		TestTankEndpoint tFar = placeTank(tLevel, new BlockPos(300, 62, 296), 1); // dist 6 — beyond the radius

		tCore.scanAndRoute();

		assertEquals(1, tCore.mReportFluid[0], "one generic endpoint");
		assertEquals(1, tCore.mReportFluid[1], "one semi endpoint");
		assertEquals(1, tCore.mReportFluid[2], "one filtered endpoint");
		assertEquals(0, tCore.mReportFilters, "no item filters in the network");
		assertEquals(1, tCore.oCPU_Control, "the used range: dist 3 - 2");
		// nothing moved: no import/export covers and empty tanks
		assertEquals(0, tCore.mMovedLast, "an idle network moves nothing");
		assertTrue(tCore.mReportItem[0] == 0 && tCore.mReportItem[1] == 0 && tCore.mReportItem[2] == 0);
	}

	// ------------------------------------------------------------------
	// the :451-500 routing + :518-555 EU accounting
	// ------------------------------------------------------------------

	@Test
	public void defragRoutesGenericIntoFilteredBeforeSemi() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mCPU_Logic = 1; tCore.mCPU_Control = 1; tCore.mCPU_Storage = 1; tCore.mCPU_Conversion = 1;
		tCore.mEnergy = 10000;

		TestTankEndpoint tA = placeTank(tLevel, new BlockPos(300, 64, 299), 1); // generic source
		tA.mTank.fill(new FluidStack(Fluids.WATER, 20000), IFluidHandler.FluidAction.EXECUTE);
		TestTankEndpoint tB = placeTank(tLevel, new BlockPos(300, 63, 299), 3); // filtered, water identity (1000 L kept)
		tB.mTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		TestTankEndpoint tC = placeTank(tLevel, new BlockPos(300, 62, 299), 2); // semi, empty (filter null)
		tCore.mFluidHandlers.put(tA, new TankHandler(tA.mTank));
		tCore.mFluidHandlers.put(tB, new TankHandler(tB.mTank));
		tCore.mFluidHandlers.put(tC, new TankHandler(tC.mTank));

		// op 1: the defrag arm generic→Filtered (:473) fires first — the semi tank stays empty
		tCore.scanAndRoute();
		assertEquals(4000, tA.mTank.amount(), "the generic source paid 16000 L");
		assertEquals(17000, tB.mTank.amount(), "the FILTERED tier took the move first");
		assertEquals(0, tC.mTank.amount(), "the SEMI tier is only reached when the filtered tier refuses");
		// the :451 exit arithmetic — a full-usage scan exits through the CONDITION increment,
		// so the counter reads ops+1 (upstream-verbatim; the break exit refunds to the exact
		// count via the :498 decrement)
		assertEquals(2, tCore.oCPU_Logic, "one productive op, exited through the loop condition");
		assertEquals(16000, tCore.mMovedLast, "the moved ledger");
	}

	@Test
	public void fluidMoveChargesDivup250PerLiter() {
		// the :525 cost — 16000 L costs divup(16000, 250) = 64 EU, 4000 L costs 16 EU
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mCPU_Logic = 1; tCore.mCPU_Control = 1; tCore.mCPU_Storage = 1; tCore.mCPU_Conversion = 1;

		TestTankEndpoint tA = placeTank(tLevel, new BlockPos(300, 64, 299), 1);
		tA.mTank.fill(new FluidStack(Fluids.WATER, 20000), IFluidHandler.FluidAction.EXECUTE);
		TestTankEndpoint tB = placeTank(tLevel, new BlockPos(300, 63, 299), 2); // semi (no identity gate)
		tCore.mFluidHandlers.put(tA, new TankHandler(tA.mTank));
		tCore.mFluidHandlers.put(tB, new TankHandler(tB.mTank));

		tCore.mEnergy = 1000;
		tCore.scanAndRoute(); // moves 16000 L (the Conversion budget), costs 64 EU
		assertEquals(4000, tA.mTank.amount());
		assertEquals(16000, tB.mTank.amount());
		assertEquals(1000 - 64, tCore.mEnergy, "the :525 divup(moved, 250) charge");
		assertEquals(1, tCore.oCPU_Conversion, "divup(16000, 16000) throughput usage");

		tCore.scanAndRoute(); // moves the remaining 4000 L, costs 16 EU
		assertEquals(0, tA.mTank.amount());
		assertEquals(20000, tB.mTank.amount());
		assertEquals(1000 - 64 - 16, tCore.mEnergy);
		assertEquals(2, tCore.oCPU_Logic, "one op per scan (one Logic CPU), the condition-exit quirk");
	}

	@Test
	public void filteredIdentityGateSpillsIntoTheSemiTier() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mCPU_Logic = 1; tCore.mCPU_Control = 1; tCore.mCPU_Storage = 1; tCore.mCPU_Conversion = 1;
		tCore.mEnergy = 10000;

		TestTankEndpoint tA = placeTank(tLevel, new BlockPos(300, 64, 299), 1);
		tA.mTank.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE);
		TestTankEndpoint tB = placeTank(tLevel, new BlockPos(300, 63, 299), 3); // filtered, LAVA identity
		tB.mTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
		TestTankEndpoint tC = placeTank(tLevel, new BlockPos(300, 62, 299), 2); // semi, empty — accepts anything
		tCore.mFluidHandlers.put(tA, new TankHandler(tA.mTank));
		tCore.mFluidHandlers.put(tB, new TankHandler(tB.mTank));
		tCore.mFluidHandlers.put(tC, new TankHandler(tC.mTank));

		tCore.scanAndRoute();
		assertEquals(0, tB.mTank.amount() - 1000, "the filtered tier refuses the non-matching fluid");
		assertEquals(1000, tB.mTank.amount());
		assertEquals(0, tA.mTank.amount(), "the water spilled into the semi tier");
		assertEquals(5000, tC.mTank.amount(), "arm 2 (generic→semi :475) picked it up");
	}

	// ------------------------------------------------------------------
	// the :479-494 dump arm (the decision function — the pairing is cover-driven)
	// ------------------------------------------------------------------

	@Test
	public void dumpArmRespectsTheProtectedSet() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mCPU_Logic = 1; tCore.mCPU_Control = 1; tCore.mCPU_Storage = 1; tCore.mCPU_Conversion = 1;

		TestTankEndpoint tItemSource = placeTank(tLevel, new BlockPos(300, 64, 299), 0);
		tItemSource.mPriorityItem = 1; // generic item storage
		ItemStackHandler tSourceItems = new ItemStackHandler(4);
		tSourceItems.setStackInSlot(0, new ItemStack(Items.STICK, 8));   // protected by the network filter
		tSourceItems.setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 8)); // dumpable
		tCore.mItemHandlers.put(tItemSource, tSourceItems);

		TestDumpSink tSink = new TestDumpSink(new BlockPos(300, 63, 299));
		tSink.setLevel(tLevel);
		tLevel.mStates.put(tSink.getBlockPos(), Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(tSink.getBlockPos(), tSink);
		tCore.mItemHandlers.put(tSink, tSink.mItems);

		// the protected set: a filtered endpoint's item filter joins it (:278) — here directly seeded
		tCore.mFilteredFor.add(new ItemStack(Items.STICK, 1));

		// op 1 — the stick is excluded, the cobblestone moves
		long tMoved = tCore.moveStacksForDump(
				new LogisticsData(tItemSource, null, null, 0),
				new LogisticsData(tSink, null, null, 0));
		assertEquals(8, tMoved, "the unprotected stack moved");
		assertTrue(ItemStack.isSameItemSameTags(tSink.mItems.getStackInSlot(0), new ItemStack(Items.COBBLESTONE, 8)),
				"the cobblestone landed in the sink, not the protected stick");
		assertEquals(8, tSourceItems.getStackInSlot(0).getCount(), "the protected stick never left the source");
		assertEquals(0, tSourceItems.getStackInSlot(1).getCount(), "the source cobblestone is gone");
		assertTrue(tCore.isFilteredFor(new ItemStack(Items.STICK, 3)), "the item-identity membership");
		assertFalse(tCore.isFilteredFor(new ItemStack(Items.COBBLESTONE, 1)));
	}

	// ------------------------------------------------------------------
	// the :680-687 energy window
	// ------------------------------------------------------------------

	@Test
	public void energyWindowPins() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mCPU_Logic = 1; tCore.mCPU_Control = 1; tCore.mCPU_Storage = 1; tCore.mCPU_Conversion = 1;

		// the oversize packet is refused (the declared no-explosion narrowing of :684)
		assertEquals(0, tCore.doEnergyInjection(TD_ENERGY, (byte)2, 2048, 1, true), "2048 > 1024 refused");
		assertEquals(0, tCore.mEnergy);

		// the theoretical pass returns the amount without charging (:683) — BEFORE the :681
		// saturation gate has anything to refuse
		assertEquals(2, tCore.doEnergyInjection(TD_ENERGY, (byte)2, 512, 2, false), "the :683 theoretical arm");
		assertEquals(0, tCore.mEnergy);

		// a 512-packet is accepted and charged
		assertEquals(4, tCore.doEnergyInjection(TD_ENERGY, (byte)2, 512, 4, true), ":686");
		assertEquals(2048, tCore.mEnergy, ":685 aAmount * aSize");

		// the saturation gate (:681): capacity = 128 + 1*256*1 = 384 — already past it, and
		// the gate precedes the :683 theoretical arm (the upstream order)
		assertEquals(0, tCore.doEnergyInjection(TD_ENERGY, (byte)2, 512, 1, true), "the :681 gate");
		assertEquals(0, tCore.doEnergyInjection(TD_ENERGY, (byte)2, 512, 1, false), "the gate precedes :683");
	}

	// ------------------------------------------------------------------
	// the NBT round trip (:72-106)
	// ------------------------------------------------------------------

	@Test
	public void nbtRoundTripCarriesEnergyAndCpuCounts() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCore tCore = placeCore(tLevel);
		tCore.mEnergy = 12345;
		tCore.mCPU_Logic = 5; tCore.mCPU_Control = 9; tCore.mCPU_Storage = 13; tCore.mCPU_Conversion = 17;
		tCore.oCPU_Logic = 2; tCore.oCPU_Control = 3; tCore.oCPU_Storage = 0; tCore.oCPU_Conversion = 4;

		CompoundTag tTag = new CompoundTag();
		tCore.saveAdditional(tTag);
		assertEquals(12345L, tTag.getLong(GT6LogisticsCoreBlockEntity.NBT_ENERGY));
		assertEquals(5, tTag.getInt(GT6LogisticsCoreBlockEntity.NBT_CPU_LOGIC));

		TestCore tRestored = sCoreType.create(CORE_POS, Blocks.BRICKS.defaultBlockState());
		tRestored.load(tTag);
		assertEquals(12345L, tRestored.mEnergy);
		assertEquals(5, tRestored.mCPU_Logic);
		assertEquals(9, tRestored.mCPU_Control);
		assertEquals(13, tRestored.mCPU_Storage);
		assertEquals(17, tRestored.mCPU_Conversion);
		assertEquals(2, tRestored.oCPU_Logic);
		assertEquals(4, tRestored.oCPU_Conversion);
	}

	/** The TD.Energy.EU reference (the test's single import of the tag constant). */
	static final gregapi.code.TagData TD_ENERGY = gregapi.data.TD.Energy.EU;
}
