package gregtech6.block.tree;

/**
 * One GT6 tree species row (task p30-w6-t1-trees-nine): the 9 worldgen trees of the
 * upstream Saplings_AB meta 0-7 + Saplings_CD meta 0 face (BlockTreeSaplingAB.java:47-54
 * and BlockTreeSaplingCD.java:45 LH rows — the display-name evidence this enum's columns
 * transcribe verbatim). The per-pair granularity is the P8 ADR ④ precedent
 * (GTStoneBlocks/GTGrassBlocks): the upstream universe is per-meta ids
 * ({@code ST.make(this, 1, aMeta)}), which a single EnumProperty block cannot express.
 *
 * <p>{@code snake} is the registry id segment: {@code <snake>_sapling} / {@code <snake>_log}
 * / {@code <snake>_leaves} (the GTGrassBlocks single-source naming rule; blue_mahoe and
 * blue_spruce snake per the upstream config names {@code tree.bluemahoe}/{@code
 * tree.bluespruce}, Loader_Worldgen.java:611/:616, with the word split).
 *
 * <p>{@code zhName} is the hand row (no upstream zh dump face exists for these blocks —
 * the anvil-family precedent, GT6ZhCn addAnvilUnits): the standard Chinese tree words.
 */
public enum GT6TreeKind {
    RUBBER("rubber", "Rubber", "橡胶树"),
    MAPLE("maple", "Maple", "枫树"),
    WILLOW("willow", "Willow", "柳树"),
    BLUE_MAHOE("blue_mahoe", "Blue Mahoe", "蓝梧桐"),
    HAZEL("hazel", "Hazel", "榛树"),
    CINNAMON("cinnamon", "Cinnamon", "肉桂"),
    COCONUT("coconut", "Coconut", "椰子树"),
    RAINBOWOOD("rainbowood", "Rainbowood", "彩虹木"),
    BLUE_SPRUCE("blue_spruce", "Blue Spruce", "蓝云杉");

    /** The registry id segment ({@code <snake>_sapling/_log/_leaves}). */
    private final String mSnake;
    /** The upstream display word (BlockTreeSaplingAB.java:47-54 / BlockTreeSaplingCD.java:45). */
    private final String mEnName;
    /** The hand zh tree word (GT6ZhCn.addTreeBlocks). */
    private final String mZhName;

    GT6TreeKind(String aSnake, String aEnName, String aZhName) {
        mSnake = aSnake;
        mEnName = aEnName;
        mZhName = aZhName;
    }

    public String snake() {
        return mSnake;
    }

    public String enName() {
        return mEnName;
    }

    public String zhName() {
        return mZhName;
    }

    /**
     * The coconut-sapling planting exception (BlockTreeSaplingAB.java:74-76: "Coconut Trees
     * should be able to grow on Sand ... easier to plant them in Deserts and Beaches" —
     * the canSustainPlant(cactus) face = the modern {@code #minecraft:sand} tag face).
     */
    public boolean canGrowOnSand() {
        return this == COCONUT;
    }
}
