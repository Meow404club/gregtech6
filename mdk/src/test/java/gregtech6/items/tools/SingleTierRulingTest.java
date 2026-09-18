/**
 * The structural snapshot of task p31-single-tier-ruling (decisions.p31-single-tier-ruling):
 * which tool classes consume the {@code GT6ItemData} identity seam (implement
 * {@link GT6ToolLadder.LadderTool}, directly or through inheritance) and which carry
 * ZERO seam faces. The ruling itself is javadoc + the decisions key; this test pins its
 * one checkable invariant — the wiring split — so a future ladder card flipping a class
 * fails here until the test and the decisions key are updated together (the mismatch is
 * the alarm). Pure {@code Class} reflection: no Item construction (the mod-Item wall,
 * {@code GT6ToolsCreativeTabTest} boot premise), no fork markers.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregtech6.items.tools.electric.GT6ElectricToolItem;
import gregtech6.items.tools.pocket.GTPocketMultitoolItem;

public class SingleTierRulingTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected; the vanilla classes are resolvable by now (the tab test premise)
		}
	}

	/** The seam-consuming set: the four converted ladders (dig/blade/machine/identity-seam). */
	@Test
	public void ladderWiredClassesImplementTheLadderToolFace() {
		List<Class<?>> tWired = List.of(
				// the crowbar (p31-identity-seam) + the machine-face ladder (p31-machine-ladder)
				GTCrowbarItem.class, GTCutterItem.class, GTChiselItem.class, GTSawItem.class,
				GT6ScrewdriverItem.class, GTHammerItem.class, GTWrenchItem.class,
				GTMonkeyWrenchItem.class, GTMagnifyingGlassItem.class, GTPincersItem.class,
				GTSoftHammerItem.class,
				// the dig ladder (p31-dig-ladder)
				GTPickaxeItem.class, GTPickaxeGemItem.class, GTPickaxeConstructionItem.class,
				GTShovelItem.class, GTSpadeItem.class, GTHoeItem.class, GTAxeItem.class,
				// the axe_double rides INHERITANCE (GTAxeDoubleItem extends GTAxeItem) — the
				// ruling notes the faces are live; only its obtainment row is missing
				GTAxeDoubleItem.class,
				// the blade ladder (p31-blade-ladder)
				GTSwordItem.class, GTKnifeItem.class, GTButcheryKnifeItem.class);
		for (Class<?> tClass : tWired) {
			assertTrue(GT6ToolLadder.LadderTool.class.isAssignableFrom(tClass),
					tClass.getSimpleName() + " must implement GT6ToolLadder.LadderTool");
		}
	}

	/**
	 * The zero-seam-face set — every class the p31-single-tier-ruling walked and left
	 * identity-less: the ladder-candidate pool (until its ladder card) plus the permanent
	 * singles (the flint_and_tinder; the electric nineteen whose axis is voltage).
	 */
	@Test
	public void singleTierClassesCarryNoSeamFace() {
		List<Class<?>> tSingle = List.of(
				// the candidate pool (decisions.p31-single-tier-ruling)
				GT6FileItem.class, GT6BuilderWandItem.class,
				GT6BendingCylinderItem.class, GT6BendingCylinderSmallItem.class,
				GTUniversalSpadeItem.class, GTClubItem.class, GTPlowItem.class,
				GTBranchCutterItem.class, GTSenseItem.class, GTHandDrillItem.class,
				GTScissorsItem.class, GTScoopItem.class, GTPlungerItem.class,
				GTRollingPinItem.class, GTPocketMultitoolItem.class,
				// the permanent single-material rulings
				GTFlintAndTinderItem.class, GT6ElectricToolItem.class);
		for (Class<?> tClass : tSingle) {
			assertFalse(GT6ToolLadder.LadderTool.class.isAssignableFrom(tClass),
					tClass.getSimpleName() + " must stay seam-free until the decisions key is updated");
		}
	}
}
