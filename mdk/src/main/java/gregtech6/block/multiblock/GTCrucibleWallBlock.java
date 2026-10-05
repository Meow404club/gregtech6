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
 *
 * <p>The DESIGN render dimension rides the metalwall group's {@code NBT_DESIGNS 7}
 * (Loader:1143-1153, the eleven siblings share the column): maxDesign 7 = the design
 * 0..7 property, so the checker's formed-wall write (the upstream :124-128 second
 * pass — every crucible wall repaints design 4 when the structure forms, task
 * mb-formed-crucible-wall) lands in the blockstate through the shared
 * setDesign→syncDesignToState seam. The 1.20.1 faces are the per-design two-layer
 * partModel walk (GT6BlockStates.addLargeCrucible).
 */
public class GTCrucibleWallBlock extends GTMultiBlockPartBlock {

	/** The metalwall group's {@code NBT_DESIGNS} (Loader:1143-1153 — the design 0..7 inclusive range). */
	public static final int WALL_DESIGNS = 7;

	public GTCrucibleWallBlock(Properties aProperties) {
		super(aProperties, WALL_DESIGNS);
	}

	/**
	 * The material-carrier form (task issue8-residual): the wall's upstream {@code
	 * NBT_MATERIAL} (the Loader :1145 "Steel Wall" column) rides the parent's 4-arg
	 * row-less tint ctor — the lazy Supplier is the GTBarrels form (MT.init runs after
	 * class-load). Null = the material-less identity (the coke-bricks posture).
	 * maxDesign = the metalwall NBT_DESIGNS 7 (task mb-formed-crucible-wall — was 0,
	 * the formed-wall repaint had no property to land in).
	 */
	public GTCrucibleWallBlock(Properties aProperties, @Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties, WALL_DESIGNS, null, aMaterial);
	}

	/**
	 * The composed-name ctor (task w3-distill-crucible ③ — the 8-material ladder): the
	 * "{@code <mat> Wall}" template over the EXISTING gt6.row.mat unit words (the card ①
	 * metal-wall composition — zero new lang unit keys). The ladder rung's
	 * upstream NBT_MATERIAL rides the same tint ctor (task issue8-residual); maxDesign =
	 * the metalwall NBT_DESIGNS 7 (task mb-formed-crucible-wall).
	 */
	public GTCrucibleWallBlock(Properties aProperties, String aTemplateKey, String aUnitKey,
			@Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties, WALL_DESIGNS, net.minecraft.network.chat.Component.translatable(aTemplateKey,
				net.minecraft.network.chat.Component.translatable(aUnitKey)), aMaterial);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6Crucibles.CRUCIBLE_WALL_BE.get().create(aPos, aState);
	}
}
