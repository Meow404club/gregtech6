/**
 * Offline pin for task cover-item-icons-dual-source — the cover ITEM-icon domain swap.
 * Upstream draws cover item icons and installed cover overlays from TWO SEPARATE PNG
 * domains: the item icons are <code>items/gt.multiitem.technological/&lt;meta&gt;.png</code>
 * (MultiItemRandom.registerIcons, MultiItemRandom.java:361-368), the installed overlays
 * are the block-domain machines/covers/* set. The port had been feeding every cover item
 * the OVERLAY sprite as layer0 — the pump icon that painted the pump machine, the shutter
 * icon that painted the frameless door plate. The generated models now point every cover
 * item at its item-domain borrow (the 62-file sha256 census in assets/README.md), with
 * the declared exceptions riding their upstream sources (vanilla torch/repeater art, the
 * reused integrated-circuit borrows for the selector tags, the asphalt keep).
 * Reads the committed generated tree on the classpath (the GT6CircuitIconDatagenTest
 * form — no datagen run); the zero-overlay-token sweep is the census verdict this card
 * ships, so a future row cannot silently re-point a cover item at the block domain.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.covers.GT6Covers;
import gregtech6.covers.covers.CoverConveyor;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6CoverItemIconDatagenTest extends GTOfflineTestBase {

    /** The declared keeps: the only cover items allowed a gt6:block/ layer0 token. */
    private static final List<String> BLOCK_DOMAIN_KEEPS = List.of("cover_asphalt");

    /**
     * The 62-file borrow census — covers/&lt;name&gt;.png to the sha256 of the
     * byte-identical upstream borrow (assets/README.md cover-item-icons-dual-source
     * section; upstream items/gt.multiitem.technological/&lt;meta&gt;.png). A re-encode,
     * a wrong file, or a half-applied borrow breaks the digest here, not silently in game.
     */
    private static final String[][] BORROW_CENSUS = {
            {"pump", "12020", "bfce9706265a724c6121db8a06c1bc40bb5ac9431e59cb129288a0966156d7a6"},
            {"shutter", "1026", "365daaef78dea93560bf8eec0b2d71fa05515c51a2b160509aee3b140a4c859b"},
            {"item_filter", "1023", "9cdba56ac64441e1da9bad03b98b1e12977ccf5952a1fca5648b718fc274dcb3"},
            {"fluid_filter", "1024", "46eaf8bbd7af97f44fec13c490425cf2b741dac7d11630d32ce7137147afdcde"},
            {"item_retriever", "1031", "3b9344dfad13602a4eebd785804fb5bed51822abbcbcc3ff5eebddf51e0d42d5"},
            {"vent", "1022", "1a900f4d7210e53c4dd9cfd396937112d2030b3e597bca909dda8260cb9eb921"},
            {"drain", "1020", "de32b74f72aee720b2eee2c0078986ab5f020044978bf77431d34a417d824edf"},
            {"pressure_valve", "2000", "05ff5b14fe0040d9b4be9e06912e7cbc4e94710ebdb076c4125ff697cf4c0db1"},
            {"crafting", "1001", "37febcf980232ad0ebc5a7ec0d150979b7c5e15b1335eb8396d0b684a126b815"},
            {"redstone_emitter", "1021", "f29f37723c614aa3d5aa9a8e0820314ee82ca782684e1511d5d671ac4798f066"},
            {"redstone_conductor_in", "1029", "9b1ba4c18296b157b5dd3ff671824623e1bc41812a813ee0266b7ddd49a01024"},
            {"redstone_conductor_out", "1030", "139b6e096c1d48dd1de0d0abffee417061fcd8b262d9ca6ce3f5e910775c4efb"},
            {"redstone_machine_switch", "1005", "947f8d5b8283eba946cb2d9744450965ac3077118187189291d1d4f55d06a232"},
            {"auto_redstone_machine_switch", "1006", "96b763922f53a7edd80650e1c2d1c2a1fff165c5ef37014ac973dbe88c167b35"},
            {"controller", "1025", "df3a646b093a73d21b71f6286b6255f4a01258a1534b6d2589a4c5e6ea1e9ba1"},
            {"machine_display", "1002", "6a47b2115c9c06563ec7d5cc7cbf3981120839d05678babf831d835eff88567a"},
            {"auto_switch", "1003", "33bd8a91c7a4f072dfb01478a2e5aecc6120e00295b17b0d466ed446c238ef7e"},
            {"energy_display", "1004", "d53f329914ec7a99190816b6b108b2bded518a1241f0eaf74704d143d6508dba"},
            {"scale_energy", "1014", "84ab66f15c37a3be2e1643f15acab4035db095189e28bdfb9acc7e7d14ee7376"},
            {"scale_progress", "1018", "009a62d18b47edfc90475fd765c9cc7d9eef7f8c35c6dc9e4933bcbad0446922"},
            {"auto_timer_1m", "1009", "1777ff6277a5fefdd2fc178a73a047c10d6a910bf67f40a9e27769766b38ab39"},
            {"auto_timer_5m", "1010", "5dba09d662ef034e510c2743c981231a666c3a8e61c9d8631f9bffa5c8f3fa93"},
            {"auto_timer_10m", "1011", "c74c7e417404488d2efd9076872aed423bf0f867f1109844142a631b4aac8810"},
            {"auto_timer_20m", "1012", "562a997ff047e08466105627fac9014bd4f1d2a8b6d5883e10ad0efad8bfca5d"},
            {"auto_timer_30m", "1013", "806a2fe5b2d42212b8ff6dcb11252e92e1eff8dd45b0822fc62d85b2b769b1a2"},
            {"selector_redstone", "1007", "0938904af7bfb6221103a31995ab22099a93bce9d1e0fc450a942b6685ed177a"},
            {"selector_manual", "1008", "6f296422d71a1cbc2ebfe94e0f59e498d96000d92cf61d2a37967990e687ac73"},
            {"selector_button_panel", "1027", "1273cc10e796a53aa7cb52a30a7ab5d19ebab1d96af7a65fb47fa31469f3f538"},
            {"display_cpu_logic", "1086", "665360aca97d5a003b20dfae32ef495862fee6e0c317932ec68ba5313304477e"},
            {"display_cpu_control", "1087", "7d5ba87aa6882cffcd130db26a8936fe64dd0a1274293c60f17ebb6f38719240"},
            {"display_cpu_storage", "1088", "b8a007d0f4398dde65e037597ddb962af613f8b6b661d2317909f9f68207b3b0"},
            {"display_cpu_conversion", "1089", "d36662e023b9811943d89d5a8058bba558090c81c23ebe42c49a7e4a3e10091a"},
            {"fluid_export", "1090", "f4e40b41987ea2bb13c62069f4ed9464cc49d257c6c5e7f7842e6d3c56a5129a"},
            {"fluid_import", "1091", "34fef95e3cfeb7bab5235c2787648d3a249583a3d89fb5b81ab31254ef0e42e6"},
            {"fluid_storage", "1092", "a97cdc5dfb5b95f5a65342da936a3bc1b637e951ae60f4d5563529b6f8b9d13a"},
            {"item_export", "1093", "d028833dbff3a8f5b6154b2eb1f030fca54dab3f0611f61e43d1ac05d5f2fe0d"},
            {"item_import", "1094", "d1d3bf4112582c6c077a74abf621da2d5cc695f38abc68b70173e29e39cd2002"},
            {"item_storage", "1095", "6febff9e3b22cd10938e58414c891080e74f13bc6db1d402e68bf6a43038abdf"},
            {"generic_export", "1096", "2c86aada1d91d89de71bed815aba2854316b346f57290516cf3e2b99a2d4942c"},
            {"generic_import", "1097", "aacb64b37a0f46a0b25876c593f40bf485a7df2e988bccd422df8e92ddec994e"},
            {"generic_storage", "1098", "3cab1bd36133ca69f2ca7fc0a068893ada99c9878949f00c2f7e7ff6ee1ba6a2"},
            {"generic_dump", "1099", "8cbeb3651233d87e26efe335a9d83a5c524945c13374c73c2cf1303b38e08b67"},
            {"conveyor_0", "12040", "42f120ea55568a11aec255250210c15bdd0ce2743d9d45fb644f26ef27a45891"},
            {"conveyor_1", "12041", "48e7979ec6ca5dda7fd4ffe155420084cfff19b27b202acfed8265d7e3f09125"},
            {"conveyor_2", "12042", "815a6a858fd4cf04aec6ee0644d1c0cfb62c7f5e956096e7ab1d5fad0b60daeb"},
            {"conveyor_3", "12043", "ea07bf9859da92be6d6d69822d8dfe8f619c8c6f88c198a1a7eab2b2f226ef07"},
            {"conveyor_4", "12044", "a8f40341c0920c579dd3952382e385e64818cefc2fa80176167e724e0ece035f"},
            {"conveyor_5", "12045", "6cd734e3a45239cf4c88ba9c74527cc1f3963c787bca9941e17600f86e98dcac"},
            {"conveyor_6", "12046", "7eeacdd0a4f40ffaf8397acf906c51b04ec382d6a189013245ea1a5b9e8ab09c"},
            {"conveyor_7", "12047", "5eb9f5eb0bd30b1ca40b7737521835e4fbe8c7e877cd1f46475e4dc6fb4ae09f"},
            {"conveyor_8", "12048", "77001ccba0f32cfa18750a7e6eddca717f778505f177f4b956817d62fc2c8b80"},
            {"conveyor_9", "12049", "740fea29f87508ea73221ed288f394579766a1ce8a697d8d6b70a8ceb0ebdf4e"},
            {"robot_arm_0", "12080", "3655fc76ffbd640443e01bf49a6cf5c2c0be6bd240c6ebf72b9e337cb4071150"},
            {"robot_arm_1", "12081", "20ffabd4054defa1b5c4f6eb4cc018f85425c9b38e25aea679d75185d8b44552"},
            {"robot_arm_2", "12082", "e474626bfa1d4b15437f5b96f9bc586fc7642df50bee4bfdedbfc8dff81e8cff"},
            {"robot_arm_3", "12083", "3f31e6ee7123b679d2f39cc77ba3d7e52f1e5878269a3a6699a9dcf5e8153e8e"},
            {"robot_arm_4", "12084", "9bdc60bc8af1cd71b790e9bf33ee526e7f28a14b1f0418e7d8715f422acdb2a0"},
            {"robot_arm_5", "12085", "aaf4a604520c70c400f79f2161e1f2e6c41da4928a6f34bfa46644a4db54de9e"},
            {"robot_arm_6", "12086", "1b1faa85db66dc20a67afa8cd9f0e29c5aeef53576f5c5da2f1a367942a7fe4c"},
            {"robot_arm_7", "12087", "b3c1d66a74eb90aa461a034c3bf0d96330c4996de5588a38558f290daf6beef2"},
            {"robot_arm_8", "12088", "12282e9a818e6a43eae521bf905ef2e1fb1825a9c7dfbf4bc98060271b1d8639"},
            {"robot_arm_9", "12089", "6d2162aa2e4182927f583a793a224e83f63f8927e7a4af8d04ed50c973452664"}};

    private static InputStream read(String aPath) {
        InputStream tStream = GT6CoverItemIconDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
        assertNotNull(tStream, "the asset must be on the classpath: " + aPath);
        return tStream;
    }

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = read(aPath)) {
            return JsonParser.parseString(new String(tStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
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

    private static void assert2DForm(String aId, String aLayer0) throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/item/" + aId + ".json");
        assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                aId + ": the 2D item/generated parent");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals(1, tTextures.size(), aId + ": exactly the layer0 texture");
        assertEquals(aLayer0, tTextures.get("layer0").getAsString(), aId + ": the item-domain icon");
    }

    /** The 62-borrow byte census + the 16x16 PNG frame every upstream icon carries. */
    @Test
    void theBorrowCensusIsByteExactAndAllSixteenBySixteen() throws Exception {
        assertEquals(62, BORROW_CENSUS.length, "the census walk broke — never pass vacuously");
        for (String[] tRow : BORROW_CENSUS) {
            String tPath = "assets/gt6/textures/item/covers/" + tRow[0] + ".png";
            assertEquals(tRow[2], sha256(tPath),
                    tRow[0] + ": the byte-identical upstream borrow (" + tRow[1] + ".png) drifted");
            byte[] tHead;
            try (InputStream tStream = read(tPath)) {
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

    /**
     * The full cover item inventory: every item-domain row points at its borrow, the
     * declared exceptions ride their upstream sources, and — the census verdict — NO
     * cover item carries a gt6:block/ overlay-domain token on layer0 except the declared
     * asphalt keep (upstream its cover item IS the Asphalt Panel whose icon is the
     * asphalt art, Loader_MultiTileEntities.java:2053-2055).
     */
    @Test
    void everyCoverItemPointsAtItsItemDomainSource() throws Exception {
        List<String> tInventory = new ArrayList<>(List.of(
                "cover_pump", "cover_redstone_emitter",
                "cover_redstone_conductor_in", "cover_redstone_conductor_out",
                "cover_redstone_machine_switch", "cover_shutter", "cover_item_filter",
                "cover_item_retriever",
                "cover_logistics_display_cpu_logic", "cover_logistics_display_cpu_control",
                "cover_logistics_display_cpu_storage", "cover_logistics_display_cpu_conversion",
                "cover_logistics_fluid_export", "cover_logistics_fluid_import", "cover_logistics_fluid_storage",
                "cover_logistics_item_export", "cover_logistics_item_import", "cover_logistics_item_storage",
                "cover_logistics_generic_export", "cover_logistics_generic_import", "cover_logistics_generic_storage",
                "cover_logistics_generic_dump",
                "cover_vent", "cover_drain", "cover_pressure_valve", "cover_fluid_filter",
                "cover_selector_redstone", "cover_selector_manual", "cover_selector_button_panel",
                "cover_auto_redstone_machine_switch", "cover_controller",
                "cover_machine_display", "cover_auto_switch", "cover_energy_display",
                "cover_scale_energy", "cover_scale_progress", "cover_crafting"));
        tInventory.addAll(List.of(GT6Covers.AUTO_TIMER_IDS));
        for (String tId : tInventory) {
            // the borrow file names drop both the cover_ and the logistics_ infix
            // (cover_logistics_display_cpu_logic -> display_cpu_logic.png)
            String tArt = tId.substring("cover_".length()).replace("logistics_", "");
            assert2DForm(tId, "gt6:item/covers/" + tArt);
        }
        // the vanilla-source exceptions (upstream rides the vanilla items, GT_API.java:799-802)
        assert2DForm("cover_redstone_torch", "minecraft:block/redstone_torch");
        assert2DForm("cover_redstone_repeater", "minecraft:block/repeater");
        // the tag-selector ladder reuses the circuit-config borrows (upstream the covers
        // ride the Integrated Circuit item damage 0-15, ItemIntegratedCircuit.java:87)
        for (int i = 0; i < 16; i++) {
            assert2DForm("cover_selector_tag_" + i, "gt6:item/integrated_circuit/" + i);
        }
        // the tier ladders: upstream gives EVERY tier its own item icon (the 12040+i /
        // 12080+i PNGs are per-tier distinct), walked over the port's tier table
        assertTrue(CoverConveyor.TIMING_TIERS.length == 10,
                "the upstream tier walk is the 10-meta ladder (MultiItemTechnological.java:51/53)");
        for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
            assert2DForm("cover_conveyor_" + i, "gt6:item/covers/conveyor_" + i);
            assert2DForm("cover_robot_arm_" + i, "gt6:item/covers/robot_arm_" + i);
        }
    }

    /** The zero-overlay-token census: no cover item layer0 re-enters the block domain. */
    @Test
    void noCoverItemLayer0CarriesAnOverlayDomainToken() throws Exception {
        List<String> tIds = new ArrayList<>(List.of(
                "cover_pump", "cover_redstone_emitter",
                "cover_redstone_conductor_in", "cover_redstone_conductor_out",
                "cover_redstone_machine_switch", "cover_shutter", "cover_item_filter",
                "cover_item_retriever", "cover_redstone_torch", "cover_redstone_repeater",
                "cover_vent", "cover_drain", "cover_pressure_valve", "cover_fluid_filter",
                "cover_selector_redstone", "cover_selector_manual", "cover_selector_button_panel",
                "cover_auto_redstone_machine_switch", "cover_controller",
                "cover_machine_display", "cover_auto_switch", "cover_energy_display",
                "cover_scale_energy", "cover_scale_progress", "cover_crafting", "cover_asphalt",
                "cover_logistics_display_cpu_logic", "cover_logistics_display_cpu_control",
                "cover_logistics_display_cpu_storage", "cover_logistics_display_cpu_conversion",
                "cover_logistics_fluid_export", "cover_logistics_fluid_import", "cover_logistics_fluid_storage",
                "cover_logistics_item_export", "cover_logistics_item_import", "cover_logistics_item_storage",
                "cover_logistics_generic_export", "cover_logistics_generic_import", "cover_logistics_generic_storage",
                "cover_logistics_generic_dump"));
        tIds.addAll(List.of(GT6Covers.AUTO_TIMER_IDS));
        for (int i = 0; i < 16; i++) tIds.add("cover_selector_tag_" + i);
        for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
            tIds.add("cover_conveyor_" + i);
            tIds.add("cover_robot_arm_" + i);
        }
        assertEquals(81, tIds.size(), "the 81-item cover inventory — a missing row is a hole in the census");
        for (String tId : tIds) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tId + ".json");
            String tLayer0 = tModel.getAsJsonObject("textures").get("layer0").getAsString();
            if (BLOCK_DOMAIN_KEEPS.contains(tId)) continue;
            assertFalse(tLayer0.startsWith("gt6:block/"),
                    tId + ": the item icon must NOT re-enter the overlay domain (got " + tLayer0 + ")");
        }
    }
}
