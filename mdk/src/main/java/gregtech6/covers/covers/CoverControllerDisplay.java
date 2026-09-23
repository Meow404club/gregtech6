package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The machine status display cover — 1.20.1 port of gregapi/cover/covers/
 * CoverControllerDisplay.java (:43-124, task p35-covers-display-scale-6; upstream
 * MultiItemTechnological.java:61 meta 1002 "Machine Status Display Cover", dump
 * 状态显示覆盖板). Shows a machine's four running states as indicator lamps AND carries
 * an ON/OFF switch face.
 *
 * <p>The four-state visual build (:61-71, CS.B lane): each capability the host carries
 * marks its presence bit (B[5] possible, B[6] passively, B[7] actively, B[8] switchable)
 * and its live state lights the paired lamp bit (B[0]..B[3]); the low 10 bits rebuild
 * every tickPost, the style bits 10+ survive. The PORTED machine-form mapping
 * (CoverMachineLanes) carries all four capabilities, so the markers set unconditionally
 * and the lamps ride the lane reads.
 *
 * <p>The click arm (:74-83): on the covered face, the switch strip of the plate toggles
 * the machine — the bottom art (style 0) switches on its top quarter strip
 * ({@code x >= PX_N[6]=0.625 && y >= PX_N[4]=0.75}), the top art (style 1) on its bottom
 * quarter strip ({@code y <= PX_P[4]=0.25}); the face-coords mapping is the
 * UT.Code.getFacingCoordsClicked port (UT.java:1734-1744 verbatim).
 *
 * <p>The chisel arm (:47-53): cycles the art style — the style bits
 * ({@code (visuals >>> 10) + 1} mod 2) in the visual lane, 100 tool damage.
 *
 * <p>Declared deviation: the upstream plate composes the style base with up to five
 * indicator overlay sprites per lamp state (:85 BlockTextureMulti); the ported plate
 * renderer paints ONE sprite, so the plate shows the style base and the live lamp states
 * ride the visual lane (the composition surface stays pooled — the CoverSelectorTag
 * pre-composite precedent covers the static case, this lane is live). The texture pick
 * (:85) reduces to the style base sprite.
 */
public class CoverControllerDisplay extends AbstractCoverAttachmentController {

	/** The bottom-art sprite path — upstream CoverControllerDisplay.java:116, lowercased/underscored. */
	public static final String SPRITE_PATH_BOTTOM = "block/status_display/bottom/base";

	/** The top-art sprite path — upstream :117. */
	public static final String SPRITE_PATH_TOP = "block/status_display/top/base";

	/** Upstream CS.TOOL_chisel — the art-style cycle id (in-class on purpose: the ICover tool-id constants are a frozen surface). */
	public static final String TOOL_CHISEL = "chisel";

	/** The CS.B lane literals used by :63-67 (B[0]..B[9] == {@code 1 << n}). */
	private static final short B0 = 1, B1 = 2, B2 = 4, B3 = 8, B5 = 32, B6 = 64, B7 = 128, B8 = 256, LOW_BITS = 1023;

	/** Upstream PX_N[6] — the switch strip's left edge (0.625). */
	private static final float PX_N_6 = 0.625F;

	/** Upstream PX_N[4] — the bottom art's switch strip lower edge (0.75, the :78 upper quarter strip). */
	private static final float PX_N_4 = 0.75F;

	/** Upstream PX_P[4] — the top art's switch strip upper edge (0.25, the :79 lower quarter strip). */
	private static final float PX_P_4 = 0.25F;

