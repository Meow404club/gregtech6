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
	// the Shredder/Crusher/Lathe machine family (task p7-basicmachine-family ②/③) — the
	// T1 registration rows Loader_MultiTileEntities.java:1294/:1300/:1306: hardness/
	// resistance 7.0F, NBT_INPUT 32 → the field defaults 16/32/64 (the T2-T4 rows
	// 128/512/2048 are the pool). Energy carrier per row: Shredder/Lathe = TD.Energy.RU,
	// Crusher = TD.Energy.KU. Crusher alone carries NBT_PARALLEL 4 + NBT_PARALLEL_DURATION
	// T (:1300). ids lowercase (1.20.1 ResourceLocation constraint).
	// ---------------------------------------------------------------------------

	public static final RegistryObject<Block> SHREDDER = BLOCKS.register("shredder",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get()));

	public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get()));

	public static final RegistryObject<Block> LATHE = BLOCKS.register("lathe",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get()));

	/**
	 * One BET per machine (the class is shared, the configuration is factory-captured — the
	 * oven's Builder.of degenerate shape with a closure). The RecipeMap is read at BE
	 * creation time (the volatile survives {@link GT6RecipeMaps#reset()} test generations);
	 * the energy-type carrier is assigned per row (:1294/:1300/:1306 NBT_ENERGY_ACCEPTED).
	 * The self-references are class-qualified on purpose — a simple-name capture inside the
	 * field's own initializer is a javac initialization-loop error.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SHREDDER_BE =
			BLOCK_ENTITY_TYPES.register("shredder", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.SHREDDER_BE.get(), aPos, aState, GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU, GTBasicMachinesMenus.SHREDDER_MENU),
					SHREDDER.get()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CRUSHER_BE =
			BLOCK_ENTITY_TYPES.register("crusher", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.CRUSHER_BE.get(), aPos, aState, GT6RecipeMaps.CRUSHER, 4, true, TD.Energy.KU, GTBasicMachinesMenus.CRUSHER_MENU),
					CRUSHER.get()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LATHE_BE =
			BLOCK_ENTITY_TYPES.register("lathe", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.LATHE_BE.get(), aPos, aState, GT6RecipeMaps.LATHE, 1, false, TD.Energy.RU, GTBasicMachinesMenus.LATHE_MENU),
					LATHE.get()).build(null));

	public static final RegistryObject<Item> SHREDDER_ITEM = ITEMS.register("shredder",
			() -> new BlockItem(SHREDDER.get(), new Item.Properties()));

	public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
			() -> new BlockItem(CRUSHER.get(), new Item.Properties()));

	public static final RegistryObject<Item> LATHE_ITEM = ITEMS.register("lathe",
			() -> new BlockItem(LATHE.get(), new Item.Properties()));

	/** The BET factory body: constructor-injected config + the per-row energy-type carrier (:1294/:1300/:1306 NBT_ENERGY_ACCEPTED). */
	private static TileEntityBasicMachine machine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState, RecipeMap aRecipes, int aParallel, boolean aParallelDuration,
			gregapi.code.TagData aEnergyType, RegistryObject<MenuType<GTBasicMachineMenu>> aMenu) {
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(aType, aPos, aState, aRecipes, aParallel, aParallelDuration, aMenu::get);
		tMachine.mEnergyTypeAccepted = aEnergyType;
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
