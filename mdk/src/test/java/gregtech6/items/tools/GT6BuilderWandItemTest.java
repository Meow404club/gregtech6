/**
 * Offline tests for task p24-builder-wand: the builder wand's classification face, the
 * pinned single-tier constants, the tag face, and the wear-accounting seam. The
 * structure-click chain itself (the neighbourhood walk, the part relay, the null-player
 * (T,F) consume) lives with the multiblock fixtures —
 * GTMultiBlockStructureCheckerFormTest.wandClicksScaffoldTheOvenNeighbourhoodByNeighbourhood
 * and the MultiBlockPartBlockEntityTest relay arms — because the stub level and the
 * controller/part fixtures are package-private there.
 *
 * <p>Assertion-surface notes (the FileSawTest constraints): a mod Item cannot be
 * constructed in this bootstrapped-and-frozen JVM (the Item.java:61 intrusive-holder
 * wall) — the classification and the wear accounting pin through the STATIC seams
 * ({@link GT6BuilderWandItem#classifies}, {@link GT6BuilderWandItem#wearOne}) over
 * VANILLA stacks. The creative ruling is structural: {@link GT6BuilderWandItem#wearOne}
 * carries NO exemption input at all — the parameter list is the proof (the vanilla
 * hurtAndBreak channel gates instabuild inside, which is why the creative click rides
 * this seam).
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.datagen.GT6ItemTags;

public class GT6BuilderWandItemTest {

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

	// ------------------------------------------------------- the action + id pins

	/** The action-name pin — "gt6_builderwand" (the FILE/SAW ADR-name shape). */
	@Test
	public void actionNameIsThePinnedLiteral() {
		assertEquals("gt6_builderwand", GT6ToolActions.BUILDER_WAND.name());
		assertNotSame(GT6ToolActions.BUILDER_WAND, GT6ToolActions.FILE, "the registry interns distinct actions");
	}

	/** The dispatch id — the upstream CS.TOOL_builderwand string verbatim (CS.java:1068). */
	@Test
	public void dispatchIdIsTheUpstreamString() {
		assertEquals("builderwand", GT6ToolActions.BUILDER_WAND_ID);
	}

	/**
	 * The stack classifier — the ONLY action this item performs is
	 * {@link GT6ToolActions#BUILDER_WAND} (the crowbar red-line shape): none of the
	 * sibling gt6 actions and none of the vanilla/Forge actions classify.
	 */
	@Test
	public void classifiesOnlyItsOwnAction() {
		assertTrue(GT6BuilderWandItem.classifies(GT6ToolActions.BUILDER_WAND), "the wand performs its own action");
		assertFalse(GT6BuilderWandItem.classifies(GT6ToolActions.CROWBAR), "not the crowbar");
		assertFalse(GT6BuilderWandItem.classifies(GT6ToolActions.CUTTER), "not the cutter");
		assertFalse(GT6BuilderWandItem.classifies(GT6ToolActions.CHISEL), "not the chisel");
		assertFalse(GT6BuilderWandItem.classifies(GT6ToolActions.FILE), "not the file");
		assertFalse(GT6BuilderWandItem.classifies(GT6ToolActions.SAW), "not the saw");
		assertFalse(GT6BuilderWandItem.classifies(ToolActions.AXE_DIG), "never a vanilla action alias");
	}

	// ------------------------------------------------------- the constants

	/**
	 * The single-tier pins (the declared deviation): 512 points (the family value),
	 * radius 2 (the mid-gem quality+1), the 10-unit unconditional dispatch return
	 * (upstream Base10 :135/:145), and the fold 10 units → 1 point
	 * (Behavior_Tool :63 units(10, 10000, 100, T) roundUp).
	 */
	@Test
	public void theSingleTierConstantsArePinned() {
		assertEquals(512, GT6BuilderWandItem.DURABILITY_POINTS);
		assertEquals(2, GT6BuilderWandItem.SCAFFOLD_RADIUS);
		assertEquals(10, GT6BuilderWandItem.SCAFFOLD_TOOL_DAMAGE);
		assertEquals(10000, GT6BuilderWandItem.UNITS_PER_DURABILITY_POINT);
		assertEquals(1, GT6BuilderWandItem.WEAR_PER_CLICK, "10 raw units fold to exactly one vanilla point");
	}

	/** The tag face — #gt6:tools/builder_wand (the snake ruling; no upstream oredict key, id402 proven). */
	@Test
	public void tagFaceIsTheSelfOwnedSnakeTag() {
		assertEquals(rl("tools/builder_wand"), GT6ItemTags.TOOLS_BUILDER_WAND.location());
	}

	// ------------------------------------------------------- the wear seam

	/** The wear accounting — exactly one point per call, NO exemption parameter (the doDamage ruling). */
	@Test
	public void wearOneAccountsExactlyOnePointPerCall() {
		ItemStack tWand = new ItemStack(Items.IRON_PICKAXE);
		int tMax = tWand.getMaxDamage();
		for (int i = 1; i <= 5; i++) {
			assertFalse(GT6BuilderWandItem.wearOne(tWand), "no wear-out before the boundary");
			assertEquals(i, tWand.getDamageValue(), "exactly one point per click, ungated");
		}
		assertTrue(tMax > 5, "the vanilla stack outlives the probe");
	}

	/**
	 * The wear-out boundary — the vanilla {@code hurt} contract (ItemStack.java:329
	 * {@code damage >= maxDamage}): the click that LANDS the damage on max breaks the
	 * wand — shrink + damage reset (the hurtAndBreak :339/:344 tail). The surviving
	 * last state is max-1.
	 */
	@Test
	public void wearOneBreaksAtTheExactBoundary() {
		ItemStack tWand = new ItemStack(Items.IRON_PICKAXE);
		int tMax = tWand.getMaxDamage();
		tWand.setDamageValue(tMax - 2);
		assertFalse(GT6BuilderWandItem.wearOne(tWand), "damage max-1 is the LAST surviving state");
		assertEquals(tMax - 1, tWand.getDamageValue());
		assertEquals(1, tWand.getCount());
		assertTrue(GT6BuilderWandItem.wearOne(tWand), "the click that REACHES max breaks the wand (vanilla hurt :329)");
		assertEquals(0, tWand.getCount(), "the stack shrank (the hurtAndBreak :339 tail)");
		assertEquals(0, tWand.getDamageValue(), "the damage reset (:344 tail)");
	}

	/** Non-damageable stacks are inert (the vanilla isDamageableItem gate, ItemStack.java:304). */
	@Test
	public void wearOneIgnoresNonDamageableStacks() {
		ItemStack tStick = new ItemStack(Items.STICK);
		assertFalse(GT6BuilderWandItem.wearOne(tStick), "nothing to wear");
		assertEquals(1, tStick.getCount());
		assertFalse(tStick.isEmpty(), "the stack survived untouched");
	}
}
