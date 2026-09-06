package gregtech6.block.stone;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

import gregtech6.registry.GTStoneBlocks;

/**
 * The 16 stone variants of one GT6 stone family (task p19-stoneblocks-registry) — the
 * upstream 16-meta universe of the single-Block BlockStones family, re-expressed as an
 * {@link EnumProperty} value set. Declared order IS the upstream meta order: the enum
 * ordinal is the 1.7.10 metadata value (BlockStones.java:71-77 — STONE=0, COBBL=1, MCOBL=2,
 * BRICK=3, CRACK=4, MBRIK=5, CHISL=6, SMOTH=7, RNFBR=8, RSTBR=9, TILES=10, STILE=11,
 * SBRIK=12, WINDA=13, WINDB=14, QBRIK=15). Never reorder: the four mapping tables
 * ({@link GTStoneBlocks#CHISEL_MAPPINGS} family, BlockStones.java:81-84) are indexed by
 * this order verbatim.
 *
 * <p>The serialized name is the upstream texture folder segment lower-cased
 * (BlockStones.java:93-108 icon names STONE/COBBLE/COBBLE_MOSSY/BRICKS/.../SQUARE_BRICKS
 * — {@link #iconSegment()} recovers the exact PNG path segment for the render card's
 * asset borrow, p19-stoneblocks-render). The lang suffix composition is the upstream
 * LH.add block verbatim (BlockStones.java:117-132, {@code getUnlocalizedName()+".N"} keys
 * split into {@code block.gt6.<stone>.<variant>} keys).
 */
public enum StoneVariant implements StringRepresentable {

	/** meta 0 — the plain stone (upstream icon STONE, lang ".0" = the bare material name). */
	STONE("stone"),
	/** meta 1 — COBBLE. */
	COBBL("cobble"),
	/** meta 2 — COBBLE_MOSSY. */
	MCOBL("cobble_mossy"),
	/** meta 3 — BRICKS. */
	BRICK("bricks"),
	/** meta 4 — BRICKS_CRACKED. */
	CRACK("bricks_cracked"),
	/** meta 5 — BRICKS_MOSSY. */
	MBRIK("bricks_mossy"),
	/** meta 6 — BRICKS_CHISELED, the stoneChiseled carrier (OP.stoneChiseled, OP.java:402). */
	CHISL("bricks_chiseled"),
	/** meta 7 — SMOOTH. */
	SMOTH("smooth"),
	/** meta 8 — BRICKS_REINFORCED. */
	RNFBR("bricks_reinforced"),
	/** meta 9 — BRICKS_REDSTONE. */
	RSTBR("bricks_redstone"),
	/** meta 10 — TILES. */
	TILES("tiles"),
	/** meta 11 — SMALL_TILES. */
	STILE("small_tiles"),
	/** meta 12 — SMALL_BRICKS. */
	SBRIK("small_bricks"),
	/** meta 13 — WINDMILL_TILES_A. */
	WINDA("windmill_tiles_a"),
	/** meta 14 — WINDMILL_TILES_B. */
	WINDB("windmill_tiles_b"),
	/** meta 15 — SQUARE_BRICKS. */
	QBRIK("square_bricks");

	/** The declaration-order array (values() clone — callers must not reorder it). */
	public static final StoneVariant[] VALUES = values();

	/** The serialized/blockstate/texture-segment name (lower-cased upstream icon name). */
	public final String snake;

	StoneVariant(String aSnake) {
		this.snake = aSnake;
	}

	/** The 1.7.10 metadata value this variant corresponds to (the declaration index). */
	public byte meta() {
		return (byte) ordinal();
	}

	@Override
	public String getSerializedName() {
		return this.snake;
	}

	/** The upstream texture folder segment (BlockStones.java:93-108 icon names) — "cobble_mossy" -> "COBBLE_MOSSY". */
	public String iconSegment() {
		return this.snake.toUpperCase(Locale.ROOT);
	}

	/**
	 * The English display name for a stone whose material localises as {@code aStoneName}
	 * — the upstream LH.add table (BlockStones.java:117-132) with
	 * {@code aDefaultLocalised = aMaterial.getLocal()} (BlockStonesGT.java:38) substituted.
	 */
	public String compose(String aStoneName) {
		return switch (this.ordinal()) {
		case 0  -> aStoneName;                                  // ".0"
		case 1  -> aStoneName + " Cobblestone";                 // ".1"
		case 2  -> "Mossy " + aStoneName + " Cobblestone";      // ".2"
		case 3  -> aStoneName + " Bricks";                      // ".3"
		case 4  -> "Cracked " + aStoneName + " Bricks";         // ".4"
		case 5  -> "Mossy " + aStoneName + " Bricks";           // ".5"
		case 6  -> "Chiseled " + aStoneName;                    // ".6"
		case 7  -> "Smooth " + aStoneName;                      // ".7"
		case 8  -> "Reinforced " + aStoneName + " Bricks";      // ".8"
		case 9  -> "Redstoned " + aStoneName + " Bricks";       // ".9"
		case 10 -> aStoneName + " Tiles";                       // ".10"
		case 11 -> "Small " + aStoneName + " Tiles";            // ".11"
		case 12 -> "Small " + aStoneName + " Bricks";           // ".12"
		case 13 -> aStoneName + " Windmill Tiles A";            // ".13"
		case 14 -> aStoneName + " Windmill Tiles B";            // ".14"
		default -> aStoneName + " Square Bricks";               // ".15"
		};
	}
}
