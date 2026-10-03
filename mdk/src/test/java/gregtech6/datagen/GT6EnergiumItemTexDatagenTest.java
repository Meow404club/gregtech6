/*
 * The energium crystal sprite pins (task r11-energium-tint): the twelve LU
 * energium batteries (red ULV-IV + cyan ULV-IV, upstream Loader:1079-1084) borrow
 * BYTE-IDENTICAL grayscale crystal sprites (upstream lu/8/sides.png == lu/32/sides.png
 * byte-for-byte), and the upstream family colour rides the mRGBa render multiply
 * (MultiTileEntityBatteryLU8.getTexture2 BlockTextureDefault.get(sTextures, mRGBa);
 * MT.java:1581-1582 — EnergiumRed 255,0,0 / EnergiumCyan 0,255,255). The p29-w4 bake
 * copied the sprites without that multiply, so all twelve items rendered the SAME
 * grey-white crystal — the user's "占位纯白" report.
 *
 * <p>The fix is the bake (route a): the script docstring and the ledger
 * (assets/README.md, the battery-family row) both declare the port BAKES the
 * per-family sprite instead of running a runtime tint lane ("the tint lane is the
 * render-pool card"), so the energium rows now bake the mRGBa multiply into the two
 * PNGs. These tests pin that bake:
 * <ul>
 * <li>the red and cyan item sprites are DISTINCT files (the red pin — failed
 *     pre-fix, both were the identical grayscale borrow);</li>
 * <li>the multiply really happened per the vanilla tint math — the red sprite
 *     carries ALL its opaque chroma in the red channel (green/blue sums zero, the
 *     (v,0,0) texel form) and the cyan sprite in green+blue (the (0,v,v) form) —
 *     a regression to the grayscale borrow fails both.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class GT6EnergiumItemTexDatagenTest {

	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	private static Path energiumSprite(String aName) {
		return mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "gt6",
				"textures", "item", "battery", aName + ".png"));
	}

	/** The opaque-pixel channel sums — the (v,0,0)/(0,v,v) multiply form reads as two zero sums. */
	private static long[] channelSums(Path aFile) throws IOException {
		BufferedImage tImage = ImageIO.read(aFile.toFile());
		assertNotNull(tImage, "decodable PNG: " + aFile);
		long tR = 0, tG = 0, tB = 0;
		for (int y = 0; y < tImage.getHeight(); y++) {
			for (int x = 0; x < tImage.getWidth(); x++) {
				int tArgb = tImage.getRGB(x, y);
				if ((tArgb >>> 24) == 0) continue;
				tR += (tArgb >> 16) & 255;
				tG += (tArgb >> 8) & 255;
				tB += tArgb & 255;
			}
		}
		return new long[] {tR, tG, tB};
	}

	@Test
	public void energiumRedAndCyanSpritesAreDistinctFiles() throws IOException {
		Path tRed = energiumSprite("energium_red"), tCyan = energiumSprite("energium_cyan");
		assertTrue(Files.isRegularFile(tRed), "the red crystal sprite exists");
		assertTrue(Files.isRegularFile(tCyan), "the cyan crystal sprite exists");
		assertNotEquals(Arrays.hashCode(Files.readAllBytes(tRed)), Arrays.hashCode(Files.readAllBytes(tCyan)),
				"energium_red.png and energium_cyan.png must differ — the identical-grayscale "
						+ "borrow is the all-grey-white bug (upstream lu/8 == lu/32 byte-for-byte, "
						+ "the family colour rides the baked mRGBa multiply)");
	}

	@Test
	public void redCrystalReadsRedAndCyanCrystalReadsCyan() throws IOException {
		long[] tRed = channelSums(energiumSprite("energium_red"));
		assertTrue(tRed[0] > 0, "the red sprite carries red chroma");
		assertEquals(0L, tRed[1], "red sprite green-channel sum (the (v,0,0) multiply form)");
		assertEquals(0L, tRed[2], "red sprite blue-channel sum (the (v,0,0) multiply form)");
		long[] tCyan = channelSums(energiumSprite("energium_cyan"));
		assertEquals(0L, tCyan[0], "cyan sprite red-channel sum (the (0,v,v) multiply form)");
		assertTrue(tCyan[1] > 0 && tCyan[2] > 0, "the cyan sprite carries green+blue chroma");
	}
}
