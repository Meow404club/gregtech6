package gregtech6.block.foam;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.foam.GT6CFoamBlockEntity;

/**
 * The owned C-Foam block carrier — the block half of upstream {@code MultiTileEntityCFoam}
 * (MTE id 32765, Loader_MultiTileEntities.java:2023): a {@link GTEntityBlock} whose BE
 * ({@link GT6CFoamBlockEntity}) carries the foam face. Registered with NO BlockItem (the
 * upstream {@code showInCreative} false, MultiTileEntityCFoam.java:152) — the only
 * placement route is the owned spray.
 *
 * <p>State: the {@link #DRIED} property carries the wet/hardened visual leg (the
 * spec_rulings.ruling_color_dim owned_render form — the sprite pair swaps through the
 * blockstate variants, the paint colour rides the BE tint); the BE keeps the authoritative
 * flags and flips this property on every dried transition.
 *
 * <p>The upstream delegated faces over the dried state:
 * <ul>
 * <li>light opacity :134 — dried = 15 (LIGHT_OPACITY_MAX), wet = 1 (LIGHT_OPACITY_WATER)
 *     via {@link #getLightBlock};</li>
 * <li>hardness :136-137 — the CFoam/CFoamFresh delegation (4.0 dried / 1.0 wet) via
 *     {@link #getDestroyProgress} (the break-face form, no per-state destroy-time hook on
 *     the 1.20.1 block face);</li>
 * <li>collision :150 — the FULL block on both states (the upstream owned foam is never
 *     passable, unlike the plain fresh block);</li>
 * <li>not opaque while wet (:142-143 isSurfaceOpaque2 = mFoamDried) → the static
 *     {@code noOcclusion()} + the dynamic leg is render-side.</li>
 * </ul>
 */
public class GT6CFoamOwnedBlock extends GTEntityBlock {

	/** The wet/hardened visual-light leg; the BE flags stay authoritative. */
	public static final BooleanProperty DRIED = BooleanProperty.create("dried");

	/** The dried hardness (upstream :136 the CFoam delegation — BlockCFoam's 4.0). */
	public static final float HARDNESS_DRIED = 4.0F;

	/** The wet hardness (upstream :136 the CFoamFresh delegation — BlockCFoamFresh's 1.0). */
	public static final float HARDNESS_WET = 1.0F;

	/** The dried light block (upstream :134 LIGHT_OPACITY_MAX). */
	public static final int LIGHT_BLOCK_DRIED = 15;

	/** The wet light block (upstream :134 LIGHT_OPACITY_WATER). */
	public static final int LIGHT_BLOCK_WET = 1;

	public GT6CFoamOwnedBlock(BlockBehaviour.Properties aProperties) {
		super(aProperties);
		registerDefaultState(stateDefinition.any().setValue(DRIED, false));
	}

	/** The registration properties — the full-block carrier over the foam face. */
	public static BlockBehaviour.Properties ownedProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOL)
				.strength(HARDNESS_DRIED, 1.5F) // the static face is the dried numbers (the delegation below)
				.sound(SoundType.WOOL)
				.noOcclusion();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(DRIED);
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6FoamBlocks.CFOAM_OWNED_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the TestMachineBlock simpleCodec fork shape; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6CFoamOwnedBlock> codec() {
		return simpleCodec(GT6CFoamOwnedBlock::new);
	}
	*///?}

	/** Upstream :134 verbatim — the dried/wet light swap over the DRIED property. */
	@Override
	public int getLightBlock(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return aState.getValue(DRIED) ? LIGHT_BLOCK_DRIED : LIGHT_BLOCK_WET;
	}

	/**
	 * Upstream :136-137 — the mining-speed delegation to the dried/wet block numbers (4.0
	 * hardened / 1.0 wet). The 1.20.1 block face has no per-state destroy-time hook (the
	 * BlockStateBase caches the static destroyTime), so the delegation rides the
	 * getDestroyProgress break face — the p24 pipe-owner override precedent
	 * (GTFluidPipeBlock.java:175; the vanilla BlockBehaviour.getDestroyProgress
	 * 1.20.1:319-327 public form, public widening legal both legs). The static destroyTime
	 * is the DRIED numbers, so the wet progress scales by DRIED/WET = 4x — the exact
	 * upstream hardness ratio. Creative bypasses (ServerPlayerGameMode isCreative destroys
	 * immediately, the upstream behaviour).
	 */
	@Override
	public float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aLevel, BlockPos aPos) {
		boolean tDried = aState.getValue(DRIED);
		if (!tDried) {
			BlockEntity tBE = aLevel.getBlockEntity(aPos);
			tDried = !(tBE instanceof GT6CFoamBlockEntity tFoam && !tFoam.mFoamDried);
		}
		return foamDestroyProgress(tDried, super.getDestroyProgress(aState, aPlayer, aLevel, aPos));
	}

	/** The pure seam the offline tests drive: wet (false) mines 4x faster — the upstream :136 ratio. */
	public static float foamDestroyProgress(boolean aDried, float aSuperProgress) {
		return aDried ? aSuperProgress : aSuperProgress * (HARDNESS_DRIED / HARDNESS_WET);
	}

	/** The BE lookup helper for the command/stat faces (null when the position lost its foam). */
	@Nullable
	public static GT6CFoamBlockEntity foamOf(BlockGetter aLevel, BlockPos aPos) {
		return aLevel.getBlockEntity(aPos) instanceof GT6CFoamBlockEntity tFoam ? tFoam : null;
	}
}
