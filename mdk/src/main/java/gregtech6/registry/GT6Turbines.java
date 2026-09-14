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
import gregtech6.tileentity.multiblocks.GTGasTurbineBlockEntity;
import gregtech6.tileentity.multiblocks.GTSteamTurbineBlockEntity;

/**
 * The Large Turbine family registration home (task p29-w3-turbine-dynamo ②③, the
 * GT6Boilers self-contained-DR form, ADR-P3-4): 8 controller blocks/items over TWO shared
 * BET rows — {@link GTSteamTurbineBlockEntity} (4) and {@link GTGasTurbineBlockEntity} (4)
 * over the {@link gregtech6.tileentity.multiblocks.GTMultiBlockConverter} base. The
 * structure walls are the card ① Dense Walls (the NBT_DESIGN column becomes the row's
 * wallPath); no creative-tab join (the p28 dynamo precedent — the tab walk lives in the
 * GTMultiBlocks tab, a shared seam this card does not touch; the items ride commands and
 * the wand stock).
 *
 * <p><b>The rows</b> (Loader_MultiTileEntities.java:1254-1257 steam / :1264-1267 gas,
 * upstream line order, every id/hardness/INPUT/OUTPUT column kept):
 * <ul>
 * <li>{@link #STEAM_ROWS} = 4 — "X Steam Turbine Main Housing", ids 17211-17214,
 *     NBT_INPUT 6144/12288/24576/196608 × STEAM_PER_EU 2 = 12288/24576/49152/393216,
 *     NBT_OUTPUT 4096/8192/16384/131072, NBT_WASTE_ENERGY T, accepted STEAM, emitted RU,
 *     NBT_DESIGN = the row's Dense Wall (18022/18026/18023/18025);</li>
 * <li>{@link #GAS_ROWS} = 4 — "X Gas Turbine Main Housing", ids 17231-17234, NBT_INPUT
 *     6144/12288/24576/196608 (HU packets), NBT_OUTPUT 4096/8192/16384/131072 (RU),
 *     NBT_WASTE_ENERGY F, NBT_LIMIT_CONSUMPTION T, NBT_FUELMAP FM.Gas, NBT_DESIGN = the
 *     same Dense Wall set.</li>
 * </ul>
 *
 * <p>The display names are ATOMIC (the upstream name column verbatim — the turbine words
 * Magnalium/Trinitanium/Graphene/Vibramantium are row words, not the housing materials,
 * so no composed template applies); the blocks resolve the vanilla
 * {@code block.gt6.&lt;path&gt;} keys (the lightning-rod-part shape).
 *
 * <p>The crafting recipes consume the STEAM TURBINE CONTROLLERS as the middle key
 * (Loader :1264-1267 {@code getItem(17211..17214)} — the gas family cannot craft before
 * the steam family exists; in this port the dependency is structural, the datagen rows
 * reference the steam paths).
 *
 * <p>KJS face (task card): registration-surface + the FM.Gas datapack rows; no KubeJS
 * special face.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Turbines {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The NBT_INPUT STEAM_PER_EU multiplier (the loader's own CS.java:240 = 2, the GTFluids carrier). */
	private static final long STEAM_PER_EU = gregtech6.fluid.GTFluids.STEAM_PER_EU;

	/** The part-block path this family resolves defensively (the row 1 Dense SS Wall, unreachable in production). */
	public static final String DEFAULT_WALL_PATH = "dense_wall_stainless_steel";

	/** One steam-turbine registration row — the block-carrier projection of one :1254-1257 line ({@code input} is the raw NBT_INPUT × STEAM_PER_EU, {@code wallPath} the NBT_DESIGN Dense Wall, hardness == resistance). */
	public record SteamTurbineRow(String path, String display, int metaId, long input, long output,
			float hardness, String wallPath) {}

	/** One gas-turbine registration row — the :1264-1267 projection (LIMIT_CONSUMPTION T / WASTE F / FM.Gas are the family constants, not row columns). */
	public record GasTurbineRow(String path, String display, int metaId, long input, long output,
			float hardness, String wallPath) {}

	/** The four steam rows (:1254-1257, the upstream line order — raw NBT_INPUT 6144..196608 ×STEAM_PER_EU). */
	public static final List<SteamTurbineRow> STEAM_ROWS = List.of(
			new SteamTurbineRow("steam_turbine_magnalium"   , "Magnalium Steam Turbine Main Housing"   , 17211,   6144 * STEAM_PER_EU,   4096,   6.0F, "dense_wall_stainless_steel"),
			new SteamTurbineRow("steam_turbine_trinitanium" , "Trinitanium Steam Turbine Main Housing" , 17212,  12288 * STEAM_PER_EU,   8192,   9.0F, "dense_wall_titanium"),
			new SteamTurbineRow("steam_turbine_graphene"    , "Graphene Steam Turbine Main Housing"    , 17213,  24576 * STEAM_PER_EU,  16384,  12.5F, "dense_wall_tungstensteel"),
			new SteamTurbineRow("steam_turbine_vibramantium", "Vibramantium Steam Turbine Main Housing", 17214, 196608 * STEAM_PER_EU, 131072, 100.0F, "dense_wall_adamantium"));

	/** The four gas rows (:1264-1267, raw NBT_INPUT 6144..196608 HU). */
	public static final List<GasTurbineRow> GAS_ROWS = List.of(
			new GasTurbineRow("gas_turbine_magnalium"   , "Magnalium Gas Turbine Main Housing"   , 17231,   6144,   4096,   6.0F, "dense_wall_stainless_steel"),
			new GasTurbineRow("gas_turbine_trinitanium" , "Trinitanium Gas Turbine Main Housing" , 17232,  12288,   8192,   9.0F, "dense_wall_titanium"),
			new GasTurbineRow("gas_turbine_graphene"    , "Graphene Gas Turbine Main Housing"    , 17233,  24576,  16384,  12.5F, "dense_wall_tungstensteel"),
			new GasTurbineRow("gas_turbine_vibramantium", "Vibramantium Gas Turbine Main Housing", 17234, 196608, 131072, 100.0F, "dense_wall_adamantium"));

	/** The registered turbine blocks by path (the BET valid lists + the datagen/loot walkers + the chains). */
	public static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (SteamTurbineRow tRow : STEAM_ROWS) {
			registerController(tRow.path(), () -> new SteamTurbineBlock(tRow, props(tRow.hardness())));
		}
		for (GasTurbineRow tRow : GAS_ROWS) {
			registerController(tRow.path(), () -> new GasTurbineBlock(tRow, props(tRow.hardness())));
		}
	}

	/** One controller block + item registration pair (the GT6Boilers static-block form, the paren debt retired). */
	private static void registerController(String aPath, java.util.function.Supplier<Block> aBlock) {
		BLOCKS_BY_PATH.put(aPath, BLOCKS.register(aPath, aBlock));
		ITEMS_BY_PATH.put(aPath, ITEMS.register(aPath,
				() -> new BlockItem(GT6Turbines.BLOCKS_BY_PATH.get(aPath).get(), new Item.Properties())));
	}

	private static BlockBehaviour.Properties props(float aHardness) {
		return BlockBehaviour.Properties.of().strength(aHardness, aHardness).sound(SoundType.METAL);
	}

	/** The steam-variant block array for the controller BET (the boilerBlockArray form). */
	private static Block[] steamBlockArray() {
		return STEAM_ROWS.stream().map(r -> BLOCKS_BY_PATH.get(r.path()).get()).toArray(Block[]::new);
	}

	/** The gas-variant block array. */
	private static Block[] gasBlockArray() {
		return GAS_ROWS.stream().map(r -> BLOCKS_BY_PATH.get(r.path()).get()).toArray(Block[]::new);
	}

	/** The Steam Turbine BET: one controller class over the four variant blocks (the Large Boiler one-BET-many-blocks form). */
	public static final RegistryObject<BlockEntityType<GTSteamTurbineBlockEntity>> STEAM_TURBINE_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_steam_turbine", () -> BlockEntityType.Builder.of(
					GTSteamTurbineBlockEntity::new, steamBlockArray()).build(null));

	/** The Gas Turbine BET, same shape. */
	public static final RegistryObject<BlockEntityType<GTGasTurbineBlockEntity>> GAS_TURBINE_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_gas_turbine", () -> BlockEntityType.Builder.of(
					GTGasTurbineBlockEntity::new, gasBlockArray()).build(null));

	/** The lookup for the chains — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<Block> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the block carriers — the GTMultiBlockControllerBlock concrete forms
	// -------------------------------------------------------------------------

	/** The steam turbine controller block — the row carrier (the GTLargeBoilerBlock form; no GUI by census, a right-click does nothing). */
	public static class SteamTurbineBlock extends GTMultiBlockControllerBlock {

		private final SteamTurbineRow mRow;

		public SteamTurbineBlock(SteamTurbineRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract — the simpleCodec representative-value
		// form (the BoilerTankBlock precedent; world save/load never runs through it).
		@Override
		protected com.mojang.serialization.MapCodec<? extends SteamTurbineBlock> codec() {
			return simpleCodec(aProperties -> new SteamTurbineBlock(STEAM_ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the BE's config injection read). */
		public SteamTurbineRow row() {
			return mRow;
		}

		/** The Dense Wall block of this row (the NBT_DESIGN column, the boiler wallBlock form). */
		public Block wallBlock() {
			Block tWall = GTMultiBlocks.anyPartBlock(mRow.wallPath());
			return tWall != null ? tWall : GTMultiBlocks.anyPartBlock(DEFAULT_WALL_PATH);
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return STEAM_TURBINE_BE.get();
		}
	}

	/** The gas turbine controller block, same shape. */
	public static class GasTurbineBlock extends GTMultiBlockControllerBlock {

		private final GasTurbineRow mRow;

		public GasTurbineBlock(GasTurbineRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}
		//? if neoforge {
		/*
		@Override
		protected com.mojang.serialization.MapCodec<? extends GasTurbineBlock> codec() {
			return simpleCodec(aProperties -> new GasTurbineBlock(GAS_ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row. */
		public GasTurbineRow row() {
			return mRow;
		}

		/** The Dense Wall block of this row. */
		public Block wallBlock() {
			Block tWall = GTMultiBlocks.anyPartBlock(mRow.wallPath());
			return tWall != null ? tWall : GTMultiBlocks.anyPartBlock(DEFAULT_WALL_PATH);
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GAS_TURBINE_BE.get();
		}
	}

	private GT6Turbines() {}

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
