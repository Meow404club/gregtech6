/**
 * Part of the GregTech 6 (1.7.10) modernization port (task p26-crucible-physics-smeltery).
 * The shared crucible physics core: the material-pile thermodynamics of the small Smeltery
 * (gregtech/tileentity/tools/MultiTileEntitySmeltery.java) and the large Crucible
 * (multiblocks/MultiTileEntityCrucible.java) — two upstream tick bodies that are ~90%
 * identical (Smeltery :186-244/:271-277/:301-313 ≈ Crucible :236-294/:321-327/:353-365,
 * the arch ruling tasks.p26-arch-crucible-chain key_rulings ②) — expressed as MC-free pure
 * functions over {@link OreDictMaterialStack} lists so both forms consume the same code
 * and the whole truth table runs in the root JUnit suite.
 *
 * <p>This file is part of GregTech.
 *
 * <p>GregTech is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * <p>Everything in here is a direct transcription of the upstream arithmetic: the thermal
 * mass equilibrium (:330-352), the 1 HU per kg/100 heating step (:301-313), the alloy scan
 * (:186-244), the evaporation/acid/phase-gate loop (:246-284) and the ore direct-smelt
 * projection (:167-183). World effects (sounds, entity damage, fire blocks, the lava
 * swap) are NOT in here — the tick loop returns the measured outcome and the mdk Block
 * Entity applies it to the World (the EnergyBridge seam discipline of p7-d1-energy-core).
 */
package gregapi.util;

import static gregapi.data.CS.U;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;

public final class CruciblePhysics {

	private CruciblePhysics() {}

	// ====================================================================================
	// the parameterization face (arch ruling ②: the small 16U/1.25/6/3 and the large
	// 432U/1.10/8/5 form consume the SAME functions)
	// ====================================================================================

	/**
	 * The per-form physics constants. SMALL = MultiTileEntitySmeltery.java:76-78
	 * (MAX_AMOUNT 16*U, HEAT_RESISTANCE_BONUS 1.25, the explode scale ceiling 6 and
	 * GAS_RANGE 3 from the :257-262/:318-319 literals); LARGE = the card-B consumption
	 * face (MultiTileEntityCrucible 432*U / 1.10 / 8 / 5) pinned here so the signature
	 * this card promised stays stable across the three crucible cards.
	 */
	public record Params(long maxAmount, double heatResistanceBonus, long explosionPower, long gasRange, long kgPerEnergy) {
		public static final Params SMALL = new Params(16 * U, 1.25, 6, 3, 100);
		public static final Params LARGE = new Params(432 * U, 1.10, 8, 5, 100);
	}

	// ====================================================================================
	// the arithmetic primitive scale (units lives in UT.Code as the single in-repo definition)
	// ====================================================================================

	/** A Value for a Scale between 0 and aMax. UT.java:1534-1538 verbatim. */
	public static long scale(long aValue, long aMax, long aScale, boolean aInvert) {
		long rScale = (aValue <= 0 ? 0 : aValue >= aMax ? aScale : aScale <= 2 ? 1 : 1 + (aValue * (aScale-1)) / aMax);
		return aInvert ? aScale - rScale : rScale;
	}

	// ====================================================================================
	// weight / total (upstream OM.total / OM.weight projections, OM.java:71-95)
	// ====================================================================================

	/** Upstream OM.total — the summed material amounts of a list (OM.java:71-80). */
	public static long total(List<OreDictMaterialStack> aList) {
		if (aList == null) return 0;
		long rTotal = 0;
		for (OreDictMaterialStack tStack : aList) if (tStack != null) rTotal += tStack.mAmount;
		return rTotal;
	}

	/** Upstream OM.weight — the summed physical weight of a list in kg (OM.java:82-95). */
	public static double weight(List<OreDictMaterialStack> aList) {
		if (aList == null) return 0;
		double rWeight = 0;
		for (OreDictMaterialStack tStack : aList) if (tStack != null) rWeight += tStack.weight();
		return rWeight;
	}

	// ====================================================================================
	// the thermal mass equilibrium (upstream addMaterialStacks :330-352)
	// ====================================================================================

