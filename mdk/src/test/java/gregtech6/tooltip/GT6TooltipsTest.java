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
 * T2-families-are-not-here-yet pin — the eleven not-yet-tabled families must not explode.
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
	public void boilerRegistryCarriesTheTwoPilotRowsAtTheirUpstreamPositions() {
		List<GT6Tooltips.GT6TooltipLine> tRows = GT6Tooltips.REGISTRY.get("boiler");
		assertEquals(2, tRows.size(), "the T1 pilot = exactly the two calibration rows");
		// MultiTileEntityBoilerTank.java:95-109 order: row 7 = :102, row 9 = :104
		assertEquals("gt6.tooltip.boiler.7", tRows.get(0).key());
		assertEquals("gt6.tooltip.boiler.9", tRows.get(1).key());
		assertSame(ChatFormatting.GOLD, tRows.get(0).style());     // Chat.ORANGE
		assertSame(ChatFormatting.DARK_RED, tRows.get(1).style()); // Chat.DRED
	}

	@Test
	public void unregisteredFamilyAppendsNothing() {
		List<Component> tTooltip = new ArrayList<>();
		GT6Tooltips.append("tank", tTooltip);
		GT6Tooltips.append("machine", tTooltip);
		GT6Tooltips.append("sensor", tTooltip);
		assertTrue(tTooltip.isEmpty(), "an unregistered family appends ZERO lines — the T2-before contract");
	}

	@Test
	public void appendProducesKeyedTranslatablesInRegistryOrder() {
		List<Component> tTooltip = new ArrayList<>();
		GT6Tooltips.append("boiler", tTooltip);
		assertEquals(2, tTooltip.size());
		TranslatableContents tRow0 = (TranslatableContents) tTooltip.get(0).getContents();
		TranslatableContents tRow1 = (TranslatableContents) tTooltip.get(1).getContents();
		assertEquals("gt6.tooltip.boiler.7", tRow0.getKey());
		assertEquals("gt6.tooltip.boiler.9", tRow1.getKey());
		// the components carry the upstream color (the palette rides the row, not the lang);
		// withStyle(ChatFormatting) stores TextColor.fromLegacyFormat — the value comparison
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), tTooltip.get(0).getStyle().getColor());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED), tTooltip.get(1).getStyle().getColor());
	}

	@Test
	public void lineArgsSlotRidesTheTranslatable() {
		GT6Tooltips.GT6TooltipLine tLine = new GT6Tooltips.GT6TooltipLine(
				"gt6.tooltip.pilot.args", ChatFormatting.WHITE, 640);
		TranslatableContents tContents = (TranslatableContents) tLine.component().getContents();
		assertEquals("gt6.tooltip.pilot.args", tContents.getKey());
		assertEquals(1, tContents.getArgs().length);
		assertEquals(640, tContents.getArgs()[0]);
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
		assertEquals(2, GT6Tooltips.REGISTRY.get("boiler").size());
	}
}
