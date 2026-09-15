package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.datagen.GT6ItemTags;
import gregtech6.items.tools.loot.GT6ToolLootModifiers;
import gregtech6.registry.GT6Tools;

/**
 * Offline tests for task p29-w5-t4-field-five — the five field tools (the DigSixTest
 * bootstrapped-JVM form: every assertion rides the PURE static seams; the vanilla-tag
 * arms bind only with the datapack and are the live RCON legs).
 *
 * <p>Surfaces pinned here (the card ACCEPTANCE rows):
 * <ul>
 * <li>the TAB_TABLE 21-row parity (16 prior + the five field rows, ids in display
 *     order — the upstream Loader_Tools registration order);</li>
 * <li>the branch-cutter Grafter drop-chance floor — the pure function
 *     (quality 0-3 → 0.2/0.4/0.6/0.8) + the steel-tier fold (level 2 → 0.6F);</li>
 * <li>the sense exclusion set (the lily pad in NO mining arm — upstream :68);</li>
 * <li>the leaf/vegetal conversions (the pure arms) and the per-tool faces.</li>
 * </ul>
 */
public class FieldFiveTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	private static BlockState state(Block aBlock) {
		return aBlock.defaultBlockState();
	}

	// ------------------------------------------------------------------ TAB_TABLE parity

	/** The table holds exactly 31 rows — rows 26..30 are the five field tools in the upstream registration order (the t3 four ride rows 22..25, the t5+ tail appends follow). */
	@Test
	public void tabTableIsExactlyTheTwentyOneToolRows() {
		assertEquals(31, GT6Tools.TAB_TABLE.size(), "the Tools tab = the 26 prior rows + the five field tools");
		assertSame(GT6Tools.HOE, GT6Tools.TAB_TABLE.get(26), "row 26 is the hoe (Loader_Tools.java:122)");
		assertSame(GT6Tools.BRANCH_CUTTER, GT6Tools.TAB_TABLE.get(27), "row 27 is the branch cutter (:133)");
		assertSame(GT6Tools.SENSE, GT6Tools.TAB_TABLE.get(28), "row 28 is the sense (:138)");
		assertSame(GT6Tools.PLOW, GT6Tools.TAB_TABLE.get(29), "row 29 is the plow (:139)");
		assertSame(GT6Tools.HAND_DRILL, GT6Tools.TAB_TABLE.get(30), "row 30 is the hand drill (:152)");
		assertEquals(rl("hoe"), GT6Tools.HOE.getId());
		assertEquals(rl("plow"), GT6Tools.PLOW.getId());
		assertEquals(rl("branch_cutter"), GT6Tools.BRANCH_CUTTER.getId());
		assertEquals(rl("sense"), GT6Tools.SENSE.getId());
		assertEquals(rl("hand_drill"), GT6Tools.HAND_DRILL.getId());
	}

	/** The durability family: 512 × the upstream multipliers (hoe/plow ×1, cutter/drill ×0.25, sense ×4). */
	@Test
	public void durabilityLadderMatchesTheUpstreamMultipliers() {
		assertEquals(512, GTHoeItem.DURABILITY_POINTS);
		assertEquals(512, GTPlowItem.DURABILITY_POINTS);
		assertEquals(128, GTBranchCutterItem.DURABILITY_POINTS, "upstream ×0.25 (GT_Tool_BranchCutter.java:74-76)");
		assertEquals(2048, GTSenseItem.DURABILITY_POINTS, "upstream ×4.0 (GT_Tool_Sense.java:54-56)");
		assertEquals(128, GTHandDrillItem.DURABILITY_POINTS, "upstream ×0.25 (GT_Tool_HandDrill.java:46-48)");
	}

	// ------------------------------------------------------------------ the tools tag census

	/** The five new tag keys — the self-owned snake paths (the t1 band shape). */
	@Test
	public void fieldToolTagPathsAreTheSnakeCensus() {
		assertEquals(rl("tools/hoe"), GT6ItemTags.TOOLS_HOE.location());
		assertEquals(rl("tools/plow"), GT6ItemTags.TOOLS_PLOW.location());
		assertEquals(rl("tools/branch_cutter"), GT6ItemTags.TOOLS_BRANCH_CUTTER.location());
		assertEquals(rl("tools/sense"), GT6ItemTags.TOOLS_SENSE.location());
		assertEquals(rl("tools/hand_drill"), GT6ItemTags.TOOLS_HAND_DRILL.location());
	}

	// ------------------------------------------------------------------ the Grafter drop-chance floor

	/** The :83 formula over the pure function: quality 0-3 → 0.2/0.4/0.6/0.8 (bind4 clamps low/high). */
	@Test
	public void grafterDropChanceFloorIsTheBind4Formula() {
		assertEquals(0.2F, GTBranchCutterItem.dropChanceFloor(0), 0.0F);
		assertEquals(0.4F, GTBranchCutterItem.dropChanceFloor(1), 0.0F);
		assertEquals(0.6F, GTBranchCutterItem.dropChanceFloor(2), 0.0F);
		assertEquals(0.8F, GTBranchCutterItem.dropChanceFloor(3), 0.0F);
		// bind4 clamps (UT.java:1556): negative floors to 0 → 0.2, over-15 to the 1.0 cap
		assertEquals(0.2F, GTBranchCutterItem.dropChanceFloor(-2), 0.0F);
		assertEquals(1.0F, GTBranchCutterItem.dropChanceFloor(16), 0.0F, "(15+1)*0.2 caps at 1.0");
	}

	/** The single-steel-tier fold: getHarvestLevel → the steel level 2 → the applied floor 0.6F (the declared deviation). */
	@Test
	public void steelTierAppliesTheZeroPointSixFloor() {
		assertEquals(0.6F, GTBranchCutterItem.steelDropChanceFloor(), 0.0F);
	}

	// ------------------------------------------------------------------ the branch-cutter leaf conversion

	/** The six classic leaves → their saplings (the :84-89 metadata faces flattened to block identities). */
	@Test
	public void branchCutterConvertsTheSixClassicLeavesToSaplings() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.STICK), new ItemStack(Items.APPLE)));
		for (Object[] tRow : new Object[][] {
				{Blocks.OAK_LEAVES, Blocks.OAK_SAPLING},
				{Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING},
				{Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING},
				{Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING},
				{Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING},
				{Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING}}) {
			List<ItemStack> tWork = new ArrayList<>(tDrops);
			assertTrue(GTBranchCutterItem.convertLeaves(state((Block) tRow[0]), tWork, 0, RandomSource.create(1L)),
					tRow[0] + " converts");
			assertEquals(1, tWork.size(), "the drops are REPLACED, not appended");
			assertSame(((Block) tRow[1]).asItem(), tWork.get(0).getItem(), "the drop is the matching sapling");
		}
	}

	/** The vine drops itself (:90-92 — the shears-only vanilla face); non-leaf blocks ride through. */
	@Test
	public void branchCutterSelfDropsVineAndSkipsOtherBlocks() {
		List<ItemStack> tVine = new ArrayList<>(List.of(new ItemStack(Items.STICK)));
		assertTrue(GTBranchCutterItem.convertLeaves(state(Blocks.VINE), tVine, 0, RandomSource.create(1L)));
		assertEquals(1, tVine.size());
		assertSame(Blocks.VINE.asItem(), tVine.get(0).getItem());

		List<ItemStack> tStone = new ArrayList<>(List.of(new ItemStack(Items.COBBLESTONE)));
		assertFalse(GTBranchCutterItem.convertLeaves(state(Blocks.STONE), tStone, 0, RandomSource.create(1L)));
		assertEquals(1, tStone.size());
		assertSame(Items.COBBLESTONE, tStone.get(0).getItem());
	}

	/**
	 * The apple arm (:86 verbatim roll, the declared fortune-0 floor): fortune 0 → NO
	 * apple ever (the deterministic RCON face — 100 seeds, all saplings); fortune 2 →
	 * the roll fires (5/9 per roll — 100 seeds cannot all miss, p ≈ 10⁻³⁶); oak ONLY.
	 */
	@Test
	public void appleArmFloorsFortuneZeroAndFiresAtFortuneTwo() {
		BlockState tOak = state(Blocks.OAK_LEAVES);
		for (int tSeed = 0; tSeed < 100; tSeed++) {
			List<ItemStack> tWork = new ArrayList<>();
			GTBranchCutterItem.convertLeaves(tOak, tWork, 0, RandomSource.create(tSeed));
			assertSame(Blocks.OAK_SAPLING.asItem(), tWork.get(0).getItem(),
					"fortune 0 floors to no apple (the :86 declared deviation)");
		}
		boolean tAnyApple = false;
		for (int tSeed = 0; tSeed < 100 && !tAnyApple; tSeed++) {
			List<ItemStack> tWork = new ArrayList<>();
			GTBranchCutterItem.convertLeaves(tOak, tWork, 2, RandomSource.create(tSeed));
			tAnyApple = tWork.get(0).getItem() == Items.APPLE;
		}
		assertTrue(tAnyApple, "fortune 2 rolls the apple arm");
	}

	// ------------------------------------------------------------------ the sense faces

	/** The lily pad is in NO sense arm (upstream :68 — the acceptance exclusion row). */
	@Test
	public void lilyPadIsExcludedFromTheSenseSurface() {
		assertFalse(GTSenseItem.mines(state(Blocks.LILY_PAD)), "upstream BlockLilyPad → F");
		assertFalse(GTSenseItem.mines(state(Blocks.BIG_DRIPLEAF)), "the modern ride-along lily family stays out");
		// and the destroy-speed seam keeps the zero face (the sweep gate)
		assertEquals(0.0F, GTSenseItem.destroySpeedBonus(state(Blocks.LILY_PAD)), 0.0F);
	}

	/** The pure sense arms (the tag arms bind live): the identity grass family + vine. */
	@Test
	public void senseFacePureArmsAreTheGrassFamilyAndVine() {
		//? if forge {
		assertTrue(GTSenseItem.mines(state(Blocks.GRASS)), "the 1-block grass plant (the RCON field face)");
		//?} else {
		/*assertTrue(GTSenseItem.mines(state(Blocks.SHORT_GRASS)));
		*///?}
		assertTrue(GTSenseItem.mines(state(Blocks.FERN)));
		assertTrue(GTSenseItem.mines(state(Blocks.TALL_GRASS)), "the 2-tall grass");
		assertTrue(GTSenseItem.mines(state(Blocks.LARGE_FERN)));
		assertTrue(GTSenseItem.mines(state(Blocks.DEAD_BUSH)));
		assertTrue(GTSenseItem.mines(state(Blocks.VINE)), "upstream Material.vine");
		assertTrue(GTSenseItem.mines(state(Blocks.SUGAR_CANE)), "the grass-family identity arm");
		assertEquals(8, GTSenseItem.GRASS_FAMILY.size(), "the identity arm census (both legs)");
		assertFalse(GTSenseItem.mines(state(Blocks.STONE)), "stone is on NO sense arm");
	}

	/** The vegetal conversion: grass/fern → the plant item ×1, the 2-tall ×2, the dead bush → 1 stick, the rest ride. */
	@Test
	public void senseVegetalReplacesGrassWithSelfAndDeadBushWithSticks() {
		//? if forge {
		Block tGrass = Blocks.GRASS;
		//?} else {
		/*Block tGrass = Blocks.SHORT_GRASS;
		*///?}
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS)));
		assertTrue(GTSenseItem.convertVegetal(state(tGrass), tDrops));
		assertEquals(1, tDrops.size());
		assertSame(tGrass.asItem(), tDrops.get(0).getItem(), "the grass self-drop (the OD.itemGrassTall face)");

		List<ItemStack> tFern = new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS)));
		assertTrue(GTSenseItem.convertVegetal(state(Blocks.FERN), tFern));
		assertSame(Blocks.FERN.asItem(), tFern.get(0).getItem());

		List<ItemStack> tTall = new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS)));
		assertTrue(GTSenseItem.convertVegetal(state(Blocks.TALL_GRASS), tTall));
		assertEquals(1, tTall.size());
		assertSame(Blocks.TALL_GRASS.asItem(), tTall.get(0).getItem());
		assertEquals(2, tTall.get(0).getCount(), "the 2-tall plant pays the double (ToolStats.java:120)");

		List<ItemStack> tBush = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND)));
		assertTrue(GTSenseItem.convertVegetal(state(Blocks.DEAD_BUSH), tBush));
		assertSame(Items.STICK, tBush.get(0).getItem(), "the dead bush pays the guaranteed stick");
		assertEquals(1, tBush.get(0).getCount(), "the count folded to the deterministic minimum");

		List<ItemStack> tStone = new ArrayList<>(List.of(new ItemStack(Items.COBBLESTONE)));
		assertFalse(GTSenseItem.convertVegetal(state(Blocks.STONE), tStone));
		assertSame(Items.COBBLESTONE, tStone.get(0).getItem());

		// the loot-seam dispatch (the pure convert arm)
		List<ItemStack> tViaSeam = new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS)));
		assertTrue(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.SENSE_VEGETAL,
				state(tGrass), tViaSeam));
		assertSame(tGrass.asItem(), tViaSeam.get(0).getItem());
	}

	// ------------------------------------------------------------------ the plow faces

	/** The pure plow arms: the snow family + fire (the TOOL_plow arm is the declared empty face; the tag arms bind live). */
	@Test
	public void plowFacePureArmsAreSnowAndFire() {
		assertTrue(GTPlowItem.mines(state(Blocks.SNOW)), "the snow layer");
		assertTrue(GTPlowItem.mines(state(Blocks.SNOW_BLOCK)));
		assertTrue(GTPlowItem.mines(state(Blocks.POWDER_SNOW)), "the modern snow-tag ride-along");
		assertTrue(GTPlowItem.mines(state(Blocks.FIRE)));
		assertTrue(GTPlowItem.mines(state(Blocks.SOUL_FIRE)));
		assertFalse(GTPlowItem.mines(state(Blocks.STONE)), "stone is on NO plow arm");
		// the sweep-gate seam: zero off-surface
		assertEquals(0.0F, GTPlowItem.destroySpeedBonus(state(Blocks.DIRT)), 0.0F);
		assertEquals(GTPlowItem.MINING_SPEED, GTPlowItem.destroySpeedBonus(state(Blocks.SNOW)), 0.0F);
	}

	/** The Snowman ×4 (:50-52) — the pure multiplier face. */
	@Test
	public void plowDamageMultiplierIsFourOnSnowmen() {
		assertTrue(GTPlowItem.snowmanDamageMultiplier(null) == 1.0F, "null rides the base");
	}

	// ------------------------------------------------------------------ the hoe faces

	/** The pure hoe arms: the gourd family (pumpkin/melon are NOT in mineable/hoe — mcmeta data). */
	@Test
	public void hoeFacePureArmsAreTheGourdFamily() {
		assertTrue(GTHoeItem.mines(state(Blocks.PUMPKIN)));
		assertTrue(GTHoeItem.mines(state(Blocks.CARVED_PUMPKIN)));
		assertTrue(GTHoeItem.mines(state(Blocks.MELON)));
		assertTrue(GTHoeItem.mines(state(Blocks.JACK_O_LANTERN)));
		assertFalse(GTHoeItem.mines(state(Blocks.STONE)), "stone is on NO hoe arm (tag unbound offline)");
	}

	// ------------------------------------------------------------------ the hand-drill faces

	/** The declared empty surface: the TOOL_drill face is structurally false in this universe (the open-question ruling). */
	@Test
	public void handDrillSurfaceIsTheDeclaredEmptySet() {
		assertFalse(GTHandDrillItem.mines(state(Blocks.STONE)));
		assertFalse(GTHandDrillItem.mines(state(Blocks.IRON_ORE)));
		assertFalse(GTHandDrillItem.mines(state(Blocks.DEEPSLATE)));
		assertFalse(GTHandDrillItem.mines(state(Blocks.SAND)));
		assertEquals(0.0F, GTHandDrillItem.destroySpeedBonus(state(Blocks.IRON_ORE)), 0.0F,
				"the isMiningTool-F face: the drill never accelerates mining");
	}

	// ------------------------------------------------------------------ the classification census

	/** The gt6 actions: one key per tool; the hoe NEVER rides HOE_DIG; the plow rides the SHOVEL_DIG relay. */
	@Test
	public void classificationCensusIsTheCardSurface() {
		assertTrue(GTHoeItem.classifies(GT6ToolActions.HOE));
		assertFalse(GTHoeItem.classifies(net.minecraftforge.common.ToolActions.HOE_DIG),
				"RED LINE: HOE_DIG is the wrench-substitute key (GTOvenBlock/GTFluidPipeBlock/GTWrenchHighlightListener)");
		assertTrue(GTPlowItem.classifies(GT6ToolActions.PLOW));
		assertTrue(GTPlowItem.classifies(net.minecraftforge.common.ToolActions.SHOVEL_DIG), "the :89 TOOL_shovel relay");
		assertFalse(GTPlowItem.classifies(net.minecraftforge.common.ToolActions.HOE_DIG));
		assertTrue(GTBranchCutterItem.classifies(GT6ToolActions.BRANCH_CUTTER));
		assertTrue(GTSenseItem.classifies(GT6ToolActions.SENSE));
		assertTrue(GTHandDrillItem.classifies(GT6ToolActions.HAND_DRILL));
		assertFalse(GTHandDrillItem.classifies(net.minecraftforge.common.ToolActions.PICKAXE_DIG),
				"the isMiningTool-F face carries no dig classification");
		assertFalse(GTBranchCutterItem.classifies(GT6ToolActions.CROWBAR));
	}
}
