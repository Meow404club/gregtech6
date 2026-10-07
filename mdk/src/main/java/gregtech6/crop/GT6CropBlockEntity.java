package gregtech6.crop;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import gregtech6.registry.GT6CropSticks;

/**
 * The crop-stick tile  --  the decompiled 1.12 TileEntityCrop (IC2, 1289 lines, the
 * ADR-CB1 mechanism authority) ported as the ADR-CB5 thin shell over the {@link CropMath}
 * engine (the cbc-1 -> cbc-2 merge wiring: this BE implements the cbc-2-owned
 * {@link CropTileView} and delegates every gameplay roll; NBT/registry/world-read duties
 * stay here). Upstream was a parasitic tile on the IC2 blockCrop (GT6 1.7.10 had no own
 * block); this is the self-owned counterpart pair with {@link GT6CropSticksBlock}.
 *
 * <p><b>NBT keys</b> are the upstream :91-135 verbatim set (cropOwner/cropId/statGrowth/
 * statGain/statResistance/storageNutrients/storageWater/storageWeedEX/terrain + currentSize/
 * growthPoints/scanLevel/crossingBase)  --  the cross-line carrier face.
 *
 * <p><b>Neighbor slots 0..3 = NORTH, SOUTH, EAST, WEST</b>  --  the attemptCrossing walk
 * order (TileEntityCrop.java:939-942) the seeded pins contract on.
 *
 * <p>Face census (cbc-6 汇核): KJS face — registration defer, the binding card declaration
 * (the crop ids are plain DeferredRegister entries, no KubeJS seam); the recipe face is
 * datapack-domain zero-adaptation. Viewer face — no machine diagram, zero JEI/EMI surfaces.
 * Jade face — another card, not this wave. RCON face — the readout/plant/stick/harvest
 * faces ARE the crop-a-world acceptance channel via /gt6crop (the GTBurnerCommand ruling).
 */
public class GT6CropBlockEntity extends BlockEntity implements CropTileView {

