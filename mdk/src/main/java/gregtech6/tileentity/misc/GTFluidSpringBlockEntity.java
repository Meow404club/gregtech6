package gregtech6.tileentity.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The bedrock fluid-spring nozzle BlockEntity (task p38-issue5-fluid-spring-nozzle) — the
 * port of the upstream MultiTileEntityFluidSpring tick body (MultiTileEntityFluidSpring
 * .java:99-146), the never-exhausting spring head: after activation it sprays the spring
 * fluid's SOURCE block at its top face with probability 1/{@code amount} per tick, and
 * once the outlet cell is a full source it refills laterally one cell per spray (the
 * infinite-oil face; the upstream BlockFluidFinite meta ladder has no port counterpart —
 * one block per fluid, the card spec).
 *
 * <p>Upstream faces, the verbatim mapping:
 * <ul>
 * <li><b>NBT</b>: "gt.spring" (the upstream FluidStack key; the port carries the spring
 *     block id string — one block per fluid makes the fluid identity the block id), the
 *     amount rides "gt.spring_amount", the flag "gt.active" (NBT_ACTIVE, CS.java:1223).</li>
 * <li><b>the refill guard</b> (:101 {@code amount <= 0 -> 600}) — also the nextInt(0)
 *     guard: the divisor never reaches zero.</li>
 * <li><b>activation</b> (:106 {@code SERVER_TIME % 20 == 1 && !WD.liquid(above)}): the
 *     global % 20 pulse folds onto the BE-local {@code mTimer % 20} (the
 *     GT6HopperBaseBlockEntity fold) — once the outlet stops being liquid the spring wakes
 *     and never deactivates (upstream writes no mActive=F anywhere).</li>
 * <li><b>production</b> (:105 {@code rng(mFluid.amount) == 0}) — 1/amount per tick on the
 *     level random; the loader amounts (6000 oils / 3000 gas / 500 geothermal / 1000
 *     lava) give one spray every ~5 min / 2.5 min / 25 s / 50 s.</li>
 * <li><b>the spray</b> (:110-141, the infinite-fluid branch): air-or-foreign-liquid above
 *     -> source there; full source above -> convert one horizontal neighbor per spray.
 *     The decisions are the static {@link #shouldSprayAbove}/{@link #shouldSpreadTo} (the
 *     offline test face).</li>
 * <li><b>skin faces not ported</b>: the per-fluid render pass + FLUID_SPRING overlay
 *     texture (the blockstate model is a static cube — the texture is declared debt), the
 *     LIGHT_OPACITY_MAX light face, the client short-sync (nothing client-visible varies:
 *     the model is static).</li>
 * </ul>
 *
 * <p>KJS face (card declaration): registry face, tier-c binding defer — same treatment as
 * every other BlockEntity in this repo.
 */
