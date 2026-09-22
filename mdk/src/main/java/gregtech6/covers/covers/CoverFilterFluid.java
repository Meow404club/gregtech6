package gregtech6.covers.covers;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;

/**
 * The fluid filter cover — 1.20.1 port of gregapi/cover/covers/CoverFilterFluid.java
 * :46-140 (task p34-covers-gameplay-10; upstream item id 1024 "Fluid Filter"). The
 * covered face passes fluid transfer only when the fluid satisfies the filter: visual
 * 0 = WHITELIST (only the filter fluid passes), visual 1 = BLACKLIST (only the filter
 * fluid is refused), toggled with the screwdriver (:62-66). The filter fluid is set by
 * right-clicking the plate with a fluid container — ONLY while the lane is empty
 * (:95, the CoverFilterItem :93/:94 same-shape gate; CHANGING it requires the
 * soft-hammer clear first) — stored in the
 * {@link CoverData#mNBTs} lane under the verbatim upstream key {@code gt.filter.fluid}
 * (:95/:106), and cleared with the soft hammer (:67-70). NOT NBT sensitive — the
 * upstream match is {@code FL.equal(filter, stack, T)} (:121/:128), which this port
 * translates to fluid identity.
 *
 * <p>The filter lane shape rides the CoverFilterItem port verbatim (the single-tag
 * {@code mNBTs} lane, the :118 empty-filter polarity) with the payload a fluid
 * registry name instead of an item stack. The right-click resolver accepts any
 * fluid-container stack ({@code FluidUtil.getFluidContained}, the upstream
 * {@code FL.getFluid(stack, T)} + the ore-dict container fallback :98-104 — the
 * ore-dict half rides the cut tool/om system).
 *
 * <p>DECLARED CUT — the pipe arms (the CoverFilterItem class-doc precedent): upstream
 * has none here and the {@code magnifyingglass} readback (:71-88) has no chat channel
 * — 0, not handled; the {@code SFX.MC_CLICK} feedback maps onto the vanilla UI click
 * (the CoverFilterItem precedent).
 */
public class CoverFilterFluid extends AbstractCoverDefault {

	/** Upstream CS.TOOL_softhammer (the filter clear, :67-70) — in-class like the filter item's TOOL_SOFTHAMMER. */
	public static final String TOOL_SOFTHAMMER = "softhammer";

	/** The verbatim upstream filter key inside the per-face {@link CoverData#mNBTs} compound (:68/:95/:106). */
	public static final String FILTER_KEY = "gt.filter.fluid";

	/** The atlas sprite of the whitelist plate — visual 0 (upstream sTextureNormal, :139). */
	//? if forge {
	public static final ResourceLocation SPRITE_WHITELIST = new ResourceLocation("gt6", "block/filterfluid/normal");
	//?} else {
	/*public static final ResourceLocation SPRITE_WHITELIST = ResourceLocation.fromNamespaceAndPath("gt6", "block/filterfluid/normal");
	 *///?}

	/** The atlas sprite of the blacklist plate — visual 1 (upstream sTextureInverted, :138). */
	//? if forge {
	public static final ResourceLocation SPRITE_BLACKLIST = new ResourceLocation("gt6", "block/filterfluid/inverted");
	//?} else {
	/*public static final ResourceLocation SPRITE_BLACKLIST = ResourceLocation.fromNamespaceAndPath("gt6", "block/filterfluid/inverted");
	 *///?}

	/**
	 * Upstream :106 — the filter-lane write for the resolved fluid: the fluid registry
	 * name under {@link #FILTER_KEY}. Pure for the offline tests (the player-narrowed
	 * click arm needs the live entity system).
	 */
	public static CompoundTag filterTagFor(Fluid aFluid) {
		CompoundTag tLane = new CompoundTag();
		tLane.putString(FILTER_KEY, ForgeRegistries.FLUIDS.getKey(aFluid).toString());
		return tLane;
	}

	/**
	 * Upstream :121/:128 — the NBT-insensitive filter match: {@code FL.equal(filter,
	 * stack, T)} over a {@code gt.filter.fluid} payload becomes fluid identity.
	 */
	public static boolean matches(CoverData aData, byte aCoverSide, @Nullable FluidStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		Fluid tFilter = ForgeRegistries.FLUIDS.getValue(ResourceLocation.tryParse(aData.mNBTs[aCoverSide].getString(FILTER_KEY)));
		return tFilter != null && tFilter == aStack.getFluid(); // FL.equal(filter, stack, T): fluid yes, NBT no, amount no
	}

	/** Upstream :120/:127 — the empty-filter polarity shared by both gates. */
	static boolean emptyFilterRefusal(CoverData aData, byte aCoverSide) {
		return aData.mVisuals[aCoverSide] == 0; // whitelist refuses everything, blacklist admits everything
	}

