package gregtech6.fluid;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

import net.minecraft.world.level.material.Fluid;

/**
 * The material-name → molten-fluid bridge (task fluid-pipes spec ⑥ skeleton; full-domain
 * walk task jade-molten-bridge-full). Upstream binds {@code OreDictMaterial.mLiquid/mGas/
 * mPlasma} FluidStacks directly onto the material objects (OreDictMaterial.java:314-315);
 * this port deletes those fields (root red line: no net.minecraft types in gregapi) and
 * recreates the lookup LIVE — no hand-kept table: the specOf id convention
 * ({@code GTFluids.java:2294-2297}: sanitized lowercase internal name + {@code "_molten"})
 * resolves against {@link GTFluids#chemicalSource}, the SOURCE_SEAM every spec-registered
 * family feeds (GTFluids.java:2368-2369), so the whole {@code gt6:<mat>_molten} domain
 * answers as rows land. iron is the W1 seed registered outside the spec tables
 * (GTFluids.java:241) and rides its own handle. The five legacy FL shorthand rows
 * (glass/plastic/molten_latex/molten_hsla/lithium_chloride — the naming-parity rows,
 * upstream {@code Loader_Fluids.java:191-192/:197/:199} + the createMolten walk) sit
 * outside the {@code <mat>_molten} convention and ride the explicit {@link #LEGACY_FL_IDS}
 * alias seam (task jade-molten-alias-seam): same official fluid face as the convention
 * rows. Unknown/unregistered material → null. The unit conversion keeps the upstream Liter
 * convention where one molten material unit = 144 L — the {@code FL.make("iron.molten", 144)}
 * binding (Loader_Fluids.java:161-190); the per-material unit amount rides on the root
 * {@code mLiquidUnit} seam (OreDictMaterial.java:175), passed in by the caller until the
 * Phase-2 conversion decision lands.
 */
public final class FluidBridge {

	/** The TCon molten-metal Liter convention (Loader_Fluids.java:161: 144 L per material unit). */
	public static final long L_PER_MOLTEN_UNIT = 144;

	/**
	 * The five legacy FL shorthand ids outside the {@code <mat>_molten} convention (task
	 * jade-molten-alias-seam): sanitized lowercase internal name → the TRUE registered id,
	 * upstream anchors verbatim — Loader_Fluids.java:191 {@code FL.create("plastic",
	 * "Molten Plastic", MT.Plastic...)} and :192 the bare {@code "glass"} row (no molten
	 * suffix at all), :197 {@code FL.create("molten.latex", ...)} (the prefix form kept;
	 * DISTINCT from the :198 bare "latex" BEE row), :199 {@code FL.create("molten hsla",
	 * ...)} (the space folded to an underscore over MT.HSLA = "HSLASteel"), and the
	 * FL.java:1077 createMolten walk over MT.LiCl (MT.java:1121 MOLTEN grant) whose id
	 * carries the underscore ({@code lithium_chloride_molten} ≠ the folded
	 * {@code lithiumchloride_molten} convention guess).
	 */
	private static final Map<String, String> LEGACY_FL_IDS = Map.of(
		"plastic"        , "plastic",
		"glass"          , "glass",
		"latex"          , "molten_latex",
		"hslasteel"      , "molten_hsla",
		"lithiumchloride", "lithium_chloride_molten");

	/**
	 * The fluid id a material resolves against: the legacy FL shorthand row when aliased,
	 * else the specOf convention (sanitized lowercase internal name + {@code "_molten"}).
	 * Public so the coverage census mirrors the exact resolution (no second table).
	 */
	public static String moltenIdForMaterial(String aMaterialName) {
		return LEGACY_FL_IDS.getOrDefault(aMaterialName, aMaterialName + "_molten");
	}

	private FluidBridge() {}

	/**
	 * The molten fluid for a material, or null when neither the alias seam nor a
	 * {@code gt6:<mat>_molten} row exists (unknown material) or the registry hasn't bound
	 * the row yet (the offline-JVM face).
	 */
	@Nullable
	public static net.minecraft.world.level.material.Fluid moltenFluidForMaterial(@Nullable String aMaterialName) {
		if (aMaterialName == null) return null;
		String tName = aMaterialName.toLowerCase(Locale.ROOT);
		//? if forge {
		net.minecraftforge.registries.RegistryObject<? extends Fluid> tEntry;
		if ("iron".equals(tName)) tEntry = GTFluids.IRON_MOLTEN;
		else tEntry = GTFluids.chemicalSource(moltenIdForMaterial(tName));
		return tEntry == null || !tEntry.isPresent() ? null : tEntry.get();
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Fluid, ? extends Fluid> tEntry;
		if ("iron".equals(tName)) tEntry = GTFluids.IRON_MOLTEN;
		else tEntry = GTFluids.chemicalSource(moltenIdForMaterial(tName));
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
