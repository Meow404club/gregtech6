package gregtech6.block.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
import gregtech6.tileentity.tank.GT6GasCylinderBlockEntity;

/**
 * The Barometer Gas Cylinder block carrier — the block side of the small-tank
 * gas-cylinder family (task small-tank-gas-cylinder), the GT6MeasuringPotBlock/
 * GTBarrelBlock carrier pattern: the block carries the upstream registration row's
 * values (Loader_MultiTileEntities.java:2101-2104, category "Fluid Containers":
 * NBT_HARDNESS 0.5, NBT_RESISTANCE 6.0 steel/stainless + 10.0 tungsten/Ta4HfC5,
 * aUtilMetal, NBT_TANK_CAPACITY 8000 on every row) plus the shared BET (the ADR-P3-1
 * multi-mount — all four rows are the one upstream TE class), and the BE carries the
 * tank + the NBT_MODE limit.
 *
 * <p>{@code use} is the two-arm click face (upstream onBlockActivated3 :73-110):
 * <ul>
 * <li>EMPTY hand on a HORIZONTAL side → the hitY fill-limit re-bind
 *     ({@link GT6GasCylinderBlockEntity#limitChain} — the eight-band chain, the
 *     measuring-pot face at four more bands);</li>
 * <li>everything else → {@code FluidUtil.interactWithFluidHandler} through the BE's
 *     FLUID_HANDLER capability (the GTBarrelBlock.use shape — the held-container
 *     fill/drain clicks; the gas-only admission gate lives on the handler, so a water
 *     bucket simply refuses). An empty hand on a vertical side falls through to PASS
 *     (the upstream super arm — the drink face rides the pool cut).</li>
 * </ul>
 *
 * <p>Geometry: the upstream collision/selection pool :144-146 — box(4,0,4 → 12,16,12)
 * px, the full-height 8x16x8 bell silhouette — over the {@code noOcclusion()} properties
 * seam (the kitchen family's #9 X-ray fix shape). Rendering rides the static two-layer
 * elements model (the GT6BlockStates.addGasCylinders colored+overlay grammar, no BER,
 * no fluid display pass — the Base09 no-sync tier-S face).
 *
 * <p>The break-drop projects the BE's tank+mode NBT onto the family item (the
 * GTBarrelBlock.getDrops seam — content survives break; placement reads it back through
 * {@link gregtech6.item.GT6GasCylinderBlockItem#placeBlock}). Since task
 * small-tank-colored-tint the colored band carries the tintindex-0 seat and the row
 * material rides the {@link #materialOf} carrier into the combined
 * {@code GTMachinePaintTint.tintMaterialOf} dispatch — the baked GTMachineTintModel
 * multiplies the row colour exactly like the upstream
 * {@code BlockTextureDefault(colored, mRGBa)} pass (:141, the barometer arm included);
 * the former measuring-pot declared deviation retires.
 */
public class GT6GasCylinderBlock extends GTEntityBlock {

	/** The upstream collision/selection pool :144-146 — box(4,0,4 → 12,16,12) px (8x16x8, full height). */
	public static final VoxelShape SHAPE = Shapes.box(0.25, 0.0, 0.25, 0.75, 1.0, 0.75);

	/**
	 * The row properties (upstream :2101-2104 NBT_HARDNESS 0.5; NBT_RESISTANCE 6.0 on the
	 * steel/stainless rows, 10.0 on the tungsten/Ta4HfC5 rows; aUtilMetal → the iron tool
	 * set). The sound is the metal-drum COPPER stand-in (the aUtilMetal carrier rows).
	 */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties rowProperties(float aResistance) {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(0.5F, aResistance).sound(SoundType.COPPER)
				.noOcclusion().isViewBlocking(GT6GasCylinderBlock::never);
	}

	/** The sub-cube rider (the GT6MeasuringPotBlock seam body — the bell silhouette never blocks the view). */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/** The row's upstream {@code NBT_MATERIAL} column (Loader_MultiTileEntities.java:2101-2104), resolved lazily — the GTBarrelBlock carrier shape. */
	@Nullable
	private final Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	public GT6GasCylinderBlock(Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			@Nullable Supplier<gregapi.oredict.OreDictMaterial> aMaterial,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		mMaterial = aMaterial;
	}

	/**
	 * The row's upstream {@code NBT_MATERIAL}, resolved lazily through the Supplier (the
	 * {@link GTBarrelBlock#material} mirror); null keeps the white identity (upstream
	 * UNCOLORED, CS.java:327 — every cylinder row carries a material).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The gas-cylinder-domain material dispatch (the {@code GTBarrelBlock.materialOf}
	 * mirror shape): only the carrier blocks resolve a material — every other block is
	 * null here.
	 */
	@Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6GasCylinderBlock tCylinder ? tCylinder.material() : null;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GT6GasCylinderBlock> codec() {
		return simpleCodec(aProperties -> new GT6GasCylinderBlock(mTickerType, mMaterial, aProperties));
	}
	 *///?}

	/** The sub-cube bell silhouette (selection AND collision — the vanilla getShape delegation, the pot form). */
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
		if (aLevel.getBlockEntity(aPos) instanceof GT6GasCylinderBlockEntity tCylinder
				&& aPlayer.getItemInHand(aHand).isEmpty()
				&& aHit.getDirection().getAxis().isHorizontal()) { // :75 SIDES_HORIZONTAL + null-held gate
			tCylinder.limitChain(aPlayer, (float) (aHit.getLocation().y - aPos.getY()));
			return InteractionResult.SUCCESS;
		}
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
		if (aLevel.getBlockEntity(aPos) instanceof GT6GasCylinderBlockEntity tCylinder
				&& aPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
				&& aHit.getDirection().getAxis().isHorizontal()) {
			tCylinder.limitChain(aPlayer, (float) (aHit.getLocation().y - aPos.getY()));
			return InteractionResult.SUCCESS;
		}
		return FluidUtil.interactWithFluidHandler(aPlayer, InteractionHand.MAIN_HAND, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	 *///?}

	/**
	 * The break-drop face — the GTBarrelBlock.getDrops seam over this family's own BE:
	 * exactly one item, the registry stack carrying the BE's tank+mode pair (the upstream
	 * writeItemNBT2 :61-65 item face; {@code tank} alone when the limit never left the
	 * class default — the tag-less empty drop, the barrel write gate).
	 */
	@Override
	public List<ItemStack> getDrops(BlockState aState, LootParams.Builder aBuilder) {
		BlockEntity tBE = aBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(tBE instanceof GT6GasCylinderBlockEntity tCylinder)) return super.getDrops(aState, aBuilder);
		List<ItemStack> tDrops = new ArrayList<>(1); // upstream :158 rList = ST.arraylist()
		//? if forge {
		ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tCylinder.writeItemNBT(tTag);
		tStack.setTag(tTag.isEmpty() ? null : tTag); // the default-capacity cylinder keeps the tag-less drop shape
		tDrops.add(tStack);
		//?} else {
		/*ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tCylinder.writeItemNBT(tTag);
		if (!tTag.isEmpty()) net.minecraft.world.item.component.CustomData.set(
				gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tTag); // the opaque small-tank envelope, the payload keys unchanged
		tDrops.add(tStack);
		 *///?}
		return tDrops;
	}
}
