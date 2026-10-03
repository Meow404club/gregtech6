/**
 * The wrench-targeting pins (task wrench-pipe-fullcube-targeting — research.r11-cover-wrench
 * Q2): under a WRENCH-KEY context the pipe outline shape is the FULL cube, the thin
 * connection envelope without one, and the collision never changes either way.
 *
 * <p>The bug: {@code getShape} served the thin diameter envelope as BOTH the outline and
 * the interaction target, so the raytrace starved in the empty space at the block-cell
 * edges — the nine-cell grid (GTWrenchHighlightListener) never showed and
 * {@code use()} never fired where the user actually aimed ("小型管道移到边缘时九宫格不显示
 * 没法连"). Upstream 1.7.10 judged the pipe as a full block when wrenching (the default
 * {@code shrunkBox} = PX_BOX full cube, TileEntityBase01Root.java:834 + CS.java:489, the
 * pipes carry no shrunkBox override; the thin box lives on collision only,
 * TileEntityBase11ConnectorStraight.java:60). The fix rides the context arm of the shared
 * wrench key ({@code GT6ToolActions.isWrenchInteractionContext} — the GTCEu PipeBlock
 * :419-436 precedent form), so the outline is full-cube exactly when the grid/overlay
 * predicate is true ("shown means clickable" holds; listener/use untouched).
 *
 * <p>The three pins:
 * <ol>
 * <li>wrench-key context (either hand, the listener's own breadth) → {@code Shapes.block()};
 *     per-connection masks included — the arm replaces the envelope wholesale;</li>
 * <li>no wrench key (empty context / empty hands / foreign stack) → the thin envelope
 *     UNCHANGED (the GTRodShapeTest diameter pins keep their values; the declared
 *     deviation: the no-wrench outline stays thin where upstream showed the full cube
 *     unconditionally — the context gate is the reviewed minimal-blast-radius form);</li>
 * <li>collision NEVER rides the arm: {@code getCollisionShape} stays the thin envelope
 *     even under the wrench context (the vanilla route keeps it inert —
 *     BlockBehaviour.getCollisionShape :290-292 answers through the two-arg
 *     {@code state.getShape} = empty context, and the foam seam wraps that same result).</li>
 * </ol>
 * The WRENCH leg needs a registered probe item (the WrenchInteractionKeyTest.probeItem
 * machinery); here it rides {@code registerItemFixture} — the environmental ItemLatch
 * lottery may ASSUME-SKIP it, while the vanilla-hoe leg (the HOE_DIG substitute, upstream
 * 1.7.10 fidelity) pins the context path unconditionally.
 */
package gregtech6.block.pipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.items.tools.GT6ToolActions;
import gregtech6.items.tools.GTWrenchItem;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;

public class PipeWrenchShapeTest extends gregtech6.tileentity.GTOfflineTestBase {

