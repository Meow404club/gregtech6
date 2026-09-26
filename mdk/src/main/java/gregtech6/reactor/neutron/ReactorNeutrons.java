package gregtech6.reactor.neutron;

/**
 * The reactor neutron/heat/durability arithmetic core (task debt-reactor-a-neutron-core):
 * the MC-free pure functions the B-card 2x2 core BE will call, each ported verbatim
 * from the upstream reactor classes with the source line pinned in the javadoc.
 * Everything here is deterministic integer/long math — no MC/forge types (acceptance ②).
 *
 * <p>Upstream sources (GT6 1.7.10): MultiTileEntityReactorCore2x2.java (tick loop),
 * MultiTileEntityReactorRodNuclear.java (fuel arithmetic), UT.Code.divup/bindInt,
 * CS.java:218-242 (coolant conversion constants).
 */
public final class ReactorNeutrons {

	/** Core2x2.java:54 — emission/reflection exchange runs at {@code SERVER_TIME % 20 == 19} ("the Sensors react to == 0, so this is the realistic fastest a Sensor can display"). */
	public static final int EXCHANGE_TICK = 19;
	/** Core2x2.java:224 — neutron account rollback one tick before the exchange ({@code mNeutronCounts[s] -= oNeutronCounts[s]} at {@code % 20 == 18}). */
	public static final int NEUTRON_ROLLBACK_TICK = 18;
	/** Core2x2.java:111 — ambient radiation burst gate at {@code % 20 == 10}. */
	public static final int RADIATION_BURST_TICK = 10;
	/** Core2x2.java:202 — meltdown doubles {@code tCalc} for the radiation burst ({@code tCalc *= 2}); the block explosion itself stays commented out upstream (:199) and is a B-card deferral. */
	public static final int MELTDOWN_STRENGTH_MULTIPLIER = 2;

	private ReactorNeutrons() {/**/}

