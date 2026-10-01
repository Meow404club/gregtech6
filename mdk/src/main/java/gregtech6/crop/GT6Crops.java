package gregtech6.crop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

/**
 * Port of the IC2 Crops.instance registry face used by GT_BaseCrop (:76-77 registerCrop /
 * registerBaseSeed) — self-owned per ADR-CB2, API shape from decompiled ic2/api/crops/Crops.java:32-39.
 *
 * <p>Cards enumerate in registration order; {@link #WEED} sits at index 0 (seeded here, port of the
 * upstream Crops.weed singleton — CropWeed.java: tier 0, stats {0,0,1,0,5}, attributes {Weed,Bad},
 * maxSize 5, growthDuration 300, not harvestable, no pick). Crossing consumes
 * {@link #crops()} order for its cumulative weight roll, so the order is part of the seeded contract.
 *
 * <p>KJS face: registration defer — binding card declaration (food/cup 家法), no recipe face here.
 */
public class GT6Crops {

	/** The weed card — port of Crops.weed / CropWeed.java. */
	public static final GT6CropCard WEED = new GT6CropCard("weed", "IndustrialCraft²", ItemStack.EMPTY, null, null,
			0, 5, 1, Integer.MAX_VALUE,
			new int[] {0, 0, 1, 0, 5}, new String[] {"Weed", "Bad"}) {
		@Override
		public boolean canBeHarvested(CropTileView aCrop) {
			return false; // CropWeed.java canBeHarvested → false
		}

		@Override
		public int growthDuration() {
			return 300; // CropWeed.java getGrowthDuration → 300
		}

		@Override
		public boolean leftClickPicksSeed() {
			return false; // CropWeed.java onLeftClick → false
		}

		@Override
		public boolean entityTramples() {
			return false; // CropWeed.java onEntityCollision → false
		}
	};

	private static final List<GT6CropCard> CROPS = new ArrayList<>(List.of(WEED));
	/** Base-seed table keyed by seed item — port of Crops.registerBaseSeed/getBaseSeed. */
	private static final Map<ItemKey, BaseSeed> BASE_SEEDS = new HashMap<>();

	private GT6Crops() {}

	/** Port of Crops.registerCrop(Crops.java:32); duplicates keep the first registration. */
	public static boolean registerCrop(GT6CropCard aCard) {
		if (aCard == null || CROPS.contains(aCard)) return false;
		CROPS.add(aCard);
		return true;
	}

	/**
	 * Port of Crops.registerBaseSeed(Crops.java:37) with the upstream parameter order
	 * (stack, crop, size, statGain, statGrowth, statResistance) — GT_BaseCrop.java:77 calls it
	 * (seed, this, 1, 1, 1, 1).
	 */
	public static boolean registerBaseSeed(ItemStack aStack, GT6CropCard aCard, int aSize, int aStatGain, int aStatGrowth, int aStatResistance) {
		if (aStack == null || aStack.isEmpty() || aCard == null) return false;
		BASE_SEEDS.put(new ItemKey(aStack), new BaseSeed(aCard, aSize, aStatGain, aStatGrowth, aStatResistance));
		return true;
	}

	/** Port of Crops.getBaseSeed(Crops.java:39); null when the stack is no base seed. */
	public static BaseSeed getBaseSeed(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		return BASE_SEEDS.get(new ItemKey(aStack));
	}

	/** Port of Crops.getCrops(Crops.java:30) — registration order, WEED first. */
	public static List<GT6CropCard> crops() {
		return Collections.unmodifiableList(CROPS);
	}

	/** The by-name walk (the IC2 getCropCard(String, String) face) — first registration wins. */
	@Nullable
	public static GT6CropCard crop(String aName) {
		if (aName == null) return null;
		for (GT6CropCard tCard : CROPS) {
			if (tCard.name().equals(aName)) return tCard;
		}
		return null;
	}

	/** Port of the IC2 BaseSeed record (ic2/api/crops/BaseSeed.java: crop+size+3 stats). */
	public record BaseSeed(GT6CropCard crop, int size, int statGain, int statGrowth, int statResistance) {}

	/**
	 * ponytail: seed identity = registry item only, components/NBT ignored — the four grain seeds
	 * (IL.Crop_* 12004-12007) and the 59-card seed item are distinct Items; widen to component-aware
	 * keys only if a damage/NBT-based seed item ever registers.
	 */
	private record ItemKey(net.minecraft.world.item.Item item) {
		ItemKey(ItemStack aStack) {
			this(aStack.getItem());
		}
	}
}
