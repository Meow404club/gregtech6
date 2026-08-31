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
 * <p>Client data this phase: item models (GT6ItemModels) + en_us lang (GT6EnUs), joined by
 * blockstates/block models (GT6BlockStates) with the p3-example-machine chest. Server providers:
 * the material prefix blocks' loot tables (GT6LootTables, task p8-prefixblock-render); recipes
 * and tags are still later phases.
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
            // task p3-example-machine: first blockstate/block model provider (chest)
            event.getGenerator().addProvider(true,
                new GT6BlockStates(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            // task p4-cover-core (W3 explicit order 3): the cover plate sprite joins the block atlas
            event.getGenerator().addProvider(true,
                new GT6Atlases(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
        }
        // task p8-prefixblock-render ④: the material prefix blocks' self-drop loot tables (server data)
        event.getGenerator().addProvider(true,
            new GT6LootTables(event.getGenerator().getPackOutput()));
    }
}
