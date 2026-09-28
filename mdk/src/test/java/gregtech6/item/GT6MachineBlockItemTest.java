package gregtech6.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.GTComposedNameItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The carrier gate (task r8-tooltip-infra acceptance ①): {@link GT6MachineBlockItem}
 * replays the family row table through the offline {@code appendHoverText} direct call —
 * the boiler fixture (row table present) appends the two pilot rows after the vanilla
 * super chain, the untabled family appends ZERO. The test call itself is the dual-leg
 * compile pin: this file compiles per leg against the REAL vanilla signature (forge =
 * the {@code Level} reference, 1.21.1 = {@code Item.TooltipContext}), so a leg drift on
 * the carrier breaks compileTestJava before any assertion runs.
 *
 * <p>The name-delegation pin guards the extends-{@link GTComposedNameItem} stance: the
 * composed-name families keep naming their stacks through the block face.
 */
public class GT6MachineBlockItemTest extends GTOfflineTestBase {

	static GT6MachineBlockItem sBoiler;
	static GT6MachineBlockItem sUntable;

	@BeforeAll
	static void buildFixtures() {
		// the vanilla-block carriers — registration sites hand real blocks, the tooltip
		// face reads nothing else off the block
		sBoiler = registerItemFixture("fixture_tooltip_pilot_boiler",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "boiler"));
		sUntable = registerItemFixture("fixture_tooltip_pilot_untabled",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "tank"));
	}

	/** The leg-swap hover call — ONE swap, the GTLightningRodBlock.Item:71-81 shape. */
	private static void callHoverText(GT6MachineBlockItem aItem, ItemStack aStack, List<Component> aTooltip) {
		//? if forge {
		aItem.appendHoverText(aStack, null, aTooltip, TooltipFlag.NORMAL);
		//?} else {
		/*aItem.appendHoverText(aStack, Item.TooltipContext.EMPTY, aTooltip, TooltipFlag.NORMAL);
		*///?}
	}

	@Test
	public void boilerFamilyAppendsThePilotRowsAfterSuper() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sBoiler, new ItemStack(sBoiler), tTooltip);
		assertEquals(2, tTooltip.size(), "the boiler family replays exactly the two pilot rows");
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		TranslatableContents tRow1 = assertInstanceOf(TranslatableContents.class, tTooltip.get(1).getContents());
		assertEquals("gt6.tooltip.boiler.7", tRow0.getKey());
		assertEquals("gt6.tooltip.boiler.9", tRow1.getKey());
		// withStyle(ChatFormatting) stores TextColor.fromLegacyFormat — the value comparison
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), tTooltip.get(0).getStyle().getColor());     // Chat.ORANGE
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED), tTooltip.get(1).getStyle().getColor()); // Chat.DRED
	}

	@Test
	public void familyWithoutRowTableAppendsNothing() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sUntable, new ItemStack(sUntable), tTooltip);
		assertTrue(tTooltip.isEmpty(), "an unregistered family (tank pre-T2) appends ZERO lines");
	}

	@Test
	public void stackNameDelegatesToTheBlockFace() {
		// the extends-GTComposedNameItem stance: the stack name rides Block#getName
		// (the composed-name families keep their template faces through the carrier swap)
		ItemStack tStack = new ItemStack(sBoiler);
		assertEquals(Blocks.BRICKS.getName().getString(), sBoiler.getName(tStack).getString());
		assertInstanceOf(GTComposedNameItem.class, sBoiler);
	}
}
