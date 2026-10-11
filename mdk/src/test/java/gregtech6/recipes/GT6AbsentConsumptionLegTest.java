/**
 * Tests for task mdh-3-clear-out-wave, acceptance 4: the recipe-consumption mechanism pin —
 * rows referencing an unregistered id fall out naturally, nothing crashes. RE-SIGNED by task
 * material-foreign-domain-registration (user ruling id1745-4): the driver no longer condemns
 * GT6-generated item ids, so on a real ABSENT install the ~11.9k committed references to the
 * atlas materials' gt6 ids RESOLVE (the item face is pin-invariant) — the JSON leg below pins
 * the mechanism itself (unregistered id → bad row, WARN + skip, never crash), which stays the
 * dormant-row face for genuinely absent content (foreign-mod ids; the still driver-gated
 * fluid rows, e.g. the annotated GTFluids chemical specs).
 *
 * <p>Two legs, the two row surfaces the card names:
 * <ul>
 * <li><b>JSON leg</b> — {@code GT6RecipeMapJsonLoader.resolveItem} answers a bad row for an
 *     unregistered id ("unregistered item id", WARN + skip, never crash the reload). The
 *     fixture resolver stands in for a shrunk registry: unresolvable ids answer null exactly
 *     like the live ForgeRegistries lookup would.</li>
 * <li><b>Java-wall leg</b> — the recipe walls resolve materials through
 *     {@code sMaterialItemResolver} (the upstream {@code mat() → null} drop): a
 *     genuinely-unregistered pair's {@code buildRecipe} answers null and the pour counts it
 *     skipped (GT6RecipesImplosion.java:187/:202-207). The fixture mirrors the real gate: an
 *     item exists iff the registration face registered it.</li>
 * </ul>
 *
 * <p>Offline discipline: no FML, resolvers injected (the GT6RecipeMapJsonLoaderTest
 * convention), driver state via the ADR-MDH3 seams, {@code reset()} after each test.
 */
package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import gregapi.oredict.OreDictMaterial;
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
 * lives in {@code gregtech6.datagen.GT6DatagenWalkLegTest}. Since ruling id1745-4 the driver
 * never removes item pairs from the INDEX, so the guards only ever fire on genuinely
 * unregistered pairs — the SEEDED_DOMAINS activation's other two preconditions (the
 * datagen-JVM short-circuit, the committed-tree verdict) keep their full force.
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
        gregtech6.registry.GT6DriverTestSupport.loadFamiliesPristine(); // mdh-6: freeze the gated family lists before any pin
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
        GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.parse("gt6:shredder"), tFile));

        assertEquals(tBefore + 1, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "only the resolvable row pours — the hidden-id row is skipped, not fatal");
        assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"), "the tracker mirrors the survivors");
    }

    /** The wall leg: the registration-truth resolver (the real-install shape post-ruling id1745-4:
     * an item exists iff the registration face registered it — the item face is pin-invariant),
     * buildRecipe drops only genuinely-unregistered rows. The row material is picked at runtime
     * from the TE PRIMARY set by registration truth (a synthetic row for a material without a
     * generated dust would say nothing about the gate). */
    @Test
    public void wallRowsFallOutThroughTheMatNullDrop() {
        // the real-install shape: an item exists iff the registration order carries the pair
        java.util.Set<GTMaterialItems.PrefixMaterial> tOrder =
                java.util.Set.copyOf(GTMaterialItems.registrationOrder());
        GT6RecipesImplosion.sMaterialItemResolver = (aPrefix, aMaterial) ->
                tOrder.contains(new GTMaterialItems.PrefixMaterial(aPrefix, aMaterial)) ? Items.IRON_INGOT : null;
        GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
        GT6RecipesImplosion.sCircuitResolver = aConfig -> new ItemStack(Items.STICK);

        // the row material: a TE PRIMARY material whose GT6 dust registers (the revival arm's
        // subject); the synthetic tier's outPrefix rides OP.dust so the row needs only that pair
        OreDictMaterial tTeRow = java.util.Arrays.stream(new OreDictMaterial[] {MT.Enderium, MT.Signalum, MT.Lumium, MT.Basalz, MT.Blitz, MT.Blizz})
                .filter(tMaterial -> tOrder.contains(new GTMaterialItems.PrefixMaterial(OP.dust, tMaterial)))
                .findFirst().orElse(null);
        assertNotNull(tTeRow, "precondition: a TE PRIMARY material with a generated dust (the item face's registration truth)");
        GT6RecipesImplosion.ImplosionTier tTier = new GT6RecipesImplosion.ImplosionTier(":leg", 8, 1, 1, OP.dust);
        GT6RecipesImplosion.ImplosionRow tTe = new GT6RecipesImplosion.ImplosionRow(":leg-te", tTeRow, tTeRow); // TE PRIMARY

        // default (all-PRESENT): the row builds
        assertNotNull(GT6RecipesImplosion.buildRecipe(tTe, tTier), "default: the TE PRIMARY row builds");

        // TE ABSENT: the row still builds — the GT6-generated items are GT6's own content and
        // register unconditionally (ruling id1745-4, the mdh ABSENT 连坐 retired from the item face)
        GT6ModDrivers.setDriver(MT.MD.TE.mID, DriverLevel.ABSENT);
        assertNotNull(GT6RecipesImplosion.buildRecipe(tTe, tTier), "ABSENT domain: the PRIMARY row still builds (the item face is pin-invariant, ruling id1745-4)");
        assertFalse(GT6ModDrivers.isVisible(MT.Enderium), "the visibility gate still answers the pin (the fluid/block faces' truth)");

        // the mat() null-drop mechanics (the upstream shape, GT6RecipesImplosion.java:202-207):
        // a genuinely-unregistered pair resolves null and the pour skips the row — pinned
        // driver-free (the fluid face keeps the driver semantics for its own rows).
        GT6ModDrivers.reset();
        GT6RecipesImplosion.sMaterialItemResolver = (aPrefix, aMaterial) -> null;
        assertNull(GT6RecipesImplosion.buildRecipe(tTe, tTier), "an unregistered pair resolves null and buildRecipe drops the row (never crashes)");
    }

    private static JsonElement json(String aJson) {
        return JsonParser.parseString(aJson);
    }
}
