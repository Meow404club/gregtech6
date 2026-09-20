package gregtech6.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.data.ExistingFileHelper;

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import gregtech6.worldgen.GT6HiveFeature;
import gregtech6.worldgen.GT6Worldgen;

/**
 * The GT6 biome-tag datagen home (task p30-w6-t1-trees-nine spec ③): the biome
 * determination face of the 9 tree worldgen rows — the upstream {@code BIOMES_*}
 * BiomeNameSets (CS.java:263-290) translate onto per-tree tags
 * {@code #gt6:trees/<snake>}, and the tree biome-modifier JSONs reference exactly these
 * tags (the datapack-native per-feature biome config face: a pack ADDS biomes by
 * extending the tag file, the WorldgenObject per-biome config equivalence).
 *
 * <p><b>The vanilla subset</b> (the declared translation, every entry a live
 * {@code net.minecraft.world.level.biome.Biomes} constant): the upstream sets mix 1.7.10
 * vanilla biome ids with modded biome NAMES (BOP/Twilight/...); the port emits the
 * vanilla members only — the modded names are the tag's datapack extension surface (a
 * BOP pack appends its ids to {@code #gt6:trees/rubber}; the modifier widens with it).
 * Mappings: taiga family → taiga/taiga_hills/snowy_taiga/snowy_taiga_hills/
 * old_growth_pine_taiga/old_growth_spruce_taiga; forest family → forest/forest_hills;
 * swampland → swamp; jungle family → jungle/jungle_hills/sparse_jungle;
 * plains → plains; beach → beach; extremeHills family → windswept_hills/
 * windswept_forest/windswept_gravelly_hills/stony_shore (the 1.18 family reshape —
 * extremeHillsEdge was REMOVED in 1.18, its coverage folds into the windswept pair).
 *
 * <p><b>The empty tag</b>: rainbowood's upstream set is {@code BIOMES_RAINBOWOOD =
 * ("Enchanted Forest")} (CS.java:286) — a Thaumcraft biome with NO vanilla counterpart,
 * so pure-vanilla upstream GT6 also generates ZERO rainbowood trees. The port emits the
 * empty {@code values: []} tag (the biome-modifier row stays uniform; the tag is the
 * pack-author activation switch — strictness unaffected, an empty list references no
 * element so the TagsProvider zero-optional discipline holds).
 *
 * <p>Directory: {@code data/gt6/tags/worldgen/biome/**} — the registry-key dir is
 * IDENTICAL on both legs ("worldgen/biome" already singular), so no
 * GT6DualDirectoryFaces row and no datagen_tree_check SEGMENT_MAP entry.
 */
public final class GT6BiomeTags extends TagsProvider<Biome> {

    /** The tag key of a tree ({@code #gt6:trees/<snake>}) — GT6WorldgenDatagen references the same composition. */
    public static TagKey<Biome> treeTag(String aTreeSnake) {
        return TagKey.create(Registries.BIOME,
                new ResourceLocation(GT6DataGenerators.MOD_ID, "trees/" + aTreeSnake));
    }

