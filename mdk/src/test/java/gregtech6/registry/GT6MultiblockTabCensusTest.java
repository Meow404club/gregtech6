package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task tabfix-a-multiblock — the multiblock-family tab-join coverage census, reshaped by
 * task small-crucible-boiler-tab-rehome: the LARGE multiblock families walk their item
 * map(s) into {@link GTMultiBlocks#MULTIBLOCKS_TAB} (gt6:multiblocks) and THIS test pins
 * the walked map sizes one family at a time, so a future row lands only with a conscious
 * census bump (the BurningBoxRowTableTest row-count posture; sizes read the
 * DeferredRegister-held maps without resolving {@code .get()} — the frozen-registry wall
 * stays untouched, the GT6ToolsCreativeTabTest form).
 *
 * <p><b>THE REHOME (task small-crucible-boiler-tab-rehome, the user ruling)</b>: the two
 * single-block families tabfix-a pooled into gt6:multiblocks left the pool — the Smeltery
 * single-block family (upstream own tab aCreativeTabID 1022 "Smelting Crucibles",
 * Loader_MultiTileEntities.java:251-292 — the crucible face of the rehome landed with
 * material-mc-c-crucible-rows along THIS ruling, the FULL 39-rung ladder since) and the 26
 * Steam Boiler Tanks (upstream tab 1204 "Steam Boilers", :553-579 — the boiler face is THIS
 * card) ride {@link GTMachines#MACHINES_TAB} (gt6:machines) now. The tabID is the 4th
 * {@code aRegistry.add} argument (MultiTileEntityRegistry.java:148); every LARGE row keeps
 * 17101 "Multiblock Machines" (walls :1143-1153, large boilers :1248-1252, turbines
 * :1254-1257/:1264-1267, dynamos :1259-1262, HEX :1245, distillation :1226-1227, valves
 * :1195-1222, crucible controllers :1270-1277) — the rehome leaves zero single-block strays
 * in the pool (the per-row side-sweep over all seven joined families).
 *
 * <p><b>COUNT ERRATUM (declared)</b>: the task card / tasks.p38-tab-census says
 * "GT6Boilers (3) ... = 62", but the card's own evidence line (GT6Boilers.java:155-157)
 * sampled only the first three rows of BOILER_ROWS; the file registers the FULL ladder
 * 13 + 13 = 26 (Loader_MultiTileEntities.java:553-565 / :567-579 — the no-gaps
 * declaration in the GT6Boilers class doc), the BurningBoxRowTableTest 27-vs-26
 * precedent. The coordinator ruling (2026-09-24): pin the disk truth. Tabfix-a pinned
 * 8 + 4 + 25 + 2 + 1 + 20 + 26 = 86 pooled (the 20 grew from 19 when the smeltery_ceramic
 * rung joined with issue45-c2, then to 121 when the FULL :251-292 Smeltery ladder landed
 * with material-mc-c-crucible-rows — the hidden mod-stone rows still ride the registration
 * walk, the tab arm skips them at build time); since the rehome the multiblocks pool is 56
 * and the rehomed machines pool is 65 (39 + 26) — 121 across the two tabs.
 */
public class GT6MultiblockTabCensusTest extends GTOfflineTestBase {

	/** The multiblocks-tab join walks, family by family — the LARGE families only, post-rehome. */
	@Test
	public void theMultiblockJoinWalksCoverTheirWholeRegistry() {
		assertEquals(8, GT6Turbines.ITEMS_BY_PATH.size(), "4 steam (:1254-1257) + 4 gas (:1264-1267)");
		assertEquals(4, GT6DynamoHousings.ITEMS_BY_PATH.size(), ":1259-1262");
		assertEquals(25, GT6Tanks.ITEMS_BY_PATH.size(), "the 25 valves (:1195-1222, the census-26 correction)");
		assertEquals(2, GT6Distillation.TOWER_ITEMS_BY_PATH.size(), "HU :1226 + CU :1227");
		assertNotNull(GT6HeatExchangers.HEAT_EXCHANGER_ITEM, "the single HEX item (:1245) — its join is the one-item walk");
		assertEquals(39, GT6Crucibles.ITEMS_BY_PATH.size(), "the FULL :251-292 Smeltery ladder (task material-mc-c-crucible-rows; the 4 grew from the 3 prior stones/metals + the smeltery_ceramic row, upstream :256 ID 1005, the issue45-c2 clay crucible card) — REHOMED to gt6:machines by small-crucible-boiler-tab-rehome, the map rides the MACHINES arm now");
		assertEquals(8, GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.size(), "the :1270-1277 controller ladder (tab 17101)");
		assertEquals(7, GT6Crucibles.CRUCIBLE_WALL_ITEMS_BY_PATH.size(), "the dedicated ladder walls (tab 17101, the part rows :1143-1153)");
		assertNotNull(GT6Crucibles.CRUCIBLE_STEEL_WALL_ITEM, "the single-rung wall (18009) — the one-item join arm");
	}

	/** The two REHOMED single-block families: their walked maps ride gt6:machines now (small-crucible-boiler-tab-rehome). */
	@Test
	public void theRehomedSingleBlockFamiliesRideTheMachinesTab() {
		assertEquals("gt6:machines", GTMachines.MACHINES_TAB.getId().toString(), "the rehome join target");
		assertEquals(39, GT6Crucibles.ITEMS_BY_PATH.size(), "the Smeltery single-block family, the FULL :251-292 ladder — upstream own tab 1022 'Smelting Crucibles'; the crucible face of the rehome landed with material-mc-c-crucible-rows along this ruling (the hidden mod-stone rows stay out of the tab arm at build time)");
		assertEquals(26, GT6Boilers.ITEMS_BY_PATH.size(), "13 (:553-565) + 13 Strong (:567-579) — upstream own tab 1204 'Steam Boilers'; the card's '3' sampled only :155-157 (declared erratum)");
	}

	/** Both rehomed families keep their delivered tab-walk handler (the CreativeTabJoinCensusTest presence form). */
	@Test
	public void theRehomedFamiliesKeepTheTabWalkHandler() throws Exception {
		for (Class<?> tFamily : List.of(GT6Crucibles.class, GT6Boilers.class)) {
			Method tWalk = tFamily.getDeclaredMethod("onBuildTabContents", BuildCreativeModeTabContentsEvent.class);
			assertTrue(java.lang.reflect.Modifier.isStatic(tWalk.getModifiers()), tFamily.getSimpleName() + " walk");
		}
	}

	/** The grand totals: the multiblocks pool holds 56 (8+4+25+2+1+16), the rehomed machines pool holds 65 (39+26) — the 121 grand total, split across the two tabs. */
	@Test
	public void thePoolsHold56Plus65Rehomed() {
		int tMultiblocks = GT6Turbines.ITEMS_BY_PATH.size() + GT6DynamoHousings.ITEMS_BY_PATH.size()
				+ GT6Tanks.ITEMS_BY_PATH.size() + GT6Distillation.TOWER_ITEMS_BY_PATH.size()
				+ 1 + (GT6Crucibles.CRUCIBLE_ITEMS_BY_PATH.size() + 1 + GT6Crucibles.CRUCIBLE_WALL_ITEMS_BY_PATH.size());
		int tRehomed = GT6Crucibles.ITEMS_BY_PATH.size() + GT6Boilers.ITEMS_BY_PATH.size();
		assertEquals(56, tMultiblocks, "the post-rehome multiblocks pool (the LARGE families only)");
		assertEquals(65, tRehomed, "the rehomed machines pool (the single-block Smeltery ladder 39 + the Steam Boiler Tanks 26)");
		assertEquals(121, tMultiblocks + tRehomed, "the tabfix-a census total, now split across the two tabs");
	}

	/** The join target is the pooled multiblocks tab itself (the LARGE families only, post-rehome). */
	@Test
	public void theJoinTargetIsTheGt6MultiblocksTab() {
		assertEquals("gt6:multiblocks", GTMultiBlocks.MULTIBLOCKS_TAB.getId().toString());
	}
}
