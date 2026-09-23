package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregtech6.item.energy.GT6BatteryItem;
import gregtech6.item.energy.GT6ZpmItem;
import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6ZpmDechargers;
import gregtech6.registry.GTWireSpecs;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The ZPM Decharger offline tests (task p36-energy-zpm-dechargers): the two-row pin (the
 * :1000-:1001 columns — meta 11170/11171, the QU in lane, the QU/EU out lanes, V[7]
 * packets, 1 slot), the ZPM slot gate positive+negative (the :36-:37 — the artifact
 * enters, the EU/LU battery ladders of the SAME IItemEnergy language fail), the split
 * type lanes (the Base10 :202 restoration), the DISCHARGE-only cycle live (band-0 pull
 * off the full ZPM, the emit arm pays ONE V[7] packet into a tier-7 box sink), the
 * intake refusal (the :179 guard over the zero chargeable count — the one-way lane),
 * and the cover-host arming (the capacitor lane the p35 covers card left unreachable).
 *
 * <p>Offline harness (the charger-test form): both BEs ride vanilla STONE states with the
 * explicit tier/slots ctor; the decharger's QU in lane is set off the STONE EU fallback,
 * the out lane comes through the fixture ctor; the sink box holds a chargeable EU battery.
 */
public class GT6ZpmDechargerTest extends GTOfflineTestBase {

	static BlockEntityType<GT6ZpmDechargerBlockEntity> sDechType;
	static BlockEntityType<GT6BatteryBoxBlockEntity> sBoxType;
	static final BlockPos POS = new BlockPos(2, 4, 5);
	static final BlockPos SINK_POS = new BlockPos(2, 4, 4); // north of the decharger — the FRONT face

	/** The ZPM packet seat (V[7]). */
	static final long PACKET = GTWireSpecs.V[7];

