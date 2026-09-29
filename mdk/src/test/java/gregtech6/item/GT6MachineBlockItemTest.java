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
	static GT6MachineBlockItem sCokeOven;
	static GT6MachineBlockItem sEnergySource;

	/**
	 * The block-construction write window (the GT6LargeMachineTexDatagenTest.BlockLatch
	 * shape): real GT block classes cannot be constructed after the offline Bootstrap
	 * froze the block registry, so the latch reopens the write window for the two fixture
	 * blocks. Unarmed (the 1.21.1 JVM form) = the real-block tests telemetry-skip — the
	 * known ItemLatch dual-leg asymmetry, the r8-tex-large-machines 备案 form.
	 */
	private static final class BlockLatch {
		static final sun.misc.Unsafe UNSAFE;
		static final long LOCKED_OFFSET;
		static final long FROZEN_OFFSET;
		static final boolean ARMED;
		static {
			sun.misc.Unsafe tUnsafe = null;
			long tLocked = 0, tFrozen = 0;
			boolean tArmed = true;
			try {
				java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tUnsafeField.setAccessible(true);
				tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
				Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass();
				tLocked = tUnsafe.objectFieldOffset(GTOfflineTestBase.findNestedField(tClass, "locked"));
				tFrozen = tUnsafe.objectFieldOffset(GTOfflineTestBase.findNestedField(tClass, "frozen"));
			} catch (Throwable ignored) {
				tArmed = false; // the fallback leg (no constructed fixtures)
			}
			UNSAFE = tUnsafe;
			LOCKED_OFFSET = tLocked;
			FROZEN_OFFSET = tFrozen;
			ARMED = tArmed;
		}
	}

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
		// the real-block carriers (task r8-tooltip-multiblock-generator acceptance ①):
		// the actual controller block classes a swap hands the carrier — the coke oven
		// (the multiblock family) and the energy-source rig (the generator family);
		// constructed AND registered under the block write window (the item registration
		// callback resolves the block's registry delegate, ForgeRegistry.getDelegateOrThrow
		// through BlockItem.registerBlocks), then mounted into item fixtures
		org.junit.jupiter.api.Assumptions.assumeTrue(BlockLatch.ARMED,
				"the offline block-registry latch is unreachable on this JVM");
		Block tCokeOven;
		Block tEnergySource;
		BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
				BlockLatch.FROZEN_OFFSET, false);
		BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
				BlockLatch.LOCKED_OFFSET, false);
		try {
			tCokeOven = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					new net.minecraft.resources.ResourceLocation("gt6", "fixture_tooltip_multiblock_coke_oven"),
					new GTCokeOvenBlock(BlockBehaviour.Properties.of()));
			tEnergySource = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					new net.minecraft.resources.ResourceLocation("gt6", "fixture_tooltip_generator_rig"),
					new GTEnergySourceBlock(BlockBehaviour.Properties.of()));
		} finally {
			BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					BlockLatch.FROZEN_OFFSET, true);
			BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					BlockLatch.LOCKED_OFFSET, true);
		}
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
	public void cokeOvenBlockHoverCarriesTheMultiblockBaseRows() {
		// task r8-tooltip-multiblock-generator acceptance ① — the real controller block
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
