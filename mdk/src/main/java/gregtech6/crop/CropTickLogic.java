package gregtech6.crop;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * The crop world-INPUT arithmetic (ADR-CB5: the pure functions over the terrain reads the BE
 * shell gathers -- biome climate, farmland moisture, dirt stack, occlusion, sky). The gameplay
 * mechanics (growth/weed/harvest/pick/trample) are {@link CropMath}'s since the cbc-2 merge
 * (the single-engine ruling -- this class used to carry a parallel engine half before the
 * cbc-1 -> cbc-2 integration retired it).
 *
 * <p>Every formula is the decompiled 1.12 TileEntityCrop verbatim (tmp/harvest/ic2-crops/
 * reference/ic2-source/decompiled/ic2/core/crop/TileEntityCrop.java -- the ADR-CB1 mechanism
 * authority, GTNH 1.7.10 wiki cross-verified per research.crop-breeding), with the source
 * line anchor per member.
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
	public static int nutrientBiomeBonus(ResourceKey<Biome> aBiome) {
		if (aBiome == null) return 0;
		Integer tBonus = NUTRIENT_ROWS.get(aBiome);
		return tBonus == null ? 0 : tBonus;
	}

	// -------------------------------------------------------------- the nutrient table rows

	/** The vanilla-biome adaptation of the IC2Crops :131-143 table (see {@link #nutrientBiomeBonus}). */
	private static final java.util.Map<ResourceKey<Biome>, Integer> NUTRIENT_ROWS = java.util.Map.ofEntries(
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

	private static java.util.Map.Entry<ResourceKey<Biome>, Integer> row(
			ResourceKey<Biome> aKey, int aBonus) {
		return java.util.Map.entry(aKey, aBonus);
	}
}
