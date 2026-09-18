package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The behavior pin of the p31-supplier-rule-pin compressor fix: the :226 dust→plateGem row's
 * gem Nor gate actually EXCLUDES gem-chain members again. The eager
 * {@code GEM_CHAIN_PREFIXES} field froze four pre-init null prefixes at mod construct (before
 * {@code OP.init()}) and the probe silently passed every material; the fix reads the prefixes
 * at call time. Both tests self-select their materials from the pure registration walk
 * ({@link GTMaterialItems#registrationOrder()} — no item registry needed), so the control arm
 * runs on both legs; the exclusion arm needs a live item index for the probe to answer at
 * all, so it skips on vanilla-bootstrap legs (junit-fml-booted legs run it — the index is
 * populated there). The synthetic resolver is installed in both arms so the ONLY null path
 * left in {@code buildTemplateRecipe} is the gate itself — a null is attributable to the gem
 * exclusion, never to an unresolvable item.
 */
public class GT6GemGateExclusionTest extends GTRecipesOfflineTestBase {

	private static final Item SYNTH = Items.IRON_INGOT;

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;

	@BeforeAll
	static void bootMaterials() {
		GTMaterialItems.initMaterials(); // the offline material universe (the mdk bootstrap house rule)
		sDefaultMaterialResolver = GT6RecipesCompressor.sMaterialItemResolver;
	}

	@AfterEach
	void restoreResolver() {
		GT6RecipesCompressor.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipeMaps.reset();
		GT6RecipesCompressor.resetForTest();
	}

	/** The :226 easy-arm gem row (the dust→plateGem row carrying the Nor gate). */
	private static GT6RecipesCompressor.CompressTemplate easyGemRow() {
		return GT6RecipesCompressor.table().stream()
				.filter(aTemplate -> ":226".equals(aTemplate.note())).findFirst().orElseThrow();
	}

	/** The materials that generated an item on any of the four gem-chain prefixes (the pure walk, both legs). */
	private static Set<OreDictMaterial> gemChainMembers() {
		return GTMaterialItems.registrationOrder().stream()
				.filter(aPair -> aPair.prefix() == OP.gemLegendary || aPair.prefix() == OP.gemExquisite
						|| aPair.prefix() == OP.gemFlawless || aPair.prefix() == OP.bouleGt)
				.map(PrefixMaterial::material).collect(Collectors.toSet());
	}

	/** The gates BEFORE the gem probe in buildTemplateRecipe — members passing these reach the exclusion. */
	private static boolean reachesGemGate(OreDictMaterial aMaterial) {
		return (aMaterial.contains(TD.Processing.FURNACE) || aMaterial.contains(TD.Properties.SOFT)) // the easy arm
				&& !aMaterial.contains(TD.Atomic.ANTIMATTER) && !aMaterial.contains(TD.Compounds.COATED)
				&& !aMaterial.contains(TD.Compounds.LAYERED) && aMaterial != MT.Ice;
	}

	/** The fix's face: a gem-chain member is excluded from the :226 row (legs with a live item index). */
	@Test
	void gemChainMembersAreExcludedFromTheGemGateRow() {
		List<OreDictMaterial> tMembers = gemChainMembers().stream()
				.filter(GT6GemGateExclusionTest::reachesGemGate).toList();
		assumeFalse(tMembers.isEmpty(), "the offline universe carries gem-chain members");
		OreDictMaterial tMember = tMembers.get(0);
		// the probe reads the live item index — vanilla-bootstrap legs register no items, skip there
		assumeTrue(GTMaterialItems.get(OP.gemLegendary, tMember) != null || GTMaterialItems.get(OP.gemExquisite, tMember) != null
				|| GTMaterialItems.get(OP.gemFlawless, tMember) != null || GTMaterialItems.get(OP.bouleGt, tMember) != null,
				"the probe needs the live item index (FML-booted legs)");
		GT6RecipesCompressor.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTH;
		assertNull(GT6RecipesCompressor.buildTemplateRecipe(easyGemRow(), tMember),
				"the gem Nor gate excludes gem-chain members from the :226 dust→plateGem row "
				+ "(null-probe = the eager-field bug — every member silently poured)");
	}

	/** The control: a structurally non-gem easy material still pours the :226 row (both legs). */
	@Test
	void nonGemMaterialsStillPourTheGemGateRow() {
		Set<OreDictMaterial> tMembers = gemChainMembers();
		List<OreDictMaterial> tControls = GTMaterialItems.registrationOrder().stream()
				.filter(aPair -> aPair.prefix() == OP.dust)
				.map(PrefixMaterial::material)
				.filter(GT6GemGateExclusionTest::reachesGemGate)
				.filter(aMaterial -> !tMembers.contains(aMaterial))
				.distinct().limit(1).toList();
		assumeFalse(tControls.isEmpty(), "the offline universe carries non-gem easy materials with dust items");
		GT6RecipesCompressor.sMaterialItemResolver = (aPrefix, aMaterial) -> SYNTH;
		assertNotNull(GT6RecipesCompressor.buildTemplateRecipe(easyGemRow(), tControls.get(0)),
				"the gate only excludes gem-chain members — a plain easy material pours the :226 row");
	}
}
