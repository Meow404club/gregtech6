package gregtech6.block.foam;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;

/**
 * Dried (hardened) C-Foam — the 1.20.1 counterpart of upstream {@code BlockCFoam}
 * (gregtech/blocks/BlockCFoam.java:37-76): hardness 4.0 / resistance 1.5 / stone sound
 * (:39 verbatim numbers), drops itself (the vanilla BlockItem default — the upstream
 * getDrops self default), the foam face: {@code applyFoam}/{@code dryFoam} constant false
 * (:53-60 — it IS already-dried foam), {@code hasFoam} true (:68-70), {@code driedFoam}
 * true (:73-75), {@code removeFoam} → air (:63-65).
 *
 * <p>The upstream :40 side effect ({@code MT.ConstructionFoam.mTextureSolid} borrowing the
 * light-gray icon) is render plumbing — this port's texture choice rides the datagen model
 * (the {@code cfoam_hardened} grayscale sprite), the spec_rulings.ruling_color_dim form.
 * The upstream harvest level 1 (:39 third int) has no port tool-tag wiring in this card
 * (the tool-system tags own that seam) — declared cut.
 */
public class GT6CFoamBlock extends Block implements IBlockFoamable {

	public GT6CFoamBlock(BlockBehaviour.Properties aProperties) {
		super(aProperties);
		registerDefaultState(stateDefinition.any().setValue(GT6CFoamFreshBlock.COLOR, 0));
	}

	/** The registration properties — the offline-assert seam (the GTGrassBlock precedent). */
	public static BlockBehaviour.Properties driedProperties() {
		// upstream :39 verbatim numbers: hardness 4.0, resistance 1.5, rock+stone face
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(4.0F, 1.5F)
				.sound(SoundType.STONE);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(GT6CFoamFreshBlock.COLOR);
	}

	// -------------------------------------------------------------------------
	// the IBlockFoamable face (upstream :53-75)
	// -------------------------------------------------------------------------

	@Override
	public boolean applyFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide, int aRGB, int aDyeIndex) {
		return false; // upstream :53-55 verbatim
	}

	@Override
	public boolean dryFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return false; // upstream :58-60 verbatim
	}

	@Override
	public boolean removeFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		aLevel.removeBlock(aPos, false); // upstream :63-65
		return true;
	}

	@Override
	public boolean hasFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return true; // upstream :68-70
	}

	@Override
	public boolean driedFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return true; // upstream :73-75
	}
}
