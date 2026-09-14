package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.TD;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GT6Turbines;

/**
 * The Steam Turbine controller (task p29-w3-turbine-dynamo ②) — the 1.20.1 counterpart of
 * MultiTileEntityLargeTurbineSteam over the {@link GTMultiBlockConverter} base. All four
 * rows accept TD.Energy.STEAM packets on the FRONT and emit RU at the far plate (the base
 * conversion is exact for this family — no override), 4096/8192/16384/131072 RU out over
 * the 12288/24576/49152/393216 SU packet windows (Loader :1254-1257, ×STEAM_PER_EU 2),
 * WASTE_ENERGY = T (the funnel vent — over-offered steam never reaches RU).
 *
 * <p>The upstream FLUID remainder (the steam tank pair, the half-split buffer, the
 * STEAM_PER_WATER 170 distilled-water counter, LargeTurbineSteam.java:46-58/:142-162) is
 * the declared crop of the base class doc — this card consumes the packet path only.
 */
public class GTSteamTurbineBlockEntity extends GTMultiBlockConverter {

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTSteamTurbineBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GTSteamTurbineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Turbines.STEAM_TURBINE_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GT6Turbines.SteamTurbineBlock tBlock) {
			GT6Turbines.SteamTurbineRow tRow = tBlock.row();
			applyRow(tRow.input(), tRow.output(), TD.Energy.STEAM, TD.Energy.RU, true, false);
		}
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_steam_turbine"; // BET registry path mirrors it (GT6Turbines.STEAM_TURBINE_BE)
	}

	@Override
	protected net.minecraft.world.level.block.Block getWallBlock() {
		net.minecraft.world.level.block.state.BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Turbines.SteamTurbineBlock tBlock) {
			return tBlock.wallBlock();
		}
		return GTMultiBlocks.anyPartBlock("dense_wall_stainless_steel"); // the offline/defensive default (row 1's wall)
	}

	@Override
	protected boolean fluidColumn() {
		return true; // LargeTurbineSteam.java:90-91 — the ONLY_FLUID/ONLY_FLUID_IN middle column
	}

	@Override
	protected int farPlateDesign() {
		return 3; // LargeTurbineSteam.java:96 — the out cell's aDesign
	}
}
