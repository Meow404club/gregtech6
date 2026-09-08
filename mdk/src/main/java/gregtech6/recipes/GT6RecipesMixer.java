/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM.Mixer C-Foam pour — task p26-c-foam-fluid-refill, the port counterpart of the
 * {@code RM.Mixer} rows inside the {@code for (FluidStack tWater : FL.waters(1000))}
 * loop body of Loader_Recipes_Other.java:230-313 (the rock groups, :249-304) plus the two
 * Pd owned-production rows :485-486 (outside the water loop, inside the 16-colour loop).
 * The :249 ConstructionFoam-dust row and the :484 chemical-dye colorize row sit OUTSIDE
 * this card's transcribed ranges (the spec pins :251-304 + :485-486) — pooled, not dropped.
 *
 * <p><b>The faithful loop transcription</b> (every enclosing multiplier is kept, the
 * Drying :567-568 {@code ANY.Clay.mToThis} walk precedent): the upstream registers one row
 * per <code>waters × clays × sands × rocks</code> combination, so the port pours
 * {@link #WATER_COUNT} × {@code ANY.Clay.mToThis.size()} × {@code ANY.SiO2.mToThis.size()}
 * × Σ(rocks) × 2 sizes rows:
 * <ul>
 * <li>the WATER loop ({@code FL.waters(1000)}, FL.java:689) = four variants at 1000 mB —
 *     Water, MnWtr, DistW, SpDew; the port ids are vanilla water + the GTFluids
 *     mnwtr/distilled_water/spdew registrations (all four live, p16);</li>
 * <li>the CLAY loop ({@code ANY.Clay.mToThis}) and the SAND loop
 *     ({@code ANY.SiO2.mToThis}) walk the live families (membership upstream-identical,
 *     pinned by the tests);</li>
 * <li>the BASE rock group (:252-254) — ten rocks → {@code FL.CFoam} (gt6:cfoam): small
 *     {@code 6U rock + 2U sand + U4 clay + 1000 water → 1000}, big {@code 24U + 8U + U +
 *     4000 → 4000}, durations 128/512, EUt 16;</li>
 * <li>the TWELVE colour groups (:256-302) — each with its OWN rock list → the dyed
 *     {@code DYED_C_FOAMS[dye]} fluids (1000/4000 = 10/40 × the 100-unit bucket): White
 *     :256, Black :260, Gray :264, LightGray :268, LightBlue :272, Lime :276, Green :280,
 *     Red :284, Yellow :288, Orange :292, Pink :296, Brown :300. FOUR colours (Cyan,
 *     Purple, Blue, Magenta) have NO upstream group — the faithful absence is kept
 *     (zero rows, pinned by the test).</li>
 * </ul>
 *
 * <p><b>The Pd owned rows</b> (:485-486, per colour index, NOT inside the water loop):
 * small {@code dustSmall Pd ×1 + dyed 100 → owned 100} (16t), big {@code dust Pd ×1 +
 * dyed 400 → owned 400} (64t), both EUt 16 — the only owned-CFoam production chain.
 *
 * <p><b>The OM.dust → prefix-item mapping</b> (OM.java:460-467, the Drying ice-ladder
 * precedent): {@code U} → {@code OP.dust ×1}, {@code U4} → {@code OP.dustSmall ×1},
 * {@code U*n} → {@code OP.dust ×n}.
 *
 * <p><b>Seams</b> (the GT6RecipesCanner/GT6RecipesDrying shape): the material-item and
 * fluid resolvers are live by default, fixtures injected offline. The tables are LAZILY
 * built (the a9027ac lesson — the @EventBusSubscriber class-load at MOD CONSTRUCTION runs
 * before MT.init(), so no MT reference may be captured in a static initializer).
 *
 * <p><b>Load timing</b>: a self-contained MOD-bus listener pouring at
 * FMLCommonSetup.enqueueWork — the GTFluids DeferredRegisters have fired by then.
 * {@code load()} is idempotent per JVM generation; an unresolvable row skips SILENTLY
 * with a count (the upstream {@code mat()} null-drop semantics).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesMixer {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The EUt column of every C-Foam row (:253-254/:485-486, the shared literal). */
	public static final long CFOAM_EUT = 16;

	/** The duration column of every small rock row (:253/:257/:261/… — the shared 128 literal). */
	public static final long ROCK_DURATION_SMALL = 128;

	/** The duration column of every big rock row (:254/:258/:262/… — the shared 512 literal). */
	public static final long ROCK_DURATION_BIG = 512;

	/** The water input of every small rock row ({@code FL.mul(tWater, 1)} over the 1000 mB FL.waters variant). */
	public static final int WATER_INPUT_SMALL = 1000;

	/** The water input of every big rock row ({@code FL.mul(tWater, 4)}). */
	public static final int WATER_INPUT_BIG = 4000;

	/** The output of every small rock row — {@code FL.CFoam.make(1000)} / {@code FL.mul(DYED_C_FOAMS[dye], 10)}: 10 × the 100-unit bucket. */
	public static final int CFOAM_OUTPUT_SMALL = 1000;

	/** The output of every big rock row — {@code FL.CFoam.make(4000)} / {@code FL.mul(DYED_C_FOAMS[dye], 40)}: 40 × the bucket. */
	public static final int CFOAM_OUTPUT_BIG = 4000;

	/** The Pd-row small duration (:485) and big duration (:486). */
	public static final long PD_DURATION_SMALL = 16;
	public static final long PD_DURATION_BIG = 64;

	/** The Pd-row fluid legs (:485 {@code DYED_C_FOAMS[i]} → owned at the 100-unit make; :486 {@code FL.mul(…, 4)}). */
	public static final int PD_FLUID_SMALL = 100;
	public static final int PD_FLUID_BIG = 400;

	/** The four FL.waters(1000) variants (FL.java:689: Water, MnWtr, DistW, SpDew) — the port lookups live in {@link #resolveWater}. */
	public static final int WATER_COUNT = 4;

	/** The dye index marker of the BASE rock group (its output is gt6:cfoam itself, :253-254). */
	public static final int BASE_GROUP = -1;

	/** The FOUR colours with no upstream Mixer group (:256-302 has no Cyan/Purple/Blue/Magenta block) — the faithful absence. */
	public static final List<Integer> MISSING_COLOURS = List.of(4, 5, 6, 13); // Blue, Purple, Cyan, Magenta (the GTSprayCanItem.DYE_NAMES order)

	/**
	 * One transcribed rock group: the Loader_Recipes_Other.java line of the :252/:256/:260/…
	 * rock-array line, the group's dye index ({@link #BASE_GROUP} for the :252 base group,
	 * whose output is the base gt6:cfoam fluid) and the rock list. LAZY — built on first
	 * {@link #groups()} use (the a9027ac lesson).
	 */
	public record CFoamGroup(String note, int dyeIndex, List<OreDictMaterial> rocks) {}

	/**
	 * The thirteen transcribed groups in upstream file order: the base ten (:252) + the
	 * twelve colour groups (:256-302). Σ colour rocks = 43; Σ all = 53.
	 */
	private static volatile List<CFoamGroup> sGroups = null;

	/** The transcribed groups, captured on first use (one material generation). */
	public static List<CFoamGroup> groups() {
		List<CFoamGroup> tTable = sGroups;
		if (tTable == null) sGroups = tTable = List.of(
			// :252 — the BASE group: ten rocks → FL.CFoam (the gt6:cfoam base fluid)
			new CFoamGroup(":252", BASE_GROUP, List.of(MT.Stone, MT.Concrete, MT.Talc, MT.STONES.Rhyolite, MT.STONES.Gneiss,
					MT.STONES.Shale, MT.Oilshale, MT.Dolomite, MT.STONES.Chert, MT.Asbestos)),
			// :256-:302 — the TWELVE colour groups, each with its own rock list → DYED_C_FOAMS[dye]
			new CFoamGroup(":256", 15, List.of(MT.STONES.Diorite, MT.STONES.Marble, MT.Chalk, MT.CaCO3, MT.Endstone,
					MT.STONES.Livingrock, MT.STONES.Holystone, MT.STONES.Castlerock)),                 // White
			new CFoamGroup(":260",  0, List.of(MT.STONES.Basalt, MT.STONES.Gabbro, MT.STONES.GraniteBlack, MT.STONES.Deepslate)), // Black
			new CFoamGroup(":264",  8, List.of(MT.STONES.Migmatite, MT.STONES.Eclogite, MT.STONES.SpaceRock, MT.STONES.Slate, MT.STONES.Cragrock)), // Gray
			new CFoamGroup(":268",  7, List.of(MT.STONES.Andesite, MT.STONES.Dacite, MT.STONES.Deadrock, MT.STONES.Greywacke,
					MT.STONES.MoonRock, MT.STONES.MoonTurf)),                                           // Light Gray
			new CFoamGroup(":272", 12, List.of(MT.STONES.Blueschist)),                                  // Light Blue
			new CFoamGroup(":276", 10, List.of(MT.STONES.Greenschist, MT.STONES.Betweenstone, MT.PrismarineLight)), // Lime
			new CFoamGroup(":280",  2, List.of(MT.STONES.Pitstone, MT.PrismarineDark, MT.STONES.Mazestone)), // Green
			new CFoamGroup(":284",  1, List.of(MT.STONES.Redrock, MT.STONES.MarsRock, MT.STONES.MarsSand, MT.Netherrack,
					MT.STONES.GraniteRed, MT.STONES.Granite)),                                          // Red
			new CFoamGroup(":288", 11, List.of(MT.STONES.Komatiite, MT.STONES.Templerock)),             // Yellow
			new CFoamGroup(":292", 14, List.of(MT.STONES.Limestone)),                                   // Orange
			new CFoamGroup(":296",  9, List.of(MT.STONES.Quartzite, MT.STONES.Siltstone)),              // Pink
			new CFoamGroup(":300",  3, List.of(MT.STONES.Umber, MT.STONES.Kimberlite)));                // Brown
		return tTable;
	}

	/** The fluid seam: water-variant index → the port fluid (vanilla water + the three p16 registrations), fixtures injected offline. */
	static IntFunction<Fluid> sWaterResolver = GT6RecipesMixer::resolveWater;

	/** The C-Foam seam: (dye index, owned) → the {@code gt6:cfoam[_owned]_<dye>} source fluid (the live default), fixtures injected offline. */
	static BiFunction<Integer, Boolean, Fluid> sCfoamResolver = (aIndex, aOwned) -> GTFluids.cfoam(aIndex, aOwned).source.get();

	/** The base-C-Foam seam: the {@code gt6:cfoam} source fluid, fixtures injected offline. */
	static java.util.function.Supplier<Fluid> sBaseCfoamResolver = () -> GTFluids.CFOAM.get();

	/** The material-item seam (the Drying shape): (prefix, material) → the registered prefix item, fixtures injected offline. */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesMixer::resolveItem;

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner/Drying form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesMixer::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the GTFluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesMixer::load);
	}

	/**
	 * Pours the C-Foam Mixer rows into {@link GT6RecipeMaps#MIXER}: the rock groups
	 * (:252-304, every water × clay × sand × rock × size combination) + the 32 Pd owned
	 * rows (:485-486). Idempotent; an unresolvable row skips with a count (the upstream
	 * {@code mat()} null drops).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java), tests may race it
		RecipeMap tMap = GT6RecipeMaps.MIXER;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (CFoamGroup tGroup : groups()) {
			for (int tWater = 0; tWater < WATER_COUNT; tWater++) {
				for (OreDictMaterial tClay : ANY.Clay.mToThis) {
					for (OreDictMaterial tSand : ANY.SiO2.mToThis) {
						for (OreDictMaterial tRock : tGroup.rocks()) {
							Recipe tSmall = rockRecipe(tGroup, tWater, tClay, tSand, tRock, false);
							if (tSmall == null) tSkipped++;
							else {tMap.addRecipe(tSmall); tPoured++;}
							Recipe tBig = rockRecipe(tGroup, tWater, tClay, tSand, tRock, true);
							if (tBig == null) tSkipped++;
							else {tMap.addRecipe(tBig); tPoured++;}
						}
					}
				}
			}
		}
		for (int i = 0; i < 16; i++) {
			Recipe tSmall = pdRecipe(i, false);
			if (tSmall == null) tSkipped++;
			else {tMap.addRecipe(tSmall); tPoured++;}
			Recipe tBig = pdRecipe(i, true);
			if (tBig == null) tSkipped++;
			else {tMap.addRecipe(tBig); tPoured++;}
		}
		sLoaded = true;
		LOGGER.info("GT6 Mixer poured: {} loaded, {} skipped (unresolvable rock/sand/clay/pd items, = upstream mat() drops)", tPoured, tSkipped);
	}

	/**
	 * The :253/:254 (base) or :257-258/:261-262/… (colour) row for ONE rock at ONE size —
	 * buffered T, EUt 16, duration 128/512, three dust inputs (rock 6U/24U, sand 2U/8U,
	 * clay U4/U), water 1000/4000, C-Foam 1000/4000 out. Null when any leg fails to resolve
	 * (the silent skip).
	 */
	@Nullable
	static Recipe rockRecipe(CFoamGroup aGroup, int aWater, OreDictMaterial aClay, OreDictMaterial aSand, OreDictMaterial aRock, boolean aBig) {
		Item tRock = sMaterialItemResolver.apply(OP.dust, aRock);
		if (tRock == null) return null;
		Item tSand = sMaterialItemResolver.apply(OP.dust, aSand);
		if (tSand == null) return null;
		// the clay leg: small = OM.dust(tClay, U4) → dustSmall x1; big = OM.dust(tClay, U) → dust x1
		Item tClay = sMaterialItemResolver.apply(aBig ? OP.dust : OP.dustSmall, aClay);
		if (tClay == null) return null;
		Fluid tWater = sWaterResolver.apply(aWater);
		if (tWater == null) return null;
		Fluid tOut = aGroup.dyeIndex() == BASE_GROUP ? sBaseCfoamResolver.get() : sCfoamResolver.apply(aGroup.dyeIndex(), false);
		if (tOut == null) return null;
		// upstream :253/:257/:261/… — addRecipeX(T,F,F,F,T, 16, 128, ST.array(dust(tRock, U*6),
		// dust(tSand, U*2), dust(tClay, U4)), FL.mul(tWater, 1), [FL.CFoam|FL.mul(DYED_C_FOAMS[dye], 10)].make/equiv, ZL_IS)
		// and the :254/… big row (U*24 / U*8 / U, water 4, output 40, duration 512)
		return new Recipe(true,
				new ItemStack[] {
						new ItemStack(tRock, aBig ? 24 : 6),
						new ItemStack(tSand, aBig ? 8 : 2),
						new ItemStack(tClay, 1)},
				new ItemStack[0],
				new FluidStack[] {new FluidStack(tWater, aBig ? WATER_INPUT_BIG : WATER_INPUT_SMALL)},
				new FluidStack[] {new FluidStack(tOut, aBig ? CFOAM_OUTPUT_BIG : CFOAM_OUTPUT_SMALL)},
				aBig ? ROCK_DURATION_BIG : ROCK_DURATION_SMALL, CFOAM_EUT, 0);
	}

	/**
	 * The :485 (small) or :486 (big) Pd owned row for colour index i — buffered T, EUt 16,
	 * one Pd dust input, dyed → owned at equal amounts. Null when any leg fails to resolve.
	 */
	@Nullable
	static Recipe pdRecipe(int aIndex, boolean aBig) {
		// OM.dust(MT.Pd, U4) → dustSmall x1 (:485); OM.dust(MT.Pd) → dust x1 (:486)
		Item tPd = sMaterialItemResolver.apply(aBig ? OP.dust : OP.dustSmall, MT.Pd);
		if (tPd == null) return null;
		Fluid tIn = sCfoamResolver.apply(aIndex, false);
		if (tIn == null) return null;
		Fluid tOut = sCfoamResolver.apply(aIndex, true);
		if (tOut == null) return null;
		// upstream :485 — addRecipe1(T, 16, 16, OM.dust(MT.Pd, U4), DYED_C_FOAMS[i], DYED_C_FOAMS_OWNED[i], ZL_IS)
		// upstream :486 — addRecipe1(T, 16, 64, OM.dust(MT.Pd), FL.mul(DYED_C_FOAMS[i], 4), FL.mul(DYED_C_FOAMS_OWNED[i], 4), ZL_IS)
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tPd, 1)},
				new ItemStack[0],
				new FluidStack[] {new FluidStack(tIn, aBig ? PD_FLUID_BIG : PD_FLUID_SMALL)},
				new FluidStack[] {new FluidStack(tOut, aBig ? PD_FLUID_BIG : PD_FLUID_SMALL)},
				aBig ? PD_DURATION_BIG : PD_DURATION_SMALL, CFOAM_EUT, 0);
	}

	/** The live water lookup — FL.waters(1000) = Water/MnWtr/DistW/SpDew (FL.java:689), all four registered in the port. */
	@Nullable
	static Fluid resolveWater(int aIndex) {
		return switch (aIndex) {
			case 0 -> Fluids.WATER;
			case 1 -> GTFluids.MNWTR.source.get();
			case 2 -> GTFluids.DISTILLED_WATER.source.get();
			case 3 -> GTFluids.SPDEW.source.get();
			default -> null;
		};
	}

	/** The live material-item lookup (GTMaterialItems.get) — null when the pair has no item-path item (the Drying shape). */
	@Nullable
	static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag and the captured tables so a fresh generation can re-pour. */
	static void resetForTest() {sLoaded = false; sGroups = null;}

	private GT6RecipesMixer() {}
}
