package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import gregtech6.block.GTOvenBlock;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTOvenRenderSnapshot.OvenOverlayGroup;
import gregtech6.client.render.GTOvenOverlayModel.OvenTextureFace;
import gregtech6.client.render.GTOvenOverlayModel.OverlayPlan;

/**
 * The oven overlay render-path sentinel tests (task render-c-oven-overlay, the
 * GTFluidPipeFlowModelTest shape): the offline quad-level half of the acceptance — the
 * upstream :1014 overlay-pick truth table (four states, mActive winning over mRunning),
 * the CS.java:528-537 FACING_ROTATIONS face table verbatim, the per-face emission and
 * sprite-id rules, the unconditional dispatch with the tinted no-snapshot transient (the
 * r11-oven-solid-layer-fix form), and the 64 per-state ModelResourceLocation
 * registrations. The
 * sprite→BakedQuad baker is pinned offline too since uvof-private-copies (the #27
 * GTOreBakedModelSideUvTest form): the overlay PNGs are upright art (the running front's
 * glow window sits in the sprite's bottom half), so the corrected canonical UV walk is
 * asserted per vertex. Since oven-texture-borrow the BODY pass carries the machine
 * paint/material tint (the {@link GTMachineTintModel#tintQuads} product over the
 * {@link GTMachinePaintTint} colour line — the oven ladder is skipped by the
 * GTMachineTintModel wrap, so the tint rides here) and the baked decals ride tintIndex
 * -1 (the P22 uncoloured-decal contract, the vanilla JSON default).
 */