	/** The :330-352 outcome: whether the material fit into the crucible, and the new temperature. */
	public record AddResult(boolean added, long temperature) {}

	/**
	 * The upstream addMaterialStacks (:330-352) as a pure function.
	 *
	 * <p>Capacity gate (:331): total(existing) + total(incoming) must fit aParams.maxAmount.
	 * Thermal equilibrium (:332-333): the classic heat-mass blend
	 * {@code newTemp = aEnvTemperature + sign * units(|oldTemp - aEnvTemperature|, w1+w2, w1, F)}
	 * where w1 = the shell + existing content weight and w2 = the incoming weight — the
	 * blend rounds DOWN through {@link #units} exactly like upstream (aRoundUp = F).
	 * The entry phase-gates (:334-347): a stack entering above its own melting point
	 * converts to mTargetSmelting when it arrives cold, and a stack entering below its
	 * melting point converts to mTargetSolidifying when it arrives hot — the upstream
	 * "iron dust enters a hot crucible becomes molten iron / molten iron enters a cold
	 * crucible becomes an ingot" arm, with the cross-conversion amounts scaled through
	 * {@link #units}(aAmount, U, target.mAmount, F) (:337/:343).
	 *
	 * @param aContent       the crucible content, mutated in place on success
	 * @param aIncoming      the material to add (NOT mutated — upstream iterates clones)
	 * @param aTemperature   the temperature of the INCOMING material (the pour/env temperature)
	 * @param aOwnTemperature the crucible's current temperature
	 * @param aShellWeight   the crucible shell weight in kg (upstream mMaterial.getWeight(U*7), :113)
	 * @param aParams        the form parameters (maxAmount)
	 * @return the AddResult (added + the crucible's new temperature)
	 */
	public static AddResult addStacks(List<OreDictMaterialStack> aContent, List<OreDictMaterialStack> aIncoming, long aTemperature, long aOwnTemperature, double aShellWeight, Params aParams) {
		if (total(aContent) + total(aIncoming) > aParams.maxAmount()) return new AddResult(false, aOwnTemperature); // :331
		double tWeight1 = weight(aContent) + aShellWeight, tWeight2 = weight(aIncoming); // :332
		long tNewTemperature = aOwnTemperature;
		if (tWeight1 + tWeight2 > 0) {
			// :333 verbatim — the +1/-1 sign rides the old-vs-incoming temperature comparison
			tNewTemperature = aTemperature + (aOwnTemperature > aTemperature ? +1 : -1) * UT.Code.units(Math.abs(aOwnTemperature - aTemperature), (long)(tWeight1 + tWeight2), (long)tWeight1, false);
		}
		for (OreDictMaterialStack tMaterial : aIncoming) { // :334-347
			if (tMaterial == null) continue;
			if (tNewTemperature >= tMaterial.mMaterial.mMeltingPoint) {
				if (aTemperature < tMaterial.mMaterial.mMeltingPoint) {
					// :337 — hot crucible + cold solid: convert to the smelting target
					new OreDictMaterialStack(tMaterial.mMaterial.mTargetSmelting.mMaterial, UT.Code.units(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSmelting.mAmount, false)).addToList(aContent);
				} else {
					tMaterial.addToList(aContent); // :339
				}
			} else {
				if (aTemperature >= tMaterial.mMaterial.mMeltingPoint) {
					// :343 — cold crucible + hot melt: convert to the solidifying target
					new OreDictMaterialStack(tMaterial.mMaterial.mTargetSolidifying.mMaterial, UT.Code.units(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSolidifying.mAmount, false)).addToList(aContent);
				} else {
					tMaterial.addToList(aContent); // :345
				}
			}
		}
		return new AddResult(true, tNewTemperature);
	}

	// ====================================================================================
	// the heating step (upstream :301-313)
	// ====================================================================================

	/** The :301-313 outcome: the new temperature, the remaining energy buffer and the cooldown counter. */
	public record TickResult(long temperature, long energy, int cooldown) {}

	/**
	 * Upstream tRequiredEnergy (:301): {@code 1 + weight / kgPerEnergy} — one Kelvin per
	 * kgPerEnergy kg per HU, always at least 1 HU per Kelvin.
	 */
	public static long requiredEnergy(double aWeight, long aKgPerEnergy) {
		return 1 + (long)(aWeight / aKgPerEnergy);
	}

