
package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * The kitchen BE offline tests (task p26-kitchen-pot-bowl) — the tank-array face of the
 * acceptance's "图形态断言（槽容按 mInputFluidCount/mOutputFluidCount）": the POT array is
 * sized by the BATH map (1 input / 3 output tanks), the BOWL by the MIXER map (6 / 2 —
 * GT6RecipeMaps.java init rows, RM.java:74/:80). Plus the fluid doors offline (the fill
 * admission's hot/heavier half, the TapFillable face) and the ACTIVATION CHAIN processed
 * round driven end-to-end offline (isServerSide() is true with no level —
 * TileEntityBase01Root.java:188 — so the top-face manual round runs against an injected
 * fixture row; the LIVE server leg is the kitchen_pot.py RCON chain).
 */
class GT6KitchenBlockEntityTest extends gregtech6.tileentity.GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);

	static BlockEntityType<GT6BathingPotBlockEntity> sPotType;
	static BlockEntityType<GT6MixingBowlBlockEntity> sBowlType;

	@BeforeEach
	void armTheMaps() {
		// the map generation the tank arrays read (the Mixer test convention: init/reset/init)
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
	}

	@AfterEach
	void dropTheMaps() {
		GT6RecipeMaps.reset();
	}

	@BeforeAll
	static void buildOfflineFixtures() {
		// offline holders avoid the RegistryObject.get() path of the runtime factories
		// (the TestMachineBlockEntityNBTTest holder-array shape); the base class reopened
		// the BE-type registry write window for exactly this
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6BathingPotBlockEntity>[] tPotHolder = (BlockEntityType<GT6BathingPotBlockEntity>[]) new BlockEntityType<?>[1];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6MixingBowlBlockEntity>[] tBowlHolder = (BlockEntityType<GT6MixingBowlBlockEntity>[]) new BlockEntityType<?>[1];
		tPotHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BathingPotBlockEntity(tPotHolder[0], aPos, aState), Blocks.STONE).build(null);
		tBowlHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MixingBowlBlockEntity(tBowlHolder[0], aPos, aState), Blocks.STONE).build(null);
		sPotType = tPotHolder[0];
		sBowlType = tBowlHolder[0];
	}

	private static GT6BathingPotBlockEntity pot() {
		return new GT6BathingPotBlockEntity(sPotType, POS, Blocks.STONE.defaultBlockState());
	}

	private static GT6MixingBowlBlockEntity bowl() {
		return new GT6MixingBowlBlockEntity(sBowlType, POS, Blocks.STONE.defaultBlockState());
	}

	/** The pot tank banks ride the BATH map: 1 input / 3 output (RM.java:80 fluids 1/3/1). */
	@Test
	void potTankArraysFollowTheBathMap() {
		GT6BathingPotBlockEntity tPot = pot();
		assertNotNull(tPot.tankInput(0), "BATH mInputFluidCount = 1 — the input tank exists");
		assertNull(tPot.tankInput(1), "…and only one (the array is map-sized, not a fixed 6)");
		for (int i = 0; i < 3; i++) assertNotNull(tPot.tankOutput(i), "BATH mOutputFluidCount = 3 — output tank " + i);
		assertNull(tPot.tankOutput(3), "…and only three");
		assertEquals(GT6RecipeMaps.BATH, tPot.recipeMap(), "the pot processes RM.Bath (upstream :64)");
	}

	/** The bowl tank banks ride the MIXER map: 6 input / 2 output (RM.java:74 fluids 6/2/0). */
	@Test
	void bowlTankArraysFollowTheMixerMap() {
		GT6MixingBowlBlockEntity tBowl = bowl();
		for (int i = 0; i < 6; i++) assertNotNull(tBowl.tankInput(i), "MIXER mInputFluidCount = 6 — input tank " + i);
		assertNull(tBowl.tankInput(6), "…and only six");
		assertNotNull(tBowl.tankOutput(0), "MIXER mOutputFluidCount = 2");
		assertNotNull(tBowl.tankOutput(1), "MIXER mOutputFluidCount = 2");
		assertNull(tBowl.tankOutput(2), "…and only two");
		assertEquals(GT6RecipeMaps.MIXER, tBowl.recipeMap(), "the bowl processes RM.Mixer (upstream :64)");
	}

	/** The fixture carrier is a vanilla block — the 1000 L / MT.Wood offline defaults (the ctor's non-kitchen arm). */
	@Test
	void theFixtureDefaultsAreTheUpstreamFallback() {
		GT6BathingPotBlockEntity tPot = pot();
		FluidTankGT tTank = tPot.tankInput(0);
		assertNotNull(tTank);
		FluidStack tWater = new FluidStack(Fluids.WATER, 1001);
		assertTrue(tTank.fill(tWater, IFluidHandler.FluidAction.SIMULATE) <= 1000, "the fixture cap is the ctor's 1000 L fallback (upstream :73)");
	}

	/**
	 * The :302 admission's containing-tank short circuit (:250 — {@code contains} wins
	 * BEFORE the doors). The full door half (the -100 K hot door, the density door) reads
	 * the FluidType carrier — a Forge mod-bus registration the offline JVM cannot bind —
	 * so the door faces ride the LIVE server: the kitchen_pot.py RCON chain injects
	 * water through the capability face (admission passed live) and the door predicates
	 * are the verbatim port of :302-307.
	 */
	@Test
	void theAdmissionShortCircuitsOnTheContainingTank() {
		GT6BathingPotBlockEntity tPot = pot();
		tPot.tankInput(0).fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE);
		FluidTankGT tFillable = tPot.getFluidTankFillable(new FluidStack(Fluids.WATER, 1000));
		assertNotNull(tFillable, "the containing input tank wins the admission (:250)");
		assertEquals(tPot.tankInput(0), tFillable, "…that exact tank, not a fresh empty one");
	}

	/** The P12 TapFillable face (the WSL-crash takeover commit) — simulate probes, execute commits. */
	@Test
	void tapFillRidesTheFillAdmission() {
		GT6BathingPotBlockEntity tPot = pot();
		tPot.tankInput(0).fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE);
		FluidStack tWater = new FluidStack(Fluids.WATER, 400);
		int tProbed = tPot.tapFill((byte) 2, tWater, false);
		assertEquals(400, tProbed, "the simulate phase accepts the full offer (800 fits the 1000 L fixture tank)");
		assertEquals(200, tPot.tankInput(0).getFluidAmount(), "…without committing");
		assertEquals(400, tPot.tapFill((byte) 2, tWater, true), "the execute phase commits");
		assertEquals(600, tPot.tankInput(0).getFluidAmount(), "the water landed in the containing tank");
	}

	/**
	 * The activation chain's manual round, end to end OFFLINE: a fixture row in the MIXER
	 * map, brick + water in → the row pays, the fluid output lands in the output tank, the
	 * report reads "processed" (the RCON acceptance channel the player path ignores).
	 */
	@Test
	void theActivationChainProcessesARowOffline() {
		Recipe tRow = new Recipe(true,
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)},
				16, 0, 0);
		RecipeMap tMixer = GT6RecipeMaps.MIXER;
		assertNotNull(tMixer, "the MIXER map lives (the p26-c-foam declaration)");
		tMixer.mRecipeList.add(tRow);
		try {
			GT6MixingBowlBlockEntity tBowl = bowl();
			tBowl.inventory().setStackInSlot(0, new ItemStack(Items.BRICK, 4));
			assertNotNull(tBowl.tankInput(0).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) > 0 ? tBowl : null,
					"the water pre-fill lands");

			String tReport = tBowl.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.2F);

			assertTrue(tReport.startsWith("processed"), "the manual round fires: " + tReport);
			assertEquals(3, tBowl.inventory().getStackInSlot(0).getCount(), "the brick input paid one");
			assertTrue(tBowl.tankOutput(0).contains(new FluidStack(Fluids.WATER, 1)), "the fluid output landed in the output tank");
		} finally {
			tMixer.mRecipeList.remove(tRow);
		}
	}

	/** The :190/:210 NEI corner is the declared no-op (no NEI in the port) — the report arm. */
	@Test
	void theNeiCornerIsADeclaredNoOp() {
		GT6MixingBowlBlockEntity tBowl = bowl();
		String tReport = tBowl.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.1F, 0.0F, 0.1F);
		assertTrue(tReport.contains("NEI corner"), "the corner quadrant reports the no-op: " + tReport);
	}

	/** With no matching row and nothing else to do, the top face reports "no action". */
	@Test
	void anEmptyTopFaceReportsNoAction() {
		GT6MixingBowlBlockEntity tBowl = bowl();
		String tReport = tBowl.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.2F);
		assertEquals("no action", tReport, "no row, no held stack, no player — the idle verdict");
		assertTrue(tBowl.inventory().getStackInSlot(0).isEmpty(), "nothing changed");
	}
}
