package gregtech6.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Seeded-RNG pins for the pure crop engine (ADR-CB5). Every expected value is a fixed pin computed
 * by the code under test against {@link RandomSource#create(long)} (LegacyRandomSource: first draws
 * are constant for small seeds — hence seed 11258 where a varying first draw is needed), then
 * hand-checked against the dual-source anchors (ADR-CB1):
 *
 * <ul>
 * <li>MECH = decompiled IC2 1.12 line tmp/harvest/ic2-crops/reference/ic2-source/decompiled/ic2/core/crop/TileEntityCrop.java
 *     (line numbers cited per test) — mechanism authority.</li>
 * <li>API/GT = gregapi/old/GT_BaseCrop.java (1.7.10 compile-face) + ic2/api/crops/CropCard.java defaults
 *     + CropWeed.java — the card surface GT6 actually binds.</li>
 * <li>WIKI = GTNH wiki IC2 Crops page (1.7.10 line): base=3+X(0..6)+Gr, needed=4(T-1)+Gr+Ga+Re,
 *     value=5×(H+N+A), surplus/deficit scaling, Re 31 immune, 256t, weed at Gr 24+, weed-EX cap/decay —
 *     all verified verbatim against MECH.</li>
 * </ul>
 */
public class CropMathTest extends GTOfflineTestBase {

	static final GT6CropCard A = new GT6CropCard("Alpha", "Tester", new ItemStack(Items.WHEAT), null, null,
			1, 5, 2, 3, 5, 6, 7, 8, 9, new String[] {"Foo", "Bar"});
	static final GT6CropCard B = new GT6CropCard("Beta", "Tester", new ItemStack(Items.CARROT), null, null,
			2, 4, 1, 2, 9, 8, 7, 6, 5, new String[] {"bar", "Baz"});
	static final GT6CropCard C = new GT6CropCard("Gamma", "Tester", new ItemStack(Items.POTATO), null, null,
			1, 3, 1, 2, 0, 0, 0, 0, 0, new String[] {"foo"});
	static final GT6CropCard T11 = new GT6CropCard("T11", "Tester", new ItemStack(Items.WHEAT), null, null,
			11, 4, 1, 2, 0, 0, 0, 0, 0, new String[0]);

	// ------------------------------------------------------------------ fixture view

	/** Mutable in-memory CropTileView — the contract cbc-1's BE implements. */
	static final class V implements CropTileView {
		GT6CropCard crop;
		boolean crossingBase;
		int size = 1, g, ga, re, points, scan, water, nutrients, weedEx;
		int hum, nut, air;
		final CropTileView[] neighbors = new CropTileView[4];
		final boolean[] soilAir = new boolean[4];
		int clearCalls;
		final List<String> seedCalls = new java.util.ArrayList<>();

		static V of(GT6CropCard pCrop, int pSize, int pG, int pGa, int pRe) {
			V v = new V();
			v.crop = pCrop;
			v.size = pSize;
			v.g = pG;
			v.ga = pGa;
			v.re = pRe;
			return v;
		}

		@Override public GT6CropCard crop() { return crop; }
		@Override public boolean crossingBase() { return crossingBase; }
		@Override public int size() { return size; }
		@Override public int statGrowth() { return g; }
		@Override public int statGain() { return ga; }
		@Override public int statResistance() { return re; }
		@Override public int growthPoints() { return points; }
		@Override public int scanLevel() { return scan; }
		@Override public int storageWater() { return water; }
		@Override public int storageNutrients() { return nutrients; }
		@Override public int storageWeedEx() { return weedEx; }
		@Override public int terrainHumidity() { return hum; }
		@Override public int terrainNutrients() { return nut; }
		@Override public int terrainAirQuality() { return air; }
		@Override public CropTileView neighbor(int aIndex) { return neighbors[aIndex]; }
		@Override public boolean soilAirAt(int aIndex) { return soilAir[aIndex]; }
		@Override public ItemStack generateSeeds(GT6CropCard aCard, int aG, int aGa, int aRe, int aScan) {
			seedCalls.add(aCard.name() + ":G" + aG + ",Ga" + aGa + ",Re" + aRe + ",s" + aScan);
			return new ItemStack(Items.WHEAT_SEEDS);
		}
		@Override public void setCrop(GT6CropCard aCrop) { crop = aCrop; }
		@Override public void setCrossingBase(boolean aCrossingBase) { crossingBase = aCrossingBase; }
		@Override public void setSize(int aSize) { size = aSize; }
		@Override public void setStats(int aG, int aGa, int aRe) { g = aG; ga = aGa; re = aRe; }
		@Override public void setGrowthPoints(int aPoints) { points = aPoints; }
		@Override public void setScanLevel(int aScan) { scan = aScan; }
		@Override public void setStorages(int aWater, int aNutrients, int aWeedEx) { water = aWater; nutrients = aNutrients; weedEx = aWeedEx; }
		@Override public void clear() { // port of reset() MECH:826-839 — storages and crossingBase survive
			crop = null; g = 0; ga = 0; re = 0; points = 0; scan = 0; size = 1;
			hum = -1; nut = -1; air = -1; clearCalls++;
		}
	}

