package gregtech6.reactor.neutron;

/**
 * The 8 reactor rod behaviours (task debt-reactor-a-neutron-core), one enum constant
 * per upstream rod class, each method citing the upstream line it ports:
 *
 * <ul>
 * <li>{@link #EMPTY} — MultiTileEntityReactorRodBase (empty Zr rod): transparent,
 *     Base.java:87-90 all defaults,
 * <li>{@link #ABSORBER} — RodAbsorber, Cd-In-Ag: 2 HU per neutron (Absorber.java:46),
 *     absorbs all reflections (:52-53),
 * <li>{@link #REFLECTOR} — RodReflector, Be: full reflection (Reflector.java:51),
 * <li>{@link #MODERATOR} — RodModerator, Graphite: reflects {@code oModeration * n}
 *     (Moderator.java:88), latches the touching-fuel count each 20t cycle (:74-78),
 *     always reports itself moderated (:92-94),
 * <li>{@link #FUEL} — RodNuclear, the 17 fuel rods: emission/durability/depletion live
 *     in {@link ReactorNeutrons} + {@link FuelRodSpec},
 * <li>{@link #BREEDER} — RodBreeder: per-side neutron loss (Breeder.java:96-98),
 *     half heat (:83), breeds by absorbing neutrons (:84-90),
 * <li>{@link #PRODUCT} — RodProduct (enriched rods): half heat (Product.java:55),
 *     absorbs all (:61-62),
 * <li>{@link #DEPLETED} — RodDepleted: pure Base defaults, neither accepts nor emits
 *     (Depleted.java:30-38).
 * </ul>
 *
 * <p>Stateful pieces that stay out (B-card BE concern): the moderator's
 * {@code mModeration/oModeration} shorts, the fuel {@code mModerated/oModerated} flags
 * and the durability longs — the pure functions here take them as parameters.
 */
public enum ReactorRodKind {
	EMPTY,
	ABSORBER,
	REFLECTOR,
	MODERATOR,
	FUEL,
	BREEDER,
	PRODUCT,
	DEPLETED;

	/**
	 * Heat added to {@code mEnergy} per tick by {@code getReactorRodNeutronReaction}:
	 * fuel 1 HU/neutron (Nuclear.java:205), absorber 2 (Absorber.java:46),
	 * breeder &amp; product integer-half (Breeder.java:83, Product.java:55), rest 0.
	 */
	public long reactionHeat(int aNeutronsOnRod) {
		return switch (this) {
			case FUEL -> aNeutronsOnRod;
			case ABSORBER -> 2L * aNeutronsOnRod;
			case BREEDER, PRODUCT -> aNeutronsOnRod / 2L;
			default -> 0L;
		};
	}

	/**
	 * Whether {@code getReactorRodNeutronReaction} returned true, i.e. the rod alone
	 * keeps the core {@code mRunning}: fuel/absorber/breeder/product return T
	 * (Nuclear.java:225, Absorber.java:47, Breeder.java:92, Product.java:56);
	 * base/reflector/moderator/depleted return F.
	 */
	public boolean reactionKeepsCoreRunning() {
		return this == FUEL || this == ABSORBER || this == BREEDER || this == PRODUCT;
	}

	/**
	 * Neutrons added onto the receiving rod's {@code mNeutronCounts} by a reflection
	 * hit of {@code aNeutrons}: fuel/absorber/product absorb all (Nuclear.java:235,
	 * Absorber.java:52, Product.java:61); breeder admits {@code n - loss} per side but
	 * only from unmoderated emitters (Breeder.java:97 — moderated fuel can't breed);
	 * empty/depleted/reflector/moderator absorb nothing.
	 */
	public int absorbedNeutrons(int aNeutrons, boolean aEmitterModerated, int aBreederLoss) {
		return switch (this) {
			case FUEL, ABSORBER, PRODUCT -> aNeutrons;
			case BREEDER -> !aEmitterModerated && aNeutrons > aBreederLoss ? aNeutrons - aBreederLoss : 0;
			default -> 0;
		};
	}

	/**
	 * Neutrons returned to the emitter by {@code getReactorRodNeutronReflection}:
	 * reflector bounces all (Reflector.java:51); moderator returns
	 * {@code oModeration * n} (Moderator.java:88); everything else 0.
	 */
	public int reflectedNeutrons(int aNeutrons, int aModeratorLatchedCount) {
		return switch (this) {
			case REFLECTOR -> aNeutrons;
			case MODERATOR -> aModeratorLatchedCount * aNeutrons;
			default -> 0;
		};
	}

	/**
	 * Whether a reflection hit with {@code aNeutrons > 0} increments the moderator's
	 * touching-fuel counter {@code mModeration++} (Moderator.java:84-87) — one count
	 * per emitting fuel rod per cycle, latched into {@code oModeration} at
	 * {@code t%20==19} (Moderator.java:74-78).
	 */
	public boolean countsModeratorTouch() {
		return this == MODERATOR;
	}

	/**
	 * Whether being hit by a moderated emitter sets this rod's {@code mModerated = T}
	 * (Nuclear.java:231-234) — only fuel rods take moderation from a moderator touch.
	 */
	public boolean moderatedByTouch() {
		return this == FUEL;
	}

	/**
	 * Whether {@code isModerated} of the rod item reports true (Moderator.java:92-94);
	 * fuel rods report their {@code oModerated} flag instead (Nuclear.java:255-257).
	 */
	public boolean alwaysModerated() {
		return this == MODERATOR;
	}
}
