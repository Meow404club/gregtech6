/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregtech6.recipes.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.recipes.GT6RecipesOreChain;
import gregtech6.recipes.RecipeMap;
import gregtech6.recipes.tree.MaterialTreeBuilder.ByproductEdge;
import gregtech6.recipes.tree.MaterialTreeBuilder.ChainEdge;
import gregtech6.registry.GTMaterialItems;

/**
 * The per-material processing-tree display model (task debt-material-tree-b, card B of the
 * A/B/C split, ruling 2026-09-26-debt-material-tree) — the viewer-NEUTRAL seam: pure vanilla
 * ({@code net.minecraft.world.item}) plus the port's own data layer, zero JEI/EMI imports.
 * The JEI category (GT6MaterialTreeJeiCategory) and the EMI recipe (GT6MaterialTreeEmiRecipe)
 * both render FROM this model, so the two viewers draw the same geometry from the same
 * functions (the batch-1 双 viewer 共享 clause).
 *
 * <p><b>The EMI recipe-tree rework</b> (task mattree-emi-tree-rework, the user ruling
 * 「不是让排版参考EMI的配方树吗」+「就矿物处理完后没了,坩埚没了」): the assembly walk is the EMI
 * recipe-tree FACE now —
 * <ul>
 * <li><b>depth bands, not a prefix whitelist</b>: the old closed {@code COLUMN_TABLE}
 *     (11 prefixes + the ore* wildcard — the 「矿物处理完后没了」 truncation root, the r11-gaps
 *     census named it) is retired. The walk is the builder's BFS
 *     ({@link MaterialTreeBuilder#depthsFromOre}) laid on DEPTH BANDS exactly like EMI's
 *     tree recursion lays children one {@code NODE_VERTICAL_SPACING} row below their parent
 *     (BoMScreen.java:334/:337 {@code depth * NODE_VERTICAL_SPACING}): every prefix the
 *     chain indexes reach is displayed — ore chain full depth, the crucible ingot (the
 *     dynamic arm), the casting shapes, the machine and hand-crafting tails. Row order
 *     within a band = BFS encounter order (deterministic, the insertion-ordered walk).</li>
 * <li><b>forward edges only</b>: a chain hop is displayed when its target's band is
 *     {@code >=} its source's (BFS shortest-path layering; alias cycles like plate↔ingot
 *     render once along their shortest hop, the back-edge half drops — EMI never has a
 *     back edge: its tree is a recursion, BoMScreen.java:311-338).</li>
 * <li><b>no caps</b>: the old {@code MAX_ROWS=7} per-band cap and its "+N" overflow
 *     markers are retired with the whitelist (the caps were the 202px-canvas budget's
 *     device; EMI spreads siblings right without a cap — BoMScreen.java:841-857
 *     {@code addToRight} — and the pan/zoom viewport carries the canvas).</li>
 * </ul>
 *
 * <p><b>Two faces, one diagram</b> (the GTCEu ore-processing-diagram precedent, its
 * GTOreByProduct.java:66-361 one-widget-two-sources shape): the CHAIN spine comes from
 * {@link MaterialTreeBuilder#chainEdges} plus the dynamic arms the builder synthesizes
 * (crucible smelting, casting), the byproduct band merges the builder's cross-material
 * edges (the derived face — now including the crucible cross arm) with the DECLARED face —
 * {@code OreDictMaterial.mByProducts} keyed on the crushing target
 * ({@link GT6RecipesOreChain#crushingTarget}, the upstream Loader_OreProcessing.java:311
 * redirect: Fe ore shows the Fe2O3 page).
 *
 * <p><b>Performance</b>: one {@link MaterialTreeBuilder#build()} sweep per viewer
 * registration (twice per client session, JEI + EMI legs — startup-only, never per open);
 * every per-material query is the builder's hash read.
 */
public final class MaterialTreeDisplay {

