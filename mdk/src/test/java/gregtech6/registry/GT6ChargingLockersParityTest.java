/**
 * The Charging Locker family parity pins (task block-family-32xxx-port): the 60-row
 * metalset ladder rides the SINGLE-SOURCE hopper table (Loader_MultiTileEntities
 * .java:139 over the :186-245 loop — the same aID/local/hardness lines the hopper pair
 * :145-146 walk), so the pins prove the derivation, not a second 60-line copy.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

class GT6ChargingLockersParityTest extends GTOfflineTestBase {

	@BeforeAll
	static void boot() {
		gregtech6.registry.GT6MaterialTestSupport.materials();
		gregtech6.registry.GT6DriverTestSupport.loadFamiliesPristine();
	}

	/** The walk parity: the charging family and the hopper pair share the ONE metalset gate. */
	@Test
	void rowsWalkTheSameMetalsetGateAsTheHoppers() {
		assertEquals(GT6Hoppers.ROWS.size(), 2 * GT6ChargingLockers.ROWS.size(),
				"the charging family walks the same 60-line gate — half the hopper pair's rows");
	}

	/** Every row: the path template, the :139 meta base over the loader aID, the shared material record. */
	@Test
	void everyRowCarriesTheLoader139Columns() {
		Map<String, GT6Hoppers.HopperMaterial> hopperMaterials = new HashMap<>();
		for (GT6Hoppers.HopperRow tHopper : GT6Hoppers.ROWS) {
			hopperMaterials.putIfAbsent(tHopper.material().slug(), tHopper.material());
		}
		assertEquals(GT6ChargingLockers.ROWS.size(), hopperMaterials.size(),
				"one charging row per gated metalset line, no duplicates");
		for (GT6ChargingLockers.ChargingLockerRow tRow : GT6ChargingLockers.ROWS) {
			assertEquals("charging_locker_" + tRow.material().slug(), tRow.path(),
					"the path template rides the slug");
			assertEquals(7500 + tRow.material().metaId(), tRow.metaId(),
					"the :139 id column 7500+aID over the loader aID (" + tRow.path() + ")");
			assertSame(hopperMaterials.get(tRow.material().slug()), tRow.material(),
					"the material record IS the hopper table's (the single-source proof)");
		}
	}

	/** The registration maps stay row-parallel (the BET multi-mount and datagen walkers consume them). */
	@Test
	void blockAndItemMapsMirrorTheRows() {
		assertEquals(GT6ChargingLockers.ROWS.size(), GT6ChargingLockers.BLOCKS_BY_PATH.size());
		assertEquals(GT6ChargingLockers.ROWS.size(), GT6ChargingLockers.ITEMS_BY_PATH.size());
		for (GT6ChargingLockers.ChargingLockerRow tRow : GT6ChargingLockers.ROWS) {
			assertTrue(GT6ChargingLockers.BLOCKS_BY_PATH.containsKey(tRow.path()));
			assertTrue(GT6ChargingLockers.ITEMS_BY_PATH.containsKey(tRow.path()));
		}
		// the payload length rides the live registry only (blockArray() dereferences the
		// handles) — the datagen walk + the BET mount are the live face (the tree pin)
	}
}