	/** UT.Code.divup (UT.java:1697-1699) verbatim — divides but rounds up. */
	static long divup(long aNumber, long aDivider) {return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);}

	/** UT.Code.bindInt (UT.java:1565) verbatim — clamps a long into the int range before the cast. */
	static int bindInt(long aBoundValue) {return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, aBoundValue));}

	/** Fuel emission outcome: the bound emission returned to the exchange plus the self neutrons the rod adds onto its own count. */
	public record FuelEmission(int emitted, int selfAdded) {/**/}

	/**
	 * RodNuclear fuel emission, Nuclear.java:197-199 verbatim shape:
	 * the coolant-modulated {@code (self, other, div)} triple (Nuclear.java:179-196,
	 * via {@link ReactorCoolant#modulateEmission}) produces
	 * {@code self} onto the rod's own running count and
	 * {@code other + divup(max(oNeutronsOnRod - self, 0), div)} as the emission
	 * towards each of the 4 neighbours. {@code oNeutronsOnRod} is the core's
	 * {@code oNeutronCounts[slot]} — the previous tick's copy.
	 */
	public static FuelEmission fuelEmission(FuelRodSpec aFuel, ReactorCoolant aCoolant, int oNeutronsOnRod) {
		ReactorCoolant.EmissionParams tParams = aCoolant.modulateEmission(aFuel.self(), aFuel.other(), aFuel.div());
		long tEmission = tParams.other() + divup(Math.max((long) oNeutronsOnRod - tParams.self(), 0), tParams.div());
		return new FuelEmission(bindInt(tEmission), tParams.self());
	}

	/**
	 * RodNuclear neutron maximum, Nuclear.java:240-252 — the coolant MaxMode
	 * applied to the rod's base {@code mNeutronMax}.
	 */
	public static int neutronMaximum(FuelRodSpec aFuel, ReactorCoolant aCoolant) {
		return aCoolant.maxMode.apply(aFuel.max());
	}

	/**
	 * RodNuclear durability loss per tick, Nuclear.java:215-216 verbatim:
	 * {@code <= max ? 100 : divup(400 * n, max)}, times 4 when the rod is moderated
	 * ({@code oModerated}). Note the 100/t floor applies even at zero neutrons.
	 */
	public static long durabilityLoss(int oNeutronsOnRod, int aNeutronMaximum, boolean aModerated) {
		long tLoss = oNeutronsOnRod <= aNeutronMaximum ? 100 : divup(400L * oNeutronsOnRod, aNeutronMaximum);
		return aModerated ? tLoss * 4 : tLoss;
	}

	/**
	 * RodNuclear durability update, Nuclear.java:217 verbatim — loss larger than the
	 * remaining durability clamps to the {@code -1} sentinel; the depletion swap
	 * (meta → {@link FuelRodSpec#depletedId()}, NBT clear) fires on
	 * {@code durability <= 0} (Nuclear.java:220-224).
	 */
	public static long durabilityAfter(long aDurability, long aLoss) {
		return aLoss > aDurability ? -1 : aDurability - aLoss;
	}

	/** Breeder tick outcome: remaining neutron budget and whether the rod became its enriched product this tick. */
	public record BreederTick(long durability, boolean becameProduct) {/**/}

	/**
	 * RodBreeder breeding tick, Breeder.java:84-90 verbatim: the on-rod neutron count
	 * drains the budget each tick; {@code <= 0} swaps the item to the enriched product
	 * in place and resets the stored durability to 0.
	 */
	public static BreederTick breederTick(long aDurability, int oNeutronsOnRod) {
		long tDurability = aDurability - oNeutronsOnRod;
		return tDurability <= 0 ? new BreederTick(0, true) : new BreederTick(tDurability, false);
	}

	/** Heat divider tick outcome: stored energy after division and this tick's HU output ({@code oEnergy}). */
	public record HeatTick(long stored, long outputThisTick) {/**/}

	/**
	 * The Na/Sn heat divider application, Core2x2.java:122-134 verbatim shape: only
	 * the heat <em>generated this tick</em> is divided ({@code divup(generated, divider)},
	 * divider 6 for molten sodium, 3 for molten tin — :129-131) and added back onto the
	 * stored energy; the divided amount is the tick output {@code oEnergy}.
	 */
	public static HeatTick heatDividerTick(long aStoredBefore, long aGeneratedThisTick, ReactorCoolant aCoolant) {
		long tOutput = divup(aGeneratedThisTick, aCoolant.heatDivider);
		return new HeatTick(aStoredBefore + tOutput, tOutput);
	}

	/**
	 * Coolant conversion litres, Core2x2.java:139/144/149/... verbatim —
	 * {@code tEnergy = mEnergy / EU_PER_X}, floor division of the stored HU by the
	 * coolant's {@link ReactorCoolant#huPerLiter}.
	 */
	public static long convertibleLiters(long aStoredEnergy, ReactorCoolant aCoolant) {
		return aStoredEnergy / aCoolant.huPerLiter;
	}

	/**
	 * Stored energy after removing {@code aLiters} litres of coolant, Core2x2.java:141/
	 * 146/151/... verbatim — {@code mEnergy -= EU_PER_X * remove(tEnergy)}.
	 */
	public static long storedAfterCoolant(long aStoredEnergy, ReactorCoolant aCoolant, long aLiters) {
		return aStoredEnergy - (long) aCoolant.huPerLiter * aLiters;
	}

	/**
	 * Hot-side output amount for the converted litres — {@code liters} for every
	 * coolant except distilled water, which makes {@code liters * 160} steam
	 * (Core2x2.java:145, STEAM_PER_WATER CS.java:242).
	 */
	public static long hotOutputAmount(long aLiters, ReactorCoolant aCoolant) {
		return aLiters * aCoolant.outputMultiplier;
	}

	/**
	 * Ambient radiation level, Core2x2.java:108 verbatim —
	 * {@code tCalc = divup(oNeutronCounts[0..3] summed, 256)}; drives {@code mRunning},
	 * the geiger arm and the radiation burst.
	 */
	public static long ambientNeutronLevel(int oNeutrons0, int oNeutrons1, int oNeutrons2, int oNeutrons3) {
		return divup((long) oNeutrons0 + oNeutrons1 + oNeutrons2 + oNeutrons3, 256);
	}

	/**
	 * Ambient radiation strength at {@code aDistance}, Core2x2.java:115 verbatim —
	 * {@code bindInt((long)(tCalc - distance))}, the float difference truncated toward
	 * zero by the cast; negative results mean "no radiation" and stay caller-gated
	 * ({@code if (tStrength > 0)}, :116). Raycast shielding is an upstream TODO
	 * (Core2x2.java:110) and stays deferred.
	 */
	public static int ambientStrength(long aNeutronLevel, double aDistance) {
		return bindInt((long) (aNeutronLevel - aDistance));
	}

	/**
	 * Radiation amplifier for {@code UT.Entities.applyRadioactivity(entity, level, strength)},
	 * Core2x2.java:116 verbatim — {@code divup(tStrength, 10)}.
	 */
	public static int radiationAmplifier(int aStrength) {
		return (int) divup(aStrength, 10);
	}
}