	/**
	 * Upstream :117-122 verbatim — the fill gate: another face is not gated (:118), a
	 * stopped controller refuses everything (:119), an EMPTY filter rides the whitelist/
	 * blacklist polarity (:120) and a set filter gates by the polarity (:121).
	 */
	@Override
	public boolean interceptFluidFill(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToFill) {
		if (aCoverSide != aSide) return false; // :118
		if (aData.mStopped || aFluidToFill == null) return true; // :119
		if (aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_STRING)) return emptyFilterRefusal(aData, aCoverSide); // :120
		return (aData.mVisuals[aCoverSide] == 0) != matches(aData, aCoverSide, aFluidToFill); // :121
	}

	/** Upstream :124-129 — the drain gate, same shape as the fill gate. */
	@Override
	public boolean interceptFluidDrain(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToDrain) {
		if (aCoverSide != aSide) return false; // :125
		if (aData.mStopped || aFluidToDrain == null) return true; // :126
		if (aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_STRING)) return emptyFilterRefusal(aData, aCoverSide); // :127
		return (aData.mVisuals[aCoverSide] == 0) != matches(aData, aCoverSide, aFluidToDrain); // :128
	}

	/**
	 * Upstream :62-70 — the screwdriver flips whitelist ↔ blacklist (visual 0 ↔ 1,
	 * damage 1000) and the soft hammer clears the filter (damage 10000). The
	 * {@code magnifyingglass} readback (:71-88) has no chat channel — 0, not handled.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short) (aData.mVisuals[aCoverSide] == 0 ? 1 : 0)); // :63
			return 1000; // :65 (the :64 chat line has no channel on the ported ICover signature)
		}
		if (TOOL_SOFTHAMMER.equals(aToolId)) {
			if (aData.mNBTs[aCoverSide] != null) aData.mNBTs[aCoverSide].remove(FILTER_KEY); // :68
			return 10000; // :69
		}
		return 0; // the :71-88 magnifyingglass readback has no chat channel
	}

	/**
	 * Upstream :93-114 — the right-click-with-container filter set. Server-side players
	 * only (:94); the held stack must carry a fluid (the container resolver below); the
	 * chat line (:108) rides the cut channel and the SFX.MC_CLICK maps onto the vanilla
	 * UI click (:107, the CoverFilterItem precedent). Always {@code true} (:114 — the
	 * click is consumed even when the body skipped, upstream included).
	 */
	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		Player tPlayer = ICover.asPlayer(aPlayer); // upstream aPlayer instanceof EntityPlayer
		if (tPlayer != null && aData.mTileEntity.isServerSideTE() && acceptsFilterWrite(aData, aCoverSide)) { // :94/:95
			ItemStack tHeld = tPlayer.getMainHandItem(); // upstream getCurrentEquippedItem
			if (!tHeld.isEmpty()) { // :97 ST.valid
				Optional<FluidStack> tFluid = FluidUtil.getFluidContained(tHeld); // :98 FL.getFluid (the ore-dict container half rides the cut om system)
				if (tFluid.isPresent() && !tFluid.get().isEmpty()) {
					aData.mNBTs[aCoverSide] = filterTagFor(tFluid.get().getFluid()); // :106
					if (aData.mTileEntity.self().getLevel() != null) // :107 SFX.MC_CLICK → the vanilla UI click placeholder
						aData.mTileEntity.self().getLevel().playSound(null, aData.mTileEntity.self().getBlockPos(),
								net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
					// :108 UT.Entities.sendchat — no chat channel (declared deviation)
				}
			}
		}
		return true; // :114
	}

	/**
	 * Upstream :95 — only an EMPTY filter lane accepts the right-click write ({@code
	 * mNBTs == null || !hasKey("gt.filter.fluid")}); CHANGING the filter fluid requires
	 * the soft-hammer clear first (:67-70), the CoverFilterItem :93/:94 same-shape gate.
	 * Pure for the offline pin (the click arm narrows the player above it).
	 */
	public static boolean acceptsFilterWrite(CoverData aData, byte aCoverSide) {
		return aData.mNBTs[aCoverSide] == null || !aData.mNBTs[aCoverSide].contains(FILTER_KEY, Tag.TAG_STRING);
	}

	/**
	 * Upstream :134 — the whitelist/blacklist mode survives the save (the CoverData
	 * writeToNBT :87 gate); the filter fluid itself rides the unconditional
	 * {@link CoverData#mNBTs} lane write (:86).
	 */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/** Upstream :131 — the plate art follows the whitelist/blacklist mode (the attachment/holder stack folds, the CoverFilterItem precedent). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? SPRITE_WHITELIST : SPRITE_BLACKLIST;
	}
}