public class GTOvenOverlayModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;
	private static final int COLOR_SLOT = 3;

	@org.junit.jupiter.api.BeforeAll
	static void bootMaterials() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		// the BLOCK registry write window (the GT6SingleBlockFacingIntegrityTest recipe):
		// the Block ctor registers its intrusive holder and NamespacedWrapper.validateWrite
		// rejects a frozen registry — unfreeze before the offline GTOvenBlock fixtures
		java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
	}

	@AfterEach
	void clearRegistration() {
		GTRenderModelListener.clearForTest();
	}

	// ---------------------------------------------------------------------------
	// the upstream :1014 pick — four-state truth table (mActive wins over mRunning)
	// ---------------------------------------------------------------------------

	@Test
	void overlayPickTruthTableMirrorsUpstream1014() {
		// (mActive, mRunning) → group; the quirk: active&&running → ACTIVE (upstream
		// mActive||worldObj==null ? Active : mRunning ? Running : Inactive, :1014)
		assertEquals(OvenOverlayGroup.NONE, new GTOvenRenderSnapshot(false, false).overlayGroup(), "inactive → Inactive (no overlay quad)");
		assertEquals(OvenOverlayGroup.ACTIVE, new GTOvenRenderSnapshot(true, false).overlayGroup(), "active → Active");
		assertEquals(OvenOverlayGroup.RUNNING, new GTOvenRenderSnapshot(false, true).overlayGroup(), "running → Running");
		assertEquals(OvenOverlayGroup.ACTIVE, new GTOvenRenderSnapshot(true, true).overlayGroup(), "the :1014 quirk: active wins over running");
	}

	@Test
	void textureKeysAreLowercasePathTokens() {
		assertEquals("none", OvenOverlayGroup.NONE.textureKey());
		assertEquals("active", OvenOverlayGroup.ACTIVE.textureKey());
		assertEquals("running", OvenOverlayGroup.RUNNING.textureKey());
		for (OvenTextureFace tFace : OvenTextureFace.values()) {
			assertEquals(tFace.name().toLowerCase(java.util.Locale.ROOT), tFace.textureKey());
		}
	}

	// ---------------------------------------------------------------------------
	// the CS.java:528-537 FACING_ROTATIONS table — full 6x6 verbatim
	// ---------------------------------------------------------------------------

	@Test
	void facingRotationsTableIsUpstreamVerbatim() {
		// [facing][side] → texture face; 0=bottom, 1=top, 2=left, 3=front, 4=right, 5=back
		OvenTextureFace[][] tUpstream = {
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.FRONT, OvenTextureFace.RIGHT, OvenTextureFace.BACK}, // row 0 (DOWN)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.FRONT, OvenTextureFace.RIGHT, OvenTextureFace.BACK}, // row 1 (UP)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.FRONT, OvenTextureFace.BACK, OvenTextureFace.RIGHT, OvenTextureFace.LEFT}, // row 2 (NORTH)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.BACK, OvenTextureFace.FRONT, OvenTextureFace.LEFT, OvenTextureFace.RIGHT}, // row 3 (SOUTH)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.RIGHT, OvenTextureFace.FRONT, OvenTextureFace.BACK}, // row 4 (WEST)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.RIGHT, OvenTextureFace.LEFT, OvenTextureFace.BACK, OvenTextureFace.FRONT}, // row 5 (EAST)
		};
		int tAssertions = 0;
		for (Direction tFacing : Direction.values()) {
			for (Direction tSide : Direction.values()) {
				assertEquals(tUpstream[tFacing.get3DDataValue()][tSide.get3DDataValue()],
						GTOvenOverlayModel.textureFaceOf(tFacing, tSide),
						"CS.java:528-537 FACING_ROTATIONS[" + tFacing + "][" + tSide + "]");
				tAssertions++;
			}
		}
		assertEquals(36, tAssertions, "the full table is pinned");
		// the horizontal-facing front reads: the front texture sits on the facing side itself
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.NORTH, Direction.NORTH));
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.EAST, Direction.EAST));
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.SOUTH, Direction.SOUTH));
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.WEST, Direction.WEST));
	}

	// ---------------------------------------------------------------------------
	// the planner — emission rules, sprite ids, slab geometry
	// ---------------------------------------------------------------------------

	@Test
	void inactiveGroupPlansNothing() {
		GTOvenRenderSnapshot tInactive = new GTOvenRenderSnapshot(false, false);
		for (Direction tSide : Direction.values()) {
			assertTrue(GTOvenOverlayModel.planOverlayQuads(tInactive, Direction.NORTH, tSide).isEmpty(),
					"the inactive state adds no overlay quad (A-tier fallback carries the face): " + tSide);
		}
		assertTrue(GTOvenOverlayModel.planOverlayQuads(tInactive, Direction.NORTH, null).isEmpty());
	}

	@Test
	void nullPassSeesNoOverlayQuads() {
		// cube-faithful emission: the quads carry a cullface, so the unculled pass sees none
		assertTrue(GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(true, false), Direction.NORTH, null).isEmpty());
		assertTrue(GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(false, true), Direction.EAST, null).isEmpty());
		assertTrue(GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(true, true), Direction.SOUTH, null).isEmpty());
	}

	@Test
	void activeAndRunningPlansCoverEveryFaceOnce() {
		for (GTOvenRenderSnapshot tSnapshot : List.of(new GTOvenRenderSnapshot(true, false), new GTOvenRenderSnapshot(false, true))) {
			Set<OvenTextureFace> tFaces = new HashSet<>();
			for (Direction tSide : Direction.values()) {
				List<OverlayPlan> tPlans = GTOvenOverlayModel.planOverlayQuads(tSnapshot, Direction.NORTH, tSide);
				assertEquals(1, tPlans.size(), "exactly one overlay quad per face pass: " + tSide);
				assertEquals(tSide, tPlans.get(0).quadFace(), "the quad sits on its own culling face");
				tFaces.add(tPlans.get(0).textureFace());
			}
			assertEquals(Set.of(OvenTextureFace.values()), tFaces, "all six upstream texture faces are drawn");
		}
	}

	@Test
	void plansCarryTheQuirkAndTheGroupSprites() {
		// (true, true) plans the ACTIVE sprites — the :1014 quirk at the planner level
		List<OverlayPlan> tQuirk = GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(true, true), Direction.NORTH, Direction.NORTH);
		assertEquals(1, tQuirk.size());
		assertEquals(GTOvenOverlayModel.spriteOf(OvenOverlayGroup.ACTIVE, OvenTextureFace.FRONT), tQuirk.get(0).sprite());
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "block/oven_overlay_active_front"), tQuirk.get(0).sprite());

		// every sprite id has the gt6:block/oven_overlay_<group>_<face> shape
		for (OvenOverlayGroup tGroup : List.of(OvenOverlayGroup.ACTIVE, OvenOverlayGroup.RUNNING)) {
			for (Direction tSide : Direction.values()) {
				OverlayPlan tPlan = GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(tGroup == OvenOverlayGroup.ACTIVE, tGroup == OvenOverlayGroup.RUNNING),
						Direction.NORTH, tSide).get(0);
				//? if forge {
				assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "block/oven_overlay_" + tGroup.textureKey() + "_" + tPlan.textureFace().textureKey()),
				//?} else {
				/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "block/oven_overlay_" + tGroup.textureKey() + "_" + tPlan.textureFace().textureKey()),
				*///?}
						tPlan.sprite(), "the sprite id mirrors the upstream NBT_TEXTURE name form");
			}
		}
	}

	@Test
	void slabGeometryCarriesTheZfightingEpsilon() {
		double e = GTOvenOverlayModel.OVERLAY_EPSILON, t = GTOvenOverlayModel.OVERLAY_THICKNESS;
		double[] tUp = GTOvenOverlayModel.slabOf(Direction.UP);
		assertEquals(1 + e, tUp[4], 1e-9, "maxY = 1 + epsilon (COVER_OVERLAY form)");
		assertEquals(1 - t, tUp[1], 1e-9, "minY = the inner plane, 1px inside");
		double[] tDown = GTOvenOverlayModel.slabOf(Direction.DOWN);
		assertEquals(-e, tDown[1], 1e-9, "minY = -epsilon");
		double[] tEast = GTOvenOverlayModel.slabOf(Direction.EAST);
		assertEquals(1 + e, tEast[3], 1e-9, "maxX = 1 + epsilon");
		assertEquals(1 - t, tEast[0], 1e-9, "minX = the inner plane");
	}

	/**
	 * THE uvof-private-copies PIN: the overlay bakes the canonical full-face UV walk on
	 * all six faces — the sprite top (V=0) on the side faces' top corners (upright; the
	 * running front's glow window keeps its bottom-of-the-door position). The old GTCEu
	 * cubeUV table baked every side face upside down (plus a U mirror on NORTH/EAST).
	 */
	@Test
	void overlayQuadsBakeTheCanonicalFullFaceUvWalk() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(new StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = ModelData.builder()
				.with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(true, false)).build();
		for (Direction tFace : Direction.values()) {
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), tData, null);
			assertEquals(1, tQuads.size(), tFace + ": the overlay face rides its own culling pass");
			FaceBakePins.assertCanonicalFullFaceUv(tQuads.get(0), tFace);
		}
	}

	// ---------------------------------------------------------------------------
	// the body tint (oven-texture-borrow) — the layerless pass retints, the decals don't
	// ---------------------------------------------------------------------------

	/** A baked-format body quad (32 ints, white vertex colours, tintIndex 0). */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.NORTH, FaceBakePins.IdentitySprite.INSTANCE, true);
	}

	/** A baked-format decal quad (untinted, the P22 overlay form). */
	private static BakedQuad decalQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFF333333);
		return new BakedQuad(tVertices, -1, Direction.NORTH, FaceBakePins.IdentitySprite.INSTANCE, true);
	}

	/** The barest fallback: returns exactly the quads the tint arms hand it. */
	private static net.minecraft.client.resources.model.BakedModel quadsFallback(List<BakedQuad> aQuads) {
		return new net.minecraft.client.resources.model.BakedModel() {
			@Override public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.state.BlockState aState,
					@org.jetbrains.annotations.Nullable Direction aSide, net.minecraft.util.RandomSource aRand) { return aQuads; }
			@Override public boolean useAmbientOcclusion() { return false; }
			@Override public boolean isGui3d() { return false; }
			@Override public boolean usesBlockLight() { return false; }
			@Override public boolean isCustomRenderer() { return false; }
			@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
			@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
			@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
		};
	}

	/** The offline oven block fixture (the GT6SingleBlockFacingIntegrityTest :365 shape). */
	private static GTOvenBlock ovenBlock() {
		return new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	private static ModelData ovenData(boolean aActive, boolean aRunning) {
		return ModelData.builder().with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(aActive, aRunning)).build();
	}

	@Test
	void layerlessPassRetintsTheBodyWithTheRowMaterial() {
		GTOvenBlock tBlock = ovenBlock();
		BakedQuad tBody = bodyQuad(), tDecal = decalQuad();
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(List.of(tBody, tDecal)),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(),
				ovenData(false, false), null); // the inactive oven: the unpainted body IS the field complaint
		assertEquals(2, tOut.size(), "body + decal, the inactive state plans no overlay");
		int tTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY,
				GTMachinePaintTint.tintMaterialOf(tBlock), 0);
		assertNotEquals(0xFFFFFFFF, tTint, "the Heat_T row material actually colours (not the white identity)");
		assertArrayEquals(GTMachineTintModel.retintVertices(tBody.getVertices(), tTint),
				tOut.get(0).getVertices(), "the body quad is the tintQuads product of the seam colour");
		assertEquals(-1, tOut.get(0).getTintIndex(), "the retinted copy rides tintIndex -1 (no second multiply)");
		assertSame(tDecal, tOut.get(1), "the decal passes through as the shared instance (P22)");
	}

	@Test
	void paintedSnapshotWinsOverTheRowMaterial() {
		GTOvenBlock tBlock = ovenBlock();
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(List.of(bodyQuad())),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tPainted = ModelData.builder()
				.with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(false, false))
				.with(GTModelProperties.PAINT, 0x00FF00)
				.build();
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(), tPainted, null);
		assertEquals(1, tOut.size());
		assertArrayEquals(GTMachineTintModel.retintVertices(bodyQuad().getVertices(), 0xFF00FF00),
				tOut.get(0).getVertices(), "the spray-paint colour wins (upstream Paintable:85)");
	}

	@Test
	void dynamicOverlayQuadsRideNoTintIndex() {
		// the P22 alignment pin: the baked decals carry tintIndex -1 (the vanilla JSON
		// default), so no tint route can ever multiply the door art
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(new StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		for (Direction tFace : Direction.values()) {
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), ovenData(true, false), null);
			assertEquals(1, tQuads.size());
			assertEquals(-1, tQuads.get(0).getTintIndex(), tFace + ": the state decal is UNCOLOURED (upstream :179-180)");
		}
	}

	// ---------------------------------------------------------------------------
	// the chunk-layer seat (r11-oven-solid-layer-fix) — the static oven.json bakes one
	// tintindex-0 body cube + six 0.01 alpha-texel decal shells and declares render_type
	// cutout; the decal PNGs carry alpha<255 texels, so any layer without alpha discard
	// (solid, GT6BlockStates machineModel :1979-1984) would paint their RGB matte as
	// opaque full-face plates OVER the tinted body — the field "white oven" report
	// (research.r11-oven-white, breakpoint P9).
	// ---------------------------------------------------------------------------

	/** The six static decal faces, in the oven.json elements 1-6 order. */
	private static final List<String> OVEN_DECAL_FACES = List.of("front", "back", "left", "right", "top", "bottom");

	/** A named atlas stub (the {@link FaceBakePins.IdentitySprite} form) — the name is the pin's membership key. */
	private static final class NamedSprite extends TextureAtlasSprite {
		private NamedSprite(ResourceLocation aName) {
			super(aName,
					new net.minecraft.client.renderer.texture.SpriteContents(aName,
							new FrameSize(1, 1),
							new com.mojang.blaze3d.platform.NativeImage(1, 1, false),
							//? if forge {
							net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.EMPTY),
							//?} else {
							/*net.minecraft.server.packs.resources.ResourceMetadata.EMPTY),*/
							//?}
					1, 1, 0, 0);
		}
	}

	/** A baked-format quad with a NAMED sprite (the bodyQuad/decalQuad form) so pins can assert sprite membership. */
	private static BakedQuad namedQuad(String aNameSpacePath, int aTintIndex) {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, aTintIndex, Direction.NORTH,
				new NamedSprite(ResourceLocation.fromNamespaceAndPath("gt6", aNameSpacePath)), true);
	}

	/** The quad's sprite id (the offline membership probe). */
	private static ResourceLocation spriteId(BakedQuad aQuad) {
		return aQuad.getSprite().contents().name();
	}

	/**
	 * The static oven.json bake mirrored as quads (the familyMachineModel form,
	 * GT6BlockStates:2037-2062): the tintindex-0 {@code oven_colored_*} body + the six
	 * untinted {@code oven_overlay_<face>} decal shells.
	 */
	private static List<BakedQuad> staticOvenBakeMirror() {
		List<BakedQuad> rQuads = new ArrayList<>();
		rQuads.add(namedQuad("block/oven_colored_front", 0));
		for (String tFace : OVEN_DECAL_FACES) rQuads.add(namedQuad("block/oven_overlay_" + tFace, -1));
		return rQuads;
	}

	@Test
	void solidLayerCarriesNoOvenOverlayDecal() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(staticOvenBakeMirror()),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		List<BakedQuad> tSolid = tModel.getQuads(ovenBlock().defaultBlockState(), Direction.NORTH,
				RandomSource.create(), ovenData(false, false), RenderType.solid());
		assertTrue(tSolid.stream().noneMatch(tQuad -> spriteId(tQuad).getPath().startsWith("block/oven_overlay_")),
				"the solid chunk layer has no alpha discard (GT6BlockStates machineModel :1979-1984), so the "
						+ "alpha-texel decal shells must never ride it — baked there they paint their RGB matte "
						+ "over the tinted body (the field white-oven report)");
	}

	@Test
	void cutoutLayerCarriesTheStaticDecals() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(staticOvenBakeMirror()),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		List<BakedQuad> tCutout = tModel.getQuads(ovenBlock().defaultBlockState(), Direction.NORTH,
				RandomSource.create(), ovenData(false, false), RenderType.cutout());
		// the collapsed single list also carries the tinted body (the green pin below), so
		// the pin is CONTAINS: exactly the six static decal shells among the cutout quads
		List<BakedQuad> tDecals = tCutout.stream()
				.filter(tQuad -> spriteId(tQuad).getPath().startsWith("block/oven_overlay_")).toList();
		assertEquals(OVEN_DECAL_FACES.size(), tDecals.size(), "the six static decal shells ride the declared cutout layer");
		for (BakedQuad tQuad : tDecals) {
			assertEquals(-1, tQuad.getTintIndex(), "the static decals deserialize to the -1 default (no tint)");
		}
	}

	@Test
	void layerlessPassIsTheUnion() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(staticOvenBakeMirror()),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		List<BakedQuad> tAll = tModel.getQuads(ovenBlock().defaultBlockState(), Direction.NORTH,
				RandomSource.create(), ovenData(false, false), null);
		assertEquals(OVEN_DECAL_FACES.size() + 1, tAll.size(), "the null pass is the union: tinted body + six decals");
	}

	/**
	 * THE green pin: with the single cutout seat, the tint-product quads (the retinted
	 * body copies) all carry the {@code oven_colored_*} art and the {@code oven_overlay_*}
	 * quads are exactly the six untinted static decal shells — nothing oven is left on
	 * solid for the matte-plate bug to ride.
	 */
	@Test
	void cutoutBodyCopiesAreTheColoredArt() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(staticOvenBakeMirror()),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		GTOvenBlock tBlock = ovenBlock();
		assertTrue(tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH,
				RandomSource.create(), ovenData(false, false), RenderType.solid()).isEmpty(),
				"the oven draws on no chunk layer but the declared cutout");
		List<BakedQuad> tCutout = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH,
				RandomSource.create(), ovenData(false, false), RenderType.cutout());
		assertEquals(OVEN_DECAL_FACES.size() + 1, tCutout.size());
		int tColored = 0;
		for (BakedQuad tQuad : tCutout) {
			if (spriteId(tQuad).getPath().startsWith("block/oven_overlay_")) {
				assertEquals(-1, tQuad.getTintIndex(), "the static decal shells stay untinted");
			} else {
				assertTrue(spriteId(tQuad).getPath().startsWith("block/oven_colored_"),
						spriteId(tQuad) + ": the tinted body copies are the colored art");
				tColored++;
			}
		}
		assertEquals(1, tColored, "exactly the body cube is the tinted colored art");
	}

	/** The asset dir walk (the GT6OvenTexAuditDatagenTest form — cannot be imported across packages). */
	private static Path blockTexturesDir() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p.resolve(Path.of("src", "main", "resources", "assets", "gt6", "textures", "block"));
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from " + Path.of("").toAbsolutePath());
	}

	@Test
	void overlayDecalPngsCarryAlphaTexels() throws IOException {
		// the pixel-level semantics pin: every oven_overlay_* PNG has alpha<255 texels, so
		// the family MUST render on an alpha-tested layer (the JSON-declared cutout); on
		// the discard-less solid layer those texels paint their RGB residue instead.
		List<Path> tDecals;
		try (Stream<Path> tWalk = Files.list(blockTexturesDir())) {
			tDecals = tWalk.map(Path::getFileName).map(Path::toString)
					.filter(tName -> tName.startsWith("oven_overlay_") && tName.endsWith(".png")).sorted()
					.map(tName -> blockTexturesDir().resolve(tName)).toList();
		}
		assertTrue(tDecals.size() >= 30, "the decal census is non-vacuous: " + tDecals.size() + " oven_overlay PNGs");
		for (Path tFile : tDecals) {
			BufferedImage tImage = ImageIO.read(tFile.toFile());
			assertNotNull(tImage, "decodable PNG: " + tFile);
			int tAlphaTexels = 0;
			for (int y = 0; y < tImage.getHeight(); y++) {
				for (int x = 0; x < tImage.getWidth(); x++) {
					if ((tImage.getRGB(x, y) >>> 24) < 255) tAlphaTexels++;
				}
			}
			assertTrue(tAlphaTexels > 0,
					tFile.getFileName() + ": carries alpha<255 texels (must ride the alpha-tested cutout layer)");
		}
	}

	// ---------------------------------------------------------------------------
	// the dispatch gate — the second ModelProperty, the cover key untouched
	// ---------------------------------------------------------------------------

	@Test
	void dispatchIsUnconditionalAndTheTransientStillTints() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(List.of(bodyQuad())),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		GTOvenBlock tBlock = ovenBlock();
		// the gate is unconditional (the GTMachineTintModel:85-88 form): the model only
		// ever sits on the oven's 64 per-state keys, so every render of those keys
		// assembles here — the no-snapshot transient (BE before its first sync) used to
		// slip out to the RAW fallback: an untinted white body for a frame (the r11
		// breakpoint 3).
		assertTrue(tModel.supportsDynamicQuads(ModelData.EMPTY));
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH,
				RandomSource.create(), ModelData.EMPTY, null);
		int tTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY,
				GTMachinePaintTint.tintMaterialOf(tBlock), 0);
		assertNotEquals(0xFFFFFFFF, tTint, "the Heat_T row material actually colours (not the white identity)");
		assertEquals(1, tOut.size());
		assertArrayEquals(GTMachineTintModel.retintVertices(bodyQuad().getVertices(), tTint),
				tOut.get(0).getVertices(), "the transient frame tints like every other pass");
	}

	// ---------------------------------------------------------------------------
	// the 64 per-state registrations (4 ladder rows x 16, task oven-heat-t-ladder)
	// ---------------------------------------------------------------------------

	@Test
	void listenerRegistersTheLadderPerStateKeys() {
		List<ModelResourceLocation> tTargets = GTOvenClientListener.targetModelIds();
		assertEquals(64, tTargets.size(), "4 ladder rows x 2 active x 4 facing x 2 running");
		assertEquals(64, new HashSet<>(tTargets).size(), "all keys distinct");
		// variant string = the StateDefinition name-sorted property order (active, facing, running)
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=false,facing=north,running=false")));
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=true,facing=east,running=true")));
		assertFalse(tTargets.contains(new ModelResourceLocation("gt6", "oven", "facing=north,active=false,running=false")),
				"the misordered variant string is NOT a key (StateDefinition sorts by name)");
		for (ModelResourceLocation tTarget : tTargets) {
			//? if forge {
			assertEquals("gt6", tTarget.getNamespace());
			assertTrue(GTOvenClientListener.OVEN_BLOCK_PATHS.contains(tTarget.getPath()),
					tTarget.getPath() + ": every ladder row path is a key");
			//?} else {
			/*assertEquals("gt6", tTarget.id().getNamespace()); // 21.1: MRL is a record over an RL id
			assertTrue(GTOvenClientListener.OVEN_BLOCK_PATHS.contains(tTarget.id().getPath()),
					tTarget.id().getPath() + ": every ladder row path is a key");
			*///?}
		}
	}

	@Test
	void registrationWrapsTheBakedPerStateModels() {
		int tBefore = GTRenderModelListener.registeredCount();
		GTOvenClientListener.register();
		assertEquals(tBefore + 64, GTRenderModelListener.registeredCount(), "all 64 per-state keys registered");

		ModelResourceLocation tKey = new ModelResourceLocation("gt6", "oven", "active=true,facing=north,running=false");
		StubFallback tBaked = new StubFallback();
		//? if forge {
		java.util.Map<ResourceLocation, net.minecraft.client.resources.model.BakedModel> tModels = new java.util.HashMap<>();
		//?} else {
		/*java.util.Map<ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> tModels = new java.util.HashMap<>(); // 21.1: the baking table keys on the MRL record
		*///?}
		tModels.put(tKey, tBaked);
		//? if forge {
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));
		//?} else {
		/*GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null, null)); // 21.1: +ModelBakery
		*///?}

		assertTrue(tModels.get(tKey) instanceof GTOvenOverlayModel, "the per-state baked model is replaced by the overlay model");
		assertSame(tBaked, ((GTOvenOverlayModel) tModels.get(tKey)).getFallbackModel(), "the baked model stays as the fallback (A-tier material layer)");
	}

	/** Minimal BakedModel stub (the GTRenderModelListenerTest probe shape). */
	public static final class StubFallback implements net.minecraft.client.resources.model.BakedModel {
		@Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState, Direction aSide, net.minecraft.util.RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return false; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

}
