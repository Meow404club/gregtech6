package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.concrete.GT6ConcreteBlock;
import gregtech6.block.concrete.GT6ConcreteSlabBlock;

/**
 * Registration home of the GT6 CONCRETE family (task concrete-blocks-register, the
 * Loader_Blocks.java:63/:67 rows): <b>64 per-pair Block+BlockItem registrations</b> —
 * 2 families (concrete, reinforced concrete) x 16 dye colours, full block + bottom slab
 * each — in the card-owned self-contained {@code @EventBusSubscriber(MOD)}
 * DeferredRegister shape (the GT6GrassBlocks/GT6FoamBlocks precedent; GT6Mod untouched).
 *
 * <p><b>granularity</b>: per-pair, NOT a colour-property single block (the P8 ADR ④ /
 * P24 grass ruling) — the dye Bath economy pours per-colour OUTPUT stacks over ALL FOUR
 * forms (Loader_Recipes_Other.java:452-458 walk {@code ST.make(block, 1, W)} -> colour i
 * for the two full blocks AND the two mSlabs[0] slabs), and the sawing row
 * (BlockMetaType.java:92, the b2c-sawing BLOCKED face this card unlocks) emits
 * {@code ST.make(this, 1, i) -> ST.make(mSlabs[0], 2, i)} per colour: per-variant ITEM
 * identities are the economy.
 *
 * <p><b>id scheme</b>: {@code gt6:concrete_<dye>} / {@code gt6:concrete_reinforced_<dye>}
 * and the slab ids suffixed {@code _slab} ({@link #slabPath}), the dye segment exactly the
 * {@link gregtech6.item.spraycan.GTSprayCanItem#DYE_IDS} snake (= the CS.DYE_INDEX meta
 * order 0=Black..15=White). NO bare {@code gt6:concrete}: the family is brand-new (no
 * legacy references to shield, so the ADR ② variant-0-bare-id rule has nothing to keep
 * stable — uniform suffixing).
 *
 * <p><b>creative tab</b>: the vanilla COLORED_BLOCKS tab — the upstream
 * {@code CreativeTabs.tabDecorations} join (BlockColored.java:48, the ctor that overrides
 * the BlockMetaType tabBlock default :63). 1.20.1 retired the standalone decorations tab
 * (CreativeModeTabs carries NO DECORATIONS key, javap 47.4.10); the dye-coloured
 * decorative block home is COLORED_BLOCKS — vanilla's own 16-dye concrete family lives
 * there. Wired through the platform {@code BuildCreativeModeTabContentsEvent} (the
 * GTGrassBlocks.onBuildTabContents form).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face with NO KubeJS-specific seam; wiring a RegistryObject-backed addon
 * event for the concrete ids is the declared defer (the GT6Bumbles.java:71-72 precedent
 * shape). The recipe face (the CR.shaped slab/reinforced rows) is a data-card domain,
 * standard datagen JSON, naturally moddable through the recipe events.
 *
 * <p><b>offline seam</b>: the class initialises offline (DeferredRegister static
 * registration accumulates suppliers only — the GT6RecipeMapDataB2cWashRowsPourTest
 * precedent), and {@link #variantOf} is unused offline; the tint listener reads the
 * fixed {@code dyeIndex} fields, no registry access.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ConcreteBlocks {

	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
	public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The two family snake roots in upstream declaration order (Loader_Blocks.java:63 then :67). */
	public static final List<String> FAMILIES = List.of("concrete", "concrete_reinforced");

	/** One (family, colour) pair — the 32-element registration/walk unit. */
	public record ConcreteRow(String family, boolean reinforced, int dyeIndex) {}

	/** The registry id of a full block ({@code concrete_light_gray} form). */
	public static String path(String aFamily, int aDyeIndex) {
		return aFamily + "_" + gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}

	/** The registry id of the paired slab ({@code concrete_light_gray_slab} form). */
	public static String slabPath(String aFamily, int aDyeIndex) {
		return path(aFamily, aDyeIndex) + "_slab";
	}

	/** The 32 (family, colour) pairs, family-major in Loader_Blocks order and colour-major in meta order. */
	public static List<ConcreteRow> registrationOrder() {
		List<ConcreteRow> rOrder = new ArrayList<>(FAMILIES.size() * 16);
		for (String tFamily : FAMILIES) {
			for (int i = 0; i < 16; i++) {
				rOrder.add(new ConcreteRow(tFamily, tFamily.equals(FAMILIES.get(1)), i));
			}
		}
		return rOrder;
	}

	/** The 32 full blocks, registration order (family-major, colour-major). */
	public static final List<RegistryObject<Block>> FULL_BLOCKS = registerFullBlocks();

	/** The 32 slabs (the mSlabs[0] face), registration order. */
	public static final List<RegistryObject<Block>> SLAB_BLOCKS = registerSlabBlocks();

	/** The 64 block items, registration order (the full-block band then the slab band). */
	public static final List<RegistryObject<Item>> ITEMS = registerItems();

	private static List<RegistryObject<Block>> registerFullBlocks() {
		List<RegistryObject<Block>> rList = new ArrayList<>(32);
		for (ConcreteRow tRow : registrationOrder()) {
			int tDye = tRow.dyeIndex();
			boolean tReinforced = tRow.reinforced();
			rList.add(BLOCKS_REG.register(path(tRow.family(), tDye), () -> new GT6ConcreteBlock(
					path(tRow.family(), tDye), tDye, tReinforced,
					tReinforced ? GT6ConcreteBlock.reinforcedProperties() : GT6ConcreteBlock.plainProperties())));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Block>> registerSlabBlocks() {
		List<RegistryObject<Block>> rList = new ArrayList<>(32);
		for (ConcreteRow tRow : registrationOrder()) {
			int tDye = tRow.dyeIndex();
			boolean tReinforced = tRow.reinforced();
			rList.add(BLOCKS_REG.register(slabPath(tRow.family(), tDye), () -> new GT6ConcreteSlabBlock(
					slabPath(tRow.family(), tDye), tDye, tReinforced,
					tReinforced ? GT6ConcreteSlabBlock.reinforcedProperties() : GT6ConcreteSlabBlock.plainProperties())));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Item>> registerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(64);
		// the id derives from the WALK (no eager RegistryObject deref — the handle is only
		// .get() inside the supplier, which runs at registration time, the GTGrassBlocks
		// registerItems form; an eager get() is the clinit NPE trap runData caught)
		for (int i = 0; i < FULL_BLOCKS.size(); i++) {
			ConcreteRow tRow = registrationOrder().get(i);
			int tIndex = i;
			rList.add(ITEMS_REG.register(path(tRow.family(), tRow.dyeIndex()),
					() -> new GTComposedNameItem(FULL_BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		for (int i = 0; i < SLAB_BLOCKS.size(); i++) {
			ConcreteRow tRow = registrationOrder().get(i);
			int tIndex = i;
			rList.add(ITEMS_REG.register(slabPath(tRow.family(), tRow.dyeIndex()),
					() -> new GTComposedNameItem(SLAB_BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		return List.copyOf(rList);
	}

	private GT6ConcreteBlocks() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6GrassBlocks.onModConstruct shape). */
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
	 * The COLORED_BLOCKS join (the upstream tabDecorations face, BlockColored.java:48 —
	 * the 1.20.1 tab mapping, see the class javadoc): all 64 items, registration order.
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.COLORED_BLOCKS) {
			for (RegistryObject<Item> tItem : ITEMS) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
