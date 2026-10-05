/**
 * Offline census for task issue7-kitchen-models — the kitchen family's render wave.
 * The four manual kitchen blocks (GT6Kitchen.java:89-113) rendered as the magenta-black
 * missing-model checkerboard: zero blockstates/models/item-models in the generated tree.
 * This census pins the fix end to end (the {@link GT6StoneBlocksRenderDatagenTest} split):
 * <ul>
 * <li>the 18-PNG kitchen borrow tree (block/tools/, the upstream colored/ tile sets)
 *     plus the declared non-kitchen tool-tree residents ({@link #OTHER_TOOL_TREE_RESIDENTS})
 *     is 1:1 with the declared face sets — zero strays, zero gaps;</li>
 * <li>every borrowed PNG is grounded in assets/README.md by basename AND by the actual
 *     sha256 of its bytes (the p31 attribution-nail pattern — the name check alone can
 *     pass while the prose describes a different file);</li>
 * <li>the four generated blockstates are property-free single-state rows;</li>
 * <li>the four generated block models pin the upstream tub geometry (the 2px walls at
 *     8px tall + the 2px base slab; the juicer's low 4px tub + the 4x7x4 pestle column)
 *     and carry tintindex 0 on EVERY face (the reserved mRGBa seam — no BlockColor is
 *     registered, the family runtime-tint pool);</li>
 * <li>the four BlockItem models parent their own block model.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6KitchenRenderDatagenTest {

    /** block id → texture family (the steel pot's model is its own; the family is the upstream texture band). */
    private static final Map<String, String> BLOCK_FAMILIES = Map.of(
            "bathing_pot_wood", "bathing_pot_wood",
            "bathing_pot_steel", "bathing_pot",
            "mixing_bowl", "mixing_bowl",
            "juicer", "juicer");

    /**
     * The grindstone's colored/overlay subdir band (task grindstone-family — the
     * MultiTileEntityGrindStone getTexture2 :238-241 eight-tile borrow). OUTSIDE
     * {@link #BLOCK_FAMILIES}: that map also feeds the property-free blockstate pin and
     * the grindstone is a FACING x STONE state machine, not a kitchen vessel. The faces
     * are SUBDIR-QUALIFIED — the family root carries the two pass bands, not flat tiles.
     */
    private static final String GRINDSTONE_FAMILY = "grindstone";
    private static final List<String> GRINDSTONE_FACES = List.of(
            "colored/legs", "colored/axle", "colored/stone", "colored/bottom",
            "overlay/legs", "overlay/axle", "overlay/stone", "overlay/bottom");

    /** The tub faces (pot pair + bowl) and the juicer's extra pestle faces. */
    private static final List<String> TUB_FACES = List.of("sides", "insides", "top", "bottom");
    private static final List<String> JUICER_FACES = List.of("sides", "insides", "top", "bottom", "middletop", "middleside");

    /** 4 + 4 + 4 + 6 + 2 + 8 — the walk upstream found exactly these per family; the kitchen_nei pair is the manual-nei derived art at the tools root (the Table faces stay unborrowed). */
    private static final int PINNED_PNG_TOTAL = 28;

    /** The manual-nei viewer-dynamic pair — port-GENERATED art sitting directly at the tools root (no family subdir). */
    private static final List<String> ROOT_DERIVED_TILES = List.of("kitchen_nei_jei.png", "kitchen_nei_emi.png");

    /**
     * The non-kitchen residents of the SHARED {@code block/tools} borrow tree (the census
     * walks the whole directory, so every later band must be declared here): the two
    /**
     * The non-kitchen residents of the SHARED {@code block/tools} borrow tree (the census
     * walks the whole directory, so every later band must be declared here): the mortar
     * 12-PNG two-layer band (task mortar-family — colored+overlay x sides/insides/top/
     * bottom/middleside/middletop) + the sifting-table 8-PNG two-layer band (task
     * sifting-table-family — colored+overlay x legs/grid/border/plate). The grindstone
     * sibling extends this list at its own card — with the sifting band (+8) landed the
     * walk is 28 + 20 = 48 (the grindstone sibling's union: the kitchen_nei pair rides the
     * ROOT_DERIVED_TILES pin inside the 28, the sifting band rides the 3-shape keys —
     * family dir leads — because the two-level colored/overlay subdirs collide in the old
     * two-shape key space).
     */
    private static final List<String> OTHER_TOOL_TREE_RESIDENTS = List.of(
            "mortar/sides.png", "mortar/insides.png", "mortar/top.png",
            "mortar/bottom.png", "mortar/middleside.png", "mortar/middletop.png",
            "mortar_overlay/sides.png", "mortar_overlay/insides.png", "mortar_overlay/top.png",
            "mortar_overlay/bottom.png", "mortar_overlay/middleside.png", "mortar_overlay/middletop.png",
            "sifting_table/colored/legs.png", "sifting_table/colored/grid.png",
            "sifting_table/colored/border.png", "sifting_table/colored/plate.png",
            "sifting_table/overlay/legs.png", "sifting_table/overlay/grid.png",
            "sifting_table/overlay/border.png", "sifting_table/overlay/plate.png");

    private static List<String> declaredFaces(String aBlockId) {
        return aBlockId.equals("juicer") ? JUICER_FACES : TUB_FACES;
    }

    /** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
        }
        throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** One committed generated-tree JSON as an object (the test classpath carries src/generated/resources). */
    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6KitchenRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * PNG census, POSITIVE side: every declared (family, face) has its borrowed PNG under
     * {@code textures/block/tools/<family>/} (18/18 + the grindstone 8/8, zero gaps).
     */
    @Test
    void everyDeclaredFaceHasItsBorrowedPng() {
        for (Map.Entry<String, String> tRow : BLOCK_FAMILIES.entrySet()) {
            for (String tFace : declaredFaces(tRow.getKey())) {
                assertNotNull(GT6KitchenRenderDatagenTest.class.getResource(
                        "/assets/gt6/textures/block/tools/" + tRow.getValue() + "/" + tFace + ".png"),
                        tRow.getValue() + "/" + tFace + ".png must be borrowed");
            }
        }
        for (String tFace : GRINDSTONE_FACES) {
            assertNotNull(GT6KitchenRenderDatagenTest.class.getResource(
                    "/assets/gt6/textures/block/tools/" + GRINDSTONE_FAMILY + "/" + tFace + ".png"),
                    GRINDSTONE_FAMILY + "/" + tFace + ".png must be borrowed");
        }
    }

    /**
     * PNG census, NEGATIVE side: the borrow tree hosts exactly the 28 pinned files (the
     * 18 kitchen + the kitchen_nei root pair + the grindstone subdir band) + the declared
     * non-kitchen tool-tree residents — no strays. The walk roots at the
     * mdk SOURCE tree (the mdkRoot() walk, the GT6GrindstoneNeiModelTest form): the neo
     * test classpath overlays a resources root whose getResource URL does not resolve to
     * the tools dir, so the classpath-URI root is leg-dependent — the source tree is not.
     */
    @Test
    void borrowedPngTreeIsExactlyTheDeclaredSet() throws Exception {
        Path tRoot = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/tools");
        assertTrue(Files.isDirectory(tRoot), "the tools borrow root must exist: " + tRoot);
        Set<String> tFound = new HashSet<>();
        try (var tWalk = Files.walk(tRoot)) {
            tWalk.filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                Path tParent = p.getParent();
                // three key shapes: the bare root tiles (kitchen_nei_*), the flat family
                // dirs (juicer/sides.png) and the two-level subdir bands
                // (grindstone/colored/legs.png — the FAMILY dir leads, collision-free)
                String tKey;
                if (tParent.equals(tRoot)) tKey = p.getFileName().toString();
                else if (tParent.getParent().equals(tRoot)) tKey = tParent.getFileName() + "/" + p.getFileName();
                else tKey = tParent.getParent().getFileName() + "/" + tParent.getFileName() + "/" + p.getFileName();
                tFound.add(tKey);
            });
        }
        assertEquals(PINNED_PNG_TOTAL + OTHER_TOOL_TREE_RESIDENTS.size(), tFound.size(),
                "28 pinned PNGs (kitchen + root NEI pair + grindstone) + the declared non-kitchen residents, walked");
        Set<String> tDeclared = new HashSet<>(ROOT_DERIVED_TILES);
        for (Map.Entry<String, String> tRow : BLOCK_FAMILIES.entrySet()) {
            for (String tFace : declaredFaces(tRow.getKey())) {
                tDeclared.add(tRow.getValue() + "/" + tFace + ".png");
            }
        }
        tDeclared.addAll(OTHER_TOOL_TREE_RESIDENTS);
        for (String tFace : GRINDSTONE_FACES) {
            tDeclared.add(GRINDSTONE_FAMILY + "/" + tFace + ".png");
        }
        assertEquals(tDeclared, tFound, "the borrow is 1:1 with the declared face sets — zero strays, zero gaps");
    }

    /**
     * The attribution nail (the p31 wave pattern): every borrowed PNG is named in
     * assets/README.md AND its actual file bytes hash to a sha256 the ledger records.
     */
    @Test
    void borrowedPngsAreGroundedInTheAssetsLedger() throws Exception {
        String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
        MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
        List<String> tViolations = new ArrayList<>();
        try (var tWalk = Files.walk(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/tools"))) {
            List<Path> tPngs = tWalk.filter(p -> p.toString().endsWith(".png")).toList();
            for (Path tPng : tPngs) {
                String tName = tPng.getFileName().toString();
                String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
                if (!tReadme.contains(tName)) tViolations.add(tName + " — filename absent from assets/README.md");
                if (!tReadme.contains(tHex)) tViolations.add(tName + " — bytes hash to " + tHex + ", not grounded in the ledger");
            }
        }
        assertTrue(tViolations.isEmpty(), "borrowed kitchen texture without attribution: " + tViolations);
    }

    /** The four generated blockstates: property-free blocks, the lone "" row pointing at gt6:block/&lt;id&gt;. */
    @Test
    void generatedBlockstatesAreSingleState() throws Exception {
        for (String tBlockId : BLOCK_FAMILIES.keySet()) {
            JsonObject tState = generatedJson("assets/gt6/blockstates/" + tBlockId + ".json");
            assertTrue(tState.has("variants"), tBlockId + ": the plain-variants form (no multipart)");
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(1, tVariants.size(), tBlockId + ": exactly the default \"\" row (property-free block)");
            JsonObject tModel = tVariants.getAsJsonObject("");
            assertEquals("gt6:block/" + tBlockId, tModel.get("model").getAsString(),
                    tBlockId + ": the row points at the block's own model");
        }
    }

    /** Every face of every element carries tintindex 0 — the reserved mRGBa seam (no BlockColor registered yet). */
    private static void assertAllFacesTinted(JsonObject aModel, String aLabel) {
        for (var tElement : aModel.getAsJsonArray("elements")) {
            JsonObject tFaces = tElement.getAsJsonObject().getAsJsonObject("faces");
            for (String tDir : tFaces.keySet()) {
                JsonObject tFace = tFaces.getAsJsonObject(tDir);
                assertTrue(tFace.has("tintindex") && tFace.get("tintindex").getAsInt() == 0,
                        aLabel + ": face " + tDir + " carries tintindex 0");
            }
        }
    }

    /** The tub geometry pin: the 2px base slab + four 8px-tall wall panels (the upstream pot boxes :345-356). */
    @Test
    void tubModelsPinTheHollowTubGeometry() throws Exception {
        for (String tBlockId : List.of("bathing_pot_wood", "bathing_pot_steel", "mixing_bowl")) {
            JsonObject tModel = generatedJson("assets/gt6/models/block/" + tBlockId + ".json");
            JsonArray tElements = tModel.getAsJsonArray("elements");
            assertEquals(5, tElements.size(), tBlockId + ": base slab + four wall panels");
            int tWalls = 0;
            boolean tBase = false;
            for (var tElement : tElements) {
                JsonArray tFrom = tElement.getAsJsonObject().getAsJsonArray("from");
                JsonArray tTo = tElement.getAsJsonObject().getAsJsonArray("to");
                if (tFrom.get(1).getAsFloat() == 2.0F && tTo.get(1).getAsFloat() == 8.0F) {
                    tWalls++;
                    float tThicknessX = tTo.get(0).getAsFloat() - tFrom.get(0).getAsFloat();
                    float tThicknessZ = tTo.get(2).getAsFloat() - tFrom.get(2).getAsFloat();
                    assertTrue(Math.min(tThicknessX, tThicknessZ) == 2.0F,
                            tBlockId + ": the wall panel is 2px thick");
                } else if (tFrom.get(1).getAsFloat() == 0.0F && tTo.get(1).getAsFloat() == 2.0F) {
                    tBase = true;
                }
            }
            assertEquals(4, tWalls, tBlockId + ": four wall panels, 2px thick x 8px tall");
            assertTrue(tBase, tBlockId + ": the 2px base slab (its up face is the cavity floor)");
            assertAllFacesTinted(tModel, tBlockId);
        }
    }

    /** The juicer geometry pin: the 1px base, the four 4px walls, the central 4x7x4 pestle column (upstream :228-237). */
    @Test
    void juicerModelPinsTheLowTubAndPestleGeometry() throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/block/juicer.json");
        JsonArray tElements = tModel.getAsJsonArray("elements");
        assertEquals(6, tElements.size(), "juicer: base slab + four walls + the pestle column");
        int tWalls = 0;
        boolean tBase = false;
        boolean tPestle = false;
        for (var tElement : tElements) {
            JsonArray tFrom = tElement.getAsJsonObject().getAsJsonArray("from");
            JsonArray tTo = tElement.getAsJsonObject().getAsJsonArray("to");
            if (tFrom.get(1).getAsFloat() == 0.0F && tTo.get(1).getAsFloat() == 4.0F) {
                tWalls++;
            } else if (tFrom.get(1).getAsFloat() == 0.0F && tTo.get(1).getAsFloat() == 7.0F) {
                assertEquals(6.0F, tFrom.get(0).getAsFloat(), "juicer: the pestle column x-min");
                assertEquals(6.0F, tFrom.get(2).getAsFloat(), "juicer: the pestle column z-min");
                assertEquals(10.0F, tTo.get(0).getAsFloat(), "juicer: the pestle column x-max (4px wide)");
                assertEquals(10.0F, tTo.get(2).getAsFloat(), "juicer: the pestle column z-max (4px deep)");
                tPestle = true;
            } else if (tFrom.get(1).getAsFloat() == 0.0F && tTo.get(1).getAsFloat() == 1.0F) {
                tBase = true;
            }
        }
        assertEquals(4, tWalls, "juicer: four wall panels, 4px tall");
        assertTrue(tBase, "juicer: the 1px base slab");
        assertTrue(tPestle, "juicer: the central pestle column");
        assertAllFacesTinted(tModel, "juicer");
    }

    /** The four BlockItem models parent their own block model (the addAnvils item form). */
    @Test
    void generatedItemModelsParentTheirBlockModel() throws Exception {
        for (String tBlockId : BLOCK_FAMILIES.keySet()) {
            JsonObject tItem = generatedJson("assets/gt6/models/item/" + tBlockId + ".json");
            assertEquals("gt6:block/" + tBlockId, tItem.get("parent").getAsString(),
                    tBlockId + ": the item parents its block model");
        }
    }
}
