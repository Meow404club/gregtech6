package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * The Juicer BE offline tests (task p33-food-machines-kitchen) — the p26
 * GT6KitchenBlockEntityTest shape over the third kitchen-family member: the tank arrays
 * ride the JUICER map (0 input / 1 output fluids, RM.java:102 the 1/3/1 item 0/1/0
 * fluid shape), the recipe map is RM.Juicer, and the manual top-face round is driven
 * end-to-end offline with a fixture row (the LIVE leg is the p33_food_machines RCON
 * chain — the honey-comb row against the juicer.json universe).
 */
class GT6JuicerBlockEntityTest extends gregtech6.tileentity.GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 2, 3);

	static BlockEntityType<GT6JuicerBlockEntity> sJuicerType;

	@BeforeEach
	void armTheMaps() {
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
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6JuicerBlockEntity>[] tHolder = (BlockEntityType<GT6JuicerBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6JuicerBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sJuicerType = tHolder[0];
	}

	private static GT6JuicerBlockEntity juicer() {
		return new GT6JuicerBlockEntity(sJuicerType, POS, Blocks.STONE.defaultBlockState());
	}

	/** The tank banks ride the JUICER map: ZERO input / ONE output tank (RM.java:102, 0/1/0). */
	@Test
	void tankArraysFollowTheJuicerMap() {
		GT6JuicerBlockEntity tJuicer = juicer();
		assertNull(tJuicer.tankInput(0), "JUICER mInputFluidCount = 0 — no input tank at all");
		assertNotNull(tJuicer.tankOutput(0), "JUICER mOutputFluidCount = 1 — the honey/slime/seedoil bank");
		assertNull(tJuicer.tankOutput(1), "…and only one");
		assertEquals(GT6RecipeMaps.JUICER, tJuicer.recipeMap(), "the juicer processes RM.Juicer (upstream :65)");
	}

	/**
	 * The activation chain's manual round, end to end OFFLINE — the upstream :139-151
	 * shape: item-only input, the fluid output lands in the single output tank, the
	 * report reads "processed", the input pays. The upstream Juicer hands the item
	 * outputs straight to the player (ST.give) — the port's null-player RCON arm has
	 * them land in the output slot, the collected face is the click-economy (the base
	 * deviation note).
	 */
	@Test
	void theActivationChainJuicesARowOffline() {
		Recipe tRow = new Recipe(true,
				new ItemStack[] {new ItemStack(Items.SLIME_BALL, 1)},
				new ItemStack[0],
				new FluidStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 125)},
				64, 16, 0);
		RecipeMap tJuicerMap = GT6RecipeMaps.JUICER;
		assertNotNull(tJuicerMap, "the JUICER map lives (the b1 juicer.json declaration)");
		tJuicerMap.mRecipeList.add(tRow);
		try {
			GT6JuicerBlockEntity tJuicer = juicer();
			tJuicer.inventory().setStackInSlot(0, new ItemStack(Items.SLIME_BALL, 3));

			String tReport = tJuicer.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.2F);

			assertTrue(tReport.startsWith("processed"), "the manual round fires: " + tReport);
			assertEquals(2, tJuicer.inventory().getStackInSlot(0).getCount(), "the slimeball input paid one");
			assertTrue(tJuicer.tankOutput(0).contains(new FluidStack(Fluids.WATER, 1)), "the juice landed in the output tank");
			// the SECOND click — the upstream ST.give arm ≈ the output slot collects
			tJuicer.inventory().setStackInSlot(gregtech6.tileentity.tools.GT6ManualKitchenBlockEntity.INPUT_SLOTS, ItemStack.EMPTY);
		} finally {
			tJuicerMap.mRecipeList.remove(tRow);
		}
	}

	/** A non-matching item on the top face with no other work is the idle verdict. */
	@Test
	void aNonMatchingItemReportsNoAction() {
		GT6JuicerBlockEntity tJuicer = juicer();
		tJuicer.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 1));
		String tReport = tJuicer.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.2F);
		assertEquals("no action", tReport, "no row matches stone — the idle verdict");
		assertEquals(1, tJuicer.inventory().getStackInSlot(0).getCount(), "nothing changed");
	}
}
