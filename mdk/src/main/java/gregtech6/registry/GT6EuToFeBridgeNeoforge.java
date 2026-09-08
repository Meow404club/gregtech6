//? if neoforge {
/*package gregtech6.registry;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;

// The 1.21.1 neoforge leg of the EU->FE outbound bridge (task p26-eu-bridge-outbound,
// design = research.p26-r-eu-bridge) — the GT6EuToFeBridgeForge javadoc applies in full;
// this file differs ONLY in the capability query face, because 21.1 deleted
// BlockEntity#getCapability (21.1.249 javap: zero capability methods) and replaced it with
// the level-level BlockCapability query (Capabilities.EnergyStorage.BLOCK =
// BlockCapability<IEnergyStorage, Direction>, javap-verified on 21.1.249; the
// GTBoilerTankBlockEntity getLevel().getCapability(...) in-repo precedent, :583).
//
// No RegisterCapabilitiesEvent here: this bridge is the QUERY side — it reads whatever FE
// face foreign BlockEntityTypes registered with the platform, it does not expose one. The
// registration event row this card touches (registerFeBattery) is for the GT6FeBatteries
// fixture, which is a capability PROVIDER on this leg.
//
// Self-contained @EventBusSubscriber (the GT6CapabilityWiring shape: no bus attribute, 21.1
// routes IModBusEvent vs game events by event type). Block-comment-free body: the whole
// file lives inside one wrapper comment, hence // comments only (ADR-P15-3 r1 rule).

@EventBusSubscriber(modid = "gt6")
public final class GT6EuToFeBridgeNeoforge {

	private GT6EuToFeBridgeNeoforge() {
	}

	// The construct event is the first mod-bus lifecycle stage — early enough that no
	// server tick can emit before the bridge is armed.
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		EnergyBridge.register(GT6EuToFeBridgeNeoforge::onForeignEnergy);
	}

	// The handler: EU type only (upstream :145 branches everything under the type check),
	// BE receivers only (the modern TileEntity), then the capability whitelist.
	static long onForeignEnergy(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
		if (aEnergyType != TD.Energy.EU) return 0;
		if (!(aReceiver instanceof BlockEntity tReceiver) || !tReceiver.hasLevel()) return 0;
		// aSide 0-5 = the vanilla Directions (the ITileEntityEnergy doc); 6 = no specific
		// side, queried side-less (the BlockCapability context is @Nullable Direction)
		Direction tDirection = aSide >= 0 && aSide <= 5 ? Direction.from3DDataValue(aSide) : null;
		// the modern whitelist probe: capability presence IS the RF-machine test
		// (upstream isElectricRFReceiver, EnergyCompat.java:89-97, modernized)
		IEnergyStorage tStorage = tReceiver.getLevel().getCapability(
				Capabilities.EnergyStorage.BLOCK, tReceiver.getBlockPos(), tDirection);
		if (tStorage == null || !tStorage.canReceive()) return 0;
		if (!EnergyBridge.gateFE(true)) return 0;
		return EnergyBridge.insertFe(tStorage::receiveEnergy, aSize, aAmount);
	}
}
*///?}
