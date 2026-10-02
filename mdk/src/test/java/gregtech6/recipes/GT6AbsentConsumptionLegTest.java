/**
 * Tests for task mdh-3-clear-out-wave, acceptance 4: the recipe-consumption mechanism pin —
 * under an ABSENT driver domain the referencing rows fall out naturally, nothing crashes.
 *
 * <p>Two legs, the two row surfaces the card names:
 * <ul>
 * <li><b>JSON leg</b> — {@code GT6RecipeMapJsonLoader.resolveItem} answers a bad row for an
 *     unregistered id ("unregistered item id", WARN + skip, never crash the reload), so the
 *     ~11.9k committed references to the 137 PRIMARY materials' ids drop per-row at reload
 *     time on a real ABSENT install. The fixture resolver stands in for the shrunk registry:
 *     hidden-material ids answer null exactly like the live ForgeRegistries lookup would.</li>
 * <li><b>Java-wall leg</b> — the recipe walls resolve materials through
 *     {@code sMaterialItemResolver} (the upstream {@code mat() → null} drop): a hidden
 *     material's {@code buildRecipe} answers null and the pour counts it skipped
 *     (GT6RecipesImplosion.java:187/:203-207). The fixture mirrors the real gate: an item
 *     exists iff the driver keeps its material visible.</li>
 * </ul>
 *
 * <p>Offline discipline: no FML, resolvers injected (the GT6RecipeMapJsonLoaderTest
 * convention), driver state via the ADR-MDH3 seams, {@code reset()} after each test.
 */
package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6ModDrivers;
import gregtech6.registry.GT6ModDrivers.DriverLevel;

/**
 * The consumption-face pins of the clear-out wave: JSON rows referencing hidden ids bad-row
 * out at reload, java walls drop through the upstream mat() null shape.
 *
 * <p><b>The third leg — the datagen walk face (review seat XVI):</b> the datagen recipe walks
 * consume the same registration root. The proven NPE face (hopperRecipeBuilder,
 * GTMaterialItems.get(...).get() under a live-FML seeded JVM) is guarded row-level by the
 * f8ffa0bd1 review fix (the builder answers null, the walk callers skip — the JSON bad-row
 * semantics); mdh-clearout-batch2 landed the remaining sweep guards and the walk-face pin
 * lives in {@code gregtech6.datagen.GT6DatagenWalkLegTest} (same package as the guarded
 * builders — the runData both-legs byte-identity rerun stays the committed-tree evidence).
 * The SEEDED_DOMAINS re-activation rode those three preconditions: the datagen-JVM
 * short-circuit (GT6ModDriversTest.datagenJvmShortCircuitsTheSeed), this sweep, and the
 * committed-tree verdict — registration precedes GatherDataEvent, so a late reset cannot
 * un-shrink the universe.
 */
public class GT6AbsentConsumptionLegTest extends GTRecipesOfflineTestBase {

    private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
    private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;
    private static final java.util.function.BiFunction<gregapi.oredict.OreDictPrefix, gregapi.oredict.OreDictMaterial, Item> sDefaultImplosionItems = GT6RecipesImplosion.sMaterialItemResolver;
    private static final java.util.function.Supplier<Item> sDefaultTnt = GT6RecipesImplosion.sTntResolver;
    private static final java.util.function.Function<Integer, ItemStack> sDefaultCircuit = GT6RecipesImplosion.sCircuitResolver;

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @AfterEach
    void restoreSeams() {
        GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems;
        GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
        GT6RecipesImplosion.sMaterialItemResolver = sDefaultImplosionItems;
        GT6RecipesImplosion.sTntResolver = sDefaultTnt;
        GT6RecipesImplosion.sCircuitResolver = sDefaultCircuit;
        GT6ModDrivers.reset();
        GT6RecipeMaps.reset();
    }

