package gregtech6.datagen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import gregtech6.worldgen.GTOreWorldgen;
import gregtech6.worldgen.GT6Worldgen;

/**
 * The both-legs loader-conditions emission pass (task nether-lens-end-yield,
 * the research.p31-nether-conditional-api asymmetry face): forge 1.20.1 RUNTIME gates
 * datapack-registry JSONs on the {@code forge:conditions} root key
 * (ICondition.java:24-30 {@code shouldRegisterEntry}, the RegistryDataLoader patch's
 * generic load loop — biome modifiers ride the same loop as vanilla worldgen,
 * ForgeMod.java:434-436 dataPackRegistry → DataPackRegistriesHooks), but the forge
 * 1.20.1 {@code DatapackBuiltinEntriesProvider} has NO conditions ctor — and the neo 1.21.1
 * native conditions map serializes in INSERTION order while every saveStable
 * re-serialization normalizes to the KEY_COMPARATOR order, so the asymmetric
 * native-emission shape made the two trees differ in member ORDER and failed the
 * datagen_tree_check byte gate. This provider is the project-owned extension that
 * closes the gap SYMMETRICALLY (the research verdict's "各腿文件内嵌自家品牌条件键沿
 * 既有 mirror pass 发射即可"): registered on BOTH legs right after the worldgen
 * provider, it reads the freshly emitted {@code data/gt6/<brand>/biome_modifier/<row>
 * .json} and rewrites it with the row's conditions under THIS leg's brand key; the
 * brand mirror rebrands it onto the sibling directory (byte-identical modulo brand).
 * <p>THE REGISTRY (task twilight-adaptation-pilot — the single-row injector generalized
 * into the condition-row registry, the mod-dimension adaptation skeleton's detection
 * face): {@link #CONDITION_ROWS} maps each conditioned biome-modifier row path to its
 * brand-keyed conditions builder. Entry order is registration-stable; every unlisted
 * row the native provider emits stays untouched (the backward-compat ratchet, pinned by
 * GT6NetherWorldgenTest). Condition TYPES are limited to mod_loaded/item_exists by the
 * card spec (both loaders evaluate them TAGS_INVALID — tag conditions would throw); the
 * registry's FIRST entry is the historical End large-vein row (the
 * {@code not(mod_loaded <planet mod>)} yield inversion,
 * {@link GT6WorldgenDatagen#PLANET_VEIN_TRIGGER_MODID}) — its emitted bytes are
 * unchanged by the generalization (the committed-tree byte gate).
 *
 * <p>Idempotent under the HashCache bookkeeping (the GT6DualDirectoryFaces doctrine):
 * run N's injection content is run N+1's native provider's cached no-op, the file keeps
 * the conditions; the rebuild-then-saveStable form makes a re-run byte-neutral.
 * Fail-visible: a missing native row or an unparseable JSON throws — no silent skip.
 */
public class GT6BiomeModifierConditions implements DataProvider {

    /**
     * THE CANONICAL KEY ORDER, hardcoded to the 1.20.1 DataProvider.KEY_COMPARATOR
     * semantics (FIXED_ORDER_FIELDS type:0/parent:1, then alphabetical — 1.20.1
     * DataProvider.java:23-27). DELIBERATELY NOT DataProvider.KEY_COMPARATOR: the 1.21.1
     * comparator pins {@code neoforge:conditions} FIRST (1.21.1 DataProvider.java:31-38
     * "Neo: conditions go first"), so the leg-dependent comparator would serialize the
     * same row into two DIFFERENT member orders and break the datagen_tree_check byte
     * gate. This fixed order is what the 1.20.1 saveStable (the canonical producer's
     * mirror) emits, so every brand twin lands byte-identical modulo the brand strings.
     *
     * <p>Package-shared (task ops-biome-keyorder) because the brand mirror rides the
     * SAME face: on the 21.1 leg the leg saveStable re-pinned {@code neoforge:conditions}
     * to the head of the re-emitted row while this provider's injection kept it at the
     * alphabetical slot — two canonical forms fighting over one file, the provider own
     * cache shouldWrite (HashCache.java:155-157) blind to the sibling's bytes = the
     * odd/even run oscillation. One row, ONE serializer.
     */
    static final java.util.Comparator<String> CANONICAL_KEY_ORDER = new java.util.Comparator<String>() {
        private final java.util.Map<String, Integer> FIXED = java.util.Map.of("type", 0, "parent", 1);

        @Override
        public int compare(String aLeft, String aRight) {
            int tDelta = FIXED.getOrDefault(aLeft, 2) - FIXED.getOrDefault(aRight, 2);
            return tDelta != 0 ? tDelta : aLeft.compareTo(aRight);
        }
    };

