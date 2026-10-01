package gregtech6.crop;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The crop tick pure functions (ADR-CB5: every formula takes an injected {@link RandomSource}
 * and {@link CropTileView}/plain inputs  --  no Level, no BlockPos, no BE state  --  so growth
 * correctness is offline seeded-pinnable and the 256t cycle never needs a live server).
 *
 * <p>Every formula is the decompiled 1.12 TileEntityCrop verbatim (tmp/harvest/ic2-crops/
 * reference/ic2-source/decompiled/ic2/core/crop/TileEntityCrop.java  --  the ADR-CB1 mechanism
 * authority, GTNH 1.7.10 wiki cross-verified per research.crop-breeding), with the source
 * line anchor per member. The world READS (biome lookup, farmland scan, sky check) stay in
 * the BE shell; only the arithmetic lives here.
 */
public final class CropTickLogic {

	/** The fixed evaluation cadence  --  TileEntityCrop.java:56 {@code tickrate = 256}. */
	public static final int TICKRATE = 256;

	private CropTickLogic() {}

	// -------------------------------------------------------------- biome humidity bonus

	/**
	 * TileEntityCrop.updateBiomeHumidityBonus :1237-1250 verbatim  --  the rainfall term
	 * {@code (int)(25*rainfall - 12.5)} clamped +-10, times the temperature coefficient
	 * {@code -2T² + 4T - 1}, clamped +-10. The caller supplies the modern biome faces
	 * ({@code getModifiedClimateSettings().downfall()} / {@code getBaseTemperature()}  --  the
	 * position height-modifier seam is private vanilla, declared deviation).
	 */
	public static int biomeHumidityBonus(float aRainfall, float aTemperature) {
		int tRainfallBonus = (int) (25.0F * aRainfall - 12.5);
		tRainfallBonus = Math.min(10, Math.max(-10, tRainfallBonus));
		int tCoefficientBonus = (int) (Math.abs(tRainfallBonus) * (-2.0 * Math.pow(aTemperature, 2.0) + 4.0F * aTemperature - 1.0));
		tCoefficientBonus = Math.min(10, Math.max(-10, tCoefficientBonus));
		return tRainfallBonus + tCoefficientBonus;
	}

	// -------------------------------------------------------------- the terrain three

	/**
	 * TileEntityCrop.updateTerrainHumidity :534-546 verbatim  --  biome bonus + farmland
	 * moisture >= 7  ->  +2 + storage >= 5  ->  +2 + {@code (storageWater + 24) / 25}.
	 */
	public static int terrainHumidity(int aBiomeBonus, boolean aFarmlandMoist, int aStorageWater) {
		int tHumidity = aBiomeBonus;
		if (aFarmlandMoist) tHumidity += 2;
		if (aStorageWater >= 5) tHumidity += 2;
		tHumidity += (aStorageWater + 24) / 25;
		return tHumidity;
	}

	/**
	 * TileEntityCrop.updateTerrainNutrients :548-557 verbatim  --  the biome-key table bonus
	 * (the {@link #nutrientBiomeBonus} adaptation of Crops.getNutrientBiomeBonus, IC2Crops
	 * :131-143) + the below-dirt count + {@code (storageNutrients + 19) / 20}.
	 */
	public static int terrainNutrients(int aBiomeBonus, int aDirtBelow, int aStorageNutrients) {
		return aBiomeBonus + aDirtBelow + (aStorageNutrients + 19) / 20;
	}

	/**
	 * TileEntityCrop.updateTerrainAirQuality :503-532 arithmetic  --  the y band
	 * {@code clamp(floor((y-40)/15), 0, 2)} + the caller-counted fresh cells ({@code fresh / 2},
	 * the corner-2x2 scan stays a world read) + canSeeSky  ->  +4.
	 */
	public static int terrainAirQuality(int aY, int aFresh, boolean aCanSeeSky) {
		int tHeight = (int) Math.floor((aY - 40) / 15.0);
		if (tHeight > 2) tHeight = 2;
		if (tHeight < 0) tHeight = 0;
		return tHeight + aFresh / 2 + (aCanSeeSky ? 4 : 0);
	}

	/**
	 * The nutrient biome table  --  IC2Crops.java:131-143 rows, the Crops.getNutrientBiomeBonus
	 * max-over-types semantics (:380-391). BiomeDictionary died in the flattening, so the
	 * 1.12 type rows ride explicit vanilla biome keys (the declared adaptation, ADR-CB3
	 * reviewed): JUNGLE/SWAMP +10, MUSHROOM/FOREST +5, RIVER +2, PLAINS 0, SAVANNA -2,
	 * HILLS/MOUNTAIN -5, WASTELAND -8, END/NETHER/DEAD -10. The WASTELAND/DEAD rows have no
	 * vanilla biome (BiomeDictionary-only types)  --  unported, modded biomes fall out at 0 like
	 * the PLAINS row. A null key (offline synthetic holders) = 0.
	 */
	public static int nutrientBiomeBonus(net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> aBiome) {
		if (aBiome == null) return 0;
		Integer tBonus = NUTRIENT_ROWS.get(aBiome);
		return tBonus == null ? 0 : tBonus;
	}

