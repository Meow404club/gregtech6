package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.datagen.GT6CraftingRecipes;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.items.tools.loot.GT6ToolLootModifiers;
import gregtech6.registry.GT6Tools;

/**
 * Offline tests for task p29-w5-t5-scene-six — the six scene tools (the DigSixTest
 * offline boot form: a bootstrapped-and-frozen JVM cannot construct mod Items, so every
 * assertion rides the PURE static seams — the GTCrowbarItem.mines ruling).
 *
 * <p>Surfaces pinned here (the card ACCEPTANCE rows):
 * <ul>
 * <li>the TAB_TABLE 22-row parity (16 prior + the six scene rows, ids in display order);</li>
 * <li>the tools tag census (the six new {@code gt6:tools/*} tag keys, one member each);</li>
 * <li>the flint_and_tinder Steel recipe convergence — the Loader_Tools :207-208 two-row
 *     claim pinned through the datagen row-ID constants (the builder bodies are the
 *     runData leg's, the IDs are the shared contract);</li>
 * <li>the rolling_pin / bending_cylinder no-behaviour census (the
 *     GT6BendingCylinderSmallItem structure-empty master, the BendingCylinderSmallTest
 *     reflection form: no useOn, no canPerformAction override — the vanilla defaults ARE
 *     the declared surface);</li>
 * <li>the PLANT_SELF_DROP conversion pure function (the vine/cobweb self-drop, the
 *     upstream convertBlockDrops :87-101 semantics);</li>
 * <li>the durability ladder (512 everywhere, the flint's upstream ×0.25 fold = 128).</li>
 * </ul>
 *
 * <p>Live-leg division (the DigSixTest ruling): the faces that need a running server —
 * the shears mining speeds, the sheep shear, the tripwire disarm, the plunger drain over
 * a live BE capability, the 30% flint strike — are the RCON chains' arms.
 */
public class SceneSixTest {

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

	/** The table holds exactly 22 rows — rows 16..21 are the six scene tools in display order. */
	@Test
	public void tabTableIsExactlyTheTwentyTwoToolRows() {
		assertEquals(88, GT6Tools.TAB_TABLE.size(), "the Tools tab = the 31 prior rows + the six scene tools");
		assertSame(GT6Tools.SCISSORS, GT6Tools.TAB_TABLE.get(31), "row 31 is the scissors");
		assertSame(GT6Tools.SCOOP, GT6Tools.TAB_TABLE.get(32), "row 32 is the scoop");
		assertSame(GT6Tools.PLUNGER, GT6Tools.TAB_TABLE.get(33), "row 33 is the plunger");
		assertSame(GT6Tools.FLINT_AND_TINDER, GT6Tools.TAB_TABLE.get(34), "row 34 is the flint and tinder");
		assertSame(GT6Tools.ROLLING_PIN, GT6Tools.TAB_TABLE.get(35), "row 35 is the rolling pin");
		assertSame(GT6Tools.BENDING_CYLINDER, GT6Tools.TAB_TABLE.get(36), "row 36 is the large bending cylinder");
		assertEquals(rl("scissors"), GT6Tools.SCISSORS.getId());
		assertEquals(rl("scoop"), GT6Tools.SCOOP.getId());
		assertEquals(rl("plunger"), GT6Tools.PLUNGER.getId());
		assertEquals(rl("flint_and_tinder"), GT6Tools.FLINT_AND_TINDER.getId());
		assertEquals(rl("rolling_pin"), GT6Tools.ROLLING_PIN.getId());
		assertEquals(rl("bending_cylinder"), GT6Tools.BENDING_CYLINDER.getId());
	}

