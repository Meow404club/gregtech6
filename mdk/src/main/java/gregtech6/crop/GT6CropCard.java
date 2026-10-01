package gregtech6.crop;

import javax.annotation.Nullable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Port of gregapi/old/GT_BaseCrop.java:40-164 (data + card behavior) as a self-owned class —
 * ADR-CB2: no types from the upstream host mod's packages; the card defaults this port absorbs
 * come from the decompiled crop API referenced below (line anchors per method).
 *
 * <p>ADR-CB4: the upstream growth-speed constructor parameter is dead code (GT_BaseCrop.java:67,
 * assignment commented out, growth duration actually runs on tier*200) and is NOT ported.
 *
 * <p>KJS face: none (pure algorithm/data carrier, no recipe or registry binding on this class).
 * RCON face: none (pure offline domain card).
 */
public class GT6CropCard {

	/** Stat indices, GT_BaseCrop.java:70-74 (chem/food/def/color/weed — CropProperties.java:44-46 same order). */
	public static final int STAT_CHEMICAL = 0, STAT_FOOD = 1, STAT_DEFENSIVE = 2, STAT_COLOR = 3, STAT_WEED = 4;

	private final String mName;
	private final String mDiscoveredBy;
	private final ItemStack mDrop;
	@Nullable
	private final ItemStack[] mSpecialDrops;
	@Nullable
	private final ItemStack mBaseSeed;
	private final int mTier, mMaxSize, mHarvestSize, mAfterHarvestSize;
	private final int[] mStats = new int[5];
	private final String[] mAttributes;

	/**
	 * Canonical constructor, clamp-for-clamp port of GT_BaseCrop.java:58-80:
	 * name lowercased de-spaced (:59), tier forced >= 1 (:65), maxSize forced >= 3 (:66),
	 * harvestSize clamped 2..maxSize (:68), afterHarvestSize clamped 1..maxSize-1 (:69).
	 * Parameter order follows the upstream constructor minus the dead growth-speed parameter (ADR-CB4).
	 */
	public GT6CropCard(String aCropName, String aDiscoveredBy, ItemStack aDrop, @Nullable ItemStack[] aSpecialDrops,
			@Nullable ItemStack aBaseSeed, int aTier, int aMaxSize, int aAfterHarvestSize, int aHarvestSize,
			int aStatChemical, int aStatFood, int aStatDefensive, int aStatColor, int aStatWeed, String[] aAttributes) {
		this(aCropName.toLowerCase().replaceAll(" ", ""), aDiscoveredBy, aDrop, aSpecialDrops, aBaseSeed,
				Math.max(1, aTier), Math.max(3, aMaxSize),
				Math.min(Math.max(aAfterHarvestSize, 1), Math.max(3, aMaxSize) - 1),
				Math.min(Math.max(aHarvestSize, 2), Math.max(3, aMaxSize)),
				new int[] {aStatChemical, aStatFood, aStatDefensive, aStatColor, aStatWeed}, aAttributes);
	}

	/** No-clamp constructor for special cards (the weed card runs tier 0 — CropWeed.java CropProperties(0,...)). */
	protected GT6CropCard(String aName, String aDiscoveredBy, ItemStack aDrop, @Nullable ItemStack[] aSpecialDrops,
			@Nullable ItemStack aBaseSeed, int aTier, int aMaxSize, int aAfterHarvestSize, int aHarvestSize,
			int[] aStats, String[] aAttributes) {
		mName = aName;
		mDiscoveredBy = (aDiscoveredBy != null && !aDiscoveredBy.isEmpty()) ? aDiscoveredBy : "Gregorius Techneticies";
		mDrop = aDrop;
		mSpecialDrops = aSpecialDrops;
		mBaseSeed = aBaseSeed;
		mTier = aTier;
		mMaxSize = aMaxSize;
		mHarvestSize = aHarvestSize;
		mAfterHarvestSize = aAfterHarvestSize;
		mAttributes = aAttributes;
		if (aStats != null) System.arraycopy(aStats, 0, mStats, 0, Math.min(aStats.length, 5));
	}

