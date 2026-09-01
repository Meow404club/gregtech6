package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The crowbar mining-surface truth table (task p10-tool-crowbar-mining acceptance,
 * offline half): the upstream isMinableBlock :108-114 modern halves pinned through
 * the static seam — rails arm (BaseRailBlock instanceof) true with speed>1, the
 * redstone-IO family arm true, stone/dirt (and everything else) false at the vanilla
 * hand speed. The instance {@code isCorrectToolForDrops}/{@code getDestroySpeed} only
 * delegate to this seam; a mod Item cannot be constructed in this bootstrapped-and-
 * frozen JVM (the CrowbarTest.java:59-62 wall), so the seam IS the pinned surface.
 *
 * <p>The hoe red line rides along: the mining half must not borrow the hoe face —
 * {@code classifies} stays CROWBAR-only (the three wrench-substitute predicate files
 * GTOvenBlock/GTFluidPipeBlock/GTWrenchHighlightListener are untouched by
 * construction, zero diff) and hoe-faced blocks do not mine.
 */
public class CrowbarMiningTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	@Test
	void railsArmTrueWithSpeedAboveOne() {
		BlockState[] tRails = {
				Blocks.RAIL.defaultBlockState(), Blocks.POWERED_RAIL.defaultBlockState(),
				Blocks.DETECTOR_RAIL.defaultBlockState(), Blocks.ACTIVATOR_RAIL.defaultBlockState()};
		for (BlockState tRail : tRails) {
			// the instanceof arm is the vanilla abstract rail root, not a block list
			assertTrue(tRail.getBlock() instanceof BaseRailBlock, "sanity: the vanilla rail root");
			assertTrue(GTCrowbarItem.mines(tRail), "the rails arm authorizes drops: " + tRail.getBlock());
			assertEquals(GTCrowbarItem.MINING_SPEED, GTCrowbarItem.destroySpeedBonus(tRail),
					"the acceptance-pinned speed>1: " + tRail.getBlock());
			assertTrue(GTCrowbarItem.destroySpeedBonus(tRail) > 1.0F);
		}
		assertEquals(6.0F, GTCrowbarItem.MINING_SPEED, "the iron-tier dig-speed scale");
	}

	@Test
	void redstoneIoFamilyArmTrue() {
		// power/transfer/logic representatives
		BlockState[] tCircuits = {
				Blocks.REDSTONE_WIRE.defaultBlockState(), Blocks.REDSTONE_TORCH.defaultBlockState(),
				Blocks.REDSTONE_WALL_TORCH.defaultBlockState(), Blocks.REPEATER.defaultBlockState(),
				Blocks.COMPARATOR.defaultBlockState(), Blocks.LEVER.defaultBlockState(),
				Blocks.OBSERVER.defaultBlockState(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState(),
				Blocks.TARGET.defaultBlockState(), Blocks.SCULK_SENSOR.defaultBlockState(),
				Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState(),
				// tripwire pair + button family representatives (stone, wood, blackstone, crimson)
				Blocks.TRIPWIRE.defaultBlockState(), Blocks.TRIPWIRE_HOOK.defaultBlockState(),
				Blocks.STONE_BUTTON.defaultBlockState(), Blocks.OAK_BUTTON.defaultBlockState(),
				Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), Blocks.WARPED_BUTTON.defaultBlockState()};
		for (BlockState tCircuit : tCircuits) {
			assertTrue(GTCrowbarItem.mines(tCircuit), "the circuits arm authorizes drops: " + tCircuit.getBlock());
			assertTrue(GTCrowbarItem.destroySpeedBonus(tCircuit) > 1.0F,
					"the mineable surface digs faster than hand: " + tCircuit.getBlock());
		}
	}

	@Test
	void stoneAndDirtStayFalseAtHandSpeed() {
		BlockState[] tNotMineable = {
				Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
				Blocks.COBBLESTONE.defaultBlockState(), Blocks.BRICKS.defaultBlockState(),
				Blocks.OAK_PLANKS.defaultBlockState(), Blocks.GLASS.defaultBlockState()};
		for (BlockState tBlock : tNotMineable) {
			assertFalse(GTCrowbarItem.mines(tBlock), "outside the isMinableBlock surface: " + tBlock.getBlock());
			assertEquals(1.0F, GTCrowbarItem.destroySpeedBonus(tBlock),
					"the vanilla hand speed, untouched: " + tBlock.getBlock());
		}
	}

	@Test
	void excludedCircuitsNeighborsStayFalse() {
		// the javadoc ruling boundary: pressure plates (1.7.10 STONE/WOOD material), the
		// redstone block (STONE) and the plain torch (DECORATION) are NOT circuits
		BlockState[] tExcluded = {
				Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), Blocks.OAK_PRESSURE_PLATE.defaultBlockState(),
				Blocks.REDSTONE_BLOCK.defaultBlockState(), Blocks.TORCH.defaultBlockState()};
		for (BlockState tBlock : tExcluded) {
			assertFalse(GTCrowbarItem.mines(tBlock), "outside the circuits ruling: " + tBlock.getBlock());
		}
	}

	@Test
	void hoeRedLineUnchangedByTheMiningHalf() {
		// the classification red line rides through every card: CROWBAR only, never
		// HOE_DIG — and the mining surface does not borrow the hoe face either (farmland
		// and the mineable/hoe blocks do not mine)
		assertTrue(GTCrowbarItem.classifies(GT6ToolActions.CROWBAR));
		assertFalse(GTCrowbarItem.classifies(ToolActions.HOE_DIG), "RED LINE — never a hoe");
		assertFalse(GTCrowbarItem.mines(Blocks.FARMLAND.defaultBlockState()), "the hoe face is not the crowbar face");
		assertFalse(GTCrowbarItem.mines(Blocks.HAY_BLOCK.defaultBlockState()), "no hoe-tagged block leaks in");
	}
}
