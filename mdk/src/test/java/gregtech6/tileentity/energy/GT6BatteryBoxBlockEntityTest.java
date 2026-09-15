package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.item.energy.GT6BatteryItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The BatteryBox BE pins (task p29-w4-battery-storage acceptance ③/④): the 4/16-slot
 * charge/discharge arms (the :108-:124 bind3 band exchange, the per-battery packet counts
 * riding the IItemEnergy faces), the emit arm (the :141-:151 packet train out of the
 * FRONT into a counting sink — the GTEnergySourceBlockEntityTest adjacency-override
 * fixture), the V[i] gates (acceptance ③: packets above V[i]*2 are consumed-but-not-stored
 * and climb the overload ladder — the ULV wall arm; packets below V[i]/2 are swallowed by
 * the Root gate without reaching doInject), the EU/LU mutual rejection (acceptance ④:
 * the :199 canInsertItem2 gate + the :210-:211 sums), and the NBT round trip.
 *
 * <p>Offline harness (the transformer form): level-less fixtures on a vanilla STONE state,
 * explicit BETs over the array-holder trick, an explicit slot count (the STONE state
 * carries no GT6 block), the counting sink wired through the adjacency override, the
 * intake side driven through the Root gated {@code doEnergyInjection} (the live path).
 */
