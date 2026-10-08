package gregtech6.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import gregapi.oredict.OreDictMaterial;
import gregapi.data.MT;

/**
 * The port wooddict plank face (task planks-blockification) — the semantic surface of the
 * upstream {@code gregapi.wooddict.PlankEntry} family (WoodDictionary.PLANKS/LIST_PLANKS,
 * gregapi/load/LoaderWoodDictionary.java), scoped to the plank leg the port recipe walks
 * consume. Upstream registers every plank the running mod set provides: the 6 vanilla rows
 * (:45-56), the 17 GT6 block rows (:66-172 — the 9 species rows :69-113, the standalone
 * generic rows :157-172, the IL.Plank DEFAULT_PLANK :66, the Crate :161) and one row per
 * loaded mod wood. The port world has no fourth-party mods, so the face is the fixed
 * 17 GT6 rows + the vanilla identities (the 1.7.10 six verbatim, extended by the 1.20.1
 * vanilla tree set — Mangrove/Cherry/Bamboo/Crimson/Warped ride the same identity-mapping
 * rule the oak-twin sawing rows established).
 *
 * <p>This is the ONE plank authority: the retired wood-planks-register prefix-item family
 * (130 flat gt6:plank_* items, a port-authority invention over the whole WOOD material
 * domain) folded into these rows — every plank is a placeable BlockItem (or the vanilla
 * plank item itself), matching the upstream ship shape (planks exist ONLY as blocks
 * upstream; Loader_Items carries zero plank rows).
 *
 * <p>The wooddict slab/stair/beam legs (PlankEntry.mSlab/mStair, WoodEntry/BeamEntry/
 * SaplingEntry) stay upstream-only — the port cards consume the plank leg only
 * (the beam face has its own GT6BeamBlocks domain; the slab/stair family is unbuilt).
 */
public final class GT6WoodDict {

    /**
     * One plank row. {@code id} is the registry id path of the plank item
     * ({@code gt6:...} or {@code minecraft:...}) — the census/pin face; {@code plank}
     * is a lazy handle (RegistryObject-backed for the GT6 rows, the vanilla constant for
     * the identities) so the plain-JVM test universe can walk the rows without dragging
     * the mod registry or the vanilla bootstrap in.
     *
     * <p>The material arm is a lazy handle TOO (task ore-loot-census-reconciliation, the
     * {@link GT6RegistryStaticInitGuardTest} root fix): this class loads at MOD CONSTRUCT
     * (the {@code GT6PlankPanels} @EventBusSubscriber reach), before {@code MT.init()} — an
     * eager {@code OreDictMaterial} component made the clinit GETSTATIC-read
     * {@code MT.WoodRubber/Wood/WoodTreated/Bamboo} (+ the fifteen {@code MT.WOODS.*} rows the
     * {@code MT$WOODS} owner hides from the scan) and froze whatever instance was live at
     * clinit — a NULL on a pre-init load, a stale one across the test JVMs' re-inits. The
     * supplier is the sanctioned lazy shape (the synthetic {@code lambda$} body is invisible
     * to the clinit scan, the guard's own control pin) and {@link #material()} resolves
     * call-time — post-init callers see the live instance, zero behavior change.
     */
    public record PlankEntry(Supplier<OreDictMaterial> materialSupply, String id, Supplier<Item> plank) {

        /** The call-time material resolve — see the record javadoc for the laziness ruling. */
        public OreDictMaterial material() {
            return this.materialSupply.get();
        }
    }

    /**
     * The 17 GT6 rows in upstream meta order — BlockTreePlanks meta 0-15 then BlockTreePlanks2
     * meta 0 (LoaderWoodDictionary.java:66-172 verbatim materials). The two MT.Wood rows are
     * upstream's own duality: meta 9 is the generic IL.Plank face (:66 DEFAULT_PLANK) and
     * meta 11 the Crate (:161) — one material, two distinct blocks.
     */
    public static final List<PlankEntry> GT6_ROWS = List.of(
        new PlankEntry(() -> MT.WoodRubber,        "rubber_planks",           () -> GT6TreeBlocks.PLANK_ITEMS.get(0).get()),       // :69
        new PlankEntry(() -> MT.WOODS.Maple,       "maple_planks",            () -> GT6TreeBlocks.PLANK_ITEMS.get(1).get()),       // :71
        new PlankEntry(() -> MT.WOODS.Willow,      "willow_planks",           () -> GT6TreeBlocks.PLANK_ITEMS.get(2).get()),       // :73
        new PlankEntry(() -> MT.WOODS.BlueMahoe,   "blue_mahoe_planks",       () -> GT6TreeBlocks.PLANK_ITEMS.get(3).get()),       // :75
        new PlankEntry(() -> MT.WOODS.Hazel,       "hazel_planks",            () -> GT6TreeBlocks.PLANK_ITEMS.get(4).get()),       // :91
        new PlankEntry(() -> MT.WOODS.Cinnamon,    "cinnamon_planks",         () -> GT6TreeBlocks.PLANK_ITEMS.get(5).get()),       // :93
        new PlankEntry(() -> MT.WOODS.Coconut,     "coconut_planks",          () -> GT6TreeBlocks.PLANK_ITEMS.get(6).get()),       // :95
        new PlankEntry(() -> MT.WOODS.Rainbowood,  "rainbowood_planks",       () -> GT6TreeBlocks.PLANK_ITEMS.get(7).get()),       // :97
        new PlankEntry(() -> MT.WOODS.Compressed,  "plank_wood_compressed",   () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(0).get()), // :157
        new PlankEntry(() -> MT.Wood,              "plank_wood",              () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(1).get()), // :66 DEFAULT_PLANK (meta 9)
        new PlankEntry(() -> MT.WoodTreated,       "plank_wood_treated",      () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(2).get()), // :159
        new PlankEntry(() -> MT.Wood,              "crate",                   () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(3).get()), // :161 (meta 11)
        new PlankEntry(() -> MT.WOODS.Dead,        "plank_wood_dead",         () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(4).get()), // :165
        new PlankEntry(() -> MT.WOODS.Rotten,      "plank_wood_rotten",       () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(5).get()), // :167
        new PlankEntry(() -> MT.WOODS.Mossy,       "plank_wood_mossy",        () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(6).get()), // :169
        new PlankEntry(() -> MT.WOODS.Frozen,      "plank_wood_frozen",       () -> GT6TreeBlocks.GENERIC_PLANK_ITEMS.get(7).get()), // :171
        new PlankEntry(() -> MT.WOODS.BlueSpruce,  "blue_spruce_planks",      () -> GT6TreeBlocks.PLANK_ITEMS.get(8).get()));      // :113 (Planks2:0)

