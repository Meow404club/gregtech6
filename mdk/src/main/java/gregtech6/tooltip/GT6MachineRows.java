package gregtech6.tooltip;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import gregtech6.block.GTBasicMachineBlock;
import gregtech6.jei.GT6RecipeMapViewerMeta;
import gregtech6.recipes.RecipeMap;
import gregtech6.tooltip.GT6Tooltips.GT6TooltipLine;

import gregapi.code.TagData;

/**
 * The BasicMachine family row table (task tooltip-basic-machine-family): ONE pure
 * transcription of the upstream {@code MultiTileEntityBasicMachine.addToolTips +
 * addToolTipsSided} (gt6-1.7.10 gregapi/tileentity/machines/MultiTileEntityBasicMachine
 * .java:260-345) parameterized by the registration-time config the BE factories hold —
 * the {@link GTBasicMachineBlock.MachineRow} columns for the row-carrying families, the
 * port-default form for the legacy tierOf trio/oven.
 *
 * <p>Rows register per ITEM under the namespaced family key {@code machine:<registry
 * path>} (the carrier family string rides the same form) — one table per machine, the
 * GT6Tooltips registry lookup unchanged. The upstream slot numbering is kept WITH gaps
 * so a restored slot never renumbers its successors:
 * <ol>
 * <li>{@code :261} Recipes (+ the {@code (up to Nx processed per run)} parallel suffix)</li>
 * <li>{@code :264} Cheap Overclocking — conditional</li>
 * <li>{@code :266} Efficiency (LH.getToolTipEfficiency, the :270 percent form) — when != 10000</li>
 * <li>{@code :288-294} Energy IN (LH.addEnergyToolTips :352 single-row form)</li>
 * <li><i>gap 5</i> — the upstream charged-energy rows (:295-301): no ported BasicMachine
 * carries a charged energy type (the mEnergyTypeCharged pool is cut)</li>
 * <li>6-9 {@code :302-345} Items IN/OUT + Fluids IN/OUT — the three upstream forms per
 * face (specific sides / {@code (auto, otherwise any)} / {@code Any Side (no auto)})</li>
 * <li>10 {@code :271} ignition requirement — the Burner Mixer family only (the one
 * {@code mRequiresIgnition = true} carrier, TileEntityBurnerMixer.java:43; the Oven cut it)</li>
 * <li>11-14, 16 {@code :273-281} the tool rows</li>
 * <li><i>gap 15</i> — the Builder Wand row (:280) is multiblock-only upstream, dead for
 * every BasicMachine</li>
 * </ol>
 *
 * <p>Deviations, declared: the composed segments upstream wrote inline ({@code (auto)},
 * {@code (no auto)}, {@code (auto, otherwise any)}, the parallel suffix, the energy
 * window words) stay Java literals — they are NOT LH constants, so the 1.7.10 zh face
 * rendered them English too; the face WORDS were LH constants, so they ride the seven
 * shared {@code gt6.tooltip.face.*} keys. The legacy trio/oven transcribe the PORT's
 * live IO config (the 127 all-sides defaults, no auto-IO pool — the port's declared
 * legacy-family simplification), not their upstream NBT columns. The upstream
 * {@code tMin <= 1} "(up to" energy branch (:352) is dead in the port — every voltage
 * window (TIER_INPUTS/ULV_TIER_INPUTS/EV_TIER_INPUTS) carries min ≥ 4 — and is cut.
 */
public final class GT6MachineRows {

	/** The slot keys — n = the upstream row slot (gaps documented in the class javadoc). */
	public static final String KEY_RECIPES = "gt6.tooltip.machine.1";
	public static final String KEY_CHEAP_OVERCLOCKING = "gt6.tooltip.machine.2";
	public static final String KEY_EFFICIENCY = "gt6.tooltip.machine.3";
	public static final String KEY_ENERGY_IN = "gt6.tooltip.machine.4";
	public static final String KEY_ITEMS_IN = "gt6.tooltip.machine.6";
	public static final String KEY_ITEMS_OUT = "gt6.tooltip.machine.7";
	public static final String KEY_FLUIDS_IN = "gt6.tooltip.machine.8";
	public static final String KEY_FLUIDS_OUT = "gt6.tooltip.machine.9";
	public static final String KEY_IGNITION = "gt6.tooltip.machine.10";
	public static final String KEY_TOOL_SCREWDRIVER = "gt6.tooltip.machine.11";
	public static final String KEY_TOOL_WRENCH_INPUTS = "gt6.tooltip.machine.12";
	public static final String KEY_TOOL_WRENCH_OUTPUTS = "gt6.tooltip.machine.13";
	public static final String KEY_TOOL_SOFT_HAMMER = "gt6.tooltip.machine.14";
	public static final String KEY_TOOL_MAGNIFIER = "gt6.tooltip.machine.16";

