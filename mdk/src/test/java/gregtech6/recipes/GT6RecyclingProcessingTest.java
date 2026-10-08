package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregtech6.components.IOreDictListenerRecyclable;
import gregtech6.components.OM;
import gregtech6.registry.GTMaterialItems;

/**
 * The recycling generator pins (task component-recycling-recipes, the six acceptance arms).
 *
 * <p>Offline posture (the OMComponentFaceTest convention): the vanilla registries bootstrap,
 * the material universe refills, the recipe maps init (phase OPEN — the window never opens),
 * and the two environment seams ({@link GT6RecyclingProcessing#sMoltenFluid},
 * {@link GT6RecyclingProcessing#sPrefixItem}) run under injected stubs — the fluid registry and
 * the GT item registration index never boot offline. The stub fluids are vanilla WATER
 * stand-ins (any non-lava fluid answers the gates identically; the lava arm is formula-covered
 * by its floor pin through the non-lava branch).
 *
 * <p>The duration pin rides the melting point in BOTH worlds: the FluidType temperature face
 * either parks offline (the declared probe) or answers a self-molten fluid whose registered
 * temperature IS the material's melting point (GTFluids.java:239) — and any carrier at or below
 * the melting point lets the upstream {@code max()} land on the melting point either way.
 */
class GT6RecyclingProcessingTest {

	private static Function<OreDictMaterial, Fluid> sProductionMolten;
	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sProductionPrefixItem;

