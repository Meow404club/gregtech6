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

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import net.minecraftforge.fluids.FluidStack;

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
	private static final java.util.function.Function<GT6RecipeMapCrucible.MatRequest, ItemStack> sProbeMatResolver =
			r -> {
				MaterialPrefixItem tItem = lookup(r);
				return tItem == null || r.count() < 1 ? null : new ItemStack(tItem, (int)Math.min(64, r.count()));
			};

	private static MaterialPrefixItem lookup(GT6RecipeMapCrucible.MatRequest r) {
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
		GT6RecipeMaps.init();
		// the mat() seam rides the probe items (the intrusive-holder lesson: the live
		// GTMaterialItems index is empty offline); restored in @AfterAll
		GT6RecipeMapCrucible.sMatResolver = sProbeMatResolver;
	}

	@AfterAll
	static void restoreMatResolver() {
		GT6RecipeMapCrucible.sMatResolver = GT6RecipeMapCrucible.DEFAULT_MAT_RESOLVER;
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
		assertEquals(2, tRows.size(), "the dust row and the ingot row");
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
		assertEquals(2, tRows.size(), "the dust row and the ingot row");

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
		assertEquals(2, tRows.size());
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
		assertEquals(2, GT6RecipeMapCrucible.alloyingDisplayRows(MT.Electrum).size(),
				"the same shape without a hidden component builds — the gate is the component's");
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
			MaterialRegistry.INSTANCE.open();
			MT.init();
			gregapi.data.OP.init();
			MaterialRegistry.INSTANCE.close();
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
			net.minecraft.core.Registry.register(tRegistry, new ResourceLocation("gt6", aProbeId), rItem);
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
