package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.components.OMComponentFaceTest;
import gregtech6.recipes.GTRecipesOfflineTestBase;

/**
 * The raw→finished tool-head FILE BELT pin test (task toolhead-r11a-file-belt — the C2
 * conversion route the r11 research closed as a true broken chain: the upstream
 * OreProcessing_CraftFrom listener half at Loader_Recipes_Handlers.java:420-434 that the
 * p37 census missed by circling Loader_OreProcessing only). The upstream canon is FIFTEEN
 * shaped rows over one grid — {"X ", " f"} = the raw head (X) + a file tool (f) → the
 * finished head — twelve one-row families (:423-434, saw/chisel/sword/pickaxe/shovel/
 * spade/universalSpade/axe/axeDouble/hoe/sense/plow) plus the arrow trio (:420 gemChipped
 * ×2, :421 rockGt ×8, :422 rawArrow ×1).
 *
 * <p>Two faces here, the census the r9-39 rows band rides as well:
 *
 * <ul>
 * <li>the GENERATED-TREE existence census — the fifteen {@code from_raw_*}/arrow form
 * directories each carry at least one row JSON in the canonical datagen tree (the red→green
 * pin: zero rows before the band, fifteen forms after);</li>
 * <li>the OFFLINE RecipeManager spot check — the raw saw head + a file-class tool really
 * crafts the finished saw head through a real RecipeManager loaded from the generated JSON
 * (acceptance ③, the GTAdvancedCraftingTableTest TestRecipeManager form).</li>
 * </ul>
 */
