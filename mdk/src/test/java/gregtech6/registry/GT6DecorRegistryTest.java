/**
 * Tests for task material-mc-g2-decor-misc: the GT6 decor-misc universe census (the 7
 * families, meta-level parity), the upstream ctor rows (strength conversions), the id
 * schemes, the spike state machine + damage tables, the bale variant grammar, the tint
 * seam and the generated-tree pin.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_Blocks.java:59 (Asphalt 16 dye metas) / :72-73 (Glass/GlowGlass) /
 *     :83 (Paths) / :94-98 (Spikes x5, 2 materials x 8 shapes each = 16 metas) /
 *     :106-111 (Bars x6 Wood/Brass/Steel/Ti/WS/Ad) / :128-129 (BalesGrass/Crop x4
 *     variants x axis) — the seven family rows this card lands.</li>
 * <li>BlockAsphalt.java:44 x BlockMetaType.java:61-62 — asphalt strength 1.5/10;
 *     BlockGlassClear.java:288 / BlockGlassGlow.java:370 — glass 0.75/5 + the glow
 *     lightLevel 1.0F; BlockPath.java:231-232 — path 1.2/0.9 (grass x2 / x1.5);
 *     BlockBaseBars.java:142-143 + BlockBarsWood.java:44 — bars 5.0/3(wood)-5(metal);
 *     BlockBaseBale.java:50-52 — bales = the vanilla hay row; BlockBaseSpike.java:112-113
 *     — spikes 30/5.</li>
 * <li>BlockBaseSpike.java:65-68 — the per-material damage tables (sharp 5/2.5+10/5,
 *     steel 8/4, super 15/7.5+50/25, metal/fancy 20/10 vs prey); :104 the OPOS[clicked]
 *     placement face; :110 damageDropped (the wall item collapse).</li>
 * <li>BlockBaleGrass.java:490-523 — the transform face (nether dry / wet moldy-rotten /
 *     else dry, dry+rotten terminal).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GT6ConcreteRegistryTest shape): the registry
 * classes initialise offline (DeferredRegister accumulates suppliers), the block ctors
 * are driven directly past the bootstrap+unfreeze bracket.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.decor.GT6AsphaltBlock;
import gregtech6.block.decor.GT6BaleBlock;
import gregtech6.block.decor.GT6BarsBlock;
import gregtech6.block.decor.GT6GlassBlock;
import gregtech6.block.decor.GT6PathBlock;
import gregtech6.block.decor.GT6SpikeBlock;
import gregtech6.item.spraycan.GTSprayCanItem;

class GT6DecorRegistryTest {

    @BeforeAll
    static void initOfflineJVM() {
        // the GT6ConcreteRegistryTest posture: bootstrap vanilla, then unfreeze the block
        // registry so the direct block ctors work self-sufficiently
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
        // the spike names + tint seam dereference MT.* (GT6SpikeBlock.nameOf / the fRGBaSolid face):
        // the hermetic material bracket — without it a cold fork reads null materials (the fork-5 NPE)
        GT6MaterialTestSupport.materials();
    }

    // ------------------------------------------------------------------ the census

    /** The seven-family census: the meta-level parity of the Loader_Blocks.java rows. */
    @Test
    void decorCensusIsPinned() {
        assertEquals(16, GT6DecorBlocks.ASPHALT_BLOCKS.size(), "Loader_Blocks.java:59 — 16 dye metas");
        assertEquals(16, GT6DecorBlocks.GLASS_BLOCKS.size(), ":72 — 16 dye metas");
        assertEquals(16, GT6DecorBlocks.GLOW_GLASS_BLOCKS.size(), ":73 — 16 dye metas");
        assertEquals(6, GT6DecorBlocks.BARS_BLOCKS.size(), ":106-111 — Wood/Brass/Steel/Ti/WS/Ad");
        assertEquals(8, GT6DecorBlocks.GRASS_BALES.size() + GT6DecorBlocks.CROP_BALES.size(), ":128-129 — 2 x 4 variants");
        assertEquals(10, GT6Spikes.BLOCKS.size(), ":94-98 — 5 families x 2 materials");
        assertEquals(30, GT6Spikes.ITEMS.size(), "wall/omni/falling x 10 materials (the crafted identities)");
        assertEquals(63, GT6DecorBlocks.ITEMS.size(), "63 decor block items (16+16+16+path+6+8)");
        assertEquals(List.of("wood", "brass", "steel", "titanium", "tungstensteel", "adamantium"),
                GT6DecorBlocks.BARS_ROWS.stream().map(gregtech6.registry.GT6DecorBlocks.BarsRow::family).toList(),
                "the :106-111 declaration order");
    }

    /** The id schemes: the dye snake segments, the spike triads, 73+30 distinct paths. */
    @Test
    void idSchemesArePinned() {
        assertEquals("asphalt_light_gray", GT6DecorBlocks.dyePath("asphalt", 7));
        assertEquals("glow_glass_white", GT6DecorBlocks.dyePath("glow_glass", 15));
        assertEquals("glass_" + GTSprayCanItem.DYE_IDS[0], GT6DecorBlocks.dyePath("glass", 0));
        assertEquals("spike_steel", GT6Spikes.path(GT6Spikes.ROWS.get(0)), "the wall spike id");
        assertEquals("spike_steel_block", GT6Spikes.omniPath(GT6Spikes.ROWS.get(0)), "the omni id");
        assertEquals("spike_steel_falling", GT6Spikes.fallingPath(GT6Spikes.ROWS.get(0)), "the falling id");
        Set<String> tPaths = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            assertTrue(tPaths.add(GT6DecorBlocks.dyePath("asphalt", i)));
            assertTrue(tPaths.add(GT6DecorBlocks.dyePath("glass", i)));
            assertTrue(tPaths.add(GT6DecorBlocks.dyePath("glow_glass", i)));
        }
        tPaths.add("path");
        for (var tRow : GT6DecorBlocks.BARS_ROWS) assertTrue(tPaths.add("bars_" + tRow.family()));
        for (String tBale : GT6DecorBlocks.BALE_PATHS) assertTrue(tPaths.add(tBale));
        assertEquals(63, tPaths.size(), "63 distinct GT6DecorBlocks paths");
        for (var tRow : GT6Spikes.ROWS) {
            assertTrue(tPaths.add(GT6Spikes.path(tRow)));
            assertTrue(tPaths.add(GT6Spikes.omniPath(tRow)));
            assertTrue(tPaths.add(GT6Spikes.fallingPath(tRow)));
        }
        assertEquals(63 + 30, tPaths.size(), "every decor path distinct");
    }

    // ------------------------------------------------------------------ the strength rows

    /** The upstream ctor rows verbatim (the strength conversions, the concrete-test form). */
    @Test
    void blocksCarryTheirUpstreamStrengths() {
        GT6AsphaltBlock tAsphalt = new GT6AsphaltBlock("asphalt_black", 0, GT6AsphaltBlock.decorProperties());
        assertEquals(1.5F, tAsphalt.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "asphalt hardness = 1.0 * 1.5 (BlockAsphalt.java:44 x BlockMetaType.java:61)");
        assertEquals(10.0F, tAsphalt.getExplosionResistance(), 1.0e-6F, "asphalt resistance = 1.0 * 10");
        GT6GlassBlock tGlass = new GT6GlassBlock("glass_black", 0, false, GT6GlassBlock.plainProperties());
        assertEquals(0.75F, tGlass.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "glass hardness = 0.5 * 1.5 (BlockGlassClear.java:288)");
        assertEquals(5.0F, tGlass.getExplosionResistance(), 1.0e-6F);
        assertFalse(tGlass.glow, "the plain family");
        assertFalse(tGlass.defaultBlockState().getLightEmission() > 0, "plain glass emits none");
        GT6GlassBlock tGlow = new GT6GlassBlock("glow_glass_black", 0, true, GT6GlassBlock.glowProperties());
        assertTrue(tGlow.glow, "the glow family");
        assertEquals(15, tGlow.defaultBlockState().getLightEmission(), "setLightLevel(1.0F) = the 15 emission");
        GT6PathBlock tPath = new GT6PathBlock(GT6PathBlock.decorProperties());
        assertEquals(1.2F, tPath.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "path hardness = grass 0.6 x 2 (BlockPath.java:231)");
        assertEquals(0.9F, tPath.getExplosionResistance(), 1.0e-6F, "path resistance = grass 0.6 x 1.5 (:232)");
        GT6BarsBlock tWood = new GT6BarsBlock("bars_wood", gregapi.data.ANY.Wood, true, GT6BarsBlock.woodProperties());
        assertEquals(5.0F, tWood.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F, "bars hardness 5 (BlockBaseBars.java:142)");
        assertEquals(3.0F, tWood.getExplosionResistance(), 1.0e-6F, "wood bars resistance 3 (BlockBarsWood.java:44)");
        assertTrue(tWood.flammable, "the wood flammability flag");
        GT6BarsBlock tMetal = new GT6BarsBlock("bars_steel", gregapi.data.MT.Steel, false, GT6BarsBlock.metalProperties());
        assertEquals(5.0F, tMetal.getExplosionResistance(), 1.0e-6F, "metal bars resistance 5 (BlockBaseBars.java:143)");
        assertFalse(tMetal.flammable, "the metal rows are fire-immune");
        GT6BaleBlock tBale = new GT6BaleBlock("grass_bale", true, GT6BaleBlock.BaleVariant.FRESH, GT6BaleBlock.baleProperties());
        assertEquals(0.5F, tBale.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "bale hardness = the vanilla hay row (BlockBaseBale.java:50)");
        GT6SpikeBlock tSpike = new GT6SpikeBlock("spike_steel", gregapi.data.MT.Steel, 5.0F, 2.5F, null, null, GT6SpikeBlock.spikeProperties());
        assertEquals(30.0F, tSpike.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "spike hardness 30 (BlockBaseSpike.java:112)");
        assertEquals(5.0F, tSpike.getExplosionResistance(), 1.0e-6F, "spike resistance 5 (:113)");
    }

    // ------------------------------------------------------------------ the spike machine

    /** The spike state machine: 24 states (6 facings x omni x falling), the meta ladder split. */
    @Test
    void spikeStateMachineIsPinned() {
        GT6SpikeBlock tSpike = new GT6SpikeBlock("spike_steel", gregapi.data.MT.Steel, 5.0F, 2.5F, null, null, GT6SpikeBlock.spikeProperties());
        assertEquals(24, tSpike.getStateDefinition().getPossibleStates().size(), "6 FACING x 2 OMNI x 2 FALLING = the 16-meta ladder");
        assertTrue(tSpike.defaultBlockState().hasProperty(GT6SpikeBlock.FACING), "the orientation property");
        assertFalse(tSpike.defaultBlockState().getValue(GT6SpikeBlock.OMNI), "the wall form is the default (the meta 0 face)");
        assertFalse(tSpike.defaultBlockState().getValue(GT6SpikeBlock.FALLING), "the wall form does not fall (the useGravity(meta 7) face)");
        // the material split: 5 families x 2 materials, family-major (the :94-98 order)
        assertEquals("sharp", GT6Spikes.ROWS.get(0).family());
        assertEquals(gregapi.data.MT.Ti, GT6Spikes.mat(GT6Spikes.ROWS.get(1)), "sharp = Steel+Ti");
        assertEquals(gregapi.data.MT.BlueSteel, GT6Spikes.mat(GT6Spikes.ROWS.get(2)), "steel = BlueSteel+RedSteel");
        assertEquals(gregapi.data.MT.RedSteel, GT6Spikes.mat(GT6Spikes.ROWS.get(3)));
        assertEquals(gregapi.data.MT.TungstenSteel, GT6Spikes.mat(GT6Spikes.ROWS.get(4)), "super = WS+Ad");
        assertEquals(gregapi.data.MT.Ad, GT6Spikes.mat(GT6Spikes.ROWS.get(5)));
        assertEquals(gregapi.data.MT.Cu, GT6Spikes.mat(GT6Spikes.ROWS.get(6)), "metal = Cu+Pb");
        assertEquals(gregapi.data.MT.Pb, GT6Spikes.mat(GT6Spikes.ROWS.get(7)));
        assertEquals(gregapi.data.MT.Au, GT6Spikes.mat(GT6Spikes.ROWS.get(8)), "fancy = Au+Ag");
        assertEquals(gregapi.data.MT.Ag, GT6Spikes.mat(GT6Spikes.ROWS.get(9)));
    }

    /** The damage tables (the five subclass rows verbatim, wall/omni half-hearts). */
    @Test
    void spikeDamageTablesAreTheUpstreamRows() {
        float[][] tExpected = {
                {5.0F, 2.5F}, {10.0F, 5.0F},   // sharp: Steel 5/2.5, Ti 10/5
                {8.0F, 4.0F}, {8.0F, 4.0F},    // steel: Blue/Red 8/4
                {15.0F, 7.5F}, {50.0F, 25.0F}, // super: WS 15/7.5, Ad 50/25
                {20.0F, 10.0F}, {20.0F, 10.0F},// metal: Cu/Pb 20/10 vs prey
                {20.0F, 10.0F}, {20.0F, 10.0F}};// fancy: Au/Ag 20/10 vs prey
        for (int i = 0; i < 10; i++) {
            assertEquals(tExpected[i][0], GT6Spikes.ROWS.get(i).wallDamage(), "wall damage row " + i);
            assertEquals(tExpected[i][1], GT6Spikes.ROWS.get(i).omniDamage(), "omni damage row " + i);
        }
        // the prey/immunity gates: metal and fancy carry prey arms, the plain families null
        assertTrue(GT6Spikes.ROWS.get(6).prey() != null, "Cu = the slime arm");
        assertTrue(GT6Spikes.ROWS.get(7).prey() != null, "Pb = the arthropod arm");
        assertTrue(GT6Spikes.ROWS.get(8).prey() != null, "Au = the undead arm");
        assertTrue(GT6Spikes.ROWS.get(9).prey() != null, "Ag = the ender arm");
        assertTrue(GT6Spikes.ROWS.get(0).prey() == null && GT6Spikes.ROWS.get(1).prey() == null,
                "the sharp family has no prey arm");
        assertTrue(GT6Spikes.ROWS.get(2).immune() != null, "BlueSteel = the golem immunity");
        assertTrue(GT6Spikes.ROWS.get(1).immune() == null, "Ti has no immunity arm");
    }

    /** The composed names: three identities over the material unit (the dump compose). */
    @Test
    void spikeNameTemplatesArePinned() {
        GT6SpikeBlock tSpike = new GT6SpikeBlock("spike_steel", gregapi.data.MT.Steel, 5.0F, 2.5F, null, null, GT6SpikeBlock.spikeProperties());
        assertEquals(GT6SpikeBlock.WALL_NAME_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) tSpike.nameOf(false, false).getContents()).getKey());
        assertEquals(GT6SpikeBlock.OMNI_NAME_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) tSpike.nameOf(true, false).getContents()).getKey());
        assertEquals(GT6SpikeBlock.FALLING_NAME_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) tSpike.nameOf(true, true).getContents()).getKey());
    }

    // ------------------------------------------------------------------ the bale grammar

    /** The bale grammar: grass transforms, crop inert; the four variants in the dump order. */
    @Test
    void baleVariantGrammarIsPinned() {
        assertEquals(List.of("grass_bale", "dry_grass_bale", "moldy_grass_bale", "rotten_grass_bale"),
                java.util.Arrays.asList(GT6DecorBlocks.BALE_PATHS).subList(0, 4), "the gt.block.bale.grass.0-3 order");
        assertEquals(List.of("rye_bale", "oats_bale", "barley_bale", "rice_bale"),
                java.util.Arrays.asList(GT6DecorBlocks.BALE_PATHS).subList(4, 8), "the gt.block.bale.crop.0-3 order");
        for (int i = 0; i < 4; i++) {
            GT6BaleBlock tGrass = new GT6BaleBlock("grass_bale", true, GT6BaleBlock.BaleVariant.values()[i], GT6BaleBlock.baleProperties());
            GT6BaleBlock tCrop = new GT6BaleBlock("rye_bale", false, GT6BaleBlock.BaleVariant.values()[i], GT6BaleBlock.baleProperties());
            assertTrue(tGrass.grass, "the grass family owns the transform face");
            assertFalse(tCrop.grass, "the crop family is inert (BlockBaleCrop has no updateTick)");
            assertTrue(tGrass.defaultBlockState().hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS),
                    "the pillar axis property (the PILLAR_RENDER face)");
        }
        assertEquals(4, GT6BaleBlock.BaleVariant.values().length, "the meta&3 variant band");
    }

    // ------------------------------------------------------------------ the tint seam

    /** The tint seam: the dye face over DYES_INT, the material face over fRGBaSolid, the rest untinted. */
    @Test
    void tintSeamResolvesTheTwoFaces() {
        GT6AsphaltBlock tAsphalt = new GT6AsphaltBlock("asphalt_black", 0, GT6AsphaltBlock.decorProperties());
        assertEquals(0xFF000000 | GTSprayCanItem.DYES_INT[0],
                gregtech6.client.decor.GT6DecorTintListener.tintARGB(tAsphalt, 0), "the asphalt dye face");
        GT6GlassBlock tGlow = new GT6GlassBlock("glow_glass_white", 15, true, GT6GlassBlock.glowProperties());
        assertEquals(0xFF000000 | GTSprayCanItem.DYES_INT[15],
                gregtech6.client.decor.GT6DecorTintListener.tintARGB(tGlow, 0), "the glow glass dye face");
        assertEquals(-1, gregtech6.client.decor.GT6DecorTintListener.tintARGB(tGlow, 1), "tint index 1 outside the grammar");
        GT6SpikeBlock tSpike = new GT6SpikeBlock("spike_gold", gregapi.data.MT.Au, 20.0F, 10.0F, null, null, GT6SpikeBlock.spikeProperties());
        int tGoldTint = gregtech6.client.decor.GT6DecorTintListener.tintARGB(tSpike, 0);
        assertTrue(tGoldTint != -1 && tGoldTint != 0xFFFFFFFF, "the material face resolves a real colour");
        GT6PathBlock tPath = new GT6PathBlock(GT6PathBlock.decorProperties());
        assertEquals(-1, gregtech6.client.decor.GT6DecorTintListener.tintARGB(tPath, 0), "the path rides its own art");
        GT6BaleBlock tBale = new GT6BaleBlock("grass_bale", true, GT6BaleBlock.BaleVariant.FRESH, GT6BaleBlock.baleProperties());
        assertEquals(-1, gregtech6.client.decor.GT6DecorTintListener.tintARGB(tBale, 0), "the bales ride per-variant art");
        assertEquals(-1, gregtech6.client.decor.GT6DecorTintListener.tintARGB(net.minecraft.world.level.block.Blocks.STONE, 0),
                "non-family blocks stay untinted");
    }

    // ------------------------------------------------------------------ the generated-tree pin

    /** The mdk root (the GT6ConcreteRegistryTest walk-up). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (p.resolve("src/main/resources/gregtech6/lang/zh_cn_ref.tsv").toFile().exists()
                    && p.resolve("mdk").toFile().exists()) {
                return p.resolve("mdk");
            }
            if (p.resolve("src/generated/resources").toFile().exists()) {
                return p;
            }
        }
        throw new IllegalStateException("mdk root not found from " + Path.of("").toAbsolutePath());
    }

    private static Path generated() {
        return mdkRoot().resolve("src/generated/resources");
    }

    /** The tree pin: the 73 blockstates + their item models + loot tables + the shared models. */
    @Test
    void generatedTreeCarriesTheDecorFace() throws Exception {
        Path tGen = generated();
        for (int i = 0; i < 16; i++) {
            for (String tFamily : List.of("asphalt", "glass", "glow_glass")) {
                String tPath = GT6DecorBlocks.dyePath(tFamily, i);
                assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tPath + ".json")), tPath + " blockstate");
                assertTrue(Files.exists(tGen.resolve("assets/gt6/models/item/" + tPath + ".json")), tPath + " item model");
                assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tPath + ".json")), tPath + " loot");
            }
        }
        assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/path.json")), "the path blockstate");
        assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/path.json")), "the path loot");
        for (var tRow : GT6DecorBlocks.BARS_ROWS) {
            String tPath = "bars_" + tRow.family();
            assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tPath + ".json")), tPath + " blockstate");
            assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tPath + ".json")), tPath + " loot");
        }
        for (String tBale : GT6DecorBlocks.BALE_PATHS) {
            assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tBale + ".json")), tBale + " blockstate");
            assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tBale + ".json")), tBale + " loot");
        }
        for (var tRow : GT6Spikes.ROWS) {
            String tWall = GT6Spikes.path(tRow);
            assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tWall + ".json")), tWall + " blockstate");
            assertTrue(Files.exists(tGen.resolve("assets/gt6/models/item/" + tWall + "_block.json")), tWall + " omni item");
            assertTrue(Files.exists(tGen.resolve("assets/gt6/models/item/" + tWall + "_falling.json")), tWall + " falling item");
            assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tWall + ".json")), tWall + " loot");
        }
        // the shared-model face: one asphalt + one glass model (the tint carries the colour)
        assertTrue(Files.exists(tGen.resolve("assets/gt6/models/block/asphalt.json")), "the shared asphalt model");
        assertTrue(Files.exists(tGen.resolve("assets/gt6/models/block/glass_clear.json")), "the shared glass model");
        assertTrue(Files.exists(tGen.resolve("assets/gt6/models/block/decor_spike.json")), "the shared spike model");
    }
}
