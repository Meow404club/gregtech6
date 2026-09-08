package gregtech6.tileentity.multiblocks;

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
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;

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
 * <p><b>The constants (upstream :78-80, verbatim)</b>: {@link #MAX_AMOUNT} = 432U
 * (= 16*3*3*3*U), {@link #KG_PER_ENERGY} = 100, {@link #HEAT_RESISTANCE_BONUS} =
 * 1.10 (the LARGE-crucible wall bonus; the small Smeltery carries 1.25 — the two
 * shapes are the same physics consumed at different parameters, the A-card
 * CruciblePhysics parameter face), {@link #GAS_RANGE} = {@link #FLAME_RANGE} = 5.
 *
 * <p><b>Structure loss = the slow cool-down (upstream :187-195)</b>: while the
 * structure check fails, the stored temperature decays toward the environment at
 * 1 K per 10 ticks and never drops below min(200, env). Declared deviation: the
 * upstream gates the decay on the GLOBAL server time (SERVER_TIME % 10); the port
 * counts on the controller's own tick cycle (mTimer % 10) — no global-clock face is
 * ported and the decay semantics (rate, floor) are identical.
 *
 * <p>The smelting physics tick (the material content, the alloy scan, the meltdown,
 * the HU consumption), the {@code ITileEntityCrucible} mold proxy and the
 * registration land with the A-card rebase commits — this file's structure half is
 * A-independent.
 */
public class TileEntityCrucible extends TileEntityBase10MultiBlockBase {

	// ---------------------------------------------------------------------------
	// the constants (upstream :78-80 verbatim)
	// ---------------------------------------------------------------------------

	/** Upstream :78 — the boiling vent radius (gas damage) and the fire spread radius. */
	public static long GAS_RANGE = 5, FLAME_RANGE = 5;

	/** Upstream :79 — 16 * 3 * 3 * 3 * U: the content capacity of the 3x3x3 cavity. */
	public static long MAX_AMOUNT = 16*3*3*3*CS.U;

	/** Upstream :79 — the heat-mass divisor: 1 HU heats the wall material mass by 1 K per 100 kg. */
	public static long KG_PER_ENERGY = 100;

	/** Upstream :80 — the LARGE-crucible wall heat bonus (the small Smeltery carries 1.25). */
	public static double HEAT_RESISTANCE_BONUS = 1.10;

	/** The default environment temperature (upstream CS.DEF_ENV_TEMP = C + 20 = 293). */
	public static final long DEF_ENV_TEMP = 293;

	/** The temperature NBT key (upstream NBT_TEMPERATURE). */
	public static final String NBT_TEMPERATURE = "temperature";

	// ---------------------------------------------------------------------------
	// the state (upstream :85)
	// ---------------------------------------------------------------------------

	/** The stored heat, in K (upstream :85 mTemperature — the physical state the tick drives). */
	public long mTemperature = DEF_ENV_TEMP;

	/**
	 * The BET-injected constructor — the production registration (the GT6Crucibles
	 * wall-variant rows) and the offline fixtures (the selfHolder recipe) both pass
	 * their own type, so the class stays decoupled from the registry (the
	 * {@code TileEntityLargeBoiler} constructor shape).
	 */
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
	// NBT (upstream :91-109, the temperature half)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) mTemperature = aNBT.getLong(NBT_TEMPERATURE);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_TEMPERATURE, mTemperature); // UT.NBT.setNumber :106
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
	 * Upstream :112-131, walked from the declared pattern (the p16-pattern-checker seam):
	 * the three wall rings carry their per-layer usage masks, the centre column at
	 * y+1/y+2 is the fail-not-clear hollow pair (upstream :115-116), and the controller's
	 * own cell (y+0 centre) passes via the checker's inherited self-cell arm. The
	 * unloaded guard keeps the upstream semantics: an unloaded probe keeps the last
	 * verdict (the boiler :289-290 form).
	 */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // the :133 no-level form
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
	// the tick (:184-195 — the structure-loss slow cool-down)
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
		// the smelting physics tick lands with the A-card rebase (the CruciblePhysics consumption)
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
}
