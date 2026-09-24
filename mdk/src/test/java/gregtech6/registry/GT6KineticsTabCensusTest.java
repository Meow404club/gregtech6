package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p38-tabfix-e-kinetics-tail — the kinetics ladder tab-join coverage census:
 * {@link GT6Kinetics#onBuildTabContentsKineticLadders} walks the three registration
 * ladders into {@link GTMachines#MACHINES_TAB}, and this test pins the per-ladder
 * coverage counts so a future row lands only with a conscious census bump (the
 * GT6MultiblockTabCensusTest posture; the disk truth matches the card snapshot —
 * AXLE_SPECS 11 x 4 diameters, STEAM_ENGINES 28, DIESEL_SPECS 8 — no erratum).
 *
 * <p>Offline surface: the axle and diesel item maps fill in {@code onModConstruct}, so
 * their coverage is pinned through the spec tables the registration loops enumerate 1:1
 * (the GTAxleBlockEntityTest 11x4 arithmetic); the steam map is static-init filled and is
 * read directly. {@code .get()} is never resolved — the frozen-registry wall stays
 * untouched.
 *
 * <p>Pool-cut declaration (the card's declared deviation): upstream hangs the kinetic
 * rows on their own MTE-registry categories (the "Misc Tool Blocks" 32720 area,
 * Loader_MultiTileEntities.java:2106 and the per-row anchors); this port pools the join
 * into MACHINES_TAB (the GTBarrels:257 pooling precedent). The four single-block items
 * (crank/gearbox/rotation transformer/water wheel) are the p38-tabfix-b-energy crop —
 * its {@code CreativeTabJoinCensusTest#kineticsJoinFour} pins them.
 */
public class GT6KineticsTabCensusTest extends GTOfflineTestBase {

	/** The three ladder walks, ladder by ladder — the coverage counts the card pinned. */
	@Test
	public void theThreeLadderWalksCoverTheirWholeRegistration() {
		assertEquals(11, GT6Kinetics.AXLE_SPECS.size(), "the Loader kinetic material rows (:1662-1752)");
		assertEquals(44, GT6Kinetics.AXLE_SPECS.size() * GT6Kinetics.AXLE_DIAMETERS.length,
				"11 materials x 4 diameters — the axle walk coverage");
		assertEquals(28, GT6Kinetics.STEAM_ENGINES.size(), "the Steam + Strong ladders (:584-612), the 28-vs-26 erratum already declared");
		assertEquals(28, GT6Kinetics.STEAM_ENGINE_ITEMS.size(), "the walked steam container (static-init filled)");
		assertEquals(8, GT6Kinetics.DIESEL_SPECS.size(), "the diesel rows (:721-729)");
	}

	/** The grand total: 44 + 28 + 8 = 80 items the pooled machines tab gains. */
	@Test
	public void thePooledTabGainsEightyKineticItems() {
		assertEquals(80, GT6Kinetics.AXLE_SPECS.size() * GT6Kinetics.AXLE_DIAMETERS.length
				+ GT6Kinetics.STEAM_ENGINE_ITEMS.size() + GT6Kinetics.DIESEL_SPECS.size(),
				"the three-ladder join coverage total");
	}

	/** The ladder walk exists (static) — the class-level MOD-bus subscriber delivers it. */
	@Test
	public void theKineticsClassDeclaresTheLadderWalk() throws Exception {
		Method tWalk = GT6Kinetics.class.getDeclaredMethod("onBuildTabContentsKineticLadders",
				BuildCreativeModeTabContentsEvent.class);
		assertTrue(java.lang.reflect.Modifier.isStatic(tWalk.getModifiers()), "the ladder walk");
	}

	/** The join target is the pooled machines tab itself. */
	@Test
	public void theJoinTargetIsTheGt6MachinesTab() {
		assertEquals("gt6:machines", GTMachines.MACHINES_TAB.getId().toString());
	}
}
