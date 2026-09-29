package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * The r8-uvof-private-copies pin fixture (the #27 GTOreBakedModelSideUvTest pattern,
 * package-shared by the flow-arrow and oven-overlay bake pins): a leg-uniform identity
 * sprite stands in for the atlas so the decoded vertex UV floats ARE the 0..16 model-space
 * values, plus the one assertion that pins a baked quad to the canonical vanilla
 * {@code BlockFaceUV} rotation-0 full-face walk.
 * <p>The vanilla walk being pinned: FaceBakery.fillVertex (FaceBakery.java:142-150) feeds
 * vertex i from {@link net.minecraft.client.renderer.block.model.BlockFaceUV#getU}/{@code
 * getV} (BlockFaceUV.java:22-38, rotation 0): vertex i reads the corner
 * {@code (0,0),(0,16),(16,16),(16,0)} in FaceInfo order (FaceInfo.java:7-42) — on every
 * side face the TOP corners are vertices 0/3, so they carry the sprite-top V (0) = the
 * upright orientation the directional art was drawn for.
 */
final class FaceBakePins {

	/** The int[] vertex stride (FaceBakery.VERTEX_INT_SIZE) and UV slot offsets. */
	private static final int STRIDE = 8, U_SLOT = 4, V_SLOT = 5;

	/** The forge FaceBakery anti-bleed nudge headroom (uv*0.999 + opposite-corner*0.001 → ≤0.016 at 0..16). */
	private static final float TOLERANCE = 0.1F;

	private FaceBakePins() {}

	/**
	 * A leg-uniform atlas stub: getU/getV return the input unchanged (so the decoded vertex
	 * floats ARE the box values) and a zero uvShrinkRatio kills FaceBakery's shrink lerp.
	 * The legs diverge in sprite coordinate conventions — forge 1.20.1 passes 0..16 into
	 * {@code getU(double)}, 1.21.1 passes 0..1 into {@code getU(float)} (FaceBakery divides
	 * by 16 first) — hence the per-leg overrides. The stock UnitTextureAtlasSprite stub is
	 * unusable for UV pins: it lacks the forge-leg identity form and its 1x1 atlas makes
	 * uvShrinkRatio = 4, so the shrink EXTRAPOLATES.
	 */
	static final class IdentitySprite extends TextureAtlasSprite {
		static final IdentitySprite INSTANCE = new IdentitySprite();

		private IdentitySprite() {
			super(ResourceLocation.fromNamespaceAndPath("gt6", "unit_uv"),
					new net.minecraft.client.renderer.texture.SpriteContents(ResourceLocation.fromNamespaceAndPath("gt6", "unit_uv"),
							new FrameSize(1, 1),
							new NativeImage(1, 1, false),
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

	/**
	 * THE r8-uvof-private-copies PIN: the quad's four vertices carry exactly the canonical
	 * rotation-0 walk (0,0),(0,16),(16,16),(16,0) in FaceInfo vertex order — the full-face
	 * {@code [0,0,16,16]} form, i.e. the sprite renders exactly as a vanilla cube JSON
	 * would. Cross-checks the orientation reading against the quad's own positions: side
	 * faces have their sprite-top V on the TOP corners (upright), UP reads V=0 at the
	 * MIN_Z corner, DOWN at the MAX_Z corner (the 1.7.10 renderFixedNegativeYFacing form,
	 * the #27 ruling).
	 */
	static void assertCanonicalFullFaceUv(BakedQuad aQuad, Direction aFace) {
		int[] tV = aQuad.getVertices();
		float[][] tWalk = {{0, 0}, {0, 16}, {16, 16}, {16, 0}};
		for (int i = 0; i < 4; i++) {
			assertEquals(tWalk[i][0], Float.intBitsToFloat(tV[i * STRIDE + U_SLOT]), TOLERANCE,
					aFace + " vertex " + i + " U");
			assertEquals(tWalk[i][1], Float.intBitsToFloat(tV[i * STRIDE + V_SLOT]), TOLERANCE,
					aFace + " vertex " + i + " V");
		}
		float tY0 = Float.intBitsToFloat(tV[1]), tY1 = Float.intBitsToFloat(tV[1 + STRIDE]),
				tY2 = Float.intBitsToFloat(tV[2 * STRIDE + 1]), tY3 = Float.intBitsToFloat(tV[3 * STRIDE + 1]);
		if (aFace.getAxis().isHorizontal()) {
			// FaceInfo.java:19-42: on the sides, vertices 0/3 are the MAX_Y corners — the
			// walk gives them V=0, so the sprite top (the arrow head / the oven glow
			// window's top) lands at the block-face top
			assertTrue(tY0 > tY1 && tY0 > tY2 && tY3 > tY1 && tY3 > tY2, aFace + ": vertices 0/3 are the top corners");
		} else if (aFace == Direction.UP) {
			assertTrue(Float.intBitsToFloat(tV[2]) < Float.intBitsToFloat(tV[2 + STRIDE]),
					aFace + ": vertex 0 sits at the MIN_Z edge (V=0 there)");
		} else {
			assertTrue(Float.intBitsToFloat(tV[2]) > Float.intBitsToFloat(tV[2 + STRIDE]),
					aFace + ": vertex 0 sits at the MAX_Z edge (V=0 there, the renderFixedNegativeYFacing form)");
		}
	}
}