    /** The JSON leg: rows referencing hidden-domain ids bad-row out, the file still pours. */
    @Test
    public void jsonRowsReferencingHiddenIdsFallOutWithoutCrashing() {
        GT6RecipeMaps.init();
        GT6RecipeMapJsonLoader.resetForTest();
        // the ABSENT-registry stand-in: the control ids resolve, every gt6 material-item id is unregistered
        GT6RecipeMapJsonLoader.sItemResolver = aId -> {
            String tId = aId.toString();
            return tId.equals("minecraft:dead_bush") ? Items.DEAD_BUSH : tId.equals("minecraft:stick") ? Items.STICK : null;
        };
        GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;

        int tBefore = GT6RecipeMaps.SHREDDER.mRecipeList.size();
        JsonElement tFile = json("""
				{"recipes": [
				  {"inputs":[{"item":"gt6:ingot_enderium","count":1}],
				   "outputs":[{"item":"gt6:plate_enderium","count":1}],"duration":16,"eut":16},
				  {"inputs":[{"item":"minecraft:dead_bush","count":1}],
				   "outputs":[{"item":"minecraft:stick","count":2}],"duration":16,"eut":16}
				]}""");
        // the pour target is the file id (the loader's map-key rule) — shredder, the JSON test's map
        GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6:shredder"), tFile));

        assertEquals(tBefore + 1, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "only the resolvable row pours — the hidden-id row is skipped, not fatal");
        assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"), "the tracker mirrors the survivors");
    }

    /** The wall leg: the driver-gated resolver nulls hidden materials, buildRecipe drops the row. */
    @Test
    public void wallRowsFallOutThroughTheMatNullDrop() {
        // the real-install shape: an item exists iff the driver keeps its material visible
        GT6RecipesImplosion.sMaterialItemResolver = (aPrefix, aMaterial) -> GT6ModDrivers.isVisible(aMaterial) ? Items.IRON_INGOT : null;
        GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
        GT6RecipesImplosion.sCircuitResolver = aConfig -> new ItemStack(Items.STICK);
        GT6RecipesImplosion.ImplosionTier tTier = new GT6RecipesImplosion.ImplosionTier(":leg", 8, 1, 1, OP.gem);

        GT6RecipesImplosion.ImplosionRow tEnderium = new GT6RecipesImplosion.ImplosionRow(":leg-te", MT.Enderium, MT.Enderium); // TE PRIMARY
        GT6RecipesImplosion.ImplosionRow tPyrotheum = new GT6RecipesImplosion.ImplosionRow(":leg-self", MT.Pyrotheum, MT.Pyrotheum); // GT6_SELF
        GT6RecipesImplosion.ImplosionRow tInvar = new GT6RecipesImplosion.ImplosionRow(":leg-cs", MT.Invar, MT.Invar); // TE COMMON_SECONDARY

        // default (all-PRESENT): every row builds
        assertNotNull(GT6RecipesImplosion.buildRecipe(tEnderium, tTier), "default: the TE PRIMARY row builds");
        assertNotNull(GT6RecipesImplosion.buildRecipe(tPyrotheum, tTier), "default: the GT6_SELF row builds");
        assertNotNull(GT6RecipesImplosion.buildRecipe(tInvar, tTier), "default: the CS row builds");

        // TE ABSENT: the PRIMARY row falls out (the upstream mat() null drop), CS/SELF survive
        GT6ModDrivers.setDriver(MT.MD.TE.mID, DriverLevel.ABSENT);
        assertNull(GT6RecipesImplosion.buildRecipe(tEnderium, tTier), "ABSENT domain: the PRIMARY row resolves null and the pour skips it");
        assertNotNull(GT6RecipesImplosion.buildRecipe(tPyrotheum, tTier), "GT6_SELF rows survive (lesson id1218)");
        assertNotNull(GT6RecipesImplosion.buildRecipe(tInvar, tTier), "COMMON_SECONDARY rows survive (ADR-MDH2)");
    }

    private static JsonElement json(String aJson) {
        return JsonParser.parseString(aJson);
    }
}
