/**
 * Offline dispatch matrix for task crucible-solid-face-matrix — the ContentFace SOLID arm
 * ({@link GT6CrucibleDatagen#bodyTexture}) spread from the four-family loud table to the
 * FULL upstream texture-set universe (the crucible holds ANY material). Pinned:
 * <ol>
 * <li>the whole {@code OreDictMaterial.MATERIAL_ARRAY} universe dispatches without
 *     throwing, every answer is one of the three mapped families (the vanilla smooth
 *     stone / a borrowed materialicons folder / the shared rough art), and every set
 *     name in the universe is a real {@code MT.SET_*} constant (a typo'd constant would
 *     otherwise fall to the shared art silently);</li>
 * <li>representative per-set dispatch pins — the upstream MT.java anchor per SET bucket,
 *     including the four-family regression (the GT6MoldAssetDatagenTest literals);</li>
 * <li>the solid tint stays the mRGBaSolid pack, the vanilla row stays un-tinted, the
 *     molten arm stays untouched (the bowl-card literals, shared).</li>
 * </ol>
 * Upstream semantics: {@code getTextureSmooth} resolves the material texture set's
 * blockSolid icon (OreDictMaterial.java:983-990 → :974-976 → BlockTextureDefault.java
 * :143-150 {@code mTextureSetsBlock.get(OP.blockSolid.mIconIndexBlock)}, the icon being
 * {@code materialicons/<SET>/<file>}, TextureSet.java:63/:78). The upstream art census
 * behind the mapping: 39 of the 41 sets carry the BYTE-IDENTICAL blockSolid.png (md5
 * 75286903), only STONE and BRICK differ (they share the second art, md5 43496774).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.OreDictMaterial;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.registry.GT6MaterialTestSupport;

public class GT6CrucibleSolidFaceMatrixTest extends GTOfflineTestBase {

    /** The four-family regression — the GT6MoldAssetDatagenTest literals, NOT derived from production code. */
    private static final String STONE_BODY  = "minecraft:block/smooth_stone";
    private static final String CERAMIC_BODY = "gt6:block/materialicons/rough/block_solid";
    private static final String BRONZE_BODY  = "gt6:block/materialicons/copper/block_solid";
    private static final String STEEL_BODY   = "gt6:block/materialicons/metallic/block_solid";

    /** The Obsidian solid RGBa 80/50/100 — the MT.java:2395 create(...) tail, packed the {@code argb} way. */
    private static final int OBSIDIAN_SOLID_TINT = 0xFF503264;

    @BeforeAll
    static void bootMaterials() {
        // the matrix walk dereferences MT statics (the GT6CrucibleBowlDatagenTest shape)
        GT6MaterialTestSupport.materials(); // the hermetic bracket (task hermetic-pour-tests)
    }

    /** Every set name the MT.SET_* constants define (reflection, so a new constant can't be forgotten here). */
    private static Set<String> legalSetNames() throws IllegalAccessException {
        Set<String> rSet = new HashSet<>();
        for (Field tField : gregapi.data.MT.class.getDeclaredFields()) {
            if (!tField.getName().startsWith("SET_")) continue;
            assertTrue(tField.getType() == String[].class, tField.getName() + ": the SET_* constants are the String[2] name references");
            rSet.add(((String[])tField.get(null))[0]);
        }
        return rSet;
    }

    /**
     * The universe walk: every runtime-resolvable material (the MATERIAL_ARRAY is exactly what
     * GT6Crucibles.materialById hands the Jade bar and the BER — the consumers of this dispatch)
     * maps without throwing, to a well-formed family, with a legal set name.
     */
    @Test
    void everyRegisteredMaterialDispatchesCleanly() throws Exception {
        Set<String> tLegalSets = legalSetNames();
        assertTrue(tLegalSets.size() >= 40, "the SET_* constant walk broke — never pass vacuously (" + tLegalSets.size() + ")");

        int tCount = 0;
        Set<String> tSetNames = new HashSet<>(), tPaths = new HashSet<>();
        for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_ARRAY) {
            if (tMaterial == null) continue;
            tCount++;
            String tSetName = tMaterial.mTextureSetsBlock.isEmpty() ? "NONE" : tMaterial.mTextureSetsBlock.get(0);
            assertTrue(tLegalSets.contains(tSetName),
                    tMaterial.mNameInternal + " (#" + tMaterial.mID + "): set name '" + tSetName + "' is not an MT.SET_* constant");
            tSetNames.add(tSetName);
            String tTexture = assertDoesNotThrow(() -> GT6CrucibleDatagen.bodyTexture(tMaterial),
                    tMaterial.mNameInternal + " (#" + tMaterial.mID + ")");
            assertTrue(tTexture.equals(STONE_BODY) || (tTexture.startsWith("gt6:block/materialicons/") && tTexture.endsWith("/block_solid")),
                    tMaterial.mNameInternal + ": unexpected body texture " + tTexture);
            tPaths.add(tTexture);
            // the tint rule stays derived from the mapping's own namespace (the bodyTinted contract)
            assertEquals(!tTexture.startsWith("minecraft:"), GT6CrucibleDatagen.bodyTinted(tMaterial),
                    tMaterial.mNameInternal + ": the bodyTinted rule drifted from the mapping");
        }
        assertTrue(tCount >= 300, "the universe walk broke — never pass vacuously (" + tCount + " materials)");
        assertTrue(tSetNames.size() >= 15, "the set census collapsed — never pass vacuously (" + tSetNames + ")");
        // the mapping is finitely branched: the vanilla row + at most one path per borrowed folder
        assertTrue(tPaths.size() <= 24, "unexpected texture families appeared: " + tPaths);
        System.out.println("[solid-face-matrix] " + tCount + " materials, " + tSetNames.size()
                + " distinct sets, " + tPaths.size() + " distinct body paths");
    }

    /**
     * The SET dispatch matrix — one representative per bucket, the upstream MT.java anchor
     * commented per row. Buckets: the four-family regression; the STONE-set exception art via
     * the brick borrow; the borrowed sets riding their own folder; the remaining sets riding
     * the shared rough art (byte-identical upstream, md5 75286903).
     */
    @Test
    void setRepresentativesDispatchToThePinnedArt() {
        // --- the four-family regression (the GT6MoldAssetDatagenTest literals) ---
        assertEquals(STONE_BODY,   GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Stone),   "Stone: the recorded #40-41 vanilla deviation (four-family)");
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Ceramic), "Ceramic: the SET_ROUGH borrow (four-family)");
        assertEquals(BRONZE_BODY,  GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Bronze),  "Bronze: the SET_COPPER borrow (four-family)");
        assertEquals(STEEL_BODY,   GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Steel),   "Steel: the SET_METALLIC borrow (four-family)");
        // --- the STONE set exception: upstream STONE/blockSolid is byte-identical to BRICK (md5 43496774) ---
        assertEquals("gt6:block/materialicons/brick/block_solid", GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Obsidian), "Obsidian: SET_STONE (MT.java:2395) rides the brick art");
        assertEquals("gt6:block/materialicons/brick/block_solid", GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Lava),     "Lava: SET_STONE (MT.java:2052) rides the brick art");
        // --- borrowed sets ride their own folder ---
        assertEquals("gt6:block/materialicons/lignite/block_solid",  GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Coal),        "Coal: SET_LIGNITE (MT.java:2312)");
        assertEquals("gt6:block/materialicons/fiery/block_solid",    GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Pyrotheum),   "Pyrotheum: SET_FIERY (MT.java:2405)");
        assertEquals("gt6:block/materialicons/lapis/block_solid",    GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Amazonite),   "Amazonite: SET_LAPIS (MT.java:2265)");
        assertEquals("gt6:block/materialicons/food/block_solid",     GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Tofu),        "Tofu: SET_FOOD (MT.java:2181)");
        assertEquals("gt6:block/materialicons/diamond/block_solid",  GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Apatite),     "Apatite: SET_DIAMOND (MT.java:2146)");
        assertEquals("gt6:block/materialicons/redstone/block_solid", GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Zanite),      "Zanite: SET_REDSTONE (MT.java:2264)");
        assertEquals("gt6:block/materialicons/wood/block_solid",     GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.PetrifiedWood), "Petrified Wood: SET_WOOD (MT.java:2115)");
        assertEquals("gt6:block/materialicons/shiny/block_solid",    GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Magic),       "Magic: SET_SHINY (MT.java:1395)");
        // --- the remaining sets ride the shared rough art (no per-set borrow, byte-identical upstream) ---
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.NetherStar),   "Nether Star: SET_NETHERSTAR (MT.java:2299) — shared art");
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Glass),        "Glass: SET_GLASS (MT.java:1941) — shared art");
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Flint),        "Flint: SET_FLINT (MT.java:1942) — shared art");
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Paper),        "Paper: SET_PAPER (MT.java:2148) — shared art");
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Oilsands),     "Oil Sand: SET_SAND (MT.java:2057) — shared art");
        assertEquals(CERAMIC_BODY, GT6CrucibleDatagen.bodyTexture(gregapi.data.MT.Alexandrite),  "Alexandrite: SET_OPAL (MT.java:2266) — shared art");
    }

    /** The solid tint stays the mRGBaSolid pack; the vanilla row stays un-tinted; the molten arm stays untouched. */
    @Test
    void solidArmTintIsTheMRgbaSolidPack() {
        // the bowl-card literals, SHARED (not re-declared) — one drift catches both tests
        assertEquals(GT6CrucibleBowlDatagenTest.CERAMIC_SOLID_TINT,
                GT6CrucibleDatagen.contentFace(gregapi.data.MT.Ceramic, false).tintARGB(),
                "Ceramic: the solid tint is the shared mRGBaSolid literal");
        assertEquals(OBSIDIAN_SOLID_TINT,
                GT6CrucibleDatagen.contentFace(gregapi.data.MT.Obsidian, false).tintARGB(),
                "Obsidian: the solid tint is the MT.java:2395 RGBa pack");
        assertEquals(-1, GT6CrucibleDatagen.contentFace(gregapi.data.MT.Stone, false).tintARGB(),
                "Stone: the finished vanilla texture takes no tint");
        assertEquals(GT6CrucibleBowlDatagenTest.STEEL_LIQUID_TINT,
                GT6CrucibleDatagen.contentFace(gregapi.data.MT.Steel, true).tintARGB(),
                "the molten arm is untouched by the solid-arm spread");
    }

    /** The ContentFace seam is TOTAL across the universe — the Jade bar and the BER never see a throw. */
    @Test
    void contentFaceIsTotalAcrossTheUniverse() {
        int tCount = 0;
        for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_ARRAY) {
            if (tMaterial == null) continue;
            assertDoesNotThrow(() -> GT6CrucibleDatagen.contentFace(tMaterial, false),
                    tMaterial.mNameInternal + ": the solid arm must answer for every material");
            assertDoesNotThrow(() -> GT6CrucibleDatagen.contentFace(tMaterial, true),
                    tMaterial.mNameInternal + ": the molten arm must answer for every material");
            tCount++;
        }
        assertTrue(tCount >= 300, "the universe walk broke — never pass vacuously (" + tCount + " materials)");
    }
}
