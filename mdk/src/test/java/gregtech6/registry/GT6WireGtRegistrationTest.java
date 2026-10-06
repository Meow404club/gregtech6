/**
 * Tests for task wiregt-prefix-item-retirement: the OP.wireGt01-16 prefix items are OFF the
 * item path — the user-ruled upstream-fidelity regression reversal (2026-10-06 field test:
 * the creative screen flooded with sixteen per-multiplier tabs of sprite-borrow wire items
 * plus the rubber wire rows; "这些东西本来就有本来就好的，这是改坏了").
 *
 * <p>Upstream canon (the re-verified archaeology): the sixteen multipliers ship ONLY on the
 * MTE BLOCK path — MultiTileEntityWireElectric.addElectricWires (upstream
 * gregapi/tileentity/connectors/MultiTileEntityWireElectric.java:72-87) lifts every multiplier
 * onto the wire MTE via one OreDictManager.setTarget_ oredient row per material, and :89-93 is
 * the same face for cableGt01/02/04/08/12; upstream Loader_Items.java:57-171 (the item loader)
 * carries ZERO wireGt rows (the grep census). The wireGtXX oredient face therefore rides BLOCK
 * ITEMS — port-side the GTWires family over GTWireSpecs (wire_&lt;mat&gt;_gtNN, the 620-spectrum),
 * and the vanilla item-tag family stays EMPTY for wireGt (the single-identity pin kept from the
 * transformer fold: GT6ElectricTransformers.WIRE_TAG_PATH rides fine_wires/%s = wireFine only).
 *
 * <p>The PREFIX DEFINITIONS stay (OP.java:1364-1382 verbatim — the fold carriers and the
 * wire-family tag face read the prefix rows); only the ITEM face is retired. Task
 * wire-gt-registration was the lift; this file pins its reversal. The GTWires three tabs
 * (electric 622 / redstone 6 / laser 1) are pinned untouched by GTWiresCreativeTabTest and
 * GTWireSpecsCensusTest — this file adds the cross-pin that the block face covers the 1x
 * AnnealedCopper wire (the extruder :748/:781 representative output binding).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.datagen.GT6ItemTags;

public class GT6WireGtRegistrationTest {

    /** The sixteen multipliers, upstream OP.java:309-324 (create order) = the port OP.java:1364-1382 rows. */
    private static final List<String> WIRE_GT_NAMES = List.of(
            "wireGt01", "wireGt02", "wireGt03", "wireGt04", "wireGt05", "wireGt06", "wireGt07", "wireGt08",
            "wireGt09", "wireGt10", "wireGt11", "wireGt12", "wireGt13", "wireGt14", "wireGt15", "wireGt16");

    /** The five cable multipliers upstream binds on the same MTE block path (:89-93) — never itemized port-side. */
    private static final List<String> CABLE_GT_NAMES = List.of("cableGt01", "cableGt02", "cableGt04", "cableGt08", "cableGt12");

    @BeforeAll
    public static void initMaterialSystem() {
        // the GT6ItemTags face class-loads vanilla registries — the GTWireDisplayNameTest
        // boot shape: version detect + offline-expected throwables swallowed
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline-expected
        }
        GTMaterialItems.initMaterials();
    }

    private static List<OreDictPrefix> prefixesNamed(List<String> aNames) {
        return aNames.stream().map(tName -> {
            for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
                if (tName.equals(tPrefix.mNameInternal)) return tPrefix;
            }
            throw new AssertionError("prefix not found: " + tName);
        }).toList();
    }

    /** Census ①: all sixteen prefixes are OFF the item path; the universe is the 110-prefix list (105 upstream + casing quartet + plank). */
    @Test
    public void allSixteenWireGtPrefixesAreOffTheItemPath() {
        List<OreDictPrefix> tPath = GTMaterialItems.itemPathPrefixes();
        for (OreDictPrefix tPrefix : prefixesNamed(WIRE_GT_NAMES)) {
            assertFalse(tPath.contains(tPrefix), tPrefix.mNameInternal + " must be OFF the item path (retired, task wiregt-prefix-item-retirement)");
        }
        assertEquals(110, tPath.size(), "105 upstream PrefixItems + the four casingMachine* additions + plank (the wire lift reverted)");
    }

    /** Census ②: zero wireGt pairs in the live registration walk — 16 x 14 = 224 items gone; cableGt was never itemized. */
    @Test
    public void registrationWalkCarriesZeroWireAndCablePairs() {
        Set<OreDictPrefix> tWire = Set.copyOf(prefixesNamed(WIRE_GT_NAMES));
        Set<OreDictPrefix> tCable = Set.copyOf(prefixesNamed(CABLE_GT_NAMES));
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            assertFalse(tWire.contains(tPair.prefix()), "retired wire item alive: " + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
            assertFalse(tCable.contains(tPair.prefix()), "cableGt was never on the item path: " + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        }
    }

    /** Census ③: no wireGt/cableGt prefix owns a creative tab — the sixteen per-multiplier tabs are gone. */
    @Test
    public void noWireOrCablePrefixOwnsACreativeTab() {
        Set<OreDictPrefix> tRetired = new HashSet<>(prefixesNamed(WIRE_GT_NAMES));
        tRetired.addAll(prefixesNamed(CABLE_GT_NAMES));
        for (OreDictPrefix tPrefix : GTMaterialItems.tabPrefixes()) {
            assertFalse(tRetired.contains(tPrefix), tPrefix.mNameInternal + " must own no tab (the items are retired; the wire tabs are the GTWires trio)");
        }
    }

    /** The prefix definitions stay — the tag face and the transformer fold read the OP rows. */
    @Test
    public void theSixteenPrefixDefinitionsStay() {
        assertEquals(16, prefixesNamed(WIRE_GT_NAMES).size(), "all sixteen OP rows still defined (OP.java:1364-1382) — only the item face retired");
    }

    /** The fold single-identity pin ①: the wireGt prefixes own NO material tag family. */
    @Test
    public void wireGtPrefixesOwnNoMaterialTagFamily() {
        for (OreDictPrefix tPrefix : prefixesNamed(WIRE_GT_NAMES)) {
            assertNull(GT6ItemTags.itemTagFamily(tPrefix),
                    tPrefix.mNameInternal + " must stay off the tag-family whitelist (single identity: the fine_wires fold carrier stays alone)");
        }
    }

    /**
     * The fold single-identity pin ②: wireFine keeps fine_wires and its Copper carrier stays
     * registered (the GT6ElectricTransformers.WIRE_TAG_PATH fold carrier, the
     * GT6ElectricTransformerBlockEntityTest precondition).
     */
    @Test
    public void fineWiresTagStaysExclusivelyTheWireFineFace() {
        assertEquals(GT6ItemTags.FINE_WIRES_FAMILY, GT6ItemTags.itemTagFamily(OP.wireFine),
                "the transformer fold carrier keeps its family (GT6ElectricTransformers.WIRE_TAG_PATH face)");
        boolean tCarrier = false;
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.wireFine && tPair.material() == gregapi.data.MT.Copper) tCarrier = true;
        }
        assertTrue(tCarrier,
                "wireFine(Copper) — the transformer fold carrier — stays registered (the GT6ElectricTransformerBlockEntityTest precondition)");
    }

    /**
     * The tag-face carrier cross-pin: the retired oredient face rides the GTWires BLOCK items —
     * the 1x AnnealedCopper wire block item exists (the extruder :748/:781 representative output
     * binding, wire_&lt;mat&gt;_gtNN = GTWireSpecs.registryName), the upstream
     * OreDictManager.setTarget_ isomorph.
     */
    @Test
    public void theWireBlockFaceCoversTheRetiredOredictFace() {
        TreeSet<String> tNames = new TreeSet<>();
        for (String tName : GTWires.FAMILY_BY_NAME.keySet()) tNames.add(tName);
        assertTrue(tNames.contains("wire_annealed_copper_gt01"),
                "the 1x AnnealedCopper wire block item must carry the retired wireGt01 oredient face (GTWireSpecs row Loader:1919)");
        assertEquals(621, tNames.size(), "the electric-family block spectrum is untouched + the wire_laser command key (the GTWireSpecsCensusTest pins the 620 variant face; the laser block keys into FAMILY_BY_NAME for /gt6wire place)");
    }
}
