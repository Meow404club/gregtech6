/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.items.bees;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

/**
 * The {@code gt.bumble} gene NBT domain (task p33-bees-lv3-a-items, do-item 2) — the
 * port of {@code IItemBumbleBee.Util} (gregapi/item/bumble/IItemBumbleBee.java:96-184):
 * the 13-key gene compound the whole Lv3 bee chain shares.
 *
 * <p><b>The carrier keys</b> (both-loader NBT seam, the GT6UsbSticks shape):
 * <ul>
 * <li>{@code gt.bumble} (COMPOUND, the upstream key verbatim :98/:100/:105) — the 13
 *     gene keys: {@code minhum}/{@code maxhum} (FLOAT), {@code mintemp}/{@code maxtemp}/
 *     {@code offspring}/{@code aggro}/{@code work}/{@code life} (LONG),
 *     {@code day}/{@code night}/{@code rain}/{@code storm}/{@code outside}/{@code inside}
 *     (BOOLEAN) — IItemBumbleBee.java:155-168 verbatim.</li>
 * <li>{@code gt.bumble.meta} (INT) — the port's SPECIES CODE carrier: the upstream bee
 *     identity lived in the item meta ({@code family*100 + tier*10 + type},
 *     MultiItemBumbles.java:512), and the port flattens the type digit onto the item id
 *     (the 8-face {@link GT6Bumbles} family), so the remaining code rides the stack NBT.</li>
 * </ul>
 *
 * <p><b>The rolls</b> (the :109-153 faces verbatim): {@link #rollGenes(Random)} is the
 * "Outsider-Plains-Biome" default (:130 — plains 286 K / rainfall 0.5 / sky / day);
 * {@link #rollGenes(long, float, boolean, boolean, Random)} is the :131-153 body (the
 * desert/mesa day-night flip folded per :131); {@link #childGenes(ItemStack, ItemStack,
 * Random)} is the :109-128 heredity walk (each gene a 50/50 pick from either parent,
 * the day/outside guarantees :124/:126).
 *
 * <p><b>The biome faces</b> (the card's "p32 biome-family base + WD.envTemp counterpart,
 * no 1.7.10 BiomeGenBase" ruling): {@link #envTemp(float)} is the WD.envTemp formula
 * (WD.java:412-414, {@code C-3+temperature*20} Kelvin over the MODERN
 * {@link Biome#getBaseTemperature()}); the humidity/day-night inputs ride
 * {@link #rainfallOf(boolean, float)} / {@link #isDesertOrMesa(boolean, float)} — the
 * vanilla climate classifier over the public {@link Biome#hasPrecipitation()} +
 * base-temperature faces.
 *
 * <p>KJS surface (the card's declaration): RUNTIME NBT semantics — no KubeJS face.
 */
public final class GT6BumbleGenes {

	/** The gene compound key (upstream "gt.bumble", IItemBumbleBee.java:98/:100/:105 verbatim). */
	public static final String NBT_BUMBLE = "gt.bumble";
	/** The port's species-code int (the upstream meta minus the type digit — see class doc). */
	public static final String NBT_BUMBLE_META = "gt.bumble.meta";

	// the 13 gene keys (IItemBumbleBee.java:155-168 verbatim)
	public static final String KEY_MIN_HUM = "minhum";
	public static final String KEY_MAX_HUM = "maxhum";
	public static final String KEY_MIN_TEMP = "mintemp";
	public static final String KEY_MAX_TEMP = "maxtemp";
	public static final String KEY_OFFSPRING = "offspring";
	public static final String KEY_AGGRO = "aggro";
	public static final String KEY_WORK = "work";
	public static final String KEY_LIFE = "life";
	public static final String KEY_RAIN = "rain";
	public static final String KEY_STORM = "storm";
	public static final String KEY_DAY = "day";
	public static final String KEY_NIGHT = "night";
	public static final String KEY_OUTSIDE = "outside";
	public static final String KEY_INSIDE = "inside";

	/** The Kelvin offset of the envTemp formula (WD.java:413 {@code C - 3}, CS.C = 273). */
	public static final long ENV_TEMP_BASE = 270;
	/** The plains envTemp (0.8 base temperature — the :130 outsider default, 270+16). */
	public static final long PLAINS_ENV_TEMP = 286;
	/** The plains rainfall (the :130 outsider default). */
	public static final float PLAINS_RAINFALL = 0.5F;

