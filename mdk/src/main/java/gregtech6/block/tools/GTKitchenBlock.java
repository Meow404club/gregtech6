package gregtech6.block.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
 */
public class GTKitchenBlock extends GTEntityBlock {

	private final long mCapacityL;
	private final Supplier<OreDictMaterial> mMaterial;
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * @param aCapacityL the per-tank litres (upstream {@code NBT_TANK_CAPACITY}: wood pot
	 *        4000, steel pot 8000, ceramic bowl 8000 — Loader_MultiTileEntities.java:2173/
	 *        :2175/:2177; the BE sizes its input AND output tank ARRAYS from the recipe
	 *        map and every tank to THIS capacity, the upstream :73-78 semantics)
	 * @param aMaterial the upstream {@code NBT_MATERIAL} (resolved lazily — class-load
	 *        precedes MT.init); only its {@code mMeltingPoint} is consumed (the -100 K
	 *        doors)
	 * @param aTickerType the family BET (pot pair share one, the bowl mounts its own)
	 */
	public GTKitchenBlock(long aCapacityL, Supplier<OreDictMaterial> aMaterial,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mCapacityL = aCapacityL;
		mMaterial = aMaterial;
		mTickerType = aTickerType;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GTKitchenBlock> codec() {
		return simpleCodec(aProperties -> new GTKitchenBlock(mCapacityL, mMaterial, mTickerType, aProperties));
	}
	 *///?}

	/** The per-tank litres (upstream NBT_TANK_CAPACITY, the GTBarrelBlock.capacityL carrier seam). */
	public long capacityL() {
		return mCapacityL;
	}

	/** The upstream NBT_MATERIAL — only {@code mMeltingPoint} is read (the -100 K doors). */
	public OreDictMaterial material() {
		return mMaterial.get();
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
