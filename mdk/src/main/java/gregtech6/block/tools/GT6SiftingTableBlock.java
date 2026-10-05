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

import net.minecraftforge.items.IItemHandler;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import java.util.function.Supplier;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.GT6SiftingTableBlockEntity;

/**
 * The Sifting Table block carrier — the block side of the manual-devices chain tail
 * (task sifting-table-family), the GTKitchenBlock shape. The carrier holds the
 * registration values upstream wrote into the MTE definition NBT — here the
 * {@code NBT_MATERIAL} (ANY.Steel, Loader_MultiTileEntities.java:2227; the paint-tint
 * face reads it) — plus the family BET.
 *
 * <p>{@code use} is the one-click manual-interaction face: the whole upstream
 * {@code onBlockActivated3} chain (:275-299) rides the BE's
 * {@link GT6SiftingTableBlockEntity#activateChain} — which doubles as the RCON
 * acceptance channel (the anvil precedent: a null player means the report path). NO
 * GUI: the upstream tooltip line is {@code LH.NO_GUI_CLICK_TO_INTERACT} (:79) and the
 * wave4 GUI ruling binds the family to menu-less carriers (zero new MenuType). The
 * shape is the upstream collision-pool row verbatim: the full 16px footprint and the
 * 12px table height (getCollisionBoundingBoxFromPool == getSelectedBoundingBoxFromPool,
 * MultiTileEntitySiftingTable.java:428-430 {@code PX_N[4]}).
 *
 * <p>KJS face (declared): the registration surface defers to the kjs-binding card
 * (the GT6Kitchen precedent); the loot/recipe faces are the datapack domain — the
 * crafting row rides the crafting datagen, naturally editable.
 */
public class GT6SiftingTableBlock extends GTEntityBlock {

	/**
	 * The table shape — the upstream collision-pool box verbatim, full footprint and the
	 * 12px height (MultiTileEntitySiftingTable.java:428-430; the render legs/plate live
	 * inside it).
	 */
	public static final VoxelShape SHAPE_TABLE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);

	private final Supplier<OreDictMaterial> mMaterial;
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * @param aMaterial the upstream {@code NBT_MATERIAL} (resolved lazily — class-load
	 *        precedes ANY.init); the paint-tint face reads the colour
	 * @param aTickerType the family BET
	 */
	public GT6SiftingTableBlock(Supplier<OreDictMaterial> aMaterial,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mMaterial = aMaterial;
		mTickerType = aTickerType;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GT6SiftingTableBlock> codec() {
		return simpleCodec(aProperties -> new GT6SiftingTableBlock(mMaterial, mTickerType, aProperties));
	}
	 *///?}

	/** The upstream NBT_MATERIAL (ANY.Steel) — the paint-tint face reads the colour. */
	public OreDictMaterial material() {
		return mMaterial.get();
	}

	/**
	 * The manual-device material dispatch (the GTKitchenBlock.materialOf mirror shape):
	 * only the sifting-table carrier resolves a material — every other block is null
	 * here. The tint-consumer dispatch row (GTMachinePaintTint) is the render-pool
	 * defer — the model ships the tintindex-0 seats, a dispatch row lands the colour
	 * without model change (the measuring-pot precedent).
	 */
	@Nullable
	public static OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6SiftingTableBlock tTable ? tTable.material() : null;
	}

	/**
	 * The sub-cube table shape (the upstream collision-pool row). One override serves
	 * selection AND collision — vanilla {@code getCollisionShape} delegates to
	 * {@code getShape} (the GTKitchenBlock census note).
	 */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE_TABLE;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	/** The public BET read for the BE base (different package). */
	public BlockEntityType<? extends TileEntityBase03TicksAndSync> betType() {
		return tickerType();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	/**
	 * The break drop contract (upstream canDrop :442 = T per slot) — the pops ride the
	 * {@link GTEntityBlock} fallback; this bridge hands over the table BE's differently
	 * spelled {@code inventory()} accessor (the {@code getInventory()} census default
	 * finds nothing here, the GTAnvilBlock bridge shape).
	 */
	@Override
	protected IItemHandler dropInventory(BlockEntity aTile) {
		return aTile instanceof GT6SiftingTableBlockEntity tTable ? tTable.inventory() : super.dropInventory(aTile);
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6SiftingTableBlockEntity tTable) {
			tTable.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
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
		if (aLevel.getBlockEntity(aPos) instanceof GT6SiftingTableBlockEntity tTable) {
			tTable.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(InteractionHand.MAIN_HAND),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	 *///?}
}
