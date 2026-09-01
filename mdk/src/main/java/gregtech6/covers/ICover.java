package gregtech6.covers;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

/**
 * The cover behaviour contract — 1.20.1 port of gregapi/cover/ICover.java (226 lines)
 * trimmed to the first-cover surface (task p4-cover-core ②, ADR
 * 2026-08-30-p4-cover-route). GT6 covers are registry-shared SINGLETONS; all per-face
 * state lives in the {@link CoverData} parallel arrays, unlike the GTCEu instantiated
 * CoverBehavior model (CoverBehavior.java:48) which the ADR rejected.
 *
 * <p>Ported method groups (upstream line anchors):
 * <ul>
 * <li>lifecycle: {@link #onCoverLoaded} :45, {@link #onCoverPlaced} :51,
 *     {@link #onCoverRemove} :57, {@link #interceptCoverPlacement} :64,
 *     {@link #interceptCoverRemoval} :70;</li>
 * <li>the four clicks :97-118 and the tick pair :85-90;</li>
 * <li>{@link #onToolClick} :123 (tool-click relay) and {@link #onAfterCrowbar} :128;</li>
 * <li>{@link #onBlockUpdate} :133, {@link #onStoppedUpdate} :138;</li>
 * <li>{@link #getCoverItem} :153 (the removal drop);</li>
 * <li>the opaque/solid/decorative flag family :158-188 incl.
 *     {@link #needsVisualsSaved} :188 (the CoverData writeToNBT :75 gate);</li>
 * <li>the texture triple :194-196 (surface/attachment/holder) — the upstream
 *     {@code ITexture} return becomes the atlas sprite id the plate renderer stitches
 *     into the block atlas.</li>
 * <li>the item-intercept family :209-216 (task p10-cover-item-intercept, ADR
 *     2026-09-01-p10-cover-item-intercept — the "pooled D card" cut is lifted, the
 *     freeze-face expansion is that ADR's single sanctioned one): the intercept pair
 *     :209-210, the override triple :211-213 and the answering triple :214-216.</li>
 * </ul>
 *
 * <p>Side order: {@code byte} face indices follow the GT6 side order, which equals
 * {@link Direction#get3DDataValue()} (the chest/oven facing precedent).
 *
 * <p>The redstone hook triple :190-192 is RESTORED with the cover redstone framework
 * (task p9-redstone-hooks, ADR 2026-09-01-p9-redstone-hooks — the P4 spec ② pool cut is
 * lifted verbatim): {@link #getRedstoneIn} is the covered face's incoming read,
 * {@link #getRedstoneOutWeak}/{@link #getRedstoneOutStrong} the emission pair the host
 * exits hand the machine default to. The item-intercept family :209-216 is RESTORED
 * with the side-aware item capability framework (task p10-cover-item-intercept, ADR
 * 2026-09-01-p10-cover-item-intercept — the "pooled D card" javadoc cut is lifted;
 * note the older ":320-343" line anchor was a transcription error: upstream ICover is
 * 226 lines total, the item family is :209-216, while :320-343 is the HOST-side
 * dispatch in TileEntityBase04Covers). The GUI hook pair :198-199 stays UNPORTED (the
 * upstream dead-code red line holds: no host dispatches them and no GUI cover exists).
 *
 * <p>Cut to the pool (spec ②): the GUI hook pair :198-199, the fluid
 * override family :220-225 and the connector hooks :75-80/:183. The two fluid
 * intercept hooks (:218-219) are RESTORED with the pump cover (task p5-barrel-side-rules
 * spec F): {@link #interceptFluidFill}/{@link #interceptFluidDrain} are the one-way
 * gate the CoverPump mounts on its covered face; the rest of the fluid family
 * (getFluidTank*Override/defaults, :220-225) stays pooled. The five consuming covers
 * (Shutter/Conveyor/RobotArm/FilterItem/RetrieverItem) stay pooled with the framework
 * card (the B-card precedent: framework without a real consumer).
 */
public interface ICover {

	/** Upstream CS.TOOL_crowbar — the tool id the dismantling path keys on. */
	String TOOL_CROWBAR = "crowbar";

