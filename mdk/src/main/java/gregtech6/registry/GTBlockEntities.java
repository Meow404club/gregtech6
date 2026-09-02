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
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTExampleChestBlock;
import gregtech6.block.TestMachineBlock;
import gregtech6.tileentity.TestMachineBlockEntity;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.tileentity.energy.GTAxleBlockEntity;
import gregtech6.tileentity.energy.GTCrankBlockEntity;
import gregtech6.tileentity.energy.GTEnergySourceBlockEntity;
import gregtech6.tileentity.example.GTExampleChestBlockEntity;

/**
 * Block + BlockEntityType registration, card-owned (ADR-P3-4): the deferred registers
 * attach to the mod bus from this self-contained {@code @EventBusSubscriber(MOD)}
 * listener — GT6Mod.java / GTModBusListener.java stay untouched (the P2 instance
 * registration form is frozen; new registration never extends it).
 *
 * <p>BET form = one shared BlockEntityType mounting several blocks (ADR-P3-1, the GT6
 * MultiTile "one TE class, many material blocks" counterpart; GTCEu GTBlockEntities
 * CABLE precedent): {@code BlockEntityType.Builder.of(factory, Block instances...)}
 * + {@code build(null)} without a datafixer (vanilla BlockEntityType.java:316-322,
 * mod convention).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBlockEntities {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** Ticking test machine block. */
	public static final RegistryObject<Block> TEST_MACHINE = BLOCKS.register("test_machine",
			() -> new TestMachineBlock(true, BlockBehaviour.Properties.of()));

	/** Passive test machine block — same BE class, no ticker (notick chain equivalence). */
	public static final RegistryObject<Block> TEST_MACHINE_IDLE = BLOCKS.register("test_machine_idle",
			() -> new TestMachineBlock(false, BlockBehaviour.Properties.of()));

	/**
	 * Shared BET: one BlockEntityType, two valid blocks (ADR-P3-1). The registry path
	 * mirrors TestMachineBlockEntity#getTileEntityName — the 1.7.10 "id" write
	 * (TileEntityBase01Root.java:148) and the registration name were the same string,
	 * the BET registry key is its 1.20.1 carrier. BLOCK registers before
	 * BLOCK_ENTITY_TYPES (vanilla registry order), so the RegistryObject .get() calls
	 * in the supplier are safe at registration time.
	 */
	public static final RegistryObject<BlockEntityType<TestMachineBlockEntity>> TEST_MACHINE_BE =
			BLOCK_ENTITY_TYPES.register("test_machine", () -> BlockEntityType.Builder.of(
					TestMachineBlockEntity::new, TEST_MACHINE.get(), TEST_MACHINE_IDLE.get()).build(null));

	// -------------------------------------------------------------------------
	// example chest (task p3-example-machine, WAVE-2)
	// -------------------------------------------------------------------------

	/**
	 * The example chest block — upstream default hardness/resistance (MultiTileEntityChest.java:85
	 * mHardness = 6, mResistance = 3), wooden sound like the wooden chest family.
	 */
	public static final RegistryObject<Block> EXAMPLE_CHEST = BLOCKS.register("example_chest",
			() -> new GTExampleChestBlock(BlockBehaviour.Properties.of().strength(6.0F, 3.0F).sound(SoundType.WOOD)));

	/**
	 * The example chest BET: one class, its one block (ADR-P3-1 shape — the multi-attach form
	 * degenerates to a single valid block until material variants arrive). Registry path mirrors
	 * GTExampleChestBlockEntity#getTileEntityName like the test machine pair.
	 */
	public static final RegistryObject<BlockEntityType<GTExampleChestBlockEntity>> EXAMPLE_CHEST_BE =
			BLOCK_ENTITY_TYPES.register("example_chest", () -> BlockEntityType.Builder.of(
					GTExampleChestBlockEntity::new, EXAMPLE_CHEST.get()).build(null));

	// -------------------------------------------------------------------------
	// electric wire (task p7-d2-cable)
	// -------------------------------------------------------------------------

	/**
	 * Shared electric-wire BET over the whole family (task p7-d2-cable spec ⑤ — the BET type
	 * row lives here per the card, the blocks/items in GTWires; ADR-P3-1 one-type-many-
	 * blocks). The supplier resolves the GTWires block RegistryObjects — safe because the
	 * vanilla registry order fires the Block event before the BlockEntityType event across
	 * DeferredRegisters. Since task p9-wire-family-w1 the valid-block list is the ONE-LINE
	 * family reference {@link GTWires#wireBlockArray()} (the p7 legacy pair + the 620
	 * GTWireSpecs variants). Registry path "wire_electric" mirrors
	 * GTWireBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<GTWireBlockEntity>> WIRE_ELECTRIC_BE =
			BLOCK_ENTITY_TYPES.register("wire_electric", () -> BlockEntityType.Builder.of(
					GTWireBlockEntity::new, GTWires.wireBlockArray()).build(null));

	// -------------------------------------------------------------------------
	// test energy source (task p8-d4-energy-source)
	// -------------------------------------------------------------------------

	/**
	 * The test energy source BET (task p8-d4-energy-source spec ② — the BET type row lives
	 * here per the card, the block/item in GTEnergySources; the WIRE_ELECTRIC_BE
	 * cross-register resolution shape). Registry path "energy_source" mirrors
	 * GTEnergySourceBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<GTEnergySourceBlockEntity>> ENERGY_SOURCE_BE =
			BLOCK_ENTITY_TYPES.register("energy_source", () -> BlockEntityType.Builder.of(
					GTEnergySourceBlockEntity::new, GTEnergySources.ENERGY_SOURCE.get()).build(null));

	// -------------------------------------------------------------------------
	// hand crank (task p12-engine-crank) — the kinetics family's first BET row
	// -------------------------------------------------------------------------

	/**
	 * The Hand Crank BET (task p12-engine-crank spec ③ — the BET type row lives here per
	 * the card, the block/item in GT6Kinetics; the ENERGY_SOURCE_BE cross-register
	 * resolution shape). Registry path "crank" mirrors
	 * GTCrankBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<GTCrankBlockEntity>> CRANK_BE =
			BLOCK_ENTITY_TYPES.register("crank", () -> BlockEntityType.Builder.of(
					GTCrankBlockEntity::new, GT6Kinetics.CRANK.get()).build(null));

	// -------------------------------------------------------------------------
	// axle (task p12-axle-family) — the kinetics family's shared multi-mount row
	// -------------------------------------------------------------------------

	/**
	 * The Axle BET (task p12-axle-family spec ③ — the BET type row lives here per the card,
	 * the 44 blocks/items in GT6Kinetics; the WIRE_ELECTRIC_BE one-type-many-blocks
	 * multi-mount form over {@link GT6Kinetics#axleBlockArray()}). Registry path "axle"
	 * mirrors GTAxleBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<GTAxleBlockEntity>> AXLE_BE =
			BLOCK_ENTITY_TYPES.register("axle", () -> BlockEntityType.Builder.of(
					GTAxleBlockEntity::new, GT6Kinetics.axleBlockArray()).build(null));

	/**
	 * Item register (appended, the material bridge keeps its RegisterEvent stream): the chest
	 * BlockItem. DeferredRegister form per the task card (Bus.MOD.bus().get() self-contained).
	 */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	public static final RegistryObject<Item> EXAMPLE_CHEST_ITEM = ITEMS.register("example_chest",
			() -> new BlockItem(EXAMPLE_CHEST.get(), new Item.Properties()));

	/**
	 * Creative tab for the example chest. Upstream tab archaeology: the chest lives in the
	 * MTE-registry-owned per-category tab — aRegistry.add(..., "Chests", ..., 32745, ...)
	 * (Loader_MultiTileEntities.java:132) creates one shared CreativeTab per category id
	 * (MultiTileEntityRegistry.java:191). The flat "chests" tab is the minimal port equivalent;
	 * the per-registry tab system is a later card.
	 */
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	public static final RegistryObject<CreativeModeTab> CHESTS_TAB = CREATIVE_MODE_TABS.register("chests",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0) // vanilla CreativeModeTab.java:46-48
					.title(Component.translatable("itemGroup.gt6.chests"))
					.icon(() -> new ItemStack(EXAMPLE_CHEST_ITEM.get()))
					.displayItems((aParameters, aOutput) -> aOutput.accept(new ItemStack(EXAMPLE_CHEST_ITEM.get())))
					.build());

	private GTBlockEntities() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any
	 * RegisterEvent (ModLoadingStage order — GTModBusListener.java:16 wires the same
	 * point). Bus.MOD.bus().get() = FMLJavaModLoadingContext.get().getModEventBus()
	 * (Mod.java:81), valid on the mod-loading thread for the whole loading lifecycle.
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
