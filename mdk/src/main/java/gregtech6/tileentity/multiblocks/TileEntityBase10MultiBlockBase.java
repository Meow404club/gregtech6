package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of gregapi/tileentity/multiblocks/TileEntityBase10MultiBlockBase.java
 * (226 lines, task p4-multiblock-framework W3) — the state machine and trigger wiring every
 * multiblock controller shares. Controllers tick (the 600-tick poll needs the dispatcher).
 *
 * <p>Port scope:
 * <ul>
 * <li>the {@code mStructureChanged}/{@code mStructureOkay} pair (:54) with the
 *     {@code NBT_STATE+".str"} persistence (:61/:95 → plain key "structure_okay");</li>
 * <li>the four structure-check triggers: the onTickFirst forced check (:112-115), the
 *     {@code mTimer % 600 == 5} poll with the not-okay fallback (:121-124), the
 *     coordinate-change trigger (:106-109), and the builder-wand entry (:130-146 — the wand
 *     itself is {@link ITileEntityMultiBlockController.Util#checkAndSetTarget}; callers drive
 *     {@link #checkStructure2} + {@link #checkStructure} exactly like upstream onToolClick2
 *     :132-133/:143-144);</li>
 * <li>the {@link #checkStructure} template method (:177-185) verbatim — recheck only when the
 *     changed-flag or aForceReset is up, flip mStructureOkay on disagreement, consume the
 *     flag either way;</li>
 * <li>the {@link #checkStructure2(BlockPos, Player, Container)} hook (:202 — the :204 no-arg
 *     compatibility overload folds away, it existed for 1.7.10 subclass back-compat only);</li>
 * <li>the visual formed bit: upstream getDirectionData/setDirectionData bit 3 (:188-189)
 *     becomes the {@link #FORMED} BlockState property — the 1.7.10 direction-data channel is
 *     gone and the formed state is world-visible state (the datagen blockstate variants are
 *     the spec ⑧ fallback rendering; the D-tier ModelData formed look stays in the pool);</li>
 * <li>the facing minimal face of TileEntityBase09FacingSingle folds in here (that 1.7.10
 *     base layer stays out of the ported base/ chain): byte mFacing in the GT6 side order
 *     (== Direction 3D data order, see TileEntityBase01Root), NBT persisted,
 *     {@code onFacingChange → onStructureChange} (:187).</li>
 * </ul>
 *
 * <p>Omissions: the IIconContainer texture sets (:56, :63-89) and getTexture2 (:192-194) are
 * the rendering surface (FORMED blockstate variants carry the visual); the tooltip pair
 * (:99-103) needs the LH stack; doDefaultStructuralChecks (:196) keeps the upstream
 * always-true default (the Root:776 environment-hazard check is stripped infrastructure).
 */
public abstract class TileEntityBase10MultiBlockBase extends TileEntityBase03TicksAndSync implements ITileEntityMultiBlockController {

	/** The formed visual bit, replacing upstream direction-data bit 3 (:188-189). */
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	/** The facing BlockState mirror (horizontal — the GTOvenBlock idiom), base-owned like FORMED. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Upstream NBT_STATE+".str" (:61/:95) in the in-repo plain-key form. */
	public static final String NBT_STRUCTURE_OKAY = "structure_okay";

	public static final String NBT_FACING = "facing";

	/** Set whenever something claims the structure may have changed (upstream :54; written by onStructureChange :199). */
	public boolean mStructureChanged = false;

	/** Whether the last completed check found the structure formed (upstream :54). */
	public boolean mStructureOkay = false;

	/**
	 * The controller facing, GT6 side order == Direction 3D data order. Default north (2) —
	 * "Main Block centered on Side and facing outwards": the structure core sits BEHIND the
	 * facing (the getOffsetXN arithmetic), per the CokeOven tooltip (MultiTileEntityCokeOven.java:64).
	 */
	public byte mFacing = 2;

	protected TileEntityBase10MultiBlockBase(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// controllers tick: the 600-tick poll (:121-124) is the maintenance trigger
		super(true, aType, aPos, aState);
	}

	// ---------------------------------------------------------------------------
	// NBT (:58-96)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STRUCTURE_OKAY, Tag.TAG_ANY_NUMERIC)) mStructureOkay = aNBT.getBoolean(NBT_STRUCTURE_OKAY);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte(NBT_FACING);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STRUCTURE_OKAY, mStructureOkay); // UT.NBT.setBoolean :95
		aNBT.putByte(NBT_FACING, mFacing);
	}

	// ---------------------------------------------------------------------------
	// the four triggers (:105-146)
	// ---------------------------------------------------------------------------

	/** Upstream :106-109. */
	@Override
	public void onCoordinateChange() {
		super.onCoordinateChange();
		checkStructure(true);
	}

	/** Upstream onTickFirst2 :112-115 — the first server tick always forces a full check. */
	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide) checkStructure(true);
	}

	/** Upstream onTick2 :118-126 — the 600-tick maintenance poll (mTimer is post-increment here, first tick sees 1). */
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (aIsServerSide) {
			if (mTimer % 600 == 5) {
				if (!checkStructure(false)) checkStructure(true);
				doDefaultStructuralChecks();
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the check template (:176-185) and its hook (:201-204)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :177-185 verbatim: the recheck happens only when the changed flag or aForceReset
	 * is up — otherwise the cached mStructureOkay is the cheap answer. A flip syncs the visual
	 * formed bit (updateClientData :181 + the FORMED blockstate write replacing the bit-3
	 * direction data :188-189).
	 */
	@Override
	public boolean checkStructure(boolean aForceReset) {
		if (isClientSide()) return mStructureOkay;
		if ((mStructureChanged || aForceReset) && mStructureOkay != checkStructure2(null, null, null)) {
			mStructureOkay = !mStructureOkay;
			updateClientData();
			applyFormedBlockState();
		}
		mStructureChanged = false;
		return mStructureOkay;
	}

	/**
	 * The subclass structure check (upstream :202), with the builder-wand support triple:
	 * the clicked cell, the acting player and the inventory to consume parts from — all
	 * nullable (the plain polling pass goes through with all three null).
	 */
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		return true;
	}

	// ---------------------------------------------------------------------------
	// controller interface + facing (upstream :187-199)
	// ---------------------------------------------------------------------------

	/** Upstream :199 — the flag the template consumes. */
	@Override
	public void onStructureChange() {
		mStructureChanged = true;
	}

	/** Upstream :196 — the environment-hazard default (the Root:776 body is stripped infrastructure). */
	public boolean doDefaultStructuralChecks() {
		return true;
	}

	/** Upstream :187 — a facing flip breaks the structure bookkeeping, force a recheck. */
	public void onFacingChange(byte aPreviousFacing) {
		onStructureChange();
	}

	public byte getFacing() {
		return mFacing;
	}

	/** Runtime facing write: NBT field + onFacingChange hook; the BlockState mirror rides the caller's state write. */
	public void setFacing(byte aFacing) {
		if (aFacing != mFacing) {
			byte tPrevious = mFacing;
			mFacing = aFacing;
			setChanged();
			onFacingChange(tPrevious);
		}
	}

	/**
	 * Placement facing (the GTOvenBlock.setPlacedBy double-write pattern): NBT field is the
	 * authority, the BlockState mirror is written immediately so the placement renders oriented.
	 */
	public void setFacingFromPlacement(Player aPlayer) {
		mFacing = (byte) aPlayer.getDirection().get3DDataValue();
		setChanged();
		applyFacingBlockState();
	}

	/**
	 * Writes the formed bit onto the BlockState (the vanilla furnace setBlock(state, 3) idiom;
	 * same-block state changes keep the BE, LevelChunk.setBlockState:292 CHECK branch — the
	 * remembered kill+recreate trap only exists for onRemove-based removals, which this avoids).
	 */
	protected void applyFormedBlockState() {
		if (!hasLevel() || isClientSide()) return;
		BlockState tState = getLevel().getBlockState(getBlockPos());
		if (!tState.hasProperty(FORMED) || tState.getValue(FORMED) == mStructureOkay) return;
		getLevel().setBlock(getBlockPos(), tState.setValue(FORMED, mStructureOkay), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
	}

	/**
	 * The facing half of the placement double-write. The property is the shared
	 * {@link #FACING} (the controller blocks add it to their state definition, the same
	 * base-owned pattern as {@link #FORMED}); only horizontal facings carry a BlockState
	 * mirror (the property is the vanilla horizontal one, the placement idiom).
	 */
	private void applyFacingBlockState() {
		if (!hasLevel() || isClientSide()) return;
		BlockState tState = getLevel().getBlockState(getBlockPos());
		if (!tState.hasProperty(FACING)) return;
		Direction tDirection = Direction.from3DDataValue(mFacing);
		if (!tDirection.getAxis().isHorizontal() || tState.getValue(FACING) == tDirection) return;
		getLevel().setBlock(getBlockPos(), tState.setValue(FACING, tDirection), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
	}
}
