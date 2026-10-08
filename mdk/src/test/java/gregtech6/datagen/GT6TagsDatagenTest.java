/**
 * Offline JSON-snapshot tests for task tags-provider-skeleton, extended band by band:
 * the tag batches are pinned against the committed generated tree so a future provider
 * refactor cannot drift the product silently. The block face — mineable/pickaxe over stone272
 * + the whole {@link GTMachines} register + the metal/gem/raw-ore prefix blocks, mineable/axe
 * over exactly the wood barrel, and the tags-prefix-materials rolling batch 1 (the shovel
 * band exactly the blockDust family; the wire universe walk-computed from GTWireSpecs; the
 * barrel closure + the fluid pipes into pickaxe); the item face — the GTCEu
 * {@code defaultTagPath} families (TagPrefix.java:279/:290/:405/:416/:729/:223 plus the
 * rolling batch 2 :456/:502/:267) sampled per material with EXACT expected member lists
 * recomputed from the same registration walks the provider uses (the
 * GT6StoneBlocksRenderDatagenTest mirror discipline: enumeration side walks the offline
 * order, generated side is asserted from the classpath tree; the write side is gated by
 * runData: first run written>0, second written:0), plus the ecosystem-alias twins of the
 * decisions.p24-material-name-normalization ruling (aluminium→aluminum,
 * aluminium_brass→aluminum_brass, direct-member mirrors).
 *
 * <p>Strictness pin (task tag-residual-convergence evolution): the old "zero optional
 * members anywhere" reader assertion is superseded by the bidirectional member law —
 * optional ⇔ the id is seed-hideable (the atlas-gated set), required ⇔ registers on every
 * install — pinned over every shipped tag JSON in GT6ForeignRowConvergenceTest. Ungated
 * members keep the strict string form, so the TagsProvider.java:85-94 throw contract
 * ("Couldn't define tag %s as it is missing following references") still fails runData
 * loudly on a genuine walk gap.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWireSpecs;

class GT6TagsDatagenTest {

    /**
     * The pickaxe-file member total: 272 stone pairs + 49 machine blocks (the oven Heat_T
     * ladder 4, task oven-heat-t-ladder + shredder/crusher/lathe ladders + 4 dryers +
     * 4 distilleries + the 4 Canner rows +
     * the W1 sifter/compressor/wiremill ladders + the 4 Press + 4 Extruder rows, task
     * w1-press-extruder-molds + the Advanced Crafting Table —
     * GTMachines.java:58/:118-151/:233-237/:347-351/:415-435 + the p26-w1 trio,
     * the whole-class {@code GTMachines.BLOCKS} walk means every landed machine row joins
     * the band) + the 1 conscious multiblock join (the Lightning Rod pillar block, task
     * lightning-rod — the wall/coil/controller follow the multiblock-family
     * non-membership convention, the provider javadoc) +
     * the 6 metal/gem/raw prefixes' live pairs (3777 - 1096 blockDust = 2681) + the
     * rolling batch 1 faces (task tags-prefix-materials): 629 wires (the legacy
     * 1x/2x pair + 620 electric + 6 redstone + 1 laser — the whole-class
     * {@code GTWires.BLOCKS} walk) + 2 fluid pipes (the task card names GTFluidPipeBlock
     * into pickaxe) + 22 barrels (plastic canister + bronze drum + logistics tank + 12 high-tier + 7 64K-tier
     * high-tier drums; the wood barrel stays axe-only). Pinned so any band change is a
     * conscious constant update — the maintenance duty the tags card declared for every
     * future machine card (the four Canner rows 2970 → 2974, the rod block 2974 → 2975,
     * the tags batch 2975 → 3621, the single-variant ACT row 3621 → 3622, task
     * act-machine — the S4 merge-order reconciliation, then the W1 trio 3622 → 3634,
     * task w1-sifter-compressor-wiremill — then the press/extruder rows 3634 → 3642,
     * task w1-press-extruder-molds — then the oven ladder rows 3642 → 3645, task
     * oven-heat-t-ladder).
     */
    // the 3 oven ladder rows joined the whole-class walk at task oven-heat-t-ladder
    // (46 -> 49 machines on the merged line, 3642 -> 3645); the six p28 ULV rows join the
    // same whole-class walk (49 -> 55, 3645 -> 3651, task c-ulv-machine-ladder); the
    // stone anvil pair joined the explicit tail (task c-anvil, 3651 -> 3653); the
    // sixteen roll-ladder machines joined the whole-class walk (task
    // w1-kinetic-roll-ladder, 55 -> 71, 3653 -> 3669); the six p29 process families
    // joined too (71 -> 95, 3669 -> 3693, task w1-kinetic-process-ladder)
    // the sixteen hu-tu rows joined at task w2-hu-tu-piggyback
    // the +9 burner/plantalyzer machine carriers join at task machines-burner-plantalyzer (the machines bucket 247 -> 256)
    // the +4 plate-gem block carriers join at task machines-bumblelyzer-crucible (the bouleGt force-table cascade: blockPlateGem over Si/Ge/RedstoneAlloy/NikolineAlloy) and the +9 p34 machine rows ride the whole-class machine band (bumblelyzer x5 + crystallisationcrucible x4)
    private static final int PINNED_PICKAXE_TOTAL = 272 + 256 + 1 + 2681 + 629 + 2 + 15 + 7 + 2 + 278 + 10 + 119 + 64 - 273 + 16 + 60 + 60 + 1 + 37 - 1 + 399 + 35 + 48 + 31;// +31 (review-seat rebase union, task material-mc-g2-decor-misc): the decor pickaxe rows join the band — 16 asphalt + 5 metal bars + 10 spikes (the BlockAsphalt BlockMetaType.java:166 whole-class TOOL_pickaxe / the BlockBaseBars.java:136 wood-metal split metal half / the BlockBaseSpike.java:107 pair; the card base carried 4122 -> 4153); 4718 -> 4749 // +48 (review-seat rebase union, task material-mc-g1-panels-dyed): the 48 dyed Cover Panel rows join the band — the Loader rows carry harvest class aStone (Loader_MultiTileEntities.java:2045/:2049/:2053), the concrete-family convention; 4670 -> 4718 // +7 (review-seat rebase union, task material-mc-f-attachment-rows): the 7-row 64K drum tier joins the aUtilMetal band, Loader :2152-2158 — on the rebased chain the bump rides 4653 -> 4660 (base measured 4183 -> 4190) // +10 the metal nozzle/cap-nozzle rows ride the ROWS pickaxe walk (:2125-2134; the 2 plastic twins ride axe) 4660 -> 4670  // +1 the Universal Plant Pot (task block-family-32xxx-port, the :2229 aUtilStone column — the pickaxe-band join) // the +14 eu-special (task w2-eu-special), +30 exotic (task w2-exotic-energy), +25 eu-core (task w2-eu-core-5tier), +16 hu-tu (task w2-hu-tu-piggyback), +5 heat-smelter (task w3-heat-smelter) and +19 eu-bridge (task w4-eu-bridge: the 15 converter rows + the 4 Roasting rows) and +5 small Massfab (task massfab) machine blocks auto-ride the whole-class band (120 + 14 + 30 + 25 + 16 + 5 + 19 + 5) and +4 QU machines (task qu-scanner-replicator: the molecular_scanner_t3 single + the replicator three-rung) +278 the fluid-pipe-matrix tree landed on main (c97bdde79) without this pin moving — the main checkout was RED on this pin (the committed 4136 vs the 3858 pin; 280 pipe rows minus the 2 W1 wood rows already counted = +278 net) — this bump carries the reconciliation — and +119 act-matrix rows (the 120-block crafting table matrix minus the one counted single-variant row, 4136 -> 4255) and +64 concrete blocks (task concrete-blocks-register: the 2 families x 16 colours x full+slab — upstream TOOL_pickaxe over the whole BlockMetaType family incl. slabs, BlockMetaType.java:166; 4255 -> 4319) and −273 +16 (task harvest-bands-wrench-machines, the mislabel fix: the 245 metal fluid pipes join gt6:mineable/wrench and the 28 wood ones join mineable/axe — upstream splits the family by material, wood aWooden axe Loader :1846-1849 / rubber aUtilWool shears :1850 / metals aMachine wrench :1851-1860; only the 7 rubber rows keep the pickaxe seat, the shears defer 4319 -> 4046 — while the 16 long-distance WIRES join the cutter expedient (upstream BlockLongDistWire.java:56-57 TOOL_cutter lvl3, the port cutter has no mining face) 4046 -> 4062) and −273 (task harvest-bands-wrench-machines, the mislabel fix: the 245 metal fluid pipes join gt6:mineable/wrench and the 28 wood ones join mineable/axe — upstream splits the family by material, wood aWooden axe Loader :1846-1849 / rubber aUtilWool shears :1850 / metals aMachine wrench :1851-1860; only the 7 rubber rows keep the pickaxe seat, the shears defer 4319 -> 4046) and +60 (task material-mc-a-storage-chests, the review-seat rebase seam: the 60 PLAIN metal chests join the pickaxe band — upstream block family aMetal = TOOL_pickaxe, Loader_MultiTileEntities.java:132 vs the :98 aMetal tool column, the brief's 金属=wrench corrected by the loader line; 4062 -> 4122) and +60 (task material-mc-b-storage-mass-shelf, the review-seat band-split seam: the 60 METAL BOTTLECRATE rows join the pickaxe band — the upstream row rides aUtilMetal = TOOL_pickaxe, Loader :144 vs the :107 aUtilMetal carrier column, NOT the aMachine wrench face its metal bookshelf sibling :143 rides; the merged walk had put all 128 metal storage rows on wrench behind a stale committed tag product; 4122 -> 4182) and +37 (task faucet-material-rows: the 37 metal faucet rungs Loader :361-388 landed after this card base ride the same FAUCET_BLOCKS_BY_PATH walk, the rebase union seam; 4581 -> 4618) and −1 +399 (task harvest-bands-card2-nonwrench: the non-wrench pickaxe domains land — 32 molds + 2 faucets (aUtilStone :347-359/:300/:305), 4 crucibles (aStone :251/:256 + aMetal :265/:267, the card-1 census erratum: upstream has NO wrench crucible), 21 sensors (aUtilMetal :1979-1999), grindstone+sifting table (:2226-2227), 5 mortars (aUtilStone :2179-2183), 3 kitchen rows (steel pot :2175/mixing bowl :2177/juicer :2184), coke oven+bricks (aStone :1138/:1193), 31 metal cells (aUtilMetal :1779-1809), cup+jug+measuring pot (aUtilStone :2094-2096), 4 gas cylinders (aUtilMetal :2101-2104), 10 taps/funnels (ceramic+4-metal, :2108-2120), 5 placed piles (:2034-2040), 2 mini portals (aStone :2003-2004), hand crank (:2106 aUtilMetal) and the 272 stone slabs (BlockMetaType.java:166); while the plastic canister LEAVES the band — upstream :2150 rides aUtilWood = axe, the old seat was the GTCEu BlockTagLoader precedent at this card own base 4062 -> 4460; the rebase recarries the delta onto 4182) and +35 (task material-mc-c-crucible-rows, the review-seat seam riding storage-tool-char-decode: the 35 material smeltery rungs ride the aStone/aMetal pickaxe walk, the Loader :250-292 FULL ladder — the mc-C landing grew GT6Crucibles 4 -> 39 without this pin moving, the main checkout RED on both this pin and the :483 rung count; 4618 -> 4653)

    /** The 16 tier-ladder machine ids of the first machines card + the oven ladder + the ACT single-variant row (the dryer/distillery/canner rows ride the total pin). */
    private static final List<String> PINNED_LADDER_MACHINES = List.of(
            "gt6:oven", "gt6:oven_t2", "gt6:oven_t3", "gt6:oven_t4", // task oven-heat-t-ladder
            "gt6:shredder", "gt6:shredder_t2", "gt6:shredder_t3", "gt6:shredder_t4",
            "gt6:crusher", "gt6:crusher_t2", "gt6:crusher_t3", "gt6:crusher_t4",
            "gt6:lathe", "gt6:lathe_t2", "gt6:lathe_t3", "gt6:lathe_t4",
            "gt6:advanced_crafting_table_steel"); // act-matrix: the steel row stands in for the retired bare path (the ladder pin stays one representative)

    /**
     * The pickaxe-band prefixes — the GT6BlockTags band set mirrored (blockDust excluded).
     * Filled in {@link #initMaterialSystem}: the OP prefix fields are registered by OP.init
     * (a static-field initializer here would read nulls on a solo class run — the whole-suite
     * order only masked it with a preceding test's init).
     */
    private static Set<OreDictPrefix> gPickaxeBlockPrefixes;

    /** The item-path family walks, computed once (mirror of the provider's two walks). */
    private static List<GTMaterialItems.PrefixMaterial> gItemWalk;
    private static List<GTMaterialItems.PrefixMaterial> gStorageWalk;

    @BeforeAll
    static void initMaterialSystem() {
        // the GT6PrefixBlockRenderDatagenTest recipe: bootStrap keeps the headless JVM safe
        // through the provider chain class-load, then the material system fills the walks
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        GTMaterialItems.initMaterials();
        gItemWalk = GTMaterialItems.registrationOrder();
        gStorageWalk = GTMaterialBlocks.registrationOrder();
        gPickaxeBlockPrefixes = Set.of(OP.blockRaw, OP.blockGem,
                OP.blockIngot, OP.blockPlate, OP.blockPlateGem, OP.blockSolid);
    }

    // ------------------------------------------------------------------ the block face

    /** One generated tag JSON's values array (classpath face of the committed tree): both member forms as id strings — the strict string form and the optional object form ({@code {"id":..,"required":false}}, the tag-residual-convergence gated-member face). */
    private static List<String> tagValues(String aDataPath) throws Exception {
        try (InputStream tStream = GT6TagsDatagenTest.class.getClassLoader().getResourceAsStream("data/" + aDataPath)) {
            assertNotNull(tStream, "the generated tag must be on the classpath: " + aDataPath);
            var tArray = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("values");
            List<String> rValues = new ArrayList<>();
            for (var tEntry : tArray) {
                rValues.add(tEntry.isJsonPrimitive() ? tEntry.getAsString()
                : tEntry.getAsJsonObject().get("id").getAsString());
            }
            return rValues;
        }
    }

    /** The pickaxe band: stone272 full, the ladder machines, the 6-prefix walk — exact total, all gt6. */
    @Test
    void pickaxeBandCoversTheMiningUniverse() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tMembers = Set.copyOf(tValues);
        // stone272, all pairs
        int tStones = 0;
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            assertTrue(tMembers.contains("gt6:" + GTStoneBlocks.path(tKey.stone().snake(), tKey.variant())),
                    "stone pair must ride the pickaxe band: " + tKey);
            tStones++;
        }
        assertEquals(272, tStones, "the 17x16 stone census");
        // the machine ladder (the whole-class register rides the total pin below)
        for (String tMachine : PINNED_LADDER_MACHINES) {
            assertTrue(tMembers.contains(tMachine), "machine must ride the pickaxe band: " + tMachine);
        }
        // the metal/gem/raw prefix walk, member-exact
        int tPrefixPairs = 0;
        for (GTMaterialItems.PrefixMaterial tPair : gStorageWalk) {
            if (!gPickaxeBlockPrefixes.contains(tPair.prefix())) continue;
            assertTrue(tMembers.contains("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())),
                    "prefix block must ride the pickaxe band: " + tPair);
            tPrefixPairs++;
        }
        assertEquals(2681, tPrefixPairs, "3777 storage pairs (+4 task machines-bumblelyzer-crucible: the blockPlateGem cascade carriers over the quartet) - 1096 blockDust pairs");
        // exact product shape: no member outside the census families
        assertEquals(PINNED_PICKAXE_TOTAL, tValues.size(),
                "272 stones + 256 machines + 1 rod + 2681 prefix blocks + 629 wires + 2 pipes + 22 barrels + 2 anvils + 60 plain chests"
                + " (the +14 eu-special machines of task w2-eu-special, the +30 exotic machines of task w2-exotic-energy, the +25 eu-core machines of task w2-eu-core-5tier the +16 hu-tu machines of task w2-hu-tu-piggyback and the +5 heat-smelter machines of task w3-heat-smelter and the +5 small Massfab machines of task massfab joined the machine walk; the +60 chests of task material-mc-a-storage-chests ride the aMetal pickaxe family, Loader :132)"
                + " + 399 non-wrench extension rows (task harvest-bands-card2-nonwrench, the pin comment carries the family table) - the 1 plastic canister that moved to the axe band + the 37 post-base faucet rungs (task faucet-material-rows, the same FAUCET_BLOCKS_BY_PATH walk) + the 35 material smeltery rungs (task material-mc-c-crucible-rows, the same GT6Crucibles walk, the Loader :250-292 full ladder) + the 7 64K-tier drum rows (review-seat rebase union, task material-mc-f-attachment-rows: Invar/StainlessSteel/Desh/Syrmorite/Efrine/Thaumium/Manasteel, Loader :2152-2158) + the 48 dyed Cover Panel rows (task material-mc-g1-panels-dyed, the aStone Loader rows :2045/:2049/:2053, the review-seat rebase union)");
        assertTrue(tValues.stream().allMatch(v -> v.startsWith("gt6:")), "mod-face-only members");
    }

    /**
     * The strictness face of the acceptance: no member is optional in any mining tag
     * (rolling batch 1 adds the shovel file to the pin).
     */
    @Test
    void miningTagsParseBothMemberForms() throws Exception {
        tagValues("minecraft/tags/blocks/mineable/pickaxe.json"); // the reader accepts both forms
        tagValues("minecraft/tags/blocks/mineable/axe.json");
        tagValues("minecraft/tags/blocks/mineable/shovel.json");
    }

    /**
     * The exclusions: blockDust belongs to the shovel band (rolling batch 1), the wood
     * barrel AND (since task harvest-bands-card2-nonwrench) the plastic canister ride
     * axe — the upstream :2150 aUtilWood column — while the metal barrels join pickaxe.
     */
    @Test
    void pickaxeBandExcludesTheDustPrefixAndTheWoodBarrel() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        assertTrue(tValues.stream().noneMatch(v -> v.startsWith("gt6:block_dust_")),
                "blockDust is the shovel band, not the pickaxe band");
        assertTrue(!tValues.contains("gt6:barrel_wood"), "the wood barrel belongs to the axe band");
        assertTrue(!tValues.contains("gt6:barrel_plastic"), "the plastic canister belongs to the axe band (:2150 aUtilWood)");
    }

    /**
     * The axe band: the wood fluid barrel + the WOOD fluid-pipe subdomain (task
     * harvest-bands-wrench-machines, the mislabel fix — the four wooden materials
     * wood/wood_treated/iron_wood/plastic ride the upstream aWooden column, Loader
     * :1846-1849; walk-computed from the same family filter the provider uses) + the 47
     * aWooden/aUtilWood rows of task harvest-bands-card2-nonwrench (the wood wall :1139,
     * the wood tank valve :1195, the 10+10 bookshelf/bottlecrate plank ladders :181-184,
     * the 9 capsule-cell wood hosts :1770-1778, the plastic tap/funnel pair :2109/:2116,
     * lantern+stick :2031/:2035-2036, the wooden bathing pot :2173-2174, the bumbliary
     * pair :2222-2223, the 4 wood_treated axles :1663-1666 + gearbox/rotation
     * transformer :1668-1669 + the 2 ironwood steam engines :591/:606, and the plastic
     * canister :2150 aUtilWood — the mislabel fix out of pickaxe; walk order preserved)
     * + the 9 GT6 tree logs (task w6-t1-trees-nine — the addTreeBand walk order appended
     * after the barrel; the census material mapping keeps wood out of pickaxe) + the 4
     * fallen-log woods (task w6-t2-surface-blocks, the addSurfacePlantBand tail-append)
     * + the 8 wood beams (task beam-blocks-register, the addBeamBand tail-append — the
     * upstream harvest-tool override BlockBaseBeam.java:54 {@code getHarvestTool =
     * TOOL_axe}, kind order) + the 17 plank cubes (task gt-tree-planks + planks-blockification,
     * the addPlankBand tail-append — the vanilla planks axe face; the 8 generic rows ride the
     * species tail in upstream meta order) + the 60 reinforced chests (task
     * material-mc-a-storage-chests — upstream block family aWooden = TOOL_axe,
     * Loader_MultiTileEntities.java:133 vs the :102 aWooden tool column, the
     * addAxeBand chest walk appended after the pipes walk).
     */
    @Test
    void axeBandIsExactlyTheWoodBarrelAndTheTreeLogs() throws Exception {
        List<String> tBeams = List.of(
                "gt6:oak_beam", "gt6:spruce_beam", "gt6:birch_beam", "gt6:jungle_beam",
                "gt6:acacia_beam", "gt6:dark_oak_beam", "gt6:rubber_wood_beam", "gt6:wood_beam",
                // task beam-fireproof-closeout — the residual families (Beam3/A/B/C), kind order
                "gt6:greatwood_beam", "gt6:silverwood_beam", "gt6:skyroot_beam", "gt6:darkwood_beam",
                "gt6:rubber_beam", "gt6:maple_beam", "gt6:willow_beam", "gt6:blue_mahoe_beam",
                "gt6:hazel_beam", "gt6:cinnamon_beam", "gt6:coconut_beam", "gt6:rainbowood_beam",
                "gt6:blue_spruce_beam");
        List<String> tFireproof = tBeams.stream().map(id -> id + "_fireproof").toList();
        List<String> tBand = new ArrayList<>(List.of("gt6:barrel_wood"));
        int tWoodPipes = 0;
        for (var tEntry : GTFluidPipes.BLOCKS_BY_PATH.entrySet()) {
            if (GTFluidPipes.rowByPath(tEntry.getKey()).material().blockFamily() == GTFluidPipes.PipeBlockFamily.WOODEN) {
                tBand.add("gt6:" + tEntry.getKey());
                tWoodPipes++;
            }
        }
        assertEquals(28, tWoodPipes, "the 4 wooden materials x 7 variants (Loader :1846-1849 aWooden)");
        // task material-mc-a-storage-chests — the 60 REINFORCED CHEST rows walk-computed
        // like the pipes (upstream aWooden = TOOL_axe, Loader_MultiTileEntities.java:133;
        // the addAxeBand chest walk order = ROWS order = the GT6Hoppers.MATERIALS order;
        // the ROW walk, not BLOCKS_BY_PATH .get() — the offline JVM has no registry)
        int tReinforcedChests = 0;
        for (gregtech6.registry.GT6Chests.ChestRow tRow : gregtech6.registry.GT6Chests.ROWS) {
            if (tRow.reinforced()) {
                tBand.add("gt6:" + tRow.path());
                tReinforcedChests++;
            }
        }
        assertEquals(60, tReinforcedChests, "the 60 reinforced chests (Loader :133 aWooden)");
        // task harvest-bands-card2-nonwrench — the 47 aWooden/aUtilWood rows, walk order
        tBand.add("gt6:wood_wall"); // :1139 aWooden — the row the wrench walk skips
        tBand.add("gt6:tank_wood"); // :1195 aWooden, the only flammable() tanks row
        // the :181-184 plank ladders — WOOD rows only (material == null): the METAL rows
        // of the same kinds are mc-B's seats (bottlecrate_metal pickaxe / bookshelf_metal
        // wrench) and would double-band here — the rebase union refinement
        for (var tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
            if (tRow.material() == null && (tRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOOKSHELF
                    || tRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE)) {
                tBand.add("gt6:" + tRow.path()); // the :181-184 plank ladders (the PLANKS walk order)
            }
        }
        for (var tRow : gregtech6.registry.GT6Cells.ROWS) {
            if (tRow.woodHost() && tRow.registers()) tBand.add("gt6:" + tRow.path()); // the :1770-1778 column
        }
        tBand.add("gt6:tap_plastic"); // :2109 aUtilWood
        tBand.add("gt6:funnel_plastic"); // :2116 aUtilWood
        tBand.add("gt6:nozzle_plastic"); // :2125 aUtilWood — the nozzle pair plastic twin (mc-F rebase union)
        tBand.add("gt6:cap_nozzle_plastic"); // :2132 aUtilWood
        tBand.add("gt6:greg_o_lantern"); // :2031
        tBand.add("gt6:placed_stick"); // :2035-2036
        tBand.add("gt6:bathing_pot_wood"); // :2173-2174
        tBand.add("gt6:bumbliary"); // :2222 aWooden
        tBand.add("gt6:bumbliary_advanced"); // :2223 aWooden
        tBand.add("gt6:axle_wood_treated_small"); // :1663-1666, the 4 wood_treated axles
        tBand.add("gt6:axle_wood_treated_medium");
        tBand.add("gt6:axle_wood_treated_large");
        tBand.add("gt6:axle_wood_treated_huge");
        tBand.add("gt6:gearbox"); // :1669 aWooden
        tBand.add("gt6:transformer_rotation"); // :1668 aWooden
        tBand.add("gt6:steam_engine_iron_wood"); // :591 aWooden
        tBand.add("gt6:strong_steam_engine_iron_wood"); // :606 aWooden
        tBand.add("gt6:barrel_plastic"); // :2150 aUtilWood — the mislabel fix out of pickaxe
        tBand.addAll(List.of(
                "gt6:rubber_log", "gt6:maple_log", "gt6:willow_log", "gt6:blue_mahoe_log",
                "gt6:hazel_log", "gt6:cinnamon_log", "gt6:coconut_log", "gt6:rainbowood_log",
                "gt6:blue_spruce_log",
                "gt6:dead_log", "gt6:rotten_log", "gt6:mossy_log", "gt6:frozen_log"));
        tBand.addAll(tBeams);
        tBand.addAll(tFireproof);
        tBand.addAll(List.of(
                "gt6:rubber_planks", "gt6:maple_planks", "gt6:willow_planks", "gt6:blue_mahoe_planks",
                "gt6:hazel_planks", "gt6:cinnamon_planks", "gt6:coconut_planks", "gt6:rainbowood_planks",
                "gt6:blue_spruce_planks",
                // task planks-blockification — the 8 generic rows (the addPlankBand second loop,
                // BlockTreePlanks metas 8-15 in meta order, tail-appended after the species walk)
                "gt6:plank_wood_compressed", "gt6:plank_wood", "gt6:plank_wood_treated", "gt6:crate",
                "gt6:plank_wood_dead", "gt6:plank_wood_rotten", "gt6:plank_wood_mossy", "gt6:plank_wood_frozen"));
        // task material-mc-g3-plank-panels (review seam) — the 28 wooden Cover Panels ride
        // the band (the addWoodenPanelBand walk in ROW_TABLE order, tail-appended after
        // the plank cubes; aWooden = Loader :2058/:2067/:2076)
        for (var tRow : gregtech6.registry.GT6PlankPanels.ROW_TABLE) {
            tBand.add("gt6:" + gregtech6.registry.GT6PlankPanels.path(tRow.slug()));
        }
        // task material-mc-g2-decor-misc — the wood bars row joins the axe band
        // (BlockBaseBars.java:136 the Material.wood split), the addDecorBand tail position
        tBand.add("gt6:bars_wood");
        assertEquals(tBand, tagValues("minecraft/tags/blocks/mineable/axe.json"));
    }

    /**
     * The tree-family vanilla bands (task w6-t1-trees-nine): the datagen file carries
     * ONLY the gt6 additions — the vanilla members (oak_log etc.) live in the vanilla
     * jar's own data layer and merge at load, so the pin is exactly the 9 gt6 members per
     * family, both the block and the item face. The item logs band is THE coke-oven
     * expansion source (GT6CokeOvenTagListener.rebuild reads ItemTags.LOGS on every real
     * tag load) — a missed membership = a silently dead recipe, hence the exact pin.
     */
    @Test
    void treeFamiliesAreExactlyTheNineGt6Members() throws Exception {
        List<String> tLogs = List.of(
                "gt6:rubber_log", "gt6:maple_log", "gt6:willow_log", "gt6:blue_mahoe_log",
                "gt6:hazel_log", "gt6:cinnamon_log", "gt6:coconut_log", "gt6:rainbowood_log",
                "gt6:blue_spruce_log",
                // task w6-t2-surface-blocks — the 4 fallen-log woods join the band
                "gt6:dead_log", "gt6:rotten_log", "gt6:mossy_log", "gt6:frozen_log");
        assertEquals(tLogs, tagValues("minecraft/tags/items/logs.json"));
        assertEquals(tLogs, tagValues("minecraft/tags/blocks/logs.json"));
        List<String> tLeaves = List.of(
                "gt6:rubber_leaves", "gt6:maple_leaves", "gt6:willow_leaves", "gt6:blue_mahoe_leaves",
                "gt6:hazel_leaves", "gt6:cinnamon_leaves", "gt6:coconut_leaves", "gt6:rainbowood_leaves",
                "gt6:blue_spruce_leaves");
        assertEquals(tLeaves, tagValues("minecraft/tags/items/leaves.json"));
        assertEquals(tLeaves, tagValues("minecraft/tags/blocks/leaves.json"));
        List<String> tSaplings = List.of(
                "gt6:rubber_sapling", "gt6:maple_sapling", "gt6:willow_sapling", "gt6:blue_mahoe_sapling",
                "gt6:hazel_sapling", "gt6:cinnamon_sapling", "gt6:coconut_sapling", "gt6:rainbowood_sapling",
                "gt6:blue_spruce_sapling");
        assertEquals(tSaplings, tagValues("minecraft/tags/items/saplings.json"));
        assertEquals(tSaplings, tagValues("minecraft/tags/blocks/saplings.json"));
    }

    /**
     * The wood-beam oredient band (task beam-oredict-seam + beam-fireproof-closeout): the
     * beam items are exactly the OD.beamWood members — upstream registers EVERY beam meta
     * of EVERY beam block (flammable AND fireproof — BlockTreeBeam1FireProof extends
     * BlockBaseBeam) to the single oredict name "beamWood" (BlockBaseBeam.java:48
     * {@code for (i < 16) OM.reg(ST.make(this, 1, i), OD.beamWood)}; OD.java:150 the key;
     * OP.java:393 the prefix comment "Usually as \"beamWood\""), so the port's per-wood
     * items + their fireproof twins are that meta universe: 21 kinds + 21 twins, kind
     * order. Both tree faces pinned (the plural 1.20.1 form + the singular 21.1 form —
     * the vanilla-tag-dual-tree discipline).
     */
    @Test
    void beamWoodBandIsExactlyTheBeamRegistrations() throws Exception {
        List<String> tBeams = new ArrayList<>(List.of(
                "gt6:oak_beam", "gt6:spruce_beam", "gt6:birch_beam", "gt6:jungle_beam",
                "gt6:acacia_beam", "gt6:dark_oak_beam", "gt6:rubber_wood_beam", "gt6:wood_beam",
                "gt6:greatwood_beam", "gt6:silverwood_beam", "gt6:skyroot_beam", "gt6:darkwood_beam",
                "gt6:rubber_beam", "gt6:maple_beam", "gt6:willow_beam", "gt6:blue_mahoe_beam",
                "gt6:hazel_beam", "gt6:cinnamon_beam", "gt6:coconut_beam", "gt6:rainbowood_beam",
                "gt6:blue_spruce_beam"));
        tBeams.addAll(tBeams.stream().map(id -> id + "_fireproof").toList());
        assertEquals(tBeams, tagValues("gt6/tags/items/beam_wood.json"));
        assertEquals(tBeams, tagValues("gt6/tags/item/beam_wood.json"));
    }

    /**
     * The shovel band, rolling batch 1: the blockDust prefix family — walk-exact
     * membership (mirror of the provider's filtered walk), the 3777-2681=1096 census count,
     * all gt6. PIN recomputed at merge order (the conscious-update duty, S3 2045c78d):
     * grass-block landed first and its addGrassBand rides the SAME shovel face — the
     * band is now the blockDust family PLUS the 6 GT grass variants (1096+6=1102).
     */
    @Test
    void shovelBandIsExactlyTheDustPrefixFamily() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/shovel.json");
        Set<String> tMembers = Set.copyOf(tValues);
        int tDustPairs = 0;
        for (GTMaterialItems.PrefixMaterial tPair : gStorageWalk) {
            if (tPair.prefix() != OP.blockDust) continue;
            assertTrue(tMembers.contains("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())),
                    "blockDust pair must ride the shovel band: " + tPair);
            tDustPairs++;
        }
        assertEquals(1096, tDustPairs, "3777 storage pairs - 2681 pickaxe-band pairs");
        // +2 (task w6-t2-surface-blocks: turf + black_sand, the addSurfacePlantBand tail)
        // +4 (task worldgen-diggables-pits: the 4 colored-clay blocks — the Diggables
        // IS_CLAY quartet rides the mineable/shovel band, BlockDiggable.java:137; the clay
        // merge ratchets this formula here, the review-seat re-measure)
        // +1 (task material-mc-g2-decor-misc: the Path block — BlockPath.java:229 TOOL_shovel lvl0,
        // the addDecorBand tail)
        assertEquals(tDustPairs + 6 + 2 + 4 + 1, tValues.size(),
                "the shovel band = the blockDust family + the 6 GT grass variants (grass-block merged first) + the 2 soil pair + the 4 colored clays + the path");
        assertTrue(tValues.stream().allMatch(v -> v.startsWith("gt6:")), "mod-face-only members");
    }

    /**
     * The wire universe, rolling batch 1: every GTWires registry path rides pickaxe —
     * walk-computed from the GTWireSpecs tables (the offline face of the provider's
     * whole-class GTWires.BLOCKS enumeration: legacy pair + electric + redstone + laser).
     */
    @Test
    void pickaxeBandCoversTheWireUniverse() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tMembers = Set.copyOf(tValues);
        List<String> tExpected = new ArrayList<>();
        tExpected.add("wire_electric_1x");
        tExpected.add("wire_electric_2x");
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) tExpected.add(GTWireSpecs.registryName(tVariant));
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) tExpected.add(GTWireSpecs.registryName(tVariant));
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) tExpected.add(GTWireSpecs.registryName(tVariant));
        assertEquals(629, tExpected.size(), "2 legacy + 620 electric + 6 redstone + 1 laser");
        for (String tPath : tExpected) {
            assertTrue(tMembers.contains("gt6:" + tPath), "wire must ride the pickaxe band: " + tPath);
        }
    }

    /**
     * The barrel closure + the pipes, rolling batch 1: the metal/logistics barrels
     * and the twelve high-tier drums ride pickaxe — REVISITED by task
     * harvest-bands-card2-nonwrench: the plastic canister LEFT the band (upstream :2150
     * rides aUtilWood = axe, the old seat was the GTCEu BlockTagLoader precedent), so the
     * standalone rows are 2, not 3. The pipe seat is the harvest-bands-wrench-machines
     * MISLABEL FIX: upstream only the rubber subdomain is aUtilWool=shears (:1850 — 1.20.1
     * has no mineable/shears, the card-4 ruling pool keeps it here), the wood rows are
     * aWooden=axe (:1846-1849, moved to the axe band) and the metals aMachine=wrench
     * (:1851-1860, moved to gt6:mineable/wrench — the old whole-register pickaxe
     * expedient is retired).
     */
    @Test
    void pickaxeBandCoversTheBarrelClosureAndThePipes() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tMembers = Set.copyOf(tValues);
        List<String> tBarrels = new ArrayList<>(List.of("barrel_metal", "barrel_logistics"));
        for (GTBarrels.MetalDrumRow tRow : GTBarrels.HIGH_TIER_METAL_DRUMS) tBarrels.add(tRow.path());
        for (GTBarrels.MetalDrumRow tRow : GTBarrels.DRUM_64K_ROWS) tBarrels.add(tRow.path()); // task material-mc-f-attachment-rows
        assertEquals(21, tBarrels.size(), "2 standalone rows + 12 high-tier drums + 7 64K drums (the plastic canister stays axe, :2150 aUtilWood — the review-seat rebase union keeps the nonwrench ruling the card's stale base predated)");
        for (String tPath : tBarrels) {
            assertTrue(tMembers.contains("gt6:" + tPath), "barrel must ride the pickaxe band: " + tPath);
        }
        // the shears defer keeps the rubber subdomain here
        assertTrue(tMembers.contains("gt6:rubber_fluid_pipe_tiny"), "the rubber pipes keep the pickaxe seat (the shears defer)");
        // the mislabel fix: the wood subdomain rides axe, the metal subdomain the wrench face
        assertTrue(!tMembers.contains("gt6:wood_fluid_pipe_small"), "the wood pipes moved to the axe band (upstream aWooden, Loader :1846-1849)");
        assertTrue(!tMembers.contains("gt6:steel_fluid_pipe_small"), "the metal pipes moved to the wrench band (upstream aMachine, Loader :1851-1860)");
        // the long-distance wires: the cutter expedient (upstream aMetalWires cutter,
        // BlockLongDistWire.java:56-57 — the port cutter has no mining face, the declared
        // deviation until the cutter-face card)
        assertTrue(tMembers.contains("gt6:long_dist_wire_0"), "the long-distance wires keep the cutter expedient");
        assertTrue(!tMembers.contains("gt6:long_dist_pipe_0"), "the long-distance pipes are aMachine — the wrench face, not pickaxe");
    }

    /**
     * The non-wrench band extension, family-by-family (task harvest-bands-card2-nonwrench —
     * the census state research.harvest-tool-census landed; every count is a live row-table
     * walk, every javadoc anchor the upstream Loader line). Pickaxe families:
     * molds 32 + faucets 2 (aUtilStone :347-359/:300/:305), crucibles 39 (aStone
     * :251-256 + aMetal :265-267 + the 35 material rungs of task material-mc-c-crucible-rows,
     * the :250-292 full ladder — the card-1 erratum: ZERO wrench crucibles upstream),
     * sensors 21 (aUtilMetal :1979-1999), grindstone + sifting table (:2226-2227), mortars
     * 5 (aUtilStone :2179-2183), kitchen stone/metal trio (:2175/:2177/:2184), coke oven +
     * bricks (aStone :1138/:1193), metal cells 31 (aUtilMetal :1779-1809), cup/jug/
     * measuring pot (:2094-2096 aUtilStone), gas cylinders 4 (:2101-2104 aUtilMetal),
     * taps/funnels 10 (ceramic+metal :2108-2120), placed piles 5 (:2034-2040), mini
     * portals 2 (:2003-2004 aStone), hand crank (:2106 aUtilMetal), stone slabs 272
     * (BlockMetaType.java:166). Axe families are pinned walk-exact in
     * {@link #axeBandIsExactlyTheWoodBarrelAndTheTreeLogs}. STILL OUT (the declared
     * non-faces): the sandwich + resin bag (aUtilWool shears — no 1.20.1 tag), the bumble
     * hive (aHive scoop :2041), the crop sticks (instant-break) and the metal kinetics
     * (aMachine — the wrench-tail card's domain, still zero-banded here).
     */
    @Test
    void nonWrenchBandExtensionFamiliesRideTheirBands() throws Exception {
        Set<String> tPickaxe = Set.copyOf(tagValues("minecraft/tags/blocks/mineable/pickaxe.json"));
        Set<String> tAxe = Set.copyOf(tagValues("minecraft/tags/blocks/mineable/axe.json"));

        int tMolds = 0;
        for (String tPath : gregtech6.registry.GT6Molds.BLOCKS_BY_PATH.keySet()) {
            assertTrue(tPickaxe.contains("gt6:" + tPath), "mold must ride the pickaxe band: " + tPath);
            tMolds++;
        }
        assertEquals(32, tMolds, "the stone + ceramic blank + 30 pre-carved ceramic molds (Loader :347-359 + :391-420)");
        // the rebase union seam: the map grew 2 -> 39 after this card base — the 37 metal
        // faucet rungs (:361-388) landed with task faucet-material-rows ride the same walk
        assertEquals(39, gregtech6.registry.GT6Molds.FAUCET_BLOCKS_BY_PATH.size(), "the stone + ceramic faucets (:300/:305 aUtilStone) + the 37 metal rungs (:361-388, task faucet-material-rows)");
        for (String tPath : gregtech6.registry.GT6Molds.FAUCET_BLOCKS_BY_PATH.keySet()) {
            assertTrue(tPickaxe.contains("gt6:" + tPath), "faucet must ride the pickaxe band: " + tPath);
        }
        assertEquals(39, gregtech6.registry.GT6Crucibles.BLOCKS_BY_PATH.size(), "the 39 smeltery rungs (stone/ceramic aStone :251/:256, bronze/steel aMetal :265/:267 + the 35 material rungs of task material-mc-c-crucible-rows, the Loader :250-292 FULL ladder — ALL pickaxe)");
        for (String tPath : gregtech6.registry.GT6Crucibles.BLOCKS_BY_PATH.keySet()) {
            assertTrue(tPickaxe.contains("gt6:" + tPath) && !tAxe.contains("gt6:" + tPath),
                    "crucible must ride pickaxe and NEVER the axe band: " + tPath);
        }
        assertEquals(21, gregtech6.registry.GT6Sensors.BLOCKS_BY_PATH.size(), "the 21 sensors (:1979-1999 aUtilMetal)");
        for (String tPath : gregtech6.registry.GT6Sensors.BLOCKS_BY_PATH.keySet()) {
            assertTrue(tPickaxe.contains("gt6:" + tPath), "sensor must ride the pickaxe band: " + tPath);
        }
        assertTrue(tPickaxe.contains("gt6:grindstone") && tPickaxe.contains("gt6:sifting_table"),
                "the grindstone (:2226) + sifting table (:2227) singles ride pickaxe");
        assertEquals(5, gregtech6.registry.GT6Mortars.BLOCKS_BY_PATH.size(), "the 5 mortars (:2179-2183 aUtilStone)");
        for (String tPath : gregtech6.registry.GT6Mortars.BLOCKS_BY_PATH.keySet()) {
            assertTrue(tPickaxe.contains("gt6:" + tPath), "mortar must ride the pickaxe band: " + tPath);
        }
        assertTrue(tPickaxe.contains("gt6:bathing_pot_steel") && tPickaxe.contains("gt6:mixing_bowl") && tPickaxe.contains("gt6:juicer"),
                "the kitchen stone/metal trio (:2175/:2177/:2184) rides pickaxe");
        assertTrue(tPickaxe.contains("gt6:multiblock_coke_oven") && tPickaxe.contains("gt6:multiblock_coke_oven_bricks"),
                "the coke oven + bricks (:1138/:1193 aStone) ride pickaxe");
        int tMetalCells = 0, tWoodCells = 0;
        for (var tRow : gregtech6.registry.GT6Cells.ROWS) {
            if (!tRow.registers()) continue; // the mdh-6 driver gate mirrors the registration walk
            if (tRow.woodHost()) {
                assertTrue(tAxe.contains("gt6:" + tRow.path()), "wood-host cell must ride the axe band: " + tRow.path());
                tWoodCells++;
            } else {
                assertTrue(tPickaxe.contains("gt6:" + tRow.path()), "metal cell must ride the pickaxe band: " + tRow.path());
                tMetalCells++;
            }
        }
        assertEquals(31, tMetalCells, "the metal capsule-cell containers (:1779-1809 aUtilMetal)");
        assertEquals(9, tWoodCells, "the 8 waxes + plastic wood hosts (:1770-1778 aUtilWood)");
        assertTrue(tPickaxe.contains("gt6:porcelain_cup") && tPickaxe.contains("gt6:ceramic_jug") && tPickaxe.contains("gt6:measuring_pot"),
                "the cup (:2094) + jug (:2095) + measuring pot (:2096) ride pickaxe (aUtilStone)");
        assertEquals(4, gregtech6.registry.GT6GasCylinders.BLOCKS_IN_ORDER.size(), "the 4 gas cylinders (:2101-2104 aUtilMetal)");
        for (var tHandle : gregtech6.registry.GT6GasCylinders.BLOCKS_IN_ORDER) {
            assertTrue(tPickaxe.contains("gt6:" + tHandle.getId().getPath()), "gas cylinder must ride the pickaxe band: " + tHandle.getId());
        }
        int tUtilAttachments = 0, tWoodAttachments = 0;
        for (var tRow : gregtech6.registry.GT6Attachments.ROWS) {
            if (tRow.path().endsWith("_plastic")) {
                assertTrue(tAxe.contains("gt6:" + tRow.path()), "plastic attachment must ride the axe band (:2109/:2116 aUtilWood): " + tRow.path());
                tWoodAttachments++;
            } else {
                assertTrue(tPickaxe.contains("gt6:" + tRow.path()), "attachment must ride the pickaxe band: " + tRow.path());
                tUtilAttachments++;
            }
        }
        assertEquals(20, tUtilAttachments, "the ceramic + 4-metal tap/funnel rows (:2108-2113/:2115-2120) + the 10 metal nozzle/cap-nozzle rows (:2125-2134, review-seat rebase union task material-mc-f-attachment-rows)");
        assertEquals(4, tWoodAttachments, "the plastic tap/funnel pair (:2109/:2116 aUtilWood) + the plastic nozzle/cap-nozzle twin (:2125/:2132, the review-seat rebase union)");
        assertTrue(tPickaxe.contains("gt6:placed_rock") && tPickaxe.contains("gt6:placed_gem_plate")
                && tPickaxe.contains("gt6:placed_ingot") && tPickaxe.contains("gt6:placed_plate") && tPickaxe.contains("gt6:placed_scrap"),
                "the 5 placed piles ride pickaxe (:2034/:2039 aUtilStone, :2037/:2038/:2040 aUtilMetal)");
        assertTrue(tPickaxe.contains("gt6:mini_portal_nether") && tPickaxe.contains("gt6:mini_portal_end"),
                "the mini portals (:2003/:2004 aStone) ride pickaxe");
        assertTrue(tPickaxe.contains("gt6:crank"), "the hand crank (:2106 aUtilMetal) rides pickaxe");
        int tSlabMembers = 0;
        for (String tValue : tPickaxe) if (tValue.endsWith("_slab")) tSlabMembers++;
        assertEquals(304, tSlabMembers, "272 stone slabs (BlockMetaType.java:166 TOOL_pickaxe over the whole family) + 32 concrete slabs");
        // the declared NON-faces: shears/scoop/instant domains and the aMachine kinetics stay zero-banded
        for (String tOrphan : new String[] {"gt6:sandwich", "gt6:bumble_hive", "gt6:crop_sticks",
                "gt6:axle_bronze_small", "gt6:axle_steel_small"}) {
            assertTrue(!tPickaxe.contains(tOrphan) && !tAxe.contains(tOrphan),
                    "the shears/scoop/instant/aMachine-tail domains stay out of both bands: " + tOrphan);
        }
    }

    // ------------------------------------------------------------------ the item face

    /** The walk-expected member list of one family/material face (empty = the tag must NOT exist). */
    private static List<String> expectedMembers(Set<OreDictPrefix> aPrefixes, String aMaterialSnake,
            List<GTMaterialItems.PrefixMaterial> aWalk) {
        List<String> rMembers = new ArrayList<>();
        for (GTMaterialItems.PrefixMaterial tPair : aWalk) {
            if (!aPrefixes.contains(tPair.prefix())) continue;
            if (!GTMaterialItems.snakeCase(tPair.material().mNameInternal).equals(aMaterialSnake)) continue;
            rMembers.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        }
        return rMembers;
    }

    /** The classpath stream of one generated tag file, or null when it does not exist. */
    private static InputStream classpathTag(String aDataPath) {
        return GT6TagsDatagenTest.class.getClassLoader().getResourceAsStream("data/" + aDataPath);
    }

    /**
     * The alias-twin face (rolling batch 2): the alias tag exists iff the main-name face
     * does (the same pair gates both), and carries EXACTLY the main-name walk members —
     * the direct-member twin form, so alias and main faces stay walk-derivable from one rule.
     */
    private static void assertAliasFace(String aFamily, Set<OreDictPrefix> aPrefixes, String aMainSnake,
            String aAliasSnake, List<GTMaterialItems.PrefixMaterial> aWalk) throws Exception {
        String tAliasPath = "forge/tags/items/" + aFamily + "/" + aAliasSnake + ".json";
        List<String> tExpected = expectedMembers(aPrefixes, aMainSnake, aWalk);
        try (InputStream tStream = classpathTag(tAliasPath)) {
            if (tExpected.isEmpty()) {
                assertNull(tStream, "no alias face may exist without a main-name face: " + tAliasPath);
                return;
            }
            assertNotNull(tStream, "the alias twin face must exist: " + tAliasPath);
            assertEquals(tExpected, tagValues(tAliasPath),
                    "alias twin must mirror the main-name members: " + tAliasPath);
        }
    }

    /** One family/material face asserted EXACT against the walk-computed members (order included). */
    private static void assertFamilyFace(String aFamilyPath, Set<OreDictPrefix> aPrefixes, String aMaterialSnake,
            List<GTMaterialItems.PrefixMaterial> aWalk) throws Exception {
        // ALWAYS the canonical tracked-tree face ("forge" — the forge-leg runData --output
        // both legs' classpaths share). The 21.1 leg's own product lives under data/c/ in
        // its NODE-LOCAL runData output (not on any classpath) and is gated 1:1 against
        // this tree by the datagen_tree_check SEGMENT_MAP c-to-forge band; the namespace
        // constant itself is pinned by namespaceSplitIsTheLegFork.
        String tPath = "forge/tags/items/" + aFamilyPath.formatted(aMaterialSnake) + ".json";
        List<String> tExpected = expectedMembers(aPrefixes, aMaterialSnake, aWalk);
        try (InputStream tStream = GT6TagsDatagenTest.class.getClassLoader().getResourceAsStream("data/" + tPath)) {
        if (tExpected.isEmpty()) {
            assertNull(tStream, "no empty tag files may ship: " + tPath);
            return;
        }
            assertNotNull(tStream, "the family face must exist: " + tPath);
            List<String> tActual = tagValues(tPath);
            assertEquals(tExpected, tActual, "exact snapshot of " + tPath);
        }
    }

    /** The item families over sample materials — ingots/nuggets/dusts/gems + the rolling batch 2 plates/rods/hot ingots. */
    @Test
    void itemFamiliesFollowTheGtceuPaths() throws Exception {
        assertFamilyFace(GT6ItemTags.INGOTS_FAMILY, Set.of(OP.ingot), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.INGOTS_FAMILY, Set.of(OP.ingot), "copper", gItemWalk);
        assertFamilyFace(GT6ItemTags.INGOTS_FAMILY, Set.of(OP.ingot), "gold", gItemWalk);
        assertFamilyFace(GT6ItemTags.NUGGETS_FAMILY, Set.of(OP.nugget), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.NUGGETS_FAMILY, Set.of(OP.nugget), "gold", gItemWalk);
        assertFamilyFace(GT6ItemTags.DUSTS_FAMILY, Set.of(OP.dust), "redstone", gItemWalk);
        assertFamilyFace(GT6ItemTags.GEMS_FAMILY, Set.of(OP.gem), "diamond", gItemWalk);
        assertFamilyFace(GT6ItemTags.GEMS_FAMILY, Set.of(OP.gem), "coal", gItemWalk);
        // rolling batch 2 (tags-prefix-materials): TagPrefix.java:456/:502/:267 paths
        assertFamilyFace(GT6ItemTags.PLATES_FAMILY, Set.of(OP.plate), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.RODS_FAMILY, Set.of(OP.stick), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.HOT_INGOTS_FAMILY, Set.of(OP.ingotHot), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.HOT_INGOTS_FAMILY, Set.of(OP.ingotHot), "copper", gItemWalk);
    }

    /**
     * The ecosystem-alias twins (rolling batch 2, the decisions.p24-material-name-normalization
     * ruling): every alias face carries EXACTLY the main-name walk members — the direct-member
     * twin form, and the alias face exists iff the main-name face does (same pair gate).
     */
    @Test
    void ecosystemAliasTwinsCarryTheMainNameMembers() throws Exception {
        // the census-backed double-mount list: aluminium→aluminum, aluminium_brass→aluminum_brass
        assertAliasFace("ingots", Set.of(OP.ingot), "aluminium", "aluminum", gItemWalk);
        assertAliasFace("dusts", Set.of(OP.dust), "aluminium", "aluminum", gItemWalk);
        assertAliasFace("plates", Set.of(OP.plate), "aluminium", "aluminum", gItemWalk);
        assertAliasFace("plates", Set.of(OP.plate), "aluminium_brass", "aluminum_brass", gItemWalk);
        assertAliasFace("storage_blocks", Set.of(OP.blockIngot, OP.blockGem, OP.blockDust),
                "aluminium", "aluminum", gStorageWalk);
        // the oredict synonym aliases stay single-named (no ecosystem tag convention)
        assertNull(classpathTag("forge/tags/items/ingots/gibbsite.json"));
        assertNull(classpathTag("forge/tags/items/ingots/co60.json"));
    }

    /** The storage-block union: blockIngot/blockGem/blockDust share storage_blocks/%s, blockRaw rides raw_%s. */
    @Test
    void storageBlockFacesAreThePrefixUnions() throws Exception {
        Set<OreDictPrefix> tStorage = Set.of(OP.blockIngot, OP.blockGem, OP.blockDust);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_FAMILY, tStorage, "iron", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_FAMILY, tStorage, "diamond", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_FAMILY, tStorage, "redstone", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_RAW_FAMILY, Set.of(OP.blockRaw), "iron", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_RAW_FAMILY, Set.of(OP.blockRaw), "copper", gStorageWalk);
    }

    /**
     * The p25 tool face (task tool-hammer-wrench + food-can-row0, the PIN
     * evolution duty): the three new self-owned crafting-tool tags carry EXACTLY their
     * one registered member each, and the ecosystem tools band grew 7 → 9 → 10 → 16 → 22
     * → 27 → 33 (the hammer + wrench pair, then the bending cylinder, then task
     * w5-t1-dig-six's six dig tools, then task w5-t5-scene-six's six scene
     * tools, then task w5-t2-blade-six's six blade tools, then task
     * w5-t4-field-five's five field tools, appended in the band order). Pinned so
     * any band change is a conscious constant update.
     */
    @Test
    void toolFacesAreTheExactMembers() throws Exception {
        assertEquals(List.of("gt6:hammer"), tagValues("gt6/tags/items/tools/hard_hammer.json"));
        assertEquals(List.of("gt6:wrench"), tagValues("gt6/tags/items/tools/wrench.json"));
        assertEquals(List.of("gt6:bending_cylinder_small"), tagValues("gt6/tags/items/tools/bending_cylinder_small.json"));
        // task w5-t1-dig-six: the six dig-tool tags join the one-member census
        assertEquals(List.of("gt6:pickaxe"), tagValues("gt6/tags/items/tools/pickaxe.json"));
        assertEquals(List.of("gt6:pickaxe_gem"), tagValues("gt6/tags/items/tools/pickaxe_gem.json"));
        assertEquals(List.of("gt6:pickaxe_construction"), tagValues("gt6/tags/items/tools/pickaxe_construction.json"));
        assertEquals(List.of("gt6:shovel"), tagValues("gt6/tags/items/tools/shovel.json"));
        assertEquals(List.of("gt6:spade"), tagValues("gt6/tags/items/tools/spade.json"));
        assertEquals(List.of("gt6:universal_spade"), tagValues("gt6/tags/items/tools/universal_spade.json"));
        // task w5-t2-blade-six: the six blade-tool tags join the one-member census
        assertEquals(List.of("gt6:sword"), tagValues("gt6/tags/items/tools/sword.json"));
        assertEquals(List.of("gt6:knife"), tagValues("gt6/tags/items/tools/knife.json"));
        assertEquals(List.of("gt6:butchery_knife"), tagValues("gt6/tags/items/tools/butchery_knife.json"));
        assertEquals(List.of("gt6:club"), tagValues("gt6/tags/items/tools/club.json"));
        assertEquals(List.of("gt6:axe"), tagValues("gt6/tags/items/tools/axe.json"));
        assertEquals(List.of("gt6:axe_double"), tagValues("gt6/tags/items/tools/axe_double.json"));
        // task w5-t4-field-five: the five field-tool tags join the one-member census
        assertEquals(List.of("gt6:hoe"), tagValues("gt6/tags/items/tools/hoe.json"));
        assertEquals(List.of("gt6:plow"), tagValues("gt6/tags/items/tools/plow.json"));
        assertEquals(List.of("gt6:branch_cutter"), tagValues("gt6/tags/items/tools/branch_cutter.json"));
        assertEquals(List.of("gt6:sense"), tagValues("gt6/tags/items/tools/sense.json"));
        assertEquals(List.of("gt6:hand_drill"), tagValues("gt6/tags/items/tools/hand_drill.json"));
        // task w5-t5-scene-six: the six scene-tool tags join the one-member census
        assertEquals(List.of("gt6:scissors"), tagValues("gt6/tags/items/tools/scissors.json"));
        assertEquals(List.of("gt6:scoop"), tagValues("gt6/tags/items/tools/scoop.json"));
        assertEquals(List.of("gt6:plunger"), tagValues("gt6/tags/items/tools/plunger.json"));
        assertEquals(List.of("gt6:flint_and_tinder"), tagValues("gt6/tags/items/tools/flint_and_tinder.json"));
        assertEquals(List.of("gt6:rolling_pin"), tagValues("gt6/tags/items/tools/rolling_pin.json"));
        assertEquals(List.of("gt6:bending_cylinder"), tagValues("gt6/tags/items/tools/bending_cylinder.json"));

        assertEquals(List.of(
                "gt6:file", "gt6:saw", "gt6:crowbar", "gt6:cutter", "gt6:chisel",
                "gt6:builder_wand", "gt6:screwdriver", "gt6:hammer", "gt6:wrench",
                "gt6:bending_cylinder_small", "gt6:pickaxe", "gt6:pickaxe_gem",
                "gt6:pickaxe_construction", "gt6:shovel", "gt6:spade", "gt6:universal_spade",
                "gt6:sword", "gt6:knife", "gt6:butchery_knife", "gt6:club", "gt6:axe",
                "gt6:axe_double", "gt6:hoe", "gt6:branch_cutter", "gt6:sense", "gt6:plow",
                "gt6:hand_drill", "gt6:scissors", "gt6:scoop", "gt6:plunger",
                "gt6:flint_and_tinder", "gt6:rolling_pin", "gt6:bending_cylinder",
                "gt6:mining_drill_lv", "gt6:mining_drill_mv", "gt6:mining_drill_hv",
                "gt6:chainsaw_lv", "gt6:chainsaw_mv", "gt6:chainsaw_hv",
                "gt6:wrench_lv", "gt6:wrench_mv", "gt6:wrench_hv",
                "gt6:jackhammer_hv_normal", "gt6:jackhammer_hv_no_ores",
                "gt6:buzzsaw_lv", "gt6:screwdriver_lv", "gt6:hand_drill_lv", "gt6:hand_mixer_lv",
                "gt6:monkey_wrench_lv", "gt6:monkey_wrench_mv", "gt6:monkey_wrench_hv",
                "gt6:trimmer_lv",
                // task w5-t7-pocket-eight appended the seven pocket switch forms at the tail
                // (the closed multitool carries no tool-name oredict upstream, Loader_Tools :176)
                "gt6:pocket_multitool_knife", "gt6:pocket_multitool_saw", "gt6:pocket_multitool_file",
                "gt6:pocket_multitool_screwdriver", "gt6:pocket_multitool_wire_cutter",
                "gt6:pocket_multitool_scissors", "gt6:pocket_multitool_chisel"),
                tagValues("forge/tags/items/tools.json"));
    }

    /**
     * The namespace split pin: the family faces above read the CANONICAL tracked tree
     * (the forge-leg --output, shared by both legs' classpaths); the namespace itself is
     * the leg fork — forge "forge" / 21.1 "c" (Tags.java:310-312 vs :799/:923) — and the
     * 21.1 leg's data/c produced face is gated 1:1 against this tree by the
     * datagen_tree_check SEGMENT_MAP c-to-forge band.
     */
    @Test
    void namespaceSplitIsTheLegFork() {
        //? if forge {
        assertEquals("forge", GT6ItemTags.MATERIALS_NAMESPACE);
        //?} else {
        /*assertEquals("c", GT6ItemTags.MATERIALS_NAMESPACE);
        *///?}
        // the GTCEu defaultTagPath precedents, verbatim (TagPrefix.java:279/:290/:405/:416/:729/:223)
        assertEquals("ingots/%s", GT6ItemTags.INGOTS_FAMILY);
        assertEquals("dusts/%s", GT6ItemTags.DUSTS_FAMILY);
        assertEquals("gems/%s", GT6ItemTags.GEMS_FAMILY);
        assertEquals("nuggets/%s", GT6ItemTags.NUGGETS_FAMILY);
        assertEquals("storage_blocks/%s", GT6ItemTags.STORAGE_BLOCKS_FAMILY);
        assertEquals("storage_blocks/raw_%s", GT6ItemTags.STORAGE_BLOCKS_RAW_FAMILY);
        // rolling batch 2 (TagPrefix.java:456/:502/:267)
        assertEquals("plates/%s", GT6ItemTags.PLATES_FAMILY);
        assertEquals("rods/%s", GT6ItemTags.RODS_FAMILY);
        assertEquals("hot_ingots/%s", GT6ItemTags.HOT_INGOTS_FAMILY);
    }

    // ------------------------------------------------------------------ the p27 twin tree face

    /**
     * The 26 vanilla-intersection faces (task vanilla-tag-dual-tree — the provider's
     * VANILLA_INTERSECTION census mirrored here as literal paths, so a provider-side
     * drift cannot self-confirm; ForgeItemTagsProvider.java:56-157 re-verified per entry).
     */
    private static final List<String> PINNED_VANILLA_INTERSECTION = List.of(
            "ingots/iron", "ingots/copper", "ingots/gold", "ingots/netherite",
            "nuggets/iron", "nuggets/gold",
            "gems/diamond", "gems/emerald", "gems/lapis", "gems/amethyst", "gems/quartz",
            "dusts/redstone", "dusts/glowstone", "dusts/prismarine",
            "storage_blocks/iron", "storage_blocks/gold", "storage_blocks/copper",
            "storage_blocks/netherite", "storage_blocks/amethyst", "storage_blocks/lapis",
            "storage_blocks/coal", "storage_blocks/redstone", "storage_blocks/quartz",
            "storage_blocks/raw_iron", "storage_blocks/raw_gold", "storage_blocks/raw_copper");

    /**
     * The p27 forward twin tree (spec ① + acceptance ③): the {@code data/c/tags/items}
     * tree exists on the canonical tracked classpath with EXACTLY the 26 intersection
     * faces, each carrying the same member list as its {@code forge:} twin (one
     * addFamilyFace member emission feeds both namespaces). The exact-file-count pin
     * makes any face beyond the census a conscious update.
     */
    @Test
    void vanillaIntersectionTwinTreesCarryTheSameMembers() throws Exception {
        assertEquals(26, PINNED_VANILLA_INTERSECTION.size(), "the research census count");
        for (String tPath : PINNED_VANILLA_INTERSECTION) {
            List<String> tForge = tagValues("forge/tags/items/" + tPath + ".json");
            assertTrue(!tForge.isEmpty(), "the forge face must be non-empty: " + tPath);
            assertEquals(tForge, tagValues("c/tags/items/" + tPath + ".json"),
                    "the c twin must carry the exact forge members: " + tPath);
        }
        // no face outside the census: the c tree is exactly the 26 files
        try (var tWalk = java.nio.file.Files.walk(java.nio.file.Paths.get(
                GT6TagsDatagenTest.class.getResource("/data/c/tags/items").toURI()))) {
            long tFiles = tWalk.filter(p -> p.toString().endsWith(".json")).count();
            assertEquals(26, tFiles, "the c tree is exactly the intersection census");
        }
    }

    /**
     * The quartz canonical-name face (spec ② + acceptance ③): the platform-canonical
     * {@code gems/quartz} + {@code storage_blocks/quartz} exist on BOTH namespaces and
     * carry the walk-expected nether_quartz members (the FAMILY_MATERIAL_CANONICAL main
     * name — ForgeItemTagsProvider.java:77/:152 ship the vanilla members under exactly
     * these ids); the GT-internal {@code nether_quartz} files stay as the direct-member
     * alias twins; the families WITHOUT an ecosystem quartz tag keep nether_quartz
     * single-named (dusts sampled — rods/bolts/small_gears/raw share the same
     * FAMILY_MATERIAL_CANONICAL keying-out, and the raw storage face never entered the
     * intersection census).
     */
    @Test
    void quartzCanonicalFacesCarryTheNetherQuartzMembers() throws Exception {
        List<String> tGem = expectedMembers(Set.of(OP.gem), "nether_quartz", gItemWalk);
        List<String> tBlock = expectedMembers(Set.of(OP.blockIngot, OP.blockGem, OP.blockDust),
                "nether_quartz", gStorageWalk);
        List<String> tRawBlock = expectedMembers(Set.of(OP.blockRaw), "nether_quartz", gStorageWalk);
        assertTrue(!tGem.isEmpty(), "the census: nether_quartz carries a gem face");
        assertTrue(!tBlock.isEmpty(), "the census: nether_quartz carries a storage face");
        for (String tNamespace : List.of("forge", "c")) {
            assertEquals(tGem, tagValues(tNamespace + "/tags/items/gems/quartz.json"),
                    tNamespace + " gems/quartz canonical face");
            assertEquals(tBlock, tagValues(tNamespace + "/tags/items/storage_blocks/quartz.json"),
                    tNamespace + " storage_blocks/quartz canonical face");
        }
        // the GT-internal alias twins ride the canonical tracked tree only — the c twin
        // tree is the MINIMAL canonical intersection face (26 files, the prior test's
        // exact-count pin), no alias faces there
        assertEquals(tGem, tagValues("forge/tags/items/gems/nether_quartz.json"),
                "forge gems/nether_quartz alias twin");
        assertEquals(tBlock, tagValues("forge/tags/items/storage_blocks/nether_quartz.json"),
                "forge storage_blocks/nether_quartz alias twin");
        assertNull(classpathTag("c/tags/items/gems/nether_quartz.json"));
        // the keyed-out families stay single-named nether_quartz (no quartz main name)
        assertEquals(expectedMembers(Set.of(OP.dust), "nether_quartz", gItemWalk),
                tagValues("forge/tags/items/dusts/nether_quartz.json"));
        assertNull(classpathTag("forge/tags/items/dusts/quartz.json"));
        assertNull(classpathTag("forge/tags/items/rods/quartz.json"));
        assertNull(classpathTag("forge/tags/items/storage_blocks/raw_quartz.json"));
        assertEquals(tRawBlock, tagValues("forge/tags/items/storage_blocks/raw_nether_quartz.json"));
    }
}
