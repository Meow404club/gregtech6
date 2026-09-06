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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
 * The RM.Drying recipe book — task p14-loop-closure-chain (the water family foundation)
 * extended by task p16-drying-rows-backfill: the port counterpart of the
 * {@code RM.Drying} rows of Loader_Recipes_Chem.java:525-532 (the water-to-distilled-water
 * half) — the ice/snow rows :510-522 land with the same card, the Distillery rows
 * :534-541 stay pooled (the circuit-selector-gated half, the distillery card's domain).
 *
 * <p><b>The seven poured water rows</b> (:525-532): {@code Water 10 → DistW 8} (:525, the
 * canonical distillation loop row that closes the P14 loop), then the p16 backfill —
 * {@code SpDew 10 → 8} (:526), {@code MnWtr 10 → 8} (:527), {@code Water_Geothermal 25 →
 * 20} (:528), {@code Water_Boiling 25 → 20} (:529), {@code Hot_Water 25 → 20} (:531) and
 * {@code Cold_Water 25 → 20} (:532) — all EUt 16, duration 16, the buffered
 * {@code addRecipe0(T, ...)} fluid-only shape. Six of the seven inputs are the
 * {@link GTFluids.AQUA_SPECS} registrations of task p16-aqua-fluids (spdew/mnwtr/
 * water_geothermal/water_boiling/hot_water/cold_water); water is vanilla.
 *
 * <p><b>The one pooled water row</b> (:530): {@code Water_Hot} — the IC2 hot-water alias
 * ("ic2hotwater", FL.java:116) is deliberately unregistered in this port (outside the
 * p16-aqua-fluids six), so its row keeps the upstream absent-fluid skip: the resolver
 * answers null and {@link #load()} drops the row with the upstream
 * {@code if (FL.Water_Hot.exists())} guard semantics (the guard sits at the END of the
 * :528 line, before the :529 pour).
 *
 * <p><b>The ice/snow family</b> (:510-522, the p16-drying-rows-backfill second half): THIRTEEN
 * one-item-input rows — the upstream census (the task card's "12" counts 12, the file holds 13
 * lines, every one transcribed): six Ice dust/gem rows (:510-515), the two vanilla ice blocks
 * (:516-517), three Snow dust rows (:518-520), the snowball (:521) and the vanilla snow block
 * (:522). The {@code OM.dust(mat, U9/U4)} amounts become the
 * dustTiny/dustSmall prefix items (OM.java:460-467: amount U9 → {@code OP.dustTiny.mat(mat, 1)},
 * U4 → dustSmall, U → dust), the {@code gemChipped/gemFlawed/gem.mat(MT.Ice, 1)} calls map
 * one-to-one. The vanilla identities are the flattening map: 1.7.10 {@code Blocks.snow} is the
 * FULL snow block → 1.20.1 {@code Blocks#SNOW_BLOCK} (1.20.1 {@code Blocks#SNOW} is the LAYER
 * block, Blocks.java:2147-2177); {@code Blocks.ice}/{@code packed_ice}/{@code Items.snowball}
 * are unchanged. Every row: EUt 16, duration = 4× the output litres (the upstream
 * {@code 111 * 4 .. 2000 * 4} literals), one item in → DistW out, the buffered
 * {@code addRecipe1(T, ...)} shape. Unresolvable inputs skip with the upstream
 * {@code mat()} null-drop semantics (the GT6RecipesShCL precedent).
 *
 * <p><b>Row shape</b>: fluid-in AND fluid-out, empty item arrays — the {@link RecipeMap#addRecipe}
 * ghost guard does not fire (the fluid leg is a real input). The offline lookup shape
 * mirrors the live machine call exactly: {@code findRecipe(..., tanks, new ItemStack[1])}
 * — the RecipeMap's empty-item-array hard return (RecipeMap.java:138) needs the length-1
 * slot array the machine always passes (TileEntityBasicMachine.java:512, mInputItemsCount
 * slots that may be empty), and the row's zero item inputs pass the stack check trivially.
 *
 * <p><b>The salt/brine family</b> (:544-557, task p19-drying-rows-backfill-2): of the seven
 * upstream Drying rows between the ice and mineral blocks only the two UNGUARDED rows pour —
 * :548 {@code Ocean 7000 → DistW 6750 + dustSmall NaCl} (11200 ticks) and :553
 * {@code Dirty_Water 8000 → DistW 7000 + vanilla dirt} (16000 ticks), both the buffered
 * {@code addRecipe0(T, ...)} shape with one item output. Their input fluids are the two
 * {@link GTFluids#SIMPLE_LIQUID_SPECS} registrations (gt6:seawater / gt6:waterdirty — the
 * architect ruling keeps them OUT of AQUA_SPECS: upstream SIMPLE+LIQUID only, no WATER tag).
 * Upstream those ids are external-mod fluids (no {@code FL.create} anywhere in GT6) and the
 * two rows would drop without the mod; the port registers the live carriers and keeps the
 * rows (declared port-owned decision). The five {@code FL.exists()}-guarded rows and the two
 * material-liquid rows are pooled — see {@link #SKIPPED_UPSTREAM}.
 *
 * <p><b>The mineral-dehydration family</b> (:559-566): eight dust-in rows —
 * {@code Mirabilite 7 → Na2SO4 7} (60000 ticks, DistW 30000), {@code FeO3H3 14 → Fe2O3 5}
 * (18000, 9000), {@code AlO3H3 14 → Al2O3 5} (18000, 9000), {@code H2WO4 7 → WO3 4}
 * (6000, 3000), {@code Bischofite 1 → MgCl2 1} (4000, 2000), {@code Trona 1 → Na2CO3 1}
 * (2000, 1000), {@code Gypsum 1 → CaSO4 1} (2000, 1000) and {@code Perlite 1 → Obsidian 1}
 * (2000, 1000) — all EUt 16, no fluid input (NF), the DistW output plus the dust item
 * output, the buffered {@code addRecipe1(T, ...)} shape.
 *
 * <p><b>The clay→ceramic loop</b> (:567-568): {@code for (tMat : ANY.Clay.mToThis)}
 * {@code dust x1 → DistW 500 + dust Ceramic x1}, 1000 ticks — the port walks the live
 * {@code ANY.Clay.mToThis} family (transcription of the loop itself; the membership
 * {Clay, ClayBrown, ClayRed, Bentonite, Palygorskite, Kaolinite} is upstream-identical and
 * pinned by the test census — the task card's "Clay 2 rows" counts the two upstream source
 * LINES, the expansion is material-count-driven).
 *
 * <p><b>The BlockDiggable row</b> (BlockDiggable.java:73): the vanilla clay block item →
 * hardened_clay, 64 ticks, EUt 16, NO fluids (NF/NF) — 1.20.1 identity
 * {@code Blocks.CLAY → Blocks.TERRACOTTA} (1.7.10 "hardened_clay" is the 1.13-flattening
 * "terracotta", Blocks.java:3565). Same file as the rest of the Drying book (the architect
 * card: 同文件落).
 *
 * <p><b>Load timing</b> (the GT6RecipesEngineFuels precedent, ADR ruling ②): a self-contained
 * MOD-bus listener pouring at FMLCommonSetup.enqueueWork — by then the DeferredRegisters of
 * {@link GTFluids} have fired, so the distilled-water output resolves. {@code load()} is
 * idempotent per JVM generation; rows that lose their fluid or item skip SILENTLY with a
 * count (must not block the others).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesDrying {


	private static final Logger LOGGER = LogUtils.getLogger();

	/** The gt6 fluid id keys the table rows reference (the upstream FL shorthands, snake-cased). */
	public static final String FLUID_WATER = "water", FLUID_DISTW = "distw", FLUID_SPDEW = "spdew",
			FLUID_MNWTR = "mnwtr", FLUID_GEOTHERMAL = "water_geothermal", FLUID_BOILING = "water_boiling",
			FLUID_HOT = "water_hot", FLUID_HOT_WATER = "hot_water", FLUID_COLD = "cold_water",
			FLUID_SEAWATER = "seawater", FLUID_WATERDIRTY = "waterdirty";

	/**
	 * The fluid seam: the live lookups by default (vanilla water in, the registered
	 * distilled water out, the six p16-aqua-fluids registrations, and the deliberate
	 * null for the unregistered water_hot alias), fixtures injected offline (the
	 * GT6RecipesEngineFuels sFluidResolver precedent).
	 */
	static Function<String, Fluid> sFluidResolver = GT6RecipesDrying::resolveFluid;

	/**
	 * One transcribed upstream row: the input fluid id, the in/out litre amounts and the
	 * note of the Loader_Recipes_Chem.java line. EUt 16 and duration 16 are family
	 * constants (every :525-532 row carries them).
	 */
	public record DryingRow(String note, String input, long inAmount, long outAmount) {}

	/**
	 * The eight transcribed water-family rows, order mirroring the upstream file order
	 * (:525-532). Lazily built — the @EventBusSubscriber class-load at MOD CONSTRUCTION
	 * must not capture registry state (it is pure strings anyway; the laziness keeps the
	 * loaders structurally identical).
	 */
	private static volatile List<DryingRow> sTable = null;

	/** The transcribed rows, captured on first use. */
	public static List<DryingRow> table() {
		List<DryingRow> tTable = sTable;
		if (tTable == null) sTable = tTable = List.of(
		// Loader_Recipes_Chem.java:525 — the Water row, the P14 loop-closure foundation
		new DryingRow(":525", FLUID_WATER     , 10,  8),
		// Loader_Recipes_Chem.java:526-532 — the p16-aqua-fluids six + the :530 pooled alias
		new DryingRow(":526", FLUID_SPDEW     , 10,  8),
		new DryingRow(":527", FLUID_MNWTR     , 10,  8),
		new DryingRow(":528", FLUID_GEOTHERMAL, 25, 20),
		new DryingRow(":529", FLUID_BOILING   , 25, 20),
		new DryingRow(":530", FLUID_HOT       , 25, 20),
		new DryingRow(":531", FLUID_HOT_WATER , 25, 20),
		new DryingRow(":532", FLUID_COLD      , 25, 20));
		return tTable;
	}

	/**
	 * The item/item seam for the ice family rows (the GT6RecipesShCL precedent): the live
	 * lookups by default ({@link GTMaterialItems#get} and the vanilla supplier), fixtures
	 * injected offline — new Items cannot be created offline, so the tests map the
	 * (prefix, material) pairs onto distinct vanilla registry entries.
	 */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesDrying::resolveItem;
	/** The vanilla item seam (only for offline determinism; live it just dereferences the supplier). */
	static Function<Supplier<Item>, Item> sVanillaItemResolver = Supplier::get;

	/**
	 * One transcribed ice/snow row: the note of the Loader_Recipes_Chem.java line, the
	 * single item input (a vanilla item reference or a (prefix, material) pair — always
	 * stack size 1 upstream), the distilled output litres and the duration. EUt 16 is a
	 * family constant (every :510-522 row carries it).
	 */
	public record IceRow(String note, @Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix,
			@Nullable OreDictMaterial material, int count, long outAmount, long duration) {
		/** The vanilla-block/item input form ({@code ST.make(Blocks.X, 1, W)} / {@code ST.make(Items.X, 1, W)}). */
		public static IceRow ofVanilla(String aNote, Supplier<Item> aItem, long aOut, long aDuration) {
			return new IceRow(aNote, aItem, null, null, 1, aOut, aDuration);
		}

		/** The material-item input form ({@code OM.dust(mat, amount)} / {@code prefix.mat(mat, 1)}). */
		public static IceRow ofMaterial(String aNote, OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aOut, long aDuration) {
			return new IceRow(aNote, null, aPrefix, aMaterial, 1, aOut, aDuration);
		}
	}

	/**
	 * The thirteen transcribed ice/snow rows (Loader_Recipes_Chem.java:510-522), order
	 * mirroring the upstream file order. Lazily built — the @EventBusSubscriber class-load
	 * at MOD CONSTRUCTION runs before MT.init()/OP.init(), so the OP/MT references must
	 * not be captured in static initializers (the a9027ac lesson, the ShCL javadoc:153-157).
	 *
	 * <p>The {@code OM.dust(mat, U9/U4)} rows become the dustTiny/dustSmall prefix items:
	 * upstream OM.dust (OM.java:460-467) maps amount U9 → {@code OP.dustTiny.mat(mat, 1)},
	 * U4 → {@code OP.dustSmall.mat(mat, 1)}, U → {@code OP.dust.mat(mat, 1)}. The :522
	 * vanilla identity is 1.7.10 {@code Blocks.snow} (the FULL snow block) → 1.20.1
	 * {@code Blocks#SNOW_BLOCK}; 1.20.1 {@code Blocks#SNOW} is the LAYER block.
	 */
	private static volatile List<IceRow> sIceTable = null;

	/** The transcribed ice/snow rows, captured on first use (one material generation). */
	public static List<IceRow> iceTable() {
		List<IceRow> tTable = sIceTable;
		if (tTable == null) sIceTable = tTable = List.of(
		// Loader_Recipes_Chem.java:510-512 — the Ice dust ladder (OM.dust U9/U4/U)
		IceRow.ofMaterial(":510", OP.dustTiny    , MT.Ice,  111,  111 * 4),
		IceRow.ofMaterial(":511", OP.dustSmall   , MT.Ice,  250,  250 * 4),
		IceRow.ofMaterial(":512", OP.dust        , MT.Ice, 1000, 1000 * 4),
		// :513-515 — the Ice gem ladder (gemChipped/gemFlawed/gem, .mat(mat, 1) verbatim)
		IceRow.ofMaterial(":513", OP.gemChipped  , MT.Ice,  250,  250 * 4),
		IceRow.ofMaterial(":514", OP.gemFlawed   , MT.Ice,  500,  500 * 4),
		IceRow.ofMaterial(":515", OP.gem         , MT.Ice, 1000, 1000 * 4),
		// :516-517 — the vanilla ice blocks
		IceRow.ofVanilla(":516", () -> Blocks.ICE.asItem()       , 1000, 1000 * 4),
		IceRow.ofVanilla(":517", () -> Blocks.PACKED_ICE.asItem(), 2000, 2000 * 4),
		// :518-520 — the Snow dust ladder (OM.dust U9/U4/U)
		IceRow.ofMaterial(":518", OP.dustTiny    , MT.Snow,  111,  111 * 4),
		IceRow.ofMaterial(":519", OP.dustSmall   , MT.Snow,  250,  250 * 4),
		IceRow.ofMaterial(":520", OP.dust        , MT.Snow, 1000, 1000 * 4),
		// :521-522 — the snowball and the vanilla snow BLOCK (:522, see the javadoc identity note)
		IceRow.ofVanilla(":521", () -> Items.SNOWBALL            ,  250,  250 * 4),
		IceRow.ofVanilla(":522", () -> Blocks.SNOW_BLOCK.asItem(), 1000, 1000 * 4));
		return tTable;
	}

	/**
	 * One transcribed salt-family row (Loader_Recipes_Chem.java:548/:553, task
	 * p19-drying-rows-backfill-2): the input fluid id, the in/out litre amounts, the
	 * duration (NOT the water family's flat 16 — these rows carry their own literals),
	 * and the single item output (stack size 1 — the upstream {@code OM.dust(MT.NaCl, U4)}
	 * is the OM.dust U4 → dustSmall x1 mapping, OM.java:460-467).
	 */
	public record SaltRow(String note, String input, long inAmount, long outAmount, long duration,
			@Nullable Supplier<Item> outVanilla, @Nullable OreDictPrefix outPrefix, @Nullable OreDictMaterial outMaterial) {
		/** The material-item output form ({@code OM.dust(mat, U4)} → dustSmall x1). */
		public static SaltRow ofMaterialOut(String aNote, String aInput, long aIn, long aOut, long aDuration,
				OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
			return new SaltRow(aNote, aInput, aIn, aOut, aDuration, null, aPrefix, aMaterial);
		}

		/** The vanilla-item output form ({@code ST.make(Blocks.dirt, 1, 0)}). */
		public static SaltRow ofVanillaOut(String aNote, String aInput, long aIn, long aOut, long aDuration, Supplier<Item> aItem) {
			return new SaltRow(aNote, aInput, aIn, aOut, aDuration, aItem, null, null);
		}
	}

	/**
	 * The two transcribed salt rows (:548/:553 — the ONLY unguarded rows of the :544-557
	 * block; the guarded/material-liquid neighbours are pooled, see
	 * {@link #SKIPPED_UPSTREAM}). Lazily built (the a9027ac lesson, see iceTable).
	 */
	private static volatile List<SaltRow> sSaltTable = null;

	/** The transcribed salt rows, captured on first use (one material generation). */
	public static List<SaltRow> saltTable() {
		List<SaltRow> tTable = sSaltTable;
		if (tTable == null) sSaltTable = tTable = List.of(
		// Loader_Recipes_Chem.java:548 — FL.Ocean ("seawater", FL.java:125), no exists() guard
		SaltRow.ofMaterialOut(":548", FLUID_SEAWATER  , 7000, 6750, 11200, OP.dustSmall, MT.NaCl),
		// Loader_Recipes_Chem.java:553 — FL.Dirty_Water ("waterdirty", FL.java:127), no guard
		SaltRow.ofVanillaOut (":553", FLUID_WATERDIRTY, 8000, 7000, 16000, () -> Blocks.DIRT.asItem()));
		return tTable;
	}

	/**
	 * One transcribed dehydration row: the single item input (a vanilla item or a
	 * (prefix, material) dust pair), the distilled output litres, the single item output
	 * and the duration. EUt 16 is a family constant (every :559-568/:73 row carries it).
	 * The BlockDiggable row carries no fluid leg (outAmount 0 = no fluid output).
	 */
	public record DehydrationRow(String note,
			@Nullable Supplier<Item> inVanilla, @Nullable OreDictPrefix inPrefix, @Nullable OreDictMaterial inMaterial, int inCount,
			long outAmount,
			@Nullable Supplier<Item> outVanilla, @Nullable OreDictPrefix outPrefix, @Nullable OreDictMaterial outMaterial, int outCount,
			long duration) {
		/** The dust-to-dust form ({@code OP.dust.mat(in, n)} → DistW + {@code OP.dust.mat(out, m)}). */
		public static DehydrationRow ofDust(String aNote, OreDictMaterial aInMaterial, int aInCount,
				long aOutAmount, OreDictMaterial aOutMaterial, int aOutCount, long aDuration) {
			return new DehydrationRow(aNote, null, OP.dust, aInMaterial, aInCount, aOutAmount, null, OP.dust, aOutMaterial, aOutCount, aDuration);
		}

		/** The vanilla block-item form, no fluids (BlockDiggable.java:73). */
		public static DehydrationRow ofVanilla(String aNote, Supplier<Item> aIn, Supplier<Item> aOut, long aDuration) {
			return new DehydrationRow(aNote, aIn, null, null, 1, 0, aOut, null, null, 1, aDuration);
		}
	}

	/**
	 * The mineral-dehydration rows (:559-566, verbatim values), the clay→ceramic loop
	 * (:567-568 — the live {@link ANY#Clay} {@code mToThis} walk, one row per family
	 * material) and the BlockDiggable.java:73 vanilla row, in upstream order. Lazily
	 * built per material generation — the loop MUST re-expand when the tables reset (the
	 * family membership rides MT/ANY init).
	 */
	private static volatile List<DehydrationRow> sDehydrationTable = null;

	/** The transcribed dehydration rows, captured on first use (one material generation). */
	public static List<DehydrationRow> dehydrationTable() {
		List<DehydrationRow> tTable = sDehydrationTable;
		if (tTable == null) {
			List<DehydrationRow> tRows = new ArrayList<>();
			// Loader_Recipes_Chem.java:559-566 — the eight mineral-dehydration rows
			tRows.add(DehydrationRow.ofDust(":559", MT.OREMATS.Mirabilite ,  7, 30000, MT.Na2SO4 , 7, 60000));
			tRows.add(DehydrationRow.ofDust(":560", MT.FeO3H3             , 14,  9000, MT.Fe2O3  , 5, 18000));
			tRows.add(DehydrationRow.ofDust(":561", MT.AlO3H3             , 14,  9000, MT.Al2O3  , 5, 18000));
			tRows.add(DehydrationRow.ofDust(":562", MT.H2WO4              ,  7,  3000, MT.WO3    , 4,  6000));
			tRows.add(DehydrationRow.ofDust(":563", MT.OREMATS.Bischofite ,  1,  2000, MT.MgCl2  , 1,  4000));
			tRows.add(DehydrationRow.ofDust(":564", MT.OREMATS.Trona      ,  1,  1000, MT.Na2CO3 , 1,  2000));
			tRows.add(DehydrationRow.ofDust(":565", MT.Gypsum             ,  1,  1000, MT.CaSO4  , 1,  2000));
			tRows.add(DehydrationRow.ofDust(":566", MT.OREMATS.Perlite    ,  1,  1000, MT.Obsidian, 1,  2000));
			// Loader_Recipes_Chem.java:567-568 — the ANY.Clay.mToThis loop, dust x1 → DistW 500 + Ceramic x1
			for (OreDictMaterial tMat : ANY.Clay.mToThis) {
				tRows.add(DehydrationRow.ofDust(":567-568", tMat, 1, 500, MT.Ceramic, 1, 1000));
			}
			// BlockDiggable.java:73 — vanilla clay block → hardened_clay (1.20.1 TERRACOTTA)
			tRows.add(DehydrationRow.ofVanilla(":73", () -> Blocks.CLAY.asItem(), () -> Blocks.TERRACOTTA.asItem(), 64));
			sDehydrationTable = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	/**
	 * The skipped upstream surface of the RM.Drying book OUTSIDE the poured families — the
	 * p19-drying-rows-backfill-2 spec ④ audit, kept as DATA for the audit walk (the
	 * GT6RecipesShCL precedent). Every entry is a POOL item of a named future family, not a
	 * silent drop; the census behind it is tasks.p19-research-drying-rows (the full-file
	 * reads, RM.Drying ≈137 statements). The food/crops/resin BODIES are deliberately not
	 * audited row-by-row here (they need their own census cards) — the entries pin the
	 * dead/pool VERDICTS and their upstream anchors.
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
		"Loader_Recipes_Chem.java:544-545 Tropics_Water + :546-547 OceanGrC + :549-550 Brine + :554-555 Swampwater + :556-557 Stagnant_Water — the FL.exists() external-fluid guard rows (FL.java:124/:126/:131/:129/:128, no GT6 FL.create): CUT = the absent-fluid skip is the guard semantics; a compat-fluids card would carry them",
		"Loader_Recipes_Chem.java:551-552 MT.SaltWater.liquid / MT.SaltedWater.liquid material-liquid rows — the port has no material-liquid registration face (MT.liquid(U, T) creates bucket fluids, not FL shorthand ids); material-liquid pool",
		"Loader_Recipes_Chem.java:530 Water_Hot (\"ic2hotwater\", FL.java:116) — stays UNREGISTERED (ruling: IC2 alias parity, absent-fluid skip; all 8 consumers upstream carry exists() guards; an IC2-compat card must handle all 8, not the Drying row alone)",
		"Loader_Recipes_Food.java:654-658 the food family, exactly 4 rows (Sap/Sap_Maple 250→DistW100+Sugar, Juice_Reed 200→DistW50+Sugar, Juice_Cactus 200→DistW50; :654 guard only gates :655) — inputs need the sap/sap_maple/juice_reed/juice_cactus fluids: food-fluids pool",
		"Loader_Recipes_Crops.java the crop/bale listener family: rice:158/:167, oats:174/:183, abyssalOats:191/:201, barley:208/:217, rye:224/:233 DEAD upstream (no vanilla registrant, ST.valid guard); tobacco/coca/marijuana :381/:385/:389/:393 DEAD (leaf*bud*Dried ST.valid); HaC :742 DEAD (MD.HaC.mLoaded); pomegranate :716 + grapes :746/:751/:756/:761 DEAD (no 1.20.1 grape/pomegranate); baleGrass :121 + itemGrass :138 inputs ARE reachable (tall grass → SHORT_GRASS via itemGrassTall re-reg LoaderOreDictReRegistrations:624; wheat → WHEAT :146-147) but outputs IL.Grass_Dry/Bale_Dry (MultiItemFood.java:53-54) are not ported — crop-bale card pool",
		"Loader_Recipes_OreDict.java:128/:136/:145/:154 the slimeball family (4 listener rows) — outputs ST.make(MD.SC2, \"ItemSlimeRubber\") = Steamcraft2, ST.java:353-354 !mLoaded → null = dead upstream; :190 logWood/logRubber — output BlocksGT.Log1 (BlockTreeLog1, Loader_Woods.java:38) not ported — resin-family pool",
		"Loader_Recipes_Ores.java:49 Sluice fluid 100→DistW50+SluiceSand U9 — needs the sluice fluid (GT6-own, unregistered); :67-68 Biotite crushedPurified/tiny → Ar gas — item legs resolve but the argon gas registration is the gas-family pool",
		"Loader_Recipes_Other.java:321-323 concrete (needs BlocksGT.Concrete/Reinforced), :490 CFoamFresh→CFoam x16 (needs the CFoam face), :672-675 the 32 dye-fluid rows (needs the 16 dye fluids x water/flower classes) — BlocksGT/dye-fluid pools");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation — a bare GT6RecipeMaps.reset()
	// (a dozen unpaired test call sites) must retire the flag WITH the maps, or load() silently
	// early-returns on the "maps cleared × flag set" poison state.
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesDrying::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the GTFluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesDrying::load);
	}

	/** Pours the water family rows and the ice/snow family rows into {@link GT6RecipeMaps#DRYING}. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java:91), tests may race it
		RecipeMap tMap = GT6RecipeMaps.DRYING;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (DryingRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the absent-fluid silent skip (upstream FL.exists drops)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		for (IceRow tRow : iceTable()) {
			Recipe tRecipe = buildIceRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the unresolvable-input silent skip (upstream mat() null drops)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		for (SaltRow tRow : saltTable()) {
			Recipe tRecipe = buildSaltRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the absent-fluid/unresolvable-output silent skip
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		for (DehydrationRow tRow : dehydrationTable()) {
			Recipe tRecipe = buildDehydrationRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the unresolvable-input/output silent skip (upstream mat() null drops)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Drying poured: {} loaded, {} skipped (unregistered fluid ids / unresolvable items, = upstream FL.exists + mat() drops)", tPoured, tSkipped);
	}

	/** Row → Recipe, or null when the fluid fails to resolve (the silent-skip semantics). */
	static Recipe buildRecipe(DryingRow aRow) {
		Fluid tInput = sFluidResolver.apply(aRow.input());
		if (tInput == null) return null;
		Fluid tOutput = sFluidResolver.apply(FLUID_DISTW);
		if (tOutput == null) return null;
		// the upstream addRecipe0(T, 16, 16, FL.in.make(in), FL.DistW.make(out), ZL_IS) shape:
		// buffered, fluid-in + fluid-out, no items, EUt 16 duration 16 verbatim (:525-532)
		return new Recipe(true,
				new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(tInput, (int)aRow.inAmount())},
				new FluidStack[] {new FluidStack(tOutput, (int)aRow.outAmount())},
				16, 16, 0);
	}

	/**
	 * Ice/snow row → Recipe, or null when the input item or the distilled fluid fails to
	 * resolve (the upstream silent-drop semantics). The upstream
	 * {@code addRecipe1(T, 16, dur, input, NF, FL.DistW.make(out), NI)} shape: buffered,
	 * one item input, no fluid inputs, one fluid output, no item outputs, EUt 16.
	 */
	static Recipe buildIceRecipe(IceRow aRow) {
		Item tInput = aRow.vanilla() != null
				? sVanillaItemResolver.apply(aRow.vanilla())
				: sMaterialItemResolver.apply(aRow.prefix(), aRow.material());
		if (tInput == null) return null;
		Fluid tOutput = sFluidResolver.apply(FLUID_DISTW);
		if (tOutput == null) return null;
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInput, aRow.count())}, new ItemStack[0],
				new FluidStack[0],
				new FluidStack[] {new FluidStack(tOutput, (int)aRow.outAmount())},
				aRow.duration(), 16, 0);
	}

	/**
	 * Salt row → Recipe, or null when the input fluid or the output item fails to resolve
	 * (the silent-skip semantics). The upstream
	 * {@code addRecipe0(T, 16, dur, FL.in.make(in), FL.DistW.make(out), item)} shape:
	 * buffered, one fluid input, one fluid output, one item output, no item inputs, EUt 16.
	 */
	static Recipe buildSaltRecipe(SaltRow aRow) {
		Fluid tInput = sFluidResolver.apply(aRow.input());
		if (tInput == null) return null;
		Fluid tOutput = sFluidResolver.apply(FLUID_DISTW);
		if (tOutput == null) return null;
		Item tOutItem = aRow.outVanilla() != null
				? sVanillaItemResolver.apply(aRow.outVanilla())
				: sMaterialItemResolver.apply(aRow.outPrefix(), aRow.outMaterial());
		if (tOutItem == null) return null;
		return new Recipe(true,
				new ItemStack[0], new ItemStack[] {new ItemStack(tOutItem, 1)},
				new FluidStack[] {new FluidStack(tInput, (int)aRow.inAmount())},
				new FluidStack[] {new FluidStack(tOutput, (int)aRow.outAmount())},
				aRow.duration(), 16, 0);
	}

	/**
	 * Dehydration row → Recipe, or null when an input/output item or the distilled fluid
	 * fails to resolve (the upstream silent-drop semantics). The mineral/clay rows are the
	 * upstream {@code addRecipe1(T, 16, dur, dustIn, NF, FL.DistW.make(out), dustOut)}
	 * shape: buffered, one item input, no fluid inputs, one fluid output, one item output,
	 * EUt 16; the BlockDiggable row has no fluid leg at all (NF/NF — outAmount 0 skips the
	 * DistW resolution and the fluid output array stays empty).
	 */
	static Recipe buildDehydrationRecipe(DehydrationRow aRow) {
		Item tInput = aRow.inVanilla() != null
				? sVanillaItemResolver.apply(aRow.inVanilla())
				: sMaterialItemResolver.apply(aRow.inPrefix(), aRow.inMaterial());
		if (tInput == null) return null;
		Item tOutItem = aRow.outVanilla() != null
				? sVanillaItemResolver.apply(aRow.outVanilla())
				: sMaterialItemResolver.apply(aRow.outPrefix(), aRow.outMaterial());
		if (tOutItem == null) return null;
		FluidStack[] tFluidOutputs = new FluidStack[0];
		if (aRow.outAmount() > 0) {
			Fluid tFluid = sFluidResolver.apply(FLUID_DISTW);
			if (tFluid == null) return null;
			tFluidOutputs = new FluidStack[] {new FluidStack(tFluid, (int)aRow.outAmount())};
		}
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInput, aRow.inCount())},
				new ItemStack[] {new ItemStack(tOutItem, aRow.outCount())},
				new FluidStack[0],
				tFluidOutputs,
				aRow.duration(), 16, 0);
	}

	/** The live material-item lookup (GTMaterialItems.get) — null when the pair has no item-path item. */
	@Nullable
	static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The live fluid lookup — vanilla water for the :525 row, the registered distilled
	 * water for the output, the six p16-aqua-fluids registrations for the :526-529/:531-532
	 * rows, the two p19 simple-liquid registrations for the :548/:553 rows (the
	 * RegistryObjects are live at load() time), and NULL for the water_hot alias
	 * (:530 — "ic2hotwater", FL.java:116, unregistered in this port): a null makes the row
	 * skip exactly like the upstream {@code if (FL.Water_Hot.exists())} guard around its
	 * pour line. Fixtures replace the whole function offline (the tests never touch the
	 * RegistryObjects outside a live registry).
	 */
	@Nullable
	static Fluid resolveFluid(String aFluidId) {
		return switch (aFluidId) {
			case FLUID_WATER       -> Fluids.WATER;
			case FLUID_DISTW       -> GTFluids.DISTILLED_WATER.source.get();
			case FLUID_SPDEW       -> GTFluids.SPDEW.source.get();
			case FLUID_MNWTR       -> GTFluids.MNWTR.source.get();
			case FLUID_GEOTHERMAL  -> GTFluids.WATER_GEOTHERMAL.source.get();
			case FLUID_BOILING     -> GTFluids.WATER_BOILING.source.get();
			case FLUID_HOT_WATER   -> GTFluids.HOT_WATER.source.get();
			case FLUID_COLD        -> GTFluids.COLD_WATER.source.get();
			case FLUID_SEAWATER    -> GTFluids.SEAWATER.source.get();
			case FLUID_WATERDIRTY  -> GTFluids.WATERDIRTY.source.get();
			default -> null; // FLUID_HOT (:530) — the absent-fluid skip, the IC2 alias stands unregistered
		};
	}

	/** Test seam: clears the poured flag and the captured tables so a fresh generation can re-pour. */
	static void resetForTest() {sLoaded = false; sIceTable = null; sSaltTable = null; sDehydrationTable = null;}

	private GT6RecipesDrying() {}
}