public class GT6FileBeltDatagenTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void initMaterialSystem() {
		// the boot order matters: the inherited GTRecipesOfflineTestBase boot runs FIRST, then
		// the material universe fills (the GT6CraftFromDatagenTest seam — the spot-check's walk
		// and the probe ids both ride it)
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	/** The fifteen upstream form keys (the row-id leaves under data/gt6/recipes/, the craftFromRowId law). */
	private static final List<String> FORM_KEYS = List.of(
			"tool_head_arrow/from_gem_chipped", // :420
			"tool_head_arrow/from_rock_gt", // :421
			"tool_head_arrow/from_raw_arrow", // :422
			"tool_head_saw/from_raw_saw", // :423
			"tool_head_chisel/from_raw_chisel", // :424
			"tool_head_sword/from_raw_sword", // :425
			"tool_head_pickaxe/from_raw_pickaxe", // :426
			"tool_head_shovel/from_raw_shovel", // :427
			"tool_head_spade/from_raw_spade", // :428
			"tool_head_universal_spade/from_raw_universal_spade", // :429
			"tool_head_axe/from_raw_axe", // :430
			"tool_head_axe_double/from_raw_axe_double", // :431
			"tool_head_hoe/from_raw_hoe", // :432
			"tool_head_sense/from_raw_sense", // :433
			"tool_head_plow/from_raw_plow"); // :434

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir (the GT6AssetCoverageGuardTest anchor). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (java.nio.file.Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	private static Path recipesTree() {
		return mdkRoot().resolve("src/generated/resources/data/gt6/recipes");
	}

	/**
	 * The generated-tree existence census: every one of the fifteen forms carries at least
	 * one row JSON (plural recipes/ tree — the recipe/ singular mirror and the advancement
	 * twins ride the same save() call), and the poured total rides the measured walk face.
	 */
	@Test
	public void theFileBeltRowsExistInTheGeneratedTree() throws IOException {
		Path tTree = recipesTree();
		assertTrue(Files.isDirectory(tTree), "the generated recipes tree exists: " + tTree);
		List<String> tMissing = new ArrayList<>();
		int tTotal = 0;
		for (String tKey : FORM_KEYS) {
			Path tDir = tTree.resolve(tKey);
			List<Path> tRows = new ArrayList<>();
			if (Files.isDirectory(tDir)) {
				try (Stream<Path> tWalk = Files.list(tDir)) {
					tWalk.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(tRows::add);
				}
			}
			if (tRows.isEmpty()) tMissing.add(tKey);
			tTotal += tRows.size();
		}
		assertEquals(List.of(), tMissing, "the fifteen file-belt forms must all pour rows (the :420-434 belt)");
		assertEquals(6314, tTotal, "the poured total == the walk census (95 + 79 + 539 + 4x312 + 8x544; +1 task gem-ice-force-rows — the chipped-face arrow row for Ice)");
	}

	/** The generated row JSON off the canonical tree — the LEG DIALECT band (the recipe-parse face: the forge codec eats the plural recipes/ "item" form, the 21.1 codec the singular recipe/ "id" form — the ops-rundata-leg-canonical dialect law). */
	static JsonObject generatedRow(String aKey, String aMaterial) throws IOException {
		//? if forge {
		Path tRow = recipesTree().resolve(aKey).resolve(aMaterial + ".json");
		//?} else {
		/*Path tRow = recipesTree().getParent().resolve("recipe").resolve(aKey).resolve(aMaterial + ".json");
		*///?}
		assertTrue(Files.isRegularFile(tRow), tRow + " rides the generated tree");
		return JsonParser.parseString(Files.readString(tRow, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	// ------------------------------------------------------------------
	// acceptance ③ — the offline RecipeManager spot check: the raw saw head
	// + a file-class tool really crafts the finished saw head (the
	// GTAdvancedCraftingTableTest TestRecipeManager face over the GENERATED
	// row JSON; the representative material walks, no dead name)
	// ------------------------------------------------------------------

	@Test
	public void theRawSawHeadAndAFileCraftTheFinishedSawHead() throws Exception {
		// the representative row (the first from_raw_saw walk row) and its generated JSON
		GT6CraftingRecipes.FileBeltCraftFromMaterialRow tWalk = GT6CraftingRecipes.fileBeltCraftFromMaterialRows().stream()
				.filter(r -> r.aForm().aKey().equals("tool_head_saw/from_raw_saw")).findFirst().orElseThrow();
		String tMat = gregtech6.registry.GTMaterialItems.snakeCase(tWalk.aMaterial().mNameInternal);
		JsonObject tJson = generatedRow("tool_head_saw/from_raw_saw", tMat);

		// the offline identity trio under the exact generated ids + a file tool. On the FML-booted
		// neo JVM the REAL mod items are already registered (the same ids), on the plain-boot
		// forge JVM they are not — itemOrProbe covers both (the ItemStack ctor resolves the
		// registry delegate eagerly, so the id must exist one way or the other)
		Item tRaw = itemOrProbe("tool_head_raw_saw_" + tMat);
		Item tDone = itemOrProbe("tool_head_saw_" + tMat);
		Item tFile = itemOrProbe("probe_file_belt");
		// the file-tag bind — bindTags has no frozen gate (MappedRegistry.java:374), and the
		// offline boot binds no item tags, so the single-entry map clobbers nothing
		net.minecraft.core.registries.BuiltInRegistries.ITEM.bindTags(
				java.util.Map.of(GT6ItemTags.TOOLS_FILE, java.util.List.of(tFile.builtInRegistryHolder())));

		// a real RecipeManager over the generated JSON
		GTRecipesOfflineTestBase.TestRecipeManager tManager = new GTRecipesOfflineTestBase.TestRecipeManager();
		tManager.load(java.util.Map.of(resourceId("tool_head_saw/from_raw_saw/" + tMat), (com.google.gson.JsonElement) tJson));
		GTRecipesOfflineTestBase.MinimalLevel tLevel = new GTRecipesOfflineTestBase.MinimalLevel(tManager);

		// the {"X ", " f"} grid: the raw head top-left, the file bottom-right
		//? if forge {
		TransientCraftingContainer tGrid = new TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(
				net.minecraft.world.inventory.MenuType.GENERIC_9x3, 0) {
			@Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player aPlayer, int aIndex) {
				return net.minecraft.world.item.ItemStack.EMPTY;
			}
			@Override public boolean stillValid(net.minecraft.world.entity.player.Player aPlayer) {
				return true;
			}
		}, 2, 2);
		tGrid.setItem(0, new ItemStack(tRaw));
		tGrid.setItem(3, new ItemStack(tFile));
		var tRecipe = tManager.getRecipeFor(RecipeType.CRAFTING, tGrid, tLevel).orElseThrow();
		ItemStack tOut = tRecipe.assemble(tGrid, tLevel.registryAccess());
		//?} else {
		/*java.util.List<ItemStack> tCells = new ArrayList<>(java.util.Collections.nCopies(4, ItemStack.EMPTY));
		tCells.set(0, new ItemStack(tRaw));
		tCells.set(3, new ItemStack(tFile));
		net.minecraft.world.item.crafting.CraftingInput tGrid = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, tCells);
		var tRecipe = tManager.getRecipeFor(RecipeType.CRAFTING, tGrid, tLevel).orElseThrow();
		ItemStack tOut = tRecipe.value().assemble(tGrid, tLevel.registryAccess());
		*///?}
		assertEquals(tDone, tOut.getItem(), "raw saw head + file crafts the finished saw head (the :423 row)");
		assertEquals(1, tOut.getCount(), "the :423 output amount");
	}

	/** The item under the exact generated id — the REAL registered one when the JVM has it (the FML boot), else the offline probe. */
	private static Item itemOrProbe(String aKey) {
		Item tExisting = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(resourceId(aKey)).orElse(null);
		return tExisting != null ? tExisting : probeItem(aKey);
	}

	/** The offline item probe under the exact generated id — folded onto the common
	 * definition OMComponentFaceTest.probeItem (gt6 namespace, bare Properties — task
	 * probeitem-latch-hygiene). */
	private static Item probeItem(String aKey) {
		return OMComponentFaceTest.probeItem("gt6", aKey, Item::new);
	}

	/** The row id (the leg-correct ResourceLocation ctor). */
	private static ResourceLocation resourceId(String aPath) {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
		*///?}
	}
}
