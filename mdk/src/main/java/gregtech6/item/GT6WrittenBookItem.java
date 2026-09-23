package gregtech6.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

import gregtech6.registry.GT6BookText;
import gregtech6.registry.GT6Books;

/**
 * The per-book written-book item (task p35-books-written) — every registry row of
 * {@link GT6BookText} gets one of these; the content is the row's static text through the
 * GT6Books single-source converter.
 *
 * <p>Leg split (the content carrier):
 * <ul>
 * <li>1.20.1 (forge): no data components exist — books are stack NBT
 * (tmp/vanilla-1.20.1 WrittenBookItem.java TAG_* face), and a plain item cannot seed
 * /give stacks with NBT. So {@link #use} synthesizes a vanilla written-book stack with
 * the converted NBT and opens it — exactly the upstream UT.Books.display shape
 * (gregapi/util/UT.java:572-574: {@code displayGUIBook(ST.make(Items.written_book, 1, 0, NBT))};
 * the 1.7.10 GT book item itself carried the content in a static map, not on the stack).</li>
 * <li>1.21.1 (neo): the content rides the item prototype's DEFAULT
 * {@code DataComponents.WRITTEN_BOOK_CONTENT} component (set in the GT6Books registration
 * lambda via {@code Item.Properties.component}) — every stack incl. /give IS a written
 * book, the vanilla {@code WrittenBookItem.use} opens it, and the vanilla byAuthor/generation
 * tooltip works for free (an asymmetry: the 1.20.1 tooltip face stays empty, the NBT the
 * vanilla appendHoverText reads never exists on our stacks).</li>
 * </ul>
 *
 * <p>Out of shape on purpose (the declared deviations, GT6Books javadoc): no creative-tab
 * entry (the tab system is the pool card), no dungeon-loot/printer recipe face (successor
 * card seams), pages shipped as the upstream English code face on both legs (the 743 dump
 * written.book.* zh page overrides are the deferred localization wave).
 */
public class GT6WrittenBookItem extends WrittenBookItem {

	private final GT6BookText.BookText book;

	public GT6WrittenBookItem(GT6BookText.BookText aBook, Item.Properties aProperties) {
		super(aProperties);
		book = aBook;
	}

	//? if forge {
	@Override
	public InteractionResultHolder<ItemStack> use(Level aLevel, Player aPlayer, InteractionHand aHand) {
		// the 1.20.1 content face: a vanilla written-book stack with the converted NBT
		// (title/author/pages/resolved — the tags the vanilla WrittenBookItem.makeSureTagIsValid
		// and BookViewScreen.WrittenBookAccess read), opened through the vanilla book screen.
		ItemStack tStack = new ItemStack(Items.WRITTEN_BOOK);
		CompoundTag tTag = new CompoundTag();
		tTag.putString("title", GT6Books.convertTitle(book));
		tTag.putString("author", book.author());
		ListTag tPages = new ListTag();
		for (String tPage : GT6Books.convertPages(book)) tPages.add(StringTag.valueOf(tPage));
		tTag.put("pages", tPages);
		tTag.putBoolean("resolved", true);
		tStack.setTag(tTag);
		aPlayer.openItemGui(tStack, aHand);
		aPlayer.awardStat(Stats.ITEM_USED.get(this));
		// the held stack is returned untouched (the vanilla use() face — reading is not consuming)
		return InteractionResultHolder.sidedSuccess(aPlayer.getItemInHand(aHand), aLevel.isClientSide);
	}
	//?}
}
