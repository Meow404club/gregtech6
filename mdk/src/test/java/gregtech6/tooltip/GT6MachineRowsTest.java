package gregtech6.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import org.junit.jupiter.api.Test;

import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.RecipeMap;
import gregtech6.tooltip.GT6Tooltips.GT6TooltipLine;

/**
 * The machine row-table pins (task tooltip-basic-machine-family acceptance ①): the
 * {@link GT6MachineRows#rows} transcription line-pinned — row order, keys, colors, and
 * the composed value slots — as a pure function over a hand-built {@link GT6MachineRows
 * .Spec}. The upstream anchors per slot live on the GT6MachineRows javadoc; the sibling
 * GT6MachineFamilyHoverTest replays REAL registration rows through the carrier.
 */
public class GT6MachineRowsTest {

	/** The probe map — a unique-name RecipeMap (the jade-provider probe convention), 1 item in / 1 out / 1 fluid in, no fluids out. */
	static final RecipeMap PROBE = new RecipeMap(new java.util.HashSet<>(),
			"gt.recipe.probe.rows", "Probe Rows", null, 0, 1, "probe", 1, 1, 0, 1, 0, 0, 0, 0);

	/** The legacy-trio form: the default-127 IO config, RU window {16, 32, 64}, no autos. */
	static GT6MachineRows.Spec legacy(RecipeMap aMap, int aParallel) {
		return new GT6MachineRows.Spec(aMap, aParallel, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
	}

	/** The keys of the row list, in order. */
	static List<String> keys(List<GT6TooltipLine> aRows) {
		List<String> rKeys = new ArrayList<>();
		for (GT6TooltipLine tRow : aRows) {
			rKeys.add(((TranslatableContents)tRow.component().getContents()).getKey());
		}
		return rKeys;
	}

	/** The first arg of a row, flattened (the composed values are single components). */
	static String arg(GT6TooltipLine aRow) {
		Object tArg = ((TranslatableContents)aRow.component().getContents()).getArgs()[0];
		return tArg instanceof Component tComponent ? tComponent.getString() : String.valueOf(tArg);
	}

	@Test
	public void theLegacyFormPinsTheEightRowTable() {
		// no cheap, no efficiency, 127 masks (all-sides form), no autos, item-only IO? — the
		// PROBE carries a fluid-in slot, so the fluid row rides the any-no-auto form
		List<String> tKeys = keys(GT6MachineRows.rows(legacy(PROBE, 1)));
		assertEquals(List.of(
				"gt6.tooltip.machine.1",  // :261 Recipes
				"gt6.tooltip.machine.4",  // :352 Energy IN
				"gt6.tooltip.machine.6",  // :310 Items IN, Any Side (no auto)
				"gt6.tooltip.machine.7",  // :321 Items OUT
				"gt6.tooltip.machine.8",  // :332 Fluids IN
				"gt6.tooltip.machine.11", // :273 Screwdriver
				"gt6.tooltip.machine.14", // :278 Soft Hammer
				"gt6.tooltip.machine.16"),// :281 Magnifier
				tKeys, "the legacy default-127 form: eight rows, the 5/10/15 slots silent");
	}

	@Test
	public void theRowColorsPinTheUpstreamPalette() {
		List<GT6TooltipLine> tRows = GT6MachineRows.rows(legacy(PROBE, 1));
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), tRows.get(0).component().getStyle().getColor(), "Chat.CYAN → AQUA (:261)");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tRows.get(1).component().getStyle().getColor(), "Chat.GREEN energy in (:352)");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tRows.get(2).component().getStyle().getColor(), "Chat.GREEN items in (:310)");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tRows.get(3).component().getStyle().getColor(), "Chat.RED items out (:321)");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tRows.get(4).component().getStyle().getColor(), "Chat.GREEN fluids in (:332)");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tRows.get(5).component().getStyle().getColor(), "Chat.DGRAY tools (:273)");
	}

	@Test
	public void theRecipesRowCarriesTheParallelSuffixInTheWhiteSlot() {
		GT6TooltipLine tRow1 = GT6MachineRows.rows(legacy(PROBE, 4)).get(0);
		assertEquals("gt6.tooltip.machine.1", ((TranslatableContents)tRow1.component().getContents()).getKey());
		assertTrue(arg(tRow1).contains("(up to 4x processed per run)"), ":261 — the upstream inline suffix");
		assertTrue(arg(tRow1).startsWith("Probe Rows"), "the map's mNameLocal rides first");
		// parallel 1 = no suffix
		assertFalse(arg(GT6MachineRows.rows(legacy(PROBE, 1)).get(0)).contains("per run"));
	}

	@Test
	public void cheapAndEfficiencyRowsRideTheirUpstreamSlots() {
		GT6MachineRows.Spec tFull = new GT6MachineRows.Spec(PROBE, 1, true, false, 8550,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		List<GT6TooltipLine> tRows = GT6MachineRows.rows(tFull);
		assertEquals("gt6.tooltip.machine.2", keys(tRows).get(1), ":264 — cheap overclocking slots in at 2");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), tRows.get(1).component().getStyle().getColor());
		assertEquals("gt6.tooltip.machine.3", keys(tRows).get(2), ":266 — efficiency slots in at 3");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), tRows.get(2).component().getStyle().getColor());
		assertEquals("85.50", arg(tRows.get(2)), "LH.java:270 percent form");
		// the 8500 → "85.00" arm and the 10000 silence
		GT6MachineRows.Spec tEven = new GT6MachineRows.Spec(PROBE, 1, false, false, 8500,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		assertEquals("85.00", arg(GT6MachineRows.rows(tEven).get(1)));
		assertFalse(keys(GT6MachineRows.rows(legacy(PROBE, 1))).contains("gt6.tooltip.machine.3"),
				"efficiency 10000 (the port default) adds NO row (:265 gate)");
	}

	@Test
	public void theEnergyRowCarriesTheWindowAndSkipsSidesAt127() {
		GT6TooltipLine tEnergy = GT6MachineRows.rows(legacy(PROBE, 1)).get(1);
		String tValue = arg(tEnergy);
		assertEquals("32 RU/t (16 to 64)", tValue, ":352 — rec unit/t (min to max); mask 127 skips the side list");
		// the equal-window "/t" form
		GT6MachineRows.Spec tEqual = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 32, 32, 32, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		assertEquals("32 RU/t", arg(GT6MachineRows.rows(tEqual).get(1)), ":352 — the rec==min==max short form");
	}

	@Test
	public void theEnergyRowListsSidesOffTheBitMask() {
		// SBIT_D = 1 (bottom), SBIT_L = 4 (left): the mask bits name faces bottom-first
		GT6MachineRows.Spec tSided = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_L),
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		String tValue = arg(GT6MachineRows.rows(tSided).get(1));
		assertTrue(tValue.contains("(16 to 64, gt6.tooltip.face.bottom, gt6.tooltip.face.left)"),
				":290 — the face words ride the shared gt6.tooltip.face.* keys, side order 0-5");
	}

	@Test
	public void theItemRowsRideTheThreeUpstreamForms() {
		// specific sides, no auto pool → plain list (the legacy trio form would show 127 —
		// here the SBIT_U|SBIT_D mask form)
		GT6MachineRows.Spec tSpecific = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D), (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
				(byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		List<GT6TooltipLine> tRows = GT6MachineRows.rows(tSpecific);
		assertEquals("gt6.tooltip.face.bottom, gt6.tooltip.face.top", arg(tRows.get(2)), ":305 — specific list, side order 0-5");
		// the auto marker rides the side equal to the auto column (:304)
		GT6MachineRows.Spec tMarked = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D), (byte)127,
				(byte)127, (byte)127, (byte)1 /*SIDE_TOP*/, (byte)-1, (byte)-1, (byte)-1);
		assertEquals("gt6.tooltip.face.bottom, gt6.tooltip.face.top (auto)", arg(GT6MachineRows.rows(tMarked).get(2)), ":304 — the (auto) marker");
		// the any-auto and any-no-auto forms (:308/:310)
		GT6MachineRows.Spec tAnyAuto = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)1, (byte)-1, (byte)5, (byte)-1);
		List<GT6TooltipLine> tAnyRows = GT6MachineRows.rows(tAnyAuto);
		assertTrue(arg(tAnyRows.get(2)).endsWith("(auto, otherwise any)"), ":308 — items IN with a valid auto column rides the any-auto form");
		assertTrue(arg(tAnyRows.get(3)).endsWith("(no auto)"), ":321 — items OUT without a valid auto column rides the no-auto form");
		assertTrue(arg(tAnyRows.get(4)).endsWith("(auto, otherwise any)"), ":330 — fluids IN with a valid auto column rides the any-auto form");
		// the valid auto columns arm the wrench rows too (:275) — the PROBE has no fluid
		// outputs, so the sided tail here closes with wrench-in (12) between the tool rows
		assertEquals("gt6.tooltip.machine.12", keys(tAnyRows).get(6), ":275 — the wrench-in row rides the tool tail");
		// the empty specific mask adds NO row (:305 stringValid gate)
		GT6MachineRows.Spec tEmpty = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)0, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		assertFalse(keys(GT6MachineRows.rows(tEmpty)).contains("gt6.tooltip.machine.6"), ":305 — an empty side list adds no Items IN row");
	}

	@Test
	public void theIgnitionRowRidesItsUpstreamSlot() {
		GT6MachineRows.Spec tIgnite = new GT6MachineRows.Spec(PROBE, 1, false, true, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)-1, (byte)-1, (byte)-1, (byte)-1);
		List<GT6TooltipLine> tRows = GT6MachineRows.rows(tIgnite);
		assertEquals("gt6.tooltip.machine.10", keys(tRows).get(5), ":271 — ignition slots in after the sided rows");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), tRows.get(5).component().getStyle().getColor(), "Chat.ORANGE → GOLD");
	}

	@Test
	public void theWrenchRowsGateOnTheAutoColumns() {
		GT6MachineRows.Spec tAuto = new GT6MachineRows.Spec(PROBE, 1, false, false, null,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)2, (byte)4, (byte)-1, (byte)-1);
		List<String> tKeys = keys(GT6MachineRows.rows(tAuto));
		assertTrue(tKeys.contains("gt6.tooltip.machine.12") && tKeys.contains("gt6.tooltip.machine.13"),
				":275/:277 — valid item auto columns arm both wrench rows");
		assertFalse(keys(GT6MachineRows.rows(legacy(PROBE, 1))).contains("gt6.tooltip.machine.12"),
				":274 — undefined autos leave the wrench rows off");
	}

	@Test
	public void theSlotNumberingKeepsTheUpstreamGaps() {
		Set<String> tSeen = new HashSet<>(keys(GT6MachineRows.rows(new GT6MachineRows.Spec(PROBE, 4, true, true, 5000,
				gregapi.data.TD.Energy.RU, 16, 32, 64, (byte)127,
				(byte)127, (byte)127, (byte)127, (byte)127, (byte)2, (byte)4, (byte)5, (byte)1))));
		for (String tGap : new String[] {"gt6.tooltip.machine.5", "gt6.tooltip.machine.15"}) {
			assertFalse(tSeen.contains(tGap), tGap + " stays a gap (the charged-energy / Builder Wand upstream slots)");
		}
		assertEquals(13, tSeen.size(), "the maximal table: 13 rows (slots 1-4, 6-8, 10-14, 16 — the PROBE map has no fluid outputs)");
	}
}
