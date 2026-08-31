/**
 * The wire tint seam (task p9-wire-family-w2): index 0 = the material {@code fRGBaSolid}
 * (the unpainted TE default, TileEntityBase07Paintable.unpaint :83), index 1 = the fixed
 * insulation gray 64,64,64 (MultiTileEntityWireElectric.java:237-238), everything else -1.
 * Pure-function form — no block instantiation needed.
 */
package gregtech6.client.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GTMaterialItems;

public class GTWireTintTest {

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @Test
    public void materialColourRidesIndexZero() {
        // tin: setRGBa 220,220,220 (MT.java :1046) — the getRGBInt ARGB encoding
        assertEquals(0xFFDCDCDC, GTWireTint.tintARGB(gregapi.data.MT.Sn, 0));
        // copper: 255,150,60-ish per MT — just pin the encoding shape, not the exact row:
        int tCu = GTWireTint.tintARGB(gregapi.data.MT.Cu, 0);
        assertEquals(0xFF000000, tCu & 0xFF000000, "opaque alpha");
        assertEquals(gregapi.data.MT.Cu.fRGBaSolid[0], (tCu >> 16) & 0xFF);
        assertEquals(gregapi.data.MT.Cu.fRGBaSolid[1], (tCu >> 8) & 0xFF);
        assertEquals(gregapi.data.MT.Cu.fRGBaSolid[2], tCu & 0xFF);
    }

    @Test
    public void insulationGrayRidesIndexOne() {
        assertEquals(0xFF404040, GTWireTint.tintARGB(gregapi.data.MT.Sn, 1), "the upstream 64,64,64 jacket");
        assertEquals(0xFF404040, GTWireTint.tintARGB(null, 1), "the jacket is material-independent");
    }

    @Test
    public void noTintOtherwise() {
        assertEquals(-1, GTWireTint.tintARGB(null, 0), "the legacy pair (no material) renders untinted");
        assertEquals(-1, GTWireTint.tintARGB(gregapi.data.MT.Sn, 2), "unknown indexes never tint");
        assertEquals(-1, GTWireTint.tintARGB(gregapi.data.MT.Sn, -1));
    }
}
