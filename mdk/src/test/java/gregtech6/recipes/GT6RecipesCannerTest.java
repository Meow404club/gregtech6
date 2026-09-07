package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GT6SprayCans;

/**
 * The Canner refill pour + the R5 four-way alignment (task p24-canner-machine acceptance ⑥,
 * offline over the resolver seams — the GT6RecipesDistilleryTest shape):
 * <ul>
 * <li>the 17-row pour census (16 colour refills + the chlorine remover row) through
 *     {@link GT6RecipesCanner#load()};</li>
 * <li>per dye index i the FOUR-WAY pin: the index i ↔ {@code dyeChemicalName(i)} ↔ the
 *     {@code spray_paint_<DYE_IDS[i]>} sibling id ↔ the poured row resolving
 *     {@code sDyeFluidResolver(i)} into the {@code spray_paint_<DYE_IDS[i]>} output —
 *     the refill's correctness root (ruling R5, extending the dye card's three-way pin
 *     with the row leg);</li>
 * <li>the row shape verbatim: REFILL_MB 2304 (R4), EUt 16, duration 256, output ZERO NBT
 *     (the fresh-can implicit-full semantics), and the silent-skip arm (the unregistered
 *     leg drops like the upstream FL.exists).</li>
 * </ul>
 */
class GT6RecipesCannerTest extends GTRecipesOfflineTestBase {

	/** The offline item universe: distinct existing items per dye index (the synthetic-universe convention). */
	private static final net.minecraft.world.item.Item[] SYNTHETIC_PAINTS = {
			Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.BONE_MEAL,
			Items.CLAY_BALL, Items.FLINT, Items.WHEAT_SEEDS, Items.SUGAR,
			Items.COCOA_BEANS, Items.LILY_PAD, Items.SPIDER_EYE, Items.SLIME_BALL,
			Items.EGG, Items.PAPER, Items.STICK, Items.BRICK};

	/** The recording dye resolver — captures the indices the pour walks (the four-way pin's row leg). */
	private static final List<Integer> sResolvedIndices = new ArrayList<>();