	@BeforeAll
	static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap(); // the Forge-patched boot throws offline at NetworkHooks — the registries are ready by then
		} catch (Throwable ignored) {}
		GTMaterialItems.initMaterials(); // MT.init + OP.init, the offline material universe
		GT6RecipeMaps.init(); // defensive + idempotent (the GT6RecipesOreChain.java:165 form); the phase is OPEN offline
		GT6RecyclingProcessing.resetForTest();
		sProductionMolten = GT6RecyclingProcessing.sMoltenFluid;
		sProductionPrefixItem = GT6RecyclingProcessing.sPrefixItem;
		GT6RecyclingProcessing.register(); // "addListener(OM) 即活": late registration replays the log
	}

	@AfterEach
	void restoreProductionSeams() {
		GT6RecyclingProcessing.sMoltenFluid = sProductionMolten;
		GT6RecyclingProcessing.sPrefixItem = sProductionPrefixItem;
	}

	@AfterAll
	static void leaveTheProductionSeamsInPlace() {
		GT6RecyclingProcessing.sMoltenFluid = sProductionMolten;
		GT6RecyclingProcessing.sPrefixItem = sProductionPrefixItem;
	}

	// ------------------------------------------------------------- ① the melt rows + the duration formula

	/** Acceptance ①: a FURNACE-flagged plate declaration pours ONE Melter row + ONE Smelter row
	 * with the fluid output at the 144 L/U convention and the upstream :290 duration at 16 EU/t.
	 * The carrier material is COPPER: the FURNACE gate (:289) admits it (MT.java:418 lists
	 * FURNACE) — the plan card's "铁板" example was upstream-wrong (iron is NEVER_FURNACE,
	 * MT.java:414, so iron plates melt NOTHING; pinned in its own test below). */
	@Test
	void furnacePlatePoursMelterAndSmelterRowsWithTheFormulaDuration() {
		Item tProbe = probeItem("recycling_probe_plate_cu");
		ItemStack tStack = new ItemStack(tProbe);
		stubMolten(MT.Cu);
		assertTrue(OM.setItemData(tStack, new OreDictItemData(OP.plate, MT.Cu))); // plate is RECYCLABLE (OP.java:1245) — the notification gate opens

		Recipe tMelter = single(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack));
		Recipe tSmelter = single(GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tStack));
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MORTAR, tStack).isEmpty());
		assertEquals(tProbe, tMelter.mInputs[0].getItem());
		assertEquals(Fluids.WATER, tMelter.mFluidOutputs[0].getFluid());
		// the upstream liquid() units conversion: one material unit (U) → 144 L (the FL.make("…molten", 144) template)
		assertEquals(144, tMelter.mFluidOutputs[0].getAmount());
		assertEquals(16, tMelter.mEUt);
		// the :290 duration formula, computed from public fields: weight(U) * (max(1357, ≤1357) - 293) / 1600, floored at the 16 tick arm
		double tWeight = new OreDictMaterialStack(MT.Cu, CS.U).weight();
		long tExpected = (long)Math.max(16, tWeight * (Math.max(MT.Cu.mMeltingPoint, 300L) - GT6RecyclingProcessing.DEF_ENV_TEMP) / 1600);
		assertEquals(tExpected, tMelter.mDuration);
		assertEquals(tExpected, tSmelter.mDuration);
		assertEquals(16, tSmelter.mEUt);
		// the consumer face actually finds the row (the MS-2 "回收链通" anchor)
		assertSame(tMelter, GT6RecipeMaps.MELTER.findRecipe(null, Long.MAX_VALUE, null, new FluidStack[0], tStack));
	}

	/** The :289 gate's negative arm, pinned because the plan card's example tripped over it:
	 * iron is NEVER_FURNACE (MT.java:414 verbatim) — an iron plate declaration yields ZERO
	 * Melter/Smelter rows, upstream verbatim (iron recycles through the crucible/melter domain's
	 * own material-handler rows upstream, never through RecyclingProcessing). */
	@Test
	void neverFurnacePlateMeltsNothing() {
		Item tProbe = probeItem("recycling_probe_plate_fe");
		ItemStack tStack = new ItemStack(tProbe);
		stubMolten(MT.Fe); // the fluid WOULD resolve — the gate, not the seam, declines
		assertTrue(OM.setItemData(tStack, new OreDictItemData(OP.plate, MT.Fe)));
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack).isEmpty());
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tStack).isEmpty());
	}

	// ------------------------------------------------------------- ② the Mortar arm

	/** Acceptance ②: prefix-less Paper grinds to the best-fitting dust (one full dust at U),
	 * 16/16; no melt rows (Paper carries no MELTING flag). */
	@Test
	void paperGrindsToDustOnTheMortarMap() {
		Item tProbe = probeItem("recycling_probe_paper");
		Item tDust = probeItem("recycling_probe_paper_dust");
		ItemStack tStack = new ItemStack(tProbe);
		GT6RecyclingProcessing.sPrefixItem = (aPrefix, aMaterial) -> aPrefix == OP.dust && aMaterial == MT.Paper ? tDust : null;
		assertTrue(OM.setItemData(tStack, new OreDictItemData(new OreDictMaterialStack(MT.Paper, CS.U))));

		Recipe tMortar = single(GT6RecyclingProcessing.rows(GT6RecipeMaps.MORTAR, tStack));
		assertEquals(tDust, tMortar.mOutputs[0].getItem());
		assertEquals(1, tMortar.mOutputs[0].getCount());
		assertEquals(16, tMortar.mEUt);
		assertEquals(16, tMortar.mDuration);
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack).isEmpty());
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tStack).isEmpty());
	}

	/** The :248 byproduct gate: Bone grinds ONLY when the data carries no byproducts. */
	@Test
	void boneGrindsOnlyWithoutByproducts() {
		Item tProbe = probeItem("recycling_probe_bone");
		Item tDust = probeItem("recycling_probe_bone_dust");
		GT6RecyclingProcessing.sPrefixItem = (aPrefix, aMaterial) -> aPrefix == OP.dust && aMaterial == MT.Bone ? tDust : null;
		ItemStack tClean = new ItemStack(tProbe);
		assertTrue(OM.setItemData(tClean, new OreDictItemData(new OreDictMaterialStack(MT.Bone, CS.U))));
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.MORTAR, tClean).size());
		// a second probe item whose Bone data carries a byproduct: the mortar arm stays silent
		Item tProbe2 = probeItem("recycling_probe_bone2");
		ItemStack tWithByproduct = new ItemStack(tProbe2);
		assertTrue(OM.setItemData(tWithByproduct, new OreDictItemData(MT.Bone, CS.U, MT.Cu, CS.U4)));
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MORTAR, tWithByproduct).isEmpty());
	}

	// ------------------------------------------------------------- ③ the ore-prefix door

	/** Acceptance ③: :255 — prefixed ORE/ORE_PROCESSING_DIRTY data declines outright (driven
	 * straight into the listener: the OM-side notification gate for ore is the central-face
	 * card's pin, this is the listener-side door behind it). */
	@Test
	void orePrefixedDataYieldsZeroRows() {
		Item tProbe = probeItem("recycling_probe_ore");
		stubMolten(MT.Cu);
		GT6RecyclingProcessing.INSTANCE.onRecycleableRegistration(new IOreDictListenerRecyclable.OreDictRecyclingContainer(
				new ItemStack(tProbe), new OreDictItemData(OP.ore, MT.Cu)));
		ItemStack tQuery = new ItemStack(tProbe);
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tQuery).isEmpty());
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tQuery).isEmpty());
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MORTAR, tQuery).isEmpty());
	}

	// ------------------------------------------------------------- ④ the single-fluid gate

	/** Acceptance ④: a material set resolving to TWO fluid-bearing melt targets nulls the whole
	 * melt (:271-284 — the second entry breaks with tFluid = null); the same data with only ONE
	 * resolvable target pours. */
	@Test
	void doubleFluidMaterialsYieldZeroRowsButSingleFluidPours() {
		// two resolvable targets: zero rows on both maps
		Item tProbe2 = probeItem("recycling_probe_alloy2");
		ItemStack tAlloy = new ItemStack(tProbe2);
		stubMolten(MT.Cu, MT.Sn);
		assertTrue(OM.setItemData(tAlloy, new OreDictItemData(new OreDictMaterialStack(MT.Cu, CS.U), new OreDictMaterialStack(MT.Sn, CS.U))));
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tAlloy).isEmpty());
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tAlloy).isEmpty());
		// one resolvable target (the other material has no registered molten row — the negative-account shape): pours
		Item tProbe1 = probeItem("recycling_probe_alloy1");
		ItemStack tSingle = new ItemStack(tProbe1);
		stubMolten(MT.Cu); // Sn resolves to null and skips silently (:277 mLiquid == null arm)
		assertTrue(OM.setItemData(tSingle, new OreDictItemData(new OreDictMaterialStack(MT.Cu, CS.U), new OreDictMaterialStack(MT.Sn, CS.U))));
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tSingle).size());
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tSingle).size());
	}

	// ------------------------------------------------------------- ⑤ the regeneration idempotence

	/** Acceptance ⑤: a re-notification of the same stack REPLACES its rows instead of stacking —
	 * through both live paths: a prefix-less re-declaration (the write-path re-registration the
	 * gate admits) and a second listener instance replaying the whole registration log (the
	 * "addListener 即活" catch-up). */
	@Test
	void regenerationIsIdempotent() {
		Item tProbe = probeItem("recycling_probe_idem");
		ItemStack tStack = new ItemStack(tProbe);
		stubMolten(MT.Cu);
		// prefix-less data re-declares freely (the OM gate only refuses prefixed overwrites)
		assertTrue(OM.setItemData(tStack, new OreDictItemData(new OreDictMaterialStack(MT.Cu, CS.U))));
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack).size());
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tStack).size());
		assertTrue(OM.setItemData(tStack, new OreDictItemData(new OreDictMaterialStack(MT.Cu, CS.U))));
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack).size());
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tStack).size());
		// a second listener instance replays the full log — the same stack's rows replace again, zero stacking
		OM.addListener(new GT6RecyclingProcessing());
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack).size());
		assertEquals(1, GT6RecyclingProcessing.rows(GT6RecipeMaps.SMELTER, tStack).size());
		// and the map itself holds exactly one row for this stack (the ledger and the maps agree)
		assertEquals(1, GT6RecipeMaps.MELTER.mRecipeList.stream().filter(r -> r.mInputs.length > 0 && r.mInputs[0].getItem() == tProbe).count());
	}

	// ------------------------------------------------------------- ⑥ the row ↔ central-face consistency pin

	/** The symptom29 anchor: the central query face and the recycling rows answer for the SAME
	 * stack — no data → no query answer AND no rows; one data write → the query face names the
	 * material and the Melter finds that stack's row through the real map. */
	@Test
	void recyclingRowsFaceTheCentralQueryConsistently() {
		Item tProbe = probeItem("recycling_probe_consistency");
		ItemStack tStack = new ItemStack(tProbe);
		stubMolten(MT.Cu);
		// the negative face: an unregistered item is unqueryable and unrecyclable
		assertNull(OM.anydata(tStack));
		assertTrue(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack).isEmpty());
		// the write: ONE declaration flips both faces together
		assertTrue(OM.setItemData(tStack, new OreDictItemData(OP.plate, MT.Cu)));
		assertEquals(MT.Cu, OM.anydata(tStack).mMaterial.mMaterial);
		Recipe tMelter = single(GT6RecyclingProcessing.rows(GT6RecipeMaps.MELTER, tStack));
		assertEquals(tProbe, tMelter.mInputs[0].getItem()); // the row is keyed to the stack the query answers for
		assertSame(tMelter, GT6RecipeMaps.MELTER.findRecipe(null, Long.MAX_VALUE, null, new FluidStack[0], tStack));
		// and the smelt target the row pours toward is the query's own material's target (the melt projection)
		assertEquals(MT.Cu.mTargetSmelting.mMaterial, MT.Cu);
	}

	// ------------------------------------------------------------- the helpers

	private static Recipe single(List<Recipe> aRows) {
		assertEquals(1, aRows.size());
		return aRows.get(0);
	}

	private static void stubMolten(OreDictMaterial... aMaterials) {
		GT6RecyclingProcessing.sMoltenFluid = aMaterial -> {
			for (OreDictMaterial tMaterial : aMaterials) if (tMaterial == aMaterial) return Fluids.WATER;
			return null;
		};
	}

	/**
	 * The offline probe item (the OMComponentFaceTest helper, mirrored): the Forge intrusive
	 * holder makes item construction/registration throw while the registry is frozen, so the
	 * probe registers under a dedicated probe id after the three forge locks open.
	 */
	private static Item probeItem(String aProbeId) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		Item rItem = new Item(new Item.Properties());
		net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
		return rItem;
	}

	/** getDeclaredField along the superclass chain (the FileSawTest helper, mirrored). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// walk up
			}
		}
		throw new NoSuchFieldException(aName);
	}
}
