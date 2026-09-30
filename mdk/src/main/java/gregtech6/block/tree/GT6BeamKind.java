package gregtech6.block.tree;

/**
 * One GT6 wood-beam row (task beam-blocks-register): the 8 vanilla-subset beams of the
 * upstream LIST_BEAMS walk — Beam1 meta 0-3 (LoaderWoodDictionary.java:51-54) and Beam2
 * meta 0-3 (:55-56 Acacia/DarkOak, :66 the DEFAULT_BEAM = IL.Beam "Wood" face, :175 the
 * Rubber Wood face). The per-pair granularity is the P8 ADR ④ / GT6TreeKind precedent:
 * upstream is per-meta ids over 2 blocks x 16 meta (BlockBaseBeam.java:47
 * {@code Math.min(4, aMaxMeta)}, 4 wood x 4 orientations), which a single EnumProperty
 * block cannot express — so one block per wood, the AXIS property carries the orientation
 * (BlockBaseBeam.java:64 onBlockPlaced → PILLAR_DATA_SIDE, the pillar/axis semantic).
 *
 * <p>{@code snake} is the registry id segment: {@code <snake>_beam} (the GT6TreeKind
 * {@code <kind>_log} single-source naming rule). {@code enName} is the upstream LH row
 * verbatim (BlockTreeBeam1.java:31-48 / BlockTreeBeam2.java:31-48), {@code zhName} the
 * 1.7.10 zh dump face verbatim (tmp/gregtech.lang gt.block.beam.1.0-.3 / 2.0-.3 — the
 * per-orientation dump metas all repeat the four base words).
 */
public enum GT6BeamKind {
    OAK("oak", "Oak Beam", "橡木梁"),
    SPRUCE("spruce", "Spruce Beam", "云杉木梁"),
    BIRCH("birch", "Birch Beam", "白桦木梁"),
    JUNGLE("jungle", "Jungle Beam", "丛林木梁"),
    ACACIA("acacia", "Acacia Beam", "金合欢木梁"),
    DARK_OAK("dark_oak", "Dark Oak Beam", "深色橡木梁"),
    RUBBER_WOOD("rubber_wood", "Rubber Wood Beam", "橡胶木梁"),
    WOOD("wood", "Wood Beam", "木梁");

    /** The registry id segment ({@code <snake>_beam}). */
    private final String mSnake;
    /** The upstream display name (BlockTreeBeam1.java:31-48 / BlockTreeBeam2.java:31-48 LH rows). */
    private final String mEnName;
    /** The 1.7.10 zh dump face (gt.block.beam.1.0-.3 / 2.0-.3). */
    private final String mZhName;

    GT6BeamKind(String aSnake, String aEnName, String aZhName) {
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
}
