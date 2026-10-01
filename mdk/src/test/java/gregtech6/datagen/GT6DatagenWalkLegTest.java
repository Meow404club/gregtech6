/**
 * Tests for task mdh-clearout-batch2: the THIRD consumption leg — the datagen walk face.
 *
 * <p>The runData NPE (f8ffa0bd1, known_bugs.main_rundata_hopper_npe) was the walk face
 * dereferencing a driver-hidden pair: the datagen providers call
 * {@code GTMaterialItems.get(...).get()} over data-driven tables, and an ABSENT domain's
 * pairs never enter the registration INDEX (the enumerate gate is the same root the censuses
 * walk; registration follows the seed on a real install). The sweep put a null-drop guard at
 * every walk face a table member can make null — this class pins the semantics: a hidden row
 * answers null / falls out of the list, the walk completes without throwing, and visible
 * rows keep building.
 *
 * <p>The lookup truth: a test JVM cannot re-run registration, so the stubbed arms swap the
 * {@code GTMaterialItems.sLookup} seam (the GT6RecipesImplosion.sMaterialItemResolver
 * precedent) for the registration-truth stub — a pair resolves iff the live
 * {@code registrationOrder()} carries it, which under a driver pin is exactly the real
 * INDEX's content (both legs). The default-mode controls read the REAL INDEX, which only
 * exists on the registry-live leg (the neo junit-fml boot registers everything; the offline
 * forge JVM never fires RegisterEvent — the declared leg asymmetry, the ItemLatch precedent).
 *
 * <p>Offline discipline: driver state via the ADR-MDH3 seams, the stub and the driver both
 * restored in afterEach, always.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.items.armor.GT6ArmorMaterials;
import gregtech6.items.tools.electric.GT6ElectricToolItem;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6Rails;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6ModDrivers;
import gregtech6.registry.GT6ModDrivers.DriverLevel;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6DatagenWalkLegTest extends GTOfflineTestBase {

    private static final GT6CraftingRecipes RECIPES = new GT6CraftingRecipes(
            new net.minecraft.data.PackOutput(java.nio.file.Path.of("build", "test-walk-leg")),
            java.util.concurrent.CompletableFuture.completedFuture(null));

    /** True when this JVM actually registered the GT6 content (the neo junit-fml leg). */
    private static boolean registryLive() {
        return !GTMaterialItems.items().isEmpty();
    }

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
    }

    @AfterEach
    void restoreSeams() {
        GT6ModDrivers.reset();
        GT6MaterialItemsLookup.restore();
    }

    /** The stub: a pair resolves iff it is in the DEFAULT registration order AND its material is
     * driver-visible — the exact content the real INDEX would have after a seeded registration
     * (the prefix-generation truth captured at stub time, the driver truth evaluated live per
     * lookup). The handle value is a vanilla anchor (resolved fine offline; null-path arms never
     * dereference it anyway). Install only at pristine-driver time (afterEach guarantees it). */
    private static void stubLookupFromRegistrationOrder() {
        Set<GTMaterialItems.PrefixMaterial> tDefaultOrder = Set.copyOf(GTMaterialItems.registrationOrder());
        GT6MaterialItemsLookup.stub(tDefaultOrder);
    }

    private static GT6Rails.RailRow railRow(String aSlug) {
        return GT6Rails.ROWS.stream().filter(tRow -> tRow.path().equals(aSlug)).findFirst().orElseThrow();
    }

    private static GT6Hoppers.HopperRow hopperRow(String aSlug) {
        return GT6Hoppers.ROWS.stream().filter(tRow -> tRow.material().slug().equals(aSlug)).findFirst().orElseThrow();
    }

    // ---- the driver-attributed null-drop (both legs, the registration-truth stub) ----

    @Test
    public void hiddenRailRowsDropWithoutThrowingAndTheWalkCompletes() {
        stubLookupFromRegistrationOrder();
        GT6ModDrivers.setDriver(MT.MD.TiC.mID, DriverLevel.ABSENT); // the Al ladder column
        assertNull(GTMaterialItems.get(OP.railGt, MT.Al), "stub precondition: the Al rail pair is out of the gated order");
        assertNull(RECIPES.railRecipeBuilder(railRow("rail_aluminium")), "the Al rail's pair is gone — the builder answers null, not NPE");
        assertNotNull(GTMaterialItems.get(OP.railGt, MT.Bronze), "the bronze rail pair survives the TiC pin (attribution control)");
        GT6ModDrivers.reset();
        GT6ModDrivers.setDriver(MT.MD.ReC.mID, DriverLevel.ABSENT);
        assertNull(RECIPES.railRecipeBuilder(railRow("rail_tungstencarbide")), "the TungstenCarbide rail (ReC PRIMARY, atlas :2412) drops under its own domain");
        // the full 30-row walk completes under both pins on the PRODUCTION lookup — the offline
        // leg walks the all-null face (its registry truth), the live leg builds for real
        GT6MaterialItemsLookup.restore();
        GT6ModDrivers.reset();
        GT6ModDrivers.setDriver(MT.MD.TiC.mID, DriverLevel.ABSENT);
        for (GT6Rails.RailRow tRow : GT6Rails.ROWS) RECIPES.railRecipeBuilder(tRow);
        GT6ModDrivers.reset();
        GT6ModDrivers.setDriver(MT.MD.ReC.mID, DriverLevel.ABSENT);
        for (GT6Rails.RailRow tRow : GT6Rails.ROWS) RECIPES.railRecipeBuilder(tRow);
    }

    @Test
    public void hiddenStoneAndHopperRowsDropWithoutThrowing() {
        stubLookupFromRegistrationOrder();
        GT6ModDrivers.setDriver(MT.MD.EtFu.mID, DriverLevel.ABSENT); // the prismarine stone families (atlas :2085/:2086)
        assertNull(RECIPES.slabFromRocksBuilder(gregtech6.registry.GTStoneBlocks.STONES.stream()
                .filter(tSpec -> tSpec.snake().equals("prismarine_light")).findFirst().orElseThrow()),
                "the EtFu prismarine rocks drop under the domain pin");
        GT6ModDrivers.reset();
        GT6ModDrivers.setDriver(MT.MD.TE.mID, DriverLevel.ABSENT);
        assertNull(RECIPES.hopperRecipeBuilder(hopperRow("lumium")), "the batch-1 proven NPE face (f8ffa0bd1): a TE hopper row null-drops under the pin");
        // the full walks complete on the PRODUCTION lookup (offline: the all-null face; live: the real build)
        GT6MaterialItemsLookup.restore();
        for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) RECIPES.hopperRecipeBuilder(tRow);
        GT6ModDrivers.reset();
        GT6ModDrivers.setDriver(MT.MD.EtFu.mID, DriverLevel.ABSENT);
        for (var tSpec : gregtech6.registry.GTStoneBlocks.STONES) RECIPES.slabFromRocksBuilder(tSpec);
    }

    @Test
    public void hiddenArmorAndToolRowsDropWithoutThrowing() {
        stubLookupFromRegistrationOrder();
        GT6ModDrivers.setDriver(MT.MD.TiC.mID, DriverLevel.ABSENT); // the HEAT suit's Al 'M' column + the MV tool ladder rung
        assertNull(RECIPES.armorPieceBuilder(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.HEAT), 0),
                "the HEAT suit skips under the TiC pin (its 'M' foil is Al)");
        assertNull(RECIPES.electricToolBuilder(GT6ElectricToolItem.MINING_DRILL_MV),
                "the MV drill skips (its Electric_T rung is Al, GTMachines ladder index 1)");
        assertNotNull(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.HEAT), "precondition: the suit table carries HEAT");
    }

    @Test
    public void theLootAndCupGuardPremisesAnswerThePins() {
        stubLookupFromRegistrationOrder();
        assertNotNull(GTMaterialItems.get(OP.dust, MT.Porcelain), "precondition: porcelain dust is in the gated order (the cup face)");
        GT6ModDrivers.setDriver(MT.MD.IHL.mID, DriverLevel.ABSENT);
        assertNull(GTMaterialItems.get(OP.dust, MT.Porcelain), "IHL ABSENT (atlas :2234): the cup guard's pair answers null");
        GT6ModDrivers.reset();
        assertTrue(GTMaterialItems.get(OP.rockGt, MT.Azurite) != null, "precondition: the Azurite indicator rock is in the gated order (the surface-loot face)");
        GT6ModDrivers.setDriver(MT.MD.TROPIC.mID, DriverLevel.ABSENT);
        assertNull(GTMaterialItems.get(OP.rockGt, MT.Azurite), "TROPIC ABSENT (atlas :2527): the surface-loot guard's pair answers null");
        GT6ModDrivers.reset();
        assertNotNull(GTMaterialItems.get(OP.oreRaw, MT.Jade), "precondition: the Jade ore axis pair is in the gated order (the ore-loot face)");
        GT6ModDrivers.setDriver(MT.MD.ERE.mID, DriverLevel.ABSENT);
        assertNull(GTMaterialItems.get(OP.oreRaw, MT.Jade), "ERE ABSENT (atlas :2301): the ore-loot guard's pair answers null");
    }

    // ---- the default-mode controls (registry-live leg only: the REAL INDEX) ----

    @Test
    public void defaultModeKeepsEveryWalkFaceBuilding() {
        assumeTrue(registryLive(), "offline JVM: no RegisterEvent, the real-INDEX controls pin on the registry-live leg");
        assertNotNull(RECIPES.hopperRecipeBuilder(hopperRow("lead")), "a never-hidden material's hopper row builds (Pb, FZ CS)");
        assertNotNull(RECIPES.railRecipeBuilder(railRow("rail_bronze")), "the bronze rail builds");
        assertNotNull(RECIPES.armorPieceBuilder(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.RADIATION), 0), "the RADIATION suit (Pb plate) builds");
        assertNotNull(RECIPES.electricToolBuilder(GT6ElectricToolItem.MINING_DRILL_LV), "the LV drill (SteelGalvanized rung) builds");
        assertEquals(4, RECIPES.usbStickRecipeRows().size(), "default mode keeps all four USB stick tiers");
        Set<String> tRocks = new HashSet<>();
        for (var tSpec : gregtech6.registry.GTStoneBlocks.STONES) {
            if (tSpec.snake().startsWith("prismarine")) assertNotNull(RECIPES.slabFromRocksBuilder(tSpec), "the prismarine rocks build by default");
            else assertNotNull(RECIPES.slabFromRocksBuilder(tSpec), "every stone family's rocks row builds by default: " + tSpec.snake());
            tRocks.add(tSpec.snake());
        }
        assertEquals(17, tRocks.size(), "the stone family census stays 17");
    }
}

