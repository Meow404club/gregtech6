package gregtech6.datagen;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * The 1.21 singular-registry directory aliases — the datagen-produced mirror of the
 * generated tree's data faces onto the directory names the 1.21+ loaders actually read
 * (task p26-w1-press-extruder-molds).
 *
 * <p><b>Why this provider exists</b> (the r2 live finding): vanilla 1.21/24w21a renamed the
 * data pack directories to the singular registry-key form — {@code tags/items → tags/item},
 * {@code tags/blocks → tags/block}, {@code recipes → recipe}, {@code loot_tables →
 * loot_table} (the 1.21.1 client-extra jar ships {@code data/minecraft/tags/item/*.json}
 * with ZERO files under {@code tags/items/}; the ADR-P17-1 era only knew the
 * {@code loot_table} face). The shared generated tree (ADR-P17-1) is written by the
 * canonical 1.20.1-forge producer in the PLURAL form, and that plural face is INVISIBLE to
 * the 1.21.1 loader — every mod data file was structurally dead on the NeoForge runtime:
 * the r2 press chain proved it live when {@code #gt6:extruder_shapes} resolved empty on the
 * 21.1 server, the not-consumable predicate read FALSE, and the crown mold was consumed.
 *
 * <p><b>Mechanics</b>: registered LAST, so the sequential {@code DataGenerator.run()} order
 * (vanilla 1.20.1 DataGenerator.java:36-47, per-provider {@code join()}) guarantees the
 * earlier providers' files are on disk when this runs. The run walks the DATA_PACK output
 * under the PLURAL families and re-saves each produced JSON at the singular path through
 * the SAME {@link DataProvider#saveStable} cache bookkeeping — the content originates from
 * the providers alone (no hand-written JSON), the aliases are tracked, deterministic and
 * cache-deduplicated (the runData 2nd-run {@code written: 0} gate holds: unchanged sources
 * hash to unchanged mirrors). A later removed source purges its stale alias through the
 * normal {@code purgeStaleAndWrite} accounting.
 *
 * <p><b>The loot face is an ADAPTER, not a byte mirror</b> (task p28-neo-loot-copy-custom-data):
 * the 1.21.1 loot parser never shipped {@code minecraft:copy_nbt} — 1.21.1
 * LootItemFunctions.java:49 registers {@code minecraft:copy_custom_data}
 * ({@code CopyCustomDataFunction}, the 1.20.5+ components-era rename) and the whole
 * registry walk holds no {@code copy_nbt} — so the byte-mirrored singular band shipped 51
 * loot tables that FAILED {@code LootDataType} parsing at boot ({@code "Unknown registry
 * key in loot_function_type: minecraft:copy_nbt"} = whole table dead = zero drops on every
 * broken pipe/painted machine). The mirror therefore rewrites the function name through
 * {@link #adaptLootFunctions21}, whose shape gate admits ONLY the codec-verified delta:
 * source/ops/path JSON is IDENTICAL across the legs — {@code NbtProviders.CODEC} (1.21.1
 * NbtProviders.java:14-20) is {@code Either(INLINE_CODEC, TYPED_CODEC)} with
 * {@code INLINE_CODEC = Codec.STRING} over "block_entity" (ContextNbtProvider.java:38-49,
 * the same bare-string form the 1.20.1 GsonAdapterFactory inline serializer wrote),
 * {@code NbtPathArgument.NbtPath.CODEC = Codec.STRING.comapFlatMap} (NbtPathArgument.java:540,
 * the path text verbatim), and the {@code MergeStrategy} names replace/append/merge are the
 * same {@code StringRepresentable}s (CopyCustomDataFunction.java:144-178 vs CopyNbtFunction
 * .java:142-192). The carrier moves underneath, invisibly to the JSON: 1.20.1 landed the ops
 * in the item's {@code tag} NBT (CopyNbtFunction.run getOrCreateTag) while 1.21.1 lands them
 * in the {@code minecraft:custom_data} COMPONENT compound (CopyCustomDataFunction.run:66-77
 * DataComponents.CUSTOM_DATA) — the {@code BlockEntityTag.'gt.foamed'} op paths stay
 * verbatim RELATIVE to that compound. Ground truth cross-check: the 1.21.1 node's own
 * datagen output (GT6LootTables.paintCopyNbt:803-817 builds the CopyCustomDataFunction
 * builder; its codec serialization) is byte-identical to the 1.20.1 table except the
 * function name. Any OTHER source shape (the typed-object nbt provider forms) is NOT
 * codec-verified and throws — a silent rename would re-create the dead-table bug class.
 *
	 * <p><b>The recipe face is a KEY-FORM ADAPTER too</b> (task p30-pool-recipe-key-form):
	 * the byte-mirrored singular recipe band made the 21.1 RecipeManager reject EVERY gt6
	 * row at boot (225/225 "Parsing error loading recipe", 2026-09-16 live run) — the
	 * 1.20.1 {@code {"item": X}}/bare-string result forms and the {@code forge:} tag
	 * values are unreadable on the 1.21.1 ItemStack codec ({@code id}) and the {@code c:}
	 * tag carrier. The mirror rewrites the census-proven delta ({@link #adaptRecipes21};
	 * the codec evidence trail lives on the method), the 1.20.1 plural face stays the byte
	 * identity (the forge runtime never scans the singular directory).
	 *
	 * <p><b>Scope</b>: the {@code gt6} and {@code minecraft} namespaces — the machine port's
 * own faces (the mold tag, the gt6 crafting rows, the loot tables, the vanilla-tag joins
 * dirt + mineable). The {@code forge} namespace is deliberately NOT mirrored: the platform
 * material-tag face on 21.1 is the {@code c:} namespace (GT6ItemTags MATERIALS_NAMESPACE),
 * so serving it needs the forge→c remap ruling — a cross-card datagen decision, declared
 * out of scope here. On the 1.21.1-neoforge node this provider is a structural no-op: its
 * own providers already write the singular names natively into the node-local
 * {@code build/datagen-output} verification tree (ADR-P17-1), so the plural walk finds
 * nothing.
 */
public class GT6DualDirectoryFaces implements DataProvider {

	/** The plural → singular root renames (vanilla 1.21/24w21a; extend when a new family ships). */
	private static final String[][] RENAMES = {
			{"tags/items", "tags/item"},
			{"tags/blocks", "tags/block"},
			{"recipes", "recipe"},
			{"loot_tables", "loot_table"},
	};

	/** The mirrored namespaces (see the Scope paragraph). */
	private static final String[] NAMESPACES = {"gt6", "minecraft"};

	/** The 1.20.1 loot carry function (LootItemFunctions.COPY_NBT — the tag-NBT carrier). */
	private static final String COPY_NBT = "minecraft:copy_nbt";

	/** The 1.21 rename (1.21.1 LootItemFunctions.java:49 — the custom_data component carrier). */
	private static final String COPY_CUSTOM_DATA = "minecraft:copy_custom_data";

	/** The loot face directory alias (the RENAMES row the adapter rides on). */
	private static final String LOOT_FACE_SINGULAR = "loot_table";

	/** The recipe face directory alias (the RENAMES row the 1.21.1 key-form adapter rides on). */
	private static final String RECIPE_FACE_SINGULAR = "recipe";

	/**
	 * The platform material-tag namespaces IN RECIPE VALUES (task p30-pool-recipe-key-form):
	 * the canonical 1.20.1 face carries {@code forge:} tag keys (the {@code Tags} constants,
	 * e.g. {@code forge:plates/steel}) while the 1.21.1 runtime tag carrier is the {@code c:}
	 * namespace (GT6ItemTags MATERIALS_NAMESPACE; the build-side neoforgeTagFaces graft lands
	 * the material band at {@code data/c/tags/item/} — build.neoforge.gradle.kts:255-262), so
	 * a mirrored {@code forge:} reference would resolve EMPTY on the 21.1 loader.
	 */
	private static final String FORGE_TAG_PREFIX = "forge:";

	private static final String COMMON_TAG_PREFIX = "c:";

	/**
	 * The biome-modifier dual-brand face (task p30-ops-biome-modifier-dual-dir, decisions
	 * .p26-worldgen-biome-modifier-dual-dir plan a): the band's directory follows the
	 * REGISTRY-KEY namespace — {@code data/gt6/forge/biome_modifier/} (the registry key
	 * {@code forge:biome_modifier}, ForgeRegistries.java:195) vs
	 * {@code data/gt6/neoforge/biome_modifier/} ({@code neoforge:biome_modifier},
	 * NeoForgeRegistries.java:61-66; the 1.21.1 directory derivation
	 * {@code Registries.elementsDirPath = CommonHooks.prefixNamespace}, Registries.java
	 * :251-253). Each leg's {@code DatapackBuiltinEntriesProvider} natively writes only its
	 * own brand — the OTHER brand's loader then reads an empty directory: the structural
	 * root cause of the r2 neo forceload (1444 chunks, zero GT6 stones, zero log errors —
	 * the shared ADR-P17-1 tree shipped only the forge face). The ruling: plan a — ONE
	 * canonical producer, BOTH brand faces shipped (a loader ignores the foreign brand's
	 * directory), never a second production path (plan b's build-side srcDir graft was
	 * ruled out for breaking the single-producer contract).
	 */
	private static final String BIOME_MODIFIER_FACE = "biome_modifier";

	/** The two brand directories (the registry-key namespaces), mirrored onto each other. */
	private static final String[] BIOME_MODIFIER_BRANDS = {"forge", "neoforge"};

	/** The AddFeaturesBiomeModifier JSON type, forge brand (ForgeBiomeModifiers.java:46 record). */
	private static final String FORGE_ADD_FEATURES = "forge:add_features";

	/** The AddFeaturesBiomeModifier JSON type, neoforge brand (BiomeModifiers.java:47 record). */
	private static final String NEOFORGE_ADD_FEATURES = "neoforge:add_features";

	private final PackOutput mOutput;

	public GT6DualDirectoryFaces(PackOutput aOutput) {
		mOutput = aOutput;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput aCache) {
		// DATA_PACK already IS the .../data root (PackOutput.Target.DATA_PACK = "data") —
		// its children are the namespace dirs (gt6/, minecraft/, ...)
		Path tData = mOutput.getOutputFolder(PackOutput.Target.DATA_PACK);
		List<CompletableFuture<?>> tSaves = new ArrayList<>(0);
		for (String tNamespace : NAMESPACES) {
			for (String[] tRename : RENAMES) {
				Path tSource = tData.resolve(tNamespace).resolve(tRename[0]);
				if (!Files.isDirectory(tSource)) continue;
				try (Stream<Path> tWalk = Files.walk(tSource)) {
					tWalk.filter(Files::isRegularFile).filter(tPath -> tPath.toString().endsWith(".json")).forEach(tFile -> {
						Path tTarget = tData.resolve(tNamespace).resolve(tRename[1])
								.resolve(tSource.relativize(tFile));
						tSaves.add(saveMirror(aCache, tFile, tTarget, tRename[1]));
					});
				} catch (IOException tError) {
					throw new RuntimeException("the dual-directory walk failed under " + tSource, tError);
				}
			}
		}
		mirrorBiomeModifiers(aCache, tData, tSaves);
		return CompletableFuture.allOf(tSaves.toArray(new CompletableFuture[0]));
	}

	/**
	 * The biome-modifier dual-brand emission (decisions.p26-worldgen-biome-modifier-dual-dir
	 * plan a): whatever brand face a leg's providers natively produced, the OTHER brand's
	 * face is emitted beside it as THE TYPE-KEY DELTA ALONE — the census-proven whole diff
	 * between the legs' biome modifier JSON (2026-09-12: 17/17 pairs byte-equal after the
	 * one {@code type} prefix swap, {@code forge:add_features} ↔ {@code neoforge:add_features};
	 * biomes/features/step codec-identical). The walk covers BOTH brand directories and is
	 * an INVOLUTION: a re-run re-mirrors the previous mirror back onto the native brand —
	 * a fixed point (the swap is self-inverse and the re-serialization is deterministic
	 * through {@link DataProvider#saveStable}), so the runData 2nd-run {@code written: 0}
	 * gate holds.
	 *
	 * <p>TWO-PHASE for the involution's read/write aliasing (the r3 live finding): phase 1
	 * reads and parses EVERY source synchronously — the async saveStable writes of one
	 * direction target exactly the other direction's read sources (forge→neoforge writes
	 * what neoforge→forge reads), and a write landing mid-read served a truncated file
	 * (parsed as {@code JsonNull} = the shape gate fired). Only after all parses may the
	 * saves be scheduled.
	 *
	 * <p>The shape gate is fail-visible (the {@link #adaptLootFunctions21} discipline): only
	 * the two {@code add_features} type names are codec-verified across the legs — a
	 * {@code remove_features}/{@code conditional} row (never generated here) has no
	 * cross-leg evidence and throws instead of emitting an unparseable JSON on the foreign
	 * loader. A silently rebranded dead row would re-create the r2 structural-zero bug
	 * class this face closes.
	 */
	private void mirrorBiomeModifiers(CachedOutput aCache, Path aData, List<CompletableFuture<?>> aSaves) {
		List<BiomeMirrorRow> tRows = new ArrayList<>(0);
		for (String tNamespace : NAMESPACES) {
			for (String tBrand : BIOME_MODIFIER_BRANDS) {
				Path tSource = aData.resolve(tNamespace).resolve(tBrand).resolve(BIOME_MODIFIER_FACE);
				if (!Files.isDirectory(tSource)) continue;
				String tTargetBrand = "forge".equals(tBrand) ? "neoforge" : "forge";
				Path tTargetRoot = aData.resolve(tNamespace).resolve(tTargetBrand).resolve(BIOME_MODIFIER_FACE);
				try (Stream<Path> tWalk = Files.walk(tSource)) {
					tWalk.filter(Files::isRegularFile).filter(tPath -> tPath.toString().endsWith(".json"))
							.forEach(tFile -> tRows.add(parseBiomeModifier(tFile,
									tTargetRoot.resolve(tSource.relativize(tFile)))));
				} catch (IOException tError) {
					throw new RuntimeException("the biome-modifier brand walk failed under " + tSource, tError);
				}
			}
		}
		for (BiomeMirrorRow tRow : tRows) {
			// task p32-ops-biome-keyorder: NOT DataProvider.saveStable — the 1.21.1 leg
			// comparator pins neoforge:conditions FIRST (1.21.1 DataProvider.java:30-38),
			// so a warm re-run re-emitted the end-yield row conditions-head while the
			// injection provider (GT6BiomeModifierConditions) keeps it at the alphabetical
			// slot: two canonical forms fighting over one file through two providers'
			// own-cache shouldWrite (vanilla HashCache.java:155-157) = the odd/even run
			// key-order oscillation (live repro 2026-09-19, node run2 flip). saveCanonical
			// is saveStable minus the leg-comparator detour: one row, ONE serializer face,
			// the brand twins byte-identical modulo the brand strings on BOTH legs (byte
			// shape identical to saveStable on the 1.20.1 leg — zero canonical-tree delta).
			aSaves.add(GT6BiomeModifierConditions.saveCanonical(aCache, tRow.mJson, tRow.mTarget));
		}
	}

	/** One collected mirror row — parsed BEFORE any save of the sibling walk is scheduled. */
	private record BiomeMirrorRow(Path mTarget, JsonObject mJson) {}

	/** Phase 1 of the mirror: read + parse + gate + the {@code type} swap (+ the conditions rebrand, p31), all synchronous. */
	private static BiomeMirrorRow parseBiomeModifier(Path aSource, Path aTarget) {
		try (Reader tReader = Files.newBufferedReader(aSource)) {
			JsonElement tJson = JsonParser.parseReader(tReader);
			if (!tJson.isJsonObject()) {
				throw new IllegalArgumentException("the biome-modifier mirror expects a JSON object (got "
						+ tJson + " in " + aSource + ")");
			}
			JsonObject tObject = tJson.getAsJsonObject();
			JsonElement tType = tObject.get("type");
			if (tType == null || !tType.isJsonPrimitive()
					|| (!FORGE_ADD_FEATURES.equals(tType.getAsString())
							&& !NEOFORGE_ADD_FEATURES.equals(tType.getAsString()))) {
				throw new IllegalArgumentException("the biome-modifier mirror only verifies the add_features "
						+ "brands (" + FORGE_ADD_FEATURES + " / " + NEOFORGE_ADD_FEATURES + ", got " + tType
						+ " in " + aSource + ") — extend parseBiomeModifier with the codec evidence "
						+ "before rebranding this shape");
			}
			// the conditions rebrand MUST run while the row's own type still names the
			// source brand — it locates the source conditions key by that type
			rebrandConditions(tObject, aSource);
			tObject.addProperty("type", FORGE_ADD_FEATURES.equals(tType.getAsString())
					? NEOFORGE_ADD_FEATURES : FORGE_ADD_FEATURES);
			return new BiomeMirrorRow(aTarget, tObject);
		} catch (IOException tError) {
			throw new RuntimeException("the biome-modifier brand mirror failed reading " + aSource, tError);
		}
	}

	/**
	 * The conditions-root-key rebrand (task p31-nether-lens-end-yield): a row carrying the
	 * loader conditions key — {@code forge:conditions} / {@code neoforge:conditions}
	 * (forge ICondition.java:25 / neo ConditionalOps.java:49 DEFAULT_CONDITIONS_KEY) —
	 * keeps that key at its member position (the end-yield row emits it FIRST, both
	 * legs' native shapes) and its condition objects keep their member order, while every
	 * condition-type VALUE string swaps namespace ({@code forge:not} ↔
	 * {@code neoforge:not}, {@code forge:mod_loaded} ↔ {@code neoforge:mod_loaded},
	 * {@code forge:item_exists} ↔ {@code neoforge:item_exists}; the JSON key is
	 * {@code "type"} on BOTH legs — forge IConditionSerializer.getJson:24 writes it, the
	 * neo ICondition.CODEC dispatch uses the DFU default key). The card spec limits the
	 * condition types to mod_loaded/item_exists (+ the composite not) — anything else is
	 * an unverified cross-leg shape and throws (the fail-visible adapter discipline).
	 */
	private static void rebrandConditions(JsonObject aObject, Path aSource) {
		String tFromKey = NEOFORGE_ADD_FEATURES.equals(aObject.get("type").getAsString())
				? "neoforge:conditions" : "forge:conditions";
		String tToKey = "forge:conditions".equals(tFromKey) ? "neoforge:conditions" : "forge:conditions";
		if (!aObject.has(tFromKey)) return; // the plain rows: no conditions face
		JsonElement tConditions = aObject.get(tFromKey);
		if (!tConditions.isJsonArray()) {
			throw new IllegalArgumentException("the conditions rebrand expects a JSON array (got " + tConditions
					+ " in " + aSource + ")");
		}
		for (JsonElement tCondition : tConditions.getAsJsonArray()) {
			if (!tCondition.isJsonObject()) {
				throw new IllegalArgumentException("the conditions rebrand expects condition objects (got "
						+ tCondition + " in " + aSource + ")");
			}
			swapConditionTypes(tCondition.getAsJsonObject(), aSource);
		}
		// re-add preserving the member positions: the conditions key keeps its slot (the
		// entry list is snapshotted — clear() before re-add keeps the Gson leg's API face,
		// which has no JsonObject.addAll)
		java.util.List<Map.Entry<String, JsonElement>> tEntries = new java.util.ArrayList<>(aObject.entrySet());
		aObject.entrySet().clear();
		for (Map.Entry<String, JsonElement> tMember : tEntries) {
			aObject.add(tFromKey.equals(tMember.getKey()) ? tToKey : tMember.getKey(), tMember.getValue());
		}
	}

	/** The recursive {@code type}-value namespace swap (see {@link #rebrandConditions}). */
	private static void swapConditionTypes(JsonObject aCondition, Path aSource) {
		for (Map.Entry<String, JsonElement> tMember : aCondition.entrySet()) {
			JsonElement tValue = tMember.getValue();
			if ("type".equals(tMember.getKey()) && tValue.isJsonPrimitive()) {
				String tType = tValue.getAsString();
				String tSwapped = tType.startsWith("forge:") ? "neoforge:" + tType.substring("forge:".length())
						: tType.startsWith("neoforge:") ? "forge:" + tType.substring("neoforge:".length()) : tType;
				if (!KNOWN_CONDITION_TYPES.contains(tSwapped)) {
					throw new IllegalArgumentException("the conditions rebrand only verifies not/mod_loaded/"
							+ "item_exists (got " + tType + " in " + aSource
							+ ") — extend KNOWN_CONDITION_TYPES with the codec evidence before rebranding");
				}
				tMember.setValue(new JsonPrimitive(tSwapped));
			} else if (tValue.isJsonObject()) {
				swapConditionTypes(tValue.getAsJsonObject(), aSource);
			}
		}
	}

	/** The verified cross-leg condition types (the card spec's mod_loaded/item_exists limit + the composite not). */
	private static final java.util.Set<String> KNOWN_CONDITION_TYPES = java.util.Set.of(
			"forge:not", "forge:mod_loaded", "forge:item_exists",
			"neoforge:not", "neoforge:mod_loaded", "neoforge:item_exists");

	/**
	 * Re-saves one produced JSON at the singular path — parse + saveStable, the canonical
	 * form both legs; the loot face rides the 1.21.1 adapter first ({@link
	 * #adaptLootFunctions21}), the recipe face rides the key-form adapter ({@link
	 * #adaptRecipes21}), every other family stays the byte identity.
	 */
	private static CompletableFuture<?> saveMirror(CachedOutput aCache, Path aSource, Path aTarget, String aSingularFace) {
		try (Reader tReader = Files.newBufferedReader(aSource)) {
			JsonElement tJson = JsonParser.parseReader(tReader);
			if (LOOT_FACE_SINGULAR.equals(aSingularFace)) {
				adaptLootFunctions21(tJson, aSource);
				adaptLootPredicates21(tJson, aSource);
			}
			else if (RECIPE_FACE_SINGULAR.equals(aSingularFace)) adaptRecipes21(tJson, aSource);
			return DataProvider.saveStable(aCache, tJson, aTarget);
		} catch (IOException tError) {
			throw new RuntimeException("the dual-directory mirror failed reading " + aSource, tError);
		}
	}

	/**
	 * The loot face 1.21.1 adapter — the ONE value-shape delta the legs' loot JSON has
	 * (task p28-neo-loot-copy-custom-data; the full codec evidence trail lives in the class
	 * javadoc). Recursive over pools/entries/functions: every {@code copy_nbt} function
	 * object becomes {@code copy_custom_data}, the member order is preserved (the Gson map
	 * keeps insertion order; an existing key's value swap is not a structural change), and
	 * the op arrays pass through untouched — their JSON is codec-identical across the legs.
	 *
	 * <p>The shape gate is fail-visible: a {@code source} that is NOT the inline bare
	 * string (the typed-object nbt-provider forms, 1.20.1 GsonAdapterFactory typed keys vs
	 * 1.21.1 {@code NbtProviders.TYPED_CODEC} dispatch) has NO verified cross-leg mapping,
	 * so the mirror throws instead of emitting a table the 1.21.1 parser may reject —
	 * a silent rename would re-create the dead-table bug class this adapter closes.
	 */
	private static void adaptLootFunctions21(JsonElement aJson, Path aSource) {
		if (aJson.isJsonObject()) {
			JsonObject tObject = aJson.getAsJsonObject();
			JsonElement tFunction = tObject.get("function");
			if (tFunction != null && tFunction.isJsonPrimitive() && COPY_NBT.equals(tFunction.getAsString())) {
				JsonElement tSource = tObject.get("source");
				if (tSource == null || !tSource.isJsonPrimitive()) {
					throw new IllegalArgumentException("the loot mirror's 1.21.1 adapter only verifies the "
							+ "inline-string nbt source (got " + tSource + " in " + aSource
							+ ") — extend adaptLootFunctions21 with the codec evidence before renaming this shape");
				}
				tObject.addProperty("function", COPY_CUSTOM_DATA);
			}
			for (Map.Entry<String, JsonElement> tMember : tObject.entrySet()) {
				adaptLootFunctions21(tMember.getValue(), aSource);
			}
		} else if (aJson.isJsonArray()) {
			for (JsonElement tElement : aJson.getAsJsonArray()) {
				adaptLootFunctions21(tElement, aSource);
			}
		}
	}

	/**
	 * The loot face 1.21.1 predicate-dialect adapter (task p33-ops-rundata-loot) — the two
	 * census-proven ItemPredicate/MatchTool dialect deltas the 1.20.1 producers emit and
	 * the 1.21.1 datagen writes natively, i.e. the INVERSE of treecheck's registered
	 * node→canonical normalizers {@code _norm_items_wrap} / {@code _norm_enchant_pred}
	 * (tools/datagen_tree_check.py:152-216, each with the vanilla census samples). Without
	 * it the forge mirror pass re-covered the singular band's committed 21.1 dialect
	 * (d6633113f, the neo node's native datagen form) with the 1.20.1 plural face's bytes
	 * on EVERY forge runData — the 1563-file loot_table write race and diff jitter P32
	 * observed across the legs (dry-run proof: the transform re-derives all 1563 committed
	 * singular files BYTE-EXACT from the plural face). The two deltas:
	 *
	 * <ul>
	 * <li>{@code items}: the 1.20.1 ItemPredicate serializes the item set — a single-item
	 * set lands as the 1-element array {@code ["X"]}; the 1.21.x datagen writes the bare
	 * string {@code "X"} (census: match_tool predicates, 179 files; sample
	 * loot_table/blocks/andesite.json). The 21.1 codec accepts both (the singular band
	 * itself carries BOTH shapes — shears leaves vs chisel strings), so only the
	 * single-element array unwraps; a multi-element array or a {@code #}-prefixed tag
	 * entry (no census instance) is left untouched.</li>
	 * <li>{@code match_tool} enchant predicate: the 1.20.1 shape nests under the
	 * condition's {@code "predicates"} map keyed
	 * {@code "minecraft:enchantments"} with per-entry key {@code "enchantments"};
	 * 1.21/24w21a flattened the EnchantmentPredicate array onto the condition object
	 * directly as {@code "enchantments"} with per-entry key {@code "enchantment"}
	 * (census: 1393 files; sample loot_table/blocks/grass.json; the codec trail in the
	 * treecheck normalizer's javadoc). ONLY the exact two-key shapes above transform —
	 * a {@code levels}-less entry keeps its shape through the same key swap; anything
	 * else fails visible.</li>
	 * </ul>
	 *
	 * <p>The shape gate is fail-visible (the {@link #adaptLootFunctions21} discipline): a
	 * predicate object whose key set matches neither verified dialect throws instead of
	 * emitting a table the 1.21.1 parser may reject. Byte-identity holds after both
	 * adapters: the Gson map preserves member order, {@code saveStable} re-serializes
	 * deterministically — the runData 2nd-run {@code written: 0} gate and the singular
	 * band's committed 21.1 dialect both stay pinned.
	 */
	private static void adaptLootPredicates21(JsonElement aJson, Path aSource) {
		if (aJson.isJsonObject()) {
			JsonObject tObject = aJson.getAsJsonObject();
			// the items unwrap — BEFORE the child walk (value replace only, not structural)
			JsonElement tItems = tObject.get("items");
			if (tItems != null && tItems.isJsonArray()) {
				com.google.gson.JsonArray tArray = tItems.getAsJsonArray();
				if (tArray.size() == 1 && tArray.get(0).isJsonPrimitive()
						&& tArray.get(0).getAsJsonPrimitive().isString()
						&& !tArray.get(0).getAsString().startsWith("#")) {
					tObject.addProperty("items", tArray.get(0).getAsString());
				}
				// multi-element / tag entries: the 21.1 codec's array+tag forms, ride untouched
			}
			// the match_tool enchant predicate flattening — the exact two-key gate
			JsonElement tEnch = tObject.get("enchantments");
			if (tEnch != null && tEnch.isJsonArray() && tObject.size() == 1) {
				com.google.gson.JsonArray tOut = new com.google.gson.JsonArray();
				for (JsonElement tEntry : tEnch.getAsJsonArray()) {
					if (!tEntry.isJsonObject()) {
						throw new IllegalArgumentException("the loot mirror's predicate adapter only verifies "
								+ "enchantment objects (got " + tEntry + " in " + aSource
								+ ") — extend adaptLootPredicates21 with the codec evidence before rewriting");
					}
					JsonObject tNew = new JsonObject();
					for (Map.Entry<String, JsonElement> tMember : tEntry.getAsJsonObject().entrySet()) {
						if ("enchantment".equals(tMember.getKey()) && tMember.getValue().isJsonPrimitive()) {
							tNew.add("enchantments", tMember.getValue());
						} else {
							tNew.add(tMember.getKey(), tMember.getValue());
						}
					}
					tOut.add(tNew);
				}
				JsonObject tPredicates = new JsonObject();
				tPredicates.add("minecraft:enchantments", tOut);
				tObject.remove("enchantments");
				tObject.add("predicates", tPredicates);
			}
			for (Map.Entry<String, JsonElement> tMember : tObject.entrySet()) {
				adaptLootPredicates21(tMember.getValue(), aSource);
			}
		} else if (aJson.isJsonArray()) {
			for (JsonElement tElement : aJson.getAsJsonArray()) {
				adaptLootPredicates21(tElement, aSource);
			}
		}
	}

	/**
	 * The recipe face 1.21.1 adapter — the key-form deltas the 21.1 RecipeManager needs to
	 * parse the singular alias (task p30-pool-recipe-key-form; the before-fix live run had
	 * ALL 225 gt6 rows dying at boot with "Parsing error loading recipe", RecipeManager
	 * .java:70 — the 1.20.1-shaped alias face is the ONLY recipe face the 1.21.1 loader
	 * scans, plural recipe/ is the 24w21a singular form it never reads). The census-proven
	 * complete delta set (the node's own datagen output is the ground truth; treecheck's
	 * recipes-band normalizers are these four in the node→canonical direction):
	 *
	 * <ul>
	 * <li>{@code result} item-form → id-form: 1.20.1 serializers write {@code {"item": X}}
	 * (objects) or the bare id string (cooking rows); 1.21.1 reads the ItemStack codec —
	 * {@code id} fieldOf + {@code count} optionalFieldOf(1) (1.21.1 ItemStack.java:103-126,
	 * STRICT_CODEC at ShapedRecipe.java:96 / ShapelessRecipe.java:86, CODEC at
	 * SimpleCookingSerializer.java:23) — and the 1.21 datagen shape is
	 * {@code {"count": N, "id": X}} (count ALWAYS written, first).</li>
	 * <li>{@code show_notification}: 1.20.1 writes the default {@code true} unconditionally
	 * on shaped rows (88/88 census), the 1.21.1 codec omits the optionalFieldOf default
	 * (ShapedRecipe.java:97) — a {@code true} is dropped, a non-default {@code false} would
	 * ride along (codec-exact: {@code true} ≡ absent).</li>
	 * <li>platform tag values {@code forge:} → {@code c:}: the 21.1 runtime tag carrier is
	 * the common namespace (see FORGE_TAG_PREFIX; node census 0 {@code forge:} survivors).</li>
	 * <li>ingredients ride UNTOUCHED: the 1.21.1 ingredient codec keeps the {@code item}
	 * /{@code tag} keys (Ingredient.java:252/:277 via CraftingHelper.makeIngredientCodec).</li>
	 * </ul>
	 *
	 * <p>The shape gate is fail-visible (the {@link #adaptLootFunctions21} discipline): a
	 * {@code result} that is neither the bare string nor the {@code item}-keyed object (extra
	 * keys like {@code nbt}, a non-string item, a non-number count) has NO codec evidence and
	 * throws instead of emitting a row the 1.21.1 parser may reject — a silent passthrough
	 * would re-create the 225-row boot-death bug class this adapter closes.
	 */
	private static void adaptRecipes21(JsonElement aJson, Path aSource) {
		if (aJson.isJsonObject()) {
			JsonObject tObject = aJson.getAsJsonObject();
			JsonElement tResult = tObject.get("result");
			if (tResult != null) adaptRecipeResult21(tObject, tResult, aSource);
			// BEFORE the child walk: the removal is structural (add-on-existing-key is not)
			JsonElement tShow = tObject.get("show_notification");
			if (tShow != null && tShow.isJsonPrimitive() && tShow.getAsBoolean()) {
				tObject.remove("show_notification");
			}
			for (Map.Entry<String, JsonElement> tMember : tObject.entrySet()) {
				JsonElement tValue = tMember.getValue();
				if ("tag".equals(tMember.getKey()) && tValue.isJsonPrimitive()
						&& tValue.getAsString().startsWith(FORGE_TAG_PREFIX)) {
					// value-layer replace on an existing key — no structural change mid-walk
					tMember.setValue(new JsonPrimitive(
							COMMON_TAG_PREFIX + tValue.getAsString().substring(FORGE_TAG_PREFIX.length())));
				}
				adaptRecipes21(tValue, aSource);
			}
		} else if (aJson.isJsonArray()) {
			for (JsonElement tElement : aJson.getAsJsonArray()) {
				adaptRecipes21(tElement, aSource);
			}
		}
	}

	/**
	 * The result member rewrite (the id-form delta): rebuilds in place as
	 * {@code {"count": N, "id": X}} — the count-first member order the 1.21 datagen shape
	 * writes (the Gson map keeps the parent's {@code result} key position untouched).
	 */
	private static void adaptRecipeResult21(JsonObject aRecipe, JsonElement aResult, Path aSource) {
		String tId;
		int tCount;
		if (aResult.isJsonPrimitive()) {
			if (!aResult.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException("the recipe mirror's 1.21.1 adapter only verifies the bare-id "
						+ "string result or the item-keyed object (got " + aResult + " in " + aSource + ")");
			}
			tId = aResult.getAsString();
			tCount = 1;
		} else if (aResult.isJsonObject()) {
			JsonObject tOld = aResult.getAsJsonObject();
			JsonElement tItem = tOld.get("item");
			if (tItem == null || !tItem.isJsonPrimitive() || !tItem.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException("the recipe mirror's 1.21.1 adapter only verifies the item-keyed "
						+ "result object (got " + aResult + " in " + aSource + ") — extend adaptRecipeResult21 with "
						+ "the codec evidence before rewriting this shape");
			}
			tId = tItem.getAsString();
			JsonElement tOldCount = tOld.get("count");
			if (tOldCount == null) {
				tCount = 1;
			} else {
				if (!tOldCount.isJsonPrimitive() || !tOldCount.getAsJsonPrimitive().isNumber()) {
					throw new IllegalArgumentException("the recipe mirror's 1.21.1 adapter only verifies a number "
							+ "count (got " + tOldCount + " in " + aSource + ")");
				}
				tCount = tOldCount.getAsInt();
			}
			// an extra key (components/...) with no nbt arm is an unverified cross-leg shape — fail-visible
			if (tOld.size() != (tOldCount == null ? 1 : 2)) {
				throw new IllegalArgumentException("the recipe mirror's 1.21.1 adapter only verifies the {item"
						+ "[,count]} result shape (got keys " + tOld.keySet() + " in " + aSource + ")");
			}
		} else {
			throw new IllegalArgumentException("the recipe mirror's 1.21.1 adapter only verifies the bare-id string "
					+ "result or the item-keyed object (got " + aResult + " in " + aSource + ")");
		}
		JsonObject tNew = new JsonObject();
		tNew.addProperty("count", tCount);
		tNew.addProperty("id", tId);
		aRecipe.add("result", tNew);
	}

	@Override
	public String getName() {
		return "GT6 Dual Directory Faces (the 1.21 singular-registry aliases)";
	}
}
