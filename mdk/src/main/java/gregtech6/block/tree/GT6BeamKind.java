package gregtech6.block.tree;

/**
 * One GT6 wood-beam row (task beam-blocks-register + beam-fireproof-closeout): the FULL
 * upstream beam block set — Beam1 meta 0-3 (LoaderWoodDictionary.java:51-54) and Beam2
 * meta 0-3 (:55-56 Acacia/DarkOak, :66 the DEFAULT_BEAM = IL.Beam "Wood" face, :175 the
 * Rubber Wood face), plus (task beam-fireproof-closeout) the residual families Beam3
 * meta 0-3 (Loader_Woods.java:60 — the Greatwood/Silverwood/Skyroot/Darkwood mod-wood
 * faces, BlockTreeBeam3.java:31-48; the port seats them GT6-native on the Beam1/2
 * registration precedent — the upstream block is GT6's own compat content, textures
 * shipped in the GT6 jar) and BeamA meta 0-3 / BeamB meta 0-3 / BeamC meta 0
 * (Loader_Woods.java:48/:50/:52 — the GT-tree 0-8 faces, BlockTreeBeamA/B/C.java:31-48;
 * gated on the GT6TreeKind port face, which IS landed — w6-t1-trees-nine). The per-pair
 * granularity is the P8 ADR ④ / GT6TreeKind precedent: upstream is per-meta ids over
 * {@code Math.min(4, aMaxMeta)} (BlockBaseBeam.java:47, 4 wood x 4 orientations), which
 * a single EnumProperty block cannot express — so one block per wood, the AXIS property
 * carries the orientation (BlockBaseBeam.java:64 onBlockPlaced → PILLAR_DATA_SIDE, the
 * pillar/axis semantic). BeamC is the single-meta family (BlockTreeBeamC.java:35
 * max-meta 1 — Blue Spruce only).
 *
 * <p>{@code snake} is the registry id segment: {@code <snake>_beam} (the GT6TreeKind
 * {@code <kind>_log} single-source naming rule). {@code enName} is the upstream LH row
 * verbatim (BlockTreeBeam1/2/3/A/B/C.java:31-48), {@code zhName} the 1.7.10 zh dump face
 * verbatim (tmp/gregtech.lang gt.block.beam.1/2/3/a/b/c — the per-orientation dump metas
 * all repeat the base words per block).
 *
 * <p>{@code family} carries the 6 upstream block families with their Loader_Recipes_Woods
 * laminator citation lines ({@code :53-58} the plate x6 leg, {@code :66-71} the foil x24
 * leg) — the single source the laminator beam rows' per-row citations ride.
 */
public enum GT6BeamKind {
    OAK("oak", "Oak Beam", "橡木梁", BeamFamily.BEAM1),
    SPRUCE("spruce", "Spruce Beam", "云杉木梁", BeamFamily.BEAM1),
    BIRCH("birch", "Birch Beam", "白桦木梁", BeamFamily.BEAM1),
    JUNGLE("jungle", "Jungle Beam", "丛林木梁", BeamFamily.BEAM1),
    ACACIA("acacia", "Acacia Beam", "金合欢木梁", BeamFamily.BEAM2),
    DARK_OAK("dark_oak", "Dark Oak Beam", "深色橡木梁", BeamFamily.BEAM2),
    RUBBER_WOOD("rubber_wood", "Rubber Wood Beam", "橡胶木梁", BeamFamily.BEAM2),
    WOOD("wood", "Wood Beam", "木梁", BeamFamily.BEAM2),
    GREATWOOD("greatwood", "Greatwood Beam", "宏伟之木梁", BeamFamily.BEAM3),
    SILVERWOOD("silverwood", "Silverwood Beam", "银树梁", BeamFamily.BEAM3),
    SKYROOT("skyroot", "Skyroot Beam", "天根木梁", BeamFamily.BEAM3),
    DARKWOOD("darkwood", "Darkwood Beam", "黑树梁", BeamFamily.BEAM3),
    RUBBER("rubber", "Rubber Beam", "橡胶梁", BeamFamily.BEAM_A),
    MAPLE("maple", "Maple Beam", "枫树梁", BeamFamily.BEAM_A),
    WILLOW("willow", "Willow Beam", "柳树梁", BeamFamily.BEAM_A),
    BLUE_MAHOE("blue_mahoe", "Blue Mahoe Beam", "高红槿梁", BeamFamily.BEAM_A),
    HAZEL("hazel", "Hazel Beam", "榛树梁", BeamFamily.BEAM_B),
    CINNAMON("cinnamon", "Cinnamon Beam", "肉桂梁", BeamFamily.BEAM_B),
    COCONUT("coconut", "Coconut Beam", "椰子树木梁", BeamFamily.BEAM_B),
    RAINBOWOOD("rainbowood", "Rainbowood Beam", "彩虹树梁", BeamFamily.BEAM_B),
    BLUE_SPRUCE("blue_spruce", "Blue Spruce Beam", "北美云杉梁", BeamFamily.BEAM_C);

    /** The 6 upstream beam block families (Loader_Woods.java:48-61) with the laminator fire-proofing citation lines. */
    public enum BeamFamily {
        BEAM1("53", "66"), // Loader_Recipes_Woods.java:53 plate / :66 foil
        BEAM2("54", "67"), // :54 / :67
        BEAM3("55", "68"), // :55 / :68
        BEAM_A("56", "69"), // :56 / :69
        BEAM_B("57", "70"), // :57 / :70
        BEAM_C("58", "71"); // :58 / :71

        private final String mPlateLine;
        private final String mFoilLine;

        BeamFamily(String aPlateLine, String aFoilLine) {
            mPlateLine = aPlateLine;
            mFoilLine = aFoilLine;
        }

        /** The RM.Laminator plate-x6 leg line (Loader_Recipes_Woods.java:53-58). */
        public String plateLine() {
            return mPlateLine;
        }

        /** The RM.Laminator foil-x24 leg line (Loader_Recipes_Woods.java:66-71). */
        public String foilLine() {
            return mFoilLine;
        }
    }

    /** The registry id segment ({@code <snake>_beam}). */
    private final String mSnake;
    /** The upstream display name (BlockTreeBeam1/2/3/A/B/C.java:31-48 LH rows). */
    private final String mEnName;
    /** The 1.7.10 zh dump face (gt.block.beam.1/2/3/a/b/c). */
    private final String mZhName;
    /** The upstream block family (the laminator citation line source). */
    private final BeamFamily mFamily;

    GT6BeamKind(String aSnake, String aEnName, String aZhName, BeamFamily aFamily) {
        mSnake = aSnake;
        mEnName = aEnName;
        mZhName = aZhName;
        mFamily = aFamily;
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

    /** The upstream block family (Loader_Woods.java:48-61). */
    public BeamFamily family() {
        return mFamily;
    }
}
