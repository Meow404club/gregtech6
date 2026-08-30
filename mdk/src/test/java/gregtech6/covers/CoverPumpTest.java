package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.Direction;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.tank.BarrelFluidHandler;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The pump-cover acceptance tables (task p5-barrel-side-rules acceptance ①/③/⑥), all
 * offline: the placement seam, the one-way gate, the screwdriver direction toggle, the
 * visual persistence gate and the gate-order over the barrel wrapper (the cover gate
 * answers BEFORE the side rule — the reachable offline proof of spec A's ordering). The
 * live per-second transfer itself is the RCON chain's job (a real level and a real
 * neighbour are out of the offline doubles' reach).
 *
 * <p>Pump host re-judgment (task p6-barrel-metal-plastic ①, the offline mirror of the
 * RCON chain re-record): the wood barrel restored the upstream decorative-only
 * {@code allowCover} (MultiTileEntityBarrelWood.java:39), so the pump mounts the metal
 * drum here — the upstream MultiTileEntityBarrelMetal takes the base default and admits
 * every cover. The wood/plastic refusals live in GTBarrelFamilyTest's predicate table.
 */
public class CoverPumpTest extends GTCoverTestBase {

	static BlockEntityType<GTBarrelMetalBlockEntity> sBarrelType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildBarrelFixture() {
		// the base @BeforeAll already bootstrapped; build the metal drum BET over the vanilla fixture block
		SharedConstants.tryDetectVersion();
		BlockEntityType<GTBarrelMetalBlockEntity>[] tHolder = (BlockEntityType<GTBarrelMetalBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelMetalBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sBarrelType = tHolder[0];
	}

	static GTBarrelMetalBlockEntity barrel() {
		return sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
	}

	/** Mounts a fresh pump cover on the face and returns the drum + its store. */
	static GTBarrelMetalBlockEntity pumpBarrel(byte aSide) {
		GTBarrelMetalBlockEntity tBarrel = barrel();
		CoverRegistry.put(Items.BRICK, new CoverPump());
		assertTrue(tBarrel.setCoverItem(aSide, new ItemStack(Items.BRICK), null, false, true), "the pump mounts on the metal drum (the mTank seam)");
		return tBarrel;
	}

	// ---------------------------------------------------------------------------
	// acceptance ③ — the placement seam: no pump tank, no pump
	// ---------------------------------------------------------------------------

	@Test
	public void pumpPlacementTracksTheHostTankSeam() {
		CoverPump tPump = new CoverPump();
		TileEntityOvenCoverProbe tOven = bareOven();
		CoverData tOvenData = CoverRegistry.coverdata(tOven, null);
		assertTrue(tPump.interceptCoverPlacement((byte) 5, tOvenData, null),
				"the oven exposes no pump tank (ICoverableTE.getCoverPumpTank default null) — the pump refuses");

		GTBarrelMetalBlockEntity tBarrel = barrel();
		CoverData tBarrelData = CoverRegistry.coverdata(tBarrel, null);
		assertFalse(tPump.interceptCoverPlacement((byte) 5, tBarrelData, null),
				"the metal drum hands out mTank — the pump mounts (p6 re-judgment: the drum admits the pump, wood/plastic refuse functional covers)");
	}

	@Test
	public void barrelOverridesThePumpSeamWithItsTankAndTheOvenStaysNull() {
		GTBarrelMetalBlockEntity tBarrel = barrel();
		assertSame(tBarrel.mTank, tBarrel.getCoverPumpTank(), "the direct-call seam is the drum's own tank");
		TileEntityOven tOven = bareOven();
		assertNull(tOven.getCoverPumpTank(), "hosts without a pump tank stay null");
	}

	// ---------------------------------------------------------------------------
	// acceptance ③ — the one-way gate table (upstream :92-93 verbatim)
	// ---------------------------------------------------------------------------

	@Test
	public void pumpOneWayGateTable() {
		GTBarrelMetalBlockEntity tBarrel = pumpBarrel((byte) 5); // east face, visual 0 = out
		CoverData tData = tBarrel.getCovers();
		assertNotNull(tData);
		CoverPump tPump = (CoverPump) tData.mBehaviours[5];

		// out-face (visual 0): the fill is refused, the drain passes the COVER gate
		assertTrue(tPump.interceptFluidFill((byte) 5, tData, (byte) 5, new FluidStack(Fluids.WATER, 100)), "the out-face refuses incoming fluid");
		assertFalse(tPump.interceptFluidDrain((byte) 5, tData, (byte) 5, new FluidStack(Fluids.WATER, 100)), "the out-face lets its own drain through the gate");

		// other faces are untouched by this cover
		assertFalse(tPump.interceptFluidFill((byte) 5, tData, (byte) 2, new FluidStack(Fluids.WATER, 100)), "the gate only binds its own face");
		assertFalse(tPump.interceptFluidDrain((byte) 5, tData, (byte) 2, new FluidStack(Fluids.WATER, 100)));

		// in-face (visual 1): the drain is refused, the fill passes the COVER gate
		tData.visual((byte) 5, (short) 1);
		assertFalse(tPump.interceptFluidFill((byte) 5, tData, (byte) 5, new FluidStack(Fluids.WATER, 100)), "the in-face takes fluid in");
		assertTrue(tPump.interceptFluidDrain((byte) 5, tData, (byte) 5, new FluidStack(Fluids.WATER, 100)), "the in-face refuses outgoing fluid");
	}

	@Test
	public void screwdriverTogglesTheDirectionAndDamagesTheTool() {
		GTBarrelMetalBlockEntity tBarrel = pumpBarrel((byte) 5);
		CoverData tData = tBarrel.getCovers();
		CoverPump tPump = (CoverPump) tData.mBehaviours[5];

		assertEquals(1000, tPump.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F),
				"the upstream screwdriver damage");
		assertEquals(1, tData.mVisuals[5], "out (0) → in (1)");

		assertEquals(1000, tPump.onToolClick((byte) 5, tData, ICover.TOOL_SCREWDRIVER, 900, null, false, (byte) 5, 0.5F, 0.5F, 0.5F));
		assertEquals(0, tData.mVisuals[5], "in (1) → out (0) — the toggle flips both ways");

		assertEquals(0, tPump.onToolClick((byte) 5, tData, ICover.TOOL_CROWBAR, 1000, null, false, (byte) 5, 0.5F, 0.5F, 0.5F),
				"the crowbar is not the pump's tool");
		assertEquals(0, tData.mVisuals[5], "an unknown tool leaves the direction alone");
	}

	@Test
	public void pumpBeatPhaseTable() {
		assertTrue(CoverPump.isPumpBeat(5), "the upstream :68 phase — tick 5 of each second");
		assertTrue(CoverPump.isPumpBeat(25), "the phase repeats every 20");
		assertFalse(CoverPump.isPumpBeat(4));
		assertFalse(CoverPump.isPumpBeat(6));
		assertFalse(CoverPump.isPumpBeat(0), "the pumps are deliberately off the zero tick");
	}

	// ---------------------------------------------------------------------------
	// acceptance ⑥ — the visual round-trip gated by needsVisualsSaved (upstream :90/:75)
	// ---------------------------------------------------------------------------

	@Test
	public void pumpDirectionRoundTripsThroughTheCoversNbt() {
		GTBarrelMetalBlockEntity tBarrel = pumpBarrel((byte) 5);
		CoverData tData = tBarrel.getCovers();
		tData.visual((byte) 5, (short) 1); // the screwdriver flipped it to IN

		CompoundTag tSaved = new CompoundTag();
		tBarrel.writeCoversToNBT(tSaved);
		CompoundTag tCoversTag = tSaved.getCompound(ICoverableTE.NBT_COVERS);
		assertEquals(1, tCoversTag.getShort(CoverData.VISUAL_KEYS[5]),
				"needsVisualsSaved=true keeps the direction in the save even in the sync-lane write (CoverData :87)");

		// the sync-lane form (aIncludeVisuals=false) is exactly where the gate bites —
		// a decorative cover (needsVisualsSaved=false) loses the visual there
		CompoundTag tPlain = new CoverData(tBarrel).set((byte) 5, new ItemStack(Items.BRICKS)).visual((byte) 5, (short) 1)
				.writeToNBT(new CompoundTag(), false);
		assertFalse(tPlain.contains(CoverData.VISUAL_KEYS[5]), "CoverTextureSimple's needsVisualsSaved=false drops the visual in the sync lane");

		CompoundTag tPumped = new CoverData(tBarrel).set((byte) 5, new ItemStack(Items.BRICK)).visual((byte) 5, (short) 1)
				.writeToNBT(new CompoundTag(), false);
		assertTrue(tPumped.contains(CoverData.VISUAL_KEYS[5]), "the pump keeps it (the gate is needsVisualsSaved)");

		// rehydration: a fresh store over the saved compound restores the behaviour + the lane
		GTBarrelMetalBlockEntity tBack = barrel();
		CoverData tBackData = CoverRegistry.coverdata(tBack, tCoversTag);
		assertNotNull(tBackData.mBehaviours[5], "the pump re-resolves from the id lane");
		assertEquals(1, tBackData.mVisuals[5], "the direction survives the round-trip");
		assertTrue(tBackData.mBehaviours[5].needsVisualsSaved((byte) 5, tBackData));
	}

	// ---------------------------------------------------------------------------
	// spec A ordering — the cover gate answers the wrapper BEFORE the side rule
	// ---------------------------------------------------------------------------

	@Test
	public void wrapperDrainHitsTheCoverGateBeforeTheSideRule() {
		// pump on the BOTTOM face, in-mode: the bottom face normally drains the heavier —
		// but the in-face drain gate refuses first, so the wrapper returns EMPTY without
		// ever reaching the density verdict (which is live-registry territory offline).
		GTBarrelMetalBlockEntity tBarrel = pumpBarrel((byte) 0);
		tBarrel.getCovers().visual((byte) 0, (short) 1);
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);

		FluidStack tDrawn = new BarrelFluidHandler(tBarrel, Direction.DOWN).drain(100, FluidAction.EXECUTE);
		assertTrue(tDrawn.isEmpty(), "the in-face pump gate refuses the drain before the side rule is consulted");
		assertEquals(500, tBarrel.mTank.amount(), "nothing left the tank");
	}

	@Test
	public void wrapperFillHonoursTheOutFaceGate() {
		// pump on the EAST face, out-mode: fill has no side rule, so the gate is the refuser
		GTBarrelMetalBlockEntity tBarrel = pumpBarrel((byte) 5);
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		FluidTankGT tTank = tBarrel.mTank;

		assertEquals(0, new BarrelFluidHandler(tBarrel, Direction.EAST).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"the out-face refuses the fill through the wrapper");
		assertEquals(500, tTank.amount());

		// the uncovered face still fills
		assertEquals(100, new BarrelFluidHandler(tBarrel, Direction.UP).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"fill stays face-open on the uncovered faces");
		assertEquals(600, tTank.amount());
	}

	@Test
	public void pumpTickWithoutALevelIsAHarmlessNoOp() {
		GTBarrelMetalBlockEntity tBarrel = pumpBarrel((byte) 5);
		tBarrel.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		// no level → no adjacent handler → the beat fires and moves nothing (offline guard)
		tBarrel.getCovers().tickPre(5, true, false, false);
		assertEquals(500, tBarrel.mTank.amount(), "the pump beat without a neighbour moves nothing");
	}

	// ---------------------------------------------------------------------------
	// the barrel-side gravity move pair lives with the side rules — the shared budget constant
	// ---------------------------------------------------------------------------

	@Test
	public void gravityBudgetIsTheCardPinnedThousand() {
		assertEquals(1000, TileEntityBase08Barrel.GRAVITY_TRANSFER_PER_TICK, "ruling ② — the passive discharge budget");
	}
}
