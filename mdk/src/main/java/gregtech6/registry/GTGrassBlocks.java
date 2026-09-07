package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

import gregtech6.block.GTGrassBlock;

/**
 * Registration home of the GT6 grass family (task p24-grass-block): <b>6 per-pair
 * Block+BlockItem registrations</b> over the upstream dye variants (NOT ecosystem types)
 * — meta 0..5 = Green/Lime/Black/LightGray/Yellow/Brown, the order pinned by THREE
 * upstream tables in the same sequence (the dye recipes BlockGrass.java:75-80, the Bath
 * rows Loader_Recipes_Other.java:467-472, the spray switch Behavior_Spray_Color.java
 * :154-160). Self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached
 * from the construct event (the GT6SprayCans shape; ADR-P3-4: GT6Mod stays untouched).
 *
 * <p>The per-pair granularity is the decisions.p24-grass-behavior-trim ④ ruling (the P8
 * ADR ④ / P21 GTStoneBlocks precedent): dye recipes, the spray-can colour routing and
 * the painting economy all need per-variant ITEM identities, which an EnumProperty
 * single block cannot express.
 *
 * <p><b>id scheme</b>: variant 0 (Green) keeps the bare id {@code gt6:grass}; the other
 * five suffix the colour snake — {@code gt6:grass_{lime,black,light_gray,yellow,brown}}
 * (the GTStoneBlocks.path variant-0-bare-id precedent).
 *
 * <p><b>dye wiring</b>: {@link #DYE_INDEXES} is the variant-order GT6 spray-can dye
 * index of each variant (GTSprayCanItem.DYE_NAMES order 0=Black..15=White), i.e. the
 * Behavior_Spray_Color.java:154-160 mapping transposed by variant — Green←2, Lime←10,
 * Black←0, LightGray←7, Yellow←11, Brown←3; the recipe band consumes the same colours
 * through the platform {@code Tags.Items.DYES_*} constants.
 *
 * <p><b>creative tab</b>: the vanilla BUILDING_BLOCKS tab — the upstream
 * {@code CreativeTabs.tabBlock} join (BlockBase.java:63), wired through the platform
 * {@code BuildCreativeModeTabContentsEvent} (forge 1.20.1 BuildCreativeModeTabContents
 * Event.java:32 + the forge docs items/index.md:37 usage face; NeoForge 21.1 the same
 * event/getTabKey/accept face — both legs construct through one shared listener body).
 *
 * <p><b>offline seam</b>: {@link #variant(int)} reads through
 * {@link #sVariantResolver} — the live face dereferences the RegistryObjects (mod-load
 * time), the offline tests inject vanilla stand-ins (the GTFluids.sFluidResolver shape;
 * inject with try/finally + reset, never leak into datagen).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GTGrassBlocks {

	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
	public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One grass variant row: the registry snake + the GT6 spray-can dye index that paints it. */
	public record GrassVariant(String snake, byte dyeIndex) {}

	/**
	 * The 6 variants in upstream meta order (BlockGrass.java:75-80 / Loader_Recipes_Other
	 * .java:467-472 / Behavior_Spray_Color.java:154-160, three same-sequence tables).
	 * {@code dyeIndex} is the GTSprayCanItem dye that produces the variant.
	 */
	public static final List<GrassVariant> VARIANTS = List.of(
			new GrassVariant("grass", (byte) 2),            // meta 0 Green
			new GrassVariant("grass_lime", (byte) 10),      // meta 1 Lime
			new GrassVariant("grass_black", (byte) 0),      // meta 2 Black
			new GrassVariant("grass_light_gray", (byte) 7), // meta 3 Light Gray
			new GrassVariant("grass_yellow", (byte) 11),    // meta 4 Yellow
			new GrassVariant("grass_brown", (byte) 3));     // meta 5 Brown

	/** The registry id paths, variant order — the datagen/lang/tag single source (variant 0 bare, the GTStoneBlocks.path rule). */
	public static final List<String> PATHS = VARIANTS.stream().map(GrassVariant::snake).toList();

	/**
	 * The colour texture tail of a registry path ({@code green/lime/black/light_gray/
	 * yellow/brown}) — the variant-semantics name of the borrowed PNG pair under
	 * {@code gt6:textures/block/grass/}. The UPSTREAM file names are the trap (meta 3
	 * LightGray renders the {@code NORMAL} PNG, meta 0 Green the {@code MEDIUM} one,
	 * Textures.java:530-565); this mapping encodes the CODE face, the borrow was done by
	 * this table (assets/README.md ledger), so consumers stay trap-blind.
	 */
	public static String textureOf(String aPath) {
		return aPath.equals(VARIANTS.get(0).snake()) ? "green" : aPath.substring("grass_".length());
	}

	/** The 6 blocks, variant order (supplier = the shared zero-field {@link GTGrassBlock}). */
	public static final List<RegistryObject<Block>> BLOCKS = registerBlocks();

	/** The 6 block items, variant order (the vanilla BlockItem — the name/tooltip route through the block). */
	public static final List<RegistryObject<Item>> ITEMS = registerItems();

	private static List<RegistryObject<Block>> registerBlocks() {
		List<RegistryObject<Block>> rList = new ArrayList<>(VARIANTS.size());
		for (GrassVariant tVariant : VARIANTS) {
			rList.add(BLOCKS_REG.register(tVariant.snake(), GTGrassBlock::new));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Item>> registerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(VARIANTS.size());
		for (int i = 0; i < VARIANTS.size(); i++) {
			int tIndex = i;
			rList.add(ITEMS_REG.register(VARIANTS.get(i).snake(),
					() -> new BlockItem(BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		return List.copyOf(rList);
	}

	/**
	 * The variant-order GT6 spray-can dye indexes ({@link GrassVariant#dyeIndex} column) —
	 * the Behavior_Spray_Color.java:154-160 switch by another face, pinned by the tests.
	 */
	public static byte[] dyeIndexes() {
		byte[] rIndexes = new byte[VARIANTS.size()];
		for (int i = 0; i < VARIANTS.size(); i++) rIndexes[i] = VARIANTS.get(i).dyeIndex();
		return rIndexes;
	}

	// ------------------------------------------------------------------ runtime access + the offline seam

	/** The live variant resolver (the mod-load face — RegistryObjects must be fired). */
	private static final java.util.function.IntFunction<Block> LIVE_RESOLVER = aIndex -> {
		RegistryObject<Block> tHandle = BLOCKS.get(aIndex);
		// the pre-registration face (the offline test JVM, the spray-can clinit path): no
		// GT grass exists yet — a null verdict instead of the RegistryObject IllegalStateException
		return tHandle.isPresent() ? tHandle.get() : null;
	};

	/** The swappable resolver (the GTFluids.sFluidResolver seam shape — tests inject stand-ins). */
	private static java.util.function.IntFunction<Block> sVariantResolver = LIVE_RESOLVER;

	/** The block of variant {@code aIndex} (0..5, upstream meta order). */
	public static Block variant(int aIndex) {
		return sVariantResolver.apply(aIndex);
	}

	/** True when {@code aBlock} is ANY of the 6 GT grass variants. */
	public static boolean isGrass(@Nullable Block aBlock) {
		if (aBlock == null) return false;
		for (int i = 0; i < VARIANTS.size(); i++) {
			if (sVariantResolver.apply(i) == aBlock) return true;
		}
		return false;
	}

	/** The variant index of {@code aBlock}, or -1 when it is not a GT grass variant. */
	public static int indexOf(@Nullable Block aBlock) {
		if (aBlock == null) return -1;
		for (int i = 0; i < VARIANTS.size(); i++) {
			if (sVariantResolver.apply(i) == aBlock) return i;
		}
		return -1;
	}

	/** Offline-test injection (the GTFluids seam discipline: try/finally + {@link #resetResolver}). */
	public static void useTestStandins(List<Block> aStandins) {
		if (aStandins.size() != VARIANTS.size()) throw new IllegalArgumentException("need exactly 6 stand-ins");
		sVariantResolver = aStandins::get;
	}

	/** Restore the live resolver after a test injection. */
	public static void resetResolver() {
		sVariantResolver = LIVE_RESOLVER;
	}

	// ------------------------------------------------------------------ lifecycle

	private GTGrassBlocks() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6SprayCans.onModConstruct shape). */
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
	 * The BUILDING_BLOCKS join (the upstream tabBlock face, BlockBase.java:63). The event
	 * fires per tab; only the building tab takes the family (all 6 items, variant order).
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
