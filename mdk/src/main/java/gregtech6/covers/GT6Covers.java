package gregtech6.covers;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.covers.covers.CoverConveyor;
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerCovers;
import gregtech6.covers.covers.CoverControllerRedstone;
import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverRedstoneConductorIN;
import gregtech6.covers.covers.CoverRedstoneConductorOUT;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.covers.covers.CoverRobotArm;
import gregtech6.covers.covers.CoverShutter;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.item.MaterialPrefixItem;
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
	 * The p11 ten conveyor timing tiers — one item per tier, upstream
	 * MultiItemTechnological.java:51 metas 12040+i ("Compact Electric Conveyor", each
	 * carrying a {@link CoverConveyor} with the {@code 512>>i} tick PERIOD). Registered
	 * through the same card-local ITEMS DeferredRegister as every own-item cover.
	 */
	public static final List<RegistryObject<Item>> COVER_CONVEYORS = new ArrayList<>();
	static {
		for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
			final int tTier = i;
			COVER_CONVEYORS.add(ITEMS.register("cover_conveyor_" + tTier, () -> new Item(new Item.Properties())));
		}
	}

	/**
	 * The p11 ten robot arm timing tiers — upstream MultiItemTechnological.java:53 metas
	 * 12080+i ("Compact Robot Arm", each carrying a {@link CoverRobotArm} with the same
	 * {@code 512>>i} table).
	 */
	public static final List<RegistryObject<Item>> COVER_ROBOT_ARMS = new ArrayList<>();
	static {
		for (int i = 0; i < CoverConveyor.TIMING_TIERS.length; i++) {
			final int tTier = i;
			COVER_ROBOT_ARMS.add(ITEMS.register("cover_robot_arm_" + tTier, () -> new Item(new Item.Properties())));
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

	/** Idempotent registration of the covers (the iron plate + the p5 pump + the p9 emitter + the p10 conductor pair + the p10 machine switch + the p11 shutter/filter pair + the p11 controller pair + the p11 conveyor/arm tiers). */
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
		CoverRegistry.put(COVER_AUTO_REDSTONE_MACHINE_SWITCH.get(), new CoverControllerAutoRedstone()); // p11 — the lets-it-finish machine switch
		CoverRegistry.put(COVER_CONTROLLER.get(), new CoverControllerCovers()); // p11 — the cover-layer stop switch + cross-face relay
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
