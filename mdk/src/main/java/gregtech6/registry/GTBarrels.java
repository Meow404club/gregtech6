package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;

/**
 * Fluid barrel registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched. Same shape as GTFluidPipes/GTFluids;
 * a separate class keeps the W3 card scopes disjoint.
 *
 * <p>The wood fluid barrel is the port of the upstream
 * {@code MultiTileEntityBarrelWood} row (Loader_MultiTileEntities.java:2136, category
 * "Fluid Containers"): the melt-down ceiling 340 K rides the block (the W1
 * registration-NBT-carrier pattern), the tank capacity stays the class default 16000 L
 * (card acceptance ①). Creative tab ownership (card note ④): the barrel belongs to the
 * "Fluid Containers" category — the upstream MTE-registry category of the same row —
 * as this card's minimal per-card tab.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBarrels {

	public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** Wood fluid barrel — 16000 L sticky-tank family, melts down at 340 K (upstream NBT_CAPACITY_HU row). */
	public static final RegistryObject<GTBarrelBlock> BARREL = BLOCKS.register("barrel_wood",
			() -> new GTBarrelBlock(340, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(1.0F, 5.0F).sound(SoundType.WOOD)));

	/** The barrel BET: one BlockEntityType over the wood barrel (registry order BLOCKS before BLOCK_ENTITY_TYPES). */
	public static final RegistryObject<BlockEntityType<GTBarrelBlockEntity>> BARREL_BE =
			BLOCK_ENTITY_TYPES.register("barrel_wood", () -> BlockEntityType.Builder.of(
					GTBarrelBlockEntity::new, BARREL.get()).build(null));

	public static final RegistryObject<Item> BARREL_ITEM = ITEMS.register("barrel_wood",
			() -> new BlockItem(BARREL.get(), new Item.Properties()));

	/** The "Fluid Containers" category tab (upstream MTE category of the barrel row, :2136 column 2). */
	public static final RegistryObject<CreativeModeTab> FLUID_CONTAINERS_TAB = CREATIVE_MODE_TABS.register("fluid_containers",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.fluid_containers"))
					.icon(() -> new ItemStack(BARREL_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(BARREL_ITEM.get()));
					})
					.build());

	private GTBarrels() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTFluidPipes.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}

	/** Registration smoke evidence (the GTFluids.onCommonSetup log shape, acceptance ④). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 fluid barrel registered: {} (block) / {} (item), melting at {} K",
					ForgeRegistries.BLOCKS.getKey(BARREL.get()),
					ForgeRegistries.ITEMS.getKey(BARREL_ITEM.get()),
					BARREL.get().meltingPointK());
		});
	}
}
