package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICoverableTE;

/**
 * The first cover — 1.20.1 port of gregapi/cover/covers/CoverTextureSimple.java:37-57
 * (task p4-cover-core ④, the ADR FIRST_PORT_CANDIDATE: 19 lines of logic covering the
 * install/crowbar/texture/sound whole chain). The upstream {@code ITexture} field becomes
 * the atlas sprite id the plate renderer stitches ({@code mSprite}); the upstream custom
 * {@code mSound} string becomes a nullable vanilla {@link SoundEvent} (null = the
 * AbstractCoverDefault placeholder sounds — the GT sound registration stays pooled).
 *
 * <p>The attachment/holder hooks (:50-51) keep the upstream shape: the attachment face
 * (toward the neighbouring block) shows the surface sprite, the holder shows the
 * surface sprite as well — the upstream {@code BACKGROUND_COVER} under-layer belongs to
 * the 1.7.10 multi-pass render stack and folds into the single-sprite plate here.
 */
public class CoverTextureSimple extends AbstractCoverDefault {

	public final ResourceLocation mSprite;
	@Nullable
	public final SoundEvent mSound;

	public CoverTextureSimple(ResourceLocation aSprite) {
		this(aSprite, null);
	}

	public CoverTextureSimple(ResourceLocation aSprite, @Nullable SoundEvent aSound) {
		mSprite = aSprite;
		mSound = aSound;
	}

	@Override public ResourceLocation getCoverTextureSurface(byte aSide, CoverData aData) {return mSprite;} // :49
	@Override public ResourceLocation getCoverTextureAttachment(byte aSide, CoverData aData, byte aTextureSide) {return mSprite;} // :50 (BACKGROUND_COVER folds into the plate)
	@Override public ResourceLocation getCoverTextureHolder(byte aSide, CoverData aData, byte aTextureSide) {return mSprite;} // :51

	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, Entity aPlayer, ItemStack aCover) { // :53
		if (aPlayer != null) {
			SoundEvent tSound = mSound == null ? SoundEvents.WOOD_PLACE : mSound; // SFX.GT_SCREWDRIVER placeholder
			aPlayer.level().playSound(aPlayer, aData.mTileEntity.self().getBlockPos(), tSound, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	@Override
	public void onAfterCrowbar(ICoverableTE aTileEntity) { // :54
		if (aTileEntity.self().getLevel() != null) {
			SoundEvent tSound = mSound == null ? SoundEvents.STONE_BREAK : mSound; // SFX.MC_BREAK placeholder
			aTileEntity.self().getLevel().playSound(null, aTileEntity.self().getBlockPos(), tSound, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	@Override public boolean isDecorative(byte aCoverSide, CoverData aData) {return true;} // :55
	@Override public boolean needsVisualsSaved(byte aSide, CoverData aData) {return false;} // :56
}
