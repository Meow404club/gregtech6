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

	/**
	 * The variant's display template key (task p20-i18n-compose-rows, the B-wave lang ruling):
	 * {@code gt6.stone.variant.<snake>} — ONE position-param template per variant (the stone
	 * name rides the {@code %s} slot as the {@code gt6.material.<snake>} small unit), replacing
	 * the 272 pre-installed per-(stone, variant) full strings the old {@code compose} fed. The
	 * en/zh template VALUES live in the lang providers; zh wording follows the dump's
	 * {@code gt.stone.andesite.N} family split (tmp/gregtech.lang:15246-15262).
	 */
	public String key() {
		return "gt6.stone.variant." + this.snake;
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

}
