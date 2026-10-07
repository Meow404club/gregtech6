/**
 * The issue #27 six-face orientation pins: every ore quad must bake the sprite's V=0
 * (top) edge onto the block face where the 1.7.10 icon orientation had it — the bake
 * now resolves to the vanilla {@code BlockElement.uvsByFace} canonical form on all six
 * faces (a full cube reads {@code [0,0,16,16]} everywhere, i.e. this model renders each
 * sprite exactly like a vanilla cube_all JSON).
 * <p>The vanilla walk that proves the side slots: FaceInfo NORTH corner 0 =
 * (MAX_X, MAX_Y, MIN_Z) (FaceInfo.java:19-24 — on every side face the TOP corners are
 * vertex indices 0/3) and BlockFaceUV.getV feeds vertices 0/3 from {@code uvs[1]}
 * (BlockFaceUV.java:31-38, rotation 0; FaceBakery.fillVertex.java:142-150). The old
 * GTCEu StaticFaceBakery cubeUV table put {@code maxY} in that slot on all four sides —
 * every side face read its sprite bottom-up (the #27 "ore sides are upside down").
 * <p>The DOWN trap (both the old table AND 1.7.10's own vanilla got it wrong here):
 * 1.7.10 renderFaceYNeg computed V inverted along Z (its body survives commented-out in
 * the upstream copy, ITexture.java:292-293), and upstream deliberately replaced it with
 * renderFixedNegativeYFacing (doRenderYNeg, ITexture.java:247-262/:288-364) — minV, the
 * sprite top, at renderMaxZ. The canonical JSON default DOWN bakes corner-for-corner to
 * that same mapping, so the port now matches BOTH generations; the old table's
 * {@code [0,16,16,0]} was flipped in both and is pinned fixed here.
 * <p>The directional sample: the lapis SET's ore.png carries its bright speckle cluster
 * in the BOTTOM half of the sprite (alpha-weighted luminance 39 top vs 66 bottom, the
 * strongest asymmetry of the 20 borrowed SETs) — one PNG, three face semantics: on the
 * corrected sides the cluster sits low (an upside-down bake hung it at eye level, the
 * user-visible symptom), on UP it reads toward the south edge (V grows with Z) and on
 * DOWN toward the north edge (V=0 at MAX_Z).
 * Offline: a leg-uniform identity stub sprite ({@link #IdentitySprite}) stands in for
 * the atlas — its getU/getV are identity and its uvShrinkRatio is zero, so the decoded
 * UV floats ARE the 0..16 model-space box values and the pins are exact on both legs
 * (the stock UnitTextureAtlasSprite stub is forge/neo-divergent and shrink-extrapolated,
 * see the class tail).
 */
package gregtech6.client.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.minecraftforge.client.model.data.ModelData;

public class GTOreBakedModelSideUvTest {

	/** The int[] vertex stride (FaceBakery.VERTEX_INT_SIZE) and slot offsets. */
	private static final int STRIDE = 8, U_SLOT = 4, V_SLOT = 5;
	private static final String DIRECTIONAL_SET = "lapis";

	/**
	 * A leg-uniform atlas stub: getU/getV return the 0..16 MODEL-SPACE uv (so the decoded
	 * vertex floats ARE the box values) and a zero uvShrinkRatio kills FaceBakery's shrink
	 * lerp. The legs diverge in sprite coordinate conventions — forge 1.20.1 passes
	 * 0..16 into {@code getU(double)} (vanilla {@code u0 + (u1-u0)*u/16}), 1.21.1 passes
	 * 0..1 into {@code getU(float)} (FaceBakery divides by 16 first, TextureAtlasSprite is
	 * u0 + (u1-u0)*u) — hence the per-leg overrides. The stock UnitTextureAtlasSprite stub
	 * is unusable for UV pins: it lacks the forge-leg identity form and its 1x1 atlas makes
	 * uvShrinkRatio = 4/atlasSize = 4, so the shrink EXTRAPOLATES ([0,0,16,16] → 2/-1-scale
	 * garbage). The remaining forge-side 0.016 wobble is the forge FaceBakery anti-bleed
	 * nudge (uv*0.999 + opposite-corner*0.001 — see the pins' tolerance).
	 */
	private static final class IdentitySprite extends net.minecraft.client.renderer.texture.TextureAtlasSprite {
		static final IdentitySprite INSTANCE = new IdentitySprite();

