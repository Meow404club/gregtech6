package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.jei.GT6EnergyCensus;
import gregtech6.jei.GT6RecipeMapIcons;
import gregtech6.jei.GT6RecipeMapViewerMeta;

/**
 * The EMI face of the energy-source page (task energy-page-emi-twin, the E4 leg of the
 * energy-source-page wave) — the native twin of the JEI leg's energy category (the JEMI
 * red line: a gt6 namespace face exists on both viewers), the one-category singleton
 * form of the {@link GT6OreGenInfoEmiCategory} precedent. One recipe page per carrier
 * chunk-run: the ten carriers of {@link GT6EnergyCensus#carriers()} (the nine
 * ENERGY_BY_MAP ones + STEAM), each page carrying the carrier's produce / consume /
 * convert families off the census.
 *
 * <p><b>THE DOUBLE HOOK</b> (the E1 red-proof fix, this card's red line): the pages are
 * NOT {@code EmiInfoRecipe}s — EMI's {@code EmiApi.displayRecipes} indexes recipes by
 * OUTPUT only (EmiRecipes' byOutput map is built from getOutputs; byInput is read only
 * by displayUses), so a pseudo-carrier recipe mounted as INPUT-only is a silent no-op.
 * {@link GT6EnergyInfoEmiRecipe} mirrors the EmiInfoRecipe double mount (getInputs AND
 * getOutputs return the pseudo carrier, {@code supportsRecipeTree()=false}) — which is
 * exactly why the gear-port arm {@link GT6EmiPlugin#displayEnergyCarrierInfo} keeps
 * working with zero code change: {@code EmiApi.displayRecipes(pseudoStack)} now finds
 * these pages through byOutput. Retirement of the {@code EmiInfoRecipe} energy instances
 * and this registration ship in the same commit (no double-category window).
 */
public final class GT6EnergyInfoEmiCategory extends EmiRecipeCategory {

	/** The one category instance (the GT6OreGenInfoEmiCategory.CATEGORY form). */
	public static final GT6EnergyInfoEmiCategory CATEGORY = new GT6EnergyInfoEmiCategory();

	/** The grid columns of one family section (the approved page IA: 8 columns x 3 rows = 24). */
	public static final int COLUMNS = 8;

	/** The per-section chunk cap — exceeding it spills the family onto the next page recipe. */
	public static final int PAGE_CELLS = COLUMNS * 3;

	private GT6EnergyInfoEmiCategory() {
		// the icon is the EU pseudo carrier — the domain marker (the ore-gen catalyst form)
		super(idOf(), GT6EnergyCarrierEmiStack.of(TD.Energy.EU));
	}

