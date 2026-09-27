package gregtech6.tileentity.multiblocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import gregapi.code.TagData;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.ore.GTBedrockOreBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.fluid.FluidTankGT;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.util.GTItemMover;

/**
 * The Bedrock Mining Drill multiblock controller (task p37-bedrock-drill) — the 1.20.1/1.21.1
 * port of gregtech/tileentity/multiblocks/MultiTileEntityBedrockDrill.java over
 * {@link TileEntityBase10MultiBlockBase} (Loader_MultiTileEntities.java:1283 re-read VERBATIM:
 * meta 17999, item 17101, "Bedrock Mining Drill Controller", MT.Ti, NBT_HARDNESS 9.0F ==
 * NBT_RESISTANCE 9.0F, NBT_TEXTURE "bedrockdrill", NBT_RECIPEMAP RM.BedrockOreList — the
 * display face only, the machine consumes no recipe map — NBT_ENERGY_ACCEPTED TD.Energy.RU).
 * The zh name 主基岩钻 (zh_cn_ref.tsv mte 17999).
 *
 * <p><b>The probe seam (the standing clause)</b>: this is the machine the declared-pattern
 * API cannot express — the y-5 base layer is TERRAIN (bedrock / bedrock ore read in place,
 * GTMultiBlockPattern.java:77-81 "Existence probes ... NOT expressible at all; the seam is
 * that binding is optional"). {@link #getStructurePattern()} stays null and
 * {@link #checkStructure2} is the hand-written form, upstream :82-120 verbatim: the y>=5
 * gate, the 3x3 y-5 probe collecting the ore materials into {@link #mList} (the large bedrock
 * ore counts its material TWICE, the small once — the duplication IS the yield weighting),
 * the bedrock-only floor gate (a non-bedrock non-ore cell fails the check), then the four
 * machine layers through
 * {@link ITileEntityMultiBlockController.Util#checkAndSetTargetOffset}: y-4 Bedrock Mining
 * Drill Heads (18103, NOTHING), y-3/y-2 Dense Titanium Walls (18026, ONLY_FLUID_IN), the
 * y-1 XOR ring — the four edge cells ((i==0) != (j==0)) are the ONLY_ENERGY_IN faces at
 * design 3, the centre+corners ONLY_FLUID_IN — and the y-0 wall ring under the controller.
 * The HBM coltan/oil arms (:95-105) are NEVER (the card ruling).
 *
 * <p><b>The production loop (:149-151)</b>: every server tick the held output moves to the
 * inventory above (the ST.move :152-154 arm); the cycle gate is {@code mEnergy >= 32768 &&
 * output empty && checkStructure(F) && tank drains 100} — then the energy deduct and the
 * two-path draw: {@code rng(128) < mList.size()} the ORE path (the material at the drawn
 * slot, 1/32 a random byproduct — the upstream UT.Code.select), else the STONE path
 * (1/1000 bedrock dust). The ore blocks are NEVER destroyed (the bedrock ore stays; the
 * machine is an infinite miner over the floor it reads).
 *
 * <p><b>Declared output deviations</b> (the p31-bedrock-ore faces): the port's ore-block
 * universe is per-(family,kind,material) over the 53-axis materials, so the ore-path yield
 * rides the P31 contract — the broken ore of the drawn {@link #mType} skin family when the
 * material is in the axis, the {@code gt6:dust_<material>} fallback otherwise (upstream
 * always has the meta block); the modded-dimension output skins (TF/ERE/ATUM/BTL, nether
 * netherrack) are CUT (no bedrock ore generates outside the overworld — P31 worldgen, so
 * the arms are unreachable); {@code mType} rolls over the port's 17 GT stones + the vanilla
 * stone + the deepslate slot (upstream stones.length 17 + 2, Loader_Rocks.java :56-:136).
 *
 * <p><b>The energy face (:274-293)</b>: RU in at recommended 2048 / min 1024 / max 4096,
 * store cap 40000, {@code doInject} accumulates {@code aAmount * |aSize|} under the cap and
 * overvoltage explodes (explode(6)); demanded 4096. The controller crafting row
 * "PYP"/"CMC"/"GIG" (Loader :1283) is CUT — 'P' Processor_Crystal_Ruby, 'Y' CONVEYERS[5]
 * and 'C' OD_CIRCUITS[6] have no port item identity (the W3 absent-input pool, the
 * implosion/graagg precedent).
 */
public class TileEntityBedrockDrill extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy {

