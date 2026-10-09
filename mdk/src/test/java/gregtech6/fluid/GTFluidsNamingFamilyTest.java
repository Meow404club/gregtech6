package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Naming-parity fluid family offline tests (task fluids-naming): the THIRTY-FOUR
 * {@link GTFluids.ChemicalFluidSpec} rows of {@link GTFluids#NAMING_FLUID_SPECS} — the
 * census-gap GT-owned {@code FL.create} rows of the upstream Loader_Fluids.java walk
 * (753 lines section-by-section, state research.p37-fluids-naming-census), asserted
 * against the DECLARED values the way the chemical/hot family tests pin theirs. Every
 * temperature/density/viscosity/luminosity/gas literal carries its upstream line anchor;
 * the densities over material rows ride the :1128-1130 {@code (long)(1000·g)} formula
 * transcribed through the live vendored MT (the uumMcfg recompute chain,
 * OreDictMaterial.java:386-410 — glass 2332, molten_hsla 7874, sugar 27221, rubber 11335,
 * mercury 13533, mcguffium 3122). The three upstream alias spellings stay unported per
 * the single-name ruling. The zh faces ride the reference table's hand layer (dump faces
 * verbatim, tmp/gregtech.lang anchors on the py table) — the zh=en subset contract is the
 * GT6LangParityTest walk, not this seat.
 */
public class GTFluidsNamingFamilyTest extends GTOfflineTestBase {

	/** The thirty-four ids in declaration order — the upstream Loader_Fluids.java line order. */
	private static final List<String> IDS = List.of(
			"netherair", "enderair",
			"uuamplifier", "ic2uumatter",
			"biomass", "mcguffium",
			"fieryblood", "fierytears",
			"squidink", "indigo",
			"pyrotheum", "cryotheum", "petrotheum", "aerotheum",
			"plastic", "glass", "molten_latex", "molten_hsla",
			"cheese_molten", "sugar_molten", "rubber_molten",
			"wax_molten", "waxbee_molten", "waxparaffin_molten", "waxplant_molten",
			"waxrefractory_molten", "waxmagic_molten", "waxamnesic_molten", "waxsoulful_molten",
			"error",
			"rainbowsap", "glue", "mercury", "sluicejuice");