	/**
	 * The upstream heating/cooling tick (:301-313) as a pure step function.
	 *
	 * <ol>
	 * <li>the window (:303): every tick with a running countdown ticks it down;</li>
	 * <li>charge (:305-309): as long as the buffer covers the required energy, the
	 *     temperature rises one Kelvin per step, the buffer pays, and the cooldown
	 *     resets to 100;</li>
	 * <li>cooling (:311): after the window expires, every 10 ticks the temperature
	 *     moves one Kelvin toward the environment;</li>
	 * <li>floor (:313): the temperature never drops below min(200, envTemp) — a
	 *     nether-roof-hot environment keeps the crucible hot.</li>
	 * </ol>
	 *
	 * @param aTemperature the current temperature
	 * @param aEnergy      the energy buffer (HU)
	 * @param aEnvTemperature the ambient temperature (the WD.envTemp seam answer)
	 * @param aWeight      the shell + content weight in kg
	 * @param aCooldown    the current cooldown counter
	 * @param aKgPerEnergy the form's KG_PER_ENERGY
	 */
	public static TickResult tickHeat(long aTemperature, long aEnergy, long aEnvTemperature, double aWeight, int aCooldown, long aKgPerEnergy) {
		long tRequiredEnergy = requiredEnergy(aWeight, aKgPerEnergy); // :301
		long tConversions = aEnergy / tRequiredEnergy;
		long rEnergy = aEnergy, rTemperature = aTemperature;
		int rCooldown = aCooldown > 0 ? aCooldown - 1 : aCooldown; // :303
		if (tConversions != 0) { // :305-309
			rEnergy -= tConversions * tRequiredEnergy;
			rTemperature += tConversions;
			rCooldown = 100;
		}
		if (rCooldown <= 0) { // :311 — the out-of-supply cool-down (and warm-up toward a hot environment)
			rCooldown = 10;
			if (rTemperature > aEnvTemperature) rTemperature--;
			if (rTemperature < aEnvTemperature) rTemperature++;
		}
		rTemperature = Math.max(rTemperature, Math.min(200, aEnvTemperature)); // :313
		return new TickResult(rTemperature, rEnergy, rCooldown);
	}

	// ====================================================================================
	// the temperature limits (upstream getTemperatureMax :360-362 + the :324预警门)
	// ====================================================================================

	/** Upstream getTemperatureMax (:360-362): the shell material melting point × the form's heat resistance bonus. */
	public static long temperatureMax(OreDictMaterial aShell, double aHeatResistanceBonus) {
		return (long)(aShell.mMeltingPoint * aHeatResistanceBonus);
	}

	/** Upstream :324 — the melt-down WARNING state: within 100 K of the ceiling. */
	public static boolean isMeltDownWarning(long aTemperature, long aTemperatureMax) {
		return aTemperature + 100 > aTemperatureMax;
	}

	// ====================================================================================
	// the alloy scan (upstream :186-244)
	// ====================================================================================

	/** The :186-244 outcome: which alloy formed and how many recipe rounds it consumed. No match = alloy null. */
	public record AlloyResult(OreDictMaterial alloy, long conversions) {}

