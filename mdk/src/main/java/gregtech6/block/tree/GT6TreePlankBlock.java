package gregtech6.block.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * The GT6 tree plank (task gt-tree-planks): one plain cube per {@link GT6TreeKind} — the
 * upstream {@code BlockTreePlanks} meta 0-7 and {@code BlockTreePlanks2} meta 0 rows fully
 * expanded per the P8 ADR ④ per-pair precedent (the tree-card granularity; the sawing walk
 * consumes one plank item per species). Properties are the upstream BlockMetaType row
 * verbatim: hardness {@code aHardnessMultiplier(1) * 1.5F} (BlockMetaType.java:61) and
 * resistance {@code aResistanceMultiplier(1) * 10.0F} (:62) over Material.wood /
 * soundTypeWood / harvestLevel 0 (BlockTreePlanks.java:39) — the modern
 * {@code strength(1.5F, 10.0F).sound(WOOD)} composition.
 *
 * <p>Fire behaviour is the {@code BlockBasePlanksFlammable} face verbatim
 * (flammability 20 / fire spread 5 / not a fire source, BlockBasePlanksFlammable.java:43-45)
 * via the platform {@code getFlammability}/{@code getFireSpreadSpeed}/{@code isFireSource}
 * block-extension overrides — shape-identical on both legs (forge IForgeBlock.java /
 * NeoForge IBlockExtension.java, verified javap over both leg jars; no vanilla tag route
 * and no FlammableBlockRegistry import needed). Function tags (#minecraft:planks block+item,
 * mineable/axe) are the datagen band's face (GT6BlockTags/GT6ItemTags addPlankBand). The
 * slab face ({@code mSlabs}, BlockTreePlanks makeSlab) and the FireProof twins stay
 * upstream-only — declared out of scope, the sawing unlock needs the plank items only.
 */
public final class GT6TreePlankBlock extends Block {

    /** The upstream plank flammability (BlockBasePlanksFlammable.java:44, every meta). */
    public static final int FLAMMABILITY = 20;
    /** The upstream plank fire spread speed (BlockBasePlanksFlammable.java:45, every meta). */
    public static final int FIRE_SPREAD_SPEED = 5;

    private final GT6TreeKind mKind;

    public GT6TreePlankBlock(GT6TreeKind aKind) {
        super(Properties.of().mapColor(MapColor.WOOD).strength(1.5F, 10.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
        mKind = aKind;
    }

    public GT6TreeKind kind() {
        return mKind;
    }

    @Override
    public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
        return FIRE_SPREAD_SPEED;
    }

    @Override
    public boolean isFireSource(BlockState aState, LevelReader aLevel, BlockPos aPos, Direction aDirection) {
        return false;
    }
}
