package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The redstone machine switch cover — 1.20.1 port of gregapi/cover/covers/
 * CoverControllerRedstone.java (71 lines, task p10-cover-controller-redstone).
 * Mounts on a switchable machine and holds it stopped while its face sees NO
 * redstone (default value-lane polarity) — or the inverse after the screwdriver
 * toggle: {@code getStateOnOff = bind1(getRedstoneIncoming(side)) != (mValues &
 * B[0])} (:68-70, UT.Code.bind1 = UT.java:1553, B[0] == 1).
 *
 * <p>The controller BASE CLASS is NOT ported (upstream
 * AbstractCoverAttachmentController.java:32-62, single consumer = YAGNI, the C-card
 * ADR ② same-shape declaration): its five arms are INLINED here verbatim —
 * <ul>
 * <li>the placement gate (:33) — non-switchable hosts refuse the cover;</li>
 * <li>the removal reset (:36-39) — the machine is released to ON;</li>
 * <li>the mount + load sync (:41-49) — the state re-derives from the face on
 *     {@code onCoverPlaced} and {@code onCoverLoaded};</li>
 * <li>the block-update drive (:52-54) — rides the GTOvenBlock.neighborChanged →
 *     {@code covers().onBlockUpdate()} seam the conductor card landed
 *     (task p10-cover-conductor-redstone, commit bef7527);</li>
 * <li>the server tick poll (:57-59) — the CoverData.tickPre dispatch the ticking
 *     hosts already run.</li>
 * </ul>
 * The upstream {@code getWorld() != null} guards translate to
 * {@code self().getLevel() != null}; the tick arm keeps its own
 * {@code aIsServerSide} gate (upstream :57-59 has no world check).
 *
 * <p>Declared deviations (both recorded):
 * <ul>
 * <li>the {@code magnifyingglass} read arm (:47-50) is cut — the ported
 *     ICover.onToolClick signature has no chat-return channel (the emitter
 *     precedent); non-screwdriver tool clicks return 0 and the :51 host-relay arm
 *     has no ported face;</li>
 * <li>the value-lane toggle write (:43) keeps the upstream TWO-ARG
 *     {@code value(side, v)} call — the switch emits NO redstone (zero
 *     getRedstoneOut overrides), so a neighbour refresh would be pointless; the
 *     tick poll (:57-59) and the next block update pick the new polarity up.</li>
 * </ul>
 *
 * <p>Attachment flag inlining (upstream AbstractCoverAttachment :35-40, the
 * emitter/conductor precedent — the base class is not ported): both click
 * intercepts fall through, the plate is non-opaque/non-sealable. Texture (:65):
 * upstream {@code machines/covers/redstoneswitch/circuit} path-maps to
 * {@code gt6:block/redstone_switch/circuit} (vanilla atlas directory source); the
 * attachment/holder faces (:62-63 with BACKGROUND_COVER) fold into the
 * single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults).
 */
public class CoverControllerRedstone extends AbstractCoverDefault {

	/** The sprite path — upstream CoverControllerRedstone.java:65, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/redstone_switch/circuit";

	/** The sprite id ({@code gt6:block/redstone_switch/circuit}); static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/** Upstream UT.Code.bind1 (UT.java:1553) — the 0/1 logic-scale clamp. */
	private static int bind1(int aValue) {
		return Math.max(0, Math.min(1, aValue));
	}

	// ---------------------------------------------------------------------------
	// arm 1 — the placement gate (AbstractCoverAttachmentController :33 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !(aData.mTileEntity instanceof ITileEntitySwitchableOnOff);
	}

	// ---------------------------------------------------------------------------
	// arm 2 — the removal reset (:36-39 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onCoverRemove(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		super.onCoverRemove(aCoverSide, aData, aPlayer);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && tTE.self().getLevel() != null) {
			tTE.setStateOnOff(true);
		}
	}

	// ---------------------------------------------------------------------------
	// arm 3 — the mount + load sync (:41-49 verbatim, both arms same shape)
	// ---------------------------------------------------------------------------

	@Override
	public void onCoverLoaded(byte aCoverSide, CoverData aData) {
		super.onCoverLoaded(aCoverSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && tTE.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aCoverSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && tTE.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	// ---------------------------------------------------------------------------
	// arm 4 — the block-update drive (:52-54; the conductor card's seam delivers)
	// ---------------------------------------------------------------------------

	@Override
	public void onBlockUpdate(byte aCoverSide, CoverData aData) {
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && tTE.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	// ---------------------------------------------------------------------------
	// arm 5 — the server tick poll (:57-59)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide && aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	// ---------------------------------------------------------------------------
	// the screwdriver polarity toggle (:42-46; the magnifyingglass arm :47-50 and
	// the :51 host-relay arm are declared deviations — see the class doc)
	// ---------------------------------------------------------------------------

	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.value(aCoverSide, (short) (aData.mValues[aCoverSide] ^ 1)); // :43 — B[0] == 1, no block update upstream
			// :44 — the chat line has no channel on the ported ICover signature
			return 1000; // :45
		}
		return 0; // the :51 onToolClick2 host-relay arm is not ported (no host face)
	}

	// ---------------------------------------------------------------------------
	// the polarity formula (:68-70 verbatim, the inlined abstract base method)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :68-70 verbatim: {@code bind1(getRedstoneIncoming(side)) !=
	 * (mValues[side] & B[0])}. Value-lane bit 0 clear (the fresh mount) = the
	 * machine RUNS while the face sees signal; set (the screwdriver toggle) = the
	 * machine runs while it does NOT — the upstream chat wording (:44).
	 */
	public boolean getStateOnOff(byte aCoverSide, CoverData aData) {
		return bind1(aData.mTileEntity.getRedstoneIncoming(aCoverSide)) != (aData.mValues[aCoverSide] & 1);
	}

	// ---------------------------------------------------------------------------
	// the inlined attachment flags (AbstractCoverAttachment :35-40) + texture
	// ---------------------------------------------------------------------------

	/** Upstream AbstractCoverAttachment :37 — a controller click is never consumed. */
	@Override
	public boolean interceptClickLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream AbstractCoverAttachment :38 — same shape as the left click. */
	@Override
	public boolean interceptClickRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		return false;
	}

	/** Upstream AbstractCoverAttachment :39 — the plate never blocks the host's light. */
	@Override
	public boolean isOpaque(byte aCoverSide, CoverData aData) {
		return false;
	}

	/** Upstream AbstractCoverAttachment :40 — the plate never seals the host. */
	@Override
	public boolean isSealable(byte aCoverSide, CoverData aData) {
		return false;
	}

	/** Upstream :65 — the switch art (the attachment/holder faces fold in, :62-63). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
