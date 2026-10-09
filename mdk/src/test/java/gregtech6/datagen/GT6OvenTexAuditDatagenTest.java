/*
 * The oven texture audit + borrow pins (task oven-texture-borrow): the R-H ⑪ segment
 * suspected the six {@code oven_colored_<face>.png} bodies were script-generated
 * near-white placeholders. The pixel census DISPROVED it — they are byte-identical
 * upstream borrows (task basicmachine-family / paint-front-overlay-split, ledger
 * README.md:8353-8357) and the upstream colored layer IS the generic light-gray
 * machine plate (identical across all six faces and all six basic-machine families,
 * CC0; the in-game colour comes from the tintindex-0 mRGBa multiply). These tests
 * nail both halves of the finding:
 * <ul>
 * <li>the six oven bodies stay the documented upstream bytes (the sha256 ledger
 *     digests) and measure far below the >95%-near-white placeholder signature;</li>
 * <li>the white-texture census over the block domain (the >95% near-white scan, the
 *     audit deliverable) finds ONLY the two documented upstream borrows — a new white
 *     block texture without a ledger row fails here.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class GT6OvenTexAuditDatagenTest {

	/** The six upstream borrow digests (README.md:8353-8357 — all six faces share the generic plate byte-for-byte). */
	private static final String UPSTREAM_DIGEST = "db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e";

	private static final List<String> OVEN_FACES = List.of("front", "back", "left", "right", "top", "bottom");

	/**
	 * The audit census result (2026-09-30, task oven-texture-borrow): the ONLY >95%
	 * near-white block-domain textures, every one a DOCUMENTED upstream borrow that is
	 * genuinely white in 1.7.10 too (the paint-tint renders it):
	 * {@code placeable/plate_gem_top.png} (README.md:9087-9107); the logistics tank
	 * three colored faces (README.md:9882-9886, task tank-render-tint's two-layer
	 * grammar section — the era that RETIRED the original {@code barrel_logistics.png}
	 * item-domain borrow this audit originally whitelisted, its successor
	 * {@code barrel_parts/logistics/colored_side.png} byte-joins it per the ledger); and
	 * the boiler barometer needle states 00..31 (task boiler-barometer's
	 * `block/barometer/` README ledger — the 4-white-texels needle art, the dial base
	 * itself is NOT near-white and stays outside this set; the rebase-drift extension
	 * when that card and tank-render-tint landed on main after this audit first ran);
	 * and {@code decor/glass_clear.png} (task material-mc-g2-decor-misc, the
	 * `block/decor/` README ledger — the upstream GLASS_CLEAR grayscale fill,
	 * alpha-48 translucent, white by design and dye-tinted at render by
	 * GT6DecorTintListener).
	 * A new white block texture must be added here WITH its ledger row, or reworked.
	 */
	private static final Set<String> DOCUMENTED_WHITE_BLOCK_BORROWS;
	static {
		Set<String> tWhite = new java.util.HashSet<>(Set.of(
				"placeable/plate_gem_top.png",
				"barrel_parts/logistics/colored_side.png",
				"barrel_parts/logistics/colored_top.png",
				"barrel_parts/logistics/colored_bottom.png",
				// task block-family-32xxx-port — the plant pot's near-white body art (the
				// :2229 Ceramic column is the tint carrier; the README 32xxx sha section)
				"plant_pot/colored_side.png",
				"plant_pot/colored_bottom.png",
				"decor/glass_clear.png",
				// task storage-massstorage: the logistics mass storage body art — the
				// upstream dedicated iconset's near-white body (the loader row paints it
				// MT.Black through the tint dispatch, Loader :142), the README
				// "Item mass storage block textures" ledger section
				"massstorage_logistics/colored_side.png",
				"massstorage_logistics/colored_top.png",
				"massstorage_logistics/colored_bottom.png",
				"massstorage_logistics/colored_front.png",
				"massstorage_logistics/colored_back.png"));
		for (int i = 0; i < 32; i++) tWhite.add(String.format("barometer/%02d.png", i));
		DOCUMENTED_WHITE_BLOCK_BORROWS = java.util.Collections.unmodifiableSet(tWhite);
	}

	/** The upstream materialicon tint-overlay family (borrow-item-material-sets-a/b): white by design, tinted at render. */
	private static final int MATERIAL_SETS_WHITE_OVERLAYS = 190;

	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	private static Path blockTexturesDir() {
		return mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "gt6", "textures", "block"));
	}

	private static String sha256(Path aFile) throws IOException {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(Files.readAllBytes(aFile)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** The placeholder signature: share of opaque pixels with r,g,b all > 230. Fully transparent (a blank upstream overlay slot, renders nothing) is 0 — it cannot LOOK white. */
	private static double nearWhiteRatio(Path aFile) throws IOException {
		BufferedImage tImage = ImageIO.read(aFile.toFile());
		assertNotNull(tImage, "decodable PNG: " + aFile);
		int tOpaque = 0, tNearWhite = 0;
		for (int y = 0; y < tImage.getHeight(); y++) {
			for (int x = 0; x < tImage.getWidth(); x++) {
				int tArgb = tImage.getRGB(x, y);
				if ((tArgb >>> 24) == 0) continue;
				tOpaque++;
				if ((tArgb >> 16 & 255) > 230 && (tArgb >> 8 & 255) > 230 && (tArgb & 255) > 230) tNearWhite++;
			}
		}
		return tOpaque == 0 ? 0.0 : (double) tNearWhite / tOpaque;
	}

	@Test
	public void ovenColoredFacesAreTheDocumentedUpstreamBorrowBytes() throws IOException {
		for (String tFace : OVEN_FACES) {
			Path tFile = blockTexturesDir().resolve("oven_colored_" + tFace + ".png");
			assertTrue(Files.isRegularFile(tFile), "the borrowed body PNG exists: " + tFile.getFileName());
			assertEquals(UPSTREAM_DIGEST, sha256(tFile),
					tFile.getFileName() + ": byte-identical to upstream "
							+ "basicmachines/oven/colored/" + tFace + ".png (the ledger digest)");
			BufferedImage tImage = ImageIO.read(tFile.toFile());			assertEquals(16, tImage.getWidth());
			assertEquals(16, tImage.getHeight());
		}
	}

	@Test
	public void ovenColoredFacesAreNotWhitePlaceholders() throws IOException {
		for (String tFace : OVEN_FACES) {
			double tRatio = nearWhiteRatio(blockTexturesDir().resolve("oven_colored_" + tFace + ".png"));
			assertTrue(tRatio < 0.95,
					tFace + ": near-white share " + String.format("%.1f%%", 100 * tRatio)
							+ " — below the >95% script-placeholder signature (the R-H ⑪ suspicion is disproven;"
							+ " a regression to a flat white here must fail)");
		}
	}

	@Test
	public void whiteBlockTextureCensusFindsOnlyTheDocumentedBorrows() throws IOException {
		Set<String> tWhite = new HashSet<>();
		int tWalked = 0;
		try (Stream<Path> tWalk = Files.walk(blockTexturesDir())) {
			for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".png")).toList()) {
				tWalked++;
				if (nearWhiteRatio(tFile) > 0.95) {
					tWhite.add(blockTexturesDir().relativize(tFile).toString());
				}
			}
		}
		assertTrue(tWalked > 1000, "the census walk is non-vacuous (walked " + tWalked + " block PNGs)");
		Set<String> tUndocumented = new HashSet<>(tWhite);
		tUndocumented.removeAll(DOCUMENTED_WHITE_BLOCK_BORROWS);
		assertTrue(tUndocumented.isEmpty(), "undocumented near-white block texture(s): " + tUndocumented
				+ " — add a README ledger row AND extend DOCUMENTED_WHITE_BLOCK_BORROWS, or rework the texture");
		assertTrue(tWhite.containsAll(DOCUMENTED_WHITE_BLOCK_BORROWS),
				"the two documented borrows still measure white (drift is fine, but say so in the ledger)");
	}

	@Test
	public void materialSetsWhiteOverlaysAreTheUpstreamTintSystem() throws IOException {
		// the >95%-near-white population OUTSIDE the block domain lives exclusively in
		// item/material_sets — the upstream white-on-transparent tint overlays (rendered
		// over the material icon). Count drift = a new set wave landed: re-run the audit
		// census and re-pin the number (the borrow manifests carry the bytes).
		int tCount = 0;
		Path tSets = blockTexturesDir().getParent().resolve("item").resolve("material_sets");
		try (Stream<Path> tWalk = Files.walk(tSets)) {
			for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".png")).toList()) {
				if (nearWhiteRatio(tFile) > 0.95) tCount++;
			}
		}
		assertEquals(MATERIAL_SETS_WHITE_OVERLAYS, tCount,
				"the material_sets white-overlay census moved — re-run the audit and update the README census note");
	}
}