		private IdentitySprite() {
			super(ResourceLocation.fromNamespaceAndPath("gt6", "unit_uv"),
					new net.minecraft.client.renderer.texture.SpriteContents(ResourceLocation.fromNamespaceAndPath("gt6", "unit_uv"),
							new net.minecraft.client.resources.metadata.animation.FrameSize(1, 1),
							new com.mojang.blaze3d.platform.NativeImage(1, 1, false),
							//? if forge {
							net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.EMPTY),
							//?} else {
							/*net.minecraft.server.packs.resources.ResourceMetadata.EMPTY),*/
							//?}
					1, 1, 0, 0);
		}

		//? if forge {
		@Override
		public float getU(double aU) { return (float) aU; }

		@Override
		public float getV(double aV) { return (float) aV; }
		//?} else {
		/*@Override
		public float getU(float aU) { return aU * 16.0F; }

		@Override
		public float getV(float aV) { return aV * 16.0F; }*/
		//?}

		@Override
		public float uvShrinkRatio() { return 0.0F; }
	}

	@BeforeAll
	static void boot() {
		// the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	/** The model over the identity stub sprite: exact 0..16 UV pins on both legs. */
	private static GTOreBakedModel model() {
		GTOreBakedModel.Params tParams = new GTOreBakedModel.Params(
				ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"),
				ResourceLocation.fromNamespaceAndPath("gt6", "block/materialicons/" + DIRECTIONAL_SET + "/ore"),
				ResourceLocation.fromNamespaceAndPath("gt6", "block/materialicons/" + DIRECTIONAL_SET + "/ore_overlay"), 0xFF2040C0);
		return new GTOreBakedModel(null, tParams, aMaterial -> IdentitySprite.INSTANCE);
	}

	/** The three stacked quads of one face (base + coloured shell + outline shell, the bakeQuads order). */
	private static List<BakedQuad> faceStack(GTOreBakedModel aModel, Direction aFace) {
		return aModel.getQuads(null, aFace, RandomSource.create(), ModelData.EMPTY, null);
	}

	/**
	 * THE #27 PIN: on all four side faces the TOP vertices (y = 1) carry the sprite-top
	 * V (0.0) and the bottom vertices the sprite-bottom V (16.0) — upright. The U slots
	 * are pinned in the canonical BlockElement.uvsByFace form (NORTH/EAST run their
	 * texture U against the face's minus edge, SOUTH/WEST with it). The tinted/outline
	 * shells ride the same corrected UVs (same bakeQuad path).
	 */
	@Test
	public void sideFacesBakeTheSpriteTopOntoTheBlockTop() {
		GTOreBakedModel tModel = model();
		for (Direction tFace : Direction.Plane.HORIZONTAL) {
			// NORTH/SOUTH faces run their U along X, WEST/EAST along Z; N/E mirror it against the +edge (canonical)
			boolean tUAlongX = tFace.getAxis() == Direction.Axis.Z;
			boolean tUMirrored = tFace == Direction.NORTH || tFace == Direction.EAST;
			List<BakedQuad> tStack = faceStack(tModel, tFace);
			assertEquals(3, tStack.size(), "base + coloured + outline on " + tFace);
			for (int tV = 0; tV < 4; tV++) {
				float tY = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + 1]);
				float tX = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE]);
				float tZ = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + 2]);
				boolean tPlusEdge = (tUAlongX ? tX : tZ) > 0.5f;
				float tU = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + U_SLOT]);
				float tVV = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + V_SLOT]);
				// the forge FaceBakery patch nudges every UV 0.1% toward the OPPOSITE corner
				// (uv.getU(i)*0.999 + uv.getU((i+2)%4)*0.001 — the anti-bleed lerp), so the
				// pins carry a 0.1 tolerance; a flipped bake misses by ~16, not ~0.016
				assertEquals(tPlusEdge == tUMirrored ? 0.0f : 16.0f, tU, 0.1f,
						tFace + " vertex " + tV + " U in the canonical uvsByFace slots");
				assertEquals(tY > 0.5f ? 0.0f : 16.0f, tVV, 0.1f,
						tFace + " vertex " + tV + " V: sprite TOP on the block top (issue #27)");
				for (int tShell = 1; tShell <= 2; tShell++) {
					assertEquals(tVV, Float.intBitsToFloat(tStack.get(tShell).getVertices()[tV * STRIDE + V_SLOT]),
							tFace + " shell " + tShell + " shares the corrected V");
				}
			}
		}
	}

	/**
	 * The horizontal faces: UP keeps the canonical sprite-top at the MIN_Z edge
	 * (unchanged by the fix — the user saw the top as normal); DOWN moves to the
	 * sprite-top at the MAX_Z edge — the upstream renderFixedNegativeYFacing form
	 * (ITexture.java:288-364, minV at renderMaxZ) that the canonical JSON default bakes
	 * corner-for-corner; the old table's {@code [0,16,16,0]} was flipped in BOTH
	 * generations and is pinned fixed here.
	 */
	@Test
	public void horizontalFacesBakeTheCanonicalTopAndTheUpstreamFixedBottom() {
		GTOreBakedModel tModel = model();
		for (Direction tFace : new Direction[] {Direction.UP, Direction.DOWN}) {
			boolean tTopAtMinusZ = tFace != Direction.DOWN;
			List<BakedQuad> tStack = faceStack(tModel, tFace);
			for (int tV = 0; tV < 4; tV++) {
				float tX = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE]);
				float tZ = Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + 2]);
				assertEquals(tX > 0.5f ? 16.0f : 0.0f, Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + U_SLOT]), 0.1f,
						tFace + " vertex " + tV + " U along X (canonical)");
				assertEquals((tZ > 0.5f) != tTopAtMinusZ ? 0.0f : 16.0f,
						Float.intBitsToFloat(tStack.get(0).getVertices()[tV * STRIDE + V_SLOT]), 0.1f,
						tFace + " vertex " + tV + " V along Z: sprite top at the " + (tTopAtMinusZ ? "MIN" : "MAX") + "_Z edge");
			}
		}
	}

	/**
	 * The directional sample (the card's "which texture, which feature" annotation): the
	 * lapis SET ore.png is genuinely top/bottom asymmetric — its bright speckle cluster
	 * sits in the BOTTOM half, so the pre-fix upside-down side bake visibly hung it at
	 * the top of the block side, and the same PNG's V axis now lands toward the south
	 * edge on UP and the north edge on DOWN. The borrowed PNG is the upstream artifact,
	 * byte-verbatim.
	 */
	@Test
	public void theDirectionalSampleSpriteIsActuallyDirectional() throws Exception {
		Path tPng = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/materialicons")
				.resolve(DIRECTIONAL_SET).resolve("ore.png");
		assertTrue(Files.isRegularFile(tPng), "the borrowed SET PNG is on disk: " + tPng);
		BufferedImage tImage;
		try (InputStream tIn = Files.newInputStream(tPng)) {
			tImage = ImageIO.read(tIn);
		}
		int tW = tImage.getWidth(), tH = tImage.getHeight();
		long tTop = 0, tBottom = 0;
		for (int tY = 0; tY < tH; tY++) {
			for (int tX = 0; tX < tW; tX++) {
				int tArgb = tImage.getRGB(tX, tY);
				int tA = (tArgb >>> 24) & 255;
				int tLuma = (((tArgb >>> 16) & 255) * 30 + ((tArgb >>> 8) & 255) * 59 + (tArgb & 255) * 11) / 100;
				if (tY < tH / 2) tTop += (long) tLuma * tA;
				else tBottom += (long) tLuma * tA;
			}
		}
		assertTrue(tBottom > tTop * 4 / 3,
				DIRECTIONAL_SET + " ore.png carries its bright cluster low (alpha-weighted luminance bottom "
						+ tBottom + " > 4/3 x top " + tTop + ") — the #27 flip was user-visible on this SET");
		assertEquals(16, tW, "the upstream ore icon is the 1.7.10 16x16 icon sheet");
		assertEquals(16, tH, "the upstream ore icon is the 1.7.10 16x16 icon sheet");
	}

	/** The mdk project root, walking up from the test working dir (the GT6OreCensusTest marker walk). */
	private static Path mdkRoot() {
		for (Path tP = Path.of("").toAbsolutePath(); tP != null; tP = tP.getParent()) {
			if (Files.isRegularFile(tP.resolve("tools").resolve("gen_textures.py"))) return tP;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from " + Path.of("").toAbsolutePath());
	}
}
