
package gregtech6.recipes;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;

/**
 * The {@code RM.Bath} static-row loader — task p26-kitchen-pot-bowl, the pour face the
 * {@link GT6RecipeMaps#BATH} field doc promises ({@code GT6RecipesBath}, FMLCommonSetup,
 * the {@link GT6RecipesMixer} shape). The upstream Bath rows split into:
 *
 * <p><b>The wood-oil ladder — Handlers:660-669, expanded here</b> (the P8 ruling:
 * handler → registration-time expansion, the OreChain/ShCL form):
 * {@code ANY.WoodUntreated.mToThis × 9 oil templates} — 7 treated rows
 * ({@code Oil_Seed/Lin/Hemp/Nut/Olive/Sunflower/Creosote} at 100 mB → the WoodTreated
 * plank) + 2 polished rows ({@code Oil_Fish} 1000 mB / {@code Oil_Whale} 500 mB → the
 * WoodPolished plank), every row duration 144 / EUt 0 / buffered (the
 * {@code addRecipe1(T, 0, 144, plank, oil, NF, treated)} shape). <b>The current port
 * universe pours ZERO rows from this ladder</b>: the plank prefix is not an item-path
 * prefix ({@code GTMaterialItems.itemPathPrefixes} has no {@code plank}) so no plank
 * item resolves, and the plant/fish-oil fluids are port-absent (only creosote lives,
 * {@code gt6:creosote}) so no oil resolves either. The ladder is wired LIVE against the
 * resolvers — the moment the plank items and the oil family land, the rows pour with no
 * code change here (the pour-face-forever rationale, pinned both ways by the test).
 *
 * <p><b>The declared dormancies (each upstream-conditional on an absent universe face):</b>
 * <ul>
 * <li>the IE/ERE treated/white planks band (GT6_Main.java:377-384) — {@code addFakeRecipe}
 *     rows (fake rows never join {@code mRecipeList}, RecipeMap.addRecipe :123) keyed on
 *     IE/ERE items that do not exist in the port;</li>
 * <li>the {@code FL.Mana_TE} band (Handlers:677-691) — upstream gated on
 *     {@code FL.Mana_TE.exists()}, the port has no Thaumcraft/Thermal fluid family;</li>
 * <li>the {@code Pb → Midasium} row (Handlers:693) and the {@code Holywater} rows
 *     (:695-696) — MT.Midasium / FL.Holywater are port-absent;</li>
 * <li>the {@code RecipeMapBath.findRecipe} on-demand arms (RecipeMapBath.java:57-183 —
 *     the plank treatment keyed through {@code WoodDictionary.PLANKS_ANY}, the Atum loot
 *     wash, the 1.7 {@code ItemArmor} dye rows, the {@code IItemColorableRGB} chlorine
 *     rows, the projectile-enchanting rows and the 1.7 potion-NBT food rows) — the P8
 *     handler layer the port does not carry, declared on the BATH field doc (the
 *     59c34422 map-declaration commit).</li>
 * </ul>
 *
 * <p><b>Seams</b> (the GT6RecipesMixer shape): the plank-item and oil-fluid resolvers
 * are live by default, fixtures injected offline. The tables are LAZILY touched — no
 * static initializer captures an MT reference (the a9027ac lesson). {@code load()} is
 * idempotent per JVM generation; an unresolvable row skips SILENTLY with a count (the
 * upstream {@code mat()} null-drop semantics), and the poured/dormant counts stay
 * readable for the offline reconciliation ({@link #lastPoured()}/{@link #lastSkipped()}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesBath {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The treated-leg oil input (:661-667 — the shared 100 literal). */
	public static final int OIL_TREATED_INPUT = 100;
	/** The fish-oil polished input (:668). */
	public static final int OIL_FISH_INPUT = 1000;
	/** The whale-oil polished input (:669). */
	public static final int OIL_WHALE_INPUT = 500;
	/** The row duration (:660-669 — the shared 144 literal). */
	public static final int WOOD_LADDER_DURATION = 144;
	/** The row EUt (:660-669 — 0, the manual map's AMP 1 / power 1 face). */
	public static final long WOOD_LADDER_EUT = 0;
	/** The treated templates (:661-667) and the polished ones (:668-669). */
	public static final int TREATED_TEMPLATES = 7;
	public static final int POLISHED_TEMPLATES = 2;
	/** The whole ladder: {@code 9 templates × ANY.WoodUntreated.mToThis} rows. */
	public static final int WOOD_LADDER_TEMPLATES = TREATED_TEMPLATES + POLISHED_TEMPLATES;

	/** The treated-leg oil names (:661-667, upstream FL order). */
	public static final List<String> TREATED_OILS = List.of("seed", "lin", "hemp", "nut", "olive", "sunflower", "creosote");

	/**
	 * One oil template: the FL name suffix, the input mB (:661-669) and the output grade
	 * ({@code polished} = the :668-669 fish/whale legs, else the :661-667 treated legs).
	 */
	public record OilLeg(String name, int amount, boolean polished) {}

	/** The nine templates in upstream file order (:661-669). */
	public static List<OilLeg> oilLegs() {
		List<OilLeg> rLegs = new java.util.ArrayList<>(WOOD_LADDER_TEMPLATES);
		for (String tOil : TREATED_OILS) rLegs.add(new OilLeg(tOil, OIL_TREATED_INPUT, false));
		rLegs.add(new OilLeg("fish", OIL_FISH_INPUT, true));
		rLegs.add(new OilLeg("whale", OIL_WHALE_INPUT, true));
		return rLegs;
	}

	/** The plank-item seam: (prefix, material) → the registered item (the Mixer resolver share). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sPlankItemResolver = GT6RecipesMixer::resolveItem;

	/** The oil-fluid seam: the live default resolves ONLY creosote (gt6:creosote); the plant/fish family is port-absent. */
	static Function<OilLeg, Fluid> sOilFluidResolver = GT6RecipesBath::resolveOil;

	/** Poured flag — one generation, one pour (the Mixer form). */
	private static boolean sLoaded = false;
	/** The reconciliation counters of the last {@link #load()} (the offline audit face). */
	private static volatile int sLastPoured = 0, sLastSkipped = 0;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner/Drying/Mixer form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesBath::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the map and the fluid registrations have fired by then. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesBath::load);
	}

	/**
	 * Pours the wood-oil ladder into {@link GT6RecipeMaps#BATH}. Idempotent; an
	 * unresolvable row skips with a count. The current universe pours 0 (the class doc) —
	 * the ladder is the live pour face, not dead code.
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod, tests may race it
		RecipeMap tMap = GT6RecipeMaps.BATH;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		java.util.Collection<OreDictMaterial> tWoods = ANY.WoodUntreated.mToThis == null ? List.of() : ANY.WoodUntreated.mToThis;
		for (OreDictMaterial tWood : tWoods) {
			for (OilLeg tLeg : oilLegs()) {
				Recipe tRow = woodOilRecipe(tWood, tLeg);
				if (tRow == null) tSkipped++;
				else {tMap.addRecipe(tRow); tPoured++;}
			}
		}
		sLastPoured = tPoured;
		sLastSkipped = tSkipped;
		LOGGER.info("GT6 RM.Bath static rows poured {} / dormant {} (the Handlers:660-669 wood-oil ladder over {} untreated woods; "
				+ "the IE/ERE fake band, the Mana_TE / Pb-Midasium / Holywater legs and the RecipeMapBath on-demand arms are the declared pool cuts)",
				tPoured, tSkipped, tWoods.size());
	}

	/**
	 * One ladder row — the {@code addRecipe1(T, 0, 144, plank, oil, NF, treatedPlank)}
	 * shape: 1 plank in, the oil at the template amount, the treated/polished plank out.
	 * Null when any leg fails to resolve (the silent skip).
	 */
	@Nullable
	static Recipe woodOilRecipe(OreDictMaterial aWood, OilLeg aOil) {
		Item tPlank = sPlankItemResolver.apply(OP.plank, aWood);
		if (tPlank == null) return null;
		Fluid tOil = sOilFluidResolver.apply(aOil);
		if (tOil == null) return null;
		OreDictMaterial tOutMaterial = aOil.polished() ? MT.WoodPolished : MT.WoodTreated;
		Item tOutPlank = sPlankItemResolver.apply(OP.plank, tOutMaterial);
		if (tOutPlank == null) return null;
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tPlank, 1)},
				new ItemStack[] {new ItemStack(tOutPlank, 1)},
				new FluidStack[] {new FluidStack(tOil, aOil.amount())},
				new FluidStack[0],
				WOOD_LADDER_DURATION, WOOD_LADDER_EUT, 0);
	}

	/** The live oil lookup — creosote is the one Loader_Fluids oil the port carries (gt6:creosote, the Coke Oven by-product). */
	@Nullable
	static Fluid resolveOil(OilLeg aLeg) {
		// the Loader_Fluids.java FL.Oil_* family (Seed/Lin/Hemp/Nut/Olive/Sunflower/Fish/Whale) is port-absent;
		// the isPresent guard is the offline-test arm (no mod registry in the forge test JVM — the Mixer resolver shape)
		//? if forge {
		return "creosote".equals(aLeg.name()) && GTFluids.CREOSOTE.isPresent() ? GTFluids.CREOSOTE.get() : null;
		//?} else {
		/*return "creosote".equals(aLeg.name()) ? GTFluids.CREOSOTE.get() : null; // 21.1: the Supplier face, the FML test JVM binds it
		*///?}
	}

	/** The last pour's poured count (the offline reconciliation face). */
	public static int lastPoured() {return sLastPoured;}

	/** The last pour's dormant count (the offline reconciliation face). */
	public static int lastSkipped() {return sLastSkipped;}

	/** Test seam: clears the poured flag and the counters so a fresh generation can re-pour. */
	static void resetForTest() {sLoaded = false; sLastPoured = 0; sLastSkipped = 0;}

	private GT6RecipesBath() {}
}
