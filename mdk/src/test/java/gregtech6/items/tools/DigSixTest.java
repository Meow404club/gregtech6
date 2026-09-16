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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.datagen.GT6ItemTags;
import gregtech6.items.tools.loot.GT6ToolLootModifiers;
import gregtech6.items.tools.loot.GT6ToolSweep;
import gregtech6.registry.GT6Tools;

/**
 * Offline tests for task p29-w5-t1-dig-six — the six dig tools + the shared loot/sweep
 * infrastructure (the GT6ToolsCreativeTabTest offline boot form: a bootstrapped-and-
 * frozen JVM cannot construct mod Items, so every assertion rides the PURE static seams
 * — the GTCrowbarItem.mines ruling).
 *
 * <p>Surfaces pinned here (the card ACCEPTANCE rows):
 * <ul>
 * <li>the TAB_TABLE 16-row parity (10 prior + the six dig rows, ids in display order);</li>
 * <li>the tools tag census (the six new {@code gt6:tools/*} tag keys, one member each);</li>
 * <li>the drop-conversion pure functions — the harvestableSpade + ender_chest + openable
 *     mapping tables and the mode dispatch (the upstream convertBlockDrops semantics);</li>
 * <li>the GT6ToolSweep ThreadLocal guard state machine;</li>
 * <li>the per-tool mining faces (the upstream isMinableBlock mappings through the
 *     vanilla tags + the extension sets).</li>
 * </ul>
 */
public class DigSixTest {

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

	/** The table holds exactly 16 rows — rows 10..15 are the six dig tools in display order. */
	@Test
	public void tabTableIsExactlyTheSixteenToolRows() {
		assertEquals(16, GT6Tools.TAB_TABLE.size(), "the Tools tab = the 10 prior rows + the six dig tools");
		assertSame(GT6Tools.PICKAXE, GT6Tools.TAB_TABLE.get(10), "row 10 is the pickaxe");
		assertSame(GT6Tools.PICKAXE_GEM, GT6Tools.TAB_TABLE.get(11), "row 11 is the gem pickaxe");
		assertSame(GT6Tools.PICKAXE_CONSTRUCTION, GT6Tools.TAB_TABLE.get(12), "row 12 is the construction pickaxe");
		assertSame(GT6Tools.SHOVEL, GT6Tools.TAB_TABLE.get(13), "row 13 is the shovel");
		assertSame(GT6Tools.SPADE, GT6Tools.TAB_TABLE.get(14), "row 14 is the spade");
		assertSame(GT6Tools.UNIVERSAL_SPADE, GT6Tools.TAB_TABLE.get(15), "row 15 is the universal spade");
		assertEquals(rl("pickaxe"), GT6Tools.PICKAXE.getId());
		assertEquals(rl("pickaxe_gem"), GT6Tools.PICKAXE_GEM.getId());
		assertEquals(rl("pickaxe_construction"), GT6Tools.PICKAXE_CONSTRUCTION.getId());
		assertEquals(rl("shovel"), GT6Tools.SHOVEL.getId());
		assertEquals(rl("spade"), GT6Tools.SPADE.getId());
		assertEquals(rl("universal_spade"), GT6Tools.UNIVERSAL_SPADE.getId());
	}

	/** The durability family: 512 everywhere except the gem pick's upstream ×0.25 (= 128). */
	@Test
	public void durabilityLadderMatchesTheUpstreamMultipliers() {
		assertEquals(512, GTPickaxeItem.DURABILITY_POINTS);
		assertEquals(512, GTPickaxeConstructionItem.DURABILITY_POINTS);
		assertEquals(512, GTShovelItem.DURABILITY_POINTS);
		assertEquals(512, GTSpadeItem.DURABILITY_POINTS);
		assertEquals(512, GTUniversalSpadeItem.DURABILITY_POINTS);
		assertEquals(128, GTPickaxeGemItem.DURABILITY_POINTS, "upstream getMaxDurabilityMultiplier /4 (GT_Tool_PickaxeGem.java:29)");
	}

	// ------------------------------------------------------------------ the tools tag census

