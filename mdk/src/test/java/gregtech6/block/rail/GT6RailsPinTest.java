package gregtech6.block.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.registry.GT6Rails;
import gregtech6.tileentity.GTOfflineTestBase;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;

/**
 * The rail family pin tests (task p35-rails-31-blocks, ACCEPTANCE ①) — the ROWS 钉测 over
 * the upstream anchor (Loader_Rails.java:39-72 read line by line) plus the speed-ladder
 * pure seam ({@link GT6Rails#ladderSpeed(float, boolean, boolean)}, the BlockBaseRail.java
 * :278-289 direct translation) pinned per material. The vanilla-root shape assertions are
 * the GTCrowbarItem-compat regression face: the crowbar's rails mining arm is a
 * {@code instanceof BaseRailBlock} test (GTCrowbarItem.mines :267-270), so every port class
 * assignable to {@link BaseRailBlock} inherits the arm — proven offline by assignability +
 * the vanilla-rail truth table (the mod-Block instances cannot exist in the frozen offline
 * JVM, the GTOfflineTestBase wall).
 */
public class GT6RailsPinTest extends GTOfflineTestBase {

	/** The upstream anchor: (slug, kind, speed, resistance) in the Loader :41-72 line order. */
	private static final Object[][] ANCHOR = {
			{"aluminium", "NORMAL", 0.20F, 6.0F},
			{"bronze", "NORMAL", 0.30F, 8.0F},
			{"magnalium", "NORMAL", 0.60F, 12.0F},
			{"steel", "NORMAL", 0.60F, 12.0F},
			{"stainlesssteel", "NORMAL", 0.80F, 10.0F},
			{"tungsten", "NORMAL", 1.00F, 20.0F},
			{"titanium", "NORMAL", 1.20F, 16.0F},
			{"tungstensteel", "NORMAL", 1.40F, 20.0F},
			{"tungstencarbide", "NORMAL", 1.60F, 24.0F},
			{"adamantium", "NORMAL", 4.00F, 100.0F},
			// the booster walk :52-61 — the boolean pair (T, F) and the SAME speed/resistance columns
			{"aluminium", "BOOSTER", 0.20F, 6.0F},
			{"bronze", "BOOSTER", 0.30F, 8.0F},
			{"magnalium", "BOOSTER", 0.60F, 12.0F},
			{"steel", "BOOSTER", 0.60F, 12.0F},
			{"stainlesssteel", "BOOSTER", 0.80F, 10.0F},
			{"tungsten", "BOOSTER", 1.00F, 20.0F},
			{"titanium", "BOOSTER", 1.20F, 16.0F},
			{"tungstensteel", "BOOSTER", 1.40F, 20.0F},
			{"tungstencarbide", "BOOSTER", 1.60F, 24.0F},
			{"adamantium", "BOOSTER", 4.00F, 100.0F},
			// the detector walk :63-72 — the boolean pair (F, T)
			{"aluminium", "DETECTOR", 0.20F, 6.0F},
			{"bronze", "DETECTOR", 0.30F, 8.0F},
			{"magnalium", "DETECTOR", 0.60F, 12.0F},
			{"steel", "DETECTOR", 0.60F, 12.0F},
			{"stainlesssteel", "DETECTOR", 0.80F, 10.0F},
			{"tungsten", "DETECTOR", 1.00F, 20.0F},
			{"titanium", "DETECTOR", 1.20F, 16.0F},
			{"tungstensteel", "DETECTOR", 1.40F, 20.0F},
			{"tungstencarbide", "DETECTOR", 1.60F, 24.0F},
			{"adamantium", "DETECTOR", 4.00F, 100.0F},
	};

	@Test
	public void rowsCensusPinsTheUpstreamAnchor() {
		assertEquals(30, GT6Rails.ROWS.size(), "the live row count (31 blocks = road + 10x3)");
		assertEquals(30, ANCHOR.length);
		for (int i = 0; i < ANCHOR.length; i++) {
			GT6Rails.RailRow tRow = GT6Rails.ROWS.get(i);
			String tKind = (String) ANCHOR[i][1];
			String tPrefix = tKind.equals("NORMAL") ? "rail_" : "rail_" + tKind.toLowerCase() + "_";
			assertEquals(tPrefix + ANCHOR[i][0], tRow.path(), "row " + i + " path (the upstream registration order pinned)");
			assertEquals(tKind, tRow.kind().name(), "row " + i + " kind");
			assertEquals((Float) ANCHOR[i][2], (Float) tRow.speed(), "row " + i + " speed (the aSpeed column)");
			assertEquals((Float) ANCHOR[i][3], (Float) tRow.resistance(), "row " + i + " resistance (the aExplosionResistance column)");
			assertEquals("block.gt6." + tRow.path(), tRow.displayKey(), "row " + i + " display key");
		}
	}

