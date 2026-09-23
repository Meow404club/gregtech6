package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.item.GT6Circuits;
import gregtech6.recipes.GT6RecipesWelder.WelderWallRow;

/**
 * The Welder wall rows in/out (task p29-w3-nbtdesign-parts ④ acceptance ⑤ + task
 * p35-crucible-wall-obtainability): the row table census (11 wall rows :1143-1153 + 11
 * dense rows :1155-1165 + the 8 DEDICATED crucible-wall rows :1143-1153 replayed onto the
 * port-side block twins, the EUt/duration columns verbatim), the REAL buildRecipe probe
 * contract — FOUR plates plus the config-10 selector circuit match, the wrong
 * configuration / the short plate stack refuse, the selector never consumed, ONE wall out
 * — and the resolver silent-skip (the FL.exists drop for the plate-less Galvanized Steel).
 */
public class GT6RecipesWelderRowTest {

	/** The vanilla stand-in for the selector circuit (LAZY — the Items class-load needs the bootstrap). */
	private static Item fixtureCircuit() {
		return Items.BARRIER;
	}

	/** The original never-consumed predicate (restored after each test — the seam is shared). */
	private static java.util.function.Predicate<ItemStack> sOriginalNotConsumable = null;

	private static Item fixtureWall() {
		return Items.IRON_BLOCK;
	}

