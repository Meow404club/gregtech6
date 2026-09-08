package gregtech6.tileentity.tools;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.tileentity.temperature.ITileEntityTemperature;
import gregapi.util.CruciblePhysics;
import gregapi.util.UT;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.MaterialStackNBT;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Mold — task p26-crucible-physics-smeltery spec ⑤ (the
 * A-card minimal face, ported from gregtech/tileentity/tools/MultiTileEntityMold.java:
 * the :74 class face), the crucible's casting partner: pour molten material in, let it
 * cool past the melting point, take the solidified shape out.
 *
 * <p><b>The tick (upstream onServerTickPost :156-211)</b>: the ±5 K drift toward the
 * environment (:160 — the mold has NO energy face, it is a heat sink); the content
 * solidifies when the temperature falls below the material's melting point (:189-202 —
 * the material swaps to mTargetSolidifying and the {@link #getMoldRecipe} prefix pours
 * the item into the output slot); an over-hot mold (above the content's boiling point or
 * above {@link #getMoldMaxTemperature}) trashes the content and melts into lava
 * (:179-186).
 *
 * <p><b>The shape</b>: the 5x5 {@code gt.mold} bitmask NBT (:95/:109) maps through
 * {@link #MOLD_RECIPES} to the output prefix with the OP.nugget fallback (:79-83). The
 * card face registers the INGOT bar shapes (the :690-694 3-wide x 5-tall bars, the three
 * column shifts) and the nugget fallback; the chisel re-carving face and the remaining
 * shape universe are the card-B surface. Declared deviation: the Stone Mold item ships
 * PRE-CARVED with the ingot bar (the row0 playable arm — the A-card acceptance chain
 * needs no chisel gymnastics; the bitmask contract itself is upstream-verbatim).
 *
 * <p><b>The pour</b>: {@link #fillMold} (:246-264 verbatim) requires an empty mold, an
 * input side, an acid-free material and a representable output, then takes exactly the
 * required amount (units(requiredUnits, U, mTargetSolidifying.mAmount, T)); the mold
 * right-click drives the adjacent {@link ITileEntityCrucible#fillMoldAtSide} (:267-294)
 * and the output hand-take (:296-322). The wrench auto-pull arm (:170-176) is card B.
 */
public class TileEntityMold extends TileEntityBase03TicksAndSync implements ITileEntityTemperature, ITileEntityMold {

	/** The shape NBT key (upstream :95/:109 verbatim 'gt.mold'). */
	public static final String NBT_MOLD = "gt.mold";
	/** The temperature NBT key (the Smeltery spelling). */
	public static final String NBT_TEMPERATURE = "gt.temperature";

	/** The upstream :75 form bonus over the shell material. */
	public static final double HEAT_RESISTANCE_BONUS = 1.25;

	/** Upstream :89 — the offline default (DEF_ENV_TEMP = C + 20). */
	public static final long DEF_ENV_TEMP = 293;

	/** The port side order: UP = 1 (the HuEnergyHandshake truth table). */
	public static final byte SIDE_TOP = 1;

	/** Upstream :81 — B[25]-1, the 25-bit shape mask of getMoldRecipe. */
	public static final int SHAPE_MASK = (1 << 25) - 1;

	/**
	 * Upstream :77 — the shape→prefix map. The card face registers the three ingot-bar
	 * shifts (MultiTileEntityMold.java:690-694); getMoldRecipe answers OP.nugget for any
	 * other non-zero shape.
	 */
	public static final Map<Integer, OreDictPrefix> MOLD_RECIPES = new HashMap<>();

	/** The ingot bar bitmask at column shift i (the :690-694 loop form). */
	public static int ingotShape(int i) {
		int rShape = 0;
		for (int row = 0; row < 5; row++) rShape |= 0b111 << (row * 5 + i);
		return rShape;
	}

	static {
		for (int i = 0; i < 3; i++) MOLD_RECIPES.put(ingotShape(i), OP.ingot);
	}

	/** The upstream :79-83 lookup — the nugget fallback answers every unknown non-zero shape. */
	@Nullable
	public static OreDictPrefix getMoldRecipe(int aShape) {
		if (aShape == 0) return null;
		OreDictPrefix rRecipe = MOLD_RECIPES.get(aShape & SHAPE_MASK);
		return rRecipe == null ? OP.nugget : rRecipe;
	}

	// public like the family BEs (the GTGeneratorSolidBlockEntity field face) — the
	// command/stat readers and the offline tests consume them directly
	public int mShape = 0;
	public long mTemperature = DEF_ENV_TEMP;
	@Nullable
	public OreDictMaterialStack mContent = null;

	/** The output slot (upstream slot 0, :199/:296). */
	public final GTItemStackAdapter mInventory = new GTItemStackAdapter();

	/**
	 * The BET-injecting ctor (the GTGeneratorSolidBlockEntity form; the offline fixtures
	 * pass a synthetic type over a vanilla block state).
	 */
	public TileEntityMold(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6Molds.MOLD_BE.get(), aPos, aState);
		if (getBlockState().getBlock() instanceof GT6Molds.MoldBlock tBlock && tBlock.row().preCarvedShape() != 0) {
			mShape = tBlock.row().preCarvedShape(); // the pre-carved stone mold (class doc deviation)
		}
	}

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the GTBarrelBlockEntity form: the self-reference stays out of the registry initializer). */
	public TileEntityMold(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gt6.mold";
	}

	/** The shell material (the :230 mMaterial) — rides the block carrier. */
	@Nullable
	public OreDictMaterial material() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Molds.MoldBlock tBlock) return tBlock.row().material();
		return null;
	}

	/** Upstream :229-231 — the mold ceiling. */
	@Override
	public long getMoldMaxTemperature() {
		OreDictMaterial tMaterial = material();
		return (long)((tMaterial == null ? MT.Stone : tMaterial).mMeltingPoint * HEAT_RESISTANCE_BONUS);
	}

	// ------------------------------------------------------------------------------------
	// the tick (upstream onServerTickPost :156-211)
	// ------------------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		long tEnvTemperature = envTemp();

		// :160 — the ±5 K drift toward the environment
		if (mTemperature > tEnvTemperature) mTemperature -= Math.min(5, mTemperature - tEnvTemperature);
		else if (mTemperature < tEnvTemperature) mTemperature += Math.min(5, tEnvTemperature - mTemperature);

		// :162-167 — the empty-content reset (the display census is the card-B render face)
		if (mContent != null && mContent.mAmount <= 0 && mInventory.isEmpty()) {
			mContent = null;
		}

		if (mContent != null) {
			// :179-186 — the over-heat destruction (boiling content or shell ceiling) → lava
			if (mTemperature > mContent.mMaterial.mBoilingPoint || mTemperature > getMoldMaxTemperature()) {
				fizz();
				mContent = null;
				mInventory.clear();
				if (hasLevel()) getLevel().setBlock(getBlockPos(), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
				return;
			}
			// :189-203 — the solidifying gate: below the melting point the shape pours out
			if (mTemperature < mContent.mMaterial.mMeltingPoint) {
				mContent.mMaterial = mContent.mMaterial.mTargetSolidifying.mMaterial;
				if (mContent.mAmount > 0 && mInventory.isEmpty()) {
					OreDictPrefix tPrefix = getMoldRecipe(mShape);
					if (tPrefix != null) {
						ItemStack tOutput = GT6RecipeMapCrucible.matStack(tPrefix, mContent.mMaterial, mContent.mAmount / tPrefix.mAmount); // :199
						if (tOutput != null) {
							mInventory.set(tOutput);
							mContent.mAmount = 0; // :200
						}
					}
				}
			}
		}
	}

	/** The SFX fizz arm (the vanilla LevelEvent bridge, the Smeltery form). */
	protected void fizz() {
		if (hasLevel()) getLevel().levelEvent(1501, getBlockPos(), 0);
	}

	// ------------------------------------------------------------------------------------
	// the mold face (ITileEntityMold, upstream :223-264)
	// ------------------------------------------------------------------------------------

	@Override
	public boolean isMoldInputSide(byte aSide) {
		return aSide == SIDE_TOP || (aSide >= 2 && aSide <= 5); // :224-226 SIDES_TOP_HORIZONTAL
	}

	@Override
	public long getMoldRequiredMaterialUnits() {
		OreDictPrefix tPrefix = getMoldRecipe(mShape);
		if (tPrefix == null) return 0;
		if (tPrefix == OP.nugget) { // :237-241 — the per-bit nugget census
			long rAmount = 0;
			for (int i = 0; i < 25; i++) if ((mShape & (1 << i)) != 0) rAmount += CS.U9;
			return rAmount;
		}
		return tPrefix.mAmount; // :242
	}

	@Override
	public long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
		if (aMaterial == null || aMaterial.mMaterial == null || aMaterial.mMaterial.contains(gregapi.data.TD.Properties.ACID)) return 0; // :247
		OreDictPrefix tPrefix = getMoldRecipe(mShape);
		if (tPrefix != null && mContent == null && mInventory.isEmpty() && isMoldInputSide(aSide) && aMaterial.mAmount > 0) { // :249
			if (GT6RecipeMapCrucible.matStack(tPrefix, aMaterial.mMaterial.mTargetSolidifying.mMaterial, 1) == null) return 0; // :254 the representable-output gate
			long tRequiredAmount = getMoldRequiredMaterialUnits();
			long rAmount = CruciblePhysics.units(tRequiredAmount, CS.U, aMaterial.mMaterial.mTargetSolidifying.mAmount, true); // :255
			if (aMaterial.mAmount >= rAmount) {
				mContent = new OreDictMaterialStack(aMaterial.mMaterial, tRequiredAmount); // :257
				mTemperature = aTemperature; // :258
				return rAmount;
			}
		}
		return 0;
	}

	// ------------------------------------------------------------------------------------
	// the temperature face (upstream :213-221)
	// ------------------------------------------------------------------------------------

	@Override
	public long getTemperatureValue(byte aSide) {
		return mTemperature;
	}

	@Override
	public long getTemperatureMax(byte aSide) {
		return getMoldMaxTemperature();
	}

	/** The {@link ITileEntityTemperature.EnvTemp} live form (the Smeltery formula, WD.envTemp :404-406). */
	public long envTemp() {
		if (!hasLevel()) return DEF_ENV_TEMP;
		BlockPos tPos = getBlockPos();
		float tBiomeTemp = getLevel().getBiome(tPos).value().getBaseTemperature();
		return Math.max(1, 273 - 3 + (long)(tBiomeTemp * 20));
	}

	// ------------------------------------------------------------------------------------
	// the world interaction (upstream onBlockActivated3 :267-294 + pickUpItem :296-322)
	// ------------------------------------------------------------------------------------

	/**
	 * The top-face click: an output item hands over (the :305 burn damage is the entity
	 * pool), an empty-handed empty mold drives the adjacent-crucible pour (:271-289).
	 */
	public boolean useTop(Player aPlayer, InteractionHand aHand) {
		if (!isServerSide() || aPlayer == null) return true;

		// :296-322 — the output hand-over
		ItemStack tOutput = mInventory.get();
		if (!tOutput.isEmpty()) {
			if (aPlayer.getInventory().add(tOutput.copy())) mInventory.clear();
			return true;
		}

		// :271-289 — the pour: scan the mold's input sides for a crucible and pull from it
		BlockPos tPos = getBlockPos();
		for (Direction tSide : Direction.values()) {
			if (tSide == Direction.DOWN) continue; // SIDES_TOP_HORIZONTAL: up + 4 horizontals
			BlockEntity tNeighbor = getLevel().getBlockEntity(tPos.relative(tSide));
			if (tNeighbor instanceof ITileEntityCrucible tCrucible) {
				byte tSideOfCrucible = (byte)tSide.getOpposite().get3DDataValue();
				byte tSideOfMold = (byte)tSide.get3DDataValue();
				tCrucible.fillMoldAtSide(this, tSideOfCrucible, tSideOfMold); // :276/:285
				return true;
			}
		}
		return true; // :291 — the top click is always consumed
	}

	// ------------------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :92-111)
	// ------------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putInt(NBT_MOLD, mShape); // :109
		aNBT.putLong(NBT_TEMPERATURE, mTemperature); // :108 (UT.NBT.setNumber — the long form)
		if (mContent != null) MaterialStackNBT.save(mContent, aNBT); // :110 NBT_MATERIALS
		//? if forge {
		aNBT.put("gt.inv", mInventory.serializeNBT());
		//?} else {
		/*aNBT.put("gt.inv", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MOLD, Tag.TAG_ANY_NUMERIC)) mShape = aNBT.getInt(NBT_MOLD); // :95
		if (aNBT.contains(NBT_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) mTemperature = aNBT.getLong(NBT_TEMPERATURE); // :99
		mContent = MaterialStackNBT.load(aNBT); // :100
		if (mContent != null && mContent.mAmount <= 0) mContent = null;
		//? if forge {
		if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound("gt.inv"));
		//?} else {
		/*if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("gt.inv")); // 21.1: provider-first
		 *///?}
	}

	// ------------------------------------------------------------------------------------
	// the tiny slot adapter (the mold has ONE output slot; the ItemStackHandler form)
	// ------------------------------------------------------------------------------------

	/** The one-slot adapter over ItemStackHandler semantics (isEmpty/set/clear). */
	public static final class GTItemStackAdapter extends net.minecraftforge.items.ItemStackHandler {
		public GTItemStackAdapter() {
			super(1);
		}

		public boolean isEmpty() {
			return getStackInSlot(0).isEmpty();
		}

		public ItemStack get() {
			return getStackInSlot(0);
		}

		public void set(ItemStack aStack) {
			setStackInSlot(0, aStack);
		}

		public void clear() {
			setStackInSlot(0, ItemStack.EMPTY);
		}
	}
}
