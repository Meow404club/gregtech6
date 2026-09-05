package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.items.tools.GTChiselItem;
import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.items.tools.GTCutterItem;

/**
 * The GT6 tool registration home — task p9-tool-crowbar spec ③, the ADR
 * 2026-09-01-p9-tool-crowbar ① surface. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GTBarrels precedent, ADR 2026-08-31-p8-prefixblocks): GT6Mod.java /
 * GTModBusListener.java stay untouched.
 *
 * <p>Task p10-tool-creative-tab adds the self-owned "Tools" creative tab
 * ({@link #TOOLS_TAB}, id {@code gt6:tools}) over a table-driven
 * {@code displayItems} ({@link #TAB_TABLE}, the GTWires.ELECTRIC_WIRES_TAB form, the
 * W1 ADR ①c) — the p9 ADR cut ("the item is {@code /give} reachable") closes here.
 *
 * <p>Declared pool cuts (ADR 2026-09-01-p10-tools-covers-split, zero code in this card):
 * <ol>
 * <li><b>Crowbar crafting recipe</b> — upstream shapes {@code {"hVS","VSV","SVf"}} per
 *     material, requiring the {@code h} hammer + {@code f} file TOOL PIECES plus a blue
 *     dye auxiliary (Loader_Tools.java:314, the OreProcessing_Tool row over
 *     toolHeadWrench); the port has no tool-piece item family and inventing vanilla
 *     substitutes is not done — unlocks with the tool-family card.</li>
 * <li><b>Crowbar material ladder</b> — upstream registers ONE meta id with an NBT-chosen
 *     material, per-material recipes (the whole ToolsGT block Loader_Tools.java:114-145;
 *     the crowbar row :128 carries {@code setMaterialAmount(3*U2)} = per-material
 *     durability); the port keeps the single steel tier at durability 512 (the pinned
 *     ADR value).</li>
 * <li><b>Crowbar runtime tint</b> — the material RGBa recolouring, coupled to the ladder:
 *     upstream {@code getRGBa} returns the primary material {@code mRGBaSolid}
 *     (GT_Tool_Crowbar.java:146-149, gregtech/items/tools/machine/); the port renders
 *     the single steel texture untinted until the ladder lands.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Tools {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/**
	 * The formal crowbar — item id {@code gt6:crowbar}. Single steel tier, durability
	 * 512 (the declared ADR value; upstream scales per material, Loader_Tools:128 —
	 * the ladder is a pool cut). Durability semantics: one vanilla point per 10000
	 * upstream tool-damage units, so one cover dismantle = one point.
	 */
	public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
			() -> new GTCrowbarItem(new Item.Properties().durability(GTCrowbarItem.DURABILITY_POINTS)));

	/**
	 * The formal wire cutter — item id {@code gt6:cutter} (task p10-tool-cutter spec ③).
	 * Single steel tier, durability 512 (upstream {@code 4*U} material-scaled,
	 * Loader_Tools.java:131 — the ladder is a pool cut, the same ruling as the crowbar).
	 * Upstream display name "Wire Cutter" (the same :131 registration row); no attack
	 * attributes — the cutter is not a weapon (GT_Tool_WireCutter :53-65 cut).
	 */
	public static final RegistryObject<Item> CUTTER = ITEMS.register("cutter",
			() -> new GTCutterItem(new Item.Properties().durability(GTCutterItem.DURABILITY_POINTS)));

	/**
	 * The formal chisel — item id {@code gt6:chisel} (task p16-chisel-decalcify spec ①).
	 * Single steel tier, durability 512 (the crowbar/cutter pinned family value; upstream
	 * scales per material, Loader_Tools.java:142 — the ladder is the same pool cut).
	 * Upstream display name "Chisel" (the same :142 registration row); the decalcify
	 * durability mapping is the upstream behaviour's mDamage=25 conversion
	 * (GT_Tool_Chisel.java:98, see {@link GTChiselItem#durabilityPoints}).
	 */
	public static final RegistryObject<Item> CHISEL = ITEMS.register("chisel",
			() -> new GTChiselItem(new Item.Properties().durability(GTChiselItem.DURABILITY_POINTS)));

	/**
	 * The "Tools" tab display table — one row per registered tool item, in display order.
	 * Table-driven so the tool-family cards append ONE row each. Pure data:
	 * {@link RegistryObject#getId()} reads the pre-registration name field
	 * (RegistryObject.java:287) and nothing here resolves {@code get()} — the offline test
	 * asserts the table shape and the ITEMS parity without touching the frozen registry;
	 * the displayItems generator below does the runtime resolution (the
	 * GTWires.ELECTRIC_WIRES_TAB form).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = List.of(CROWBAR, CUTTER, CHISEL);

	/**
	 * The tab title lang key — the single source both the builder and the GT6EnUs datagen
	 * row use, so the two faces cannot drift (the offline test pins the literal).
	 */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.tools";

	/**
	 * The "Tools" category tab — id {@code gt6:tools}, title key {@link #TAB_TITLE_KEY},
	 * icon the crowbar, entries the {@link #TAB_TABLE} rows (the crowbar then the cutter;
	 * the upstream analogue is the ToolsGT meta-tool block living in its own creative
	 * category, the registration rows Loader_Tools.java:114-145).
	 */
	public static final RegistryObject<CreativeModeTab> TOOLS_TAB = CREATIVE_MODE_TABS.register("tools",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(CROWBAR.get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : TAB_TABLE) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6Tools() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBarrels.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GTMachines fork precedent.
		*///?}
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}

	/** Registration smoke evidence (the GTFluids/GTBarrels onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CROWBAR.get()), GTCrowbarItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CUTTER.get()), GTCutterItem.DURABILITY_POINTS);
			GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
					ForgeRegistries.ITEMS.getKey(GT6Tools.CHISEL.get()), GTChiselItem.DURABILITY_POINTS);
			// The registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(TOOLS_TAB.get()), TAB_TABLE.size());
		});
	}
}
