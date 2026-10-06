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
    public void unconnectedPipeIsJustTheCoreStub() {
        // the wood small row: PX_P[6] (MultiTileEntityPipeFluid :93)
        List<Shape> tShapes = GTRodBakedModel.planShapes(6, 0, 0);
        assertEquals(6, tShapes.size(), "a standalone pipe is just the 6 core faces");
        for (Shape tShape : tShapes) {
            assertEquals(SpriteKind.BASE, tShape.kind());
            assertEquals(0, tShape.tintIndex(), "material colour rides tint index 0");
            assertNull(tShape.cull(), "internal faces never cull");
            org.junit.jupiter.api.Assertions.assertArrayEquals(core(6), tShape.box(), 1e-9);
        }
    }

    @Test
    public void fullyConnectedPipeHasCorePlusFiveSidedArms() {
        List<Shape> tShapes = GTRodBakedModel.planShapes(8, 63, 0);
        assertEquals(6 + 6 * 5, tShapes.size(), "core + one 5-face arm per side (the buried face is skipped, :139)");
        assertEquals(36, countKind(tShapes, SpriteKind.BASE));
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

    @Test
    public void overlayBandsTwinEveryBaseQuadUntintedAndInflated() {
        List<Shape> tOne = GTRodBakedModel.planShapes(4, 0, 1);
        assertEquals(12, tOne.size(), "core: 6 base + 6 overlay twins");
        for (Shape tShape : tOne) {
            if (tShape.kind() == SpriteKind.OVERLAY) {
                assertEquals(-1, tShape.tintIndex(), "the overlay outline never tints");
                assertEquals(0, tShape.band());
                assertEquals(core(4)[0] - GTRodBakedModel.OVERLAY_EPSILON, tShape.box()[0], 1e-9,
                        "band 0 inflates by one epsilon");
            } else {
                assertEquals(0, tShape.tintIndex());
            }
        }
        // the restrictive rows: two bands (pipe_side_overlay + pipe_restrictor), full mask
        List<Shape> tTwo = GTRodBakedModel.planShapes(8, 63, 2);
        assertEquals((6 + 30) * 3, tTwo.size(), "every base quad twins twice");
        assertEquals(36, countKind(tTwo, SpriteKind.BASE));
        assertEquals(72, countKind(tTwo, SpriteKind.OVERLAY));
        double tBand1 = tTwo.stream().filter(s -> s.kind() == SpriteKind.OVERLAY && s.band() == 1).findFirst().orElseThrow().box()[0];
        assertEquals(core(8)[0] - 2 * GTRodBakedModel.OVERLAY_EPSILON, tBand1, 1e-9,
                "band 1 steps one more epsilon out (the JSON tintedPipeModel 0.01 form)");
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
