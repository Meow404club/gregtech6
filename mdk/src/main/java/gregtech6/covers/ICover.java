package gregtech6.covers;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
 * </ul>
 *
 * <p>Side order: {@code byte} face indices follow the GT6 side order, which equals
 * {@link Direction#get3DDataValue()} (the chest/oven facing precedent).
 *
 * <p>Cut to the pool (spec ②): the logistics/fluid intercept family :209-225, the
 * redstone hooks :190-192, the GUI hooks :198-199 (zero-GUI: the negative-GUIID
 * openCoverGUI path is dead code upstream, R4-3), the bounds/collision family
 * :201-207, onWalkOver :148, addToolTips :143 and the connector hooks :75-80/:183.
 */
public interface ICover {

	/** Upstream CS.TOOL_crowbar — the tool id the dismantling path keys on. */
	String TOOL_CROWBAR = "crowbar";

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
	 * Convenience for player-type narrowing (upstream callers pass {@code Entity} and
	 * the covers only ever use the inventory/abilities surface).
	 */
	static @Nullable Player asPlayer(@Nullable Entity aEntity) {
		return aEntity instanceof Player tPlayer ? tPlayer : null;
	}
}
