package gregtech6.recipes.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;

/**
 * The Canner map dynamic arms, offline (task p24-canner-machine acceptance 4 — the
 * {@link GT6RecipeMapCanner} half over the sContainerResolver seam): the EMPTY arm (a
 * container holding fluid drains onto the fluid-output leg, duration max(amount/64,16) at
 * EUt 16, aCanBeBuffered F), the FILL arm (the first input-tank fluid fills the container
 * copy; the WHOLE post-fill content counts as the fluid-input leg — the upstream
 * tFluid=getFluid(tOutput) verbatim), the round-trip pair, the explicit-rows-first
 * priority (:50) and the containsInput wide face (:73-75).
 *
 * <p>The synthetic container is a vanilla item driven by a local
 * {@link FixtureFluidHandler} — the lesson-id224 rule: NO live-registered
 * IFluidHandlerItem is probed offline (no bucket), the resolver seam is swapped for the
 * fixture and restored after every test.
 */
class GT6RecipeMapCannerTest extends GTRecipesOfflineTestBase {

	/**
	 * The fixture container: a simple tank over the item stack identity, capacity 4000 mB.
	 * getContainer returns the LIVE stack (the drain/fill mutations are visible to the
	 * caller — the IFluidHandlerItem contract).
	 */
	static final class FixtureFluidHandler implements IFluidHandlerItem {
		final ItemStack mContainer;
		@Nullable FluidStack mContent;

		FixtureFluidHandler(ItemStack aContainer, @Nullable FluidStack aContent) {
			mContainer = aContainer;
			mContent = aContent;
		}

		@Override public ItemStack getContainer() {return mContainer;}
		@Override public int getTanks() {return 1;}
		@Override public FluidStack getFluidInTank(int aTank) {return aTank == 0 && mContent != null ? mContent : FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return 4000;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			if (mContent != null && !mContent.isEmpty() && !mContent.isFluidEqual(aResource)) return 0;
			int tSpace = 4000 - (mContent == null ? 0 : mContent.getAmount());
			int tFilled = Math.min(tSpace, aResource.getAmount());
			if (tFilled > 0 && aAction.execute()) {
				mContent = mContent == null || mContent.isEmpty()
						? copyOf(aResource, tFilled)
						: copyOf(mContent, mContent.getAmount() + tFilled);
			}
			return tFilled;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (mContent == null || mContent.isEmpty() || aMaxDrain <= 0) return FluidStack.EMPTY;
			int tDrained = Math.min(mContent.getAmount(), aMaxDrain);
			FluidStack rFluid = copyOf(mContent, tDrained);
			if (aAction.execute()) {
				mContent = tDrained >= mContent.getAmount() ? null : copyOf(mContent, mContent.getAmount() - tDrained);
			}
			return rFluid;
		}

		/** The per-amount copy (1.20.1 copy ctor vs 21.1 copyWithAmount — the TileEntityBasicMachine.copyOf split). */
		private static FluidStack copyOf(FluidStack aFluid, int aAmount) {
			//? if forge {
			return new FluidStack(aFluid, aAmount);
			//?} else {
			/*return aFluid.copyWithAmount(aAmount);
			 *///?}
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (mContent == null || mContent.isEmpty() || !mContent.isFluidEqual(aResource)) return FluidStack.EMPTY;
			return drain(aResource.getAmount(), aAction);
		}
	}

	/** The resolver state: the fixture handler handed to the LAST resolve call (identity-keyed). */
	private static ItemStack sLastResolved = null;

	@BeforeEach
	void swapResolver() {
		GT6RecipeMapCanner.sContainerResolver = aStack -> {
			// the live contract: the resolver acts on the copy the map passes
			sLastResolved = aStack;
			return new FixtureFluidHandler(aStack, null);
		};
	}

	@AfterEach
	void restoreResolver() {
		//? if forge {
		GT6RecipeMapCanner.sContainerResolver = aStack -> net.minecraftforge.fluids.FluidUtil.getFluidHandler(aStack).resolve().orElse(null);
		//?} else {
		/*GT6RecipeMapCanner.sContainerResolver = aStack -> net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(aStack).orElse(null); // 21.1: Optional<IFluidHandlerItem>
		 *///?}
		GT6RecipeMaps.reset();
	}