	// -------------------------------------------------------------- the 256t cycle pieces

	/**
	 * The growth evaluation  --  TileEntityCrop.performGrowthTick :285-318 verbatim: base
	 * {@code 3 + rand(7) + statGrowth}; minimum {@code max(0, (tier-1)*4 + G + Ga + Re)};
	 * provided {@code weightInfluences(H,N,A) * 5}; surplus scales up, deficit
	 * {@code aux = deficit*4} scales down and {@code aux > 100 && rand(32) > Re} kills the
	 * plant (the caller applies {@code reset()}). Returns true when the plant died.
	 */
	public static boolean growthTick(CropTileView aCrop, CropCardView aCard, RandomSource aRandom) {
		if (aCard == null) return false;
		int tBaseGrowth = 3 + aRandom.nextInt(7) + aCrop.getStatGrowth();
		int tMinimumQuality = (aCard.tier() - 1) * 4 + aCrop.getStatGrowth() + aCrop.getStatGain() + aCrop.getStatResistance();
		tMinimumQuality = Math.max(0, tMinimumQuality);
		int tProvidedQuality = aCard.weightInfluences(
				aCrop.getTerrainHumidity(), aCrop.getTerrainNutrients(), aCrop.getTerrainAirQuality()) * 5;
		int tTotalGrowth;
		if (tProvidedQuality >= tMinimumQuality) {
			tTotalGrowth = tBaseGrowth * (100 + (tProvidedQuality - tMinimumQuality)) / 100;
		} else {
			int tAux = (tMinimumQuality - tProvidedQuality) * 4;
			if (tAux > 100 && aRandom.nextInt(32) > aCrop.getStatResistance()) {
				return true; // the caller resets  --  the :301 reset() ride-along
			}
			tTotalGrowth = tBaseGrowth * (100 - tAux) / 100;
			tTotalGrowth = Math.max(0, tTotalGrowth);
		}
		aCrop.setGrowthPoints(aCrop.getGrowthPoints() + tTotalGrowth);
		return false;
	}

	/**
	 * The empty-tile weed branch  --  TileEntityCrop.performTick :240-252 verbatim (the
	 * crossing/spreading attempt arms ahead of it are cbc-2's engine hooks). RNG order is
	 * contractual for the seeded pins: nextInt(100) first; the Weed-EX drain nextInt(10)
	 * only when the tile is protected. Weed-EX > 0 fully suppresses (:241).
	 */
	public enum WeedRoll { NOTHING, DRAIN_WEED_EX, BECOME_WEED }

	public static WeedRoll weedSelfGenRoll(int aStorageWeedEX, RandomSource aRandom) {
		if (aRandom.nextInt(100) != 0 || aStorageWeedEX > 0) {
			if (aStorageWeedEX > 0 && aRandom.nextInt(10) == 0) return WeedRoll.DRAIN_WEED_EX;
			return WeedRoll.NOTHING;
		}
		return WeedRoll.BECOME_WEED;
	}

	/**
	 * The weed-spread work gate  --  TileEntityCrop :280 verbatim
	 * {@code crop.isWeed(this) && rand(50) - statGrowth <= 2}. The work itself (neighbor
	 * conversion/grass seeding) is cbc-2's hook; this gate only paces it.
	 */
	public static boolean weedWorkDue(CropTileView aCrop, CropCardView aCard, RandomSource aRandom) {
		return aCard.isWeed(aCrop) && aRandom.nextInt(50) - aCrop.getStatGrowth() <= 2;
	}

	/**
	 * TileEntityCrop.performHarvest :793-823 verbatim  --  requires canBeHarvested; the count is
	 * the Gaussian {@code round(gauss * chance * 0.6827 + chance)} over
	 * {@code dropGainChance() * 1.03^Ga}; each gain item rolls {@code rand(100) <= Ga  ->  +1}
	 * (:815). Returns null when nothing was harvestable (the :821 null contract).
	 */
	public static List<ItemStack> harvestGains(CropTileView aCrop, CropCardView aCard, RandomSource aRandom) {
		if (!aCard.canBeHarvested(aCrop)) return null;
		double tChance = aCard.dropGainChance() * Math.pow(1.03, aCrop.getStatGain());
		int tDropCount = (int) Math.max(0L, Math.round(aRandom.nextGaussian() * tChance * 0.6827 + tChance));
		List<ItemStack> tRet = new ArrayList<>(tDropCount);
		for (int i = 0; i < tDropCount; i++) {
			for (ItemStack tDrop : aCard.gains(aCrop)) {
				if (!tDrop.isEmpty() && aRandom.nextInt(100) <= aCrop.getStatGain()) {
					tDrop.grow(1);
				}
				tRet.add(tDrop);
			}
		}
		return tRet;
	}

