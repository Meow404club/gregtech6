/**
 * Tests for task mdh-6-family-gate: the six remaining registration faces join the mod
 * driver gate — the material BLOCK enumeration (the visibilityGate mirror of
 * GTMaterialItems.enumerate) plus the five table-driven families (metal drums, capsule
 * cells, hoppers, fluid pipes, crafting tables), each carrying a per-row
 * {@code driverDomain} decision consulted in its registration walk (the GTFluids spec-row
 * column precedent, because every family LIST freezes at class-init — strictly before
 * {@code MT.init()} fills the material fields, so the columns are Strings keyed by slug,
 * reconciled against the atlas here rather than re-derived at runtime).
 *
 * <p>The kept 对账 (the mdh-3 家法): the switch columns must agree with
 * {@link GT6ForeignMaterialAtlas#domainOf} for every resolvable row material (atlas single
 * source, ADR-MDH2), and a per-domain ABSENT pin flips exactly the atlas-attributed rows of
 * that domain — the per-domain family-row ledger this card's report quotes falls out of
 * {@link #absentPinDropsExactlyTheAttributedBlockPairs} + the flip arm.
 *
 * <p><b>The Draconium ruling (user, 2026-10-03, decisions.2026-10-03-draconium-visible):</b>
 * Draconium stays bare-visor visible — upstream puts it on MD.DE as COMMON_ORE without
 * visDefault (MT.java:2512), the atlas records COMMON_SECONDARY, and a COMMON_SECONDARY
 * material never hides. Pinned here across all six faces with DE AND AV pinned ABSENT.
 *
 * <p><b>Freeze semantics:</b> the family lists freeze at class-init, and this test JVM
 * class-loads them in the pristine default (the lists are already consumed by the sibling
 * census tests), so the live-install shrink (seed precedes class-init,
 * FMLModContainer.constructMod order) is observed here at the ROW-DECISION level — the same
 * conclusion the fluid face reached (GT6ModDriverClearOutWaveTest javadoc): the offline
 * assertion covers the decision function; the class-init ordering is the mechanism.
 *
 * <p><b>Forge item-tag dangling entries (card ruling, 2026-10-03):</b> tolerated. The
 * committed datagen tree stays default-mode by declared design (the datagen short-circuit,
 * GT6ModDrivers, pinned by the runData byte-identity rerun), so per-material tag JSONs keep
 * their full universe; on an absent-domain install the unregistered gt6 items leave entries
 * vanilla tag loading silently drops (item tags have no required-entry validation). No walk
 * guard: gating the datagen walk would break the byte-identity pin for zero runtime gain.
 *
 * <p>The offline JVM has no FML and the neo junit-fml JVM is kept out of seeding by the
 * GT6ModDrivers test-JVM guard, so every ABSENT state here is explicit seam injection and
 * the pristine default is the zero-change proof.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregapi.data.MT;
import gregtech6.registry.GT6ModDrivers.DriverLevel;

public class GT6ModDriverFamilyGateTest {

    /** The default freeze census (pair-for-pair the pre-mdh-6 universe — the zero-change proof). */
    private static final int BLOCK_PAIRS = 3777; // GTMaterialBlocksRegistrationTest PINNED_TOTAL
    private static final int DRUMS = 12, CELLS = 40, HOPPER_ROWS = 120, PIPE_ROWS = 280, TABLE_ROWS = 120;

    @BeforeAll
    public static void initMaterialSystem() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline init noise; the registries are usable by now
        }
        GTMaterialItems.initMaterials();
        GT6DriverTestSupport.loadFamiliesPristine(); // freeze the five family lists BEFORE any pin (see the support class)
    }

    @AfterEach
    public void restorePristineDriver() {
        GT6ModDrivers.reset();
    }

    // ---- the default zero-change proof (frozen lists, offline JVM) ----

    @Test
    public void defaultFreezeIsThePreMdh6Universe() {
        assertEquals(DRUMS, GTBarrels.HIGH_TIER_METAL_DRUMS.size(), "drum table frozen at the full 12");
        assertEquals(CELLS, GT6Cells.ROWS.size(), "cell table frozen at the full 40");
        assertEquals(CELLS, GT6Cells.BLOCKS_IN_ORDER.size(), "cell blocks frozen one-per-row");
        assertEquals(HOPPER_ROWS, GT6Hoppers.ROWS.size(), "hopper rows frozen at the full 120");
        assertEquals(PIPE_ROWS, GTFluidPipes.ROWS.size(), "pipe rows frozen at the full 280");
        assertEquals(TABLE_ROWS, GTMachines.CRAFTING_TABLE_ROWS.size(), "crafting table rows frozen at the full 120");
        assertEquals(BLOCK_PAIRS, GTMaterialBlocks.registrationOrder().size(), "block pairs frozen at the pinned census");
    }

    // ---- the switch columns reconcile against the atlas (ADR-MDH2 single source) ----

    @Test
    public void familySwitchColumnsMatchTheAtlas() {
        for (GTBarrels.MetalDrumRow tRow : GTBarrels.HIGH_TIER_METAL_DRUMS) {
            assertEquals(GT6ForeignMaterialAtlas.domainOf(tRow.material().get()), tRow.driverDomain(),
                    "drum " + tRow.path() + " column vs atlas");
        }
        for (GT6Cells.CellRow tRow : GT6Cells.ROWS) {
            assertEquals(GT6ForeignMaterialAtlas.domainOf(tRow.material().get()), GT6Cells.driverDomainOf(tRow),
                    "cell " + tRow.path() + " column vs atlas");
        }
        for (GT6Hoppers.HopperMaterial tMat : GT6Hoppers.MATERIALS) {
            assertEquals(GT6ForeignMaterialAtlas.domainOf(tMat.mt()), tMat.driverDomain(),
                    "hopper " + tMat.slug() + " column vs atlas");
        }
        for (GTFluidPipes.FluidPipeMaterial tMat : GTFluidPipes.MATERIALS) {
            assertEquals(GT6ForeignMaterialAtlas.domainOf(tMat.oreDictMaterial()), tMat.driverDomain(),
                    "pipe " + tMat.slug() + " column vs atlas");
        }
        for (GTMachines.CraftingTableMaterial tMat : GTMachines.CRAFTING_TABLE_MATERIALS) {
            assertEquals(GT6ForeignMaterialAtlas.domainOf(tMat.mt()), tMat.driverDomain(),
                    "crafting table " + tMat.slug() + " column vs atlas");
        }
    }

    // ---- the per-domain ABSENT pin flips exactly the atlas-attributed rows (kept 对账) ----

    @Test
    public void absentPinFlipsExactlyTheAttributedFamilyRows() {
        List<String> tSeedable = GT6ForeignMaterialAtlas.seedableDomains();
        assertFalse(tSeedable.isEmpty(), "the atlas drives the seed");
        for (String tDomain : tSeedable) {
            GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
            try {
                for (GTBarrels.MetalDrumRow tRow : GTBarrels.HIGH_TIER_METAL_DRUMS) {
                    assertEquals(!tDomain.equals(GT6ForeignMaterialAtlas.domainOf(tRow.material().get())),
                            tRow.registers(), "drum " + tRow.path() + " under " + tDomain + " ABSENT");
                }
                for (GT6Cells.CellRow tRow : GT6Cells.ROWS) {
                    assertEquals(!tDomain.equals(GT6ForeignMaterialAtlas.domainOf(tRow.material().get())),
                            tRow.registers(), "cell " + tRow.path() + " under " + tDomain + " ABSENT");
                }
                for (GT6Hoppers.HopperMaterial tMat : GT6Hoppers.MATERIALS) {
                    assertEquals(!tDomain.equals(GT6ForeignMaterialAtlas.domainOf(tMat.mt())),
                            tMat.registers(), "hopper " + tMat.slug() + " under " + tDomain + " ABSENT");
                }
                for (GTFluidPipes.FluidPipeMaterial tMat : GTFluidPipes.MATERIALS) {
                    assertEquals(!tDomain.equals(GT6ForeignMaterialAtlas.domainOf(tMat.oreDictMaterial())),
                            tMat.registers(), "pipe " + tMat.slug() + " under " + tDomain + " ABSENT");
                }
                for (GTMachines.CraftingTableMaterial tMat : GTMachines.CRAFTING_TABLE_MATERIALS) {
                    assertEquals(!tDomain.equals(GT6ForeignMaterialAtlas.domainOf(tMat.mt())),
                            tMat.registers(), "crafting table " + tMat.slug() + " under " + tDomain + " ABSENT");
                }
            } finally {
                GT6ModDrivers.reset();
            }
        }
    }

    // ---- the block-face kept 对账: the ABSENT pin drops exactly the attributed pairs ----

    @Test
    public void absentPinDropsExactlyTheAttributedBlockPairs() {
        List<GTMaterialItems.PrefixMaterial> tPre = GTMaterialBlocks.enumerate().kept();
        for (String tDomain : GT6ForeignMaterialAtlas.seedableDomains()) {
            long tAttributed = tPre.stream().filter(p -> tDomain.equals(GT6ForeignMaterialAtlas.domainOf(p.material()))).count();
            GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
            try {
                assertEquals(tPre.size() - tAttributed, GTMaterialBlocks.enumerate().kept().size(),
                        "block pairs kept under " + tDomain + " ABSENT (attributed: " + tAttributed + ")");
            } finally {
                GT6ModDrivers.reset();
            }
        }
    }

    // ---- the named high-value arms: AV + DE (the card's headline domains) ----

    @Test
    public void infinityRowsHideWithAvAbsent() {
        GT6ModDrivers.setDriver(MT.MD.AV.mID, DriverLevel.ABSENT);
        assertFalse(GTBarrels.HIGH_TIER_METAL_DRUMS.stream().filter(r -> r.path().equals("barrel_infinity")).findFirst().orElseThrow().registers(), "infinity drum");
        assertFalse(GT6Cells.ROWS.stream().filter(r -> r.path().equals("cell_infinity")).findFirst().orElseThrow().registers(), "infinity cell");
        assertFalse(GT6Hoppers.MATERIALS.stream().filter(m -> m.slug().equals("infinity")).findFirst().orElseThrow().registers(), "infinity hopper line");
        assertFalse(GTFluidPipes.MATERIALS.stream().filter(m -> m.slug().equals("infinity")).findFirst().orElseThrow().registers(), "infinity pipe line");
        assertFalse(GTMachines.CRAFTING_TABLE_MATERIALS.stream().filter(m -> m.slug().equals("infinity")).findFirst().orElseThrow().registers(), "infinity crafting table line");
    }

    @Test
    public void awakenedDraconiumRowsHideWithDeAbsent() {
        GT6ModDrivers.setDriver(MT.MD.DE.mID, DriverLevel.ABSENT);
        assertFalse(GTBarrels.HIGH_TIER_METAL_DRUMS.stream().filter(r -> r.path().equals("barrel_awakened_draconium")).findFirst().orElseThrow().registers(), "awakened drum");
        assertFalse(GT6Cells.ROWS.stream().filter(r -> r.path().equals("cell_awakened_draconium")).findFirst().orElseThrow().registers(), "awakened cell");
        assertFalse(GT6Hoppers.MATERIALS.stream().filter(m -> m.slug().equals("awakened_draconium")).findFirst().orElseThrow().registers(), "awakened hopper line");
        assertFalse(GTFluidPipes.MATERIALS.stream().filter(m -> m.slug().equals("awakened_draconium")).findFirst().orElseThrow().registers(), "awakened pipe line");
        assertFalse(GTMachines.CRAFTING_TABLE_MATERIALS.stream().filter(m -> m.slug().equals("draconium_awakened")).findFirst().orElseThrow().registers(), "awakened crafting table line");
    }

    // ---- the Draconium ruling (user, 2026-10-03): COMMON_SECONDARY never hides ----

    @Test
    public void draconiumStaysVisibleWithDeAndAvAbsent() {
        GT6ModDrivers.setDriver(MT.MD.DE.mID, DriverLevel.ABSENT);
        GT6ModDrivers.setDriver(MT.MD.AV.mID, DriverLevel.ABSENT);
        assertTrue(GT6ForeignMaterialAtlas.domainOf(MT.Draconium) == null, "atlas keeps Draconium unattributed (COMMON_SECONDARY, ADR-MDH2)");
        assertTrue(GTBarrels.HIGH_TIER_METAL_DRUMS.stream().filter(r -> r.path().equals("barrel_draconium")).findFirst().orElseThrow().registers(), "draconium drum");
        assertTrue(GT6Cells.ROWS.stream().filter(r -> r.path().equals("cell_draconium")).findFirst().orElseThrow().registers(), "draconium cell");
        assertTrue(GT6Hoppers.MATERIALS.stream().filter(m -> m.slug().equals("draconium")).findFirst().orElseThrow().registers(), "draconium hopper line");
        assertTrue(GTFluidPipes.MATERIALS.stream().filter(m -> m.slug().equals("draconium")).findFirst().orElseThrow().registers(), "draconium pipe line");
        assertTrue(GTMachines.CRAFTING_TABLE_MATERIALS.stream().filter(m -> m.slug().equals("draconium")).findFirst().orElseThrow().registers(), "draconium crafting table line");
        long tKeptDraconiumBlocks = GTMaterialBlocks.enumerate().kept().stream()
                .filter(p -> p.material() == MT.Draconium).count();
        assertTrue(tKeptDraconiumBlocks > 0, "draconium storage-block pairs survive the DE+AV pin");
    }
}
