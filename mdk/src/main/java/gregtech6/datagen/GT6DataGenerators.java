package gregtech6.datagen;

import java.util.Set;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

//? if forge {
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
//?} else {
/*import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
//21.1: same simple name, neoforge.common.data package (the GT6WorldgenDatagen javadoc
//carries the seam evidence; both legs share the 4-arg ctor shape).
*///?}

/**
 * DataGen entry (ADR-P2-4). Annotation-based mod-bus listener (GTCEu data/DataGenerators.java:25-31
 * precedent) so the datagen wiring needs no change in the mod constructor; the event is the
 * same-named GatherDataEvent on both legs (Forge 1.20.1: net.minecraftforge.data.event;
 * NeoForge 21.1: net.neoforged.neoforge.data.event — the import shift is mechanical) and
 * neither has Client/Server inner sub-events — providers are gated with
 * {@code event.includeClient()} only.
 *
 * <p>Client data this phase: item models (GT6ItemModels) + en_us lang (GT6EnUs) + zh_cn lang
 * (GT6ZhCn, task p20-i18n-zhcn-provider), joined by
 * blockstates/block models (GT6BlockStates) with the p3-example-machine chest. Server providers:
 * the material prefix blocks' loot tables (GT6LootTables, task p8-prefixblock-render) and, since
 * task p24-tool-system, the first tags provider (GT6ItemTags — registered BEFORE the recipes so
 * the tag band keeps precedence as the recipe band grows) and the first recipe provider
 * (GT6CraftingRecipes). The registry lookup ({@code event.getLookupProvider()},
 * present on both legs) is handed to the 1.21-shaped providers (GT6LootTables/GT6Atlases);
 * the 1.20.1 LootTableProvider/SpriteSourceProvider signatures simply ignore it — see those
 * classes for the leg-specific {@code super} wiring.
 */
@Mod.EventBusSubscriber(modid = GT6DataGenerators.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6DataGenerators {

    public static final String MOD_ID = "gt6";

    private GT6DataGenerators() {
    }

    //? if forge {
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    //?} else {
    /*@SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    *///?}
    public static void onGatherData(GatherDataEvent event) {
        if (event.includeClient()) {
            event.getGenerator().addProvider(true,
                new GT6ItemModels(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            event.getGenerator().addProvider(true,
                new GT6EnUs(event.getGenerator().getPackOutput()));
            // task p20-i18n-zhcn-provider: the zh_cn companion walks the same small-unit faces,
            // values joined from the committed zh_cn_ref.tsv (ADR 2026-09-06-p20-i18n-zhcn-pipeline)
            event.getGenerator().addProvider(true,
                new GT6ZhCn(event.getGenerator().getPackOutput()));
            // task p3-example-machine: first blockstate/block model provider (chest)
            event.getGenerator().addProvider(true,
                new GT6BlockStates(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            // task p4-cover-core (W3 explicit order 3): the cover plate sprite joins the block atlas
            event.getGenerator().addProvider(true,
                new GT6Atlases(event.getGenerator().getPackOutput(), event.getLookupProvider(),
                    event.getExistingFileHelper()));
        }
        // task p8-prefixblock-render ④: the material prefix blocks' self-drop loot tables (server data)
        event.getGenerator().addProvider(true,
            new GT6LootTables(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        // task p24-tool-system ④: the first tags provider — BEFORE the recipes so the tag
        // band keeps precedence (the takeover card appends its bands inside GT6ItemTags)
        event.getGenerator().addProvider(true,
            new GT6ItemTags(event.getGenerator().getPackOutput(), event.getLookupProvider(),
                event.getExistingFileHelper()));
        // task p24-tags-provider-skeleton ④: the block tags provider — the mining-tool bands
        // (pickaxe over the stone/machine/prefix-block universe, axe over the wood barrel).
        // The four-argument BlockTagsProvider ctor is shape-identical on both legs (forge
        // BlockTagsProvider.java:19 / NeoForge 21.1 :18 — the research card's "3-param Neo"
        // form is void), so only the import forks inside GT6BlockTags; the item provider
        // above needs no contentsGetter() wiring (its first-batch members are plain
        // registry elements, no block-tag copying).
        event.getGenerator().addProvider(true,
            new GT6BlockTags(event.getGenerator().getPackOutput(), event.getLookupProvider(),
                event.getExistingFileHelper()));
        // task p24-tool-system ③: the first recipe provider — the empty spray can crafting
        // (both legs construct through the two-arg form; the forge leg ignores the lookup)
        event.getGenerator().addProvider(true,
            new GT6CraftingRecipes(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        // task p26-worldgen-pipeline-skeleton: the first dynamic-registry provider — the 17
        // stone blobs' configured/placed features + biome modifiers off ONE RegistrySetBuilder
        // (three BootstapContexts, GT6WorldgenDatagen.BUILDER; GTCEu DataGenerators.java:40
        // precedent). Biome-modifier JSONs land per leg at data/gt6/<loader>/biome_modifier/
        // (the registry-key namespace drives the directory — no local wiring here).
        event.getGenerator().addProvider(true,
            new DatapackBuiltinEntriesProvider(event.getGenerator().getPackOutput(),
                event.getLookupProvider(), GT6WorldgenDatagen.BUILDER, Set.of("gt6")));
        // task p26-w1-press-extruder-molds: the 1.21 singular-registry aliases — MUST stay
        // LAST (the sequential per-provider join order is the contract: the mirror walks the
        // earlier providers' on-disk output; see GT6DualDirectoryFaces)
        event.getGenerator().addProvider(true,
            new GT6DualDirectoryFaces(event.getGenerator().getPackOutput()));
    }
}