	/**
	 * TileEntityCrop.pick :731-756 verbatim  --  the seed count only (the caller builds stacks
	 * from {@code card.seeds} and resets): mature  ->  the (chance+1)*0.8 first roll + the
	 * Ga-tapered second roll; immature  ->  the single 1.5x roll.
	 */
	public static int pickSeedCount(CropTileView aCrop, CropCardView aCard, RandomSource aRandom) {
		if (aCard == null) return 0;
		boolean tBonus = aCard.canBeHarvested(aCrop);
		float tFirstChance = (float) (aCard.dropSeedChance(aCrop) * Math.pow(1.1, aCrop.getStatResistance()));
		int tDropCount = 0;
		if (tBonus) {
			if (aRandom.nextFloat() <= (tFirstChance + 1.0F) * 0.8F) tDropCount++;
			float tChance = aCard.dropSeedChance(aCrop) + aCrop.getStatGrowth() / 100.0F;
			for (int index = 23; index < aCrop.getStatGain(); index++) tChance *= 0.95F;
			if (aRandom.nextFloat() <= tChance) tDropCount++;
		} else if (aRandom.nextFloat() <= tFirstChance * 1.5F) {
			tDropCount++;
		}
		return tDropCount;
	}

	/**
	 * TileEntityCrop.onEntityCollision :485-499 verbatim  --  the IC2 CropCard default gate
	 * {@code LivingBase && isSprinting} (ic2/api/crops/CropCard.java :165-167, the GT6 cards
	 * inherit it; the caller does the instanceof) rides in as the flag: 1% x
	 * {@code rand(40) > statResistance} tramples (the caller resets + farmland -> dirt).
	 */
	public static boolean trampleDue(CropTileView aCrop, boolean aSprintingLivingEntity, RandomSource aRandom) {
		if (!aSprintingLivingEntity) return false;
		return aRandom.nextInt(100) == 0 && aRandom.nextInt(40) > aCrop.getStatResistance();
	}

	// -------------------------------------------------------------- the nutrient table rows

	/** The vanilla-biome adaptation of the IC2Crops :131-143 table (see {@link #nutrientBiomeBonus}). */
	private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>, Integer> NUTRIENT_ROWS = java.util.Map.ofEntries(
			// JUNGLE +10
			row(net.minecraft.world.level.biome.Biomes.JUNGLE, 10),
			row(net.minecraft.world.level.biome.Biomes.BAMBOO_JUNGLE, 10),
			row(net.minecraft.world.level.biome.Biomes.SPARSE_JUNGLE, 10),
			// SWAMP +10
			row(net.minecraft.world.level.biome.Biomes.SWAMP, 10),
			row(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP, 10),
			// MUSHROOM +5
			row(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS, 5),
			// FOREST +5 (the 1.12 FOREST type spanned the forest + taiga families)
			row(net.minecraft.world.level.biome.Biomes.FOREST, 5),
			row(net.minecraft.world.level.biome.Biomes.BIRCH_FOREST, 5),
			row(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_BIRCH_FOREST, 5),
			row(net.minecraft.world.level.biome.Biomes.DARK_FOREST, 5),
			row(net.minecraft.world.level.biome.Biomes.FLOWER_FOREST, 5),
			row(net.minecraft.world.level.biome.Biomes.TAIGA, 5),
			row(net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA, 5),
			row(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA, 5),
			row(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_SPRUCE_TAIGA, 5),
			// RIVER +2
			row(net.minecraft.world.level.biome.Biomes.RIVER, 2),
			row(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER, 2),
			// SAVANNA -2
			row(net.minecraft.world.level.biome.Biomes.SAVANNA, -2),
			row(net.minecraft.world.level.biome.Biomes.SAVANNA_PLATEAU, -2),
			row(net.minecraft.world.level.biome.Biomes.WINDSWEPT_SAVANNA, -2),
			// HILLS -5
			row(net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS, -5),
			row(net.minecraft.world.level.biome.Biomes.WINDSWEPT_GRAVELLY_HILLS, -5),
			// MOUNTAIN -5 (the 1.12 MOUNTAIN type = the extreme-hills family  ->  the 1.18 peak band)
			row(net.minecraft.world.level.biome.Biomes.JAGGED_PEAKS, -5),
			row(net.minecraft.world.level.biome.Biomes.FROZEN_PEAKS, -5),
			row(net.minecraft.world.level.biome.Biomes.STONY_PEAKS, -5),
			row(net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES, -5),
			row(net.minecraft.world.level.biome.Biomes.MEADOW, -5),
			// END -10
			row(net.minecraft.world.level.biome.Biomes.THE_END, -10),
			row(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, -10),
			row(net.minecraft.world.level.biome.Biomes.END_MIDLANDS, -10),
			row(net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS, -10),
			row(net.minecraft.world.level.biome.Biomes.END_BARRENS, -10),
			// NETHER -10
			row(net.minecraft.world.level.biome.Biomes.NETHER_WASTES, -10),
			row(net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY, -10),
			row(net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST, -10),
			row(net.minecraft.world.level.biome.Biomes.WARPED_FOREST, -10),
			row(net.minecraft.world.level.biome.Biomes.BASALT_DELTAS, -10));

	private static java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>, Integer> row(
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> aKey, int aBonus) {
		return java.util.Map.entry(aKey, aBonus);
	}
}
