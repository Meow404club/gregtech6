package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p38-tabfix-a-multiblock — the seven multiblock-family tab-join coverage census:
 * every family's {@code onBuildTabContents} walks its item map(s) into
 * {@link GTMultiBlocks#MULTIBLOCKS_TAB} (gt6:multiblocks), and THIS test pins the walked
 * map sizes one family at a time, so a future row lands only with a conscious census
 * bump (the BurningBoxRowTableTest row-count posture; sizes read the
 * DeferredRegister-held maps without resolving {@code .get()} — the frozen-registry wall
 * stays untouched, the GT6ToolsCreativeTabTest form).
 *
 * <p><b>COUNT ERRATUM (declared)</b>: the task card / tasks.p38-tab-census says
 * "GT6Boilers (3) ... = 62", but the card's own evidence line (GT6Boilers.java:155-157)
 * sampled only the first three rows of BOILER_ROWS; the file registers the FULL ladder
 * 13 + 13 = 26 (Loader_MultiTileEntities.java:553-565 / :567-579 — the no-gaps
 * declaration in the GT6Boilers class doc), the BurningBoxRowTableTest 27-vs-26
 * precedent. The coordinator ruling (2026-09-24): pin the disk truth. The tests pin
 * 8 + 4 + 25 + 2 + 1 + 19 + 26 = 85.
 *
 * <p>Pool-cut declaration (the census ruling): upstream every family rode the per-family
 * "Multiblock Machines" creative tab (aCreativeTabID 17101, Loader_MultiTileEntities
 * .java:1195-1283); the port pools them into the single gt6:multiblocks tab.
 */
public class GT6MultiblockTabCensusTest extends GTOfflineTestBase {

	/** The seven join walks, family by family — the coverage counts the card pinned. */
	@Test
	public void theSevenFamilyJoinWalksCoverTheirWholeRegistry() {
		assertEquals(8, GT6Turbines.ITEMS_BY_PATH.size(), "4 steam (:1254-1257) + 4 gas (:1264-1267)");
		assertEquals(4, GT6DynamoHousings.ITEMS_BY_PATH.size(), ":1259-1262");
		assertEquals(25, GT6Tanks.ITEMS_BY_PATH.size(), "the 25 valves (:1195-1222, the census-26 correction)");
		assertEquals(2, GT6Distillation.TOWER_ITEMS_BY_PATH.size(), "HU :1226 + CU :1227");
		assertNotNull(GT6HeatExchangers.HEAT_EXCHANGER_ITEM, "the single HEX item (:1245) — its join is the one-item walk");
		assertEquals(3, GT6Crucibles.ITEMS_BY_PATH.size(), "the Smeltery rungs");
		assertEquals(8, GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.size(), "the :1270-1277 controller ladder");
		assertEquals(7, GT6Crucibles.CRUCIBLE_WALL_ITEMS_BY_PATH.size(), "the dedicated ladder walls");
		assertNotNull(GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM, "the single-rung wall (18009) — the one-item join arm");
		assertEquals(26, GT6Boilers.ITEMS_BY_PATH.size(), "13 (:553-565) + 13 Strong (:567-579) — the card's '3' sampled only :155-157 (declared erratum)");
	}

	/** The grand total: 8 + 4 + 25 + 2 + 1 + 19 + 26 = 85 (the 62 erratum above). */
	@Test
	public void thePooledTabGains85MultiblockItems() {
		assertEquals(85, GT6Turbines.ITEMS_BY_PATH.size() + GT6DynamoHousings.ITEMS_BY_PATH.size()
				+ GT6Tanks.ITEMS_BY_PATH.size() + GT6Distillation.TOWER_ITEMS_BY_PATH.size()
				+ 1 + (GT6Crucibles.ITEMS_BY_PATH.size() + 1 + GT6Crucibles.CRUCIBLE_WALL_ITEMS_BY_PATH.size()
						+ GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.size())
				+ GT6Boilers.ITEMS_BY_PATH.size(), "the seven-family join coverage total");
	}

	/** The join target is the pooled multiblocks tab itself (not the machines tab). */
	@Test
	public void theJoinTargetIsTheGt6MultiblocksTab() {
		assertEquals("gt6:multiblocks", GTMultiBlocks.MULTIBLOCKS_TAB.getId().toString());
	}
}
