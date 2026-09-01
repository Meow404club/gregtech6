/**
 * The 64-connection-state quad structure of {@link GTWireBakedModel} (task
 * p9-wire-family-w2 acceptance "64 连接态 quad 结构单测"): the planner is a pure
 * (insulated, diameter, mask) → face/box/tint/sprite-kind function — the offline half of
 * the visual correctness (the runClient eye check stays with the user). Every expected box
 * below is the direct transcription of the upstream arm switch
 * (TileEntityBase10ConnectorRendered.setBlockBounds2 :124-129) and core box (:115), and the
 * texture pick of :138-139 (core = getTextureSide, caps = getTextureConnected, buried faces
 * skipped), with the :238 insulation tier ladder from MultiTileEntityWireElectric.
 * Task p11-wire-fiber-texture appends the fiber form (planShapesFiber): the laser family
 * twins every material quad with an untinted FIBER_WIRE_OVERLAY (WireLaser :121-122).
 */
package gregtech6.client.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

import gregtech6.client.wire.GTWireBakedModel.Shape;
import gregtech6.client.wire.GTWireBakedModel.SpriteKind;

public class GTWireQuadStructureTest {

    /** The core box at diameter d/16 (upstream :115: (1-d)/2 .. 1-(1-d)/2 on all axes). */
    private static double[] core(float d) {
        double tHalf = (1.0 - d / 16.0) / 2.0;
        return new double[] {tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf, 1 - tHalf};
    }

    private static long countKind(List<Shape> aShapes, SpriteKind aKind) {
        return aShapes.stream().filter(s -> s.kind() == aKind).count();
    }

    private static long countFace(List<Shape> aShapes, Direction aFace) {
        return aShapes.stream().filter(s -> s.face() == aFace).count();
    }

    @Test
    public void bareWireMaskZeroIsTheBareCore() {
        List<Shape> tShapes = GTWireBakedModel.planShapes(false, 2, 0);
        assertEquals(6, tShapes.size(), "a standalone bare wire is just the 6 core faces");
        for (Shape tShape : tShapes) {
            assertEquals(SpriteKind.WIRE, tShape.kind());
            assertEquals(0, tShape.tintIndex(), "material colour rides tint index 0");
            assertNull(tShape.cull(), "internal faces never cull");
            org.junit.jupiter.api.Assertions.assertArrayEquals(core(2), tShape.box(), 1e-9);
        }
    }

    @Test
    public void bareWireFullMaskHasCorePlusFiveSidedArms() {
        // diameter 8px = 0.5 — the upstream wireGt08 size
        List<Shape> tShapes = GTWireBakedModel.planShapes(false, 8, 63);
        assertEquals(6 + 6 * 5, tShapes.size(), "core + one 5-face arm per side (the buried face is skipped, :139)");
        assertEquals(36, countKind(tShapes, SpriteKind.WIRE), "bare wire: every face is material");
        assertEquals(0, countKind(tShapes, SpriteKind.INSULATION_FULL), "no insulation anywhere on a bare wire");
        // every side has an outward cap whose face plane sits exactly on the block boundary
        for (Direction tDir : Direction.values()) {
            Shape tCap = tShapes.stream().filter(s -> s.face() == tDir && s.cull() == tDir).findFirst().orElse(null);
            assertTrue(tCap != null, "missing outward cap for " + tDir);
            assertEquals(0, tCap.tintIndex());
            // negative dirs reach the boundary at min (0), positive dirs at max (1) — :124-129
            double tFacePlane = tDir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? boxMax(tCap, tDir) : boxMin(tCap, tDir);
            assertEquals(tDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : 0.0, tFacePlane, 1e-9,
                    "the " + tDir + " cap must sit on the block boundary");
        }
    }

    /** The lower coordinate along the direction's axis (X=0, Y=1, Z=2 in the box layout). */
    private static double boxMin(Shape aShape, Direction aDir) {
        return aShape.box()[aDir.getAxis().ordinal()];
    }

    /** The upper coordinate along the direction's axis. */
    private static double boxMax(Shape aShape, Direction aDir) {
        return aShape.box()[3 + aDir.getAxis().ordinal()];
    }