	/** Upstream CS.TOOL_screwdriver — the tool id the pump cover's direction toggle keys on (p5 spec C). */
	String TOOL_SCREWDRIVER = "screwdriver";

	/** Called when the cover got successfully loaded (upstream :45). */
	void onCoverLoaded(byte aCoverSide, CoverData aData);

	/**
	 * Called when the cover got successfully placed (upstream :51).
	 *
	 * @param aPlayer CAN be null!
	 */
	void onCoverPlaced(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover);

	/**
	 * Called when the cover got successfully removed (upstream :57).
	 *
	 * @param aPlayer CAN be null!
	 */
	void onCoverRemove(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer);

	/**
	 * Upstream :64.
	 *
	 * @return true to prevent that the cover gets placed.
	 */
	boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer);

	/**
	 * Upstream :70.
	 *
	 * @return true to prevent that the cover gets removed.
	 */
	boolean interceptCoverRemoval(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer);

	/** Upstream :85 — before the host ticks its own business. */
	void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate);

	/** Upstream :90 — after the host ticked its own business. */
	void onTickPost(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate);

	/** Upstream :97 — @return true to intercept the click and cause the click animation. */
	boolean onCoverClickedLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ);

	/** Upstream :104 — @return true to intercept the click without the click animation. */
	boolean interceptClickLeft(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ);

	/** Upstream :111 — @return true to intercept the click and cause the click animation. */
	boolean onCoverClickedRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ);

	/** Upstream :118 — @return true to intercept the click without the click animation. */
	boolean interceptClickRight(byte aCoverSide, CoverData aData, Entity aPlayer, byte aSideClicked, float aHitX, float aHitY, float aHitZ);

	/**
	 * Upstream :123 — the tool click relay. {@code aToolId} is the future GT tool-system
	 * id (e.g. {@link #TOOL_CROWBAR}); the first port classifies hoe-class stacks as
	 * crowbar substitutes at the {@link ICoverableTE#onCoverToolClick} entry instead
	 * (spec ④, the formal tool-type system stays pooled).
	 */
	long onToolClick(byte aCoverSide, CoverData aData, String aToolId, long aRemainingDurability, Entity aPlayer, boolean aSneaking, byte aSideClicked, float aHitX, float aHitY, float aHitZ);

	/** Upstream :128 — after this cover was removed via the crowbar(-substitute) path. */
	void onAfterCrowbar(ICoverableTE aTileEntity);

	/** Upstream :133. */
	void onBlockUpdate(byte aCoverSide, CoverData aData);

	/** Upstream :138 — a controller cover stopped the covers on this block. */
	void onStoppedUpdate(byte aCoverSide, CoverData aData, boolean aStopped);

	/** Upstream :153 — the item that drops when this cover is removed. */
	ItemStack getCoverItem(byte aCoverSide, CoverData aData);

	/** Upstream :158. */
	boolean isSolid(byte aCoverSide, CoverData aData);

	/** Upstream :163. */
	boolean isOpaque(byte aCoverSide, CoverData aData);

	/** Upstream :168. */
	boolean isFullTexture(byte aCoverSide, CoverData aData);

	/** Upstream :173 — merely decorative covers skip most gameplay interactions. */
	boolean isDecorative(byte aCoverSide, CoverData aData);

	/** Upstream :178. */
	boolean isSealable(byte aCoverSide, CoverData aData);

	/** Upstream :188 — the CoverData writeToNBT :75 visual-persistence gate. */
	boolean needsVisualsSaved(byte aCoverSide, CoverData aData);

	/**
	 * Upstream :190 — the incoming redstone read on the face carrying this cover. The
	 * default (AbstractCoverDefault :78) passes the neighbouring block's signal straight
	 * through, so a plain cover never blocks redstone; a redstone cover (the emitter
	 * family, C card) overrides the world query. The host dispatches through
	 * {@link ICoverableTE#getRedstoneIncoming}.
	 */
	byte getRedstoneIn(byte aCoverSide, CoverData aData);

	/**
	 * Upstream :191 — the weak redstone emission of the face carrying this cover.
	 * {@code aDefaultRedstone} is the machine's own weak emission on the queried face:
	 * covers that do not emit just return it (the host exits feed the machine value in,
	 * upstream :429/:436).
	 */
	byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone);

	/** Upstream :192 — the strong (comparator-grade) emission, same contract as the weak one. */
	byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone);

	/** Upstream :194 — the atlas sprite id painted on the cover plate's outer face (null = no plate). */
	@Nullable
	ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData);

	/** Upstream :195 — the sprite for the plate face attaching toward the neighbouring block. */
	@Nullable
	ResourceLocation getCoverTextureAttachment(byte aCoverSide, CoverData aData, byte aTextureSide);

	/** Upstream :196 — the sprite for the holder face visible on adjacent covered sides. */
	@Nullable
	ResourceLocation getCoverTextureHolder(byte aCoverSide, CoverData aData, byte aTextureSide);

	/**
	 * Upstream :209 — the insert interceptor. @return true to REFUSE the insert through
	 * the face carrying this cover (the first gate the host dispatch runs, before the
	 * override/answering pair).
	 *
	 * @param aSlot  the target inventory slot
	 * @param aStack the stack offered (never null — {@link ItemStack#EMPTY} is the
	 *               1.20.1 form of the upstream null stack)
	 * @param aSide  the face the insert was requested on
	 */
	boolean interceptItemInsert(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide);

	/** Upstream :210 — the extract interceptor, same contract as {@link #interceptItemInsert}. */
	boolean interceptItemExtract(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide);

	/**
	 * Upstream :211 — the accessible-slots override claim. @return true to make the
	 * face's {@link #getAccessibleSlotsFromSide} answer (instead of passing the host
	 * default through).
	 */
	boolean getAccessibleSlotsFromSideOverride(byte aCoverSide, CoverData aData, byte aSide);

	/**
	 * Upstream :212 — the insert override claim. @return true to make the face's
	 * {@link #canInsertItem} answer (ANDed with the host admission, upstream :353).
	 */
	boolean canInsertItemOverride(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide);

	/** Upstream :213 — the extract override claim, same contract as {@link #canInsertItemOverride}. */
	boolean canExtractItemOverride(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide);

	/**
	 * Upstream :214 — the accessible slots answer. Only consulted when
	 * {@link #getAccessibleSlotsFromSideOverride} returned true; {@code aDefault} is the
	 * host's own slot array, pass it through unchanged when the cover does not narrow.
	 */
	int[] getAccessibleSlotsFromSide(byte aCoverSide, CoverData aData, byte aSide, int[] aDefault);

	/**
	 * Upstream :215 — the insert answer. Only consulted when
	 * {@link #canInsertItemOverride} returned true; the result is ANDed with the host's
	 * own admission (upstream :353 {@code && canInsertItem2}).
	 */
	boolean canInsertItem(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide);

	/** Upstream :216 — the extract answer, same contract as {@link #canInsertItem}. */
	boolean canExtractItem(byte aCoverSide, CoverData aData, int aSlot, ItemStack aStack, byte aSide);

	/**
	 * Upstream :218 — the restored fluid intercept. {@code aCoverSide} is the face carrying
	 * this cover, {@code aSide} the face the fill was requested on; they only differ for
	 * callers that route one face's request across another cover. @return true to prevent
	 * the fill (the CoverPump out-face refuses incoming fluid, CoverPump.java:92).
	 */
	boolean interceptFluidFill(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToFill);

	/**
	 * Upstream :219 — @return true to prevent the drain out of that face (the CoverPump
	 * in-face refuses outgoing fluid, CoverPump.java:93).
	 */
	boolean interceptFluidDrain(byte aCoverSide, CoverData aData, byte aSide, @Nullable FluidStack aFluidToDrain);

	/**
	 * Convenience for player-type narrowing (upstream callers pass {@code Entity} and
	 * the covers only ever use the inventory/abilities surface).
	 */
	static @Nullable Player asPlayer(@Nullable Entity aEntity) {
		return aEntity instanceof Player tPlayer ? tPlayer : null;
	}
}
