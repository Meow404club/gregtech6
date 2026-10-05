package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@code minecraft:water} extension pins (review-seat seam fix, task
 * worldgen-deepocean-corals): the three water-replace bodies (task worldgen-water-replace,
 * GTFluids WATER_REPLACE_BLOCK_IDS) + their flowing twins ride the vanilla fluid tag. The
 * consumer is the deep-ocean pylon water gate (GT6DeepOceanFeature — {@code FluidTags.WATER},
 * the WorldgenDeepOcean.java:49 WD.anywater port face): without the tag the swapped oceans
 * go invisible to the gate and every other water-tag consumer (seagrass/kelp placement, the
 * water-mob spawn placements).
 *
 * <p><b>The classpath-shadowing face</b>: the plural {@code tags/fluids} path resolves to
 * the mod's generated file on both legs (the 1.21 vanilla jar carries no plural face), but
 * the singular {@code tags/fluid} path is ALSO carried by the 1.21 vanilla jar itself, and
 * the test-JVM classpath order lets the vanilla copy shadow the mod's for the
 * minecraft-namespace singular (the pre-existing {@link GT6DualDirectoryFacesTest} singular
 * pins are all gt6-namespace or null-assertions, so this shadow was never exercised). The
 * runtime truth is the DATAPACK MERGE (the mod jar's tag joins vanilla's at world load,
 * order-independent union), so the singular pin reads the committed generated tree off the
 * SOURCE TREE (the locateMdkRoot face) instead of the classloader.
 */
public class GT6FluidTagsTest {

    /** The six members: the three water bodies + their flowing twins (the GTFluids family naming). */
    private static final List<String> MEMBERS = List.of(
            "gt6:seawater", "gt6:seawater_flowing",
            "gt6:riverwater", "gt6:riverwater_flowing",
            "gt6:waterdirty", "gt6:waterdirty_flowing");

    /** The canonical plural face: exactly the six members, no replace flip. */
    @Test
    void waterTagCarriesExactlyTheThreeWaterBodies() throws Exception {
        JsonObject tTag = resource("data/minecraft/tags/fluids/water.json");
        assertFalse(tTag.has("replace") && tTag.get("replace").getAsBoolean(),
                "the extension face must not replace the vanilla tag");
        List<String> tValues = values(tTag);
        for (String tMember : MEMBERS) {
            assertTrue(tValues.contains(tMember), tMember + " must ride minecraft:water");
        }
        assertEquals(MEMBERS.size(), tValues.size(),
                "the extension face carries exactly the six members: " + tValues);
    }

    /** The singular 1.21 alias mirrors the plural face (the GT6DualDirectoryFaces identity), read off the source tree. */
    @Test
    void singularAliasMirrorsThePluralFace() throws Exception {
        Path tMdk = locateMdkRoot();
        assertNotNull(tMdk, "the mdk root anchors the source-tree read");
        String tPlural = Files.readString(
                tMdk.resolve("src/generated/resources/data/minecraft/tags/fluids/water.json"));
        String tSingular = Files.readString(
                tMdk.resolve("src/generated/resources/data/minecraft/tags/fluid/water.json"));
        assertEquals(tPlural, tSingular, "the alias is the same provider-produced JSON");
    }

    /** Climb from the working directory to the mdk root (the GTEntityBlockRenderShapeCensusTest face). */
    private static Path locateMdkRoot() {
        Path tDir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
            if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/datagen/GT6DataGenerators.java"))) {
                return tDir;
            }
        }
        return null;
    }

    private static JsonObject resource(String aPath) throws Exception {
        try (InputStream tStream = GT6FluidTagsTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, "committed datagen JSON missing: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> values(JsonObject aTag) {
        return aTag.getAsJsonArray("values").asList().stream().map(tEntry ->
                tEntry.isJsonPrimitive() ? tEntry.getAsString() : tEntry.getAsJsonObject().get("id").getAsString()).toList();
    }
}
