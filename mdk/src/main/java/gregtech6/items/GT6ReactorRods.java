package gregtech6.items;

import java.util.List;
import java.util.Map;

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

import gregtech6.reactor.IReactorRodItem;
import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.FuelRodSpec;
import gregtech6.reactor.neutron.ReactorRodKind;

/**
 * The 46 reactor rod ITEMS (task debt-reactor-c-rods) — the C-card face the B card
 * declared, over the {@link IReactorRodItem} seam: the 4 functional rods + the 17 fuel
 * rods + the 17 depleted rods + the 4 breeder rods + the 4 enriched product rods of
 * Loader_MultiTileEntities.java:741-790. {@link #ROWS} is the verbatim transcription of
 * the upstream registration rows (id = the multi-tile meta, name = the display name
 * column); the NUMBERS live once, on the A-card {@link FuelRodSpec}/{@link BreederRodSpec}
 * tables, resolved by id — no second copy of the neutron parameters exists.
 *
 * <p>Behaviour rides the A-card {@link ReactorRodKind} enum and the B-card
 * {@link ReactorRodNbt} carrier: the mutable burn state (gt.durability / gt.nuclear.mod)
 * rides the stack, and the fresh-rod burn budget (upstream registration NBT
 * NBT_MAXDURABILITY, which the 1.7.10 item template bakes and the Canner output carries)
 * is written by the recipe pour through {@link gregtech6.reactor.ReactorRodNbt#setMaxDurability}.
 * The in-place depletion/breeding swap is the seam method {@link GT6ReactorRodItem#rodSwapTarget}
 * over {@link #BY_ID} (fuel 9210→9310 ..., breeder 9410→9411 ...).
 *
 * <p>Declared poolings: the 1x1 core and its config gate stay deferred (the B-card ruling,
 * upstream default OFF); the two core CRAFTING rows (LME:734/:738) stay pooled — their
 * 'P' (IL.PISTONS[4]) and 'M' (OP.casingMachineDense) legs have no port items/blocks yet
 * (the GT6CraftingRecipes.java:2867 absent-component ruling). KJS surface: registration
 * face only, deferred to the KJS binding card (the GT6Reactors declaration form).
 *
 * <p>Creative tab: the upstream "Reactors" category pools into MACHINES_TAB (the
 * GT6Reactors pooling precedent). Textures: every rod shares the two borrowed
 * reactor_rods byte borrows (assets/README.md) — upstream differentiates the rods ONLY
 * by the material tint (mRGBa, RodBase.java:69-70), which rides the render-pool card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ReactorRods {

	/** The lang-key prefix of the shared tooltip lines (the {@link #TOOLTIP_KEYS} walk). */
	public static final String TOOLTIP_PREFIX = "item.gt6.reactor_rod.";

	/**
	 * The shared tooltip lines, upstream addToolTips text verbatim minus the colour codes
	 * (RodBase:51-52, Absorber:35-36, Reflector:35-36, Moderator:35-38, Nuclear:57-60 +
	 * the case-0 water page, Depleted:33-35, Breeder:66-74, Product:35-38). The numeric
	 * lines carry %s args composed from the specs at hover time.
	 */
	public static final String[] TOOLTIP_KEYS = {
		"used_in_core",
		"empty",
		"absorber",
		"reflector",
		"moderator.1", "moderator.2", "moderator.3",
		"fuel.concept.1", "fuel.concept.2", "fuel.concept.3", "fuel.concept.4",
		"fuel.emission", "fuel.self", "fuel.maximum", "fuel.factor", "fuel.critical",
		"depleted.1", "depleted.2",
		"product.1", "product.2", "product.breeds",
		"breeder.1", "breeder.2", "breeder.3", "breeder.4", "breeder.5", "breeder.6",
		"breeder.into", "breeder.needed", "breeder.loss"};

	/** One registration row — the LME:741-790 line's item projection. */
	public record RodRow(int id, String path, String name, ReactorRodKind kind) {}

	/** Loader_MultiTileEntities.java:741-790 verbatim, registration order (the 46-item census table). */
	public static final List<RodRow> ROWS = List.of(
		// :741-744 — the functional four
		new RodRow(9201, "empty_reactor_rod",        "Empty Reactor Rod",        ReactorRodKind.EMPTY),
		new RodRow(9202, "neutron_absorber_rod",     "Neutron Absorber Rod",     ReactorRodKind.ABSORBER),
		new RodRow(9203, "neutron_reflector_rod",    "Neutron Reflector Rod",    ReactorRodKind.REFLECTOR),
		new RodRow(9204, "neutron_moderator_rod",    "Neutron Moderator Rod",    ReactorRodKind.MODERATOR),
		// :746-762 — the 17 fuel rods
		new RodRow(9210, "thorium_232_fuel_rod",         "Thorium-232 Fuel Rod",         ReactorRodKind.FUEL),
		new RodRow(9219, "cyanite_fuel_rod",             "Cyanite Fuel Rod",             ReactorRodKind.FUEL),
		new RodRow(9220, "uranium_238_fuel_rod",         "Uranium-238 Fuel Rod",         ReactorRodKind.FUEL),
		new RodRow(9221, "uranium_235_fuel_rod",         "Uranium-235 Fuel Rod",         ReactorRodKind.FUEL),
		new RodRow(9222, "uranium_233_fuel_rod",         "Uranium-233 Fuel Rod",         ReactorRodKind.FUEL),
		new RodRow(9229, "yellorium_fuel_rod",           "Yellorium Fuel Rod",           ReactorRodKind.FUEL),
		new RodRow(9230, "plutonium_244_fuel_rod",       "Plutonium-244 Fuel Rod",       ReactorRodKind.FUEL),
		new RodRow(9231, "plutonium_241_fuel_rod",       "Plutonium-241 Fuel Rod",       ReactorRodKind.FUEL),
		new RodRow(9232, "plutonium_243_fuel_rod",       "Plutonium-243 Fuel Rod",       ReactorRodKind.FUEL),
		new RodRow(9233, "plutonium_239_fuel_rod",       "Plutonium-239 Fuel Rod",       ReactorRodKind.FUEL),
		new RodRow(9239, "blutonium_fuel_rod",           "Blutonium Fuel Rod",           ReactorRodKind.FUEL),
		new RodRow(9240, "americium_245_fuel_rod",       "Americium-245 Fuel Rod",       ReactorRodKind.FUEL),
		new RodRow(9241, "americium_241_fuel_rod",       "Americium-241 Fuel Rod",       ReactorRodKind.FUEL),
		new RodRow(9249, "ludicrite_fuel_rod",           "Ludicrite Fuel Rod",           ReactorRodKind.FUEL),
		new RodRow(9250, "cobalt_60_fuel_rod",           "Cobalt-60 Fuel Rod",           ReactorRodKind.FUEL),
		new RodRow(9260, "enriched_naquadah_fuel_rod",   "Enriched Naquadah Fuel Rod",   ReactorRodKind.FUEL),
		new RodRow(9261, "naquadria_fuel_rod",           "Naquadria Fuel Rod",           ReactorRodKind.FUEL),
		// :764-780 — the 17 depleted rods (the fuel ids + 100)
		new RodRow(9310, "depleted_thorium_232_fuel_rod",       "Depleted Thorium-232 Fuel Rod",       ReactorRodKind.DEPLETED),
		new RodRow(9319, "depleted_cyanite_fuel_rod",           "Depleted Cyanite Fuel Rod",           ReactorRodKind.DEPLETED),
		new RodRow(9320, "depleted_uranium_238_fuel_rod",       "Depleted Uranium-238 Fuel Rod",       ReactorRodKind.DEPLETED),
		new RodRow(9321, "depleted_uranium_235_fuel_rod",       "Depleted Uranium-235 Fuel Rod",       ReactorRodKind.DEPLETED),
		new RodRow(9322, "depleted_uranium_233_fuel_rod",       "Depleted Uranium-233 Fuel Rod",       ReactorRodKind.DEPLETED),
		new RodRow(9329, "depleted_yellorium_fuel_rod",         "Depleted Yellorium Fuel Rod",         ReactorRodKind.DEPLETED),
		new RodRow(9330, "depleted_plutonium_244_fuel_rod",     "Depleted Plutonium-244 Fuel Rod",     ReactorRodKind.DEPLETED),
		new RodRow(9331, "depleted_plutonium_241_fuel_rod",     "Depleted Plutonium-241 Fuel Rod",     ReactorRodKind.DEPLETED),
		new RodRow(9332, "depleted_plutonium_243_fuel_rod",     "Depleted Plutonium-243 Fuel Rod",     ReactorRodKind.DEPLETED),
		new RodRow(9333, "depleted_plutonium_239_fuel_rod",     "Depleted Plutonium-239 Fuel Rod",     ReactorRodKind.DEPLETED),
		new RodRow(9339, "depleted_blutonium_fuel_rod",         "Depleted Blutonium Fuel Rod",         ReactorRodKind.DEPLETED),
		new RodRow(9340, "depleted_americium_245_fuel_rod",     "Depleted Americium-245 Fuel Rod",     ReactorRodKind.DEPLETED),
		new RodRow(9341, "depleted_americium_241_fuel_rod",     "Depleted Americium-241 Fuel Rod",     ReactorRodKind.DEPLETED),
		new RodRow(9349, "depleted_ludicrite_fuel_rod",         "Depleted Ludicrite Fuel Rod",         ReactorRodKind.DEPLETED),
		new RodRow(9350, "depleted_cobalt_60_fuel_rod",         "Depleted Cobalt-60 Fuel Rod",         ReactorRodKind.DEPLETED),
		new RodRow(9360, "depleted_enriched_naquadah_fuel_rod", "Depleted Enriched Naquadah Fuel Rod", ReactorRodKind.DEPLETED),
		new RodRow(9361, "depleted_naquadria_fuel_rod",         "Depleted Naquadria Fuel Rod",         ReactorRodKind.DEPLETED),
		// :782-785 — the 4 breeder rods
		new RodRow(9410, "thorium_232_breeder_rod", "Thorium-232 Breeder Rod", ReactorRodKind.BREEDER),
		new RodRow(9420, "uranium_238_breeder_rod", "Uranium-238 Breeder Rod", ReactorRodKind.BREEDER),
		new RodRow(9430, "lithium_breeder_rod",     "Lithium Breeder Rod",     ReactorRodKind.BREEDER),
		new RodRow(9440, "naquadah_breeder_rod",    "Naquadah Breeder Rod",    ReactorRodKind.BREEDER),
		// :787-790 — the 4 enriched product rods
		new RodRow(9411, "uranium_233_enriched_rod",         "Uranium-233 Enriched Rod",           ReactorRodKind.PRODUCT),
		new RodRow(9421, "plutonium_239_enriched_rod",       "Plutonium-239 Enriched Rod",         ReactorRodKind.PRODUCT),
		new RodRow(9431, "tritium_enriched_rod",             "Tritium Enriched Rod",               ReactorRodKind.PRODUCT),
		new RodRow(9441, "enriched_naquadah_enriched_rod",   "Enriched Naquadah Enriched Rod",     ReactorRodKind.PRODUCT));

	/** The self-contained registration container (the GT6LaserGas shape, ADR-P3-4). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	/** Legacy meta id → the registered item (the rodSwapTarget dispatch + the recipe legs). */
	public static final Map<Integer, RegistryObject<Item>> BY_ID = new java.util.LinkedHashMap<>();
	static {
		for (RodRow tRow : ROWS) BY_ID.put(tRow.id(), ITEMS.register(tRow.path(), () -> new GT6ReactorRodItem(tRow)));
	}

	/** Row lookup by legacy meta id. */
	public static java.util.Optional<RodRow> rowOf(int aId) {
		return ROWS.stream().filter(r -> r.id() == aId).findFirst();
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6LaserGas shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Attachments fork).
		 *///?}
		ITEMS.register(tModBus);
	}

	/** The tab walk (the GT6Reactors "Reactors"→MACHINES_TAB pooling precedent). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(gregtech6.registry.GTMachines.MACHINES_TAB.getId())) {
			for (RodRow tRow : ROWS) aEvent.accept(new ItemStack(BY_ID.get(tRow.id()).get()));
		}
	}

	private GT6ReactorRods() {}

	/**
	 * The rod item: the behavioural face over the immutable row. The spec tables are
	 * resolved BY ROW ID (the A-card tables are the single source of the numbers); the
	 * burn state lives on the stack ({@link gregtech6.reactor.ReactorRodNbt}).
	 */
	public static final class GT6ReactorRodItem extends Item implements IReactorRodItem {

		private final RodRow mRow;

		public GT6ReactorRodItem(RodRow aRow) {
			super(new Item.Properties());
			mRow = aRow;
		}

		/** The upstream row (id/path/name/kind) — the census and the recipes read this. */
		public RodRow row() {
			return mRow;
		}

		@Override
		public ReactorRodKind rodKind(ItemStack aStack) {
			return mRow.kind();
		}

		@Override
		@Nullable
		public FuelRodSpec fuelSpec(ItemStack aStack) {
			return mRow.kind() == ReactorRodKind.FUEL ? FuelRodSpec.byId(mRow.id()).orElse(null) : null;
		}

		@Override
		@Nullable
		public BreederRodSpec breederSpec(ItemStack aStack) {
			return mRow.kind() == ReactorRodKind.BREEDER ? BreederRodSpec.byId(mRow.id()).orElse(null) : null;
		}

		/**
		 * The in-place swap target: the depleted rod for a fuel id (Nuclear.java:221), the
		 * enriched product for a breeder id (Breeder.java:86). Null = unknown target (the
		 * fixture seam semantics of {@link IReactorRodItem#rodSwapTarget}).
		 */
		@Override
		@Nullable
		public ItemStack rodSwapTarget(int aTargetId) {
			RegistryObject<Item> tTarget = BY_ID.get(aTargetId);
			return tTarget == null ? null : new ItemStack(tTarget.get());
		}

		/** The breeder rod id this product breeds from (the reverse of NBT_VALUE), or 0. */
		public int breederIdOfProduct() {
			return BreederRodSpec.RODS.stream().filter(r -> r.productId() == mRow.id()).findFirst().map(BreederRodSpec::id).orElse(0);
		}

		/** The item-name component of a row id (the tooltip "Turns into"/"Breed from" args). */
		private static Component nameOf(int aId) {
			return Component.translatable("item.gt6." + rowOf(aId).map(RodRow::path).orElse("unknown"));
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			hoverLines(aStack, aTooltip);
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			//21.1: the hover signature carries the Item.TooltipContext (the GT6LaserGasItem fork shape)
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			hoverLines(aStack, aTooltip);
		}
		*///?}

		/** The shared hover face (the whole upstream addToolTips transcription). */
		private void hoverLines(ItemStack aStack, List<Component> aTooltip) {
			aTooltip.add(line("used_in_core"));
			switch (mRow.kind()) {
				case EMPTY -> aTooltip.add(line("empty"));
				case ABSORBER -> aTooltip.add(line("absorber"));
				case REFLECTOR -> aTooltip.add(line("reflector"));
				case MODERATOR -> {
					aTooltip.add(line("moderator.1"));
					aTooltip.add(line("moderator.2"));
					aTooltip.add(line("moderator.3"));
				}
				case DEPLETED -> {
					aTooltip.add(line("depleted.1"));
					aTooltip.add(line("depleted.2"));
				}
				case PRODUCT -> {
					aTooltip.add(line("product.1"));
					aTooltip.add(line("product.2"));
					int tBreeder = breederIdOfProduct();
					if (tBreeder != 0) aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "product.breeds", nameOf(tBreeder)));
				}
				case BREEDER -> {
					aTooltip.add(line("breeder.1"));
					aTooltip.add(line("breeder.2"));
					aTooltip.add(line("breeder.3"));
					aTooltip.add(line("breeder.4"));
					aTooltip.add(line("breeder.5"));
					aTooltip.add(line("breeder.6"));
					BreederRodSpec tBreeder = breederSpec(aStack);
					if (tBreeder != null) {
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "breeder.into", nameOf(tBreeder.productId())));
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "breeder.needed", tBreeder.needed()));
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "breeder.loss", tBreeder.loss()));
					}
				}
				case FUEL -> {
					aTooltip.add(line("fuel.concept.1"));
					aTooltip.add(line("fuel.concept.2"));
					aTooltip.add(line("fuel.concept.3"));
					aTooltip.add(line("fuel.concept.4"));
					FuelRodSpec tFuel = fuelSpec(aStack);
					if (tFuel != null) {
						// the case-0 (water) page of the upstream CLIENT_TIME rotation — the
						// rotating coolant pages ride the render/RCON pool (declared)
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "fuel.emission", tFuel.other()));
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "fuel.self", tFuel.self()));
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "fuel.maximum", tFuel.max()));
						aTooltip.add(Component.translatable(TOOLTIP_PREFIX + "fuel.factor", tFuel.div()));
						if (tFuel.critical()) aTooltip.add(line("fuel.critical"));
					}
				}
				default -> {/**/}
			}
		}

		private Component line(String aKey) {
			return Component.translatable(TOOLTIP_PREFIX + aKey);
		}
	}
}
