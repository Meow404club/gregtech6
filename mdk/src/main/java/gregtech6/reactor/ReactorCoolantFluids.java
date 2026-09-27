package gregtech6.reactor;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluids;
import gregtech6.reactor.neutron.ReactorCoolant;

/**
 * The coolant fluid binding (task debt-reactor-b-2x2-be) — the port-registry half of
 * {@link ReactorCoolant}: the A-card table carries neutral row ids by design (MC-free
 * domain), this class binds each row to the actual port {@link Fluid} pair — the cold
 * input of the 11-entry fill whitelist (MultiTileEntityReactorCore.java:255-267) and
 * the hot output of the conversion branches (Core2x2.java:138-195, the upstream FL/MT
 * bindings; distilled water outputs STEAM ×160, CS.java:242, and thorium
 * salt outputs PLAIN molten LiCl — not the hot variant, Core2x2.java:190).
 *
 * <p>Resolution goes through {@link GTFluids#liveFluidSource} lazily (call-at-pour-time
 * only — the DeferredRegister handles are dead offline); the two OVERRIDE maps are the
 * offline test seams (populate before driving the BE, the bedrock-drill lubricant-lazy
 * -resolve precedent).
 */
public final class ReactorCoolantFluids {

	/** The port registry ids of one row: cold input first, hot output second. */
	private record Pair(String input, String hot) {/**/}

	/** The 11 rows in the {@link ReactorCoolant} declaration order. */
	private static final Map<ReactorCoolant, Pair> IDS = new EnumMap<>(ReactorCoolant.class);
	static {
		IDS.put(ReactorCoolant.IC2_COOLANT      , new Pair("ic2coolant"           , "ic2hotcoolant"));          // FL.Coolant_IC2 / _Hot
		IDS.put(ReactorCoolant.DISTILLED_WATER  , new Pair("distilled_water"      , "steam"));                  // FL.distw / FL.Steam ×160
		IDS.put(ReactorCoolant.THORIUM_SALT     , new Pair("thoriumsalt"          , "lithium_chloride_molten"));// FL.Thorium_Salt / PLAIN MT.LiCl (Core2x2:190)
		IDS.put(ReactorCoolant.MOLTEN_TIN       , new Pair("tin_molten"           , "hotmoltentin"));           // MT.Sn / FL.Hot_Molten_Tin
		IDS.put(ReactorCoolant.MOLTEN_SODIUM    , new Pair("sodium_molten"        , "hotmoltensodium"));        // MT.Na / FL.Hot_Molten_Sodium
		IDS.put(ReactorCoolant.SEMIHEAVY_WATER  , new Pair("semiheavywater"       , "hotsemiheavywater"));      // MT.HDO / FL.Hot_Semi_Heavy_Water
		IDS.put(ReactorCoolant.HEAVY_WATER      , new Pair("heavywater"           , "hotheavywater"));          // MT.D2O / FL.Hot_Heavy_Water
		IDS.put(ReactorCoolant.TRITIATED_WATER  , new Pair("tritiatedwater"       , "hottritiatedwater"));      // MT.T2O / FL.Hot_Tritiated_Water
		IDS.put(ReactorCoolant.MOLTEN_LICL      , new Pair("lithium_chloride_molten", "hotmoltenlicl"));        // MT.LiCl / FL.Hot_Molten_LiCl
		IDS.put(ReactorCoolant.HELIUM           , new Pair("helium"               , "hothelium"));              // MT.He gas / FL.Hot_Helium
		IDS.put(ReactorCoolant.CARBON_DIOXIDE   , new Pair("carbondioxide"        , "hotcarbondioxide"));       // MT.CO2 gas / FL.Hot_Carbon_Dioxide
	}

	/** The offline seams: row → resolved fluid, bypassing the DeferredRegister lookups. */
	public static final Map<ReactorCoolant, Fluid> INPUT_OVERRIDES = new EnumMap<>(ReactorCoolant.class);
	/** The offline seam of the hot side. */
	public static final Map<ReactorCoolant, Fluid> HOT_OVERRIDES = new EnumMap<>(ReactorCoolant.class);

	private ReactorCoolantFluids() {/**/}

	/**
	 * The live-registry resolve of one id — null when the DeferredRegister handles are
	 * not resolved (the offline JVM: an unresolved {@code RegistryObject.get()} throws),
	 * so the whitelist scan degrades to "row not live" instead of crashing.
	 */
	@Nullable
	private static Fluid safeSource(String aId) {
		try {
			return GTFluids.liveFluidSource(aId);
		} catch (RuntimeException aUnresolved) {
			return null;
		}
	}

	/** The cold input fluid of a row (live registries only; the test seam first). */
	@Nullable
	public static Fluid input(ReactorCoolant aCoolant) {
		Fluid tOverride = INPUT_OVERRIDES.get(aCoolant);
		if (tOverride != null) return tOverride;
		return safeSource(IDS.get(aCoolant).input());
	}

	/** The hot output fluid of a row (live registries only; the test seam first). */
	@Nullable
	public static Fluid hotOutput(ReactorCoolant aCoolant) {
		Fluid tOverride = HOT_OVERRIDES.get(aCoolant);
		if (tOverride != null) return tOverride;
		return safeSource(IDS.get(aCoolant).hot());
	}

	/** The hot-side stack of {@code aAmount} litres (the FL.X.make(tEnergy) shape), null when unresolvable. */
	@Nullable
	public static FluidStack hotStack(ReactorCoolant aCoolant, long aAmount) {
		Fluid tHot = hotOutput(aCoolant);
		return tHot == null || aAmount <= 0 ? null : new FluidStack(tHot, FluidTankGT.bindInt(aAmount));
	}

	/** Whether a stack carries steam (the output-tank capacity stretch, Core:62). */
	public static boolean isSteam(FluidStack aFluid) {
		if (aFluid.isEmpty()) return false;
		Fluid tSteam = HOT_OVERRIDES.get(ReactorCoolant.DISTILLED_WATER);
		if (tSteam == null) tSteam = safeSource("steam");
		return tSteam != null && aFluid.getFluid() == tSteam;
	}

	/** The coolant row of a fluid stack ({@code null} = off the whitelist) — the fill gate of Core:255-267. */
	@Nullable
	public static ReactorCoolant coolantOf(FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return null;
		for (ReactorCoolant tRow : ReactorCoolant.values()) {
			Fluid tInput = input(tRow);
			if (tInput != null && aFluid.getFluid() == tInput) return tRow;
		}
		return null;
	}
}