public class GTFluidSpringBlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream :101 — the amount floor ({@code if (mFluid.amount <= 0) mFluid.amount = 600;}), also the nextInt(0) guard. */
	public static final int DEFAULT_AMOUNT = 600;

	/** Upstream "gt.spring" (writeItemNBT :63) — the FluidStack there, the block id here (the one-block-per-fluid face). */
	public static final String NBT_SPRING = "gt.spring";

	/** The amount companion key — the upstream FluidStack amount, split out of "gt.spring". */
	public static final String NBT_SPRING_AMOUNT = "gt.spring_amount";

	/** Upstream NBT_ACTIVE = "gt.active" (CS.java:1223). */
	public static final String NBT_ACTIVE = "gt.active";

	/** The block id the nozzle sprays (the row's blockId — "gt6:liquid_medium_oil_block" / "minecraft:lava"). */
	private String mSpringBlockId = null;

	/** The upstream mFluid.amount — the 1/amount production divisor. */
	private int mSpringAmount = DEFAULT_AMOUNT;

	/** Upstream mActive — the one-way wake latch. */
	private boolean mActive = false;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTFluidSpringBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.FLUID_SPRING_BE.get(), aPos, aState);
	}

	/** Full constructor. */
	public GTFluidSpringBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
	}

	/** Bare constructor for offline unit tests (no registry, no BlockEntity triple). */
	public GTFluidSpringBlockEntity() {
		super(true, null, BlockPos.ZERO, null);
	}

	@Override
	public String getTileEntityName() {
		return "fluid_spring"; // the BET registry path mirrors it (the house convention)
	}

	/** The worldgen write (upstream setBlock's "gt.spring" NBT face): the spray identity + the 1/amount divisor. */
	public void setSpring(String aBlockId, int aAmount) {
		mSpringBlockId = aBlockId;
		mSpringAmount = aAmount > 0 ? aAmount : DEFAULT_AMOUNT;
		mActive = false; // a fresh worldgen placement starts dormant, upstream same
		setChanged();
	}

	public String getSpringBlockId() {
		return mSpringBlockId;
	}

	public int getSpringAmount() {
		return mSpringAmount;
	}

	public boolean isActive() {
		return mActive;
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide || mSpringBlockId == null || mSpringBlockId.isEmpty()) return;
		if (mSpringAmount <= 0) mSpringAmount = DEFAULT_AMOUNT; // :101
		if (mActive) {
			if (getLevel().random.nextInt(mSpringAmount) == 0) spray(); // :105 rng(amount)==0
		} else if (aTimer % 20 == 0 && !isLiquid(worldPosition.above())) { // :106 — the expose check
			mActive = true; // the one-way wake, upstream :107
			spray();
		}
	}

	private boolean isLiquid(BlockPos aPos) {
		FluidState tFluid = getLevel().getFluidState(aPos);
		return !tFluid.isEmpty(); // WD.liquid — any liquid, flowing or source
	}

	private void spray() {
		Level tLevel = getLevel();
		if (tLevel == null || tLevel.isClientSide()) return;
		BlockState tSource = sourceState(mSpringBlockId);
		if (tSource == null) return; // the loud-refusal face: an unresolvable id never sprays
		BlockPos tAbove = worldPosition.above();
		BlockState tThere = tLevel.getBlockState(tAbove);
		if (shouldSprayAbove(tThere, tSource)) {
			tLevel.setBlock(tAbove, tSource, Block.UPDATE_ALL); // :118/:140 — the source at the top face, flag 3
		} else if (tThere == tSource) { // :114/:122 — the outlet is a full source: refill laterally
			for (Direction tSide : Direction.Plane.HORIZONTAL) { // :124 ALL_SIDES_HORIZONTAL
				BlockPos tNeighbor = tAbove.relative(tSide);
				BlockState tState = tLevel.getBlockState(tNeighbor);
				if (shouldSpreadTo(tState, tSource)) {
					tLevel.setBlock(tNeighbor, tSource, Block.UPDATE_ALL); // :128/:132 — one cell per spray
					break;
				}
			}
		}
	}

	/**
	 * The above-cell spray decision (:117/:139 — {@code WD.liquid(tAbove) || tAbove.isAir}):
	 * air or any liquid that is not already the full source state sprays; solids and the
	 * full source itself do not (the source itself takes the lateral branch). The liquid
	 * face is the {@code LiquidBlock} instance check, NOT the state's cached
	 * {@code getFluidState()} — the Forge bootstrap that fills those caches (Blocks'
	 * {@code initCache} loop) never runs in a bare unit JVM (every state reads EmptyFluid
	 * offline — this card's test probe), and every fluid this nozzle can face (the GT
	 * spring liquids, vanilla water/lava, modded LiquidBlock subclasses) mounts LiquidBlock.
	 */
	public static boolean shouldSprayAbove(BlockState aThere, BlockState aSource) {
		return aThere.isAir() || (aThere.getBlock() instanceof LiquidBlock && aThere != aSource);
	}

	/**
	 * The lateral refill decision (:126-134 — a same-fluid non-source (flowing) cell or an
	 * air cell converts to a source; everything else keeps the walk scanning).
	 */
	public static boolean shouldSpreadTo(BlockState aNeighbor, BlockState aSource) {
		return aNeighbor.isAir() || (aNeighbor.getBlock() == aSource.getBlock() && aNeighbor != aSource);
	}

	/**
	 * The spring fluid's SOURCE state for a block id, or null when unresolvable — hoisted
	 * from GT6FluidSpringFeature.fluidState as the ONE resolver (the worldgen placement and
	 * the runtime spray resolve identically; the DefaultedRegistry miss face = null).
	 */
	public static BlockState sourceState(String aBlockId) {
		int tColon = aBlockId.indexOf(':');
		if (tColon <= 0 || tColon == aBlockId.length() - 1) return null;
		Block tBlock = BuiltInRegistries.BLOCK.get(
				ResourceLocation.fromNamespaceAndPath(aBlockId.substring(0, tColon), aBlockId.substring(tColon + 1)));
		return tBlock == Blocks.AIR ? null : tBlock.defaultBlockState(); // the DefaultedRegistry miss face
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mSpringBlockId != null) aNBT.putString(NBT_SPRING, mSpringBlockId);
		aNBT.putInt(NBT_SPRING_AMOUNT, mSpringAmount);
		aNBT.putBoolean(NBT_ACTIVE, mActive);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		mSpringBlockId = aNBT.contains(NBT_SPRING, Tag.TAG_STRING) ? aNBT.getString(NBT_SPRING) : null;
		if (aNBT.contains(NBT_SPRING_AMOUNT, Tag.TAG_ANY_NUMERIC)) {
			mSpringAmount = aNBT.getInt(NBT_SPRING_AMOUNT);
		}
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) {
			mActive = aNBT.getBoolean(NBT_ACTIVE);
		}
	}
}
