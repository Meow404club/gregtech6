package gregtech6.covers;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.covers.covers.CoverConveyor;
import gregtech6.covers.covers.CoverAsphalt;
import gregtech6.covers.covers.CoverControllerAuto;
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerAutoTimer;
import gregtech6.covers.covers.CoverControllerCovers;
import gregtech6.covers.covers.CoverControllerDisplay;
import gregtech6.covers.covers.CoverControllerRedstone;
import gregtech6.covers.covers.CoverCrafting;
import gregtech6.covers.covers.CoverDisplayEnergy;
import gregtech6.covers.covers.CoverDrain;
import gregtech6.covers.covers.CoverFilterFluid;
import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverPressureValve;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverRedstoneConductorIN;
import gregtech6.covers.covers.CoverRedstoneConductorOUT;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.covers.covers.CoverRedstoneRepeater;
import gregtech6.covers.covers.CoverRedstoneTorch;
import gregtech6.covers.covers.CoverRetrieverItem;
import gregtech6.covers.covers.CoverRobotArm;
import gregtech6.covers.covers.CoverScaleEnergy;
import gregtech6.covers.covers.CoverScaleProgress;
import gregtech6.covers.covers.CoverSelectorButtonPanel;
import gregtech6.covers.covers.CoverSelectorManual;
import gregtech6.covers.covers.CoverSelectorRedstone;
import gregtech6.covers.covers.CoverSelectorTag;
import gregtech6.covers.covers.CoverShutter;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.covers.covers.CoverVent;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPUControl;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPUConversion;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPULogic;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPUStorage;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidExport;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidImport;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidStorage;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericDump;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericExport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericImport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericStorage;
import gregtech6.covers.covers.logistics.CoverLogisticsItemExport;
import gregtech6.covers.covers.logistics.CoverLogisticsItemImport;
import gregtech6.covers.covers.logistics.CoverLogisticsItemStorage;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Logistics;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMaterialItems;
import gregtech6.datagen.GT6ItemModels;
import gregapi.data.MT;
import gregapi.data.OP;

