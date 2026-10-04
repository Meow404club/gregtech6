/**
 * Tests for task worldgen-edge-ores-b1-vein-axis (the B1 card of the
 * research.stonelayer-edge-ores route B): the LARGE-VEIN TABLE AXIS EXTENSION.
 *
 * <p>Root cause under test (the research ruling): 11 materials have NO natural source in
 * the port because their large-vein SLOTS die on the registration-axis validity gate
 * (GT6VeinGenerator.valid — the modern mID&gt;0 face, GT6OreBlocks.materialAxis()). The
 * upstream compensation rows are Loader_Worldgen.java:889-911, already ported verbatim
 * into GT6WorldgenDatagen.LARGE_VEIN_TABLE (+ the deep mirrors) — extending the axis
 * revives them with ZERO row edits (the a-ore-axis-extension B-plan declare,
 * GT6WorldgenDatagen "these rows start generating with zero edits here").
 *
 * <p>The 11 primary materials (the player-experience face: Li/Ti/Zr/I chain entries):
 * Lazurite/Sodalite (:889 lapis), KIO3 (:891 iodinesalt), Lepidolite/Spodumene (:892
 * rocksalt), Talc (:893 asbestos), Kyanite/Glauconite (:902 peridot), TiO2/Zircon/
 * Ilmenite (:911 titanium). The CONNECTING revival (the whole-dead veins — all four
 * slots outside the axis today = the row never draws, GT6VeinGenerator.valid
 * :109-110): bauxite (:890), monazite (:898), quartz (:901), molybdenum (:905) — their
 * slot materials join the axis in the same verbatim rows; titanium (:911) is the fifth
 * whole-dead row and rides the primary set.
 *
 * <p>mdh-5 interlink: the extension consciously reopens the atlas-PRIMARY static state
 * for exactly TWO materials — Zircon (TROPIC, atlas :2526) and Nd (HBM, atlas :2403) —
 * GT6AxisTakeoverCensusTest pins the amended faces.
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only
 * (the GT6LargeVeinTest posture).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.GT6VeinGenerator;

class GT6VeinAxisExtensionTest {

    /** The 22 new axis members, LARGE_VEIN_ORES order = upstream :889-911 first-appearance order (mNameInternal). */
    private static final List<String> NEW_AXIS_MEMBERS = List.of(
        "Lazurite", "Sodalite",                                             // :889 lapis top/bottom
        "Bauxite", "Ilmenite",                                              // :890 bauxite top/spread
        "IodineSalt",                                                       // :891 iodinesalt top (MT.KIO3)
        "Lepidolite", "Spodumene",                                          // :892 rocksalt between/spread
        "Talc",                                                             // :893 asbestos bottom
        "Bastnasite", "Monazite", "Neodymium",                              // :898 monazite top/between/spread
        "MilkyQuartz", "Barite", "CertusQuartz",                            // :901 quartz top/bottom/between+spread
        "Kyanite", "Glauconite",                                            // :902 peridot top/spread
        "Wulfenite", "Molybdenite", "Molybdenum", "Powellite",              // :905 molybdenum top/bottom/between/spread
        "Rutile", "Zircon");                                                // :911 titanium top+bottom/between (MT.TiO2)

    /** The axis M after the extension: 122 (53 worldgen + 13 stone-layer + 56 gem-pool) + 22 large-vein. */
    private static final int PINNED_M = 144;

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The material for an internal name (the pin's spelling oracle — a wrong name names itself). */
    private static OreDictMaterial mat(String aInternalName) {
        OreDictMaterial rMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.get(aInternalName);
        assertTrue(rMaterial != null && rMaterial.mID > 0, "material must exist: " + aInternalName);
        return rMaterial;
    }

    /** The one vein row by name (LARGE_VEIN_TABLE). */
    private static GTVeinConfig vein(String aName) {
        for (GTVeinConfig tVein : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            if (tVein.name().equals(aName)) return tVein;
        }
        return null;
    }

    /** The one deep mirror row by tail. */
    private static GTVeinConfig deepVein(String aTail) {
        for (GTVeinConfig tVein : GT6WorldgenDatagen.DEEP_VEIN_TABLE) {
            if (tVein.name().equals("ore.large.deep." + aTail)) return tVein;
        }
        return null;
    }

    /**
     * THE verbatim row pins (upstream :889-911, slot-by-slot): every one of the 11
     * materials sits in exactly the upstream slot(s), surface AND deep mirror — and
     * after the extension every pinned slot passes the validity gate (the zero-edit
     * revival face). A drift in any slot names its upstream line.
     */
    @Test
    void elevenMaterialsSitInTheirUpstreamSlots_andRevive() {
        // :889 lapis — Lazurite top / Sodalite bottom (+ deep mirror :1513)
        assertEquals(mat("Lazurite"), vein("ore.large.lapis").oreTop(), ":889 lapis top");
        assertEquals(mat("Sodalite"), vein("ore.large.lapis").oreBottom(), ":889 lapis bottom");
        assertEquals(mat("Lazurite"), deepVein("lapis").oreTop(), "deep :889 top");
        assertEquals(mat("Sodalite"), deepVein("lapis").oreBottom(), "deep :889 bottom");
        // :890 bauxite — Ilmenite spread
        assertEquals(mat("Ilmenite"), vein("ore.large.bauxite").oreSpread(), ":890 bauxite spread");
        // :891 iodinesalt — KIO3 top
        assertEquals(mat("IodineSalt"), vein("ore.large.iodinesalt").oreTop(), ":891 iodinesalt top (MT.KIO3)");
        // :892 rocksalt — Lepidolite between / Spodumene spread
        assertEquals(mat("Lepidolite"), vein("ore.large.rocksalt").oreBetween(), ":892 rocksalt between");
        assertEquals(mat("Spodumene"), vein("ore.large.rocksalt").oreSpread(), ":892 rocksalt spread");
        // :893 asbestos — Talc bottom
        assertEquals(mat("Talc"), vein("ore.large.asbestos").oreBottom(), ":893 asbestos bottom");
        // :902 peridot — Kyanite top / Glauconite spread (+ deep mirror :1521)
        assertEquals(mat("Kyanite"), vein("ore.large.peridot").oreTop(), ":902 peridot top");
        assertEquals(mat("Glauconite"), vein("ore.large.peridot").oreSpread(), ":902 peridot spread");
        assertEquals(mat("Kyanite"), deepVein("peridot").oreTop(), "deep :902 top");
        assertEquals(mat("Glauconite"), deepVein("peridot").oreSpread(), "deep :902 spread");
        // :911 titanium — TiO2 top+bottom / Zircon between / Ilmenite spread (+ deep mirror :1528)
        assertEquals(mat("Rutile"), vein("ore.large.titanium").oreTop(), ":911 titanium top (MT.TiO2)");
        assertEquals(mat("Rutile"), vein("ore.large.titanium").oreBottom(), ":911 titanium bottom");
        assertEquals(mat("Zircon"), vein("ore.large.titanium").oreBetween(), ":911 titanium between");
        assertEquals(mat("Ilmenite"), vein("ore.large.titanium").oreSpread(), ":911 titanium spread");
        assertEquals(mat("Rutile"), deepVein("titanium").oreTop(), "deep :911 top");
        assertEquals(mat("Zircon"), deepVein("titanium").oreBetween(), "deep :911 between");
        assertEquals(mat("Ilmenite"), deepVein("titanium").oreSpread(), "deep :911 spread");

        // THE revival face: every pinned slot passes the gate (zero row edits — the gate
        // itself moved). This is the assertion that is RED before the axis extension.
        // The asbestos row rides it with THREE of four slots: its Gypsum between slot
        // (:893) stays OUTSIDE the axis by the B1 ruling — the row already drew (Chromite/
        // Asbestos on the axis) and Gypsum's only revival faces are the mod-gated small-ore
        // pool (upstream :872 MD.IHL), another card's territory. The boundary pin:
        assertTrue(!GT6VeinGenerator.valid(mat("Gypsum")),
                "the B1 boundary: Gypsum stays outside the axis (the asbestos row draws via Chromite/Talc/Asbestos)");
        for (GTVeinConfig tVein : List.of(
                vein("ore.large.lapis"), vein("ore.large.iodinesalt"), vein("ore.large.rocksalt"),
                vein("ore.large.peridot"), vein("ore.large.titanium"),
                vein("ore.large.bauxite"), vein("ore.large.monazite"), vein("ore.large.quartz"),
                vein("ore.large.molybdenum"),
                deepVein("lapis"), deepVein("peridot"), deepVein("titanium"))) {
            for (OreDictMaterial tSlot : List.of(tVein.oreTop(), tVein.oreBottom(), tVein.oreBetween(), tVein.oreSpread())) {
                assertTrue(GT6VeinGenerator.valid(tSlot),
                        tVein.name() + " slot " + tSlot.mNameInternal + " must pass the axis gate after the extension");
            }
        }
        // the asbestos row's post-extension valid face (Chromite/Asbestos pre-existing, Talc the B1 member)
        assertTrue(GT6VeinGenerator.valid(vein("ore.large.asbestos").oreBottom()), ":893 Talc passes");
        assertTrue(GT6VeinGenerator.valid(vein("ore.large.asbestos").oreTop()), "Chromite pre-existing");
        assertTrue(GT6VeinGenerator.valid(vein("ore.large.asbestos").oreSpread()), "Asbestos pre-existing");

        // and each of the 11 rides >= 1 drawable overworld row (a natural source)
        List<String> tDrawable = drawableNames(false);
        for (String[] tPair : new String[][] {
                {"Lazurite", "ore.large.lapis"}, {"Sodalite", "ore.large.lapis"},
                {"IodineSalt", "ore.large.iodinesalt"}, {"Lepidolite", "ore.large.rocksalt"},
                {"Spodumene", "ore.large.rocksalt"}, {"Talc", "ore.large.asbestos"},
                {"Kyanite", "ore.large.peridot"}, {"Glauconite", "ore.large.peridot"},
                {"Ilmenite", "ore.large.bauxite"}, {"Rutile", "ore.large.titanium"},
                {"Zircon", "ore.large.titanium"}}) {
            assertTrue(tDrawable.contains(tPair[1]),
                    tPair[0] + " revival row " + tPair[1] + " must be drawable overworld");
        }
    }

    /**
     * The connecting revival (the whole-dead rows): bauxite/quartz/monazite/molybdenum
     * carried ALL FOUR slots outside the axis (4 slots out = the row never draws, the
     * GT6VeinGenerator.valid :109-110 face) — after the extension each row draws with
     * its verbatim slots, overworld weights enter the draw mass, and molybdenum joins
     * the END drawable set (4 rows to 5, the GT6VeinGenerator javadoc's axis-extension face).
     */
    @Test
    void fourWholeVeinsReviveWithVerbatimSlots() {
        // verbatim slots + the upstream weights (the draw-mass arithmetic input)
        assertEquals(mat("Bauxite"), vein("ore.large.bauxite").oreTop(), ":890 top");
        assertEquals(mat("Ilmenite"), vein("ore.large.bauxite").oreSpread(), ":890 spread");
        assertEquals(80, vein("ore.large.bauxite").weight(), ":890 weight");
        assertEquals(mat("Bastnasite"), vein("ore.large.monazite").oreTop(), ":898 top");
        assertEquals(mat("Monazite"), vein("ore.large.monazite").oreBetween(), ":898 between");
        assertEquals(mat("Neodymium"), vein("ore.large.monazite").oreSpread(), ":898 spread");
        assertEquals(30, vein("ore.large.monazite").weight(), ":898 weight");
        assertEquals(mat("MilkyQuartz"), vein("ore.large.quartz").oreTop(), ":901 top");
        assertEquals(mat("Barite"), vein("ore.large.quartz").oreBottom(), ":901 bottom");
        assertEquals(mat("CertusQuartz"), vein("ore.large.quartz").oreBetween(), ":901 between");
        assertEquals(mat("CertusQuartz"), vein("ore.large.quartz").oreSpread(), ":901 spread");
        assertEquals(60, vein("ore.large.quartz").weight(), ":901 weight");
        assertEquals(mat("Wulfenite"), vein("ore.large.molybdenum").oreTop(), ":905 top");
        assertEquals(mat("Molybdenite"), vein("ore.large.molybdenum").oreBottom(), ":905 bottom");
        assertEquals(mat("Molybdenum"), vein("ore.large.molybdenum").oreBetween(), ":905 between");
        assertEquals(mat("Powellite"), vein("ore.large.molybdenum").oreSpread(), ":905 spread");
        assertEquals(5, vein("ore.large.molybdenum").weight(), ":905 weight");

        // all four rows drawable overworld; titanium (the fifth whole-dead row) rides the primary set
        List<String> tDrawable = drawableNames(false);
        for (String tName : List.of("ore.large.bauxite", "ore.large.monazite", "ore.large.quartz",
                "ore.large.molybdenum", "ore.large.titanium")) {
            assertTrue(tDrawable.contains(tName), tName + " must be drawable overworld after the extension");
        }

        // the draw mass: 1360 (the 25-row pre-state) + 80 bauxite + 30 monazite + 60 quartz
        // + 5 molybdenum + 40 titanium = 1575
        int tWeightSum = 0;
        for (GTVeinConfig tVein : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            if (!tVein.overworld()) continue;
            if (tVein.oreTop() != null && GT6VeinGenerator.valid(tVein.oreTop())
                    || tVein.oreBottom() != null && GT6VeinGenerator.valid(tVein.oreBottom())
                    || tVein.oreBetween() != null && GT6VeinGenerator.valid(tVein.oreBetween())
                    || tVein.oreSpread() != null && GT6VeinGenerator.valid(tVein.oreSpread())) {
                tWeightSum += tVein.weight();
            }
        }
        assertEquals(1575, tWeightSum, "the overworld draw mass after the extension");

        // the END drawable set grows 4 -> 5 (molybdenum joins platinum/cassiterite/naquadah/trinium)
        List<String> tEndDrawable = drawableNames(true);
        assertEquals(List.of("ore.large.platinum", "ore.large.molybdenum", "ore.large.cassiterite",
                "ore.large.naquadah", "ore.large.trinium"), tEndDrawable,
                "the END draw list after the extension (the :904-919 ORE_END rows, 4 -> 5)");
    }

    /** The axis census: M = 144 = 122 + 22, the 22 appended in upstream :889-911 first-appearance order after the 122. */
    @Test
    void axisGrowsByThe22LargeVeinMembers() {
        List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
        assertEquals(PINNED_M, tAxis.size(), "M = 122 (53+13+56) + 22 large-vein members");
        List<String> tNames = tAxis.stream().map(m -> m.mNameInternal).toList();
        List<String> tTail = tNames.subList(122, tNames.size());
        assertEquals(NEW_AXIS_MEMBERS, tTail,
                "the 22 new members appended in LARGE_VEIN_ORES order = upstream :889-911 first appearance");
        for (String tName : NEW_AXIS_MEMBERS) {
            assertTrue(tNames.contains(tName), "axis member: " + tName);
        }
    }

    /** The drawable row names of one dimension face (the GT6VeinGenerator.valid :109-110 walk). */
    private static List<String> drawableNames(boolean aEndRows) {
        List<String> rNames = new ArrayList<>();
        for (GTVeinConfig tVein : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            if (aEndRows ? !tVein.end() : !tVein.overworld()) continue;
            if (!GT6VeinGenerator.valid(tVein.oreTop()) && !GT6VeinGenerator.valid(tVein.oreBottom())
                    && !GT6VeinGenerator.valid(tVein.oreBetween()) && !GT6VeinGenerator.valid(tVein.oreSpread())) continue;
            rNames.add(tVein.name());
        }
        return rNames;
    }
}
