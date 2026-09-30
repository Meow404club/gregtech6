package gregtech6.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import gregtech6.block.GTComposedNameItem;
import gregtech6.tooltip.GT6Tooltips;

/**
 * The unified machine-family BlockItem carrier (task tooltip-infra, design card T1):
 * every GT6 machine block item replays its family's static tooltip table through
 * {@link GT6Tooltips#append} — the registration sites swap their bare/composed BlockItem
 * for this class with the family short key (the twelve-key vocabulary lives on
 * {@link GT6Tooltips}).
 *
 * <p>Extends {@link GTComposedNameItem}, NOT BlockItem directly: the p20 composed-name
 * families (the boilers here, the burning boxes/crucibles/distillation towers next) name
 * their stacks through {@code Block#getName()} compose templates, and a bare BlockItem
 * stack would fall back to the raw description id — the carrier keeps that delegation so
 * the registration-site swap stays a one-line, zero-regression move (blocks with atomic
 * keys resolve the identical string, the GTComposedNameItem javadoc stance).
 *
 * <p>The dual-leg hover seam is the GTLightningRodBlock.Item:71-81 shape, ONE swap: on
 * forge the second parameter is the {@link Level} reference, on 1.21.1 it became
 * {@code Item.TooltipContext} (vanilla 1.21.1 Item.java:292 — the same four-argument
 * shape, a different carrier type). The super call runs first, then the family rows.
 */
public class GT6MachineBlockItem extends GTComposedNameItem {

	private final String mFamily;
	/** The row's own constants ([in, out, cap] for the boilers, [meltingPointK] for the barrels,
	 * [voltage, tierName, amperage, loss] for the wires, [capacity/2, capacity] for the fluid
	 * pipes, [stepSize, invSize] for the item pipes) — the fallback array of {@link GT6Tooltips#append(String, List, Object...)}. */
	private final Object[] mLineArgs;

	/** The T1 shape — an arg-less family table (every line carries its own constants). */
	public GT6MachineBlockItem(Block aBlock, Properties aProperties, String aFamily) {
		this(aBlock, aProperties, aFamily, new Object[0]);
	}

	/** The per-variant form (task tooltip-boiler-tank): the numeric rows ride positional slots. */
	public GT6MachineBlockItem(Block aBlock, Properties aProperties, String aFamily, Object... aLineArgs) {
		super(aBlock, aProperties);
		mFamily = aFamily;
		mLineArgs = aLineArgs;
	}

	/** The family short key ({@code boiler / machine / wire / ...} — the GT6Tooltips vocabulary). */
	public String family() {
		return mFamily;
	}

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
	//?} else {
	/*// 21.1: the Level second parameter became Item.TooltipContext (vanilla 1.21.1
	//Item.java:292 — the same four-argument shape, a different carrier type).
	@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
	*///?}
		GT6Tooltips.append(mFamily, aTooltip, mLineArgs);
	}
}
