package gregtech6.tileentity.energy.generators;

import gregapi.util.UT;
import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.fluids.FluidStack;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMapFurnaceFuel;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Solid Burning Box base — task p13-burning-box-family
 * spec ①/⑤, ported from gregtech/tileentity/energy/generators/MultiTileEntityGeneratorSolid.java
 * (:53-287) as the HU source of the whole burning-box family: EVERY family row emits
 * {@code TD.Energy.HU} (the Loader_MultiTileEntities.java:517-704 {@code NBT_ENERGY_EMITTED}
 * column, one value across all five subtypes), through the TOP face only.
 *
 * <p><b>The tick (upstream onTick2 :100-175, server branch verbatim)</b>:
 * <ol>
 * <li>emit — {@code mEnergy >= mRate} pushes one packet of
 *     {@code min(mRate, mEnergy)} through the Util handshake and subtracts
 *     {@code mRate} UNCONDITIONALLY (:103-105 — the packet is spent even into an
 *     empty face, the "打空也扣能" generator family, the diesel-engine :109-112
 *     form; with no HU consumer this wave the buffer still drains, the W3 boilers
 *     become the sink);</li>
 * <li>fire spread (:106-108) — every EMITTING tick the pure judgment
 *     {@code mEfficiency < 1 || rng(mEfficiency) == 0} drops one fire block into the
 *     7×5×7 volume {@code x-3+rng(7), y-1+rng(5), z-3+rng(7)} (FLAME_RANGE=3, :54)
 *     through {@link #trySpreadFire}, the {@code WD.fire(world, x, y, z, T)} port
 *     (see {@link #placeFire} for the flammability gate translation);</li>
 * <li>the refuel gate (:110-160) — while {@code mEnergy < mRate * 2} (the "缓冲
 *     不足两包才续烧"门): the front-face {@code WD.burn} arm (:111) is POOLED (the
 *     flammable-block ignition surface — declared, no live seam); and when the output
 *     slot can take the container item, the front face has air, is not liquid and has
 *     oxygen (:113), one fuel unit is charged (:151-158): the fresh
 *     {@link RecipeMapFurnaceFuel} row is consumed and the buffer credits
	 *     {@code units(absoluteTotalPower, 10000, mEfficiency, F)} (:156, the
	 *     {@link gregapi.util.UT.Code#units} form). The upstream
 *     {@code mOutput1} holding field (:62, moved into slot 1 at :112) folds into a
 *     direct guarded slot-1 write in {@link #chargeFuel} — same observable, one less
 *     field (declared).</li>
 * <li>burn-out (:163) — {@code mEnergy < mRate} stops the burning flag;</li>
 * <li>the front auto-ignite (:166-168) — while NOT burning,
 *     {@code rng(200) == 0 && front-is-fire} lights the box (the classic "light a fire
 *     in front of it" mechanic, the live arm the RCON chain's fire cluster drives).</li>
 * </ol>
 *
 * <p><b>The :113 oxygen gate</b>: upstream {@code WD.oxygen} (WD.java:396-398) is the
 * Galacticraft check — WITHOUT that mod loaded it answers TRUE unconditionally, and
 * this port carries no Galacticraft, so the gate folds to the air/liquid part only
 * (collision-shape-empty + non-liquid block + empty fluid state), documented on
 * {@link #frontFaceOpen}.</p>
 *
 * <p><b>The energy face (:269-275 verbatim)</b>: HU only,
 * {@code isEnergyEmittingTo = SIDES_TOP[aSide] && super} — the top-face byte (1 in the
 * port's {@code Direction.get3DDataValue} order, the pre-pinned HuEnergyHandshakeTest
 * truth table), the offered packet {@code min(mRate, mEnergy)}, and the
 * recommended/min/max output sizes all {@code mRate} (:271-274).</p>
 *
 * <p><b>The row values ride the BLOCK carrier</b> (the GTAxleBlock/SteamEngineBlock
 * registration-carrier form): efficiency and output rate come off the placed
 * {@code GT6BurningBoxes} row, NBT re-carries them for a clean round trip (the diesel
 * NBT_OUTPUT form).</p>
 *
 * <p><b>Cropped with declaration (the upstream surfaces with no port seam)</b>: the
 * Firestone refined/cracked special (:114-149 — the Railcraft items do not exist in
 * this port), the front {@code WD.burn} flammable-block arm (:111), the contact heat
 * damage (:258 {@code applyHeatDamage min(10, mRate/10)}) and the ×16 burning hardness
 * (:281) — all pool-card content, none observable without entities/tools; the client
 * burning particles (:173, the AV pool); the cover/tool/shovel faces (:177/:220-236,
 * the cover and tool pools); {@code ITileEntityRunningActively} (:277-279 — the state
 * surfaces ride {@code /gt6burner stat}); and the 0.875-height collision box (:259 —
 * the single-cube port block shape is the family-wide declared deviation, the
 * crank/axle precedent). NO GUI by census — the upstream tooltip
 * {@code LH.NO_GUI_CLICK_TO_INVENTORY} (:92) IS the interaction contract, ported as
 * {@link #useOnFront} (the front-face insert/extract, :180-217).
 */
public abstract class GTGeneratorSolidBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream :54 flame range — fire drops land in {@code [-FLAME_RANGE, +FLAME_RANGE]} around the box. */
	public static final int FLAME_RANGE = 3;

	/** The vanilla UP ordinal — the port's byte for the upstream CS SIDES_TOP slot (the HuEnergyHandshakeTest SIDE_TOP). */
	public static final byte SIDE_TOP = 1;

	/** The upstream NBT keys (the CS NBT_ENERGY/NBT_ACTIVE/NBT_OUTPUT/NBT_EFFICIENCY family, the diesel spellings). */
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_ACTIVE = "gt.active";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_EFFICIENCY = "gt.efficiency";

	/** The upstream :56 efficiency (NBT_EFFICIENCY of the registration row; the rows carry 2500..10000). */
	public short mEfficiency = 10000;

	/** The upstream :57 stored heat and the per-row packet rate (NBT_OUTPUT of the row, 16..1024 HU/t). */
	public long mEnergy = 0, mRate = 1;

	/** The upstream :58 burning flag (the synced visual bit, :250-254). */
	public boolean mBurning = false;

	/** The upstream :59 energy type — every Loader :517-704 burning-box row carries TD.Energy.HU. */
	public TagData mEnergyTypeEmitted = TD.Energy.HU;

	/**
	 * The upstream :62/:267 two-slot inventory: slot 0 = the fuel in, slot 1 = the
	 * ash/container out (the :112/:154-155 {@code mOutput1} landing slot).
	 */
	public final GTItemStackHandler mInventory = new GTItemStackHandler(2, this::setChanged);

	/** The BE runtime facing mirror (byte, the GT6 side order == Direction.get3DDataValue; the FRONT is the fuel/ignite face). */
	public byte mFacing = 2; // NORTH

	/** The fuel map override — the injected fixture offline; the live FM.Furnace synthesis otherwise. */
	@Nullable
	protected RecipeMapFurnaceFuel mRecipesMapOverride = null;

	/** The fuel map accessor — the live FM.Furnace synthesis by default (RecipeMapFurnaceFuel, never row-poured). */
	@Nullable
	public RecipeMapFurnaceFuel recipeMap() {
		return mRecipesMapOverride != null ? mRecipesMapOverride : GT6RecipeMaps.FURNACE_FUEL;
	}

	/**
	 * BET factory for BlockEntityType.Builder.of — the concrete family resolves its shared
	 * type (the diesel full-ctor form). The ROW VALUES (rate/efficiency) read off the
	 * placed BLOCK carrier HERE — the GTDieselEngineBlockEntity ctor pattern (:201) — so
	 * every placement path (/setblock and RCON place included, not just BlockItem
	 * setPlacedBy) mounts the row config.
	 */
	public GTGeneratorSolidBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
		if (aState.getBlock() instanceof gregtech6.registry.GT6BurningBoxes.BurningBoxBlock tBoxBlock) {
			mRate = Math.max(1, tBoxBlock.row().rate());
			mEfficiency = tBoxBlock.row().efficiency();
		}
	}

	@Override
	public String getTileEntityName() {
		return "burning_box.solid"; // the family base name; the concrete BET rows carry their own registry paths
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :100-175 server branch, the emit/spread/refuel order)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the :101 server branch (the :173 client particles are the AV pool)
		syncFacingFromState();
		if (mBurning) {
			// :103-105 — the emit: one packet, spent unconditionally
			if (mEnergy >= mRate) {
				ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyTypeEmitted, 1, Math.min(mRate, mEnergy), this, adjacency());
				mEnergy -= mRate;
				// :106-108 — the fire spread judgment, the pure function + the live rng
				if (shouldSpreadFire(mEfficiency, rng(mEfficiency))) trySpreadFire();
			}
			// :110-160 — the refuel gate ("buffer under two packets → burn one more unit")
			if (mEnergy < mRate * 2) {
				// :111 — WD.burn front-face flammable ignition: POOLED (class doc)
				// :113 — the air/liquid/oxygen gate on the front face + a present fuel stack + a free output slot
				if (frontFaceOpen() && !mInventory.getStackInSlot(0).isEmpty() && canTakeContainerOutput()) {
					chargeFuel();
				}
			}
			// :163 — out of fuel
			if (mEnergy < mRate) mBurning = false;
		} else {
			// :166-168 — something burning in front of it? Let's ignite!
			if (rng(200) == 0 && frontIsFlaming()) mBurning = true;
		}
		if (mEnergy < 0) mEnergy = 0; // :171
	}

	/** The :113 output-slot arm — the folded {@code mOutput1 == null} check: slot 1 has room for the container item. */
	protected boolean canTakeContainerOutput() {
		ItemStack tOut = mInventory.getStackInSlot(1);
		return tOut.getCount() < Math.max(1, mInventory.getSlotLimit(1));
	}

	/**
	 * The :151-158 charge, split out for the offline tests: the fresh furnace-fuel row is
	 * probe-checked and consumed one unit deep, the container item (bucket et al) lands
	 * in the output slot (:154-155), and the buffer credits
	 * {@code units(absoluteTotalPower, 10000, mEfficiency, F)} (:156).
	 */
	protected void chargeFuel() {
		RecipeMapFurnaceFuel tMap = recipeMap();
		if (tMap == null) return;
		Recipe tRecipe = tMap.findFuelRecipe(mInventory.getStackInSlot(0));
		if (tRecipe == null) return;
		if (tRecipe.mInputs.length <= 0) return;
		ItemStack tFuel = mInventory.getStackInSlot(0);
		if (!tRecipe.isRecipeInputEqual(false, false, new FluidStack[0], tFuel)) return; // the :152 probe half
		// the consume half (:152 isRecipeInputEqual(T, F, ...)) — one unit out of slot 0
		mInventory.extractItem(0, tRecipe.mInputs[0].getCount(), false);
		// :154-155 — the outputs (the container item) to the output slot
		for (ItemStack tOut : tRecipe.mOutputs) {
			if (tOut == null || tOut.isEmpty()) continue;
			if (mInventory.getStackInSlot(1).isEmpty()) mInventory.setStackInSlot(1, tOut.copy());
			else if (ItemStack.isSameItemSameTags(mInventory.getStackInSlot(1), tOut)) mInventory.getStackInSlot(1).grow(tOut.getCount());
		}
		// :156 — the efficiency translation
		mEnergy += UT.Code.units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false);
	}

	// ---------------------------------------------------------------------------
	// the rng seam + the fire spread (upstream :106-108, the WD.fire(T) port)
	// ---------------------------------------------------------------------------

	/**
	 * The upstream rng judgment (:106, the same formula in every burning-box subtype),
	 * PURE so the offline truth table pins it: {@code mEfficiency < 1 || rngValue == 0}.
	 * (An efficiency below 1 divides the rng range to nothing — upstream spreads fire
	 * EVERY tick there.)
	 */
	public static boolean shouldSpreadFire(long aEfficiency, int aRngValue) {
		return aEfficiency < 1 || aRngValue == 0;
	}

	/** The live rng (the upstream TileEntityBase rng family) — the world Random, overridable offline. */
	protected int rng(int aRange) {
		if (aRange <= 0) return 0;
		if (mRngOverride != null) return mRngOverride.getAsInt();
		return hasLevel() ? getLevel().random.nextInt(aRange) : 0;
	}

	/** The offline rng seam (the adjacency-override pattern). */
	@Nullable
	private java.util.function.IntSupplier mRngOverride = null;

	void setRngOverride(@Nullable java.util.function.IntSupplier aRng) {
		mRngOverride = aRng;
	}

	/**
	 * The :107 {@code WD.fire(world, x-3+rng(7), y-1+rng(5), z-3+rng(7), T)} port — the
	 * 7×5×7 volume around the box, the Solid family's FLAME_RANGE 3.
	 */
	void trySpreadFire() {
		if (!hasLevel()) return;
		BlockPos tCenter = getBlockPos();
		int tX = tCenter.getX() - FLAME_RANGE + rng(2 * FLAME_RANGE + 1);
		int tY = tCenter.getY() - 1 + rng(2 + FLAME_RANGE);
		int tZ = tCenter.getZ() - FLAME_RANGE + rng(2 * FLAME_RANGE + 1);
		placeFire(getLevel(), new BlockPos(tX, tY, tZ));
	}

	/**
	 * The WD.fire(x, y, z, T) body (WD.java:706-724). The translation, arm by arm:
	 * a LAVA/FIRE target refuses (:708), a cell with a collision box refuses (:709 the
	 * {@code getCollisionBoundingBoxFromPool != null} arm — the carpet half of the
	 * upstream disjunction is subsumed, carpets have no full collision shape either
	 * way), a flammable target ignites (:711), and a non-flammable target ignites only
	 * when one of its six neighbours is flammable or a chest (:713-718) — the
	 * aCheckFlammability=T arm every burning-box call site passes. The 1.20.1
	 * carriers: {@code BlockState.getFlammability(BlockGetter, BlockPos, Direction)}
	 * (IForgeBlockState.java:485, the Forge flammability patch) and a plain
	 * {@code Blocks.FIRE} setBlock flag 3 (:711/:720 the {@code Blocks.fire} writes).
	 */
	public static boolean placeFire(Level aLevel, BlockPos aPos) {
		boolean tPlaced = placeFireInner(aLevel, aPos);
		if (tPlaced) sFiresSpreadTotal++; // the cumulative acceptance telemetry (the /gt6burner fires readout)
		return tPlaced;
	}

	/** The cumulative fire-drop count across the family since class load — the robust arm of the RCON 明火蔓延臂 (live fire is too transient to scan for). */
	public static long sFiresSpreadTotal = 0;

	/** The WD.fire body proper (the placeFire doc), split so the telemetry wrap stays one line. */
	private static boolean placeFireInner(Level aLevel, BlockPos aPos) {
		BlockState tState = aLevel.getBlockState(aPos);
		if (aLevel.getFluidState(aPos).is(net.minecraft.tags.FluidTags.LAVA) || tState.getBlock() == Blocks.FIRE) return false; // :708
		if (!tState.getCollisionShape(aLevel, aPos).isEmpty()) return false; // :709 the no-collision gate
		if (tState.getFlammability(aLevel, aPos, Direction.UP) > 0) { // :711 the flammable target
			aLevel.setBlock(aPos, Blocks.FIRE.defaultBlockState(), 3);
			return true;
		}
		// :713-718 — the aCheckFlammability=T arm: a flammable (or chest) neighbour ignites the cell
		for (Direction tSide : Direction.values()) {
			BlockPos tNeighbor = aPos.relative(tSide);
			BlockState tAdj = aLevel.getBlockState(tNeighbor);
			if (tAdj.is(Blocks.CHEST) || tAdj.is(Blocks.TRAPPED_CHEST)
					|| tAdj.getFlammability(aLevel, tNeighbor, tSide.getOpposite()) > 0) {
				aLevel.setBlock(aPos, Blocks.FIRE.defaultBlockState(), 3);
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the front face (the :113/:166 WD.hasCollide/liquid/oxygen/flaming gates)
	// ---------------------------------------------------------------------------

	/** The front face position (the :113 {@code getOffset(mFacing, 1)} form — one block out of the FRONT). */
	public BlockPos frontPos() {
		return getBlockPos().relative(Direction.from3DDataValue(mFacing));
	}

	/**
	 * The :113 gate — front face has air ({@code WD.hasCollide} = the collision-shape
	 * check), is not a liquid block, and has oxygen. WD.oxygen (WD.java:396-398) is the
	 * Galacticraft check, TRUE unconditionally without that mod — this port carries no
	 * Galacticraft, so the oxygen arm folds away (class doc).
	 */
	protected boolean frontFaceOpen() {
		if (!hasLevel()) return true; // the offline path (tests drive chargeFuel/useOnFront directly)
		Level tLevel = getLevel();
		BlockPos tFront = frontPos();
		BlockState tState = tLevel.getBlockState(tFront);
		return tState.getCollisionShape(tLevel, tFront).isEmpty() && !tState.liquid() && tLevel.getFluidState(tFront).isEmpty();
	}

	/** The :166 {@code WD.flaming} port (WD.java:699) — the front face holds a fire block. */
	protected boolean frontIsFlaming() {
		return hasLevel() && getLevel().getBlockState(frontPos()).getBlock() == Blocks.FIRE;
	}

	// ---------------------------------------------------------------------------
	// the front-face insert/extract (upstream onBlockActivated3 :180-217 — the
	// LH.NO_GUI_CLICK_TO_INVENTORY contract; the GTOvenBlock use precedent)
	// ---------------------------------------------------------------------------

	/**
	 * The :180-217 front click: only the FRONT face reacts (the Block.use caller gates
	 * that). Empty hand = take the output (burning hands hurt, :188 — the damage line is
	 * the entity pool, the move is not), then take the un-burnt fuel (:191-195, not
	 * while burning); held stack = insert into the empty fuel slot under the
	 * containsInput gate (:196-201 + canInsertItem2 :264), top up a matching fuel stack
	 * (:202-206), or drain back a matching output stack (:207-214). Every front click is
	 * consumed (:216 — no GUI ever opens).
	 */
	public boolean useOnFront(Player aPlayer, InteractionHand aHand) {
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		if (tHeld.isEmpty()) {
			// :185-190 — take the output
			ItemStack tOut = mInventory.getStackInSlot(1);
			if (!tOut.isEmpty()) {
				giveTo(aPlayer, aHand, tOut);
				mInventory.setStackInSlot(1, ItemStack.EMPTY);
				return true;
			}
			// :191-195 — take the fuel back, only while NOT burning
			ItemStack tFuel = mInventory.getStackInSlot(0);
			if (!mBurning && !tFuel.isEmpty()) {
				giveTo(aPlayer, aHand, tFuel);
				mInventory.setStackInSlot(0, ItemStack.EMPTY);
			}
			return true; // :216
		}
		// :196-201 — insert into the empty fuel slot (the canInsertItem2 :264 containsInput gate)
		if (mInventory.getStackInSlot(0).isEmpty()) {
			RecipeMapFurnaceFuel tMap = recipeMap();
			if (tMap != null && tMap.containsFuelInput(tHeld)) {
				// :198-199 — the whole held stack moves in (the :198 slot(0, aStack) + :199 null-hand form)
				ItemStack tInsert = tHeld.copy();
				tInsert.setCount(Math.min(tHeld.getCount(), Math.max(1, mInventory.getSlotLimit(0))));
				mInventory.setStackInSlot(0, tInsert);
				tHeld.shrink(tInsert.getCount());
			}
			return true; // :216 — consumed either way (no GUI)
		}
		// :202-206 — top up a matching fuel stack
		ItemStack tFuel = mInventory.getStackInSlot(0);
		if (ItemStack.isSameItemSameTags(tHeld, tFuel)) {
			int tRoom = Math.min(tHeld.getCount(), tFuel.getMaxStackSize() - tFuel.getCount());
			if (tRoom > 0) {
				tHeld.shrink(tRoom);
				tFuel.grow(tRoom);
			}
			return true;
		}
		// :207-214 — drain back a matching output stack
		ItemStack tOut = mInventory.getStackInSlot(1);
		if (ItemStack.isSameItemSameTags(tHeld, tOut)) {
			int tTake = Math.min(tOut.getCount(), tHeld.getMaxStackSize() - tHeld.getCount());
			if (tTake > 0) {
				tOut.shrink(tTake);
				tHeld.grow(tTake);
				if (tOut.isEmpty()) mInventory.setStackInSlot(1, ItemStack.EMPTY);
			}
			return true;
		}
		return true; // :216
	}

	/** The :186/:192 {@code player.inventory.setInventorySlotContents(currentItem, ...)} — the held-stack merge form. */
	private static void giveTo(Player aPlayer, InteractionHand aHand, ItemStack aStack) {
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		if (tHeld.isEmpty()) aPlayer.setItemInHand(aHand, aStack);
		else if (ItemStack.isSameItemSameTags(tHeld, aStack)) tHeld.grow(aStack.getCount());
		else aPlayer.getInventory().add(aStack);
	}

	// ---------------------------------------------------------------------------
	// the energy face family (upstream :269-275 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == mEnergyTypeEmitted; // :269
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return aSide == SIDE_TOP && super.isEnergyEmittingTo(aEnergyType, aSide, aTheoretical); // :270 SIDES_TOP
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		return Math.min(mRate, mEnergy); // :271
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mRate;} // :272

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return mRate;} // :273

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return mRate;} // :274

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeEmitted.AS_LIST; // :275
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the diesel/crank form) + the facing mirror
	// ---------------------------------------------------------------------------

	/** The offline test seam (the crank mAdjacencyOverride form). */
	@Nullable
	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	/** The per-tick adjacency resolver (protected — the Liquid/GAS subclasses share the tick emit path). */
	protected IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	/** The /setblock and placement paths: the state carries the facing, the BE mirror re-syncs at each tick head. */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			mFacing = (byte)getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
		}
	}

	public byte getFacing() {
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :64-74 / writeToNBT2 :76-82)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :79
		aNBT.putBoolean(NBT_ACTIVE, mBurning); // :80
		aNBT.putShort(NBT_EFFICIENCY, mEfficiency);
		aNBT.putLong(NBT_OUTPUT, mRate); // the row rate rides NBT for a clean round trip
		//? if forge {
		aNBT.put("gt.inv", mInventory.serializeNBT());
		//?} else {
		/*aNBT.put("gt.inv", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		*///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :67
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mBurning = aNBT.getBoolean(NBT_ACTIVE); // :68
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) mRate = Math.max(1, aNBT.getLong(NBT_OUTPUT)); // :70
		if (aNBT.contains(NBT_EFFICIENCY, Tag.TAG_ANY_NUMERIC)) {
			mEfficiency = (short)Math.max(0, Math.min(10000, aNBT.getShort(NBT_EFFICIENCY))); // :72 UT.Code.bind_(0, 10000, ...)
		}
		//? if forge {
		if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound("gt.inv"));
		//?} else {
		/*if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("gt.inv")); // 21.1: provider-first
		*///?}
	}
}
