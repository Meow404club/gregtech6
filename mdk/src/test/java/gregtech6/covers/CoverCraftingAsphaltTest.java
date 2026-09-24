package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

import gregtech6.block.GTOvenBlock;
import gregtech6.covers.covers.CoverAsphalt;
import gregtech6.covers.covers.CoverCrafting;

/**
 * The last-two-classes behavior pins (task p37-covers-crafting-asphalt acceptance ②):
 *
 * <ul>
 * <li>the asphalt walk boost — the upstream CoverAsphalt :39 gate table (boost ×1.3 only
 *     while moving, dry and not sneaking; the event always consumed :40) over the walk
 *     entity double;</li>
 * <li>the host walk chain — the GTOvenBlock.stepOn representative (condition ③: one host
 *     representative of the 9-block uniform dispatch) routes the vanilla walk entry into
 *     {@link ICoverableTE#onCoverWalkOver} and only the TOP-face cover answers (the
 *     upstream TileEntityBase06Covers :428 shape); the other 8 host blocks share the
 *     identical one-line shape (the review diff check);</li>
 * <li>the crafting cover — the click intercept (:46-55 returns true; the ServerPlayer.openMenu
 *     arm is the declared offline limit, the GT6CokeOvenMenuTest posture: menu construction
 *     with live MenuTypes/Inventory/Player is MC-bootstrap territory) plus the :57/:58
 *     flag pair and the plate sprites.</li>
 * </ul>
 */
public class CoverCraftingAsphaltTest extends GTCoverTestBase {

	/** The TOP face — the only face the walk dispatch answers (upstream :428). */
	private static final byte TOP = (byte) net.minecraft.core.Direction.UP.get3DDataValue();
	private static final byte SIDE = (byte) net.minecraft.core.Direction.NORTH.get3DDataValue();

	/**
	 * The entity doubles bypass the constructor: the Forge-patched {@code Entity.<init>}
	 * resolves the fluid-type registry (FluidType.java:71) which never boots offline — the
	 * declared-limit sibling of the GT6CokeOvenMenuTest posture. The walk path under test
	 * touches only the delta-movement/shift fields, which the allocator's defaults satisfy.
	 */
	private static <T> T entityWithoutCtor(Class<T> aClass) {
		try {
			java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tTheUnsafe.setAccessible(true);
			sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
			return aClass.cast(tUnsafe.allocateInstance(aClass));
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("offline entity allocator failed", aE);
		}
	}

	/** The walk entity double — a bare LivingEntity (the upstream EntityLivingBase gate) with a settable water flag. */
	private static final class WalkEntity extends net.minecraft.world.entity.LivingEntity {
		boolean mInWater;
		boolean mShift;

		WalkEntity() {
			super(EntityType.ZOMBIE, null);
		}

