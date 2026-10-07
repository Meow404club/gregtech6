package gregtech6.recipes.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.configurations.OreDictConfigurationComponent;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * The CRUCIBLE_SMELTING/CRUCIBLE_ALLOYING map faces (task crucible-physics-smeltery
 * acceptance): zero static rows on both maps, the findRecipe ON-DEMAND arm derives the
 * smelting row from the input's material data (dust iron → ingot iron, duration =
 * mMeltingPoint, EUt 0), and the alloying display rows synthesize off the material graph.
 */
public class GT6RecipeMapCrucibleTest extends GTRecipesOfflineTestBase {

	private static MaterialPrefixItem DUST_IRON, INGOT_IRON, DUST_AU, DUST_AG, INGOT_AU, INGOT_AG, INGOT_ELECTRUM;
	// the Invar/StainlessSteel component-closure probes (acceptance ②/③): WroughtIron, Ni,
	// Invar, Cr, Mn — enough that BOTH chains resolve offline (StainlessSteel = WroughtIron
	// 4U + Invar 3U + Cr 1U + Mn 1U, MT.java:2504)
	private static MaterialPrefixItem DUST_WI, INGOT_WI, DUST_NI, INGOT_NI, DUST_INV, INGOT_INV, DUST_CR, INGOT_CR, DUST_MN, INGOT_MN;
	// the hidden-component gate fixture (acceptance ⑤): a private-registry material with
	// RESOLVABLE probes, so the gate — not item resolution — is what kills the rows
	private static OreDictMaterial HIDDEN_COMP;
	private static MaterialPrefixItem DUST_HID, INGOT_HID;
	private static MaterialPrefixItem DUST_SS, INGOT_SS;
	// the crucible-smelting-page-fuller fixtures: the Fe :63-67 self-family probes and the
	// private-registry cross-source material (smelts INTO Iron) with its full :58-67 family
	private static OreDictMaterial SMELT_SOURCE;
	private static MaterialPrefixItem BLOCKDUST_IRON, CRUSHED_IRON, CRUSHEDP_IRON, CRUSHEDC_IRON;
	private static MaterialPrefixItem INGOT_SRC, BLOCKINGOT_SRC, GEM_SRC, BLOCKGEM_SRC, DUST_SRC, BLOCKDUST_SRC, CRUSHED_SRC, CRUSHEDP_SRC, CRUSHEDC_SRC;
	// the flux/Air card probes (task crucible-alloying-flux-rows): the flux pair
	// (Coal/Limestone), the C/CaCO3 component forms, and the Steel-family closure
	// (Steel, MeteoricIron, MeteoricSteel) for the real Air-config alloys
	private static MaterialPrefixItem DUST_C, INGOT_C, DUST_COAL, DUST_CACO3, INGOT_CACO3, DUST_LIME;
	private static MaterialPrefixItem DUST_STEEL, INGOT_STEEL, DUST_MI, INGOT_MI, DUST_MSTEEL, INGOT_MSTEEL;
	// the private-registry flux/Air fixtures: the output faces need their own probes
	private static OreDictMaterial FLUX_ALLOY, LIME_ALLOY, AIR_ALLOY;
	private static MaterialPrefixItem DUST_FLUX_ALLOY, INGOT_FLUX_ALLOY, DUST_LIME_ALLOY, INGOT_LIME_ALLOY, DUST_AIR_ALLOY, INGOT_AIR_ALLOY;
	private static final java.util.function.Function<GT6RecipeMapCrucible.MatRequest, ItemStack> sProbeMatResolver =
			r -> {
				MaterialPrefixItem tItem = lookup(r);
				return tItem == null || r.count() < 1 ? null : new ItemStack(tItem, (int)Math.min(64, r.count()));
			};

