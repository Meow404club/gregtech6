/**
 * Tests for task p30-ore-1-mech: the ore universe census (26 families, the 74 form-rows,
 * the material axis M, the total block count) + the verbatim family rows + the per-pair
 * id scheme + the block property face + the stone-anchor semantics + the tab structure.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree or a
 * conscious decision is forced):
 * <ul>
 * <li>tasks.p30-arch-ore-registration.enumeration — 26 families = 9 vanilla-anchored
 *     (stone/deepslate/netherrack/endstone/sandstone 3-form + gravel/sand/redsand/mud
 *     2-form broken≡normal) + 17 GT stones 3-form; 74 form-rows per material.</li>
 * <li>Loader_Ores.java:56-128 — the vanilla-anchor rows (hardness/resistance/harvest
 *     offset+minimum/gravity/ender-proof) and the XP columns (:77-83, endstone 2-3 :80).</li>
 * <li>Loader_Ores.java:212 + :438-439 — the deepslate family: the EtFu rockset default
 *     row (1.0/1.0/h0), broken = halved (:474).</li>
 * <li>Loader_Rocks.java:57-139 — the 17 GT-stone rows: normal (h, r, 0, level), broken
 *     (h/2, r/2, -1, level-1, gravity), small (h, r, -1, level); XP 0..max(1, level) :144.</li>
 * <li>PrefixBlock.java:176 — mHarvestLevelMinimum clamps at 0.</li>
 * <li>PrefixBlockItem.java:62-67 — the tab gate: SHOW_ORE_BLOCK_PREFIXES=false default
 *     leaves exactly the stone normal family creative-visible.</li>
 * <li>OP.java:1098 (modern) — OP.ore setCondition(ORES), the material-axis criterion.</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GTStoneBlocksRegistrationTest lesson): the census
 * walks touch only the family table and the material axis — the block-ctor assertions
 * bootstrap + unfreeze the registries themselves.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.block.ore.GTOreFallingBlock;
import gregtech6.registry.GT6OreBlocks.Form;
import gregtech6.registry.GT6OreBlocks.FormKind;
import gregtech6.registry.GT6OreBlocks.OreFamily;

class GT6OreBlocksRegistrationTest {

    /** The 26 family snakes in FAMILIES order (vanilla anchors first, then Loader_Rocks name tails). */
    private static final List<String> SNAKES = List.of(
        "stone", "deepslate", "netherrack", "endstone", "sandstone",
        "gravel", "sand", "redsand", "mud",
        "blackgranite", "redgranite", "basalt", "marble", "limestone", "granite", "diorite",
        "andesite", "komatiite", "greenschist", "blueschist", "kimberlite", "quartzite",
        "lightprismarine", "darkprismarine", "slate", "shale");

    /** The pinned form-row total (22 three-form + 4 two-form families). */
    private static final int PINNED_ROWS = 74;
    /**
     * The pinned material axis M (the reviewer-corrected口径, 2026-09-16): the 53 distinct
     * upstream always-on worldgen small-ore materials (Loader_Worldgen.java:800-852 — 53
     * rows whose redcinnabar :828 / cinnabar :851 pair shares MT.OREMATS.Cinnabar — plus
     * nikolite :875), each passing OP.ore.isGeneratingItem. The architect table's "M>=54"
     * counted ROWS; the unique-material axis is 53. The bare isGeneratingItem walk over
     * the whole MATERIAL_ARRAY measures 618 and was REJECTED in review (nine tenths of it
     * materials no ore placement ever references) — the 45732-block face is gone.
     */
    private static final int PINNED_M = 53;
    /** The pinned total block count (74 x M) — see materialAxisIsPinned. */
    private static final int PINNED_TOTAL = PINNED_ROWS * PINNED_M;

    @BeforeAll
    static void initMaterialSystem() {
        GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        // the block-ctor assertions create intrusive holders past the bootstrap freeze —
        // the GTStoneBlocksRegistrationTest.initMaterialSystem posture, self-sufficient
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Throwable aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    /** The family census: 26 families, walk order, unique snakes, exactly 74 form-rows. */
    @Test
    void familyCensusIsPinned() {
        assertEquals(26, GT6OreBlocks.FAMILIES.size(), "the ruling pins exactly 26 stone families");
        assertEquals(SNAKES, GT6OreBlocks.FAMILIES.stream().map(OreFamily::snake).toList(),
                "family order must be the architect enumeration order");
        Set<String> tSeen = new HashSet<>();
        for (OreFamily tFamily : GT6OreBlocks.FAMILIES) assertTrue(tSeen.add(tFamily.snake()), "unique family snakes");
        assertEquals(PINNED_ROWS, GT6OreBlocks.FAMILIES.stream().mapToInt(f -> f.kinds().size()).sum(),
                "22 three-form + 4 two-form families = the pinned 74 form-rows");
        // every family row triple is complete
        for (OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            assertNotNull(tFamily.normal(), tFamily.snake() + " normal row");
            assertNotNull(tFamily.small(), tFamily.snake() + " small row");
            assertNotNull(tFamily.prefix(), tFamily.snake() + " prefix");
            assertNotNull(tFamily.tool(), tFamily.snake() + " tool");
            assertNotNull(tFamily.sound(), tFamily.snake() + " sound");
            assertNotNull(tFamily.color(), tFamily.snake() + " map colour");
            assertTrue(tFamily.xpMin() <= tFamily.xpMax(), tFamily.snake() + " xp range");
        }
    }

    /** The material axis M (the census pin) and the derived totals. */
    @Test
    void materialAxisIsPinned() {
        List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
        assertEquals(PINNED_M, tAxis.size(),
                "M = the 53 distinct upstream always-on worldgen small-ore materials — bump PINNED_M only with an upstream row delta");
        // every axis material passes the authoritative oredict filter (OP.java:1098 setCondition(ORES))
        for (OreDictMaterial tMaterial : tAxis) {
            assertTrue(OP.ore.isGeneratingItem(tMaterial), "every axis material passes the OP.ore criterion");
        }
        // the axis IS the pinned upstream worldgen set, row order preserved (Loader_Worldgen.java:800-852 + :875)
        List<String> tExpected = List.of(
            "Copper", "Chalcopyrite", "Malachite", "Tin", "Cassiterite", "Zinc", "Sphalerite", "Smithsonite",
            "Stibnite", "Bismuth", "Lead", "Galena", "Silver", "Gold", "Pyrite", "Hematite", "Pyrolusite", "Garnierite",
            "Pentlandite", "Scheelite", "Salt", "Sylvite", "Borax", "Asbestos", "Diamond", "Amber",
            "Craponite", "Redstone", "Cinnabar", "Lapis", "Eudialyte", "Azurite", "Coal", "Graphite",
            "Pollucite", "Zeolite", "Coltan", "Platinum", "Iridium", "Sperrylite", "Cooperite", "Naquadah", "Trinium",
            "Dolamide", "Endium", "Sugilite", "Ambrosium", "Zanite", "Sulfur", "Niter", "Efrine",
            "AncientDebris", "Nikolite");
        assertEquals(tExpected, tAxis.stream().map(m -> m.mNameInternal).toList(),
                "the axis is the pinned always-on worldgen set in row order");
        assertEquals(PINNED_TOTAL, GT6OreBlocks.registrationOrder().size(), "74 x M total blocks");
    }

    /** The 74 form-rows are all non-empty: every (family, kind) row registers exactly M pairs, all ids distinct. */
    @Test
    void formRowsAllNonEmpty() {
        List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
        Set<String> tPaths = new HashSet<>();
        for (OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            for (FormKind tKind : FormKind.values()) {
                Form tForm = tFamily.form(tKind);
                if (tKind == FormKind.BROKEN && tForm == null) continue; // the broken≡normal families
                assertNotNull(tForm, tFamily.snake() + "/" + tKind + " must be a non-empty row");
                for (OreDictMaterial tMaterial : tAxis) {
                    assertTrue(tPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(tFamily, tKind, tMaterial))),
                            "per-pair ids must be unique: " + tFamily.snake() + "/" + tKind + "/" + tMaterial.mNameInternal);
                }
                assertTrue(tPaths.size() >= tAxis.size(), "row non-empty: " + tFamily.snake() + "/" + tKind);
            }
        }
        assertEquals(PINNED_TOTAL, tPaths.size(), "the walk covers 74 x M distinct ids exactly");
    }

    /** The per-pair id scheme: no form segment on normal, the form segment on broken/small, family+material tails. */
    @Test
    void perPairIdSchemeIsPinned() {
        OreDictMaterial tIron = MT.Iron;
        OreFamily tStone = GT6OreBlocks.FAMILIES.get(0);
        assertEquals("ore_stone_iron", GT6OreBlocks.path(new GT6OreBlocks.OreKey(tStone, FormKind.NORMAL, tIron)));
        assertEquals("ore_broken_stone_iron", GT6OreBlocks.path(new GT6OreBlocks.OreKey(tStone, FormKind.BROKEN, tIron)));
        assertEquals("ore_small_stone_iron", GT6OreBlocks.path(new GT6OreBlocks.OreKey(tStone, FormKind.SMALL, tIron)));
        OreFamily tGraniteBlack = GT6OreBlocks.FAMILIES.get(9);
        assertEquals("ore_blackgranite_iron", GT6OreBlocks.path(new GT6OreBlocks.OreKey(tGraniteBlack, FormKind.NORMAL, tIron)),
                "the GT17 family snakes are the upstream internal names (underscore-free tokens)");
    }

    /**
     * The vanilla-anchor rows verbatim (Loader_Ores.java:56-128): the edge cases of every
     * column — hardness/resistance pairs, the harvest offset/minimum, gravity, the
     * ender-proof endstone family, the XP columns, the broken≡normal dust families.
     */
    @Test
    void vanillaAnchorRowsAreVerbatim() {
        OreFamily tStone = GT6OreBlocks.FAMILIES.get(0);
        assertSame(OP.oreVanillastone, tStone.prefix());
        assertEquals(new Form(1.0F, 1.0F, 0, 0, false), tStone.normal(), ":61");
        assertEquals(new Form(0.5F, 0.5F, -1, 0, true), tStone.broken(), ":56");
        assertEquals(new Form(1.0F, 1.0F, -1, 0, false), tStone.small(), ":69");
        assertEquals(0, tStone.xpMin());
        assertEquals(1, tStone.xpMax());

        OreFamily tDeepslate = GT6OreBlocks.FAMILIES.get(1);
        assertSame(OP.oreDeepslate, tDeepslate.prefix());
        assertEquals(new Form(1.0F, 1.0F, 0, 0, false), tDeepslate.normal(), "rockset defaults :439 via :212");
        assertEquals(new Form(0.5F, 0.5F, -1, 0, true), tDeepslate.broken(), ":474 halved+gravity");

        OreFamily tNetherrack = GT6OreBlocks.FAMILIES.get(2);
        assertEquals(new Form(0.5F, 0.5F, 0, 0, false), tNetherrack.normal(), ":63");
        assertEquals(new Form(0.2F, 0.2F, -1, 0, true), tNetherrack.broken(), ":58");

        OreFamily tEndstone = GT6OreBlocks.FAMILIES.get(3);
        assertTrue(tEndstone.enderProof(), "the endstone family is ender-dragon-proof (:59/:64/:72)");
        assertEquals(2, tEndstone.xpMin());
        assertEquals(3, tEndstone.xpMax(), "the :80 XP row");
        assertEquals(new Form(1.0F, 2.0F, 0, 0, false), tEndstone.normal(), ":64");

        OreFamily tSandstone = GT6OreBlocks.FAMILIES.get(4);
        assertEquals(new Form(0.6F, 0.8F, 0, 0, false), tSandstone.normal(), ":62");
        assertEquals(new Form(0.3F, 0.4F, -1, 0, true), tSandstone.broken(), ":57");

        // the two-form dust families: broken==null, gravity rows, shovel tools
        for (int i = 5; i <= 8; i++) {
            OreFamily tDust = GT6OreBlocks.FAMILIES.get(i);
            assertNull(tDust.broken(), tDust.snake() + " is broken≡normal");
            assertTrue(tDust.normal().gravity() && tDust.small().gravity(), tDust.snake() + " rows fall");
            assertEquals("shovel", tDust.tool(), tDust.snake() + " is a TOOL_shovel family");
        }
        OreFamily tGravel = GT6OreBlocks.FAMILIES.get(5);
        assertEquals(new Form(0.6F, 0.8F, 0, 0, true), tGravel.normal(), ":65");
        OreFamily tSand = GT6OreBlocks.FAMILIES.get(6);
        assertEquals(new Form(0.4F, 0.6F, 0, 0, true), tSand.normal(), ":66");
        OreFamily tRedsand = GT6OreBlocks.FAMILIES.get(7);
        assertEquals(new Form(0.4F, 0.6F, 0, 0, true), tRedsand.normal(), ":67");
        OreFamily tMud = GT6OreBlocks.FAMILIES.get(8);
        assertSame(OP.oreMud, tMud.prefix());
        assertEquals(new Form(0.3F, 0.5F, 0, 0, true), tMud.normal(), ":121");
        assertSame(net.minecraft.world.level.block.SoundType.GRAVEL, tMud.sound(), "the :121 soundTypeGravel column");
        assertSame(net.minecraft.world.level.material.MapColor.DIRT, tMud.color(), "the Material.ground stand-in");

        // the pickaxe column for every other family
        for (int i = 0; i <= 4; i++) assertEquals("pickaxe", GT6OreBlocks.FAMILIES.get(i).tool());
        for (int i = 9; i < 26; i++) assertEquals("pickaxe", GT6OreBlocks.FAMILIES.get(i).tool());
        // the small prefix is OP.oreSmall for every family; normal/broken carry the family prefix
        for (OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            assertSame(OP.oreSmall, tFamily.prefix(FormKind.SMALL), tFamily.snake() + " small = OP.oreSmall");
            assertSame(tFamily.prefix(), tFamily.prefix(FormKind.NORMAL), tFamily.snake() + " normal = family prefix");
            assertSame(tFamily.prefix(), tFamily.prefix(FormKind.BROKEN), tFamily.snake() + " broken shares the family prefix");
        }
    }

    /**
     * The 17 GT-stone rows verbatim (Loader_Rocks.java:57-139): normal (h, r, 0, level),
     * broken (h/2, r/2, -1, max(0, level-1), gravity), small (h, r, -1, level); XP
     * 0..max(1, level); the OP-field mapping incl. the granite exception.
     */
    @Test
    void gtStoneRowsAreVerbatim() {
        OreFamily tBlackGranite = GT6OreBlocks.FAMILIES.get(9);
        assertSame(OP.oreBlackgranite, tBlackGranite.prefix());
        assertEquals(new Form(3.0F, 6.0F, 0, 3, false), tBlackGranite.normal(), ":57");
        assertEquals(new Form(1.5F, 3.0F, -1, 2, true), tBlackGranite.broken(), ":58");
        assertEquals(new Form(3.0F, 6.0F, -1, 3, false), tBlackGranite.small(), ":59");
        assertEquals(3, tBlackGranite.xpMax(), ":144 max(1, level)");

        OreFamily tBasalt = GT6OreBlocks.FAMILIES.get(11);
        assertSame(OP.oreBasalt, tBasalt.prefix());
        assertEquals(new Form(2.0F, 3.0F, 0, 2, false), tBasalt.normal(), ":67");
        assertEquals(new Form(1.0F, 1.5F, -1, 1, true), tBasalt.broken(), ":68");

        OreFamily tMarble = GT6OreBlocks.FAMILIES.get(12);
        assertEquals(new Form(0.5F, 0.75F, 0, 0, false), tMarble.normal(), ":72");
        assertEquals(new Form(0.25F, 0.37F, -1, 0, true), tMarble.broken(), ":73 — the level-1 clamp at 0 (PrefixBlock.java:176)");

        OreFamily tGranite = GT6OreBlocks.FAMILIES.get(14);
        assertSame(OP.oreVanillagranite, tGranite.prefix(), "the granite family carries oreVanillagranite (:82)");
        assertEquals(new Form(1.0F, 1.5F, 0, 1, false), tGranite.normal(), ":82");

        OreFamily tPrismarineDark = GT6OreBlocks.FAMILIES.get(23);
        assertSame(OP.oreDarkprismarine, tPrismarineDark.prefix());
        assertEquals(new Form(0.5F, 0.75F, 0, 1, false), tPrismarineDark.normal(), ":127");
        assertEquals(new Form(0.25F, 0.37F, -1, 0, true), tPrismarineDark.broken(), ":128");

        // every GT-stone family: STONE sound/colour, pickaxe, three forms, gravity pattern
        for (int i = 9; i < 26; i++) {
            OreFamily tFamily = GT6OreBlocks.FAMILIES.get(i);
            assertSame(net.minecraft.world.level.block.SoundType.STONE, tFamily.sound(), tFamily.snake() + " sound");
            assertSame(net.minecraft.world.level.material.MapColor.STONE, tFamily.color(), tFamily.snake() + " colour");
            assertFalse(tFamily.normal().gravity() && tFamily.small().gravity(), tFamily.snake() + " normal+small stand");
            assertTrue(tFamily.broken().gravity(), tFamily.snake() + " broken falls");
        }
    }

    /** The block face: pure per-pair blocks carrying the verbatim properties; gravity rows are the FallingBlock subclass. */
    @Test
    void blocksCarryTheirRowsAndGravityColumn() {
        OreFamily tStone = GT6OreBlocks.FAMILIES.get(0);
        OreFamily tGravel = GT6OreBlocks.FAMILIES.get(5);
        OreFamily tGraniteBlack = GT6OreBlocks.FAMILIES.get(9);
        OreDictMaterial tIron = MT.Iron;

        GTOreBlock tNormal = new GTOreBlock(tStone, FormKind.NORMAL, tStone.normal(), tStone.prefix(FormKind.NORMAL), tIron);
        assertSame(tStone, tNormal.family);
        assertSame(OP.oreVanillastone, tNormal.prefix);
        assertEquals(1.0F, tNormal.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F, ":61 hardness");
        assertEquals(1.0F, tNormal.getExplosionResistance(), 1.0e-6F, ":61 resistance (Block.java:331 doubling-free form)");
        // tNormal is a GTOreBlock — only GTOreFallingBlock rows fall (statically guaranteed)

        net.minecraft.world.level.block.Block tBroken =
                new GTOreFallingBlock(tStone, FormKind.BROKEN, tStone.broken(), tStone.prefix(FormKind.BROKEN), tIron);
        assertTrue(tBroken instanceof net.minecraft.world.level.block.FallingBlock, "the :56 gravity column");
        assertEquals(0.5F, tBroken.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F, ":56 hardness");
        assertSame(net.minecraft.world.level.block.SoundType.STONE, tBroken.defaultBlockState().getSoundType(), "the family sound");

        net.minecraft.world.level.block.Block tGravelNormal =
                new GTOreFallingBlock(tGravel, FormKind.NORMAL, tGravel.normal(), tGravel.prefix(FormKind.NORMAL), tIron);
        assertTrue(tGravelNormal instanceof net.minecraft.world.level.block.FallingBlock, "the :65 gravity row");
        assertSame(net.minecraft.world.level.block.SoundType.GRAVEL, tGravelNormal.defaultBlockState().getSoundType());

        GTOreBlock tGraniteNormal = new GTOreBlock(tGraniteBlack, FormKind.NORMAL, tGraniteBlack.normal(),
                tGraniteBlack.prefix(FormKind.NORMAL), tIron);
        assertEquals(3.0F, tGraniteNormal.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F, ":57 hardness");

        // the small forms ride OP.oreSmall and keep the family hardness
        GTOreBlock tSmall = new GTOreBlock(tStone, FormKind.SMALL, tStone.small(), OP.oreSmall, tIron);
        assertSame(OP.oreSmall, tSmall.prefix);
        assertEquals(1.0F, tSmall.defaultBlockState().getDestroySpeed(null, null), 1.0e-6F, ":69 hardness");
        // tSmall is a GTOreBlock — the stone small row stands (statically guaranteed)
        net.minecraft.world.level.block.Block tGravelSmall =
                new GTOreFallingBlock(tGravel, FormKind.SMALL, tGravel.small(), OP.oreSmall, tIron);
        assertTrue(tGravelSmall instanceof net.minecraft.world.level.block.FallingBlock, "the :73 small row falls");
    }

    /**
     * The stone-anchor semantics (the stoneToNormalOres structure): vanilla anchors are the
     * vanilla blocks (live-resolvable offline), mud anchors the vanilla mud (the declared
     * deviation), the GT17 anchors are the GTStoneBlocks handles (live-only, not resolved here).
     */
    @Test
    void stoneAnchorSemantics() {
        OreFamily tStone = GT6OreBlocks.FAMILIES.get(0);
        assertSame(net.minecraft.world.level.block.Blocks.STONE, tStone.stoneAnchor().get());
        assertSame(net.minecraft.world.level.block.Blocks.COBBLESTONE, tStone.brokenAnchor().get(), ":85 vs :93");
        OreFamily tDeepslate = GT6OreBlocks.FAMILIES.get(1);
        assertSame(net.minecraft.world.level.block.Blocks.DEEPSLATE, tDeepslate.stoneAnchor().get());
        assertSame(net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE, tDeepslate.brokenAnchor().get());
        OreFamily tRedsand = GT6OreBlocks.FAMILIES.get(7);
        assertSame(net.minecraft.world.level.block.Blocks.RED_SAND, tRedsand.stoneAnchor().get(), "upstream sand meta 1");
        OreFamily tMud = GT6OreBlocks.FAMILIES.get(8);
        assertSame(net.minecraft.world.level.block.Blocks.MUD, tMud.stoneAnchor().get(), "the Diggables deviation");
        OreFamily tNetherrack = GT6OreBlocks.FAMILIES.get(2);
        assertSame(net.minecraft.world.level.block.Blocks.NETHERRACK, tNetherrack.brokenAnchor().get(), ":94 maps the same rock");
        // the GT17 anchor suppliers exist but resolve live only (GTStoneBlocks handles are
        // populated at RegisterEvent — an empty map offline, so only the supplier presence is pinned)
        OreFamily tGraniteBlack = GT6OreBlocks.FAMILIES.get(9);
        assertNotNull(tGraniteBlack.stoneAnchor(), "the GT17 anchor rides the GTStoneBlocks handle");
        assertNotNull(tGraniteBlack.brokenAnchor(), "upstream :146 maps the same stone into the broken map");
    }

    /** The tab structure: exactly the stone family is upstream-visible (PrefixBlockItem.java:62-67), title key pinned. */
    @Test
    void tabStructureIsUpstreamVisible() {
        assertSame(GT6OreBlocks.FAMILIES.get(0), GT6OreBlocks.TAB_FAMILY);
        assertSame(OP.oreVanillastone, GT6OreBlocks.TAB_FAMILY.prefix());
        assertEquals("itemGroup.gt6.ore_vanillastone", GT6OreBlocks.TAB_TITLE_KEY);
        assertEquals("stone", GT6OreBlocks.TAB_FAMILY.snake());
    }
}
