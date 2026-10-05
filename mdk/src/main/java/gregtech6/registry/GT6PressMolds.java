package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The GT6 press-mold registration home — task explosives-chain, the bullet-casing mold
 * trio of the upstream {@code Shape_Press} domain (MultiItemTechnological.java:352-354,
 * metas 10896-10898 "Bullet Casing Mold (Small/Medium/Large)"; the task-card citation
 * "Loader_MultiTileEntities.java:352-358" was the file-drift erratum — those lines are the
 * Mold BLOCKS rung, the casing molds are MIT ITEMS). The molds are the shaping tools of the
 * RM.Press bullet-casing rows (Loader_Recipes_Other.java:657-668) — the rows themselves
 * stay POOLED with the bullet card (their not-consumable input face is not expressible in
 * the JSON v1 row schema); this card ships the registration + the handcraft trio
 * (:356-358, GT6CraftingRecipes). Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event (the
 * GT6ExtruderMolds shape verbatim — the sibling extruder family class stays untouched, the
 * r11c head-family extension rides a different branch).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemTechnological meta item; the port flattens to one id per mold, snake of the IL
 * field name (IL.java:232 — {@code Shape_Press_Bullet_Casing_Small/Medium/Large} →
 * {@code shape_press_bullet_casing_small}/{@code _medium}/{@code _large}). The molds are
 * PLAIN items — zero shaping behaviour on the item itself, so a fresh {@code Item} carries
 * the whole declared behaviour.
 *
 * <p><b>The not-consumable face</b> is NOT wired here (YAGNI — no consuming row ships in
 * this card): the upstream rows mark the mold input size-0 (never consumed); when the
 * bullet card pours them it joins the molds into a family tag behind the
 * {@code Recipe.sNotConsumable} seam (the GT6ExtruderMolds.EXTRUDER_SHAPES_TAG paradigm) or
 * a row-schema extension.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6PressMolds {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The small mold — id {@code gt6:shape_press_bullet_casing_small} (upstream meta 10896,
	 * the :657/:660/:663/:666 casing-row shaping tool).
	 */
	public static final RegistryObject<Item> SHAPE_PRESS_BULLET_CASING_SMALL = ITEMS.register("shape_press_bullet_casing_small",
			() -> new Item(new Item.Properties()));

	/**
	 * The medium mold — id {@code gt6:shape_press_bullet_casing_medium} (upstream meta
	 * 10897, the :658/:661/:664/:667 casing-row shaping tool).
	 */
	public static final RegistryObject<Item> SHAPE_PRESS_BULLET_CASING_MEDIUM = ITEMS.register("shape_press_bullet_casing_medium",
			() -> new Item(new Item.Properties()));

	/**
	 * The large mold — id {@code gt6:shape_press_bullet_casing_large} (upstream meta 10898,
	 * the :659/:662/:665/:668 casing-row shaping tool).
	 */
	public static final RegistryObject<Item> SHAPE_PRESS_BULLET_CASING_LARGE = ITEMS.register("shape_press_bullet_casing_large",
			() -> new Item(new Item.Properties()));

	/** The row0 mold set in upstream meta order (:352 small, :353 medium, :354 large). */
	public static final List<RegistryObject<Item>> MOLDS = List.of(
			SHAPE_PRESS_BULLET_CASING_SMALL, SHAPE_PRESS_BULLET_CASING_MEDIUM, SHAPE_PRESS_BULLET_CASING_LARGE);

	private GT6PressMolds() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6ExtruderMolds.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/**
	 * The MACHINES_TAB join (the GT6SlicerBlades.onBuildTabContents verbatim form): upstream
	 * the molds ride the MultiItemTechnological GT tab list; the port pools them with the
	 * machines tab (the GT6Molds pooling ruling).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tMold : MOLDS) {
				aEvent.accept(new ItemStack(tMold.get()));
			}
		}
	}
}
