package gregtech6.block.surface;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Task debt-issue12-shape-follow-tilt (GitHub #12 residual) — the ONE shared variant
 * table seam for the surface deco band: the datagen face ({@link gregtech6.datagen
 * .GT6BlockStates} addSurfaceBand) emits the blockstate variant list by iterating these
 * enums, and the shape face ({@link GT6SurfaceRockBlock}/{@link GT6SurfaceStickBlock}
 * getShape) picks from the SAME table with the same scan — declaration order ==
 * JSON array order == the WeightedRandom linear-scan order, so the selection box
 * follows the rendered variant bit-for-bit (the C2-era fixed six-state tables always
 * showed the default pose's wireframe under every variant).
 *
 * <p><b>The seed chain, transcribed VERBATIM — do not "fix" any step.</b> This is the
 * renderer's position-seeded draw, quadruple-pinned over vanilla 1.20.1 (draw
 * {@code Math.abs((int)nextLong()) % totalWeight} WeightedBakedModel.java:30, per-face
 * reseed ModelBlockRenderer.java:71-82, position seed BlockRenderDispatcher.java:57 ->
 * Mth.getSeed Mth.java:275-279) / vanilla 1.21.1 (same three lines +0) / Embeddium
 * 0.3.31 (BlockRenderer.java:143-148 reseeds every face with
 * {@code blockState.getSeed(blockPos)}, ChunkBuilderMeshingTask.java:127) / Sodium
 * 0.6.13-neo (BlockRenderer.java:87 {@code randomSeed = state.getSeed(pos)} +
 * AbstractBlockRenderContext.java:114-117 reseed-per-draw): the server replays
 * <pre>
 * RandomSource r = RandomSource.create();      // the java.util.Random LCG family (SingleThreadedRandomSource) — NOT Xoroshiro
 * r.setSeed(state.getSeed(pos));               // the Mth.getSeed(x,y,z) position hash (int-multiply wrap kept as-is)
 * int draw = Math.abs((int) r.nextLong()) % totalWeight;
 * // then WeightedRandom.getWeightedItem(table, draw) — the linear scan, WeightedRandom.java:37-46
 * </pre>
 * Kept-verbatim quirks: {@code Math.abs(Integer.MIN_VALUE)} stays negative, so the draw
 * can go negative with probability ~1/2^32 — the vanilla scan then lands the FIRST
 * entry (first {@code draw -= weight} goes negative) and so does this helper; a future
 * renderer is under NO contract to keep drawing per-position (vanilla re-seeds every
 * face and every getQuads today — Embeddium/Sodium mirror it, but a renderer that
 * caches or reorders the draw will drift from this shape chain; that is the declared
 * compatibility face of this port).
 */
public final class GT6SurfaceVariants {

	private GT6SurfaceVariants() {} // static table holder

	/** One weighted variant row: the datagen face (model + arm) and the shape face (raw box). */
	public interface Variant extends WeightedEntry {

		/** The variant-list weight (JSON weight key, omitted when 1). */
		int weight();

		/** The blockstate variant arm rotation (0 or 90) this entry rides on top of the FACING y-rotation. */
		int armY();

		/** The datagen model id path fragment ({@code gt6:block/<model>}). */
		String model();

		/**
		 * The raw model-space pixel box {x1,y1,z1,x2,y2,z2} — EXACT for the axis-aligned
		 * tiers, the conservative axis-aligned ENVELOPE for the tilt tiers (a tilted
		 * bar's quads are not axis-aligned; VoxelShape cannot express them — declared
		 * deviation, the box overhangs the visual by at most ~1.5px per side).
		 */
		int[] box();

		@Override
		default Weight getWeight() {
			return Weight.of(weight()); // the WeightedRandom scan face (Weight.asInt inside)
		}
	}

	/**
	 * The stick variant pool — 8 entries (the card's 5-8 band), weights all 1 (the
	 * upstream uniform slide, MultiTileEntityStick.java:58-68): the two rotationY arms
	 * of the default centered bar (the upstream 50/50 X-long/Z-long pair), the two
	 * slide tiers each paired with ONE arm (the card's 择优组合 — slide and tilt never
	 * stack, and the slide tiers split across the arms so the pool stays flat instead
	 * of exploding to 3x2x2), and the two tilt models (element rotation about the bar's
	 * own centre, origin 8,1,8) each on both arms — a +22.5 bar on the 90-arm reads as
	 * 112.5, so TWO models cover the four diagonal headings 22.5/45/112.5/135; the
	 * mirror headings (67.5/157.5, the negative angles) are CUT to stay inside the
	 * card's 5-8 entry band — a mirrored diagonal reads as the same tilt to the eye
	 * (the bar itself is 180-degree symmetric, only the heading sign differs).
	 *
	 * <p>The tilt shape boxes are the 12x2 bar rotated 22.5/45 about (8,8) in XZ:
	 * half-extents 6c+1s / 6s+1c, rounded outward — 22.5: X 2.07..13.93 -> 2..14,
	 * Z 4.78..11.22 -> 4..12; 45: 3.05..12.95 -> 3..13 both axes.
	 */
	public enum Stick implements Variant {
		CENTERED_X(1, 0, "surface_stick", 2, 0, 7, 14, 2, 9),
		CENTERED_Z(1, 90, "surface_stick", 2, 0, 7, 14, 2, 9),
		SLIDE_X(1, 0, "surface_stick_a", 2, 0, 5, 14, 2, 7),
		SLIDE_Z(1, 90, "surface_stick_b", 2, 0, 9, 14, 2, 11),
		TILT_22(1, 0, "surface_stick_t22", 2, 0, 4, 14, 2, 12),
		TILT_112(1, 90, "surface_stick_t22", 2, 0, 4, 14, 2, 12),
		TILT_45(1, 0, "surface_stick_t45", 3, 0, 3, 13, 2, 13),
		TILT_135(1, 90, "surface_stick_t45", 3, 0, 3, 13, 2, 13);

		private final int weight, armY;
		private final String model;
		private final int[] box;

		Stick(int aWeight, int aArmY, String aModel, int... aBox) {
			this.weight = aWeight;
			this.armY = aArmY;
			this.model = aModel;
			this.box = aBox;
		}

		@Override public int weight() { return weight; }
		@Override public int armY() { return armY; }
		@Override public String model() { return model; }
		@Override public int[] box() { return box; }
	}

	/**
	 * The rock variant pool — the C2 three size tiers of the upstream 2..8px-wide x
	 * 1..4px-high random micro box (MultiTileEntityRock.java:58-67), weights 3/2/1
	 * (the representative tier heaviest), every tier now its own EXACT shape (the
	 * boxes always sat inside the old 8x3x8 envelope, so the null-pos fallback keeps
	 * the p38-issue1-4 pin).
	 */
	public enum Rock implements Variant {
		REPRESENTATIVE(3, "surface_rock", 4, 0, 4, 12, 3, 12),
		MID(2, "surface_rock_a", 5, 0, 5, 11, 2, 11),
		SMALL(1, "surface_rock_b", 6, 0, 6, 10, 1, 10);

		private final int weight;
		private final String model;
		private final int[] box;

		Rock(int aWeight, String aModel, int... aBox) {
			this.weight = aWeight;
			this.model = aModel;
			this.box = aBox;
		}

		@Override public int weight() { return weight; }
		@Override public int armY() { return 0; } // the rock band has no rotation arms
		@Override public String model() { return model; }
		@Override public int[] box() { return box; }
	}

	/**
	 * The renderer-chain variant pick (the transcription pinned in the class javadoc).
	 * The scan is the REAL vanilla {@link WeightedRandom#getWeightedItem} — the only
	 * hand-copied part is the draw arithmetic, and the orElse first-entry fallback
	 * mirrors the vanilla negative-draw behaviour (see the 1/2^32 quirk note).
	 */
	public static <V extends Variant> V pick(V[] aTable, BlockState aState, BlockPos aPos) {
		RandomSource tRandom = RandomSource.create();
		tRandom.setSeed(aState.getSeed(aPos));
		List<V> tList = List.of(aTable);
		int tDraw = Math.abs((int) tRandom.nextLong()) % WeightedRandom.getTotalWeight(tList);
		return WeightedRandom.getWeightedItem(tList, tDraw).orElse(aTable[0]);
	}

	/**
	 * The FACING x-rotation of the blockstate dispatch — the ONE source both datagen
	 * (addSurfaceBand) and the shape side iterate, so the wireframe cannot drift from
	 * the emitted rotation. The blockstate variant grammar has no rotationZ, so the
	 * walls carry no x-rotation (the floor-lying quirk GTCEu's table shares).
	 */
	public static int xRotOf(Direction aFacing) {
		return switch (aFacing) {
			case UP -> 180;
			case NORTH -> 270;
			case SOUTH -> 90;
			case DOWN, WEST, EAST -> 0;
		};
	}

	/** The FACING y-rotation of the blockstate dispatch (EAST 90 / WEST 270, else 0). */
	public static int yRotOf(Direction aFacing) {
		return switch (aFacing) {
			case EAST -> 90;
			case WEST -> 270;
			default -> 0;
		};
	}

	/**
	 * The raw box {x1,y1,z1,x2,y2,z2} carried through the same rotation the blockstate
	 * dispatch applies. BlockModelRotation composes {@code rotateYXZ(-y, -x, 0)} — the
	 * x-rotation hits the model FIRST, then the y — about the 16-unit block centre,
	 * giving the point maps x90:(x,z,16-y) x180:(x,16-y,16-z) x270:(x,16-z,y)
	 * y90:(16-z,y,x) y270:(z,y,16-x); the x maps anchor to the vanilla end_rod
	 * blockstate (facing=north = "x": 90 tips the up-model's head to -Z — the
	 * BlockModelRotation.java:42 quaternion plus the :49-51 ROT_90_X_NEG group, so JSON
	 * x=90 is the NEGATIVE mathematical rotation), the y maps to the furnace front
	 * (y=90 carries the north-drawn face to +X). NOTE: the r3-stick-shape-random C2
	 * table carried x90/x270 TRANSPOSED against this dispatch (the pin test passed
	 * tautologically); this card's rewrite pins the corrected NORTH/SOUTH boxes.
	 * 90-degree steps keep an AABB exact, so a tilt envelope stays a conservative
	 * envelope.
	 */
	public static int[] rotate(int[] aBox, int aXRot, int aYRot) {
		int[] tBox = aBox;
		if (aXRot != 0) tBox = switch (aXRot) {
			case 90 -> new int[] {tBox[0], tBox[2], 16 - tBox[4], tBox[3], tBox[5], 16 - tBox[1]};
			case 180 -> new int[] {tBox[0], 16 - tBox[4], 16 - tBox[5], tBox[3], 16 - tBox[1], 16 - tBox[2]};
			case 270 -> new int[] {tBox[0], 16 - tBox[5], tBox[1], tBox[3], 16 - tBox[2], tBox[4]};
			default -> tBox;
		};
		if (aYRot != 0) tBox = switch (aYRot) {
			case 90 -> new int[] {16 - tBox[5], tBox[1], tBox[0], 16 - tBox[2], tBox[4], tBox[3]};
			case 180 -> new int[] {16 - tBox[3], tBox[1], 16 - tBox[5], 16 - tBox[0], tBox[4], 16 - tBox[2]};
			case 270 -> new int[] {tBox[2], tBox[1], 16 - tBox[3], tBox[5], tBox[4], 16 - tBox[0]};
			default -> tBox;
		};
		return tBox;
	}

	/** The variant's shape for a facing: the raw box through the dispatch rotations (x from FACING, y = FACING y + arm). */
	public static VoxelShape shapeOf(Variant aVariant, Direction aFacing) {
		int[] tBox = rotate(aVariant.box(), xRotOf(aFacing), (yRotOf(aFacing) + aVariant.armY()) % 360);
		return Block.box(tBox[0], tBox[1], tBox[2], tBox[3], tBox[4], tBox[5]);
	}
}
