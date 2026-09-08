package gregtech6.tileentity.multiblocks;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.tileentity.temperature.ITileEntityTemperature;
import gregapi.util.CruciblePhysics;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.tileentity.MaterialStackNBT;

/**
 * 1.20.1 counterpart of gregtech/tileentity/multiblocks/MultiTileEntityCrucible.java
 * (714 lines, task p26-crucible-multiblock) — the LARGE 3x3x3 crucible multiblock:
 * a hollow of wall parts with the opening on top, the controller at the
 * bottom-centre cell ("Main at Bottom-Center", upstream tooltip :139-140).
 *
 * <p><b>The three-layer wall semantics (upstream checkStructure2 :112-131, verbatim
 * per layer)</b>: the y+0 ring of 8 walls is the energy intake layer
 * ({@link MultiBlockPartBlockEntity#ONLY_ENERGY_IN} — the burning boxes touch HERE),
 * the y+1 ring is the mold-access layer ({@link MultiBlockPartBlockEntity#ONLY_CRUCIBLE}),
 * the y+2 ring is the item/fluid feed layer
 * ({@link MultiBlockPartBlockEntity#ONLY_ITEM_FLUID} — the top opening). The centre
 * column at y+1/y+2 must ALREADY be air (upstream :115-116: the getAir gate with the
 * idempotent setBlockToAir — a standing block FAILS the check, never cleared; the
 * checker's fail-not-clear hollow semantics are the same judgement). The layer masks
 * are consumed, never re-declared — the three ONLY_* constants are verbatim already
 * in {@link MultiBlockPartBlockEntity} (:103/:113/:118). The structure check walks
 * the declared pattern through the shared checker (the p16-pattern-checker seam, the
 * CokeOven production pilot) — one judgement source for the server check and the
 * ghost preview.
 *
 * <p><b>The physics tick (upstream onServerTickPost :184-384) consumes the A-card
 * CruciblePhysics parameter face</b> — the LARGE parameter set
 * ({@link CruciblePhysics.Params#LARGE}: 432U / 1.10 / 8 / 5 / 100) drives the same
 * functions the small Smeltery runs at SMALL, exactly the shared-thermodynamics
 * arch ruling (tasks.p26-arch-crucible-chain ②). Per tick, upstream order:
 * <ol>
 * <li>the alloy scan + consumption (:236-294 → {@link CruciblePhysics#alloyScan} +
 *     {@link CruciblePhysics#applyAlloy});</li>
 * <li>the evaporation/acid/phase-gate loop (:296-334 →
 *     {@link CruciblePhysics#phaseGates}) with the world effects applied BE-side
 *     (the explosion via the Root {@code explode(strength)} :312-313, the acid
 *     destruction via {@code setToAir} :319, the gas/fire surfaces deferred — no
 *     entity-damage or WD.fire face is ported);</li>
 * <li>the weight census (:336-344) and the {@code oTemperature} latch (:346);</li>
 * <li>the HU heating step (:353-365 → {@link CruciblePhysics#tickHeat});</li>
 * <li>the meltdown gate (:367-378): over the ceiling the content is trashed and the
 *     3x3x3 cavity becomes lava (the core cell included — the controller dies in the
 *     flow);</li>
 * <li>the melt-down WARNING latch (:380-383 → {@link CruciblePhysics#isMeltDownWarning}).</li>
 * </ol>
 *
 * <p><b>The through-wall mold proxy</b>: the controller implements
 * {@link ITileEntityCrucible} with the upstream fillMoldAtSide (:547-556) verbatim —
 * the wall part answers the pour (the part-side relay is the B-card mold walking
 * onto the wall, the upstream MultiBlockPart :687-690 consumer). The wall parts
 * themselves need NO extra forwarding: the B-card mold already walks
 * controller-ward through the part's target resolution, so the controller-side
 * method IS the whole proxy (the card C ruling: controller-side implementation
 * preferred, the {@code MultiBlockPartBlockEntity implements} arm stays unused).
 *
 * <p><b>Structure loss = the slow cool-down (upstream :187-195)</b>: while the
 * structure check fails, the stored temperature decays toward the environment at
 * 1 K per 10 ticks and never drops below min(200, env). Declared deviation: the
 * upstream gates the decay on the GLOBAL server time (SERVER_TIME % 10); the port
 * counts on the controller's own tick cycle (mTimer % 10) — no global-clock face is
 * ported and the decay semantics (rate, floor) are identical.
 */
