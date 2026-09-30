package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTItemPaintTint;
import gregtech6.itemdata.GT6ItemData;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.bees.GT6BumbleHiveBlockEntity;

/**
 * Offline gate for task 15-hive-jade-tint (issue #15 tail): the Jade icon tint chain —
 * the {@link GT6PaintIconProvider#iconStack} pure seam (the {@code getIcon} static, the
 * GT6MachineProviderTest posture: the BlockAccessor wrapper itself is live-only) and its
 * consumption by the fix-A {@link GTItemPaintTint#itemColor()} over the SAME payload form.
 *
 * <p>The fixture is the REAL hive BE over a vanilla-BRICKS BET (the
 * GT6BumbleHiveBlockEntityTest shape — no registry reads), so the c-face pins the exact
 * disease subject. The b-face rides BRICKS stacks as the material-less hive stand-in
 * (the GTItemPaintTintTest convention: the ItemColor tag read is item-agnostic, and the
 * hive's {@code tintMaterialOf} arm is material-less — identical code path, identical
 * values). The a-face (the registration membership + the REAL hive item) is the 21.1 leg
 * only: the forge leg resolves no RegistryObjects offline (the GT6BeeHivesTest
 * {@code paintableBlockArrayHoldsTheWholeFamily} posture — compilation is the forge-leg
 * proof that HIVE_ITEM joined {@code GTClientHandlers.beePaintItems()}).
 */
public class GT6PaintIconProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);

	/** The worldgen family colour (DYE_INT_LightGray 0xC0C0C0, the GT6BumbleHiveBlockEntityTest pin). */
	static final int FAMILY_GREY = 0xC0C0C0;

	static BlockEntityType<GT6BumbleHiveBlockEntity> sHiveType;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6BumbleHiveBlockEntity>[] tHolder =
				(BlockEntityType<GT6BumbleHiveBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BumbleHiveBlockEntity(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sHiveType = tHolder[0];
	}

	static GT6BumbleHiveBlockEntity hive() {
		return new GT6BumbleHiveBlockEntity(sHiveType, POS, Blocks.BRICKS.defaultBlockState());
	}

	// ------------------------------------------------------------------------------------
	// group c: the provider seam (painted BE -> the pair rides the stack; unpainted -> null)
	// ------------------------------------------------------------------------------------

	@Test
	void paintedBeverridesTheIconStackWithThePaintPair() {
		GT6BumbleHiveBlockEntity tHive = hive();
		assertTrue(tHive.paint(FAMILY_GREY), "the family colour stores (the worldgen born-painted face)");
		ItemStack tIcon = GT6PaintIconProvider.iconStack(Blocks.BRICKS, tHive);
		CompoundTag tPayload = GT6ItemData.rawTag(tIcon);
		assertTrue(tPayload.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED), "gt.painted rides the display copy");
		assertEquals(FAMILY_GREY, tPayload.getInt(TileEntityBase03TicksAndSync.NBT_COLOR), "gt.color rides verbatim");
	}

	@Test
	void unpaintedBeverridesNothing() {
		GT6BumbleHiveBlockEntity tHive = hive();
		assertTrue(!tHive.isPainted(), "fresh fixture = unpainted");
		assertNull(GT6PaintIconProvider.iconStack(Blocks.BRICKS, tHive),
				"an unpainted BE answers null — the Jade default icon stays (the bare stack carries no NBT either way)");
	}

	// ------------------------------------------------------------------------------------
	// group b: the ItemColor consumption — the FULL A->B icon chain offline
	// ------------------------------------------------------------------------------------

	@Test
	void theIconChainTintsThePaintedDisplayStack() {
		GT6BumbleHiveBlockEntity tHive = hive();
		tHive.paint(FAMILY_GREY);
		ItemStack tIcon = GT6PaintIconProvider.iconStack(Blocks.BRICKS, tHive);
		assertEquals(0xFFC0C0C0, GTItemPaintTint.itemColor().getColor(tIcon, 0),
				"the fix-A ItemColor consumes the fix-B stack at the stored colour (the Jade icon chain values)");
	}

	@Test
	void theUnpaintedFaceStaysFlatWhite() {
		// the :2041 fidelity arm — the hive's tintMaterialOf arm is material-less, so the
		// unpainted stack answers the -1 no-tint sentinel (byte-identical to the old stance)
		assertEquals(-1, GTItemPaintTint.itemColor().getColor(new ItemStack(Items.BRICKS, 1), 0),
				"a bare material-less stack (the hive shape) is white — the upstream flat-white icon preserved");
	}

	// ------------------------------------------------------------------------------------
	// group a: the registration membership — the 21.1 leg only (the GT6BeeHivesTest posture)
	// ------------------------------------------------------------------------------------

	//? if neoforge {
	/*// the trio rides ONE registration seam, and the REAL hive item speaks the same tint
	// values end to end (bare = the flat-white :2041 icon; paint-carrying = the family colour).
	@Test
	void beePaintRegistrationCoversTheHiveTrio() {
		net.minecraft.world.item.Item[] tItems = gregtech6.client.GTClientHandlers.beePaintItems();
		assertEquals(3, tItems.length, "the Bumbliary pair + the hive");
		assertSame(gregtech6.registry.GT6BeeHives.BUMBLIARY_ITEM.get(), tItems[0]);
		assertSame(gregtech6.registry.GT6BeeHives.BUMBLIARY_ADVANCED_ITEM.get(), tItems[1]);
		assertSame(gregtech6.registry.GT6BeeHives.HIVE_ITEM.get(), tItems[2], "the #15 tail: the hive joins");
		GT6BumbleHiveBlockEntity tHive = new GT6BumbleHiveBlockEntity(
				gregtech6.registry.GT6BeeHives.HIVE_BE.get(), POS,
				gregtech6.registry.GT6BeeHives.HIVE.get().defaultBlockState());
		assertEquals(-1, GTItemPaintTint.itemColor().getColor(
				new ItemStack(gregtech6.registry.GT6BeeHives.HIVE_ITEM.get()), 0),
				"the bare hive item is the flat-white :2041 icon");
		assertTrue(tHive.paint(FAMILY_GREY), "the born-painted face on the real hive BE");
		assertEquals(0xFFC0C0C0, GTItemPaintTint.itemColor().getColor(
				GT6PaintIconProvider.iconStack(gregtech6.registry.GT6BeeHives.HIVE.get(), tHive), 0),
				"the provider stack over the REAL hive block/BE tints at the family colour");
	}
	*///?}
}
