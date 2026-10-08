package gregtech6.tileentity.attachment;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import gregtech6.block.attachment.GTAttachmentSmallBlock;

/**
 * 1.20.1 counterpart of gregtech/tileentity/tools/MultiTileEntityFluidNozzle.java and
 * MultiTileEntityFluidCapNozzle.java (task nozzle-function) — the gas attachment pair.
 * One BE class serves BOTH BETs (NOZZLE_BE + CAP_NOZZLE_BE — the ADR-P3-1
 * shared-class-multi-mount form; the chain picks by the block carrier's family, the
 * live discriminator: the landed one-arg ctor mounts every row over the NOZZLE_BE type,
 * so {@link #getType()} cannot tell the mounts apart).
 *
 * <p>The upstream activation is a PLAYER CLICK (onBlockActivated3 on both classes),
 * never a redstone gate and never a tick — the pool-card "auto drain" reading was
 * wrong, the archaeology verdict: both halves are right-click chains through
 * {@code mFacing} (MultiTileEntityFluidNozzle.java:73 / MultiTileEntityFluidCapNozzle
 * .java:74 getAdjacentTileEntity(mFacing)), and the interface arms they call are the
 * root-default delegators ({@code nozzleDrain → tapDrain} TileEntityBase01Root.java:936,
 * {@code capnozzleFill → funnelFill} :934 — the only upstream overrides live on the
 * EnderGarbageDump/MultiBlockPart/BasicMachine TEs the port has not cut). The port
 * calls the SAME {@link GTTapBlockEntity.TapAccessible}/{@link GTFunnelBlockEntity
 * .FunnelAccessible} faces the barrels/tanks already ship (the delegation is the
 * identity there), so no container-BE arm was added.
 *
 * <p>The nozzle (drain, MultiTileEntityFluidNozzle.java:71-129): VOIDING held item
 * first (:76-80, drains all + trash); then the probe (:81) gated on the TAP'S MIRROR —
 * gases ONLY ({@code FL.gas} true, where the tap demands false), amount {@code > 0},
 * and the acid door ({@code mAcidProof || !FL.acid}, :82). The empty hand (:83-116)
 * turns XP/mob gases into orbs (the default-rate forms — the OpenBlocks branch is the
 * declared cut, the tap-funnel-attachment precedent) and otherwise consumes the click;
 * a held container is filled EXECUTED from the probe and the tank pays exactly what
 * landed (:117-124, the tap's {@code fillHeldContainer}/{@code giveFilledContainer}
 * statics verbatim — the upstream bodies are identical modulo the drain hook).
 *
 * <p>The cap nozzle (fill, MultiTileEntityFluidCapNozzle.java:68-93): the held item
 * must CARRY gas (:72-73, the same mirror gate), the mounted container must be
 * {@link GTFunnelBlockEntity.FunnelAccessible}; the whole content must fit on the
 * probe (:76-77) before the executed pour consumes the spent container and returns its
 * empty form (:79-80) — else the {@code count == 1} handler form lands first and drains
 * the held by exactly what landed (:83-87), the funnel's declared translation verbatim.
 *
 * <p>A null player is the RCON acceptance channel ({@code /gt6tank nozzle|capnozzle
 * <pos>} — GTBarrelCommand, the tap/funnel counterfactual form; the virtual held item
 * is a metal-barrel stack, the one container whose item face is gas-proof).
 */
public class GTNozzleBlockEntity extends GTAttachmentSmallBlockEntity {

