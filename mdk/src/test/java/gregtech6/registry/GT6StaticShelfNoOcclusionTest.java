/*
 * The symptom27 shelf-family noOcclusion pin (task hopper-culling-xray, the review-seat
 * stitch — the GT6HopperNoOcclusionTest form): EVERY BOOKSHELF row of the static storage
 * family (10 planks + 60 metals = 70) must register {@code canOcclude = false}. The
 * family renders the open-front six-box frame model (the 28-book BER niches) over the
 * FULL-CUBE SHELF_SHAPE (task shelf-crate-2px-realign's three-way ruling), so the default
 * {@code canOcclude = true} makes getOcclusionShape (=getShape, vanilla
 * BlockBehaviour.java:240-242/911) cull the neighbor faces against the open front — the
 * hopper family's exact X-ray mechanism, and vanilla's own hopper carries .noOcclusion()
 * (Blocks.java:3093) as the vanilla-side precedent.
 *
 * <p>The pin works over the per-row property SEAM ({@code StaticRow.properties()}): the
 * static registration walk is the family's ONLY construction site (GT6StaticStorages
 * static{} block), so asserting a block per ROW pins all 70 registration points.
 *
 * <p>The innocence pins (read-only boundary): the BOTTLECRATE rows owe NO patch — their
 * 8px CRATE_SELECTION_SHAPE keeps the occlusion shape non-full so no neighbor face is
 * culled (asserted by shape geometry + the default canOcclude surviving), and the closed
 * machine rows (lockers/safes — full-cube solid models) keep the default occlusion: the
 * patch must NOT leak beyond the shelf kind.
 *
 * <p>Offline block construction needs the vanilla block registry unfrozen (the
 * GT6HopperNoOcclusionTest bracket verbatim); the material registry is booted as cheap
 * insurance for the metal rows.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GT6StaticShelfNoOcclusionTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the block registry is usable by now
			// (the GT6HopperNoOcclusionTest bracket).
		}
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		GTMaterialItems.initMaterials(); // insurance: the metal rows dereference materials lazily
	}

	@Test
	public void everyShelfRowRegistersNoOcclusion() {
		long tShelfRows = GT6StaticStorages.ROWS.stream()
				.filter(tRow -> tRow.kind() == GT6StaticStorages.Kind.BOOKSHELF).count();
		assertEquals(70, tShelfRows,
				"the shelf family census drifted (10 planks + the 60-material metal ladder, Loader :143)");
		for (GT6StaticStorages.StaticRow tRow : GT6StaticStorages.ROWS) {
			if (tRow.kind() != GT6StaticStorages.Kind.BOOKSHELF) continue;
			// the EXACT registration payload (the static walk's single construction site)
			assertFalse(new GT6StaticStorages.GT6StorageBlock(tRow, tRow.properties()).defaultBlockState().canOcclude(),
					tRow.path() + " must register .noOcclusion() (symptom27: the open-front frame model over the "
							+ "full-cube SHELF_SHAPE culls neighbor faces = the shelf X-ray)");
		}
	}

	@Test
	public void theCrateStaysInnocent() {
		// the crate's 8px body box keeps getOcclusionShape non-full — no neighbor face is
		// culled, so no patch is owed (the hopper-card 顺核 verdict form)
		assertEquals(0.5, GT6StaticStorages.GT6StorageBlock.CRATE_SELECTION_SHAPE.max(Direction.Axis.Y),
				"the crate outline is the 8px body box (the 2px-realign ruling) — the innocence premise");
		GT6StaticStorages.StaticRow tCrate = GT6StaticStorages.ROWS.stream()
				.filter(tRow -> tRow.kind() == GT6StaticStorages.Kind.BOTTLECRATE).findFirst().orElseThrow();
		assertTrue(new GT6StaticStorages.GT6StorageBlock(tCrate, tCrate.properties()).defaultBlockState().canOcclude(),
				"the crate keeps the default occlusion (innocent — the patch must not leak to the crate kind)");
	}

	@Test
	public void theClosedMachineRowsKeepOcclusion() {
		// the lockers/safes render full-cube closed models — the default occlusion is
		// CORRECT there and must stay (the patch is bookshelf-scoped)
		GT6StaticStorages.StaticRow tLocker = GT6StaticStorages.ROWS.stream()
				.filter(tRow -> tRow.kind() == GT6StaticStorages.Kind.LOCKER).findFirst().orElseThrow();
		assertTrue(new GT6StaticStorages.GT6StorageBlock(tLocker, tLocker.properties()).defaultBlockState().canOcclude(),
				"the closed machine rows keep the full-cube occlusion (no over-patch)");
	}
}