	/** {@code gt6:energy_info} — mirrors the JEI twin's category uid one-to-one (the JEMI skip key). */
	public static ResourceLocation idOf() {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", "energy_info");
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "energy_info");
		 *///?}
	}

	/** The category + every carrier's page run (the GT6OreGenInfoEmiCategory.register shape). */
	public static void register(EmiRegistry aRegistry) {
		aRegistry.addCategory(CATEGORY);
		for (TagData tCarrier : GT6EnergyCensus.carriers()) {
			for (GT6EnergyInfoEmiRecipe tPage : pagesOf(tCarrier)) {
				aRegistry.addRecipe(tPage);
			}
		}
	}

	/** The page run of one live carrier (the census-fed registration face). */
	public static List<GT6EnergyInfoEmiRecipe> pagesOf(TagData aCarrier) {
		GT6EnergyCensus.Families tFamilies = GT6EnergyCensus.familiesOf(aCarrier);
		return pagesOf(aCarrier, tFamilies.produce(), tFamilies.consume(), tFamilies.convert());
	}

	/**
	 * The page splitter: each family chunks at {@link #PAGE_CELLS} — the consume census
	 * records (one per map, carrying the map's whole workstation list) flatten into
	 * per-machine cells first, because the approved grid unit is the MACHINE (the design's
	 * "43 台", hover = machine name, click = its map). Chunks are emitted round-robin per
	 * chunk index (produce k, consume k, convert k) so page 1 reproduces the approved
	 * wireframe (header + produce + consume + converters + transfer) whenever the three
	 * first chunks fit, and the sections pack greedily into the fixed page budget — a
	 * page never exceeds the canvas whatever the census grows to. Every carrier gets at
	 * least one page, unconditionally (empty families render the empty state — an empty
	 * run would silently never open the screen).
	 */
	static List<GT6EnergyInfoEmiRecipe> pagesOf(TagData aCarrier, List<GT6EnergyCensus.Machine> aProduce,
			List<GT6EnergyCensus.Consumer> aConsume, List<GT6EnergyCensus.Converter> aConvert) {
		List<GT6EnergyInfoEmiRecipe.ConsumerCell> tCells = new ArrayList<>();
		for (GT6EnergyCensus.Consumer tConsumer : aConsume) {
			for (GT6RecipeMapIcons.Workstation tWs : tConsumer.workstations()) {
				tCells.add(new GT6EnergyInfoEmiRecipe.ConsumerCell(tConsumer.map(), tWs));
			}
		}
		List<List<GT6EnergyCensus.Machine>> tProduce = chunks(aProduce);
		List<List<GT6EnergyInfoEmiRecipe.ConsumerCell>> tConsume = chunks(tCells);
		List<List<GT6EnergyCensus.Converter>> tConvert = chunks(aConvert);
		int tMax = Math.max(1, Math.max(tProduce.size(), Math.max(tConsume.size(), tConvert.size())));

		List<GT6EnergyInfoEmiRecipe.Section> tQueue = new ArrayList<>();
		if (aProduce.isEmpty()) tQueue.add(GT6EnergyInfoEmiRecipe.produceEmpty());
		for (int i = 0; i < tMax; i++) {
			if (i < tProduce.size()) tQueue.add(GT6EnergyInfoEmiRecipe.produce(tProduce.get(i)));
			if (i < tConsume.size()) tQueue.add(GT6EnergyInfoEmiRecipe.consume(tConsume.get(i)));
			if (i < tConvert.size()) tQueue.add(GT6EnergyInfoEmiRecipe.convert(tConvert.get(i)));
		}
		tQueue.add(GT6EnergyInfoEmiRecipe.transfer());

		List<List<GT6EnergyInfoEmiRecipe.Section>> tPages = new ArrayList<>();
		List<GT6EnergyInfoEmiRecipe.Section> tCurrent = new ArrayList<>();
		int tUsed = 0;
		int tBudget = GT6EnergyInfoEmiRecipe.HEIGHT - GT6EnergyInfoEmiRecipe.BODY_BUDGET;
		for (GT6EnergyInfoEmiRecipe.Section tSection : tQueue) {
			int tHeight = tSection.height();
			// two rules push a section onto a fresh page: the canvas budget, and the
			// same-family rule — two chunks of one family never share a page (the approved
			// grid form is 8x3 per family per page; a merged 4-row grid is not the shape)
			boolean tSameFamily = !tCurrent.isEmpty()
					&& tCurrent.get(tCurrent.size() - 1).mKind() == tSection.mKind();
			if (!tCurrent.isEmpty() && (tSameFamily || tUsed + tHeight > tBudget)) {
				tPages.add(tCurrent);
				tCurrent = new ArrayList<>();
				tUsed = 0;
			}
			tCurrent.add(tSection);
			tUsed += tHeight;
		}
		if (!tCurrent.isEmpty()) tPages.add(tCurrent);

		List<GT6EnergyInfoEmiRecipe> rPages = new ArrayList<>();
		for (int i = 0; i < tPages.size(); i++) {
			rPages.add(new GT6EnergyInfoEmiRecipe(aCarrier, tPages.get(i), i, tPages.size()));
		}
		return List.copyOf(rPages);
	}

	private static <T> List<List<T>> chunks(List<T> aCells) {
		List<List<T>> rChunks = new ArrayList<>();
		for (int i = 0; i < aCells.size(); i += PAGE_CELLS) {
			rChunks.add(aCells.subList(i, Math.min(aCells.size(), i + PAGE_CELLS)));
		}
		return rChunks;
	}

	/** The {@code gt6:energy_info/<code>/<page>} id tail — short-code derived, stable across sessions. */
	static ResourceLocation pageId(TagData aCarrier, int aPage) {
		String tCode = GT6RecipeMapViewerMeta.energyTypeShortCode(aCarrier).toLowerCase(java.util.Locale.ROOT);
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", "energy_info/" + tCode + "/" + aPage);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "energy_info/" + tCode + "/" + aPage);
		 *///?}
	}

	@Override
	public net.minecraft.network.chat.Component getName() {
		// ponytail: literal — the approved lang key set (E3 card) has no category-title key;
		// a translatable would render the raw key. Add the key in the lang wave if wanted.
		return net.minecraft.network.chat.Component.literal("Energy Sources");
	}
}
