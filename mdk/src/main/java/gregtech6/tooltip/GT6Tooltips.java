package gregtech6.tooltip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The GT6 item-tooltip line registry (task r8-tooltip-infra): family short key → the
 * static row table the {@link gregtech6.item.GT6MachineBlockItem} carrier replays through
 * {@link #append}. Design = design.r8-jade-tooltip card T1 — the registry is a pure static
 * table, the rows are upstream addToolTips transcriptions (en = the upstream line verbatim,
 * key shape {@code gt6.tooltip.<family>.<n>} with n the upstream row order), and the colors
 * ride {@link GT6TooltipStyle}.
 *
 * <p>The canonical family vocabulary (the twelve upstream base classes the T2-T5 cards
 * walk): {@code boiler / boiler_large / tank / barrel / machine / multiblock / converter /
 * generator / wire / pipe_item / pipe_fluid / sensor}. An unregistered family appends ZERO
 * lines ({@link #append} returns on the null row table) — that is the T2-families-are-not-
 * here-yet contract, exercised by {@link GT6TooltipsTest}.
 *
 * <p>The pilot carries the boiler family's two calibration rows at their upstream positions
 * (MultiTileEntityBoilerTank.java:95-109: row 7 = :102 REQUIREMENT_WATER_PURE, row 9 = :104
 * HAZARD_EXPLOSION_STEAM — both pure-static, one ORANGE + one DRED so the palette mapping
 * is exercised twice); the T2 card fills rows 1-6, 8, 10-12 in place.
 */
public final class GT6Tooltips {

	/** The registry — insertion-ordered, family → immutable row list (T2-T5 add via {@link #register}). */
	public static final Map<String, List<GT6TooltipLine>> REGISTRY = new LinkedHashMap<>();

	static {
		register("boiler", List.of(
				new GT6TooltipLine("gt6.tooltip.boiler.7", GT6TooltipStyle.ORANGE),  // :102 LH.REQUIREMENT_WATER_PURE
				new GT6TooltipLine("gt6.tooltip.boiler.9", GT6TooltipStyle.DRED)));  // :104 LH.HAZARD_EXPLOSION_STEAM
	}

	/** Registers a family row table (T2-T5's entry seam; duplicate family = a datagen-order bug, fail loud). */
	public static void register(String aFamily, List<GT6TooltipLine> aRows) {
		if (REGISTRY.putIfAbsent(aFamily, aRows) != null) {
			throw new IllegalStateException("GT6 tooltip family already registered: " + aFamily);
		}
	}

	/**
	 * Appends the family's row table after the vanilla super chain (the caller's
	 * {@code super.appendHoverText} tail). Zero rows for an unregistered family — the
	 * T2-before state must not explode the twelve not-yet-tabled families.
	 */
	public static void append(String aFamily, List<Component> aTooltip) {
		List<GT6TooltipLine> tRows = REGISTRY.get(aFamily);
		if (tRows == null) return;
		for (GT6TooltipLine tRow : tRows) {
			aTooltip.add(tRow.component());
		}
	}

	private GT6Tooltips() {}

	/**
	 * One static tooltip line: the lang key, the {@link GT6TooltipStyle} color and the
	 * constant argument slots (upstream rows interpolate {@code mOutput}/{@code mCapacity}
	 * style constants at build time — the slots ride the translatable, never a literal).
	 * Lives in this file because the row tables are this package's whole story (the
	 * FILES_SCOPE of the T cards lists no third file).
	 */
	record GT6TooltipLine(String key, ChatFormatting style, Object... args) {

		/** The component the carrier appends — keyed translatable + the upstream color. */
		Component component() {
			return Component.translatable(key, args).withStyle(style);
		}
	}
}
