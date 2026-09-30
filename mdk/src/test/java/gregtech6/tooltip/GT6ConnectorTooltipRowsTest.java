package gregtech6.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import org.junit.jupiter.api.Test;

/**
 * The four connector families' row-table gate (task tooltip-wire-pipe-sensor acceptance
 * ①): row count, row order, key shape and the ChatFormatting per row, pinned as executable
 * assertions — the GT6TooltipsTest pure-JVM form (the registry, the line record and
 * {@code Component.translatable} are plain-object faces, no Bootstrap).
 *
 * <p>The upstream anchors the pins cite: MultiTileEntityWireElectric.addToolTips :126-132,
 * MultiTileEntityPipeFluid :215-228, MultiTileEntityPipeItem :115-120,
 * MultiTileEntitySensor :88-97 (the :90 per-sensor description row stays a gap — key .1
 * must NOT exist on the sensor table). The carrier-constants contract rides the fallback
 * array: the stat rows carry NO own args, so {@code append(family, list, args)} feeds the
 * positional slots — pinned per row (the voltage/loss numerals never enter the en text).
 */
public class GT6ConnectorTooltipRowsTest {

	private static List<String> keysOf(String aFamily) {
		return GT6Tooltips.REGISTRY.get(aFamily).stream().map(GT6Tooltips.GT6TooltipLine::key).toList();
	}

	@Test
	public void wireFamilyPinsTheThreeStatRows() {
		// MultiTileEntityWireElectric.java:126-132 — :127 voltage (the VN tier word rides
		// %2$s), :128 amperage, :129 loss; every row Chat.CYAN
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("wire");
		assertEquals(3, tRows.size());
		assertEquals(List.of("gt6.tooltip.wire.1", "gt6.tooltip.wire.2", "gt6.tooltip.wire.3"), keysOf("wire"));
		assertSame(GT6TooltipStyle.CYAN, tRows.get(0).style());
		assertSame(GT6TooltipStyle.CYAN, tRows.get(1).style());
		assertSame(GT6TooltipStyle.CYAN, tRows.get(2).style());
	}

	@Test
	public void wireContactSiblingAddsTheDarkRedHazardRow() {
		// the same three stat rows + :130 HAZARD_CONTACT (Chat.DRED → DARK_RED) — the
		// barrel_gas sibling shape, the registration site picks it by contactDamage()
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("wire_contact");
		assertEquals(4, tRows.size());
		assertEquals("gt6.tooltip.wire_contact.4", tRows.get(3).key());
		assertSame(GT6TooltipStyle.CYAN, tRows.get(0).style());
		assertSame(GT6TooltipStyle.DRED, tRows.get(3).style());
	}

	@Test
	public void pipeFluidFamilyPinsCapacityRowsPlusMagnifier() {
		// MultiTileEntityPipeFluid.java:215-228 — :216 bandwidth, :217 capacity (Chat.CYAN),
		// :226 magnifier (Chat.DGRAY); the cut rows stay upstream-position gaps (keys 3-9)
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("pipe_fluid");
		assertEquals(3, tRows.size());
		assertEquals(List.of("gt6.tooltip.pipe_fluid.1", "gt6.tooltip.pipe_fluid.2", "gt6.tooltip.pipe_fluid.10"), keysOf("pipe_fluid"));
		assertSame(GT6TooltipStyle.CYAN, tRows.get(0).style());
		assertSame(GT6TooltipStyle.CYAN, tRows.get(1).style());
		assertSame(GT6TooltipStyle.DGRAY, tRows.get(2).style());
	}

