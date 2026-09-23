package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.item.GT6WrittenBookItem;

/**
 * The written-book registration home (task p35-books-written) — the GT6Sensors shape:
 * a self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event, one item per row over the generated {@link GT6BookText} data (the
 * mechanical extractor output, never hand-edited).
 *
 * <p>CENSUS BASIS (the task-card 申报, coordinator-approved): the upstream book loader
 * carries 20 createWrittenBook calls (loaders/b/Loader_Books.java), not the 3 the p35
 * census-refresh ledger claimed — :45/:61/:72 are just the first three calls (the same
 * read-truncation class as the RM "67 字段" errata ①). The obtainability face is the
 * "gt.books" dungeon-loot table (Loader_Loot.java:340-356, exactly 17 books); this port
 * ships its 15 STATIC books. The five CUT books (each with independent evidence, the
 * generator docstring carries the same list):
 * <ul>
 * <li>Manual_Punch_Cards (:45) — no obtainability face upstream at all (the punch-card
 * system was never implemented; the book is a placeholder).</li>
 * <li>Manual_Microwave (:61) — obtainable only through the Microwave recipe-map easter
 * egg (RecipeMapMicrowave.java:56); the Microwave RM is not ported.</li>
 * <li>Manual_Portal_TF (:111) — Twilight Forest domain (the TF portal-room chest +
 * TwilightTreasureReplacer.java:164/:176); TF is not ported.</li>
 * <li>Manual_Alloys (:582) / Manual_Elements (:596) — pages generated at runtime from
 * the OreDictMaterial registry (:574-580/:588-594), not static text; no mechanical
 * comparison target exists.</li>
 * </ul>
 *
 * <p>Shape projection: the upstream family is ONE meta item (ItemsGT.BOOKS, the
 * MultiItemBooks dye-carrier family) whose stacks carry title/author/pages NBT; the port
 * registers ONE item PER BOOK (the census 稀疏档 ruling — a written_book_content carrier
 * is per-title data), the item display name IS the book title (the lang walk, GT6EnUs).
 * The content conversion is the SINGLE-SOURCE converter below: the upstream
 * createWrittenBook page semantics (gregapi/util/UT.java:622-628) — pages of raw length
 * &gt;= 256 are dropped (no static page hits the bound, measured max 253; the rule only
 * ever fired for the dynamic books) and '¶' page markers become newlines — plus the
 * 1.21.1 written_book_content codec's 0..32 title bound (WrittenBookContent.CODEC title
 * field), applied on BOTH legs so the converter stays single-sourced (one title, the
 * "Hunting Guide for Blazes and Ghasts" 35-char row, truncates on the forge NBT leg too).
 *
 * <p>Obtainability: /give-reachable (the GT6Sensors posture — the creative-tab system is
 * the pool card). The dungeon-loot face (the upstream "gt.books" table via the p34 loot
 * injection seam) and the Printer recipe face (GT6_Main.java:352) are successor-card
 * seams, out of this card's files scope. The 1.7.10 zh langfile page overrides (the dump
 * carries 743 written.book.* page faces, tmp/gregtech.lang) are a deferred localization
 * wave: 1.20.1 book pages are plain strings (no per-locale component resolution) — the
 * shipped content is the upstream code-face English default, uniform on both legs.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Books {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The items, one per book row (the GT6Sensors ITEMS_BY_PATH form). */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();
	static {
		for (GT6BookText.BookText tRow : GT6BookText.BOOKS) {
			final GT6BookText.BookText fRow = tRow;
			//? if forge {
			ITEMS_BY_PATH.put(fRow.path(), ITEMS.register(fRow.path(),
					() -> new GT6WrittenBookItem(fRow, new Item.Properties())));
			//?} else {
			/*ITEMS_BY_PATH.put(fRow.path(), ITEMS.register(fRow.path(),
					() -> new GT6WrittenBookItem(fRow, new Item.Properties().component(
							net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
							new net.minecraft.world.item.component.WrittenBookContent(
									net.minecraft.server.network.Filterable.passThrough(convertTitle(fRow)),
									fRow.author(), 0,
									convertPages(fRow).stream()
											// the cast pins Filterable<Component> — literal() is a MutableComponent
											.map(aPage -> net.minecraft.server.network.Filterable.passThrough(
													(net.minecraft.network.chat.Component) net.minecraft.network.chat.Component.literal(aPage))).toList(),
									true)))));
			*///?}
		}
	}

	/**
	 * The single-source page converter — the upstream createWrittenBook semantics
	 * (gregapi/util/UT.java:622-628): pages of raw length &gt;= 256 are dropped (the
	 * 1.7.10 page cap; the else-branch printed a loader warning), '¶' markers become
	 * newlines. The raw length is measured BEFORE the marker replacement (the '¶' is one
	 * char, exactly like the upstream guard).
	 */
	public static List<String> convertPages(GT6BookText.BookText aBook) {
		return aBook.pages().stream()
				.filter(aPage -> aPage.length() < 256)
				.map(aPage -> aPage.replace("¶", "\n"))
				.toList();
	}

	/**
	 * The single-source title converter: the 1.21.1 written_book_content codec binds the
	 * title to 0..32 chars (net.minecraft.world.item.component.WrittenBookContent CODEC,
	 * {@code Filterable.codec(Codec.string(0, 32))} — a 35-char title would throw at
	 * encode time, a data-loss class bug); applied on both legs so the converter stays
	 * single-sourced. The full title survives as the item display name (the lang walk).
	 */
	public static String convertTitle(GT6BookText.BookText aBook) {
		return aBook.title().length() <= 32 ? aBook.title() : aBook.title().substring(0, 32);
	}

	private GT6Books() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Sensors shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Sensors fork).
		*///?}
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Sensors.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 books registered: {} written books ({} items valid)",
				GT6BookText.BOOKS.size(), ITEMS_BY_PATH.size()));
	}
}
