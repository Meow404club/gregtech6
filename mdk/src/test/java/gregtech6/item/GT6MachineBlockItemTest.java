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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.energy.GTEnergySourceBlock;
import gregtech6.block.multiblock.GTCokeOvenBlock;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The carrier gate (task tooltip-infra acceptance ①, task tooltip-boiler-tank
 * expansion): {@link GT6MachineBlockItem} replays the family row table through the offline
 * {@code appendHoverText} direct call — the boiler fixture appends the full 14-row table
 * (the 13-row upstream table + the .14 port-authored annex, task debt-boiler-heat-tip)
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
	static GT6MachineBlockItem sCokeOven;
	static GT6MachineBlockItem sEnergySource;
	static GT6MachineBlockItem sConverter;

	/**
	 * The block-construction write window: real GT block classes cannot be constructed
	 * after the offline Bootstrap froze the block registry, so the base's BlockLatch
	 * (the per-class mirror folded onto GTOfflineTestBase — task probeitem-latch-hygiene)
	 * reopens the write window for the two fixture blocks. 'locked' only exists on the
	 * forge 1.20.1 wrapper — the 21.1 leg runs on 'frozen' alone (the
	 * offline-testbase-latch-neo repair); if even that flag is unreachable the real-block
	 * tests telemetry-skip.
	 */

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
		// the converter family carrier (the review rider, task tooltip-multiblock-generator):
		// the turbine/dynamo registration sites hand family="converter" — the vanilla-block
		// fixture pins the replay without the block-registry latch
		sConverter = registerItemFixture("fixture_tooltip_converter_rider",
				() -> new GT6MachineBlockItem(Blocks.BRICKS, new Item.Properties(), "converter"));
		// the real-block carriers (task tooltip-multiblock-generator acceptance ①):
		// the actual controller block classes a swap hands the carrier — the coke oven
		// (the multiblock family) and the energy-source rig (the generator family);
		// constructed AND registered under the block write window (the item registration
		// callback resolves the block's registry delegate, ForgeRegistry.getDelegateOrThrow
		// through BlockItem.registerBlocks), then mounted into item fixtures
		Block tCokeOven = registerBlockFixture("fixture_tooltip_multiblock_coke_oven",
				() -> new GTCokeOvenBlock(BlockBehaviour.Properties.of()));
		Block tEnergySource = registerBlockFixture("fixture_tooltip_generator_rig",
				() -> new GTEnergySourceBlock(BlockBehaviour.Properties.of()));
		sCokeOven = registerItemFixture("fixture_tooltip_multiblock_coke_oven",
				() -> new GT6MachineBlockItem(tCokeOven, new Item.Properties(), "multiblock"));
		sEnergySource = registerItemFixture("fixture_tooltip_generator_rig",
				() -> new GT6MachineBlockItem(tEnergySource, new Item.Properties(), "generator"));
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
		assertEquals(14, tTooltip.size(), "the boiler family replays the 13-row upstream table + the .14 port-authored annex");
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
	public void converterFamilyAppendsTheConverterBaseRows() {
		// the review rider — the three TileEntityBase11MultiBlockConverter super-chain rows
		// (:94 → the Base10MultiBlockBase :100-101 pair + the :61 facing row), the family
		// the turbine/dynamo carriers replay (upstream MultiTileEntityLargeTurbine/LargeDynamo
		// extend TileEntityBase11MultiBlockConverter)
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sConverter, new ItemStack(sConverter), tTooltip);
		assertEquals(3, tTooltip.size(), "the converter family replays the three base rows");
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		TranslatableContents tRow2 = assertInstanceOf(TranslatableContents.class, tTooltip.get(2).getContents());
		assertEquals("gt6.tooltip.converter.4", tRow0.getKey());
		assertEquals("gt6.tooltip.converter.6", tRow2.getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(0).getStyle().getColor());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(2).getStyle().getColor());
	}

	@Test
	public void familyWithoutRowTableAppendsNothing() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sUntable, new ItemStack(sUntable), tTooltip);
		assertTrue(tTooltip.isEmpty(), "an unregistered family (machine pre-T3) appends ZERO lines");
	}

	@Test
	public void cokeOvenBlockHoverCarriesTheMultiblockBaseRows() {
		// task tooltip-multiblock-generator acceptance ① — the real controller block
		// through the carrier: the three Base10MultiBlockBase chain rows in upstream order
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sCokeOven, new ItemStack(sCokeOven), tTooltip);
		assertEquals(3, tTooltip.size(), "the multiblock family replays the three base rows");
		TranslatableContents tRow0 = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		TranslatableContents tRow2 = assertInstanceOf(TranslatableContents.class, tTooltip.get(2).getContents());
		assertEquals("gt6.tooltip.multiblock.1", tRow0.getKey());
		assertEquals("gt6.tooltip.multiblock.3", tRow2.getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(0).getStyle().getColor());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tTooltip.get(2).getStyle().getColor());
	}

	@Test
	public void generatorBlockHoverCarriesTheSolidConstantBlock() {
		// acceptance ① second block — the eight MultiTileEntityGeneratorSolid constant
		// rows: the requirements head ORANGE, the hazard pair DARK_RED (the DRED pin)
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sEnergySource, new ItemStack(sEnergySource), tTooltip);
		assertEquals(8, tTooltip.size(), "the generator family replays the constant block");
		TranslatableContents tHead = assertInstanceOf(TranslatableContents.class, tTooltip.get(0).getContents());
		TranslatableContents tFire = assertInstanceOf(TranslatableContents.class, tTooltip.get(4).getContents());
		TranslatableContents tContact = assertInstanceOf(TranslatableContents.class, tTooltip.get(5).getContents());
		TranslatableContents tTail = assertInstanceOf(TranslatableContents.class, tTooltip.get(7).getContents());
		assertEquals("gt6.tooltip.generator.4", tHead.getKey());
		assertEquals("gt6.tooltip.generator.8", tFire.getKey());
		assertEquals("gt6.tooltip.generator.9", tContact.getKey());
		assertEquals("gt6.tooltip.generator.11", tTail.getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), tTooltip.get(0).getStyle().getColor());     // Chat.ORANGE
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED), tTooltip.get(4).getStyle().getColor()); // Chat.DRED
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED), tTooltip.get(5).getStyle().getColor()); // Chat.DRED
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