	/**
	 * The show switch (the GTCEu hideOreProcessingDiagrams precedent, ConfigHolder.java:221).
	 * Config disposition (task debt-material-tree-c, acceptance ② written off as specified):
	 * the port carries NO mod-level config surface (zero ForgeConfigSpec in mdk main — the
	 * only spec-shaped hits are datagen blockstate classes), so fabricating one for a single
	 * boolean is the config infrastructure the card explicitly forbids. This non-final static
	 * stays the single switch: both viewer plugins and all registration sites read it
	 * directly, and a future config card assigns it during config load, before any viewer
	 * plugin loads (JEI/EMI construct lazily on their own entries — no earlier read exists).
	 */
	public static boolean SHOWN = true;

	/** The stable category id path (JEI RecipeType and EMI category id both {@code gt6:material_tree}). */
	public static final String CATEGORY_UID_PATH = "material_tree";

	/** Shared viewer-neutral strings — one source for both plugins (the GT6RecipeViewerText seam shape). */
	public static final String CATEGORY_TITLE = "Material Tree";
	public static final String BYPRODUCT_HEADER = "Byproducts";
	public static final String VIA_PREFIX = "via ";
	public static final String DECLARED_TEXT = "declared";

	// ---- the layout constants (the EMI recipe-tree spacing table) ------
	//
	// EVIDENCE (tmp/harvest/emi-1.20.1-src BoMScreen.java, byte-identical constants on the
	// 1.21.1 generation BoMScreen.java:63-65):
	//   NODE_WIDTH             30 (:61)  -> our BOX_WIDTH (the recipe-node box, EMI :660-661 width=42
	//                                       when the node carries a recipe — ours carries the machine icon)
	//   NODE_HORIZONTAL_SPACING 8 (:62)  -> the sibling gap inside LANE_PITCH (50 = box 42 + 8)
	//   NODE_VERTICAL_SPACING  20 (:63)  -> BAND_PITCH (the depth-band axis, ADR deviation below)
	//
	// ADR (the deliberate deviations, none silent):
	// - BAND_PITCH 28, not EMI's 20: EMI's boxes OVERLAP their next band by 1px and its
	//   connector rail paints UNDER the child icons (BoMScreen.java:684 off = 19 against a
	//   21-tall box, lines drawn before the icons at :675-728). Ours is a 42-wide two-icon
	//   box with an outline only (no fill) — a rail inside the box would cross the outline.
	//   28 = box 20 + a clean 8px wire gap (rail at +4 into the gap).
	// - LANE_PITCH 50, uniform: EMI spaces SIBLING EXTENTS (addToRight, variable subtree
	//   widths); ours spaces uniform lane centres — the layered-DAG reading of the same rule
	//   (siblings never overlap at any depth).
	// - the integer-step window-relative zoom of BoMScreen.java:352-361 stays viewport
	//   territory (MaterialTreeViewport), unchanged this task.

	/** y of the material-name header (above the first band). */
	public static final int HEADER_Y = 4;
	/** y centre of band 0 (the ore roots' row). */
	public static final int BAND_Y0 = 16;
	/** The depth-band pitch — one BFS layer per band (the EMI {@code NODE_VERTICAL_SPACING} seat). */
	public static final int BAND_PITCH = 28;
	/** The process-node box width (EMI's recipe-node 42, BoMScreen.java:660-661). */
	public static final int BOX_WIDTH = 42;
	/** The process-node box height (symmetric ±10 about the band centre; EMI :697-717 outline). */
	public static final int BOX_HEIGHT = 20;
	/** x centre of the first lane (the box's left edge lands on x 4). */
	public static final int LANE_X0 = 25;
	/** The lane pitch — box 42 + one {@code NODE_HORIZONTAL_SPACING} (8, BoMScreen.java:62). */
	public static final int LANE_PITCH = 50;

	/**
	 * The viewer PAGES' nominal frame (the JEI per-category {@code getWidth/getHeight} seat —
	 * a category dim cannot see a recipe, so it publishes this fixed frame; the CONTENT is
	 * the layout engine's content box, which the pan/zoom viewport carries when a tree grows
	 * past the frame). Nominal, not worst-case: uncapped trees have no worst case — this
	 * frame fits the reference shape (8 lanes x 6 bands + margins).
	 */
	public static final int WIDTH = LANE_X0 + (8 - 1) * LANE_PITCH + BOX_WIDTH / 2 + 12;
	public static final int HEIGHT = BAND_Y0 + 6 * BAND_PITCH + BOX_HEIGHT / 2 + 12;

