package gregtech6.datagen;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
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
						tSaves.add(saveMirror(aCache, tFile, tTarget));
					});
				} catch (IOException tError) {
					throw new RuntimeException("the dual-directory walk failed under " + tSource, tError);
				}
			}
		}
		return CompletableFuture.allOf(tSaves.toArray(new CompletableFuture[0]));
	}

	/** Re-saves one produced JSON at the singular path — parse + saveStable, the canonical form both legs. */
	private static CompletableFuture<?> saveMirror(CachedOutput aCache, Path aSource, Path aTarget) {
		try (Reader tReader = Files.newBufferedReader(aSource)) {
			JsonElement tJson = JsonParser.parseReader(tReader);
			return DataProvider.saveStable(aCache, tJson, aTarget);
		} catch (IOException tError) {
			throw new RuntimeException("the dual-directory mirror failed reading " + aSource, tError);
		}
	}

	@Override
	public String getName() {
		return "GT6 Dual Directory Faces (the 1.21 singular-registry aliases)";
	}
}
