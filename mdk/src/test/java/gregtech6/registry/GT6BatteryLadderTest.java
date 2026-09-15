package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The battery LADDER census (task p29-w4-battery-storage acceptance ① and ⑤-half): the 37
 * capacity-ladder rows VERBATIM (every literal pinned independently — the card-① density
 * lesson: the registry table and this test must disagree loudly when a transcription slips,
 * so the expected values here are typed out, never derived from V[i]×mult), the type
 * domain split (25 EU + 12 LU), the tag-seam mapping (the re_battery0..4 / re_crystal0..5
 * translations of the upstream gt:re-battery/gt:re-crystal oredicts — the W5 capacity-sum
 * consumption face), the 7 circuit carriers (the OD_CIRCUITS[0..6] closure), the 12
 * BatteryBox rows (the V[0..5] tier closure of the upstream 20), and the wiring-surface
 * census (every GT6Batteries *_BE field — the GT6CapabilityWiringSeamTest kitchen shape).
 */
public class GT6BatteryLadderTest extends GTOfflineTestBase {

	@BeforeAll
	static void warmUp() {
		// the TD class-init warm-up (the W2 CME lesson: resolveEnergyType("TU") mints the
		// TagData population before any row lambda resolves)
		gregtech6.tileentity.energy.GTEnergySourceBlockEntity.resolveEnergyType("TU");
	}

	/** The literal expected ladder — 37 rows of (path, metaId, sizeRec, capacity), the Loader :1009-:1092 columns. */
	private static final Object[][] EXPECTED = {
			{"battery_lead_acid_ulv", 14000, 8L, 16000L},
			{"battery_lead_acid_lv", 14001, 32L, 64000L},
			{"battery_lead_acid_mv", 14002, 128L, 256000L},
			{"battery_lead_acid_hv", 14003, 512L, 1024000L},
			{"battery_lead_acid_ev", 14004, 2048L, 4096000L},
			{"battery_alkaline_ulv", 14010, 8L, 32000L},
			{"battery_alkaline_lv", 14011, 32L, 128000L},
			{"battery_alkaline_mv", 14012, 128L, 512000L},
			{"battery_alkaline_hv", 14013, 512L, 2048000L},
			{"battery_alkaline_ev", 14014, 2048L, 8192000L},
			{"battery_nicd_ulv", 14020, 8L, 32000L},
			{"battery_nicd_lv", 14021, 32L, 128000L},
			{"battery_nicd_mv", 14022, 128L, 512000L},
			{"battery_nicd_hv", 14023, 512L, 2048000L},
			{"battery_nicd_ev", 14024, 2048L, 8192000L},
			{"battery_licoo2_ulv", 14030, 8L, 512000L},
			{"battery_licoo2_lv", 14031, 32L, 2048000L},
			{"battery_licoo2_mv", 14032, 128L, 8192000L},
			{"battery_licoo2_hv", 14033, 512L, 32768000L},
			{"battery_licoo2_ev", 14034, 2048L, 131072000L},
			{"battery_limn_ulv", 14040, 8L, 1024000L},
			{"battery_limn_lv", 14041, 32L, 4096000L},
			{"battery_limn_mv", 14042, 128L, 16384000L},
			{"battery_limn_hv", 14043, 512L, 65536000L},
			{"battery_limn_ev", 14044, 2048L, 262144000L},
			{"energium_red_ulv", 14500, 8L, 3200000L},
			{"energium_red_lv", 14501, 32L, 12800000L},
			{"energium_red_mv", 14502, 128L, 51200000L},
			{"energium_red_hv", 14503, 512L, 204800000L},
			{"energium_red_ev", 14504, 2048L, 819200000L},
			{"energium_red_iv", 14505, 8192L, 3276800000L},
			{"energium_cyan_ulv", 14510, 8L, 6400000L},
			{"energium_cyan_lv", 14511, 32L, 25600000L},
			{"energium_cyan_mv", 14512, 128L, 102400000L},
			{"energium_cyan_hv", 14513, 512L, 409600000L},
			{"energium_cyan_ev", 14514, 2048L, 1638400000L},
			{"energium_cyan_iv", 14515, 8192L, 6553600000L},
	};

	@Test
	public void the37CapacityLadderRowsTranscribeVerbatim() {
		assertEquals(37, GT6Batteries.ROWS.size(), "the 37-item ladder (5+5+5+5+5+6+6 over 16 upstream MTE classes)");
		for (int i = 0; i < EXPECTED.length; i++) {
			GT6Batteries.BatteryRow tRow = GT6Batteries.ROWS.get(i);
			assertEquals(EXPECTED[i][0], tRow.path(), "row " + i + " path");
			assertEquals(EXPECTED[i][1], tRow.metaId(), "row " + i + " meta parity column");
			assertEquals(EXPECTED[i][2], tRow.sizeRec(), "row " + i + " packet size (the NBT_INPUT column, V[tier])");
			assertEquals(EXPECTED[i][3], tRow.capacity(), "row " + i + " capacity (the NBT_CAPACITY column, the LITERAL V×mult)");
		}
	}

