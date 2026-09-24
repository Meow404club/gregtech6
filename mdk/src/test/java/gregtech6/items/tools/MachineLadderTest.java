package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * Offline tests for task p31-machine-ladder — the machine-family material ladder over
 * the {@link GT6ToolLadder} seam (the DigLadderTest premise: the mod-Item wall keeps the
 * items out, the math rides vanilla stacks — the identity is item-agnostic, so the
 * stamped {@link Items#STICK} stands in for every family item; the CLASS faces pinned
 * here are the one-line {@link GT6ToolLadder} delegations the items run).
 *
 * <p>Material anchors (MT.java, the qual(type, speed, durability, quality) rows):
 * Steel = qual(3, 6.0, 512, 2) — the identity-less fallback, Bronze = qual(3, 5.5, 448, 2),
 * TungstenSteel = qual(3, 10.0, 5120, 4).
 *
 * <p>The LIVE per-material registration/attribute face is the RCON chain
 * p31_tool_ladder_machine_stats (the id686 lesson: cleanTest green never proves a
 * registration alive).
 */
public class MachineLadderTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		MT.init();
	}

	private static ItemStack stamped(OreDictMaterial aMaterial, float aMultiplier) {
		return GT6ToolLadder.stampIdentity(new ItemStack(Items.STICK), aMaterial, aMultiplier);
	}

	// ------------------------------------------------------------------ the form multipliers

	/** The ×1.0 family (wrench/cutter/chisel/saw/screwdriver/hammer/crowbar/magnifying glass/pincers). */
	@Test
	public void familyMultipliersAreUnity() {
		assertEquals(1.0F, GTWrenchItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GTCutterItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GTChiselItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GTSawItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GT6ScrewdriverItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GTHammerItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GTMagnifyingGlassItem.DURABILITY_MULTIPLIER);
		assertEquals(1.0F, GTPincersItem.DURABILITY_MULTIPLIER);
		// the monkey wrench inherits the wrench face (the subclass form)
		assertTrue(GTMonkeyWrenchItem.classifies(GTMonkeyWrenchItem.ACTION));
	}

	/** The soft hammer's upstream ×8 (GT_Tool_SoftHammer.java:79-81) is LIVE: Steel ×8 = 4096. */
	@Test
	public void softHammerMultiplierIsLive() {
		assertEquals(8.0F, GTSoftHammerItem.MAX_DURABILITY_MULTIPLIER);
		assertEquals(4096, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(new ItemStack(Items.STICK),
				GTSoftHammerItem.MAX_DURABILITY_MULTIPLIER)), "identity-less = Steel 512 ×8 (the pre-ladder flat 512 was the pool-cut shell)");
		assertEquals(3584, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(stamped(MT.Bronze, 8.0F), 8.0F)),
				"Bronze 448 ×8 = 3584");
	}

	// ------------------------------------------------------------------ durability + the math seams

	/** The ×1.0 family ladder: Bronze 448 / TungstenSteel 5120, the identity-less arm stays 512. */
	@Test
	public void durabilityScalesPerMaterial() {
		assertEquals(448, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(stamped(MT.Bronze, 1.0F), 1.0F)));
		assertEquals(5120, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(stamped(MT.TungstenSteel, 1.0F), 1.0F)));
		assertEquals(512, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(new ItemStack(Items.STICK), 1.0F)),
				"the identity-less arm = the Steel fallback = the family constant");
	}

	// ------------------------------------------------------------------ tint + name

	/** The family tint face: index 0 = the material mRGBaSolid (steel fallback), others the sentinel. */
	@Test
	public void tintFollowsTheMaterialWithTheSteelFallback() {
		ItemStack tBronze = stamped(MT.Bronze, 1.0F);
		int tExpected = 0xFF000000 | (MT.Bronze.mRGBaSolid[0] << 16) | (MT.Bronze.mRGBaSolid[1] << 8) | MT.Bronze.mRGBaSolid[2];
		assertEquals(tExpected, GTWrenchItem.tintARGB(tBronze, 0));
		assertEquals(tExpected, GTCutterItem.tintARGB(tBronze, 0));
		assertEquals(-1, GTWrenchItem.tintARGB(tBronze, 1), "the overlay pass stays the -1 sentinel");
		assertEquals(0xFF000000 | (MT.Steel.mRGBaSolid[0] << 16) | (MT.Steel.mRGBaSolid[1] << 8) | MT.Steel.mRGBaSolid[2],
				GTWrenchItem.tintARGB(new ItemStack(Items.STICK), 0), "the identity-less arm = the steel fallback");
	}

	/**
	 * Task p38-issue6-tool-4layer-tint: the screwdriver/hammer four-pass tint — index 0 =
	 * the head (the primary, the Steel fallback), index 2 = the handle (the secondary,
	 * the Spruce fallback), the overlays stay the -1 sentinel (the supersession of the
	 * census-erratum "composed single, un-tinted" ruling; upstream GT_Tool_Screwdriver
	 * .getRGBa :120-122 / GT_Tool_HardHammer.getRGBa :127-129 verbatim).
	 */
	@Test
	public void fourPassTintFollowsPrimaryAndSecondary() {
		ItemStack tStamped = new ItemStack(Items.STICK);
		GT6ItemData.set(tStamped, GT6ToolStats.KEY,
				GT6ToolStats.of(MT.Bronze, MT.WOODS.Spruce, 1.0F));
		int tHead = 0xFF000000 | (MT.Bronze.mRGBaSolid[0] << 16) | (MT.Bronze.mRGBaSolid[1] << 8) | MT.Bronze.mRGBaSolid[2];
		int tHandle = 0xFF000000 | (MT.WOODS.Spruce.mRGBaSolid[0] << 16) | (MT.WOODS.Spruce.mRGBaSolid[1] << 8) | MT.WOODS.Spruce.mRGBaSolid[2];
		assertEquals(tHead, GT6ToolLadder.fourPassTintARGB(tStamped, 0), "index 0 = the head pass (the primary)");
		assertEquals(tHandle, GT6ToolLadder.fourPassTintARGB(tStamped, 2), "index 2 = the handle pass (the secondary)");
		assertEquals(-1, GT6ToolLadder.fourPassTintARGB(tStamped, 1), "the head OVERLAY stays un-tinted");
		assertEquals(-1, GT6ToolLadder.fourPassTintARGB(tStamped, 3), "the handle OVERLAY stays un-tinted");
		// the identity-less arms: head = the Steel fallback, handle = the Spruce fallback
		ItemStack tBare = new ItemStack(Items.STICK);
		assertEquals(0xFF000000 | (MT.Steel.mRGBaSolid[0] << 16) | (MT.Steel.mRGBaSolid[1] << 8) | MT.Steel.mRGBaSolid[2],
				GT6ToolLadder.fourPassTintARGB(tBare, 0), "identity-less head = the steel fallback");
		assertEquals(tHandle, GT6ToolLadder.fourPassTintARGB(tBare, 2), "identity-less handle = the spruce fallback");
	}

	/** The composed name: the identity-less stack keeps the bare key, a stamped one appends the material. */
	@Test
	public void nameComposesTheMaterialWord() {
		ItemStack tBare = new ItemStack(Items.STICK);
		assertEquals("item.stick", GT6ToolLadder.displayName(tBare, "item.stick").getString(),
				"the identity-less arm = the bare translatable");
		String tNamed = GT6ToolLadder.displayName(stamped(MT.Bronze, 1.0F), "item.stick").getString();
		assertTrue(tNamed.startsWith("item.stick ("), "the composed form appends the parenthesised material");
		assertTrue(tNamed.toLowerCase().contains("bronze"), "the material word rides the gt6.material family");
	}

	// ------------------------------------------------------------------ the crowbar unify (identity-seam → ladder)

	/** The crowbar's per-class statics survived the unify: explicit-missing materialOf, the ratio face. */
	@Test
	public void crowbarStaticsKeptTheirArms() {
		assertNull(GTCrowbarItem.materialOf(new ItemStack(Items.STICK)), "no identity → null (the explicit-missing arm)");
		assertEquals(512, GTCrowbarItem.durabilityPoints(null), "the legacy arm = the ADR value");
		assertEquals(448, GTCrowbarItem.durabilityPoints(GT6ToolStats.of(MT.Bronze, null, 1.0F)));
		// the unified instance face reads the same numbers through the ladder route
		assertEquals(512, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(new ItemStack(Items.STICK), 1.0F)));
	}

	// ------------------------------------------------------------------ the identity payload shape (unchanged seam)

	/** The stamped payload keys stay the dig/blade seam's — zero migration for existing stacks. */
	@Test
	public void stampedPayloadKeepsTheSeamShape() {
		ItemStack tStack = stamped(MT.Bronze, 1.0F);
		assertEquals(MT.Bronze, GT6ToolLadder.materialOf(tStack), "the seam read face on both legs");
		//? if forge {
		CompoundTag tRaw = GT6ItemData.rawTag(tStack);
		assertTrue(tRaw.contains(GT6ToolStats.KEY.nbtName()), "the identity rides the keyed compound (the GT.ToolStats carrier)");
		//?} else {
		/*// the 1.21.1 carrier is the registered DataComponentType (the GT6ItemDataTest fork):
		//the root-NBT raw read is forge-only, the component presence is the neo face.
		assertTrue(GT6ItemData.find(tStack, GT6ToolStats.KEY).isPresent(), "the component carries the payload");
		*///?}
	}
}
