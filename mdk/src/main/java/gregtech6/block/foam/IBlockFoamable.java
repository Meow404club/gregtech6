package gregtech6.block.foam;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The block-side C-Foam face — the 1.20.1 counterpart of upstream
 * {@code gregapi/block/IBlockFoamable.java:27-42} (five methods verbatim). Consumers:
 * the spray's block arm (Behavior_Spray_Foam.java:116 — this card) and the Remover's
 * block arm (Behavior_Spray_Foam_Remover.java — task card C).
 *
 * <p>Modernization folds (both recorded, the pipe-BE precedent):
 * <ul>
 * <li>{@code (World, int x, int y, int z)} → {@code (Level, BlockPos)};</li>
 * <li>the {@code byte aSide} side index → {@code Direction}, {@code SIDE_ANY} = {@code null}
 *     (the 0..5 index set and {@link Direction#get3DDataValue()} agree value-for-value,
 *     the GT6FoamSprayItem useOn precedent); every implementation in this family carries
 *     one foam state across all faces, so the side is uniformity-only;</li>
 * <li>{@code short[] aCFoamRGB + byte aVanillaColor} → {@code (int aRGB, int aDyeIndex)} —
 *     the DYES table slot rides next to its final 0xRRGGBB value ({@code UT.Code.getRGBInt}
 *     is identity over the table, the GTFluidPipeBlockEntity.applyFoam fold).</li>
 * </ul>
 *
 * <p>Implementations (the research.p26-r-foam-family census): the fresh/dried full blocks
 * and the fresh/dried slabs ({@code BlockCFoamFresh.java:45} / {@code BlockCFoam.java:37}
 * implement the upstream face; the slabs are the mSlabs forms of the same two classes).
 * The owned BE carries the TE-side face instead ({@code ITileEntityFoamable} — upstream
 * {@code MultiTileEntityCFoam.java:52}). Both plain foamable blocks answer
 * {@link #applyFoam} {@code false} CONSTANTLY — they ARE the foam (upstream :105-107/:53-55);
 * the block arm is therefore shape-only for them (the research "零行为差" finding).
 */
public interface IBlockFoamable {

	/** @return if it got applied successfully (upstream :29). */
	boolean applyFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide, int aRGB, int aDyeIndex);

	/** @return if it got dried successfully (upstream :32 — fresh → dried, colour preserved). */
	boolean dryFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide);

	/** @return if it got removed successfully (upstream :35 — the foam becomes air). */
	boolean removeFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide);

	/** @return if it is foamed (upstream :38). */
	boolean hasFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide);

	/** @return if it is dried (upstream :41). */
	boolean driedFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide);
}