	/** The production item resolver: the registered (prefix, material) handle, or null. */
	public static final BiFunction<OreDictPrefix, OreDictMaterial, Item> STANDARD_ITEMS = (aPrefix, aMaterial) -> {
		var tHandle = GTMaterialItems.get(aPrefix, aMaterial); // RegistryObject/DeferredHolder per leg — both expose .get()
		return tHandle == null ? null : tHandle.get();
	};

	// ---- the model ----------------------------------------------------

	/**
	 * One displayed slot: the prefix node at its DEPTH-BAND position with its resolved
	 * stack. {@code band} is the BFS layer from the ore roots ({@code 0} = the mined
	 * entry), {@code row} the lane within the band (the EMI sibling-spread axis).
	 */
	public record Node(OreDictPrefix prefix, int band, int row, ItemStack stack) {}

	/**
	 * One displayed chain hop: both endpoints displayed, carrying the producing maps' internal
	 * names and the machine face — {@code machine} is the representative machine stack
	 * resolved off {@link #mapNames()} through {@link MaterialTreeWorkstations#workstationStack}
	 * (first resolvable name wins; the rest ride the producing node's tooltip), EMPTY = the
	 * degrade face (the EMI box renders without its machine icon; the via-label text lives
	 * in the node tooltip).
	 */
	public record Edge(OreDictPrefix from, OreDictPrefix to, List<String> mapNames, ItemStack machine) {
		/** The label for the producing node's tooltip / the degraded via-text ("via Shredder/Anvil"). */
		public String viaLabel() { return MaterialTreeDisplay.viaLabel(mapNames()); }
	}

	/**
	 * One byproduct band slot: derived (a real row's cross-material output — now including
	 * the crucible cross arm) or declared.
	 *
	 * <p>{@code step} is the producing PROCESS STEP's band (task mattree-r2-byproduct-per-step):
	 * the BFS band the producing row's material-scoped input sits in — the machine icon of
	 * that row rides the producing node's box, so 「第 n 步的副产」 = the {@code step == n}
	 * slots. The derived face derives it from the row's input prefix's depth; the declared
	 * face is the upstream crushing redirect, which fires on the ore roots ({@code band 0}).
	 * {@code -1} = the producing input is not part of this walk — the slot belongs to no
	 * displayed step, so only the {@link gregtech6.recipes.tree.MaterialTreeLayout.ByproductView#ALL}
	 * aggregate face shows it.
	 */
	public record Byproduct(ItemStack stack, boolean derived, String sourceLabel, int step) {}

	public final OreDictMaterial material;
	private final List<Node> mNodes;
	private final List<Edge> mEdges;
	private final List<Byproduct> mByproducts;

	private MaterialTreeDisplay(OreDictMaterial aMaterial, List<Node> aNodes, List<Edge> aEdges, List<Byproduct> aByproducts) {
		material = aMaterial;
		mNodes = aNodes;
		mEdges = aEdges;
		mByproducts = aByproducts;
	}

	/** The displayed nodes (BFS layer order: bands top-down, lanes left-right within a band). */
	public List<Node> nodes() { return Collections.unmodifiableList(mNodes); }
	/** The displayed chain edges. */
	public List<Edge> edges() { return Collections.unmodifiableList(mEdges); }
	/** The merged two-face byproduct band. */
	public List<Byproduct> byproducts() { return Collections.unmodifiableList(mByproducts); }

	/** The localized producing-map label ({@code gt.recipe.shredder} → "Shredder"), internal name as fallback. */
	public static String mapLabel(String aMapName) {
		RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(aMapName);
		return tMap == null ? aMapName : tMap.mNameLocal;
	}

	/** The merged via-label ("via Shredder/Anvil") — one source for both viewers' node tooltips. */
	public static String viaLabel(List<String> aMapNames) {
		StringBuilder rLabel = new StringBuilder(VIA_PREFIX);
		for (int i = 0; i < aMapNames.size(); i++) {
			if (i > 0) rLabel.append('/');
			rLabel.append(mapLabel(aMapNames.get(i)));
		}
		return rLabel.toString();
	}

