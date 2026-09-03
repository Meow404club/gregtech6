//? if neoforge {
/*package gregtech6.registry;

import java.util.Objects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;
import gregtech6.tileentity.tank.GTBarrelItemFluidHandler;

// RegisterCapabilitiesEvent wiring (task p15-adapt-registry-core) — the 1.21.1 leg of the two
// capability seams W3 left open:
// · machines (p15-fork-capability-machines): the six forked BE classes expose a plain
//   getCapability(BlockCapability, Direction) seam method (no @Override — 21.1 deleted
//   BlockEntity#getCapability); this class hands each BlockEntityType to the event with a
//   provider delegating into that seam, per the handoff shape
//   registerBlockEntity(cap, beType, (be, side) -> be.getCapability(cap, side)).
//   Capability coverage mirrors each forge getCapability exactly: shredder/crusher/lathe
//   serve item + fluid, the oven serves item only, boiler tank/steam engine/large
//   boiler/fluid pipe serve fluid only.
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
// Why BuiltInRegistries lookups instead of holder fields: the registry homes
// (GTMachines/GTBlockEntities/GTMultiBlocks/GTFluidPipes/GTBarrels) still sit on the 21.1
// unresolved RegistryObject type on this leg — referencing their fields from here would
// cascade unresolved-type errors into this file (the type is the error, not the field).
// Switch to DeferredHolder references when the registry-blocks cards fork those homes. The
// eight BET paths are pinned against the registry rows by GT6CapabilityWiringSeamTest
// (RegistryObject.getId() reads the construction-time name, no binding needed). Every lookup
// fails fast: a missing row must never surface as a silently capability-less block or item.
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
		registerBarrelItemHandlers(aEvent);
	}

	// -- the machines seam: registerBlockEntity(cap, beType, (be, side) -> be.getCapability(cap, side)) --

	private static void registerMachineBlockEntities(RegisterCapabilitiesEvent aEvent) {
		// the p7 shredder/crusher/lathe ladder — one BE class, three BETs, item + fluid faces
		BlockEntityType<TileEntityBasicMachine> tShredder = type(TileEntityBasicMachine.class, "shredder");
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tShredder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tShredder,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tCrusher = type(TileEntityBasicMachine.class, "crusher");
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tCrusher,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tCrusher,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityBasicMachine> tLathe = type(TileEntityBasicMachine.class, "lathe");
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tLathe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLathe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		// the p4 oven — its forge getCapability serves the gated item handler alone
		BlockEntityType<TileEntityOven> tOven = type(TileEntityOven.class, "oven");
		aEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, tOven,
				(aBe, aSide) -> aBe.getCapability(Capabilities.ItemHandler.BLOCK, aSide));
		// the fluid-only faces (p13 boiler family, p12 steam engine, p8 large boiler, p4 pipe)
		BlockEntityType<GTBoilerTankBlockEntity> tBoilerTank = type(GTBoilerTankBlockEntity.class, "boiler_tank");
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tBoilerTank,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<GTSteamEngineBlockEntity> tEngine = type(GTSteamEngineBlockEntity.class, "steam_engine");
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tEngine,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<TileEntityLargeBoiler> tLargeBoiler = type(TileEntityLargeBoiler.class, "multiblock_large_boiler");
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tLargeBoiler,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
		BlockEntityType<GTFluidPipeBlockEntity> tPipe = type(GTFluidPipeBlockEntity.class, "fluid_pipe");
		aEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tPipe,
				(aBe, aSide) -> aBe.getCapability(Capabilities.FluidHandler.BLOCK, aSide));
	}

	// The unchecked cast is the BET's own class invariant: the factory in the registry row
	// constructs exactly this BE class (GTMachines.SHREDDER_BE, GTBlockEntities.STEAM_ENGINE_BE,
	// GTMultiBlocks.LARGE_BOILER_BE, GTFluidPipes.FLUID_PIPE_BE — all typed to the same class).
	// The class literal is only the call-site type witness for the inference.
	private static <BE extends BlockEntity> BlockEntityType<BE> type(Class<? extends BlockEntity> aWitness, String aPath) {
		BlockEntityType<?> tType = Objects.requireNonNull(
				BuiltInRegistries.BLOCK_ENTITY_TYPE.get(gtId(aPath)),
				"gt6 capability wiring: BlockEntityType gt6:" + aPath + " not present at RegisterCapabilitiesEvent — registry rows must fire before capability wiring");
		@SuppressWarnings("unchecked")
		BlockEntityType<BE> rType = (BlockEntityType<BE>) tType;
		return rType;
	}

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

	private static ResourceLocation gtId(String aPath) {
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
	}
}
 *///?}
