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
 * Acceptance 3 (task fluid-pipes): FluidTankGT long-amount NBT round trip verified
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

	@Test
	public void unknownFluidNameDegradesToATrulyEmptyTank() {
		// The keepFilter payload shape with a bogus identity: {FluidName, Amount: 0}. The codec
		// face fails it to EMPTY and the read-side rebuild (ADR-P18) must fold an unregistered
		// name to an empty tank — exactly the 1.20.1 degradation (loadFluidStackFromNBT hands
		// back the EMPTY stack for an unknown name, the :84-86 arm folds it to null). Same
		// assertions on both legs, zero chisel. NOTE: the name must stay syntactically VALID —
		// a malformed one throws inside the frozen forge leg's own "new ResourceLocation"
		// before our guard ever runs.
		CompoundTag tGarbage = new CompoundTag();
		tGarbage.putString("FluidName", "gt6:not_a_fluid");
		tGarbage.putInt("Amount", 0);
		CompoundTag tOuter = new CompoundTag();
		tOuter.put("tank", tGarbage);

		FluidTankGT tTank = new FluidTankGT(1000).readFromNBT(tOuter, "tank");
		assertTrue(tTank.isEmpty(), "an unknown FluidName loads as a truly empty tank, never a crash");
		assertEquals(0, tTank.amount(), "the degraded tank holds 0 L");
		assertNull(tTank.getFluid(), "no identity is fabricated for an unknown name");
	}

	@Test
	public void keepFilterZeroAmountPayloadSurvivesTheLegDialect() {
		// known_bugs barrel_zerofluid_nbt_leg_dialect (a P29 tank-data-merge key-form fork
		// variant): {FluidName, Amount: 0} is what BOTH legs' writeToNBT emit for a keepFilter
		// tank, and what a forge-leg save carries. The 21.1 codec face cannot represent it
		// (no "id" key; the amount field is POSITIVE_INT — neo FluidStack.java:61-68), so the
		// dialect read arm must come from the FluidName contract keys and land the forge leg's
		// :87-94 state verbatim: a unit-amount carrier, mAmount the authoritative 0, identity
		// WATER. Same assertions on both legs, zero chisel.
		CompoundTag tKeepFilter = new CompoundTag();
		tKeepFilter.putString("FluidName", "minecraft:water");
		tKeepFilter.putInt("Amount", 0);
		CompoundTag tOuter = new CompoundTag();
		tOuter.put("tank", tKeepFilter);

		FluidTankGT tTank = new FluidTankGT(1000).readFromNBT(tOuter, "tank");
		assertFalse(tTank.isEmpty(), "the kept filter identity survives the leg dialect");
		assertEquals(0, tTank.amount(), "0 L with the identity kept is the keepFilter state");
		assertTrue(tTank.contains(new FluidStack(Fluids.WATER, 1)), "the kept identity is WATER");
	}

	@Test
	public void legacyForgeFormPayloadsRestoreTheirAmount() {
		// A full-tank forge-form payload (a world written by the 1.20.1 leg, or the same keys a
		// legacy GT6 reader names): the read must restore the amount the way the forge leg's
		// :96 arm does — identity, "Amount", and the "LAmount" overflow. Before the dialect
		// read arm the 21.1 leg's codec failure silently zeroed these. Zero chisel: the forge
		// leg answers the same payload through loadFluidStackFromNBT natively.
		CompoundTag tFull = new CompoundTag();
		tFull.putString("FluidName", "minecraft:water");
		tFull.putInt("Amount", 5000);
		CompoundTag tOuter = new CompoundTag();
		tOuter.put("tank", tFull);
		FluidTankGT tBack = new FluidTankGT(16000).readFromNBT(tOuter, "tank");
		assertEquals(5000, tBack.amount());
		assertTrue(tBack.contains(new FluidStack(Fluids.WATER, 1)));

		CompoundTag tOverflow = new CompoundTag();
		tOverflow.putString("FluidName", "minecraft:lava");
		tOverflow.putInt("Amount", Integer.MAX_VALUE);
		tOverflow.putLong("LAmount", OVERFLOW_AMOUNT);
		CompoundTag tOuterOverflow = new CompoundTag();
		tOuterOverflow.put("tank", tOverflow);
		FluidTankGT tBackOverflow = new FluidTankGT(Long.MAX_VALUE).readFromNBT(tOuterOverflow, "tank");
		assertEquals(OVERFLOW_AMOUNT, tBackOverflow.amount(), "the overflow key wins over the int-bound Amount");
		assertTrue(tBackOverflow.contains(new FluidStack(Fluids.LAVA, 1)));
	}

	@Test
	public void forgeFormZeroAmountReadEmitsNoCodecLoaderError() {
		// The "silent" half of barrel_zerofluid_nbt_leg_dialect: the 21.1 codec loader logs
		// "Tried to load invalid fluid" on EVERY failed parse (neo FluidStack.parse:190
		// resultOrPartial → LOGGER.error) — the once-per-chunk-reload warning the bug tracks.
		// Capture the root logger's ERROR events across the read: the dialect arm must not
		// reach the codec face, so no loader error may appear. On the forge leg the read never
		// touches a codec at all, so the silence assertion holds trivially.
		org.apache.logging.log4j.core.LoggerContext tContext = (org.apache.logging.log4j.core.LoggerContext)org.apache.logging.log4j.LogManager.getContext(false);
		org.apache.logging.log4j.core.Logger tRoot = tContext.getRootLogger();
		LoaderErrorCapture tCapture = new LoaderErrorCapture();
		tCapture.start();
		tRoot.addAppender(tCapture);
		try {
			CompoundTag tKeepFilter = new CompoundTag();
			tKeepFilter.putString("FluidName", "minecraft:water");
			tKeepFilter.putInt("Amount", 0);
			CompoundTag tOuter = new CompoundTag();
			tOuter.put("tank", tKeepFilter);
			assertTrue(new FluidTankGT(1000).readFromNBT(tOuter, "tank").contains(new FluidStack(Fluids.WATER, 1)));
			//? if neoforge {
			/*// Positive control: the codec face itself, fed a codec-form tag with an unknown
			//registry "id", DOES trip the loader's error line — the capture is live, and the
			//codec failure (not the key form alone) is what the warning rode on.
			CompoundTag tBrokenCodecForm = new CompoundTag();
			tBrokenCodecForm.putString("id", "gt6:not_a_fluid");
			tBrokenCodecForm.putInt("amount", 1);
			assertTrue(FluidStack.parseOptional(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, tBrokenCodecForm).isEmpty(), "the control tag must fail the codec parse");
			assertTrue(tCapture.containsLoaderError(), "the codec loader's error line must reach the root capture");
			tCapture.lines.clear();
			// and one more dialect read after the control, still silent:
			assertTrue(new FluidTankGT(1000).readFromNBT(tOuter, "tank").contains(new FluidStack(Fluids.WATER, 1)));
			*/
			//?}
			assertTrue(tCapture.lines.isEmpty(), "no ERROR line during the dialect read: " + tCapture.lines);
		} finally {
			tRoot.removeAppender(tCapture);
			tCapture.stop();
		}
	}

	/** Captures the ERROR lines reaching the root logger — the observable face of the codec loader's parse diagnostics. */
	private static final class LoaderErrorCapture extends org.apache.logging.log4j.core.appender.AbstractAppender {
		final java.util.List<String> lines = new java.util.ArrayList<>();
		LoaderErrorCapture() {
			super("gt6TankDialectCapture", null, null, true);
		}
		@Override
		public void append(org.apache.logging.log4j.core.LogEvent aEvent) {
			if (aEvent.getLevel() == org.apache.logging.log4j.Level.ERROR) lines.add(String.valueOf(aEvent.getMessage().getFormattedMessage()));
		}
		boolean containsLoaderError() {
			for (String tLine : lines) if (tLine.contains("Tried to load invalid fluid")) return true;
			return false;
		}
	}

	static int bindable(long aAmount) {
		return FluidTankGT.bindInt(aAmount);
	}
}
