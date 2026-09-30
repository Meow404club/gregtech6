/**
 * The selection/collision shapes of the rod connector families (task rod-render-pool
 * spec ③ "getShape/collision 随几何改, 上游 bounds 实读"): the pipes carry the upstream
 * connector envelope (TileEntityBase11ConnectorStraight.addCollisionBoxesToList2 :60 —
 * the core diameter box extended flush on every connected side), the sensor the 2px
 * wall plate (MultiTileEntitySensor.java:250 verbatim — collision stays the full MTE
 * default, MultiTileEntityBlock.java:192), the attachments the taper/spout stack
 * envelopes (MultiTileEntityFluidTap.java:226-233 / MultiTileEntityFluidFunnel
 * :156-163). Offline block construction = the GTWireContactDamageTest unfreeze form.
 * Review rework: the sensor plate hugs the edge OPPOSITE the display face (the
 * wall-mount semantics, MultiTileEntitySensor.java:148-150 + :257) and the collision
 * rides the same plate (:249 — the "MTE default full cell" pin was a misread of the
 * MultiTileEntityBlock.java:192 IMTE_GetCollisionBoundingBoxFromPool router).
 */
package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.attachment.GTAttachmentSmallBlock;
import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.pipe.GTItemPipeBlock;
import gregtech6.block.sensors.GTSensorBlock;
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;

public class GTRodShapeTest extends gregtech6.tileentity.GTOfflineTestBase {

    // the blocks construct LAZILY (after the base @BeforeAll bootstrap) — the
    // GTWireContactDamageTest memoized-form: the unfreeze runs per construction call
    private static void unfreeze() {
        try {
            Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    private static GTFluidPipeBlock fluidPipe(String aPath) {
        unfreeze();
        return new GTFluidPipeBlock(GTFluidPipes.ROWS.stream().filter(r -> r.path().equals(aPath)).findFirst().orElseThrow(),
                BlockBehaviour.Properties.of());
    }

    private static GTItemPipeBlock itemPipe(String aPath) {
        unfreeze();
        return new GTItemPipeBlock(GTItemPipes.ROWS.stream().filter(r -> r.path().equals(aPath)).findFirst().orElseThrow(),
                BlockBehaviour.Properties.of());
    }

    private static AABB onlyBox(VoxelShape aShape) {
        assertEquals(1, aShape.toAabbs().size(), "the shape is one box");
        return aShape.toAabbs().get(0);
    }

    private static void assertBox(VoxelShape aShape, double aMinX, double aMinY, double aMinZ,
            double aMaxX, double aMaxY, double aMaxZ) {
        AABB tBox = onlyBox(aShape);
        assertEquals(aMinX, tBox.minX, 1e-9, "minX");
        assertEquals(aMinY, tBox.minY, 1e-9, "minY");
        assertEquals(aMinZ, tBox.minZ, 1e-9, "minZ");
        assertEquals(aMaxX, tBox.maxX, 1e-9, "maxX");
        assertEquals(aMaxY, tBox.maxY, 1e-9, "maxY");
        assertEquals(aMaxZ, tBox.maxZ, 1e-9, "maxZ");
    }

    // ------------------------------------------------------------------
    // the pipes: the connection-aware thin envelope
    // ------------------------------------------------------------------

    @Test
    public void fluidPipeStubIsTheDiameterCore() {
        // wood small = PX_P[6]: inset (16-6)/32 = 0.3125
        BlockState tState = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0);
        assertBox(tState.getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()),
                0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875);
    }

