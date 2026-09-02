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

import gregtech6.block.energy.GTCrankBlock;

/**
 * The kinetics registration family home (task p12-engine-crank spec ③): the shared
 * DeferredRegister pair for the engine + rotation-transmission family — the Hand Crank
 * is its first member, the axle/gearbox/transformer/engine cards append here. Card-owned
 * (ADR-P3-4): self-contained {@code @EventBusSubscriber(MOD)} DeferredRegisters attached
 * from the construct event — GT6Mod.java / GTModBusListener.java stay untouched (the
 * frozen P2 form is never extended). Same shape as GTEnergySources / GTWires
 * (the GTEnergySources.java Supplier-block precedent); the shared BET row lives in
 * {@link GTBlockEntities#CRANK_BE} per the card (the BET type row in the shared registry
 * file, the ENERGY_SOURCE_BE/WIRE_ELECTRIC_BE cross-register resolution shape), and its
 * supplier resolves the block here — safe because the vanilla registry order fires the
 * Block registration event before the BlockEntityType one, across DeferredRegisters
 * (GTBlockEntities doc).
 *
 * <p>Block constants from the upstream registration row (Loader_MultiTileEntities.java
 * :2106): hardness 1.0F / resistance 6.0F, the metal tool material →
 * {@link SoundType#METAL}. No creative tab (the tab system is the per-family pool card;
 * the item stays /give-reachable).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Kinetics {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The Hand Crank (upstream "Hand Crank" meta 32111, MultiTileEntityCrank port). */
	public static final RegistryObject<GTCrankBlock> CRANK = BLOCKS.register("crank",
			() -> new GTCrankBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 6.0F).sound(SoundType.METAL)));

	public static final RegistryObject<Item> CRANK_ITEM = ITEMS.register("crank",
			() -> new BlockItem(CRANK.get(), new Item.Properties()));

	private GT6Kinetics() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}
}
