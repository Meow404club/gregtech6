package gregtech6.reactor.neutron;

/**
 * The reactor core coolant table (task debt-reactor-a-neutron-core): the 11-entry
 * fill whitelist of the upstream abstract core (MultiTileEntityReactorCore.java:255-267,
 * same order as {@code getFluidTankFillable2}/{@code funnelFill}) with every per-coolant
 * arithmetic rule the rods and the tick loop apply to it, extracted as table rows:
 *
 * <ul>
 * <li>emission modulation of fuel rods (MultiTileEntityReactorRodNuclear.java:179-196),
 * <li>neutron maximum modulation (Nuclear.java:240-252, {@link MaxMode}),
 * <li>fuel moderation flag (Nuclear.java:208-214 — distilled/semiheavy/heavy/tritiated
 *     water force {@code mModerated = oModerated = true}),
 * <li>per-tick heat divider — molten sodium /6, molten tin /3 (Core2x2.java:129-131),
 * <li>hot-output conversion energy per litre (Core2x2.java:138-195, constants
 *     CS.java:218-238) plus the output identity/multiplier for the B-card tank stage
 *     (water makes {@code tEnergy * 160} steam, CS.java:242; thorium salt makes plain
 *     — not hot — molten LiCl, Core2x2.java:190).
 * </ul>
 *
 * <p>MC-free by construction: rows carry neutral string ids, the B-card 2x2 BE maps
 * them to actual {@code Fluid}s. No {@code net.minecraft*}/{@code net.minecraftforge*}
 * import anywhere in this package (acceptance ②).
 */
public enum ReactorCoolant {

	/** FL.Coolant_IC2 — Industrial Coolant. */
	IC2_COOLANT("ic2_coolant", 20, 1, 4, 4, false, false, 0, 2, MaxMode.PLAIN, false, "ic2_coolant_hot", 1),
	/** FL.distw — Distilled Water, output is steam ×160. */
	DISTILLED_WATER("distilled_water", 80, 1, 1, 1, false, false, 0, 1, MaxMode.PLAIN, true, "steam", 160),
	/** FL.Thorium_Salt — EU_PER_THORIUM_SALT suspiciously large, see below. Output is plain molten LiCl. */
	THORIUM_SALT("thorium_salt", 2_560_000, 1, 1, 1, true, true, -1, 1, MaxMode.TIMES4, false, "licl", 1),
	/** MT.Sn liquid — molten Tin, heat divider 3. */
	MOLTEN_TIN("molten_tin", 40, 3, 1, 1, false, false, -1, 1, MaxMode.PLAIN, false, "hot_molten_tin", 1),
	/** MT.Na liquid — molten Sodium, heat divider 6. */
	MOLTEN_SODIUM("molten_sodium", 30, 6, 1, 1, false, false, -1, 1, MaxMode.PLAIN, false, "hot_molten_sodium", 1),
	/** MT.HDO liquid — Semiheavy Water. */
	SEMIHEAVY_WATER("semiheavy_water", 40, 1, 1, 1, false, false, 0, 1, MaxMode.PLAIN, true, "hot_semiheavy_water", 1),
	/** MT.D2O liquid — Heavy Water. */
	HEAVY_WATER("heavy_water", 50, 1, 1, 1, false, false, 0, 1, MaxMode.DIV8, true, "hot_heavy_water", 1),
	/** MT.T2O liquid — Tritiated Water. */
	TRITIATED_WATER("tritiated_water", 60, 1, 1, 1, false, false, 0, 1, MaxMode.DIV16, true, "hot_tritiated_water", 1),
	/** MT.LiCl liquid — molten Lithium Chloride. */
	MOLTEN_LICL("molten_licl", 15, 1, 5, 1, false, true, 0, 1, MaxMode.PLUS_DIV4, false, "hot_molten_licl", 1),
	/** MT.He gas — Helium. */
	HELIUM("helium", 30, 1, 1, 1, false, true, 0, 1, MaxMode.PLAIN, false, "hot_helium", 1),
	/** MT.CO2 gas — Carbon Dioxide. */
	CARBON_DIOXIDE("carbon_dioxide", 20, 1, 3, 1, false, false, 0, 1, MaxMode.PLAIN, false, "hot_carbon_dioxide", 1);

