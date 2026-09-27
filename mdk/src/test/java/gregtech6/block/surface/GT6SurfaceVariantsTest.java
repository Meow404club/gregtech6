/**
 * Task debt-issue12-shape-follow-tilt (GitHub #12 residual) — the shared variant table
 * unit: the seed-chain transcription, the scan-order bit alignment, and the FACING
 * rotation math the selection box rides.
 *
 * <p>The seed-chain assertion is a DOUBLE transcription on purpose: the helper below
 * re-derives the draw with a bare {@code new java.util.Random()} (the LCG family the
 * research pinned — RandomSource.create() must be the SingleThreadedRandomSource
 * java.util.Random wrapper or the draws diverge) and a hand-summed total weight, so
 * {@link GT6SurfaceVariants#pick} sharing vanilla helpers with the test would still
 * fail if its chain drifted one line.
 */
package gregtech6.block.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

class GT6SurfaceVariantsTest {

    @BeforeAll
    static void boot() {
        // the version detect must precede bootStrap (the GT6SurfaceBlocksTest recipe — a
        // bare-JVM first boot poisons DataFixers for every later suite in this JVM)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        // offline Block construction needs the block registry temporarily unfrozen (the
        // GTWireContactDamageTest.block / GT6SurfaceBlocksTest.stickSelection... form)
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
        STICK = new GT6SurfaceStickBlock(
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission());
    }

    static GT6SurfaceStickBlock STICK;

    /**
     * The INDEPENDENT transcription: bare java.util.Random (not RandomSource — the test
     * fails if pick() ever leaves the LCG family), hand-summed total (not
     * getTotalWeight), the draw arithmetic copied from the pinned chain, then the real
     * vanilla {@link WeightedRandom#getWeightedItem} scan both faces consume.
     */
    private static <V extends GT6SurfaceVariants.Variant> V handDraw(V[] aTable, BlockState aState, BlockPos aPos) {
        java.util.Random tRandom = new java.util.Random(); // the LCG family (SingleThreadedRandomSource wraps this)
        tRandom.setSeed(aState.getSeed(aPos));
        int tTotal = 0;
        for (V tVariant : aTable) tTotal += tVariant.weight();
        int tDraw = Math.abs((int) tRandom.nextLong()) % tTotal;
        return WeightedRandom.getWeightedItem(List.of(aTable), tDraw).orElse(aTable[0]);
    }

    /**
     * The card's seed determinism pin: for a deterministic position sweep, the helper
     * table pick IS the hand-copied draw — same position, same variant, every time; the
     * same position queried twice never flips (the same-position-stable face of the
     * renderer chain). The sweep must also reach EVERY stick entry (a degenerate
     * first-entry-always pass would make the follow claim vacuous).
     */
    @Test
    void pickReplaysTheHandCopiedRendererChain() {
        int[] tHits = new int[GT6SurfaceVariants.Stick.values().length];
        int[] tRockHits = new int[GT6SurfaceVariants.Rock.values().length];
        GT6SurfaceVariants.Rock[] tRockTable = GT6SurfaceVariants.Rock.values();
        for (int x = -3; x <= 3; x++) for (int y = 0; y <= 2; y++) for (int z = -3; z <= 3; z++) {
            BlockPos tPos = new BlockPos(x, y, z);
            for (Direction tFacing : List.of(Direction.DOWN, Direction.NORTH, Direction.EAST)) {
                BlockState tState = STICK.defaultBlockState().setValue(GT6SurfaceRockBlock.FACING, tFacing);
                GT6SurfaceVariants.Stick tExpected = handDraw(GT6SurfaceVariants.Stick.values(), tState, tPos);
                assertEquals(tExpected, GT6SurfaceVariants.pick(GT6SurfaceVariants.Stick.values(), tState, tPos),
                        "stick draw at " + tPos + "/" + tFacing);
                assertEquals(tExpected, GT6SurfaceVariants.pick(GT6SurfaceVariants.Stick.values(), tState, tPos),
                        "same position, same variant (the stable-draw face)");
                // the rock rides the same chain over its own table
                assertEquals(handDraw(tRockTable, tState, tPos), GT6SurfaceVariants.pick(tRockTable, tState, tPos),
                        "rock draw at " + tPos + "/" + tFacing);
                if (tFacing == Direction.DOWN) {
                    tHits[tExpected.ordinal()]++;
                    tRockHits[handDraw(tRockTable, tState, tPos).ordinal()]++;
                }
            }
        }
        for (int i = 0; i < tHits.length; i++)
            assertTrue(tHits[i] > 0, "sweep must reach every stick variant (entry " + i + " never drawn)");
        for (int i = 0; i < tRockHits.length; i++)
            assertTrue(tRockHits[i] > 0, "sweep must reach every rock tier (entry " + i + " never drawn)");
    }

    /**
     * The bit-alignment pin: declaration order == the WeightedRandom scan order == the
     * JSON array order the datagen emits. The scan maps cumulative draw i to entry i
     * for the uniform stick table (0..7), and the 3/2/1 rock weights to 0..2 / 3..4 / 5
     * — any declaration-order change in the enums breaks this and silently rotates the
     * selection box against the rendered band.
     */
    @Test
    void scanOrderIsDeclarationOrder() {
        GT6SurfaceVariants.Stick[] tStick = GT6SurfaceVariants.Stick.values();
        assertEquals(8, tStick.length, "the 5-8 band lands at 8");
        for (int i = 0; i < 8; i++)
            assertEquals(tStick[i], WeightedRandom.getWeightedItem(List.of(tStick), i).orElseThrow(),
                    "stick draw " + i + " lands entry " + i);
        GT6SurfaceVariants.Rock[] tRock = GT6SurfaceVariants.Rock.values();
        for (int i = 0; i <= 2; i++)
            assertEquals(GT6SurfaceVariants.Rock.REPRESENTATIVE, WeightedRandom.getWeightedItem(List.of(tRock), i).orElseThrow(),
                    "rock draw " + i + " lands the w3 representative");
        for (int i = 3; i <= 4; i++)
            assertEquals(GT6SurfaceVariants.Rock.MID, WeightedRandom.getWeightedItem(List.of(tRock), i).orElseThrow(),
                    "rock draw " + i + " lands the w2 mid tier");
        assertEquals(GT6SurfaceVariants.Rock.SMALL, WeightedRandom.getWeightedItem(List.of(tRock), 5).orElseThrow(),
                "rock draw 5 lands the w1 small tier");
    }