	@Test
	public void theTableCarriesThirtyFourRowsInDeclarationOrder() {
		assertEquals(IDS, GTFluids.NAMING_FLUID_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(34, GTFluids.NAMING_FLUID_SPECS.size());
		assertEquals(34, GTFluids.NAMING_FLUIDS.size(), "the live registrations walk the same table");
	}

	/**
	 * The per-fluid declared census — {id, display, tempK, density, viscosity, gas, lum}.
	 * The rows with a material anchor carry the formula transcription in the row comment;
	 * the material-null rows ride the honest 1000/1000 liquid defaults (the royal_jelly
	 * precedent) with PORT-OWNED tints (asserted only for non-default where meaningful).
	 */
	@Test
	public void declaredValuesMatchTheUpstreamAnchors() {
		Object[][] tCensus = {
			// the two dimension airs (:50-51) — state-2 gases, the setDensity(0) neutral-buoyancy literal
			{"netherair" , "Nether Air" , 370,     0,  200, true ,  0},
			{"enderair"  , "Ender Air"  , 280,     0,  200, true ,  0},
			// the UU pair (:72-73) — the null displays ride the material locals, the 100 K + amount-1 literals
			{"uuamplifier", "UU-Amplifier", 100,  1000, 1000, false,  0}, // MT.UUAmplifier 1.0 default g (MT.java:2083)
			{"ic2uumatter", "UU-Matter"   , 100,  1000, 1000, false,  0}, // MT.UUMatter 1.0 default g (MT.java:2084)
			// biomass (:83) — the four-arg 300 K default over the 1.0 default g (MT.java:2053)
			{"biomass"   , "Biomass"     , 300,  1000, 1000, false,  0},
			// mcguffium (:105) — the 300 K literal; 1000·3.122 over MT.Mcg (MT.java:1689)
			{"mcguffium" , "Mc Guffium 239", 300, 3122, 1000, false, 0},
			// the fiery pair (:108-109) — the 1500 K + setLuminosity(10) literals, material-null honest density
			{"fieryblood", "Fiery Blood" , 1500, 1000, 1000, false, 10},
			{"fierytears", "Fiery Tears" , 1500, 1000, 1000, false, 10},
			// the dye pair (:111-112) — material-null honest defaults
			{"squidink"  , "Squid Ink"   , 300,  1000, 1000, false,  0},
			{"indigo"    , "Indigo Dye"  , 300,  1000, 1000, false,  0},
			// the theum quartet (:130-133) — the four setDensity/setViscosity/setLuminosity literals
			{"pyrotheum" , "Blazing Pyrotheum"   , 4000,  2000, 1200, false, 15},
			{"cryotheum" , "Gelid Cryotheum"     ,   50,  4000, 3000, false,  0},
			{"petrotheum", "Tectonic Petrotheum" ,  400,  4000, 1500, false,  0},
			{"aerotheum" , "Zephyrean Aerotheum" ,  300,  -800,  100, true ,  0},
			// the molten quartet (:191-192/:197/:199)
			{"plastic"    , "Molten Plastic"   ,  423,  1000, 1000, false, 0}, // :191 the 423 K literal, 1.0 default g (MT.java:2150)
			{"glass"      , "Molten Glass"     , 1200,  2332, 1000, false, 0}, // :192 1000·2.33246 over uumMcfg(SiO2 1U) (MT.java:1941/:1939)
			{"molten_latex", "Latex"           ,  293,  1000, 1000, false, 0}, // :197 the DEF_ENV_TEMP literal (upstream CS.java:135)
			{"molten_hsla" , "Molten HSLA Steel", 1873,  7874, 1000, false, 5}, // :199 1000·7.874 over the steal chain to Fe (MT.java:2467/:1011); lum 5 literal
			// the createMolten food-material block (:202-212) — temp = melting point, lum 0 (the chocolate_molten BEE_ROW form)
			{"cheese_molten"       , "Molten Cheese"        ,  320,  1000, 1000, false, 0}, // MT.Cheese heat(320,500) (MT.java:2183)
			{"sugar_molten"        , "Molten Sugar"         ,  459, 27221, 1000, false, 0}, // MT.Sugar heat(459) + uumMcfg(C12H22O11) g 27.2217 (MT.java:1922)
			{"rubber_molten"       , "Molten Rubber"        ,  410, 11335, 1000, false, 0}, // MT.Rubber heat(410) + uumMcfg(C5H8) g 11.3357 (MT.java:2149)
			{"wax_molten"          , "Molten Wax"           ,  350,  1000, 1000, false, 0}, // MT.Wax heat(350) (MT.java:2116)
			{"waxbee_molten"       , "Molten Bees Wax"      ,  350,  1000, 1000, false, 0}, // MT.WaxBee local "Bees Wax" (MT.java:2117)
			{"waxparaffin_molten"  , "Molten Paraffin Wax"  ,  400,  1000, 1000, false, 0}, // MT.WaxParaffin heat(400) (MT.java:2119)
			{"waxplant_molten"     , "Molten Plant Wax"     ,  350,  1000, 1000, false, 0}, // MT.WaxPlant (MT.java:2120)
			{"waxrefractory_molten", "Molten Refractory Wax", 2600,  1000, 1000, false, 0}, // MT.WaxRefractory heat(2600) (MT.java:2118)
			{"waxmagic_molten"     , "Molten Magic Wax"     ,  350,  1000, 1000, false, 0}, // MT.WaxMagic (MT.java:2121)
			{"waxamnesic_molten"   , "Molten WaxAmnesic"    ,  350,  1000, 1000, false, 0}, // :211 — NO setLocal upstream (MT.java:2122), the internal name rides verbatim
			{"waxsoulful_molten"   , "Molten Soulful Wax"   ,  350,  1000, 1000, false, 0}, // MT.WaxSoulful (MT.java:2123)
			// the error sentinel (:358) — the 0 K literal
			{"error"      , "Liquid Error" ,    0,  1000, 1000, false, 0},
			// the drink-block leftovers — the DrinkStat seam stays the p33-b2 declared cut
			{"rainbowsap" , "Rainbow Sap" , 300,  1000, 1000, false, 0}, // :464
			{"glue"       , "Glue"        , 300,  1000, 1000, false, 0}, // :616 four-arg 300 K default (MT.java:2080)
			{"mercury"    , "Mercury"     , 300, 13533, 1000, false, 0}, // :618 1000·13.5336 over MT.Hg (MT.java:1123)
			{"sluicejuice", "Sluice Juice", 300,  1000, 1000, false, 0}, // :619
		};
		assertEquals(34, tCensus.length, "the census walks every row");
		for (Object[] tRow : tCensus) {
			String tId = (String)tRow[0];
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.namingSpec(tId);
			assertNotNull(tSpec, tId);
			assertEquals(tRow[1], tSpec.displayName(), tId + ": the FL.create local name");
			assertEquals(tRow[2], tSpec.temperature(), tId + ": the temperature literal");
			assertEquals(tRow[3], tSpec.density(), tId + ": the density");
			assertEquals(tRow[4], tSpec.viscosity(), tId + ": the viscosity");
			assertEquals(tRow[5], tSpec.gas(), tId + ": the state flag");
			assertEquals(tRow[6], tSpec.luminosity(), tId + ": the luminosity literal");
		}
	}

	/**
	 * The three state-2 rows must sit in the {@link GTFluidLists#GAS} list under their port
	 * registry paths — the FL.java:1105 auto-add rule every port fluid born gaseous rides
	 * (the pool-gas-seeds-13 reconciliation shape); the liquids and the null-density
	 * airs' non-gas siblings must not.
	 */
	@Test
	public void gaseousRowsReconcileWithTheGasList() {
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.NAMING_FLUID_SPECS) {
			assertEquals(tSpec.gas(), GTFluidLists.isGas(tSpec.name()),
					tSpec.name() + ": the GAS-list reconciliation");
		}
	}

