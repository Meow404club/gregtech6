/**
 * The issue #9 noOcclusion census (task p38-issue9-wire-noocclusion): EVERY wire/pipe-family
 * block must register {@code canOcclude = false}. Root cause being pinned: these carriers
 * render sub-cube quads (GTWireBakedModel / the pipe family models) over the DEFAULT
 * full-cube getShape, and neither the family block classes nor the registration touched the
 * occlusion face — vanilla defaults {@code canOcclude = true} (BlockBehaviour.java:911) and
 * {@code getOcclusionShape = getShape} (:240-242), so {@code Block.shouldRenderFace} culled
 * the neighbor faces against the empty space around the thin bar = the hollow X-ray to the
 * underground (the VisGraph misread the wire cell the same way). The fix is the axle
 * pipe-block convention (GT6Kinetics.java:244 "noOcclusion for the sub-cube rod shape");
 * Embeddium 0.3.31 shares the contract (BlockOcclusionCache.shouldDrawSide returns true when
 * {@code !adjState.canOcclude()}), so the pin is the deterministic fix on BOTH renderers.
 *
 * <p>The pin works over the per-file property SEAMS ({@code GTWires.wireProperties()} /
 * {@code GTFluidPipes.pipeProperties()} / {@code GTItemPipes.pipeProperties()} — the
 * GT6Logistics.makeWireBlock test-replay seam form): each seam is the ONE chain every
 * registration point of that file builds from, so asserting a block built from the seam
 * pins the whole family. The family sizes are pinned over
 * {@code DeferredRegister.getEntries()} (populated by the register() calls before any
 * registry event fires, so the counts are assertable offline) — a census-size drift forces
 * a look, and a NEW registration point must reuse the seam to stay on the fixed chain.
 * The cfoam/leaves/axle/wheel families already carry noOcclusion and are pinned by their
 * own cards; the full-cube MODEL blocks (machines/multiblock parts/hoppers/boilers/...)
 * have full-cube shapes and are NOT in scope (issue #9 full-scan verdict: the four files
 * of this card are the complete hit set).
 *
 * <p>Offline block construction needs the vanilla block registry unfrozen (the
 * GTWireContactDamageTest bracket); the material registry is booted as cheap insurance for
 * the carrier rows (the GTWireSpecs post-MT.init lazy-row discipline).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.pipe.GTItemPipeBlock;
import gregtech6.block.wire.GTWireBlock;

public class GTNoOcclusionCensusTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the block registry is usable by now
			// (the GTWireContactDamageTest / GTBlockPropertyIdentityTest bracket).
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

	private static void assertNeverOccludes(String aFamily, Block aBlock) {
		BlockState tState = aBlock.defaultBlockState();
		assertFalse(tState.canOcclude(),
				aFamily + " must register .noOcclusion() (issue #9: a full-cube default shape over "
						+ "sub-cube quads culls neighbor faces = the underground X-ray)");
	}

	@Test
	public void wireFamilyBlocksNeverOcclude() {
		// the legacy pair ctor (no row identity) over the ONE shared family chain
		assertNeverOccludes("wire", new GTWireBlock(32, 1, 1, GTWires.wireProperties()));
		// the size pin over the real DeferredRegister entries: 2 legacy + 620 electric + 6 redstone + 1 laser
		assertEquals(2 + 620 + 6 + 1, GTWires.BLOCKS.getEntries().size(), "the wire register census drifted");
	}

	@Test
	public void fluidPipeBlocksNeverOcclude() {
		assertNeverOccludes("fluid pipe", new GTFluidPipeBlock(50, GTFluidPipes.pipeProperties()));
		assertEquals(2, GTFluidPipes.BLOCKS.getEntries().size(), "the fluid-pipe register census drifted");
	}

	@Test
	public void itemPipeBlocksNeverOcclude() {
		assertNeverOccludes("item pipe",
				new GTItemPipeBlock(GTItemPipes.ROWS.get(0), GTItemPipes.pipeProperties()));
		assertEquals(18, GTItemPipes.BLOCKS.getEntries().size(),
				"the item-pipe register census drifted (3 materials x 6 variants)");
	}

	@Test
	public void logisticsWireNeverOccludes() {
		// the direct test-replay seam (the EXACT makeWireBlock payload the DeferredRegister pushes)
		assertFalse(GT6Logistics.makeWireBlock().defaultBlockState().canOcclude(),
				"the logistics wire must register .noOcclusion() (issue #9 — the GTWires family face; "
						+ "the logistics CORE stays a full cube and keeps vanilla occlusion)");
	}
}
