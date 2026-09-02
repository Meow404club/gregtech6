package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * 1.20.1 counterpart of gregapi/tileentity/multiblocks/ITileEntityMultiBlockController.java
 * (task p4-multiblock-framework, W3).
 *
 * <p>The three structural queries (:41-43) carry over verbatim. The fourth method,
 * {@code onToolClickMultiBlock} (:44), is the 1.7.10 tool-click dispatch seam — there is no
 * tool system yet, so the builder-wand entry is the abstract
 * {@link TileEntityBase10MultiBlockBase#checkStructure2(BlockPos, Player, Container)}
 * hook, callable directly (the task card ③ ruling: the acceptance command drives that path
 * with a stocked FakePlayer inventory to simulate the wand).
 *
 * <p>The {@code ITileEntityUnloadable}/{@code IHasWorldAndCoords} super-interfaces fold away:
 * every 1.20.1 controller is a BlockEntity ({@link #Util} casts), so {@code isDead}/world/coords
 * come from {@link BlockEntity} itself.
 */
public interface ITileEntityMultiBlockController {

	/** Upstream :41. */
	boolean isInsideStructure(int aX, int aY, int aZ);

	/** Upstream :42. */
	boolean checkStructure(boolean aForceReset);

	/** Upstream :43. */
	void onStructureChange();

	/**
	 * The declared structure pattern (task p12-ghost-pattern-api) — read-only display
	 * data for client-side consumers (the ghost preview). Default null = no declaration:
	 * existence-probe machines (LightningRod/BedrockDrill census class) keep their
	 * hand-written checkStructure2 and never bind one. NEVER consulted by
	 * {@link TileEntityBase10MultiBlockBase#checkStructure} — the server check stays
	 * hand-written per machine (the pattern derives from that check, not vice versa, and
	 * checkStructure2 is never run to build one — it writes the world).
	 */
	@Nullable
	default GTMultiBlockPattern getStructurePattern() {
		return null;
	}

	/**
	 * Upstream Util (:46-85) — the structure-filling core shared by every multiblock.
	 *
	 * <p>Port substitutions: the 1.7.10 MTE registry pair (aRegistryID/aRegistryMeta, :47)
	 * becomes the part {@link Block} identity — the GT6 "one part MTE per brick type" maps to
	 * one Block instance per part type (ADR-P3-1 shape, the MultiTileEntityRegistry lookup is
	 * gone). {@code ChunkCoordinates} becomes {@link BlockPos}.
	 */
	class Util {

		/**
		 * Upstream :47-77 verbatim, in four beats:
		 * <ol>
		 * <li>the controller's own cell is always a pass (:48-49);</li>
		 * <li>the builder-wand auto-place (:51-68): when an inventory (or player) is present
		 *     and the target is within ±1 of the clicked cell (or the wand was clicked on the
		 *     controller itself, aClickedAt == null in the checkStructure pass), replace an
		 *     easy-to-replace block with the part block — creative players place for free,
		 *     everyone else consumes one matching item from the inventory (scanned
		 *     back-to-front, :58);</li>
		 * <li>the occupation arbitration (:70-75): a part of the requested block type already
		 *     claimed by ANOTHER controller that still contains this cell is a failure;
		 *     otherwise the part gets (re)claimed by this controller;</li>
		 * <li>any other cell content is a failure (:76).</li>
		 * </ol>
		 *
		 * <p>Verbatim quirk kept: the {@code tTileEntity} reference is captured once (:48) —
		 * when the wand placement in beat 2 filled the cell, the beat-3 check still sees the
		 * STALE pre-placement reference and fails for this call. The wand is therefore a
		 * two-pass affair (place here, link on the caller's follow-up
		 * {@link TileEntityBase10MultiBlockBase#checkStructure} pass) — exactly the upstream
		 * onToolClick2 sequence :132-133/:143-144 relies on.
		 *
		 * <p>Permission chain (task card ③ declared simplification): upstream
		 * {@code WD.easyRep} (WD.java:688 — air/replaceable-plant family) is the vanilla
		 * {@code isAir() || canBeReplaced()} pair; upstream {@code UT.Entities.canEdit}
		 * (UT.java:3159 — non-players auto-approve, players canPlayerEdit) is ruled as
		 * creative-or-OP(2) (the first-generation tool-less world only reaches this path
		 * with a null player or a creative one). The itemized builder wand itself stays in
		 * the pool; this method IS the wand semantics.
		 */
		public static boolean checkAndSetTarget(ITileEntityMultiBlockController aController, int aX, int aY, int aZ,
				Block aPartBlock, int aDesign, int aMode,
				@Nullable BlockPos aClickedAt, @Nullable Player aPlayer, @Nullable Container aInventory) {
			Level tLevel = ((BlockEntity) aController).getLevel();
			if (tLevel == null) return false;

			BlockEntity tTileEntity = tLevel.getBlockEntity(new BlockPos(aX, aY, aZ));
			if (tTileEntity == aController) return true; // :49

			// :51-68 — the wand auto-place, gated to the clicked neighbourhood
			if ((aInventory != null || aPlayer != null)
					&& (aClickedAt == null || (Math.abs(aX - aClickedAt.getX()) < 2 && Math.abs(aY - aClickedAt.getY()) < 2 && Math.abs(aZ - aClickedAt.getZ()) < 2))) {
				ItemStack aStack = new ItemStack(aPartBlock); // ST.make(registry, 1, meta) :52
				if (easyRep(tLevel, aX, aY, aZ) && canEdit(aPlayer)) {
					if (aInventory == null || hasInfiniteItems(aPlayer)) {
						if (tLevel.setBlock(new BlockPos(aX, aY, aZ), aPartBlock.defaultBlockState(), 3)) {
							// UT.Sounds SFX.MC_XP :56 — no sound surface yet, cut
						}
					} else {
						for (int i = aInventory.getContainerSize() - 1; i >= 0; i--) { // :58 back-to-front scan
							ItemStack tStack = aInventory.getItem(i);
							if (ItemStack.isSameItemSameTags(aStack, tStack) && useOne(aPlayer, tStack)) {
								tLevel.setBlock(new BlockPos(aX, aY, aZ), aPartBlock.defaultBlockState(), 3); // WD.set :61
								break;
							}
						}
					}
				}
			}

			// :70-75 — the occupation arbitration (tTileEntity may be the stale pre-placement
			// reference when beat 2 just filled the cell; the caller's next pass links it)
			if (tTileEntity instanceof MultiBlockPartBlockEntity tPart && tLevel.getBlockState(new BlockPos(aX, aY, aZ)).is(aPartBlock)) {
				ITileEntityMultiBlockController tTarget = tPart.getTarget(false);
				if (tTarget != aController && tTarget != null && tTarget.isInsideStructure(aX, aY, aZ)) return false;
				tPart.setTarget(aController, aDesign, aMode);
				return true;
			}
			return false; // :76
		}

		/** Upstream :79-81 — coordinates are controller-relative. */
		public static boolean checkAndSetTargetOffset(ITileEntityMultiBlockController aController, int aOffsetX, int aOffsetY, int aOffsetZ,
				Block aPartBlock, int aDesign, int aMode,
				@Nullable BlockPos aClickedAt, @Nullable Player aPlayer, @Nullable Container aInventory) {
			BlockPos tPos = ((BlockEntity) aController).getBlockPos();
			return checkAndSetTarget(aController, aOffsetX + tPos.getX(), aOffsetY + tPos.getY(), aOffsetZ + tPos.getZ(),
					aPartBlock, aDesign, aMode, aClickedAt, aPlayer, aInventory);
		}

		/** WD.easyRep (WD.java:687-688): the air/replaceable family — vanilla isAir || canBeReplaced. */
		private static boolean easyRep(Level aLevel, int aX, int aY, int aZ) {
			BlockState tState = aLevel.getBlockState(new BlockPos(aX, aY, aZ));
			return tState.isAir() || tState.canBeReplaced();
		}

		/** UT.Entities.canEdit (UT.java:3159) ruled creative-or-OP(2); non-players auto-approve (null). */
		private static boolean canEdit(@Nullable Player aPlayer) {
			return aPlayer == null || aPlayer.isCreative() || aPlayer.hasPermissions(2);
		}

		/** UT.Entities.hasInfiniteItems (UT.java:3187). */
		private static boolean hasInfiniteItems(@Nullable Player aPlayer) {
			return aPlayer != null && aPlayer.isCreative();
		}

		/**
		 * ST.use(player, T, T, stack, 1) (ST.java:304-319): infinite-items players consume
		 * nothing, everyone else shrinks the stack in place; the container-item handout and the
		 * PlayerDestroyItemEvent (players branch :315-318) are cut with the tool surface.
		 */
		private static boolean useOne(@Nullable Player aPlayer, ItemStack aStack) {
			if (hasInfiniteItems(aPlayer)) return true;
			if (aStack.isEmpty() || aStack.getCount() < 1) return false;
			aStack.shrink(1);
			return true;
		}
	}
}
