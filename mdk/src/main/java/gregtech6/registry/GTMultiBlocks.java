package gregtech6.registry;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.multiblock.GTCokeOvenBlock;
import gregtech6.block.multiblock.GTHeatTransmitterBlock;
import gregtech6.block.multiblock.GTImplosionCompressorBlock;
import gregtech6.block.multiblock.GTMassfabBlock;
import gregtech6.block.multiblock.GTFusionReactorBlock;
import gregtech6.block.multiblock.GTLargeBoilerBlock;
import gregtech6.block.multiblock.GTLightningRodBlock;
import gregtech6.block.multiblock.GTMultiBlockPartBlock;
import gregtech6.block.multiblock.GTVonDaGraaggBlock;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.multiblocks.HeatTransmitterBlockEntity;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;
import gregtech6.tileentity.multiblocks.TileEntityLightningRod;
import gregtech6.tileentity.multiblocks.TileEntityFusionReactor;
import gregtech6.tileentity.multiblocks.TileEntityMassfab;
import gregtech6.tileentity.multiblocks.TileEntityVonDaGraagg;

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
						// task p24-lightning-rod — the controller (the addToolTips item) then the three parts
						aOutput.accept(new ItemStack(GTMultiBlocks.LIGHTNING_ROD_ITEM.get()));
						for (RegistryObject<Item> tItem : GTMultiBlocks.LIGHTNING_ROD_PART_ITEMS_BY_PATH.values()) aOutput.accept(new ItemStack(tItem.get()));
						// task p29-w3-nbtdesign-parts — the part-family expansion (walls, coils, parts, ventilation, processor units, wood wall)
						for (RegistryObject<Item> tItem : GTMultiBlocks.NEW_PART_ITEMS_BY_PATH.values()) aOutput.accept(new ItemStack(tItem.get()));
						// task p29-w3-large-12 — the twelve large-machine controllers
						for (RegistryObject<Item> tItem : GT6LargeMachines.ITEMS_BY_PATH.values()) aOutput.accept(new ItemStack(tItem.get()));
						// task p31-implosion — the Implosion Compressor controller
						aOutput.accept(new ItemStack(GTMultiBlocks.IMPLOSION_COMPRESSOR_ITEM.get()));
						// task p31-graagg — the Von da Graagg controller
						aOutput.accept(new ItemStack(GTMultiBlocks.VON_DA_GRAAGG_ITEM.get()));
						// task p31-massfab — the Large Matter Fabricator controller
						aOutput.accept(new ItemStack(GTMultiBlocks.MASSFAB_ITEM.get()));
						// task p31-fusion — the Fusion Reactor controller
						aOutput.accept(new ItemStack(GTMultiBlocks.FUSION_REACTOR_ITEM.get()));
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

	/** One Dense Wall / Heat Transmitter part row — the Loader part-id columns ({@code matDisplay} is the row material word, verbatim). */
	public record MultiblockPartRow(String path, String matDisplay, int metaId, float hardness) {}

	/**
	 * One Large Boiler variant row — the block-carrier projection of one :1248-1252 line
	 * (the BoilerRow form): {@code outputSteamPerTick} is the raw NBT_OUTPUT_SU value (the
	 * loader already multiplied by STEAM_PER_EU 2), {@code wallPath} is the NBT_DESIGN
	 * Dense Wall, hardness == resistance.
	 */
	public record LargeBoilerRow(String path, String material, int metaId,
			long outputSteamPerTick, float hardness, String wallPath) {}

	/** The composed Dense Wall display template "{@code Dense %s Wall}" — one material slot (task p20-i18n-compose-rows). */
	public static final String DENSE_WALL_DISPLAY_KEY = "gt6.row.dense_wall.display";
	/** The composed Large Boiler display template "{@code %s Boiler Main Barometer}" — one material slot. */
	public static final String LARGE_BOILER_DISPLAY_KEY = "gt6.row.large_boiler.display";

	/** The wall row's material small-unit key (the slug is the path tail after {@code dense_wall_}). */
	public static String wallMatUnitKeyOf(MultiblockPartRow aRow) {
		return "gt6.row.mat." + aRow.path().substring("dense_wall_".length());
	}

	/** The boiler row's material small-unit key (the slug is the path tail after {@code large_boiler_}). */
	public static String boilerMatUnitKeyOf(LargeBoilerRow aRow) {
		return "gt6.row.mat." + aRow.path().substring("large_boiler_".length());
	}

	/** The composed name of a Large Boiler row (the pure compose seam). */
	public static net.minecraft.network.chat.MutableComponent largeBoilerDisplayOf(LargeBoilerRow aRow) {
		return net.minecraft.network.chat.Component.translatable(LARGE_BOILER_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(boilerMatUnitKeyOf(aRow)));
	}

	/** The five Dense Wall rows (:1159-1165, the registration order) + the six Dense additions (:1155-1165, task p29-w3-nbtdesign-parts ③ — appended, the p13 EDIT-ruling append-only shape; 11 = the full metalwalldense family, DESIGNS 7). */
	public static final java.util.List<MultiblockPartRow> WALL_ROWS = java.util.List.of(
			new MultiblockPartRow("dense_wall_stainless_steel", "Stainless Steel", 18022,   6.0F),
			new MultiblockPartRow("dense_wall_invar"          , "Invar"          , 18027,   6.0F),
			new MultiblockPartRow("dense_wall_titanium"       , "Titanium"       , 18026,   9.0F),
			new MultiblockPartRow("dense_wall_tungstensteel"  , "Tungstensteel"  , 18023,  12.5F),
			new MultiblockPartRow("dense_wall_adamantium"     , "Adamantium"     , 18025, 100.0F),
			new MultiblockPartRow("dense_wall_lead"           , "Lead"           , 18031,   6.0F),
			new MultiblockPartRow("dense_wall_bronze"         , "Bronze"         , 18030,   6.0F),
			new MultiblockPartRow("dense_wall_steel"          , "Steel"          , 18029,   6.0F),
			new MultiblockPartRow("dense_wall_galvanized_steel", "Galvanized Steel", 18028,  6.0F),
			new MultiblockPartRow("dense_wall_tungsten"       , "Tungsten"       , 18024,  10.0F),
			new MultiblockPartRow("dense_wall_tantalum_hafnium_carbide", "Ta4HfC5", 18032, 12.5F));

	/** The Heat Transmitter row (:1176) — the ATOMIC form (a bare noun, nothing to compose; the wire_laser/bricks precedent). */
	public static final MultiblockPartRow TRANSMITTER_ROW = new MultiblockPartRow("heat_transmitter", "Heat Transmitter", 18101, 10.0F);

	/** The five Large Boiler rows (:1248-1252, the upstream line order — raw NBT_OUTPUT_SU 4096/4096/8192/16384/131072). */
	public static final java.util.List<LargeBoilerRow> LARGE_BOILER_ROWS = java.util.List.of(
			boilerRow("Stainless Steel", 17201,   4096,   6.0F, "dense_wall_stainless_steel"),
			boilerRow("Invar"          , 17205,   4096,   6.0F, "dense_wall_invar"),
			boilerRow("Titanium"       , 17202,   8192,   9.0F, "dense_wall_titanium"),
			boilerRow("Tungstensteel"  , 17203,  16384,  12.5F, "dense_wall_tungstensteel"),
			boilerRow("Adamantium"     , 17204, 131072, 100.0F, "dense_wall_adamantium"));

	/** A boiler row builder — the path is {@code large_boiler_<slug>}, the output carries the loader's *STEAM_PER_EU. */
	private static LargeBoilerRow boilerRow(String aMaterial, int aMetaId, long aRawOutput, float aHardness, String aWallPath) {
		return new LargeBoilerRow("large_boiler_" + aMaterial.toLowerCase(java.util.Locale.ROOT).replace(" ", "_"),
				aMaterial, aMetaId, aRawOutput * GTFluids.STEAM_PER_EU, aHardness, aWallPath);
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
					() -> new GTMultiBlockPartBlock(partProperties(tRow.hardness()), tRow)));
			PART_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(WALL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		// the transmitter item keeps the atomic name (the block resolves the vanilla default; the
		// composed-name delegate is a no-op for it — same string through Block.getName)
		PART_ITEMS_BY_PATH.put(TRANSMITTER_ROW.path(), ITEMS.register(TRANSMITTER_ROW.path(),
				() -> new GTComposedNameItem(HEAT_TRANSMITTER.get(), new Item.Properties())));
		// the five boiler variant blocks + items over ONE shared BE class (the GT6Boilers row form)
		for (LargeBoilerRow tRow : LARGE_BOILER_ROWS) {
			LARGE_BOILER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTLargeBoilerBlock(tRow, partProperties(tRow.hardness()))));
			LARGE_BOILER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(LARGE_BOILER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
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

	/** The shared part-BET block array: coke oven bricks, the Dense Walls, the Lightning Rod parts, then the p29-w3 part-family expansion (the card's valid-list appends). */
	private static net.minecraft.world.level.block.Block[] sharedPartBlockArray() {
		net.minecraft.world.level.block.Block[] rBlocks = new net.minecraft.world.level.block.Block[1 + GTMultiBlocks.WALL_BLOCKS_BY_PATH.size() + GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.size() + GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.size()];
		rBlocks[0] = COKE_OVEN_BRICKS.get();
		int i = 1;
		for (RegistryObject<GTMultiBlockPartBlock> tHandle : GTMultiBlocks.WALL_BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		for (RegistryObject<GTMultiBlockPartBlock> tHandle : GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		for (RegistryObject<GTMultiBlockPartBlock> tHandle : GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
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

	// ===========================================================================
	// task p24-lightning-rod — the Lightning Rod family section (append-only per the
	// card EDIT ruling). Rows re-read VERBATIM from Loader_MultiTileEntities.java at
	// implementation time:
	//   :1151 — the Tungsten Wall (part id 18004, ANY.W, hardness == resistance 10.0,
	//     texture "metalwall", NBT_DESIGNS 7 — the plain machine-wall family, NOT the
	//     dense-wall family, the card's census correction);
	//   :1168 — the Large Niobium-Titanium Coil (part id 18041, hardness == resistance
	//     6.0, texture "coil", NBT_DESIGNS 1);
	//   :1179 — the Lightning Rod (part id 18104, hardness == resistance 8.0, texture
	//     "lightningrod", NBT_DESIGNS 0);
	//   :1282 — the controller "Lightning Rod Electric Output" (MTE 17998, hardness ==
	//     resistance 10.0, texture "lightningrod", NBT_CAPACITY 18000 * VREC[6] — the
	//     compile-time CAPACITY constant, TileEntityLightningRod).
	// All three part names are ATOMIC (bare nouns, nothing to compose — the TRANSMITTER_ROW
	// form), so the blocks ride the ROW-LESS GTMultiBlockPartBlock constructor (the
	// row-carrying one composes the Dense Wall template and would slice these paths wrong);
	// the display names land on the vanilla block.gt6.&lt;path&gt; keys (the coke-oven-bricks
	// shape).
	// ===========================================================================

	/** The three Lightning Rod part rows (Loader :1151/:1168/:1179, the registration order). */
	public static final java.util.List<MultiblockPartRow> LIGHTNING_ROD_PART_ROWS = java.util.List.of(
			new MultiblockPartRow("machine_wall_tungsten", "Tungsten Wall", 18004, 10.0F),
			new MultiblockPartRow("niobium_titanium_coil", "Large Niobium-Titanium Coil", 18041, 6.0F),
			new MultiblockPartRow("lightning_rod", "Lightning Rod", 18104, 8.0F));

	/** The three Lightning Rod part blocks by path (the BET valid list + the BE part resolution + datagen). */
	public static final java.util.Map<String, RegistryObject<GTMultiBlockPartBlock>> LIGHTNING_ROD_PART_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The three Lightning Rod part items, same keys (the tab walk + the wand stock). */
	public static final java.util.Map<String, RegistryObject<Item>> LIGHTNING_ROD_PART_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The Lightning Rod controller block (FACING + FORMED from the base; the facing is structurally meaningless). */
	public static final RegistryObject<GTLightningRodBlock> LIGHTNING_ROD = BLOCKS.register("multiblock_lightning_rod",
			() -> new GTLightningRodBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(10.0F, 10.0F).sound(SoundType.METAL)));

	/** The Lightning Rod controller item — the addToolTips replay (the :101-115 face). */
	public static final RegistryObject<Item> LIGHTNING_ROD_ITEM = ITEMS.register("multiblock_lightning_rod",
			() -> new GTLightningRodBlock.Item(LIGHTNING_ROD.get(), new Item.Properties()));

	/**
	 * The Lightning Rod BET: one controller class over its one block (the CokeOven BET
	 * degenerate shape). Registry path mirrors {@link TileEntityLightningRod#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityLightningRod>> LIGHTNING_ROD_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_lightning_rod", () -> BlockEntityType.Builder.of(
					TileEntityLightningRod::new, LIGHTNING_ROD.get()).build(null));

	static {
		// the three Lightning Rod part blocks + items (the shared part BET mounts them; the
		// forward-reference lambda form — the BET builder resolves these handles at REGISTER
		// time, after every static field is initialized, the WALL_ROWS lesson comment above)
		for (MultiblockPartRow tRow : LIGHTNING_ROD_PART_ROWS) {
			LIGHTNING_ROD_PART_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTMultiBlockPartBlock(partProperties(tRow.hardness()))));
			LIGHTNING_ROD_PART_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Lightning Rod part block by path — the coke-oven-bricks defensive default for an unknown path (unreachable in production). */
	public static net.minecraft.world.level.block.Block lightningRodPartBlock(String aPath) {
		RegistryObject<GTMultiBlockPartBlock> tHandle = LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? COKE_OVEN_BRICKS.get() : tHandle.get();
	}

	// ===========================================================================
	// task p31-implosion — the Implosion Compressor controller (Loader_MultiTileEntities
	// .java:1228 re-read VERBATIM at implementation time: meta 17110, MT.TungstenSteel,
	// NBT_HARDNESS 12.5 == NBT_RESISTANCE 12.5, NBT_TEXTURE "implosioncompressor",
	// NBT_INPUT 1 / MIN 1 / MAX 16, NBT_ENERGY_ACCEPTED TD.Energy.TU,
	// NBT_RECIPEMAP RM.ImplosionCompressor, NBT_INV/TANK_SIDE_AUTO_OUT SIDE_BOTTOM,
	// NBT_PARALLEL 64, NBT_NO_CONSTANT_POWER T — the config lands in the BE constructor,
	// the GT6LargeMachineBlockEntity row-injection form). The controller crafting row
	// "CPC/PAP/RMR" is CUT (the W3 absent-input pool: 'R' IL.Processor_Crystal_Ruby and
	// 'A' IL.ROBOT_ARMS[2] have no port item identity; GT6CraftingRecipes.java:880-886).
	// The part block the structure is built from is the EXISTING dense_wall_tungstensteel
	// row (upstream 18023, the :1162 Dense Wall registration) — zero new part blocks.
	// ===========================================================================

	/** The Implosion Compressor controller block — the FACING+FORMED base owns the visuals. */
	public static final RegistryObject<GTImplosionCompressorBlock> IMPLOSION_COMPRESSOR = BLOCKS.register("implosion_compressor",
			() -> new GTImplosionCompressorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL)));

	/** The Implosion Compressor controller item (the plain BlockItem — the twelve-row shape; no addToolTips replay exists this port). */
	public static final RegistryObject<Item> IMPLOSION_COMPRESSOR_ITEM = ITEMS.register("implosion_compressor",
			() -> new BlockItem(IMPLOSION_COMPRESSOR.get(), new Item.Properties()));

	/**
	 * The Implosion Compressor BET: one controller class over its one block (the
	 * CokeOven/LightningRod BET degenerate shape). Registry path mirrors
	 * {@link TileEntityImplosionCompressor#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityImplosionCompressor>> IMPLOSION_COMPRESSOR_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_implosion_compressor", () -> BlockEntityType.Builder.of(
					TileEntityImplosionCompressor::new, IMPLOSION_COMPRESSOR.get()).build(null));

	// ===========================================================================
	// task p31-graagg — the Von da Graagg controller (Loader_MultiTileEntities.java:1280
	// re-read VERBATIM at implementation time: meta 17996, item 17101, "Von da Graagg
	// Generator", MT.SteelGalvanized, NBT_HARDNESS 6.0F == NBT_RESISTANCE 6.0F,
	// NBT_TEXTURE "vondagraagg", NBT_ENERGY_ACCEPTED TD.Energy.EU). The controller
	// crafting row "CSC"/"PMP"/"CEC" is CUT (the W3 absent-input pool convention, the
	// implosion CUT precedent): the 'P' IL.Processor_Crystal_Ruby and 'C' OD_CIRCUITS[6]
	// have no port item identity (and the row is absent-input on 'S' Nether Star gem /
	// 'E' Eye of Ender identities too — one CUT for the whole row). The structure parts
	// are EXISTING rows — dense_wall_galvanized_steel (18028, :1162 family),
	// large_copper_coil (18040, :1170), dense_wall_steel (18029, :1163) — zero new part
	// blocks (the p29-w3-nbtdesign-parts census). The suppression face rides
	// EntityJoinLevelEvent (CheckSpawn has no modern counterpart; the deviation ledger
	// lives on TileEntityVonDaGraagg/GTGraaggSpawnListener).
	// ===========================================================================

	/** The Von da Graagg controller block — the FACING+FORMED base owns the visuals. */
	public static final RegistryObject<GTVonDaGraaggBlock> VON_DA_GRAAGG = BLOCKS.register("von_da_graagg",
			() -> new GTVonDaGraaggBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL)));

	/** The Von da Graagg controller item (the plain BlockItem — the implosion twelve-row shape). */
	public static final RegistryObject<Item> VON_DA_GRAAGG_ITEM = ITEMS.register("von_da_graagg",
			() -> new BlockItem(VON_DA_GRAAGG.get(), new Item.Properties()));

	/**
	 * The Von da Graagg BET: one controller class over its one block (the
	 * CokeOven/LightningRod BET degenerate shape). Registry path mirrors
	 * {@link TileEntityVonDaGraagg#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityVonDaGraagg>> VON_DA_GRAAGG_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_von_da_graagg", () -> BlockEntityType.Builder.of(
					TileEntityVonDaGraagg::new, VON_DA_GRAAGG.get()).build(null));

	// ===========================================================================
	// task p31-massfab — the Large Matter Fabricator controller (Loader_MultiTileEntities
	// .java:1241 re-read VERBATIM at implementation time: meta 17199, item 17101, "Large
	// Matter Fabricator", MT.Pb, NBT_HARDNESS 6.0F == NBT_RESISTANCE 6.0F, NBT_TEXTURE
	// "largemassfab", NBT_INPUT 1 / MIN 1 / MAX 2097152, NBT_ENERGY_ACCEPTED TD.Energy.QU,
	// NBT_RECIPEMAP RM.Massfab, NBT_INV/TANK_SIDE_AUTO_OUT SIDE_BOTTOM, NBT_CHEAP_
	// OVERCLOCKING T, NBT_PARALLEL 64, NBT_PARALLEL_DURATION T, NBT_NO_CONSTANT_POWER T —
	// the config lands in the TileEntityMassfab constructor, the row-injection form).
	// The controller crafting row "FFF"/"FMF"/"FFF" ('M' = the item(18031) Dense Lead
	// Wall, 'F' = IL.FIELD_GENERATORS[5]) is CUT — the 'F' item family has no port
	// identity (the W3 absent-input pool, the implosion/graagg CUT precedent; the same
	// absence CUTs the small Massfab T1-T5 crafting rows "RFS"/"FMF"/"RFS" on the
	// GTMachines side, whose 'R'/'S' Processor_Crystal_Ruby/Sapphire are absent too).
	// The structure parts are EXISTING rows — dense_wall_lead (18031), large_osmium_coil
	// (18044), ventilation_unit (18299), processor_unit_versatile/control/conversion
	// (18200/18202/18204) — zero new part blocks (the p29-w3-nbtdesign-parts census).
	// ===========================================================================

	/** The Large Matter Fabricator controller block — the FACING+FORMED base owns the visuals. */
	public static final RegistryObject<GTMassfabBlock> MASSFAB = BLOCKS.register("large_massfab",
			() -> new GTMassfabBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL)));

	/** The Large Matter Fabricator controller item (the plain BlockItem — the implosion twelve-row shape). */
	public static final RegistryObject<Item> MASSFAB_ITEM = ITEMS.register("large_massfab",
			() -> new BlockItem(MASSFAB.get(), new Item.Properties()));

	/**
	 * The Massfab BET: one controller class over its one block (the
	 * CokeOven/LightningRod BET degenerate shape). Registry path mirrors
	 * {@link TileEntityMassfab#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityMassfab>> MASSFAB_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_massfab", () -> BlockEntityType.Builder.of(
					TileEntityMassfab::new, MASSFAB.get()).build(null));

	// ===========================================================================
	// task p31-fusion — the Fusion Reactor controller (Loader_MultiTileEntities.java:1242
	// re-read VERBATIM at implementation time: meta 17198, item 17101, "Fusion Reactor",
	// MT.SteelGalvanized, NBT_HARDNESS 12.5F == NBT_RESISTANCE 12.5F, NBT_TEXTURE
	// "fusionreactor", NBT_INPUT 8192 / NBT_INPUT_MIN 1 / NBT_INPUT_MAX 16384,
	// NBT_ENERGY_ACCEPTED TD.Energy.TU, NBT_RECIPEMAP RM.Fusion, NBT_ENERGY_ACCEPTED_2
	// TD.Energy.LU, NBT_ENERGY_EMITTED TD.Energy.EU, NBT_SPECIAL_IS_START_ENERGY T — the
	// ignition column IS ported (task p32-ignition-gate, the S31-7 waiver flipped once the
	// laser domain landed the LU economy: the flag IS supplied through readFromNBT2 :112-124
	// -> the :755 write is reachable -> the :809 gate closes until the :497-500 LU decrement
	// pays it, D-D 730*8192*16 ~= 95.6M LU/arm — the flag + the :755/:809 arms live on
	// TileEntityBase10MultiBlockMachine, the :497 charged arm on TileEntityFusionReactor,
	// the constructor row-injection form), NBT_NO_CONSTANT_POWER T — the config
	// lands in the TileEntityFusionReactor constructor, the row-injection form). The
	// controller crafting row "FFF"/"FMF"/"FFF" ('M' = the item(18003) Tungstensteel
	// Wall, 'F' = IL.FIELD_GENERATORS[5]) is CUT — the 'F' item family has no port
	// identity (the W3 absent-input pool, the implosion/graagg/massfab CUT precedent).
	// The structure parts are EXISTING rows — machine_wall_galvanized_steel (18008),
	// machine_wall_tungstensteel (18003, the design 0/2/5/6 'glass' ring),
	// machine_wall_stainless_steel (18002), large_iridium_coil (18045),
	// ventilation_unit (18299), processor_unit_versatile/logic/control (18200/18201/
	// 18202) — zero new part blocks (the p29-w3-nbtdesign-parts census).
	// ===========================================================================

	/** The Fusion Reactor controller block — the FACING+FORMED base owns the visuals. */
	public static final RegistryObject<GTFusionReactorBlock> FUSION_REACTOR = BLOCKS.register("fusion_reactor",
			() -> new GTFusionReactorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL)));

	/** The Fusion Reactor controller item (the plain BlockItem — the massfab twelve-row shape). */
	public static final RegistryObject<Item> FUSION_REACTOR_ITEM = ITEMS.register("fusion_reactor",
			() -> new BlockItem(FUSION_REACTOR.get(), new Item.Properties()));

	/**
	 * The Fusion Reactor BET: one controller class over its one block (the
	 * CokeOven/LightningRod BET degenerate shape). Registry path mirrors
	 * {@link TileEntityFusionReactor#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityFusionReactor>> FUSION_REACTOR_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_fusion_reactor", () -> BlockEntityType.Builder.of(
					TileEntityFusionReactor::new, FUSION_REACTOR.get()).build(null));

	// ===========================================================================
	// task p29-w3-nbtdesign-parts ③ — the part-family expansion (Loader_MultiTileEntities
	// .java:1138-1189 re-read VERBATIM at implementation time). Every row is one
	// MultiTileEntityMultiBlockPart registration: NBT_TEXTURE → the texture family (the
	// per-design model source), NBT_DESIGNS → the DESIGN property range (the variant
	// COUNT, mTextures[bind8(n)+1][6], MultiTileEntityMultiBlockPart.java:138-146),
	// NBT_HARDNESS == NBT_RESISTANCE per the family convention. The DISPLAY faces:
	//   - the METAL WALL rows compose "{@code <mat> Wall}" (the
	//     {@code gt6.row.metal_wall.display} template over the EXISTING gt6.row.mat words —
	//     all eleven are already in the lang table, zero new unit keys);
	//   - every other new row is ATOMIC (the TRANSMITTER_ROW / lightning_rod precedent:
	//     the display lands on the vanilla {@code block.gt6.<path>} key).
	// Reuse rulings (decisions.p29-w3-split-rulings): the Tungsten Wall (18004) is NOT
	// re-registered — machine_wall_tungsten stays exactly where the Lightning Rod family
	// registered it (row normalization = the census/welder/datagen tables carry the row,
	// the registration loop skips it); the Fire Bricks (18000) are NOT re-registered —
	// multiblock_coke_oven_bricks IS the upstream 18000 (the p4 card ruling).
	// ===========================================================================

	/**
	 * One new-form part row — the Loader row columns. {@code display} is the material word
	 * for the metal-wall family (the composed template slot) and the ATOMIC display name
	 * for everything else; {@code designs} is the row's NBT_DESIGNS; {@code textureFamily}
	 * is the NBT_TEXTURE column (the upstream multiblockparts/&lt;family&gt;/&lt;design&gt;/
	 * {colored,overlay}/{bottom,top,side} texture source for the datagen walk).
	 */
	public record PartRow(String path, String display, int metaId, float hardness, int designs, String textureFamily) {

		/** The composed metal-wall template ("{@code %s Wall}"). */
		public static final String METAL_WALL_DISPLAY_KEY = "gt6.row.metal_wall.display";

		/** Whether this row composes its name over the material word (the metal-wall family shape). */
		public boolean metalWall() {
			return path().startsWith("machine_wall_");
		}

		/** The row's material small-unit key (the slug after {@code machine_wall_}; a no-op shape for non-wall rows). */
		public String unitKey() {
			return "gt6.row.mat." + path().substring("machine_wall_".length());
		}
	}

	/** The eleven Metal Wall rows (:1143-1153, the registration order — texture "metalwall", NBT_DESIGNS 7). */
	public static final java.util.List<PartRow> METAL_WALL_ROWS = java.util.List.of(
			new PartRow("machine_wall_lead"                    , "Lead"                     , 18011,   6.0F, 7, "metalwall"),
			new PartRow("machine_wall_bronze"                  , "Bronze"                   , 18010,   6.0F, 7, "metalwall"),
			new PartRow("machine_wall_steel"                   , "Steel"                    , 18009,   6.0F, 7, "metalwall"),
			new PartRow("machine_wall_galvanized_steel"        , "Galvanized Steel"         , 18008,   6.0F, 7, "metalwall"),
			new PartRow("machine_wall_stainless_steel"         , "Stainless Steel"          , 18002,   6.0F, 7, "metalwall"),
			new PartRow("machine_wall_invar"                   , "Invar"                    , 18007,   6.0F, 7, "metalwall"),
			new PartRow("machine_wall_titanium"                , "Titanium"                 , 18006,   9.0F, 7, "metalwall"),
			new PartRow("machine_wall_tungstensteel"           , "Tungstensteel"            , 18003,  12.5F, 7, "metalwall"),
			new PartRow("machine_wall_tungsten"                , "Tungsten"                 , 18004,  10.0F, 7, "metalwall"), // REUSED — the Lightning Rod family's block
			new PartRow("machine_wall_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide" , 18012,  12.5F, 7, "metalwall"),
			new PartRow("machine_wall_adamantium"              , "Adamantium"               , 18005, 100.0F, 7, "metalwall"));

	/** The five new Coil rows (:1167-1172 minus the registered 18041; texture "coil", NBT_DESIGNS 1 — designs 0/1). */
	public static final java.util.List<PartRow> COIL_ROWS = java.util.List.of(
			new PartRow("large_copper_coil"     , "Large Copper Coil"     , 18040, 6.0F, 1, "coil"),
			new PartRow("large_nichrome_coil"   , "Large Nichrome Coil"   , 18042, 6.0F, 1, "coil"),
			new PartRow("large_carborundum_coil", "Large Carborundum Coil", 18043, 6.0F, 1, "coil"),
			new PartRow("large_osmium_coil"     , "Large Osmium Coil"     , 18044, 6.0F, 1, "coil"),
			new PartRow("large_iridium_coil"    , "Large Iridium Coil"    , 18045, 6.0F, 1, "coil"));

	/**
	 * The seven Part rows (:1174-1182 minus the registered transmitter/rod; per-row
	 * NBT_TEXTURE, per-row NBT_DESIGNS). The Bedrock Mining Drill Head (:1178) joined in
	 * task p30-pool-drillhead-18103 — the research.p29-gap-refresh-worldgen-mb
	 * mb_residual.part_miss line: the card-① census carried
	 * 18100/18101/18102/18105/18106/18107/18108 and missed 18103; the parent Bedrock
	 * Mining Drill machine (17999, :1283) itself STAYS pool-deferred (the W6 bedrock
	 * coupling), this row is the standalone part block only.
	 */
	public static final java.util.List<PartRow> PART_ROWS = java.util.List.of(
			new PartRow("centrifuge_part"     , "Centrifuge Part"          , 18100, 12.5F, 8, "centrifugeparts"),
			new PartRow("electrolyzer_part"   , "Electrolyzer Part"        , 18105, 12.5F, 7, "electrolyzerparts"),
			new PartRow("distill_part"        , "Distillation Tower Part"  , 18102,  6.0F, 1, "distillationtowerparts"),
			new PartRow("bedrock_drill_head"  , "Bedrock Mining Drill Head", 18103, 12.5F, 0, "bedrockdrill"),
			new PartRow("sluice_part"         , "Sluice Part"              , 18106,  9.0F, 7, "sluiceparts"),
			new PartRow("crusher_wheels"      , "Crusher Wheels"           , 18107,  9.0F, 3, "crusherwheels"),
			new PartRow("shredder_blades"     , "Shredder Blades"          , 18108,  9.0F, 3, "shredderblades"));

	/** The Ventilation Unit row (:1184 — NBT_DESIGNS 0). */
	public static final PartRow VENTILATION_ROW = new PartRow("ventilation_unit", "Ventilation Unit", 18299, 6.0F, 0, "ventilationunit");

	/** The five Quadcore Processor Unit rows (:1185-1189 — NBT_DESIGNS 0). */
	public static final java.util.List<PartRow> PROCESSOR_UNIT_ROWS = java.util.List.of(
			new PartRow("processor_unit_versatile" , "Versatile Quadcore Processor Unit" , 18200, 6.0F, 0, "processorversatile"),
			new PartRow("processor_unit_logic"     , "Logic Quadcore Processor Unit"     , 18201, 6.0F, 0, "processorlogic"),
			new PartRow("processor_unit_control"   , "Control Quadcore Processor Unit"   , 18202, 6.0F, 0, "processorcontrol"),
			new PartRow("processor_unit_storage"   , "Storage Quadcore Processor Unit"   , 18203, 6.0F, 0, "processorstorage"),
			new PartRow("processor_unit_conversion", "Conversion Quadcore Processor Unit", 18204, 6.0F, 0, "processorconversion"));

	/** The Wood Wall row (:1139 — texture "woodwall", NBT_DESIGNS 0, NBT_FLAMMABILITY 150; the Tank wood-wall precursor). */
	public static final PartRow WOOD_WALL_ROW = new PartRow("wood_wall", "Wood Wall", 18001, 5.0F, 0, "woodwall");

	/** Every new-form row in registration order (the tab walk + the census + the datagen walk). */
	public static final java.util.List<PartRow> NEW_PART_ROWS;
	static {
		java.util.List<PartRow> tRows = new java.util.ArrayList<>();
		tRows.addAll(METAL_WALL_ROWS);
		tRows.addAll(COIL_ROWS);
		tRows.addAll(PART_ROWS);
		tRows.add(VENTILATION_ROW);
		tRows.addAll(PROCESSOR_UNIT_ROWS);
		tRows.add(WOOD_WALL_ROW);
		NEW_PART_ROWS = java.util.List.copyOf(tRows);
	}

	/** The Wood Wall — the NBT_FLAMMABILITY 150 face (upstream TileEntityBase07Paintable :107-108 feeds BOTH the fire spread speed and the flammability). */
	public static final class WoodWallPartBlock extends GTMultiBlockPartBlock {
		public WoodWallPartBlock(Properties aProperties) {
			super(aProperties, WOOD_WALL_ROW.designs(), null);
		}

		@Override
		public int getFlammability(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
			return 150; // NBT_FLAMMABILITY :1139
		}

		@Override
		public boolean isFlammable(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
			return true;
		}

		@Override
		public int getFireSpreadSpeed(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
			return 150; // :107 — the same value drives both fire faces
		}
	}

	/** The composed metal-wall display (the {@code gt6.row.metal_wall.display} template over the existing unit word). */
	public static net.minecraft.network.chat.MutableComponent metalWallDisplayOf(PartRow aRow) {
		return net.minecraft.network.chat.Component.translatable(PartRow.METAL_WALL_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(aRow.unitKey()));
	}

	/** The new-form part blocks by path (the BET valid list + the tab walk + datagen). */
	public static final java.util.Map<String, RegistryObject<GTMultiBlockPartBlock>> NEW_PART_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The new-form part items, same keys (the tab walk + the wand stock). */
	public static final java.util.Map<String, RegistryObject<Item>> NEW_PART_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		// the expansion registrations (the forward-reference lambda form, the WALL_ROWS
		// lesson: the BET builder resolves these handles at REGISTER time). The Tungsten
		// Wall row SKIPS — machine_wall_tungsten stays the Lightning Rod family's
		// registration, zero references changed (decisions.p29-w3-split-rulings).
		for (PartRow tRow : METAL_WALL_ROWS) {
			if (LIGHTNING_ROD_PART_BLOCKS_BY_PATH.containsKey(tRow.path())) continue;
			NEW_PART_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTMultiBlockPartBlock(partProperties(tRow.hardness()), tRow.designs(), metalWallDisplayOf(tRow))));
			NEW_PART_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(NEW_PART_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		// the wood wall rides the WOOD sound (upstream aWooden block column) and the
		// flammability subclass; DESIGNS 0 → no property
		NEW_PART_BLOCKS_BY_PATH.put(WOOD_WALL_ROW.path(), BLOCKS.register(WOOD_WALL_ROW.path(),
				() -> new WoodWallPartBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
						.strength(WOOD_WALL_ROW.hardness(), WOOD_WALL_ROW.hardness()).sound(SoundType.WOOD))));
		NEW_PART_ITEMS_BY_PATH.put(WOOD_WALL_ROW.path(), ITEMS.register(WOOD_WALL_ROW.path(),
				() -> new GTComposedNameItem(NEW_PART_BLOCKS_BY_PATH.get(WOOD_WALL_ROW.path()).get(), new Item.Properties())));
		// the coils + parts + ventilation + processor units (the ATOMIC rows)
		for (PartRow tRow : COIL_ROWS) registerAtomicPart(tRow);
		for (PartRow tRow : PART_ROWS) registerAtomicPart(tRow);
		registerAtomicPart(VENTILATION_ROW);
		for (PartRow tRow : PROCESSOR_UNIT_ROWS) registerAtomicPart(tRow);
	}

	/** One ATOMIC-row registration pair (the name resolves through the vanilla {@code block.gt6.<path>} key). */
	private static void registerAtomicPart(PartRow aRow) {
		NEW_PART_BLOCKS_BY_PATH.put(aRow.path(), BLOCKS.register(aRow.path(),
				() -> new GTMultiBlockPartBlock(partProperties(aRow.hardness()), aRow.designs(), null)));
		NEW_PART_ITEMS_BY_PATH.put(aRow.path(), ITEMS.register(aRow.path(),
				() -> new GTComposedNameItem(NEW_PART_BLOCKS_BY_PATH.get(aRow.path()).get(), new Item.Properties())));
	}

	/** The part BLOCK by path over every registration map (null when the path is unknown; the leg-neutral form — no Forge registry types in the signature). */
	@Nullable
	public static GTMultiBlockPartBlock anyPartBlock(String aPath) {
		RegistryObject<GTMultiBlockPartBlock> tHandle = NEW_PART_BLOCKS_BY_PATH.get(aPath);
		if (tHandle == null) tHandle = WALL_BLOCKS_BY_PATH.get(aPath);
		if (tHandle == null) tHandle = LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}
}
