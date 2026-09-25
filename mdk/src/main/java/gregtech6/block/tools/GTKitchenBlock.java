package gregtech6.block.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import java.util.function.Supplier;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.GT6ManualKitchenBlockEntity;

/**
 * The manual kitchen block carrier — the block side of the BathingPot/MixingBowl family
 * (task p26-kitchen-pot-bowl). The carrier pattern (the GTBarrelBlock shape): the block
 * carries the registration values upstream wrote into the MTE definition NBT — here the
 * {@code NBT_TANK_CAPACITY} per-tank litres, the {@code NBT_MATERIAL} melting point (the
 * canOutput :178 / fill-gate :304 {@code mMaterial.mMeltingPoint - 100} door) and the
 * {@code NBT_HARDNESS}/{@code NBT_RESISTANCE} column — plus the family BET this member
 * mounts (one shared BET over the wood + steel pot pair, ADR-P3-1, and one over the
 * ceramic bowl).
 *
 * <p>Upstream rows (Loader_MultiTileEntities.java:2173-2177, category "Misc Tool Blocks"):
 * <ul>
 * <li>Wooden Bathing Pot — ANY.Wood, 4000 L, NBT_FLAMMABILITY 100, aUtilWood;</li>
 * <li>Bathing Pot (steel) — MT.StainlessSteel, 8000 L, aUtilMetal;</li>
 * <li>Ceramic Bowl — MT.Ceramic, 8000 L, aUtilStone, RM.Mixer.</li>
 * </ul>
 * The Table variants (:2174/:2176) are the task-card pool cut. The wood
 * NBT_FLAMMABILITY=100 row has no fire-spread bridge in the port (the flammability pool)
 * — the value is recorded here and on the tab, no {@code FireBlock} hook is shipped.
 *
 * <p>{@code use} is the one-click manual-interaction face: the whole upstream
 * {@code onBlockActivated3} chain (:186-275 pot / :204-296 bowl) rides the BE's
 * {@link GT6ManualKitchenBlockEntity#activateChain} — which doubles as the RCON
 * acceptance channel (the tap {@code activateChain} precedent: a null player means the
 * empty-hand report path). NO GUI: the upstream tooltip line is
 * {@code LH.NO_GUI_CLICK_TO_INTERACT} (:92) and the wave4 GUI ruling binds the family to
 * menu-less carriers (zero new MenuType).
 *
 * <p>Task p38-c3-kitchen-tint-shape — the SUB-CUBE SHAPE + the tint carrier: the datagen
 * models are the sub-cube hollow tubs (GT6BlockStates.addKitchen) while the block rode the
 * default full-cube shape over bare properties — the #1 oversized-selection-box and the #9
 * occlusion X-ray compound. The shape now carries the upstream collision-pool rows verbatim
 * ({@code getCollisionBoundingBoxFromPool} == {@code getSelectedBoundingBoxFromPool}:
 * pot/bowl (0,0,0)-(16,8,16), MultiTileEntityBathingPot.java:406-407 /
 * MultiTileEntityMixingBowl.java:427-428; juicer (2,0,2)-(14,4,14), MultiTileEntityJuicer
 * .java:286-287), and the registrations ride the {@code noOcclusion()} properties seam
 * (GT6Kitchen.kitchenProperties, the GTWires.wireProperties seam form). The tint half: the
 * family models bake tintindex 0 on every face (the #7 reservation) and the colour source
 * is the row material through {@link #materialOf} — the #8 part-family dispatch extension,
 * consumed by the baked GTMachineTintModel (world) and GTItemPaintTint (inventory); NO
 * runtime BlockColor (the p32 ruling). Upstream multiplies the same grayscale colored/
 * tiles with mRGBa on every structural pass (MultiTileEntityBathingPot.getTexture2 :375-379).
 */
public class GTKitchenBlock extends GTEntityBlock {

