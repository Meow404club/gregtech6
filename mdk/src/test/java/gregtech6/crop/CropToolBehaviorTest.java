package gregtech6.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.crop.behavior.CropnalyzerBehavior;
import gregtech6.crop.behavior.CropScytheBehavior;
import gregtech6.crop.behavior.CropWateringBehavior;
import gregtech6.crop.behavior.CropWeedExBehavior;
import gregtech6.items.tools.GTSenseItem;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The cbc-4-crop-tools offline suite  --  the four tool hook points pinned over the cbc-1
 * storage primitives (GT6CropBlockEntity.applyHydration/applyWeedEx/applyFertilizer), each
 * test double-marked with its upstream anchor:
 * <ul>
 * <li>hydration  --  TileEntityBase08FluidContainer.java:251-258 (== Behavior_Watering_Crops
 *     :49-57): the {@code min((200-h)/10, available)} drain, the 1 mB -> 10 hydration rate,
 *     the 200 cap, and the integer-division floor quirk.</li>
 * <li>Weed-Ex  --  IC2 1.12 decompiled TileEntityCrop.applyWeedEx :1213-1226 (the ADR-CB1
 *     authority; GT_Spray_Bug_Item :65-68 is dead commented code): manual cap 100, automatic
 *     cap 150, fill-to-limit, the +100 dose.</li>
 * <li>Cropnalyzer  --  Behavior_Cropnalyzer.java:76-100: the scanLevel-4 bump + the four
 *     verbatim readout lines (Type/Plant/Environment/Attributes).</li>
 * <li>sense/scythe  --  ToolCompat.java:174-183: the 3x3x3 harvest walk (centre included),
 *     one damage point per harvested tile (the :179 10000-unit fold), the world-drop face,
 *     the canCollect-gated 4x2x4 WD.suckAll box (WD.java:111-125, gate = ToolStats.java:77
 *     default-F via MultiItemTool.java:215-218).</li>
 * </ul>
 * Same posture as CropBlockEntityTest: fixture cards over vanilla stacks (the grain items
 * are FML-leg/RCON-only), the map-backed world double, seeded random.
 */
public class CropToolBehaviorTest extends GTOfflineTestBase {

	static BlockEntityType<GT6CropBlockEntity> sBeType;
	static GT6CropSticksBlock sBlock;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** The vanilla-stack fixture twin of the rye row (the GrainCard ctor is package-visible). */
	static final CropCardView RYEISH = new GT6CropGrains.GrainCard("rye", "Binnie", "food_crop_rye",
			1, 7, 2, 7, new int[] {0, 4, 0, 0, 2}, new String[] {"Wheat", "Food", "Grain"}) {
		@Override public List<ItemStack> gains(CropTileView aCrop) { return List.of(new ItemStack(Items.WHEAT)); }
		@Override public ItemStack seeds(CropTileView aCrop) { return new ItemStack(Items.WHEAT_SEEDS); }
	};

