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
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;
import gregtech6.tileentity.tank.GTBarrelPlasticBlockEntity;

/**
 * Fluid barrel registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched. Same shape as GTFluidPipes/GTFluids;
 * a separate class keeps the W3 card scopes disjoint.
 *
 * <p>The barrel family (task p4-fluid-barrel wood, p6-barrel-metal-plastic wood-carrier
 * extension + plastic/metal) ports the upstream tank rows (Loader_MultiTileEntities
 * .java:2136/:2140/:2150/:2151, category "Fluid Containers"): each material row carries
 * its own TE class and BET, the capacity and melt-down ceiling ride the block (the W1
 * registration-NBT-carrier pattern). Creative tab ownership (card note ④): all barrels
 * belong to the "Fluid Containers" category — the upstream MTE-registry category of the
 * same rows — as this card's minimal per-card tab.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBarrels {

	public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/**
	 * Wood fluid barrel — 16000 L sticky-tank family, melts down at 340 K (upstream
	 * NBT_CAPACITY_HU row). The capacity/ceiling/ticker-type now ride the block carrier
	 * explicitly (task p6-barrel-metal-plastic): 16000 L was already the class default,
	 * so this row is a zero-behaviour-change re-statement.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL = BLOCKS.register("barrel_wood",
			() -> new GTBarrelBlock(16000, 340, () -> GTBarrels.BARREL_BE.get(), net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(1.0F, 5.0F).sound(SoundType.WOOD)));

	/** The barrel BET: one BlockEntityType over the wood barrel (registry order BLOCKS before BLOCK_ENTITY_TYPES). */
	public static final RegistryObject<BlockEntityType<GTBarrelBlockEntity>> BARREL_BE =
			BLOCK_ENTITY_TYPES.register("barrel_wood", () -> BlockEntityType.Builder.of(
					GTBarrelBlockEntity::new, BARREL.get()).build(null));

	public static final RegistryObject<Item> BARREL_ITEM = ITEMS.register("barrel_wood",
			() -> new BlockItem(BARREL.get(), new Item.Properties()));

	/**
	 * Plastic canister — 32000 L, melts down at 370 K (upstream NBT_CAPACITY_HU row,
	 * Loader_MultiTileEntities.java:2150; the upstream GASPROOF flag is a P4 quartet pool
	 * cut with no consumer). The block properties are placeholders like every barrel
	 * texture here: WOOL is the closest vanilla stand-in for plastic.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL_PLASTIC = BLOCKS.register("barrel_plastic",
			() -> new GTBarrelBlock(32000, 370, () -> GTBarrels.BARREL_PLASTIC_BE.get(),
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
							.strength(1.0F, 5.0F).sound(SoundType.WOOL)));

	/** The plastic canister BET: validity over the plastic barrel only (one TE class per material row). */
	public static final RegistryObject<BlockEntityType<GTBarrelPlasticBlockEntity>> BARREL_PLASTIC_BE =
			BLOCK_ENTITY_TYPES.register("barrel_plastic", () -> BlockEntityType.Builder.of(
					GTBarrelPlasticBlockEntity::new, BARREL_PLASTIC.get()).build(null));

	public static final RegistryObject<Item> BARREL_PLASTIC_ITEM = ITEMS.register("barrel_plastic",
			() -> new BlockItem(BARREL_PLASTIC.get(), new Item.Properties()));

	/**
	 * Metal drum — 64000 L bronze tier (upstream row Loader_MultiTileEntities.java:2151,
	 * the lowest metal drum of the 64K→10B ladder). Declared deviation: MAX_VALUE ceiling
	 * (never melts) — the upstream rows carry no explicit HU and the
	 * {@code mMaterial.mMeltingPoint * 1.25} formula needs the material melting-point
	 * bridge this repo does not ship (pool item). Copper sound for the bronze drum.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL_METAL = BLOCKS.register("barrel_metal",
			() -> new GTBarrelBlock(64000, Long.MAX_VALUE, () -> GTBarrels.BARREL_METAL_BE.get(),
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
							.strength(1.0F, 5.0F).sound(SoundType.COPPER)));

	/** The metal drum BET: validity over the metal drum only (one TE class per material row). */
	public static final RegistryObject<BlockEntityType<GTBarrelMetalBlockEntity>> BARREL_METAL_BE =
			BLOCK_ENTITY_TYPES.register("barrel_metal", () -> BlockEntityType.Builder.of(
					GTBarrelMetalBlockEntity::new, BARREL_METAL.get()).build(null));

	public static final RegistryObject<Item> BARREL_METAL_ITEM = ITEMS.register("barrel_metal",
			() -> new BlockItem(BARREL_METAL.get(), new Item.Properties()));

	/** The "Fluid Containers" category tab (upstream MTE category of the barrel row, :2136 column 2). */
	public static final RegistryObject<CreativeModeTab> FLUID_CONTAINERS_TAB = CREATIVE_MODE_TABS.register("fluid_containers",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.fluid_containers"))
					.icon(() -> new ItemStack(BARREL_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(BARREL_ITEM.get()));
						aOutput.accept(new ItemStack(BARREL_PLASTIC_ITEM.get()));
						aOutput.accept(new ItemStack(BARREL_METAL_ITEM.get()));
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
			GT6Mod.LOGGER.info("GT6 fluid barrels registered: {} {} L @ {} K / {} {} L @ {} K / {} {} L @ MAX K",
					ForgeRegistries.BLOCKS.getKey(BARREL.get()), BARREL.get().capacityL(), BARREL.get().meltingPointK(),
					ForgeRegistries.BLOCKS.getKey(BARREL_PLASTIC.get()), BARREL_PLASTIC.get().capacityL(), BARREL_PLASTIC.get().meltingPointK(),
					ForgeRegistries.BLOCKS.getKey(BARREL_METAL.get()), BARREL_METAL.get().capacityL());
		});
	}
}
