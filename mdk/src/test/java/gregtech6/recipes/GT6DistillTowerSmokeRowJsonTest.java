package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The tower datapack acceptance (task p29-w3-distill-crucible, the OFFLINE half — the
 * tower half RESHAPED by the p29-w4-hot-lube review round: the W3 smoke row
 * (gt6:oil 1000 → gt6:creosote, 120/160) had no upstream anchor and crowded the
 * Loader_Recipes_Chem.java:356 Oil_Normal true row off its own input face, so the file now
 * carries the SIX :352-:360 true rows while {@code cryodistillationtower} keeps its
 * water → ice freeze row) — both committed {@code data/gt6/recipe_maps/<map>.json} files
 * pour through the PUBLIC {@link GT6RecipeMapJsonLoader#pour} seam into their maps (the
 * GT6EuHuSmokeRowJsonTest fixture convention), and the map-key anchors resolve (the
 * consumer-card JSON direct-pour seam). The map constants rows carry the RM.java:65/:66
 * columns.
 */
class GT6DistillTowerSmokeRowJsonTest extends GTRecipesOfflineTestBase {

	// the fixtures widen to catch-all gt6 stand-ins (the review-round reshape): the six
	// true rows carry SEVEN fluid product ids + three dustTiny item ids per row — a null
	// from the resolver DROPS the row (the loader bad-row seam), so every gt6 id stands in
	private static final Function<ResourceLocation, Item> ITEM_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:ice" -> Items.ICE;
		default -> aId.getNamespace().equals("gt6") ? Items.IRON_INGOT : null;
	};

	private static final Function<ResourceLocation, Fluid> FLUID_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:water" -> Fluids.WATER;
		case "minecraft:lava" -> Fluids.LAVA;
		default -> aId.getNamespace().equals("gt6") ? Fluids.WATER : null; // the identity is what the test pins
	};

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = ITEM_FIXTURE;
		GT6RecipeMapJsonLoader.sFluidResolver = FLUID_FIXTURE;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems;
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset();
	}

	/** Reads one committed smoke-row file off the test classpath (src/main/resources rides it). */
	private static JsonElement resource(String aName) throws Exception {
		try (InputStream tStream = GT6DistillTowerSmokeRowJsonTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aName)) {
			assertNotNull(tStream, "the committed smoke row must be on the classpath: " + aName);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void theTwoFilesPourThroughTheLoaderSeam() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "distillationtower"), resource("distillationtower.json"));
		tData.put(new ResourceLocation("gt6", "cryodistillationtower"), resource("cryodistillationtower.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		// the review-round ratchet: the tower file pours the SIX :352-:360 true rows (the
		// W3 smoke row removed — no upstream anchor, it crowded :356 off the oil input face)
		assertEquals(6, GT6RecipeMapJsonLoader.pouredCount("distillationtower"), "the six tower true rows");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("cryodistillationtower"), "one cryo smoke row");
	}

	@Test
	public void thePouredRowIsFindableThroughTheTowerLookup() throws Exception {
		// the tower's own lookup shape (the checkRecipe override): one empty item slot, the
		// input-tank snapshot, size mInputMax — the oil stand-in rides the ALL rows share one
		// input face shape (25 L), so the lookup resolves a true row and the shared faces pin
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "distillationtower"), resource("distillationtower.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		Recipe tFound = GT6RecipeMaps.DISTILLATION_TOWER.findRecipe(null, 1024, ItemStack.EMPTY,
				new FluidStack[]{new FluidStack(Fluids.WATER, 25)}, ItemStack.EMPTY);
		assertNotNull(tFound, "the true-row file must be findable with an all-empty item array");
		assertEquals(64, tFound.mEUt, "the shared :352-:360 EUt");
		assertEquals(7, tFound.mFluidOutputs.length, "the SEVEN product slots");
	}

	@Test
	public void theMapKeysAnchorTheConsumerSeam() {
		assertSame(GT6RecipeMaps.DISTILLATION_TOWER, GT6RecipeMapJsonLoader.mapFor("distillationtower"));
		assertSame(GT6RecipeMaps.CRYO_DISTILLATION_TOWER, GT6RecipeMapJsonLoader.mapFor("cryodistillationtower"));
	}

	@Test
	public void theMapConstantsAreTheUpstreamRmRows() {
		// RM.java:65/:66 — items 1/3/0, fluids 1/9/0, MIN 1, AMP 1; the twin rows differ ONLY
		// by map name/GUI (the STEAM_CRACKING/its-twin judged form)
		for (RecipeMap tMap : new RecipeMap[] {GT6RecipeMaps.DISTILLATION_TOWER, GT6RecipeMaps.CRYO_DISTILLATION_TOWER}) {
			assertEquals(1, tMap.mInputItemsCount, "IN-ITEM");
			assertEquals(3, tMap.mOutputItemsCount, "OUT-ITEM");
			assertEquals(0, tMap.mMinimalInputItems, "MIN-ITEM");
			assertEquals(1, tMap.mInputFluidCount, "IN-FLUID");
			assertEquals(9, tMap.mOutputFluidCount, "OUT-FLUID");
			assertEquals(0, tMap.mMinimalInputFluids, "MIN-FLUID");
			assertEquals(1, tMap.mMinimalInputs, "MIN");
			assertEquals(1, tMap.mPower, "AMP");
		}
		assertEquals("gt6:textures/gui/machines/distillationtower.png", GT6RecipeMaps.DISTILLATION_TOWER.mGUIPath, "the lowercased upstream GUI word (+ the ctor .png)");
		assertEquals("gt6:textures/gui/machines/cryodistillationtower.png", GT6RecipeMaps.CRYO_DISTILLATION_TOWER.mGUIPath, "the lowercased upstream GUI word (+ the ctor .png)");
	}
}
