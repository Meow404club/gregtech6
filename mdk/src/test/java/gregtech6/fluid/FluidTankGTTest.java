package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Acceptance 3 (task p4-fluid-pipes): FluidTankGT long-amount NBT round trip verified
 * offline (CompoundTag + FluidStack.loadFluidStackFromNBT need no registry beyond the
 * GTOfflineTestBase boot; the water/lava FluidStacks resolve against the vanilla fluid
 * registry that bootStrap populates).
 */
public class FluidTankGTTest extends GTOfflineTestBase {

	/** Past Integer.MAX_VALUE — the "LAmount" overflow key is the whole point. */
	static final long OVERFLOW_AMOUNT = 3_000_000_000L;

	@BeforeAll
	static void sanity() {
		// the offline boot must have the vanilla fluids resolvable for the FluidStack ctor
		assertNotEquals(null, Fluids.WATER);
	}

	@Test
	public void intRangeRoundTripUsesThePlainFluidAmount() {
		FluidTankGT tTank = new FluidTankGT(1000).setIndex(0);
		tTank.fill(new FluidStack(Fluids.WATER, 700), FluidAction.EXECUTE);

		CompoundTag tSaved = tTank.writeToNBT(new CompoundTag(), "tank");
		assertTrue(tSaved.contains("tank", Tag.TAG_COMPOUND));
		assertFalse(tSaved.getCompound("tank").contains("LAmount"), "no overflow key below the int range");

		FluidTankGT tBack = new FluidTankGT(1000).readFromNBT(tSaved, "tank");
		assertEquals(700, tBack.amount());
		assertTrue(tBack.contains(new FluidStack(Fluids.WATER, 1)));
	}

	@Test
	public void longOverflowRoundTripCarriesTheLAmountKey() {
		// fill beyond int range through the long primitives (add takes long, :158-168)
		FluidTankGT tTank = new FluidTankGT(Long.MAX_VALUE);
		assertTrue(tTank.add(OVERFLOW_AMOUNT, new FluidStack(Fluids.LAVA, bindable(OVERFLOW_AMOUNT))) > 0);
		assertEquals(OVERFLOW_AMOUNT, tTank.amount());

		CompoundTag tSaved = tTank.writeToNBT(new CompoundTag(), "tank");
		CompoundTag tInner = tSaved.getCompound("tank");
		// upstream :73-75: Amount holds the int-bound value, the exact 63-bit value rides in "LAmount"
		assertEquals(Integer.MAX_VALUE, tInner.getInt("Amount"));
		assertEquals(OVERFLOW_AMOUNT, tInner.getLong("LAmount"));

		// upstream :56/:64: the overflow key wins on read back
		FluidTankGT tBack = new FluidTankGT(Long.MAX_VALUE).readFromNBT(tSaved, "tank");
		assertEquals(OVERFLOW_AMOUNT, tBack.amount());
		assertEquals(Integer.MAX_VALUE, tBack.getFluidAmount(), "int surface stays clamped");
	}

	@Test
	public void emptyTankDropsItsNbtKeyAndRoundTripsEmpty() {
		FluidTankGT tTank = new FluidTankGT(500);
		CompoundTag tSaved = tTank.writeToNBT(new CompoundTag(), "tank");
		assertFalse(tSaved.contains("tank"), "upstream :77 removes the key for empty tanks");

		// a full round trip of a small content too
		tTank.fill(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE);
		FluidTankGT tBack = new FluidTankGT(500).readFromNBT(tTank.writeToNBT(new CompoundTag(), "tank"), "tank");
		assertEquals(250, tBack.amount());
	}

	@Test
	public void simulateDoesNotMutate() {
		FluidTankGT tTank = new FluidTankGT(1000);
		tTank.fill(new FluidStack(Fluids.WATER, 300), FluidAction.EXECUTE);

		assertEquals(400, tTank.fill(new FluidStack(Fluids.WATER, 400), FluidAction.SIMULATE), "upstream :200 simulate path");
		assertEquals(300, tTank.amount(), "simulate left the amount alone");
		assertEquals(400, tTank.fill(new FluidStack(Fluids.WATER, 400), FluidAction.SIMULATE), "simulate is repeatable");

		FluidStack tDrained = tTank.drain(200, FluidAction.SIMULATE);
		assertEquals(200, tDrained.getAmount());
		assertEquals(300, tTank.amount(), "drain simulate left the amount alone");

		assertNull(new FluidTankGT(100).get(), "empty tank reports null fluid like upstream :359");

		assertEquals(0, tTank.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.SIMULATE), "upstream :200 — different fluid simulates as zero");
	}

	@Test
	public void singleFluidRuleAndVoidExcess() {
		FluidTankGT tTank = new FluidTankGT(1000);
		assertEquals(300, tTank.fill(new FluidStack(Fluids.WATER, 300), FluidAction.EXECUTE));
		assertEquals(0, tTank.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE), "upstream :192 — different fluid is a no-op");
		assertEquals(300, tTank.amount());

		// without mVoidExcess the fill clamps to the free space
		assertEquals(700, tTank.fill(new FluidStack(Fluids.WATER, 900), FluidAction.EXECUTE));
		assertEquals(1000, tTank.amount());

		// with mVoidExcess the reported fill is the offered amount (upstream :198)
		FluidTankGT tVoiding = new FluidTankGT(100).setVoidExcess(true);
		assertEquals(500, tVoiding.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE));
		assertEquals(100, tVoiding.amount());
	}

	@Test
	public void preventDrainingKeepsTheFluidIdentity() {
		FluidTankGT tTank = new FluidTankGT(1000).setPreventDraining(true);
		tTank.fill(new FluidStack(Fluids.WATER, 300), FluidAction.EXECUTE);

		assertEquals(300, tTank.remove(300));
		assertEquals(0, tTank.amount());
		assertFalse(tTank.isEmpty(), "upstream :121-122 — mPreventDraining keeps the fluid slot occupied");
		assertEquals(0, tTank.remove(300), "upstream :145 — an empty amount short-circuits remove; the identity is what survives");

		FluidTankGT tPlain = new FluidTankGT(1000);
		tPlain.fill(new FluidStack(Fluids.WATER, 300), FluidAction.EXECUTE);
		tPlain.remove(300);
		assertTrue(tPlain.isEmpty(), "without the flag the tank empties (upstream :124)");
	}

	@Test
	public void longAddRemovePrimitivesBalance() {
		FluidTankGT tFrom = new FluidTankGT(Long.MAX_VALUE);
		FluidTankGT tTo = new FluidTankGT(500);
		tFrom.add(1000, new FluidStack(Fluids.WATER, 300));

		long tMoved = tFrom.remove(tTo.add(tFrom.amount(200), tFrom.get()));
		assertEquals(200, tMoved);
		assertEquals(800, tFrom.amount());
		assertEquals(200, tTo.amount());

		// capacity clamps through the long path, void only under the flag
		assertEquals(300, tTo.add(300, tFrom.get()));
		assertEquals(500, tTo.amount());
		assertEquals(0, tTo.add(1, new FluidStack(Fluids.LAVA, 1)));
	}

	static int bindable(long aAmount) {
		return FluidTankGT.bindInt(aAmount);
	}
}
