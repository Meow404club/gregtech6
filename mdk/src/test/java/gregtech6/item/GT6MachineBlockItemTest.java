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
 * The carrier gate (task r8-tooltip-infra acceptance ①, task r8-tooltip-boiler-tank
 * expansion): {@link GT6MachineBlockItem} replays the family row table through the offline
 * {@code appendHoverText} direct call — the boiler fixture appends the full 13-row table
 * after the vanilla super chain (the T1 pilot pair pinned at rows 7/9 inside it), the
 * arg-less vs per-variant carrier forms both pinned, the untabled family appends ZERO.
 * The test call itself is the dual-leg compile pin: this file compiles per leg against the
 * REAL vanilla signature (forge = the {@code Level} reference, 1.21.1 =
 * {@code Item.TooltipContext}), so a leg drift on the carrier breaks compileTestJava
 * before any assertion runs.
 *
 * <p>The name-delegation pin guards the extends-{@link GTComposedNameItem} stance: the
 * composed-name families keep naming their stacks through the block face.
 */
public class GT6MachineBlockItemTest extends GTOfflineTestBase {

	static GT6MachineBlockItem sBoiler;
	static GT6MachineBlockItem sBoilerArgs;
	static GT6MachineBlockItem sUntable;

	@BeforeAll
	static void buildFixtures() {
		// the vanilla-block carriers — registration sites hand real blocks, the tooltip
		// face reads nothing else off the block
		sBoiler = registerItemFixture("fixture_tooltip_pilot_boiler",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "boiler"));
		sBoilerArgs = registerItemFixture("fixture_tooltip_pilot_boiler_args",
				// the per-variant form: the registration-site constants (a lead boiler tank's
				// [in=16 HU/t, out=32 Steam/t, cap=320000] — the :98-:101 slots)
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "boiler", 16, 32, 320000));
		sUntable = registerItemFixture("fixture_tooltip_pilot_untabled",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "machine"));
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
	public void boilerFamilyAppendsTheFullRowTableAfterSuper() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sBoiler, new ItemStack(sBoiler), tTooltip);
		assertEquals(13, tTooltip.size(), "the boiler family replays the full 13-row table");
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		TranslatableContents tRow6 = assertInstanceOf(TranslatableContents.class, tTooltip.get(6).getContents());
		TranslatableContents tRow8 = assertInstanceOf(TranslatableContents.class, tTooltip.get(8).getContents());
		assertEquals("gt6.tooltip.boiler.1", tRow0.getKey());
		assertEquals("gt6.tooltip.boiler.7", tRow6.getKey()); // the T1 pilot row 1
		assertEquals("gt6.tooltip.boiler.9", tRow8.getKey()); // the T1 pilot row 2
		// withStyle(ChatFormatting) stores TextColor.fromLegacyFormat — the value comparison
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), tTooltip.get(0).getStyle().getColor());      // Chat.CYAN
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), tTooltip.get(6).getStyle().getColor());      // Chat.ORANGE
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED), tTooltip.get(8).getStyle().getColor());  // Chat.DRED
	}

	@Test
	public void carrierLineArgsRideTheNumericRows() {
		// the per-variant registration form: the arg-less rows take the carrier array into
		// the positional slots (a lead boiler tank's [16 HU/t, 32 Steam/t, 320000])
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sBoilerArgs, new ItemStack(sBoilerArgs), tTooltip);
		TranslatableContents tRow3 = assertInstanceOf(TranslatableContents.class, tTooltip.get(2).getContents());
		assertEquals("gt6.tooltip.boiler.3", tRow3.getKey());
		assertEquals(3, tRow3.getArgs().length);
		assertEquals(16, tRow3.getArgs()[0]);
		assertEquals(32, tRow3.getArgs()[1]);
		assertEquals(320000, tRow3.getArgs()[2]);
	}

	@Test
	public void familyWithoutRowTableAppendsNothing() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sUntable, new ItemStack(sUntable), tTooltip);
		assertTrue(tTooltip.isEmpty(), "an unregistered family (machine pre-T3) appends ZERO lines");
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
