/**
 * The connection-state quad structure of {@link GTRodBakedModel} (task rod-render-pool)
 * — the {@link gregtech6.client.wire.GTWireQuadStructureTest} form carried onto the
 * rod planner: the planner is a pure (diameter, mask, bands) → face/box/tint/sprite-kind
 * function. Every expected box is the direct transcription of the upstream connector
 * geometry (TileEntityBase10ConnectorRendered setBlockBounds2 :113-133, arm length 0) —
 * the pipe diameters NBT_DIAMETER PX_P[4/6/8/12/16/16/16]
 * (MultiTileEntityPipeFluid.java:92-98), the logistics wire PX_P[6] (Loader :1819), the
 * axles PX_P[6/9/12/16] (CS.java:492), the overlay bands the TextureSet two-pass stack.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import gregtech6.client.render.GTRodBakedModel.Shape;
import gregtech6.client.render.GTRodBakedModel.SpriteKind;

public class GTRodQuadStructureTest extends GTOfflineTestBase {

    /** The core box at diameter d/16 (upstream :115). */
    private static double[] core(int aDiameterPx) {
        double tHalf = (1.0 - aDiameterPx / 16.0) / 2.0;
        return new double[] {tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf, 1 - tHalf};
    }

    private static long countKind(List<Shape> aShapes, SpriteKind aKind) {
        return aShapes.stream().filter(s -> s.kind() == aKind).count();
    }

    @Test
    public void unconnectedPipeCarriesTheConnectedArtOnAllSixCoreFaces() {
        // the wood small row: PX_P[6] (MultiTileEntityPipeFluid :93). Task
        // pipe-arm-atlas-facemap — an ISOLATED pipe renders the CONNECTED art on every
        // core face (upstream :138 mConnections == 0 branch), not the side art.
        List<Shape> tShapes = GTRodBakedModel.planShapes(6, 0, 0);
        assertEquals(6, tShapes.size(), "a standalone pipe is just the 6 core faces");
        for (Shape tShape : tShapes) {
            assertEquals(SpriteKind.ARM, tShape.kind(), "the :138 isolated pipe rides the connected art (:265)");
            assertEquals(0, tShape.tintIndex(), "material colour rides tint index 0");
            assertNull(tShape.cull(), "internal faces never cull");
            org.junit.jupiter.api.Assertions.assertArrayEquals(core(6), tShape.box(), 1e-9);
        }
    }

    /**
     * The per-face texture table (task pipe-arm-atlas-facemap, upstream :136-140): a
     * single connection's arm = end cap ARM + four side faces BASE + the buried face
     * absent; a connected pipe's core stays BASE.
     */
    @Test
    public void facemapFollowsTheUpstreamPerFaceTable() {
        // WEST only (bit 4), the large PX_P[12]
        float tHalf = (1.0F - 12 / 16F) / 2F;
        List<Shape> tShapes = GTRodBakedModel.planShapes(12, 1 << 4, 0);
        assertEquals(6 + 5, tShapes.size());
        assertEquals(10, countKind(tShapes, SpriteKind.BASE), "the core's six faces + the arm's four side faces (:139 getTextureSide)");
        assertEquals(1, countKind(tShapes, SpriteKind.ARM), "only the arm's end cap rides the connected art (:139)");
        Shape tCap = tShapes.stream().filter(s -> s.face() == Direction.WEST && s.cull() == Direction.WEST).findFirst().orElseThrow();
        assertEquals(SpriteKind.ARM, tCap.kind(), "the end face (aSide == the pass direction) = getTextureConnected");
        for (Direction tSide : List.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)) {
            Shape tFlank = tShapes.stream()
                    .filter(s -> s.face() == tSide && java.util.Arrays.equals(s.box(), GTRodBakedModel.armBox(Direction.WEST, tHalf)))
                    .findFirst().orElseThrow();
            assertEquals(SpriteKind.BASE, tFlank.kind(), "the " + tSide + " flank of the arm rides the side art (getTextureSide)");
        }
        // the item form (ITEM_MASK = 12, the N-S segment): two ARM caps, geometry unchanged
        List<Shape> tItem = GTRodBakedModel.planShapes(8, GTRodBakedModel.ITEM_MASK, 0);
        assertEquals((6 + 5 * 2), tItem.size());
        assertEquals(2, countKind(tItem, SpriteKind.ARM), "the N and S end caps");
        assertEquals(6 + 8, countKind(tItem, SpriteKind.BASE), "core six + both arms' four flanks");
        Shape tNCap = tItem.stream().filter(s -> s.face() == Direction.NORTH && s.cull() == Direction.NORTH).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf(8), tHalf(8), 0, 1 - tHalf(8), 1 - tHalf(8), tHalf(8)}, tNCap.box(), 1e-9,
                "the N cap sits flush on the block boundary (the arm box transcription, :126)");
    }

    private static double tHalf(int aDiameterPx) {
        return (1.0 - aDiameterPx / 16.0) / 2.0;
    }

    @Test
    public void fullyConnectedPipeHasCorePlusFiveSidedArms() {
        List<Shape> tShapes = GTRodBakedModel.planShapes(8, 63, 0);
        assertEquals(6 + 6 * 5, tShapes.size(), "core + one 5-face arm per side (the buried face is skipped, :139)");
        // task pipe-arm-atlas-facemap — the :136-140 per-face table: the core rides BASE
        // (pipeSide, :264), each arm its end cap ARM (the per-diameter connected art, :265)
        // and its four side faces BASE (:139 getTextureSide)
        assertEquals(6 + 24, countKind(tShapes, SpriteKind.BASE));
        assertEquals(6, countKind(tShapes, SpriteKind.ARM));
        // every side has an outward cap whose face plane sits exactly on the block boundary
        for (Direction tDir : Direction.values()) {
            Shape tCap = tShapes.stream().filter(s -> s.face() == tDir && s.cull() == tDir).findFirst().orElse(null);
            assertTrue(tCap != null, "missing outward cap for " + tDir);
            assertEquals(0, tCap.tintIndex());
            double tFacePlane = tDir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? tCap.box()[3 + tDir.getAxis().ordinal()] : tCap.box()[tDir.getAxis().ordinal()];
            assertEquals(tDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 : 0.0, tFacePlane, 1e-9,
                    "the " + tDir + " cap must sit on the block boundary");
        }
    }

    @Test
    public void singleBitMaskProducesTheUpstreamArmBox() {
        // the huge diameter PX_P[16] is degenerate for arms (half=0); use the large PX_P[12]
        float tHalf = (1.0F - 12 / 16F) / 2F;
        List<Shape> tShapes = GTRodBakedModel.planShapes(12, 1 << 4, 0);
        assertEquals(6 + 5, tShapes.size());
        assertEquals(1, tShapes.stream().filter(s -> s.face() == Direction.EAST).count(),
                "only the core has an EAST face (the arm's buried opposite face is skipped)");
        Shape tCap = tShapes.stream().filter(s -> s.face() == Direction.WEST && s.cull() == Direction.WEST).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {0, tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf}, tCap.box(), 1e-9,
                "the WEST arm box must equal the :124 SIDE_X_NEG transcription");
        List<Shape> tDown = GTRodBakedModel.planShapes(12, 1, 0);
        Shape tDownCap = tDown.stream().filter(s -> s.face() == Direction.DOWN && s.cull() == Direction.DOWN).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf, 0, tHalf, 1 - tHalf, tHalf, 1 - tHalf}, tDownCap.box(), 1e-9,
                "the DOWN arm box must equal the :125 SIDE_Y_NEG transcription");
    }

    /**
     * Task rod-armbox-sixpin: all six arm boxes pinned whole, each the coordinate-by-coordinate
     * transcription of its upstream case line (TileEntityBase10ConnectorRendered :124-129,
     * tLength = 0). The former rod pins (singleBitMaskProducesTheUpstreamArmBox) covered only
     * WEST and DOWN, and the cap-boundary check above only sees the outer plane — the same
     * blind spot that let the wire model's NORTH :126 inner-plane typo survive (the wire card
     * sealed its side; the rod twin was already fixed by the pipe card b452cd67aa with no pin
     * of its own). This pin seals that rod-side blind spot. planBaseShapes :436 is the only
     * production caller of armBox, so pinning armBox here pins the plan path too.
     */
    @Test
    public void everyArmBoxIsTheUpstream124to129Transcription() {
        // the wood small row PX_P[6] (MultiTileEntityPipeFluid :93; PX_P[16] is degenerate, half = 0)
        float tD = 6, tHalf = (1.0F - tD / 16F) / 2F;
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {0, tHalf, tHalf, tHalf, 1 - tHalf, 1 - tHalf}, GTRodBakedModel.armBox(Direction.WEST, tHalf), 1e-9,
                "the WEST arm box must equal the :124 SIDE_X_NEG transcription");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf, 0, tHalf, 1 - tHalf, tHalf, 1 - tHalf}, GTRodBakedModel.armBox(Direction.DOWN, tHalf), 1e-9,
                "the DOWN arm box must equal the :125 SIDE_Y_NEG transcription");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf, tHalf, 0, 1 - tHalf, 1 - tHalf, tHalf}, GTRodBakedModel.armBox(Direction.NORTH, tHalf), 1e-9,
                "the NORTH arm box must equal the :126 SIDE_Z_NEG transcription (z-max = aHalf, the typo pin)");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {1 - tHalf, tHalf, tHalf, 1, 1 - tHalf, 1 - tHalf}, GTRodBakedModel.armBox(Direction.EAST, tHalf), 1e-9,
                "the EAST arm box must equal the :127 SIDE_X_POS transcription");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf, 1 - tHalf, tHalf, 1 - tHalf, 1, 1 - tHalf}, GTRodBakedModel.armBox(Direction.UP, tHalf), 1e-9,
                "the UP arm box must equal the :128 SIDE_Y_POS transcription");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {tHalf, tHalf, 1 - tHalf, 1 - tHalf, 1 - tHalf, 1}, GTRodBakedModel.armBox(Direction.SOUTH, tHalf), 1e-9,
                "the SOUTH arm box must equal the :129 SIDE_Z_POS transcription");
    }

    @Test
    public void overlayBandsTwinEveryBaseQuadUntintedOneEpsilonOut() {
        List<Shape> tOne = GTRodBakedModel.planShapes(4, 0, 1);
        assertEquals(12, tOne.size(), "core: 6 base + 6 overlay twins");
        for (Shape tShape : tOne) {
            if (tShape.kind() == SpriteKind.OVERLAY) {
                assertEquals(-1, tShape.tintIndex(), "the overlay outline never tints");
                assertEquals(0, tShape.band());
            } else {
                assertEquals(0, tShape.tintIndex());
            }
        }
        // the twin of the core DOWN face: ONLY its own plane moved one epsilon out (task
        // pipe-arm-atlas-facemap — a translation, not the former six-way inflation whose
        // epsilon strip coplanar-overlapped the flush arm twins at every core/arm seam)
        Shape tDown = tOne.stream().filter(s -> s.kind() == SpriteKind.OVERLAY && s.face() == Direction.DOWN).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new double[] {core(4)[0], core(4)[1] - GTRodBakedModel.OVERLAY_EPSILON, core(4)[2],
                        core(4)[3], core(4)[4], core(4)[5]}, tDown.box(), 1e-9,
                "band 0 = the base box translated one epsilon out along the normal, no lateral growth");
        // the restrictive rows: two bands (pipe_side_overlay + pipe_restrictor), full mask
        List<Shape> tTwo = GTRodBakedModel.planShapes(8, 63, 2);
        assertEquals((6 + 30) * 3, tTwo.size(), "every base quad twins twice");
        assertEquals(30, countKind(tTwo, SpriteKind.BASE), "core six + the arms' four flanks each (:139 getTextureSide)");
        assertEquals(6, countKind(tTwo, SpriteKind.ARM), "the six end caps (:139 getTextureConnected)");
        assertEquals(72, countKind(tTwo, SpriteKind.OVERLAY));
        double tBand1 = tTwo.stream().filter(s -> s.kind() == SpriteKind.OVERLAY && s.band() == 1).findFirst().orElseThrow().box()[1];
        assertEquals(core(8)[1] - 2 * GTRodBakedModel.OVERLAY_EPSILON, tBand1, 1e-9,
                "band 1 steps one more epsilon out (the stepped decal stack)");
    }

    /**
     * The twin de-overlap pin (task pipe-arm-atlas-facemap acceptance): over ALL 64
     * connection masks and both decal bands, no two coplanar overlay twins have a
     * projected-area intersection. The former six-way inflation made the core twin and
     * the flush arm twins overlap in an epsilon strip at every core/arm seam — the
     * flickering black outline line on N-S runs (the user report). N-S, E-W and the
     * L-shapes all live inside the 64-mask sweep.
     */
    @Test
    public void coplanarTwinsNeverOverlapOnAnyConnectionMask() {
        for (int tMask = 0; tMask < 64; tMask++) {
            for (int tBandValue = 0; tBandValue < 2; tBandValue++) {
                final int tBand = tBandValue;
                List<Shape> tTwins = GTRodBakedModel.planShapes(8, tMask, 2).stream()
                        .filter(s -> s.kind() == SpriteKind.OVERLAY && s.band() == tBand).toList();
                for (int tA = 0; tA < tTwins.size(); tA++) {
                    for (int tB = tA + 1; tB < tTwins.size(); tB++) {
                        Shape tShapeA = tTwins.get(tA), tShapeB = tTwins.get(tB);
                        if (tShapeA.face() != tShapeB.face()) continue;
                        if (planeOf(tShapeA) != planeOf(tShapeB)) continue; // different planes never z-fight
                        double tArea = overlapArea(footprint(tShapeA), footprint(tShapeB));
                        assertTrue(tArea <= 1e-12, "mask " + tMask + " band " + tBand + ": coplanar "
                                + tShapeA.face() + " twins overlap by " + tArea + " — boxes "
                                + java.util.Arrays.toString(tShapeA.box()) + " vs "
                                + java.util.Arrays.toString(tShapeB.box()));
                    }
                }
            }
        }
    }

    /** The plane coordinate of a twin's quad along its own normal. */
    private static double planeOf(Shape aShape) {
        return switch (aShape.face()) {
            case DOWN -> aShape.box()[1];
            case UP -> aShape.box()[4];
            case NORTH -> aShape.box()[2];
            case SOUTH -> aShape.box()[5];
            case WEST -> aShape.box()[0];
            case EAST -> aShape.box()[3];
        };
    }

    /** The quad's rectangle over the two in-plane axes: {minA, minB, maxA, maxB}. */
    private static double[] footprint(Shape aShape) {
        double[] tBox = aShape.box();
        return switch (aShape.face()) {
            case UP, DOWN -> new double[] {tBox[0], tBox[2], tBox[3], tBox[5]};
            case NORTH, SOUTH -> new double[] {tBox[0], tBox[1], tBox[3], tBox[4]};
            case WEST, EAST -> new double[] {tBox[2], tBox[1], tBox[5], tBox[4]};
        };
    }

    private static double overlapArea(double[] aA, double[] aB) {
        double tW = Math.min(aA[2], aB[2]) - Math.max(aA[0], aB[0]);
        double tH = Math.min(aA[3], aB[3]) - Math.max(aA[1], aB[1]);
        return Math.max(0, tW) * Math.max(0, tH);
    }

    @Test
    public void maskFollowsTheBlockstateContract() {
        assertEquals(GTRodBakedModel.ITEM_MASK, GTRodBakedModel.maskOf(null),
                "the item form renders the upstream worldObj==null N-S segment (ConnectorStraight :37)");
        // the axle line: AXIS x/y/z = the straight rod (both axis ends connected)
        BlockState tX = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        BlockState tY = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        BlockState tZ = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
        assertEquals(48, GTRodBakedModel.maskOf(tX), "X rod = WEST|EAST");
        assertEquals(3, GTRodBakedModel.maskOf(tY), "Y rod = DOWN|UP");
        assertEquals(12, GTRodBakedModel.maskOf(tZ), "Z rod = NORTH|SOUTH");
    }

    /**
     * The item pipe connection render pin (task pipe-render-closeout acceptance): all 64
     * CONNECTIONS states of a real {@link gregtech6.block.pipe.GTItemPipeBlock} resolve
     * through {@link GTRodBakedModel#maskOf} into state-varying arm geometry — the
     * core+arms plan, never one shared full-cube model (the pre-rod placeholder shape:
     * every state the same 6 faces). The row identity (the brass medium row, PX_P[8])
     * rides the registration table, exactly what the bake dispatch consumes.
     */
    @Test
    public void itemPipeAll64ConnectionStatesProduceStateVaryingArmGeometry() {
        GTMaterialItems.initMaterials();
        gregtech6.registry.GTItemPipes.ItemPipeRow tRow = gregtech6.registry.GTItemPipes.rowByPath("brass_item_pipe_medium");
        // the registry write window for direct block construction (GTPipeTintGateTest.itemPipe shape)
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
        gregtech6.block.pipe.GTItemPipeBlock tPipe = new gregtech6.block.pipe.GTItemPipeBlock(tRow,
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
        int tDiameter = tRow.variant().diameterPx;
        assertEquals(8, tDiameter, "the brass medium row = PX_P[8] (MultiTileEntityPipeItem :76-82)");
        for (int tMask = 0; tMask < 64; tMask++) {
            BlockState tState = tPipe.defaultBlockState().setValue(gregtech6.block.pipe.GTItemPipeBlock.CONNECTIONS, tMask);
            assertEquals(tMask, GTRodBakedModel.maskOf(tState), "the state's CONNECTIONS is the whole mask source");
            List<Shape> tShapes = GTRodBakedModel.planShapes(tDiameter, tMask, 1);
            int tArms = Integer.bitCount(tMask);
            assertEquals((6 + 5 * tArms) * 2, tShapes.size(), "mask " + tMask + " lost the core+arms plan");
            // the geometry IS the state: every connected bit has exactly one outward cap
            // on the block boundary, every unconnected bit has none (the placeholder era
            // rendered all 64 states as the same full cube)
            for (Direction tDir : Direction.values()) {
                long tCaps = tShapes.stream().filter(s -> s.face() == tDir && s.cull() == tDir).count();
                assertEquals((tMask & (1 << tDir.get3DDataValue())) != 0 ? 2 : 0, tCaps,
                        "mask " + tMask + " cap count on " + tDir + " (base + overlay twin)");
            }
        }
        assertEquals(6 * 2, GTRodBakedModel.planShapes(tDiameter, 0, 1).size(), "mask 0 = the bare core stub");
        assertEquals((6 + 30) * 2, GTRodBakedModel.planShapes(tDiameter, 63, 1).size(), "mask 63 = full junction");
    }
}