/** The per-leg typed seam holder — the stub's value type differs per leg (RegistryObject vs
 * DeferredHolder), so the assignment lives in one forked nest here instead of five test arms. */
//? if forge {
final class GT6MaterialItemsLookup {
    private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item>> sOriginal;
    static void stub(Set<GTMaterialItems.PrefixMaterial> aKept) {
        sOriginal = GTMaterialItems.sLookup;
        net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tStick =
                net.minecraftforge.registries.RegistryObject.create(new net.minecraft.resources.ResourceLocation("minecraft", "stick"),
                        net.minecraft.core.registries.Registries.ITEM, "minecraft");
        GTMaterialItems.sLookup = (aPrefix, aMaterial) ->
                aKept.contains(new GTMaterialItems.PrefixMaterial(aPrefix, aMaterial)) && GT6ModDrivers.isVisible(aMaterial) ? tStick : null;
    }
    static void restore() {
        if (sOriginal != null) GTMaterialItems.sLookup = sOriginal;
        sOriginal = null;
    }
}
//?} else {
/*final class GT6MaterialItemsLookup {
    private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item>> sOriginal;
    static void stub(Set<GTMaterialItems.PrefixMaterial> aKept) {
        sOriginal = GTMaterialItems.sLookup;
        net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> tStick =
                net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.core.registries.Registries.ITEM,
                        new net.minecraft.resources.ResourceLocation("minecraft", "stick"));
        GTMaterialItems.sLookup = (aPrefix, aMaterial) ->
                aKept.contains(new GTMaterialItems.PrefixMaterial(aPrefix, aMaterial)) && GT6ModDrivers.isVisible(aMaterial) ? tStick : null;
    }
    static void restore() {
        if (sOriginal != null) GTMaterialItems.sLookup = sOriginal;
        sOriginal = null;
    }
}
*///?}