	@Test
	public void pipeItemFamilyPinsTheFourRows() {
		// MultiTileEntityPipeItem.java:115-120 — :116 stepsize, :117 bandwidth (Chat.CYAN),
		// :119/:120 the input/output monkey wrench rows (Chat.DGRAY)
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("pipe_item");
		assertEquals(4, tRows.size());
		assertEquals(List.of("gt6.tooltip.pipe_item.1", "gt6.tooltip.pipe_item.2", "gt6.tooltip.pipe_item.3", "gt6.tooltip.pipe_item.4"), keysOf("pipe_item"));
		assertSame(GT6TooltipStyle.CYAN, tRows.get(0).style());
		assertSame(GT6TooltipStyle.CYAN, tRows.get(1).style());
		assertSame(GT6TooltipStyle.DGRAY, tRows.get(2).style());
		assertSame(GT6TooltipStyle.DGRAY, tRows.get(3).style());
	}

	@Test
	public void sensorFamilySkipsThePerSensorDescriptionRow() {
		// MultiTileEntitySensor.java:88-97 — :90 getSensorDescription() is the ONE
		// per-sensor abstract row, no port data: key .1 must not exist, rows run .2-.7
		// (:91 ORANGE no-gui, :92-:96 the DGRAY tool rows)
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("sensor");
		assertEquals(6, tRows.size());
		assertEquals(List.of("gt6.tooltip.sensor.2", "gt6.tooltip.sensor.3", "gt6.tooltip.sensor.4",
				"gt6.tooltip.sensor.5", "gt6.tooltip.sensor.6", "gt6.tooltip.sensor.7"), keysOf("sensor"));
		assertTrue(keysOf("sensor").stream().noneMatch(pKey -> pKey.endsWith(".1")), "the per-sensor description row stays a gap");
		assertSame(GT6TooltipStyle.ORANGE, tRows.get(0).style());
		assertSame(GT6TooltipStyle.DGRAY, tRows.get(1).style());
		assertSame(GT6TooltipStyle.DGRAY, tRows.get(5).style());
	}

	@Test
	public void statRowsRideTheFallbackArgsNotLiterals() {
		// acceptance ① the args-slot pin: the wire stat rows carry NO own args — the carrier
		// array [voltage, tierName, amperage, loss] feeds the positional slots verbatim
		List<Component> tTooltip = new ArrayList<>();
		GT6Tooltips.append("wire", tTooltip, 32L, "LV", 2L, "1");
		assertEquals(3, tTooltip.size());
		TranslatableContents tRow0 = (TranslatableContents) tTooltip.get(0).getContents();
		assertEquals("gt6.tooltip.wire.1", tRow0.getKey());
		assertEquals(4, tRow0.getArgs().length);
		assertEquals(32L, tRow0.getArgs()[0]);
		assertEquals("LV", tRow0.getArgs()[1]);
		TranslatableContents tRow1 = (TranslatableContents) tTooltip.get(1).getContents();
		assertEquals("gt6.tooltip.wire.2", tRow1.getKey());
		assertEquals(2L, tRow1.getArgs()[2]);
		TranslatableContents tRow2 = (TranslatableContents) tTooltip.get(2).getContents();
		assertEquals("gt6.tooltip.wire.3", tRow2.getKey());
		assertEquals("1", tRow2.getArgs()[3]);
	}

	@Test
	public void unregisteredWireSiblingsAppendZeroRows() {
		// wire_redstone / wire_laser are deliberately untabled — the upstream WireElectric
		// stat rows must not leak onto the redstone/laser families (voltage 0 there)
		List<Component> tTooltip = new ArrayList<>();
		GT6Tooltips.append("wire_redstone", tTooltip);
		GT6Tooltips.append("wire_laser", tTooltip);
		assertTrue(tTooltip.isEmpty());
	}

	@Test
	public void makeStringKeepsTheUpstreamUnderscoreFace() {
		// UT.java:1296-1310 verbatim — plain below 10000, underscore thousands above
		assertEquals("50", GT6Tooltips.makeString(50L));
		assertEquals("9999", GT6Tooltips.makeString(9999L));
		assertEquals("32_768", GT6Tooltips.makeString(32768L));
		assertEquals("1_000_000", GT6Tooltips.makeString(1000000L));
		assertEquals("-32_768", GT6Tooltips.makeString(-32768L));
	}
}