	/** The material's header name, internal as fallback (mNameLocal fills late in some init orders). */
	public static String materialName(OreDictMaterial aMaterial) {
		return aMaterial.mNameLocal != null ? aMaterial.mNameLocal : aMaterial.mNameInternal;
	}

	// ---- assembly -----------------------------------------------------

	/** One display per ore-universe material ({@link GT6RecipesOreChain#expandOreMaterials}), the production resolver. */
	public static List<MaterialTreeDisplay> buildAll(MaterialTreeBuilder aTree) {
		return buildAll(aTree, STANDARD_ITEMS);
	}

	/**
	 * One display per ore-universe material with at least one resolvable displayed node;
	 * the resolver seam lets the offline tests inject probe items (the MaterialTreeBuilderTest
	 * fixture pattern).
	 */
	public static List<MaterialTreeDisplay> buildAll(MaterialTreeBuilder aTree, BiFunction<OreDictPrefix, OreDictMaterial, Item> aItems) {
		return buildAll(aTree, aItems, MaterialTreeWorkstations::workstationStack);
	}

	/** {@link #buildAll(MaterialTreeBuilder, BiFunction)} with the machine-stack resolver seam (offline tests inject non-registry stacks). */
	public static List<MaterialTreeDisplay> buildAll(MaterialTreeBuilder aTree, BiFunction<OreDictPrefix, OreDictMaterial, Item> aItems, Function<String, ItemStack> aMachines) {
		List<MaterialTreeDisplay> rDisplays = new ArrayList<>();
		Set<OreDictMaterial> tSeen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (OreDictMaterial tMaterial : GT6RecipesOreChain.expandOreMaterials()) {
			if (!tSeen.add(tMaterial)) continue;
			MaterialTreeDisplay tDisplay = of(aTree, tMaterial, aItems, aMachines);
			if (tDisplay != null) rDisplays.add(tDisplay);
		}
		return rDisplays;
	}

	/** The one material's display, or null when nothing from its ore walk is displayable. */
	public static MaterialTreeDisplay of(MaterialTreeBuilder aTree, OreDictMaterial aMaterial, BiFunction<OreDictPrefix, OreDictMaterial, Item> aItems) {
		return of(aTree, aMaterial, aItems, MaterialTreeWorkstations::workstationStack);
	}

