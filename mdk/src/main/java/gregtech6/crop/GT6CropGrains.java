package gregtech6.crop;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.registries.RegistryObject;

import gregtech6.registry.GT6CropFoods;

/**
 * The four GT6 grain crops + the base-seed/crop registry seams (card cbc-1-cropstick-base).
 *
 * <p><b>Data rows</b>  --  the Compat_Recipes_IndustrialCraft.java:610-613 verbatim walk (the
 * ADR-CB1 dual-source anchors): Rye "Binnie" / Barley "Glitchfiend" / Oats "Pam" /
 * Rice "Ellpeck", all TIER 1, SIZE 7, growthSpeed 0 (the ADR-CB4 dead column  --  not ported),
 * AH 2, HS 7, stats CH 0 / FD 4 / DF 0 / CO 0 / WD 2, attributes Wheat+Food+Grain. Drop and
 * base seed are BOTH the grain food item itself ({@code IL.Crop_*}  --  the GT6 1.7.10
 * registerBaseSeed(form, size 1, G 1, Ga 1, Re 1) shape, GT_BaseCrop.java:77), carried by
 * the {@link GT6CropFoods#PLAIN_ROWS} band items (food-crop-items T5a).
 *
 * <p><b>Registry seams</b>  --  the minimal cbc-1 form of the IC2 {@code Crops.instance}
 * counterpart; card cbc-2 owns the full registry ({@code GT6Crops}  --  ADR-CB2 naming) and
 * migrates these maps on merge (merge queue cbc-1  ->  cbc-2, "aligned when cbc-2 merges").
 * Both maps key on registry KEYS (not live item references)  --  the lazy-safe face that reads
 * the same offline (unbound keys simply answer null/AIR, the fixture-test posture).
 */
public final class GT6CropGrains {

	/** The GT_BaseCrop :58-80 construction as data  --  the 59-row census (cbc-3) folds this band. */
	public static class GrainCard implements CropCardView {

		private final String mName;
		private final String mDiscoveredBy;
		private final String mItemId; // the GT6CropFoods row id  --  the drop/seed carrier binding
		private final int mTier;
		private final int mMaxSize;
		private final int mAfterHarvestSize;
		private final int mHarvestSize;
		private final int[] mStats; // Chem, Food, Def, Color, Weed
		private final String[] mAttributes;

		GrainCard(String aName, String aDiscoveredBy, String aItemId,
				int aTier, int aMaxSize, int aAfterHarvestSize, int aHarvestSize,
				int[] aStats, String[] aAttributes) {
			mName = aName;
			mDiscoveredBy = aDiscoveredBy;
			mItemId = aItemId;
			mTier = aTier;
			mMaxSize = aMaxSize;
			mAfterHarvestSize = aAfterHarvestSize;
			mHarvestSize = aHarvestSize;
			mStats = aStats;
			mAttributes = aAttributes;
		}

		@Override public String name() { return mName; }
		@Override public int tier() { return mTier; }
		@Override public int maxSize() { return mMaxSize; }
		@Override public int harvestSize() { return mHarvestSize; }
		@Override public int afterHarvestSize() { return mAfterHarvestSize; }
		@Override public int[] properties() { return mStats; }
		@Override public String[] attributes() { return mAttributes; }

		/** The drop/seed carrier  --  the T5a row item (live; the offline fixtures override the stack faces). */
		private Item item() {
			for (int i = 0; i < GT6CropFoods.PLAIN_ROWS.size(); i++) {
				if (GT6CropFoods.PLAIN_ROWS.get(i).id().equals(mItemId)) return GT6CropFoods.PLAINS.get(i).get();
			}
			throw new IllegalStateException("GT6CropFoods lost the grain row " + mItemId);
		}

		@Override
		public List<ItemStack> gains(CropTileView aCrop) {
			return List.of(new ItemStack(item())); // the :610-613 drop column, 1 item
		}

		@Override
		public ItemStack seeds(CropTileView aCrop) {
			return new ItemStack(item()); // the declared interim (CropCardView class doc)
		}

		/** The discovery credit  --  the CropCard face, unused by the tick formulas. */
		public String discoveredBy() { return mDiscoveredBy; }
	}

	/** The IC2 {@code BaseSeed} record form (crop + size + G/Ga/Re  --  GT_BaseCrop.java:77). */
	public record BaseSeed(CropCardView card, int size, int statGrowth, int statGain, int statResistance) {}

	// the :610-613 rows
	public static final CropCardView RYE = new GrainCard("rye", "Binnie", "food_crop_rye",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});
	public static final CropCardView BARLEY = new GrainCard("barley", "Glitchfiend", "food_crop_barley",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});
	public static final CropCardView OATS = new GrainCard("oats", "Pam", "food_crop_oats",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});
	public static final CropCardView RICE = new GrainCard("rice", "Ellpeck", "food_crop_rice",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});

	// -------------------------------------------------------------- the registry seams

	/** The item-vs-table binding census (the test face): grain name  ->  its GT6CropFoods row id. */
	public static final Map<String, String> GRAIN_ITEM_IDS = Map.of(
			"rye", "food_crop_rye",
			"barley", "food_crop_barley",
			"oats", "food_crop_oats",
			"rice", "food_crop_rice");


	private static final Map<String, CropCardView> CROPS = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, BaseSeed> BASE_SEEDS = new ConcurrentHashMap<>();

	static {
		registerCrop(RYE);
		registerCrop(BARLEY);
		registerCrop(OATS);
		registerCrop(RICE);
		// the GT_BaseCrop.java:77 default-seed walk  --  grain item itself, size 1, G/Ga/Re = 1/1/1
		registerBaseSeed(RYE);
		registerBaseSeed(BARLEY);
		registerBaseSeed(OATS);
		registerBaseSeed(RICE);
	}

	/** The card walk  --  the {@code Crops.instance.getCrops()} minimal face (cbc-2 migrates). */
	public static java.util.Collection<CropCardView> crops() {
		return java.util.Collections.unmodifiableCollection(CROPS.values());
	}

	@Nullable
	public static CropCardView crop(String aName) {
		return CROPS.get(aName);
	}

	/** The {@code registerCrop} seam (GT_BaseCrop.java:76)  --  cbc-2's GT6Crops migrates this. */
	public static void registerCrop(CropCardView aCard) {
		CROPS.put(aCard.name(), aCard);
	}

	/**
	 * The {@code registerBaseSeed(stack, crop, 1, 1, 1, 1)} seam, key-keyed (the class doc)  -- 
	 * cbc-2's item-stack-keyed registry replaces it.
	 */
	public static void registerBaseSeed(CropCardView aCard) {
		String tItemId = grainItemId(aCard.name()); // the local keeps the 2-arg ctor swap-matchable on 21.1
		BASE_SEEDS.put(new ResourceLocation("gt6", tItemId), new BaseSeed(aCard, 1, 1, 1, 1));
	}

	/** The {@code Crops.instance.getBaseSeed(stack)} face  --  null when the item plants nothing. */
	@Nullable
	public static BaseSeed baseSeed(ItemStack aStack) {
		if (aStack.isEmpty()) return null;
		ResourceLocation tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		return tKey == null ? null : BASE_SEEDS.get(tKey);
	}

	private static String grainItemId(String aGrainName) {
		String tId = GRAIN_ITEM_IDS.get(aGrainName);
		if (tId == null) throw new IllegalStateException("no grain item binding for " + aGrainName);
		return tId;
	}

	private GT6CropGrains() {}
}