	private final long mCapacityL;
	private final Supplier<OreDictMaterial> mMaterial;
	private final VoxelShape mShape;
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * The pot-pair/bowl vessel shape — the upstream collision-pool box verbatim, full
	 * footprint and the 8px wall height (MultiTileEntityBathingPot.java:406 /
	 * MultiTileEntityMixingBowl.java:427; the render walls are the same 8px band).
	 */
	public static final VoxelShape SHAPE_TUB = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

	/**
	 * The juicer shape — the upstream collision-pool box verbatim, the 12px footprint
	 * (inset 2px) and the 4px wall height (MultiTileEntityJuicer.java:286); the 7px pestle
	 * rides above it exactly like upstream (the pool boxes are the tub, not the pestle).
	 */
	public static final VoxelShape SHAPE_JUICER = Shapes.box(0.125, 0.0, 0.125, 0.875, 0.25, 0.875);

	/**
	 * @param aCapacityL the per-tank litres (upstream {@code NBT_TANK_CAPACITY}: wood pot
	 *        4000, steel pot 8000, ceramic bowl 8000 — Loader_MultiTileEntities.java:2173/
	 *        :2175/:2177; the BE sizes its input AND output tank ARRAYS from the recipe
	 *        map and every tank to THIS capacity, the upstream :73-78 semantics)
	 * @param aMaterial the upstream {@code NBT_MATERIAL} (resolved lazily — class-load
	 *        precedes MT.init); the -100 K melt doors read {@code mMeltingPoint} and the
	 *        paint tint reads the colour (the {@link #materialOf} dispatch)
	 * @param aShape the sub-cube vessel shape ({@link #SHAPE_TUB} / {@link #SHAPE_JUICER} —
	 *        the upstream collision-pool row)
	 * @param aTickerType the family BET (pot pair share one, the bowl mounts its own)
	 */
	public GTKitchenBlock(long aCapacityL, Supplier<OreDictMaterial> aMaterial, VoxelShape aShape,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mCapacityL = aCapacityL;
		mMaterial = aMaterial;
		mShape = aShape;
		mTickerType = aTickerType;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GTKitchenBlock> codec() {
		return simpleCodec(aProperties -> new GTKitchenBlock(mCapacityL, mMaterial, mShape, mTickerType, aProperties));
	}
	 *///?}

	/** The per-tank litres (upstream NBT_TANK_CAPACITY, the GTBarrelBlock.capacityL carrier seam). */
	public long capacityL() {
		return mCapacityL;
	}

	/** The upstream NBT_MATERIAL — the -100 K melt doors read {@code mMeltingPoint}, the paint tint reads the colour. */
	public OreDictMaterial material() {
		return mMaterial.get();
	}

	/**
	 * The kitchen-domain material dispatch (task p38-c3-kitchen-tint-shape, the
	 * {@code GTMultiBlockPartBlock.materialOf} mirror shape): only the kitchen carriers
	 * resolve a material — every other block (machines, parts, barrels, vanilla states) is
	 * null here, the domain gate {@code GTMachinePaintTint.tintMaterialOf} layers into the
	 * combined dispatch.
	 */
	@Nullable
	public static OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GTKitchenBlock tKitchen ? tKitchen.material() : null;
	}

	/**
	 * The sub-cube vessel shape (the upstream collision-pool row). One override serves
	 * selection AND collision — vanilla {@code getCollisionShape} delegates to
	 * {@code getShape} (BlockBehaviour.java:290, the census default) — which is exactly the
	 * upstream form: getCollisionBoundingBoxFromPool == getSelectedBoundingBoxFromPool.
	 */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return mShape;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	/** The public BET read for the BE base (different package — the kitchen BE tree is under tileentity.tools). */
	public BlockEntityType<? extends TileEntityBase03TicksAndSync> betType() {
		return tickerType();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6ManualKitchenBlockEntity tKitchen) {
			tKitchen.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem receives both empty-hand and item-hand
	// right clicks exactly like the 1.20.1 use(); the hand-degradation deviation is the
	// GTBarrelBlock 1.21.1 form verbatim — MAIN_HAND stands in.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6ManualKitchenBlockEntity tKitchen) {
			tKitchen.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(InteractionHand.MAIN_HAND),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	 *///?}
}
