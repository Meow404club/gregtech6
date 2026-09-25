package gregtech6.block.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Crucibles;

/**
 * The LARGE-crucible wall part block (upstream "Steel Wall", Loader_MultiTileEntities.java
 * :1145 — part id 18009, the metalwall texture family, hardness == resistance 6.0; the
 * crucible's NBT_DESIGN :1270 wall reference becomes a Block identity, the p13 wall-variant
 * shape). The {@link GTMultiBlockPartBlock} body with the one difference that matters: the
 * block-entity factory mounts the {@link gregtech6.tileentity.multiblocks.CrucibleWallBlockEntity}
 * (the energy + crucible relaying part) over its OWN BET — the shared part BET cannot mount
 * it without a GTMultiBlocks.java touch this card does not own (the GTHeatTransmitterBlock
 * precedent verbatim). Everything else (onPlace/playerWillDestroy propagation, the
 * no-onRemove red line, RenderShape.MODEL) is inherited untouched.
 */
public class GTCrucibleWallBlock extends GTMultiBlockPartBlock {

	public GTCrucibleWallBlock(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The material-carrier form (task issue8-residual): the wall's upstream {@code
	 * NBT_MATERIAL} (the Loader :1145 "Steel Wall" column) rides the parent's 4-arg
	 * row-less tint ctor — the lazy Supplier is the GTBarrels form (MT.init runs after
	 * class-load). Null = the material-less identity (the coke-bricks posture).
	 */
	public GTCrucibleWallBlock(Properties aProperties, @Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties, 0, null, aMaterial);
	}

	/**
	 * The composed-name ctor (task p29-w3-distill-crucible ③ — the 8-material ladder): the
	 * "{@code <mat> Wall}" template over the EXISTING gt6.row.mat unit words (the card ①
	 * metal-wall composition — zero new lang unit keys; the composed carrier is the
	 * GTMultiBlockPartBlock :134 form, DESIGNS 0 → no DESIGN property). The ladder rung's
	 * upstream NBT_MATERIAL rides the same tint ctor (task issue8-residual).
	 */
	public GTCrucibleWallBlock(Properties aProperties, String aTemplateKey, String aUnitKey,
			@Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties, 0, net.minecraft.network.chat.Component.translatable(aTemplateKey,
				net.minecraft.network.chat.Component.translatable(aUnitKey)), aMaterial);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6Crucibles.CRUCIBLE_WALL_BE.get().create(aPos, aState);
	}
}
