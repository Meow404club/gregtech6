package gregtech6.tileentity.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import gregtech6.block.tank.GT6CellBlock;
import gregtech6.fluid.GTFluidLists;
import gregtech6.item.GT6CellBlockItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The cell BE offline tests (task small-tank-cell) — the LIQUIDPROOF F gas-only
 * admission gate (upstream TileEntityBase08FluidContainer.isFluidAllowed :419-421 over
 * the row pair NBT_GASPROOF T + NBT_LIQUIDPROOF F, the acceptance 装气拒液 face), the
 * tank NBT pair (capacity FIXED — no NBT_MODE, unlike the cylinder limit), the
 * stack-64 family override (upstream MultiTileEntityCell.java:76 {@code aDefault} — a
 * FILLED cell still stacks, the one small-tank family that does) and the block
 * geometry pool (MultiTileEntityCell.java:70-72, the 6x12x6 VoxelShape). The offline
 * JVM has no level and {@code isServerSide()} answers true (the
 * GT6GasCylinderBlockEntityTest precedent); the gas carrier is the temporary
 * GTFluidLists.GAS.add("water") fixture. The LIVE server leg is not exercised (RCON
 * exemption declared on the card).
 */
class GT6CellBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GT6CellBlockEntity> sType;

	@BeforeAll
	static void buildOfflineFixture() {
		// the offline holder-array form (the GT6GasCylinderBlockEntityTest.buildOfflineFixture
		// shape) — the base @BeforeAll already booted vanilla AND reopened the BE-type
		// registry write window
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6CellBlockEntity>[] tHolder = (BlockEntityType<GT6CellBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6CellBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	private static GT6CellBlockEntity cell() {
		return new GT6CellBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the family constants (the Loader :1770-1809 shared columns)
	// ---------------------------------------------------------------------------

	@Test
	void theFamilyConstantsAreTheLoaderSharedColumns() {
		assertEquals(1000, GT6CellBlockEntity.CAPACITY, "NBT_TANK_CAPACITY 1000 on every one of the 40 rows");
		assertEquals(64, GT6CellBlockEntity.STACK_SIZE, "the stack-64 registration column (every row registers 64)");
		assertEquals("gt.multitileentity.cell", cell().getTileEntityName(), "the upstream MultiTileEntityCell.java:80 name");
	}

	// ---------------------------------------------------------------------------
	// the gas-only admission gate (upstream isFluidAllowed :419-421 — LIQUIDPROOF F)
	// ---------------------------------------------------------------------------

	/** The acceptance 装气拒液 face: a plain liquid refuses at the BE handler; a gas fills (the offline GAS-carrier fixture). */
	@Test
	void theCellTakesGasAndRefusesLiquid() {
		GT6CellBlockEntity tCell = cell();
		IFluidHandler tHandler = tCell.newFluidHandler();
		assertEquals(1000, tHandler.getTankCapacity(0), "the fixed 1000 L row tank");
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				"LIQUIDPROOF F — plain water refuses (upstream :420 mLiquidProof tail)");
		assertEquals(0, tCell.mTank.amount(), "…and nothing landed");
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(1000, tHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
					"GASPROOF T — the gas band fills (upstream :420 FL.gas branch)");
			assertEquals(1000, tCell.mTank.amount());
		} finally {
			GTFluidLists.GAS.remove("water");
		}
	}

	/** Steam — the POWER_CONDUCTING list refuses even as a gas (the :420 FL.powerconducting head, the barrel :250 order). */
	@Test
	void thePowerConductingGasRefuses() {
		assertTrue(GTFluidLists.isGas("steam") && GTFluidLists.isPowerConducting("steam"), "the seed: steam is both");
		assertFalse(GT6CellBlockEntity.allowsFluid("steam"), "the power-conducting head refuses steam");
		assertTrue(GT6CellBlockEntity.allowsFluid("hydrogen"), "the plain gas band answers");
		assertFalse(GT6CellBlockEntity.allowsFluid("water"), "the plain liquid refuses");
		assertFalse(GT6CellBlockEntity.allowsFluid(null), "the null name refuses (the :419 head)");
		assertTrue(cell().newFluidHandler().getFluidInTank(0).isEmpty(), "the tank stays empty");
	}

	/** The drain carries no fluid gate — whatever got in comes back out (the barrel drain face). */
	@Test
	void theDrainCarriesNoGate() {
		GT6CellBlockEntity tCell = cell();
		GTFluidLists.GAS.add("water");
		try {
			tCell.mTank.fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		IFluidHandler tHandler = tCell.newFluidHandler();
		assertEquals(600, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount(), "the drain walks the same tank");
		assertEquals(0, tCell.mTank.amount());
	}

	// ---------------------------------------------------------------------------
	// the tank NBT pair (capacity FIXED — no NBT_MODE; the write gate keeps the key iff content)
	// ---------------------------------------------------------------------------

	/** The content rides NBT_TANK; the cell writes NO capacity key (fixed 1000 L — the cylinder's NBT_MODE face does not exist here). */
	@Test
	void theTankRoundTripsThroughNbt() {
		GT6CellBlockEntity tCell = cell();
		GTFluidLists.GAS.add("water");
		try {
			assertTrue(tCell.mTank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) > 0,
					"the gas pre-fill lands (clamped to the 1000 L tank)");
		} finally {
			GTFluidLists.GAS.remove("water");
		}

		CompoundTag tNbt = new CompoundTag();
		tCell.saveAdditional(tNbt);
		assertTrue(tNbt.contains(GT6CellBlockEntity.NBT_TANK, Tag.TAG_COMPOUND), "the content rides NBT_TANK");
		assertFalse(tNbt.contains("mode"), "the cell has NO capacity override key — the row 1000 L is fixed");

		GT6CellBlockEntity tRestored = cell();
		tRestored.load(tNbt);
		assertEquals(1000, tRestored.mTank.amount(), "the content survives the load");
		assertEquals(1000, tRestored.mTank.capacity(), "the capacity stays the fixed row value");
	}

	/** The empty cell writes NO tank key — the write gate keeps it present iff content is (the tag-less empty drop shape). */
	@Test
	void theEmptyCellWritesNoTankKey() {
		GT6CellBlockEntity tCell = cell();
		CompoundTag tNbt = tCell.writeItemNBT(new CompoundTag());
		assertFalse(tNbt.contains(GT6CellBlockEntity.NBT_TANK), "the empty tank stays unwritten (the FluidTankGT write gate)");
		assertTrue(tNbt.isEmpty(), "the item-face write of an empty cell is the tag-less drop shape");
	}

	/** The item-face pair: the drop carries the tank; the placement readback restores it; the item handler re-hydrates. */
	@Test
	void theItemNbtPairSurvivesBreakAndPlace() {
		GT6CellBlockEntity tCell = cell();
		GTFluidLists.GAS.add("water");
		try {
			tCell.mTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		} finally {
			GTFluidLists.GAS.remove("water");
		}

		// the drop face — GT6CellBlock.getDrops rides writeItemNBT onto the family item
		CompoundTag tDropTag = tCell.writeItemNBT(new CompoundTag());
		assertEquals(1000, tDropTag.getCompound(GT6CellBlockEntity.NBT_TANK).getInt("Amount"), "the content rides the drop");

		// the placement readback — the fresh BE adopts the tank
		GT6CellBlockEntity tPlaced = cell();
		tPlaced.readItemNBT(tDropTag);
		assertEquals(1000, tPlaced.mTank.amount(), "the content survives place");

		// the item handler over the same tag re-hydrates the content too
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tStack.setTag(tDropTag);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tDropTag); // 21.1: the payload rides the CustomData component
		 *///?}
		GT6CellItemFluidHandler tItemHandler = new GT6CellItemFluidHandler(tStack, GT6CellBlockEntity.CAPACITY);
		assertEquals(1000, tItemHandler.getTankCapacity(0), "the item tank rides the fixed row value");
		assertEquals(1000, tItemHandler.getFluidInTank(0).getAmount(), "the item content reads through");
	}

	/** The item-face write gate mirrors the BE gate + the template count guard; the write-back drops the emptied tag. */
	@Test
	void theItemHandlerCarriesTheSameDoors() {
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		GT6CellItemFluidHandler tHandler = new GT6CellItemFluidHandler(tStack, GT6CellBlockEntity.CAPACITY);
		assertEquals(1000, tHandler.getTankCapacity(0), "the fixed 1000 L rides the fresh handler");
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE), "LIQUIDPROOF F — the item face refuses liquid");
		//? if forge {
		assertNull(tStack.getTag(), "the refused fill wrote nothing (the stack stays tag-less)");
		//?} else {
		/*assertNull(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT), "the refused fill wrote nothing (the stack stays payload-less)");
		 *///?}

		// the template :108 count guard — a count-2 stack refuses both ways
		ItemStack tDouble = new ItemStack(Items.GLASS_BOTTLE, 2);
		GT6CellItemFluidHandler tDoubleHandler = new GT6CellItemFluidHandler(tDouble, GT6CellBlockEntity.CAPACITY);
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(0, tDoubleHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
					"count != 1 refuses the fill (the FluidHandlerItemStack template guard)");
			assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
					"the gas band fills through the item face");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		assertEquals(100, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount());
		//? if forge {
		assertNull(tStack.getTag(), "the drained cell drops the tag (the write gate keeps stacking clean)");
		//?} else {
		/*assertNull(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT), "the drained cell drops the payload (the write gate keeps stacking clean)");
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the stack-64 family override (upstream MultiTileEntityCell.java:76)
	// ---------------------------------------------------------------------------

	/** The one small-tank family that keeps stacking: the :76 override returns aDefault — filled or not, 64. */
	@Test
	void theFilledCellStillStacksToSixtyFour() {
		// the carrier block is irrelevant to the override face — fresh Block/Item ctors are
		// impossible after the offline registry freeze (the GTOfflineTestBase doc), so the
		// fixture form registers the probe item with the item latch momentarily open
		GT6CellBlockItem tItem = registerItemFixture("cell_stack_probe",
				() -> new GT6CellBlockItem(Blocks.STONE, new net.minecraft.world.item.Item.Properties().stacksTo(GT6CellBlockEntity.STACK_SIZE)));
		ItemStack tEmpty = new ItemStack(tItem);
		assertEquals(64, tItem.getMaxStackSize(tEmpty), "the empty item stacks 64");
		ItemStack tFilled = new ItemStack(tItem);
		CompoundTag tTank = new CompoundTag();
		tTank.putString("FluidName", "hydrogen");
		tTank.putInt("Amount", 1000);
		//? if forge {
		CompoundTag tTag = tFilled.getOrCreateTag();
		tTag.put(GT6CellBlockEntity.NBT_TANK, tTank);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tFilled, tTank); // 21.1: the payload rides the CustomData component
		 *///?}
		assertEquals(64, tItem.getMaxStackSize(tFilled), "the FILLED item STILL stacks 64 — the :76 aDefault override, no content gate");
	}

	// ---------------------------------------------------------------------------
	// the fill-level bucket (the LIQUID_LEVEL display face, event-driven)
	// ---------------------------------------------------------------------------

	/** The 0..8 bucket: empty = 0, the eight equal 125 L bands (1 + (amount-1)*8/capacity). */
	@Test
	void theLevelBucketWalksTheEightBands() {
		GT6CellBlockEntity tCell = cell();
		assertEquals(0, tCell.levelBucket(), "the empty tank rides level 0");
		long[] tAmounts = {1, 125, 126, 250, 251, 375, 376, 500, 501, 625, 626, 750, 751, 875, 876, 1000};
		int[] tBuckets = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8};
		for (int tIndex = 0; tIndex < tAmounts.length; tIndex++) {
			assertEquals(tBuckets[tIndex], fillTo(tCell, (int)tAmounts[tIndex]),
					tAmounts[tIndex] + "L rides bucket " + tBuckets[tIndex]);
		}
	}

	/** Fills exactly {@code aAmount} L through the (gas-gated) BE tank and returns the bucket — the gas-carrier fixture dance. */
	private static int fillTo(GT6CellBlockEntity aCell, int aAmount) {
		aCell.mTank.setEmpty();
		GTFluidLists.GAS.add("water");
		try {
			aCell.mTank.fill(new FluidStack(Fluids.WATER, aAmount), IFluidHandler.FluidAction.EXECUTE);
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		return aCell.levelBucket();
	}

	// ---------------------------------------------------------------------------
	// the geometry pin (upstream getCollisionBoundingBoxFromPool :70-72)
	// ---------------------------------------------------------------------------

	/** The 6x12x6 px canister silhouette — the collision AND selection pool (:70-72, PX_P[5]..PX_N[4]). */
	@Test
	void theShapeIsTheSixByTwelveBySixCanister() {
		VoxelShape tShape = GT6CellBlock.SHAPE;
		assertEquals(5.0 / 16.0, tShape.min(Direction.Axis.X), 1e-9, "the 5px x inset (:70 PX_P[5])");
		assertEquals(11.0 / 16.0, tShape.max(Direction.Axis.X), 1e-9, "the 11px x out (:70 PX_N[5])");
		assertEquals(0.0, tShape.min(Direction.Axis.Y), 1e-9, "floor sits on the ground");
		assertEquals(12.0 / 16.0, tShape.max(Direction.Axis.Y), 1e-9, "the 12px height (:70 PX_N[4])");
		assertEquals(5.0 / 16.0, tShape.min(Direction.Axis.Z), 1e-9, "the 5px z inset");
		assertEquals(11.0 / 16.0, tShape.max(Direction.Axis.Z), 1e-9, "the 11px z out");
	}
}