	/** Upstream :61 — the 16000 L lubricant tank. */
	public static final long TANK_CAPACITY = 16000;
	/** Upstream :155 — the cycle energy deduct (32768 RU per produced block). */
	public static final long ENERGY_PER_CYCLE = 32768;
	/** Upstream :275/:291 — the store cap (doInject refuses above, getEnergyCapacity reports). */
	public static final long ENERGY_CAPACITY = 40000;
	/** Upstream :286 — the packet-train demand. */
	public static final long ENERGY_DEMANDED = 4096;
	/** Upstream :287-289 — the RU input window literals. */
	public static final long INPUT_RECOMMENDED = 2048, INPUT_MIN = 1024, INPUT_MAX = 4096;
	/** Upstream :155 — the lubricant per cycle. */
	public static final long LUBE_PER_CYCLE = 100;
	/** Upstream :158 — the mType re-roll chance arm (rng(1000) == 0). */
	public static final int RETYPE_RNG = 1000;
	/** Upstream :160 — the ore/stone draw scale (rng(128) < mList.size). */
	public static final int SELECTOR_RNG = 128;
	/** Upstream :163 — the byproduct chance (rng(32) == 0). */
	public static final int BYPRODUCT_RNG = 32;
	/** Upstream :207 — the bedrock dust chance arm of the stone path. */
	public static final int BEDROCK_DUST_RNG = 1000;
	/** The mType domain: the port's 17 GT stones + the vanilla stone slot + the deepslate slot (upstream :158 stones.length + 2, deepslate always exists in 1.20.1). */
	public static final int TYPE_DOMAIN = GTStoneBlocks.STONES.size() + 2;
	/** The GT-stone family index base in {@link GT6OreBlocks#FAMILIES} (5 three-form vanilla anchors + 4 two-form dust families precede the 17 GT stones). */
	public static final int GT_STONE_FAMILY_BASE = 9;

	/** Upstream NBT_ENERGY (:76) in the W3 generator spelling convention. */
	public static final String NBT_ENERGY = "gt.energy";
	/** Upstream NBT_VALUE (:77) — the output skin type. */
	public static final String NBT_TYPE = "gt.type";
	/** The tank key (the base machine NBT_TANK+"."+0 spelling, :78/:70). */
	public static final String NBT_TANK = "gt.tank.0";
	/** The held output (:152 slot 0). */
	public static final String NBT_OUTPUT = "gt.output";

	/** Upstream :60 — the registration column, read-only final (the graagg form). */
	public final TagData mEnergyTypeAccepted = TD.Energy.RU;

