package gregtech6.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6CropFoods;
import gregtech6.registry.GT6CropSticks;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The cbc-1-cropstick-base offline suite  --  every mechanism pin rides the ADR-CB5 pure
 * functions with an injected seeded {@link RandomSource}, the upstream anchors double-marked
 * (decompiled TileEntityCrop.java line + GT_BaseCrop.java line per the card discipline):
 * growth formula (:285-318), the 256t cadence (:56, :186-208), the terrain three
 * (:503-532/:534-546/:548-557 + IC2Crops :131-143), the weed self-gen Weed-EX suppression
 * (:240-252), harvest (:793-823), pick (:731-776), trample (:485-499 + CropCard :166),
 * crossing/canCross (GT_BaseCrop :113-115), the grain rows (Compat_Recipes_IndustrialCraft
 * :610-613), and the NBT carrier (:91-135).
 *
 * <p>The stick item faces ride the registry KEY (the BE lazy-safe posture): the forge leg
 * registers a fixture under gt6:crop_stick through the item latch, the FML leg uses the real
 * registration. The four-grain LIVE-item face (GT6CropFoods RegistryObject stacks) is the
 * FML-leg + RCON chain  --  on this bootstrapped-frozen JVM the mod items are not constructible,
 * so the formula pins ride vanilla-wheat fixture cards and the grain items pin at the table
 * level (the GT6CropFoodsTest posture).
 */
