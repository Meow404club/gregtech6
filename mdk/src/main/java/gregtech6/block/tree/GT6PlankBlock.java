package gregtech6.block.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 plank cube (task gt-tree-planks, generalized by task planks-blockification): a
 * plain cube per upstream {@code BlockTreePlanks} meta row — both the 9 tree species rows
 * (BlockTreePlanks meta 0-7 + BlockTreePlanks2 meta 0, one block per {@link GT6TreeKind})
 * and the 8 generic rows (metas 8-15, {@link gregtech6.registry.GT6TreeBlocks#GENERIC_PLANKS})
 * — fully expanded per the P8 ADR ④ per-pair precedent (the tree-card granularity; 1.20.1
 * has no metadata to fold the upstream 16-meta block into). Properties are the upstream
 * BlockMetaType row verbatim: resistance {@code aResistanceMultiplier(1) * 10.0F}
 * (BlockMetaType.java:62) over Material.wood / soundTypeWood / harvestLevel 0
 * (BlockTreePlanks.java:39); the hardness is the {@code getBlockHardness} split verbatim
 * (BlockTreePlanks.java:90 — meta {@code < 12} rides the {@code 1.0F} multiplier over the
 * base {@code 1.5F} (BlockMetaType.java:61), metas 12-15 (Dead/Rotten/Mossy/Frozen) ride
 * {@code 0.5F} = 0.75F).
 *
 * <p>Fire behaviour is the {@code BlockBasePlanksFlammable} face verbatim
 * (flammability 20 / fire spread 5 / not a fire source, BlockBasePlanksFlammable.java:43-45)
 * via the platform {@code getFlammability}/{@code getFireSpreadSpeed}/{@code isFireSource}
 * block-extension overrides — shape-identical on both legs (forge IForgeBlock.java /
 * NeoForge IBlockExtension.java, verified javap over both leg jars; no vanilla tag route
 * and no FlammableBlockRegistry import needed). The upstream class carries EVERY meta
 * including the treated row — the treated planks' creosote face rides the recipe chain,
 * not the block class. Function tags (#minecraft:planks block+item, mineable/axe) are the
 * datagen band's face (GT6BlockTags/GT6ItemTags addPlankBand). The slab face ({@code mSlabs},
 * BlockTreePlanks makeSlab) and the FireProof twins stay upstream-only — declared out of
 * scope, the sawing unlock needs the plank items only.
 */
public class GT6PlankBlock extends Block {

    /** The upstream plank flammability (BlockBasePlanksFlammable.java:44, every meta). */
    public static final int FLAMMABILITY = 20;
    /** The upstream plank fire spread speed (BlockBasePlanksFlammable.java:45, every meta). */
    public static final int FIRE_SPREAD_SPEED = 5;
    /** The upstream base plank hardness (BlockMetaType.java:61, multiplier 1 — metas 0-11). */
    public static final float HARDNESS = 1.5F;
    /** The upstream soft plank hardness (BlockTreePlanks.java:90 — metas 12-15 ride the 0.5F split). */
    public static final float HARDNESS_SOFT = 0.75F;

    /**
     * @param aHardness the destroy-speed row: {@link #HARDNESS} (metas 0-11) or
     *                  {@link #HARDNESS_SOFT} (the Dead/Rotten/Mossy/Frozen rows, the :90 split).
     */
    public GT6PlankBlock(float aHardness) {
        super(Properties.of().mapColor(MapColor.WOOD).strength(aHardness, 10.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
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
