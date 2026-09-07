package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase01Root;

/**
 * 1.20.1 counterpart of gregapi/tileentity/multiblocks/MultiTileEntityMultiBlockPart.java
 * (698 lines, task p4-multiblock-framework W3) — the notick half of a multiblock: a cell
 * holding a back-reference to its controller.
 *
 * <p>Port scope (the 698-line relay face cut to the capability minimum, task card ②):
 * <ul>
 * <li>{@code mTargetPos} NBT (:76, :131, :161-166 — NBT_TARGET + X/Y/Z longs) and the
 *     {@code mTarget} runtime cache (:78);</li>
 * <li>{@link #getTarget(boolean)} (:199-214) verbatim: lazy rebuild from mTargetPos with the
 *     {@code isInsideStructure} ownership validation, the dead-controller drop, and the
 *     aCheckValidity gate that runs a full (cheap-path) checkStructure;</li>
 * <li>{@link #setTarget} (:216-221) + {@link #setDesign} (:223-231 — the design is the
 *     upstream texture-group index; with no texture system it persists and does nothing
 *     visual, and the updateClientData :227 becomes a setChanged);</li>
 * <li>the {@code mMode} permission bitmask (:85-126) verbatim, constants and all — the
 *     CokeOven asks its parts for {@link #ONLY_ITEM_FLUID_ENERGY}; the relay consumer that
 *     reads the mask arrives with the IO cards;</li>
 * <li>the relay: upstream re-exposed 20+ interfaces onto the controller (fill/drain, item
 *     slots, energy, logistics...); the 1.20.1 equivalent is capability forwarding —
 *     ITEM_HANDLER / FLUID_HANDLER / ENERGY resolve against the target controller
 *     ({@link gregtech6.tileentity.connectors.GTFluidPipeBlockEntity} getCapability
 *     side-wrap precedent), everything else falls through to the (empty) part defaults;</li>
 * <li>the {@code mDesign} visual (:82) persists as NBT for round-trip fidelity.</li>
 * </ul>
 *
 * <p>Life-cycle hooks are BLOCK-side in 1.20.1: the upstream IMTE_BreakBlock (:176-184) and
 * IMTE_OnBlockAdded (:187-197) propagation lives on the part Block
 * ({@code onPlace}/{@code playerWillDestroy}) — the remembered BaseEntityBlock onRemove trap
 * (BE kill+recreate loop on same-block state changes) is why onRemove is never touched.
 * The block calls {@link #clearTarget()}.
 *
 * <p>The controller-liveness probe uses the vanilla {@code isRemoved()} (the upstream
 * {@code mTarget.isDead()} GT flag rides the ported Root only; the vanilla removal
 * lifecycle is the 1.20.1 truth for "the controller BE is gone").
 */
public class MultiBlockPartBlockEntity extends TileEntityBase01Root {

	public static final String NBT_TARGET = "target";
	public static final String NBT_TARGET_X = "target_x";
	public static final String NBT_TARGET_Y = "target_y";
	public static final String NBT_TARGET_Z = "target_z";
	public static final String NBT_DESIGN = "design";
	public static final String NBT_MODE = "mode";

	/** The controller cell this part belongs to (upstream :76, NBT-persisted). */
	@Nullable
	public BlockPos mTargetPos = null;

	/** The resolved controller cache — ALWAYS re-derivable from mTargetPos (upstream :78). */
	@Nullable
	public ITileEntityMultiBlockController mTarget = null;

	/** Texture-group index (upstream :82) — visual surface not ported, NBT round-trip only. */
	public short mDesign = 0;

	/** Permission bitmask (upstream :83). */
	public int mMode = 0;

	// the upstream bitmask table :85-126, verbatim
	public static final int
		  EVERYTHING                 = 0

	, NO_ENERGY_OUT              = 1
	, NO_ENERGY_IN               = 2
	, NO_FLUID_OUT               = 4
	, NO_FLUID_IN                = 8
	, NO_ITEM_OUT                = 16
	, NO_ITEM_IN                 = 32
	, NO_LOGISTICS               = 64
	, NO_CRUCIBLE                = 128

	, NO_ENERGY                  = NO_ENERGY_IN | NO_ENERGY_OUT
	, NO_FLUID                   = NO_FLUID_IN  | NO_FLUID_OUT
	, NO_ITEM                    = NO_ITEM_IN   | NO_ITEM_OUT

	, ONLY_IN                    = NO_ENERGY_OUT | NO_FLUID_OUT | NO_ITEM_OUT | NO_LOGISTICS | NO_CRUCIBLE
	, ONLY_OUT                   = NO_ENERGY_IN  | NO_FLUID_IN  | NO_ITEM_IN  | NO_LOGISTICS | NO_CRUCIBLE

