package gregtech6.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

import javax.annotation.Nullable;

/**
 * The F-2 Lubricant container item (task p29-w4-hot-lube spec ④) — the port counterpart of
 * the upstream crafting ingredient {@code OD.itemLubricant} (the Diesel Engine rows' 'L'
 * slot, Loader_MultiTileEntities.java:722-729 verbatim {@code 'L', OD.itemLubricant}; the
 * OD entry is the 1000 mB fluid-container re-registration face,
 * LoaderOreDictReRegistrations.java:978-979).
 *
 * <p><b>Carrier shape</b> (the architect ruling, decisions.p29-w4-split-rulings
 * conflicts[5]): the p14 single-item precedent — ONE registered item, plain {@link Item},
 * the p14 "Damage is free data" NBT axis left UNWRITTEN here because nothing consumes a
 * contents number yet. The DECLARED DEVIATION (the ruling's own wording): this is a
 * crafting-ingredient face ONLY — no {@code FLUID_HANDLER_ITEM} capability, so the bucket
 * neither fills from a fluid face nor drains; the empty-container return of the upstream
 * container craft (Recipe.getRemainingItems) is cut accordingly — the row CONSUMES the
 * bucket whole. When a fluid-container capability card lands, the Damage carrier is the
 * contents slot (GT6Circuits precedent) and the crafting row gains the remainder.
 *
 * <p>The creative-tab face is CUT (the GT6Machines tab walk is the card-③ exclusive file;
 * wiring the item into a tab there is a one-line follow-up, YAGNI until a player-facing
 * acquisition chain asks for it — crafting + give cover the live faces).
 *
 * <p>KJS surface (the card's declaration): REGISTRATION face (1 item) only; NO KubeJS
 * specific seam.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6LubricantBucket {

	/** The tooltip lang key (GT6EnUs pins the upstream FoodStatDrink "Industrial Use ONLY!" line, Loader_Fluids.java:617). */
	public static final String TOOLTIP_KEY = "item.gt6.lubricant_bucket.tooltip";

	/**
	 * The item registration, id {@code gt6:lubricant_bucket} (the architect ruling's naming
	 * candidate; the lowercase registry-path convention). Self-contained listener shape
	 * (ADR-P3-4, the GT6Circuits.java precedent).
	 */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	public static final RegistryObject<Item> LUBRICANT_BUCKET = ITEMS.register("lubricant_bucket",
			() -> new LubricantBucketItem(new Item.Properties()));

	/** The item body: the "Industrial Use ONLY!" tooltip (the upstream :617 drink description verbatim). */
	public static final class LubricantBucketItem extends Item {

		public LubricantBucketItem(Properties aProperties) {
			super(aProperties);
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			aTooltip.add(Component.translatable(TOOLTIP_KEY));
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			//21.1: the hover signature carries the Item.TooltipContext (vanilla 1.21.1 Item.java:468)
			//— the GT6Circuits fork shape.
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			aTooltip.add(Component.translatable(TOOLTIP_KEY));
		}
		*///?}
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Circuits.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	private GT6LubricantBucket() {}
}