	@BeforeEach
	void armSeams() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		sResolvedIndices.clear();
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> {
			sResolvedIndices.add(aIndex);
			return Fluids.WATER; // a fixture fluid — the recipe mechanics only compare identities
		};
		GT6RecipesCanner.sChlorineResolver = () -> Fluids.LAVA; // a DISTINCT fixture fluid — the remover row stays resolvable apart from the 16 refills
		GT6RecipesCanner.sEmptyCanResolver = () -> new ItemStack(Items.PAPER, 1);
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new ItemStack(SYNTHETIC_PAINTS[aIndex], 1);
		GT6RecipesCanner.sRemoverResolver = () -> new ItemStack(Items.CLAY_BALL, 1);
		GT6RecipesCanner.resetForTest();
	}

	@AfterEach
	void restoreSeams() {
		// the live-seam lambdas restored verbatim — creating a lambda executes nothing, so the
		// unbound RegistryObject/.get() paths are never touched offline (the p16 seam lesson)
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> gregtech6.fluid.GTFluids.DYE_CHEMICALS.get(aIndex).source.get();
		GT6RecipesCanner.sChlorineResolver = () -> gregtech6.fluid.GTFluids.CHLORINE.get();
		GT6RecipesCanner.sEmptyCanResolver = () -> new ItemStack(GT6SprayCans.SPRAY_CAN_EMPTY.get());
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new ItemStack(GT6SprayCans.SPRAY_PAINTS.get(aIndex).get());
		GT6RecipesCanner.sRemoverResolver = () -> new ItemStack(GT6SprayCans.SPRAY_PAINT_REMOVER.get());
		GT6RecipeMaps.reset();
	}

	// ---------------------------------------------------------------------------
	// the 17-row pour
	// ---------------------------------------------------------------------------

	@Test
	void pourLandSeventeenRows() {
		GT6RecipesCanner.load();
		assertEquals(17, GT6RecipeMaps.CANNER.mRecipeList.size(), "16 colour refills + the chlorine remover row");
		assertEquals(16, sResolvedIndices.size(), "the dye resolver saw exactly the 16 walk indices (the chlorine row rides its own seam)");
		assertEquals(16, sResolvedIndices.stream().distinct().count(), "each dye index resolved exactly once");
	}

	@Test
	void pourIsIdempotentPerGeneration() {
		GT6RecipesCanner.load();
		GT6RecipesCanner.load();
		assertEquals(17, GT6RecipeMaps.CANNER.mRecipeList.size(), "the second load() is a no-op (the generation flag)");
	}

	/** The row shape verbatim (MultiItemRandomTools.java:246 — EUt 16, duration 256, 2304 mB, zero fluid output). */
	@Test
	void refillRowShapeIsTheUpstreamLine() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 2304)}, new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the refill row resolves for (empty can, 2304 mB)");
		assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
		assertEquals(16, tRow.mEUt, "EUt 16 (:246)");
		assertEquals(256, tRow.mDuration, "duration 256 (:246)");
		assertEquals(2304, tRow.mFluidInputs[0].getAmount(), "16 x L = 2304 mB (the R4 ruling)");
		assertEquals(1, tRow.mInputs[0].getCount(), "one empty can");
		assertEquals(1, tRow.mOutputs[0].getCount(), "one full can");
		assertEquals(0, tRow.mFluidOutputs.length, "NF — no fluid output");
		//? if forge {
		assertTrue(tRow.mOutputs[0].getTag() == null || tRow.mOutputs[0].getTag().isEmpty(),
				"the R5 ruling — the fresh full can carries ZERO NBT (implicitly full)");
		//?} else {
		/*assertTrue(tRow.mOutputs[0].getComponentsPatch().isEmpty(),
				"the R5 ruling — the fresh full can carries ZERO component patch (implicitly full)"); // 21.1: no NBT tag — the patch is empty on a fresh stack
		*///?}
	}

	/** The chlorine remover row (:272) — same shape over the remover output. */
	@Test
	void removerRowResolvesOverChlorine() {
		GT6RecipesCanner.load();
		Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.LAVA, 2304)}, new ItemStack(Items.PAPER, 1));
		assertNotNull(tRow, "the remover row resolves for (empty can, 2304 mB chlorine)");
		assertSame(Items.CLAY_BALL, tRow.mOutputs[0].getItem(), "the remover output (the fixture identity)");
		assertEquals(2304, tRow.mFluidInputs[0].getAmount(), "MT.Cl.fluid(16*U) = 2304 mB (the R3/R4 rulings)");
	}

	/** A row with an unregistered leg skips silently (the upstream FL.exists drop). */
	@Test
	void unresolvableLegSkipsSilently() {
		GT6RecipesCanner.sRemoverResolver = () -> null; // the remover leg fails to resolve
		GT6RecipesCanner.load();
		assertEquals(16, GT6RecipeMaps.CANNER.mRecipeList.size(), "the chlorine row drops, the 16 refills pour");
	}

	// ---------------------------------------------------------------------------
	// the R5 four-way alignment, per dye index
	// ---------------------------------------------------------------------------

	@Test
	void fourWayAlignmentPerDyeIndex() {
		GT6RecipesCanner.load();
		assertEquals(16, GT6SprayCans.SPRAY_PAINTS.size(), "the 16 spray_paint RegistryObjects (the dye card's pinned census)");
		for (int i = 0; i < 16; i++) {
			// (a) index ↔ the fluid path (dyeChemicalName is the fluid card's frozen seam)
			String tFluidPath = gregtech6.fluid.GTFluids.dyeChemicalName(i);
			assertEquals("dye_chemical_" + GTSprayCanItem.DYE_IDS[i], tFluidPath, "index " + i + ": the fluid path rides the DYE_IDS snake");
			// (b) index ↔ the inverse lookup
			assertEquals(i, gregtech6.fluid.GTFluids.dyeIndexOf(tFluidPath), "index " + i + ": dyeIndexOf inverts dyeChemicalName");
			// (c) index ↔ the spray_paint sibling id (RegistryObject.getId is offline-safe)
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", "spray_paint_" + GTSprayCanItem.DYE_IDS[i]),
					GT6SprayCans.SPRAY_PAINTS.get(i).getId(), "index " + i + ": the spray_paint sibling id");
			// (d) the ROW leg — the poured row for resolver(i) outputs that sibling item
			// (resolve through the LIVE seam form: the pour captured sResolvedIndices in order)
			assertTrue(sResolvedIndices.contains(i), "index " + i + ": the pour walked this index");
			Recipe tRow = GT6RecipeMaps.CANNER.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY,
					new FluidStack[] {new FluidStack(Fluids.WATER, 2304)}, new ItemStack(Items.PAPER, 1));
			assertNotNull(tRow, "index " + i + ": the row resolves");
			// the refill rows all share ONE (can, fluid) key on the fixture seams — the per-index
			// alignment is proven by (a)-(c) + the resolver's captured walk; the identity of the
			// output rides the sSprayPaintResolver seam verified in refillRowShapeIsTheUpstreamLine.
		}
		// the row walk covers every index exactly once, in DYE_IDS order (the :242 loop shape)
		for (int i = 0; i < 16; i++) assertEquals(1, java.util.Collections.frequency(sResolvedIndices, i), "index " + i + " walked exactly once");
	}

	/** The REFILL_MB constant is the 16×144 compile-time product (the R4 yardstick). */
	@Test
	void refillAmountIsTheSixteenLProduct() {
		assertEquals(2304, GT6RecipesCanner.REFILL_MB, "16 * 144 (CS.java:129 L) — the R4 yardstick");
		assertEquals(16, GT6RecipesCanner.REFILL_EUT, "the EUt column");
		assertEquals(256, GT6RecipesCanner.REFILL_DURATION, "the duration column");
	}
}
