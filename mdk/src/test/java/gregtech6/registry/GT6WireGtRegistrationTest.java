/**
 * Tests for task wire-gt-registration: the OP.wireGt01-16 prefix-item registration.
 *
 * <p>The sixteen wire multipliers are upstream OP.java:309-324 rows (condition
 * {@code WIRES} = the {@code ITEMGENERATOR.WIRES} material tag, TD.java:564) — upstream
 * ships them on the MTE BLOCK path only (MultiTileEntityWireElectric.java:72-109,
 * {@code setTarget_} over the per-material addElectricWires rows, Loader_MultiTileEntities
 * .java:1914+), so the items domain carries zero wireGt icons. The port registers them on
 * the item path (the third declared item-path deviation after the casingMachine quartet
 * and plank, GTMaterialItems.itemPathPrefixes javadoc) and the per-material gate is the
 * upstream WIRES condition verbatim — zero new gate code, the casing precedent.
 *
 * <p>The material census reconciles the upstream WIRES-condition face: the 13 explicit WIRES
 * rows (upstream MT.java:1301-1306/:1656/:1744-1763, the polymer/sealant band, :1656
 * (AnnealedCopper) and :1744-1763 (the clloy band — every clloy_ row rides SET_COPPER,
 * MT.java:712), byte-for-byte the same rows in the port MT.java (:2149-2155/:2410/:2488-2494/
 * :2510)) plus the Graphene G_MACHINE-expansion leg — 14 materials, the
 * {@link #WIRES_MATERIALS} javadoc carries both faces.
 *
 * <p>The fold single-identity pins (the ⚠️ of the task card): the transformer card folded
 * wireGt01/wireGt04 Cu onto the fine_wires/copper tag (GT6ElectricTransformers.WIRE_TAG_PATH,
 * the GT6ElectricTransformerBlockEntityTest carrier pin) because no wire items existed.
 * With real wireGt items registered, the tag face must stay single-target: the wireGt
 * prefixes own NO material tag family (GT6ItemTags.itemTagFamily whitelist) so a wireGt
 * item can never ride the fine_wires tag, and the upstream wireFine ≢ wireGt relation
 * (different prefixes: U8 fine wire vs U2*n plain wire, OP.java:1265 vs :309-324) is
 * declared on the registration bridge.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.datagen.GT6ItemModels;

public class GT6WireGtRegistrationTest {

    /** The sixteen multipliers, upstream OP.java:309-324 (create order) = the port OP.java:1364-1382 rows. */
    private static final List<String> WIRE_GT_NAMES = List.of(
            "wireGt01", "wireGt02", "wireGt03", "wireGt04", "wireGt05", "wireGt06", "wireGt07", "wireGt08",
            "wireGt09", "wireGt10", "wireGt11", "wireGt12", "wireGt13", "wireGt14", "wireGt15", "wireGt16");

    /**
     * The WIRES-condition material face, measured over the live registration walk. The 13
     * explicit WIRES rows are upstream MT.java:1301-1306/:1656/:1744-1763 (the polymer band,
     * AnnealedCopper, the clloy band — every clloy_ row rides SET_COPPER, MT.java:712),
     * byte-for-byte the same rows in the port MT.java (:2149-2155/:2410/:2488-2494/:2510) —
     * with the internal-name law applied: the :1306 row is
     * {@code create(8199, "Hard Plastic", ..., "Polycarbonate")}, so the registrable member
     * is {@code HardPlastic} and "Polycarbonate" is only its identical-name alias. The
     * fourteenth member, {@code Graphene}, rides the upstream varargs dispatch (OreDictMaterial
     * .put, OreDictMaterial.java:1452-1486): the {@code G_MACHINE} group array (TD.java:610 =
     * {PROJECTILES, PLATES, STICKS, ARMORS, WIRES, FOILS, PARTS}) spreads into per-tag
     * membership, so every G_MACHINE material carries the WIRES tag — Graphene is the one
     * G_MACHINE material of the port array (MT.java:2323), measured not assumed.
     */
    private static final Set<String> WIRES_MATERIALS = Set.of(
            "Rubber", "Plastic", "Teflon", "PVC", "Bakelite", "HardPlastic",
            "AnnealedCopper", "RedAlloy", "BlueAlloy", "PurpleAlloy", "Mingrade", "ElectrotineAlloy", "SolderingAlloy",
            "Graphene");

    @BeforeAll
    public static void initMaterialSystem() {
        // the GT6ItemTags/ItemModels faces class-load vanilla registries — the GTWireDisplayNameTest
        // boot shape: version detect + offline-expected throwables swallowed
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline-expected
        }
        GTMaterialItems.initMaterials();
    }

    private static List<OreDictPrefix> wireGtPrefixes() {
        return WIRE_GT_NAMES.stream().map(tName -> {
            for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
                if (tName.equals(tPrefix.mNameInternal)) return tPrefix;
            }
            throw new AssertionError("prefix not found: " + tName);
        }).toList();
    }

    /** Census ①: all sixteen prefixes sit on the item path (the registration-bridge universe). */
    @Test
    public void allSixteenWireGtPrefixesAreOnTheItemPath() {
        List<OreDictPrefix> tPath = GTMaterialItems.itemPathPrefixes();
        for (OreDictPrefix tPrefix : wireGtPrefixes()) {
            assertTrue(tPath.contains(tPrefix), tPrefix.mNameInternal + " must be on the item path");
        }
        assertEquals(126, tPath.size(), "110 (105 upstream + casing quartet + plank) + the sixteen wire multipliers");
    }

    /** Census ②: the conditioned face per multiplier == the 13 WIRES materials, zero missing, zero extra. */
    @Test
    public void wireItemCensusIsTheUpstreamWiresMaterialFace() {
        for (OreDictPrefix tPrefix : wireGtPrefixes()) {
            Set<String> tActual = new TreeSet<>();
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
                if (tPair.prefix() == tPrefix) tActual.add(tPair.material().mNameInternal);
            }
            assertEquals(WIRES_MATERIALS, tActual, tPrefix.mNameInternal + " item face == the upstream WIRES-condition rows (zero missing)");
            assertEquals(14, tActual.size(), tPrefix.mNameInternal + ": 14 materials x 16 multipliers = 224 wire items");
        }
    }

    /** Census ③: the wire items span exactly three texture sets — the 6-sprite borrow face (assets/README.md). */
    @Test
    public void wireItemsSpanExactlyTheThreeBorrowedTextureSets() {
        Set<String> tSets = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.wireGt01) tSets.add(GT6ItemModels.iconsetOf(tPair.material()));
        }
        assertEquals(Set.of("copper", "dull", "rubber"), tSets,
                "SET_COPPER clloy band + SET_DULL polymers + SET_RUBBER (MT.java:712 clloy_ = SET_COPPER) — the borrow is 3 sets x 2 sprites");
    }

    /** The fold single-identity pin ①: the wireGt prefixes own NO material tag family. */
    @Test
    public void wireGtPrefixesOwnNoMaterialTagFamily() {
        for (OreDictPrefix tPrefix : wireGtPrefixes()) {
            assertNull(GT6ItemTags.itemTagFamily(tPrefix),
                    tPrefix.mNameInternal + " must stay off the tag-family whitelist (single identity: the fine_wires fold carrier stays alone)");
        }
    }

    /** The fold single-identity pin ②: wireFine keeps fine_wires and no registered wireGt id lands on it. */
    @Test
    public void fineWiresTagStaysExclusivelyTheWireFineFace() {
        assertEquals(GT6ItemTags.FINE_WIRES_FAMILY, GT6ItemTags.itemTagFamily(OP.wireFine),
                "the transformer fold carrier keeps its family (GT6ElectricTransformers.WIRE_TAG_PATH face)");
        Set<String> tFineWireIds = new LinkedHashSet<>();
        Set<String> tWireGtIds = new LinkedHashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.wireFine) tFineWireIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
            for (OreDictPrefix tWire : wireGtPrefixes()) {
                if (tPair.prefix() == tWire) tWireGtIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
            }
        }
        for (String tId : tWireGtIds) {
            assertFalse(tFineWireIds.contains(tId), "double identity: " + tId);
        }
        boolean tCarrier = false;
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.wireFine && tPair.material() == gregapi.data.MT.Copper) tCarrier = true;
        }
        assertTrue(tCarrier,
                "wireFine(Copper) — the transformer fold carrier — stays registered (the GT6ElectricTransformerBlockEntityTest precondition)");
    }
}
