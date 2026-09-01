/**
 * The wire texture census (task p9-wire-family-w2 spec ④ — "census 先行"): the borrowed
 * {@code materialicons/<set>/wire.png} coverage is counted per ICONSET (not per material —
 * 30 rows collapse into few sets). The set of a row comes from
 * {@link gregtech6.client.wire.GTWireTextures#blockSetOf} (the MC-free single source the
 * datagen and the client listener both consume); an empty list = "none" = upstream
 * SET_NONE (TextureSet.java:188). Census 2026-09-01: exactly
 * copper/shiny/metallic/dull/quartz/rad/none over the 30 rows, and the upstream ships
 * wire.png in EVERY set — zero coverage gaps, no placeholder fallback ever fires today
 * (the runtime fallback path stays as belt-and-suspenders).
 *
 * <p>Also pins the borrow itself: all 13 files resolve on the classpath and the 7 wire.png
 * copies are byte-identical (upstream ships the same wire icon in every set — sha256
 * d9343ea9…, assets/README.md).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.wire.GTWireTextures;

public class GTWireTextureCensusTest {

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @Test
    public void theThirtyRowsCollapseIntoSevenIconsets() {
        Set<String> tSets = new TreeSet<>();
        for (GTWireSpecs.Row tRow : GTWireSpecs.ROWS) tSets.add(GTWireTextures.blockSetOf(tRow.material().get()));
        assertEquals(Set.of("copper", "shiny", "metallic", "dull", "quartz", "rad", "none"),
                tSets, "census 2026-09-01 — 30 rows, 7 iconsets; 'none' = the setless Superconductor row");
    }

    @Test
    public void everyNeededWireTextureIsBorrowed() {
        for (String tSet : List.of("copper", "shiny", "metallic", "dull", "quartz", "rad", "none")) {
            assertNotNull(getClass().getResource("/assets/gt6/textures/block/materialicons/" + tSet + "/wire.png"),
                    "missing borrowed wire.png for set " + tSet);
        }
        for (String tTail : List.of("tiny", "small", "medium", "large", "huge", "full")) {
            assertNotNull(getClass().getResource("/assets/gt6/textures/block/iconsets/insulation_" + tTail + ".png"),
                    "missing borrowed insulation_" + tTail + ".png");
        }
        // task p11-wire-fiber-texture — the laser fiber pair (MultiTileEntityWireLaser :121-122)
        assertNotNull(getClass().getResource("/assets/gt6/textures/block/iconsets/fiber_wire.png"),
                "missing borrowed fiber_wire.png");
        assertNotNull(getClass().getResource("/assets/gt6/textures/block/iconsets/fiber_wire_overlay.png"),
                "missing borrowed fiber_wire_overlay.png");
    }

    @Test
    public void theSevenWireIconsAreOneUpstreamFile() throws Exception {
        MessageDigest tDigest = MessageDigest.getInstance("SHA-256");
        String tReference = null;
        for (String tSet : List.of("copper", "shiny", "metallic", "dull", "quartz", "rad", "none")) {
            byte[] tBytes = getClass().getResourceAsStream(
                    "/assets/gt6/textures/block/materialicons/" + tSet + "/wire.png").readAllBytes();
            StringBuilder tHex = new StringBuilder();
            for (byte tB : tDigest.digest(tBytes)) tHex.append(String.format("%02x", tB));
            if (tReference == null) tReference = tHex.toString();
            assertEquals(tReference, tHex.toString(), "set " + tSet + " drifted from the shared borrow");
        }
        assertEquals("d9343ea989b6585f8fea8ed6aba7db86f477bc7f63553db1f8d4d06794077bad", tReference,
                "the upstream wire.png bytes (byte-identical borrow)");
        assertTrue(tReference != null && tReference.length() == 64);
    }

    /** Task p11-wire-fiber-texture: the fiber pair pins its upstream sha256 (assets/README.md). */
    @Test
    public void theFiberPairIsTheUpstreamBytes() throws Exception {
        MessageDigest tDigest = MessageDigest.getInstance("SHA-256");
        assertEquals("1b383e640e9882b2cf927dde24cc0a1563c23e025224bb2846946f6a958e731e",
                sha256(tDigest, "/assets/gt6/textures/block/iconsets/fiber_wire.png"),
                "fiber_wire.png must be the byte-identical upstream FIBER_WIRE.png");
        assertEquals("1aa8d9d32a7626d16eec2f54cad4056772a8c6c2abc738c0649994a8ea989add",
                sha256(tDigest, "/assets/gt6/textures/block/iconsets/fiber_wire_overlay.png"),
                "fiber_wire_overlay.png must be the byte-identical upstream FIBER_WIRE_OVERLAY.png");
    }

    private static String sha256(MessageDigest aDigest, String aResource) throws Exception {
        byte[] tBytes = GTWireTextureCensusTest.class.getResourceAsStream(aResource).readAllBytes();
        StringBuilder tHex = new StringBuilder();
        for (byte tB : aDigest.digest(tBytes)) tHex.append(String.format("%02x", tB));
        return tHex.toString();
    }
}