	/**
	 * The upstream alloy scan (:186-244) as a pure function, zero recipe-table lookups —
	 * every candidate comes off the material graph (mAlloyComponentReferences ←
	 * addAlloyingRecipe wiring, mAlloyCreationRecipes = the component configurations).
	 *
	 * <p>Gate ladder, upstream-verbatim:
	 * <ul>
	 * <li>:194 — a content stack only scans when the crucible is at/above its melting point;</li>
	 * <li>:195 — the candidate alloy must itself be molten (its melting point at most the
	 *     crucible temperature) and gets checked once per tick;</li>
	 * <li>:199 — the needed amounts are the undivided components clamped to at least 1 unit;</li>
	 * <li>:203-219 — tNonMolten counts components below their melting point and every
	 *     component must be present in the content, tracking the max whole conversions;</li>
	 * <li>:221 — at most ONE component may be non-molten (the "One may not be molten
	 *     yet" rule) and conversions must be positive;</li>
	 * <li>:222-227 — the preferred recipe is the one maximizing
	 *     conversions × commonDivider (the recipe-yield product).</li>
	 * </ul>
	 *
	 * <p>This function does NOT consume the content — the caller applies
	 * {@link #applyAlloy} on success (the upstream :234-244 half).
	 *
	 * @param aContent     the crucible content (read-only here)
	 * @param aTemperature the crucible temperature
	 * @return the preferred alloy result, or alloy == null when nothing can form
	 */
	public static AlloyResult alloyScan(List<OreDictMaterialStack> aContent, long aTemperature) {
		Set<OreDictMaterial> tAlreadyCheckedAlloys = new HashSet<>(); // :186

		OreDictMaterial tPreferredAlloy = null; // :188
		IOreDictConfigurationComponent tPreferredRecipe = null;
		long tMaxConversions = 0; // :190

		for (OreDictMaterialStack tMaterial : aContent) {
			if (tMaterial == null || aTemperature < tMaterial.mMaterial.mMeltingPoint) continue; // :194
			for (OreDictMaterial tAlloy : tMaterial.mMaterial.mAlloyComponentReferences) {
				if (!tAlreadyCheckedAlloys.add(tAlloy) || aTemperature < tAlloy.mMeltingPoint) continue; // :195
				for (IOreDictConfigurationComponent tAlloyRecipe : alloyRecipes(tAlloy)) { // :196
					List<OreDictMaterialStack> tNeededStuff = new java.util.ArrayList<>();
					for (OreDictMaterialStack tComponent : tAlloyRecipe.getUndividedComponents()) {
						tNeededStuff.add(new OreDictMaterialStack(tComponent.mMaterial, Math.max(1, tComponent.mAmount / U))); // :199
					}

					if (tNeededStuff.isEmpty()) continue; // :202
					int tNonMolten = 0; // :203

					boolean tBreak = false;
					long tConversions = Long.MAX_VALUE; // :206
					for (OreDictMaterialStack tComponent : tNeededStuff) {
						if (aTemperature < tComponent.mMaterial.mMeltingPoint) tNonMolten++; // :208

						tBreak = true;
						for (OreDictMaterialStack tContent : aContent) {
							if (tContent != null && tContent.mMaterial == tComponent.mMaterial) {
								tConversions = Math.min(tConversions, tContent.mAmount / tComponent.mAmount); // :213
								tBreak = false;
								break;
							}
						}
						if (tBreak) break; // :218
					}

					if (!tBreak && tNonMolten <= 1 && tConversions > 0) { // :221
						if (tPreferredAlloy == null || tPreferredRecipe == null || tConversions * tAlloyRecipe.getCommonDivider() > tMaxConversions * tPreferredRecipe.getCommonDivider()) { // :222
							tMaxConversions = tConversions;
							tPreferredRecipe = tAlloyRecipe;
							tPreferredAlloy = tAlloy;
						}
					}
				}
			}
		}
		return new AlloyResult(tPreferredAlloy, tPreferredRecipe == null ? 0 : tMaxConversions);
	}

	/** Read-only copy of mAlloyCreationRecipes for the scan. */
	private static List<IOreDictConfigurationComponent> alloyRecipes(OreDictMaterial aAlloy) {
		return new java.util.ArrayList<>(aAlloy.mAlloyCreationRecipes);
	}

	/**
	 * The upstream :234-244 consumption half: subtract the per-component amounts of
	 * {@code aConversions} rounds from the content and add the alloy product back
	 * (commonDivider × conversions units). Call only after a non-null {@link #alloyScan}.
	 */
	public static void applyAlloy(List<OreDictMaterialStack> aContent, OreDictMaterial aAlloy, long aConversions) {
		if (aAlloy == null || aAlloy.mComponents == null || aConversions <= 0) return;
		List<OreDictMaterialStack> tComponents = aAlloy.mComponents.getUndividedComponents();
		for (OreDictMaterialStack tComponent : tComponents) { // :235-242
			for (OreDictMaterialStack tContent : aContent) {
				if (tContent != null && tContent.mMaterial == tComponent.mMaterial) {
					tContent.mAmount -= UT.Code.units(aConversions, U, tComponent.mAmount, true); // :238 UT.Code.units_(maxConversions, U, tComponent.mAmount, T)
					break;
				}
			}
		}
		new OreDictMaterialStack(aAlloy, aAlloy.mComponents.getCommonDivider() * aConversions).addToList(aContent); // :243
	}

