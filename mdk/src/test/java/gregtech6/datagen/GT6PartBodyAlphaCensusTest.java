package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * The part-family BODY-texture alpha census (task crucible-formed-vanish-probe, round 5
 * root cause). The invariant: a {@code parts/<family>/<design>/colored/<face>.png} BODY
 * texture must carry at least one OPAQUE pixel.
 *
 * <p><b>The bug this pins.</b> Upstream ships {@code metalwall/4/colored/*} (and the dense
 * twin {@code metalwalldense/4/colored/*}) as RGB(255,255,255) × alpha=0 — a BLANK. Upstream
 * 1.7.10 terrain renders these as the tinted-white body (the alpha plane never discards in
 * that pipeline), and the formed crucible repaints every wall to design 4 (upstream
 * MultiTileEntityCrucible :124-128 — the literal {@code 4}), so the formed look is the flat
 * material-coloured plate. The port copied the PNGs byte-identical (0ea8db003b) and renders
 * them on a CUTOUT layer, where every alpha=0 fragment is DISCARDED — a design-4 wall bakes
 * to zero visible pixels: invisible block, intact hitbox/BE, no missing-model checkerboard
 * (the sprites EXIST, they are just fully transparent), no crash. Task mb-formed-crucible-wall
 * then pointed the formed repaint at design 4 (the upstream-verbatim
 * {@code TileEntityCrucible.FORMED_WALL_DESIGN}), so the moment a large crucible forms, its
 * ENTIRE wall shell goes invisible — the "formed multiblock turns transparently invisible"
 * field report, five rounds of render-chain probes all clean because the defect was in the
 * ASSET, not the chain. The fix flattens the alpha plane to opaque on the six blank bodies
 * (RGB stays the upstream 255,255,255 — the GTMachineTintModel material tint then produces
 * the upstream flat-plate formed look); the overlay (decal) layer is exempt by design —
 * transparent-texel decals are the port's normal (D2 leg6 ruling), and design 0's blank
 * overlay is the standing precedent.
 *
 * <p>The positive control pins the KNOWN-blank overlay so the detector can never pass
 * vacuously.
 */
public class GT6PartBodyAlphaCensusTest {

	/** The two wall texture families the port's design ladders consume (GT6BlockStates.partModel). */
	private static final List<String> FAMILIES = List.of("metalwall", "metalwalldense");

	/** The NBT_DESIGNS 7 ladder — designs 0..7 inclusive, three faces per layer. */
	private static final List<String> FACES = List.of("bottom", "top", "side");

	@Test
	public void everyWallBodyTextureCarriesAnOpaquePixel() throws Exception {
		for (String tFamily : FAMILIES) {
			for (int d = 0; d <= 7; d++) {
				for (String tFace : FACES) {
					String tPath = path(tFamily, d, "colored", tFace);
					BufferedImage tImage = read(tPath);
					assertTrue(opaquePixels(tImage) > 0,
							tPath + " is a fully-transparent BODY texture — the design-" + d
									+ " wall variant renders zero visible pixels (the formed-crucible vanish)");
				}
			}
		}
	}

	/** The detector's positive control: the blank OVERLAY stays blank (decals are exempt). */
	@Test
	public void theBlankOverlayControlStaysBlank() throws Exception {
		assertEquals(0, opaquePixels(read(path("metalwall", 4, "overlay", "side"))),
				"metalwall/4/overlay/side is the known-blank decal — if this turned opaque the census walk is stale");
	}

	/** All 48 body files are the 16x16 upstream tile size (the census walks the real ladder, not stale paths). */
	@Test
	public void everyWallBodyTextureIsTheUpstreamTileSize() throws Exception {
		for (String tFamily : FAMILIES) {
			for (int d = 0; d <= 7; d++) {
				for (String tFace : FACES) {
					BufferedImage tImage = read(path(tFamily, d, "colored", tFace));
					assertEquals(16, tImage.getWidth(), path(tFamily, d, "colored", tFace) + " width");
					assertEquals(16, tImage.getHeight(), path(tFamily, d, "colored", tFace) + " height");
				}
			}
		}
	}

	private static String path(String aFamily, int aDesign, String aLayer, String aFace) {
		return "assets/gt6/textures/block/parts/" + aFamily + "/" + aDesign + "/" + aLayer + "/" + aFace + ".png";
	}

	private static BufferedImage read(String aPath) throws Exception {
		try (InputStream tStream = GT6PartBodyAlphaCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " must exist on the classpath");
			return ImageIO.read(tStream);
		}
	}

	/** The opaque-pixel count over the alpha band (a 3-band raster counts as fully opaque). */
	private static int opaquePixels(BufferedImage aImage) {
		if (aImage == null) return 0;
		var tRaster = aImage.getRaster();
		if (tRaster.getNumBands() < 4) return tRaster.getWidth() * tRaster.getHeight();
		int rCount = 0;
		int[] tPixel = new int[4];
		for (int y = 0; y < tRaster.getHeight(); y++) {
			for (int x = 0; x < tRaster.getWidth(); x++) {
				tRaster.getPixel(x, y, tPixel);
				if (tPixel[3] > 0) rCount++;
			}
		}
		return rCount;
	}
}
