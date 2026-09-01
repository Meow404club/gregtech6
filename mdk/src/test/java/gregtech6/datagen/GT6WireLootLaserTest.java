/**
 * Offline assertions for task p11-wire-laser-loot: the laser wire block joins the shared
 * wire loot provider (the p10 E1 redstone six precedent, commit 23e2292), so breaking a
 * placed laser fiber wire drops the block itself.
 *
 * <p>Why the JSON layer: {@link gregtech6.registry.GTWires#wireBlockArray} family blocks live
 * in {@code RegistryObject}s that are only bound when the real registry events fire (datagen
 * or game JVM) — {@code GT6LootTables.wireLootBlocks()} dereferences them and therefore
 * cannot run in this headless JVM, and a real break needs the vanilla loot manager. This is
 * the {@link GT6PrefixBlockRenderDatagenTest#lootTableCountEqualsBlockCountByConstruction}
 * split verbatim: the enumeration side is gated by runData (written&gt;0 then written:0), the
 * generated-JSON side is asserted here against the committed tree.
 *
 * <p>Card-premise note, pinned by {@link #laserFamilyIsExactlyOneBlockOnTheWireLaserPath}:
 * the task card said "16 laser blocks", but the family is exactly ONE block — upstream
 * registers a single "Laser Fiber Wire" (Loader_MultiTileEntities.java:1814-1815, one
 * {@code aRegistry.add}; there is no size ladder and no cable form) and the port pins that
 * census with {@link gregtech6.registry.GTWireSpecs#EXPECTED_LASER_VARIANTS} == 1. The card's
 * "independent loot tree" also does not exist: d3380ae's 3 artifacts are the blockstate /
 * item model / lang triple (render+lang, other cards' surfaces) — the laser block simply
 * shipped table-less until this card.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;

public class GT6WireLootLaserTest {

    /** The laser registry path (GTWireSpecs.registryName over the single laser variant). */
    private static final String LASER_PATH = "wire_laser";

    /** The E1 precedent rows (commit 23e2292) — the isomorphism yardstick and the regression sentinel. */
    private static final List<String> REDSTONE_PATHS = List.of(
            "wire_red_alloy", "cable_red_alloy",
            "wire_signalum", "cable_signalum",
            "wire_lumium", "cable_lumium");

    @BeforeAll
    public static void initMaterialSystem() {
        // the GTWireSpecsCensusTest boot: the spec table is MC-free, only the material
        // dereference suppliers need the MT rows.
        GTMaterialItems.initMaterials();
    }

    private static String raw(String aPath) throws IOException {
        try (InputStream tStream = GT6WireLootLaserTest.class.getClassLoader()
                .getResourceAsStream("data/gt6/loot_tables/blocks/" + aPath + ".json")) {
            assertNotNull(tStream, "the generated loot table must be on the classpath: " + aPath);
            return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject tree(String aPath) throws IOException {
        return JsonParser.parseString(raw(aPath)).getAsJsonObject();
    }

    /**
     * The family census: exactly ONE laser variant (upstream Loader:1814-1815 is a single
     * registration), un-insulated (the bare fiber), registry path {@code wire_laser} — which
     * is the vanilla default loot location {@code gt6:blocks/wire_laser} with ZERO block code.
     */
    @Test
    public void laserFamilyIsExactlyOneBlockOnTheWireLaserPath() {
        List<GTWireSpecs.Variant> tLaser = GTWireSpecs.laserVariants();
        assertEquals(1, tLaser.size(), "upstream registers exactly one Laser Fiber Wire (Loader:1814-1815)");
        assertEquals(GTWireSpecs.EXPECTED_LASER_VARIANTS, tLaser.size(), "the pinned census yardstick");
        GTWireSpecs.Variant tVariant = tLaser.get(0);
        assertEquals(LASER_PATH, GTWireSpecs.registryName(tVariant), "the loot path carrier: gt6:blocks/wire_laser");
        assertTrue(!tVariant.insulated(), "the bare fiber wire — no cable form upstream");
    }

    /**
     * The generated table is the dropSelf shape: one unconditional single-item pool over the
     * block's own item, no silk-touch/fortune dispatch, the vanilla survives_explosion
     * condition — breaking the placed block drops the block itself.
     */
    @Test
    public void generatedLaserLootIsTheSelfDropShape() throws IOException {
        JsonObject tLoot = tree(LASER_PATH);
        assertEquals("minecraft:block", tLoot.get("type").getAsString(), "the BLOCK param set table");
        assertEquals(1, tLoot.getAsJsonArray("pools").size(), "exactly one pool (dropSelf)");
        JsonObject tPool = tLoot.getAsJsonArray("pools").get(0).getAsJsonObject();
        assertEquals(1.0, tPool.get("rolls").getAsDouble(), "one roll");
        assertEquals(1, tPool.getAsJsonArray("entries").size(), "exactly one entry (dropSelf)");
        JsonObject tEntry = tPool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("minecraft:item", tEntry.get("type").getAsString());
        assertEquals("gt6:" + LASER_PATH, tEntry.get("name").getAsString(), "the block drops ITSELF");
        assertEquals(1, tPool.getAsJsonArray("conditions").size(), "no silk/fortune dispatch — one condition");
        assertEquals("minecraft:survives_explosion",
                tPool.getAsJsonArray("conditions").get(0).getAsJsonObject().get("condition").getAsString());
        assertEquals("gt6:blocks/" + LASER_PATH, tLoot.get("random_sequence").getAsString());
    }

    /**
     * The laser table is ISOMORPHIC to the E1 redstone precedent row: both fall out of the
     * same {@code GT6WireBlockLoot.generate()} dropSelf walk, so after swapping the two
     * name-bearing fields the trees are structurally identical.
     */
    @Test
    public void laserLootIsIsomorphicToTheRedstonePrecedentRow() throws IOException {
        JsonObject tLaser = tree(LASER_PATH);
        JsonObject tRedAlloy = tree("wire_red_alloy");
        assertEquals("gt6:wire_laser", tLaser.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        tLaser.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().addProperty("name", "gt6:wire_red_alloy");
        tLaser.addProperty("random_sequence", "gt6:blocks/wire_red_alloy");
        assertEquals(tRedAlloy, tLaser, "same provider, same dropSelf shape — only the identity differs");
    }

    /**
     * The E1 regression sentinel: all six redstone tables are still present and still
     * isomorphic to the laser table (the byte-level zero-diff is proven by git — this card
     * appends to the provider's walk, it never rewrites the existing families).
     */
    @Test
    public void redstonePrecedentRowsRemainPresentAndIsomorphic() throws IOException {
        for (String tPath : REDSTONE_PATHS) {
            JsonObject tRedstone = tree(tPath);
            assertEquals("gt6:" + tPath, tRedstone.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString(),
                    tPath + " still drops itself");
            assertEquals(normalized(LASER_PATH), normalized(tPath),
                    tPath + " and the laser table fall out of the same provider");
        }
    }

    /** The tree with the two identity fields masked out (entry name + random_sequence). */
    private static JsonObject normalized(String aPath) throws IOException {
        JsonObject tLoot = tree(aPath).deepCopy();
        tLoot.addProperty("random_sequence", "<id>");
        tLoot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().addProperty("name", "<id>");
        return tLoot;
    }
}
