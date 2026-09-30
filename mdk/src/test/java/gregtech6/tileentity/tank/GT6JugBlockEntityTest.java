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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.block.tank.GT6JugBlock;
import gregtech6.fluid.GTFluidLists;
import gregtech6.item.GT6JugBlockItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The jug BE offline tests (task small-tank-jug) — the row columns
 * (Loader_MultiTileEntities.java:2095: 2000 L / stack 16 / LIQUIDPROOF T / MAGICPROOF F
 * — the cup's magic-T is the exception, not this row / the Ceramic heat(2000) recorded
 * temperature), the liquid-only admission door (the base :419-421 gate over the pair),
 * the two jug-only flags (:87-88), the 2000 L ceiling, the tank NBT pair, the item
 * handler doors, the content-kills-stacking rule (the base :423 over the column 16),
 * the 0..8 level bucket over 2000 L (250 L bands), the top-only drink face
 * (MultiTileEntityJug.java:90) and the 10x14x10 geometry pool (:81-83). The scoop and
 * watering faces pin through their pure cores ({@link GT6JugBlockItem#isScoopableSource},
 * {@link GT6JugBlockItem#scoopInto}, {@link GT6JugBlockItem#cauldronTier}) — the level
 * mutation lines (setBlock) and the rain tick body need a live level, the RCON
 * exemption on the card.
 */
class GT6JugBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GT6JugBlockEntity> sType;

	@BeforeAll
	static void buildOfflineFixture() {
		// the offline holder-array form (the GT6CupBlockEntityTest.buildOfflineFixture shape)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6JugBlockEntity>[] tHolder = (BlockEntityType<GT6JugBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6JugBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	private static GT6JugBlockEntity jug() {
		return new GT6JugBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the family constants (the Loader :2095 row)
	// ---------------------------------------------------------------------------

	@Test
	void theRowConstantsAreTheLoaderColumns() {
		assertEquals(2000, GT6JugBlockEntity.CAPACITY, "NBT_TANK_CAPACITY 2000 (:2095)");
		assertEquals(16, GT6JugBlockEntity.STACK_SIZE, "the stack-16 registration column (:2095)");
		assertEquals(2000, GT6JugBlockEntity.TEMPERATURE_MAX, "NBT_TEMPERATURE = the Ceramic heat(2000) row (MT.java:1279, recorded-only)");
		assertTrue(GT6JugBlockEntity.CAN_WATER_CROPS, "the MultiTileEntityJug.java:87 canWaterCrops flag");
		assertTrue(GT6JugBlockEntity.CAN_PICK_UP_FLUIDS, "the MultiTileEntityJug.java:88 canPickUpFluids flag");
		assertTrue(jug().mLiquidProof, "the :2095 NBT_LIQUIDPROOF T column");
		assertFalse(jug().mGasProof, "the :2095 NBT_GASPROOF F column");
		assertFalse(jug().mMagicProof, "the :2095 NBT_MAGICPROOF F column — the cup's T is the exception, not this row");
		assertTrue(jug().canFillWithRain(), "the MultiTileEntityJug.java:89 rain flag");
		assertEquals("gt.multitileentity.jug", jug().getTileEntityName(), "the upstream MultiTileEntityJug.java:93 name");
	}

	// ---------------------------------------------------------------------------
	// the liquid-only admission door + the 2000 L ceiling (:419-421 over :2095)
	// ---------------------------------------------------------------------------

	@Test
	void theJugTakesLiquidAndRefusesGas() {
		GT6JugBlockEntity tJug = jug();
		IFluidHandler tHandler = tJug.newFluidHandler();
		assertEquals(2000, tHandler.getTankCapacity(0), "the fixed 2000 L row tank");
		assertEquals(2000, tHandler.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE),
				"LIQUIDPROOF T — the full 2000 L pour lands (water AND lava both ride the liquid band)");
		assertEquals(2000, tJug.mTank.amount(), "the tank reads full");
		// the gas branch, vanilla-carrier form (the cup fixture): a "gas"-listed water refuses
		GT6JugBlockEntity tFresh = jug();
		IFluidHandler tFreshHandler = tFresh.newFluidHandler();
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(0, tFreshHandler.fill(new FluidStack(Fluids.WATER, 50), IFluidHandler.FluidAction.EXECUTE),
					"GASPROOF F — the gas band refuses (the isGas branch keys the name, not the vanilla fluid)");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		assertEquals(0, tFresh.mTank.amount(), "nothing landed");
	}

	/** The acceptance ceiling face: 1900 L in, a 200 L pour clamps to the 100 free litres. */
	@Test
	void the2000LCeilingClampsThePour() {
		GT6JugBlockEntity tJug = jug();
		IFluidHandler tHandler = tJug.newFluidHandler();
		assertEquals(1900, tHandler.fill(new FluidStack(Fluids.WATER, 1900), IFluidHandler.FluidAction.EXECUTE), "the 1900 L pour");
		assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE),
				"the 2000 L ceiling clamps the second pour to the 100 free litres");
		assertEquals(2000, tJug.mTank.amount(), "the tank reads full");
		// lava lands too (the scoop face needs the lava half of the liquid band)
		GT6JugBlockEntity tLava = jug();
		assertEquals(1000, tLava.mTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE),
				"lava is a liquid — LIQUIDPROOF T admits (the :295 lava branch of the scoop)");
	}

	// ---------------------------------------------------------------------------
	// the drink seam (the base :158-168/:415-417 over the 2000 L tank — 250 L bands)
	// ---------------------------------------------------------------------------

	@Test
	void theDrinkCoreDrainsExactlyTheUpstreamQuarter() {
		GT6JugBlockEntity tJug = jug();
		assertEquals(250, tJug.mTank.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE), "a quarter tank");
		assertNotNull(tJug.drinkableStat(), "water is REGISTER-keyed (the :415-417 gate — the 250 L threshold met)");
		assertTrue(tJug.consumeDrink(), "the drink act fires at 250 L held");
		assertEquals(0, tJug.mTank.amount(), "the tank drains to EMPTY — the :166 mTank.remove(250)");
		assertFalse(tJug.consumeDrink(), "an empty jug refuses");
	}

	// ---------------------------------------------------------------------------
	// the tank NBT pair + the item round trip (the cup form)
	// ---------------------------------------------------------------------------

	@Test
	void theTankRoundTripsThroughNbt() {
		GT6JugBlockEntity tJug = jug();
		tJug.mTank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);

		CompoundTag tNbt = new CompoundTag();
		tJug.saveAdditional(tNbt);
		assertTrue(tNbt.contains(GT6JugBlockEntity.NBT_TANK, Tag.TAG_COMPOUND), "the content rides NBT_TANK");

		GT6JugBlockEntity tRestored = jug();
		tRestored.load(tNbt);
		assertEquals(2000, tRestored.mTank.amount(), "the content survives the load");

		GT6JugBlockEntity tEmpty = jug();
		CompoundTag tEmptyTag = tEmpty.writeItemNBT(new CompoundTag());
		assertFalse(tEmptyTag.contains(GT6JugBlockEntity.NBT_TANK), "the empty tank stays unwritten (the tag-less drop shape)");
	}

	/** The item-face pair: the drop carries the tank; the placement readback restores it; the item handler re-hydrates. */
	@Test
	void theItemNbtPairSurvivesBreakAndPlace() {
		GT6JugBlockEntity tJug = jug();
		tJug.mTank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);

		CompoundTag tDropTag = tJug.writeItemNBT(new CompoundTag());
		assertEquals(2000, tDropTag.getCompound(GT6JugBlockEntity.NBT_TANK).getInt("Amount"), "the content rides the drop");

		GT6JugBlockEntity tPlaced = jug();
		tPlaced.readItemNBT(tDropTag);
		assertEquals(2000, tPlaced.mTank.amount(), "the content survives place");

		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tStack.setTag(tDropTag);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tDropTag);
		 *///?}
		GT6JugItemFluidHandler tItemHandler = new GT6JugItemFluidHandler(tStack);
		assertEquals(2000, tItemHandler.getTankCapacity(0), "the item tank rides the fixed row value");
		assertEquals(2000, tItemHandler.getFluidInTank(0).getAmount(), "the item content reads through");
	}

	/** The item handler carries the same liquid door + the template count guard; the write-back drops the emptied tag. */
	@Test
	void theItemHandlerCarriesTheSameDoors() {
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		GT6JugItemFluidHandler tHandler = new GT6JugItemFluidHandler(tStack);
		assertEquals(100, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
				"LIQUIDPROOF T — the item face fills liquid");
		GTFluidLists.GAS.add("water");
		try {
			assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
					"GASPROOF F — the item face refuses the gas band (the cup fixture form)");
		} finally {
			GTFluidLists.GAS.remove("water");
		}
		assertEquals(100, tHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount());
		//? if forge {
		assertNull(tStack.getTag(), "the drained jug drops the tag (the write gate keeps stacking clean)");
		//?} else {
		/*assertNull(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT), "the drained jug drops the payload");
		 *///?}

		ItemStack tDouble = new ItemStack(Items.GLASS_BOTTLE, 2);
		GT6JugItemFluidHandler tDoubleHandler = new GT6JugItemFluidHandler(tDouble);
		assertEquals(0, tDoubleHandler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
				"count != 1 refuses the fill (the FluidHandlerItemStack template :108 guard)");
	}

	// ---------------------------------------------------------------------------
	// the content-kills-stacking rule (the base :423 over the :2095 column 16)
	// ---------------------------------------------------------------------------

	@Test
	void theFilledJugStacksOneTheEmptyOneStacksSixteen() {
		GT6JugBlockItem tItem = registerItemFixture("jug_stack_probe",
				() -> new GT6JugBlockItem(Blocks.STONE, new net.minecraft.world.item.Item.Properties().stacksTo(GT6JugBlockEntity.STACK_SIZE)));
		ItemStack tEmpty = new ItemStack(tItem);
		assertEquals(16, tItem.getMaxStackSize(tEmpty), "the empty item stacks 16 (the :2095 column)");
		ItemStack tFilled = new ItemStack(tItem);
		CompoundTag tTank = new CompoundTag();
		tTank.putString("FluidName", "water");
		tTank.putInt("Amount", 2000);
		//? if forge {
		CompoundTag tTag = tFilled.getOrCreateTag();
		tTag.put(GT6JugBlockEntity.NBT_TANK, tTank);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tFilled, tTank);
		 *///?}
		assertEquals(1, tItem.getMaxStackSize(tFilled), "the FILLED item stacks 1 — the :423 content gate");
	}

	// ---------------------------------------------------------------------------
	// the fill-level bucket (the LIQUID_LEVEL display face — 250 L bands over 2000 L)
	// ---------------------------------------------------------------------------

	/** The 0..8 bucket over the 2000 L tank: bands of 250 L (1 + (amount-1)*8/2000). */
	@Test
	void theLevelBucketWalksTheEightBands() {
		GT6JugBlockEntity tJug = jug();
		assertEquals(0, tJug.levelBucket(), "the empty tank rides level 0");
		long[] tAmounts = {1, 250, 251, 500, 501, 1000, 1750, 1751, 2000};
		int[] tBuckets = {1, 1, 2, 2, 3, 4, 7, 8, 8};
		for (int tIndex = 0; tIndex < tAmounts.length; tIndex++) {
			tJug.mTank.setEmpty();
			tJug.mTank.fill(new FluidStack(Fluids.WATER, (int)tAmounts[tIndex]), IFluidHandler.FluidAction.EXECUTE);
			assertEquals(tBuckets[tIndex], tJug.levelBucket(),
					tAmounts[tIndex] + "L rides bucket " + tBuckets[tIndex]);
		}
	}

	// ---------------------------------------------------------------------------
	// the top-only drink face (upstream canDrinkFromSide :90 → SIDES_TOP)
	// ---------------------------------------------------------------------------

	/** The jug drinks from the TOP face only — the cup drank from any side, this is the one behavioural override. */
	@Test
	void theDrinkFaceIsTheTopSideOnly() {
		assertTrue(GT6JugBlock.drinksFromSide(Direction.UP), "the top face drinks (:90 SIDES_TOP)");
		for (Direction tSide : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
			assertFalse(GT6JugBlock.drinksFromSide(tSide), tSide + " refuses the drink (the :90 gate)");
		}
	}

	// ---------------------------------------------------------------------------
	// the scoop face (the pure cores of the upstream onItemRightClick :270-342)
	// ---------------------------------------------------------------------------

	/** The source gate: water AND lava sources scoop; flowing states and the empty state refuse. */
	@Test
	void theScoopGateIsSourceStatesOnly() {
		assertTrue(GT6JugBlockItem.isScoopableSource(Fluids.WATER.defaultFluidState()), "the water source scoops (:277 metadata-0)");
		assertTrue(GT6JugBlockItem.isScoopableSource(Fluids.LAVA.defaultFluidState()), "the lava source scoops (:295 metadata-0)");
		assertFalse(GT6JugBlockItem.isScoopableSource(Fluids.FLOWING_WATER.defaultFluidState()),
				"the flowing state refuses (the metadata!=0 half of the :277 check)");
		assertFalse(GT6JugBlockItem.isScoopableSource(Fluids.EMPTY.defaultFluidState()), "the empty state refuses");
		assertFalse(GT6JugBlockItem.isScoopableSource(null), "the null state refuses");
	}

	/** The scoop core: a source fills exactly 1000 L through the item handler; a full tank (or a partial free space) refuses. */
	@Test
	void theScoopCoreFillsExactlyOneBucketOrNothing() {
		GT6JugItemFluidHandler tHandler = new GT6JugItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE));
		assertEquals(1000, GT6JugBlockItem.scoopInto(tHandler, Fluids.WATER.defaultFluidState()),
				"the water source scoops the full bucket (:281 FL.Water.make(1000))");
		assertEquals(1000, tHandler.getFluidInTank(0).getAmount(), "the 1000 L landed in the item tank");

		GT6JugItemFluidHandler tLavaHandler = new GT6JugItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE));
		assertEquals(1000, GT6JugBlockItem.scoopInto(tLavaHandler, Fluids.LAVA.defaultFluidState()),
				"the lava source scoops the full bucket (:295 FL.Lava.make(1000))");

		GT6JugItemFluidHandler tFull = new GT6JugItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE));
		tFull.fill(new FluidStack(Fluids.WATER, 1500), IFluidHandler.FluidAction.EXECUTE);
		assertEquals(0, GT6JugBlockItem.scoopInto(tFull, Fluids.WATER.defaultFluidState()),
				"only 500 L free — the simulate gate refuses the pour (the :283 fill==1000 face)");
	}

	// ---------------------------------------------------------------------------
	// the watering face (the pure tier table of the upstream onItemUseFirst :215-227)
	// ---------------------------------------------------------------------------

	/** The 334/667/1000 equivalence face: the three tiers over the empty and partial cauldron levels, plus the refusals. */
	@Test
	void theCauldronTiersAreTheUpstreamThreeBands() {
		// the :217 1000/full branch — from the empty cauldron only
		assertArray(GT6JugBlockItem.cauldronTier(1000, 0), 1000, 3, "1000 L into the empty cauldron fills it (:217 meta+3)");
		assertArray(GT6JugBlockItem.cauldronTier(2000, 0), 1000, 3, "the pour clamps at the full cauldron dose");
		// the :220 667/+2 branch — level <= 1
		assertArray(GT6JugBlockItem.cauldronTier(667, 0), 667, 2, "667 L into the empty cauldron (:220 meta+2)");
		assertArray(GT6JugBlockItem.cauldronTier(667, 1), 667, 3, "667 L tops a level-1 cauldron (:220)");
		// the :224 334/+1 tail — the residual
		assertArray(GT6JugBlockItem.cauldronTier(334, 0), 334, 1, "334 L into the empty cauldron (:224 meta+1)");
		assertArray(GT6JugBlockItem.cauldronTier(334, 2), 334, 3, "334 L tops a level-2 cauldron (:224)");
		assertArray(GT6JugBlockItem.cauldronTier(999, 0), 667, 2, "999 L < the 1000 dose rides the 667 band (the :220 cascade)");
		// the :215 head refusals
		assertNull(GT6JugBlockItem.cauldronTier(1000, 3), "the full cauldron refuses (the :215 aMeta >= 3 head)");
		assertNull(GT6JugBlockItem.cauldronTier(333, 0), "below the 334 L head refuses (the :215 amount head)");
		assertNull(GT6JugBlockItem.cauldronTier(0, 0), "the empty tank refuses");
	}

	private static void assertArray(int[] aActual, int aDrain, int aLevel, String aMessage) {
		assertNotNull(aActual, aMessage);
		assertEquals(aDrain, aActual[0], aMessage + " — the drain column");
		assertEquals(aLevel, aActual[1], aMessage + " — the level column");
	}

	// ---------------------------------------------------------------------------
	// the geometry pin (upstream getCollisionBoundingBoxFromPool :81-83)
	// ---------------------------------------------------------------------------

	/** The 10x14x10 px thick-walled cross — the collision AND selection pool (:81-83, PX_P[3]..PX_N[3]x0..PX_N[2]). */
	@Test
	void theShapeIsTheTenByFourteenByTenCross() {
		VoxelShape tShape = GT6JugBlock.SHAPE;
		assertEquals(3.0 / 16.0, tShape.min(Direction.Axis.X), 1e-9, "the 3px x inset (:81 PX_P[3])");
		assertEquals(13.0 / 16.0, tShape.max(Direction.Axis.X), 1e-9, "the 13px x out (:81 PX_N[3])");
		assertEquals(0.0, tShape.min(Direction.Axis.Y), 1e-9, "floor sits on the ground");
		assertEquals(14.0 / 16.0, tShape.max(Direction.Axis.Y), 1e-9, "the 14px height (:81 PX_N[2])");
		assertEquals(3.0 / 16.0, tShape.min(Direction.Axis.Z), 1e-9, "the 3px z inset");
		assertEquals(13.0 / 16.0, tShape.max(Direction.Axis.Z), 1e-9, "the 13px z out");
	}
}
