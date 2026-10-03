package gregtech6.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The viewer-neutral per-map category metadata + layout math + cost-text formatter of the
 * generic RM recipe-category factory (task debt-jei-emi-batch1, the batch-1 ruling of
 * decisions.2026-09-26-debt-jei-emi-coverage) — the shared seam consumed by BOTH the JEI
 * category ({@link GT6RecipeMapJeiCategory}) and the EMI category
 * (gregtech6.emi.GT6RecipeMapEmiCategory/GT6RecipeMapEmiRecipe), the same cross-package
 * sharing shape as the retired tier-b viewer-text seam (the GT6RecipeViewerText holder,
 * retired task mbpreview-shell-replicate). Strictly vanilla +
 * gregtech6.recipes imports: no JEI, no EMI, no client class — the whole face is
 * offline-testable.
 *
 * <p><b>The porting source (read in full, not guessed):</b> upstream
 * gregapi/NEI_RecipeMap.java, the 718-line single NEI handler that served EVERY
 * mNEIAllowed map — the ctor layout switches (:170-275 item inputs, :282-386 item
 * outputs, :389-390 the fluid rows), the drawExtras cost/tier/time/special text
 * (:680-717), the chance tooltip (:662-666) and the not-consumed tooltip (:671). The
 * per-map columns this table restores are the trailing NEI args of the upstream RM.java /
 * FM.java RecipeMap ctor rows (upstream Recipe.java:106 — aFuelMap,
 * aShowVoltageAmperageInNEI, aNEIAllowed, aConfigAllowed, aNeedsOutputs, aCombinePower,
 * aUseBucketSizeIn, aUseBucketSizeOut) that the port's 15-arg ctor folded away (the
 * declared fold documented on every GT6RecipeMaps field). Only the columns a viewer
 * consumes are restored: aNEIAllowed, aShowVoltageAmperageInNEI, aCombinePower and the
 * special-value triple. Dropped by ruling: aUseBucketSizeIn/Out (upstream
 * container-ized the fluids into buckets for NEI display, :389 FL.display — the modern
 * viewers render FluidStack natively, decisions.2026-09-26-debt-jei-emi-coverage ⑤),
 * aConfigAllowed/aNeedsOutputs (NEI-config faces with no port counterpart).
 *
 * <p><b>The viewer backdrop (task 34-viewer-gui-bg, GitHub #34):</b> upstream
 * drawBackground (:629-635) composited TWO layers under the slots — the grey
 * {@code machines/NEI.png} backdrop plate, then the per-map machine GUI texture as a
 * band — and folded the panel origin (5,11) into every slot coordinate on the way out
 * (:66/:112). The viewers restore exactly that composition, RE-ANCHORED 4px up the
 * texture for the modern viewers' zero-headroom category rects (task
 * viewer-row-headroom — the {@link #S_OFFSET_Y} doc): the crop quadruples
 * {@link #PLATE_CROP}/{@link #BAND_CROP} plus {@link #PLATE_TEXTURE} feed both legs'
 * background draws, and the {@code viewer*Pos} exits below do the
 * sOffset fold ONCE here (the layout switches above stay in machine-GUI coordinates,
 * faithful to the upstream switch). The per-map→PNG mapping needs no table of its own:
 * {@link #guiTexture} reads the live mGUIPath each GT6RecipeMaps row declares (the
 * research card's 72-visible-map transcription lives in state
 * research.r9-34-nei-gui-bg.q2_mapping; anvilbend folds AnvilBendingBig, the five fuel
 * maps share default.png — all already in the rows).
 *
 * <p><b>The exclusion table</b> (r-jei-emi-coverage id927, the research-card pin): the
 * true-zero / special-surface maps never enter a category even though their upstream
 * mNEIAllowed may be true — FURNACE (the port is the vanilla smelting mirror, the vanilla
 * viewer category already shows it), FURNACE_FUEL (the on-demand ForgeHooks burn-time
 * synthesizer, zero static rows ever), CRUCIBLE_SMELTING + CRUCIBLE_ALLOYING (the
 * dynamic material-graph derivation, zero static rows), BUMBLELYZER (the display stock
 * rides sFakeRecipes OUTSIDE mRecipeList) and PLANTALYZER (the Forestry/IC2 compat dead
 * surface). CHISEL and AUTOCRAFTER are excluded by the upstream mNEIAllowed=F itself
 * (RM.java:138/:63) — the faithful form, no ruling needed.
 *
 * <p><b>The visibility ruling (batch 1 canary → batch 2 full opening):</b> batch 1
 * shipped the six canaries of decisions.2026-09-26-debt-jei-emi-coverage ②
 * (COKE_OVEN/SHREDDER/CRUSHER/LATHE/DISTILLERY/DRYING) plus BEDROCK_ORE_LIST
 * (upstream RM.java:153 IS a NEI display map, in the acceptance face); batch 2
 * (task debt-jei-emi-batch2) opens visibility to the WHOLE eligible set — a map is
 * visible exactly when {@link #eligible} says so. The closure stays the exclusion
 * table (6) plus the upstream mNEIAllowed=F rows: 80 census maps → 72 visible.
 *
 * <p><b>The registration-cost ruling (batch 2, the big maps this opens — MIXER's
 * ~56000 rows, MASSFAB's ~4220):</b> structurally linear, no wall clock needed to see
 * it. The JEI leg hands each map's rows to the viewer as ONE defensive copy
 * (GT6JeiPlugin.registerRecipeMapCategoriesRows) — zero per-row work of ours; JEI
 * builds its own index once. The EMI leg additionally sorts the COPY once (O(n log n),
 * the ROW_ORDER key) and allocates one lightweight wrapper per row
 * (gregtech6.emi.GT6RecipeMapEmiRecipe: field assigns + four bounded array scans). No
 * widget exists at registration — slots/text are built only for the row on screen
 * (JEI setRecipe per page render, EMI addWidgets per render). This is the GTCEu Modern
 * shape for the same problem size, read off their source: GTRecipeJEICategory.registerRecipes
 * is {@code List.copyOf} + addRecipes per category (:27-45), GTRecipeEMICategory.
 * registerDisplays the same on the EMI side (:25-45), GTEMIPlugin.java:47-51 the loop —
 * no big-map special-casing there either. The one-time cost that remains lives inside
 * the viewers themselves (JEI's recipe index build, EMI's EmiRecipes.bake over all
 * registered rows) — shared with every mod at viewer init, not ours to amortize.
 * Guard: the wall-clock-free structural assertions in GT6RecipeMapEmiCategoryTest
 * (row-independence of the map scan, copy-not-mutate, deterministic tie-heavy sort at
 * the MIXER scale).
 */
public final class GT6RecipeMapViewerMeta {

	/** NEI handler geometry (NEI_RecipeMap.init NBT: handlerWidth 166): the shared category width. */
	public static final int CATEGORY_WIDTH = 166;
	/** NEI's fixed text band ran to y123 (+9 font) under a 135-tall handler; +5 margin → the shared height. */
	public static final int CATEGORY_HEIGHT = 140;
	/** NEI drawExtras x=10 (NEI_RecipeMap.java:689). */
	public static final int TEXT_X = 10;
	/** NEI's 10px line pitch (73/83/93/103/113/123). */
	public static final int TEXT_LINE_HEIGHT = 10;
	/**
	 * The text-band first Y, panel system (task 34-viewer-gui-bg): upstream drew the
	 * cost lines at FIXED y73..123 in panel coordinates (:680-717) — 3px under the band
	 * bottom edge. Re-anchored (task viewer-row-headroom): 73 + the 4px
	 * {@link #S_OFFSET_Y} shift = 77, still exactly 3px under the band bottom edge
	 * (BAND_CROP v7 + h75 = 82 → category y74), so the text-to-art gap is untouched.
	 * The batch-1 "+10 shift on fluid maps" deviation stayed dead: the fixed band rides
	 * every map, and the FUSION 6-line face (77..127, +9 font = 136) still clears the
	 * 140-high category.
	 */
	public static final int TEXT_BASE_Y = 77;
	/** The per-map title key domain (task issues #29/#34a) — one key per visible map, both locales. */
	public static final String TITLE_KEY_PREFIX = "gt6.jei.recipe_map.";
	/** NEI :671's not-consumed tooltip, upstream verbatim wording — now the lang-key face. */
	public static final String NOT_CONSUMED_KEY = "gt6.jei.cost.not_consumed";
	/** The cost/tier/time/special line keys (task issues #29/#34a) — the drawExtras label faces. */
	public static final String KEY_COSTS = "gt6.jei.cost.costs";
	public static final String KEY_USAGE = "gt6.jei.cost.usage";
	public static final String KEY_TIER = "gt6.jei.cost.tier";
	public static final String KEY_TIER_UNSPECIFIED = "gt6.jei.cost.tier_unspecified";
	public static final String KEY_POWER = "gt6.jei.cost.power";
	public static final String KEY_GAIN = "gt6.jei.cost.gain";
	public static final String KEY_OUTPUT = "gt6.jei.cost.output";
	public static final String KEY_CHANCE = "gt6.jei.cost.chance";
	public static final String KEY_CHANCE_EACH = "gt6.jei.cost.chance_each";
	public static final String KEY_TIME = "gt6.jei.cost.time";
	public static final String KEY_UNIT_TICKS = "gt6.jei.cost.unit_ticks";
	public static final String KEY_UNIT_SECS = "gt6.jei.cost.unit_secs";
	public static final String KEY_UNIT_MINS = "gt6.jei.cost.unit_mins";
	public static final String KEY_START = "gt6.jei.cost.start";
	public static final String KEY_TEMPERATURE = "gt6.jei.cost.temperature";
	/** The unit-suffix faces of the energy-column maps (task #30a, GitHub #30 phase 1):
	 * same label wording as the GU keys with the unit lifted into the second arg, so the
	 * colored short code rides as a styled Component (the en values are the :680-717
	 * literals with " GU" → "%s %s"). Only maps with a pinned carrier use these. */
	public static final String KEY_COSTS_UNIT = "gt6.jei.cost.costs_unit";
	public static final String KEY_USAGE_UNIT = "gt6.jei.cost.usage_unit";
	public static final String KEY_TIER_UNIT = "gt6.jei.cost.tier_unit";
	public static final String KEY_GAIN_UNIT = "gt6.jei.cost.gain_unit";
	public static final String KEY_OUTPUT_UNIT = "gt6.jei.cost.output_unit";

	private GT6RecipeMapViewerMeta() {}

	/**
	 * The upstream trailing-NEI-arg columns a viewer consumes, keyed by the RecipeMap
	 * internal name (the GT6RecipeMaps census key). A pure transcription — {@code null}
	 * special strings are "" and the multiplier folds to 1 exactly like every census row
	 * that carries no special value.
	 */
	public record MapMeta(boolean neiAllowed, boolean showVoltageAmperage, boolean combinePower,
			String specialValuePre, long specialValueMultiplier, String specialValuePost) {
		public static final MapMeta STANDARD = new MapMeta(true, true, false, "", 1, "");
	}

	/** The RM.java:138/:63 + FM.java:38 rows whose upstream aNEIAllowed is F. */
	private static final Set<String> NEI_DISALLOWED = Set.of("gt.recipe.chisel", "gt.recipe.autocrafting", "mc.recipe.furnacefuel");

	/**
	 * The ruled exclusion table (the class doc) — these never enter a category, whatever
	 * their upstream mNEIAllowed column says.
	 */
	private static final Set<String> EXCLUDED = Set.of(
			"mc.recipe.furnace",            // vanilla mirror: the vanilla viewer category is the face
			"mc.recipe.furnacefuel",        // on-demand synthesizer, zero static rows
			"gt.recipe.cruciblesmelting",   // dynamic material-graph derivation, zero static rows
			"gt.recipe.cruciblealloying",   // dynamic alloying display, zero static rows
			"gt.recipe.bumblelyzer",        // display stock rides sFakeRecipes outside mRecipeList
			"gt.recipe.plantalyzer");       // compat dead surface

	/**
	 * The batch-2 visible set = the eligible set (the canary switch of batch 1 fully
	 * open — the class doc). Kept as the plugins' named seam: GT6JeiPlugin and
	 * gregtech6.emi.GT6EmiPlugin both iterate exactly this predicate.
	 */
	public static boolean visibleToViewers(RecipeMap aMap) {
		return eligible(aMap);
	}
	/**
	 * The per-map special deviations from {@link MapMeta#STANDARD} — the census rows whose
	 * upstream tail actually differs. Every census row NOT listed here is STANDARD
	 * ({@code T,T,T,T,F} over the 7-arg form, special {@code "",1,""}): the overwhelming
	 * RM.java shape. The deviations, with their upstream line pins:
	 * <ul>
	 * <li>the five fuel maps carry combinePower=T (FM.java:40/:41/:42/:43/:45 — the
	 *     {@code T,T,T,F,T,F,F} rows);</li>
	 * <li>FUSION carries the special-value triple {@code "Start: ", 1, " LU"}
	 *     (RM.java:146);</li>
	 * <li>the crucible pair carries {@code "Temperature: ", 1, " K"} (RM.java:128/:129 —
	 *     excluded from categories, tabled for census completeness);</li>
	 * <li>NEI_DISALLOWED above carries the mNEIAllowed=F rows.</li>
	 * </ul>
	 */
	private static final Map<String, MapMeta> DEVIATIONS = new HashMap<>();

	private static void deviation(String aName, boolean aNeiAllowed, boolean aShowVoltage, boolean aCombinePower,
			String aPre, long aMultiplier, String aPost) {
		DEVIATIONS.put(aName, new MapMeta(aNeiAllowed, aShowVoltage, aCombinePower, aPre, aMultiplier, aPost));
	}

	static {
		// the mNEIAllowed=F rows (furnacefuel's aShowVoltageAmperageInNEI is T per the
		// FM.java:38 T,F,... row tail — batch 1 had misread it F; dead value anyway, the
		// map is EXCLUDED and never renders)
		deviation("gt.recipe.chisel", false, true, false, "", 1, "");       // RM.java:138
		deviation("gt.recipe.autocrafting", false, true, false, "", 1, ""); // RM.java:63
		deviation("mc.recipe.furnacefuel", false, true, false, "", 1, "");  // FM.java:38
		// the fuel maps: combinePower=T (FM.java:40/:41/:42/:43/:45)
		deviation("gt.recipe.fuels.fluidbed", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.burn", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.gas", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.hot", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.engine", true, true, true, "", 1, "");
		// the special-value rows (RM.java:146/:128/:129)
		deviation("gt.recipe.fusionreactor", true, true, false, "Start: ", 1, " LU");
		deviation("gt.recipe.cruciblealloying", true, true, false, "Temperature: ", 1, " K");
		deviation("gt.recipe.cruciblesmelting", true, true, false, "Temperature: ", 1, " K");
		// every other census row (69 of 75) is STANDARD: neiAllowed=T, showVoltage=T,
		// combinePower=F, no special value — the RM.java:63-153 dominant column shape
	}

	/** The per-map columns; every non-census name reads as STANDARD (the defensive default). */
	public static MapMeta metaOf(RecipeMap aMap) {
		return DEVIATIONS.getOrDefault(aMap.mNameInternal, MapMeta.STANDARD);
	}

	// -----------------------------------------------------------------------
	// The per-map accepted-energy column (task #30a, GitHub #30 phase 1) — the
	// upstream "Costs: n GU" face splits into the concrete carrier short code where the
	// registration rows are unambiguous, and stays GU where they are not (GU's very
	// semantics: Recipe.mEUt is energy-type-agnostic, upstream NEI_RecipeMap.java:688-710
	// writes the generic unit for every map).
	//
	// Transcription source (the same discipline as DEVIATIONS above): the machine
	// registration rows that carry a RecipeMap AND its accepted-energy carrier —
	// GTMachines.java single-block families, GT6LargeMachines.java LargeMachineRow rows
	// (upstream Loader :1229-1240), GT6Distillation.java TowerRows and the single-carrier
	// special TileEntities. The port's p28 ULV ladder rows (SHREDDER/CRUSHER/SIFTING/
	// WIREMILL/ROLLINGMILL on EU) are a declared port-side tier extension ("NO upstream
	// VN[0] machine exists", GTMachines.java ULV header) and do not enter this upstream
	// transcription. Absent name = the GU face.
	// -----------------------------------------------------------------------

	/**
	 * The per-map carrier, keyed by the internal name (the census key). A map lands here
	 * only when EVERY registration row carrying it agrees on one TagData; the mixed set
	 * is the #30a conflict list, reported on the card and left on GU:
	 * gt.recipe.crusher (KU small GTMachines:438-440 vs RU large GT6LargeMachines:1238),
	 * gt.recipe.squeezer (KU :2940 vs RU :1240), gt.recipe.sifter (KU :949 vs EU
	 * electricsifter :1689), gt.recipe.mixer (RU :1631/:1234 vs EU electricmixer :1652)
	 * and gt.recipe.loom (RU :4280 vs EU electricloom :1673) — every pair upstream-faithful,
	 * which is exactly why upstream printed GU.
	 */
	private static final Map<String, TagData> ENERGY_BY_MAP = new HashMap<>();

	private static void energy(String aName, TagData aType) {
		ENERGY_BY_MAP.put(aName, aType);
	}

	static {
		// EU (GTMachines single-block rows, all Electric_T families)
		energy("gt.recipe.boxinator", TD.Energy.EU);            // :1709
		energy("gt.recipe.canner", TD.Energy.EU);               // :644
		energy("gt.recipe.electrolyzer", TD.Energy.EU);         // :3592 (large :1230 same)
		energy("gt.recipe.injector", TD.Energy.EU);             // :3611
		energy("gt.recipe.lightning", TD.Energy.EU);            // :3289
		energy("gt.recipe.printer", TD.Energy.EU);              // :3631
		energy("gt.recipe.scannervisuals", TD.Energy.EU);       // :3650
		energy("gt.recipe.slicer", TD.Energy.EU);               // :3669
		energy("gt.recipe.unboxinator", TD.Energy.EU);          // :1724
		// RU (Kinetic_T families + the large rows that agree)
		energy("gt.recipe.burnmixer", TD.Energy.RU);            // :5256
		energy("gt.recipe.centrifuge", TD.Energy.RU);           // :2954 (large :1229 same)
		energy("gt.recipe.clustermill", TD.Energy.RU);          // :1363-1366
		energy("gt.recipe.cutter", TD.Energy.RU);               // :2926 buzzsaw
		energy("gt.recipe.lathe", TD.Energy.RU);                // :448-449
		energy("gt.recipe.pressurewasher", TD.Energy.RU);       // :2996 debarker
		energy("gt.recipe.rollbender", TD.Energy.RU);           // :1349-1352
		energy("gt.recipe.rollformer", TD.Energy.RU);           // :1356-1359
		energy("gt.recipe.rollingmill", TD.Energy.RU);          // :1342-1345
		energy("gt.recipe.sharpener", TD.Energy.RU);            // :2982 sander
		energy("gt.recipe.shredder", TD.Energy.RU);             // :425-426 (large :1239 same)
		energy("gt.recipe.sluice", TD.Energy.RU);               // :2968 (large :1237 same)
		energy("gt.recipe.wiremill", TD.Energy.RU);             // :977
		// KU (Kinetic_T families without a disagreeing large row)
		energy("gt.recipe.compressor", TD.Energy.KU);           // :963
		energy("gt.recipe.press", TD.Energy.KU);                // :716
		// HU (Heat_T families + the distillation tower row)
		energy("gt.recipe.distillationtower", TD.Energy.HU);    // GT6Distillation:148
		energy("gt.recipe.distillery", TD.Energy.HU);           // :2631
		energy("gt.recipe.drying", TD.Energy.HU);               // :514
		energy("gt.recipe.extruder", TD.Energy.HU);             // :803
		energy("gt.recipe.fermenter", TD.Energy.HU);            // :1745 (large :1235 same)
		energy("gt.recipe.laminator", TD.Energy.HU);            // :3318
		energy("gt.recipe.melter", TD.Energy.HU);               // :4572
		energy("gt.recipe.roaster", TD.Energy.HU);              // :4994
		energy("gt.recipe.smelter", TD.Energy.HU);              // :4567
		energy("gt.recipe.catalyticcracking", TD.Energy.HU);    // Loader:1570-1573, the port's crackerRow HU (GTMachines:4139-4143+:4172)
		energy("gt.recipe.steamcracking", TD.Energy.HU);        // Loader:1576-1579, the port's crackerRow HU (GTMachines:4132-4136+:4172)
		// CU (the cryo families + the cryo tower row)
		energy("gt.recipe.cryodistillationtower", TD.Energy.CU); // GT6Distillation:150
		energy("gt.recipe.cryomixer", TD.Energy.CU);            // :2210
		energy("gt.recipe.freezer", TD.Energy.CU);              // :2195
		energy("gt.recipe.crystallisationcrucible", TD.Energy.HU); // :5113 (a VISIBLE map — not in the exclusion table)
		// LU (the laser families)
		energy("gt.recipe.laserengraver", TD.Energy.LU);        // :2165
		energy("gt.recipe.welder", TD.Energy.LU);               // :2180 laserwelder
		// MU (the magnetic families)
		energy("gt.recipe.magneticseparator", TD.Energy.MU);    // :2150
		energy("gt.recipe.polarizer", TD.Energy.MU);            // :2132
		// QU (the quantum families)
		energy("gt.recipe.massfab", TD.Energy.QU);              // :2270 (multiblock TileEntityMassfab:121 same)
		energy("gt.recipe.replicator", TD.Energy.QU);           // :2365
		energy("gt.recipe.scannermolecular", TD.Energy.QU);     // :2350
		// TU (the time-carrier families + the fusion reactor's accepted face)
		energy("gt.recipe.autoclave", TD.Energy.TU);            // :4262 (large :1232 same)
		energy("gt.recipe.bath", TD.Energy.TU);                 // :4243 (large :1233 same)
		energy("gt.recipe.coagulator", TD.Energy.TU);           // :4205 (large :1231 same)
		energy("gt.recipe.fusionreactor", TD.Energy.TU);        // TileEntityFusionReactor:142
		energy("gt.recipe.generifier", TD.Energy.TU);           // :4224
	}

	/** The per-map accepted-energy carrier; {@code null} = the upstream GU face (mixed or carrier-less). */
	public static TagData energyOf(RecipeMap aMap) {
		return ENERGY_BY_MAP.get(aMap.mNameInternal);
	}

	/**
	 * The carrier colors — the upstream TD.Energy createTagData 4th argument (TD.java:81-144)
	 * through the LH.Chat constants (LH.java:658+, each is the same-named
	 * EnumChatFormatting). The port's TD.java:39 strips that presentation-only parameter,
	 * so the consumer side carries it; only the nine carriers the registration rows use.
	 */
	private static final Map<TagData, ChatFormatting> ENERGY_COLORS = Map.of(
			TD.Energy.EU, ChatFormatting.BLUE,          // TD.java:81 LH.Chat.BLUE
			TD.Energy.RU, ChatFormatting.GREEN,         // :88 GREEN
			TD.Energy.KU, ChatFormatting.DARK_GREEN,    // :95 DGREEN
			TD.Energy.HU, ChatFormatting.RED,           // :102 RED
			TD.Energy.CU, ChatFormatting.AQUA,          // :109 CYAN (= AQUA, LH.java:680)
			TD.Energy.LU, ChatFormatting.YELLOW,        // :116 YELLOW
			TD.Energy.MU, ChatFormatting.DARK_GRAY,     // :123 DGRAY
			TD.Energy.QU, ChatFormatting.DARK_PURPLE,   // :137 PURPLE (= DARK_PURPLE, :669)
			TD.Energy.TU, ChatFormatting.DARK_BLUE);    // :144 DBLUE

	// -----------------------------------------------------------------------
	// The energy-carrier short codes (task machine-energy-display-fix, moved HERE in
	// task debt-viewer-polish). History: energyUnit used to call the Jade integration
	// class gregtech6.jade.GT6MachineProvider, whose interfaces (snownee.jade.api.*) are
	// compileOnly — on a no-Jade runtime classpath the first recipe-page draw died in
	// NoClassDefFoundError (crash-2026-09-30_01.24.41-client.txt, known_bugs
	// r934_nojade_recipe_page_draw_ncdfe). The dependency is now inverted: the pure
	// TagData→String table lives in this Jade-free seam and the Jade provider references
	// IT — the viewer path never touches a Jade class whatever the runtime carries.
	// -----------------------------------------------------------------------

	/**
	 * 能量类型短码表（task machine-energy-display-fix，原 gregtech6.jade.GT6MachineProvider
	 * 侧表 verbatim 移入）：accepted-energy 载体在 {@code mEnergyTypeAccepted}
	 * （TileEntityBasicMachine.java:254），但 {@link TagData#mName} 不是显示名——移植把名字
	 * 全大写折叠、丢弃 LH 短/长本地名（root TagData.java:68-71 createTagData 丢
	 * aLocalShort/aLocalLong，mName="ENERGY.RU" :88），短码按研究卡裁定落本映射（root 不动）。
	 * 恒等查找安全：createTagData 按名去重（TagData.java:77-81），TD.Energy 常量即单例。
	 * 短码值 = 上游 aLocalShort 字面 verbatim（root TD.java:81/:88/:95/:102/:109/:116/:123/
	 * :130/:137/:144/:151/:158/:165/:172/:175-185——被丢弃的实参仍原样在盘）。
	 */
	private static final Map<TagData, String> ENERGY_SHORT_CODES = Map.ofEntries(
			Map.entry(TD.Energy.EU, "EU"),           // TD.java:81 ELECTRICITY（Canner 电机族）
			Map.entry(TD.Energy.RU, "RU"),           // :88 KINETIC_ROTATION（Shredder/Lathe/Wiremill）
			Map.entry(TD.Energy.KU, "KU"),           // :95 KINETIC_PUSH（Crusher/Sifter/Compressor/Press）
			Map.entry(TD.Energy.HU, "HU"),           // :102 HEAT（Oven/Dryer/Extruder/Distillery）
			Map.entry(TD.Energy.CU, "CU"),           // :109 CRYO
			Map.entry(TD.Energy.LU, "LU"),           // :116 LIGHT
			Map.entry(TD.Energy.MU, "MU"),           // :123 MAGNETIC
			Map.entry(TD.Energy.NU, "NU"),           // :130 NEUTRON
			Map.entry(TD.Energy.QU, "QU"),           // :137 QUANTUM
			Map.entry(TD.Energy.TU, "TU"),           // :144 TIME（:254 字段默认）
			Map.entry(TD.Energy.RF, "RF"),           // :151 REDSTONE_FLUX
			Map.entry(TD.Energy.MJ, "MJ"),           // :158 MINECRAFT_JOULES
			Map.entry(TD.Energy.STEAM, "Steam"),     // :165（上游短名是词不是字头）
			Map.entry(TD.Energy.AU, "AU"),           // :172 AIR
			Map.entry(TD.Energy.VIS_ORDO, "Ordo"),       // :175
			Map.entry(TD.Energy.VIS_AER, "Aer"),         // :177
			Map.entry(TD.Energy.VIS_AQUA, "Aqua"),       // :179
			Map.entry(TD.Energy.VIS_TERRA, "Terra"),     // :181
			Map.entry(TD.Energy.VIS_IGNIS, "Ignis"),     // :183
			Map.entry(TD.Energy.VIS_PERDITIO, "Perditio")); // :185

	/**
	 * 能量载体的显示短码（{@link #ENERGY_SHORT_CODES} 查找；回退 = 折叠 mName 剥 "ENERGY."
	 * 前缀——大写、无参数（TagData.java:69-71 折叠语义），兜住映射未及的未来载体。
	 * The one short-code home: the Jade provider (GT6MachineProvider) and the viewer
	 * cost lines both read through here.
	 */
	public static String energyTypeShortCode(TagData aType) {
		String tCode = aType == null ? null : ENERGY_SHORT_CODES.get(aType);
		if (tCode != null) return tCode;
		String tName = aType == null ? "" : aType.mName;
		return tName.startsWith("ENERGY.") ? tName.substring("ENERGY.".length()) : tName;
	}

	/**
	 * The colored short-code unit component: the short code from the Jade-free table above
	 * and the color the upstream LH.Chat transcription. Both viewers eat the style — JEI
	 * draws via GuiGraphics.drawString (vanilla Font applies the per-run style color) and
	 * EMI's addText goes through Text.asOrderedText (style runs preserved).
	 */
	public static Component energyUnit(TagData aEnergy) {
		return Component.literal(energyTypeShortCode(aEnergy))
				.withStyle(ENERGY_COLORS.getOrDefault(aEnergy, ChatFormatting.WHITE));
	}

	/** Category-eligible: tabled census map, upstream mNEIAllowed, not ruled out. */
	public static boolean eligible(RecipeMap aMap) {
		return !NEI_DISALLOWED.contains(aMap.mNameInternal) && !EXCLUDED.contains(aMap.mNameInternal);
	}

	/** The visible maps in deterministic (name-sorted) registration order. */
	public static List<RecipeMap> visibleMaps() {
		List<RecipeMap> rMaps = new ArrayList<>();
		Set<String> tSorted = new TreeSet<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) if (visibleToViewers(tMap)) tSorted.add(tMap.mNameInternal);
		for (String tName : tSorted) rMaps.add(RecipeMap.RECIPE_MAPS.get(tName));
		return rMaps;
	}

	/**
	 * The per-map category title lang key (task issues #29/#34a, GitHub #29b): the internal name
	 * minus its {@code gt.recipe.}/{@code mc.recipe.} prefix, dots folded to underscores —
	 * {@code gt.recipe.cokeoven} → {@code gt6.jei.recipe_map.cokeoven},
	 * {@code gt.recipe.anvil.bend} → {@code gt6.jei.recipe_map.anvil_bend}. One formula,
	 * the datagen providers (GT6EnUs/GT6ZhCn) and both viewer legs consume it, so key
	 * drift between producer and consumers is structurally impossible.
	 */
	public static String titleKey(RecipeMap aMap) {
		String tName = aMap.mNameInternal;
		String tTail = tName.startsWith("gt.recipe.") || tName.startsWith("mc.recipe.") ? tName.substring(10) : tName;
		return TITLE_KEY_PREFIX + tTail.replace('.', '_');
	}

	// -----------------------------------------------------------------------
	// The layout math — NEI_RecipeMap.CachedDefaultRecipe ctor switches, translated.
	// Item slots: a 3-column 18px grid anchored at x17 (inputs) / x107 (outputs); the row
	// count and the row Ys depend on the declared slot counts exactly like the upstream
	// switch (1-3 = one row at y7-or-25 by the fluid threshold >6; 4-6 = two rows whose
	// Ys shift by the fluid threshold >3; 7+ = the fixed 3x3 at y7/25/43, then at most a
	// three-slot fourth 61-row whose anchoring hugs the grid's LAST column on BOTH sides,
	// and no slot past the 12th is ever drawn — the switch has no case rendering one).
	// Fluids: the NEI :389-390 bottom rows.
	// -----------------------------------------------------------------------

	/**
	 * Item-input slot {x,y} (the NEI :170-275 switch). {@code null} = no slot at this
	 * index (count 0, past the declared count, or past the 12th drawn slot).
	 */
	public static int[] inputPos(int aIndex, RecipeMap aMap) {
		return itemPos(aIndex, aMap.mInputItemsCount, aMap.mInputFluidCount, 17, true);
	}

	/** Item-output slot {x,y} (the NEI :282-386 switch, mirrored anchor 107 — LEFT-anchored). */
	public static int[] outputPos(int aIndex, RecipeMap aMap) {
		return itemPos(aIndex, aMap.mOutputItemsCount, aMap.mOutputFluidCount, 107, false);
	}

	/**
	 * The switch translation. One asymmetry the upstream switch encodes and the tests
	 * pin: the INPUT rows are RIGHT-anchored (1 in at x53, 2 at 35/53, the 4-slot shape
	 * the {35,53} pair) while the OUTPUT rows are LEFT-anchored (1 out at x107, 2 at
	 * 107/125, the 4-slot shape the {107,125} pair) — the two sides grow away from the
	 * progress arrow in the middle.
	 */
	private static int[] itemPos(int aIndex, int aItemCount, int aFluidCount, int aAnchorX, boolean aRightAnchored) {
		if (aIndex < 0 || aIndex >= aItemCount) return null;
		if (aItemCount <= 3) {
			int tCol = aRightAnchored ? aIndex + 3 - aItemCount : aIndex;
			return new int[] {aAnchorX + 18 * tCol, aFluidCount > 6 ? 7 : 25};
		}
		if (aItemCount <= 6) {
			return new int[] {aAnchorX + 18 * twoRowCol(aIndex, aItemCount, aRightAnchored), twoRowY(aIndex < 3 ? 0 : 1, aFluidCount)};
		}
		// 7+ → the fixed 3x3 at y7/25/43, then the upstream fourth 61-row: at most three
		// slots, RIGHT-hugging on BOTH grids — upstream case 10 hangs its 10th slot on the
		// grid's LAST column (inputs x53, NEI_RecipeMap.java:246; outputs x143, :358), case
		// 11 fills {last-1, last} (:258-259 / :370-371), the default fills the full row.
		// And the switch draws at most 12 slots per side (no case renders a 13th) — the
		// batch-2 ruling pins this whole tail DEAD on the live census (no map declares
		// 10/11/13+ item slots either side), so it exists purely as the faithful
		// transcription the day a map outgrows the census.
		if (aIndex >= 12) return null;
		if (aIndex < 9) return new int[] {aAnchorX + 18 * (aIndex % 3), 7 + 18 * (aIndex / 3)};
		return new int[] {aAnchorX + 18 * (aIndex - 9 + Math.max(0, 12 - aItemCount)), 61};
	}

	/** The 4-6-slot two-row column shapes: 4 = {1,2}|{0,1}, 5 = {0,1,2}/{1,2}|{0,1,2}/{0,1}, 6 = full. */
	private static int twoRowCol(int aIndex, int aItemCount, boolean aRightAnchored) {
		int tLead = aRightAnchored ? 1 : 0;
		if (aItemCount == 4) return tLead + aIndex % 2;
		if (aItemCount == 5) return aIndex < 3 ? aIndex : tLead + (aIndex - 3);
		return aIndex % 3; // 6
	}

	/** The 4-6-slot row Ys: >3 fluids → {7,25}, else {16,34} (the NEI ternaries). */
	private static int twoRowY(int aRowIndex, int aFluidCount) {
		if (aFluidCount > 3) return aRowIndex == 0 ? 7 : 25;
		return aRowIndex == 0 ? 16 : 34;
	}

	/** Fluid-input slot {x,y} — NEI :389: {@code 53 - (i%3)*18, 63 - (i/3)*18}. */
	public static int[] fluidInputPos(int aIndex) {
		return new int[] {53 - aIndex % 3 * 18, 63 - aIndex / 3 * 18};
	}

	/** Fluid-output slot {x,y} — NEI :390: {@code 107 + (i%3)*18, 63 - (i/3)*18}. */
	public static int[] fluidOutputPos(int aIndex) {
		return new int[] {107 + aIndex % 3 * 18, 63 - aIndex / 3 * 18};
	}

	// -----------------------------------------------------------------------
	// The viewer backdrop geometry (task 34-viewer-gui-bg, GitHub #34) — the
	// two-layer composite upstream NEI_RecipeMap.drawBackground(:629-635) drew and the
	// port's viewers shipped without (items floated on the raw category grey, read as
	// "misaligned" though every coordinate was faithful). Layer 1: the grey backdrop
	// plate gt6:textures/gui/machines/nei.png. Layer 2: the per-map machine GUI texture
	// (mGUIPath — upstream getGuiTexture :653, drawn as the (-5,-8, 0,3,176,79) band).
	// Both layers anchor the PANEL origin at texture pixel (5,7) of the machine band /
	// (5,12) of the plate — the offset upstream folded into every PositionedStack (the
	// ctor super call at :112 over :66 sOffsetX/Y=5/11), re-anchored 4px for the modern
	// viewers' zero-headroom category rects (task viewer-row-headroom, the S_OFFSET_Y
	// doc) — the layout switches above stay in machine-GUI coordinates and the viewer*
	// exits below do the fold ONCE here, so neither viewer leg ever folds twice.
	// -----------------------------------------------------------------------

	/**
	 * The panel origin inside the machine texture — NEI_RecipeMap.java:66's
	 * {@code sOffsetX=5} verbatim, and {@code sOffsetY} RE-ANCHORED 11 → 7 (task
	 * viewer-row-headroom). Upstream's 11 put the machine band 8px above the panel
	 * origin — the band drew from panel −8 ({@code drawTexturedModalRect(-5,-8,...)}
	 * :634) and the topmost slot row sat at panel −4 — headroom NEI's unclipped handler
	 * padding provided for free. The modern viewers give the category rect ZERO room
	 * above its origin (JEI drops the layout directly under its title/page chrome and
	 * runs the layout's input identity off that rect — RecipeLayout.isMouseOver is
	 * {@code area.contains}; EMI pins the group origin right under its pagination bars),
	 * so a negative slot row lands on viewer chrome, outside the managed rect — the
	 * "首行被裁剪" report. The origin therefore re-anchors 4px up the texture (11−4):
	 * every viewer y translates +4 (the upstream composition had 8px of headroom, 4 of
	 * which the topmost slot row consumed — re-anchoring by that 4 makes it land at y0
	 * exactly), the relative geometry (slot↔band art, text gap, gear spot) is
	 * pixel-identical, and the raw {@code inputPos}/{@code outputPos} exits stay in
	 * machine-GUI coordinates, faithful to the upstream switch.
	 */
	public static final int S_OFFSET_X = 5;
	public static final int S_OFFSET_Y = 7;
	/**
	 * The backdrop plate crop, {u,v,w,h} panel system (NEI_RecipeMap.java:632
	 * {@code drawTexturedModalRect(-5,-16,0,0,176,166)}). Upstream mapped panel y to
	 * texture row − 16; re-anchored (the same +4 translation as {@link #S_OFFSET_Y},
	 * the plate origin riding 5px below the band origin in texture space: 16−12 = 11−7).
	 * Drawn at (0,0); h stays 140 = the full category height — the rows the shifted
	 * window drops at its bottom are the plate's transparent margin, and the 4 rows it
	 * gains at its top sit under the band, which covers the full 166-wide strip.
	 */
	public static final int[] PLATE_CROP = {5, 12, 166, 140};
	/**
	 * The machine-band crop, {u,v,w,h} panel system. Upstream drew
	 * {@code (-5,-8, 0,3,176,79)} = texture rows 3..81 with panel y = row − 11
	 * (NEI_RecipeMap.java:634); the re-anchored origin maps panel y = row − 7
	 * ({@link #S_OFFSET_Y}), so v slides to 7 and h extends 71 → 75 to keep the SAME
	 * bottom edge (v+h = 82: the band still ends on texture row 81 — the SHREDDER
	 * fourth-row slot holes at rows 61..78 stay fully covered). The art the shifted
	 * window newly shows at its top is exactly the strip upstream drew at panel
	 * −4..−1; the 4px above it (rows 3..6, upstream panel −8..−5) stays cropped — the
	 * one sliver of the upstream composition no fixed-origin viewer can show. Drawn at
	 * (0,0) OVER the plate.
	 */
	public static final int[] BAND_CROP = {5, 7, 166, 75};
	/** The backdrop plate texture (NEI_RecipeMap.java:632; port assets/README.md #34 section, amazawa redraw). */
	public static final ResourceLocation PLATE_TEXTURE = ResourceLocation.fromNamespaceAndPath("gt6", "textures/gui/machines/nei.png");

	/**
	 * The per-map machine GUI texture (upstream getGuiTexture :653 = mGUIPath, the
	 * Recipe.java:124 ".png"-suffixed string — the live per-map→PNG mapping table, no
	 * second table needed) as a texture {@link ResourceLocation}. The five fuel maps and
	 * the like ride the shared default.png exactly as their GT6RecipeMaps rows declare.
	 * Same parse as GTBasicMachineScreen.backgroundOf (the machine-Screen consumer of the
	 * very same string).
	 */
	public static ResourceLocation guiTexture(RecipeMap aMap) {
		String tPath = aMap.mGUIPath;
		int tColon = tPath.indexOf(':');
		if (tColon < 0) throw new IllegalArgumentException("RecipeMap mGUIPath is not a namespaced path: " + tPath);
		return ResourceLocation.fromNamespaceAndPath(tPath.substring(0, tColon), tPath.substring(tColon + 1));
	}

	/** The fold: machine-GUI coordinates → panel/viewer coordinates ({@code null} passes through) — the re-anchored -(5,7). */
	private static int[] fold(int[] aPos) {
		return aPos == null ? null : new int[] {aPos[0] - S_OFFSET_X, aPos[1] - S_OFFSET_Y};
	}
	
	// -----------------------------------------------------------------------
	// The gear-spot jump port (task viewer-energy-jump-gear, the user ruling:
	// "跳转对应能量怎么产的页面也应该做，跳转口可以做在之间的齿轮图标") — the baked gear
	// decoration on the NEI plate becomes the clickable entrance onto the map's accepted
	// energy carrier's info page. Both viewer legs consume the same fold and the same
	// carrier query; a GU map (energyOf null) draws no port — the gear stays decoration.
	// -----------------------------------------------------------------------
	
	/**
	 * The baked gear-art spot in machine-GUI coordinates — upstream NEI_RecipeMap.java:278
	 * drew its rare machine icon at (152,83), the same pixel the plate's gear decoration
	 * bakes into (the debt-viewer-polish icon rode exactly here pre-retirement).
	 */
	public static final int[] GEAR_POS_GUI = {152, 83};
	
	/** The port's hit box — the 18px slot pitch, the art's gear face. */
	public static final int GEAR_SIZE = 18;
	
	/** {@link #GEAR_POS_GUI} folded into panel/viewer coordinates — (147,76) under the re-anchored -(5,7). */
	public static int[] viewerGearPos() {
		return fold(GEAR_POS_GUI);
	}
	
	/**
	 * The per-carrier info-page lang key domain (task viewer-energy-jump-gear): the
	 * {@code gt6.jei.info.*} viewer-neutral family (the coke-oven/ore-gen precedent) with
	 * the short code as the tail — {@code gt6.jei.info.energy.eu}. The datagen en
	 * producer, the zh TSV hand band and both viewer legs derive the key through this one
	 * formula, so the census (pinned in GT6EnergyJumpTest) cannot drift.
	 */
	public static final String INFO_KEY_ENERGY_PREFIX = "gt6.jei.info.energy.";
	
	/** The gear port's affordance tooltip (both legs) — the one line that makes the baked art discoverable as a button. */
	public static final String ENERGY_JUMP_HINT_KEY = "gt6.jei.info.energy_jump_hint";
	
	/** The info-page key of one carrier: the short code lowercased (Locale.ROOT) — {@code gt6.jei.info.energy.eu}. */
	public static String energyInfoKey(TagData aCarrier) {
		return INFO_KEY_ENERGY_PREFIX + energyTypeShortCode(aCarrier).toLowerCase(Locale.ROOT);
	}
	
	/**
	 * The carrier census, derived — every TagData the {@link #ENERGY_BY_MAP} transcription
	 * pins (dedup via the TagData singleton contract), short-code-sorted for determinism:
	 * the nine EU/RU/KU/HU/CU/LU/MU/QU/TU faces. The single source of truth for the jump
	 * faces' info-page registration AND the key-existence census — a carrier entering the
	 * table without its lang rows turns the census red, never the page.
	 */
	public static List<TagData> pinnedEnergyCarriers() {
		return ENERGY_BY_MAP.values().stream().distinct()
				.sorted(java.util.Comparator.comparing(GT6RecipeMapViewerMeta::energyTypeShortCode))
				.toList();
	}
	
	/**
	 * The carrier long names — the upstream TD.Energy createTagData 3rd argument verbatim
	 * (TD.java:81/:88/:95/:102/:109/:116/:123/:137/:147 "Electric Energy"…"Time"), the
	 * port's TD.java:39 stripped it as presentation data; only the nine carriers the
	 * registration rows use. The en info-page body's opening face (the GT6EnUs producer
	 * appends the units sentence; TU's is "Ticks" per the upstream "Amount = Ticks" doc).
	 */
	private static final Map<TagData, String> ENERGY_LONG_NAMES = Map.of(
			TD.Energy.EU, "Electric Energy",
			TD.Energy.RU, "Rotation Energy",
			TD.Energy.KU, "Kinetic Energy",
			TD.Energy.HU, "Heat Energy",
			TD.Energy.CU, "Cryo Energy",
			TD.Energy.LU, "Light Energy",
			TD.Energy.MU, "Magnetic Energy",
			TD.Energy.QU, "Quantum Energy",
			TD.Energy.TU, "Time");
	
	/** The upstream long name of one carrier ({@code null} = not one of the nine). */
	public static String energyLongName(TagData aCarrier) {
		return ENERGY_LONG_NAMES.get(aCarrier);
	}

	/** {@link #inputPos} folded into panel/viewer coordinates — the only form the viewer legs consume. */
	public static int[] viewerInputPos(int aIndex, RecipeMap aMap) {
		return fold(inputPos(aIndex, aMap));
	}

	/** {@link #outputPos} folded into panel/viewer coordinates — the only form the viewer legs consume. */
	public static int[] viewerOutputPos(int aIndex, RecipeMap aMap) {
		return fold(outputPos(aIndex, aMap));
	}

	/** {@link #fluidInputPos} folded into panel/viewer coordinates — the only form the viewer legs consume. */
	public static int[] viewerFluidInputPos(int aIndex) {
		return fold(fluidInputPos(aIndex));
	}

	/** {@link #fluidOutputPos} folded into panel/viewer coordinates — the only form the viewer legs consume. */
	public static int[] viewerFluidOutputPos(int aIndex) {
		return fold(fluidOutputPos(aIndex));
	}

	// -----------------------------------------------------------------------
	// The gear-spot machine-icon exits were RETIRED (task viewer-icon-retire-gu-pin, the
	// user ruling): EMI renders the workstation list itself (RecipeScreen.java:203-217)
	// and JEI renders the catalyst column itself (RecipesGui.java:635-636 →
	// RecipeCatalysts) — both fed by GT6EmiPlugin:157/GT6JeiPlugin:164-167 off the
	// GT6RecipeMapIcons table, which stays the data face (category tab icons + catalysts).
	// Upstream NEI_RecipeMap.java:278 only drew for the rare non-empty
	// mRecipeMachineList, so the per-map gear-spot draw (debt-viewer-polish's #34 defer)
	// was an over-generalization — GUI_MACHINE_ICON_POS/machineIconPos/machineIcon are
	// gone, zero production consumers remain.
	//
	// The SPOT itself returns (task viewer-energy-jump-gear, the user ruling) — no longer
	// a machine icon, but the CLICKABLE jump port onto the per-carrier "how is this
	// energy produced" info page: GEAR_POS_GUI/viewerGearPos below. Carrier-less (GU)
	// maps keep the gear as pure decoration — both viewer legs skip the port when
	// energyOf is null.
	//
	// The cost/tier/time/special text — NEI_RecipeMap.drawExtras :680-717, verbatim
	// arithmetic (UT.Code.makeString folds to plain long-to-string).
	// -----------------------------------------------------------------------

	/**
	 * The drawExtras line list, top-down (the viewers draw them at TEXT_LINE_HEIGHT pitch).
	 * Since task issues #29/#34a the label faces are translatable components with the verbatim
	 * upstream numbers as args — the en values are the :680-717 literals to the character.
	 * Since task #30a a map with a pinned carrier (the ENERGY_BY_MAP column) prints its
	 * unit-suffix key with the colored short code as the second arg; carrier-less and
	 * mixed-carrier maps keep the GU keys byte-identical.
	 */
	public static List<Component> costLines(RecipeMap aMap, Recipe aRecipe) {
		List<Component> rLines = new ArrayList<>();
		MapMeta tMeta = metaOf(aMap);
		TagData tEnergy = energyOf(aMap);
		long tGUt = aRecipe.mEUt;
		long tDuration = aRecipe.mDuration;
		if (tGUt == 0) {
			if (tMeta.showVoltageAmperage()) rLines.add(Component.translatable(KEY_TIER_UNSPECIFIED));
		} else if (tGUt > 0) {
			rLines.add(unitLine(KEY_COSTS, KEY_COSTS_UNIT, tGUt * tDuration, tEnergy));
			if (tMeta.showVoltageAmperage()) {
				if (!tMeta.combinePower()) rLines.add(unitLine(KEY_USAGE, KEY_USAGE_UNIT, tGUt, tEnergy));
				rLines.add(unitLine(KEY_TIER, KEY_TIER_UNIT, tGUt / aMap.mPower, tEnergy));
				rLines.add(Component.translatable(KEY_POWER, aMap.mPower));
			} else if (tGUt != 1 && !tMeta.combinePower()) {
				rLines.add(unitLine(KEY_USAGE, KEY_USAGE_UNIT, tGUt, tEnergy));
			}
		} else {
			tGUt *= -1;
			rLines.add(unitLine(KEY_GAIN, KEY_GAIN_UNIT, tGUt * tDuration, tEnergy));
			if (tMeta.showVoltageAmperage()) {
				if (!tMeta.combinePower()) rLines.add(unitLine(KEY_OUTPUT, KEY_OUTPUT_UNIT, tGUt, tEnergy));
				rLines.add(unitLine(KEY_TIER, KEY_TIER_UNIT, tGUt / aMap.mPower, tEnergy));
				rLines.add(Component.translatable(KEY_POWER, aMap.mPower));
			} else if (tGUt != 1 && !tMeta.combinePower()) {
				rLines.add(unitLine(KEY_OUTPUT, KEY_OUTPUT_UNIT, tGUt, tEnergy));
			}
		}
		if (tDuration > 0) rLines.add(timeLine(tDuration));
		if (!tMeta.specialValuePre().isEmpty() || !tMeta.specialValuePost().isEmpty())
			rLines.add(Component.translatable(specialKey(tMeta.specialValuePre()),
					aRecipe.mSpecialValue * tMeta.specialValueMultiplier(), tMeta.specialValuePost()));
		return rLines;
	}

	/**
	 * One cost line: no carrier → the GU key, one number arg (the issues #29/#34a face,
	 * untouched); pinned carrier → the _unit key, number + colored short code.
	 */
	private static Component unitLine(String aGuKey, String aUnitKey, long aNumber, TagData aEnergy) {
		return aEnergy == null
				? Component.translatable(aGuKey, aNumber)
				: Component.translatable(aUnitKey, aNumber, energyUnit(aEnergy));
	}

	/** The special-value triple's key: the FUSION "Start: " face or the crucible "Temperature: " one. */
	private static String specialKey(String aPre) {
		return "Start: ".equals(aPre) ? KEY_START : KEY_TEMPERATURE;
	}

	/** NEI :714: {@code <1200 ticks, <36000 secs, else mins} (the 20 tps / 1200-per-min folds). */
	public static Component timeLine(long aDuration) {
		if (aDuration < 1200) return Component.translatable(KEY_TIME, aDuration, Component.translatable(KEY_UNIT_TICKS));
		if (aDuration < 36000) return Component.translatable(KEY_TIME, aDuration / 20, Component.translatable(KEY_UNIT_SECS));
		return Component.translatable(KEY_TIME, aDuration / 1200, Component.translatable(KEY_UNIT_MINS));
	}

	/**
	 * NEI :662-666: the chance tooltip of one output slot — shown when the chance is
	 * strictly between 0 and the folded max 10000; the percentage prints as two decimals
	 * (the {@code (tChance/100) "." padded (tChance%100) "%"} arithmetic verbatim), with
	 * " each" when the stack is larger than one. {@code null} = no tooltip.
	 */
	public static Component chanceLine(long aChance10000, int aStackSize) {
		if (aChance10000 <= 0 || aChance10000 >= 10000) return null;
		long tFraction = aChance10000 % 100;
		String tPercent = aChance10000 / 100 + "." + (tFraction < 10 ? "0" : "") + tFraction + "%";
		return aStackSize > 1
				? Component.translatable(KEY_CHANCE_EACH, tPercent)
				: Component.translatable(KEY_CHANCE, tPercent);
	}

	/**
	 * The per-output chance read with the port's folded-max semantics (Recipe.mChances:
	 * null/short array reads 10000 past its end — Recipe.outputChance's public mirror;
	 * upstream getMaxChance folds to the 10000 constant).
	 */
	public static long outputChance(Recipe aRecipe, int aOutputIndex) {
		if (aRecipe.mChances == null || aOutputIndex < 0 || aOutputIndex >= aRecipe.mChances.length) return 10000;
		return aRecipe.mChances[aOutputIndex];
	}

	/**
	 * The not-consumed predicate for one INPUT slot: the port of the upstream
	 * stack-size-0 marker (NEI :671 {@code tStack.item.stackSize == 0}) — size-0 is not
	 * portable to 1.20.1, the port carries the never-consumed identity at count 1 behind
	 * {@link Recipe#sNotConsumable} (the circuit/mold/USB/blade disjunctions, Recipe.java
	 * field doc), so the tooltip rides the same predicate.
	 */
	public static boolean notConsumable(net.minecraft.world.item.ItemStack aInput) {
		return Recipe.sNotConsumable.test(aInput);
	}
}
