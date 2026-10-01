package gregtech6.crop;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * The crop card read face the cbc-1 tick skeleton and interactions need  --  the GT6 1.7.10
 * {@code GT_BaseCrop} + IC2 1.12 {@code CropCard} semantics in port form.
 *
 * <p><b>OWNERSHIP NOTE (the card seam)</b>: the full card/registry face ({@code GT6CropCard},
 * {@code GT6Crops}, attributes/properties, special drops, card hooks) is card cbc-2's deliverable
 * (ADR-CB2 counterparty naming); this minimal face is the cbc-1 base it will implement/align
 * ("aligned when cbc-2 merges"  --  merge queue cbc-1  ->  cbc-2). Every default below is the IC2
 * 1.12 decompiled {@code CropCard} default (ic2/api/crops/CropCard.java :64-176), the ADR-CB1
 * mechanism authority; the GT6 grain rows pin the GT6 overrides (GT_BaseCrop.java :103-115
 * canGrow/canBeHarvested/canCross finals).
 *
 * <p><b>Declared interim</b> (ADR-CB3, reviewed not silent): {@link #seeds} returns the base
 * grain item  --  the stat-carrying seed item form (IC2 ItemCropSeed NBT G/Ga/Re/scan) is card
 * cbc-3's deliverable, so a picked seed replants at base stats until cbc-3 lands.
 */
public interface CropCardView {

	/** The registry id  --  upstream {@code name()} (GT_BaseCrop :59 lowercase-no-spaces law). */
	String name();

	int tier();

	int maxSize();

	/** The harvest threshold  --  GT_BaseCrop :108-110 final (size >= mHarvestSize). */
	int harvestSize();

	/** The post-harvest reset size  --  GT_BaseCrop :88-90 (mAfterHarvestSize, clamped 1..maxSize-1). */
	int afterHarvestSize();

	/** The 5 breeding properties (Chem/Food/Def/Color/Weed)  --  the crossing weights' face (cbc-2's full walk). */
	int[] properties();

	/** The attribute strings  --  the crossing weight +5 pairs (lowercase-insensitive, decompiled :1113-1119). */
	String[] attributes();

	/** IC2 CropCard :68-70 default tier*200. */
	default int growthDuration(CropTileView aCrop) {
		return tier() * 200;
	}

	/** IC2 CropCard :72-74; the GT6 final is the same body (GT_BaseCrop :103-105). */
	default boolean canGrow(CropTileView aCrop) {
		return aCrop.getCurrentSize() < maxSize();
	}

	/** The GT6 final (GT_BaseCrop :108-110); the IC2 default (size == maxSize) is NOT the GT6 face. */
	default boolean canBeHarvested(CropTileView aCrop) {
		return aCrop.getCurrentSize() >= harvestSize();
	}

	/** The GT6 override (GT_BaseCrop :113-115, size+2>maxSize); IC2 default is size >= 3. */
	default boolean canCross(CropTileView aCrop) {
		return aCrop.getCurrentSize() + 2 > maxSize();
	}

	/** IC2 CropCard :76-79 default H+N+A. */
	default int weightInfluences(int aHumidity, int aNutrients, int aAir) {
		return aHumidity + aNutrients + aAir;
	}

	/** IC2 CropCard :100-102 default 0.95^tier. */
	default double dropGainChance() {
		return Math.pow(0.95, tier());
	}

	/** IC2 CropCard :121-137 default: size 1  ->  0, size 2  ->  half, then 0.8^tier. */
	default float dropSeedChance(CropTileView aCrop) {
		if (aCrop.getCurrentSize() == 1) return 0.0F;
		float tBase = 0.5F;
		if (aCrop.getCurrentSize() == 2) tBase /= 2.0F;
		for (int i = 0; i < tier(); i++) tBase = (float) (tBase * 0.8);
		return tBase;
	}

	/** IC2 CropCard :146-152 default size == maxSize; the GT6 final (>= harvestSize) rides {@link #canBeHarvested}. */
	default boolean isWeed(CropTileView aCrop) {
		return aCrop.getCurrentSize() >= 2 && (aCrop.getStatGrowth() >= 24);
	}

	/** The harvest products per drop  --  GT_BaseCrop :139-145 (special-drop lottery rides the card impl; the 59-row census is cbc-3). */
	List<ItemStack> gains(CropTileView aCrop);

	/** The seed carrier  --  see the class-level interim declaration (cbc-3 lands the stat-carrying form). */
	ItemStack seeds(CropTileView aCrop);

	/** The per-cycle card hook (IC2 CropCard :168-170 empty default)  --  cbc-2 wires special crops. */
	default void tick(CropTileView aCrop) {}
}
