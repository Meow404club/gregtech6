/**
 * Tests for task debt-slab-gap (the upstream mSlabs[0] gap, decisions.2026-09-26-debt-
 * stairs-walls-slab ③): the 272-pair stone-slab census, the {@code _slab} id scheme, the
 * halved-strength ctor rows and the composed-name face.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>BlockMetaType.java:55/:74-81 — the {@code mSlabs} six-side array; only
 *     {@code mSlabs[0]} (SIDE_DOWN) is visible (:83 hides the five others) and
 *     referenced by the stone rows (BlockStones.java:269/:327).</li>
 * <li>BlockMetaType.java:75-80 — the slab is created with the family multipliers
 *     HALVED, then the :61-62 {@code * 1.5} / {@code * 10} conversion applies.</li>
 * <li>BlockMetaType.java:89/:93 — the generic conversion rows walk ALL 16 metas, so
 *     the faithful slab surface is per-(stone, variant), not COBBL-only.</li>
 * <li>tmp/gregtech.lang:15977-15982 — the {@code gt.stone.<stone>.slab.0.<meta>} naming
 *     family (花岗岩半砖 / 花岗岩圆石半砖 = the variant name + 半砖).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GTStoneBlocksRegistrationTest lesson): the walks
 * touch only the spec records and the enum; the slab block ctor is exercised on fixture
 * instances (the unfreeze bracket).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.block.stone.GTStoneSlabBlock;
import gregtech6.block.stone.StoneVariant;
import net.minecraft.world.level.block.state.properties.SlabType;

class GTStoneSlabBlocksRegistrationTest {

    /** The 272-pair census (17 x 16) — the slab registration walk AND the id yardstick. */
    private static final int PINNED_TOTAL = 272;

    @BeforeAll
    static void initMaterialSystem() {
        // GTStoneBlocksRegistrationTest shape: material system, vanilla bootstrap, the
        // offline unfreeze bracket (the fixture slab ctors create Block instances).
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Throwable aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    /** The census: the slab walk is exactly the 272-pair stone walk (one slab per pair). */
    @Test
    void slabCensusIsPinned() {
        assertEquals(PINNED_TOTAL, GTStoneBlocks.registrationOrder().size(), "17 x 16 pairs, the same walk");
        Set<String> tPaths = new HashSet<>();
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            assertTrue(tPaths.add(GTStoneSlabBlocks.slabPath(tKey.stone().snake(), tKey.variant())),
                    "slab ids must be unique: " + tKey);
        }
        assertEquals(PINNED_TOTAL, tPaths.size(), "272 distinct slab ids");
    }

    /**
     * The id scheme: the slab id is the paired full block's composite path suffixed
     * {@code _slab} — variant 0 {@code gt6:<snake>_slab}, the other 15
     * {@code gt6:<snake>_<variant>_slab} (the vanilla {@code <block>_slab} idiom).
     */
    @Test
    void slabIdSchemeIsPinned() {
        assertEquals("marble_slab", GTStoneSlabBlocks.slabPath("marble", StoneVariant.STONE));
        assertEquals("marble_cobble_slab", GTStoneSlabBlocks.slabPath("marble", StoneVariant.COBBL));
        assertEquals("granite_black_bricks_chiseled_slab", GTStoneSlabBlocks.slabPath("granite_black", StoneVariant.CHISL));
        assertEquals("prismarine_light_windmill_tiles_a_slab", GTStoneSlabBlocks.slabPath("prismarine_light", StoneVariant.WINDA));
    }

    /**
     * The slab block: a vanilla {@link net.minecraft.world.level.block.SlabBlock} with the
     * family multipliers HALVED (BlockMetaType.java:75-80 halving, then the :61-62
     * conversion — hardness = mult * 0.75, resistance = mult * 5), the default state the
     * upstream-visible bottom slab (mSlabs[0] = SIDE_DOWN), and the vanilla TYPE face
     * (bottom/top/double) covering the upstream player-visible surface.
     */
    @Test
    void slabBlockCarriesHalvedUpstreamStrength() {
        GTStoneSlabBlock tSlab = new GTStoneSlabBlock("granite_black", StoneVariant.SMOTH, MT.STONES.GraniteBlack,
                3.00F, 6.00F);
        assertSame(StoneVariant.SMOTH, tSlab.variant, "the slab's variant is fixed at construction");
        assertSame(MT.STONES.GraniteBlack, tSlab.material, "the material rides the block");
        // the impl ignores both args (BlockBehaviour.java:566-568 returns the field) — nulls are the offline form
        assertEquals(3.00F * 1.5F / 2.0F, tSlab.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "slab hardness = 3.00 * 1.5 / 2 (BlockMetaType.java:75 halved, then :61)");
        assertEquals(6.00F * 10.0F / 2.0F, tSlab.getExplosionResistance(), 1.0e-6F,
                "slab resistance = 6.00 * 10 / 2 (BlockMetaType.java:75 halved, then :62)");
        assertEquals(SlabType.BOTTOM,
                tSlab.defaultBlockState().getValue(net.minecraft.world.level.block.SlabBlock.TYPE),
                "the default is the upstream-visible bottom slab (mSlabs[0], SIDE_DOWN)");
        assertTrue(tSlab.defaultBlockState().hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED),
                "the vanilla slab base carries WATERLOGGED");
    }

    /**
     * The composed-name face (the p20 template family): ONE slab template
     * {@code gt6.stone.slab} whose single slot is the SAME composed variant name the full
     * block shows (the variant template over the gt6.material small unit) — the nesting
     * shape the GT6LangParityTest ratchet pins on the lang faces.
     */
    @Test
    void slabNameComposesOverTheVariantName() {
        GTStoneSlabBlock tSlab = new GTStoneSlabBlock("granite_black", StoneVariant.COBBL, MT.STONES.GraniteBlack,
                3.00F, 6.00F);
        net.minecraft.network.chat.contents.TranslatableContents tContents =
                (net.minecraft.network.chat.contents.TranslatableContents) tSlab.getName().getContents();
        assertEquals(GTStoneSlabBlock.SLAB_NAME_KEY, tContents.getKey(),
                "the slab composes the ONE slab template gt6.stone.slab");
        assertEquals(1, tContents.getArgs().length, "one slot: the composed variant name");
        net.minecraft.network.chat.contents.TranslatableContents tVariantSlot =
                (net.minecraft.network.chat.contents.TranslatableContents)
                        ((net.minecraft.network.chat.Component) tContents.getArgs()[0]).getContents();
        assertEquals(StoneVariant.COBBL.key(), tVariantSlot.getKey(),
                "the slot is the variant template the full block composes");
        net.minecraft.network.chat.contents.TranslatableContents tMaterialSlot =
                (net.minecraft.network.chat.contents.TranslatableContents)
                        ((net.minecraft.network.chat.Component) tVariantSlot.getArgs()[0]).getContents();
        assertEquals("gt6.material.granite_black", tMaterialSlot.getKey(),
                "the variant slot's argument is the material small-unit component");
    }
}
