/**
 * The C5 clean-up wiring census (task r3-world-tint-render-type): the two #8 stragglers
 * the R3 research caught (research.issues-r3-wall q3) join the baked-tint domain —
 * <ul>
 * <li><b>the boiler tank family</b> — every upstream row carries NBT_MATERIAL
 *     (Loader_MultiTileEntities.java:553-579, the {@code aMat} column: Pb/Bi/Bronze/
 *     ArsenenicCopper/ArsenicBronze/Invar/ANY.Steel/Cr/Ti/Netherite/ANY.W/
 *     TungstenSteel/Ultimet); the port rows now mirror it as lazy suppliers and the
 *     shared {@code steam_boiler_tank} model seats the tint (tintindex 0 body) while
 *     keeping the barometer front face.</li>
 * <li><b>machine_wall_tungsten</b> — the :1151 metal-wall row the Lightning Rod family
 *     registers (the GTMultiBlocks:713 skip): the row material (ANY.W) now rides the
 *     row-less material-carrier constructor and the model is the metalwall design-0
 *     two-layer partModel (body tintindex 0 + the six 0.01 decals).</li>
 * </ul>
 *
 * <p>OFFLINE posture (the GTMachinesMaterialRowTest shape): the row tables and the
 * colour seam are the assertion surface; the live block/registration half rides the
 * RCON probe (the r3wall chain) and the render-type census
 * (GT6PaintableRenderTypeCensusTest) covers the generated-tree face.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.registry.GT6Boilers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMultiBlocks;

class GT6BoilerWallTintDatagenTest {

    /**
     * The upstream Loader :553-579 aMat column, keyed by the row slug. Built per call —
     * the MT/ANY fields resolve only after {@code GTMaterialItems.initMaterials()} (the
     * class-init NPE trap the GTMachinesMaterialRowTest ladder avoids by reading inside
     * its test bodies).
     */
    private static Map<String, OreDictMaterial> upstreamMaterials() {
        return Map.ofEntries(
                Map.entry("lead", gregapi.data.MT.Pb),
                Map.entry("bismuth", gregapi.data.MT.Bi),
                Map.entry("bronze", gregapi.data.MT.Bronze),
                Map.entry("arsenic_copper", gregapi.data.MT.ArsenicCopper),
                Map.entry("arsenic_bronze", gregapi.data.MT.ArsenicBronze),
                Map.entry("invar", gregapi.data.MT.Invar),
                Map.entry("steel", gregapi.data.ANY.Steel), // upstream ANY.Steel, NOT MT.Steel
                Map.entry("chromium", gregapi.data.MT.Cr),
                Map.entry("titanium", gregapi.data.MT.Ti),
                Map.entry("netherite", gregapi.data.MT.Netherite),
                Map.entry("tungsten", gregapi.data.ANY.W), // upstream ANY.W, NOT MT.W
                Map.entry("tungstensteel", gregapi.data.MT.TungstenSteel),
                Map.entry("ultimet", gregapi.data.MT.Ultimet));
    }

    @BeforeAll
    public static void bootOfflineThenMaterials() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline (the GTMachinesMaterialRowTest shape)
        }
        GTMaterialItems.initMaterials();
    }

    /** Both boiler ladders (26 rows) mirror the upstream aMat column exactly. */
    @Test
    public void boilerRowsCarryTheUpstreamMaterials() {
        Map<String, OreDictMaterial> tExpected = upstreamMaterials();
        assertEquals(26, GT6Boilers.allRows().size(), "13 standard + 13 Strong");
        for (GT6Boilers.BoilerRow tRow : GT6Boilers.allRows()) {
            assertSame(tExpected.get(tRow.material().slug()), tRow.material().mat().get(),
                    "the " + tRow.path() + " material must mirror Loader :553-579");
        }
    }

    /**
     * The RCON assertion base: Invar boils down to the warm yellow-white
     * (220,220,150) = 0xDCDC96 — the exact same tint the D2 probe read off dryer_t2
     * (MT.java:2498), and tungsten stays the dark grey (50,50,50) = 0x323232 that
     * makes the wall hard to eyeball (the talk-track note, faithful upstream).
     */
    @Test
    public void theRconColourBasesHold() {
        assertEquals(0xDCDC96, GTBasicMachineBlock.materialColor(GT6Boilers.MAT_INVAR.mat().get()));
        assertEquals(0x323232, GTBasicMachineBlock.materialColor(GT6Boilers.MAT_TUNGSTEN.mat().get()));
    }

    /**
     * The Lightning Rod registration's tungsten row carries the port's wall material
     * (MT.W — the METAL_WALL_ROWS/LIGHTNING_ROD_PART_ROWS/burning-box convention;
     * upstream's ANY.W union steals its looks from MT.W anyway, ANY.java:133, so the
     * rendered tint is the same (50,50,50)).
     */
    @Test
    public void tungstenWallRowCarriesTheWallMaterial() {
        boolean tFound = false;
        for (var tRow : GTMultiBlocks.LIGHTNING_ROD_PART_ROWS) {
            if (tRow.path().equals("machine_wall_tungsten")) {
                assertSame(gregapi.data.MT.W, tRow.material().get(), "the port wall-row material");
                tFound = true;
            }
        }
        assertEquals(true, tFound, "the Lightning Rod rows must name machine_wall_tungsten");
    }

    /** The boiler model seats the tint on every body face and keeps the barometer front. */
    @Test
    public void boilerModelSeatsTheTintAndKeepsTheBarometerFace() throws Exception {
        JsonObject tJson = model("steam_boiler_tank");
        JsonArray tElements = tJson.getAsJsonArray("elements");
        assertEquals(1, tElements.size(), "the single tinted body cube (no shell — the gauge is in the front art)");
        JsonObject tBody = tElements.get(0).getAsJsonObject();
        for (Map.Entry<String, JsonElement> tFace : tBody.getAsJsonObject("faces").entrySet()) {
            assertEquals(0, tFace.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                    "the " + tFace.getKey() + " face is a tint seat");
        }
        JsonObject tTextures = tJson.getAsJsonObject("textures");
        assertEquals("gt6:block/boiler_steam/front", tTextures.get("north").getAsString(),
                "the barometer face stays the north/front texture");
        assertEquals("gt6:block/boiler_steam/side", tTextures.get("south").getAsString());
    }

    /** The tungsten wall model is the metalwall two-layer form (tinted body + six decals). */
    @Test
    public void tungstenWallModelJoinsTheMetalwallFamily() throws Exception {
        JsonObject tJson = model("machine_wall_tungsten");
        JsonObject tTextures = tJson.getAsJsonObject("textures");
        assertEquals("gt6:block/parts/metalwall/0/colored/side", tTextures.get("north").getAsString(),
                "the wall art is the design-0 metalwall set (the former borrow's identical bytes)");
        JsonArray tElements = tJson.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), "the body cube + the six 0.01 wall decals (partModel shape)");
        JsonObject tBody = tElements.get(0).getAsJsonObject();
        for (Map.Entry<String, JsonElement> tFace : tBody.getAsJsonObject("faces").entrySet()) {
            assertEquals(0, tFace.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                    "the " + tFace.getKey() + " body face is a tint seat");
        }
    }

    private static JsonObject model(String aName) throws Exception {
        try (InputStream tStream = GT6BoilerWallTintDatagenTest.class.getClassLoader()
                .getResourceAsStream("assets/gt6/models/block/" + aName + ".json")) {
            assertNotNull(tStream, "the generated model must be on the classpath: " + aName);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
