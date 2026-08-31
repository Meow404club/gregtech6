package gregtech6.covers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.fluid.FluidTankGT;
import gregtech6.util.UT6;

/**
 * The composite coverable-BE surface — 1.20.1 port of the upstream
 * {@code ITileEntityCoverable} (ITileEntityCoverable.java:33-43) PLUS the cover-facing
 * logic of {@code TileEntityBase06Covers} (task p4-cover-core ⑤). The ADR pins the
 * attachment as COMPOSITION: a host BlockEntity declares {@code implements ICoverableTE},
 * owns the {@code mCovers} field through {@link #getCovers()}/{@link #setCovers} and
 * inherits the whole 06Covers behaviour as default methods — the BE base-class chain
 * (01Root..03TicksAndSync) stays untouched.
 *
 * <p>Ported 06Covers anchors: {@link #setCoverItem} :284-311 with the
 * checkIfCoversEmptyAndDeleteIfNeeded tail :313-317 (an all-empty store returns to
 * {@code null}), {@link #getCoverItem} :279-281, {@link #checkCoverValidity} :207-215,
 * {@link #allowCover} :222-224 / {@link #allowCovers} :226-228,
 * {@link #attachCoversFirst} :234-236, {@link #hasCovers} :246-248,
 * {@link #isCovered} :250-252, {@link #updateCoverVisuals} :258-261, the right-click
 * dispatch {@link #onCoverUse} :106-133 and the tool-click (crowbar) dispatch
 * {@link #onCoverToolClick} :140-163.
 *
 * <p>Sync + render: cover changes flag the BE sync window via {@link #syncCoverClientData}
 * (upstream updateClientData :308 → the 03 two-channel sync — chunk data + block-update
 * packets both carry the covers through {@code saveAdditional/load}) and the client lands
 * the render refresh in its own {@code onDataPacket}/{@code onLoad} hooks calling
 * {@code GTRenderUpdates.scheduleRenderUpdate} (the pair's client branch; the server
 * blockEvent forward stays the GTRenderUpdates class-doc template for Blocks we own).
 *
 * <p>Redstone query exits (task p9-redstone-hooks, upstream 04Covers :408-441 with the
 * Root :577-588 world-only form): {@link #getRedstoneIncoming} — the covered face
 * answers through {@link ICover#getRedstoneIn}, bare faces read the world directly;
 * {@link #getRedstoneOutWeak}/{@link #getRedstoneOutStrong} — the cover on the OPOS
 * face overrides the machine's own emission (fed in as the default argument). The
 * Block-carrier bridge lives on {@code GTOvenBlock.getSignal/getDirectSignal}; the
 * multiblock bases are deliberately NOT bridged (ADR FORBIDDEN ④ — the multiblock
 * per-face redstone dispatch is unresearched).
 */
public interface ICoverableTE {

	// ---------------------------------------------------------------------------
	// host accessors (the composition contract)
	// ---------------------------------------------------------------------------

	/** The host's cover store — {@code null} while no face carries a cover. */
	@Nullable
	CoverData getCovers();

	/** The host's store setter (the :313-317 empty-store cleanup writes {@code null}). */
	void setCovers(@Nullable CoverData aCoverData);

	/** The BlockEntity view of the host — every host IS a BlockEntity by contract. */
	default BlockEntity self() {
		return (BlockEntity) this;
	}

	/** @return true when the host BE lives on the server side. */
	default boolean isServerSideTE() {
		Level tLevel = self().getLevel();
		return tLevel != null && !tLevel.isClientSide;
	}

	// ---------------------------------------------------------------------------
	// state queries (06Covers :246-252)
	// ---------------------------------------------------------------------------

	/** Upstream :246-248. */
	default boolean hasCovers() {
		return getCovers() != null;
	}

	/** Upstream :250-252. */
	default boolean isCovered(byte aSide) {
		return hasCovers() && validSide(aSide) && getCovers().mBehaviours[aSide] != null;
	}

	/** The GT6 side order == {@code Direction.get3DDataValue()} (0..5). */
	static boolean validSide(byte aSide) {
		return aSide >= 0 && aSide < 6;
	}

	// ---------------------------------------------------------------------------
	// admission policy (06Covers :222-236)
	// ---------------------------------------------------------------------------

	/** Upstream :222-224. */
	default boolean allowCover(byte aSide, ICover aCover) {
		return allowCovers(aSide);
	}

	/** Upstream :226-228 — hosts narrow (e.g. pipe ends refuse covers later). */
	default boolean allowCovers(byte aSide) {
		return true;
	}

