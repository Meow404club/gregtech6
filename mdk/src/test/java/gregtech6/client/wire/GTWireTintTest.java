/**
 * The wire tint seam (task p9-wire-family-w2): index 0 = the material {@code fRGBaSolid}
 * (the unpainted TE default, TileEntityBase07Paintable.unpaint :83), index 1 = the fixed
 * insulation jacket — PER FAMILY since task p11-wire-brightness spec 3: the electric gray
 * 64,64,64 (MultiTileEntityWireElectric.java:237-238) vs the redstone {@code 96,64,64}
 * (MultiTileEntityWireRedstoneInsulated.java:184-185, the INSULATION_FULL side jacket AND
 * the tier caps carry the same constant), everything else -1. Pure-function form — no
 * block instantiation needed.
 */
package gregtech6.client.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs.Row.Family;

public class GTWireTintTest {

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @Test
    public void materialColourRidesIndexZero() {
        // tin: setRGBa 220,220,220 (MT.java :1046) — the getRGBInt ARGB encoding
        assertEquals(0xFFDCDCDC, GTWireTint.tintARGB(gregapi.data.MT.Sn, Family.ELECTRIC, 0));
        // copper: 255,150,60-ish per MT — just pin the encoding shape, not the exact row:
        int tCu = GTWireTint.tintARGB(gregapi.data.MT.Cu, Family.ELECTRIC, 0);
        assertEquals(0xFF000000, tCu & 0xFF000000, "opaque alpha");
        assertEquals(gregapi.data.MT.Cu.fRGBaSolid[0], (tCu >> 16) & 0xFF);
        assertEquals(gregapi.data.MT.Cu.fRGBaSolid[1], (tCu >> 8) & 0xFF);
        assertEquals(gregapi.data.MT.Cu.fRGBaSolid[2], tCu & 0xFF);
        // the dye is family-independent — the redstone family's material quads tint the same way
        assertEquals(0xFFDCDCDC, GTWireTint.tintARGB(gregapi.data.MT.Sn, Family.REDSTONE, 0));
    }

    @Test
    public void theJacketIsPerFamily() {
        assertEquals(0xFF404040, GTWireTint.tintARGB(gregapi.data.MT.Sn, Family.ELECTRIC, 1),
                "the electric jacket: upstream 64,64,64 (WireElectric :237-238)");
        assertEquals(0xFF604040, GTWireTint.tintARGB(gregapi.data.MT.RedAlloy, Family.REDSTONE, 1),
                "the redstone jacket: upstream 96,64,64 (WireRedstoneInsulated :184-185)");
        assertEquals(0xFF604040, GTWireTint.tintARGB(gregapi.data.MT.Lumium, Family.REDSTONE, 1),
                "every redstone row — the jacket colour is the family constant, not the material");
        assertEquals(0xFF404040, GTWireTint.tintARGB(null, Family.ELECTRIC, 1),
                "the electric jacket is material-independent (the legacy pair)");
        assertEquals(0xFF404040, GTWireTint.tintARGB(null, null, 1),
                "a family-less block falls back to the electric constant");
    }

    @Test
    public void noTintOtherwise() {
        assertEquals(-1, GTWireTint.tintARGB(null, Family.ELECTRIC, 0), "the legacy pair (no material) renders untinted");
        assertEquals(-1, GTWireTint.tintARGB(gregapi.data.MT.Sn, Family.ELECTRIC, 2), "unknown indexes never tint");
        assertEquals(-1, GTWireTint.tintARGB(gregapi.data.MT.Sn, Family.REDSTONE, -1));
        assertEquals(-1, GTWireTint.tintARGB(gregapi.data.MT.RedAlloy, Family.REDSTONE, 2));
    }
}
