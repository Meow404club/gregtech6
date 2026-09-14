package gregtech6.block.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;

/**
 * The multiblock part block — the 1.20.1 body of the GT6 multiblock part MTE family (the
 * "one part TE class, many part blocks" shape: the shared {@link GTMultiBlocks#MULTIBLOCK_PART_BE},
 * one Block instance per part type, ADR-P3-1).
 *
 * <p>Extends {@link BaseEntityBlock} DIRECTLY, not GTEntityBlock: the part BE is the notick
 * chain (MultiBlockPartBlockEntity extends TileEntityBase01Root, no dispatcher), while
 * GTEntityBlock hard-types its ticker to TileEntityBase03TicksAndSync. BaseEntityBlock's
 * default {@code getTicker} is null — exactly the notick-chain equivalent, no override needed.
 *
 * <p>Change propagation (upstream IMTE_OnBlockAdded :187-197 / IMTE_BreakBlock :176-184 →
 * controller.onStructureChange, task card ⑤): the 1.20.1 hooks are {@link #onPlace} and
 * {@link #playerWillDestroy}. NEVER onRemove — the remembered BaseEntityBlock trap (a
 * self-written onRemove removes the BE on same-block state changes and drives setBlock-based
 * state writes into a kill+recreate loop; LevelChunk.setBlockState:292 CHECK branch keeps the
 * BE across flips). Same-block state flips are now EXACTLY the {@code design} property's
 * writes (the DESIGN render dimension, below), so the never-onRemove rule is load-bearing
 * for this class, not just belt-and-braces.
 *
 * <p>onPlace ordering note: LevelChunk.setBlockState calls onPlace (:282) BEFORE the new
 * block entity is attached (:286-292) — the propagation here touches only NEIGHBOUR cells
 * (whose BEs exist), never this block's own.
 *
 * <p><b>The DESIGN render dimension (task p29-w3-nbtdesign-parts ①).</b> Upstream every
 * part row carries {@code NBT_DESIGNS} = the TEXTURE-VARIANT COUNT and renders
 * {@code mTextures[mDesign][face]} with
 * {@code mTextures = new IIconContainer[UT.Code.bind8(NBT_DESIGNS)+1][6]}
 * (MultiTileEntityMultiBlockPart.java:138-146 — the plural NBT_DESIGNS counts the
 * variants, the singular NBT_DESIGN indexes them; per-variant texture path
 * {@code machines/multiblockparts/<family>/<design>/{colored,overlay}/{bottom,top,side}}).
 * The 1.20.1 equivalent of the visual half is the {@code design} IntegerProperty on
 * {@code 0..N} inclusive (N = the row's NBT_DESIGNS): the checker's per-cell design
 * write (GTMultiBlockStructureChecker → {@code Util.checkAndSetTarget} →
 * {@link MultiBlockPartBlockEntity#setDesign}) lands in the blockstate and the datagen
 * emits one model per variant ({@code block/parts/<family>/design_<n>}) so a
 * {@code dense_wall design=2} cell renders the Dynamo emitter plate look and a
 * {@code distill_part design=1} cell the back-hole column look, while every other
 * family stays on design 0. DESIGNS-0 rows carry NO property at all (a single-value
 * IntegerProperty cannot exist — the min&lt;max gate) — their single variant is the
 * property-less state, and the datagen walk and the BE sync branch on the null property.
 */
public class GTMultiBlockPartBlock extends BaseEntityBlock {

	/**
	 * The per-range DESIGN property cache — the identity guarantee. 1.20.1 tolerates
	 * same-named distinct IntegerProperty instances (StateHolder.values is an
	 * equals-keyed ImmutableMap, GTBlockProperties.java doc), but the 1.21.1 leg switched
	 * to identity lookup (Reference2ObjectArrayMap), so the StateDefinition's property
	 * and {@link #DESIGN} MUST be one instance: both resolve through this cache.
	 */
	private static final IntegerProperty[] DESIGN_BY_MAX = new IntegerProperty[256];

