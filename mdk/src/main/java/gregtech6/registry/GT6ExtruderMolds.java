package gregtech6.registry;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;

/**
 * The GT6 extruder-mold registration home — task w1-press-extruder-molds (the row0
 * plate/rod pair), extended to the tool-head family by task toolhead-r11c-extruder-heads:
 * the 8 {@code Shape_Extruder_*} head molds + the 8 {@code Shape_SimpleEx_*} low-heat
 * twins that the extruder.json head rows consume (MultiItemTechnological.java:215-222/
 * :276-283; the rest of the 30+30-mold upstream census stays POOLED with the forming/
 * wire/pipe cards). Card-owned self-contained {@code @EventBusSubscriber(MOD)}
 * DeferredRegister attached from the construct event (the GT6FoodCans shape verbatim;
 * GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemTechnological meta item (10015-10022/10215-10222); the port flattens to one
 * id per mold, snake of the upstream name ({@code "Extruder Shape (Sword Blade)"} →
 * {@code shape_extruder_sword}). The molds are PLAIN items — zero shaping behaviour on
 * the item itself, so a fresh {@code Item} carries the whole declared behaviour.
 *
 * <p><b>The not-consumable face</b> (the archaeology conclusion, remember id478): upstream
 * marks every mold recipe input with STACK SIZE 0 (RM.java:405/:407/:410-417, the
 * Handlers:750-762 handler walks) — size-0 is not portable to 1.20.1, so the port
 * carries the never-consumed net effect through {@code Recipe.sNotConsumable}, whose
 * production default consults {@link #isMold}. The mold identity rides the
 * {@link #EXTRUDER_SHAPES_TAG} tag (the bidirectional tag paradigm: the datagen provider
 * fills the membership, the runtime predicate reads it — the Recipe.VANILLA_TAG_TEST
 * seam shape), behind the {@link #sMoldTest} test seam (the offline JVM resolves no tags —
 * the sTagTest precedent, Recipe.java:98-114).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ExtruderMolds {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The mold-family tag — {@code gt6:extruder_shapes}. The upstream census has no oredict
	 * key for the extruder shapes (they match recipes by exact item), so the tag is the
	 * port's family face: the not-consumable predicate and future mold-keyed rows key on
	 * the TAG, not the item (the TOOLS_BUILDER_WAND ruling, GT6ItemTags.java:64-70).
	 */
	public static final TagKey<Item> EXTRUDER_SHAPES_TAG = TagKey.create(Registries.ITEM, new net.minecraft.resources.ResourceLocation("gt6", "extruder_shapes"));

	/**
	 * The plate mold — id {@code gt6:shape_extruder_plate} (upstream meta 10001, the
	 * RM.Extruder plate row's shaping tool, RM.java:405).
	 */
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PLATE = ITEMS.register("shape_extruder_plate",
			() -> new Item(new Item.Properties()));

	/**
	 * The rod mold — id {@code gt6:shape_extruder_rod} (upstream meta 10027, the RM.Extruder
	 * rod row's shaping tool, RM.java:407).
	 */
	public static final RegistryObject<Item> SHAPE_EXTRUDER_ROD = ITEMS.register("shape_extruder_rod",
			() -> new Item(new Item.Properties()));

	// ---- the tool-head family (task toolhead-r11c-extruder-heads; the rows these shape
	// ---- live in data/gt6/recipe_maps/extruder.json). Upstream metas 10015-10022 /
	// ---- 10215-10222 (MultiItemTechnological.java:215-222/:276-283), registration order
	// ---- preserved in {@link #MOLDS}.

	public static final RegistryObject<Item> SHAPE_EXTRUDER_SWORD = ITEMS.register("shape_extruder_sword",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PICKAXE = ITEMS.register("shape_extruder_pickaxe",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_SHOVEL = ITEMS.register("shape_extruder_shovel",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_AXE = ITEMS.register("shape_extruder_axe",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_HOE = ITEMS.register("shape_extruder_hoe",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_HAMMER = ITEMS.register("shape_extruder_hammer",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_FILE = ITEMS.register("shape_extruder_file",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_SAW = ITEMS.register("shape_extruder_saw",
			() -> new Item(new Item.Properties()));

	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_SWORD = ITEMS.register("shape_simple_ex_sword",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PICKAXE = ITEMS.register("shape_simple_ex_pickaxe",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_SHOVEL = ITEMS.register("shape_simple_ex_shovel",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_AXE = ITEMS.register("shape_simple_ex_axe",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_HOE = ITEMS.register("shape_simple_ex_hoe",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_HAMMER = ITEMS.register("shape_simple_ex_hammer",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_FILE = ITEMS.register("shape_simple_ex_file",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_SAW = ITEMS.register("shape_simple_ex_saw",
			() -> new Item(new Item.Properties()));

	/**
	 * The mold set in upstream meta order (the plate :186 lead, the head family
	 * 10015-10022, the rod :212 tail, then the SimpleEx head family 10215-10222).
	 */
	public static final List<RegistryObject<Item>> MOLDS = List.of(
			SHAPE_EXTRUDER_PLATE,
			SHAPE_EXTRUDER_SWORD, SHAPE_EXTRUDER_PICKAXE, SHAPE_EXTRUDER_SHOVEL, SHAPE_EXTRUDER_AXE,
			SHAPE_EXTRUDER_HOE, SHAPE_EXTRUDER_HAMMER, SHAPE_EXTRUDER_FILE, SHAPE_EXTRUDER_SAW,
			SHAPE_EXTRUDER_ROD,
			SHAPE_SIMPLE_EX_SWORD, SHAPE_SIMPLE_EX_PICKAXE, SHAPE_SIMPLE_EX_SHOVEL, SHAPE_SIMPLE_EX_AXE,
			SHAPE_SIMPLE_EX_HOE, SHAPE_SIMPLE_EX_HAMMER, SHAPE_SIMPLE_EX_FILE, SHAPE_SIMPLE_EX_SAW);

	/**
	 * The mold-identity seam (production default = the {@link #EXTRUDER_SHAPES_TAG} tag
	 * membership — {@code ItemStack.is}, the Recipe.VANILLA_TAG_TEST production binding).
	 * The offline JVM binds no tags (every tag reads empty), so the offline tests swap this
	 * with a fixture predicate — the same two-contract rule: the stub answers the negatives
	 * exactly as production does, and the positives the stub grants are exactly what the
	 * RCON live chain re-proves with the real registry.
	 */
	public static java.util.function.Predicate<ItemStack> sMoldTest = aStack -> aStack != null && !aStack.isEmpty() && aStack.is(EXTRUDER_SHAPES_TAG);

	/** The mold identity face {@code Recipe.sNotConsumable} consults — null-safe. */
	public static boolean isMold(@Nullable ItemStack aStack) {
		return sMoldTest.test(aStack);
	}

	private GT6ExtruderMolds() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6FoodCans.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6FoodCans onCommonSetup log shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 extruder molds registered: {} molds (row0 pair + the tool-head family + its SimpleEx twins)", MOLDS.size());
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered mold would throw here and fail the runServer gate.
			for (RegistryObject<Item> tMold : MOLDS) {
				GT6Mod.LOGGER.info("GT6 extruder mold registered: {}", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tMold.get()));
			}
		});
	}

	/**
	 * The MACHINES_TAB join (task toolhead-r11c-extruder-heads; the GT6SlicerBlades
	 * verbatim form, delivered by the class-level MOD-bus {@code @Mod.EventBusSubscriber}
	 * at the class head). Upstream the molds ride the GT tab list as MultiItemTechnological
	 * metas; the port pools them with the machines tab. The walk covers the FULL
	 * {@link #MOLDS} census — registered-but-tab-less is invisible in both the creative
	 * menu and JEI (the issue #10 lesson).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(net.minecraftforge.event.BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tMold : MOLDS) {
				aEvent.accept(new ItemStack(tMold.get()));
			}
		}
	}
}
