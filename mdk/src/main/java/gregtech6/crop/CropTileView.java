package gregtech6.crop;

/**
 * The crop tile state view  --  the ICropTile read/write face over one crop-stick tile,
 * consumed by the pure tick functions in {@link CropTickLogic} (ADR-CB5: growth/weed/
 * trample logic takes a RandomSource + this view; the BE only gathers world inputs).
 *
 * <p><b>OWNERSHIP NOTE (the card seam)</b>: this interface is a MINIMAL LOCAL PLACEHOLDER
 * owned in final form by the parallel card cbc-2 (the ADR-CB2 registry counterpart
 * naming {@code CropTileView}); the wording "aligned when cbc-2 merges" is the card
 * ruling  --  cbc-2's engine replaces this file and the merge queue is cbc-1  ->  cbc-2.
 * Every method below is the 1.12 decompiled {@code ICropTile} face the tick formulas
 * actually read (tmp/harvest/ic2-crops/reference/ic2-source/decompiled/ic2/core/crop/
 * TileEntityCrop.java getter/setter band :560-702).
 */
public interface CropTileView {

	/** The planted card, or null for an empty stick tile (upstream {@code crop}). */
	CropCardView getCrop();

	/** Plants/swaps the card (upstream {@code setCrop}  --  terrain refresh rides the BE shell). */
	void setCrop(CropCardView aCard);

	int getCurrentSize();

	void setCurrentSize(int aSize);

	int getStatGrowth();

	void setStatGrowth(int aGrowth);

	int getStatGain();

	void setStatGain(int aGain);

	int getStatResistance();

	void setStatResistance(int aResistance);

	int getGrowthPoints();

	void setGrowthPoints(int aPoints);

	int getScanLevel();

	void setScanLevel(int aScanLevel);

	int getStorageWater();

	void setStorageWater(int aWater);

	int getStorageNutrients();

	void setStorageNutrients(int aNutrients);

	int getStorageWeedEX();

	void setStorageWeedEX(int aWeedEX);

	int getTerrainHumidity();

	void setTerrainHumidity(int aHumidity);

	int getTerrainNutrients();

	void setTerrainNutrients(int aNutrients);

	int getTerrainAirQuality();

	void setTerrainAirQuality(int aAir);

	boolean isCrossingBase();

	void setCrossingBase(boolean aCrossingBase);

	/** The upstream {@code reset()}  --  wipes to an empty tile (size 1, terrain -1, TileEntityCrop :826-839). */
	void reset();

	/** The dirty flag  ->  block sync (upstream {@code dirty} :55, consumed in updateEntityServer :196-207). */
	void markDirty();
}
