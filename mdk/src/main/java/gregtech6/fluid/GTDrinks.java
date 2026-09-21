package gregtech6.fluid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The GT self-held drink-stat registry (task p33-food-fluids-b2) — the port counterpart of
 * the upstream DrinksGT.REGISTER (FoodStatDrink.java:35-83: every FoodStatDrink
 * constructor puts itself into the map keyed by the fluid name, and
 * TileEntityBase08FluidContainer.isDrinkable() :415-417 gates the tank drink seam on
 * REGISTER.containsKey(mTank.name())).
 *
 * <p>The modern legs carry NO hydration/alcohol/caffeine/sugar/fat/radiation positions
 * (research.p33-r-food drink_seam: 1.20.1 FoodProperties is nutrition/saturation/
 * canAlwaysEat, FoodProperties.java:8-17; 1.21.1 is the same shape — Consumable's
 * consumeEffects only exists 1.21.2+), so the extended stat surface lives HERE, keyed by
 * the gt6 fluid registry path — the mirror image of the upstream fluid-name key. The
 * vanilla-visible face stays minimal: hunger/saturation ride Player.foodData.eat
 * (FoodData.java:21), the potion effects ride MobEffectInstance adds, the
 * hydration/temperature numbers are the declared GT data this registry holds for the
 * downstream consumers (the barrel-block drink seam + the p33-food-drink card).
 *
 * <p>Every row is census-anchored to the upstream FoodStatDrink lines of
 * Loader_Fluids.java (the :230-350 card-block potion run + the 345-row FoodStatDrink
 * census over the PORTED fluid ids); the upstream tooltip line rides each row verbatim —
 * the drink feedback face. The count reconciliation is GTDrinksB2Test.
 */
public final class GTDrinks {

	/** One potion effect pair — the upstream {@code Potion.<x>.id, <duration>, <amplifier>} triple (the FoodStat potionEffects semantics; the upstream chance-of-effect channel is the AppleCore seam, absent on the modern legs). The effect face is the one leg-forked surface: MobEffects.X are bare MobEffect on 1.20.1 and Holder&lt;MobEffect&gt; on 1.21.1 — the record header rides the leg, the accessor name stays {@code effect()} everywhere. */
	//? if forge {
	public record DrinkEffect(MobEffect effect, int duration, int amplifier) {}
	//?} else {
	/*public record DrinkEffect(Holder<MobEffect> effect, int duration, int amplifier) {}
	 *///?}

	/**
	 * One drink row — the upstream FoodStatDrink constructor surface, the extended stats
	 * as plain declared data: food level (half-drumsticks), saturation, hydration (0-100),
	 * temperature (K), temperature effect, luminosity (0/10, the upstream
	 * {@code .setLuminosity(10)} rows), and the potion effects.
	 */
	public record DrinkStat(String fluidId, String tooltip, int food, float saturation, int hydration,
			int temperature, float temperatureEffect, int luminosity, List<DrinkEffect> effects) {
		public DrinkStat(String aFluidId, String aTooltip, int aFood, float aSaturation, int aHydration,
				int aTemperature, float aTemperatureEffect, DrinkEffect... aEffects) {
			this(aFluidId, aTooltip, aFood, aSaturation, aHydration, aTemperature, aTemperatureEffect, 0, List.of(aEffects));
		}
		public DrinkStat(String aFluidId, String aTooltip, int aFood, float aSaturation, int aHydration,
				int aTemperature, float aTemperatureEffect, int aLuminosity, DrinkEffect... aEffects) {
			this(aFluidId, aTooltip, aFood, aSaturation, aHydration, aTemperature, aTemperatureEffect, aLuminosity, List.of(aEffects));
		}
	}

	/** The registry — the DrinksGT.REGISTER mirror, insertion-ordered, keyed by the gt6 fluid path. */
	public static final Map<String, DrinkStat> REGISTER = new LinkedHashMap<>();

	/** The upstream drink seam threshold verbatim (TileEntityBase08FluidContainer.java:101/:416 — {@code mTank.has(250)}). */
	public static final int DRINK_MB = 250;

	/** The stat for a gt6 fluid path, or null (the DrinksGT.REGISTER.get shape). */
	@Nullable
	public static DrinkStat stat(@Nullable String aFluidId) {
		return aFluidId == null ? null : REGISTER.get(aFluidId);
	}

	/**
	 * The drink action: hunger/saturation through FoodData.eat (FoodData.java:21), the
	 * effects as MobEffectInstance adds. False when the player cannot drink the row — the
	 * upstream per-row alwaysEdible flag (FoodStatDrink's leading T/F) folds onto the
	 * zero-food heuristic: a row with no food value is always drinkable (water/poison/the
	 * medicines — the hydration-only face the modern stat surface cannot carry), a
	 * food-carrying row needs an open hunger slot (the Item.use canEat face, Item.java:138).
	 * Declared deviation: the handful of upstream F-flagged zero-food water rows
	 * (:360/:361/:363/:626) over-permit at a full hunger bar — they still pay the 250 mB.
	 */
	public static boolean drink(Player aPlayer, DrinkStat aStat) {
		if (!aPlayer.canEat(aStat.food() == 0)) return false;
		if (aStat.food() > 0 || aStat.saturation() > 0) aPlayer.getFoodData().eat(aStat.food(), aStat.saturation());
		for (DrinkEffect tEffect : aStat.effects()) {
			// one line both legs (the p33-food-tail un-fork): the record accessor name IS the
			// leg seam (MobEffect vs Holder<MobEffect>), so the statement is leg-invariant —
			// the identical-body //? fork here was dead weight
			aPlayer.addEffect(new MobEffectInstance(tEffect.effect(), tEffect.duration(), tEffect.amplifier()));
		}
		return true;
	}

	/** True when the entity is a valid drink target (the upstream canDrinkFromSide face simplified — the tileentity seam rides the barrel block). */
	public static boolean canDrink(@Nullable LivingEntity aEntity) {
		return aEntity instanceof Player;
	}

	private GTDrinks() {}



	// The KEPT rows: the 345-row upstream FoodStatDrink census over the PORTED fluid ids
	// (b1 + the earlier tables + the p31 honey family) — keyed by the gt6 registry path,
	// the binnie./dotted upstream names ride the comment. 215 rows (incl. the one
	// vanilla-carried "water" row — the barrel seam keys bare registry paths, so a
	// water-filled barrel resolves minecraft:water → "water", the upstream :360 row —
	// and the p33-food-tail "mnwtr" fill, the census walk had skipped the :371
	// potion.mineralwater row because its ported id is the dotted-name-abbreviated
	// p16 aqua id).

