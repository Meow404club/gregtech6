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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.covers.covers.AbstractCoverAttachmentTorch;
import gregtech6.covers.covers.CoverFilterFluid;
import gregtech6.covers.covers.CoverRedstoneRepeater;
import gregtech6.covers.covers.CoverRedstoneTorch;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;

/**
 * The fluid-filter and torch-family offline acceptance (task p34-covers-gameplay-10):
 * the filter gates (the whitelist/blacklist polarity over a set and an empty filter,
 * the screwdriver flip, the soft-hammer clear) and the torch-family emission truth
 * (the torch inverts the wire, the repeater follows it, the full-15 emission rides the
 * visual lane, placement disconnects the wire face).
 */
public class CoverFilterFluidTorchTest extends GTCoverTestBase {

	static final BlockPos WIRE_POS = new BlockPos(6, 6, 6);

	static BlockEntityType<GTBarrelMetalBlockEntity> sBarrelType;
	static BlockEntityType<WireCoverProbe> sWireType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildFilterTorchFixtures() {
		// the base @BeforeAll already bootstrapped; the trio-test barrel form + the wire carrier
		SharedConstants.tryDetectVersion();
		BlockEntityType<GTBarrelMetalBlockEntity>[] tHolder = (BlockEntityType<GTBarrelMetalBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelMetalBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sBarrelType = tHolder[0];
		BlockEntityType<WireCoverProbe>[] tWireHolder = (BlockEntityType<WireCoverProbe>[]) new BlockEntityType<?>[1];
		tWireHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new WireCoverProbe(tWireHolder[0]),
				Blocks.BRICKS).build(null);
		sWireType = tWireHolder[0];
	}

	// ------------------------------------------------------------------
	// the fluid filter (upstream CoverFilterFluid)
	// ------------------------------------------------------------------

	/** A fresh cover data with the filter set to the given fluid (the pure lane write). */
	static CoverData filterData(ICoverableTE aHost, net.minecraft.world.level.material.Fluid aFluid) {
		CoverData tCovers = new CoverData(aHost);
		tCovers.mNBTs[3] = CoverFilterFluid.filterTagFor(aFluid);
		tCovers.mBehaviours[3] = new CoverFilterFluid();
		return tCovers;
	}

	@Test
	public void fluidFilterTagRoundTripsTheRegistryName() {
		GTBarrelMetalBlockEntity tBarrel = sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
		CoverData tCovers = filterData(tBarrel, Fluids.WATER);
		assertTrue(CoverFilterFluid.matches(tCovers, (byte) 3, new FluidStack(Fluids.WATER, 100)),
				"the same fluid matches (FL.equal NBT-insensitive)");
		assertFalse(CoverFilterFluid.matches(tCovers, (byte) 3, new FluidStack(Fluids.LAVA, 100)),
				"another fluid does not match");
		assertFalse(CoverFilterFluid.matches(tCovers, (byte) 3, null), "the null stack never matches");
	}

	@Test
	public void fluidFilterWhitelistGatesBothDirections() {
		GTBarrelMetalBlockEntity tBarrel = sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
		CoverData tCovers = filterData(tBarrel, Fluids.WATER);
		CoverFilterFluid tFilter = new CoverFilterFluid();
		// visual 0 = whitelist: the filter fluid passes, everything else refuses (upstream :121/:128)
		assertFalse(tFilter.interceptFluidFill((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.WATER, 100)), "the filter fluid fills");
		assertTrue(tFilter.interceptFluidFill((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.LAVA, 100)), "a foreign fill refuses");
		assertFalse(tFilter.interceptFluidDrain((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.WATER, 100)), "the filter fluid drains");
		assertTrue(tFilter.interceptFluidDrain((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.LAVA, 100)), "a foreign drain refuses");
	}

	@Test
	public void fluidFilterEmptyFilterPolarityMatchesTheUpstreamTable() {
		GTBarrelMetalBlockEntity tBarrel = sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
		CoverData tCovers = new CoverData(tBarrel);
		tCovers.mBehaviours[3] = new CoverFilterFluid();
		CoverFilterFluid tFilter = new CoverFilterFluid();
		// empty filter + whitelist (visual 0): refuse everything (:120)
		assertTrue(tFilter.interceptFluidFill((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.WATER, 100)));
		// empty filter + blacklist (visual 1): admit everything (:120)
		tCovers.mVisuals[3] = 1;
		assertFalse(tFilter.interceptFluidFill((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.WATER, 100)));
		assertFalse(tFilter.interceptFluidDrain((byte) 3, tCovers, (byte) 3, new FluidStack(Fluids.LAVA, 100)));
	}

	@Test
	public void fluidFilterOtherFacesAreNotGated() {
		GTBarrelMetalBlockEntity tBarrel = sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
		CoverData tCovers = filterData(tBarrel, Fluids.WATER);
		CoverFilterFluid tFilter = new CoverFilterFluid();
		assertFalse(tFilter.interceptFluidFill((byte) 3, tCovers, (byte) 2, new FluidStack(Fluids.LAVA, 100)),
				"a fill requested on another face is not gated (:118)");
	}

