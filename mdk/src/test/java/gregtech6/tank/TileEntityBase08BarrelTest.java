package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.item.GTBarrelBlockItem;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.GTBarrelLogisticsBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;
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
	static BlockEntityType<GTBarrelMetalBlockEntity> sMetalType;
	static BlockEntityType<GTBarrelLogisticsBlockEntity> sLogisticsType;

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

		BlockEntityType<GTBarrelMetalBlockEntity>[] tMetal = (BlockEntityType<GTBarrelMetalBlockEntity>[]) new BlockEntityType<?>[1];
		tMetal[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelMetalBlockEntity(tMetal[0], aPos, aState),
				Blocks.STONE).build(null);
		sMetalType = tMetal[0];

		// the real logistics BE (task p12-barrel-keepfilter-logistics) over the same vanilla
		// fixture shape — the BE ctor takes the offline BET, the GTBarrels constants stay untouched
		BlockEntityType<GTBarrelLogisticsBlockEntity>[] tLogistics = (BlockEntityType<GTBarrelLogisticsBlockEntity>[]) new BlockEntityType<?>[1];
		tLogistics[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelLogisticsBlockEntity(tLogistics[0], aPos, aState),
				Blocks.STONE).build(null);
		sLogisticsType = tLogistics[0];
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

		// the empty-but-identity state writes the NBT_TANK key under preventDraining (writeToNBT
		// :85) AND carries the real identity since task p12-barrel-keepfilter-logistics: the
		// W1-era payload degraded to "minecraft:empty" (the 1.20.1 empty flag collapsed
		// FluidStack.writeToNBT) — the logistics card's FluidTankGT serialization fix now writes
		// the real FluidName at Amount 0, asserted over the real BE below
		// (logisticsBarrelIdentitySurvivesTheZeroAmountRoundTrip). The W1 assertion here (the key
		// stays under preventDraining) is unchanged and still passes.
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

	// ---------------------------------------------------------------------------
	// task p12-fluid-item-carrier — the BE↔item round-trip seam: getDrops writes the
	// tank onto the drop item (upstream 03:157-162 → Base08:81-85), placement reads it
	// back (GTBarrelBlockItem.applyItemNBT); both halves over the offline BE fixtures
	// (a mod Block cannot be constructed — the getDrops/placeBlock wrappers themselves
	// are the live /gt6tank show assertions)
	// ---------------------------------------------------------------------------

	/** The break half: a filled barrel's drop item carries the tank NBT; the place half reads the same keys back into a fresh BE. */
	@Test
	public void dropItemNbtCarriesTankAndRestoresOnPlacement() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBe.mTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);

		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		//? if forge {
		assertTrue(tDrop.hasTag(), "the filled drop carries a tag");
		CompoundTag tTankTag = tDrop.getTag().getCompound(TileEntityBase08Barrel.NBT_TANK);
		//?} else {
		/*assertTrue(tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "the filled drop carries its payload component");
		CompoundTag tTankTag = tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT).copyTag().getCompound(TileEntityBase08Barrel.NBT_TANK);
		*///?}
		assertEquals("minecraft:water", tTankTag.getString("FluidName"), "the drop item NBT names the fluid");
		assertEquals(1234, tTankTag.getInt("Amount"), "the drop item NBT carries the amount");

		// the place half: the same tag loads into a fresh BE — the round trip closes
		GTBarrelBlockEntity tPlaced = sType.create(POS, Blocks.STONE.defaultBlockState());
		GTBarrelBlockItem.applyItemNBT(tDrop, tPlaced);
		assertEquals(1234, tPlaced.mTank.amount(), "placement restores the content (the fix's other half)");
		assertTrue(tPlaced.mTank.contains(new FluidStack(Fluids.WATER, 1)), "placement restores the identity");
	}

	/** An empty, cover-less barrel drops byte-identical to the pre-card behaviour: no tag at all. */
	@Test
	public void emptyBarrelDropKeepsTheLegacyShape() {
		GTBarrelBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		ItemStack tDrop = GTBarrelBlock.writeItemNBT(tBe, new ItemStack(Items.GLASS_BOTTLE));
		//? if forge {
		assertFalse(tDrop.hasTag(), "no content, no covers → the tagless pre-card drop (acceptance a)");
		//?} else {
		/*assertFalse(tDrop.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "no content, no covers → the payload-less pre-card drop (acceptance a)");
		*///?}
		assertFalse(GTBarrelBlockItem.hasContent(tDrop), "the :290 stacking predicate reads empty");
	}

	/** The :290 {@code mTank.has() ? 1 : aDefault} predicate over the item tag (the override itself is live-side). */
	@Test
	public void hasContentPredicateDrivesTheStackingRule() {
		ItemStack tEmpty = new ItemStack(Items.GLASS_BOTTLE);
		assertFalse(GTBarrelBlockItem.hasContent(tEmpty), "no tag → no content");

		ItemStack tFilled = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tFilled.getOrCreateTag().put(TileEntityBase08Barrel.NBT_TANK, new FluidStack(Fluids.WATER, 1).writeToNBT(new CompoundTag()));
		//?} else {
		/*CompoundTag tPre = new CompoundTag();
		tPre.put(TileEntityBase08Barrel.NBT_TANK, new FluidStack(Fluids.WATER, 1).save(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, new CompoundTag())); // 21.1: the codec save face
		net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tFilled, tPre); // 21.1: the payload rides the CustomData component
		*///?}
		assertTrue(GTBarrelBlockItem.hasContent(tFilled), "a non-empty tank compound = content → max stack 1");

		ItemStack tBlank = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tBlank.getOrCreateTag().put(TileEntityBase08Barrel.NBT_TANK, new CompoundTag());
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tBlank, new CompoundTag()); // 21.1: a present-but-empty payload compound
		*///?}
		assertFalse(GTBarrelBlockItem.hasContent(tBlank), "a present-but-empty compound is not content");
	}

	// ---------------------------------------------------------------------------
	// task p12-barrel-keepfilter-logistics — the real BE replaces the sticky test
	// mount as the keepsFilter()=T consumer (the mount above stays as the base-class
	// regression). The StickyBarrelBlockEntity mount (:49) is retained per the card
	// spec ③; the logistics BE asserts the same stickiness over the real class.
	// ---------------------------------------------------------------------------

	/** The upstream :39-40 override pair on the real BE — keepsFilter T (the only one) + canBeSealed F; the metal control arm stays F/null. */
	@Test
	public void logisticsBarrelKeepsFilterAndRefusesSealing() {
		GTBarrelLogisticsBlockEntity tBe = sLogisticsType.create(POS, Blocks.STONE.defaultBlockState());
		assertTrue(tBe.keepsFilter(), "the MultiTileEntityBarrelLogistics override (Logistics.java:40) — the port's only T");
		assertFalse(tBe.canBeSealed(), "the :39 transcription — the logistics tank refuses the seal");
		assertEquals("barrel_logistics", tBe.getTileEntityName(), "the BET path mirror (GTBarrels.BARREL_LOGISTICS_BE)");

		// the control arm: the metal drum takes the base default (drains to null, seal face cut port-wide)
		GTBarrelMetalBlockEntity tMetal = sMetalType.create(POS, Blocks.STONE.defaultBlockState());
		assertFalse(tMetal.keepsFilter(), "metal takes the base :284 default — keepsFilter=F (zero semantic drift)");
	}

	/**
	 * The keepFilter acceptance (card acceptance a): fill → drain to 0 → the saved NBT
	 * carries the REAL identity at Amount 0 (the writeToNBT :85 judgement + the 1.20.1
	 * empty-flag fix) → a fresh BE loads it back with the identity intact → refilling the
	 * same fluid succeeds. The drain only sticks after a load pass — the :132
	 * {@code setPreventDraining(keepsFilter())} wiring is the load-time re-arm.
	 */
	@Test
	public void logisticsBarrelIdentitySurvivesTheZeroAmountRoundTrip() {
		// seed: fill 100, save, reload — the load pass arms preventDraining (:132)
		GTBarrelLogisticsBlockEntity tBe = sLogisticsType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(100, tBe.mTank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE));
		GTBarrelLogisticsBlockEntity tArmed = sLogisticsType.create(POS, Blocks.STONE.defaultBlockState());
		tArmed.load(tBe.saveWithoutMetadata());

		// drain to 0: the identity stays in the tank (mPreventDraining, upstream :121-126)
		tArmed.mTank.drain(100, FluidAction.EXECUTE);
		assertEquals(0, tArmed.mTank.amount());
		assertTrue(tArmed.mTank.contains(new FluidStack(Fluids.WATER, 1)), "the identity survives the drain to 0 in memory");

		// the write half: the saved NBT carries the REAL fluid name at Amount 0 — the
		// writeToNBT fix (pre-card the payload degraded to "minecraft:empty", the W1 known gap)
		CompoundTag tZero = tArmed.saveWithoutMetadata();
		assertTrue(tZero.contains(TileEntityBase08Barrel.NBT_TANK, Tag.TAG_COMPOUND), "the :85 judgement keeps the key at 0 L");
		CompoundTag tTankTag = tZero.getCompound(TileEntityBase08Barrel.NBT_TANK);
		assertEquals("minecraft:water", tTankTag.getString("FluidName"), "writeToNBT 0 量含身份 — the real FluidName, not minecraft:empty");
		assertEquals(0, tTankTag.getInt("Amount"), "Amount 0 — the 0-amount state itself persists");

		// the read half: a fresh BE loads the 0-amount identity back (loadFluidStackFromNBT
		// keeps the raw fluid on the empty-flagged stack) and refilling the same fluid succeeds
		GTBarrelLogisticsBlockEntity tBack = sLogisticsType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tZero);
		assertEquals(0, tBack.mTank.amount(), "the loaded tank is at 0 L");
		assertTrue(tBack.mTank.contains(new FluidStack(Fluids.WATER, 1)), "load→身份在 — the identity rides the round trip");
		assertEquals(100, tBack.mTank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE),
				"重灌同流体成功 — the kept filter accepts its own fluid again");

		// and the re-drained tank saves the same shape again (the :132 re-arm on the load pass above)
		tBack.mTank.drain(100, FluidAction.EXECUTE);
		CompoundTag tAgain = tBack.saveWithoutMetadata();
		assertEquals("minecraft:water", tAgain.getCompound(TileEntityBase08Barrel.NBT_TANK).getString("FluidName"),
				"the zero-amount identity persists on every subsequent save");
	}

	/** The control arm (card acceptance a): the metal drum at drain-0 is a TRUE empty — no identity, no tank key (base semantics, zero drift). */
	@Test
	public void metalControlBarrelDrainsToTrueEmptyWithoutIdentity() {
		GTBarrelMetalBlockEntity tBe = sMetalType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(100, tBe.mTank.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE));
		GTBarrelMetalBlockEntity tArmed = sMetalType.create(POS, Blocks.STONE.defaultBlockState());
		tArmed.load(tBe.saveWithoutMetadata()); // the :132 wiring runs with keepsFilter=F — nothing to arm

		tArmed.mTank.drain(100, FluidAction.EXECUTE);
		assertEquals(0, tArmed.mTank.amount());
		assertNull(tArmed.mTank.getFluid(), "drain 0 身份清 — the base barrels drain to a null content (upstream :124)");
		CompoundTag tSaved = tArmed.saveWithoutMetadata();
		assertFalse(tSaved.contains(TileEntityBase08Barrel.NBT_TANK, Tag.TAG_COMPOUND),
				"the writeToNBT :91 remove branch — no key, no identity (the keepFilter contrast)");
	}
}
