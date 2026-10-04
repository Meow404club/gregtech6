/**
 * Offline pin for task rail-item-model-fix (GitHub #22) — the 31 rail BlockItem
 * models carry the vanilla rail item form: {@code item/generated} over the flat arm's
 * own texture (the vanilla item/rail.json shape). The former parent-the-flat-model form
 * inherited the 16x1x16 {@code rail_flat} slab, which renders near-invisible under the
 * inventory's 3D item camera (the user-visible blank slot). Reads the committed
 * generated tree (the {@link GT6FallenLogBlockstateTest} classpath form — no datagen
 * run); the block models are pinned UNCHANGED in the same breath, so the item fix can
 * never leak into the world face.
 *
 * <p>Task r11c-rail-cutout (GitHub #22 follow-up, the white-background symptom): every
 * {@code rail_*} block model declares {@code render_type: minecraft:cutout} —
 * {@code render_type} does NOT ride the parent chain
 * ({@link GT6ConverterPaintRenderDatagenTest} self-evidence), and without it the
 * vanilla {@code ItemBlockRenderTypes.TYPE_BY_BLOCK} lookup (keyed by Block INSTANCE)
 * misses the GT6 rail subclasses and falls back to the discard-less solid layer, where
 * the transparent texels paint as white. The 61 rail PNGs are pixel-pinned to carry
 * alpha&lt;255 texels, so the cutout declaration is the actual cure.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Rails;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6RailItemModelDatagenTest extends GTOfflineTestBase {

    /** The mdk project root, walking up from the (leg-dependent) test working dir (mdk/tools anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6RailItemModelDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The flat arm's own texture per row — the layer0 source (GT6BlockStates.addRails). */
    private static String expectedLayer0(GT6Rails.RailRow aRow) {
        String tSlug = GT6Rails.SLUGS[GT6Rails.ROWS.indexOf(aRow) % GT6Rails.SLUGS.length];
        return switch (aRow.kind()) {
            case NORMAL -> "gt6:block/rail_straight_" + tSlug;
            case BOOSTER -> "gt6:block/rail_booster_" + tSlug;
            case DETECTOR -> "gt6:block/rail_detector_" + tSlug;
        };
    }

    /** All 30 material rows: item model = item/generated + the flat arm texture as layer0. */
    @Test
    void railItemModelsAreTheVanillaGeneratedForm() throws Exception {
        assertFalse(GT6Rails.ROWS.isEmpty(), "the row walk broke — never pass vacuously");
        for (GT6Rails.RailRow tRow : GT6Rails.ROWS) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                    tRow.path() + ": the vanilla item/generated parent");
            JsonObject tTextures = tModel.getAsJsonObject("textures");
            assertEquals(1, tTextures.size(), tRow.path() + ": exactly the layer0 texture");
            assertEquals(expectedLayer0(tRow), tTextures.get("layer0").getAsString(),
                    tRow.path() + ": the flat arm's own texture");
        }
    }

    /** The road stripe joins the same form over the RAIL_ROAD_STRIPE texture. */
    @Test
    void roadItemModelIsTheVanillaGeneratedForm() throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/item/" + GT6Rails.ROAD_PATH + ".json");
        assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                "rail_road: the vanilla item/generated parent");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals(1, tTextures.size(), "rail_road: exactly the layer0 texture");
        assertEquals("gt6:block/rail_road_stripe", tTextures.get("layer0").getAsString(),
                "rail_road: the RAIL_ROAD_STRIPE texture");
    }

    /**
     * The world-face guard: the block flat models keep the vanilla rail_flat parent and
     * the rail texture key — one archetype per family form (the item fix must not leak
     * into the blockstate/model chain).
     */
    @Test
    void blockFlatModelsKeepTheVanillaParents() throws Exception {
        String[][] tArchetypes = {
                {"rail_aluminium_flat", "gt6:block/rail_straight_aluminium"},
                {"rail_booster_steel_off_flat", "gt6:block/rail_booster_steel"},
                {"rail_detector_tungstencarbide_off_flat", "gt6:block/rail_detector_tungstencarbide"},
                {"rail_road_off_flat", "gt6:block/rail_road_stripe"}};
        for (String[] tCase : tArchetypes) {
            JsonObject tModel = generatedJson("assets/gt6/models/block/" + tCase[0] + ".json");
            assertEquals("minecraft:block/rail_flat", tModel.get("parent").getAsString(),
                    tCase[0] + ": the vanilla flat parent (the world face is untouched)");
            assertTrue(tModel.has("textures") && tModel.getAsJsonObject("textures").has("rail"),
                    tCase[0] + ": the rail texture key");
            assertEquals(tCase[1], tModel.getAsJsonObject("textures").get("rail").getAsString(),
                    tCase[0] + ": the flat arm texture");
        }
    }

    /** The committed generated-tree census floor: every rail block model the factory emits. */
    private static final int RAIL_BLOCK_MODEL_CENSUS = 292;

    /**
     * Task r11c-rail-cutout: EVERY {@code rail_*} block model declares
     * {@code render_type: minecraft:cutout} — it does not ride the parent chain, and the
     * absence painted the transparent texels as white on the discard-less solid layer.
     */
    @Test
    void railBlockModelsAllDeclareTheCutoutLayer() throws Exception {
        Path tModelsDir = mdkRoot().resolve("src/generated/resources/assets/gt6/models/block");
        List<Path> tRailModels;
        try (Stream<Path> tWalk = Files.list(tModelsDir)) {
            tRailModels = tWalk.map(Path::getFileName).map(Path::toString)
                    .filter(tName -> tName.startsWith("rail_") && tName.endsWith(".json")).sorted()
                    .map(tModelsDir::resolve).toList();
        }
        assertTrue(tRailModels.size() >= RAIL_BLOCK_MODEL_CENSUS,
                "the rail model census is non-vacuous: " + tRailModels.size());
        for (Path tFile : tRailModels) {
            JsonObject tModel = JsonParser.parseString(Files.readString(tFile, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals("minecraft:cutout",
                    tModel.has("render_type") ? tModel.get("render_type").getAsString() : null,
                    tFile.getFileName() + ": the alpha-tested cutout layer (transparent texels must discard)");
        }
    }

    /**
     * The pixel-level premise pin: all 61 {@code rail_*.png} carry alpha&lt;255 texels, so
     * the cutout declaration is the actual cure (on the discard-less solid layer those
     * texels paint their RGB residue as the white background).
     */
    @Test
    void railTexturesCarryAlphaTexels() throws Exception {
        Path tTexturesDir = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block");
        List<Path> tRailTextures;
        try (Stream<Path> tWalk = Files.list(tTexturesDir)) {
            tRailTextures = tWalk.map(Path::getFileName).map(Path::toString)
                    .filter(tName -> tName.startsWith("rail_") && tName.endsWith(".png")).sorted()
                    .map(tTexturesDir::resolve).toList();
        }
        assertTrue(tRailTextures.size() >= 61, "the rail texture census is non-vacuous: " + tRailTextures.size());
        for (Path tFile : tRailTextures) {
            BufferedImage tImage = ImageIO.read(tFile.toFile());
            assertNotNull(tImage, "decodable PNG: " + tFile);
            int tAlphaTexels = 0;
            for (int y = 0; y < tImage.getHeight(); y++) {
                for (int x = 0; x < tImage.getWidth(); x++) {
                    if ((tImage.getRGB(x, y) >>> 24) < 255) tAlphaTexels++;
                }
            }
            assertTrue(tAlphaTexels > 0,
                    tFile.getFileName() + ": carries alpha<255 texels (the cutout layer must discard them)");
        }
    }
}
