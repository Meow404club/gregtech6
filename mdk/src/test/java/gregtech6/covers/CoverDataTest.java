package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

//? if neoforge {
/*import net.minecraft.world.item.component.CustomData;
import gregtech6.registry.GT6DataComponents;
 *///?}

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverTextureSimple;

/**
 * CoverData NBT contract tests (task p4-cover-core acceptance ①): the 6-face parallel
 * arrays, the GT6 save-format key families (a-f/g-l/m-r/0-5/s-x/y), the
 * needsVisualsSaved write gate and the empty-compound lane normalization.
 */
public class CoverDataTest extends GTCoverTestBase {

	@Test
	void nbtRoundTripSixFaces() {
		CoverData tData = new CoverData(bareOven());
		for (byte i = 0; i < 6; i++) {
			tData.set(i, (short) CoverRegistry.getId(COVER_ITEMS[i]), (short) 0, null);
			tData.visual(i, (short) (7 + i));
			tData.value(i, (short) (100 + i));
		}
		tData.mStopped = true;

		CompoundTag tTag = tData.writeToNBT();

		CoverData tBack = new CoverData(bareOven(), tTag);
		for (byte i = 0; i < 6; i++) {
			assertEquals(CoverRegistry.getId(COVER_ITEMS[i]), tBack.mIDs[i], "id lane " + i);
			assertSame(CoverRegistry.get((short) CoverRegistry.getId(COVER_ITEMS[i]), (short) 0), tBack.mBehaviours[i], "behaviour lane " + i);
			assertEquals((short) (7 + i), tBack.mVisuals[i], "visual lane " + i);
			assertEquals((short) (100 + i), tBack.mValues[i], "value lane " + i);
			assertNull(tBack.mNBTs[i], "nbt lane " + i);
		}
		assertTrue(tBack.mStopped, "the y key rides along");
	}

	@Test
	void nbtKeysAreTheGT6SaveShape() {
		CoverData tData = new CoverData(bareOven());
		// face 0 only: the a/g/m/0/s family, no b..f leak, no y (not stopped)
		CompoundTag tPayload = new CompoundTag();
		tPayload.putBoolean("foo", true);
		ItemStack tCoverStack = new ItemStack(Items.BRICKS);
		//? if forge {
		tCoverStack.setTag(tPayload);
		//?} else {
		/*CustomData.set(GT6DataComponents.COVER_PAYLOAD, tCoverStack, tPayload);
		 *///?}
		tData.set((byte) 0, tCoverStack);
		tData.visual((byte) 0, (short) 3);
		tData.value((byte) 0, (short) 9);
		CompoundTag tTag = tData.writeToNBT();

		assertTrue(tTag.contains("a"), "face 0 id key");
		assertFalse(tTag.contains("b"), "face 1 id key must not leak");
		assertFalse(tTag.contains("g"), "zero metas are omitted (upstream :72)");
		assertTrue(tTag.contains("m"), "visual key");
		assertTrue(tTag.contains("0"), "value key");
		assertTrue(tTag.contains("s"), "nbt key");
		assertTrue(tTag.getCompound("s").getBoolean("foo"), "the payload survives");
		assertFalse(tTag.contains("y"), "mStopped=false omits y");

		CoverData tBack = new CoverData(bareOven(), tTag);
		assertEquals(Items.BRICKS, tBack.getCoverItem((byte) 0).getItem(), "the removal drop rebuilds");
		//? if forge {
		assertTrue(tBack.getCoverItem((byte) 0).getTag().getBoolean("foo"), "the payload rehydrates");
		//?} else {
		/*CustomData tBackData = tBack.getCoverItem((byte) 0).get(GT6DataComponents.COVER_PAYLOAD);
		assertTrue(tBackData != null && tBackData.copyTag().getBoolean("foo"), "the payload rehydrates");
		 *///?}
	}

	@Test
	void emptyCompoundsAndEmptyStacksNormalizeToNull() {
		CoverData tData = new CoverData(bareOven());
		// upstream :129/:50 — an empty compound lane is stored as null
		tData.set((byte) 0, (short) CoverRegistry.getId(Items.BRICKS), (short) 0, new CompoundTag());
		assertNull(tData.mNBTs[0], "empty compound → null");
		// the EMPTY-stack form of set() clears the face (upstream :123)
		tData.set((byte) 0, ItemStack.EMPTY);
		assertEquals((short) 0, tData.mIDs[0], "empty stack clears the id lane");
		assertNull(tData.mBehaviours[0], "empty stack clears the behaviour lane");
	}

	@Test
	void needsVisualsSavedGatesTheVisualWrite() {
		CoverData tData = new CoverData(bareOven());
		ICover tVisualCover = new CoverTextureSimple(TEST_SPRITE) {
			@Override
			public boolean needsVisualsSaved(byte aSide, CoverData aData) {
				return true;
			}
		};
		CoverRegistry.put(Items.BRICKS, tVisualCover);
		tData.set((byte) 0, (short) CoverRegistry.getId(Items.BRICKS), (short) 0, null);
		tData.set((byte) 1, (short) CoverRegistry.getId(Items.IRON_INGOT), (short) 0, null);
		tData.visual((byte) 0, (short) 5);
		tData.visual((byte) 1, (short) 6);

		CompoundTag tItemForm = tData.writeToNBT(new CompoundTag(), false); // the item-NBT form (06Covers :83)
		assertFalse(tItemForm.contains("n"), "needsVisualsSaved=false drops the visual");
		assertTrue(tItemForm.contains("m"), "needsVisualsSaved=true keeps the visual");

		CompoundTag tSaveForm = tData.writeToNBT(new CompoundTag(), true); // the save form (:74)
		assertTrue(tSaveForm.contains("m") && tSaveForm.contains("n"), "aIncludeVisuals writes both");
	}

	@Test
	void setStoppedFiresOnStoppedUpdateOnTheChange() {
		CoverData tData = new CoverData(bareOven());
		tData.set((byte) 0, (short) CoverRegistry.getId(Items.BRICKS), (short) 0, null);
		assertFalse(tData.setStopped(false), "no change, no fire");
		assertTrue(tData.setStopped(true), "the change fires");
		assertTrue(tData.mStopped);
		assertTrue(tData.writeToNBT().getBoolean("y"), "the y key persists the stop");
	}

	@Test
	void requiresSyncFollowsTheVisualFlags() {
		CoverData tData = new CoverData(bareOven());
		assertFalse(tData.requiresSync());
		tData.visual((byte) 3, (short) 1);
		assertTrue(tData.requiresSync(), "the :150 flag arms the sync");
		tData.resetSync();
		assertFalse(tData.requiresSync());
	}
}
