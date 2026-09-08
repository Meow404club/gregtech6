//? if forge {
/*package gregtech6.registry;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;

// The 1.20.1 forge leg of the EU->FE outbound bridge (task p26-eu-bridge-outbound, design =
// research.p26-r-eu-bridge). Installs the root EnergyBridge handler at mod construct:
// whenever a GT emitter (generator, wire, machine) pushes an EU packet train at a NON-GT
// receiver, the handler resolves the receiver's forge FE storage and bills the packet math
// through EnergyBridge.insertFe (4 FE per 1 EU, packet-aligned) — the upstream EnergyCompat
// RF branch (EnergyCompat.java:210-216) with isElectricRFReceiver's class-name whitelist
// replaced by capability presence (the modern whitelist; GT_API.java:505's EMIT_EU_AS_RF
// stays F, see EnergyBridge.EMIT_EU_AS_RF).
//
// Capability note (the SPEC "dual token" trap, resolved by platform evidence): the GTCEu
// issue #4286 "ForgeCapabilities.ENERGY vs CapabilityEnergy.ENERGY split" describes the
// 1.16-era mechanics, where CapabilityEnergy was its own CapabilityManager key. On
// forge-1.20.1 (47.4.10) the CapabilityEnergy class no longer exists (universal + sources
// jar both verified — net/minecraftforge/energy/ ships only IEnergyStorage/EnergyStorage/
// EmptyEnergyStorage) and CapabilityManager.get keys providers by the interned INTERFACE
// NAME string (CapabilityManager.java:31-38, computeIfAbsent over the transformer-extracted
// generic), so every IEnergyStorage token resolves to the SAME Capability instance.
// Titanium's own 1.20 branch answers exactly ForgeCapabilities.ENERGY (PoweredTile.java:81,
// GitHub raw fetch 2026-09-08). One token here therefore covers the whole FE ecosystem on
// this leg; the 21.1 leg has a single BlockCapability and never had the question.
//
// Self-contained mod-bus listener (ADR-P3-4, the GTEnergySources construct-event shape);
// GT6Mod / GTModBusListener stay untouched. checkOverCharge (EnergyCompat.java:129-137) is
// deliberately absent — the declared deviation, see the EnergyBridge ledger note.
//
// Block-comment-free body: the whole file lives inside one wrapper comment, hence //
// comments only (the ADR-P15-3 r1 implementation rule, GT6CapabilityWiring precedent).
// Deliberately a 1.20.1-only file: the 1.21.1 leg (GT6EuToFeBridgeNeoforge) queries the
// level BlockCapability instead — BlockEntity has no getCapability to call there.

@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6EuToFeBridgeForge {

	private GT6EuToFeBridgeForge() {
	}

	// The construct event is the first mod-bus stage (the GTEnergySources doc) — early
	// enough that no server tick can emit before the bridge is armed.
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		EnergyBridge.register(GT6EuToFeBridgeForge::onForeignEnergy);
	}

	// The handler: EU type only (upstream :145 branches everything under the type check),
	// BE receivers only (the modern TileEntity), then the capability whitelist.
	static long onForeignEnergy(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, Object aReceiver) {
		if (aEnergyType != TD.Energy.EU) return 0;
		if (!(aReceiver instanceof BlockEntity tReceiver) || !tReceiver.hasLevel()) return 0;
		// aSide 0-5 = the vanilla Directions (the ITileEntityEnergy doc); 6 = no specific
		// side, queried side-less
		Direction tDirection = aSide >= 0 && aSide <= 5 ? Direction.from3DDataValue(aSide) : null;
		// the modern whitelist probe: capability presence IS the RF-machine test
		// (upstream isElectricRFReceiver, EnergyCompat.java:89-97, modernized)
		IEnergyStorage tStorage = tReceiver.getCapability(ForgeCapabilities.ENERGY, tDirection).orElse(null);
		if (tStorage == null || !tStorage.canReceive()) return 0;
		if (!EnergyBridge.gateFE(true)) return 0;
		return EnergyBridge.insertFe(tStorage::receiveEnergy, aSize, aAmount);
	}
}
*///?}