	@Test
	public void roadStripePinsThe39Line() {
		assertEquals("rail_road", GT6Rails.ROAD_PATH);
		assertEquals(0.50F, GT6Rails.ROAD_SPEED, "the :39 aSpeed column");
		assertEquals(20.0F, GT6Rails.ROAD_RESISTANCE, "the :39 aExplosionResistance column");
		assertEquals(0.35F, GT6Rails.RAIL_HARDNESS / 2.0F, "the BlockRailRoad.java:137 half-hardness face");
	}

	@Test
	public void speedLadderPinsPerRow() {
		for (GT6Rails.RailRow tRow : GT6Rails.ROWS) {
			float tSpeed = tRow.speed();
			String tWhere = tRow.path();
			// the straight-run arm: full speed when the 17-chunk guard is warm (:282/:285)
			assertEquals(tSpeed, GT6Rails.ladderSpeed(tSpeed, true, true), tWhere + " straight+loaded");
			// the cold-guard arm: the 1.0F bleed cap (the upstream doChunksNearChunkExist false face)
			assertEquals(Math.min(tSpeed, 1.0F), GT6Rails.ladderSpeed(tSpeed, true, false), tWhere + " straight+cold");
			// the default arm: curves/slopes/isolated fall back to the vanilla 0.4F cap (:287)
			assertEquals(Math.min(tSpeed, 0.4F), GT6Rails.ladderSpeed(tSpeed, false, true), tWhere + " not-straight");
			assertEquals(Math.min(tSpeed, 0.4F), GT6Rails.ladderSpeed(tSpeed, false, false), tWhere + " not-straight+cold");
		}
		// the vanilla-parity tail: a vanilla-speed rail never accelerates anywhere on the ladder
		assertEquals(0.4F, GT6Rails.ladderSpeed(0.4F, true, true), "vanilla speed stays vanilla");
		// the Adamantium face: 4.00F raw; the 1.2 per-cart rail cap (getMaxCartSpeedOnRail, both
		// legs) clamps it at runtime — a platform constant, declared in the GT6Rails javadoc
		assertEquals(4.0F, GT6Rails.ladderSpeed(4.0F, true, true), "the raw ladder value is the loader column");
	}

	@Test
	public void railClassesExtendTheVanillaRoots() {
		assertEquals(RailBlock.class, GT6RailBlock.class.getSuperclass(), "normal = the vanilla flexible rail");
		assertEquals(PoweredRailBlock.class, GT6BoosterRailBlock.class.getSuperclass(), "booster = the vanilla powered rail");
		assertEquals(PoweredRailBlock.class, GT6RoadRailBlock.class.getSuperclass(), "road stripe = the always-powered rail");
		assertTrue(BaseRailBlock.class.isAssignableFrom(GT6DetectorRailBlock.class), "detector under the BaseRailBlock root");
		// the GTCrowbarItem compat regression: the mines() arm is instanceof BaseRailBlock —
		// assignability IS the proof the arm covers the family
		for (Class<?> tClass : new Class<?>[] {GT6RailBlock.class, GT6BoosterRailBlock.class,
				GT6DetectorRailBlock.class, GT6RoadRailBlock.class}) {
			assertTrue(BaseRailBlock.class.isAssignableFrom(tClass), tClass.getSimpleName() + " under the crowbar rails arm");
		}
	}

	@Test
	public void crowbarMiningRegressionOnVanillaRails() {
		// the truth table the p10 acceptance pinned — the arm must keep covering the vanilla
		// rails (my blocks ride it through the same instanceof)
		assertTrue(GTCrowbarItem.mines(Blocks.RAIL.defaultBlockState()), "vanilla rail");
		assertTrue(GTCrowbarItem.mines(Blocks.POWERED_RAIL.defaultBlockState()), "vanilla powered rail");
		assertTrue(GTCrowbarItem.mines(Blocks.DETECTOR_RAIL.defaultBlockState()), "vanilla detector rail");
		assertTrue(GTCrowbarItem.mines(Blocks.ACTIVATOR_RAIL.defaultBlockState()), "vanilla activator rail");
		assertFalse(GTCrowbarItem.mines(Blocks.STONE.defaultBlockState()), "stone is not the crowbar surface");
		assertFalse(GTCrowbarItem.mines(Blocks.REDSTONE_BLOCK.defaultBlockState()), "the redstone BLOCK is STONE material, not circuits (the javadoc ruling)");
	}
}