	/** Port of name() :124-126. */
	public String name() {
		return mName;
	}

	/** Port of discoveredBy() :98-100 (default "Gregorius Techneticies" :41). */
	public String discoveredBy() {
		return mDiscoveredBy;
	}

	/** Port of tier() :129-131. */
	public int tier() {
		return mTier;
	}

	/** Port of maxSize() :134-136. */
	public int maxSize() {
		return mMaxSize;
	}

	/** Harvest threshold, port of canBeHarvested's mHarvestSize :108-110. */
	public int harvestSize() {
		return mHarvestSize;
	}

	/** Port of getSizeAfterHarvest() :88-90. */
	public int sizeAfterHarvest() {
		return mAfterHarvestSize;
	}

	/** Port of stat(int) :118-121 (out of range → 0). */
	public int stat(int aStat) {
		if (aStat < 0 || aStat >= mStats.length) return 0;
		return mStats[aStat];
	}

	/** Port of attributes() :93-95. */
	public String[] attributes() {
		return mAttributes;
	}

	/** The registered base seed stack, null = crossbreed-only (GT_BaseCrop.java:77). */
	@Nullable
	public ItemStack baseSeed() {
		return mBaseSeed;
	}

	// ------------------------------------------------- behavior (ICropTile-facing defaults)

	/** Port of canGrow :103-105 (CropCard.java:76-78 default). */
	public boolean canGrow(CropTileView aCrop) {
		return aCrop.size() < maxSize();
	}

	/** Port of canBeHarvested :108-110 (GT override of CropCard.java:96-98 == maxSize). */
	public boolean canBeHarvested(CropTileView aCrop) {
		return aCrop.size() >= mHarvestSize;
	}

	/** Port of canCross :113-115 (size+2 > maxSize; GT override of CropCard.java:84-86 size>=3). */
	public boolean canCross(CropTileView aCrop) {
		return aCrop.size() + 2 > maxSize();
	}

	/** CropCard.java:72-74 default (GT cards don't override; the weed card does — 300, CropWeed.java). */
	public int growthDuration() {
		return mTier * 200;
	}

	/** CropCard.java:80-82 default (GT cards don't override). */
	public int weightInfluences(int aHumidity, int aNutrients, int aAirQuality) {
		return aHumidity + aNutrients + aAirQuality;
	}

	/** CropCard.java:100-102: 0.95^tier. */
	public double dropGainChance() {
		return Math.pow(0.95, mTier);
	}

	/** CropCard.java:121-136: size 1 → 0; base 0.5 halved at size 2; ×0.8 per tier. */
	public float dropSeedChance(CropTileView aCrop) {
		if (aCrop.size() == 1) return 0.0F;
		float base = 0.5F;
		if (aCrop.size() == 2) base /= 2.0F;
		for (int i = 0; i < mTier; i++) {
			base = (float)(base * 0.8);
		}
		return base;
	}

	/** CropCard.java:167-169: size >= 2 and (the weed card itself or statGrowth >= 24). */
	public boolean isWeed(CropTileView aCrop) {
		return aCrop.size() >= 2 && (this == GT6Crops.WEED || aCrop.statGrowth() >= 24);
	}

	/** CropCard.java:117-119 onLeftClick default true; the weed card blocks picking (CropWeed.java onLeftClick false). */
	public boolean leftClickPicksSeed() {
		return true;
	}

	/**
	 * Port of getGain :139-145 with the RNG injected instead of the upstream global RNGSUS:
	 * specialDrops rolled against a (len+4) range, falling back to the plain drop.
	 */
	public ItemStack pickGain(CropTileView aCrop, RandomSource aRNG) {
		if (mSpecialDrops != null) {
			int tDrop = aRNG.nextInt(mSpecialDrops.length + 4);
			if (tDrop < mSpecialDrops.length && mSpecialDrops[tDrop] != null) return mSpecialDrops[tDrop].copy();
		}
		return mDrop.copy();
	}
}
