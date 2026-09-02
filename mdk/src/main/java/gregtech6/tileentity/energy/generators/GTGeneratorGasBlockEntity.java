package gregtech6.tileentity.energy.generators;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fluids.FluidStack;

/**
 * 1.20.1 counterpart of the GT6 Gas Burning Box — task p13-burning-box-family spec ③,
 * ported from gregtech/tileentity/energy/generators/MultiTileEntityGeneratorGas.java
 * (:38-80): {@code extends MultiTileEntityGeneratorLiquid} there, the same shape here.
 *
 * <p><b>The FM.Burn ruling (NOT FM.Gas!)</b>: the task card pins it and the Loader
 * confirms it — EVERY Gas Burning Box row registers {@code NBT_FUELMAP, FM.Burn}
 * (Loader_MultiTileEntities.java:645-673, the Gas section :649-659 + the Dense
 * :663-673), NOT {@code FM.Gas} (FM.java:42, a map NO burning-box row uses). The
 * {@link #acceptsFuelFluidGas} fill gate below is the :41 {@code FL.gas(...)} FILTER on
 * the shared FM.Burn map — the Gas box burns gas fuel ROWS, the same map the Liquid
 * family burns liquid rows from.</p>
 *
 * <p><b>The fill gate (:40-42)</b>: the tank accepts a fluid only when the FM.Burn map
 * contains it as an input AND it is a gas ({@code FL.gas(aFluidToFill)}) — the exact
 * inverse of the Liquid family's {@code && !FL.gas(...)} tail
 * ({@link GTGeneratorLiquidBlockEntity#acceptsFuelFluid}).</p>
 *
 * <p><b>The on-off face (:44-46, ITileEntityAdjacentOnOff)</b>: the redstone/cover
 * adjacency stop surface — {@code setAdjacentOnOff(false)} kills the flame
 * ({@code mBurning = F; mCooldown = 0}) and answers the live state. The port has no
 * cover/adjacency-on-off seam yet, so the STOP FACE rides the command door
 * ({@code /gt6burner extinguish}, the GTEngineCommand mode precedent) — the same three
 * methods as the pure on-off triple here.</p>
 *
 * <p><b>Cropped with declaration</b>: the no-op burning particles (:49-51 — the Gas
 * box burns clean, the AV pool rides the Liquid-class seam anyway); and the texture
 * family (:53-77 — one shared block texture, the crank ruling).
 */
public class GTGeneratorGasBlockEntity extends GTGeneratorLiquidBlockEntity {

	public GTGeneratorGasBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	/** The BET factory form (the diesel 2-arg shape — the shared type resolves at tick time). */
	public GTGeneratorGasBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "burning_box.gas"; // the family base name; the concrete BET rows carry their own registry paths
	}

	/**
	 * The :40-42 fill gate — the FM.Burn containsInput AND the gas filter (the exact
	 * inverse tail of the Liquid family's {@code acceptsFuelFluid}).
	 */
	public boolean acceptsFuelFluidGas(@Nullable FluidStack aFluid) {
		return containsBurnInput(aFluid) && gregtech6.fluid.GTFluidLists.isGas(fluidPath(aFluid));
	}

	// ---------------------------------------------------------------------------
	// the on-off triple (upstream :44-46 verbatim — the ITileEntityAdjacentOnOff face,
	// the /gt6burner extinguish door's backend)
	// ---------------------------------------------------------------------------

	/** Upstream :44/:45 — extinguishing clears the cooldown too; answers the LIVE burning state. */
	public boolean setStateOnOff(boolean aOnOff) {
		if (mBurning && !aOnOff) {
			mBurning = false;
			mCooldown = 0;
		}
		return mBurning;
	}

	/** Upstream :44 setAdjacentOnOff — the same body (the adjacency carrier is the pool). */
	public boolean setAdjacentOnOff(boolean aOnOff) {
		return setStateOnOff(aOnOff);
	}

	/** Upstream :46 — the live state. */
	public boolean getStateOnOff() {
		return mBurning;
	}
}
