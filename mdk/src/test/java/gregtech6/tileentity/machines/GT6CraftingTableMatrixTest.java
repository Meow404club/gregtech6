package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMachines.CraftingTableRow;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task act-matrix acceptance (offline): the 120-row crafting table matrix over the
 * metalset two-line family (Loader_MultiTileEntities.java:136-137 over the 60-material
 * loop :186-245) — the census, the verbatim id/hardness columns, the upstream
 * registration order (per material the plain line then the charging line), the slug/MT
 * join, the registration map completeness and the charging BE's tool-slot charger arm
 * (MultiTileEntityChargingCraftingTable.java:46-58 verbatim shape).
 */
public class GT6CraftingTableMatrixTest extends GTOfflineTestBase {

	static BlockEntityType<TileEntityChargingCraftingTable> sChargingType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** The LU battery fixture (the GT6CrystalChargerTest registerItemFixture posture). */
	private static gregtech6.item.energy.GT6BatteryItem sBattery;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<TileEntityChargingCraftingTable>[] tHolder = (BlockEntityType<TileEntityChargingCraftingTable>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityChargingCraftingTable(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sChargingType = tHolder[0];
		sBattery = registerItemFixture("fixture_act_matrix_battery",
				() -> new gregtech6.item.energy.GT6BatteryItem(new net.minecraft.world.item.Item.Properties(), 32, 32 * 2000, gregapi.data.TD.Energy.LU));
	}

	// ---------------------------------------------------------------------------
	// the census + the verbatim columns (Loader :186-245 x :136-137)
	// ---------------------------------------------------------------------------

	@Test
	public void matrixCensusIsOneHundredTwentyRowsOverSixtyMaterials() {
		assertEquals(60, GTMachines.CRAFTING_TABLE_MATERIALS.size(), "the metalset loop :186-245 (the tail comment '60 is next')");
		assertEquals(120, GTMachines.CRAFTING_TABLE_ROWS.size(), "60 materials x the :136/:137 two-line family");
		// the upstream registration order: per material the plain line then the charging line
		for (int i = 0; i < 60; i++) {
			CraftingTableRow tPlain = GTMachines.CRAFTING_TABLE_ROWS.get(i * 2);
			CraftingTableRow tCharging = GTMachines.CRAFTING_TABLE_ROWS.get(i * 2 + 1);
			assertFalse(tPlain.charging());
			assertTrue(tCharging.charging());
			assertEquals(tPlain.material(), tCharging.material(), "the pair shares the metalset line");
		}
	}

	/**
	 * The full verbatim id column: plain 5000+aID, charging 5500+aID — the 60 (aID,
	 * slug, hardness) triples in loader line order :186-245 (the W line rides the ANY.W
	 * group representative MT.W, id 26).
	 */
	@Test
	public void idAndHardnessColumnsAreLoaderVerbatim() {
		String[][] tExpected = {
			{"lead", "0", "4.0"}, {"bismuth", "16", "4.0"}, {"antimony", "47", "4.0"}, {"nickel", "22", "4.0"},
			{"constantan", "37", "4.0"}, {"bronze", "9", "7.0"}, {"arsenic_copper", "57", "7.5"}, {"aluminium", "1", "2.0"},
			{"brass", "8", "2.5"}, {"tin_alloy", "5", "3.0"}, {"cobalt", "21", "4.0"}, {"ardite", "38", "2.0"},
			{"arsenic_bronze", "58", "8.0"}, {"bismuth_bronze", "56", "8.0"}, {"germanium", "23", "4.0"}, {"invar", "6", "4.0"},
			{"steel", "10", "6.0"}, {"hsla", "18", "6.0"}, {"gold", "2", "3.0"}, {"silver", "3", "3.0"},
			{"manganese", "46", "6.0"}, {"manyullyn", "39", "4.0"}, {"lumium", "54", "2.0"}, {"knightmetal", "25", "7.0"},
			{"galvanized_steel", "19", "6.0"}, {"meteorite", "43", "7.0"}, {"meteoric_steel", "24", "8.0"}, {"gilded_iron", "20", "6.0"},
			{"molybdenum", "49", "6.0"}, {"syrmorite", "44", "4.0"}, {"electrum", "7", "3.0"}, {"stainless_steel", "11", "5.0"},
			{"thaumium", "27", "9.0"}, {"manasteel", "40", "9.0"}, {"efrine", "53", "8.0"}, {"tungsten_alloy", "52", "8.0"},
			{"titanium", "12", "9.0"}, {"netherite", "51", "10.0"}, {"chromium", "13", "4.0"}, {"platinum", "4", "2.0"},
			{"octine", "45", "8.0"}, {"desh", "30", "15.0"}, {"terrasteel", "42", "15.0"}, {"tungstensteel", "14", "12.5"},
			{"tungsten_carbide", "17", "12.5"}, {"duranium_alloy", "31", "20.0"}, {"draconium", "35", "50.0"}, {"ultimet", "48", "12.5"},
			{"desh_alloy", "55", "15.0"}, {"tungsten", "26", "10.0"}, {"palladium", "59", "15.0"}, {"iridium", "15", "15.0"},
			{"osmium", "29", "9.0"}, {"void_metal", "28", "30.0"}, {"elementium", "41", "30.0"}, {"tritanium_alloy", "32", "30.0"},
			{"adamantium", "33", "100.0"}, {"bedrock_hsla_alloy", "34", "100.0"}, {"draconium_awakened", "36", "100.0"}, {"infinity", "50", "100.0"},
		};
		assertEquals(60, tExpected.length);
		for (int i = 0; i < 60; i++) {
			CraftingTableRow tPlain = GTMachines.CRAFTING_TABLE_ROWS.get(i * 2);
			CraftingTableRow tCharging = GTMachines.CRAFTING_TABLE_ROWS.get(i * 2 + 1);
			int tLoaderId = Integer.parseInt(tExpected[i][1]);
			float tHardness = Float.parseFloat(tExpected[i][2]);
			assertEquals("advanced_crafting_table_" + tExpected[i][0], tPlain.path(), "plain path, line " + i);
			assertEquals("charging_crafting_table_" + tExpected[i][0], tCharging.path(), "charging path, line " + i);
			assertEquals(5000 + tLoaderId, tPlain.metaId(), "plain id 5000+aID, line " + i);
			assertEquals(5500 + tLoaderId, tCharging.metaId(), "charging id 5500+aID, line " + i);
			assertEquals(tExpected[i][0], tPlain.material().slug());
			assertEquals(tHardness, tPlain.material().hardness(), "the shared aHardness(==aResistance) column, line " + i);
			// the slug joins the loader material (the switch is loud on drift — the ISE is the
			// unknown-slug face; the resolved mNameLocal face is the en datagen walk's)
			final CraftingTableRow tProbe = tPlain;
			org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> tProbe.material().mt(), "the MT face resolves, line " + i);
		}
		// the spot anchors (the first/last lines and the steel row the command re-points at)
		assertEquals("lead", GTMachines.CRAFTING_TABLE_MATERIALS.get(0).slug());
		assertEquals("infinity", GTMachines.CRAFTING_TABLE_MATERIALS.get(59).slug());
		assertEquals(10, GTMachines.CRAFTING_TABLE_MATERIALS.get(16).metaId());
		assertEquals("steel", GTMachines.CRAFTING_TABLE_MATERIALS.get(16).slug());
	}