	public GTNozzleBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public GTNozzleBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : gregtech6.registry.GTBlockEntities.NOZZLE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "nozzle"; // BET registry paths mirror it (GTBlockEntities.NOZZLE_BE; the cap BET shares the class)
	}

	/**
	 * The cap mount verdict: the block carrier's family (the row config rides the block,
	 * the GT6Attachments row form). Public — the acceptance command discriminates the
	 * two mounts with it (GTBarrelCommand nozzle/capnozzle). Overridable for the offline
	 * fixtures — a stone state carries no row, so the tests force the cap half here.
	 */
	public boolean isCapNozzle() {
		return getBlockState().getBlock() instanceof GTAttachmentSmallBlock tBlock
				&& tBlock.row().family() == GTAttachmentSmallBlock.Family.CAP_NOZZLE;
	}

	@Override
	protected String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld) {
		if (!isServerSide()) return "client side";
		return isCapNozzle() ? activateCapChain(aPlayer, aHeld) : activateDrainChain(aPlayer, aHeld);
	}

	// ---------------------------------------------------------------------------
	// the drain chain (upstream MultiTileEntityFluidNozzle.onBlockActivated3 :71-129)
	// ---------------------------------------------------------------------------

	/** The gas-drain half, the whole :71-129 chain over the shared adjacency seam. */
	protected String activateDrainChain(@Nullable Player aPlayer, @Nullable ItemStack aHeld) {
		BlockEntity tTarget = adjacent();
		if (!(tTarget instanceof GTTapBlockEntity.TapAccessible tSource)) return "no tap-accessible container on the facing side";
		byte tSide = sideFacingBack(mFacing);

		// :76-80 — VOIDING held item: everything drains through the nozzle face and is trashed
		if (aHeld != null && !aHeld.isEmpty() && GTTapBlockEntity.VOIDING_ITEMS.contains(aHeld.getItem())) {
			FluidStack tAll = tSource.tapDrain(tSide, Integer.MAX_VALUE, true);
			return "voided " + (tAll == null ? 0 : tAll.getAmount()) + " L";
		}

		// :81-82 — probe, then the tap's mirror gate: gases ONLY, amount > 0, the acid door
		FluidStack aFluid = tSource.tapDrain(tSide, Integer.MAX_VALUE, false);
		if (aFluid == null || aFluid.getAmount() <= 0) return "nothing to draw";
		if (!isGas(aFluid)) return "refused a non-gas (the nozzle draws gases only)";
		if (!isAcidProof() && isAcid(aFluid)) return "refused an acid (nozzle is not acid proof)";

		// :83-116 — empty hand: XP/mob gases become orbs, everything else just consumes the click
		if (aHeld == null || aHeld.isEmpty()) {
			String tXp = spawnXpOrb(tSource, tSide, aFluid);
			return tXp != null ? tXp : "consumed (no effect)";
		}

		// :117-124 — held container: fill EXECUTED from the probe, the tank pays what landed
		IFluidHandlerItem tHandler = heldItemHandler(aHeld);
		if (tHandler == null) return "the held item is no fluid container";
		int tFilled = GTTapBlockEntity.fillHeldContainer(tHandler, aFluid);
		if (tFilled <= 0) return "the container accepted nothing";
		if (tSource.tapDrain(tSide, tFilled, true) == null) return "source refused the executed drain";
		GTTapBlockEntity.giveFilledContainer(aPlayer, tHandler, aHeld);
		return "filled " + tFilled + " L of gas into the held container";
	}

	/**
	 * The :84-113 XP half, the nozzle's own copy (spawns at the NOZZLE — upstream :92/:101
	 * use this TE's coords; the tap keeps its own identical-shaped method at its coords).
	 * The OpenBlocks ratio branch is the declared cut (the tap-funnel-attachment form):
	 * XP gases {@code min(50, L/20)} draining {@code 20 L per point} (:98-100), mob
	 * essences {@code min(50, L*3/200)} draining {@code (tXP*200)/3} (:107-110).
	 */
	@Nullable
	protected String spawnXpOrb(GTTapBlockEntity.TapAccessible aSource, byte aSide, FluidStack aFluid) {
		if (!hasLevel() || !(getLevel() instanceof ServerLevel tLevel)) return null;
		int tXp = -1;
		int tDrain = 0;
		if (isXpFluid(aFluid)) {
			tXp = Math.min(50, aFluid.getAmount() / 20); // :98
			tDrain = tXp * 20; // :100
		} else if (isMobFluid(aFluid)) {
			tXp = Math.min(50, (aFluid.getAmount() * 3) / 200); // :107
			tDrain = (tXp * 200) / 3; // :109
		}
		if (tXp <= 0) return null;
		aSource.tapDrain(aSide, tDrain, true);
		Vec3 tCenter = Vec3.atCenterOf(getBlockPos());
		net.minecraft.world.entity.ExperienceOrb.award(tLevel, tCenter.add(0, -0.3, 0), tXp); // the :92/:101 EntityXPOrb spawn
		return "spawned an XP orb worth " + tXp + " for " + tDrain + " L";
	}

	// ---------------------------------------------------------------------------
	// the cap-fill chain (upstream MultiTileEntityFluidCapNozzle.onBlockActivated3 :68-93)
	// ---------------------------------------------------------------------------

	/** The gas-fill half, the whole :68-93 chain. {@code aHeld} null/empty is the no-op. */
	protected String activateCapChain(@Nullable Player aPlayer, @Nullable ItemStack aHeld) {
		if (aHeld == null || aHeld.isEmpty()) return "nothing held";

		// :72 — ONE item's fluid content, and the mirror gate: gases ONLY, the acid door
		FluidStack tFluid = probeHeldFluid(aHeld);
		if (tFluid == null || tFluid.getAmount() <= 0) return "the held item carries no fluid";
		if (!isGas(tFluid)) return "refused a non-gas (the cap nozzle fills gases only)";
		if (!isAcidProof() && isAcid(tFluid)) return "refused an acid (cap nozzle is not acid proof)";

		BlockEntity tTarget = adjacent();
		if (!(tTarget instanceof GTFunnelBlockEntity.FunnelAccessible tAccessible)) return "no funnel-accessible container on the facing side";
		byte tSide = sideFacingBack(mFacing);

		// :76-82 — the whole-content pour: the probe must take it ALL, the executed pass pays,
		// the spent container is consumed and its empty form returned
		int tProbed = tAccessible.funnelFill(tSide, tFluid, false);
		if (tProbed >= tFluid.getAmount() && tAccessible.funnelFill(tSide, tFluid, true) > 0) {
			GTFunnelBlockEntity.giveEmptyContainer(aPlayer, aHeld);
			return "filled " + tFluid.getAmount() + " L of " + fluidKey(tFluid) + " from the held gas container, empty container returned";
		}

		// :83-87 — the container-handler form (count == 1): land first, drain the held by what landed
		IFluidHandlerItem tHandler = heldItemHandler(aHeld);
		if (tHandler != null && aHeld.getCount() == 1) {
			int tLanded = tAccessible.funnelFill(tSide, tFluid, true);
			if (tLanded > 0) tHandler.drain(tLanded, FluidAction.EXECUTE);
			return tLanded > 0 ? "drained " + tLanded + " L from the held gas container" : "the container refused the fill";
		}
		return "the container would not take the whole content";
	}

	/**
	 * The :72 probe seam (the funnel's {@code probeHeldFluid} twin) — overridable so the
	 * offline tests inject the held gas without the capability dispatch.
	 */
	protected FluidStack probeHeldFluid(ItemStack aHeld) {
		return GTFunnelBlockEntity.heldFluid(aHeld);
	}
}