	private static IntegerProperty designPropertyByMax(int aMax) {
		if (DESIGN_BY_MAX[aMax] == null) DESIGN_BY_MAX[aMax] = IntegerProperty.create("design", 0, aMax);
		return DESIGN_BY_MAX[aMax];
	}

	/**
	 * The pending DESIGN range for the constructor running on THIS thread — the
	 * staging for the vanilla ctor order: Block's ctor calls
	 * {@link #createBlockStateDefinition} BEFORE any instance field assignment can run
	 * (vanilla Block.java:170-175 — the builder consumes the properties mid-super), so a
	 * per-instance range parameter reaches the callback only through this static stage,
	 * parked by {@link #stage} from the super-argument expression. Registration is
	 * single-threaded (the DeferredRegister register phase) and the ranges are fixed
	 * census values, so the window is benign.
	 */
	private static int sPendingMaxDesign = 0;

	/** The super-argument staging hook — runs BEFORE the Block ctor body (the createBlockStateDefinition reader). */
	private static Properties stage(Properties aProperties, int aMaxDesign) {
		sPendingMaxDesign = Math.max(0, Mth.clamp(aMaxDesign, 0, 255));
		return aProperties;
	}

	/** The single-variant form (DESIGNS 0 rows) — the pre-card ctor kept for the Lightning Rod parts / bricks. */
	public GTMultiBlockPartBlock(Properties aProperties) {
		this(aProperties, null, 0);
	}

	/** The design-range form (task p29-w3-nbtdesign-parts ①): an NBT_DESIGNS row without a composed row. */
	public GTMultiBlockPartBlock(Properties aProperties, int aMaxDesign) {
		this(aProperties, null, aMaxDesign);
	}

	/** The wall-carrier form (task p20-i18n-compose-rows): the row feeds the composed Dense Wall name; the Dense family carries NBT_DESIGNS 7. */
	public GTMultiBlockPartBlock(Properties aProperties, gregtech6.registry.GTMultiBlocks.MultiblockPartRow aRow) {
		this(aProperties, aRow, 7);
	}

	/**
	 * The design-range form (task p29-w3-nbtdesign-parts ①): {@code aMaxDesign} = the
	 * row's NBT_DESIGNS — upstream {@code mTextures[bind8(NBT_DESIGNS)+1][6]} is the
	 * variant COUNT, so the property range is {@code 0..N} INCLUSIVE.
	 */
	public GTMultiBlockPartBlock(Properties aProperties, @Nullable gregtech6.registry.GTMultiBlocks.MultiblockPartRow aRow, int aMaxDesign) {
		super(stage(aProperties, aMaxDesign));
		this.mRow = aRow;
		this.mMaxDesign = sPendingMaxDesign;
		this.mComposedName = null;
		// IntegerProperty demands min < max (IntegerProperty.java:19) — a DESIGNS-0 row has
		// no variant axis, so the property is ABSENT there (the datagen walk and the BE sync
		// branch on null)
		this.DESIGN = mMaxDesign > 0 ? designPropertyByMax(mMaxDesign) : null;
		if (DESIGN != null) registerDefaultState(this.stateDefinition.any().setValue(DESIGN, 0));
	}

	/**
	 * The precomposed-name form (task p29-w3-nbtdesign-parts ③): the metal-wall rows
	 * compose "{@code <mat> Wall}" (the {@code gt6.row.metal_wall.display} template over
	 * the gt6.row.mat small unit) at registration; the other new rows pass null and keep
	 * the vanilla atomic-key lookup.
	 */
	public GTMultiBlockPartBlock(Properties aProperties, int aMaxDesign, @Nullable net.minecraft.network.chat.Component aComposedName) {
		this(aProperties, null, aMaxDesign);
		this.mComposedName = aComposedName;
	}

	/**
	 * The DESIGN render dimension — {@code 0..maxDesign()} inclusive, one shared instance
	 * per range; NULL on the DESIGNS-0 rows (a single-value IntegerProperty cannot exist).
	 */
	@Nullable
	public final IntegerProperty DESIGN;

