package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.covers.CoverData;
import gregtech6.fluid.FluidTankGT;

/**
 * The drain cover — 1.20.1 port of gregapi/cover/covers/CoverDrain.java:63-254 (task
 * p34-covers-gameplay-10; upstream item id 1020 "Drain"). Three collection arms on the
 * ported surface: the rain arm (:70-84 — every 100-tick beat, phase-offset per beat
 * slot, a top/horizontal face with sky access fills {@code 1000 * (thundering?2:1)} L
 * of water in precipitating biomes above 0.2 temperature), and the fluid-block arms
 * (:109-156 — a vanilla water source in front fills 1000 L and the block goes air,
 * UNLESS the cell rides the upstream infinite-water rule WD.java:690 (the sea-level
 * band {@code [waterLevel-15, waterLevel]} AND a BIOMES_RIVER_LAKE biome — the 1.20.1
 * approximation folds the vanilla rows onto BiomeTags.IS_RIVER, the method doc), which
 * refills forever at the 16000 L upstream bonus rate and rides NO face gate (:126-127);
 * a vanilla lava source fills 1000 L and goes air behind the :147 face gate).
 *
 * <p>DECLARED DEVIATION — the placement gate keys on the pump-tank seam (the CoverVent
 * class doc; upstream {@code canTick() && instanceof IFluidHandler} :64), the beat
 * carrier is the host timer (the CoverPump :68 precedent), and the biome rainfall
 * float folds onto the precipitation boolean (the GT6BumbleGenes.rainfallOf classifier
 * precedent — 1.20.1 biomes carry no live rainfall axis).
 *
 * <p>DECLARED CUT — the mob-fluid zoo: the XP-orb arm (:85-108, FL.XP/FL.Mob), the
 * walk-over arms (:162-224, sewage/ink/slime), the GT fluid-block family
 * (BlockBaseFluid/River/Ocean/Swamp, :113-145), the generic {@code IFluidBlock} drain
 * and the OpenBlocks XP drain all key on fluids/entities this port does not register
 * (the census fluid gaps); the slab/stairs rain-geometry special cases (:76-80) fold
 * into the sky-access check. The {@code onWalkOver} hook exists since the asphalt
 * restoration (task p37-covers-crafting-asphalt) but the drain's walk arms stay cut with
 * the fluid zoo they key on.
 */
public class CoverDrain extends AbstractCoverDefault {

	/** The upstream :129/:134 per-source fill — one bucket per beat. */
	public static final long SOURCE_FILL = 1000;

	/** The upstream :127/:137 infinite-water bonus rate. */
	public static final long INFINITE_WATER_FILL = 16000;

	/** The upstream :70 rain beat. */
	public static boolean isRainBeat(long aTimer) {
		return aTimer % 100 == 10;
	}

	/** The upstream :109 fluid-block sweep beat. */
	public static boolean isSweepBeat(long aTimer) {
		return aTimer % 20 == 5;
	}