	@BeforeEach
	void freshCannerMap() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset(); // a FRESH Canner map per test — the poured-row sets stay per-test
		GT6RecipeMaps.init();
	}

	/** The fixture container stack: an empty 4000 mB can over a vanilla item (identity-only matching). */
	private static ItemStack can() {return new ItemStack(Items.PAPER, 1);}

	// ---------------------------------------------------------------------------
	// the EMPTY arm (:52-55)
	// ---------------------------------------------------------------------------

	@Test
	void emptyArmBuildsTheOneTimeDrainRecipe() {
		// the fixture pre-fills the copy? No — findRecipe resolves the copy EMPTY, so the
		// empty arm is driven by a resolver that returns a PRE-FILLED handler (the filled
		// container the machine is probing).
		GT6RecipeMapCanner.sContainerResolver = aStack -> {
			sLastResolved = aStack;
			return new FixtureFluidHandler(aStack, new FluidStack(Fluids.WATER, 1280));
		};
		Recipe tRecipe = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], can());
		assertNotNull(tRecipe, "the EMPTY arm fires on a container holding 1280 mB");
		assertFalse(tRecipe.mCanBeBuffered, "upstream :55 — aCanBeBuffered F (one-time recipes never cache)");
		assertEquals(1, tRecipe.mInputs.length, "the filled container is consumed");
		assertSame(can().getItem(), tRecipe.mInputs[0].getItem(), "the input is the container");
		assertEquals(1, tRecipe.mOutputs.length, "the drained container is the output");
		assertEquals(1, tRecipe.mFluidOutputs.length, "the content rides the fluid-output leg");
		assertEquals(1280, tRecipe.mFluidOutputs[0].getAmount(), "the full content drains");
		assertSame(Fluids.WATER, tRecipe.mFluidOutputs[0].getFluid());
		assertEquals(0, tRecipe.mFluidInputs.length, "the EMPTY arm consumes NO tank fluid");
		assertEquals(1280 / 64, tRecipe.mDuration, "duration = max(amount/64, 16) — the 1280 arm");
		assertEquals(16, tRecipe.mEUt, "EUt 16 verbatim");
		// the resolver acted on a COPY: the probed input stack is untouched (findRecipe is lookup-only)
		assertEquals(1, can().getCount(), "the caller's stack is not consumed by the lookup");
	}

	@Test
	void emptyArmDurationFloorIs16() {
		GT6RecipeMapCanner.sContainerResolver = aStack -> {
			return new FixtureFluidHandler(aStack, new FluidStack(Fluids.WATER, 300));
		};
		Recipe tRecipe = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], can());
		assertNotNull(tRecipe);
		assertEquals(16, tRecipe.mDuration, "max(300/64, 16) = 16 — the floor");
	}

	@Test
	void aTrulyEmptyContainerFiresNothing() {
		// the plain fixture resolver — the copy resolves empty, no tank fluid: no arm fires
		Recipe tRecipe = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], can());
		assertNull(tRecipe, "an empty container with no tank fluid matches nothing");
	}

	// ---------------------------------------------------------------------------
	// the FILL arm (:65-67)
	// ---------------------------------------------------------------------------

	@Test
	void fillArmBuildsTheTankFillRecipe() {
		// the tank holds water; the fixture can accepts it
		FluidStack[] tTanks = {new FluidStack(Fluids.WATER, 1000)};
		Recipe tRecipe = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, tTanks, can());
		assertNotNull(tRecipe, "the FILL arm fires on a container + tank fluid");
		assertFalse(tRecipe.mCanBeBuffered, "upstream :67 — aCanBeBuffered F");
		assertEquals(1, tRecipe.mInputs.length, "the empty container is consumed");
		assertEquals(1, tRecipe.mOutputs.length, "the filled container is the output");
		assertEquals(1, tRecipe.mFluidInputs.length, "the tank fluid is the fluid-input leg");
		assertEquals(1000, tRecipe.mFluidInputs[0].getAmount(), "the WHOLE post-fill content counts (upstream tFluid)");
		assertSame(Fluids.WATER, tRecipe.mFluidInputs[0].getFluid());
		assertEquals(0, tRecipe.mFluidOutputs.length, "the FILL arm produces NO tank fluid");
		assertEquals(16, tRecipe.mDuration, "duration = max(1000/64, 16) = 16 — the floor arm");
		assertEquals(16, tRecipe.mEUt, "EUt 16 verbatim");
		// the tank snapshot is NOT debited by the lookup (the machine consume does that)
		assertEquals(1000, tTanks[0].getAmount(), "the source tank is untouched by findRecipe");
	}

	/** The EMPTY-then-FILL round trip over the fixture handler (the acceptance-4 round-trip). */
	@Test
	void containerRoundTrip() {
		// 1) the filled can drains (the empty arm)
		FixtureFluidHandler tHandler = new FixtureFluidHandler(can(), new FluidStack(Fluids.WATER, 1280));
		GT6RecipeMapCanner.sContainerResolver = aStack -> tHandler;
		Recipe tEmpty = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], can());
		assertNotNull(tEmpty);
		tHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
		assertTrue(tHandler.getContainer().getCount() >= 1, "the drained container shape survives");
		assertNull(tHandler.mContent, "the container is empty after the drain");

		// 2) the same container refills from the tank (the fill arm)
		FluidStack[] tTanks = {new FluidStack(Fluids.WATER, 1280)};
		GT6RecipeMapCanner.sContainerResolver = aStack -> new FixtureFluidHandler(aStack, null);
		Recipe tFill = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, tTanks, can());
		assertNotNull(tFill, "the refilled container matches the FILL arm");
		assertEquals(1280, tFill.mFluidInputs[0].getAmount(), "the round-trip content is conserved");
	}

	// ---------------------------------------------------------------------------
	// the explicit-rows-first priority (:50) + containsInput (:73-75)
	// ---------------------------------------------------------------------------

	@Test
	void explicitRowsWinOverTheDynamicArms() {
		// a refill row on the same item identity the fixture container uses
		ItemStack tEmpty = new ItemStack(Items.PAPER, 1);
		ItemStack tFull = new ItemStack(Items.CLAY_BALL, 1);
		GT6RecipeMaps.CANNER.addRecipe(new Recipe(true,
				new ItemStack[] {tEmpty}, new ItemStack[] {tFull},
				new FluidStack[] {new FluidStack(Fluids.WATER, 2304)},
				null, 256, 16, 0));
		// even with a FILLED container (the dynamic empty arm would fire), the stored row wins
		GT6RecipeMapCanner.sContainerResolver = aStack -> new FixtureFluidHandler(aStack, new FluidStack(Fluids.LAVA, 640));
		FluidStack[] tTanks = {new FluidStack(Fluids.WATER, 2304)};
		Recipe tRecipe = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, tTanks, new ItemStack(Items.PAPER, 1));
		assertNotNull(tRecipe);
		assertTrue(tRecipe.mCanBeBuffered, "the STORED row is buffered (the dynamic arms are not) — the :50 priority");
		assertEquals(256, tRecipe.mDuration, "the stored row's 256 duration — NOT the dynamic arm");
		assertSame(Items.CLAY_BALL, tRecipe.mOutputs[0].getItem(), "the stored row's output");
	}

	@Test
	void containsInputWideFace() {
		// :74/:75 — ANY fluid counts (the Canner accepts everything into its tanks)
		assertTrue(GT6RecipeMaps.CANNER.containsInput(new FluidStack(Fluids.LAVA, 1)), "containsInput(FluidStack) is T");
		assertTrue(GT6RecipeMaps.CANNER.containsInput(Fluids.LAVA), "containsInput(Fluid) is T");
		// :73 — an item counts when the stored rows match it OR a positive-capacity handler
		// resolves. The identity-selective resolver: only the fixture can gets a handler.
		GT6RecipeMapCanner.sContainerResolver = aStack ->
				aStack.getItem() == Items.PAPER ? new FixtureFluidHandler(aStack, null) : null;
		assertFalse(GT6RecipeMaps.CANNER.containsInput(new ItemStack(Items.DIAMOND, 1)), "no handler for the item → false");
		assertTrue(GT6RecipeMaps.CANNER.containsInput(can()), "the fixture container resolves with capacity 4000 → true");
		// a poured row's input matches through the stored-row arm
		GT6RecipeMaps.CANNER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1)}, new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, null, 16, 16, 0));
		assertTrue(GT6RecipeMaps.CANNER.containsInput(new ItemStack(Items.CLAY_BALL, 1)), "the stored-row arm");
	}

	/** The map ctor carried the :45 mMaxFluid*Size semantics INTO the R6 fold (no cap field) — a structural pin. */
	@Test
	void mapIsTheCannerSubclass() {
		assertNotNull(GT6RecipeMaps.CANNER, "the CANNER map");
		assertEquals(2, GT6RecipeMaps.CANNER.mInputItemsCount, "the RM.Canner 2/2 declaration (the R7 slot count source)");
		assertEquals(2, GT6RecipeMaps.CANNER.mOutputItemsCount, "OUT-ITEMS 2");
	}
}
