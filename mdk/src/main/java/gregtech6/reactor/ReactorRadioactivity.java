package gregtech6.reactor;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import gregtech6.items.armor.GT6HazardSets;

/**
 * The radiation application helper (task debt-reactor-b-2x2-be) — the port of
 * {@code UT.Entities.applyRadioactivity} (UT.java:3033-3053): radiation is a POTION
 * GROUP, not a damage source. The gates verbatim: positive level, living and alive,
 * neither UNDEAD nor ARTHROPOD ({@code getCreatureAttribute()}, the 1.20.1
 * {@code getMobType}/MobType leg and the 1.21.1 EntityTypeTags leg are the two
 * modern carriers of that check), and not wearing the full radiation hazmat set
 * ({@link GT6HazardSets}, the ported isWearingFullRadioHazmat).
 *
 * <p>The five vanilla potions and their constants verbatim (:3041-3045): slowness
 * (base 140), mining fatigue (150), nausea (130), weakness (150), hunger (130), each
 * {@code level * base * amount + max(0, current duration)} ticks at amplifier
 * {@code bind(0, 5, 5*level/7)}. The GT radiation potion arm (:3046-3048) runs only
 * when {@code PotionsGT.ID_RADIATION >= 0}; the port has no GT radiation potion, so
 * the declared fallback arm — WITHER, base 130, amplifier cap 5 (:3049-3050, the
 * exact upstream no-radiation-potion branch) — is the live one.
 *
 * <p>DECLARED CUT (the card's defer list): {@code EntityFoodTracker.changeRadiation}
 * (:3037-3038) — the food-radiation tracker is not ported, so the potion group is the
 * whole effect (upstream reaches it only when the tracker is absent, the same shape).
 */
public final class ReactorRadioactivity {

	private ReactorRadioactivity() {/**/}

	/**
	 * UT.Entities.applyRadioactivity (UT.java:3033-3053) — returns whether anything
	 * was applied (the upstream T/F contract the burst scan ignores).
	 */
	public static boolean apply(LivingEntity aEntity, int aLevel, int aAmountOfItems) {
		if (aLevel > 0 && aEntity.isAlive() && !isImmuneCreature(aEntity)
				&& !GT6HazardSets.isWearingFullSet(aEntity, GT6HazardSets.Hazard.RADIATION)) {
			int tAmplifier = Math.max(0, Math.min(5, (5 * aLevel) / 7)); // UT.Code.bind(0, 5, 5*level/7)
			applyPotion(aEntity, MobEffects.MOVEMENT_SLOWDOWN, aLevel * 140 * aAmountOfItems, tAmplifier); // :3041
			applyPotion(aEntity, MobEffects.DIG_SLOWDOWN     , aLevel * 150 * aAmountOfItems, tAmplifier); // :3042
			applyPotion(aEntity, MobEffects.CONFUSION         , aLevel * 130 * aAmountOfItems, tAmplifier); // :3043
			applyPotion(aEntity, MobEffects.WEAKNESS          , aLevel * 150 * aAmountOfItems, tAmplifier); // :3044
			applyPotion(aEntity, MobEffects.HUNGER            , aLevel * 130 * aAmountOfItems, tAmplifier); // :3045
			applyPotion(aEntity, MobEffects.WITHER            , aLevel * 130 * aAmountOfItems, tAmplifier); // :3049-3050 the no-radiation-potion fallback arm
			return true;
		}
		return false;
	}

	/** The undead/arthropod immunity gate (upstream :3034, {@code getCreatureAttribute}). */
	private static boolean isImmuneCreature(LivingEntity aEntity) {
		//? if forge {
		return aEntity.getMobType() == net.minecraft.world.entity.MobType.UNDEAD
				|| aEntity.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD;
		//?} else {
		/*// 21.1: MobType died in the 1.20.5 tag refactor — vanilla's own undead checks run
		//on EntityTypeTags (javap 21.1.209, UNDEAD/ARTHROPOD tag keys present).
		return aEntity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)
				|| aEntity.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD);
		 *///?}
	}

	/**
	 * UT.Entities.applyHeatDamage (UT.java:2999-3003) — the running core's contact burn
	 * (Core:303, damage 5): refused on Blazes, under fire resistance and inside the full
	 * heat hazmat set; the modern damage carrier is the {@code onFire} source (the
	 * GTBoilerTank contact-face precedent, TFC_DAMAGE_MULTIPLIER == 1).
	 */
	public static boolean applyHeatDamage(LivingEntity aEntity, float aDamage) {
		if (aDamage > 0 && aEntity.isAlive()
				&& !(aEntity instanceof net.minecraft.world.entity.monster.Blaze)
				&& !aEntity.hasEffect(MobEffects.FIRE_RESISTANCE)
				&& !GT6HazardSets.isWearingFullSet(aEntity, GT6HazardSets.Hazard.HEAT)) {
			aEntity.hurt(aEntity.damageSources().onFire(), aDamage);
			return true;
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the applyPotion half (UT.java:3055-3078 reduced to the face used here):
	// duration extended by any current effect, refused when non-positive; the modern
	// carrier is addEffect (the vanilla immunity handling inside). The per-leg fork is
	// only the effect reference type (bare MobEffect 1.20.1, Holder 21.1 — the
	// GTCrankBlockEntity.pot shape).
	// ---------------------------------------------------------------------------

	//? if forge {
	private static void applyPotion(LivingEntity aEntity, MobEffect aEffect, int aDuration, int aAmplifier) {
		if (aDuration <= 0) return; // upstream applyPotion: non-positive durations are refused
		MobEffectInstance tCurrent = aEntity.getEffect(aEffect);
		aEntity.addEffect(new MobEffectInstance(aEffect, aDuration + Math.max(0, tCurrent == null ? 0 : tCurrent.getDuration()), aAmplifier));
	}
	//?} else {
	/*private static void applyPotion(LivingEntity aEntity, net.minecraft.core.Holder<MobEffect> aEffect, int aDuration, int aAmplifier) {
		if (aDuration <= 0) return;
		MobEffectInstance tCurrent = aEntity.getEffect(aEffect);
		aEntity.addEffect(new MobEffectInstance(aEffect, aDuration + Math.max(0, tCurrent == null ? 0 : tCurrent.getDuration()), aAmplifier));
	}
	 *///?}
}