	@BeforeAll
	static void buildOfflineFixtures() {
		//? if forge {
		CropBlockEntityTest.seedFluidTypeSize(); // the ItemEntity ctor's FluidType read dead-headed (the cbc-1 seam)
		//?}
		GTOfflineTestBase.unfreezeBlockEntityTypeRegistry(); // the base @BeforeAll already rode; idempotent
		sBlock = block();
		sBeType = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6CropBlockEntity(sBeType, aPos, aState), sBlock).build(null);
	}

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

	/** The world double with live block entities  --  the scythe 3x3x3 walks it. */
	static class ToolLevel extends CropBlockEntityTest.CropLevel {
		final Map<BlockPos, BlockEntity> mTiles = new HashMap<>();

		void place(BlockPos aPos, GT6CropBlockEntity aTile) {
			aTile.setLevel(this);
			mTiles.put(aPos, aTile);
		}

		@Override public BlockEntity getBlockEntity(BlockPos aPos) { return mTiles.get(aPos); }

		@Override
		public <T extends Entity> List<T> getEntitiesOfClass(Class<T> aClass, AABB aBox, Predicate<? super T> aFilter) {
			List<T> rFound = new ArrayList<>();
			for (ItemEntity tEntity : mDrops) {
				if (!tEntity.isRemoved() && aClass.isInstance(tEntity)
						&& tEntity.getBoundingBox().intersects(aBox) && aFilter.test(aClass.cast(tEntity))) {
					rFound.add(aClass.cast(tEntity));
				}
			}
			return rFound;
		}

		@Override
		public <T extends Entity> List<T> getEntitiesOfClass(Class<T> aClass, AABB aBox) {
			return getEntitiesOfClass(aClass, aBox, aEntity -> true);
		}
	}

	private static GT6CropBlockEntity tile(GT6CropSticksBlock aBlock) {
		return new GT6CropBlockEntity(sBeType, POS, aBlock.defaultBlockState());
	}

	/** A planted tile at the given size with the 1/1/1 base stats (the /gt6crop plant face). */
	private static GT6CropBlockEntity planted(CropCardView aCard, int aSize) {
		GT6CropBlockEntity tTile = new GT6CropBlockEntity(sBeType, POS, sBlock.defaultBlockState());
		tTile.tryPlantIn(aCard, aSize, 1, 1, 1, 0);
		return tTile;
	}

	// ---------------------------------------------------------------- ① hydration

	/** TileEntityBase08FluidContainer.java:253 verbatim arithmetic, the floor quirk included. */
	@Test
	public void hydrationDrainFollowsTheUpstreamMath() {
		assertEquals(20, CropWateringBehavior.drainForHydration(0, 1000), "dry tile drinks to the 200 cap");
		assertEquals(15, CropWateringBehavior.drainForHydration(0, 15), "the tank amount bounds the drain");
		assertEquals(0, CropWateringBehavior.drainForHydration(195, 10), "the /10 floor: 5 hydration can never top off (:253)");
		assertEquals(5, CropWateringBehavior.drainForHydration(150, 6), "50 open capacity = 5 mB asked");
		assertEquals(0, CropWateringBehavior.drainForHydration(200, 1000), "at cap nothing flows");
	}

	/** :254-256 through the cbc-1 applyHydration carrier  --  1 mB pays 10 hydration, cap 200. */
	@Test
	public void waterCropAppliesTheTenfoldRate() {
		GT6CropBlockEntity tTile = tile(sBlock);
		assertTrue(CropWateringBehavior.waterCrop(tTile, 10), "10 mB flow into the dry tile");
		assertEquals(100, tTile.getStorageWater(), "10 mB -> 100 hydration (:256 the *10 rate)");
		assertTrue(CropWateringBehavior.waterCrop(tTile, 10), "(200-100)/10 = 10 mB still asked, the full tank amount flows");
		assertEquals(200, tTile.getStorageWater(), "the carrier lands exactly on the 200 cap");
		assertFalse(CropWateringBehavior.waterCrop(tTile, 10), "at cap nothing flows, nothing drained");
	}

	// ---------------------------------------------------------------- ② Weed-Ex

	/** IC2 1.12 decompiled applyWeedEx :1213-1226 — manual cap 100, fill-to-limit, dose +100. */
	@Test
	public void weedExSprayFillsToTheManualCap() {
		GT6CropBlockEntity tTile = tile(sBlock);
		assertTrue(CropWeedExBehavior.spray(tTile), "dry tile sprays");
		assertEquals(100, tTile.getStorageWeedEX(), "the +100 dose lands at the manual cap");
		assertFalse(CropWeedExBehavior.spray(tTile), "at the cap the spray refuses (nothing consumed)");
		assertEquals(100, tTile.getStorageWeedEX(), "the refusal leaves the storage alone (NOT the dead-code +100 overshoot to 200)");
	}

	/** :1214 {@code limit = manual ? 100 : 150} — the partial state and the automatic face. */
	@Test
	public void weedExCapsDifferByManualArm() {
		GT6CropBlockEntity tHalf = tile(sBlock);
		tHalf.setStorageWeedEX(50);
		assertTrue(CropWeedExBehavior.spray(tHalf), "50 < 100 sprays");
		assertEquals(100, tHalf.getStorageWeedEX(), "fill-to-cap: 50+100 clamps to 100, not 150");

		GT6CropBlockEntity tAuto = tile(sBlock);
		tAuto.setStorageWeedEX(100);
		assertTrue(CropWeedExBehavior.sprayAutomatic(tAuto), "the automatic face runs to 150");
		assertEquals(150, tAuto.getStorageWeedEX(), "the automatic cap 150 (:1214)");
		assertFalse(CropWeedExBehavior.sprayAutomatic(tAuto), "150 = the automatic ceiling");
		assertFalse(CropWeedExBehavior.spray(tAuto), "manual stays capped below the automatic state");
	}

	// ---------------------------------------------------------------- ③ Cropnalyzer

	/** Behavior_Cropnalyzer.java:76-100 — the bump + the four verbatim lines. */
	@Test
	public void cropnalyzerScanBumpsScanLevelAndReadsEveryStorage() {
		GT6CropBlockEntity tTile = planted(GT6CropGrains.RYE, 3);
		tTile.setStorageWater(120);
		tTile.setStorageNutrients(40);
		tTile.setStorageWeedEX(25);
		tTile.setTerrainHumidity(3);
		tTile.setTerrainNutrients(5);
		tTile.setTerrainAirQuality(7);

		List<String> tLines = CropnalyzerBehavior.scan(tTile, POS);
		assertEquals(5, tLines.size(), "the :77/:84/:89/:94/:99 line set");
		assertEquals("--- X: 2 Y: 3 Z: 4 ---", tLines.get(0), "the :77 header");
		assertEquals("Type -- Name: rye   Growth: 1   Gain: 1   Resistance: 1", tLines.get(1), "the :84-88 Type line");
		assertEquals("Plant -- Fertilizer: 40   Water: 120   Weed-Ex: 25", tLines.get(2), "the :89-93 Plant line (the three storages)");
		assertEquals("Environment -- Nutrients: 5   Humidity: 3   Air-Quality: 7", tLines.get(3), "the :94-97 Environment line (the terrain three)");
		assertEquals("Attributes: Wheat, Food, Grain", tLines.get(4), "the :99-100 Attributes line");
		assertEquals(4, tTile.getScanLevel(), "the :78-80 first-scan bump to 4");

		CropnalyzerBehavior.scan(tTile, POS);
		assertEquals(4, tTile.getScanLevel(), "the repeat scan does not re-bump");
		assertEquals(4096L, CropnalyzerBehavior.COST_FIRST_SCAN, "upstream :79 V[6]");
		assertEquals(64L, CropnalyzerBehavior.COST_RESCAN, "upstream :82 V[3]");
	}

	// ---------------------------------------------------------------- ④ sense/scythe

	/** ToolCompat.java:174-183 — the 3x3x3 walk, centre included, one point per harvested tile. */
	@Test
	public void scytheHarvestsTheThreeCubeAndPaysPerTile() {
		ToolLevel tLevel = new ToolLevel();
		BlockPos tCenter = POS;
		BlockPos tEast = POS.east();
		BlockPos tNorth = POS.north();
		BlockPos tFar = POS.east(3); // outside the 3x3
		GT6CropBlockEntity tCentreTile = planted(RYEISH, 7);
		GT6CropBlockEntity tEastTile = planted(RYEISH, 7);
		GT6CropBlockEntity tImmature = planted(RYEISH, 3); // in range, canBeHarvested false -> skipped
		GT6CropBlockEntity tFarTile = planted(RYEISH, 7);
		tLevel.place(tCenter, tCentreTile);
		tLevel.place(tEast, tEastTile);
		tLevel.place(tNorth, tImmature);
		tLevel.place(tFar, tFarTile);

		int tDamage = CropScytheBehavior.harvestArea(tLevel, tCenter);
		assertEquals(2, tDamage, "the two mature in-range tiles (:177-180), immature and far excluded");
		assertEquals(2, tCentreTile.getCurrentSize(), "the afterHarvest reset rode the harvest");
		assertEquals(2, tEastTile.getCurrentSize(), "the east neighbour harvested too");
		assertEquals(3, tImmature.getCurrentSize(), "the immature tile untouched");
		assertEquals(7, tFarTile.getCurrentSize(), "the out-of-range tile untouched");
		// the drop COUNT is the :793-823 gaussian (0..2 per tile at tier 1) — the range pin,
		// not an exact-count pin (the exact roll is CropBlockEntityTest's seeded domain)
		assertTrue(tLevel.mDrops.size() >= 2, "each harvested tile spilled at least one drop (the harvest(T) world-drop face)");
		for (ItemEntity tEntity : tLevel.mDrops) {
			assertSame(Items.WHEAT, tEntity.getItem().getItem(), "the fixture drop column");
			assertTrue(tEntity.getItem().getCount() >= 1, "a whole gain stack rides (:815 Ga bonus may grow it)");
		}
	}

	/** The WD.java:111-125 sweep — the 4x2x4 box, removal-and-return, outsiders stay. */
	@Test
	public void suckBoxSweepsExactlyTheFourByTwoByFour() {
		ToolLevel tLevel = new ToolLevel();
		ItemEntity tInside = new ItemEntity(tLevel, POS.getX() + 0.5, POS.getY() + 0.5, POS.getZ() + 0.5, new ItemStack(Items.WHEAT, 3));
		ItemEntity tTooFarEast = new ItemEntity(tLevel, POS.getX() + 3.5, POS.getY() + 0.5, POS.getZ() + 0.5, new ItemStack(Items.WHEAT));
		ItemEntity tTooHigh = new ItemEntity(tLevel, POS.getX() + 0.5, POS.getY() + 2.5, POS.getZ() + 0.5, new ItemStack(Items.WHEAT));
		tLevel.mDrops.add(tInside);
		tLevel.mDrops.add(tTooFarEast);
		tLevel.mDrops.add(tTooHigh);

		List<ItemStack> tSwept = CropScytheBehavior.suckBox(tLevel, POS);
		assertEquals(1, tSwept.size(), "only the in-box entity");
		assertEquals(3, tSwept.get(0).getCount(), "the stack rides whole");
		assertTrue(tInside.isRemoved(), "the upstream removeEntity+setDead face (:117/:120)");
		assertFalse(tTooFarEast.isRemoved(), "the x+2.5 edge excludes the 4th column");
		assertFalse(tTooHigh.isRemoved(), "the y+1.5 ceiling excludes the 2-high box top");
	}

	/** The :102 gate lands at the item face — the plain sense never sweeps (ToolStats.java:77). */
	@Test
	public void thePlainSenseDoesNotCollect() {
		assertFalse(GTSenseItem.COLLECTS_DROPS, "canCollectDropsDirectly = ToolStats.canCollect() F default, GT_Tool_Sense never overrides");
	}

	/** The fertilizer manual arm rides the same carrier (the card SPEC's +100 cap-100 row). */
	@Test
	public void fertilizerManualArmPinsTheCarrier() {
		GT6CropBlockEntity tTile = tile(sBlock);
		assertTrue(tTile.applyFertilizer(true), "manual fertilizer applies");
		assertEquals(100, tTile.getStorageNutrients(), "the +100 manual dose at the 100 cap (decompiled applyFertilizer :1228-1235)");
		assertFalse(tTile.applyFertilizer(true), "at cap refused");
		GT6CropBlockEntity tAuto = tile(sBlock);
		tAuto.setStorageNutrients(40);
		assertTrue(tAuto.applyFertilizer(false), "the automatic face");
		assertEquals(100, tAuto.getStorageNutrients(), "40+90 clamps to the 100 cap");
	}

	/** The scan face survives the save/load carrier (the NBT keys are the cross-line face). */
	@Test
	public void scanLevelAndStoragesRoundTripThroughNbt() {
		GT6CropBlockEntity tTile = planted(GT6CropGrains.RYE, 2);
		tTile.setStorageWater(180);
		tTile.setStorageWeedEX(75);
		tTile.setScanLevel(4);
		CompoundTag tTag = new CompoundTag();
		tTile.saveCrop(tTag);
		GT6CropBlockEntity tRead = tile(sBlock);
		tRead.load(tTag);
		assertEquals(180, tRead.getStorageWater(), "storageWater rides the :91-135 key set");
		assertEquals(75, tRead.getStorageWeedEX(), "storageWeedEX rides");
		assertEquals(4, tRead.getScanLevel(), "scanLevel rides");
		assertEquals("rye", tRead.getCrop().name(), "the card rebinding (the registry-key face)");
		assertEquals(3, tRead.getCrop().attributes().length, "the rye attribute row rides with the card");
	}
}