	private static MaterialPrefixItem lookup(GT6RecipeMapCrucible.MatRequest r) {
		if (r.material() == SMELT_SOURCE) { // the cross-source fixture answers its whole :58-67 family
			if (r.prefix() == gregapi.data.OP.ingot) return INGOT_SRC;
			if (r.prefix() == gregapi.data.OP.blockIngot) return BLOCKINGOT_SRC;
			if (r.prefix() == gregapi.data.OP.gem) return GEM_SRC;
			if (r.prefix() == gregapi.data.OP.blockGem) return BLOCKGEM_SRC;
			if (r.prefix() == gregapi.data.OP.dust) return DUST_SRC;
			if (r.prefix() == gregapi.data.OP.blockDust) return BLOCKDUST_SRC;
			if (r.prefix() == gregapi.data.OP.crushed) return CRUSHED_SRC;
			if (r.prefix() == gregapi.data.OP.crushedPurified) return CRUSHEDP_SRC;
			if (r.prefix() == gregapi.data.OP.crushedCentrifuged) return CRUSHEDC_SRC;
			return null;
		}
		if (r.prefix() == gregapi.data.OP.dust) {
			if (r.material() == MT.Au) return DUST_AU;
			if (r.material() == MT.Ag) return DUST_AG;
			if (r.material() == MT.Iron) return DUST_IRON;
			if (r.material() == MT.WroughtIron) return DUST_WI;
			if (r.material() == MT.Ni) return DUST_NI;
			if (r.material() == MT.Invar) return DUST_INV;
			if (r.material() == MT.Cr) return DUST_CR;
			if (r.material() == MT.Mn) return DUST_MN;
			if (r.material() == MT.StainlessSteel) return DUST_SS;
			if (r.material() == HIDDEN_COMP) return DUST_HID;
			if (r.material() == MT.C) return DUST_C;
			if (r.material() == MT.Coal) return DUST_COAL;
			if (r.material() == MT.CaCO3) return DUST_CACO3;
			if (r.material() == MT.STONES.Limestone) return DUST_LIME;
			if (r.material() == MT.Steel) return DUST_STEEL;
			if (r.material() == MT.MeteoricIron) return DUST_MI;
			if (r.material() == MT.MeteoricSteel) return DUST_MSTEEL;
			if (r.material() == FLUX_ALLOY) return DUST_FLUX_ALLOY;
			if (r.material() == LIME_ALLOY) return DUST_LIME_ALLOY;
			if (r.material() == AIR_ALLOY) return DUST_AIR_ALLOY;
		}
		if (r.material() == MT.Iron) { // the :63-67 self-family probes beyond plain dust
			if (r.prefix() == gregapi.data.OP.blockDust) return BLOCKDUST_IRON;
			if (r.prefix() == gregapi.data.OP.crushed) return CRUSHED_IRON;
			if (r.prefix() == gregapi.data.OP.crushedPurified) return CRUSHEDP_IRON;
			if (r.prefix() == gregapi.data.OP.crushedCentrifuged) return CRUSHEDC_IRON;
		}
		if (r.prefix() == gregapi.data.OP.ingot) {
			if (r.material() == MT.Au) return INGOT_AU;
			if (r.material() == MT.Ag) return INGOT_AG;
			if (r.material() == MT.Electrum) return INGOT_ELECTRUM;
			if (r.material() == MT.Iron) return INGOT_IRON;
			if (r.material() == MT.WroughtIron) return INGOT_WI;
			if (r.material() == MT.Ni) return INGOT_NI;
			if (r.material() == MT.Invar) return INGOT_INV;
			if (r.material() == MT.Cr) return INGOT_CR;
			if (r.material() == MT.Mn) return INGOT_MN;
			if (r.material() == MT.StainlessSteel) return INGOT_SS;
			if (r.material() == HIDDEN_COMP) return INGOT_HID;
			if (r.material() == MT.C) return INGOT_C;
			if (r.material() == MT.CaCO3) return INGOT_CACO3;
			if (r.material() == MT.Steel) return INGOT_STEEL;
			if (r.material() == MT.MeteoricIron) return INGOT_MI;
			if (r.material() == MT.MeteoricSteel) return INGOT_MSTEEL;
			if (r.material() == FLUX_ALLOY) return INGOT_FLUX_ALLOY;
			if (r.material() == LIME_ALLOY) return INGOT_LIME_ALLOY;
			if (r.material() == AIR_ALLOY) return INGOT_AIR_ALLOY;
		}
		return null;
	}

