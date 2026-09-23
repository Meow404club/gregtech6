package gregtech6.item.energy;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import gregapi.code.TagData;
import gregapi.data.TD;

/**
 * The Zero-Point-Module — task p36-energy-zpm-dechargers. Upstream files the ZPM as the
 * item-form MTE {@code MultiTileEntityZPM} (gregtech/tileentity/batteries/qu/…ZPM.java:43-83,
 * Loader_MultiTileEntities.java:1103, meta 14999, "Zero-Point-Module (ZPM)"): a
 * {@code TileEntityBase08Battery} of {@code NBT_CAPACITY 2_000_000_000_000L} riding
 * {@code TD.Energy.QU} with {@code NBT_INPUT V[7]=131072}, {@code NBT_INPUT_MIN 1} and
 * {@code NBT_INPUT_MAX VMAX[7]=262144} — the discharge band opens to [1..262144] so ANY
 * QU consumer packet size fits (the hand-rolled NBT_INPUT_MIN override of the :63-64
 * derived band, the {@link GT6BatteryItem} doc). ONE item — upstream has no tier family
 * here (the 37-row ladder closes at the Energium crystals; the ZPM is the single QU
 * artifact row, the card-① reconciliation).
 *
 * <h2>The discharge-only face (the :79-:80 verbatim, the card's biggest-risk pin)</h2>
 * <ul>
 * <li>{@code doEnergyInjection} returns 0 unconditionally (:79);</li>
 * <li>{@code canEnergyInjection} returns false unconditionally (:80) — the base face's
 *     type/count/band checks are ALL overridden away: the ZPM accepts NO charge from any
 *     source, ever. Downstream this makes every BatteryBox-family charge arm a no-op on
 *     a ZPM (the Base10 :113-:114 push arms call through and get 0), and the
 *     {@code gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity} intake guard
 *     (the Base10 :179 {@code mReceivablePower} seat) reads 0 chargeable count — the
 *     decharger REFUSES network intake exactly like upstream.</li>
 * </ul>
 *
 * <h2>The dual-form display (the :45-:52 + :78 carriers, the item posture)</h2>
 * Upstream the active/inert split is the block-form light value
 * ({@code IMTE_GetLightValue :78} — {@code mDisplayedEnergy} 0..15) plus the dim/active
 * texture pair (:60); the port ZPM is an ITEM (the battery family's item posture —
 * the port has no MTE layer, the 37-battery precedent), so the split rides the TOOLTIP
 * face verbatim: between the extremes the standard :107 charge line shows; empty OR
 * full shows the :49-:50 artifact pair ("An Ancient Artifact of huge Power" +
 * "Capacity: …"). The light-value and texture faces are declared CUT with the block
 * form (the render pool; p36-render-texture-bake posture). Both extremes stack to 16 —
 * the charged-single :143 face keeps the base behavior (charge &gt; 0 = stack 1, so a
 * FULL artifact still stacks 1; the upstream getMaxStackSize override :76 restores the
 * default for the ZPM: {@code aDefault} — the port keeps the base :143 narrowing, the
 * declared carrier simplification: a full ZPM is a single item either way).
 *
 * <p>Obtainment: upstream spawns it 2/3-FULL in the GT6 dungeon library rooms
 * (DungeonData.zpm:306-310 — {@code NBT_ACTIVE_ENERGY} = {@code next2in3()}); the port
 * rides the vanilla {@code chests/simple_dungeon} injection (GT6LootInjectionDatagen,
 * the P34 seam) with the full-active lane collapsed to always-full (the empty ZPM is an
 * unchargeable dead drop) — the declared deviation on the card face. Creative gives BOTH
 * the empty and the full stack (the :111-:117 getSubItems pair, the
 * {@code GT6Batteries} tab walk).
 */
public class GT6ZpmItem extends GT6BatteryItem {

	/** The upstream short name of the QU domain (TD.java:137 aLocalShort "QU"; the port drops the localized names — the GT6MachineProvider map face). */
	public static final String ENERGY_SHORT = "QU";

	/** The upstream :1103 columns ride the {@code GT6Batteries.ZPM} row (the single source; the test pins both faces). */
	public GT6ZpmItem(Properties aProperties, long aSizeRec, long aCapacity, long aSizeMin) {
		// the NBT_INPUT_MIN 1 / NBT_INPUT_MAX VMAX[7]=262144 explicit band (the :63-66
		// readFromNBT2 override ladder), TD.Energy.QU.
		super(aProperties, aSizeRec, aCapacity, aSizeMin, TD.Energy.QU);
	}

	// ---------------------------------------------------------------------------
	// the discharge-only face (the :79-:80 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean canEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize) {
		return false; // :80 — unconditional, the type/count/band base checks all overridden away
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoInject) {
		return 0; // :79
	}

	/** The creative pair — the :111-:117 getSubItems verbatim (empty + full). */
	public static ItemStack[] creativeStacks(GT6ZpmItem aItem) {
		ItemStack tEmpty = new ItemStack(aItem);
		ItemStack tFull = aItem.setEnergyStored(aItem.mType, new ItemStack(aItem), aItem.mCapacity);
		return new ItemStack[] {tEmpty, tFull};
	}

	// ---------------------------------------------------------------------------
	// the dual-form tooltip (the :45-:52 + :107 face; the block light/texture pair is
	// the declared cut — the item posture)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aStack, aTooltip);
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aStack, aTooltip);
	}
	 *///?}

	/** The :45-:52 split — between the extremes the :107 charge line, else the artifact pair. */
	private void tooltipLines(ItemStack aStack, List<Component> aTooltip) {
		long tStored = readStored(this, aStack);
		if (tStored > 0 && tStored < mCapacity) { // UT.Code.inside(1, mCapacity-1, mEnergy) :46
			// the :107 charge line — "X / Y QU - Size: up to Z" (sizeMin==1 → the up-to form)
			aTooltip.add(Component.literal(makeString(Math.min(mCapacity, tStored)) + " / " + makeString(mCapacity)
					+ " " + ENERGY_SHORT + " - Size: up to " + mSizeMax).withStyle(ChatFormatting.WHITE));
		} else {
			// the :49-:50 artifact pair (CYAN + WHITE, the LH.Chat literal colors)
			aTooltip.add(Component.literal("An Ancient Artifact of huge Power").withStyle(ChatFormatting.AQUA));
			aTooltip.add(Component.literal("Capacity: " + makeString(mCapacity) + " " + ENERGY_SHORT).withStyle(ChatFormatting.WHITE));
		}
	}

	/** The upstream UT.Code.makeString (UT.java:1296 verbatim) — underscores as decimal separators. */
	public static String makeString(long aNumber) {
		if (aNumber > -10000 && aNumber < 10000) return Long.toString(aNumber);
		StringBuilder rString = new StringBuilder();
		if (aNumber < 0) {
			aNumber *= -1;
			rString.append('-');
		}
		boolean tLeading = true;
		for (long i = 1000000000000000000L; i > 0; i /= 10) {
			long tDigit = (aNumber / i) % 10;
			if (tLeading && tDigit != 0) tLeading = false;
			if (!tLeading) {
				rString.append(tDigit);
				if (i != 1) for (long j = i; j > 0; j /= 1000) if (j == 1) rString.append('_');
			}
		}
		return rString.toString();
	}
}
