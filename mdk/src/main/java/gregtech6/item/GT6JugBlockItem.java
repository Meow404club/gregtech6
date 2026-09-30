package gregtech6.item;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

//? if forge {
import net.minecraftforge.common.capabilities.ICapabilityProvider;
//?} else {
/*import net.minecraft.world.item.component.CustomData;
import gregtech6.registry.GT6DataComponents;
 *///?}

import gregtech6.fluid.GTFluidLists;
import gregtech6.tileentity.tank.GT6JugBlockEntity;
import gregtech6.tileentity.tank.GT6JugItemFluidHandler;

/**
 * The Ceramic Jug BlockItem (task small-tank-jug) — the {@link GT6CupBlockItem} carrier
 * face set (the content-kills-stacking rule, the FLUID_HANDLER_ITEM capability, the
 * placeBlock round trip) PLUS the two jug-only interaction faces the upstream item
 * carried and the cup did not (MultiTileEntityJug.java:87-88 flags over the
 * TileEntityBase08FluidContainer item faces):
 *
 * <ul>
 * <li><b>scoop</b> ({@code use}, the upstream onItemRightClick :270-342 over
 *     {@code canPickUpFluids} :88): right-clicking AT a source fluid block raytraces
 *     the modern SOURCE_ONLY clip (the BottleItem/BucketItem form standing in for
 *     {@code WD.getMOP}), fills 1000 L of the source's fluid through the item handler
 *     and — on the full fill — removes the source block (the BucketPickup semantics:
 *     {@code setBlock(AIR, 11)}, the pickup sound, the FLUID_PICKUP game event). The
 *     upstream water-infinite-pool exception (WD.infiniteWater, :280-284) is not
 *     ported — the declared modern equivalence is scoop + source-gone + 1000 L; the
 *     River/Ocean/Swamp/IFluidBlock special blocks are vanilla-FluidState-covered or
 *     cut (the modded source rides its own FluidState).</li>
 * <li><b>watering</b> ({@code useOn}, the upstream onItemUseFirst :202-228 over
 *     {@code canWaterCrops} :87): a water-holding jug right-clicked on a cauldron pours
 *     the upstream three tiers — 1000/667/334 L into LEVEL +3/+2/+1, the head gate
 *     refuses a full or under-334 L pour. The GrC paddy / IC2 ICropTile / TC crucible
 *     branches of :229-262 have no modern host — declared cuts.</li>
 * </ul>
 */
public class GT6JugBlockItem extends BlockItem {

	/** The scoop dose — the upstream :281/:296 {@code FL.Water/Lava.make(1000)} bucket face. */
	public static final int SCOOP_MB = 1000;

	public GT6JugBlockItem(Block aBlock, Properties aProperties) {
		super(aBlock, aProperties);
	}

	/** The upstream :423 face — a filled jug stacks 1, the empty one rides the 16 registration column. */
	@Override
	public int getMaxStackSize(ItemStack aStack) {
		if (hasTankContent(aStack)) return 1;
		return super.getMaxStackSize(aStack);
	}

