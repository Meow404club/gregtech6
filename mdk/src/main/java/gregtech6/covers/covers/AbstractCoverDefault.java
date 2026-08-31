package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.CoverRegistry;
import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;

/**
 * The default implementation for regular covers — 1.20.1 port of
 * gregapi/cover/covers/AbstractCoverDefault.java (:49-112), trimmed to the ported
 * {@link ICover} surface (task p4-cover-core ②). The sound hooks map the upstream
 * custom SFX.GT_SCREWDRIVER/SFX.MC_BREAK onto vanilla equivalents (the GT sound
 * registration system stays pooled); the addToolTips :77 and bounds/collisions :85-88
 * stay pooled; the redstone defaults :78-80 are RESTORED with the cover redstone
 * framework (task p9-redstone-hooks — a plain cover is transparent to redstone); the
 * GUI/logistics defaults :82-83/:93-109 live in their pooled method groups.
 */
public abstract class AbstractCoverDefault implements ICover {

	/** Upstream :206 BOXES_COVERS — the cover plate spans the outer 2 pixels of the face (e.g. DOWN: y 0..2/16). */
	public static final float COVER_PLATE_THICKNESS = 2.0F / 16.0F;

	@Override public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {return false;} // :50
	@Override public boolean interceptCoverRemoval(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {return false;} // :51
	@Override public void onCoverLoaded(byte aCoverSide, CoverData aData) {/**/} // :52

	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) { // :53 — SFX.GT_SCREWDRIVER → vanilla placeholder
		if (aPlayer != null && aPlayer.level() != null) aPlayer.level().playSound(null, aData.mTileEntity.self().getBlockPos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	@Override public void onCoverRemove(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {/**/} // :54

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {/**/} // :55

	@Override
	public void onTickPost(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {/**/} // :56

	@Override public boolean onCoverClickedLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return false;} // :57
	@Override public boolean interceptClickLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return true;} // :58
	@Override public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return false;} // :59
	@Override public boolean interceptClickRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return true;} // :60

	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {return 0;} // :61

	@Override
	public ItemStack getCoverItem(byte aCoverSide, CoverData aData) { // :62 — ST.make(id, 1, meta, nbt)
		ItemStack tStack = new ItemStack(CoverRegistry.getItem(aData.mIDs[aCoverSide]), 1);
		CompoundTag tTag = aData.mNBTs[aCoverSide];
		if (tTag != null && !tTag.isEmpty()) tStack.setTag(tTag.copy());
		return tStack;
	}

	@Override public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {return null;} // :63
	@Override public ResourceLocation getCoverTextureAttachment(byte aCoverSide, CoverData aData, byte aTextureSide) {return getCoverTextureSurface(aCoverSide, aData);} // :64
	@Override public ResourceLocation getCoverTextureHolder(byte aCoverSide, CoverData aData, byte aTextureSide) {return getCoverTextureSurface(aCoverSide, aData);} // :65

	@Override public boolean isSolid(byte aCoverSide, CoverData aData) {return true;} // :66
	@Override public boolean isOpaque(byte aCoverSide, CoverData aData) {return true;} // :67
	@Override public boolean isSealable(byte aCoverSide, CoverData aData) {return true;} // :68
	@Override public boolean isFullTexture(byte aCoverSide, CoverData aData) {return true;} // :69
	@Override public boolean isDecorative(byte aCoverSide, CoverData aData) {return false;} // :70
	@Override public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {return false;} // :72

	@Override
	public void onAfterCrowbar(ICoverableTE aTileEntity) { // :74 — SFX.MC_BREAK → vanilla placeholder
		if (aTileEntity.self().getLevel() != null) aTileEntity.self().getLevel().playSound(null, aTileEntity.self().getBlockPos(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	@Override public void onBlockUpdate(byte aCoverSide, CoverData aData) {/**/} // :75
	@Override public void onStoppedUpdate(byte aCoverSide, CoverData aData, boolean aStopped) {/**/} // :76

	/**
	 * Upstream :78 — the neighbouring block's signal read at the covered face. The
	 * {@code getIndirectPowerLevelTo} counterpart is {@code Level.getSignal} at
	 * {@code pos.relative(face)} (the ADR declared deviation, truth-table pinned), the
	 * result clamped to the 0..15 redstone scale (UT.Code.bind4). A plain cover is
	 * transparent to redstone — the world read passes straight through.
	 */
	@Override
	public byte getRedstoneIn(byte aCoverSide, CoverData aData) {
		Level tLevel = aData.mTileEntity.self().getLevel();
		if (tLevel == null) return 0;
		Direction tFace = Direction.from3DDataValue(aCoverSide);
		BlockPos tNeighbour = aData.mTileEntity.self().getBlockPos().relative(tFace);
		return (byte) Math.max(0, Math.min(15, tLevel.getSignal(tNeighbour, tFace)));
	}

	/** Upstream :79 — a non-emitting cover passes the machine's own weak emission through. */
	@Override
	public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {return aDefaultRedstone;}

	/** Upstream :80 — a non-emitting cover passes the machine's own strong emission through. */
	@Override
	public byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {return aDefaultRedstone;}

	/** Upstream default for the :218 hook — no gate unless the cover mounts one (the pump cover does). */
	@Override public boolean interceptFluidFill(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToFill) {return false;}

	/** Upstream default for the :219 hook. */
	@Override public boolean interceptFluidDrain(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToDrain) {return false;}
}
