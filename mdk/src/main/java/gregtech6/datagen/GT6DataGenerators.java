package gregtech6.datagen;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * DataGen entry (ADR-P2-4). Annotation-based mod-bus listener (GTCEu data/DataGenerators.java:25-31
 * precedent) so the datagen wiring needs no change in the mod constructor; 1.20.1 GatherDataEvent
 * lives in net.minecraftforge.data.event and has no Client/Server inner sub-events — providers are
 * gated with {@code event.includeClient()} only (GatherDataEvent.java:28).
 *
 * <p>Client data this phase: item models (GT6ItemModels) + en_us lang (GT6EnUs). Server providers
 * (recipes/tags/loot) are later phases; blockstates do not exist yet.
 */
@Mod.EventBusSubscriber(modid = GT6DataGenerators.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6DataGenerators {

    public static final String MOD_ID = "gt6";

    private GT6DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        if (event.includeClient()) {
            event.getGenerator().addProvider(true,
                new GT6ItemModels(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            event.getGenerator().addProvider(true,
                new GT6EnUs(event.getGenerator().getPackOutput()));
        }
    }
}