    /**
     * The FACING rotation pin, anchored to the VANILLA convention (not our own table):
     * end_rod "facing=north" = {"x": 90} tips the up-model's head to -Z, so JSON x=90
     * maps a point (x,y,z) to (x, z, 16-y) — the card's x-rot90 formula. The
     * r3-stick-shape-random C2 table carried x90/x270 TRANSPOSED against the emitted
     * dispatch (NORTH/SOUTH wireframes sat on the wrong side of the block); these pins
     * are the corrected boxes.
     */
    @Test
    void facingRotationsMatchTheVanillaConvention() {
        // the default centered bar through every facing (raw box 2,0,7,14,2,9)
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.CENTERED_X, Direction.DOWN), 2, 0, 7, 14, 2, 9, "down");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.CENTERED_X, Direction.UP), 2, 14, 7, 14, 16, 9, "up");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.CENTERED_X, Direction.NORTH), 2, 7, 0, 14, 9, 2, "north (x270)");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.CENTERED_X, Direction.SOUTH), 2, 7, 14, 14, 9, 16, "south (x90)");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.CENTERED_X, Direction.WEST), 7, 0, 2, 9, 2, 14, "west (y270)");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.CENTERED_X, Direction.EAST), 7, 0, 2, 9, 2, 14, "east (y90)");
        // the arm composition: the t22 envelope (2,0,4,14,2,12) on the 90 arm = the y90
        // rotation = the true 112.5-degree envelope (4,0,2,12,2,14); the t45 envelope is
        // X/Z-symmetric so its arm twin keeps the box; the slides ride the same maps
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.TILT_112, Direction.DOWN), 4, 0, 2, 12, 2, 14, "tilt envelope on the 90 arm");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.TILT_135, Direction.DOWN), 3, 0, 3, 13, 2, 13, "tilt45 envelope on the 90 arm");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.SLIDE_X, Direction.NORTH), 2, 9, 0, 14, 11, 2, "slide x270");
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Stick.SLIDE_Z, Direction.EAST), 2, 0, 5, 14, 2, 7, "slide z on east (y180)");
        // the rock wall follow: the mid tier (5,0,5,11,2,11) x270 against the north wall
        assertBox(GT6SurfaceVariants.shapeOf(GT6SurfaceVariants.Rock.MID, Direction.NORTH), 5, 5, 0, 11, 11, 2, "rock mid tier north wall");
    }

    /**
     * The tilt envelopes re-derived from trigonometry (independent of the enum literals):
     * the 12x2 bar rotated about (8,8) in XZ has half-extents 6c+1s / 6s+1c, rounded
     * OUTWARD so the box always conservatively covers the tilted quads.
     */
    @Test
    void tiltEnvelopesAreTheOutwardRoundedRotations() {
        for (double tAngle : new double[] {22.5, 45.0}) {
            double tRad = Math.toRadians(tAngle);
            double tHx = 6 * Math.cos(tRad) + 1 * Math.sin(tRad);
            double tHz = 6 * Math.sin(tRad) + 1 * Math.cos(tRad);
            int[] tExpected = {(int) Math.floor(8 - tHx), 0, (int) Math.floor(8 - tHz),
                    (int) Math.ceil(8 + tHx), 2, (int) Math.ceil(8 + tHz)};
            int[] tActual = (tAngle == 22.5 ? GT6SurfaceVariants.Stick.TILT_22 : GT6SurfaceVariants.Stick.TILT_45).box();
            assertEquals(java.util.Arrays.toString(tExpected), java.util.Arrays.toString(tActual),
                    "tilt envelope at " + tAngle + " degrees");
        }
    }

    /** pixel ints -> the normalised 0..1 toAabbs space (px/16). */
    private static void assertBox(net.minecraft.world.phys.shapes.VoxelShape aShape,
            int aX1, int aY1, int aZ1, int aX2, int aY2, int aZ2, String aMessage) {
        List<AABB> tBoxes = aShape.toAabbs();
        assertEquals(1, tBoxes.size(), aMessage + ": one box");
        AABB tExpected = new AABB(aX1 / 16.0, aY1 / 16.0, aZ1 / 16.0, aX2 / 16.0, aY2 / 16.0, aZ2 / 16.0);
        assertTrue(Math.abs(tBoxes.get(0).minX - tExpected.minX) < 1e-9 && Math.abs(tBoxes.get(0).minY - tExpected.minY) < 1e-9
                && Math.abs(tBoxes.get(0).minZ - tExpected.minZ) < 1e-9 && Math.abs(tBoxes.get(0).maxX - tExpected.maxX) < 1e-9
                && Math.abs(tBoxes.get(0).maxY - tExpected.maxY) < 1e-9 && Math.abs(tBoxes.get(0).maxZ - tExpected.maxZ) < 1e-9,
                aMessage + ": expected " + tExpected + " got " + tBoxes.get(0));
    }
}
