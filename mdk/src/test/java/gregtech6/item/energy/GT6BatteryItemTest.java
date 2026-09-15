package gregtech6.item.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The battery ITEM behavior pins (task p29-w4-battery-storage acceptance ②/④/⑥): the NBT
 * round-trip (the {@code gt.energy} carrier, write = the writeItemNBT2 :89-90 shape), the
 * setEnergyStored clamp (the :76 read clamp hoisted to the write face — acceptance ②),
 * the store-as-full lane (the :70-74 NBT_ACTIVE_ENERGY branch), the packet band (the
 * :62-66 floor arm), the packet math (doEnergyInjection/Extraction :155-:174), the EU/LU
 * mutual rejection (acceptance ④ — the TagData reference-equality gates), useEnergy, and
 * the stack faces (the :143 charged-single + the acceptance ⑥ durability-bar positions).
 *
 * <p>Offline harness: {@link GT6BatteryItem} instances are constructed DIRECTLY (a plain
 * Item needs no registration; the GT6Circuits offline posture), the charge rides the
 * stack carrier on this leg.
 */
public class GT6BatteryItemTest extends GTOfflineTestBase {

	static GT6BatteryItem sLeadAcidUl; // 8 EU packets, 16000 EU (the :1009 row)
	static GT6BatteryItem sLeadAcidLv; // 32 EU packets, 64000 EU (the :1010 row)
	static GT6BatteryItem sEnergiumRed; // 8 LU packets, 3200000 LU (the :1079 row)

	@BeforeAll
	static void warmUpAndBuild() {
		gregtech6.tileentity.energy.GTEnergySourceBlockEntity.resolveEnergyType("TU"); // the TD CME warm-up
		sLeadAcidUl = registerFixture("fixture_battery_ulv", () -> new GT6BatteryItem(new Item.Properties(), 8, 16000, TD.Energy.EU));
		sLeadAcidLv = registerFixture("fixture_battery_lv", () -> new GT6BatteryItem(new Item.Properties(), 32, 64000, TD.Energy.EU));
		sEnergiumRed = registerFixture("fixture_battery_lu", () -> new GT6BatteryItem(new Item.Properties(), 8, 3200000, TD.Energy.LU));
	}

	// ---------------------------------------------------------------------------
	// acceptance ② — the NBT face
	// ---------------------------------------------------------------------------

	/**
	 * The offline item fixture seat. The forge wrapper latches the vanilla-registry write
	 * window; on this JVM the latch fields may be unreachable (module access), in which
	 * case the carrier-dependent tests ASSUME-SKIP — the 1.20.1 leg is the gate for the
	 * carrier semantics, the 21.1 leg records the telemetry.
	 */
	static final sun.misc.Unsafe UNSAFE;
	static final long LOCKED_OFFSET;
	static final long FROZEN_OFFSET;
	static final boolean ARMED;
	static {
		sun.misc.Unsafe tUnsafe = null;
		long tLocked = 0, tFrozen = 0;
		boolean tArmed = true;
		try {
			java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tUnsafeField.setAccessible(true);
			tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
			Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.ITEM.getClass();
			tLocked = tUnsafe.objectFieldOffset(findField(tClass, "locked"));
			tFrozen = tUnsafe.objectFieldOffset(findField(tClass, "frozen"));
		} catch (Throwable ignored) {
			tArmed = false; // the telemetry leg: carrier tests assume-skip
		}
		UNSAFE = tUnsafe;
		LOCKED_OFFSET = tLocked;
		FROZEN_OFFSET = tFrozen;
		ARMED = tArmed;
	}