    /**
     * The biome keys are built by NAME (not the Biomes constants): the 1.20.1 decompiled
     * ref carries only 65 of the register rows (taiga_hills/forest_hills/jungle_hills are
     * missing from it), and name-built keys are reference-exact on both legs — the
     * TagsProvider strictness validates every name against the live registry at datagen
     * time, so a typo fails runData loudly (the zero-optional discipline).
     */
    // ------------------------------------------------------------------
    // The bumble-hive family tags (task p32-bees-lv2) — the WorldgenHives surface
    // chain's biome-name families (WorldgenHives.java:157-172) onto
    // {@code #gt6:bumble_hives/<family>}: the vanilla members live, the modded
    // families (magical/volcanic/end/nether — 1.7.10 modded BiomeNameSets) emit
    // EMPTY (the rainbowood activation-switch face). Jungle = the 1.7.10 jungle
    // family's 1.18 survivors; frozen = the snowy family (the BIOMES_FROZEN
    // vanilla subset); shore = the OCEAN_BEACH+LAKE pair onto the vanilla ocean/
    // beach/river tags; shroom = the mushroom island biome (the mycelium contact
    // face covers it at runtime too, WorldgenHives.java:173).
    // ------------------------------------------------------------------
    private void addHiveTags() {
        tag(GT6HiveFeature.hiveTag("magical"));   // the EMPTY pack surface (the :157 family)
        tag(GT6HiveFeature.hiveTag("volcanic"));  // the EMPTY pack surface (the :159 family)
        tag(GT6HiveFeature.hiveTag("end"));       // the EMPTY pack surface (the :161 family)
        tag(GT6HiveFeature.hiveTag("nether"));    // the EMPTY pack surface (the :163 family)
        tag(GT6HiveFeature.hiveTag("shroom"))     // :165 — the mushroom fields biome
                .add(biome("mushroom_fields"));
        tag(GT6HiveFeature.hiveTag("shore"))      // :167 — the OCEAN_BEACH+LAKE pair
                .addTag(BiomeTags.IS_OCEAN)
                .addTag(BiomeTags.IS_BEACH)
                .addTag(BiomeTags.IS_RIVER);
        tag(GT6HiveFeature.hiveTag("jungle"))     // :169 — the jungle family
                .add(biome("jungle"), biome("sparse_jungle"), biome("bamboo_jungle"));
        tag(GT6HiveFeature.hiveTag("frozen"))     // :171 — the snowy family
                .add(biome("snowy_plains"), biome("ice_spikes"), biome("snowy_taiga"),
                        biome("snowy_slopes"), biome("frozen_peaks"), biome("jagged_peaks"),
                        biome("frozen_ocean"), biome("deep_frozen_ocean"), biome("frozen_river"),
                        biome("snowy_beach"));
    }