	/** The face words (upstream LH.FACES + FACE_ANY, LH.java:469-471 en VERBATIM) — shared with the T4/T5 row tables. */
	public static final String[] FACE_KEYS = {
			"gt6.tooltip.face.bottom", "gt6.tooltip.face.top", "gt6.tooltip.face.left",
			"gt6.tooltip.face.front", "gt6.tooltip.face.right", "gt6.tooltip.face.back"};
	public static final String FACE_ANY_KEY = "gt6.tooltip.face.any";

	/**
	 * The registration-time config — the upstream BE fields the rows read, flattened.
	 * Masks ride the GTBasicMachineBlock SBIT_* convention (bit = 1 &lt;&lt; side, side order
	 * bottom/top/left/front/right/back, 127 = the all-sides default); the auto sides ride
	 * the raw SIDE_* bytes (valid 0-5, -1 = undefined).
	 */
	public record Spec(RecipeMap recipes, int parallel, boolean cheapOverclocking, boolean requiresIgnition,
			@Nullable Integer efficiency, @Nullable TagData energyType,
			long energyMin, long energyRec, long energyMax, byte energySides,
			byte itemIn, byte itemOut, byte fluidIn, byte fluidOut,
			byte itemAutoIn, byte itemAutoOut, byte fluidAutoIn, byte fluidAutoOut) {}

	/** The MachineRow adapter — the voltage window mirrors the BE factories ({@code machine}/machineUlv/euFiveTierMachine). */
	public static Spec spec(GTBasicMachineBlock.MachineRow aRow) {
		return spec(aRow, false);
	}

	/** The MachineRow adapter with the ignition override (the Burner Mixer family, the {@code :43 mRequiresIgnition = true} carrier). */
	public static Spec spec(GTBasicMachineBlock.MachineRow aRow, boolean aIgnition) {
		long[] tWindow = aRow.ulvVoltage()
				? gregtech6.registry.GTMachines.ULV_TIER_INPUTS
				: gregtech6.registry.GTMachines.euFiveTierWindow(aRow.tier());
		return new Spec(aRow.recipes().get(), aRow.parallel(), aRow.cheapOverclocking(), aIgnition, aRow.efficiency(),
				aRow.energyType(), tWindow[0], tWindow[1], tWindow[2], aRow.energySides(),
				aRow.itemIn(), aRow.itemOut(), aRow.fluidIn(), aRow.fluidOut(),
				aRow.itemAutoIn(), aRow.itemAutoOut(), aRow.fluidAutoIn(), aRow.fluidAutoOut());
	}

	/** The row-table registration for one MachineRow (the GTMachines walk seam; key = {@code machine:<path>}). */
	public static void register(GTBasicMachineBlock.MachineRow aRow) {
		register(aRow, false);
	}

	/** The MachineRow registration with the ignition override. */
	public static void register(GTBasicMachineBlock.MachineRow aRow, boolean aIgnition) {
		register("machine:" + aRow.path(), spec(aRow, aIgnition));
	}

	/** The row-table registration for a hand-built spec (the legacy trio/oven seam). */
	public static void register(String aKey, Spec aSpec) {
		GT6Tooltips.register(aKey, rows(aSpec));
	}