	/** The neighbor slot order  --  :939-942 north/south/east/west verbatim. */
	private static final Direction[] NEIGHBOR_SLOTS = {
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	/** The upstream save/sync snapshot carries every gameplay field (the :113-135 set minus customData). */
	@Nullable
	private GT6CropCard mCrop;
	private int mCurrentSize = 1;
	private int mGrowthPoints;
	private int mStatGrowth;
	private int mStatGain;
	private int mStatResistance;
	private int mScanLevel;
	private int mStorageWater;
	private int mStorageNutrients;
	private int mStorageWeedEX;
	private int mTerrainHumidity = -1;
	private int mTerrainNutrients = -1;
	private int mTerrainAirQuality = -1;
	private boolean mCrossingBase;
	private int mBiomeHumidityBonus;
	private long mTicker;
	private boolean mDirty;

	/** The BET-factory ctor (the GTBlockEntities form). */
	public GT6CropBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6CropSticks.CROP_STICKS_BE.get(), aPos, aState);
	}

	/** The fixture ctor  --  the offline-test seam (the GTEntityBlockInventoryDropTest shape). */
	public GT6CropBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	// -------------------------------------------------------------- the ticker shell

	/**
	 * The 256t shell  --  updateEntityServer :186-208: the ticker phase-offsets on load
	 * (onLoaded :173-177 {@code nextInt(256)}), every 40th cycle refreshes the biome bonus
	 * (:212-217), the three terrain reads spread over the 1024 band (:219-238), then the
	 * gameplay cycle rides {@link CropMath#tickCrop} (the performTick :240-283 port). World
	 * reads and world effects here; arithmetic in the engine.
	 */
	public static void tick(Level aLevel, BlockPos aPos, BlockState aState, GT6CropBlockEntity aTile) {
		if (aLevel.isClientSide) return;
		aTile.mTicker++;
		if (aTile.mTicker % CropTickLogic.TICKRATE != 0) return;
		aTile.serverCycle(aLevel, aPos);
	}

	private void serverCycle(Level aLevel, BlockPos aPos) {
		if (mTicker % (CropTickLogic.TICKRATE * 10 << 2) == 0) {
			updateBiomeHumidityBonus(aLevel, aPos);
		}
		if (mTicker % (CropTickLogic.TICKRATE << 2) == 0) {
			updateTerrainHumidity(aLevel, aPos);
		}
		if ((mTicker + CropTickLogic.TICKRATE) % (CropTickLogic.TICKRATE << 2) == 0) {
			updateTerrainNutrients(aLevel, aPos);
		}
		if ((mTicker + CropTickLogic.TICKRATE * 2) % (CropTickLogic.TICKRATE << 2) == 0) {
			updateTerrainAirQuality(aLevel, aPos);
		}

		// the performTick :240-283 port  --  empty-tile crossing/spreading/weed-self-gen and the
		// planted growth/decay/weed-work gate, one engine walk under the level random
		int tGrassSlot = CropMath.tickCrop(this, GT6Crops.crops(), aLevel.getRandom());
		if (tGrassSlot >= 0) {
			growGrassAt(aLevel, aPos, tGrassSlot); // the :347-357 world effect
		}
		markDirty();
	}

	/**
	 * The performWeedWork grass branch  --  :347-357: the returned slot's cell is air over
	 * dirt/grass/farmland, so the soil turns grass and the cell grows tall grass. The engine
	 * probed via {@link #soilAirAt}; this re-checks air before the write (cheap, avoids
	 * replacing a block that changed under the view).
	 */
	private void growGrassAt(Level aLevel, BlockPos aPos, int aSlot) {
		BlockPos tDst = aPos.offset(NEIGHBOR_SLOTS[aSlot].getNormal());
		if (!aLevel.getBlockState(tDst).isAir()) return;
		BlockPos tSoil = tDst.below();
		BlockState tSoilState = aLevel.getBlockState(tSoil);
		if (tSoilState.is(Blocks.DIRT) || tSoilState.is(Blocks.GRASS_BLOCK) || tSoilState.is(Blocks.FARMLAND)) {
			aLevel.setBlock(tSoil, Blocks.GRASS_BLOCK.defaultBlockState(), 7);
			aLevel.setBlock(tDst, Blocks.TALL_GRASS.defaultBlockState(), 7);
		}
	}

	// -------------------------------------------------------------- terrain reads

	/** :1237-1250  --  the rainfall/temperature pair through the declared modern faces. */
	public void updateBiomeHumidityBonus(Level aLevel, BlockPos aPos) {
		var tBiome = aLevel.getBiome(aPos).value();
		mBiomeHumidityBonus = CropTickLogic.biomeHumidityBonus(
				tBiome.getModifiedClimateSettings().downfall(), tBiome.getBaseTemperature());
	}

	/** :534-546  --  the world read half (farmland moisture), arithmetic in CropTickLogic. */
	public void updateTerrainHumidity(Level aLevel, BlockPos aPos) {
		BlockState tBelow = aLevel.getBlockState(aPos.below());
		boolean tMoist = tBelow.is(Blocks.FARMLAND)
				&& tBelow.getValue(BlockStateProperties.MOISTURE) >= 7;
		setTerrainHumidity(CropTickLogic.terrainHumidity(mBiomeHumidityBonus, tMoist, mStorageWater));
	}

	/** :548-557  --  the biome-key table + the 4-deep dirt scan. */
	public void updateTerrainNutrients(Level aLevel, BlockPos aPos) {
		ResourceKey<net.minecraft.world.level.biome.Biome> tKey =
				aLevel.getBiome(aPos).unwrapKey().orElse(null);
		int tDirt = 0;
		for (int i = 1; i < 5 && aLevel.getBlockState(aPos.below(i)).is(Blocks.DIRT); i++) tDirt++;
		setTerrainNutrients(CropTickLogic.terrainNutrients(
				CropTickLogic.nutrientBiomeBonus(tKey), tDirt, mStorageNutrients));
	}

	/**
	 * :503-532  --  the corner-2x2 fresh scan (the decompiled {@code x-1..<x} window verbatim  --
	 * the tile sits on the far corner) + {@code isBlockNormalCube}  ->  the modern
	 * {@code isRedstoneConductor} (the 1.12 isNormalCube rename lineage).
	 */
	public void updateTerrainAirQuality(Level aLevel, BlockPos aPos) {
		int tFresh = 9;
		for (int tX = aPos.getX() - 1; tX < aPos.getX() + 1 && tFresh > 0; tX++) {
			for (int tZ = aPos.getZ() - 1; tZ < aPos.getZ() + 1 && tFresh > 0; tZ++) {
				BlockPos tCheck = new BlockPos(tX, aPos.getY(), tZ);
				if (aLevel.getBlockState(tCheck).isRedstoneConductor(aLevel, tCheck)
						|| aLevel.getBlockEntity(tCheck) instanceof GT6CropBlockEntity) {
					tFresh--;
				}
			}
		}
		setTerrainAirQuality(CropTickLogic.terrainAirQuality(
				aPos.getY(), tFresh, aLevel.canSeeSky(aPos.above())));
	}

	// -------------------------------------------------------------- interactions

	/**
	 * The right-click face  --  rightClick :410-462, the item arms in order: stick  ->  crossing
	 * upgrade; base seed  ->  plant (via the {@link GT6CropGrains#baseSeed} seam); else the
	 * card harvest (GT_BaseCrop.rightclick :148-151  --  immature refuses). The fertilizer /
	 * hydration / Weed-EX item arms are card cbc-4 (the storage primitives live below).
	 *
	 * @return true when the click was consumed
	 */
	public boolean rightClick(Level aLevel, BlockPos aPos, ItemStack aHeld, boolean aCreative) {
		if (!aHeld.isEmpty()) {
			if (mCrop == null && !mCrossingBase && isStickItem(aHeld)) {
				if (!aCreative) aHeld.shrink(1);
				setCrossingBase(true);
				syncCrossingState(aLevel, aPos);
				markDirty();
				return true;
			}
			if (mCrop == null && !mCrossingBase) {
				GT6Crops.BaseSeed tSeed = GT6CropGrains.baseSeed(aHeld);
				if (tSeed != null) {
					clear();
					setCrop(tSeed.crop());
					mCurrentSize = tSeed.size();
					mStatGain = tSeed.statGain();
					mStatGrowth = tSeed.statGrowth();
					mStatResistance = tSeed.statResistance();
					if (!aCreative) aHeld.shrink(1);
					markDirty();
					return true;
				}
			}
		}
		if (mCrop == null) return false;
		if (!mCrop.canBeHarvested(this)) return false; // the GT_BaseCrop :149 refusal
		performManualHarvest(aLevel, aPos);
		return true;
	}

	/**
	 * The left-click face  --  onClicked :376-384: a planted tile picks (the seed drop), an
	 * empty crossing tile downgrades and spills one stick.
	 */
	public void leftClick(Level aLevel, BlockPos aPos) {
		if (mCrop != null) {
			pick(aLevel, aPos);
		} else if (mCrossingBase) {
			mCrossingBase = false;
			syncCrossingState(aLevel, aPos);
			markDirty();
			BlockPos carved = aPos; // the :382 dropAsEntity seat
			ItemStack tStick = stickStack(); // AIR (no-op drop) until the item is registered  --  lazy-safe
			if (!tStick.isEmpty()) {
				net.minecraft.world.Containers.dropItemStack(aLevel, carved.getX() + 0.5, carved.getY() + 0.5, carved.getZ() + 0.5, tStick);
			}
		}
	}

	/** The blockstate carrier of the crossing face  --  keep the twin in step (the render face). */
	private void syncCrossingState(Level aLevel, BlockPos aPos) {
		BlockState tState = aLevel.getBlockState(aPos);
		if (tState.getBlock() instanceof GT6CropSticksBlock && tState.getValue(GT6CropSticksBlock.CROSSING) != mCrossingBase) {
			aLevel.setBlock(aPos, tState.setValue(GT6CropSticksBlock.CROSSING, mCrossingBase), 3);
		}
	}

	/** tryPlantIn :464-482  --  the programmatic plant face (the /gt6crop smoke seam). */
	public boolean tryPlantIn(GT6CropCard aCard, int aSize, int aGr, int aGa, int aRe, int aScan) {
		if (aCard == null || mCrossingBase) return false;
		clear();
		setCrop(aCard);
		setSize(aSize);
		setStatGain(aGa);
		setStatGrowth(aGr);
		setStatResistance(aRe);
		setScanLevel(aScan);
		markDirty();
		return true;
	}

	/** The stick item key  --  the registry-KEY face (lazy-safe offline, the class doc posture). */
	static final net.minecraft.resources.ResourceLocation STICK_KEY =
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "crop_stick");

	private static boolean isStickItem(ItemStack aStack) {
		return STICK_KEY.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem()));
	}

	private static ItemStack stickStack() {
		net.minecraft.world.item.Item tItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(STICK_KEY);
		return tItem == null ? ItemStack.EMPTY : new ItemStack(tItem);
	}

	/** The count-carrying stick face  --  the break-drop seat (lazy-safe, the stickStack posture). */
	static ItemStack stickStackWithCount(int aCount) {
		ItemStack tStack = stickStack();
		tStack.setCount(aCount);
		return tStack;
	}

	/** The storage primitives  --  the decompiled :1198-1235 faces (the cbc-4 items wire these). */

	/** applyHydration :1198-1211  --  drain up to the 200 cap. */
	public boolean applyHydration(int aAmount) {
		if (mStorageWater >= 200 || aAmount <= 0) return false;
		mStorageWater = Math.min(200, mStorageWater + aAmount);
		markDirty();
		return true;
	}

	/** applyWeedEx :1213-1226  --  manual cap 100, automatic 150. */
	public boolean applyWeedEx(int aAmount, boolean aManual) {
		int tLimit = aManual ? 100 : 150;
		if (mStorageWeedEX >= tLimit || aAmount <= 0) return false;
		mStorageWeedEX = Math.min(tLimit, mStorageWeedEX + aAmount);
		markDirty();
		return true;
	}

	/** applyFertilizer :1228-1235  --  +100 manual / +90 automatic, the 100 cap. */
	public boolean applyFertilizer(boolean aManual) {
		if (mStorageNutrients >= 100) return false;
		mStorageNutrients = Math.min(100, mStorageNutrients + (aManual ? 100 : 90));
		markDirty();
		return true;
	}

	/** hasWeedEX :361-368  --  the -5 consumption read. */
	public boolean hasWeedEX() {
		if (mStorageWeedEX > 0) {
			mStorageWeedEX -= 5;
			return true;
		}
		return false;
	}

	/** The trample face  --  onEntityCollision :485-499, the sprint gate + the engine roll. */
	public void onEntityCollision(Level aLevel, BlockPos aPos, net.minecraft.world.entity.Entity aEntity) {
		if (mCrop == null) return;
		boolean tSprint = aEntity instanceof LivingEntity tLiving && tLiving.isSprinting(); // CropCard :166 default
		if (!CropMath.isTrampled(mCrop, this, tSprint, aLevel.getRandom())) return;
		clear();
		aLevel.setBlock(aPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		markDirty();
	}

	// -------------------------------------------------------------- harvest / pick

	/** performManualHarvest :779-790  --  the drops spill; true when something fell. */
	public boolean performManualHarvest(Level aLevel, BlockPos aPos) {
		List<ItemStack> tDrops = performHarvest();
		if (tDrops == null || tDrops.isEmpty()) return false;
		for (ItemStack tDrop : tDrops) {
			net.minecraft.world.Containers.dropItemStack(aLevel, aPos.getX() + 0.5, aPos.getY() + 0.5, aPos.getZ() + 0.5, tDrop);
		}
		return true;
	}

	/**
	 * performHarvest :793-823  --  the engine roll + the sizeAfterHarvest reset; null when not
	 * harvestable (the :821 null contract the command/scythe faces drive).
	 */
	@Nullable
	public List<ItemStack> performHarvest() {
		if (mCrop == null || !mCrop.canBeHarvested(this)) return null;
		return CropMath.performHarvest(mCrop, this, randomOrNew());
	}

	/**
	 * pick :731-776  --  the seed drop + the reset (the engine carries both, the seeds built
	 * BEFORE the :764 clear); false on an empty tile.
	 */
	public boolean pick(Level aLevel, BlockPos aPos) {
		if (mCrop == null) return false;
		List<ItemStack> tSeeds = CropMath.pickSeed(mCrop, this, randomOrNew());
		for (ItemStack tSeed : tSeeds) {
			net.minecraft.world.Containers.dropItemStack(aLevel, aPos.getX() + 0.5, aPos.getY() + 0.5, aPos.getZ() + 0.5, tSeed);
		}
		return true;
	}

	/**
	 * The offline seam  --  the seeded pins inject; live rides the level random (the reference
	 * port's random() face).
	 */
	private RandomSource randomOrNew() {
		return getLevel() != null ? getLevel().getRandom() : RandomSource.create();
	}

	// -------------------------------------------------------------- CropTileView

	@Override @Nullable public GT6CropCard crop() { return mCrop; }

	@Override
	public void setCrop(@Nullable GT6CropCard aCard) {
		mCrop = aCard;
		markDirty();
		// :567-569  --  the setCrop terrain refresh rides the next cycle in the shell (the
		// world reads need the level; the -1 sentinels keep the first growth honest).
	}

	@Override public boolean crossingBase() { return mCrossingBase; }
	@Override public void setCrossingBase(boolean aCrossingBase) { mCrossingBase = aCrossingBase; }
	@Override public int size() { return mCurrentSize; }
	@Override public void setSize(int aSize) { mCurrentSize = aSize; }
	@Override public int statGrowth() { return mStatGrowth; }
	@Override public int statGain() { return mStatGain; }
	@Override public int statResistance() { return mStatResistance; }
	@Override public void setStats(int aGrowth, int aGain, int aResistance) {
		mStatGrowth = aGrowth;
		mStatGain = aGain;
		mStatResistance = aResistance;
	}

	/** The single-stat setter face  --  the rightClick/tryPlantIn arms (not part of the view). */
	public void setStatGrowth(int aGrowth) { mStatGrowth = aGrowth; }
	/** The single-stat setter face  --  the rightClick/tryPlantIn arms (not part of the view). */
	public void setStatGain(int aGain) { mStatGain = aGain; }
	/** The single-stat setter face  --  the rightClick/tryPlantIn arms (not part of the view). */
	public void setStatResistance(int aResistance) { mStatResistance = aResistance; }

	@Override public int growthPoints() { return mGrowthPoints; }
	@Override public void setGrowthPoints(int aPoints) { mGrowthPoints = aPoints; }
	@Override public int scanLevel() { return mScanLevel; }
	@Override public void setScanLevel(int aScanLevel) { mScanLevel = aScanLevel; }
	@Override public int storageWater() { return mStorageWater; }
	@Override public int storageNutrients() { return mStorageNutrients; }
	@Override public int storageWeedEx() { return mStorageWeedEX; }
	@Override public void setStorages(int aWater, int aNutrients, int aWeedEx) {
		mStorageWater = aWater;
		mStorageNutrients = aNutrients;
		mStorageWeedEX = aWeedEx;
	}
	@Override public int terrainHumidity() { return mTerrainHumidity; }
	@Override public int terrainNutrients() { return mTerrainNutrients; }
	@Override public int terrainAirQuality() { return mTerrainAirQuality; }

	/** The terrain rig face  --  the test seam (the shell recomputes at the :219-238 cadence). */
	public void setTerrainHumidity(int aHumidity) { mTerrainHumidity = aHumidity; }
	/** The terrain rig face  --  the test seam (the shell recomputes at the :219-238 cadence). */
	public void setTerrainNutrients(int aNutrients) { mTerrainNutrients = aNutrients; }
	/** The terrain rig face  --  the test seam (the shell recomputes at the :219-238 cadence). */
	public void setTerrainAirQuality(int aAir) { mTerrainAirQuality = aAir; }

	@Override
	@Nullable
	public CropTileView neighbor(int aIndex) {
		if (level == null || aIndex < 0 || aIndex >= NEIGHBOR_SLOTS.length) return null;
		BlockPos tPos = worldPosition.offset(NEIGHBOR_SLOTS[aIndex].getNormal());
		return level.getBlockEntity(tPos) instanceof GT6CropBlockEntity tCrop ? tCrop : null;
	}

	@Override
	public boolean soilAirAt(int aIndex) {
		if (level == null || aIndex < 0 || aIndex >= NEIGHBOR_SLOTS.length) return false;
		BlockPos tDst = worldPosition.offset(NEIGHBOR_SLOTS[aIndex].getNormal());
		if (!level.getBlockState(tDst).isAir()) return false;
		BlockState tSoil = level.getBlockState(tDst.below());
		return tSoil.is(Blocks.DIRT) || tSoil.is(Blocks.GRASS_BLOCK) || tSoil.is(Blocks.FARMLAND);
	}

	@Override
	public ItemStack generateSeeds(GT6CropCard aCard, int aGrowth, int aGain, int aResistance, int aScan) {
		// the seed face routes through the card: the stat-carrying gt6:crop_seed live, the
		// base-seed copy while the seed item is unbound (the offline legs)
		return aCard == null ? ItemStack.EMPTY : aCard.seedStack(aGrowth, aGain, aResistance, aScan);
	}

	@Override
	public void clear() {
		mCrop = null; // :826-839 verbatim (customData is a declared cbc-2 cut  --  no card uses it yet)
		mStatGain = 0;
		mStatResistance = 0;
		mStatGrowth = 0;
		mTerrainAirQuality = -1;
		mTerrainHumidity = -1;
		mTerrainNutrients = -1;
		mGrowthPoints = 0;
		mScanLevel = 0;
		mCurrentSize = 1;
		markDirty();
	}

	/** The dirty flag  ->  block sync (not a view member -- the BE's own sync face). */
	public void markDirty() {
		mDirty = true;
		setChanged();
	}

	// -------------------------------------------------------------- NBT + sync

	//? if forge {
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		saveCrop(aNBT);
	}
	//?} else {
	/*// 21.1: the provider hooks (the TileEntityBase01Root fork form) -- the (CompoundTag)
	// members below stay plain so the tests and the command keep the same call face.
	@Override
	protected void saveAdditional(CompoundTag aNBT, net.minecraft.core.HolderLookup.Provider aProvider) {
		super.saveAdditional(aNBT, aProvider);
		saveCrop(aNBT);
	}
	 *///?}

	/** The package-private save face -- the offline NBT round-trip rides this on both legs. */
	void saveCrop(CompoundTag aNBT) {
		aNBT.putBoolean("crossingBase", mCrossingBase);
		if (mCrop != null) {
			aNBT.putString("cropOwner", "gt6"); // the single-owner port (upstream getOwner() face)
			aNBT.putString("cropId", mCrop.name());
			aNBT.putByte("statGrowth", (byte) mStatGrowth);
			aNBT.putByte("statGain", (byte) mStatGain);
			aNBT.putByte("statResistance", (byte) mStatResistance);
			aNBT.putShort("storageNutrients", (short) mStorageNutrients);
			aNBT.putShort("storageWater", (short) mStorageWater);
			aNBT.putShort("storageWeedEX", (short) mStorageWeedEX);
			aNBT.putByte("terrainHumidity", (byte) mTerrainHumidity);
			aNBT.putByte("terrainNutrients", (byte) mTerrainNutrients);
			aNBT.putByte("terrainAirQuality", (byte) mTerrainAirQuality);
			aNBT.putByte("currentSize", (byte) mCurrentSize);
			aNBT.putShort("growthPoints", (short) mGrowthPoints);
			aNBT.putByte("scanLevel", (byte) mScanLevel);
		}
	}

	/**
	 * The load face -- the tests and the /gt6crop smoke ride this on both legs. On the forge
	 * leg it doubles as the vanilla override (the 1.20.1 BlockEntity.load signature); the
	 * 21.1 leg bridges the provider hook into it (below, the TileEntityBase01Root form).
	 */
	public void load(CompoundTag aNBT) {
		//? if forge {
		super.load(aNBT);
		//?}
		loadCrop(aNBT);
	}

	//? if neoforge {
	/*// 21.1: the provider hook bridges into the plain member (the TileEntityBase01Root form).
	@Override
	protected void loadAdditional(CompoundTag aNBT, net.minecraft.core.HolderLookup.Provider aProvider) {
		super.loadAdditional(aNBT, aProvider);
		load(aNBT);
	}
	 *///?}

	private void loadCrop(CompoundTag aNBT) {
		mCrossingBase = aNBT.getBoolean("crossingBase");
		if (aNBT.contains("cropOwner") && aNBT.contains("cropId")) {
			mCrop = GT6Crops.crop(aNBT.getString("cropId"));
			if (mCrop == null) return; // unknown id  --  stay an empty stick
			mStatGrowth = aNBT.getByte("statGrowth");
			mStatGain = aNBT.getByte("statGain");
			mStatResistance = aNBT.getByte("statResistance");
			mStorageNutrients = aNBT.getShort("storageNutrients");
			mStorageWater = aNBT.getShort("storageWater");
			mStorageWeedEX = aNBT.getShort("storageWeedEX");
			mTerrainHumidity = aNBT.getByte("terrainHumidity");
			mTerrainNutrients = aNBT.getByte("terrainNutrients");
			mTerrainAirQuality = aNBT.getByte("terrainAirQuality");
			mCurrentSize = aNBT.getByte("currentSize");
			mGrowthPoints = aNBT.getShort("growthPoints");
			mScanLevel = aNBT.getByte("scanLevel");
		}
	}

	//? if forge {
	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata(); // the fluid-spring face  --  the full snapshot rides the chunk sync
	}

	//?} else {
	/*@Override
	public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider aProvider) {
	//21.1: BlockEntity.getUpdateTag/saveWithoutMetadata take the registries (the TileEntityBase03TicksAndSync fork form).
		return saveWithoutMetadata(aProvider);
	}
	*///?}

	@Override
	@javax.annotation.Nullable
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this); // the :437-444 base face  --  the client default onDataPacket loads the tag
	}

	// -------------------------------------------------------------- misc faces

	/** The upstream :176-177 onLoaded phase offset + the :178 immediate biome bonus refresh. */
	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide) {
			if (mTicker == 0) {
				mTicker = level.getRandom().nextInt(CropTickLogic.TICKRATE);
			}
			// the onLoaded :178 ride-along -- the biome bonus is valid from the FIRST 256t cycle
			// (without it the shell would answer 0 until the next :212 40-cycle refresh, up to
			// 10240t of humidity math over a stale zero).
			updateBiomeHumidityBonus(level, worldPosition);
		}
	}

	/** The stored biome humidity bonus (the :58 byte face) -- the wiring-pin seam for the offline suite. */
	public int biomeHumidityBonus() {
		return mBiomeHumidityBonus;
	}

	/** The readout line  --  the /gt6crop readout smoke face (the Cropnalyzer domain is cbc-4). */
	public String readout() {
		if (mCrop == null) return mCrossingBase ? "crossing=true" : "empty";
		return "crop=" + mCrop.name() + " size=" + mCurrentSize + " growth=" + mGrowthPoints
				+ " G/Ga/Re=" + mStatGrowth + "/" + mStatGain + "/" + mStatResistance
				+ " terrain=" + mTerrainHumidity + "/" + mTerrainNutrients + "/" + mTerrainAirQuality
				+ " water=" + mStorageWater + " nutrients=" + mStorageNutrients + " weedEx=" + mStorageWeedEX;
	}

}