	/** Upstream :234-236 — install before the host's own right-click action. */
	default boolean attachCoversFirst(byte aSide) {
		return true;
	}

	// ---------------------------------------------------------------------------
	// items (06Covers :279-281)
	// ---------------------------------------------------------------------------

	/** Upstream :279-281 — the removal drop of that face. */
	default ItemStack getCoverItem(byte aSide) {
		return hasCovers() && validSide(aSide) ? getCovers().getCoverItem(aSide) : ItemStack.EMPTY;
	}

	// ---------------------------------------------------------------------------
	// setCoverItem (06Covers :284-317 verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :284-311 verbatim: admission gate (:285), no-op removal (:286), lazy store
	 * (:288), the non-force intercepts (:290-298 — occupied face, unregistered item,
	 * allowCover, interceptCoverPlacement), onCoverRemove (:300), the set (:302),
	 * onCoverPlaced (:304), causeBlockUpdate (:306), updateClientData (:308) and the
	 * empty-store cleanup :310/:313-317. Empty stacks are the 1.20.1 form of the upstream
	 * {@code null} stack.
	 *
	 * @return true when the store ended in a consistent state (install accepted, removal
	 *         happened, or the rejected request left the store empty).
	 */
	default boolean setCoverItem(byte aSide, @Nullable ItemStack aStack, @Nullable Entity aPlayer, boolean aForce, boolean aBlockUpdate) {
		if (!validSide(aSide) || (!allowCovers(aSide) && aStack != null && !aStack.isEmpty())) return false;
		if ((aStack == null || aStack.isEmpty()) && getCoverItem(aSide).isEmpty()) return false;

		if (getCovers() == null) setCovers(CoverRegistry.coverdata(this, null));

		if (!aForce) {
			if (aStack == null || aStack.isEmpty()) {
				// :292
				if (getCovers().mBehaviours[aSide] != null && getCovers().mBehaviours[aSide].interceptCoverRemoval(aSide, getCovers(), aPlayer)) return !checkIfCoversEmptyAndDeleteIfNeeded();
			} else {
				// :294 — the face is occupied
				if (getCovers().mBehaviours[aSide] != null) return !checkIfCoversEmptyAndDeleteIfNeeded();
				// :295-296 — the item must carry a registered cover
				ICover tCover = CoverRegistry.get(aStack);
				if (tCover == null || !allowCover(aSide, tCover) || tCover.interceptCoverPlacement(aSide, getCovers(), aPlayer)) return !checkIfCoversEmptyAndDeleteIfNeeded();
			}
		}

		// :300-304 — remove old, set, place
		if (getCovers().mBehaviours[aSide] != null) getCovers().mBehaviours[aSide].onCoverRemove(aSide, getCovers(), aPlayer);
		getCovers().set(aSide, aStack);
		if (getCovers().mBehaviours[aSide] != null) getCovers().mBehaviours[aSide].onCoverPlaced(aSide, getCovers(), aPlayer, aStack == null ? ItemStack.EMPTY : aStack);

		if (aBlockUpdate) causeBlockUpdate(); // :306

		syncCoverClientData(); // :308 updateClientData — the two-channel sync carries the covers

		return checkIfCoversEmptyAndDeleteIfNeeded(); // :310
	}

	/** Upstream :313-317 — an all-empty store dissolves back to {@code null} (acceptance: 全空回 null). */
	default boolean checkIfCoversEmptyAndDeleteIfNeeded() {
		if (getCovers() == null) return true;
		if (!getCovers().isEmpty()) return true;
		setCovers(null);
		return true;
	}