    // the blocks construct LAZILY (after the base @BeforeAll bootstrap) — the
    // GTRodShapeTest unfreeze form
    private static void unfreeze() {
        try {
            java.lang.reflect.Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
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

    // ------------------------------------------------------------------
    // the offline doubles: a Player whose hands answer directly, and the
    // EntityCollisionContext carrying it
    // ------------------------------------------------------------------

    /**
     * The offline Player double (the GT6AnvilBlockEntityTest.BagPlayer Unsafe-allocation
     * form) — the hands are overridden instead of an injected Inventory, because the
     * context seam reads {@code getMainHandItem}/{@code getOffhandItem} directly.
     */
    public static final class HeldPlayer extends Player {
        private HeldPlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form

        private ItemStack mMain = ItemStack.EMPTY;
        private ItemStack mOff = ItemStack.EMPTY;

        @Override public ItemStack getMainHandItem() { return this.mMain; }
        @Override public ItemStack getOffhandItem() { return this.mOff; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }

    private static HeldPlayer heldPlayer(ItemStack aMain, ItemStack aOff) {
        try {
            Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            tUnsafeField.setAccessible(true);
            sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
            HeldPlayer rPlayer = (HeldPlayer) tUnsafe.allocateInstance(HeldPlayer.class);
            rPlayer.mMain = aMain;
            rPlayer.mOff = aOff;
            return rPlayer;
        } catch (ReflectiveOperationException aE) {
            throw new IllegalStateException("the offline held player failed", aE);
        }
    }

    /** The context under test — the five-arg protected ctor through a local subclass (identical signature on both legs; skips the deprecated one-arg's live-entity derefs an Unsafe double cannot answer). */
    private static CollisionContext context(Player aPlayer) {
        class HeldContext extends EntityCollisionContext {
            HeldContext(Entity aEntity) { super(false, 0.0, ItemStack.EMPTY, f -> false, aEntity); }
        }
        return new HeldContext(aPlayer);
    }

    /** A fake BlockGetter with no block entity (the CoverRedstoneConductorTest fixture form) — enough for the fluid pipe's foam-collision lookup. */
    private static BlockGetter emptyLevel() {
        return new BlockGetter() {
            @Override public BlockState getBlockState(BlockPos aPos) { return null; }
            @Override public FluidState getFluidState(BlockPos aPos) { return Fluids.EMPTY.defaultFluidState(); }
            @Override public BlockEntity getBlockEntity(BlockPos aPos) { return null; }
            @Override public int getHeight() { return 384; }
            @Override public int getMinBuildHeight() { return -64; }
            @Override public int getMaxBuildHeight() { return 320; }
        };
    }

    private static void assertBox(VoxelShape aShape, double aMinX, double aMinY, double aMinZ,
            double aMaxX, double aMaxY, double aMaxZ, String aLabel) {
        assertEquals(1, aShape.toAabbs().size(), aLabel + ": the shape is one box");
        AABB tBox = aShape.toAabbs().get(0);
        assertEquals(aMinX, tBox.minX, 1e-9, aLabel + ": minX");
        assertEquals(aMinY, tBox.minY, 1e-9, aLabel + ": minY");
        assertEquals(aMinZ, tBox.minZ, 1e-9, aLabel + ": minZ");
        assertEquals(aMaxX, tBox.maxX, 1e-9, aLabel + ": maxX");
        assertEquals(aMaxY, tBox.maxY, 1e-9, aLabel + ": maxY");
        assertEquals(aMaxZ, tBox.maxZ, 1e-9, aLabel + ": maxZ");
    }

    // ------------------------------------------------------------------
    // pin 1 — the wrench-key context targets the FULL cube
    // ------------------------------------------------------------------

    @Test
    public void wrenchContextOutlineIsTheFullCube() {
        // wood small (PX_P[6]) — the hoe leg, main hand: the aim-to-the-edge case fixed
        BlockState tState = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0);
        assertBox(tState.getShape(null, null, context(heldPlayer(new ItemStack(Items.WOODEN_HOE), ItemStack.EMPTY))),
                0, 0, 0, 1, 1, 1, "fluid pipe, hoe main hand");
        // either hand — the grid overlay (GTWrenchHighlightListener) triggers on both, so
        // the outline must not discriminate or "shown" would exceed "clickable" surface
        assertBox(tState.getShape(null, null, context(heldPlayer(ItemStack.EMPTY, new ItemStack(Items.WOODEN_HOE)))),
                0, 0, 0, 1, 1, 1, "fluid pipe, hoe off hand");
        // per-connection masks included: the arm replaces the envelope wholesale
        BlockState tSpan = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 48);
        assertBox(tSpan.getShape(null, null, context(heldPlayer(new ItemStack(Items.WOODEN_HOE), ItemStack.EMPTY))),
                0, 0, 0, 1, 1, 1, "fluid pipe W|E span, hoe main hand");
        // the item pipe family rides the same arm (brass restrictive medium, PX_P[8])
        BlockState tItem = itemPipe("brass_item_pipe_restrictive_medium").defaultBlockState().setValue(GTItemPipeBlock.CONNECTIONS, 0);
        assertBox(tItem.getShape(null, null, context(heldPlayer(new ItemStack(Items.WOODEN_HOE), ItemStack.EMPTY))),
                0, 0, 0, 1, 1, 1, "item pipe, hoe main hand");
        // the WRENCH leg through a registered probe (ItemLatch lottery — may skip; LAST so
        // a skip never masks the unconditional assertions above)
        GTWrenchItem tWrench = registerItemFixture("wrench_shape_probe_wrench", () -> new GTWrenchItem(new net.minecraft.world.item.Item.Properties().durability(512)));
        assertBox(tState.getShape(null, null, context(heldPlayer(new ItemStack(tWrench), ItemStack.EMPTY))),
                0, 0, 0, 1, 1, 1, "fluid pipe, formal wrench");
    }

    // ------------------------------------------------------------------
    // pin 2 — without the key, the thin envelope is UNCHANGED
    // ------------------------------------------------------------------

    @Test
    public void noWrenchContextKeepsTheThinEnvelope() {
        // the empty context (entity-less raytraces, collision, misc shape reads)
        BlockState tState = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0);
        assertBox(tState.getShape(null, null, CollisionContext.empty()),
                0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875, "fluid pipe, empty context");
        // a player with nothing/foreign in hand stays on the thin envelope (the declared
        // deviation face: upstream showed the full cube unconditionally, this port gates it)
        assertBox(tState.getShape(null, null, context(heldPlayer(ItemStack.EMPTY, ItemStack.EMPTY))),
                0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875, "fluid pipe, empty hands");
        assertBox(tState.getShape(null, null, context(heldPlayer(new ItemStack(Items.DIRT), ItemStack.EMPTY))),
                0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875, "fluid pipe, foreign stack");
        BlockState tItem = itemPipe("brass_item_pipe_restrictive_medium").defaultBlockState().setValue(GTItemPipeBlock.CONNECTIONS, 0);
        assertBox(tItem.getShape(null, null, CollisionContext.empty()),
                0.25, 0.25, 0.25, 0.75, 0.75, 0.75, "item pipe, empty context");
    }