	// ====================================================================================
	// the evaporation / acid / phase-gate loop (upstream :246-284)
	// ====================================================================================

	/**
	 * The :246-284 outcome. The World effects ride out as counters and flags; the BE
	 * applies them (fizz sound, gas damage, fire blocks, the explosion, the acid
	 * destruction). The content itself is mutated in place (the removals and the
	 * phase conversions).
	 */
	public record PhaseOutcome(boolean fizz, long gasDamageCount, long gasDamageTemperature, long fireCount, long explosionStrength, boolean acidDestroyed) {
		public static final PhaseOutcome NONE = new PhaseOutcome(false, 0, 0, 0, 0, false);
	}

	/**
	 * The upstream content loop (:246-284) as a pure function. Per stack, in order:
	 * <ol>
	 * <li>:249 — null / MT.NULL / MT.Air / non-positive amounts drop out;</li>
	 * <li>:251 — lighter-than-air materials evaporate harmlessly (fizz);</li>
	 * <li>:254 — at/above the boiling point, or a flammable (and not unburnable/melting)
	 *     material above 40°C: the stack evaporates (fizz). boilingPoint ≥ 320 applies
	 *     temperature damage to the surroundings (gasDamageCount/Temperature); ≥ 2000
	 *     spawns fire (fireCount, the 9·amount/U bound); EXPLOSIVE clears the crucible
	 *     and explodes at the amount-scaled strength (explosionStrength — the :262
	 *     scale(amount, maxAmount, explosionPower, F) product);</li>
	 * <li>:265 — an ACID material in a non-acidproof crucible destroys everything
	 *     (acidDestroyed);</li>
	 * <li>:271 — the SMELTING gate: at/above the melting point AND (crossing it upward
	 *     or fresh content) the stack converts to mTargetSmelting;</li>
	 * <li>:274 — the SOLIDIFYING gate: below the melting point AND (crossing downward
	 *     or fresh content) the stack converts to mTargetSolidifying.</li>
	 * </ol>
	 *
	 * @param aContent        the crucible content, mutated in place
	 * @param aTemperature    the current temperature
	 * @param aOldTemperature the previous tick's temperature (the crossing detection)
	 * @param aNewContent     whether new material arrived this tick (the tNewContent arm)
	 * @param aAcidProof      the crucible acidproofing
	 * @param aParams         the form parameters (maxAmount + explosionPower)
	 * @return the outcome record (upstream NONE = no reaction)
	 */
	public static PhaseOutcome phaseGates(List<OreDictMaterialStack> aContent, long aTemperature, long aOldTemperature, boolean aNewContent, boolean aAcidProof, Params aParams) {
		boolean tFizz = false;
		long tGasDamageCount = 0, tGasDamageTemperature = 0, tFireCount = 0, tExplosionStrength = 0;
		boolean tAcidDestroyed = false;

		List<OreDictMaterialStack> tToBeAdded = new java.util.ArrayList<>(); // :246

		for (int i = 0; i < aContent.size(); i++) {
			OreDictMaterialStack tMaterial = aContent.get(i);
			if (tMaterial == null || tMaterial.mMaterial == MT.NULL || tMaterial.mMaterial == MT.Air || tMaterial.mAmount <= 0) { // :249
				aContent.remove(i--);
			} else if (tMaterial.mMaterial.mGramPerCubicCentimeter <= 0.0012) { // :251 — the WEIGHT_AIR gate (MT.java:111)
				aContent.remove(i--);
				tFizz = true;
			} else if (boiling(tMaterial, aTemperature)) { // :254 — the verbatim boiling/flammable disjunction
				aContent.remove(i--);
				tFizz = true;
				if (tMaterial.mMaterial.mBoilingPoint >= 320) { // :257
					tGasDamageCount++;
					tGasDamageTemperature = Math.max(tGasDamageTemperature, tMaterial.mMaterial.mBoilingPoint);
				}
				if (tMaterial.mMaterial.mBoilingPoint >= 2000) { // :258
					tFireCount += Math.max(1, (9 * tMaterial.mAmount) / U);
				}
				if (tMaterial.mMaterial.contains(gregapi.data.TD.Properties.EXPLOSIVE)) { // :259
					tExplosionStrength = scale(tMaterial.mAmount, aParams.maxAmount(), aParams.explosionPower(), false); // :262
					aContent.clear(); // :260 GarbageGT.trash(mContent)
					break;
				}
			} else if (!aAcidProof && tMaterial.mMaterial.contains(gregapi.data.TD.Properties.ACID)) { // :265
				aContent.clear(); // :266
				tAcidDestroyed = true;
				break;
			} else if (aTemperature >= tMaterial.mMaterial.mMeltingPoint && (aOldTemperature < tMaterial.mMaterial.mMeltingPoint || aNewContent)) { // :271
				OreDictMaterialStack tConverted = new OreDictMaterialStack(tMaterial.mMaterial.mTargetSmelting.mMaterial, UT.Code.units(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSmelting.mAmount, false)); // :273
				aContent.remove(i--);
				tConverted.addToList(tToBeAdded);
			} else if (aTemperature < tMaterial.mMaterial.mMeltingPoint && (aOldTemperature >= tMaterial.mMaterial.mMeltingPoint || aNewContent)) { // :274
				OreDictMaterialStack tConverted = new OreDictMaterialStack(tMaterial.mMaterial.mTargetSolidifying.mMaterial, UT.Code.units(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSolidifying.mAmount, false)); // :276
				aContent.remove(i--);
				tConverted.addToList(tToBeAdded);
			}
		}
		for (int i = 0; i < tToBeAdded.size(); i++) { // :279-284
			OreDictMaterialStack tMaterial = tToBeAdded.get(i);
			if (tMaterial != null && tMaterial.mAmount > 0 && tMaterial.mMaterial != MT.NULL && tMaterial.mMaterial != MT.Air) {
				tMaterial.addToList(aContent);
			}
		}
		if (tExplosionStrength > 0 || tAcidDestroyed) tFizz = true; // :316/:268 the fizz of the destruction arms
		return new PhaseOutcome(tFizz, tGasDamageCount, tGasDamageTemperature, tFireCount, tExplosionStrength, tAcidDestroyed);
	}