	, ONLY_ENERGY_OUT            = ~NO_ENERGY_OUT
	, ONLY_ENERGY_IN             = ~NO_ENERGY_IN
	, ONLY_FLUID_OUT             = ~NO_FLUID_OUT
	, ONLY_FLUID_IN              = ~NO_FLUID_IN
	, ONLY_ITEM_OUT              = ~NO_ITEM_OUT
	, ONLY_ITEM_IN               = ~NO_ITEM_IN
	, ONLY_ITEM_FLUID_OUT        = ~(NO_ITEM_OUT | NO_FLUID_OUT)
	, ONLY_ITEM_FLUID_IN         = ~(NO_ITEM_IN  | NO_FLUID_IN )
	, ONLY_ITEM_FLUID_ENERGY_OUT = ~(NO_ITEM_OUT | NO_FLUID_OUT | NO_ENERGY_OUT)
	, ONLY_ITEM_FLUID_ENERGY_IN  = ~(NO_ITEM_IN  | NO_FLUID_IN  | NO_ENERGY_IN )

	, ONLY_CRUCIBLE              = ~NO_CRUCIBLE
	, ONLY_LOGISTICS             = ~NO_LOGISTICS
	, ONLY_ENERGY                = ~NO_ENERGY
	, ONLY_FLUID                 = ~NO_FLUID
	, ONLY_ITEM                  = ~NO_ITEM
	, ONLY_ITEM_FLUID            = ~(NO_ITEM  | NO_FLUID )
	, ONLY_ITEM_FLUID_ENERGY     = ~(NO_ITEM  | NO_FLUID | NO_ENERGY)
	, ONLY_ITEM_ENERGY           = ~(NO_ITEM  | NO_ENERGY)
	, ONLY_FLUID_ENERGY          = ~(NO_FLUID | NO_ENERGY)

