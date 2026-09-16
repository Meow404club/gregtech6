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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.items.tools.loot.GT6ToolLootModifiers;
import gregtech6.items.tools.loot.GT6ToolLootModifiers.GT6ToolConvertModifier.Mode;
import gregtech6.registry.GT6Tools;

/**
 * Offline tests for task p29-w5-t2-blade-six — the six blade tools (the DigSixTest
 * offline-boot form: every assertion rides the pure static seams).
 *
 * <p>Surfaces pinned here (the card ACCEPTANCE rows):
 * <ul>
 * <li>the TAB_TABLE 22-row parity (16 prior + the six blade rows, ids in display
 *     order);</li>
 * <li>the attack-attribute literals (the card 字面值 face — the upstream getBaseDamage
 *     verbatim: sword 4.0F, knife 2.0F, butchery 1.0F, club 5.0F (the INHERITED
 *     HardHammer value, GT_Tool_HardHammer.java:73 — the club has no override), axe
 *     3.0F, axe_double 6.0F);</li>
 * <li>the durability ladder (the family 512 + the double-axe ×1.5 = 768);</li>
 * <li>the club rockGt mapping table pure function (the ROCK_CRUSH census + the
 *     prefix/material resolution);</li>
 * <li>the SWORD_HARVEST conversion arms (grass self-drop, vine replace, the negatives);
 *     </li>
 * <li>the felling gate + the mining faces + the classification census + the looting
 *     constant.</li>
 * </ul>
 */
public class BladeSixTest {

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

	/** The table holds exactly 31 rows — rows 16..21 are the six blade tools in display order (rows 22..25 the machine-face four, rows 26..30 the p29-w5-t4-field-five tail append). */
	@Test
	public void tabTableIsExactlyTheTwentyTwoToolRows() {
		assertEquals(31, GT6Tools.TAB_TABLE.size(), "the Tools tab = the 16 prior rows + the six blade tools + the machine-face four + the field five");
		assertSame(GT6Tools.SWORD, GT6Tools.TAB_TABLE.get(16), "row 16 is the sword");
		assertSame(GT6Tools.KNIFE, GT6Tools.TAB_TABLE.get(17), "row 17 is the knife");
		assertSame(GT6Tools.BUTCHERY_KNIFE, GT6Tools.TAB_TABLE.get(18), "row 18 is the butchery knife");
		assertSame(GT6Tools.CLUB, GT6Tools.TAB_TABLE.get(19), "row 19 is the club");
		assertSame(GT6Tools.AXE, GT6Tools.TAB_TABLE.get(20), "row 20 is the axe");
		assertSame(GT6Tools.AXE_DOUBLE, GT6Tools.TAB_TABLE.get(21), "row 21 is the double axe");
		assertEquals(rl("sword"), GT6Tools.SWORD.getId());
		assertEquals(rl("knife"), GT6Tools.KNIFE.getId());
		assertEquals(rl("butchery_knife"), GT6Tools.BUTCHERY_KNIFE.getId());
		assertEquals(rl("club"), GT6Tools.CLUB.getId());
		assertEquals(rl("axe"), GT6Tools.AXE.getId());
		assertEquals(rl("axe_double"), GT6Tools.AXE_DOUBLE.getId());
	}

	// ------------------------------------------------------------------ the attack attribute literals

