/**
 * The client param table (task p9-wire-family-w2): 620 electric family paths + 6 redstone
 * rows (task p11-wire-brightness) + 1 laser row (task p11-wire-fiber-texture) + 2 legacy
 * anchors, each carrying its row's texture set, insulated form and PX_P diameter — the
 * dispatch fuel {@link GTWireClientListener} feeds the bake replacement. Spot rows pin the
 * set census per form (bare wires AND cables of one row share the set — the insulation is
 * a quad layer, not a different texture set). The laser row pins the fixed FIBER_WIRE+OVERLAY
 * pair (MultiTileEntityWireLaser.java:121-122) and the electric rows pin overlaySprite ==
 * null (the branch separation — the p11 laser form must never leak into the electric plans).
 */
package gregtech6.client.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTRenderModelListener;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.resources.ResourceLocation;

public class GTWireClientParamsTest {

    @BeforeAll
    public static void buildTable() {
        GTMaterialItems.initMaterials();
        GTWireClientListener.clearForTest();
        GTWireClientListener.buildParams();
    }

    @Test
    public void tableCoversTheFullSpectrum() {
        assertEquals(629, GTWireClientListener.paramsCount(),
                "620 electric rows + 6 redstone rows + 1 laser row + 2 legacy anchors");
        assertNotNull(GTWireClientListener.paramsFor("wire_tin_gt01"));
        assertNotNull(GTWireClientListener.paramsFor("cable_tungsten_gt08"));
        assertNotNull(GTWireClientListener.paramsFor("wire_superconductor_gt16"));
        assertNotNull(GTWireClientListener.paramsFor("wire_laser"), "the p11 laser row joins the per-state MRL table");
        assertNotNull(GTWireClientListener.paramsFor("wire_red_alloy"), "the p11 redstone rows join the per-state MRL table");
        assertNull(GTWireClientListener.paramsFor("wire_unknown_gt01"), "no phantom paths");
        assertNull(GTWireClientListener.paramsFor("oven"), "non-wire paths stay vanilla");
    }

    @Test
    public void redstoneRowsCarryTheElectricForm() {
        // task p11-wire-brightness — the six rows (3 materials x wire/cable), no size ladder,
        // so the registry names carry no _gt tail (GTWireSpecs.registryName).
        for (String tPath : new String[] {"wire_red_alloy", "cable_red_alloy", "wire_signalum",
                "cable_signalum", "wire_lumium", "cable_lumium"}) {
            assertNotNull(GTWireClientListener.paramsFor(tPath), tPath + " must be on the per-state MRL table");
            assertNull(GTWireClientListener.paramsFor(tPath).overlaySprite(),
                    tPath + " rides the electric planner (no fiber overlay)");
        }
        GTWireBakedModel.Params tWire = GTWireClientListener.paramsFor("wire_red_alloy");
        assertEquals(false, tWire.insulated(), "the bare form (upstream getTextureSide :81 = the material wire icon)");
        assertEquals(2, tWire.diameterPx(), "PX_P[2] (Loader:1895 NBT_DIAMETER)");
        assertEquals("block/materialicons/copper/wire", tWire.wireSprite().getPath(),
                "all three materials resolve to the borrowed copper set (the datagen shared-model source)");
        GTWireBakedModel.Params tCable = GTWireClientListener.paramsFor("cable_lumium");
        assertEquals(true, tCable.insulated(), "the cable form carries the insulation layers (upstream :184-185)");
        assertEquals(4, tCable.diameterPx(), "PX_P[4] (Loader:1902 NBT_DIAMETER)");
        assertEquals("block/materialicons/copper/wire", tCable.wireSprite().getPath(),
                "the jacket is a quad layer — the set stays the row's set");
    }

