package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverTextureSimple;

/**
 * The CoverRegistry contract (task p4-cover-core acceptance ①): the Item holder key,
 * the short-id/meta lookup surface and the CoverData factory.
 */
public class CoverRegistryTest extends GTCoverTestBase {

	@Test
	void itemKeyLookup() {
		assertSame(CoverTextureSimple.class, CoverRegistry.get(new ItemStack(Items.BRICKS)).getClass());
		assertSame(CoverRegistry.get(new ItemStack(Items.GOLD_INGOT)), CoverRegistry.get(new ItemStack(Items.GOLD_INGOT)), "the singleton contract");
		assertNull(CoverRegistry.get((ItemStack) null), "null stack → null");
		assertNull(CoverRegistry.get(ItemStack.EMPTY), "empty stack → null");
		assertNull(CoverRegistry.get(new ItemStack(Items.STICK)), "an unregistered item → null");
	}

	@Test
	void shortIdMetaLookupSurface() {
		short tId = (short) CoverRegistry.getId(Items.DIAMOND);
		assertSame(CoverRegistry.get(tId, (short) 0), CoverRegistry.get(new ItemStack(Items.DIAMOND)), "the save-format twin of Item.getId");
		assertNull(CoverRegistry.get((short) 0, (short) 0), "id 0 → null (the CoverData empty lane)");
		assertNull(CoverRegistry.get(new ItemStack(Items.STICK)), "unregistered → null");
	}

	@Test
	void coverdataFactory() {
		TileEntityOvenCoverProbe tOven = bareOven();
		CoverData tFresh = CoverRegistry.coverdata(tOven, null);
		assertEquals(new CoverData(tOven).getClass(), tFresh.getClass(), "null nbt → the fresh store");
		CompoundTag tTag = new CompoundTag();
		tTag.putShort("a", (short) CoverRegistry.getId(Items.BRICKS));
		CoverData tBack = CoverRegistry.coverdata(tOven, tTag);
		assertEquals((short) CoverRegistry.getId(Items.BRICKS), tBack.mIDs[0], "the nbt form rehydrates");
	}
}