    private static ResourceKey<Biome> biome(String aName) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", aName));
    }

    /** The vanilla subset per kind, GT6TreeBlocks.KINDS order (the class-javadoc mapping). */
    public static final List<List<ResourceKey<Biome>>> TREE_BIOMES = List.of(
            // RUBBER = BIOMES_RUBBER (CS.java:267, the taiga family) — the 1.7.10
            // taigaHills/coldTaigaHills hills variants were REMOVED in 1.18 (folded
            // into the plains taiga/snowy_taiga), so the family is the four survivors
            // plus the two old-growth giants
            List.of(biome("taiga"), biome("snowy_taiga"),
                    biome("old_growth_pine_taiga"), biome("old_growth_spruce_taiga")),
            // MAPLE = BIOMES_MAPLE (CS.java:280, the vanilla members forest/forestHills)
            // — forest_hills REMOVED in 1.18
            List.of(biome("forest")),
            // WILLOW = BIOMES_WILLOW (CS.java:264, the vanilla member swampland)
            List.of(biome("swamp")),
            // BLUE_MAHOE = BIOMES_BLUEMAHOE (CS.java:256, the jungle family — jungle_hills
            // REMOVED in 1.18)
            List.of(biome("jungle"), biome("sparse_jungle")),
            // HAZEL = BIOMES_HAZEL (CS.java:283, the vanilla member plains)
            List.of(biome("plains")),
            // CINNAMON = BIOMES_CINNAMON (CS.java:256, the jungle family)
            List.of(biome("jungle"), biome("sparse_jungle")),
            // COCONUT = BIOMES_COCONUT (CS.java:285, the vanilla member beach; the
            // upstream mountain/frozen/taiga/swamp/woods exclusions never intersect it)
            List.of(biome("beach")),
            // RAINBOWOOD = BIOMES_RAINBOWOOD (CS.java:286) — NO vanilla member, the empty
            // activation-switch tag (see the class javadoc)
            List.of(),
            // BLUE_SPRUCE = BIOMES_BLUESPRUCE (CS.java:288, the extremeHills family)
            List.of(biome("windswept_hills"), biome("windswept_forest"), biome("windswept_gravelly_hills"),
                    biome("stony_shore")));

    public GT6BiomeTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider,
            ExistingFileHelper aExistingFileHelper) {
        super(aOutput, Registries.BIOME, aLookupProvider, GT6DataGenerators.MOD_ID, aExistingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider aProvider) {
        addHiveTags(); // task p32-bees-lv2 — the 8 bumble-hive family tags (tail-append)
        for (int i = 0; i < gregtech6.registry.GT6TreeBlocks.KINDS.size(); i++) {
            List<ResourceKey<Biome>> tBiomes = TREE_BIOMES.get(i);
            if (tBiomes.isEmpty()) {
                tag(treeTag(gregtech6.registry.GT6TreeBlocks.KINDS.get(i).snake())); // the empty activation switch
                continue;
            }
            tag(treeTag(gregtech6.registry.GT6TreeBlocks.KINDS.get(i).snake()))
                    .add(tBiomes.toArray(new ResourceKey[0]));
        }
        // task p30-w6-rocks-sticks — the surface deco bands (the tags are NOT
        // loader-branded, one band serves both legs; the rocks version's
        // BiomeTagsProvider base folds into this TagsProvider<Biome>):
        // WorldgenRocks.java:54 — the nine rock biome groups (wastelands skipped, no vanilla tag).
        tag(GT6Worldgen.SURFACE_ROCKS_BIOMES)
                .add(Biomes.DESERT, Biomes.PLAINS, Biomes.SNOWY_PLAINS, Biomes.SUNFLOWER_PLAINS,
                        Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
                .addTag(BiomeTags.IS_BADLANDS)  // MESA
                .addTag(BiomeTags.IS_TAIGA)
                .addTag(BiomeTags.IS_SAVANNA)
                .addTag(BiomeTags.IS_FOREST)    // WOODS
                .addTag(BiomeTags.IS_MOUNTAIN)  // MOUNTAINS
                .addTag(BiomeTags.IS_HILL);     // the windswept extremeHills family
        // WorldgenSticks.java:53 — woods|swamp.
        tag(GT6Worldgen.STICKS_DENSE_BIOMES)
                .add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
                .addTag(BiomeTags.IS_FOREST);
        // :54 — river|plains|savanna.
        tag(GT6Worldgen.STICKS_MODERATE_BIOMES)
                .add(Biomes.PLAINS, Biomes.SNOWY_PLAINS, Biomes.SUNFLOWER_PLAINS)
                .addTag(BiomeTags.IS_RIVER)
                .addTag(BiomeTags.IS_SAVANNA);
        // :55 — taiga|mesa|wasteland.
        tag(GT6Worldgen.STICKS_SPARSE_BIOMES)
                .addTag(BiomeTags.IS_TAIGA)
                .addTag(BiomeTags.IS_BADLANDS);

        // task p30-w6-t2-surface-blocks — the surface-plants + soil bands. The
        // BIOMES_* vanilla-subset discipline (the class javadoc): 1.7.10 sets
        // carry the 1.18-removed hills names, the survivors + the vanilla tag
        // families stand in, modded names are the datapack extension face.
        // WorldgenGlowtus.java:49 — jungle | Fire Swamp (twilight, no vanilla).
        tag(GT6Worldgen.GLOWTUS_BIOMES)
                .add(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);
        // WorldgenBushes.java:56 — plains|woods MINUS the frozen set.
        tag(GT6Worldgen.BUSH_BIOMES)
                .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS)
                .addTag(BiomeTags.IS_FOREST);
        // WorldgenBlackSand.java:49-51 — river minus ocean/beach/swamp (the river
        // tag never intersects those families; modded exclusions are the pack face).
        tag(GT6Worldgen.BLACKSAND_BIOMES)
                .addTag(BiomeTags.IS_RIVER);
        // WorldgenTurf.java:51 — swamp (no vanilla swamp tag: the explicit pair).
        tag(GT6Worldgen.TURF_BIOMES)
                .add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
        // WorldgenPit.java:58 — plains|savanna (the chunk-centre gate).
        tag(GT6Worldgen.PIT_CLAY_BIOMES)
                .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS)
                .addTag(BiomeTags.IS_SAVANNA);
        // WorldgenLogDry.java:50 — plains|woods|savanna|desert|mesa|wastelands.
        tag(GT6Worldgen.LOG_DRY_BIOMES)
                .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.DESERT)
                .addTag(BiomeTags.IS_FOREST)
                .addTag(BiomeTags.IS_SAVANNA)
                .addTag(BiomeTags.IS_BADLANDS);
        // WorldgenLogRotten.java:49 — swamp|jungle.
        tag(GT6Worldgen.LOG_ROTTEN_BIOMES)
                .add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
                .addTag(BiomeTags.IS_JUNGLE);
        // WorldgenLogMossy.java:52 — plains|woods|swamp.
        tag(GT6Worldgen.LOG_MOSSY_BIOMES)
                .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
                .addTag(BiomeTags.IS_FOREST);
        // WorldgenLogFrozen.java:50 — frozen (no vanilla snowy tag: the explicit trio).
        tag(GT6Worldgen.LOG_FROZEN_BIOMES)
                .add(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA);
    }
}
