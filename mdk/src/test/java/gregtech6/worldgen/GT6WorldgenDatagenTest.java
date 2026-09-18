/**
 * Tests for task p26-worldgen-pipeline-skeleton: the worldgen constant table + the
 * RegistrySetBuilder band — the acceptance's offline audit unit.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_Worldgen.java:654-661 — the WorldgenStone loop; the overworld row binds
 *     (amount=1, size=200, probability=100, minY=0, maxY=120), WorldgenStone.java:41
 *     ctor order; upstream EXCLUDES the two prismarines — the card pins 17 anyway
 *     (GT6Worldgen javadoc "documented deviation"). The size 200 rides the vanilla
 *     OreConfiguration codec cap (intRange(0,64)) through GT6Worldgen.oreBlobSize().</li>
 * <li>WorldgenBlob.java:55-57 — the probability/amount/size config binds.</li>
 * <li>GTStoneBlocksRegistrationTest.SNAKES — the 17-stone CS.java:1668 order (the
 *     key tables must align GTStoneBlocks.STONES 1:1).</li>
 * <li>GTCEu GTConfiguredFeatures/GTPlacedFeatures/GTBiomeModifiers — the three-
 *     bootstrap shape the builder mirrors.</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GTStoneBlocksRegistrationTest posture): only
 * ResourceKey interns, string paths and the DeferredRegister entriesView — no
 * RegisterEvent, no bootstrapped registries, no BlockState construction.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;

class GT6WorldgenDatagenTest {

    /** The snake ids of the 17 stones, CS.java:1668 order (GTStoneBlocksRegistrationTest.SNAKES verbatim). */
    private static final List<String> SNAKES = List.of(
        "granite_black", "granite_red", "basalt", "marble", "limestone", "granite", "diorite",
        "andesite", "komatiite", "greenschist", "blueschist", "kimberlite", "quartzite",
        "prismarine_light", "prismarine_dark", "slate", "shale");

    @BeforeAll
    static void boot() {
        // the material system must exist before GTStoneBlocks.STONES dereferences its
        // suppliers (GTStoneBlocksRegistrationTest posture); vanilla bootstrap bracket for
        // the ResourceKey/registry-key classes (offline throwables ignored).
        GTMaterialItems.initMaterials();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The Loader_Worldgen.java:655 overworld row binds, transcribed into the constant table. */
    @Test
    void blobConstantsArePinned() {
        assertEquals(1, GT6Worldgen.BLOB_AMOUNT, "amount=1 blob per probability hit (WorldgenBlob bind 1..16)");
        assertEquals(200, GT6Worldgen.BLOB_SIZE, "size=200 (WorldgenBlob bind 4..250)");
        assertEquals(64, GT6Worldgen.ORE_SIZE_CODEC_CAP,
                "the vanilla OreConfiguration size codec cap (Codec.intRange(0, 64), 1.20.1 :14 / 1.21.1 :15)");
        assertEquals(64, GT6Worldgen.oreBlobSize(),
                "the datagen size = min(upstream 200, codec cap 64) — the documented deviation; raw 200 fails encode AND datapack decode");
        assertEquals(100, GT6Worldgen.BLOB_PROBABILITY, "probability=100 -> 1/100 chunk attempts");
        assertEquals(0, GT6Worldgen.OVERWORLD_MIN_Y, "overworld MinHeight 0");
        assertEquals(120, GT6Worldgen.OVERWORLD_MAX_Y, "overworld MaxHeight 120");
        assertEquals("overworld_stone_marble", GT6Worldgen.entryPath("marble"),
                "entry id = the upstream config name overworld.stone.<material>, dots flattened");
        assertEquals("overworld.stone.", GT6Worldgen.UPSTREAM_CATEGORY_PREFIX, "the naming evidence anchor");
    }

    /**
     * The 12 blob configured keys, BLOB_STONES order, all in the CONFIGURED_FEATURE
     * registry. Task p31-strata-lens: the 5 marker stones ride the strata-lens feature
     * (GT6Worldgen.LENS_STONE_SNAKES), their blob rows retired.
     */
    @Test
    void configuredKeysOrderIsPinned() {
        assertEquals(12, GT6Worldgen.CONFIGURED_KEYS.size(),
                "12 stone blobs (task p31-strata-lens: 17 minus the 5 marker stones, see the lens band)");
        assertEquals(SNAKES, GTStoneBlocks.STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList(),
                "precondition: STONES order is the CS.java:1668 order");
        List<String> tBlobSnakes = GT6Worldgen.BLOB_STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList();
        assertEquals(SNAKES.stream().filter(tSnake -> !GT6Worldgen.LENS_STONE_SNAKES.contains(tSnake)).toList(),
                tBlobSnakes, "the blob band = STONES minus the 5 marker stones, order preserved");
        for (int i = 0; i < tBlobSnakes.size(); i++) {
            assertEquals("minecraft:worldgen/configured_feature",
                    GT6Worldgen.CONFIGURED_KEYS.get(i).registry().toString(),
                    "configured key " + i + " must live in minecraft:configured_feature");
            assertEquals("gt6:" + GT6Worldgen.entryPath(tBlobSnakes.get(i)),
                    GT6Worldgen.CONFIGURED_KEYS.get(i).location().toString(),
                    "configured key " + i + " must be overworld_stone_" + tBlobSnakes.get(i));
        }
    }

    /** The 12 blob placed keys, same order/paths as the configured band (placed[i] hangs off configured[i]). */
    @Test
    void placedKeysOrderIsPinned() {
        assertEquals(12, GT6Worldgen.PLACED_KEYS.size(), "12 placed features");
        List<String> tBlobSnakes = GT6Worldgen.BLOB_STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList();
        for (int i = 0; i < tBlobSnakes.size(); i++) {
            assertEquals("minecraft:worldgen/placed_feature",
                    GT6Worldgen.PLACED_KEYS.get(i).registry().toString(),
                    "placed key " + i + " must live in minecraft:placed_feature");
            assertEquals("gt6:" + GT6Worldgen.entryPath(tBlobSnakes.get(i)),
                    GT6Worldgen.PLACED_KEYS.get(i).location().toString(),
                    "placed key " + i + " shares the configured path (GTCEu blob form)");
        }
    }

    /** The 12 blob biome-modifier keys, same order/paths, in the leg's biome_modifier registry. */
    @Test
    void biomeModifierKeysOrderIsPinned() {
        assertEquals(12, GT6WorldgenDatagen.BIOME_MODIFIER_KEYS.size(),
                "12 AddFeaturesBiomeModifier rows (the 5 marker stones ride the one strata_lenses modifier)");
        ResourceLocation tLegRegistry = GT6WorldgenDatagen.biomeModifierRegistryKey().location();
        assertTrue(tLegRegistry.getPath().equals("biome_modifier") && !tLegRegistry.getNamespace().equals("minecraft"),
                "the biome-modifier registry key must be the leg's <loader>:biome_modifier, got " + tLegRegistry);
        List<String> tBlobSnakes = GT6Worldgen.BLOB_STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList();
        for (int i = 0; i < tBlobSnakes.size(); i++) {
            assertEquals(tLegRegistry.toString(),
                    GT6WorldgenDatagen.BIOME_MODIFIER_KEYS.get(i).registry().toString(),
                    "biome modifier key " + i + " must live in " + tLegRegistry);
            assertEquals("gt6:" + GT6Worldgen.entryPath(tBlobSnakes.get(i)),
                    GT6WorldgenDatagen.BIOME_MODIFIER_KEYS.get(i).location().toString(),
                    "biome modifier key " + i + " shares the blob path");
        }
    }

    /** The builder carries exactly the three BootstapContext bands (the acceptance ctx-key audit). */
    @Test
    void builderEntryKeysArePinned() {
        // 1.20.1 RegistrySetBuilder has no key accessor (getEntryKeys is 1.21.1-only,
        // vanilla-mc 1.21.1 RegistrySetBuilder.java:75) — the common pin rides the
        // BUILDER_KEYS mirror (the BUILDER rows are constructed from the same three keys,
        // GT6WorldgenDatagen.BUILDER textually bound), the neoforge leg additionally
        // introspects the real builder below.
        List<String> tKeyLocations = GT6WorldgenDatagen.BUILDER_KEYS.stream()
                .map(tKey -> tKey.location().toString()).toList();
        Set<String> tKeys = new HashSet<>(tKeyLocations);
        assertEquals(tKeyLocations.size(), tKeys.size(), "builder registry keys must be distinct");
        Set<String> tExpected = Set.of(
                "minecraft:worldgen/configured_feature",
                "minecraft:worldgen/placed_feature",
                GT6WorldgenDatagen.biomeModifierRegistryKey().location().toString());
        assertEquals(tExpected, tKeys, "exactly configured/placed/biome_modifier, no other band");
        //? if neoforge {
        /*assertEquals(GT6WorldgenDatagen.BUILDER_KEYS, GT6WorldgenDatagen.BUILDER.getEntryKeys(),
                "1.21.1 builder introspection must equal the BUILDER_KEYS mirror");
        *///?}
    }

    /**
     * The registration face: the worldgen universe's own DeferredRegister declares
     * Feature-only (no BLOCK register in the package) and every worldgen id maps back 1:1
     * onto a GTStoneBlocks.STONES row. Task p30-w6-t1-trees-nine note: the
     * {@code GT6Features.TREE_FEATURES} instance face is NOT loadable here — Feature's
     * clinit chains into MonsterRoomFeature/EntityType (Feature.java:82) whose bootstrap
     * needs the live datafixer (Util.fetchChoiceType), so the offline JVM ignores the
     * bracket and the 9-entry/id face is audited by runData (9 configured JSONs typed
     * gt6:tree_&lt;snake&gt;) + datagen_tree_check instead.
     */
    @Test
    void zeroNewBlockDependencyIsPinned() {
        assertEquals(17, GTStoneBlocks.STONES.size(), "the GTStoneBlocksRegistrationTest.java:108 pin holds");
        // NOTE: GT6Features itself is NOT loadable in the offline JVM since
        // p30-w6-t1-trees-nine — its clinit instantiates GT6TreeFeature (extends Feature),
        // and Feature's clinit chains into MonsterRoomFeature/EntityType (Feature.java:82)
        // whose bootstrap needs the live datafixer (Util.fetchChoiceType) — so the
        // registers-Feature-only pin and the 9-entry face are audited by runData (9
        // configured JSONs typed gt6:tree_<snake>) + datagen_tree_check + the RCON arm.
        assertEquals(GT6TreeBlocks.KINDS.size(), GT6Worldgen.TREE_CONFIGURED_KEYS.size(),
                "9 tree configured keys — the offline-safe face of the 9-feature band");
        List<String> tWorldgenPaths = new ArrayList<>();
        for (int i = 0; i < GT6Worldgen.CONFIGURED_KEYS.size(); i++) {
            tWorldgenPaths.add(GT6Worldgen.CONFIGURED_KEYS.get(i).location().getPath());
        }
        List<String> tBlobSnakes = GT6Worldgen.BLOB_STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList();
        assertEquals(tWorldgenPaths.size(), tBlobSnakes.size(), "one blob id per blob-stone row (12 after the lens split)");
        for (int i = 0; i < tWorldgenPaths.size(); i++) {
            assertEquals(GT6Worldgen.entryPath(tBlobSnakes.get(i)), tWorldgenPaths.get(i),
                    "blob id " + i + " derives from the BLOB_STONES row, no invented block id");
        }
    }
}
