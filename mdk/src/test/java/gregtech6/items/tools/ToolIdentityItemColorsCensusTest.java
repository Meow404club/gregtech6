package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.client.GTClientHandlers;

import gregapi.data.MT;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * Offline census pin for task tint-coverage-batch — the ItemColors registration seam
 * {@link GTClientHandlers#fourPassToolItems()}: the file (the 锉刀全白 root cause, its
 * four-layer model carries grayscale layers with no ItemColor) plus the nine
 * un-tinted single-tier families must ride the SAME
 * {@code GT6ToolLadder::fourPassTintARGB} face the screwdriver/hammer row registered
 * (GTClientHandlers.onRegisterToolIdentityItemColors). The {@code RegistryObject} list
 * form keeps both legs offline-safe ({@code getId().getPath()} never touches the live
 * registry, the GT6BeeHivesTest posture).
 *
 * <p>The four-pass VALUE math is MachineLadderTest's face (primary head/secondary
 * handle with the Steel/Spruce fallbacks); this pin adds one representative dispatch
 * row so the census and the colour source read as one card.
 */
public class ToolIdentityItemColorsCensusTest {

	/** The exact ten-id census, registration order (GT6Tools rows :164/:419/:556-:572/:255/:514/:520/:568). */
	private static final List<String> CENSUS = List.of(
			"file", "club", "scissors", "scoop", "plunger",
			"branch_cutter", "hand_drill", "rolling_pin",
			"bending_cylinder", "bending_cylinder_small");

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GT6MaterialTestSupport.materials(); // the hermetic bracket (task hermetic-pour-tests)
	}

	/** Acceptance: the seam carries exactly the ten ids (no drift, no omission — the file row is the root-cause pin). */
	@Test
	public void fourPassRegistrationCoversTheTenToolFamilies() {
		List<String> tPaths = GTClientHandlers.fourPassToolItems().stream()
				.map(tHandle -> tHandle.getId().getPath()).toList();
		assertEquals(CENSUS, tPaths, "the four-pass ItemColor registration census (file + the nine families)");
	}

	/** The colour source the census registers: the composed two-layer form tints whole-sprite on index 0 (the head pass). */
	@Test
	public void registeredLambdaTintsTheGrayscaleLayer() {
		// the identity-less single tier = the Steel head fallback over the grayscale sprite
		int tExpectedHead = 0xFF000000 | (MT.Steel.mRGBaSolid[0] << 16) | (MT.Steel.mRGBaSolid[1] << 8) | MT.Steel.mRGBaSolid[2];
		assertEquals(tExpectedHead, GT6ToolLadder.fourPassTintARGB(new ItemStack(Items.STICK), 0),
				"index 0 = the head pass, the Steel fallback (the composed-form whole-sprite tint)");
		// a stamped stack colours the head with its primary (the file rows once laddered)
		ItemStack tBronze = GT6ToolLadder.stampIdentity(new ItemStack(Items.STICK), MT.Bronze, 1.0F);
		int tBronzeHead = 0xFF000000 | (MT.Bronze.mRGBaSolid[0] << 16) | (MT.Bronze.mRGBaSolid[1] << 8) | MT.Bronze.mRGBaSolid[2];
		assertEquals(tBronzeHead, GT6ToolLadder.fourPassTintARGB(tBronze, 0), "the head follows the identity primary");
	}
}
