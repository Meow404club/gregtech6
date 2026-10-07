/**
 * Tests for task material-mc-g3-plank-panels: the GT6 WOODEN Cover Panel census (28
 * per-row Block+BlockItem pairs over the plank authority walk), the upstream PlankData
 * slot mapping, the id scheme, the MTE-default numbers + the no-flammability negative,
 * the family name face (NO wood word — the dump face), the tooltip identity face, and
 * the generated-tree pins (blockstates/item models riding the existing plank models /
 * loot / recipes / lang).
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_MultiTileEntities.java:2057-2083 — the three 100-loops, "Wooden Panel",
 *     MultiTileEntityPanelWood, {@code 0, 16, aWooden}, {@code NBT_TEXTURE=i},
 *     {@code NBT_HIDDEN=ST.invalid(PlankData.PLANKS[i])} — 300 MTE rows over texture
 *     indices 0-299 (:2058/:2067/:2076).</li>
 * <li>PlankEntry.java:115-121 — the slots fill at runtime from the registered planks
 *     ({@code PLANK_ENTRIES[i] == null} gate, first-come); the invalid slots' rows are
 *     hidden + recipe-less (Loader :2059/:2068/:2077 {@code if (ST.valid(...))}).</li>
 * <li>LoaderWoodDictionary.java:51-56/:66-172 — the vanilla slots 0-5 and the GT6 ladder
 *     (Rubber 6, Maple 7, Willow 37, BlueMahoe 38, Hazel 39, Compressed 54, DEFAULT_PLANK
 *     55, Treated 62, Crate 63, Cinnamon 97, Coconut 98, Rainbowood 99, Dead 100, Rotten
 *     101, Mossy 102, Frozen 103, BlueSpruce 239); the FireProof twins collide on slot 0
 *     and hold none.</li>
 * <li>PlankEntry.java:120 — the texture leg: {@code IconContainerCopied(ST.block(mPlank),
 *     ST.meta_(mPlank))} — a panel COPIES its plank's texture (the port points the
 *     blockstate at the existing plank model, zero new PNG).</li>
 * <li>tmp/gregtech.lang:13133/:13233/:13381 — the dump faces 木制覆盖板: NO wood word,
 *     one name for all 300 rows.</li>
 * <li>MultiTileEntityPanelWood.java:48-52 — the wood-identity tooltip (the plank display
 *     name, LH.Chat.CYAN = the modern AQUA).</li>
 * <li>MultiTileEntityPanel.java:38 — canPlace=F: the placement face is the declared
 *     deviation (the G1 GT6PanelBlock set).</li>
 * <li>CR.java:211/:229 — the 's' saw / 'd' screwdriver tool letters of the panel recipe
 *     pattern "TsT"/"TPT"/"TdT" (result 6x, Loader :2060/:2069/:2078).</li>
 * <li>TileEntityBase07Paintable.java:51 — the MTE defaults the rows inherit (no
 *     NBT_HARDNESS/NBT_RESISTANCE on the panel rows): hardness 1.0, resistance 3.0;
 *     NO NBT_FLAMMABILITY either — the panel does NOT burn (the negative ledger).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GT6PanelsRegistryTest shape): the registry class
 * initialises offline (DeferredRegister accumulates suppliers); the block ctor is driven
 * directly past the bootstrap+unfreeze bracket.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.panels.GT6PlankPanelBlock;

class GT6PlankPanelsRegistryTest {

    /** The 28-row census — the port plank face (GT6WoodDict.ROWS 1:1): 17 GT6 rows + 11 vanilla rows. */
    private static final int PINNED_TOTAL = 28;
    /** The 23 rows that fill a real upstream PlankData slot (300 − 277 unfilled — the negative ledger). */
    private static final int PINNED_FILLED = 23;

    @BeforeAll
    static void initOfflineJVM() {
        // the GT6ConcreteRegistryTest posture: bootstrap vanilla, then unfreeze the block
        // registry so the direct block ctor works self-sufficiently
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Throwable aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
        // MT.init BEFORE the first rows() touch — the dict clinit captures MT.WOODS.* and
        // the capture is JVM-WIDE (the same construct-order trap the registration class's
        // clinit discipline dodges in production; the GT6PlankRegistrationTest posture —
        // a dict touch on an un-inited material table poisons every later suite here)
        GTMaterialItems.initMaterials();
    }

    /** The authority walk: 28 rows 1:1 over GT6WoodDict.ROWS, same order, same plank ids. */
    @Test
    void rowsWalkThePlankAuthorityOneToOne() {
        assertEquals(PINNED_TOTAL, GT6PlankPanels.rows().size(), "the walk covers the whole authority");
        assertEquals(GT6WoodDict.ROWS.size(), GT6PlankPanels.rows().size(), "the authority size");
        for (int i = 0; i < GT6PlankPanels.rows().size(); i++) {
            assertEquals(GT6WoodDict.ROWS.get(i).id(), GT6PlankPanels.rows().get(i).spec().plankId(),
                    "same plank id at walk index " + i);
            assertEquals(GT6WoodDict.ROWS.get(i).plank(), GT6PlankPanels.rows().get(i).plankEntry().plank(),
                    "same plank supplier at walk index " + i);
        }
        assertEquals(PINNED_TOTAL, GT6PlankPanels.BLOCKS.size(), "28 blocks");
        assertEquals(PINNED_TOTAL, GT6PlankPanels.ITEMS.size(), "28 block items");
    }

    /**
     * The upstream PlankData slot map (LoaderWoodDictionary.java:51-56/:66-172 verbatim):
     * the 23 filled slots exact, the 5 port-native vanilla-tree rows slot-less, the band
     * arithmetic 300 − 23 = 277 unfilled (pinned, NOT fabricated as blocks).
     */
    @Test
    void upstreamSlotMapIsVerbatim() {
        assertEquals(PINNED_FILLED, GT6PlankPanels.FILLED_SLOTS, "the filled-slot count");
        assertEquals(300, GT6PlankPanels.UPSTREAM_BAND, "the upstream band (3 x 100, Loader :2057-2083)");
        // the vanilla six (:51-56) + the GT6 ladder (:66-172), the Loader row order verbatim
        record Pin(String plankId, int slot) {}
        List<Pin> tPinned = List.of(
            new Pin("minecraft:oak_planks", 0), new Pin("minecraft:spruce_planks", 1),
            new Pin("minecraft:birch_planks", 2), new Pin("minecraft:jungle_planks", 3),
            new Pin("minecraft:acacia_planks", 4), new Pin("minecraft:dark_oak_planks", 5),
            new Pin("rubber_planks", 6), new Pin("maple_planks", 7),
            new Pin("plank_wood_compressed", 54), new Pin("plank_wood", 55),
            new Pin("plank_wood_treated", 62), new Pin("crate", 63),
            new Pin("willow_planks", 37), new Pin("blue_mahoe_planks", 38), new Pin("hazel_planks", 39),
            new Pin("cinnamon_planks", 97), new Pin("coconut_planks", 98), new Pin("rainbowood_planks", 99),
            new Pin("plank_wood_dead", 100), new Pin("plank_wood_rotten", 101),
            new Pin("plank_wood_mossy", 102), new Pin("plank_wood_frozen", 103),
            new Pin("blue_spruce_planks", 239));
        Set<Integer> tSlots = new HashSet<>();
        for (Pin tPin : tPinned) {
            gregtech6.registry.GT6PlankPanels.PanelRow tRow = rowByPlankId(tPin.plankId());
            assertEquals(tPin.slot(), tRow.spec().upstreamSlot(), "the pinned slot of " + tPin.plankId());
            assertTrue(tSlots.add(tPin.slot()), "distinct upstream slots: " + tPin.slot());
        }
        assertEquals(PINNED_FILLED, tSlots.size());
        // the 1.20.1 vanilla tree set: NO upstream slot (1.7.10 predates them — the
        // GT6WoodDict identity-rule ride, the declared port-native face)
        for (String tId : new String[] {"minecraft:mangrove_planks", "minecraft:cherry_planks",
                "minecraft:bamboo_planks", "minecraft:crimson_planks", "minecraft:warped_planks"}) {
            assertEquals(-1, rowByPlankId(tId).spec().upstreamSlot(), "no upstream slot: " + tId);
        }
    }

    private static gregtech6.registry.GT6PlankPanels.PanelRow rowByPlankId(String aPlankId) {
        return GT6PlankPanels.rows().stream().filter(aRow -> aRow.spec().plankId().equals(aPlankId))
                .findFirst().orElseThrow(() -> new AssertionError("missing row: " + aPlankId));
    }

    /** The id scheme: {@code wooden_panel_<wood>}, 28 distinct paths; the item band covers exactly the block paths. */
    @Test
    void perRowIdSchemeIsPinned() {
        assertEquals("wooden_panel_oak", GT6PlankPanels.path("oak"));
        assertEquals("wooden_panel_rubber", GT6PlankPanels.path("rubber"));
        assertEquals("wooden_panel_compressed_wood", GT6PlankPanels.path("compressed_wood"));
        assertEquals("wooden_panel_warped", GT6PlankPanels.path("warped"));
        Set<String> tPaths = new HashSet<>();
        for (gregtech6.registry.GT6PlankPanels.PanelRow tRow : GT6PlankPanels.rows()) {
            assertTrue(tPaths.add(tRow.path()), "distinct ids: " + tRow.path());
        }
        assertEquals(PINNED_TOTAL, tPaths.size(), "28 distinct registry paths");
        Set<String> tItemIds = new HashSet<>();
        for (var tEntry : GT6PlankPanels.ITEMS_REG.getEntries()) {
            tItemIds.add(tEntry.getId().getPath());
        }
        assertEquals(tPaths, tItemIds, "the item band covers exactly the 28 block paths");
    }

    /**
     * The MTE-default numbers (TileEntityBase07Paintable.java:51 — the rows carry no
     * NBT_HARDNESS/NBT_RESISTANCE): 1.0/3.0, aWooden → WOOD sound, no state properties.
     * The NEGATIVE ledger: NO flammability (the rows carry no NBT_FLAMMABILITY — the plank
     * family's BlockBasePlanksFlammable 20/5 face does NOT ride along).
     */
    @Test
    void blockCarriesTheMteDefaultsAndTheNoFlammabilityNegative() {
        GT6PlankPanelBlock tPanel = new GT6PlankPanelBlock("wooden_panel_oak",
                GT6WoodDict.VANILLA_ROWS.get(0).plank());
        assertTrue(tPanel.defaultBlockState().getProperties().isEmpty(), "the pure-block form");
        assertEquals(1.0F, tPanel.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "hardness = the MTE default 1.0 (TileEntityBase07Paintable.java:51)");
        assertEquals(3.0F, tPanel.getExplosionResistance(), 1.0e-6F,
                "resistance = the MTE default 3.0 (TileEntityBase07Paintable.java:51)");
        assertEquals(net.minecraft.world.level.block.SoundType.WOOD, tPanel.defaultBlockState().getSoundType(),
                "aWooden → the WOOD sound face");
        // the negative: upstream declares no flammability on the panel rows (the Rope rows
        // :2087-2092 declare NBT_FLAMMABILITY explicitly, the panels do not) — the port
        // block stays non-flammable, the plank 20/5 face does NOT ride along
        assertEquals(0, tPanel.getFlammability(tPanel.defaultBlockState(), null, null, net.minecraft.core.Direction.UP),
                "NOT flammable — the no-NBT_FLAMMABILITY negative");
        assertEquals(0, tPanel.getFireSpreadSpeed(tPanel.defaultBlockState(), null, null, net.minecraft.core.Direction.UP),
                "no fire spread — the same negative");
    }

    /**
     * The family name face: the dump carries NO wood word — ONE atomic key (no args, no
     * per-row keys; the dump face tmp/gregtech.lang:13381 band).
     */
    @Test
    void familyNameFaceIsAtomic() {
        GT6PlankPanelBlock tPanel = new GT6PlankPanelBlock("wooden_panel_oak",
                GT6WoodDict.VANILLA_ROWS.get(0).plank());
        net.minecraft.network.chat.contents.TranslatableContents tContents =
                (net.minecraft.network.chat.contents.TranslatableContents) tPanel.getName().getContents();
        assertEquals("gt6.panel.wood", tContents.getKey(), "the family key");
        assertEquals(0, tContents.getArgs().length, "atomic — no wood slot (the dump face)");
        assertEquals("gt6.panel.wood", GT6PlankPanelBlock.NAME_KEY, "the constant");
    }

    /** The row slugs: the wood segments of the 28 ids, the ROW_TABLE order verbatim (the walk cannot drift). */
    @Test
    void rowTableCoversTheAuthorityExactly() {
        assertEquals(PINNED_TOTAL, GT6PlankPanels.ROW_TABLE.size(), "the table is the walk");
        Set<String> tSlugs = new HashSet<>();
        for (gregtech6.registry.GT6PlankPanels.RowSpec tSpec : GT6PlankPanels.ROW_TABLE) {
            assertTrue(tSlugs.add(tSpec.slug()), "distinct slugs: " + tSpec.slug());
        }
        assertEquals(PINNED_TOTAL, tSlugs.size());
    }

    // ------------------------------------------------------------------ the generated-tree pins

    /** The mdk root (the GT6PanelsRegistryTest walk-up). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (p.resolve("src/main/resources/gregtech6/lang/zh_cn_ref.tsv").toFile().exists()
                    && p.resolve("mdk").toFile().exists()) {
                return p.resolve("mdk");
            }
            if (p.resolve("src/generated/resources").toFile().exists()) {
                return p;
            }
        }
        throw new IllegalStateException("mdk root not found from " + Path.of("").toAbsolutePath());
    }

    private static Path generated() {
        return mdkRoot().resolve("src/generated/resources");
    }

    /** The Gson tree of a generated JSON file — all roots are objects. */
    private static com.google.gson.JsonObject json(Path aFile) throws Exception {
        return com.google.gson.JsonParser.parseString(Files.readString(aFile)).getAsJsonObject();
    }

    /**
     * The tree pin: 28 blockstates + 28 item models + 28 loot tables; the blockstate
     * REFERENCES the existing plank model (the IconContainerCopied face — the reference IS
     * the upstream semantic) and NO new panel model file exists (zero-PNG posture).
     */
    @Test
    void generatedTreeCarriesThePanelFaceOverThePlankModels() throws Exception {
        Path tGen = generated();
        for (gregtech6.registry.GT6PlankPanels.PanelRow tRow : GT6PlankPanels.rows()) {
            String tPath = tRow.path();
            var tBlockstate = json(tGen.resolve("assets/gt6/blockstates/" + tPath + ".json"));
            var tVariantValue = tBlockstate.getAsJsonObject("variants").entrySet().iterator().next().getValue();
            com.google.gson.JsonObject tVariant = tVariantValue.isJsonArray()
                    ? tVariantValue.getAsJsonArray().get(0).getAsJsonObject() : tVariantValue.getAsJsonObject();
            String tModelRef = tVariant.get("model").getAsString();
            String tPlankId = tRow.spec().plankId();
            String tExpected = tPlankId.startsWith("minecraft:")
                    ? "minecraft:block/" + tPlankId.substring("minecraft:".length())
                    : "gt6:block/" + tPlankId;
            assertEquals(tExpected, tModelRef, "the blockstate rides the plank cube on " + tPath);
            var tItemModel = json(tGen.resolve("assets/gt6/models/item/" + tPath + ".json"));
            assertEquals(tExpected, tItemModel.get("parent").getAsString(),
                    "the item model parents the plank cube on " + tPath);
            assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tPath + ".json")),
                    tPath + " loot");
        }
        // the zero-model posture: no dedicated wooden_panel BLOCK model file exists (the
        // item models parent the plank cubes and live under models/item — out of scope)
        try (var tStream = Files.walk(tGen.resolve("assets/gt6/models/block"))) {
            assertTrue(tStream.noneMatch(aPath -> aPath.getFileName().toString().startsWith("wooden_panel")),
                    "no dedicated panel block models — the plank-reference face (a copy would fork the plank art)");
        }
    }

    /**
     * The recipe band: 28 JSONs, the upstream pattern "TsT"/"TPT"/"TdT" → 6x, iron screw +
     * saw + screwdriver; 'P' = the row's OWN plank (the vanilla rows ride the vanilla
     * planks, the GT6 rows the GT6 plank items — the ST.valid face is all-true in the
     * port world).
     */
    @Test
    void recipeBandIs28WithThePerRowPlankInput() throws Exception {
        Path tRecipes = generated().resolve("data/gt6/recipes");
        for (gregtech6.registry.GT6PlankPanels.PanelRow tRow : GT6PlankPanels.rows()) {
            String tPath = tRow.path() + ".json";
            var tRecipe = json(tRecipes.resolve(tPath));
            var tPattern = tRecipe.getAsJsonArray("pattern");
            assertEquals("TsT", tPattern.get(0).getAsString(), "the upstream row 1 on " + tPath);
            assertEquals("TPT", tPattern.get(1).getAsString(), "the upstream row 2 on " + tPath);
            assertEquals("TdT", tPattern.get(2).getAsString(), "the upstream row 3 on " + tPath);
            assertEquals("gt6:screw_iron", tRecipe.getAsJsonObject("key").getAsJsonObject("T").get("item").getAsString(),
                    "the ANY.Iron->iron screw fold on " + tPath);
            assertEquals("gt6:tools/saw", tRecipe.getAsJsonObject("key").getAsJsonObject("s").get("tag").getAsString(),
                    "the 's' saw letter (CR.java:211) on " + tPath);
            assertEquals("gt6:tools/screwdriver",
                    tRecipe.getAsJsonObject("key").getAsJsonObject("d").get("tag").getAsString(),
                    "the 'd' screwdriver letter (CR.java:229) on " + tPath);
            var tResult = tRecipe.getAsJsonObject("result");
            assertEquals(6, tResult.get("count").getAsInt(), "the 6x output on " + tPath);
            assertEquals("gt6:" + tRow.path(), tResult.get("item").getAsString(),
                    "the result-path id on " + tPath);
            String tInput = tRecipe.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString();
            String tPlankId = tRow.spec().plankId();
            assertEquals(tPlankId.startsWith("minecraft:") ? tPlankId : "gt6:" + tPlankId, tInput,
                    "the per-row plank source on " + tPath);
        }
    }

    /** The loot pin: the panels self-drop (the MTE default face). */
    @Test
    void lootFacesAreSelfDrop() throws Exception {
        var tOak = json(generated().resolve("data/gt6/loot_tables/blocks/wooden_panel_oak.json"));
        assertEquals("gt6:wooden_panel_oak",
                tOak.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                        .getAsJsonObject().get("name").getAsString(), "the self-drop");
        var tCrate = json(generated().resolve("data/gt6/loot_tables/blocks/wooden_panel_crate.json"));
        assertEquals("gt6:wooden_panel_crate",
                tCrate.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                        .getAsJsonObject().get("name").getAsString(), "the self-drop (the crate slot 63 face)");
    }

    /** The lang pin: the ONE atomic family key in BOTH locales, the dump faces verbatim. */
    @Test
    void generatedLangCarriesTheFamilyKey() throws Exception {
        var tEn = json(generated().resolve("assets/gt6/lang/en_us.json"));
        var tZh = json(generated().resolve("assets/gt6/lang/zh_cn.json"));
        assertEquals("Wooden Panel", tEn.get("gt6.panel.wood").getAsString());
        assertNull(tEn.get("gt6.panel.oak"), "no per-row keys — the dump face is one name");
        assertEquals("木制覆盖板", tZh.get("gt6.panel.wood").getAsString(), "the dump face (mte 32500 band)");
    }
}
