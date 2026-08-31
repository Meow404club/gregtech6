package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.wire.GTWireBlock;
import gregtech6.block.wire.GTWireBlockItem;

/**
 * Electric wire registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched (the frozen P2 form is never
 * extended). Same shape as GTFluidPipes (the GTFluidPipes.java:48 Supplier block
 * precedent); the shared BET lives in {@link GTBlockEntities#WIRE_ELECTRIC_BE} per the
 * task card (the BET type row in the shared registry file), and its supplier resolves the
 * blocks here — safe because the vanilla registry order fires the Block registration
 * event before the BlockEntityType one, across DeferredRegisters (GTBlockEntities doc).
 *
 * <p>The two variants follow the task card (spec ⑤): 1x = 32 EU / 1 A / 1 loss (the
 * upstream field defaults, MultiTileEntityWireElectric.java:64) and 2x = 32 EU / 2 A /
 * 1 loss (the upstream "2x" bandwidth doubling :73). The 16-wire 5-cable material
 * spectrum (addElectricWires :71-109) is a pool item.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTWires {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** 1x electric wire — 32 EU / 1 A / 1 loss per segment (upstream :64 defaults). */
	public static final RegistryObject<GTWireBlock> WIRE_ELECTRIC_1X = BLOCKS.register("wire_electric_1x",
			() -> new GTWireBlock(32, 1, 1, BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	/** 2x electric wire — 32 EU / 2 A / 1 loss per segment (upstream :73 bandwidth doubling). */
	public static final RegistryObject<GTWireBlock> WIRE_ELECTRIC_2X = BLOCKS.register("wire_electric_2x",
			() -> new GTWireBlock(32, 2, 1, BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	public static final RegistryObject<Item> WIRE_ELECTRIC_1X_ITEM = ITEMS.register("wire_electric_1x",
			() -> new GTWireBlockItem(WIRE_ELECTRIC_1X.get(), new Item.Properties()));

	public static final RegistryObject<Item> WIRE_ELECTRIC_2X_ITEM = ITEMS.register("wire_electric_2x",
			() -> new GTWireBlockItem(WIRE_ELECTRIC_2X.get(), new Item.Properties()));

	/**
	 * The "Electric Wires" category tab — the upstream MTE-registry category
	 * ("Electric Wires", MultiTileEntityWireElectric.java:72 addElectricWires
	 * aCreativeTabID) as the minimal per-card tab, the GTFluidPipes.FLUID_PIPES_TAB shape.
	 */
	public static final RegistryObject<CreativeModeTab> ELECTRIC_WIRES_TAB = CREATIVE_MODE_TABS.register("electric_wires",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.electric_wires"))
					.icon(() -> new ItemStack(WIRE_ELECTRIC_2X_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(WIRE_ELECTRIC_1X_ITEM.get()));
						aOutput.accept(new ItemStack(WIRE_ELECTRIC_2X_ITEM.get()));
					})
					.build());

	private GTWires() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}
}
