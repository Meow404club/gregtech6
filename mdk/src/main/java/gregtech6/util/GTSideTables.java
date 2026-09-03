/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.util;

/**
 * The shared side-algebra lookup tables (task p14-machine-fluid-face ①) — the CS.java
 * translations every side-masked machine face reads. Upstream keeps both tables in
 * {@code CS} (gregapi/data/CS.java); the root gregapi port carries no tables yet and the
 * per-class private copies (MultiBlockFluidHandler.java:64 FACING_ROTATIONS,
 * GTGearBoxBlockEntity.faceConnected bit test) predate this shared home — new consumers
 * read from HERE, the private tables stay untouched (zero-diff discipline).
 *
 * <p>Side bytes are the GT6 order == the vanilla {@code Direction.get3DDataValue()}:
 * 0 = bottom/down, 1 = top/up, 2..5 = n/s/w/e, 6 = undefined.
 */
public final class GTSideTables {

	private GTSideTables() {/**/}

	/** The CS.java T/F literals (CS.java:35). */
	private static final boolean F = false, T = true;

	/**
	 * CS.java:528-537 verbatim — {@code [Facing, Side] -> Side} mappings for blocks, which
	 * don't face up- and downwards. The row is the world facing, the value the
	 * machine-relative side: 0 = bottom, 1 = top, 2 = left, 3 = front, 4 = right,
	 * 5 = back, 6 = undefined. Rows 0/1 (a vertical facing) keep the identity and every
	 * row maps bottom→bottom and top→top.
	 */
	public static final byte[][] FACING_ROTATIONS = {
		{0,1,2,3,4,5,6,6},
		{0,1,2,3,4,5,6,6},
		{0,1,3,5,4,2,6,6},
		{0,1,5,3,2,4,6,6},
		{0,1,2,4,3,5,6,6},
		{0,1,4,2,5,3,6,6},
		{0,1,2,3,4,5,6,6},
		{0,1,2,3,4,5,6,6}
	};

	/**
	 * CS.java:598-606 verbatim — insert a machine-relative Side and a Connectivity BitMask
	 * to see if it is connecting to that Side. Technically this is a simple Bit Operation
	 * ({@code FACE_CONNECTED[aSide][aMask] == (aMask & (1 << aSide)) != 0}), but accessing
	 * the table looks nicer (upstream CS.java:597 javadoc) and it keeps the 6 (undefined)
	 * row — bit 64+ — reachable for masks that set it (the all-sides default 127 has bit 6
	 * set, so a side-less byte still reads {@code true}).
	 */
	public static final boolean[][] FACE_CONNECTED = {
		{F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T},
		{F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T},
		{F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T},
		{F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T},
		{F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T},
		{F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T},
		{F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T}
	};

	/**
	 * The rotated connected-face verdict — the exact lookup shape of the upstream
	 * side gates (MultiTileEntityBasicMachine.java:511/:566/:575):
	 * {@code FACE_CONNECTED[FACING_ROTATIONS[aFacing][aWorldSide]][aMask]}.
	 * Both operands are masked into the table domain (&amp;7 / &amp;127, the
	 * MultiBlockFluidHandler.relativeSide masking precedent), so out-of-range
	 * bytes fold onto the 6/7 undefined entries instead of throwing.
	 */
	public static boolean faceConnected(byte aFacing, byte aWorldSide, byte aMask) {
		return FACE_CONNECTED[FACING_ROTATIONS[aFacing & 7][aWorldSide & 7]][aMask & 127];
	}
}
