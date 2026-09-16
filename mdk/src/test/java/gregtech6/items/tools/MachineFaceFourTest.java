/**
 * Offline tests for task p29-w5-t3-machine-face-four: the machine-face four (the soft
 * hammer, the monkey wrench, the magnifying glass, the pincers) — the stat pins, the
 * action/dispatch-id pins, the tag faces, the soft-hammer rotation cut list (the card's
 * 1.20.1 vanilla 对位 ruling, the literals in test) and the RED LINE walls (the monkey
 * wrench stays off the machine side; the three HOE_DIG-keyed wrench-substitute predicates
 * never see any of the four).
 *
 * <p>Assertion surface notes (the HammerWrenchTest posture verbatim): the mod items are
 * NOT constructible in this bootstrapped-and-frozen JVM (the Item.java:61 intrusive-holder
 * wall), so the faces pin through the STATIC seams ({@link GTSoftHammerItem#rotatedForm},
 * {@link GTMagnifyingGlassItem#inspectSound}, the classifies trio) over VANILLA
 * blockstates/random sources, and the has/get PAIRING iron law pins via reflection (the
 * dead-gate regression shape — an item whose getCraftingRemainingItem is overridden
 * WITHOUT the has gate swallows tools in the vanilla channel; each of the four must carry
 * its own has override). The live useOn faces (rotation, the egg collect, the AHA/HMM
 * zero-change face) ride the {@code /gt6machineface} RCON stand-in arms.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GT6Tools;

public class MachineFaceFourTest {

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

	// ------------------------------------------------- the stat + literal pins

	/** The single steel tier — 512 durability points on all four (the family value). */
	@Test
	public void durabilityIsTheFamilyValue() {
		assertEquals(512, GTSoftHammerItem.DURABILITY_POINTS);
		assertEquals(512, GTMonkeyWrenchItem.DURABILITY_POINTS);
		assertEquals(512, GTMagnifyingGlassItem.DURABILITY_POINTS);
		assertEquals(512, GTPincersItem.DURABILITY_POINTS);
	}

	/**
	 * The soft-hammer stat literals (the card's test-literal ruling): the 0.1F dig speed
	 * multiplier (GT_Tool_SoftHammer.java:74-76) and the 8.0F max-durability multiplier
	 * (:79-81) — the latter a DECLARATION only, the shell stays 512 (the pool cut).
	 */
	@Test
	public void softHammerStatLiteralsAreTheUpstreamValues() {
		assertEquals(0.1F, GTSoftHammerItem.SPEED_MULTIPLIER);
		assertEquals(8.0F, GTSoftHammerItem.MAX_DURABILITY_MULTIPLIER);
	}

	/** The pay-per-action literals — one point per rotation / collect (the 100-unit click fold). */
	@Test
	public void payPerActionIsOnePoint() {
		assertEquals(1, GTSoftHammerItem.DAMAGE_PER_ROTATION);
		assertEquals(1, GTPincersItem.DAMAGE_PER_COLLECT);
	}

	// ------------------------------------------------- the action + dispatch-id pins

	/** The action names — "gt6_softhammer"/"gt6_monkeywrench"/"gt6_magnifyingglass"/"gt6_pincers" (the gt6_hammer shape). */
	@Test
	public void actionNamesAreThePinnedLiterals() {
		assertEquals("gt6_softhammer", GTSoftHammerItem.ACTION.name());
		assertEquals("gt6_monkeywrench", GTMonkeyWrenchItem.ACTION.name());
		assertEquals("gt6_magnifyingglass", GTMagnifyingGlassItem.ACTION.name());
		assertEquals("gt6_pincers", GTPincersItem.ACTION.name());
	}

	/** The dispatch ids — the upstream CS.TOOL_* strings verbatim (CS.java:1063/:1039/:1070/:1041). */
	@Test
	public void dispatchIdsAreTheUpstreamStrings() {
		assertEquals("softhammer", GTSoftHammerItem.ID);
		assertEquals("monkeywrench", GTMonkeyWrenchItem.ID);
		assertEquals("magnifyingglass", GTMagnifyingGlassItem.ID);
		assertEquals("pincers", GTPincersItem.ID);
	}

	/** Each item classifies on exactly its own action; NEVER on HOE_DIG (the wrench-substitute red line). */
	@Test
	public void classificationIsOneActionEachAndNeverHoeDig() {
		assertTrue(GTSoftHammerItem.classifies(GTSoftHammerItem.ACTION));
		assertTrue(GTMonkeyWrenchItem.classifies(GTMonkeyWrenchItem.ACTION));
		assertTrue(GTMagnifyingGlassItem.classifies(GTMagnifyingGlassItem.ACTION));
		assertTrue(GTPincersItem.classifies(GTPincersItem.ACTION));
		assertFalse(GTSoftHammerItem.classifies(ToolActions.HOE_DIG));
		assertFalse(GTMonkeyWrenchItem.classifies(ToolActions.HOE_DIG));
		assertFalse(GTMagnifyingGlassItem.classifies(ToolActions.HOE_DIG));
		assertFalse(GTPincersItem.classifies(ToolActions.HOE_DIG));
		// the monkey wrench is its OWN action — never an alias of the plain wrench
		// (the machine-side wrench consumers stay WRENCH-keyed untouched, the RED LINE)
		assertFalse(GTMonkeyWrenchItem.classifies(GT6ToolActions.WRENCH));
		assertFalse(GTSoftHammerItem.classifies(GTMonkeyWrenchItem.ACTION));
	}

	/**
	 * The has/get PAIRING iron law — the resolved has gate on each of the four must come
	 * from GT6 code, NEVER the vanilla default (the id410 dead-gate wall: the vanilla
	 * {@code hasCraftingRemainingItem} constant-false would swallow every craft). The
	 * monkey wrench inherits the gate from {@link GTWrenchItem} — the subclass form.
	 */
	@Test
	public void theCraftingLossGateIsOverriddenOnAllFour() throws NoSuchMethodException {
		assertNotEquals(Item.class, GTSoftHammerItem.class.getMethod("hasCraftingRemainingItem", ItemStack.class).getDeclaringClass());
		assertNotEquals(Item.class, GTMonkeyWrenchItem.class.getMethod("hasCraftingRemainingItem", ItemStack.class).getDeclaringClass());
		assertNotEquals(Item.class, GTMagnifyingGlassItem.class.getMethod("hasCraftingRemainingItem", ItemStack.class).getDeclaringClass());
		assertNotEquals(Item.class, GTPincersItem.class.getMethod("hasCraftingRemainingItem", ItemStack.class).getDeclaringClass());
		assertEquals(GTWrenchItem.class, GTMonkeyWrenchItem.class.getMethod("hasCraftingRemainingItem", ItemStack.class).getDeclaringClass(),
				"the monkey wrench inherits the wrench gate verbatim (the subclass form)");
	}

	/**
	 * The RED LINE wall — the monkey wrench must NOT declare a {@code useOn} override (the
	 * machine-dismantle face is the machine-interaction pool). The soft hammer, the
	 * magnifying glass and the pincers DO carry the vanilla-block faces (their declared
	 * card scope) — pinned by presence, the inverse wall.
	 */
	@Test
	public void theRedLineUseOnWallsHold() throws NoSuchMethodException {
		assertThrows(NoSuchMethodException.class,
				() -> GTMonkeyWrenchItem.class.getDeclaredMethod("useOn", net.minecraft.world.item.context.UseOnContext.class),
				"the monkey wrench must not override useOn (the machine pool ruling)");
		assertNotNull(GTSoftHammerItem.class.getDeclaredMethod("useOn", net.minecraft.world.item.context.UseOnContext.class));
		assertNotNull(GTMagnifyingGlassItem.class.getDeclaredMethod("useOn", net.minecraft.world.item.context.UseOnContext.class));
		assertNotNull(GTPincersItem.class.getDeclaredMethod("useOn", net.minecraft.world.item.context.UseOnContext.class));
	}

	// ------------------------------------------------- the TAB_TABLE rows (the dynamic-tail form)

	/**
	 * The four machine-face rows are the table TAIL (rows size-4 .. size-1) in display
	 * order — the BendingCylinderSmallTest tail-index form, so the serial-merge queue
	 * (t1→t2→t3) rebases without touching this pin. The FULL size census is the
	 * GT6ToolsCreativeTabTest ratchet.
	 */
	@Test
	public void theFourRowsAreTheTableTailInOrder() {
		int tSize = GT6Tools.TAB_TABLE.size();
		assertTrue(tSize >= 14, "the table must carry at least the ten pre-wave rows plus the four");
		assertSame(GT6Tools.SOFT_HAMMER, GT6Tools.TAB_TABLE.get(tSize - 4), "the soft hammer is the tail's first row");
		assertSame(GT6Tools.MONKEY_WRENCH, GT6Tools.TAB_TABLE.get(tSize - 3), "the monkey wrench is the tail's second row");
		assertSame(GT6Tools.MAGNIFYING_GLASS, GT6Tools.TAB_TABLE.get(tSize - 2), "the magnifying glass is the tail's third row");
		assertSame(GT6Tools.PINCERS, GT6Tools.TAB_TABLE.get(tSize - 1), "the pincers is the tail's last row");
		assertEquals(rl("soft_hammer"), GT6Tools.SOFT_HAMMER.getId());
		assertEquals(rl("monkey_wrench"), GT6Tools.MONKEY_WRENCH.getId());
		assertEquals(rl("magnifying_glass"), GT6Tools.MAGNIFYING_GLASS.getId());
		assertEquals(rl("pincers"), GT6Tools.PINCERS.getId());
	}

	// ------------------------------------------------- the tag faces

	/**
	 * The four self-owned tags — the oredict-name snake translation ruling:
	 * craftingToolSoftHammer (CS.java:1891) → tools/soft_hammer,
	 * craftingToolMonkeyWrench → tools/monkey_wrench (the SINGLE-name card ruling — the
	 * upstream :144 wrench double-name does NOT fold into this tag), the magnifying glass
	 * and the pincers (CS.java:1880) the same shape.
	 */
	@Test
	public void tagNamesAreTheSnakeTranslations() {
		assertEquals(rl("tools/soft_hammer"), GT6ItemTags.TOOLS_SOFT_HAMMER.location());
		assertEquals(rl("tools/monkey_wrench"), GT6ItemTags.TOOLS_MONKEY_WRENCH.location());
		assertEquals(rl("tools/magnifying_glass"), GT6ItemTags.TOOLS_MAGNIFYING_GLASS.location());
		assertEquals(rl("tools/pincers"), GT6ItemTags.TOOLS_PINCERS.location());
		// the single-name census: the monkey-wrench tag is NOT the wrench tag
		assertNotEquals(GT6ItemTags.TOOLS_WRENCH.location(), GT6ItemTags.TOOLS_MONKEY_WRENCH.location());
	}

	// ------------------------------------------------- the rotation cut list (the card literals)

	/** The state.rotate family — stairs/doors/curved rails rotate under CLOCKWISE_90; pillars cycle their axis. */
	@Test
	public void theRotateFamilyRotates() {
		assertNotSame(Blocks.OAK_STAIRS.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.OAK_STAIRS.defaultBlockState()), "stairs rotate (the RCON facing face)");
		assertNotSame(Blocks.OAK_DOOR.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.OAK_DOOR.defaultBlockState()), "doors rotate");
		assertNotSame(Blocks.OAK_LOG.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.OAK_LOG.defaultBlockState()), "pillars cycle their axis (the upstream BlockRotatedPillar toggle — the vanilla CLOCKWISE_90 no-ops on Y)");
		assertNotSame(Blocks.RAIL.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.RAIL.defaultBlockState()), "curved rails rotate (the RCON rail-shape face)");
		BlockState tCurved = Blocks.RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE, RailShape.NORTH_EAST);
		assertNotSame(tCurved, GTSoftHammerItem.rotatedForm(tCurved), "the curve forms rotate too");
	}

	/**
	 * The straight rails — the powered AND activator rails are PoweredRailBlock instances
	 * (Blocks.java:708/:3115): the flat N-S ↔ E-W two-flip (the upstream (aMeta + 8) % 16
	 * arm, ToolCompat.java:273-284); ascending forms stay put (the 1.7.10 quirk pool).
	 */
	@Test
	public void theStraightRailsTwoFlip() {
		BlockState tNS = Blocks.POWERED_RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT, RailShape.NORTH_SOUTH);
		BlockState tEW = tNS.setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT, RailShape.EAST_WEST);
		assertEquals(tEW, GTSoftHammerItem.rotatedForm(tNS), "N-S flips to E-W");
		assertEquals(tNS, GTSoftHammerItem.rotatedForm(tEW), "E-W flips back to N-S");
		BlockState tAsc = Blocks.POWERED_RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT, RailShape.ASCENDING_NORTH);
		assertSame(tAsc, GTSoftHammerItem.rotatedForm(tAsc), "ascending forms stay (the quirk pool)");
		// the detector rail is NOT in the upstream arm (:273/:279 golden + activator only)
		BlockState tDetector = Blocks.DETECTOR_RAIL.defaultBlockState();
		assertSame(tDetector, GTSoftHammerItem.rotatedForm(tDetector), "the detector rail stays outside the cut list");
	}

	/** The fence gate (the HORIZONTAL_FACING cycle — no rotate override exists) and the lamp (the LIT cycle). */
	@Test
	public void theCycleFacesCycle() {
		assertNotSame(Blocks.OAK_FENCE_GATE.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.OAK_FENCE_GATE.defaultBlockState()), "fence gates cycle their facing");
		BlockState tUnlit = Blocks.REDSTONE_LAMP.defaultBlockState();
		BlockState tLit = GTSoftHammerItem.rotatedForm(tUnlit);
		assertEquals(Boolean.TRUE, tLit.getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT), "the unlit lamp cycles lit (the upstream :261-272 toggle)");
		assertEquals(tUnlit, GTSoftHammerItem.rotatedForm(tLit), "the lit lamp cycles back");
	}

	/**
	 * The outside-the-cut-list pool — pistons, furnaces, chests, hoppers, pumpkins (the
	 * upstream :285-297 arms CUT) and a plain stone block are all untouched; the whole
	 * GT6 machine side is the RED LINE external pool by the same ruling.
	 */
	@Test
	public void outsideTheCutListIsUntouched() {
		assertSame(Blocks.PISTON.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.PISTON.defaultBlockState()), "pistons cut");
		assertSame(Blocks.FURNACE.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.FURNACE.defaultBlockState()), "furnaces cut");
		assertSame(Blocks.CHEST.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.CHEST.defaultBlockState()), "chests cut");
		assertSame(Blocks.HOPPER.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.HOPPER.defaultBlockState()), "hoppers cut");
		assertSame(Blocks.CARVED_PUMPKIN.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.CARVED_PUMPKIN.defaultBlockState()), "pumpkins cut");
		assertSame(Blocks.STONE.defaultBlockState(), GTSoftHammerItem.rotatedForm(Blocks.STONE.defaultBlockState()), "plain blocks untouched");
	}

	// ------------------------------------------------- the AHA/HMM arm

	/** The sound literals — the villager voice pair (the card's open-question ruling mapping). */
	@Test
	public void theSoundsAreTheVillagerPair() {
		assertSame(SoundEvents.VILLAGER_YES, GTMagnifyingGlassItem.AHA_SOUND, "the AHA arm = entity.villager.yes (the 1.7.10 mob.villager.haggle counterpart)");
		assertSame(SoundEvents.VILLAGER_NO, GTMagnifyingGlassItem.HMM_SOUND, "the HMM arm = entity.villager.no (the 1.7.10 mob.villager.idle counterpart)");
	}

	/** The 1-in-3 HMM ratio (GT_Tool_MagnifyingGlass.java:68 verbatim) — the structure pin over fresh seeds. */
	@Test
	public void theSoundRatioIsOneInThree() {
		assertEquals(3, GTMagnifyingGlassItem.HMM_ONE_IN);
		boolean tSawAha = false;
		boolean tSawHmm = false;
		for (int i = 0; i < 64; i++) {
			SoundEvent tSound = GTMagnifyingGlassItem.inspectSound(RandomSource.create(i));
			if (tSound == GTMagnifyingGlassItem.AHA_SOUND) tSawAha = true;
			if (tSound == GTMagnifyingGlassItem.HMM_SOUND) tSawHmm = true;
		}
		assertTrue(tSawAha, "the AHA arm is reachable");
		assertTrue(tSawHmm, "the HMM arm is reachable");
	}

	// ------------------------------------------------- the class-shape walls

	/** The monkey wrench rides the GTWrenchItem subclass form (the card's 子类 ruling). */
	@Test
	public void theMonkeyWrenchExtendsTheWrench() {
		assertTrue(GTWrenchItem.class.isAssignableFrom(GTMonkeyWrenchItem.class));
	}
}
