package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6WorldgenDatagen;

/**
 * The per-material ore-generation distribution model (task debt-ore-gen-data) — the DATA
 * layer of the ore-gen info page, pure aggregation over the three worldgen row tables,
 * zero JEI/EMI/client dependency (the display card M consumes this API; it never reads
 * the tables itself).
 *
 * <p><b>Entry unit = material</b> (the research-card aggregation ruling): the ore-block
 * universe is 53 materials x 26 families (GT6OreBlocks.java:441) and every host variant
 * of a material shares the same distribution (the reskin happens at placement,
 * WD.setOre), so the page aggregates on the material. Keys are the ALIAS-RESOLVED
 * materials — the {@link MaterialRegistry} registration-target chain, the
 * GT6OreBlocks.materialAxis identity — so e.g. the two Cinnabar small-ore rows
 * (:828 redcinnabar + :851 cinnabar) land on ONE entry.
 *
 * <p><b>Source tables</b> (frozen constants, read-only here):
 * <ul>
 * <li>small ores — {@link GTOreWorldgen#ROWS} 54 rows / 91 (row, dim) pairs, consumed
 * through {@link GTOreWorldgen#placementPairs()} which IS the generating face (the
 * placement-gated ancientdebris row and the dim-less modded-only rows drop out);</li>
 * <li>large veins — {@link GT6WorldgenDatagen#LARGE_VEIN_TABLE} 40 rows, the four
 * material slots walked top/bottom/between/spread, a material emitting ONE face per
 * (vein row, dim) no matter how many slots carry it (the naquadah row's four Nq slots
 * are one vein);</li>
 * <li>bedrock ores — {@link GT6WorldgenDatagen#BEDROCK_ORE_TABLE} 46 rows.</li>
 * </ul>
 *
 * <p><b>The generating-face rule</b> (why some table materials have NO entry): rows that
 * never place in a vanilla dimension stay out — vein rows with neither overworld nor end
 * (the 7 offworld rows :917-925, table-census data until dim cards exist) and bedrock
 * rows with {@code overworld == false} (the 13 offworld rows :758-770). The axis is the
 * UNION of the three generating faces — 118 materials at the 2026-09-26 table state,
 * pinned (with the per-table subcounts) by OreDistributionInfoTest.
 *
 * <p><b>Large veins are their own column</b> (coordinator ruling ③): {@link Entry#veins}
 * never merges into {@link Entry#smallOres} — the display card renders them separately.
 *
 * <p><b>DISPLAY SEMANTICS — the amount/probability口径 (read before showing numbers to
 * players):</b> the model carries the UPSTREAM table values verbatim. A small-ore
 * {@code amount} is the upstream per-chunk attempt count semantics (the
 * WorldgenOresSmall.java:61 range, mean ~0.75*amount) — but THIS port places at
 * {@link GTOreWorldgen#ORE_SIZE} = 4, whose vein mean is 1.5 blocks, so the ACTUAL
 * placed density is ~1.5x upstream (the GTOreWorldgen.java:220 calibration note). A
 * bedrock {@code probability} P is the 1/P per-chunk roll, one INDEPENDENT roll per row
 * in table order (two rows CAN hit one chunk — the gold.a/gold.b pair is two 1/32000
 * rolls and both are kept); the ore body sits at the bedrock floor
 * (GT6BedrockOreGenerator: BEDROCK_Y -64 patch + muffin to -58, sparse small-ore tails
 * up to TAIL_TOP_Y 62). Info pages must present these as the upstream semantics with
 * the ORE_SIZE caveat, not as live block-count promises.
 *
 * <p>Immutable, deterministic (first-appearance order: small rows, then vein rows, then
 * bedrock rows; within a small row the {@link GTOreWorldgen.Dim} declaration order),
 * built once at class init — the GT6WorldgenDatagen posture (the vein/bedrock tables
 * reference MT constants directly, so first touch must follow material init, like every
 * other consumer of those tables).
 */
public final class OreDistributionInfo {

    /** One small-ore face: a {@link GTOreWorldgen.Placement} pair's dim + the row's Y band and amount (upstream semantics, see the class javadoc). */
    public record SmallOre(GTOreWorldgen.Dim dim, String name, int minY, int maxY, int amount) {}

    /** One large-vein face: the material occurs in this vein (any of the four slots) in this dim. */
    public record Vein(GTOreWorldgen.Dim dim, String name, int minY, int maxY, int weight, int size) {}

    /** One bedrock-ore face: an independent 1/{@code probability} per-chunk roll (overworld rows only — see the class javadoc). */
    public record BedrockOre(String name, int probability) {}

