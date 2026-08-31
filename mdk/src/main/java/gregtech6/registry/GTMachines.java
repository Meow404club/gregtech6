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
							GTBasicMachinesMenus.SHREDDER_MENU),
					SHREDDER.get(), SHREDDER_T2.get(), SHREDDER_T3.get(), SHREDDER_T4.get()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CRUSHER_BE =
			BLOCK_ENTITY_TYPES.register("crusher", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.CRUSHER_BE.get(), aPos, aState, GT6RecipeMaps.CRUSHER,
							CRUSHER_PARALLEL[tierOf(aState.getBlock(), GTMachines.CRUSHER, GTMachines.CRUSHER_T2, GTMachines.CRUSHER_T3, GTMachines.CRUSHER_T4)], true,
							TD.Energy.KU, tierOf(aState.getBlock(), GTMachines.CRUSHER, GTMachines.CRUSHER_T2, GTMachines.CRUSHER_T3, GTMachines.CRUSHER_T4),
							GTBasicMachinesMenus.CRUSHER_MENU),
					CRUSHER.get(), CRUSHER_T2.get(), CRUSHER_T3.get(), CRUSHER_T4.get()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LATHE_BE =
			BLOCK_ENTITY_TYPES.register("lathe", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.LATHE_BE.get(), aPos, aState, GT6RecipeMaps.LATHE, 1, false,
							TD.Energy.RU, tierOf(aState.getBlock(), GTMachines.LATHE, GTMachines.LATHE_T2, GTMachines.LATHE_T3, GTMachines.LATHE_T4),
							GTBasicMachinesMenus.LATHE_MENU),
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
	 */
	private static TileEntityBasicMachine machine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState, RecipeMap aRecipes, int aParallel, boolean aParallelDuration,
			gregapi.code.TagData aEnergyType, int aTier, RegistryObject<MenuType<GTBasicMachineMenu>> aMenu) {
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(aType, aPos, aState, aRecipes, aParallel, aParallelDuration, aMenu::get);
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
