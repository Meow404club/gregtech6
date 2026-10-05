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
 * The GT6 electrode registration home — task press-electrodes, the Forestry electrode
 * thirteen of {@code MultiItemTechnological.java:488-500} (metas 29987-29999): Copper,
 * Tin, Bronze, Iron, Gold, Diamond, Obsidian, Blaze, Rubber, Emerald, Apatite, Lapis,
 * Ender. They are the output faces of the RM.Press electrode band (MIT :502-543, poured
 * as the press.json electrode rows). Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event (the
 * GT6Explosives shape verbatim; GT6Mod.java stays untouched).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemTechnological meta item; the port flattens to one id per electrode, snake of
 * the IL field name (IL.java:472-484 — {@code Electrode_FR_Copper} →
 * {@code electrode_fr_copper}). The electrodes are PLAIN items — the upstream item carries
 * only an OreDictItemData composition face, so a fresh {@code Item} carries the whole
 * declared behaviour (the GT6PressMolds plain-item ruling).
 *
 * <p><b>Declared deviation — the creative face</b>: upstream the thirteen ride
 * {@code MD.FR.mLoaded ? null : TD.Creative.HIDDEN} (Forestry-gated hiding; the port has
 * no Forestry and no TD.Creative.HIDDEN seam for plain DeferredRegister items). The port
 * pools them with the machines tab (the GT6Explosives/GT6PressMolds pooling ruling) —
 * hidden-but-craftable would blind the JEI/EMI face of the 89 press rows that consume
 * them. The sprites are the upstream {@code gt.multiitem.technological} 29987-29999 PNGs
 * borrowed byte-identical (the assets/README.md sha256 ledger), pre-coloured — no tint
 * seam (the plain-item form).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Electrodes {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One registration row — the MultiItemTechnological :488-500 projection (upstream meta = 29987 + the row index). */
	public record ElectrodeRow(String path, RegistryObject<Item> item) {}

	/** The thirteen rows in upstream meta order (:488 Copper .. :500 Ender). */
	public static final List<ElectrodeRow> ROWS = List.of(
			register("electrode_fr_copper"),
			register("electrode_fr_tin"),
			register("electrode_fr_bronze"),
			register("electrode_fr_iron"),
			register("electrode_fr_gold"),
			register("electrode_fr_diamond"),
			register("electrode_fr_obsidian"),
			register("electrode_fr_blaze"),
			register("electrode_fr_rubber"),
			register("electrode_fr_emerald"),
			register("electrode_fr_apatite"),
			register("electrode_fr_lapis"),
			register("electrode_fr_ender"));

	private static ElectrodeRow register(String aPath) {
		return new ElectrodeRow(aPath, ITEMS.register(aPath, () -> new Item(new Item.Properties())));
	}

	private GT6Electrodes() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Explosives.onModConstruct shape). */
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
	 * The MACHINES_TAB join (the GT6Explosives.onBuildTabContents verbatim form): the
	 * declared deviation of the upstream Forestry-gated HIDDEN face (the class javadoc).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (ElectrodeRow tRow : ROWS) {
				aEvent.accept(new ItemStack(tRow.item().get()));
			}
		}
	}
}
