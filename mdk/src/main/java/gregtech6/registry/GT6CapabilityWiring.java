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
import gregtech6.item.GTBarrelBlockItem;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity; // p26 tail-append
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;
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
		registerBarrelBlockFluidHandler(aEvent);
		registerBarrelItemHandlers(aEvent);
		registerFeBattery(aEvent); // task p26-eu-bridge-outbound (tail-append; shared serial file)
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

	// -- the p26 FE battery fixture (task p26-eu-bridge-outbound; TAIL-APPENDED ROW, the
	// shared serial file: the W1 base-machines card also touches this file — append-only) --
	// The fixture is a capability PROVIDER on this leg (21.1 BlockEntity has no
	// getCapability override): registerBlockEntity hands the BET the same
	// fresh-or-field provider shape as its siblings. The forge leg answers through the
	// GT6FeBatteryBlockEntity.getCapability override (the 01Root:439 shape) and cannot see
	// this file. Capabilities.EnergyStorage.BLOCK = BlockCapability<IEnergyStorage,
	// Direction> (javap 21.1.249); the receiver of this face is the EU->FE outbound bridge
	// (the EU->FE bridge's neo arm in GT6FeBatteries.onForeignEnergy), which queries the
	// storage through the LEVEL face exactly like any foreign FE consumer would.

	private static void registerFeBattery(RegisterCapabilitiesEvent aEvent) {
		BlockEntityType<GT6FeBatteryBlockEntity> tBattery = GT6FeBatteries.FE_BATTERY_BE.get();
		aEvent.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, tBattery,
				(aBe, aSide) -> aBe.energyStorage());
	}

}
 *///?}
