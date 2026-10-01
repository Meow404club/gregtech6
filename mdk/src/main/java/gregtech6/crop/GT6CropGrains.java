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
 * The four GT6 grain crops + the base-seed binding seam (card cbc-1-cropstick-base; the
 * cbc-2 merge folded the card body into {@link GT6CropCard} and the registry into
 * {@link GT6Crops} — this class keeps only the grain-band DATA rows and the item binding
 * the full 59-row table (cbc-3) will fold last).
 *
 * <p><b>Data rows</b>  --  the Compat_Recipes_IndustrialCraft.java:610-613 verbatim walk (the
 * ADR-CB1 dual-source anchors): Rye "Binnie" / Barley "Glitchfiend" / Oats "Pam" /
 * Rice "Ellpeck", all TIER 1, SIZE 7, the dead growth-speed column unported (ADR-CB4),
 * AH 2, HS 7, stats CH 0 / FD 4 / DF 0 / CO 0 / WD 2, attributes Wheat+Food+Grain. Drop and
 * base seed are BOTH the grain food item itself ({@code IL.Crop_*}  --  the GT6 1.7.10
 * registerBaseSeed(form, size 1, G 1, Ga 1, Re 1) shape, GT_BaseCrop.java:77), carried by
 * the {@link GT6CropFoods#PLAIN_ROWS} band items (food-crop-items T5a).
 *
 * <p><b>The lazy-safe item face</b>  --  the drop/seed stacks resolve LIVE through
 * {@code item()} (the registry-KEY face, unbound keys answer through the offline fixtures);
 * {@link #baseSeed(ItemStack)} keys on registry KEYS, not live item references  --  the same
 * face that reads identically offline (the fixture-test posture). The item-stack-keyed
 * {@link GT6Crops#getBaseSeed} table is the cbc-3 registry fold.
 */
public final class GT6CropGrains {

	/** The GT_BaseCrop :58-80 construction as data  --  the 59-row census (cbc-3) folds this band. */
	public static class GrainCard extends GT6CropCard {

		private final String mItemId; // the GT6CropFoods row id  --  the drop/seed carrier binding

		GrainCard(String aName, String aDiscoveredBy, String aItemId,
				int aTier, int aMaxSize, int aAfterHarvestSize, int aHarvestSize,
				int[] aStats, String[] aAttributes) {
			// the protected no-clamp ctor: these rows are clamp-legal as written (tier 1, size 7,
			// HS 7 <= size, AH 2 <= size-1) and the drop resolves live, not at construction
			super(aName, aDiscoveredBy, ItemStack.EMPTY, null, null,
					aTier, aMaxSize, aAfterHarvestSize, aHarvestSize, aStats, aAttributes);
			mItemId = aItemId;
		}

		/** The drop/seed carrier  --  the T5a row item (live; the offline fixtures override the stack faces). */
		private Item item() {
			for (int i = 0; i < GT6CropFoods.PLAIN_ROWS.size(); i++) {
				if (GT6CropFoods.PLAIN_ROWS.get(i).id().equals(mItemId)) return GT6CropFoods.PLAINS.get(i).get();
			}
			throw new IllegalStateException("GT6CropFoods lost the grain row " + mItemId);
		}

		/** The drop  --  the :610-613 drop column, 1 item. */
		@Override
		public ItemStack pickGain(CropTileView aCrop, net.minecraft.util.RandomSource aRNG) {
			return new ItemStack(item());
		}

		// the picked seed rides the default seedStack face: a stat-carrying gt6:crop_seed live
		// (the IC2 CropCard.getSeeds default), the grain-item base-seed copy offline
	}

	// the :610-613 rows
	public static final GT6CropCard RYE = new GrainCard("rye", "Binnie", "food_crop_rye",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});
	public static final GT6CropCard BARLEY = new GrainCard("barley", "Glitchfiend", "food_crop_barley",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});
	public static final GT6CropCard OATS = new GrainCard("oats", "Pam", "food_crop_oats",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});
	public static final GT6CropCard RICE = new GrainCard("rice", "Ellpeck", "food_crop_rice",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"});

	// -------------------------------------------------------------- the registry seams

	/** The item-vs-table binding census (the test face): grain name  ->  its GT6CropFoods row id. */
	public static final Map<String, String> GRAIN_ITEM_IDS = Map.of(
			"rye", "food_crop_rye",
			"barley", "food_crop_barley",
			"oats", "food_crop_oats",
			"rice", "food_crop_rice");

	private static final Map<String, GT6CropCard> CROPS = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, GT6Crops.BaseSeed> BASE_SEEDS = new ConcurrentHashMap<>();

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

	/** The card walk  --  the {@code Crops.instance.getCrops()} grain-band face (cbc-3 folds). */
	public static java.util.Collection<GT6CropCard> crops() {
		return java.util.Collections.unmodifiableCollection(CROPS.values());
	}

	/** The grain-band card lookup  --  {@link GT6Crops#crop} is the full-registry walk. */
	@Nullable
	public static GT6CropCard crop(String aName) {
		return CROPS.get(aName);
	}

	/**
	 * The {@code registerCrop} seam (GT_BaseCrop.java:76)  --  the LOCAL grain-band map only
	 * since the cbc-3 merge: the GT6Crops registry is owned by the 59-row
	 * {@link GT6CropCards#ensureRegistered} walk (the four grain rows included), so a dual
	 * registration here would shadow them with duplicates.
	 */
	public static void registerCrop(GT6CropCard aCard) {
		CROPS.put(aCard.name(), aCard);
	}

	/**
	 * The {@code registerBaseSeed(stack, crop, 1, 1, 1, 1)} seam, key-keyed (the class doc)  --
	 * cbc-3's item-stack-keyed {@link GT6Crops} table replaces it.
	 */
	public static void registerBaseSeed(GT6CropCard aCard) {
		String tItemId = grainItemId(aCard.name()); // the local keeps the 2-arg ctor swap-matchable on 21.1
		BASE_SEEDS.put(new ResourceLocation("gt6", tItemId), new GT6Crops.BaseSeed(aCard, 1, 1, 1, 1));
	}

	/** The {@code Crops.instance.getBaseSeed(stack)} face  --  null when the item plants nothing. */
	@Nullable
	public static GT6Crops.BaseSeed baseSeed(ItemStack aStack) {
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
