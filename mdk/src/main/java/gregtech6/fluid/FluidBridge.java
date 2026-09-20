package gregtech6.fluid;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.level.material.Fluid;

/**
 * The material-name → fluid bridge skeleton (task p4-fluid-pipes spec ⑥). Upstream binds
 * {@code OreDictMaterial.mLiquid/mGas/mPlasma} FluidStacks directly onto the material
 * objects (OreDictMaterial.java:314-315); this port deletes those fields (root red line:
 * no net.minecraft types in gregapi) and recreates the lookup from the material's
 * internal name — the seam the root OreDictMaterial.java:174-175 comment leaves open
 * ("the Phase-2 fluid module recreates them from mNameInternal").
 *
 * <p>Shape: material name (lowercase, {@code mNameInternal}) → {@link RegistryObject}
 * of the still fluid. The W1 table carries the seed entry ("iron" →
 * {@link GTFluids#IRON_MOLTEN}); water/lava need no entry (vanilla fluids, resolved by
 * the callers directly). The unit conversion keeps the upstream Liter convention where
 * one molten material unit = 144 L — the {@code FL.make("iron.molten", 144)} binding
 * (Loader_Fluids.java:161-190); the per-material unit amount rides on the root
 * {@code mLiquidUnit} seam (OreDictMaterial.java:175), passed in by the caller until the
 * Phase-2 conversion decision lands.
 */
public final class FluidBridge {

	/** The TCon molten-metal Liter convention (Loader_Fluids.java:161: 144 L per material unit). */
	public static final long L_PER_MOLTEN_UNIT = 144;

	//? if forge {
	private static final Map<String, RegistryObject<? extends Fluid>> MOLTEN_FLUIDS = new HashMap<>();
	//?} else {
	/*private static final Map<String, net.neoforged.neoforge.registries.DeferredHolder<Fluid, ? extends Fluid>> MOLTEN_FLUIDS = new HashMap<>();
	//21.1: the swap's wildcard entry ate the "? extends " literal segment (id258 family) —
	//the type face is spelled out per leg here; the wildcard second parameter is covariant
	//(Supplier/Holder read positions only), so the FlowingFluid holder puts in fine.
	*///?}

	static {
		MOLTEN_FLUIDS.put("iron", GTFluids.IRON_MOLTEN);
		// task p32-qu-scanner-replicator — the molten.redstone carrier lands (the GTFluids
		// :194 row), so the Smeltery pour-back and any material walker resolve MT.Redstone.
		// The handle rides the table-driven CHEMICALS registration through the chemicalSource
		// seam (the bridge map stays material-name keyed).
		MOLTEN_FLUIDS.put("redstone", GTFluids.chemicalSource("redstone_molten"));
	}

	private FluidBridge() {}

	/** The molten fluid for a material, or null when the bridge has no entry (unknown/unregistered). */
	@Nullable
	public static net.minecraft.world.level.material.Fluid moltenFluidForMaterial(@Nullable String aMaterialName) {
		if (aMaterialName == null) return null;
		//? if forge {
		RegistryObject<? extends Fluid> tEntry = MOLTEN_FLUIDS.get(aMaterialName.toLowerCase(java.util.Locale.ROOT));
		return tEntry == null || !tEntry.isPresent() ? null : tEntry.get();
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Fluid, ? extends Fluid> tEntry = MOLTEN_FLUIDS.get(aMaterialName.toLowerCase(java.util.Locale.ROOT));
		//21.1: RegistryObject.isPresent → Holder.isBound (the command-file truth).
		return tEntry == null || !tEntry.isBound() ? null : tEntry.get();
		*///?}
	}

	/**
	 * A molten FluidStack of {@code aUnits} material units for the material, or null when
	 * unresolvable/non-positive. The int boundary clamps through {@link FluidTankGT#bindInt}
	 * (upstream FL.make(long) semantics, FL.java:805-810).
	 */
	@Nullable
	public static FluidStack moltenStack(@Nullable String aMaterialName, long aUnits, long aLitersPerUnit) {
		if (aUnits <= 0 || aLitersPerUnit <= 0) return null;
		net.minecraft.world.level.material.Fluid tFluid = moltenFluidForMaterial(aMaterialName);
		return tFluid == null ? null : new FluidStack(tFluid, FluidTankGT.bindInt(aUnits * aLitersPerUnit));
	}

	/** The 144 L/unit convenience of the upstream {@code FL.make("iron.molten", 144)} binding. */
	@Nullable
	public static FluidStack moltenStack(@Nullable String aMaterialName, long aUnits) {
		return moltenStack(aMaterialName, aUnits, L_PER_MOLTEN_UNIT);
	}
}