    private final PackOutput mOutput;

    public GT6BiomeModifierConditions(PackOutput aOutput) {
        mOutput = aOutput;
    }

    /**
     * One conditioned biome-modifier row: the JSON path under
     * {@code data/gt6/<brand>/biome_modifier/} (no {@code .json}) and the brand-keyed
     * conditions builder. The row is the registry's unit — a future mod-dimension card
     * appends one entry and the emission, brand mirror and tree gate follow with zero
     * further edits here.
     */
    public record ConditionRow(String rowPath, java.util.function.Function<String, JsonArray> conditions) {}

    /**
     * The condition-row registry (the class javadoc's skeleton face): path → conditions
     * builder, in emission order. The single-condition positive form for a mod-dimension
     * mount is {@link #modLoadedConditions}; composite faces (the yield inversion's
     * {@code not} wrapper) bring their own builder.
     */
    public static final List<ConditionRow> CONDITION_ROWS = List.of(
            new ConditionRow(GT6WorldgenDatagen.END_YIELD_MODIFIER_KEY.location().getPath(),
                    GT6BiomeModifierConditions::conditionsArray),
            // task twilight-adaptation-pilot — the SECOND row and the positive form's first
            // tenant: the twilight_ores modifier mounts only WITH Twilight Forest present
            // (the mod-dimension adaptation skeleton's detection face). TF absent: the
            // entry skips at datapack load before the #twilightforest:in_twilight_forest
            // tag resolves (forge ICondition.java:24-30 shouldRegisterEntry) — zero mounts,
            // zero errors, the unconditioned rows untouched.
            new ConditionRow(GT6WorldgenDatagen.TWILIGHT_ORES_MODIFIER_KEY.location().getPath(),
                    aBrand -> modLoadedConditions(aBrand, GTOreWorldgen.TWILIGHT_MODID)),
            // task twilight-stone-rows — the THIRD row, the twilight_stones modifier (the
            // 17 WorldgenStone twilight rows, Loader_Worldgen.java:657) riding the SAME
            // positive form and the SAME TF-absence semantics as the twilight_ores row.
            new ConditionRow(GT6WorldgenDatagen.TWILIGHT_STONES_MODIFIER_KEY.location().getPath(),
                    aBrand -> modLoadedConditions(aBrand, GTOreWorldgen.TWILIGHT_MODID)),
            // task twilight-vegetation — the FOURTH row, the positive form's second tenant:
            // the twilight_surface_rocks modifier (the twilight.rocks GEN_TWILIGHT row,
            // Loader_Worldgen.java:621) mounts only WITH Twilight Forest present. The tag
            // is our own #gt6:surface_rocks_twilight (TF members optional = empty
            // resolution would no-op anyway); the condition keeps the mod-dimension
            // skeleton's detection face uniform and skips the row at datapack load.
            new ConditionRow(GT6WorldgenDatagen.TWILIGHT_SURFACE_ROCKS_MODIFIER_KEY.location().getPath(),
                    aBrand -> modLoadedConditions(aBrand, GTOreWorldgen.TWILIGHT_MODID)),
            // task twilight-hives-springs — the FIFTH row (rebase union: the twilight_stones
            // and twilight_surface_rocks rows landed first): the twilight_bumble_hives modifier
            // mounts only WITH Twilight Forest present (the twilight_ores row's detection
            // face, the same modid — one registry entry per mod-dimension mount).
            new ConditionRow(GT6WorldgenDatagen.TWILIGHT_HIVES_MODIFIER_KEY.location().getPath(),
                    aBrand -> modLoadedConditions(aBrand, GTOreWorldgen.TWILIGHT_MODID)),
            // task twilight-hives-springs — the SIXTH row (rebase union: the twilight_stones
            // and twilight_surface_rocks rows landed first): the twilight_fluid_springs
            // modifier, the same detection face (TF absent, the :795-796 rows stay dormant
            // census and the tag never resolves).
            new ConditionRow(GT6WorldgenDatagen.TWILIGHT_FLUID_SPRINGS_MODIFIER_KEY.location().getPath(),
                    aBrand -> modLoadedConditions(aBrand, GTOreWorldgen.TWILIGHT_MODID)),
            // task atum-dim-adaptation — the positive form's atum tenants, band by band
            // (the registry's append-only contract): each atum modifier mounts only WITH
            // Atum present. Atum absent: the entry skips at datapack load before the
            // #gt6:atum_biomes tag resolves — the tag is OURS and would resolve EMPTY
            // anyway, the condition is the twilight-form parity plus the explicit
            // dead-mod declaration.
            new ConditionRow(GT6WorldgenDatagen.ATUM_FLUID_SPRINGS_MODIFIER_KEY.location().getPath(),
                    aBrand -> modLoadedConditions(aBrand, GT6Worldgen.ATUM_MODID)));

