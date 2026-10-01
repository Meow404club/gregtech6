package gregtech6.block.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tank.GT6CellBlockEntity;

/**
 * The Capsule-Cell-Container block carrier — the block side of the small-tank cell
 * family (task small-tank-cell), the {@link GT6GasCylinderBlock}/GTBarrelBlock carrier
 * pattern: the block carries the upstream registration row's shared values
 * (Loader_MultiTileEntities.java:1770-1809, category "Fluid Containers": NBT_HARDNESS
 * 0.5, NBT_RESISTANCE 6.0 on ALL 40 rows, aUtilWood on the nine wax/plastic rows and
 * aUtilMetal on the 31 metal rows, NBT_TANK_CAPACITY 1000, NBT_GASPROOF T on every
 * row) plus the shared BET (the ADR-P3-1 multi-mount — all 40 rows are the one
 * upstream TE class), and the BE carries the tank.
 *
 * <p>Geometry: the upstream collision/selection pool MultiTileEntityCell.java:70-72 —
 * box(PX_P[5], 0, PX_P[5] → PX_N[5], PX_N[4], PX_N[5]) px = the 6x12x6 canister
 * silhouette. Rendering rides the shared static per-level elements model (the
 * {@link #LIQUID_LEVEL} 0..8 blockstate after the crucible-bowl-card form; the
 * GT6BlockStates.addCells colored+overlay grammar with the insides + fluid layers
 * behind the sides-tile window — no BER).
 *
 * <p>{@code use} is the container chain only (the GTBarrelBlock.use FluidUtil shape —
 * the held-container fill/drain clicks; the gas-only admission gate lives on the
 * handler, so a water bucket simply refuses). The upstream family has NO limit face
 * (that is the gas cylinder's :73-110 exclusivity) and NO drink face (gas-only
 * admission keeps isDrinkable false — the pool cut on the BE doc). The upstream
 * {@code IMTE_IgnorePlayerCollisionWhenPlacing} flag has no BlockBehaviour seam —
 * declared deviation (the sub-cube collision makes it near-invisible).
 *
 * <p>The break-drop projects the BE's tank NBT onto the family item (the
 * GTBarrelBlock.getDrops seam — content survives break; placement reads it back
 * through {@link gregtech6.item.GT6CellBlockItem#placeBlock}). Since task
 * small-tank-colored-tint the colored band carries the tintindex-0 seat and the row
 * material rides the {@link #materialOf} carrier into the combined
 * {@code GTMachinePaintTint.tintMaterialOf} dispatch — the baked GTMachineTintModel
 * multiplies the row colour exactly like the upstream
 * {@code BlockTextureDefault(colored, mRGBa)} passes (MultiTileEntityCell.java:42-44);
 * the former measuring-pot declared deviation retires.
 */
public class GT6CellBlock extends GTEntityBlock {

	/** The upstream collision/selection pool :70-72 — box(5,0,5 → 11,12,11) px (6x12x6, the canister silhouette). */
	public static final VoxelShape SHAPE = Shapes.box(5.0 / 16.0, 0.0, 5.0 / 16.0, 11.0 / 16.0, 12.0 / 16.0, 11.0 / 16.0);

	/** The fill-level bucket 0..8 (the crucible {@code gt_liquid_level} property form — 9 variants, 9 shared models). */
	public static final IntegerProperty LIQUID_LEVEL = IntegerProperty.create("gt_liquid_level", 0, 8);

	/**
	 * The row properties (upstream :1770-1809 NBT_HARDNESS 0.5, NBT_RESISTANCE 6.0 on
	 * every row; the aUtilWood rows ride WOOD, the aUtilMetal rows the metal-drum COPPER
	 * stand-in). The sound is the only host-block face the port carries.
	 */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties rowProperties(boolean aWoodHost) {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(0.5F, 6.0F).sound(aWoodHost ? SoundType.WOOD : SoundType.COPPER)
				.noOcclusion().isViewBlocking(GT6CellBlock::never);
	}

	/** The sub-cube rider (the GT6MeasuringPotBlock seam body — the canister silhouette never blocks the view). */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/** The row's upstream {@code NBT_MATERIAL} column (Loader_MultiTileEntities.java:1770-1809), resolved lazily — the GTBarrelBlock carrier shape. */
	@javax.annotation.Nullable
	private final java.util.function.Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	public GT6CellBlock(Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			@javax.annotation.Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		mMaterial = aMaterial;
		registerDefaultState(getStateDefinition().any().setValue(LIQUID_LEVEL, 0));
	}

	/**
	 * The row's upstream {@code NBT_MATERIAL}, resolved lazily through the Supplier (the
	 * {@link GTBarrelBlock#material} mirror); null keeps the white identity (upstream
	 * UNCOLORED, CS.java:327 — every cell row carries a material, the column is
	 * recorded-only future-proofing).
	 */
	@javax.annotation.Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The cell-domain material dispatch (the {@code GTBarrelBlock.materialOf} mirror
	 * shape): only the carrier blocks resolve a material — every other block is null here.
	 */
	@javax.annotation.Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@javax.annotation.Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6CellBlock tCell ? tCell.material() : null;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GT6CellBlock> codec() {
		return simpleCodec(aProperties -> new GT6CellBlock(mTickerType, mMaterial, aProperties));
	}
	 *///?}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<
			net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {
		aBuilder.add(LIQUID_LEVEL);
	}

	/** The sub-cube canister silhouette (selection AND collision — the vanilla getShape delegation, the pot form). */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		// the container chain (the GTBarrelBlock.use FluidUtil shape — both sides, the server
		// pass authoritative; the gas-only gate lives on the handler, liquids refuse)
		return FluidUtil.interactWithFluidHandler(aPlayer, aHand, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem receives both empty-hand and item-hand
	// right clicks exactly like the 1.20.1 use(); MAIN_HAND stands in, the GTBarrelBlock
	// 1.21.1 declared deviation.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		return FluidUtil.interactWithFluidHandler(aPlayer, InteractionHand.MAIN_HAND, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	 *///?}

	/**
	 * The break-drop face — the GTBarrelBlock.getDrops seam over this family's own BE:
	 * exactly one item, the registry stack carrying the BE's tank (the upstream
	 * writeItemNBT2 item face; {@code tank} alone when content is — the write gate keeps
	 * the key present iff content is, so an empty cell drops tag-less and stacks clean).
	 */
	@Override
	public List<ItemStack> getDrops(BlockState aState, LootParams.Builder aBuilder) {
		BlockEntity tBE = aBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(tBE instanceof GT6CellBlockEntity tCell)) return super.getDrops(aState, aBuilder);
		List<ItemStack> tDrops = new ArrayList<>(1); // upstream :158 rList = ST.arraylist()
		//? if forge {
		ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tCell.writeItemNBT(tTag);
		tStack.setTag(tTag.isEmpty() ? null : tTag); // the empty cell keeps the tag-less drop shape
		tDrops.add(tStack);
		//?} else {
		/*ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tCell.writeItemNBT(tTag);
		if (!tTag.isEmpty()) net.minecraft.world.item.component.CustomData.set(
				gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tTag); // the opaque small-tank envelope, the payload keys unchanged
		tDrops.add(tStack);
		 *///?}
		return tDrops;
	}
}