	private static GT6ZpmItem sZpm;
	private static GT6BatteryItem sEuBattery;
	private static GT6BatteryItem sLuCrystal;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// the offline boot noise (the charger-test shape); registries are ready
		}
		BlockEntityType<GT6ZpmDechargerBlockEntity>[] tDech = (BlockEntityType<GT6ZpmDechargerBlockEntity>[]) new BlockEntityType<?>[1];
		tDech[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6ZpmDechargerBlockEntity(tDech[0], aPos, aState), Blocks.STONE).build(null);
		sDechType = tDech[0];
		BlockEntityType<GT6BatteryBoxBlockEntity>[] tBox = (BlockEntityType<GT6BatteryBoxBlockEntity>[]) new BlockEntityType<?>[1];
		tBox[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BatteryBoxBlockEntity(tBox[0], aPos, aState), Blocks.STONE).build(null);
		sBoxType = tBox[0];
		// the ZPM (the row-driven band) + the same-language rejections (the EU/LU ladders)
		sZpm = registerItemFixture("fixture_zpm_decharger_target", () -> new GT6ZpmItem(new Item.Properties(),
				GT6Batteries.ZPM.sizeRec(), GT6Batteries.ZPM.capacity(), GT6Batteries.ZPM.sizeMin()));
		sEuBattery = registerItemFixture("fixture_zpm_decharger_eu", () -> new GT6BatteryItem(new Item.Properties(), PACKET, PACKET * 2000, TD.Energy.EU));
		sLuCrystal = registerItemFixture("fixture_zpm_decharger_lu", () -> new GT6BatteryItem(new Item.Properties(), 32, 64000, TD.Energy.LU));
	}

	/** The Electric decharger fixture: the :1001 row (QU in, EU out) over the STONE state. */
	private GT6ZpmDechargerBlockEntity electricDecharger() {
		GT6ZpmDechargerBlockEntity rDech = new GT6ZpmDechargerBlockEntity(sDechType, POS, Blocks.STONE.defaultBlockState(), TD.Energy.EU);
		rDech.mEnergyType = TD.Energy.QU; // the block column resolved offline (the STONE fallback is EU)
		return rDech;
	}

	/** The full ZPM in the slot. */
	private ItemStack fullZpm(GT6ZpmDechargerBlockEntity aDech) {
		ItemStack tStack = new ItemStack(sZpm);
		sZpm.setEnergyStored(TD.Energy.QU, tStack, GT6Batteries.ZPM.capacity());
		aDech.inv().setStackInSlot(0, tStack);
		return tStack;
	}

	@Test
	public void theRowsPinTheLoader1000And1001Columns() {
		assertEquals(2, GT6ZpmDechargers.ROWS.size(), "the two :1000-:1001 rows");
		GT6ZpmDechargers.DechargerRow tQuantum = GT6ZpmDechargers.ROWS.get(0);
		GT6ZpmDechargers.DechargerRow tElectric = GT6ZpmDechargers.ROWS.get(1);
		assertEquals("zpm_decharger_quantum", tQuantum.path());
		assertEquals(11170, tQuantum.metaId(), "the :1000 meta");
		assertEquals("ZPM Decharger (Quantum)", tQuantum.enName(), "the :1000 display column");
		assertEquals(TD.Energy.QU, tQuantum.outType().get(), ":1000 — the QU-ladder feed");
		assertEquals("zpm_decharger_electric", tElectric.path());
		assertEquals(11171, tElectric.metaId(), "the :1001 meta");
		assertEquals("ZPM Decharger (Electric)", tElectric.enName(), "the :1001 display column");
		assertEquals(TD.Energy.EU, tElectric.outType().get(), ":1001 — the EU-ladder feed");
		for (GT6ZpmDechargers.DechargerRow tRow : GT6ZpmDechargers.ROWS) {
			assertEquals(7, tRow.tier(), "NBT_INPUT/NBT_OUTPUT V[7]");
			assertEquals(1, tRow.slots(), "NBT_INV_SIZE 1");
		}
	}

	@Test
	public void theSlotGateTakesOnlyTheZpm() {
		GT6ZpmDechargerBlockEntity tDech = electricDecharger();
		// the positive pin: the artifact enters (any charge state — the IL.ZPM.equal form)
		assertTrue(tDech.inv().isItemValid(0, new ItemStack(sZpm)), ":36 — the ZPM enters");
		ItemStack tFull = new ItemStack(sZpm);
		sZpm.setEnergyStored(TD.Energy.QU, tFull, GT6Batteries.ZPM.capacity());
		assertTrue(tDech.inv().isItemValid(0, tFull), "the charged artifact enters too");
		// the negative pins: the SAME IItemEnergy language, wrong artifact
		assertFalse(tDech.inv().isItemValid(0, new ItemStack(sEuBattery)), ":37 — the EU ladder fails the gate");
		assertFalse(tDech.inv().isItemValid(0, new ItemStack(sLuCrystal)), ":37 — the LU crystal fails the gate");
		assertFalse(tDech.inv().isItemValid(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)), "the plain stack fails");
	}

	@Test
	public void theSplitTypeLanesRestoreTheBase202Face() {
		GT6ZpmDechargerBlockEntity tElectric = electricDecharger();
		// the in lane: QU accepted, EU refused
		assertTrue(tElectric.isEnergyType(TD.Energy.QU, (byte) 0, false), ":202 — the QU in lane");
		assertFalse(tElectric.isEnergyType(TD.Energy.EU, (byte) 0, false), ":202 — EU is NOT the accepted lane");
		// the out lane: EU emitted, QU not
		assertTrue(tElectric.isEnergyType(TD.Energy.EU, (byte) 2, true), ":202 — the EU out lane (the front seat)");
		assertFalse(tElectric.isEnergyType(TD.Energy.QU, (byte) 2, true), ":202 — QU is not the Electric emit lane");
		assertEquals(java.util.List.of(TD.Energy.QU, TD.Energy.EU), java.util.List.copyOf(tElectric.getEnergyTypes((byte) 0)),
				":212 — the two-lane pair");
		// the Quantum twin emits QU (the :1000 lane)
		GT6ZpmDechargerBlockEntity tQuantum = new GT6ZpmDechargerBlockEntity(sDechType, POS, Blocks.STONE.defaultBlockState(), TD.Energy.QU);
		tQuantum.mEnergyType = TD.Energy.QU;
		assertTrue(tQuantum.isEnergyType(TD.Energy.QU, (byte) 2, true), ":1000 — the Quantum emit lane is QU");
		assertTrue(tQuantum.emitType() == TD.Energy.QU && tElectric.emitType() == TD.Energy.EU, "the emitType seat splits the rows");
	}

	@Test
	public void theDischargeCyclePaysTheSinkLive() {
		GT6ZpmDechargerBlockEntity tDech = electricDecharger();
		ItemStack tZpm = fullZpm(tDech);
		// the sink: a tier-7 box (the V[7] band [65536..262144]) with a chargeable EU
		// battery of the SAME band (the :179 headroom needs a chargeable carrier)
		GT6BatteryBoxBlockEntity tSink = new GT6BatteryBoxBlockEntity(sBoxType, SINK_POS, Blocks.STONE.defaultBlockState(), 7, 4);
		tSink.inv().setStackInSlot(0, new ItemStack(sEuBattery));
		tSink.recountBatteries(); // a live box recounts each 20-tick phase first
		assertEquals(0, tSink.mEnergy, "the sink starts empty");
		// the FRONT face adjacency: the decharger emits into the sink
		tDech.setAdjacencyOverride(aSide -> aSide == 2 ? new EnergyTarget(tSink, (byte) 3) : null);

		tDech.onTick(21, true); // the :108 20-tick phase (band 0 pull) + the emit arm
		// the ZPM PAID: 40 packets × V[7] left the artifact (the discharge-only live face)
		long tAfterPull = GT6BatteryItem.readStoredRaw(tDech.inv().getStackInSlot(0));
		assertTrue(tAfterPull < GT6Batteries.ZPM.capacity(), "the artifact drained");
		assertEquals(GT6Batteries.ZPM.capacity() - 40 * PACKET, tAfterPull, "the band-0 pull took exactly 40 packets");
		// the buffer holds the pull minus the ONE emitted packet
		assertEquals(39 * PACKET, tDech.mEnergy, "the buffer paid one emitted packet");
		assertTrue(tDech.mEmitsEnergy, ":146 — the emit lane ran");
		// the SINK got paid: one 131072-sized EU packet landed in its buffer
		assertEquals(PACKET, tSink.mEnergy, "the EU-ladder feed delivered the packet");
	}

	@Test
	public void theIntakeRefusesTheNetworkPacket() {
		GT6ZpmDechargerBlockEntity tDech = electricDecharger();
		fullZpm(tDech);
		tDech.recountBatteries();
		// the :179 guard over the ZERO chargeable count (the ZPM canEnergyInjection=F):
		// the decharger accepts NO network intake even on its QU in lane
		assertEquals(0, tDech.mChargeableCount, "the artifact never registers chargeable");
		assertEquals(1, tDech.mBatteryCount, "the artifact registers as the battery");
		assertEquals(0, tDech.mReceivablePower, ":153 — the headroom reads 0");
		assertEquals(0, tDech.doInject(TD.Energy.QU, (byte) 3, PACKET, 10, true), ":179 — the one-way lane refuses");
		assertEquals(0, tDech.mEnergy, "nothing entered the buffer");
		// and the QU-typed network packet against the QUANTUM row refuses identically
		GT6ZpmDechargerBlockEntity tQuantum = new GT6ZpmDechargerBlockEntity(sDechType, POS, Blocks.STONE.defaultBlockState(), TD.Energy.QU);
		tQuantum.mEnergyType = TD.Energy.QU;
		fullZpm(tQuantum);
		tQuantum.recountBatteries();
		assertEquals(0, tQuantum.doInject(TD.Energy.QU, (byte) 3, PACKET, 10, true), ":179 — both rows refuse");
	}

	@Test
	public void theQuantumRowKeepsTheChargeArmDead() {
		// the :113-:114 push arms call through to the ZPM injection face and drain 0 — the
		// buffer can NEVER push back into the artifact (the charger cycle has no ZPM twin)
		GT6ZpmDechargerBlockEntity tDech = electricDecharger();
		ItemStack tZpm = fullZpm(tDech);
		long tBefore = GT6BatteryItem.readStoredRaw(tZpm);
		tDech.mEnergy = 7 * tDech.bandScale(); // force band 7 — the strongest push arm
		tDech.onTick(21, true);
		assertEquals(tBefore, GT6BatteryItem.readStoredRaw(tZpm), "the band-7 push left the artifact untouched");
	}
	// the cover-host arming rides the covers-package test (CoverDechargerHostTest — the
	// CoverMachineLanes admission is package-private there)
}