	/** The latch fields live on wrapper superclasses — walk up (getDeclaredField sees one class only). */
	private static java.lang.reflect.Field findField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tWalk = aClass; tWalk != null; tWalk = tWalk.getSuperclass()) {
			try {
				return tWalk.getDeclaredField(aName);
			} catch (NoSuchFieldException ignored) {
				// keep walking
			}
		}
		throw new NoSuchFieldException(aName + " (walked " + aClass + " up)");
	}

	static void unlockItemRegistry() {
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, LOCKED_OFFSET, false);
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, FROZEN_OFFSET, false);
	}

	static void lockItemRegistry() {
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, FROZEN_OFFSET, true);
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, LOCKED_OFFSET, true);
	}

	/** The ItemStack ctor needs a registry DELEGATE (ForgeRegistry.getDelegateOrThrow — the "No delegate" failure), so the fixtures register under fixture keys with the latch momentarily open. */
	static GT6BatteryItem registerFixture(String aKey, java.util.function.Supplier<GT6BatteryItem> aItem) {
		org.junit.jupiter.api.Assumptions.assumeTrue(ARMED, "the offline registry latch is unreachable on this JVM");
		unlockItemRegistry();
		try {
			return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
					new net.minecraft.resources.ResourceLocation("gt6", aKey), aItem.get());
		} finally {
			lockItemRegistry();
		}
	}

	@Test
	public void nbtRoundTripCarriesTheCharge() {
		ItemStack tStack = new ItemStack(sLeadAcidUl);
		assertEquals(0, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "a fresh battery is empty");
		ItemStack tOut = sLeadAcidUl.setEnergyStored(TD.Energy.EU, tStack, 1000);
		assertTrue(tOut == tStack, "setEnergyStored returns the same stack (the :101 convenience)");
		assertEquals(1000, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "the write lands in the carrier");
		// a FRESH stack over the same item re-reads the charge off the carrier — the round trip
		ItemStack tCopy = tStack.copy();
		assertEquals(1000, GT6BatteryItem.readStored(sLeadAcidUl, tCopy), "the carrier survives the stack copy");
	}

	@Test
	public void setEnergyStoredClampsToTheCapacityWindow() {
		ItemStack tStack = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tStack, 999999);
		assertEquals(16000, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "the upper clamp = capacity");
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tStack, -5);
		assertEquals(0, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "the lower clamp = 0");
	}

	@Test
	public void theActiveEnergyLaneReadsAsFull() {
		ItemStack tStack = new ItemStack(sLeadAcidUl);
		GT6BatteryItem.writeItemNBT(tStack, 12345);
		// the store-as-full lane: the :70-71 branch collapses the charge to capacity
		//? if forge {
		tStack.getOrCreateTag().putBoolean(GT6BatteryItem.NBT_ACTIVE_ENERGY, true);
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = tStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putBoolean(GT6BatteryItem.NBT_ACTIVE_ENERGY, true);
		tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
		assertEquals(16000, GT6BatteryItem.readStored(sLeadAcidUl, tStack), "gt.active.energy true = FULL");
		// and the write face clears the lane again (the :90 verbatim)
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tStack, 500);
		assertFalse(GT6BatteryItem.readActiveEnergy(tStack), "the write face resets gt.active.energy (the :90 verbatim)");
		assertEquals(500, GT6BatteryItem.readStored(sLeadAcidUl, tStack));
	}

	// ---------------------------------------------------------------------------
	// acceptance ④ — the EU/LU mutual rejection
	// ---------------------------------------------------------------------------

	@Test
	public void euAndLuDomainsRejectEachOther() {
		ItemStack tEuBattery = new ItemStack(sLeadAcidUl);
		ItemStack tLuBattery = new ItemStack(sEnergiumRed);
		// the LU packets bounce off the EU battery (the :228/:157 type gate)
		assertEquals(0, sLeadAcidUl.doEnergyInjection(TD.Energy.LU, tEuBattery, 8, 5, true), "LU injection into an EU battery: rejected");
		assertEquals(0, sLeadAcidUl.doEnergyExtraction(TD.Energy.LU, tEuBattery, 8, 5, true), "LU extraction from an EU battery: rejected");
		// and the mirror
		assertEquals(0, sEnergiumRed.doEnergyInjection(TD.Energy.EU, tLuBattery, 8, 5, true), "EU injection into an LU battery: rejected");
		// the storage faces read 0 across the domain line (the :218/:219)
		assertEquals(0, sLeadAcidUl.getEnergyStored(TD.Energy.LU, tEuBattery), "the stored face is domain-gated");
		assertEquals(0, sLeadAcidUl.getEnergyCapacity(TD.Energy.LU, tEuBattery), "the capacity face is domain-gated");
		// but the same-domain faces work
		assertEquals(16000, sLeadAcidUl.getEnergyCapacity(TD.Energy.EU, tEuBattery), "the same-domain capacity face");
		assertEquals(3200000, sEnergiumRed.getEnergyCapacity(TD.Energy.LU, tLuBattery), "the LU battery answers LU");
		assertFalse(sLeadAcidUl.useEnergy(TD.Energy.LU, tEuBattery, 1, false), "useEnergy refuses the wrong domain (the :194)");
	}

	// ---------------------------------------------------------------------------
	// the packet band + the packet math (the :62-66/:155-:174 transcription)
	// ---------------------------------------------------------------------------

	@Test
	public void thePacketBandFloorsTheUlvMinToOne() {
		// ULV: rec 8 -> min = 4 -> the :64 floor arm opens to 1; max = 16
		assertEquals(1, sLeadAcidUl.mSizeMin, "the ULV floor arm (mSizeMin <= 8 -> 1)");
		assertEquals(16, sLeadAcidUl.mSizeMax, "the :63 max = rec*2");
		assertTrue(sLeadAcidUl.canEnergyInjection(TD.Energy.EU, new ItemStack(sLeadAcidUl), 1), "the 1 EU packet fits the ULV window");
		assertFalse(sLeadAcidUl.canEnergyInjection(TD.Energy.EU, new ItemStack(sLeadAcidUl), 17), "17 EU overloads the ULV window");
		// LV: rec 32 -> the regular band [16..64]
		assertEquals(16, sLeadAcidLv.mSizeMin, "LV keeps the regular rec/2 min");
		assertEquals(64, sLeadAcidLv.mSizeMax);
		assertFalse(sLeadAcidLv.canEnergyInjection(TD.Energy.EU, new ItemStack(sLeadAcidLv), 8), "8 EU dies below the LV window (the ULV wall)");
		assertTrue(sLeadAcidLv.canEnergyInjection(TD.Energy.EU, new ItemStack(sLeadAcidLv), 64), "64 EU fits the LV max");
	}

	@Test
	public void thePacketMathCountsUsedPackets() {
		ItemStack tStack = new ItemStack(sLeadAcidUl);
		// 5 packets of 8 EU land fully
		assertEquals(5, sLeadAcidUl.doEnergyInjection(TD.Energy.EU, tStack, 8, 5, true), "the used-packet count");
		assertEquals(40, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack));
		// a size-0 stack cannot be charged (the :228 canEnergyInjection gate)
		ItemStack tDouble = new ItemStack(sLeadAcidUl, 2);
		assertEquals(0, sLeadAcidUl.doEnergyInjection(TD.Energy.EU, tDouble, 8, 5, true), "the single-stack gate");
		// extraction takes from the same window
		assertEquals(3, sLeadAcidUl.doEnergyExtraction(TD.Energy.EU, tStack, 8, 3, true), "3 packets extracted");
		assertEquals(16, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack));
		// over-extraction refuses: asking for a 32 EU packet from a 16 EU charge (also over the ULV max 16)
		assertEquals(0, sLeadAcidUl.doEnergyExtraction(TD.Energy.EU, tStack, 32, 1, true), "over-band extraction rejected");
		// the per-call packet cap: rAmount = min(mSizeRec, aAmount) — 8 packets of 8 EU per call (the :159)
		assertEquals(8, sLeadAcidUl.doEnergyInjection(TD.Energy.EU, tStack, 8, 4000, true), "one call moves at most mSizeRec packets");
		assertEquals(80, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack));
		// the capacity limiter: driving the loop to the top, the :160 while-loop trims the final call
		while (sLeadAcidUl.doEnergyInjection(TD.Energy.EU, tStack, 8, 8, true) > 0) ;
		assertEquals(16000, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "the battery is full");
		assertEquals(0, sLeadAcidUl.doEnergyInjection(TD.Energy.EU, tStack, 8, 1, true), "the full gate (:158)");
	}

	@Test
	public void useEnergyIsTheCombinedSimulateFace() {
		ItemStack tStack = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tStack, 100);
		assertTrue(sLeadAcidUl.useEnergy(TD.Energy.EU, tStack, 40, false), "simulate: can use");
		assertEquals(100, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "simulate does not deduct");
		assertTrue(sLeadAcidUl.useEnergy(TD.Energy.EU, tStack, 40, true), "do: can use");
		assertEquals(60, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "do deducts");
		assertFalse(sLeadAcidUl.useEnergy(TD.Energy.EU, tStack, 100, true), "cannot overdraw");
		assertEquals(0, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tStack), "the failed use drains to 0 (the :204)");
	}

	// ---------------------------------------------------------------------------
	// acceptance ⑥ — the stack faces
	// ---------------------------------------------------------------------------

	@Test
	public void chargedStacksAreSinglesAndTheBarShowsMidChargeOnly() {
		ItemStack tEmpty = new ItemStack(sLeadAcidUl);
		assertEquals(16, sLeadAcidUl.getMaxStackSize(tEmpty), "empty batteries stack 16 (the :143 else arm)");
		assertFalse(sLeadAcidUl.isBarVisible(tEmpty), "acceptance ⑥: empty = no bar");
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tEmpty, 8000);
		assertEquals(1, sLeadAcidUl.getMaxStackSize(tEmpty), "charged batteries are singles (the :143)");
		assertTrue(sLeadAcidUl.isBarVisible(tEmpty), "acceptance ⑥: mid charge = bar");
		assertEquals(6, sLeadAcidUl.getBarWidth(tEmpty), "8000/16000 * 13 = 6.5 -> 6 (integer division)");
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tEmpty, 16000);
		assertFalse(sLeadAcidUl.isBarVisible(tEmpty), "acceptance ⑥: full = no bar (the vanilla undamaged-tool analogy)");
	}
}
