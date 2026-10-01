package gregtech6.crop;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Pure-function crop engine — port of the decompiled IC2 mechanics
 * (tmp/harvest/ic2-crops/reference/ic2-source/decompiled/ic2/core/crop/TileEntityCrop.java, 1.12 line
 * 2.8.x, cross-verified against the GTNH wiki 1.7.10 line per ADR-CB1; line anchors cite the
 * decompiled file). ADR-CB5: every function takes the state {@link CropTileView} and an injected
 * {@link RandomSource}, so seeded tests pin exact outcomes and the cbc-1 BE stays a thin shell.
 *
 * <p>RNG call order mirrors the upstream flow statement-for-statement (including short-circuits) —
 * it is part of the pinned contract.
 *
 * <p>RCON face: none — growth takes 256t × tier*200 cycles, offline seeded pins own correctness
 * (ADR-CB5 verdict); KJS face: none (algorithm layer). Viewer face: none — no machine diagram,
 * zero JEI/EMI surfaces. Jade face: another card, not this wave (the cbc-6 face-census).
 */
public final class CropMath {

	private CropMath() {}

	// ------------------------------------------------------------------ growth

	/**
	 * Port of performGrowthTick TileEntityCrop.java:285-318.
	 * base = 3 + rand(7) + statGrowth; needed = (tier-1)*4 + G + Ga + Re (min 0);
	 * provided = weightInfluences(H, N, A) * 5. Surplus scales up, deficit ≥ 25 (aux*4 > 100)
	 * risks death (rand(32) > Re → reset) — Re 31 is immune — otherwise scales down, floored at 0.
	 */
	public static void performGrowthTick(GT6CropCard aCard, CropTileView aCrop, RandomSource aRNG) {
		int baseGrowth = 3 + aRNG.nextInt(7) + aCrop.statGrowth();
		int minimumQuality = (aCard.tier() - 1) * 4 + aCrop.statGrowth() + aCrop.statGain() + aCrop.statResistance();
		minimumQuality = Math.max(minimumQuality, 0);
		int providedQuality = aCard.weightInfluences(aCrop.terrainHumidity(), aCrop.terrainNutrients(), aCrop.terrainAirQuality()) * 5;
		int totalGrowth;
		if (providedQuality >= minimumQuality) {
			totalGrowth = baseGrowth * (100 + (providedQuality - minimumQuality)) / 100;
		} else {
			int aux = (minimumQuality - providedQuality) * 4;
			if (aux > 100 && aRNG.nextInt(32) > aCrop.statResistance()) {
				aCrop.clear(); // death, TileEntityCrop.java:301
				totalGrowth = 0;
			} else {
				totalGrowth = baseGrowth * (100 - aux) / 100;
				totalGrowth = Math.max(totalGrowth, 0);
			}
		}
		aCrop.setGrowthPoints(aCrop.growthPoints() + totalGrowth);
	}

	// ------------------------------------------------------------------ weeds

	/**
	 * Port of performWeedWork TileEntityCrop.java:320-359. Picks a random neighbor slot, tries to
	 * convert a crop neighbor to weed (empty sticks convert unconditionally, weeds are immune,
	 * rand(32) >= Re and weed-EX resist; new statGrowth = max(src,dst) then +1 on a 50% roll below 31),
	 * else reports a grass placement slot when the target is soil+air.
	 *
	 * @return neighbor slot to grow grass+tallgrass on, or -1 (world effect executed by the BE).
	 */
	public static int performWeedWork(CropTileView aCrop, RandomSource aRNG) {
		int dir = aRNG.nextInt(4);
		CropTileView dst = aCrop.neighbor(dir);
		if (dst != null) {
			GT6CropCard neighborCrop = dst.crop();
			// TileEntityCrop.java:331-332 — null crop converts with no roll; short-circuits preserve RNG order
			if (neighborCrop == null || (!neighborCrop.isWeed(dst) && aRNG.nextInt(32) >= dst.statResistance() && !consumeWeedEx(dst))) {
				int newGrowth = Math.max(aCrop.statGrowth(), dst.statGrowth());
				if (newGrowth < 31 && aRNG.nextBoolean()) newGrowth++;
				dst.clear();
				dst.setCrop(GT6Crops.WEED);
				dst.setSize(1);
				dst.setStats(newGrowth, 0, 0);
			}
		} else if (aCrop.soilAirAt(dir)) {
			return dir; // TileEntityCrop.java:347-357 grass branch
		}
		return -1;
	}

