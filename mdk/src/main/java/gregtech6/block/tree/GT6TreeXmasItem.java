package gregtech6.block.tree;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import gregtech6.easter.GT6Calendars;

/**
 * The Blue Spruce block item with the Christmas-in-July tooltip (task
 * easter-s3-xmas-seasonal): upstream BlockTreePlanks2.java:96-98 — and its seven family
 * twins (BlockTreePlanks2FireProof.java:96-97, BlockTreeBeamC.java:61-62,
 * BlockTreeBeamCFireProof.java:61-62, BlockTreeLogC.java:105-106,
 * BlockTreeLogCFireProof.java:103-104, BlockTreeSaplingCD.java:110-111,
 * BlockTreeLeavesCD.java:130-131 — the census is the 8-file grep of "Christmas in July"):
 * while {@code XMAS_IN_JULY} is up, the RAINBOW_SLOW-coloured line "Save on everything at
 * Christmas in July!" rides the tooltip. The {@code aMeta == 0} guard upstream selects the
 * Blue Spruce meta of the CD/AB family blocks — the port's per-pair blocks have no meta,
 * so the whole item IS the meta-0 row.
 *
 * <p>Carrier faces: the port has four of the eight upstream blocks (planks/log/sapling/
 * leaves — the two fireproof twins and the Blue Spruce BEAM have no port domain, the
 * port's {@link GT6BeamKind} rows are the vanilla-subset 8). XMAS_IN_DECEMBER deliberately
 * does NOT trigger the tooltip (upstream :96 checks XMAS_IN_JULY only) — it drives the
 * leaves texture swap instead. The RAINBOW_SLOW colour is the live
 * {@link GT6Calendars#rainbowSlow} cycle: appendHoverText runs per frame, so the colour
 * animates exactly like the upstream CLIENT_TIME switch (:571-582).
 */
public class GT6TreeXmasItem extends BlockItem {

	public GT6TreeXmasItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	// the hover-text seam moved between legs: 1.20.1 takes the @Nullable Level, 1.21.1 the
	// Item.TooltipContext record (Item.java:292, tmp/refs/vanilla-mc/1.21.1) — the shared
	// body rides addXmasLine
	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		addXmasLine(aTooltip, System.currentTimeMillis());
	}
	//? } else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		addXmasLine(aTooltip, System.currentTimeMillis());
	}*/
	//? }

	/**
	 * The census row body all 8 upstream carriers share (the XMAS_IN_JULY-gated
	 * {@code LH.Chat.RAINBOW_SLOW + "Save on everything at Christmas in July!"} line — the
	 * per-file differences are only the meta guards). Public static over an injected clock
	 * for the census pin, the {@link gregtech6.items.tools.GTPistolItem#foolDisplayName}
	 * posture.
	 */
	public static void addXmasLine(List<Component> aTooltip, long aEpochMillis) {
		if (GT6Calendars.XMAS_IN_JULY) {
			aTooltip.add(Component.literal("Save on everything at Christmas in July!")
					.withStyle(GT6Calendars.rainbowSlow(aEpochMillis)));
		}
	}
}
