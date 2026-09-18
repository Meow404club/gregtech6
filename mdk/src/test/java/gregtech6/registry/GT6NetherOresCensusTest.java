package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The nether surface-form band census (task p31-nether-lens-end-yield, the coordinator
 * option A): 14 minimal carriers, upstream identity order, distinct paths, resolvable
 * materials. Offline-safe by construction (the KEYS table is a static List, the census
 * walk reads no registry — the GT6BedrockOreBlocksRegistrationTest posture).
 */
public class GT6NetherOresCensusTest {

    @org.junit.jupiter.api.BeforeAll
    static void initMaterials() {
        gregtech6.registry.GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
    }

    @Test
    public void bandShipsFourteenBlocksInUpstreamIdentityOrder() {
        List<GT6NetherOres.NetherOreKey> tKeys = GT6NetherOres.KEYS;
        assertEquals(14, tKeys.size(), "1 dense quartz + 12 crystals + 1 red clay");
        assertEquals("dense_nether_quartz_ore", tKeys.get(0).path(), "RockOres meta 8 (oreDense NetherQuartz)");
        // the 12 crystal metas in BlockCrystalOres.java:43 order
        List<String> tCrystals = List.of(
                "crystal_arsenopyrite", "crystal_chalcopyrite", "crystal_cinnabar", "crystal_cobaltite",
                "crystal_galena", "crystal_kesterite", "crystal_molybdenite", "crystal_pyrite",
                "crystal_sphalerite", "crystal_stannite", "crystal_stibnite", "crystal_tetrahedrite");
        for (int i = 0; i < 12; i++) {
            assertEquals(tCrystals.get(i), tKeys.get(1 + i).path(), "crystal meta " + i + " identity");
        }
        assertEquals("nether_red_clay", tKeys.get(13).path(), "Diggables meta 3 (Loader_Worldgen.java:599)");
    }

    @Test
    public void pathsAreUniqueAndMaterialsResolve() {
        Set<String> tPaths = new HashSet<>();
        for (GT6NetherOres.NetherOreKey tKey : GT6NetherOres.KEYS) {
            assertTrue(tPaths.add(tKey.path()), "duplicate path: " + tKey.path());
            OreDictMaterial tMaterial = tKey.resolve();
            assertNotNull(tMaterial, tKey.path() + " must carry a material");
            assertTrue(tMaterial.mID > 0, tKey.path() + " material must be registered (post-OP.init)");
        }
    }

    @Test
    public void crystalMaterialsAreTheUpstreamSulfideTwelve() {
        List<OreDictMaterial> tAxis = GT6NetherOres.KEYS.subList(1, 13).stream()
                .map(GT6NetherOres.NetherOreKey::resolve).toList();
        assertEquals(MT.OREMATS.Arsenopyrite, tAxis.get(0));
        assertEquals(MT.OREMATS.Chalcopyrite, tAxis.get(1));
        assertEquals(MT.OREMATS.Cinnabar, tAxis.get(2));
        assertEquals(MT.OREMATS.Cobaltite, tAxis.get(3));
        assertEquals(MT.OREMATS.Galena, tAxis.get(4));
        assertEquals(MT.OREMATS.Kesterite, tAxis.get(5));
        assertEquals(MT.OREMATS.Molybdenite, tAxis.get(6));
        assertEquals(MT.Pyrite, tAxis.get(7));
        assertEquals(MT.OREMATS.Sphalerite, tAxis.get(8));
        assertEquals(MT.OREMATS.Stannite, tAxis.get(9));
        assertEquals(MT.OREMATS.Stibnite, tAxis.get(10));
        assertEquals(MT.OREMATS.Tetrahedrite, tAxis.get(11));
    }

    @Test
    public void everyTextureIsAVanillaStandInPath() {
        for (GT6NetherOres.NetherOreKey tKey : GT6NetherOres.KEYS) {
            assertTrue(tKey.vanillaTexture().startsWith("block/"),
                    "the stand-in texture is a minecraft path-tail (block/...): " + tKey.vanillaTexture());
        }
    }
}