	@Test
	public void theTypeDomainSplits25EuAnd12Lu() {
		int tEu = 0, tLu = 0;
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.type().get() == TD.Energy.EU) tEu++;
			else if (tRow.type().get() == TD.Energy.LU) tLu++;
		}
		assertEquals(25, tEu, "the five EU families (LeadAcid/Alkaline/NiCd/LiCoO2/LiMn × V[0..4])");
		assertEquals(12, tLu, "the two LU crystal families (Red/Cyan × V[0..5])");
	}

	@Test
	public void theTagSeamsMapTheOredictLadder() {
		// the W5 capacity-sum face iterates gt:re-battery<i>/gt:re-crystal<i>; every battery
		// row must join exactly the tag its upstream oredict tail column carries
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.type().get() == TD.Energy.EU) {
				assertEquals("re_battery" + tRow.tier(), tRow.tagPath(), tRow.path() + " rides re_battery<tier>");
			} else {
				assertEquals("re_crystal" + tRow.tier(), tRow.tagPath(), tRow.path() + " rides re_crystal<tier>");
			}
		}
		Set<String> tTags = new LinkedHashSet<>();
		GT6Batteries.ROWS.forEach(tRow -> tTags.add(tRow.tagPath()));
		assertEquals(new LinkedHashSet<>(java.util.List.of("re_battery0", "re_battery1", "re_battery2", "re_battery3", "re_battery4",
				"re_crystal0", "re_crystal1", "re_crystal2", "re_crystal3", "re_crystal4", "re_crystal5")),
				tTags, "the W5 consumption face: re-battery0-4 + re-crystal0-5");
	}

	@Test
	public void theCircuitCarriersCloseTheOdCircuitsLadder() {
		assertEquals(7, GT6Batteries.CIRCUIT_ROWS.size(), "OD_CIRCUITS[0..6]: Primitive..Ultimate");
		for (int i = 0; i < GT6Batteries.CIRCUIT_ROWS.size(); i++) {
			GT6Batteries.CircuitRow tRow = GT6Batteries.CIRCUIT_ROWS.get(i);
			assertEquals(i, tRow.tier());
			assertEquals("circuit" + i, tRow.tagPath(), "the upstream oredict name byte-kept (CS.java:166)");
		}
		// the mapping face: standard families ride [2]/[3]/[4] from MV, the advanced ladder [tier+2]
		assertEquals(2, GT6Batteries.batteryCircuitTier(GT6Batteries.ROWS.get(2)), "lead_acid_mv -> circuit2");
		assertEquals(-1, GT6Batteries.batteryCircuitTier(GT6Batteries.ROWS.get(0)), "lead_acid_ulv -> no circuit");
		assertEquals(2, GT6Batteries.batteryCircuitTier(GT6Batteries.ROWS.get(15)), "licoo2_ulv -> circuit2 (the shifted advanced ladder)");
		assertEquals(6, GT6Batteries.batteryCircuitTier(GT6Batteries.ROWS.get(19)), "licoo2_ev -> circuit6 (the Ultimate carrier)");
	}

	@Test
	public void theBoxRowsCloseAtThePortTiers() {
		assertEquals(12, GT6Batteries.BOX_ROWS.size(), "6 tiers × 2 sizes (the V[0..5] closure of the upstream 20)");
		for (int i = 0; i < 6; i++) {
			GT6Batteries.BoxRow tSmall = GT6Batteries.BOX_ROWS.get(i);
			GT6Batteries.BoxRow tLarge = GT6Batteries.BOX_ROWS.get(6 + i);
			assertEquals(10080 + i, tSmall.metaId(), "the :894 meta ladder");
			assertEquals(10090 + i, tLarge.metaId(), "the :895 meta ladder");
			assertEquals(4, tSmall.slots(), "NBT_INV_SIZE 4");
			assertEquals(16, tLarge.slots(), "NBT_INV_SIZE 16");
		}
		assertTrue(GT6Batteries.BOX_PATTERN[0].equals("WCW") && GT6Batteries.BOX_PATTERN[1].equals("WCW")
				&& GT6Batteries.BOX_PATTERN[2].equals("XMX"), "the :894-:895 recipe strings verbatim");
	}

	@Test
	public void everyFamilyCellPathResolvesToACellRow() {
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (tRow.family().startsWith("energium")) continue; // no cell, no recipe
			String tCellPath = GT6Batteries.batteryCellPath(tRow);
			assertTrue(GT6Batteries.CELL_ITEMS.containsKey(tCellPath), "the 'B' column must resolve: " + tCellPath);
		}
		assertEquals(5, GT6Batteries.CELL_ITEMS.size(), "the five Filled cells (MultiItemTechnological 20001..20009 odd)");
	}

	@Test
	public void theWiringSurfaceCensusPinsTheTwoBoxBets() throws Exception {
		// the GT6CapabilityWiringSeamTest kitchen shape: every GT6Batteries *_BE field must
		// keep its row in the 21.1-only wiring file (this leg pins the census so a new family
		// BET forces the same-change wiring declaration)
		Set<String> tLive = new LinkedHashSet<>();
		for (java.lang.reflect.Field tField : GT6Batteries.class.getDeclaredFields()) {
			if (!tField.getName().endsWith("_BE")) continue;
			Object tHolder = tField.get(null);
			Object tId = tHolder.getClass().getMethod("getId").invoke(tHolder);
			tLive.add((String) tId.getClass().getMethod("getPath").invoke(tId));
		}
		assertEquals(new LinkedHashSet<>(java.util.List.of("battery_box", "battery_box_large")), tLive,
				"the battery BET census drifted — declare the new family's ItemHandler rows in "
						+ "GT6CapabilityWiring.registerBatteryBoxFamily in the same change");
	}
}
