package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

import gregtech6.block.energy.GTEnergySourceBlock;

/**
 * Test energy source registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched (the frozen P2 form is never
 * extended). Same shape as GTWires / GTFluidPipes (the GTFluidPipes.java:48 Supplier
 * block precedent); the shared BET lives in {@link GTBlockEntities#ENERGY_SOURCE_BE} per
 * the task card (the BET type row in the shared registry file), and its supplier resolves
 * the block here — safe because the vanilla registry order fires the Block registration
 * event before the BlockEntityType one, across DeferredRegisters (GTBlockEntities doc).
 *
 * <p>No creative tab: the rig is command-driven (/gt6energy place), the task card lists
 * block + BlockItem + BET only.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTEnergySources {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The command-driven test generator (task p8-d4-energy-source spec ①/②). */
	public static final RegistryObject<GTEnergySourceBlock> ENERGY_SOURCE = BLOCKS.register("energy_source",
			() -> new GTEnergySourceBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	public static final RegistryObject<Item> ENERGY_SOURCE_ITEM = ITEMS.register("energy_source",
			() -> new BlockItem(ENERGY_SOURCE.get(), new Item.Properties()));

	private GTEnergySources() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}
}