	// ------------------------------------------------------------------ card (GT_BaseCrop port form)

	@Test
	public void cardConstructorClampsPortGtBaseCrop() {
		// name lowercased de-spaced — GT_BaseCrop.java:59
		assertEquals("alphaprime", new GT6CropCard("Alpha Prime", null, new ItemStack(Items.WHEAT), null, null,
				1, 4, 1, 2, 0, 0, 0, 0, 0, new String[0]).name());
		// default discoverer — GT_BaseCrop.java:41/:60
		assertEquals("Gregorius Techneticies", new GT6CropCard("X", null, new ItemStack(Items.WHEAT), null, null,
				1, 4, 1, 2, 0, 0, 0, 0, 0, new String[0]).discoveredBy());
		// tier forced >=1, maxSize forced >=3 — :65-66
		GT6CropCard clamped = new GT6CropCard("Y", "T", new ItemStack(Items.WHEAT), null, null,
				0, 2, 9, 0, 0, 0, 0, 0, 0, new String[0]);
		assertEquals(1, clamped.tier());
		assertEquals(3, clamped.maxSize());
		// harvestSize clamp 2..maxSize :68, afterHarvestSize clamp 1..maxSize-1 :69
		assertEquals(2, clamped.harvestSize());
		assertEquals(2, clamped.sizeAfterHarvest());
		// stat out of range → 0 :118-121
		assertEquals(0, clamped.stat(-1));
		assertEquals(0, clamped.stat(5));
		assertEquals(0, clamped.stat(GT6CropCard.STAT_COLOR)); // all-zero ctor args
		// the five stats land in declared order — GT_BaseCrop.java:70-74
		GT6CropCard stats = new GT6CropCard("Z", "T", new ItemStack(Items.WHEAT), null, null,
				1, 4, 1, 2, 1, 2, 3, 4, 5, new String[0]);
		assertEquals(1, stats.stat(GT6CropCard.STAT_CHEMICAL));
		assertEquals(2, stats.stat(GT6CropCard.STAT_FOOD));
		assertEquals(3, stats.stat(GT6CropCard.STAT_DEFENSIVE));
		assertEquals(4, stats.stat(GT6CropCard.STAT_COLOR));
		assertEquals(5, stats.stat(GT6CropCard.STAT_WEED));
		// canGrow/canBeHarvested/canCross — :103-105/:108-110/:113-115
		V v = V.of(A, 4, 0, 0, 0);
		assertTrue(A.canGrow(v));
		assertTrue(A.canBeHarvested(v)); // size 4 >= harvestSize 3
		assertTrue(A.canCross(v)); // 4+2 > 5
		assertFalse(A.canCross(V.of(A, 3, 0, 0, 0))); // 3+2 > 5 is false — ADR-CB1 API anchor :113-115
		// weed card — CropWeed.java: tier 0, maxSize 5, attrs, no harvest/pick, duration 300
		assertEquals(0, GT6Crops.WEED.tier());
		assertEquals(5, GT6Crops.WEED.maxSize());
		assertEquals(300, GT6Crops.WEED.growthDuration());
		assertFalse(GT6Crops.WEED.canBeHarvested(V.of(GT6Crops.WEED, 5, 0, 0, 0)));
		assertFalse(GT6Crops.WEED.leftClickPicksSeed());
		assertEquals(1, GT6Crops.WEED.stat(GT6CropCard.STAT_DEFENSIVE));
		assertEquals(5, GT6Crops.WEED.stat(GT6CropCard.STAT_WEED));
		// growthDuration default tier*200 — CropCard.java:72-74 (GTNH: growth length T×200)
		assertEquals(200, A.growthDuration());
		assertEquals(400, B.growthDuration());
		// dropSeedChance — CropCard.java:121-136: 0.5, halved at size 2, then ×0.8 per tier (all sizes)
		assertEquals(0.0F, A.dropSeedChance(V.of(A, 1, 0, 0, 0)), 0.0F);
		assertEquals(0.25F * 0.8F, A.dropSeedChance(V.of(A, 2, 0, 0, 0)), 0.0F); // halved at size 2, ×0.8^1
		assertEquals(0.5F * 0.8F, A.dropSeedChance(V.of(A, 3, 0, 0, 0)), 0.0F);
		assertEquals(0.25F * 0.8F * 0.8F, B.dropSeedChance(V.of(B, 2, 0, 0, 0)), 1e-6F); // tier 2 → 0.16
		// dropGainChance 0.95^tier — CropCard.java:100-102
		assertEquals(Math.pow(0.95, 1), A.dropGainChance(), 0.0);
		// isWeed — CropCard.java:167-169: size>=2 && (weed card || Gr>=24); WIKI: Gr 24+, stage 2+
		assertFalse(A.isWeed(V.of(A, 2, 23, 0, 0)));
		assertTrue(A.isWeed(V.of(A, 2, 24, 0, 0)));
		assertFalse(A.isWeed(V.of(A, 1, 31, 0, 0)));
		assertTrue(GT6Crops.WEED.isWeed(V.of(GT6Crops.WEED, 2, 0, 0, 0)));
		// specialDrops gain pick — GT_BaseCrop.java:139-145 with injected RNG: len+4 range, <len → special
		GT6CropCard sp = new GT6CropCard("Sp", "T", new ItemStack(Items.WHEAT),
				new ItemStack[] {new ItemStack(Items.CARROT), new ItemStack(Items.POTATO)}, null,
				1, 3, 1, 2, 0, 0, 0, 0, 0, new String[0]);
		// seed 42's first draw nextInt(6) = 2 ≥ len(2) → falls through to the plain wheat drop;
		// the special-slot path is pinned end-to-end by the specialDrops harvest case below
		ItemStack gain = sp.pickGain(V.of(sp, 1, 0, 0, 0), RandomSource.create(42));
		assertEquals(Items.WHEAT, gain.getItem());
	}

