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
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTExampleChestBlock;
import gregtech6.block.GTFluidSpringBlock;
import gregtech6.block.TestMachineBlock;
import gregtech6.tileentity.TestMachineBlockEntity;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.tileentity.energy.GTAxleBlockEntity;
import gregtech6.tileentity.energy.GTCrankBlockEntity;
import gregtech6.tileentity.energy.GTDieselEngineBlockEntity;
import gregtech6.tileentity.energy.GTEnergySourceBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.GT6WaterWheelBlockEntity;
import gregtech6.tileentity.energy.GTGearBoxBlockEntity;
import gregtech6.tileentity.energy.GTTransformerRotationBlockEntity;
import gregtech6.tileentity.example.GTExampleChestBlockEntity;
import gregtech6.tileentity.misc.GTFluidSpringBlockEntity;

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

	// -------------------------------------------------------------------------
	// wall attachments (task p12-tap-funnel-attachment) — the shared-BET pair
	// -------------------------------------------------------------------------

	/**
	 * The fluid-tap BET (task p12-tap-funnel-attachment spec ⑤ — the BET type rows live
	 * here per the card, the 6 tap blocks/items in GT6Attachments; the CRANK_BE
	 * cross-register resolution shape, the shared-BET multi-mount ADR-P3-1 over the
	 * whole family). Registry path "tap" mirrors GTTapBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.attachment.GTTapBlockEntity>> TAP_BE =
			BLOCK_ENTITY_TYPES.register("tap", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.attachment.GTTapBlockEntity::new, GT6Attachments.tapBlockArray()).build(null));

	/**
	 * The fluid-funnel BET — the same shape over the 6 funnel blocks. Registry path
	 * "funnel" mirrors GTFunnelBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.attachment.GTFunnelBlockEntity>> FUNNEL_BE =
			BLOCK_ENTITY_TYPES.register("funnel", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.attachment.GTFunnelBlockEntity::new, GT6Attachments.funnelBlockArray()).build(null));

	// -------------------------------------------------------------------------
	// steam engine (task p12-engine-steam) — the kinetics family's second BET row
	// -------------------------------------------------------------------------

	/**
	 * The Steam Engine BET (task p12-engine-steam spec ② — the BET type row lives here per
	 * the card, the 28 blocks/items in {@link GT6Kinetics#STEAM_ENGINE_BLOCKS}; the
	 * WIRE_ELECTRIC_BE one-line family-reference shape over
	 * {@link GT6Kinetics#steamEngineBlockArray()}). ADR-P3-1 one-shared-type-many-blocks:
	 * the engine variants are one BE class, the row config rides the block carrier.
	 * Registry path "steam_engine" mirrors
	 * GTSteamEngineBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<GTSteamEngineBlockEntity>> STEAM_ENGINE_BE =
			BLOCK_ENTITY_TYPES.register("steam_engine", () -> BlockEntityType.Builder.of(
					GTSteamEngineBlockEntity::new, GT6Kinetics.steamEngineBlockArray()).build(null));

	// diesel engine (task p12-engine-diesel) — the kinetics family's shared multi-mount row
	// -------------------------------------------------------------------------

	/**
	 * The Diesel Engine BET (task p12-engine-diesel — the BET type row lives here per the
	 * card, the 8 tier blocks/items in GT6Kinetics; the AXLE_BE one-type-many-blocks
	 * multi-mount form over {@link GT6Kinetics#dieselBlockArray()}). Registry path
	 * "diesel_engine" mirrors GTDieselEngineBlockEntity#getTileEntityName like every other
	 * row.
	 */
	public static final RegistryObject<BlockEntityType<GTDieselEngineBlockEntity>> DIESEL_ENGINE_BE =
			BLOCK_ENTITY_TYPES.register("diesel_engine", () -> BlockEntityType.Builder.of(
					GTDieselEngineBlockEntity::new, GT6Kinetics.dieselBlockArray()).build(null));
	// gearbox + rotation transformer (task p12-gearbox-transformer)
	// -------------------------------------------------------------------------

	/**
	 * The GearBox BET (task p12-gearbox-transformer — the BET type row lives here per the
	 * card family form, the block/item in GT6Kinetics; the CRANK_BE single-mount shape).
	 * Registry path "gearbox" mirrors GTGearBoxBlockEntity#getTileEntityName like every
	 * other row.
	 */
	public static final RegistryObject<BlockEntityType<GTGearBoxBlockEntity>> GEARBOX_BE =
			BLOCK_ENTITY_TYPES.register("gearbox", () -> BlockEntityType.Builder.of(
					GTGearBoxBlockEntity::new, GT6Kinetics.GEARBOX.get()).build(null));

	/**
	 * The Rotation Transformer BET (task p12-gearbox-transformer — the CRANK_BE
	 * single-mount shape, the block/item in GT6Kinetics). Registry path
	 * "transformer_rotation" mirrors GTTransformerRotationBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<GTTransformerRotationBlockEntity>> TRANSFORMER_BE =
			BLOCK_ENTITY_TYPES.register("transformer_rotation", () -> BlockEntityType.Builder.of(
					GTTransformerRotationBlockEntity::new, GT6Kinetics.TRANSFORMER_ROTATION.get()).build(null));

	// water wheel (task p28-c-water-wheel) — the kinetics family's RU-source row
	// -------------------------------------------------------------------------

	/**
	 * The Water Wheel BET (task p28-c-water-wheel — the CRANK_BE single-mount shape, the
	 * block/item in GT6Kinetics). Registry path "water_wheel" mirrors
	 * GT6WaterWheelBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<GT6WaterWheelBlockEntity>> WATER_WHEEL_BE =
			BLOCK_ENTITY_TYPES.register("water_wheel", () -> BlockEntityType.Builder.of(
					GT6WaterWheelBlockEntity::new, GT6Kinetics.WATER_WHEEL.get()).build(null));

	// -------------------------------------------------------------------------
	// the burning boxes (task p13-burning-box-family) — the four shared family rows
	// -------------------------------------------------------------------------

	/**
	 * The Solid Burning Box BET (task p13-burning-box-family spec ⑦ — the shared
	 * multi-mount form over the Brick row + the 26 metal Solid rows of
	 * {@link GT6BurningBoxes}; the AXLE_BE one-type-many-blocks shape). The abstract
	 * family base mounts through an anonymous concrete subclass — the SOLID family has
	 * no named concrete BE (the card's "Brick/Metal 走块载体行不另类" ruling).
	 * Registry path "burning_box_solid".
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity>> BURNING_BOX_SOLID_BE =
			BLOCK_ENTITY_TYPES.register("burning_box_solid", () -> BlockEntityType.Builder.of(
					GTBlockEntities::solidBurningBoxFactory,
					GT6BurningBoxes.blockArray(gregtech6.registry.GT6BurningBoxes.Family.SOLID)).build(null));

	/**
	 * The SOLID family factory — the abstract base mounts through an anonymous concrete
	 * subclass, and the shared type resolves at BE-CREATION time (post-registration, the
	 * TestMachineBlockEntity 2-arg ruling — a NULL type would break the ticker identity
	 * gate AND the saveId registry lookup).
	 */
	private static gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity solidBurningBoxFactory(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		return new gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity(BURNING_BOX_SOLID_BE.get(), aPos, aState) {};
	}

	/**
	 * The Liquid Burning Box BET — the 22 FM.Burn liquid rows. Registry path
	 * "burning_box_liquid" mirrors GTGeneratorLiquidBlockEntity.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.generators.GTGeneratorLiquidBlockEntity>> BURNING_BOX_LIQUID_BE =
			BLOCK_ENTITY_TYPES.register("burning_box_liquid", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.energy.generators.GTGeneratorLiquidBlockEntity::new,
					GT6BurningBoxes.blockArray(gregtech6.registry.GT6BurningBoxes.Family.LIQUID)).build(null));

	/**
	 * The Gas Burning Box BET — the 22 FM.Burn gas rows (FM.Burn, NOT FM.Gas — the
	 * Loader :645-673 ruling). Registry path "burning_box_gas" mirrors
	 * GTGeneratorGasBlockEntity.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.generators.GTGeneratorGasBlockEntity>> BURNING_BOX_GAS_BE =
			BLOCK_ENTITY_TYPES.register("burning_box_gas", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.energy.generators.GTGeneratorGasBlockEntity::new,
					GT6BurningBoxes.blockArray(gregtech6.registry.GT6BurningBoxes.Family.GAS)).build(null));

	/**
	 * The Fluidized Bed Burning Box BET — the 26 FM.FluidBed rows. Registry path
	 * "burning_box_fluidbed" mirrors GTGeneratorFluidBedBlockEntity.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.generators.GTGeneratorFluidBedBlockEntity>> BURNING_BOX_FLUIDBED_BE =
			BLOCK_ENTITY_TYPES.register("burning_box_fluidbed", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.energy.generators.GTGeneratorFluidBedBlockEntity::new,
					GT6BurningBoxes.blockArray(gregtech6.registry.GT6BurningBoxes.Family.FLUIDBED)).build(null));

	// -------------------------------------------------------------------------
	// the steam boiler tank (task p13-boiler-tank) — the converters family's BET row
	// -------------------------------------------------------------------------

	/**
	 * The Steam Boiler Tank BET (task p13-boiler-tank spec ⑨ — the BET type row lives here
	 * per the card, the 26 blocks/items in {@link GT6Boilers}; the AXLE_BE one-type-many-
	 * blocks multi-mount form over {@link GT6Boilers#blockArray()}). ADR-P3-1 one-shared-
	 * type-many-blocks: both ladders (Steam + Strong, 13+13) are ONE BE class, the row
	 * config rides the block carrier. Registry path "boiler_tank" mirrors
	 * GTBoilerTankBlockEntity#getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity>> BOILER_TANK_BE =
			BLOCK_ENTITY_TYPES.register("boiler_tank", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity::new, GT6Boilers.blockArray()).build(null));

		// -------------------------------------------------------------------------
	// the bedrock fluid-spring nozzle (task p38-issue5-fluid-spring-nozzle)
	// -------------------------------------------------------------------------

	/**
	 * The fluid-spring nozzle block (the upstream MultiTileEntityFluidSpring 32763 carrier,
	 * WorldgenFluidSpring.java:77-79): worldgen-only — no BlockItem, no loot
	 * ({@code noLootTable} = the upstream Drops_None face, getBlockHardness -1), bedrock-
	 * grade blast resistance (upstream getExplosionResistance2 = the vanilla bedrock
	 * value), the vanilla bedrock {@code strength(-1, 3600000)} property face. The
	 * blockstate/model face is the GT6OreBlockStates fluid-spring band (datagen only).
	 */
	public static final RegistryObject<Block> FLUID_SPRING = BLOCKS.register("fluid_spring",
			() -> new GTFluidSpringBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE)
					.strength(-1.0F, 3600000.0F) // unmineable + the upstream bedrock blast face
					.sound(SoundType.STONE)
					.noLootTable()));

	/**
	 * The fluid-spring nozzle BET (the CRANK_BE single-mount shape). Registry path
	 * "fluid_spring" mirrors GTFluidSpringBlockEntity#getTileEntityName like every row.
	 */
	public static final RegistryObject<BlockEntityType<GTFluidSpringBlockEntity>> FLUID_SPRING_BE =
			BLOCK_ENTITY_TYPES.register("fluid_spring", () -> BlockEntityType.Builder.of(
					GTFluidSpringBlockEntity::new, FLUID_SPRING.get()).build(null));

	// -------------------------------------------------------------------------
	// the storage hoppers (task p26-storage-hopper-family) — the two family rows
	// -------------------------------------------------------------------------

	/**
	 * The regular Hopper BET (task p26-storage-hopper-family — the BET type rows live here
	 * per the card, the 4 blocks/items in GT6Hoppers; the BOILER_TANK_BE one-type-many-blocks
	 * multi-mount form over {@link GT6Hoppers#hopperBlockArray()}). ADR-P3-1: both material
	 * ladders (Bronze 3 / Steel 5) are ONE BE class, the row config rides the block carrier.
	 * Registry path "hopper" mirrors GT6HopperBlockEntity#getTileEntityName like every row.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6HopperBlockEntity>> HOPPER_BE =
			BLOCK_ENTITY_TYPES.register("hopper", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6HopperBlockEntity::new, GT6Hoppers.hopperBlockArray()).build(null));

	/**
	 * The Queue Hopper BET — the same shape over the queue-kind pair
	 * ({@link GT6Hoppers#queueBlockArray()}; the FIFO compaction class). Registry path
	 * "queue_hopper" mirrors GT6QueueHopperBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6QueueHopperBlockEntity>> QUEUE_HOPPER_BE =
			BLOCK_ENTITY_TYPES.register("queue_hopper", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6QueueHopperBlockEntity::new, GT6Hoppers.queueBlockArray()).build(null));

	// -------------------------------------------------------------------------
	// the sensors (task p26-sensors-core, batch p34-sensors-trivial-14) — the 18 BET rows
	// -------------------------------------------------------------------------

	/**
	 * The Progressmeter BET (task p26-sensors-core — the BET type rows live here per the
	 * card, the blocks/items in {@link GT6Sensors}; the CRANK_BE single-mount shape). One
	 * BET per sensor class: the concrete sensors are unrelated subtypes of the abstract
	 * {@code GTSensorBlockEntity} base, so the ADR-P3-1 one-type-many-blocks degenerates
	 * per class (the family grows by appending rows — the p34 batch appended 15).
	 * Registry path "progressmeter" mirrors getTileEntityName like every other row.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6ProgressmeterBlockEntity>> PROGRESSMETER_BE =
			BLOCK_ENTITY_TYPES.register("progressmeter", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6ProgressmeterBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("progressmeter").get()).build(null));

	/** The Fluidometer BET — the Fluid-O-Meter row (Loader:1986). Registry path "fluidometer". */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6FluidometerBlockEntity>> FLUIDOMETER_BE =
			BLOCK_ENTITY_TYPES.register("fluidometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6FluidometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("fluidometer").get()).build(null));

	/** The Electrometer BET — the Electrometer row (Loader:1997). Registry path "electrometer". */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6ElectrometerBlockEntity>> ELECTROMETER_BE =
			BLOCK_ENTITY_TYPES.register("electrometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6ElectrometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("electrometer").get()).build(null));

	// The p34-sensors-trivial-14 batch — Loader_MultiTileEntities.java:1979-1994 row
	// order, one BET per class over its own GT6Sensors block (the CRANK_BE form). The
	// tachometer/geigercounter/laserometer rows stay pooled on their missing seams (the
	// GT6Sensors census-erratum javadoc).
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6ThermometerBlockEntity>> THERMOMETER_BE =
			BLOCK_ENTITY_TYPES.register("thermometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6ThermometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("thermometer").get()).build(null)); // :1979
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6LuminometerBlockEntity>> LUMINOMETER_BE =
			BLOCK_ENTITY_TYPES.register("luminometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6LuminometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("luminometer").get()).build(null)); // :1980
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6ChronometerBlockEntity>> CHRONOMETER_BE =
			BLOCK_ENTITY_TYPES.register("chronometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6ChronometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("chronometer").get()).build(null)); // :1981
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6GibblometerBlockEntity>> GIBBLOMETER_BE =
			BLOCK_ENTITY_TYPES.register("gibblometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6GibblometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("gibblometer").get()).build(null)); // :1982
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6KiloGibblometerBlockEntity>> KILOGIBBLOMETER_BE =
			BLOCK_ENTITY_TYPES.register("kilogibblometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6KiloGibblometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("kilogibblometer").get()).build(null)); // :1983
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6ItemometerBlockEntity>> ITEMOMETER_BE =
			BLOCK_ENTITY_TYPES.register("itemometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6ItemometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("itemometer").get()).build(null)); // :1984
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6StackometerBlockEntity>> STACKOMETER_BE =
			BLOCK_ENTITY_TYPES.register("stackometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6StackometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("stackometer").get()).build(null)); // :1985
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6BucketometerBlockEntity>> BUCKETOMETER_BE =
			BLOCK_ENTITY_TYPES.register("bucketometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6BucketometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("bucketometer").get()).build(null)); // :1987
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6KiloBucketometerBlockEntity>> KILOBUCKETOMETER_BE =
			BLOCK_ENTITY_TYPES.register("kilobucketometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6KiloBucketometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("kilobucketometer").get()).build(null)); // :1988
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6LightWeightometerBlockEntity>> LIGHTWEIGHTOMETER_BE =
			BLOCK_ENTITY_TYPES.register("lightweightometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6LightWeightometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("lightweightometer").get()).build(null)); // :1989
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6MediumWeightometerBlockEntity>> MEDIUMWEIGHTOMETER_BE =
			BLOCK_ENTITY_TYPES.register("mediumweightometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6MediumWeightometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("mediumweightometer").get()).build(null)); // :1990
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6HeavyWeightometerBlockEntity>> HEAVYWEIGHTOMETER_BE =
			BLOCK_ENTITY_TYPES.register("heavyweightometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6HeavyWeightometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("heavyweightometer").get()).build(null)); // :1991
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6SuperHeavyWeightometerBlockEntity>> SUPERHEAVYWEIGHTOMETER_BE =
			BLOCK_ENTITY_TYPES.register("superheavyweightometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6SuperHeavyWeightometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("superheavyweightometer").get()).build(null)); // :1992
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6TpsmeterBlockEntity>> TPSMETER_BE =
			BLOCK_ENTITY_TYPES.register("tpsmeter", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6TpsmeterBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("tpsmeter").get()).build(null)); // :1993
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6PlayerCounterBlockEntity>> PLAYERCOUNTER_BE =
			BLOCK_ENTITY_TYPES.register("playercounter", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6PlayerCounterBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("playercounter").get()).build(null)); // :1994
	// p37-sensors-3 — the pool closure (the anchor's remaining rows :1996/:1998/:1999,
	// one BET per class over its own GT6Sensors block, the CRANK_BE form)
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6GeigerCounterBlockEntity>> GEIGERCOUNTER_BE =
			BLOCK_ENTITY_TYPES.register("geigercounter", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6GeigerCounterBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("geigercounter").get()).build(null)); // :1996
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6TachometerBlockEntity>> TACHOMETER_BE =
			BLOCK_ENTITY_TYPES.register("tachometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6TachometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("tachometer").get()).build(null)); // :1998
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.sensors.GT6LaserometerBlockEntity>> LASEROMETER_BE =
			BLOCK_ENTITY_TYPES.register("laserometer", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.sensors.GT6LaserometerBlockEntity::new, GT6Sensors.BLOCKS_BY_PATH.get("laserometer").get()).build(null)); // :1999

	// -------------------------------------------------------------------------
	// the static storage batch (task p26-storage-static-batch) — the six family rows
	// -------------------------------------------------------------------------

	/**
	 * The Locker BET (task p26-storage-static-batch — the BET type rows live here per the
	 * card, the 2 blocks/items in GT6StaticStorages; the AXLE_BE one-type-many-blocks
	 * multi-mount form over {@link GT6StaticStorages#blockArray}. ADR-P3-1: the metal
	 * ladder (Bronze/Steel) is ONE BE class, the row config rides the block carrier.
	 * Registry path "locker" mirrors GT6LockerBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6LockerBlockEntity>> LOCKER_BE =
			BLOCK_ENTITY_TYPES.register("locker", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6LockerBlockEntity::new,
					GT6StaticStorages.blockArray(gregtech6.registry.GT6StaticStorages.Kind.LOCKER)).build(null));

	/**
	 * The Compartment Drawer BET — the same shape over the two drawer rows (the 144-slot
	 * four-quadrant class). Registry path "drawer_quad" mirrors
	 * GT6DrawerQuadBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6DrawerQuadBlockEntity>> DRAWER_QUAD_BE =
			BLOCK_ENTITY_TYPES.register("drawer_quad", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6DrawerQuadBlockEntity::new,
					GT6StaticStorages.blockArray(gregtech6.registry.GT6StaticStorages.Kind.DRAWER)).build(null));

	/**
	 * The Mechanical Safe BET — the same shape over the two safe rows (the blast-resistant
	 * 15-slot class with the dungeon-loot seam). Registry path "safe_mechanical" mirrors
	 * GT6SafeBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6SafeBlockEntity>> SAFE_BE =
			BLOCK_ENTITY_TYPES.register("safe_mechanical", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6SafeBlockEntity::new,
					GT6StaticStorages.blockArray(gregtech6.registry.GT6StaticStorages.Kind.SAFE_MECHANICAL)).build(null));

	/**
	 * The Key Locked Safe BET — the KeyLocked personality over its two rows (the latch
	 * subclass). Registry path "safe_keylocked" mirrors
	 * GT6SafeKeyLockedBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity>> SAFE_KEYLOCKED_BE =
			BLOCK_ENTITY_TYPES.register("safe_keylocked", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity::new,
					GT6StaticStorages.blockArray(gregtech6.registry.GT6StaticStorages.Kind.SAFE_KEYLOCKED)).build(null));

	/**
	 * The Wooden Bookshelf BET — the vanilla-planks subset rows (the 300-ladder fold, the
	 * declared wave-4 deviation). Registry path "bookshelf" mirrors
	 * GT6BookShelfBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6BookShelfBlockEntity>> BOOKSHELF_BE =
			BLOCK_ENTITY_TYPES.register("bookshelf", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6BookShelfBlockEntity::new,
					GT6StaticStorages.blockArray(gregtech6.registry.GT6StaticStorages.Kind.BOOKSHELF)).build(null));

	/**
	 * The Wooden Bottlecrate BET — the same vanilla-planks subset over the crate class.
	 * Registry path "bottlecrate" mirrors GT6BottleCrateBlockEntity#getTileEntityName.
	 */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.inventories.GT6BottleCrateBlockEntity>> BOTTLECRATE_BE =
			BLOCK_ENTITY_TYPES.register("bottlecrate", () -> BlockEntityType.Builder.of(
					gregtech6.tileentity.inventories.GT6BottleCrateBlockEntity::new,
					GT6StaticStorages.blockArray(gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE)).build(null));


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
}
