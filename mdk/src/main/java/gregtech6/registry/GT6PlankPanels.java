package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.panels.GT6PlankPanelBlock;

/**
 * Registration home of the GT6 WOODEN COVER PANEL family (task material-mc-g3-plank-panels,
 * the residual_sweep G3 card — the MultiTileEntityPanelWood band, Loader_MultiTileEntities
 * .java:2057-2083): <b>28 per-row Block+BlockItem registrations</b>, ONE panel per plank row
 * the port world provides. The row face is NOT a hand-list parallel universe — it walks the
 * single plank authority ({@link GT6WoodDict#ROWS}, the planks-blockification face) 1:1, the
 * upstream semantic verbatim: upstream ships one panel row per {@code PlankData.PLANKS[i]}
 * texture slot (Loader :2058/:2067/:2076, i 0-299) and the slots fill at runtime from the
 * registered planks (PlankEntry.java:115-121), the invalid slots' rows ride
 * {@code NBT_HIDDEN} (Loader :2058 {@code NBT_HIDDEN, ST.invalid(PlankData.PLANKS[i])}) —
 * hidden, recipe-less, never seen. The port world's plank face is fixed (no fourth-party
 * mods), so the port fills exactly the rows its authority holds: the 23 upstream slots the
 * port planks occupy (vanilla 0-5, LoaderWoodDictionary.java:51-56; GT6 6/7/37/38/39/54/55/
 * 62/63/97/98/99/100/101/102/103/239, :66-172 — the FireProof twins collide on slot 0 and
 * hold none, first-come) + the 5 port-native 1.20.1 vanilla-tree rows (slot {@code -1} —
 * 1.7.10 predates Mangrove/Cherry/Bamboo/Crimson/Warped; they ride the GT6WoodDict
 * VANILLA_ROWS identity rule the oak-twin sawing rows established).
 *
 * <p><b>Negative ledger (the 277, pinned not fabricated)</b>: the remaining 300-23=277
 * upstream slots are the modded-wood ladder (BoP/Forestry/... compat registrations) — no
 * port plank face, no panel. The port registers neither blocks nor items for them; the
 * {@code GT6PlankPanelsRegistryTest} slot-mapping pin holds the fill set exact.
 *
 * <p><b>granularity</b>: per-pair, NOT a texture-index property block (the P8 ADR ④ ruling,
 * the G1 {@code GT6Panels} precedent) — the panel crafting emits per-plank OUTPUT stacks
 * ({@code CR.shaped 6x panel <- 1x plank + iron screws}, Loader :2060/:2069/:2078, only for
 * valid slots), so per-row ITEM identities are the economy.
 *
 * <p><b>id scheme</b>: {@code gt6:wooden_panel_<wood>} (the {@code <family>_<variant>} G1
 * form; the wood slugs live in {@link #ROW_TABLE}).
 *
 * <p><b>creative tab</b>: BUILDING_BLOCKS — the plank-family tab row (GT6TreeBlocks :214-221
 * precedent; the upstream "Panels" NEI category has no 1.20.1 vanilla tab, the undyed
 * decorative home rides the planks).
 *
 * <p><b>THE CLINIT DISCIPLINE (probe-proven, the runData gear-loss case)</b>: the
 * {@code @Mod.EventBusSubscriber} form makes FML {@code Class.forName} this class AT MOD
 * CONSTRUCT — before {@code MT.init()} (GT6Mod enqueues it onto enqueueWork, construct runs
 * first). Any static-initializer reach into {@link GT6WoodDict} from here would fire the
 * dict's clinit while {@code MT.WOODS.*} are still NULL, and the captured row materials
 * stay null for the WHOLE JVM — {@code hasPlank}/{@code plankOrNull} die, which silently
 * drops the {@code gear_gt_small/from_plank} recipe band (25 rows) from any fresh
 * regeneration alongside this panel face. So: this clinit touches ONLY {@link #ROW_TABLE}
 * (pure id/int literals) and the DeferredRegisters; every dict reference lives behind a
 * call-time lambda or the lazy {@link #rows()} (first call = datagen/recipe time, post
 * MT.init).
 *
 * <p><b>KJS surface declaration (the task-card wording)</b>: this card produces the
 * REGISTRATION face with NO KubeJS-specific seam; the blockstate/model/recipe faces are
 * datapack domains, naturally moddable through the standard events; the registration face
 * is the declared defer (the GT6TreeBlocks javadoc posture).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6PlankPanels {

	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
	public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The registry id of a panel row ({@code wooden_panel_oak} form).
	 *
	 * @param aSlug the wood slug ({@link RowSpec#slug}).
	 */
	public static String path(String aSlug) {
		return "wooden_panel_" + aSlug;
	}

	/**
	 * One row of the wood→(slug, upstream PlankData slot) table — the fill map of the
	 * upstream 300-slot band onto the port plank face. {@code upstreamSlot < 0} = the row
	 * holds NO upstream slot (the 1.20.1 vanilla-tree set, the declared port-native ride).
	 * Pure literals — clinit-safe (see the class clinit discipline).
	 */
	public record RowSpec(String plankId, String slug, int upstreamSlot) {}

	/**
	 * The 28-row table in {@link GT6WoodDict#ROWS} order (the 17 GT6 rows then the 11
	 * vanilla rows), the upstream slot assignments verbatim from LoaderWoodDictionary
	 * .java:51-56 (vanilla 0-5) and :66-172 (the GT6 ladder; the two MT.Wood rows are the
	 * DEFAULT_PLANK :66 → 55 and the Crate :161 → 63, the Compressed :54, the Treated :62).
	 */
	public static final List<RowSpec> ROW_TABLE = List.of(
		new RowSpec("rubber_planks",           "rubber",           6),  // :69
		new RowSpec("maple_planks",            "maple",            7),  // :71
		new RowSpec("willow_planks",           "willow",          37),  // :73
		new RowSpec("blue_mahoe_planks",       "blue_mahoe",      38),  // :75
		new RowSpec("hazel_planks",            "hazel",           39),  // :91
		new RowSpec("cinnamon_planks",         "cinnamon",        97),  // :93
		new RowSpec("coconut_planks",          "coconut",         98),  // :95
		new RowSpec("rainbowood_planks",       "rainbowood",      99),  // :97
		new RowSpec("plank_wood_compressed",   "compressed_wood", 54),  // :157
		new RowSpec("plank_wood",              "wood",            55),  // :66 DEFAULT_PLANK
		new RowSpec("plank_wood_treated",      "treated_wood",    62),  // :159
		new RowSpec("crate",                   "crate",           63),  // :161 the Crate
		new RowSpec("plank_wood_dead",         "dead_wood",      100),  // :165
		new RowSpec("plank_wood_rotten",       "rotten_wood",    101),  // :167
		new RowSpec("plank_wood_mossy",        "mossy_wood",     102),  // :169
		new RowSpec("plank_wood_frozen",       "frozen_wood",    103),  // :171
		new RowSpec("blue_spruce_planks",      "blue_spruce",   239),   // :113 (Planks2:0)
		new RowSpec("minecraft:oak_planks",      "oak",       0),        // :51
		new RowSpec("minecraft:spruce_planks",   "spruce",    1),        // :52
		new RowSpec("minecraft:birch_planks",    "birch",     2),        // :53
		new RowSpec("minecraft:jungle_planks",   "jungle",    3),        // :54
		new RowSpec("minecraft:acacia_planks",   "acacia",    4),        // :55
		new RowSpec("minecraft:dark_oak_planks", "dark_oak",  5),        // :56
		new RowSpec("minecraft:mangrove_planks", "mangrove", -1),        // the 1.20.1 tree set — no upstream slot
		new RowSpec("minecraft:cherry_planks",   "cherry",   -1),
		new RowSpec("minecraft:bamboo_planks",   "bamboo",   -1),
		new RowSpec("minecraft:crimson_planks",  "crimson",  -1),
		new RowSpec("minecraft:warped_planks",   "warped",   -1));

	/** How many rows fill a real upstream PlankData slot (the negative-ledger face: 300 − 23 = 277 unfilled). */
	public static final int FILLED_SLOTS = (int) ROW_TABLE.stream().filter(aRow -> aRow.upstreamSlot() >= 0).count();

	/** The upstream PlankData band size (Loader :2057-2083 — 3 x 100 rows, texture indices 0-299). */
	public static final int UPSTREAM_BAND = 300;

	/** The lazily-materialized walk (see {@link #rows()}) — volatile: the first materialization must publish fully. */
	private static volatile List<PanelRow> sRows;

	/** The 28 panel blocks, registration order. */
	public static final List<RegistryObject<Block>> BLOCKS = registerBlocks();

	/** The 28 block items, registration order. */
	public static final List<RegistryObject<Item>> ITEMS = registerItems();

	/**
	 * One (spec, plank) registration/walk unit — the plank leg resolved from {@link GT6WoodDict}.
	 * Built ONLY inside {@link #rows()} (never at construct).
	 */
	public record PanelRow(RowSpec spec, GT6WoodDict.PlankEntry plankEntry) {
		/** The registry id ({@code wooden_panel_oak} form). */
		public String path() {
			return GT6PlankPanels.path(this.spec.slug());
		}
	}

	/**
	 * The walk face: {@link #ROW_TABLE} joined to the plank authority 1:1, in table order.
	 * LAZY — the first call materializes and caches; a table id with no dict row (or the
	 * reverse) is a construction-time failure, the walk cannot drift from the authority.
	 * Call-time is always post-MT.init (datagen providers, recipes, runtime pours), so the
	 * dict clinit captures live materials (the class clinit discipline).
	 */
	public static List<PanelRow> rows() {
		List<PanelRow> tRows = sRows;
		if (tRows == null) {
			Map<String, GT6WoodDict.PlankEntry> tById = new LinkedHashMap<>();
			for (GT6WoodDict.PlankEntry tEntry : GT6WoodDict.ROWS) {
				tById.put(tEntry.id(), tEntry);
			}
			List<PanelRow> tBuild = new ArrayList<>(ROW_TABLE.size());
			for (RowSpec tSpec : ROW_TABLE) {
				GT6WoodDict.PlankEntry tEntry = tById.remove(tSpec.plankId());
				if (tEntry == null) {
					throw new IllegalStateException("the panel row table drifted from the plank authority: " + tSpec.plankId());
				}
				tBuild.add(new PanelRow(tSpec, tEntry));
			}
			if (!tById.isEmpty()) {
				throw new IllegalStateException("the plank authority rows the panel table never covers: " + tById.keySet());
			}
			sRows = tRows = List.copyOf(tBuild);
		}
		return tRows;
	}

	/**
	 * The live plank item of one row (CALL-TIME dict resolve — the block-supplier and
	 * datagen faces; never a construct-time read, the class clinit discipline).
	 */
	public static Item plankItem(RowSpec aSpec) {
		for (GT6WoodDict.PlankEntry tEntry : GT6WoodDict.ROWS) {
			if (tEntry.id().equals(aSpec.plankId())) return tEntry.plank().get();
		}
		throw new IllegalStateException("the panel row table drifted from the plank authority: " + aSpec.plankId());
	}

	/** The block's lazy plank handle (resolves at first tooltip/recipe read, post-MT.init). */
	private static Supplier<Item> plankHandle(RowSpec aSpec) {
		return () -> plankItem(aSpec);
	}

	private static List<RegistryObject<Block>> registerBlocks() {
		List<RegistryObject<Block>> rList = new ArrayList<>(ROW_TABLE.size());
		for (RowSpec tSpec : ROW_TABLE) {
			rList.add(BLOCKS_REG.register(path(tSpec.slug()),
					() -> new GT6PlankPanelBlock(path(tSpec.slug()), plankHandle(tSpec))));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Item>> registerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(ROW_TABLE.size());
		// the id derives from the WALK (no eager RegistryObject deref — the .get() only
		// runs inside the supplier at registration time, the GT6Panels clinit-NPE lesson)
		for (int i = 0; i < BLOCKS.size(); i++) {
			String tPath = path(ROW_TABLE.get(i).slug());
			int tIndex = i;
			rList.add(ITEMS_REG.register(tPath,
					() -> new GTComposedNameItem(BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		return List.copyOf(rList);
	}

	private GT6PlankPanels() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6TreeBlocks.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		net.minecraftforge.eventbus.api.IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*net.minecraftforge.eventbus.api.IEventBus tModBus =
				net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS_REG.register(tModBus);
		ITEMS_REG.register(tModBus);
	}

	/**
	 * The BUILDING_BLOCKS join (the plank-family tab row — GT6TreeBlocks :214-221): all 28
	 * items, registration order.
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			for (RegistryObject<Item> tItem : ITEMS) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