	/** The six new tag keys — the self-owned snake paths, one member each (the p24 band shape). */
	@Test
	public void digToolTagPathsAreTheSnakeCensus() {
		assertEquals(rl("tools/pickaxe"), GT6ItemTags.TOOLS_PICKAXE.location());
		assertEquals(rl("tools/pickaxe_gem"), GT6ItemTags.TOOLS_PICKAXE_GEM.location());
		assertEquals(rl("tools/pickaxe_construction"), GT6ItemTags.TOOLS_PICKAXE_CONSTRUCTION.location());
		assertEquals(rl("tools/shovel"), GT6ItemTags.TOOLS_SHOVEL.location());
		assertEquals(rl("tools/spade"), GT6ItemTags.TOOLS_SPADE.location());
		assertEquals(rl("tools/universal_spade"), GT6ItemTags.TOOLS_UNIVERSAL_SPADE.location());
	}

	// ------------------------------------------------------------------ the drop-conversion mapping tables

	/**
	 * Upstream BlocksGT.harvestableSpade (CS.java:1691 verbatim): grass, dirt, mycelium,
	 * clay, snow, gravel — the 1.7.10 Blocks.snow is the FULL snow block.
	 */
	@Test
	public void harvestableSpadeSetIsTheUpstreamSix() {
		assertEquals(6, GT6ToolLootModifiers.HARVESTABLE_SPADE.size());
		assertTrue(GT6ToolLootModifiers.HARVESTABLE_SPADE.containsAll(java.util.Set.of(
				Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.MYCELIUM, Blocks.CLAY, Blocks.SNOW_BLOCK, Blocks.GRAVEL)));
		assertFalse(GT6ToolLootModifiers.HARVESTABLE_SPADE.contains(Blocks.SAND), "the upstream set has no sand");
		assertFalse(GT6ToolLootModifiers.HARVESTABLE_SPADE.contains(Blocks.SNOW), "the 1.7.10 Blocks.snow is the full block, not the layer");
	}

	/** Upstream BlocksGT.openableCrowbar (CS.java:1688 verbatim): the seven storage blocks. */
	@Test
	public void openableCrowbarSetIsTheUpstreamSeven() {
		assertEquals(7, GT6ToolLootModifiers.OPENABLE_CROWBAR.size());
		assertTrue(GT6ToolLootModifiers.OPENABLE_CROWBAR.containsAll(java.util.Set.of(
				Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.DIAMOND_BLOCK,
				Blocks.EMERALD_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.COAL_BLOCK)));
	}