	// the all-deny sentinel: ~0 — every NO_* bit set, so no face admits anything (item,
	// fluid, energy, logistics, crucible, in and out). Constraint: it is already the
	// full-width complement, the terminal ~ form of this table — it must never be OR-combined
	// with further NO_* bits or used as a base to derive new modes from (a mask that denies
	// everything leaves no selectable bit to compose on); use it verbatim as a mode value.
	, NOTHING                    = ~EVERYTHING
	;

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the oven precedent). */
	public MultiBlockPartBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTMultiBlocks.MULTIBLOCK_PART_BE.get(), aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	public MultiBlockPartBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// notick: the upstream part never self-ticks (TileEntityBase05Paintable notick chain)
		super(false, aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_part";
	}

	// ---------------------------------------------------------------------------
	// NBT (:128-167)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TARGET, Tag.TAG_ANY_NUMERIC)) {
			mTargetPos = new BlockPos(
					(int) Mth.clamp(aNBT.getLong(NBT_TARGET_X), Integer.MIN_VALUE, Integer.MAX_VALUE), // UT.Code.bindInt :131
					(int) Mth.clamp(aNBT.getLong(NBT_TARGET_Y), Integer.MIN_VALUE, Integer.MAX_VALUE),
					(int) Mth.clamp(aNBT.getLong(NBT_TARGET_Z), Integer.MIN_VALUE, Integer.MAX_VALUE));
		} else {
			mTargetPos = null;
		}
		if (aNBT.contains(NBT_DESIGN, Tag.TAG_ANY_NUMERIC)) mDesign = (short) (aNBT.getByte(NBT_DESIGN) & 0xFF); // UT.Code.unsignB :132
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getInt(NBT_MODE);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mDesign != 0) aNBT.putByte(NBT_DESIGN, (byte) mDesign); // :159
		if (mMode != 0) aNBT.putInt(NBT_MODE, mMode);               // :160
		if (mTargetPos != null) {                                   // :161-166
			aNBT.putBoolean(NBT_TARGET, true);
			aNBT.putLong(NBT_TARGET_X, mTargetPos.getX());
			aNBT.putLong(NBT_TARGET_Y, mTargetPos.getY());
			aNBT.putLong(NBT_TARGET_Z, mTargetPos.getZ());
		}
	}

	// ---------------------------------------------------------------------------
	// target resolution (:199-231)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :199-214 verbatim. Lazy rebuild: a missing/dead cache re-resolves mTargetPos
	 * through the {@code isInsideStructure} ownership check — a stale pointer to a controller
	 * that no longer contains this cell clears itself (mTargetPos = null + design reset
	 * :208-209). aCheckValidity additionally demands the controller's structure to still hold.
	 */
	@Nullable
	public ITileEntityMultiBlockController getTarget(boolean aCheckValidity) {
		if (mTargetPos == null) return null;
		if (mTarget == null || (mTarget instanceof BlockEntity tController && tController.isRemoved())) {
			mTarget = null;
			if (hasLevel() && getLevel().isLoaded(mTargetPos)) {
				BlockEntity tTarget = getLevel().getBlockEntity(mTargetPos);
				if (tTarget instanceof ITileEntityMultiBlockController tController
						&& tController.isInsideStructure(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())) {
					mTarget = tController;
				} else {
					mTargetPos = null;
					setDesign(0);
				}
			}
		}
		return aCheckValidity && mTarget != null && !mTarget.checkStructure(false) ? null : mTarget;
	}

	/** Upstream :216-221. */
	public void setTarget(ITileEntityMultiBlockController aTarget, int aDesign, int aMode) {
		mTarget = aTarget;
		mTargetPos = (aTarget == null ? null : ((BlockEntity) aTarget).getBlockPos());
		mMode = aMode;
		setDesign(aDesign);
	}

	/** Upstream :223-231 with the texture surface cut (updateClientData :227 → setChanged). */
	public boolean setDesign(int aDesign) {
		aDesign = Mth.clamp(aDesign, 0, 255); // UT.Code.bind8
		if (aDesign != mDesign) {
			mDesign = (short) aDesign;
			setChanged();
			return true;
		}
		return false;
	}

	/** The breakBlock body (:176-184, minus the onStructureChange the Block hook fires). */
	public void clearTarget() {
		if (mTarget != null || mTargetPos != null) {
			mTarget = null;
			mTargetPos = null;
			setChanged();
		}
	}

	/**
	 * The builder-wand relay target (task p24-builder-wand — the minimal faithful face of
	 * the upstream tool-relay, MultiTileEntityMultiBlockPart.java:251-266). The wand
	 * clicks THIS part, the CONTROLLER does the work: {@code getTarget(false)} resolves
	 * the owner with the lazy {@code isInsideStructure} rebuild (:199-214), the explicit
	 * :261 ownership re-check guards the stale-cache case the lazy rebuild cannot see,
	 * and only a controller whose structure still contains this cell is returned
	 * (upstream :262-263 drops the claim on the mismatch — the drop itself stays in the
	 * lazy rebuild, the relay just refuses to fire).
	 *
	 * <p>Declared deviation: the upstream no-controller arm answered the wand with the
	 * chat line "There is no Multiblock Controller for this Block." (:256-258) — the
	 * {@code aChatReturn} mechanism has no port counterpart, so the unlinked-part click
	 * is a silent no-op (the caller {@code PASS}es).
	 *
	 * <p>Builder-wand exclusive: the only upstream part-relay tools are the wand and the
	 * magnifying glass (:256); the magnifier face is not ported, so this relay has no
	 * generic {@code IBlockToolable} broadcast layer (research gap ③, the future
	 * tool-system domain) — the wand item resolves this method directly.
	 *
	 * @return the owning {@link TileEntityBase10MultiBlockBase} (the only port
	 *         controller population answering the upstream
	 *         {@code onToolClickMultiBlock} wand arm), or null = no scaffold target.
	 */
	@Nullable
	public TileEntityBase10MultiBlockBase wandTarget() {
		if (getTarget(false) instanceof TileEntityBase10MultiBlockBase tController
				&& tController.isInsideStructure(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())) {
			return tController;
		}
		return null;
	}

	// ---------------------------------------------------------------------------
	// the capability relay (the 698-line interface face → 3 capabilities)
	// ---------------------------------------------------------------------------

	//? if forge {
	/**
	 * ITEM_HANDLER / FLUID_HANDLER / ENERGY resolve against the target controller — the
	 * upstream re-exposure of the controller's IO surfaces onto every part (:206-225 and the
	 * IFluidHandler/ITileEntityEnergy implements). The LazyOptional travels with the
	 * controller's own invalidation lifecycle; an unresolved/empty controller answers empty.
	 */
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER
				|| aCapability == ForgeCapabilities.FLUID_HANDLER
				|| aCapability == ForgeCapabilities.ENERGY) {
			BlockEntity tController = relayTarget();
			if (tController != null) {
				return tController.getCapability(aCapability, aSide);
			}
			return LazyOptional.empty();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?}
	//? if neoforge {
	/* // 21.1 face: BlockEntity carries no getCapability to override — the part relay
	   // re-forms as RegisterCapabilitiesEvent provider wiring: the part's provider
	   // resolves the controller (relayTarget()) and answers through the level query
	   // (ILevelExtension.getCapability), preserving the controller-owned lifecycle.
	 *///?}

	/**
	 * The relay resolution half, as a seam: ForgeCapabilities cannot
	 * class-init offline ("This will be implemented by a transformer", CapabilityToken:28),
	 * so the tests verify THIS half and the one-line getCapability delegation rides the
	 * already-covered Forge mechanism. Package-private at birth; widened to public for
	 * the 21.1 provider wiring (GT6CapabilityWiring lives in gregtech6.registry — a
	 * cross-package consumer), a visibility-only change with no behavioral delta on
	 * either leg.
	 */
	@Nullable
	public BlockEntity relayTarget() {
		ITileEntityMultiBlockController tTarget = getTarget(false);
		return tTarget instanceof BlockEntity tController ? tController : null;
	}
}
