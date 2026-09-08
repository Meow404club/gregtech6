package gregtech6.tileentity.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.tileentity.temperature.ITileEntityTemperature;
import gregapi.util.CruciblePhysics;
import gregapi.util.UT;
import gregtech6.fluid.FluidBridge;
import gregtech6.recipes.maps.GT6RecipeMapCanner;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.MaterialStackNBT;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 small Smeltery — task p26-crucible-physics-smeltery,
 * ported from gregtech/tileentity/tools/MultiTileEntitySmeltery.java (the :75 class face)
 * as the row0 crucible: a single-block material pile heated by raw HU. THE PHYSICS IS
 * {@link CruciblePhysics} (the shared pure core, class doc there) — this BE is the World
 * adapter: the item suck (:154), the feed ladder (:158-183 incl. the :167-183 ore
 * direct-smelt), the alloy scan + consumption (:186-244), the phase loop world effects
 * (:246-284), the heat/cool step (:301-313), the melt-down (:315-327) and the world
 * interaction (:412-507).
 *
 * <p><b>Energy face (upstream :688-698, the HU leg only)</b>: accepts HU from ALL sides
 * ({@code isEnergyAcceptingFrom} :692 verbatim — the burning box EMITS from its TOP face
 * into whatever neighbor sits there, GTGeneratorSolidBlockEntity.isEnergyEmittingTo, so a
 * burning box placed BESIDE/BELOW the crucible injects through the shared face; the
 * emission direction is the p13 HuEnergyHandshake truth, not a guess), credits
 * {@code mEnergy += aSize * aAmount} inside {@link #doInject} (:693 — the KU/CU legs are
 * the defer pool), demands {@code Long.MAX_VALUE - mEnergy} (:694) and takes any packet
 * size (min 1 / recommended 2048 / max MAX_VALUE, :695-697).
 *
 * <p><b>No GUI (upstream census)</b>: the top-face click IS the interface — empty hand
 * takes the feed slot (:419-424) or scoops SCRAP from the cooled lightest content
 * (:426-448, with the burn-hand exhaust), a bucket drains molten material (:450-468, the
 * temperature gate) or pours a molten fluid back in (:469-490, the bind(melting+25,
 * boiling-1) gate).
 *
 * <p><b>Declared deviations (all pool/declared)</b>: entity temperature damage rides the
 * UT.Entities pool (the phase-loop gas damage outcome is measured, the application is a
 * no-op this card); rain collection (:147-152), the entity-egg walkover arm (:622-650)
 * and the tool/thermometer faces (:510-537) are the defer pool; the collision-box hollow
 * shape (:652-662) is pool — the suck box therefore extends OVER the top face (see
 * {@link #suckTopItem}); the fire-spread RNG of the melt-down (:319) uses the vanilla
 * {@code GTGeneratorSolidBlockEntity.placeFire} bridge with the same volume; the fluid
 * pour-back accepts the FluidBridge molten fluids only (the upstream FLUID_MAP universe
 * is the Phase-2 fluid module pool).
 */
public class TileEntitySmeltery extends TileEntityBase03TicksAndSync implements ITileEntityEnergy, ITileEntityTemperature, ITileEntityCrucible, ITileEntityMold {

	/** Upstream :76 — the flame/gas radius of the small form. */
	public static final long GAS_RANGE = 3, FLAME_RANGE = 3;

	/** Upstream :83 — the NBT keys (CS.java:1130-1142 spellings). */
	public static final String NBT_TEMPERATURE = "gt.temperature";

	/** Upstream :83 DEF_ENV_TEMP = C + 20 = 293 (CS.java:135, 293.15 IRL) — the offline default. */
	public static final long DEF_ENV_TEMP = 293;

	/** The vanilla 273 K constant (CS.java:132 C) — the env-temp formula offset. */
	public static final long C = 273;

	/** Upstream :84 — the material pile. */
	public final List<OreDictMaterialStack> mContent = new ArrayList<>();

	/** Upstream :83 — the energy buffer (HU), the temperature and the previous tick's. */
	public long mEnergy = 0, mTemperature = DEF_ENV_TEMP, oTemperature = 0;

	/** Upstream :81-82 — the supply cooldown, the meltdown warning latch and the acidproof flag. */
	public int mCooldown = 100;
	public boolean mMeltDown = false, mAcidProof = false;

	/** Upstream :682 — the single feed slot (top face only, :684). */
	public final GTItemStackHandler mInventory = new GTItemStackHandler(1, this::setChanged);

	/** The vanilla-ore bridge for the feed ladder (the OM.anydata counterpart for un-oredicted vanilla ores; declared minimal set, the oredict universe rides MaterialPrefixItem). */
	public static final Map<net.minecraft.world.level.ItemLike, OreDictMaterial> VANILLA_ORES = Map.of(
			net.minecraft.world.item.Items.IRON_ORE, MT.Fe,
			net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE, MT.Fe,
			net.minecraft.world.item.Items.RAW_IRON, MT.Fe,
			net.minecraft.world.item.Items.GOLD_ORE, MT.Au,
			net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE, MT.Au,
			net.minecraft.world.item.Items.RAW_GOLD, MT.Au,
			net.minecraft.world.item.Items.COPPER_ORE, MT.Cu,
			net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE, MT.Cu,
			net.minecraft.world.item.Items.RAW_COPPER, MT.Cu);

	/**
	 * The BET-injecting ctor (the GTGeneratorSolidBlockEntity form): the registration
	 * lambda passes the family BET, the offline fixtures pass a synthetic type over a
	 * vanilla block state (the intrusive-holder lesson).
	 */
	public TileEntitySmeltery(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6Crucibles.CRUCIBLE_BE.get(), aPos, aState);
	}

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the GTBarrelBlockEntity form: the self-reference stays out of the registry initializer). */
	public TileEntitySmeltery(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gt6.smeltery";
	}

	/** The shell material (the :113/:286 mMaterial) — rides the block carrier (the row-values carrier pattern). */
	@Nullable
	public OreDictMaterial material() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Crucibles.CrucibleBlock tBlock) return tBlock.row().material().get();
		return null;
	}

	/** Upstream :361 — the form bonus ceiling over the shell material. */
	public long temperatureMax() {
		OreDictMaterial tMaterial = material();
		return CruciblePhysics.temperatureMax(tMaterial == null ? MT.Stone : tMaterial, CruciblePhysics.Params.SMALL.heatResistanceBonus());
	}

	/** The shell weight in kg (upstream :286 {@code mMaterial.getWeight(U*7)}). */
	public double shellWeight() {
		OreDictMaterial tMaterial = material();
		return (tMaterial == null ? MT.Stone : tMaterial).getWeight(CS.U * 7);
	}

	// ------------------------------------------------------------------------------------
	// the tick (upstream onServerTickPost :144-328)
	// ------------------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		long tEnvTemperature = envTemp(); // :145 WD.envTemp
		boolean tNewContent = false;

		// :154 — the item suck (the hollow-interior box extended over the top face, class doc)
		if (mInventory.getStackInSlot(0).isEmpty()) {
			ItemStack tSucked = suckTopItem();
			if (!tSucked.isEmpty()) mInventory.setStackInSlot(0, tSucked);
		}

		// :158-183 — the feed ladder
		ItemStack tStack = mInventory.getStackInSlot(0);
		if (!tStack.isEmpty()) {
			List<OreDictMaterialStack> tFeed = feedStacks(tStack);
			if (tFeed == null) {
				// :161-162 — no material data: trash + fizz
				mInventory.setStackInSlot(0, ItemStack.EMPTY);
				fizz();
			} else if (addStacks(tFeed, tEnvTemperature)) {
				mInventory.setStackInSlot(0, ItemStack.EMPTY); // :166 decrStackSize(0, 1) — the slot held one
				tNewContent = true;
			}
		}

		// :186-244 — the alloy scan and consumption (zero table lookups)
		CruciblePhysics.AlloyResult tAlloy = CruciblePhysics.alloyScan(mContent, mTemperature);
		if (tAlloy.alloy() != null && tAlloy.conversions() > 0) {
			CruciblePhysics.applyAlloy(mContent, tAlloy.alloy(), tAlloy.conversions());
			tNewContent = true;
		}

		// :246-284 — the phase gates, world effects applied here
		CruciblePhysics.PhaseOutcome tOutcome = CruciblePhysics.phaseGates(mContent, mTemperature, oTemperature, tNewContent, mAcidProof, CruciblePhysics.Params.SMALL);
		if (tOutcome.fizz()) fizz();
		if (tOutcome.fireCount() > 0) spreadFire(Math.min(tOutcome.fireCount(), 8)); // :258, capped per tick for the RCON-friendly log
		if (tOutcome.explosionStrength() > 0) { // :259-263
			GarbageTruncate();
			explode(true, tOutcome.explosionStrength());
			return;
		}
		if (tOutcome.acidDestroyed()) { // :265-270
			GarbageTruncate();
			setToAir();
			return;
		}

		// :286-301 — the weight census and the heat/cool step
		double tWeight = shellWeight() + CruciblePhysics.weight(mContent);
		// :296-299 — the display census rides the blockstate property
		long tTotal = CruciblePhysics.total(mContent);
		int tLevelBucket = (int)CruciblePhysics.scale(tTotal, CruciblePhysics.Params.SMALL.maxAmount(), 8, false);
		oTemperature = mTemperature;

		CruciblePhysics.TickResult tHeat = CruciblePhysics.tickHeat(mTemperature, mEnergy, tEnvTemperature, tWeight, mCooldown, CruciblePhysics.Params.SMALL.kgPerEnergy());
		mTemperature = tHeat.temperature();
		mEnergy = tHeat.energy();
		mCooldown = tHeat.cooldown();

		// :315-322 — the melt-down: over the ceiling → everything trashes, the block becomes lava
		if (mTemperature > temperatureMax()) {
			GarbageTruncate();
			spreadFire(Math.min(mTemperature / 25, 8));
			if (hasLevel()) getLevel().setBlock(getBlockPos(), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
			return;
		}

		// :324-327 — the meltdown WARNING latch
		if (mMeltDown != CruciblePhysics.isMeltDownWarning(mTemperature, temperatureMax())) {
			mMeltDown = !mMeltDown;
			updateClientData();
		}

		// the LIQUID_LEVEL bucket (the mDisplayedHeight :298 counterpart over the blockstate property)
		if (getBlockState().hasProperty(GT6Crucibles.CrucibleBlock.LIQUID_LEVEL)
				&& getBlockState().getValue(GT6Crucibles.CrucibleBlock.LIQUID_LEVEL) != tLevelBucket && hasLevel()) {
			getLevel().setBlock(getBlockPos(), getBlockState().setValue(GT6Crucibles.CrucibleBlock.LIQUID_LEVEL, tLevelBucket), Block.UPDATE_CLIENTS);
		}
	}

	/** The :154 WD.suck box — inside the hollow crown PLUS the top face (the no-hollow-collision deviation, class doc). */
	@Nullable
	private ItemStack suckTopItem() {
		if (!hasLevel()) return ItemStack.EMPTY;
		BlockPos tPos = getBlockPos();
		AABB tBox = new AABB(tPos.getX() + 0.125, tPos.getY() + 0.125, tPos.getZ() + 0.125, tPos.getX() + 0.875, tPos.getY() + 1.25, tPos.getZ() + 0.875);
		List<ItemEntity> tEntities = getLevel().getEntitiesOfClass(ItemEntity.class, tBox);
		for (ItemEntity tEntity : tEntities) {
			if (tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			if (tStack.isEmpty()) continue;
			ItemStack tOne = tStack.copy();
			tOne.setCount(1);
			tStack.shrink(1);
			if (tStack.isEmpty()) tEntity.discard();
			return tOne;
		}
		return ItemStack.EMPTY;
	}

	/**
	 * The :158-183 feed ladder: a MaterialPrefixItem feeds its prefix amount per item
	 * (the :179-183 generic arm); ore-family prefixes feed the ore-direct projection
	 * (:167-183 — mTargetCrushing × mOreMultiplier with the form-factor scaling); a
	 * vanilla ore rides {@link #VANILLA_ORES}; anything else returns null (the
	 * :160-162 trash+fizz arm).
	 */
	@Nullable
	public List<OreDictMaterialStack> feedStacks(ItemStack aStack) {
		if (aStack.getItem() instanceof gregtech6.item.MaterialPrefixItem tItem) {
			long tCount = aStack.getCount();
			List<OreDictMaterialStack> rList = new ArrayList<>();
			if (tItem.prefix == OP.oreRaw || tItem.prefix.contains(TD.Prefix.STANDARD_ORE)) {
				rList.add(CruciblePhysics.oreDirect(tItem.material, 1)); // :167-168/:175-176
			} else if (tItem.prefix == OP.blockRaw) {
				rList.add(CruciblePhysics.oreDirect(tItem.material, 9)); // :169-170
			} else if (tItem.prefix.contains(TD.Prefix.DENSE_ORE)) {
				rList.add(CruciblePhysics.oreDirect(tItem.material, 2)); // :177-178
			} else if (tItem.prefix.mAmount > 0) {
				rList.add(new OreDictMaterialStack(tItem.material, tItem.prefix.mAmount * tCount)); // :179-183
			}
			rList.removeIf(tStack -> tStack.mAmount <= 0);
			return rList.isEmpty() ? null : rList;
		}
		OreDictMaterial tVanilla = VANILLA_ORES.get(aStack.getItem());
		if (tVanilla != null) {
			List<OreDictMaterialStack> rList = new ArrayList<>();
			rList.add(CruciblePhysics.oreDirect(tVanilla, 1)); // a vanilla ore block = one standard ore
			return rList;
		}
		return null;
	}

	/**
	 * The :330-352 addMaterialStacks over the pure core — the World-side wrapper.
	 * Returns whether everything fit (the caller then clears the feed slot).
	 */
	public boolean addStacks(List<OreDictMaterialStack> aIncoming, long aIncomingTemperature) {
		CruciblePhysics.AddResult tResult = CruciblePhysics.addStacks(mContent, aIncoming, aIncomingTemperature, mTemperature, shellWeight(), CruciblePhysics.Params.SMALL);
		if (!tResult.added()) return false;
		mTemperature = tResult.temperature();
		return true;
	}

	/** The SFX.MC_FIZZ arm (:162/:253/:256) — the sound-only sink (the AV pool keeps the packet face). */
	protected void fizz() {
		if (hasLevel()) getLevel().levelEvent(1501, getBlockPos(), 0); // the vanilla LevelEvent fire-extinguish fizz
	}

	/** The :258/:319 fire arm over the p13 placeFire bridge (the 7x5x7 volume, FLAME_RANGE 3). */
	private void spreadFire(long aCount) {
		if (!hasLevel()) return;
		BlockPos tCenter = getBlockPos();
		for (long i = 0; i < aCount; i++) {
			int tX = tCenter.getX() - (int)FLAME_RANGE + getLevel().random.nextInt((int)(2 * FLAME_RANGE + 1));
			int tY = tCenter.getY() - 1 + getLevel().random.nextInt(2 + (int)FLAME_RANGE);
			int tZ = tCenter.getZ() - (int)FLAME_RANGE + getLevel().random.nextInt((int)(2 * FLAME_RANGE + 1));
			GTGeneratorSolidFireHelper.placeFire(getLevel(), new BlockPos(tX, tY, tZ));
		}
	}

	/** (indirection shim) keeps the fire bridge import in one place. */
	private static final class GTGeneratorSolidFireHelper {
		static boolean placeFire(Level aLevel, BlockPos aPos) {
			return gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity.placeFire(aLevel, aPos);
		}
	}

	/** The :260/:266/:317 trash arm (GarbageGT.trash is the AV pool — the piles just drop). */
	private void GarbageTruncate() {
		mContent.clear();
		mInventory.setStackInSlot(0, ItemStack.EMPTY);
	}

	/** The :269/:398 setToAir arm. */
	private void setToAir() {
		if (hasLevel()) getLevel().removeBlock(getBlockPos(), false);
	}

	// ------------------------------------------------------------------------------------
	// the world interaction (upstream onBlockActivated3 :412-495)
	// ------------------------------------------------------------------------------------

	/**
	 * The top-face click (the block carrier routes SIDES_TOP only). Empty hand takes the
	 * feed slot or scoops scrap; a fluid container drains molten material or pours a
	 * molten fluid back. Server side does the work; the client just consumes the click.
	 */
	public boolean useTop(Player aPlayer, InteractionHand aHand) {
		if (!isServerSide() || aPlayer == null) return true;
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		OreDictMaterialStack tLightest = lightest();

		// :419-424 — take the feed slot back (burning hands hurt — the damage is the entity pool)
		if (!mInventory.getStackInSlot(0).isEmpty()) {
			if (tHeld.isEmpty()) {
				aPlayer.setItemInHand(aHand, mInventory.getStackInSlot(0));
				mInventory.setStackInSlot(0, ItemStack.EMPTY);
			}
			return true;
		}

		// :426-448 — scoop scrap from the COOLED lightest content
		if (tHeld.isEmpty() && tLightest != null && mTemperature < tLightest.mMaterial.mMeltingPoint) {
			ItemStack tScrap = GT6RecipeMapCrucible.matStack(OP.scrapGt, tLightest.mMaterial, 1);
			long tScrapAmount = OP.scrapGt.mAmount;
			if (tScrap == null || tLightest.mAmount < tScrapAmount) {
				tLightest.mAmount = 0; // :428-433 — the remainder dusts away
				aPlayer.causeFoodExhaustion(0.4F); // the UT.Entities.exhaust arm
				return true;
			}
			tLightest.mAmount -= tScrapAmount;
			if (!aPlayer.getInventory().add(tScrap)) aPlayer.drop(tScrap, false); // the ST.add/give form
			aPlayer.causeFoodExhaustion(0.1F);
			return true;
		}

		// :450-468 + :469-490 — the fluid-container arm (drain molten out / pour back), playerless
		if (!tHeld.isEmpty()) {
			ContainerArm tArm = fluidContainerArm(tHeld);
			if (tArm != null) {
				aPlayer.setItemInHand(aHand, tArm.heldAfter());
				if (!aPlayer.getInventory().add(tArm.containerOut())) aPlayer.drop(tArm.containerOut(), false);
				return true;
			}
		}
		return true; // :492 — the top click is always consumed
	}

	/** The two-slot outcome of the container arm: the held stack after the shrink and the swapped-out container. */
	public record ContainerArm(ItemStack heldAfter, ItemStack containerOut) {}

	/**
	 * The :450-468 (an empty container DRAINS the lightest molten content) + :469-490 (a molten
	 * container POURS back through the bind(melting+25, boiling-1) gate) arm, playerless — the
	 * useTop bucket face and the /gt6crucible bucket driver share it. Shrinks {@code aHeld} on
	 * success and answers the swapped-out container; null = the arm falls through (no handler,
	 * nothing molten, one of the gates refused).
	 */
	@Nullable
	public ContainerArm fluidContainerArm(ItemStack aHeld) {
		IFluidHandlerItem tHandler = GT6RecipeMapCanner.sContainerResolver.apply(aHeld.copy());
		if (tHandler == null) return null;
		FluidStack tHeldFluid = tHandler.getFluidInTank(0);
		if (tHeldFluid == null || tHeldFluid.isEmpty()) {
			OreDictMaterialStack tLightest = lightest();
			if (tLightest == null || mTemperature < tLightest.mMaterial.mMeltingPoint) return null;
			net.minecraft.world.level.material.Fluid tMolten = FluidBridge.moltenFluidForMaterial(tLightest.mMaterial.mNameInternal);
			if (tMolten == null) return null;
			long tLiters = Math.min(1000, Math.max(1, CruciblePhysics.units(tLightest.mAmount, CS.U, FluidBridge.L_PER_MOLTEN_UNIT, false)));
			FluidStack tFill = new FluidStack(tMolten, (int)tLiters);
			// the :455 gate — the fluid must not be hotter than the crucible unless cold
			int tFluidTemp = tFill.getFluid().getFluidType().getTemperature();
			if (tFluidTemp >= 320 && mTemperature < tFluidTemp) return null;
			int tFilled = tHandler.fill(tFill, IFluidHandler.FluidAction.EXECUTE);
			if (tFilled <= 0) return null;
			ItemStack tContainer = tHandler.getContainer();
			tLightest.mAmount -= CruciblePhysics.units(tFilled, FluidBridge.L_PER_MOLTEN_UNIT, CS.U, true); // :461 back-conversion
			aHeld.shrink(1);
			return new ContainerArm(aHeld, tContainer);
		}
		// :469-490 — POUR the molten fluid back in (the bind(melting+25, boiling-1) temperature gate)
		OreDictMaterial tFluidMaterial = materialOfFluid(tHeldFluid.getFluid());
		if (tFluidMaterial == null) return null;
		long tUnits = CruciblePhysics.units(tHeldFluid.getAmount(), FluidBridge.L_PER_MOLTEN_UNIT, CS.U, false);
		long tPourTemperature = UT.Code.bind(tFluidMaterial.mMeltingPoint + 25, tFluidMaterial.mBoilingPoint - 1, tHeldFluid.getFluid().getFluidType().getTemperature());
		if (!addStacks(new ArrayList<>(List.of(new OreDictMaterialStack(tFluidMaterial, tUnits))), tPourTemperature)) return null;
		// the container must leave EMPTY: drain before getContainer — the wrappers answer their
		// internal stack verbatim (FluidBucketWrapper.getContainer → the container field), so an
		// undrained handler would hand the FILLED container back and dupe the molten charge
		// (upstream :471 ST.container — the item's own emptied container)
		tHandler.drain(tHeldFluid, IFluidHandler.FluidAction.EXECUTE);
		ItemStack tEmpty = tHandler.getContainer();
		aHeld.shrink(1);
		return new ContainerArm(aHeld, tEmpty);
	}

	/** The :416-417 lightest-content census (the same walk feeds scrap and the bucket arm). */
	@Nullable
	public OreDictMaterialStack lightest() {
		OreDictMaterialStack rLightest = null;
		for (OreDictMaterialStack tMaterial : mContent) {
			if (rLightest == null || tMaterial.mMaterial.mGramPerCubicCentimeter < rLightest.mMaterial.mGramPerCubicCentimeter) rLightest = tMaterial;
		}
		return rLightest;
	}

	/** The reverse FluidBridge walk (the upstream OreDictMaterial.FLUID_MAP face, :472). */
	@Nullable
	public static OreDictMaterial materialOfFluid(net.minecraft.world.level.material.Fluid aFluid) {
		if (aFluid == null) return null;
		for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			if (FluidBridge.moltenFluidForMaterial(tMaterial.mNameInternal) == aFluid) return tMaterial;
		}
		return null;
	}

	// ------------------------------------------------------------------------------------
	// the crucible/mold seam (upstream :498-507 and :354-386)
	// ------------------------------------------------------------------------------------

	@Override
	public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
		for (OreDictMaterialStack tContent : mContent) { // :498-507 verbatim
			if (tContent != null && mTemperature >= tContent.mMaterial.mMeltingPoint && tContent.mMaterial.mTargetSmelting.mMaterial == tContent.mMaterial) {
				long tAmount = aMold.fillMold(tContent, mTemperature, aSideOfMold);
				if (tAmount > 0) {
					tContent.mAmount -= tAmount;
					return true;
				}
			}
		}
		return false;
	}

	/** Upstream :365 SIDES_TOP — the port side order: UP = 1 (the HuEnergyHandshake truth table). */
	public static final byte SIDE_TOP = 1;

	@Override
	public boolean isMoldInputSide(byte aSide) {
		return aSide == SIDE_TOP; // :365-367 SIDES_TOP
	}

	@Override
	public long getMoldMaxTemperature() {
		return temperatureMax(); // :370-372
	}

	@Override
	public long getMoldRequiredMaterialUnits() {
		return 1; // :375-377
	}

	@Override
	public long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
		if (isMoldInputSide(aSide)) { // :380-386 verbatim
			if (addStacks(new ArrayList<>(List.of(aMaterial)), aTemperature)) return aMaterial.mAmount;
			if (aMaterial.mAmount > CS.U && addStacks(new ArrayList<>(List.of(new OreDictMaterialStack(aMaterial.mMaterial, CS.U))), aTemperature)) return CS.U;
		}
		return 0;
	}

	// ------------------------------------------------------------------------------------
	// the temperature face (ITileEntityTemperature :354-362)
	// ------------------------------------------------------------------------------------

	@Override
	public long getTemperatureValue(byte aSide) {
		return mTemperature;
	}

	@Override
	public long getTemperatureMax(byte aSide) {
		return temperatureMax();
	}

	/** The {@link ITileEntityTemperature.EnvTemp} live form (WD.envTemp :404-406 over the vanilla biome climate). */
	public long envTemp() {
		if (!hasLevel()) return DEF_ENV_TEMP;
		BlockPos tPos = getBlockPos();
		float tBiomeTemp = getLevel().getBiome(tPos).value().getBaseTemperature();
		return Math.max(1, C - 3 + (long)(tBiomeTemp * 20));
	}

	// ------------------------------------------------------------------------------------
	// the energy face (upstream :688-698, the HU leg)
	// ------------------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == TD.Energy.HU; // :690 (the KU/CU/VIS legs are the defer pool)
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return aEnergyType == TD.Energy.HU; // :692 — from ALL sides
	}

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aDoInject && aEnergyType == TD.Energy.HU) mEnergy += Math.abs(aAmount * aSize); // :693 HU leg
		return aAmount;
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return Long.MAX_VALUE - mEnergy; // :694
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 1; // :695
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return 2048; // :696
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return Long.MAX_VALUE; // :697
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.HU.AS_LIST; // :698 (the HU leg)
	}

	// ------------------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :87-104)
	// ------------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_TEMPERATURE, mTemperature);
		aNBT.putLong("gt.temperature.old", oTemperature);
		aNBT.putLong("gt.energy", mEnergy);
		aNBT.putInt("gt.cooldown", mCooldown);
		aNBT.putBoolean("gt.meltdown", mMeltDown);
		aNBT.putBoolean("gt.acidproof", mAcidProof);
		//? if forge {
		aNBT.put("gt.inv", mInventory.serializeNBT());
		//?} else {
		/*aNBT.put("gt.inv", mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		 *///?}
		MaterialStackNBT.saveList(mContent, "gt.materials", aNBT); // :103 NBT_MATERIALS
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) mTemperature = aNBT.getLong(NBT_TEMPERATURE); // :91
		if (aNBT.contains("gt.temperature.old", Tag.TAG_ANY_NUMERIC)) oTemperature = aNBT.getLong("gt.temperature.old"); // :92
		if (aNBT.contains("gt.energy", Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong("gt.energy"); // :89
		if (aNBT.contains("gt.cooldown", Tag.TAG_ANY_NUMERIC)) mCooldown = aNBT.getInt("gt.cooldown");
		if (aNBT.contains("gt.meltdown", Tag.TAG_ANY_NUMERIC)) mMeltDown = aNBT.getBoolean("gt.meltdown");
		if (aNBT.contains("gt.acidproof", Tag.TAG_ANY_NUMERIC)) mAcidProof = aNBT.getBoolean("gt.acidproof");
		//? if forge {
		if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound("gt.inv"));
		//?} else {
		/*if (aNBT.contains("gt.inv", Tag.TAG_COMPOUND)) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound("gt.inv")); // 21.1: provider-first
		 *///?}
		mContent.clear(); // :93 loadList — list replace, not merge
		mContent.addAll(MaterialStackNBT.loadList("gt.materials", aNBT));
		mMeltDown = mTemperature + 100 > temperatureMax(); // :94 — recomputed on load
	}
}
