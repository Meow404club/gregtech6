package gregtech6.registry;

import javax.annotation.Nullable;

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
import gregtech6.block.multiblock.GTHeatTransmitterBlock;
import gregtech6.block.multiblock.GTLargeBoilerBlock;
import gregtech6.block.multiblock.GTMultiBlockPartBlock;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.multiblocks.HeatTransmitterBlockEntity;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

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
	 * <p>task p13-large-boiler — the five Dense Wall blocks join the valid list (the card's
	 * sanctioned append; the Heat Transmitter is NOT here — its relaying BE is
	 * {@link #HEAT_TRANSMITTER_BE}).
	 */
	public static final RegistryObject<BlockEntityType<MultiBlockPartBlockEntity>> MULTIBLOCK_PART_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_part", () -> BlockEntityType.Builder.of(
					MultiBlockPartBlockEntity::new,
					sharedPartBlockArray()).build(null));

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
						// task p13-large-boiler — the wall/transmitter parts then the five boiler mains
						for (RegistryObject<Item> tItem : GTMultiBlocks.PART_ITEMS_BY_PATH.values()) aOutput.accept(new ItemStack(tItem.get()));
						for (RegistryObject<Item> tItem : GTMultiBlocks.LARGE_BOILER_ITEMS_BY_PATH.values()) aOutput.accept(new ItemStack(tItem.get()));
					})
					.build());

	private GTMultiBlocks() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
	 * (GTBlockEntities/GTMachines precedent; Bus.MOD.bus().get() = Mod.java:81).
	 */
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

	// ===========================================================================
	// task p13-large-boiler — the Large Boiler family section (append-only per the
	// card EDIT ruling). Rows re-read VERBATIM from Loader_MultiTileEntities.java at
	// implementation time:
	//   :1159-1165 — the five Dense Walls (part ids 18022/18027/18026/18023/18025,
	//     "Dense X Wall", NBT_HARDNESS == NBT_RESISTANCE, NBT_DESIGNS 7);
	//   :1176      — the Heat Transmitter (part id 18101, Invar, hardness 10.0,
	//     NBT_DESIGNS 0);
	//   :1248-1252 — the five Large Boiler mains (ids 17201/17205/17202/17203/17204 —
	//     the line order is NOT id-ordered, kept verbatim), each NBT_DESIGN naming its
	//     Dense Wall and NBT_OUTPUT_SU = raw * STEAM_PER_EU (the loader's CS.java:240
	//     = 2), NBT_HARDNESS == NBT_RESISTANCE.
	// The upstream class default mBoilerWalls = 18002 (the PLAIN Stainless Steel Wall,
	// :66) is a defensive value no Loader row relies on (all five mains carry
	// NBT_DESIGN); the 18002 plain wall itself is NOT in this port's six part rows —
	// the defensive fallback resolves to the 18022 Dense Stainless Steel Wall instead
	// (same material, the declared erratum on the task card).
	// ===========================================================================

	/** One Dense Wall / Heat Transmitter part row — the Loader part-id columns. */
	public record MultiblockPartRow(String path, String displayName, int metaId, float hardness) {}

	/**
	 * One Large Boiler variant row — the block-carrier projection of one :1248-1252 line
	 * (the BoilerRow form): {@code outputSteamPerTick} is the raw NBT_OUTPUT_SU value (the
	 * loader already multiplied by STEAM_PER_EU 2), {@code wallPath} is the NBT_DESIGN
	 * Dense Wall, hardness == resistance.
	 */
	public record LargeBoilerRow(String path, String displayName, String material, int metaId,
			long outputSteamPerTick, float hardness, String wallPath) {}

	/** The five Dense Wall rows (:1159-1165, the registration order). */
	public static final java.util.List<MultiblockPartRow> WALL_ROWS = java.util.List.of(
			new MultiblockPartRow("dense_wall_stainless_steel", "Dense Stainless Steel Wall", 18022,   6.0F),
			new MultiblockPartRow("dense_wall_invar"          , "Dense Invar Wall"           , 18027,   6.0F),
			new MultiblockPartRow("dense_wall_titanium"       , "Dense Titanium Wall"        , 18026,   9.0F),
			new MultiblockPartRow("dense_wall_tungstensteel"  , "Dense Tungstensteel Wall"   , 18023,  12.5F),
			new MultiblockPartRow("dense_wall_adamantium"     , "Dense Adamantium Wall"      , 18025, 100.0F));

	/** The Heat Transmitter row (:1176). */
	public static final MultiblockPartRow TRANSMITTER_ROW = new MultiblockPartRow("heat_transmitter", "Heat Transmitter", 18101, 10.0F);

	/** The five Large Boiler rows (:1248-1252, the upstream line order — raw NBT_OUTPUT_SU 4096/4096/8192/16384/131072). */
	public static final java.util.List<LargeBoilerRow> LARGE_BOILER_ROWS = java.util.List.of(
			boilerRow("Stainless Steel", "Stainless Steel Boiler Main Barometer", 17201,   4096,   6.0F, "dense_wall_stainless_steel"),
			boilerRow("Invar"          , "Invar Boiler Main Barometer"           , 17205,   4096,   6.0F, "dense_wall_invar"),
			boilerRow("Titanium"       , "Titanium Boiler Main Barometer"        , 17202,   8192,   9.0F, "dense_wall_titanium"),
			boilerRow("Tungstensteel"  , "Tungstensteel Boiler Main Barometer"   , 17203,  16384,  12.5F, "dense_wall_tungstensteel"),
			boilerRow("Adamantium"     , "Adamantium Boiler Main Barometer"      , 17204, 131072, 100.0F, "dense_wall_adamantium"));

	/** A boiler row builder — the path is {@code large_boiler_<slug>}, the output carries the loader's *STEAM_PER_EU. */
	private static LargeBoilerRow boilerRow(String aMaterial, String aDisplay, int aMetaId, long aRawOutput, float aHardness, String aWallPath) {
		return new LargeBoilerRow("large_boiler_" + aMaterial.toLowerCase(java.util.Locale.ROOT).replace(" ", "_"),
				aDisplay, aMaterial, aMetaId, aRawOutput * GTFluids.STEAM_PER_EU, aHardness, aWallPath);
	}

	/** The part properties (hardness == resistance on every row; the METAL sound, the BoilerRow convention). */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties partProperties(float aHardness) {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(aHardness, aHardness).sound(SoundType.METAL);
	}

	/** The registered wall blocks by path (the BET valid list + the boiler wallBlock resolution + datagen). */
	public static final java.util.Map<String, RegistryObject<GTMultiBlockPartBlock>> WALL_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The wall + transmitter items, same keys as the block maps (the tab walk + the wand stock). */
	public static final java.util.Map<String, RegistryObject<Item>> PART_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered boiler variant blocks by path (the BET valid list + the /gt6multiblock boiler place lookup). */
	public static final java.util.Map<String, RegistryObject<GTLargeBoilerBlock>> LARGE_BOILER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The boiler variant items, same keys (the tab walk + the wand-less placement). */
	public static final java.util.Map<String, RegistryObject<Item>> LARGE_BOILER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The Heat Transmitter block (the 18101 part, the ONLY_ENERGY_IN base layer). */
	public static final RegistryObject<GTHeatTransmitterBlock> HEAT_TRANSMITTER = BLOCKS.register(TRANSMITTER_ROW.path(),
			() -> new GTHeatTransmitterBlock(partProperties(TRANSMITTER_ROW.hardness())));

	static {
		// the five Dense Wall part blocks + items (the shared part BET mounts them — the
		// forward-reference lambda form, the GT6Kinetics P6 lesson: the BET builders below
		// resolve these handles at REGISTER time, after every static field is initialized)
		for (MultiblockPartRow tRow : WALL_ROWS) {
			WALL_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTMultiBlockPartBlock(partProperties(tRow.hardness()))));
			PART_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(WALL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		PART_ITEMS_BY_PATH.put(TRANSMITTER_ROW.path(), ITEMS.register(TRANSMITTER_ROW.path(),
				() -> new BlockItem(HEAT_TRANSMITTER.get(), new Item.Properties())));
		// the five boiler variant blocks + items over ONE shared BE class (the GT6Boilers row form)
		for (LargeBoilerRow tRow : LARGE_BOILER_ROWS) {
			LARGE_BOILER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTLargeBoilerBlock(tRow, partProperties(tRow.hardness()))));
			LARGE_BOILER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(LARGE_BOILER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The Heat Transmitter BET: the energy-relaying part subclass over its one block (the
	 * CokeOven BET degenerate shape; the SHARED part BET cannot mount it — the plain
	 * factory would swallow the firebox's interface-path injections, the class doc ruling).
	 */
	public static final RegistryObject<BlockEntityType<HeatTransmitterBlockEntity>> HEAT_TRANSMITTER_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_heat_transmitter", () -> BlockEntityType.Builder.of(
					HeatTransmitterBlockEntity::new, HEAT_TRANSMITTER.get()).build(null));

	/**
	 * The Large Boiler BET: one controller class over the five variant blocks (the
	 * GT6Boilers one-BET-many-blocks form; the variant config rides the block carrier).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityLargeBoiler>> LARGE_BOILER_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_large_boiler", () -> BlockEntityType.Builder.of(
					TileEntityLargeBoiler::new, boilerBlockArray()).build(null));

	/** The shared part-BET block array: the coke oven bricks then the five Dense Walls (the card's valid-list append). */
	private static net.minecraft.world.level.block.Block[] sharedPartBlockArray() {
		net.minecraft.world.level.block.Block[] rBlocks = new net.minecraft.world.level.block.Block[1 + GTMultiBlocks.WALL_BLOCKS_BY_PATH.size()];
		rBlocks[0] = COKE_OVEN_BRICKS.get();
		int i = 1;
		for (RegistryObject<GTMultiBlockPartBlock> tHandle : GTMultiBlocks.WALL_BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		return rBlocks;
	}

	/** The boiler-variant array for the controller BET (the same varargs shape). */
	private static net.minecraft.world.level.block.Block[] boilerBlockArray() {
		return GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new);
	}

	/** The lookup for /gt6multiblock boiler place — null for an unknown variant path. */
	@Nullable
	public static net.minecraft.world.level.block.Block boilerBlockByPath(String aPath) {
		RegistryObject<GTLargeBoilerBlock> tHandle = LARGE_BOILER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}
}
