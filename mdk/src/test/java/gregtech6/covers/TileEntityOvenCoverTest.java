package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTModelProperties;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The oven cover chain tests (task p4-cover-core acceptance ①/② offline half): the
 * setCoverItem admission gates (06Covers :284-317), the hoe-class crowbar substitute
 * dismantle (:140-163), the use dispatch (:106-133), the validity sweep (:207-215) and
 * the render snapshot hook.
 */
public class TileEntityOvenCoverTest extends GTCoverTestBase {

	@Test
	void setCoverItemRejectsUnregisteredItems() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertFalse(tOven.setCoverItem((byte) 1, new ItemStack(Items.STICK), null, false, true), "no registered cover on the stack → :295-296 reject");
		assertNull(tOven.getCovers(), "the rejected install leaves no store behind (全空回 null)");
		assertFalse(tOven.isCovered((byte) 1));
	}

	@Test
	void setCoverItemInstallsRegisteredCovers() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true));
		assertTrue(tOven.isCovered((byte) 1));
		assertSame(CoverRegistry.get(new ItemStack(Items.IRON_INGOT)), tOven.getCovers().mBehaviours[1], "the singleton rides the lanes");
		assertEquals((short) CoverRegistry.getId(Items.IRON_INGOT), tOven.getCovers().mIDs[1]);
	}

	@Test
	void occupiedFaceRejectedAndSecondCoverStaysRejected() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true));
		assertFalse(tOven.setCoverItem((byte) 1, new ItemStack(Items.GOLD_INGOT), null, false, true), ":294 — the face is occupied");
		assertSame(CoverRegistry.get(new ItemStack(Items.IRON_INGOT)), tOven.getCovers().mBehaviours[1], "the first cover survives");
	}

	@Test
	void removalDissolvesTheStoreToNull() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true);
		assertTrue(tOven.setCoverItem((byte) 1, ItemStack.EMPTY, null, false, true), ":292 removal accepted");
		assertNull(tOven.getCovers(), ":313-317 — an all-empty store returns to null");
		// removing again is the :286 no-op
		assertFalse(tOven.setCoverItem((byte) 1, ItemStack.EMPTY, null, false, true));
	}

	/**
	 * Task p10-debug-oven-cover-resurrect regression (known_bugs 2026-09-01): a cover
	 * store mutation MUST flag the BE changed — the dispatch used to land on the
	 * TileEntityBase01Root.causeBlockUpdate override (final, mDoesBlockUpdate buffer),
	 * which shadows the persisting ICoverableTE default, so the removal left the chunk
	 * clean: the alive-window save kept the covers NBT on disk, later saves were skipped
	 * (ChunkMap.save !isUnsaved) and the dismantled cover resurrected across restart.
	 */
	@Test
	void coverStoreMutationsMarkTheBEChanged() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		int tBefore = tOven.mChangedCount;
		assertTrue(tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true));
		assertTrue(tOven.mChangedCount > tBefore, "the install reaches setChanged — the cover reaches disk with the next save");

		tBefore = tOven.mChangedCount;
		assertTrue(tOven.setCoverItem((byte) 1, ItemStack.EMPTY, null, false, true));
		assertNull(tOven.getCovers());
		assertTrue(tOven.mChangedCount > tBefore, "the dismantle reaches setChanged — the removal re-dirties the chunk so the stale covers NBT is overwritten (the resurrect root cause)");

		// the :286 no-op removal mutates nothing → no persistence mark
		tBefore = tOven.mChangedCount;
		assertFalse(tOven.setCoverItem((byte) 1, ItemStack.EMPTY, null, false, true));
		assertEquals(tBefore, tOven.mChangedCount, "the rejected no-op does not mark the BE changed");

		// the value/visual lanes ride the same seam (CoverData.value/visual →
		// sendBlockUpdateFromCover) — the emitter tier now persists too
		tOven.setCoverItem((byte) 2, new ItemStack(Items.IRON_INGOT), null, false, true);
		tBefore = tOven.mChangedCount;
		tOven.getCovers().value((byte) 2, (short) 3, true);
		assertTrue(tOven.mChangedCount > tBefore, "CoverData.value(aBlockUpdate=T) reaches setChanged through the same seam");
	}

	@Test
	void disallowedFaceRefusedAndSweptByCheckCoverValidity() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		tOven.mAllowMask = ~(1 << 1); // side 1 (UP) no longer admitted
		assertFalse(tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true), ":285 admission gate");
		// a cover already on the face (placed before the policy flip) is swept on the next onTickFirst
		tOven.mAllowMask = 0b111111;
		tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true);
		tOven.mAllowMask = ~(1 << 1);
		tOven.checkCoverValidity(); // :207-215 — the force removal + drop + break sound
		assertNull(tOven.getCovers(), "the swept face empties the store");
		assertEquals(1, tOven.mDropped.size(), ":211 — the cover drops");
		assertEquals(Items.IRON_INGOT, tOven.mDropped.get(0).getItem());
		assertEquals((byte) 1, tOven.mDropSides.get(0), "the drop records the covered face");
	}

	@Test
	void invalidSideRefused() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertFalse(tOven.setCoverItem((byte) 6, new ItemStack(Items.IRON_INGOT), null, false, true));
		assertFalse(tOven.setCoverItem((byte) -1, new ItemStack(Items.IRON_INGOT), null, false, true));
	}

	@Test
	void hoeDismantleDropsAndDissolves() {
		assertTrue(new ItemStack(Items.WOODEN_HOE).canPerformAction(ToolActions.HOE_DIG), "the crowbar substitute premise");
		TileEntityOvenCoverProbe tOven = leveledOven();
		tOven.setCoverItem((byte) 4, new ItemStack(Items.GOLD_INGOT), null, false, true);

		// a non-hoe tool is not a crowbar: the relay finds no cover behaviour → 0
		assertEquals(0, tOven.onCoverToolClick("", null, new ItemStack(Items.STICK), (byte) 4, false));
		assertTrue(tOven.isCovered((byte) 4), "the cover survives the non-tool click");

		// the hoe runs the :145-152 path
		long tDamage = tOven.onCoverToolClick("", null, new ItemStack(Items.WOODEN_HOE), (byte) 4, false);
		assertEquals(10000, tDamage, ":151 — the tool damage");
		assertEquals(1, tOven.mDropped.size(), ":149 — the cover drops (null player → the ST.place branch)");
		assertEquals(Items.GOLD_INGOT, tOven.mDropped.get(0).getItem());
		assertEquals((byte) 4, tOven.mDropSides.get(0));
		assertNull(tOven.getCovers(), ":313-317 — the store dissolves");
	}

	@Test
	void coversRideTheFullBEsaveLoad() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		tOven.setCoverItem((byte) 2, new ItemStack(Items.DIAMOND), null, false, true);

		CompoundTag tSaved = tOven.saveWithoutMetadata();
		assertTrue(tSaved.contains(ICoverableTE.NBT_COVERS, net.minecraft.nbt.Tag.TAG_COMPOUND), "the save form carries covers");

		TileEntityOvenCoverProbe tReloaded = bareOven();
		tReloaded.load(tSaved);
		assertTrue(tReloaded.isCovered((byte) 2), "the load path rehydrates the cover");
		assertSame(CoverRegistry.get(new ItemStack(Items.DIAMOND)), tReloaded.getCovers().mBehaviours[2]);
	}

	@Test
	void getModelDataCarriesTheSnapshot() {
		TileEntityOvenCoverProbe tOven = bareOven();
		assertFalse(tOven.getModelData().has(GTModelProperties.RENDER_SNAPSHOT), "uncovered → no snapshot, fallback rendering");

		tOven.setCoverItem((byte) 0, new ItemStack(Items.BRICKS), null, false, true);
		tOven.setCoverItem((byte) 5, new ItemStack(Items.EMERALD), null, false, true);
		ModelData tData = tOven.getModelData();
		assertTrue(tData.has(GTModelProperties.RENDER_SNAPSHOT));
		GTCoverRenderSnapshot tSnapshot = (GTCoverRenderSnapshot) tData.get(GTModelProperties.RENDER_SNAPSHOT);
		assertEquals((byte) ((1 << 0) | (1 << 5)), tSnapshot.mask(), "DOWN|EAST mask");
		assertSame(GTCoverTestBase.TEST_SPRITE, tSnapshot.sprite(net.minecraft.core.Direction.DOWN), "the :194 surface sprite");

		tOven.setCoverItem((byte) 0, ItemStack.EMPTY, null, false, true);
		tOven.setCoverItem((byte) 5, ItemStack.EMPTY, null, false, true);
		assertFalse(tOven.getModelData().has(GTModelProperties.RENDER_SNAPSHOT), "empty store → no snapshot again");
	}

	@Test
	void onCoverUseInstallsAndConsumesOne() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		ItemStack tHeld = new ItemStack(Items.IRON_INGOT, 4);
		assertTrue(tOven.onCoverUse(null, (byte) 1, tHeld, 0.5F, 0.5F, 0.5F), ":120-122 — the install consumes the click");
		assertTrue(tOven.isCovered((byte) 1));
		assertEquals(3, tHeld.getCount(), ":121 — one item paid (non-creative)");
	}

	@Test
	void onCoverUseFallsThroughOnNonCoverItems() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertFalse(tOven.onCoverUse(null, (byte) 1, new ItemStack(Items.STICK, 1), 0.5F, 0.5F, 0.5F), ":124 — the caller opens its GUI");
		assertNull(tOven.getCovers());
	}

	@Test
	void onCoverUseInterceptedOnCoveredFaces() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true);
		// AbstractCoverDefault.interceptClickRight = true — the covered face consumes any non-install click
		assertTrue(tOven.onCoverUse(null, (byte) 1, new ItemStack(Items.STICK), 0.5F, 0.5F, 0.5F));
		// installing a second cover type on the covered face is still rejected; the click consumed either way
		assertTrue(tOven.onCoverUse(null, (byte) 1, new ItemStack(Items.GOLD_INGOT), 0.5F, 0.5F, 0.5F));
		assertSame(CoverRegistry.get(new ItemStack(Items.IRON_INGOT)), tOven.getCovers().mBehaviours[1]);
	}

	@Test
	void syncFlagFollowsCoverVisuals() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true);
		tOven.getCovers().visual((byte) 1, (short) 2);
		assertTrue(tOven.onTickCheck(1), "06Covers :184-186 — the visual sync arms the sync window");
		tOven.onTickChecked(1); // :180 — resetSync rides onTickChecked
		assertFalse(tOven.getCovers().requiresSync());
	}
}
