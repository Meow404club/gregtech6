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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * The tier-b datapack JSON seam of the runtime RecipeMaps (task
 * p26-tier-b-rm-json-loader, design state research.p26-r-tier-b-rm-json): pack authors
 * drop {@code data/gt6/recipe_maps/<map_key>.json} files — {@code {"recipes": [...]}} —
 * and every row is poured straight into the matching {@link GT6RecipeMaps} map at each
 * (re)load of the server data.
 *
 * <p><b>Hook</b> (both legs read-verified): a {@link SimpleJsonResourceReloadListener}
 * registered through {@code AddReloadListenerEvent#addListener} (forge-1.20.1
 * AddReloadListenerEvent.java:30-48 — the event fires on the main bus per server-data
 * reload, its registry access is "loaded and frozen by this point" :72-80; the 1.21.1
 * NeoForge twin carries the same three methods). The {@code apply} half of the listener
 * runs on the SERVER thread (SimplePreparableReloadListener.java:9-20, the
 * {@code gameExecutor} leg), so the pour into the static volatile RM needs no extra
 * sync — the same thread consumes it through {@code findRecipe} on the BET tick.
 * The event only ever fires for the logical server's resource chain (dedicated boot,
 * integrated world load, /reload), so a pure client JVM never pours anything.
 *
 * <p><b>File format</b>: one file per RecipeMap, object-shaped
 * {@code {"recipes": [<row>, ...]}} — the vanilla datapack convention (top-level
 * objects, no top-level arrays) where two packs shipping the same path resolve by
 * priority to a WHOLE-FILE override (the {@code listMatchingResources} scan of the
 * inherited {@code prepare()}). Rows carry no identity of their own (the upstream
 * {@code mRecipeList} is an identity HashSet of anonymous rows), so the file is the
 * pack-authoring unit: a (re)load REPLACES this loader's previous subset per map —
 * identity-tracked remove-then-add, the {@code GT6CokeOvenTagListener.replaceLogRecipes}
 * shape (:102-108) — which makes repeated reloads idempotent and never touches rows
 * owned by anyone else.
 *
 * <p><b>Three-owner co-existence</b>: the {@code mRecipeList} of a map is shared by
 * (1) the seven static pour loaders (FMLCommonSetup rows — not ours), (2) the
 * {@code GT6CokeOvenTagListener} tag-derived subset (not ours), and (3) this loader's
 * JSON subset (the only rows we ever remove). There is deliberately NO content-level
 * dedup: a JSON row with the same inputs as a static row leaves both in the map and
 * {@code findRecipe} answers with the first scan hit (an undefined-order face, the
 * documented v1 limitation — row deletion needs the tier-c script tier).
 *
 * <p><b>MAP KEYS</b> = the 13 pourable maps ({@link #POURABLE}, the census minus the
 * furnace pair; MIXER joined at the p26-c-foam-fluid-refill review ruling, BATH joins
 * with its own map declaration — task p26-kitchen-pot-bowl). A file named
 * {@code furnace.json} or {@code furnace_fuel.json} is REJECTED with an ERROR log and
 * the whole file is skipped: {@code FURNACE} proxies the vanilla RecipeManager and
 * {@code FURNACE_FUEL} synthesizes rows on demand — neither ever reads its row stock,
 * so poured rows would be dead weight (GT6RecipeMaps.java:203-209/:154).
 *
 * <p><b>Row semantics</b> (the 7-field schema → the 9-arg {@link Recipe}
 * constructor (Recipe.java:147-163) → {@link RecipeMap#addRecipe} (RecipeMap.java:120-125)):
 * <ul>
 * <li>{@code inputs}/{@code outputs}: {@code {"item": "<id>", "count": 1}}; outputs take
 *     an optional per-slot {@code "chance"} (10000 base; the row without any chance key
 *     stays {@code null}-chanced = deterministic; {@code chance <= 0} is a LEGAL
 *     "never produces" slot with a WARN — the port's declared deviation from the
 *     upstream {@code <= 0 → 10000} rewrite, Recipe.java:74-77).</li>
 * <li>{@code fluidInputs}/{@code fluidOutputs}: {@code {"fluid": "<id>", "amount": <mB>}}
 *     — the amount is millibuckets, GT6 liquid-unit conversions happen at row-writing
 *     time (the p6 U2=500/U=1000 precedent), the loader converts nothing.</li>
 * <li>{@code duration} (ticks, required, {@code <= 0} = bad row), {@code eut} (may be
 *     NEGATIVE — the fuel-map semantics; {@code Recipe.getAbsoluteTotalPower} :171-173
 *     takes the absolute value and the findRecipe gate is absolute-valued), and
 *     {@code specialValue} (optional, default 0, consumed only by the SHCL display face,
 *     TileEntityBasicMachine.java:320/:606).</li>
 * </ul>
 *
 * <p><b>BAD ROWS</b> never crash the reload: each one logs a WARN (file id + row index +
 * reason) and is skipped — the vanilla per-file tolerance style
 * (SimpleJsonResourceReloadListener :47-49), the skip-and-count loader convention of
 * this repo, and a crashed /reload would punish the whole server for one typo. The only
 * inherited hard failure is a duplicate file ID across packs
 * (the vanilla {@code IllegalStateException} at :44-46). Every apply ends with an INFO
 * line carrying the per-file before/after row counts read from the live map (the audit
 * face, the GT6CokeOvenTagListener.java:94 log shape).
 *
 * <p><b>v1 BOUNDARIES — the tag-input TRAP</b>: tag inputs are NOT supported, and that
 * is a TIMING ruling, not a parsing one. {@code TagManager.apply} only STORES the loaded
 * tag contents — the binding happens in the reload-finish callback
 * ({@code MinecraftServer.updateRegistryTags}, vanilla 1.20.1 :1327) which runs AFTER
 * every listener's apply, and only then posts {@code TagsUpdatedEvent}. At the moment
 * this method runs, an {@code ITag} read is stale (warm reload) or empty (first boot),
 * so expanding tags here would bake stale data into rows. Pack authors who want tag
 * tolerance: the row input can be a GT6 {@code MaterialPrefixItem} id — the P25
 * matching-period fallback ({@code Recipe.matchesByMaterialTag}, Recipe.java:343-398)
 * then matches the whole material family for free at lookup time.
 *
 * <p>Also v1-out: row deletion and content-level overwrite of static rows (file =
 * whole-file per map, see above), client-side sync (a dedicated-server client JVM
 * receives no rows — the JEI text pages stay static, the category bridge is the P12
 * pool), row-level flags ({@code mFakeRecipe}/{@code mNeedsEmptyOutput}/... stay at
 * their defaults), and per-row notConsumed (a circuit in the inputs is already
 * never-consumed through {@code Recipe.sNotConsumable}).
 *
 * <p><b>Dual-leg</b>: zero mechanism divergence — the vanilla listener base is
 * verbatim-identical on 1.20.1/1.21.1, and the four loader-divergence faces (the event
 * import, the bus import, the registry lookups, the annotation) all ride the existing
 * stonecutter swap table (the GT6CokeOvenTagListener.java:12-19 precedent of unguarded
 * forge-form imports), no {@code //?} block of its own.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GT6RecipeMapJsonLoader extends SimpleJsonResourceReloadListener {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The datapack directory under {@code data/<namespace>/}, per the design ruling. */
	public static final String DIRECTORY = "recipe_maps";

	/** The namespace this seam reads ({@code data/gt6/recipe_maps/}). */
	public static final String DATA_NAMESPACE = "gt6";

	/**
	 * The 21 pourable map keys (the registered {@link GT6RecipeMaps} census minus
	 * FURNACE/FURNACE_FUEL — 11 at the tier-b landing, the 12th is the MIXER append of
	 * task p26-c-foam-fluid-refill (whose review ruling joins it here), the 13th the
	 * BATH append of task p26-kitchen-pot-bowl; the P29 W1 wave appends its owning
	 * cards' keys: the four roll-ladder keys of task p29-w1-kinetic-roll-ladder
	 * (rollingmill — the map the p28 ULV rung shares — rollbender, rollformer,
	 * clustermill) and the six batch-C process keys of task
	 * p29-w1-kinetic-process-ladder (cutter, squeezer, centrifuge, sluice, sharpening,
	 * pressurewasher) — the whitelist keeps growing with its census).
	 * The pourable map keys (the registered {@link GT6RecipeMaps} census minus
	 * FURNACE/FURNACE_FUEL — 13 through the p26-kitchen-pot-bowl BATH append; the four
	 * card-D datapack-domain maps of task p29-w1-eu-hu-families join at the p29 W1 wave:
	 * {@code loom}, {@code boxinator}, {@code unboxinator}, {@code fermenter} — the map
	 * names are the card-A twelve-map block's GT6RecipeMapJsonLoader anchors, now live
	 * pour targets for the smoke rows the eu-hu machine chains drive).	 */
	private static final Set<String> POURABLE = Set.of(
			"coke_oven", "shredder", "crusher", "lathe", "chisel", "engine_fuels",
			"fluidbed", "burn", "distillery", "drying", "canner", "mixer", "bath",
			// the P29 W1 card-B roll-ladder four (task p29-w1-kinetic-roll-ladder)
			"rollingmill", "rollbender", "rollformer", "clustermill",
			// the P29 W1 process-card six (task p29-w1-kinetic-process-ladder): the batch-C
			// smoke-row map keys — the card-A constants the Sluice tail-append joins
			"cutter", "squeezer", "centrifuge", "sluice", "sharpening", "pressurewasher");
			"loom", "boxinator", "unboxinator", "fermenter");
	/** The two zero-static-row-stock maps: a file for them is a hard ERROR (class doc). */
	private static final Set<String> FORBIDDEN = Set.of("furnace", "furnace_fuel");

	/** The singleton registered at every AddReloadListenerEvent (stateless — all state is static, the RM is a static registry). */
	private static final GT6RecipeMapJsonLoader INSTANCE = new GT6RecipeMapJsonLoader();

	/**
	 * The item seam: the live registry lookups by default, fixtures injected offline
	 * (the GTEngineFuelsTest {@code sFluidResolver} convention). The lambda body — not a
	 * method reference — so the 1.21.1 stonecutter swap rewrites the lookup face.
	 */
	static Function<ResourceLocation, Item> sItemResolver = aId -> ForgeRegistries.ITEMS.getValue(aId);

	/** The fluid seam, same shape as {@link #sItemResolver}. */
	static Function<ResourceLocation, Fluid> sFluidResolver = aId -> ForgeRegistries.FLUIDS.getValue(aId);

	/**
	 * The currently-poured JSON subsets, per map key — the identity-tracker of the
	 * remove-then-add replace (the TagListener {@code sLogRecipes} shape, keyed per map
	 * because a reload can pour any subset of the 11 maps).
	 */
	private static volatile Map<String, List<Recipe>> sPoured = new HashMap<>();

	// ADR-P18: the tracker joins the GT6RecipeMaps generation — a reset() retires the
	// tracker WITH the maps, so no stale-instance residue survives into the next
	// generation (the apply-side null-gate stays as the broken-lifecycle guard).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipeMapJsonLoader::resetForTest);}

	private GT6RecipeMapJsonLoader() {
		super(new Gson(), DIRECTORY);
	}

	/** The event hook: registers the listener for every logical-server data (re)load (main bus; a client JVM never fires it). */
	@SubscribeEvent
	public static void onAddReloadListeners(AddReloadListenerEvent aEvent) {
		aEvent.addListener(INSTANCE);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> aData, ResourceManager aResourceManager, ProfilerFiller aProfiler) {
		pour(aData);
	}

	/** Test/lifecycle seam: clears the identity tracker so a fresh generation starts empty. */
	static void resetForTest() {
		sPoured = new HashMap<>();
	}

	/** The current JSON subset size for a map key (the audit/acceptance read; 0 when nothing poured). */
	public static int pouredCount(String aMapKey) {
		List<Recipe> tSubset = sPoured.get(aMapKey);
		return tSubset == null ? 0 : tSubset.size();
	}

	/**
	 * The pour: every whitelisted file's rows replace that map's JSON subset. Visible
	 * offline and live; the {@code apply} override delegates here with the prepared scan.
	 */
	public static void pour(Map<ResourceLocation, JsonElement> aData) {
		int tFiles = 0;
		for (Map.Entry<ResourceLocation, JsonElement> tEntry : aData.entrySet()) {
			ResourceLocation tId = tEntry.getKey();
			String tKey = tId.getPath();
			if (!DATA_NAMESPACE.equals(tId.getNamespace())) {
				// a foreign namespace is another pack's own directory — ours only when it
				// squats one of OUR keys (likely a mistake): visible, then skipped.
				if (POURABLE.contains(tKey) || FORBIDDEN.contains(tKey)) {
					LOGGER.warn("GT6 Recipe Maps JSON: skipped file {} — the '{}' map key is only read from the {} namespace", tId, tKey, DATA_NAMESPACE);
				}
				continue;
			}
			if (FORBIDDEN.contains(tKey)) {
				LOGGER.error("GT6 Recipe Maps JSON: file {} REJECTED — the '{}' map has zero static row stock (FURNACE proxies the vanilla RecipeManager, FURNACE_FUEL synthesizes on demand); JSON rows there would never be consumed", tId, tKey);
				continue;
			}
			RecipeMap tMap = mapFor(tKey);
			if (tMap == null) {
				if (!POURABLE.contains(tKey)) {
					LOGGER.warn("GT6 Recipe Maps JSON: skipped file {} — '{}' is not one of the pourable map keys", tId, tKey);
					continue;
				}
				// whitelist hit but the map instance is gone (a broken lifecycle / pre-init scan)
				LOGGER.warn("GT6 Recipe Maps JSON: skipped file {} — the '{}' map instance is not registered (broken lifecycle?)", tId, tKey);
				continue;
			}
			if (pourFile(tId, tKey, tMap, tEntry.getValue())) tFiles++;
		}
		LOGGER.info("GT6 Recipe Maps JSON: {} file(s) poured", tFiles);
	}

	/** One file → one subset replace. Returns false when the file shape itself was rejected (whole-file skip). */
	private static boolean pourFile(ResourceLocation aFileId, String aKey, RecipeMap aMap, JsonElement aJson) {
		if (!aJson.isJsonObject()) {
			LOGGER.error("GT6 Recipe Maps JSON: skipped file {} — top level must be an object {{\"recipes\": [...]}}, got {}", aFileId, describe(aJson));
			return false;
		}
		JsonElement tRecipes = aJson.getAsJsonObject().get("recipes");
		if (tRecipes == null || !tRecipes.isJsonArray()) {
			LOGGER.error("GT6 Recipe Maps JSON: skipped file {} — missing or non-array \"recipes\" member", aFileId);
			return false;
		}
		JsonArray tRows = tRecipes.getAsJsonArray();
		List<Recipe> tRowsPoured = new ArrayList<>(tRows.size());
		long tBefore = aMap.mRecipeList.size();
		int tSkipped = 0;
		for (int i = 0; i < tRows.size(); i++) {
			Recipe tRecipe = parseRow(tRows.get(i), aFileId, i);
			if (tRecipe == null) {tSkipped++; continue;}
			if (aMap.addRecipe(tRecipe) == null) {
				// the double-empty ghost guard (RecipeMap.java:122) — the parse could not see it coming only
				// when the row declared no input leg at all
				LOGGER.warn("GT6 Recipe Maps JSON: skipped row {} in {} — no inputs at all (the ghost-recipe guard rejects input-less rows)", i, aFileId);
				tSkipped++;
				continue;
			}
			tRowsPoured.add(tRecipe);
		}
		// the subset replace: out with OUR previous instances (identity), in with the fresh ones.
		List<Recipe> tPrevious = sPoured.put(aKey, tRowsPoured);
		if (tPrevious != null) aMap.mRecipeList.removeAll(tPrevious);
		LOGGER.info("GT6 Recipe Maps JSON: {} → {}: rows {} → {} (+{} JSON poured, {} skipped)", aFileId, aMap.mNameInternal, tBefore, aMap.mRecipeList.size(), tRowsPoured.size(), tSkipped);
		return true;
	}

	/** Row → Recipe, or null after a WARN (file id + row index + reason) — a bad row never crashes the reload. */
	@Nullable
	private static Recipe parseRow(JsonElement aElement, ResourceLocation aFileId, int aIndex) {
		if (!aElement.isJsonObject()) return badRow(aFileId, aIndex, "row must be a JSON object, got " + describe(aElement));
		JsonObject tRow = aElement.getAsJsonObject();

		ItemStack[] tInputs = parseItemSlots(tRow.get("inputs"), aFileId, aIndex);
		if (tInputs == null) return null;
		ItemStack[] tOutputs;
		long[] tChances;
		if (tRow.get("outputs") == null) {
			tOutputs = new ItemStack[0];
			tChances = null;
		} else {
			if (!tRow.get("outputs").isJsonArray()) return badRow(aFileId, aIndex, "\"outputs\" must be an array");
			JsonArray tOut = tRow.get("outputs").getAsJsonArray();
			tOutputs = new ItemStack[tOut.size()];
			tChances = null;
			for (int i = 0; i < tOut.size(); i++) {
				if (!tOut.get(i).isJsonObject()) return badRow(aFileId, aIndex, "output slot " + i + " must be an object");
				JsonObject tSlot = tOut.get(i).getAsJsonObject();
				if (tSlot.has("tag")) return badRow(aFileId, aIndex, tagTrap("output slot " + i));
				Item tItem = resolveItem(tSlot, aFileId, aIndex);
				if (tItem == null) return null;
				Long tCount = optBoundedLong(tSlot, "count", 1, aFileId, aIndex);
				if (tCount == null) return null;
				tOutputs[i] = new ItemStack(tItem, tCount.intValue());
				if (tSlot.has("chance")) {
					Long tChance = optLong(tSlot, "chance", 0, aFileId, aIndex);
					if (tChance == null) return null;
					if (tChance.longValue() <= 0) {
						// LEGAL no-output slot — the port's declared deviation: upstream would rewrite
						// chance<=0 to 10000 (Recipe.java:906); the port keeps 0 = never (Recipe.java:74-77)
						LOGGER.warn("GT6 Recipe Maps JSON: row {} in {} output slot {} — chance {} <= 0 keeps the NO-OUTPUT semantics (upstream would rewrite it to 10000; declared port deviation)", aIndex, aFileId, i, tChance);
					}
					if (tChances == null) tChances = filled(tOut.size(), 10000L);
					tChances[i] = tChance.longValue();
				}
			}
		}
		FluidStack[] tFluidInputs = parseFluidSlots(tRow.get("fluidInputs"), aFileId, aIndex);
		if (tFluidInputs == null) return null;
		FluidStack[] tFluidOutputs = parseFluidSlots(tRow.get("fluidOutputs"), aFileId, aIndex);
		if (tFluidOutputs == null) return null;

		if (!tRow.has("duration")) return badRow(aFileId, aIndex, "\"duration\" is required (ticks)");
		Long tDuration = optLong(tRow, "duration", 0, aFileId, aIndex);
		if (tDuration == null) return null;
		if (tDuration.longValue() <= 0) return badRow(aFileId, aIndex, "\"duration\" must be > 0, got " + tDuration);
		Long tEUt = optLong(tRow, "eut", 0, aFileId, aIndex);
		if (tEUt == null) return null;
		Long tSpecial = optLong(tRow, "specialValue", 0, aFileId, aIndex);
		if (tSpecial == null) return null;

		// the 9-arg chances-bearing constructor (Recipe.java:147-163): canBeBuffered T, no
		// row flags (v1 boundary), chances aligned/trimmed by the constructor itself
		return new Recipe(true, tInputs, tOutputs, tFluidInputs, tFluidOutputs, tDuration.longValue(), tEUt.longValue(), tSpecial.longValue(), tChances);
	}

	/** The "inputs" array (optional, default empty) → item slots; null after a WARN. */
	@Nullable
	private static ItemStack[] parseItemSlots(@Nullable JsonElement aElement, ResourceLocation aFileId, int aIndex) {
		if (aElement == null) return new ItemStack[0];
		if (!aElement.isJsonArray()) {badRow(aFileId, aIndex, "\"inputs\" must be an array"); return null;}
		JsonArray tArray = aElement.getAsJsonArray();
		ItemStack[] rSlots = new ItemStack[tArray.size()];
		for (int i = 0; i < tArray.size(); i++) {
			if (!tArray.get(i).isJsonObject()) {badRow(aFileId, aIndex, "input slot " + i + " must be an object"); return null;}
			JsonObject tSlot = tArray.get(i).getAsJsonObject();
			if (tSlot.has("tag")) {badRow(aFileId, aIndex, tagTrap("input slot " + i)); return null;}
			Item tItem = resolveItem(tSlot, aFileId, aIndex);
			if (tItem == null) return null;
			Long tCount = optBoundedLong(tSlot, "count", 1, aFileId, aIndex);
			if (tCount == null) return null;
			rSlots[i] = new ItemStack(tItem, tCount.intValue());
		}
		return rSlots;
	}

	/** The fluidInputs/fluidOutputs array (optional, default empty) → fluid slots; null after a WARN. */
	@Nullable
	private static FluidStack[] parseFluidSlots(@Nullable JsonElement aElement, ResourceLocation aFileId, int aIndex) {
		if (aElement == null) return new FluidStack[0];
		if (!aElement.isJsonArray()) {badRow(aFileId, aIndex, "fluid slot array must be a JSON array"); return null;}
		JsonArray tArray = aElement.getAsJsonArray();
		FluidStack[] rSlots = new FluidStack[tArray.size()];
		for (int i = 0; i < tArray.size(); i++) {
			if (!tArray.get(i).isJsonObject()) {badRow(aFileId, aIndex, "fluid slot " + i + " must be an object"); return null;}
			JsonObject tSlot = tArray.get(i).getAsJsonObject();
			if (tSlot.has("tag")) {badRow(aFileId, aIndex, tagTrap("fluid slot " + i)); return null;}
			Fluid tFluid = resolveFluid(tSlot, aFileId, aIndex);
			if (tFluid == null) return null;
			Long tAmount = optBoundedLong(tSlot, "amount", 1, aFileId, aIndex);
			if (tAmount == null) return null;
			rSlots[i] = new FluidStack(tFluid, tAmount.intValue()); // amount = mB (class doc); both legs take (Fluid, int)
		}
		return rSlots;
	}

	/** The {"item": "<id>"} member → a live Item, or null after a WARN (unregistered ids are bad rows). */
	@Nullable
	private static Item resolveItem(JsonObject aSlot, ResourceLocation aFileId, int aIndex) {
		JsonElement tId = aSlot.get("item");
		if (tId == null || !tId.isJsonPrimitive()) {badRow(aFileId, aIndex, "item slot needs a string \"item\" id"); return null;}
		String tText = tId.getAsString(); // hoisted: the 1.21.1 parse swap's regex takes no nested-call args
		ResourceLocation tKey;
		try {
			tKey = new ResourceLocation(tText);
		} catch (IllegalArgumentException e) {
			return badRow(aFileId, aIndex, "malformed item id \"" + tText + "\": " + e.getMessage());
		}
		Item tItem = sItemResolver.apply(tKey);
		// the live registry lookup answers the MISSING-ENTRY placeholder (vanilla air), not
		// null, for an unregistered id — an air stand-in would silently collapse the slot and
		// drop the row at the ghost guard with a misleading reason, so it is a bad row HERE
		// (the GT6RecipesCokeOven mat()→null drop precedent, made visible)
		if (tItem == null || tItem == Items.AIR) return badRow(aFileId, aIndex, "unregistered item id \"" + tText + "\"");
		return tItem;
	}

	/** The {"fluid": "<id>"} member → a live Fluid, same contract as {@link #resolveItem}. */
	@Nullable
	private static Fluid resolveFluid(JsonObject aSlot, ResourceLocation aFileId, int aIndex) {
		JsonElement tId = aSlot.get("fluid");
		if (tId == null || !tId.isJsonPrimitive()) {badRow(aFileId, aIndex, "fluid slot needs a string \"fluid\" id"); return null;}
		String tText = tId.getAsString();
		ResourceLocation tKey;
		try {
			tKey = new ResourceLocation(tText);
		} catch (IllegalArgumentException e) {
			return badRow(aFileId, aIndex, "malformed fluid id \"" + tText + "\": " + e.getMessage());
		}
		Fluid tFluid = sFluidResolver.apply(tKey);
		// the missing-entry placeholder (the empty fluid), same contract as resolveItem
		if (tFluid == null || tFluid == Fluids.EMPTY) return badRow(aFileId, aIndex, "unregistered fluid id \"" + tText + "\"");
		return tFluid;
	}

	/** An optional long member with the given default; present-but-non-numeric is a bad row (null after the WARN). */
	@Nullable
	private static Long optLong(JsonObject aRow, String aKey, long aDefault, ResourceLocation aFileId, int aIndex) {
		JsonElement tElement = aRow.get(aKey);
		if (tElement == null) return Long.valueOf(aDefault);
		if (!tElement.isJsonPrimitive() || !tElement.getAsJsonPrimitive().isNumber()) {
			return badRow(aFileId, aIndex, "\"" + aKey + "\" must be a number, got " + describe(tElement));
		}
		return Long.valueOf(tElement.getAsLong());
	}

	/** An optional long member with a positivity floor ({@code <= 0} = bad row); also rejects counts past the int slots. */
	@Nullable
	private static Long optBoundedLong(JsonObject aRow, String aKey, long aDefault, ResourceLocation aFileId, int aIndex) {
		Long tValue = optLong(aRow, aKey, aDefault, aFileId, aIndex);
		if (tValue == null) return null;
		if (tValue.longValue() <= 0) {
			return badRow(aFileId, aIndex, "\"" + aKey + "\" must be > 0, got " + tValue);
		}
		if (tValue.longValue() > 0x7FFFFFFFL) {
			return badRow(aFileId, aIndex, "\"" + aKey + "\" overflows the stack/amount int slot, got " + tValue);
		}
		return tValue;
	}

	/** The v1 tag-input rejection reason — the timing trap verbatim (class doc). */
	private static String tagTrap(String aWhere) {
		return aWhere + " carries a \"tag\" member — tag inputs are NOT supported in v1: the reload-apply stage runs BEFORE vanilla binds tags (TagManager.apply only stores; the bind and TagsUpdatedEvent happen after apply), so a tag expanded here would bake stale/empty data. Use concrete ids; a gt6 MaterialPrefixItem input inherits the P25 matching-period material-family fallback for free.";
	}

	/** The uniform bad-row verdict: WARN with file + index + reason, skip the row (never crash the reload). */
	@Nullable
	private static <T> T badRow(ResourceLocation aFileId, int aIndex, String aReason) {
		LOGGER.warn("GT6 Recipe Maps JSON: skipped row {} in {} — {}", aIndex, aFileId, aReason);
		return null;
	}

	/** A terse JsonElement descriptor for the log messages. */
	private static String describe(@Nullable JsonElement aElement) {
		if (aElement == null) return "nothing";
		if (aElement.isJsonNull()) return "null";
		if (aElement.isJsonObject()) return "an object";
		if (aElement.isJsonArray()) return "an array";
		if (aElement.isJsonPrimitive()) return "a primitive (" + aElement.getAsString() + ")";
		return aElement.toString();
	}

	private static long[] filled(int aSize, long aValue) {
		long[] rArray = new long[aSize];
		java.util.Arrays.fill(rArray, aValue);
		return rArray;
	}

	/** The map-key → map-instance resolution over the static volatile registry (null = not registered). */
	static RecipeMap mapFor(String aKey) {
		return switch (aKey) {
			case "coke_oven" -> GT6RecipeMaps.COKE_OVEN;
			case "shredder" -> GT6RecipeMaps.SHREDDER;
			case "crusher" -> GT6RecipeMaps.CRUSHER;
			case "lathe" -> GT6RecipeMaps.LATHE;
			case "chisel" -> GT6RecipeMaps.CHISEL;
			case "engine_fuels" -> GT6RecipeMaps.ENGINE_FUELS;
			case "fluidbed" -> GT6RecipeMaps.FLUIDBED;
			case "burn" -> GT6RecipeMaps.BURN;
			case "distillery" -> GT6RecipeMaps.DISTILLERY;
			case "drying" -> GT6RecipeMaps.DRYING;
			case "canner" -> GT6RecipeMaps.CANNER;
			case "mixer" -> GT6RecipeMaps.MIXER;
			case "bath" -> GT6RecipeMaps.BATH;
			case "rollingmill" -> GT6RecipeMaps.ROLLING_MILL; // task p29-w1-kinetic-roll-ladder — the roll-ladder smoke rows
			case "rollbender" -> GT6RecipeMaps.ROLL_BENDER;
			case "rollformer" -> GT6RecipeMaps.ROLL_FORMER;
			case "clustermill" -> GT6RecipeMaps.CLUSTER_MILL;
			// the P29 W1 process-card six (task p29-w1-kinetic-process-ladder; "sharpening"
			// keys the SHARPENING field — the key form is the field-name snake case, the
			// gt.recipe.sharpener LOCAL name is the upstream GUI word)
			case "cutter" -> GT6RecipeMaps.CUTTER;
			case "squeezer" -> GT6RecipeMaps.SQUEEZER;
			case "centrifuge" -> GT6RecipeMaps.CENTRIFUGE;
			case "sluice" -> GT6RecipeMaps.SLUICE;
			case "sharpening" -> GT6RecipeMaps.SHARPENING;
			case "pressurewasher" -> GT6RecipeMaps.PRESSURE_WASHER;
			default -> null;
		};
	}
}