    /**
     * The vanilla identities (LoaderWoodDictionary.java:45-56): the 1.7.10 six verbatim
     * (:51-56) plus the 1.20.1 vanilla tree set under the same identity rule — the port
     * material for each vanilla wood maps to the vanilla plank block itself, no GT6 cube.
     */
    public static final List<PlankEntry> VANILLA_ROWS = List.of(
        new PlankEntry(() -> MT.WOODS.Oak,      "minecraft:oak_planks",      () -> Items.OAK_PLANKS),      // :51
        new PlankEntry(() -> MT.WOODS.Spruce,   "minecraft:spruce_planks",   () -> Items.SPRUCE_PLANKS),   // :52
        new PlankEntry(() -> MT.WOODS.Birch,    "minecraft:birch_planks",    () -> Items.BIRCH_PLANKS),    // :53
        new PlankEntry(() -> MT.WOODS.Jungle,   "minecraft:jungle_planks",   () -> Items.JUNGLE_PLANKS),   // :54
        new PlankEntry(() -> MT.WOODS.Acacia,   "minecraft:acacia_planks",   () -> Items.ACACIA_PLANKS),   // :55
        new PlankEntry(() -> MT.WOODS.DarkOak,  "minecraft:dark_oak_planks", () -> Items.DARK_OAK_PLANKS), // :56
        new PlankEntry(() -> MT.WOODS.Mangrove, "minecraft:mangrove_planks", () -> Items.MANGROVE_PLANKS), // the 1.20.1 tree set
        new PlankEntry(() -> MT.WOODS.Cherry,   "minecraft:cherry_planks",   () -> Items.CHERRY_PLANKS),
        new PlankEntry(() -> MT.Bamboo,         "minecraft:bamboo_planks",   () -> Items.BAMBOO_PLANKS),
        new PlankEntry(() -> MT.WOODS.Crimson,  "minecraft:crimson_planks",  () -> Items.CRIMSON_PLANKS),
        new PlankEntry(() -> MT.WOODS.Warped,   "minecraft:warped_planks",   () -> Items.WARPED_PLANKS));

    /** The full walk face (the upstream LIST_PLANKS shape): every plank row the port world provides. */
    public static final List<PlankEntry> ROWS = concat(GT6_ROWS, VANILLA_ROWS);

    /**
     * The plank item of one material, or null when the material has no plank face (the
     * silent-skip guard). The match is mID-based: the alias-merged registry row can be a
     * different instance than the {@code MT.*} constant the row resolves (the registration
     * id is the stable key).
     *
     * <p>The resolve is the SILENT-SKIP face verbatim: an unbound RegistryObject (the
     * isolated plain-JVM test universe where the tree DeferredRegister never fired) reads
     * as "no plank face" (null), not a crash — the GT6RecipesBath ladder skip-counts it.
     */
    public static Item plankOrNull(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mID < 0) return null;
        for (PlankEntry tRow : ROWS) {
            if (tRow.material() != null && tRow.material().mID == aMaterial.mID) {
                try {
                    return tRow.plank().get();
                } catch (NullPointerException | IllegalStateException e) {
                    return null; // the offline-unbound face — RegistryObject.get() when the mod registration never ran
                }
            }
        }
        return null;
    }

    /** Whether the material carries a plank face (the walk gate, resolution-free, the same mID key). */
    public static boolean hasPlank(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mID < 0) return false;
        for (PlankEntry tRow : ROWS) {
            if (tRow.material() != null && tRow.material().mID == aMaterial.mID) return true;
        }
        return false;
    }

    private static List<PlankEntry> concat(List<PlankEntry> aLeft, List<PlankEntry> aRight) {
        List<PlankEntry> rRows = new java.util.ArrayList<>(aLeft.size() + aRight.size());
        rRows.addAll(aLeft);
        rRows.addAll(aRight);
        return List.copyOf(rRows);
    }

    private GT6WoodDict() {}
}
