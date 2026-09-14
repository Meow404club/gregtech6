package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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

import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.GTLargeDynamoBlockEntity;

/**
 * The Large Dynamo family registration home (task p29-w3-turbine-dynamo ④, the
 * GT6Boilers self-contained-DR form, ADR-P3-4): 4 controller blocks/items over ONE shared
 * BET row — {@link GTLargeDynamoBlockEntity} over the
 * {@link gregtech6.tileentity.multiblocks.GTMultiBlockConverter} base. RU in at the front,
 * EU out at the far plate at exactly 75% (Loader_MultiTileEntities.java:1259-1262:
 * 4096→3072 / 8192→6144 / 16384→12288 / 131072→98304), WASTE_ENERGY = T; the structure
 * walls are the card ① Dense Walls, the middle segment the 18 Large Copper Coils (18040).
 * No creative-tab join (the p28 dynamo precedent).
 *
 * <p>The display names are the upstream name column verbatim, ATOMIC
 * ("X Dynamo Main Housing"); the blocks resolve the vanilla {@code block.gt6.<path>} keys.
 * The crafting recipes ({@code "SwS","CMC","SBS"} — stickLong + circuits[6] + re-battery,
 * Loader :1259-1262) are the tier-a datagen rows.
 *
 * <p>KJS face (task card): registration-surface; no KubeJS special face.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6DynamoHousings {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The part-block path this family resolves defensively (the row 1 Dense SS Wall, unreachable in production). */
	public static final String DEFAULT_WALL_PATH = "dense_wall_stainless_steel";

	/** One dynamo registration row — the block-carrier projection of one :1259-1262 line (the 75% ratio lives in the input/output pair). */
	public record DynamoRow(String path, String display, int metaId, long input, long output,
			float hardness, String wallPath) {}

	/** The four dynamo rows (:1259-1262, the upstream line order, hardness == resistance). */
	public static final List<DynamoRow> DYNAMO_ROWS = List.of(
			new DynamoRow("large_dynamo_stainless_steel", "Stainless Steel Dynamo Main Housing", 17221,   4096,   3072,   6.0F, "dense_wall_stainless_steel"),
			new DynamoRow("large_dynamo_titanium"       , "Titanium Dynamo Main Housing"       , 17222,   8192,   6144,   9.0F, "dense_wall_titanium"),
			new DynamoRow("large_dynamo_tungstensteel"  , "Tungstensteel Dynamo Main Housing"  , 17223,  16384,  12288,  12.5F, "dense_wall_tungstensteel"),
			new DynamoRow("large_dynamo_adamantium"     , "Adamantium Dynamo Main Housing"     , 17224, 131072,  98304, 100.0F, "dense_wall_adamantium"));

	/** The registered dynamo blocks by path (the BET valid list + the datagen/loot walkers + the chains). */
	public static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (DynamoRow tRow : DYNAMO_ROWS) {
			registerController(tRow.path(), () -> new DynamoBlock(tRow, props(tRow.hardness())));
		}
	}

	/** One controller block + item registration pair (the GT6Turbines form). */
	private static void registerController(String aPath, java.util.function.Supplier<Block> aBlock) {
		BLOCKS_BY_PATH.put(aPath, BLOCKS.register(aPath, aBlock));
		ITEMS_BY_PATH.put(aPath, ITEMS.register(aPath,
				() -> new BlockItem(GT6DynamoHousings.BLOCKS_BY_PATH.get(aPath).get(), new Item.Properties())));
	}

	private static BlockBehaviour.Properties props(float aHardness) {
		return BlockBehaviour.Properties.of().strength(aHardness, aHardness).sound(SoundType.METAL);
	}

	/** The dynamo-variant block array for the controller BET. */
	private static Block[] dynamoBlockArray() {
		return DYNAMO_ROWS.stream().map(r -> BLOCKS_BY_PATH.get(r.path()).get()).toArray(Block[]::new);
	}

	/** The Large Dynamo BET: one controller class over the four variant blocks. */
	public static final RegistryObject<BlockEntityType<GTLargeDynamoBlockEntity>> DYNAMO_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_large_dynamo", () -> BlockEntityType.Builder.of(
					GTLargeDynamoBlockEntity::new, dynamoBlockArray()).build(null));

	/** The lookup for the chains — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<Block> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The dynamo controller block — the row carrier (the GTLargeBoilerBlock form; no GUI by census). */
	public static class DynamoBlock extends GTMultiBlockControllerBlock {

		private final DynamoRow mRow;

		public DynamoBlock(DynamoRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract — the simpleCodec representative-value
		// form (the BoilerTankBlock precedent; world save/load never runs through it).
		@Override
		protected com.mojang.serialization.MapCodec<? extends DynamoBlock> codec() {
			return simpleCodec(aProperties -> new DynamoBlock(DYNAMO_ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the BE's config injection read). */
		public DynamoRow row() {
			return mRow;
		}

		/** The Dense Wall block of this row (the NBT_DESIGN column, the boiler wallBlock form). */
		public Block wallBlock() {
			Block tWall = GTMultiBlocks.anyPartBlock(mRow.wallPath());
			return tWall != null ? tWall : GTMultiBlocks.anyPartBlock(DEFAULT_WALL_PATH);
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return DYNAMO_BE.get();
		}
	}

	private GT6DynamoHousings() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Boilers form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (the GT6Boilers fork precedent).
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}
}
