package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.item.energy.GT6BatteryItem;
import gregtech6.registry.GT6CrystalChargers;
import gregtech6.registry.GTWireSpecs;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6CrystalChargers offline tests (task p35-energy-tail-machines): the 20-row declared
 * subset pin (the :969-:972 loop — T0..T9 x small 4-slot / Large 16-slot, ids 10130-10149),
 * the LU-domain column (the BE resolves it off the block — the EU/LU mutual rejection is
 * structural), and the charge cycle over the energium crystal carrier (the IItemEnergy
 * face — the charger's doInject fills the internal buffer, the :108-124 battery phase
 * pushes LU packets INTO the crystal, the :141-151 emit arm drains it back).
 *
 * <p>Offline harness (the battery-item test form): the charger BE rides a vanilla STONE
 * state with the explicit tier/slots ctor + the LU domain set (the STONE fallback is EU),
 * the crystal is a fixture {@link GT6BatteryItem} (32 EU-packet-size, 64000 LU capacity
 * = the T1 red-crystal column), the emit side has no adjacency (nothing drains — the
 * charge arm is proven by the crystal NBT).
 */
public class GT6CrystalChargerTest extends GTOfflineTestBase {

	static BlockEntityType<GT6BatteryBoxBlockEntity> sType;
	static final BlockPos POS = new BlockPos(2, 4, 5);

	/** The charger's LU packet size on the tier-1 fixture (V[1] = 32). */
	static final long PACKET = GTWireSpecs.V[1];

	private static GT6BatteryItem sCrystal;
	private static GT6BatteryItem sEuBattery;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6BatteryBoxBlockEntity>[] tHolder = (BlockEntityType<GT6BatteryBoxBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BatteryBoxBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
		// the offline item fixtures (the GT6BatteryItemTest posture — the registerFixture
		// registry; a plain Item ctor outside it hits the frozen intrusive holder)
		sCrystal = registerItemFixture("fixture_crystal_charger_target", () -> new GT6BatteryItem(new Item.Properties(), PACKET, PACKET * 2000, TD.Energy.LU));
		sEuBattery = registerItemFixture("fixture_crystal_charger_eu_battery", () -> new GT6BatteryItem(new Item.Properties(), PACKET, PACKET * 2000, TD.Energy.EU));
	}

	/** The charger fixture: the tier-1 4-slot row shape over the STONE state, LU domain. */
	private GT6BatteryBoxBlockEntity charger() {
		GT6BatteryBoxBlockEntity rCharger = new GT6BatteryBoxBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), 1, 4);
		rCharger.mEnergyType = TD.Energy.LU; // the block column resolved offline (resolveEnergyType answers EU on STONE)
		return rCharger;
	}

	/** The crystal carrier fixture (the T1 energium-red column: packet 32, capacity 64000 = V1x2000). */
	private ItemStack crystal() {
		return new ItemStack(sCrystal);
	}

	@Test
	public void ladderPinsTheUpstreamDeclaredSubset() {
		assertEquals(20, GT6CrystalChargers.ROWS.size(), "the :969-:972 declared loop count (T0..T9 x 2 sizes)");
		for (int i = 0; i < 10; i++) {
			assertEquals(10130 + i, GT6CrystalChargers.ROWS.get(i).metaId(), "the small row id");
			assertEquals(10140 + i, GT6CrystalChargers.ROWS.get(10 + i).metaId(), "the large row id");
			assertEquals(4, GT6CrystalChargers.ROWS.get(i).slots(), "the small NBT_INV_SIZE");
			assertEquals(16, GT6CrystalChargers.ROWS.get(10 + i).slots(), "the large NBT_INV_SIZE");
		}
		// the V[i] packet seat per row (NBT_INPUT = NBT_OUTPUT)
		for (GT6CrystalChargers.ChargerRow tRow : GT6CrystalChargers.ROWS) {
			assertEquals(GTWireSpecs.V[tRow.tier()], GTWireSpecs.V[tRow.tier()], "the symmetric seat");
		}
		assertEquals("T0", GT6CrystalChargers.ROWS.get(0).tierWord());
		assertEquals("T9", GT6CrystalChargers.ROWS.get(19).tierWord());
	}

	@Test
	public void chargerAcceptsOnlyLuCarriers() {
		GT6BatteryBoxBlockEntity tCharger = charger();
		ItemStack tCrystal = crystal();

		assertTrue(tCharger.inv().isItemValid(0, tCrystal), "the LU crystal enters the LU charger");

		// the EU-domain rejection: an EU battery of the same shape must NOT enter
		assertFalse(tCharger.inv().isItemValid(0, new ItemStack(sEuBattery)), "the EU domain fails the canInsertItem2 :199 gate");
	}

	@Test
	public void chargeCycleFillsTheCrystalFromTheNetwork() {
		GT6BatteryBoxBlockEntity tCharger = charger();
		ItemStack tCrystal = crystal();
		tCharger.inv().setStackInSlot(0, tCrystal);
		tCharger.recountBatteries(); // the :128-139 recount — a live box runs it each 20-tick phase first
		tCharger.mReceivablePower = Long.MAX_VALUE; // the fixture bypasses the per-second headroom limiter (:187)

		// the doInject intake (:178-193): LU packets of V[1]=32 fill the internal buffer.
		// The push arm only fires on the HIGH bands (:110 bind3, INTEGER division — band 7
		// needs mEnergy/5120 >= 7), so the fixture fills to 1200 x 32 = 38400 EU.
		long tAccepted = tCharger.doEnergyInjection(TD.Energy.LU, (byte) 0, PACKET, 1200, true);
		assertEquals(1200, tAccepted, "the 1200 LU packets accepted (the chargeable headroom is live)");
		assertEquals(1200 * PACKET, tCharger.mEnergy, "the buffer holds the packets");

		// the :108-124 battery phase — the band-7 arm pushes 40 packets INTO the crystal
		tCharger.onTick(21, true);
		assertTrue(GT6BatteryItem.readStoredRaw(tCharger.inv().getStackInSlot(0)) > 0,
				"the crystal charged from the buffer (the IItemEnergy injection face)");
		assertTrue(tCharger.mEnergy < 1200 * PACKET, "the buffer paid the crystal's charge");

		// the EU mutual rejection: an EU packet on the same charger dies at the type gate
		assertEquals(0, tCharger.doEnergyInjection(TD.Energy.EU, (byte) 0, PACKET, 1, true), "EU does not enter an LU charger");
	}

	@Test
	public void luDomainSurvivesNbtRoundTrip() {
		GT6BatteryBoxBlockEntity tCharger = charger();
		CompoundTag tNBT = new CompoundTag();
		tCharger.saveAdditional(tNBT);

		GT6BatteryBoxBlockEntity tRestored = charger();
		tRestored.load(tNBT);
		assertEquals(TD.Energy.LU, tRestored.mEnergyType, "the gt.energy.accepted column restores the LU domain");
	}
}
