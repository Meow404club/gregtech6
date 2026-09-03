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
			() -> new BlockItem(OVEN.get(), new Item.Properties()));

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

	/** The Crusher parallel row (upstream NBT_PARALLEL 4/8/16/32, :1300-1303); Shredder/Lathe carry no key → 1. */
	public static final int[] CRUSHER_PARALLEL = {4, 8, 16, 32};

	public static final RegistryObject<Block> SHREDDER = BLOCKS.register("shredder",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get()));

	public static final RegistryObject<Block> SHREDDER_T2 = BLOCKS.register("shredder_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get()));

	public static final RegistryObject<Block> SHREDDER_T3 = BLOCKS.register("shredder_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get()));

	public static final RegistryObject<Block> SHREDDER_T4 = BLOCKS.register("shredder_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get()));

	public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get()));

	public static final RegistryObject<Block> CRUSHER_T2 = BLOCKS.register("crusher_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get()));

	public static final RegistryObject<Block> CRUSHER_T3 = BLOCKS.register("crusher_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get()));

	public static final RegistryObject<Block> CRUSHER_T4 = BLOCKS.register("crusher_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get()));

	public static final RegistryObject<Block> LATHE = BLOCKS.register("lathe",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get()));

	public static final RegistryObject<Block> LATHE_T2 = BLOCKS.register("lathe_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get()));

	public static final RegistryObject<Block> LATHE_T3 = BLOCKS.register("lathe_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get()));

	public static final RegistryObject<Block> LATHE_T4 = BLOCKS.register("lathe_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get()));

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
			() -> new BlockItem(SHREDDER.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T2_ITEM = ITEMS.register("shredder_t2",
			() -> new BlockItem(SHREDDER_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T3_ITEM = ITEMS.register("shredder_t3",
			() -> new BlockItem(SHREDDER_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T4_ITEM = ITEMS.register("shredder_t4",
			() -> new BlockItem(SHREDDER_T4.get(), new Item.Properties()));

	public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
			() -> new BlockItem(CRUSHER.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T2_ITEM = ITEMS.register("crusher_t2",
			() -> new BlockItem(CRUSHER_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T3_ITEM = ITEMS.register("crusher_t3",
			() -> new BlockItem(CRUSHER_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T4_ITEM = ITEMS.register("crusher_t4",
			() -> new BlockItem(CRUSHER_T4.get(), new Item.Properties()));

	public static final RegistryObject<Item> LATHE_ITEM = ITEMS.register("lathe",
			() -> new BlockItem(LATHE.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T2_ITEM = ITEMS.register("lathe_t2",
			() -> new BlockItem(LATHE_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T3_ITEM = ITEMS.register("lathe_t3",
			() -> new BlockItem(LATHE_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T4_ITEM = ITEMS.register("lathe_t4",
			() -> new BlockItem(LATHE_T4.get(), new Item.Properties()));

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

	/** The four Dryer rows, upstream line order :1477-1480 (T1-T4). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> DRYER_ROWS = java.util.List.of(
			dryer("dryer"  , "Dryer (Steel)"          , 20311, "Steel"          ,  6.0F, 0,   8),
			dryer("dryer_t2", "Dryer (Invar)"         , 20312, "Invar"          ,  4.0F, 1,  16),
			dryer("dryer_t3", "Dryer (Titanium)"      , 20313, "Titanium"       ,  9.0F, 2,  32),
			dryer("dryer_t4", "Dryer (Tungsten Carbide)", 20314, "Tungsten Carbide", 12.5F, 3, 64));

	/**
	 * One row factory — the four Dryer columns that differ (path/name/id/material/hardness/
	 * tier/parallel) plus the seven that are family constants (RM.Drying through the
	 * supplier, HU, "dryer" texture, the masks, the auto sides, cheap overclocking T, no
	 * menu — the GUI pool card owns the {@code gt6:dryer} MenuType registration).
	 */
	private static GTBasicMachineBlock.MachineRow dryer(String aPath, String aDisplay, int aMetaId, String aMaterial, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aDisplay, aMetaId, aMaterial, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.DRYING, TD.Energy.HU, "dryer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
				(byte)5 /*NBT_TANK_SIDE_AUTO_IN SIDE_BACK*/, (byte)1 /*NBT_TANK_SIDE_AUTO_OUT SIDE_TOP*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu — the GUI pool card*/, true /*NBT_CHEAP_OVERCLOCKING T*/);
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
			DRYER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
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
	 * rides the shared {@link #machine} helper (menu null — the GUI pool), then the
	 * W1a carrier assignment lands the row's connectivity masks directly on the BE
	 * (mEnergyInputs/mFluidInputs/mFluidOutputs — the :511/:566/:575 gate geometry;
	 * the default 127 zero-regression stays proven for the legacy families).
	 */
	private static TileEntityBasicMachine dryerMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), null);
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
	 * method reference would capture the null receiver and explode at BE creation).
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
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
		GT6RecipeMaps.init(); // W1 handoff: the FURNACE map lifecycle is this card's job (GT6RecipeMaps.java:36-37)
	}
}
