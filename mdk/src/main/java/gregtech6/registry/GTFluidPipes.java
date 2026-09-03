package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.pipe.GTFluidPipeBlockItem;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;

/**
 * Fluid pipe registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched (the frozen P2 form is never
 * extended). Same shape as GTBlockEntities; a separate class keeps the W1/W3 card
 * scopes disjoint (the chest registration owns its DRs, this card owns its own).
 *
 * <p>The two wood tiers follow the task card fix (aStat=50 → 50 L / 300 L per tank; the
 * upstream multiplier row is MultiTileEntityPipeFluid.java:92-94, wooden max temperature
 * Loader_MultiTileEntities.java:1846-1847). Registration order follows the vanilla
 * registry order BLOCKS before BLOCK_ENTITY_TYPES (GTBlockEntities doc, BlockEntityType
 * needs the block instances at supplier time). The pipe BE is shared across both tiers
 * (ADR-P3-1, the GT6 "one TE class, many blocks" multi-mount).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTFluidPipes {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** Wood small fluid pipe — 50 L per tank (card: aStat=50 → 50). */
	public static final RegistryObject<GTFluidPipeBlock> WOOD_FLUID_PIPE_SMALL = BLOCKS.register("wood_fluid_pipe_small",
			() -> new GTFluidPipeBlock(50, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.WOOD)));

	/** Wood medium fluid pipe — 300 L per tank (card: aStat=50 → 300; upstream medium = aStat*6). */
	public static final RegistryObject<GTFluidPipeBlock> WOOD_FLUID_PIPE_MEDIUM = BLOCKS.register("wood_fluid_pipe_medium",
			() -> new GTFluidPipeBlock(300, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.WOOD)));

	/**
	 * Shared pipe BET: one BlockEntityType over both tiers (ADR-P3-1). Registry path
	 * "fluid_pipe" mirrors GTFluidPipeBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<GTFluidPipeBlockEntity>> FLUID_PIPE_BE =
			BLOCK_ENTITY_TYPES.register("fluid_pipe", () -> BlockEntityType.Builder.of(
					GTFluidPipeBlockEntity::new, WOOD_FLUID_PIPE_SMALL.get(), WOOD_FLUID_PIPE_MEDIUM.get()).build(null));

	public static final RegistryObject<Item> WOOD_FLUID_PIPE_SMALL_ITEM = ITEMS.register("wood_fluid_pipe_small",
			() -> new GTFluidPipeBlockItem(WOOD_FLUID_PIPE_SMALL.get(), new Item.Properties()));

	public static final RegistryObject<Item> WOOD_FLUID_PIPE_MEDIUM_ITEM = ITEMS.register("wood_fluid_pipe_medium",
			() -> new GTFluidPipeBlockItem(WOOD_FLUID_PIPE_MEDIUM.get(), new Item.Properties()));

	/**
	 * The "Fluid Pipes" category tab — the upstream MTE-registry category
	 * ("Fluid Pipes", MultiTileEntityPipeFluid.java:83 addFluidPipes aCreativeTabID) as the
	 * minimal per-card tab, same shape as the chest CHESTS_TAB.
	 */
	public static final RegistryObject<CreativeModeTab> FLUID_PIPES_TAB = CREATIVE_MODE_TABS.register("fluid_pipes",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.fluid_pipes"))
					.icon(() -> new ItemStack(WOOD_FLUID_PIPE_MEDIUM_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(WOOD_FLUID_PIPE_SMALL_ITEM.get()));
						aOutput.accept(new ItemStack(WOOD_FLUID_PIPE_MEDIUM_ITEM.get()));
					})
					.build());

	private GTFluidPipes() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GT6Mod/GTMenuTypes fork precedent.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}
}