    @Test
    public void fluidPipeExtendsFlushOnConnectedSides() {
        // WEST|EAST connected (bit 4|5 = 48): the rod runs the full X span
        BlockState tState = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 48);
        assertBox(tState.getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()),
                0, 0.3125, 0.3125, 1, 0.6875, 0.6875);
        // DOWN|UP (3): the full Y span
        BlockState tVertical = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 3);
        assertBox(tVertical.getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()),
                0.3125, 0, 0.3125, 0.6875, 1, 0.6875);
    }

    @Test
    public void itemPipeRidesItsOwnDiameter() {
        // restrictive medium = PX_P[8]: inset (16-8)/32 = 0.25
        BlockState tState = itemPipe("brass_item_pipe_restrictive_medium").defaultBlockState()
                .setValue(GTItemPipeBlock.CONNECTIONS, 0);
        assertBox(tState.getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()),
                0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    }

    // ------------------------------------------------------------------
    // the sensor: the 2px wall plate on the side OPPOSITE the display
    // ------------------------------------------------------------------

    private static GTSensorBlock sSensor;

    private static GTSensorBlock sensor() {
        if (sSensor == null) {
            unfreeze();
            sSensor = new GTSensorBlock(() -> null, BlockBehaviour.Properties.of());
        }
        return sSensor;
    }

    @Test
    public void sensorSelectionIsTheTwoPixelPlate() {
        // MultiTileEntitySensor :148-150 — the plate hugs the WALL behind the display:
        // mFacing=SIDE_Z_NEG (north) puts the body at PX_P[14]..PX_N[0] = z 14..16,
        // SIDE_X_POS (east) at PX_P[0]..PX_N[14] = x 0..2, SIDE_Y_POS (up) at y 0..2
        // (:257 isSurfaceOpaque2 = OPOS[mFacing] — the wall side is the solid one)
        assertBox(sensor().defaultBlockState().setValue(GTSensorBlock.FACING, Direction.NORTH)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0, 0, 0.875, 1, 1, 1);
        assertBox(sensor().defaultBlockState().setValue(GTSensorBlock.FACING, Direction.EAST)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0, 0, 0, 0.125, 1, 1);
        assertBox(sensor().defaultBlockState().setValue(GTSensorBlock.FACING, Direction.UP)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0, 0, 0, 1, 0.125, 1);
    }

    @Test
    public void sensorCollisionIsTheSameTwoPixelPlate() {
        // MultiTileEntitySensor.java:249 — getCollisionBoundingBoxFromPool = the same
        // plate box as the selection (:250); the vanilla collision = shape default
        // carries it (MultiTileEntityBlock.java:192 routes the IMTE interface)
        assertBox(sensor().defaultBlockState().setValue(GTSensorBlock.FACING, Direction.NORTH)
                .getCollisionShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0, 0, 0.875, 1, 1, 1);
        assertBox(sensor().defaultBlockState().setValue(GTSensorBlock.FACING, Direction.UP)
                .getCollisionShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0, 0, 0, 1, 0.125, 1);
    }

    // ------------------------------------------------------------------
    // the attachments: the upstream taper/spout envelopes
    // ------------------------------------------------------------------

    private static GTAttachmentSmallBlock sTap;
    private static GTAttachmentSmallBlock sFunnel;

    private static GTAttachmentSmallBlock tap() {
        if (sTap == null) {
            unfreeze();
            sTap = new GTAttachmentSmallBlock(
                    GT6Attachments.ROWS.stream().filter(r -> r.family() == GTAttachmentSmallBlock.Family.TAP).findFirst().orElseThrow(),
                    false, () -> null, BlockBehaviour.Properties.of());
        }
        return sTap;
    }

    private static GTAttachmentSmallBlock funnel() {
        if (sFunnel == null) {
            unfreeze();
            sFunnel = new GTAttachmentSmallBlock(
                    GT6Attachments.ROWS.stream().filter(r -> r.family() == GTAttachmentSmallBlock.Family.FUNNEL).findFirst().orElseThrow(),
                    false, () -> null, BlockBehaviour.Properties.of());
        }
        return sFunnel;
    }

    @Test
    public void tapEnvelopeMatchesTheUpstreamSelectionBox() {
        // MultiTileEntityFluidTap :226-233 — (6,3,0)-(10,7,6) px on the north mount
        assertBox(tap().defaultBlockState().setValue(GTAttachmentSmallBlock.FACING, Direction.NORTH)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0.375, 0.1875, 0, 0.625, 0.4375, 0.375);
        assertBox(tap().defaultBlockState().setValue(GTAttachmentSmallBlock.FACING, Direction.EAST)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0.625, 0.1875, 0.375, 1, 0.4375, 0.625);
    }

    @Test
    public void funnelEnvelopeCoversTheDownMount() {
        // MultiTileEntityFluidFunnel :156-163 — (5,7,0)-(11,10,6) px north / (5,0,5)-(11,3,11) down
        assertBox(funnel().defaultBlockState().setValue(GTAttachmentSmallBlock.FACING, Direction.NORTH)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0.3125, 0.4375, 0, 0.6875, 0.625, 0.375);
        assertBox(funnel().defaultBlockState().setValue(GTAttachmentSmallBlock.FACING, Direction.DOWN)
                .getShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()), 0.3125, 0, 0.3125, 0.6875, 0.1875, 0.6875);
    }

    @Test
    public void attachmentsStillBlockNothing() {
        // TileEntityBase10Attachment:35 — the collision-free mount
        assertTrue(tap().defaultBlockState().setValue(GTAttachmentSmallBlock.FACING, Direction.NORTH)
                .getCollisionShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()).isEmpty(),
                "the tap collision stays empty");
        assertTrue(funnel().defaultBlockState().setValue(GTAttachmentSmallBlock.FACING, Direction.DOWN)
                .getCollisionShape((net.minecraft.world.level.BlockGetter) null, (net.minecraft.core.BlockPos) null, CollisionContext.empty()).isEmpty(),
                "the funnel collision stays empty");
    }
}