	/**
	 * The pure transcription — upstream addToolTips:260-284 then addToolTipsSided:286-345,
	 * row for row (the class javadoc carries the per-slot anchors and the declared gaps).
	 */
	public static List<GT6TooltipLine> rows(Spec aSpec) {
		List<GT6TooltipLine> rRows = new ArrayList<>(16);

		// :261 — CYAN "Recipes: " + WHITE map name + the optional parallel suffix (an
		// upstream :261 inline literal, composed into the WHITE segment). The map name rides
		// the shared viewer title key (task lang-batch2-fixes) — the literal mNameLocal was
		// the row table's last English-only face; gt6.jei.recipe_map.* carries both locales
		// (72 maps, task r6-29-34a), the same formula the JEI/EMI categories resolve.
		MutableComponent tName = Component.translatable(GT6RecipeMapViewerMeta.titleKey(aSpec.recipes())).withStyle(GT6TooltipStyle.WHITE);
		if (aSpec.parallel() > 1) tName.append(" (up to " + aSpec.parallel() + "x processed per run)");
		rRows.add(new GT6TooltipLine(KEY_RECIPES, GT6TooltipStyle.CYAN, tName));

		// :263-264
		if (aSpec.cheapOverclocking()) rRows.add(new GT6TooltipLine(KEY_CHEAP_OVERCLOCKING, GT6TooltipStyle.YELLOW));

		// :265-266 — LH.getToolTipEfficiency (LH.java:311): the :270 percent form, WHITE value
		if (aSpec.efficiency() != null && aSpec.efficiency() != 10000) {
			rRows.add(new GT6TooltipLine(KEY_EFFICIENCY, GT6TooltipStyle.YELLOW, white(percent(aSpec.efficiency()))));
		}

		// addToolTipsSided :288-294 — the energy row (LH.addEnergyToolTips :352); the charged
		// call (:295-301) has no ported carrier, slot 5 stays a gap
		if (aSpec.energyType() != null) {
			rRows.add(new GT6TooltipLine(KEY_ENERGY_IN, GT6TooltipStyle.GREEN, energyValue(aSpec)));
		}

		// :302-312 items IN (GREEN) / :313-323 items OUT (RED)
		ioRow(rRows, KEY_ITEMS_IN, GT6TooltipStyle.GREEN, aSpec.itemIn(), aSpec.itemAutoIn(),
				aSpec.recipes().mInputItemsCount > 0);
		ioRow(rRows, KEY_ITEMS_OUT, GT6TooltipStyle.RED, aSpec.itemOut(), aSpec.itemAutoOut(),
				aSpec.recipes().mOutputItemsCount > 0);

		// :324-334 fluids IN (GREEN) / :335-345 fluids OUT (RED)
		ioRow(rRows, KEY_FLUIDS_IN, GT6TooltipStyle.GREEN, aSpec.fluidIn(), aSpec.fluidAutoIn(),
				aSpec.recipes().mInputFluidCount > 0);
		ioRow(rRows, KEY_FLUIDS_OUT, GT6TooltipStyle.RED, aSpec.fluidOut(), aSpec.fluidAutoOut(),
				aSpec.recipes().mOutputFluidCount > 0);

		// :270-271
		if (aSpec.requiresIgnition()) rRows.add(new GT6TooltipLine(KEY_IGNITION, GT6TooltipStyle.ORANGE));

		// :273-281 — the tool rows (the :279 Builder Wand arm is multiblock-only, gap 15;
		// the wrench gates read the auto-IO columns per the upstream SIDES_VALID test)
		rRows.add(new GT6TooltipLine(KEY_TOOL_SCREWDRIVER, GT6TooltipStyle.DGRAY));
		if (sideValid(aSpec.fluidAutoIn()) || sideValid(aSpec.itemAutoIn())) {
			rRows.add(new GT6TooltipLine(KEY_TOOL_WRENCH_INPUTS, GT6TooltipStyle.DGRAY));
		}
		if (sideValid(aSpec.fluidAutoOut()) || sideValid(aSpec.itemAutoOut())) {
			rRows.add(new GT6TooltipLine(KEY_TOOL_WRENCH_OUTPUTS, GT6TooltipStyle.DGRAY));
		}
		rRows.add(new GT6TooltipLine(KEY_TOOL_SOFT_HAMMER, GT6TooltipStyle.DGRAY));
		rRows.add(new GT6TooltipLine(KEY_TOOL_MAGNIFIER, GT6TooltipStyle.DGRAY));

		return List.copyOf(rRows);
	}

	/**
	 * The upstream :352 energy value — {@code <rec> <unit>/t} plus the window tail; the
	 * 127 mask skips the side list (:289-291), the {@code tMin <= 1 "(up to"} branch is
	 * cut (dead in the port, class javadoc).
	 */
	private static MutableComponent energyValue(Spec aSpec) {
		MutableComponent rValue = Component.literal(aSpec.energyRec() + " " + shortType(aSpec.energyType())).withStyle(GT6TooltipStyle.WHITE);
		if (aSpec.energyRec() == aSpec.energyMin() && aSpec.energyRec() == aSpec.energyMax()) {
			rValue.append("/t");
		} else {
			rValue.append("/t (").append(String.valueOf(aSpec.energyMin())).append(" to ").append(String.valueOf(aSpec.energyMax()));
			if (aSpec.energySides() != 127) {
				rValue.append(", ");
				appendSides(rValue, aSpec.energySides(), (byte)-1);
			}
			rValue.append(")");
		}
		return rValue;
	}

