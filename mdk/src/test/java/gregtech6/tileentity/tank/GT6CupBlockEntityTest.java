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

import gregtech6.block.tank.GT6CupBlock;
import gregtech6.fluid.GTFluidLists;
import gregtech6.item.GT6CupBlockItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The cup BE offline tests (task small-tank-cup) — the liquid-only admission door
 * (upstream TileEntityBase08FluidContainer.isFluidAllowed :419-421 over the row pair
 * NBT_LIQUIDPROOF T + NBT_GASPROOF F, the :2094 row), the drink seam core (the :158-168
 * act over isDrinkable :415-417 — the 250 L 扣量 + the REGISTER gate; the Player-visible
 * sound/food faces ride {@link gregtech6.fluid.GTDrinks#drink} whose census is the
 * GTDrinksB2Test domain, the live player is not constructible offline — the RCON
 * exemption on the card), the tank NBT pair, the content-kills-stacking item rule
 * (the base :423 over the :2094 stack column 16) and the bowl geometry pool
 * (MultiTileEntityCup.java:78-80, the 6x5x6 VoxelShape). The rain tick body needs a
 * level (the offline JVM has none) — {@code canFillWithRain()} is pinned, the formula
 * rides the base javadoc anchors.
 */
class GT6CupBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GT6CupBlockEntity> sType;

	@BeforeAll
	static void buildOfflineFixture() {
		// the offline holder-array form (the GT6CellBlockEntityTest.buildOfflineFixture shape)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6CupBlockEntity>[] tHolder = (BlockEntityType<GT6CupBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6CupBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	private static GT6CupBlockEntity cup() {
		return new GT6CupBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the family constants (the Loader :2094 row)
	// ---------------------------------------------------------------------------

	@Test
	void theRowConstantsAreTheLoaderColumns() {
		assertEquals(250, GT6CupBlockEntity.CAPACITY, "NBT_TANK_CAPACITY 250 (:2094)");
		assertEquals(16, GT6CupBlockEntity.STACK_SIZE, "the stack-16 registration column (:2094)");
		assertEquals(1800, GT6CupBlockEntity.TEMPERATURE_MAX, "NBT_TEMPERATURE = the Porcelain heat(1800) row (recorded-only)");
		assertTrue(cup().mMagicProof, "the :2094 NBT_MAGICPROOF T column (recorded — the dataset cut is the base doc)");
		assertTrue(cup().mLiquidProof, "the :2094 NBT_LIQUIDPROOF T column");
		assertFalse(cup().mGasProof, "the :2094 NBT_GASPROOF F column");
		assertTrue(cup().canFillWithRain(), "the MultiTileEntityCup.java:84 rain flag");
		assertEquals("gt.multitileentity.cup", cup().getTileEntityName(), "the upstream MultiTileEntityCup.java:88 name");
	}

	// ---------------------------------------------------------------------------
	// the liquid-only admission door (upstream isFluidAllowed :419-421 — GASPROOF F)
	// ---------------------------------------------------------------------------

	/** The acceptance face: a plain liquid fills (LIQUIDPROOF T); a gas refuses (GASPROOF F — the cell pair inverted). */
	@Test
	void theCupTakesLiquidAndRefusesGas() {
		GT6CupBlockEntity tCup = cup();
		IFluidHandler tHandler = tCup.newFluidHandler();
		assertEquals(250, tHandler.getTankCapacity(0), "the fixed 250 L row tank");
		assertEquals(200, tHandler.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE),
				"LIQUIDPROOF T — plain water fills (upstream :420 mLiquidProof branch)");
		assertEquals(200, tCup.mTank.amount(), "…and it landed");
		assertEquals(50, tHandler.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE),
				"the 250 L ceiling clamps the second pour to the 50 free litres");
		// the gas branch, vanilla-carrier form (the cell fixture): a "gas"-listed water refuses
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 50), IFluidHandler.FluidAction.EXECUTE),
					"GASPROOF F — the gas band refuses (the isGas branch keys the name, not the vanilla fluid)");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		assertEquals(250, tCup.mTank.amount(), "the tank reads full — 200 + the clamped 50");
	}

	/** Steam — the POWER_CONDUCTING list refuses even as a liquid-classed carrier (the :420 head, the barrel :250 order). */
	@Test
	void theDoorShapeMirrorsTheBaseGate() {
		assertTrue(GTFluidLists.isGas("hydrogen"), "the seed: hydrogen is a gas (the cell test's own seed)");
		assertTrue(GT6CupBlockEntity.admits("water"), "the liquid band admits");
		assertFalse(GT6CupBlockEntity.admits("hydrogen"), "the gas band refuses");
		assertFalse(GT6CupBlockEntity.admits(null), "the null name refuses (the :419 head)");
		assertTrue(cup().allowsFluid("water"), "the instance door answers the row pair too");
		assertFalse(cup().allowsFluid("hydrogen"), "the instance door refuses the gas band");
	}

	// ---------------------------------------------------------------------------
	// the drink seam (upstream onBlockActivated3 :158-168 + isDrinkable :415-417)
	// ---------------------------------------------------------------------------

	/** The acceptance 扣量 face: a ≥250 L drinkable tank drains exactly 250 L; a partial tank refuses; a non-REGISTER fluid is not drinkable. */
	@Test
	void theDrinkCoreDrainsExactlyTheUpstreamQuarter() {
		GT6CupBlockEntity tCup = cup();
		assertEquals(250, tCup.mTank.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE), "the pour");
		assertNotNull(tCup.drinkableStat(), "water is REGISTER-keyed (the :415-417 DrinksGT gate — the GTDrinks 'water' row)");
		assertTrue(tCup.consumeDrink(), "the drink act fires at 250 L");
		assertEquals(0, tCup.mTank.amount(), "the tank drains to EMPTY — 250 L of the 250 L tank (the :166 mTank.remove(250))");
		assertFalse(tCup.consumeDrink(), "an empty cup refuses");
	}

	/** Below the threshold and off-registry fluids refuse — the :415-417 double gate. */
	@Test
	void theDrinkGateIsThresholdAndRegistry() {
		GT6CupBlockEntity tPartial = cup();
		tPartial.mTank.fill(new FluidStack(Fluids.WATER, 249), IFluidHandler.FluidAction.EXECUTE);
		assertNull(tPartial.drinkableStat(), "249 L < the :416 mTank.has(250) threshold");
		assertFalse(tPartial.consumeDrink(), "the partial cup refuses");

		GT6CupBlockEntity tLava = cup();
		tLava.mTank.fill(new FluidStack(Fluids.LAVA, 250), IFluidHandler.FluidAction.EXECUTE);
		assertNotNull(tLava.mTank.getFluid(), "lava landed (a liquid — LIQUIDPROOF T admits)");
		assertNull(tLava.drinkableStat(), "lava is not REGISTER-keyed — the :417 DrinksGT.REGISTER.containsKey tail refuses");
	}

	// ---------------------------------------------------------------------------
	// the tank NBT pair (the cell form — the write gate keeps the key iff content)
	// ---------------------------------------------------------------------------

	@Test
	void theTankRoundTripsThroughNbt() {
		GT6CupBlockEntity tCup = cup();
		tCup.mTank.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE);

		CompoundTag tNbt = new CompoundTag();
		tCup.saveAdditional(tNbt);
		assertTrue(tNbt.contains(GT6CupBlockEntity.NBT_TANK, Tag.TAG_COMPOUND), "the content rides NBT_TANK");

		GT6CupBlockEntity tRestored = cup();
		tRestored.load(tNbt);
		assertEquals(250, tRestored.mTank.amount(), "the content survives the load");

		GT6CupBlockEntity tEmpty = cup();
		CompoundTag tEmptyTag = tEmpty.writeItemNBT(new CompoundTag());
		assertFalse(tEmptyTag.contains(GT6CupBlockEntity.NBT_TANK), "the empty tank stays unwritten (the tag-less drop shape)");
	}

	/** The item-face pair: the drop carries the tank; the placement readback restores it; the item handler re-hydrates. */
	@Test
	void theItemNbtPairSurvivesBreakAndPlace() {
		GT6CupBlockEntity tCup = cup();
		tCup.mTank.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE);

		CompoundTag tDropTag = tCup.writeItemNBT(new CompoundTag());
		assertEquals(250, tDropTag.getCompound(GT6CupBlockEntity.NBT_TANK).getInt("Amount"), "the content rides the drop");

		GT6CupBlockEntity tPlaced = cup();
		tPlaced.readItemNBT(tDropTag);
		assertEquals(250, tPlaced.mTank.amount(), "the content survives place");

		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tStack.setTag(tDropTag);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tDropTag);
		 *///?}
		GT6CupItemFluidHandler tItemHandler = new GT6CupItemFluidHandler(tStack);
		assertEquals(250, tItemHandler.getTankCapacity(0), "the item tank rides the fixed row value");
		assertEquals(250, tItemHandler.getFluidInTank(0).getAmount(), "the item content reads through");
	}

	/** The item handler carries the same liquid door + the template count guard; the write-back drops the emptied tag. */
	@Test
	void theItemHandlerCarriesTheSameDoors() {
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		GT6CupItemFluidHandler tHandler = new GT6CupItemFluidHandler(tStack);
		assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
				"LIQUIDPROOF T — the item face fills liquid");
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
					"GASPROOF F — the item face refuses the gas band (the cell fixture form)");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		assertEquals(100, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount());
		//? if forge {
		assertNull(tStack.getTag(), "the drained cup drops the tag (the write gate keeps stacking clean)");
		//?} else {
		/*assertNull(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT), "the drained cup drops the payload");
		 *///?}

		ItemStack tDouble = new ItemStack(Items.GLASS_BOTTLE, 2);
		GT6CupItemFluidHandler tDoubleHandler = new GT6CupItemFluidHandler(tDouble);
		assertEquals(0, tDoubleHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
				"count != 1 refuses the fill (the FluidHandlerItemStack template :108 guard)");
	}

	// ---------------------------------------------------------------------------
	// the content-kills-stacking rule (the base :423 over the :2094 column 16)
	// ---------------------------------------------------------------------------

	/** The empty cup stacks 16 (the registration column); the filled one stacks 1 (the :423 mTank.has() gate). */
	@Test
	void theFilledCupStacksOneTheEmptyOneStacksSixteen() {
		GT6CupBlockItem tItem = registerItemFixture("cup_stack_probe",
				() -> new GT6CupBlockItem(Blocks.STONE, new net.minecraft.world.item.Item.Properties().stacksTo(GT6CupBlockEntity.STACK_SIZE)));
		ItemStack tEmpty = new ItemStack(tItem);
		assertEquals(16, tItem.getMaxStackSize(tEmpty), "the empty item stacks 16 (the :2094 column)");
		ItemStack tFilled = new ItemStack(tItem);
		CompoundTag tTank = new CompoundTag();
		tTank.putString("FluidName", "water");
		tTank.putInt("Amount", 250);
		//? if forge {
		CompoundTag tTag = tFilled.getOrCreateTag();
		tTag.put(GT6CupBlockEntity.NBT_TANK, tTank);
		//?} else {
		/*// the producer (GT6CupBlock.getDrops) ships the whole envelope as the CustomData root —
		//the tank rides the NBT_TANK key inside it, the bare tank misses the read gate
		CompoundTag tEnvelope = new CompoundTag();
		tEnvelope.put(GT6CupBlockEntity.NBT_TANK, tTank);
		net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tFilled, tEnvelope);
		 *///?}
		assertEquals(1, tItem.getMaxStackSize(tFilled), "the FILLED item stacks 1 — the :423 content gate");
	}

	// ---------------------------------------------------------------------------
	// the fill-level bucket (the LIQUID_LEVEL display face, the cell formula over 250 L)
	// ---------------------------------------------------------------------------

	/** The 0..8 bucket over the 250 L tank: bands of 31.25 L (1 + (amount-1)*8/capacity). */
	@Test
	void theLevelBucketWalksTheEightBands() {
		GT6CupBlockEntity tCup = cup();
		assertEquals(0, tCup.levelBucket(), "the empty tank rides level 0");
		long[] tAmounts = {1, 32, 33, 63, 64, 125, 126, 250};
		int[] tBuckets = {1, 1, 2, 2, 3, 4, 5, 8};
		for (int tIndex = 0; tIndex < tAmounts.length; tIndex++) {
			tCup.mTank.setEmpty();
			tCup.mTank.fill(new FluidStack(Fluids.WATER, (int)tAmounts[tIndex]), IFluidHandler.FluidAction.EXECUTE);
			assertEquals(tBuckets[tIndex], tCup.levelBucket(),
					tAmounts[tIndex] + "L rides bucket " + tBuckets[tIndex]);
		}
	}

	// ---------------------------------------------------------------------------
	// the geometry pin (upstream getCollisionBoundingBoxFromPool :78-80)
	// ---------------------------------------------------------------------------

	/** The 6x5x6 px bowl silhouette — the collision AND selection pool (:78-80, PX_P[5]..PX_N[5]x0..PX_N[11]). */
	@Test
	void theShapeIsTheSixByFiveBySixBowl() {
		VoxelShape tShape = GT6CupBlock.SHAPE;
		assertEquals(5.0 / 16.0, tShape.min(Direction.Axis.X), 1e-9, "the 5px x inset (:78 PX_P[5])");
		assertEquals(11.0 / 16.0, tShape.max(Direction.Axis.X), 1e-9, "the 11px x out (:78 PX_N[5])");
		assertEquals(0.0, tShape.min(Direction.Axis.Y), 1e-9, "floor sits on the ground");
		assertEquals(5.0 / 16.0, tShape.max(Direction.Axis.Y), 1e-9, "the 5px height (:79 PX_N[11])");
		assertEquals(5.0 / 16.0, tShape.min(Direction.Axis.Z), 1e-9, "the 5px z inset");
		assertEquals(11.0 / 16.0, tShape.max(Direction.Axis.Z), 1e-9, "the 11px z out");
	}
}