	// ------------------------------------------------------------------ growth (MECH:285-318)

	@Test
	public void growthSurplusDeficitDeathPins() {
		// WIKI: base=3+X(0..6)+Gr, needed=4(T-1)+Gr+Ga+Re, value=5×(H+N+A) — verbatim MECH:292-295
		// seed 42: surplus — tier1 G0, terrain (10,10,10): base 3+n7(1)=4, needed 0, provided 150
		// → 4×250/100 = 10
		V v = V.of(A, 1, 0, 0, 0);
		v.hum = 10; v.nut = 10; v.air = 10;
		CropMath.performGrowthTick(A, v, RandomSource.create(42));
		assertEquals(10, v.growthPoints());
		// seed 42: mild deficit — tier5 G0 terrain 0: needed 16, provided 0, aux 64 ≤ 100
		// → 4×(100-64)/100 = 1 (integer division)
		GT6CropCard t5 = new GT6CropCard("T5", "Tester", new ItemStack(Items.WHEAT), null, null,
				5, 4, 1, 2, 0, 0, 0, 0, 0, new String[0]);
		v = V.of(t5, 1, 0, 0, 0);
		CropMath.performGrowthTick(t5, v, RandomSource.create(42));
		assertEquals(1, v.growthPoints());
		// seed 42: death — tier11 G0Re0 terrain 0: needed 40, aux 160 > 100, n32 = 1 > Re 0
		// → reset (MECH:300-302) — WIKI: "may die", Re 31 completely immune
		v = V.of(T11, 1, 0, 0, 0);
		CropMath.performGrowthTick(T11, v, RandomSource.create(42));
		assertNull(v.crop());
		assertEquals(1, v.clearCalls);
		assertEquals(1, v.size());
		assertEquals(0, v.growthPoints());
		// Re 31 immune — 50 seeded cycles, never dies (MECH:300 nextInt(32) ≤ 31 is never > 31)
		v = V.of(T11, 1, 0, 0, 31);
		RandomSource rng = RandomSource.create(7);
		for (int i = 0; i < 50; i++) CropMath.performGrowthTick(T11, v, rng);
		assertEquals(T11, v.crop());
		assertEquals(0, v.growthPoints()); // survive branch: 4×(100-160)/100 floors to 0 every cycle
		// Re 1 survives the same draw (n32 = 1 is not > 1) and the negative total floors at 0 — MECH:304-305
		v = V.of(T11, 1, 0, 0, 1);
		CropMath.performGrowthTick(T11, v, RandomSource.create(42));
		assertEquals(T11, v.crop());
		assertEquals(0, v.growthPoints());
	}