	// -------------------------------------------------------------------------
	// the code carrier — per-leg NBT seam (1.20.1 freeform tag / 21.1 CUSTOM_DATA)
	// -------------------------------------------------------------------------

	/** The stack's species code, 0 (Wild) when untagged (the port's meta counterpart). */
	public static int codeOf(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag() ? aStack.getTag().getInt(NBT_BUMBLE_META) : 0;
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt(NBT_BUMBLE_META);
		 *///?}
	}

	/** Writes the species code (the hive-loot / breeding-te write face). */
	public static void setCode(ItemStack aStack, int aCode) {
		//? if forge {
		aStack.getOrCreateTag().putInt(NBT_BUMBLE_META, aCode);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putInt(NBT_BUMBLE_META, aCode);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
	}

	/** The gene compound, or null when absent (the tooltip/read leg, upstream :507). */
	@Nullable
	public static CompoundTag readGenes(ItemStack aStack) {
		//? if forge {
		if (!aStack.hasTag() || !aStack.getTag().contains(NBT_BUMBLE, net.minecraft.nbt.Tag.TAG_COMPOUND)) return null;
		CompoundTag tGenes = aStack.getTag().getCompound(NBT_BUMBLE);
		return tGenes.isEmpty() ? null : tGenes;
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		if (!tTag.contains(NBT_BUMBLE, net.minecraft.nbt.Tag.TAG_COMPOUND)) return null;
		CompoundTag tGenes = tTag.getCompound(NBT_BUMBLE);
		return tGenes.isEmpty() ? null : tGenes;
		 *///?}
	}

	/** Writes the gene compound (upstream setBumbleTag :104-107). */
	public static void setGenes(ItemStack aStack, CompoundTag aGenes) {
		//? if forge {
		aStack.getOrCreateTag().put(NBT_BUMBLE, aGenes);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.put(NBT_BUMBLE, aGenes);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
	}

	/**
	 * The lazy gene face (upstream getBumbleTag :97-102): an absent compound ROLLS the
	 * outsider-plains genes and stores them — the "Generates random 'Outsider-Plains-Biome'
	 * Genes when used" tooltip promise (MultiItemBumbles.java:510).
	 */
	public static CompoundTag getOrCreateGenes(ItemStack aStack, Random aRandom) {
		CompoundTag tGenes = readGenes(aStack);
		if (tGenes == null) tGenes = rollGenes(aRandom);
		setGenes(aStack, tGenes);
		return tGenes;
	}

	// -------------------------------------------------------------------------
	// the rolls (IItemBumbleBee.java:109-153)
	// -------------------------------------------------------------------------

	/** The :130 outsider-plains default — the lazy/untagged roll. */
	public static CompoundTag rollGenes(Random aRandom) {
		return rollGenes(PLAINS_ENV_TEMP, PLAINS_RAINFALL, true, false, aRandom);
	}

	/**
	 * The :131-153 wild roll: the desert/mesa day-night flip (:131) folded into the
	 * :132-153 body — humidity window = rainfall ±(0.10..0.50), temperature window =
	 * envTemp ±(15..45), offspring 1-4, work 1-10000, aggro 100-10000, life 1200-144000;
	 * sky biomes roll rain/storm proofing against the rainfall, roofed ones go inside-only.
	 */
	public static CompoundTag rollGenes(long aEnvTemp, float aRainfall, boolean aHasSky, boolean aDesertOrMesa, Random aRandom) {
		CompoundTag rGenes = new CompoundTag();
		setHumidityMin(rGenes, aRainfall - 0.10F - aRandom.nextInt(41) / 100.0F);
		setHumidityMax(rGenes, aRainfall + 0.10F + aRandom.nextInt(41) / 100.0F);
		setTemperatureMin(rGenes, aEnvTemp - 15 - aRandom.nextInt(31));
		setTemperatureMax(rGenes, aEnvTemp + 15 + aRandom.nextInt(31));
		setOffspring    (rGenes,    1 + aRandom.nextInt(     4));
		setWorkForce    (rGenes,    1 + aRandom.nextInt( 10000));
		setAggressiveness(rGenes, 100 + aRandom.nextInt(  9901));
		setLifeSpan     (rGenes, 1200 + aRandom.nextInt(142801));
		// the :131 fold (aDay = !desert, aNight = desert) through the :142-144 guarantee
		// (the false,false case can never survive the guards)
		setDayActive    (rGenes, !aDesertOrMesa);
		setNightActive  (rGenes,  aDesertOrMesa);
		if (aHasSky) {
			setOutsideActive(rGenes, true);
			// the :147-148 verbatim shape — the keys are only WRITTEN when the roll passes
			// (the absent-key read is false, IItemBumbleBee.java:178-179)
			if (aRandom.nextInt(10000) < (long)(aRainfall * 10000)) setRainproof(rGenes, true);
			if (aRandom.nextInt(20000) < (long)(aRainfall * 10000)) setStormproof(rGenes, true);
		} else {
			setInsideActive(rGenes, true);
		}
		return rGenes;
	}

