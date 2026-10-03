/**
 * The derived kitchen NEI glyph census (task kitchen-nei-corner-jump): the two
 * viewer-dynamic corner tiles exist under block/tools/, carry the upstream NEI sheet's
 * tile structure (128x128, the 16px-cell text band y3..12), and bake the upstream render
 * tint (the opaque pixels are yellow-dominant with the anti-aliasing gray levels kept —
 * NOT one flat colour, the glyph must survive being squeezed onto the 2x2px rim-corner
 * quad). The assets/README.md "Derived" ledger rows pin both sha256 digests plus the
 * upstream derivation-source sheet, so a silent re-encode or re-draw turns this red.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

public class GT6KitchenNeiTextureCensusTest {

	private static final String[] TILES = {"kitchen_nei_jei", "kitchen_nei_emi"};
	private static final String JEI_SHA = "7a74e7a8e7e901d20024a417eef56c111afcb216d9eda46b3a43198000157ef0";
	private static final String EMI_SHA = "2c812935abf7245e00771d0457cb2684a2ecbd3c748e0a53c9a99dea3a0d09aa";
	private static final String UPSTREAM_SHEET_SHA = "40ea340701c4eac78eb39adee3a3b30047c189fb31e735813feb7b7308b380b2";

	/** The mdk root from the test working directory (the CropCardsTest.locateMdkRoot form). */
	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/datagen/GT6BlockStates.java"))) {
				return tDir;
			}
		}
		return null;
	}

	/** Both tiles: 128x128, the glyph band populated with the yellow-baked gray ramp (B==0, R==G, multiple levels). */
	@Test
	public void derivedGlyphTilesExistAndCarryTheYellowBakedRamp() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		for (String tName : TILES) {
			Path tTile = tMdk.resolve("src/main/resources/assets/gt6/textures/block/tools/" + tName + ".png");
			assertTrue(Files.isRegularFile(tTile), tName + " exists");
			BufferedImage tImage = ImageIO.read(tTile.toFile());
			assertEquals(128, tImage.getWidth(), tName + ": the upstream sheet's canvas width");
			assertEquals(128, tImage.getHeight(), tName + ": the upstream sheet's canvas height");
			Set<Integer> tLevels = new HashSet<>();
			for (int y = 0; y < 128; y++) {
				for (int x = 0; x < 128; x++) {
					int tP = tImage.getRGB(x, y);
					if ((tP >> 24 & 255) == 0) continue; // the transparent field
					int tR = tP >> 16 & 255, tG = tP >> 8 & 255, tB = tP & 255;
					assertEquals(0, tB, tName + ": blue channel empty — the CA_YELLOW_255 product");
					assertEquals(tR, tG, tName + ": the gray luminance rides R==G");
					tLevels.add(tR);
				}
			}
			assertTrue(tLevels.size() > 1, tName + ": the glyph is not one flat colour — the AA gray ramp survives (" + tLevels.size() + " levels)");
		}
	}

	/** The text band of the first cell spells the letters: populated from x2 (the upstream margin) and empty in the border ring. */
	@Test
	public void glyphBandSitsInTheUpstreamCellLayout() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		for (String tName : TILES) {
			BufferedImage tImage = ImageIO.read(
					tMdk.resolve("src/main/resources/assets/gt6/textures/block/tools/" + tName + ".png").toFile());
			int tBand = 0;
			for (int y = 3; y <= 12; y++) {
				for (int x = 2; x <= 13; x++) {
					if ((tImage.getRGB(x, y) >> 24 & 255) > 0) tBand++;
				}
			}
			assertTrue(tBand >= 20, tName + ": the 10px text band inside the first cell is populated (" + tBand + " px)");
			// the border ring stays transparent (the upstream cell margins verbatim)
			assertEquals(0, tImage.getRGB(0, 0) >> 24 & 255, tName + ": corner margin transparent");
			assertEquals(0, tImage.getRGB(15, 15) >> 24 & 255, tName + ": cell-edge margin transparent");
		}
	}

	/** The README "Derived" ledger rows pin both digests + the upstream derivation sheet. */
	@Test
	public void readmeLedgerPinsTheDigests() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		String tReadme = Files.readString(tMdk.resolve("src/main/resources/assets/README.md"));
		assertTrue(Pattern.compile("kitchen_nei_jei\\.png.*" + JEI_SHA).matcher(tReadme).find(),
				"the jei tile ledger row");
		assertTrue(Pattern.compile("kitchen_nei_emi\\.png.*" + EMI_SHA).matcher(tReadme).find(),
				"the emi tile ledger row");
		assertTrue(tReadme.contains(UPSTREAM_SHEET_SHA), "the upstream derivation sheet is recorded (NOT borrowed)");
	}
}