	/** Upstream :64 — the drain only mounts on hosts with a tank to collect into (the pump-seam gate form). */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return aData.mTileEntity.getCoverPumpTank() == null;
	}

	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (!aIsServerSide || aData.mStopped) return;
		FluidTankGT tTank = aData.mTileEntity.getCoverPumpTank();
		if (tTank == null) return;
		BlockEntity tHost = aData.mTileEntity.self();
		Level tLevel = tHost.getLevel();
		if (tLevel == null) return;
		Direction tDir = Direction.from3DDataValue(aCoverSide);
		BlockPos tFront = tHost.getBlockPos().relative(tDir);

		// :70-84 — the rain arm: top and horizontal faces, precipitating and warm enough, sky above
		if (isRainBeat(aTimer) && aCoverSide != Direction.DOWN.get3DDataValue() && tLevel.isRaining()
				&& tLevel.canSeeSky(tFront)) {
			Biome tBiome = tLevel.getBiome(tFront).value();
			if (tBiome.hasPrecipitation() && tBiome.getBaseTemperature() >= 0.2F) {
				long tFill = SOURCE_FILL * (tLevel.isThundering() ? 2 : 1); // :81 — the thundering doubling (the rainfall float folds, the class doc)
				tTank.fill(new FluidStack(Fluids.WATER, (int) tFill), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
			}
		}

		// :109-156 — the vanilla fluid-block sweep (aReceivedBlockUpdate folds into the beat, the CoverPump trim precedent)
		if (isSweepBeat(aTimer)) {
			BlockState tState = tLevel.getBlockState(tFront);
			FluidStack tFluid = null;
			if (tState.getBlock() == Blocks.WATER && tState.getValue(LiquidBlock.LEVEL) == 0) { // :124-125 — the full source
				// :126-127 — the infinite-water arm rides NO face gate (the review correction:
				// upstream fills the sea-level river/lake band straight off the block check)
				if (isInfiniteWater(tLevel, tFront)) {
					tTank.fill(new FluidStack(Fluids.WATER, (int) INFINITE_WATER_FILL), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
				} else {
					tFluid = new FluidStack(Fluids.WATER, (int) SOURCE_FILL); // :129
				}
			} else if (tState.getBlock() == Blocks.LAVA && tState.getValue(LiquidBlock.LEVEL) == 0) { // :133-134 — the full source
				tFluid = new FluidStack(Fluids.LAVA, (int) SOURCE_FILL); // :134
			}
			// :147 verbatim shape — the face orientation gate rides the tFluid CONSUMPTION
			// only (the infinite-water arm above is already gone): a horizontal face admits
			// every fluid; a vertical face admits gases and lighter-than-air fluids through
			// the BOTTOM face, everything else through the TOP face. The ported vanilla
			// set (water/lava) takes the TOP arm — the predicate below writes the upstream
			// form with the guarded isGas density proxy standing in for FL.gas/FL.lighter
			// (equivalent on water/lava, the class-doc cut note).
			if (tFluid != null) {
				boolean tVerticalOkay = aCoverSide == (isGas(tFluid)
						? Direction.DOWN.get3DDataValue() // FL.gas || FL.lighter → SIDES_BOTTOM
						: Direction.UP.get3DDataValue()); // else → SIDES_TOP
				if (aCoverSide != Direction.DOWN.get3DDataValue() || tVerticalOkay) { // SIDES_HORIZONTAL[aCoverSide] ||
					if (tTank.fill(tFluid, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == tFluid.getAmount()) {
						tTank.fill(tFluid, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
						tLevel.removeBlock(tFront, false); // :152 — setBlockToAir
					}
				}
			}
		}
	}

	/**
	 * Upstream :126 WD.infiniteWater (WD.java:690) — the covered cell sits in the
	 * sea-level band {@code [waterLevel-15, waterLevel]} AND the biome is a river/lake
	 * biome. The band rides the waterLevel table (WD.java:417-440): the overworld
	 * default 62, no-sky dimensions 31 (the TFC/TF rows ride cut subsystems). The biome
	 * set is CS.java:249 BIOMES_RIVER_LAKE (vanilla river + frozenRiver + the mod-named
	 * river/lake rows); the 1.20.1 APPROXIMATION folds the vanilla rows onto
	 * {@code BiomeTags.IS_RIVER} (which covers river and frozen_river, and any mod river
	 * joining the tag) — the mod-named LAKE rows have no vanilla counterpart and are
	 * declared unreachable in this port.
	 */
	static boolean isInfiniteWater(Level aLevel, BlockPos aPos) {
		if (!inSeaLevelBand(aPos.getY(), waterLevelOf(aLevel.dimensionType().hasSkyLight()))) return false;
		return aLevel.getBiome(aPos).is(BiomeTags.IS_RIVER); // BIOMES_RIVER_LAKE, the declared 1.20.1 approximation (the Holder tag face)
	}

	/**
	 * The guarded density proxy of :147 ({@code FL.gas(tFluid)} — the same form as the
	 * valve's isGas; duplicated because the shared-helper seam would touch an existing
	 * cover class, the zero-diff discipline).
	 */
	private static boolean isGas(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return false;
		try {
			return aFluid.getFluid().getFluidType().isLighterThanAir();
		} catch (NullPointerException tUnbound) {
			return false; // the offline doubles cannot bind the vanilla fluid-type RegistryObjects
		}
	}

	/** The {@code UT.Code.inside(tLevel-15, tLevel, aY)} band of WD.java:690, pure for the offline pin. */
	public static boolean inSeaLevelBand(int aY, int aWaterLevel) {
		return aY >= aWaterLevel - 15 && aY <= aWaterLevel;
	}

	/** The waterLevel table of WD.java:417-440 — sky-lit worlds ride 62, no-sky worlds 31 (the TFC/TF rows ride cut subsystems). Pure for the offline pin. */
	public static int waterLevelOf(boolean aHasSkyLight) {
		return aHasSkyLight ? 62 : 31;
	}

	/** Upstream :252 — the drain plate art (the sides/back stack folds into the single sprite). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/drain/front");
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/drain/front");
		 *///?}
	}
}
