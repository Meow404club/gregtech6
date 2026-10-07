/**
 * The Charging Locker recipe A-ruling pins (task block-family-32xxx-port): the :139
 * recipe's 'M' column is the SAME-material plain locker, and the port plain locker is
 * the storage-static-batch two-anchor fold — the Bronze/Steel rows resolve their 'M',
 * the other 58 null-drop WITHOUT throwing (the coordinator ruling 2026-10-07, the
 * mc-A1 backfill owns the tail; no material-compensating substitute). The generated
 * tree is the build face: exactly 2 recipe JSONs ship.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6ChargingLockers;
import gregtech6.registry.GTMaterialItems;

import gregtech6.tileentity.GTOfflineTestBase;

class GT6ChargingLockerRecipeARulingTest extends GTOfflineTestBase {

	private static final GT6CraftingRecipes RECIPES = new GT6CraftingRecipes(
			new net.minecraft.data.PackOutput(java.nio.file.Path.of("build", "test-charging-locker-a")),
			java.util.concurrent.CompletableFuture.completedFuture(null));

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
		gregtech6.registry.GT6DriverTestSupport.loadFamiliesPristine();
	}

	private static GT6ChargingLockers.ChargingLockerRow row(String aSlug) {
		return GT6ChargingLockers.ROWS.stream()
				.filter(aRow -> aRow.material().slug().equals(aSlug)).findFirst().orElseThrow();
	}

	/**
	 * The A ruling: the 58 non-anchor rows null-drop through the handle seam; the two
	 * anchor rows carry a mapped handle (the registration-truth face — the live build
	 * lands through the generated-tree pin below, the cold JVM never dereferences .get()).
	 */
	@Test
	void theFiftyEightTailRowsNullDropAndTheAnchorsCarryMappedHandles() {
		for (GT6ChargingLockers.ChargingLockerRow tRow : GT6ChargingLockers.ROWS) {
			switch (tRow.material().slug()) {
				case "bronze", "steel" ->
					assertNotNull(GT6CraftingRecipes.plainLockerHandle(tRow.material().slug()),
							"the " + tRow.path() + " anchor maps the storage-static-batch locker");
				default -> {
					assertNull(GT6CraftingRecipes.plainLockerHandle(tRow.material().slug()),
							"the " + tRow.path() + " row has NO plain locker anchor (the A ruling)");
					assertNull(RECIPES.chargingLockerRecipeBuilder(tRow),
							"the " + tRow.path() + " row null-drops without throwing");
				}
			}
		}
	}

	/**
	 * The generated tree is the build face: exactly 2 charging-locker recipe JSONs
	 * (bronze/steel) ship, the 58-row tail stays absent (the 2/60 honest seat).
	 */
	@Test
	void theGeneratedTreeShipsExactlyTheTwoAnchorRecipes() throws Exception {
		Path tMdk = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tMdk != null; i++, tMdk = tMdk.getParent()) {
			if (Files.isRegularFile(tMdk.resolve("src/main/java/gregtech6/datagen/GT6BlockStates.java"))) break;
		}
		assertNotNull(tMdk, "mdk root not found");
		Path tRecipes = tMdk.resolve("src/generated/resources/data/gt6/recipe");
		assertTrue(Files.isRegularFile(tRecipes.resolve("charging_locker_bronze.json")), "the bronze row builds");
		assertTrue(Files.isRegularFile(tRecipes.resolve("charging_locker_steel.json")), "the steel row builds");
		assertFalse(Files.isRegularFile(tRecipes.resolve("charging_locker_lead.json")),
				"the lead row stays UNGENERATED (the A ruling, the mc-A1 backfill owns it)");
		int tCount = 0;
		try (var tStream = Files.list(tRecipes)) {
			tCount = (int) tStream.filter(aP -> aP.getFileName().toString().startsWith("charging_locker_")).count();
		}
		assertEquals(2, tCount, "exactly 2/60 recipes ship — the A-ruling honest seat");
	}

	/** The recipe id face: result-path convention, one per row. */
	@Test
	void recipeIdsRideTheResultPathConvention() {
		assertEquals("gt6:charging_locker_lead", RECIPES.chargingLockerRecipeId(row("lead")).toString());
	}
}
