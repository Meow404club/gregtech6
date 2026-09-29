package gregtech6.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;

import org.junit.jupiter.api.Test;

/**
 * The registry pure-function gate (task r8-tooltip-infra acceptance ①): row order, key
 * shape and the LH.Chat→ChatFormatting palette pinned as executable assertions (the
 * mapping table is gregapi/data/LH.java:685-710 verbatim — {@link GT6TooltipStyle}
 * javadoc). Plain JVM, no Bootstrap: the registry, the line record and
 * {@code Component.translatable} are all plain-object faces.
 *
 * <p>The zero-output contract (append on an unregistered family = zero lines) is the
 * T3-T5-families-are-not-here-yet pin — the not-yet-tabled families must not explode.
 */
public class GT6TooltipsTest {

	@Test
	public void paletteMapsUpstreamChatVerbatim() {
		// LH.java:685-710, each pair the upstream EnumChatFormatting projection
		assertSame(ChatFormatting.AQUA, GT6TooltipStyle.CYAN);       // :698
		assertSame(ChatFormatting.GREEN, GT6TooltipStyle.GREEN);     // :697
		assertSame(ChatFormatting.RED, GT6TooltipStyle.RED);         // :699
		assertSame(ChatFormatting.GOLD, GT6TooltipStyle.ORANGE);     // :692
		assertSame(ChatFormatting.DARK_RED, GT6TooltipStyle.DRED);   // :690
		assertSame(ChatFormatting.DARK_GRAY, GT6TooltipStyle.DGRAY); // :695
		assertSame(ChatFormatting.YELLOW, GT6TooltipStyle.YELLOW);   // :701
		assertSame(ChatFormatting.WHITE, GT6TooltipStyle.WHITE);     // :702
	}

	@Test
	public void boilerRegistryPinsTheThirteenRowTable() {
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("boiler");
		// MultiTileEntityBoilerTank.addToolTips :95-109 = rows 1-12 (:96-:107) + the super
		// facing row (TileEntityBase09FacingSingle.java:61, wrench :82) = row 13 — the row
		// indexes ARE the upstream row positions
		assertEquals(13, tRows.size(), "the boiler family = the 13-row upstream table");
		for (int i = 0; i < 13; i++) {
			assertEquals("gt6.tooltip.boiler." + (i + 1), tRows.get(i).key(), "row " + (i + 1) + " rides its upstream position");
		}
		// the lead colors, row by row (Chat.CYAN/YELLOW/GREEN/GREEN/RED/RED/ORANGE/ORANGE/
		// DRED/DRED/DGRAY/DGRAY/DGRAY — the :96-:107 + :61 chat prefixes)
		ChatFormatting[] tExpected = {
				ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.GREEN,
				ChatFormatting.RED, ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.GOLD,
				ChatFormatting.DARK_RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_GRAY,
				ChatFormatting.DARK_GRAY, ChatFormatting.DARK_GRAY};
		for (int i = 0; i < 13; i++) {
			assertSame(tExpected[i], tRows.get(i).style(), "row " + (i + 1) + " lead color");
		}
		// the T1 pilot pair survives inside the full table, unconflicted at its positions
		assertEquals("gt6.tooltip.boiler.7", tRows.get(6).key());
		assertEquals("gt6.tooltip.boiler.9", tRows.get(8).key());
	}

