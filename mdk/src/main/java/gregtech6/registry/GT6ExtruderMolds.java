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
 * The GT6 extruder-mold registration home — task p26-w1-press-extruder-molds, the row0
 * MINIMAL subset of the upstream {@code Shape_Extruder_*} domain (the content-unit
 * completeness ruling: the molds and their RM rows land on the SAME card, the P25
 * food-can row0 precedent). Card-owned self-contained {@code @EventBusSubscriber(MOD)}
 * DeferredRegister attached from the construct event (the GT6FoodCans shape verbatim;
 * GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>The subset — 2 items of the upstream 30-mold census (MultiItemTechnological.java
 * :182-256, everything else POOLED with the W2 forming-chain card):
 * <ul>
 * <li><b>the plate mold</b> — upstream {@code IL.Shape_Extruder_Plate} meta 10001
 *     "Extruder Shape (Plate)" (MultiItemTechnological.java:186); the shaping tool of the
 *     RM.Extruder plate row (:405, block + mold → 9 plates).</li>
 * <li><b>the rod mold</b> — upstream {@code IL.Shape_Extruder_Rod} meta 10027 "Extruder
 *     Shape (Rod)" (MultiItemTechnological.java:212); the shaping tool of the RM.Extruder
 *     rod row (:407, block + mold → 18 sticks).</li>
 * </ul>
 * POOLED: the other 28 Shape_Extruder_* molds, the Shape_SimpleEx_* low-heat family
 * (:254-285), the Shape_Mold_* press-mold family (the RM.Press rename/credit dynamic arms'
 * items — the GT6RecipeMapFormingPress row0 face carries their pooling note) and the
 * :406 plateCurved row (its mold leaves row0 with the W2 forming-chain card).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemTechnological meta item (10001/10027); the port flattens to one id per mold,
 * snake of the upstream name ({@code "Extruder Shape (Plate)"} →
 * {@code shape_extruder_plate}). The molds are PLAIN items — zero shaping behaviour on
 * the item itself, so a fresh {@code Item} carries the whole declared behaviour.
 *
 * <p><b>The not-consumable face</b> (the archaeology conclusion, remember id478): upstream
 * marks every mold recipe input with STACK SIZE 0 (RM.java:405/:407
 * {@code IL.Shape_Extruder_*.get(0)}) — size-0 is not portable to 1.20.1, so the port
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

	/** The row0 mold set in upstream meta order (:186 plate before :212 rod). */
	public static final List<RegistryObject<Item>> MOLDS = List.of(SHAPE_EXTRUDER_PLATE, SHAPE_EXTRUDER_ROD);

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
			GT6Mod.LOGGER.info("GT6 extruder molds registered: {} molds (the row0 subset: plate + rod)", MOLDS.size());
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered mold would throw here and fail the runServer gate.
			for (RegistryObject<Item> tMold : MOLDS) {
				GT6Mod.LOGGER.info("GT6 extruder mold registered: {}", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tMold.get()));
			}
		});
	}
}
