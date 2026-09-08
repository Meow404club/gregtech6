package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.tileentity.energy.EnergyBridge;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The EU->FE outbound bridge integration pins (task p26-eu-bridge-outbound), the mdk-side
 * half: the FE battery fixture's platform EnergyStorage reference implementation driven
 * through the root {@code EnergyBridge.insertFe} math exactly the way the per-leg handlers
 * drive it (storage::receiveEnergy adapted to EnergyBridge.IFEReceiver), plus the fixture's
 * NBT round trip.
 *
 * <p>Offline-harness boundary (the TileEntityBase01Root:457 record): ForgeCapabilities
 * cannot class-init offline (CapabilityToken.getType needs the runtime transformer), so the
 * handler's capability-query arm is NOT exercised here — the per-leg handler arms live
 * behind the //? hunks in GT6FeBatteries and their storage-resolution face is proven by
 * the live RCON chain on both legs (the dual-leg runServer acceptance). What THIS class
 * pins is the everything-after-the-query half: fixture storage semantics + the root math,
 * integrated.
 */
public class GT6EuToFeBridgeSeamTest extends GTOfflineTestBase {

	static BlockEntityType<GT6FeBatteryBlockEntity> sType;
	static final BlockPos POS = new BlockPos(7, 8, 9);

	@BeforeAll
	static void buildOfflineFixture() {
		BlockEntityType<GT6FeBatteryBlockEntity>[] tHolder = (BlockEntityType<GT6FeBatteryBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6FeBatteryBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	@AfterEach
	public void restoreDefaultSeam() {
		EnergyBridge.register(null); // the ITileEntityEnergyTest discipline
	}

	private static BlockState stoneState() {
		return Blocks.STONE.defaultBlockState();
	}

	/**
	 * THE acceptance math, integrated: the fixture BE's storage answers a bridged 32 EU x 1 A
	 * packet train with exactly 128 FE (4:1, CS.RF_PER_EU = CS.java:54) — the same call shape
	 * the per-leg handlers make after their capability query returns.
	 */
	@Test
	public void fixtureStorageBridges32EuTo128FeThroughTheRootMath() {
		GT6FeBatteryBlockEntity tBattery = new GT6FeBatteryBlockEntity(sType, POS, stoneState());
		long tUsed = EnergyBridge.insertFe(tBattery.energyStorage()::receiveEnergy, 32, 1);
		assertEquals(1, tUsed, "one 32 EU packet fully accepted -> billed 1 packet");
		assertEquals(128, tBattery.storedFe(), "32 EU x 4 = 128 FE in the fixture");
	}

	@Test
	public void fixtureStorageSoaksAMultiPacketTrainLinearly() {
		GT6FeBatteryBlockEntity tBattery = new GT6FeBatteryBlockEntity(sType, POS, stoneState());
		long tUsed = EnergyBridge.insertFe(tBattery.energyStorage()::receiveEnergy, 32, 5);
		assertEquals(5, tUsed);
		assertEquals(640, tBattery.storedFe(), "5 A x 32 EU x 4 = 640 FE");
	}

	@Test
	public void fixtureStorageIsAPureSinkAndNeverSaturatesInTheLinearRegion() {
		GT6FeBatteryBlockEntity tBattery = new GT6FeBatteryBlockEntity(sType, POS, stoneState());
		// the reference implementation's own canExtract face: maxExtract = 0 -> extraction 0
		assertEquals(0, tBattery.energyStorage().extractEnergy(1000, false), "outbound-only: the fixture never feeds back");
		assertEquals(0, tBattery.energyStorage().extractEnergy(1000, true));
		// a canReceive probe must answer true before any insert (the handler's whitelist gate order)
		assertTrue(tBattery.energyStorage().canReceive(), "the fixture advertises receive");
		// the 100k capacity absorbs the default rig's rate for the whole RCON window
		EnergyBridge.insertFe(tBattery.energyStorage()::receiveEnergy, 32, 1);
		assertEquals(GT6FeBatteryBlockEntity.CAPACITY - 128, tBattery.energyStorage().receiveEnergy(Integer.MAX_VALUE, true),
				"remaining room after the first packet = capacity - 128");
	}

	@Test
	public void fixtureNBTRoundTripsTheStoredFe() {
		GT6FeBatteryBlockEntity tBattery = new GT6FeBatteryBlockEntity(sType, POS, stoneState());
		EnergyBridge.insertFe(tBattery.energyStorage()::receiveEnergy, 32, 3); // 384 FE
		CompoundTag tNBT = new CompoundTag();
		// the vanilla save/load hook signatures diverged (1.21.1: saveAdditional/
		// loadAdditional + HolderLookup.Provider, which the fixture's implementation
		// never reads — null is safe there)
		//? if forge {
		tBattery.saveAdditional(tNBT);
		//?} else {
		/*tBattery.saveAdditional(tNBT, null);
		 *///?}
		assertTrue(tNBT.contains(GT6FeBatteryBlockEntity.NBT_ENERGY), "the stored-FE key persists");
		GT6FeBatteryBlockEntity tRestored = new GT6FeBatteryBlockEntity(sType, POS, stoneState());
		//? if forge {
		tRestored.load(tNBT.copy());
		//?} else {
		/*tRestored.loadAdditional(tNBT.copy(), null);
		 *///?}
		assertEquals(384, tRestored.storedFe(), "the stored-FE restore face round-trips");
	}

	@Test
	public void theDefaultSeamStillBridgesNothingWithoutAHandler() {
		// the mdk-side regression half: with NO handler installed (the shipped state before
		// mod construct, and after any test restore), a foreign FE-flavoured dispatch is 0 —
		// GT receivers are dispatched before the bridge and never reach it (root-covered).
		GT6FeBatteryBlockEntity tBattery = new GT6FeBatteryBlockEntity(sType, POS, stoneState());
		assertEquals(0, EnergyBridge.insertEnergyInto(
				gregapi.data.TD.Energy.EU, (byte)2, 32, 5, null, tBattery),
				"no handler -> the seam answers 0 (the upstream :141 fall-through)");
		assertEquals(0, tBattery.storedFe());
	}
}
