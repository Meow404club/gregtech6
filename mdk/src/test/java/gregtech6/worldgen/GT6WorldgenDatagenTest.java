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
 *     (GT6Worldgen javadoc "documented deviation").</li>
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
import static org.junit.jupiter.api.Assertions.assertSame;
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
        assertEquals(100, GT6Worldgen.BLOB_PROBABILITY, "probability=100 -> 1/100 chunk attempts");
        assertEquals(0, GT6Worldgen.OVERWORLD_MIN_Y, "overworld MinHeight 0");
        assertEquals(120, GT6Worldgen.OVERWORLD_MAX_Y, "overworld MaxHeight 120");
        assertEquals("overworld_stone_marble", GT6Worldgen.entryPath("marble"),
                "entry id = the upstream config name overworld.stone.<material>, dots flattened");
        assertEquals("overworld.stone.", GT6Worldgen.UPSTREAM_CATEGORY_PREFIX, "the naming evidence anchor");
    }

    /** The 17 configured keys, GTStoneBlocks.STONES order, all in the CONFIGURED_FEATURE registry. */
    @Test
    void configuredKeysOrderIsPinned() {
        assertEquals(17, GT6Worldgen.CONFIGURED_KEYS.size(), "17 stone blobs (the card pin; upstream is 15 — see the deviation note)");
        assertEquals(SNAKES, GTStoneBlocks.STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList(),
                "precondition: STONES order is the CS.java:1668 order");
        for (int i = 0; i < SNAKES.size(); i++) {
            assertEquals("minecraft:worldgen/configured_feature",
                    GT6Worldgen.CONFIGURED_KEYS.get(i).registry().toString(),
                    "configured key " + i + " must live in minecraft:configured_feature");
            assertEquals("gt6:" + GT6Worldgen.entryPath(SNAKES.get(i)),
                    GT6Worldgen.CONFIGURED_KEYS.get(i).location().toString(),
                    "configured key " + i + " must be overworld_stone_" + SNAKES.get(i));
        }
    }

    /** The 17 placed keys, same order/paths as the configured band (placed[i] hangs off configured[i]). */
    @Test
    void placedKeysOrderIsPinned() {
        assertEquals(17, GT6Worldgen.PLACED_KEYS.size(), "17 placed features");
        for (int i = 0; i < SNAKES.size(); i++) {
            assertEquals("minecraft:worldgen/placed_feature",
                    GT6Worldgen.PLACED_KEYS.get(i).registry().toString(),
                    "placed key " + i + " must live in minecraft:placed_feature");
            assertEquals("gt6:" + GT6Worldgen.entryPath(SNAKES.get(i)),
                    GT6Worldgen.PLACED_KEYS.get(i).location().toString(),
                    "placed key " + i + " shares the configured path (GTCEu blob form)");
        }
    }

    /** The 17 biome-modifier keys, same order/paths, in the leg's biome_modifier registry. */
    @Test
    void biomeModifierKeysOrderIsPinned() {
        assertEquals(17, GT6WorldgenDatagen.BIOME_MODIFIER_KEYS.size(), "17 AddFeaturesBiomeModifier rows");
        ResourceLocation tLegRegistry = GT6WorldgenDatagen.biomeModifierRegistryKey().location();
        assertTrue(tLegRegistry.getPath().equals("biome_modifier") && !tLegRegistry.getNamespace().equals("minecraft"),
                "the biome-modifier registry key must be the leg's <loader>:biome_modifier, got " + tLegRegistry);
        for (int i = 0; i < SNAKES.size(); i++) {
            assertEquals(tLegRegistry.toString(),
                    GT6WorldgenDatagen.BIOME_MODIFIER_KEYS.get(i).registry().toString(),
                    "biome modifier key " + i + " must live in " + tLegRegistry);
            assertEquals("gt6:" + GT6Worldgen.entryPath(SNAKES.get(i)),
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
     * The zero-new-block dependency: the worldgen universe's registration face declares
     * Feature-only (no BLOCK DeferredRegister in the package), and every worldgen id maps
     * back 1:1 onto a GTStoneBlocks.STONES row — the 17 pin is the whole block universe
     * this card touches.
     */
    @Test
    void zeroNewBlockDependencyIsPinned() {
        assertEquals(17, GTStoneBlocks.STONES.size(), "the GTStoneBlocksRegistrationTest.java:108 pin holds");
        assertSame(Registries.FEATURE, GT6Features.FEATURES.getRegistryKey(),
                "GT6Features registers Feature only — no new blocks");
        assertEquals(0, GT6Features.FEATURES.getEntries().size(),
                "L0 registers zero entries (the blobs are pure vanilla-OreFeature datagen)");
        List<String> tWorldgenPaths = new ArrayList<>();
        for (int i = 0; i < GT6Worldgen.CONFIGURED_KEYS.size(); i++) {
            tWorldgenPaths.add(GT6Worldgen.CONFIGURED_KEYS.get(i).location().getPath());
        }
        assertEquals(tWorldgenPaths.size(), GTStoneBlocks.STONES.size(), "one blob id per stone row");
        for (int i = 0; i < tWorldgenPaths.size(); i++) {
            assertEquals(GT6Worldgen.entryPath(SNAKES.get(i)), tWorldgenPaths.get(i),
                    "blob id " + i + " derives from the STONES row, no invented block id");
        }
    }
}
