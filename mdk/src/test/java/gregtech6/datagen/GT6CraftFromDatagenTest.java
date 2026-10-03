/**
 * The CraftFrom band pin test (task craftfrom-plategem acceptance ③, coordinator
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
 * <p>task craftfrom-stick extension (the same family law): the stick/stickLong
 * rows (Loader_OreProcessing.java:156-163, 1586 rows = 109x3 stickLong tiers +
 * 731 stickLong split + 201 regular gem + 109x3 stick tiers), the universe pins
 * against the live conditioned prefix faces, the :156-163 amount pins, and the
 * three-grid identity faces (["sf"," X"] / ["s "," X"] / ["s ","fX"]).
 *
 * <p>task craftfrom-foil extension (the coordinator fine-wire-batch ruling):
 * the upstream foil-family OUTPUT face is the EMPTY set — foil is only the :168
 * INPUT — so the batch = :168 foil2wireFine + :169 plate2wire (219 measured foil
 * rows + 0 wire rows, the GT6RecipesWiremill seam), the single-row grid pins
 * (["Xx"] / ["Px"]), the wire-cutter tag, and the count=1 key-omission identity.
 *
 * <p>task craftfrom-rockgt extension (the rockGt batch): :148 is the ONLY
 * rockGt statement of the 47-statement panel (:149-150 output gearGt from stick —
 * not this family, the residual closeout card owns them) — the single
 * {"XYX","YfY","XYX"} gear row carrying the FIRST POSITIVE material condition
 * (And(ANTIMATTER.NOT, COATED.NOT, STONE, MT.Stone.NOT, MT.Bedrock.NOT)): the
 * universe pin rides the gearGt ∩ rockGt ∩ GTStoneBlocks-family three-face
 * intersection under that verbatim tail, and the stone companion carrier (the
 * block-path OP.stone face, keyed on the family STONE-variant block item) rides
 * the declared carrier deviation.
 *
 * <p>task craftfrom-residual extension (the panel closeout — the 28 residual
 * statements in five shaped bands + the shapeless panel): the :149 gear twin rides
 * the SAME universe as the :148 rockGt row (the equality pin), :150 folds
 * +SMITHABLE, the casingMachine faces UNLOCKED at 4x209=836 rows with the item
 * family (task casing-machine-register), the :151 plank cut UNLOCKED with the
 * WOOD-gated plank items (task wood-planks-register; cableGt still pours
 * ZERO — no MaterialPrefixItems — the fine-wire :169 seam), the small-parts/minecartWheels
 * universes ride the multi-face item-truth intersection under the verbatim
 * ANTIMATTER-only/COATED tails, the shapeless panel rides the per-material slot
 * substitution law (the :480-510 .dat(m) face) with the fixed MT.Empty slots, the
 * ANY-group multi-item slots and the tool-tag slots, and the :186 meltmin(293)
 * face (CS.java:135 C+20).
 *
 * <p>task toolhead-r11a-file-belt extension (the raw→finished tool-head file
 * belt): the OreProcessing_CraftFrom listener half living in
 * Loader_Recipes_Handlers.java:420-434 (the p37 census circled
 * Loader_OreProcessing.java only — the file-boundary scope miss) — twelve
 * one-row families + the arrow trio (:420 gemChipped ×2, :421 rockGt ×8, :422
 * rawArrow ×1), all the {"X ", " f"} grid over the TOOLS_FILE tag; the
 * universes ride the bare output ∩ input item-truth intersection under the
 * verbatim condition legs (the :421 STONE positive face included).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    // task craftfrom-stick — the stick/stickLong family
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

    // ------------------------------------------------------------------
    // task craftfrom-foil — the fine-wire batch (the coordinator ruling:
    // the upstream foil-family OUTPUT face is the EMPTY set — foil is only
    // the :168 INPUT — so the batch = :168 foil2wireFine + :169 plate2wire,
    // the two fine-wire/wire-domain rows of the residual pool)
    // ------------------------------------------------------------------

    private static Set<String> fineWireMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.FineWireCraftFromMaterialRow tRow : GT6CraftingRecipes.fineWireCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count pin (the measured item truth, the ruling-A caliber): 219 foil cuts + 14 plate2wire rows (the :169 unlock, task wire-gt-registration) = 233. */
    @Test
    public void theFineWireRowCountIsTheMeasuredItemTruth() {
        assertEquals(233, GT6CraftingRecipes.fineWireCraftFromMaterialRows().size(), "219 foil cuts + 14 plate2wire rows");
        assertEquals(219, fineWireMaterialsOf("foil2wire_fine").size(), "the :168 foil cut (the wireFine ∩ foil faces)");
        assertEquals(14, fineWireMaterialsOf("plate2wire").size(),
                "the :169 rows pour since task wire-gt-registration lifted OP.wireGt01 onto the item path — the face is the whole WIRES item face (all 14 materials carry plate items: the G_INGOT_MACHINE/G_MACHINE group-array expansion stamps the PLATES tag on every member, the OP.plate condition leg)");
    }

    /** Universe SET pin: every form's rows == the live CONDITIONED output face ∩ the input face. */
    @Test
    public void theFineWireUniversesAreTheLivePrefixFaces() {
        for (GT6CraftingRecipes.FineWireCraftFromForm tForm : GT6CraftingRecipes.fineWireCraftFromForms()) {
            Set<String> tExpected = new HashSet<>(conditionedFace(tForm.aOutput()));
            tExpected.retainAll(conditionedFace(tForm.aInput()));
            assertEquals(tExpected, fineWireMaterialsOf(tForm.aKey()),
                    "the universe == the conditioned " + tForm.aOutput().mNameInternal + " ∩ " + tForm.aInput().mNameInternal + " faces");
        }
    }

    /** Amount pin: the :168-169 output amounts verbatim (both 1). */
    @Test
    public void theFineWireAmountsAreTheUpstreamVerbatim() {
        for (GT6CraftingRecipes.FineWireCraftFromMaterialRow tRow : GT6CraftingRecipes.fineWireCraftFromMaterialRows()) {
            assertEquals(switch (tRow.aForm().aKey()) {
                case "foil2wire_fine" -> 1; // :168
                case "plate2wire" -> 1; // :169
                default -> throw new IllegalArgumentException("unknown form: " + tRow.aForm().aKey());
            }, tRow.aForm().aCount(), "the output amount: " + tRow.aForm().aKey());
        }
    }

    /** Row-shape pin: the upstream grids verbatim — :168 {"Xx"} (foil2wireFine), :169 {"Px"} (plate2wire, P = the null-SpecialPrefix plate default :535). */
    @Test
    public void theFineWireFormsCarryTheUpstreamGrids() {
        assertEquals("Xx", fineWireFormRow("foil2wire_fine"), "the :168 grid (X = foil, x = wire cutter CR.java:359)");
        assertEquals("Px", fineWireFormRow("plate2wire"), "the :169 grid (P = plate, x = wire cutter)");
    }

    private static String fineWireFormRow(String aKey) {
        for (GT6CraftingRecipes.FineWireCraftFromForm tForm : GT6CraftingRecipes.fineWireCraftFromForms()) {
            if (tForm.aKey().equals(aKey)) return tForm.aRow();
        }
        throw new IllegalArgumentException("unknown form: " + aKey);
    }

    /** The row id — the craftFromRowId form over the gt6 namespace (the shared id law). */
    @Test
    public void theFineWireRowIdIsTheFormKeyPlusMaterialLeaf() {
        assertEquals("gt6:foil2wire_fine/aluminium", GT6CraftingRecipes.craftFromRowId("foil2wire_fine", "aluminium").toString());
        assertEquals("gt6:plate2wire/iron", GT6CraftingRecipes.craftFromRowId("plate2wire", "iron").toString());
    }

    /** The single-row grid identity: ["Xx"], 'x' = the wire-cutter tag, the input letter = the input item, count=1 rides the vanilla key omission. */
    private static void assertFineWireGrid(JsonObject aRow, String aExpectedResultItem, String aExpectedInputItem, String aInputLetter) {
        assertEquals(1, aRow.getAsJsonArray("pattern").size(), "the single-row frame (upstream mRecipes[i].length == 1)");
        assertEquals(aInputLetter + "x", aRow.getAsJsonArray("pattern").get(0).getAsString(), "the cutter row");
        assertTrue(aRow.getAsJsonObject("key").get("x").getAsJsonObject().get("tag").getAsString().endsWith("tools/wire_cutter"),
                "'x' = the wire-cutter tool tag (upstream CR.java:359)");
        assertEquals(aExpectedInputItem, aRow.getAsJsonObject("key").get(aInputLetter).getAsJsonObject().get("item").getAsString(),
                "the input letter = the same-material input item");
        assertEquals(aExpectedResultItem, aRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
        int tCount = aRow.getAsJsonObject("result").has("count") ? aRow.getAsJsonObject("result").get("count").getAsInt() : 1;
        assertEquals(1, tCount, "the result count (the vanilla count=1 key omission)");
    }

    /** Identity face: the :168 foil-cut row for Aluminium (["Xx"], the wire-cutter arm). */
    @Test
    public void theAluminiumFoilCutRowCarriesTheUpstreamGrid() throws Exception {
        assertFineWireGrid(generated("foil2wire_fine/aluminium"),
                "gt6:" + GTMaterialItems.itemIdOf(OP.wireFine, material("Aluminium")),
                "gt6:" + GTMaterialItems.itemIdOf(OP.foil, material("Aluminium")), "X");
    }

    // ------------------------------------------------------------------
    // task craftfrom-rockgt — the rockGt batch (the :148 gear row, the
    // ONLY rockGt statement of the 47-statement panel; :149-150 output
    // gearGt from stick — not this family, the residual closeout card owns
    // them)
    // ------------------------------------------------------------------

    private static Set<String> rockGtMaterials() {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.RockGtCraftFromMaterialRow tRow : GT6CraftingRecipes.rockGtCraftFromMaterialRows()) {
            rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count + universe SET pin: the walk == the gearGt ∩ rockGt ∩ GTStoneBlocks-family faces under the verbatim :148 condition tail (STONE positive, Stone/Bedrock out), the measured item truth dead-written. */
    @Test
    public void theRockGtUniverseIsTheGearRockFamilyFace() {
        Set<String> tExpected = new HashSet<>(conditionedFace(OP.gearGt));
        tExpected.retainAll(conditionedFace(OP.rockGt));
        Set<String> tFamily = new HashSet<>();
        for (gregtech6.registry.GTStoneBlocks.StoneSpec tStone : gregtech6.registry.GTStoneBlocks.STONES) {
            tFamily.add(tStone.material().get().mNameInternal);
        }
        tExpected.retainAll(tFamily);
        tExpected.removeIf(tName -> !material(tName).contains(gregapi.data.TD.Properties.STONE)); // STONE — the positive rock face
        tExpected.remove("Stone"); // MT.Stone.NOT (the vanilla-covered identity)
        tExpected.remove("Bedrock"); // MT.Bedrock.NOT
        assertEquals(tExpected, rockGtMaterials(), "the :148 universe == the three-face intersection under the verbatim condition");
        assertEquals(17, rockGtMaterials().size(), "the measured item truth (every GTStoneBlocks family: gear+rock+stone items all exist)");
        assertTrue(rockGtMaterials().contains("Marble"), "the representative family (the JSON identity leg)");
        assertFalse(rockGtMaterials().contains("Stone"), "the MT.Stone.NOT exclusion rides the face");
        assertFalse(rockGtMaterials().contains("Bedrock"), "the MT.Bedrock.NOT exclusion rides the face");
    }

    /** Form pin: the single :148 form verbatim — key, prefixes, count, the {"XYX","YfY","XYX"} grid. */
    @Test
    public void theRockGtFormCarriesTheUpstreamGrid() {
        assertEquals(1, GT6CraftingRecipes.rockGtCraftFromForms().size(), "the :148 single statement");
        GT6CraftingRecipes.RockGtCraftFromForm tForm = GT6CraftingRecipes.rockGtCraftFromForms().get(0);
        assertEquals("rock_gt2gear_gt", tForm.aKey(), "the composed key (the null upstream category, the snake law)");
        assertEquals(OP.gearGt, tForm.aOutput(), "the output prefix (count 1)");
        assertEquals(1, tForm.aCount(), "the :148 output amount");
        assertEquals(OP.rockGt, tForm.aInput(), "the X slot = rockGt (the 4 corners)");
        assertEquals(OP.stone, tForm.aCompanion(), "the Y slot = stone (the 4 edges, OP.java:1464)");
        assertArrayEquals(new String[] {"XYX", "YfY", "XYX"}, tForm.aRows(), "the :148 grid verbatim ('f' = the file tool center)");
    }

    /** The row id — the craftFromRowId form over the gt6 namespace (the shared id law). */
    @Test
    public void theRockGtRowIdIsTheFormKeyPlusMaterialLeaf() {
        assertEquals("gt6:rock_gt2gear_gt/marble", GT6CraftingRecipes.craftFromRowId("rock_gt2gear_gt", "marble").toString());
        assertEquals("gt6:rock_gt2gear_gt/granite_black", GT6CraftingRecipes.craftFromRowId("rock_gt2gear_gt", "granite_black").toString());
    }

    /** Identity face: the :148 gear row for Marble — 4x rock + 4x stone around the file, count=1 rides the vanilla key omission. */
    @Test
    public void theMarbleGearRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("rock_gt2gear_gt/marble");
        assertEquals(3, tRow.getAsJsonArray("pattern").size(), "the 3x3 frame");
        assertEquals("XYX", tRow.getAsJsonArray("pattern").get(0).getAsString(), "the :148 top row");
        assertEquals("YfY", tRow.getAsJsonArray("pattern").get(1).getAsString(), "the :148 middle row");
        assertEquals("XYX", tRow.getAsJsonArray("pattern").get(2).getAsString(), "the :148 bottom row");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.rockGt, material("Marble")), tRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the rockGt item (the corners)");
        assertEquals("gt6:marble", tRow.getAsJsonObject("key").get("Y").getAsJsonObject().get("item").getAsString(),
                "'Y' = the family STONE-variant block item (the bare-snake variant-0 path, the declared carrier deviation)");
        assertTrue(tRow.getAsJsonObject("key").get("f").getAsJsonObject().get("tag").getAsString().endsWith("tools/file"),
                "'f' = the file tool tag (upstream CR.java:231)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.gearGt, material("Marble")), tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
        int tCount = tRow.getAsJsonObject("result").has("count") ? tRow.getAsJsonObject("result").get("count").getAsInt() : 1;
        assertEquals(1, tCount, "the result count (the vanilla count=1 key omission)");
    }

    // ------------------------------------------------------------------
    // task craftfrom-residual — the 28 residual statements (the panel
    // closeout): five shaped bands + the shapeless panel
    // ------------------------------------------------------------------

    private static Set<String> gearMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.GearGtCraftFromMaterialRow tRow : GT6CraftingRecipes.gearGtCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count pin (the measured item truth): the :149 twin == the :148 rockGt universe, :150 the smith face, :151 the plank cut (live since task wood-planks-register). */
    @Test
    public void theGearRowCountIsTheMeasuredItemTruth() throws Exception {
        Set<String> tRockGtMaterials = rockGtMaterials();
        assertEquals(tRockGtMaterials, gearMaterialsOf("gear_gt/from_stick_stone"),
                "the :149 twin universe == the :148 rockGt universe (same gearGt/stone family faces + the verbatim five-condition tail)");
        assertEquals(206, gearMaterialsOf("gear_gt/from_stick_plate").size(), "the :150 smith face (gearGt ∩ stick ∩ plate + SMITHABLE)");
        // the :151 plank cut pours over the plank WOOD domain (task wood-planks-register): gearGtSmall
        // item truth ∩ the 130 plank pairs, minus MT.Wood.NOT / COATED.NOT / ANTIMATTER.NOT — the
        // remainder is a re-pin on material-tree or gate drift, so the count is re-read, not assumed
        Set<String> tPlankCut = gearMaterialsOf("gear_gt_small/from_plank");
        assertFalse(tPlankCut.contains("Wood"), "the :151 MT.Wood.NOT cut holds (the identity wood rides the :149/:150 faces)");
        assertTrue(tPlankCut.size() > 100, "the :151 plank cut pours over the registered plank domain (" + tPlankCut.size() + " rows)");
        // the generated face rides the same walk: the FIRST plank cut row verbatim (the
        // companion-less 2x2 grid used to be swallowed by the datagen item-truth guard —
        // this pin keeps the generated rows flush with the material face)
        GT6CraftingRecipes.GearGtCraftFromMaterialRow tFirstCut = GT6CraftingRecipes.gearGtCraftFromMaterialRows().stream()
                .filter(tRow -> tRow.aForm().aKey().equals("gear_gt_small/from_plank")).findFirst().orElseThrow();
        String tCutPath = "gear_gt_small/from_plank/" + GTMaterialItems.snakeCase(tFirstCut.aMaterial().mNameInternal);
        JsonObject tPlankRow = generated(tCutPath);
        assertEquals(2, tPlankRow.getAsJsonArray("pattern").size(), "the 2x2 plank cut");
        assertEquals("X ", tPlankRow.getAsJsonArray("pattern").get(0).getAsString(), "the :151 top row");
        assertEquals(" s", tPlankRow.getAsJsonArray("pattern").get(1).getAsString(), "the saw row");
        assertTrue(tPlankRow.getAsJsonObject("key").get("s").getAsJsonObject().get("tag").getAsString().endsWith("tools/saw"),
                "'s' = the saw tool tag (the :151 verbatim tail)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.plank, tFirstCut.aMaterial()), tPlankRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the plank item of the walked material");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.gearGtSmall, tFirstCut.aMaterial()), tPlankRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Universe SET pin: :150 == the conditioned gearGt ∩ stick ∩ plate faces under +SMITHABLE. */
    @Test
    public void theGearPlateUniverseIsTheSmithFace() {
        Set<String> tExpected = new HashSet<>(conditionedFace(OP.gearGt));
        tExpected.retainAll(conditionedFace(OP.stick));
        tExpected.retainAll(conditionedFace(OP.plate));
        tExpected.removeIf(tName -> !material(tName).contains(gregapi.data.TD.Processing.SMITHABLE)); // SMITHABLE — the positive smith face
        assertEquals(tExpected, gearMaterialsOf("gear_gt/from_stick_plate"), "the :150 universe == the three-face smith intersection");
    }

    /** Form pin: the three gear-face grids verbatim ({"XYX","YfY","XYX"} / {"XYX","YwY","XYX"} / {"X "," s"}), counts 1. */
    @Test
    public void theGearFormsCarryTheUpstreamGrids() {
        assertEquals(3, GT6CraftingRecipes.gearGtCraftFromForms().size(), "the :149-151 three statements");
        for (GT6CraftingRecipes.GearGtCraftFromForm tForm : GT6CraftingRecipes.gearGtCraftFromForms()) {
            assertEquals(1, tForm.aCount(), "the output amount (all three rows count 1): " + tForm.aKey());
            assertArrayEquals(switch (tForm.aKey()) {
                case "gear_gt/from_stick_stone" -> new String[] {"XYX", "YfY", "XYX"};
                case "gear_gt/from_stick_plate" -> new String[] {"XYX", "YwY", "XYX"};
                default -> new String[] {"X ", " s"};
            }, tForm.aRows(), "the grid verbatim: " + tForm.aKey());
        }
    }

    /** Identity face: the :150 plated-gear row for Iron — 4x stick + 4x plate around the wrench. */
    @Test
    public void theIronPlatedGearRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("gear_gt/from_stick_plate/iron");
        assertArrayEquals(new String[] {"XYX", "YwY", "XYX"}, new String[] {
                tRow.getAsJsonArray("pattern").get(0).getAsString(),
                tRow.getAsJsonArray("pattern").get(1).getAsString(),
                tRow.getAsJsonArray("pattern").get(2).getAsString()}, "the :150 grid verbatim");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.stick, material("Iron")), tRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the stick item (the corners)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.plate, material("Iron")), tRow.getAsJsonObject("key").get("Y").getAsJsonObject().get("item").getAsString(),
                "'Y' = the plate item (the edges)");
        assertTrue(tRow.getAsJsonObject("key").get("w").getAsJsonObject().get("tag").getAsString().endsWith("tools/wrench"),
                "'w' = the wrench tool tag (upstream CR.java:358)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.gearGt, material("Iron")), tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    private static Set<String> toolHeadMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.ToolHeadCraftFromMaterialRow tRow : GT6CraftingRecipes.toolHeadCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count + universe SET pins: the rotor/buzzSaw faces (the measured item truth + the live multi-face intersections). */
    @Test
    public void theToolHeadUniversesAreTheLiveMultiFaces() {
        // the rotor: rotor ∩ plateCurved ∩ plate ∩ screw under And(ANTIMATTER.NOT, COATED.NOT, SMITHABLE)
        Set<String> tRotor = new HashSet<>(conditionedFace(OP.rotor));
        tRotor.retainAll(conditionedFace(OP.plateCurved));
        tRotor.retainAll(conditionedFace(OP.plate));
        tRotor.retainAll(conditionedFace(OP.screw));
        tRotor.removeIf(tName -> !material(tName).contains(gregapi.data.TD.Processing.SMITHABLE)); // SMITHABLE
        assertEquals(tRotor, toolHeadMaterialsOf("rotor"), "the :145 universe == the four-face smith intersection");
        assertEquals(206, toolHeadMaterialsOf("rotor").size(), "the measured rotor face");
        // the buzzsaw blades: plate and plateGem bodies
        Set<String> tSawPlate = new HashSet<>(conditionedFace(OP.toolHeadBuzzSaw));
        tSawPlate.retainAll(conditionedFace(OP.plate));
        assertEquals(tSawPlate, toolHeadMaterialsOf("tool_head_buzz_saw/plate"), "the :146 universe == the buzzSaw ∩ plate faces");
        Set<String> tSawGem = new HashSet<>(conditionedFace(OP.toolHeadBuzzSaw));
        tSawGem.retainAll(conditionedFace(OP.plateGem));
        assertEquals(tSawGem, toolHeadMaterialsOf("tool_head_buzz_saw/gem"), "the :147 universe == the buzzSaw ∩ plateGem faces");
    }

    /** Form pin: the :145-147 grids verbatim. */
    @Test
    public void theToolHeadFormsCarryTheUpstreamGrids() {
        assertEquals(3, GT6CraftingRecipes.toolHeadCraftFromForms().size(), "the :145-147 three statements");
        for (GT6CraftingRecipes.ToolHeadCraftFromForm tForm : GT6CraftingRecipes.toolHeadCraftFromForms()) {
            assertEquals(1, tForm.aCount(), "the output amount: " + tForm.aKey());
            assertArrayEquals(switch (tForm.aKey()) {
                case "rotor" -> new String[] {"YhY", "TXf", "YdY"};
                case "tool_head_buzz_saw/plate" -> new String[] {"wPh", "P P", "fPx"};
                default -> new String[] {"wCh", "C C", "fCx"};
            }, tForm.aRows(), "the grid verbatim: " + tForm.aKey());
        }
    }

    /** Identity face: the :145 rotor row for Iron — curved-plate edges, screw+plate body, hammer/file/screwdriver tools. */
    @Test
    public void theIronRotorRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("rotor/iron");
        assertArrayEquals(new String[] {"YhY", "TXf", "YdY"}, new String[] {
                tRow.getAsJsonArray("pattern").get(0).getAsString(),
                tRow.getAsJsonArray("pattern").get(1).getAsString(),
                tRow.getAsJsonArray("pattern").get(2).getAsString()}, "the :145 grid verbatim");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.plateCurved, material("Iron")), tRow.getAsJsonObject("key").get("Y").getAsJsonObject().get("item").getAsString(),
                "'Y' = the curved plate (the edges)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.screw, material("Iron")), tRow.getAsJsonObject("key").get("T").getAsJsonObject().get("item").getAsString(),
                "'T' = the screw (the fixed letter)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.plate, material("Iron")), tRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the plate (the null-SpecialPrefix default :535)");
        assertTrue(tRow.getAsJsonObject("key").get("h").getAsJsonObject().get("tag").getAsString().endsWith("tools/hard_hammer"),
                "'h' = the HARD hammer tag (craftingToolHardHammer, the p25 ruling)");
        assertTrue(tRow.getAsJsonObject("key").get("d").getAsJsonObject().get("tag").getAsString().endsWith("tools/screwdriver"),
                "'d' = the screwdriver tag (upstream CR.java:342)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.rotor, material("Iron")), tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Row-count pin: the casingMachine forms UNLOCKED with the item family (task casing-machine-register registers the four prefixes on the item path, so the dormant :152-155 band pours). */
    @Test
    public void theCasingFormsPourWithTheItemFamily() {
        assertEquals(4, GT6CraftingRecipes.casingCraftFromForms().size(), "the :152-155 four statements");
        assertEquals(836, GT6CraftingRecipes.casingCraftFromMaterialRows().size(),
                "the :152-155 rows pour 4x209 = 836 — the item-truth intersection over the registered quartet (210 items per family, one material lacks the plate/stickLong truth)");
        assertEquals(209, GT6CraftingRecipes.casingCraftFromMaterialRows().stream().filter(r -> r.aForm().aKey().equals("casing_machine")).count(), "the :152 casing_machine face");
        assertEquals(209, GT6CraftingRecipes.casingCraftFromMaterialRows().stream().filter(r -> r.aForm().aKey().equals("casing_machine_double")).count(), "the :153 casing_machine_double face");
        assertEquals(209, GT6CraftingRecipes.casingCraftFromMaterialRows().stream().filter(r -> r.aForm().aKey().equals("casing_machine_quadruple")).count(), "the :154 casing_machine_quadruple face");
        assertEquals(209, GT6CraftingRecipes.casingCraftFromMaterialRows().stream().filter(r -> r.aForm().aKey().equals("casing_machine_dense")).count(), "the :155 casing_machine_dense face");
        for (GT6CraftingRecipes.CasingCraftFromForm tForm : GT6CraftingRecipes.casingCraftFromForms()) {
            assertArrayEquals(new String[] {"YXX", "XwX", "XXY"}, tForm.aRows(), "the :152-155 grid verbatim: " + tForm.aKey());
        }
        // the verbatim condition tail: ANTIMATTER.NOT only — no ANTIMATTER material rides any row
        for (GT6CraftingRecipes.CasingCraftFromMaterialRow tRow : GT6CraftingRecipes.casingCraftFromMaterialRows()) {
            assertFalse(tRow.aMaterial().contains(gregapi.data.TD.Atomic.ANTIMATTER), "ANTIMATTER.NOT rides every row: " + tRow.aMaterial().mNameInternal);
        }
    }

    private static Set<String> smallPartMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.SmallPartCraftFromMaterialRow tRow : GT6CraftingRecipes.smallPartCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count pin (the measured item truth): the five small-parts faces. */
    @Test
    public void theSmallPartRowCountIsTheMeasuredItemTruth() {
        assertEquals(730, smallPartMaterialsOf("stick2bolt").size(), "the :164 bolt face (bolt ∩ stick, minus Wood)");
        assertEquals(731, smallPartMaterialsOf("bolt2screw").size(), "the :165 screw face (screw ∩ bolt)");
        assertEquals(132, smallPartMaterialsOf("gem2ring").size(), "the :166 ring face (ring ∩ gem)");
        assertEquals(339, smallPartMaterialsOf("chunk2round").size(), "the :167 round face (round ∩ chunkGt)");
        assertEquals(558, smallPartMaterialsOf("plate2plate_tiny/regular").size(), "the :170 plateTiny face (plateTiny ∩ plate, minus Paper/Wood)");
        assertFalse(smallPartMaterialsOf("stick2bolt").contains("Wood"), "the MT.Wood.NOT exclusion rides the bolt face");
        assertFalse(smallPartMaterialsOf("plate2plate_tiny/regular").contains("Paper"), "the MT.Paper.NOT exclusion rides the plateTiny face");
        assertFalse(smallPartMaterialsOf("plate2plate_tiny/regular").contains("Wood"), "the MT.Wood.NOT exclusion rides the plateTiny face");
    }

    /** Universe SET pin: every small-parts form == the conditioned output ∩ input faces minus the verbatim exclusions. */
    @Test
    public void theSmallPartUniversesAreTheLivePrefixFaces() {
        for (GT6CraftingRecipes.SmallPartCraftFromForm tForm : GT6CraftingRecipes.smallPartCraftFromForms()) {
            Set<String> tExpected = new HashSet<>(conditionedFace(tForm.aOutput()));
            tExpected.retainAll(conditionedFace(tForm.aInput()));
            if (tForm.aNoWood()) tExpected.remove("Wood"); // MT.Wood.NOT
            if (tForm.aNoPaper()) tExpected.remove("Paper"); // MT.Paper.NOT
            assertEquals(tExpected, smallPartMaterialsOf(tForm.aKey()),
                    "the universe == the conditioned " + tForm.aOutput().mNameInternal + " ∩ " + tForm.aInput().mNameInternal + " faces: " + tForm.aKey());
        }
    }

    /** Amount + grid pins: the :164-167/:170 amounts and frames verbatim. */
    @Test
    public void theSmallPartFormsCarryTheUpstreamGrids() {
        assertEquals(5, GT6CraftingRecipes.smallPartCraftFromForms().size(), "the five statements");
        for (GT6CraftingRecipes.SmallPartCraftFromForm tForm : GT6CraftingRecipes.smallPartCraftFromForms()) {
            assertEquals(switch (tForm.aKey()) {
                case "stick2bolt" -> 2; // :164
                case "bolt2screw" -> 1; // :165
                case "gem2ring" -> 1; // :166
                case "chunk2round" -> 1; // :167
                default -> 8; // :170
            }, tForm.aCount(), "the output amount: " + tForm.aKey());
            assertArrayEquals(switch (tForm.aKey()) {
                case "stick2bolt" -> new String[] {"s ", " S"};
                case "bolt2screw" -> new String[] {"fX", "X "};
                case "gem2ring", "chunk2round" -> new String[] {"f ", " X"};
                default -> new String[] {"s ", " P"};
            }, tForm.aRows(), "the grid verbatim: " + tForm.aKey());
        }
    }

    /** Identity face: the :164 bolt row for Iron — 'S' = the FIXED stick letter, saw top row, count 2. */
    @Test
    public void theIronBoltRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("stick2bolt/iron");
        assertEquals("s ", tRow.getAsJsonArray("pattern").get(0).getAsString(), "the :164 saw row");
        assertEquals(" S", tRow.getAsJsonArray("pattern").get(1).getAsString(), "the :164 stick row ('S' = the fixed letter, :520-533)");
        assertTrue(tRow.getAsJsonObject("key").get("S").getAsJsonObject().get("item").getAsString().equals("gt6:" + GTMaterialItems.itemIdOf(OP.stick, material("Iron"))),
                "'S' = the stick item (the fixed letter)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.bolt, material("Iron")), tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
        assertEquals(2, tRow.getAsJsonObject("result").get("count").getAsInt(), "the :164 output amount");
    }

    /** Row-count pin: the minecartWheels face (the measured item truth, ANTIMATTER.NOT only — no COATED leg upstream). */
    @Test
    public void theMinecartWheelsRowCountIsTheMeasuredItemTruth() {
        Set<String> tExpected = new HashSet<>();
        Set<String> tWheels = new HashSet<>(), tRing = new HashSet<>(), tStick = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.minecartWheels) tWheels.add(tPair.material().mNameInternal);
            if (tPair.prefix() == OP.ring) tRing.add(tPair.material().mNameInternal);
            if (tPair.prefix() == OP.stick) tStick.add(tPair.material().mNameInternal);
        }
        tExpected.addAll(tWheels);
        tExpected.retainAll(tRing);
        tExpected.retainAll(tStick);
        tExpected.removeIf(tName -> material(tName).contains(gregapi.data.TD.Atomic.ANTIMATTER)); // ANTIMATTER.NOT — the ONLY leg
        assertEquals(1, GT6CraftingRecipes.minecartWheelsCraftFromForms().size(), "the :179 single statement");
        assertArrayEquals(new String[] {" h ", "XSX", " w "}, GT6CraftingRecipes.minecartWheelsCraftFromForms().get(0).aRows(), "the :179 grid verbatim");
        assertEquals(209, GT6CraftingRecipes.minecartWheelsCraftFromMaterialRows().size(), "the measured wheels face (wheels ∩ ring ∩ stick)");
        assertEquals(tExpected, materialsOfMinecart(), "the universe == the bare three-face intersection minus ANTIMATTER");
    }

    private static Set<String> materialsOfMinecart() {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.MinecartWheelsCraftFromMaterialRow tRow : GT6CraftingRecipes.minecartWheelsCraftFromMaterialRows()) {
            rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count pin: the shapeless panel (the measured item truth; the cable faces pour ZERO — the wire/cable block domain). */
    @Test
    public void theShapelessRowCountIsTheMeasuredItemTruth() {
        assertEquals(12, GT6CraftingRecipes.shapelessCraftFromForms().size(), "the :181-192 twelve statements");
        assertEquals(539, shapelessMaterialsOf("arrows_wooden").size(), "the :181 wooden arrow face");
        assertEquals(539, shapelessMaterialsOf("arrows_plastic").size(), "the :182 light arrow face");
        assertEquals(0, shapelessMaterialsOf("cable_gt01/from_wire_gt01").size(),
                "the :183 rows pour zero — the wireGt01 INPUT face exists since task wire-gt-registration, but cableGt01 has no MaterialPrefixItems (still the block domain; the output face is the missing half, rows unlock with that item family)");
        assertEquals(0, shapelessMaterialsOf("cable_gt02/from_wire_gt02").size(),
                "the :184 rows pour zero — the same seam (the cableGt02 output face is the missing half)");
        assertEquals(1096, shapelessMaterialsOf("chemtube/from_dust_tiny").size(), "the :185 glass-tube face (unconditional)");
        assertEquals(1068, shapelessMaterialsOf("dust_tiny/from_chemtube").size(), "the :186 melt face (meltmin 293)");
        assertEquals(309, shapelessMaterialsOf("tool_head_raw_universal_spade/from_shovel").size(), "the :187 face");
        assertEquals(309, shapelessMaterialsOf("tool_head_raw_universal_spade/from_spade").size(), "the :188 face");
        assertEquals(309, shapelessMaterialsOf("tool_head_construction_pickaxe/from_raw_pickaxe").size(), "the :189 face");
        assertEquals(95, shapelessMaterialsOf("tool_head_pickaxe_gem/from_raw_any_iron").size(), "the :190 face");
        assertEquals(95, shapelessMaterialsOf("tool_head_pickaxe_gem/from_any_iron").size(), "the :191 face");
        assertEquals(95, shapelessMaterialsOf("tool_head_pickaxe_gem/retip").size(), "the :192 face — the fixed MT.Empty head slot exists since the :621 force landed (task toolhead-family-closeout); the rows ride the same toolHeadPickaxeGem ∩ gemFlawed face as :191");
    }

    private static Set<String> shapelessMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.ShapelessCraftFromMaterialRow tRow : GT6CraftingRecipes.shapelessCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Universe SET pins: the :186 meltmin face and the :190 gem-flawed face against the live intersections. */
    @Test
    public void theShapelessUniversesAreTheLivePrefixFaces() {
        // :186 — chemtube ∩ dustTiny under meltmin(DEF_ENV_TEMP = 293): NO COATED/ANTIMATTER legs upstream,
        // so the expectation rides the BARE registrationOrder faces (the conditionedFace helper would strip
        // the ANTIMATTER materials the upstream condition keeps)
        Set<String> tMelt = new HashSet<>();
        Set<String> tChemtube = new HashSet<>(), tDustTiny = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.chemtube) tChemtube.add(tPair.material().mNameInternal);
            if (tPair.prefix() == OP.dustTiny) tDustTiny.add(tPair.material().mNameInternal);
        }
        tMelt.addAll(tChemtube);
        tMelt.retainAll(tDustTiny);
        tMelt.removeIf(tName -> material(tName).mMeltingPoint < 293); // meltmin(DEF_ENV_TEMP), CS.java:135
        assertEquals(tMelt, shapelessMaterialsOf("dust_tiny/from_chemtube"), "the :186 universe == the bare meltmin face");
        // :187 — rawUniversalSpade ∩ toolHeadShovel under And(ANTIMATTER.NOT, COATED.NOT)
        Set<String> tSpade = new HashSet<>(conditionedFace(OP.toolHeadRawUniversalSpade));
        tSpade.retainAll(conditionedFace(OP.toolHeadShovel));
        assertEquals(tSpade, shapelessMaterialsOf("tool_head_raw_universal_spade/from_shovel"), "the :187 universe == the spade-shovel faces");
        // :192 — the retip face pours since the :621 force landed (task toolhead-family-closeout):
        // the fixed MT.Empty head slot exists now; the rows still ride the gemFlawed item truth
        // (the Empty head itself stays row-less — gemFlawed(Empty) resolves nothing, the
        // never-unresolvable-ingredient law; upstream shapes the same face: the :192 listener
        // would fire gemFlawed.dat(Empty) = a dead 1.7.10 row)
        assertTrue(GT6CraftingRecipes.itemPairExists(OP.toolHeadPickaxeGem, gregapi.data.MT.Empty), "the :192 fixed MT.Empty retip head item exists (the :621 force, the seam closed)");
        Set<String> tRetipFace = new HashSet<>(conditionedFace(OP.toolHeadPickaxeGem));
        tRetipFace.retainAll(conditionedFace(OP.gemFlawed));
        assertEquals(tRetipFace, shapelessMaterialsOf("tool_head_pickaxe_gem/retip"), "the :192 universe == the pickaxeGem ∩ gemFlawed conditioned face (the blank head excluded — no gemFlawed(Empty) item)");
        assertTrue(GT6CraftingRecipes.itemPairExists(OP.arrowGtWood, gregapi.data.MT.Empty), "the :181 fixed MT.Empty shaft item exists");
    }

    /** Amount + condition pins: the :181-192 amounts (all 1) and the condition kinds upstream verbatim. */
    @Test
    public void theShapelessFormsCarryTheUpstreamConditions() {
        for (GT6CraftingRecipes.ShapelessCraftFromForm tForm : GT6CraftingRecipes.shapelessCraftFromForms()) {
            assertEquals(1, tForm.aCount(), "the output amount (every shapeless statement outputs 1): " + tForm.aKey());
            assertEquals(switch (tForm.aKey()) {
                case "tool_head_raw_universal_spade/from_shovel", "tool_head_raw_universal_spade/from_spade",
                        "tool_head_construction_pickaxe/from_raw_pickaxe" -> GT6CraftingRecipes.COND_COATED_ANTIMATTER; // :187-189
                case "chemtube/from_dust_tiny" -> GT6CraftingRecipes.COND_TRUE; // :185
                case "dust_tiny/from_chemtube" -> GT6CraftingRecipes.COND_MELT_MIN_ENV; // :186
                default -> GT6CraftingRecipes.COND_ANTIMATTER; // :181-184/:190-192
            }, tForm.aCondition(), "the condition kind: " + tForm.aKey());
        }
    }

    /** Identity face: the :181 wooden-arrow row for Iron — head + the MT.Empty shaft, the SHAPELESS type. */
    @Test
    public void theIronWoodenArrowRowCarriesTheUpstreamShape() throws Exception {
        JsonObject tRow = generated("arrows_wooden/iron");
        assertEquals("minecraft:crafting_shapeless", tRow.get("type").getAsString(), "the shapeless type");
        assertEquals(2, tRow.getAsJsonArray("ingredients").size(), "the :181 two slots (head + shaft)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.toolHeadArrow, material("Iron")),
                tRow.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString(),
                "the bare toolHeadArrow slot (the :501-502 .dat(m) substitution)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.arrowGtWood, gregapi.data.MT.Empty),
                tRow.getAsJsonArray("ingredients").get(1).getAsJsonObject().get("item").getAsString(),
                "the fixed arrowGtWood.dat(MT.Empty) shaft slot");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.arrowGtWood, material("Iron")), tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :190 gem-tipped row — 2x flawed gem + the any-iron raw head (the multi-item group ingredient) + 3 tool tags. */
    @Test
    public void theAmethystPickaxeGemRowCarriesTheUpstreamShape() throws Exception {
        String tFormKey = "tool_head_pickaxe_gem/from_raw_any_iron";
        assertTrue(shapelessMaterialsOf(tFormKey).contains("Amethyst"), "the representative material rides the :190 face");
        JsonObject tRow = generated(tFormKey + "/amethyst");
        assertEquals("minecraft:crafting_shapeless", tRow.get("type").getAsString(), "the shapeless type");
        assertEquals(6, tRow.getAsJsonArray("ingredients").size(), "the :190 six slots (2x gem + 1x any-iron head + 3x tools)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.gemFlawed, material("Amethyst")),
                tRow.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString(), "the first flawed-gem slot");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.gemFlawed, material("Amethyst")),
                tRow.getAsJsonArray("ingredients").get(1).getAsJsonObject().get("item").getAsString(), "the second flawed-gem slot (the doubled :190 slot)");
        // the vanilla serialization of a multi-item Ingredient is a JSON ARRAY of {"item":...} entries
        assertTrue(tRow.getAsJsonArray("ingredients").get(2).isJsonArray(), "the any-iron head rides the multi-item group ingredient (the vanilla ingredient array form)");
        boolean tHeadFound = false;
        for (var tItem : tRow.getAsJsonArray("ingredients").get(2).getAsJsonArray()) {
            if (tItem.getAsJsonObject().get("item").getAsString().equals("gt6:" + GTMaterialItems.itemIdOf(OP.toolHeadRawPickaxe, material("Iron")))) tHeadFound = true;
        }
        assertTrue(tHeadFound, "the group covers the iron raw pickaxe head (the ANY.Iron members)");
        assertTrue(tRow.getAsJsonArray("ingredients").get(3).getAsJsonObject().get("tag").getAsString().endsWith("tools/file"), "the file slot");
        assertTrue(tRow.getAsJsonArray("ingredients").get(4).getAsJsonObject().get("tag").getAsString().endsWith("tools/hard_hammer"), "the hammer slot");
        assertTrue(tRow.getAsJsonArray("ingredients").get(5).getAsJsonObject().get("tag").getAsString().endsWith("tools/saw"), "the saw slot");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(OP.toolHeadPickaxeGem, material("Amethyst")), tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    // ------------------------------------------------------------------
    // task toolhead-r11a-file-belt — the raw→finished tool-head file belt
    // (Loader_Recipes_Handlers.java:420-434 — the OreProcessing_CraftFrom
    // listener half the p37 panel census missed by circling
    // Loader_OreProcessing.java only: 12 one-row families + the arrow trio,
    // all the {"X ", " f"} grid)
    // ------------------------------------------------------------------

    private static Set<String> fileBeltMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.FileBeltCraftFromMaterialRow tRow : GT6CraftingRecipes.fileBeltCraftFromMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Row-count pin (the measured item truth): 6313 = the arrow trio (95 chipped + 79 rock + 539 raw) + 4x312 + 8x544 family faces. */
    @Test
    public void theFileBeltRowCountIsTheMeasuredItemTruth() {
        assertEquals(6313, GT6CraftingRecipes.fileBeltCraftFromMaterialRows().size(),
                "95 + 79 + 539 + 4x312 (saw/chisel/universalSpade/axeDouble) + 8x544 (sword/pickaxe/shovel/spade/axe/hoe/sense/plow)");
        assertEquals(95, fileBeltMaterialsOf("tool_head_arrow/from_gem_chipped").size(), "the :420 chipped face (the flawed-gem-tier face, the :190 twin)");
        assertEquals(79, fileBeltMaterialsOf("tool_head_arrow/from_rock_gt").size(), "the :421 rock face (the STONE positive leg)");
        assertEquals(539, fileBeltMaterialsOf("tool_head_arrow/from_raw_arrow").size(), "the :422 raw-arrow face (== the whole toolHeadArrow face, the :181 twin)");
        assertEquals(312, fileBeltMaterialsOf("tool_head_saw/from_raw_saw").size(), "the :423 saw face");
        assertEquals(544, fileBeltMaterialsOf("tool_head_sword/from_raw_sword").size(), "the :425 sword face");
    }

    /** Universe SET pin: every form's rows == the bare output ∩ input item-truth faces under the verbatim condition legs. */
    @Test
    public void theFileBeltUniversesAreTheLivePrefixFaces() {
        for (GT6CraftingRecipes.FileBeltCraftFromForm tForm : GT6CraftingRecipes.fileBeltCraftFromForms()) {
            Set<String> tExpected = new HashSet<>();
            Set<String> tOutputs = new HashSet<>(), tInputs = new HashSet<>();
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
                if (tPair.prefix() == tForm.aOutput()) tOutputs.add(tPair.material().mNameInternal);
                if (tPair.prefix() == tForm.aInput()) tInputs.add(tPair.material().mNameInternal);
            }
            tExpected.addAll(tOutputs);
            tExpected.retainAll(tInputs);
            tExpected.removeIf(tName -> switch (tForm.aCondition()) {
                case GT6CraftingRecipes.COND_COATED_ANTIMATTER -> material(tName).contains(gregapi.data.TD.Atomic.ANTIMATTER)
                        || material(tName).contains(gregapi.data.TD.Compounds.COATED); // :420
                case GT6CraftingRecipes.COND_COATED_ANTIMATTER_STONE -> material(tName).contains(gregapi.data.TD.Atomic.ANTIMATTER)
                        || material(tName).contains(gregapi.data.TD.Compounds.COATED)
                        || !material(tName).contains(gregapi.data.TD.Properties.STONE); // :421
                default -> material(tName).contains(gregapi.data.TD.Atomic.ANTIMATTER); // :422-434
            });
            assertEquals(tExpected, fileBeltMaterialsOf(tForm.aKey()),
                    "the universe == the output ∩ input faces under the verbatim condition: " + tForm.aKey());
        }
    }

    /** Amount pin: the :420-434 output amounts verbatim (2/8/1 + twelve 1s). */
    @Test
    public void theFileBeltAmountsAreTheUpstreamVerbatim() {
        for (GT6CraftingRecipes.FileBeltCraftFromForm tForm : GT6CraftingRecipes.fileBeltCraftFromForms()) {
            assertEquals(switch (tForm.aKey()) {
                case "tool_head_arrow/from_gem_chipped" -> 2; // :420
                case "tool_head_arrow/from_rock_gt" -> 8; // :421
                default -> 1; // :422-434
            }, tForm.aCount(), "the output amount: " + tForm.aKey());
        }
    }

    /** The row id — the craftFromRowId form over the gt6 namespace (the shared id law). */
    @Test
    public void theFileBeltRowIdIsTheFormKeyPlusMaterialLeaf() {
        assertEquals("gt6:tool_head_saw/from_raw_saw/iron", GT6CraftingRecipes.craftFromRowId("tool_head_saw/from_raw_saw", "iron").toString());
        assertEquals("gt6:tool_head_arrow/from_rock_gt/andesite", GT6CraftingRecipes.craftFromRowId("tool_head_arrow/from_rock_gt", "andesite").toString());
    }
}