	@Test
	public void boilerLargeRegistryPinsTheEighteenRowTable() {
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("boiler_large");
		// MultiTileEntityLargeBoiler.addToolTips :150-167 = rows 1-15 (the STRUCTURE block
		// :151-155 over the :143-146 keys) + TileEntityBase10MultiBlockBase.java:100-101 =
		// rows 16-17 + the facing row :61 = row 18
		assertEquals(18, tRows.size(), "the boiler_large family = the 18-row upstream table");
		for (int i = 0; i < 18; i++) {
			assertEquals("gt6.tooltip.boiler_large." + (i + 1), tRows.get(i).key(), "row " + (i + 1) + " rides its upstream position");
		}
		// the STRUCTURE sub-rows 2-5 = Chat.WHITE (the :152-:155 prefixes)
		for (int i = 1; i <= 4; i++) {
			assertSame(ChatFormatting.WHITE, tRows.get(i).style(), "structure row " + (i + 1) + " = WHITE");
		}
		assertSame(ChatFormatting.AQUA, tRows.get(0).style());      // :151 LH.STRUCTURE header
		assertSame(ChatFormatting.GREEN, tRows.get(7).style());     // :158 ENERGY_INPUT
		assertSame(ChatFormatting.RED, tRows.get(9).style());       // :160 ENERGY_OUTPUT
		assertSame(ChatFormatting.GOLD, tRows.get(11).style());     // :162 REQUIREMENT_WATER_PURE
		assertSame(ChatFormatting.DARK_RED, tRows.get(12).style()); // :163 HAZARD_EXPLOSION_STEAM
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(15).style()); // 10MultiBlockBase:100 builder wand
	}

	@Test
	public void tankRegistryCarriesTheSingleStaticRow() {
		// TileEntityBase08FluidContainer.addToolTips :99-111 — the ONE pure-static row :100
		// (contentcap); the table is dormant until the port grows a tank registration class
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("tank");
		assertEquals(1, tRows.size());
		assertEquals("gt6.tooltip.tank.1", tRows.get(0).key());
		assertSame(ChatFormatting.AQUA, tRows.get(0).style()); // Chat.CYAN
	}

	@Test
	public void barrelRegistriesDeleteTheCarryStateRows() {
		// the design ruling: the :90 contentcap + :91 "Sealed (n)" carry rows are NOT
		// ported (the canonical-TE rebuild is a second-phase face) — the row indexes keep
		// the upstream positions, so keys .1/.2 must not exist in EITHER barrel table
		List<String> tBarrelKeys = GT6Tooltips.REGISTRY.get("barrel").stream().map(GT6Tooltips.GT6TooltipLine::key).toList();
		assertEquals(List.of("gt6.tooltip.barrel.3", "gt6.tooltip.barrel.4", "gt6.tooltip.barrel.10",
				"gt6.tooltip.barrel.11", "gt6.tooltip.barrel.12", "gt6.tooltip.barrel.13"), tBarrelKeys);
		List<String> tGasKeys = GT6Tooltips.REGISTRY.get("barrel_gas").stream().map(GT6Tooltips.GT6TooltipLine::key).toList();
		assertEquals(List.of("gt6.tooltip.barrel_gas.3", "gt6.tooltip.barrel_gas.4", "gt6.tooltip.barrel_gas.6",
				"gt6.tooltip.barrel_gas.10", "gt6.tooltip.barrel_gas.11", "gt6.tooltip.barrel_gas.12",
				"gt6.tooltip.barrel_gas.13"), tGasKeys);
		// the carry-state assertion: no barrel table carries rows 1-2 (Sealed/content)
		for (String tFamily : List.of("barrel", "barrel_gas")) {
			for (GT6Tooltips.GT6TooltipLine tRow : GT6Tooltips.REGISTRY.get(tFamily)) {
				assertTrue(!tRow.key().endsWith(".1") && !tRow.key().endsWith(".2"),
						tFamily + " must not carry the Sealed/content carry rows");
			}
		}
		// the gas split: ONLY barrel_gas carries the :95 proof row
		assertEquals(7, GT6Tooltips.REGISTRY.get("barrel_gas").size());
		assertEquals(6, GT6Tooltips.REGISTRY.get("barrel").size(), "the wood barrel is the one gas=F row");
	}

	@Test
	public void multiblockTablePinsTheBaseChainRows() {
		// task r8-tooltip-multiblock-generator — TileEntityBase10MultiBlockBase.java:99-103
		// + the TileEntityBase09FacingSingle.java:61 super tail, in upstream order
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("multiblock");
		assertEquals(3, tRows.size(), "the multiblock family = the three family-constant base rows");
		assertEquals("gt6.tooltip.multiblock.1", tRows.get(0).key()); // :100 LH.TOOL_TO_BUILD_BUILDER_WAND
		assertEquals("gt6.tooltip.multiblock.2", tRows.get(1).key()); // :101 LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS
		assertEquals("gt6.tooltip.multiblock.3", tRows.get(2).key()); // Base09:61 the facing row
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(0).style());   // Chat.DGRAY, all three
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(1).style());
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(2).style());
	}

	@Test
	public void converterTablePinsTheStaticTailAtBase11Positions() {
		// TileEntityBase11MultiBlockConverter.java:91-94 sequence — slots 1-3 are the
		// per-instance energy-in/out + efficiency faces (no static face), the family
		// table carries the super tail at positions 4-6
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("converter");
		assertEquals(3, tRows.size());
		assertEquals("gt6.tooltip.converter.4", tRows.get(0).key());
		assertEquals("gt6.tooltip.converter.5", tRows.get(1).key());
		assertEquals("gt6.tooltip.converter.6", tRows.get(2).key());
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(0).style());
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(1).style());
		assertSame(ChatFormatting.DARK_GRAY, tRows.get(2).style());
	}

	@Test
	public void generatorTablePinsTheConstantBlockAtSolidPositions() {
		// MultiTileEntityGeneratorSolid.java:85-97 — slots 1-3 are the per-instance
		// recipes/efficiency/energy-out faces; the constant block = :89-95 + the
		// Base09 facing tail, 4 ORANGE requirements + 2 DRED hazards + 2 DGRAY tools
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("generator");
		assertEquals(8, tRows.size());
		String[] tKeys = {"gt6.tooltip.generator.4", "gt6.tooltip.generator.5", "gt6.tooltip.generator.6",
				"gt6.tooltip.generator.7", "gt6.tooltip.generator.8", "gt6.tooltip.generator.9",
				"gt6.tooltip.generator.10", "gt6.tooltip.generator.11"};
		ChatFormatting[] tStyles = {ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.GOLD,
				ChatFormatting.GOLD, ChatFormatting.DARK_RED, ChatFormatting.DARK_RED,
				ChatFormatting.DARK_GRAY, ChatFormatting.DARK_GRAY};
		for (int i = 0; i < tKeys.length; i++) {
			assertEquals(tKeys[i], tRows.get(i).key(), "generator row " + i + " rides its upstream slot");
			assertSame(tStyles[i], tRows.get(i).style(), "generator row " + i + " color");
		}
	}

	@Test
	public void unregisteredFamilyAppendsNothing() {
		List<Component> tTooltip = new ArrayList<>();
		// the zero-row contract (task r8-tooltip-wire-pipe-sensor rebase): sensor/wire/
		// pipe_fluid/pipe_item tabled by T4/T5 — the walk-on families are the ones still
		// waiting for their cards (machine = the T3-side remainder; wire_redstone and
		// wire_laser = the declared-unregistered wire siblings)
		GT6Tooltips.append("machine", tTooltip);
		GT6Tooltips.append("wire_redstone", tTooltip);
		GT6Tooltips.append("wire_laser", tTooltip);
		assertTrue(tTooltip.isEmpty(), "an unregistered family appends ZERO lines — the T-before contract");
	}

	@Test
	public void appendProducesKeyedTranslatablesInRegistryOrder() {
		List<Component> tTooltip = new ArrayList<>();
		GT6Tooltips.append("boiler", tTooltip);
		assertEquals(13, tTooltip.size());
		TranslatableContents tRow0 = (TranslatableContents) tTooltip.get(0).getContents();
		TranslatableContents tRow1 = (TranslatableContents) tTooltip.get(1).getContents();
		assertEquals("gt6.tooltip.boiler.1", tRow0.getKey());
		assertEquals("gt6.tooltip.boiler.2", tRow1.getKey());
		// the components carry the upstream color (the palette rides the row, not the lang);
		// withStyle(ChatFormatting) stores TextColor.fromLegacyFormat — the value comparison
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), tTooltip.get(0).getStyle().getColor());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), tTooltip.get(1).getStyle().getColor());
	}

	@Test
	public void fallbackArgsRideThePositionalSlots() {
		// the per-variant form: the carrier hands [in, out, cap] and every arg-less line
		// takes the array through its positional slots (TranslatableContents.java:87-88)
		List<Component> tTooltip = new ArrayList<>();
		GT6Tooltips.append("boiler", tTooltip, 16, 32, 320000);
		assertEquals(13, tTooltip.size());
		TranslatableContents tRow3 = (TranslatableContents) tTooltip.get(2).getContents(); // boiler.3 = "%1$s HU/t"
		assertEquals(3, tRow3.getArgs().length);
		assertEquals(16, tRow3.getArgs()[0]);
		assertEquals(320000, tRow3.getArgs()[2]);
		// a line with OWN constant args ignores the fallback (the T1 args-slot stance)
		GT6Tooltips.GT6TooltipLine tOwn = new GT6Tooltips.GT6TooltipLine("gt6.tooltip.pilot.args", ChatFormatting.WHITE, 640);
		TranslatableContents tOwnContents = (TranslatableContents) tOwn.component(1, 2, 3).getContents();
		assertEquals(1, tOwnContents.getArgs().length);
		assertEquals(640, tOwnContents.getArgs()[0]);
	}

	@Test
	public void duplicateFamilyRegistrationFailsLoud() {
		// the T2-T5 entry seam guards against double-tabled families (a datagen-order bug)
		boolean tThrew = false;
		try {
			GT6Tooltips.register("boiler", List.of());
		} catch (IllegalStateException tExpected) {
			tThrew = true;
		}
		assertTrue(tThrew, "a duplicate family registration must fail loud");
		// the failed put left the original table in place (the registry rows unchanged)
		assertEquals(13, GT6Tooltips.REGISTRY.get("boiler").size());
	}
}
