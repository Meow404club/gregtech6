package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;

/**
 * The shutter cover — 1.20.1 port of gregapi/cover/covers/CoverShutter.java:40-96
 * (task p11-cover-shutter-filter; upstream item id 1026 "Shutter Cover",
 * MultiItemTechnological.java:85). A pure gate: the covered face refuses item AND
 * fluid transfer while the gate is CLOSED, admits it while OPEN. Upstream :82-85
 * verbatim — closed means {@code (visual == 0) == mStopped}, i.e. a NORMAL plate
 * (visual 0) is open by default and closes when a controller cover stops the covers,
 * while an INVERTED plate (visual 1, the screwdriver toggle :52-54) is closed by
 * default and opens on the stop. The screwdriver flip returns the tool damage 1000
 * (:62); the {@code magnifyingglass} readback (:64-67) has no chat channel on the
 * ported ICover.onToolClick signature and answers 0 (the CoverRedstoneEmitter
 * declared-deviation precedent — the readback rides the /gt6cover mode report).
 *
 * <p>DECLARED CUT — the pipe/wire connect arms: upstream toggles the HOST's pipe/wire
 * connection alongside the plate ({@code ITileEntityConnector.connect/disconnect} at
 * :55-61 inside the screwdriver arm and :72-80 in {@code onStoppedUpdate}). This port
 * has no pipe/wire connector domain behind the coverable hosts (the connectors/ freeze
 * face), so both arms collapse to nothing: {@code onStoppedUpdate} keeps the base
 * no-op and the screwdriver arm toggles only the visual lane. Upstream
 * {@code showsConnectorFront=false} (:91) rides the pooled connector-hooks group
 * (ICover class doc) and has no face here either.
 *
 * <p>Attachment flag inlining (upstream base AbstractCoverAttachment :35-40, not
 * ported — the CoverRedstoneEmitter precedent): the click intercepts stay false so a
 * click on the plate falls through to the host's own action, and the plate is
 * non-opaque/non-sealable like every attachment cover. The {@code addToolTips} pair
 * (:44-48) rides the cut tooltip channel.
 */
public class CoverShutter extends AbstractCoverDefault {

	/** The atlas sprite of the normal (default-open) plate — visual 0. */
	public static final ResourceLocation SPRITE_NORMAL = new ResourceLocation("gt6", "block/shutter/normal");

	/** The atlas sprite of the inverted (default-closed) plate — visual 1. */
	public static final ResourceLocation SPRITE_INVERTED = new ResourceLocation("gt6", "block/shutter/inverted");

	/**
	 * Upstream :82-85 verbatim — the closed predicate shared by the four intercepts:
	 * a normal plate (visual 0) gates exactly while the covers are stopped, an
	 * inverted plate (visual 1) gates exactly while they run.
	 */
	public static boolean isClosed(byte aCoverSide, CoverData aData) {
		return (aData.mVisuals[aCoverSide] == 0) == aData.mStopped;
	}

	/** Upstream :82 — the closed gate refuses the insert through its face. */
	@Override
	public boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return isClosed(aCoverSide, aData);
	}

	/** Upstream :83 — the closed gate refuses the extract through its face. */
	@Override
	public boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide) {
		return isClosed(aCoverSide, aData);
	}

	/** Upstream :84 — the closed gate refuses the fill through its face. */
	@Override
	public boolean interceptFluidFill(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToFill) {
		return isClosed(aCoverSide, aData);
	}

	/** Upstream :85 — the closed gate refuses the drain through its face. */
	@Override
	public boolean interceptFluidDrain(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToDrain) {
		return isClosed(aCoverSide, aData);
	}

	/**
	 * Upstream :52-63 — the screwdriver flips normal ↔ inverted (visual 0 ↔ 1) and
	 * returns the tool damage 1000. The upstream {@code ITileEntityConnector}
	 * connect/disconnect arm (:55-61) is the DECLARED CUT recorded in the class doc —
	 * no connector host exists behind the ported coverable family. The
	 * {@code magnifyingglass} readback (:64-67) has no chat channel — 0, not handled.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.visual(aCoverSide, (short) (aData.mVisuals[aCoverSide] == 0 ? 1 : 0)); // :53
			return 1000; // :62 (the :54 chat line has no channel on the ported ICover signature)
		}
		return 0; // the :55-61 connector arm is the declared cut; :64-67 magnifyingglass has no chat channel
	}

	/**
	 * Upstream :90 — the open/closed state survives the save (the CoverData
	 * writeToNBT :87 gate): the shutter state must outlive a restart.
	 */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/**
	 * Upstream :87 — the plate art follows the normal/inverted state. The upstream
	 * {@code BlockTextureMulti(BACKGROUND_COVER, plate)} stack lands as the single
	 * borrowed sprite on the single-sprite plate renderer (assets/README.md); the
	 * attachment/holder faces (:88-89) fold into the same sprite.
	 */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return aData.mVisuals[aCoverSide] == 0 ? SPRITE_NORMAL : SPRITE_INVERTED;
	}
}
