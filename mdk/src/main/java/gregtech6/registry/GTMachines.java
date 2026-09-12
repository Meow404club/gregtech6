package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.block.GTOvenBlock;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * Machine block + BlockEntityType + item registration, card-owned (ADR-P3-4): the deferred
 * registers attach to the mod bus from this self-contained {@code @EventBusSubscriber(MOD)}
 * listener — GT6Mod.java / GTModBusListener.java / GTBlockEntities.java stay untouched
 * (the W2 oven is the first machine, and the machine family gets its own registration
 * home instead of growing the example-chest registry).
 *
 * <p>Also wires {@link GT6RecipeMaps#init()} into the mod lifecycle — the W1 recipe-core
 * handoff left the FURNACE map un-initialized on purpose ("W2 (p4-machine-oven) wires
 * init() into the mod lifecycle", GT6RecipeMaps.java:36-37). init() is idempotent and
 * runs before any BlockEvent/BET registration, so every TileEntityOven resolves
 * {@code RM.Furnace} from its very first tick.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMachines {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	// ---------------------------------------------------------------------------
	// the Oven family (task p27-oven-heat-t-ladder) — the four Heat_T[1..4] rows
	// Loader_MultiTileEntities.java:1288-1291 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "oven" on every row, TD.Energy.HU upstream / the port EU carrier,
	// RM.Furnace, no parallel keys): one class (GTOvenBlock), one BET (OVEN_BE — the
	// ADR-P3-1 one-class-many-material-blocks counterpart), four material blocks in
	// upstream row order. The NBT_HARDNESS column 6.0/4.0/9.0/12.5 (NBT_RESISTANCE ==
	// hardness, T2 = the 4.0F special case); the NBT_INPUT column 32/128/512/2048 rides
	// {@link #TIER_INPUTS} through the GTOvenBlock row index (the TileEntityOven
	// constructor assignment); the display names compose over
	// {@link #OVEN_DISPLAY_KEY} + the gt6.row.mat units (the W1 one-slot form — the
	// upstream name column "Oven ("+aMat.getLocal()+")", the Dryer/Distillery/Extruder
	// Heat_T word set). Zero-regression: the T1 id stays "oven".
	// ---------------------------------------------------------------------------

	/**
	 * The three upstream tier ladders (task p27-machine-material-tint-fidelity) — the
	 * lazy material suppliers behind every row's NBT_MATERIAL column. Upstream
	 * MT.java:3689-3691 declares them as Java 0-based arrays the Loader rows index
	 * {@code [1..4]} (Loader_MultiTileEntities.java:1288-1318/:1343/:1373/:1379/:1398/
	 * :1406/:1425): Heat_T[1..4] = Steel/Invar/Ti/TungstenCarbide, Kinetic_T[1..4] =
	 * Bronze/Steel/Ti/TungstenSteel, Electric_T[1..4] = SteelGalvanized/Al/
	 * StainlessSteel/Cr (the Canner VN-name rows — the name column rides the voltage
	 * word, the NBT_MATERIAL column the Electric_T material). The 1.7.10 registration
	 * derives the render colour from exactly these (MultiTileEntityClassContainer.java:51,
	 * {@code getRGBInt(material.fRGBaSolid)}). SUPPLIERS, not fields: the GTWireSpecs:35
	 * ruling — the registry classes load before {@code MT.init()}, a direct {@code MT.X}
	 * read in a row initializer would resolve null.
	 */
	public static final java.util.List<java.util.function.Supplier<OreDictMaterial>> HEAT_T_LADDER = java.util.List.of(
			() -> gregapi.data.MT.Steel, () -> gregapi.data.MT.Invar, () -> gregapi.data.MT.Ti, () -> gregapi.data.MT.TungstenCarbide);

	/** The Kinetic_T[1..4] ladder (upstream MT.java:3690) — the Shredder/Crusher/Lathe/Sifter/Compressor/Wiremill/Press rows. */
	public static final java.util.List<java.util.function.Supplier<OreDictMaterial>> KINETIC_T_LADDER = java.util.List.of(
			() -> gregapi.data.MT.Bronze, () -> gregapi.data.MT.Steel, () -> gregapi.data.MT.Ti, () -> gregapi.data.MT.TungstenSteel);

	/** The Electric_T[1..4] ladder (upstream MT.java:3691) — the Canner rows (the VN display words stay LV/MV/HV/EV). */
	public static final java.util.List<java.util.function.Supplier<OreDictMaterial>> ELECTRIC_T_LADDER = java.util.List.of(
			() -> gregapi.data.MT.SteelGalvanized, () -> gregapi.data.MT.Al, () -> gregapi.data.MT.StainlessSteel, () -> gregapi.data.MT.Cr);

	// the T0 housing materials (task p28-c-ulv-machine-ladder) — upstream MT.java:3690-3691
	// index 0, the ladder rung BELOW the four [1..4] rows every existing family rides:
	// Kinetic_T[0] = ANY.Wood ("Any Wood", ANY.java:77 createMaterial(-1, "Any Wood", ...);
	// the port alias MT.AnyWood = ANY.Wood, MT.java:2801) and Electric_T[0] = TinAlloy
	// (MT.java:2491, the same material the upstream ULV-LV Transformer :881 registers its
	// housing with). The upstream rows themselves have NO ULV rung (research.p28-r-ulv-tier-
	// design: VN[0] carries zero machines upstream) — these are the declared-deviation ULV
	// rows' NBT_MATERIAL columns, the card's "T0 对应" arm.
	/** The Kinetic_T[0] rung (ANY.Wood, upstream MT.java:3690 index 0) — the kinetic ULV rows' NBT_MATERIAL column. */
	public static final java.util.function.Supplier<OreDictMaterial> KINETIC_T0 = () -> gregapi.data.ANY.Wood;
	/** The Electric_T[0] rung (TinAlloy, upstream MT.java:3691 index 0) — the Canner ULV row's NBT_MATERIAL column. */
	public static final java.util.function.Supplier<OreDictMaterial> ELECTRIC_T0 = () -> gregapi.data.MT.TinAlloy;

	/** The Oven family display template key ({@code gt6.row.oven.display} — the W1 one-slot material-word form). */
	public static final String OVEN_DISPLAY_KEY = "gt6.row.oven.display";

	/** One Oven ladder row — the upstream-parity columns of one aRegistry.add line (:1288-1291). */
	public record OvenRow(String path, String matSlug, String matDisplay, java.util.function.Supplier<OreDictMaterial> material, int metaId, float hardness, int tier) {}

	/**
	 * The four Oven rows, upstream line order :1288-1291 (T1-T4, the Heat_T ladder — the
	 * MT.java:3689 locals ANY.Steel/Invar/Ti/TungstenCarbide, the same Steel/Invar/
	 * Titanium/Tungsten Carbide word set the Dryer/Distillery/Extruder Heat_T families
	 * carry; the :1289 hardness 4.0F is the upstream T2 special case). The material
	 * column is the task p27-machine-material-tint-fidelity NBT_MATERIAL mirror.
	 */
	public static final java.util.List<OvenRow> OVEN_ROWS = java.util.List.of(
			new OvenRow("oven"   , "steel"           , "Steel"           , HEAT_T_LADDER.get(0), 20001,  6.0F, 0),
			new OvenRow("oven_t2", "invar"           , "Invar"           , HEAT_T_LADDER.get(1), 20002,  4.0F, 1),
			new OvenRow("oven_t3", "titanium"        , "Titanium"        , HEAT_T_LADDER.get(2), 20003,  9.0F, 2),
			new OvenRow("oven_t4", "tungsten_carbide", "Tungsten Carbide", HEAT_T_LADDER.get(3), 20004, 12.5F, 3));

	/**
	 * The composed {@code "Oven (<material word>)"} supplier of one ladder row — the
	 * GTBasicMachineBlock tierName shape re-based on the gt6.row.mat unit (the tier rides
	 * the material word upstream, so the family template fills exactly ONE slot).
	 */
	private static java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> ovenName(String aMatSlug) {
		return () -> net.minecraft.network.chat.Component.translatable(OVEN_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable("gt6.row.mat." + aMatSlug));
	}

	/** The Oven T1 block — the original "oven" id (the hardness/resistance 6.0/6.0 row :1288). */
	public static final RegistryObject<net.minecraft.world.level.block.Block> OVEN = BLOCKS.register("oven",
			() -> new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), 0, ovenName("steel")));

	public static final RegistryObject<net.minecraft.world.level.block.Block> OVEN_T2 = BLOCKS.register("oven_t2",
			() -> new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(4.0F, 4.0F).sound(SoundType.METAL), 1, ovenName("invar")));

	public static final RegistryObject<net.minecraft.world.level.block.Block> OVEN_T3 = BLOCKS.register("oven_t3",
			() -> new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), 2, ovenName("titanium")));

	public static final RegistryObject<net.minecraft.world.level.block.Block> OVEN_T4 = BLOCKS.register("oven_t4",
			() -> new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), 3, ovenName("tungsten_carbide")));

	/**
	 * The Oven BET: one class, the four ladder blocks multi-attached (the p8 family-BET
	 * shape — Builder.of varargs). Registry path mirrors TileEntityOven#getTileEntityName
	 * like the chest/test-machine pairs.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityOven>> OVEN_BE =
			BLOCK_ENTITY_TYPES.register("oven", () -> BlockEntityType.Builder.of(
					TileEntityOven::new, OVEN.get(), OVEN_T2.get(), OVEN_T3.get(), OVEN_T4.get()).build(null));

	public static final RegistryObject<Item> OVEN_ITEM = ITEMS.register("oven",
			() -> new gregtech6.block.GTComposedNameItem(OVEN.get(), new Item.Properties()));

	public static final RegistryObject<Item> OVEN_T2_ITEM = ITEMS.register("oven_t2",
			() -> new gregtech6.block.GTComposedNameItem(OVEN_T2.get(), new Item.Properties()));

	public static final RegistryObject<Item> OVEN_T3_ITEM = ITEMS.register("oven_t3",
			() -> new gregtech6.block.GTComposedNameItem(OVEN_T3.get(), new Item.Properties()));

	public static final RegistryObject<Item> OVEN_T4_ITEM = ITEMS.register("oven_t4",
			() -> new gregtech6.block.GTComposedNameItem(OVEN_T4.get(), new Item.Properties()));

	/**
	 * The Oven tab walk, in upstream row order (the GTWires/GT6Tools TAB_TABLE form): the
	 * MACHINES_TAB displayItems and the offline parity test share this one table — a row
	 * referencing an unregistered item would crash the displayItems generator at runtime,
	 * a registered item missing here is invisible (no orphans).
	 */
	public static final java.util.List<RegistryObject<Item>> OVEN_TAB_ITEMS =
			java.util.List.of(OVEN_ITEM, OVEN_T2_ITEM, OVEN_T3_ITEM, OVEN_T4_ITEM);

	// ---------------------------------------------------------------------------
	// the Shredder/Crusher/Lathe machine family (task p7-basicmachine-family ②/③, the
	// T2-T4 full ladder added by task p8-machine-tiers-doinject ①) — the registration rows
	// Loader_MultiTileEntities.java:1294-1309: hardness/resistance 7.0F; NBT_INPUT
	// 32/128/512/2048 → the :126 conversion min = in/2, max = in*2 (TIER_INPUTS below);
	// Crusher alone carries NBT_PARALLEL 4/8/16/32 + NBT_PARALLEL_DURATION T
	// (:1300-1303), Shredder/Lathe register no parallel key → 1. ids lowercase (the
	// 1.20.1 ResourceLocation constraint).
	//
	// NBT_ENERGY_ACCEPTED :1294 (Shredder) / :1300 (Crusher) / :1306 (Lathe) — carrier
	// only; RU/KU net supply pending rotor family; live supply = A-tier via supplyEnergy().
	// (The upstream :501 type-equality gate keeps the EU-only network out of the RU/KU
	// rows — changing the carrier to EU is REFUSED: it would misplace :501/:510/:519 and
	// the root TD.java:216 ALL_ALTERNATING = (F, KU) semantics all hang on the real type.)
	// ---------------------------------------------------------------------------

	/**
	 * The tier energy table (upstream NBT_INPUT 32/128/512/2048 through the :126 conversion
	 * min = in/2 / max = in*2): TIER_INPUTS[tier] = {mInputMin, mInput, mInputMax} for
	 * tier 0 (T1, the :98 field defaults) .. tier 3 (T4).
	 *
	 * <p>Naming semantics (task p27-machine-energy-display-fix, constant kept — the RCON/
	 * command/test churn outweighs the rename): a MATERIAL tier ladder, not a voltage one —
	 * the four tier variants ride the Kinetic/Heat_T material words ({@link
	 * #KINETIC_TIER_MAT_SLUGS}, Kinetic_T[1..4] MT.java:3690); voltage names (LV/MV/HV/EV)
	 * belong to the Electric_T motor classes only (the Canner family, :1379-1382).
	 */
	public static final long[][] TIER_INPUTS = {{16, 32, 64}, {64, 128, 256}, {256, 512, 1024}, {1024, 2048, 4096}};

	/**
	 * The ULV voltage window (task p28-c-ulv-machine-ladder) — V[0] = 8 EU × 1 A through the
	 * same :126 conversion the TIER_INPUTS rows ride (min = in/2, max = in*2): mInputMin 4 /
	 * mInput 8 / mInputMax 16. The packet-domain closure (research.p28-r-ulv-tier-design
	 * chain_closure): the 8 EU packet of the FE converter / the ULV Electric Dynamo lands
	 * MID-WINDOW (min 4 ≤ 8 ≤ max 16), while every TIER_INPUTS machine starts at min 16 —
	 * an 8 EU packet is dead below LV, which IS the ULV wall the tier exists to make.
	 * Consumed by the {@link #machineUlv} factory arm off the rows'
	 * {@code ulvVoltage()} marker. Recipe side: the shared RM rows (Wiremill/Sifting/
	 * RollingMill EUt ≤ 16, research.p28-r-ulv-tier-design recipe_eut) run un-overclocked
	 * at the :773 loop (mMinEnergy ≥ mInputMin 4 → the 4x/2x fold never fires) — ULV saves
	 * the operator, not the time.
	 */
	public static final long[] ULV_TIER_INPUTS = {4, 8, 16};

	/** The ULV melting-gate ceiling every ULV row carries — the stone-crucible ceiling (GT6Crucibles.java:86, decisions.p28-ulv-tier-rulings). */
	public static final long ULV_MELTING_GATE_K = 1375L;

	/**
	 * The shared 4/8/16/32 parallel table (the W1 合流互指 ruling, never re-defined): the
	 * upstream NBT_PARALLEL {4, 8, 16, 32} + NBT_PARALLEL_DURATION T shape carried by the
	 * Crusher rows (:1300-1303), the Sifter (:1312-1315) / Compressor (:1343-1346) rows
	 * (task p26-w1-sifter-compressor-wiremill) AND the Press rows (:1425-1428, task
	 * p26-w1-press-extruder-molds) — one constant, every consumer references it. Declared
	 * BEFORE its consumers (the static-initializer order — the illegal-forward-reference
	 * lesson).
	 */
	public static final int[] PARALLEL_4_32 = {4, 8, 16, 32};

	/** The Crusher parallel row (upstream NBT_PARALLEL :1300-1303) — the SAME array as {@link #PARALLEL_4_32}, the W1 merge ruling. */
	public static final int[] CRUSHER_PARALLEL = PARALLEL_4_32;

	// the composed tier-ladder name face (task p20-i18n-compose-rows, materialized by task
	// p27-machine-energy-display-fix): the "{Machine} (Material)" rows compose from the
	// machine word + the Kinetic_T material word — the upstream name column is
	// "Shredder ("+aMat.getLocal()+")" over Kinetic_T[1..4] (:1294-1309, MT.java:3690),
	// NOT an ordinal tier (the voltage names ride the Electric_T motor classes only — the
	// Canner family, :1379-1382)
	public static final String MACHINE_DISPLAY_KEY = "gt6.row.machine.display";
	public static final String MACHINE_SHREDDER_UNIT_KEY = "gt6.row.machine.shredder";
	public static final String MACHINE_CRUSHER_UNIT_KEY = "gt6.row.machine.crusher";
	public static final String MACHINE_LATHE_UNIT_KEY = "gt6.row.machine.lathe";
	// the W1 Kinetic trio unit keys (task p26-w1-sifter-compressor-wiremill, the :101-104
	// shape): the MachineRow carrier fills exactly ONE format slot (the gt6.row.mat unit —
	// the tier rides the material word, Kinetic_T[1..4] = Bronze/Steel/Titanium/
	// Tungstensteel), so the family template rides its unit key's value ("Sifter (%s)" —
	// the CANNER_DISPLAY_KEY one-slot contract, not the two-slot MACHINE_DISPLAY_KEY form).
	public static final String MACHINE_SIFTER_UNIT_KEY = "gt6.row.machine.sifter";
	public static final String MACHINE_COMPRESSOR_UNIT_KEY = "gt6.row.machine.compressor";
	public static final String MACHINE_WIREMILL_UNIT_KEY = "gt6.row.machine.wiremill";
	// the p28-c-ulv-machine-ladder one-slot templates: the W1 trio unit-key shape extended
	// to the three families whose legacy ladders have no row-carrier template face — the
	// Shredder/Crusher ULV rows are row CARRIERS (their T1-T4 siblings stay tierOf), and
	// the row getName() fills exactly ONE slot (the GTBasicMachineBlock :221 compose form),
	// so each needs its own one-slot family template. RollingMill is a new family whose
	// single row rides the trio form directly.
	/** The Shredder family one-slot display template (the ULV row carrier's displayKey; zh 粉碎机, dump gt.multitileentity.20011 form). */
	public static final String MACHINE_SHREDDER_DISPLAY_KEY = "gt6.row.machine.shredder.display";
	/** The Crusher family one-slot display template (the ULV row carrier's displayKey). */
	public static final String MACHINE_CRUSHER_DISPLAY_KEY = "gt6.row.machine.crusher.display";
	/** The Rolling Mill family unit word (the W1 trio :101-104 key form; upstream name column "Rolling Mill ("+aMat.getLocal()+")", Loader:1349-1352). */
	public static final String MACHINE_ROLLING_MILL_UNIT_KEY = "gt6.row.machine.rolling_mill";
	/** The Press family unit word (task p26-w1-press-extruder-molds, the :101-104 key form). */
	public static final String MACHINE_PRESS_UNIT_KEY = "gt6.row.machine.press";
	/** The Extruder family unit word (task p26-w1-press-extruder-molds, T2-T4; the :101-104 key form). */
	public static final String MACHINE_EXTRUDER_UNIT_KEY = "gt6.row.machine.extruder";
	/** The T1 Extruder unit word — the upstream name column differs at T1: "Low Heat Extruder" (:1406) vs "Extruder" (:1407-1409). */
	public static final String MACHINE_EXTRUDER_LOW_HEAT_UNIT_KEY = "gt6.row.machine.extruder_low_heat";
	/** The Press family display template (the row displayKey face, the Dryer/Distillery convention). */
	public static final String PRESS_DISPLAY_KEY = "gt6.row.machine.press.display";
	/** The Extruder family display template (T2-T4 rows). */
	public static final String EXTRUDER_DISPLAY_KEY = "gt6.row.machine.extruder.display";
	/** The T1 Extruder display template ("Low Heat Extruder (Steel)", the :1406 name column). */
	public static final String EXTRUDER_LOW_HEAT_DISPLAY_KEY = "gt6.row.machine.extruder.low_heat.display";

	/**
	 * The Kinetic tier material ladder (upstream MT.java:3690 Kinetic_T[1..4] = Bronze/Steel/
	 * Titanium/Tungstensteel — the name column "Shredder ("+aMat.getLocal()+")", :1294-1309):
	 * the gt6.row.mat small-unit slugs, indexed tier 1..4 (the {@link #tierOf} index + 1).
	 * MATERIAL tiers, not voltage tiers — the four variants pick the Kinetic/Heat_T material
	 * words (this ladder and the Heat_T Dryer/Oven ladders alike); voltage names (LV/MV/HV/
	 * EV) belong to the Electric_T motor classes only (the Canner family, :1379-1382).
	 * T1 keeps the pre-ladder atomic name ("Shredder", block.gt6.shredder), T2-T4 compose
	 * over {@code gt6.row.mat.<slug>} — task p27-machine-energy-display-fix retired the
	 * ordinal gt6.row.tier.* units.
	 */
	public static final String[] KINETIC_TIER_MAT_SLUGS = {"bronze", "steel", "titanium", "tungstensteel"};

	/** The pre-composed name supplier of a tier block (the 4-arg GTBasicMachineBlock carrier): machine word + the tier's Kinetic material word. */
	private static java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> tierName(
			String aMachineUnitKey, int aTier) {
		return () -> net.minecraft.network.chat.Component.translatable(MACHINE_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(aMachineUnitKey),
				net.minecraft.network.chat.Component.translatable("gt6.row.mat." + KINETIC_TIER_MAT_SLUGS[aTier - 1]));
	}

	public static final RegistryObject<Block> SHREDDER = BLOCKS.register("shredder",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, null, KINETIC_T_LADDER.get(0)));

	public static final RegistryObject<Block> SHREDDER_T2 = BLOCKS.register("shredder_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, tierName(MACHINE_SHREDDER_UNIT_KEY, 2), KINETIC_T_LADDER.get(1)));

	public static final RegistryObject<Block> SHREDDER_T3 = BLOCKS.register("shredder_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, tierName(MACHINE_SHREDDER_UNIT_KEY, 3), KINETIC_T_LADDER.get(2)));

	public static final RegistryObject<Block> SHREDDER_T4 = BLOCKS.register("shredder_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.SHREDDER_BE.get(), null, tierName(MACHINE_SHREDDER_UNIT_KEY, 4), KINETIC_T_LADDER.get(3)));

	public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, null, KINETIC_T_LADDER.get(0)));

	public static final RegistryObject<Block> CRUSHER_T2 = BLOCKS.register("crusher_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, tierName(MACHINE_CRUSHER_UNIT_KEY, 2), KINETIC_T_LADDER.get(1)));

	public static final RegistryObject<Block> CRUSHER_T3 = BLOCKS.register("crusher_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, tierName(MACHINE_CRUSHER_UNIT_KEY, 3), KINETIC_T_LADDER.get(2)));

	public static final RegistryObject<Block> CRUSHER_T4 = BLOCKS.register("crusher_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.CRUSHER_BE.get(), null, tierName(MACHINE_CRUSHER_UNIT_KEY, 4), KINETIC_T_LADDER.get(3)));

	public static final RegistryObject<Block> LATHE = BLOCKS.register("lathe",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(7.0F, 7.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, null, KINETIC_T_LADDER.get(0)));

	public static final RegistryObject<Block> LATHE_T2 = BLOCKS.register("lathe_t2",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, tierName(MACHINE_LATHE_UNIT_KEY, 2), KINETIC_T_LADDER.get(1)));

	public static final RegistryObject<Block> LATHE_T3 = BLOCKS.register("lathe_t3",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(9.0F, 9.0F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, tierName(MACHINE_LATHE_UNIT_KEY, 3), KINETIC_T_LADDER.get(2)));

	public static final RegistryObject<Block> LATHE_T4 = BLOCKS.register("lathe_t4",
			() -> new GTBasicMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(12.5F, 12.5F).sound(SoundType.METAL), () -> GTMachines.LATHE_BE.get(), null, tierName(MACHINE_LATHE_UNIT_KEY, 4), KINETIC_T_LADDER.get(3)));

	/**
	 * One BET per machine FAMILY (task p8-machine-tiers-doinject ①, the P6 barrel-ladder
	 * precedent): the class and the configuration factory are shared, the validBlocks set
	 * multi-attaches the four tier blocks T1-T4 (Builder.of varargs), and the factory reads
	 * the tier off the placed BlockState (the tier rows are compile-time constants upstream
	 * — NBT_INPUT :1294-1309 — so the block identity IS the config selector). The RecipeMap
	 * is read at BE creation time (the volatile survives {@link GT6RecipeMaps#reset()} test
	 * generations); the energy-type carrier is assigned per family (:1294/:1300/:1306
	 * NBT_ENERGY_ACCEPTED). The self-references are class-qualified on purpose — a
	 * simple-name capture inside the field's own initializer is a javac initialization-loop
	 * error.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SHREDDER_BE =
			BLOCK_ENTITY_TYPES.register("shredder", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> {
						// task p28-c-ulv-machine-ladder: the ULV row carrier (the ONLY row
						// this family carries) routes through the machineUlv arm — the
						// T1-T4 tier blocks keep the tierOf dispatch below, byte-identical
						GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
						if (tRow != null) return machineUlv(GTMachines.SHREDDER_BE.get(), aPos, aState, tRow);
						return machine(GTMachines.SHREDDER_BE.get(), aPos, aState, GT6RecipeMaps.SHREDDER, 1, false,
								TD.Energy.RU, tierOf(aState.getBlock(), GTMachines.SHREDDER, GTMachines.SHREDDER_T2, GTMachines.SHREDDER_T3, GTMachines.SHREDDER_T4),
								null /*ModularUI family — no vanilla MenuType (p26-mui-a-menu-deregistration)*/);
					},
					SHREDDER.get(), SHREDDER_T2.get(), SHREDDER_T3.get(), SHREDDER_T4.get(),
					GTMachines.SHREDDER_ULV.get() /*the p28 ULV row, the qualified forward-reference form (the P6 lambda lesson)*/).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CRUSHER_BE =
			BLOCK_ENTITY_TYPES.register("crusher", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> {
						// the p28 ULV row carrier arm — the shredder branch shape verbatim
						GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
						if (tRow != null) return machineUlv(GTMachines.CRUSHER_BE.get(), aPos, aState, tRow);
						return machine(GTMachines.CRUSHER_BE.get(), aPos, aState, GT6RecipeMaps.CRUSHER,
								CRUSHER_PARALLEL[tierOf(aState.getBlock(), GTMachines.CRUSHER, GTMachines.CRUSHER_T2, GTMachines.CRUSHER_T3, GTMachines.CRUSHER_T4)], true,
								TD.Energy.KU, tierOf(aState.getBlock(), GTMachines.CRUSHER, GTMachines.CRUSHER_T2, GTMachines.CRUSHER_T3, GTMachines.CRUSHER_T4),
								null /*ModularUI family — no vanilla MenuType (p26-mui-a-menu-deregistration)*/);
					},
					CRUSHER.get(), CRUSHER_T2.get(), CRUSHER_T3.get(), CRUSHER_T4.get(),
					GTMachines.CRUSHER_ULV.get() /*the p28 ULV row*/).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LATHE_BE =
			BLOCK_ENTITY_TYPES.register("lathe", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> machine(GTMachines.LATHE_BE.get(), aPos, aState, GT6RecipeMaps.LATHE, 1, false,
							TD.Energy.RU, tierOf(aState.getBlock(), GTMachines.LATHE, GTMachines.LATHE_T2, GTMachines.LATHE_T3, GTMachines.LATHE_T4),
							null /*ModularUI family — no vanilla MenuType (p26-mui-a-menu-deregistration)*/),
					LATHE.get(), LATHE_T2.get(), LATHE_T3.get(), LATHE_T4.get()).build(null));

	public static final RegistryObject<Item> SHREDDER_ITEM = ITEMS.register("shredder",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T2_ITEM = ITEMS.register("shredder_t2",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T3_ITEM = ITEMS.register("shredder_t3",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> SHREDDER_T4_ITEM = ITEMS.register("shredder_t4",
			() -> new gregtech6.block.GTComposedNameItem(SHREDDER_T4.get(), new Item.Properties()));

	public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T2_ITEM = ITEMS.register("crusher_t2",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T3_ITEM = ITEMS.register("crusher_t3",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> CRUSHER_T4_ITEM = ITEMS.register("crusher_t4",
			() -> new gregtech6.block.GTComposedNameItem(CRUSHER_T4.get(), new Item.Properties()));

	public static final RegistryObject<Item> LATHE_ITEM = ITEMS.register("lathe",
			() -> new gregtech6.block.GTComposedNameItem(LATHE.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T2_ITEM = ITEMS.register("lathe_t2",
			() -> new gregtech6.block.GTComposedNameItem(LATHE_T2.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T3_ITEM = ITEMS.register("lathe_t3",
			() -> new gregtech6.block.GTComposedNameItem(LATHE_T3.get(), new Item.Properties()));
	public static final RegistryObject<Item> LATHE_T4_ITEM = ITEMS.register("lathe_t4",
			() -> new gregtech6.block.GTComposedNameItem(LATHE_T4.get(), new Item.Properties()));

	// ---------------------------------------------------------------------------
	// the Dryer family (task p14-dryer-family) — the four rows
	// Loader_MultiTileEntities.java:1476-1480 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "dryer", TD.Energy.HU, RM.Drying, NBT_CHEAP_OVERCLOCKING T,
	// NBT_PARALLEL_DURATION T). ONE family BET over the four tier blocks — the tier
	// config is the MachineRow carried by the placed block (the GT6Boilers row-record
	// precedent), not a tierOf dispatch. The connectivity masks (CS.java:612 SBIT
	// values, the GTBasicMachineBlock copies): energy = SBIT_D|SBIT_A (the :151 read
	// ORs SBIT_A onto NBT_ENERGY_ACCEPTED_SIDES), tank in = SBIT_B|SBIT_L|SBIT_A (the
	// :143 read), tank out = SBIT_U|SBIT_A (the :144 read), item in/out the same
	// :137/:138 columns (data-only — the item-face gate rides the item-IO pool); the
	// four auto sides are the :139/:140/:145/:146 columns (the auto-IO pool, data-only).
	// ---------------------------------------------------------------------------

	/** The Dryer family display template key ({@code gt6.row.dryer.display}, task p20-i18n-compose-rows). */
	public static final String DRYER_DISPLAY_KEY = "gt6.row.dryer.display";

	/** The four Dryer rows, upstream line order :1477-1480 (T1-T4). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> DRYER_ROWS = java.util.List.of(
			dryer("dryer"  , "steel"           , "Steel"           , 20311,  6.0F, 0,   8),
			dryer("dryer_t2", "invar"           , "Invar"           , 20312,  4.0F, 1,  16),
			dryer("dryer_t3", "titanium"        , "Titanium"        , 20313,  9.0F, 2,  32),
			dryer("dryer_t4", "tungsten_carbide", "Tungsten Carbide", 20314, 12.5F, 3, 64));

	/**
	 * One row factory — the four Dryer columns that differ (path/name/id/material/hardness/
	 * tier/parallel) plus the seven that are family constants (RM.Drying through the
	 * supplier, HU, "dryer" texture, the masks, the auto sides, cheap overclocking T) and
	 * the {@code gt6:dryer} MenuType through the supplier (task p16-machine-fluid-gui ① —
	 * the p14 row.menu pool promise redeemed; the supplier form survives the deferred
	 * registration, the BE reads it lazily at createMenu time).
	 */
	private static GTBasicMachineBlock.MachineRow dryer(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, HEAT_T_LADDER.get(aTier), DRYER_DISPLAY_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.DRYING, TD.Energy.HU, "dryer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
				(byte)5 /*NBT_TANK_SIDE_AUTO_IN SIDE_BACK*/, (byte)1 /*NBT_TANK_SIDE_AUTO_OUT SIDE_TOP*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				GTBasicMachinesMenus.DRYER_MENU::get /*gt6:dryer — the GUI pool card redeemed (p16-machine-fluid-gui ①)*/, true /*NBT_CHEAP_OVERCLOCKING T*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** The registered Dryer blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> DRYER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Dryer items, same keys as {@link #DRYER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> DRYER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : DRYER_ROWS) {
			DRYER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.DRYER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			DRYER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Dryer block list in registration order (the loot/datagen walkers). */
	public static Block[] dryerBlockArray() {
		Block[] rBlocks = new Block[DRYER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : DRYER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine dryer — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block dryerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = DRYER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Dryer family BET: the class and the configuration factory are shared, the
	 * validBlocks set multi-attaches the four tier blocks T1-T4 (the p8 ladder shape), and
	 * the factory reads the row off the placed BlockState's block — the block identity IS
	 * the config carrier here (the row record replaced the tierOf dispatch).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> DRYER_BE =
			BLOCK_ENTITY_TYPES.register("dryer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> dryerMachine(GTMachines.DRYER_BE.get(), aPos, aState),
					dryerBlockArray()).build(null));

	/**
	 * The Dryer BET factory body: the row's parallel/duration/energy-type/tier-input half
	 * rides the shared {@link #machine} helper (the menu travels as the row's
	 * {@code gt6:dryer} supplier, task p16-machine-fluid-gui ①), then the W1a carrier
	 * assignment lands the row's connectivity masks directly on the BE
	 * (mEnergyInputs/mFluidInputs/mFluidOutputs — the :511/:566/:575 gate geometry;
	 * the default 127 zero-regression stays proven for the legacy families).
	 */
	private static TileEntityBasicMachine dryerMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/**
	 * The row→BE mask assignment (public — the offline row test drives it against the
	 * fixture machine; the BET factory calls it right after the {@link #machine} half).
	 * Task p28-c-ulv-machine-ladder adds the melting-gate column: the row's
	 * {@code maxMeltingPointK} rides onto {@code mMaxMeltingPointK} with the masks
	 * (null = no gate, the every-legacy-row shape — the gate arms only on the ULV rows).
	 */
	public static TileEntityBasicMachine applyRow(TileEntityBasicMachine aMachine, GTBasicMachineBlock.MachineRow aRow) {
		aMachine.mEnergyInputs = aRow.energySides();
		aMachine.mFluidInputs = aRow.fluidIn();
		aMachine.mFluidOutputs = aRow.fluidOut();
		aMachine.mMaxMeltingPointK = aRow.maxMeltingPointK();
		return aMachine;
	}

	// ---------------------------------------------------------------------------
	// the Canner family (task p24-canner-machine) — the four rows
	// Loader_MultiTileEntities.java:1379-1382 (aClass = MultiTileEntityBasicMachineElectric,
	// NBT_TEXTURE "canner", TD.Energy.EU, RM.Canner, NBT_USE_OUTPUT_TANK T, NBT_TANK_CAPACITY
	// 128000/512000/2048000/8192000, no NBT_PARALLEL → 1, no NBT_PARALLEL_DURATION → F).
	// ONE family BET over the four tier blocks — the DRYER_ROWS MachineRow shape, with the
	// tank-capacity and use-output-tank columns riding the {@link #cannerMachine} factory
	// (the row record is out of this card's FILES_SCOPE, so the two extra columns are
	// tier-indexed constants here, the TIER_INPUTS carrier form). The T5 row (:1383,
	// NBT_INPUT 8192 / 32768000) STAYS POOLED — the R2 ruling: this repo has no 5-tier
	// MachineRow precedent, the 5th tier unlocks with the first 5-tier family.
	//
	// The connectivity masks (:1379 verbatim): energy = SBIT_B (the :151 read ORs SBIT_A),
	// item+tank in = SBIT_U|SBIT_L (NBT_INV_SIDE_IN == NBT_TANK_SIDE_IN SBIT_U|SBIT_L, the
	// :137/:143 reads), item+tank out = SBIT_R|SBIT_D (the :138/:144 reads), tank auto in =
	// SIDE_TOP(1), tank auto out = SIDE_BOTTOM(0), item auto in = SIDE_LEFT(2), item auto
	// out = SIDE_RIGHT(4) (the auto-IO pool, data-only). Display name = the VN voltage
	// ladder (upstream "Canning Machine ("+VN[tier]+")", CS.java:154 LV/MV/HV/EV) — NOT a
	// material name like the Heat_T families, so the row mat slugs ARE the voltage ids.
	// ---------------------------------------------------------------------------

	/** The Canner family display template key ({@code gt6.row.canner.display}). */
	public static final String CANNER_DISPLAY_KEY = "gt6.row.canner.display";

	/** The registration-row tank capacities (NBT_TANK_CAPACITY :1379-1382, mB — the mMaxFluid*Size 128000 map ceiling folded into T1, the R6 ruling). */
	public static final long[] CANNER_TANK_CAPACITY = {128000L, 512000L, 2048000L, 8192000L};

	/** The four Canner rows, upstream line order :1379-1382 (T1-T4, the VN ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CANNER_ROWS = java.util.List.of(
			canner("canner"   , "lv", "LV", 20161,  4.0F, 0),
			canner("canner_t2", "mv", "MV", 20162,  4.0F, 1),
			canner("canner_t3", "hv", "HV", 20163,  4.0F, 2),
			canner("canner_t4", "ev", "EV", 20164,  4.0F, 3));

	/**
	 * One row factory — the Canner columns that differ (path/name/id/metaId/hardness/tier)
	 * plus the family constants: EU (NBT_ENERGY_ACCEPTED), the "canner" texture, the :1379
	 * masks and auto sides, parallel 1 / parallelDuration F (no NBT keys), cheap
	 * overclocking T (the :773 loop runs unconditionally in the port) and the
	 * {@code gt6:canner} menu supplier.
	 */
	private static GTBasicMachineBlock.MachineRow canner(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, ELECTRIC_T_LADDER.get(aTier), CANNER_DISPLAY_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.CANNER, TD.Energy.EU, "canner",
				(byte)(GTBasicMachineBlock.SBIT_B) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				GTBasicMachinesMenus.CANNER_MENU::get /*gt6:canner*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** The registered Canner blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> CANNER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Canner items, same keys as {@link #CANNER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> CANNER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : CANNER_ROWS) {
			CANNER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.CANNER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			CANNER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Canner block list in registration order (the loot/datagen walkers). */
	public static Block[] cannerBlockArray() {
		Block[] rBlocks = new Block[CANNER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : CANNER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine canner — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block cannerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = CANNER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// ---------------------------------------------------------------------------
	// the Press family (task p26-w1-press-extruder-molds) — the four rows
	// Loader_MultiTileEntities.java:1425-1428 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "press", TD.Energy.KU, RM.Press, NBT_PARALLEL 4/8/16/32 +
	// NBT_PARALLEL_DURATION T, no tank keys — the map is 0/0/0 fluids). ONE family BET
	// over the four tier blocks — the CANNER_ROWS MachineRow shape. Connectivity masks
	// (:1425 verbatim, the :137/:138/:151 reads OR SBIT_A): energy = SBIT_U|SBIT_A
	// (NBT_ENERGY_ACCEPTED_SIDES SBIT_U), item in = SBIT_L|SBIT_A (NBT_INV_SIDE_IN
	// SBIT_L), item out = SBIT_R|SBIT_A (NBT_INV_SIDE_OUT SBIT_R), item auto in =
	// SIDE_LEFT(2), item auto out = SIDE_RIGHT(4); the fluid masks ride 0 (no NBT_TANK
	// keys upstream — the zero-fluid face, data-only). The GUI clause: menu = null (the
	// menu-less carrier, the Distillery precedent — zero new gt6:* MenuType, the use()
	// gate stays inert until the seam-① micro card lands the MUI dispatch).
	// ---------------------------------------------------------------------------

	/** The four Press rows, upstream line order :1425-1428 (T1-T4, the Kinetic_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> PRESS_ROWS = java.util.List.of(
			press("press"   , "steel"           , "Steel"           , 20231,  7.0F, 0),
			press("press_t2", "invar"           , "Invar"           , 20232,  6.0F, 1),
			press("press_t3", "titanium"        , "Titanium"        , 20233,  9.0F, 2),
			press("press_t4", "tungsten_carbide", "Tungsten Carbide", 20234, 12.5F, 3));

	/**
	 * One row factory — the Canner shape: family constants KU / "press" texture /
	 * PARALLEL_4_32 (the :1425-1428 NBT_PARALLEL column, the shared ladder) /
	 * parallelDuration T / the SBIT_U|SBIT_A energy face / the :1425 item masks / zero
	 * fluid masks / the null menu supplier (the GUI clause) / cheap overclocking T.
	 */
	private static GTBasicMachineBlock.MachineRow press(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), PRESS_DISPLAY_KEY, aMetaId, aHardness, aTier,
				PARALLEL_4_32[aTier], true,
				() -> GT6RecipeMaps.PRESS, TD.Energy.KU, "press",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U, the :151 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_IN — the zero-fluid face*/,
				(byte)0 /*no NBT_TANK_SIDE_OUT*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L, the :137 OR*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R, the :138 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_AUTO_IN*/, (byte)0 /*no NBT_TANK_SIDE_AUTO_OUT*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the GUI clause: zero new gt6:* MenuType*/, true, null, false);
	}

	/** The registered Press blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> PRESS_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Press items, same keys as {@link #PRESS_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> PRESS_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : PRESS_ROWS) {
			PRESS_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.PRESS_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			PRESS_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.PRESS_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Press block list in registration order (the loot/datagen walkers). */
	public static Block[] pressBlockArray() {
		Block[] rBlocks = new Block[PRESS_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : PRESS_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine press — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block pressBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = PRESS_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Press family BET: the Canner shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> PRESS_BE =
			BLOCK_ENTITY_TYPES.register("press", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> pressMachine(GTMachines.PRESS_BE.get(), aPos, aState),
					pressBlockArray()).build(null));

	/** The Press BET factory body — the dryerMachine body verbatim over the Press rows. */
	private static TileEntityBasicMachine pressMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	// ---------------------------------------------------------------------------
	// the Extruder family (task p26-w1-press-extruder-molds) — the four rows
	// Loader_MultiTileEntities.java:1406-1409 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "extruder", TD.Energy.HU, RM.Extruder, no parallel key → 1, no
	// NBT_PARALLEL_DURATION → F, no tank keys). ONE family BET over the four tier
	// blocks. Connectivity masks (:1406 verbatim, the :137/:138/:151 reads OR SBIT_A):
	// energy = SBIT_D|SBIT_A (NBT_ENERGY_ACCEPTED_SIDES SBIT_D), item in = SBIT_L|SBIT_A,
	// item out = SBIT_R|SBIT_A, item auto in = SIDE_LEFT(2), auto out = SIDE_RIGHT(4);
	// the fluid masks ride 0 (the zero-fluid face). The T1 name column DIFFERS upstream
	// ("Low Heat Extruder" :1406 vs "Extruder" :1407-1409) — the T1 row carries its own
	// display template. menu = null (the GUI clause, the Press ruling).
	// ---------------------------------------------------------------------------

	/** The four Extruder rows, upstream line order :1406-1409 (T1-T4, the Heat_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> EXTRUDER_ROWS = java.util.List.of(
			extruder("extruder"   , "steel"           , "Steel"           , 20201,  6.0F, 0, EXTRUDER_LOW_HEAT_DISPLAY_KEY),
			extruder("extruder_t2", "invar"           , "Invar"           , 20202,  4.0F, 1, EXTRUDER_DISPLAY_KEY),
			extruder("extruder_t3", "titanium"        , "Titanium"        , 20203,  9.0F, 2, EXTRUDER_DISPLAY_KEY),
			extruder("extruder_t4", "tungsten_carbide", "Tungsten Carbide", 20204, 12.5F, 3, EXTRUDER_DISPLAY_KEY));

	/**
	 * One row factory — the Press shape with the extruder columns: HU / the "extruder"
	 * texture / parallel 1 + duration F (no NBT keys) / the SBIT_D|SBIT_A energy face /
	 * the :1406 item masks / zero fluid masks / the per-row display template (the T1
	 * Low Heat face) / the null menu supplier (the GUI clause).
	 */
	private static GTBasicMachineBlock.MachineRow extruder(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, String aDisplayKey) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, HEAT_T_LADDER.get(aTier), aDisplayKey, aMetaId, aHardness, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1406-1409*/,
				() -> GT6RecipeMaps.EXTRUDER, TD.Energy.HU, "extruder",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D, the :151 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_IN — the zero-fluid face*/,
				(byte)0 /*no NBT_TANK_SIDE_OUT*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L, the :137 OR*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R, the :138 OR*/,
				(byte)0 /*no NBT_TANK_SIDE_AUTO_IN*/, (byte)0 /*no NBT_TANK_SIDE_AUTO_OUT*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the GUI clause: zero new gt6:* MenuType*/, true, null, false);
	}

	/** The registered Extruder blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> EXTRUDER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Extruder items, same keys as {@link #EXTRUDER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> EXTRUDER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : EXTRUDER_ROWS) {
			EXTRUDER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.EXTRUDER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			EXTRUDER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.EXTRUDER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Extruder block list in registration order (the loot/datagen walkers). */
	public static Block[] extruderBlockArray() {
		Block[] rBlocks = new Block[EXTRUDER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : EXTRUDER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine extruder — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block extruderBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = EXTRUDER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Extruder family BET: the Press shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> EXTRUDER_BE =
			BLOCK_ENTITY_TYPES.register("extruder", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> extruderMachine(GTMachines.EXTRUDER_BE.get(), aPos, aState),
					extruderBlockArray()).build(null));

	/** The Extruder BET factory body — the dryerMachine body verbatim over the Extruder rows. */
	private static TileEntityBasicMachine extruderMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/**
	 * The ONE Canner family BET: the Dryer shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CANNER_BE =
			BLOCK_ENTITY_TYPES.register("canner", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> cannerMachine(GTMachines.CANNER_BE.get(), aPos, aState),
					cannerBlockArray()).build(null));

	/**
	 * The Canner BET factory body — the dryerMachine body plus the two extra registration
	 * columns the MachineRow record does not carry: NBT_USE_OUTPUT_TANK T (the mCanUseOutputTanks
	 * fallback, upstream :132) and NBT_TANK_CAPACITY (:1379-1382, the tier-indexed
	 * {@link #CANNER_TANK_CAPACITY} through {@link TileEntityBasicMachine#applyTankCapacity}).
	 */
	private static TileEntityBasicMachine cannerMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		if (tRow.ulvVoltage()) { // the p28 ULV row — the voltage window arm, then the shared canner extras
			TileEntityBasicMachine tUlv = machineUlv(aType, aPos, aState, tRow);
			tUlv.mCanUseOutputTanks = true; // NBT_USE_OUTPUT_TANK T (:1379)
			tUlv.mTankCapacity = CANNER_TANK_CAPACITY[tRow.tier()]; // the T0 rung rides the T1 column (128000 — the smallest tank)
			tUlv.applyTankCapacity(); // :157-160 — the tanks are re-armed AT the row capacity
			return tUlv;
		}
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		applyRow(tMachine, tRow);
		tMachine.mCanUseOutputTanks = true; // NBT_USE_OUTPUT_TANK T (:1379)
		tMachine.mTankCapacity = CANNER_TANK_CAPACITY[tRow.tier()]; // NBT_TANK_CAPACITY (:1379-1382)
		tMachine.applyTankCapacity(); // :157-160 — the tanks are re-armed AT the row capacity
		return tMachine;
	}

	// ---------------------------------------------------------------------------
	// the W1 Kinetic trio — Sifter (KU, :1312-1315), Compressor (KU, :1343-1346), Wiremill
	// (RU, :1373-1376), all MultiTileEntityBasicMachine with MT.DATA.Kinetic_T[1..4]
	// (Bronze / ANY.Steel / Ti / TungstenSteel — the tier-material word; the port local
	// "Tungstensteel" per the GT6Kinetics convention), NBT_INPUT 32/128/512/2048 through
	// the TIER_INPUTS conversion, NBT_TEXTURE "sifter"/"compressor"/"wiremill", hardness
	// 7.0/6.0/9.0/12.5 (NBT_RESISTANCE == hardness). Sifter and Compressor carry
	// NBT_PARALLEL {4, 8, 16, 32} + NBT_PARALLEL_DURATION T — the PARALLEL_4_32 shared
	// constant; the Wiremill rows carry no NBT_PARALLEL key → 1 (the :97 ruling shape).
	// ONE family BET per family over the four tier blocks — the DRYER_ROWS MachineRow
	// carrier shape (the block identity is the config carrier; the factory is the
	// dryerMachine body verbatim). The connectivity masks, upstream-verbatim: Sifter and
	// Compressor take item in over the top (NBT_INV_SIDE_IN SBIT_U, NBT_INV_SIDE_AUTO_IN
	// SIDE_TOP) and emit over the bottom (NBT_INV_SIDE_OUT SBIT_D, AUTO_OUT SIDE_BOTTOM),
	// the Wiremill takes left and emits right (SBIT_L / SIDE_LEFT / SBIT_R / SIDE_RIGHT,
	// the :137/:138 column shape); energy = SBIT_B (Sifter/Wiremill) / SBIT_L (Compressor)
	// through NBT_ENERGY_ACCEPTED_SIDES (the :151 read ORs SBIT_A onto every mask — the
	// row bytes carry the post-read values, the dryer-row convention). No NBT_TANK_SIDE_*
	// keys on any of the twelve rows → the upstream field defaults 127 (the all-sides
	// zero-regression face, TileEntityBasicMachine :245/:786) with SIDE_UNDEFINED auto
	// sides — zero fluid recipes are NOT a zero fluid face (the Shredder precedent).
	// menu = null on every row: ZERO new gt6:* MenuType (the card GUI clause — use()
	// stays inert until the menu==null dispatch seam goes live).
	// ---------------------------------------------------------------------------

	/** The four Sifter rows, upstream line order :1312-1315 (T1-T4, the Kinetic_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SIFTER_ROWS = java.util.List.of(
			sifter("sifter"   , "bronze"       , "Bronze"       , 20051,  7.0F, 0, PARALLEL_4_32[0]),
			sifter("sifter_t2", "steel"        , "Steel"        , 20052,  6.0F, 1, PARALLEL_4_32[1]),
			sifter("sifter_t3", "titanium"     , "Titanium"     , 20053,  9.0F, 2, PARALLEL_4_32[2]),
			sifter("sifter_t4", "tungstensteel", "Tungstensteel", 20054, 12.5F, 3, PARALLEL_4_32[3]));

	/** The four Compressor rows, upstream line order :1343-1346 (T1-T4, the Kinetic_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> COMPRESSOR_ROWS = java.util.List.of(
			compressor("compressor"   , "bronze"       , "Bronze"       , 20101,  7.0F, 0, PARALLEL_4_32[0]),
			compressor("compressor_t2", "steel"        , "Steel"        , 20102,  6.0F, 1, PARALLEL_4_32[1]),
			compressor("compressor_t3", "titanium"     , "Titanium"     , 20103,  9.0F, 2, PARALLEL_4_32[2]),
			compressor("compressor_t4", "tungstensteel", "Tungstensteel", 20104, 12.5F, 3, PARALLEL_4_32[3]));

	/** The four Wiremill rows, upstream line order :1373-1376 (T1-T4, the Kinetic_T ladder; no NBT_PARALLEL → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> WIREMILL_ROWS = java.util.List.of(
			wiremill("wiremill"   , "bronze"       , "Bronze"       , 20151,  7.0F, 0),
			wiremill("wiremill_t2", "steel"        , "Steel"        , 20152,  6.0F, 1),
			wiremill("wiremill_t3", "titanium"     , "Titanium"     , 20153,  9.0F, 2),
			wiremill("wiremill_t4", "tungstensteel", "Tungstensteel", 20154, 12.5F, 3));

	/**
	 * One Sifter row factory — the differing columns (path/name/id/material/hardness/tier/
	 * parallel) plus the family constants: RM.Sifting through the supplier, KU, the
	 * "sifter" texture, the :1312 masks and auto sides, parallelDuration T (NBT_PARALLEL_
	 * DURATION), cheap overclocking T (the :773 loop runs unconditionally in the port) and
	 * the null menu supplier (the zero-new-MenuType GUI clause).
	 */
	private static GTBasicMachineBlock.MachineRow sifter(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_SIFTER_UNIT_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.SIFTING, TD.Energy.KU, "sifter",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B, the :151 read ORs SBIT_A*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U, the :137 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D*/,
				(byte)-1 /*no NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				null /*the menu-less carrier — zero new gt6:* MenuType (the card GUI clause)*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** One Compressor row factory — the sifter shape verbatim over the :1343 masks (energy SBIT_L) and RM.Compressor/KU. */
	private static GTBasicMachineBlock.MachineRow compressor(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_COMPRESSOR_UNIT_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.COMPRESSOR, TD.Energy.KU, "compressor",
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_L*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** One Wiremill row factory — the :1373 masks (left in / right out, energy SBIT_B), RM.Wiremill/RU, NO parallel key → 1. */
	private static GTBasicMachineBlock.MachineRow wiremill(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_WIREMILL_UNIT_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.WIREMILL, TD.Energy.RU, "wiremill",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** The registered Sifter blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> SIFTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Sifter items, same keys as {@link #SIFTER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> SIFTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Compressor blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> COMPRESSOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Compressor items, same keys as {@link #COMPRESSOR_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> COMPRESSOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Wiremill blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> WIREMILL_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Wiremill items, same keys as {@link #WIREMILL_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> WIREMILL_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : SIFTER_ROWS) {
			SIFTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SIFTER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			SIFTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : COMPRESSOR_ROWS) {
			COMPRESSOR_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.COMPRESSOR_BE.get(), tRow)));
			COMPRESSOR_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.COMPRESSOR_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : WIREMILL_ROWS) {
			WIREMILL_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.WIREMILL_BE.get(), tRow)));
			WIREMILL_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.WIREMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Sifter block list in registration order (the loot/datagen walkers). */
	public static Block[] sifterBlockArray() {
		Block[] rBlocks = new Block[SIFTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SIFTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Compressor block list in registration order (the loot/datagen walkers). */
	public static Block[] compressorBlockArray() {
		Block[] rBlocks = new Block[COMPRESSOR_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : COMPRESSOR_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Wiremill block list in registration order (the loot/datagen walkers). */
	public static Block[] wiremillBlockArray() {
		Block[] rBlocks = new Block[WIREMILL_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : WIREMILL_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine sifter — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block sifterBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = SIFTER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine compressor — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block compressorBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = COMPRESSOR_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine wiremill — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block wiremillBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = WIREMILL_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Sifter family BET: the Dryer shape verbatim — the shared factory, the four
	 * tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SIFTER_BE =
			BLOCK_ENTITY_TYPES.register("sifter", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.SIFTER_BE.get(), aPos, aState),
					sifterBlockArray()).build(null));

	/**
	 * The ONE Compressor family BET: the Dryer shape verbatim. The registry id
	 * "compressor" is free — the legacy p7/p8 family is the CRUSHER (gt6:crusher), a
	 * different machine with a different recipe map.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> COMPRESSOR_BE =
			BLOCK_ENTITY_TYPES.register("compressor", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.COMPRESSOR_BE.get(), aPos, aState),
					compressorBlockArray()).build(null));

	/** The ONE Wiremill family BET: the Dryer shape verbatim. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> WIREMILL_BE =
			BLOCK_ENTITY_TYPES.register("wiremill", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.WIREMILL_BE.get(), aPos, aState),
					wiremillBlockArray()).build(null));

	/**
	 * The W1-trio BET factory body — the dryerMachine body verbatim (the row carries every
	 * column the three families need; no extra registration columns, unlike the Canner).
	 * Shared by all three BETs (the row drives the recipe map, energy type, parallel and
	 * masks; only the BlockEntityType argument differs). Task p28-c-ulv-machine-ladder adds
	 * the ULV arm: a row with the {@code ulvVoltage} marker routes through
	 * {@link #machineUlv} (the {4, 8, 16} window), the T1-T4 rows keep the TIER_INPUTS
	 * assignment byte-identical.
	 */
	private static TileEntityBasicMachine kineticMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		if (tRow.ulvVoltage()) return machineUlv(aType, aPos, aState, tRow); // the p28 ULV row
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/**
	 * The ULV row→BE factory arm (task p28-c-ulv-machine-ladder): the {@link #machine}
	 * half verbatim, then the V[0] window {@link #ULV_TIER_INPUTS} = {4, 8, 16} overrides
	 * the TIER_INPUTS[tier] assignment (the row's ulvVoltage marker is the selector — the
	 * upstream NBT_INPUT column the port folds into tier cannot say "8" for a tier-0 row).
	 * The melting-gate column rides applyRow like the masks.
	 */
	private static TileEntityBasicMachine machineUlv(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState, GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, aRow.recipes().get(), aRow.parallel(), aRow.parallelDuration(), aRow.energyType(), aRow.tier(), aRow.menu());
		tMachine.mInputMin = ULV_TIER_INPUTS[0];
		tMachine.mInput = ULV_TIER_INPUTS[1];
		tMachine.mInputMax = ULV_TIER_INPUTS[2];
		return applyRow(tMachine, aRow);
	}

	// ---------------------------------------------------------------------------
	// the p28-c-ulv-machine-ladder ULV machine ladder — the six V[0] = 8 EU x 1 A rows
	// (min 4 / in 8 / max 16, every row gated at 1375 K). NO upstream VN[0] machine
	// exists (research.p28-r-ulv-tier-design upstream_census: only the Transformer :881
	// and the two Battery rows :1009/:1033) — the whole ladder is the declared-deviation
	// tier extension, energy EU on every row (the EU-variant precedent: ElectricMixer
	// :1504-1508 / ElectricLoom :1511-1515 / ElectricSifter :1518-1522 are the upstream
	// EU-variant forms of KU/RU machines). The five extension rows ride the SAME family
	// BETs as their T1-T4 siblings; RollingMill is the one NEW family. Material columns =
	// the T0 rungs (KINETIC_T0 = ANY.Wood / ELECTRIC_T0 = TinAlloy, MT.java:3690-3691
	// index 0 — the card's "T0 对应" arm; the T0 rung exists upstream but no row uses it).
	// Connectivity masks = each family's upstream row verbatim + the ORed SBIT_A.
	// metaIds: no upstream ULV row exists — family base + 5 (the free id after T4,
	// data-only yardstick, the deviation documented on each row).
	// ---------------------------------------------------------------------------

	/** The Canner ULV row (Electric_T[0] = TinAlloy material, the VN[0] = "ULV" word — CS.java:154 — riding the Canner name column form). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CANNER_ULV_ROWS = java.util.List.of(cannerUlv());

	/** The Shredder ULV row (Kinetic_T[0] = ANY.Wood; masks = the :1294 row verbatim — item top-in/bottom-out, energy left). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SHREDDER_ULV_ROWS = java.util.List.of(
			ulvKinetic("shredder_ulv", 20015, 7.0F, KINETIC_T0, MACHINE_SHREDDER_DISPLAY_KEY,
					() -> GT6RecipeMaps.SHREDDER, TD.Energy.EU, "shredder",
					(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_L :1294*/,
					(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
					(byte)1 /*SIDE_TOP*/, (byte)0 /*SIDE_BOTTOM*/, 1, false));

	/** The Crusher ULV row (Kinetic_T[0]; masks = the :1300 row verbatim — item top-in/bottom-out, energy back; the :1300 NBT_PARALLEL 4 + DURATION T kept). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CRUSHER_ULV_ROWS = java.util.List.of(
			ulvKinetic("crusher_ulv", 20025, 7.0F, KINETIC_T0, MACHINE_CRUSHER_DISPLAY_KEY,
					() -> GT6RecipeMaps.CRUSHER, TD.Energy.EU, "crusher",
					(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B :1300*/,
					(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
					(byte)1 /*SIDE_TOP*/, (byte)0 /*SIDE_BOTTOM*/, CRUSHER_PARALLEL[0], true)); /*NBT_PARALLEL_DURATION T :1300*/

	/** The Sifter ULV row (Kinetic_T[0]; masks = the :1312 family verbatim; the PARALLEL_4_32[0] = 4 + DURATION T family shape kept). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SIFTER_ULV_ROWS = java.util.List.of(
			ulvKinetic("sifter_ulv", 20055, 7.0F, KINETIC_T0, MACHINE_SIFTER_UNIT_KEY,
					() -> GT6RecipeMaps.SIFTING, TD.Energy.EU, "sifter",
					(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B :1312*/,
					(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
					(byte)1 /*SIDE_TOP*/, (byte)0 /*SIDE_BOTTOM*/, PARALLEL_4_32[0], true)); /*NBT_PARALLEL_DURATION T :1312*/

	/** The Wiremill ULV row (Kinetic_T[0]; masks = the :1373 row verbatim — item left-in/right-out, energy back; NO parallel key → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> WIREMILL_ULV_ROWS = java.util.List.of(
			ulvKinetic("wiremill_ulv", 20155, 7.0F, KINETIC_T0, MACHINE_WIREMILL_UNIT_KEY,
					() -> GT6RecipeMaps.WIREMILL, TD.Energy.EU, "wiremill",
					(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B :1373*/,
					(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A), (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
					(byte)2 /*SIDE_LEFT*/, (byte)4 /*SIDE_RIGHT*/, 1, false));

	/** The one Rolling Mill row — the SINGLE ULV electric rung (the card scope: RU 4-ladder family defers to the P29 batch A). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ROLLINGMILL_ROWS = java.util.List.of(
			ulvKinetic("rollingmill", 20115, 7.0F, KINETIC_T0, MACHINE_ROLLING_MILL_UNIT_KEY,
					() -> GT6RecipeMaps.ROLLING_MILL, TD.Energy.EU, "rollingmill",
					(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B :1349*/,
					(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A), (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
					(byte)2 /*SIDE_LEFT*/, (byte)4 /*SIDE_RIGHT*/, 1, false));

	/**
	 * One kinetic-family ULV row factory — the wiremill() row shape with the p28 columns:
	 * EU on every row (the EU-variant precedent), the 1375 K gate, the ulvVoltage marker,
	 * menu = null (zero new gt6:* MenuType), cheap overclocking T, the 127/-1 no-tank-key
	 * faces (zero fluid recipes ≠ zero fluid face, the Shredder precedent).
	 */
	// (the aEnergyType → aTexture parameter order keeps the `TD.Energy.<KIND>, "<family>",`
	// literal pair in every call site — the borrow_port_overlays.py family census parses
	// exactly that face, so the rollingmill borrow set stays census-driven, no hand list)
	private static GTBasicMachineBlock.MachineRow ulvKinetic(String aPath, int aMetaId, float aHardness,
			java.util.function.Supplier<OreDictMaterial> aMaterial, String aDisplayKey,
			java.util.function.Supplier<RecipeMap> aRecipes, TagData aEnergyType, String aTexture,
			byte aEnergySides, byte aItemIn, byte aItemOut, byte aItemAutoIn, byte aItemAutoOut, int aParallel, boolean aParallelDuration) {
		return new GTBasicMachineBlock.MachineRow(aPath, "any_wood", "Any Wood", aMaterial, aDisplayKey, aMetaId, aHardness,
				0 /*tier — the material rung index; the voltage window rides ulvVoltage, NOT the tier table*/, aParallel, aParallelDuration,
				aRecipes, aEnergyType /*the p28 V[0] carrier — every ULV row is electric EU*/, aTexture,
				aEnergySides,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				aItemIn, aItemOut,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				aItemAutoIn, aItemAutoOut,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/,
				ULV_MELTING_GATE_K /*the 1375 K stone-crucible ceiling*/, true /*ulvVoltage — the {4,8,16} window*/);
	}

	/** The Canner ULV row factory — the canner() row shape (tank keys + the live gt6:canner menu kept) with the p28 columns. */
	private static GTBasicMachineBlock.MachineRow cannerUlv() {
		return new GTBasicMachineBlock.MachineRow("canner_ulv", "ulv", "ULV", ELECTRIC_T0, CANNER_DISPLAY_KEY, 20166, 4.0F,
				0, 1, false,
				() -> GT6RecipeMaps.CANNER, TD.Energy.EU, "canner",
				(byte)(GTBasicMachineBlock.SBIT_B) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L) /*NBT_TANK_SIDE_IN*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D) /*NBT_TANK_SIDE_OUT*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L) /*NBT_INV_SIDE_IN*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D) /*NBT_INV_SIDE_OUT*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				GTBasicMachinesMenus.CANNER_MENU::get /*gt6:canner — the family menu the ULV row shares*/,
				true /*NBT_CHEAP_OVERCLOCKING*/,
				ULV_MELTING_GATE_K /*the 1375 K stone-crucible ceiling*/, true /*ulvVoltage*/);
	}

	/** The registered Rolling Mill blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> ROLLINGMILL_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Rolling Mill items, same keys as {@link #ROLLINGMILL_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> ROLLINGMILL_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	// the ULV blocks join the FAMILY BY_PATH maps (the BET validBlocks ride the
	// blockArray() walkers; loot/datagen/creative-tab walk the same tables) — the
	// shredder/crusher ULV blocks are explicit ROs below (their families have no maps).
	static {
		for (GTBasicMachineBlock.MachineRow tRow : CANNER_ULV_ROWS) {
			CANNER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.CANNER_BE.get(), tRow)));
			CANNER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : SIFTER_ULV_ROWS) {
			SIFTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SIFTER_BE.get(), tRow)));
			SIFTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : WIREMILL_ULV_ROWS) {
			WIREMILL_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.WIREMILL_BE.get(), tRow)));
			WIREMILL_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.WIREMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : ROLLINGMILL_ROWS) {
			ROLLINGMILL_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ROLLINGMILL_BE.get(), tRow)));
			ROLLINGMILL_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ROLLINGMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The p28 ULV shredder row block (the explicit-RO form — the legacy family has no BY_PATH map). */
	public static final RegistryObject<Block> SHREDDER_ULV = BLOCKS.register("shredder_ulv",
			() -> new GTBasicMachineBlock(SHREDDER_ULV_ROWS.get(0).properties(), () -> GTMachines.SHREDDER_BE.get(), SHREDDER_ULV_ROWS.get(0)));

	public static final RegistryObject<Item> SHREDDER_ULV_ITEM = ITEMS.register("shredder_ulv",
			() -> new gregtech6.block.GTComposedNameItem(GTMachines.SHREDDER_ULV.get(), new Item.Properties()));

	/** The p28 ULV crusher row block (the explicit-RO form). */
	public static final RegistryObject<Block> CRUSHER_ULV = BLOCKS.register("crusher_ulv",
			() -> new GTBasicMachineBlock(CRUSHER_ULV_ROWS.get(0).properties(), () -> GTMachines.CRUSHER_BE.get(), CRUSHER_ULV_ROWS.get(0)));

	public static final RegistryObject<Item> CRUSHER_ULV_ITEM = ITEMS.register("crusher_ulv",
			() -> new gregtech6.block.GTComposedNameItem(GTMachines.CRUSHER_ULV.get(), new Item.Properties()));

	/** The Rolling Mill block list in registration order (the loot/datagen walkers). */
	public static Block[] rollingmillBlockArray() {
		Block[] rBlocks = new Block[ROLLINGMILL_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ROLLINGMILL_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine rollingmill — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block rollingmillBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ROLLINGMILL_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Rolling Mill family BET: the Dryer shape verbatim over the single ULV row
	 * (the card scope — the RU 4-ladder family is the P29 batch A). The registry id
	 * "rollingmill" is free (no legacy family).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ROLLINGMILL_BE =
			BLOCK_ENTITY_TYPES.register("rollingmill", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ROLLINGMILL_BE.get(), aPos, aState),
					rollingmillBlockArray()).build(null));

	// ---------------------------------------------------------------------------
	// the Advanced Crafting Table (task p24-act-machine) — the SINGLE-VARIANT machine
	// (decisions.p24-act-be-form: the upstream MTE extends TileEntityBase09FacingSingle,
	// NOT the TileEntityBasicMachine energy family — zero energy, zero tick auto-craft —
	// so the registration is the OVEN three-row shape, not a MachineRow ladder): one
	// block + one BET + one item, id gt6:advanced_crafting_table (upstream
	// "gt.multitileentity.crafting.advanced", Loader_MultiTileEntities.java:136
	// metalset id 5000+aID — the 1.7.10 numeric id axis is dead on the string axis, the
	// variant ladder consciously unpinned, the deviation ⑥ ruling). Hardness 6.0F
	// (the oven tier-1 row shape, GTMachines:58-59).
	// ---------------------------------------------------------------------------

	public static final RegistryObject<Block> ADVANCED_CRAFTING_TABLE = BLOCKS.register("advanced_crafting_table",
			() -> new gregtech6.block.GTAdvancedCraftingTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL)));

	public static final RegistryObject<BlockEntityType<TileEntityAdvancedCraftingTable>> ADVANCED_CRAFTING_TABLE_BE =
			BLOCK_ENTITY_TYPES.register("advanced_crafting_table", () -> BlockEntityType.Builder.of(
					TileEntityAdvancedCraftingTable::new, ADVANCED_CRAFTING_TABLE.get()).build(null));

	public static final RegistryObject<Item> ADVANCED_CRAFTING_TABLE_ITEM = ITEMS.register("advanced_crafting_table",
			() -> new gregtech6.block.GTComposedNameItem(ADVANCED_CRAFTING_TABLE.get(), new Item.Properties()));


	// ---------------------------------------------------------------------------
	// the Distillery family (task p16-distillery-family ②) — the four rows
	// Loader_MultiTileEntities.java:1398-1401 (aClass = MultiTileEntityBasicMachine,
	// NBT_TEXTURE "distillery", TD.Energy.HU, RM.Distillery, NBT_CHEAP_OVERCLOCKING T,
	// NBT_PARALLEL_DURATION T). ONE family BET over the four tier blocks — the same
	// MachineRow carrier shape as the Dryer ladder above. The connectivity masks
	// (CS.java:612 SBIT values): energy = SBIT_D|SBIT_A (the :151 read ORs SBIT_A onto
	// NBT_ENERGY_ACCEPTED_SIDES SBIT_D), tank in = SBIT_U|SBIT_L|SBIT_A (the :143 read —
	// upstream NBT_TANK_SIDE_IN SBIT_U|SBIT_L), tank out = SBIT_B|SBIT_A (the :144 read),
	// item in = SBIT_U|SBIT_L|SBIT_A (the :137 read — NBT_INV_SIDE_IN SBIT_U|SBIT_L), item
	// out = SBIT_R|SBIT_A (the :138 read); the four auto sides are the :139/:140/:145/:146
	// columns (data-only — the auto-IO pool). menu = the menu-less carrier (the GUI pool
	// precedent — use() stays inert, the acceptance drives inject+check like the pre-gui
	// dryer).
	// ---------------------------------------------------------------------------

	/** The Distillery family display template key ({@code gt6.row.distillery.display}, task p20-i18n-compose-rows). */
	public static final String DISTILLERY_DISPLAY_KEY = "gt6.row.distillery.display";

	/** The four Distillery rows, upstream line order :1398-1401 (T1-T4). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> DISTILLERY_ROWS = java.util.List.of(
			distillery("distillery"   , "steel"           , "Steel"           , 20191,  6.0F, 0,   8),
			distillery("distillery_t2", "invar"           , "Invar"           , 20192,  4.0F, 1,  16),
			distillery("distillery_t3", "titanium"        , "Titanium"        , 20193,  9.0F, 2,  32),
			distillery("distillery_t4", "tungsten_carbide", "Tungsten Carbide", 20194, 12.5F, 3, 64));

	/**
	 * One row factory — the four Distillery columns that differ (path/name/id/material/
	 * hardness/tier/parallel) plus the seven that are family constants (RM.Distillery
	 * through the supplier, HU, the "distillery" texture, the masks, the auto sides, cheap
	 * overclocking T) and the null menu supplier (the menu-less carrier).
	 */
	private static GTBasicMachineBlock.MachineRow distillery(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, HEAT_T_LADDER.get(aTier), DISTILLERY_DISPLAY_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.DISTILLERY, TD.Energy.HU, "distillery",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A),
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A),
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)5 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BACK*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the GUI pool precedent*/, true /*NBT_CHEAP_OVERCLOCKING T*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** The registered Distillery blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> DISTILLERY_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Distillery items, same keys as {@link #DISTILLERY_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> DISTILLERY_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : DISTILLERY_ROWS) {
			DISTILLERY_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.DISTILLERY_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			DISTILLERY_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Distillery block list in registration order (the loot/datagen walkers). */
	public static Block[] distilleryBlockArray() {
		Block[] rBlocks = new Block[DISTILLERY_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : DISTILLERY_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/**
	 * The paint-tint walker (task p21-paintable-tint-render; the Canner ladder joins in task
	 * p24-canner-machine; the W1 Kinetic trio joins in task p26-w1-sifter-compressor-wiremill;
	 * the Oven ladder joins in task p27-oven-heat-t-ladder; the six ULV rows join in task
	 * p28-c-ulv-machine-ladder):
	 * the 54 machine-domain blocks the client paint BlockColor
	 * registers over — the oven ladder (4) + the shredder/crusher/lathe ladders (4 each = 12) + the
	 * dryer (4) + the distillery (4) + the canner (4) + the sifter/compressor/wiremill
	 * ladders (4 each = 12) + press (4) + extruder (4) + the ULV rows (5 + the rollingmill
	 * rung = 6),
	 * the upstream {@code MultiTileEntityBasicMachine} render census (the getTexture2 :1014
	 * grayscale x mRGBa consumers). Card_A put the paint capability on the 03 base, so the
	 * whole 03 family can carry PAINT model data (barrels/pipes included) — but this card's
	 * v1 registers the tint over exactly this machine array; the family-wide extension
	 * (connectors/barrels/pipes rendering) stays pooled. Client-side call time only.
	 */
	public static Block[] paintableBlockArray() {
		java.util.List<Block> rBlocks = new java.util.ArrayList<>(54);
		rBlocks.add(OVEN.get());
		rBlocks.add(OVEN_T2.get()); // task p27-oven-heat-t-ladder
		rBlocks.add(OVEN_T3.get());
		rBlocks.add(OVEN_T4.get());
		for (RegistryObject<Block> tBlock : java.util.List.of(
				SHREDDER, SHREDDER_T2, SHREDDER_T3, SHREDDER_T4,
				CRUSHER, CRUSHER_T2, CRUSHER_T3, CRUSHER_T4,
				LATHE, LATHE_T2, LATHE_T3, LATHE_T4)) {
			rBlocks.add(tBlock.get());
		}
		java.util.Collections.addAll(rBlocks, dryerBlockArray());
		java.util.Collections.addAll(rBlocks, distilleryBlockArray());
		java.util.Collections.addAll(rBlocks, cannerBlockArray()); // + the canner_ulv rung (the BY_PATH map walk carries the p28 ULV row)
		java.util.Collections.addAll(rBlocks, sifterBlockArray()); // + sifter_ulv
		java.util.Collections.addAll(rBlocks, compressorBlockArray());
		java.util.Collections.addAll(rBlocks, wiremillBlockArray()); // + wiremill_ulv
		java.util.Collections.addAll(rBlocks, pressBlockArray()); // task p26-w1-press-extruder-molds
		java.util.Collections.addAll(rBlocks, extruderBlockArray()); // task p26-w1-press-extruder-molds
		rBlocks.add(SHREDDER_ULV.get()); // task p28-c-ulv-machine-ladder — the explicit-RO ULV rows + the new RollingMill family
		rBlocks.add(CRUSHER_ULV.get());
		java.util.Collections.addAll(rBlocks, rollingmillBlockArray());
		return rBlocks.toArray(new Block[0]);
	}

	/** The lookup for /gt6machine distillery — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block distilleryBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = DISTILLERY_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Distillery family BET: the Dryer shape verbatim — the shared factory, the
	 * four tier blocks multi-attached, the row read off the placed block.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> DISTILLERY_BE =
			BLOCK_ENTITY_TYPES.register("distillery", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> distilleryMachine(GTMachines.DISTILLERY_BE.get(), aPos, aState),
					distilleryBlockArray()).build(null));

	/** The Distillery BET factory body — the dryerMachine body verbatim over the Distillery rows. */
	private static TileEntityBasicMachine distilleryMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyRow(tMachine, tRow);
	}

	/** The tier index of a family block (0=T1 .. 3=T4) — BE creation time, every RO is resolved. */
	private static int tierOf(Block aBlock, RegistryObject<Block> aT1, RegistryObject<Block> aT2, RegistryObject<Block> aT3, RegistryObject<Block> aT4) {
		if (aBlock == aT2.get()) return 1;
		if (aBlock == aT3.get()) return 2;
		if (aBlock == aT4.get()) return 3;
		return 0; // aT1 — the BET validBlocks only contain the family's own four blocks
	}

	/**
	 * The BET factory body: constructor-injected config + the per-row carrier and energy
	 * three-value assignment (:1294-1309 NBT_ENERGY_ACCEPTED + NBT_INPUT through the :126
	 * conversion — TileEntityBasicMachine :137 fields are non-final by upstream design :98).
	 * The menu travels as a plain supplier since task p14-dryer-family (null = the
	 * menu-less carrier — the supplier form is what a null menu needs, a RegistryObject
	 * method reference would capture the null receiver and explode at BE creation); since
	 * task p16-machine-fluid-gui the Dryer rows pass the bound {@code gt6:dryer} supplier
	 * the same way.
	 */
	private static TileEntityBasicMachine machine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState, RecipeMap aRecipes, int aParallel, boolean aParallelDuration,
			gregapi.code.TagData aEnergyType, int aTier, @javax.annotation.Nullable java.util.function.Supplier<MenuType<GTBasicMachineMenu>> aMenu) {
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(aType, aPos, aState, aRecipes, aParallel, aParallelDuration, aMenu);
		tMachine.mEnergyTypeAccepted = aEnergyType;
		long[] tInputs = TIER_INPUTS[aTier];
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return tMachine;
	}

	/**
	 * Creative tab for the machine family — the upstream "Basic Machines" MTE-registry category
	 * (Loader_MultiTileEntities.java:1288 aRegistry category column).
	 */
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	public static final RegistryObject<CreativeModeTab> MACHINES_TAB = CREATIVE_MODE_TABS.register("machines",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.machines"))
					.icon(() -> new ItemStack(OVEN_ITEM.get()))
						.displayItems((aParameters, aOutput) -> {
							// task p27-oven-heat-t-ladder: the Oven Heat_T ladder, +3 rows —
							// the OVEN_TAB_ITEMS walk (upstream row order :1288-1291)
							for (RegistryObject<Item> tOvenItem : OVEN_TAB_ITEMS) {
								aOutput.accept(new ItemStack(tOvenItem.get()));
							}
							aOutput.accept(new ItemStack(SHREDDER_ITEM.get())); // task p7-basicmachine-family: +3 machine family rows
							aOutput.accept(new ItemStack(CRUSHER_ITEM.get()));
							aOutput.accept(new ItemStack(LATHE_ITEM.get()));
							// task p8-machine-tiers-doinject ①: the T2-T4 ladder, +9 rows
							aOutput.accept(new ItemStack(SHREDDER_T2_ITEM.get()));
							aOutput.accept(new ItemStack(SHREDDER_T3_ITEM.get()));
							aOutput.accept(new ItemStack(SHREDDER_T4_ITEM.get()));
							aOutput.accept(new ItemStack(CRUSHER_T2_ITEM.get()));
							aOutput.accept(new ItemStack(CRUSHER_T3_ITEM.get()));
							aOutput.accept(new ItemStack(CRUSHER_T4_ITEM.get()));
								aOutput.accept(new ItemStack(LATHE_T2_ITEM.get()));
								aOutput.accept(new ItemStack(LATHE_T3_ITEM.get()));
								aOutput.accept(new ItemStack(LATHE_T4_ITEM.get()));
								// task p14-dryer-family: the Dryer ladder, +4 rows
								for (GTBasicMachineBlock.MachineRow tRow : DRYER_ROWS) {
									aOutput.accept(new ItemStack(DRYER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p16-distillery-family: the Distillery ladder, +4 rows
								for (GTBasicMachineBlock.MachineRow tRow : DISTILLERY_ROWS) {
									aOutput.accept(new ItemStack(DISTILLERY_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p24-canner-machine: the Canner ladder, +4 rows
								for (GTBasicMachineBlock.MachineRow tRow : CANNER_ROWS) {
									aOutput.accept(new ItemStack(CANNER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p26-w1-sifter-compressor-wiremill: the Kinetic trio, +12 rows
								for (GTBasicMachineBlock.MachineRow tRow : SIFTER_ROWS) {
									aOutput.accept(new ItemStack(SIFTER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : COMPRESSOR_ROWS) {
									aOutput.accept(new ItemStack(COMPRESSOR_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : WIREMILL_ROWS) {
									aOutput.accept(new ItemStack(WIREMILL_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p26-w1-press-extruder-molds: the Press + Extruder ladders, +8 rows
								for (GTBasicMachineBlock.MachineRow tRow : PRESS_ROWS) {
									aOutput.accept(new ItemStack(PRESS_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : EXTRUDER_ROWS) {
									aOutput.accept(new ItemStack(EXTRUDER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p28-c-ulv-machine-ladder: the six ULV rows, +6 (the
								// shredder/crusher explicit ROs + the canner/sifter/wiremill/
								// rollingmill ULV rungs — the V[0] = 8 EU tier face)
								aOutput.accept(new ItemStack(SHREDDER_ULV_ITEM.get()));
								aOutput.accept(new ItemStack(CRUSHER_ULV_ITEM.get()));
								for (GTBasicMachineBlock.MachineRow tRow : CANNER_ULV_ROWS) {
									aOutput.accept(new ItemStack(CANNER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : SIFTER_ULV_ROWS) {
									aOutput.accept(new ItemStack(SIFTER_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : WIREMILL_ULV_ROWS) {
									aOutput.accept(new ItemStack(WIREMILL_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								for (GTBasicMachineBlock.MachineRow tRow : ROLLINGMILL_ROWS) {
									aOutput.accept(new ItemStack(ROLLINGMILL_ITEMS_BY_PATH.get(tRow.path()).get()));
								}
								// task p24-act-machine: the Advanced Crafting Table (the single-variant row)
								aOutput.accept(new ItemStack(ADVANCED_CRAFTING_TABLE_ITEM.get()));
								// task p16-distillery-family ①: the Integrated Circuit ("Selector Tag") —
								// the recipe-slot selector feeds these machines, the machines tab is the
								// nearest live category (the gregapi items tab is not ported, declared)
								aOutput.accept(new ItemStack(gregtech6.item.GT6Circuits.INTEGRATED_CIRCUIT.get()));
								// task p26-w1-press-extruder-molds: the extruder-mold row0 pair — the
								// shaping tools feed the press/extruder, the nearest live category (the
								// circuit precedent; the upstream Technological items tab is not ported)
								aOutput.accept(new ItemStack(gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()));
								aOutput.accept(new ItemStack(gregtech6.registry.GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()));
						})
					.build());

	private GTMachines() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
	 * (GTBlockEntities precedent). Bus.MOD.bus().get() = FMLJavaModLoadingContext.get().getModEventBus()
	 * (Mod.java:81). The recipe-map init rides here: data-only, both sides, before first tick.
	 */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GT6Mod/GTMenuTypes fork precedent.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
		GT6RecipeMaps.init(); // W1 handoff: the FURNACE map lifecycle is this card's job (GT6RecipeMaps.java:36-37)
	}
}
