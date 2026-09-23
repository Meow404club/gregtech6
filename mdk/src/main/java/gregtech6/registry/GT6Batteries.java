package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.block.energy.GT6BatteryBoxBlock;
import gregtech6.item.energy.GT6BatteryItem;

/**
 * The battery + energy-storage registration (task p29-w4-battery-storage) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6FeConverters shape (ADR-P3-4; a separate class keeps the card scopes disjoint,
 * GTMachines stays zero-edited).
 *
 * <h2>THE CAPACITY LADDER — the transcription and its mechanism</h2>
 * (the card-① density lesson: every transcribed number carries its mechanism, and the
 * table stores the LITERAL product, never a runtime V[i]×mult recomputation — the test
 * pins the same 37 literals independently so a wrong premise cannot hide behind a wrong
 * formula). Upstream files each battery row as
 * {@code NBT_INPUT, V[i], NBT_CAPACITY, V[i] * MULT} (Loader_MultiTileEntities.java
 * :1009-:1092) — the capacity is the tier packet size (V = {8, 32, 128, 512, 2048, 8192},
 * GTWireSpecs.V) times the FAMILY multiplier, i.e. the battery holds exactly
 * {@code MULT} packets of its tier: LeadAcid 2000, Alkaline 4000, NiCd 4000,
 * LiCoO2 64000, LiMn 128000, Energium Red 400000, Cyan 800000 EU/LU. The multiplier IS
 * the chemistry: a bigger multiple means more total charge in the same packet window.
 *
 * <h2>The family/type layout</h2>
 * 37 items over 16 upstream MTE classes: the five EU families ride V[0..4]
 * (ULV..EV, TD.Energy.EU — LeadAcid 14000-14004 / Alkaline 14010-14014 / NiCd 14020-14024
 * / LiCoO2 14030-14034 / LiMn 14040-14044) and the two LU crystal families ride V[0..5]
 * (ULV..IV, TD.Energy.LU — Red 14500-14505 / Cyan 14510-14515; the LU domain is live in
 * the port, TD.java carries it and the W2 source-block short codes prove the routing).
 * The metaIds ride {@link BatteryRow#metaId} as the parity column (the dynamo form).
 *
 * <h2>The declared deviations</h2>
 * <ul>
 * <li><b>The circuit ladder (7 items)</b>: upstream recipes reference circuits by the
 *     oredict tag ladder {@code OD_CIRCUITS[i] = "gt:circuit0..9"} (upstream CS.java:166),
 *     carried by the OP.circuit tier materials (MT.DATA.CIRCUITS = Primitive/Basic/Good/
 *     Advanced/Elite/Master/...). The port has NO circuit item rows (the itemPathPrefixes
 *     gate omits OP.circuit — the generic prefix walk would register one circuit item per
 *     EVERY material, which is not the upstream shape either; the real circuit family is
 *     the p24 circuit-census card's domain). This card registers the MINIMUM seven-item
 *     ladder ({@code gt6:circuit_primitive..circuit_ultimate}, the OD_CIRCUITS[0..6]
 *     carriers — [6] = Ultimate, the column the LiCoO2/LiMn EV rows carry) so the
 *     battery/box crafting closure resolves; the selector/256-icon face
 *     stays with the future circuit card. Tags: {@code #gt6:circuit0..circuit5} — the
 *     upstream tag names byte-kept (already snake-legal).</li>
 * <li><b>The BatteryBox tier closure (12 blocks, not upstream 20)</b>: upstream registers
 *     10 tiers × 2 sizes (:893-:896, V[0..9]); the port tier system closes at IV
 *     (V[0..5] — the W2 5-tier window, the p28 Electric_T[0..5] ladder), so this card
 *     registers 6×2 = 12 BatteryBoxes. The V[6..9] rows are the extension seat (the
 *     transformer higher-pairs posture: "the ladder rows land when their tiers do").</li>
 * <li><b>The tag translation</b>: the upstream oredict seams {@code "gt:re-battery0..4"}
 *     and {@code "gt:re-crystal0..5"} (the Loader :1009-:1092 tail columns — the W5
 *     electric-tool capacity-sum face iterates exactly these, Loader_Tools.java:356-377)
 *     become the item tags {@code #gt6:re_battery0..4} / {@code #gt6:re_crystal0..5}
 *     (the dash→underscore snake rule; the tier digits stay attached, no precedent splits
 *     them).</li>
 * <li><b>The cells</b>: the five {@code Battery_*_Cell_Filled} items (upstream
 *     MultiItemTechnological.java:462/:467/:472/:477/:482 — "Lead-Acid Cell (Filled)" and
 *     siblings) are plain single items (the P14 single-item ruling); the empty-cell twins
 *     and their canning rows ride the pool.</li>
 * </ul>
 *
 * <p>The battery CRAFTING rows (the :1009-:1068 recipe strings: plate BatteryAlloy + the
 * family cell + the tier cable [+ the tier circuit from MV up]) and the BatteryBox rows
 * (:894-:895: tier wire + tier cable + tier circuit + the tier casing / the tier
 * transformer) live in the GT6CraftingRecipes datagen face over THESE row tables. The
 * Energium crystals carry NO crafting row — upstream registers none either (:1079-:1092
 * are bare registrations).
 *
 * <p>KJS surface (the card declaration): this card's output is the REGISTRATION face
 * (37 batteries + 5 cells + 7 circuits + 12 BatteryBox blocks/BEs) plus the tier-a
 * crafting JSON; there are NO datapack recipe-map rows and NO KubeJS face.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Batteries {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One battery row — the upstream-parity columns of one Loader :1009-:1092 registration line. */
	public record BatteryRow(String path, int metaId, String family, int tier, String tierWord, Supplier<TagData> type,
			long sizeRec, long capacity, String tagPath) {}

	/** One cell row — the MultiItemTechnological Filled-cell subset (the crafting 'B' column). */
	public record CellRow(String path, int metaId, String enName) {}

	/** One circuit row — the OD_CIRCUITS[0..6] carrier (the class-doc deviation). */
	public record CircuitRow(String path, int tier, String tagPath, String enName) {}

	/** One BatteryBox row — the Loader :893-:896 loop iteration (the port tier-closed subset). */
	public record BoxRow(String path, int metaId, int tier, String tierWord, int slots) {}

	/** The tier words VN[0..5] (upstream CS.java:154; VN[5] = "IV", the W2 naming erratum). */
	public static final String[] TIER_WORDS = {"ULV", "LV", "MV", "HV", "EV", "IV"};

	private static Supplier<TagData> eu() {return () -> TD.Energy.EU;}
	private static Supplier<TagData> lu() {return () -> TD.Energy.LU;}

	/**
	 * The 37 battery rows, upstream line order (:1009-:1013 LeadAcid, :1033-:1037 Alkaline,
	 * :1044-:1048 NiCd, :1054-:1058 LiCoO2, :1064-:1068 LiMn, :1079-:1084 Red, :1087-:1092
	 * Cyan). The capacities are the LITERAL V[i]×mult products (the class-doc mechanism);
	 * GT6BatteryLadderTest pins every one of the 37 numbers independently.
	 */
	public static final List<BatteryRow> ROWS = List.of(
			// Lead-Acid — V[0..4] × 2000 (:1009-:1013)
			new BatteryRow("battery_lead_acid_ulv", 14000, "lead_acid", 0, "ULV", eu(),   8,      16000, "re_battery0"),
			new BatteryRow("battery_lead_acid_lv" , 14001, "lead_acid", 1, "LV" , eu(),  32,      64000, "re_battery1"),
			new BatteryRow("battery_lead_acid_mv" , 14002, "lead_acid", 2, "MV" , eu(), 128,     256000, "re_battery2"),
			new BatteryRow("battery_lead_acid_hv" , 14003, "lead_acid", 3, "HV" , eu(), 512,    1024000, "re_battery3"),
			new BatteryRow("battery_lead_acid_ev" , 14004, "lead_acid", 4, "EV" , eu(),2048,    4096000, "re_battery4"),
			// Alkaline — V[0..4] × 4000 (:1033-:1037)
			new BatteryRow("battery_alkaline_ulv" , 14010, "alkaline" , 0, "ULV", eu(),   8,      32000, "re_battery0"),
			new BatteryRow("battery_alkaline_lv"  , 14011, "alkaline" , 1, "LV" , eu(),  32,     128000, "re_battery1"),
			new BatteryRow("battery_alkaline_mv"  , 14012, "alkaline" , 2, "MV" , eu(), 128,     512000, "re_battery2"),
			new BatteryRow("battery_alkaline_hv"  , 14013, "alkaline" , 3, "HV" , eu(), 512,    2048000, "re_battery3"),
			new BatteryRow("battery_alkaline_ev"  , 14014, "alkaline" , 4, "EV" , eu(),2048,    8192000, "re_battery4"),
			// Nickel-Cadmium — V[0..4] × 4000 (:1044-:1048)
			new BatteryRow("battery_nicd_ulv"     , 14020, "nicd"     , 0, "ULV", eu(),   8,      32000, "re_battery0"),
			new BatteryRow("battery_nicd_lv"      , 14021, "nicd"     , 1, "LV" , eu(),  32,     128000, "re_battery1"),
			new BatteryRow("battery_nicd_mv"      , 14022, "nicd"     , 2, "MV" , eu(), 128,     512000, "re_battery2"),
			new BatteryRow("battery_nicd_hv"      , 14023, "nicd"     , 3, "HV" , eu(), 512,    2048000, "re_battery3"),
			new BatteryRow("battery_nicd_ev"      , 14024, "nicd"     , 4, "EV" , eu(),2048,    8192000, "re_battery4"),
			// Lithium-Cobalt — V[0..4] × 64000 (:1054-:1058)
			new BatteryRow("battery_licoo2_ulv"   , 14030, "licoo2"   , 0, "ULV", eu(),   8,     512000, "re_battery0"),
			new BatteryRow("battery_licoo2_lv"    , 14031, "licoo2"   , 1, "LV" , eu(),  32,    2048000, "re_battery1"),
			new BatteryRow("battery_licoo2_mv"    , 14032, "licoo2"   , 2, "MV" , eu(), 128,    8192000, "re_battery2"),
			new BatteryRow("battery_licoo2_hv"    , 14033, "licoo2"   , 3, "HV" , eu(), 512,   32768000, "re_battery3"),
			new BatteryRow("battery_licoo2_ev"    , 14034, "licoo2"   , 4, "EV" , eu(),2048,  131072000, "re_battery4"),
			// Lithium-Manganese — V[0..4] × 128000 (:1064-:1068)
			new BatteryRow("battery_limn_ulv"     , 14040, "limn"     , 0, "ULV", eu(),   8,    1024000, "re_battery0"),
			new BatteryRow("battery_limn_lv"      , 14041, "limn"     , 1, "LV" , eu(),  32,    4096000, "re_battery1"),
			new BatteryRow("battery_limn_mv"      , 14042, "limn"     , 2, "MV" , eu(), 128,   16384000, "re_battery2"),
			new BatteryRow("battery_limn_hv"      , 14043, "limn"     , 3, "HV" , eu(), 512,   65536000, "re_battery3"),
			new BatteryRow("battery_limn_ev"      , 14044, "limn"     , 4, "EV" , eu(),2048,  262144000, "re_battery4"),
			// Red Energium Crystal — V[0..5] × 400000 (:1079-:1084, TD.Energy.LU)
			new BatteryRow("energium_red_ulv"     , 14500, "energium_red" , 0, "ULV", lu(),   8,   3200000, "re_crystal0"),
			new BatteryRow("energium_red_lv"      , 14501, "energium_red" , 1, "LV" , lu(),  32,  12800000, "re_crystal1"),
			new BatteryRow("energium_red_mv"      , 14502, "energium_red" , 2, "MV" , lu(), 128,  51200000, "re_crystal2"),
			new BatteryRow("energium_red_hv"      , 14503, "energium_red" , 3, "HV" , lu(), 512, 204800000, "re_crystal3"),
			new BatteryRow("energium_red_ev"      , 14504, "energium_red" , 4, "EV" , lu(),2048, 819200000, "re_crystal4"),
			new BatteryRow("energium_red_iv"      , 14505, "energium_red" , 5, "IV" , lu(),8192,3276800000L, "re_crystal5"),
			// Cyan Energium Crystal — V[0..5] × 800000 (:1087-:1092, TD.Energy.LU)
			new BatteryRow("energium_cyan_ulv"    , 14510, "energium_cyan", 0, "ULV", lu(),   8,   6400000, "re_crystal0"),
			new BatteryRow("energium_cyan_lv"     , 14511, "energium_cyan", 1, "LV" , lu(),  32,  25600000, "re_crystal1"),
			new BatteryRow("energium_cyan_mv"     , 14512, "energium_cyan", 2, "MV" , lu(), 128, 102400000, "re_crystal2"),
			new BatteryRow("energium_cyan_hv"     , 14513, "energium_cyan", 3, "HV" , lu(), 512, 409600000, "re_crystal3"),
			new BatteryRow("energium_cyan_ev"     , 14514, "energium_cyan", 4, "EV" , lu(),2048,1638400000L, "re_crystal4"),
			new BatteryRow("energium_cyan_iv"     , 14515, "energium_cyan", 5, "IV" , lu(),8192,6553600000L, "re_crystal5"));

	/** The five Filled cells (upstream MultiItemTechnological :462/:467/:472/:477/:482). */
	public static final List<CellRow> CELL_ROWS = List.of(
			new CellRow("battery_cell_lead_acid", 20001, "Lead-Acid Cell (Filled)"),
			new CellRow("battery_cell_alkaline" , 20003, "Alkaline Button Cell (Filled)"),
			new CellRow("battery_cell_nicd"     , 20005, "Nickel-Cadmium Cell (Filled)"),
			new CellRow("battery_cell_licoo2"   , 20007, "Lithium-Cobalt Cell (Filled)"),
			new CellRow("battery_cell_limn"     , 20009, "Lithium-Manganese Cell (Filled)"));

	/** The seven circuit carriers — upstream MT.DATA.CIRCUITS[0..6] + the OD_CIRCUITS tag ladder (the class-doc deviation). */
	public static final List<CircuitRow> CIRCUIT_ROWS = List.of(
			new CircuitRow("circuit_primitive", 0, "circuit0", "Primitive Circuit"),
			new CircuitRow("circuit_basic"    , 1, "circuit1", "Basic Electronic Circuit"),
			new CircuitRow("circuit_good"     , 2, "circuit2", "Good Electronic Circuit"),
			new CircuitRow("circuit_advanced" , 3, "circuit3", "Advanced Electronic Circuit"),
			new CircuitRow("circuit_elite"    , 4, "circuit4", "Elite Electronic Circuit"),
			new CircuitRow("circuit_master"   , 5, "circuit5", "Master Electronic Circuit"),
			new CircuitRow("circuit_ultimate" , 6, "circuit6", "Ultimate Electronic Circuit"));

	/**
	 * The 12 BatteryBox rows — the :893-:896 loop closed at the port tiers (the class-doc
	 * tier-closure deviation): 6 small (NBT_INV_SIZE 4, metaId 10080+i) + 6 large
	 * (NBT_INV_SIZE 16, metaId 10090+i). The in/out ride V[tier] BOTH directions
	 * ({@code NBT_INPUT, V[i], NBT_OUTPUT, V[i]}).
	 */
	public static final List<BoxRow> BOX_ROWS = List.of(
			new BoxRow("battery_box_ulv"        , 10080, 0, "ULV",  4),
			new BoxRow("battery_box_lv"         , 10081, 1, "LV" ,  4),
			new BoxRow("battery_box_mv"         , 10082, 2, "MV" ,  4),
			new BoxRow("battery_box_hv"         , 10083, 3, "HV" ,  4),
			new BoxRow("battery_box_ev"         , 10084, 4, "EV" ,  4),
			new BoxRow("battery_box_iv"         , 10085, 5, "IV" ,  4),
			new BoxRow("battery_box_large_ulv"  , 10090, 0, "ULV", 16),
			new BoxRow("battery_box_large_lv"   , 10091, 1, "LV" , 16),
			new BoxRow("battery_box_large_mv"   , 10092, 2, "MV" , 16),
			new BoxRow("battery_box_large_hv"   , 10093, 3, "HV" , 16),
			new BoxRow("battery_box_large_ev"   , 10094, 4, "EV" , 16),
			new BoxRow("battery_box_large_iv"   , 10095, 5, "IV" , 16));

	// ---------------------------------------------------------------------------
	// the registrations (the GTWires static-loop shape — the row tables are the source)
	// ---------------------------------------------------------------------------

	/**
	 * The ZPM row — the Loader :1103 single-item registration (task p36-energy-zpm-dechargers;
	 * NOT a {@link BatteryRow}: the ladder records drive the crafting/cell/tag faces and the
	 * ZPM carries none of them — no recipe (the dungeon artifact, TODO.md:389), no cell, no
	 * tag). Columns: meta 14999, packet V[7]=131072, the explicit NBT_INPUT_MIN 1 /
	 * NBT_INPUT_MAX VMAX[7]=262144 band, NBT_CAPACITY 2_000_000_000_000L, TD.Energy.QU.
	 * The discharge-only face rides {@link gregtech6.item.energy.GT6ZpmItem} (the :79-:80
	 * overrides), the decharger seat is {@code GT6ZpmDechargers}.
	 */
	public record ZpmRow(String path, int metaId, long sizeRec, long sizeMin, long sizeMax, long capacity) {}

	/** The ZPM row literals — the LadderTest-form pins read this face. */
	public static final ZpmRow ZPM = new ZpmRow("zpm", 14999, GTWireSpecs.V[7], 1L, GTWireSpecs.VMAX[7], 2_000_000_000_000L);

	/** The ZPM item — stack 16 (the :1103 stack column), the :111-:117 creative pair rides the tab walk. */
	public static final RegistryObject<Item> ZPM_ITEM = ITEMS.register(ZPM.path(),
			() -> new gregtech6.item.energy.GT6ZpmItem(new Item.Properties().stacksTo(16),
					ZPM.sizeRec(), ZPM.capacity(), ZPM.sizeMin()));

	/** The 37 battery items, keyed by path (the datagen/test lookup seam). */
	public static final Map<String, RegistryObject<Item>> BATTERY_ITEMS = new LinkedHashMap<>();
	static {
		for (BatteryRow tRow : ROWS) {
			BATTERY_ITEMS.put(tRow.path(), ITEMS.register(tRow.path(), () -> batteryItem(tRow)));
		}
	}

	private static GT6BatteryItem batteryItem(BatteryRow aRow) {
		// stack 16 (the upstream aRegistry stack column); the per-stack charge overrides
		// the limit down to 1 when charged (the GT6BatteryItem :143 face)
		return new GT6BatteryItem(new Item.Properties().stacksTo(16), aRow.sizeRec(), aRow.capacity(), aRow.type().get());
	}

	/** The five Filled cells — plain single items (the P14 ruling). */
	public static final Map<String, RegistryObject<Item>> CELL_ITEMS = new LinkedHashMap<>();
	static {
		for (CellRow tRow : CELL_ROWS) {
			CELL_ITEMS.put(tRow.path(), ITEMS.register(tRow.path(), () -> new Item(new Item.Properties())));
		}
	}

	/** The seven circuit carriers — plain single items (the class-doc deviation). */
	public static final Map<String, RegistryObject<Item>> CIRCUIT_ITEMS = new LinkedHashMap<>();
	static {
		for (CircuitRow tRow : CIRCUIT_ROWS) {
			CIRCUIT_ITEMS.put(tRow.path(), ITEMS.register(tRow.path(), () -> new Item(new Item.Properties())));
		}
	}

	/** The 12 BatteryBox blocks, keyed by path. */
	public static final Map<String, RegistryObject<Block>> BATTERY_BOX_BLOCKS = new LinkedHashMap<>();
	/** The 12 BatteryBox items, keyed by path (stack 16, the upstream column). */
	public static final Map<String, RegistryObject<Item>> BATTERY_BOX_ITEMS = new LinkedHashMap<>();

	/** The 4-slot family BET (the six small blocks). */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity>> BATTERY_BOX_BE =
			BLOCK_ENTITY_TYPES.register("battery_box", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity(null, aPos, aState),
					BATTERY_BOX_BLOCKS.get("battery_box_ulv").get(), BATTERY_BOX_BLOCKS.get("battery_box_lv").get(),
					BATTERY_BOX_BLOCKS.get("battery_box_mv").get(), BATTERY_BOX_BLOCKS.get("battery_box_hv").get(),
					BATTERY_BOX_BLOCKS.get("battery_box_ev").get(), BATTERY_BOX_BLOCKS.get("battery_box_iv").get()).build(null));

	/** The 16-slot family BET (the six large blocks). */
	public static final RegistryObject<BlockEntityType<gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity>> BATTERY_BOX_LARGE_BE =
			BLOCK_ENTITY_TYPES.register("battery_box_large", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity(null, aPos, aState),
					BATTERY_BOX_BLOCKS.get("battery_box_large_ulv").get(), BATTERY_BOX_BLOCKS.get("battery_box_large_lv").get(),
					BATTERY_BOX_BLOCKS.get("battery_box_large_mv").get(), BATTERY_BOX_BLOCKS.get("battery_box_large_hv").get(),
					BATTERY_BOX_BLOCKS.get("battery_box_large_ev").get(), BATTERY_BOX_BLOCKS.get("battery_box_large_iv").get()).build(null));

	static {
		for (BoxRow tRow : BOX_ROWS) {
			RegistryObject<Block> tBlock = BLOCKS.register(tRow.path(), () -> batteryBox(tRow));
			BATTERY_BOX_BLOCKS.put(tRow.path(), tBlock);
			BATTERY_BOX_ITEMS.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(BATTERY_BOX_BLOCKS.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	/**
	 * The row block: hardness/resistance 4.0/4.0 (the NBT_HARDNESS/NBT_RESISTANCE columns),
	 * metal sounds, the family BET supplier picked by the slot count (the two-loop form of
	 * the :893-:896 pair).
	 */
	private static GT6BatteryBoxBlock batteryBox(BoxRow aRow) {
		return new GT6BatteryBoxBlock(BlockBehaviour.Properties.of().strength(4.0F, 4.0F).sound(SoundType.METAL),
				aRow.tier(), aRow.slots(), () -> aRow.slots() == 4 ? BATTERY_BOX_BE.get() : BATTERY_BOX_LARGE_BE.get());
	}

	// ---------------------------------------------------------------------------
	// the tab join (the GT6FeConverters onBuildTabContents verbatim form — zero GTMachines edits)
	// ---------------------------------------------------------------------------

	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (BatteryRow tRow : ROWS) aEvent.accept(new ItemStack(BATTERY_ITEMS.get(tRow.path()).get()));
			// the ZPM creative pair — the :111-:117 getSubItems verbatim (empty + FULL)
			for (ItemStack tStack : gregtech6.item.energy.GT6ZpmItem.creativeStacks(
					(gregtech6.item.energy.GT6ZpmItem) ZPM_ITEM.get())) aEvent.accept(tStack);
			for (CellRow tRow : CELL_ROWS) aEvent.accept(new ItemStack(CELL_ITEMS.get(tRow.path()).get()));
			for (CircuitRow tRow : CIRCUIT_ROWS) aEvent.accept(new ItemStack(CIRCUIT_ITEMS.get(tRow.path()).get()));
			for (BoxRow tRow : BOX_ROWS) aEvent.accept(new ItemStack(BATTERY_BOX_ITEMS.get(tRow.path()).get()));
		}
	}

	private GT6Batteries() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ElectricDynamos fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework — the GTBarrels fork form.
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}

	/** Registration smoke evidence (the GT6FoodCans onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 batteries registered: {} battery items + the ZPM artifact ({} QU capacity) + {} cells + {} circuit carriers + {} battery boxes (the p29-w4 storage face, the p36 ZPM tail)",
					BATTERY_ITEMS.size(), ZPM.capacity(), CELL_ITEMS.size(), CIRCUIT_ITEMS.size(), BATTERY_BOX_BLOCKS.size());
		});
	}

	/** The display name of a battery row (the upstream composed form, the datagen lang face). */
	public static String batteryEnName(BatteryRow aRow) {
		return switch (aRow.family()) {
			case "lead_acid" -> "Lead-Acid Battery (" + aRow.tierWord() + ")";
			case "alkaline" -> "Alkaline Battery (" + aRow.tierWord() + ")";
			case "nicd" -> "Nickel-Cadmium Battery (" + aRow.tierWord() + ")";
			case "licoo2" -> "Lithium-Cobalt Battery (" + aRow.tierWord() + ")";
			case "limn" -> "Lithium-Manganese Battery (" + aRow.tierWord() + ")";
			case "energium_red" -> "Red Energium Crystal (T" + aRow.tier() + ")";
			case "energium_cyan" -> "Cyan Energium Crystal (T" + aRow.tier() + ")";
			default -> throw new IllegalStateException("gt6 batteries: unknown family " + aRow.family());
		};
	}

	/** The display name of a BatteryBox row (the :894-:895 composed form). */
	public static String boxEnName(BoxRow aRow) {
		return (aRow.slots() == 16 ? "Large Battery Box (" : "Battery Box (") + aRow.tierWord() + ")";
	}

	// ---------------------------------------------------------------------------
	// the recipe mapping (the Loader :1009-:1092 / :893-:896 columns as data — the
	// datagen and the tests share this single source)
	// ---------------------------------------------------------------------------

	/**
	 * The CABLES_01/WIRES_01 [0..5] wire tokens (upstream MT.java:3595/:3631 = Pb, Sn,
	 * ANY.Cu, Au, Al, Pt — the wire rows' tokens are "lead"/"tin"/"copper"/"gold"/
	 * "aluminium"/"platinum", GTWireSpecs.ROWS). The port wire/cable items ride these
	 * tokens: {@code wire_<token>_gt<size>} / {@code cable_<token>_gt<size>} (the
	 * GTWireSpecs.registryName composition).
	 */
	public static final String[] WIRE_TOKENS = {"lead", "tin", "copper", "gold", "aluminium", "platinum"};

	/** The registry path of a wire/cable item — the GTWireSpecs.registryName composition rule. */
	public static String wirePath(int aTier, int aSize, boolean aInsulated) {
		return (aInsulated ? "cable_" : "wire_") + WIRE_TOKENS[aTier] + "_gt" + (aSize < 10 ? "0" : "") + aSize;
	}

	/** The battery row's crafting pattern (the :1009-:1068 recipe strings; 'W' wire, 'x' wirecutter tool, 'B' cell, 'P' plate, 'C' circuit). */
	public static String[] batteryPattern(BatteryRow aRow) {
		boolean tAdv = aRow.family().equals("licoo2") || aRow.family().equals("limn");
		return switch (aRow.tierWord()) {
			case "ULV" -> tAdv ? new String[] {"Wx", "BC", "P "} : new String[] {"Wx", "B ", "P "};
			case "LV"  -> tAdv ? new String[] {"CWx", "PBP", " B "} : new String[] {" Wx", "PBP", " B "};
			case "MV"  -> new String[] {"WxW", "BCB", "PBP"};
			case "HV"  -> new String[] {"WxW", "BCB", "BPB"};
			case "EV"  -> new String[] {"WPW", "BCB", "BBB"};
			default -> throw new IllegalStateException("gt6 batteries: no recipe for " + aRow.path());
		};
	}

	/**
	 * The battery row's circuit-tag tier (the 'C' column = OD_CIRCUITS[i]), or -1 when the
	 * row carries no circuit: standard families ride [2]/[3]/[4] on MV/HV/EV, the
	 * advanced (LiCoO2/LiMn) ladder is shifted one rung up ([2]/[3]/[4]/[5]/[6] from ULV).
	 */
	public static int batteryCircuitTier(BatteryRow aRow) {
		boolean tAdv = aRow.family().equals("licoo2") || aRow.family().equals("limn");
		if (tAdv) return aRow.tier() + 2; // ULV..EV -> circuit2..circuit6
		return switch (aRow.tierWord()) {
			case "MV" -> 2;
			case "HV" -> 3;
			case "EV" -> 4;
			default -> -1;
		};
	}

	/** The battery row's cell item path (the 'B' column — the family's Filled cell). */
	public static String batteryCellPath(BatteryRow aRow) {
		return "battery_cell_" + aRow.family();
	}

	/** The BatteryBox crafting pattern (the :894-:895 verbatim: 'W' wire, 'C' cable, 'X' circuit, 'M' casing/transformer). */
	public static final String[] BOX_PATTERN = {"WCW", "WCW", "XMX"};

	/** The display rows the datagen lang face walks (batteries, then cells, then circuits, then boxes — the tab order). */
	public static List<String> allItemPaths() {
		List<String> rPaths = new ArrayList<>(BATTERY_ITEMS.size() + CELL_ITEMS.size() + CIRCUIT_ITEMS.size() + BATTERY_BOX_ITEMS.size());
		rPaths.addAll(BATTERY_ITEMS.keySet());
		rPaths.addAll(CELL_ITEMS.keySet());
		rPaths.addAll(CIRCUIT_ITEMS.keySet());
		rPaths.addAll(BATTERY_BOX_ITEMS.keySet());
		return rPaths;
	}
}
