/**
 * The tint-chain closure pins (task tint-chain-hopper-grindstone-sifting — the
 * research.tint-translation-census L1/L2/L3 rows): the 120 storage-hopper rows resolve
 * their loader NBT_MATERIAL columns through the {@code GT6HopperBlock} row carrier, the
 * Grindstone and the Sifting Table their single ANY.Steel rows through the block-class
 * carriers — the combined {@code GTMachinePaintTint.tintMaterialOf} dispatch every tint
 * consumer (the baked wrap, the NEI-model self-tint arms, the ItemColor inventory half)
 * funnels into. The upstream anchor is the BlockTextureMulti(colored × mRGBa) pass
 * (MultiTileEntityHopper.java:281 / GrindStone :238-241 / SiftingTable :406-424); the
 * values pin the registration derivation {@code getRGBInt(fRGBaSolid)}
 * (MultiTileEntityClassContainer.java:51), and the shipped funnel models' tintindex
 * seats pin the datagen half (the colored body band seat, the P22 untinted overlays).
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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6MaterialTestSupport;

class GT6TintChainHopperGrindstoneSiftingTest extends GTOfflineRenderTestBase {

	/** The six shared funnel models (3 per kind — the addHoppers walk). */
	private static final String[] HOPPER_MODELS = {
			"gt6_hopper_down", "gt6_hopper", "gt6_hopper_top",
			"gt6_queuehopper_down", "gt6_queuehopper", "gt6_queuehopper_top"};

	@BeforeAll
	static void bootMaterials() {
		// the hermetic bracket (the r11e reset-first house rule): the row materials resolve live
		GT6MaterialTestSupport.materials();
	}

	/** The BLOCK registry write window (the GT6GrindstoneNeiModelTest recipe). */
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

	/** A bare hopper carrier over one loader row (properties irrelevant to the material gate). */
	private static net.minecraft.world.level.block.Block hopperBlock(String aPath) {
		unfreezeBlockRegistry();
		GT6Hoppers.HopperRow tRow = GT6Hoppers.ROWS.stream()
				.filter(aRow -> aRow.path().equals(aPath)).findFirst().orElseThrow();
		return new GT6Hoppers.GT6HopperBlock(tRow, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	// ------------------------------------------------------------------
	// L1 — the 120 storage-hopper rows
	// ------------------------------------------------------------------

	/** The dispatch arm: representative rows (plain + queue halves, dull + bright + the :235 ANY.W line). */
	@Test
	void hopperRowsResolveTheirLoaderRowMaterials() {
		assertSame(MT.Pb, GTMachinePaintTint.tintMaterialOf(hopperBlock("hopper_lead")),
				"the lead hopper rides the :186 row material");
		assertSame(MT.Steel, GTMachinePaintTint.tintMaterialOf(hopperBlock("hopper_steel")),
				"the steel hopper rides MT.Steel (the :202 anchor, NOT the ANY.Steel alias)");
		assertSame(MT.Au, GTMachinePaintTint.tintMaterialOf(hopperBlock("queue_hopper_gold")),
				"the queue half rides the same loader line's material");
		assertSame(MT.W, GTMachinePaintTint.tintMaterialOf(hopperBlock("hopper_tungsten")),
				"the :235 ANY.W line resolves the Tungsten face (ANY.java:133 setLocal)");
		// the full walk: every row resolves a non-null material (the census L1 closure, all 120)
		unfreezeBlockRegistry();
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			assertNotNull(GTMachinePaintTint.tintMaterialOf(
					new GT6Hoppers.GT6HopperBlock(tRow, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
					"the row " + tRow.path() + " resolves its material");
		}
	}

	/** The value pins: the upstream mRGBa products (getRGBInt over fRGBaSolid), pairwise distinct, paint wins. */
	@Test
	void hopperTintValuesAreTheUpstreamMRgbaProducts() {
		net.minecraft.world.level.block.Block tLead = hopperBlock("hopper_lead");
		net.minecraft.world.level.block.Block tSteel = hopperBlock("hopper_steel");
		net.minecraft.world.level.block.Block tGold = hopperBlock("queue_hopper_gold");
		// MT.java:472 Pb 60,40,110; MT.java:1713 Steel 130,130,130
		assertEquals(0xFF3C286E, GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tLead), 0),
				"the lead hopper tints 60,40,110 — the dark lead row (the 不深灰 symptom killer)");
		assertEquals(0xFF828282, GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tSteel), 0),
				"the steel hopper tints 130,130,130 gray-white");
		int tLeadTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tLead), 0) & 0xFFFFFF;
		int tSteelTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tSteel), 0) & 0xFFFFFF;
		int tGoldTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tGold), 0) & 0xFFFFFF;
		assertNotEquals(tLeadTint, tSteelTint);
		assertNotEquals(tSteelTint, tGoldTint);
		assertNotEquals(tLeadTint, tGoldTint, "lead/steel/gold pairwise distinct (the all-gray lesson)");
		// the spray-paint override wins over the row colour (upstream Paintable:85)
		ModelData tPainted = GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(0xFF0000)).build();
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(tPainted, GTMachinePaintTint.tintMaterialOf(tLead), 0),
				"painted wins over the lead row colour");
	}

	// ------------------------------------------------------------------
	// L2/L3 — the Grindstone and the Sifting Table
	// ------------------------------------------------------------------

	/**
	 * Both manual-tool carriers ride the ANY.Steel row (Loader :2226/:2227). The ANY.Steel
	 * row STEALS the MT.Steel looks (upstream ANY.java:120 {@code Steel.stealLooks(MT.Steel)}
	 * / port ANY.java:200), so the closure resolves the 130,130,130 gray-white — the census
	 * "打磨石无钢色" symptom IS the chain gap, and the unpainted arm renders the steel
	 * product once the dispatch lands. The spray-paint override stays the live payoff.
	 */
	@Test
	void grindstoneAndSiftingRideTheSteelRowCarrier() {
		unfreezeBlockRegistry();
		gregtech6.block.tools.GT6GrindstoneBlock tGrindstone =
				new gregtech6.block.tools.GT6GrindstoneBlock(() -> null,
						net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
		gregtech6.block.tools.GT6SiftingTableBlock tSifting =
				new gregtech6.block.tools.GT6SiftingTableBlock(() -> ANY.Steel, () -> null,
						net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
		assertSame(ANY.Steel, GTMachinePaintTint.tintMaterialOf(tGrindstone), "the grindstone rides the :2226 row");
		assertSame(ANY.Steel, GTMachinePaintTint.tintMaterialOf(tSifting), "the sifting table rides the :2227 row");
		// the ANY.Steel product = the MT.Steel looks (130,130,130) — the same gray-white every
		// steel machine renders (GTMachinePaintTintTest's steel pin)
		assertEquals(0xFF828282, GTMachinePaintTint.tintARGB(ModelData.EMPTY, ANY.Steel, 0),
				"the ANY.Steel row renders the stolen MT.Steel looks 130,130,130");
		// the spray-paint override: the closure's live payoff (upstream Paintable:85)
		ModelData tPainted = GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(0xFF0000)).build();
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(tPainted, ANY.Steel, 0),
				"a painted grindstone/sifting table renders the stored colour");
	}

	// ------------------------------------------------------------------
	// the datagen seat half — the shipped funnel models
	// ------------------------------------------------------------------

	/**
	 * The six shared funnel models carry the tint seats: every colored-band face has
	 * {@code tintindex 0}, every overlay twin none (the P22 decal contract) — the seat
	 * the {@code GTMachineTintModel} wrap multiplies the row colour into (the census L1
	 * "模型 tintindex 齐全" leg, all 120 states ride these six models).
	 */
	@Test
	void hopperFunnelModelsCarryTheTintSeats() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		for (String tName : HOPPER_MODELS) {
			Path tJson = tMdk.resolve("src/generated/resources/assets/gt6/models/block/" + tName + ".json");
			assertTrue(Files.isRegularFile(tJson), "the model " + tName + " ships");
			JsonElement tRoot = JsonParser.parseString(Files.readString(tJson));
			int tBodyFaces = 0, tOverlayFaces = 0;
			for (JsonElement tElement : tRoot.getAsJsonObject().getAsJsonArray("elements")) {
				for (var tFaceEntry : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
					String tTexture = tFaceEntry.getValue().getAsJsonObject().get("texture").getAsString();
					JsonElement tTint = tFaceEntry.getValue().getAsJsonObject().get("tintindex");
					if (tTexture.startsWith("#overlay_")) {
						tOverlayFaces++;
						assertNull(tTint, tName + " overlay face " + tFaceEntry.getKey() + " stays untinted (the P22 contract)");
					} else {
						tBodyFaces++;
						assertNotNull(tTint, tName + " colored face " + tFaceEntry.getKey() + " carries the seat");
						assertEquals(0, tTint.getAsInt(), tName + " colored face " + tFaceEntry.getKey() + " is seat 0");
					}
				}
			}
			// rim (6+6) + middle (5+5, the skipped UP face) + the spout forms (5+5 / none):
			// the plain/north models count 16+16, the top (spout-less) form 11+11 — the same
			// skip applies to both layers, so the bands mirror exactly
			assertTrue(tBodyFaces == 16 || tBodyFaces == 11,
					tName + " body face count " + tBodyFaces + " matches the box plan");
			assertEquals(tBodyFaces, tOverlayFaces, tName + " overlay twins mirror the body band");
		}
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
