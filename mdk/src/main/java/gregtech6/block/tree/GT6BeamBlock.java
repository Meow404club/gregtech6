package gregtech6.block.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * The GT6 wood beam (task beam-blocks-register): one {@link RotatedPillarBlock} per
 * {@link GT6BeamKind} — the AXIS pillar face the upstream pillar render id rides
 * (BlockBaseBeam.java:60 PILLAR_RENDER = CS.java:764 render 31, the vanilla log renderer;
 * :64 onBlockPlaced → the orientation axis; :55 damageDropped → the orientation-free
 * meta, which the one-block-per-wood form gets for free). Properties follow the upstream
 * overrides: hardness = the vanilla log hardness/2 (BlockBaseBeam.java:57 — 2.0/2 = 1.0),
 * blast resistance = the log's 2.0 (:58), wood sound, mapColor WOOD (the
 * GT6TreeLogBlock shape; 1.0F strength default resistance 2.0 differs — the explicit
 * (1.0F, 2.0F) pair pins the :58 face).
 *
 * <p>Flammability is the BlockBaseBeamFlammable override verbatim (5/5,
 * BlockBaseBeamFlammable.java:33-34) on the platform extension face
 * (IForgeBlock.java:614/:655 = IBlockExtension.java:646/:685, same signature both legs).
 *
 * <p>The FireProof twins (task beam-fireproof-closeout, Loader_Woods.java:49/:51/:53/:57/
 * :59/:61) are the SAME block shape with the flammability face off: upstream
 * {@code BlockTreeBeam*FireProof extends BlockBaseBeam} (NOT the Flammable subclass —
 * BlockTreeBeam1FireProof.java:27 vs BlockTreeBeam1.java:27; identical texture arrays,
 * the LH rows add " (Fireproof)"), so the twin is this one class with the fireproof flag
 * holding the overrides at the plain-block 0/0 default — getFlammability/
 * getFireSpreadSpeed are the ONLY behavioural difference between the upstream pair.
 */
public final class GT6BeamBlock extends RotatedPillarBlock {

    private final GT6BeamKind mKind;
    /** True = the FireProof twin (Loader_Woods.java:49-61, BlockBaseBeam NOT BlockBaseBeamFlammable). */
    private final boolean mFireproof;

    public GT6BeamBlock(GT6BeamKind aKind) {
        this(aKind, false);
    }

    /** The FireProof twin constructor ({@code <kind>_beam_fireproof}). */
    public GT6BeamBlock(GT6BeamKind aKind, boolean aFireproof) {
        super(Properties.of().mapColor(MapColor.WOOD).strength(1.0F, 2.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
        mKind = aKind;
        mFireproof = aFireproof;
    }

    public GT6BeamKind kind() {
        return mKind;
    }

    /** The FireProof twin flag ({@code <kind>_beam_fireproof}). */
    public boolean fireproof() {
        return mFireproof;
    }

    @Override
    public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
        return mFireproof ? 0 : 5; // BlockBaseBeamFlammable.java:33 — the twin keeps the plain BlockBaseBeam default (no override upstream)
    }

    @Override
    public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
        return mFireproof ? 0 : 5; // BlockBaseBeamFlammable.java:34
    }
}