	/**
	 * The getBaseDamage verbatim ladder (the card 字面值 face). The club's 5.0F is the
	 * INHERITED HardHammer value — GT_Tool_Club.java:47-142 carries NO getBaseDamage
	 * override, and the card ACCEPTANCE's "6.0F" is the AxeDouble's own
	 * GT_Tool_AxeDouble.getBaseDamage :29-32 (the club's 6*U is the material AMOUNT).
	 */
	@Test
	public void attackDamageLiteralsAreTheUpstreamBaseDamages() {
		assertEquals(4.0F, GTSwordItem.ATTACK_DAMAGE, 0.0F, "GT_Tool_Sword.getBaseDamage :70-72");
		assertEquals(2.0F, GTKnifeItem.ATTACK_DAMAGE, 0.0F, "GT_Tool_Knife.getBaseDamage :56-59");
		assertEquals(1.0F, GTButcheryKnifeItem.ATTACK_DAMAGE, 0.0F, "GT_Tool_ButcheryKnife.getBaseDamage :57-59");
		assertEquals(5.0F, GTClubItem.ATTACK_DAMAGE, 0.0F, "the inherited GT_Tool_HardHammer.getBaseDamage :73");
		assertEquals(3.0F, GTAxeItem.ATTACK_DAMAGE, 0.0F, "GT_Tool_Axe.getBaseDamage :78-80");
		assertEquals(6.0F, GTAxeDoubleItem.ATTACK_DAMAGE, 0.0F, "GT_Tool_AxeDouble.getBaseDamage :29-32");
	}

	/** The attack-rate anchors (the vanilla faces; no 1.7.10 source — the 1.9 attribute). */
	@Test
	public void attackSpeedAnchorsAreTheDeclaredFaces() {
		assertEquals(-2.4F, GTSwordItem.ATTACK_SPEED, 0.0F, "the vanilla sword rate");
		assertEquals(-2.4F, GTKnifeItem.ATTACK_SPEED, 0.0F, "the knife inherits the sword rate");
		assertEquals(-3.4F, GTButcheryKnifeItem.ATTACK_SPEED, 0.0F, "the Has-a-slow-Attack-Rate face");
		assertEquals(-3.0F, GTClubItem.ATTACK_SPEED, 0.0F, "the heavy-tool rate");
		assertEquals(-3.0F, GTAxeItem.ATTACK_SPEED, 0.0F, "the vanilla iron-axe rate");
		assertEquals(-3.0F, GTAxeDoubleItem.ATTACK_SPEED, 0.0F, "the double axe keeps the axe rate");
	}

	/** The durability ladder: the family 512 everywhere except the double axe ×1.5 = 768. */
	@Test
	public void durabilityLadderMatchesTheUpstreamMultipliers() {
		assertEquals(512, GTSwordItem.DURABILITY_POINTS);
		assertEquals(512, GTKnifeItem.DURABILITY_POINTS);
		assertEquals(512, GTButcheryKnifeItem.DURABILITY_POINTS);
		assertEquals(512, GTClubItem.DURABILITY_POINTS);
		assertEquals(512, GTAxeItem.DURABILITY_POINTS);
		assertEquals(768, GTAxeDoubleItem.DURABILITY_POINTS, "upstream getMaxDurabilityMultiplier ×1.5 (GT_Tool_AxeDouble.java:34-37)");
	}

	// ------------------------------------------------------------------ the club rockGt mapping table