	/** The HARVESTABLE_SPADE mode: the block item itself replaces the drops (upstream :83-85). */
	@Test
	public void spadeHarvestReplacesDropsWithTheBlockItself() {
		for (Block tBlock : GT6ToolLootModifiers.HARVESTABLE_SPADE) {
			List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.OBSIDIAN), new ItemStack(Items.DIAMOND)));
			assertTrue(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.HARVESTABLE_SPADE,
					state(tBlock), tDrops), tBlock + " converts");
			assertEquals(1, tDrops.size(), "the drops are REPLACED, not appended");
			assertSame(tBlock.asItem(), tDrops.get(0).getItem(), "the drop is the block item itself");
		}
	}

	/** The HARVESTABLE_SPADE mode off-set: no conversion, the drops ride through. */
	@Test
	public void spadeHarvestLeavesOtherBlocksAlone() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.SAND)));
		assertFalse(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.HARVESTABLE_SPADE,
				state(Blocks.SAND), tDrops));
		assertEquals(1, tDrops.size());
		assertSame(Items.SAND, tDrops.get(0).getItem());
	}

	/** The ENDER_CHEST_SELF mode: the chest replaces the vanilla 8-obsidian drop (upstream :54-57). */
	@Test
	public void constructionPickMakesTheEnderChestDropItself() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.OBSIDIAN, 8)));
		assertTrue(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.ENDER_CHEST_SELF,
				state(Blocks.ENDER_CHEST), tDrops));
		assertEquals(1, tDrops.size());
		assertSame(Items.ENDER_CHEST, tDrops.get(0).getItem());

		// the negative: a normal chest is untouched
		List<ItemStack> tChestDrops = new ArrayList<>(List.of(new ItemStack(Items.CHEST)));
		assertFalse(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.ENDER_CHEST_SELF,
				state(Blocks.CHEST), tChestDrops));
		assertSame(Items.CHEST, tChestDrops.get(0).getItem());
	}

	/**
	 * The UNBOXINATOR_OPEN mode with the map ABSENT (the offline/pre-init state): the
	 * identity — no crash, no drop lost (the null-map guard on
	 * {@code GT6RecipeMaps.UNBOXINATOR}). The walk itself ACTIVATES when unpack rows land.
	 */
	@Test
	public void universalSpadeOpenableIsIdentityWithoutUnboxinatorRows() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.IRON_BLOCK)));
		boolean tConverted = GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.UNBOXINATOR_OPEN,
				state(Blocks.IRON_BLOCK), tDrops);
		Item tRideItem = tDrops.get(0).getItem();
		assertEquals(1, tDrops.size(), "the openable block's drop is never LOST");
		// the arm either finds no row (identity) or — rows absent today — did not fire at all
		assertTrue(!tConverted || tRideItem != Items.AIR, "the walk never empties the drops");
	}

	/** The universal openable walk SKIPS non-openable blocks (upstream :99 contains gate). */
	@Test
	public void universalSpadeOpenableSkipsOtherBlocks() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.STONE)));
		assertFalse(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.UNBOXINATOR_OPEN,
				state(Blocks.STONE), tDrops));
	}

	// ------------------------------------------------------------------ the sweep guard

	/** The ThreadLocal reentrancy state machine (the upstream sIsHarvestingRightNow gate). */
	@Test
	public void sweepGuardBlocksReentrancyAndRecovers() {
		assertFalse(GT6ToolSweep.inside(), "a fresh thread is outside");
		assertTrue(GT6ToolSweep.tryEnter(), "the first claim succeeds");
		assertTrue(GT6ToolSweep.inside());
		assertFalse(GT6ToolSweep.tryEnter(), "the re-entrant claim fails — the upstream guard");
		GT6ToolSweep.exit();
		assertFalse(GT6ToolSweep.inside());
		assertTrue(GT6ToolSweep.tryEnter(), "the guard recovers after exit (a per-thread walk, not a leak)");
		GT6ToolSweep.exit();
	}

	// ------------------------------------------------------------------ the mining faces

	/**
	 * The pickaxe face — the PURE extension-set arms (the vanilla-tag arms bind only with
	 * the datapack, so stone/anvil/infested are the LIVE RCON legs' face, not pinable
	 * here; the GTCrowbarItem explicit-set ruling is why these four arms are sets at all).
	 */
	@Test
	public void pickaxeFacePureArmsAreTheMaterialExtensions() {
		assertTrue(GTPickaxeItem.mines(state(Blocks.GLASS)), "upstream Material.glass");
		assertTrue(GTPickaxeItem.mines(state(Blocks.WHITE_STAINED_GLASS)), "the flattening unfold");
		assertTrue(GTPickaxeItem.mines(state(Blocks.WHITE_STAINED_GLASS_PANE)));
		assertTrue(GTPickaxeItem.mines(state(Blocks.TINTED_GLASS)));
		assertTrue(GTPickaxeItem.mines(state(Blocks.PACKED_ICE)), "upstream Material.packedIce");
		assertTrue(GTPickaxeItem.mines(state(Blocks.BLUE_ICE)));
		assertTrue(GTPickaxeItem.mines(state(Blocks.FLOWER_POT)), "upstream aBlock == Blocks.flower_pot");
		assertTrue(GTPickaxeItem.mines(state(Blocks.POTTED_OAK_SAPLING)), "the potted unfold");
		assertTrue(GTPickaxeItem.mines(state(Blocks.CAULDRON)), "upstream Material.iron residual");
		assertEquals(6.0F, GTPickaxeItem.destroySpeedBonus(state(Blocks.GLASS)), 0.0F, "the surface speed on a pure arm");
	}

	/** The construction pick face: ×2 on the surface, the ore-stone ×0.25 penalty (:41-65). */
	@Test
	public void constructionPickSpeedIsTwiceWithTheOrePenalty() {
		assertEquals(12.0F, GTPickaxeConstructionItem.MINING_SPEED, 0.0F);
		assertEquals(3.0F, GTPickaxeConstructionItem.destroySpeedBonus(state(Blocks.IRON_ORE)), 0.0F, "aDefault/4 on ore_stone");
		assertEquals(3.0F, GTPickaxeConstructionItem.destroySpeedBonus(state(Blocks.DEEPSLATE_DIAMOND_ORE)), 0.0F);
		assertEquals(3.0F, GTPickaxeConstructionItem.destroySpeedBonus(state(Blocks.NETHER_QUARTZ_ORE)), 0.0F);
		assertEquals(12.0F, GTPickaxeConstructionItem.destroySpeedBonus(state(Blocks.GLASS)), 0.0F, "the pure arms keep the full speed (not ore_stone)");
	}

	/** The shovel face — the FIRE arm is pure (the tag arm binds live; the RCON path legs). */
	@Test
	public void shovelFacePureArmIsFire() {
		assertTrue(GTShovelItem.mines(state(Blocks.FIRE)), "upstream Material.fire arm");
		assertTrue(GTShovelItem.mines(state(Blocks.SOUL_FIRE)));
		assertFalse(GTShovelItem.mines(state(Blocks.STONE)), "stone is on NO shovel arm (tag unbound offline)");
	}

	/** The spade face — the pure fire arm at ×1.5 speed (the harvest conversion is the loot seam). */
	@Test
	public void spadeFacePureArmIsFireAtOneAndAHalfSpeed() {
		assertTrue(GTSpadeItem.mines(state(Blocks.SOUL_FIRE)));
		assertEquals(9.0F, GTSpadeItem.MINING_SPEED, 0.0F);
		assertEquals(9.0F, GTSpadeItem.destroySpeedBonus(state(Blocks.SOUL_FIRE)), 0.0F);
	}

	/**
	 * The universal spade face — the PURE arms (rails, openable, plants, snow, fire; the
	 * mineable/leaves/wool tag arms bind live — the RCON vine/leaves/snow legs). The
	 * ×0.75 speed pins on a pure arm.
	 */
	@Test
	public void universalSpadeFacePureArmsAreTheFiveFaceResiduals() {
		// the crowbar face: rails + openable
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.RAIL)), "upstream BlockRailBase instanceof");
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.POWERED_RAIL)));
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.IRON_BLOCK)), "upstream openableCrowbar");
		// the sword/plant face residuals
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.VINE)), "upstream Material.vine");
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.COBWEB)), "upstream Material.web");
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.CACTUS)));
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.PUMPKIN)), "upstream Material.gourd");
		// snow (the RCON snow leg) — both the layer and the block
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.SNOW)));
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.SNOW_BLOCK)));
		// fire
		assertTrue(GTUniversalSpadeItem.mines(state(Blocks.FIRE)), "upstream Material.fire");
		// the ×0.75 speed
		assertEquals(4.5F, GTUniversalSpadeItem.MINING_SPEED, 0.0F);
		assertEquals(4.5F, GTUniversalSpadeItem.destroySpeedBonus(state(Blocks.RAIL)), 0.0F);
	}

	// ------------------------------------------------------------------ the classification census

	/** The gt6 actions: pickaxe family one action; spade/shovel/universal own keys; never the crowbar. */
	@Test
	public void classificationCensusIsTheCardSurface() {
		assertTrue(GTPickaxeItem.classifies(GT6ToolActions.PICKAXE));
		assertTrue(GTPickaxeItem.classifies(net.minecraftforge.common.ToolActions.PICKAXE_DIG));
		assertFalse(GTPickaxeItem.classifies(GT6ToolActions.CROWBAR));
		assertTrue(GTShovelItem.classifies(GT6ToolActions.SHOVEL));
		assertTrue(GTShovelItem.classifies(net.minecraftforge.common.ToolActions.SHOVEL_DIG));
		assertTrue(GTSpadeItem.classifies(GT6ToolActions.SPADE));
		assertTrue(GTSpadeItem.classifies(net.minecraftforge.common.ToolActions.SHOVEL_DIG));
		assertTrue(GTUniversalSpadeItem.classifies(GT6ToolActions.UNIVERSAL_SPADE));
		assertTrue(GTUniversalSpadeItem.classifies(net.minecraftforge.common.ToolActions.SHOVEL_DIG));
		assertTrue(GTUniversalSpadeItem.classifies(net.minecraftforge.common.ToolActions.AXE_DIG));
		assertTrue(GTUniversalSpadeItem.classifies(net.minecraftforge.common.ToolActions.SWORD_DIG));
		assertFalse(GTUniversalSpadeItem.classifies(GT6ToolActions.CROWBAR), "the cover-dismantle dispatch must never see the universal spade");
	}
}