	/**
	 * The one material's display with the machine resolver seam: {@code aMachines} maps a
	 * RecipeMap internal name to its representative machine stack (EMPTY = untabled/unbound —
	 * the degrade face: the node box renders without its machine icon). Offline tests inject
	 * probe stacks here; production resolves through {@link MaterialTreeWorkstations#workstationStack}.
	 */
	public static MaterialTreeDisplay of(MaterialTreeBuilder aTree, OreDictMaterial aMaterial, BiFunction<OreDictPrefix, OreDictMaterial, Item> aItems, Function<String, ItemStack> aMachines) {
		// the EMI layering face: the BFS walk IS the band assignment (every reachable prefix
		// displays — no whitelist), insertion order = layer order = row order
		Map<OreDictPrefix, Integer> tDepths = aTree.depthsFromOre(aMaterial);
		if (tDepths.isEmpty()) return null;

		Map<Integer, Integer> tRowPerBand = new LinkedHashMap<>();
		Map<OreDictPrefix, Node> tNodeByPrefix = new LinkedHashMap<>();
		List<Node> tNodes = new ArrayList<>();
		for (Map.Entry<OreDictPrefix, Integer> tEntry : tDepths.entrySet()) {
			Item tItem = aItems.apply(tEntry.getKey(), aMaterial);
			if (tItem == null) continue; // no item — no node (the display drops, the walk keeps)
			int tBand = tEntry.getValue();
			int tRow = tRowPerBand.getOrDefault(tBand, 0);
			tRowPerBand.put(tBand, tRow + 1);
			Node tNode = new Node(tEntry.getKey(), tBand, tRow, new ItemStack(tItem));
			tNodes.add(tNode);
			tNodeByPrefix.put(tNode.prefix(), tNode);
		}
		if (tNodes.isEmpty()) return null;

		// edges: only hops whose both endpoints survived resolution; FORWARD (or same-band)
		// hops only — a back edge (to a shallower prefix) is the alias cycle's second half,
		// its forward twin already shows the relation (the EMI recursion never draws one,
		// BoMScreen.java:311-338); labels merged per (from, to); the machine face = the first
		// mapName with a resolvable representative stack
		Map<OreDictPrefix, Map<OreDictPrefix, Set<String>>> tEdgeMaps = new LinkedHashMap<>();
		for (ChainEdge tChain : aTree.chainEdges(aMaterial)) {
			Node tFrom = tNodeByPrefix.get(tChain.from()), tTo = tNodeByPrefix.get(tChain.to());
			if (tFrom == null || tTo == null) continue;
			if (tTo.band() < tFrom.band()) continue;
			tEdgeMaps.computeIfAbsent(tChain.from(), k -> new LinkedHashMap<>())
					.computeIfAbsent(tChain.to(), k -> new LinkedHashSet<>())
					.add(tChain.mapName());
		}
		List<Edge> tEdges = new ArrayList<>();
		for (Map.Entry<OreDictPrefix, Map<OreDictPrefix, Set<String>>> tFrom : tEdgeMaps.entrySet()) {
			for (Map.Entry<OreDictPrefix, Set<String>> tTo : tFrom.getValue().entrySet()) {
				List<String> tNames = List.copyOf(tTo.getValue());
				ItemStack tMachine = ItemStack.EMPTY;
				for (String tName : tNames) {
					tMachine = aMachines.apply(tName);
					if (!tMachine.isEmpty()) break;
				}
				tEdges.add(new Edge(tFrom.getKey(), tTo.getKey(), tNames, tMachine));
			}
		}

		// byproducts: derived first (what the rows really emit — the builder's cross-material
		// index, now carrying the crucible cross arm too), then the declared face (the
		// crushing target's mByProducts) minus what the derived face already shows; each slot
		// carries its producing step (the input prefix's BFS band — per-step hang, task
		// mattree-r2-byproduct-per-step; the declaration is the crushing redirect = band 0).
		// The derived walk is TWO-pass ON PURPOSE (the determinism face): several rows can
		// produce the same target through different input prefixes (the crusher's ore :153 and
		// raw-block :154 templates) and only ONE slot shows per target — the pass preferring
		// inputs ON the walk (a real step band) makes the visible face independent of the
		// index's insertion order, instead of whoever the set yielded first.
		List<Byproduct> tByproducts = new ArrayList<>();
		Set<OreDictMaterial> tShown = Collections.newSetFromMap(new IdentityHashMap<>());
		List<ByproductEdge> tOnWalk = new ArrayList<>(), tOffWalk = new ArrayList<>();
		for (ByproductEdge tDerived : aTree.byproductEdges(aMaterial)) {
			if (tDepths.containsKey(tDerived.inPrefix())) tOnWalk.add(tDerived);
			else tOffWalk.add(tDerived);
		}
		for (ByproductEdge tDerived : tOnWalk) {
			Item tItem = aItems.apply(tDerived.outPrefix(), tDerived.to());
			if (tItem == null || !tShown.add(tDerived.to())) continue;
			tByproducts.add(new Byproduct(new ItemStack(tItem), true, VIA_PREFIX + mapLabel(tDerived.mapName()),
					tDepths.get(tDerived.inPrefix())));
		}
		for (ByproductEdge tDerived : tOffWalk) {
			Item tItem = aItems.apply(tDerived.outPrefix(), tDerived.to());
			if (tItem == null || !tShown.add(tDerived.to())) continue;
			tByproducts.add(new Byproduct(new ItemStack(tItem), true, VIA_PREFIX + mapLabel(tDerived.mapName()), -1));
		}
		for (OreDictMaterial tDeclared : GT6RecipesOreChain.crushingTarget(aMaterial).mByProducts) {
			if (tDeclared == null || tDeclared.mID <= 0 || !tShown.add(tDeclared)) continue;
			Item tItem = aItems.apply(OP.dust, tDeclared);
			if (tItem == null) continue;
			tByproducts.add(new Byproduct(new ItemStack(tItem), false, DECLARED_TEXT, 0));
		}

		return new MaterialTreeDisplay(aMaterial, tNodes, tEdges, tByproducts);
	}
}