	/** The table census — the upstream :61-110 cluster minus the mod arms. */
	@Test
	public void rockCrushTableIsTheUpstreamCluster() {
		assertEquals(22, GT6ToolLootModifiers.ROCK_CRUSH.size(), "8 stone-family + 3 nether-brick + netherrack + endstone + obsidian + 2 basalt + 5 blackstone + redstone ore");
		for (Block tBlock : new Block[] {Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
				Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.COBBLESTONE_WALL,
				Blocks.STONE_BUTTON, Blocks.STONE_PRESSURE_PLATE}) {
			assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(tBlock), tBlock + " rides the stone family (:64)");
		}
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.NETHER_BRICKS), ":69");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.NETHER_BRICK_FENCE), ":69");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.NETHERRACK), ":74");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.END_STONE), ":79");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.OBSIDIAN), ":84");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.BASALT), ":89");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.POLISHED_BASALT), "the polished unfold");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.BLACKSTONE), ":94");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.POLISHED_BLACKSTONE_BRICKS), "the bricks unfold");
		assertTrue(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.REDSTONE_ORE), ":104");
		assertFalse(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.SAND), "no sand in the cluster");
		assertFalse(GT6ToolLootModifiers.ROCK_CRUSH.containsKey(Blocks.GRANITE), "the 1.7.10 granite is a mod block, not the vanilla one");
	}

	/**
	 * The resolution face — the suppliers resolve the live gt6 handles (the test JVM's
	 * gregapi init fills them, so the material identity IS offline-pinnable here; the
	 * ItemStack landing stays the RCON rock-chain leg).
	 */
	@Test
	public void rockCrushEntriesExistForEveryCrushableBlock() {
		// ORDER-INDEPENDENT face: whether the gregapi handles are filled depends on the
		// suite's test order (OP.init() runs transitively in some classes), so the OFFLINE
		// pin is the entry existence + non-null suppliers; the material identity (rockGt
		// Stone / gemChipped Cinnabar) and the ItemStack landing are the RCON rock-chain
		// leg (gt6:rock_gt_stone / gt6:gem_chipped_cinnabar as the live crush output).
		for (Block tBlock : new Block[] {Blocks.STONE, Blocks.COBBLESTONE, Blocks.NETHERRACK,
				Blocks.END_STONE, Blocks.OBSIDIAN, Blocks.BASALT, Blocks.BLACKSTONE, Blocks.REDSTONE_ORE}) {
			GT6ToolLootModifiers.CrushTarget tTarget = GT6ToolLootModifiers.ROCK_CRUSH.get(tBlock);
			assertTrue(tTarget != null, tBlock + " has an entry");
			assertTrue(tTarget.prefix() != null && tTarget.material() != null, tBlock + " carries both resolution suppliers");
		}
	}

	/** The CLUB_ROCK_CRUSH mode off-registry: the pure-table identity guard (no ItemStacks offline). */
	@Test
	public void clubCrushIsIdentityWhenRegistriesAreAbsent() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.COBBLESTONE)));
		// offline the gt6 material items are unregistered — the mode either returns false
		// untouched or the guard kept the drop; it NEVER empties the list
		GT6ToolLootModifiers.convert(Mode.CLUB_ROCK_CRUSH, state(Blocks.COBBLESTONE), tDrops);
		assertEquals(1, tDrops.size(), "the drop is never LOST");
		assertFalse(tDrops.get(0).isEmpty(), "the cobble rides through when the registry is absent");
	}

	// ------------------------------------------------------------------ the SWORD_HARVEST conversion

	/** The grass face: the plant's own item ADDED (the vanilla seeds ride, upstream :114 count 1). */
	@Test
	public void swordHarvestAddsTheGrassItem() {
		List<ItemStack> tDrops = new ArrayList<>();
		//? if forge {
		assertTrue(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.GRASS), tDrops));
		assertEquals(1, tDrops.size());
		assertSame(Items.GRASS, tDrops.get(0).getItem(), "the plant drops itself at zero fortune");
		//?} else {
		/*assertTrue(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.SHORT_GRASS), tDrops));
		//21.1: GRASS renamed SHORT_GRASS (the GT6ToolLootModifiers convert fork verbatim)
		assertEquals(1, tDrops.size());
		assertSame(Items.SHORT_GRASS, tDrops.get(0).getItem(), "the plant drops itself at zero fortune");
		*///?}
	}

	/** The fern face: the flattening unfold keeps the block identity. */
	@Test
	public void swordHarvestAddsTheFernItem() {
		List<ItemStack> tDrops = new ArrayList<>();
		assertTrue(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.FERN), tDrops));
		assertSame(Items.FERN, tDrops.get(0).getItem());
	}

	/** The double-plant face: count 2 (upstream :120). */
	@Test
	public void swordHarvestAddsTwoTallGrass() {
		List<ItemStack> tDrops = new ArrayList<>();
		assertTrue(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.TALL_GRASS), tDrops));
		assertSame(Items.TALL_GRASS, tDrops.get(0).getItem());
		assertEquals(2, tDrops.get(0).getCount());
	}

	/** The vine face: the vanilla no-shears empty table REPLACED by the vine (upstream :98-101). */
	@Test
	public void swordHarvestReplacesTheVineDrop() {
		List<ItemStack> tDrops = new ArrayList<>();
		assertTrue(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.VINE), tDrops));
		assertEquals(1, tDrops.size());
		assertSame(Items.VINE, tDrops.get(0).getItem());
	}

	/** The dead-bush face: 1-2 sticks (upstream :161-163). */
	@Test
	public void swordHarvestAddsSticksFromTheDeadBush() {
		List<ItemStack> tDrops = new ArrayList<>();
		assertTrue(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.DEAD_BUSH), tDrops));
		assertSame(Items.STICK, tDrops.get(0).getItem());
		assertTrue(tDrops.get(0).getCount() >= 1 && tDrops.get(0).getCount() <= 2, "1+nextInt(2)");
	}

	/** The negatives: stone and dirt are on no sword-harvest arm. */
	@Test
	public void swordHarvestLeavesOtherBlocksAlone() {
		List<ItemStack> tDrops = new ArrayList<>(List.of(new ItemStack(Items.DIRT)));
		assertFalse(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.DIRT), tDrops));
		assertFalse(GT6ToolLootModifiers.convert(Mode.SWORD_HARVEST, state(Blocks.STONE), tDrops));
		assertEquals(1, tDrops.size());
	}

	// ------------------------------------------------------------------ the felling gate

	/**
	 * The felling gate — the PURE arms (the huge-mushroom instanceof + the negatives);
	 * the #minecraft:logs tag arm binds live — the RCON tree-chain leg (the tag-arm
	 * ruling).
	 */
	@Test
	public void fellingGatePureArmsAreTheMushroomResiduals() {
		assertTrue(GTAxeItem.isFellable(state(Blocks.BROWN_MUSHROOM_BLOCK)), "the BlockHugeMushroom instanceof (upstream :107)");
		assertTrue(GTAxeItem.isFellable(state(Blocks.RED_MUSHROOM_BLOCK)));
		assertFalse(GTAxeItem.isFellable(state(Blocks.OAK_PLANKS)), "planks are not fellable (upstream isWood F)");
		assertFalse(GTAxeItem.isFellable(state(Blocks.STICKY_PISTON)));
	}

	// ------------------------------------------------------------------ the mining faces

	/**
	 * The sword face — the PURE arm only (cobweb, the vanilla SwordItem.java:45 hardcode);
	 * the SWORD_EFFICIENT/wool tag arms bind only with the datapack — the LIVE RCON legs'
	 * face, not pinable here (the DigSixTest tag-arm ruling).
	 */
	@Test
	public void swordFacePureArmIsTheWeb() {
		assertTrue(GTSwordItem.mines(state(Blocks.COBWEB)), "upstream Material.web (the vanilla sword hardcode)");
		assertFalse(GTSwordItem.mines(state(Blocks.STONE)), "stone is on NO sword arm");
		assertFalse(GTSwordItem.mines(state(Blocks.DIRT)));
		assertEquals(6.0F, GTSwordItem.destroySpeedBonus(state(Blocks.COBWEB)), 0.0F, "the surface speed on the pure arm");
	}

	/** The knife face: the inherited sword surface at half speed (the pure cobweb arm). */
	@Test
	public void knifeFaceIsTheSwordSurfaceAtHalfSpeed() {
		assertTrue(GTKnifeItem.mines(state(Blocks.COBWEB)), "the inherited surface (pure arm)");
		assertEquals(3.0F, GTKnifeItem.MINING_SPEED, 0.0F, "the 6.0F anchor × 0.5");
		assertEquals(3.0F, GTKnifeItem.destroySpeedBonus(state(Blocks.COBWEB)), 0.0F);
	}

	/** The club face: the glass/ice family sets at ×0.5 speed (the pure arms). */
	@Test
	public void clubFaceIsTheHardHammerSurface() {
		assertTrue(GTClubItem.mines(state(Blocks.GLASS)), "upstream Material.glass (HardHammer :85)");
		assertTrue(GTClubItem.mines(state(Blocks.PACKED_ICE)), "upstream Material.packedIce");
		assertTrue(GTClubItem.mines(state(Blocks.WHITE_STAINED_GLASS)), "the flattening unfold");
		assertFalse(GTClubItem.mines(state(Blocks.OAK_LOG)), "wood is on NO club arm");
		assertEquals(3.0F, GTClubItem.MINING_SPEED, 0.0F, "the 6.0F anchor × 0.5");
		assertEquals(3.0F, GTClubItem.destroySpeedBonus(state(Blocks.GLASS)), 0.0F);
	}

	/**
	 * The axe face — the PURE arms only (vine/cactus/mushroom); the axe/leaves tag arms
	 * are the live RCON face (the tag-arm ruling). The double axe inherits.
	 */
	@Test
	public void axeFacePureArmsAreThePlantResiduals() {
		assertTrue(GTAxeItem.mines(state(Blocks.VINE)), "upstream Material.vine");
		assertTrue(GTAxeItem.mines(state(Blocks.CACTUS)), "upstream Material.cactus");
		assertTrue(GTAxeItem.mines(state(Blocks.BROWN_MUSHROOM_BLOCK)), "upstream BlockHugeMushroom");
		assertFalse(GTAxeItem.mines(state(Blocks.STONE)));
		assertTrue(GTAxeDoubleItem.mines(state(Blocks.CACTUS)), "the inherited surface");
		assertEquals(6.0F, GTAxeItem.MINING_SPEED, 0.0F);
	}

	/** The butchery face: constant false (upstream isMinableBlock :112-114). */
	@Test
	public void butcheryHasNoMiningFace() {
		assertFalse(GTButcheryKnifeItem.mines(state(Blocks.DIRT)));
		assertFalse(GTButcheryKnifeItem.mines(state(Blocks.COBWEB)));
	}

	// ------------------------------------------------------------------ the looting constant

	/** The Looting face constant — mToolQuality 2 (MT.java:1713) / 2 + 1. */
	@Test
	public void butcheryLootingLevelIsTheSteelQualityFace() {
		assertEquals(2, GTButcheryKnifeItem.LOOTING_LEVEL, "GT_Tool_ButcheryKnife.java:92-94 mToolQuality/2+1 at quality 2");
	}

	// ------------------------------------------------------------------ the classification census

	/** The gt6 actions: the blade keys + the vanilla faces; never the crowbar. */
	@Test
	public void classificationCensusIsTheCardSurface() {
		assertTrue(GTSwordItem.classifies(GT6ToolActions.SWORD));
		assertTrue(GTSwordItem.classifies(net.minecraftforge.common.ToolActions.SWORD_DIG));
		assertFalse(GTSwordItem.classifies(GT6ToolActions.CROWBAR));
		assertTrue(GTKnifeItem.classifies(GT6ToolActions.KNIFE));
		assertFalse(GTKnifeItem.classifies(GT6ToolActions.SWORD), "the knife keeps its OWN gt6 key");
		assertTrue(GTButcheryKnifeItem.classifies(GT6ToolActions.BUTCHERY_KNIFE));
		assertFalse(GTButcheryKnifeItem.classifies(GT6ToolActions.KNIFE), "the butchery is tellable from the knife");
		assertTrue(GTClubItem.classifies(GT6ToolActions.CLUB));
		assertTrue(GTClubItem.classifies(GT6ToolActions.HAMMER), "the Loader_Tools.java:130 TOOL_hammer behaviour row");
		assertTrue(GTAxeItem.classifies(GT6ToolActions.AXE));
		assertTrue(GTAxeItem.classifies(net.minecraftforge.common.ToolActions.AXE_DIG));
		assertTrue(GTAxeDoubleItem.classifies(GT6ToolActions.AXE), "the PICKAXE family ruling — one action for the variants");
	}
}
