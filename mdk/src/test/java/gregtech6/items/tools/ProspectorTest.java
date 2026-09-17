/**
 * Offline tests for task p30-pool-prospector: the HardHammer's prospector SECOND
 * behavior arm — the classification face (HAMMER + PROSPECTOR both true), the scan
 * radius pin (bind(1,20,quality+4) = 4 at the HardHammer quality 0), the vanilla-ore
 * trace table row by row, the five chat literals byte-for-byte (the upstream
 * ToolCompat.java:391-437 rows — deliberately NOT localized, the :400-401 ruling) and
 * the two message compositions.
 *
 * <p>Assertion surface notes (the HammerWrenchTest posture): the state-gated arms
 * (prospectable tags, the ray, the sampling) need a live world — they ride the RCON
 * chain ({@code /gt6tool prospect} five scenarios, the same GT6Prospector seam the
 * item's useOn runs); this test pins the pure faces a bare bootstrapped JVM can see
 * (Blocks constants are alive post-Bootstrap, MT/OP are plain data — the
 * GT6LangParityTest precedent).
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;

public class ProspectorTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	// ------------------------------------------------- the classification face

	/** The double behavior — classifies(HAMMER) AND classifies(PROSPECTOR) both true (the GTClubItem multi-action pin). */
	@Test
	public void classifiesHammerAndProspectorBothTrue() {
		assertTrue(GTHammerItem.classifies(GT6ToolActions.HAMMER), "the crafting-tool identity stays");
		assertTrue(GTHammerItem.classifies(GT6ToolActions.PROSPECTOR), "the second behavior arm answers");
	}

	/** The action-name pin — "gt6_prospector" (the gt6_hammer/gt6_crowbar literal shape), a distinct interned action. */
	@Test
	public void actionNameIsThePinnedLiteral() {
		assertEquals("gt6_prospector", GT6ToolActions.PROSPECTOR.name());
		assertTrue(GT6ToolActions.PROSPECTOR != GT6ToolActions.HAMMER, "the two arms stay distinct actions");
	}

	// ------------------------------------------------- the radius pin

	/**
	 * The scan radius = 4: bind(1, 20, quality + 4) at the HardHammer's base quality 0
	 * (ToolStats.java:67 default, GT_Tool_HardHammer no override; ToolCompat.java:393).
	 * The ray length AND the sampling cube half-width ride this one number.
	 */
	@Test
	public void scanRadiusIsPinnedToFour() {
		assertEquals(0L, GT6Prospector.BASE_QUALITY);
		assertEquals(4, GT6Prospector.scanRadius());
	}

	// ------------------------------------------------- the vanilla trace table

	/** The vanilla-ore trace table, row by row (the declared degradation face). */
	@Test
	public void vanillaTraceTableRowsArePinned() {
		expectTrace(Blocks.IRON_ORE, MT.Fe);
		expectTrace(Blocks.DEEPSLATE_IRON_ORE, MT.Fe);
		expectTrace(Blocks.COPPER_ORE, MT.Cu);
		expectTrace(Blocks.DEEPSLATE_COPPER_ORE, MT.Cu);
		expectTrace(Blocks.GOLD_ORE, MT.Au);
		expectTrace(Blocks.DEEPSLATE_GOLD_ORE, MT.Au);
		expectTrace(Blocks.NETHER_GOLD_ORE, MT.Au);
		expectTrace(Blocks.REDSTONE_ORE, MT.Redstone);
		expectTrace(Blocks.DEEPSLATE_REDSTONE_ORE, MT.Redstone);
		expectTrace(Blocks.DIAMOND_ORE, MT.Diamond);
		expectTrace(Blocks.DEEPSLATE_DIAMOND_ORE, MT.Diamond);
		expectTrace(Blocks.LAPIS_ORE, MT.Lapis);
		expectTrace(Blocks.DEEPSLATE_LAPIS_ORE, MT.Lapis);
		expectTrace(Blocks.COAL_ORE, MT.Coal);
		expectTrace(Blocks.DEEPSLATE_COAL_ORE, MT.Coal);
		expectTrace(Blocks.EMERALD_ORE, MT.Emerald);
		expectTrace(Blocks.DEEPSLATE_EMERALD_ORE, MT.Emerald);
		expectTrace(Blocks.NETHER_QUARTZ_ORE, MT.NetherQuartz);
	}

	/** Non-ore host rocks answer no trace (the sampling exclusion faces). */
	@Test
	public void nonOreBlocksAnswerNoTrace() {
		assertEquals(null, GT6Prospector.traceMaterial(Blocks.STONE.defaultBlockState()));
		assertEquals(null, GT6Prospector.traceMaterial(Blocks.OBSIDIAN.defaultBlockState()));
		assertEquals(null, GT6Prospector.traceMaterial(Blocks.NETHERRACK.defaultBlockState()));
	}

	// ------------------------------------------------- the five chat literals

	/** The five + the trace-prefix chat literals, byte-for-byte the upstream :403/:407/:411/:415/:430/:435 rows. */
	@Test
	public void chatLiteralsAreByteForByteTheUpstreamRows() {
		assertEquals("No traces of Ore found", GT6Prospector.MSG_NO_TRACES);
		assertEquals("There is Lava behind this Rock", GT6Prospector.MSG_LAVA);
		assertEquals("There is a Fluid behind this Rock", GT6Prospector.MSG_FLUID);
		assertEquals("There is an Air Pocket behind this Rock", GT6Prospector.MSG_AIR);
		assertEquals("Material is changing behind this Rock", GT6Prospector.MSG_CHANGING);
		assertEquals("Found traces of ", GT6Prospector.MSG_TRACES);
	}

	/** The message compositions — the :430 trace row and the :385 ore row through the ported getLocalName. */
	@Test
	public void messageCompositionsCarryTheMaterialLocal() {
		assertEquals("Found traces of Copper", GT6Prospector.traceMessage(MT.Cu));
		assertEquals("Found traces of Iron", GT6Prospector.traceMessage(MT.Fe));
		assertEquals("Small Copper Ore!", GT6Prospector.oreMessage(OP.oreSmall, MT.Cu));
		assertEquals("Stone Iron Ore!", GT6Prospector.oreMessage(OP.oreVanillastone, MT.Fe));
	}

	/** The tooltip key — the CS.java:1154 row the hammer carries (the lang face pins it in both locales). */
	@Test
	public void tooltipKeyIsThePinnedLiteral() {
		assertEquals("item.gt6.hammer.tooltip_prospector", GTHammerItem.TOOLTIP_KEY_PROSPECTOR);
	}

	private static void expectTrace(net.minecraft.world.level.block.Block aOre, OreDictMaterial aMaterial) {
		BlockState tState = aOre.defaultBlockState();
		assertEquals(aMaterial, GT6Prospector.traceMaterial(tState), aOre.toString());
	}
}
