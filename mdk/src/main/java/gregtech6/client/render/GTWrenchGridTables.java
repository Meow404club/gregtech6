package gregtech6.client.render;

import gregtech6.util.UT6;

/**
 * The wrench 3x3 grid tables (task p5-wrench-ui-gtceu, ADR 2026-08-30-p5-wrench-ui
 * ruling ⑤ "the table IS the specification"): the hover grid is a pure visual layer
 * over {@link UT6#getSideWrenching} — the very function the click path uses
 * (GTFluidPipeBlock.use:114) — so what the grid draws is constructively what a click
 * picks. Geometry is exported as 1/16-grid constants for the renderer; the side
 * mapping itself lives here so it stays offline-testable.
 *
 * <p>Grid semantics (GTCEu BlockHighlightRenderer:246-272 layout, but with the
 * GTCEu RelativeDirection placement chain replaced by the in-face axis tables —
 * world-axis identical to the click pick): centre cell = the clicked face, the four
 * edge cells = the four perpendicular faces, the four corner cells = the opposite
 * face. The pick thresholds are the 0.25/0.75 edges of {@link UT6#getSideWrenching},
 * so the 3x3 grid is NOT thirds — the corner cells occupy exactly [0,0.25] bands and
 * the edge cells sit centred inside the [0.25,0.75] pick band, which is why every
 * point inside a cell's icon quad maps back to that cell's side (test ②).
 *
 * <p>Pure static functions over bytes/floats — zero Minecraft imports, the whole
 * surface is offline-testable. The renderer and listener are the only consumers.
 */
public final class GTWrenchGridTables {

	/**
	 * The grid icons (GTCEu GTGuiTextures.java:699-718 TOOL_* counterparts). The path
	 * is relative to {@code assets/gt6/} and stays a String here to keep this class
	 * MC-free; the renderer wraps it into a ResourceLocation for the direct
	 * {@code RenderType.text} bind (no atlas — GT6Atlases untouched).
	 */
	public enum GTWrenchGridIcon {
		/** Green wrench icon — the face is connected (borrowed tool_pipe_connect.png). */
		PIPE_CONNECT("textures/gui/overlay/tool_pipe_connect.png"),
		/** Red wrench icon — the face is not connected / the mode bit is unset. */
		PIPE_BLOCK("textures/gui/overlay/tool_pipe_block.png"),
		/** Arrow icon — the face carries the output-arrow ioMask bit (shift mode). */
		IO_FACING_ROTATION("textures/gui/overlay/tool_io_facing_rotation.png"),
		/** Rotation icon — the cell rotates the machine's front facing (shift mode, the oven). */
		FRONT_FACING_ROTATION("textures/gui/overlay/tool_front_facing_rotation.png");

		/** Texture path relative to {@code assets/gt6/}. */
		public final String texturePath;

		GTWrenchGridIcon(String aTexturePath) {
			texturePath = aTexturePath;
		}
	}

	/** Icon edge length in 1/16 grid units (GTCEu drawOverlayTextureWithMargin:308-313 — the 4x4 cell). */
	public static final int CELL_16 = 4;

	/** Icon inset in 1/16 grid units (GTCEu MARGIN :244/:312 — 0.2 of the 4x4 cell). */
	public static final float MARGIN_16 = 0.2F;

	/**
	 * Cell origin per column/row index 0..2 in 1/16 grid units: corner cells at 0
	 * (the [0,0.25] pick band), edge cells at 6 (centred in the [0.25,0.75] pick
	 * band), and the matching far corner at 12. Index by col/row, same for both axes.
	 */
	public static final int[] CELL_ORIGIN_16 = { 0, 6, 12 };

	private GTWrenchGridTables() {
	}

	// ---------------------------------------------------------------------------
	// in-face axis tables — {0,1}→(X,Z) / {2,3}→(X,Y) / {4,5}→(Z,Y)
	// (GT6 side order == Direction.get3DDataValue: DOWN UP NORTH SOUTH WEST EAST;
	// axis ids 0=X, 1=Y, 2=Z)
	// ---------------------------------------------------------------------------

	/** The first (column) in-face axis of aFace — axis id {0:X, 1:Y, 2:Z}. */
	public static byte cellUAxis(byte aFace) {
		switch (aFace) {
		case 0: case 1: return 0; // DOWN/UP  → (X, Z)
		case 2: case 3: return 0; // N/S      → (X, Y)
		case 4: case 5: return 2; // WEST/EAST→ (Z, Y)
		}
		throw new IllegalArgumentException("face " + aFace + " is not a valid side 0..5");
	}

