package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

//? if forge {
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
//?} else {
/*import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
 *///?}

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;

import gregtech6.block.energy.GT6FeBatteryBlock;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity;

/**
 * The FE battery test fixture registration, card-owned (ADR-P3-4): a self-contained
 * {@code @EventBusSubscriber(MOD)} triple DeferredRegister attached from the construct
 * event — the GTEnergySources shape verbatim (task p8-d4), including the BLOCK-before-BET
 * registration order (vanilla registry order holds across DeferredRegisters, the
 * GTBlockEntities doc). GT6Mod / GTModBusListener stay untouched (the frozen P2 form).
 *
 * <p>This battery is the RECEIVING end of the p26 EU->FE outbound bridge acceptance chain:
 * a plain platform {@code EnergyStorage} reference implementation (capacity = maxReceive,
 * maxExtract = 0 — a pure sink, the bridge is outbound-only) behind a simple-cube block.
 * The RCON chain drives it headless (/gt6febattery place|stat|reset); no creative tab.
 *
 * <p>The texture is borrowed from the energy_source rig placeholder (the same
 * placeholder-borrow posture as the p8 source rig, assets/README.md attribution row).
 *
 * <h2>The EU->FE outbound bridge arm lives here (task p26-eu-bridge-outbound)</h2>
 *
 * This class also arms the root {@link EnergyBridge} handler ({@code onForeignEnergy},
 * registered from {@link #onModConstruct}) — the per-leg capability query differs, so the
 * arm is a {@code //?} else-hunk pair, NOT a per-leg file. The reason is a stonecutter
 * wiring fact the former standalone GT6EuToFeBridgeForge.java learned the hard way: the
 * ACTIVE node (1.20.1-forge) compiles the shared source RAW — {@code configureSource}
 * hands the active node {@code mdk/src} directly with no processing task
 * (StonecutterBuildTasksImpl.kt:57-66, in the stonecutter-src-07 harvest) — so the
 * full-file wrapper-comment form (a condition marker opening the file and the body held
 * inside a block comment) left the whole former file inside a
 * block comment: javac compiled an EMPTY unit, no class, no annotation-scan target, and
 * the bridge never armed (the first RCON pass: three phases, stored 0, zero server
 * errors). Internal hunks are the only forge-side leg fork that survives the raw compile,
 * which is why every other dual-leg file in mdk/src forks this way. The former
 * GT6EuToFeBridgeNeoforge.java folded in here too (symmetry, one wiring point): on the
 * 21.1 node the shared source IS processed (the generated tree), so both hunks resolve to
 * exactly the same arms the standalone files compiled before.
 *
 * <p>Query-face ledger (the SPEC "dual token" trap, resolved by platform evidence): the
 * GTCEu issue #4286 "ForgeCapabilities.ENERGY vs CapabilityEnergy.ENERGY split" describes
 * the 1.16-era mechanics, where CapabilityEnergy was its own CapabilityManager key. On
 * forge-1.20.1 (47.4.10) the CapabilityEnergy class no longer exists (universal + sources
 * jar both verified — net/minecraftforge/energy/ ships only IEnergyStorage/
 * EnergyStorage/EmptyEnergyStorage) and CapabilityManager.get keys providers by the
 * interned INTERFACE name (CapabilityManager.java:31-38, computeIfAbsent over the
 * transformer-extracted generic), so every IEnergyStorage token resolves to the SAME
 * Capability instance. Titanium's own 1.20 branch answers exactly ForgeCapabilities.ENERGY
 * (PoweredTile.java:81). One token therefore covers the whole FE ecosystem on this leg;
 * the 21.1 leg has a single BlockCapability and never had the question.
 *
 * <p>The handler body: EU type first (upstream EnergyCompat.java:145 branches everything
 * under the type check), BlockEntity receivers only, then the capability-presence
 * whitelist (the modern isElectricRFReceiver, EnergyCompat.java:89-97, modernized), then
 * the root packet math {@link EnergyBridge#insertFe} (4 FE per 1 EU, packet-aligned).
 * checkOverCharge (EnergyCompat.java:129-137) is deliberately absent — the declared
 * deviation, see the EnergyBridge ledger note.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FeBatteries {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** The FE sink fixture block (the p26 outbound bridge acceptance receiver). */
	public static final RegistryObject<GT6FeBatteryBlock> FE_BATTERY = BLOCKS.register("fe_battery",
			() -> new GT6FeBatteryBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	public static final RegistryObject<Item> FE_BATTERY_ITEM = ITEMS.register("fe_battery",
			() -> new BlockItem(FE_BATTERY.get(), new Item.Properties()));

	/** BET registers after BLOCK (vanilla registry order) so the validBlocks supplier resolves. */
	public static final RegistryObject<BlockEntityType<GT6FeBatteryBlockEntity>> FE_BATTERY_BE =
			BLOCK_ENTITY_TYPES.register("fe_battery", () -> BlockEntityType.Builder.of(
					GT6FeBatteryBlockEntity::new, FE_BATTERY.get()).build(null));

	private GT6FeBatteries() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (the GTEnergySources fork precedent).
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		// the EU->FE outbound bridge arm — armed at the same construct stage the standalone
		// handler classes used, from the listener the runtime proves fires on both legs
		// (the fixture blocks below register from it). See the class doc for the wiring.
		EnergyBridge.register(GT6FeBatteries::onForeignEnergy);
		// the theoretical connect probe for the conductor handshakes (GTWireBlockEntity
		// .canConnect -> EnergyBridge.bridgesForeign): same query minus the insert.
		EnergyBridge.registerForeignConnectProbe(GT6FeBatteries::isForeignFeReceiver);
	}

	// ---------------------------------------------------------------------------
	// the EU->FE outbound bridge arm (the per-leg capability query; the shared contract
	// is EnergyBridge.IEnergyBridgeHandler). Body comments stay // — the hunk pair already
	// forks the file, and the forge arm must compile RAW on the active node (the class doc).
	// ---------------------------------------------------------------------------

	//? if forge {
	// The 1.20.1 face: the receiver BE answers the token through its getCapability override
	// (the TileEntityBase01Root.java:439 shape; IEnergyStorage carries
	// @AutoRegisterCapability, forge-1.20.1 IEnergyStorage.java:19, so no registration
	// event exists on this leg).
	static long onForeignEnergy(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
		if (aEnergyType != TD.Energy.EU) return 0;
		if (!(aReceiver instanceof BlockEntity tReceiver) || !tReceiver.hasLevel()) return 0;
		// aSide 0-5 = the vanilla Directions (the ITileEntityEnergy doc); 6 = no specific
		// side, queried side-less
		Direction tDirection = aSide >= 0 && aSide <= 5 ? Direction.from3DDataValue(aSide) : null;
		// the modern whitelist probe: capability presence IS the RF-machine test
		IEnergyStorage tStorage = tReceiver.getCapability(ForgeCapabilities.ENERGY, tDirection).orElse(null);
		if (tStorage == null || !tStorage.canReceive()) return 0;
		if (!EnergyBridge.gateFE(true)) return 0;
		return EnergyBridge.insertFe(tStorage::receiveEnergy, aSize, aAmount);
	}

	// The theoretical probe behind EnergyBridge.bridgesForeign — side-less (the handshake
	// asks whether the receiver is bridgeable AT ALL; the packet faces are resolved later).
	static boolean isForeignFeReceiver(Object aReceiver) {
		if (!(aReceiver instanceof BlockEntity tReceiver) || !tReceiver.hasLevel()) return false;
		IEnergyStorage tStorage = tReceiver.getCapability(ForgeCapabilities.ENERGY, null).orElse(null);
		return tStorage != null && tStorage.canReceive();
	}
	//?} else {
	/*// The 1.21.1 face: 21.1 deleted BlockEntity#getCapability (21.1.249 javap) — the query
	// rides the LEVEL BlockCapability (Capabilities.EnergyStorage.BLOCK =
	// BlockCapability<IEnergyStorage, Direction>, javap 21.1.249; the in-repo
	// GTBoilerTankBlockEntity:583 precedent). No RegisterCapabilitiesEvent here: this arm
	// is the QUERY side; the fixture's provider row lives in GT6CapabilityWiring.
	static long onForeignEnergy(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
		if (aEnergyType != TD.Energy.EU) return 0;
		if (!(aReceiver instanceof BlockEntity tReceiver) || !tReceiver.hasLevel()) return 0;
		Direction tDirection = aSide >= 0 && aSide <= 5 ? Direction.from3DDataValue(aSide) : null;
		IEnergyStorage tStorage = tReceiver.getLevel().getCapability(
				Capabilities.EnergyStorage.BLOCK, tReceiver.getBlockPos(), tDirection);
		if (tStorage == null || !tStorage.canReceive()) return 0;
		if (!EnergyBridge.gateFE(true)) return 0;
		return EnergyBridge.insertFe(tStorage::receiveEnergy, aSize, aAmount);
	}

	// The theoretical probe behind EnergyBridge.bridgesForeign — side-less (the handshake
	// asks whether the receiver is bridgeable AT ALL; the packet faces are resolved later).
	static boolean isForeignFeReceiver(Object aReceiver) {
		if (!(aReceiver instanceof BlockEntity tReceiver) || !tReceiver.hasLevel()) return false;
		IEnergyStorage tStorage = tReceiver.getLevel().getCapability(
				Capabilities.EnergyStorage.BLOCK, tReceiver.getBlockPos(), null);
		return tStorage != null && tStorage.canReceive();
	}
	 *///?}
}