	/** Upstream :58. Persists. */
	public long mEnergy = 0;
	/** Upstream :59 — the output skin type slot. Persists. */
	public int mType = 0;
	/** Upstream :61 — the lubricant tank (fill gate :295 lubricant only). */
	public final FluidTankGT mTank = new FluidTankGT(TANK_CAPACITY);
	/** Upstream :62 — the collected floor materials, duplicates included (the yield weighting). Rebuilt by every structure check. */
	public final List<OreDictMaterial> mList = new ArrayList<>();
	/** The held output (:152 slot 0) — extracted from any side, auto-moved up. */
	@Nullable
	protected ItemStack mOutput = null;

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + the offline test seam, the graagg form)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of. */
	public TileEntityBedrockDrill(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the frozen-registry seam). */
	public TileEntityBedrockDrill(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTMultiBlocks.BEDROCK_DRILL_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_bedrock_drill";
	}

	/** The y-4 drill head layer (upstream part 18103 — the existing GTMultiBlocks part row). */
	protected Block getDrillHeadBlock() {
		return GTMultiBlocks.anyPartBlock("bedrock_drill_head");
	}

	/** The wall layers (upstream part 18026, Dense Titanium Wall). */
	protected Block getWallBlock() {
		return GTMultiBlocks.anyPartBlock("dense_wall_titanium");
	}

	/** The bedrock floor judgement (:106 WD.bedrock — vanilla bedrock; the BTL compat arm is cut). */
	protected boolean isBedrock(BlockState aState) {
		return aState.is(Blocks.BEDROCK);
	}

	// ---------------------------------------------------------------------------
	// the hand-written structure (:82-120 — the probe seam, no declared pattern)
	// ---------------------------------------------------------------------------

	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		return null; // the probe-style machine keeps its hand-written check — GTMultiBlockPattern.java seam
	}

	/** Upstream :82-120 verbatim over the Util offset walk. */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay;
		if (getBlockPos().getY() < 5) return false; // :83
		mList.clear(); // :84
		boolean tSuccess = true, tBedrock = true; // :85 (tOverride rides the HBM arms — never)
		Block tHead = getDrillHeadBlock(), tWall = getWallBlock();
		for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
			BlockState tState = getLevel().getBlockState(getBlockPos().offset(i, -5, j)); // :88 getBlockOffset(i, -5, j)
			if (tState.getBlock() instanceof GTBedrockOreBlock tOre) {
				// :89-91 large counts twice, :92-94 small once — the duplication is the weighting
				mList.add(tOre.material);
				if (!tOre.small) mList.add(tOre.material);
			} else if (!isBedrock(tState)) {
				tBedrock = false; // :106-107
			}
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, -4, j, tHead, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :109
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, -3, j, tWall, 0, MultiBlockPartBlockEntity.ONLY_FLUID_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :110
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, -2, j, tWall, 0, MultiBlockPartBlockEntity.ONLY_FLUID_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :111
			if ((i == 0) != (j == 0)) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, -1, j, tWall, 3, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :113 the energy ring, design 3
			} else {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, -1, j, tWall, 0, MultiBlockPartBlockEntity.ONLY_FLUID_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :115
			}
			if (!ITileEntityMultiBlockController.Util.checkAndSetTargetOffset(this, i, 0, j, tWall, 0, MultiBlockPartBlockEntity.ONLY_FLUID_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :117 (the centre is the controller self-cell pass)
		}
		return tSuccess && tBedrock; // :119 (the tOverride arm never fires)
	}

	/** Upstream :144-146 verbatim — the controller-anchored 3x3x6 box (y-5..y). */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tPos = getBlockPos();
		return aX >= tPos.getX() - 1 && aY >= tPos.getY() - 5 && aZ >= tPos.getZ() - 1
				&& aX <= tPos.getX() + 1 && aY <= tPos.getY() && aZ <= tPos.getZ() + 1;
	}

	// ---------------------------------------------------------------------------
	// the production loop (:149-251)
	// ---------------------------------------------------------------------------

	/** The live rng (:158/:160/:163 — the lightning-rod seam form). */
	protected int rng(int aRange) {
		if (aRange <= 0) return 0;
		if (mRngOverride != null) return mRngOverride.getAsInt();
		return hasLevel() ? getLevel().random.nextInt(aRange) : 0;
	}

	@Nullable
	private java.util.function.IntSupplier mRngOverride = null;

	/** The offline rng seam. */
	public void setRngOverride(@Nullable java.util.function.IntSupplier aRng) {
		mRngOverride = aRng;
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide) return;
		if (hasOutput()) moveOutputUp(); // :152-154 the ST.move arm
		if (mEnergy >= ENERGY_PER_CYCLE && !hasOutput() && checkStructure(false) && drainLube()) {
			mEnergy -= ENERGY_PER_CYCLE; // :156
			if (rng(RETYPE_RNG) == 0) mType = rng(TYPE_DOMAIN); // :157-158
			mOutput = draw(rng(SELECTOR_RNG)); // :160-248
			setChanged();
		}
	}

	/** Upstream :155 mTank.drainAll(100) — drains exactly 100 iff at least 100 held. */
	boolean drainLube() {
		if (!mTank.has(LUBE_PER_CYCLE)) return false;
		mTank.drain((int)LUBE_PER_CYCLE, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
		return true;
	}

	boolean hasOutput() {
		return mOutput != null && !mOutput.isEmpty();
	}

	/**
	 * The pure two-path draw (:160-248) — the offline seam. aSelector is the drawn
	 * {@code rng(128)}; the byproduct and retype arms read the rng seam themselves.
	 */
	@Nullable
	public ItemStack draw(int aSelector) {
		if (aSelector < mList.size()) {
			// :161-163 — the material at the slot, 1/32 a random byproduct (UT.Code.select)
			OreDictMaterial tMaterial = mList.get(aSelector);
			if (rng(BYPRODUCT_RNG) == 0 && !tMaterial.mByProducts.isEmpty()) {
				tMaterial = tMaterial.mByProducts.get(rng(tMaterial.mByProducts.size()));
			}
			ItemStack tStack = brokenOreItem(tMaterial);
			return tStack == null ? dustItem(tMaterial) : tStack; // the P31 dust fallback
		}
		// :203-247 — the stone path (the modded-dimension arms cut, see the class doc)
		if (rng(BEDROCK_DUST_RNG) == 0) return dustItem(MT.Bedrock); // :205-207
		return stoneSkinItem();
	}

	/**
	 * The ore-path skin (:174-201): the broken ore of the family the {@link #mType} slot
	 * names — the 17 GT stone families, the vanilla stone slot and the deepslate slot.
	 * Null when the pair has no block (out-of-axis materials ride the dust fallback).
	 */
	@Nullable
	ItemStack brokenOreItem(OreDictMaterial aMaterial) {
		GT6OreBlocks.OreFamily tFamily = mType < GTStoneBlocks.STONES.size()
				? GT6OreBlocks.FAMILIES.get(GT_STONE_FAMILY_BASE + mType)      // the GT stone slot
				: (mType == GTStoneBlocks.STONES.size() ? stoneFamily() : deepslateFamily()); // :201 the vanilla-stone / deepslate slots
		var tHandle = GT6OreBlocks.get(tFamily, GT6OreBlocks.FormKind.BROKEN, aMaterial);
		return tHandle == null ? null : new ItemStack(tHandle.get());
	}

	/** The stone-path skin (:236-246): GT stone cobble / vanilla cobblestone / cobbled deepslate. */
	@Nullable
	ItemStack stoneSkinItem() {
		if (mType < GTStoneBlocks.STONES.size()) {
			var tItem = GTStoneBlocks.item(GTStoneBlocks.STONES.get(mType).snake(), StoneVariant.COBBL);
			return tItem == null ? null : new ItemStack(tItem.get()); // :238 stones[mType] meta 1 (cobble)
		}
		return mType == GTStoneBlocks.STONES.size() ? new ItemStack(Items.COBBLESTONE) : new ItemStack(Items.COBBLED_DEEPSLATE); // :242/:246
	}

	@Nullable
	ItemStack dustItem(OreDictMaterial aMaterial) {
		var tHandle = GTMaterialItems.get(OP.dust, aMaterial);
		return tHandle == null ? null : new ItemStack(tHandle.get());
	}

	static GT6OreBlocks.OreFamily stoneFamily() {
		return GT6OreBlocks.FAMILIES.get(0);
	}

	static GT6OreBlocks.OreFamily deepslateFamily() {
		return GT6OreBlocks.FAMILIES.get(1);
	}

	/**
	 * The :152-154 output move — the held stack to the inventory above through the item
	 * adjacency face (the ST.move defaults: up to one stack per tick). The source view is
	 * the extract-only {@link DrillOutputHandler} — the move consumes through the same
	 * face a hopper below would.
	 */
	protected void moveOutputUp() {
		if (!hasLevel() || mOutput == null) return;
		BlockPos tAbove = getBlockPos().above();
		if (!getLevel().isLoaded(tAbove)) return;
		net.minecraft.world.level.block.entity.BlockEntity tTE = getLevel().getBlockEntity(tAbove);
		if (tTE == null || tTE.isRemoved()) return;
		//? if forge {
		net.minecraftforge.items.IItemHandler tTarget = tTE.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
		//?} else {
		/*net.minecraftforge.items.IItemHandler tTarget = getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, tAbove, Direction.DOWN);
		 *///?}
		if (tTarget == null) return;
		if (GTItemMover.move(new DrillOutputHandler(this), tTarget) > 0) setChanged();
	}

	// ---------------------------------------------------------------------------
	// the energy face (:274-293 — the RU input window)
	// ---------------------------------------------------------------------------

	/** Upstream :274-281 verbatim. */
	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mEnergy > ENERGY_CAPACITY) return 0; // :275
		aSize = Math.abs(aSize);
		if (!aDoInject) return aAmount; // :277
		if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) {explode(6); return aAmount;} // :278 — the port explode(double) strength face
		mEnergy += aAmount * aSize; // :279
		return aAmount;
	}

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :283
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :292
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return ENERGY_DEMANDED; // :286
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return INPUT_RECOMMENDED; // :287
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return INPUT_MIN; // :288
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return INPUT_MAX; // :289
	}

	// ---------------------------------------------------------------------------
	// the fluid face (:295-300 — fill lubricant only, drain the tank) and the item face
	// ---------------------------------------------------------------------------

	/** The resolved lubricant (lazy; the gt6:lubricant registry id, the resolver seam shape). */
	@Nullable
	private Fluid mLubricant = null;

	/** Upstream :295/:298 — the FluidsGT.LUBRICANT fill gate. */
	protected boolean isLubricant(@Nullable net.minecraftforge.fluids.FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return false;
		if (mLubricant == null) {
			//? if forge {
			mLubricant = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(new ResourceLocation("gt6", "lubricant"));
			//?} else {
			/*mLubricant = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("gt6", "lubricant"));
			 *///?}
		}
		return aFluid.getFluid() == mLubricant;
	}

	//? if forge {
	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
			return net.minecraftforge.common.util.LazyOptional.of(() -> new DrillFluidHandler(this)).cast(); // the fresh-wrapper-per-call form
		}
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
			return net.minecraftforge.common.util.LazyOptional.of(() -> new DrillOutputHandler(this)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?}

	/** The lubricant tank face (:295-300): fill gated to lubricant, drain open. */
	public static final class DrillFluidHandler implements net.minecraftforge.fluids.capability.IFluidHandler {

		private final TileEntityBedrockDrill mDrill;

		public DrillFluidHandler(TileEntityBedrockDrill aDrill) {
			mDrill = aDrill;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public net.minecraftforge.fluids.FluidStack getFluidInTank(int aTank) {
			net.minecraftforge.fluids.FluidStack tFluid = mDrill.mTank.fluid();
			return tFluid == null ? net.minecraftforge.fluids.FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			return aTank == 0 ? mDrill.mTank.getCapacity() : 0;
		}

		@Override
		public boolean isFluidValid(int aTank, net.minecraftforge.fluids.FluidStack aStack) {
			return mDrill.isLubricant(aStack);
		}

		@Override
		public int fill(net.minecraftforge.fluids.FluidStack aResource, FluidAction aAction) {
			return mDrill.isLubricant(aResource) ? mDrill.mTank.fill(aResource, aAction) : 0; // :295
		}

		@Override
		public net.minecraftforge.fluids.FluidStack drain(int aMaxDrain, FluidAction aAction) {
			return mDrill.mTank.drain(aMaxDrain, aAction); // :296
		}

		@Override
		public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack aResource, FluidAction aAction) {
			return mDrill.mTank.drain(aResource, aAction);
		}
	}

	/** The output face (:306-308): one extract-only slot. */
	public static final class DrillOutputHandler implements net.minecraftforge.items.IItemHandler {

		private final TileEntityBedrockDrill mDrill;

		public DrillOutputHandler(TileEntityBedrockDrill aDrill) {
			mDrill = aDrill;
		}

		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int aSlot) {
			return aSlot == 0 && mDrill.mOutput != null ? mDrill.mOutput : ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
			return aStack; // :307 canInsertItem2 = F
		}

		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
			if (aSlot != 0 || aAmount <= 0 || mDrill.mOutput == null || mDrill.mOutput.isEmpty()) return ItemStack.EMPTY;
			int tCount = Math.min(aAmount, mDrill.mOutput.getCount());
			ItemStack rStack = mDrill.mOutput.copy();
			rStack.setCount(tCount);
			if (!aSimulate) {
				mDrill.mOutput.shrink(tCount);
				if (mDrill.mOutput.isEmpty()) mDrill.mOutput = null;
				mDrill.setChanged();
			}
			return rStack;
		}

		@Override
		public int getSlotLimit(int aSlot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int aSlot, ItemStack aStack) {
			return false;
		}
	}

	// ---------------------------------------------------------------------------
	// NBT (:64-79 — energy, type, tank; the output rides the slot persistence)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy);
		aNBT.putInt(NBT_TYPE, mType);
		mTank.writeToNBT(aNBT, NBT_TANK);
		if (mOutput != null && !mOutput.isEmpty()) aNBT.put(NBT_OUTPUT, mOutput.save(new CompoundTag()));
	}
	//?} else {
	/* // 21.1 NBT face: only the ItemStack IO forks onto the provider (the 10-machine
	   // saveAdditional fork form, the frozen builtin NBT_ACCESS view).
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy);
		aNBT.putInt(NBT_TYPE, mType);
		mTank.writeToNBT(aNBT, NBT_TANK);
		if (mOutput != null && !mOutput.isEmpty()) aNBT.put(NBT_OUTPUT, mOutput.save(NBT_ACCESS, new CompoundTag()));
	}
	 *///?}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		mEnergy = aNBT.getLong(NBT_ENERGY);
		mType = aNBT.getInt(NBT_TYPE);
		mTank.readFromNBT(aNBT, NBT_TANK);
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_COMPOUND)) {
			//? if forge {
			mOutput = ItemStack.of(aNBT.getCompound(NBT_OUTPUT));
			//?} else {
			/*mOutput = ItemStack.parseOptional(NBT_ACCESS, aNBT.getCompound(NBT_OUTPUT));
			 *///?}
		}
	}
}