	/** Port of hasWeedEX TileEntityCrop.java:361-368 — a suppression consumes 5 storage. */
	private static boolean consumeWeedEx(CropTileView aCrop) {
		int weedEx = aCrop.storageWeedEx();
		if (weedEx > 0) {
			aCrop.setStorages(aCrop.storageWater(), aCrop.storageNutrients(), weedEx - 5);
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ crossing

	/**
	 * Qualification gate of a neighbor — port of checkCrossingAvailability's base
	 * TileEntityCrop.java:1140-1151: 4, +1 at statGrowth 16, +1 at 30, + (27 - Re) when Re >= 28.
	 * Qualifies when base >= rand(16) (:1153). Shared verbatim by spreading (:1068-1079).
	 */
	public static int crossingGate(CropTileView aNeighbor) {
		int base = 4;
		if (aNeighbor.statGrowth() >= 16) base++;
		if (aNeighbor.statGrowth() >= 30) base++;
		if (aNeighbor.statResistance() >= 28) base += 27 - aNeighbor.statResistance();
		return base;
	}

	/**
	 * Weight of a candidate crop against one neighbor — port of calculateRatioFor
	 * TileEntityCrop.java:1098-1131: same crop 500; else Σ over the 5 stats of (2 - |diff|),
	 * +5 per shared attribute (case-insensitive), tier diff > 1 → -2*diff, diff < -3 → +diff
	 * (i.e. -(-diff)), clamped >= 0.
	 */
	public static int crossingWeight(GT6CropCard aNewCrop, GT6CropCard aOldCrop) {
		if (aNewCrop == aOldCrop) return 500;
		int value = 0;
		for (int i = 0; i < 5; i++) {
			value += -Math.abs(aOldCrop.stat(i) - aNewCrop.stat(i)) + 2;
		}
		for (String attributeNew : aNewCrop.attributes()) {
			for (String attributeOld : aOldCrop.attributes()) {
				if (attributeNew.equalsIgnoreCase(attributeOld)) value += 5;
			}
		}
		int diff = aNewCrop.tier() - aOldCrop.tier();
		if (diff > 1) value -= 2 * diff;
		if (diff < -3) value -= -diff;
		return Math.max(value, 0);
	}

	/**
	 * Port of attemptCrossing TileEntityCrop.java:933-1044 — only meaningful on an empty crossing
	 * base (caller checks crop()==null && crossingBase()). 1/3 chance per cycle; the 4 neighbor slots
	 * qualify via {@link #crossingGate} against rand(16) and at least 2 must pass; the resulting crop
	 * is a weighted pick over all registered candidates; stats inherit the neighbor average with a
	 * ±rand(1+2n)-n perturbation, clamped 0..31.
	 *
	 * <p>ponytail: candidate pick is a linear scan of the cumulative table (upstream binary-searches
	 * :994-1006) — pure performance swap, same selection; revisit only if candidate counts explode.
	 * Declared deviation: total weight 0 returns false (upstream nextInt(0) would throw, :986).
	 */
	public static boolean attemptCrossing(CropTileView aCrop, List<GT6CropCard> aCandidates, RandomSource aRNG) {
		if (aRNG.nextInt(3) != 0) return false;

		List<CropTileView> neighbours = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) { // slot order N,S,E,W — TileEntityCrop.java:939-942
			CropTileView side = aCrop.neighbor(i);
			if (side == null) continue;
			GT6CropCard neighborCrop = side.crop();
			if (neighborCrop != null && neighborCrop.canGrow(aCrop) && neighborCrop.canCross(side)
					&& crossingGate(side) >= aRNG.nextInt(16)) { // :1153, roll only after the static conditions
				neighbours.add(side);
			}
		}
		if (neighbours.size() < 2) return false;
		if (aCandidates.isEmpty()) return false;

		int[] ratios = new int[aCandidates.size()];
		int total = 0;
		for (int i = 0; i < ratios.length; i++) { // :962-974 — cumulative, ineligible crops keep a zero-width band
			GT6CropCard crop = aCandidates.get(i);
			if (crop.canGrow(aCrop)) {
				for (CropTileView neighbour : neighbours) {
					total += crossingWeight(crop, neighbour.crop());
				}
			}
			ratios[i] = total;
		}
		if (total <= 0) return false;

		int search = aRNG.nextInt(total); // :986
		int picked = 0;
		while (search >= ratios[picked]) picked++; // first cumulative band exceeding the roll

		aCrop.setCrossingBase(false);
		aCrop.setCrop(aCandidates.get(picked));
		aCrop.setSize(1);

		int sumGrowth = 0, sumGain = 0, sumResistance = 0; // :1024-1033 sum → average
		for (CropTileView neighbour : neighbours) {
			sumGrowth += neighbour.statGrowth();
			sumResistance += neighbour.statResistance();
			sumGain += neighbour.statGain();
		}
		int count = neighbours.size();
		int growth = sumGrowth / count, gain = sumGain / count, resistance = sumResistance / count;
		growth += aRNG.nextInt(1 + 2 * count) - count; // :1034-1036 perturbation order G, Ga, Re
		gain += aRNG.nextInt(1 + 2 * count) - count;
		resistance += aRNG.nextInt(1 + 2 * count) - count;
		aCrop.setStats(clamp31(growth), clamp31(gain), clamp31(resistance)); // Util.limit 0..31, :1037-1039
		return true;
	}

	private static int clamp31(int aValue) {
		return Math.max(0, Math.min(31, aValue));
	}

	// ------------------------------------------------------------------ spreading

	/**
	 * Port of attemptSpreading TileEntityCrop.java:1046-1096 — exactly one neighbor crop tile with a
	 * crop on it passes the gate and clones itself (same card, same stats, size 1) onto this empty base.
	 */
	public static boolean attemptSpreading(CropTileView aCrop, RandomSource aRNG) {
		CropTileView side = null;
		for (int i = 0; i < 4; i++) {
			CropTileView neighbor = aCrop.neighbor(i);
			if (neighbor != null) {
				if (side != null) return false; // second crop tile — not "exactly one"
				side = neighbor;
			}
		}
		if (side == null || side.crop() == null) return false;

		GT6CropCard neighborCrop = side.crop();
		if (neighborCrop.canGrow(aCrop) && neighborCrop.canCross(side)) { // :1067, center view quirk kept literal
			if (crossingGate(side) < aRNG.nextInt(16)) return false;
			aCrop.setCrossingBase(false);
			aCrop.setCrop(neighborCrop);
			aCrop.setSize(1);
			aCrop.setStats(side.statGrowth(), side.statGain(), side.statResistance());
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ harvest

	/**
	 * Port of performHarvest TileEntityCrop.java:793-823 — chance = dropGainChance(0.95^tier) ×
	 * 1.03^Ga; count = round(gauss × chance × 0.6827 + chance) floored at 0; each drop rolls the
	 * card's gain (specialDrops via {@link GT6CropCard#pickGain}) and +1 on rand(100) <= Ga.
	 * Size resets to the card's after-harvest size. Empty list when not harvestable (upstream null).
	 */
	public static List<ItemStack> performHarvest(GT6CropCard aCard, CropTileView aCrop, RandomSource aRNG) {
		if (aCard == null || !aCard.canBeHarvested(aCrop)) return List.of();
		double chance = aCard.dropGainChance() * Math.pow(1.03, aCrop.statGain());
		int dropCount = (int)Math.max(0L, Math.round(aRNG.nextGaussian() * chance * 0.6827 + chance));
		List<ItemStack> ret = new ArrayList<>();
		for (int i = 0; i < dropCount; i++) {
			ItemStack gain = aCard.pickGain(aCrop, aRNG);
			if (!gain.isEmpty() && aRNG.nextInt(100) <= aCrop.statGain()) gain.grow(1); // :815
			ret.add(gain);
		}
		aCrop.setSize(aCard.sizeAfterHarvest());
		return ret;
	}

	/**
	 * Port of pick TileEntityCrop.java:731-776 — left-click seed pull. Mature crops roll twice
	 * (first at (chance+1)*0.8, second at chance + statGrowth/100 with Ga > 23 ×0.95 per point),
	 * unripe once at chance × 1.5; chance = dropSeedChance(0.5 base, size 2 halved, ×0.8^tier) ×
	 * 1.1^Re. Seeds carry the pre-reset stats, then the tile resets.
	 */
	public static List<ItemStack> pickSeed(GT6CropCard aCard, CropTileView aCrop, RandomSource aRNG) {
		if (aCard == null || !aCard.leftClickPicksSeed()) return List.of();
		boolean bonus = aCard.canBeHarvested(aCrop);
		float firstChance = (float)(aCard.dropSeedChance(aCrop) * Math.pow(1.1, aCrop.statResistance()));
		int dropCount = 0;
		if (bonus) {
			if (aRNG.nextFloat() <= (firstChance + 1.0F) * 0.8F) dropCount++;
			float chance = aCard.dropSeedChance(aCrop) + aCrop.statGrowth() / 100.0F;
			for (int index = 23; index < aCrop.statGain(); index++) {
				chance *= 0.95F;
			}
			if (aRNG.nextFloat() <= chance) dropCount++;
		} else if (aRNG.nextFloat() <= firstChance * 1.5F) {
			dropCount++;
		}
		List<ItemStack> seeds = new ArrayList<>(dropCount);
		for (int i = 0; i < dropCount; i++) {
			seeds.add(aCrop.generateSeeds(aCard, aCrop.statGrowth(), aCrop.statGain(), aCrop.statResistance(), aCrop.scanLevel()));
		}
		aCrop.clear(); // :764, after the seeds carry the stats off
		return seeds;
	}

	// ------------------------------------------------------------------ 256t cycle orchestration

	/**
	 * Port of performTick TileEntityCrop.java:210-283 (one 256t cycle; the ticker-modulo terrain
	 * refresh and the card tick no-op stay in the BE / are absorbed — GT_BaseCrop does not override
	 * CropCard.tick). Empty bases attempt crossing then spreading; otherwise the 1% self-weed roll
	 * runs (weed-EX suppresses and decays 1/10). Grown crops accumulate growth points, size up on
	 * reaching the growth duration, storages drain 1/cycle, and weeds do weed work.
	 *
	 * @return neighbor slot for the BE to grow grass on (weed work), or -1.
	 */
	public static int tickCrop(CropTileView aCrop, List<GT6CropCard> aCandidates, RandomSource aRNG) {
		if (aCrop.crop() == null) {
			boolean crossed = aCrop.crossingBase() && attemptCrossing(aCrop, aCandidates, aRNG);
			boolean spread = !crossed && aCrop.crossingBase() && attemptSpreading(aCrop, aRNG);
			if (aCrop.crop() == null && !crossed && !spread) { // :240 literal
				if (aRNG.nextInt(100) != 0 || aCrop.storageWeedEx() > 0) { // :241
					if (aCrop.storageWeedEx() > 0 && aRNG.nextInt(10) == 0) { // :242-244
						aCrop.setStorages(aCrop.storageWater(), aCrop.storageNutrients(), aCrop.storageWeedEx() - 1);
					}
					return -1;
				}
				aCrop.clear();
				aCrop.setCrop(GT6Crops.WEED); // :249-251 self-generated weed
				aCrop.setSize(1);
			}
		}

		GT6CropCard card = aCrop.crop();
		if (card == null) return -1; // unreachable upstream (crop.tick would NPE); defensive for the view contract

		if (card.canGrow(aCrop)) { // :259-270
			performGrowthTick(card, aCrop, aRNG);
			if (aCrop.crop() == null) return -1; // died in the growth tick
			if (aCrop.growthPoints() >= card.growthDuration()) {
				aCrop.setGrowthPoints(0);
				aCrop.setSize(aCrop.size() + 1);
			}
		}

		if (aCrop.storageNutrients() > 0) { // :272-274 nutrients before water
			aCrop.setStorages(aCrop.storageWater(), aCrop.storageNutrients() - 1, aCrop.storageWeedEx());
		}
		if (aCrop.storageWater() > 0) { // :276-278
			aCrop.setStorages(aCrop.storageWater() - 1, aCrop.storageNutrients(), aCrop.storageWeedEx());
		}

		if (card.isWeed(aCrop) && aRNG.nextInt(50) - aCrop.statGrowth() <= 2) { // :280, roll only when weed-like
			return performWeedWork(aCrop, aRNG);
		}
		return -1;
	}

	/**
	 * Trample roll — port of onEntityCollision TileEntityCrop.java:492-498; block change stays
	 * with the BE. The card hook rides first ({@code crop.onEntityCollision} — the sprint check
	 * is caller-side; CropWeed answers false and never rolls, no RNG consumed on that arm).
	 */
	public static boolean isTrampled(@Nullable GT6CropCard aCard, CropTileView aCrop, RandomSource aRNG) {
		return aCard != null && aCard.entityTramples()
				&& aRNG.nextInt(100) == 0 && aRNG.nextInt(40) > aCrop.statResistance();
	}

	/** The caller-side sprint gate overload (CropCard :166 default = a living, sprinting entity);
	 *  no RNG is consumed on the walk-by arm. */
	public static boolean isTrampled(@Nullable GT6CropCard aCard, CropTileView aCrop, boolean aSprintingLiving, RandomSource aRNG) {
		return aSprintingLiving && isTrampled(aCard, aCrop, aRNG);
	}
}
