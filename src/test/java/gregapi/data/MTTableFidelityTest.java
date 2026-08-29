package gregapi.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;

/**
 * Fidelity tests for task gt-material-dataset (card 3): the ported MT/AM/ANY tables against
 * their upstream 1.7.10 sources. Every spot check cites the upstream file:line it freezes.
 *
 * TDG identity conclusion (inherited from 12d8eda, re-proven through the live table): the
 * tags put on the materials here are the REAL TD constants, e.g. Fe carries
 * TD.Processing.MELTING (same instance as OreDictMaterial.TDG.MELTING), so the card-2
 * placeholder unification holds at runtime.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class MTTableFidelityTest {

	@BeforeAll
	static void bootstrap() {
		// The registry may hold state from other test classes; the table needs a clean, open registry.
		MaterialRegistry.INSTANCE.reset();
		MT.init();
	}

	@Test
	void a000_registryCounts() {
		// Structure: every create() result is tracked (upstream MT.java:56 ALL_MATERIALS_REGISTERED_HERE.add).
		// 1273 field/alias slots in the main class (upstream declarations :523-1945), the registry
		// map holds one entry per distinct internal name (aliases and identical names resolve to
		// their targets via re-registration, not new map entries).
		// One MT.init() drives the whole cascade (MT table + AM + ANY + TECH/OREMATS/STONES/WOODS/UNUSED).
		assertEquals(2200, MaterialRegistry.INSTANCE.MATERIAL_MAP.size());
		assertEquals(1496, MT.ALL_MATERIALS_REGISTERED_HERE.size()); // create() results of the MT table only (AM/ANY create via OreDictMaterial.createMaterial directly, identical-name aliases are not create() results)
		// element IDs are protons * 10 upstream (see the unknown() helper at MT.java:337-338: aProtonsAndElectrons = aID / 10)
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_ARRAY[260] == MT.Fe);
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_ARRAY[920] == MT.U_238);
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_ARRAY[9830] == MT.Air);
	}

	@Test
	void a010_nullObject() {
		// upstream MT.java:524 NULL = create(-1, "NULL").setStatsElement(0,0,0,0,0).put(INVALID_MATERIAL, DONT_SHOW_THIS_COMPONENT);
		assertEquals(-1, MT.NULL.mID);
		assertEquals("NULL", MT.NULL.mNameInternal);
		assertTrue(MT.NULL.contains(TD.Properties.INVALID_MATERIAL));
		assertTrue(MT.NULL.contains(TD.Properties.DONT_SHOW_THIS_COMPONENT));
		assertEquals(0, MT.NULL.mProtons);
		assertEquals(0, MT.NULL.mMass);
		// MaterialRegistry.get falls back to the same instance (upstream OreDictMaterial.java:186-188).
		assertSame(MT.NULL, MaterialRegistry.INSTANCE.get("DoesNotExist"));
	}

	@Test
	void a020_ironElement() {
		// upstream MT.java:582 Fe = iron().qual(3, 6.0, 256, 2).setRGBaLiquid(255, 64, 32, 255);
		assertEquals(260, MT.Fe.mID); // element IDs are protons * 10 (upstream iron() = element(260, ...))
		assertEquals("Iron", MT.Fe.mNameInternal);
		assertEquals(26, MT.Fe.mProtons);
		assertEquals(30, MT.Fe.mNeutrons);
		assertEquals(56, MT.Fe.mMass);
		assertEquals(7.874, MT.Fe.mGramPerCubicCentimeter, 1e-9);
		// qual(3, 6.0, 256, 2) — upstream OreDictMaterial.qual :893-902
		assertEquals(3, MT.Fe.mToolTypes);
		assertEquals(6.0F, MT.Fe.mToolSpeed);
		assertEquals(256, MT.Fe.mToolDurability);
		assertEquals(2, MT.Fe.mToolQuality);
		// setRGBaLiquid(255, 64, 32, 255) — upstream OreDictMaterial :1060-1073
		assertEquals(255, MT.Fe.mRGBaLiquid[0]);
		assertEquals(64, MT.Fe.mRGBaLiquid[1]);
		assertEquals(32, MT.Fe.mRGBaLiquid[2]);
		assertEquals(255, MT.Fe.mRGBaLiquid[3]);
		// the element() wrapper tags METAL + SMITHABLE + MELTING (upstream :62) land as the real TD instances
		assertTrue(MT.Fe.contains(TD.Atomic.ELEMENT));
		assertTrue(MT.Fe.contains(TD.Atomic.METAL));
		assertTrue(MT.Fe.contains(TD.Processing.MELTING));
		// create(260) sets MD.GAPI (upstream MT.java:56), then TECH.init :2017-2021 Fe.put(MD.MC) overrides it
		assertEquals("minecraft", MT.Fe.mOriginalMod);
	}

	@Test
	void a030_goldTungstenOsmiumQual() {
		// upstream MT.java:636 Au = gold().qual(3, 12.5, 64, 2);
		assertEquals(12.5F, MT.Au.mToolSpeed, 1e-6);
		assertEquals(64, MT.Au.mToolDurability);
		// upstream MT.java:631 W = tungsten().qual(3, 8.0, 5120, 3);
		assertEquals(5120, MT.W.mToolDurability);
		assertEquals(3, MT.W.mToolQuality);
		// upstream MT.java:633 Os = osmium().qual(3, 16.0, 1280, 4).setLocal("Osmium");
		assertEquals("Osmium", MT.Os.mNameLocal);
		// upstream MT.java:600 Tc = technetium().qual(3, 10.0, 1280, 1); Gregorium=Tc;
		assertSame(MT.Tc, MT.Gregorium);
	}

	@Test
	void a040_uraniumIds() {
		// upstream uranium() = element(920, "Uranium", "U", 92, 146, 1405, 4404, 18.95, ...);
		assertEquals(920, MT.U_238.mID);
		assertEquals("Uranium", MT.U_238.mNameInternal);
		assertEquals(92, MT.U_238.mProtons);
		assertEquals(146, MT.U_238.mNeutrons);
		assertEquals(238, MT.U_238.mMass);
		assertEquals(1405, MT.U_238.mMeltingPoint);
		assertEquals(4404, MT.U_238.mBoilingPoint);
		// upstream MT.java:650 U_238 = uranium().qual(3, 6.0, 512, 3);
		assertEquals(512, MT.U_238.mToolDurability);
	}

	@Test
	void a050_airComposition() {
		// upstream MT.java:1027 Air = gas(9830, "Air", ..., TRANSPARENT, GASES).uumMcfg(0, N, 40*U, O, 11*U, Ar, 1*U).heat(100, 200).setDensity(0.0012);
		assertEquals(9830, MT.Air.mID);
		assertEquals(100, MT.Air.mMeltingPoint);
		assertEquals(200, MT.Air.mBoilingPoint);
		assertEquals(0.0012, MT.Air.mGramPerCubicCentimeter, 1e-9);
		IOreDictConfigurationComponent c = MT.Air.mComponents;
		assertNotNull(c);
		// 40+11+1 = 52 material units -> the setMcfg(0, ...) auto-divider is 52 (upstream setMcfg :533-534)
		assertEquals(52, c.getCommonDivider());
		java.util.List<OreDictMaterialStack> undivided = c.getUndividedComponents();

		assertEquals(MT.N, undivided.get(0).mMaterial);
		assertEquals(40 * CS.U, undivided.get(0).mAmount);
		assertEquals(MT.O, undivided.get(1).mMaterial);
		assertEquals(11 * CS.U, undivided.get(1).mAmount);
		assertEquals(MT.Ar, undivided.get(2).mMaterial);
		assertTrue(MT.Air.contains(TD.ItemGenerator.GASES));
		assertTrue(MT.Air.contains(TD.Processing.UUM)); // uumMcfg puts UUM (:511)
	}

	@Test
	void a060_water() {
		// upstream MT.java:1007 H2O = lquddcmp(9800, "Water", ...).uumMcfg(0, H, 2*U, O, 1*U).heat(CS.C, CS.C+100).setDensity(1.0);
		assertEquals(9800, MT.H2O.mID);
		assertSame(MT.H2O, MT.Water);
		assertEquals(273, MT.H2O.mMeltingPoint); // CS.C = 273 (upstream CS.java:132)
		assertEquals(373, MT.H2O.mBoilingPoint);
		assertEquals(1.0, MT.H2O.mGramPerCubicCentimeter, 1e-9);
		assertEquals(2, MT.H2O.mComponents.getUndividedComponents().size());
		assertEquals(MT.H, MT.H2O.mComponents.getUndividedComponents().get(0).mMaterial);
		assertEquals(2 * CS.U, MT.H2O.mComponents.getUndividedComponents().get(0).mAmount);
		assertTrue(MT.H2O.contains(TD.Compounds.DECOMPOSABLE));
	}

	@Test
	void a070_chemicalCompounds() {
		// upstream MT.java:1056 SiO2 = oredustdcmp(8000, "Silicon Dioxide", ...).uumMcfg(0, Si, 1*U, O, 2*U).heat(1986, 3220);
		assertEquals(8000, MT.SiO2.mID);
		assertEquals(1986, MT.SiO2.mMeltingPoint);
		assertEquals(3220, MT.SiO2.mBoilingPoint);
		// upstream MT.java:1047 H2SO4 = lqudaciddcmp(9824, "Sulfuric Acid", ...).uumMcfg(0, H, 2*U, S, 1*U, O, 4*U).setDensity(1.5).heat(200, 400);
		assertEquals(9824, MT.H2SO4.mID);
		assertEquals(1.5, MT.H2SO4.mGramPerCubicCentimeter, 1e-9);
		assertEquals(3, MT.H2SO4.mComponents.getUndividedComponents().size());
		assertEquals(MT.S, MT.H2SO4.mComponents.getUndividedComponents().get(1).mMaterial);
		assertEquals(1 * CS.U, MT.H2SO4.mComponents.getUndividedComponents().get(1).mAmount);
		// upstream MT.java:1037 CH4 = gaschemelec(9832, "Methane", ...).uumMcfg(0, C, 1*U, H, 4*U).heat(100, 200);
		assertEquals(9832, MT.CH4.mID);
		assertTrue(MT.CH4.contains(TD.Properties.FLAMMABLE));
		assertTrue(MT.CH4.contains(TD.Processing.ELECTROLYSER));
	}

	@Test
	void a080_alloysAndAlloyingRecipes() {
		// upstream MT.java:1805 ObsidianSteel = alloy(8731, "Obsidian Steel", ...).qual(Steel).setAloy(1, Steel, 1*U, Obsidian, 9*U);
		assertEquals(8731, MT.ObsidianSteel.mID);
		assertNotNull(MT.ObsidianSteel.mComponents);
		// setAloy(1, Steel, 1*U, Obsidian, 9*U): explicit divider 1, two undivided components
		assertEquals(1, MT.ObsidianSteel.mComponents.getCommonDivider());
		assertEquals(2, MT.ObsidianSteel.mComponents.getUndividedComponents().size());
		assertTrue(MT.ObsidianSteel.contains(TD.Compounds.ALLOY));
		assertTrue(MT.ObsidianSteel.contains(TD.Processing.CRUCIBLE_ALLOY));
		// addAlloyingRecipe side effects (upstream OreDictMaterial.java:455-466): Fe registered via the :3333-3337 recipes.
		assertTrue(MaterialRegistry.INSTANCE.ALLOYS.contains(MT.Fe));
		assertFalse(MT.Fe.mAlloyCreationRecipes.isEmpty());
		assertTrue(MT.Fe2O3.mAlloyComponentReferences.contains(MT.Fe));
		assertTrue(MT.CaCO3.mAlloyComponentReferences.contains(MT.Fe));
		// upstream MT.java:1732 TungstenSintered: setGenerifying(W) + setForging(null, U)
		assertSame(MT.W, MT.TungstenSintered.mTargetGenerifying.mMaterial);
		assertSame(MT.TungstenSintered, MT.TungstenSintered.mTargetForging.mMaterial);
	}

	@Test
	void a090_oreMultiplierPriorityPrefix() {
		// upstream MT.java:164 glowstone() = oredustcent(...).setPriorityPrefix(2).setOreMultiplier(4);
		assertEquals(4, MT.Glowstone.mOreMultiplier);
		assertEquals(2, MT.Glowstone.mPriorityPrefixIndex);
		// upstream MT.java:570 Si = silicon().setPriorityPrefix(5);
		assertEquals(5, MT.Si.mPriorityPrefixIndex);
		// upstream MT.java:572 S = sulfur().setPriorityPrefix(2);
		assertEquals(2, MT.S.mPriorityPrefixIndex);
	}

	@Test
	void a100_diamondGems() {
		// upstream MT.java:208 diamond() = valgemdcmp(...).put(ANY.Diamond, COMMON_ORE).steal(C).qual(3, 8.0, 1280, 3).setSmelting(C, 2*U).setBurning(Ash, U);
		// diamond() :208 steals C's stats first, then uumMcfg(1, C, 4*U) recomputes the molecule: Diamond = C4 -> 24 protons, 24 neutrons, mass 48 (upstream :2183)
		assertEquals(24, MT.Diamond.mProtons);
		assertEquals(48, MT.Diamond.mMass);
		assertEquals(3.53, MT.Diamond.mGramPerCubicCentimeter, 1e-9); // explicit setDensity(3.53) at the :2183 call site
		assertSame(MT.C, MT.Diamond.mTargetSmelting.mMaterial);
		assertEquals(2 * CS.U, MT.Diamond.mTargetSmelting.mAmount); // setSmelting(C, 2*U) at diamond() :601
		// upstream MT.java:1571 Diamantine = gem_aa(8377, ..., Diamond, VALUABLE) — gem_aa: uumMcfg(0, aCopy, U).steal(aCopy)
		assertSame(MT.Diamond, MT.Diamantine.mTargetGenerifying.mMaterial);
	}

	@Test
	void a110_enchantmentStrip() {
		// upstream MT.java:2724 Plastic.addEnchantmentForDamage(Enchantment.knockback, 1).addEnchantmentForRanged(Enchantment.punch, 1);
		// MC-strip: Enchantment object -> vanilla ID string.
		assertEquals(1, MT.Plastic.mEnchantmentWeapons.size());
		assertEquals("knockback", MT.Plastic.mEnchantmentWeapons.get(0).mEnchantmentID);
		assertEquals(1, MT.Plastic.mEnchantmentAmmo.size());
		assertEquals("punch", MT.Plastic.mEnchantmentRanged.get(0).mEnchantmentID);
		// upstream MT.java:2740 Skyroot: addEnchantmentForFishing(Enchantment.field_151370_z, 1) -> "luckOfTheSea"
		assertEquals("luckOfTheSea", MT.Skyroot.mEnchantmentFishing.get(0).mEnchantmentID);
	}

	@Test
	void a120_originalModOverrides() {
		// upstream TECH.init :2017+ Empty.put(MD.MC) — put dispatches ModData to setOriginalMod (OreDictMaterial :1463-1466)
		assertEquals(MT.MD.MC.mID, MT.Empty.mOriginalMod);
		assertEquals(MT.MD.MC.mID, MT.Wood.mOriginalMod);
		// upstream MT.java:52 tier() passes MD.GAPI, then TECH.init :2175 Primitive.put(MD.GT) overrides it
		assertEquals("gregtech", MT.Primitive.mOriginalMod);
		// upstream MD.GT5U = new ModData(ModIDs.GT, ...) — ObsidianSteel created via alloy(..., MD.TG)
		assertEquals(MT.MD.TG.mID, MT.ObsidianSteel.mOriginalMod);
	}

	@Test
	void a130_deprecatedAliasesAndInnerClasses() {
		// upstream MT.java:1904 @Deprecated aliases are field-to-field identities
		assertSame(MT.Ke, MT.Trinium);
		assertSame(MT.Vb, MT.Vibranium);
		assertSame(MT.Al2O3, MT.Alumina);
		// upstream MT.java:1907-1945 STONES alias block
		assertSame(MT.STONES.Marble, MT.Marble);
		assertSame(MT.STONES.Basalt, MT.Basalt);
		assertSame(MT.STONES.Quartzite, MT.Quartzite);
		// TECH aliases (upstream MT.java:1950)
		assertSame(MT.Brick, MT.TECH.Brick);
		assertSame(ANY.Wood, MT.TECH.AnyWood);
		// WOODS/OREMATS (upstream :3696/:3869 force-loaded by init)
		assertEquals(9302, MT.WOODS.Spruce.mID);
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_MAP.containsKey("Cassiterite"));
		assertSame(MT.OREMATS.Magnetite, MaterialRegistry.INSTANCE.MATERIAL_MAP.get("Magnetite"));
	}

	@Test
	void a140_anyGroups() {
		// upstream ANY.java:32 any() = createMaterial(-1, ...).put(UNUSED_MATERIAL, INVALID_MATERIAL, IGNORE_IN_COLOR_LOG);
		assertEquals(-1, ANY.Cu.mID);
		assertTrue(ANY.Cu.contains(TD.Properties.INVALID_MATERIAL));
		assertTrue(ANY.Cu.contains(TD.Properties.UNUSED_MATERIAL));
		// upstream ANY.java:125 Cu: steal(MT.Cu) + addReRegistrationToThis(MT.Cu, MT.AnnealedCopper)
		assertEquals(MT.Cu.mMass, ANY.Cu.mMass);
		assertTrue(ANY.Cu.mToThis.contains(MT.Cu));
		assertTrue(ANY.Cu.mToThis.contains(MT.AnnealedCopper));
		// upstream ANY.java:151-156 mTargetReversing wiring
		assertSame(ANY.W, MT.W.mTargetReversing);
		assertSame(ANY.Cu, MT.Cu.mTargetReversing);
		assertSame(ANY.Iron, MT.Fe.mTargetReversing);
		assertSame(ANY.Steel, MT.Steel.mTargetReversing);
		// upstream ANY.java:130 ANY.SiO2 re-registrations include the stones/glass/flint family
		assertTrue(ANY.SiO2.mToThis.contains(MT.Glass));
		assertTrue(ANY.SiO2.mToThis.contains(MT.STONES.Quartzite));
		// upstream ANY.java:96 Glowstone group carries MT.Glowstone's output chain
		assertSame(MT.Glowstone, ANY.Glowstone.mTargetPulver.mMaterial);
	}

	@Test
	void a150_antiMatterTable() {
		// upstream AM.java:70 Hydrogen = diatomic(4010, "Anti-Hydrogen", 1, 0, 14, 20, 0.00008988, ...).put(CONTAINERS_GAS, UUM);
		assertEquals(4010, AM.Hydrogen.mID);
		assertEquals("AntiHydrogen", AM.Hydrogen.mNameInternal); // createMaterial sanitizes away the minus (upstream OreDictMaterial :204-206)
		assertEquals(1, AM.Hydrogen.mProtons);
		assertEquals(0, AM.Hydrogen.mNeutrons);
		assertTrue(AM.Hydrogen.contains(TD.Atomic.ANTIMATTER));
		assertTrue(AM.Hydrogen.contains(TD.Atomic.DIATOMIC_NONMETAL));
		assertEquals("gregapi", AM.Hydrogen.mOriginalMod); // upstream AM.java:40 put(ANTIMATTER, MD.GAPI)
		// upstream AM.java:59 n = create(4003, "Anti-Neutron").setStatsElement(0,0,1,0,0)
		assertEquals(4003, AM.n.mID);
		assertSame(AM.n, AM.Neutron);
		// upstream AM.java:168 U_238 = actinide(4920, "Anti-Uranium", 92, 146, 1405, 4404, 18.95, ...), Uranium238=U_238.addIdenticalNames(...)
		assertEquals(4920, AM.U_238.mID);
		assertEquals(92, AM.U_238.mProtons);
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_MAP.containsKey("AntiUranium238")); // addIdenticalNames sanitizes away the minus
		// upstream AM.java:257 Nq = element(5740, "Anti-Naquadah", ...).setRGBaLiquid(0, 255, 0, 255), Usq = Nq
		assertEquals(5740, AM.Nq.mID);
		assertSame(AM.Nq, AM.Usq);
		assertEquals(0, AM.Nq.mRGBaLiquid[0]);
		// upstream AM.java:88 Aluminum=Al.addIdenticalNames("Anti-Aluminum")
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_MAP.containsKey("AntiAluminum")); // addIdenticalNames sanitizes away the minus (upstream AM.java:88)
	}

	@Test
	void a160_tdgTagInstancesUnifyWithRealTD() {
		// Card-2 handoff question, now proven against the LIVE table: the tags carried by the
		// materials are the same instances as the ported TD constants (12d8eda verified the
		// OreDictMaterial.TDG placeholders unify via TagData.createTagData idempotence).
		assertTrue(MT.Fe.contains(TD.Processing.MELTING));
		assertTrue(MT.ObsidianSteel.contains(TD.Processing.CRUCIBLE_ALLOY));
		assertTrue(MT.Air.contains(TD.Processing.UUM));
		assertTrue(MT.Fe.contains(TD.Atomic.ELEMENT));
		assertTrue(MT.ObsidianSteel.contains(TD.Compounds.ALLOY));
	}

	@Test
	void a170_textureNameReferences() {
		// upstream MT.java:539 Magic = create(4000, "Magic", ...).setTextures(SET_SHINY) — MC-strip: {"SHINY","SHINY"}
		assertTrue(MT.Magic.mTextureSetsBlock.contains("SHINY"));
		assertTrue(MT.Magic.mTextureSetsItems.contains("SHINY"));
		assertTrue(MT.Magic.contains(TD.Atomic.ELEMENT));
		assertTrue(MT.Magic.contains(TD.Properties.MAGICAL));
		// upstream MT.java:582 Fe gets SET_METALLIC from its iron() wrapper
		assertTrue(MT.Fe.mTextureSetsBlock.contains("METALLIC"));
		// hide() (upstream MT.java:539 .hide())
		assertTrue(MT.Magic.mHidden);
		assertFalse(MT.Fe.mHidden);
	}

	@Test
	void a180_reinitIsIdempotentAndResetAware() {
		// upstream MT.java:1888 guard; port adds registry-reset awareness (port-only concept).
		MT.init(); // second call must be a no-op
		assertSame(MT.Fe, MaterialRegistry.INSTANCE.MATERIAL_ARRAY[260]);
		// after a registry wipe the next init() re-registers the whole table
		MaterialRegistry.INSTANCE.reset();
		MT.init();
		assertSame(MT.Fe, MaterialRegistry.INSTANCE.MATERIAL_ARRAY[260]);
		assertEquals("Iron", MT.Fe.mNameInternal);
		assertTrue(MaterialRegistry.INSTANCE.ALLOYS.contains(MT.Fe));
	}
}
