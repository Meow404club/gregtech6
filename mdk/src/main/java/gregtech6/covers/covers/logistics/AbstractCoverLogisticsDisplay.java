package gregtech6.covers.covers.logistics;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.tooltip.GT6TooltipStyle;

/**
 * The logistics CPU-display base — 1.20.1 port of gregapi/cover/covers/
 * AbstractCoverAttachmentLogisticsDisplay.java (:34-55), task logistics-covers-12.
 * The upstream body is 20 lines: the redstone emission is the value lane bound to 0-15
 * (:42-49 {@code UT.Code.bind4(mValues)}), the visuals lane persists (:52), and the
 * priority/stacksize lanes are OFF (:53-54).
 */
public abstract class AbstractCoverLogisticsDisplay extends AbstractCoverAttachmentLogistics {

	/** Upstream :42-49 — the value lane IS the redstone level (bind4 keeps it in 0..15). */
	@Override
	public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return (byte) Math.max(0, Math.min(15, aData.mValues[aCoverSide])); // UT.Code.bind4
	}

	@Override
	public byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return getRedstoneOutWeak(aCoverSide, aData, aDefaultRedstone); // :42/:47 the identical pair
	}

	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true; // :52 — the display bar rides the visual lane through the save
	}

	@Override
	public boolean usePriorities() {
		return false; // :54
	}

	@Override
	public boolean useTargetStackSize() {
		return false; // :53
	}

	/** Upstream :301/:306/:311/:316 — the display-bar arithmetic the Core drives per second (visual 0..10). */
	public static int displayVisual(int aUsed, int aTotal) {
		if (aUsed <= 0) return 0;
		if (aUsed >= aTotal) return 10;
		return 9 - (int)Math.max(0, Math.min(8, ((long)(aTotal - aUsed) * 9L) / aTotal));
	}

	/** Upstream :301/:306/:311/:316 — the redstone value twin of {@link #displayVisual}. */
	public static int displayValue(int aUsed, int aTotal) {
		if (aUsed <= 0) return 0;
		if (aUsed >= aTotal) return 15;
		return 14 - (int)Math.max(0, Math.min(13, ((long)(aTotal - aUsed) * 14L) / aTotal));
	}

	/** The display family answer — the CoverData visual lane picks the 0..10 bar sprite (the underlay folds in at 0). */
	public ResourceLocation displayTexture(CoverData aData, byte aSide, String aFamily) {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/display/" + aFamily + "/" + Math.max(0, Math.min(10, aData.mVisuals[aSide])));
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/display/" + aFamily + "/" + Math.max(0, Math.min(10, aData.mVisuals[aSide])));
		 *///?}
	}

	/**
	 * Upstream AbstractCoverAttachmentLogisticsDisplay:36-39 — the DGRAY status face (the
	 * upstream hardcoded-en literal); the super chain adds the controller row (both port
	 * predicates are F on this family, the class doc), task tooltip-cover-face.
	 */
	@Override
	public void addToolTips(List<Component> aList, ItemStack aStack, boolean aF3_H) {
		super.addToolTips(aList, aStack, aF3_H); // the logistics chain (the base + controller rows)
		aList.add(Component.translatable("gt6.tooltip.cover.logistics_display_status").withStyle(GT6TooltipStyle.DGRAY)); // :38
	}
}