/**
 * The runtime cover registrations (task p4-cover-core ③ — zero new items; the first
 * cover mounts an EXISTING one). Item selection, per the card note:
 *
 * <p><b>gt6:plate_iron</b> — covers are literally material plates upstream
 * (Loader_OreProcessing.java:214 registers CoverTextureSimple with the material's own
 * texture; :88-97 the block-texture family), and the iron plate is the thinnest
 * material form this repo already ships (the P2/P3 material-prefix item with its
 * metallic iconset texture). The cover texture is therefore the plate item's own
 * sprite — {@code gt6:item/material_sets/metallic/plate} — derived with the same
 * formula the item-model datagen used (GT6ItemModels.iconsetOf).
 *
 * <p><b>gt6:cover_pump</b> (task p5-barrel-side-rules ruling ⑥) — the first cover that
 * owns its item: the pump has no plate-item analogue, so the card registers a dedicated
 * one through the card-local ITEMS DeferredRegister (the GTFluids four-DR shape,
 * construct-phase registration) and mounts {@link CoverPump} on it in {@link #init()}.
 *
 * <p><b>gt6:cover_redstone_emitter</b> (task p9-redstone-cover-emitter) — the first real
 * redstone cover, same own-item route as the pump; mounts {@link CoverRedstoneEmitter}
 * in {@link #init()}.
 *
	 * <p><b>gt6:cover_conveyor_0..9 / gt6:cover_robot_arm_0..9</b> (task
	 * p11-cover-conveyor-robotarm) — the ten timing tiers of the two item-transport covers
	 * (upstream MultiItemTechnological.java:51/:53 metas 12040+i / 12080+i, one item per
	 * {@code 512>>i} tick PERIOD), mounting {@link CoverConveyor} / {@link CoverRobotArm}
	 * in {@link #init()}.
	 *
	 * <p>Lifecycle: {@link #init()} is idempotent and runs from FMLCommonSetup (after item
 * registration, before any world interaction). Offline tests never call it — they
 * register their own vanilla-item covers, because {@code RegistryObject.get()} is
 * unbound outside the mod lifecycle.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Covers {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The p5 pump-cover item register (ruling ⑥) — construct-phase, like the GTFluids DRs. */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	public static final RegistryObject<Item> COVER_PUMP = ITEMS.register("cover_pump",
			() -> new Item(new Item.Properties()));

	/**
	 * The p9 redstone-emitter cover item — the second cover that owns its item (the
	 * emitter's 16-zone keypad plate has no plate-item analogue either). Registered
	 * through the same card-local ITEMS DeferredRegister as the pump.
	 */
	public static final RegistryObject<Item> COVER_REDSTONE_EMITTER = ITEMS.register("cover_redstone_emitter",
			() -> new Item(new Item.Properties()));

	/**
	 * The p10 redstone conductor pair — the accept marker and the emit face of the
	 * wire-through cover (task p10-cover-conductor-redstone; upstream
	 * MultiItemTechnological.java:88-89 metas 1029/1030). Same card-local ITEMS
	 * DeferredRegister as the pump and the emitter.
	 */
	public static final RegistryObject<Item> COVER_REDSTONE_CONDUCTOR_IN = ITEMS.register("cover_redstone_conductor_in",
			() -> new Item(new Item.Properties()));

	public static final RegistryObject<Item> COVER_REDSTONE_CONDUCTOR_OUT = ITEMS.register("cover_redstone_conductor_out",
			() -> new Item(new Item.Properties()));

	/**
	 * The p10 redstone machine switch cover — the controller that holds a switchable
	 * machine stopped/running by the redstone on its face (task
	 * p10-cover-controller-redstone; upstream MultiItemTechnological.java:64 meta 1005).
	 * Same card-local ITEMS DeferredRegister as the pump, the emitter and the conductor
	 * pair.
	 */
	public static final RegistryObject<Item> COVER_REDSTONE_MACHINE_SWITCH = ITEMS.register("cover_redstone_machine_switch",
			() -> new Item(new Item.Properties()));

	/**
	 * The p11 shutter cover item — the pure open/closed transfer gate on a face
	 * (task p11-cover-shutter-filter; upstream MultiItemTechnological.java:85 meta
	 * 1026 "Shutter Cover"). Same card-local ITEMS DeferredRegister as the pump,
	 * the emitter, the conductor pair and the machine switch.
	 */
	public static final RegistryObject<Item> COVER_SHUTTER = ITEMS.register("cover_shutter",
			() -> new Item(new Item.Properties()));

	/**
	 * The p11 item-filter cover item — the whitelist/blacklist face gate storing its
	 * filter item in the CoverData mNBTs lane (task p11-cover-shutter-filter; upstream
	 * MultiItemTechnological.java:82 meta 1023 "Item Filter", class CoverFilterItem).
	 * Same card-local ITEMS DeferredRegister as the rest of the cover family.
	 */
	public static final RegistryObject<Item> COVER_ITEM_FILTER = ITEMS.register("cover_item_filter",
			() -> new Item(new Item.Properties()));

	/**
	 * The p31 item-retriever cover item — the pipe-network puller storing its filter item
	 * in the CoverData mNBTs lane (task p31-retriever-cover; upstream
	 * MultiItemTechnological.java:90 meta 1031 "Item Retriever Cover", class
	 * CoverRetrieverItem). Mounts only on item pipes; pulls through the pipe network into
	 * the container at the covered face. Same card-local ITEMS DeferredRegister as the
	 * rest of the cover family.
	 */
	public static final RegistryObject<Item> COVER_ITEM_RETRIEVER = ITEMS.register("cover_item_retriever",
			() -> new Item(new Item.Properties()));

	/**
	 * The p33 logistics cover family — the 12 dead-endpoint covers (task
	 * p33-logistics-covers-12; upstream MultiItemTechnological.java:101-114 metas
	 * 1086-1099). The registration PLANE lives here (one item per cover); the BEHAVIOUR
	 * plane is the pure-Java cover classes + the Core's cover-bus registration arm. Same
	 * card-local ITEMS DeferredRegister as the rest of the cover family. Upstream id →
	 * item: 1086 cpu_logic, 1087 cpu_control, 1088 cpu_storage, 1089 cpu_conversion,
	 * 1090 fluid_export, 1091 fluid_import, 1092 fluid_storage, 1093 item_export,
	 * 1094 item_import, 1095 item_storage, 1096 generic_export, 1097 generic_import,
	 * 1098 generic_storage, 1099 generic_dump — 12 classes, generic_export/import/
	 * storage being the unfiltered pair-transport buses.
	 */
	public static final RegistryObject<Item> COVER_LOGISTICS_DISPLAY_CPU_LOGIC = ITEMS.register("cover_logistics_display_cpu_logic",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_DISPLAY_CPU_CONTROL = ITEMS.register("cover_logistics_display_cpu_control",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_DISPLAY_CPU_STORAGE = ITEMS.register("cover_logistics_display_cpu_storage",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_DISPLAY_CPU_CONVERSION = ITEMS.register("cover_logistics_display_cpu_conversion",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_FLUID_EXPORT = ITEMS.register("cover_logistics_fluid_export",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_FLUID_IMPORT = ITEMS.register("cover_logistics_fluid_import",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_FLUID_STORAGE = ITEMS.register("cover_logistics_fluid_storage",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_ITEM_EXPORT = ITEMS.register("cover_logistics_item_export",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_ITEM_IMPORT = ITEMS.register("cover_logistics_item_import",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_ITEM_STORAGE = ITEMS.register("cover_logistics_item_storage",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_GENERIC_EXPORT = ITEMS.register("cover_logistics_generic_export",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_GENERIC_IMPORT = ITEMS.register("cover_logistics_generic_import",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_GENERIC_STORAGE = ITEMS.register("cover_logistics_generic_storage",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_LOGISTICS_GENERIC_DUMP = ITEMS.register("cover_logistics_generic_dump",
			() -> new Item(new Item.Properties()));

	/**
	 * The p34 gameplay cover family (task p34-covers-gameplay-10; upstream
	 * MultiItemTechnological.java metas 1007/1008/1020/1022/1024/1027/2000) — the same
	 * card-local ITEMS DeferredRegister as the rest of the cover family. The tag
	 * selector ladder rides the 16-item static loop below (upstream ONE
	 * CoverSelectorTag(i) per integrated-circuit meta, ItemIntegratedCircuit.java:87;
	 * 1.20.1 items carry no meta axis, the p11 conveyor one-item-per-constant form).
	 * The torch/repeater pair is its own item family (upstream they rode the vanilla
	 * redstone torch/repeater items, GT_API.java:799-802 — the own-item form per the
	 * card ruling).
	 */
	public static final RegistryObject<Item> COVER_VENT = ITEMS.register("cover_vent",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_DRAIN = ITEMS.register("cover_drain",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_PRESSURE_VALVE = ITEMS.register("cover_pressure_valve",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_FLUID_FILTER = ITEMS.register("cover_fluid_filter",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_REDSTONE_TORCH = ITEMS.register("cover_redstone_torch",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_REDSTONE_REPEATER = ITEMS.register("cover_redstone_repeater",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_SELECTOR_REDSTONE = ITEMS.register("cover_selector_redstone",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_SELECTOR_MANUAL = ITEMS.register("cover_selector_manual",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_SELECTOR_BUTTON_PANEL = ITEMS.register("cover_selector_button_panel",
			() -> new Item(new Item.Properties()));

	/**
	 * The p35 display/scale cover family (task p35-covers-display-scale-6; upstream
	 * MultiItemTechnological.java:61/:63/:73/:77 metas 1002/1004/1014/1018) — the five
	 * singletons of the display/scale face: the machine status display, the energy
	 * display, the energy sensor and the progress sensor. The auto switch (meta 1003)
	 * and the reboot-switch ladder (:68-72 metas 1009-1013) ride the same family below.
	 * Same card-local ITEMS DeferredRegister as the rest of the cover family.
	 */
	public static final RegistryObject<Item> COVER_MACHINE_DISPLAY = ITEMS.register("cover_machine_display",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_AUTO_SWITCH = ITEMS.register("cover_auto_switch",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_ENERGY_DISPLAY = ITEMS.register("cover_energy_display",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_SCALE_ENERGY = ITEMS.register("cover_scale_energy",
			() -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COVER_SCALE_PROGRESS = ITEMS.register("cover_scale_progress",
			() -> new Item(new Item.Properties()));

	/**
	 * The p37 crafting-table cover item — the vanilla-workbench face (task
	 * p37-covers-crafting-asphalt; upstream MultiItemTechnological.java:60 meta 1001,
	 * "Crafting Table Cover"). Same card-local ITEMS DeferredRegister as the rest of the
	 * cover family.
	 */
	public static final RegistryObject<Item> COVER_CRAFTING = ITEMS.register("cover_crafting",
			() -> new Item(new Item.Properties()));

	/**
	 * The p37 asphalt cover item — the walk-speed plate (task p37-covers-crafting-asphalt;
	 * upstream the Asphalt Panel items Loader_MultiTileEntities.java:2053-2055 carried the
	 * CoverAsphalt — the dedicated cover-item form per the family convention, the zh face
	 * 沥青覆盖板 rides the panel row verbatim).
	 */
	public static final RegistryObject<Item> COVER_ASPHALT = ITEMS.register("cover_asphalt",
			() -> new Item(new Item.Properties()));

	/**
	 * The p11 auto redstone machine switch — the "lets it finish" controller (task
	 * p11-cover-controllers; upstream MultiItemTechnological.java:65 meta 1006,
	 * "Auto Redstone Machine Switch"). Holds a mid-process machine ON through a
	 * signal drop until the current process produces. Same card-local ITEMS
	 * DeferredRegister as the P10 switch.
	 */
	public static final RegistryObject<Item> COVER_AUTO_REDSTONE_MACHINE_SWITCH = ITEMS.register("cover_auto_redstone_machine_switch",
			() -> new Item(new Item.Properties()));

	/**
	 * The p11 cover controller — the cover-layer stop switch (task
	 * p11-cover-controllers; upstream MultiItemTechnological.java:84 meta 1025,
	 * "Cover Controller"). Drives {@code CoverData.setStopped} for the OTHER covers
	 * on the block and relays clicks/tool clicks across faces. Same card-local
	 * ITEMS DeferredRegister.
	 */
	public static final RegistryObject<Item> COVER_CONTROLLER = ITEMS.register("cover_controller",
			() -> new Item(new Item.Properties()));

	/**
	 * The conveyor display template (task p20-i18n-compose-wires): "{@code Compact Electric
	 * Conveyor (%s)}" — the former per-tier full-string keys became ONE position-param
	 * template, the tier slot filled at getName time (zh rides the dump's own template
	 * value, dump gt.multiitem.technological.12040 = "输送机模块 (ULV)").
	 */
	public static final String CONVEYOR_DISPLAY_KEY = "gt6.cover.conveyor.display";

	/** The robot-arm display template (same card): "{@code Compact Robot Arm (%s)}" (dump :12080 = "机械臂 (ULV)"). */
	public static final String ROBOT_ARM_DISPLAY_KEY = "gt6.cover.robot_arm.display";

	/**
	 * The tier names of both ladders — the CS.java:154 voltage numerals ULV..PUV1 (the
	 * former GT6EnUs.addCovers literal array). Voltage-level proper nouns: they stay the
	 * en literals in BOTH locales (the arch ruling), so a plain literal arg — no small-unit
	 * keys, zero translation ceremony.
	 */
	public static final String[] TIER_NAMES = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1"};

	/**
	 * The p34 tag-selector display template (task p34-covers-gameplay-10):
	 * "{@code Tag Selector (%s)}" — the 16 ladder items compose the mode numeral
	 * (the conveyor template form; the upstream tag selector IS the integrated-circuit
	 * item, whose dump face 选择器标签 names the family).
	 */
	public static final String SELECTOR_TAG_DISPLAY_KEY = "gt6.cover.selector_tag.display";

	/**
	 * The p11 ten conveyor timing tiers — one item per tier, upstream
	 * MultiItemTechnological.java:51 metas 12040+i ("Compact Electric Conveyor", each
	 * carrying a {@link CoverConveyor} with the {@code 512>>i} tick PERIOD). Registered
	 * through the same card-local ITEMS DeferredRegister as every own-item cover. Task
	 * p20-i18n-compose-wires: the display name composes the {@link #CONVEYOR_DISPLAY_KEY}
	 * template with the tier literal instead of resolving a per-tier lang key.
	 */
	public static final List<RegistryObject<Item>> COVER_CONVEYORS = new ArrayList<>();
	static {
		for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
			final int tTier = i;
			COVER_CONVEYORS.add(ITEMS.register("cover_conveyor_" + tTier, () -> new Item(new Item.Properties()) {
				@Override
				public Component getName(ItemStack aStack) {
					return Component.translatable(CONVEYOR_DISPLAY_KEY, TIER_NAMES[tTier]);
				}
			}));
		}
	}

	/**
	 * The p11 ten robot arm timing tiers — upstream MultiItemTechnological.java:53 metas
	 * 12080+i ("Compact Robot Arm", each carrying a {@link CoverRobotArm} with the same
	 * {@code 512>>i} table). Same composed display face as the conveyors
	 * (task p20-i18n-compose-wires).
	 */
	public static final List<RegistryObject<Item>> COVER_ROBOT_ARMS = new ArrayList<>();
	static {
		for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
			final int tTier = i;
			COVER_ROBOT_ARMS.add(ITEMS.register("cover_robot_arm_" + tTier, () -> new Item(new Item.Properties()) {
				@Override
				public Component getName(ItemStack aStack) {
					return Component.translatable(ROBOT_ARM_DISPLAY_KEY, TIER_NAMES[tTier]);
				}
			}));
		}
	}

	/**
	 * The p34 16 tag-selector ladder items — one item per selector mode 0..15, upstream
	 * the integrated-circuit meta ladder (ItemIntegratedCircuit.java:87). Each item
	 * composes the {@link #SELECTOR_TAG_DISPLAY_KEY} template with the hex mode numeral
	 * (the conveyor composed-display form).
	 */
	public static final List<RegistryObject<Item>> COVER_SELECTOR_TAGS = new ArrayList<>();
	static {
		for (int i = 0; i < 16; i++) {
			final int tMode = i;
			COVER_SELECTOR_TAGS.add(ITEMS.register("cover_selector_tag_" + tMode, () -> new Item(new Item.Properties()) {
				@Override
				public Component getName(ItemStack aStack) {
					return Component.translatable(SELECTOR_TAG_DISPLAY_KEY, Integer.toHexString(tMode).toUpperCase());
				}
			}));
		}
	}

	/**
	 * The p35 five auto reboot switch ladder items — one item per duration, upstream
	 * MultiItemTechnological.java:68-72 metas 1009-1013 ("Auto Reboot Switch (1 min)" ..
	 * "(30 mins)", each carrying a {@link CoverControllerAutoTimer} with the
	 * 1200..36000 tick cycle). The duration word rides the per-item lang key (the zh
	 * faces are not literal-arg composable across locales — the conveyor template form
	 * needs locale-neutral args).
	 */
	public static final String[] AUTO_TIMER_IDS = {"cover_auto_timer_1m", "cover_auto_timer_5m", "cover_auto_timer_10m", "cover_auto_timer_20m", "cover_auto_timer_30m"};

	/** The p35 auto reboot switch ladder — the five duration items, in {@link #AUTO_TIMER_IDS} order. */
	public static final List<RegistryObject<Item>> COVER_AUTO_TIMERS = new ArrayList<>();
	static {
		for (String tId : AUTO_TIMER_IDS) {
			COVER_AUTO_TIMERS.add(ITEMS.register(tId, () -> new Item(new Item.Properties())));
		}
	}

	private static boolean sInitialized = false;

	private GT6Covers() {
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: the FQ Mod.EventBusSubscriber.Bus form is gone with the annotation rework; a
		//self-contained listener reaches the mod bus through its mod container (javap
		//loader-4.0.44: ModContainer.getEventBus public abstract) — the GT6Mod/GTMenuTypes
		//fork precedent.
		*///?}
		ITEMS.register(tModBus); // the RegisterEvent listener must be in place before registration runs
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6Covers::init);
	}

	/**
	 * The tab walk (task p33-logistics-covers-12 — the lv2/lv3 遗留账: the logistics
	 * wire item, the core item and the 14 logistics covers join the machines tab; the
	 * GT6Placeables.onBuildTabContents verbatim form). The EARLIER cover family stays
	 * tab-less — its items are cover-placed tool faces, the upstream tab walk never
	 * listed them (MultiItemTechnological items ride the GT tab list upstream, the port
	 * keeps the narrower declared scope of the landed cards).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(GT6Logistics.LOGISTICS_WIRE_ITEM.get()));
			aEvent.accept(new ItemStack(GT6Logistics.LOGISTICS_CORE_ITEM.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_DISPLAY_CPU_LOGIC.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_DISPLAY_CPU_CONTROL.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_DISPLAY_CPU_STORAGE.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_DISPLAY_CPU_CONVERSION.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_FLUID_EXPORT.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_FLUID_IMPORT.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_FLUID_STORAGE.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_ITEM_EXPORT.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_ITEM_IMPORT.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_ITEM_STORAGE.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_GENERIC_EXPORT.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_GENERIC_IMPORT.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_GENERIC_STORAGE.get()));
			aEvent.accept(new ItemStack(COVER_LOGISTICS_GENERIC_DUMP.get()));
			// task p34-covers-gameplay-10 — the gameplay cover family joins the machines
			// tab (the logistics-family precedent; upstream the MultiItemTechnological items
			// ride the GT tab list)
			aEvent.accept(new ItemStack(COVER_VENT.get()));
			aEvent.accept(new ItemStack(COVER_DRAIN.get()));
			aEvent.accept(new ItemStack(COVER_PRESSURE_VALVE.get()));
			aEvent.accept(new ItemStack(COVER_FLUID_FILTER.get()));
			aEvent.accept(new ItemStack(COVER_REDSTONE_TORCH.get()));
			aEvent.accept(new ItemStack(COVER_REDSTONE_REPEATER.get()));
			for (int i = 0; i < 16; i++) aEvent.accept(new ItemStack(COVER_SELECTOR_TAGS.get(i).get()));
			aEvent.accept(new ItemStack(COVER_SELECTOR_REDSTONE.get()));
			aEvent.accept(new ItemStack(COVER_SELECTOR_MANUAL.get()));
			aEvent.accept(new ItemStack(COVER_SELECTOR_BUTTON_PANEL.get()));
			// task p35-covers-display-scale-6 — the display/scale family joins the machines
			// tab (the p34 gameplay-family precedent; upstream the MultiItemTechnological
			// items ride the GT tab list)
			aEvent.accept(new ItemStack(COVER_MACHINE_DISPLAY.get()));
			aEvent.accept(new ItemStack(COVER_AUTO_SWITCH.get()));
			aEvent.accept(new ItemStack(COVER_ENERGY_DISPLAY.get()));
			aEvent.accept(new ItemStack(COVER_SCALE_ENERGY.get()));
			aEvent.accept(new ItemStack(COVER_SCALE_PROGRESS.get()));
			for (int i = 0; i < COVER_AUTO_TIMERS.size(); i++) aEvent.accept(new ItemStack(COVER_AUTO_TIMERS.get(i).get()));
			// task p37-covers-crafting-asphalt — the crafting + asphalt pair joins the
			// machines tab (the p34/p35 family precedents; upstream the crafting cover rides
			// the MultiItemTechnological GT tab list, the asphalt panel the Panels list)
			aEvent.accept(new ItemStack(COVER_CRAFTING.get()));
			aEvent.accept(new ItemStack(COVER_ASPHALT.get()));
		}
	}

	/** Idempotent registration of the covers (the iron plate + the p5 pump + the p9 emitter + the p10 conductor pair + the p10 machine switch + the p11 shutter/filter pair + the p11 controller pair + the p11 conveyor/arm tiers + the p31 retriever). */
	public static void init() {
		if (sInitialized) return;
		sInitialized = true;
		Item tPlate = GTMaterialItems.get(OP.plate, MT.Iron).get();
		CoverRegistry.put(tPlate, new CoverTextureSimple(ironPlateSprite()));
		CoverRegistry.put(COVER_PUMP.get(), new CoverPump()); // p5 spec C — the pump mounts its own item
		CoverRegistry.put(COVER_REDSTONE_EMITTER.get(), new CoverRedstoneEmitter()); // p9 — the first real redstone cover
		CoverRegistry.put(COVER_REDSTONE_CONDUCTOR_IN.get(), new CoverRedstoneConductorIN()); // p10 — the accept marker
		CoverRegistry.put(COVER_REDSTONE_CONDUCTOR_OUT.get(), new CoverRedstoneConductorOUT()); // p10 — the wire-through face
		CoverRegistry.put(COVER_REDSTONE_MACHINE_SWITCH.get(), new CoverControllerRedstone()); // p10 — the redstone on/off machine switch
		CoverRegistry.put(COVER_SHUTTER.get(), new CoverShutter()); // p11 — the open/closed face gate
		CoverRegistry.put(COVER_ITEM_FILTER.get(), new CoverFilterItem()); // p11 — the whitelist/blacklist face filter
		CoverRegistry.put(COVER_ITEM_RETRIEVER.get(), new CoverRetrieverItem()); // p31 — the pipe-network retriever
		// p33 — the logistics cover family (upstream MultiItemTechnological.java:101-114):
		// the 4 CPU displays + the filtered fluid/item trios + the generic trio + the dump
		CoverRegistry.put(COVER_LOGISTICS_DISPLAY_CPU_LOGIC.get(), CoverLogisticsDisplayCPULogic.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_DISPLAY_CPU_CONTROL.get(), CoverLogisticsDisplayCPUControl.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_DISPLAY_CPU_STORAGE.get(), CoverLogisticsDisplayCPUStorage.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_DISPLAY_CPU_CONVERSION.get(), CoverLogisticsDisplayCPUConversion.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_FLUID_EXPORT.get(), CoverLogisticsFluidExport.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_FLUID_IMPORT.get(), CoverLogisticsFluidImport.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_FLUID_STORAGE.get(), CoverLogisticsFluidStorage.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_ITEM_EXPORT.get(), CoverLogisticsItemExport.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_ITEM_IMPORT.get(), CoverLogisticsItemImport.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_ITEM_STORAGE.get(), CoverLogisticsItemStorage.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_GENERIC_EXPORT.get(), CoverLogisticsGenericExport.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_GENERIC_IMPORT.get(), CoverLogisticsGenericImport.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_GENERIC_STORAGE.get(), CoverLogisticsGenericStorage.INSTANCE);
		CoverRegistry.put(COVER_LOGISTICS_GENERIC_DUMP.get(), CoverLogisticsGenericDump.INSTANCE);
		CoverRegistry.put(COVER_AUTO_REDSTONE_MACHINE_SWITCH.get(), new CoverControllerAutoRedstone()); // p11 — the lets-it-finish machine switch
		CoverRegistry.put(COVER_CONTROLLER.get(), new CoverControllerCovers()); // p11 — the cover-layer stop switch + cross-face relay
		// p34 — the gameplay cover family (upstream MultiItemTechnological 1007/1008/1020/1022/1024/1027/2000
		// + the integrated-circuit tag ladder ItemIntegratedCircuit.java:87):
		// the fluid trio + the fluid filter + the torch pair + the four selectors
		CoverRegistry.put(COVER_VENT.get(), new CoverVent()); // p34 — the boiler air-intake face (the declared-minimal air seam)
		CoverRegistry.put(COVER_DRAIN.get(), new CoverDrain()); // p34 — the rain/water/lava collection face
		CoverRegistry.put(COVER_PRESSURE_VALVE.get(), new CoverPressureValve()); // p34 — the single-tank pipe safety valve
		CoverRegistry.put(COVER_FLUID_FILTER.get(), new CoverFilterFluid()); // p34 — the whitelist/blacklist fluid face filter
		CoverRegistry.put(COVER_REDSTONE_TORCH.get(), new CoverRedstoneTorch()); // p34 — the wire inverter face
		CoverRegistry.put(COVER_REDSTONE_REPEATER.get(), new CoverRedstoneRepeater()); // p34 — the wire follower face
		for (int i = 0; i < 16; i++) {
			// p34 — the 16 tag-selector modes (upstream one CoverSelectorTag(i) per circuit meta)
			CoverRegistry.put(COVER_SELECTOR_TAGS.get(i).get(), new CoverSelectorTag((byte) i));
		}
		CoverRegistry.put(COVER_SELECTOR_REDSTONE.get(), new CoverSelectorRedstone()); // p34 — the signal-driven dial
		CoverRegistry.put(COVER_SELECTOR_MANUAL.get(), new CoverSelectorManual()); // p34 — the arrow/bit plate GUI
		CoverRegistry.put(COVER_SELECTOR_BUTTON_PANEL.get(), new CoverSelectorButtonPanel()); // p34 — the 4x4 button grid
		// p35 — the display/scale family (upstream MultiItemTechnological :61/:63/:73/:77
		// + the :68-72 reboot-switch ladder): the status display + the auto switch + the
		// energy display + the two sensors + the five timer durations
		CoverRegistry.put(COVER_MACHINE_DISPLAY.get(), new CoverControllerDisplay()); // p35 — the status display + switch face
		CoverRegistry.put(COVER_AUTO_SWITCH.get(), new CoverControllerAuto()); // p35 — the runs-when-needed switch
		CoverRegistry.put(COVER_ENERGY_DISPLAY.get(), new CoverDisplayEnergy()); // p35 — the 11-step energy gauge
		CoverRegistry.put(COVER_SCALE_ENERGY.get(), new CoverScaleEnergy()); // p35 — the energy redstone sensor
		CoverRegistry.put(COVER_SCALE_PROGRESS.get(), new CoverScaleProgress()); // p35 — the progress redstone sensor
		for (int i = 0; i < COVER_AUTO_TIMERS.size(); i++) {
			// p35 — the five reboot durations (1200..36000 tick cycles)
			CoverRegistry.put(COVER_AUTO_TIMERS.get(i).get(), new CoverControllerAutoTimer(CoverControllerAutoTimer.TIMER_TIMES[i]));
		}
		// p37 — the last two gameplay classes: the vanilla-workbench face + the walk-speed plate
		CoverRegistry.put(COVER_CRAFTING.get(), new CoverCrafting());
		CoverRegistry.put(COVER_ASPHALT.get(), new CoverAsphalt());
		for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
			// p11 — the ten timing tiers of the two item-transport covers (512>>i tick periods)
			CoverRegistry.put(COVER_CONVEYORS.get(i).get(), new CoverConveyor(CoverConveyor.TIMING_TIERS[i]));
			CoverRegistry.put(COVER_ROBOT_ARMS.get(i).get(), new CoverRobotArm(CoverConveyor.TIMING_TIERS[i]));
		}
		LOGGER.info("GT6 covers registered: {} -> CoverTextureSimple({}), {} -> CoverPump, {} -> CoverRedstoneEmitter, {} -> CoverRedstoneConductorIN, {} -> CoverRedstoneConductorOUT, {} -> CoverControllerRedstone, {} -> CoverShutter, {} -> CoverFilterItem, {} -> CoverControllerAutoRedstone, {} -> CoverControllerCovers, {} conveyor tiers, {} robot arm tiers",
				tPlate, ironPlateSprite(), COVER_PUMP.getId(), COVER_REDSTONE_EMITTER.getId(), COVER_REDSTONE_CONDUCTOR_IN.getId(), COVER_REDSTONE_CONDUCTOR_OUT.getId(), COVER_REDSTONE_MACHINE_SWITCH.getId(), COVER_SHUTTER.getId(), COVER_ITEM_FILTER.getId(), COVER_AUTO_REDSTONE_MACHINE_SWITCH.getId(), COVER_CONTROLLER.getId(),
				COVER_CONVEYORS.size(), COVER_ROBOT_ARMS.size());
	}

	/**
	 * The plate sprite id, derived with the item-model datagen formula
	 * (GT6ItemModels.iconsetOf + MaterialPrefixItem.snakeCase) so the cover texture and
	 * the item texture can never drift apart.
	 */
	public static ResourceLocation ironPlateSprite() {
		//? if forge {
		return new ResourceLocation("gt6", "item/material_sets/" + GT6ItemModels.iconsetOf(MT.Iron) + "/" + MaterialPrefixItem.snakeCase(OP.plate.mNameInternal));
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "item/material_sets/" + GT6ItemModels.iconsetOf(MT.Iron) + "/" + MaterialPrefixItem.snakeCase(OP.plate.mNameInternal));
		//21.1 privatised the two-arg ResourceLocation constructor; the swap-table regex
		//(conservative no-parens argument form) cannot reach this row — the embedded
		//iconsetOf(MT.Iron) call — so it forks here (ADR-P15-3 r1 priority 3).
		*///?}
	}

	/** Test seam (P1 registry discipline). */
	static void resetForTest() {
		sInitialized = false;
	}
}
