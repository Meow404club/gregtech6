package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
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

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.block.GTOvenBlock;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * Machine block + BlockEntityType + item registration, card-owned (ADR-P3-4): the deferred
 * registers attach to the mod bus from this self-contained {@code @EventBusSubscriber(MOD)}
 * listener — GT6Mod.java / GTModBusListener.java / GTBlockEntities.java stay untouched
 * (the W2 oven is the first machine, and the machine family gets its own registration
 * home instead of growing the example-chest registry).
 *
 * <p>Also wires {@link GT6RecipeMaps#init()} into the mod lifecycle — the W1 recipe-core
 * handoff left the FURNACE map un-initialized on purpose ("W2 (p4-machine-oven) wires
 * init() into the mod lifecycle", GT6RecipeMaps.java:36-37). init() is idempotent and
 * runs before any BlockEvent/BET registration, so every TileEntityOven resolves
 * {@code RM.Furnace} from its very first tick.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMachines {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The Oven block — the "Basic Machines" MTE family (Loader_MultiTileEntities.java:1288-1291):
	 * hardness/resistance 6.0/6.0 (tier-1 row :1288), metal sound like the upstream
	 * MaterialMachines/soundTypeMetal machine block (Example_Mod.java:162).
	 */
	public static final RegistryObject<net.minecraft.world.level.block.Block> OVEN = BLOCKS.register("oven",
			() -> new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL)));

	/**
	 * The Oven BET: one class, its one block (ADR-P3-1 degenerate shape). Registry path mirrors
	 * TileEntityOven#getTileEntityName like the chest/test-machine pairs.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityOven>> OVEN_BE =
			BLOCK_ENTITY_TYPES.register("oven", () -> BlockEntityType.Builder.of(
					TileEntityOven::new, OVEN.get()).build(null));

	public static final RegistryObject<Item> OVEN_ITEM = ITEMS.register("oven",
			() -> new gregtech6.block.GTComposedNameItem(OVEN.get(), new Item.Properties()));

	// ---------------------------------------------------------------------------
	// the Shredder/Crusher/Lathe machine family (task p7-basicmachine-family ②/③, the
	// T2-T4 full ladder added by task p8-machine-tiers-doinject ①) — the registration rows
	// Loader_MultiTileEntities.java:1294-1309: hardness/resistance 7.0F; NBT_INPUT
	// 32/128/512/2048 → the :126 conversion min = in/2, max = in*2 (TIER_INPUTS below);
	// Crusher alone carries NBT_PARALLEL 4/8/16/32 + NBT_PARALLEL_DURATION T
	// (:1300-1303), Shredder/Lathe register no parallel key → 1. ids lowercase (the
	// 1.20.1 ResourceLocation constraint).
	//
	// NBT_ENERGY_ACCEPTED :1294 (Shredder) / :1300 (Crusher) / :1306 (Lathe) — carrier
	// only; RU/KU net supply pending rotor family; live supply = A-tier via supplyEnergy().
	// (The upstream :501 type-equality gate keeps the EU-only network out of the RU/KU
	// rows — changing the carrier to EU is REFUSED: it would misplace :501/:510/:519 and
	// the root TD.java:216 ALL_ALTERNATING = (F, KU) semantics all hang on the real type.)
	// ---------------------------------------------------------------------------

	/**
	 * The tier energy table (upstream NBT_INPUT 32/128/512/2048 through the :126 conversion
	 * min = in/2 / max = in*2): TIER_INPUTS[tier] = {mInputMin, mInput, mInputMax} for
	 * tier 0 (T1, the :98 field defaults) .. tier 3 (T4).
	 */
	public static final long[][] TIER_INPUTS = {{16, 32, 64}, {64, 128, 256}, {256, 512, 1024}, {1024, 2048, 4096}};

	/**
	 * The shared 4/8/16/32 parallel table (task p26-w1-press-extruder-molds, the card-A
	 * merge constant): the Crusher row (:1300-1303) and the Press rows (:1425-1428
	 * NBT_PARALLEL 4/8/16/32) carry the same ladder — one constant, both consumers
	 * reference it (the 合流互指 ruling, no duplicated ladder). Declared BEFORE its
	 * consumers (the static-initializer order — the illegal-forward-reference lesson).
	 */
	public static final int[] PARALLEL_4_32 = {4, 8, 16, 32};

	/** The Crusher parallel row (upstream NBT_PARALLEL 4/8/16/32, :1300-1303) — the shared {@link #PARALLEL_4_32} ladder (the 合流互指 ruling). */
	public static final int[] CRUSHER_PARALLEL = PARALLEL_4_32;

	// the composed tier-ladder name face (task p20-i18n-compose-rows): the "{Machine} (Tier N)"
	// rows compose from the machine word + the ordinal tier unit over one template
	public static final String MACHINE_DISPLAY_KEY = "gt6.row.machine.display";
	public static final String MACHINE_SHREDDER_UNIT_KEY = "gt6.row.machine.shredder";
	public static final String MACHINE_CRUSHER_UNIT_KEY = "gt6.row.machine.crusher";
	public static final String MACHINE_LATHE_UNIT_KEY = "gt6.row.machine.lathe";
	/** The Press family unit word (task p26-w1-press-extruder-molds, the :101-104 key form). */
	public static final String MACHINE_PRESS_UNIT_KEY = "gt6.row.machine.press";
	/** The Extruder family unit word (task p26-w1-press-extruder-molds, T2-T4; the :101-104 key form). */
	public static final String MACHINE_EXTRUDER_UNIT_KEY = "gt6.row.machine.extruder";
	/** The T1 Extruder unit word — the upstream name column differs at T1: "Low Heat Extruder" (:1406) vs "Extruder" (:1407-1409). */
	public static final String MACHINE_EXTRUDER_LOW_HEAT_UNIT_KEY = "gt6.row.machine.extruder_low_heat";
	/** The Press family display template (the row displayKey face, the Dryer/Distillery convention). */
	public static final String PRESS_DISPLAY_KEY = "gt6.row.machine.press.display";
	/** The Extruder family display template (T2-T4 rows). */
	public static final String EXTRUDER_DISPLAY_KEY = "gt6.row.machine.extruder.display";
	/** The T1 Extruder display template ("Low Heat Extruder (Steel)", the :1406 name column). */
	public static final String EXTRUDER_LOW_HEAT_DISPLAY_KEY = "gt6.row.machine.extruder.low_heat.display";

	/** The tier ordinal unit key ({@code gt6.row.tier.<n>}). */
	public static String machineTierUnitKey(int aTier) {
		return "gt6.row.tier." + aTier;
	}

	/** The pre-composed name supplier of a tier block (the 4-arg GTBasicMachineBlock carrier). */
	private static java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> tierName(
			String aMachineUnitKey, int aTier) {
		return () -> net.minecraft.network.chat.Component.translatable(MACHINE_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(aMachineUnitKey),
				net.minecraft.network.chat.Component.translatable(machineTierUnitKey(aTier)));
	}

	public static final RegistryObject<Block> SHREDDER = BLOCKS.register("shredder",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get()));

	public static final RegistryObject<Block> SHREDDER_T2 = BLOCKS.register("shredder_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, tierName(MACHINE_SHREDDER_UNIT_KEY, 2)));

	public static final RegistryObject<Block> SHREDDER_T3 = BLOCKS.register("shredder_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, tierName(MACHINE_SHREDDER_UNIT_KEY, 3)));

	public static final RegistryObject<Block> SHREDDER_T4 = BLOCKS.register("shredder_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, tierName(MACHINE_SHREDDER_UNIT_KEY, 4)));

	public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get()));

	public static final RegistryObject<Block> CRUSHER_T2 = BLOCKS.register("crusher_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, tierName(MACHINE_CRUSHER_UNIT_KEY, 2)));

	public static final RegistryObject<Block> CRUSHER_T3 = BLOCKS.register("crusher_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, tierName(MACHINE_CRUSHER_UNIT_KEY, 3)));

	public static final RegistryObject<Block> CRUSHER_T4 = BLOCKS.register("crusher_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, tierName(MACHINE_CRUSHER_UNIT_KEY, 4)));

	public static final RegistryObject<Block> LATHE = BLOCKS.register("lathe",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get()));

	public static final RegistryObject<Block> LATHE_T2 = BLOCKS.register("lathe_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, tierName(MACHINE_LATHE_UNIT_KEY, 2)));

	public static final RegistryObject<Block> LATHE_T3 = BLOCKS.register("lathe_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, tierName(MACHINE_LATHE_UNIT_KEY, 3)));

	public static final RegistryObject<Block> LATHE_T4 = BLOCKS.register("lathe_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, tierName(MACHINE_LATHE_UNIT_KEY, 4)));

	/**
	 * One BET per machine FAMILY (task p8-machine-tiers-doinject ①, the P6 barrel-ladder
	 * precedent): the class and the configuration factory are shared, the validBlocks set
	 * multi-attaches the four tier blocks T1-T4 (Builder.of varargs), and the factory reads
	 * the tier off the placed BlockState (the tier rows are compile-time constants upstream
	 * — NBT_INPUT :1294-1309 — so the block identity IS the config selector). The RecipeMap
	 * is read at BE creation time (the volatile survives {@link GT6RecipeMaps#reset()} test
	 * generations); the energy-type carrier is assigned per family (:1294/:1300/:1306
	 * NBT_ENERGY_ACCEPTED). The self-references are class-qualified on purpose — a
	 * simple-name capture inside the field's own initializer is a javac initialization-loop
	 * error.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SHREDDER_BE =
			BLOCK_ENTITY_TYPES.register("shredder", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.SHREDDER_BE.get(), aPos, aState, GT6RecipeMaps.SHREDDER, 1, false,
							TD.Energy.RU, tierOf(aState.getBlock(), GTMachines.SHREDDER, GTMachines.SHREDDER_T2, GTMachines.SHREDDER_T3, GTMachines.SHREDDER_T4),
							GTBasicMachinesMenus.SHREDDER_MENU::get),
					SHREDDER.get(), SHREDDER_T2.get(), SHREDDER_T3.get(), SHREDDER_T4.get()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CRUSHER_BE =
			BLOCK_ENTITY_TYPES.register("crusher", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.CRUSHER_BE.get(), aPos, aState, GT6RecipeMaps.CRUSHER,
							CRUSHER_PARALLEL[tierOf(aState.getBlock(), GTMachines.CRUSHER, GTMachines.CRUSHER_T2, GTMachines.CRUSHER_T3, GTMachines.CRUSHER_T4)], true,
							TD.Energy.KU, tierOf(aState.getBlock(), GTMachines.CRUSHER, GTMachines.CRUSHER_T2, GTMachines.CRUSHER_T3, GTMachines.CRUSHER_T4),
							GTBasicMachinesMenus.CRUSHER_MENU::get),
					CRUSHER.get(), CRUSHER_T2.get(), CRUSHER_T3.get(), CRUSHER_T4.get()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LATHE_BE =
			BLOCK_ENTITY_TYPES.register("lathe", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.LATHE_BE.get(), aPos, aState, GT6RecipeMaps.LATHE, 1, false,
							TD.Energy.RU, tierOf(aState.getBlock(), GTMachines.LATHE, GTMachines.LATHE_T2, GTMachines.LATHE_T3, GTMachines.LATHE_T4),
							GTBasicMachinesMenus.LATHE_MENU::get),
					LATHE.get(), LATHE_T2.get(), LATHE_T3.get(), LATHE_T4.get()).build(null));

	public static final RegistryObject<Item> SHREDDER_ITEM = ITEMS.register("shredder",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T2_ITEM = ITEMS.register("shredder_t2",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T3_ITEM = ITEMS.register("shredder_t3",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T4_ITEM = ITEMS.register("shredder_t4",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER_T4.get(), new Item.Properties()));

	public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T2_ITEM = ITEMS.register("crusher_t2",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T3_ITEM = ITEMS.register("crusher_t3",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T4_ITEM = ITEMS.register("crusher_t4",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER_T4.get(), new Item.Properties()));

	public static final RegistryObject<Item> LATHE_ITEM = ITEMS.register("lathe",
			() -> new gregtech6.block.GTComposedNameItem(LATHE.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T2_ITEM = ITEMS.register("lathe_t2",
			() -> new gregtech6.block.GTComposedNameItem(LATHE_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T3_ITEM = ITEMS.register("lathe_t3",
			() -> new gregtech6.block.GTComposedNameItem(LATHE_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T4_ITEM = ITEMS.register("lathe_t4",
			() -> new gregtech6.block.GTComposedNameItem(LATHE_T4.get(), new Item.Properties()));

	// ---------------------------------------------------------------------------
	// the Dryer family (task p14-dryer-family) — the four rows
	// Loader_MultiTileEntities.java:1476-1480 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "dryer", TD.Energy.HU, RM.Drying, NBT_CHEAP_OVERCLOCKING T,
	// NBT_PARALLEL_DURATION T). ONE family BET over the four tier blocks — the tier
	// config is the MachineRow carried by the placed block (the GT6Boilers row-record
	// precedent), not a tierOf dispatch. The connectivity masks (CS.java:612 SBIT
	// values, the GTBasicMachineBlock copies): energy = SBIT_D|SBIT_A (the :151 read
	// ORs SBIT_A onto NBT_ENERGY_ACCEPTED_SIDES), tank in = SBIT_B|SBIT_L|SBIT_A (the
	// :143 read), tank out = SBIT_U|SBIT_A (the :144 read), item in/out the same
	// :137/:138 columns (data-only — the item-face gate rides the item-IO pool); the
	// four auto sides are the :139/:140/:145/:146 columns (the auto-IO pool, data-only).
	// ---------------------------------------------------------------------------

	/** The Dryer family display template key ({@code gt6.row.dryer.display}, task p20-i18n-compose-rows). */
	public static final String DRYER_DISPLAY_KEY = "gt6.row.dryer.display";

	/** The four Dryer rows, upstream line order :1477-1480 (T1-T4). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> DRYER_ROWS = java.util.List.of(
			dryer("dryer"  , "steel"           , "Steel"           , 20311,  6.0F, 0,   8),
			dryer("dryer_t2", "invar"           , "Invar"           , 20312,  4.0F, 1,  16),
			dryer("dryer_t3", "titanium"        , "Titanium"        , 20313,  9.0F, 2,  32),
			dryer("dryer_t4", "tungsten_carbide", "Tungsten Carbide", 20314, 12.5F, 3, 64));

	/**
	 * One row factory — the four Dryer columns that differ (path/name/id/material/hardness/
	 * tier/parallel) plus the seven that are family constants (RM.Drying through the
	 * supplier, HU, "dryer" texture, the masks, the auto sides, cheap overclocking T) and
	 * the {@code gt6:dryer} MenuType through the supplier (task p16-machine-fluid-gui ① —
	 * the p14 row.menu pool promise redeemed; the supplier form survives the deferred
	 * registration, the BE reads it lazily at createMenu time).
	 */
	private static GTBasicMachineBlock.MachineRow dryer(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, DRYER_DISPLAY_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.DRYING, TD.Energy.HU, "dryer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
				(byte)5 /*NBT_TANK_SIDE_AUTO_IN SIDE_BACK*/, (byte)1 /*NBT_TANK_SIDE_AUTO_OUT SIDE_TOP*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				GTBasicMachinesMenus.DRYER_MENU::get /*gt6:dryer — the GUI pool card redeemed (p16-machine-fluid-gui ①)*/, true /*NBT_CHEAP_OVERCLOCKING T*/);
	}

	/** The registered Dryer blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> DRYER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Dryer items, same keys as {@link #DRYER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> DRYER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : DRYER_ROWS) {
			DRYER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.DRYER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			DRYER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Dryer block list in registration order (the loot/datagen walkers). */
	public static Block[] dryerBlockArray() {
		Block[] rBlocks = new Block[DRYER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : DRYER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine dryer — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block dryerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = DRYER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Dryer family BET: the class and the configuration factory are shared, the
	 * validBlocks set multi-attaches the four tier blocks T1-T4 (the p8 ladder shape), and
	 * the factory reads the row off the placed BlockState's block — the block identity IS
	 * the config carrier here (the row record replaced the tierOf dispatch).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> DRYER_BE =
			BLOCK_ENTITY_TYPES.register("dryer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> dryerMachine(GTMachines.DRYER_BE.get(), aPos, aState),
					dryerBlockArray()).build(null));

	/**
	 * The Dryer BET factory body: the row's parallel/duration/energy-type/tier-input half
	 * rides the shared {@link #machine} helper (the menu travels as the row's
	 * {@code gt6:dryer} supplier, task p16-machine-fluid-gui ①), then the W1a carrier
	 * assignment lands the row's connectivity masks directly on the BE
	 * (mEnergyInputs/mFluidInputs/mFluidOutputs — the :511/:566/:575 gate geometry;
	 * the default 127 zero-regression stays proven for the legacy families).
	 */
	private static TileEntityBasicMachine dryerMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/**
	 * The row→BE mask assignment (public — the offline row test drives it against the
	 * fixture machine; the BET factory calls it right after the {@link #machine} half).
	 */
	public static TileEntityBasicMachine applyRow(TileEntityBasicMachine aMachine, GTBasicMachineBlock.MachineRow aRow) {
		aMachine.mEnergyInputs = aRow.energySides();
		aMachine.mFluidInputs = aRow.fluidIn();
		aMachine.mFluidOutputs = aRow.fluidOut();
		return aMachine;
	}

	// ---------------------------------------------------------------------------
	// the Canner family (task p24-canner-machine) — the four rows
	// Loader_MultiTileEntities.java:1379-1382 (aClass = MultiTileEntityBasicMachineElectric,
	// NBT_TEXTURE "canner", TD.Energy.EU, RM.Canner, NBT_USE_OUTPUT_TANK T, NBT_TANK_CAPACITY
	// 128000/512000/2048000/8192000, no NBT_PARALLEL → 1, no NBT_PARALLEL_DURATION → F).
	// ONE family BET over the four tier blocks — the DRYER_ROWS MachineRow shape, with the
	// tank-capacity and use-output-tank columns riding the {@link #cannerMachine} factory
	// (the row record is out of this card's FILES_SCOPE, so the two extra columns are
	// tier-indexed constants here, the TIER_INPUTS carrier form). The T5 row (:1383,
	// NBT_INPUT 8192 / 32768000) STAYS POOLED — the R2 ruling: this repo has no 5-tier
	// MachineRow precedent, the 5th tier unlocks with the first 5-tier family.
	//
	// The connectivity masks (:1379 verbatim): energy = SBIT_B (the :151 read ORs SBIT_A),
	// item+tank in = SBIT_U|SBIT_L (NBT_INV_SIDE_IN == NBT_TANK_SIDE_IN SBIT_U|SBIT_L, the
	// :137/:143 reads), item+tank out = SBIT_R|SBIT_D (the :138/:144 reads), tank auto in =
	// SIDE_TOP(1), tank auto out = SIDE_BOTTOM(0), item auto in = SIDE_LEFT(2), item auto
	// out = SIDE_RIGHT(4) (the auto-IO pool, data-only). Display name = the VN voltage
	// ladder (upstream "Canning Machine ("+VN[tier]+")", CS.java:154 LV/MV/HV/EV) — NOT a
	// material name like the Heat_T families, so the row mat slugs ARE the voltage ids.
	// ---------------------------------------------------------------------------

	/** The Canner family display template key ({@code gt6.row.canner.display}). */
	public static final String CANNER_DISPLAY_KEY = "gt6.row.canner.display";

	/** The registration-row tank capacities (NBT_TANK_CAPACITY :1379-1382, mB — the mMaxFluid*Size 128000 map ceiling folded into T1, the R6 ruling). */
	public static final long[] CANNER_TANK_CAPACITY = {128000L, 512000L, 2048000L, 8192000L};

	/** The four Canner rows, upstream line order :1379-1382 (T1-T4, the VN ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CANNER_ROWS = java.util.List.of(
			canner("canner"   , "lv", "LV", 20161,  4.0F, 0),
			canner("canner_t2", "mv", "MV", 20162,  4.0F, 1),
			canner("canner_t3", "hv", "HV", 20163,  4.0F, 2),
			canner("canner_t4", "ev", "EV", 20164,  4.0F, 3));

	/**
	 * One row factory — the Canner columns that differ (path/name/id/metaId/hardness/tier)
	 * plus the family constants: EU (NBT_ENERGY_ACCEPTED), the "canner" texture, the :1379
	 * masks and auto sides, parallel 1 / parallelDuration F (no NBT keys), cheap
	 * overclocking T (the :773 loop runs unconditionally in the port) and the
	 * {@code gt6:canner} menu supplier.
	 */
	private static GTBasicMachineBlock.MachineRow canner(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, CANNER_DISPLAY_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.CANNER, TD.Energy.EU, "canner",
				(byte)(GTBasicMachineBlock.SBIT_B) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				GTBasicMachinesMenus.CANNER_MENU::get /*gt6:canner*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/);
	}

	/** The registered Canner blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> CANNER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Canner items, same keys as {@link #CANNER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> CANNER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : CANNER_ROWS) {
			CANNER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.CANNER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			CANNER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Canner block list in registration order (the loot/datagen walkers). */
	public static Block[] cannerBlockArray() {
		Block[] rBlocks = new Block[CANNER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : CANNER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine canner — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block cannerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = CANNER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// ---------------------------------------------------------------------------
	// the Press family (task p26-w1-press-extruder-molds) — the four rows
	// Loader_MultiTileEntities.java:1425-1428 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "press", TD.Energy.KU, RM.Press, NBT_PARALLEL 4/8/16/32 +
	// NBT_PARALLEL_DURATION T, no tank keys — the map is 0/0/0 fluids). ONE family BET
	// over the four tier blocks — the CANNER_ROWS MachineRow shape. Connectivity masks
	// (:1425 verbatim, the :137/:138/:151 reads OR SBIT_A): energy = SBIT_U|SBIT_A
	// (NBT_ENERGY_ACCEPTED_SIDES SBIT_U), item in = SBIT_L|SBIT_A (NBT_INV_SIDE_IN
	// SBIT_L), item out = SBIT_R|SBIT_A (NBT_INV_SIDE_OUT SBIT_R), item auto in =
	// SIDE_LEFT(2), item auto out = SIDE_RIGHT(4); the fluid masks ride 0 (no NBT_TANK
	// keys upstream — the zero-fluid face, data-only). The GUI clause: menu = null (the
	// menu-less carrier, the Distillery precedent — zero new gt6:* MenuType, the use()
	// gate stays inert until the seam-① micro card lands the MUI dispatch).
	// ---------------------------------------------------------------------------

	/** The four Press rows, upstream line order :1425-1428 (T1-T4, the Kinetic_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> PRESS_ROWS = java.util.List.of(
			press("press"   , "steel"           , "Steel"           , 20231,  7.0F, 0),
			press("press_t2", "invar"           , "Invar"           , 20232,  6.0F, 1),
			press("press_t3", "titanium"        , "Titanium"        , 20233,  9.0F, 2),
			press("press_t4", "tungsten_carbide", "Tungsten Carbide", 20234, 12.5F, 3));

	/**
	 * One row factory — the Canner shape: family constants KU / "press" texture /
	 * PARALLEL_4_32 (the :1425-1428 NBT_PARALLEL column, the shared ladder) /
	 * parallelDuration T / the SBIT_U|SBIT_A energy face / the :1425 item masks / zero
	 * fluid masks / the null menu supplier (the GUI clause) / cheap overclocking T.
	 */
	private static GTBasicMachineBlock.MachineRow press(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, PRESS_DISPLAY_KEY, aMetaId, aHardness, aTier,
				PARALLEL_4_32[aTier], true,
				() -> GT6RecipeMaps.PRESS, TD.Energy.KU, "press",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U, the :151 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_IN — the zero-fluid face*/,
				(byte)0 /*no NBT_TANK_SIDE_OUT*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L, the :137 OR*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R, the :138 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_AUTO_IN*/, (byte)0 /*no NBT_TANK_SIDE_AUTO_OUT*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the GUI clause: zero new gt6:* MenuType*/, true);
	}

	/** The registered Press blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> PRESS_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Press items, same keys as {@link #PRESS_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> PRESS_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : PRESS_ROWS) {
			PRESS_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.PRESS_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			PRESS_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.PRESS_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Press block list in registration order (the loot/datagen walkers). */
	public static Block[] pressBlockArray() {
		Block[] rBlocks = new Block[PRESS_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : PRESS_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine press — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block pressBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = PRESS_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Press family BET: the Canner shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> PRESS_BE =
			BLOCK_ENTITY_TYPES.register("press", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> pressMachine(GTMachines.PRESS_BE.get(), aPos, aState),
					pressBlockArray()).build(null));

	/** The Press BET factory body — the dryerMachine body verbatim over the Press rows. */
	private static TileEntityBasicMachine pressMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	// ---------------------------------------------------------------------------
	// the Extruder family (task p26-w1-press-extruder-molds) — the four rows
	// Loader_MultiTileEntities.java:1406-1409 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "extruder", TD.Energy.HU, RM.Extruder, no parallel key → 1, no
	// NBT_PARALLEL_DURATION → F, no tank keys). ONE family BET over the four tier
	// blocks. Connectivity masks (:1406 verbatim, the :137/:138/:151 reads OR SBIT_A):
	// energy = SBIT_D|SBIT_A (NBT_ENERGY_ACCEPTED_SIDES SBIT_D), item in = SBIT_L|SBIT_A,
	// item out = SBIT_R|SBIT_A, item auto in = SIDE_LEFT(2), auto out = SIDE_RIGHT(4);
	// the fluid masks ride 0 (the zero-fluid face). The T1 name column DIFFERS upstream
	// ("Low Heat Extruder" :1406 vs "Extruder" :1407-1409) — the T1 row carries its own
	// display template. menu = null (the GUI clause, the Press ruling).
	// ---------------------------------------------------------------------------

	/** The four Extruder rows, upstream line order :1406-1409 (T1-T4, the Heat_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> EXTRUDER_ROWS = java.util.List.of(
			extruder("extruder"   , "steel"           , "Steel"           , 20201,  6.0F, 0, EXTRUDER_LOW_HEAT_DISPLAY_KEY),
			extruder("extruder_t2", "invar"           , "Invar"           , 20202,  4.0F, 1, EXTRUDER_DISPLAY_KEY),
			extruder("extruder_t3", "titanium"        , "Titanium"        , 20203,  9.0F, 2, EXTRUDER_DISPLAY_KEY),
			extruder("extruder_t4", "tungsten_carbide", "Tungsten Carbide", 20204, 12.5F, 3, EXTRUDER_DISPLAY_KEY));

	/**
	 * One row factory — the Press shape with the extruder columns: HU / the "extruder"
	 * texture / parallel 1 + duration F (no NBT keys) / the SBIT_D|SBIT_A energy face /
	 * the :1406 item masks / zero fluid masks / the per-row display template (the T1
	 * Low Heat face) / the null menu supplier (the GUI clause).
	 */
	private static GTBasicMachineBlock.MachineRow extruder(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, String aDisplayKey) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, aDisplayKey, aMetaId, aHardness, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1406-1409*/,
				() -> GT6RecipeMaps.EXTRUDER, TD.Energy.HU, "extruder",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D, the :151 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_IN — the zero-fluid face*/,
				(byte)0 /*no NBT_TANK_SIDE_OUT*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L, the :137 OR*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R, the :138 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_AUTO_IN*/, (byte)0 /*no NBT_TANK_SIDE_AUTO_OUT*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the GUI clause: zero new gt6:* MenuType*/, true);
	}

	/** The registered Extruder blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> EXTRUDER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Extruder items, same keys as {@link #EXTRUDER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> EXTRUDER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : EXTRUDER_ROWS) {
			EXTRUDER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.EXTRUDER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			EXTRUDER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.EXTRUDER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Extruder block list in registration order (the loot/datagen walkers). */
	public static Block[] extruderBlockArray() {
		Block[] rBlocks = new Block[EXTRUDER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : EXTRUDER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine extruder — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block extruderBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = EXTRUDER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Extruder family BET: the Press shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> EXTRUDER_BE =
			BLOCK_ENTITY_TYPES.register("extruder", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> extruderMachine(GTMachines.EXTRUDER_BE.get(), aPos, aState),
					extruderBlockArray()).build(null));

	/** The Extruder BET factory body — the dryerMachine body verbatim over the Extruder rows. */
	private static TileEntityBasicMachine extruderMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/**
	 * The ONE Canner family BET: the Dryer shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CANNER_BE =
			BLOCK_ENTITY_TYPES.register("canner", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> cannerMachine(GTMachines.CANNER_BE.get(), aPos, aState),
					cannerBlockArray()).build(null));

	/**
	 * The Canner BET factory body — the dryerMachine body plus the two extra registration
	 * columns the MachineRow record does not carry: NBT_USE_OUTPUT_TANK T (the mCanUseOutputTanks
	 * fallback, upstream :132) and NBT_TANK_CAPACITY (:1379-1382, the tier-indexed
	 * {@link #CANNER_TANK_CAPACITY} through {@link TileEntityBasicMachine#applyTankCapacity}).
	 */
	private static TileEntityBasicMachine cannerMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		applyRow(tMachine, tRow);
		tMachine.mCanUseOutputTanks = true; // NBT_USE_OUTPUT_TANK T (:1379)
		tMachine.mTankCapacity = CANNER_TANK_CAPACITY[tRow.tier()]; // NBT_TANK_CAPACITY (:1379-1382)
		tMachine.applyTankCapacity(); // :157-160 — the tanks are re-armed AT the row capacity
		return tMachine;
	}

	// ---------------------------------------------------------------------------
	// the Advanced Crafting Table (task p24-act-machine) — the SINGLE-VARIANT machine
	// (decisions.p24-act-be-form: the upstream MTE extends TileEntityBase09FacingSingle,
	// NOT the TileEntityBasicMachine energy family — zero energy, zero tick auto-craft —
	// so the registration is the OVEN three-row shape, not a MachineRow ladder): one
	// block + one BET + one item, id gt6:advanced_crafting_table (upstream
	// "gt.multitileentity.crafting.advanced", Loader_MultiTileEntities.java:136
	// metalset id 5000+aID — the 1.7.10 numeric id axis is dead on the string axis, the
	// variant ladder consciously unpinned, the deviation ⑥ ruling). Hardness 6.0F
	// (the oven tier-1 row shape, GTMachines:58-59).
	// ---------------------------------------------------------------------------

	public static final RegistryObject<Block> ADVANCED_CRAFTING_TABLE = BLOCKS.register("advanced_crafting_table",
			() -> new gregtech6.block.GTAdvancedCraftingTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL)));

	public static final RegistryObject<BlockEntityType<TileEntityAdvancedCraftingTable>> ADVANCED_CRAFTING_TABLE_BE =
			BLOCK_ENTITY_TYPES.register("advanced_crafting_table", () -> BlockEntityType.Builder.of(
					TileEntityAdvancedCraftingTable::new, ADVANCED_CRAFTING_TABLE.get()).build(null));

	public static final RegistryObject<Item> ADVANCED_CRAFTING_TABLE_ITEM = ITEMS.register("advanced_crafting_table",
			() -> new gregtech6.block.GTComposedNameItem(ADVANCED_CRAFTING_TABLE.get(), new Item.Properties()));


	// ---------------------------------------------------------------------------
	// the Distillery family (task p16-distillery-family ②) — the four rows
	// Loader_MultiTileEntities.java:1398-1401 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "distillery", TD.Energy.HU, RM.Distillery, NBT_CHEAP_OVERCLOCKING T,
	// NBT_PARALLEL_DURATION T). ONE family BET over the four tier blocks — the same
	// MachineRow carrier shape as the Dryer ladder above. The connectivity masks
	// (CS.java:612 SBIT values): energy = SBIT_D|SBIT_A (the :151 read ORs SBIT_A onto
	// NBT_ENERGY_ACCEPTED_SIDES SBIT_D), tank in = SBIT_U|SBIT_L|SBIT_A (the :143 read —
	// upstream NBT_TANK_SIDE_IN SBIT_U|SBIT_L), tank out = SBIT_B|SBIT_A (the :144 read),
	// item in = SBIT_U|SBIT_L|SBIT_A (the :137 read — NBT_INV_SIDE_IN SBIT_U|SBIT_L), item
	// out = SBIT_R|SBIT_A (the :138 read); the four auto sides are the :139/:140/:145/:146
	// columns (data-only — the auto-IO pool). menu = the menu-less carrier (the GUI pool
	// precedent — use() stays inert, the acceptance drives inject+check like the pre-gui
	// dryer).
	// ---------------------------------------------------------------------------

	/** The Distillery family display template key ({@code gt6.row.distillery.display}, task p20-i18n-compose-rows). */
	public static final String DISTILLERY_DISPLAY_KEY = "gt6.row.distillery.display";

	/** The four Distillery rows, upstream line order :1398-1401 (T1-T4). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> DISTILLERY_ROWS = java.util.List.of(
			distillery("distillery"   , "steel"           , "Steel"           , 20191,  6.0F, 0,   8),
			distillery("distillery_t2", "invar"           , "Invar"           , 20192,  4.0F, 1,  16),
			distillery("distillery_t3", "titanium"        , "Titanium"        , 20193,  9.0F, 2,  32),
			distillery("distillery_t4", "tungsten_carbide", "Tungsten Carbide", 20194, 12.5F, 3, 64));

	/**
	 * One row factory — the four Distillery columns that differ (path/name/id/material/
	 * hardness/tier/parallel) plus the seven that are family constants (RM.Distillery
	 * through the supplier, HU, the "distillery" texture, the masks, the auto sides, cheap
	 * overclocking T) and the null menu supplier (the menu-less carrier).
	 */
	private static GTBasicMachineBlock.MachineRow distillery(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, DISTILLERY_DISPLAY_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.DISTILLERY, TD.Energy.HU, "distillery",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)5 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BACK*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the GUI pool precedent*/, true /*NBT_CHEAP_OVERCLOCKING T*/);
	}

	/** The registered Distillery blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> DISTILLERY_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Distillery items, same keys as {@link #DISTILLERY_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> DISTILLERY_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : DISTILLERY_ROWS) {
			DISTILLERY_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.DISTILLERY_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			DISTILLERY_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Distillery block list in registration order (the loot/datagen walkers). */
	public static Block[] distilleryBlockArray() {
		Block[] rBlocks = new Block[DISTILLERY_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : DISTILLERY_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/**
	 * The paint-tint walker (task p21-paintable-tint-render; the Canner ladder joins in task
	 * p24-canner-machine): the pinned 25 machine-domain blocks the client paint BlockColor
	 * registers over — the oven (1) + the shredder/crusher/lathe ladders (4 each = 12) + the
	 * dryer (4) + the distillery (4) + the canner (4),
	 * the upstream {@code MultiTileEntityBasicMachine} render census (the getTexture2 :1014
	 * grayscale x mRGBa consumers). Card_A put the paint capability on the 03 base, so the
	 * whole 03 family can carry PAINT model data (barrels/pipes included) — but this card's
	 * v1 registers the tint over exactly this machine array; the family-wide extension
	 * (connectors/barrels/pipes rendering) stays pooled. Client-side call time only.
	 */
	public static Block[] paintableBlockArray() {
		java.util.List<Block> rBlocks = new java.util.ArrayList<>(25);
		rBlocks.add(OVEN.get());
		for (RegistryObject<Block> tBlock : java.util.List.of(
				SHREDDER, SHREDDER_T2, SHREDDER_T3, SHREDDER_T4,
				CRUSHER, CRUSHER_T2, CRUSHER_T3, CRUSHER_T4,
				LATHE, LATHE_T2, LATHE_T3, LATHE_T4)) {
			rBlocks.add(tBlock.get());
		}
		java.util.Collections.addAll(rBlocks, dryerBlockArray());
		java.util.Collections.addAll(rBlocks, distilleryBlockArray());
		java.util.Collections.addAll(rBlocks, cannerBlockArray());
		java.util.Collections.addAll(rBlocks, pressBlockArray()); // task p26-w1-press-extruder-molds
		java.util.Collections.addAll(rBlocks, extruderBlockArray()); // task p26-w1-press-extruder-molds
		return rBlocks.toArray(new Block[0]);
	}

	/** The lookup for /gt6machine distillery — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block distilleryBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = DISTILLERY_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Distillery family BET: the Dryer shape verbatim — the shared factory, the
	 * four tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> DISTILLERY_BE =
			BLOCK_ENTITY_TYPES.register("distillery", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> distilleryMachine(GTMachines.DISTILLERY_BE.get(), aPos, aState),
					distilleryBlockArray()).build(null));

	/** The Distillery BET factory body — the dryerMachine body verbatim over the Distillery rows. */
	private static TileEntityBasicMachine distilleryMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/** The tier index of a family block (0=T1 .. 3=T4) — BE creation time, every RO is resolved. */
	private static int tierOf(Block aBlock, RegistryObject<Block> aT1, RegistryObject<Block> aT2, RegistryObject<Block> aT3, RegistryObject<Block> aT4) {
		if (aBlock == aT2.get()) return 1;
		if (aBlock == aT3.get()) return 2;
		if (aBlock == aT4.get()) return 3;
		return 0; // aT1 — the BET validBlocks only contain the family's own four blocks
	}

	/**
	 * The BET factory body: constructor-injected config + the per-row carrier and energy
	 * three-value assignment (:1294-1309 NBT_ENERGY_ACCEPTED + NBT_INPUT through the :126
	 * conversion — TileEntityBasicMachine :137 fields are non-final by upstream design :98).
	 * The menu travels as a plain supplier since task p14-dryer-family (null = the
	 * menu-less carrier — the supplier form is what a null menu needs, a RegistryObject
	 * method reference would capture the null receiver and explode at BE creation); since
	 * task p16-machine-fluid-gui the Dryer rows pass the bound {@code gt6:dryer} supplier
	 * the same way.
	 */
	private static TileEntityBasicMachine machine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState, RecipeMap aRecipes, int aParallel, boolean aParallelDuration,
			gregapi.code.TagData aEnergyType, int aTier, @javax.annotation.Nullable java.util.function.Supplier<MenuType<GTBasicMachineMenu>> aMenu) {
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(aType, aPos, aState, aRecipes, aParallel, aParallelDuration, aMenu);
		tMachine.mEnergyTypeAccepted = aEnergyType;
		long[] tInputs = TIER_INPUTS[aTier];
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return tMachine;
	}

	/**
	 * Creative tab for the machine family — the upstream "Basic Machines" MTE-registry category
	 * (Loader_MultiTileEntities.java:1288 aRegistry category column).
	 */
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	public static final RegistryObject<CreativeModeTab> MACHINES_TAB = CREATIVE_MODE_TABS.register("machines",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.machines"))
					.icon(() -> new ItemStack(OVEN_ITEM.get()))
						.displayItems((aParameters, aOutput) -> {
							aOutput.accept(new ItemStack(OVEN_ITEM.get()));
							aOutput.accept(new ItemStack(SHREDDER_ITEM.get())); // task p7-basicmachine-family: +3 machine family rows
							aOutput.accept(new ItemStack(CRUSHER_ITEM.get()));
							aOutput.accept(new ItemStack(LATHE_ITEM.get()));
							// task p8-machine-tiers-doinject ①: the T2-T4 ladder, +9 rows
							aOutput.accept(new ItemStack(SHREDDER_T2_ITEM.get()));
							aOutput.accept(new ItemStack(SHREDDER_T3_ITEM.get()));
							aOutput.accept(new ItemStack(SHREDDER_T4_ITEM.get()));
							aOutput.accept(new ItemStack(CRUSHER_T2_ITEM.get()));
							aOutput.accept(new ItemStack(CRUSHER_T3_ITEM.get()));
							aOutput.accept(new ItemStack(CRUSHER_T4_ITEM.get()));
								aOutput.accept(new ItemStack(LATHE_T2_ITEM.get()));
								aOutput.accept(new ItemStack(LATHE_T3_ITEM.get()));
								aOutput.accept(new ItemStack(LATHE_T4_ITEM.get()));
								// task p14-dryer-family: the Dryer ladder, +4 rows
								for (GTBasicMachineBlock.MachineRow tRow : DRYER_ROWS) {
									aOutput.accept(new ItemStack(DRYER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p16-distillery-family: the Distillery ladder, +4 rows
								for (GTBasicMachineBlock.MachineRow tRow : DISTILLERY_ROWS) {
									aOutput.accept(new ItemStack(DISTILLERY_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p24-canner-machine: the Canner ladder, +4 rows
								for (GTBasicMachineBlock.MachineRow tRow : CANNER_ROWS) {
									aOutput.accept(new ItemStack(CANNER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p26-w1-press-extruder-molds: the Press + Extruder ladders, +8 rows
								for (GTBasicMachineBlock.MachineRow tRow : PRESS_ROWS) {
									aOutput.accept(new ItemStack(PRESS_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : EXTRUDER_ROWS) {
									aOutput.accept(new ItemStack(EXTRUDER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p24-act-machine: the Advanced Crafting Table (the single-variant row)
								aOutput.accept(new ItemStack(ADVANCED_CRAFTING_TABLE_ITEM.get()));
								// task p16-distillery-family ①: the Integrated Circuit ("Selector Tag") —
								// the recipe-slot selector feeds these machines, the machines tab is the
								// nearest live category (the gregapi items tab is not ported, declared)
								aOutput.accept(new ItemStack(gregtech6.item.GT6Circuits.INTEGRATED_CIRCUIT.get()));
								// task p26-w1-press-extruder-molds: the extruder-mold row0 pair — the
								// shaping tools feed the press/extruder, the nearest live category (the
								// circuit precedent; the upstream Technological items tab is not ported)
								aOutput.accept(new ItemStack(gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()));
								aOutput.accept(new ItemStack(gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()));
						})
					.build());

	private GTMachines() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
	 * (GTBlockEntities precedent). Bus.MOD.bus().get() = FMLJavaModLoadingContext.get().getModEventBus()
	 * (Mod.java:81). The recipe-map init rides here: data-only, both sides, before first tick.
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
		GT6RecipeMaps.init(); // W1 handoff: the FURNACE map lifecycle is this card's job (GT6RecipeMaps.java:36-37)
	}
}