    @Test
    public void spotRowsCarryTheirRowIdentity() {
        GTWireBakedModel.Params tTin01 = GTWireClientListener.paramsFor("wire_tin_gt01");
        assertEquals(false, tTin01.insulated());
        assertEquals(2, tTin01.diameterPx(), "wireGt01 = PX_P[2]");
        assertEquals(new ResourceLocation(GTRenderModelListener.MOD_ID, "block/materialicons/copper/wire"),
                tTin01.wireSprite(), "tin = SET_COPPER");

        GTWireBakedModel.Params tCable12 = GTWireClientListener.paramsFor("cable_tin_gt12");
        assertEquals(true, tCable12.insulated());
        assertEquals(16, tCable12.diameterPx(), "cableGt12 = PX_P[16]");
        assertEquals("block/materialicons/copper/wire", tCable12.wireSprite().getPath(),
                "cables keep the row's set — the jacket is a quad layer, not a texture set");

        GTWireBakedModel.Params tSuper = GTWireClientListener.paramsFor("wire_superconductor_gt01");
        assertEquals("block/materialicons/none/wire", tSuper.wireSprite().getPath(),
                "the setless Superconductor row lands on the borrowed SET_NONE texture");
        assertEquals(false, tSuper.insulated());

        GTWireBakedModel.Params tQuartz = GTWireClientListener.paramsFor("wire_carborundum_gt16");
        assertEquals("block/materialicons/quartz/wire", tQuartz.wireSprite().getPath(), "SiC = SET_QUARTZ");
        GTWireBakedModel.Params tRad = GTWireClientListener.paramsFor("wire_naquadah_gt08");
        assertEquals("block/materialicons/rad/wire", tRad.wireSprite().getPath(), "Naquadah = SET_RAD");
        GTWireBakedModel.Params tDull = GTWireClientListener.paramsFor("cable_lead_gt04");
        assertEquals("block/materialicons/dull/wire", tDull.wireSprite().getPath(), "Lead = SET_DULL");
    }

    @Test
    public void legacyPairStaysOnThePlaceholderTexture() {
        GTWireBakedModel.Params tLegacy = GTWireClientListener.paramsFor("wire_electric_1x");
        assertEquals(new ResourceLocation(GTRenderModelListener.MOD_ID, "block/wire_electric"), tLegacy.wireSprite());
        assertEquals(false, tLegacy.insulated());
        assertEquals(0, tLegacy.diameterPx(), "diameter 0 = the model floors at PX_P[2] (readFromNBT2 :64 clamp)");
        assertEquals(tLegacy, GTWireClientListener.paramsFor("wire_electric_2x"), "both anchors share the form");
    }

    @Test
    public void laserRowCarriesTheFixedFiberPair() {
        // MultiTileEntityWireLaser.java:121-122 — BlockTextureMulti(FIBER_WIRE tinted mRGBa,
        // FIBER_WIRE_OVERLAY untinted); Loader:1814-1815 — one block, PX_P[6], no cable form.
        GTWireBakedModel.Params tLaser = GTWireClientListener.paramsFor("wire_laser");
        assertNotNull(tLaser);
        assertEquals(new ResourceLocation(GTRenderModelListener.MOD_ID, "block/iconsets/fiber_wire"), tLaser.wireSprite(),
                "the borrowed FIBER_WIRE base (tint index 0 = the mRGBa dye)");
        assertEquals(new ResourceLocation(GTRenderModelListener.MOD_ID, "block/iconsets/fiber_wire_overlay"),
                tLaser.overlaySprite(), "the untinted FIBER_WIRE_OVERLAY layer");
        assertEquals(false, tLaser.insulated(), "the bare fiber form — no cable upstream");
        assertEquals(6, tLaser.diameterPx(), "PX_P[6] (Loader:1815 NBT_DIAMETER)");
    }

    @Test
    public void electricRowsStayOverlayFree() {
        // branch separation: null overlaySprite = the pre-p11 electric/redstone planner form
        assertNull(GTWireClientListener.paramsFor("wire_tin_gt01").overlaySprite());
        assertNull(GTWireClientListener.paramsFor("cable_tin_gt12").overlaySprite());
        assertNull(GTWireClientListener.paramsFor("wire_electric_1x").overlaySprite());
    }
}
