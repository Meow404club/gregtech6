package gregtech6.crop;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The 59 GT6 crop-card data rows — task cbc-3-crop-data-assets. Verbatim transcription of the 59
 * {@code new GT_BaseCrop(...)} lines (tmp/gt6-1.7.10/src/main/java/gregtech/compat/
 * Compat_Recipes_IndustrialCraft.java:585-646, 24 material/exotic :585-608, 4 grains :610-613,
 * 27 food :615-641, 4 magic :643-646) into {@link GT6Crops} via the cbc-2 registration face
 * ({@link #ensureRegistered}). The upstream constructor law runs in {@link GT6CropCard} —
 * name lowercased de-spaced (GT_BaseCrop.java:59), tier/maxSize/harvest clamps (:65-69), the
 * null-drop skip (:61: a card whose drop item does not exist registers NOTHING), the base-seed
 * registration {@code (seed, this, 1, 1, 1, 1)} (:77).
 *
 * <p>ADR-CB4: the upstream growth-speed constructor column is DEAD code (GT_BaseCrop.java:67,
 * assignment commented out) and is NOT transcribed. The <code>:58x</code> line anchor on every
 * row is the upstream source line the row walks.
 *
 * <p>Drop/seed item references are SYMBOLIC ({@link Seg}), resolved live through the two family
 * resolver seams (the GT6RecipesCrops shape): prefix×material pairs walk
 * {@link GTMaterialItems} (the plantGt families, dust, nugget), ids walk the item registry (the
 * T5a {@code gt6:food_*} band ids, work/food-crop-items — the same ids GT6RecipesCrops
 * consumes). Rows whose drops fail to resolve stay unregistered by the upstream null-drop law,
 * which is exactly what upstream itself does when the producing mod is absent (the foreign
 * ARS/TC/TF faces: Shimmerleaf's TC base seed and the Liveroots special drop — declared
 * absences, null slots in the rows; the Desert Nova / Cerublossom rows ride the GT6 fallback
 * items, GT6CropFoods meta 12010/12011 — the cbc-6 re-seat).
 *
 * <p>Textures: every card renders crop/&lt;name&gt;/&lt;1..maxSize&gt; block sprites
 * (GT_BaseCrop.java:160-163) — borrowed byte-identical in the assets commit of this card, the
 * README sha256 ledger band "crop stage sprites".
 *
 * <p>KJS face: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java declaration form). RCON face: none (offline registration card).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CropCards {

	/**
	 * One symbolic item reference — the port of an upstream {@code OP.*.mat(MT.*, n)} /
	 * {@code IL.*.get(n)} / {@code ST.make(Items.*, n)} argument as plain data (the
	 * GT6RecipesCrops Slot shape). Exactly one of {prefix×material, id, item} is set.
	 */
	public record Seg(@Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, @Nullable String id,
			@Nullable Item item, int count) {
		/** The OP.x.mat(MT.y, n) form — resolved through {@link #sMaterialItemResolver}. */
		public static Seg mat(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) {
			return new Seg(aPrefix, aMaterial, null, null, aCount);
		}

		/** The IL.x.get(n) form for port/mod ids — resolved through {@link #sIdItemResolver} at resolve time. */
		public static Seg gt(String aId, int aCount) {
			return new Seg(null, null, aId, null, aCount);
		}

		/** The ST.make(Items.x, n) form — vanilla items need no registry lookup. */
		public static Seg van(Item aItem, int aCount) {
			return new Seg(null, null, null, aItem, aCount);
		}
	}

	/**
	 * One crop-card row — the columns of one upstream construction line. {@code afterHarvest}/
	 * {@code harvest} are the raw AH/HA columns (the clamps run in the GT6CropCard canonical
	 * constructor, the GT_BaseCrop.java:68-69 law); the stat columns are CH FD DF CO WD
	 * (GT_BaseCrop.java:70-74 order).
	 */
	public record CropCardRow(int line, String name, String discoverer, Seg drop, @Nullable Seg[] specialDrops,
			@Nullable Seg baseSeed, int tier, int size, int afterHarvest, int harvest,
			int ch, int fd, int df, int co, int wd, String[] attributes) {}

	/**
	 * The 59 rows, upstream line order (the segments: see the class doc for the id universe).
	 * LAZY, never a field initializer — the GTWireSpecs:35 ruling: this class loads at mod
	 * construct, BEFORE MT.init()/OP.init(), and a direct MT.X read in {@code <clinit>} would
	 * resolve null (the GT6RegistryStaticInitGuardTest red line). First callers: the
	 * common-setup registration walk ({@link #onCommonSetup}) and the offline tests after their
	 * {@code GTMaterialItems.initMaterials()} boot — both strictly post-init.
	 * ponytail: single lazy holder; a builder-pattern row table is not worth it at 59 fixed rows.
	 */
	public static List<CropCardRow> rows() {
		List<CropCardRow> tRows = sRows;
		if (tRows == null) sRows = tRows = buildRows();
		return tRows;
	}

	private static volatile List<CropCardRow> sRows;

	private static List<CropCardRow> buildRows() {
		return List.of(
		// ---------------------------------------------------------------- materials & exotics :585-608 (24)
		row( 585, "Indigo"             , "Eloraam"                , Seg.mat(OP.plantGtBlossom, MT.Indigo, 1), null, Seg.mat(OP.plantGtBlossom, MT.Indigo, 4), 2, 4, 1, 4, 1, 1, 0, 4, 0, "Flower", "Color", "Ingredient"),
		row( 586, "Flax"               , "Eloraam"                , Seg.van(Items.STRING, 1), null, null, 2, 4, 1, 4, 1, 1, 2, 0, 1, "Vine", "Silk", "Addictive"),
		row( 587, "Oil Berries"        , "Spacetoad"              , Seg.mat(OP.plantGtBerry, MT.Oil, 1), null, null, 9, 4, 1, 4, 6, 1, 2, 1,12, "Reed", "Fire", "Dark", "Rotten", "Coal", "Oil", "Berry"),
		row( 588, "Bobsyeruncle Ranks" , "GenerikB"               , Seg.mat(OP.plantGtBerry, MT.Emerald, 1), null, null,11, 4, 1, 4, 4, 0, 8, 2, 9, "Vine", "Shiny", "Emerald", "Berylium", "Crystal"),
		row( 589, "Diareed"            , "Direwolf20"             , Seg.mat(OP.dustTiny, MT.Diamond, 1), null, null,12, 4, 1, 4, 5, 0,10, 2,10, "Reed", "Fire", "Shiny", "Coal", "Diamond", "Crystal"),
		row( 590, "Withereed"          , "CovertJaguar"           , Seg.mat(OP.dust, MT.Coal, 1), new Seg[] {Seg.van(Items.COAL, 1), Seg.van(Items.COAL, 1)}, null, 8, 4, 1, 4, 2, 0, 4, 1, 6, "Reed", "Fire", "Undead", "Coal", "Rotten", "Wither"),
		row( 591, "Blaze Reed"         , "Mr. Brain"              , Seg.van(Items.BLAZE_POWDER, 1), new Seg[] {Seg.van(Items.BLAZE_ROD, 1)}, null,10, 4, 1, 4, 4, 0, 8, 2,10, "Reed", "Fire", "Blaze", "Sulfur"),
		row( 592, "Eggplant"           , "Link"                   , Seg.van(Items.EGG, 1), new Seg[] {Seg.van(Items.CHICKEN, 1), Seg.van(Items.FEATHER, 1), Seg.van(Items.FEATHER, 1), Seg.van(Items.FEATHER, 1)}, null, 6, 3, 2, 3, 0, 4, 1, 0, 0, "Flower", "Food", "Chicken", "Egg", "Feather", "Addictive"),
		row( 593, "Corium"             , "Gregorius Techneticies" , Seg.van(Items.LEATHER, 1), null, null, 6, 4, 1, 4, 0, 2, 3, 1, 0, "Vine", "Cow", "Silk"),
		row( 594, "Corpse Plant"       , "Mr. Kenny"              , Seg.van(Items.ROTTEN_FLESH, 1), new Seg[] {Seg.van(Items.BONE_MEAL, 1), Seg.van(Items.BONE_MEAL, 1), Seg.van(Items.BONE, 1)}, null, 5, 4, 1, 4, 0, 2, 1, 0, 3, "Vine", "Food", "Toxic", "Undead", "Rotten"),
		row( 595, "Creeper Weed"       , "General Spaz"           , Seg.mat(OP.dust, MT.Gunpowder, 1), null, null, 7, 4, 1, 4, 3, 0, 5, 1, 3, "Vine", "Creeper", "Explosive", "Fire", "Sulfur", "Saltpeter", "Coal"),
		row( 596, "Ender Bloom"        , "RichardG"               , Seg.mat(OP.dust, MT.EnderPearl, 1), new Seg[] {Seg.van(Items.ENDER_PEARL, 1), Seg.van(Items.ENDER_PEARL, 1), Seg.van(Items.ENDER_EYE, 1)}, null,10, 4, 1, 4, 5, 0, 2, 1, 6, "Flower", "Shiny", "Ender"),
		// :597 dye meta 9 = the 1.7.10 pink dye (the GT6RecipesShCL Dye_Bonemeal identity law shape)
		row( 597, "Meat Rose"          , "VintageBeef"            , Seg.van(Items.PINK_DYE, 1), new Seg[] {Seg.van(Items.BEEF, 1), Seg.van(Items.PORKCHOP, 1), Seg.van(Items.CHICKEN, 1), Seg.van(Items.COD, 1)}, null, 7, 4, 1, 4, 0, 4, 1, 3, 0, "Flower", "Food", "Cow", "Fish", "Chicken", "Pig"),
		row( 598, "Milkwart"           , "Mr. Brain"              , Seg.mat(OP.plantGtWart, MT.Milk, 1), null, Seg.mat(OP.plantGtWart, MT.Milk, 1), 6, 3, 1, 3, 0, 3, 0, 1, 0, "Wart", "Food", "Milk", "Cow", "Ingredient"),
		row( 599, "Glowshrooms"        , "Speiger"                , Seg.mat(OP.plantGtWart, MT.Glowstone, 1), null, Seg.mat(OP.plantGtWart, MT.Glowstone, 1), 6, 3, 1, 3, 5, 0, 0, 2, 0, "Wart", "Shiny", "Ingredient"),
		row( 600, "Slime Plant"        , "Neowulf"                , Seg.van(Items.SLIME_BALL, 1), null, null, 6, 4, 3, 4, 3, 0, 0, 0, 2, "Bush", "Slime", "Bouncy", "Sticky"),
		row( 601, "Spidernip"          , "Ms. Muffet"             , Seg.van(Items.STRING, 1), new Seg[] {Seg.van(Items.SPIDER_EYE, 1), Seg.gt("minecraft:cobweb", 1)}, null, 4, 4, 1, 4, 2, 1, 4, 1, 3, "Flower", "Toxic", "Silk", "Spider", "Ingredient", "Addictive"),
		row( 602, "Tear Stalks"        , "Neowulf"                , Seg.van(Items.GHAST_TEAR, 1), null, null, 8, 4, 1, 4, 1, 2, 0, 0, 0, "Reed", "Healing", "Nether", "Ingredient", "Ghast"),
		row( 603, "Tine"               , "Gregorius Techneticies" , Seg.mat(OP.plantGtTwig, MT.Sn, 1), null, null, 5, 3, 2, 3, 2, 0, 3, 0, 0, "Bush", "Shiny", "Metal", "Pine", "Tin"),
		row( 604, "Coppon"             , "Mr. Brain"              , Seg.mat(OP.plantGtFiber, MT.Cu, 1), null, null, 6, 3, 2, 3, 2, 0, 1, 1, 1, "Bush", "Shiny", "Metal", "Cotton", "Copper"),
		row( 605, "Argentia"           , "Eloraam"                , Seg.mat(OP.plantGtBlossom, MT.Ag, 1), null, null, 7, 4, 3, 4, 2, 0, 1, 0, 0, "Reed", "Shiny", "Metal", "Silver"),
		row( 606, "Plumbilia"          , "KingLemming"            , Seg.mat(OP.plantGtBlossom, MT.Pb, 1), null, null, 6, 4, 3, 4, 2, 0, 3, 1, 1, "Reed", "Heavy", "Metal", "Lead"),
		row( 607, "Steeleaf Ranks"     , "Benimatic"              , Seg.mat(OP.nugget, MT.Steeleaf, 1), new Seg[] {Seg.mat(OP.ingot, MT.Steeleaf, 1)}, null,10, 4, 1, 4, 3, 0, 7, 2, 8, "Vine", "Metal", "Iron"),
		// :608 IL.TF_LiveRoot = the Twilight Forest item, foreign-mod declared absence (null slot, the pickGain null-guard skips it)
		row( 608, "Liveroots"          , "Benimatic"              , Seg.mat(OP.dust, MT.LiveRoot, 1), new Seg[] {null}, null, 8, 4, 1, 4, 2, 0, 5, 2, 6, "Vine", "Wood"),
		// ---------------------------------------------------------------- grains :610-613 (4)
		row( 610, "Rye"                , "Binnie"                 , Seg.gt("gt6:food_crop_rye"   , 1), null, Seg.gt("gt6:food_crop_rye"   , 1), 1, 7, 2, 7, 0, 4, 0, 0, 2, "Wheat", "Food", "Grain"),
		row( 611, "Barley"             , "Glitchfiend"            , Seg.gt("gt6:food_crop_barley", 1), null, Seg.gt("gt6:food_crop_barley", 1), 1, 7, 2, 7, 0, 4, 0, 0, 2, "Wheat", "Food", "Grain"),
		row( 612, "Oats"               , "Pam"                    , Seg.gt("gt6:food_crop_oats"  , 1), null, Seg.gt("gt6:food_crop_oats"  , 1), 1, 7, 2, 7, 0, 4, 0, 0, 2, "Wheat", "Food", "Grain"),
		row( 613, "Rice"               , "Ellpeck"                , Seg.gt("gt6:food_crop_rice"  , 1), null, Seg.gt("gt6:food_crop_rice"  , 1), 1, 7, 2, 7, 0, 4, 0, 0, 2, "Wheat", "Food", "Grain"),
		// ---------------------------------------------------------------- food :615-641 (27)
		row( 615, "Tea"                , "Pam"                    , Seg.mat(OP.plantGtBlossom, MT.Tea, 1), null, Seg.mat(OP.plantGtBlossom, MT.Tea, 4), 2, 4, 1, 4, 2, 4, 2, 0, 1, "Leaves", "Food", "Addictive"),
		row( 616, "Mint"               , "Gregorius Techneticies" , Seg.mat(OP.plantGtBlossom, MT.Mint, 1), null, Seg.mat(OP.plantGtBlossom, MT.Mint, 4), 2, 4, 1, 4, 2, 3, 5, 0, 1, "Leaves", "Food", "Fresh"),
		row( 617, "Lemon Plant"        , "Cave Johnson"           , Seg.gt("gt6:food_lemon", 1), null, Seg.gt("gt6:food_lemon", 4), 3, 4, 3, 4, 3, 4, 3, 1, 2, "Bush", "Food", "Fruit", "Explosive"),
		// :618-621 Food_Apple_Red = vanilla apple (the GT6CropFoods alias ruling), the other three are T5a ids
		row( 618, "Green Apple Tree"   , "The Guy"                , Seg.gt("gt6:food_apple_green", 1), null, Seg.gt("gt6:food_apple_green", 4), 3, 4, 3, 4, 3, 3, 5, 1, 2, "Bush", "Food", "Fruit", "Apple"),
		row( 619, "Yellow Apple Tree"  , "The Guy"                , Seg.gt("gt6:food_apple_yellow", 1), null, Seg.gt("gt6:food_apple_yellow", 4), 3, 4, 3, 4, 2, 4, 4, 1, 2, "Bush", "Food", "Fruit", "Apple"),
		row( 620, "Red Apple Tree"     , "The Guy"                , Seg.van(Items.APPLE, 1), null, Seg.van(Items.APPLE, 4), 3, 4, 3, 4, 2, 4, 3, 1, 2, "Bush", "Food", "Fruit", "Apple"),
		row( 621, "Dark Red Apple Tree", "The Guy"                , Seg.gt("gt6:food_apple_darkred", 1), null, Seg.gt("gt6:food_apple_darkred", 4), 3, 4, 3, 4, 2, 5, 2, 2, 3, "Bush", "Food", "Fruit", "Apple"),
		row( 622, "Chili"              , "Gregorius Techneticies" , Seg.gt("gt6:food_chili_pepper", 1), null, Seg.gt("gt6:food_chili_pepper", 4), 3, 4, 3, 4, 4, 4, 6, 2, 1, "Vine", "Food", "Fruit", "Fire"),
		row( 623, "Tomato Plant"       , "Kirby"                  , Seg.gt("gt6:food_tomato", 1), null, Seg.gt("gt6:food_tomato", 4), 2, 4, 3, 4, 2, 4, 2, 1, 3, "Vine", "Food", "Fruit", "Vegetable"),
		row( 624, "Red Grapes"         , "Pam"                    , Seg.gt("gt6:food_grapes_red", 1), null, Seg.gt("gt6:food_grapes_red", 4), 3, 4, 3, 4, 1, 4, 0, 2, 3, "Vine", "Food", "Fruit"),
		row( 625, "White Grapes"       , "Binnie"                 , Seg.gt("gt6:food_grapes_white", 1), null, Seg.gt("gt6:food_grapes_white", 4), 3, 4, 3, 4, 1, 4, 0, 0, 3, "Vine", "Food", "Fruit"),
		row( 626, "Green Grapes"       , "Gwafu"                  , Seg.gt("gt6:food_grapes_green", 1), null, Seg.gt("gt6:food_grapes_green", 4), 3, 4, 3, 4, 1, 4, 0, 1, 3, "Vine", "Food", "Fruit"),
		// :627 getWithName(1, "Member Berries") — the display-override face stays pooled (a plain stack here)
		row( 627, "Purple Grapes"      , "Gregorius Techneticies" , Seg.gt("gt6:food_grapes_purple", 1), new Seg[] {Seg.gt("gt6:food_grapes_purple", 1)}, Seg.gt("gt6:food_grapes_purple", 4), 3, 4, 3, 4, 1, 4, 0, 2, 3, "Vine", "Food", "Fruit", "Member?"),
		row( 628, "Blueberry Bush"     , "Pam"                    , Seg.gt("gt6:food_blueberry", 1), null, Seg.gt("gt6:food_blueberry", 4), 3, 5, 4, 5, 1, 4, 1, 4, 2, "Bush", "Food", "Fruit", "Berry", "Color"),
		row( 629, "Gooseberry Bush"    , "Pam"                    , Seg.gt("gt6:food_gooseberry", 1), null, Seg.gt("gt6:food_gooseberry", 4), 3, 5, 4, 5, 1, 4, 1, 1, 2, "Bush", "Food", "Fruit", "Berry"),
		row( 630, "Candleberry Bush"   , "Pam"                    , Seg.gt("gt6:food_candleberry", 1), null, Seg.gt("gt6:food_candleberry", 4), 3, 5, 4, 5, 3, 3, 1, 0, 2, "Bush", "Food", "Fruit", "Berry", "Wax"),
		row( 631, "Cranberries"        , "Pam"                    , Seg.gt("gt6:food_cranberry", 1), null, Seg.gt("gt6:food_cranberry", 4), 3, 4, 3, 4, 1, 4, 0, 2, 3, "Vine", "Food", "Fruit", "Berry"),
		row( 632, "Black Currants"     , "Gregorius Techneticies" , Seg.gt("gt6:food_currants_black", 1), null, Seg.gt("gt6:food_currants_black", 4), 3, 5, 4, 5, 1, 4, 1, 1, 2, "Bush", "Food", "Fruit", "Berry"),
		row( 633, "White Currants"     , "Gregorius Techneticies" , Seg.gt("gt6:food_currants_white", 1), null, Seg.gt("gt6:food_currants_white", 4), 3, 5, 4, 5, 1, 4, 1, 0, 2, "Bush", "Food", "Fruit", "Berry"),
		row( 634, "Red Currants"       , "Gregorius Techneticies" , Seg.gt("gt6:food_currants_red", 1), null, Seg.gt("gt6:food_currants_red", 4), 3, 5, 4, 5, 1, 4, 1, 2, 2, "Bush", "Food", "Fruit", "Berry"),
		row( 635, "Blackberries"       , "Pam"                    , Seg.gt("gt6:food_blackberry", 1), null, Seg.gt("gt6:food_blackberry", 4), 3, 4, 3, 4, 1, 4, 4, 2, 3, "Vine", "Food", "Fruit", "Berry"),
		row( 636, "Raspberries"        , "Pam"                    , Seg.gt("gt6:food_raspberry", 1), null, Seg.gt("gt6:food_raspberry", 4), 3, 4, 3, 4, 1, 4, 1, 2, 3, "Vine", "Food", "Fruit", "Berry"),
		row( 637, "Strawberries"       , "Pam"                    , Seg.gt("gt6:food_strawberry", 1), null, Seg.gt("gt6:food_strawberry", 4), 3, 4, 1, 4, 1, 4, 0, 2, 4, "Bush", "Food", "Fruit", "Berry"),
		row( 638, "Onion"              , "Onion San"              , Seg.gt("gt6:food_onion", 1), null, Seg.gt("gt6:food_onion", 4), 2, 4, 1, 4, 3, 3, 3, 0, 1, "Vegetable", "Food", "Ingredient"),
		row( 639, "Cucumber"           , "Pam"                    , Seg.gt("gt6:food_cucumber", 1), null, Seg.gt("gt6:food_cucumber", 4), 2, 4, 1, 4, 1, 5, 0, 0, 2, "Vegetable", "Food", "Ingredient"),
		row( 640, "Peanuts"            , "Snoopy"                 , Seg.gt("gt6:food_peanut", 1), null, Seg.gt("gt6:food_peanut", 4), 3, 4, 1, 4, 1, 4, 0, 2, 4, "Bush", "Food", "Nut"),
		row( 641, "Ananas"             , "Spongebob"              , Seg.gt("gt6:food_ananas", 1), null, Seg.gt("gt6:food_ananas", 4), 4, 3, 1, 3, 3, 3, 5, 1, 1, "Bush", "Food", "Fruit", "Pine", "Apple"),
		// ---------------------------------------------------------------- magic :643-646 (4)
		// :643-644 the drop was IL.ARS_* (foreign) with the IL.DesertNova/Cerublossom GT6
		// fallback — the fallback faces re-seated by task cbc-6-crop-test-faces onto the
		// T5a-band items (GT6CropFoods meta 12010/12011, MultiItemFood.java:98-99): the
		// drop rides the ARS count 1 and the base seed the ARS count 4, both of the GT6
		// fallback item (the upstream get(1, fallback)/get(4, fallback) shape). The
		// Behavior_Turn_Into swap face stays pooled (ARS foreign, the declared absence).
		// The icons are the cbc-6 composed placeholders (assets/README.md).
		row( 643, "Desert Nova"        , "Mithion"                , Seg.gt("gt6:food_desertnova"  , 1), null, Seg.gt("gt6:food_desertnova"  , 4), 6, 4, 1, 4, 5, 1, 7, 4,10, "Cactus", "Magic", "Fire", "Explosive"),
		row( 644, "Cerublossom"        , "Mithion"                , Seg.gt("gt6:food_cerublossom" , 1), null, Seg.gt("gt6:food_cerublossom" , 4), 6, 4, 1, 4, 1, 1, 2, 4,10, "Flower", "Magic", "Shiny"),
		row( 645, "Shimmerleaf"        , "Azanor"                 , Seg.mat(OP.chunkGt, MT.Hg, 1), null, null,11, 4, 1, 4, 5, 1, 4, 1, 8, "Flower", "Magic", "Shiny", "Metal", "Mercury"),
		row( 646, "Cinderpearl"        , "Azanor"                 , Seg.van(Items.BLAZE_POWDER, 1), null, null, 8, 4, 1, 4, 3, 1, 8, 2, 8, "Flower", "Magic", "Fire", "Blaze", "Sulfur", "Ingredient"));
	}

	/** The (prefix, material) item seam: the live default is the GTMaterialItems walk (the Mixer resolver share). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6CropCards::resolveMaterialItem;
	/** The registry-id item seam ("gt6:food_crop_rye") — the GT6RecipesCrops sIdItemResolver shape. */
	static Function<String, Item> sIdItemResolver = GT6CropCards::resolveItemById;

	private static boolean sRegistered = false;

	private GT6CropCards() {}

	/** The compact row builder — the attributes ride the varargs tail. */
	private static CropCardRow row(int aLine, String aName, String aDiscoverer, Seg aDrop, @Nullable Seg[] aSpecial,
			@Nullable Seg aBaseSeed, int aTier, int aSize, int aAfterHarvest, int aHarvest,
			int aCH, int aFD, int aDF, int aCO, int aWD, String... aAttributes) {
		return new CropCardRow(aLine, aName, aDiscoverer, aDrop, aSpecial, aBaseSeed,
				aTier, aSize, aAfterHarvest, aHarvest, aCH, aFD, aDF, aCO, aWD, aAttributes);
	}

	/** The live material-item lookup ({@link GTMaterialItems#get}) — null when the pair has no item (the Mixer shape). */
	@Nullable
	private static Item resolveMaterialItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** The registry-id lookup — null when unregistered (containsKey/get, never the AIR fallthrough). */
	@Nullable
	private static Item resolveItemById(String aId) {
		ResourceLocation tKey = new ResourceLocation(aId);
		//? if forge {
		if (!ForgeRegistries.ITEMS.containsKey(tKey)) return null;
		Item tItem = ForgeRegistries.ITEMS.getValue(tKey);
		//?} else {
		/*Item tItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(tKey); // 1.21.1: vanilla get is null when absent
		 *///?}
		return tItem == null || tItem == Items.AIR ? null : tItem;
	}

	/** Seg → stack, or null when the reference does not resolve (the upstream silent-drop shape). */
	@Nullable
	public static ItemStack resolve(@Nullable Seg aSeg) {
		if (aSeg == null) return null;
		if (aSeg.item() != null) return new ItemStack(aSeg.item(), aSeg.count());
		Item tItem = aSeg.id() != null ? sIdItemResolver.apply(aSeg.id())
				: sMaterialItemResolver.apply(aSeg.prefix(), aSeg.material());
		return tItem == null ? null : new ItemStack(tItem, aSeg.count());
	}

	/**
	 * Row → card, the GT_BaseCrop.java:58-80 construction law: null drop = no card (the :61
	 * skip), everything else rides the {@link GT6CropCard} clamping constructor. Test-visible
	 * WITHOUT the global registration side effect.
	 */
	@Nullable
	public static GT6CropCard card(CropCardRow aRow) {
		ItemStack tDrop = resolve(aRow.drop());
		if (tDrop == null) return null;
		ItemStack[] tSpecial = null;
		if (aRow.specialDrops() != null) {
			tSpecial = new ItemStack[aRow.specialDrops().length];
			for (int i = 0; i < tSpecial.length; i++) tSpecial[i] = resolve(aRow.specialDrops()[i]);
		}
		return new GT6CropCard(aRow.name(), aRow.discoverer(), tDrop, tSpecial, resolve(aRow.baseSeed()),
				aRow.tier(), aRow.size(), aRow.afterHarvest(), aRow.harvest(),
				aRow.ch(), aRow.fd(), aRow.df(), aRow.co(), aRow.wd(), aRow.attributes());
	}

	/**
	 * The registration walk into {@link GT6Crops} — idempotent, the seed seat is the common-setup
	 * enqueueWork (post-registration, pre-world; upstream registered during its init phase).
	 * Skipped rows log once and stay skipped (the upstream null-drop law is environment-faithful).
	 */
	public static synchronized void ensureRegistered() {
		if (sRegistered) return;
		int tLive = 0, tSeeded = 0;
		for (CropCardRow tRow : rows()) {
			GT6CropCard tCard = card(tRow);
			if (tCard == null) {
				GT6Mod.LOGGER.warn("GT6 crop card {} skipped: drop item absent in this environment (GT_BaseCrop.java:61 law, upstream :{})",
						tRow.name(), tRow.line());
				continue;
			}
			GT6Crops.registerCrop(tCard);
			tLive++;
			ItemStack tSeed = tCard.baseSeed();
			if (tSeed != null && !tSeed.isEmpty()) {
				GT6Crops.registerBaseSeed(tSeed, tCard, 1, 1, 1, 1); // GT_BaseCrop.java:77 verbatim
				tSeeded++;
			}
		}
		sRegistered = true;
		GT6Mod.LOGGER.info("GT6 crop cards registered: {}/{} rows live, {} base seeds (upstream :585-646)", tLive, rows().size(), tSeeded);
	}

	/** Registration smoke evidence, the GT6Foods onCommonSetup seat. */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6CropCards::ensureRegistered);
	}
}
