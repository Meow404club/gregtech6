package gregtech6.tileentity.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.block.tank.GT6GasCylinderBlock;
import gregtech6.fluid.GTFluidLists;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The gas cylinder BE offline tests (task small-tank-gas-cylinder) — the eight-band
 * hitY fill-limit walk (upstream MultiTileEntityBarometerGasCylinder.onBlockActivated3
 * :73-110, the GT6 signature interaction at twice the measuring-pot band count) driven
 * straight against the server-side chain, the LIQUIDPROOF F gas-only admission gate
 * (upstream isFluidAllowed :419-421 — the acceptance 液体拒装 face), the NBT_MODE
 * persistence pair (:49-59 + the :61-65 item face) and the block geometry pool
 * (:144-146, the 8x16x8 VoxelShape). The offline JVM has no level and
 * {@code isServerSide()} answers true (the GT6MeasuringPotBlockEntityTest precedent);
 * the gas carrier is the temporary GTFluidLists.GAS.add("water") fixture (the
 * GTGeneratorLiquidBlockEntityTest pattern — live, GTFluids.STEAM registration).
 * The LIVE server leg is not exercised (RCON exemption declared on the card).
 */
class GT6GasCylinderBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GT6GasCylinderBlockEntity> sType;

	@BeforeAll
	static void buildOfflineFixture() {
		// the offline holder-array form (the GT6MeasuringPotBlockEntityTest.buildOfflineFixture
		// shape) — the base @BeforeAll already booted vanilla AND reopened the BE-type
		// registry write window
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6GasCylinderBlockEntity>[] tHolder = (BlockEntityType<GT6GasCylinderBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6GasCylinderBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	private static GT6GasCylinderBlockEntity cylinder() {
		return new GT6GasCylinderBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the eight-band hitY walk (upstream :77-104, coarse steps — the null player never sneaks)
	// ---------------------------------------------------------------------------

	@Test
	void theUpperBandsRaise() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		tCyl.mTank.setCapacity(1000);
		assertEquals("Limit: 1500L", tCyl.limitChain(null, 15.0F / 16.0F), "the >14px band commits +500 (:79-80)");
		tCyl.limitChain(null, 13.0F / 16.0F);
		assertEquals(1600, tCyl.mTank.capacity(), "the 12-14px band commits +100 (:81-82)");
		tCyl.limitChain(null, 11.0F / 16.0F);
		assertEquals(1650, tCyl.mTank.capacity(), "the 10-12px band commits +50 (:85-86)");
		tCyl.limitChain(null, 9.0F / 16.0F);
		assertEquals(1660, tCyl.mTank.capacity(), "the 8-10px band commits +10 (:87-88)");
	}

	@Test
	void theLowerBandsLower() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		assertEquals(8000, tCyl.mTank.capacity(), "the fresh cylinder starts at the class default");
		assertEquals("Limit: 7990L", tCyl.limitChain(null, 7.0F / 16.0F), "the 6-8px band commits -10 (:93-94)");
		assertEquals("Limit: 7940L", tCyl.limitChain(null, 5.0F / 16.0F), "the 4-6px band commits -50 (:95-96)");
		assertEquals("Limit: 7840L", tCyl.limitChain(null, 3.0F / 16.0F), "the 2-4px band commits -100 (:99-100)");
		assertEquals("Limit: 7340L", tCyl.limitChain(null, 1.0F / 16.0F), "the <=2px band commits -500 (:101-102)");
	}

	/** The exact band-boundary pixels ride the lower band (the upstream strict > comparisons over PX_P[2..14]). */
	@Test
	void theBoundaryPixelsStayStrict() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		assertEquals("Limit: 7990L", tCyl.limitChain(null, 8.0F / 16.0F), "hitY == 8px is NOT > 8px — the -10 band (:93-94), not the +10 half");
		assertEquals("Limit: 7940L", tCyl.limitChain(null, 6.0F / 16.0F), "hitY == 6px is NOT > 6px — the -50 band (:95-96)");
		assertEquals("Limit: 7840L", tCyl.limitChain(null, 4.0F / 16.0F), "hitY == 4px is NOT > 4px — the -100 band (:99-100)");
		assertEquals("Limit: 7340L", tCyl.limitChain(null, 2.0F / 16.0F), "hitY == 2px is NOT > 2px — the -500 band (:101-102)");
		tCyl.mTank.setCapacity(1000);
		assertEquals("Limit: 1100L", tCyl.limitChain(null, 14.0F / 16.0F), "hitY == 14px is NOT > 14px — the +100 band (:81-82), not the +500 half");
		assertEquals("Limit: 1150L", tCyl.limitChain(null, 12.0F / 16.0F), "hitY == 12px is NOT > 12px — the +50 band (:85-86)");
	}

	/** The bind clamp: [1, 8000] — the upstream UT.Code.bind(1, mCapacity, ...) against the CLASS default (:80). */
	@Test
	void theBindClampsToEightThousandAndOne() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		tCyl.mTank.setCapacity(7900);
		assertEquals("Limit: 8000L", tCyl.limitChain(null, 15.0F / 16.0F), "the ceiling is the class default 8000, not 7900+500");
		tCyl.mTank.setCapacity(400);
		assertEquals("Limit: 1L", tCyl.limitChain(null, 1.0F / 16.0F), "the floor is 1 (upstream bind(1, ...))");
		assertEquals(1, tCyl.mTank.capacity());
	}

	/** The sneak pairs (upstream :80/:82/:86/:88/:94/:96/:100/:102 ternaries) — the static band face (a live sneaking Player is not constructible offline). */
	@Test
	void theSneakStepsDivideByTen() {
		assertEquals(GT6GasCylinderBlockEntity.STEP_FINE_50, GT6GasCylinderBlockEntity.band(true, GT6GasCylinderBlockEntity.STEP_500, GT6GasCylinderBlockEntity.STEP_FINE_50), "sneaking swaps 500 -> 50");
		assertEquals(GT6GasCylinderBlockEntity.STEP_FINE_10, GT6GasCylinderBlockEntity.band(true, GT6GasCylinderBlockEntity.STEP_100, GT6GasCylinderBlockEntity.STEP_FINE_10), "sneaking swaps 100 -> 10");
		assertEquals(GT6GasCylinderBlockEntity.STEP_FINE_5, GT6GasCylinderBlockEntity.band(true, GT6GasCylinderBlockEntity.STEP_50, GT6GasCylinderBlockEntity.STEP_FINE_5), "sneaking swaps 50 -> 5");
		assertEquals(GT6GasCylinderBlockEntity.STEP_FINE_1, GT6GasCylinderBlockEntity.band(true, GT6GasCylinderBlockEntity.STEP_10, GT6GasCylinderBlockEntity.STEP_FINE_1), "sneaking swaps 10 -> 1");
		assertEquals(GT6GasCylinderBlockEntity.STEP_500, GT6GasCylinderBlockEntity.band(false, GT6GasCylinderBlockEntity.STEP_500, GT6GasCylinderBlockEntity.STEP_FINE_50), "plain keeps 500");
		assertEquals(GT6GasCylinderBlockEntity.STEP_10, GT6GasCylinderBlockEntity.band(false, GT6GasCylinderBlockEntity.STEP_10, GT6GasCylinderBlockEntity.STEP_FINE_1), "plain keeps 10");
	}

	// ---------------------------------------------------------------------------
	// the gas-only admission gate (upstream isFluidAllowed :419-421 — LIQUIDPROOF F)
	// ---------------------------------------------------------------------------

	/** The acceptance 液体拒装 face: a plain liquid refuses at the BE handler; a gas fills (the offline GAS-carrier fixture). */
	@Test
	void theCylinderTakesGasAndRefusesLiquid() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		IFluidHandler tHandler = tCyl.newFluidHandler();
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				"LIQUIDPROOF F — plain water refuses (upstream :420 mLiquidProof tail)");
		assertEquals(0, tCyl.mTank.amount(), "…and nothing landed");
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(1000, tHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
					"GASPROOF T — the gas band fills (upstream :420 FL.gas branch)");
			assertEquals(1000, tCyl.mTank.amount());
		} finally {
			GTFluidLists.GAS.remove("water");
		}
	}

	/** Steam — the POWER_CONDUCTING list refuses even as a gas (the :420 FL.powerconducting head, the barrel :250 order). */
	@Test
	void thePowerConductingGasRefuses() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		assertTrue(GTFluidLists.isGas("steam") && GTFluidLists.isPowerConducting("steam"), "the seed: steam is both");
		IFluidHandler tHandler = tCyl.newFluidHandler();
		// steam's own registry path IS the conducting gas — the vanilla boot has no steam
		// fluid, so the name gate is pinned directly
		assertFalse(GT6GasCylinderBlockEntity.allowsFluid("steam"), "the power-conducting head refuses steam");
		assertTrue(GT6GasCylinderBlockEntity.allowsFluid("hydrogen"), "the plain gas band answers");
		assertFalse(GT6GasCylinderBlockEntity.allowsFluid("water"), "the plain liquid refuses");
		assertTrue(tHandler.getFluidInTank(0).isEmpty(), "the tank stays empty");
	}

	/** The drain carries no fluid gate — whatever got in comes back out (the barrel drain face). */
	@Test
	void theDrainCarriesNoGate() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		GTFluidLists.GAS.add("water");
		try {
			tCyl.mTank.fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		IFluidHandler tHandler = tCyl.newFluidHandler();
		assertEquals(600, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount(), "the drain walks the same tank");
		assertEquals(0, tCyl.mTank.amount());
	}

	// ---------------------------------------------------------------------------
	// NBT_MODE persistence (upstream writeToNBT2 :55-59 / readFromNBT2 :49-53 / the item face :61-65)
	// ---------------------------------------------------------------------------

	/** The adjusted capacity rides NBT_MODE — written only off the default, restored on load, the content rides NBT_TANK. */
	@Test
	void theModeAndTankRoundTripThroughNbt() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		tCyl.limitChain(null, 1.0F / 16.0F); // 8000 -> 7500
		assertEquals(7500, tCyl.mTank.capacity());
		GTFluidLists.GAS.add("water");
		try {
			assertNotNull(tCyl.mTank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) > 0 ? tCyl : null,
					"the gas pre-fill lands");
		} finally {
			GTFluidLists.GAS.remove("water");
		}

		CompoundTag tNbt = new CompoundTag();
		tCyl.saveAdditional(tNbt);
		assertEquals(7500, tNbt.getLong(GT6GasCylinderBlockEntity.NBT_MODE), "the off-default capacity rides NBT_MODE (:57)");
		assertTrue(tNbt.contains(GT6GasCylinderBlockEntity.NBT_TANK, Tag.TAG_COMPOUND), "the content rides NBT_TANK (:58)");

		GT6GasCylinderBlockEntity tRestored = cylinder();
		tRestored.load(tNbt);
		assertEquals(7500, tRestored.mTank.capacity(), "the limit survives the load (:52)");
		assertEquals(2000, tRestored.mTank.amount(), "the content survives the load");
	}

	/** The class-default capacity writes NO NBT_MODE (the upstream :57 guard). */
	@Test
	void theDefaultCapacityWritesNoMode() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		CompoundTag tNbt = new CompoundTag();
		tCyl.saveAdditional(tNbt);
		assertFalse(tNbt.contains(GT6GasCylinderBlockEntity.NBT_MODE), "the untouched cylinder carries no mode override");
	}

	/** The item-face pair (upstream writeItemNBT2 :61-65): the drop carries tank+mode; the placement readback restores both. */
	@Test
	void theItemNbtPairSurvivesBreakAndPlace() {
		GT6GasCylinderBlockEntity tCyl = cylinder();
		tCyl.limitChain(null, 1.0F / 16.0F); // 7500
		GTFluidLists.GAS.add("water");
		try {
			tCyl.mTank.fill(new FluidStack(Fluids.WATER, 1234), IFluidHandler.FluidAction.EXECUTE);
		} finally {
			GTFluidLists.GAS.remove("water");
		}

		// the drop face — GT6GasCylinderBlock.getDrops rides writeItemNBT onto the family item
		CompoundTag tDropTag = tCyl.writeItemNBT(new CompoundTag());
		assertEquals(7500, tDropTag.getLong(GT6GasCylinderBlockEntity.NBT_MODE), "the mode rides the drop (:63)");
		assertEquals(1234, tDropTag.getCompound(GT6GasCylinderBlockEntity.NBT_TANK).getInt("Amount"), "the content rides the drop");

		// the placement readback — the fresh BE adopts the pair
		GT6GasCylinderBlockEntity tPlaced = cylinder();
		tPlaced.readItemNBT(tDropTag);
		assertEquals(7500, tPlaced.mTank.capacity(), "the limit survives place");
		assertEquals(1234, tPlaced.mTank.amount(), "the content survives place");

		// the item handler over the same tag re-hydrates the adjusted capacity too
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tStack.setTag(tDropTag);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tDropTag); // 21.1: the payload rides the CustomData component
		 *///?}
		GT6GasCylinderItemFluidHandler tItemHandler = new GT6GasCylinderItemFluidHandler(tStack, GT6GasCylinderBlockEntity.DEFAULT_CAPACITY);
		assertEquals(7500, tItemHandler.getTankCapacity(0), "the item tank re-binds off the mode key");
		assertEquals(1234, tItemHandler.getFluidInTank(0).getAmount(), "the item content reads through");
	}

	/** The item-face write gate mirrors the BE gate: liquid refuses, gas fills, the write-back drops the drained-default tag. */
	@Test
	void theItemHandlerCarriesTheSameDoors() {
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		GT6GasCylinderItemFluidHandler tHandler = new GT6GasCylinderItemFluidHandler(tStack, GT6GasCylinderBlockEntity.DEFAULT_CAPACITY);
		assertEquals(8000, tHandler.getTankCapacity(0), "the class default rides the fresh handler");
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE), "LIQUIDPROOF F — the item face refuses liquid");
		//? if forge {
		assertNull(tStack.getTag(), "the refused fill wrote nothing (the stack stays tag-less, stacking intact)");
		//?} else {
		/*assertNull(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT), "the refused fill wrote nothing (the stack stays payload-less, stacking intact)");
		 *///?}

		GTFluidLists.GAS.add("water");
		try {
			assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE), "the gas band fills through the item face");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		assertEquals(100, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount());
		//? if forge {
		assertNull(tStack.getTag(), "the drained default-limit cylinder drops the tag — stacking restored (the barrel write gate)");
		//?} else {
		/*assertNull(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT), "the drained default-limit cylinder drops the payload — stacking restored (the barrel write gate)");
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the geometry pin (upstream getCollisionBoundingBoxFromPool :144)
	// ---------------------------------------------------------------------------

	/** The 8x16x8 px full-height bell silhouette — the collision AND selection pool (:144-146). */
	@Test
	void theShapeIsTheEightBySixteenByEightBell() {
		VoxelShape tShape = GT6GasCylinderBlock.SHAPE;
		assertEquals(4.0 / 16.0, tShape.min(net.minecraft.core.Direction.Axis.X), 1e-9, "the 4px x inset (:144 PX_P[4])");
		assertEquals(12.0 / 16.0, tShape.max(net.minecraft.core.Direction.Axis.X), 1e-9, "the 12px x out (:144 PX_N[4])");
		assertEquals(0.0, tShape.min(net.minecraft.core.Direction.Axis.Y), 1e-9, "full height floor");
		assertEquals(1.0, tShape.max(net.minecraft.core.Direction.Axis.Y), 1e-9, "full height ceiling");
		assertEquals(4.0 / 16.0, tShape.min(net.minecraft.core.Direction.Axis.Z), 1e-9, "the 4px z inset");
		assertEquals(12.0 / 16.0, tShape.max(net.minecraft.core.Direction.Axis.Z), 1e-9, "the 12px z out");
	}
}
