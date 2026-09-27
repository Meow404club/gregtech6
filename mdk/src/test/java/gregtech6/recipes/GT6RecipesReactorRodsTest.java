package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.items.GT6ReactorRods;
import gregtech6.items.GT6ReactorRods.RodRow;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;
import gregtech6.reactor.ReactorRodNbt;
import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.ReactorRodKind;
import gregtech6.reactor.neutron.FuelRodSpec;

/**
 * The reactor-rod recipe pour test (task debt-reactor-c-rods, acceptance ③): the 25
 * Canner rows (3 functional + 17 fuel + 4 breeder fills + the Tritium unpack) and the
 * 20 Centrifuge rows (17 depleted + 3 solid enriched) pinned in count and shape over
 * the resolver seams (the GT6RecipesCannerTest posture — synthetic universes offline),
 * plus the LIVE resolvability ratchet: every (prefix, material) leg of the row tables
 * must be in {@link GTMaterialItems#registrationOrder()}, so a poured-on-silicon-only
 * drift cannot silently shrink the live pour (the unresolvable-leg drop stays the
 * upstream posture, but the legs must EXIST).
 */
class GT6RecipesReactorRodsTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void initMaterialGeneration() {
		// the lazy row tables snapshot the MT/OP statics — they must build against an
		// INITIALISED generation (the GTMaterialItemsForceTest posture)
		GTMaterialItems.initMaterials();
	}

	/** The offline rod universe: one distinct existing item per upstream rod id (the synthetic-universe convention). */
	private static final Item[] SYNTHETIC_RODS = {
			Items.PAPER, Items.CLAY_BALL, Items.BRICK, Items.SNOWBALL,
			Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.BONE,
			Items.COD_BUCKET, Items.LILY_PAD, Items.WHEAT_SEEDS, Items.COCOA_BEANS,
			Items.SPIDER_EYE, Items.SLIME_BALL, Items.EGG, Items.LEATHER,
			Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.NETHERITE_SCRAP,
			Items.QUARTZ, Items.AMETHYST_SHARD, Items.OBSIDIAN, Items.CARROT,
			Items.POTATO, Items.BEETROOT, Items.APPLE, Items.MELON_SLICE,
			Items.PUMPKIN, Items.WHEAT, Items.OAK_PLANKS, Items.COBBLESTONE,
			Items.DIRT, Items.GRAVEL, Items.SAND, Items.GLASS_BOTTLE,
			Items.BOWL, Items.STRING, Items.FEATHER, Items.RABBIT_HIDE,
			Items.HONEYCOMB, Items.CHARCOAL, Items.BAMBOO, Items.KELP,
			Items.CACTUS, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.COCOA_BEANS};
	private static final Item SYNTH_STICK = Items.STICK;
	private static final Item SYNTH_BOLT = Items.BONE_MEAL;
	private static final Item SYNTH_SCRAP = Items.FLINT;
	private static final Item SYNTH_TINY = Items.WHEAT_SEEDS;
	private static final Item SYNTH_DIV72 = Items.SUGAR;
	private static final Fluid SYNTH_TRITIUM = Fluids.FLOWING_WATER;

	/** 46 synthetic rods keyed by the upstream ids (the resolver seam fixture). */
	private static Map<Integer, ItemStack> sRods;

	@BeforeEach
	void armSeams() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		sRods = new java.util.LinkedHashMap<>();
		for (int i = 0; i < GT6ReactorRods.ROWS.size(); i++) {
			sRods.put(GT6ReactorRods.ROWS.get(i).id(), new ItemStack(SYNTHETIC_RODS[i], 1));
		}
		GT6RecipesReactorRods.sRodResolver = aId -> {
			ItemStack tStack = sRods.get(aId);
			return tStack == null ? null : tStack.copy();
		};
		// the material legs: distinct fixture items per prefix family (never colliding with
		// the rod ids or each other in findRecipe space)
		GT6RecipesReactorRods.sMaterialResolver = (aPrefix, aMaterial) -> new ItemStack(
				aPrefix == OP.stick ? SYNTH_STICK : aPrefix == OP.bolt ? SYNTH_BOLT
				: aPrefix == OP.scrapGt ? SYNTH_SCRAP : aPrefix == OP.dustTiny ? SYNTH_TINY
				: SYNTH_DIV72, 1);
		GT6RecipesReactorRods.sTritiumResolver = () -> SYNTH_TRITIUM;
		GT6RecipesReactorRods.resetForTest();
	}

	@AfterEach
	void restoreSeams() {
		// the live-seam lambdas restored verbatim — creating a lambda executes nothing, so
		// the unbound RegistryObject/.get() paths are never touched offline (the p16 lesson)
		GT6RecipesReactorRods.sRodResolver = GT6RecipesReactorRods::liveRod;
		GT6RecipesReactorRods.sMaterialResolver = GT6RecipesReactorRods::liveMaterial;
		GT6RecipesReactorRods.sTritiumResolver = GT6RecipesReactorRods::liveTritium;
	}

	/** The 24 fill rows + the Tritium unpack pour into CANNER, idempotent per generation. */
	@Test
	void theCannerPourIsTwentyFiveRows() {
		GT6RecipesReactorRods.load();
		assertEquals(25, GT6RecipeMaps.CANNER.mRecipeList.size(),
				":742-744 the 3 functional + :746-762 the 17 fuel + :782-785 the 4 breeder + :789 the Tritium unpack");
		GT6RecipesReactorRods.load();
		assertEquals(25, GT6RecipeMaps.CANNER.mRecipeList.size(), "the second load() is a no-op (the generation flag)");
	}

	/** The 17 depleted + 3 enriched rows pour into CENTRIFUGE. */
	@Test
	void theCentrifugePourIsTwentyRows() {
		GT6RecipesReactorRods.load();
		assertEquals(20, GT6RecipeMaps.CENTRIFUGE.mRecipeList.size(),
				":764-780 the 17 depleted + :787/:788/:790 the 3 solid enriched");
	}

	/** The fuel fill row shape: [stick, empty rod] in, NOT buffered, EUt 16/16t, the rod out with gt.maxdurability. */
	@Test
	void fuelFillRowShapeAndFreshRodBudget() {
		GT6RecipesReactorRods.load();
		Recipe tRow = rowWithOutput(sRods.get(9210).getItem());
		assertNotNull(tRow, "the Th-232 fill row");
		assertFalse(tRow.mCanBeBuffered, "the upstream F buffered column");
		assertEquals(16, tRow.mEUt, ":746 the EUt column");
		assertEquals(16, tRow.mDuration, ":746 the duration column");
		assertEquals(2, tRow.mInputs.length, "stick + empty rod");
		assertEquals(SYNTH_STICK, tRow.mInputs[0].getItem(), "the stick leg");
		assertEquals(1, tRow.mInputs[0].getCount(), "the stick count");
		assertEquals(sRods.get(9201).getItem(), tRow.mInputs[1].getItem(), "the empty-rod leg (id 9201)");
		assertEquals(1, tRow.mOutputs.length, "the filled rod alone");
		assertEquals(12_000_000_000L, ReactorRodNbt.durability(tRow.mOutputs[0]),
				"the output carries gt.maxdurability (the registration-NBT bake, Nuclear.java:49 fallback)");
	}

	/** The breeder fill row shape: bolt×4 in, the rod out budgeted with the needed column. */
	@Test
	void breederFillRowShapeAndFreshRodBudget() {
		GT6RecipesReactorRods.load();
		Recipe tRow = rowWithOutput(sRods.get(9410).getItem());
		assertNotNull(tRow, "the Th-232 breeder fill row");
		assertEquals(SYNTH_BOLT, tRow.mInputs[0].getItem(), "the bolt leg");
		assertEquals(4, tRow.mInputs[0].getCount(), ":782 the bolt×4 count");
		assertEquals(64_000_000L, ReactorRodNbt.durability(tRow.mOutputs[0]),
				"the output budget = NBT_MAXDURABILITY = the needed column");
		// the Naquadah outlier (4_096_000_000L, :785)
		Recipe tNq = rowWithOutput(sRods.get(9440).getItem());
		assertEquals(4_096_000_000L, ReactorRodNbt.durability(tNq.mOutputs[0]), "the :785 budget");
	}

	/** The functional fills carry NO budget (the upstream registration has no NBT_MAXDURABILITY there). */
	@Test
	void functionalFillRowsCarryNoBudget() {
		GT6RecipesReactorRods.load();
		for (int tId : new int[] {9202, 9203, 9204}) {
			Recipe tRow = rowWithOutput(sRods.get(tId).getItem());
			assertNotNull(tRow, "the functional fill row " + tId);
			assertEquals(0L, ReactorRodNbt.durability(tRow.mOutputs[0]), "no burn budget on " + tId);
		}
	}

	/** The Tritium unpack (:789): the enriched rod in, NO fluid in, 500 mB tritium + the empty rod out. */
	@Test
	void tritiumUnpackRowShape() {
		GT6RecipesReactorRods.load();
		Recipe tRow = GT6RecipeMaps.CANNER.mRecipeList.stream()
				.filter(r -> r.mFluidOutputs.length > 0).findFirst().orElse(null);
		assertNotNull(tRow, "the :789 row is the only Canner row with a fluid output");
		assertEquals(1, tRow.mInputs.length, "the enriched rod alone");
		assertEquals(sRods.get(9431).getItem(), tRow.mInputs[0].getItem(), "the Tritium Enriched Rod input");
		assertEquals(0, tRow.mFluidInputs.length, "no fluid inputs");
		assertEquals(1, tRow.mFluidOutputs.length, "the tritium output");
		assertEquals(500, tRow.mFluidOutputs[0].getAmount(), "FL.amount(MT.T.mGas, 500L) mB 1:1");
		assertEquals(SYNTH_TRITIUM, tRow.mFluidOutputs[0].getFluid(), "the tritium leg");
		assertEquals(1, tRow.mOutputs.length, "the empty rod alone in the item outputs");
		assertEquals(sRods.get(9201).getItem(), tRow.mOutputs[0].getItem(), "the empty-rod output");
	}

	/** The depleted centrifuge shape: [scrapGt×9, dustTiny×1, dustDiv72×6] out at EUt 64/256t. */
	@Test
	void depletedCentrifugeRowShape() {
		GT6RecipesReactorRods.load();
		Recipe tRow = rowWithInput(sRods.get(9310).getItem(), GT6RecipeMaps.CENTRIFUGE);
		assertNotNull(tRow, "the Depleted Th-232 row");
		assertEquals(64, tRow.mEUt, ":764 the EUt column");
		assertEquals(256, tRow.mDuration, ":764 the duration column");
		assertEquals(1, tRow.mInputs.length, "the rod alone");
		assertEquals(3, tRow.mOutputs.length, "scrap + tiny + div72");
		assertEquals(SYNTH_SCRAP, tRow.mOutputs[0].getItem(), "the scrapGt(Zr) leg");
		assertEquals(9, tRow.mOutputs[0].getCount(), "the scrapGt(Zr, 9) count");
		assertEquals(SYNTH_TINY, tRow.mOutputs[1].getItem(), "the dustTiny(Th) leg");
		assertEquals(1, tRow.mOutputs[1].getCount(), "the dustTiny(mat, 1) count");
		assertEquals(SYNTH_DIV72, tRow.mOutputs[2].getItem(), "the dustDiv72(U_238) leg");
		assertEquals(6, tRow.mOutputs[2].getCount(), "the dustDiv72(mat, 6) count");
	}

	/** The enriched centrifuge shapes: 4-of-each columns; the :790 Naquadah outlier rides EUt 512. */
	@Test
	void enrichedCentrifugeRowShapes() {
		GT6RecipesReactorRods.load();
		Recipe tU_233 = rowWithInput(sRods.get(9411).getItem(), GT6RecipeMaps.CENTRIFUGE);
		assertNotNull(tU_233, "the U-233 enriched row (:787)");
		assertEquals(4, tU_233.mOutputs[1].getCount(), ":787 the dustTiny(mat, 4) count");
		assertEquals(4, tU_233.mOutputs[2].getCount(), ":787 the dustDiv72(mat, 4) count");
		assertEquals(64, tU_233.mEUt, ":787 the EUt column");
		Recipe tNq = rowWithInput(sRods.get(9441).getItem(), GT6RecipeMaps.CENTRIFUGE);
		assertNotNull(tNq, "the Naquadah enriched row (:790)");
		assertEquals(512, tNq.mEUt, ":790 the family-outlier EUt column");
	}

	/**
	 * The LIVE resolvability ratchet: every (prefix, material) leg of the pour tables is
	 * in the registration order — the offline boot fills the material system, the row
	 * tables resolve against it (the lazy-table discipline), and any leg gone missing
	 * from the item path goes red here instead of shrinking the live pour silently.
	 */
	@Test
	void everyMaterialLegIsInTheLiveRegistrationOrder() {
		GTMaterialItems.initMaterials();
		List<PrefixMaterial> tUniverse = GTMaterialItems.registrationOrder();
		Map<PrefixMaterial, Long> tIndex = tUniverse.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		for (GT6RecipesReactorRods.FillRow tRow : GT6RecipesReactorRods.fillRows()) {
			PrefixMaterial tPair = new PrefixMaterial(tRow.prefix(), tRow.material());
			assertNotNull(tIndex.get(tPair), "the fill leg missing from the item path: " + tPair);
		}
		assertNotNull(tIndex.get(new PrefixMaterial(OP.scrapGt, MT.Zr)), "the Zr scrap leg");
		for (GT6RecipesReactorRods.CentrifugeRow tRow : GT6RecipesReactorRods.centrifugeRows()) {
			assertNotNull(tIndex.get(new PrefixMaterial(OP.dustTiny, tRow.tinyMat())), "the tiny leg of " + tRow.rodId());
			assertNotNull(tIndex.get(new PrefixMaterial(OP.dustDiv72, tRow.div72Mat())), "the div72 leg of " + tRow.rodId());
		}
	}

	/** The row-table/material-system boundary sanity (both sides offline-resolvable). */
	@Test
	void fillTableCoversEveryFillableRod() {
		GTMaterialItems.initMaterials();
		assertEquals(24, GT6RecipesReactorRods.fillRows().size(), "3 functional + 17 fuel + 4 breeder");
		assertEquals(20, GT6RecipesReactorRods.centrifugeRows().size(), "17 depleted + 3 enriched");
		// every FUEL/BREEDER id has exactly one fill row; every DEPLETED/solid-PRODUCT id exactly one centrifuge row
		for (RodRow tRow : GT6ReactorRods.ROWS) {
			long tFills = GT6RecipesReactorRods.fillRows().stream().filter(r -> r.rodId() == tRow.id()).count();
			long tCent = GT6RecipesReactorRods.centrifugeRows().stream().filter(r -> r.rodId() == tRow.id()).count();
			switch (tRow.kind()) {
				case ABSORBER, REFLECTOR, MODERATOR, FUEL -> assertEquals(1L, tFills, "the fill row of " + tRow.id());
				case BREEDER -> assertEquals(1L, tFills, "the fill row of " + tRow.id());
				case DEPLETED -> assertEquals(1L, tCent, "the centrifuge row of " + tRow.id());
				case PRODUCT -> assertEquals(tRow.id() == 9431 ? 0L : 1L, tCent,
						":789 Tritium unpacks in the Canner, the other three centrifuge");
				default -> assertEquals(0L, tFills + tCent, "the Empty Rod has no recipe");
			}
			assertTrue(tCent == 0 || tRow.kind() == ReactorRodKind.DEPLETED || tRow.kind() == ReactorRodKind.PRODUCT,
					"only depleted/enriched rods centrifuge: " + tRow.id());
		}
	}

	/** The breeding-chain material edges: the depleted tiny = the fuel's own material identity. */
	@Test
	void depletedCentrifugeTinyLegsMatchTheFuelIdentities() {
		GTMaterialItems.initMaterials();
		for (FuelRodSpec tSpec : FuelRodSpec.RODS) {
			GT6RecipesReactorRods.CentrifugeRow tRow = centrifugeRow(tSpec.depletedId());
			assertNotNull(tRow, "the depleted row of " + tSpec.depletedId());
			// the row identity is by-id; the tiny leg must be the fuel's own material (the
			// MT static the A-card row names) — resolved through the fill row of the SAME fuel
			GT6RecipesReactorRods.FillRow tFill = fillRow(tSpec.id());
			assertNotNull(tFill, "the fill row of " + tSpec.id());
			assertEquals(tFill.material(), tRow.tinyMat(), "the tiny leg = the fuel material of " + tSpec.id());
		}
	}

	/** An unresolvable leg skips SILENTLY (the upstream mat() null drop, the count shows it). */
	@Test
	void unresolvableLegSkipsSilently() {
		GT6RecipesReactorRods.sMaterialResolver = (aPrefix, aMaterial) -> aPrefix == OP.bolt ? null : new ItemStack(SYNTH_STICK, 1);
		GT6RecipesReactorRods.load();
		assertEquals(21, GT6RecipeMaps.CANNER.mRecipeList.size(), "25 minus the 4 bolt-less breeder rows");
	}

	// ---------------------------------------------------------------------------

	private static Recipe rowWithOutput(Item aItem) {
		return rowWithOutput(aItem, GT6RecipeMaps.CANNER);
	}

	private static Recipe rowWithOutput(Item aItem, RecipeMap aMap) {
		return aMap.mRecipeList.stream().filter(r -> r.mOutputs.length > 0 && r.mOutputs[0].getItem() == aItem)
				.findFirst().orElse(null);
	}

	private static Recipe rowWithInput(Item aItem, RecipeMap aMap) {
		return aMap.mRecipeList.stream().filter(r -> r.mInputs.length > 0 && r.mInputs[0].getItem() == aItem)
				.findFirst().orElse(null);
	}

	private static GT6RecipesReactorRods.FillRow fillRow(int aRodId) {
		return GT6RecipesReactorRods.fillRows().stream().filter(r -> r.rodId() == aRodId).findFirst().orElse(null);
	}

	private static GT6RecipesReactorRods.CentrifugeRow centrifugeRow(int aRodId) {
		return GT6RecipesReactorRods.centrifugeRows().stream().filter(r -> r.rodId() == aRodId).findFirst().orElse(null);
	}
}
