//? if neoforge {
/*package gregtech6.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GT6Kitchen;
import gregtech6.tileentity.tools.GT6BathingPotBlockEntity;
import gregtech6.tileentity.tools.GT6MixingBowlBlockEntity;
import gregtech6.item.GTBarrelBlockItem;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity; // p26 tail-append
import gregtech6.tileentity.energy.GT6FeConverterBlockEntity; // p28 tail-append
import gregtech6.tileentity.energy.GT6FeSourceBlockEntity; // p28 tail-append
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.inventories.GT6HopperBlockEntity; // p26 tail-append
import gregtech6.tileentity.inventories.GT6QueueHopperBlockEntity; // p26 tail-append
import gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity; // p29-w4 tail-append
import gregtech6.tileentity.multiblocks.GTGasTurbineBlockEntity; // p29-w3 tail-append
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;
import gregtech6.tileentity.multiblocks.GT6HeatExchangerBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;
import gregtech6.tileentity.tank.BarrelFluidHandler;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.GTBarrelItemFluidHandler;

// RegisterCapabilitiesEvent wiring (task p15-adapt-registry-core) — the 1.21.1 leg of the two
// capability seams W3 left open:
// · machines (p15-fork-capability-machines): the six forked BE classes expose a plain
//   getCapability(BlockCapability, Direction) seam method (no @Override — 21.1 deleted
//   BlockEntity#getCapability); this class hands each BlockEntityType to the event with a
//   provider delegating into that seam, per the handoff shape
//   registerBlockEntity(cap, beType, (be, side) -> be.getCapability(cap, side)).
//   Capability coverage mirrors each forge getCapability exactly: shredder/crusher/lathe
//   and the p14 dryer / p16 distillery serve item + fluid, the oven serves item only,
//   boiler tank/steam engine/large boiler/fluid pipe serve fluid only. The commands-card
//   handoff (p15-adapt-commands)
//   adds the coke-oven pair (item + fluid off the TileEntityBase10MultiBlockMachine seam)
//   and the barrel BLOCK fluid face (the fresh-per-call BarrelFluidHandler) — both were
//   CAPABILITY MISSING at runtime before this card.
// · item carrier (p15-fork-carrier-components): GTBarrelBlockItem.initCapabilities is
//   Forge-only; on 21.1 the same GTBarrelItemFluidHandler construction (capacityL AND the
//   gasProof row flag off the block carrier) rides registerItem instead. The item face is
//   wired by a CLASS scan (every GTBarrelBlockItem in the item registry), mirroring the
//   Forge face where initCapabilities lives on the item class — no id list to drift.
//
// Timing truth (bytecode, neoforge-21.1.249 + loader-4.0.44): this event is constructed in
// CapabilityHooks.init and posted via ModLoader.postEventWrapContainerInModOrder; that init
// runs as CommonModLoader's "registration" init task AFTER GameData.postRegisterEvents —
// every RegisterEvent (items, BETs, tabs) has fired and the registries are bound by the time
// this handler runs, so live BuiltInRegistries lookups resolve. registerItem resolves its
// ItemLike arguments EAGERLY (asItem() inside the handler, javap), which is exactly why the
// lookups happen here and not earlier.
//
// Holder references, not id lookups: the registry-blocks card (p15-adapt-registry-blocks)
// forked the registry homes onto DeferredRegister/DeferredHolder, so this class reads the
// BET handles directly — GTMachines.SHREDDER_BE.get() & co. — instead of the former
// BuiltInRegistries id lookups. Same fail-fast semantics, one step earlier: an unbound
// DeferredHolder.get() throws exactly where the Objects.requireNonNull lookup threw, and
// the registry paths stay pinned against the rows by GT6CapabilityWiringSeamTest
// (DeferredHolder.getId() reads the construction-time name, no binding needed — the old
// RegistryObject.getId() precedent verbatim). The barrel carrier seam keeps its registry
// CLASS scan (BuiltInRegistries.ITEM.stream) — there is no holder list to reference: the
// scan mirrors the Forge initCapabilities face, where the capability rides the item class.
//
// Self-contained @EventBusSubscriber (ADR-P3-4), no bus attribute (21.1 deprecates it and
// routes by event type — RegisterCapabilitiesEvent is an IModBusEvent; the GT6DataComponents
// precedent). Deliberately a 1.21.1-only file: the 1.20.1 leg wires the same capabilities
// through the BE getCapability overrides and Item.initCapabilities, which need no
// registration event. Block-comment-free body: the whole file lives inside one wrapper
// comment, hence // comments only (ADR-P15-3 r1 implementation rule).

@EventBusSubscriber(modid = "gt6")
public final class GT6CapabilityWiring {

	private GT6CapabilityWiring() {
	}

	@SubscribeEvent
	public static void onRegisterCapabilities(RegisterCapabilitiesEvent aEvent) {
		registerMachineBlockEntities(aEvent);
		registerCokeOvenFaces(aEvent);
		registerLargeMachineFaces(aEvent); // task p29-w3-large-12 — the twelve large-machine controllers
		registerBarrelBlockFluidHandler(aEvent);
		registerBarrelItemHandlers(aEvent);
		registerFeBattery(aEvent); // the pure-sink fixture row, the W3 dynamo-chain measurement end (tail-append; shared serial file)
		registerFeConverters(aEvent); // task p28-b-fe-converter-machine (tail-append; shared serial file)
		registerFeSource(aEvent); // task p28-b-fe-converter-machine (tail-append; shared serial file)
		registerKitchenFaces(aEvent);
		registerHopperFamily(aEvent); // task p26-storage-hopper-family (tail-append; shared serial file)
		registerStaticStorages(aEvent); // task p26-storage-static-batch (tail-append; shared serial file)
		registerGasTurbine(aEvent); // task p29-w3-turbine-dynamo (tail-append; shared serial file)
		registerDistillationFaces(aEvent); // task p29-w3-distill-crucible (tail-append; shared serial file)
		registerBatteryBoxFamily(aEvent); // task p29-w4-battery-storage (tail-append; shared serial file)
	}

	// -- the machines seam: registerBlockEntity(cap, beType, (be, side) -> be.getCapability(cap, side)) --

	private static void registerMachineBlockEntities(RegisterCapabilitiesEvent aEvent) {
		// the p7 shredder/crusher/lathe ladder — one BE class, three BETs, item + fluid faces
		BlockEntityType<TileEntityBasicMachine> tShredder = GTMachines.SHREDDER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tShredder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tShredder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCrusher = GTMachines.CRUSHER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCrusher,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCrusher,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLathe = GTMachines.LATHE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLathe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLathe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// the p14 dryer + p16 distillery — the same BE class and the same item + fluid
		// faces as the ladder above. 2026-09-05: both families postdate the fork and had
		// no row here — on this node every external hopper push (the
		// VanillaInventoryCodeHooks.insertHook level ItemHandler.BLOCK query) and every
		// cross-machine fluid auto-IO (fluidHandlerAt's level FluidHandler.BLOCK query)
		// landed capability-blind; the 1.20.1 leg answers from the BE override and cannot
		// see the gap. The ADR-P15-4 shape again (the part relay and the barrel quartet
		// were the earlier instances); GT6CapabilityWiringSeamTest
		// .BASIC_MACHINE_FAMILY_FACES now mechanizes the guard against the next family.
		BlockEntityType<TileEntityBasicMachine> tDryer = GTMachines.DRYER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tDryer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tDryer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tDistillery = GTMachines.DISTILLERY_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tDistillery,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tDistillery,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p24-canner-machine — the Canner ladder joins the family: the same BE class,
		// the same item + fluid faces (the tileentity class is shared, so the forge leg's
		// override is already correct; this row is the 21.1 registration only)
		BlockEntityType<TileEntityBasicMachine> tCanner = GTMachines.CANNER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCanner,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCanner,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p26-w1-sifter-compressor-wiremill — the W1 Kinetic trio (Sifter/Compressor/
		// Wiremill) joins the family: the same BE class, the same item + fluid faces — zero
		// fluid recipes are NOT a zero fluid face (the Shredder precedent; the rows carry
		// the 127 all-sides tank defaults)
		BlockEntityType<TileEntityBasicMachine> tSifter = GTMachines.SIFTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSifter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSifter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCompressor = GTMachines.COMPRESSOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCompressor,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCompressor,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tWiremill = GTMachines.WIREMILL_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tWiremill,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tWiremill,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p26-w1-press-extruder-molds — the Press + Extruder ladders join the family:
		// the same BE class, the same item + fluid faces (zero-fluid RECIPE maps, but the
		// fluid FACE stays — the seam-② hard constraint: zero fluid recipes ≠ zero fluid face)
		BlockEntityType<TileEntityBasicMachine> tPress = GTMachines.PRESS_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tPress,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPress,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tExtruder = GTMachines.EXTRUDER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tExtruder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tExtruder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p28-c-ulv-machine-ladder — the Rolling Mill family joins (tail-append;
		// shared serial file): the new single-row family's BET, the same item + fluid
		// faces as every TileEntityBasicMachine family above (the ULV rows of the five
		// existing families need no row here — they ride their family BETs verbatim)
		BlockEntityType<TileEntityBasicMachine> tRollingmill = GTMachines.ROLLINGMILL_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tRollingmill,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tRollingmill,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p29-w1-kinetic-roll-ladder — the Roll Bender / Roll Former / Cluster Mill
		// families join (tail-append; shared serial file): the same item + fluid faces as
		// every TileEntityBasicMachine family above (the RU RollingMill ladder needs no
		// row here — it rides the p28 rollingmill BET verbatim, the ULV-rows precedent)
		BlockEntityType<TileEntityBasicMachine> tRollbender = GTMachines.ROLLBENDER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tRollbender,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tRollbender,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tRollformer = GTMachines.ROLLFORMER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tRollformer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tRollformer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tClustermill = GTMachines.CLUSTERMILL_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tClustermill,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tClustermill,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));

		// task p29-w1-kinetic-process-ladder — the six process families join (tail-append;
		// shared serial file): the Buzzsaw/Squeezer/Centrifuge/Sluice/Sanding Machine/
		// Pressure Washer BETs, the same item + fluid faces as every
		// TileEntityBasicMachine family above (the zero-fluid masks of the sander rows
		// stay a data-only face — the seam-② hard constraint)
		BlockEntityType<TileEntityBasicMachine> tBuzzsaw = GTMachines.BUZZSAW_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBuzzsaw,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBuzzsaw,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tSqueezer = GTMachines.SQUEEZER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSqueezer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSqueezer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCentrifuge = GTMachines.CENTRIFUGE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCentrifuge,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCentrifuge,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tSluice = GTMachines.SLUICE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSluice,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSluice,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tSanding = GTMachines.SANDING_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSanding,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSanding,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tPressurewasher = GTMachines.PRESSURE_WASHER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tPressurewasher,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPressurewasher,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p29-w1-eu-hu-families — the seven eu-hu families join (tail-append;
		// shared serial file): the same BE class, the same item + fluid faces (the
		// mixer/boxinator families' zero-or-fluid rows ride the 127 defaults — zero
		// fluid recipes are NOT a zero fluid face, the seam-② hard constraint)
		BlockEntityType<TileEntityBasicMachine> tMixer = GTMachines.MIXER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tElectricMixer = GTMachines.ELECTRIC_MIXER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tElectricMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tElectricMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tElectricLoom = GTMachines.ELECTRIC_LOOM_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tElectricLoom,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tElectricLoom,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tElectricSifter = GTMachines.ELECTRIC_SIFTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tElectricSifter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tElectricSifter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tBoxinator = GTMachines.BOXINATOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBoxinator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBoxinator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tUnboxinator = GTMachines.UNBOXINATOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tUnboxinator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tUnboxinator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tFermenter = GTMachines.FERMENTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tFermenter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tFermenter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p29-w2-eu-special — the three eu-special families join (tail-append;
		// shared serial file): the Autocrafter/Lightning/Laminator BETs, the same
		// item + fluid faces as every TileEntityBasicMachine family above — the
		// Autocrafter/Laminator rows carry NO tank keys (the 127 all-sides defaults,
		// zero fluid recipes are NOT a zero fluid face) and the Lightning rows the
		// U|L / R|D tank masks the :716-732 output-tank fallback consumes through
		BlockEntityType<TileEntityBasicMachine> tAutocrafter = GTMachines.AUTOCRAFTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tAutocrafter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tAutocrafter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLightning = GTMachines.LIGHTNING_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLightning,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLightning,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLaminator = GTMachines.LAMINATOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLaminator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLaminator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));

		// task p29-w2-exotic-energy — the six exotic-energy families join (tail-append;
		// shared serial file): the Polarizer/MagneticSeparator (MU) + LaserEngraver/
		// LaserWelder (LU) + Freezer/CryoMixer (CU) BETs, the first machine consumers of
		// the exotic energy domains — the same item + fluid faces as every
		// TileEntityBasicMachine family above (the energy TYPE gate lives on the BE's
		// isEnergyType reference-equality face, NOT on this registration; the Laser Welder's
		// no-tank-out row keeps the fluid FACE like every zero-fluid-mask family — the
		// seam-② hard constraint)
		BlockEntityType<TileEntityBasicMachine> tPolarizer = GTMachines.POLARIZER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tPolarizer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPolarizer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tMagneticSeparator = GTMachines.MAGNETIC_SEPARATOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tMagneticSeparator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tMagneticSeparator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLaserEngraver = GTMachines.LASER_ENGRAVER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLaserEngraver,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLaserEngraver,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLaserWelder = GTMachines.LASER_WELDER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLaserWelder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLaserWelder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tFreezer = GTMachines.FREEZER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tFreezer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tFreezer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCryoMixer = GTMachines.CRYO_MIXER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCryoMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCryoMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));

		// task p31-massfab — the small Massfab 5-ladder joins (tail-append; the same item +
		// fluid faces; the auto-out sink arm of the RCON chain consumes the UP fluid face)
		BlockEntityType<TileEntityBasicMachine> tMassfabSmall = GTMachines.MASSFAB_SMALL_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tMassfabSmall,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tMassfabSmall,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));

		// task p32-qu-scanner-replicator — the QU machine pair joins (tail-append; the same
		// item + fluid faces; the replicator arm feeds the matter fluids through the U|L
		// input faces, the scanner arm is item-only)
		BlockEntityType<TileEntityBasicMachine> tMolecularScanner = GTMachines.MOLECULAR_SCANNER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tMolecularScanner,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tMolecularScanner,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tReplicator = GTMachines.REPLICATOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tReplicator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tReplicator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));

		// task p29-w2-eu-core-5tier — the five eu-core families join (tail-append; shared
		// serial file): the Electrolyzer/Injector/Printer/Scanner(Visuals)/Slicer BETs, the
		// same item + fluid faces as every TileEntityBasicMachine family above (the
		// slicer/scannervisuals zero-fluid MASKS stay a data-only face — the seam-② hard
		// constraint; the printer tank-out 127 default likewise)
		BlockEntityType<TileEntityBasicMachine> tElectrolyzer = GTMachines.ELECTROLYZER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tElectrolyzer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tElectrolyzer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tInjector = GTMachines.INJECTOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tInjector,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tInjector,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tPrinter = GTMachines.PRINTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tPrinter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPrinter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tScannerVisuals = GTMachines.SCANNER_VISUALS_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tScannerVisuals,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tScannerVisuals,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tSlicer = GTMachines.SLICER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSlicer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSlicer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));

		// task p29-w2-hu-tu-piggyback — the seven hu-tu families join (tail-append;
		// shared serial file): the same BE class, the same item + fluid faces (the TU four
		// carry tank faces — the coagulator zero-item row rides the 127 inv-in default,
		// a zero-item RECIPE is not a zero-item FACE, the seam-② hard constraint; the
		// ENERGY face rides the gregapi energy net, not these capabilities)
		BlockEntityType<TileEntityBasicMachine> tSteamcracker = GTMachines.STEAM_CRACKER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSteamcracker,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSteamcracker,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCatalyticcracker = GTMachines.CATALYTIC_CRACKER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCatalyticcracker,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCatalyticcracker,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCoagulator = GTMachines.COAGULATOR_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCoagulator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCoagulator,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tGenerifier = GTMachines.GENERIFIER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tGenerifier,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tGenerifier,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tBath = GTMachines.BATH_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBath,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBath,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tAutoclave = GTMachines.AUTOCLAVE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tAutoclave,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tAutoclave,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLoom = GTMachines.LOOM_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLoom,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLoom,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p29-w3-heat-smelter — the two heat families join (the HU machines whose
		// output is the fluid face: the ice row's water lands in mTanksOutput)
		BlockEntityType<TileEntityBasicMachine> tSmelter = GTMachines.SMELTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tSmelter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tSmelter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tMelter = GTMachines.MELTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tMelter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tMelter,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p29-w3-heat-smelter — the HEX controller joins as a FLUID-ONLY face (the
		// :222 fuel door + the :228 overflow drain; no item face on the controller)
				// task p29-w4-eu-bridge — the Roasting Oven ladder joins (tail-append; the
		// smelter/melter shape): the HU machine whose CO2 input rides the fluid-in face
		// and whose CO output lands in mTanksOutput, both faces load-bearing on this node
		BlockEntityType<TileEntityBasicMachine> tRoastingOven = GTMachines.ROASTING_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tRoastingOven,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tRoastingOven,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p34-machines-bumblelyzer-crucible — the two machine families join (tail-append;
		// the smelter/roasting shape): the Bumblelyzer's scan arm drinks the honey tank leg,
		// the Crystallisation Crucible drinks the noble-gas + molten legs, both item faces
		// load-bearing (the bee + the dust input, the scanned bee + the boule output)
		BlockEntityType<TileEntityBasicMachine> tBumblelyzer = GTMachines.BUMBLELYZER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBumblelyzer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBumblelyzer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCrystallisation = GTMachines.CRYSTALLISATION_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCrystallisation,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCrystallisation,
		// task p34-machines-burner-plantalyzer — the two machine families join (tail-append;
		// the roasting shape): the Burner Mixer's tank-in face is the JSON row chemistry's
		// load-bearing fluid INPUT, the Plantalyzer's single tank-in keeps the p34-gui
		// fluid seat live on this node; both BETs item + fluid
		BlockEntityType<TileEntityBasicMachine> tBurnerMixer = GTMachines.BURNER_MIXER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBurnerMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBurnerMixer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tPlantalyzer = GTMachines.PLANTALYZER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tPlantalyzer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPlantalyzer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
BlockEntityType<GT6HeatExchangerBlockEntity> tHeatExchanger = GT6HeatExchangers.HEAT_EXCHANGER_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tHeatExchanger,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// the p4 oven — its forge getCapability serves the gated item handler alone
		BlockEntityType<TileEntityOven> tOven = GTMachines.OVEN_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tOven,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		// task p24-act-machine — the ACT joins as the SECOND item-only face (zero fluid
		// tanks; the base getCapability serves the 71-slot handler through the gated face)
		BlockEntityType<TileEntityAdvancedCraftingTable> tAct = GTMachines.ADVANCED_CRAFTING_TABLE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tAct,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		// the fluid-only faces (p13 boiler family, p12 steam engine, p8 large boiler, p4 pipe)
		BlockEntityType<GTBoilerTankBlockEntity> tBoilerTank = GTBlockEntities.BOILER_TANK_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBoilerTank,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<GTSteamEngineBlockEntity> tEngine = GTBlockEntities.STEAM_ENGINE_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tEngine,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityLargeBoiler> tLargeBoiler = GTMultiBlocks.LARGE_BOILER_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLargeBoiler,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<GTFluidPipeBlockEntity> tPipe = GTFluidPipes.FLUID_PIPE_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPipe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p26-pipe-item — the item pipe family joins (ADR-P15-4 census discipline):
		// the forge getCapability serves the gated SideItemHandler item face alone (zero
		// fluid tanks on the class). Without this row every external hopper push/pull on
		// this node lands capability-blind while the 1.20.1 BE override hides the gap —
		// GT6CapabilityWiringSeamTest pins the registry row against exactly that.
		BlockEntityType<GTItemPipeBlockEntity> tItemPipe = GTItemPipes.ITEM_PIPE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tItemPipe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		// the multiblock PART relay (the pipe-hole family: the collector attach on
		// GTFluidPipeBlockEntity.canConnect + the boiler push face) — the forge face is
		// MultiBlockPartBlockEntity.getCapability relaying ITEM/FLUID to the target
		// controller; the 21.1 provider resolves relayTarget() (the seam widened for
		// this wiring, its stated consumer) and answers through the level query, which
		// lands on the controller's own registered provider. 2026-09-04, ADR-P15-4:
		// without it every pipe-hole position is capability-blind on this node and the
		// /gt6pipe place against a hole reports connections 0 (1.20.1 cannot see the
		// gap — the BE override answers directly). Fluid face only: the chain- and
		// player-reachable face of the part family on this node today.
		BlockEntityType<MultiBlockPartBlockEntity> tPart = GTMultiBlocks.MULTIBLOCK_PART_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPart,
				(aBe, aSide) -> {
					net.minecraft.world.level.block.entity.BlockEntity tTarget = aBe.relayTarget();
					if (tTarget == null || !tTarget.hasLevel()) return null;
					return tTarget.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
							tTarget.getBlockPos(), aSide);
				});
		// task p29-w3-tank-valves — the Tank Main Valve fluid face (the large-boiler row
		// shape): the forge getCapability serves the fresh wrapper-per-call TankValveFluid-
		// Handler; without this row every wall-relayed fill and pipe draw on this node is
		// capability-blind while the 1.20.1 BE override hides the gap (the p26-pipe-item
		// census discipline).
		BlockEntityType<gregtech6.tileentity.multiblocks.GTTankValveBlockEntity> tTankValve = GT6Tanks.TANK_VALVE_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tTankValve,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	// -- the kitchen family (task p26-kitchen-pot-bowl): the pot pair (one shared BET,
	// ADR-P3-1) and the bowl — item + fluid faces both, the machine-family shape; the
	// forge leg answers from the GT6ManualKitchenBlockEntity override (the fresh
	// wrapper-per-call fluid face, the cached side-less item face)
	private static void registerKitchenFaces(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6BathingPotBlockEntity> tPot = GT6Kitchen.BATHING_POT_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tPot,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPot,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<GT6MixingBowlBlockEntity> tBowl = GT6Kitchen.MIXING_BOWL_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBowl,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBowl,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// task p33-food-machines-kitchen — the Juicer joins the kitchen family (the same
		// item + fluid faces; the 0-in/1-out JUICER tank array answers through the same BE)
		BlockEntityType<gregtech6.tileentity.tools.GT6JuicerBlockEntity> tJuicer = GT6Kitchen.JUICER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tJuicer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tJuicer,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	// -- the coke oven (p8 multiblock controller; the commands-card handoff) --
	// The forge face lives on TileEntityBase10MultiBlockMachine.getCapability (item = the
	// gated inventory surface, fluid = the fresh per-side MultiBlockFluidHandler); the 21.1
	// seam member of the same shape (:731) serves both through the same lambda form as the
	// machines above.

	private static void registerCokeOvenFaces(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<TileEntityCokeOven> tOven = GTMultiBlocks.COKE_OVEN_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tOven,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tOven,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	// task p29-w3-distill-crucible — the distillation tower pair (tail-append; shared serial
	// file): the item face rides the inherited machine seam, the FLUID face answers the
	// tower's OWN handler (the input-tank fill + the output-tank drain, the
	// TileEntityDistillationTower.TowerFluidHandler) — the getCapability seam override in
	// the BE, delegated into here exactly like the coke-oven row above.
	private static void registerDistillationFaces(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<gregtech6.registry.GT6Distillation.TileEntityDistillationTower> tTower = GT6Distillation.TOWER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tTower,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tTower,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	// -- the barrel block (p4/p6 tank family) --
	// The forge face is TileEntityBase08Barrel.getCapability: FLUID_HANDLER → a FRESH
	// BarrelFluidHandler per call, the side part of the handler identity (the gasProof-quartet
	// comment there). The provider lambda keeps the fresh-per-call semantics verbatim —
	// NOT a memoized handler, or the first-queried side would freeze into it.
	//
	// ALL FOUR barrel BETs, not just the wood one (2026-09-04, ADR-P15-4 runtime gate):
	// registerBlockEntity resolves against the BET's validBlocks — the wood-only row left
	// metal/plastic/logistics drums capability-blind on this node (live probe: both
	// "gt6tank fill ... CAPABILITY MISSING" sided AND side-less; the forge leg cannot see
	// the gap because every BE overrides getCapability directly). One BET per material
	// row (GTBarrels:83/:104/:207/:240), same fresh-per-call provider for all.

	// -- the twelve large machines (p29-w3-large-12) --
	// The forge face lives on GTLargeMachineBlockEntity.getCapability (item = the base
	// gated inventory surface, fluid = the fill+drain LargeMachineFluidHandler); this
	// provider row delegates to the same BE seam member (the coke-oven-pair form).
	// DECLARED DEVIATION: FILES_SCOPE listed no GT6CapabilityWiring touch — the
	// p29-w2-eu-special precedent applies (the seam test's live-census face + the
	// neoforge capability-blind gap force the shared seam; tail-append, card-③'s own
	// STEAM tail-append rebases on top).

	private static void registerLargeMachineFaces(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6LargeMachines.GTLargeMachineBlockEntity> tBe = GT6LargeMachines.LARGE_MACHINE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	private static void registerBarrelBlockFluidHandler(RegisterCapabilitiesEvent aEvent) {
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GTBarrels.BARREL_BE.get(),
				(aBe, aSide) -> new BarrelFluidHandler(aBe, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GTBarrels.BARREL_PLASTIC_BE.get(),
				(aBe, aSide) -> new BarrelFluidHandler(aBe, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GTBarrels.BARREL_METAL_BE.get(),
				(aBe, aSide) -> new BarrelFluidHandler(aBe, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GTBarrels.BARREL_LOGISTICS_BE.get(),
				(aBe, aSide) -> new BarrelFluidHandler(aBe, aSide));
	}

// (The former id-lookup helper — BuiltInRegistries.BLOCK_ENTITY_TYPE.get + an unchecked
// cast witnessed by a class literal — is gone with the swap to holder references: each
// GT*_*_BE field is already typed DeferredHolder<BlockEntityType<?>, BlockEntityType<BE>>,
// so .get() hands over the exact type with no cast at all.)

	// -- the carrier seam: registerItem(FLUID_HANDLER_ITEM, provider, every GTBarrelBlockItem) --

	private static void registerBarrelItemHandlers(RegisterCapabilitiesEvent aEvent) {
		Item[] tBarrels = BuiltInRegistries.ITEM.stream()
				.filter(GTBarrelBlockItem.class::isInstance)
				.toArray(Item[]::new);
		if (tBarrels.length == 0) {
			throw new IllegalStateException("gt6 capability wiring: no GTBarrelBlockItem items present at RegisterCapabilitiesEvent — the barrel rows must fire before capability wiring");
		}
		aEvent.registerItem(Capabilities.FluidHandler.ITEM, (aStack, aContext) -> barrelFluidHandler(aStack), tBarrels);
	}

	// The exact construction the Forge initCapabilities performs (GTBarrelBlockItem, tasks
	// p12 + p13): the capacity AND the NBT_GASPROOF row flag ride the block carrier.
	private static GTBarrelItemFluidHandler barrelFluidHandler(ItemStack aStack) {
		GTBarrelBlock tBlock = (GTBarrelBlock) ((GTBarrelBlockItem) aStack.getItem()).getBlock();
		return new GTBarrelItemFluidHandler(aStack, tBlock.capacityL()).setGasProof(tBlock.gasProof());
	}

	// -- the p26 FE battery fixture (TAIL-APPENDED ROW, the shared serial file: the W1
	// base-machines card also touches this file — append-only) --
	// The fixture is a capability PROVIDER on this leg (21.1 BlockEntity has no
	// getCapability override): registerBlockEntity hands the BET the same
	// fresh-or-field provider shape as its siblings. The forge leg answers through the
	// GT6FeBatteryBlockEntity.getCapability override (the 01Root:439 shape) and cannot see
	// this file. Capabilities.EnergyStorage.BLOCK = BlockCapability<IEnergyStorage,
	// Direction> (javap 21.1.249); the receivers of this face are foreign FE consumers
	// (and the W3 dynamo-chain measurement end), which query the storage through the
	// LEVEL face.

	private static void registerFeBattery(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6FeBatteryBlockEntity> tBattery = GT6FeBatteries.FE_BATTERY_BE.get();
		aEvent.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, tBattery,
				(aBe, aSide) -> aBe.energyStorage());
	}

	// -- the p28 FE converter family (task p28-b-fe-converter-machine; TAIL-APPENDED ROW,
	// the shared serial file: append-only discipline) --
	// One shared BET over the ONE ULV block (the balance ruling: a single machine, no
	// ladder); the intake face serves on EVERY side (the
	// BE's declared all-sides simplification), so the provider ignores the side. The forge
	// leg answers through the GT6FeConverterBlockEntity.getCapability override and cannot
	// see this file.

	private static void registerFeConverters(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6FeConverterBlockEntity> tConverter = GT6FeConverters.FE_CONVERTER_BE.get();
		aEvent.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, tConverter,
				(aBe, aSide) -> aBe.energyStorage());
	}

	// -- the p28 FE source fixture (task p28-b-fe-converter-machine; TAIL-APPENDED ROW) --
	// The EXTRACTABLE twin of the sink battery above: the converter's pull face resolves
	// this storage through the level query exactly like any foreign FE source.

	private static void registerFeSource(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6FeSourceBlockEntity> tSource = GT6FeBatteries.FE_SOURCE_BE.get();
		aEvent.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, tSource,
				(aBe, aSide) -> aBe.energyStorage());
	}

	// -- the storage hopper family (p26-storage-hopper-family; TAIL-APPENDED ROW, the
	// shared serial file: append-only discipline) --
	// The two family BETs join as item-only faces (zero fluid tanks on the classes, the
	// oven/ACT shape). The forge leg answers through the GT6HopperBaseBlockEntity
	// getCapability override (fresh per-call SideItemHandler — the side view IS the
	// upstream getAccessibleSlotsFromSide2/canInsertItem2/canExtractItem2 triple); this
	// row is the 21.1 registration only. Without it every external hopper push (the
	// VanillaInventoryCodeHooks.insertHook level ItemHandler.BLOCK query) is
	// capability-blind on this node while the 1.20.1 BE override hides the gap — the
	// ADR-P15-4 census discipline, mechanized by GT6CapabilityWiringSeamTest.

	private static void registerHopperFamily(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6HopperBlockEntity> tHopper = GTBlockEntities.HOPPER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tHopper,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		BlockEntityType<GT6QueueHopperBlockEntity> tQueue = GTBlockEntities.QUEUE_HOPPER_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tQueue,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
	}

	// -- the static storage batch (p26-storage-static-batch; TAIL-APPENDED ROW, the
	// shared serial file: append-only discipline) --
	// Six family BETs join as item-only faces (zero fluid tanks on the classes — the
	// oven/ACT shape). The forge leg answers through the GT6StaticStorageBaseBlockEntity
	// getCapability override (fresh per-call side view — the side view IS the upstream
	// getAccessibleSlotsFromSide2/canInsertItem2/canExtractItem2 triple; the Safe answers
	// a 0-slot view, upstream :105 ZL_INTEGER). Without these rows every external hopper
	// push (the VanillaInventoryCodeHooks.insertHook level ItemHandler.BLOCK query) is
	// capability-blind on this node while the 1.20.1 BE override hides the gap — the
	// ADR-P15-4 census discipline, mechanized by GT6CapabilityWiringSeamTest.

	private static void registerStaticStorages(RegisterCapabilitiesEvent aEvent) {
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GTBlockEntities.LOCKER_BE.get(),
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GTBlockEntities.DRAWER_QUAD_BE.get(),
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GTBlockEntities.SAFE_BE.get(),
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GTBlockEntities.SAFE_KEYLOCKED_BE.get(),
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GTBlockEntities.BOOKSHELF_BE.get(),
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GTBlockEntities.BOTTLECRATE_BE.get(),
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
	}

	// -- the gas turbine (p29-w3-turbine-dynamo; TAIL-APPENDED ROW, the shared serial
	// file: append-only discipline) --
	// The Gas Turbine controller joins as a fluid-only face (the FM.Gas fill gate + the
	// three exhaust tanks; the Large Boiler face shape). The forge leg answers through the
	// GTGasTurbineBlockEntity getCapability override (fresh per-call GasFluidHandler — the
	// :141 fill containment + the :147 rotating exhaust drain); this row is the 21.1
	// registration only, delegating into the fluidCapability seam member. Without it every
	// external fluid push on the gas turbine is capability-blind on this node while the
	// 1.20.1 BE override hides the gap — the ADR-P15-4 census discipline, mechanized by
	// GT6CapabilityWiringSeamTest.

	private static void registerGasTurbine(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GTGasTurbineBlockEntity> tGasTurbine = GT6Turbines.GAS_TURBINE_BE.get();
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tGasTurbine,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	// -- the battery family (p29-w4-battery-storage; TAIL-APPENDED ROW, the shared serial
	// file: append-only discipline) --
	// The two BatteryBox BETs join as an ITEM-ONLY face (the canInsertItem2/upstream
	// :199 battery-slot access; the energy face is GT-native — the box answers
	// ITileEntityEnergy directly, so NO EnergyStorage capability row exists here: an FE
	// face on an EU box would BE the cut EU→FE outbound bridge / the un-ruled FE→EU
	// bypass, decisions.p28-cut-eu-fe-bridge + the p28 inbound-converter wall). The forge
	// leg answers through the TileEntityBase01Root.getCapability override over the
	// Root mInventory carrier; this row is the 21.1 registration only. Without it every
	// external battery push (the hopper/pipe level ItemHandler.BLOCK query) is
	// capability-blind on this node while the 1.20.1 BE override hides the gap — the
	// ADR-P15-4 census discipline, mechanized by GT6CapabilityWiringSeamTest.

	private static void registerBatteryBoxFamily(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6BatteryBoxBlockEntity> tBox = GT6Batteries.BATTERY_BOX_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tBox,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		BlockEntityType<GT6BatteryBoxBlockEntity> tLarge = GT6Batteries.BATTERY_BOX_LARGE_BE.get();
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLarge,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
	}

}
 *///?}
