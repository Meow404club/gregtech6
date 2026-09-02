package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.covers.ICoverableTE;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The auto redstone machine switch cover — 1.20.1 port of gregapi/cover/covers/
 * CoverControllerAutoRedstone.java (73 lines, task p11-cover-controllers, the
 * p11-research-cover-remainder card q2). Mounts on a switchable machine and adds
 * the "lets it finish" arm to the plain redstone switch: while the machine is
 * actively running WITHOUT having produced ({@code mActive && !mSuccessful}) the
 * cover holds it ON regardless of the redstone on its face, so a mid-process
 * signal drop does not scrap the batch — upstream :70-72 verbatim:
 * {@code (RunningActively && !RunningSuccessfully) || bind1(getRedstoneIncoming(side)) != (mValues & B[0])}.
 *
 * <p>Five inlined arms — the SAME shape as {@link CoverControllerRedstone} (the
 * P10 controller; the research card's "second inline" ruling — the shared base
 * class {@code AbstractCoverAttachmentController} stays unported):
 * <ul>
 * <li>the placement gate (:33) — non-switchable hosts refuse the cover;</li>
 * <li>the removal reset (:36-39) — the machine is released to ON;</li>
 * <li>the mount + load sync (:41-49) — the state re-derives from the face and
 *     machine state on {@code onCoverPlaced} and {@code onCoverLoaded};</li>
 * <li>the block-update drive (:52-54) — rides the GTOvenBlock.neighborChanged →
 *     {@code covers().onBlockUpdate()} seam (the P10 controller precedent);</li>
 * <li>the server tick poll (:57-59) — the CoverData.tickPre dispatch, which is
 *     what picks the auto arm up mid-process (no block update fires between
 *     ticks, the face signal alone does not change).</li>
 * </ul>
 *
 * <p>Machine-state consumption (the research card's read-only mapping): upstream
 * {@code getStateRunningActively/getStateRunningSuccessfully} are
 * {@code return mActive}/{@code return mSuccessful} (MultiTileEntityBasicMachine.java:1025-1026);
 * this repo carries the same {@code public boolean mSuccessful, mActive, mRunning}
 * lane verbatim on BOTH coverable machine forms — TileEntityOven.java:162 (the
 * oven family's own copy) and TileEntityBasicMachine.java:176 (the
 * Shredder/Crusher/Lathe family). Neither class may be touched by this card, so
 * the auto arm reads the public fields through a two-form instanceof (the
 * upstream interface {@code ITileEntityRunningActively/Successfully} has no
 * ported implementor to key on — declared, not silent). Every other host: the
 * auto arm folds to false and the cover degrades to the plain P10 switch.
 *
 * <p>Declared deviations (both the P10 controller's, carried over):
 * <ul>
 * <li>the {@code magnifyingglass} read arm (:49-52) is cut — the ported
 *     ICover.onToolClick signature has no chat-return channel (the emitter
 *     precedent);</li>
 * <li>the {@code onToolClick2} host relay (:53) is cut (no ported host face);
 *     non-screwdriver tool clicks return 0.</li>
 * </ul>
 *
 * <p>Attachment flag inlining (upstream AbstractCoverAttachment :35-40) and the
 * texture (:67): upstream {@code machines/covers/autoredstoneswitch/circuit}
 * path-maps to {@code gt6:block/auto_redstone_switch/circuit}; the
 * attachment/holder faces (:64-65 with BACKGROUND_COVER) fold into the
 * single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults).
 */
public class CoverControllerAutoRedstone extends AbstractCoverDefault {

	/** The sprite path — upstream CoverControllerAutoRedstone.java:67, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/auto_redstone_switch/circuit";

	/** The sprite id ({@code gt6:block/auto_redstone_switch/circuit}); static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/** Upstream UT.Code.bind1 (UT.java:1553) — the 0/1 logic-scale clamp (the P10 controller's private copy, inlined again per the ruling). */
	private static int bind1(int aValue) {
		return Math.max(0, Math.min(1, aValue));
	}

	/**
	 * The auto arm's machine half, upstream {@code instanceof ITileEntityRunningActively
	 * && getStateRunningActively() && !(instanceof ITileEntityRunningSuccessfully &&
	 * getStateRunningSuccessfully())} (:71). Both coverable machine forms carry the
	 * {@code mActive}/{@code mSuccessful} lane verbatim, so the two-interface dance
	 * folds into one boolean read per form (the class-doc declared mapping); any
	 * other host folds to false — the plain redstone-switch semantics.
	 */
	private static boolean runsActivelyWithoutSuccess(ICoverableTE aTileEntity) {
		if (aTileEntity instanceof TileEntityOven) return ((TileEntityOven) aTileEntity).mActive && !((TileEntityOven) aTileEntity).mSuccessful;
		if (aTileEntity instanceof TileEntityBasicMachine) return ((TileEntityBasicMachine) aTileEntity).mActive && !((TileEntityBasicMachine) aTileEntity).mSuccessful;
		return false;
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
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(true);
		}
	}

	// ---------------------------------------------------------------------------
	// arm 3 — the mount + load sync (:41-49 verbatim, both arms same shape)
	// ---------------------------------------------------------------------------

	@Override
	public void onCoverLoaded(byte aCoverSide, CoverData aData) {
		super.onCoverLoaded(aCoverSide, aData);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	@Override
	public void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aCoverSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
			tTE.setStateOnOff(getStateOnOff(aCoverSide, aData));
		}
	}

	// ---------------------------------------------------------------------------
	// arm 4 — the block-update drive (:52-54; the conductor card's seam delivers)
	// ---------------------------------------------------------------------------

	@Override
	public void onBlockUpdate(byte aCoverSide, CoverData aData) {
		if (aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE && aData.mTileEntity.self().getLevel() != null) {
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
	// the screwdriver polarity toggle (:44-48; the magnifyingglass arm :49-52 and
	// the :53 host-relay arm are declared deviations — see the class doc)
	// ---------------------------------------------------------------------------

	@Override
	public long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ) {
		if (ICover.TOOL_SCREWDRIVER.equals(aToolId)) {
			aData.value(aCoverSide, (short) (aData.mValues[aCoverSide] ^ 1)); // :45 — B[0] == 1, no block update upstream
			// :46 — the chat line has no channel on the ported ICover signature
			return 1000; // :47
		}
		return 0; // the :53 onToolClick2 host-relay arm is not ported (no host face)
	}

	// ---------------------------------------------------------------------------
	// the polarity formula (:70-72 verbatim, the inlined abstract base method)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :70-72 verbatim: {@code (actively && !successfully) || bind1(incoming)
	 * != (mValues[side] & B[0])}. The auto arm holds the machine ON through a
	 * mid-process signal drop (active, nothing produced yet — "lets it finish",
	 * the upstream item tooltip); the redstone arm takes over again once the
	 * process ends (successful) or never started (inactive).
	 */
	public boolean getStateOnOff(byte aCoverSide, CoverData aData) {
		return runsActivelyWithoutSuccess(aData.mTileEntity) || bind1(aData.mTileEntity.getRedstoneIncoming(aCoverSide)) != (aData.mValues[aCoverSide] & 1);
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

	/** Upstream :67 — the switch art (the attachment/holder faces fold in, :64-65). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