	// ---------------------------------------------------------------------------
	// the registration completeness (paths + maps + the BET walk shape)
	// ---------------------------------------------------------------------------

	@Test
	public void registrationMapsCarryEveryRow() {
		assertEquals(120, GTMachines.CRAFTING_TABLE_BLOCKS_BY_PATH.size());
		assertEquals(120, GTMachines.CRAFTING_TABLE_ITEMS_BY_PATH.size());
		Set<String> tPaths = new LinkedHashSet<>();
		for (CraftingTableRow tRow : GTMachines.CRAFTING_TABLE_ROWS) {
			assertTrue(GTMachines.CRAFTING_TABLE_BLOCKS_BY_PATH.containsKey(tRow.path()), "block map: " + tRow.path());
			assertTrue(GTMachines.CRAFTING_TABLE_ITEMS_BY_PATH.containsKey(tRow.path()), "item map: " + tRow.path());
			tPaths.add(tRow.path());
		}
		assertEquals(120, tPaths.size(), "no path collisions");
		assertTrue(GTMachines.CRAFTING_TABLE_BLOCKS_BY_PATH.containsKey("advanced_crafting_table_stainless_steel"),
				"the dungeon 5011 row (metalset aID 11)");
		assertTrue(GTMachines.CRAFTING_TABLE_BLOCKS_BY_PATH.containsKey("advanced_crafting_table_steel"),
				"the /gt6act place anchor (metalset aID 10)");
		// the retired deviation-⑥ bare path stays retired
		assertFalse(GTMachines.CRAFTING_TABLE_BLOCKS_BY_PATH.containsKey("advanced_crafting_table"));
	}

