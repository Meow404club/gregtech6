/**
 * The drop-face ratchet (task kitchen-loot-dropface): the manual-kitchen family loot face
 * ({@code GT6LootTables.GT6KitchenBlockLoot}, the tank-valve/item-pipe dropSelf lane) ships
 * the seven kitchen blocks' self-drop tables — pre-card the family was table-less and broke
 * into NOTHING (the 1.20.1 vanilla default: missing table = zero drops).
 *
 * <p>Shape-b ruling (the task-card archaeology, read in tmp/gt6-1.7.10): the kitchen MTEs
 * override NEITHER getDrops NOR writeItemNBT, so breaking one lands the MTE-default
 * self-drop (TileEntityBase04MultiTileEntities.getDrops:166-171) whose item NBT is ONLY
 * the customName/paint pair (writeItemNBT:128-133) — the variant material is baked into
 * the per-row MTE ID, and the tank contents are trashed explicitly by the upstream
 * breakBlock overrides (MultiTileEntityBathingPot.java:333-336 /
 * MultiTileEntityMixingBowl.java:354-357 / MultiTileEntityJuicer.java:217-219,
 * {@code GarbageGT.trash(mTanks)}). The port BE exposes no item inventory, so its fluids
 * die with the removal too — dropSelf is the 1:1 translation, and the behavior pin below
 * forbids any {@code functions}/{@code copy_nbt} carry forever. No driver-domain skip:
 * pure Ceramic/StainlessSteel/WoodTreated, zero foreign-gated materials.
 *
 * <p>Why tree-decisions, not the face itself: the family blocks live in
 * {@code RegistryObject}s that only bind under real registry events, so
 * {@code kitchenLootBlocks()} cannot dereference in this headless JVM (the
 * GT6ItemPipeLootSelfPinTest split verbatim). The runData + datagen_tree_check gate closes
 * the loop on the emitted tree.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregtech6.registry.GTMaterialItems;

public class GT6KitchenLootDropFacePinTest {

    /**
     * The ledger: the seven upstream rows (Loader_MultiTileEntities.java:2173-2184 — the
     * wood/steel pot pair, the bowl, the Juicer, and the three table variants :2174/:2176/
     * :2178), exactly the GT6Kitchen block registrations.
     */
    private static final List<String> KITCHEN_SLUGS = List.of(
            "bathing_pot_wood", "bathing_pot_steel", "mixing_bowl", "juicer",
            "bathing_pot_table_wood", "bathing_pot_table_steel", "mixing_bowl_table");

    /** The kitchen-family filename filter over the blocks-loot dir (family-exact, no strays counted). */
    private static boolean kitchenTable(String aFile) {
        return KITCHEN_SLUGS.contains(aFile.substring(0, aFile.length() - ".json".length()));
    }

    @BeforeAll
    public static void initMaterialSystem() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline init noise; the registries are usable by now
        }
        GTMaterialItems.initMaterials();
    }

    /** The mdk project root, walking up from the (leg-dependent) test working dir (the convergence-test anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The committed band dirs for the blocks-loot domain (1.20.1 plural + 1.21 singular twin). */
    private static List<Path> bands() {
        Path tData = mdkRoot().resolve("src/generated/resources/data/gt6");
        return List.of(tData.resolve("loot_tables").resolve("blocks"), tData.resolve("loot_table").resolve("blocks"));
    }

    // ---- the tree ratchet: exactly the seven tables, both committed bands ----

    @Test
    public void theSevenTablesShipInBothBands() throws IOException {
        for (Path tBand : bands()) {
            assertTrue(Files.isDirectory(tBand), "the committed band must exist: " + tBand);
            for (String tSlug : KITCHEN_SLUGS)
                assertTrue(Files.isRegularFile(tBand.resolve(tSlug + ".json")),
                        tBand.getFileName() + ": the kitchen table must ship: " + tSlug);
        }
    }

    @Test
    public void theFamilyTotalIsExactlyTheLedgerWithNoStrays() throws IOException {
        for (Path tBand : bands()) {
            try (Stream<Path> tWalk = Files.walk(tBand)) {
                List<String> tKitchen = tWalk.filter(p -> p.toString().endsWith(".json"))
                        .map(p -> p.getFileName().toString())
                        .filter(GT6KitchenLootDropFacePinTest::kitchenTable).toList();
                assertEquals(KITCHEN_SLUGS.size(), tKitchen.size(), tBand + ": kitchen tables");
            }
        }
    }

    // ---- the shape-b behavior pin: the pure upstream self-drop, zero NBT carry ----

    @Test
    public void everyKitchenTableIsThePureDropSelfShape() throws IOException {
        Path tBand = bands().get(0);
        for (String tSlug : KITCHEN_SLUGS) {
            String tJson = Files.readString(tBand.resolve(tSlug + ".json"));
            assertTrue(tJson.contains("\"type\": \"minecraft:block\""), tSlug + ": a block table");
            assertTrue(tJson.contains("\"name\": \"gt6:" + tSlug + "\""), tSlug + ": drops its own item");
            assertTrue(tJson.contains("minecraft:survives_explosion"), tSlug + ": the explosion gate");
            assertTrue(tJson.contains("\"rolls\": 1.0") && tJson.contains("\"bonus_rolls\": 0.0"),
                    tSlug + ": one roll");
            // the shape-b verdict: NO item-NBT functions — upstream carries only customName/paint
            // (TileEntityBase04MultiTileEntities.writeItemNBT:128-133), and the port BE exposes
            // neither face; the tank contents are trashed upstream too, dropSelf loses nothing.
            assertFalse(tJson.contains("\"functions\""), tSlug + ": no loot functions (no copy_nbt)");
            assertFalse(tJson.contains("copy_nbt") || tJson.contains("copy_components"),
                    tSlug + ": no NBT carry");
        }
    }
}
