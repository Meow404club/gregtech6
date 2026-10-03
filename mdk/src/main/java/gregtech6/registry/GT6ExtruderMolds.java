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
 * plate/rod pair), extended to the tool-head family by task toolhead-r11c-extruder-heads
 * (8+8 head molds), and closed to the FULL upstream family by task mold-extruder-shapes:
 * all 32 {@code Shape_Extruder_*} items + all 32 {@code Shape_SimpleEx_*} low-heat twins
 * (MultiItemTechnological.java:182-216/:258-292, metas 10000-10031/10200-10231 — the
 * Empty leads of both families included). Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GT6FoodCans shape verbatim; GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemTechnological meta item (10000-10031/10200-10231); the port flattens to one
 * id per mold, snake of the upstream IL field ({@code Shape_Extruder_Sword} →
 * {@code shape_extruder_sword}, the r11c form). The molds are PLAIN items — zero shaping
 * behaviour on the item itself, so a fresh {@code Item} carries the whole declared
 * behaviour.
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

	// ---- the FULL family (task mold-extruder-shapes): every upstream
	// ---- MultiItemTechnological meta 10000-10031 (:182-216) and 10200-10231 (:258-292),
	// ---- declared in meta order below and mirrored by {@link #MOLDS}. The row0 pair
	// ---- (task w1-press-extruder-molds) and the head family (task
	// ---- toolhead-r11c-extruder-heads, the rows extruder.json consumes) are the seated
	// ---- ancestors of this walk; the remaining 46 close the upstream census.

	public static final RegistryObject<Item> SHAPE_EXTRUDER_EMPTY = ITEMS.register("shape_extruder_empty",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PLATE = ITEMS.register("shape_extruder_plate",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_ROD_LONG = ITEMS.register("shape_extruder_rod_long",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_BOLT = ITEMS.register("shape_extruder_bolt",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_RING = ITEMS.register("shape_extruder_ring",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_CELL = ITEMS.register("shape_extruder_cell",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_INGOT = ITEMS.register("shape_extruder_ingot",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_WIRE = ITEMS.register("shape_extruder_wire",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_CASING = ITEMS.register("shape_extruder_casing",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PIPE_TINY = ITEMS.register("shape_extruder_pipe_tiny",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PIPE_SMALL = ITEMS.register("shape_extruder_pipe_small",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PIPE_MEDIUM = ITEMS.register("shape_extruder_pipe_medium",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PIPE_LARGE = ITEMS.register("shape_extruder_pipe_large",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PIPE_HUGE = ITEMS.register("shape_extruder_pipe_huge",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_BLOCK = ITEMS.register("shape_extruder_block",
			() -> new Item(new Item.Properties()));
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
	public static final RegistryObject<Item> SHAPE_EXTRUDER_GEAR = ITEMS.register("shape_extruder_gear",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_BOTTLE = ITEMS.register("shape_extruder_bottle",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PLATE_CURVED = ITEMS.register("shape_extruder_plate_curved",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_GEAR_SMALL = ITEMS.register("shape_extruder_gear_small",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_ROD = ITEMS.register("shape_extruder_rod",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_CCC = ITEMS.register("shape_extruder_ccc",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_FOIL = ITEMS.register("shape_extruder_foil",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_PLATE_TINY = ITEMS.register("shape_extruder_plate_tiny",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_EXTRUDER_WIRE_FINE = ITEMS.register("shape_extruder_wire_fine",
			() -> new Item(new Item.Properties()));

	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_EMPTY = ITEMS.register("shape_simple_ex_empty",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PLATE = ITEMS.register("shape_simple_ex_plate",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_ROD_LONG = ITEMS.register("shape_simple_ex_rod_long",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_BOLT = ITEMS.register("shape_simple_ex_bolt",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_RING = ITEMS.register("shape_simple_ex_ring",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_CELL = ITEMS.register("shape_simple_ex_cell",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_INGOT = ITEMS.register("shape_simple_ex_ingot",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_WIRE = ITEMS.register("shape_simple_ex_wire",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_CASING = ITEMS.register("shape_simple_ex_casing",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PIPE_TINY = ITEMS.register("shape_simple_ex_pipe_tiny",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PIPE_SMALL = ITEMS.register("shape_simple_ex_pipe_small",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PIPE_MEDIUM = ITEMS.register("shape_simple_ex_pipe_medium",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PIPE_LARGE = ITEMS.register("shape_simple_ex_pipe_large",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PIPE_HUGE = ITEMS.register("shape_simple_ex_pipe_huge",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_BLOCK = ITEMS.register("shape_simple_ex_block",
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
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_GEAR = ITEMS.register("shape_simple_ex_gear",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_BOTTLE = ITEMS.register("shape_simple_ex_bottle",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PLATE_CURVED = ITEMS.register("shape_simple_ex_plate_curved",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_GEAR_SMALL = ITEMS.register("shape_simple_ex_gear_small",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_ROD = ITEMS.register("shape_simple_ex_rod",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_CCC = ITEMS.register("shape_simple_ex_ccc",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_FOIL = ITEMS.register("shape_simple_ex_foil",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_PLATE_TINY = ITEMS.register("shape_simple_ex_plate_tiny",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHAPE_SIMPLE_EX_WIRE_FINE = ITEMS.register("shape_simple_ex_wire_fine",
			() -> new Item(new Item.Properties()));

	/**
	 * The mold set in upstream meta order: the full Shape_Extruder block (10000 Empty lead
	 * through 10031 Wire_Fine tail, MultiItemTechnological.java:182-216) then the full
	 * SimpleEx block (10200-10231, :258-292) — the registration walk verbatim.
	 */
	public static final List<RegistryObject<Item>> MOLDS = List.of(
			SHAPE_EXTRUDER_EMPTY,
			SHAPE_EXTRUDER_PLATE, SHAPE_EXTRUDER_ROD_LONG, SHAPE_EXTRUDER_BOLT, SHAPE_EXTRUDER_RING,
			SHAPE_EXTRUDER_CELL, SHAPE_EXTRUDER_INGOT, SHAPE_EXTRUDER_WIRE, SHAPE_EXTRUDER_CASING,
			SHAPE_EXTRUDER_PIPE_TINY, SHAPE_EXTRUDER_PIPE_SMALL, SHAPE_EXTRUDER_PIPE_MEDIUM, SHAPE_EXTRUDER_PIPE_LARGE,
			SHAPE_EXTRUDER_PIPE_HUGE, SHAPE_EXTRUDER_BLOCK, SHAPE_EXTRUDER_SWORD, SHAPE_EXTRUDER_PICKAXE,
			SHAPE_EXTRUDER_SHOVEL, SHAPE_EXTRUDER_AXE, SHAPE_EXTRUDER_HOE, SHAPE_EXTRUDER_HAMMER,
			SHAPE_EXTRUDER_FILE, SHAPE_EXTRUDER_SAW, SHAPE_EXTRUDER_GEAR, SHAPE_EXTRUDER_BOTTLE,
			SHAPE_EXTRUDER_PLATE_CURVED, SHAPE_EXTRUDER_GEAR_SMALL, SHAPE_EXTRUDER_ROD, SHAPE_EXTRUDER_CCC,
			SHAPE_EXTRUDER_FOIL, SHAPE_EXTRUDER_PLATE_TINY, SHAPE_EXTRUDER_WIRE_FINE,
			SHAPE_SIMPLE_EX_EMPTY,
			SHAPE_SIMPLE_EX_PLATE, SHAPE_SIMPLE_EX_ROD_LONG, SHAPE_SIMPLE_EX_BOLT, SHAPE_SIMPLE_EX_RING,
			SHAPE_SIMPLE_EX_CELL, SHAPE_SIMPLE_EX_INGOT, SHAPE_SIMPLE_EX_WIRE, SHAPE_SIMPLE_EX_CASING,
			SHAPE_SIMPLE_EX_PIPE_TINY, SHAPE_SIMPLE_EX_PIPE_SMALL, SHAPE_SIMPLE_EX_PIPE_MEDIUM, SHAPE_SIMPLE_EX_PIPE_LARGE,
			SHAPE_SIMPLE_EX_PIPE_HUGE, SHAPE_SIMPLE_EX_BLOCK, SHAPE_SIMPLE_EX_SWORD, SHAPE_SIMPLE_EX_PICKAXE,
			SHAPE_SIMPLE_EX_SHOVEL, SHAPE_SIMPLE_EX_AXE, SHAPE_SIMPLE_EX_HOE, SHAPE_SIMPLE_EX_HAMMER,
			SHAPE_SIMPLE_EX_FILE, SHAPE_SIMPLE_EX_SAW, SHAPE_SIMPLE_EX_GEAR, SHAPE_SIMPLE_EX_BOTTLE,
			SHAPE_SIMPLE_EX_PLATE_CURVED, SHAPE_SIMPLE_EX_GEAR_SMALL, SHAPE_SIMPLE_EX_ROD, SHAPE_SIMPLE_EX_CCC,
			SHAPE_SIMPLE_EX_FOIL, SHAPE_SIMPLE_EX_PLATE_TINY, SHAPE_SIMPLE_EX_WIRE_FINE);

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
			GT6Mod.LOGGER.info("GT6 extruder molds registered: {} molds (the full 32+32 upstream family)", MOLDS.size());
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
