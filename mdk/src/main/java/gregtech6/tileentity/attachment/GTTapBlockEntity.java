package gregtech6.tileentity.attachment;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.FluidUtil;

import gregtech6.fluid.FluidTankGT;

/**
 * 1.20.1 counterpart of gregtech/tileentity/tools/MultiTileEntityFluidTap.java
 * (:77-176 onBlockActivated3, task p12-tap-funnel-attachment spec ②) — the wall tap:
 * right-clicking DRAWS from the fluid container it is mounted on. The activation is a
 * strict priority chain, translated in order; the first branch that acts consumes the
 * click:
 *
 * <ol>
 * <li>VOIDING held item → the source drains to nothing (MultiTileEntityFluidTap.java
 *     :82-86, {@code GarbageGT.trash}); the upstream list is the cross-mod void-pipe
 *     registry — the port ships an EMPTY {@link #VOIDING_ITEMS} list with the same
 *     membership semantics (no ported item is voiding; declared minimal like the acid
 *     list);</li>
 * <li>SIMULATE first (:87-88): the source is probed with
 *     {@code tapDrain(side, Integer.MAX_VALUE, doDrain=false)} and the transfer is
 *     refused for gases and, on a non-acid-proof tap, acids — the upstream pair
 *     {@code !FL.gas(aFluid, T) && aFluid.amount > 0 && (mAcidProof || !FL.acid(aFluid))};</li>
 * <li>EMPTY HAND (:89-162): the tap looks DOWN:
 *     <ul>
 *     <li>water cauldron below → fill it by the 334/667/1000 L tier table (:92-109,
 *         the {@link #cauldronFillPlan} port; upstream keyed the vanilla 1.7.10
 *         BlockCauldron meta 0-3, 1.20.1 keys {@code water_cauldron[level=1-3]} with the
 *         empty cauldron as level 0);</li>
 *     <li>{@link TapFillable} below → the tap-to-tap chain (:110-118): the target is
 *         offered a bounded portion (lava 1000, everything else 250 — upstream :113
 *         resolves a per-material amount from {@code OreDictMaterial.FLUID_MAP}, a
 *         dataset bridge the port has not built, DECLARED deviation) and the source is
 *         drained by exactly what the target accepted, executed in one step;</li>
 *     <li>(the sandwich-plate branch :119-130 is CUT — {@code MultiTileEntitySandwich}
 *         is not ported, the food-family pool card owns it);</li>
 *     <li>XP / mob fluids (:132-160) → spawn a vanilla ExperienceOrb. The upstream
 *         OpenBlocks ratio helper is the declared cut (MD.OB never loaded in the port);
 *         the fallback default-rate forms are the verbatim port: XP {@code min(50, L/20)}
 *         draining {@code 20 L per point}, mob essence {@code min(50, L*3/200)} draining
 *         {@code 200/3 L per point}. The player XP-level cap of :135 rides the dropped
 *         OpenBlocks branch — the port always spawns at the default rate (the empty-hand
 *         command channel has no player, DECLARED deviation);</li>
 *     <li>anything else → the click is consumed with no effect (:162).</li>
 *     </ul></li>
 * <li>HELD FLUID CONTAINER (:164-171): the container is filled from the probed stack
 *     (executed), the source pays EXACTLY what landed, the spent container item is
 *     consumed and the filled container given back (upstream {@code aStack.stackSize--}
 *     + {@code ST.give(aPlayer, tStack)}; the 1.20.1 container identity is the
 *     {@link IFluidHandlerItem#getContainer()} after the fill).</li>
 * </ol>
 *
 * <p>The interaction target resolution and the adjacency are the shared
 * {@link GTAttachmentSmallBlockEntity#adjacent()} seam (overridable for the offline
 * tests, the crank {@code setAdjacencyOverride} precedent). {@link #activate} runs the
 * SERVER branch only (upstream {@code isServerSide()}); the block {@code use} supplies
 * the player and the clicked side. A null player is the RCON acceptance channel
 * ({@code /gt6tank tap <pos>} — the deterministic counterfactual of "an empty-handed
 * player clicks": no held item, no XP cap; DECLARED deviation, the card's acceptance
 * (b)).
 */
public class GTTapBlockEntity extends GTAttachmentSmallBlockEntity {

	/**
	 * Upstream {@code ItemsGT.VOIDING_ITEMS} (CS.java:1615, populated by the Ender
	 * Garbage Bin row Loader_MultiTileEntities.java:2232 and cross-mod void pipes) —
	 * the port ships the EMPTY list; membership = item identity, test-injectable.
	 */
	public static final Set<Item> VOIDING_ITEMS = new HashSet<>();

