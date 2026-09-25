/**
 * Tests for task p30-ore-4-loot: the ore loot parity — the offline decision faces of
 * {@link GT6OreLootTables} pinned column-by-column against the upstream Drops /
 * Drops_SmallOre semantics (the card's 对拍表; the live RCON break-drop leg is card 5).
 *
 * <p>Compile anchors:
 * <ul>
 * <li>Drops.java:72-76 — the four-way ternary ({@code aFortune>0 ? aSilkTouch?
 *     mDropFortune : mDropSilkFortune : aSilkTouch? mDropSilkTouch : mDropNormal} with
 *     count {@code mFortunable?1+RNGSUS.nextInt(aFortune+1):1}) — fortune beats silk
 *     (mPreferSilk=false, Loader_Ores.java:77-83 five-arg ctor), count
 *     {@code 1 + uniform(0..fortune)} = vanilla {@code apply_bonus uniform_bonus_count}
 *     bonusMultiplier 1 (vendored 1.21.1 ApplyBonusCount.java:174-176).</li>
 * <li>Loader_Ores.java:77-83 — the XP columns: 0-1 regular, endstone 2-3 (:80).</li>
 * <li>Loader_Ores.java:81-83 + :128 — the loose dust families broken≡normal
 *     (mDropNormal == mDropSilkTouch == the block itself).</li>
 * <li>Drops_SmallOre.java:48-103 — the collapsed small-ore face; the ruling
 *     2026-09-16 pins the drop to rockGt of the BLOCK's material (the ore-bearing
 *     rock), NOT the seed column (mud's seed is null :122, deepslate has no row).</li>
 * <li>LootTable.java:40-44 — no experience field on either leg: the XP column stays
 *     data on OreFamily (block-side attachment = the declared carry, see
 *     GT6OreLootTables javadoc).</li>
 * </ul>
 *
 * <p>Offline-safe: the walks touch only the family table, the material axis and the
 * GTMaterialItems enumeration (registration-order walk, offline = live, the
 * GT6RecipesOreChainTest precedent) — no registry-bound handle is resolved.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6OreLootTables.Arm;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6OreBlocks.FormKind;
import gregtech6.registry.GT6OreBlocks.OreFamily;
import gregtech6.registry.GT6OreBlocks.OreKey;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

class GT6OreLootParityTest {

    /** The pinned per-kind table counts (26 three-or-two-form families x 53 materials). */
    private static final int PINNED_M = 53;

    @BeforeAll
    static void initMaterialSystem() {
        GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap(); // the family table statics (SoundType) need the vanilla bootstrap
        } catch (Throwable ignored) {
        }
    }

    /** The per-form table census: every registered block gets exactly one table. */
    @Test
    void tableCensusIsPinned() {
        int tNormal = 0, tBroken = 0, tSmall = 0;
        for (OreKey tKey : GT6OreBlocks.registrationOrder()) {
            switch (tKey.kind()) {
                case NORMAL -> tNormal++;
                case BROKEN -> tBroken++;
                case SMALL -> tSmall++;
            }
        }
        int tM = GT6OreBlocks.materialAxis().size();
        assertEquals(PINNED_M, tM, "the axis pin (GT6OreBlocksRegistrationTest yardstick)");
        assertEquals(26 * tM, tNormal, "every family's normal form tables");
        assertEquals(22 * tM, tBroken, "the 22 three-form families' broken tables");
        assertEquals(26 * tM, tSmall, "every family's small tables");
        assertEquals(74 * tM, tNormal + tBroken + tSmall, "3922 = 74 x 53, one table per block");
    }

    /**
     * The Drops.java:74 four-way ternary, arm-for-arm over every registered block and
     * every (fortune, silk) condition pair — including the fortune-beats-silk priority
     * (the mPreferSilk=false face: both fortune>0 branches read the raw pair).
     */
    @Test
    void dispatchMatchesUpstreamTernary() {
        for (OreKey tKey : GT6OreBlocks.registrationOrder()) {
            for (int tFortune : new int[] {0, 1, 3}) {
                for (boolean tSilk : new boolean[] {false, true}) {
                    Arm tArm = GT6OreLootTables.armOf(tKey, tFortune, tSilk);
                    // the upstream ternary reference, verbatim shape
                    boolean tUpRaw = tKey.kind() == FormKind.NORMAL && tFortune > 0;
                    boolean tUpSilkSelf = !tUpRaw && tKey.kind() == FormKind.NORMAL
                            && tKey.family().broken() != null && tSilk;
                    Arm tExpected = tUpRaw ? Arm.FORTUNE_RAW : tUpSilkSelf ? Arm.SILK_SELF : Arm.PLAIN;
                    assertEquals(tExpected, tArm,
                            tKey.family().snake() + "/" + tKey.kind() + "/" + tKey.material().mNameInternal
                                    + " fortune=" + tFortune + " silk=" + tSilk);
                }
            }
        }
    }

    /** The arm items column: raw/rock = the same material's OP item, silk/self = the block, plain = the pair rule. */
    @Test
    void armItemsMatchTheUpstreamColumns() {
        List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
        for (OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            for (FormKind tKind : tFamily.kinds()) {
                for (OreDictMaterial tMaterial : tAxis) {
                    OreKey tKey = new OreKey(tFamily, tKind, tMaterial);
                    // the raw arm: the OP.oreRaw item of the SAME material (Loader_Ores.java:77-83)
                    if (tKind == FormKind.NORMAL) {
                        assertEquals(GTMaterialItems.itemIdOf(OP.oreRaw, tMaterial),
                                GT6OreLootTables.armPath(tKey, Arm.FORTUNE_RAW), "the raw column");
                    }
                    // the silk arm: the block itself
                    if (GT6OreLootTables.armOf(tKey, 0, true) == Arm.SILK_SELF) {
                        assertEquals(GT6OreBlocks.path(tKey), GT6OreLootTables.armPath(tKey, Arm.SILK_SELF),
                                "the silk column drops self");
                    }
                    // the plain arm: broken-pair sibling for the three-form normals, self otherwise,
                    // rockGt for the small form
                    String tPlain = GT6OreLootTables.armPath(tKey, Arm.PLAIN);
                    switch (tKind) {
                        case NORMAL -> assertEquals(GT6OreBlocks.path(tKey.family().broken() == null ? tKey
                                : new OreKey(tFamily, FormKind.BROKEN, tMaterial)), tPlain, "the plain column");
                        case BROKEN -> assertEquals(GT6OreBlocks.path(tKey), tPlain, "broken self-drop");
                        case SMALL -> assertEquals(GTMaterialItems.itemIdOf(OP.rockGt, tMaterial), tPlain,
                                "small = the rockGt(X) bearing rock (the 2026-09-16 ruling)");
                    }
                }
            }
        }
    }

    /** The concrete id composition, pinned on the stone/copper reference triple (Copper = the axis head). */
    @Test
    void referencePathsArePinned() {
        OreFamily tStone = GT6OreBlocks.FAMILIES.get(0);
        OreDictMaterial tCopper = GT6OreBlocks.materialAxis().stream()
                .filter(m -> "Copper".equals(m.mNameInternal)).findFirst().orElse(null);
        assertNotNull(tCopper, "Copper rides the axis");
        OreKey tNormal = new OreKey(tStone, FormKind.NORMAL, tCopper);
        assertEquals("ore_raw_copper", GT6OreLootTables.armPath(tNormal, Arm.FORTUNE_RAW));
        assertEquals("ore_stone_copper", GT6OreLootTables.armPath(tNormal, Arm.SILK_SELF));
        assertEquals("ore_broken_stone_copper", GT6OreLootTables.armPath(tNormal, Arm.PLAIN));
        assertEquals("ore_gravel_copper", GT6OreLootTables.armPath(
                new OreKey(GT6OreBlocks.FAMILIES.get(5), FormKind.NORMAL, tCopper), Arm.PLAIN),
                "the loose families drop themselves (broken≡normal, :81-83)");
        assertEquals("rock_gt_copper", GT6OreLootTables.armPath(
                new OreKey(tStone, FormKind.SMALL, tCopper), Arm.PLAIN), "small = rockGt(copper)");
    }

    /** The loose dust families: no silk arm (self under every fortune-0 condition), broken==null rows. */
    @Test
    void looseFamiliesAreBrokenNormal() {
        for (String tSnake : List.of("gravel", "sand", "redsand", "mud")) {
            OreFamily tFamily = GT6OreBlocks.FAMILIES.stream().filter(f -> tSnake.equals(f.snake())).findFirst()
                    .orElseThrow();
            assertTrue(tFamily.broken() == null, tSnake + " has no broken block");
            for (OreDictMaterial tMaterial : GT6OreBlocks.materialAxis()) {
                OreKey tKey = new OreKey(tFamily, FormKind.NORMAL, tMaterial);
                assertEquals(Arm.PLAIN, GT6OreLootTables.armOf(tKey, 0, true), tSnake + " silk is not an arm");
                assertEquals(GT6OreBlocks.path(tKey), GT6OreLootTables.armPath(tKey, Arm.PLAIN),
                        tSnake + " drops itself at fortune 0 (Loader_Ores.java:81-83)");
            }
        }
    }

    /**
     * The XP columns (the loot-JSON-unrepresentable face, pinned as data): the nine
     * vanilla-anchor families read Loader_Ores.java:77-83 — 0-1 regular, endstone 2-3
     * (:80, the mud row rides :128); the 17 GT stones read Loader_Rocks.java:144 —
     * 0..max(1, level), the level riding the normal form's harvestLevelMinimum. The
     * small form rides the family column family-uniform (the declared deviation from
     * the Drops_SmallOre super(1, 2) 1..3 face).
     */
    @Test
    void xpColumnsMatchUpstream() {
        for (OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            switch (tFamily.snake()) {
                case "endstone" -> {
                    assertEquals(2, tFamily.xpMin(), "endstone xp min (:80)");
                    assertEquals(3, tFamily.xpMax(), "endstone xp max (:80)");
                }
                case "stone", "deepslate", "netherrack", "sandstone", "gravel", "sand", "redsand", "mud" -> {
                    assertEquals(0, tFamily.xpMin(), tFamily.snake() + " xp min");
                    assertEquals(1, tFamily.xpMax(), tFamily.snake() + " xp max");
                }
                default -> {
                    assertEquals(0, tFamily.xpMin(), tFamily.snake() + " xp min (:144)");
                    assertEquals(Math.max(1, tFamily.normal().harvestLevelMinimum()), tFamily.xpMax(),
                            tFamily.snake() + " xp max = max(1, level), Loader_Rocks.java:144");
                }
            }
            assertTrue(tFamily.xpMin() <= tFamily.xpMax(), tFamily.snake() + " xp range sane");
        }
    }

    /** The raw and rock items exist in the registered universe for every axis material (the loot items resolve). */
    @Test
    void lootItemsExistInTheUniverse() {
        Set<PrefixMaterial> tUniverse = new HashSet<>(GTMaterialItems.registrationOrder());
        for (OreDictMaterial tMaterial : GT6OreBlocks.materialAxis()) {
            assertTrue(tUniverse.contains(new PrefixMaterial(OP.oreRaw, tMaterial)),
                    "oreRaw item registered for " + tMaterial.mNameInternal);
            assertTrue(tUniverse.contains(new PrefixMaterial(OP.rockGt, tMaterial)),
                    "rockGt item registered for " + tMaterial.mNameInternal);
        }
    }
}
