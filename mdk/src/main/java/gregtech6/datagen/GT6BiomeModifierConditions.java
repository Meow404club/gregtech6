package gregtech6.datagen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

/**
 * The both-legs loader-conditions emission pass (task p31-nether-lens-end-yield,
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
 *
 * <p>THE ROW: exactly {@link GT6WorldgenDatagen#END_YIELD_MODIFIER_KEY} — the End
 * large-vein biome modifier carrying the {@code not(mod_loaded <planet mod>)} yield
 * inversion ({@link GT6WorldgenDatagen#PLANET_VEIN_TRIGGER_MODID}; the condition types
 * are limited to mod_loaded/item_exists by the card spec, both loaders evaluate them
 * TAGS_INVALID — tag conditions would throw). Everything else the native provider
 * emits stays untouched.
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
     * <p>Package-shared (task p32-ops-biome-keyorder) because the brand mirror rides the
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

    @Override
    public CompletableFuture<?> run(CachedOutput aCache) {
        Path tData = mOutput.getOutputFolder(PackOutput.Target.DATA_PACK);
        // THIS leg's brand directory — the class is registered on both legs and the
        // brand mirror duplicates the row onto the sibling directory afterwards
        String tBrand = GT6WorldgenDatagen.biomeModifierRegistryKey().location().getNamespace();
        Path tFile = tData.resolve("gt6").resolve(tBrand).resolve("biome_modifier")
                .resolve(GT6WorldgenDatagen.END_YIELD_MODIFIER_KEY.location().getPath() + ".json");
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
            String tKey = tBrand + ":conditions";
            if (tRoot.has(tKey)) {
                tRoot.remove(tKey); // the re-run face: rebuild from the carrier alone
            }
            tRoot.add(tKey, conditionsArray(tBrand));
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
            return CompletableFuture.completedFuture(null);
        } catch (IOException tError) {
            throw new RuntimeException("the conditions injection failed processing " + tFile, tError);
        }
    }

    /**
     * The ONE biome-modifier serializer face (task p32-ops-biome-keyorder): the byte shape
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

    @Override
    public String getName() {
        return "GT6 Biome Modifier Conditions (the forge-leg forge:conditions emission)";
    }
}