    @Test
    public void singleBitMaskProducesTheUpstreamArmBox() {
        // bit 4 = WEST (3D data value) — upstream :124 SIDE_X_NEG: [0, h, h, h, 1-h, 1-h]
        float tD = 6, tHalf = (1.0F - tD / 16F) / 2F;
        List<Shape> tShapes = GTWireBakedModel.planShapes(false, (int) tD, 1 << 4);
        // core (6) + arm: N/S/E/W-down... the arm's non-opposite faces = DOWN, UP, NORTH, SOUTH + WEST cap
        assertEquals(6 + 5, tShapes.size());
        assertEquals(1, countFace(tShapes, Direction.EAST), "only the core has an EAST face (the arm's buried opposite face is skipped)");
        Shape tCap = tShapes.stream().filter(s -> s.face() == Direction.WEST && s.cull() == Direction.WEST).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {0, tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf}, tCap.box(), 1e-9,
                "the WEST arm box must equal the :124 SIDE_X_NEG transcription");
        // DOWN arm (bit 0) — :125 SIDE_Y_NEG
        List<Shape> tDown = GTWireBakedModel.planShapes(false, (int) tD, 1);
        Shape tDownCap = tDown.stream().filter(s -> s.face() == Direction.DOWN && s.cull() == Direction.DOWN).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf, 0, tHalf, 1 - tHalf, tHalf, 1 - tHalf}, tDownCap.box(), 1e-9,
                "the DOWN arm box must equal the :125 SIDE_Y_NEG transcription");
    }

    @Test
    public void cableMaskZeroOverlaysTheTierOnTheCore() {
        // cableGt01: diameter 4px = 0.25 → TINY (< 0.37, upstream :238)
        List<Shape> tShapes = GTWireBakedModel.planShapes(true, 4, 0);
        assertEquals(12, tShapes.size(), "core: 6 material + 6 tier overlay");
        assertEquals(6, countKind(tShapes, SpriteKind.INSULATION_TINY));
        for (Shape tShape : tShapes) {
            if (tShape.kind() == SpriteKind.INSULATION_TINY) {
                assertEquals(1, tShape.tintIndex(), "the insulation jacket rides tint index 1");
                // inflated by the z-fight epsilon over the material quad
                assertEquals(core(4)[0] - GTWireBakedModel.INSULATION_EPSILON, tShape.box()[0], 1e-9);
            } else {
                assertEquals(0, tShape.tintIndex());
            }
        }
    }

    @Test
    public void cableFullMaskIsFullCoreWithCappedArms() {
        // cableGt12: diameter 16px = 1.0 → HUGE (not < 0.99, upstream :238)
        List<Shape> tShapes = GTWireBakedModel.planShapes(true, 16, 63);
        // core 6 FULL + per arm: 4 FULL walls + WIRE cap + HUGE cap = 6 → 6 + 36 = 42
        assertEquals(42, tShapes.size());
        assertEquals(6 + 24, countKind(tShapes, SpriteKind.INSULATION_FULL), "core faces + arm side walls (:139 getTextureSide)");
        assertEquals(6, countKind(tShapes, SpriteKind.WIRE), "the six outer caps carry the material");
        assertEquals(6, countKind(tShapes, SpriteKind.INSULATION_HUGE), "the six outer caps carry the tier overlay");
        // the walls and the full core ride the gray jacket tint, caps' material quad does not
        for (Shape tShape : tShapes) {
            if (tShape.kind() == SpriteKind.WIRE) assertEquals(0, tShape.tintIndex());
            else assertEquals(1, tShape.tintIndex());
        }
    }

    @Test
    public void insulationTierLadderMatchesUpstream238() {
        assertEquals(SpriteKind.INSULATION_TINY, GTWireBakedModel.insulationTier(0.25F), "cableGt01 = 4/16");
        assertEquals(SpriteKind.INSULATION_SMALL, GTWireBakedModel.insulationTier(0.375F), "cableGt02 = 6/16");
        assertEquals(SpriteKind.INSULATION_MEDIUM, GTWireBakedModel.insulationTier(0.5F), "cableGt04 = 8/16");
        assertEquals(SpriteKind.INSULATION_LARGE, GTWireBakedModel.insulationTier(0.75F), "cableGt08 = 12/16");
        assertEquals(SpriteKind.INSULATION_HUGE, GTWireBakedModel.insulationTier(1.0F), "cableGt12 = 16/16");
    }

    @Test
    public void everyMaskHasAMaterialQuadAndThinnerMasksHaveFewerQuads() {
        for (int tMask = 0; tMask < 64; tMask++) {
            List<Shape> tBare = GTWireBakedModel.planShapes(false, 2, tMask);
            List<Shape> tCable = GTWireBakedModel.planShapes(true, 4, tMask);
            assertTrue(tBare.size() >= 6, "mask " + tMask + " bare: at least the core");
            assertTrue(tCable.size() > tBare.size(), "mask " + tMask + " cable: the jacket adds quads");
            // quad count is monotone in the mask (each bit adds one 5-face arm = 5 bare / 6 cable faces)
            int tBits = Integer.bitCount(tMask);
            assertEquals(6 + 5 * tBits, tBare.size(), "bare quad count at mask " + tMask);
            assertEquals((tMask == 0 ? 12 : 6) + 6 * tBits, tCable.size(), "cable quad count at mask " + tMask);
        }
    }

    @Test
    public void legacyDiameterClampsToTheUpstreamFloor() {
        // the p7 legacy pair carries diameter 0 — the readFromNBT2 :64 clamp floors at PX_P[2] = 2/16
        List<Shape> tShapes = GTWireBakedModel.planShapes(false, 0, 0);
        assertEquals(0.4375, tShapes.get(0).box()[0], 1e-9, "(1 - 0.125) / 2 = 0.4375");
    }

    // -------------------------------------------------------------------------
    // task p11-wire-fiber-texture — the laser (fiber) family form
    // -------------------------------------------------------------------------

    @Test
    public void fiberMaskZeroIsTheCorePair() {
        // PX_P[6] = 6/16 diameter (Loader:1815) — the core pair: 6 dyed bases + 6 untinted overlays
        List<Shape> tShapes = GTWireBakedModel.planShapesFiber(6, 0);
        assertEquals(12, tShapes.size(), "core: 6 FIBER_WIRE bases + 6 FIBER_WIRE_OVERLAY twins");
        assertEquals(6, countKind(tShapes, SpriteKind.WIRE));
        assertEquals(6, countKind(tShapes, SpriteKind.FIBER_OVERLAY));
        for (Shape tShape : tShapes) {
            if (tShape.kind() == SpriteKind.WIRE) {
                assertEquals(0, tShape.tintIndex(), "the fiber base carries the mRGBa dye on tint index 0");
            } else {
                assertEquals(-1, tShape.tintIndex(), "FIBER_WIRE_OVERLAY is untinted (the no-colour BlockTextureDefault)");
                assertEquals(SpriteKind.FIBER_OVERLAY, tShape.kind());
                assertEquals(core(6)[0] - GTWireBakedModel.INSULATION_EPSILON, tShape.box()[0], 1e-9,
                        "the overlay box is inflated past the base face (the same outward form as the tier masks)");
                assertNull(tShape.cull(), "internal faces never cull");
            }
        }
    }

    @Test
    public void fiberFullMaskTwinsEveryMaterialQuad() {
        List<Shape> tShapes = GTWireBakedModel.planShapesFiber(6, 63);
        // the bare plan (6 core + 6x5 arm faces = 36) plus one twin each — :121/:122 pick
        // the SAME pair for getTextureSide AND getTextureConnected, so caps AND walls twin
        assertEquals(72, tShapes.size());
        assertEquals(36, countKind(tShapes, SpriteKind.WIRE));
        assertEquals(36, countKind(tShapes, SpriteKind.FIBER_OVERLAY));
        assertEquals(0, countKind(tShapes, SpriteKind.INSULATION_FULL), "no insulation on the fiber family");
        // every twin mirrors its base one-to-one: same face + same cull + the exact
        // epsilon-inflated box (inflate = outward on all six planes, the tier-mask form)
        for (Shape tBase : tShapes) {
            if (tBase.kind() != SpriteKind.WIRE) continue;
            double[] tExpectedBox = new double[6];
            for (int tI = 0; tI < 6; tI++) tExpectedBox[tI] = tBase.box()[tI] + (tI < 3 ? -1.0 : 1.0) * GTWireBakedModel.INSULATION_EPSILON;
            Shape tTwin = tShapes.stream().filter(s -> s.kind() == SpriteKind.FIBER_OVERLAY
                    && s.face() == tBase.face() && s.cull() == tBase.cull()).filter(s -> {
                        for (int tI = 0; tI < 6; tI++) if (Math.abs(s.box()[tI] - tExpectedBox[tI]) > 1e-9) return false;
                        return true;
                    }).findFirst().orElse(null);
            assertTrue(tTwin != null, "missing overlay twin for the " + tBase.face() + " base quad "
                    + java.util.Arrays.toString(tBase.box()));
        }
    }

    @Test
    public void fiberPlansNeverLeakIntoTheElectricForms() {
        // the electric/redstone planner entry stays overlay-free — the p11 branch keys off
        // Params.overlaySprite, the (insulated, diameter, mask) plans are untouched
        for (int tMask = 0; tMask < 64; tMask++) {
            assertEquals(0, countKind(GTWireBakedModel.planShapes(false, 6, tMask), SpriteKind.FIBER_OVERLAY),
                    "bare electric plan must not grow fiber overlays at mask " + tMask);
            assertEquals(0, countKind(GTWireBakedModel.planShapes(true, 6, tMask), SpriteKind.FIBER_OVERLAY),
                    "cable electric plan must not grow fiber overlays at mask " + tMask);
        }
        assertEquals(6, GTWireBakedModel.planShapes(false, 6, 0).size(), "the bare plan count is unchanged by p11");
    }

    @Test
    public void fiberQuadCountIsMonotoneInTheMask() {
        for (int tMask = 0; tMask < 64; tMask++) {
            int tBits = Integer.bitCount(tMask);
            assertEquals(2 * (6 + 5 * tBits), GTWireBakedModel.planShapesFiber(6, tMask).size(),
                    "fiber quad count at mask " + tMask + " = the bare count doubled");
        }
    }
}