	/** This block's NBT_DESIGNS (the top of the DESIGN range; kept beside the property for the sync clamp). */
	private final int mMaxDesign;

	/** The carried part row (task p13-large-boiler record; null = the rows without a composed name — the coke-oven bricks). */
	@Nullable
	private final gregtech6.registry.GTMultiBlocks.MultiblockPartRow mRow;

	/** The precomposed display name (the metal-wall template form); null = the mRow/vanilla-key resolution. */
	@Nullable
	private net.minecraft.network.chat.Component mComposedName;

	/** The top of this block's DESIGN range (the row's NBT_DESIGNS; 0 = single-variant rows). */
	public int maxDesign() {
		return mMaxDesign;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		// the range comes from the stage() parking lot — the vanilla Block ctor (Block.java:170-175)
		// calls this BEFORE the instance fields exist; DESIGNS-0 rows add nothing
		if (sPendingMaxDesign > 0) aBuilder.add(designPropertyByMax(sPendingMaxDesign));
	}

	/** The design index carried by a state of THIS block (the BE sync + the datagen walk); 0 on the property-less rows. */
	public int designOf(BlockState aState) {
		return DESIGN == null ? 0 : aState.getValue(DESIGN);
	}

	/**
	 * The composed Dense Wall name (task p20-i18n-compose-rows): the row-carried form fills
	 * the {@code gt6.row.dense_wall.display} template over the gt6.row.mat small unit; the
	 * row-less forms (the bricks) keep the vanilla atomic-key lookup.
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		if (mComposedName != null) return mComposedName.copy();
		if (mRow == null) return super.getName();
		return net.minecraft.network.chat.Component.translatable(gregtech6.registry.GTMultiBlocks.DENSE_WALL_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(gregtech6.registry.GTMultiBlocks.wallMatUnitKeyOf(mRow)));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTMultiBlockPartBlock> codec() {
		return simpleCodec(aProperties -> new GTMultiBlockPartBlock(aProperties));
	}
	*///?}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default is INVISIBLE (BER assumption)
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		// BlockEntityType.create -> factory (BlockEntityType.java:288-290)
		return GTMultiBlocks.MULTIBLOCK_PART_BE.get().create(aPos, aState);
	}

	/** Upstream onBlockAdded :187-197 — flag every adjacent part's controller and every adjacent controller. */
	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aMoving) {
		super.onPlace(aState, aLevel, aPos, aOldState, aMoving);
		if (aLevel.isClientSide() || aState.is(aOldState.getBlock())) return; // same-block flip guard (belt & braces)
		for (Direction tSide : Direction.values()) {
			BlockEntity tNeighbor = aLevel.getBlockEntity(aPos.relative(tSide));
			if (tNeighbor instanceof MultiBlockPartBlockEntity tPart) {
				ITileEntityMultiBlockController tController = tPart.getTarget(false);
				if (tController != null) tController.onStructureChange();
			} else if (tNeighbor instanceof ITileEntityMultiBlockController tController) {
				tController.onStructureChange();
			}
		}
	}

	/** Upstream breakBlock :176-184 — release the claim, then force the controller recheck. */
	//? if forge {
	@Override
	public void playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
		super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
		if (aLevel.isClientSide()) return;
	//?} else {
	/*@Override
	public BlockState playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
	//21.1: BlockBehaviour.playerWillDestroy returns BlockState (void on 1.20.1, javap Block
	//21.1.249) — the override return type drifts; the tail hands back the state untouched.
		super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
		if (aLevel.isClientSide()) return aState;
	*///?}
		if (aLevel.getBlockEntity(aPos) instanceof MultiBlockPartBlockEntity tPart) {
			ITileEntityMultiBlockController tTarget = tPart.getTarget(false);
			if (tTarget != null) {
				tPart.clearTarget();
				tTarget.onStructureChange();
			}
		}
		//? if neoforge {
		/*return aState;
		*///?}
	}
}
