package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p38-tabfix-c-misc — the four misc-family tab-coverage pools pinned at the
 * disk-read truth (the a-card erratum lesson: read the disk BEFORE pinning numbers).
 * Each group's join handler walks its pool WHOLESALE (the GT6BurningBoxes
 * .onBuildTabContents verbatim form — no filter), so pool count = join count by
 * construction; the offline JVM cannot execute the event handlers themselves, exactly
 * the GTWiresCreativeTabTest discipline (RegistryObject.getId without resolving get()).
 *
 * <p><b>COUNT ERRATUM (declared, the BurningBoxRowTableTest form)</b>: the census card
 * (tasks.p38-tab-census) says "SlicerBlades 3" and "Molds 34 finished / 32 raw":
 * <ul>
 * <li>GT6SlicerBlades — the disk truth is EIGHT: the census's "3" (fields :71/:78/:86)
 *     predates the p36-recipes-obtainability completion that registered the full
 *     eight-item crafting census ({@link GT6SlicerBlades#ALL}); the join covers all 8.</li>
 * <li>GT6Molds — the census's "34 finished / 32 raw" spans BOTH families of the file:
 *     32 molds (1 stone :347 + 1 blank :352 + 30 carved :391-420) + 2 faucets
 *     (:300/:305) = 34 finished; 31 mold raws + 1 faucet raw = 32 raw. The join pool
 *     cut rides the finished/raw axis (raws stay OUT — craft-only upstream,
 *     MultiItemRandomTools.java:127+), not a family axis.</li>
 * </ul>
 * GT6FoamBlocks (the 2 dried BlockItems) and GT6BeeHives (the Bumbliary pair beside
 * the pre-existing p34 hive row) match the census. GT6LubricantBucket: the census
 * rules it OUT of any tab (its own javadoc declares the creative-tab face CUT) —
 * zero code, nothing pinned here.
 */
public class GT6MiscTabCoverageCensusTest extends GTOfflineTestBase {

	/** The mold family: join pool 34 = 32 molds + 2 faucets; the raw pool 32 stays out. */
	@Test
	public void moldJoinPoolIs34AndTheRawPool32StaysOut() {
		assertEquals(32, GT6Molds.ITEMS_BY_PATH.size(), "1 stone (:347) + 1 blank (:352) + 30 pre-carved (:391-420)");
		assertEquals("mold_stone", GT6Molds.ITEMS_BY_PATH.keySet().iterator().next(), "the stone rung leads (the card-A row0)");
		assertTrue(GT6Molds.ITEMS_BY_PATH.containsKey("mold_ceramic"), "the carvable blank (:352)");
		assertTrue(GT6Molds.ITEMS_BY_PATH.containsKey("mold_ceramic_nugget"), "the nugget fallback row (:420)");
		assertEquals(2, GT6Molds.FAUCET_ITEMS_BY_PATH.size(), ":300 stone / :305 ceramic");
		assertTrue(GT6Molds.FAUCET_ITEMS_BY_PATH.containsKey("faucet_stone"));
		assertTrue(GT6Molds.FAUCET_ITEMS_BY_PATH.containsKey("faucet_ceramic"));
		assertEquals(34, GT6Molds.ITEMS_BY_PATH.size() + GT6Molds.FAUCET_ITEMS_BY_PATH.size(),
				"the join pool — the census's '34 finished' spans both maps of the file");
		assertEquals(32, GT6Molds.RAW_ITEMS_BY_PATH.size() + 1,
				"the raw pool stays OUT of the tab — 31 mold raws + FAUCET_CERAMIC_RAW");
		assertNotNull(GT6Molds.FAUCET_CERAMIC_RAW, "the :305 faucet raw rides the raw pool");
		// the split invariant: every raw item id is a formed id + "_raw", and no raw id
		// can leak into the finished join pool
		Set<String> tJoinIds = new LinkedHashSet<>();
		for (String tPath : GT6Molds.ITEMS_BY_PATH.keySet()) tJoinIds.add(tPath);
		for (String tPath : GT6Molds.FAUCET_ITEMS_BY_PATH.keySet()) tJoinIds.add(tPath);
		for (String tPath : GT6Molds.RAW_ITEMS_BY_PATH.keySet()) {
			assertFalse(tJoinIds.contains(tPath + "_raw"), "raw id " + tPath + "_raw must not ride the finished pool");
		}
	}

	/** The slicer-blade family: the join pool is the FULL eight-item census (erratum 3→8). */
	@Test
	public void slicerBladeJoinPoolIsTheFullEightItemCensus() {
		assertEquals(8, GT6SlicerBlades.ALL.size(), "the p36 completion — the census's '3' predates it (declared erratum)");
		assertEquals(2, GT6SlicerBlades.BLADES.size(), "the RM row0 pair stays a subset of ALL");
		assertEquals("shape_slicer_empty", GT6SlicerBlades.ALL.get(0).getId().getPath(), "the frame leads (upstream meta :362)");
		assertEquals("shape_slicer_quarters_hollow", GT6SlicerBlades.ALL.get(7).getId().getPath(), "the quarters-hollow closes (upstream meta :372)");
	}

	/** The C-Foam family: the join pool is exactly the two dried BlockItems. */
	@Test
	public void foamBlockJoinPoolIsTheTwoDriedBlockItems() {
		assertEquals(2, GT6FoamBlocks.ITEMS.getEntries().size(),
				"exactly the two dried BlockItems — the fresh blocks carry none (spray-only), the owned is showInCreative false");
		assertEquals(List.of("cfoam", "cfoam_slab"),
				GT6FoamBlocks.ITEMS.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the dried full block then the dried slab");
	}

	/** The bee family: the gt6:bee display walk = the 20 combs + the three hive-family items. */
	@Test
	public void theBeeTabWalkCoversTheCombsPlusTheThreeHiveFamilyItems() {
		assertEquals(20, GT6BeeCombs.COMBS.size(), "the 20-comb static chain (p31)");
		assertEquals(3, GT6BeeHives.ITEMS.getEntries().size(),
				"the hive BlockItem (p34, already on the walk) + the Bumbliary pair (this card)");
		assertEquals(List.of("bumble_hive", "bumbliary", "bumbliary_advanced"),
				GT6BeeHives.ITEMS.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the walk appends the pair after the p34 hive row");
	}
}
