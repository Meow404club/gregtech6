package gregtech6.tooltip;

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

import gregtech6.block.pipe.GTFluidPipeBlockItem;
import gregtech6.block.pipe.GTItemPipeBlockItem;
import gregtech6.block.wire.GTWireBlockItem;
import gregtech6.item.GT6MachineBlockItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The four connector registration points' carrier gate (task r8-tooltip-wire-pipe-sensor
 * acceptance ②): the registration-site item classes — {@link GTWireBlockItem},
 * {@link GTFluidPipeBlockItem}, {@link GTItemPipeBlockItem} (the specialized onPlaced
 * carriers, parents moved to the machine carrier) and the bare
 * {@link GT6MachineBlockItem} swap at {@code GT6Sensors} — replay their family row table
 * through the offline {@code appendHoverText} direct call, the GT6MachineBlockItemTest
 * form. Vanilla blocks stand in for the real registration blocks: the tooltip face reads
 * nothing off the block, only the family key + the per-variant constants the site hands.
 *
 * <p>The name pins guard the parent swap (the r8 coordinator check a): the
 * compose-less vanilla block resolves the identical string through the inherited
 * {@code GTComposedNameItem.getName} (block-face delegation) that the raw
 * {@code BlockItem} base resolved — and {@code GTWireBlockItem} rides the same delegation
 * its retired local override made. The test call itself is the dual-leg compile pin: this
 * file compiles per leg against the REAL vanilla signature.
 */
public class GT6ConnectorCarrierTooltipTest extends GTOfflineTestBase {

	static GT6MachineBlockItem sWire;
	static GT6MachineBlockItem sWireContact;
	static GT6MachineBlockItem sFluidPipe;
	static GT6MachineBlockItem sItemPipe;
	static GT6MachineBlockItem sSensor;

	@BeforeAll
	static void buildFixtures() {
		// the registration-site argument faces verbatim (GTWires / GTFluidPipes / GTItemPipes
		// / GT6Sensors hand these constants through the carrier ctor)
		sWire = registerItemFixture("fixture_connector_wire",
				() -> new GTWireBlockItem(Blocks.BRICKS, new Item.Properties(), "wire", 32L, "LV", 1L, "1"));
		sWireContact = registerItemFixture("fixture_connector_wire_contact",
				() -> new GTWireBlockItem(Blocks.BRICKS, new Item.Properties(), "wire_contact", 128L, "MV", 1L, "2"));
		sFluidPipe = registerItemFixture("fixture_connector_fluid_pipe",
				() -> new GTFluidPipeBlockItem(Blocks.BRICKS, new Item.Properties(), "pipe_fluid", "25", "50"));
		sItemPipe = registerItemFixture("fixture_connector_item_pipe",
				() -> new GTItemPipeBlockItem(Blocks.BRICKS, new Item.Properties(), "pipe_item", "32_768", "1"));
		sSensor = registerItemFixture("fixture_connector_sensor",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "sensor"));
	}

	/** The leg-swap hover call — ONE swap, the GTLightningRodBlock.Item:71-81 shape. */
	private static void callHoverText(GT6MachineBlockItem aItem, List<Component> aTooltip) {
		//? if forge {
		aItem.appendHoverText(new ItemStack(aItem), null, aTooltip, TooltipFlag.NORMAL);
		//?} else {
		/*aItem.appendHoverText(new ItemStack(aItem), Item.TooltipContext.EMPTY, aTooltip, TooltipFlag.NORMAL);
		*///?}
	}

	@Test
	public void wireCarrierReplaysTheThreeStatRowsWithItsConstants() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sWire, tTooltip);
		assertEquals(3, tTooltip.size());
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		assertEquals("gt6.tooltip.wire.1", tRow0.getKey());
		assertEquals(32L, tRow0.getArgs()[0]);
		assertEquals("LV", tRow0.getArgs()[1]);
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), tTooltip.get(0).getStyle().getColor()); // Chat.CYAN
		TranslatableContents tRow2 = assertInstanceOf(TranslatableContents.class, tTooltip.get(2).getContents());
		assertEquals("gt6.tooltip.wire.3", tRow2.getKey());
		assertEquals("1", tRow2.getArgs()[3]); // the makeString-formatted loss rides as a string
	}

	@Test
	public void wireContactCarrierAddsTheHazardRow() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sWireContact, tTooltip);
		assertEquals(4, tTooltip.size());
		TranslatableContents tRow3 = assertInstanceOf(TranslatableContents.class, tTooltip.get(3).getContents());
		assertEquals("gt6.tooltip.wire_contact.4", tRow3.getKey());
		assertEquals(128L, tRow3.getArgs()[0]); // the contact sibling feeds its own constants
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED), tTooltip.get(3).getStyle().getColor()); // Chat.DRED
	}

	@Test
	public void fluidPipeCarrierReplaysCapacityRowsAndMagnifier() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sFluidPipe, tTooltip);
		assertEquals(3, tTooltip.size());
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		assertEquals("gt6.tooltip.pipe_fluid.1", tRow0.getKey());
		assertEquals("25", tRow0.getArgs()[0]); // capacity/2, the bandwidth slot
		assertEquals("50", tRow0.getArgs()[1]); // capacity, the capacity slot
		TranslatableContents tRow2 = assertInstanceOf(TranslatableContents.class, tTooltip.get(2).getContents());
		assertEquals("gt6.tooltip.pipe_fluid.10", tRow2.getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(2).getStyle().getColor()); // Chat.DGRAY
	}

	@Test
	public void itemPipeCarrierReplaysTheFourRows() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sItemPipe, tTooltip);
		assertEquals(4, tTooltip.size());
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		assertEquals("gt6.tooltip.pipe_item.1", tRow0.getKey());
		assertEquals("32_768", tRow0.getArgs()[0]); // the upstream makeString face, formatted at the site
		TranslatableContents tRow2 = assertInstanceOf(TranslatableContents.class, tTooltip.get(2).getContents());
		assertEquals("gt6.tooltip.pipe_item.3", tRow2.getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(3).getStyle().getColor());
	}

	@Test
	public void sensorCarrierReplaysTheSixToolRows() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sSensor, tTooltip);
		assertEquals(6, tTooltip.size());
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		assertEquals("gt6.tooltip.sensor.2", tRow0.getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), tTooltip.get(0).getStyle().getColor()); // Chat.ORANGE
		TranslatableContents tRow5 = assertInstanceOf(TranslatableContents.class, tTooltip.get(5).getContents());
		assertEquals("gt6.tooltip.sensor.7", tRow5.getKey()); // the fused facing row
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(5).getStyle().getColor());
	}

	@Test
	public void nameFaceStaysPutThroughTheParentSwap() {
		// the r8 coordinator check a: the compose-less block resolves the SAME string the
		// raw BlockItem base resolved (vanilla descriptionId face), and the wire item's
		// retired override lands on the identical block-face delegation
		String tExpected = Blocks.BRICKS.getName().getString();
		assertEquals(tExpected, sFluidPipe.getName(new ItemStack(sFluidPipe)).getString());
		assertEquals(tExpected, sWire.getName(new ItemStack(sWire)).getString());
		assertEquals(tExpected, sItemPipe.getName(new ItemStack(sItemPipe)).getString());
		// the carriers stayed on the machine-carrier chain (the composed-name posture)
		assertInstanceOf(GT6MachineBlockItem.class, sWire);
		assertTrue(sFluidPipe instanceof GT6MachineBlockItem && sItemPipe instanceof GT6MachineBlockItem);
	}
}