	/** The neutron-maximum modulation of Nuclear.java:240-252, one mode per coolant branch. */
	public enum MaxMode {
		/** plain {@code mNeutronMax} */
		PLAIN {
			@Override public int apply(int aMax) {return aMax;}
		},
		/** LiCl: {@code max + divup(max, 4)} */
		PLUS_DIV4 {
			@Override public int apply(int aMax) {return aMax + (int) divup(aMax, 4);}
		},
		/** Thorium Salt: {@code max * 4} */
		TIMES4 {
			@Override public int apply(int aMax) {return aMax * 4;}
		},
		/** D2O: {@code divup(max, 8)} */
		DIV8 {
			@Override public int apply(int aMax) {return (int) divup(aMax, 8);}
		},
		/** T2O: {@code divup(max, 16)} */
		DIV16 {
			@Override public int apply(int aMax) {return (int) divup(aMax, 16);}
		};

		/** UT.Code.divup (UT.java:1697-1699) shape — divides but rounds up. */
		static long divup(long aNumber, long aDivider) {return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);}

		/** Applies the modulation to a rod's base neutron maximum. */
		public abstract int apply(int aMax);
	}

	/** Neutral row id; the B card maps this to the actual fluid pair. */
	public final String id;
	/** HU needed per converted litre (CS.java:218-238; EU_PER_THORIUM_SALT is the odd one out). */
	public final int huPerLiter;
	/** Per-tick heat divider of the tick loop (Core2x2.java:129-131): Na 6, Sn 3, else 1. */
	public final int heatDivider;
	/** Whether this coolant forces fuel rods moderated (Nuclear.java:208-214). */
	public final boolean moderatesFuel;
	/** Fuel {@code mNeutronSelf} multiplier (Nuclear.java:180/184/189). */
	public final int selfMultiplier;
	/** Fuel {@code mNeutronOther} multiplier (Nuclear.java:180, IC2 ×4 only). */
	public final int otherMultiplier;
	/** Fuel {@code mNeutronSelf} forced to 0 (Nuclear.java:192, Thorium Salt). */
	public final boolean selfZero;
	/** Fuel {@code mNeutronOther} reduced by {@code divup(other, 2)} (Nuclear.java:186/188/191). */
	public final boolean otherHalved;
	/** Additive {@code mNeutronDiv} delta (Nuclear.java:193/195: ThSalt/Sn/Na -1). */
	public final int divAdd;
	/** Multiplicative {@code mNeutronDiv} factor (Nuclear.java:182: IC2 ×2). */
	public final int divMultiplier;
	/** Neutron maximum modulation (Nuclear.java:240-252). */
	public final MaxMode maxMode;
	/** Hot-side output identity (neutral id, Core2x2.java:138-195). */
	public final String hotOutputId;
	/** Output amount multiplier — steam ×160 for water (CS.java:242), 1 otherwise. */
	public final int outputMultiplier;

	ReactorCoolant(String aId, int aHuPerLiter, int aHeatDivider, int aSelfMultiplier, int aOtherMultiplier, boolean aSelfZero, boolean aOtherHalved, int aDivAdd, int aDivMultiplier, MaxMode aMaxMode, boolean aModeratesFuel, String aHotOutputId, int aOutputMultiplier) {
		id = aId;
		huPerLiter = aHuPerLiter;
		heatDivider = aHeatDivider;
		selfMultiplier = aSelfMultiplier;
		otherMultiplier = aOtherMultiplier;
		selfZero = aSelfZero;
		otherHalved = aOtherHalved;
		divAdd = aDivAdd;
		divMultiplier = aDivMultiplier;
		maxMode = aMaxMode;
		moderatesFuel = aModeratesFuel;
		hotOutputId = aHotOutputId;
		outputMultiplier = aOutputMultiplier;
	}

	/**
	 * The fuel emission modulation, Nuclear.java:179-196 shape: each coolant takes
	 * exactly one upstream branch, so the composed table row reproduces the branch
	 * outcome for the rod's base {@code (self, other, div)} triple.
	 */
	public EmissionParams modulateEmission(int aSelf, int aOther, int aDiv) {
		int tSelf = aSelf * selfMultiplier;
		if (selfZero) tSelf = 0;
		int tOther = aOther * otherMultiplier;
		if (otherHalved) tOther -= (int) MaxMode.divup(aOther, 2);
		return new EmissionParams(tSelf, tOther, aDiv * divMultiplier + divAdd);
	}

	/** The modulated {@code (self, other, div)} triple of {@link #modulateEmission}. */
	public record EmissionParams(int self, int other, int div) {/**/}
}