    @Override
    public CompletableFuture<?> run(CachedOutput aCache) {
        Path tData = mOutput.getOutputFolder(PackOutput.Target.DATA_PACK);
        // THIS leg's brand directory — the class is registered on both legs and the
        // brand mirror duplicates the rows onto the sibling directory afterwards
        String tBrand = GT6WorldgenDatagen.biomeModifierRegistryKey().location().getNamespace();
        for (ConditionRow tRow : CONDITION_ROWS) {
            injectConditions(tData, tBrand, tRow);
        }
        return CompletableFuture.completedFuture(null);
    }

    /** The one-row injection, verbatim mechanics of the pre-registry single-row form. */
    private static void injectConditions(Path aData, String aBrand, ConditionRow aRow) {
        Path tFile = aData.resolve("gt6").resolve(aBrand).resolve("biome_modifier")
                .resolve(aRow.rowPath() + ".json");
        if (!Files.isRegularFile(tFile)) {
            throw new RuntimeException("the conditions injection expects the native worldgen provider to have "
                    + "emitted " + tFile + " earlier in this run (provider order contract, GT6DataGenerators)");
        }
        try {
            byte[] tCurrent = Files.readAllBytes(tFile);
            JsonElement tJson = JsonParser.parseReader(new StringReader(new String(tCurrent, java.nio.charset.StandardCharsets.UTF_8)));
            if (!tJson.isJsonObject()) {
                throw new RuntimeException("the conditions injection expects a JSON object (got " + tJson + ")");
            }
            JsonObject tRoot = tJson.getAsJsonObject();
            String tKey = aBrand + ":conditions";
            if (tRoot.has(tKey)) {
                tRoot.remove(tKey); // the re-run face: rebuild from the carrier alone
            }
            tRoot.add(tKey, aRow.conditions().apply(aBrand));
            // DIRECT content-compare write, NOT saveStable: the path is owned by TWO
            // providers (the native base writer + this one), so saveStable's per-provider
            // shouldWrite skip would leave a sibling's bytes on disk. The bytes replicate
            // DataProvider.saveStable's serializer verbatim — JsonWriter UTF-8,
            // serializeNulls(false), two-space indent, the CANONICAL_KEY_ORDER
            // normalization (fixed "type" first, then alphabetical).
            byte[] tTarget = serializeCanonical(tRoot);
            if (!java.util.Arrays.equals(tTarget, tCurrent)) {
                Files.write(tFile, tTarget);
            }
        } catch (IOException tError) {
            throw new RuntimeException("the conditions injection failed processing " + tFile, tError);
        }
    }

    /**
     * The ONE biome-modifier serializer face (task ops-biome-keyorder): the byte shape
     * of the 1.20.1 {@code DataProvider.saveStable} (JsonWriter UTF-8, serializeNulls(false),
     * two-space indent, GsonHelper.writeValue under {@link #CANONICAL_KEY_ORDER}, recursive
     * — GsonHelper.java:532-562 both legs) with the LEG-COMPARATOR DETOUR REMOVED. The 1.21.1
     * saveStable pins {@code neoforge:conditions} ahead of {@code type} (DataProvider.java
     * :30-38), so any row routed through the leg face carries a DIFFERENT member order than
     * the injection writes here — the flip this class exists to make impossible.
     */
    static byte[] serializeCanonical(JsonElement aJson) throws IOException {
        ByteArrayOutputStream tOut = new ByteArrayOutputStream();
        JsonWriter tWriter = new JsonWriter(new OutputStreamWriter(tOut, StandardCharsets.UTF_8));
        tWriter.setSerializeNulls(false);
        tWriter.setIndent("  ");
        net.minecraft.util.GsonHelper.writeValue(tWriter, aJson, CANONICAL_KEY_ORDER);
        tWriter.close();
        return tOut.toByteArray();
    }

