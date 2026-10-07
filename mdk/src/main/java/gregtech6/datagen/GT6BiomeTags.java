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
import gregtech6.worldgen.GTOreWorldgen;

/**
 * The GT6 biome-tag datagen home (task w6-t1-trees-nine spec ③): the biome
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
 * <p><b>The twilight band</b> (task twilight-vegetation — the extension surface's first
 * FIRST-PARTY tenant; the main-session ruling overturned the TF-wide mount premise:
 * every WorldgenOnSurface subclass overrides {@code canGenerate} with a BiomeNameSet
 * name check (CS.java:263-289), so the port is a PER-FEATURE TAG ADDITION, not a
 * modifier-wide TF mount): {@link #TWILIGHT_MEMBERS} maps each band tag to the
 * intersection of its upstream name set with the TF 1.20.1 biome universe
 * (twilightforest init/TFBiomes.java:19-48, display names cross-read from the en_us
 * lang), appended via {@code addOptional} (TagsProvider.java:152, {@code required:
 * false} — TF absent = the unknown members drop at datapack load = empty resolution,
 * zero mounts, zero errors). Name drift declared: TF 1.20.1 renamed 1.7.10's
 * "Dense Twilight Forest" → "Dense Forest" (twilightforest:dense_forest) — the new
 * display name is in NO upstream name set, so dense_forest rides the WOODS/FOREST
 * rows BY DECLARATION (the same biome renamed; a strict display-name match would
 * exclude it). The other cross-version catches are pure display-name matches
 * (BiomeNameSet.java:44-48, case-insensitive exact): clearing = "Twilight Clearing"
 * (in BIOMES_PLAINS :276 AND BIOMES_HAZEL :277 verbatim) and oak_savannah = "Oak
 * Savanna" — which lives ONLY in BIOMES_SAVANNA (:261), NOT in the plains/woods
 * families, so it belongs to exactly the savanna-flavored rows (sticks moderate,
 * log dry, twilight rocks) and to no tree tag.
 * The TF members deliberately NOT appended anywhere: spooky_forest ("Spooky Forest"
 * is in no card name set), mushroom_forest/dense_mushroom_forest (SHROOM unchecked),
 * lake (BIOMES_RIVER_LAKE only — black sand checks BIOMES_RIVER), final_plateau and
 * the two underground cave biomes.
 *
 * <p><b>The empty tag</b>: rainbowood's upstream set is {@code BIOMES_RAINBOWOOD =
 * ("Enchanted Forest")} (CS.java:289) — no vanilla counterpart, so pure-vanilla GT6
 * generates ZERO rainbowood trees and the port emits the empty {@code values: []} tag
 * (the pack-author activation switch). The twilight band is the switch's first
 * tenant: twilightforest:enchanted_forest rides this very tag — a TF-wide modifier
 * mount would instead have painted rainbow trees across every dimension.
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
    // The bumble-hive family tags (task bees-lv2) — the WorldgenHives surface
    // chain's biome-name families (WorldgenHives.java:157-172) onto
    // {@code #gt6:bumble_hives/<family>}: the vanilla members live, the modded
    // families (magical/volcanic/end/nether — 1.7.10 modded BiomeNameSets) emit
    // EMPTY except the magical family's TF slice (twilight-hives-springs:
    // twilightforest:enchanted_forest, required:false — the deadrock face).
    // Jungle = the 1.7.10 jungle
    // family's 1.18 survivors; frozen = the snowy family (the BIOMES_FROZEN
    // vanilla subset); shore = the OCEAN_BEACH+LAKE pair onto the vanilla ocean/
    // beach/river tags; shroom = the mushroom island biome (the mycelium contact
    // face covers it at runtime too, WorldgenHives.java:173).
    // ------------------------------------------------------------------
    private void addHiveTags() {
        // task twilight-hives-springs — the magical family's FIRST modded member: TF's
        // enchanted_forest ("Enchanted Forest", TFBiomes.ENCHANTED_FOREST — the one
        // BIOMES_MAGICAL name (CS.java:288) surviving into modern TF; "Magical Forest"/
        // "Eldritch"/"Tainted Land"/"Eerie" are Thaumcraft/BoP names, not TF ids).
        // required:false = the deadrock-tag face: TF absent, the tag resolves empty, no
        // crash, no hang (zero REQUIRED elements — the zero-optional discipline holds).
        // The remaining TF slice of the other families ("Mushroom Forest"/"Deep Mushroom
        // Forest" ∈ BIOMES_SHROOM, "Fire Swamp" ∈ BIOMES_VOLCANIC, "Snowy Forest"/
        // "Twilight Glacier" ∈ BIOMES_FROZEN) stays the pack-extension surface, out of
        // this card's declared face.
        tag(GT6HiveFeature.hiveTag("magical"))    // the :157 family — the pack surface + the TF slice
                .addOptional(new ResourceLocation("twilightforest", "enchanted_forest"));
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

    /**
     * The twilight band (task twilight-vegetation) — the per-feature reconciliation
     * table as data: each band tag path (under {@code data/gt6/tags/worldgen/biome/},
     * "trees/&lt;snake&gt;" for the six TF rows of the nine trees) → the TF 1.20.1
     * member ids, the intersection of the upstream BiomeNameSet with the TF universe
     * (the class javadoc carries the per-row upstream anchors). The map IS the single
     * source: the emission walk and the pin test read the same rows. Every member is
     * appended {@code required:false}.
     */
    public static final java.util.Map<String, List<String>> TWILIGHT_MEMBERS;
    static {
        java.util.Map<String, List<String>> tMap = new java.util.LinkedHashMap<>();
        // the six TF trees (Loader_Worldgen.java:608/:609/:610/:612/:615/:616):
        // rubber = BIOMES_RUBBER (CS.java:267) ∩ TF; bluespruce = BIOMES_BLUESPRUCE
        // (CS.java:282 — "Twilight Highlands"+"Thornlands", NOT the snowy pair);
        // rainbowood = BIOMES_RAINBOWOOD (CS.java:289).
        tMap.put("trees/rubber", List.of("highlands", "snowy_forest"));
        tMap.put("trees/maple", List.of("firefly_forest"));
        tMap.put("trees/willow", List.of("swamp"));
        // hazel = BIOMES_HAZEL (CS.java:277 — "Twilight Clearing" only; TF's
        // oak_savannah displays "Oak Savanna", a BIOMES_SAVANNA member, not hazel).
        tMap.put("trees/hazel", List.of("clearing"));
        tMap.put("trees/rainbowood", List.of("enchanted_forest"));
        tMap.put("trees/blue_spruce", List.of("highlands", "thornlands"));
        // the sticks tiers (Loader_Worldgen.java:630; WorldgenSticks.java:53-55): woods|
        // swamp → dense, river|plains|savanna → moderate, taiga|mesa|wastelands → sparse.
        tMap.put("sticks_dense", List.of("forest", "dense_forest", "firefly_forest", "dark_forest",
                "dark_forest_center", "swamp", "fire_swamp"));
        tMap.put("sticks_moderate", List.of("stream", "clearing", "oak_savannah"));
        tMap.put("sticks_sparse", List.of("highlands", "snowy_forest"));
        // glowtus (Loader_Worldgen.java:632; WorldgenGlowtus.java:49 jungle | "Fire Swamp").
        tMap.put("surface_glowtus", List.of("fire_swamp"));
        // bush (Loader_Worldgen.java:633; WorldgenBushes.java:56 plains|woods minus frozen —
        // no SAVANNA group, so TF's "Oak Savanna" is NOT a bush biome).
        tMap.put("surface_bush", List.of("forest", "dense_forest", "firefly_forest", "dark_forest",
                "dark_forest_center", "clearing"));
        // black sand (Loader_Worldgen.java:581; WorldgenBlackSand.java:49-51 — the
        // BIOMES_RIVER requirement ∧ ¬OCEAN_BEACH ∧ ¬SWAMP; "Twilight Stream" is TF's
        // only river-family biome, "Twilight Lake" rides BIOMES_RIVER_LAKE = excluded).
        tMap.put("surface_blacksand", List.of("stream"));
        // the four fallen logs (Loader_Worldgen.java:603-606): dry = PLAINS|WOODS|SAVANNA|
        // DESERT|MESA|WASTELANDS (WorldgenLogDry.java:50 — the SAVANNA group is why TF's
        // "Oak Savanna" rides dry but NOT mossy, whose check :52 is PLAINS|WOODS|SWAMP).
        tMap.put("surface_log_dry", List.of("forest", "dense_forest", "firefly_forest", "dark_forest",
                "dark_forest_center", "clearing", "oak_savannah"));
        tMap.put("surface_log_rotten", List.of("swamp", "fire_swamp"));
        tMap.put("surface_log_mossy", List.of("forest", "dense_forest", "firefly_forest", "dark_forest",
                "dark_forest_center", "clearing", "swamp", "fire_swamp"));
        tMap.put("surface_log_frozen", List.of("snowy_forest", "glacier"));
        // the twilight rocks (Loader_Worldgen.java:621; WorldgenRocks.java:54 nine groups —
        // DESERT|MESA|TAIGA|SWAMP|SAVANNA|PLAINS|WOODS|MOUNTAINS|WASTELANDS; the TF
        // universe minus the untouched seven, 12 members).
        tMap.put("surface_rocks_twilight", List.of("highlands", "snowy_forest", "thornlands", "swamp",
                "fire_swamp", "clearing", "oak_savannah", "forest", "dense_forest", "firefly_forest",
                "dark_forest", "dark_forest_center"));
        TWILIGHT_MEMBERS = java.util.Collections.unmodifiableMap(tMap);
    }

    /** The twilight namespace member id ({@code twilightforest:<aPath>}). */
    private static ResourceLocation twilightforest(String aPath) {
        return ResourceLocation.fromNamespaceAndPath(GTOreWorldgen.TWILIGHT_MODID, aPath);
    }

    /**
     * The twilight band emission (task twilight-vegetation): re-opens each band tag
     * (TagsProvider.tag is computeIfAbsent — the vanilla rows above stay untouched) and
     * appends the optional members, plus the NEW twilight-rocks tag (optional-only, the
     * rainbowood empty-switch form: TF absent = the tag resolves empty = the modifier
     * row is a structural no-op even before its mod_loaded condition skips it).
     */
    private void addTwilightVegetationBand() {
        for (java.util.Map.Entry<String, List<String>> tRow : TWILIGHT_MEMBERS.entrySet()) {
            var tAppender = tag(TagKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(GT6DataGenerators.MOD_ID, tRow.getKey())));
            for (String tMember : tRow.getValue()) {
                tAppender.addOptional(twilightforest(tMember));
            }
        }
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
        addHiveTags(); // task bees-lv2 — the 8 bumble-hive family tags (tail-append)
        for (int i = 0; i < gregtech6.registry.GT6TreeBlocks.KINDS.size(); i++) {
            List<ResourceKey<Biome>> tBiomes = TREE_BIOMES.get(i);
            if (tBiomes.isEmpty()) {
                tag(treeTag(gregtech6.registry.GT6TreeBlocks.KINDS.get(i).snake())); // the empty activation switch
                continue;
            }
            tag(treeTag(gregtech6.registry.GT6TreeBlocks.KINDS.get(i).snake()))
                    .add(tBiomes.toArray(new ResourceKey[0]));
        }
        // task w6-rocks-sticks — the surface deco bands (the tags are NOT
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

        // task w6-t2-surface-blocks — the surface-plants + soil bands. The
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
        addTwilightVegetationBand(); // task twilight-vegetation — the 16-tag TF band (tail-append)

        addAtumTags(); // task atum-dim-adaptation — the dimension-mount tag + the coconut member
    }

    /**
     * The atum band (task atum-dim-adaptation) — the extension surface's mod-dim
     * tenant, the vegetation-card {@code addOptional required:false} form:
     * <ul>
     * <li>{@code #gt6:atum_biomes} = the 11 atum biome ids verbatim
     * ({@link GT6Worldgen#ATUM_BIOME_IDS}, AtumBiomes.java:10-20 of the atum2 master
     * harvest). Atum's own {@code #forge:is_atum} fill covers only 2 of the 11
     * (is_atum.json = strange_sands + oasis), so the mount/routing gate is OUR OWN
     * tag; a member a given Atum build lacks drops silently at datapack load.</li>
     * <li>the coconut tree tag gains {@code atum:oasis} — the ONLY atum biome the
     * upstream coconut gate admits: BIOMES_COCONUT (CS.java:285) carries the literal
     * "Oasis" name and atum:oasis displays exactly "Oasis" (atum en_us.json), while
     * the exclusion sets (MOUNTAINS/FROZEN/TAIGA/SWAMP/WOODS — WorldgenTreeCoconut
     * .java:52-58) match none of the 11 display names ("Limestone Mountains" ≠
     * "Mountains", "Dense Woods" ∉ BIOMES_WOODS — exact-name matching).</li>
     * </ul>
     */
    private void addAtumTags() {
        for (ResourceLocation tId : GT6Worldgen.ATUM_BIOME_IDS) {
            tag(GT6Worldgen.ATUM_BIOMES).addOptional(tId);
        }
        tag(treeTag("coconut")).addOptional(
                ResourceLocation.fromNamespaceAndPath(GT6Worldgen.ATUM_MODID, "oasis"));
    }
}
