package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * The registration-arm census pin (task p34-covers-gameplay-10 acceptance ①): the
 * {@link GT6Covers#ITEMS} DeferredRegister carries the EXACT expected id set — the landed
 * families verbatim (the pump/emitter/conductor/switch/shutter/filter/retriever singletons
 * + the p33 logistics 14 + the controller pair + the 2x10 conveyor/arm ladders + the 9
 * gameplay singletons + the 16 tag-selector ladder + the p35 display/scale five + the five
 * reboot timers) PLUS the p37 crafting + asphalt pair: 81 items total. The pin freezes the
 * append-only discipline: a renamed, dropped or extra cover item anywhere in the family
 * fails the census.
 */
public class GT6CoversGameplayCensusTest {

	/** The expected full registry arm. */
	private static final Set<String> EXPECTED = Set.of(
			// the landed singleton family (p5/p9/p10/p11/p31)
			"cover_pump", "cover_redstone_emitter", "cover_redstone_conductor_in", "cover_redstone_conductor_out",
			"cover_redstone_machine_switch", "cover_shutter", "cover_item_filter", "cover_item_retriever",
			"cover_auto_redstone_machine_switch", "cover_controller",
			// the p33 logistics 14
			"cover_logistics_display_cpu_logic", "cover_logistics_display_cpu_control", "cover_logistics_display_cpu_storage",
			"cover_logistics_display_cpu_conversion", "cover_logistics_fluid_export", "cover_logistics_fluid_import",
			"cover_logistics_fluid_storage", "cover_logistics_item_export", "cover_logistics_item_import",
			"cover_logistics_item_storage", "cover_logistics_generic_export", "cover_logistics_generic_import",
			"cover_logistics_generic_storage", "cover_logistics_generic_dump",
			// the p11 conveyor + robot arm ladders
			"cover_conveyor_0", "cover_conveyor_1", "cover_conveyor_2", "cover_conveyor_3", "cover_conveyor_4",
			"cover_conveyor_5", "cover_conveyor_6", "cover_conveyor_7", "cover_conveyor_8", "cover_conveyor_9",
			"cover_robot_arm_0", "cover_robot_arm_1", "cover_robot_arm_2", "cover_robot_arm_3", "cover_robot_arm_4",
			"cover_robot_arm_5", "cover_robot_arm_6", "cover_robot_arm_7", "cover_robot_arm_8", "cover_robot_arm_9",
			// task p34-covers-gameplay-10 — the 9 gameplay singletons
			"cover_vent", "cover_drain", "cover_pressure_valve", "cover_fluid_filter",
			"cover_redstone_torch", "cover_redstone_repeater",
			"cover_selector_redstone", "cover_selector_manual", "cover_selector_button_panel",
			// task p34-covers-gameplay-10 — the 16 tag-selector ladder
			"cover_selector_tag_0", "cover_selector_tag_1", "cover_selector_tag_2", "cover_selector_tag_3",
			"cover_selector_tag_4", "cover_selector_tag_5", "cover_selector_tag_6", "cover_selector_tag_7",
			"cover_selector_tag_8", "cover_selector_tag_9", "cover_selector_tag_10", "cover_selector_tag_11",
			"cover_selector_tag_12", "cover_selector_tag_13", "cover_selector_tag_14", "cover_selector_tag_15",
			// task p35-covers-display-scale-6 — the 5 display/scale singletons + the 5 reboot-switch ladder
			"cover_machine_display", "cover_auto_switch", "cover_energy_display", "cover_scale_energy", "cover_scale_progress",
			"cover_auto_timer_1m", "cover_auto_timer_5m", "cover_auto_timer_10m", "cover_auto_timer_20m", "cover_auto_timer_30m",
			// task p37-covers-crafting-asphalt — the last two gameplay classes: the
			// vanilla-workbench face + the walk-speed plate (the census 收满: the p35
			// research note's remaining Crafting/Asphalt gap closes here)
			"cover_crafting", "cover_asphalt");

	@Test
	public void registrationArmCarriesTheExactCensusSet() {
		Set<String> tActual = GT6Covers.ITEMS.getEntries().stream()
				.map(tEntry -> tEntry.getId().getPath())
				.collect(Collectors.toCollection(TreeSet::new));
		Set<String> tExpectedSorted = new TreeSet<>(EXPECTED);
		assertEquals(tExpectedSorted, tActual,
				"the cover registry arm drifted — the census is exact (acceptance ①)");
		assertEquals(81, tActual.size(), "81 cover items: 79 landed (10 singletons + 14 logistics + 2x10 ladders + 9 gameplay + 5 display/scale + 5 timers) + 16 tag ladder + this card's 2");
	}

}