	/**
	 * Upstream :44 — the display mounts on any running-state or switchable host (the
	 * ported machine forms carry the running face, the declared mapping).
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !(CoverMachineLanes.isMachineForm(aData.mTileEntity) || aData.mTileEntity instanceof ITileEntitySwitchableOnOff);
	}

	/**
	 * Upstream :56-58 — the display OVERRIDES the controller tick poll with an empty body
	 * ("Override usual Functionality"): the display never drives the machine from the
	 * poll; its click face is the only switch arm.
	 */
	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		// Override usual Functionality
	}

	/**
	 * Upstream :61-71 verbatim over the ported lane reads — clear the low 10 bits, mark
	 * each carried capability and light each live state.
	 */
	@Override
	public void onTickPost(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide) {
			short rVisuals = (short) (aData.mVisuals[aCoverSide] & ~LOW_BITS);
			// the ported machine forms carry all four capabilities — the instanceof gates
			// fold into the lane reads (the CoverMachineLanes mapping)
			rVisuals |= B5;
			if (CoverMachineLanes.runningPossible(aData.mTileEntity)) rVisuals |= B0;
			rVisuals |= B6;
			if (CoverMachineLanes.runningPassively(aData.mTileEntity)) rVisuals |= B1;
			rVisuals |= B7;
			if (CoverMachineLanes.runningActively(aData.mTileEntity)) rVisuals |= B2;
			if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff) {
				rVisuals |= B8;
				if (((ITileEntitySwitchableOnOff) aData.mTileEntity).getStateOnOff()) rVisuals |= B3;
			}
			aData.visual(aCoverSide, rVisuals);
		}
	}

	/**
	 * Upstream :74-83 — the switch strip of the plate toggles the machine (server side);
	 * the active strip depends on the art style. The :76 coords mapping is the
	 * getFacingCoordsClicked port.
	 */
	@Override
	public boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (aCoverSide == aSideClicked && aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE) {
			float[] tCoords = facingCoordsClicked(aSideClicked, aHitX, aHitY, aHitZ);
			switch ((short) ((aData.mVisuals[aCoverSide] >>> 10) % 2)) {
				case 0: if (tCoords[0] >= PX_N_6 && tCoords[1] >= PX_N_4) {if (aData.mTileEntity.isServerSideTE()) tTE.setStateOnOff(!tTE.getStateOnOff()); return true;} break;
				case 1: if (tCoords[0] >= PX_N_6 && tCoords[1] <= PX_P_4) {if (aData.mTileEntity.isServerSideTE()) tTE.setStateOnOff(!tTE.getStateOnOff()); return true;} break;
			}
		}
		return false;
	}

	/**
	 * Upstream :47-53 — the chisel cycles the style bits (the low 10 bits ride along);
	 * non-chisel ids answer 0 (the :52 host relay is not ported). The
	 * {@code sTexturesBase.length > 1} gate folds — the port always ships two styles.
	 */
	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (TOOL_CHISEL.equals(aToolId)) {
			aData.visual(aCoverSide, (short) ((aData.mVisuals[aCoverSide] & LOW_BITS) | ((((aData.mVisuals[aCoverSide] >>> 10) + 1) % 2) << 10)));
			return 100;
		}
		return 0;
	}

	/**
	 * Upstream :121-123 — the controller answer is the machine's own ON/OFF latch (the
	 * ported guard replaces the upstream raw cast; the base arms consult it on
	 * mount/load/block-update, the tick poll is overridden away).
	 */
	@Override
	public boolean getStateOnOff(byte aCoverSide, CoverData aData) {
		return aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && tTE.getStateOnOff();
	}

	/** Upstream :88 — the display's visual lane persists even though the base attachment does not. */
	@Override
	public boolean needsVisualsSaved(byte aCoverSide, CoverData aData) {
		return true;
	}

	/**
	 * Upstream :85-86 — the plate art reduces to the style base sprite (the indicator
	 * overlay composition is the declared deviation, see the class doc).
	 */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		String tPath = (aData.mVisuals[aCoverSide] >>> 10) % 2 != 0 ? SPRITE_PATH_TOP : SPRITE_PATH_BOTTOM;
		//? if forge {
		return new ResourceLocation("gt6", tPath);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", tPath);
		 *///?}
	}

	/**
	 * Upstream UT.Code.getFacingCoordsClicked (UT.java:1734-1744) verbatim — the click
	 * point on the face, in the face's 0..1 art space.
	 */
	private static float[] facingCoordsClicked(byte aSide, float aHitX, float aHitY, float aHitZ) {
		switch (aSide) {
			case 0: return new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitZ))};
			case 1: return new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, aHitZ))};
			case 2: return new float[] {Math.min(0.99F, Math.max(0, 1 - aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 3: return new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 4: return new float[] {Math.min(0.99F, Math.max(0, aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 5: return new float[] {Math.min(0.99F, Math.max(0, 1 - aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			default: return new float[] {0.5F, 0.5F};
		}
	}
}
