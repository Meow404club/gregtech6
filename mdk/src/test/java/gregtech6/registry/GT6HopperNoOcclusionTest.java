/**
 * The symptom27 noOcclusion pin (task hopper-culling-xray — the GTNoOcclusionCensusTest
 * precedent form): EVERY hopper-family block (60 materials x the plain/queue pair = 120)
 * must register {@code canOcclude = false}. Symptom: placing a hopper culled the block
 * faces beneath it = the underground X-ray. Root cause being pinned: the family renders
 * the shared sub-cube funnel models (gt6_hopper.json — the top-rim elements from y=10,
 * plus the down/top variants) over the DEFAULT full-cube getShape ({@code GT6HopperBlock}
 * overrides neither getShape nor getOcclusionShape), so vanilla's {@code canOcclude = true}
 * (BlockBehaviour.java:911) + {@code getOcclusionShape = getShape} (:240-242) culled the
 * neighbor faces against the open funnel mouth — the exact issue #9 wire/pipe mechanism
 * (GTNoOcclusionCensusTest), whose full-scan verdict had reserved this fix ("hopper 漏斗
 * … 落地时须带 noOcclusion") because the family still rendered a full cube back then.
 *
 * <p>The pin works over the per-row property SEAM ({@code HopperRow.properties()}): the
 * static registration walk is the family's ONLY construction site and every row builds its
 * properties from that one chain, so asserting a block per ROW pins all 120 registration
 * points (a future row-specific property override breaks the walk-out and goes red).
 * The census size rides the same DeferredRegister offline contract as the wire/pipe pin.
 *
 * <p>The 顺核 verdict (not pinned — read-only): the tap/funnel/faucet attachment family
 * (GT6Attachments over {@code GTAttachmentSmallBlock}) and the mold family
 * ({@code GT6Molds.MoldBlock}) both OVERRIDE getShape with non-full-cube boxes, so their
 * occlusion shape is already non-full and no neighbor face is culled — no fix owed there;
 * the remaining细-shape families (grindstone/kitchen/mortar/sifting/surface/kinetics)
 * already carry .noOcclusion() (the GTNoOcclusionCensusTest roster).
 *
 * <p>Offline block construction needs the vanilla block registry unfrozen (the
 * GTNoOcclusionCensusTest bracket verbatim); the material registry is booted as cheap
 * insurance for the carrier rows.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GT6HopperNoOcclusionTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the block registry is usable by now
			// (the GTNoOcclusionCensusTest bracket).
		}
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		GTMaterialItems.initMaterials(); // insurance: the carrier rows dereference materials lazily
	}

	@Test
	public void everyHopperRowRegistersNoOcclusion() {
		assertEquals(120, GT6Hoppers.ROWS.size(),
				"the hopper row census drifted (60 loader materials x the plain/queue pair)");
		assertEquals(120, GT6Hoppers.BLOCKS.getEntries().size(), "the hopper register census drifted");
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			// the EXACT registration payload (the static walk's single construction site)
			assertFalse(new GT6Hoppers.GT6HopperBlock(tRow, tRow.properties()).defaultBlockState().canOcclude(),
					tRow.path() + " must register .noOcclusion() (symptom27: the sub-cube funnel model over the "
							+ "full-cube occlusion culls neighbor faces = the underground X-ray)");
		}
	}
}
