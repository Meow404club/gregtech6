package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * Acceptance ①/③ (task p4-fluid-barrel): the 16000 L capacity, the sticky
 * preventDraining NBT round trip, the voidExcess-off behaviour and the melt-down
 * judgment, all offline. CompoundTag and BlockEntityType.Builder.of(...).build(null)
 * construct without a registry (GTOfflineTestBase); the vanilla water/lava FluidStacks
 * resolve against the boot-populated fluid registry. The offline vanilla fixture block
 * keeps the barrel at the MAX_VALUE melting default; the :66 NBT_CAPACITY_HU read seam
 * sets the wood 340 K ceiling in the melting tests (gt6:iron_molten needs the live
 * registry and is exercised by the /gt6tank melt command instead).
 */
public class TileEntityBase08BarrelTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(2, 3, 4);
	static final long WOOD_MELTING_POINT = 340; // upstream NBT_CAPACITY_HU row, Loader_MultiTileEntities.java:2136

	static BlockEntityType<GTBarrelBlockEntity> sType;
	static BlockEntityType<StickyBarrelBlockEntity> sStickyType;

	/** The MultiTileEntityBarrelLogistics keepsFilter()=T precedent (Logistics.java:40) as the sticky test mount. */
	static final class StickyBarrelBlockEntity extends TileEntityBase08Barrel {

		StickyBarrelBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(true, aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "sticky_barrel";
		}

		@Override
		public boolean keepsFilter() {
			return true; // the stickiness the base wiring setPreventDraining(keepsFilter()) (:69) picks up
		}
	}

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildOfflineFixtures() {
		BlockEntityType<GTBarrelBlockEntity>[] tHolder = (BlockEntityType<GTBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sType = tHolder[0];

		BlockEntityType<StickyBarrelBlockEntity>[] tStickyHolder = (BlockEntityType<StickyBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tStickyHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new StickyBarrelBlockEntity(tStickyHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sStickyType = tStickyHolder[0];
	}

	@Test
	public void capacityIs16000AndOverflowIsRejected() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(16000, tBe.mTank.capacity(), "the class default FluidTankGT(16000) (upstream :53, card acceptance ①)");

		assertEquals(16000, tBe.mTank.fill(new FluidStack(Fluids.WATER, 16000), FluidAction.EXECUTE));
		assertTrue(tBe.mTank.isFull());

		// voidExcess is off (upstream barrels never void): the next fill is refused, nothing changes
		assertEquals(0, tBe.mTank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE));
		assertEquals(16000, tBe.mTank.amount());
	}

	@Test
	public void stickyBarrelRoundTripsContentAndKeepsIdentityWhenEmptied() {
		StickyBarrelBlockEntity tBe = sStickyType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);

		CompoundTag tSaved = tBe.saveWithoutMetadata();
		assertTrue(tSaved.contains(TileEntityBase08Barrel.NBT_TANK, Tag.TAG_COMPOUND), "the tank rides NBT_TANK (:77)");

		StickyBarrelBlockEntity tBack = sStickyType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(100, tBack.mTank.amount(), "the content round-trips through load→readFromNBT (:69)");

		// the stickiness: draining to 0 L keeps the fluid identity (mPreventDraining, upstream :69)
		tBack.mTank.drain(100, FluidAction.EXECUTE);
		assertEquals(0, tBack.mTank.amount());
		assertTrue(tBack.mTank.contains(new FluidStack(Fluids.WATER, 1)), "preventDraining keeps the fluid identity at 0 L");
		assertTrue(tBack.mTank.getFluid() != null, "the identity stack stays reachable");

		// the empty-but-identity state still writes the NBT_TANK key under preventDraining (writeToNBT :85),
		// but its payload degrades: W1 FluidTankGT.writeToNBT rebinds the live stack to 0 L in place and a
		// 0-amount 1.20.1 FluidStack is empty-flagged (FluidStack.updateEmpty :173), so the written
		// FluidName serializes as "minecraft:empty". Known FluidTankGT gap (out of this card's files scope)
		// — the keepFilter/logistics barrel that needs 0 L persistence carries the fix with its port card;
		// the wood barrel (keepsFilter=F) never reaches the 0-amount-identity state (drains to null).
		CompoundTag tEmptied = tBack.saveWithoutMetadata();
		assertTrue(tEmptied.contains(TileEntityBase08Barrel.NBT_TANK, Tag.TAG_COMPOUND), "the key stays under preventDraining (:85)");
	}

	@Test
	public void nonStickyBaseBarrelDrainsToTrueEmpty() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		tBe.mTank.drain(100, FluidAction.EXECUTE);

		// wood does not override keepsFilter() upstream — the barrel drains to a null content
		assertNull(tBe.mTank.getFluid());

		// and the saved NBT drops the tank key entirely (writeToNBT :91 remove branch)
		CompoundTag tSaved = tBe.saveWithoutMetadata();
		assertFalse(tSaved.contains(TileEntityBase08Barrel.NBT_TANK, Tag.TAG_COMPOUND));
	}

	@Test
	public void tankContentRoundTripsThroughTheBarrelNbtPair() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);

		CompoundTag tSaved = tBe.saveWithoutMetadata();
		assertEquals("barrel_wood", tSaved.getString("te_name"), "the 01Root te_name key carries the BET path");

		GTBarrelBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(1234, tBack.mTank.amount());
		assertTrue(tBack.mTank.contains(new FluidStack(Fluids.WATER, 1)));
	}

	@Test
	public void capacityHuNbtSeamOverridesCeilingAndCapacity() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(Long.MAX_VALUE, tBe.mMeltingPoint, "no material bridge: the bare barrel never melts (upstream :55)");

		CompoundTag tTag = new CompoundTag();
		tTag.putLong(TileEntityBase08Barrel.NBT_CAPACITY_HU, WOOD_MELTING_POINT);
		tTag.putLong(TileEntityBase08Barrel.NBT_TANK_CAPACITY, 32000);
		tBe.load(tTag);

		assertEquals(WOOD_MELTING_POINT, tBe.mMeltingPoint, "the :66 NBT_CAPACITY_HU read seam");
		assertEquals(32000, tBe.mTank.capacity(), "the :69 NBT_TANK_CAPACITY read seam");
	}

	@Test
	public void meltingJudgmentWaterSafeMoltenIronAndLavaMelt() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		CompoundTag tTag = new CompoundTag();
		tTag.putLong(TileEntityBase08Barrel.NBT_CAPACITY_HU, WOOD_MELTING_POINT);
		tBe.load(tTag);

		// the raw :162 comparison (meltsDown(long)) — the FluidType temperature lookup itself
		// needs the live registry and is exercised by /gt6tank (water 300 K, iron 1811 K live-side)
		assertFalse(tBe.meltsDown(300), "vanilla water sits at 300 K — safe against the 340 K wood ceiling");
		assertTrue(tBe.meltsDown(1300), "vanilla lava sits at 1300 K");
		assertTrue(tBe.meltsDown(1811), "gt6:iron_molten is the card's verification fluid at 1811 K");
		assertTrue(tBe.meltsDown(WOOD_MELTING_POINT), "the >= boundary melts at exactly the ceiling");
	}

	@Test
	public void defaultCeilingNeverMeltsEvenMoltenIron() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(Long.MAX_VALUE, tBe.mMeltingPoint, "no material bridge: the bare barrel never melts (upstream :55)");
		assertFalse(tBe.meltsDown(1811), "MAX_VALUE ceiling — nothing melts without the registration NBT");
	}

	@Test
	public void meltdownVoidsTheTank() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mTank.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
		assertTrue(TileEntityBase08Barrel.isLava(tBe.mTank.getFluid()));

		// offline (no level): the world effects of meltdown() are level-guarded, the tank trash always runs
		assertTrue(tBe.meltdown());
		assertEquals(0, tBe.mTank.amount(), "meltdown() voids the tank (the GarbageGT.trash half, :223/:226)");
		assertNull(tBe.mTank.getFluid());
	}
}
