package gregtech6.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The three technological component families (task debt-emitter-sensor-generators) —
 * the {@code IL.FIELD_GENERATORS}/{@code IL.EMITTERS}/{@code IL.SENSORS} columns of the
 * upstream component loop, MultiItemTechnological.java:54-56 ({@code for (int i = 0;
 * i < 10; i++)} — ten rungs per family over {@code VN[0..9]} = ULV/LV/MV/HV/EV/IV/LuV/
 * ZPM/UV/PUV1):
 * <ul>
 * <li>FIELD_GENERATORS — upstream ids 12100+i, "Compact Force Field Emitter (&lt;tier&gt;)"
 *     (the :54 row; {@code gt6:field_generator_<tier>}).</li>
 * <li>EMITTERS — ids 12120+i, "Compact Signal Emitter (&lt;tier&gt;)" (the :55 row;
 *     {@code gt6:signal_emitter_<tier>}).</li>
 * <li>SENSORS — ids 12140+i, "Compact Sensor (&lt;tier&gt;)" (the :56 row;
 *     {@code gt6:sensor_<tier>}).</li>
 * </ul>
 * The upstream tooltip column is the EMPTY string on all 30 rows (the {@code ""}
 * third {@code addItem} argument — no tooltip face ported), and the TC aspect columns
 * (:54-56 {@code TC.stack} tails) ride the unported Thaumcraft face (N/A here).
 *
 * <p><b>Why this card exists</b> — the usb-stick card's Q1 ruling (2026-09-26): all
 * three families had NO port identity (the GTMultiBlocks/GT6QuantumEnergizers fusion-
 * wall CUT notes and the GTMachines.java molecular-scanner "absent port identities"
 * ruling all name them as the absent columns). With this file the items land; the
 * SCANNER T3 crafting-seam RESTORE itself is DEFERRED — the seam note lives on the
 * unmerged {@code work/p37-usb-peripherals} branch (commit d0c7cb4f2 rewrote the
 * GT6CraftingRecipes seam band), so writing there now would conflict; the restore
 * rides this branch's rebase or a post-merge mini card.
 *
 * <p><b>Upstream consumer panorama</b> (the card's archaeology, all grep-verified over
 * the upstream tree): the 30 self-crafting rows MultiItemTechnological.java:425-456
 * (ported where the port carries every column — see the GT6CraftingRecipes
 * compactComponent band: rungs 7-9 CUT, no {@code #gt6:circuit7..9} tags exist; the
 * FIELD_GENERATORS ULV row CUT, no fine-wire carrier); the machine crafting rows
 * Loader_MultiTileEntities.java (Quantum Energizers :962-966, Crystal Chargers
 * :970-971, ZPM Dechargers :1000-1001, Matter Fabricators :1542-1546, Molecular
 * Scanner T3 :1551, Matter Replicators :1556-1560, Nanoscale Fabricators :1563-1567,
 * Plantalyzers :1601-1605, Bumblelyzers :1608-1612, the Fusion Reactor :1242, the
 * Large Matter Fabricator :1241, the Logistics Tank :2171 — all already ported with
 * those columns CUT or pooled); the Remote Activator hand tool row
 * MultiItemRandomTools.java:522 (the tool itself unported — pool); the
 * Compat_Recipes_OpenModularTurrets :76-80 rows (no OMT compat in the port — N/A).
 *
 * <p>The creative-tab face joins MACHINES_TAB (the GT6UsbSticks/GT6LaserGas pooling
 * precedent — upstream hangs the rows on the Technological items tab this port does
 * not split out, the GTBarrels:257 fold). KJS surface: REGISTRATION face only,
 * deferred to the KJS binding card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Emitters {

	/** The self-contained registration listener (the GT6UsbSticks shape, ADR-P3-4). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	/** The tier path tokens, VN[0..9] lowercased (CS.java:154; == GTWireSpecs.VN[0..9]). */
	public static final String[] TIER_TOKENS = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1"};

	/** The three family path heads, upstream registration order :54/:55/:56. */
	public static final String FAMILY_FIELD_GENERATORS = "field_generator";
	/** The :55 family head. */
	public static final String FAMILY_EMITTERS = "signal_emitter";
	/** The :56 family head. */
	public static final String FAMILY_SENSORS = "sensor";

	/**
	 * One component row — the (family, tier) pair plus the upstream id for the ledger.
	 * Family-major order (all ten FIELD_GENERATORS, then EMITTERS, then SENSORS);
	 * upstream {@code addItems} interleaves tier-major (:48-57), a display-order
	 * deviation declared here.
	 */
	public record ComponentRow(String family, int tier, int upstreamId) {

		/** The registry path: {@code <family>_<tier token>}. */
		public String path() {
			return family + "_" + TIER_TOKENS[tier];
		}
	}

	/** The 30 rows, family-major over the :54-56 ladder. */
	public static final java.util.List<ComponentRow> ROWS = buildRows();

	private static java.util.List<ComponentRow> buildRows() {
		java.util.List<ComponentRow> rRows = new java.util.ArrayList<>(30);
		for (int i = 0; i < 10; i++) rRows.add(new ComponentRow(FAMILY_FIELD_GENERATORS, i, 12100 + i)); // :54
		for (int i = 0; i < 10; i++) rRows.add(new ComponentRow(FAMILY_EMITTERS, i, 12120 + i));          // :55
		for (int i = 0; i < 10; i++) rRows.add(new ComponentRow(FAMILY_SENSORS, i, 12140 + i));           // :56
		return java.util.List.copyOf(rRows);
	}

	/** The registered items by path (the GT6LaserGas walk container, but map-shaped for the 30). */
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (ComponentRow tRow : ROWS) {
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new Item(new Item.Properties())));
		}
	}

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
	 * The tab walk (the GT6UsbSticks verbatim form — all 30 join the machines tab; the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less would be invisible in both the creative menu
	 * and JEI (the issue #10 lesson).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(gregtech6.registry.GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) aEvent.accept(new ItemStack(tItem.get()));
		}
	}

	private GT6Emitters() {}
}
