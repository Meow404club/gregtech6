package gregtech6.crop;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

/**
 * Read/write state view a {@link GT6CropCard} and {@link CropMath} operate on — the port of IC2's
 * ICropTile读写器面 (API 权威: GT_BaseCrop/Behavior_* 实引的 ICropTile 签名, 机制正典:
 * tmp/harvest/ic2-crops/reference/ic2-source/decompiled/ic2/core/crop/TileEntityCrop.java).
 *
 * <p>ADR-CB5 contract: the crop-stick BlockEntity (cbc-1) implements this view and only does
 * registration/IO/level glue; all mechanics live in {@link CropMath} as pure functions taking
 * (view, RandomSource). Ownership: this interface belongs to cbc-2-crop-engine.
 *
 * <p>Neighbor indexing: slots 0..3 = NORTH, SOUTH, EAST, WEST — the fixed order used by
 * attemptCrossing (TileEntityCrop.java:939-942). A slot is null when that position does not hold
 * a crop tile.
 */
public interface CropTileView {

	/** Current crop card, null on an empty stick (ICropTile.getCrop). */
	@Nullable
	GT6CropCard crop();

	/** True while the tile is a double-stick crossing base (ICropTile.isCrossingBase). */
	boolean crossingBase();

	/** 1..maxSize (ICropTile.getCurrentSize). */
	int size();

	int statGrowth();

	int statGain();

	int statResistance();

	int growthPoints();

	int scanLevel();

	int storageWater();

	int storageNutrients();

	int storageWeedEx();

	/** Cached terrain values, recomputed by the BE; negative means "stale" (reset :832-834). */
	int terrainHumidity();

	int terrainNutrients();

	int terrainAirQuality();

	/**
	 * Crop tile at orthogonal neighbor slot (0..3), null if none. Writable — weed conversion and
	 * crossing/spreading mutate neighbors through the same view contract.
	 */
	@Nullable
	CropTileView neighbor(int aIndex);

	/**
	 * True when the position at neighbor slot is air over dirt/grass/farmland — the world probe of
	 * performWeedWork's grass branch (TileEntityCrop.java:347-357). World effect execution stays in
	 * the BE; this is only the read.
	 */
	boolean soilAirAt(int aIndex);

	/**
	 * Seed stack carrying (card, growth, gain, resistance, scan) — port of ICropTile.generateSeeds
	 * (TileEntityCrop.java:894-897, delegate to the seed item). The seed item itself is cbc-3's face.
	 */
	ItemStack generateSeeds(GT6CropCard aCard, int aGrowth, int aGain, int aResistance, int aScan);

	void setCrop(@Nullable GT6CropCard aCrop);

	void setCrossingBase(boolean aCrossingBase);

	void setSize(int aSize);

	void setStats(int aGrowth, int aGain, int aResistance);

	void setGrowthPoints(int aGrowthPoints);

	void setScanLevel(int aScanLevel);

	void setStorages(int aWater, int aNutrients, int aWeedEx);

	/** Full reset — port of TileEntityCrop.reset() :826-839 (crop cleared, stats zeroed, terrain stale, size 1). */
	void clear();
}
