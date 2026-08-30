package gregtech6.covers;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.covers.covers.CoverPump;
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

	private static boolean sInitialized = false;

	private GT6Covers() {
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
		ITEMS.register(tModBus); // the RegisterEvent listener must be in place before registration runs
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6Covers::init);
	}

	/** Idempotent registration of the covers (the iron plate + the p5 pump). */
	public static void init() {
		if (sInitialized) return;
		sInitialized = true;
		Item tPlate = GTMaterialItems.get(OP.plate, MT.Iron).get();
		CoverRegistry.put(tPlate, new CoverTextureSimple(ironPlateSprite()));
		CoverRegistry.put(COVER_PUMP.get(), new CoverPump()); // p5 spec C — the pump mounts its own item
		LOGGER.info("GT6 covers registered: {} -> CoverTextureSimple({}), {} -> CoverPump",
				tPlate, ironPlateSprite(), COVER_PUMP.getId());
	}

	/**
	 * The plate sprite id, derived with the item-model datagen formula
	 * (GT6ItemModels.iconsetOf + MaterialPrefixItem.snakeCase) so the cover texture and
	 * the item texture can never drift apart.
	 */
	public static ResourceLocation ironPlateSprite() {
		return new ResourceLocation("gt6", "item/material_sets/" + GT6ItemModels.iconsetOf(MT.Iron) + "/" + MaterialPrefixItem.snakeCase(OP.plate.mNameInternal));
	}

	/** Test seam (P1 registry discipline). */
	static void resetForTest() {
		sInitialized = false;
	}
}
