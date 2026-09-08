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
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import net.minecraftforge.fluids.FluidStack;

/**
 * The CRUCIBLE_SMELTING/CRUCIBLE_ALLOYING map faces (task p26-crucible-physics-smeltery
 * acceptance): zero static rows on both maps, the findRecipe ON-DEMAND arm derives the
 * smelting row from the input's material data (dust iron → ingot iron, duration =
 * mMeltingPoint, EUt 0), and the alloying display rows synthesize off the material graph.
 */
public class GT6RecipeMapCrucibleTest extends GTRecipesOfflineTestBase {

	private static MaterialPrefixItem DUST_IRON, INGOT_IRON, DUST_AU, DUST_AG, INGOT_AU, INGOT_AG, INGOT_ELECTRUM;
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
		}
		if (r.prefix() == gregapi.data.OP.ingot) {
			if (r.material() == MT.Au) return INGOT_AU;
			if (r.material() == MT.Ag) return INGOT_AG;
			if (r.material() == MT.Electrum) return INGOT_ELECTRUM;
			if (r.material() == MT.Iron) return INGOT_IRON;
		}
		return null;
	}

	@BeforeAll
	static void boot() {
		GTMaterialItemsBoot.boot(); // the shared offline material universe + registry probes
		DUST_IRON = GTMaterialItemsBoot.probePrefix("p26cruc_probe_dust_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Iron));
		INGOT_IRON = GTMaterialItemsBoot.probePrefix("p26cruc_probe_ingot_iron", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Iron));
		DUST_AU = GTMaterialItemsBoot.probePrefix("p26cruc_probe_dust_au", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Au));
		DUST_AG = GTMaterialItemsBoot.probePrefix("p26cruc_probe_dust_ag", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.dust, MT.Ag));
		INGOT_AU = GTMaterialItemsBoot.probePrefix("p26cruc_probe_ingot_au", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Au));
		INGOT_AG = GTMaterialItemsBoot.probePrefix("p26cruc_probe_ingot_ag", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Ag));
		INGOT_ELECTRUM = GTMaterialItemsBoot.probePrefix("p26cruc_probe_ingot_electrum", () -> new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.ingot, MT.Electrum));
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