	/**
	 * The three upstream IO forms per face (:302-345): the specific-side list with the
	 * {@code (auto)} marker, the {@code <face> (auto, otherwise any)} form, or the
	 * {@code Any Side (no auto)} form; an empty specific list adds NO row (the upstream
	 * {@code stringValid} gate).
	 */
	private static void ioRow(List<GT6TooltipLine> aRows, String aKey, ChatFormatting aStyle, byte aMask, byte aAuto, boolean aPresent) {
		if (!aPresent) return;
		if (aMask != 127) {
			if (faceCount(aMask) > 0) {
				MutableComponent tSides = Component.literal("").withStyle(GT6TooltipStyle.WHITE);
				appendSides(tSides, aMask, aAuto);
				aRows.add(new GT6TooltipLine(aKey, aStyle, tSides));
			}
		} else if (sideValid(aAuto)) {
			// :308/:319/:330/:340 — the face stays a translatable (the :308 inline tail is a literal)
			MutableComponent tFace = Component.translatable(FACE_KEYS[aAuto]).withStyle(GT6TooltipStyle.WHITE);
			tFace.append(" (auto, otherwise any)");
			aRows.add(new GT6TooltipLine(aKey, aStyle, tFace));
		} else {
			MutableComponent tFace = Component.translatable(FACE_ANY_KEY).withStyle(GT6TooltipStyle.WHITE);
			tFace.append(" (no auto)");
			aRows.add(new GT6TooltipLine(aKey, aStyle, tFace));
		}
	}

	/** The side list {@code "Bottom, Top"} (+ the {@code (auto)} marker on the auto side), upstream :290/:304 — separators BETWEEN entries. */
	private static void appendSides(MutableComponent aTarget, byte aMask, byte aAuto) {
		boolean tFirst = true;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if ((aMask & (1 << tSide)) != 0) {
				if (!tFirst) aTarget.append(", ");
				tFirst = false;
				aTarget.append(Component.translatable(FACE_KEYS[tSide]));
				if (tSide == aAuto) aTarget.append(" (auto)");
			}
		}
	}

	/** CS.java:598 FACE_CONNECTED = the plain bit test; sides 0-5 only (ALL_SIDES_VALID, CS.java:671). */
	private static int faceCount(byte aMask) {
		int rCount = 0;
		for (byte tSide = 0; tSide < 6; tSide++) if ((aMask & (1 << tSide)) != 0) rCount++;
		return rCount;
	}

	/** CS.java:699 SIDES_VALID — the auto-side bytes 0-5, -1 (SIDE_UNDEFINED) invalid. */
	private static boolean sideValid(byte aSide) {
		return aSide >= 0 && aSide <= 5;
	}

	/** The WHITE value segment wrapper (the upstream Chat.WHITE slot inside a colored row). */
	private static MutableComponent white(String aText) {
		return Component.literal(aText).withStyle(GT6TooltipStyle.WHITE);
	}

	/** The upstream LH.java:270 percent form verbatim (10000 → "100.00"). */
	private static String percent(long aNumber) {
		return (aNumber / 100) + ((aNumber % 100) > 9 ? "." + aNumber % 100 : ".0" + (aNumber % 100));
	}

	/**
	 * The energy-type unit word — the GT6ConverterProvider.shortType identity table, kept
	 * LOCAL on purpose: that class implements the Jade API, and class-loading it from the
	 * tooltip path would drag the Jade classes into a Jade-less install (NoClassDefFoundError).
	 */
	private static String shortType(TagData aType) {
		if (aType == gregapi.data.TD.Energy.EU) return "EU";
		if (aType == gregapi.data.TD.Energy.RU) return "RU";
		if (aType == gregapi.data.TD.Energy.KU) return "KU";
		if (aType == gregapi.data.TD.Energy.HU) return "HU";
		return aType.mName;
	}

	private GT6MachineRows() {}
}
