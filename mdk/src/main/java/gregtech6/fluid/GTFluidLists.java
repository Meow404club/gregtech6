package gregtech6.fluid;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The GT6 fluid name lists (task p13-steam-proof-repay spec ①) — the 1.20.1 counterpart
 * of the upstream registration-set machinery: FL.java:571-575 adds every fluid literal
 * into the {@code Collection<String>} sets its enum-ctor receives, and the consumers
 * query the sets by NAME ({@code UT.powerconducting}, UT.java:187 =
 * {@code FluidsGT.POWER_CONDUCTING.contains(aFluid.getName())}).
 *
 * <p>Name sets, not Fluid objects: the entries are plain registry-path strings, so the
 * lists carry ZERO coupling to the {@link GTFluids} static-init order (the javadoc
 * lesson at GTFluids.java:60-63 — at MOD construction the MT.* fields are still null;
 * name strings have no such hazard). The 1.20.1 name of a fluid is the registry path
 * ({@code ForgeRegistries.FLUIDS.getKey(f).getPath()}, {@link #name}); the predicates
 * take the String so the whole face stays offline-testable.
 *
 * <p>Seeds (the task-card literals):
 * <ul>
 * <li>{@code steam} in BOTH sets — FL.java:85 {@code Steam("steam", SIMPLE, GAS, STEAM,
 *     POWER_CONDUCTING)}; the IC2 compat names ic2steam/ic2superheatedsteam (:86-87) are
 *     NOT seeded (no such fluids in the port — the register API is the seam if compat
 *     aliases ever materialize);</li>
 * <li>{@code natural_gas} in GAS only — the port's own registry path
 *     (GTFluids NATURAL_GAS, the {@code gt6:natural_gas} registration). Upstream names
 *     the same fluid {@code gas_natural_gas} with old names {@code naturalgas}/
 *     {@code gas.natural} (FL.java:410, the three-name ctor :583-591 adds all three to
 *     GAS) — the port registered it as {@code natural_gas} (task p5-barrel-side-rules),
 *     and the lists key on OUR registry paths, so the port name is the seed. The
 *     upstream CS.java:1534 pre-seed {@code "rc fusion plasma"} stays unported (a
 *     RotaryCraft compat name, no such fluid — pool).</li>
 * <li>the gaseous chemical seeds (task p30-pool-gas-seeds-13) in GAS — upstream,
 *     {@code FL.create} AUTO-adds every STATE_GASEOUS fluid to {@code FluidsGT.GAS}
 *     (FL.java:1105), so every port fluid born gaseous is a GAS member by the same rule:
 *     the thirteen-row createGas closure + the four cracked hydrocarbons (both from task
 *     p29-w4-f1-chemicals, Loader_Fluids.java:45-48 and the :660 walk over the GASES-flag
 *     materials, MT.java:380/:383/:395-398/:406/:425/:443/:476/:1034-1037) + the p24
 *     standalone {@code chlorine} row (MT.java:405 diatomicgas, the same walk). The
 *     plasmas are the deliberate NON-members (Loader_Fluids.java:40-41 — STATE_PLASMA
 *     routes to {@code FluidsGT.PLASMA}, FL.java:1106, never GAS); the oil/liquidoxygen
 *     rows ride STATE_LIQUID.</li>
 * </ul>
 */
public final class GTFluidLists {

	/** The FL GAS set (upstream FluidsGT.GAS): every fluid the :180 gas tick gate and the :251 item-fill gas gate refuse. */
	public static final Set<String> GAS = new HashSet<>();

	/** The FL POWER_CONDUCTING set (upstream FluidsGT.POWER_CONDUCTING): every fluid the :184 allowFluid gate voids on tick and the :250 item-fill gate refuses. */
	public static final Set<String> POWER_CONDUCTING = new HashSet<>();

	static {
		register("steam", GAS, POWER_CONDUCTING); // FL.java:85 — SIMPLE, GAS, STEAM, POWER_CONDUCTING
		register("natural_gas", GAS); // the port registry path (GTFluids NATURAL_GAS); upstream FL.java:410 gas_natural_gas

		// task p30-pool-gas-seeds-13 — the gaseous chemical seeds. Upstream the FL.create
		// STATE_GASEOUS arm auto-adds the name to FluidsGT.GAS (FL.java:1105); the port rows
		// below were born gaseous (the spec gas flag) but the list never followed. Port
		// registry paths, one per birth row:
		// the gas closure — the Loader_Fluids.java:660 createGas walk over the GASES-flag
		// materials (the :1128-1136 density rule lives in GTFluids CHEMICAL_SPECS, zero
		// changes here — this list carries names only)
		register("hydrogen"       , GAS); // MT.java:380 diatomicgas
		register("nitrogen"       , GAS); // MT.java:395 diatomicgas
		register("oxygen"         , GAS); // MT.java:396 diatomicgas
		register("fluorine"       , GAS); // MT.java:397 diatomicgas
		register("helium"         , GAS); // MT.java:383 noblegas
		register("neon"           , GAS); // MT.java:398 noblegas
		register("argon"          , GAS); // MT.java:406 noblegas
		register("krypton"        , GAS); // MT.java:425 noblegas
		register("xenon"          , GAS); // MT.java:443 noblegas
		register("radon"          , GAS); // MT.java:476 noblegas
		register("methane"        , GAS); // MT.java:1037 gaschemelec, the GASES flag
		register("carbondioxide"  , GAS); // MT.java:1035 gaschemelec
		register("carbonmonoxide" , GAS); // MT.java:1034 gaschemelec
		// the cracked hydrocarbons — Loader_Fluids.java:45-48, the four-arg create at state 2
		register("propane"        , GAS); // :45
		register("butane"         , GAS); // :46
		register("propylene"      , GAS); // :47
		register("ethylene"       , GAS); // :48
		// the p24 standalone row — MT.java:405 diatomicgas, the same :660 walk
		register("chlorine"       , GAS); // density −100, the :1105 gaseous carrier shape
	}

	private GTFluidLists() {}

	/**
	 * The FL.java:571-575 enum-ctor body as a registration API — the exact
	 * {@code for (Collection<String> aFluidSet : aFluidSets) aFluidSet.add(mName);} shape.
	 * Idempotent by set semantics.
	 */
	@SafeVarargs
	public static void register(String aName, Collection<String>... aSets) {
		for (Collection<String> aSet : aSets) aSet.add(aName);
	}

	/** The FL.gas name-set form: is the registry-path name in the GAS list? */
	public static boolean isGas(@Nullable String aFluidName) {
		return aFluidName != null && GAS.contains(aFluidName);
	}

	/** The UT.powerconducting name-set form (UT.java:187): is the registry-path name in the POWER_CONDUCTING list? */
	public static boolean isPowerConducting(@Nullable String aFluidName) {
		return aFluidName != null && POWER_CONDUCTING.contains(aFluidName);
	}

	/**
	 * The 1.20.1 {@code Fluid.getName()} — the fluid's registry path, empty for a null or
	 * empty stack or an unregistered fluid. This is the key the two lists are seeded with.
	 */
	public static String name(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return "";
		ResourceLocation tKey = ForgeRegistries.FLUIDS.getKey(aFluid.getFluid());
		return tKey == null ? "" : tKey.getPath();
	}
}
