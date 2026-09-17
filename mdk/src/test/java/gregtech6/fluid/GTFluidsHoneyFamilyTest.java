package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Honey + bee-row fluid family offline tests (task p31-bees-lv1 — the registration-row
 * assertions against the DECLARED values): the four honey-family
 * {@link GTFluids.ChemicalFluidSpec} rows (Loader_Fluids.java:572/:575/:577/:576 — the
 * Bumblelyzer accept set) and the seven bee-row dependency rows (Loader_Fluids.java:49/
 * :196/:201/:364/:627/:198 + FL.java:476 — the coordinator-approved SPEC deviation that
 * keeps the 20 comb centrifuge rows at zero skips). The live loop is the RCON chain
 * (offline cannot touch the Forge registries).
 */
public class GTFluidsHoneyFamilyTest extends GTOfflineTestBase {

	/** The four honey ids in declaration order — the Loader_Fluids.java :572/:575/:577/:576 order. */
	private static final List<String> HONEY_IDS = List.of("honey", "honeydew", "royal_jelly", "ambrosia");

	/** The seven bee-row dependency ids in declaration order — the class-doc :49/:196/:201/:364/:627/:198/:476 order. */
	private static final List<String> BEE_ROW_IDS = List.of(
			"dragon_breath", "concrete", "chocolate_molten", "ice", "soup_mushroom", "latex", "potion_harm_1");

	@Test
	public void theTablesCarryTheElevenRowsInDeclarationOrder() {
		assertEquals(HONEY_IDS, GTFluids.HONEY_FLUID_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(BEE_ROW_IDS, GTFluids.BEE_ROW_FLUID_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(HONEY_IDS.size(), GTFluids.HONEY_FLUIDS.size(), "the live registrations walk the same table");
		assertEquals(BEE_ROW_IDS.size(), GTFluids.BEE_ROW_FLUIDS.size(), "the live registrations walk the same table");
	}

	/** The per-fluid honey census — id / temperature / density / viscosity. */
	@Test
	public void honeyDeclaredValuesMatchTheUpstreamAnchors() {
		// {id, tempK, density, viscosity} — the FL.create literals (:572/:575 material rows,
		// :577/:576 the null-material 275 K rows over the honest defaults)
		Object[][] tCensus = {
			{"honey"       , 300, 1000, 1000}, // :572 — MT.Honey, 300 K
			{"honeydew"    , 300, 1000, 1000}, // :575 — MT.Honeydew, 300 K
			{"royal_jelly" , 275, 1000, 1000}, // :577 — the 275 K literal
			{"ambrosia"    , 275, 1000, 1000}, // :576 "potion.ambrosia" — the 275 K literal
		};
		for (Object[] tRow : tCensus) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.honeySpec((String)tRow[0]);
			assertEquals(tRow[1], tSpec.temperature(), tRow[0] + " temperature");
			assertEquals(tRow[2], tSpec.density(), tRow[0] + " density");
			assertEquals(tRow[3], tSpec.viscosity(), tRow[0] + " viscosity");
			assertEquals("fluid.gt6." + tRow[0], tSpec.descriptionId(), tRow[0] + " descriptionId");
		}
	}

	/** The per-fluid bee-row census — the density/luminosity/temperature literals verbatim + the state-2 gas flag. */
	@Test
	public void beeRowDeclaredValuesMatchTheUpstreamAnchors() {
		// {id, tempK, density, viscosity, lum, gas} — :49 the aState=2 STATE_GASEOUS row
		// (FL.java:1105 viscosity 200 + gaseous; the explicit setDensity(100) literal
		// overrides the state's −100 density carrier) + the setLuminosity(5) literal;
		// :196/:201/:364/:627/:198 the material-formula/1.0-default 1000s;
		// FL.java:476 the water-based potion carrier.
		Object[][] tCensus = {
			{"dragon_breath"   , 300,  100,  200, 5, true }, // :49 — the state-2 gaseous + density/lum literals
			{"concrete"        , 300, 1000, 1000, 0, false}, // :196 — 1.0 g/cm³ default formula
			{"chocolate_molten", 313, 1000, 1000, 0, false}, // :201 — the .heat(C+40) melting rule
			{"ice"             , 273, 1000, 1000, 0, false}, // :364 — the C literal; MT.Ice setDensity 1.0
			{"soup_mushroom"   , 300, 1000, 1000, 0, false}, // :627 "mushroomsoup"
			{"latex"           , 300, 1000, 1000, 0, false}, // :198 — DEF_ENV_TEMP
			{"potion_harm_1"   , 300, 1000, 1000, 0, false}, // FL.java:476 "potion.damage"
		};
		for (Object[] tRow : tCensus) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.beeRowSpec((String)tRow[0]);
			assertEquals(tRow[1], tSpec.temperature(), tRow[0] + " temperature");
			assertEquals(tRow[2], tSpec.density(), tRow[0] + " density");
			assertEquals(tRow[3], tSpec.viscosity(), tRow[0] + " viscosity");
			assertEquals(tRow[4], tSpec.luminosity(), tRow[0] + " luminosity");
			assertEquals(tRow[5], tSpec.gas(), tRow[0] + " state flag (dragon_breath rides the :49 aState=2 gaseous form)");
		}
	}

	/** Every honey/bee-row id sits under the 340 K wood-barrel ceiling — the barrel-transport census. */
	@Test
	public void everyRowRidesUnderTheWoodBarrelCeiling() {
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HONEY_FLUID_SPECS) assertTrue(tSpec.temperature() < 340, tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.BEE_ROW_FLUID_SPECS) assertTrue(tSpec.temperature() < 340, tSpec.name());
	}
}