	// ------------------------------------------------------------------ crossing gate (MECH:1140-1153)

	@Test
	public void crossingGatePins() {
		// base 4, +1 Gr>=16, +1 Gr>=30, +(27-Re) when Re>=28 — WIKI/research: gate bonuses
		assertEquals(4, CropMath.crossingGate(V.of(A, 1, 0, 0, 0)));
		assertEquals(5, CropMath.crossingGate(V.of(A, 1, 16, 0, 0)));
		assertEquals(6, CropMath.crossingGate(V.of(A, 1, 30, 0, 0)));
		assertEquals(6, CropMath.crossingGate(V.of(A, 1, 31, 0, 0)));
		assertEquals(3, CropMath.crossingGate(V.of(A, 1, 0, 0, 28))); // 4 + 27-28
		assertEquals(0, CropMath.crossingGate(V.of(A, 1, 0, 0, 31))); // 4 + 27-31
		assertEquals(2, CropMath.crossingGate(V.of(A, 1, 31, 0, 31)));
		assertEquals(4, CropMath.crossingGate(V.of(A, 1, 15, 0, 0)));
		assertEquals(5, CropMath.crossingGate(V.of(A, 1, 29, 0, 0)));
		assertEquals(4, CropMath.crossingGate(V.of(A, 1, 0, 0, 27)));
	}

	// ------------------------------------------------------------------ weight table (MECH:1098-1131)

	@Test
	public void crossingWeightPins() {
		// same crop 500 — :1099-1101
		assertEquals(500, CropMath.crossingWeight(A, A));
		// A vs B: Σ(2-|statdiff|) = -2; +5 shared attribute (Bar/bar, case-insensitive :1115);
		// tier diff new-old = -1 → no penalty → 3
		assertEquals(3, CropMath.crossingWeight(A, B));
		assertEquals(3, CropMath.crossingWeight(B, A));
		// A vs C: Σ = -25, +5 Foo/foo = -20 → clamp 0 — :1130 max(value, 0)
		assertEquals(0, CropMath.crossingWeight(A, C));
		assertEquals(0, CropMath.crossingWeight(C, A));
		// weed vs A: Σ = -19, no shared attrs → 0
		assertEquals(0, CropMath.crossingWeight(GT6Crops.WEED, A));
		// tier penalty, diff > 1 → -2×diff :1122-1124 — Hi(t8) vs C(t1): 10 - 14 → 0
		GT6CropCard hi = new GT6CropCard("Hi", "Tester", new ItemStack(Items.WHEAT), null, null,
				8, 3, 1, 2, 0, 0, 0, 0, 0, new String[0]);
		assertEquals(0, CropMath.crossingWeight(hi, C));
		// diff < -3 → -=(-diff) :1126-1128 — C(t1) vs Hi(t8): 10 - 7 = 3
		assertEquals(3, CropMath.crossingWeight(C, hi));
	}

	// ------------------------------------------------------------------ crossing (MECH:933-1044, :1133-1157)

	@Test
	public void crossingAttemptPins() {
		// 1/3 chance per cycle — :934 nextInt(3) != 0 → miss; WIKI/CropsNH guide: "1-in-3 each tick".
		// seed 42 first draw nextInt(3) = 2 → miss, tile untouched
		V center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 30, 8, 2);
		center.neighbors[1] = V.of(B, 3, 30, 4, 6);
		assertFalse(CropMath.attemptCrossing(center, List.of(GT6Crops.WEED, A, B), RandomSource.create(42)));
		assertNull(center.crop());
		assertTrue(center.crossingBase);

		// full pin, seed 1: draws n3=0 (hit); slot0 gate 6 ≥ n16=1 ✓; slot1 gate 6 ≥ n16=6 ✓;
		// weights over candidates [WEED, A, B]: WEED 0+0, A 500+3, B 3+500 → cumulative [0, 503, 1006];
		// pick roll lands on B; stats avg(30,30)/avg(8,4)/avg(2,6) + nextInt(5)-2 perturbation,
		// clamped 0..31 (Util.limit :1037-1039) → G31 Ga8 Re6, size 1, base cleared
		boolean ok = CropMath.attemptCrossing(center, List.of(GT6Crops.WEED, A, B), RandomSource.create(1));
		assertTrue(ok);
		assertSame(B, center.crop());
		assertEquals(1, center.size());
		assertEquals(31, center.statGrowth());
		assertEquals(8, center.statGain());
		assertEquals(6, center.statResistance());
		assertFalse(center.crossingBase);

