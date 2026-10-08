/**
 * Tests for task material-mc-g1-panels-dyed: the GT6 dyed Cover Panel census (48 per-row
 * Block+BlockItem ids over 3 families x 16 colours), the upstream meta mapping, the
 * id-scheme, the MTE-default numbers, the family name face (NO colour word — the dump
 * face), the tint seam, and the generated-tree pins (blockstates/item models/loot/
 * recipes/lang).
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_MultiTileEntities.java:2043-2056 — the 16-loop, Concrete Panel 32452+i
 *     (:2045) / C-Foam Panel 32468+i (:2049) / Asphalt Panel 32484+i (:2053),
 *     {@code NBT_COLOR=i}, harvest class aStone, stack 16.</li>
 * <li>MultiTileEntityPanelColored.java:33-36 — the tint face: ONE grayscale texture per
 *     family (Textures.BlockIcons CONCRETE/CFOAM_HARDENED/ASPHALT) x {@code DYES[mColor]}.</li>
 * <li>TileEntityBase07Paintable.java:51 — the MTE defaults the rows inherit (no
 *     NBT_HARDNESS/NBT_RESISTANCE on the panel rows): hardness 1.0, resistance 3.0.</li>
 * <li>tmp/gregtech.lang:13333/:13349/:13365 — the dump faces 混凝土覆盖板 /
 *     建筑泡沫覆盖板 / 沥青覆盖板: NO colour word, one name per family.</li>
 * <li>MultiTileEntityPanel.java:38 — canPlace=F: the placement face is the declared
 *     deviation, the dyed cover-mount face pools (GT6PanelBlock javadoc).</li>
 * <li>CR.java:211/:229 — the 's' saw / 'd' screwdriver tool letters of the panel recipe
 *     pattern "TsT"/"TPT"/"TdT" (result 6x).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GT6ConcreteRegistryTest shape): the registry class
 * initialises offline (DeferredRegister accumulates suppliers); the block ctors are driven
 * directly past the bootstrap+unfreeze bracket.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.panels.GT6PanelBlock;
import gregtech6.item.spraycan.GTSprayCanItem;

class GT6PanelsRegistryTest {

    /** The 48-row census — the Loader_MultiTileEntities.java:2043-2056 loop (3 families x 16 colours). */
    private static final int PINNED_TOTAL = 48;

    @BeforeAll
    static void initOfflineJVM() {
        // the GT6ConcreteRegistryTest posture: bootstrap vanilla, then unfreeze the block
        // registry so the direct block ctors work self-sufficiently
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
    }

    /** The census: 3 families x 16 colours, family-major in the Loader registration order (:2045/:2049/:2053). */
    @Test
    void panelsCensusIsPinned() {
        assertEquals(PINNED_TOTAL, GT6Panels.registrationOrder().size(), "3 x 16 registration rows");
        for (int i = 0; i < 48; i++) {
            assertEquals(GT6PanelBlock.Family.values()[i / 16], GT6Panels.registrationOrder().get(i).family(),
                    "family-major walk at " + i);
            assertEquals(i % 16, GT6Panels.registrationOrder().get(i).dyeIndex(), "colour-major in meta order at " + i);
        }
        assertEquals(PINNED_TOTAL, GT6Panels.BLOCKS.size(), "48 blocks");
        assertEquals(PINNED_TOTAL, GT6Panels.ITEMS.size(), "48 block items");
    }

    /** The id scheme: {@code <family>_panel_<dye>}, the dye segment the DYE_IDS snake; 48 distinct paths. */
    @Test
    void perRowIdSchemeIsPinned() {
        assertEquals("concrete_panel_light_gray", GT6Panels.path(GT6PanelBlock.Family.CONCRETE, 7));
        assertEquals("cfoam_panel_white", GT6Panels.path(GT6PanelBlock.Family.CFOAM, 15));
        assertEquals("asphalt_panel_black", GT6Panels.path(GT6PanelBlock.Family.ASPHALT, 0));
        Set<String> tPaths = new HashSet<>();
        for (GT6Panels.PanelRow tRow : GT6Panels.registrationOrder()) {
            assertTrue(tPaths.add(GT6Panels.path(tRow.family(), tRow.dyeIndex())), "distinct ids: " + tRow);
        }
        assertEquals(PINNED_TOTAL, tPaths.size(), "48 distinct registry paths");
        Set<String> tItemIds = new HashSet<>();
        for (var tEntry : GT6Panels.ITEMS_REG.getEntries()) {
            tItemIds.add(tEntry.getId().getPath());
        }
        assertEquals(tPaths, tItemIds, "the item band covers exactly the 48 block paths");
    }

    /**
     * The MTE-default numbers (TileEntityBase07Paintable.java:51 — the rows carry no
     * NBT_HARDNESS/NBT_RESISTANCE): 1.0/3.0, aStone -> STONE sound, no state properties.
     */
    @Test
    void blockCarriesTheMteDefaultsAndFixedColour() {
        GT6PanelBlock tPanel = new GT6PanelBlock("concrete_panel_light_gray", GT6PanelBlock.Family.CONCRETE, 7);
        assertEquals(7, tPanel.dyeIndex, "the fixed dye index");
        assertEquals(GT6PanelBlock.Family.CONCRETE, tPanel.family, "the family field");
        assertTrue(tPanel.defaultBlockState().getProperties().isEmpty(), "the pure-block form (no colour property)");
        assertEquals(1.0F, tPanel.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "hardness = the MTE default 1.0 (TileEntityBase07Paintable.java:51)");
        assertEquals(3.0F, tPanel.getExplosionResistance(), 1.0e-6F,
                "resistance = the MTE default 3.0 (TileEntityBase07Paintable.java:51)");
    }

    /**
     * The family name face: the dump carries NO colour word — one ATOMIC key per family
     * (no args, no dye compose; the addConcrete template face does not apply).
     */
    @Test
    void familyNameFaceIsAtomicPerFamily() {
        for (GT6PanelBlock.Family tFamily : GT6PanelBlock.Family.values()) {
            GT6PanelBlock tPanel = new GT6PanelBlock(tFamily.snake + "_black", tFamily, 0);
            net.minecraft.network.chat.contents.TranslatableContents tContents =
                    (net.minecraft.network.chat.contents.TranslatableContents) tPanel.getName().getContents();
            assertEquals(tFamily.nameKey, tContents.getKey(), "the family key of " + tFamily);
            assertEquals(0, tContents.getArgs().length, "atomic — no colour slot (the dump face) for " + tFamily);
        }
        assertEquals("gt6.panel.concrete", GT6PanelBlock.Family.CONCRETE.nameKey);
        assertEquals("gt6.panel.cfoam", GT6PanelBlock.Family.CFOAM.nameKey);
        assertEquals("gt6.panel.asphalt", GT6PanelBlock.Family.ASPHALT.nameKey);
    }

    /**
     * The tint seam (the GT6ConcreteTintListener posture plus the Item half): the
     * constant lookup of the block's FIXED dye index over {@code GTSprayCanItem.DYES_INT}
     * (the MultiTileEntityPanelColored.java:33-36 face), tint index 0 only, non-panel
     * blocks untinted, the 16 colours pairwise distinct.
     */
    @Test
    void tintSeamResolvesTheFixedDyeIndex() {
        GT6PanelBlock tPanel = new GT6PanelBlock("concrete_panel_light_gray", GT6PanelBlock.Family.CONCRETE, 7);
        assertEquals(0xFF000000 | GTSprayCanItem.DYES_INT[7],
                gregtech6.client.panels.GT6PanelTintListener.panelTintARGB(tPanel, 0), "tints DYES_INT[7]");
        assertEquals(-1, gregtech6.client.panels.GT6PanelTintListener.panelTintARGB(tPanel, 1),
                "tint index 1 is outside the family grammar");
        assertEquals(-1, gregtech6.client.panels.GT6PanelTintListener.panelTintARGB(
                net.minecraft.world.level.block.Blocks.STONE, 0), "non-family blocks stay untinted");
        Set<Integer> tColours = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            GT6PanelBlock tRow = new GT6PanelBlock("cfoam_panel_" + GTSprayCanItem.DYE_IDS[i], GT6PanelBlock.Family.CFOAM, i);
            assertTrue(tColours.add(gregtech6.client.panels.GT6PanelTintListener.panelTintARGB(tRow, 0)),
                    "the 16 colours are pairwise distinct at " + i);
        }
        assertEquals(16, tColours.size());
        // the item half rides the same constant (the attachment listener form)
        assertTrue(gregtech6.client.panels.GT6PanelTintListener.panelItemColor() != null, "the ItemColor seam exists");
    }

    // ------------------------------------------------------------------ the generated-tree pins

    /** The mdk root (the GT6ConcreteRegistryTest walk-up). */
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

    /** The tintindex of one element face ({@code -1} when the face carries none). */
    private static int elementFaceTintindex(com.google.gson.JsonObject aModel, int aElement, String aFace) {
        var tFace = aModel.getAsJsonArray("elements").get(aElement).getAsJsonObject().getAsJsonObject("faces")
                .getAsJsonObject(aFace);
        return tFace.has("tintindex") ? tFace.get("tintindex").getAsInt() : -1;
    }

    /** The tree pin: 48 blockstates + 48 item models + 48 loot tables, no per-colour model. */
    @Test
    void generatedTreeCarriesThePanelFace() throws Exception {
        Path tGen = generated();
        for (GT6PanelBlock.Family tFamily : GT6PanelBlock.Family.values()) {
            for (int i = 0; i < 16; i++) {
                String tPath = GT6Panels.path(tFamily, i);
                assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tPath + ".json")), tPath + " blockstate");
                assertTrue(Files.exists(tGen.resolve("assets/gt6/models/item/" + tPath + ".json")), tPath + " item model");
                assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tPath + ".json")), tPath + " loot");
            }
        }
        // the THREE shared tinted cubes — and ONLY 3 (no per-colour model files: a
        // per-colour copy would mean the tint leg died)
        Set<String> tModels = new HashSet<>();
        try (var tStream = Files.list(tGen.resolve("assets/gt6/models/block"))) {
            tStream.map(aPath -> aPath.getFileName().toString())
                    .filter(aName -> aName.endsWith("_panel.json")).forEach(tModels::add);
        }
        assertEquals(Set.of("concrete_panel.json", "cfoam_panel.json", "asphalt_panel.json"), tModels,
                "exactly the 3 shared tinted panel models");
    }

    /** The model-body pins: every family model carries tintindex 0 over ITS grayscale tile, one element. */
    @Test
    void tintedModelsStaySingleElementOverTheGrayscaleTiles() throws Exception {
        var tConcrete = json(generated().resolve("assets/gt6/models/block/concrete_panel.json"));
        assertEquals("gt6:block/concrete", tConcrete.getAsJsonObject("textures").get("all").getAsString(),
                "the CONCRETE grayscale tile (upstream iconsets/CONCRETE.png byte-verbatim)");
        var tCfoam = json(generated().resolve("assets/gt6/models/block/cfoam_panel.json"));
        assertEquals("gt6:block/cfoam_hardened", tCfoam.getAsJsonObject("textures").get("all").getAsString(),
                "the CFOAM_HARDENED seat (the foam family's port art, the declared texture-seat deviation)");
        var tAsphalt = json(generated().resolve("assets/gt6/models/block/asphalt_panel.json"));
        assertEquals("gt6:block/asphalt", tAsphalt.getAsJsonObject("textures").get("all").getAsString(),
                "the ASPHALT grayscale tile (upstream iconsets/ASPHALT.png byte-verbatim)");
        for (String tName : new String[] {"concrete_panel", "cfoam_panel", "asphalt_panel"}) {
            var tModel = json(generated().resolve("assets/gt6/models/block/" + tName + ".json"));
            assertEquals(1, tModel.getAsJsonArray("elements").size(), "ONE element on " + tName);
            for (String tFace : new String[] {"down", "up", "north", "south", "west", "east"}) {
                assertEquals(0, elementFaceTintindex(tModel, 0, tFace), "tintindex 0 on " + tName + "/" + tFace);
            }
        }
    }

    /**
     * The recipe band: 32 JSONs (Concrete 16 faithful + C-Foam 16 over the uncoloured
     * cfoam input), the upstream pattern "TsT"/"TPT"/"TdT" -> 6x, iron screw + saw +
     * screwdriver; the Asphalt 16 build NO recipe (the declared G2 cut — no port asphalt
     * block family).
     */
    @Test
    void recipeBandIs32WithTheAsphaltCutDeclared() throws Exception {
        Path tRecipes = generated().resolve("data/gt6/recipes");
        for (GT6PanelBlock.Family tFamily : GT6PanelBlock.Family.values()) {
            for (int i = 0; i < 16; i++) {
                String tPath = GT6Panels.path(tFamily, i) + ".json";
                if (tFamily == GT6PanelBlock.Family.ASPHALT) {
                    assertFalse(Files.exists(tRecipes.resolve(tPath)),
                            "the declared asphalt cut stays dead: " + tPath);
                    continue;
                }
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
                assertEquals("gt6:" + tPath.substring(0, tPath.length() - 5), tResult.get("item").getAsString(),
                        "the result-path id on " + tPath);
            }
        }
        // the source columns: Concrete takes the colour-matched concrete item (faithful),
        // C-Foam takes the single uncoloured cfoam item (the collapsed item ladder fold)
        var tConcrete = json(tRecipes.resolve("concrete_panel_red.json"));
        assertEquals("gt6:concrete_red",
                tConcrete.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(),
                "the colour-matched concrete source");
        var tCfoam = json(tRecipes.resolve("cfoam_panel_red.json"));
        assertEquals("gt6:cfoam", tCfoam.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(),
                "the uncoloured cfoam source (the declared input fold)");
        assertEquals("gt6:cfoam", json(tRecipes.resolve("cfoam_panel_blue.json"))
                .getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(),
                "every cfoam row rides the same uncoloured input");
    }

    /** The loot pin: the panels self-drop (the concrete full-block arm). */
    @Test
    void lootFacesAreSelfDrop() throws Exception {
        var tFull = json(generated().resolve("data/gt6/loot_tables/blocks/concrete_panel_black.json"));
        assertEquals("gt6:concrete_panel_black",
                tFull.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                        .getAsJsonObject().get("name").getAsString(), "the self-drop");
        var tAsphalt = json(generated().resolve("data/gt6/loot_tables/blocks/asphalt_panel_white.json"));
        assertEquals("gt6:asphalt_panel_white",
                tAsphalt.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                        .getAsJsonObject().get("name").getAsString(), "the self-drop (asphalt family)");
    }

    /** The lang pin: the 3 atomic keys in BOTH locales, the dump faces verbatim. */
    @Test
    void generatedLangCarriesThe3FamilyKeys() throws Exception {
        var tEn = json(generated().resolve("assets/gt6/lang/en_us.json"));
        var tZh = json(generated().resolve("assets/gt6/lang/zh_cn.json"));
        assertEquals("Concrete Panel", tEn.get("gt6.panel.concrete").getAsString());
        assertEquals("C-Foam Panel", tEn.get("gt6.panel.cfoam").getAsString());
        assertEquals("Asphalt Panel", tEn.get("gt6.panel.asphalt").getAsString());
        assertEquals("混凝土覆盖板", tZh.get("gt6.panel.concrete").getAsString(), "the dump face (mte 32452 band)");
        assertEquals("建筑泡沫覆盖板", tZh.get("gt6.panel.cfoam").getAsString(), "the dump face (mte 32468 band)");
        assertEquals("沥青覆盖板", tZh.get("gt6.panel.asphalt").getAsString(), "the dump face (mte 32484 band)");
    }
}