	/** The upstream :113 portion table: lava fills 1000, every other fluid 250 (the per-material bridge is the declared cut). */
	public static final long TAP_TO_TAP_LAVA = 1000;
	public static final long TAP_TO_TAP_DEFAULT = 250;

	/** The vanilla bucket is 1000 L (the cauldron full tier, the :95 drain amount). */
	public static final long CAULDRON_FULL = 1000;
	public static final long CAULDRON_TWO_TIERS = 667;
	public static final long CAULDRON_ONE_TIER = 334;

	public GTTapBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public GTTapBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : gregtech6.registry.GTBlockEntities.TAP_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "tap"; // BET registry path mirrors it (GTBlockEntities.TAP_BE)
	}

	/**
	 * The tap interface the mounted container implements (upstream
	 * {@code ITileEntityTapAccessible}, ITileEntityTapAccessible.java:27-30): one method —
	 * {@code aDoDrain=false} is the probe, {@code true} the executed withdrawal. The
	 * {@code nozzleDrain} half of the upstream pair rides the Nozzle pool card.
	 */
	public interface TapAccessible {
		@Nullable
		FluidStack tapDrain(byte aSide, int aMaxDrain, boolean aDoDrain);
	}

	/**
	 * The tap-fill interface of a container a tap can pour into (upstream
	 * {@code ITileEntityTapFillable}, ITileEntityTapFillable.java:27-29) — the tap-to-tap
	 * chain target. The port's barrel family implements {@link TapAccessible} and the
	 * funnel face only; the fillable consumers (upstream the metal tank multiblocks) are
	 * the tank-family pool.
	 */
	public interface TapFillable {
		int tapFill(byte aSide, FluidStack aFluid, boolean aDoFill);
	}

	// ---------------------------------------------------------------------------
	// the activation chain (upstream onBlockActivated3 :77-176)
	// ---------------------------------------------------------------------------

	/**
	 * The server-side activation, the whole :77-176 chain. Returns a human-readable
	 * action report for the RCON acceptance channel (the player path ignores it).
	 *
	 * @param aPlayer null = the /gt6tank tap acceptance channel (empty-hand semantics)
	 * @param aHeld   the held stack (the block-use path passes the main hand); null or
	 *                empty = the empty-hand branches
	 */
	@Override
	protected String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld) {
		if (!isServerSide()) return "client side";
		BlockEntity tTarget = adjacent();
		if (!(tTarget instanceof TapAccessible tSource)) return "no tap-accessible container on the facing side";
		byte tSide = sideFacingBack(mFacing);

		// :82-86 — VOIDING held item: everything drains and is trashed
		if (aHeld != null && !aHeld.isEmpty() && VOIDING_ITEMS.contains(aHeld.getItem())) {
			FluidStack tAll = tSource.tapDrain(tSide, Integer.MAX_VALUE, true);
			long tTrashed = tAll == null ? 0 : tAll.getAmount();
			return "voided " + tTrashed + " L";
		}

		// :87-88 — probe the source (simulate), refuse gases and (non-acid-proof) acids
		FluidStack aFluid = tSource.tapDrain(tSide, Integer.MAX_VALUE, false);
		if (aFluid == null || aFluid.getAmount() <= 0) return "nothing to draw";
		if (isGas(aFluid)) return "refused a gas";
		if (!isAcidProof() && isAcid(aFluid)) return "refused an acid (tap is not acid proof)";

		// :89 — EMPTY HAND branches
		if (aHeld == null || aHeld.isEmpty()) {
			return activateEmptyHand(tSource, tSide, aFluid, aPlayer);
		}

		// :164-171 — held container: fill it, the source pays what landed
		return activateHeldContainer(tSource, tSide, aFluid, aPlayer, aHeld);
	}

	/** The :89-162 empty-hand half, split so the offline tests can drive it with fakes. */
	protected String activateEmptyHand(TapAccessible aSource, byte aSide, FluidStack aFluid, @Nullable Player aPlayer) {
		// :90-109 — the cauldron below
		if (hasLevel() && !hasAdjacentBelowBE()) {
			String tCauldron = fillCauldronBelow(aSource, aSide, aFluid);
			if (tCauldron != null) return tCauldron;
		}
		// :110-118 — the tap-to-tap chain
		BlockEntity tBelow = hasLevel() ? getLevel().getBlockEntity(getBlockPos().below()) : null;
		if (tBelow instanceof TapFillable tFillable) {
			return tapToTap(aSource, aSide, aFluid, tFillable);
		}
		// (:119-130 the sandwich plate is the cut food-family pool item)
		// :132-160 — XP / mob fluids become experience orbs
		String tXp = spawnXpOrb(aSource, aSide, aFluid);
		if (tXp != null) return tXp;
		// :162 — nothing left to check for empty hands
		return "consumed (no effect)";
	}

	/** The :91 guard — the cauldron branch runs only when the cell below has NO BlockEntity. */
	private boolean hasAdjacentBelowBE() {
		return getLevel().getBlockEntity(getBlockPos().below()) != null;
	}

	/**
	 * The :92-109 cauldron fill, live half: the tier plan comes from
	 * {@link #cauldronFillPlan} (the offline-tested pure table), the source pays the
	 * tier amount executed, the cauldron level climbs. Water-only, like upstream
	 * ({@code FL.water(aFluid)} — the port's strict same-fluid check over vanilla water).
	 */
	@Nullable
	private String fillCauldronBelow(TapAccessible aSource, byte aSide, FluidStack aFluid) {
		BlockPos tBelow = getBlockPos().below();
		BlockState tState = getLevel().getBlockState(tBelow);
		Block tBlock = tState.getBlock();
		if (tBlock != Blocks.CAULDRON && tBlock != Blocks.WATER_CAULDRON) return null;
		if (!isWater(aFluid)) return "refused non-water for the cauldron";
		int tLevel = tBlock == Blocks.WATER_CAULDRON ? tState.getValue(LayeredCauldronBlock.LEVEL) : 0;
		int[] tPlan = cauldronFillPlan(tLevel, true, aFluid.getAmount());
		if (tPlan == null) return "cauldron full or too little water";
		if (aSource.tapDrain(aSide, tPlan[0], true) == null) return "source refused the executed drain";
		getLevel().setBlock(tBelow,
				Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, tLevel + tPlan[1]),
				Block.UPDATE_ALL);
		return "cauldron level " + tLevel + " -> " + (tLevel + tPlan[1]) + ", drained " + tPlan[0] + " L";
	}

	/**
	 * The :94-104 tier table verbatim over the 1.20.1 cauldron levels — the offline pure
	 * seam. Upstream keyed meta 0-3 with the branches {@code >=1000 && tMeta<=0 → +3
	 * (drain 1000)}, {@code >=667 && tMeta<=1 → +2 (drain 667)}, {@code tMeta<=2 → +1
	 * (drain 334)}; a full cauldron or a non-water source is no plan. Returns
	 * {@code {drainAmount, tierDelta}} or null.
	 */
	public static int[] cauldronFillPlan(int aLevel, boolean aWater, long aAvailable) {
		if (!aWater || aLevel >= 3 || aAvailable < CAULDRON_ONE_TIER) return null;
		if (aAvailable >= CAULDRON_FULL && aLevel <= 0) return new int[] {(int)CAULDRON_FULL, 3};
		if (aAvailable >= CAULDRON_TWO_TIERS && aLevel <= 1) return new int[] {(int)CAULDRON_TWO_TIERS, 2};
		return new int[] {(int)CAULDRON_ONE_TIER, 1};
	}

	/** Upstream FL.water — the strict same-fluid check over vanilla water (WaterFluid.isSame covers the flowing state, the barrel isLava form). */
	public static boolean isWater(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && aFluid.getFluid().isSame(Fluids.WATER);
	}

	/**
	 * The :110-118 tap-to-tap chain: offer the target a bounded portion (executed), then
	 * drain the source by EXACTLY what the target accepted — upstream :114 nests the
	 * calls {@code tapDrain(side, bindInt(target.tapFill(side, fluid, T)), T)}. The
	 * target sits BELOW the tap, so the side of it that faces the tap is always UP
	 * (upstream {@code tDelegator2.mSideOfTileEntity} of the SIDE_BOTTOM delegator).
	 */
	protected String tapToTap(TapAccessible aSource, byte aSide, FluidStack aFluid, TapFillable aTarget) {
		long tPortion = tapToTapPortion(aFluid);
		FluidStack tOffer = aFluid.copy();
		tOffer.setAmount(FluidTankGT.bindInt(tPortion));
		// the upstream one-step nesting: the fillable target executes first, the source pays its number
		byte tTargetSide = (byte)Direction.UP.get3DDataValue();
		int tAccepted = aTarget.tapFill(tTargetSide, tOffer, true);
		if (tAccepted > 0) aSource.tapDrain(aSide, tAccepted, true);
		return tAccepted > 0 ? "tap-to-tap moved " + tAccepted + " L" : "the target refused the pour";
	}

	/**
	 * The :113 portion bound — lava 1000, everything else 250. The upstream middle branch
	 * ({@code tMaterial.mAmount} per non-water fluid) is the OreDictMaterial.FLUID_MAP
	 * dataset bridge cut (DECLARED deviation: every non-lava fluid rides the :113
	 * else-form 250).
	 */
	public static long tapToTapPortion(FluidStack aFluid) {
		return isLava(aFluid) ? TAP_TO_TAP_LAVA : TAP_TO_TAP_DEFAULT;
	}

	/** Upstream FL.lava — the same-fluid check that also covers the flowing state (the barrel isLava form). */
	public static boolean isLava(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && aFluid.getFluid().isSame(Fluids.LAVA);
	}

	/**
	 * The :132-160 XP half (the OpenBlocks branch is the declared cut): the liquid-XP
	 * default rate {@code min(50, L/20)} draining {@code 20 L per point} (:145-147) and
	 * the mob-essence rate {@code min(50, L*3/200)} draining {@code (tXP*200)/3} (:154-156)
	 * — each spawns a vanilla ExperienceOrb at the tap. Returns null when the fluid is
	 * neither (the chain falls through).
	 */
	@Nullable
	protected String spawnXpOrb(TapAccessible aSource, byte aSide, FluidStack aFluid) {
		if (!hasLevel() || !(getLevel() instanceof ServerLevel tLevel)) return null;
		int tXp = -1;
		int tDrain = 0;
		if (isXpFluid(aFluid)) {
			tXp = Math.min(50, aFluid.getAmount() / 20); // :145
			tDrain = tXp * 20; // :147
		} else if (isMobFluid(aFluid)) {
			tXp = Math.min(50, (aFluid.getAmount() * 3) / 200); // :154
			tDrain = (tXp * 200) / 3; // :156
		}
		if (tXp <= 0) return null;
		aSource.tapDrain(aSide, tDrain, true);
		Vec3 tCenter = Vec3.atCenterOf(getBlockPos());
		net.minecraft.world.entity.ExperienceOrb.award(tLevel, tCenter.add(0, -0.3, 0), tXp); // the :139/:148/:157 EntityXPOrb spawn, the 1.20.1 award form
		return "spawned an XP orb worth " + tXp + " for " + tDrain + " L";
	}

	/**
	 * The :164-171 held-container half: the container is filled EXECUTED from the probed
	 * stack, the source pays exactly what landed, the spent item is consumed and the
	 * filled container given back. The static pair below is the offline seam.
	 */
	protected String activateHeldContainer(TapAccessible aSource, byte aSide, FluidStack aFluid, @Nullable Player aPlayer, ItemStack aHeld) {
		IFluidHandlerItem tHandler = heldItemHandler(aHeld);
		if (tHandler == null) return "the held item is no fluid container";
		int tFilled = fillHeldContainer(tHandler, aFluid);
		if (tFilled <= 0) return "the container accepted nothing";
		if (aSource.tapDrain(aSide, tFilled, true) == null) return "source refused the executed drain";
		giveFilledContainer(aPlayer, tHandler, aHeld);
		return "filled " + tFilled + " L into the held container";
	}

	/**
	 * The :165 FL.fill executed half — the container fills from the probed stack; returns
	 * the landed amount, the :166 {@code aFluid.amount > tNewFluid.amount} difference.
	 */
	public static int fillHeldContainer(IFluidHandlerItem aHandler, FluidStack aProbed) {
		return aHandler.fill(aProbed.copy(), FluidAction.EXECUTE);
	}

	/**
	 * The :168-169 spent-item half: {@code aStack.stackSize--} + {@code ST.give(aPlayer,
	 * tStack, T)} — the 1.20.1 container identity is {@code getContainer()} after the
	 * fill; without a player (the RCON channel) the spent item is just consumed.
	 */
	public static void giveFilledContainer(@Nullable Player aPlayer, IFluidHandlerItem aHandler, ItemStack aHeld) {
		aHeld.shrink(1); // :168
		ItemStack tContainer = aHandler.getContainer(); // the filled container identity
		if (aPlayer != null && tContainer != null && !tContainer.isEmpty()) {
			aPlayer.getInventory().placeItemBackInInventory(tContainer); // :169 ST.give
		}
	}
}