	// ---------------------------------------------------------------------------
	// the charging arm (MultiTileEntityChargingCraftingTable :46-58)
	// ---------------------------------------------------------------------------

	@Test
	public void chargingTableInjectsOnePacketPerOccupiedToolSlot() {
		TileEntityChargingCraftingTable tTable = new TileEntityChargingCraftingTable(sChargingType, POS, Blocks.STONE.defaultBlockState());
		assertEquals("charging_crafting_table", tTable.getTileEntityName());
		// empty tools: the walk returns 0
		assertEquals(0, tTable.doInject(gregapi.data.TD.Energy.LU, (byte) 6, 32, 10, true));
		// a non-energy stack in a tool slot does not block the walk nor inject
		tTable.getInventory().setStackInSlot(16, new ItemStack(Items.STICK));
		assertEquals(0, tTable.doInject(gregapi.data.TD.Energy.LU, (byte) 6, 32, 10, true));
		// one LU battery in the tool belt: ONE packet of 32 EU-LU per call, the battery charges
		ItemStack tBattery = new ItemStack(sBattery, 1);
		tTable.getInventory().setStackInSlot(16, tBattery);
		// the return counts PACKETS (upstream rReturn += inject(...): one packet per slot),
		// the stored energy grows by packet x size (32 EU-LU per packet of 32)
		assertEquals(1, tTable.doInject(gregapi.data.TD.Energy.LU, (byte) 6, 32, 10, true),
				"one packet consumed for the one occupied tool slot");
		assertEquals(32, gregtech6.item.energy.GT6BatteryItem.readStoredRaw(tTable.getInventory().getStackInSlot(16)),
				"the tool slot battery charged by one packet of 32");
		// a second packet rides the next call (upstream one-packet-per-slot-per-inject)
		assertEquals(1, tTable.doInject(gregapi.data.TD.Energy.LU, (byte) 6, 32, 10, true));
		assertEquals(64, gregtech6.item.energy.GT6BatteryItem.readStoredRaw(tTable.getInventory().getStackInSlot(16)));
	}

	@Test
	public void chargingEnergyBandIsUpstreamVerbatim() {
		TileEntityChargingCraftingTable tTable = new TileEntityChargingCraftingTable(sChargingType, POS, Blocks.STONE.defaultBlockState());
		// :54-58 — accept-only (never emitting), every type, band min 1 / max MAX (the Root
		// defaults would overflow on Rec=MAX: Min=Rec/2, Max=Rec*2)
		assertTrue(tTable.isEnergyType(gregapi.data.TD.Energy.LU, (byte) 6, false));
		assertFalse(tTable.isEnergyType(gregapi.data.TD.Energy.LU, (byte) 6, true));
		assertTrue(tTable.getEnergyTypes((byte) 6).contains(gregapi.data.TD.Energy.LU));
		assertTrue(tTable.getEnergyTypes((byte) 6).contains(gregapi.data.TD.Energy.EU));
		assertEquals(1, tTable.getEnergySizeInputMin(gregapi.data.TD.Energy.LU, (byte) 6));
		assertEquals(Long.MAX_VALUE, tTable.getEnergySizeInputMax(gregapi.data.TD.Energy.LU, (byte) 6));
		assertEquals(Long.MAX_VALUE, tTable.getEnergySizeInputRecommended(gregapi.data.TD.Energy.LU, (byte) 6));
	}
}
