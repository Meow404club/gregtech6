/*
 * Offline pinned tests for task p22-painted-item-domain + p25-paint-loot-dotkey-fix: the
 * generated paint-carry loot tables assert against the committed src/generated tree (the
 * GT6MachinePaintRenderDatagenTest read-only split: the write side is gated by runData,
 * first run written>0, second run written:0).
 *
 * <p>Census ground truth: EVERY table generated through the shared
 * {@code GT6LootTables.paintSelfTable} — the p22 painted-item domain (oven 1 +
 * shredder/crusher/lathe T1-T4 12 + dryer 4 + distillery 4 = 21) plus the p24 rows that
 * reuse the same builder (canner 4 + advanced_crafting_table 1) = 26 tables, each carrying
 * ONE copy_nbt function with the TWO paint ops in the SNBT-quoted dot-key form.
 *
 * <p>Task p25-paint-loot-dotkey-fix adds the semantic half: the committed op path strings
 * are driven through the REAL vanilla NbtPathArgument (the loot runtime's own parser, via
 * the CopyNbtFunction.compileNbtPath arm) against synthetic block-entity NBT — the quoted
 * form must carry the paint pair into the item's BlockEntityTag compound, the raw unquoted
 * form must keep missing (the defect pin).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

class GT6MachinePaintItemLootDatagenTest {

    /**
     * The 38 paint-carry bases, in the census order: the p22 painted-item domain
     * ({@code paintableBlockArray}: oven, shredder/crusher/lathe, dryer, distillery) plus
     * the p24 canner rows, the p26 W1 kinetic trio and the ACT controller — every consumer
     * of the shared {@code paintSelfTable} builder, so the pinned shape covers the full
     * regen surface.
     */
    private static final List<String> PAINT_BASES = List.of(
            "oven",
            "shredder", "shredder_t2", "shredder_t3", "shredder_t4",
            "crusher", "crusher_t2", "crusher_t3", "crusher_t4",
            "lathe", "lathe_t2", "lathe_t3", "lathe_t4",
            "dryer", "dryer_t2", "dryer_t3", "dryer_t4",
            "distillery", "distillery_t2", "distillery_t3", "distillery_t4",
            "canner", "canner_t2", "canner_t3", "canner_t4",
            "sifter", "sifter_t2", "sifter_t3", "sifter_t4",
            "compressor", "compressor_t2", "compressor_t3", "compressor_t4",
            "wiremill", "wiremill_t2", "wiremill_t3", "wiremill_t4",
            "advanced_crafting_table");

    /** The paint keys the 03 base writes while painted (CS.java:1161-1162, verbatim upstream). */
    private static final String NBT_COLOR = "gt.color";
    private static final String NBT_PAINTED = "gt.painted";

    /** The offline boot before the first NBT/NbtPathArgument touch (GTWiresCreativeTabTest.boot shape). */
    @BeforeAll
    static void boot() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline; registries are ready by now.
        }
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6MachinePaintItemLootDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The census shape: all 38 paintSelfTable tables carry the (quoted) paint carry function. */
    @Test
    void pinnedMachinePaintLootCensus() throws Exception {
        assertEquals(38, PAINT_BASES.size(), "the paintSelfTable census (21 p22 rows + canner 4 + kinetic trio 12 + ACT 1)");
        for (String tBase : PAINT_BASES) assertPaintSelfTable(tBase);
    }

    /**
     * One machine table: the vanilla createSingleItemTable shape (one pool, rolls 1,
     * survives_explosion) whose single item entry is the block's OWN item carrying the
     * copy_nbt function (source block_entity, exactly the two REPLACE ops into BlockEntityTag).
     */
    private static void assertPaintSelfTable(String aBase) throws Exception {
        JsonObject tTable = json("data/gt6/loot_tables/blocks/" + aBase + ".json");
        assertEquals("minecraft:block", tTable.get("type").getAsString(), aBase + ": the block loot type");
        assertTrue(tTable.get("random_sequence").getAsString().equals("gt6:blocks/" + aBase),
                aBase + ": the vanilla default random sequence");

        JsonArray tPools = tTable.getAsJsonArray("pools");
        assertEquals(1, tPools.size(), aBase + ": the self-drop single pool");
        JsonObject tPool = tPools.get(0).getAsJsonObject();
        assertEquals(1.0, tPool.get("rolls").getAsDouble(), aBase + ": one roll");
        boolean tSurvives = false;
        for (JsonElement tCondition : tPool.getAsJsonArray("conditions")) {
            if ("minecraft:survives_explosion".equals(tCondition.getAsJsonObject().get("condition").getAsString())) tSurvives = true;
        }
        assertTrue(tSurvives, aBase + ": the vanilla survives_explosion pool condition");

        JsonArray tEntries = tPool.getAsJsonArray("entries");
        assertEquals(1, tEntries.size(), aBase + ": one entry");
        JsonObject tEntry = tEntries.get(0).getAsJsonObject();
        assertEquals("minecraft:item", tEntry.get("type").getAsString(), aBase + ": the item entry");
        assertEquals("gt6:" + aBase, tEntry.get("name").getAsString(), aBase + ": the self-drop item");

        JsonArray tFunctions = tEntry.getAsJsonArray("functions");
        assertEquals(1, tFunctions.size(), aBase + ": exactly the paint carry function");
        JsonObject tFunction = tFunctions.get(0).getAsJsonObject();
        assertEquals("minecraft:copy_nbt", tFunction.get("function").getAsString(), aBase + ": the copy_nbt function");
        assertEquals("block_entity", tFunction.get("source").getAsString(), aBase + ": the block entity source");

        JsonArray tOps = tFunction.getAsJsonArray("ops");
        assertEquals(2, tOps.size(), aBase + ": exactly the two paint ops");
        assertOp(tOps.get(0).getAsJsonObject(), NBT_COLOR, aBase);
        assertOp(tOps.get(1).getAsJsonObject(), NBT_PAINTED, aBase);
    }

    /**
     * One REPLACE op in the QUOTED form (task p25-paint-loot-dotkey-fix): the dotted key
     * rides as a single-quote SNBT segment both on the source and under BlockEntityTag —
     * the unquoted raw form parsed as two compound-child nodes and silently no-oped.
     */
    private static void assertOp(JsonObject aOp, String aKey, String aBase) {
        assertEquals(quoted(aKey), aOp.get("source").getAsString(), aBase + ": the op source key (SNBT quoted segment)");
        assertEquals("BlockEntityTag." + quoted(aKey), aOp.get("target").getAsString(),
                aBase + ": the op target key (quoted segment under BlockEntityTag)");
        assertEquals("replace", aOp.get("op").getAsString(), aBase + ": the REPLACE merge strategy");
    }

    /**
     * The semantic probe (task p25-paint-loot-dotkey-fix): the committed oven-table op
     * strings, parsed by the REAL vanilla NbtPathArgument (the loot runtime's own parse
     * arm — CopyNbtFunction.compileNbtPath), must resolve the flat BE paint keys and land
     * the pair as flat keys INSIDE the item's BlockEntityTag compound. The drive is the
     * exact CopyNbtFunction.CopyOperation.apply flow — sourcePath.get then the
     * MergeStrategy.REPLACE arm (NbtPath.set) — over synthetic NBT, asserting the
     * GTItemPaintTint.java:80-86 read contract: the dropped stack tints, and on placement
     * BlockItem re-creates the painted BE.
     */
    @Test
    void quotedOpsCarryThePaintRoundTrip() throws Exception {
        JsonObject tTable = json("data/gt6/loot_tables/blocks/oven.json");
        JsonArray tOps = tTable.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonArray("functions").get(0).getAsJsonObject()
                .getAsJsonArray("ops");

        CompoundTag tBlockEntity = new CompoundTag(); // the ContextNbtProvider.BLOCK_ENTITY payload (saveWithoutMetadata shape)
        int tColor = 0x0000FF00;
        tBlockEntity.putInt(NBT_COLOR, tColor); // TileEntityBase03TicksAndSync.java:323 — the FLAT dotted key
        tBlockEntity.putBoolean(NBT_PAINTED, true); // :324
        CompoundTag tItemRoot = new CompoundTag(); // the dropped stack's root tag

        for (JsonElement tOpEl : tOps) {
            JsonObject tOp = tOpEl.getAsJsonObject();
            NbtPathArgument.NbtPath tSource = compileNbtPath(tOp.get("source").getAsString());
            List<Tag> tFound = tSource.get(tBlockEntity); // THROWS on miss — the pre-fix silent no-op point
            assertFalse(tFound.isEmpty(), tOp.get("source").getAsString() + ": must resolve the flat BE key");
            NbtPathArgument.NbtPath tTarget = compileNbtPath(tOp.get("target").getAsString());
            tTarget.set(tItemRoot, tFound.get(tFound.size() - 1)); // the REPLACE merge arm
        }

        CompoundTag tCarried = tItemRoot.getCompound("BlockEntityTag");
        assertTrue(tCarried.contains(NBT_PAINTED, Tag.TAG_ANY_NUMERIC), "the painted flag rides inside BlockEntityTag");
        assertTrue(tCarried.getBoolean(NBT_PAINTED), "the painted flag survives the round trip");
        assertEquals(tColor, tCarried.getInt(NBT_COLOR), "the colour rides inside BlockEntityTag");
        assertFalse(tCarried.contains("gt"), "NO junk nested gt compound — the flat dotted key stays flat");
    }

    /**
     * The defect pin (task p25-paint-loot-dotkey-fix): the OLD unquoted path text still
     * parses (into TWO compound-child nodes) but misses the flat key — the vanilla parser
     * throws "nothing found", which CopyNbtFunction.CopyOperation.apply swallows silently
     * (vanilla 1.20.1 CopyNbtFunction.java:143-150). Guards the quoted form against a
     * "simplification" back to the raw dotted key.
     */
    @Test
    void unquotedDottedKeyMissesTheFlatKey() throws Exception {
        CompoundTag tBlockEntity = new CompoundTag();
        tBlockEntity.putInt(NBT_COLOR, 1);
        NbtPathArgument.NbtPath tPath = compileNbtPath("gt.color"); // parses fine — as TWO nodes
        assertThrows(CommandSyntaxException.class, () -> tPath.get(tBlockEntity),
                "the unquoted dotted key must NOT resolve the flat BE key (the p25 defect)");
    }

    /** The vanilla CopyNbtFunction.compileNbtPath mirror (the loot runtime's own parse arm). */
    private static NbtPathArgument.NbtPath compileNbtPath(String aText) throws CommandSyntaxException {
        return new NbtPathArgument().parse(new StringReader(aText));
    }

    /** The single-quote SNBT-path segment wrap (the GT6LootTables.quoted mirror, for the pinned assertions). */
    private static String quoted(String aKey) {
        return "'" + aKey + "'";
    }
}
