package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.covers.covers.CoverPressureValve;
import gregtech6.covers.covers.CoverRedstoneTorch;
import gregtech6.covers.covers.CoverSelectorTag;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The real-host mount acceptance (task p34-pool-cover-hosts): the production carrier
 * classes themselves implement the cover seams now — {@link GTFluidPipeBlockEntity}
 * (the PressureValve host) and {@link GTWireBlockEntity} (the Torch + selector host and
 * the FIRST {@link gregtech6.tileentity.machines.ITileEntitySwitchableMode} live host).
 * The covers-card probes (WireCoverProbe/PipeCoverProbe/TileEntityModeDialProbe) stay as
 * the composition-store pins; THIS file mounts the same covers through the REAL dispatch
 * ({@link ICoverableTE#setCoverItem}) on the REAL carriers, pins the covers NBT round
 * trip through the carriers' own save/load, and drives the new wire-side emission route
 * ({@code GTWireBlockEntity.getRedstoneOut} consulting the cover exits).
 */
public class CoverHostMountTest extends GTCoverTestBase {

    static final BlockPos HOST_POS = new BlockPos(7, 7, 7);

    static BlockEntityType<GTFluidPipeBlockEntity> sPipeType;
    static BlockEntityType<GTWireBlockEntity> sWireType;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void buildHostFixtures() {
        // the base @BeforeAll already bootstrapped; the trio-test holder trick for the
        // self-typed BETs (the (pos,state) factory ignores its arguments — the carriers
        // are block-carrier-driven, the offline carrier is the vanilla BRICKS fixture)
        SharedConstants.tryDetectVersion();
        BlockEntityType<GTFluidPipeBlockEntity>[] tPipeHolder = (BlockEntityType<GTFluidPipeBlockEntity>[]) new BlockEntityType<?>[1];
        tPipeHolder[0] = BlockEntityType.Builder.of(
                (aPos, aState) -> new GTFluidPipeBlockEntity(tPipeHolder[0], HOST_POS, Blocks.BRICKS.defaultBlockState()),
                Blocks.BRICKS).build(null);
        sPipeType = tPipeHolder[0];
        BlockEntityType<GTWireBlockEntity>[] tWireHolder = (BlockEntityType<GTWireBlockEntity>[]) new BlockEntityType<?>[1];
        tWireHolder[0] = BlockEntityType.Builder.of(
                (aPos, aState) -> new GTWireBlockEntity(tWireHolder[0], HOST_POS, Blocks.BRICKS.defaultBlockState()),
                Blocks.BRICKS).build(null);
        sWireType = tWireHolder[0];
    }

    /**
     * The machine stub level with the world queries quieted — the vanilla
     * {@code Level.getBlockEntity} needs a chunk source the offline stub does not own
     * (the GTWireGlowLightTest protected-seam posture on the test side): the emission
     * route's neighbour probe reads {@code null} and the connected-gate does the rest.
     */
    static class QuietLevel extends MachineLevel {
        QuietLevel() {
            super(new TestRecipeManager());
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos aPos) {
            return null;
        }
    }

    /** A REAL fluid pipe host on the quiet stub level (the drumWith form). */
    static GTFluidPipeBlockEntity pipe() {
        GTFluidPipeBlockEntity tPipe = sPipeType.create(HOST_POS, Blocks.BRICKS.defaultBlockState());
        tPipe.setLevel(new QuietLevel());
        return tPipe;
    }

    /** A REAL wire carrier host on the quiet stub level (the drumWith form). */
    static GTWireBlockEntity wire() {
        GTWireBlockEntity tWire = sWireType.create(HOST_POS, Blocks.BRICKS.defaultBlockState());
        tWire.setLevel(new QuietLevel());
        return tWire;
    }

    /**
     * The redstone-family wire carrier — the offline seam for the family flag: the
     * production flag resolves from the GTWireBlock blockstate, which the vanilla-BRICKS
     * fixture cannot carry; the torch gate reads the public {@code isRedstone()} exit,
     * so the probe pins the family directly (the p35 cover-side narrowing).
     */
    static class RedstoneFamilyWire extends GTWireBlockEntity {
        RedstoneFamilyWire() {
            super(sWireType, HOST_POS, Blocks.BRICKS.defaultBlockState());
        }
        @Override
        public boolean isRedstone() {
            return true;
        }
    }

    /** The redstone-family wire carrier on the quiet stub level (the torch's legal host). */
    static GTWireBlockEntity redstoneWire() {
        GTWireBlockEntity tWire = new RedstoneFamilyWire();
        tWire.setLevel(new QuietLevel());
        return tWire;
    }

    // ------------------------------------------------------------------
    // the pipe host (upstream CoverPressureValve :44/:49-64)
    // ------------------------------------------------------------------

    @Test
    public void pressureValveMountsOnTheRealPipeThroughTheRealDispatch() {
        CoverPressureValve tValve = new CoverPressureValve();
        CoverRegistry.put(Items.BRICK, tValve);
        GTFluidPipeBlockEntity tPipe = pipe();
        assertFalse(tValve.interceptCoverPlacement((byte) 3, new CoverData(tPipe), null),
                "the REAL single-tank pipe carrier admits the valve (upstream :44)");
        assertTrue(tPipe.setCoverItem((byte) 3, new ItemStack(Items.BRICK), null, false, true),
                "the setCoverItem dispatch mounts the valve on the real pipe");
        assertTrue(tPipe.isCovered((byte) 3), "the store holds the behaviour");
        assertTrue(tPipe.hasCovers(), "the composition store is alive");
    }

    @Test
    public void pressureValveTickPostRunsOnTheRealPipeAndCoversNbtRoundTrips() {
        CoverPressureValve tValve = new CoverPressureValve();
        CoverRegistry.put(Items.BRICK, tValve);
        GTFluidPipeBlockEntity tPipe = pipe();
        assertTrue(tPipe.setCoverItem((byte) 3, new ItemStack(Items.BRICK), null, false, true));
        tPipe.mTanks[0].fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        tPipe.getCovers().tickPost(10, true, false, false);
        assertFalse(tPipe.connected((byte) 3), "the full-tank tickPost ran on the real pipe: the valve face is an endpoint (:52)");
        assertEquals(1000, tPipe.mTanks[0].amount(), "a liquid backlog with no handler keeps its tank (the trio-table arm)");
        // the NBT round trip through the carrier's own saveAdditional/load
        CompoundTag tTag = tPipe.saveWithoutMetadata();
        GTFluidPipeBlockEntity tClone = sPipeType.create(HOST_POS, Blocks.BRICKS.defaultBlockState());
        tClone.load(tTag);
        assertTrue(tClone.isCovered((byte) 3), "the covers NBT rides the real pipe save/load (06Covers :68/:74)");
    }

    // ------------------------------------------------------------------
    // the wire host — the torch (upstream AbstractCoverAttachmentTorch + the
    // 04Covers :427-438 emission override the host now routes). The torch gate is the
    // REDSTONE family of the carrier (p35 narrowing — upstream keys the insulated
    // redstone wire class), so the torch tests run on the redstone-family carrier.
    // ------------------------------------------------------------------

    @Test
    public void torchMountsOnTheRealWireAndDrivesTheEmissionExit() {
        CoverRedstoneTorch tTorch = new CoverRedstoneTorch();
        CoverRegistry.put(Items.BRICK, tTorch);
        GTWireBlockEntity tWire = redstoneWire();
        assertFalse(tTorch.interceptCoverPlacement((byte) 1, new CoverData(tWire), null),
                "the REAL redstone-family wire carrier admits the torch (the family gate)");
        assertTrue(tWire.setCoverItem((byte) 1, new ItemStack(Items.BRICK), null, false, true));
        // the dead wire: the torch art flips ON and the HOST exit answers the full 15
        tWire.getCovers().tickPost(10, true, false, false);
        assertEquals(0, tWire.getCovers().mVisuals[1], "dead wire -> the torch art ON (visual 0)");
        assertEquals(15, tWire.getRedstoneOut((byte) 0, true), "the ON torch answers the wire's own strong exit (query from the DOWN receiver, emission face UP)");
        assertEquals(15, tWire.getRedstoneOut((byte) 0, false), "weak == strong on the torch family (:55-57)");
        // the powered wire: the art flips OFF and the exit falls to zero — the inversion
        tWire.mRedstone = 12345;
        tWire.getCovers().tickPost(11, true, false, false);
        assertEquals(1, tWire.getCovers().mVisuals[1], "powered wire -> the torch art OFF (visual 1)");
        assertEquals(0, tWire.getRedstoneOut((byte) 0, true), "the OFF torch emits nothing through the host exit");
    }

    @Test
    public void torchFaceSelectivityOnTheRealWire() {
        CoverRedstoneTorch tTorch = new CoverRedstoneTorch();
        CoverRegistry.put(Items.BRICK, tTorch);
        GTWireBlockEntity tWire = redstoneWire();
        assertTrue(tWire.setCoverItem((byte) 1, new ItemStack(Items.BRICK), null, false, true));
        tWire.getCovers().tickPost(10, true, false, false); // the dead wire: art ON
        assertEquals(15, tWire.getRedstoneOut((byte) 0, true), "the covered face answers the exit (query DOWN -> emission face UP)");
        assertEquals(0, tWire.getRedstoneOut((byte) 1, true), "a query on another face hits the bare face — the torch does not leak (the bare-face pass-through)");
    }

    @Test
    public void torchRefusesTheNonRedstoneFamilyRows() {
        CoverRedstoneTorch tTorch = new CoverRedstoneTorch();
        CoverRegistry.put(Items.BRICK, tTorch);
        // the BRICKS fixture wire resolves to the ELECTRIC family — the p35 narrowing:
        // the cover-side gate refuses it exactly as the upstream electric wire classes did
        GTWireBlockEntity tWire = wire();
        assertTrue(tTorch.interceptCoverPlacement((byte) 1, new CoverData(tWire), null),
                "the electric-family wire row REFUSES the torch (the cover-side narrowing)");
        assertFalse(tWire.setCoverItem((byte) 1, new ItemStack(Items.BRICK), null, false, true),
                "the real dispatch refuses the mount");
        assertFalse(tWire.hasCovers(), "the refused mount left no store behind");
    }

    @Test
    public void bareWireEmissionExitIsUnchanged() {
        GTWireBlockEntity tWire = wire();
        tWire.mRedstone = 0;
        assertEquals(0, tWire.getRedstoneOut((byte) 0, true), "a cover-less wire keeps its own emission exit verbatim (zero diff without covers)");
        assertTrue(tWire.getCovers() == null, "no store was created by the bare query");
    }

    // ------------------------------------------------------------------
    // the wire host — the selector dial (the FIRST ITileEntitySwitchableMode
    // live host; the covers-card negative flips positive here)
    // ------------------------------------------------------------------

    @Test
    public void selectorMountsOnTheRealWireAndDrivesTheModeDial() {
        CoverSelectorTag tTag = new CoverSelectorTag((byte) 5);
        CoverRegistry.put(Items.BRICK, tTag);
        GTWireBlockEntity tWire = wire();
        assertFalse(tTag.interceptCoverPlacement((byte) 2, new CoverData(tWire), null),
                "THE FLIPPED NEGATIVE: the real wire IS a switchable-mode host — the selector mounts");
        assertTrue(tWire.setCoverItem((byte) 2, new ItemStack(Items.BRICK), null, false, true));
        assertEquals(5, tWire.mMode, "placement asserted the constructor mode (upstream CoverSelectorTag :44-47)");
        assertEquals(9, tWire.setStateMode((byte) 9), "the dial write returns the POST-switch state (upstream :177 passthrough)");
        assertEquals(9, tWire.getStateMode(), "the dial reads back (upstream :178)");
        assertEquals(9, tWire.mMode);
    }

    @Test
    public void wireCoversNbtRoundTripsThroughTheCarrier() {
        CoverSelectorTag tTag = new CoverSelectorTag((byte) 3);
        CoverRegistry.put(Items.BRICK, tTag);
        GTWireBlockEntity tWire = wire();
        assertTrue(tWire.setCoverItem((byte) 2, new ItemStack(Items.BRICK), null, false, true));
        CompoundTag tTag2 = tWire.saveWithoutMetadata();
        GTWireBlockEntity tClone = sWireType.create(HOST_POS, Blocks.BRICKS.defaultBlockState());
        tClone.load(tTag2);
        assertTrue(tClone.isCovered((byte) 2), "the covers NBT rides the real wire save/load (06Covers :68/:74)");
    }
}
