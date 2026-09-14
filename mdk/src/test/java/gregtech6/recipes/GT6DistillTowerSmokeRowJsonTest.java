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
 * The tower smoke-row datapack acceptance (task p29-w3-distill-crucible, the OFFLINE half):
 * the two committed {@code data/gt6/recipe_maps/<map>.json} files —
 * {@code distillationtower} (gt6:oil → gt6:creosote fraction row) and
 * {@code cryodistillationtower} (water → ice freeze row) — pour through the PUBLIC
 * {@link GT6RecipeMapJsonLoader#pour} seam into their maps (the GT6EuHuSmokeRowJsonTest
 * fixture convention), and the map-key anchors resolve (the consumer-card JSON
 * direct-pour seam). The map constants rows carry the RM.java:65/:66 columns.
 */
class GT6DistillTowerSmokeRowJsonTest extends GTRecipesOfflineTestBase {

	/** The ids the two smoke rows carry. */
	private static final Function<ResourceLocation, Item> ITEM_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:ice" -> Items.ICE;
		default -> null;
	};

	private static final Function<ResourceLocation, Fluid> FLUID_FIXTURE = aId -> switch (aId.toString()) {
		case "gt6:oil" -> Fluids.WATER; // the stand-in for the port oil fluid (the row's id is what the test pins)
		case "gt6:creosote" -> Fluids.LAVA;
		case "minecraft:water" -> Fluids.WATER;
		default -> null;
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
	public void theTwoSmokeRowsPourThroughTheLoaderSeam() throws Exception {
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "distillationtower"), resource("distillationtower.json"));
		tData.put(new ResourceLocation("gt6", "cryodistillationtower"), resource("cryodistillationtower.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("distillationtower"), "one tower smoke row");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("cryodistillationtower"), "one cryo smoke row");
	}

	@Test
	public void thePouredRowIsFindableThroughTheTowerLookup() throws Exception {
		// the tower's own lookup shape (the checkRecipe override): one empty item slot, the
		// input-tank snapshot, size mInputMax
		Map<ResourceLocation, JsonElement> tData = new HashMap<>();
		tData.put(new ResourceLocation("gt6", "distillationtower"), resource("distillationtower.json"));
		GT6RecipeMapJsonLoader.pour(tData);

		Recipe tFound = GT6RecipeMaps.DISTILLATION_TOWER.findRecipe(null, 1024, ItemStack.EMPTY,
				new FluidStack[]{new FluidStack(Fluids.WATER, 1000)}, ItemStack.EMPTY);
		assertNotNull(tFound, "the pure-fluid row must be findable with an all-empty item array");
		assertEquals(120, tFound.mEUt);
		assertEquals(160, tFound.mDuration);
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
