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
 * <p>Client data this phase: item models (GT6ItemModels) + en_us lang + zh_cn lang
 * (GT6ZhCn, task p20-i18n-zhcn-provider), joined by
 * blockstates/block models (GT6BlockStates) with the p3-example-machine chest. The en_us
 * writer is the SINGLE registered {@link GT6MoldDatagen.Lang} — the chain tail
 * GT6EnUs ← GT6CrucibleDatagen.Lang ← GT6MoldDatagen.Lang replays base ⊕ crucible ⊕ mold
 * through the super calls (task p30-ops-datagen-lang-order: registering more than one
 * full-file lang writer made the winner an annotation-scan lottery). Server providers:
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
            // task p30-ops-datagen-lang-order: the ONE en_us writer — the mold-chain tail
            // (GT6MoldDatagen.Lang → GT6CrucibleDatagen.Lang → GT6EnUs) replays the full table
            // through its super calls, so base + the 4 crucible keys + the 66 mold keys land in
            // one deterministic write. The two chained providers are never registered
            // themselves; a second registered writer would re-open the last-writer lottery the
            // three-subscriber form suffered (an annotation-scan order a recompile flips).
            event.getGenerator().addProvider(true,
                new GT6MoldDatagen.Lang(event.getGenerator().getPackOutput()));
            // task p20-i18n-zhcn-provider: the zh_cn companion walks the same small-unit faces,
            // values joined from the committed zh_cn_ref.tsv (ADR 2026-09-06-p20-i18n-zhcn-pipeline)
            event.getGenerator().addProvider(true,
                new GT6ZhCn(event.getGenerator().getPackOutput()));
            // task p3-example-machine: first blockstate/block model provider (chest)
            event.getGenerator().addProvider(true,
                new GT6BlockStates(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            // task p30-ore-3-datagen: the ore universe's shared-placeholder blockstates + item
            // models (3922 + 3922 + 28 shared; the dual-sprite render face is the baked model)
            event.getGenerator().addProvider(true,
                new GT6OreBlockStates(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
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
        // task p30-w6-t1-trees-nine ③: the biome tags provider — the #gt6:trees/<snake>
        // bands the tree biome modifiers reference (the registry-key dir
        // tags/worldgen/biome is IDENTICAL on both legs, no mirror row and no
        // datagen_tree_check SEGMENT_MAP entry).
        // task p30-w6-rocks-sticks: the biome tag band — the surface_rocks + sticks_*
        // groups the surface biome modifiers hang off (the BiomeTagsProvider base; the
        // tags are NOT loader-branded, one band serves both legs).
        event.getGenerator().addProvider(true,
            new GT6BiomeTags(event.getGenerator().getPackOutput(), event.getLookupProvider(),
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
        // task p30-ops-datagen-lang-order: the crucible/mold provider bands — appended HERE so
        // exactly ONE GatherDataEvent listener registers every provider (the pre-p30
        // three-subscriber form ordered itself by the annotation-scan lottery, and its split
        // registration could even land the lang writers after this listener). Their crafting
        // faces MUST precede the mirror below: GT6DualDirectoryFaces walks the on-disk plural
        // face (recipes/), so in-run fresh output replaces the stale-seeding the split form
        // relied on.
        GT6CrucibleDatagen.appendProviders(event);
        GT6MoldDatagen.appendProviders(event);
        // task p26-w1-press-extruder-molds: the 1.21 singular-registry aliases — MUST stay
        // LAST (the sequential per-provider join order is the contract: the mirror walks the
        // earlier providers' on-disk output; see GT6DualDirectoryFaces)
        event.getGenerator().addProvider(true,
            new GT6DualDirectoryFaces(event.getGenerator().getPackOutput()));
        // task p29-w5-t1-dig-six ①: the drop-conversion loot seam — one GLM JSON per
        // converting tool + the platform index; the forge leg (canonical producer) also
        // writes the neoforge-namespaced twin index the 21.1 runtime reads (ADR-P17-1).
        // Registered AFTER the dual-directory mirror: the mirror's walk ignores the
        // loot_modifiers family (not in RENAMES), so ordering is only nominal — kept
        // last-append per the tail-append seam discipline.
        //? if forge {
        event.getGenerator().addProvider(true,
            new gregtech6.items.tools.loot.GT6ToolLootModifiersDatagen(event.getGenerator().getPackOutput()));
        //?} else {
        /*event.getGenerator().addProvider(true,
            new gregtech6.items.tools.loot.GT6ToolLootModifiersDatagen(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        *///?}
    }
}
