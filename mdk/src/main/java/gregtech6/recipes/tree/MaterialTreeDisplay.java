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
 * <p><b>Two faces, one diagram</b> (the GTCEu ore-processing-diagram precedent, its
 * GTOreByProduct.java:66-361 one-widget-two-sources shape): the CHAIN spine comes from
 * {@link MaterialTreeBuilder#chainEdges} (what the live RecipeMap rows actually do), the
 * byproduct side column merges the builder's cross-material edges (the derived face) with
 * the DECLARED face — {@code OreDictMaterial.mByProducts} keyed on the crushing target
 * ({@link GT6RecipesOreChain#crushingTarget}, the upstream Loader_OreProcessing.java:311
 * redirect: Fe ore shows the Fe2O3 page). ponytail note: the declared face is read straight
 * off the material data here; it may migrate to OreByproductInfo once debt-byproducts-data
 * merges (equivalent refactor, zero behavior change — that class is a thin query model over
 * the same three public seams this card consumes).
 *
 * <p><b>Table-driven layout, not hand-drawn</b> (the anti-GTCEu-static-template ruling):
 * {@link #COLUMN_TABLE} is the single per-prefix column-position constant table — every
 * displayed node sits in the column its prefix maps to, rows stack top-down per column in
 * BFS encounter order. Prefixes with no table entry are NOT displayed (the table is also
 * the ore-chain filter: the item-tree tails like ingot/plate never leak into the diagram).
 * The layout adapts to the live maps for free: when the pooled producer rows LANDED (task
 * debt-ore-purified-edge, the :351 DUST_ORE sifting walk) crushedPurified simply occupied its
 * table column — the table only positions nodes, the maps decide which exist.
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

	// ---- the layout table (the 布局坐标表 constants) ------------------
	//
	// ROW/COLUMN SEMANTIC MAP (the rotation, task mattree-vertical-layout):
	//
	//   pre-rotation (horizontal)                    post-rotation (vertical)
	//   -------------------------                    -----------------------
	//   processing stage = grid column,              processing stage = band row,
	//     x = COLUMN_X[c], left→right,                 y = STAGE_Y[c], top→down,
	//     COLUMN_PITCH 40                              STAGE_PITCH 40
	//   parallel nodes = rows within a column,       parallel nodes = lanes within a band,
	//     y = ROW_Y0 + r*20, ROW_PITCH 20              x = LANE_X0 + r*28, LANE_PITCH 28
	//     (18 px slot + 2 px gap = the 挤)             (18 px slot + 10 px gap)
	//   byproduct side column rides RIGHT,           byproduct band rides BOTTOM,
	//     slots stack DOWN                             slots spread RIGHT
	//
	// Node.column() keeps naming the SEMANTIC stage (the COLUMN_TABLE lookup and the whole
	// three-fold display filter are UNTOUCHED — the coverage card owns those); Node.row()
	// keeps naming the parallel index. Only the coordinate functions below rotate.

	public static final int COL_ORE = 0, COL_CRUSHED = 1, COL_PURIFIED = 2, COL_DUST = 3, COL_BYPRODUCT = 4;

	/**
	 * The band pitch, VERTICAL since the rotation (task mattree-vertical-layout): 18 px slot
	 * + a 22 px gap BELOW each band that budgets exactly 2 px entry wire + 16 px machine icon
	 * + 4 px arrowhead (the rotated v2 three-part edge, spec clause ② — the machine rides the
	 * gap under the from band). Unchanged value from the horizontal COLUMN_PITCH: that axis
	 * was already loose (the r11 census: 「列距40已松」); the squeeze lived in the old
	 * ROW_PITCH, now the horizontal LANE_PITCH.
	 */
	public static final int STAGE_PITCH = 40;

	/**
	 * The lane pitch, HORIZONTAL since the rotation (task mattree-vertical-layout): 18 px
	 * slot + 10 px gap — the spec's ≥28 floor, killing the old ROW_PITCH 20 (2 px gap) that
	 * was the 「挤的太紧」 root cause. Lanes carry no machine icons (chain hops always
	 * advance stage bands), so 10 px of visual air is the whole budget.
	 */
	public static final int LANE_PITCH = 28;

	/** y of each stage band's slot row (stages grow DOWN; the material-name header sits above). */
	static final int[] STAGE_Y = {16, 56, 96, 136, 176};
	/** x of the first lane (parallel nodes spread RIGHT from here). */
	public static final int LANE_X0 = 4;
	/**
	 * Parallel-slot cap per stage band — EXPLICIT overflow since v2 (see {@link Overflow}),
	 * never silently dropped. RAISED 5 → 7 by the rotation (task mattree-vertical-layout
	 * cap re-evaluation): the old cap was a VERTICAL screen budget (a 128 px-tall canvas);
	 * rotated, that semantic moves to the HORIZONTAL width budget, where the new canvas is
	 * 202 px wide — under the old 228 px width both viewers already shipped. The live pour's
	 * chain columns top out at 5 reachable nodes (the ore* wildcard: oreRaw + the four
	 * DUST_ORE sifting inputs), so Fe/Cu censuses are unchanged; the byproduct-rich declared
	 * faces (Sn/Cassiterite, MT.java:3812-3813: nine declared each) truncate 5 → 7 shown
	 * instead of 5.
	 */
	public static final int MAX_ROWS = 7;
	/**
	 * The rotated canvas: 7 lanes at pitch 28 (the last slot's right edge + 12 px margin).
	 * The viewers both declare width/height freely (the "fixed 178x166" JEI lore is a
	 * misreading — GT6MaterialTreeJeiCategory has always shipped its own), and both stacks
	 * paginate tall categories natively (JEI RecipeGuiLayouts 1-per-page stacking, EMI
	 * RecipeTab per-recipe heights — the r11 vertical-support evidence).
	 */
	public static final int WIDTH = LANE_X0 + (MAX_ROWS - 1) * LANE_PITCH + 18 + 12;
	/** Last band's slot row plus the 10 px overflow-marker strip ({@link #overflowY}). */
	public static final int HEIGHT = STAGE_Y[COL_BYPRODUCT] + 18 + 12;
	/** The "Byproducts" band header rides the wire-free gap just ABOVE its band. */
	public static final int BYPRODUCT_HEADER_Y = STAGE_Y[COL_BYPRODUCT] - 11;

	/**
	 * The per-prefix column-position constant table (the 表驱动 layout, spec clause): the
	 * closed ore-chain prefixes get explicit entries; the OPEN {@code ore*} family
	 * (ore/oreRaw/orePoor/oreDense/…, the {@code startsWith("ore")} face
	 * MaterialTreeBuilder.isOreFamilyPrefix shares) is the table's one wildcard row.
	 */
	private static final Map<String, Integer> COLUMN_TABLE = new LinkedHashMap<>();
	static {
		COLUMN_TABLE.put("blockRaw", COL_ORE); // the OreChain :154 template's raw-block input form
		COLUMN_TABLE.put("crushed", COL_CRUSHED);
		COLUMN_TABLE.put("crushedTiny", COL_CRUSHED);
		COLUMN_TABLE.put("crushedPurified", COL_PURIFIED);
		COLUMN_TABLE.put("crushedPurifiedTiny", COL_PURIFIED);
		COLUMN_TABLE.put("crushedCentrifuged", COL_PURIFIED);
		COLUMN_TABLE.put("crushedCentrifugedTiny", COL_PURIFIED);
		COLUMN_TABLE.put("dust", COL_DUST);
		COLUMN_TABLE.put("dustSmall", COL_DUST);
		COLUMN_TABLE.put("dustTiny", COL_DUST);
		COLUMN_TABLE.put("dustDiv72", COL_DUST); // the ShCL :167-172 chance-row outputs
	}

	/** The table lookup: explicit entry first, then the {@code ore*} wildcard; -1 = not displayed. */
	public static int columnOf(OreDictPrefix aPrefix) {
		Integer tEntry = COLUMN_TABLE.get(aPrefix.mNameInternal);
		if (tEntry != null) return tEntry.intValue();
		if (aPrefix.mNameInternal.startsWith("ore")) return COL_ORE;
		return -1;
	}

	/** The production item resolver: the registered (prefix, material) handle, or null. */
	public static final BiFunction<OreDictPrefix, OreDictMaterial, Item> STANDARD_ITEMS = (aPrefix, aMaterial) -> {
		var tHandle = GTMaterialItems.get(aPrefix, aMaterial); // RegistryObject/DeferredHolder per leg — both expose .get()
		return tHandle == null ? null : tHandle.get();
	};

	// ---- the model ----------------------------------------------------

	/**
	 * The v2 node roles (spec clause ①): grid {@link Node}s riding the columns are
	 * {@code ITEM}s; the machine-icon nodes that split every chain edge into
	 * 「from –线– [机器] –箭头→ to」 are carried on {@link Edge#machine()} and positioned by
	 * {@link MaterialTreeLayout} — the MACHINE role names the face, the edge owns the data
	 * (keeps {@link #nodes()} the pure column census the existing pins read).
	 */
	public enum Role { ITEM, MACHINE }

	/** One displayed slot: the prefix node at its table position with its resolved stack. */
	public record Node(OreDictPrefix prefix, int column, int row, ItemStack stack, Role role) {
		/** The grid (ITEM) node form — the role-less call sites all mean {@link Role#ITEM}. */
		public Node(OreDictPrefix aPrefix, int aColumn, int aRow, ItemStack aStack) {
			this(aPrefix, aColumn, aRow, aStack, Role.ITEM);
		}
	}

	/**
	 * One displayed chain hop: both endpoints displayed, carrying the producing maps' internal
	 * names and the v2 machine face — {@code machine} is the representative machine stack
	 * resolved off {@link #mapNames()} through {@link MaterialTreeWorkstations#workstationStack}
	 * (first resolvable name wins; the rest ride the machine node's tooltip), EMPTY = the
	 * degrade face (plain arrow, via-label text, spec clause ②'s fallback).
	 */
	public record Edge(OreDictPrefix from, OreDictPrefix to, List<String> mapNames, ItemStack machine) {
		/** The label for the machine tooltip / the degraded via-text ("via Shredder/Anvil"). */
		public String viaLabel() { return MaterialTreeDisplay.viaLabel(mapNames()); }
	}

	/**
	 * One byproduct side-column slot: derived (a real row's cross-material output) or declared.
	 *
	 * <p>{@code step} is the producing PROCESS STEP's band (task mattree-r2-byproduct-per-step):
	 * the band the producing row's material-scoped input sits in — the machine icon of that row
	 * rides the gap under exactly this band, so 「第 n 步的副产」 = the {@code step == n} slots.
	 * The derived face derives it from the row's input prefix ({@code columnOf(inPrefix)}); the
	 * declared face is the upstream crushing redirect, which fires on the ore family
	 * ({@code COL_ORE}). {@code -1} = the producing input is OUTSIDE the display table (an
	 * item-tree tail row) — the slot belongs to no displayed step, so only the
	 * {@link gregtech6.recipes.tree.MaterialTreeLayout.ByproductView#ALL} aggregate face shows it.
	 */
	public record Byproduct(ItemStack stack, boolean derived, String sourceLabel, int step) {}

	/**
	 * The explicit overflow marker (the v2 no-silent-drop clause): {@code hidden} prefixes of
	 * a chain column (or byproduct slots in the side column) that did not fit under
	 * {@link #MAX_ROWS}. The viewers render "+N" at ({@link #overflowX}, {@link #overflowY});
	 * v1 just dropped them (MaterialTreeDisplay :234 of the old world).
	 */
	public record Overflow(int column, int hidden) {}

	/**
	 * x of a node's slot (the parallel lane: lanes grow RIGHT).
	 *
	 * <p><b>The legacy fixed-grid view</b> (task mattree-r2-layout-engine): the layout
	 * ENGINE face is {@link MaterialTreeLayout#plan} — its {@code Result} computes these
	 * coordinates from a {@code Policy} and derives the canvas from the content. This
	 * static table (and {@code WIDTH/HEIGHT}) survives as the VIEWER PAGES' published
	 * geometry (the EMI/JEI page legs, the nav tables); under the default policy the two
	 * are numerically identical — the equality is pinned by {@code MaterialTreeLayoutTest}.
	 * The standalone screen consumes the engine face.
	 */
	public static int nodeX(Node aNode) { return LANE_X0 + aNode.row() * LANE_PITCH; }
	/** y of a node's slot (the stage band: stages grow DOWN). */
	public static int nodeY(Node aNode) { return STAGE_Y[aNode.column()]; }
	/** x of a byproduct slot (lane aIndex of the bottom band). */
	public static int byproductX(int aIndex) { return LANE_X0 + aIndex * LANE_PITCH; }
	/** y of the byproduct band's slot row. */
	public static int byproductY() { return STAGE_Y[COL_BYPRODUCT]; }
	/** y of a stage band's slot row (the rotated columnX: stage depth = downward y). */
	public static int stageY(int aStage) { return STAGE_Y[aStage]; }
	/** x of a stage band's "+N" overflow marker (under the band's first lane). */
	public static int overflowX() { return LANE_X0; }
	/** y of a stage band's "+N" overflow marker (the explicit-not-silent clause, per band). */
	public static int overflowY(int aStage) { return STAGE_Y[aStage] + 20; }

	public final OreDictMaterial material;
	private final List<Node> mNodes;
	private final List<Edge> mEdges;
	private final List<Byproduct> mByproducts;
	private final List<Overflow> mOverflow;

	private MaterialTreeDisplay(OreDictMaterial aMaterial, List<Node> aNodes, List<Edge> aEdges, List<Byproduct> aByproducts, List<Overflow> aOverflow) {
		material = aMaterial;
		mNodes = aNodes;
		mEdges = aEdges;
		mByproducts = aByproducts;
		mOverflow = aOverflow;
	}

	/** The displayed nodes (column order, then BFS row order). */
	public List<Node> nodes() { return Collections.unmodifiableList(mNodes); }
	/** The displayed chain edges. */
	public List<Edge> edges() { return Collections.unmodifiableList(mEdges); }
	/** The merged two-face byproduct column. */
	public List<Byproduct> byproducts() { return Collections.unmodifiableList(mByproducts); }
	/** The explicit overflow bookkeeping (empty when the whole tree fit under the row caps). */
	public List<Overflow> overflow() { return Collections.unmodifiableList(mOverflow); }

	/** The localized producing-map label ({@code gt.recipe.shredder} → "Shredder"), internal name as fallback. */
	public static String mapLabel(String aMapName) {
		RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(aMapName);
		return tMap == null ? aMapName : tMap.mNameLocal;
	}

	/** The merged via-label ("via Shredder/Anvil") — one source for both viewers' machine tooltips and degraded edges. */
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

	/** {@link #buildAll(MaterialTreeBuilder, BiFunction)} with the v2 machine-stack resolver seam (offline tests inject non-registry stacks). */
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
	 * The one material's display with the v2 machine resolver seam: {@code aMachines} maps a
	 * RecipeMap internal name to its representative machine stack (EMPTY = untabled/unbound —
	 * the edge degrades to a plain arrow). Offline tests inject probe stacks here; production
	 * resolves through {@link MaterialTreeWorkstations#workstationStack}.
	 */
	public static MaterialTreeDisplay of(MaterialTreeBuilder aTree, OreDictMaterial aMaterial, BiFunction<OreDictPrefix, OreDictMaterial, Item> aItems, Function<String, ItemStack> aMachines) {
		Set<OreDictPrefix> tReachable = aTree.reachableFromOre(aMaterial);
		if (tReachable.isEmpty()) return null;

		// nodes: table-filtered, item-resolved, row-stacked per column (cap MAX_ROWS with
		// EXPLICIT overflow bookkeeping — the v2 no-silent-drop clause)
		Map<Integer, Integer> tRowPerColumn = new LinkedHashMap<>();
		int[] tHidden = new int[COL_BYPRODUCT + 1];
		Map<OreDictPrefix, Node> tNodeByPrefix = new LinkedHashMap<>();
		List<Node> tNodes = new ArrayList<>();
		for (OreDictPrefix tPrefix : tReachable) {
			int tColumn = columnOf(tPrefix);
			if (tColumn < 0) continue; // outside the ore-chain table — the item-tree tail does not leak in
			int tRow = tRowPerColumn.getOrDefault(tColumn, 0);
			if (tRow >= MAX_ROWS) { tHidden[tColumn]++; continue; } // explicit "+N" marker, never silent
			Item tItem = aItems.apply(tPrefix, aMaterial);
			if (tItem == null) continue;
			tRowPerColumn.put(tColumn, tRow + 1);
			tNodes.add(new Node(tPrefix, tColumn, tRow, new ItemStack(tItem)));
			tNodeByPrefix.put(tPrefix, tNodes.get(tNodes.size() - 1));
		}
		if (tNodes.isEmpty()) return null;

		// edges: only hops whose both endpoints survived the table; labels merged per (from, to);
		// the machine face = the first mapName with a resolvable representative stack
		Map<OreDictPrefix, Map<OreDictPrefix, Set<String>>> tEdgeMaps = new LinkedHashMap<>();
		for (ChainEdge tChain : aTree.chainEdges(aMaterial)) {
			if (!tNodeByPrefix.containsKey(tChain.from()) || !tNodeByPrefix.containsKey(tChain.to())) continue;
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

		// byproducts: derived first (what the rows really emit), then the declared face
		// (the crushing target's mByProducts) minus what the derived face already shows —
		// over MAX_ROWS the excess is COUNTED (the side column's "+N" marker), not dropped;
		// each slot carries its producing step (the input band — per-step hang, task
		// mattree-r2-byproduct-per-step; the declaration is the crushing redirect = COL_ORE)
		List<Byproduct> tByproducts = new ArrayList<>();
		Set<OreDictMaterial> tShown = Collections.newSetFromMap(new IdentityHashMap<>());
		for (ByproductEdge tDerived : aTree.byproductEdges(aMaterial)) {
			if (tByproducts.size() >= MAX_ROWS) { tHidden[COL_BYPRODUCT]++; continue; }
			Item tItem = aItems.apply(tDerived.outPrefix(), tDerived.to());
			if (tItem == null || !tShown.add(tDerived.to())) continue;
			tByproducts.add(new Byproduct(new ItemStack(tItem), true, VIA_PREFIX + mapLabel(tDerived.mapName()), columnOf(tDerived.inPrefix())));
		}
		for (OreDictMaterial tDeclared : GT6RecipesOreChain.crushingTarget(aMaterial).mByProducts) {
			if (tByproducts.size() >= MAX_ROWS) { tHidden[COL_BYPRODUCT]++; continue; }
			if (tDeclared == null || tDeclared.mID <= 0 || !tShown.add(tDeclared)) continue;
			Item tItem = aItems.apply(OP.dust, tDeclared);
			if (tItem == null) continue;
			tByproducts.add(new Byproduct(new ItemStack(tItem), false, DECLARED_TEXT, COL_ORE));
		}

		List<Overflow> tOverflow = new ArrayList<>();
		for (int c = 0; c <= COL_BYPRODUCT; c++)
			if (tHidden[c] > 0) tOverflow.add(new Overflow(c, tHidden[c]));

		return new MaterialTreeDisplay(aMaterial, tNodes, tEdges, tByproducts, tOverflow);
	}
}