public class TileEntityCrucible extends TileEntityBase10MultiBlockBase implements ITileEntityCrucible, ITileEntityTemperature {

	// ---------------------------------------------------------------------------
	// the constants (upstream :78-80, consuming the A-card LARGE parameter face)
	// ---------------------------------------------------------------------------

	/** Upstream :78 — the boiling vent radius (gas damage) and the fire spread radius. */
	public static long GAS_RANGE = CruciblePhysics.Params.LARGE.gasRange(), FLAME_RANGE = 5;

	/** Upstream :79 — 16 * 3 * 3 * 3 * U: the content capacity of the 3x3x3 cavity. */
	public static long MAX_AMOUNT = CruciblePhysics.Params.LARGE.maxAmount();

	/** Upstream :79 — the heat-mass divisor: 1 HU heats the wall material mass by 1 K per 100 kg. */
	public static long KG_PER_ENERGY = CruciblePhysics.Params.LARGE.kgPerEnergy();

	/** Upstream :80 — the LARGE-crucible wall heat bonus (the small Smeltery carries 1.25). */
	public static double HEAT_RESISTANCE_BONUS = CruciblePhysics.Params.LARGE.heatResistanceBonus();

	/** The default environment temperature (upstream CS.DEF_ENV_TEMP = C + 20 = 293). */
	public static final long DEF_ENV_TEMP = 293;

	/** The NBT keys (upstream NBT_TEMPERATURE / NBT_ENERGY / NBT_ACIDPROOF / NBT_MATERIALS). */
	public static final String NBT_TEMPERATURE = "temperature";
	public static final String NBT_ENERGY = "energy";
	public static final String NBT_ACIDPROOF = "acidproof";
	public static final String NBT_MATERIALS = "materials";

	// ---------------------------------------------------------------------------
	// the state (upstream :82-86)
	// ---------------------------------------------------------------------------

	/** The stored heat, in K (upstream :85 mTemperature — the physical state the tick drives). */
	public long mTemperature = DEF_ENV_TEMP;

	/** The previous tick's pre-heat temperature, the phase-crossing latch (upstream :85 oTemperature). */
	public long oTemperature = 0;

	/** The HU buffer (upstream :85 mEnergy — the burning-box feed pays into this). */
	public long mEnergy = 0;

	/** The heat countdown (upstream :83 mCooldown — 100 ticks of grace after the last charge). */
	public int mCooldown = 100;

	/** The melt-down WARNING latch (upstream :82 mMeltDown — the visual alarm state). */
	public boolean mMeltDown = false;

	/** The acidproofing (upstream :82 mAcidProof — a row property of the wall material). */
	public boolean mAcidProof = false;

	/** The molten content (upstream :86 mContent — the List of material stacks). */
	public final List<OreDictMaterialStack> mContent = new ArrayList<>();

	protected TileEntityCrucible(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		// the crucible has NO facing semantics upstream (getDefaultSide SIDE_UP :689, the
		// structure fully symmetric around the controller cell). The shared checker's cell
		// arithmetic ("the structure core sits BEHIND the facing", cellOffset = p - OFF)
		// degenerates to the identity at facing 0 (OFF[0] = 0,0,0) — the canonical facing
		// this controller is pinned to, so the pattern coordinates stay controller-relative
		// exactly like the upstream checkAndSetTargetOffset loop (:119-121, pure xCoord math).
		mFacing = 0;
	}