	@BeforeAll
	static void boot() {
		GTMaterialItemsBoot.boot(); // the shared offline material universe + registry probes
		DUST_IRON = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Iron));
		INGOT_IRON = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Iron));
		DUST_AU = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_au", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Au));
		DUST_AG = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_ag", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Ag));
		INGOT_AU = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_au", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Au));
		INGOT_AG = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_ag", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Ag));
		INGOT_ELECTRUM = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_electrum", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Electrum));
		// the acceptance-②/③ component closure (private-registry synthetic for the hidden
		// comp — zero touches on the shared MT singleton's MATERIAL_MAP)
		MaterialRegistry tPrivate = new MaterialRegistry();
		HIDDEN_COMP = tPrivate.createMaterial(-1, "CrucibleHiddenComp", "Crucible Hidden Comp").hide();
		DUST_WI = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_wrought_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.WroughtIron));
		INGOT_WI = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_wrought_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.WroughtIron));
		DUST_NI = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_ni", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Ni));
		INGOT_NI = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_ni", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Ni));
		DUST_INV = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_invar", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Invar));
		INGOT_INV = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_invar", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Invar));
		DUST_CR = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_cr", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Cr));
		INGOT_CR = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_cr", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Cr));
		DUST_MN = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_mn", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Mn));
		INGOT_MN = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_mn", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Mn));
		DUST_HID = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_hidden", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, HIDDEN_COMP));
		INGOT_HID = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_hidden", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, HIDDEN_COMP));
		DUST_SS = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_stainless_steel", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.StainlessSteel));
		INGOT_SS = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_stainless_steel", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.StainlessSteel));
		// the crucible-smelting-page-fuller cross-source fixture: a private-registry material
		// that smelts INTO Iron (mTargetedSmelting drives MaterialGraph.targeting regardless
		// of registry), MELTING-tagged so the :87 gate passes, heated away from 1000 so the
		// SpecialValue pin is non-trivial
		MaterialRegistry tSourceRegistry = new MaterialRegistry();
		SMELT_SOURCE = tSourceRegistry.createMaterial(-1, "CrucibleSmeltSource", "Crucible Smelt Source")
				.put(gregapi.data.TD.Processing.MELTING).setSmelting(MT.Iron, gregapi.data.CS.U).heat(1234);
		BLOCKDUST_IRON = GTMaterialItemsBoot.probePrefix("crucible_probe_blockdust_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.blockDust, MT.Iron));
		CRUSHED_IRON = GTMaterialItemsBoot.probePrefix("crucible_probe_crushed_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.crushed, MT.Iron));
		CRUSHEDP_IRON = GTMaterialItemsBoot.probePrefix("crucible_probe_crushedp_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.crushedPurified, MT.Iron));
		CRUSHEDC_IRON = GTMaterialItemsBoot.probePrefix("crucible_probe_crushedc_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.crushedCentrifuged, MT.Iron));
		INGOT_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_ingot", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, SMELT_SOURCE));
		BLOCKINGOT_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_blockingot", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.blockIngot, SMELT_SOURCE));
		GEM_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_gem", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.gem, SMELT_SOURCE));
		BLOCKGEM_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_blockgem", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.blockGem, SMELT_SOURCE));
		DUST_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_dust", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, SMELT_SOURCE));
		BLOCKDUST_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_blockdust", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.blockDust, SMELT_SOURCE));
		CRUSHED_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_crushed", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.crushed, SMELT_SOURCE));
		CRUSHEDP_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_crushedp", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.crushedPurified, SMELT_SOURCE));
		CRUSHEDC_SRC = GTMaterialItemsBoot.probePrefix("crucible_probe_src_crushedc", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.crushedCentrifuged, SMELT_SOURCE));
		// the flux/Air card probes (task crucible-alloying-flux-rows)
		DUST_C = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_c", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.C));
		INGOT_C = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_c", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.C));
		DUST_COAL = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_coal", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Coal));
		DUST_CACO3 = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_caco3", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.CaCO3));
		INGOT_CACO3 = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_caco3", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.CaCO3));
		DUST_LIME = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_limestone", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.STONES.Limestone));
		DUST_STEEL = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_steel", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Steel));
		INGOT_STEEL = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_steel", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Steel));
		DUST_MI = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_meteoric_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.MeteoricIron));
		INGOT_MI = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_meteoric_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.MeteoricIron));
		DUST_MSTEEL = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_meteoric_steel", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.MeteoricSteel));
		INGOT_MSTEEL = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_meteoric_steel", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.MeteoricSteel));
		// the private-registry flux/Air fixtures (same posture as the hidden-comp fixture:
		// zero touches on the shared MT singleton's MATERIAL_MAP) — the output faces need
		// probes or the rows die at the output null-guard before the asserts run
		MaterialRegistry tFixtures = new MaterialRegistry();
		FLUX_ALLOY = tFixtures.createMaterial(-1, "CrucibleFluxAlloy", "Crucible Flux Alloy")
				.setMcfg(0, MT.Iron, CS.U, MT.C, CS.U).alloyCentrifuge();
		LIME_ALLOY = tFixtures.createMaterial(-1, "CrucibleLimeAlloy", "Crucible Lime Alloy")
				.setMcfg(0, MT.Iron, CS.U, MT.CaCO3, CS.U).alloyCentrifuge();
		AIR_ALLOY = tFixtures.createMaterial(-1, "CrucibleAirAlloy", "Crucible Air Alloy")
				.setMcfg(0, MT.Iron, CS.U);
		// the MT.java:4118 verbatim shape (port) / MT.java:3348 (upstream): WroughtIron +
		// Air → Steel is the ONLY Air-config alloy family in both sources
		AIR_ALLOY.addAlloyingRecipe(new OreDictConfigurationComponent(1, new OreDictMaterialStack(MT.Iron, CS.U), new OreDictMaterialStack(MT.Air, CS.U)));
		DUST_FLUX_ALLOY = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_flux_alloy", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, FLUX_ALLOY));
		INGOT_FLUX_ALLOY = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_flux_alloy", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, FLUX_ALLOY));
		DUST_LIME_ALLOY = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_lime_alloy", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, LIME_ALLOY));
		INGOT_LIME_ALLOY = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_lime_alloy", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, LIME_ALLOY));
		DUST_AIR_ALLOY = GTMaterialItemsBoot.probePrefix("crucible_probe_dust_air_alloy", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, AIR_ALLOY));
		INGOT_AIR_ALLOY = GTMaterialItemsBoot.probePrefix("crucible_probe_ingot_air_alloy", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, AIR_ALLOY));
		GT6RecipeMaps.reset(); // hermetic: retire boot/sibling generations first (task hermetic-pour-tests)
		GT6RecipeMaps.init();
		// the mat() seam rides the probe items (the intrusive-holder lesson: the live
		// GTMaterialItems index is empty offline); restored in @AfterAll
		GT6RecipeMapCrucible.sMatResolver = sProbeMatResolver;
		// the Air display seam rides a stand-in fluid: the port has NO gt6:air fluid yet
		// (the census precheck miss), so the live walk answers null and the fluid-face
		// asserts below could not pin the mB math; restored in @AfterAll
		GT6RecipeMapCrucible.sAirDisplayFluid = () -> Fluids.WATER;
	}

	@AfterAll
	static void restoreMatResolver() {
		GT6RecipeMapCrucible.sMatResolver = GT6RecipeMapCrucible.DEFAULT_MAT_RESOLVER;
		GT6RecipeMapCrucible.sAirDisplayFluid = GT6RecipeMapCrucible.DEFAULT_AIR_DISPLAY_FLUID;
	}

	@AfterEach
	void keepGeneration() {
		// the maps stay alive for the other test classes; only THIS class asserts on them
		assertNotNull(GT6RecipeMaps.CRUCIBLE_SMELTING);
	}

	/** RM.java:129 constants — the 6/6/1 item face, minimal inputs 0, power 1. */
	@Test
	public void crucibleSmeltingConstants() {
		RecipeMap tMap = GT6RecipeMaps.CRUCIBLE_SMELTING;
		assertSame(tMap, RecipeMap.RECIPE_MAPS.get("gt.recipe.cruciblesmelting"));
		assertEquals("Crucible Smelting", tMap.mNameLocal);
		assertEquals(6, tMap.mInputItemsCount);
		assertEquals(6, tMap.mOutputItemsCount);
		assertEquals(1, tMap.mMinimalInputItems);
		assertEquals(0, tMap.mMinimalInputs);
		assertEquals(1, tMap.mPower);
	}

	/** RM.java:128 constants — the 12/12/1 Combination Smelting face. */
	@Test
	public void crucibleAlloyingConstants() {
		RecipeMap tMap = GT6RecipeMaps.CRUCIBLE_ALLOYING;
		assertSame(tMap, RecipeMap.RECIPE_MAPS.get("gt.recipe.cruciblealloying"));
		assertEquals("Combination Smelting", tMap.mNameLocal);
		assertEquals(12, tMap.mInputItemsCount);
		assertEquals(12, tMap.mOutputItemsCount);
		assertEquals(1, tMap.mMinimalInputItems);
	}

	/** The zero-static-rows acceptance: both maps carry an EMPTY recipe list. */
	@Test
	public void bothMapsAreZeroStaticRow() {
		assertTrue(GT6RecipeMaps.CRUCIBLE_SMELTING.mRecipeList.isEmpty(), "the smelting map derives, it never stores");
		assertTrue(GT6RecipeMaps.CRUCIBLE_ALLOYING.mRecipeList.isEmpty(), "the alloying map synthesizes, it never stores");
	}

	/** The findRecipe on-demand arm: 2 dust iron → 2 ingot iron, duration = Fe.mMeltingPoint, EUt 0. */
	@Test
	public void findRecipeOnDemandIronDustToIngot() {
		assertTrue(MT.Iron.contains(gregapi.data.TD.Processing.MELTING), "precondition: iron is meltable");
		ItemStack tInput = new ItemStack(DUST_IRON, 2);

		Recipe tRecipe = GT6RecipeMaps.CRUCIBLE_SMELTING.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], tInput);
		assertNotNull(tRecipe, "the on-demand arm must derive the row");
		// findRecipe is LOOKUP ONLY — the input stack must survive
		assertEquals(2, tInput.getCount());

		assertEquals(1, tRecipe.mInputs.length);
		assertEquals(1, tRecipe.mInputs[0].getCount(), "the row consumes ONE item per process");
		assertSame(DUST_IRON, tRecipe.mInputs[0].getItem());

		assertEquals(1, tRecipe.mOutputs.length);
		assertSame(INGOT_IRON, tRecipe.mOutputs[0].getItem(), "ingotOrDust answers the ingot");
		assertEquals(2, tRecipe.mOutputs[0].getCount(), "two dusts = two ingots (dust mAmount = U)");

		assertEquals(MT.Iron.mMeltingPoint, tRecipe.mDuration, "the duration IS the melting point");
		assertEquals(0, tRecipe.mEUt, "the crucible heats with raw HU — the row carries no power");
		assertFalse(tRecipe.mCanBeBuffered, "one-time rows never cache (the :96 F)");
	}

	/** The material gates: a non-MELTING material and a data-less vanilla item derive nothing. */
	@Test
	public void findRecipeGates() {
		// no material data → null (the :84 drop)
		assertNull(GT6RecipeMaps.CRUCIBLE_SMELTING.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], new ItemStack(Items.IRON_INGOT, 1)));
		// vanilla iron ore rides the BE's VANILLA_ORES bridge, not the RM face — null here
		assertNull(GT6RecipeMaps.CRUCIBLE_SMELTING.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], new ItemStack(Items.IRON_ORE, 1)));
	}

	/** The alloying display synthesis: Electrum (Au+Ag halves) builds two fake rows with the temperature special. */
	@Test
	public void alloyingDisplayRowsSynthesizeFromTheGraph() {
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(MT.Electrum);
		// phase-neutral pair invariant: the upstream postInit pass (GT_API_Post.java:820)
		// re-adds mComponents verbatim with no dedup, so RUNTIME walks every simple alloy's
		// config twice (two identical pairs — the documented verbatim quirk,
		// MaterialGraph.applyCrucibleAlloyReferences). The forge offline leg answers
		// pre-postInit (2 rows), the neo mod-flood leg post-postInit (4) — the CONTENT pins
		// below ride the FIRST pair, identical in both phases.
		assertTrue(tRows.size() >= 2 && tRows.size() % 2 == 0, "whole dust/ingot pairs, at least one");
		for (Recipe tRow : tRows) {
			assertTrue(tRow.mFakeRecipe, "display rows never enter the findable list");
			assertEquals(1, tRow.mOutputs.length);
			assertEquals(MT.Electrum.mComponents.getCommonDivider(), tRow.mOutputs[0].getCount(), "commonDivider units of alloy per round");
			// :478-481 — the special value is the second-highest component melting point at least
			long tExpectedSpecial = Math.max(
					MT.Au.mMeltingPoint >= MT.Ag.mMeltingPoint ? MT.Ag.mMeltingPoint : MT.Au.mMeltingPoint,
					MT.Electrum.mMeltingPoint);
			assertEquals(tExpectedSpecial, tRow.mSpecialValue);
		}
		// the dust row carries the component dusts, the ingot row the component ingots
		// (Electrum = uumAloy(0, Ag 1U, Au 1U) — the dataset component order puts Ag first)
		assertSame(DUST_AG, tRows.get(0).mInputs[0].getItem());
		assertSame(DUST_AU, tRows.get(0).mInputs[1].getItem());
		assertSame(INGOT_AG, tRows.get(1).mInputs[0].getItem());
		assertSame(INGOT_AU, tRows.get(1).mInputs[1].getItem());
	}

	/** The plain (non-alloy) material and null have no display rows. */
	@Test
	public void alloyingDisplayRowsRejectPlainMaterials() {
		assertTrue(GT6RecipeMapCrucible.alloyingDisplayRows(MT.Iron).isEmpty(), "iron has no alloy composition");
		assertTrue(GT6RecipeMapCrucible.alloyingDisplayRows(null).isEmpty());
	}

	/**
	 * Acceptance ② — the Invar chain builds rows with the UNEVEN split: WroughtIron 2U +
	 * Ni 1U (MT.java:2498, the MaterialGraphTest:292 chain) → dust row inputs [WI dust ×2,
	 * Ni dust ×1], output = commonDivider(3) ingots of Invar.
	 */
	@Test
	public void invarAlloyingRowsCarryTheUnevenComponentSplit() {
		assertEquals(3, MT.Invar.mComponents.getCommonDivider(), "WroughtIron 2U + Ni 1U = 3U (MT.java:2498)");
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(MT.Invar);
		// phase-neutral (the GT_API_Post.java:820 config re-add doubles the pair at runtime)
		assertTrue(tRows.size() >= 2 && tRows.size() % 2 == 0, "whole pairs, at least one");

		Recipe tDustRow = tRows.get(0);
		assertEquals(2, tDustRow.mInputs.length);
		assertSame(DUST_WI, tDustRow.mInputs[0].getItem());
		assertEquals(2, tDustRow.mInputs[0].getCount(), "WroughtIron rides 2U");
		assertSame(DUST_NI, tDustRow.mInputs[1].getItem());
		assertEquals(1, tDustRow.mInputs[1].getCount(), "Ni rides 1U");
		assertEquals(3, tDustRow.mOutputs[0].getCount(), "commonDivider units of alloy per round");
		assertSame(INGOT_INV, tDustRow.mOutputs[0].getItem());

		Recipe tIngotRow = tRows.get(1);
		assertSame(INGOT_WI, tIngotRow.mInputs[0].getItem());
		assertSame(INGOT_NI, tIngotRow.mInputs[1].getItem());

		// :476-481 — second-highest component melting point, at least the alloy's own
		long tSecondHighest = Math.min(MT.WroughtIron.mMeltingPoint, MT.Ni.mMeltingPoint);
		assertEquals(Math.max(tSecondHighest, MT.Invar.mMeltingPoint), tDustRow.mSpecialValue);
	}

	/**
	 * Acceptance ③ — the StainlessSteel four-component face (WroughtIron 4U + Invar 3U +
	 * Cr 1U + Mn 1U, MT.java:2504) builds both rows with ALL components as inputs and the
	 * 9U commonDivider output.
	 */
	@Test
	public void stainlessSteelAlloyingRowsBuildAllFourComponents() {
		assertEquals(9, MT.StainlessSteel.mComponents.getCommonDivider(), "4U + 3U + 1U + 1U = 9U (MT.java:2504)");
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(MT.StainlessSteel);
		// phase-neutral (the GT_API_Post.java:820 config re-add doubles the mComponents pair
		// at runtime; the :4123 Nichrome config has no offline item on either leg)
		assertTrue(tRows.size() >= 2 && tRows.size() % 2 == 0, "whole pairs, at least one");
		Recipe tDustRow = tRows.get(0);
		assertEquals(4, tDustRow.mInputs.length, "every component gets its input slot");
		java.util.Map<OreDictMaterial, Integer> tInputCounts = new java.util.HashMap<>();
		for (ItemStack tInput : tDustRow.mInputs)
			tInputCounts.put(((MaterialPrefixItem)tInput.getItem()).material, tInput.getCount());
		assertEquals(java.util.Map.of(MT.WroughtIron, 4, MT.Invar, 3, MT.Cr, 1, MT.Mn, 1), tInputCounts,
				"the MT.java:2504 split, order-free: WroughtIron 4U + Invar 3U + Cr 1U + Mn 1U");
		assertEquals(9, tDustRow.mOutputs[0].getCount(), "9U = 9 ingots per round");
		assertSame(INGOT_SS, tDustRow.mOutputs[0].getItem(), "the output is the alloy's own ingot");
	}

	/**
	 * Acceptance ⑤ — the GT6_Main.java:460 hidden gate: a hidden component kills the whole
	 * alloy row pair, even though its items resolve. The open twin (same shape, no hidden
	 * component) still builds — the gate keys on the COMPONENT's mHidden, not the alloy's.
	 */
	@Test
	public void alloyingDisplayRowsSkipHiddenComponents() {
		assertTrue(HIDDEN_COMP.mHidden, "precondition: the fixture comp is hidden");
		MaterialRegistry tPrivate = new MaterialRegistry();
		OreDictMaterial tHiddenAlloy = tPrivate.createMaterial(-1, "CrucibleHiddenAlloy", "Crucible Hidden Alloy")
				.setMcfg(0, MT.Iron, gregapi.data.CS.U, HIDDEN_COMP, gregapi.data.CS.U).alloyCentrifuge();
		assertTrue(GT6RecipeMapCrucible.alloyingDisplayRows(tHiddenAlloy).isEmpty(),
				"a hidden component skips the whole row pair (GT6_Main.java:460)");
		// the open control rides the LIVE Electrum (probes resolve, no hidden comp): the
		// empty result above comes from the GATE, not from row synthesis being broken
		// (phase-neutral: the postInit config re-add changes the COUNT, never the non-emptiness)
		assertFalse(GT6RecipeMapCrucible.alloyingDisplayRows(MT.Electrum).isEmpty(),
				"the same shape without a hidden component builds — the gate is the component's");
	}

	/**
	 * Acceptance (crucible-alloying-flux-rows) — the GT6_Main.java:467/:480-481 flux third
	 * row: a C component displays Coal at twice its amount on a THIRD fake row INSTEAD of
	 * its own dust (the :474 else-arm skip), while the dust/ingot pair keeps the component
	 * forms; a configuration without C carries no flux row on ANY of its pairs.
	 */
	@Test
	public void alloyingFluxThirdRowForCarbonComponent() {
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(FLUX_ALLOY);
		assertEquals(3, tRows.size(), "the dust row, the ingot row and the Coal flux row");
		Recipe tFluxRow = tRows.get(2);
		assertTrue(tFluxRow.mFakeRecipe, "the flux row is display-only like its siblings");
		assertEquals(2, tFluxRow.mInputs.length, "Iron's dust (the :474 else-arm) plus Coal (C's :467 flux)");
		assertSame(DUST_IRON, tFluxRow.mInputs[0].getItem());
		assertSame(DUST_COAL, tFluxRow.mInputs[1].getItem(), "the C component rides Coal on the flux row");
		assertEquals(2, tFluxRow.mInputs[1].getCount(), "Coal rides mAmount*2 (the :467 literal)");
		// the dust/ingot rows keep the component's own forms — the flux replaces it on row three only
		assertEquals(2, tRows.get(0).mInputs.length);
		assertSame(DUST_C, tRows.get(0).mInputs[1].getItem());
		// the flux row shares the pair's output and temperature face (:481 = :478 verbatim)
		assertSame(tRows.get(0).mOutputs[0].getItem(), tFluxRow.mOutputs[0].getItem());
		assertEquals(tRows.get(0).mSpecialValue, tFluxRow.mSpecialValue);
		// the no-C control rides the LIVE Electrum: phase-neutral (the postInit config
		// re-add, GT_API_Post.java:820, carries no C either) — NO row of ANY pair shows Coal
		assertTrue(GT6RecipeMapCrucible.alloyingDisplayRows(MT.Electrum).stream()
				.noneMatch(r -> java.util.Arrays.stream(r.mInputs).anyMatch(s -> s.getItem() == DUST_COAL)),
				"no C → no flux row on any pair");
	}

	/** The :468 twin: a CaCO3 component displays STONES.Limestone at twice its amount. */
	@Test
	public void alloyingFluxThirdRowForCalciumCarbonateComponent() {
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(LIME_ALLOY);
		assertEquals(3, tRows.size(), "the dust row, the ingot row and the Limestone flux row");
		Recipe tFluxRow = tRows.get(2);
		assertEquals(2, tFluxRow.mInputs.length);
		assertSame(DUST_LIME, tFluxRow.mInputs[1].getItem(), "the CaCO3 component rides STONES.Limestone (the :468 literal)");
		assertEquals(2, tFluxRow.mInputs[1].getCount(), "Limestone rides mAmount*2");
		assertSame(DUST_CACO3, tRows.get(0).mInputs[1].getItem(), "the dust row keeps the CaCO3 dust itself");
	}

	/**
	 * Acceptance — the GT6_Main.java:461-466 Air component face: the pair SURVIVES (the
	 * pre-fix :211 null-guard family killed whole pairs over unrepresentable components),
	 * Air contributes NO item input and NO melting point, and the Air fluid stack (the
	 * :462 mB form) rides every row's fluid face. The fluid itself is a stand-in — the
	 * port has no gt6:air fluid yet (the census precheck miss); the live seam answers null
	 * and the row renders the remaining component inputs alone until a fluids card lands.
	 */
	@Test
	public void alloyingAirComponentRidesFluidFaceNotPairKill() {
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(AIR_ALLOY);
		assertEquals(2, tRows.size(), "the Air component no longer kills the pair");
		Recipe tDustRow = tRows.get(0), tIngotRow = tRows.get(1);
		assertEquals(1, tDustRow.mInputs.length, "Air contributes no item input (the :466 continue)");
		assertSame(DUST_IRON, tDustRow.mInputs[0].getItem(), "the only item input is Iron — Air rides the fluid face");
		assertSame(INGOT_IRON, tIngotRow.mInputs[0].getItem());
		assertSame(INGOT_AIR_ALLOY, tDustRow.mOutputs[0].getItem(), "the output is the alloy itself");
		for (Recipe tRow : tRows) {
			assertEquals(1, tRow.mFluidInputs.length, "the Air fluid stack rides the row's fluid face (:461-465)");
			assertEquals(1000, tRow.mFluidInputs[0].getAmount(), "UT.Code.units(1U, U, 1000, round) = 1000 mB (the :462 literal)");
			assertSame(Fluids.WATER, tRow.mFluidInputs[0].getFluid(), "the stand-in fluid of the sAirDisplayFluid seam");
		}
		// the :465 continue sits BEFORE the :470 melting-point add — Air contributes none,
		// so the single-point face answers the alloy's own temperature (:478 else-arm)
		assertEquals(AIR_ALLOY.mMeltingPoint, tDustRow.mSpecialValue);
	}

	/**
	 * Acceptance — the real Air-config alloys, the ONLY two in either source (port
	 * MT.java:4118-4119 = upstream MT.java:3348-3349, verified pair by pair): the
	 * Steel/MeteoricSteel display rows surface WITH the Air fluid face. Pre-fix the walk
	 * never saw the creation configuration (the mComponents walk) and the :211 family
	 * killed unrepresentable components — neither the pair nor its fluid face existed.
	 */
	@Test
	public void steelFamilyAirConfigPairsRestored() {
		List<Recipe> tSteel = GT6RecipeMapCrucible.alloyingDisplayRows(MT.Steel);
		assertFalse(tSteel.isEmpty(), "Steel builds rows");
		assertTrue(tSteel.size() % 2 == 0, "whole dust/ingot pairs only");
		assertTrue(tSteel.stream().anyMatch(r -> r.mFluidInputs != null && r.mFluidInputs.length > 0),
				"the Steel WroughtIron+Air configuration pair renders its Air fluid face (MT.java:4118)");
		List<Recipe> tMeteoric = GT6RecipeMapCrucible.alloyingDisplayRows(MT.MeteoricSteel);
		assertFalse(tMeteoric.isEmpty(), "MeteoricSteel builds rows");
		assertTrue(tMeteoric.size() % 2 == 0, "whole dust/ingot pairs only");
		assertTrue(tMeteoric.stream().anyMatch(r -> r.mFluidInputs != null && r.mFluidInputs.length > 0),
				"the MeteoricSteel MeteoricIron+Air configuration pair renders its Air fluid face (MT.java:4119)");
	}

	/**
	 * Acceptance ⑥ — the smelting display face carries the upstream NEI SpecialValue: the
	 * row's SpecialValue IS the SOURCE material's mMeltingPoint (RecipeMapCrucible.java:96,
	 * rendered as "Temperature: N K") and its duration is 0 — the live findRecipe arm keeps
	 * duration = mMeltingPoint for the machine, the DISPLAY layer re-pins the pair. The Fe
	 * self row rides the all-face walk (the upstream :66-67 self arm sits OUTSIDE the
	 * tMat != self skip — dust iron → ingot iron is THE crucible page row).
	 */
	@Test
	public void smeltingDisplayRowsCarryTheMeltingPointSpecial() {
		List<Recipe> tRows = GT6RecipeMapCrucible.allSmeltingDisplayRows();
		Recipe tIronRow = tRows.stream().filter(r -> r.mInputs.length > 0 && r.mInputs[0].getItem() == DUST_IRON).findFirst()
				.orElseThrow(() -> new AssertionError("the Fe self row must exist in the full smelting walk"));
		assertEquals(MT.Iron.mMeltingPoint, tIronRow.mSpecialValue, "the Fe row's temperature IS Fe.mMeltingPoint");
		assertTrue(tIronRow.mSpecialValue > 0, "SpecialValue is a melting point, never 0 (acceptance ⑥)");
		assertEquals(0, tIronRow.mDuration, "the display row carries no duration (the :96 pair)");
		assertTrue(tIronRow.mFakeRecipe, "display rows never enter the findable list");
	}

	/**
	 * The crucible-smelting-page-fuller cross-source parity pin: ONE source material yields
	 * the FULL upstream RecipeMapCrucible.java:58-67 family — nine rows, one per prefix, in
	 * the verbatim upstream order — each row a fake display row smelting into the output's
	 * ingot with the SOURCE material's melting point as the temperature special (:96).
	 * Negative pin in the same walk: the eight no-item upstream prefixes (:68-75 chunk …
	 * reduced, no items upstream either per Loader_Items.java:57-171) render NOWHERE.
	 */
	@Test
	public void crossSourceRowsCarryTheFullUpstreamPrefixFamily() {
		assertTrue(SMELT_SOURCE.contains(gregapi.data.TD.Processing.MELTING), "precondition: the fixture passes the :87 MELTING gate");
		List<Recipe> tRows = GT6RecipeMapCrucible.smeltingDisplayRows(MT.Iron);
		gregapi.oredict.OreDictPrefix[] tExpected = {gregapi.data.OP.ingot, gregapi.data.OP.blockIngot, gregapi.data.OP.gem, gregapi.data.OP.blockGem,
				gregapi.data.OP.dust, gregapi.data.OP.blockDust, gregapi.data.OP.crushed, gregapi.data.OP.crushedPurified, gregapi.data.OP.crushedCentrifuged};
		assertEquals(9, tRows.size(), "one row per :58-67 prefix, no others resolve");
		for (int i = 0; i < tExpected.length; i++) {
			MaterialPrefixItem tItem = (MaterialPrefixItem) tRows.get(i).mInputs[0].getItem();
			assertSame(tExpected[i], tItem.prefix, "row " + i + " rides the verbatim :58-67 prefix order");
			assertSame(SMELT_SOURCE, tItem.material, "row " + i + " inputs the source material");
			assertSame(INGOT_IRON, tRows.get(i).mOutputs[0].getItem(), "row " + i + " smelts into the Fe ingot");
			assertEquals(SMELT_SOURCE.mMeltingPoint, tRows.get(i).mSpecialValue, "row " + i + " temperature = the SOURCE's melting point (:96)");
			assertEquals(0, tRows.get(i).mDuration, "row " + i + " is a display-row re-pin (duration 0)");
			assertTrue(tRows.get(i).mFakeRecipe, "row " + i + " never enters the findable list");
		}
		for (Recipe tRow : tRows) {
			gregapi.oredict.OreDictPrefix tPrefix = ((MaterialPrefixItem) tRow.mInputs[0].getItem()).prefix;
			assertFalse(tPrefix == gregapi.data.OP.chunk || tPrefix == gregapi.data.OP.rubble || tPrefix == gregapi.data.OP.pebbles
					|| tPrefix == gregapi.data.OP.cluster || tPrefix == gregapi.data.OP.cleanGravel || tPrefix == gregapi.data.OP.dirtyGravel
					|| tPrefix == gregapi.data.OP.crystalline || tPrefix == gregapi.data.OP.reduced,
					"the :68-75 no-item prefixes stay unrendered (upstream null-drops them too)");
		}
	}

	/**
	 * The self-arm half of the fuller card: Iron's own rows carry the FULL :63-67 dust
	 * family — five forms, verbatim upstream order — not just plain dust.
	 */
	@Test
	public void selfRowsCarryTheFullDustFamily() {
		List<Recipe> tRows = GT6RecipeMapCrucible.allSmeltingDisplayRows().stream()
				.filter(r -> r.mInputs.length > 0 && ((MaterialPrefixItem) r.mInputs[0].getItem()).material == MT.Iron).toList();
		gregapi.oredict.OreDictPrefix[] tExpected = {gregapi.data.OP.dust, gregapi.data.OP.blockDust, gregapi.data.OP.crushed, gregapi.data.OP.crushedPurified, gregapi.data.OP.crushedCentrifuged};
		assertEquals(5, tRows.size(), "the Fe self family: dust + the four :64-67 forms");
		for (int i = 0; i < tExpected.length; i++) {
			MaterialPrefixItem tItem = (MaterialPrefixItem) tRows.get(i).mInputs[0].getItem();
			assertSame(tExpected[i], tItem.prefix, "self row " + i + " rides the verbatim :63-67 prefix order");
			assertSame(INGOT_IRON, tRows.get(i).mOutputs[0].getItem(), "self row " + i + " smelts into the Fe ingot");
			assertEquals(MT.Iron.mMeltingPoint, tRows.get(i).mSpecialValue);
		}
	}

	/**
	 * The page-size multiplier pin (census acceptance): with this class's probe set the
	 * offline smelting page is EXACTLY 27 rows — 5 Fe full-family self forms + 8 dust-only
	 * self materials (Au Ag WI Ni Invar Cr Mn SS, one form each; the hidden fixture stays
	 * gated, the cross-source fixture lives in a private registry so it contributes no self
	 * rows) + 9 cross-source rows (the fixture family into Fe, reached through Iron's
	 * mTargetedSmelting regardless of the fixture's registry) + 5 self rows from the
	 * flux/Air card's dust+ingot probe materials (C CaCO3 Steel MeteoricIron MeteoricSteel —
	 * each carries the MELTING gate; the Coal/Limestone dust-only probes stay out, no ingot
	 * face and no MELTING tag). Any walk change that silently adds or drops rows breaks
	 * this number.
	 */
	@Test
	public void smeltingPageSizeMultiplierPinned() {
		assertEquals(27, GT6RecipeMapCrucible.allSmeltingDisplayRows().size(),
				"5 Fe self + 8 dust-only self + 9 cross-source + 5 flux/Air-card probe materials");
	}

	/**
	 * Acceptance ⑦ — the JEMI red line's row parity: both viewer legs register from the ONE
	 * rowsOf seam, so the JEI face (rowsOf as-is) and the EMI face (its own rowsOf copy,
	 * re-sorted — order never changes the count) carry the SAME row universe, and that
	 * universe is exactly the data-face walk (nothing dropped, nothing doubled). The
	 * acceptance-① chain surfaces through the seam: the Electrum pair rides the page.
	 * Offline caveat, pinned deliberately: OreDictMaterial.ALLOYS is completed by
	 * applyCrucibleAlloyReferences at FML setup (GT6Mod.java:72), which tests don't run —
	 * the offline walk therefore sees only the MT.init()-era ALLOYS members, so no
	 * absolute count is pinned here; Invar/StainlessSteel rows are pinned per-alloy above
	 * (they read mComponents directly) and the runtime universe is the same walk.
	 */
	@Test
	public void viewerLegsRowParityOnBothCrucibleMaps() {
		for (RecipeMap tMap : new RecipeMap[] {GT6RecipeMaps.CRUCIBLE_SMELTING, GT6RecipeMaps.CRUCIBLE_ALLOYING}) {
			List<Recipe> tJeiFace = gregtech6.jei.GT6RecipeMapViewerMeta.rowsOf(tMap);
			List<Recipe> tEmiFace = new java.util.ArrayList<>(gregtech6.jei.GT6RecipeMapViewerMeta.rowsOf(tMap));
			tEmiFace.sort(java.util.Comparator.comparing(r -> String.valueOf(r.mSpecialValue))); // any deterministic order
			assertEquals(tJeiFace.size(), tEmiFace.size(), tMap.mNameInternal + ": the legs' row counts are pinned equal");
		}
		assertEquals(GT6RecipeMapCrucible.allSmeltingDisplayRows().size(),
				gregtech6.jei.GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_SMELTING).size(),
				"the smelting page registers the whole data-face walk");
		assertEquals(GT6RecipeMapCrucible.allAlloyingDisplayRows().size(),
				gregtech6.jei.GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_ALLOYING).size(),
				"the alloying page registers the whole data-face walk");
		// the acceptance-① chain surfaces through the seam
		assertTrue(gregtech6.jei.GT6RecipeMapViewerMeta.rowsOf(GT6RecipeMaps.CRUCIBLE_ALLOYING).size() >= 2,
				"the Electrum pair (or any probed alloy's pair) rides the registered page");
	}

	/**
	 * The offline boot + probe-item helper (the GT6RecipeTagFallbackTest posture): the
	 * probe ids are throwaway registry names local to this test class.
	 */
	static final class GTMaterialItemsBoot {
		static void boot() {
			SharedConstants.tryDetectVersion();
			try {
				Bootstrap.bootStrap();
			} catch (Throwable ignored) {
				// NetworkHooks.init() failure is expected offline; registries are ready by now.
			}
			GT6MaterialTestSupport.materials(); // the hermetic bracket: reset FIRST, then the full refill (task hermetic-pour-tests)
		}

		static MaterialPrefixItem probePrefix(String aProbeId, java.util.function.Supplier<MaterialPrefixItem> aCreator) {
			var tRegistry = BuiltInRegistries.ITEM;
			//? if forge {
			try {
				// the Forge runtime shape: THREE locks must open (the GT6RecipeTagFallbackTest
				// walk — the vanilla frozen flag, the delegate ForgeRegistry.isFrozen, the
				// NamespacedWrapper.locked register gate)
				java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(tRegistry);
			} catch (Exception aE) {
				throw new IllegalStateException("could not unfreeze the offline item registry", aE);
			}
			try {
				java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
				tDelegate.setAccessible(true);
				Object tForgeRegistry = tDelegate.get(tRegistry);
				java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
				tForgeUnfreeze.setAccessible(true);
				tForgeUnfreeze.invoke(tForgeRegistry);
			} catch (NoSuchFieldException | NoSuchMethodException ignored) {
				// the 21.1 face: no forge delegate behind the vanilla registry
			} catch (Exception aE) {
				throw new IllegalStateException("could not open the offline forge registry", aE);
			}
			try {
				java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
				tLocked.setBoolean(tRegistry, false);
			} catch (NoSuchFieldException ignored) {
				// the 21.1 face: nothing but the vanilla frozen flag to unlock
			} catch (Exception aE) {
				throw new IllegalStateException("could not clear the offline registry lock", aE);
			}
			//?} else {
			/*try {
				// the 21.1 runtime shape: the plain vanilla DefaultedMappedRegistry — a single
				// frozen flag guards both the intrusive-holder construction and Registry.register
				java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(tRegistry);
			} catch (Exception aE) {
				throw new IllegalStateException("could not clear the offline registry lock", aE);
			}
			*///?}
			MaterialPrefixItem rItem = aCreator.get();
			net.minecraft.core.Registry.register(tRegistry, ResourceLocation.fromNamespaceAndPath("gt6", aProbeId), rItem);
			return rItem;
		}

		private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
			for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
				try {
					java.lang.reflect.Field rField = tClass.getDeclaredField(aName);
					rField.setAccessible(true);
					return rField;
				} catch (NoSuchFieldException ignored) {}
			}
			throw new NoSuchFieldException(aName);
		}
	}
}