    /** One material's combined faces — at least one of the three lists is non-empty by construction. */
    public record Entry(OreDictMaterial material, List<SmallOre> smallOres, List<Vein> veins, List<BedrockOre> bedrockOres) {}

    private static final List<Entry> ENTRIES = build();

    private static final Map<OreDictMaterial, Entry> INDEX = buildIndex(ENTRIES);

    /** All entries, first-appearance order (small rows, then veins, then bedrock). */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** The entry of a material (alias-resolved), or null when it has no generating face. */
    public static Entry of(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mID <= 0) return null;
        return INDEX.get(MaterialRegistry.INSTANCE.get(aMaterial));
    }

    // ---------------------------------------------------------------- build

    /** The one-shot aggregation — the three-table walk, each row order-preserving. */
    private static List<Entry> build() {
        Map<OreDictMaterial, Faces> tFaces = new LinkedHashMap<>();

        // 1. small ores — placementPairs() is already the generating face (gated + dim-less rows out)
        for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
            OreDictMaterial tMaterial = GTOreWorldgen.resolve(tPair.row());
            if (tMaterial == null || tMaterial.mID <= 0) continue;
            faces(tFaces, tMaterial).smallOres.add(new SmallOre(tPair.dim(), tPair.row().name(),
                    tPair.row().minY(), tPair.row().maxY(), tPair.row().amount()));
        }

        // 2. large veins — the generating rows only (overworld || end), one face per (row, dim, material)
        for (GTVeinConfig tVein : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            if (!tVein.overworld() && !tVein.end()) continue;
            Set<OreDictMaterial> tRowSeen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (OreDictMaterial tSlot : new OreDictMaterial[] {tVein.oreTop(), tVein.oreBottom(), tVein.oreBetween(), tVein.oreSpread()}) {
                OreDictMaterial tMaterial = resolve(tSlot);
                if (tMaterial == null || !tRowSeen.add(tMaterial)) continue;
                for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
                    if (tDim == GTOreWorldgen.Dim.OVERWORLD ? tVein.overworld() : tDim == GTOreWorldgen.Dim.END && tVein.end()) {
                        faces(tFaces, tMaterial).veins.add(new Vein(tDim, tVein.name(),
                                tVein.minY(), tVein.maxY(), tVein.weight(), tVein.size()));
                    }
                }
            }
        }

        // 3. bedrock ores — the overworld (GEN_FLOOR) rows, both gold rows kept (independent rolls)
        for (GTBedrockOreConfig tRow : GT6WorldgenDatagen.BEDROCK_ORE_TABLE) {
            if (!tRow.overworld()) continue;
            OreDictMaterial tMaterial = resolve(tRow.material());
            if (tMaterial == null) continue;
            faces(tFaces, tMaterial).bedrockOres.add(new BedrockOre(tRow.name(), tRow.probability()));
        }

        List<Entry> rEntries = new ArrayList<>(tFaces.size());
        for (Map.Entry<OreDictMaterial, Faces> tFace : tFaces.entrySet()) {
            Faces tFaces2 = tFace.getValue();
            rEntries.add(new Entry(tFace.getKey(), List.copyOf(tFaces2.smallOres), List.copyOf(tFaces2.veins), List.copyOf(tFaces2.bedrockOres)));
        }
        return List.copyOf(rEntries);
    }

    private static Map<OreDictMaterial, Entry> buildIndex(List<Entry> aEntries) {
        Map<OreDictMaterial, Entry> rIndex = new LinkedHashMap<>();
        for (Entry tEntry : aEntries) rIndex.put(tEntry.material(), tEntry);
        return Collections.unmodifiableMap(rIndex);
    }

    private static Faces faces(Map<OreDictMaterial, Faces> aFaces, OreDictMaterial aMaterial) {
        return aFaces.computeIfAbsent(aMaterial, aKey -> new Faces());
    }

    /** The alias-chain resolve of a direct table slot (null when the slot is invalid — the upstream mID &lt;= 0 semantics). */
    private static OreDictMaterial resolve(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mID <= 0) return null;
        return MaterialRegistry.INSTANCE.get(aMaterial);
    }

    /** The mutable build accumulator (the GT6VeinGenerator.axis laziness is not needed — one shot at class init). */
    private static final class Faces {
        final List<SmallOre> smallOres = new ArrayList<>();
        final List<Vein> veins = new ArrayList<>();
        final List<BedrockOre> bedrockOres = new ArrayList<>();
    }
}
