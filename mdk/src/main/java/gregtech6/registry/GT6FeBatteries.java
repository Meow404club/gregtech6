package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GT6FeBatteryBlock;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity;

/**
 * The FE battery test fixture registration, card-owned (ADR-P3-4): a self-contained
 * {@code @EventBusSubscriber(MOD)} triple DeferredRegister attached from the construct
 * event — the GTEnergySources shape verbatim (task p8-d4), including the BLOCK-before-BET
 * registration order (vanilla registry order holds across DeferredRegisters, the
 * GTBlockEntities doc). GT6Mod / GTModBusListener stay untouched (the frozen P2 form).
 *
 * <p>This battery is the RECEIVING end of the p26 EU->FE outbound bridge acceptance chain:
 * a plain platform {@code EnergyStorage} reference implementation (capacity = maxReceive,
 * maxExtract = 0 — a pure sink, the bridge is outbound-only) behind a simple-cube block.
 * The RCON chain drives it headless (/gt6febattery place|stat|reset); no creative tab.
 *
 * <p>The texture is borrowed from the energy_source rig placeholder (the same
 * placeholder-borrow posture as the p8 source rig, assets/README.md attribution row).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FeBatteries {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** The FE sink fixture block (the p26 outbound bridge acceptance receiver). */
	public static final RegistryObject<GT6FeBatteryBlock> FE_BATTERY = BLOCKS.register("fe_battery",
			() -> new GT6FeBatteryBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	public static final RegistryObject<Item> FE_BATTERY_ITEM = ITEMS.register("fe_battery",
			() -> new BlockItem(FE_BATTERY.get(), new Item.Properties()));

	/** BET registers after BLOCK (vanilla registry order) so the validBlocks supplier resolves. */
	public static final RegistryObject<BlockEntityType<GT6FeBatteryBlockEntity>> FE_BATTERY_BE =
			BLOCK_ENTITY_TYPES.register("fe_battery", () -> BlockEntityType.Builder.of(
					GT6FeBatteryBlockEntity::new, FE_BATTERY.get()).build(null));

	private GT6FeBatteries() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (the GTEnergySources fork precedent).
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}
}
