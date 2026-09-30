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
 * <p>The pilot carried the boiler family's two calibration rows at their upstream positions
 * (MultiTileEntityBoilerTank.java:95-109: row 7 = :102 REQUIREMENT_WATER_PURE, row 9 = :104
 * HAZARD_EXPLOSION_STEAM); task r8-tooltip-boiler-tank (T2) folded them into the full 13-row
 * table and added the boiler_large / tank / barrel(+barrel_gas) tables — every row numbered
 * by its upstream row index, so a gap in the numbering IS the ported-out row set.
 *
 * <p>Task r8-tooltip-multiblock-generator tables the multiblock / converter / generator
 * families (the T4 row tables — upstream anchors TileEntityBase10MultiBlockBase.java:99-103
 * + TileEntityBase09FacingSingle.java:61, TileEntityBase11MultiBlockConverter.java:91-104,
 * MultiTileEntityGeneratorSolid.java:85-97; per-instance numeric rows keep their slots
 * empty, see the static block note).
 */
public final class GT6Tooltips {

	/** The registry — insertion-ordered, family → immutable row list (T2-T5 add via {@link #register}). */
	public static final Map<String, List<GT6TooltipLine>> REGISTRY = new LinkedHashMap<>();

	static {
		// the Steam Boiler Tank family (both the standard and the Strong rows ride it) —
		// MultiTileEntityBoilerTank.addToolTips :95-109 + the super chain's facing row
		// (TileEntityBase09FacingSingle.java:61, getFacingTool = TOOL_wrench :82 = row 13);
		// rows 3-6 carry the per-variant constants in positional slots (%1$s = Energy IN,
		// %2$s = Energy OUT, %3$s = Capacity — the carrier hands [in, out, cap] per
		// BoilerRow, the mOutput/STEAM_PER_EU + mOutput + mOutput*10000 shape of :98-:101);
		// row 14 = the port-authored output-condition annex (task r10-debt-boiler-heat-tip)
		register("boiler", List.of(
				new GT6TooltipLine("gt6.tooltip.boiler.1", GT6TooltipStyle.CYAN),    // :96 LH.CONVERTS_FROM_X.. (80 HU -> 160 L Steam, constant across all 26 rows)
				new GT6TooltipLine("gt6.tooltip.boiler.2", GT6TooltipStyle.YELLOW),  // :97 LH.getToolTipEfficiency(10000) — pristine, the calcified state is carry state
				new GT6TooltipLine("gt6.tooltip.boiler.3", GT6TooltipStyle.GREEN),   // :98 LH.ENERGY_INPUT (Any Side)
				new GT6TooltipLine("gt6.tooltip.boiler.4", GT6TooltipStyle.GREEN),   // :99 LH.ENERGY_CAPACITY (HU)
				new GT6TooltipLine("gt6.tooltip.boiler.5", GT6TooltipStyle.RED),     // :100 LH.ENERGY_OUTPUT (Top)
				new GT6TooltipLine("gt6.tooltip.boiler.6", GT6TooltipStyle.RED),     // :101 LH.ENERGY_CAPACITY (Steam)
				new GT6TooltipLine("gt6.tooltip.boiler.7", GT6TooltipStyle.ORANGE),  // :102 LH.REQUIREMENT_WATER_PURE (the T1 pilot row 1)
				new GT6TooltipLine("gt6.tooltip.boiler.8", GT6TooltipStyle.ORANGE),  // :103 LH.NO_GUI_FUNNEL_TO_TANK
				new GT6TooltipLine("gt6.tooltip.boiler.9", GT6TooltipStyle.DRED),    // :104 LH.HAZARD_EXPLOSION_STEAM (the T1 pilot row 2)
				new GT6TooltipLine("gt6.tooltip.boiler.10", GT6TooltipStyle.DRED),   // :105 LH.HAZARD_MELTDOWN
				new GT6TooltipLine("gt6.tooltip.boiler.11", GT6TooltipStyle.DGRAY),  // :106 LH.TOOL_TO_DECALCIFY_CHISEL
				new GT6TooltipLine("gt6.tooltip.boiler.12", GT6TooltipStyle.DGRAY),  // :107 LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS
				new GT6TooltipLine("gt6.tooltip.boiler.13", GT6TooltipStyle.DGRAY),  // super :61 TOOL_TO_SET_FACING_PRE + Wrench (:82) + POST
				// row 14 is the port-authored annex (task r10-debt-boiler-heat-tip, issue #17
				// UX): the upstream addToolTips :95-109 carries NO output-condition line — the
				// >half-full gate is tick-body-only (MultiTileEntityBoilerTank.java:139-142 =
				// the port BE :281-287), and LH.java has no Heat/CONDENSE/half key to transplant
				new GT6TooltipLine("gt6.tooltip.boiler.14", GT6TooltipStyle.ORANGE)));
		// the Large Boiler family — MultiTileEntityLargeBoiler.addToolTips :150-167 (rows 1-15,
		// the STRUCTURE block :151-155 over the static :143-146 LH.add keys) + the multiblock
		// base rows (TileEntityBase10MultiBlockBase.java:100-101 = rows 16-17) + the facing
		// row (TileEntityBase09FacingSingle.java:61 = row 18); rows 8-11 ride the same
		// positional slots as the small boiler, the carrier hands [in, out, cap] per
		// LargeBoilerRow (NBT_OUTPUT_SU raw 4096..131072, same mOutput*10000 capacity shape :80);
		// row 19 = the port-authored output-condition annex (the small-boiler .14 sister line,
		// task r10-debt-boilerlarge-tip)
		register("boiler_large", List.of(
				new GT6TooltipLine("gt6.tooltip.boiler_large.1", GT6TooltipStyle.CYAN),    // :151 LH.STRUCTURE + ":"
				new GT6TooltipLine("gt6.tooltip.boiler_large.2", GT6TooltipStyle.WHITE),   // :152 the :143 line
				new GT6TooltipLine("gt6.tooltip.boiler_large.3", GT6TooltipStyle.WHITE),   // :153 the :144 line
				new GT6TooltipLine("gt6.tooltip.boiler_large.4", GT6TooltipStyle.WHITE),   // :154 the :145 line
				new GT6TooltipLine("gt6.tooltip.boiler_large.5", GT6TooltipStyle.WHITE),   // :155 the :146 line
				new GT6TooltipLine("gt6.tooltip.boiler_large.6", GT6TooltipStyle.CYAN),    // :156 LH.CONVERTS_FROM_X..
				new GT6TooltipLine("gt6.tooltip.boiler_large.7", GT6TooltipStyle.YELLOW),  // :157 getToolTipEfficiency (pristine)
				new GT6TooltipLine("gt6.tooltip.boiler_large.8", GT6TooltipStyle.GREEN),   // :158 LH.ENERGY_INPUT (Heat Transmitters)
				new GT6TooltipLine("gt6.tooltip.boiler_large.9", GT6TooltipStyle.GREEN),   // :159 LH.ENERGY_CAPACITY (HU)
				new GT6TooltipLine("gt6.tooltip.boiler_large.10", GT6TooltipStyle.RED),    // :160 LH.ENERGY_OUTPUT (Pipe Holes)
				new GT6TooltipLine("gt6.tooltip.boiler_large.11", GT6TooltipStyle.RED),    // :161 LH.ENERGY_CAPACITY (Steam)
				new GT6TooltipLine("gt6.tooltip.boiler_large.12", GT6TooltipStyle.ORANGE), // :162 LH.REQUIREMENT_WATER_PURE
				new GT6TooltipLine("gt6.tooltip.boiler_large.13", GT6TooltipStyle.DRED),   // :163 LH.HAZARD_EXPLOSION_STEAM
				new GT6TooltipLine("gt6.tooltip.boiler_large.14", GT6TooltipStyle.DRED),   // :164 LH.HAZARD_MELTDOWN
				new GT6TooltipLine("gt6.tooltip.boiler_large.15", GT6TooltipStyle.DGRAY),  // :165 LH.TOOL_TO_DECALCIFY_CHISEL
				new GT6TooltipLine("gt6.tooltip.boiler_large.16", GT6TooltipStyle.DGRAY),  // 10MultiBlockBase:100 LH.TOOL_TO_BUILD_BUILDER_WAND
				new GT6TooltipLine("gt6.tooltip.boiler_large.17", GT6TooltipStyle.DGRAY),  // 10MultiBlockBase:101 LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS
				new GT6TooltipLine("gt6.tooltip.boiler_large.18", GT6TooltipStyle.DGRAY),  // super :61 the facing row (Wrench)
				// row 19 is the port-authored annex (the small-boiler .14 sister line, task
				// r10-debt-boilerlarge-tip, issue #17 UX): the upstream addToolTips :150-167
				// carries NO output-condition line — the >half-full gate is tick-body-only
				// (MultiTileEntityLargeBoiler.java:203-207 = the port BE :417)
				new GT6TooltipLine("gt6.tooltip.boiler_large.19", GT6TooltipStyle.ORANGE)));
		// the Fluid Container (tank) family — TileEntityBase08FluidContainer.addToolTips
		// :99-111, the ONE pure-static row :100 (mTank.contentcap(), the empty-tank face =
		// "Capacity: <capacity> L"); the :101-110 rows are proof/drinkable conditionals and
		// the capacity arg is per-tank — DORMANT until the port has a tank registration
		// class to hand the carrier its capacity (the port has zero 08FluidContainer blocks)
		register("tank", List.of(
				new GT6TooltipLine("gt6.tooltip.tank.1", GT6TooltipStyle.CYAN)));
		// the Barrel family — TileEntityBase08Barrel.addToolTips :89-103, rows numbered by the
		// upstream row index (gaps = the ported-out rows). Rows 1-2 (:90 contentcap /
		// :91 "Sealed (n)") are the carry-state rows the design ruling DELETES (the canonical-TE
		// rebuild is a second-phase face — the registry must never carry them, pinned by
		// GT6TooltipsTest). Row 5 (:94 onlySimple) is the port's pool cut (onlySimple is a
		// constant false in the port BE base); rows 7-9 (:96-98 acid/plasma/magic proof) have
		// no port flag data (the GTBarrels P4 quartet cut) — a row claiming a capability the
		// port does not gate would be a lie, so they wait for the flags. The gas-proof row 6
		// (:95) IS port-gated (GTBarrelItemFluidHandler.setGasProof) — the wood barrel is the
		// one gas=F row, so it gets its own table and the 15 gas rows share the other.
		// ponytail: acid/plasma/magic proof rows return when the port models the flags (GTBarrels javadoc keeps the quartet noted)
		register("barrel", List.of(
				new GT6TooltipLine("gt6.tooltip.barrel.3", GT6TooltipStyle.ORANGE),   // :92 LH.NO_GUI_FUNNEL_TAP_TO_TANK
				new GT6TooltipLine("gt6.tooltip.barrel.4", GT6TooltipStyle.ORANGE),   // :93 LH.NO_POWER_CONDUCTING_FLUIDS
				new GT6TooltipLine("gt6.tooltip.barrel.10", GT6TooltipStyle.DRED),    // :99 LH.HAZARD_MELTDOWN + " (%s K)" — the carrier hands the row's melting point
				new GT6TooltipLine("gt6.tooltip.barrel.11", GT6TooltipStyle.DGRAY),   // :100 LH.TOOL_TO_TOGGLE_AUTO_OUTPUTS_MONKEY_WRENCH
				new GT6TooltipLine("gt6.tooltip.barrel.12", GT6TooltipStyle.DGRAY),   // :101 LH.TOOL_TO_TOGGLE_SOFT_HAMMER
				new GT6TooltipLine("gt6.tooltip.barrel.13", GT6TooltipStyle.DGRAY))); // :102 LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS
		// the gas-proof barrel table — the "barrel" table + row 6 (:95 LH.TOOLTIP_GASPROOF,
		// Chat.ORANGE), everything else identical (the wood barrel is the only gas=F port row)
		register("barrel_gas", List.of(
				new GT6TooltipLine("gt6.tooltip.barrel_gas.3", GT6TooltipStyle.ORANGE),   // :92
				new GT6TooltipLine("gt6.tooltip.barrel_gas.4", GT6TooltipStyle.ORANGE),   // :93
				new GT6TooltipLine("gt6.tooltip.barrel_gas.6", GT6TooltipStyle.ORANGE),   // :95 LH.TOOLTIP_GASPROOF
				new GT6TooltipLine("gt6.tooltip.barrel_gas.10", GT6TooltipStyle.DRED),    // :99 (%s K)
				new GT6TooltipLine("gt6.tooltip.barrel_gas.11", GT6TooltipStyle.DGRAY),   // :100
				new GT6TooltipLine("gt6.tooltip.barrel_gas.12", GT6TooltipStyle.DGRAY),   // :101
				new GT6TooltipLine("gt6.tooltip.barrel_gas.13", GT6TooltipStyle.DGRAY))); // :102

		// task r8-tooltip-multiblock-generator — the T4 three-family tables (tail-append,
		// the boiler band above stays the T1/T2 seam). Row numbers n = the upstream slot
		// position in the family's addToolTips sequence; the per-INSTANCE numeric faces
		// (energy rates, efficiency percent, recipes names, per-machine structure texts)
		// keep their slots EMPTY — a static family table can only carry family-constant
		// rows (the live numbers ride the Jade providers, design.r8-jade-tooltip).
		register("multiblock", List.of(
				new GT6TooltipLine("gt6.tooltip.multiblock.1", GT6TooltipStyle.DGRAY),   // TileEntityBase10MultiBlockBase.java:100 LH.TOOL_TO_BUILD_BUILDER_WAND
				new GT6TooltipLine("gt6.tooltip.multiblock.2", GT6TooltipStyle.DGRAY),   // :101 LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS
				new GT6TooltipLine("gt6.tooltip.multiblock.3", GT6TooltipStyle.DGRAY))); // TileEntityBase09FacingSingle.java:61 LH.TOOL_TO_SET_FACING_PRE + "Wrench" + POST (getFacingTool = TOOL_wrench, :82)
		register("converter", List.of(
				new GT6TooltipLine("gt6.tooltip.converter.4", GT6TooltipStyle.DGRAY),    // TileEntityBase11MultiBlockConverter.java:91-94 order — slots 1-3 = the per-instance energy-in/out + efficiency faces
				new GT6TooltipLine("gt6.tooltip.converter.5", GT6TooltipStyle.DGRAY),
				new GT6TooltipLine("gt6.tooltip.converter.6", GT6TooltipStyle.DGRAY)));
		register("generator", List.of(
				new GT6TooltipLine("gt6.tooltip.generator.4", GT6TooltipStyle.ORANGE),   // MultiTileEntityGeneratorSolid.java:89 LH.REQUIREMENT_AIR_IN_FRONT
				new GT6TooltipLine("gt6.tooltip.generator.5", GT6TooltipStyle.ORANGE),   // :90 LH.REQUIREMENT_EMPTY_ASHES + " (" + FACE_FRONT + ")"
				new GT6TooltipLine("gt6.tooltip.generator.6", GT6TooltipStyle.ORANGE),   // :91 LH.REQUIREMENT_IGNITE_FIRE + " (" + FACE_FRONT + ")"
				new GT6TooltipLine("gt6.tooltip.generator.7", GT6TooltipStyle.ORANGE),   // :92 LH.NO_GUI_CLICK_TO_INVENTORY + " (" + FACE_FRONT + ")"
				new GT6TooltipLine("gt6.tooltip.generator.8", GT6TooltipStyle.DRED),     // :93 LH.HAZARD_FIRE + " (4m)" (FLAME_RANGE = 3, :54)
				new GT6TooltipLine("gt6.tooltip.generator.9", GT6TooltipStyle.DRED),     // :94 LH.HAZARD_CONTACT + " (" + FACE_TOP + ")"
				new GT6TooltipLine("gt6.tooltip.generator.10", GT6TooltipStyle.DGRAY),   // :95 LH.TOOL_TO_REMOVE_SHOVEL
				new GT6TooltipLine("gt6.tooltip.generator.11", GT6TooltipStyle.DGRAY))); // TileEntityBase09FacingSingle.java:61 facing row (the super chain tail)

		// the electric wire family — MultiTileEntityWireElectric.addToolTips :126-132. The
		// three stat rows (:127-129) carry the per-variant constants through the carrier's
		// fallback array [voltage, tierName, amperage, loss] over positional slots (%1$s EU
		// (%2$s) = the VN[UT.Code.tierMin(mVoltage)] tier word, :127; %3$s = mAmperage, :128;
		// %4$s = makeString(mLoss), :129). Voltage/loss numerals ride the args, never the
		// literal text. ponytail: the upstream makeString underscore face for >=10000 values
		// is applied at the registration site, not re-derived here.
		register("wire", List.of(
				new GT6TooltipLine("gt6.tooltip.wire.1", GT6TooltipStyle.CYAN), // :127 WIRE_STATS_VOLTAGE + EU + VN[tier]
				new GT6TooltipLine("gt6.tooltip.wire.2", GT6TooltipStyle.CYAN), // :128 WIRE_STATS_AMPERAGE
				new GT6TooltipLine("gt6.tooltip.wire.3", GT6TooltipStyle.CYAN))); // :129 WIRE_STATS_LOSS + EU/m
		// the contact-damage sibling (the T2 barrel_gas precedent: the exception family gets
		// its own table over its own key prefix) — the SAME three stat rows + row 4 :130
		// HAZARD_CONTACT (Chat.DRED). Upstream gates the row on mContactDamage; the port
		// derives the flag per block (GTWireBlock.contactDamageOf — bare wires of the 28
		// shock-flagged rows only), so the registration site picks the sibling by the flag.
		register("wire_contact", List.of(
				new GT6TooltipLine("gt6.tooltip.wire_contact.1", GT6TooltipStyle.CYAN), // :127
				new GT6TooltipLine("gt6.tooltip.wire_contact.2", GT6TooltipStyle.CYAN), // :128
				new GT6TooltipLine("gt6.tooltip.wire_contact.3", GT6TooltipStyle.CYAN), // :129
				new GT6TooltipLine("gt6.tooltip.wire_contact.4", GT6TooltipStyle.DRED))); // :130 LH.HAZARD_CONTACT
		// the fluid pipe family — MultiTileEntityPipeFluid.addToolTips :215-228. Rows 1-2
		// (:216-217 bandwidth = makeString(mCapacity/2) + " L/t", capacity = makeString(mCapacity)
		// + " L") ride the carrier fallback [capacity/2, capacity]. Cut rows (upstream positions
		// preserved as gaps): row 3 :219 the >1-tank amount row (the port ships single-tank
		// pipes, GTFluidPipeBlockEntity :370 one slot), row 4 :220 the meltdown temperature
		// and rows 5-8 :221-224 the four proof flags (the port carries neither temperature nor
		// proof data — a row claiming an unported capability would be a lie, the barrel
		// ruling), row 9 :225 the contact-damage row (no port flag). Row 10 :226 rides.
		register("pipe_fluid", List.of(
				new GT6TooltipLine("gt6.tooltip.pipe_fluid.1", GT6TooltipStyle.CYAN),  // :216 PIPE_STATS_BANDWIDTH + L/t
				new GT6TooltipLine("gt6.tooltip.pipe_fluid.2", GT6TooltipStyle.CYAN),  // :217 PIPE_STATS_CAPACITY + L
				new GT6TooltipLine("gt6.tooltip.pipe_fluid.10", GT6TooltipStyle.DGRAY))); // :226 TOOL_TO_DETAIL_MAGNIFYINGGLASS
		// the item pipe family — MultiTileEntityPipeItem.addToolTips :115-120. Rows 1-2
		// (:116 stepsize = makeString(mStepSize), :117 bandwidth = makeString(getPipeCapacity())
		// + "/s" = invsize) ride the carrier fallback [stepSize, invSize]; the :117 WHITE value
		// segment collapses into the row's CYAN (one style per row, the T1 record shape).
		// Rows 3-4 (:119-120) are the input/output monkey wrench rows. The :118 super tail
		// (the fixed flammable/enchant/crowbar block) is the standing scope cut.
		register("pipe_item", List.of(
				new GT6TooltipLine("gt6.tooltip.pipe_item.1", GT6TooltipStyle.CYAN),  // :116 PIPE_STATS_STEPSIZE
				new GT6TooltipLine("gt6.tooltip.pipe_item.2", GT6TooltipStyle.CYAN),  // :117 PIPE_STATS_BANDWIDTH + /s
				new GT6TooltipLine("gt6.tooltip.pipe_item.3", GT6TooltipStyle.DGRAY), // :119 TOOL_TO_SET_INPUT_MONKEY_WRENCH
				new GT6TooltipLine("gt6.tooltip.pipe_item.4", GT6TooltipStyle.DGRAY))); // :120 TOOL_TO_SET_OUTPUT_MONKEY_WRENCH
		// the sensor family — MultiTileEntitySensor.addToolTips :88-97, the tool rows. Row 1
		// (:90 getSensorDescription) is the one PER-SENSOR abstract row — the port carries no
		// per-sensor description data, and a generic line would be a lie, so it waits (gap
		// kept at .1). Row 7 :96 = the facing row ("Use " + gt.lang.tool.name.wrench + " to set
		// Facing" — getFacingTool resolves to TOOL_wrench, TileEntityBase09FacingSingle:82)
		// fused into one key over the three upstream lang fragments.
		register("sensor", List.of(
				new GT6TooltipLine("gt6.tooltip.sensor.2", GT6TooltipStyle.ORANGE),  // :91 NO_GUI_CLICK_TO_INTERACT
				new GT6TooltipLine("gt6.tooltip.sensor.3", GT6TooltipStyle.DGRAY),   // :92 screwdrive.buttons
				new GT6TooltipLine("gt6.tooltip.sensor.4", GT6TooltipStyle.DGRAY),   // :93 screwdrive.display
				new GT6TooltipLine("gt6.tooltip.sensor.5", GT6TooltipStyle.DGRAY),   // :94 screwdrive.modes
				new GT6TooltipLine("gt6.tooltip.sensor.6", GT6TooltipStyle.DGRAY),   // :95 TOOL_TO_SET_INPUT_MONKEY_WRENCH
				new GT6TooltipLine("gt6.tooltip.sensor.7", GT6TooltipStyle.DGRAY))); // :96 TOOL_TO_SET_FACING_PRE + Wrench + POST
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
	 * T1-before state must not explode the families the later T cards have not tabled.
	 */
	public static void append(String aFamily, List<Component> aTooltip) {
		append(aFamily, aTooltip, new Object[0]);
	}

	/**
	 * The per-variant form (task r8-tooltip-boiler-tank): the registration site hands the
	 * row's own constants ([in, out, cap] for the boilers, [meltingPointK] for the barrels,
	 * [voltage, tierName, amperage, loss] for the wires, [capacity/2, capacity] for the fluid
	 * pipes, [stepSize, invSize] for the item pipes) and every line
	 * (%1$s/%2$s/%3$s — TranslatableContents.java:87-88 resolves the argument index, the
	 * vanilla 1.20.1 face). Lines with own args ignore the fallback.
	 */
	public static void append(String aFamily, List<Component> aTooltip, Object... aFallbackArgs) {
		List<GT6TooltipLine> tRows = REGISTRY.get(aFamily);
		if (tRows == null) return;
		for (GT6TooltipLine tRow : tRows) {
			aTooltip.add(tRow.component(aFallbackArgs));
		}
	}

	/**
	 * The upstream {@code UT.Code.makeString} display face (gregapi/util/UT.java:1296-1310,
	 * verbatim): plain digits below 10000, underscore thousands separators from 10000 up —
	 * the formatting the upstream loss/bandwidth/stepsize rows apply to their constants
	 * before the row leaves the addToolTips body. The registration sites format there and
	 * hand the STRING through the carrier's fallback array (the translatable arg slot).
	 */
	public static String makeString(long aNumber) {
		if (aNumber > -10000 && aNumber < 10000) return Long.toString(aNumber);
		StringBuilder rString = new StringBuilder();
		if (aNumber < 0) {
			rString.append('-');
			aNumber = -aNumber;
		}
		String tDigits = Long.toString(aNumber);
		for (int i = 0; i < tDigits.length(); i++) {
			if (i > 0 && (tDigits.length() - i) % 3 == 0) rString.append('_');
			rString.append(tDigits.charAt(i));
		}
		return rString.toString();
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
			return component(new Object[0]);
		}

		/** The per-variant form — a line without own constant args rides the carrier's fallback array. */
		Component component(Object... aFallbackArgs) {
			return Component.translatable(key, args.length > 0 ? args : aFallbackArgs).withStyle(style);
		}
	}
}
