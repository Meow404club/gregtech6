package gregtech6;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//? if forge {
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/*// 21.1: FMLJavaModLoadingContext no longer exists. The mod event bus arrives through
// constructor injection instead — FMLModContainer.constructMod picks the public constructor
// and injects IEventBus / ModContainer / Dist parameters (loader-4.0.44 bytecode,
// getConstructors() + Map.of(IEventBus, ModContainer, FMLModContainer, Dist)).
 *///?}
import net.minecraftforge.fml.loading.FMLEnvironment;

import gregapi.oredict.MaterialGraph;
import gregtech6.client.GTClientHandlers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTModBusListener;

/**
 * Mod entry: the three-segment registration bridge (ADR-P2-2).
 * <ol>
 * <li>{@code FMLConstructModEvent.enqueueWork} -> {@code MT.init()} + {@code OP.init()}
 * (MaterialRegistry open -> closed; MT.init() is a per-generation full refill, wired once);</li>
 * <li>{@code RegisterEvent} at {@code EventPriority.LOW} (GTCEu GTRegistrate.java:148-151
 * precedent) -> MaterialRegistry x whitelist-prefix items plus one creative tab;</li>
 * <li>{@code FMLCommonSetupEvent.enqueueWork} -> {@code MaterialGraph.applyCrucibleAlloyReferences()}
 * (GT6 postInit equivalent, GT_API_Post.java:816-820), then the RegisterEvent listener is
 * unregistered (GTRegistrate.java:156-159 precedent).</li>
 * </ol>
 */
@Mod("gt6")
public class GT6Mod {

    public static final Logger LOGGER = LogManager.getLogger("gt6");

    private final IEventBus modBus;
    /** Registered by instance so it can be unregistered again after CommonSetup (eventbus-6.0.5 keys object registrations by instance). */
    private final GTModBusListener modBusListener = new GTModBusListener();

    //? if forge {
    public GT6Mod() {
        // 1.20.1's only way to obtain the mod bus: FMLJavaModLoadingContext.get().getModEventBus()
        // (constructor-injected IEventBus only exists in 20.2+).
        modBus = FMLJavaModLoadingContext.get().getModEventBus();
    //?} else {
    /*public GT6Mod(IEventBus aModBus) {
        // 21.1 constructor injection — the P15 wiring hang point: this is the bus every
        // DeferredRegister, the RegisterEvent bridge and the RegisterCapabilitiesEvent
        // listener (GT6CapabilityWiring) ride. One-injection-point deviation from the 1.20.1
        // no-arg form; the three-segment bridge below is untouched.
        modBus = aModBus;
     *///?}
        Objects.requireNonNull(modBus, "mod event bus must be available at construct time");
        modBus.register(modBusListener);
        modBus.addListener(this::onCommonSetup);
        // client-only event listening (RegisterColorHandlersEvent.Item fires on the client only); keep the side isolation inside the @OnlyIn class
        if (FMLEnvironment.dist == Dist.CLIENT) GTClientHandlers.init(modBus);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int tReferences = MaterialGraph.applyCrucibleAlloyReferences();
            GT6Mod.LOGGER.info("GT6 common setup: applyCrucibleAlloyReferences completed ({} alloy reverse references)", tReferences);
        });
        // unregister the RegisterEvent listener right after use (precedent: GTCEu GTRegistrate.java:156-159)
        modBus.unregister(modBusListener);
    }
}
