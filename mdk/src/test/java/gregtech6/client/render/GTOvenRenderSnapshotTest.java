package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.CoverRegistry;
import gregtech6.covers.GTCoverRenderSnapshot;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The RENDER_SNAPSHOT-contract tests for the oven snapshot (task p9-render-c-oven-overlay
 * acceptance): the record is a self-contained immutable projection (no BE reference,
 * boolean payload only), {@code getModelData()} projects the live mActive/mRunning fields
 * at the call moment (single-writer: the snapshot is a reader, never a second write path),
 * and the second ModelProperty coexists with the p4 cover chain — a covered oven carries
 * BOTH snapshots and the cover value is never overwritten.
 */
public class GTOvenRenderSnapshotTest extends GTMachinesOfflineTestBase {

	private static final ResourceLocation TEST_SPRITE = new ResourceLocation("gt6", "block/cover/test_plate");

	private static BlockEntityType<TileEntityOven> sSnapshotOvenType;

	@BeforeAll
	static void buildSnapshotOvenFixture() {
		sSnapshotOvenType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(sSnapshotOvenType, aPos, aState),
				Blocks.BRICKS).build(null);
	}

	private static TileEntityOven oven() {
		return new TileEntityOven(sSnapshotOvenType, new BlockPos(5, 2, 3), Blocks.BRICKS.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the snapshot contract
	// ---------------------------------------------------------------------------

	@Test
	void snapshotIsASelfContainedImmutableProjection() {
		GTOvenRenderSnapshot tSnapshot = new GTOvenRenderSnapshot(true, false);
		assertTrue(tSnapshot instanceof GTRenderSnapshot, "rides the render-snapshot contract");
		assertEquals(new GTOvenRenderSnapshot(true, false), tSnapshot, "value equality (record)");
		assertNotEquals(new GTOvenRenderSnapshot(false, false), tSnapshot);
		// the payload is two primitive booleans — nothing reachable can reference a live BE
		assertEquals(true, tSnapshot.active());
		assertEquals(false, tSnapshot.running());
	}

	// ---------------------------------------------------------------------------
	// the getModelData projection
	// ---------------------------------------------------------------------------

	@Test
	void getModelDataProjectsTheLiveFields() {
		TileEntityOven tOven = oven();
		tOven.mActive = true;
		tOven.mRunning = true;
		ModelData tData = tOven.getModelData();
		assertTrue(tData.has(GTModelProperties.OVEN_SNAPSHOT), "the oven snapshot is always present (uncovered ovens too)");
		assertEquals(new GTOvenRenderSnapshot(true, true), tData.get(GTModelProperties.OVEN_SNAPSHOT));

		tOven.mActive = false;
		tOven.mRunning = false;
		assertEquals(new GTOvenRenderSnapshot(false, false), tOven.getModelData().get(GTModelProperties.OVEN_SNAPSHOT),
				"the next getModelData() re-projects the mutated fields");
		// the old ModelData is untouched — the snapshot was frozen at the call moment
		assertEquals(new GTOvenRenderSnapshot(true, true), tData.get(GTModelProperties.OVEN_SNAPSHOT),
				"the earlier snapshot is immutable (render-thread view)");
	}

	@Test
	void uncoveredOvenKeepsTheCoverKeyAbsent() {
		TileEntityOven tOven = oven();
		tOven.mActive = true;
		ModelData tData = tOven.getModelData();
		assertTrue(tData.has(GTModelProperties.OVEN_SNAPSHOT));
		assertFalse(tData.has(GTModelProperties.RENDER_SNAPSHOT), "uncovered → no cover snapshot (the p4 contract line)");
	}

	// ---------------------------------------------------------------------------
	// cover coexistence — the second ModelProperty
	// ---------------------------------------------------------------------------

	@Test
	void coveredOvenCarriesBothSnapshots() {
		TileEntityOven tOven = oven();
		tOven.mActive = false;
		tOven.mRunning = true;
		CoverRegistry.reset();
		CoverRegistry.put(Items.IRON_INGOT, new CoverTextureSimple(TEST_SPRITE));
		try {
			assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.IRON_INGOT), null, false, true), "cover install");
			ModelData tData = tOven.getModelData();
			assertTrue(tData.has(GTModelProperties.RENDER_SNAPSHOT), "the cover chain still publishes its snapshot");
			assertTrue(tData.has(GTModelProperties.OVEN_SNAPSHOT), "the oven snapshot rides the second property alongside");
			GTCoverRenderSnapshot tCover = (GTCoverRenderSnapshot) tData.get(GTModelProperties.RENDER_SNAPSHOT);
			assertEquals((byte) (1 << 4), tCover.mask(), "the cover mask is unbroken (WEST face)");
			assertSameSprite(TEST_SPRITE, tCover);
			assertEquals(new GTOvenRenderSnapshot(false, true), tData.get(GTModelProperties.OVEN_SNAPSHOT),
					"the oven projection is unbroken by the cover install");
		} finally {
			CoverRegistry.reset();
		}
	}

	private static void assertSameSprite(ResourceLocation aExpected, GTCoverRenderSnapshot aCover) {
		assertEquals(aExpected, aCover.sprite(net.minecraft.core.Direction.WEST));
	}

	@Test
	void emptyCoverStoreKeepsOnlyTheOvenSnapshot() {
		TileEntityOven tOven = oven();
		CoverRegistry.reset();
		CoverRegistry.put(Items.IRON_INGOT, new CoverTextureSimple(TEST_SPRITE));
		try {
			assertTrue(tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true));
			assertTrue(tOven.setCoverItem((byte) 1, ItemStack.EMPTY, null, false, true), "removal accepted");
			ModelData tData = tOven.getModelData();
			assertFalse(tData.has(GTModelProperties.RENDER_SNAPSHOT), "empty store → the cover snapshot dissolves (the p4 contract line)");
			assertTrue(tData.has(GTModelProperties.OVEN_SNAPSHOT), "the oven projection persists");
		} finally {
			CoverRegistry.reset();
		}
	}
}
