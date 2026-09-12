package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The p28-c-ulv-machine-ladder melting gate (the OFFLINE pure-function + hook half):
 * {@link TileEntityBasicMachine#meltingGateBlocks} is the decision function, the
 * {@code checkRecipe} arm is its only production consumer. Threshold = 1375 K, the
 * stone-crucible ceiling (GT6Crucibles.java:86, TileEntitySmelteryOfflineTest :281) —
 * the rulings' "meltable in the stone crucible = processable in ULV" symmetry, so the
 * tri-state is exactly: below the ceiling passes, AT the ceiling passes (inclusive — the
 * Smeltery :194 {@code mTemperature >= mMeltingPoint} melt-comparison is inclusive on the
 * same word), above refuses. Any ONE exceeding stack refuses the whole recipe (the
 * 任一超即拒 ruling); stacks with NO material data (vanilla items) pass — the
 * "no material = no gate" arm keeps the vanilla-compat recipes runnable.
 *
 * <p>OFFLINE posture: the probe items are REAL {@link MaterialPrefixItem}s constructed
 * against throwaway negative-ID materials (the ANY.java {@code createMaterial(-1, ...)}
 * form — legal with the registry CLOSED, and the throwaway keeps the shared MT universe
 * unmutated). No registry registration: the gate reads the item's {@code material} field,
 * and the recipe matcher compares stack identities — neither touches the item registry.
 */
public class GT6MeltingGateTest extends TileEntityBasicMachineOfflineTestBase {

	/** The stone-crucible ceiling every ULV row carries (decisions.p28-ulv-tier-rulings). */
	static final long CEILING = 1375L;

	static OreDictMaterial sCold, sEdge, sHot;
	static MaterialPrefixItem sColdItem, sEdgeItem, sHotItem;

	@BeforeAll
	static void buildProbeItems() {
		// negative-ID materials are creatable with the registry closed (MaterialRegistry
		// guards only ID >= 0) — no open/close cycle, no shared-state mutation.
		sCold = MaterialRegistry.INSTANCE.createMaterial(-1, "Melting Gate Cold", "Melting Gate Cold");
		sEdge = MaterialRegistry.INSTANCE.createMaterial(-1, "Melting Gate Edge", "Melting Gate Edge");
		sHot  = MaterialRegistry.INSTANCE.createMaterial(-1, "Melting Gate Hot" , "Melting Gate Hot");
		sCold.mMeltingPoint = 500;   // the Sn-class low-melt band (505 K upstream)
		sEdge.mMeltingPoint = 1375;  // EXACTLY the ceiling — the inclusive boundary word
		sHot.mMeltingPoint  = 1811;  // the Fe-class band (1811 K upstream) — above the ceiling
		sColdItem = probeItem("melting_gate_cold", sCold);
		sEdgeItem = probeItem("melting_gate_edge", sEdge);
		sHotItem  = probeItem("melting_gate_hot" , sHot);
	}

	/**
	 * The probe-item helper (the GT6RecipeMapCrucibleTest GTMaterialItemsBoot posture):
	 * the Forge 1.20.1 Item constructor registers an INTRUSIVE HOLDER (Item.java:61 →
	 * NamespacedWrapper.createIntrusiveHolder), so the frozen registry must be unfrozen
	 * before any offline construction, and the probe lands under a throwaway id local to
	 * this test class.
	 */
	private static MaterialPrefixItem probeItem(String aProbeId, OreDictMaterial aMaterial) {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
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
		MaterialPrefixItem rItem = new MaterialPrefixItem(new net.minecraft.world.item.Item.Properties(), gregapi.data.OP.ingot, aMaterial);
		net.minecraft.core.Registry.register(tRegistry, new net.minecraft.resources.ResourceLocation("gt6", aProbeId), rItem);
		return rItem;
	}

	/** The first declared field up the hierarchy (the GTMaterialItemsBoot walk). */
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

	// ------------------------------------------------------------------
	// the pure function tri-state + the multi-stack arm
	// ------------------------------------------------------------------

	@Test
	void meltingGateTriState() {
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING, new ItemStack(sColdItem, 1)),
				"500 K < 1375 K — the low-melt band passes (the ULV design premise: Sn/Pb/Zn/Al/Cu processable)");
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING, new ItemStack(sEdgeItem, 1)),
				"1375 K AT the ceiling passes — the gate is mMeltingPoint > threshold (inclusive, the Smeltery :194 word)");
		assertTrue(TileEntityBasicMachine.meltingGateBlocks(CEILING, new ItemStack(sHotItem, 1)),
				"1811 K > 1375 K — the Fe-class band refuses (the ore-chain progress wall holds)");
	}

	@Test
	void anyOneExceedingStackRefusesTheWholeRecipe() {
		assertTrue(TileEntityBasicMachine.meltingGateBlocks(CEILING,
						new ItemStack(sColdItem, 1), new ItemStack(sColdItem, 1), new ItemStack(sHotItem, 1)),
				"任一超即拒: one Fe-class stack among clean ones blocks");
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING,
						new ItemStack(sColdItem, 1), new ItemStack(sEdgeItem, 1), new ItemStack(sColdItem, 1)),
				"all stacks at-or-below the ceiling pass");
	}

	@Test
	void materialLessStacksPassAndNullArraysPass() {
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING, new ItemStack(Items.COBBLESTONE, 1)),
				"vanilla items carry no material data — no gate (the vanilla-compat arm)");
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING, ItemStack.EMPTY),
				"empty stacks pass");
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING, (ItemStack[]) null),
				"no inputs at all — nothing to refuse");
		assertFalse(TileEntityBasicMachine.meltingGateBlocks(CEILING),
				"varargs-empty — nothing to refuse");
	}

	// ------------------------------------------------------------------
	// the checkRecipe hook — refuse BEFORE any consume, both arms
	// ------------------------------------------------------------------

	/** Pours one probe-input row into a FRESH shredder map and builds the gated fixture. */
	private TileEntityBasicMachine gatedMachine(MaterialPrefixItem aInput) {
		RecipeMap tMap = GT6RecipeMaps.SHREDDER; // fresh per test (the base initRecipeMaps)
		tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(aInput, 1)},
				new ItemStack[] {new ItemStack(Items.SAND, 1)}, null, null, 16, 16, 0));
		TileEntityBasicMachine tMachine = makeMachine(tMap, 1, false);
		tMachine.mMaxMeltingPointK = CEILING;
		tMachine.getInventory().setStackInSlot(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(aInput, 1));
		return tMachine;
	}

	@Test
	void checkRecipeRefusesAboveTheCeilingBeforeAnyConsume() {
		TileEntityBasicMachine tMachine = gatedMachine(sHotItem);
		// the probe arm refuses identically — the machine never even starts
		assertEquals(TileEntityBasicMachine.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
				tMachine.checkRecipe(false, false), "the probe arm refuses the over-ceiling input");
		assertEquals(TileEntityBasicMachine.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
				tMachine.checkRecipe(true, false), "the consume arm refuses too");
		assertEquals(1, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
				"the refusal lands BEFORE the consume — the input stack stays");
		assertNull(tMachine.mCurrentRecipe, "no recipe latched on a gated refusal");
	}

	@Test
	void checkRecipePassesAtOrBelowTheCeilingAndWithTheGateOff() {
		TileEntityBasicMachine tEdge = gatedMachine(sEdgeItem);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
				tEdge.checkRecipe(true, false), "1375 K AT the ceiling runs — the inclusive word");
		TileEntityBasicMachine tCold = gatedMachine(sColdItem);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
				tCold.checkRecipe(true, false), "500 K under the ceiling runs");
		// gate off (null — every legacy row) = byte-identical legacy behaviour
		TileEntityBasicMachine tUngated = gatedMachine(sHotItem);
		tUngated.mMaxMeltingPointK = null;
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
				tUngated.checkRecipe(true, false), "null gate = no gate, the legacy rows are untouched");
	}

	/** The fixture machine default: no gate (the field-init null — the every-legacy-row shape). */
	@Test
	void fixtureDefaultCarriesNoGate() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		assertNull(tMachine.mMaxMeltingPointK, "the BE default is gate-off");
	}
}