	@BeforeAll
	static void injectOutputResolver() {
		// the vanilla boot BEFORE any Blocks/Items touch (the GTMultiBlocksOfflineTestBase order)
		try {
			net.minecraft.SharedConstants.tryDetectVersion();
		} catch (Throwable aIgnored) {
			// best effort
		}
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable aIgnored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now
		}
		// the fixture wall carrier (the Forge item registry cannot be written offline —
		// the output seam rides the same injection shape as the plate seam)
		GT6RecipesWelder.sOutputResolver = aPath -> fixtureWall();
		// the fixture selector carries the configuration on the Damage key — the
		// GT6Circuits.applyConfiguration 1.20.1 face (vanilla ItemStack:295-297)
		// the PUBLIC selector helper — the configuration carrier is leg-dependent
		// (1.20.1 Damage key / 1.21.1 CUSTOM_DATA envelope), the helper owns the fork
		GT6RecipesWelder.sSelectorResolver = aConfig -> GT6Circuits.selector(fixtureCircuit(), aConfig);
	}

	@BeforeEach
	void swapNotConsumableSeam() {
		// the never-consumed seam: the fixture circuit stands in for the IntegratedCircuitItem
		// identity (the GT6RecipesShCLTest synthetic-item convention — the vanilla item
		// registry freezes at bootstrap, so the real circuit item cannot be constructed here).
		// BEFORE EACH — the AfterEach restore hands the seam back to the production predicate.
		sOriginalNotConsumable = Recipe.sNotConsumable;
		Item tFixtureCircuit = fixtureCircuit();
		Recipe.sNotConsumable = aStack -> aStack.is(tFixtureCircuit) || sOriginalNotConsumable.test(aStack);
	}

	@AfterEach
	void restoreNotConsumable() {
		if (sOriginalNotConsumable != null) {
			Recipe.sNotConsumable = sOriginalNotConsumable;
			sOriginalNotConsumable = null;
		}
	}

	private static ItemStack fixturePlates(int aCount) {
		return new ItemStack(Items.IRON_INGOT, aCount);
	}

	@Test
	void theRowTableCoversAllThirtyWalls() {
		assertEquals(30, GT6RecipesWelder.table().size(), "11 metal walls + 11 dense walls + the 8 dedicated crucible walls (p35)");
		long tDense = GT6RecipesWelder.table().stream().filter(WelderWallRow::dense).count();
		assertEquals(11, tDense);
		Set<String> tPaths = new HashSet<>();
		for (WelderWallRow tRow : GT6RecipesWelder.table()) {
			assertTrue(tPaths.add(tRow.wallPath()), "no duplicate wall path: " + tRow.wallPath());
			// the material-name switch is total — an unknown path throws, so this must never throw
			assertNotNull(GT6RecipesWelder.materialNameOf(tRow.wallPath()), tRow.wallPath());
		}
		assertTrue(tPaths.contains("machine_wall_tungstensteel"));
		assertTrue(tPaths.contains("dense_wall_tungsten"), "18024 — the Dynamo emitter plate wall");
		// p35 — every dedicated crucible wall carries its welder row (the obtainability half)
		for (String tCrucibleWall : new String[] {"crucible_steel_wall", "crucible_stainless_steel_wall", "crucible_invar_wall",
				"crucible_titanium_wall", "crucible_tungstensteel_wall", "crucible_tungsten_wall",
				"crucible_tantalum_hafnium_carbide_wall", "crucible_adamantium_wall"}) {
			assertTrue(tPaths.contains(tCrucibleWall), "the dedicated crucible wall row: " + tCrucibleWall);
		}
	}

	@Test
	void theEnergyAndDurationColumnsAreUpstreamLiteral() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> fixturePlates(1);
		for (WelderWallRow tRow : GT6RecipesWelder.table()) {
			Recipe tRecipe = GT6RecipesWelder.buildRecipe(tRow);
			assertNotNull(tRecipe, tRow.wallPath());
			assertEquals(tRow.dense() ? 512 : 256, tRecipe.mDuration, tRow.wallPath() + " duration");
			assertEquals(tRow.dense() ? 64 : 16, tRecipe.mEUt, tRow.wallPath() + " eUt");
			assertFalse(tRecipe.mCanBeBuffered, "the addRecipe2(F, ...) first argument — UNBUFFERED");
		}
	}

	@Test
	void fourPlatesAndTheConfigTenSelectorMatch() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> fixturePlates(1);
		Recipe tRecipe = GT6RecipesWelder.buildRecipe(GT6RecipesWelder.table().get(0));
		assertNotNull(tRecipe);

		ItemStack tSelector10 = GT6Circuits.selector(fixtureCircuit(), GT6RecipesWelder.SELECTOR_CONFIG);
		ItemStack tPlate4 = fixturePlates(GT6RecipesWelder.PLATE_COUNT);

		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, tPlate4, tSelector10),
				"4 plates + the config-10 selector match the row");
		// the consume pass (aDecreaseStacksizeBySuccess = true)
		assertTrue(tRecipe.isRecipeInputEqual(true, false, null, tPlate4, tSelector10), "the consume pass succeeds too");
		assertEquals(0, tPlate4.getCount(), "the consume pass eats the plates");
		assertEquals(1, tSelector10.getCount(), "the selector survives — the circuit identity-skip");
	}

	@Test
	void theWrongSelectorConfigurationRefuses() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> fixturePlates(1);
		Recipe tRecipe = GT6RecipesWelder.buildRecipe(GT6RecipesWelder.table().get(0));
		assertNotNull(tRecipe);
		ItemStack tSelector9 = GT6Circuits.selector(fixtureCircuit(), 9);
		ItemStack tPlate4 = fixturePlates(GT6RecipesWelder.PLATE_COUNT);

		assertFalse(tRecipe.isRecipeInputEqual(false, false, null, tPlate4, tSelector9),
				"the ST.tag(10) row refuses a config-9 selector");
	}

	@Test
	void theWrongPlateMaterialRefuses() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> fixturePlates(1);
		Recipe tRecipe = GT6RecipesWelder.buildRecipe(GT6RecipesWelder.table().get(0));
		assertNotNull(tRecipe);
		ItemStack tSelector10 = GT6Circuits.selector(fixtureCircuit(), GT6RecipesWelder.SELECTOR_CONFIG);
		ItemStack tWrongPlate = new ItemStack(Items.GOLD_INGOT, GT6RecipesWelder.PLATE_COUNT);

		assertFalse(tRecipe.isRecipeInputEqual(false, false, null, tWrongPlate, tSelector10),
				"a different material's plates do not weld the wall");
	}

	@Test
	void theShortPlateStackRefuses() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> fixturePlates(1);
		Recipe tRecipe = GT6RecipesWelder.buildRecipe(GT6RecipesWelder.table().get(0));
		assertNotNull(tRecipe);
		ItemStack tSelector10 = GT6Circuits.selector(fixtureCircuit(), GT6RecipesWelder.SELECTOR_CONFIG);
		ItemStack tPlate2 = fixturePlates(2);

		assertFalse(tRecipe.isRecipeInputEqual(false, false, null, tPlate2, tSelector10),
				"the plate column is FOUR — two do not weld a wall");
	}

	@Test
	void theOutputIsOneWall() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> fixturePlates(1);
		Recipe tRecipe = GT6RecipesWelder.buildRecipe(GT6RecipesWelder.table().get(0));
		assertNotNull(tRecipe);
		assertEquals(1, tRecipe.mOutputs.length);
		assertEquals(fixtureWall(), tRecipe.mOutputs[0].getItem(), "the fixture wall carrier");
		assertEquals(1, tRecipe.mOutputs[0].getCount(), "one wall per weld");
	}

	@Test
	void theUnresolvablePlateSkipsSilently() {
		GT6RecipesWelder.sPlateResolver = (aPrefix, aMaterial) -> null; // the FL.exists null
		assertNull(GT6RecipesWelder.buildRecipe(GT6RecipesWelder.table().get(0)), "no plate item = no row (the silent skip)");
	}

	@Test
	void theMaterialSwitchRejectsUnknownPaths() {
		assertThrows(IllegalArgumentException.class, () -> GT6RecipesWelder.materialNameOf("not_a_wall"));
	}

	@Test
	void theMaterialNamesMatchTheLoaderColumns() {
		// p36 fix — the registered registry keys (mNameInternal), NOT the MT FIELD short
		// forms: OreDictMaterial.get resolves the former, MT.NULL on the latter
		assertEquals("Lead", GT6RecipesWelder.materialNameOf("machine_wall_lead"));
		assertEquals("Steel", GT6RecipesWelder.materialNameOf("machine_wall_steel"), "ANY.Steel's canonical name");
		assertEquals("Tungsten", GT6RecipesWelder.materialNameOf("dense_wall_tungsten"));
		assertEquals("TantalumHafniumCarbide", GT6RecipesWelder.materialNameOf("machine_wall_tantalum_hafnium_carbide"));
		assertEquals("SteelGalvanized", GT6RecipesWelder.materialNameOf("dense_wall_galvanized_steel"));
		// p35 — the dedicated crucible-wall twins ride the rung shell materials (:1270-1277)
		assertEquals("Steel", GT6RecipesWelder.materialNameOf("crucible_steel_wall"));
		assertEquals("StainlessSteel", GT6RecipesWelder.materialNameOf("crucible_stainless_steel_wall"));
		assertEquals("TungstenSteel", GT6RecipesWelder.materialNameOf("crucible_tungstensteel_wall"));
		assertEquals("TantalumHafniumCarbide", GT6RecipesWelder.materialNameOf("crucible_tantalum_hafnium_carbide_wall"));
	}

	/**
	 * The p36 resolution pin (the GT6CrucibleLadderCensusTest bootstrap shape): with the
	 * material registry booted, EVERY table row's name resolves to a REAL material (not
	 * MT.NULL) — the live pour face. The pre-p36 switch carried five MT FIELD short forms
	 * (Pb/Ti/W/Ta4HfC5/Ad) that silently dropped 14 rows through MT.NULL.
	 */
	@Test
	void everyRowMaterialResolvesInTheBootedRegistry() {
		gregapi.oredict.MaterialRegistry.INSTANCE.open();
		try {
			gregapi.data.MT.init();
		} catch (Throwable aIgnored) {
			// the best-effort offline boot (the census-test form)
		}
		try {
			for (WelderWallRow tRow : GT6RecipesWelder.table()) {
				gregapi.oredict.OreDictMaterial tMat = GT6RecipesWelder.sMaterialResolver.apply(GT6RecipesWelder.materialNameOf(tRow.wallPath()));
				assertNotNull(tMat, tRow.wallPath());
				assertTrue(tMat != gregapi.data.MT.NULL,
						tRow.wallPath() + " resolved to MT.NULL — the " + GT6RecipesWelder.materialNameOf(tRow.wallPath()) + " name does not resolve");
			}
		} finally {
			gregapi.oredict.MaterialRegistry.INSTANCE.close();
		}
	}
}