	@Test
	public void fluidFilterScrewdriverFlipsAndSofthammerClears() {
		GTBarrelMetalBlockEntity tBarrel = sBarrelType.create(COVER_POS, Blocks.STONE.defaultBlockState());
		CoverData tCovers = filterData(tBarrel, Fluids.WATER);
		CoverFilterFluid tFilter = new CoverFilterFluid();
		assertEquals(1000, tFilter.onToolClick((byte) 3, tCovers, ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 3, 0, 0, 0), "the screwdriver damage 1000 (:65)");
		assertEquals(1, tCovers.mVisuals[3], "visual 0 -> 1 (whitelist -> blacklist, :63)");
		assertEquals(10000, tFilter.onToolClick((byte) 3, tCovers, CoverFilterFluid.TOOL_SOFTHAMMER, 0, null, false, (byte) 3, 0, 0, 0), "the soft hammer damage 10000 (:69)");
		assertFalse(tCovers.mNBTs[3].contains(CoverFilterFluid.FILTER_KEY), "the filter lane is cleared (:68)");
	}

	// ------------------------------------------------------------------
	// the torch family (upstream CoverRedstoneTorch / CoverRedstoneRepeater)
	// ------------------------------------------------------------------

	/**
	 * The wire carrier plus the cover store — the composition the declared
	 * host-composition card will land on the production carrier; the offline pins drive
	 * it directly (the torch behaviors cast the host to the wire carrier).
	 */
	static class WireCoverProbe extends GTWireBlockEntity implements ICoverableTE {

		/** The cover store (the composition contract field). */
		@javax.annotation.Nullable
		public CoverData mCovers;

		WireCoverProbe(BlockEntityType<WireCoverProbe> aType) {
			super(aType, WIRE_POS, Blocks.BRICKS.defaultBlockState());
			// the self-typed BET via the holder trick (the pump-test barrel form)
		}

		/** Seeds a raw connection bit (SBIT[side] = 1 << side, the base :33 form) — the protected-mask test seam. */
		void seedConnection(byte aSide) {
			mConnections |= (byte) (1 << aSide);
		}

		@Override
		@javax.annotation.Nullable
		public CoverData getCovers() {
			return mCovers;
		}

		@Override
		public void setCovers(@javax.annotation.Nullable CoverData aCoverData) {
			mCovers = aCoverData;
		}
	}


	static WireCoverProbe wire() {
		return sWireType.create(WIRE_POS, Blocks.BRICKS.defaultBlockState());
	}

	/** A wire store with the given torch-family cover on face 3. */
	static CoverData torchData(GTWireBlockEntity aWire, AbstractCoverAttachmentTorch aTorch) {
		CoverData tCovers = new CoverData((ICoverableTE) aWire);
		tCovers.mBehaviours[3] = aTorch;
		return tCovers;
	}

	@Test
	public void torchRefusesNonWireHosts() {
		TileEntityOvenCoverProbe tOven = bareOven();
		tOven.setCovers(new CoverData(tOven));
		assertTrue(new CoverRedstoneTorch().interceptCoverPlacement((byte) 3, tOven.getCovers(), null),
				"the torch only mounts on the wire carrier (upstream :35)");
		assertFalse(new CoverRedstoneTorch().interceptCoverPlacement((byte) 3, torchData(wire(), new CoverRedstoneTorch()), null),
				"the wire carrier admits the torch");
	}

	@Test
	public void torchInvertsTheWireSignal() {
		GTWireBlockEntity tWire = wire();
		CoverRedstoneTorch tTorch = new CoverRedstoneTorch();
		CoverData tCovers = torchData(tWire, tTorch);
		tWire.mRedstone = 0; // the dead wire
		tCovers.tickPost(10, true, false, false);
		assertEquals(0, tCovers.mVisuals[3], "dead wire -> the torch art ON (visual 0, upstream :60-68)");
		assertEquals(15, tTorch.getRedstoneOutStrong((byte) 3, tCovers, (byte) 0), "the ON torch emits the full 15 (:50-53)");
		assertEquals(15, tTorch.getRedstoneOutWeak((byte) 3, tCovers, (byte) 0), "weak == strong (:55-57)");
		tWire.mRedstone = 12345; // a powered wire (any non-zero, the > 0 literal)
		tCovers.tickPost(11, true, false, false);
		assertEquals(1, tCovers.mVisuals[3], "powered wire -> the torch art OFF (visual 1)");
		assertEquals(0, tTorch.getRedstoneOutStrong((byte) 3, tCovers, (byte) 0), "the OFF torch emits nothing");
	}

	@Test
	public void repeaterFollowsTheWireSignal() {
		GTWireBlockEntity tWire = wire();
		CoverRedstoneRepeater tRepeater = new CoverRedstoneRepeater();
		CoverData tCovers = torchData(tWire, tRepeater);
		tWire.mRedstone = 999; // the powered wire
		tCovers.tickPost(10, true, false, false);
		assertEquals(0, tCovers.mVisuals[3], "powered wire -> the repeater ON (visual 0)");
		assertEquals(15, tRepeater.getRedstoneOutStrong((byte) 3, tCovers, (byte) 0), "the ON repeater emits the full 15");
		tWire.mRedstone = 0;
		tCovers.tickPost(11, true, false, false);
		assertEquals(1, tCovers.mVisuals[3], "dead wire -> the repeater OFF");
		assertEquals(0, tRepeater.getRedstoneOutWeak((byte) 3, tCovers, (byte) 0), "the OFF repeater emits nothing");
	}

	@Test
	public void torchPlacementDisconnectsTheWireFace() {
		WireCoverProbe tWire = wire();
		CoverRedstoneTorch tTorch = new CoverRedstoneTorch();
		// seed a connection bit on the torch face directly (the protected-mask test seam)
		tWire.seedConnection((byte) 3);
		assertTrue(tWire.connected((byte) 3), "the fixture wire carries the bit");
		tTorch.onCoverPlaced((byte) 3, torchData(tWire, tTorch), null, ItemStack.EMPTY);
		assertFalse(tWire.connected((byte) 3), "placement disconnects the torch face (upstream :39-42)");
	}
}
