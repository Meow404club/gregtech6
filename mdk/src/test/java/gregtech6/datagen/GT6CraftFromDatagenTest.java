/**
 * The CraftFrom band pin test (task p36-craftfrom-plategem acceptance ③, coordinator
 * ruling A on the declared口径 conflict — the EMPIRICAL caliber): the row-count pins are
 * the measured item-truth numbers written dead (11 / 205 / 109x5 / 201 = 962), the
 * universe pins are SET assertions against the live registrationOrder faces (the
 * plate-split rows == the plateGem face — plateGemTiny's condition IS plateGem, the
 * gem-tier rows == the gem-tier prefix face), the boule rows ride exactly the eleven
 * forced boule materials (the GTMaterialItemsForceTest BOULE_MATERIALS set), the amount
 * pins are the :171-178 amounts verbatim, and the representative generated-JSON rows get
 * the identity faces (the GT6BumbliaryCraftingJsonTest read form, the forge-leg plural
 * tree).
 *
 * <p>口径勘误 background: the research card's "plateGem=11 / gem tiers=7 sapphires"
 * misread the ForceTest quartet pins as the full set — the p8 census "PlateGem 203"
 * and the runData walk both give the ≈205/109 faces; this test pins THOSE.
 *
 * <p>task p37-craftfrom-stick extension (the same family law): the stick/stickLong
 * rows (Loader_OreProcessing.java:156-163, 1586 rows = 109x3 stickLong tiers +
 * 731 stickLong split + 201 regular gem + 109x3 stick tiers), the universe pins
 * against the live conditioned prefix faces, the :156-163 amount pins, and the
 * three-grid identity faces (["sf"," X"] / ["s "," X"] / ["s ","fX"]).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6CraftFromDatagenTest extends GTOfflineTestBase {

    /** The eleven boule materials (the GTMaterialItemsForceTest BOULE_MATERIALS set). */
    private static final List<String> BOULE_MATERIALS = List.of(
        "Silicon", "Germanium", "RedstoneAlloy", "NikolineAlloy",
        "Sapphire", "BlueSapphire", "GreenSapphire", "YellowSapphire", "OrangeSapphire", "PurpleSapphire", "Ruby");

    @BeforeAll
    static void initMaterialSystem() {
        // the boot order matters: the inherited GTOfflineTestBase boot runs FIRST (the
        // GT6CraftingRecipes class init drags GT6Hoppers → ForgeRegistries → needs Bootstrap)
        GTMaterialItems.initMaterials(); // the force table rides initMaterials (the C2 seam)
    }

    private static Set<String> materialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.CraftFromMaterialRow tRow : GT6CraftingRecipes.craftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Acceptance ③ row-count pin, the EMPIRICAL numbers dead-written (ruling A ①): 11 + 205 + 109x5 + 201 = 962. */
    @Test
    public void theRowCountIsTheMeasuredItemTruth() {
        List<GT6CraftingRecipes.CraftFromMaterialRow> tRows = GT6CraftingRecipes.craftFromMaterialRows();
        assertEquals(962, tRows.size(), "11 boule + 205 plate split + 5 tiers x 109 + regular 201");
        assertEquals(11, materialsOf("boule2plate_gem").size(), "the :178 boule cut (the forced-boule face)");
        assertEquals(205, materialsOf("plate2plate_tiny").size(), "the :171 plate split (the plateGem face)");
        assertEquals(109, materialsOf("gem2plate_gem/chipped").size(), "the :172 chipped tier (the chipped face)");
        assertEquals(109, materialsOf("gem2plate_gem/flawed").size(), "the :173 flawed tier");
        assertEquals(201, materialsOf("gem2plate_gem/regular").size(), "the :174 regular tier (the whole GEMS face)");
        assertEquals(109, materialsOf("gem2plate_gem/flawless").size(), "the :175 flawless tier");
        assertEquals(109, materialsOf("gem2plate_gem/exquisite").size(), "the :176 exquisite tier");
        assertEquals(109, materialsOf("gem2plate_gem/legendary").size(), "the :177 legendary tier");
    }

    /**
     * The registrationOrder face of one prefix minus the row-condition exclusions — the
     * upstream And(ANTIMATTER.NOT, COATED.NOT) drops COATED/ANTIMATTER materials from the
     * listener universe even though their items exist, so the row set is the CONDITIONED
     * face, not the bare face.
     */
    private static Set<String> conditionedFace(gregapi.oredict.OreDictPrefix aPrefix) {
        Set<String> rNames = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() != aPrefix) continue;
            OreDictMaterial tMaterial = tPair.material();
            if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
            if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
            rNames.add(tMaterial.mNameInternal);
        }
        return rNames;
    }

    /** Acceptance ③ universe SET pin (ruling A ②): the split/tier rows == the live CONDITIONED prefix faces — no 11-caliber assertion anywhere. */
    @Test
    public void theUniversesAreTheLivePrefixFaces() {
        Set<String> tPlateGemFace = conditionedFace(OP.plateGem);
        // plateGemTiny's condition IS plateGem (:1248) — the split rows cover the whole conditioned face
        assertEquals(tPlateGemFace, materialsOf("plate2plate_tiny"), "the :171 universe == the conditioned plateGem face");
        // the tier rows == the tier prefix face ∩ the plateGem face (the plateGem face covers the tiers)
        for (String tTier : new String[] {"gem2plate_gem/chipped", "gem2plate_gem/flawed", "gem2plate_gem/flawless",
                "gem2plate_gem/exquisite", "gem2plate_gem/legendary"}) {
            Set<String> tTierFace = new HashSet<>(conditionedFace(switch (tTier) {
                case "gem2plate_gem/chipped" -> OP.gemChipped;
                case "gem2plate_gem/flawed" -> OP.gemFlawed;
                case "gem2plate_gem/flawless" -> OP.gemFlawless;
                case "gem2plate_gem/exquisite" -> OP.gemExquisite;
                default -> OP.gemLegendary;
            }));
            assertTrue(tPlateGemFace.containsAll(tTierFace), "the plateGem face covers the tier face: " + tTier);
            assertEquals(tTierFace, materialsOf(tTier), "the tier universe == the conditioned tier face: " + tTier);
        }
        // the :174 input face is the whole gem walk, but the row needs the OUTPUT item too:
        // gem ∩ plateGemTiny (some gem materials — e.g. Phosphor — carry no PLATES flag)
        Set<String> tGemFace = new HashSet<>(conditionedFace(OP.gem));
        tGemFace.retainAll(conditionedFace(OP.plateGemTiny));
        assertEquals(tGemFace, materialsOf("gem2plate_gem/regular"), "the :174 universe == the gem ∩ plateGemTiny faces");
    }

    /** Acceptance ③ universe pin: the boule rows ride exactly the eleven forced materials. */
    @Test
    public void theBouleUniverseIsExactlyTheElevenForcedMaterials() {
        assertEquals(new HashSet<>(BOULE_MATERIALS), materialsOf("boule2plate_gem"), "the :178 universe == the force set");
    }

    /** Acceptance ③ amount pin: the :171-178 output amounts verbatim, one row per form-material. */
    @Test
    public void theAmountsAreTheUpstreamVerbatim() {
        for (GT6CraftingRecipes.CraftFromMaterialRow tRow : GT6CraftingRecipes.craftFromMaterialRows()) {
            assertEquals(switch (tRow.aForm().aKey()) {
                case "boule2plate_gem" -> 3; // :178
                case "plate2plate_tiny" -> 8; // :171
                case "gem2plate_gem/chipped" -> 2; // :172
                case "gem2plate_gem/flawed" -> 4; // :173
                case "gem2plate_gem/regular" -> 8; // :174
                case "gem2plate_gem/flawless" -> 1; // :175
                case "gem2plate_gem/exquisite" -> 3; // :176
                case "gem2plate_gem/legendary" -> 7; // :177
                default -> throw new IllegalArgumentException("unknown form: " + tRow.aForm().aKey());
            }, tRow.aForm().aCount(), "the output amount: " + tRow.aForm().aKey());
        }
    }

    /** The row id — the digLadderRowId form over the gt6 namespace. */
    @Test
    public void theRowIdIsTheFormKeyPlusMaterialLeaf() {
        assertEquals("gt6:boule2plate_gem/silicon", GT6CraftingRecipes.craftFromRowId("boule2plate_gem", "silicon").toString());
        assertEquals("gt6:gem2plate_gem/regular/sapphire", GT6CraftingRecipes.craftFromRowId("gem2plate_gem/regular", "sapphire").toString());
    }

    private static JsonObject generated(String aPath) throws Exception {
        String tPath = "/data/gt6/recipes/" + aPath + ".json";
        try (InputStream tStream = GT6CraftFromDatagenTest.class.getResourceAsStream(tPath)) {
            assertNotNull(tStream, tPath + " rides the generated-resources classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertSawFrame(JsonObject aRow, String aExpectedResultItem, int aExpectedCount, String aExpectedInputItem) {
        assertEquals(2, aRow.getAsJsonArray("pattern").size(), "the 2x2 frame");
        assertEquals("s ", aRow.getAsJsonArray("pattern").get(0).getAsString(), "the saw row");
        assertEquals(" X", aRow.getAsJsonArray("pattern").get(1).getAsString(), "the input row");
        assertTrue(aRow.getAsJsonObject("key").get("s").getAsJsonObject().get("tag").getAsString().endsWith("tools/saw"),
                "'s' = the saw tool tag (the spray-can translation)");
        assertEquals(aExpectedInputItem, aRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the same-material input item");
        assertEquals(aExpectedResultItem, aRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
        assertEquals(aExpectedCount, aRow.getAsJsonObject("result").get("count").getAsInt(), "the result count");
    }

    /** Acceptance ③ identity face: the :178 boule row for Silicon (the quartet leg). */
    @Test
    public void theSiliconBouleRowCarriesTheUpstreamGrid() throws Exception {
        assertSawFrame(generated("boule2plate_gem/silicon"),
                "gt6:" + GTMaterialItems.itemIdOf(OP.plateGem, material("Silicon")), 3,
                "gt6:" + GTMaterialItems.itemIdOf(OP.bouleGt, material("Silicon")));
    }

    /** Acceptance ③ identity face: the :171 plate-split row for Amethyst (the wide-gem face leg). */
    @Test
    public void theAmethystPlateSplitRowCarriesTheUpstreamGrid() throws Exception {
        assertSawFrame(generated("plate2plate_tiny/amethyst"),
                "gt6:" + GTMaterialItems.itemIdOf(OP.plateGemTiny, material("Amethyst")), 8,
                "gt6:" + GTMaterialItems.itemIdOf(OP.plateGem, material("Amethyst")));
    }

    /** Acceptance ③ identity face: the :174 regular-gem tier row for Sapphire. */
    @Test
    public void theSapphireRegularGemRowCarriesTheUpstreamGrid() throws Exception {
        assertSawFrame(generated("gem2plate_gem/regular/sapphire"),
                "gt6:" + GTMaterialItems.itemIdOf(OP.plateGemTiny, material("Sapphire")), 8,
                "gt6:" + GTMaterialItems.itemIdOf(OP.gem, material("Sapphire")));
    }

    /** Acceptance ③ identity face: the :177 legendary tier row for Amethyst (the 7x payout). */
    @Test
    public void theAmethystLegendaryRowCarriesTheUpstreamGrid() throws Exception {
        assertSawFrame(generated("gem2plate_gem/legendary/amethyst"),
                "gt6:" + GTMaterialItems.itemIdOf(OP.plateGem, material("Amethyst")), 7,
                "gt6:" + GTMaterialItems.itemIdOf(OP.gemLegendary, material("Amethyst")));
    }

    private static OreDictMaterial material(String aName) {
        for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial != null && tMaterial.mID >= 0 && tMaterial.mNameInternal.equals(aName)) return tMaterial;
        }
        throw new IllegalArgumentException("no material: " + aName);
    }

    // ------------------------------------------------------------------
    // task p37-craftfrom-stick — the stick/stickLong family
    // (Loader_OreProcessing.java:156-163, the coordinator-approved boundary:
    // BOTH stick* output prefixes, the plategem companion shape)
    // ------------------------------------------------------------------

    private static Set<String> stickMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.StickCraftFromMaterialRow tRow : GT6CraftingRecipes.stickCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count pin (the measured item truth, the ruling-A caliber): 109x3 + 731 + 201 + 109x3 = 1586. */
    @Test
    public void theStickRowCountIsTheMeasuredItemTruth() {
        assertEquals(1586, GT6CraftingRecipes.stickCraftFromMaterialRows().size(),
                "3 stickLong tiers x 109 + the stickLong split 731 + regular 201 + 3 stick tiers x 109");
        assertEquals(109, stickMaterialsOf("gem2stick_long/flawless").size(), "the :156 tier (the flawless face)");
        assertEquals(109, stickMaterialsOf("gem2stick_long/exquisite").size(), "the :157 tier");
        assertEquals(109, stickMaterialsOf("gem2stick_long/legendary").size(), "the :158 tier");
        assertEquals(731, stickMaterialsOf("stick_long2stick").size(), "the :159 split (the stick ∩ stickLong faces)");
        assertEquals(201, stickMaterialsOf("gem2stick/regular").size(), "the :160 regular tier (the whole conditioned gem face)");
        assertEquals(109, stickMaterialsOf("gem2stick/flawless").size(), "the :161 tier");
        assertEquals(109, stickMaterialsOf("gem2stick/exquisite").size(), "the :162 tier");
        assertEquals(109, stickMaterialsOf("gem2stick/legendary").size(), "the :163 tier");
    }

    /** Universe SET pin: every form's rows == the live CONDITIONED output face ∩ the input face. */
    @Test
    public void theStickUniversesAreTheLivePrefixFaces() {
        for (GT6CraftingRecipes.StickCraftFromForm tForm : GT6CraftingRecipes.stickCraftFromForms()) {
            Set<String> tExpected = new HashSet<>(conditionedFace(tForm.aOutput()));
            tExpected.retainAll(conditionedFace(tForm.aInput()));
            assertEquals(tExpected, stickMaterialsOf(tForm.aKey()),
                    "the universe == the conditioned " + tForm.aOutput().mNameInternal + " ∩ " + tForm.aInput().mNameInternal + " faces");
        }
    }

    /** Amount pin: the :156-163 output amounts verbatim, one row per form-material. */
    @Test
    public void theStickAmountsAreTheUpstreamVerbatim() {
        for (GT6CraftingRecipes.StickCraftFromMaterialRow tRow : GT6CraftingRecipes.stickCraftFromMaterialRows()) {
            assertEquals(switch (tRow.aForm().aKey()) {
                case "gem2stick_long/flawless" -> 1; // :156
                case "gem2stick_long/exquisite" -> 2; // :157
                case "gem2stick_long/legendary" -> 4; // :158
                case "stick_long2stick" -> 2; // :159
                case "gem2stick/regular" -> 1; // :160
                case "gem2stick/flawless" -> 2; // :161
                case "gem2stick/exquisite" -> 4; // :162
                case "gem2stick/legendary" -> 8; // :163
                default -> throw new IllegalArgumentException("unknown form: " + tRow.aForm().aKey());
            }, tRow.aForm().aCount(), "the output amount: " + tRow.aForm().aKey());
        }
    }

    /** The row id — the craftFromRowId form over the gt6 namespace (the shared id law). */
    @Test
    public void theStickRowIdIsTheFormKeyPlusMaterialLeaf() {
        assertEquals("gt6:gem2stick/regular/sapphire", GT6CraftingRecipes.craftFromRowId("gem2stick/regular", "sapphire").toString());
        assertEquals("gt6:stick_long2stick/diamond", GT6CraftingRecipes.craftFromRowId("stick_long2stick", "diamond").toString());
    }

    private static void assertStickGrid(JsonObject aRow, String aTop, String aBottom, String aExpectedResultItem, int aExpectedCount,
            String aExpectedInputItem, boolean aExpectFile) {
        assertEquals(2, aRow.getAsJsonArray("pattern").size(), "the 2x2 frame");
        assertEquals(aTop, aRow.getAsJsonArray("pattern").get(0).getAsString(), "the top row");
        assertEquals(aBottom, aRow.getAsJsonArray("pattern").get(1).getAsString(), "the bottom row");
        assertTrue(aRow.getAsJsonObject("key").get("s").getAsJsonObject().get("tag").getAsString().endsWith("tools/saw"),
                "'s' = the saw tool tag");
        if (aExpectFile) {
            assertTrue(aRow.getAsJsonObject("key").get("f").getAsJsonObject().get("tag").getAsString().endsWith("tools/file"),
                    "'f' = the file tool tag (upstream CR.java:231)");
        } else {
            assertTrue(aRow.getAsJsonObject("key").get("f") == null, "no phantom file key on the saw-only frame (the shape-driven define)");
        }
        assertEquals(aExpectedInputItem, aRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the same-material input item");
        assertEquals(aExpectedResultItem, aRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
        int tCount = aRow.getAsJsonObject("result").has("count") ? aRow.getAsJsonObject("result").get("count").getAsInt() : 1;
        assertEquals(aExpectedCount, tCount, "the result count (the vanilla count=1 key omission)");
    }

    /** Identity face: the :160 regular-gem row for Sapphire (["s ","fX"], the file arm). */
    @Test
    public void theSapphireRegularGemStickRowCarriesTheUpstreamGrid() throws Exception {
        assertStickGrid(generated("gem2stick/regular/sapphire"), "s ", "fX",
                "gt6:" + GTMaterialItems.itemIdOf(OP.stick, material("Sapphire")), 1,
                "gt6:" + GTMaterialItems.itemIdOf(OP.gem, material("Sapphire")), true);
    }

    /** Identity face: the :156 flawless tier row for Diamond (["sf"," X"], saw+file top row → stickLong). */
    @Test
    public void theDiamondFlawlessStickLongRowCarriesTheUpstreamGrid() throws Exception {
        assertStickGrid(generated("gem2stick_long/flawless/diamond"), "sf", " X",
                "gt6:" + GTMaterialItems.itemIdOf(OP.stickLong, material("Diamond")), 1,
                "gt6:" + GTMaterialItems.itemIdOf(OP.gemFlawless, material("Diamond")), true);
    }

    /** Identity face: the :159 stickLong split for Diamond (["s "," X"], the saw-only frame). */
    @Test
    public void theDiamondStickLongSplitRowCarriesTheUpstreamGrid() throws Exception {
        assertStickGrid(generated("stick_long2stick/diamond"), "s ", " X",
                "gt6:" + GTMaterialItems.itemIdOf(OP.stick, material("Diamond")), 2,
                "gt6:" + GTMaterialItems.itemIdOf(OP.stickLong, material("Diamond")), false);
    }
}
