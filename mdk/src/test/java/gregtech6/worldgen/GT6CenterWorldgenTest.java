package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.worldgen.GT6CenterFeature.Env;
import gregtech6.worldgen.GT6CenterFeature.RoadMode;
import gregtech6.worldgen.GT6CenterFeature.Sink;

/**
 * The world-origin center showcase acceptance (task worldgen-center-nexus) — the Nexus/
 * Streets/Beacon trio against WorldgenNexus.java:56-338 / WorldgenStreets.java:66-889 /
 * WorldgenBeacon.java:58-116: the chunk-gate verbatim if-translations, the road far-field
 * mode decision, the 512 cadences, the offline SINK REPLAY of all three geometry bodies
 * (every write is an absolute-coordinate attempt — a null state is the offline-
 * unresolvable GT block, the position is still pinned), and the datagen-committed rows.
 *
 * <p>Offline-safe by construction: pure dispatch + sink replays (no registry, no world —
 * the GT6HiveWorldgenTest posture). Chunk (cx,cz) owns blocks [cx*16, cx*16+15].
 */
class GT6CenterWorldgenTest {

	@BeforeAll
	static void boot() {
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@AfterEach
	void resetFlags() {
		GT6CenterFeature.CENTER_BIOMES = false;
		GT6CenterFeature.STREETS = false;
		GT6CenterFeature.NEXUS = false;
		GT6CenterFeature.BEACON = false;
		GT6CenterFeature.TESTING = false;
		GT6CenterFeature.HEIGHT = 66;
	}

	// ---------------------------------------------------------------- the harness

	/** One write attempt — the state may be null (the offline-unresolvable GT block). */
	record Attempt(int x, int y, int z, BlockState state) {}

	static class Recorder implements Sink {
		final List<Attempt> rows = new ArrayList<>();
		@Override public void set(int aX, int aY, int aZ, BlockState aState) {
			rows.add(new Attempt(aX, aY, aZ, aState));
		}
		List<Attempt> at(int aX, int aY, int aZ) {
			return rows.stream().filter(t -> t.x == aX && t.y == aY && t.z == aZ).toList();
		}
		/** The winning-overwrite attempt — the last write at a spot (the portal/shed pass overdraws the sky-clear). */
		Attempt lastAt(int aX, int aY, int aZ) {
			List<Attempt> tHits = at(aX, aY, aZ);
			return tHits.get(tHits.size() - 1);
		}
		long countWhere(java.util.function.Predicate<Attempt> p) {
			return rows.stream().filter(p).count();
		}
	}

	static final class TestEnv implements Env {
		final List<String> signs = new ArrayList<>();
		final List<String> beacons = new ArrayList<>();
		final List<long[]> spawns = new ArrayList<>();
		@Override public boolean opq(int aX, int aY, int aZ) { return false; }
		@Override public String biomeName(int aX, int aZ) { return "test_biome_" + aX + "_" + aZ; }
		@Override public boolean infiniteWater(int aX, int aZ) { return false; }
		@Override public void sign(int aX, int aY, int aZ, int aRotation, String... aLines) {
			signs.add(aX + "," + aY + "," + aZ + ":" + aRotation + ":" + String.join("|", aLines));
		}
		@Override public void beacon(int aX, int aY, int aZ, int aPrimary, int aSecondary) {
			beacons.add(aX + "," + aY + "," + aZ + ":" + aPrimary + "," + aSecondary);
		}
		@Override public void spawn(int aX, int aY, int aZ) { spawns.add(new long[] {aX, aY, aZ}); }
	}

	/** The chunk dispatch with the given flags — the shared replay driver. */
	private static Recorder run(int aCx, int aCz, boolean aStreets, boolean aNexus, boolean aBeacon, RoadMode aFar, TestEnv aEnv) {
		GT6CenterFeature.STREETS = aStreets;
		GT6CenterFeature.NEXUS = aNexus;
		GT6CenterFeature.BEACON = aBeacon;
		Recorder tRows = new Recorder();
		GT6CenterFeature.dispatch(tRows, aEnv, aCx, aCz, (cx, cz, axisX) -> aFar);
		return tRows;
	}

	// ---------------------------------------------------------------- switches + gates

	@Test
	void switchesDefaultFalseAndHeightVerbatim() {
		assertFalse(GT6CenterFeature.CENTER_BIOMES); // :646 default F
		assertFalse(GT6CenterFeature.STREETS); // :647
		assertFalse(GT6CenterFeature.NEXUS); // :648
		assertFalse(GT6CenterFeature.BEACON); // :649
		assertFalse(GT6CenterFeature.TESTING); // :650
		assertEquals(66, GT6CenterFeature.HEIGHT); // WD.waterLevel()+4 — water top 62 in both editions
		assertEquals(64, GT6CenterFeature.TUNNEL_THRESHOLD); // the halved 128-band scan
	}

	@Test
	void fWindowMatchesTheUpstreamLiteral() {
		boolean[] tExpected = {false, true, true, true, true, false, true, true, true, true, false, true, true, true, true, false};
		assertEquals(tExpected.length, GT6CenterFeature.F_WINDOW.length);
		for (int i = 0; i < tExpected.length; i++) assertEquals(tExpected[i], GT6CenterFeature.F_WINDOW[i], "fWindow[" + i + "]");
	}

	@Test
	void chunkGatesMatchTheUpstreamIfTranslations() {
		assertTrue(GT6CenterFeature.isNexusChunk(1, -3)); // :57 aMinX==16&&aMinZ==-48
		for (int cx = -3; cx <= 3; cx++) for (int cz = -5; cz <= 5; cz++) {
			if (cx != 1 || cz != -3) assertFalse(GT6CenterFeature.isNexusChunk(cx, cz), cx + "," + cz);
		}
		// the beacon columns {-1,0} at every Z — the upstream dead-code clause (:60) javadoc'd on the class
		assertTrue(GT6CenterFeature.isBeaconColumn(-1));
		assertTrue(GT6CenterFeature.isBeaconColumn(0));
		assertFalse(GT6CenterFeature.isBeaconColumn(1));
		assertFalse(GT6CenterFeature.isBeaconColumn(-2));
		// the plaza 4x4 {-2..1}²
		for (int cx = -2; cx <= 1; cx++) for (int cz = -2; cz <= 1; cz++) assertTrue(GT6CenterFeature.isPlazaChunk(cx, cz));
		assertFalse(GT6CenterFeature.isPlazaChunk(-3, -1));
		assertFalse(GT6CenterFeature.isPlazaChunk(2, 1));
		// the gap rows/columns (:385 aMinZ!=−32&&aMinZ!=16 / the :412 mirror)
		assertFalse(GT6CenterFeature.roadXApplies(-1, -2));
		assertFalse(GT6CenterFeature.roadXApplies(0, 1));
		assertTrue(GT6CenterFeature.roadXApplies(-1, -5));
		assertTrue(GT6CenterFeature.roadXApplies(0, 9));
		assertFalse(GT6CenterFeature.roadXApplies(-2, -5));
		assertFalse(GT6CenterFeature.roadXApplies(-1, 0)); // the plaza wins
		assertFalse(GT6CenterFeature.roadZApplies(-2, 0));
		assertFalse(GT6CenterFeature.roadZApplies(1, -1));
		assertTrue(GT6CenterFeature.roadZApplies(-8, -1));
		assertTrue(GT6CenterFeature.roadZApplies(9, 0));
		assertFalse(GT6CenterFeature.roadZApplies(-8, -2));
	}

	@Test
	void farRoadDecisionMatchesTheUpstreamCalls() {
		assertEquals(RoadMode.TUNNEL, GT6CenterFeature.farRoadDecision(true, false)); // :371 F,F,T,F,F
		assertEquals(RoadMode.BRIDGE, GT6CenterFeature.farRoadDecision(false, true)); // :381 F,T,F,T,T
		assertEquals(RoadMode.LAND, GT6CenterFeature.farRoadDecision(false, false)); // :383 T,T,F,F,T
	}

	@Test
	void cadencesMatchTheUpstreamShiftMath() {
		assertTrue(GT6CenterStreets.levelMarker(0)); // 0>>9=0, -16>>9=-1
		assertTrue(GT6CenterStreets.levelMarker(512));
		assertTrue(GT6CenterStreets.levelMarker(-512));
		assertFalse(GT6CenterStreets.levelMarker(16));
		assertFalse(GT6CenterStreets.levelMarker(-16));
		assertTrue(GT6CenterStreets.interchange(-16)); // -16>>9=-1, 0>>9=0
		assertFalse(GT6CenterStreets.interchange(0)); // 0>>9=0, 16>>9=0
		assertTrue(GT6CenterStreets.interchange(496));
	}

	// ---------------------------------------------------------------- Nexus replay (chunk (1,-3) = blocks x 16..31, z -48..-33)

	@Test
	void nexusStreetsGeometryReplay() {
		Recorder tRows = run(1, -3, false, true, false, RoadMode.RING, new TestEnv());
		// chunk (1,-3) = x 16..31, z -48..-33 (the upstream aMinZ==-48 gate) — the sky-clear
		// top row k=63 (:61) and the roof row H+13 (:86) cover all 256 columns
		assertEquals(256, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 63
				&& t.x >= 16 && t.x < 32 && t.z >= -48 && t.z < -32));
		// the roof row H+13 (:86) rides INSIDE the k<64 sky-clear band (:61) — every column
		// gets the clear + the roof overwrite = the upstream-faithful 512 double-writes
		assertEquals(512, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 13));
		// the column fill start k=1 (:62/:93) — every column filled from y=1
		assertEquals(256, tRows.countWhere(t -> t.y == 1 && t.x >= 16 && t.x < 32 && t.z >= -48 && t.z < -32));
		// the obsidian interior at H+0 (:84) — 14x14
		assertEquals(196, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT && t.state != null && t.state.is(Blocks.OBSIDIAN)));
		// the window rows H+9..11: 48 of the 60 rim cells carry fWindow (12 plain: 3 per edge) × 3 rows
		// — the stone branch (:104-107) rides the vanilla glass pane
		assertEquals(144, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.GLASS_PANE)));
		// the portal frame ring (:182-206): the legacy meta → FACING mapping (lastAt = the
		// ring pass wins over the interior H+1 underdraw)
		assertEquals(Blocks.GLOWSTONE, tRows.lastAt(17, GT6CenterFeature.HEIGHT + 1, -47).state.getBlock()); // (1,1)
		Attempt tNorth = tRows.lastAt(18, GT6CenterFeature.HEIGHT + 1, -47); // (2,1) meta 0 → SOUTH
		assertEquals(Blocks.END_PORTAL_FRAME, tNorth.state.getBlock());
		assertEquals(Direction.SOUTH, tNorth.state.getValue(EndPortalFrameBlock.FACING));
		assertEquals(Direction.NORTH, tRows.lastAt(18, GT6CenterFeature.HEIGHT + 1, -43).state.getValue(EndPortalFrameBlock.FACING)); // (2,5) meta 2
		assertEquals(Direction.WEST, tRows.lastAt(21, GT6CenterFeature.HEIGHT + 1, -46).state.getValue(EndPortalFrameBlock.FACING)); // (5,2) meta 1
		assertEquals(Direction.EAST, tRows.lastAt(17, GT6CenterFeature.HEIGHT + 1, -46).state.getValue(EndPortalFrameBlock.FACING)); // (1,2) meta 3
		// the shed corners: smooth H+2 / chiseled H+3 / glow H+4 (:302-315) at (1,10) = (17,-38)
		assertEquals(Blocks.SMOOTH_SANDSTONE, tRows.lastAt(17, GT6CenterFeature.HEIGHT + 2, -38).state.getBlock()); // meta 2
		assertEquals(Blocks.CHISELED_SANDSTONE, tRows.lastAt(17, GT6CenterFeature.HEIGHT + 3, -38).state.getBlock()); // meta 1
		assertEquals(Blocks.GLOWSTONE, tRows.lastAt(17, GT6CenterFeature.HEIGHT + 4, -38).state.getBlock());
		// the garden pool 2x2 water (:221-222/:226-227) at x 28..29 + the 12-flower perimeter (:235-246)
		assertEquals(4, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.WATER)
				&& t.y == GT6CenterFeature.HEIGHT + 1 && t.x >= 28 && t.x <= 29));
		assertEquals(12, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.DANDELION) && t.y == GT6CenterFeature.HEIGHT + 2));
		// the south gate cutout at (6, +2, 15) = (22, ·, -33) (:127-144)
		assertTrue(tRows.countWhere(t -> t.x == 22 && t.z == -33 && t.y == GT6CenterFeature.HEIGHT + 2) > 0);
		// the spawn (:336) — (0, HEIGHT+5, 0)
		TestEnv tEnv = new TestEnv();
		run(1, -3, false, true, false, RoadMode.RING, tEnv);
		assertEquals(1, tEnv.spawns.size());
		assertEquals(0L, tEnv.spawns.get(0)[0]);
		assertEquals(GT6CenterFeature.HEIGHT + 5, tEnv.spawns.get(0)[1]);
	}

	@Test
	void nexusStreetsCoupledVariantReplay() {
		// the GENERATE_STREETS coupling (:59) — the same chunk builds the concrete form:
		// the windows ride the light-blue stained glass (the vanilla block resolves offline),
		// the GT concrete/cfoam faces stay the offline nulls (position-only pins)
		Recorder tRows = run(1, -3, true, true, false, RoadMode.RING, new TestEnv());
		assertEquals(144, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.LIGHT_BLUE_STAINED_GLASS)));
		assertEquals(196, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.OBSIDIAN) && t.y == GT6CenterFeature.HEIGHT));
		// the stone-branch rim draw is gone — the rim cells carry the concrete attempts instead
		assertTrue(tRows.at(16, GT6CenterFeature.HEIGHT, -48).size() > 0); // the (0,0) rim corner
	}

	// ---------------------------------------------------------------- Beacon replay

	@Test
	void beaconPyramidReplayAndGate() {
		// the gate runs the beacon columns {-1,0} at EVERY Z (:60 the dead second clause) but the
		// pyramid writes are origin-owned: chunk (-1,-100) attempts, owns nothing, still spawns
		TestEnv tEnvFar = new TestEnv();
		Recorder tRows = run(-1, -100, false, false, true, RoadMode.RING, tEnvFar);
		assertEquals(0, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.IRON_BLOCK)));
		assertEquals(0, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.BEACON)));
		assertEquals(1, tEnvFar.spawns.size()); // :114
		// the diagonal chunk (-1,-1) owns the (-1,-1) beacon + the inner pyramid quarters
		// (the owned z rows shrink per ring: -5..-1 / -4..-1 / -3..-1 / -2..-1)
		TestEnv tEnv = new TestEnv();
		Recorder tRowsDiag = run(-1, -1, false, false, true, RoadMode.RING, tEnv);
		assertEquals(25, tRowsDiag.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 1 && t.state != null && t.state.is(Blocks.IRON_BLOCK))); // x,z∈[-5,-1]
		assertEquals(16, tRowsDiag.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 2 && t.state != null && t.state.is(Blocks.IRON_BLOCK)));
		assertEquals(9, tRowsDiag.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 3 && t.state != null && t.state.is(Blocks.IRON_BLOCK)));
		assertEquals(4, tRowsDiag.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 4 && t.state != null && t.state.is(Blocks.IRON_BLOCK)));
		assertEquals(1, tRowsDiag.countWhere(t -> t.state != null && t.state.is(Blocks.BEACON) && t.y == GT6CenterFeature.HEIGHT + 5));
		// the upstream NBT effect pair (:74-78) — speed/speed on the (-1,-1) beacon
		assertEquals(List.of("-1," + (GT6CenterFeature.HEIGHT + 5) + ",-1:" + GT6CenterFeature.MOVEMENT_SPEED + "," + GT6CenterFeature.MOVEMENT_SPEED), tEnv.beacons);
		// the (0,0) chunk carries the lone mixed pair — resistance/regeneration (:107-110)
		TestEnv tEnv00 = new TestEnv();
		Recorder tRows00 = run(0, 0, false, false, true, RoadMode.RING, tEnv00);
		assertEquals(1, tRows00.countWhere(t -> t.state != null && t.state.is(Blocks.BEACON)));
		assertEquals(List.of("0," + (GT6CenterFeature.HEIGHT + 5) + ",0:" + GT6CenterFeature.DAMAGE_RESISTANCE + "," + GT6CenterFeature.REGENERATION), tEnv00.beacons);
		// the (0,-1) chunk owns the (0,-1) beacon — strength/strength (:91-99)
		TestEnv tEnv0m1 = new TestEnv();
		run(0, -1, false, false, true, RoadMode.RING, tEnv0m1);
		assertEquals(List.of("0," + (GT6CenterFeature.HEIGHT + 5) + ",-1:" + GT6CenterFeature.DAMAGE_BOOST + "," + GT6CenterFeature.DAMAGE_BOOST), tEnv0m1.beacons);
		// the STREETS suppression (:61) — no beacon when the streets variant owns the plaza
		Recorder tSuppressed = run(0, 3, true, false, true, RoadMode.RING, new TestEnv());
		assertEquals(0, tSuppressed.countWhere(t -> t.state != null && t.state.is(Blocks.BEACON)));
		assertEquals(0, tSuppressed.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 1 && t.state != null && t.state.is(Blocks.IRON_BLOCK)));
	}

	// ---------------------------------------------------------------- Streets replay

	@Test
	void ringRoadXProfileReplay() {
		// chunk (-1, 2): the X-road ring row — RING mode (bridge, no kill sky, side walls)
		Recorder tRows = run(-1, 2, true, false, false, RoadMode.RING, new TestEnv());
		// the asphalt lanes band -12..-2 at H (the BLACK_CONCRETE mapping), along z = 32
		for (int band = -12; band <= -2; band++) {
			assertTrue(tRows.at(band, GT6CenterFeature.HEIGHT, 32).size() > 0, "lane " + band);
		}
		// the median -1 concrete at H-1/H/H+1 (the non-land face)
		assertTrue(tRows.at(-1, GT6CenterFeature.HEIGHT - 1, 32).size() > 0);
		assertTrue(tRows.at(-1, GT6CenterFeature.HEIGHT + 1, 32).size() > 0);
		// the rails at -11, -7, -3 owned here; 2/6/10 belong to chunk 0
		assertTrue(tRows.at(-11, GT6CenterFeature.HEIGHT + 1, 32).size() > 0);
		assertTrue(tRows.at(-7, GT6CenterFeature.HEIGHT + 1, 32).size() > 0);
		// no far kill-sky in RING mode: nothing above H+7
		assertEquals(0, tRows.countWhere(t -> t.y > GT6CenterFeature.HEIGHT + 7 && t.state != null));
		// the bridge beams ride -13/+12 (:438-443) — band -13 owned here, +12 in chunk 0
		assertTrue(tRows.at(-13, GT6CenterFeature.HEIGHT, 32).size() > 0);
		assertTrue(tRows.at(-13, GT6CenterFeature.HEIGHT + 1, 32).size() > 0);
		// the undercarriage (:592-647): the deck underside at H-2/H-3 (along 38..41 × bands -9..-6)
		assertTrue(tRows.at(-7, GT6CenterFeature.HEIGHT - 2, 39).size() > 0);
		assertTrue(tRows.at(-6, GT6CenterFeature.HEIGHT - 3, 40).size() > 0);
		// the west pillar shaft (bands -8/-7 × along 39/40) descends H-4..1 — the all-false opq env never grounds it
		assertEquals(4 * (GT6CenterFeature.HEIGHT - 4), tRows.countWhere(t -> (t.x == -7 || t.x == -8)
				&& (t.z == 39 || t.z == 40) && t.y >= 1 && t.y <= GT6CenterFeature.HEIGHT - 4));
		// no land shoulders in RING mode
		assertEquals(0, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.GRAVEL) && t.y < GT6CenterFeature.HEIGHT - 1));
		// chunk (0, 2) owns the 0..15 band
		Recorder tRows0 = run(0, 2, true, false, false, RoadMode.RING, new TestEnv());
		assertTrue(tRows0.at(11, GT6CenterFeature.HEIGHT, 32).size() > 0);
		assertTrue(tRows0.at(10, GT6CenterFeature.HEIGHT + 1, 32).size() > 0);
		assertTrue(tRows0.at(-11, GT6CenterFeature.HEIGHT + 1, 32).isEmpty()); // band -11 is not this chunk's
		// the +12 beam lands here, +13 does NOT (the upstream beams are -13/+12, not ±13)
		assertTrue(tRows0.at(12, GT6CenterFeature.HEIGHT, 32).size() > 0);
		assertTrue(tRows0.at(13, GT6CenterFeature.HEIGHT, 32).isEmpty());
		// the east pillar shaft (bands 6/7 × along 39/40)
		assertEquals(4 * (GT6CenterFeature.HEIGHT - 4), tRows0.countWhere(t -> (t.x == 6 || t.x == 7)
				&& (t.z == 39 || t.z == 40) && t.y >= 1 && t.y <= GT6CenterFeature.HEIGHT - 4));
	}

	@Test
	void farRoadKillSkyAsymmetryVerbatim() {
		// the X-road far field: k < 64 (:445)
		Recorder tRowsX = run(-1, -8, true, false, false, RoadMode.LAND, new TestEnv());
		assertTrue(tRowsX.countWhere(t -> t.x == -12 && t.z == -128 && t.y == GT6CenterFeature.HEIGHT + 63) > 0);
		assertTrue(tRowsX.countWhere(t -> t.x == -12 && t.z == -128 && t.y == GT6CenterFeature.HEIGHT + 64) == 0);
		// the Z-road far field: k < 32 (:682) — the asymmetry pin
		Recorder tRowsZ = run(-8, -1, true, false, false, RoadMode.LAND, new TestEnv());
		assertTrue(tRowsZ.countWhere(t -> t.x == -128 && t.z == -11 && t.y == GT6CenterFeature.HEIGHT + 31) > 0);
		assertTrue(tRowsZ.countWhere(t -> t.x == -128 && t.z == -11 && t.y == GT6CenterFeature.HEIGHT + 32) == 0);
		// the land mode fills foundations down to y=1 (the all-air env never stops the scan)
		assertTrue(tRowsX.countWhere(t -> t.x == -16 && t.z == -128 && t.y == 1) > 0); // the -16 shoulder
		assertTrue(tRowsX.countWhere(t -> t.x == -12 && t.z == -128 && t.y == 1) > 0); // the roadbed fill
	}

	@Test
	void tunnelAndLevelMarkerReplay() {
		// the Z-road at base cx=-512 (levelMarker(-512): -512>>9=-1, -528>>9=-2 → true), band owner cz=-1
		Recorder tRows = run(-512, -1, true, false, false, RoadMode.TUNNEL, new TestEnv());
		// the tunnel walls at band -13 (chunk cz=-1 owns band -16..-1), along x = -8192
		assertTrue(tRows.at(-8192, GT6CenterFeature.HEIGHT, -13).size() > 0);
		assertTrue(tRows.at(-8192, GT6CenterFeature.HEIGHT + 6, -13).size() > 0);
		// the ceiling band -12..-1 at H+7 (:430)
		assertTrue(tRows.at(-8191, GT6CenterFeature.HEIGHT + 7, -12).size() > 0);
		// the level-marker panel at band -13 (:530-548), fill = blue for base<0 Z-road (DYE 4)
		assertTrue(tRows.countWhere(t -> t.z == -13 && t.x == -8192 + 6 && t.y == GT6CenterFeature.HEIGHT + 3) > 0);
		// the coordinate signs at band -12, offsets 7/8 (:772-775) — lines ["", "X: -2/-1", "Z: -1", ""]
		TestEnv tEnv = new TestEnv();
		run(-512, -1, true, false, false, RoadMode.TUNNEL, tEnv);
		assertTrue(tEnv.signs.stream().anyMatch(s -> s.startsWith("-8185," + (GT6CenterFeature.HEIGHT + 3) + ",-12:")));
		assertTrue(tEnv.signs.stream().anyMatch(s -> s.contains("|X: -2|Z: -1|")));
		assertTrue(tEnv.signs.stream().anyMatch(s -> s.contains("|X: -1|Z: -1|")));
	}

	@Test
	void plazaReplayOwnedColumnsAndBeacon() {
		GT6CenterFeature.STREETS = true;
		GT6CenterFeature.BEACON = true;
		Recorder tRows = new Recorder();
		TestEnv tEnv = new TestEnv();
		GT6CenterFeature.dispatch(tRows, tEnv, -1, -1, (cx, cz, ax) -> RoadMode.RING);
		// the sky clear over the own 16x16 plaza columns (:70) — the k∈[2,63] band, 62 rows each
		assertEquals(256 * 62, tRows.countWhere(t -> t.y >= GT6CenterFeature.HEIGHT + 2 && t.y <= GT6CenterFeature.HEIGHT + 63
				&& t.x >= -16 && t.x < 0 && t.z >= -16 && t.z < 0));
		// the inner roads at (x=-13, z=-16): neither inside(-12,11) → the asphalt strip (:95)
		assertTrue(tRows.at(-13, GT6CenterFeature.HEIGHT, -16).size() > 0);
		// the pavilion field at (-5,-5): inside both → concrete floor + cfoam/concrete at H+1 (:91-93)
		assertTrue(tRows.at(-5, GT6CenterFeature.HEIGHT, -5).size() > 0);
		assertTrue(tRows.at(-5, GT6CenterFeature.HEIGHT + 1, -5).size() > 0);
		// the under-plaza beacon: 25 of the 100-cell top ring + the 1 owned beacon of 4
		assertEquals(25, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT - 3 && t.state != null
				&& t.state.is(Blocks.IRON_BLOCK) && t.x >= -5 && t.x <= -1 && t.z >= -5 && t.z <= -1));
		assertEquals(1, tRows.countWhere(t -> t.state != null && t.state.is(Blocks.BEACON)));
		// the plaza beacon carries the speed/speed NBT pair (:313-320)
		assertEquals(List.of("-1," + (GT6CenterFeature.HEIGHT + 1) + ",-1:" + GT6CenterFeature.MOVEMENT_SPEED + "," + GT6CenterFeature.MOVEMENT_SPEED), tEnv.beacons);
		// the sign pair (x=-1,z=-30) lives in chunk (-1,-2), (x=-30,·) in (-2,·) → none owned here
		assertEquals(0, tEnv.signs.size());
		// the spawn (:358)
		assertEquals(1, tEnv.spawns.size());
	}

	@Test
	void plazaSignOwnershipSplitsAcrossChunks() {
		GT6CenterFeature.STREETS = true;
		// the west sign pair x=-30, z=0 lives in chunk (-2, 0)
		TestEnv tEnv = new TestEnv();
		GT6CenterFeature.dispatch(new Recorder(), tEnv, -2, 0, (cx, cz, ax) -> RoadMode.RING);
		assertEquals(2, tEnv.signs.size()); // the H+3 + H+2 pair
		assertTrue(tEnv.signs.get(0).endsWith(":test_biome_-4096_95|test_biome_-3584_95|test_biome_-3072_95|test_biome_-2560_95"));
		// the east sign pair x=29 lives in chunk (1, 0)
		TestEnv tEnv2 = new TestEnv();
		GT6CenterFeature.dispatch(new Recorder(), tEnv2, 1, 0, (cx, cz, ax) -> RoadMode.RING);
		assertEquals(2, tEnv2.signs.size());
	}

	@Test
	void dispatchReturnsTrueOnlyOnOwnedChunks() {
		Recorder tRows = new Recorder();
		GT6CenterFeature.STREETS = true;
		assertTrue(GT6CenterFeature.dispatch(tRows, new TestEnv(), -1, 2, (cx, cz, ax) -> RoadMode.RING)); // the X-road
		assertTrue(GT6CenterFeature.dispatch(tRows, new TestEnv(), -8, -1, (cx, cz, ax) -> RoadMode.RING)); // the Z-road
		// (1,-3) with the nexus flag off: no road, no plaza, no beacon column → nothing
		GT6CenterFeature.NEXUS = false;
		Recorder tRows2 = new Recorder();
		assertFalse(GT6CenterFeature.dispatch(tRows2, new TestEnv(), 1, -3, (cx, cz, ax) -> RoadMode.RING));
		// all off → nothing generates
		GT6CenterFeature.STREETS = false;
		assertFalse(GT6CenterFeature.dispatch(tRows2, new TestEnv(), -1, 2, (cx, cz, ax) -> RoadMode.RING));
		assertEquals(0, tRows2.rows.size());
	}

	@Test
	void determinismTwoReplaysIdentical() {
		GT6CenterFeature.STREETS = true;
		GT6CenterFeature.NEXUS = true;
		assertEquals(replay(1, -3), replay(1, -3));
		assertEquals(replay(-1, -1), replay(-1, -1));
	}

	private static Set<String> replay(int aCx, int aCz) {
		Recorder tRows = new Recorder();
		GT6CenterFeature.dispatch(tRows, new TestEnv(), aCx, aCz, (cx, cz, ax) -> RoadMode.RING);
		Set<String> rOut = new HashSet<>();
		for (Attempt tAttempt : tRows.rows) {
			rOut.add(tAttempt.x + "," + tAttempt.y + "," + tAttempt.z + ","
					+ (tAttempt.state == null ? "null" : tAttempt.state.toString()));
		}
		return rOut;
	}

	// ---------------------------------------------------------------- Testing shell (task worldgen-center-testing, WorldgenTesting.java)

	@Test
	void testingGateMatchesTheUpstreamIfTranslation() {
		// :67 (aMinX!=32&&aMinX!=48)||(aMinZ!=-32&&aMinZ!=-48) — the 4-chunk box, columns {2,3} x rows {-3,-2}
		// (aMinX/aMinZ are chunk-min BLOCK coords: 32/48 = cx 2/3, -48/-32 = cz -3/-2)
		assertTrue(GT6CenterFeature.isTestingChunk(2, -3));
		assertTrue(GT6CenterFeature.isTestingChunk(2, -2));
		assertTrue(GT6CenterFeature.isTestingChunk(3, -3));
		assertTrue(GT6CenterFeature.isTestingChunk(3, -2));
		for (int cx = -4; cx <= 4; cx++) for (int cz = -5; cz <= 5; cz++) {
			boolean tExpected = (cx == 2 || cx == 3) && (cz == -3 || cz == -2);
			assertEquals(tExpected, GT6CenterFeature.isTestingChunk(cx, cz), cx + "," + cz);
		}
		// the box never overlaps the trio: nexus (1,-3), plaza {-2..1}^2, roads on the {-1,0} bands
		assertFalse(GT6CenterFeature.isTestingChunk(1, -3));
		assertFalse(GT6CenterFeature.isTestingChunk(2, -1));
		assertFalse(GT6CenterFeature.isTestingChunk(0, -2));
	}

	@Test
	void testingShellGeometryReplay() {
		GT6CenterFeature.TESTING = true;
		// chunk (2,-3) — the west+north edge chunk, blocks x 32..47, z -48..-33
		Recorder tRows = new Recorder();
		TestEnv tEnv = new TestEnv();
		assertTrue(GT6CenterFeature.dispatch(tRows, tEnv, 2, -3, (cx, cz, ax) -> RoadMode.RING));
		// the solid pedestal k=1..HEIGHT (:70) — gray concrete, the offline-unresolvable GT block = null attempts
		assertEquals(256, tRows.countWhere(t -> t.y == 1 && t.x >= 32 && t.x < 48 && t.z >= -48 && t.z < -32));
		assertEquals(256, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT));
		// the gray CFoam floor at H+1 (:73) — every column
		assertEquals(256, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 1));
		// the sky clear H+2..255 (:71) — the 1.7.10 world cap carries verbatim; nothing above it
		assertEquals(256, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 63));
		assertEquals(256, tRows.countWhere(t -> t.y == 255));
		assertEquals(0, tRows.countWhere(t -> t.y > 255));
		// the edge walls (:74-88): west i=0 + north j=0 share the corner column → 31 columns x 14 rows
		assertEquals(31 * 14, tRows.countWhere(t -> t.y >= GT6CenterFeature.HEIGHT + 2 && t.y <= GT6CenterFeature.HEIGHT + 15
				&& ((t.x == 32 && t.z >= -48 && t.z < -32) || (t.z == -48 && t.x >= 32 && t.x < 48))));
		// the ceiling (:89-93): 225 interior columns — the glow-glass field (i,j outside {1,5,10,14}) = 11x11 = 121,
		// the cfoam-slab cross = 104 (the offline nulls; the glow glass rides the vanilla stained glass)
		assertEquals(121, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 15
				&& t.state != null && t.state.is(Blocks.LIGHT_BLUE_STAINED_GLASS)));
		assertEquals(104, tRows.countWhere(t -> t.y == GT6CenterFeature.HEIGHT + 15
				&& t.x >= 33 && t.x < 48 && t.z >= -47 && t.z < -32 && t.state == null));
		// the slab cross hits the {1,5,10,14} lanes (x 33/37/42/46 = i, z -47/-43/-38/-34 = j)
		assertTrue(tRows.at(33, GT6CenterFeature.HEIGHT + 15, -43).size() > 0);
		// the spawn (:374) — (0, HEIGHT+5, 0) from every box chunk
		assertEquals(1, tEnv.spawns.size());
		assertEquals(0L, tEnv.spawns.get(0)[0]);
		assertEquals(GT6CenterFeature.HEIGHT + 5, tEnv.spawns.get(0)[1]);
		// the switch-off gate: the same chunk stays silent
		GT6CenterFeature.TESTING = false;
		Recorder tOff = new Recorder();
		assertFalse(GT6CenterFeature.dispatch(tOff, new TestEnv(), 2, -3, (cx, cz, ax) -> RoadMode.RING));
		assertEquals(0, tOff.rows.size());
	}

	@Test
	void testingShellEdgePatternIsChunkRelative() {
		GT6CenterFeature.TESTING = true;
		// chunk (3,-2) — the east+south edge chunk (x 48..63, z -32..-17): the mirrored edges
		Recorder tRows = new Recorder();
		GT6CenterFeature.dispatch(tRows, new TestEnv(), 3, -2, (cx, cz, ax) -> RoadMode.RING);
		assertEquals(31 * 14, tRows.countWhere(t -> t.y >= GT6CenterFeature.HEIGHT + 2 && t.y <= GT6CenterFeature.HEIGHT + 15
				&& ((t.x == 63 && t.z >= -32 && t.z < -16) || (t.z == -17 && t.x >= 48 && t.x < 64))));
		// no door in the (3,-2) chunk — the west wall x=48 keeps its full wall run at H+2..H+5
		assertEquals(14, tRows.countWhere(t -> t.x == 48 && t.z == -25
				&& t.y >= GT6CenterFeature.HEIGHT + 2 && t.y <= GT6CenterFeature.HEIGHT + 15));
	}

	@Test
	void testingDoorwayReplay() {
		GT6CenterFeature.TESTING = true;
		// :96-131 live in the (aMinX==32 && aMinZ==-32) chunk = (2,-2) — x 32..47, z -32..-17
		Recorder tRows = new Recorder();
		GT6CenterFeature.dispatch(tRows, new TestEnv(), 2, -2, (cx, cz, ax) -> RoadMode.RING);
		// the 4-wide 3-high opening at x=32, j=6..9 (z -26..-23), H+2..H+4 — the door carve wins over the wall run
		for (int z = -26; z <= -23; z++) for (int y = GT6CenterFeature.HEIGHT + 2; y <= GT6CenterFeature.HEIGHT + 4; y++) {
			Attempt tLast = tRows.lastAt(32, y, z);
			assertTrue(tLast.state != null && tLast.state.isAir(), "opening " + y + "," + z);
		}
		// the H+2 gray jamb pair (:97/:102) + the interior pilasters (:103-104) — null cfoam attempts at the spots
		assertTrue(tRows.at(32, GT6CenterFeature.HEIGHT + 2, -27).size() > 0);
		assertTrue(tRows.at(32, GT6CenterFeature.HEIGHT + 2, -22).size() > 0);
		assertTrue(tRows.at(33, GT6CenterFeature.HEIGHT + 2, -26).size() > 0);
		assertTrue(tRows.at(33, GT6CenterFeature.HEIGHT + 2, -23).size() > 0);
		// the H+3 yellow band (:106-113)
		assertTrue(tRows.at(32, GT6CenterFeature.HEIGHT + 3, -27).size() > 0);
		assertTrue(tRows.at(33, GT6CenterFeature.HEIGHT + 3, -26).size() > 0);
		for (int z = -26; z <= -23; z++) assertTrue(tRows.lastAt(32, GT6CenterFeature.HEIGHT + 3, z).state.isAir());
		// the H+4 gray band with the full pilaster run (:115-124)
		assertTrue(tRows.at(32, GT6CenterFeature.HEIGHT + 4, -27).size() > 0);
		for (int z = -26; z <= -23; z++) assertTrue(tRows.at(33, GT6CenterFeature.HEIGHT + 4, z).size() > 0);
		// the H+5 gray lintel row (:126-131)
		for (int z = -27; z <= -22; z++) assertTrue(tRows.at(32, GT6CenterFeature.HEIGHT + 5, z).size() > 0);
		// the wall above the door keeps the LightBlue rows H+6.. (the edge wall pass is not re-cut above the lintel)
		assertTrue(tRows.at(32, GT6CenterFeature.HEIGHT + 6, -25).size() > 0);
	}

	// ---------------------------------------------------------------- datagen rows (the RED pin)

	@Test
	void centerDatagenRowsAreCommitted() throws Exception {
		Path tRoot = mdkRoot().resolve("src/generated/resources/data/gt6");
		assertTrue(Files.exists(tRoot.resolve("worldgen/configured_feature/center.json")), "configured_feature/center.json missing");
		assertTrue(Files.exists(tRoot.resolve("worldgen/placed_feature/center.json")), "placed_feature/center.json missing");
		// the biome modifiers reference the placed key, both loader brands, at the TOP_LAYER_MODIFICATION step
		String tForgeModifier = Files.readString(tRoot.resolve("forge/biome_modifier/center.json"));
		assertTrue(tForgeModifier.contains("gt6:center"), "the forge biome modifier must place gt6:center");
		assertTrue(tForgeModifier.contains("top_layer_modification"), "the center row rides the last decoration pass");
		String tNeoModifier = Files.readString(tRoot.resolve("neoforge/biome_modifier/center.json"));
		assertTrue(tNeoModifier.contains("gt6:center"), "the neo biome modifier must place gt6:center");
		assertTrue(tNeoModifier.contains("top_layer_modification"), "the center row rides the last decoration pass");
	}

	/** The mdk project root, walking up from the (leg-dependent) test working dir (the datagen-test precedent). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from " + Path.of("").toAbsolutePath());
	}
}
