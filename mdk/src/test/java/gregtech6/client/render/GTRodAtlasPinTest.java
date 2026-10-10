/*
 * The block-atlas manifest pin for the rod family's bake-only arts (task
 * pipe-arm-atlas-facemap): every sprite id a baked rod/wire model resolves at bake time
 * with NO model JSON must be an explicit per-id single in the generated
 * {@code assets/minecraft/atlases/blocks.json} (GT6Atlases) — the consumer-side
 * stitching contract. Runtime the vanilla cross-namespace directory(block) source and
 * the closeout card's materialicons DirectoryLister already stitch all of these, so the
 * manifest is belt-and-suspenders — but "somewhere stitched" is not a contract: the pin
 * walks the REAL rows (GTFluidPipes/GTItemPipes → pipeArmSprite) and asserts the ids
 * verbatim, so a dropped directory source or an id drift between the manifest and the
 * texture table fails here, not as in-world missingno chessboard arms (the reported
 * symptom class).
 *
 * <p>Offline posture (the GT6PipeTextureDatagenTest shape): the
 * {@code @BeforeAll} boot precedes every registry-class touch, and the
 * GTFluidPipes/GTItemPipes walks live INSIDE the test methods (the lazy-accessor rule —
 * a static-final class-init before the boot would poison the shared test JVM).
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

import gregtech6.client.wire.GTWireTextures;

public class GTRodAtlasPinTest {

    @BeforeAll
    static void bootMaterials() {
        // the vanilla bootstrap + material system (the pipe texture datagen test shape)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        gregtech6.registry.GTMaterialItems.initMaterials();
    }

    private static Path mdkRoot() {
        for (Path tPath = Path.of("").toAbsolutePath(); tPath != null; tPath = tPath.getParent()) {
            if (Files.isRegularFile(tPath.resolve("tools").resolve("gen_textures.py"))) {
                return tPath;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The explicit per-id single resources of the generated block-atlas manifest. */
    private static Set<String> atlasSingles() throws IOException {
        Path tAtlas = mdkRoot().resolve("src/generated/resources/assets/minecraft/atlases/blocks.json");
        assertTrue(Files.isRegularFile(tAtlas), "the generated blocks atlas manifest is missing: " + tAtlas);
        JsonObject tJson = JsonParser.parseString(Files.readString(tAtlas, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> rSingles = new HashSet<>();
        for (JsonElement tSource : tJson.getAsJsonArray("sources")) {
            JsonObject tEntry = tSource.getAsJsonObject();
            if (tEntry.get("type").getAsString().equals("minecraft:single")) {
                rSingles.add(tEntry.get("resource").getAsString());
            }
        }
        return rSingles;
    }

    private static void assertPngOnStaticTree(ResourceLocation aId) {
        Path tPng = mdkRoot().resolve("src/main/resources/assets/" + aId.getNamespace()
                + "/textures/" + aId.getPath() + ".png");
        assertTrue(Files.isRegularFile(tPng), "atlas single points at a missing PNG: " + tPng);
    }

    /**
     * The per-diameter arm arts: the ids the REAL rows resolve (the full pipeArmSprite
     * codomain — wood/rubber/copper x tiny..huge, the upstream :265 selector) are each an
     * explicit single in the manifest, and each points at a borrowed PNG on the static
     * tree.
     */
    @Test
    public void everyPipeArmSpriteIsAnExplicitAtlasSingle() throws Exception {
        Set<String> tSingles = atlasSingles();
        List<ResourceLocation> tCodomain = GTWireTextures.allPipeArmSprites();
        assertEquals(15, tCodomain.size(), "the selector codomain: 3 art sets x 5 diameters");
        Set<ResourceLocation> tResolved = new HashSet<>();
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            tResolved.add(GTWireTextures.pipeArmSprite(tRow.material().oreDictMaterial(), tRow.variant().diameterPx));
        }
        for (gregtech6.registry.GTItemPipes.ItemPipeRow tRow : gregtech6.registry.GTItemPipes.ROWS) {
            tResolved.add(GTWireTextures.pipeArmSprite(tRow.material().oreDictMaterial(), tRow.variant().diameterPx));
        }
        assertTrue(tResolved.size() >= 10, "the live rows walk the arm selector broadly: " + tResolved);
        assertTrue(tSingles.containsAll(tResolved.stream().map(ResourceLocation::toString).toList()),
                "every row-resolved arm art must be a manifest single");
        for (ResourceLocation tId : tCodomain) {
            assertTrue(tSingles.contains(tId.toString()), "arm art missing from the atlas manifest: " + tId);
            assertPngOnStaticTree(tId);
        }
    }

    /**
     * The remaining rod bake-only audit set (task pipe-arm-atlas-facemap acceptance): the
     * untinted pipeSide outline, the restrictive decal plate, the logistics wire pair and
     * the axle barrel art — each a manifest single over a real PNG.
     */
    @Test
    public void rodBakeOnlySpritesAreExplicitAtlasSingles() throws Exception {
        Set<String> tSingles = atlasSingles();
        for (ResourceLocation tId : List.of(GTWireTextures.PIPE_SIDE_OVERLAY_SPRITE,
                GTWireTextures.PIPE_RESTRICTOR_SPRITE, GTWireTextures.LOGISTICS_WIRE_SPRITE,
                GTWireTextures.LOGISTICS_WIRE_OVERLAY_SPRITE, GTWireTextures.AXLE_SPRITE)) {
            assertTrue(tSingles.contains(tId.toString()), "bake-only art missing from the atlas manifest: " + tId);
            assertPngOnStaticTree(tId);
        }
    }
}
