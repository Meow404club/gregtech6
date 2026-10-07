/**
 * Tests for task planks-blockification: the plank face convergence — the wood-planks-register
 * prefix-item family (130 flat gt6:plank_* items) RETIRED, the plank face re-homed on the
 * wooddict BlockItem rows ({@link GT6WoodDict}: the 17 BlockTreePlanks/2 cubes + the vanilla
 * identities).
 *
 * <p>Upstream grounding: planks upstream exist ONLY as blocks (Loader_Woods.java:62-65
 * BlockTreePlanks/2; the item loader carries zero plank rows — loaders/a/Loader_Items.java
 * census; OP.plank has no PrefixItem and no creative tab, PrefixItem.java:89-91 semantics).
 * The oredict plank population is the WoodDictionary PLANKS face (LoaderWoodDictionary.java:45-172):
 * the 6 vanilla rows, the 17 GT6 rows and one row per loaded mod wood — the mod woods being
 * the rows the mod-less port world declares ABSENT (the compat-cut ruling).
 *
 * <p>The absorb seam history: the sawing increment card (task sawing-plank-concrete-increment)
 * rode IL.Plank -> minecraft:oak_planks; task plank-mapping-sweep re-poured the 76 declared
 * rows onto the then-registered gt6:plank_wood prefix item; THIS card lands the generic plank
 * BLOCK under the same id (gt6:plank_wood = the BlockTreePlanks meta 9 cube), so those rows
 * are already on the converged face.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class GT6PlankRegistrationTest {

    @BeforeAll
    public static void initMaterialSystem() {
        // the vanilla bootstrap bracket (the GT6WireGtRegistrationTest shape): the wooddict
        // suppliers touch GT6TreeBlocks/Registries statics (the placement pin), and an
        // UN-bootstrapped first clinit poisons the registry classes for every later suite
        // in the JVM — version detect must precede bootStrap
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline-expected
        }
        GTMaterialItems.initMaterials();
    }

    @Test
    public void thePlankPrefixIsOffTheItemPath() {
        // the retirement: no OP.plank pair survives in the registration universe
        long tCount = GTMaterialItems.registrationOrder().stream().filter(tPair -> tPair.prefix() == OP.plank).count();
        assertEquals(0, tCount, "zero plank prefix items (the wood-planks-register family retired, the upstream ship shape)");
        assertFalse(GTMaterialItems.itemPathPrefixes().contains(OP.plank), "OP.plank is off the item-path list");
        // and no surviving pair composes the retired id shape (the reference-zero face)
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            String tId = GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material());
            assertFalse(tId.startsWith("plank_"), "no surviving item may compose the retired plank_ id shape: " + tId);
        }
    }

    @Test
    public void theGateCarriesNoPortAuthorityException() {
        // the former WOOD gate leg is gone — every prefix evaluates the plain upstream criterion
        assertEquals(OP.plate.isGeneratingItem(MT.Iron), GTMaterialItems.generatesItemPathItem(OP.plate, MT.Iron),
                "the gate is transparent (the plank exception removed)");
        assertEquals(OP.plate.isGeneratingItem(MT.Wood), GTMaterialItems.generatesItemPathItem(OP.plate, MT.Wood),
                "the gate is transparent for the wood family too");
    }

    @Test
    public void theWooddictFaceCarriesTheSeventeenGt6Rows() {
        // the upstream meta order over the LoaderWoodDictionary materials (:66-172)
        String[] tExpected = {
                "rubber_planks", "maple_planks", "willow_planks", "blue_mahoe_planks", "hazel_planks",
                "cinnamon_planks", "coconut_planks", "rainbowood_planks", "plank_wood_compressed", "plank_wood",
                "plank_wood_treated", "crate", "plank_wood_dead", "plank_wood_rotten", "plank_wood_mossy",
                "plank_wood_frozen", "blue_spruce_planks"};
        assertEquals(tExpected.length, GT6WoodDict.GT6_ROWS.size(), "17 GT6 rows (BlockTreePlanks 16 metas + Planks2:0)");
        for (int i = 0; i < tExpected.length; i++) {
            assertEquals(tExpected[i], GT6WoodDict.GT6_ROWS.get(i).id(), "GT6 row " + i + " = the upstream meta order");
        }
        // the upstream material anchors (LoaderWoodDictionary.java:69/:71/:73/:75/:91/:93/:95/:97/:157/:66/:159/:161/:165/:167/:169/:171/:113)
        assertEquals(MT.WoodRubber, GT6WoodDict.GT6_ROWS.get(0).material(), "the rubber row is WoodRubber (:69)");
        assertEquals(MT.Wood, GT6WoodDict.GT6_ROWS.get(9).material(), "meta 9 is the MT.Wood identity face (:66 DEFAULT_PLANK)");
        assertEquals(MT.WoodTreated, GT6WoodDict.GT6_ROWS.get(10).material(), "meta 10 is WoodTreated (:159)");
        assertEquals(MT.Wood, GT6WoodDict.GT6_ROWS.get(11).material(), "meta 11 (Crate) is MT.Wood too (:161 — the upstream duality)");
        assertEquals(MT.WOODS.BlueSpruce, GT6WoodDict.GT6_ROWS.get(16).material(), "Planks2:0 is BlueSpruce (:113)");
    }

    @Test
    public void theVanillaIdentitiesAreTheWooddictVanillaFace() {
        // the 1.7.10 six verbatim + the 1.20.1 tree set under the same identity rule
        String[] tExpected = {
                "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks",
                "minecraft:acacia_planks", "minecraft:dark_oak_planks", "minecraft:mangrove_planks", "minecraft:cherry_planks",
                "minecraft:bamboo_planks", "minecraft:crimson_planks", "minecraft:warped_planks"};
        assertEquals(tExpected.length, GT6WoodDict.VANILLA_ROWS.size(), "11 vanilla identity rows");
        for (int i = 0; i < tExpected.length; i++) {
            assertEquals(tExpected[i], GT6WoodDict.VANILLA_ROWS.get(i).id(), "vanilla row " + i);
        }
        assertEquals(MT.WOODS.Oak, GT6WoodDict.VANILLA_ROWS.get(0).material(), "the oak identity (:51)");
    }

    @Test
    public void everyGt6RowResolvesAPlaceableBlockItem() {
        // THE placement pin: every GT6 row resolves an Item that IS a BlockItem — planks place.
        // The id686 guard (the GT6JuicerRegistrationTest form): the RegistryObject handles are
        // bound only in a live mod-runtime JVM, so the .get() half runs guarded; the offline
        // leg pins the single-path identity (the BlockItem registration reuses the block id
        // path, GT6TreeBlocks.registerItems) and the vanilla half resolves unguarded.
        for (GT6WoodDict.PlankEntry tRow : GT6WoodDict.GT6_ROWS) {
            //? if forge {
            net.minecraft.resources.ResourceLocation tLoc = new net.minecraft.resources.ResourceLocation("gt6", tRow.id());
            //?} else {
            /*net.minecraft.resources.ResourceLocation tLoc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tRow.id());
             *///?}
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(tLoc)) {
                Item tItem = tRow.plank().get();
                assertTrue(tItem instanceof BlockItem, tRow.id() + " resolves a BlockItem (placeable, the symptom32 face)");
                assertTrue(((BlockItem) tItem).getBlock() instanceof gregtech6.block.tree.GT6PlankBlock,
                        tRow.id() + " places a GT6PlankBlock cube");
                assertSame(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(tLoc), tItem,
                        tRow.id() + " is the registered item face");
            } else {
                assertTrue(isRegistrationPath(tRow.id()), tRow.id() + " is a GT6TreeBlocks registration path (the offline identity leg)");
            }
        }
        for (GT6WoodDict.PlankEntry tRow : GT6WoodDict.VANILLA_ROWS) {
            assertTrue(tRow.plank().get() instanceof BlockItem, tRow.id() + " resolves a placeable vanilla plank");
        }
    }

    /** Whether a wooddict row id is one of the 17 GT6TreeBlocks plank registration paths (RegistryObject.getId() is unbind-safe). */
    private static boolean isRegistrationPath(String aId) {
        for (RegistryObject<Item> tHandle : GT6TreeBlocks.PLANK_ITEMS) {
            if (tHandle.getId().getPath().equals(aId)) return true;
        }
        for (RegistryObject<Item> tHandle : GT6TreeBlocks.GENERIC_PLANK_ITEMS) {
            if (tHandle.getId().getPath().equals(aId)) return true;
        }
        return false;
    }

    @Test
    public void theResolverAnswersTheConvergedFaces() {
        // the identity material face (the sawing/unboxinator output id — unchanged across the convergence)
        assertEquals("plank_wood", GT6WoodDict.GT6_ROWS.get(9).id(), "gt6:plank_wood is the generic plank id");
        // the bound-face asserts ride the id686 guard (the RegistryObject handles are live only
        // in a mod-runtime JVM — the GT6JuicerRegistrationTest posture)
        if (plankWoodBound()) {
            assertTrue(GT6WoodDict.plankOrNull(MT.Wood) instanceof BlockItem, "the IL.Plank material resolves the placeable cube");
            assertTrue(GT6WoodDict.plankOrNull(MT.WOODS.Rainbowood) instanceof BlockItem, "the species face (the BlockTreePlanks.java:57 carrier)");
            assertTrue(GT6WoodDict.plankOrNull(MT.WoodTreated) instanceof BlockItem, "the bath treated-leg output (meta 10)");
        }
        assertEquals(Items.OAK_PLANKS, GT6WoodDict.plankOrNull(MT.WOODS.Oak), "the oak identity (the oak-twin ruling face)");
        assertNull(GT6WoodDict.plankOrNull(MT.WoodPolished), "no WoodPolished plank face upstream — the bath polished leg skips");
        assertNull(GT6WoodDict.plankOrNull(MT.Iron), "non-wood materials have no plank face");
        assertFalse(GT6WoodDict.hasPlank(MT.WoodPolished), "hasPlank agrees with the resolver");
    }

    /** Whether the mod-runtime JVM bound the plank item face (the id686 guard probe). */
    private static boolean plankWoodBound() {
        //? if forge {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(new net.minecraft.resources.ResourceLocation("gt6", "plank_wood"));
        //?} else {
        /*return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "plank_wood"));
         *///?}
    }

    @Test
    public void theWooddictRowsAreDistinctIdsOverDistinctHandles() {
        Set<String> tIds = new HashSet<>();
        for (GT6WoodDict.PlankEntry tRow : GT6WoodDict.ROWS) {
            assertTrue(tIds.add(tRow.id()), "distinct plank id: " + tRow.id());
        }
        assertEquals(GT6WoodDict.ROWS.size(), tIds.size(), "no id duality across the walk face");
    }

    @Test
    public void theRetiredItemDomainIsTheWoodFamilyMinusTheConvergedRows() {
        // audit: the WOOD-gated domain (the old 130) minus the materials that keep a plank face
        // (the 17 GT6 rows over 16 materials + the 11 vanilla identities) is the compat cut
        Set<OreDictMaterial> tWithFace = new HashSet<>();
        for (GT6WoodDict.PlankEntry tRow : GT6WoodDict.ROWS) tWithFace.add(tRow.material());
        Set<String> tCut = new TreeSet<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
            if (tMaterial == null || tMaterial.mID < 0) continue;
            if (tMaterial.contains(gregapi.data.TD.Properties.WOOD) && !tWithFace.contains(tMaterial)) {
                tCut.add(tMaterial.mNameInternal);
            }
        }
        // the mod woods (Greatwood/Silverwood/Steeleaf/... — upstream mod-loaded rows) plus the
        // port-absent grades (WoodPolished/Varnished/Bleached/Tainted/Scorched — no plank block
        // in the BlockTreePlanks face); Wood itself KEEPS its face (the meta 9 row)
        assertTrue(tCut.contains("Greatwood") && tCut.contains("Silverwood"), "the mod woods are the compat cut");
        assertTrue(tCut.contains("WoodPolished"), "the grade woods without a BlockTreePlanks row are cut");
        assertFalse(tCut.contains("Wood"), "the identity wood keeps its face (the meta 9 row)");
        assertEquals(130 - 27, tCut.size(), "130 WOOD materials - 27 faced materials = the cut census (re-pin on material drift)");
    }
}