	/**
	 * The :109-128 heredity walk: every gene a 50/50 pick from either parent (both parents
	 * lazily roll if untagged), the day (:124) and outside (:126) guarantees verbatim.
	 */
	public static CompoundTag childGenes(ItemStack aPrincess, ItemStack aDrone, Random aRandom) {
		CompoundTag rGenes = new CompoundTag(), tGenesA = getOrCreateGenes(aPrincess, aRandom), tGenesB = getOrCreateGenes(aDrone, aRandom);
		setHumidityMin    (rGenes, getHumidityMin    (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setHumidityMax    (rGenes, getHumidityMax    (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setOffspring      (rGenes, getOffspring      (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setWorkForce      (rGenes, getWorkForce      (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setAggressiveness (rGenes, getAggressiveness (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setLifeSpan       (rGenes, getLifeSpan       (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setTemperatureMin (rGenes, getTemperatureMin (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setTemperatureMax (rGenes, getTemperatureMax (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setRainproof      (rGenes, getRainproof      (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setStormproof     (rGenes, getStormproof     (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setNightActive    (rGenes, getNightActive    (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setDayActive      (rGenes, getDayActive      (aRandom.nextBoolean() ? tGenesA : tGenesB) || !getNightActive(rGenes));
		setInsideActive   (rGenes, getInsideActive   (aRandom.nextBoolean() ? tGenesA : tGenesB));
		setOutsideActive  (rGenes, getOutsideActive  (aRandom.nextBoolean() ? tGenesA : tGenesB) || !getInsideActive(rGenes));
		return rGenes;
	}

	// -------------------------------------------------------------------------
	// the biome faces — the WD.envTemp counterpart over the modern public API
	// -------------------------------------------------------------------------

	/** The WD.envTemp formula (WD.java:412-414: {@code max(1, C-3+temperature*20)} Kelvin) over the modern base temperature. */
	public static long envTemp(float aBaseTemperature) {
		return Math.max(1, ENV_TEMP_BASE + (long)(aBaseTemperature * 20));
	}

	/** The :408 world-position counterpart shape (the biome overload). */
	public static long envTemp(Biome aBiome) {
		return aBiome == null ? PLAINS_ENV_TEMP : envTemp(aBiome.getBaseTemperature());
	}

	/**
	 * The {@code BiomeGenBase.rainfall} counterpart over the public climate faces.
	 * ponytail: {@code Biome.ClimateSettings.downfall} is package-private on BOTH 1.20.1
	 * loaders (forge/neoforge only expose the package-private record via
	 * getModifiedClimateSettings), so the port classifies: no precipitation → 0.0 (desert/
	 * badlands/mushroom), hot-dry → 0.0 (savanna), the jungle band → 0.9, else 0.5 — the
	 * gene VALUES only need the upstream SHAPE; the humidity CHECK face (the Lv3 breeding
	 * card) compares tag windows against THIS same face so both sides drift together.
	 * Swap in a real downfall accessor when a loader exposes one.
	 */
	public static float rainfallOf(boolean aHasPrecipitation, float aBaseTemperature) {
		if (!aHasPrecipitation || aBaseTemperature >= 2.0F) return 0.0F;
		if (aBaseTemperature >= 1.0F) return 0.9F;
		return 0.5F;
	}

	/** The {@code BIOMES_DESERT || BIOMES_MESA} day-night flip driver (:131) — the hot/dry classification. */
	public static boolean isDesertOrMesa(boolean aHasPrecipitation, float aBaseTemperature) {
		return !aHasPrecipitation || aBaseTemperature >= 2.0F;
	}

	// -------------------------------------------------------------------------
	// the 13 gene get/set pairs (IItemBumbleBee.java:155-183, the bind/bound faces verbatim)
	// -------------------------------------------------------------------------

	public static void setHumidityMin   (CompoundTag aTag, float  aV) {aTag.putFloat(KEY_MIN_HUM, aV < 0.01F ? 0     : aV);}
	public static void setHumidityMax   (CompoundTag aTag, float  aV) {aTag.putFloat(KEY_MAX_HUM, aV < 0.01F ? 0.01F : aV);}
	public static void setTemperatureMin(CompoundTag aTag, long   aV) {aTag.putLong(KEY_MIN_TEMP, aV);}
	public static void setTemperatureMax(CompoundTag aTag, long   aV) {aTag.putLong(KEY_MAX_TEMP, aV);}
	public static void setOffspring     (CompoundTag aTag, long   aV) {aTag.putLong(KEY_OFFSPRING, bindStack(aV));}
	public static void setAggressiveness(CompoundTag aTag, long   aV) {aTag.putLong(KEY_AGGRO, bind(100, 10000, aV));}
	public static void setWorkForce     (CompoundTag aTag, long   aV) {aTag.putLong(KEY_WORK, bind(1, 10000, aV));}
	public static void setLifeSpan      (CompoundTag aTag, long   aV) {aTag.putLong(KEY_LIFE, bind(1200, 144000, aV));}
	public static void setRainproof     (CompoundTag aTag, boolean aV) {aTag.putBoolean(KEY_RAIN, aV);}
	public static void setStormproof    (CompoundTag aTag, boolean aV) {aTag.putBoolean(KEY_STORM, aV);}
	public static void setDayActive     (CompoundTag aTag, boolean aV) {aTag.putBoolean(KEY_DAY, aV);}
	public static void setNightActive   (CompoundTag aTag, boolean aV) {aTag.putBoolean(KEY_NIGHT, aV);}
	public static void setOutsideActive (CompoundTag aTag, boolean aV) {aTag.putBoolean(KEY_OUTSIDE, aV);}
	public static void setInsideActive  (CompoundTag aTag, boolean aV) {aTag.putBoolean(KEY_INSIDE, aV);}

	public static float getHumidityMin   (CompoundTag aTag) {return Math.max(0, aTag.getFloat(KEY_MIN_HUM));}
	public static float getHumidityMax   (CompoundTag aTag) {return Math.max(0.01F, aTag.getFloat(KEY_MAX_HUM));}
	public static long getTemperatureMin (CompoundTag aTag) {return aTag.getLong(KEY_MIN_TEMP);}
	public static long getTemperatureMax (CompoundTag aTag) {return aTag.getLong(KEY_MAX_TEMP);}
	public static long getOffspring      (CompoundTag aTag) {return bindStack(aTag.getLong(KEY_OFFSPRING));}
	public static long getAggressiveness (CompoundTag aTag) {return bind(100, 10000, aTag.getLong(KEY_AGGRO));}
	public static long getWorkForce      (CompoundTag aTag) {return bind(1, 10000, aTag.getLong(KEY_WORK));}
	public static long getLifeSpan       (CompoundTag aTag) {return bind(1200, 144000, aTag.getLong(KEY_LIFE));}
	public static boolean getRainproof   (CompoundTag aTag) {return aTag.getBoolean(KEY_RAIN);}
	public static boolean getStormproof  (CompoundTag aTag) {return aTag.getBoolean(KEY_STORM);}
	public static boolean getDayActive   (CompoundTag aTag) {return aTag.getBoolean(KEY_DAY);}
	public static boolean getNightActive (CompoundTag aTag) {return aTag.getBoolean(KEY_NIGHT);}
	public static boolean getOutsideActive(CompoundTag aTag) {return aTag.getBoolean(KEY_OUTSIDE);}
	public static boolean getInsideActive(CompoundTag aTag) {return aTag.getBoolean(KEY_INSIDE);}

	/** The UT.Code.bind counterpart ({@code min} when below, {@code max} when above). */
	public static long bind(long aMin, long aMax, long aNumber) {
		return aNumber < aMin ? aMin : aNumber > aMax ? aMax : aNumber;
	}

	/** The UT.Code.bindStack counterpart (the 1..64 stack bound). */
	public static long bindStack(long aNumber) {
		return bind(1, 64, aNumber);
	}

	private GT6BumbleGenes() {
	}
}