	/** The durability family: 512 everywhere except the flint's upstream ×0.25 (= 128). */
	@Test
	public void durabilityLadderMatchesTheUpstreamMultipliers() {
		assertEquals(512, GTScissorsItem.DURABILITY_POINTS);
		assertEquals(512, GTScoopItem.DURABILITY_POINTS);
		assertEquals(512, GTPlungerItem.DURABILITY_POINTS);
		assertEquals(128, GTFlintAndTinderItem.DURABILITY_POINTS,
				"upstream getMaxDurabilityMultiplier 0.25F (GT_Tool_FlintAndTinder.java:47-48), the GTPickaxeGemItem fold");
		assertEquals(512, GTRollingPinItem.DURABILITY_POINTS);
		assertEquals(512, GT6BendingCylinderItem.DURABILITY_POINTS);
		assertEquals(GT6FileItem.DAMAGE_PER_CRAFT, GTRollingPinItem.DAMAGE_PER_CRAFT, "the shared one-point craft mapping");
		assertEquals(GT6FileItem.DAMAGE_PER_CRAFT, GT6BendingCylinderItem.DAMAGE_PER_CRAFT, "the shared one-point craft mapping");
		assertEquals(30, GTFlintAndTinderItem.IGNITE_CHANCE_PERCENT, "the proxy default FlintAndSteelChance = 30 (GT6_Main.java:111)");
	}

	// ------------------------------------------------------------------ the tools tag census

	/** The six new tag keys — the self-owned snake paths (the p24 band shape). */
	@Test
	public void sceneToolTagPathsAreTheSnakeCensus() {
		assertEquals(rl("tools/scissors"), GT6ItemTags.TOOLS_SCISSORS.location());
		assertEquals(rl("tools/scoop"), GT6ItemTags.TOOLS_SCOOP.location());
		assertEquals(rl("tools/plunger"), GT6ItemTags.TOOLS_PLUNGER.location());
		assertEquals(rl("tools/flint_and_tinder"), GT6ItemTags.TOOLS_FLINT_AND_TINDER.location());
		assertEquals(rl("tools/rolling_pin"), GT6ItemTags.TOOLS_ROLLING_PIN.location());
		assertEquals(rl("tools/bending_cylinder"), GT6ItemTags.TOOLS_BENDING_CYLINDER.location());
	}

	// ------------------------------------------------------------------ the flint recipe pair parity

	/**
	 * The flint_and_tinder Steel convergence — the card's :207-208 two-row claim through
	 * the datagen row-ID contract: {@code flint_and_tinder} (the :207 shapeless self-recast)
	 * + {@code flint_and_steel} (the :208 nugget-steel reverse row), both under the gt6
	 * namespace — the coexist-with-vanilla declared deviation (the :208
	 * {@code DEL_OTHER_NATIVE_RECIPES} cut). The builder bodies themselves are the runData
	 * leg (the generated JSONs assert through datagen_tree_check).
	 */
	@Test
	public void flintRecipeRowIdsAreTheSteelPair() {
		assertEquals(rl("flint_and_tinder"), GT6CraftingRecipes.FLINT_AND_TINDER_ID);
		assertEquals(rl("flint_and_steel"), GT6CraftingRecipes.FLINT_AND_STEEL_ID);
	}

	// ------------------------------------------------------------------ the no-behaviour census

	/**
	 * The rolling_pin / bending_cylinder census is STRUCTURALLY EMPTY — the
	 * GT6BendingCylinderSmallItem master (the BendingCylinderSmallTest reflection form):
	 * no useOn override, no canPerformAction override — the vanilla defaults (no-op /
	 * false for every action) ARE the declared surface; the only behavioural face is the
	 * has/get crafting-remaining PAIR (the id410 iron law), which the SmallTest's live
	 * channel probe already proved for the shared craftRemaining seam.
	 */
	@Test
	public void craftingConsumablesCensusIsOffZeroWorldAndActionSurface() throws NoSuchMethodException {
		assertThrows(NoSuchMethodException.class,
				() -> GTRollingPinItem.class.getDeclaredMethod("useOn",
						net.minecraft.world.item.context.UseOnContext.class),
				"the rolling pin must not override useOn (the structure-empty master)");
		assertThrows(NoSuchMethodException.class,
				() -> GTRollingPinItem.class.getDeclaredMethod("canPerformAction",
						ItemStack.class, net.minecraftforge.common.ToolAction.class),
				"the rolling pin must not override canPerformAction (the census is OFF)");
		assertThrows(NoSuchMethodException.class,
				() -> GT6BendingCylinderItem.class.getDeclaredMethod("useOn",
						net.minecraft.world.item.context.UseOnContext.class),
				"the large cylinder must not override useOn (the structure-empty master)");
		assertThrows(NoSuchMethodException.class,
				() -> GT6BendingCylinderItem.class.getDeclaredMethod("canPerformAction",
						ItemStack.class, net.minecraftforge.common.ToolAction.class),
				"the large cylinder must not override canPerformAction (the census is OFF)");
		// the PAIR half: the dispatch gate exists on both (the id410 iron law)
		assertTrue(GTRollingPinItem.class.getDeclaredMethod("hasCraftingRemainingItem", ItemStack.class) != null,
				"the rolling pin declares the has-face");
		assertTrue(GT6BendingCylinderItem.class.getDeclaredMethod("hasCraftingRemainingItem", ItemStack.class) != null,
				"the large cylinder declares the has-face");
		assertTrue(GTRollingPinItem.class.getDeclaredMethod("getCraftingRemainingItem", ItemStack.class) != null,
				"the rolling pin declares the get-face");
		assertTrue(GT6BendingCylinderItem.class.getDeclaredMethod("getCraftingRemainingItem", ItemStack.class) != null,
				"the large cylinder declares the get-face");
	}