	/**
	 * The single-name ruling: the three upstream alias spellings of fluids the port already
	 * carries under their canonical ids stay UNPORTED — kerosene (:79, the kerosine
	 * GTFluidsChemicalFamilyTest:301 pin), bioethanol (:103, the engine ethanol row) and
	 * ic2biomass (:84, this card's biomass row).
	 */
	@Test
	public void aliasSpellingsStayUnported() {
		assertNull(GTFluids.namingSpec("kerosene"), "the :79 alias — kerosine is the port id");
		assertNull(GTFluids.namingSpec("bioethanol"), "the :103 alias — the engine ethanol row");
		assertNull(GTFluids.namingSpec("ic2biomass"), "the :84 alias — biomass is the port id");
		assertNull(GTFluids.namingSpec("molten.latex"), "the port id folds the dot (molten_latex)");
	}

	/** The registration shape: source = the id, flowing = id + "_flowing" (the four-DR template). */
	@Test
	public void registrationShapeCarriesSourceAndFlowingIds() {
		for (GTFluids.ChemicalFluid tFamily : GTFluids.NAMING_FLUIDS) {
			//? if forge {
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId(), "source id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId(), "flowing id");
			*///?}
		}
		assertEquals(GTFluids.NAMING_FLUID_SPECS, GTFluids.NAMING_FLUIDS.stream().map(f -> f.spec).toList(),
				"the offline walk keeps the spec list and the live handles aligned, same order");
	}

	/** The two molten Latex carriers stay DISTINCT rows — :197 molten.latex vs the :198 latex (BEE_ROW). */
	@Test
	public void theTwoLatexCarriersStayDistinct() {
		assertNotNull(GTFluids.namingSpec("molten_latex"), ":197");
		assertNotNull(GTFluids.beeRowSpec("latex"), ":198 — the BEE_ROW carrier");
		assertEquals(293, GTFluids.namingSpec("molten_latex").temperature(), "the DEF_ENV_TEMP literal");
		assertEquals(300, GTFluids.beeRowSpec("latex").temperature(), "the :198 300 K literal");
		assertTrue(GTFluids.namingSpec("molten_latex") != GTFluids.beeRowSpec("latex"), "two registrations");
	}
}