	/** The 215 kept-family rows, census order. */
	public static final List<DrinkStat> KEPT_SPECS = List.of(

		new DrinkStat("water", "Water? Lloyd!", 0, 0.0F, 40, 308, 0.50F), // upstream "water" (Loader_Fluids.java:360) — the VANILLA water the barrels hold; no gt6 fluid id, the map key rides the bare registry path
		new DrinkStat("spdew", "Ghostly Water", 0, 0.0F, 40, 310, 0.50F), // upstream "spectral_dew"
		new DrinkStat("distilled_water", "Distilled H2O", 0, 0.0F, 40, 308, 0.50F), // upstream "ic2distilledwater"
		new DrinkStat("ice", "Almost frozen H2O", 0, 0.0F, 40, 303, 0.50F, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 400, 1), new DrinkEffect(MobEffects.DIG_SLOWDOWN, 400, 1)), // upstream "ice"
		new DrinkStat("waterdirty", "Dirty", 0, 0.0F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1), new DrinkEffect(MobEffects.POISON, 200, 0)), // upstream "waterdirty"
		new DrinkStat("seawater", "Salty", 0, 0.0F, 10, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 2)), // upstream "seawater"
		new DrinkStat("soda", "Simply carbonated Water", 1, 0.1F, 50, 308, 0.50F), // upstream "soda"
		new DrinkStat("mnwtr", "Stay hydrated!", 1, 0.1F, 40, 308, 0.50F, new DrinkEffect(MobEffects.REGENERATION, 100, 1)), // upstream "potion.mineralwater" (Loader_Fluids.java:371 — the FL.MnWtr fluid, ported as the p16 aqua id "mnwtr"; the p33-food-tail KEPT fill between the :370/:372 census neighbours)
		new DrinkStat("mineralsoda", "Stay hydrated!", 1, 0.2F, 50, 308, 0.50F, new DrinkEffect(MobEffects.REGENERATION, 100, 1)), // upstream "mineralsoda"
		new DrinkStat("water_geothermal", "Fresh from the Geothermal Hot Spring!", 1, 0.1F, 40, 313, 0.50F, new DrinkEffect(MobEffects.REGENERATION, 100, 1)), // upstream "watergeothermal"
		new DrinkStat("juice_juice", "From a Random Fruit", 3, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "juice" (Loader_Fluids.java:376) — the b1 "juice_juice" id
		new DrinkStat("strawberryjuice", "Where is the Straw for that Berry Juice?", 6, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "strawberryjuice"
		new DrinkStat("melonjuice", "Yoshis favourite Fruit Juice", 2, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "melonjuice"
		new DrinkStat("kiwijuice", "Made of little Birds!", 4, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "kiwijuice"
		new DrinkStat("persimmonjuice", "Only one per Simon", 3, 0.8F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "persimmonjuice"
		new DrinkStat("currantjuice", "Runs electric Currant", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "currantjuice"
		new DrinkStat("raspberryjuice", "Made of tiny Computers!", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "raspberryjuice"
		new DrinkStat("blackberryjuice", "Made of Smartphones!", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "blackberryjuice"
		new DrinkStat("blueberryjuice", "Colors everything Blue", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "blueberryjuice"
		new DrinkStat("gooseberryjuice", "Made of real Geese!", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "gooseberryjuice"
		new DrinkStat("juicecranberry", "Made of real Cranes!", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "binnie.juicecranberry"
		new DrinkStat("juiceelderberry", "Made of elderly Berries!", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "binnie.juiceelderberry"
		new DrinkStat("hellderberryjuice", "Smells quite a bit", 3, 0.4F, 30, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "hellderberryjuice"
		new DrinkStat("juicelemon", "Maybe adding Sugar will make it less sour", 3, 0.8F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "binnie.juicelemon"
		new DrinkStat("juicepineapple", "Sponsored by Daniel Rustage!", 3, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "binnie.juicepineapple" (Loader_Fluids.java:390 — the PINEAPPLE JUICE; "winepineapple" is the separate Ananas Cider key below :520)
		new DrinkStat("juiceorange", "All 100% of it!", 3, 0.8F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "binnie.juiceorange"
		new DrinkStat("juiceapricot", "", 3, 0.8F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "binnie.juiceapricot"
		new DrinkStat("juicelime", "", 3, 0.8F, 20, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "binnie.juicelime"
		new DrinkStat("juicepear", "", 6, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 0)), // upstream "binnie.juicepear"
		new DrinkStat("juicecherry", "", 6, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "binnie.juicecherry"
		new DrinkStat("juiceplum", "May have a cleaning effect on your internals.", 2, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "binnie.juiceplum"
		new DrinkStat("juicepeach", "The Princess is in another Castle", 2, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "binnie.juicepeach"
		new DrinkStat("juicegrapefruit", "Not suitable for Diets!", 3, 0.8F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "binnie.juicegrapefruit"
		new DrinkStat("juicebanana", "Big juiced Banana", 6, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "binnie.juicebanana" (Loader_Fluids.java:399)
		new DrinkStat("grapejuice", "May have a cleaning effect on your internals.", 4, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "grapejuice"
		new DrinkStat("grc_grapewine0", "May have a cleaning effect on your internals.", 4, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "grc.grapewine0"
		new DrinkStat("juiceredgrape", "May have a cleaning effect on your internals.", 4, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "binnie.juiceredgrape"
		new DrinkStat("juicewhitegrape", "May have a cleaning effect on your internals.", 4, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "binnie.juicewhitegrape"
		new DrinkStat("juiceapple", "Made of the Apples from our best Oak Farms", 6, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 0)), // upstream "binnie.juiceapple"
		new DrinkStat("grc_applecider0", "Made of the Apples from our best Oak Farms", 6, 0.4F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 0)), // upstream "grc.applecider0"
		new DrinkStat("figjuice", "", 2, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "figjuice"
		new DrinkStat("pomegranatejuice", "", 2, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "pomegranatejuice"
		new DrinkStat("mangojuice", "", 3, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "mangojuice"
		new DrinkStat("starfruitjuice", "", 3, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "starfruitjuice"
		new DrinkStat("papayajuice", "", 3, 0.4F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "papayajuice"
		new DrinkStat("coconutmilk", "", 6, 0.8F, 40, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "coconutmilk"
		new DrinkStat("fruitsmoothie", "", 2, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "fruitsmoothie"
		new DrinkStat("strawberrysmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "strawberrysmoothie"
		new DrinkStat("melonsmoothie", "", 1, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "melonsmoothie"
		new DrinkStat("kiwismoothie", "", 3, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "kiwismoothie"
		new DrinkStat("currantsmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "currantsmoothie"
		new DrinkStat("raspberrysmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "raspberrysmoothie"
		new DrinkStat("blackberrysmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "blackberrysmoothie"
		new DrinkStat("blueberrysmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "blueberrysmoothie"
		new DrinkStat("gooseberrysmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "gooseberrysmoothie"
		new DrinkStat("cranberrysmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "cranberrysmoothie"
		new DrinkStat("elderberrysmoothie", "", 2, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "elderberrysmoothie"
		new DrinkStat("lemonsmoothie", "", 2, 0.4F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "lemonsmoothie"
		new DrinkStat("pineapplesmoothie", "", 2, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "pineapplesmoothie"
		new DrinkStat("orangesmoothie", "", 2, 0.4F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "orangesmoothie"
		new DrinkStat("persimmonsmoothie", "", 2, 0.4F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "persimmonsmoothie"
		new DrinkStat("apricotsmoothie", "", 2, 0.4F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "apricotsmoothie"
		new DrinkStat("limesmoothie", "", 2, 0.4F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "limesmoothie"
		new DrinkStat("pearsmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 0)), // upstream "pearsmoothie"
		new DrinkStat("cherrysmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "cherrysmoothie"
		new DrinkStat("plumsmoothie", "", 1, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "plumsmoothie"
		new DrinkStat("peachsmoothie", "", 1, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "peachsmoothie"
		new DrinkStat("grapefruitsmoothie", "", 2, 0.4F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "grapefruitsmoothie"
		new DrinkStat("bananasmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "bananasmoothie" (Loader_Fluids.java:435)
		new DrinkStat("redgrapesmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "redgrapesmoothie"
		new DrinkStat("whitegrapesmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "whitegrapesmoothie"
		new DrinkStat("purplegrapesmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "purplegrapesmoothie"
		new DrinkStat("grapesmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 1)), // upstream "grapesmoothie"
		new DrinkStat("applesmoothie", "", 4, 0.2F, 20, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 200, 0)), // upstream "applesmoothie"
		new DrinkStat("figsmoothie", "", 1, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "figsmoothie"
		new DrinkStat("pomegranatesmoothie", "", 1, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "pomegranatesmoothie"
		new DrinkStat("mangosmoothie", "", 2, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "mangosmoothie"
		new DrinkStat("starfruitsmoothie", "", 2, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "starfruitsmoothie"
		new DrinkStat("papayasmoothie", "", 2, 0.2F, 40, 308, 0.50F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "papayasmoothie"
		new DrinkStat("coconutsmoothie", "", 4, 0.4F, 40, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "coconutsmoothie"
		new DrinkStat("juicetomato", "Used for Ketchup and Tomato Sauces", 1, 0.2F, 40, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.juicetomato"
		new DrinkStat("juicecarrot", "Not scientifically proven to improve eyesight!", 1, 0.2F, 40, 310, 0.50F, new DrinkEffect(MobEffects.NIGHT_VISION, 400, 0)), // upstream "binnie.juicecarrot"
		new DrinkStat("beetjuice", "Beets me.", 1, 0.2F, 40, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 200, 1)), // upstream "beetjuice"
		new DrinkStat("pumpkinjuice", "Perfect for Halloween", 1, 0.2F, 40, 310, 0.50F), // upstream "pumpkinjuice"
		new DrinkStat("potatojuice", "Ever seen Potato Juice in stores? No? That has a reason.", 3, 0.3F, 20, 310, 0.50F), // upstream "potatojuice"
		new DrinkStat("hopsmash", "Every Beer has a start", 1, 0.1F, 15, 310, 0.50F), // upstream "hopsmash"
		new DrinkStat("wheathopsmash", "Also known as 'Duff-Lite'", 1, 0.1F, 15, 310, 0.50F), // upstream "wheathopsmash"
		new DrinkStat("mashwheat", "Is this liquefied Bread or what?", 2, 0.1F, 15, 310, 0.50F), // upstream "binnie.mashwheat"
		new DrinkStat("mashcorn", "", 2, 0.1F, 15, 310, 0.50F), // upstream "binnie.mashcorn"
		new DrinkStat("mashgrain", "", 2, 0.1F, 15, 310, 0.50F), // upstream "binnie.mashgrain"
		new DrinkStat("mashrye", "", 2, 0.1F, 15, 310, 0.50F), // upstream "binnie.mashrye"
		new DrinkStat("ricewater", "", 1, 0.1F, 20, 310, 0.50F), // upstream "ricewater"
		new DrinkStat("reedwater", "I guess this tastes better when fermented", 1, 0.1F, 20, 310, 0.50F), // upstream "reedwater"
		new DrinkStat("cactuswater", "", 1, 0.1F, 10, 310, 0.50F, new DrinkEffect(MobEffects.POISON, 100, 0)), // upstream "cactuswater"
		new DrinkStat("maplesap", "May or may not be Canadian", 3, 0.2F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 300, 1)), // upstream "maplesap"
		new DrinkStat("juice_wine_fruit", "What Fruits is this made of?!", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.juice"
		new DrinkStat("wineagave", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.wineagave"
		new DrinkStat("wineapricot", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.wineapricot"
		new DrinkStat("winebanana", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winebanana" (Loader_Fluids.java:471)
		new DrinkStat("winecarrot", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winecarrot"
		new DrinkStat("winecherry", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winecherry"
		new DrinkStat("winecitrus", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winecitrus"
		new DrinkStat("winecranberry", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winecranberry"
		new DrinkStat("wineelderberry", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.wineelderberry"
		new DrinkStat("wineplum", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.wineplum"
		new DrinkStat("winesparkling", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winesparkling"
		new DrinkStat("winetomato", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winetomato"
		new DrinkStat("winefortified", "", 3, 0.4F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 1)), // upstream "binnie.winefortified"
		new DrinkStat("wine", "Exquisite", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "wine"
		new DrinkStat("ricardosanchez", "Wubalubadubdub", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "ricardosanchez"
		new DrinkStat("winered", "Exquisite", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winered"
		new DrinkStat("winewhite", "Exquisite", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0)), // upstream "binnie.winewhite"
		new DrinkStat("limoncello", "An alcoholic Drink which tastes like Lemons", 2, 0.4F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 1200, 0)), // upstream "limoncello"
		new DrinkStat("beerale", "", 6, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 400, 2)), // upstream "binnie.beerale"
		new DrinkStat("beercorn", "", 6, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 400, 2)), // upstream "binnie.beercorn"
		new DrinkStat("beerrye", "", 6, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 400, 2)), // upstream "binnie.beerrye"
		new DrinkStat("beerstout", "", 6, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 400, 2)), // upstream "binnie.beerstout"
		new DrinkStat("beerwheat", "", 6, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 400, 2)), // upstream "binnie.beerwheat"
		new DrinkStat("beerlager", "", 4, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1)), // upstream "binnie.beerlager"
		new DrinkStat("beer", "Not to be confused with Beerus, the God of Destruction", 6, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 400, 2)), // upstream "beer"
		new DrinkStat("darkbeer", "Dark Beer, for the real Men", 4, 0.4F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1)), // upstream "darkbeer"
		new DrinkStat("brandyapple", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandyapple"
		new DrinkStat("brandyapricot", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandyapricot"
		new DrinkStat("brandycherry", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandycherry"
		new DrinkStat("brandycitrus", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandycitrus"
		new DrinkStat("brandyelderberry", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandyelderberry"
		new DrinkStat("brandyfruit", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandyfruit"
		new DrinkStat("brandygrape", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandygrape"
		new DrinkStat("brandypear", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandypear"
		new DrinkStat("brandyplum", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.brandyplum"
		new DrinkStat("whiskey", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.whiskey"
		new DrinkStat("whiskeycorn", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.whiskeycorn"
		new DrinkStat("whiskeyrye", "", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.whiskeyrye"
		new DrinkStat("whiskeywheat", "Technically this is just a Whisky", 2, 0.1F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.whiskeywheat"
		new DrinkStat("glenmckenner", "Don't hand to easily surprised people, they will shatter it.", 2, 0.1F, 5, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 2)), // upstream "glenmckenner"
		new DrinkStat("rumwhite", "A buddle o' rum", 4, 0.4F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1)), // upstream "binnie.rumwhite"
		new DrinkStat("rumdark", "Set the Sails, we are going to Torrentuga!", 4, 0.4F, 5, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 2)), // upstream "binnie.rumdark"
		new DrinkStat("pina_colada", "", 4, 0.4F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1)), // upstream "pina.colada"
		new DrinkStat("ciderpear", "If you have nothing better to do with your Pears", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.ciderpear"
		new DrinkStat("ciderpeach", "If you have nothing better to do with your Peaches", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.ciderpeach"
		new DrinkStat("winepineapple", "If you have nothing better to do with your Pineapples", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.winepineapple"
		new DrinkStat("ciderapple", "If you have nothing better to do with your Apples", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.ciderapple"
		new DrinkStat("liqueuralmond", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueuralmond"
		new DrinkStat("liqueuranise", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueuranise"
		new DrinkStat("liqueurbanana", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurbanana" (Loader_Fluids.java:525)
		new DrinkStat("liqueurblackberry", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurblackberry"
		new DrinkStat("liqueurblackcurrant", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurblackcurrant"
		new DrinkStat("liqueurcherry", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurcherry"
		new DrinkStat("liqueurchocolate", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurchocolate"
		new DrinkStat("liqueurcinnamon", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurcinnamon"
		new DrinkStat("liqueurcoffee", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurcoffee"
		new DrinkStat("liqueurhazelnut", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurhazelnut"
		new DrinkStat("liqueurherbal", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurherbal"
		new DrinkStat("liqueurlemon", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurlemon"
		new DrinkStat("liqueurmelon", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurmelon"
		new DrinkStat("liqueurmint", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurmint"
		new DrinkStat("liqueurorange", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurorange"
		new DrinkStat("liqueurpeach", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurpeach"
		new DrinkStat("liqueurraspberry", "", 4, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 1)), // upstream "binnie.liqueurraspberry"
		new DrinkStat("liquorpear", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.liquorpear"
		new DrinkStat("liquorfruit", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.liquorfruit"
		new DrinkStat("liquorelderberry", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.liquorelderberry"
		new DrinkStat("liquorcherry", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.liquorcherry"
		new DrinkStat("liquorapricot", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.liquorapricot"
		new DrinkStat("liquorapple", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.liquorapple"
		new DrinkStat("spiritsugarcane", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.spiritsugarcane"
		new DrinkStat("spiritneutral", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.spiritneutral"
		new DrinkStat("spiritgin", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.spiritgin"
		new DrinkStat("tequila", "", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.tequila"
		new DrinkStat("vodka", "Not to be confused with Water", 2, 0.2F, 10, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 1)), // upstream "binnie.vodka"
		new DrinkStat("short_mead", "A Vikings favourite brew", 3, 0.1F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1), new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 300, 1)), // upstream "short.mead"
		new DrinkStat("mead", "A Vikings favourite brew", 3, 0.1F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1), new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 300, 1)), // upstream "mead"
		new DrinkStat("vinegar", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 1)), // upstream "vinegar"
		new DrinkStat("applevinegar", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 400, 2)), // upstream "applevinegar"
		new DrinkStat("canevinegar", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 2)), // upstream "canevinegar"
		new DrinkStat("ricevinegar", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 2)), // upstream "ricevinegar"
		new DrinkStat("chillysauce", "Spicy", 2, 0.1F, 10, 315, 0.50F, new DrinkEffect(MobEffects.CONFUSION, 1000, 0), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 1000, 0)), // upstream "chillysauce"
		new DrinkStat("honey", "Bee careful with it", 1, 0.1F, 20, 310, 0.50F), // upstream "honey"
		new DrinkStat("grc_honey", "Bee careful with it", 1, 0.1F, 20, 310, 0.50F), // upstream "grc.honey"
		new DrinkStat("for_honey", "Bee careful with it", 1, 0.1F, 20, 310, 0.50F), // upstream "for.honey"
		new DrinkStat("honeydew", "Sweet sweet Honeydew", 2, 0.2F, 20, 310, 0.50F, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 600, 0)), // upstream "honeydew"
		new DrinkStat("royal_jelly", "you jelly?", 2, 0.2F, 20, 310, 0.75F, new DrinkEffect(MobEffects.REGENERATION, 150, 1)), // upstream "royaljelly"
		new DrinkStat("sunfloweroil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "sunfloweroil"
		new DrinkStat("nutoil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "nutoil"
		new DrinkStat("juiceolive", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "binnie.juiceolive"
		new DrinkStat("seedoil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "seedoil"
		new DrinkStat("linoil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "linoil"
		new DrinkStat("hempoil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "hempoil"
		new DrinkStat("fishoil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "fishoil"
		new DrinkStat("whaleoil", "", 2, 0.2F, 10, 310, 0.50F, new DrinkEffect(MobEffects.HUNGER, 400, 1)), // upstream "whaleoil"
		new DrinkStat("mayo", "Tastes like Cardboard", 3, 0.5F, 5, 310, 0.25F), // upstream "mayo"
		new DrinkStat("grcmilk_cream", "", 2, 0.4F, 20, 310, 0.75F), // upstream "grcmilk.cream"
		new DrinkStat("coconutcream", "", 2, 0.4F, 20, 310, 0.75F), // upstream "coconutcream"
		new DrinkStat("ketchup", "this tooltip doesn't make sans", 4, 0.3F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0), new DrinkEffect(MobEffects.MOVEMENT_SPEED, 200, 0)), // upstream "ketchup"
		new DrinkStat("bbqsauce", "Also called BBQ Sauce", 4, 0.3F, 20, 310, 0.50F, new DrinkEffect(MobEffects.HEAL, 1, 0), new DrinkEffect(MobEffects.MOVEMENT_SPEED, 200, 0)), // upstream "bbqsauce"
		new DrinkStat("chocolatecream", "", 4, 0.2F, 5, 310, 0.15F), // upstream "chocolatecream"
		new DrinkStat("nutella", "For Germans: It is 'die' Nutella, not 'der' nor 'das'", 8, 0.4F, 5, 310, 0.15F), // upstream "nutella"
		new DrinkStat("peanutbutter", "This is NUTS!!!", 8, 0.4F, 5, 310, 0.15F), // upstream "peanutbutter"
		new DrinkStat("maplesyrup", "Etho paused to look at a vintage Beef, then he unpaused", 4, 0.2F, 20, 310, 0.30F), // upstream "maplesyrup"
		new DrinkStat("purpledrink", "How about Lemonade? Or some Ice Tea? I got Purple Drink!", 8, 0.2F, 30, 308, 0.50F, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 1200, 2)), // upstream "purpledrink"
		new DrinkStat("lemonade", "Cold and refreshing Lemonade", 4, 0.3F, 20, 308, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 900, 1)), // upstream "potion.lemonade" (Loader_Fluids.java:603) — the b1 "lemonade" id
		new DrinkStat("cavejohnsonsgrenadejuice", "When life gives you Lemons, make Life take them Lemons back!", 0, 0.0F, 20, 310, 0.50F), // upstream "potion.cavejohnsonsgrenadejuice" (Loader_Fluids.java:614 — the GT-only conductive/explosive channels have no modern position; the b1 "cavejohnsonsgrenadejuice" id)
		new DrinkStat("lubricant", "Industrial Use ONLY!", 0, 0.0F, 0, 310, 0.00F), // upstream "lubricant"
		new DrinkStat("soymilk", "Milk Substitute", 0, 0.0F, 20, 310, 0.50F), // upstream "soymilk"
		new DrinkStat("grcmilk_milk", "Got Milk?", 0, 0.0F, 20, 310, 0.75F), // upstream "grcmilk.milk"
		new DrinkStat("milk", "Got Milk?", 0, 0.0F, 20, 310, 0.75F), // upstream "milk"
		new DrinkStat("spoiledmilk", "Smells a little", 0, 0.0F, 0, 310, 0.75F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "spoiledmilk"
		new DrinkStat("mushroomsoup", "", 2, 0.6F, 5, 310, 0.15F), // upstream "mushroomsoup"
		new DrinkStat("slime_blue", "Blue Slime Juice", 2, 0.5F, 20, 310, 0.50F, new DrinkEffect(MobEffects.JUMP, 600, 0)), // upstream "slime.blue"
		new DrinkStat("pinkslime", "Meaty Slime Juice", 4, 0.5F, 20, 310, 0.50F, new DrinkEffect(MobEffects.JUMP, 1200, 1)), // upstream "pinkslime"
		new DrinkStat("slime", "Green Slime Juice", 2, 0.5F, 20, 310, 0.50F, new DrinkEffect(MobEffects.JUMP, 600, 0)), // upstream "slime"
		new DrinkStat("bawls", "Here, take a cold and refreshing sip of my BAWLS", 2, 0.5F, 20, 308, 0.50F, new DrinkEffect(MobEffects.JUMP, 1200, 1)), // upstream "bawls"
		new DrinkStat("tea", "Side Effects may include: Infinite Wealth, clipping through Walls and ascending to Godhood", 2, 0.2F, 20, 312, 0.50F), // upstream "tea"
		new DrinkStat("sweettea", "How about an actual Tea Party? In Boston?", 2, 0.2F, 20, 312, 0.50F), // upstream "sweettea"
		new DrinkStat("icetea", "Ice 'T'", 6, 0.4F, 30, 308, 0.50F) // upstream "icetea"

	);

	// The RESIDUAL rows: the 9 FOOD-flag drink fluids the b2 table newly registers — the
	// DrinkStat rows the KEPT walk could not carry (their fluids did not exist before this card).

	/** The 9 residual rows (the b2 registrations). */
	public static final List<DrinkStat> RESIDUAL_SPECS = List.of(
		new DrinkStat("riverwater", "Lloyd? Water!", 0, 0.0F, 40, 308, 0.50F), // upstream "riverwater"
		new DrinkStat("ic2distilledwater", "Distilled H2O", 0, 0.0F, 40, 308, 0.50F), // upstream "ic2distilledwater"
		new DrinkStat("goldencarrotjuice", "A golden Carrot in liquid form", 4, 0.2F, 100, 310, 0.75F, 15, new DrinkEffect(MobEffects.NIGHT_VISION, 1200, 0)), // upstream "goldencarrotjuice" (Loader_Fluids.java:606, the .setLuminosity(15) row)
		new DrinkStat("holywater", "May the holy Planks be with you", 0, 0.0F, 10, 303, 0.50F, new DrinkEffect(MobEffects.POISON, 100, 1)), // upstream "holywater"
		new DrinkStat("rottendrink", "Smells rotten", 0, 0.0F, 0, 310, 0.75F, new DrinkEffect(MobEffects.HUNGER, 100, 1)), // upstream "rottendrink"
		new DrinkStat("poison", "Poison", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.POISON, 450, 1)), // upstream "poison"
		new DrinkStat("chocolatemilk", "Sweet Goodness", 4, 0.4F, 10, 310, 0.50F), // upstream "chocolatemilk"
		new DrinkStat("medicine.heal", "Heals up to 20 Hearts", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.REGENERATION, 120, 4)), // upstream "medicine.heal"
		new DrinkStat("medicine.laxative", "Removes 10 Hunger/Saturation", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HUNGER, 300, 10)) // upstream "medicine.laxative"
	);

	// The POTION rows: the 94-row card-block brew run, Loader_Fluids.java:230-350 verbatim
	// semantics (the gold-apple rows :607-610 are OUTSIDE the card block — the p33-food-tail
	// TAIL_SPECS below).

	// 94 potion rows — Loader_Fluids.java:230-350 verbatim semantics (tip = the upstream tooltip verbatim).
	public static final List<DrinkStat> POTION_SPECS = List.of(
		new DrinkStat("potion.tainted", "tainted between the lands", 0, 0.0F, 20, 310, 0.00F, new DrinkEffect(MobEffects.POISON, 100, 3), new DrinkEffect(MobEffects.HUNGER, 100, 3)),
		new DrinkStat("potion.awkward", "well, that's awkward", 0, 0.0F, 20, 310, 0.00F),
		new DrinkStat("potion.thick", "thick and gooey", 0, 0.0F, 15, 310, 0.00F),
		new DrinkStat("potion.mundane", "how mundane of you", 0, 0.0F, 25, 310, 0.00F),
		new DrinkStat("potion.damage", "Instant Damage I", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HARM, 1, 0)),
		new DrinkStat("potion.damage.strong", "Instant Damage II", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.HARM, 1, 1)),
		new DrinkStat("potion.damage.splash", "Instant Damage I", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HARM, 1, 0)),
		new DrinkStat("potion.damage.strong.splash", "Instant Damage II", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.HARM, 1, 1)),
		new DrinkStat("potion.health", "Instant Health I", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HEAL, 1, 0)),
		new DrinkStat("potion.health.strong", "Instant Health II", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.HEAL, 1, 1)),
		new DrinkStat("potion.health.splash", "Instant Health I", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HEAL, 1, 0)),
		new DrinkStat("potion.health.strong.splash", "Instant Health II", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.HEAL, 1, 1)),
		new DrinkStat("potion.jump", "Jump Boost I (3:00)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.JUMP, 3600, 0)),
		new DrinkStat("potion.jump.strong", "Jump Boost II (1:30)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.JUMP, 1800, 1)),
		new DrinkStat("potion.jump.splash", "Jump Boost I (2:15)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.JUMP, 2700, 0)),
		new DrinkStat("potion.jump.strong.splash", "Jump Boost II (1:07)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.JUMP, 1350, 1)),
		new DrinkStat("potion.speed", "Speed I (3:00)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 3600, 0)),
		new DrinkStat("potion.speed.strong", "Speed II (1:30)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 1800, 1)),
		new DrinkStat("potion.speed.long", "Speed I (8:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 9600, 0)),
		new DrinkStat("potion.speed.splash", "Speed I (2:15)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 2700, 0)),
		new DrinkStat("potion.speed.strong.splash", "Speed II (1:07)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 1350, 1)),
		new DrinkStat("potion.speed.long.splash", "Speed I (6:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 7200, 0)),
		new DrinkStat("potion.strength", "Strength I (3:00)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 3600, 0)),
		new DrinkStat("potion.strength.strong", "Strength II (1:30)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.DAMAGE_BOOST, 1800, 1)),
		new DrinkStat("potion.strength.long", "Strength I (8:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.DAMAGE_BOOST, 9600, 0)),
		new DrinkStat("potion.strength.splash", "Strength I (2:15)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 2700, 0)),
		new DrinkStat("potion.strength.strong.splash", "Strength II (1:07)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.DAMAGE_BOOST, 1350, 1)),
		new DrinkStat("potion.strength.long.splash", "Strength I (6:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.DAMAGE_BOOST, 7200, 0)),
		new DrinkStat("potion.regen", "Regeneration I (0:45)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.REGENERATION, 900, 0)),
		new DrinkStat("potion.regen.strong", "Regeneration II (0:22)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.REGENERATION, 450, 1)),
		new DrinkStat("potion.regen.long", "Regeneration I (2:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.REGENERATION, 2400, 0)),
		new DrinkStat("potion.regen.splash", "Regeneration I (0:33)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.REGENERATION, 666, 0)),
		new DrinkStat("potion.regen.strong.splash", "Regeneration II (0:16)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.REGENERATION, 333, 1)),
		new DrinkStat("potion.regen.long.splash", "Regeneration I (1:30)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.REGENERATION, 1800, 0)),
		new DrinkStat("potion.poison", "Poison I (0:45)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.POISON, 900, 0)),
		new DrinkStat("potion.poison.strong", "Poison II (0:22)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.POISON, 450, 1)),
		new DrinkStat("potion.poison.long", "Poison I (2:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.POISON, 2400, 0)),
		new DrinkStat("potion.poison.splash", "Poison I (0:33)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.POISON, 666, 0)),
		new DrinkStat("potion.poison.strong.splash", "Poison II (0:16)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.POISON, 333, 1)),
		new DrinkStat("potion.poison.long.splash", "Poison I (1:30)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.POISON, 1800, 0)),
		new DrinkStat("potion.fireresistance", "Fire Resistance (3:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.FIRE_RESISTANCE, 3600, 0)),
		new DrinkStat("potion.fireresistance.long", "Fire Resistance (8:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.FIRE_RESISTANCE, 9600, 0)),
		new DrinkStat("potion.fireresistance.splash", "Fire Resistance (2:15)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.FIRE_RESISTANCE, 2700, 0)),
		new DrinkStat("potion.fireresistance.long.splash", "Fire Resistance (6:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.FIRE_RESISTANCE, 7200, 0)),
		new DrinkStat("potion.nightvision", "Night Vision (3:00)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.NIGHT_VISION, 3600, 0)),
		new DrinkStat("potion.nightvision.long", "Night Vision (8:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.NIGHT_VISION, 9600, 0)),
		new DrinkStat("potion.nightvision.splash", "Night Vision (2:15)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.NIGHT_VISION, 2700, 0)),
		new DrinkStat("potion.nightvision.long.splash", "Night Vision (6:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.NIGHT_VISION, 7200, 0)),
		new DrinkStat("potion.weakness", "Weakness (1:30)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.WEAKNESS, 1800, 0)),
		new DrinkStat("potion.weakness.long", "Weakness (4:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.WEAKNESS, 4800, 0)),
		new DrinkStat("potion.weakness.splash", "Weakness (1:07)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.WEAKNESS, 1350, 0)),
		new DrinkStat("potion.weakness.long.splash", "Weakness (3:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.WEAKNESS, 3600, 0)),
		new DrinkStat("potion.slowness", "Slowness (1:30)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 1800, 0)),
		new DrinkStat("potion.slowness.long", "Slowness (4:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 4800, 0)),
		new DrinkStat("potion.slowness.splash", "Slowness (1:07)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 1350, 0)),
		new DrinkStat("potion.slowness.long.splash", "Slowness (3:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 3600, 0)),
		new DrinkStat("potion.waterbreathing", "Water Breathing (3:00)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.WATER_BREATHING, 3600, 0)),
		new DrinkStat("potion.waterbreathing.long", "Water Breathing (8:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.WATER_BREATHING, 9600, 0)),
		new DrinkStat("potion.waterbreathing.splash", "Water Breathing (2:15)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.WATER_BREATHING, 2700, 0)),
		new DrinkStat("potion.waterbreathing.long.splash", "Water Breathing (6:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.WATER_BREATHING, 7200, 0)),
		new DrinkStat("potion.invisibility", "Invisibility (3:00)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.INVISIBILITY, 3600, 0)),
		new DrinkStat("potion.invisibility.long", "Invisibility (8:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.INVISIBILITY, 9600, 0)),
		new DrinkStat("potion.invisibility.splash", "Invisibility (2:15)", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.INVISIBILITY, 2700, 0)),
		new DrinkStat("potion.invisibility.long.splash", "Invisibility (6:00)", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.INVISIBILITY, 7200, 0)),
		new DrinkStat("potion.damage.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HARM, 1, 0)),
		new DrinkStat("potion.damage.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.HARM, 1, 1)),
		new DrinkStat("potion.health.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.HEAL, 1, 0)),
		new DrinkStat("potion.health.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.HEAL, 1, 1)),
		new DrinkStat("potion.jump.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.JUMP, 900, 0)),
		new DrinkStat("potion.jump.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.JUMP, 450, 1)),
		new DrinkStat("potion.speed.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 900, 0)),
		new DrinkStat("potion.speed.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 450, 1)),
		new DrinkStat("potion.speed.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SPEED, 2400, 0)),
		new DrinkStat("potion.strength.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 900, 0)),
		new DrinkStat("potion.strength.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.DAMAGE_BOOST, 450, 1)),
		new DrinkStat("potion.strength.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.DAMAGE_BOOST, 2400, 0)),
		new DrinkStat("potion.regen.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.REGENERATION, 222, 0)),
		new DrinkStat("potion.regen.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.REGENERATION, 111, 1)),
		new DrinkStat("potion.regen.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.REGENERATION, 600, 0)),
		new DrinkStat("potion.poison.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.POISON, 222, 0)),
		new DrinkStat("potion.poison.strong.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.POISON, 111, 1)),
		new DrinkStat("potion.poison.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.POISON, 600, 0)),
		new DrinkStat("potion.fireresistance.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.FIRE_RESISTANCE, 900, 0)),
		new DrinkStat("potion.fireresistance.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.FIRE_RESISTANCE, 2400, 0)),
		new DrinkStat("potion.nightvision.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.NIGHT_VISION, 900, 0)),
		new DrinkStat("potion.nightvision.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.NIGHT_VISION, 2400, 0)),
		new DrinkStat("potion.weakness.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.WEAKNESS, 450, 0)),
		new DrinkStat("potion.weakness.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.WEAKNESS, 1200, 0)),
		new DrinkStat("potion.slowness.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 450, 0)),
		new DrinkStat("potion.slowness.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0)),
		new DrinkStat("potion.waterbreathing.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.WATER_BREATHING, 900, 0)),
		new DrinkStat("potion.waterbreathing.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.WATER_BREATHING, 2400, 0)),
		new DrinkStat("potion.invisibility.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, new DrinkEffect(MobEffects.INVISIBILITY, 900, 0)),
		new DrinkStat("potion.invisibility.long.lingering", "lingering_potion", 0, 0.0F, 0, 310, 0.00F, 10, new DrinkEffect(MobEffects.INVISIBILITY, 2400, 0))
	);

	// The TAIL rows: the 10 card-block-outside drink fluids the p33-food-tail table newly
	// registers — the golden-apple ENCHANTED_EFFECT brews (Loader_Fluids.java:607-610, the
	// .setLuminosity(15) family) and the coffee-family drinks (:637-642), the census walk
	// the b2 card could not carry (their fluids did not exist before the tail table).

	/** The 10 tail rows (the p33-food-tail registrations), upstream line order. */
	public static final List<DrinkStat> TAIL_SPECS = List.of(
		new DrinkStat("potion.goldenapplejuice", "A golden Apple in liquid form", 4, 0.2F, 100, 310, 0.75F, 15, new DrinkEffect(MobEffects.ABSORPTION, 2400, 0), new DrinkEffect(MobEffects.REGENERATION, 100, 1)), // upstream "potion.goldenapplejuice" (Loader_Fluids.java:607 — field_76444_x = absorption)
		new DrinkStat("potion.goldencider", "More Resistance, less Regeneration", 4, 0.2F, 100, 310, 0.75F, 15, new DrinkEffect(MobEffects.ABSORPTION, 2400, 1)), // upstream "potion.goldencider" (:608)
		new DrinkStat("potion.idunsapplejuice", "So you got the Idea of using Notch Apples for a drink?", 4, 0.2F, 100, 310, 0.75F, 15, new DrinkEffect(MobEffects.REGENERATION, 600, 4), new DrinkEffect(MobEffects.ABSORPTION, 2400, 0), new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 6000, 0), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 6000, 0)), // upstream "potion.idunsapplejuice" (:609)
		new DrinkStat("potion.notchesbrew", "This is just overpowered", 4, 0.2F, 100, 310, 0.75F, 15, new DrinkEffect(MobEffects.REGENERATION, 700, 4), new DrinkEffect(MobEffects.ABSORPTION, 3000, 1), new DrinkEffect(MobEffects.DAMAGE_RESISTANCE, 7000, 1), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 7000, 0)), // upstream "potion.notchesbrew" (:610)
		new DrinkStat("potion.darkcoffee", "Coffee, dark, without anything else", 2, 0.2F, 5, 312, 0.50F), // upstream "potion.darkcoffee" (:637 — the C+39 fold, the tea-row shape)
		new DrinkStat("potion.darkcafeaulait", "Keeping you awake the whole night", 2, 0.2F, 5, 312, 0.50F), // upstream "potion.darkcafeaulait" (:638)
		new DrinkStat("potion.coffee", "Just the regular morning Coffee", 4, 0.4F, 5, 312, 0.50F), // upstream "potion.coffee" (:639)
		new DrinkStat("potion.cafeaulait", "Sweet Coffee", 4, 0.4F, 5, 312, 0.50F), // upstream "potion.cafeaulait" (:640)
		new DrinkStat("potion.laitaucafe", "You want Coffee to your Sugar?", 4, 0.4F, 5, 312, 0.50F), // upstream "potion.laitaucafe" (:641)
		new DrinkStat("potion.darkchocolatemilk", "A bit bitter, better add a bit Sugar", 4, 0.4F, 10, 310, 0.50F) // upstream "potion.darkchocolatemilk" (:642 — the C+37 fold, the chocolatemilk :643 sibling)
	);

	// The KEPT fills (task p33-food-tail, the reverse-census closure): the 10 potion.-prefixed
	// upstream FoodStatDrink rows whose fluids ALREADY rode the b1/p31 registrations — the
	// b2 census walk stripped only the binnie. prefix, so these slipped the KEPT face exactly
	// like the :371 mineralwater row. Keyed by the ported b1/p31 ids; the GT-only potion
	// channels (ID_STICKY/FLAMMABLE/DEHYDRATION/INSANITY/SLIPPERY) have no modern face, the
	// vanilla MobEffects ride verbatim. Carrier temps stay the b1 honest-default 300 K rows
	// (the declared carrier/stat split); the C+X folds here are the drink-side face.

	/** The 10 kept-fill rows, upstream line order (the fluid carriers predate this card). */
	public static final List<DrinkStat> KEPT_FILLS = List.of(
		new DrinkStat("sake", "Rice Wine", 4, 0.4F, 10, 310, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 1)), // upstream "potion.sake" (Loader_Fluids.java:486 — the b1 "sake" id)
		new DrinkStat("dragonblood", "FUS RO DAH!", 4, 0.4F, 5, 313, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 300, 2)), // upstream "potion.dragonblood" (:496, the C+40 fold — the ID_INSANITY channel has no modern face)
		new DrinkStat("leninade", "Let the Communism flow through you!", 2, 0.2F, 5, 308, 0.50F, new DrinkEffect(MobEffects.DAMAGE_BOOST, 500, 2)), // upstream "potion.leninade" (:552, the C+35 fold)
		new DrinkStat("alcopops", "Don't let your Children drink this junk!", 2, 0.2F, 10, 303, 0.50F, new DrinkEffect(MobEffects.DIG_SPEED, 900, 1)), // upstream "potion.alcopops" (:553, the C+30 fold)
		new DrinkStat("hotsauce", "Very Spicy, I guess?", 2, 0.1F, 10, 317, 0.50F, new DrinkEffect(MobEffects.CONFUSION, 2000, 0), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 2000, 0)), // upstream "potion.hotsauce" (:566, the C+44 fold)
		new DrinkStat("diabolosauce", "As if the Devil made this Sauce", 2, 0.1F, 10, 319, 0.50F, new DrinkEffect(MobEffects.CONFUSION, 3000, 1), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 3000, 0)), // upstream "potion.diabolosauce" (:567, the C+46 fold)
		new DrinkStat("diablosauce", "Diablo always comes back!", 2, 0.1F, 10, 321, 0.50F, new DrinkEffect(MobEffects.CONFUSION, 4000, 1), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 4000, 0)), // upstream "potion.diablosauce" (:568, the C+48 fold)
		new DrinkStat("diablosauce_strong", "[Missing No]", 2, 0.1F, 10, 323, 0.50F, 15, new DrinkEffect(MobEffects.CONFUSION, 9999, 2), new DrinkEffect(MobEffects.FIRE_RESISTANCE, 9999, 9)), // upstream "potion.diablosauce.strong" (:569 — the .setLuminosity(15) row, the C+50 fold)
		new DrinkStat("ambrosia", "It's the Bee Movie, but everytime someone says Bee, it will...", 2, 0.2F, 20, 310, 0.75F, new DrinkEffect(MobEffects.REGENERATION, 150, 0)), // upstream "potion.ambrosia" (:576 — the p31 honey-family id; the ID_STICKY channel has no modern face)
		new DrinkStat("dressing", "For making yourself a Salad", 1, 0.5F, 5, 309, 0.25F) // upstream "potion.dressing" (:591, the C+36 fold — the ID_SLIPPERY channel has no modern face)
	);

	static {
		for (DrinkStat tStat : KEPT_SPECS) REGISTER.put(tStat.fluidId(), tStat);
		for (DrinkStat tStat : KEPT_FILLS) REGISTER.put(tStat.fluidId(), tStat);
		for (DrinkStat tStat : RESIDUAL_SPECS) REGISTER.put(tStat.fluidId(), tStat);
		for (DrinkStat tStat : POTION_SPECS) REGISTER.put(tStat.fluidId(), tStat);
		for (DrinkStat tStat : TAIL_SPECS) REGISTER.put(tStat.fluidId(), tStat);
	}


}

