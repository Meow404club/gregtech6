package gregtech6.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.item.GT6MachineBlockItem;
import gregtech6.registry.GTMachines;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The real-machine hover gate (task r8-tooltip-basic-machine-family acceptance ①): the
 * GTMachines clinit walks every MachineRow list into the {@link GT6Tooltips} registry
 * (the walk IS the batch-replacement census — a duplicate path fails loud at clinit),
 * and fixture carriers keyed {@code machine:<path>} replay the REAL machines' rows
 * through the offline {@code appendHoverText} direct call (the GT6MachineBlockItemTest
 * shape — the call is the dual-leg compile pin). Three real machines, one per config
 * shape: the Dryer (a full MachineRow family), the Shredder (a legacy default-127
 * trio), the Burner Mixer (the ignition carrier); the Crusher T2 pins the parallel
 * suffix off a real row. The fixture seat assume-skips on JVMs where the item latch is
 * unreachable (the documented 21.1 asymmetry, GTOfflineTestBase.registerItemFixture).
 */
public class GT6MachineFamilyHoverTest extends GTOfflineTestBase {

	static GT6MachineBlockItem sDryer;
	static GT6MachineBlockItem sShredder;
	static GT6MachineBlockItem sBurnerMixer;

	@BeforeAll
	static void buildFixtures() {
		// the GTMachines clinit (the walk + the item holders) runs here, INSIDE the
		// bootstrapped window — never in a static field initializer (that would race the
		// superclass @BeforeAll bootstrap)
		// the maps are volatile, NULL until init() — the same lifecycle onModConstruct runs
		gregtech6.recipes.GT6RecipeMaps.init();
		GTMachines.registerMachineTooltipRows();
		assertNotNull(GTMachines.DRYER_ROWS, "the GTMachines clinit ran (the row lists are up)");
		sDryer = registerItemFixture("fixture_tooltip_machine_dryer",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "machine:dryer"));
		sShredder = registerItemFixture("fixture_tooltip_machine_shredder",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "machine:shredder"));
		sBurnerMixer = registerItemFixture("fixture_tooltip_machine_burner_mixer",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "machine:burner_mixer"));
	}

	private static void callHoverText(GT6MachineBlockItem aItem, ItemStack aStack, List<Component> aTooltip) {
		//? if forge {
		aItem.appendHoverText(aStack, null, aTooltip, TooltipFlag.NORMAL);
		//?} else {
		/*aItem.appendHoverText(aStack, Item.TooltipContext.EMPTY, aTooltip, TooltipFlag.NORMAL);
		*///?}
	}

	private static List<String> hoverKeys(GT6MachineBlockItem aItem) {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(aItem, new ItemStack(aItem), tTooltip);
		List<String> rKeys = new ArrayList<>();
		for (Component tLine : tTooltip) {
			rKeys.add(((TranslatableContents)tLine.getContents()).getKey());
		}
		return rKeys;
	}

	@Test
	public void theWalkRegistersEveryRowPathAndTheLegacySeventeen() {
		// the batch-replacement census: every MachineRow list row + the 17 legacy specs
		// (trio/oven ladders) hold a machine:<path> table — a miss is a swapped carrier
		// hovering NOTHING
		int tCensus = 0;
		for (var tRows : List.of(
				GTMachines.DRYER_ROWS, GTMachines.CANNER_ROWS, GTMachines.PRESS_ROWS, GTMachines.EXTRUDER_ROWS,
				GTMachines.SIFTER_ROWS, GTMachines.COMPRESSOR_ROWS, GTMachines.WIREMILL_ROWS,
				GTMachines.CANNER_ULV_ROWS, GTMachines.SHREDDER_ULV_ROWS, GTMachines.CRUSHER_ULV_ROWS,
				GTMachines.SIFTER_ULV_ROWS, GTMachines.WIREMILL_ULV_ROWS, GTMachines.ROLLINGMILL_ROWS,
				GTMachines.ROLLINGMILL_RU_ROWS, GTMachines.ROLL_BENDER_ROWS, GTMachines.ROLL_FORMER_ROWS,
				GTMachines.CLUSTER_MILL_ROWS, GTMachines.MIXER_ROWS, GTMachines.ELECTRIC_MIXER_ROWS,
				GTMachines.ELECTRIC_LOOM_ROWS, GTMachines.ELECTRIC_SIFTER_ROWS, GTMachines.BOXINATOR_ROWS,
				GTMachines.UNBOXINATOR_ROWS, GTMachines.FERMENTER_ROWS, GTMachines.POLARIZER_ROWS,
				GTMachines.MAGNETIC_SEPARATOR_ROWS, GTMachines.LASER_ENGRAVER_ROWS, GTMachines.LASER_WELDER_ROWS,
				GTMachines.FREEZER_ROWS, GTMachines.CRYO_MIXER_ROWS, GTMachines.MASSFAB_SMALL_ROWS,
				GTMachines.MOLECULAR_SCANNER_ROWS, GTMachines.REPLICATOR_ROWS, GTMachines.DISTILLERY_ROWS,
				GTMachines.BUZZSAW_ROWS, GTMachines.SQUEEZER_ROWS, GTMachines.CENTRIFUGE_ROWS,
				GTMachines.SLUICE_ROWS, GTMachines.SANDING_ROWS, GTMachines.PRESSURE_WASHER_ROWS,
				GTMachines.AUTOCRAFTER_ROWS, GTMachines.LIGHTNING_ROWS, GTMachines.LAMINATOR_ROWS,
				GTMachines.ELECTROLYZER_ROWS, GTMachines.INJECTOR_ROWS, GTMachines.PRINTER_ROWS,
				GTMachines.SCANNER_VISUALS_ROWS, GTMachines.SLICER_ROWS, GTMachines.STEAM_CRACKER_ROWS,
				GTMachines.CATALYTIC_CRACKER_ROWS, GTMachines.COAGULATOR_ROWS, GTMachines.GENERIFIER_ROWS,
				GTMachines.BATH_ROWS, GTMachines.AUTOCLAVE_ROWS, GTMachines.LOOM_ROWS, GTMachines.SMELTER_ROWS,
				GTMachines.MELTER_ROWS, GTMachines.ROASTING_ROWS, GTMachines.BUMBLELYZER_ROWS,
				GTMachines.CRYSTALLISATION_ROWS, GTMachines.BURNER_MIXER_ROWS, GTMachines.PLANTALYZER_ROWS)) {
			for (var tRow : tRows) {
				assertNotNull(GT6Tooltips.REGISTRY.get("machine:" + tRow.path()),
						"the walk registered machine:" + tRow.path());
				tCensus++;
			}
		}
		String[] tSuffixes = {"", "_t2", "_t3", "_t4"};
		for (String tBase : new String[] {"shredder", "crusher", "lathe", "oven"}) {
			for (String tSuffix : tSuffixes) {
				assertNotNull(GT6Tooltips.REGISTRY.get("machine:" + tBase + tSuffix),
						"the legacy walk registered machine:" + tBase + tSuffix);
				tCensus++;
			}
		}
		assertEquals(tCensus, GT6Tooltips.REGISTRY.keySet().stream().filter(k -> k.startsWith("machine:")).count(),
				"the machine: namespace holds exactly the walked families (no strays, the boiler pilot untouched)");
	}

	@Test
	public void theDryerHoversItsMachineRowFamilyTable() {
		List<String> tKeys = hoverKeys(sDryer);
		// the DRYER T1 row: cheap overclocking T, HU window {16,32,64} sides SBIT_D|SBIT_A,
		// item/fluid masks keyed, auto columns keyed → the full MachineRow family shape
		assertEquals("gt6.tooltip.machine.1", tKeys.get(0));
		assertTrue(tKeys.contains("gt6.tooltip.machine.2"), "the Dryer row is a cheap-overclocking carrier (:264)");
		assertEquals("gt6.tooltip.machine.4", tKeys.get(2), "the energy row slots in at position 3 (slot 3 efficiency silent)");
		assertEquals("gt6.tooltip.machine.11", tKeys.get(tKeys.size() - 5), ":273 — screwdriver opens the tool tail (the keyed auto columns arm both wrench rows)");
		assertEquals("gt6.tooltip.machine.16", tKeys.get(tKeys.size() - 1), ":281 — magnifier closes the table");
		assertTrue(tKeys.indexOf("gt6.tooltip.machine.6") < tKeys.indexOf("gt6.tooltip.machine.7")
				&& tKeys.indexOf("gt6.tooltip.machine.8") < tKeys.indexOf("gt6.tooltip.machine.9"),
				":302-345 — the IO rows ride upstream order (items before fluids, in before out)");
	}

	@Test
	public void theShredderHoversTheLegacyDefaultForm() {
		List<String> tKeys = hoverKeys(sShredder);
		assertEquals(List.of(
				"gt6.tooltip.machine.1", "gt6.tooltip.machine.4", "gt6.tooltip.machine.6",
				"gt6.tooltip.machine.7", "gt6.tooltip.machine.11", "gt6.tooltip.machine.14",
				"gt6.tooltip.machine.16"), tKeys,
				"the legacy default-127 trio: no cheap/efficiency/wrench rows, RM.Shredder has no fluid slots");
	}

	@Test
	public void theBurnerMixerHoverCarriesTheIgnitionRow() {
		List<String> tKeys = hoverKeys(sBurnerMixer);
		assertTrue(tKeys.contains("gt6.tooltip.machine.10"), ":271 — the mRequiresIgnition carrier shows the ignition row");
		// the row after the ignition slot: the tool tail
		assertEquals(tKeys.indexOf("gt6.tooltip.machine.10") + 1, tKeys.indexOf("gt6.tooltip.machine.11"),
				"ignition (10) sits directly before the screwdriver (11)");
	}

	@Test
	public void theCrusherT2RowCarriesTheRealParallelColumn() {
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("machine:crusher_t2");
		assertNotNull(tRows);
		Object tArg = ((TranslatableContents)tRows.get(0).component().getContents()).getArgs()[0];
		String tValue = tArg instanceof Component tComponent ? tComponent.getString() : String.valueOf(tArg);
		assertTrue(tValue.contains("(up to " + GTMachines.CRUSHER_PARALLEL[1] + "x processed per run)"),
				":261 — the T2 column 8 rides the real CRUSHER_PARALLEL table");
	}

	@Test
	public void theHoverReplaysExactlyTheRegisteredTable() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sDryer, new ItemStack(sDryer), tTooltip);
		assertEquals(GT6Tooltips.REGISTRY.get("machine:dryer").size(), tTooltip.size(),
				"the carrier replays the family table verbatim after the vanilla super chain");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), tTooltip.get(0).getStyle().getColor(), "the recipes row rides Chat.CYAN → AQUA");
	}
}
