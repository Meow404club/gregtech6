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
	 * <p>The shape gate is fail-visible (the {@link #adaptLootFunctions21} discipline): only
	 * the two {@code add_features} type names are codec-verified across the legs — a
	 * {@code remove_features}/{@code conditional} row (never generated here) has no
	 * cross-leg evidence and throws instead of emitting an unparseable JSON on the foreign
	 * loader. A silently rebranded dead row would re-create the r2 structural-zero bug
	 * class this face closes.
	 */
	private void mirrorBiomeModifiers(CachedOutput aCache, Path aData, List<CompletableFuture<?>> aSaves) {
		for (String tNamespace : NAMESPACES) {
			for (String tBrand : BIOME_MODIFIER_BRANDS) {
				Path tSource = aData.resolve(tNamespace).resolve(tBrand).resolve(BIOME_MODIFIER_FACE);
				if (!Files.isDirectory(tSource)) continue;
				String tTargetBrand = "forge".equals(tBrand) ? "neoforge" : "forge";
				Path tTargetRoot = aData.resolve(tNamespace).resolve(tTargetBrand).resolve(BIOME_MODIFIER_FACE);
				try (Stream<Path> tWalk = Files.walk(tSource)) {
					tWalk.filter(Files::isRegularFile).filter(tPath -> tPath.toString().endsWith(".json")).forEach(tFile -> {
						aSaves.add(saveBiomeModifierMirror(aCache, tFile,
								tTargetRoot.resolve(tSource.relativize(tFile))));
					});
				} catch (IOException tError) {
					throw new RuntimeException("the biome-modifier brand walk failed under " + tSource, tError);
				}
			}
		}
	}

	/** One brand mirror: parse + the {@code type} swap (fail-visible gate) + saveStable. */
	private static CompletableFuture<?> saveBiomeModifierMirror(CachedOutput aCache, Path aSource, Path aTarget) {
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
						+ " in " + aSource + ") — extend saveBiomeModifierMirror with the codec evidence "
						+ "before rebranding this shape");
			}
			tObject.addProperty("type", FORGE_ADD_FEATURES.equals(tType.getAsString())
					? NEOFORGE_ADD_FEATURES : FORGE_ADD_FEATURES);
			return DataProvider.saveStable(aCache, tJson, aTarget);
		} catch (IOException tError) {
			throw new RuntimeException("the biome-modifier brand mirror failed reading " + aSource, tError);
		}
	}

	/**
	 * Re-saves one produced JSON at the singular path — parse + saveStable, the canonical
	 * form both legs; the loot face rides the 1.21.1 adapter first ({@link
	 * #adaptLootFunctions21}), every other family stays the byte identity.
	 */
	private static CompletableFuture<?> saveMirror(CachedOutput aCache, Path aSource, Path aTarget, String aSingularFace) {
		try (Reader tReader = Files.newBufferedReader(aSource)) {
			JsonElement tJson = JsonParser.parseReader(tReader);
			if (LOOT_FACE_SINGULAR.equals(aSingularFace)) adaptLootFunctions21(tJson, aSource);
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

	@Override
	public String getName() {
		return "GT6 Dual Directory Faces (the 1.21 singular-registry aliases)";
	}
}
