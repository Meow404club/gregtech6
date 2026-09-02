package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
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

import gregtech6.block.multiblock.GTCokeOvenBlock;
import gregtech6.block.multiblock.GTMultiBlockPartBlock;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * Multiblock domain registration, card-owned (ADR-P3-4 self-contained listener form, the
 * GTMachines precedent): controller + part blocks, their BlockEntityTypes, items, and the
 * "multiblocks" creative tab — task p4-multiblock-framework.
 *
 * <p>id choice (task card ④ note): the registry paths keep the card-ruled {@code gt6:multiblock_*}
 * prefix — {@code multiblock_coke_oven} (controller), {@code multiblock_coke_oven_bricks}
 * (the 26-cell part, upstream MTE id 18000 "coke oven bricks"). The BET paths mirror the
 * {@code getTileEntityName} values like every prior pair: {@code multiblock_coke_oven} and
 * the SHARED {@code multiblock_part} — one part BE type mounting every future part block
 * (ADR-P3-1: MultiTileEntityMultiBlockPart was one MTE class for all part types, the modern
 * form is one BET + per-type Blocks; new part blocks append to the valid list).
 *
 * <p>Block properties: the upstream coke-oven MTE hardness was not surveyed (RM.CokeOven
 * processing face is a pool card) — the vanilla-stone tier (3.5/6.0, STONE sound) is chosen
 * for both blocks as a placeholder matching the brick masonry look.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMultiBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The Coke Oven controller block (FACING + FORMED properties, GTMultiBlockControllerBlock). */
	public static final RegistryObject<GTCokeOvenBlock> COKE_OVEN = BLOCKS.register("multiblock_coke_oven",
			() -> new GTCokeOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.5F, 6.0F).sound(SoundType.STONE)));

	/** The coke oven bricks part block (the 26-cell structure body, upstream MTE id 18000). */
	public static final RegistryObject<GTMultiBlockPartBlock> COKE_OVEN_BRICKS = BLOCKS.register("multiblock_coke_oven_bricks",
			() -> new GTMultiBlockPartBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.5F, 6.0F).sound(SoundType.STONE)));

	/**
	 * The Coke Oven BET: one class, its one block (ADR-P3-1 degenerate shape; future oven
	 * variants append to the valid list). Registry path mirrors
	 * {@link TileEntityCokeOven#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityCokeOven>> COKE_OVEN_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_coke_oven", () -> BlockEntityType.Builder.of(
					TileEntityCokeOven::new, COKE_OVEN.get()).build(null));

	/**
	 * The SHARED part BET (ADR-P3-1): one MultiBlockPartBlockEntity class, every part block.
	 * Registry path mirrors {@link MultiBlockPartBlockEntity#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<MultiBlockPartBlockEntity>> MULTIBLOCK_PART_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_part", () -> BlockEntityType.Builder.of(
					MultiBlockPartBlockEntity::new, COKE_OVEN_BRICKS.get()).build(null));

	public static final RegistryObject<Item> COKE_OVEN_ITEM = ITEMS.register("multiblock_coke_oven",
			() -> new BlockItem(COKE_OVEN.get(), new Item.Properties()));

	public static final RegistryObject<Item> COKE_OVEN_BRICKS_ITEM = ITEMS.register("multiblock_coke_oven_bricks",
			() -> new BlockItem(COKE_OVEN_BRICKS.get(), new Item.Properties()));

	/** The multiblock family tab (upstream: the MTE-registry per-category tab form). */
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	public static final RegistryObject<CreativeModeTab> MULTIBLOCKS_TAB = CREATIVE_MODE_TABS.register("multiblocks",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.multiblocks"))
					.icon(() -> new ItemStack(COKE_OVEN_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(COKE_OVEN_ITEM.get()));
						aOutput.accept(new ItemStack(COKE_OVEN_BRICKS_ITEM.get()));
					})
					.build());

	private GTMultiBlocks() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
	 * (GTBlockEntities/GTMachines precedent; Bus.MOD.bus().get() = Mod.java:81).
	 */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}
}