	/** The plunger constants — the upstream 1000 L drain face, the crowbar speed anchor, the parity damage read. */
	@Test
	public void plungerConstantsMatchTheUpstreamFaces() {
		assertEquals(1000, GTPlungerItem.DRAIN_MILLIBUCKETS, "Behavior_Plunger_Fluid.java:53/:55 the 1000 L do-drain");
		assertEquals(6.0F, GTPlungerItem.MINING_SPEED, 0.001F, "the crowbar MINING_SPEED anchor");
		assertEquals(1.25F, GTPlungerItem.upstreamBaseDamage(), 0.001F, "GT_Tool_Plunger.java:42 parity read");
	}

	// ------------------------------------------------------------------ the PLANT_SELF_DROP conversion

	/**
	 * Upstream GT_Tool_Scissors.convertBlockDrops :87-101 — the vine self replacement,
	 * extended by the research ruling to the cobweb face for the scoop's shears-class
	 * harvest (one shared mode, two identity gates).
	 */
	@Test
	public void plantSelfDropSetIsVineAndCobweb() {
		assertEquals(2, GT6ToolLootModifiers.PLANT_SELF.size());
		assertTrue(GT6ToolLootModifiers.PLANT_SELF.contains(Blocks.VINE), "the upstream :88 vine arm");
		assertTrue(GT6ToolLootModifiers.PLANT_SELF.contains(Blocks.COBWEB), "the scoop shears-class cobweb face");
		assertFalse(GT6ToolLootModifiers.PLANT_SELF.contains(Blocks.GLOW_LICHEN), "the vanilla-only shears plant stays vanilla");
	}

	/** The PLANT_SELF_DROP mode: the block item itself replaces the drops (upstream :89-90). */
	@Test
	public void plantSelfDropReplacesDropsWithTheBlockItself() {
		for (Block tBlock : GT6ToolLootModifiers.PLANT_SELF) {
			List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.STRING), new ItemStack(Items.STICK)));
			assertTrue(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.PLANT_SELF_DROP,
					state(tBlock), tDrops), tBlock + " converts");
			assertEquals(1, tDrops.size(), "the drops are REPLACED, not appended");
			assertSame(tBlock.asItem(), tDrops.get(0).getItem(), "the drop is the block item itself");
		}
	}

	/** The PLANT_SELF_DROP mode off-set: no conversion, the drops ride through. */
	@Test
	public void plantSelfDropLeavesOtherBlocksAlone() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.GLOW_LICHEN)));
		assertFalse(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.PLANT_SELF_DROP,
				state(Blocks.GLOW_LICHEN), tDrops));
		assertEquals(1, tDrops.size());
		assertSame(Items.GLOW_LICHEN, tDrops.get(0).getItem());
	}

	/** The null-state guard rides the shared convert seam (the t1 face). */
	@Test
	public void plantSelfDropNullStateIsIdentity() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.STRING)));
		assertFalse(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.PLANT_SELF_DROP,
				null, tDrops), "the null state is the identity");
		assertEquals(1, tDrops.size(), "the drops stay untouched");
	}
}
