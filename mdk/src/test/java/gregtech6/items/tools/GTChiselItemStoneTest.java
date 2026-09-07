package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesStoneChisel;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;

/**
 * The GTChiselItem universal gate truth table (task p19-chisel-recipes acceptance):
 * the ToolCompat.java:224-229 transcription over the poured CHISEL book.
 *
 * <p><b>Offline drive shape</b> (the CutterTest convention): a map-driven Level double
 * stands in for the world ({@code setBlock} records, {@code getBlockState} replays),
 * a {@code UseOnContext} double rides the protected five-arg constructor, and the pour
 * resolves the GT stone rows onto synthetic items — the two vanilla rows (:772-773) are
 * the REAL conversion arms here, the GT stone variant rows are pinned in
 * GT6RecipesStoneChiselTest (row shapes + exact-tag lookups) and driven LIVE against the
 * registered blocks by the RCON chain (offline blocks have no BlockItem, so the
 * stackFromState leg cannot resolve them — a declared offline wall, not a cut).
 *
 * <p>Three acceptance arms: a recipe block converts and returns the upstream 10000 (with
 * the 25-point conversion pinned; the null-player acceptance channel pays NOTHING — the
 * real payment is the RCON fake-player arm), a sneaking player PASSes (the :224
 * {@code !aSneaking} gate, through a minimal Player probe), a non-recipe block PASSes.
 */
class GTChiselItemStoneTest extends GTRecipesOfflineTestBase {

	private static final BlockPos POS = new BlockPos(1, 64, 1);

	/** The level double: a map-backed blockstate store, no block entities (the gate arm runs). */
	public static final class StoneLevel extends GTMachinesOfflineTestBase.MachineLevel {
		private final Map<BlockPos, BlockState> mStates = new HashMap<>();

		StoneLevel(BlockPos aPos, BlockState aState) {
			super(new TestRecipeManager());
			mStates.put(aPos, aState);
		}

		@Override
		public BlockState getBlockState(BlockPos aPos) {
			return mStates.getOrDefault(aPos, Blocks.AIR.defaultBlockState());
		}