public class GT6BatteryBoxBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6BatteryBoxBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	static GT6BatteryItem sLeadAcidUl; // 8 EU packets, 16000 EU — the ULV box's own tier (band [1..16])
	static GT6BatteryItem sEnergiumRed; // 8 LU packets, 3200000 LU

	@BeforeAll
	static void warmUpAndBuild() {
		gregtech6.tileentity.energy.GTEnergySourceBlockEntity.resolveEnergyType("TU"); // the TD CME warm-up
		BlockEntityType<GT6BatteryBoxBlockEntity>[] tHolder = (BlockEntityType<GT6BatteryBoxBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BatteryBoxBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
		sLeadAcidUl = registerFixture("fixture_box_battery_ulv", () -> new GT6BatteryItem(new Item.Properties(), 8, 16000, TD.Energy.EU));
		sEnergiumRed = registerFixture("fixture_box_battery_lu", () -> new GT6BatteryItem(new Item.Properties(), 8, 3200000, TD.Energy.LU));
	}


	/** A 4-slot ULV box (V[0]=8 in/out; buffer 8*320*4 = 10240; band divisor 8*40*4 = 1280). */
	private GT6BatteryBoxBlockEntity box4() {
		return new GT6BatteryBoxBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), 0, 4);
	}

	/** A 16-slot box (buffer 8*320*16 = 40960; band divisor 8*40*16 = 5120). */
	private GT6BatteryBoxBlockEntity box16() {
		return new GT6BatteryBoxBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), 0, 16);
	}

	/** The counting sink (the p26 eu-bridge fixture): counts accepted EU packets. */
	static final class CountingSink implements ITileEntityEnergy {
		long mPackets = 0;
		long mLastSize = 0;

		@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEmitting && aEnergyType == TD.Energy.EU;}
		@Override public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}
		@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}
		@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return aEnergyType == TD.Energy.EU;}
		@Override public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aDoInject) {mPackets += aAmount; mLastSize = aSize;}
			return aAmount;
		}
		@Override public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}
		@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	// ---------------------------------------------------------------------------
	// acceptance ③ — the charge arm (the box pushing packets INTO its batteries)
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
	public void theBoxChargesBatteriesFromTheBufferTopBand() {
		GT6BatteryBoxBlockEntity tBox = box4();
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.recountBatteries();
		assertEquals(1, tBox.mChargeableCount, "the LV battery accepts 32 EU packets");
		// band 7 needs mEnergy >= 7 * (8*40*4) = 8960; the doInject arm fills to 8*40 = 320 packets of headroom
		tBox.mEnergy = 8960;
		tBox.doBatteryPhase();
		long tStored = sLeadAcidUl.getEnergyStored(TD.Energy.EU, tBattery);
		// the ITEM caps one call at min(mSizeRec, aAmount) = 8 packets (the Base08 :159 —
		// the upstream box asks 40, the battery answers 8 per phase; the full push takes 5 phases)
		assertEquals(8 * 8, tStored, "the :114 arm pushes up to mSizeRec=8 packets of 8 EU into the battery");
		assertEquals(8960 - 8 * 8, tBox.mEnergy, "the buffer pays the pushed charge");
	}

	@Test
	public void theBoxDischargesBatteriesIntoTheBufferBottomBand() {
		GT6BatteryBoxBlockEntity tBox = box4();
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tBattery, 640); // 80 packets of 8
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.recountBatteries();
		assertEquals(1, tBox.mBatteryCount, "the charged LV battery can emit 32 EU packets");
		// band 0: mEnergy < 1280 — the box asks 40 packets of 8; the battery answers 8 per call (the :159 cap)
		tBox.doBatteryPhase();
		assertEquals(8 * 8, tBox.mEnergy, "the :111 arm pulls mSizeRec=8 packets of 8 EU into the buffer");
		assertEquals(640 - 8 * 8, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tBattery), "the battery pays");
	}

	@Test
	public void theMidBandsStayIdle() {
		GT6BatteryBoxBlockEntity tBox = box4();
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tBattery, 320);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.mEnergy = 4000; // 4000 / 1280 = 3 -> band 3, idle
		tBox.doBatteryPhase();
		assertEquals(320, sLeadAcidUl.getEnergyStored(TD.Energy.EU, tBattery), "bands 2..5 move nothing");
		assertEquals(4000, tBox.mEnergy);
	}

	// ---------------------------------------------------------------------------
	// acceptance ③ — the emit arm (the box powering a downstream EU receiver)
	// ---------------------------------------------------------------------------

	@Test
	public void theBoxEmitsToTheFrontSink() {
		GT6BatteryBoxBlockEntity tBox = box4();
		CountingSink tSink = new CountingSink();
		List<EnergyTarget> tTargets = new ArrayList<>();
		tTargets.add(new EnergyTarget(tSink, (byte) 2));
		tBox.setAdjacencyOverride(aSide -> tTargets.get(0));
		tBox.mEnergy = 1000;
		tBox.doEmit();
		// with NO batteries mBatteryCount = 0 -> the :143 cap emits nothing
		assertEquals(0, tSink.mPackets, "the :143 cap: no batteries = no emit");
		assertEquals(1000, tBox.mEnergy, "nothing left the buffer");
		// WITH a battery the emit rides: 1 battery -> up to 1 packet per pass
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tBattery, 640);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.recountBatteries();
		tBox.doEmit();
		assertEquals(1, tSink.mPackets, "one packet of 8 EU out (mBatteryCount = 1)");
		assertEquals(8, tSink.mLastSize, "the packet size is the row V[i] = 8");
		assertEquals(1000 - 8, tBox.mEnergy, "the buffer pays mOutput per accepted packet");
		assertTrue(tBox.mEmitsEnergy, "the :146 display lane");
		assertTrue(tBox.mActive, "the :126 activity flag");
	}

	@Test
	public void theStopGateAndTheVGateShapeTheEmission() {
		GT6BatteryBoxBlockEntity tBox = box4();
		tBox.mEnergy = 500;
		tBox.doEmit();
		assertFalse(tBox.mEmitsEnergy, "nothing emits with no batteries (the mBatteryCount cap)");
		// the emit face gates by face: the FRONT (byte 2) emits, the others accept
		assertTrue(tBox.isEnergyEmittingTo(TD.Energy.EU, (byte) 2, false), "FRONT emits (not stopped)");
		assertTrue(tBox.isEnergyEmittingTo(TD.Energy.EU, (byte) 3, false) == false, "a non-front face never emits");
		assertTrue(tBox.isEnergyAcceptingFrom(TD.Energy.EU, (byte) 3, false), "a non-front face accepts");
		assertFalse(tBox.isEnergyAcceptingFrom(TD.Energy.EU, (byte) 2, false), "the FRONT never accepts");
		// the stop gate: the stopped box answers a NON-theoretical emit probe false (the :205)
		tBox.mStopped = true;
		assertFalse(tBox.isEnergyEmittingTo(TD.Energy.EU, (byte) 2, false), "stopped = no live emit face");
		assertTrue(tBox.isEnergyEmittingTo(TD.Energy.EU, (byte) 2, true), "the theoretical probe keeps the static face (the conductor rule)");
	}

	// ---------------------------------------------------------------------------
	// acceptance ③ — the V[i] intake gate (overvoltage + undervoltage)
	// ---------------------------------------------------------------------------

	@Test
	public void overvoltagePacketsAreConsumedButNotStoredAndClimbTheLadder() {
		GT6BatteryBoxBlockEntity tBox = box4(); // ULV: in 8, inMax = 16, min = 4
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.recountBatteries();
		long tBefore = tBox.mEnergy;
		// a 32 EU packet vs the ULV max 16: consumed-but-not-stored (the :181-:183), one ladder strike
		assertEquals(1, tBox.doEnergyInjection(TD.Energy.EU, (byte) 3, 32, 1, true), "the oversized packet counts as USED");
		assertEquals(tBefore, tBox.mEnergy, "the buffer did NOT grow — 超压拒充");
		assertEquals(1, tBox.mExplosionPrevention, "one soft strike on the :140-148 ladder");
		// 100 more oversized strikes keep the box alive (the soft-ladder span)
		for (int i = 0; i < 99; i++) tBox.doEnergyInjection(TD.Energy.EU, (byte) 3, 32, 1, true);
		assertEquals(100, tBox.mExplosionPrevention, "the ladder caps at 100");
		assertTrue(tBox.isRemoved() == false, "the box survives the soft ladder (the Root overcharge fires past it — the live arm)");
	}

	@Test
	public void undervoltagePacketsAreSwallowedByTheRootGate() {
		GT6BatteryBoxBlockEntity tBox = box4(); // ULV: min = 8/2 = 4
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.recountBatteries();
		// a 2 EU packet dies below the min: the gate swallows the offered amount WITHOUT doInject
		assertEquals(1, tBox.doEnergyInjection(TD.Energy.EU, (byte) 3, 2, 1, true), "the gate returns aAmount (the swallowed offer)");
		assertEquals(0, tBox.mEnergy, "nothing stored");
		assertEquals(0, tBox.mExplosionPrevention, "no ladder climb — the packet never reached doInject");
		// a 4 EU packet is inside the band and stores
		assertEquals(1, tBox.doEnergyInjection(TD.Energy.EU, (byte) 3, 4, 1, true), "4 EU is inside the ULV band [4..16]");
		assertEquals(4, tBox.mEnergy, "the buffer grew by the packet size");
	}

	@Test
	public void theBufferCapsAtTheSlotHeadroom() {
		GT6BatteryBoxBlockEntity tBox = box4();
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.recountBatteries();
		assertEquals(10240, tBox.capacity(), "mInput * 320 * slots = 8*320*4 (the :185/:216 term)");
		// the :187 ReceivablePower trim: one chargeable battery = 8*2 = 16 EU of headroom
		// -> the while-loop allows ceil(16/8)+1 = 3 packets, consumed-but-stored only 3
		assertEquals(3, tBox.doEnergyInjection(TD.Energy.EU, (byte) 3, 8, 10000, true), "the band accepts, the :187 trim caps at 3");
		assertEquals(24, tBox.mEnergy, "the stored charge rides the trim");
		// a FULL buffer refuses outright (the :185 gate)
		tBox.mEnergy = tBox.capacity();
		assertEquals(0, tBox.doEnergyInjection(TD.Energy.EU, (byte) 3, 8, 1, true), "the full gate: 0");
	}

	// ---------------------------------------------------------------------------
	// acceptance ④ — the EU/LU item gate + the sums; the 16-slot family
	// ---------------------------------------------------------------------------

	@Test
	public void luBatteriesCannotEnterAndTheSumsAreDomainGated() {
		GT6BatteryBoxBlockEntity tBox = box4();
		ItemStack tLuBattery = new ItemStack(sEnergiumRed);
		assertFalse(tBox.inv().isItemValid(0, tLuBattery), "the :199 gate: an LU battery is not insertable");
		assertTrue(tBox.inv().isItemValid(0, new ItemStack(sLeadAcidUl)), "the EU battery inserts");
		// the sums walk the IItemEnergy faces (the :210-:211)
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tBattery, 1000);
		tBox.inv().setStackInSlot(0, tBattery);
		assertEquals(1000, tBox.getEnergyStored(TD.Energy.EU, (byte) 6), "the stored sum reads the battery");
		assertEquals(16000, tBox.getEnergyCapacity(TD.Energy.EU, (byte) 6), "the capacity sum reads the battery");
		assertEquals(0, tBox.getEnergyStored(TD.Energy.LU, (byte) 6), "the LU sum over EU batteries reads 0");
	}

	@Test
	public void the16SlotFamilyScalesTheHeadroom() {
		GT6BatteryBoxBlockEntity tBox = box16();
		assertEquals(16, tBox.slots(), "NBT_INV_SIZE 16");
		assertEquals(40960, tBox.capacity(), "8*320*16 — the Large box holds 4x the small one");
		assertEquals("battery_box_large", tBox.getTileEntityName(), "the family name face");
		ItemStack tBattery = new ItemStack(sLeadAcidUl);
		sLeadAcidUl.setEnergyStored(TD.Energy.EU, tBattery, 640);
		tBox.inv().setStackInSlot(0, tBattery);
		tBox.mEnergy = 0; // band 0
		tBox.doBatteryPhase();
		assertEquals(8 * 8, tBox.mEnergy, "the 16-slot exchange moves the same capped 8 packets per battery (the :111 verbatim)");
	}

	// ---------------------------------------------------------------------------
	// the NBT round trip
	// ---------------------------------------------------------------------------

	@Test
	public void nbtRoundTripCarriesTheBuffer() {
		GT6BatteryBoxBlockEntity tBox = box4();
		tBox.mEnergy = 1234;
		tBox.mMode = 5;
		CompoundTag tNbt = new CompoundTag();
		tBox.saveAdditional(tNbt);
		GT6BatteryBoxBlockEntity tOther = box4();
		tOther.load(tNbt);
		assertEquals(1234, tOther.mEnergy, "gt.energy rides");
		assertEquals(5, tOther.mMode, "gt.mode rides");
		assertEquals(8, tOther.mInput, "the row default survives (the :65 read guard)");
	}
}