	// ---------------------------------------------------------------------------
	// validity sweep (06Covers :207-215)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :207-215 verbatim: server-side, every face the host no longer admits drops
	 * its cover (popResource at the covered face + the break sound). Rides the
	 * {@code onTickFirst} chain (06Covers :189-193). The kill+recreate lesson (remember
	 * id59) pins this to the cover path — NO onRemove is touched anywhere.
	 */
	default void checkCoverValidity() {
		BlockEntity tBE = self();
		Level tLevel = tBE.getLevel();
		if (tLevel == null || tLevel.isClientSide || !hasCovers()) return;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (allowCovers(tSide)) continue;
			ItemStack tStack = getCoverItem(tSide);
			if (!tStack.isEmpty() && setCoverItem(tSide, ItemStack.EMPTY, null, true, true)) { // :210 (aForce = T)
				dropCoverStack(tStack, tSide); // :211 ST.place
				tLevel.playSound(null, tBE.getBlockPos(), net.minecraft.sounds.SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F); // :212 SFX.MC_BREAK
			}
		}
	}

	// ---------------------------------------------------------------------------
	// right-click dispatch (06Covers :106-133)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :106-133 trimmed verbatim: the covered-face click gates (:109-117 —
	 * onCoverClickedRight then interceptClickRight consume the click), then the
	 * attachCoversFirst install branch (:118-123 — one item consumed, creative players
	 * don't pay :121). {@code false} = the caller proceeds to its own use action
	 * (upstream onBlockActivated3 :124/:126 — the oven opens its GUI). The pipe-placement
	 * re-side (:108) is cut with its subsystem; the :107 allowInteraction gate (with its
	 * {@code aPlayer == null} short-circuit) is cut with ownership (dropped in P3) — a
	 * null player simply pays for the install and skips the placed-sound hook.
	 *
	 * @param aHeldStack the stack in the clicking hand (caller passes the live hand stack)
	 * @return true when the cover machinery consumed the click.
	 */
	default boolean onCoverUse(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeldStack, float aHitX, float aHitY, float aHitZ) {
		if (!validSide(aSide)) return false; // :107 shape (the player-null/allowInteraction halves are the ownership cut)
		if (hasCovers() && getCovers().mBehaviours[aSide] != null) { // :110
			ICover tCover = getCovers().mBehaviours[aSide];
			if (tCover.onCoverClickedRight(aSide, getCovers(), aPlayer, aSide, aHitX, aHitY, aHitZ)) return true; // :111
			if (tCover.interceptClickRight(aSide, getCovers(), aPlayer, aSide, aHitX, aHitY, aHitZ)) return true; // :112 (AbstractCoverDefault = true)
		}
		// :118-123 attachCoversFirst branch — :124 (onBlockActivated3) is the caller's action
		if (!attachCoversFirst(aSide)) return false;
		if (aHeldStack != null && !aHeldStack.isEmpty() && setCoverItem(aSide, aHeldStack, aPlayer, false, true)) { // :120
			if (aPlayer == null || !aPlayer.getAbilities().instabuild) aHeldStack.shrink(1); // :121 (UT.Entities.hasInfiniteItems → instabuild)
			return true;
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// tool-click dispatch (06Covers :140-163) — the crowbar(-substitute) path
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :140-163 trimmed verbatim. The upstream {@code TOOL_crowbar} string gate
	 * (:145) gains its first-port substitute: a hoe-class stack
	 * ({@code canPerformAction(ToolActions.HOE_DIG)} — HoeItem's Forge patch returns
	 * DEFAULT_HOE_ACTIONS) OR the reserved tool id. The dismantling itself is :146-152:
	 * take the drop, setCoverItem(null, force=F), give-or-drop, onAfterCrowbar, return
	 * the tool damage. {@code 0} = not handled (the caller falls through to the host's
	 * own tool handling).
	 *
	 * @param aToolId the future tool-system id ({@link ICover#TOOL_CROWBAR}; empty in the
	 *                first port — classification runs on the stack)
	 * @return the tool damage (upstream 10000) or 0.
	 */
	default long onCoverToolClick(@Nullable String aToolId, @Nullable Entity aPlayer, @Nullable ItemStack aToolStack, byte aSide, boolean aSneaking) {
		if (!validSide(aSide) || !hasCovers()) return 0; // :143
		boolean tCrowbar = ICover.TOOL_CROWBAR.equals(aToolId)
				|| (aToolStack != null && !aToolStack.isEmpty() && aToolStack.canPerformAction(ToolActions.HOE_DIG)); // :145 substitute
		if (tCrowbar && isServerSideTE()) {
			ItemStack tStack = getCoverItem(aSide); // :146
			ICover tCover = getCovers().mBehaviours[aSide]; // :147
			if (!tStack.isEmpty() && setCoverItem(aSide, ItemStack.EMPTY, aPlayer, false, true)) { // :148
				Player tPlayer = ICover.asPlayer(aPlayer);
				if (tPlayer == null || !tPlayer.getInventory().add(tStack)) { // :149 ST.add
					dropCoverStack(tStack, aSide); // :149 ST.place
				}
				if (tCover != null) tCover.onAfterCrowbar(this); // :150
				return 10000; // :151
			}
		}
		if (getCovers().mBehaviours[aSide] != null) { // :154-159 — the tool relay (the :155-158 re-side half is cut with pipe placement)
			return getCovers().mBehaviours[aSide].onToolClick(aSide, getCovers(), aToolId == null ? "" : aToolId, aToolStack == null ? 0 : aToolStack.getMaxDamage() - aToolStack.getDamageValue(), aPlayer, aSneaking, aSide, 0.5F, 0.5F, 0.5F);
		}
		return 0; // :162
	}

	/**
	 * The cover drop placement — upstream :149/:211 {@code ST.place} at the covered face.
	 * Test seam (the offline doubles have no entity system) and future reuse.
	 */
	default void dropCoverStack(ItemStack aStack, byte aSide) {
		BlockPos tPos = self().getBlockPos().relative(Direction.from3DDataValue(aSide));
		Block.popResource(self().getLevel(), tPos, aStack);
	}

	// ---------------------------------------------------------------------------
	// host-side hooks (the upstream ITileEntityCoverable members :38-41)
	// ---------------------------------------------------------------------------

	/** Upstream sendBlockUpdateFromCover — neighbors + change marker. */
	default void sendBlockUpdateFromCover() {
		causeBlockUpdate();
	}

	/** Upstream causeBlockUpdate — neighbor notifications + the BE change marker. */
	default void causeBlockUpdate() {
		BlockEntity tBE = self();
		Level tLevel = tBE.getLevel();
		if (tLevel == null) return;
		tBE.setChanged();
		tLevel.updateNeighborsAt(tBE.getBlockPos(), tBE.getBlockState().getBlock());
	}

	/** Upstream updateCoverVisuals :258-261 — no-op base; the host may refresh client visuals. */
	default void updateCoverVisuals() {/**/}

	/** Upstream updateClientData :308 — flags the 03 sync window; the host narrows. */
	default void syncCoverClientData() {
		if (this instanceof gregtech6.tileentity.TileEntityBase03TicksAndSync tSync) tSync.updateClientData();
	}

	// ---------------------------------------------------------------------------
	// fluid intercept iteration (upstream 04Covers :368-374, restored with p5-barrel-side-rules)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream getFluidTankFillable's cover gate (:368-372): the cover on the queried face
	 * may refuse the fill. The BarrelFluidHandler wrapper calls this BEFORE its side rules
	 * (spec A — the cover gate first, then the admission rule). The side-less query (-1)
	 * carries no face, so no cover can answer it.
	 */
	default boolean interceptFluidFill(byte aSide, @Nullable FluidStack aFluidToFill) {
		if (!hasCovers() || !validSide(aSide)) return false; // :368
		ICover tCover = getCovers().mBehaviours[aSide];
		return tCover != null && tCover.interceptFluidFill(aSide, getCovers(), aSide, aFluidToFill); // :370
	}

	/** Upstream getFluidTankDrainable's cover gate (:374+) — same shape as the fill gate. */
	default boolean interceptFluidDrain(byte aSide, @Nullable FluidStack aFluidToDrain) {
		if (!hasCovers() || !validSide(aSide)) return false;
		ICover tCover = getCovers().mBehaviours[aSide];
		return tCover != null && tCover.interceptFluidDrain(aSide, getCovers(), aSide, aFluidToDrain);
	}

	/**
	 * The pump-cover direct-call seam (p5 spec F, ADR ⑦): the tank the CoverPump pushes
	 * through. {@code null} = the host has no pump-addressable tank (the pump refuses
	 * placement there, the upstream :43 {@code canTick() && instanceof IFluidHandler}
	 * gate). The barrel overrides this to hand out {@code mTank} — the pump moves fluid
	 * straight through the tank and so bypasses the BarrelFluidHandler side rules, the
	 * free reverse-output exemption (the upstream FL.move(IFluidTank, ...) direct-call
	 * shape, FL.java:845-846).
	 */
	default @Nullable FluidTankGT getCoverPumpTank() {
		return null;
	}

	// ---------------------------------------------------------------------------
	// redstone query exits (upstream 04Covers :408-441, task p9-redstone-hooks)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :409-424 (the bare-host branch is the Root :577-588 world-only form).
	 * The covered face answers through {@link ICover#getRedstoneIn} — a redstone cover
	 * OVERRIDES the world query; a bare face reads the neighbouring block directly
	 * ({@code Level.getSignal} is the {@code getIndirectPowerLevelTo} counterpart, the
	 * ADR declared deviation); the invalid side folds to the six-face maximum with the
	 * early 15 exit (:411-422, cover-aware — the Root :580-585 form is its cover-free
	 * degenerate case). A host without a level reads 0 (upstream Root :578).
	 *
	 * @param aSide the GT6 side index, or any out-of-domain byte for the six-face fold
	 */
	default byte getRedstoneIncoming(byte aSide) {
		Level tLevel = self().getLevel();
		if (tLevel == null) return 0; // upstream Root :578
		if (!validSide(aSide)) { // upstream SIDES_INVALID :411-422
			byte rRedstone = 0;
			for (byte tSide = 0; tSide < 6; tSide++) {
				rRedstone = (byte) Math.max(rRedstone, coverOrWorldRedstoneIn(tLevel, tSide));
				if (rRedstone >= 15) return 15; // :419
			}
			return rRedstone;
		}
		if (!hasCovers()) return worldRedstoneIn(tLevel, aSide); // :410 super form (Root :587)
		return coverOrWorldRedstoneIn(tLevel, aSide); // :423
	}

	/**
	 * Upstream :427-431 {@code isProvidingWeakPower} — the vanilla query hands in the
	 * side the RECEIVER sees the machine from ({@code aOppositeSide}); the actual
	 * emission face is {@link UT6#OPOS} of it. The cover on that face OVERRIDES the
	 * machine's own weak emission; no cover passes the default straight through.
	 *
	 * @param aOppositeSide    the GT6 side the querying neighbour sits on (0..5)
	 * @param aDefaultRedstone the machine's own weak emission (the Block bridge feeds
	 *                         the vanilla Block default in)
	 */
	default int getRedstoneOutWeak(byte aOppositeSide, int aDefaultRedstone) {
		return redstoneOut(aOppositeSide, aDefaultRedstone, true); // :427-431
	}

	/** Upstream :433-438 {@code isProvidingStrongPower} — same shape as the weak exit. */
	default int getRedstoneOutStrong(byte aOppositeSide, int aDefaultRedstone) {
		return redstoneOut(aOppositeSide, aDefaultRedstone, false); // :433-438
	}

	/** The :427-438 shared body — {@code aWeak} picks the cover hook (weak vs strong). */
	private int redstoneOut(byte aOppositeSide, int aDefaultRedstone, boolean aWeak) {
		if (aOppositeSide < 0 || aOppositeSide >= UT6.OPOS.length) return aDefaultRedstone;
		byte tActualSide = UT6.OPOS[aOppositeSide]; // :428/:435
		CoverData tCovers = getCovers();
		if (tCovers != null && validSide(tActualSide) && tCovers.mBehaviours[tActualSide] != null) {
			byte tDefault = bind4(aDefaultRedstone);
			return aWeak
					? tCovers.mBehaviours[tActualSide].getRedstoneOutWeak(tActualSide, tCovers, tDefault) // :429
					: tCovers.mBehaviours[tActualSide].getRedstoneOutStrong(tActualSide, tCovers, tDefault); // :436
		}
		return aDefaultRedstone; // :430/:437 — the machine emission passes through
	}

	/** The :415/:417/:423 face resolution — the cover on that face overrides the world query. */
	private byte coverOrWorldRedstoneIn(Level aLevel, byte aSide) {
		CoverData tCovers = getCovers();
		if (tCovers != null && tCovers.mBehaviours[aSide] != null) return tCovers.mBehaviours[aSide].getRedstoneIn(aSide, tCovers);
		return worldRedstoneIn(aLevel, aSide);
	}

	/** Upstream Root :587 — the neighbouring block's signal, clamped 0..15 (UT.Code.bind4). */
	private byte worldRedstoneIn(Level aLevel, byte aSide) {
		Direction tFace = Direction.from3DDataValue(aSide);
		return bind4(aLevel.getSignal(self().getBlockPos().relative(tFace), tFace));
	}

	/** Upstream UT.Code.bind4 — the 0..15 redstone scale clamp. */
	static byte bind4(int aValue) {
		return (byte) Math.max(0, Math.min(15, aValue));
	}

	// ---------------------------------------------------------------------------
	// NBT (06Covers :66-75) — the host's saveAdditional/load call these
	// ---------------------------------------------------------------------------

	/** Upstream :74 — the {@code covers} tag (NBT_COVERS, plain in-repo key form). */
	public static final String NBT_COVERS = "covers";

	default void writeCoversToNBT(CompoundTag aNBT) {
		if (hasCovers()) aNBT.put(NBT_COVERS, getCovers().writeToNBT(new CompoundTag(), true));
	}

	/** Upstream :68. */
	default void readCoversFromNBT(CompoundTag aNBT) {
		if (aNBT.contains(NBT_COVERS, net.minecraft.nbt.Tag.TAG_COMPOUND)) setCovers(CoverRegistry.coverdata(this, aNBT.getCompound(NBT_COVERS)));
	}
}