	/** The content probe over the shared key (the item handler's rehydration reads the same tag). */
	private static boolean hasTankContent(ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		return tTag != null && tTag.contains(GT6JugBlockEntity.NBT_TANK)
				&& tTag.getCompound(GT6JugBlockEntity.NBT_TANK).contains("Amount")
				&& tTag.getCompound(GT6JugBlockEntity.NBT_TANK).getInt("Amount") > 0;
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null) return false;
		CompoundTag tTank = tData.copyTag().getCompound(GT6JugBlockEntity.NBT_TANK);
		return tTank.contains("Amount") && tTank.getInt("Amount") > 0;
		 *///?}
	}

	//? if forge {
	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack aStack, @Nullable CompoundTag aNBT) {
		// the item-face gate rides the carrier: the fixed 2000 L row tank AND the liquid-only door
		return new GT6JugItemFluidHandler(aStack);
	}
	//?} else {
	/*// (1.21.1: IForgeItem.initCapabilities does not exist — the W4 registration wave
	// exposes the same GT6JugItemFluidHandler through RegisterCapabilitiesEvent
	// .registerItem(FLUID_HANDLER_ITEM, item, provider), the cup declared defer.)
	 *///?}

	// ---------------------------------------------------------------------------
	// the scoop face (upstream onItemRightClick :270-342 over canPickUpFluids :88)
	// ---------------------------------------------------------------------------

	/**
	 * The scoopable-source gate (the upstream :277/:295 metadata-0 source check — the
	 * modern {@code isSource()} form; non-empty covers water, lava AND modded source
	 * fluids in one FluidState face). Pure so the offline test pins it.
	 */
	public static boolean isScoopableSource(@Nullable FluidState aFluid) {
		return aFluid != null && !aFluid.isEmpty() && aFluid.isSource();
	}

	/**
	 * The scoop core (the upstream :281-284 simulate-then-execute form): 1000 L of the
	 * source's fluid into the handler, or 0 when the tank cannot take the full bucket —
	 * the caller removes the source block ONLY on the full dose. Pure handler+state, no
	 * level — the offline-test seam.
	 */
	public static int scoopInto(IFluidHandler aHandler, FluidState aFluid) {
		if (aHandler.fill(new FluidStack(aFluid.getType(), SCOOP_MB), IFluidHandler.FluidAction.SIMULATE) != SCOOP_MB) return 0;
		return aHandler.fill(new FluidStack(aFluid.getType(), SCOOP_MB), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level aLevel, Player aPlayer, InteractionHand aHand) {
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		if (!GT6JugBlockEntity.CAN_PICK_UP_FLUIDS) return InteractionResultHolder.pass(tStack);
		BlockHitResult tHit = getPlayerPOVHitResult(aLevel, aPlayer, ClipContext.Fluid.SOURCE_ONLY);
		if (tHit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(tStack);
		BlockPos tPos = tHit.getBlockPos();
		FluidState tFluid = aLevel.getFluidState(tPos);
		if (!isScoopableSource(tFluid) || !aLevel.mayInteract(aPlayer, tPos)) return InteractionResultHolder.pass(tStack);
		GT6JugItemFluidHandler tHandler = new GT6JugItemFluidHandler(tStack);
		if (scoopInto(tHandler, tFluid) != SCOOP_MB) return InteractionResultHolder.pass(tStack);
		if (!aLevel.isClientSide) {
			aLevel.setBlock(tPos, Blocks.AIR.defaultBlockState(), 11); // the LiquidBlock.pickupBlock form
			tFluid.getType().getPickupSound().ifPresent(tSound ->
					aLevel.playSound(null, tPos, tSound, SoundSource.PLAYERS, 1.0F, 1.0F)); // the BucketItem :60 face
			aLevel.gameEvent(aPlayer, GameEvent.FLUID_PICKUP, tPos); // the BucketItem :61 face
		}
		return InteractionResultHolder.sidedSuccess(tStack, aLevel.isClientSide());
	}

	// ---------------------------------------------------------------------------
	// the watering face (upstream onItemUseFirst :202-228 over canWaterCrops :87)
	// ---------------------------------------------------------------------------

	/**
	 * The upstream :215-227 tier table as a pure function — (tank amount, cauldron level)
	 * → {drain, newLevel}, or null when the pour refuses ({@code aMeta >= 3 ||
	 * mFluid.amount < 334} head :215, then the 1000/full, 667/+2, 334/+1 cascade). The
	 * offline-test seam for the 334/667/1000 equivalence face.
	 */
	@Nullable
	public static int[] cauldronTier(long aAmount, int aLevel) {
		if (aLevel >= 3 || aAmount < 334) return null;
		if (aAmount >= 1000 && aLevel <= 0) return new int[] {1000, aLevel + 3};
		if (aAmount >= 667 && aLevel <= 1) return new int[] {667, aLevel + 2};
		return new int[] {334, aLevel + 1}; // the :224 tail — aMeta <= 2 is the residual
	}

	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		if (GT6JugBlockEntity.CAN_WATER_CROPS && waterCauldron(aContext)) {
			return InteractionResult.sidedSuccess(aContext.getLevel().isClientSide());
		}
		return super.useOn(aContext); // the placement face
	}

	/** The cauldron pour (the :209-228 body): water only, the three tiers, BUCKET_EMPTY pour sound. */
	private boolean waterCauldron(UseOnContext aContext) {
		ItemStack tStack = aContext.getItemInHand();
		GT6JugItemFluidHandler tHandler = new GT6JugItemFluidHandler(tStack);
		FluidStack tContent = tHandler.getFluidInTank(0);
		if (tContent.isEmpty() || !"water".equals(GTFluidLists.name(tContent))) return false; // the :210 FL.water head
		BlockPos tPos = aContext.getClickedPos();
		BlockState tState = aContext.getLevel().getBlockState(tPos);
		int tLevel = tState.is(Blocks.WATER_CAULDRON) ? tState.getValue(LayeredCauldronBlock.LEVEL)
				: tState.is(Blocks.CAULDRON) ? 0 : -1;
		if (tLevel < 0) return false;
		int[] tTier = cauldronTier(tContent.getAmount(), tLevel);
		if (tTier == null) return false;
		tHandler.drain(new FluidStack(Fluids.WATER, tTier[0]), IFluidHandler.FluidAction.EXECUTE);
		if (!aContext.getLevel().isClientSide) {
			aContext.getLevel().setBlock(tPos,
					Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, tTier[1]), 3);
			aContext.getLevel().playSound(null, tPos, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
			aContext.getLevel().gameEvent(aContext.getPlayer(), GameEvent.BLOCK_CHANGE, tPos);
		}
		return true;
	}

	// ---------------------------------------------------------------------------
	// the placement round trip (the cup form)
	// ---------------------------------------------------------------------------

	/**
	 * The item → world half of the round trip (the upstream MTE placement read): after
	 * vanilla places the block, the fresh BE adopts the item's tank — the same key
	 * {@link gregtech6.block.tank.GT6JugBlock#getDrops} wrote into the drop.
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext aContext, BlockState aState) {
		if (!super.placeBlock(aContext, aState)) return false;
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (tBE instanceof GT6JugBlockEntity tJug) applyItemNBT(aContext.getItemInHand(), tJug);
		return true;
	}

	/** The tank read (the GT6CupBlockItem.applyItemNBT shape over the BE's own load gate — contains-guarded). */
	public static void applyItemNBT(ItemStack aStack, GT6JugBlockEntity aJug) {
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		if (tTag == null) return;
		aJug.readItemNBT(tTag);
		//?} else {
		/*CustomData tData = aStack.get(GT6DataComponents.BARREL_CONTENT);
		if (tData == null || tData.isEmpty()) return;
		aJug.readItemNBT(tData.copyTag());
		 *///?}
	}
}
