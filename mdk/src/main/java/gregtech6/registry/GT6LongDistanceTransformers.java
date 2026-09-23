package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.tileentity.energy.GT6LongDistanceTransformerBlockEntity;

/**
 * The Long Distance Transformer Endpoint registration (task p35-energy-tail-machines) —
 * the five rows of the upstream declared subset verbatim (Loader_MultiTileEntities
 * :909-:913, meta ids 10064-10068 as the parity column): Electric_T[4..8] housings
 * (Cr/Ti/Ir/Os/Trinitanium), NBT_INPUT = NBT_OUTPUT = V[4..8], WASTE F, EU/EU, display
 * "Long Distance Transformer Endpoint ("+VN[i]+")". The block carrier REUSES the
 * {@link GT6ElectricTransformerBlock} facing cube (the FRONT = input / BACK = output
 * convention is the same shape; the tier column selects the row) — one block class, one
 * BE class, two families of blocks over two BETs.
 *
 * <p>The crafting row (the :909 shape "WMW","MxM","WMW"): 'M' = the SAME-tier electric
 * transformer item (getItem(10044..10048) — the p35 transformer ladder), 'W' =
 * cableGt04(AnnealedCopper) → the insulated cable item; the unbound 'x' dead cell folds
 * to a space (the :881 'm' fold). KJS surface: none (registration face deferred — the
 * KJS binding pool).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6LongDistanceTransformers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One LD transformer row — the upstream-parity columns of one Loader :909-:913 aRegistry.add line. */
	public record LDRow(String path, int metaId, int tier, String voltageWord) {}

	/** The five rows, the :909-:913 line order (tier = the ladder index i, the V[i] packet seat; the path suffix _t(i+1), the bridgePath convention). */
	public static final java.util.List<LDRow> ROWS = java.util.List.of(
			new LDRow("longdist_transformer_t5", 10064, 4, "EV"),
			new LDRow("longdist_transformer_t6", 10065, 5, "IV"),
			new LDRow("longdist_transformer_t7", 10066, 6, "LuV"),
			new LDRow("longdist_transformer_t8", 10067, 7, "ZPM"),
			new LDRow("longdist_transformer_t9", 10068, 8, "UV"));

	// the block/item registrations — one per row, the transformer map form
	/** The row blocks by registry path (the datagen/loot walk seat). */
	public static final java.util.Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The row items by registry path (the recipe result seat). */
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (LDRow tRow : ROWS) {
			int tTier = tRow.tier();
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(), () -> ldTransformer(tTier)));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new BlockItem(
					BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	/** The row block: the facing-cube carrier REUSED from the electric transformer family (the tier column = the row). */
	private static GT6ElectricTransformerBlock ldTransformer(int aTier) {
		return new GT6ElectricTransformerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(4.0F, 4.0F).sound(SoundType.METAL), () -> LONG_DISTANCE_TRANSFORMER_BE.get(), aTier);
	}

	/** The BET — the one BE class over the five ladder blocks (the family-BET shape). */
	public static final RegistryObject<BlockEntityType<GT6LongDistanceTransformerBlockEntity>> LONG_DISTANCE_TRANSFORMER_BE =
			BLOCK_ENTITY_TYPES.register("longdist_transformer", () -> BlockEntityType.Builder.of(
					GT6LongDistanceTransformerBlockEntity::new,
					ROWS.stream().map(aRow -> BLOCKS_BY_PATH.get(aRow.path()).get()).toArray(Block[]::new)).build(null));

	private GT6LongDistanceTransformers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ElectricDynamos fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework — the GTBarrels fork form.
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}

	/** Registration smoke evidence (the GT6ElectricDynamos.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 long distance transformers registered: " + ROWS.size()
					+ " rows EV..UV, single-hop wire-line delegation (ids 10064-10068, the p35 energy tail card)");
		});
	}
}
