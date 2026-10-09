/**
 * Tests for task concrete-blocks-register: the GT6 concrete universe census (64 per-pair
 * Block+BlockItem ids), the dye-colour table transcription, the upstream ctor rows
 * (strength conversions), the id-scheme ruling, the composed-name face, the tint seam
 * and the generated-tree pin.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_Blocks.java:63/:67 — the two family registrations, declaration order
 *     (concrete then concrete.reinforced).</li>
 * <li>BlockConcrete.java:51 — Material.rock / stone sound / "Concrete" / resistance
 *     2.0F / hardness 1.0F / harvest 1 / CONCRETES icons.</li>
 * <li>BlockConcreteReinforced.java:41 — "Reinforced Concrete" / 8.0F / 4.0F / 3 /
 *     CONCRETES_REINFORCED icons.</li>
 * <li>BlockMetaType.java:61-62 — hardness = multiplier * 1.5, resistance = multiplier
 *     * 10; :75-80/:108-109 — the slab multipliers HALVED before the same conversion.</li>
 * <li>BlockColored.java:45 — 16 metas = the CS.DYE_INDEX order (DYE_NAMES 0=Black..15=
 *     White); :63-73 — the DYES_INT[meta] tint face; :704-705 Textures — ONE grayscale
 *     tile per family (UT.Code.fill), the tint (not per-colour PNGs) carries the colour.</li>
 * <li>Loader_Recipes_Other.java:452-458 — the per-colour Bath outputs over ALL FOUR
 *     forms (the per-pair item identity economy).</li>
 * <li>BlockMetaType.java:92 — the b2c-sawing BLOCKED row this card unlocks:
 *     {@code RM.sawing(ST.make(this,1,i), ST.make(mSlabs[0],2,i))} per colour.</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GTStoneBlocksRegistrationTest shape): the registry
 * class initialises offline (DeferredRegister accumulates suppliers — the
 * GT6RecipeMapDataB2cWashRowsPourTest lesson); the block ctors are driven directly past
 * the bootstrap+unfreeze bracket.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.concrete.GT6ConcreteBlock;
import gregtech6.block.concrete.GT6ConcreteSlabBlock;
import gregtech6.item.spraycan.GTSprayCanItem;

class GT6ConcreteRegistryTest {

    /** The 16-colour census — the BlockColored.java:45 meta set (CS.DYE_INDEX order). */
    private static final int PINNED_TOTAL = 32;

    @BeforeAll
    static void initOfflineJVM() {
        // the GTStoneBlocksRegistrationTest posture: bootstrap vanilla, then unfreeze the
        // block registry so the direct block ctors (intrusive holder past the freeze) work
        // self-sufficiently — no reliance on another test class unfreezing first
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

    /** The census: 2 families x 16 colours, family-major in the Loader_Blocks declaration order. */
    @Test
    void concreteCensusIsPinned() {
        assertEquals(PINNED_TOTAL, GT6ConcreteBlocks.registrationOrder().size(), "2 x 16 registration pairs");
        assertEquals(List.of("concrete", "concrete_reinforced"), GT6ConcreteBlocks.FAMILIES,
                "the Loader_Blocks.java:63/:67 declaration order");
        for (int i = 0; i < 16; i++) {
            assertFalse(GT6ConcreteBlocks.registrationOrder().get(i).reinforced(), "the first band is the plain family");
            assertTrue(GT6ConcreteBlocks.registrationOrder().get(16 + i).reinforced(), "the second band is the reinforced family");
            assertEquals(i, GT6ConcreteBlocks.registrationOrder().get(i).dyeIndex(), "colour-major in meta order");
        }
        assertEquals(PINNED_TOTAL, GT6ConcreteBlocks.FULL_BLOCKS.size(), "32 full blocks");
        assertEquals(PINNED_TOTAL, GT6ConcreteBlocks.SLAB_BLOCKS.size(), "32 slabs (the mSlabs[0] face)");
        assertEquals(64, GT6ConcreteBlocks.ITEMS.size(), "64 block items (the full band then the slab band)");
    }

    /**
     * The id scheme: {@code concrete_<dye>} / {@code concrete_reinforced_<dye>} (+ the
     * slab {@code _slab} suffix), the dye segment the GTSprayCanItem.DYE_IDS snake in
     * CS.DYE_INDEX order; NO bare {@code gt6:concrete} (brand-new family, the ADR ②
     * variant-0-bare rule has no legacy id to shield); the 64 ids are pairwise distinct.
     */
    @Test
    void perPairIdSchemeIsPinned() {
        assertEquals("concrete_light_gray", GT6ConcreteBlocks.path("concrete", 7), "the DYE_IDS snake segment");
        assertEquals("concrete_reinforced_white", GT6ConcreteBlocks.path("concrete_reinforced", 15));
        assertEquals("concrete_black_slab", GT6ConcreteBlocks.slabPath("concrete", 0), "the slab suffix");
        assertEquals("concrete_reinforced_lime_slab", GT6ConcreteBlocks.slabPath("concrete_reinforced", 10));
        // offline discipline: the ids derive from the WALK (no RegistryObject deref — the
        // handle .get() is the runData NPE trap this card fixed in registerItems)
        Set<String> tPaths = new HashSet<>();
        for (GT6ConcreteBlocks.ConcreteRow tRow : GT6ConcreteBlocks.registrationOrder()) {
            assertTrue(tPaths.add(GT6ConcreteBlocks.path(tRow.family(), tRow.dyeIndex())), "distinct full ids: " + tRow);
            assertTrue(tPaths.add(GT6ConcreteBlocks.slabPath(tRow.family(), tRow.dyeIndex())), "distinct slab ids: " + tRow);
        }
        assertEquals(64, tPaths.size(), "64 distinct registry paths");
        assertFalse(tPaths.contains("concrete"), "no bare concrete id — uniform suffixing");
        // every item id is one of the 64 paths (the WrapperHolder carries the id offline;
        // only .get() needs the fired registry)
        Set<String> tItemIds = new HashSet<>();
        for (var tEntry : GT6ConcreteBlocks.ITEMS_REG.getEntries()) {
            tItemIds.add(tEntry.getId().getPath());
        }
        assertEquals(tPaths, tItemIds, "the item band covers exactly the 64 block paths");
    }

    /** The colour walk is the GTSprayCanItem.DYE_IDS table (= the CS.DYE_INDEX meta order), verbatim. */
    @Test
    void colourTableIsTheDyeTable() {
        assertEquals(16, GTSprayCanItem.DYE_IDS.length);
        assertEquals("black", GTSprayCanItem.DYE_IDS[0], "meta 0 = Black");
        assertEquals("light_gray", GTSprayCanItem.DYE_IDS[7], "meta 7 = Light Gray (the Drying product colour)");
        assertEquals("white", GTSprayCanItem.DYE_IDS[15], "meta 15 = White");
        for (int i = 0; i < 16; i++) {
            assertEquals(GT6ConcreteBlocks.path("concrete", i), "concrete_" + GTSprayCanItem.DYE_IDS[i],
                    "the id segment IS the DYE_IDS snake at " + i);
            assertEquals("gt6.dye." + GTSprayCanItem.DYE_IDS[i], GT6ConcreteBlock.dyeKey(i),
                    "the dye small-unit key at " + i);
        }
    }

    /**
     * The upstream ctor rows verbatim (the strength conversions): the full block
     * 1.5/20 (BlockConcrete.java:51 x BlockMetaType.java:61-62), the reinforced 6/80
     * (BlockConcreteReinforced.java:41), the slabs halved (BlockMetaType.java:75-80
     * then :108-109). NO requiresCorrectToolForDrops (the stone-family red line).
     */
    @Test
    void blockCarriesItsFixedColourAndUpstreamStrength() {
        GT6ConcreteBlock tPlain = new GT6ConcreteBlock("concrete_light_gray", 7, false, GT6ConcreteBlock.plainProperties());
        assertEquals(7, tPlain.dyeIndex, "the fixed dye index");
        assertFalse(tPlain.reinforced, "the plain family flag");
        assertTrue(tPlain.defaultBlockState().getProperties().isEmpty(), "the pure-block form (no colour property)");
        assertEquals(1.5F, tPlain.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "hardness = 1.0 * 1.5 (BlockConcrete.java:51, BlockMetaType.java:61)");
        assertEquals(20.0F, tPlain.getExplosionResistance(), 1.0e-6F,
                "resistance = 2.0 * 10 (BlockMetaType.java:62)");
        GT6ConcreteBlock tReinforced = new GT6ConcreteBlock("concrete_reinforced_light_gray", 7, true, GT6ConcreteBlock.reinforcedProperties());
        assertTrue(tReinforced.reinforced, "the reinforced family flag");
        assertEquals(6.0F, tReinforced.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "hardness = 4.0 * 1.5 (BlockConcreteReinforced.java:41)");
        assertEquals(80.0F, tReinforced.getExplosionResistance(), 1.0e-6F,
                "resistance = 8.0 * 10 (BlockMetaType.java:62)");
        GT6ConcreteSlabBlock tSlab = new GT6ConcreteSlabBlock("concrete_light_gray_slab", 7, false, GT6ConcreteSlabBlock.plainProperties());
        assertEquals(0.75F, tSlab.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "slab hardness = 1.0 * 1.5 / 2 (BlockMetaType.java:75-80, :108-109)");
        assertEquals(10.0F, tSlab.getExplosionResistance(), 1.0e-6F, "slab resistance = 2.0 * 10 / 2");
        GT6ConcreteSlabBlock tRSlab = new GT6ConcreteSlabBlock("concrete_reinforced_light_gray_slab", 7, true, GT6ConcreteSlabBlock.reinforcedProperties());
        assertEquals(3.0F, tRSlab.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F,
                "reinforced slab hardness = 4.0 * 1.5 / 2");
        assertEquals(40.0F, tRSlab.getExplosionResistance(), 1.0e-6F, "reinforced slab resistance = 8.0 * 10 / 2");
        assertTrue(tSlab.defaultBlockState().hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE),
                "the vanilla TYPE property (the ruling_slab superset face)");
    }

    /**
     * The composed-name face: the 20-key lang contract — 4 family templates over the
     * gt6.dye small unit (the upstream LH ladder DYE_NAMES[i] + " " + default [+ " Slab"],
     * BlockColored.java:46/:58, %s-ified) — and each block composes ITS family template
     * over its OWN colour unit.
     */
    @Test
    void composedNameFacePinsTheTemplateContract() {
        Set<String> tKeys = new HashSet<>(List.of(GT6ConcreteBlock.BLOCK_NAME_KEY, GT6ConcreteBlock.REINFORCED_NAME_KEY,
                GT6ConcreteSlabBlock.SLAB_NAME_KEY, GT6ConcreteSlabBlock.REINFORCED_SLAB_NAME_KEY));
        assertTrue(tKeys.containsAll(List.of("gt6.concrete.block", "gt6.concrete.block_reinforced",
                "gt6.concrete.slab", "gt6.concrete.slab_reinforced")), "the 4 template keys");
        for (int i = 0; i < 16; i++) {
            assertTrue(GT6ConcreteBlock.dyeKey(i).startsWith("gt6.dye."), "the dye-unit namespace: " + i);
        }
        GT6ConcreteBlock tPlain = new GT6ConcreteBlock("concrete_light_gray", 7, false, GT6ConcreteBlock.plainProperties());
        net.minecraft.network.chat.contents.TranslatableContents tContents =
                (net.minecraft.network.chat.contents.TranslatableContents) tPlain.getName().getContents();
        assertEquals(GT6ConcreteBlock.BLOCK_NAME_KEY, tContents.getKey(), "the plain family composes the plain template");
        assertEquals(1, tContents.getArgs().length, "one slot: the gt6.dye small unit");
        assertEquals("gt6.dye.light_gray",
                ((net.minecraft.network.chat.contents.TranslatableContents)
                        ((net.minecraft.network.chat.Component) tContents.getArgs()[0]).getContents()).getKey(),
                "the colour slot is the block's own dye unit");
        GT6ConcreteBlock tReinforced = new GT6ConcreteBlock("concrete_reinforced_light_gray", 7, true, GT6ConcreteBlock.reinforcedProperties());
        assertEquals(GT6ConcreteBlock.REINFORCED_NAME_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) tReinforced.getName().getContents()).getKey(),
                "the reinforced family composes the reinforced template");
        GT6ConcreteSlabBlock tSlab = new GT6ConcreteSlabBlock("concrete_light_gray_slab", 7, false, GT6ConcreteSlabBlock.plainProperties());
        assertEquals(GT6ConcreteSlabBlock.SLAB_NAME_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) tSlab.getName().getContents()).getKey(),
                "the slab composes the slab template");
        GT6ConcreteSlabBlock tRSlab = new GT6ConcreteSlabBlock("concrete_reinforced_light_gray_slab", 7, true, GT6ConcreteSlabBlock.reinforcedProperties());
        assertEquals(GT6ConcreteSlabBlock.REINFORCED_SLAB_NAME_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) tRSlab.getName().getContents()).getKey(),
                "the reinforced slab composes the reinforced slab template");
    }

    /**
     * The tint seam (the GTCFoamTintListener.cfoamTintARGB posture): the BlockColor is a
     * constant lookup of the block's FIXED dye index over {@code GTSprayCanItem.DYES_INT}
     * (the BlockColored.java:63-73 face), tint index 0 only, non-concrete blocks untinted.
     */
    @Test
    void tintSeamResolvesTheFixedDyeIndex() {
        GT6ConcreteBlock tPlain = new GT6ConcreteBlock("concrete_light_gray", 7, false, GT6ConcreteBlock.plainProperties());
        assertEquals(0xFF000000 | GTSprayCanItem.DYES_INT[7],
                gregtech6.client.concrete.GT6ConcreteTintListener.concreteTintARGB(tPlain, 0),
                "the plain block tints DYES_INT[7] (Light Gray)");
        assertEquals(-1, gregtech6.client.concrete.GT6ConcreteTintListener.concreteTintARGB(tPlain, 1),
                "tint index 1 is outside the family grammar");
        GT6ConcreteSlabBlock tRSlab = new GT6ConcreteSlabBlock("concrete_reinforced_white_slab", 15, true, GT6ConcreteSlabBlock.reinforcedProperties());
        assertEquals(0xFF000000 | GTSprayCanItem.DYES_INT[15],
                gregtech6.client.concrete.GT6ConcreteTintListener.concreteTintARGB(tRSlab, 0),
                "the reinforced slab tints its own fixed index");
        assertEquals(-1, gregtech6.client.concrete.GT6ConcreteTintListener.concreteTintARGB(
                net.minecraft.world.level.block.Blocks.STONE, 0), "non-family blocks stay untinted");
    }

    // ------------------------------------------------------------------ the generated-tree pin

    /** The mdk root (the BurningBoxRenderDatagenTest walk-up). */
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

    /** The tree pin: 64 blockstates + 64 item models + 8 shared models + 66 loot tables, no per-colour model. */
    @Test
    void generatedTreeCarriesTheConcreteFace() throws Exception {
        Path tGen = generated();
        for (int i = 0; i < 16; i++) {
            for (String tFamily : List.of("concrete", "concrete_reinforced")) {
                String tPath = GT6ConcreteBlocks.path(tFamily, i);
                assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tPath + ".json")), tPath + " blockstate");
                assertTrue(Files.exists(tGen.resolve("assets/gt6/models/item/" + tPath + ".json")), tPath + " item model");
                String tSlab = GT6ConcreteBlocks.slabPath(tFamily, i);
                assertTrue(Files.exists(tGen.resolve("assets/gt6/blockstates/" + tSlab + ".json")), tSlab + " blockstate");
                assertTrue(Files.exists(tGen.resolve("assets/gt6/models/item/" + tSlab + ".json")), tSlab + " item model");
                assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tPath + ".json")), tPath + " loot");
                assertTrue(Files.exists(tGen.resolve("data/gt6/loot_tables/blocks/" + tSlab + ".json")), tSlab + " loot");
            }
        }
        // the 8 shared tinted models (2 cubes + 2 slab triads) — and ONLY 8, the shared-model
        // face (no per-colour model files: a per-colour copy would mean the tint leg died).
        // The dyed Cover Panel family model (concrete_panel.json, task
        // material-mc-g1-panels-dyed) shares the "concrete" prefix but is its own family's
        // single tinted cube — excluded here, pinned by GT6PanelsRegistryTest.
        Set<String> tModels = new HashSet<>();
        try (var tStream = Files.list(tGen.resolve("assets/gt6/models/block"))) {
            tStream.map(aPath -> aPath.getFileName().toString())
                    .filter(aName -> aName.startsWith("concrete") && !aName.equals("concrete_panel.json"))
                    .forEach(tModels::add);
        }
        assertEquals(Set.of("concrete.json", "concrete_reinforced.json",
                "concrete_slab_bottom.json", "concrete_slab_top.json", "concrete_slab_double.json",
                "concrete_reinforced_slab_bottom.json", "concrete_reinforced_slab_top.json",
                "concrete_reinforced_slab_double.json"), tModels, "exactly the 8 shared tinted models");
    }

    /**
     * The model-body pins: every family model carries tintindex 0 over the grayscale
     * gt6:block/concrete[_reinforced] tile, and the slab model is ONE element (the
     * runData element-stacking artifact this card's first pass produced — 16 appended
     * copies on the cached builder — pinned to stay dead).
     */
    @Test
    void tintedModelsStaySingleElementOverTheGrayscaleTile() throws Exception {
        var tCube = json(generated().resolve("assets/gt6/models/block/concrete.json"));
        assertEquals("gt6:block/concrete", tCube.getAsJsonObject("textures").get("all").getAsString(), "the grayscale tile");
        assertEquals(1, tCube.getAsJsonArray("elements").size(), "ONE element (the stacking artifact stays dead)");
        assertEquals(0, elementFaceTintindex(tCube, 0, "down"), "tintindex 0");
        var tSlab = json(generated().resolve("assets/gt6/models/block/concrete_slab_bottom.json"));
        assertEquals("gt6:block/concrete", tSlab.getAsJsonObject("textures").get("all").getAsString());
        assertEquals(1, tSlab.getAsJsonArray("elements").size(), "ONE half-cube element");
        assertEquals(8, tSlab.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to").get(1).getAsInt(),
                "the half height");
        assertEquals(0, elementFaceTintindex(tSlab, 0, "up"), "tintindex 0 on the slab too");
        var tDouble = json(generated().resolve("assets/gt6/models/block/concrete_reinforced_slab_double.json"));
        assertEquals("gt6:block/concrete_reinforced", tDouble.getAsJsonObject("textures").get("all").getAsString(),
                "the reinforced tile");
        assertEquals(1, tDouble.getAsJsonArray("elements").size());
    }

    /** The lang-tree pin: the 20 keys land in BOTH generated locale files, values verbatim. */
    @Test
    void generatedLangCarriesThe20Keys() throws Exception {
        var tEn = json(generated().resolve("assets/gt6/lang/en_us.json"));
        var tZh = json(generated().resolve("assets/gt6/lang/zh_cn.json"));
        for (int i = 0; i < 16; i++) {
            String tKey = GT6ConcreteBlock.dyeKey(i);
            assertEquals(GTSprayCanItem.DYE_NAMES[i], tEn.get(tKey).getAsString(), "en dye unit " + tKey);
            assertTrue(tZh.has(tKey), "zh dye unit present " + tKey);
        }
        assertEquals("%s Concrete", tEn.get(GT6ConcreteBlock.BLOCK_NAME_KEY).getAsString());
        assertEquals("%s Reinforced Concrete", tEn.get(GT6ConcreteBlock.REINFORCED_NAME_KEY).getAsString());
        assertEquals("%s Concrete Slab", tEn.get(GT6ConcreteSlabBlock.SLAB_NAME_KEY).getAsString());
        assertEquals("%s Reinforced Concrete Slab", tEn.get(GT6ConcreteSlabBlock.REINFORCED_SLAB_NAME_KEY).getAsString());
        assertEquals("%s混凝土", tZh.get(GT6ConcreteBlock.BLOCK_NAME_KEY).getAsString(), "the dump face verbatim (黑色混凝土)");
        assertEquals("%s强化混凝土半砖", tZh.get(GT6ConcreteSlabBlock.REINFORCED_SLAB_NAME_KEY).getAsString(),
                "the dump face verbatim (黑色强化混凝土半砖)");
        assertEquals("褐色", tZh.get("gt6.dye.brown").getAsString(), "the dump word (282 hits vs 棕色 0)");
    }

    /** The loot pins: the full block self-drops, the slab table doubles ONLY on double. */
    @Test
    void lootFacesAreSelfDropAndSlabDispatch() throws Exception {
        var tFull = json(generated().resolve("data/gt6/loot_tables/blocks/concrete_black.json"));
        assertEquals("gt6:concrete_black",
                tFull.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                        .getAsJsonObject().get("name").getAsString(), "the self-drop");
        var tSlab = json(generated().resolve("data/gt6/loot_tables/blocks/concrete_red_slab.json"));
        assertEquals("gt6:concrete_red_slab",
                tSlab.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                        .getAsJsonObject().get("name").getAsString());
        var tEntry = tSlab.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject();
        assertEquals("minecraft:block_state_property",
                tEntry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonArray("conditions").get(0)
                        .getAsJsonObject().get("condition").getAsString(), "the TYPE dispatch arm");
        assertEquals("double",
                tEntry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonArray("conditions").get(0)
                        .getAsJsonObject().getAsJsonObject("properties").get("type").getAsString());
        assertEquals(2.0,
                tEntry.getAsJsonArray("functions").get(0).getAsJsonObject().get("count").getAsDouble(), 1.0e-9,
                "the double doubles");
    }

    /** The pickaxe tag pin: all 64 ids joined (upstream TOOL_pickaxe over the whole family incl. slabs). */
    @Test
    void pickaxeTagCarriesThe64Ids() throws Exception {
        var tTag = json(generated().resolve("data/minecraft/tags/blocks/mineable/pickaxe.json"));
        Set<String> tValues = new HashSet<>();
        // the band carries both entry shapes since the harvest-bands gating cards: plain ids and
        // {id, required:false} optional rows — unwrap the object form to its id before the pin walk
        tTag.getAsJsonArray("values").forEach(aNode -> tValues.add(
                aNode.isJsonObject() ? aNode.getAsJsonObject().get("id").getAsString() : aNode.getAsString()));
        for (int i = 0; i < 16; i++) {
            for (String tFamily : List.of("concrete", "concrete_reinforced")) {
                assertTrue(tValues.contains("gt6:" + GT6ConcreteBlocks.path(tFamily, i)), "full block in the band: " + tFamily + i);
                assertTrue(tValues.contains("gt6:" + GT6ConcreteBlocks.slabPath(tFamily, i)), "slab in the band: " + tFamily + i);
            }
        }
    }

    /** The borrow pin: the 2 grayscale tiles ship byte-identical (the assets/README.md ledger hashes). */
    @Test
    void borrowedTilesShipByteIdentical() throws Exception {
        assertEquals("fd31a7e446d7a3023aeb304f09be7b693e97a313440d64bde42219be15a26142",
                sha256(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/concrete.png")), "the CONCRETE tile");
        assertEquals("f8b1c0e1e829f261910d536b7b12aa50676e94a3f23f26561d9c06937f1d9041",
                sha256(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/concrete_reinforced.png")),
                "the CONCRETE_REINFORCED tile");
        // the grayscale-by-design pin (16 colour PNGs would mean the tint leg died)
        for (String tName : List.of("concrete", "concrete_reinforced")) {
            var tImage = javax.imageio.ImageIO.read(
                    mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/" + tName + ".png").toFile());
            for (int y = 0; y < tImage.getHeight(); y++) {
                for (int x = 0; x < tImage.getWidth(); x++) {
                    int tArgb = tImage.getRGB(x, y);
                    assertEquals((tArgb >> 16) & 0xFF, (tArgb >> 8) & 0xFF, tName + " r==g at " + x + "," + y);
                    assertEquals((tArgb >> 16) & 0xFF, tArgb & 0xFF, tName + " r==b at " + x + "," + y);
                }
            }
        }
    }

    /** The Gson tree of a generated JSON file (the BurningBoxRenderDatagenTest parser) — all roots are objects. */
    private static com.google.gson.JsonObject json(Path aFile) throws Exception {
        return com.google.gson.JsonParser.parseString(Files.readString(aFile)).getAsJsonObject();
    }

    /** The tintindex of one element face ({@code -1} when the face carries none). */
    private static int elementFaceTintindex(com.google.gson.JsonObject aModel, int aElement, String aFace) {
        var tFace = aModel.getAsJsonArray("elements").get(aElement).getAsJsonObject().getAsJsonObject("faces")
                .getAsJsonObject(aFace);
        return tFace.has("tintindex") ? tFace.get("tintindex").getAsInt() : -1;
    }

    private static String sha256(Path aFile) throws Exception {
        var tDigest = java.security.MessageDigest.getInstance("SHA-256");
        try (var tIn = Files.newInputStream(aFile)) {
            byte[] tBuf = new byte[8192];
            int tRead;
            while ((tRead = tIn.read(tBuf)) > 0) {
                tDigest.update(tBuf, 0, tRead);
            }
        }
        var tHex = new StringBuilder();
        for (byte tByte : tDigest.digest()) {
            tHex.append(String.format("%02x", tByte));
        }
        return tHex.toString();
    }
}
