package gregtech6.registry;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Mod-bus listener object, registered by instance in the {@code @Mod} constructor so that
 * {@code IEventBus.unregister(Object)} can remove it again after CommonSetup (EventBus keys
 * instance registrations by the object itself, unlike consumer registrations which are keyed by
 * {@code consumer.getClass()} — verified against eventbus-6.0.5 bytecode).
 */
public final class GTModBusListener {

    /** Segment 1: material flood runs during CONSTRUCT, strictly before any RegisterEvent (ModLoadingStage.java:20-55 order). */
    @SubscribeEvent
    public void onConstructMod(FMLConstructModEvent event) {
        event.enqueueWork(GTMaterialItems::initMaterials); // ParallelDispatchEvent.enqueueWork, FMLConstructModEvent.java:11
    }

    /** Segment 2: dynamic item + creative tab injection at LOW priority (GTCEu GTRegistrate.java:148-151 precedent). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRegister(RegisterEvent event) {
        GTMaterialItems.onRegister(event);
    }
}
