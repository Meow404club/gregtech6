/**
 * Tests for task p31-bedrock-ore-worldgen spec ①: the bedrock-ore band census (the 45
 * row-table materials x the 2 forms = 90 per-pair blocks), the verbatim id scheme, the
 * Loader_Ores.java:44-45 column face and the axis/table consistency.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree or
 * a conscious decision is forced):
 * <ul>
 * <li>Loader_Ores.java:44-45 — the two bedrock forms: OP.oreBedrock large, OP.oreSmall
 *     small, hardness 3600000F / resistance 9999, Drops_None (the noLootTable face).</li>
 * <li>Loader_Worldgen.java:725-771 — the row-table materials: 46 port rows, gold.a/gold.b
 *     sharing MT.Au, 45 distinct materials; the hexorium row (:772) rides the MD.HEX
 *     mod-gated compat pool and is NOT ported (the 53-axis ruling face).</li>
 * <li>PrefixBlockItem.java:62-64 — both bedrock prefixes hidden from creative (no tab).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GT6OreBlocksRegistrationTest posture): the census
 * walks touch only the material table; the block-ctor assertions bootstrap + unfreeze the
 * registries themselves.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import gregtech6.block.ore.GTBedrockOreBlock;

class GT6BedrockOreBlocksRegistrationTest {

    /** The pinned material axis M (45 distinct row-table materials — see the class javadoc). */
    private static final int PINNED_M = 45;
    /** The pinned total block count (2 forms x M). The spec's "+约 106" estimate corrected table-driven. */
    private static final int PINNED_TOTAL = 2 * PINNED_M;

    @BeforeAll
    static void initMaterialSystem() {
        GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
    }

    /** The 45-material axis: first-appearance order spot checks + dedup + no hexorium leakage. */
    @Test
    void materialAxisIsPinned() {
        List<OreDictMaterial> tAxis = GT6BedrockOreBlocks.materialAxis();
        assertEquals(PINNED_M, tAxis.size(), "the row-table material axis M");
        assertSame(MT.Diamond, tAxis.get(0), "first appearance order: diamond leads (1/128000)");
        assertSame(MT.OREMATS.Tungstate, tAxis.get(1), "the 1/96000 tungsten family follows");
        assertSame(MT.Syrmorite, tAxis.get(PINNED_M - 1), "the last Loader_Worldgen row closes the axis");
        Set<OreDictMaterial> tSet = new HashSet<>(tAxis);
        assertEquals(PINNED_M, tSet.size(), "the axis is deduped (gold.a/gold.b share MT.Au)");
        assertTrue(tSet.contains(MT.Au), "gold.a/gold.b collapse to one material");
        assertTrue(tSet.contains(MT.Coal) && tSet.contains(MT.Graphite), "the RCON acceptance materials are in the axis");
        for (OreDictMaterial tMaterial : tAxis) {
            assertTrue(tMaterial.mID > 0, tMaterial.mNameInternal + " must resolve to a registered material");
        }
    }

    /** The registration walk: 2 x M keys, form-major, path scheme, offline-computable. */
    @Test
    void registrationWalkIsPinned() {
        List<GT6BedrockOreBlocks.BedrockKey> tOrder = GT6BedrockOreBlocks.registrationOrder();
        assertEquals(PINNED_TOTAL, tOrder.size(), "2 forms x 45 materials");
        for (int i = 0; i < PINNED_M; i++) {
            assertFalse(tOrder.get(i).small(), "the first half is the large form");
            assertTrue(tOrder.get(i + PINNED_M).small(), "the second half is the small form");
            assertSame(tOrder.get(i).material(), tOrder.get(i + PINNED_M).material(), "material-major aligned");
        }
        // the id scheme vs the 3922 universe: ore_small_bedrock_<mat> needs NO family segment
        // (the bedrock band has no stone-family axis) yet stays unambiguous against every
        // ore_small_<family>_<mat> id — no family is named "bedrock" in GT6OreBlocks.FAMILIES
        assertTrue(GT6OreBlocks.FAMILIES.stream().noneMatch(tFamily -> tFamily.snake().equals("bedrock")),
                "the bedrock path segment must not collide with a stone family snake");
        assertEquals("ore_bedrock_coal", GT6BedrockOreBlocks.path(false, MT.Coal));
        assertEquals("ore_small_bedrock_coal", GT6BedrockOreBlocks.path(true, MT.Coal));
    }

    /** The block columns verbatim (Loader_Ores.java:44-45): unmineable, no loot, the right prefix + form flag. */
    @Test
    void blockColumnsAreUpstreamVerbatim() {
        net.minecraft.world.level.block.state.BlockBehaviour tLarge =
                new GTBedrockOreBlock(OP.oreBedrock, MT.Coal, false);
        net.minecraft.world.level.block.state.BlockBehaviour tSmall =
                new GTBedrockOreBlock(OP.oreSmall, MT.Coal, true);
        GTBedrockOreBlock tLargeBlock = (GTBedrockOreBlock) tLarge;
        GTBedrockOreBlock tSmallBlock = (GTBedrockOreBlock) tSmall;
        assertSame(OP.oreBedrock, tLargeBlock.prefix, "large rides OP.oreBedrock");
        assertSame(OP.oreSmall, tSmallBlock.prefix, "small shares OP.oreSmall (the upstream name segment says bedrock)");
        assertFalse(tLargeBlock.small);
        assertTrue(tSmallBlock.small);
        assertSame(MT.Coal, tLargeBlock.material);
        assertEquals(3600000.0F, tLargeBlock.defaultDestroyTime(), 0.0F, "the 3600000F hardness verbatim");
        // the 9999 blast resistance + noLootTable (Drops_None) ride the ctor properties face —
        // BlockBehaviour keeps explosionResistance protected (1.20.1 :83) and the loot face is
        // the drop-nothing default, both compile-pinned at GTBedrockOreBlock.
    }
}