    // ------------------------------------------------------------------
    // pin 3 — collision never rides the arm
    // ------------------------------------------------------------------

    @Test
    public void collisionStaysTheThinEnvelopeUnderTheWrenchContext() {
        BlockPos tPos = BlockPos.ZERO;
        // fluid: the foam-collision override wraps the super route, which answers through
        // the two-arg getShape (empty context) — the full cube must NOT leak into physics
        BlockState tState = fluidPipe("wood_fluid_pipe_small").defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0);
        assertBox(tState.getCollisionShape(emptyLevel(), tPos, context(heldPlayer(new ItemStack(Items.WOODEN_HOE), ItemStack.EMPTY))),
                0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875, "fluid pipe collision, hoe in hand");
        // item: the BlockBehaviour default routes the same two-arg way (no override at all)
        BlockState tItem = itemPipe("brass_item_pipe_restrictive_medium").defaultBlockState().setValue(GTItemPipeBlock.CONNECTIONS, 0);
        assertBox(tItem.getCollisionShape(emptyLevel(), tPos, context(heldPlayer(new ItemStack(Items.WOODEN_HOE), ItemStack.EMPTY))),
                0.25, 0.25, 0.25, 0.75, 0.75, 0.75, "item pipe collision, hoe in hand");
    }

    // ------------------------------------------------------------------
    // the seam itself composes the stack key (one assertion set, no second truth
    // table — the stack-level pins live in WrenchInteractionKeyTest)
    // ------------------------------------------------------------------

    @Test
    public void contextSeamComposesTheStackKey() {
        assertFalse(GT6ToolActions.isWrenchInteractionContext(CollisionContext.empty()), "no entity, no key");
        assertFalse(GT6ToolActions.isWrenchInteractionContext(context(heldPlayer(ItemStack.EMPTY, ItemStack.EMPTY))), "bare hands, no key");
        assertTrue(GT6ToolActions.isWrenchInteractionContext(context(heldPlayer(new ItemStack(Items.WOODEN_HOE), ItemStack.EMPTY))), "the hoe key shows");
        assertFalse(GT6ToolActions.isWrenchInteractionContext(context(heldPlayer(new ItemStack(Items.STICK), ItemStack.EMPTY))), "foreign stacks stay out");
    }
}