		@Override
		public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags) {
			mStates.put(aPos, aState);
			return true;
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return null; // no boiler tank here — the universal gate arm answers
		}
	}

	/** The context double — the protected five-argument UseOnContext constructor (the CutterTest shape). */
	public static final class TestContext extends UseOnContext {
		public TestContext(Level aLevel, Player aPlayer, InteractionHand aHand, ItemStack aStack, BlockHitResult aHit) {
			super(aLevel, aPlayer, aHand, aStack, aHit);
		}
	}

	/**
	 * The minimal player probe — REMOVED: a live Player instance is not constructible
	 * offline (the Forge fluid-type lazy registry dies inside the Entity ctor); the sneak
	 * arm rides the injected-bit overload and the RCON fake player.
	 */

	/**
	 * Reopens the write window of the BLOCK registry so the probe GTStoneBlock of the
	 * variant-plane test stays constructible after Bootstrap froze it (the
	 * GTWireConnectBranchTest.java:112 reflection form; intrusive-holder writes hit the
	 * frozen registry otherwise — order-dependence is not relied on).
	 */
	@BeforeAll
	static void reopenBlockRegistry() {
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Throwable ignored) {
			// the probe construction then depends on the suite's earlier unfreeze — never mask an assertion
		}
	}

	@BeforeEach
	void pourBookAndResetCounter() {
		GT6RecipesStoneChisel.sStoneItemResolver = aSnake -> Items.BRICKS; // synthetic stand-in for every GT stone item
		GT6RecipesStoneChisel.load();
		GTChiselItem.sPayPerPointCalls = 0;
	}

	@AfterEach
	void resetGeneration() {
		GT6RecipesStoneChisel.sStoneItemResolver = aSnake -> Items.BRICKS;
		GT6RecipeMaps.reset();
		GT6RecipesStoneChisel.resetForTest();
	}

	private static UseOnContext context(Level aLevel, Player aPlayer, ItemStack aStack) {
		return new TestContext(aLevel, aPlayer, InteractionHand.MAIN_HAND, aStack,
				new BlockHitResult(Vec3.atCenterOf(POS), net.minecraft.core.Direction.UP, POS, false));
	}

	// ------------------------------------------------------------------
	// the blockInBlockOut pure predicate (upstream Recipe.java:719-721)
	// ------------------------------------------------------------------

	/** The Recipe.java:719-721 transcription: one item leg each way, no fluids, single counts, both block-forms. */
	@Test
	void blockInBlockOutTruthTable() {
		Recipe tBlockRow = new Recipe(true,
				new ItemStack[] {new ItemStack(Blocks.STONE.asItem(), 1)},
				new ItemStack[] {new ItemStack(Blocks.CHISELED_STONE_BRICKS.asItem(), 1)},
				new net.minecraftforge.fluids.FluidStack[0], new net.minecraftforge.fluids.FluidStack[0], 16, 16, 0);
		assertTrue(GTChiselItem.blockInBlockOut(tBlockRow), "the :772 shape qualifies");

		Recipe tTwoOutputs = new Recipe(true,
				new ItemStack[] {new ItemStack(Blocks.STONE.asItem(), 1)},
				new ItemStack[] {new ItemStack(Blocks.CHISELED_STONE_BRICKS.asItem(), 1), new ItemStack(Blocks.STONE.asItem(), 1)},
				new net.minecraftforge.fluids.FluidStack[0], new net.minecraftforge.fluids.FluidStack[0], 16, 16, 0);
		assertFalse(GTChiselItem.blockInBlockOut(tTwoOutputs), "two outputs disqualify");

		Recipe tFluidRow = new Recipe(true,
				new ItemStack[] {new ItemStack(Blocks.STONE.asItem(), 1)},
				new ItemStack[] {new ItemStack(Blocks.CHISELED_STONE_BRICKS.asItem(), 1)},
				new net.minecraftforge.fluids.FluidStack[0],
				new net.minecraftforge.fluids.FluidStack[]{new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)},
				16, 16, 0);
		assertFalse(GTChiselItem.blockInBlockOut(tFluidRow), "a fluid output disqualifies");

		Recipe tCountedOutput = new Recipe(true,
				new ItemStack[] {new ItemStack(Blocks.STONE.asItem(), 1)},
				new ItemStack[] {new ItemStack(Blocks.STONE.asItem(), 4)},
				new net.minecraftforge.fluids.FluidStack[0], new net.minecraftforge.fluids.FluidStack[0], 16, 16, 0);
		assertFalse(GTChiselItem.blockInBlockOut(tCountedOutput), "a non-single output disqualifies");

		Recipe tNonBlockOutput = new Recipe(true,
				new ItemStack[] {new ItemStack(Blocks.STONE.asItem(), 1)},
				new ItemStack[] {new ItemStack(Items.IRON_INGOT, 1)},
				new net.minecraftforge.fluids.FluidStack[0], new net.minecraftforge.fluids.FluidStack[0], 16, 16, 0);
		assertFalse(GTChiselItem.blockInBlockOut(tNonBlockOutput), "a non-block output disqualifies");
	}

	// ------------------------------------------------------------------
	// the three acceptance arms
	// ------------------------------------------------------------------

	/** Arm 1: a recipe block converts and returns 10000 — the vanilla :772 arm STONE → CHISELED_STONE_BRICKS. */
	@Test
	void gateConvertsARecipeBlockAndReturns10000() {
		StoneLevel tLevel = new StoneLevel(POS, Blocks.STONE.defaultBlockState());
		ItemStack tChisel = new ItemStack(Items.IRON_INGOT); // the payment carrier (the null-player channel pays nothing)
		long tDamage = GTChiselItem.stoneToolClick(context(tLevel, null, tChisel));
		assertEquals(10000, tDamage, "the :228 return 10000");
		assertEquals(Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), tLevel.getBlockState(POS),
				"the recipe output block is written back (the WD.set half)");
		assertEquals(1, GTChiselItem.sPayPerPointCalls, "the conversion arms the payment exactly once");
		assertEquals(0, tChisel.getDamageValue(), "the null-player acceptance channel pays nothing (the cutter/crowbar ruling)");
	}

	/** Arm 1b: the durability conversion — the Behavior_Tool :63 units(10000, 10000, 25, T) = 25 points. */
	@Test
	void the10000ReturnConvertsTo25Points() {
		assertEquals(25, GTChiselItem.durabilityPoints(GTChiselItem.TOOL_DAMAGE_UNIT), "GT_Tool_Chisel.java:98 mDamage=25");
		assertEquals(1, GTChiselItem.durabilityPoints(1), "round-up: any non-zero repair pays at least one point");
		assertEquals(0, GTChiselItem.durabilityPoints(0), "a zero return pays nothing");
	}

	/**
	 * Arm 2: a sneaking click PASSes (the :224 !aSneaking gate) — no conversion, no payment.
	 * The gate core takes the decoded sneak bit (the production overload reads the player's
	 * bit exactly once and lands here); a live Player instance is not constructible offline
	 * (the Forge fluid-type lazy registry dies inside the Entity ctor) — the live sneak arm
	 * is the RCON fake player's setShiftKeyDown.
	 */
	@Test
	void sneakingClickPasses() {
		StoneLevel tLevel = new StoneLevel(POS, Blocks.STONE.defaultBlockState());
		long tDamage = GTChiselItem.stoneToolClick(context(tLevel, null, new ItemStack(Items.IRON_INGOT)), true);
		assertEquals(0, tDamage, "sneaking declines the gate");
		assertEquals(Blocks.STONE.defaultBlockState(), tLevel.getBlockState(POS), "no conversion");
		assertEquals(0, GTChiselItem.sPayPerPointCalls, "no payment");

		long tUnsneaking = GTChiselItem.stoneToolClick(context(tLevel, null, new ItemStack(Items.IRON_INGOT)), false);
		assertEquals(10000, tUnsneaking, "the same probe unsneaking converts (the gate is the sneak bit alone)");
	}

	/** Arm 3: a non-recipe block PASSes — minecraft:diorite has no row in the book. */
	@Test
	void nonRecipeBlockPasses() {
		StoneLevel tLevel = new StoneLevel(POS, Blocks.DIORITE.defaultBlockState());
		long tDamage = GTChiselItem.stoneToolClick(context(tLevel, null, new ItemStack(Items.IRON_INGOT)));
		assertEquals(0, tDamage, "no row behind diorite");
		assertEquals(Blocks.DIORITE.defaultBlockState(), tLevel.getBlockState(POS), "no conversion");
		assertEquals(0, GTChiselItem.sPayPerPointCalls, "no payment");
	}

	/** The negative space: the chiseled output has no reverse row, and an air target has no item form. */
	@Test
	void chiseledOutputAndAirTargetPass() {
		StoneLevel tLevel = new StoneLevel(POS, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		assertEquals(0, GTChiselItem.stoneToolClick(context(tLevel, null, new ItemStack(Items.IRON_INGOT))),
				"the book is one-way: no chiseled → stone row exists");
		assertEquals(Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), tLevel.getBlockState(POS));

		assertEquals(0, GTChiselItem.stoneToolClick(context(new StoneLevel(POS, Blocks.AIR.defaultBlockState()), null, new ItemStack(Items.IRON_INGOT))),
				"an air target has no item form (the :226 gate)");
	}

	/** Arm 1c: the :773 arm STONE_BRICKS → CRACKED_STONE_BRICKS (the second vanilla row, live). */
	@Test
	void gateConvertsStoneBricksToCracked() {
		StoneLevel tLevel = new StoneLevel(POS, Blocks.STONE_BRICKS.defaultBlockState());
		assertEquals(10000, GTChiselItem.stoneToolClick(context(tLevel, null, new ItemStack(Items.IRON_INGOT))));
		assertEquals(Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), tLevel.getBlockState(POS));
	}

	// ------------------------------------------------------------------
	// the variant carrier: stack encoding and state decoding
	// ------------------------------------------------------------------

	/** stackFromState: a vanilla block encodes to a plain stack; the recipe output decodes back to its state. */
	@Test
	void stackAndStateCarrierRoundtripForVanilla() {
		ItemStack tStack = GTChiselItem.stackFromState(Blocks.STONE_BRICKS.defaultBlockState());
		assertNotNull(tStack);
		assertEquals(Blocks.STONE_BRICKS.asItem(), tStack.getItem());
		assertNull(GT6RecipesStoneChisel.variantOf(tStack), "a vanilla state encodes untagged");

		assertEquals(Blocks.STONE_BRICKS.defaultBlockState(), GTChiselItem.stateFromStack(tStack), "the decode roundtrips");
		assertNull(GTChiselItem.stateFromStack(new ItemStack(Items.IRON_INGOT, 1)), "a non-block stack decodes to no state");
		assertNull(GTChiselItem.stackFromState(Blocks.AIR.defaultBlockState()), "air has no item form");
	}

	/**
	 * The GT stone variant domain: the tag roundtrip pins the carrier, and the probe block
	 * pins the p21 per-pair plane — one block per (stone, variant), NO blockstate property
	 * any more (the P19 EnumProperty retired), the variant riding the block instance
	 * (stackFromState writes it into the tag; stateFromStack decodes through the BlockItem's
	 * own block — the full block path stays with the RCON live arm, the offline wall above).
	 */
	@Test
	void gtStoneVariantCarrierAndStatePlane() {
		ItemStack tStack = new ItemStack(Items.BRICKS, 1);
		GT6RecipesStoneChisel.withVariant(tStack, StoneVariant.CHISL);
		assertEquals(StoneVariant.CHISL, GT6RecipesStoneChisel.variantOf(tStack), "the pour-side tag is readable");
		assertEquals(StoneVariant.CHISL, GT6RecipesStoneChisel.variantByName("bricks_chiseled"),
				"the serialized name decodes through the same table");

		GTStoneBlock tProbe = new GTStoneBlock("granite_black", StoneVariant.CHISL, null, 1.0F, 1.0F, 0, false); // the p21 per-pair probe shape
		assertEquals(StoneVariant.CHISL, tProbe.variant, "the block carries its FIXED variant");
		assertTrue(tProbe.defaultBlockState().getProperties().isEmpty(),
				"the p21 block is a pure block — the P19 variant property is retired");
	}
}