		@Override public boolean isInWater() { return mInWater; }
		@Override public boolean isShiftKeyDown() { return mShift; }
		@Override public void setShiftKeyDown(boolean aShift) { mShift = aShift; }
//? if forge {
		@Override protected void defineSynchedData() {/**/}
//?} else {
		/*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder aBuilder) {}
*///?}
		@Override public void readAdditionalSaveData(CompoundTag aTag) {/**/}
		@Override public void addAdditionalSaveData(CompoundTag aTag) {/**/}
		@Override public Iterable<ItemStack> getArmorSlots() { return java.util.List.of(); }
		@Override public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot aSlot) { return ItemStack.EMPTY; }
		@Override public void setItemSlot(net.minecraft.world.entity.EquipmentSlot aSlot, ItemStack aStack) {/**/}
		@Override public net.minecraft.world.entity.HumanoidArm getMainArm() { return net.minecraft.world.entity.HumanoidArm.RIGHT; }
	}

	// the doubles hand the allocator the concrete type (the ctor never runs)
	private static WalkEntity walkEntity() { return entityWithoutCtor(WalkEntity.class); }
	private static ItemEntityDouble itemEntity() { return entityWithoutCtor(ItemEntityDouble.class); }

	/** The level stub with the probe reachable — the stepOn chain reads it through getBlockEntity. */
	private static final class WalkLevel extends MachineLevel {
		final TileEntityOvenCoverProbe mProbe;

		WalkLevel(TileEntityOvenCoverProbe aProbe) {
			super(new TestRecipeManager());
			mProbe = aProbe;
		}

		@Override public BlockEntity getBlockEntity(BlockPos aPos) {
			return COVER_POS.equals(aPos) ? mProbe : null;
		}
	}

	/** Mounts a cover on the probe at the given face (the emitter-test install form). */
	private static TileEntityOvenCoverProbe ovenWith(ICover aCover, byte aSide) {
		TileEntityOvenCoverProbe tOven = bareOven();
		CoverRegistry.put(Items.BRICK, aCover);
		assertTrue(tOven.setCoverItem(aSide, new ItemStack(Items.BRICK), null, true, false), "install accepted");
		return tOven;
	}

	// ---------------------------------------------------------------------------
	// the asphalt walk boost (upstream CoverAsphalt :38-41)
	// ---------------------------------------------------------------------------

	@Test
	void walkBoostAppliesOnlyToMovingDryAwakeEntities() {
		CoverAsphalt tCover = new CoverAsphalt();

		WalkEntity tWalker = walkEntity();
		tWalker.setDeltaMovement(1.0, 0.0, 0.0);
		assertTrue(tCover.onWalkOver(TOP, null, tWalker), "the walk event is consumed");
		assertEquals(new Vec3(CoverAsphalt.WALK_BOOST, 0.0, 0.0), tWalker.getDeltaMovement(), "the horizontal x1.3 boost");

		WalkEntity tDiagonal = walkEntity();
		tDiagonal.setDeltaMovement(2.0, 1.0, 4.0);
		tCover.onWalkOver(TOP, null, tDiagonal);
		assertEquals(new Vec3(2.6, 1.0, 5.2), tDiagonal.getDeltaMovement(), "x/z boost, y untouched");
	}

	@Test
	void walkBoostGatesStationaryWaterAndSneaking() {
		CoverAsphalt tCover = new CoverAsphalt();

		WalkEntity tStationary = walkEntity();
		tStationary.setDeltaMovement(Vec3.ZERO); // the allocator skips the field initializer
		assertTrue(tCover.onWalkOver(TOP, null, tStationary));
		assertEquals(Vec3.ZERO, tStationary.getDeltaMovement(), "no motion, no boost");

		WalkEntity tSneaking = walkEntity();
		tSneaking.setDeltaMovement(1.0, 0.0, 0.0);
		tSneaking.mShift = true;
		tCover.onWalkOver(TOP, null, tSneaking);
		assertEquals(new Vec3(1.0, 0.0, 0.0), tSneaking.getDeltaMovement(), "sneaking never boosts");

		WalkEntity tSwimming = walkEntity();
		tSwimming.mInWater = true;
		tSwimming.setDeltaMovement(1.0, 0.0, 0.0);
		tCover.onWalkOver(TOP, null, tSwimming);
		assertEquals(new Vec3(1.0, 0.0, 0.0), tSwimming.getDeltaMovement(), "in-water never boosts");
	}

	// ---------------------------------------------------------------------------
	// the host walk chain (the 06Covers :428 dispatch through the stepOn entry)
	// ---------------------------------------------------------------------------

	@Test
	void hostWalkChainBoostsOnlyThroughTheTopFaceCover() {
		// the top-face asphalt cover: the stepOn entry reaches it and boosts
		TileEntityOvenCoverProbe tOven = ovenWith(new CoverAsphalt(), TOP);
		GTOvenBlock tBlock = entityWithoutCtor(GTOvenBlock.class); // the frozen-registry ctor cut — stepOn touches no fields
		WalkEntity tWalker = walkEntity();
		tWalker.setDeltaMovement(1.0, 0.0, 0.0);
		tBlock.stepOn(new WalkLevel(tOven), COVER_POS, Blocks.BRICKS.defaultBlockState(), tWalker);
		assertEquals(new Vec3(CoverAsphalt.WALK_BOOST, 0.0, 0.0), tWalker.getDeltaMovement(), "the vanilla walk entry boosts through the top cover");

		// the same asphalt cover on a SIDE face: the walk dispatch does not answer it
		TileEntityOvenCoverProbe tSideOven = ovenWith(new CoverAsphalt(), SIDE);
		WalkEntity tSideWalker = walkEntity();
		tSideWalker.setDeltaMovement(1.0, 0.0, 0.0);
		tBlock.stepOn(new WalkLevel(tSideOven), COVER_POS, Blocks.BRICKS.defaultBlockState(), tSideWalker);
		assertEquals(new Vec3(1.0, 0.0, 0.0), tSideWalker.getDeltaMovement(), "only SIDE_TOP answers the walk dispatch");

		// the cover-free host: the dispatch is a no-op (the !hasCovers gate)
		TileEntityOvenCoverProbe tBare = bareOven();
		WalkEntity tBareWalker = walkEntity();
		tBareWalker.setDeltaMovement(1.0, 0.0, 0.0);
		tBlock.stepOn(new WalkLevel(tBare), COVER_POS, Blocks.BRICKS.defaultBlockState(), tBareWalker);
		assertEquals(new Vec3(1.0, 0.0, 0.0), tBareWalker.getDeltaMovement(), "the cover-free host is a walk no-op");

		// a non-living entity never reaches the cover (the upstream EntityLivingBase gate)
		TileEntityOvenCoverProbe tItemHost = ovenWith(new CoverAsphalt(), TOP);
		WalkLevel tItemLevel = new WalkLevel(tItemHost);
		ItemEntityDouble tItem = itemEntity();
		tItem.setDeltaMovement(1.0, 0.0, 0.0);
		tBlock.stepOn(tItemLevel, COVER_POS, Blocks.BRICKS.defaultBlockState(), tItem);
		assertEquals(new Vec3(1.0, 0.0, 0.0), tItem.getDeltaMovement(), "non-living entities do not trigger covers");
	}

	/** The non-living entity double (Entity, not LivingEntity — the narrowing gate). */
	private static final class ItemEntityDouble extends Entity {
		ItemEntityDouble(net.minecraft.world.level.Level aLevel) {
			super(EntityType.ITEM, aLevel);
		}
//? if forge {
		@Override protected void defineSynchedData() {/**/}
//?} else {
		/*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder aBuilder) {}
*///?}
		@Override protected void readAdditionalSaveData(CompoundTag aTag) {/**/}
		@Override protected void addAdditionalSaveData(CompoundTag aTag) {/**/}
	}

	// ---------------------------------------------------------------------------
	// the crafting cover (upstream CoverCrafting :46-60)
	// ---------------------------------------------------------------------------

	@Test
	void craftingCoverConsumesTheClickAndCarriesTheFlagPair() {
		CoverCrafting tCover = new CoverCrafting();
		// the null-player offline form (the upstream EntityPlayerMP gate skips the menu arm;
		// the ServerPlayer.openMenu face is the declared offline limit — see the class doc)
		assertTrue(tCover.onCoverClickedRight(TOP, null, null, TOP, 0.5F, 0.5F, 0.5F), "the click is consumed");
		assertFalse(tCover.isSealable(TOP, null), "the crafting face never seals (:57)");
		assertTrue(tCover.isDecorative(TOP, null), "the crafting face is decorative (:58)");
	}

	@Test
	void theTwoPlatesCarryTheirSprites() {
		CoverAsphalt tAsphalt = new CoverAsphalt();
		assertEquals(rid("block/asphalt"), tAsphalt.getCoverTextureSurface(TOP, null), "the asphalt plate sprite");
		CoverCrafting tCrafting = new CoverCrafting();
		assertEquals(rid("block/crafting/0"), tCrafting.getCoverTextureSurface(TOP, null), "the crafting plate sprite (variant 0, the declared fold)");
		assertEquals(tCrafting.getCoverTextureSurface(TOP, null), tCrafting.getCoverTextureAttachment(TOP, null, TOP), "the attachment face folds onto the plate (CoverTextureSimple)");
		assertNotNull(tAsphalt.mSprite, "the plate renderer seam is bound");
	}

	private static ResourceLocation rid(String aPath) {
		//? if forge {
		return new ResourceLocation("gt6", aPath);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
		 *///?}
	}
}