	@Override
	public String getTileEntityName() {
		return "crucible";
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream :91-109)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) mTemperature = aNBT.getLong(NBT_TEMPERATURE);
		if (aNBT.contains(NBT_TEMPERATURE + ".old", Tag.TAG_ANY_NUMERIC)) oTemperature = aNBT.getLong(NBT_TEMPERATURE + ".old");
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY);
		if (aNBT.contains(NBT_ACIDPROOF, Tag.TAG_ANY_NUMERIC)) mAcidProof = aNBT.getBoolean(NBT_ACIDPROOF);
		mContent.clear();
		mContent.addAll(MaterialStackNBT.loadList(NBT_MATERIALS, aNBT)); // :98 OreDictMaterialStack.loadList
		mMeltDown = CruciblePhysics.isMeltDownWarning(mTemperature, getTemperatureMax((byte)0)); // :99 re-derived, never stored
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_TEMPERATURE, mTemperature);              // UT.NBT.setNumber :106
		aNBT.putLong(NBT_TEMPERATURE + ".old", oTemperature);     // :107
		aNBT.putLong(NBT_ENERGY, mEnergy);                        // :105
		MaterialStackNBT.saveList(mContent, NBT_MATERIALS, aNBT); // :108 OreDictMaterialStack.saveList
	}

	// ---------------------------------------------------------------------------
	// the structure (:112-136)
	// ---------------------------------------------------------------------------

	/**
	 * The wall part block this structure is built from (upstream :88 mWalls = the part
	 * MTE id 18002 + the NBT_DESIGN wall swap — the registry pair becomes one Block per
	 * wall material, the LargeBoiler wall-variant shape). The production override reads
	 * the controller block's carried wall (the GT6Crucibles registration); the default
	 * here is the offline-test binding only.
	 */
	protected Block getWallBlock() {
		return Blocks.BRICKS;
	}

	/**
	 * The shell material — the wall material the physics ride (upstream mMaterial, the
	 * MTE registration row material; :336 the shell weight, :416-418 the temperature
	 * ceiling). Steel is the single-rung material ladder (the card C spec ⑤ ruling);
	 * the production override binds the registered wall row.
	 */
	@Nullable
	protected OreDictMaterial getShellMaterial() {
		return MT.Steel;
	}

	/** The physics parameter face this form consumes ({@link CruciblePhysics.Params#LARGE}). */
	protected CruciblePhysics.Params params() {
		return CruciblePhysics.Params.LARGE;
	}

	/**
	 * Upstream :112-131, walked from the declared pattern (the p16-pattern-checker seam):
	 * the three wall rings carry their per-layer usage masks, the centre column at
	 * y+1/y+2 is the fail-not-clear hollow pair (upstream :115-116), and the controller's
	 * own cell (y+0 centre) passes via the checker's inherited self-cell arm. The
	 * unloaded guard keeps the upstream semantics: an unloaded probe keeps the last
	 * verdict (the boiler :289-290 form).
	 */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // :133
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				this, mFacing, aCoordinates, aPlayer, aInventory);
		if (tVerdict.unloaded) return mStructureOkay; // unloaded cells keep the last verdict
		return tVerdict.formed;
	}

	/**
	 * The declared structure pattern: 24 forming wall cells in three rings (y+0
	 * ONLY_ENERGY_IN, y+1 ONLY_CRUCIBLE, y+2 ONLY_ITEM_FLUID — the upstream :119-121
	 * mask column) plus the two keep-hollow centre cells (y+1/y+2, the :115-116 top
	 * opening), appended last. The y+0 centre is the controller itself — never declared.
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			Block tWall = getWallBlock();
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			// :119 — the y+0 ring, ONLY_ENERGY_IN (the burning boxes touch here)
			ring(tBuilder, tWall, 0, MultiBlockPartBlockEntity.ONLY_ENERGY_IN);
			// :120 — the y+1 ring, ONLY_CRUCIBLE (the mold access layer)
			ring(tBuilder, tWall, 1, MultiBlockPartBlockEntity.ONLY_CRUCIBLE);
			// :121 — the y+2 ring, ONLY_ITEM_FLUID (the feed layer)
			ring(tBuilder, tWall, 2, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID);
			// :115-116 — the centre column, fail-not-clear air
			tBuilder.hollow(0, 1, 0, GTMultiBlockPattern.AIR);
			tBuilder.hollow(0, 2, 0, GTMultiBlockPattern.AIR);
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	/** One wall ring: the 8 cells around the centre at the given layer height. */
	private static void ring(GTMultiBlockPattern.Builder aBuilder, Block aWall, int aY, int aUsage) {
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue; // the centre column is not a wall cell
			aBuilder.formingPart(tDX, aY, tDZ, aWall, aUsage, 0);
		}
	}

	/** Upstream :134-136 verbatim — the box around the controller, y from 0 to +2. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		return aX >= getBlockPos().getX() - 1 && aY >= getBlockPos().getY() && aZ >= getBlockPos().getZ() - 1
			&& aX <= getBlockPos().getX() + 1 && aY <= getBlockPos().getY() + 2 && aZ <= getBlockPos().getZ() + 1;
	}

	// ---------------------------------------------------------------------------
	// the temperature interface (upstream :410-418, ITileEntityTemperature)
	// ---------------------------------------------------------------------------

	@Override
	public long getTemperatureValue(byte aSide) {
		return mTemperature;
	}

	@Override
	public long getTemperatureMax(byte aSide) {
		OreDictMaterial tShell = getShellMaterial();
		if (tShell == null) return Long.MAX_VALUE; // the offline no-material form never melts
		return CruciblePhysics.temperatureMax(tShell, HEAT_RESISTANCE_BONUS); // :416-418
	}

	// ---------------------------------------------------------------------------
	// the content admission (upstream addMaterialStacks :386-408)
	// ---------------------------------------------------------------------------

	/** The shell weight the thermal blend rides (upstream :336/:388 mMaterial.getWeight(U*100)). */
	protected double shellWeight() {
		OreDictMaterial tShell = getShellMaterial();
		return tShell == null ? 0 : tShell.getWeight(CS.U * 100); // upstream :336 verbatim
	}

	/**
	 * Upstream addMaterialStacks (:386-408) via the shared physics: the capacity gate
	 * and the thermal blend live in {@link CruciblePhysics#addStacks}; the structure
	 * check stays here (the BE side owns the world question).
	 *
	 * @return if the material fit (and only then was blended in)
	 */
	public boolean addMaterialStacks(List<OreDictMaterialStack> aList, long aTemperature) {
		if (!checkStructure(false)) return false; // :387 the structure gate
		CruciblePhysics.AddResult tResult = CruciblePhysics.addStacks(mContent, aList, aTemperature, mTemperature, shellWeight(), params());
		if (tResult.added()) {
			mTemperature = tResult.temperature(); // :389 the blend
			setChanged();
		}
		return tResult.added();
	}

	/** The content census (upstream OM.total). */
	public long totalContent() {
		return CruciblePhysics.total(mContent);
	}

	// ---------------------------------------------------------------------------
	// the tick (:184-384)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide) return;

		// :187-195 — the structure loss arm: decay toward the environment, floor min(200, env)
		if (!checkStructure(false)) {
			coolStep(aTimer);
			return;
		}
		tickPhysics();
	}

	/**
	 * The formed-structure physics tick, upstream :236-383 order verbatim (the feed
	 * :204-234 and the rain :197-202 ride the slot/suck commit — no suck face yet).
	 */
	private void tickPhysics() {
		int tHashBefore = mContent.hashCode(); // :185 tHash

		// :236-294 — the alloy scan + the consumption half
		CruciblePhysics.AlloyResult tAlloy = CruciblePhysics.alloyScan(mContent, mTemperature);
		CruciblePhysics.applyAlloy(mContent, tAlloy.alloy(), tAlloy.conversions());

		// :296-334 — the evaporation/acid/phase-gate loop, world effects BE-side
		boolean tNewContent = (tHashBefore != mContent.hashCode()); // :241
		CruciblePhysics.PhaseOutcome tOutcome = CruciblePhysics.phaseGates(mContent, mTemperature, oTemperature, tNewContent, mAcidProof, params());
		if (tOutcome.fizz()) { /* SFX.MC_FIZZ :303/:306/:318 — no sound face ported */ }

		// :336-344 — the weight census (the lightest-stack display work is the render defer)
		double tWeight = shellWeight() + CruciblePhysics.weight(mContent);
		if (tWeight < 0) tWeight = 0;

		// :346 — the crossing latch BEFORE the heat step (the next tick's gates read this)
		oTemperature = mTemperature;

		// the destruction arms (:309-320) — content already cleared by the physics
		if (tOutcome.explosionStrength() > 0) {
			explode(tOutcome.explosionStrength()); // :312-313 the Root blast
			return;
		}
		if (tOutcome.acidDestroyed()) {
			setToAir(); // :319 — the acid melt-through kills the controller block
			return;
		}
		// the :307/:370 gas-damage and :308/:371 fire surfaces defer (no entity/fire face)

		// :353-365 — the HU heating step
		CruciblePhysics.TickResult tHeat = CruciblePhysics.tickHeat(mTemperature, mEnergy, envTemperature(), tWeight, mCooldown, KG_PER_ENERGY);
		mTemperature = tHeat.temperature();
		mEnergy = tHeat.energy();
		mCooldown = tHeat.cooldown();

		// :367-378 — the meltdown gate
		long tMax = getTemperatureMax((byte)0);
		if (mTemperature > tMax) {
			meltdown(tMax);
			return;
		}

		// :380-383 — the melt-down WARNING latch
		boolean tWarning = CruciblePhysics.isMeltDownWarning(mTemperature, tMax);
		if (mMeltDown != tWarning) {
			mMeltDown = tWarning;
			updateClientData();
		}
		setChanged();
	}

	/**
	 * The :192-193 decay pair — 1 K per 10 ticks toward the environment, floored at
	 * min(200, env). Package-visible for the offline tests.
	 */
	void coolStep(long aTimer) {
		long tEnv = envTemperature();
		if (aTimer % 10 == 0) {
			if (mTemperature > tEnv) mTemperature--;
			if (mTemperature < tEnv) mTemperature++;
		}
		mTemperature = Math.max(mTemperature, Math.min(200, tEnv));
		setChanged();
	}

	/**
	 * The environment temperature seam. The production form binds the A-card
	 * temperature/environment face; the default here is the flat DEF_ENV_TEMP the
	 * upstream WD.envTemp degenerates to in a climate-less world.
	 */
	protected long envTemperature() {
		return DEF_ENV_TEMP;
	}

	/** Upstream onPlaced :589-592 — the stored heat starts at the environment. */
	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide && mTimer == 0) mTemperature = envTemperature();
	}

	/**
	 * Upstream :367-377 — the meltdown: the content is trashed and the 3x3x3 cavity
	 * becomes flowing lava (the controller's own cell included, so the multiblock dies
	 * in the flow). The gas-damage and fire-spread arms (:370-371) defer with the other
	 * world-effect surfaces. Package-visible for the offline tests.
	 */
	void meltdown(long aTemperatureMax) {
		mContent.clear(); // :369 GarbageGT.trash(mContent)
		if (hasLevel() && isServerSide()) {
			int tX = getBlockPos().getX(), tY = getBlockPos().getY(), tZ = getBlockPos().getZ();
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) { // :372-376
				getLevel().setBlock(new BlockPos(tX + i, tY    , tZ + j), Blocks.LAVA.defaultBlockState(), 3);
				getLevel().setBlock(new BlockPos(tX + i, tY + 1, tZ + j), Blocks.LAVA.defaultBlockState(), 3);
				getLevel().setBlock(new BlockPos(tX + i, tY + 2, tZ + j), Blocks.LAVA.defaultBlockState(), 3);
			}
		}
		setChanged();
	}

	/** Upstream :319 setToAir — the acid melt-through. */
	private void setToAir() {
		if (hasLevel() && isServerSide()) {
			getLevel().setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
		}
	}

	// ---------------------------------------------------------------------------
	// the through-wall mold proxy (upstream :547-556, ITileEntityCrucible)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream fillMoldAtSide (:547-556) verbatim: a formed structure pours its
	 * molten, self-smelted stacks (the mTargetSmelting identity gate — the molten
	 * metal proper, not an intermediate) into the adjacent Mold, one stack per call,
	 * the pour amount subtracted from the content. The wall part relays here through
	 * its target resolution (the upstream MultiBlockPart :687-690 walk).
	 */
	@Override
	public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
		if (checkStructure(false)) for (OreDictMaterialStack tContent : mContent) {
			if (tContent != null && mTemperature >= tContent.mMaterial.mMeltingPoint
					&& tContent.mMaterial.mTargetSmelting.mMaterial == tContent.mMaterial) {
				long tAmount = aMold.fillMold(tContent, mTemperature, aSideOfMold);
				if (tAmount > 0) {
					tContent.mAmount -= tAmount;
					setChanged();
					return true;
				}
			}
		}
		return false;
	}
}