public class CropBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6CropBlockEntity> sBeType;
	static GT6CropSticksBlock sBlock;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	// ---------------------------------------------------------------- fixtures

	/** The grain-shaped fixture card  --  vanilla wheat stacks (offline-constructible). */
	static final GT6CropCard WHEATISH = new GT6CropGrains.GrainCard("rye", "Binnie", "food_crop_rye",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"}) {
		@Override
		public ItemStack pickGain(CropTileView aCrop, RandomSource aRNG) {
			return new ItemStack(Items.WHEAT);
		}

		@Override
		public ItemStack seedStack(int aGrowth, int aGain, int aResistance, int aScan) {
			return new ItemStack(Items.WHEAT_SEEDS); // the offline stand-in for the gt6:crop_seed payload
		}
	};

	/** The kill-arm fixture: tier 10  ->  aux 144 > 100 vs the death roll. */
	static final GT6CropCard DEADLY = new GT6CropGrains.GrainCard("deadly", "test", "food_crop_rye",
			10, 7, 2, 7, new int[] {0, 0, 0, 0, 0}, new String[0]) {
		@Override
		public ItemStack pickGain(CropTileView aCrop, RandomSource aRNG) {
			return new ItemStack(Items.WHEAT);
		}

		@Override
		public ItemStack seedStack(int aGrowth, int aGain, int aResistance, int aScan) {
			return new ItemStack(Items.WHEAT_SEEDS); // the offline stand-in for the gt6:crop_seed payload
		}
	};

	//? if forge {
	private static Item sStickFixture;
	private static Item sGrainFixture;
	//?}

	/** The stick item -- the fixture under gt6:crop_stick (forge latch) / the real registration (FML leg). */
	static Item stickItem() {
		//? if forge {
		if (sStickFixture == null) sStickFixture = registerItemFixture("crop_stick", () -> new Item(new Item.Properties()));
		return sStickFixture;
		//?} else {
		/*return GT6CropSticks.CROP_STICK_ITEM.get(); // the FML leg boots with live registries
		 *///?}
	}

	/** The grain item -- the fixture under gt6:food_crop_rye (the bound base-seed key) / the real rye row. */
	static Item grainItem() {
		//? if forge {
		if (sGrainFixture == null) sGrainFixture = registerItemFixture("food_crop_rye", () -> new Item(new Item.Properties()));
		return sGrainFixture;
		//?} else {
		/*for (int i = 0; i < GT6CropFoods.PLAIN_ROWS.size(); i++) {
			if (GT6CropFoods.PLAIN_ROWS.get(i).id().equals("food_crop_rye")) return GT6CropFoods.PLAINS.get(i).get();
		}
		throw new IllegalStateException("the rye row vanished");
		 *///?}
	}

	@BeforeAll
	static void buildOfflineFixtures() {
		//? if forge {
		seedFluidTypeSize(); // the bare-JUnit leg: the Entity ctor's FluidType read dead-headed (the GTEntityBlockInventoryDropTest seam)
		//?}
		GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		// the crop registry rides the fixture cards offline (the live registry is
		// GT6CropCards.ensureRegistered's 59-row walk; the NBT load face needs cropId=rye here)
		GT6Crops.registerCrop(WHEATISH);
		GT6Crops.registerCrop(DEADLY);
		sBlock = block();
		sBeType = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6CropBlockEntity(sBeType, aPos, aState), sBlock).build(null);
	}

	/** Offline Block construction under the temporarily-unfrozen block registry (the GTWireContactDamageTest form). */
	private static GT6CropSticksBlock block() {
		try {
			java.lang.reflect.Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		return new GT6CropSticksBlock(GT6CropSticksBlock.cropProperties());
	}

	/**
	 * The map-backed world double: controllable states, biome holder, sky flag, a seeded
	 * shared random (the tick shell draws from it), entity capture for the drop faces.
	 */
	static class CropLevel extends gregtech6.recipes.GTRecipesOfflineTestBase.MinimalLevel {
		final Map<BlockPos, BlockState> mStates = new HashMap<>();
		final List<ItemEntity> mDrops = new ArrayList<>();
		final RandomSource mRandom = RandomSource.create(42);
		Holder<Biome> mBiome = scriptedBiome(Biomes.PLAINS, 0.4F, 0.8F); // the plains-arm climate
		boolean mSky = true;

		/**
		 * A real Biome built offline (the javap-verified builder chain -- 1.20.1 leg jar:
		 * BiomeSpecialEffects.Builder needs the four colors, MobSpawnSettings.EMPTY is static,
		 * BiomeGenerationSettings.PlainBuilder builds empty) wrapped in the keyed synthetic
		 * holder (the MinimalLevel.dimensionHolder anonymous-impl form).
		 */
		static Holder<Biome> scriptedBiome(ResourceKey<Biome> aKey, float aDownfall, float aTemperature) {
			Biome tBiome = new Biome.BiomeBuilder()
					.hasPrecipitation(true).temperature(aTemperature).downfall(aDownfall)
					.specialEffects(new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder()
							.fogColor(0).waterColor(0).waterFogColor(0).skyColor(0).build())
					.mobSpawnSettings(net.minecraft.world.level.biome.MobSpawnSettings.EMPTY)
					.generationSettings(new net.minecraft.world.level.biome.BiomeGenerationSettings.PlainBuilder().build())
					.build();
			return new KeyedHolder(aKey, tBiome);
		}

		/** The synthetic reference holder -- value + unwrapKey both controlled (the dimensionHolder form). */
		static final class KeyedHolder implements Holder<Biome> {
			private final ResourceKey<Biome> mKey;
			private final Biome mValue;

			KeyedHolder(ResourceKey<Biome> aKey, Biome aValue) {
				mKey = aKey;
				mValue = aValue;
			}

			@Override public Biome value() { return mValue; }
			@Override public boolean isBound() { return true; }
			@Override public boolean is(ResourceLocation aId) { return false; }
			@Override public boolean is(ResourceKey<Biome> aKey) { return false; }
			@Override public boolean is(java.util.function.Predicate<ResourceKey<Biome>> aPredicate) { return false; }
			@Override public boolean is(TagKey<Biome> aTag) { return false; } // the IReverseTag bridge (forge-patched Holder)
			//? if neoforge {
			/*@Override public boolean is(Holder<Biome> aHolder) { return false; } // 21.1 holder-identity probe
			*///?}
			@Override public java.util.stream.Stream<TagKey<Biome>> tags() { return java.util.stream.Stream.empty(); }
			@Override public Either<ResourceKey<Biome>, Biome> unwrap() { return Either.left(mKey); } // the forge-patched unwrap (Either = com.mojang.datafixers.util)
			@Override public java.util.Optional<ResourceKey<Biome>> unwrapKey() { return java.util.Optional.of(mKey); }
			@Override public Kind kind() { return Kind.REFERENCE; }
			@Override public boolean canSerializeIn(HolderOwner<Biome> aOwner) { return true; }
		}

		CropLevel() {
			super(null);
		}

		void layFarm() {
			// moist farmland below + 4-deep dirt + open sky  --  the ideal-terrain rig
			mStates.put(POS.below(), Blocks.FARMLAND.defaultBlockState()
					.setValue(BlockStateProperties.MOISTURE, 7));
			for (int i = 2; i <= 5; i++) mStates.put(POS.below(i), Blocks.DIRT.defaultBlockState());
			mStates.put(POS, Blocks.AIR.defaultBlockState());
		}

		@Override public BlockState getBlockState(BlockPos aPos) {
			return mStates.getOrDefault(aPos, Blocks.AIR.defaultBlockState());
		}

		@Override public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags) {
			mStates.put(aPos, aState);
			return true;
		}

		@Override public boolean removeBlock(BlockPos aPos, boolean aIsMoving) {
			mStates.remove(aPos);
			return true;
		}

		@Override public void removeBlockEntity(BlockPos aPos) {}

		@Override public BlockEntity getBlockEntity(BlockPos aPos) { return null; }

		@Override public boolean addFreshEntity(Entity aEntity) {
			mDrops.add((ItemEntity) aEntity);
			return true;
		}

		@Override public GameRules getGameRules() { return new GameRules(); }

		@Override public boolean hasChunkAt(BlockPos aPos) { return false; }
		@Override public boolean hasChunkAt(int aX, int aZ) { return false; }

		@Override public RandomSource getRandom() { return mRandom; }

		@Override public Holder<Biome> getBiome(BlockPos aPos) { return mBiome; }

		@Override public boolean canSeeSky(BlockPos aPos) { return mSky; }
	}

	/** A fresh tile wired into its level (setLevel so the tick shell + drops route through it). */
	private static GT6CropBlockEntity tile(CropLevel aLevel) {
		GT6CropBlockEntity tTile = new GT6CropBlockEntity(sBeType, POS, sBlock.defaultBlockState());
		tTile.setLevel(aLevel);
		return tTile;
	}

	/** Drives n game ticks through the real static shell. */
	private static void drive(GT6CropBlockEntity aTile, CropLevel aLevel, int aTicks) {
		for (int i = 0; i < aTicks; i++) {
			GT6CropBlockEntity.tick(aLevel, POS, sBlock.defaultBlockState(), aTile);
		}
	}

	// ---------------------------------------------------------------- the grain rows

	/** Compat_Recipes_IndustrialCraft.java:610-613 + GT_BaseCrop.java:103-115. */
	@Test
	public void theGrainRowsAreVerbatim() {
		String[][] tRows = {
				{"rye", "Binnie"}, {"barley", "Glitchfiend"}, {"oats", "Pam"}, {"rice", "Ellpeck"}};
		for (String[] tRow : tRows) {
			GT6CropCard tCard = GT6CropGrains.crop(tRow[0]);
			assertNotNull(tCard, tRow[0] + " registered");
			assertTrue(tCard instanceof GT6CropGrains.GrainCard, "the GrainCard data form");
			GT6CropGrains.GrainCard tGrain = (GT6CropGrains.GrainCard) tCard;
			assertEquals(tRow[1], tGrain.discoveredBy(), tRow[0] + " credit");
			assertEquals(1, tCard.tier(), tRow[0] + " tier 1");
			assertEquals(7, tCard.maxSize(), tRow[0] + " size 7");
			assertEquals(7, tCard.harvestSize(), tRow[0] + " harvest size 7");
			assertEquals(2, tCard.sizeAfterHarvest(), tRow[0] + " after-harvest 2");
			assertEquals(200, tCard.growthDuration(), tRow[0] + " tier*200 (CropCard :68-70)");
			assertEquals(0, tCard.stat(0), tRow[0] + " Chem 0");
			assertEquals(4, tCard.stat(1), tRow[0] + " Food 4");
			assertEquals(0, tCard.stat(2), tRow[0] + " Def 0");
			assertEquals(0, tCard.stat(3), tRow[0] + " Color 0");
			assertEquals(2, tCard.stat(4), tRow[0] + " Weed 2");
			assertEquals(3, tCard.attributes().length, tRow[0] + " Wheat+Food+Grain");
			assertEquals("Wheat", tCard.attributes()[0], tRow[0] + " attr head");
		}
		assertEquals(4, GT6CropGrains.crops().size(), "exactly the four grains registered");
	}

	/** GT_BaseCrop.java:103-115  --  canGrow/canBeHarvested/canCross at the clamp-relevant sizes. */
	@Test
	public void theCardFacesFollowGtBaseCrop() {
		GT6CropBlockEntity tTile = new GT6CropBlockEntity(sBeType, POS, sBlock.defaultBlockState());
		assertTrue(WHEATISH.canGrow(tTile), "size 1 < 7 grows");
		assertFalse(WHEATISH.canBeHarvested(tTile), "size 1 < harvestSize 7 refuses");
		assertFalse(WHEATISH.canCross(tTile), "size+2=3 > 7 false (GT_BaseCrop :113-115)");
		tTile.setSize(5);
		assertTrue(WHEATISH.canGrow(tTile), "5 < 7 grows");
		assertFalse(WHEATISH.canBeHarvested(tTile), "5 < 7 refuses");
		assertFalse(WHEATISH.canCross(tTile), "7 > 7 false");
		tTile.setSize(6);
		assertTrue(WHEATISH.canCross(tTile), "8 > 7  --  the cross gate opens at 6");
		tTile.setSize(7);
		assertFalse(WHEATISH.canGrow(tTile), "maxSize stops growth");
		assertTrue(WHEATISH.canBeHarvested(tTile), "mature harvests");
	}

	/** The grain items bind as base seeds  --  the table-level face (real .get() rides the FML leg + RCON). */
	@Test
	public void theGrainItemsBindAsBaseSeeds() {
		for (Map.Entry<String, String> tRow : GT6CropGrains.GRAIN_ITEM_IDS.entrySet()) {
			assertNotNull(GT6CropGrains.crop(tRow.getKey()), tRow.getKey() + " card present");
			assertTrue(GT6CropFoods.PLAIN_ROWS.stream().anyMatch(aRow -> aRow.id().equals(tRow.getValue())),
					tRow.getValue() + " exists in the T5a band");
		}
		assertNull(GT6CropGrains.baseSeed(ItemStack.EMPTY), "empty plants nothing");
		// vanilla wheat seeds are NOT a GT6 grain base seed (the key face answers null)
		assertNull(GT6CropGrains.baseSeed(new ItemStack(Items.WHEAT_SEEDS)), "foreign items plant nothing");
	}

	// ---------------------------------------------------------------- growth + cadence

	/**
	 * The 256t shell  --  :56/:186-208  --  and the seeded first-cycle growth pin: the mirror
	 * transcribes :285-318 under the same seed-42 walk the shell's level random runs.
	 */
	@Test
	public void growthAdvancesOnlyEvery256TicksAndMatchesTheFormula() {
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.tryPlantIn(WHEATISH, 1, 0, 0, 0, 0), "planted");
		tTile.setTerrainHumidity(4);
		tTile.setTerrainNutrients(3);
		tTile.setTerrainAirQuality(5);

		drive(tTile, tLevel, 255);
		assertEquals(0, tTile.growthPoints(), "no cycle before tick 256");
		drive(tTile, tLevel, 1);
		// terrain 4/3/5  ->  provided = 12*5 = 60, minimum = 0 (tier 1, stats 0)  ->  base*(160)/100
		RandomSource tMirror = RandomSource.create(42);
		int tBaseGrowth = 3 + tMirror.nextInt(7);
		int tExpected = tBaseGrowth * (100 + 60) / 100;
		assertEquals(tExpected, tTile.growthPoints(), "the first cycle = the transcribed formula under seed 42");
	}

	/** The maturity drive  --  plant  ->  cycles  ->  size 7  ->  harvestable (the acceptance walk). */
	@Test
	public void grainsDriveToMaturityOffline() {
		CropLevel tLevel = new CropLevel();
		tLevel.layFarm();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.tryPlantIn(WHEATISH, 1, 1, 1, 1, 0), "the base-seed 1/1/1 stats");
		// base growth ~13/cycle at the farmland rig  ->  ~16 cycles per size-up  ->  ~93 total
		drive(tTile, tLevel, 256 * 150);
		assertEquals(7, tTile.size(), "mature after the offline drive");
		assertTrue(WHEATISH.canBeHarvested(tTile), "harvestable at 7");
		assertTrue(tTile.growthPoints() < 200, "the counter rests below duration at maturity");
	}

	/** The deficit arms  --  :296-307  --  slowed growth and the quality death (aux > 100, rand(32) > Re). */
	@Test
	public void qualityDeficitSlowsAndCanKill() {
		// the death arm: tier 10  ->  minimum 36, terrain 0  ->  provided 0  ->  aux 144 > 100
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.tryPlantIn(DEADLY, 1, 0, 0, 0, 0), "planted");
		tTile.setTerrainHumidity(0);
		tTile.setTerrainNutrients(0);
		tTile.setTerrainAirQuality(0);
		// Re 0: rand(32) > 0 dies unless rand(32)==0 (1/32 survive)
		drive(tTile, tLevel, 256);
		assertTrue(tTile.crop() == null && tTile.size() == 1 || tTile.growthPoints() == 0,
				"the first deficit cycle either kills or rolls the 1/32 survive");

		// the high-Re survival arm: Re 31  ->  rand(32) > 31 impossible  ->  survives, slowed only
		GT6CropBlockEntity tHardy = tile(tLevel);
		assertTrue(tHardy.tryPlantIn(DEADLY, 1, 0, 0, 31, 0), "planted");
		tHardy.setTerrainHumidity(0);
		tHardy.setTerrainNutrients(0);
		tHardy.setTerrainAirQuality(0);
		drive(tHardy, tLevel, 256 * 3);
		assertNotNull(tHardy.crop(), "Re 31 survives the aux>100 band");
		assertEquals(0, tHardy.growthPoints(), "aux 144  ->  the (100-144)/100 floor keeps growth at 0");
	}

	// ---------------------------------------------------------------- terrain three

	/** The pure arithmetic pins  --  :1237-1250, :534-546, :548-557, :503-532, IC2Crops :131-143. */
	@Test
	public void terrainFormulasPinAgainstDecompiled() {
		// biome humidity bonus: rainfall 1.0/temperature 0.5  ->  (25-12.5) -> 10, coefficient (10*0.5)=5  ->  15
		assertEquals(15, CropTickLogic.biomeHumidityBonus(1.0F, 0.5F), "wet-warm ceiling arm");
		// rainfall 0.0/temperature 2.0  ->  -12.5 -> -10, coefficient (10*(-8+8-1))=-10  ->  -20
		assertEquals(-20, CropTickLogic.biomeHumidityBonus(0.0F, 2.0F), "dry-hot floor arm");
		// rainfall 0.4/temp 0.8 (plains-ish): (int)(10-12.5)=-2, coefficient (int)(2*0.92)=1  ->  -1
		assertEquals(-1, CropTickLogic.biomeHumidityBonus(0.4F, 0.8F), "the plains arm");
		// humidity: bonus + moist farmland +2 + storage>=5 +2 + (200+24)/25=8
		assertEquals(12, CropTickLogic.terrainHumidity(0, true, 200), "the watered face");
		assertEquals(0, CropTickLogic.terrainHumidity(0, false, 0), "the dry face");
		// nutrients: table + dirt + (100+19)/20=5
		assertEquals(13, CropTickLogic.terrainNutrients(5, 3, 100), "the fertilized face");
		assertEquals(0, CropTickLogic.terrainNutrients(0, 0, 0), "the barren face");
		// air: y=100  ->  floor(60/15)=4 -> 2, fresh 9  ->  +4, sky +4  ->  10
		assertEquals(10, CropTickLogic.terrainAirQuality(100, 9, true), "the open-sky highlands face");
		assertEquals(4, CropTickLogic.terrainAirQuality(40, 8, false), "the buried face");
		// the nutrient table: the max-over-rows adaptation
		assertEquals(10, CropTickLogic.nutrientBiomeBonus(Biomes.JUNGLE), "JUNGLE +10");
		assertEquals(10, CropTickLogic.nutrientBiomeBonus(Biomes.SWAMP), "SWAMP +10");
		assertEquals(5, CropTickLogic.nutrientBiomeBonus(Biomes.FOREST), "FOREST +5");
		assertEquals(2, CropTickLogic.nutrientBiomeBonus(Biomes.RIVER), "RIVER +2");
		assertEquals(0, CropTickLogic.nutrientBiomeBonus(Biomes.PLAINS), "PLAINS 0");
		assertEquals(-2, CropTickLogic.nutrientBiomeBonus(Biomes.SAVANNA), "SAVANNA -2");
		assertEquals(-5, CropTickLogic.nutrientBiomeBonus(Biomes.WINDSWEPT_HILLS), "HILLS -5");
		assertEquals(-10, CropTickLogic.nutrientBiomeBonus(Biomes.NETHER_WASTES), "NETHER -10");
		assertEquals(-10, CropTickLogic.nutrientBiomeBonus(Biomes.THE_END), "END -10");
		assertEquals(0, CropTickLogic.nutrientBiomeBonus(Biomes.DESERT), "desert carries no nutrient row");
		assertEquals(0, CropTickLogic.nutrientBiomeBonus(null), "null key = 0");
	}

	/** The BE world-read halves over the map level  --  farmland moisture, dirt stack, 2x2 occlusion, sky. */
	@Test
	public void terrainReadsFollowTheWorld() {
		CropLevel tLevel = new CropLevel();
		tLevel.layFarm();
		tLevel.mBiome = CropLevel.scriptedBiome(Biomes.SWAMP, 0.9F, 0.8F); // the swamp-arm climate
		GT6CropBlockEntity tTile = tile(tLevel);
		tTile.updateBiomeHumidityBonus(tLevel, POS); // the :176-179 onLoaded face (the shell cadence is 40 cycles)
		tTile.updateTerrainHumidity(tLevel, POS);
		tTile.updateTerrainNutrients(tLevel, POS);
		tTile.updateTerrainAirQuality(tLevel, POS);
		int tSwampBonus = tTile.biomeHumidityBonus();
		assertEquals(tSwampBonus + 2 + 0 + 0, tTile.terrainHumidity(),
				"moist farmland +2, storage 0  ->  the (24)/25 term is 0 (the bonus rides the :176-179 face; the FORMULA pins live in the pure test)");
		// the :551 scan counts CONSECUTIVE dirt from below(1) -- the farmland seat breaks it at 0
		assertEquals(10 + 0 + 0, tTile.terrainNutrients(), "swamp +10, farmland below breaks the dirt walk");
		// the pure dirt stack arm: below(1..4) all dirt -> +4
		for (int i = 1; i <= 4; i++) tLevel.mStates.put(POS.below(i), Blocks.DIRT.defaultBlockState());
		tTile.updateTerrainNutrients(tLevel, POS);
		assertEquals(10 + 4 + 0, tTile.terrainNutrients(), "swamp +10, four dirt +4, storage 0");
		// the corner-2x2 window (x-1..x, z-1..z): this rig answers getBlockEntity() null, so the
		// tile's own cell rides the AIR face and nothing counts -- fresh rests at 9, fresh/2 = 4.
		// POS.y=3: floor((3-40)/15) clamps to 0; sky +4  ->  0+4+4 = 8
		assertEquals(8, tTile.terrainAirQuality(), "the open-sky read (fresh 9, the :515 head)");
		// one wall stone  ->  fresh 8  ->  8/2 = 4 (the half-floor holds)
		tLevel.mStates.put(new BlockPos(POS.getX() - 1, POS.getY(), POS.getZ() - 1), Blocks.STONE.defaultBlockState());
		tTile.updateTerrainAirQuality(tLevel, POS);
		assertEquals(0 + 4 + 4, tTile.terrainAirQuality(), "one occluded window cell (fresh 8, the half-floor holds)");
		// wall the remaining reachable window cells (the rig placed 3 stones; POS itself is the
		// 4th window cell and rides AIR here)  ->  fresh 6  ->  6/2 = 3
		tLevel.mStates.put(new BlockPos(POS.getX() - 1, POS.getY(), POS.getZ()), Blocks.STONE.defaultBlockState());
		tLevel.mStates.put(new BlockPos(POS.getX(), POS.getY(), POS.getZ() - 1), Blocks.STONE.defaultBlockState());
		tTile.updateTerrainAirQuality(tLevel, POS);
		assertEquals(0 + 3 + 4, tTile.terrainAirQuality(), "the walled window (fresh 6)");
		// buried: the sky flag off
		tLevel.mSky = false;
		tTile.updateTerrainAirQuality(tLevel, POS);
		assertEquals(0 + 3 + 0, tTile.terrainAirQuality(), "canSeeSky off  ->  no +4");
	}

	// ---------------------------------------------------------------- weed + weedEX

	/** :240-252  --  Weed-EX fully suppresses the 1% self-gen and drains at 1/10 per protected cycle. */
	@Test
	public void weedExSuppressesAndDrainsSeeded() {
		// the engine roll over empty NON-crossing tiles (tickCrop consumes exactly the
		// weedSelfGenRoll sequence there: the crossing attempts short-circuit before any RNG):
		// protected tiles never roll BECOME_WEED, and the drain is the 1/10 arm
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		tTile.setStorages(0, 0, 100);
		int tBecame = 0;
		int tDrained = 0;
		RandomSource tRandom = RandomSource.create(7);
		for (int i = 0; i < 10000; i++) {
			int tBefore = tTile.storageWeedEx();
			CropMath.tickCrop(tTile, List.of(), tRandom);
			if (tTile.crop() != null) tBecame++;
			if (tTile.storageWeedEx() < tBefore) tDrained++;
			tTile.clear();
			tTile.setStorages(0, 0, 100);
		}
		assertEquals(0, tBecame, "Weed-EX > 0 fully suppresses (:241)");
		assertTrue(tDrained > 900 && tDrained < 1100, "the 1/10 drain band, got " + tDrained);

		GT6CropBlockEntity tBareTile = tile(tLevel);
		int tBecameBare = 0;
		RandomSource tBare = RandomSource.create(7);
		for (int i = 0; i < 10000; i++) {
			CropMath.tickCrop(tBareTile, List.of(), tBare);
			if (tBareTile.crop() != null) tBecameBare++;
			tBareTile.clear();
		}
		assertTrue(tBecameBare > 80 && tBecameBare < 120, "the 1% band unprotected, got " + tBecameBare);
	}

	/** The seeded BE drain pin  --  the exact remaining value under seed 42 (dual-transcribed). */
	@Test
	public void weedExDrainIsExactUnderSeed() {
		// mirror: each cycle draws nextInt(100); while protected, the drain rolls nextInt(10)
		int tStorage = 150;
		RandomSource tRandom = RandomSource.create(42);
		for (int i = 0; i < 200; i++) {
			if (tRandom.nextInt(100) != 0 || tStorage > 0) {
				if (tStorage > 0 && tRandom.nextInt(10) == 0) tStorage--;
			}
		}
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		tTile.setStorages(0, 0, 150);
		drive(tTile, tLevel, 256 * 200); // 200 cycles
		assertEquals(tStorage, tTile.storageWeedEx(), "the shell's RNG walk = the transcription under seed 42");
		assertTrue(tTile.storageWeedEx() < 150, "the storage actually drains");
		assertNull(tTile.crop(), "an empty tile stays empty under Weed-EX");
	}

	// ---------------------------------------------------------------- harvest + pick

	/** :793-823 + GT_BaseCrop :139-145/148-151  --  the harvest count, the gains face, the size reset. */
	@Test
	public void harvestPinsSeededAndResetsSize() {
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.tryPlantIn(WHEATISH, 7, 0, 25, 0, 0), "mature with Ga 25");
		List<ItemStack> tDrops = tTile.performHarvest();
		assertNotNull(tDrops, "mature harvests");
		assertTrue(tDrops.size() >= 1, "tier 1 chance 0.95*1.03^25 ~ 1.99  ->  a seeded count >= 1");
		for (ItemStack tDrop : tDrops) {
			assertEquals(Items.WHEAT, tDrop.getItem(), "the gains face");
		}
		assertEquals(2, tTile.size(), "size  ->  afterHarvestSize (the :817 reset)");
		assertNotNull(tTile.crop(), "harvest keeps the plant");

		// the immature refusal  --  GT_BaseCrop :149
		GT6CropBlockEntity tYoung = tile(tLevel);
		tYoung.tryPlantIn(WHEATISH, 3, 0, 0, 0, 0);
		assertNull(tYoung.performHarvest(), "size 3 < harvestSize 7 returns null");
	}

	/** :731-776  --  the seed count mirror, the spill face, and the reset. */
	@Test
	public void pickPinsSeededAndResets() {
		// the mirror transcription over seed 42, a mature Ga-0 tile
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		tTile.tryPlantIn(WHEATISH, 7, 0, 0, 0, 0);
		int tExpected = 0;
		RandomSource tMirror = RandomSource.create(42); // the pick consumes the level random's rolls
		float tFirstChance = (float) (WHEATISH.dropSeedChance(tTile) * Math.pow(1.1, 0));
		if (tMirror.nextFloat() <= (tFirstChance + 1.0F) * 0.8F) tExpected++;
		if (tMirror.nextFloat() <= WHEATISH.dropSeedChance(tTile)) tExpected++;
		assertTrue(tTile.pick(tLevel, POS), "a planted tile picks");
		assertEquals(tExpected, tLevel.mDrops.size(), "the seed count = the transcription");
		for (ItemEntity tDrop : tLevel.mDrops) {
			assertTrue(tDrop.getItem().is(Items.WHEAT_SEEDS), "the seeds face built BEFORE reset");
		}
		assertNull(tTile.crop(), "the :764 reset");
		assertEquals(1, tTile.size(), "reset to size 1");
		assertEquals(0, tTile.statGrowth(), "stats wiped");

		// the size-1 band: dropSeedChance = 0  ->  the only route is the exact-zero float (rare)
		GT6CropBlockEntity tBare = tile(tLevel);
		tBare.tryPlantIn(WHEATISH, 1, 0, 0, 0, 0);
		tLevel.mDrops.clear();
		tBare.pick(tLevel, POS);
		assertTrue(tLevel.mDrops.size() <= 1, "the size-1 seed chance is the float-zero lottery at most");
	}

	// ---------------------------------------------------------------- planting + crossing

	/** tryPlantIn :464-482 + the rightClick arms :410-462 + the leftClick downgrade :376-384. */
	@Test
	public void plantingAndCrossingUpgradeFaces() {
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.tryPlantIn(WHEATISH, 1, 1, 1, 1, 0), "plant on an empty stick");
		// the :465 gate = null card || weed card || crossingBase -- an OCCUPIED tile replants (the :473 reset)
		assertTrue(tTile.tryPlantIn(WHEATISH, 2, 3, 4, 5, 1), "an occupied tile replants over the reset");
		assertEquals(2, tTile.size(), "the replant took");
		assertEquals(3, tTile.statGrowth(), "the replanted stats took");
		// a planted tile refuses the crossing arm (:414 gate) and refuses foreign clicks
		assertFalse(tTile.rightClick(tLevel, POS, new ItemStack(stickItem()), false), "the stick arm needs an empty tile");
		assertFalse(tTile.rightClick(tLevel, POS, ItemStack.EMPTY, false), "an empty hand on an immature crop consumes nothing");

		GT6CropBlockEntity tEmpty = tile(tLevel);
		ItemStack tGrain = new ItemStack(grainItem(), 3);
		assertTrue(tEmpty.rightClick(tLevel, POS, tGrain, false), "the base-seed arm consumed");
		assertEquals(2, tGrain.getCount(), "one grain consumed (the :454 shrink)");
		assertNotNull(tEmpty.crop(), "planted from the item");
		assertEquals(1, tEmpty.size(), "size 1 (GT_BaseCrop.java:77)");
		assertEquals(1, tEmpty.statGrowth(), "G 1");
		assertEquals(1, tEmpty.statGain(), "Ga 1");
		assertEquals(1, tEmpty.statResistance(), "Re 1");

		// the crossing arm on a second empty tile  --  creative consumes nothing  --  and the downgrade
		GT6CropBlockEntity tCross = tile(tLevel);
		tLevel.mStates.put(POS, sBlock.defaultBlockState()); // the carrier state (the sync face walks the live state)
		ItemStack tSticks = new ItemStack(stickItem(), 4);
		assertTrue(tCross.rightClick(tLevel, POS, tSticks, true), "the stick arm consumed");
		assertTrue(tCross.crossingBase(), "crossingBase set");
		assertEquals(4, tSticks.getCount(), "creative does not consume (the :415 gate)");
		assertEquals(Boolean.TRUE, tLevel.getBlockState(POS).getValue(GT6CropSticksBlock.CROSSING), "the blockstate carrier synced");
		assertFalse(tCross.tryPlantIn(WHEATISH, 1, 1, 1, 1, 0), "a crossing tile refuses plants (:465)");
		tLevel.mDrops.clear();
		tCross.leftClick(tLevel, POS);
		assertFalse(tCross.crossingBase(), "the :379-383 downgrade");
		assertEquals(Boolean.FALSE, tLevel.getBlockState(POS).getValue(GT6CropSticksBlock.CROSSING), "the blockstate carrier re-synced");
		assertEquals(1, tLevel.mDrops.size(), "one stick spills");
		assertTrue(tLevel.mDrops.get(0).getItem().is(stickItem()), "the spill face");
	}

	// ---------------------------------------------------------------- trample

	/** :485-499 + CropCard :166  --  the sprint gate, the Re shield, the 1% band. */
	@Test
	public void trampleFollowsTheSprintGateAndResistance() {
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		tTile.tryPlantIn(WHEATISH, 7, 0, 0, 40, 0);
		// Re 40: rand(40) > 40 impossible  --  the shield face
		int tTrampled = 0;
		for (int i = 0; i < 5000; i++) {
			if (CropMath.isTrampled(WHEATISH, tTile, true, tLevel.getRandom())) tTrampled++;
		}
		assertEquals(0, tTrampled, "Re 40 shields the crop");

		// Re 31 leaves the 8/40 residual band (rand(40) in 32..39)  --  the observed ~10/5000
		GT6CropBlockEntity tEdge = tile(tLevel);
		tEdge.tryPlantIn(WHEATISH, 7, 0, 0, 31, 0);
		int tEdgeBreaks = 0;
		for (int i = 0; i < 5000; i++) {
			if (CropMath.isTrampled(WHEATISH, tEdge, true, tLevel.getRandom())) tEdgeBreaks++;
		}
		assertTrue(tEdgeBreaks > 3 && tEdgeBreaks < 20, "the Re-31 residual 1%*8/40 = 0.2%, got " + tEdgeBreaks);

		GT6CropBlockEntity tSoft = tile(tLevel);
		tSoft.tryPlantIn(WHEATISH, 7, 0, 0, 0, 0);
		int tBroke = 0;
		for (int i = 0; i < 20000; i++) {
			if (CropMath.isTrampled(WHEATISH, tSoft, true, tLevel.getRandom())) tBroke++;
		}
		assertTrue(tBroke > 130 && tBroke < 270, "the 1%*(40-0)/40 = 1% band, got " + tBroke);
		assertFalse(CropMath.isTrampled(WHEATISH, tSoft, false, tLevel.getRandom()), "a walking entity never tramples");

		// the BE gate: an empty tile ignores collisions entirely (the :486 crop null-guard)
		GT6CropBlockEntity tBare = tile(tLevel);
		tBare.onEntityCollision(tLevel, POS, null); // a null entity survives the crop==null early-out
		assertNull(tBare.crop(), "empty tiles never trample");
	}

	// ---------------------------------------------------------------- NBT + load

	/** :91-135  --  the verbatim key set round-trips. */
	@Test
	public void nbtRoundTripsAllFields() {
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.tryPlantIn(WHEATISH, 5, 7, 9, 11, 2), "planted with stats");
		tTile.setGrowthPoints(123);
		tTile.setStorages(120, 40, 30);
		tTile.setTerrainHumidity(3);
		tTile.setTerrainNutrients(4);
		tTile.setTerrainAirQuality(9);
		CompoundTag tTag = new CompoundTag();
		tTile.saveCrop(tTag);
		assertEquals("rye", tTag.getString("cropId"), "the upstream cropId key");
		assertEquals("gt6", tTag.getString("cropOwner"), "the single-owner port key");
		assertEquals((byte) 5, tTag.getByte("currentSize"), "the upstream currentSize key");

		GT6CropBlockEntity tLoaded = tile(tLevel);
		tLoaded.load(tTag);
		assertNotNull(tLoaded.crop(), "the card resolves by id");
		assertEquals("rye", tLoaded.crop().name());
		assertEquals(5, tLoaded.size());
		assertEquals(123, tLoaded.growthPoints());
		assertEquals(7, tLoaded.statGrowth());
		assertEquals(9, tLoaded.statGain());
		assertEquals(11, tLoaded.statResistance());
		assertEquals(2, tLoaded.scanLevel());
		assertEquals(120, tLoaded.storageWater());
		assertEquals(40, tLoaded.storageNutrients());
		assertEquals(30, tLoaded.storageWeedEx());
		assertEquals(3, tLoaded.terrainHumidity());
		assertEquals(4, tLoaded.terrainNutrients());
		assertEquals(9, tLoaded.terrainAirQuality());

		// the empty-stick carrier: crossingBase rides the tag without a card
		GT6CropBlockEntity tEmpty = tile(tLevel);
		tEmpty.setCrossingBase(true);
		CompoundTag tEmptyTag = new CompoundTag();
		tEmpty.saveCrop(tEmptyTag);
		assertTrue(tEmptyTag.getBoolean("crossingBase"), "the crossingBase key");
		assertFalse(tEmptyTag.contains("cropId"), "an empty tile writes no card keys");
		GT6CropBlockEntity tEmptyLoaded = tile(tLevel);
		tEmptyLoaded.load(tEmptyTag);
		assertTrue(tEmptyLoaded.crossingBase(), "the crossing face round-trips");
		assertNull(tEmptyLoaded.crop(), "still an empty tile");

		// an unknown card id degrades to an empty stick (the load guard)
		CompoundTag tGhost = tTag.copy();
		tGhost.putString("cropId", "ghost_crop");
		GT6CropBlockEntity tGhostTile = tile(tLevel);
		tGhostTile.load(tGhost);
		assertNull(tGhostTile.crop(), "unknown ids stay empty");
	}

	// ---------------------------------------------------------------- storage primitives

	/** :1198-1235 + :361-368  --  the cap/drain semantics the cbc-4 items wire. */
	@Test
	public void storagePrimitivesHonorTheCaps() {
		CropLevel tLevel = new CropLevel();
		GT6CropBlockEntity tTile = tile(tLevel);
		assertTrue(tTile.applyHydration(150), "hydrate");
		assertTrue(tTile.applyHydration(100), "the overflow DRAINS TO the cap (the :1203 limit-storage face)");
		assertEquals(200, tTile.storageWater(), "the 200 cap holds the remainder");
		assertFalse(tTile.applyHydration(10), "a full tank refuses");
		tTile.setStorages(200, 100, 0);
		assertFalse(tTile.applyFertilizer(true), "already at the 100 cap");
		tTile.setStorages(200, 0, 0);
		assertTrue(tTile.applyFertilizer(true), "manual +100");
		assertEquals(100, tTile.storageNutrients());
		tTile.setStorages(200, 0, 0);
		assertTrue(tTile.applyFertilizer(false), "automatic +90");
		assertEquals(90, tTile.storageNutrients());
		assertTrue(tTile.applyWeedEx(60, true), "manual weedEx");
		assertEquals(60, tTile.storageWeedEx());
		assertTrue(tTile.applyWeedEx(100, true), "the 100 cap stops at 100");
		assertEquals(100, tTile.storageWeedEx());
		assertTrue(tTile.hasWeedEX(), "the -5 consumption read");
		assertEquals(95, tTile.storageWeedEx(), "hasWeedEX burns 5 (:361-368)");
	}

	// ---------------------------------------------------------------- structural

	/** The scan needle, assembled from parts (the guard source stays residue-free itself). */
	private static final String IC2_PACKAGE_NEEDLE = "ic2" + ".";

	/** The ADR-CB2 self-enforcement  --  no IC2 package residue in the crop package. */
	@Test
	public void noIc2NameResidueInCropPackage() throws Exception {
		Path tCrop = mdkRoot().resolve(Path.of("src", "main", "java", "gregtech6", "crop"));
		assertTrue(Files.isDirectory(tCrop), "the crop package exists");
		try (Stream<Path> tWalk = Files.walk(tCrop)) {
			List<String> tHits = new ArrayList<>();
			tWalk.filter(aPath -> aPath.toString().endsWith(".java")).forEach(aPath -> {
				try {
					String tSource = Files.readString(aPath);
					// the needle is assembled so this guard source carries no literal residue itself
				if (tSource.contains(IC2_PACKAGE_NEEDLE)) tHits.add(aPath.getFileName().toString());
				} catch (Exception aE) {
					throw new RuntimeException(aE);
				}
			});
			assertTrue(tHits.isEmpty(), "IC2 package residue in " + tHits);
		}
	}

	/** The mdk root walk (the GT6CropFoodsTest form). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
		}
		throw new AssertionError("mdk root not found upward from " + Path.of("").toAbsolutePath());
	}

	// ---------------------------------------------------------------- the forge-leg Entity seam

	/**
	 * The bare-JUnit leg cannot construct an Entity: the Forge-patched ctor reads
	 * {@code FluidType.SIZE} (FluidType.java:71), a Lazy over ForgeRegistries.FLUID_TYPES
	 * that is dead offline  --  the GTEntityBlockInventoryDropTest seed verbatim.
	 */
	//? if forge {
	static void seedFluidTypeSize() {
		try {
			Object tLazy = net.minecraftforge.fluids.FluidType.class.getField("SIZE").get(null);
			java.lang.reflect.Field tSupplier = tLazy.getClass().getDeclaredField("supplier");
			tSupplier.setAccessible(true);
			tSupplier.set(tLazy, null);
			java.lang.reflect.Field tInstance = tLazy.getClass().getDeclaredField("instance");
			tInstance.setAccessible(true);
			tInstance.set(tLazy, 3);
			Object tEmptyType = net.minecraftforge.common.ForgeMod.class.getField("EMPTY_TYPE").get(null);
			java.lang.reflect.Field tValue = tEmptyType.getClass().getDeclaredField("value");
			tValue.setAccessible(true);
			if (tValue.get(tEmptyType) == null) {
				tValue.set(tEmptyType, new net.minecraftforge.fluids.FluidType(
						net.minecraftforge.fluids.FluidType.Properties.create()));
			}
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("could not seed the FluidType seams offline", aE);
		}
	}
	//?}
}
