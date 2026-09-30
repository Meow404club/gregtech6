/**
 * Offline pin for task circuit-config-icons — the two Selector Tag icon miswires:
 * <ol>
 * <li>the 25 config variants (configs 0-24, upstream ItemIntegratedCircuit.java:90-118)
 *     all rode the config-0 art. The generated {@code integrated_circuit.json} now
 *     carries the 25-entry override ladder (descending 24-0 — ItemOverrides.resolve
 *     takes the FIRST entry whose predicate value is &lt;= the property value) over 25
 *     per-config variant models, each pointing at its own borrowed upstream PNG
 *     ({@code gt.integrated_circuit/0-24.png}, assets/README.md);</li>
 * <li>the 7 battery-closure circuit carriers (GT6Batteries.CIRCUIT_ROWS) all rode the
 *     Selector Tag icon. Each now shows its upstream tier art — the MultiItemTechnological
 *     30301-30306 borrows; tier 0 has no upstream item of its own (IL.Circuit_Primitive is
 *     never set, LoaderOreDictReRegistrations.java:375 chains OD_CIRCUITS[1] items under
 *     OD_CIRCUITS[0]), so {@code primitive} shares the 30301 bytes with {@code basic},
 *     the declared upstream ground.</li>
 * </ol>
 * Reads the committed generated tree on the classpath (the
 * {@link GT6MoldAssetDatagenTest} form — no datagen run) and pins the full sha256
 * census of the 32 borrows so a re-encode or a missed rung goes red.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Batteries;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6CircuitIconDatagenTest extends GTOfflineTestBase {

    /** The override predicate the client property registers (GTClientHandlers.onClientSetup). */
    private static final String PREDICATE = "gt6:config";
    private static final int CONFIGS = 25;
    private static final String FLAT_RETIREMENT = "assets/gt6/textures/item/integrated_circuit.png";

    /**
     * The full borrow census — path (under assets/gt6/textures/item/) to the sha256 of the
     * byte-identical upstream borrow (assets/README.md circuit-config-icons section; the
     * config-0 row lives in the distillery-family section). A re-encode, a wrong file, or a
     * half-applied borrow breaks the digest here, not silently in game.
     */
    private static final String[][] BORROW_CENSUS = {
            {"integrated_circuit/0.png", "ce72e7832572432152196b3f3a96bc0efd5ed9da41878b436e9022d0090837f2"},
            {"integrated_circuit/1.png", "574c8324ee1b39ae6f313557a68429afb84f96b65331c6a38dac4105a7f3345d"},
            {"integrated_circuit/2.png", "300b5f03cd068660c7ca12110ee09f85636357b0d79f79477354094d51c5764a"},
            {"integrated_circuit/3.png", "852b4dfafda99cf0c3c212d52dad684a1cfb160e963babe86bbb26c7c525dd5e"},
            {"integrated_circuit/4.png", "112c89c54455a30b62ac730f5c3f554001588ae7efe4cc56ebd3c31cde802537"},
            {"integrated_circuit/5.png", "71c016474b196dd82bd06b3c94cd86b2162f566942c63b29fbe6a19b2ba7c268"},
            {"integrated_circuit/6.png", "20fda0bf438d84d04e19cf2cc008c65fa3d8dc9363f6b83f63384878081ec42b"},
            {"integrated_circuit/7.png", "0cfa533fa5ff4e30ad69a8677452c823cd24ecf66f1e7a82d9ce5b6ae20c25ab"},
            {"integrated_circuit/8.png", "6bc8c7a76c296db48788cc848665bd8cb03314bf19cf19eb8fae25f6561f601e"},
            {"integrated_circuit/9.png", "8bad35bb6726aa5c5bc8c38e001d847f836f7745f03eb6cea9aefbad11a47939"},
            {"integrated_circuit/10.png", "c49747da1f6c4f6fe83934071b84bf0a8df041b01c6d2bb125e6cc27afc3a354"},
            {"integrated_circuit/11.png", "b3075a79d2a2b0db3198343fd4cb54d1fc00f4de4e68fbc4a0fe616dd06f487d"},
            {"integrated_circuit/12.png", "5a4845d37a16411c695b6a946bec13e82d32b3e77f2f842ecc40b48ade0f146b"},
            {"integrated_circuit/13.png", "04a64971ad8fd228857fd11573b610f1ea8a696da7df1ca4315992f72934a45f"},
            {"integrated_circuit/14.png", "8b49d3b8286b9e61fbf6fb01bc02d9c9b3fcea6adba1e5532efe3189d1f1d50b"},
            {"integrated_circuit/15.png", "44c814176d7691e28a81a7b8328dc2d889c9e7b0df12597890b42d754ef09587"},
            {"integrated_circuit/16.png", "beffb8601ee31f61e475679764d45e2a3f1838a984904a948a8344e5d097ec37"},
            {"integrated_circuit/17.png", "79380bcdcc584b9f664f2bb98a296763a50062fb8d4fcad734e8d8f081be8ebe"},
            {"integrated_circuit/18.png", "89bb665c5189f326a9da87556c57a9d770a0aefcc87262df266a117b75751eba"},
            {"integrated_circuit/19.png", "303c319c4e22bba49a28f15e1877d03ba98e599d135b5f234d74c3935e61beb7"},
            {"integrated_circuit/20.png", "ec6a0b02c57799d646c2a5a036d33f58347c499a604159b3b6f54cf77b107a23"},
            {"integrated_circuit/21.png", "c9d1bcd3410545bbba8d9b29cce3c86348553fe933294ba0c23cb9a881d06dbf"},
            {"integrated_circuit/22.png", "8eb4c56a2910858378ca8dc679e7101442f26205794fcd9d3455e693741cd9ac"},
            {"integrated_circuit/23.png", "a8b7851ab969f9905b14e19387a4f80db65c0e7ebbbb631424129621a835faa2"},
            {"integrated_circuit/24.png", "024bc31fa2864664eeac7fa306e1ea55b6970fdf691c571d3c1c0b96adf3dcad"},
            {"circuit/primitive.png", "0d5f644597f3f6d89b45a9c271edac5a18aa0054b2acb14cb3b38c352968f4a9"},
            {"circuit/basic.png", "0d5f644597f3f6d89b45a9c271edac5a18aa0054b2acb14cb3b38c352968f4a9"},
            {"circuit/good.png", "0cacca0562f14152435f5e0a5c6c21ad26a8b20827541772d2b521230e1cd590"},
            {"circuit/advanced.png", "6bbd7e90beb8a6dd80be5bcaa5322c0d50525e70e618d7fdac50f9f89103eafe"},
            {"circuit/elite.png", "612fb8b0599c44b2d713bfa6982ad01ccc784e73d24c00a7d368774af062732b"},
            {"circuit/master.png", "b06850446db17761966dccf1f3d44729b201a890be515ad39ef3c7f03d2a58f3"},
            {"circuit/ultimate.png", "9e6986071e38698b7226aa7e64d0c27e70e532a2fed0cfedbedd0a1ffdbec7c9"}};

    private static InputStream read(String aPath) {
        InputStream tStream = GT6CircuitIconDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
        assertNotNull(tStream, "the asset must be on the classpath: " + aPath);
        return tStream;
    }

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = read(aPath)) {
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static String sha256(String aPath) throws Exception {
        try (InputStream tStream = read(aPath)) {
            MessageDigest tDigest = MessageDigest.getInstance("SHA-256");
            byte[] tBuffer = new byte[8192];
            int tRead;
            while ((tRead = tStream.read(tBuffer)) > 0) tDigest.update(tBuffer, 0, tRead);
            StringBuilder tHex = new StringBuilder();
            for (byte tByte : tDigest.digest()) tHex.append(String.format("%02x", tByte));
            return tHex.toString();
        }
    }

    /** The 32-borrow byte census + the 16x16 frame every upstream icon carries. */
    @Test
    void theBorrowCensusIsByteExactAndAllSixteenBySixteen() throws Exception {
        assertEquals(32, BORROW_CENSUS.length, "the census walk broke — never pass vacuously");
        for (String[] tRow : BORROW_CENSUS) {
            assertEquals(tRow[1], sha256("assets/gt6/textures/item/" + tRow[0]),
                    tRow[0] + ": the byte-identical upstream borrow drifted");
            byte[] tHead;
            try (InputStream tStream = read("assets/gt6/textures/item/" + tRow[0])) {
                tHead = new byte[24];
                assertEquals(24, tStream.readNBytes(tHead, 0, 24), tRow[0] + ": the PNG header window");
            }
            assertTrue(java.util.Arrays.equals(java.util.Arrays.copyOfRange(tHead, 0, 8),
                            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
                    tRow[0] + ": not a PNG");
            int tWidth = ((tHead[16] & 0xFF) << 24) | ((tHead[17] & 0xFF) << 16) | ((tHead[18] & 0xFF) << 8) | (tHead[19] & 0xFF);
            int tHeight = ((tHead[20] & 0xFF) << 24) | ((tHead[21] & 0xFF) << 16) | ((tHead[22] & 0xFF) << 8) | (tHead[23] & 0xFF);
            assertEquals(16, tWidth, tRow[0] + ": the upstream 16x16 frame");
            assertEquals(16, tHeight, tRow[0] + ": the upstream 16x16 frame");
        }
    }

    /** The flat placeholder is retired — nothing may reference the moved file again. */
    @Test
    void theFlatPlaceholderStaysRetired() {
        assertNull(GT6CircuitIconDatagenTest.class.getClassLoader().getResource(FLAT_RETIREMENT),
                "the flat integrated_circuit.png must stay deleted (moved to integrated_circuit/0.png)");
    }

    /**
     * The base model: item/generated over the config-0 borrow, plus the descending
     * 24-0 override ladder keyed on gt6:config — a missed rung is the miswire this
     * task fixes, so every one of the 25 entries is pinned in place and in order.
     */
    @Test
    void theConfigLadderCoversAll25ConfigsInDescendingOrder() throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/item/integrated_circuit.json");
        assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                "integrated_circuit: the vanilla item/generated parent");
        assertEquals("gt6:item/integrated_circuit/0", tModel.getAsJsonObject("textures").get("layer0").getAsString(),
                "integrated_circuit: the base face is the config-0 borrow");
        assertTrue(tModel.has("overrides"), "integrated_circuit: the override ladder is the fix");
        JsonArray tOverrides = tModel.getAsJsonArray("overrides");
        assertEquals(CONFIGS, tOverrides.size(), "integrated_circuit: all 25 rungs — a missing config is the miswire");
        for (int tI = 0; tI < CONFIGS; tI++) {
            int tConfig = CONFIGS - 1 - tI; // descending: the FIRST predicate <= value wins
            JsonObject tEntry = tOverrides.get(tI).getAsJsonObject();
            JsonObject tPredicates = tEntry.getAsJsonObject("predicate");
            assertEquals(1, tPredicates.size(), "rung " + tI + ": exactly the gt6:config predicate");
            assertTrue(tPredicates.has(PREDICATE), "rung " + tI + ": the gt6:config key");
            assertEquals((float) tConfig, tPredicates.get(PREDICATE).getAsFloat(),
                    "rung " + tI + ": descending order, expected config " + tConfig);
            assertEquals("gt6:item/integrated_circuit/config_" + tConfig, tEntry.get("model").getAsString(),
                    "rung " + tI + ": the per-config variant model");
        }
    }

    /** Each of the 25 variant models points at its own config art; the art is the borrowed PNG. */
    @Test
    void everyConfigVariantModelPointsAtItsOwnConfigArt() throws Exception {
        Set<String> tLayer0s = new HashSet<>();
        for (int tI = 0; tI < CONFIGS; tI++) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/integrated_circuit/config_" + tI + ".json");
            assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                    "config_" + tI + ": the vanilla item/generated parent");
            String tLayer0 = tModel.getAsJsonObject("textures").get("layer0").getAsString();
            assertEquals("gt6:item/integrated_circuit/" + tI, tLayer0,
                    "config_" + tI + ": its own borrowed config art");
            tLayer0s.add(tLayer0);
        }
        assertEquals(CONFIGS, tLayer0s.size(), "each config must point at a DISTINCT art (upstream :118 registers 25)");
    }

    /** The 7 carriers: each its upstream tier art, walked over the registration rows. */
    @Test
    void everyCarrierShowsItsUpstreamTierArt() throws Exception {
        assertEquals(7, GT6Batteries.CIRCUIT_ROWS.size(), "the carrier walk broke — never pass vacuously");
        List<String> tSuffixes = new ArrayList<>();
        for (GT6Batteries.CircuitRow tRow : GT6Batteries.CIRCUIT_ROWS) {
            String tSuffix = tRow.path().substring("circuit_".length());
            tSuffixes.add(tSuffix);
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                    tRow.path() + ": the vanilla item/generated parent");
            String tLayer0 = tModel.getAsJsonObject("textures").get("layer0").getAsString();
            assertEquals("gt6:item/circuit/" + tSuffix, tLayer0,
                    tRow.path() + ": its upstream tier art, not the Selector Tag borrow");
            assertEquals(tRow.tier(), tSuffixes.size() - 1, tRow.path() + ": the tier ladder rides the row order");
        }
        assertEquals(List.of("primitive", "basic", "good", "advanced", "elite", "master", "ultimate"),
                tSuffixes, "the carrier universe drifted — re-pin deliberately");
    }
}
