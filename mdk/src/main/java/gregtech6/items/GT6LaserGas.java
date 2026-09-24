package gregtech6.items;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The Gas Laser Emitter components (task p32-qu-laser-domain) — the two rows the CO2
 * laser chain needs, upstream MultiItemTechnological.java:384/:394:
 * <ul>
 * <li>{@code gt6:comp_laser_gas_empty} — the :384 row (id 11000, "Empty Gas Laser
 *     Emitter", tooltip "For Electric Lasers"), the Canner fill row's input item.</li>
 * <li>{@code gt6:comp_laser_gas_co2} — the :394 row (id 11008, "Carbon Dioxide Laser
 *     Emitter", tooltip "Purpose: Strong Material Processing"), the crafting component of
 *     every Electric CO2 Laser rung (the 'L' key, :930-934).</li>
 * </ul>
 * The other six gases (He/Ne/Ar/Kr/Xe/HeNe/CO, :387-393) stay out — no consumer rows in
 * this port (the fill-row scope is the CO2 line the task card names).
 *
 * <p>The creative-tab face joins MACHINES_TAB (task p38-tabfix-b-energy,
 * {@link #onBuildTabContents} — supersedes the old CUT ruling; the GTBarrels:257 pooling
 * precedent, the upstream crafting rows ride the Technological items tab this port does
 * not split out). The upstream crafting row of the empty emitter (:385) is the crafting
 * pool. KJS surface: REGISTRATION face only, deferred to the KJS binding card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6LaserGas {

	/** The self-contained registration listener (the GT6UsbSticks shape, ADR-P3-4). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	/** The :384 row (id 11000) — "Empty Gas Laser Emitter". */
	public static final RegistryObject<Item> COMP_LASER_GAS_EMPTY = ITEMS.register("comp_laser_gas_empty",
			() -> new GT6LaserGasItem(new Item.Properties(), "item.gt6.comp_laser_gas_empty.tooltip"));
	/** The :394 row (id 11008) — "Carbon Dioxide Laser Emitter". */
	public static final RegistryObject<Item> COMP_LASER_GAS_CO2 = ITEMS.register("comp_laser_gas_co2",
			() -> new GT6LaserGasItem(new Item.Properties(), "item.gt6.comp_laser_gas_co2.tooltip"));

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6UsbSticks shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — both emitter items join the machines tab;
	 * the GT6BurningBoxes.onBuildTabContents verbatim form, the class-level MOD-bus
	 * {@code @Mod.EventBusSubscriber} at the class head is what delivers this handler).
	 * JEI 1.20.1 derives its item list from the tab display items, so registered-but-
	 * tab-less was invisible in both the creative menu and JEI. Pool-cut declaration:
	 * upstream hangs the rows on the Technological items tab (MultiItemTechnological
	 * .java:384/:394) which this port does not split out; the join pools into MACHINES_TAB
	 * (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(gregtech6.registry.GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(COMP_LASER_GAS_EMPTY.get()));
			aEvent.accept(new ItemStack(COMP_LASER_GAS_CO2.get()));
		}
	}

	private GT6LaserGas() {}

	/** The emitter item: the static purpose line (the :384/:394 tooltip column). */
	public static final class GT6LaserGasItem extends Item {

		/** The tooltip key of this emitter (the upstream description column, lang-carried). */
		public final String mTooltipKey;

		public GT6LaserGasItem(Properties aProperties, String aTooltipKey) {
			super(aProperties);
			mTooltipKey = aTooltipKey;
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			aTooltip.add(Component.translatable(mTooltipKey));
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			//21.1: the hover signature carries the Item.TooltipContext (the GT6UsbStickItem fork shape)
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			aTooltip.add(Component.translatable(mTooltipKey));
		}
		*///?}
	}
}
