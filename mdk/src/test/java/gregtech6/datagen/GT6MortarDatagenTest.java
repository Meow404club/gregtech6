/**
 * The mortar datagen census (task mortar-family, the GT6CupDatagenTest shape): the five
 * registration rows (the Loader :2179-2183 columns), the 12-PNG two-layer borrow 1:1 with
 * the assets/README.md sha256 ledger, the generated tree (five blockstates -> five model
 * files, the item parents, the model geometry + tint-index census), the five shapeless
 * crafting pairs (the iron-column quirk on the steel row pinned), and the lang band on
 * both locales. The generated tree reads off the test classpath (src/generated/resources).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GT6MortarDatagenTest extends gregtech6.tileentity.GTOfflineTestBase {

	private static final List<String> PATHS = List.of("mortar_steel", "mortar_netherite",
			"mortar_sapphire", "mortar_diamond", "mortar_amethyst");
	/** The pestle materials in DESIGN order (MultiTileEntityMortar.java:59). */
	private static final List<String> PESTLE_SLUGS = List.of("Steel", "Netherite", "Sapphire", "Diamond", "Amethyst");
	/** The crafting ingredients in row order (the 'P' column: :2179 ANY.Iron — NOT the pestle — then :2180-2183). */
	private static final List<String> INGREDIENTS = List.of("ingot_iron", "ingot_netherite",
			"gem_sapphire", "gem_diamond", "gem_amethyst");
	/** 2 layers x 6 faces — the borrowed tile set (assets/README.md, the mortar section). */
	private static final int PINNED_PNG_TOTAL = 12;
	private static final List<String> FACES = List.of("sides", "insides", "top", "bottom", "middleside", "middletop");

	@BeforeAll
	static void bootMaterials() {
		// the vanilla boot comes from the base; the gregapi material registry populates on
		// the mod lifecycle — the GT6CupDatagenTest shape
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	/** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
		}
		throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** One committed generated-tree JSON as an object (the test classpath carries src/generated/resources).
	 *  The leading slash is stripped HERE: {@link ClassLoader#getResourceAsStream} does not
	 *  normalise it (unlike the class-relative {@link Class#getResource}) — a "/"-prefixed
	 *  name never matches any classpath entry (the observed 4-null sweep, the cup form). */
	private static JsonObject generatedJson(String aPath) throws Exception {
		String tPath = aPath.startsWith("/") ? aPath.substring(1) : aPath;
		try (InputStream tStream = GT6MortarDatagenTest.class.getClassLoader().getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + tPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ---------------------------------------------------------------- the registration rows

	/** The five rows in upstream ID order with their DESIGN columns and the pestle/ingredient derivations. */
	@Test
	void theRowsAreTheLoaderMortarRows() {
		assertEquals(5, gregtech6.registry.GT6Mortars.ROWS.size(), "the five :2179-2183 rows");
		for (int i = 0; i < 5; i++) {
			gregtech6.registry.GT6Mortars.MortarRow tRow = gregtech6.registry.GT6Mortars.ROWS.get(i);
			assertEquals(i, tRow.design(), "the row index IS the NBT_DESIGN (the registration order)");
			assertEquals(PATHS.get(i), tRow.path(), "the registry path order");
			assertEquals(PESTLE_SLUGS.get(i), tRow.pestle().get().mNameInternal, "the MORTAR_MATERIALS row");
		}
		// the crafting derivations: the DESIGN-0 STEEL-pestle row crafts from an IRON ingot
		// (:2179 — the upstream 'P' column), the rest from their pestle materials
		assertEquals(gregapi.data.OP.ingot, gregtech6.registry.GT6Mortars.pestlePrefix(gregtech6.registry.GT6Mortars.ROWS.get(0)));
		assertEquals(gregapi.data.OP.ingot, gregtech6.registry.GT6Mortars.pestlePrefix(gregtech6.registry.GT6Mortars.ROWS.get(1)));
		assertEquals(gregapi.data.OP.gem, gregtech6.registry.GT6Mortars.pestlePrefix(gregtech6.registry.GT6Mortars.ROWS.get(2)));
		assertEquals(gregapi.data.MT.Iron, gregtech6.registry.GT6Mortars.pestleIngredient(gregtech6.registry.GT6Mortars.ROWS.get(0)),
				"the :2179 quirk — the steel-pestle row crafts from ANY.Iron");
		assertEquals(gregapi.data.MT.Netherite, gregtech6.registry.GT6Mortars.pestleIngredient(gregtech6.registry.GT6Mortars.ROWS.get(1)));
		assertEquals(gregapi.data.MT.Amethyst, gregtech6.registry.GT6Mortars.pestleIngredient(gregtech6.registry.GT6Mortars.ROWS.get(4)));
	}

	// ---------------------------------------------------------------- the PNG borrow census

	@Test
	void everyDeclaredFaceHasItsBorrowedPng() {
		for (String tFace : FACES) {
			assertNotNull(GT6MortarDatagenTest.class.getResource("/assets/gt6/textures/block/tools/mortar/" + tFace + ".png"),
					"colored " + tFace + ".png must be borrowed");
			assertNotNull(GT6MortarDatagenTest.class.getResource("/assets/gt6/textures/block/tools/mortar_overlay/" + tFace + ".png"),
					"overlay " + tFace + ".png must be borrowed");
		}
	}

	/** The attribution nail: every borrowed PNG is named in assets/README.md AND hashes to a sha256 the ledger records. */
	@Test
	void borrowedPngsAreGroundedInTheAssetsLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		List<Path> tPngs = new ArrayList<>();
		tPngs.add(Path.of(GT6MortarDatagenTest.class.getResource("/assets/gt6/textures/block/tools/mortar").toURI()));
		tPngs.add(Path.of(GT6MortarDatagenTest.class.getResource("/assets/gt6/textures/block/tools/mortar_overlay").toURI()));
		List<String> tViolations = new ArrayList<>();
		int tWalked = 0;
		for (Path tDir : tPngs) {
			try (var tWalk = Files.walk(tDir)) {
				for (Path tPng : tWalk.filter(p -> p.toString().endsWith(".png")).toList()) {
					tWalked++;
					String tName = tPng.getFileName().toString();
					String tHex = java.util.HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
					if (!tReadme.contains(tName)) tViolations.add(tName + " — filename absent from assets/README.md");
					if (!tReadme.contains(tHex)) tViolations.add(tName + " — bytes hash to " + tHex + ", not grounded in the ledger");
				}
			}
		}
		assertEquals(PINNED_PNG_TOTAL, tWalked, "the borrow is exactly the declared 12-tile set — zero strays");
		assertTrue(tViolations.isEmpty(), "borrowed mortar texture without attribution: " + tViolations);
	}

	// ---------------------------------------------------------------- the generated tree

	/** The five blockstates bind 1:1 to five DISTINCT model files; the item models parent their row model. */
	@Test
	void theGeneratedTreeBindsEachVariantToItsOwnModel() throws Exception {
		Set<String> tModelIds = new HashSet<>();
		for (String tPath : PATHS) {
			JsonObject tBlockstate = generatedJson("/assets/gt6/blockstates/" + tPath + ".json");
			String tModelRef = tBlockstate.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString();
			assertEquals("gt6:block/" + tPath, tModelRef, tPath + " binds its own model file (pairwise distinct bindings)");
			tModelIds.add(tModelRef);
			JsonObject tItem = generatedJson("/assets/gt6/models/item/" + tPath + ".json");
			assertEquals("gt6:block/" + tPath, tItem.get("parent").getAsString(), tPath + " item parents its row model");
		}
		assertEquals(5, tModelIds.size(), "five distinct model files — the variant row never collapses");
	}

	/** The model geometry: 12 elements (6 body + 6 overlay shells), cutout, the tint-index census, the tile band. */
	@Test
	void theModelGeometryIsThePassTableVerbatim() throws Exception {
		JsonObject tModel = generatedJson("/assets/gt6/models/block/mortar_steel.json");
		assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "the overlay shells must discard");
		var tElements = tModel.getAsJsonArray("elements");
		assertEquals(12, tElements.size(), "floor + 4 walls + pestle, each with its 0.01 overlay shell");
		int tTint0 = 0, tTint1 = 0, tUntinted = 0;
		Set<String> tTextures = new HashSet<>();
		for (var tElement : tElements) {
			for (var tEntry : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
				JsonObject tFace = tEntry.getValue().getAsJsonObject();
				tTextures.add(tFace.get("texture").getAsString());
				if (tFace.has("tintindex")) {
					int tIndex = tFace.get("tintindex").getAsInt();
					if (tIndex == 0) tTint0++;
					else if (tIndex == 1) tTint1++;
					else tUntinted++;
				} else tUntinted++;
			}
		}
		assertEquals(22, tTint0, "the Ceramic body faces (the tintindex-0 seat)");
		assertEquals(5, tTint1, "the pestle faces (the tintindex-1 NBT_DESIGN seat: up + 4 sides)");
		assertEquals(27, tUntinted, "the overlay shells (the P22 decal contract)");
		assertTrue(tTextures.contains("#middleside") && tTextures.contains("#middletop"),
				"the pestle's middle tiles ride the model");
		assertTrue(tTextures.contains("#overlay_middleside") && tTextures.contains("#overlay_top"),
				"the two-layer overlay band rides the model");
	}

	// ---------------------------------------------------------------- the crafting rows

	/** The five shapeless pairs verbatim: mixing_bowl + the 'P' ingredient (the :2179 iron quirk pinned). */
	@Test
	void theCraftingRowsAreTheShapelessPairs() throws Exception {
		for (int i = 0; i < 5; i++) {
			JsonObject tRecipe = generatedJson("/data/gt6/recipes/" + PATHS.get(i) + ".json");
			assertEquals("minecraft:crafting_shapeless", tRecipe.get("type").getAsString(), PATHS.get(i) + " is shapeless");
			var tIngredients = tRecipe.getAsJsonArray("ingredients");
			assertEquals(2, tIngredients.size(), "the 'P','B' pair");
			assertEquals("gt6:mixing_bowl", tIngredients.get(0).getAsJsonObject().get("item").getAsString(),
					"the base = the Ceramic-bowl stand-in");
			assertEquals("gt6:" + INGREDIENTS.get(i), tIngredients.get(1).getAsJsonObject().get("item").getAsString(),
					PATHS.get(i) + " crafts from the upstream 'P' ingredient");
			assertEquals("gt6:" + PATHS.get(i), tRecipe.getAsJsonObject("result").get("item").getAsString(),
					"the result-path convention");
		}
	}

	// ---------------------------------------------------------------- the lang band

	/** Both locales carry the five keys; the shared dump-verbatim names (Mortar / 研钵). */
	@Test
	void theLangBandShipsOnBothLocales() throws Exception {
		JsonObject tEn = generatedJson("/assets/gt6/lang/en_us.json");
		JsonObject tZh = generatedJson("/assets/gt6/lang/zh_cn.json");
		for (String tPath : PATHS) {
			String tKey = "block.gt6." + tPath;
			assertEquals("Mortar", tEn.get(tKey).getAsString(), "the upstream row name verbatim");
			assertEquals("研钵", tZh.get(tKey).getAsString(), "the dump-verbatim shared name (tmp/gregtech.lang:13556 band)");
		}
	}
}