	/**
	 * The :254 flammable arm verbatim: {@code contains(FLAMMABLE) &&
	 * !containsAny(UNBURNABLE, TD.Processing.MELTING)} — the boiling gate only burns the
	 * flammable-and-burnable materials.
	 */
	public static boolean flammable(gregapi.oredict.OreDictMaterial aMaterial) {
		return aMaterial.contains(gregapi.data.TD.Properties.FLAMMABLE)
				&& !aMaterial.containsAny(gregapi.data.TD.Properties.UNBURNABLE, gregapi.data.TD.Processing.MELTING);
	}

	/** The :254 boiling arm verbatim (uses {@link #flammable} for the flammable half). */
	public static boolean boiling(OreDictMaterialStack aStack, long aTemperature) {
		return aTemperature >= aStack.mMaterial.mBoilingPoint || (aTemperature > 273 + 40 && flammable(aStack.mMaterial));
	}

	// ====================================================================================
	// the ore direct-smelt projection (upstream :167-183)
	// ====================================================================================

	/**
	 * The :167-183 ore-to-material projection: an ore of {@code aOreMaterial} melts
	 * straight into its mTargetCrushing × mOreMultiplier WITHOUT any crushing step —
	 * the smeltery does the crusher's job while it melts. The raw/small/dense/block/crate
	 * form factors only scale the multiplier (1/1, 2, 9, 16, 64).
	 */
	public static OreDictMaterialStack oreDirect(OreDictMaterial aOreMaterial, long aOreMultiplier) {
		return new OreDictMaterialStack(aOreMaterial.mTargetCrushing.mMaterial, aOreMaterial.mTargetCrushing.mAmount * aOreMaterial.mOreMultiplier * aOreMultiplier);
	}
}