		// clamp 31 — neighbors G31 Ga31 Re20, seed 1: avg 31/31/20 + 2 → 33→31, 33→31, 22
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 31, 31, 20);
		center.neighbors[1] = V.of(B, 3, 31, 31, 20);
		assertTrue(CropMath.attemptCrossing(center, List.of(A), RandomSource.create(1)));
		assertEquals(31, center.statGrowth());
		assertEquals(31, center.statGain());
		assertEquals(22, center.statResistance());

		// clamp 0 — zero-stat neighbors, seed 12: perturbation drags Ga/Re below 0 → floored
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 0, 0, 0);
		center.neighbors[1] = V.of(B, 3, 0, 0, 0);
		assertTrue(CropMath.attemptCrossing(center, List.of(A), RandomSource.create(12)));
		assertEquals(2, center.statGrowth());
		assertEquals(0, center.statGain());
		assertEquals(0, center.statResistance());

		// gate miss via Re 31 (gate 4+27-31 = 0 < every rand ≥ 0 with this draw) — :1149-1153
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 0, 0, 31);
		center.neighbors[1] = V.of(B, 3, 0, 0, 31);
		assertFalse(CropMath.attemptCrossing(center, List.of(A, B), RandomSource.create(1)));
		assertNull(center.crop());
		assertTrue(center.crossingBase);

		// fewer than 2 qualified neighbors — :953
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 30, 8, 2);
		assertFalse(CropMath.attemptCrossing(center, List.of(A, B), RandomSource.create(1)));
		assertNull(center.crop());

		// empty candidate registry — declared deviation: MECH:986 nextInt(0) would throw, we return false
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 30, 8, 2);
		center.neighbors[1] = V.of(B, 3, 30, 4, 6);
		assertFalse(CropMath.attemptCrossing(center, List.of(), RandomSource.create(1)));
	}

	// ------------------------------------------------------------------ spreading (MECH:1046-1096)

	@Test
	public void spreadingPins() {
		// exactly one neighbor, gate 4 (G12<16, Re3<28) ≥ n16 — seed 11258 (small seeds draw a
		// constant 11 first and always fail this gate; documented LegacyRandomSource quirk)
		V center = new V();
		center.crossingBase = true;
		center.neighbors[2] = V.of(A, 4, 12, 9, 3);
		assertTrue(CropMath.attemptSpreading(center, RandomSource.create(11258)));
		assertSame(A, center.crop());
		assertEquals(1, center.size());
		assertEquals(12, center.statGrowth());
		assertEquals(9, center.statGain());
		assertEquals(3, center.statResistance());
		assertFalse(center.crossingBase);

		// two neighbors → :1057 size != 1 → false, untouched, no draw consumed
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = V.of(A, 4, 12, 9, 3);
		center.neighbors[1] = V.of(B, 3, 30, 4, 6);
		assertFalse(CropMath.attemptSpreading(center, RandomSource.create(1)));
		assertNull(center.crop());

		// single empty stick → neighborCrop == null → false :1063-1065
		center = new V();
		center.crossingBase = true;
		center.neighbors[0] = new V();
		assertFalse(CropMath.attemptSpreading(center, RandomSource.create(1)));

		// gate fail — seed 1 first draw nextInt(16) = 11 > gate 4 → false, base survives
		center = new V();
		center.crossingBase = true;
		center.neighbors[2] = V.of(A, 4, 0, 0, 0);
		assertFalse(CropMath.attemptSpreading(center, RandomSource.create(1)));
		assertNull(center.crop());
		assertTrue(center.crossingBase);

		// tick-level spread — seed 0: crossing n3=0 hit but 1 neighbor < 2, then spreading clones
		center = new V();
		center.crossingBase = true;
		center.neighbors[2] = V.of(A, 4, 12, 9, 3);
		assertEquals(-1, CropMath.tickCrop(center, List.of(A, B), RandomSource.create(0)));
		assertSame(A, center.crop());
		assertEquals(12, center.statGrowth());
	}

	// ------------------------------------------------------------------ weeds (MECH:320-368, :241-251, :280)

	@Test
	public void weedWorkPins() {
		// seed 42 first nextInt(4) = 2 → slot 2 — conversion: rand(32)=1 ≥ Re 0 passes, weed-EX empty,
		// newGrowth = max(10, 3) = 10, nextBoolean = true → 11 (MECH:337-345)
		V src = V.of(GT6Crops.WEED, 2, 10, 0, 0);
		V dst = V.of(A, 1, 3, 5, 0);
		src.neighbors[2] = dst;
		assertEquals(-1, CropMath.performWeedWork(src, RandomSource.create(42)));
		assertSame(GT6Crops.WEED, dst.crop());
		assertEquals(1, dst.size());
		assertEquals(11, dst.statGrowth());
		assertEquals(0, dst.statGain());
		assertEquals(0, dst.statResistance());

		// boolean false → max without increment — seed 0
		src = V.of(GT6Crops.WEED, 2, 10, 0, 0);
		dst = V.of(A, 1, 3, 5, 0);
		src.neighbors[2] = dst;
		CropMath.performWeedWork(src, RandomSource.create(0));
		assertSame(GT6Crops.WEED, dst.crop());
		assertEquals(10, dst.statGrowth());

		// weed-EX suppression — hasWeedEX consumes 5 (MECH:361-368), target survives
		src = V.of(GT6Crops.WEED, 2, 10, 0, 0);
		dst = V.of(A, 1, 3, 5, 0);
		dst.weedEx = 10;
		src.neighbors[2] = dst;
		assertEquals(-1, CropMath.performWeedWork(src, RandomSource.create(42)));
		assertSame(A, dst.crop());
		assertEquals(5, dst.weedEx);

		// empty stick converts with no roll (neighborCrop == null arm :331) — src G10, boolean false path
		src = V.of(GT6Crops.WEED, 2, 10, 0, 0);
		V empty = new V();
		src.neighbors[2] = empty;
		CropMath.performWeedWork(src, RandomSource.create(42));
		assertSame(GT6Crops.WEED, empty.crop());
		assertEquals(1, empty.size());
		assertEquals(10, empty.statGrowth());

		// already-weed neighbor immune (isWeed short-circuits before any draw) :332
		src = V.of(GT6Crops.WEED, 2, 30, 0, 0);
		V weedDst = V.of(GT6Crops.WEED, 2, 5, 0, 0);
		src.neighbors[2] = weedDst;
		assertEquals(-1, CropMath.performWeedWork(src, RandomSource.create(42)));
		assertSame(GT6Crops.WEED, weedDst.crop());
		assertEquals(5, weedDst.statGrowth());

		// resistance 31 blocks (rand(32)=1 >= 31 false) :332
		src = V.of(GT6Crops.WEED, 2, 25, 0, 0);
		dst = V.of(A, 1, 3, 5, 31);
		src.neighbors[2] = dst;
		assertEquals(-1, CropMath.performWeedWork(src, RandomSource.create(42)));
		assertSame(A, dst.crop());

		// grass branch — MECH:347-357, slot reported to the BE (world effect stays BE-side)
		src = V.of(GT6Crops.WEED, 2, 25, 0, 0);
		src.soilAir[2] = true;
		assertEquals(2, CropMath.performWeedWork(src, RandomSource.create(42)));
		// neither crop neighbor nor soil → nothing
		assertEquals(-1, CropMath.performWeedWork(V.of(GT6Crops.WEED, 2, 25, 0, 0), RandomSource.create(42)));

		// tick end-to-end: weed work due roll rand(50)-Gr ≤ 2 (:280) fires on seed 29 —
		// weed G31 converts A G5 neighbor: newGrowth 31, no increment (31 not < 31)
		V weed = V.of(GT6Crops.WEED, 2, 31, 0, 0);
		V victim = V.of(A, 1, 5, 7, 0);
		weed.neighbors[0] = victim;
		assertEquals(-1, CropMath.tickCrop(weed, List.of(A), RandomSource.create(29)));
		assertSame(GT6Crops.WEED, victim.crop());
		assertEquals(31, victim.statGrowth());
		assertEquals(1, victim.size());

		// self-generated weed on an empty stick: 1% rand(100)==0 — seed 18 (:249-251)
		V bare = new V();
		CropMath.tickCrop(bare, List.of(A), RandomSource.create(18));
		assertSame(GT6Crops.WEED, bare.crop());
		assertEquals(1, bare.size());

		// weed-EX decay on an empty stick: rand(100)=0 and rand(10)=0 → -1 — seed 3 (:242-244)
		V ex = new V();
		ex.weedEx = 10;
		CropMath.tickCrop(ex, List.of(A), RandomSource.create(3));
		assertNull(ex.crop());
		assertEquals(9, ex.weedEx);

		// weed-EX > 0 blocks self-weed entirely, no decay on this draw — seed 18
		ex = new V();
		ex.weedEx = 3;
		CropMath.tickCrop(ex, List.of(A), RandomSource.create(18));
		assertNull(ex.crop());
		assertEquals(3, ex.weedEx);

		// rand(100) != 0 → neither weed nor decay — seed 42
		ex = new V();
		ex.weedEx = 7;
		CropMath.tickCrop(ex, List.of(A), RandomSource.create(42));
		assertNull(ex.crop());
		assertEquals(7, ex.weedEx);
	}

	// ------------------------------------------------------------------ harvest (MECH:793-823)

	@Test
	public void harvestPins() {
		// mature A (harvestSize 3) at size 5, Ga 0, tier 1: chance 0.95,
		// count = round(gauss×0.95×0.6827 + 0.95) = 2 on seed 42; both stacks wheat ×1
		// (Ga roll rand(100) ≤ 0 never fires); size resets to afterHarvestSize 2 (:817)
		V v = V.of(A, 5, 0, 0, 0);
		List<ItemStack> drops = CropMath.performHarvest(A, v, RandomSource.create(42));
		assertEquals(2, drops.size());
		assertEquals(Items.WHEAT, drops.get(0).getItem());
		assertEquals(1, drops.get(0).getCount());
		assertEquals(2, v.size());

		// Ga 20: chance 0.95×1.03^20 = 1.7158 → 3 drops on seed 42
		v = V.of(A, 5, 0, 20, 0);
		assertEquals(3, CropMath.performHarvest(A, v, RandomSource.create(42)).size());

		// Ga 100: every drop gains +1 (rand(100) ≤ 100 always) — count 2 each
		v = V.of(A, 5, 0, 100, 0);
		List<ItemStack> fat = CropMath.performHarvest(A, v, RandomSource.create(42));
		assertTrue(fat.size() >= 2);
		assertEquals(2, fat.get(0).getCount());

		// unripe → empty, size untouched (:794 canBeHarvested gate)
		v = V.of(A, 2, 0, 0, 0);
		assertTrue(CropMath.performHarvest(A, v, RandomSource.create(42)).isEmpty());
		assertEquals(2, v.size());

		// specialDrops — GT_BaseCrop.java:139-145 semantics via pickGain: seed 42 post-gauss draw
		// picks the special slot → first drop carrot ×1
		GT6CropCard sp = new GT6CropCard("Sp", "Tester", new ItemStack(Items.WHEAT),
				new ItemStack[] {new ItemStack(Items.CARROT), new ItemStack(Items.POTATO)}, null,
				1, 3, 1, 2, 0, 0, 0, 0, 0, new String[0]);
		v = V.of(sp, 3, 0, 0, 0);
		List<ItemStack> mixed = CropMath.performHarvest(sp, v, RandomSource.create(42));
		assertEquals(2, mixed.size());
		assertEquals(Items.CARROT, mixed.get(0).getItem());
		assertEquals(1, mixed.get(0).getCount());

		// count floors at 0 — MECH:812 Math.max(0L, round(...)); seed 11259 first gaussian
		// -1.06649 → round(-1.06649×0.6486 + 0.95) = round(0.258) = 0
		v = V.of(A, 5, 0, 0, 0);
		assertTrue(CropMath.performHarvest(A, v, RandomSource.create(11259)).isEmpty());
		assertEquals(2, v.size()); // size still resets — an empty harvest is still a harvest
	}

	// ------------------------------------------------------------------ pick seed (MECH:731-776)

	@Test
	public void pickSeedPins() {
		// mature (size 5 ≥ harvest 3): double roll — first always passes at (0.5856+1)×0.8 > 1,
		// second passes on seed 42 → 2 seeds carrying PRE-reset stats (:758-764), then clear()
		V v = V.of(A, 5, 10, 25, 4);
		v.scan = 2;
		List<ItemStack> seeds = CropMath.pickSeed(A, v, RandomSource.create(42));
		assertEquals(2, seeds.size());
		assertEquals(List.of("alpha:G10,Ga25,Re4,s2", "alpha:G10,Ga25,Re4,s2"), v.seedCalls);
		assertEquals(1, v.clearCalls);
		assertNull(v.crop());

		// unripe: single roll at chance×1.5 → misses on seed 42, but the tile still resets (:764)
		v = V.of(A, 2, 10, 25, 4);
		assertTrue(CropMath.pickSeed(A, v, RandomSource.create(42)).isEmpty());
		assertEquals(1, v.clearCalls);
		assertNull(v.crop());

		// size 1 → dropSeedChance 0 → no seed; reset still runs
		v = V.of(A, 1, 10, 25, 4);
		assertTrue(CropMath.pickSeed(A, v, RandomSource.create(42)).isEmpty());
		assertEquals(1, v.clearCalls);

		// the weed card never picks and never resets (CropWeed.java onLeftClick false)
		v = V.of(GT6Crops.WEED, 3, 10, 25, 4);
		assertTrue(CropMath.pickSeed(GT6Crops.WEED, v, RandomSource.create(42)).isEmpty());
		assertEquals(0, v.clearCalls);
		assertSame(GT6Crops.WEED, v.crop());
	}

	// ------------------------------------------------------------------ 256t cycle (MECH:210-283)

	@Test
	public void tickOrchestrationPins() {
		// empty non-base stick, rand(100) != 0 (seed 42) → nothing
		V v = new V();
		assertEquals(-1, CropMath.tickCrop(v, List.of(A, B), RandomSource.create(42)));
		assertNull(v.crop());

		// growth + storage drain — seed 42: points +10 (surplus chain as in growth test),
		// nutrients 3→2 then water 5→4 (:272-278, nutrients before water)
		v = V.of(A, 4, 0, 0, 0);
		v.hum = 10; v.nut = 10; v.air = 10;
		v.water = 5; v.nutrients = 3;
		assertEquals(-1, CropMath.tickCrop(v, List.of(A), RandomSource.create(42)));
		assertEquals(10, v.growthPoints());
		assertEquals(4, v.storageWater());
		assertEquals(2, v.storageNutrients());
		assertEquals(4, v.size()); // 10 < growthDuration 200

		// size-up at the duration threshold — preloaded 195 points + 181-cycle gain ≥ 200 →
		// points reset, size 4→5 (:265-269); seed 42 G31 terrain(30,30,30) totals 181
		v = V.of(A, 4, 31, 0, 0);
		v.hum = 30; v.nut = 30; v.air = 30;
		v.points = 195;
		CropMath.tickCrop(v, List.of(A), RandomSource.create(42));
		assertEquals(5, v.size());
		assertEquals(0, v.growthPoints());

		// death inside the growth tick aborts the cycle — MECH:261-263
		v = V.of(T11, 1, 0, 0, 0);
		v.hum = 0; v.nut = 0; v.air = 0;
		assertEquals(-1, CropMath.tickCrop(v, List.of(T11), RandomSource.create(42)));
		assertNull(v.crop());
		assertEquals(0, v.growthPoints());

		// trample — sprint collision 1% × rand(40) > Re (:492-498); block change stays BE-side
		V trampled = V.of(A, 1, 0, 0, 0);
		assertTrue(CropMath.isTrampled(A, trampled, RandomSource.create(18))); // rand(100)=0, rand(40)=4 > 0
		assertFalse(CropMath.isTrampled(A, V.of(A, 1, 0, 0, 5), RandomSource.create(18))); // 4 > 5 false
		assertFalse(CropMath.isTrampled(A, V.of(A, 1, 0, 0, 0), RandomSource.create(42))); // rand(100)=70 ≠ 0
	}

	// ------------------------------------------------------------------ registry (Crops.java:30-39 port)

	@Test
	public void registryPins() {
		// WEED sits at index 0 — seeded registration, crossing enumeration order contract
		assertSame(GT6Crops.WEED, GT6Crops.crops().get(0));
		assertThrows(UnsupportedOperationException.class, () -> GT6Crops.crops().add(A));

		int before = GT6Crops.crops().size();
		assertTrue(GT6Crops.registerCrop(A)); // first registration
		assertEquals(before + 1, GT6Crops.crops().size());
		assertFalse(GT6Crops.registerCrop(A)); // duplicate keeps first registration
		assertEquals(before + 1, GT6Crops.crops().size());
		assertFalse(GT6Crops.registerCrop(null));

		// base seed roundtrip — Crops.java:37-39 parameter order (stack, crop, size, Ga, Gr, Re)
		assertTrue(GT6Crops.registerBaseSeed(new ItemStack(Items.WHEAT), A, 1, 1, 1, 1));
		GT6Crops.BaseSeed seed = GT6Crops.getBaseSeed(new ItemStack(Items.WHEAT, 7));
		assertSame(A, seed.crop());
		assertEquals(1, seed.size());
		assertEquals(1, seed.statGain());
		assertEquals(1, seed.statGrowth());
		assertEquals(1, seed.statResistance());
		assertNull(GT6Crops.getBaseSeed(new ItemStack(Items.CARROT)));
		assertNull(GT6Crops.getBaseSeed(ItemStack.EMPTY));
		assertFalse(GT6Crops.registerBaseSeed(ItemStack.EMPTY, A, 1, 1, 1, 1));
		assertFalse(GT6Crops.registerBaseSeed(new ItemStack(Items.WHEAT), null, 1, 1, 1, 1));
	}
}