	/** The second (row) in-face axis of aFace — axis id {0:X, 1:Y, 2:Z}. */
	public static byte cellVAxis(byte aFace) {
		switch (aFace) {
		case 0: case 1: return 2;
		case 2: case 3: return 1;
		case 4: case 5: return 1;
		}
		throw new IllegalArgumentException("face " + aFace + " is not a valid side 0..5");
	}

	/** The cell centre on an in-face axis: (2*col+1)/6 → 1/6, 3/6, 5/6. */
	public static float cellCenter(int aCol) {
		return (2 * aCol + 1) / 6.0F;
	}

	/**
	 * The side a grid cell stands for: the cell centre fed through
	 * {@link UT6#getSideWrenching} (the click-pick function itself — centre hits the
	 * face, edges hit the axis neighbours, corners fall back onto OPOS).
	 */
	public static byte cellSide(byte aFace, int aCol, int aRow) {
		float[] tHit = assembleHit(aFace, cellCenter(aCol), cellCenter(aRow));
		return UT6.getSideWrenching(aFace, tHit[0], tHit[1], tHit[2]);
	}

	/**
	 * The side under a hover hit: the single UT6 entry point of the renderer, fed the
	 * same 0..1 hit offsets the click path uses (BlockHitResult location minus the
	 * BlockPos — GTFluidPipeBlock.use:111-113). Same function, so the highlighted
	 * cell is always exactly the cell a click would act on.
	 */
	public static byte hoverSide(byte aFace, float aHitX, float aHitY, float aHitZ) {
		return UT6.getSideWrenching(aFace, aHitX, aHitY, aHitZ);
	}

	/** The hover grid shows every face; the icon follows the active mode's bit. */
	public static GTWrenchGridIcon iconFor(boolean aShift, byte aSide, byte aConnections, byte aIoMask) {
		int tBit = 1 << aSide;
		if (aShift) {
			// shift mode shows the ioMask bits as arrows; unmarked faces stay red
			// (the bit itself is displayable without a connection — the connected gate
			// of isOutputFace is runtime push semantics, not display semantics)
			return (aIoMask & tBit) != 0 ? GTWrenchGridIcon.IO_FACING_ROTATION : GTWrenchGridIcon.PIPE_BLOCK;
		}
		return (aConnections & tBit) != 0 ? GTWrenchGridIcon.PIPE_CONNECT : GTWrenchGridIcon.PIPE_BLOCK;
	}

	/**
	 * The machine-rotation icon table (task p6-oven-rotation, the second table of the
	 * grid — ADR 2026-08-30-p6-oven-rotation ruling ④): the oven counterpart of
	 * {@link #iconFor}. Without shift the machine grid draws no icons at all (GTCEu
	 * MetaMachine sideTips :683-688 — the arrows are the shift mode only; the grid
	 * lines still render via shouldRenderGrid :670). With shift, a cell is live when
	 * the side it stands for is horizontal (2..5, the HORIZONTAL_FACING domain — the
	 * RotationState.HORIZONTAL :777 counterpart) and not the machine's own front (the
	 * isFacingValid :770-771 front exclusion); the front's own side, the vertical
	 * sides (down/up plus the corner cells that fall back onto vertical OPOS entries)
	 * and SIDE_INVALID all stay null. Note the corner cells of a horizontal face stand
	 * for the opposite horizontal side ({@code OPOS[face]}), so they rotate too — the
	 * table IS the specification, same ruling as the pipe table.
	 */
	public static GTWrenchGridIcon ovenCellIcon(boolean aShift, byte aCellSide, byte aFrontFacing) {
		if (!aShift) return null;
		return aCellSide >= 2 && aCellSide <= 5 && aCellSide != aFrontFacing
				? GTWrenchGridIcon.FRONT_FACING_ROTATION : null;
	}

	/** Assemble a UT6 hit vector from in-face (u,v): the normal axis stays 0.5. */
	private static float[] assembleHit(byte aFace, float aU, float aV) {
		float[] tHit = { 0.5F, 0.5F, 0.5F };
		tHit[cellUAxis(aFace)] = aU;
		tHit[cellVAxis(aFace)] = aV;
		return tHit;
	}
}
