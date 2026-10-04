package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Batteries;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline assertions for task battery-box-loot-selfdrop (the p36 item-3 field report):
 * breaking a Battery Box dropped only its CONTENT batteries — the box body itself never
 * dropped. Root cause: w4-battery-storage registered the 12 boxes
 * ({@code GT6Batteries.BOX_ROWS}) without any loot provider (research.machine-break-behavior;
 * zero {@code battery_box*.json} in the committed tree), so vanilla resolved the empty
 * default table and the body vanished; the contents kept dropping through the
 * GTEntityBlock.onRemove bridge (r4-19 + r10).
 *
 * <p>Upstream parity: the MTE body drop is TE.getDrops over the registered item
 * (TileEntityBase04MultiTileEntities.java:166-171) with canDrop all-true for every box
 * (TileEntityBase10EnergyBatBox.java:220); with the keepSlot gate the body is the EMPTY box
 * (05Inventories.java:74) and the contents scatter separately — no double drop. The 1.20.1
 * equivalent is exactly the family's dropSelf form (the
 * {@link GT6LootTables#GT6CrystalChargerBlockLoot charger shape verbatim} — the 20-row LU
 * family rides the same storage block shape).
 *
 * <p>Why the JSON layer: the 12 blocks live in {@code RegistryObject}s that only bind under
 * real registry events, so {@code GT6LootTables.batteryBoxLootBlocks()} cannot dereference in
 * this headless JVM — the {@link GT6WireLootLaserTest} split verbatim: provider
 * existence/registration is pinned structurally here, the generated-JSON side against the
 * committed tree, the runData + datagen_tree_check gate closes the loop.
 */
public class GT6BatteryBoxLootDatagenTest extends GTOfflineTestBase {

	/** The row paths — the loot carriers (the vanilla default block-table location gt6:blocks/&lt;path&gt;). */
	private static List<String> boxPaths() {
		return GT6Batteries.BOX_ROWS.stream().map(GT6Batteries.BoxRow::path).toList();
	}

	private static String raw(String aPath) throws IOException {
		try (InputStream tStream = GT6BatteryBoxLootDatagenTest.class.getClassLoader()
				.getResourceAsStream("data/gt6/loot_tables/blocks/" + aPath + ".json")) {
			assertNotNull(tStream, "the generated loot table must be on the classpath: " + aPath);
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** The 21.1 singular band (the GT6OreCensusTest two-JSON face) — existence off the source tree. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (p.resolve("src/generated/resources").toFile().exists()) return p;
		}
		throw new IllegalStateException("mdk root not found from " + Path.of("").toAbsolutePath());
	}

	/**
	 * All 12 box tables exist and are the dropSelf shape: one unconditional single-item pool
	 * over the block's own item, the vanilla survives_explosion condition — breaking the
	 * placed box drops the BOX ITSELF (the empty body; contents keep the onRemove bridge).
	 */
	@Test
	public void theTwelveBoxLootTablesAreSelfDropsInTheGeneratedTree() throws IOException {
		List<String> tPaths = boxPaths();
		assertEquals(12, tPaths.size(), "the tier-closed family (the GT6BatteryLadderTest census)");
		for (String tPath : tPaths) {
			JsonObject tLoot = JsonParser.parseString(raw(tPath)).getAsJsonObject();
			assertEquals("minecraft:block", tLoot.get("type").getAsString(), tPath + ": the BLOCK param set table");
			assertEquals(1, tLoot.getAsJsonArray("pools").size(), tPath + ": exactly one pool (dropSelf)");
			JsonObject tPool = tLoot.getAsJsonArray("pools").get(0).getAsJsonObject();
			assertEquals(1.0, tPool.get("rolls").getAsDouble(), tPath + ": one roll");
			assertEquals(1, tPool.getAsJsonArray("entries").size(), tPath + ": exactly one entry (dropSelf)");
			JsonObject tEntry = tPool.getAsJsonArray("entries").get(0).getAsJsonObject();
			assertEquals("minecraft:item", tEntry.get("type").getAsString(), tPath + ": the item entry");
			assertEquals("gt6:" + tPath, tEntry.get("name").getAsString(), tPath + ": the box drops ITSELF");
			assertEquals(1, tPool.getAsJsonArray("conditions").size(), tPath + ": no silk/fortune dispatch");
			assertEquals("minecraft:survives_explosion", tPool.getAsJsonArray("conditions").get(0)
					.getAsJsonObject().get("condition").getAsString(), tPath + ": the vanilla condition");
			assertEquals("gt6:blocks/" + tPath, tLoot.get("random_sequence").getAsString(), tPath + ": the sequence");
			// the 21.1 singular band — same table, the cross-leg identity is tree_check's job
			assertTrue(Files.exists(mdkRoot().resolve("src/generated/resources/data/gt6/loot_table/blocks/" + tPath + ".json")),
					tPath + ": the 21.1 loot_table band JSON");
		}
	}

	/** The provider exists: the sub-provider class + the blocks-list builder (the charger twins). */
	@Test
	public void theBatteryBoxLootProviderExists() throws Exception {
		assertNotNull(Class.forName("gregtech6.datagen.GT6LootTables$GT6BatteryBoxBlockLoot"),
				"the dropSelf sub-provider (the GT6CrystalChargerBlockLoot twin)");
		assertNotNull(GT6LootTables.class.getMethod("batteryBoxLootBlocks"),
				"the blocks-list builder (the crystalChargerLootBlocks twin)");
	}

	/** The provider is wired into BOTH SubProviderEntry legs (the w4 omission was exactly this list). */
	@Test
	public void theProviderIsRegisteredOnBothSubProviderLegs() throws IOException {
		Path tSource = mdkRoot().resolve("src/main/java/gregtech6/datagen/GT6LootTables.java");
		String tText = Files.readString(tSource, StandardCharsets.UTF_8);
		assertEquals(2, tText.split("GT6BatteryBoxBlockLoot::new", -1).length - 1,
				"one SubProviderEntry per leg (neo 1.21.1 + forge 1.20.1), like the charger entry");
	}
}
