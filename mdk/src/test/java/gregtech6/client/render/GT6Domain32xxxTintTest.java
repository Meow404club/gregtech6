/**
 * The 32xxx-domain tint-chain pins (task block-family-32xxx-port): the 60 charging-locker
 * rows resolve their loader NBT_MATERIAL through the row carrier (Loader :139 over the
 * metalset walk — the MultiTileEntityLockerCharging :65 colored × mRGBa pass), the sap
 * bag its MT.Leather column (:2221, SapBag :133) and the plant pot its MT.Ceramic column
 * (:2229, PlantPot :76); the shipped models carry the tintindex-0 body seats with the
 * untinted overlay/overlay_full decals (the P22 contract).
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.registry.GT6ChargingLockers;
import gregtech6.registry.GT6MaterialTestSupport;
import gregtech6.registry.GT6MiscToolBlocks;

class GT6Domain32xxxTintTest extends GTOfflineRenderTestBase {

	@BeforeAll
	static void bootMaterials() {
		GT6MaterialTestSupport.materials();
		gregtech6.registry.GT6DriverTestSupport.loadFamiliesPristine();
	}

	private static void unfreezeBlockRegistry() {
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/** A bare charging-locker carrier over one loader row (the hopper-test form). */
	private static net.minecraft.world.level.block.Block chargingLockerBlock(String aSlug) {
		unfreezeBlockRegistry();
		GT6ChargingLockers.ChargingLockerRow tRow = GT6ChargingLockers.ROWS.stream()
				.filter(aRow -> aRow.material().slug().equals(aSlug)).findFirst().orElseThrow();
		return new GT6ChargingLockers.GT6ChargingLockerBlock(tRow, BlockBehaviour.Properties.of());
	}

	// ------------------------------------------------------------------
	// the dispatch arms
	// ------------------------------------------------------------------

	/** Representative charging rows resolve their loader materials; the single rows their fixed columns. */
	@Test
	void the32xxxBodiesResolveTheirRowMaterials() {
		assertSame(MT.Pb, GTMachinePaintTint.tintMaterialOf(chargingLockerBlock("lead")),
				"the lead charging locker rides the :186 row material");
		assertSame(MT.Steel, GTMachinePaintTint.tintMaterialOf(chargingLockerBlock("steel")),
				"the steel charging locker rides MT.Steel");
		assertSame(MT.W, GTMachinePaintTint.tintMaterialOf(chargingLockerBlock("tungsten")),
				"the :235 ANY.W line resolves the Tungsten face");
		// the full 60-row walk (the census parity leg)
		unfreezeBlockRegistry();
		for (GT6ChargingLockers.ChargingLockerRow tRow : GT6ChargingLockers.ROWS) {
			assertNotNull(GTMachinePaintTint.tintMaterialOf(
					new GT6ChargingLockers.GT6ChargingLockerBlock(tRow, BlockBehaviour.Properties.of())),
					"the row " + tRow.path() + " resolves its material");
		}
		// the two singles
		unfreezeBlockRegistry();
		assertSame(MT.Leather, GTMachinePaintTint.tintMaterialOf(new GT6MiscToolBlocks.GT6SapBagBlock(
				BlockBehaviour.Properties.of())), "the sap bag rides the :2221 MT.Leather column");
		assertSame(MT.Ceramic, GTMachinePaintTint.tintMaterialOf(new GT6MiscToolBlocks.GT6PlantPotBlock(
				BlockBehaviour.Properties.of())), "the plant pot rides the :2229 MT.Ceramic column");
	}

	/** The unpainted products differ pairwise (the all-gray lesson); the paint override wins. */
	@Test
	void theProductsDifferAndPaintWins() {
		int tLeather = GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Leather, 0) & 0xFFFFFF;
		int tCeramic = GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Ceramic, 0) & 0xFFFFFF;
		int tLead = GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Pb, 0) & 0xFFFFFF;
		assertNotEquals(tLeather, tCeramic);
		assertNotEquals(tCeramic, tLead);
		assertNotEquals(tLeather, tLead);
		var tPainted = GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(0xFF0000)).build();
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(tPainted, MT.Leather, 0),
				"the spray-paint override wins over the leather row colour (Paintable:85)");
	}

	// ------------------------------------------------------------------
	// the datagen seat half — the shipped models
	// ------------------------------------------------------------------

	/** The charging locker kind model: 7 elements, the body tintindex-0, the 6 decals untinted. */
	@Test
	void chargingLockerModelCarriesTheTintSeats() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		Path tJson = tMdk.resolve("src/generated/resources/assets/gt6/models/block/charging_locker.json");
		assertTrue(Files.isRegularFile(tJson), "the charging locker model ships");
		JsonElement tRoot = JsonParser.parseString(Files.readString(tJson));
		var tTextures = tRoot.getAsJsonObject().getAsJsonObject("textures");
		Set<String> tExpectedKeys = Set.of("down", "up", "north", "south", "west", "east", "particle",
				"overlay_down", "overlay_up", "overlay_north", "overlay_south", "overlay_west", "overlay_east");
		assertEquals(tExpectedKeys, tTextures.keySet(),
				"the 5-face colored/overlay band (the :70-82 icon table) binds all 13 keys");
		int tBodyFaces = 0, tOverlayFaces = 0;
		var tElements = tRoot.getAsJsonObject().getAsJsonArray("elements");
		assertEquals(7, tElements.size(), "the body cube + the six 0.01 decal plates");
		for (JsonElement tElement : tElements) {
			for (var tFaceEntry : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
				String tTexture = tFaceEntry.getValue().getAsJsonObject().get("texture").getAsString();
				JsonElement tTint = tFaceEntry.getValue().getAsJsonObject().get("tintindex");
				if (tTexture.startsWith("#overlay_")) {
					tOverlayFaces++;
					assertNull(tTint, "overlay face " + tFaceEntry.getKey() + " stays untinted (the P22 contract)");
				} else {
					tBodyFaces++;
					assertEquals(0, tTint.getAsInt(), "colored face " + tFaceEntry.getKey() + " is seat 0");
				}
			}
		}
		assertEquals(6, tBodyFaces, "the body cube carries six seat faces");
		assertEquals(6, tOverlayFaces, "the six decals mirror the body band");
	}

	/** The sap bag models: the FULL swap rebinds the three overlay keys to the overlay_full art. */
	@Test
	void sapBagModelsSwapTheFullOverlay() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found");
		Path tEmpty = tMdk.resolve("src/generated/resources/assets/gt6/models/block/gt6_sap_bag.json");
		Path tFull = tMdk.resolve("src/generated/resources/assets/gt6/models/block/gt6_sap_bag_full.json");
		assertTrue(Files.isRegularFile(tEmpty), "the sap bag model ships");
		assertTrue(Files.isRegularFile(tFull), "the FULL model ships (:145-149 trio)");
		var tEmptyRoot = JsonParser.parseString(Files.readString(tEmpty)).getAsJsonObject();
		var tFullRoot = JsonParser.parseString(Files.readString(tFull)).getAsJsonObject();
		// the body box 5,0,0..11,7,6 (:124) over both
		for (com.google.gson.JsonObject tRoot : new com.google.gson.JsonObject[] {tEmptyRoot, tFullRoot}) {
			var tFrom = tRoot.getAsJsonArray("elements").get(0).getAsJsonObject().get("from");
			assertEquals(5.0F, tFrom.getAsJsonArray().get(0).getAsFloat(), "the bag hugs x 5 (:124 Z_NEG form)");
			assertEquals(0.0F, tFrom.getAsJsonArray().get(1).getAsFloat(), "the bag sits on the floor");
		}
		assertEquals("gt6:block/sap_bag/overlay_top",
				tEmptyRoot.getAsJsonObject("textures").get("overlay_top").getAsString());
		assertEquals("gt6:block/sap_bag/overlay_full_top",
				tFullRoot.getAsJsonObject("textures").get("overlay_top").getAsString(),
				"the FULL model rebinds the overlay band to the overlay_full art");
	}

	/** The plant pot model: 4 elements (the plate + the body over their overlay twins), the two-pass skips. */
	@Test
	void plantPotModelCarriesTheTwoPassSkips() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found");
		Path tJson = tMdk.resolve("src/generated/resources/assets/gt6/models/block/gt6_plant_pot.json");
		assertTrue(Files.isRegularFile(tJson), "the plant pot model ships");
		var tElements = JsonParser.parseString(Files.readString(tJson)).getAsJsonObject().getAsJsonArray("elements");
		assertEquals(4, tElements.size(), "the plate + body over their overlay twins (:66-70)");
		// element 0 = the plate (0,10,0..16): no down face; element 1 = its overlay twin
		var tPlateFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
		assertEquals(5, tPlateFaces.size(), "the plate draws 5 faces — the down face skipped (the body sits under it)");
		assertNull(tPlateFaces.get("down"), "the plate's down face is the two-pass skip");
		// the body: from (1,0,1), the up face skipped under the plate
		var tBody = tElements.get(2).getAsJsonObject();
		assertEquals(1.0F, tBody.get("from").getAsJsonArray().get(0).getAsFloat(), "the body inset x 1 (:69)");
		var tBodyFaces = tBody.getAsJsonObject("faces");
		assertNull(tBodyFaces.get("up"), "the body's up face is the two-pass skip");
		assertEquals(5, tBodyFaces.size(), "the body draws 5 faces");
		// the colored faces carry the seat, the overlay twins do not
		for (int i = 0; i < 4; i++) {
			var tFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
			boolean tOverlay = i % 2 == 1;
			for (var tFaceEntry : tFaces.entrySet()) {
				JsonElement tTint = tFaceEntry.getValue().getAsJsonObject().get("tintindex");
				if (tOverlay) {
					assertNull(tTint, "the overlay twin face stays untinted (the P22 contract)");
				} else {
					assertNotNull(tTint, "the colored face carries the seat");
					assertEquals(0, tTint.getAsInt());
				}
			}
		}
	}

	/** The blockstates: 60 charging-locker states ride the shared model over the 4 facing variants; the bag has 8. */
	@Test
	void blockstatesRideTheSharedModels() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found");
		Path tStates = tMdk.resolve("src/generated/resources/assets/gt6/blockstates");
		int tLockerStates = 0;
		try (var tStream = Files.list(tStates)) {
			tLockerStates = (int) tStream.filter(aP -> aP.getFileName().toString().startsWith("charging_locker_")).count();
		}
		assertEquals(GT6ChargingLockers.ROWS.size(), tLockerStates, "one blockstate per row");
		var tLocker = JsonParser.parseString(Files.readString(tStates.resolve("charging_locker_lead.json"))).getAsJsonObject();
		assertEquals(4, tLocker.getAsJsonObject("variants").size(), "the 4 facing y-rotations");
		var tBag = JsonParser.parseString(Files.readString(tStates.resolve("sap_bag.json"))).getAsJsonObject();
		assertEquals(8, tBag.getAsJsonObject("variants").size(), "4 facings x the FULL pair");
	}

	/** The mdk root from the test working directory (the GT6GrindstoneNeiModelTest form). */
	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/datagen/GT6BlockStates.java"))) {
				return tDir;
			}
		}
		return null;
	}
}
