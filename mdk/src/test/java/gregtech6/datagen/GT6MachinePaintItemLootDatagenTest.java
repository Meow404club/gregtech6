/*
 * Offline pinned tests for task p22-painted-item-domain: the generated machine loot tables
 * carry the paint round-trip copy_nbt function — asserted against the committed
 * src/generated tree (the GT6MachinePaintRenderDatagenTest read-only split: the write side
 * is gated by runData, first run written>0, second run written:0).
 *
 * <p>Census ground truth: the paintable machine domain is oven (1) +
 * shredder/crusher/lathe T1-T4 (12) + dryer (4) + distillery (4) = 21 blocks
 * (GTMachines.paintableBlockArray; the GT6MachinePaintRenderDatagenTest:71 census), so
 * exactly 21 block loot tables each carry one copy_nbt function with the TWO paint ops.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6MachinePaintItemLootDatagenTest {

    /** The 21 machine-domain bases, in registration order (the paintableBlockArray census). */
    private static final List<String> MACHINE_BASES = List.of(
            "oven",
            "shredder", "shredder_t2", "shredder_t3", "shredder_t4",
            "crusher", "crusher_t2", "crusher_t3", "crusher_t4",
            "lathe", "lathe_t2", "lathe_t3", "lathe_t4",
            "dryer", "dryer_t2", "dryer_t3", "dryer_t4",
            "distillery", "distillery_t2", "distillery_t3", "distillery_t4");

    /** The paint keys the 03 base writes while painted (CS.java:1161-1162, verbatim upstream). */
    private static final String NBT_COLOR = "gt.color";
    private static final String NBT_PAINTED = "gt.painted";

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6MachinePaintItemLootDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The census shape: all 21 paintable machine tables carry the paint carry function. */
    @Test
    void pinnedMachinePaintLootCensus() throws Exception {
        assertEquals(21, MACHINE_BASES.size(), "the machine block census (paintableBlockArray)");
        for (String tBase : MACHINE_BASES) assertPaintSelfTable(tBase);
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

    /** One REPLACE op: the paint key straight into the BlockEntityTag compound. */
    private static void assertOp(JsonObject aOp, String aKey, String aBase) {
        assertEquals(aKey, aOp.get("source").getAsString(), aBase + ": the op source key");
        assertEquals("BlockEntityTag." + aKey, aOp.get("target").getAsString(), aBase + ": the op target key");
        assertEquals("replace", aOp.get("op").getAsString(), aBase + ": the REPLACE merge strategy");
    }
}
