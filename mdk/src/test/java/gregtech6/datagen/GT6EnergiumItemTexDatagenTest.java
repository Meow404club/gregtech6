/*
 * The energium crystal sprite pins (task r11-energium-tint, re-opened by task
 * energium-crystal-appearance-2): the twelve LU energium batteries (red ULV-IV + cyan
 * ULV-IV, upstream Loader_MultiTileEntities.java:1078-1092) borrow BYTE-IDENTICAL
 * grayscale crystal sprites (upstream lu/8/sides.png == lu/32/sides.png byte-for-byte),
 * and the upstream family colour rides the mRGBa render multiply
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
 *
 * <p>Task energium-crystal-appearance-2 added the VISUAL pins (the round-one lesson:
 * a byte-MATCH against the bake script's own expectation is self-referential — the
 * user still saw "纯色块填满格子", because the upstream item form is the
 * RendererBlockTextured.renderInventoryBlock 3D render of the 4x4x4 battery box
 * (MultiTileEntityRegistry.java:105, RendererBlockTextured.java:50-98,
 * MultiTileEntityBatteryLU8.java:50), not a flat full-cell sprite). So:
 * <ul>
 * <li>the 12 energium ITEM MODELS must be the 3D box form (parent block/cube, one
 *     element 6,0,6→10,4,10, full-uv faces) — the flat item/generated parent IS the
 *     reported bug shape;</li>
 * <li>the two sprites must pass IMAGE-STATISTICS floors (tint-channel spread, unique
 *     colour count, adjacent-pixel edge energy, luminance stdev) — a flat colour fill
 *     or an over-compressed smear fails these numbers, no byte pin can.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Batteries;

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

	// ------------------------------------------------------------------
	// the visual pins (task energium-crystal-appearance-2): a flat colour
	// fill must fail these IMAGE statistics regardless of any byte pin.
	// Measured on the shipped bakes: red spread 97 / 54 colours / 3.26
	// mean-adjacent-diff / 6.36 lum-stdev; cyan 96-97 / 93 / 3.26 / 14.93.
	// The floors sit at roughly half the measured values; a solid fill is
	// 0-1 across the board (±1 encode noise at best).
	// ------------------------------------------------------------------

	/** The tint-channel floor per family: the index of the channel the mRGBa multiply leaves. */
	private static final String[][] TINT_CHANNELS = {
			{"energium_red", "0"}, // red (255,0,0): the R channel
			{"energium_cyan", "1"}, // cyan (0,255,255): the G channel (B rides it, byte-equal form)
			{"energium_cyan", "2"}};

	@Test
	public void energiumSpritesCarryRealImageStructureNotFlatFills() throws IOException {
		for (String[] tPin : TINT_CHANNELS) {
			BufferedImage tImage = ImageIO.read(energiumSprite(tPin[0]).toFile());
			assertNotNull(tImage, "decodable PNG: " + tPin[0]);
			int tChannel = Integer.parseInt(tPin[1]);
			int tWidth = tImage.getWidth(), tHeight = tImage.getHeight();
			int tMin = 255, tMax = 0;
			long tDiffSum = 0;
			int tDiffCount = 0;
			java.util.Set<Integer> tColours = new java.util.HashSet<>();
			double tLumSum = 0, tLumSqSum = 0;
			int tCount = 0;
			for (int y = 0; y < tHeight; y++)
				for (int x = 0; x < tWidth; x++) {
					int tArgb = tImage.getRGB(x, y);
					int tV = channel(tArgb, tChannel);
					tMin = Math.min(tMin, tV);
					tMax = Math.max(tMax, tV);
					tColours.add(tArgb & 0xFFFFFF);
					double tLum = 0.299 * ((tArgb >> 16) & 255) + 0.587 * ((tArgb >> 8) & 255) + 0.114 * (tArgb & 255);
					tLumSum += tLum;
					tLumSqSum += tLum * tLum;
					tCount++;
					if (x + 1 < tWidth) { tDiffSum += Math.abs(tV - channel(tImage.getRGB(x + 1, y), tChannel)); tDiffCount++; }
					if (y + 1 < tHeight) { tDiffSum += Math.abs(tV - channel(tImage.getRGB(x, y + 1), tChannel)); tDiffCount++; }
				}
			double tMeanDiff = (double) tDiffSum / tDiffCount;
			double tLumMean = tLumSum / tCount;
			double tLumStdev = Math.sqrt(Math.max(0.0, tLumSqSum / tCount - tLumMean * tLumMean));
			String tWhere = tPin[0] + " channel " + tChannel;
			assertTrue(tMax - tMin >= 48, tWhere + ": tint-channel spread " + (tMax - tMin)
					+ " < 48 — the upstream diagonal gradient (255→158, lu/8 sides.png) is gone; a flat fill has spread ~0");
			assertTrue(tColours.size() >= 24, tWhere + ": only " + tColours.size()
					+ " distinct colours < 24 — a solid colour block has 1, ±1 encode noise at most ~8");
			assertTrue(tMeanDiff >= 1.5, tWhere + ": mean adjacent-pixel diff " + tMeanDiff
					+ " < 1.5 — no edge/gradient energy; the sprite reads as a uniform fill in the cell");
			assertTrue(tLumStdev >= 2.0, tWhere + ": luminance stdev " + tLumStdev
					+ " < 2.0 — the image is flat, no brightness structure survives");
		}
	}

	/** The requested colour channel of an ARGB int: 0=R, 1=G, 2=B. */
	private static int channel(int aArgb, int aChannel) {
		return (aArgb >> ((2 - aChannel) * 8)) & 255;
	}

	/**
	 * The item-model shape pin: the twelve energium rows must generate the 3D box form —
	 * parent block/cube, ONE element 6,0,6→10,4,10 (the upstream setBlockBounds2 battery
	 * box, MultiTileEntityBatteryLU8.java:50), every face full-uv 0,0→16,16 (RenderBlocks
	 * renders the whole icon per face). The flat {@code item/generated} parent IS the
	 * user-reported "纯色块填满格子" bug shape — this pin goes red if anyone routes the
	 * crystals back to a full-cell sprite.
	 */
	@Test
	public void energiumItemModelsAreTheUpstreamBoxFormNotFlatSprites() throws Exception {
		int tPinned = 0;
		for (GT6Batteries.BatteryRow tRow : GT6Batteries.ROWS) {
			if (!tRow.family().startsWith("energium_")) continue;
			tPinned++;
			JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
			assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
					tRow.path() + ": the 3D box form (the flat item/generated parent is the reported bug shape)");
			JsonArray tElements = tModel.getAsJsonArray("elements");
			assertEquals(1, tElements.size(), tRow.path() + ": exactly the battery-box element");
			JsonObject tBox = tElements.get(0).getAsJsonObject();
			assertArrayEquals(new double[] {6.0, 0.0, 6.0}, toDoubles(tBox.getAsJsonArray("from")),
					tRow.path() + ": box from (PX_P[6], PX_P[0], PX_P[6])");
			assertArrayEquals(new double[] {10.0, 4.0, 10.0}, toDoubles(tBox.getAsJsonArray("to")),
					tRow.path() + ": box to (PX_N[6], PX_N[12], PX_N[6])");
			int tFaces = 0;
			for (String tDir : new String[] {"down", "up", "north", "south", "west", "east"}) {
				JsonObject tFace = tBox.getAsJsonObject("faces").getAsJsonObject(tDir);
				assertNotNull(tFace, tRow.path() + ": face " + tDir);
				assertArrayEquals(new double[] {0.0, 0.0, 16.0, 16.0}, toDoubles(tFace.getAsJsonArray("uv")),
						tRow.path() + ": face " + tDir + " full-uv (the whole upstream icon per face)");
				tFaces++;
			}
			assertEquals(6, tFaces, tRow.path() + ": all six faces");
		}
		assertEquals(12, tPinned, "the walk covers all twelve energium rows");
	}

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6EnergiumItemTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated asset must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static double[] toDoubles(JsonArray aArray) {
		double[] tOut = new double[aArray.size()];
		for (int i = 0; i < tOut.length; i++) tOut[i] = aArray.get(i).getAsDouble();
		return tOut;
	}

	private static void assertArrayEquals(double[] aExpected, double[] aActual, String aMessage) {
		assertEquals(aExpected.length, aActual.length, aMessage);
		for (int i = 0; i < aExpected.length; i++) assertEquals(aExpected[i], aActual[i], 1.0e-6, aMessage);
	}
}
