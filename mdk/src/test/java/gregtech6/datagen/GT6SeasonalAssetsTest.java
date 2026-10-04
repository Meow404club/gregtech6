/**
 * The offline asset census of the Christmas + seasonal pack (task easter-s3-xmas-seasonal,
 * the S3 card of research.easter-egg-census id1451). Pins, pure-JUnit (the
 * GT6TextureCensusTest file-walking posture, no registry, no datagen run):
 *
 * <ul>
 * <li>the six borrowed upstream art files (the XMAS blue-spruce leaves texture + its
 *     animation mcmeta, the four seasonal maple leaves textures) are byte-identical to
 *     their upstream sources (the sha256 ledger face, assets/README.md rows this card);</li>
 * <li>the two base leaves PNGs are UNTOUCHED — the seasonal swap is bake-time model
 *     substitution (the coordinator-approved route), never an overwrite of the borrowed
 *     base art;</li>
 * <li>the five seasonal variant models exist in the generated tree over the right
 *     textures with the leaves render type (the datagen product the
 *     GT6SeasonalLeafClientListener bake swap consumes);</li>
 * <li>the two leaves BLOCKSTATE JSONs stay plain — no seasonal variants leak into the
 *     blockstate layer (upstream swaps the icon pointer, the blockstates never change).</li>
 * </ul>
 *
 * <p>Upstream anchors: GT_API_Proxy_Client.java:133-142 (the XMAS icon swap,
 * XMAS_IN_JULY / XMAS_IN_DECEMBER) and :144-167 (the monthly maple icon swap);
 * Textures.java:159 (the LEAVES_BLUESPRUCE_XMAS / LEAVES_MAPLE_* icon names — the port
 * borrows exactly those iconsets PNGs). The OPAQUE twin textures (LEAVES_OPAQUE_*) are
 * deliberately NOT borrowed: the port has one cutout_mipped leaves carrier per species,
 * the upstream opaque array face (LEAVES_CD[8] / LEAVES_AB[9]) has no carrier here.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6SeasonalAssetsTest {

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
			}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** The borrowed-art file sha256s, over the upstream iconsets sources (snapshot v6.17.06-22-g3703e4030). */
	private static final String[][] BORROWED = {
		{"assets/gt6/textures/block/tree/leaves_blue_spruce_xmas.png",
		 "9314eb232f6cfa14eb0bd370a05fd688497f6f40e251a374ee7ffa2879a3a36e",
		 "LEAVES_BLUESPRUCE_XMAS.png"},
		{"assets/gt6/textures/block/tree/leaves_blue_spruce_xmas.png.mcmeta",
		 "6fa30d635125baf1212840c239add6b9e7f0d5be8e11094191f950c557881913",
		 "LEAVES_BLUESPRUCE_XMAS.png.mcmeta (the 4-frame animation, frametime 16)"},
		{"assets/gt6/textures/block/tree/leaves_maple_brown.png",
		 "5e1f82deed20d1c7b785d2ca199bbeb2b2087e7cf45d2c96d9dc40edb3bdf9f0",
		 "LEAVES_MAPLE_BROWN.png"},
		{"assets/gt6/textures/block/tree/leaves_maple_yellow.png",
		 "d7baac9ba09e7acb601f555013f6484ba6e7c48e7d51d304b047f8bc2fa41ec5",
		 "LEAVES_MAPLE_YELLOW.png"},
		{"assets/gt6/textures/block/tree/leaves_maple_orange.png",
		 "a8d2d23566aa8ec8288f3430fcea56d6e106d708fa6819bfef9a11d6754c3d3b",
		 "LEAVES_MAPLE_ORANGE.png"},
		{"assets/gt6/textures/block/tree/leaves_maple_red.png",
		 "0a84a730f2fc37fb8a98edb5cc9292e0b5d05ad03a8f52259d4c8e3f41566432",
		 "LEAVES_MAPLE_RED.png"},
	};

	private static String sha256(Path aPath) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(aPath)));
	}

	@Test
	void borrowedSeasonalArtIsByteIdentical() throws Exception {
		Path tRoot = mdkRoot();
		for (String[] tRow : BORROWED) {
			Path tFile = tRoot.resolve("src/main/resources").resolve(tRow[0]);
			assertTrue(Files.isRegularFile(tFile), tRow[0] + " missing (upstream " + tRow[2] + ")");
			assertEquals(tRow[1], sha256(tFile), tRow[0] + " drifted from upstream " + tRow[2]);
		}
	}

	@Test
	void baseLeavesArtIsUntouched() throws Exception {
		// the pre-existing borrows stay byte-identical: the seasonal face REPLACES the baked
		// model at client bake time, never the base PNG (an overwrite would lose the plain-months art)
		Path tTree = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/tree");
		assertEquals("abfd1a7b9ed372353f119a6d1bbefb99a63086cf5904f297862c48cae1da3679",
				sha256(tTree.resolve("leaves_blue_spruce.png")), "the blue spruce base");
		assertEquals("1cd28c8bc82579ad6d81d8ca9a95f8a46b9e3ccf9fea83e7763543459643cbd1",
				sha256(tTree.resolve("leaves_maple.png")), "the maple base");
	}

	/** The five seasonal variant models: name -> the texture the "all" face must bind. */
	private static final String[][] SEASONAL_MODELS = {
		{"blue_spruce_leaves_xmas", "gt6:block/tree/leaves_blue_spruce_xmas"},
		{"maple_leaves_brown", "gt6:block/tree/leaves_maple_brown"},
		{"maple_leaves_yellow", "gt6:block/tree/leaves_maple_yellow"},
		{"maple_leaves_orange", "gt6:block/tree/leaves_maple_orange"},
		{"maple_leaves_red", "gt6:block/tree/leaves_maple_red"},
	};

	@Test
	void seasonalVariantModelsExistOverTheBorrowedTextures() throws Exception {
		Path tModels = mdkRoot().resolve("src/generated/resources/assets/gt6/models/block");
		for (String[] tRow : SEASONAL_MODELS) {
			Path tJson = tModels.resolve(tRow[0] + ".json");
			assertTrue(Files.isRegularFile(tJson), tRow[0] + " model missing from the generated tree");
			JsonObject tModel = JsonParser.parseString(Files.readString(tJson, StandardCharsets.UTF_8))
					.getAsJsonObject();
			assertEquals("minecraft:block/cube_all", tModel.get("parent").getAsString(),
					tRow[0] + ": the same cube_all geometry as the plain leaves model");
			assertEquals("minecraft:cutout_mipped", tModel.get("render_type").getAsString(),
					tRow[0] + ": the leaves render type (GT6BlockStates.addTreeBlocks face)");
			assertEquals(tRow[1], tModel.getAsJsonObject("textures").get("all").getAsString(),
					tRow[0] + ": the borrowed seasonal texture");
		}
	}

	@Test
	void leavesBlockstatesStayPlain() throws Exception {
		// the upstream face swaps the icon POINTER (Textures.BlockIcons array rows); the
		// blockstate layer never changes — pin the single plain variant so no seasonal
		// variant can leak into the blockstate JSONs
		Path tStates = mdkRoot().resolve("src/generated/resources/assets/gt6/blockstates");
		for (String tName : new String[] {"blue_spruce_leaves", "maple_leaves"}) {
			JsonObject tState = JsonParser.parseString(
					Files.readString(tStates.resolve(tName + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject tVariants = tState.getAsJsonObject("variants");
			assertEquals(1, tVariants.size(), tName + ": exactly the one plain variant");
			assertTrue(tVariants.has(""), tName + ": the plain \"\" variant (the simpleBlock form)");
			assertTrue(tVariants.get("").getAsJsonObject().get("model").getAsString().endsWith("/" + tName),
					tName + ": the plain model, not a seasonal twin");
		}
	}

	@Test
	void seasonalArtRowsAreLedgered() throws Exception {
		// acceptance 1: every borrowed seasonal file carries its assets/README.md attribution
		// row (name + sha256, the TextureCensusTest pin-e face for this card's wave)
		String tReadme = Files.readString(
				mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		for (String[] tRow : BORROWED) {
			String tName = Path.of(tRow[0]).getFileName().toString();
			assertTrue(tReadme.contains(tName), tName + " absent from the assets ledger");
			assertTrue(tReadme.contains(tRow[1]), tName + " sha256 not grounded in the assets ledger");
		}
	}
}