    /**
     * The mirror twin of {@code DataProvider.saveStable} over {@link #serializeCanonical}:
     * same async contract (Util.backgroundExecutor), same HashCache bookkeeping
     * (CachedOutput.writeIfNeeded + the sha1 the cache bookkeeps), the comparator alone is
     * canonical. IOException rides saveStable's log-and-continue parity (a policy change
     * here is NOT this card's business); GT6DualDirectoryFaces.mirrorBiomeModifiers routes
     * EVERY brand re-emission through this, so both brands of a row come out of ONE face
     * byte-identical modulo the brand strings on BOTH legs.
     */
    static CompletableFuture<?> saveCanonical(CachedOutput aCache, JsonElement aJson, Path aTarget) {
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] tBytes = serializeCanonical(aJson);
                aCache.writeIfNeeded(aTarget, tBytes, Hashing.sha1().hashBytes(tBytes));
            } catch (IOException tError) {
                DataProvider.LOGGER.error("Failed to save file to {}", aTarget, tError);
            }
        }, Util.backgroundExecutor());
    }

    /**
     * The yield conditions, the brand-keyed JSON both loaders parse: the
     * {@code not(mod_loaded <planet mod>)} inversion. Shape evidence: forge
     * IConditionSerializer.getJson writes {@code type} (CraftingHelper.getCondition reads
     * it, CraftingHelper.java:228) + ModLoadedCondition.Serializer's {@code modid} +
     * NotCondition.Serializer's {@code value}; the dispatch/encoder orders make
     * {@code type} the first member of every condition object (the byte contract with
     * the neo leg's native emission — GT6DualDirectoryFaces rebrands it 1:1).
     *
     * <p>Division of labor (mdh-4 closeout, KEEP ruling; the registry's multi-row face
     * keeps the rule per-row): this emission is a live runtime mod-presence condition and
     * it stays — datapack conditions evaluate at datapack-load time, before any registry
     * face exists, so they cannot migrate to the registry-side unified driver
     * GT6ModDrivers (mdh series; isLoaded/visibilityGate); that driver owns the
     * registration face only.
     */
    public static JsonArray conditionsArray(String aBrand) {
        JsonObject tModLoaded = new JsonObject();
        tModLoaded.addProperty("type", aBrand + ":mod_loaded");
        tModLoaded.addProperty("modid", GT6WorldgenDatagen.PLANET_VEIN_TRIGGER_MODID);
        JsonObject tNot = new JsonObject();
        tNot.addProperty("type", aBrand + ":not");
        tNot.add("value", tModLoaded);
        JsonArray rList = new JsonArray();
        rList.add(tNot);
        return rList;
    }

    /**
     * The POSITIVE single-condition form (task twilight-adaptation-pilot — the new face
     * the generalization exists for): {@code [mod_loaded <modid>]}, brand-keyed. A mount
     * row built with it applies when the mod IS present and is skipped at datapack load
     * (the RegistryDataLoader debug-level skip) when absent — the TF-absence semantics.
     * Same {@code type}-first member order as {@link #conditionsArray} (the rebrand walk
     * treats it identically, KNOWN_CONDITION_TYPES mod_loaded).
     */
    public static JsonArray modLoadedConditions(String aBrand, String aModid) {
        JsonObject tModLoaded = new JsonObject();
        tModLoaded.addProperty("type", aBrand + ":mod_loaded");
        tModLoaded.addProperty("modid", aModid);
        JsonArray rList = new JsonArray();
        rList.add(tModLoaded);
        return rList;
    }

    @Override
    public String getName() {
        return "GT6 Biome Modifier Conditions (the forge-leg forge:conditions emission)";
    }
}
