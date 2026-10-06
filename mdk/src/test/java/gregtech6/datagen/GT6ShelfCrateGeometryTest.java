/*
 * Offline pins for task shelf-crate-geometry — the shelf/crate geometry contract:
 * <ul>
 * <li>the book-display mapping (the new GT6BookShelfRenderer statics): the 28 niches
 *     verbatim from the upstream render-pass boxes (MultiTileEntityBookShelf
 *     .setBlockBounds2 :314-322, the north-default form) — front top row = slots 0..6
 *     at y 9..15, front bottom = 7..13 at y 1..7 (the seat-25 correction —
 *     {@code PX_N[9]} reads 7px, both rows are 6px bands), the back face mirrors the columns
 *     (the :105/:211 picker faces ride the same table, getFacingCoordsClicked
 *     UT.java:1738 = the screen-style u/v pair);</li>
 * <li>the three-way alignment (task shelf-crate-2px-realign, the user ruling 2026-10-06
 *     "我想要让渲染、碰撞箱、描边对齐，看起来不奇怪"): the rendered model envelope, the
 *     collision box and the selection outline coincide per family — the crate on the 8px
 *     body box, the shelf on the full cube. The upstream-faithful 6px-selection/10px-
 *     collision split (:212-214) read as a 6/8/10 staircase against the 8px walls and the
 *     2px-inset shelf outline (:349-350) outlined a hole 2px inside the visible mass —
 *     the reported "outline vs model ~2px" mismatch; both stay deliberate deviations
 *     from upstream, looks first;</li>
 * <li>the blockstate/item wiring: all four facings ride the yaw table and every item
 *     model parents its plank leaf (the three-faces-same-change rule).</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.phys.AABB;

class GT6ShelfCrateGeometryTest {

    @BeforeAll
    static void bootOffline() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = GT6ShelfCrateGeometryTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The element footprint list as px strings (from/to pairs, read order). */
    private static java.util.List<String> boxes(String aModel) throws IOException {
        return json("assets/gt6/models/block/" + aModel + ".json").getAsJsonArray("elements").asList().stream()
                .map(GT6ShelfCrateGeometryTest::boxLabel).toList();
    }

    private static String boxLabel(JsonElement aElement) {
        JsonObject tElement = aElement.getAsJsonObject();
        return tElement.getAsJsonArray("from") + " -> " + tElement.getAsJsonArray("to");
    }

    /** The px envelope of every element in the model (min/max over all from/to triples). */
    private static AABB envelope(String aModel) throws IOException {
        double tMinX = 16, tMinY = 16, tMinZ = 16, tMaxX = 0, tMaxY = 0, tMaxZ = 0;
        for (JsonElement tElement : json("assets/gt6/models/block/" + aModel + ".json")
                .getAsJsonArray("elements").asList()) {
            var tFrom = tElement.getAsJsonObject().getAsJsonArray("from");
            var tTo = tElement.getAsJsonObject().getAsJsonArray("to");
            tMinX = Math.min(tMinX, tFrom.get(0).getAsDouble());
            tMinY = Math.min(tMinY, tFrom.get(1).getAsDouble());
            tMinZ = Math.min(tMinZ, tFrom.get(2).getAsDouble());
            tMaxX = Math.max(tMaxX, tTo.get(0).getAsDouble());
            tMaxY = Math.max(tMaxY, tTo.get(1).getAsDouble());
            tMaxZ = Math.max(tMaxZ, tTo.get(2).getAsDouble());
        }
        return new AABB(tMinX / 16.0, tMinY / 16.0, tMinZ / 16.0, tMaxX / 16.0, tMaxY / 16.0, tMaxZ / 16.0);
    }

    // ------------------------------------------------------------------
    // the book-display mapping (upstream :314-322, north default)
    // ------------------------------------------------------------------

    @Test
    public void bookSlotsMapToTheTwentyEightNiches() {
        // faces: front 0..13, back 14..27 (the SLOTS_PER_FACE split)
        assertFalse(gregtech6.client.render.GT6BookShelfRenderer.isBackFace(0), "slot 0 rides the front face");
        assertFalse(gregtech6.client.render.GT6BookShelfRenderer.isBackFace(13), "slot 13 rides the front face");
        assertTrue(gregtech6.client.render.GT6BookShelfRenderer.isBackFace(14), "slot 14 rides the back face");
        assertTrue(gregtech6.client.render.GT6BookShelfRenderer.isBackFace(27), "slot 27 rides the back face");
        // rows: top 0..6 / 14..20 at y 12px (the 9..15 band), bottom 7..13 / 21..27 at y 4px (1..7)
        assertTrue(gregtech6.client.render.GT6BookShelfRenderer.isTopRow(0), "slot 0 rides the top row");
        assertTrue(gregtech6.client.render.GT6BookShelfRenderer.isTopRow(6), "slot 6 rides the top row");
        assertFalse(gregtech6.client.render.GT6BookShelfRenderer.isTopRow(7), "slot 7 rides the bottom row");
        assertTrue(gregtech6.client.render.GT6BookShelfRenderer.isTopRow(20), "slot 20 rides the top row");
        assertFalse(gregtech6.client.render.GT6BookShelfRenderer.isTopRow(21), "slot 21 rides the bottom row");
        assertEquals(12.0F, gregtech6.client.render.GT6BookShelfRenderer.rowCenterPx(0), 1e-6F,
                "the top row centres at y 12px (the :314 band 9..15)");
        assertEquals(4.0F, gregtech6.client.render.GT6BookShelfRenderer.rowCenterPx(7), 1e-6F,
                "the bottom row centres at y 4px (the :315 band 1..7, PX_N[9]=7px)");
        assertEquals(12.0F, gregtech6.client.render.GT6BookShelfRenderer.rowCenterPx(14), 1e-6F,
                "the back top row shares the height");
        // columns: front x band [1+2c, 3+2c] (:314), back mirrored [13-2c, 15-2c] (:316)
        assertEquals(2.0F, gregtech6.client.render.GT6BookShelfRenderer.columnCenterPx(0), 1e-6F,
                "front column 0 centres at x 2px");
        assertEquals(14.0F, gregtech6.client.render.GT6BookShelfRenderer.columnCenterPx(6), 1e-6F,
                "front column 6 centres at x 14px");
        assertEquals(14.0F, gregtech6.client.render.GT6BookShelfRenderer.columnCenterPx(14), 1e-6F,
                "back column 0 mirrors to x 14px");
        assertEquals(2.0F, gregtech6.client.render.GT6BookShelfRenderer.columnCenterPx(20), 1e-6F,
                "back column 6 mirrors to x 2px");
        assertEquals(2.0F, gregtech6.client.render.GT6BookShelfRenderer.columnCenterPx(27), 1e-6F,
                "back bottom col 6 mirrors to x 2px (the :322 band 1..3)");
        // depths: front band z 2..7 (:314), back band z 9..14 (:316)
        assertEquals(4.5F, gregtech6.client.render.GT6BookShelfRenderer.depthCenterPx(0), 1e-6F,
                "front books sit mid-niche at z 4.5px");
        assertEquals(11.5F, gregtech6.client.render.GT6BookShelfRenderer.depthCenterPx(14), 1e-6F,
                "back books sit mid-niche at z 11.5px");
        // the spine scale: 2px wide over the 2px column, the row-height fills
        assertEquals(0.125F, gregtech6.client.render.GT6BookShelfRenderer.SPINE_WIDTH, 1e-9F,
                "the spine is 2px wide (the upstream book box width, :314)");
        assertEquals(0.375F, gregtech6.client.render.GT6BookShelfRenderer.topRowHeight(), 1e-9F,
                "the top-row book fills its 6px band (:314)");
        assertEquals(0.375F, gregtech6.client.render.GT6BookShelfRenderer.bottomRowHeight(), 1e-9F,
                "the bottom-row book fills its 6px band (the :315 band 1..7)");
    }

    // ------------------------------------------------------------------
    // the elements-to-shape consistency (the card headline)
    // ------------------------------------------------------------------

    @Test
    public void crateModelCollisionAndOutlineAreOneEnvelope() throws IOException {
        // the three-way ruling (shelf-crate-2px-realign): the rendered model, the
        // collision and the selection outline coincide on the (0,0,0)-(16,8,16) body
        // box — the upstream-faithful 6px selection / 10px collision (:212-214) read
        // as a 6/8/10 staircase against the 8px walls (the reported ~2px mismatch)
        AABB tEnvelope = envelope("gt6_bottlecrate_frame");
        assertEquals(8.0 / 16.0, tEnvelope.maxY, 1e-9,
                "the crate frame tops at 8px (the wall boxes :165-166)");
        AABB tCollision = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.CRATE_COLLISION_SHAPE.bounds();
        AABB tSelection = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.CRATE_SELECTION_SHAPE.bounds();
        assertEquals(tEnvelope.minX, tSelection.minX, 1e-9, "outline west = model west");
        assertEquals(tEnvelope.maxX, tSelection.maxX, 1e-9, "outline east = model east");
        assertEquals(tEnvelope.minY, tSelection.minY, 1e-9, "outline bottom = model bottom");
        assertEquals(tEnvelope.maxY, tSelection.maxY, 1e-9, "outline top = the 8px wall tops");
        assertEquals(tEnvelope.minZ, tSelection.minZ, 1e-9, "outline north = model north");
        assertEquals(tEnvelope.maxZ, tSelection.maxZ, 1e-9, "outline south = model south");
        assertEquals(tSelection.minX, tCollision.minX, 1e-9, "collision west = outline west");
        assertEquals(tSelection.maxX, tCollision.maxX, 1e-9, "collision east = outline east");
        assertEquals(tSelection.minY, tCollision.minY, 1e-9, "collision bottom = outline bottom");
        assertEquals(tSelection.maxY, tCollision.maxY, 1e-9, "collision top = outline top");
        assertEquals(tSelection.minZ, tCollision.minZ, 1e-9, "collision north = outline north");
        assertEquals(tSelection.maxZ, tCollision.maxZ, 1e-9, "collision south = outline south");
    }

    @Test
    public void shelfModelCollisionAndOutlineAreOneFullCube() throws IOException {
        AABB tEnvelope = envelope("gt6_bookshelf_frame");
        assertEquals(0.0, tEnvelope.minX, 1e-9, "the shelf frame spans the full cube (the :296-297 walls)");
        assertEquals(1.0, tEnvelope.maxX, 1e-9, "the shelf frame spans the full cube");
        assertEquals(0.0, tEnvelope.minY, 1e-9, "the bottom slab grounds the cube (:294)");
        assertEquals(1.0, tEnvelope.maxY, 1e-9, "the top slab roofs the cube (:295)");
        assertEquals(0.0, tEnvelope.minZ, 1e-9, "the walls reach both open faces");
        assertEquals(1.0, tEnvelope.maxZ, 1e-9, "the walls reach both open faces");
        // the three-way ruling: the outline rides the full cube (the named SHELF_SHAPE —
        // a Block instance cannot be constructed offline, the frozen block registry) and
        // the collision keeps the untouched vanilla cube, numerically the same box
        AABB tOutline = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.SHELF_SHAPE.bounds();
        AABB tVanillaCube = net.minecraft.world.phys.shapes.Shapes.block().bounds();
        assertEquals(0.0, tOutline.minX, 1e-9, "the outline hugs the west wall");
        assertEquals(1.0, tOutline.maxX, 1e-9, "the outline hugs the east wall");
        assertEquals(0.0, tOutline.minY, 1e-9, "the outline hugs the bottom");
        assertEquals(1.0, tOutline.maxY, 1e-9, "the outline hugs the top");
        assertEquals(0.0, tOutline.minZ, 1e-9, "the outline hugs the north wall");
        assertEquals(1.0, tOutline.maxZ, 1e-9, "the outline hugs the south wall");
        assertEquals(tVanillaCube.minX, tOutline.minX, 1e-9, "collision (vanilla cube) = outline west");
        assertEquals(tVanillaCube.maxX, tOutline.maxX, 1e-9, "collision (vanilla cube) = outline east");
        assertEquals(tVanillaCube.maxY, tOutline.maxY, 1e-9, "collision (vanilla cube) = outline top");
        // the books (the BER display) live in the open niches: the frame must NOT close
        // the front/back — the mid-band z 7..9 spine is the only z-wall between them
        assertEquals(6, boxes("gt6_bookshelf_frame").size(), "the frame stays the six-box shelf");
    }

    // ------------------------------------------------------------------
    // the blockstate/item wiring (the three-faces-same-change rule)
    // ------------------------------------------------------------------

    @Test
    public void blockstatesWireTheFacingYawTable() throws IOException {
        for (String tPath : java.util.List.of("bookshelf_oak", "bottlecrate_oak")) {
            var tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            assertEquals(4, tVariants.size(), tPath + ": all four facings resolve");
            assertTrue(tVariants.getAsJsonObject("facing=north").has("y") == false,
                    tPath + ": north is the authored default (y absent)");
            assertEquals(90, tVariants.getAsJsonObject("facing=east").get("y").getAsInt(), tPath + ": east y=90");
            assertEquals(180, tVariants.getAsJsonObject("facing=south").get("y").getAsInt(), tPath + ": south y=180");
            assertEquals(270, tVariants.getAsJsonObject("facing=west").get("y").getAsInt(), tPath + ": west y=270");
            assertEquals("gt6:block/gt6_" + tPath, tVariants.getAsJsonObject("facing=north").get("model").getAsString(),
                    tPath + ": every facing rides the plank leaf");
        }
    }

    @Test
    public void itemModelsParentTheirPlankLeaves() throws IOException {
        for (String tPath : java.util.List.of("bookshelf_oak", "bottlecrate_oak")) {
            assertEquals("gt6:block/gt6_" + tPath,
                    json("assets/gt6/models/item/" + tPath + ".json").get("parent").getAsString(),
                    tPath + ": the item form is the plank leaf (the held art matches the world)");
        }
    }
}
