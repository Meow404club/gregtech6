/**
 * Census and representative-row assertions for the electric-wire data table (task
 * p9-wire-family-w1 spec ⑦). The table ({@link GTWireSpecs}) is the direct transcription of
 * upstream addElectricWires (MultiTileEntityWireElectric.java:71-109) and the 30-material
 * registration loop (Loader_MultiTileEntities.java:1914-1950); this test pins the 620 count
 * (28 cable rows x 21 + 2 pure-wire rows x 16), the CS.java:148-154 voltage table, one fully
 * decomposed representative row per voltage tier band, the 16-wire/5-cable shape ladders and
 * the selector/name invariants. An independent recount walks the rows again (the
 * GTMaterialItemsRegistrationTest "independent recount" discipline).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTWireSpecs.Row;
import gregtech6.registry.GTWireSpecs.Variant;

public class GTWireSpecsCensusTest {

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    /** The CS.java:148-154 V table, transcribed independently for the cross-check. */
    private static final long[] UPSTREAM_V = {
            8, 32, 128, 512, 2048, 8192, 32768, 131072,
            524288, 2097152, 8388608, 33554432, 134217728, 536870912, 2147483648L, 8589934592L};

    @Test
    public void censusIsExactly620() {
        List<Variant> tVariants = GTWireSpecs.variants();
        assertEquals(620, tVariants.size(), "28 cable rows x 21 + 2 pure-wire rows x 16 = 620 (the ADR scale ruling)");
        // independent recount, from the rows alone
        int tRecount = 0;
        for (Row tRow : GTWireSpecs.ROWS) tRecount += tRow.cable() ? 16 + 5 : 16;
        assertEquals(620, tRecount);
        assertEquals(GTWireSpecs.EXPECTED_VARIANTS, tVariants.size());
        assertEquals(30, GTWireSpecs.ROWS.size(), "Loader:1914-1950 registers exactly 30 material rows");
        assertEquals(28, GTWireSpecs.ROWS.stream().filter(Row::cable).count(), "Graphene (:1948) and Superconductor (:1950) are the only pure-wire rows");
        // the derived list preserves the row-major upstream order: 21 variants per cable row, 16 per pure-wire row
        assertEquals(21, tVariants.stream().filter(v -> v.row() == GTWireSpecs.ROWS.get(0)).count());
        assertEquals(16, tVariants.stream().filter(v -> v.row() == GTWireSpecs.ROWS.get(28)).count());
    }

    @Test
    public void voltageTableMatchesCS148to154() {
        assertEquals(16, GTWireSpecs.V.length);
        for (int i = 0; i < 16; i++) assertEquals(UPSTREAM_V[i], GTWireSpecs.V[i], "V[" + i + "] must equal CS.java:148-154");
        assertEquals(8, GTWireSpecs.V[0]);
        assertEquals(8589934592L, GTWireSpecs.V[15]);
    }

    /** Helper: the bare-wire variant of a row at size n. */
    private static Variant wire(Row aRow, int aSize) {
        return GTWireSpecs.variants().stream().filter(v -> v.row() == aRow && !v.insulated() && v.size() == aSize).findFirst().orElse(null);
    }

    /** Helper: the insulated-cable variant of a row at size n. */
    private static Variant cable(Row aRow, int aSize) {
        return GTWireSpecs.variants().stream().filter(v -> v.row() == aRow && v.insulated() && v.size() == aSize).findFirst().orElse(null);
    }

    @Test
    public void representativeRowsAreDirectTranslations() {
        // Sn, Loader:1914 — V[1]*1 = 32 EU, 1 A base, lossWire 2 / lossCable 1
        Row tSn = GTWireSpecs.ROWS.get(0);
        assertEquals(32, tSn.voltage());
        assertEquals(1, tSn.amperage());
        assertEquals(2, tSn.lossWire());
        assertEquals(1, tSn.lossCable());
        assertTrue(tSn.cable());
        assertEquals(32, wire(tSn, 1).voltage());
        assertEquals(1, wire(tSn, 1).amperage());
        assertEquals(2, wire(tSn, 1).loss());
        // Cu, Loader:1918 — V[2]*2 = 256 EU
        assertEquals(256, GTWireSpecs.ROWS.get(3).voltage());
        // Kanthal, Loader:1922 — V[3] = 512 EU, 4 A, 4/3 loss
        Row tKanthal = GTWireSpecs.ROWS.get(6);
        assertEquals(512, tKanthal.voltage());
        assertEquals(4, tKanthal.amperage());
        assertEquals(4, tKanthal.lossWire());
        assertEquals(3, tKanthal.lossCable());
        // W, Loader:1934 — V[4]*3 = 6144 EU, 8 A, 3/2 loss; wireGt16 carries 8*16 = 128 A
        Row tW = GTWireSpecs.ROWS.get(17);
        assertEquals(6144, tW.voltage());
        assertEquals(8, tW.amperage());
        assertEquals(3, tW.lossWire());
        assertEquals(2, tW.lossCable());
        assertEquals(128, wire(tW, 16).amperage());
        assertEquals(6144, wire(tW, 16).voltage());
        // Superconductor, Loader:1950 — V[15] = 8589934592 EU, 4 A, 1/1 loss, pure wire (16 variants only)
        Row tSuper = GTWireSpecs.ROWS.get(29);
        assertEquals(8589934592L, tSuper.voltage());
        assertEquals(4, tSuper.amperage());
        assertEquals(1, tSuper.lossWire());
        assertEquals(1, tSuper.lossCable());
        assertTrue(!tSuper.cable());
        assertNull(cable(tSuper, 1), "Superconductor registers no cables");
        assertNull(cable(GTWireSpecs.ROWS.get(28), 1), "Graphene (:1948) registers no cables");
        // the pure-wire rows also carry contactDamageWire=false (upstream F,F,F)
        assertTrue(!GTWireSpecs.ROWS.get(28).contactDamageWire() && !GTWireSpecs.ROWS.get(29).contactDamageWire());
        assertTrue(GTWireSpecs.ROWS.get(0).contactDamageWire() && !GTWireSpecs.ROWS.get(0).contactDamageCable(), "metal rows: T, F (upstream :1914-1946)");
    }

    @Test
    public void wireAndCableShapeLaddersMatchAddElectricWires() {
        Row tSn = GTWireSpecs.ROWS.get(0);
        // bare wires :72-87 — diameters PX_P[2,3,4,6,7,7,8,8,9,10,11,12,13,14,15,16], stack 64/n, amp *n
        int[] tDiameters = {2, 3, 4, 6, 7, 7, 8, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        for (int tSize = 1; tSize <= 16; tSize++) {
            Variant tWire = wire(tSn, tSize);
            assertNotNull(tWire, "wireGt" + (tSize < 10 ? "0" : "") + tSize);
            assertEquals(tDiameters[tSize - 1], tWire.diameter(), "PX_P diameter of wireGt" + tSize);
            assertEquals(64 / tSize, tWire.maxStack(), "max stack 64/" + tSize);
            assertEquals(tSn.amperage() * tSize, tWire.amperage(), "bandwidth a*n at size " + tSize);
            assertEquals(tSn.lossWire(), tWire.loss());
            assertTrue(!tWire.insulated());
        }
        // insulated cables :89-93 — sizes 1/2/4/8/12, stacks 64/32/16/8/4 (upstream literals), diameters 4/6/8/12/16
        int[] tSizes = {1, 2, 4, 8, 12};
        int[] tStacks = {64, 32, 16, 8, 4};
        int[] tCableDiameters = {4, 6, 8, 12, 16};
        for (int i = 0; i < tSizes.length; i++) {
            Variant tCable = cable(tSn, tSizes[i]);
            assertNotNull(tCable, "cableGt" + (tSizes[i] < 10 ? "0" : "") + tSizes[i]);
            assertEquals(tStacks[i], tCable.maxStack(), "the upstream literal stack of cableGt" + tSizes[i]);
            assertEquals(tCableDiameters[i], tCable.diameter());
            assertEquals(tSn.amperage() * tSizes[i], tCable.amperage());
            assertEquals(tSn.lossCable(), tCable.loss(), "cables burn aLossCable, not aLossWire");
            assertTrue(tCable.insulated());
            assertEquals(tSn.voltage(), tCable.voltage());
        }
        // there is no cableGt03/05..07 style row
        assertNull(cable(tSn, 3));
        assertNull(cable(tSn, 16));
    }

    @Test
    public void tokensMatchMaterialsAndNamesAreUnique() {
        Set<String> tNames = new HashSet<>();
        List<Variant> tVariants = GTWireSpecs.variants();
        for (Variant tVariant : tVariants) {
            String tName = GTWireSpecs.registryName(tVariant);
            assertTrue(tNames.add(tName), "registry name must be unique: " + tName);
            // the stored token must equal the live snake-cased material internal name (drift lock)
            OreDictMaterial tMaterial = tVariant.row().material().get();
            assertNotNull(tMaterial);
            assertEquals(GTMaterialItems.snakeCase(tMaterial.mNameInternal), GTWireSpecs.materialToken(tVariant.row()), "token drift on " + tName);
            assertTrue(tMaterial.mNameLocal != null && !tMaterial.mNameLocal.isBlank(), "display-name source missing for " + tName);
        }
        assertEquals(620, tNames.size());
        // name forms
        assertTrue(tNames.contains("wire_tin_gt01"));
        assertTrue(tNames.contains("wire_tungsten_gt16"));
        assertTrue(tNames.contains("cable_tin_gt12"));
        assertTrue(tNames.contains("wire_superconductor_gt01"));
        assertTrue(!tNames.contains("cable_superconductor_gt01"));
    }

    @Test
    public void selectorFindsByMaterialSizeForm() {
        assertEquals(32, GTWireSpecs.find("tin", 1, false).voltage());
        assertEquals(4, GTWireSpecs.find("tin", 12, true).maxStack(), "12x Tin Cable stacks to the upstream literal 4");
        assertEquals("cable_tungsten_gt08", GTWireSpecs.registryName(GTWireSpecs.find("tungsten", 8, true)));
        assertEquals(8589934592L, GTWireSpecs.find("superconductor", 4, false).voltage());
        assertEquals(GTWireSpecs.find("Tin", 4, false).voltage(), GTWireSpecs.find("tin", 4, false).voltage(), "token match is case-insensitive");
        assertNull(GTWireSpecs.find("unobtainium", 1, false));
        assertNull(GTWireSpecs.find("tin", 3, true), "no cableGt03");
        assertNull(GTWireSpecs.find("superconductor", 1, true), "pure-wire row has no cable form");
    }

    @Test
    public void displayNamesComposeTheWireTemplates() {
        // task p20-i18n-compose-wires: the pre-installed full strings ("1x Tin Wire" &co)
        // became the GTWireBlock.displayNameOf composition — "%sx %s%s" over (size,
        // material small unit, form unit). The REGISTRY names above stay untouched; the
        // en template wording lives in the provider, here the slot structure is the pin.
        TranslatableContents tName = contents(GTWireBlock.displayNameOf(GTWireSpecs.find("tin", 1, false)));
        assertEquals(GTWireBlock.DISPLAY_KEY, tName.getKey());
        assertEquals(3, tName.getArgs().length, "size + material + form slots");
        assertEquals(1, ((Integer)tName.getArgs()[0]).intValue(), "slot 0 = the size numeral");
        assertArg(tName.getArgs()[1], "gt6.material.tin");
        assertArg(tName.getArgs()[2], GTWireBlock.FORM_WIRE_KEY);
        // the insulated form flips the form slot; the size slot carries the multiplier
        TranslatableContents tCable = contents(GTWireBlock.displayNameOf(GTWireSpecs.find("tin", 12, true)));
        assertEquals(GTWireBlock.DISPLAY_KEY, tCable.getKey());
        assertEquals(12, ((Integer)tCable.getArgs()[0]).intValue());
        assertArg(tCable.getArgs()[2], GTWireBlock.FORM_CABLE_KEY);
        // the pure-wire rows compose like any electric row
        assertEquals(GTWireBlock.DISPLAY_KEY, contents(GTWireBlock.displayNameOf(GTWireSpecs.find("superconductor", 1, false))).getKey());
        assertArg(contents(GTWireBlock.displayNameOf(GTWireSpecs.find("tungsten", 8, false))).getArgs()[1], "gt6.material.tungsten");
    }

    /** The compose seam: asserts translatable shape and returns the contents (key + args). */
    private static TranslatableContents contents(Component aComponent) {
        assertTrue(aComponent.getContents() instanceof TranslatableContents, "the wire display must be a translatable composition");
        return (TranslatableContents)aComponent.getContents();
    }

    /** The material/form slot: the arg must itself be the translatable small unit with the given key. */
    private static void assertArg(Object aArg, String aKey) {
        assertTrue(aArg instanceof Component, "the compose slots are nested translatables");
        assertEquals(aKey, contents((Component)aArg).getKey());
    }

    // -------------------------------------------------------------------------
    // the redstone family (task p10-wire-redstone-family, Loader:1893-1902)
    // -------------------------------------------------------------------------

    /** The upstream losses as the raw long divisions (MAX_RANGE = Integer.MAX_VALUE, ITileEntityRedstoneWire :32). */
    private static final long MAX_RANGE = Integer.MAX_VALUE;

    @Test
    public void redstoneFamilyCensus() {
        assertEquals(3, GTWireSpecs.REDSTONE_ROWS.size(), "Loader:1893-1902 registers exactly 3 redstone material rows");
        List<Variant> tVariants = GTWireSpecs.redstoneVariants();
        assertEquals(6, tVariants.size(), "3 rows x 2 forms (wire + cable), NO size ladder upstream");
        assertEquals(GTWireSpecs.EXPECTED_REDSTONE_VARIANTS, tVariants.size());
        // the electric census stays EXACTLY the 620 — the redstone family is a separate pipeline
        assertEquals(620, GTWireSpecs.variants().size());
        assertEquals(30, GTWireSpecs.ROWS.size());
        // every redstone row carries both forms, no contact damage, amperage 1 (upstream :80)
        for (Row tRow : GTWireSpecs.REDSTONE_ROWS) {
            assertTrue(tRow.cable(), "redstone rows register the cable form too");
            assertTrue(!tRow.contactDamageWire() && !tRow.contactDamageCable(), "no CONTACTDAMAGE on Loader:1893-1902 (spec 7: no shock, no burn)");
            assertEquals(1, tRow.amperage(), "the upstream PIPE_STATS_BANDWIDTH literal (Insulated :80)");
            assertEquals(GTWireSpecs.Row.Family.REDSTONE, tRow.family());
        }
    }

    @Test
    public void redstoneFamilyRowsAreDirectTranslations() {
        // RedAlloy, Loader:1893-1895 — loss MAX_RANGE/16, not luminous
        Row tRedAlloy = GTWireSpecs.REDSTONE_ROWS.get(0);
        assertEquals("red_alloy", tRedAlloy.token());
        assertEquals(MAX_RANGE / 16, tRedAlloy.lossWire());
        assertEquals(MAX_RANGE / 16, tRedAlloy.lossCable());
        assertTrue(!tRedAlloy.luminous());
        // Signalum, Loader:1896-1898 — loss MAX_RANGE/64 (the 4x range per strength point)
        Row tSignalum = GTWireSpecs.REDSTONE_ROWS.get(1);
        assertEquals("signalum", tSignalum.token());
        assertEquals(MAX_RANGE / 64, tSignalum.lossWire());
        assertEquals(MAX_RANGE / 64, tSignalum.lossCable());
        assertTrue(!tSignalum.luminous());
        // Lumium, Loader:1899-1901 — loss MAX_RANGE/16, the GLOWING material (MT.java:1792)
        Row tLumium = GTWireSpecs.REDSTONE_ROWS.get(2);
        assertEquals("lumium", tLumium.token());
        assertEquals(MAX_RANGE / 16, tLumium.lossWire());
        assertTrue(tLumium.luminous(), "Lumium carries GLOWING (mIsGlowing) — the wirelamp data pin");
        // no voltage: the EU face family is not mounted on redstone wires
        assertEquals(0, tRedAlloy.voltage());
        assertEquals(134217727L, MAX_RANGE / 16, "Integer.MAX_VALUE/16 integer division");
        assertEquals(33554431L, MAX_RANGE / 64, "Integer.MAX_VALUE/64 integer division");
    }

    @Test
    public void redstoneVariantsShapesAndNames() {
        // wire = PX_P[2] diameter, cable = PX_P[4]; both maxStack 64 (one form, no ladder)
        for (Variant tVariant : GTWireSpecs.redstoneVariants()) {
            assertEquals(1, tVariant.size());
            assertEquals(tVariant.insulated() ? 4 : 2, tVariant.diameter(), "PX_P[2]/PX_P[4] (Loader:1893-1902 NBT_DIAMETER)");
            assertEquals(64, tVariant.maxStack());
            assertEquals(0, tVariant.voltage());
        }
        // registry names: no _gt tail (no size ladder upstream)
        assertEquals("wire_red_alloy", GTWireSpecs.registryName(GTWireSpecs.findRedstone("red_alloy", false)));
        assertEquals("cable_red_alloy", GTWireSpecs.registryName(GTWireSpecs.findRedstone("red_alloy", true)));
        assertEquals("wire_signalum", GTWireSpecs.registryName(GTWireSpecs.findRedstone("signalum", false)));
        assertEquals("cable_lumium", GTWireSpecs.registryName(GTWireSpecs.findRedstone("lumium", true)));
        // display: the size-less PLAIN template (upstream shows no multiplier on the
        // family); the bare Lumium wire takes the WIRELAMP form unit (Loader:1900)
        // — task p20-i18n-compose-wires
        TranslatableContents tWire = contents(GTWireBlock.displayNameOf(GTWireSpecs.findRedstone("red_alloy", false)));
        assertEquals(GTWireBlock.DISPLAY_PLAIN_KEY, tWire.getKey());
        assertEquals(2, tWire.getArgs().length, "material + form slots, NO size slot");
        assertArg(tWire.getArgs()[0], "gt6.material.red_alloy");
        assertArg(tWire.getArgs()[1], GTWireBlock.FORM_WIRE_KEY);
        TranslatableContents tCable = contents(GTWireBlock.displayNameOf(GTWireSpecs.findRedstone("red_alloy", true)));
        assertEquals(GTWireBlock.DISPLAY_PLAIN_KEY, tCable.getKey());
        assertArg(tCable.getArgs()[1], GTWireBlock.FORM_CABLE_KEY);
        assertArg(contents(GTWireBlock.displayNameOf(GTWireSpecs.findRedstone("lumium", false))).getArgs()[1],
                GTWireBlock.FORM_WIRELAMP_KEY);
        // selector edges
        assertNull(GTWireSpecs.findRedstone("unobtainium", false));
        assertNull(GTWireSpecs.findRedstone("tin", false), "the electric tokens are not redstone rows");
        assertNull(GTWireSpecs.find("red_alloy", 1, false), "the redstone tokens are not electric rows");
    }

    // -------------------------------------------------------------------------
    // the laser family (task p10-wire-laser-placeholder, Loader:1814-1815)
    // -------------------------------------------------------------------------

    @Test
    public void laserFamilyCensus() {
        assertEquals(1, GTWireSpecs.LASER_ROWS.size(), "Loader:1814-1815 registers exactly ONE laser row");
        List<Variant> tVariants = GTWireSpecs.laserVariants();
        assertEquals(1, tVariants.size(), "one form only — the bare fiber wire, NO size ladder, NO cable upstream");
        assertEquals(GTWireSpecs.EXPECTED_LASER_VARIANTS, tVariants.size());
        // the electric and redstone censuses stay EXACTLY theirs — three separate pipelines
        assertEquals(620, GTWireSpecs.variants().size());
        assertEquals(30, GTWireSpecs.ROWS.size());
        assertEquals(3, GTWireSpecs.REDSTONE_ROWS.size());
        assertEquals(6, GTWireSpecs.redstoneVariants().size());
    }

    @Test
    public void laserRowIsDirectTranslation() {
        // Loader:1814-1815 — "Laser Fiber Wire", 24900, NBT_MATERIAL MT.NULL, NBT_DIAMETER
        // PX_P[6], NBT_CONTACTDAMAGE F, maxStack 64; lossless (:114), LU ratings Long.MAX_VALUE
        Row tLaser = GTWireSpecs.LASER_ROWS.get(0);
        assertEquals("laser", tLaser.token(), "the token derives from the TE name gt.multitileentity.connector.wire.laser (:128), not the material");
        assertEquals(GTWireSpecs.Row.Family.LASER, tLaser.family());
        assertEquals(MT.NULL, tLaser.material().get(), "NBT_MATERIAL MT.NULL (Loader:1815) — the row is material-less in spirit");
        assertEquals(0, tLaser.lossWire(), "getEnergyLossPerMeter = 0 (MultiTileEntityWireLaser :114) — the LOSSLESS wire");
        assertTrue(!tLaser.cable(), "a single registration :1815 — no cable form");
        assertTrue(!tLaser.contactDamageWire() && !tLaser.contactDamageCable(), "NBT_CONTACTDAMAGE F (:1815); the laser class mounts no burn machinery at all");
        assertEquals(0, tLaser.voltage(), "the EU face family is NOT mounted on the laser family");
        assertTrue(!tLaser.luminous(), "FIBER_WIRE is a fixed texture, not a glowing material (:121-122)");
        assertEquals(Long.MAX_VALUE, GTWireSpecs.LASER_CAPACITY, "the LU ratings :101-106/:112-113 stay the data pin");
    }

    @Test
    public void laserVariantShapeAndName() {
        Variant tLaser = GTWireSpecs.laserVariants().get(0);
        assertFalse(tLaser.insulated(), "the bare fiber wire only");
        assertEquals(6, tLaser.diameter(), "NBT_DIAMETER PX_P[6] (Loader:1815)");
        assertEquals(64, tLaser.maxStack(), "the :1815 maxStack literal");
        assertEquals(0, tLaser.voltage());
        assertEquals(0, tLaser.loss());
        assertEquals("wire_laser", GTWireSpecs.registryName(tLaser), "no _gt tail — a single id upstream");
        assertNull(GTWireBlock.displayNameOf(tLaser), "the material-less laser stays ATOMIC — no composition applies (:1815 verbatim key)");
        // no collision with the 620 electric names
        Set<String> tElectricNames = new HashSet<>();
        for (Variant tVariant : GTWireSpecs.variants()) tElectricNames.add(GTWireSpecs.registryName(tVariant));
        assertFalse(tElectricNames.contains("wire_laser"));
    }
}
