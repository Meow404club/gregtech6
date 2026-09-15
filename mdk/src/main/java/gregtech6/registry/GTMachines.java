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
import gregapi.tileentity.energy.ITileEntityEnergy;
	import gregtech6.block.GTBasicMachineBlock;
	import gregtech6.block.GTOvenBlock;
import gregtech6.block.energy.GT6DynamoBlock;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.energy.GT6DynamoBlockEntity;
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

	/**
	 * The Electric_T[5] rung (Ti, upstream MT.java:3691 index 5) — the 5-tier 立行制's
	 * material column (task p29-w2-energy-types-5tier ②, the Electrolyzer family the
	 * first consumer, card ②). Same lazy-supplier form as the T0 rungs above (the
	 * GTWireSpecs:35 ruling — the registry classes load before {@code MT.init()}).
	 */
	public static final java.util.function.Supplier<OreDictMaterial> ELECTRIC_T5 = () -> gregapi.data.MT.Ti;

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

	/**
	 * The 5-tier voltage window (task p29-w2-energy-types-5tier ②) — the :126 conversion
	 * (min = in/2, max = in*2) over NBT_INPUT 8192: mInputMin 4096 / mInput 8192 /
	 * mInputMax 16384 (the T5 Electrolyzer row NBT_INPUT column, Loader_MultiTileEntities
	 * .java:1340). A PARALLEL constant to {@link #TIER_INPUTS} by design: the 4-row table
	 * stays byte-identical so every tier-indexed consumer (the GTOvenBlock row index, the
	 * W1 four-tier rows) keeps addressing it without drift; card ② rides
	 * {@code TIER_INPUTS[0..3] + EV_TIER_INPUTS}.
	 *
	 * <p>Naming erratum (declared, the W1 units()-direction erratum form): the constant
	 * keeps the card's name as the consumers' API anchor, but the card gloss "电压词表第
	 * 5 词 EV" is refuted by the evidence — upstream CS.java:154 VN[5] = "IV" (the
	 * Loader:1340 T5 name column rides VN[5]), and "ev" is ALREADY T4's word in this repo
	 * (the canner/Electric* rows :1379-1382/:1504-1522, the gt6.row.mat.ev unit since
	 * p24). The lang face therefore carries gt6.row.mat.iv (VN[5], both locales).
	 * NO consumer row yet (card ② owns the first 5-tier family).
	 */
	public static final long[] EV_TIER_INPUTS = {4096, 8192, 16384};

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

	/**
	 * The Centrifuge NON-standard parallel table (task p29-w1-rm-maps-scaffold ③): the
	 * upstream NBT_PARALLEL {1, 2, 4, 8} + NBT_PARALLEL_DURATION T columns of the four
	 * Centrifuge rows (Loader_MultiTileEntities.java:1330-1333) — the T1 = 1 arm is what
	 * makes the ladder non-standard, unlike the 4/8/16/32 Crusher/Sifter/Compressor/Press
	 * shape ({@link #PARALLEL_4_32}). NO consumer row yet (the batch C Centrifuge family,
	 * card p29-w1-kinetic-process-ladder, consumes it); declared BEFORE its consumers per
	 * the static-initializer order lesson, next to {@link #PARALLEL_4_32} as the card
	 * ordered.
	 */
	public static final int[] CENTRIFUGE_PARALLEL = {1, 2, 4, 8};

	/**
	 * The CryoMixer parallel table (task p29-w2-energy-types-5tier ④): the upstream
	 * NBT_PARALLEL {4, 8, 16, 32, 64} + NBT_PARALLEL_DURATION T columns of the five CU
	 * CryoMixer rows (Loader_MultiTileEntities.java:1628-1632) — the T1 = 4 arm matches
	 * the PARALLEL_4_32 shape extended one rung. Declared BEFORE its consumer (card ④
	 * owns the CryoMixer rows), next to {@link #CENTRIFUGE_PARALLEL} as the card ordered
	 * (the static-initializer order lesson).
	 */
	public static final int[] CRYO_PARALLEL = {4, 8, 16, 32, 64};

	/**
	 * The Electrolyzer parallel table (task p29-w2-eu-core-5tier): the upstream NBT_PARALLEL
	 * {1, 2, 4, 8, 16} + NBT_PARALLEL_DURATION T columns of the five EU Electrolyzer rows
	 * (Loader_MultiTileEntities.java:1336-1340) — the FIRST FIVE-RUNG parallel table (the
	 * T5 arm rides {@link #EV_TIER_INPUTS}), the T1 = 1 arm matching the NON-standard
	 * {@link #CENTRIFUGE_PARALLEL} shape it sits beside (the card ordered the constant
	 * next to it; the static-initializer order lesson). Consumed by the Electrolyzer rows
	 * below; the other four eu-core families register no NBT_PARALLEL key → 1.
	 */
	public static final int[] ELECTROLYZER_PARALLEL = {1, 2, 4, 8, 16};

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
	 * Task p29-w1-rm-maps-scaffold adds the efficiency column: the row's
	 * {@code efficiency} rides onto {@code mEfficiency} through the upstream :125 bind form
	 * {@code bind(0, 10000, value)} — null = no NBT_EFFICIENCY key, the BE keeps its :96
	 * default 10000 and the progress math stays byte-identical (the zero-drift face).
	 */
	public static TileEntityBasicMachine applyRow(TileEntityBasicMachine aMachine, GTBasicMachineBlock.MachineRow aRow) {
		aMachine.mEnergyInputs = aRow.energySides();
		aMachine.mFluidInputs = aRow.fluidIn();
		aMachine.mFluidOutputs = aRow.fluidOut();
		aMachine.mMaxMeltingPointK = aRow.maxMeltingPointK();
		if (aRow.efficiency() != null) aMachine.mEfficiency = (short)gregapi.util.UT.Code.bind(0, 10000, aRow.efficiency());
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
	// the P29 W1 roll-ladder families (task p29-w1-kinetic-roll-ladder) — RollingMill
	// (RU, :1349-1352), RollBender (RU, :1355-1358), RollFormer (RU, :1361-1364) and
	// ClusterMill (RU, :1367-1370), all MultiTileEntityBasicMachine with
	// MT.DATA.Kinetic_T[1..4] (Bronze / ANY.Steel / Ti / TungstenSteel), NBT_INPUT
	// 32/128/512/2048 through the TIER_INPUTS conversion, NBT_TEXTURE
	// "rollingmill"/"rollbender"/"rollformer"/"clustermill", hardness 7.0/6.0/9.0/12.5
	// (NBT_RESISTANCE == hardness), NO NBT_PARALLEL key on any of the sixteen rows → 1
	// (the :97 ruling shape, the Wiremill column set), and the :1349-1370 connectivity
	// face ALL FOUR families share verbatim: item in over the left (NBT_INV_SIDE_IN
	// SBIT_L, AUTO_IN SIDE_LEFT), out over the right (NBT_INV_SIDE_OUT SBIT_R, AUTO_OUT
	// SIDE_RIGHT), energy over the back (NBT_ENERGY_ACCEPTED_SIDES SBIT_B, the :151 read
	// ORs SBIT_A). No NBT_TANK_SIDE_* keys → the upstream field defaults 127 (the
	// all-sides zero-regression face) with SIDE_UNDEFINED auto sides. menu = null on
	// every row: ZERO new gt6:* MenuType (the p26 W1 GUI clause). Rows ride the 26-arg
	// MachineRow constructor (efficiency = null — kinetic machines carry no
	// NBT_EFFICIENCY key, the BE keeps the :96 default 10000; the card-A column is
	// consumed by the Electric_* rows only).
	//
	// RollingMill path note: the p28 ULV rung owns the bare path "rollingmill" (20115,
	// EU) — the RU ladder (:1349-1352, 20111-20114) registers as rollingmill_t1.._t4 in
	// the SAME BY_PATH maps and the SAME family BET (the validBlocks walk is the map
	// walk), so the ULV electric rung and the RU material ladder share the map and run
	// their own rows (ULV window vs TIER_INPUTS), the same-map co-existence the card
	// orders. The other three families are new registries, ids free.
	//
	// KJS surface declaration (the card contract): this section's product is the
	// REGISTRATION face (4 families x 4 MachineRow rows) plus the datapack domain (the
	// data/gt6/recipe_maps/<map>.json smoke rows poured through the GT6RecipeMapJsonLoader
	// seam). The four recipe maps are the card-A GT6RecipeMaps constants REUSED verbatim —
	// no KubeJS face, no new map constant, no new GUI MenuType lives here.
	// ---------------------------------------------------------------------------
	/** The Roll Bender family unit word (the W1 trio :101-104 key form; upstream name column "Roll Bender ("+aMat.getLocal()+")", Loader:1355-1358). */
	public static final String MACHINE_ROLL_BENDER_UNIT_KEY = "gt6.row.machine.roll_bender";
	/** The Roll Former family unit word (the same key form; "Roll Former (", Loader:1361-1364). */
	public static final String MACHINE_ROLL_FORMER_UNIT_KEY = "gt6.row.machine.roll_former";
	/** The Cluster Mill family unit word (the same key form; "Cluster Mill (", Loader:1367-1370). */
	public static final String MACHINE_CLUSTER_MILL_UNIT_KEY = "gt6.row.machine.cluster_mill";

	/** The four Rolling Mill RU rows, upstream line order :1349-1352 (T1-T4, the Kinetic_T ladder; the p28 ULV rung keeps the bare path + the EU row). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ROLLINGMILL_RU_ROWS = java.util.List.of(
			rollFamily(TD.Energy.RU, "rollingmill", () -> GT6RecipeMaps.ROLLING_MILL, MACHINE_ROLLING_MILL_UNIT_KEY, "rollingmill_t1", "bronze"       , "Bronze"       , 20111,  7.0F, 0),
			rollFamily(TD.Energy.RU, "rollingmill", () -> GT6RecipeMaps.ROLLING_MILL, MACHINE_ROLLING_MILL_UNIT_KEY, "rollingmill_t2", "steel"        , "Steel"        , 20112,  6.0F, 1),
			rollFamily(TD.Energy.RU, "rollingmill", () -> GT6RecipeMaps.ROLLING_MILL, MACHINE_ROLLING_MILL_UNIT_KEY, "rollingmill_t3", "titanium"     , "Titanium"     , 20113,  9.0F, 2),
			rollFamily(TD.Energy.RU, "rollingmill", () -> GT6RecipeMaps.ROLLING_MILL, MACHINE_ROLLING_MILL_UNIT_KEY, "rollingmill_t4", "tungstensteel", "Tungstensteel", 20114, 12.5F, 3));

	/** The four Roll Bender rows, upstream line order :1355-1358 (T1-T4, the Kinetic_T ladder; no NBT_PARALLEL → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ROLL_BENDER_ROWS = java.util.List.of(
			rollFamily(TD.Energy.RU, "rollbender", () -> GT6RecipeMaps.ROLL_BENDER, MACHINE_ROLL_BENDER_UNIT_KEY, "rollbender"   , "bronze"       , "Bronze"       , 20121,  7.0F, 0),
			rollFamily(TD.Energy.RU, "rollbender", () -> GT6RecipeMaps.ROLL_BENDER, MACHINE_ROLL_BENDER_UNIT_KEY, "rollbender_t2", "steel"        , "Steel"        , 20122,  6.0F, 1),
			rollFamily(TD.Energy.RU, "rollbender", () -> GT6RecipeMaps.ROLL_BENDER, MACHINE_ROLL_BENDER_UNIT_KEY, "rollbender_t3", "titanium"     , "Titanium"     , 20123,  9.0F, 2),
			rollFamily(TD.Energy.RU, "rollbender", () -> GT6RecipeMaps.ROLL_BENDER, MACHINE_ROLL_BENDER_UNIT_KEY, "rollbender_t4", "tungstensteel", "Tungstensteel", 20124, 12.5F, 3));

	/** The four Roll Former rows, upstream line order :1361-1364 (T1-T4, the Kinetic_T ladder; no NBT_PARALLEL → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ROLL_FORMER_ROWS = java.util.List.of(
			rollFamily(TD.Energy.RU, "rollformer", () -> GT6RecipeMaps.ROLL_FORMER, MACHINE_ROLL_FORMER_UNIT_KEY, "rollformer"   , "bronze"       , "Bronze"       , 20131,  7.0F, 0),
			rollFamily(TD.Energy.RU, "rollformer", () -> GT6RecipeMaps.ROLL_FORMER, MACHINE_ROLL_FORMER_UNIT_KEY, "rollformer_t2", "steel"        , "Steel"        , 20132,  6.0F, 1),
			rollFamily(TD.Energy.RU, "rollformer", () -> GT6RecipeMaps.ROLL_FORMER, MACHINE_ROLL_FORMER_UNIT_KEY, "rollformer_t3", "titanium"     , "Titanium"     , 20133,  9.0F, 2),
			rollFamily(TD.Energy.RU, "rollformer", () -> GT6RecipeMaps.ROLL_FORMER, MACHINE_ROLL_FORMER_UNIT_KEY, "rollformer_t4", "tungstensteel", "Tungstensteel", 20134, 12.5F, 3));

	/** The four Cluster Mill rows, upstream line order :1367-1370 (T1-T4, the Kinetic_T ladder; the casingMachineQuadruple recipe family — no NBT_PARALLEL → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CLUSTER_MILL_ROWS = java.util.List.of(
			rollFamily(TD.Energy.RU, "clustermill", () -> GT6RecipeMaps.CLUSTER_MILL, MACHINE_CLUSTER_MILL_UNIT_KEY, "clustermill"   , "bronze"       , "Bronze"       , 20141,  7.0F, 0),
			rollFamily(TD.Energy.RU, "clustermill", () -> GT6RecipeMaps.CLUSTER_MILL, MACHINE_CLUSTER_MILL_UNIT_KEY, "clustermill_t2", "steel"        , "Steel"        , 20142,  6.0F, 1),
			rollFamily(TD.Energy.RU, "clustermill", () -> GT6RecipeMaps.CLUSTER_MILL, MACHINE_CLUSTER_MILL_UNIT_KEY, "clustermill_t3", "titanium"     , "Titanium"     , 20143,  9.0F, 2),
			rollFamily(TD.Energy.RU, "clustermill", () -> GT6RecipeMaps.CLUSTER_MILL, MACHINE_CLUSTER_MILL_UNIT_KEY, "clustermill_t4", "tungstensteel", "Tungstensteel", 20144, 12.5F, 3));

	/**
	 * One roll-family row factory — the wiremill() row shape verbatim over the
	 * :1349-1370 face (left in / right out, energy back, no tank keys, no parallel → 1,
	 * RU), parameterised over the family energy/texture/map/unit-key; the differing
	 * columns (path/id/material rung/hardness/tier) ride the arguments. The recipe map
	 * rides the LAZY supplier (the ulvKinetic form — the offline test suite re-inits the
	 * map registry per test, so a captured instance would go stale). The
	 * {@code TD.Energy.RU, "<family>",} literal pair at the call sites is the
	 * borrow_port_overlays.py census face (the family texture borrow set parses exactly
	 * that face). The 26-arg MachineRow constructor — efficiency = null (the kinetic
	 * no-NBT_EFFICIENCY ruling).
	 */
	private static GTBasicMachineBlock.MachineRow rollFamily(gregapi.code.TagData aEnergyType, String aTexture,
			java.util.function.Supplier<gregtech6.recipes.RecipeMap> aMap, String aUnitKey,
			String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), aUnitKey, aMetaId, aHardness, aTier, 1, false,
				aMap, aEnergyType, aTexture,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B, the :151 read ORs SBIT_A*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L, the :137 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*no NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType (the p26 W1 GUI clause)*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate on the legacy rows*/, false);
	}

	/** The registered Roll Bender blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> ROLLBENDER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Roll Bender items, same keys as {@link #ROLLBENDER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> ROLLBENDER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Roll Former blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ROLLFORMER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Roll Former items, same keys as {@link #ROLLFORMER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> ROLLFORMER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Cluster Mill blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> CLUSTERMILL_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Cluster Mill items, same keys as {@link #CLUSTERMILL_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> CLUSTERMILL_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	// the roll-ladder blocks join the family BY_PATH maps (the BET validBlocks ride the
	// blockArray() walkers; loot/datagen/creative-tab/paint walk the same tables) — the
	// RollingMill RU rows join the p28 ULV rung's OWN maps (the shared-BET ruling above).
	static {
		for (GTBasicMachineBlock.MachineRow tRow : ROLLINGMILL_RU_ROWS) {
			ROLLINGMILL_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ROLLINGMILL_BE.get(), tRow)));
			ROLLINGMILL_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ROLLINGMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : ROLL_BENDER_ROWS) {
			ROLLBENDER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ROLLBENDER_BE.get(), tRow)));
			ROLLBENDER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ROLLBENDER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : ROLL_FORMER_ROWS) {
			ROLLFORMER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ROLLFORMER_BE.get(), tRow)));
			ROLLFORMER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ROLLFORMER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : CLUSTER_MILL_ROWS) {
			CLUSTERMILL_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.CLUSTERMILL_BE.get(), tRow)));
			CLUSTERMILL_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.CLUSTERMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Roll Bender block list in registration order (the loot/datagen walkers). */
	public static Block[] rollbenderBlockArray() {
		Block[] rBlocks = new Block[ROLLBENDER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ROLLBENDER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Roll Former block list in registration order (the loot/datagen walkers). */
	public static Block[] rollformerBlockArray() {
		Block[] rBlocks = new Block[ROLLFORMER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ROLLFORMER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Cluster Mill block list in registration order (the loot/datagen walkers). */
	public static Block[] clustermillBlockArray() {
		Block[] rBlocks = new Block[CLUSTERMILL_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : CLUSTERMILL_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine rollbender — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block rollbenderBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ROLLBENDER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine rollformer — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block rollformerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ROLLFORMER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine clustermill — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block clustermillBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = CLUSTERMILL_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The Roll Bender family BET: the Wiremill shape verbatim — the shared kineticMachine factory, the four tier blocks multi-attached. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ROLLBENDER_BE =
			BLOCK_ENTITY_TYPES.register("rollbender", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ROLLBENDER_BE.get(), aPos, aState),
					rollbenderBlockArray()).build(null));

	/** The Roll Former family BET: the Wiremill shape verbatim. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ROLLFORMER_BE =
			BLOCK_ENTITY_TYPES.register("rollformer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ROLLFORMER_BE.get(), aPos, aState),
					rollformerBlockArray()).build(null));

	/** The Cluster Mill family BET: the Wiremill shape verbatim. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CLUSTERMILL_BE =
			BLOCK_ENTITY_TYPES.register("clustermill", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.CLUSTERMILL_BE.get(), aPos, aState),
					clustermillBlockArray()).build(null));

	// the P29 W1 EU/HU families (task p29-w1-eu-hu-families) — six machine families
	// plus the Mixer kinetic ladder, all MachineRow carriers over ONE family BET each
	// (the W1-trio shape; the registration rows are the upstream lines verbatim):
	//
	// Mixer RU 20181-84 (Loader_MultiTileEntities.java:1392-1395, Kinetic_T[1..4],
	//   NBT_TEXTURE "mixer", RM.Mixer, NBT_PARALLEL 4/8/16/32 + DURATION T) and
	//   ElectricMixer EU 20351-54 (:1504-1508, MultiTileEntityBasicMachineElectric,
	//   Electric_T[1..4], NBT_TEXTURE "electricmixer", RM.Mixer, NBT_EFFICIENCY 5000)
	//   are the SHARED-MAP PAIR: both rows ride the ONE GT6RecipeMaps.MIXER map — the
	//   kinetic row runs it at efficiency null (= the :96 default 10000, the units()
	//   identity) while the electric row pays the :1504 plug-in convenience at 5000.
	//   Efficiency direction (the card-A review erratum, UT.java:1677 + LH.java:311
	//   double evidence): units(a, orig, targ) = a × targ/orig, so 5000 = 2× the
	//   REQUIRED progress = HALF SPEED / 2× the energy consumption per process —
	//   NOT a 2× speed-up; the same wall-clock sits at exactly half the bar.
	//   IO masks (:1392 verbatim, the port bytes carry the post-read OR-SBIT_A form):
	//   item+tank in = SBIT_L|SBIT_U, out = SBIT_R|SBIT_B, item auto left/right,
	//   tank auto in top / out back, energy = SBIT_D.
	// ElectricLoom 20361-64 (:1511-1515): EU, RM.Loom, efficiency 5000, no parallel,
	//   item top-in/bottom-out (auto top/bottom), energy = SBIT_L|SBIT_R (BOTH side
	//   faces, the only such row in the family), zero tank keys → the 127 default.
	// ElectricSifter 20371-74 (:1518-1522): EU, the SHARED RM.Sifting map (the
	//   kinetic sifter rows :1312-1315 and the p26-w1 trio pour feed the same map),
	//   efficiency 5000, no parallel, loom masks but energy = SBIT_B.
	// Boxinator 20581-84 (:1635-1639) / Unboxinator 20591-94 (:1642-1646): EU,
	//   RM.Boxinator / RM.Unboxinator, NO NBT_EFFICIENCY key → the 10000 identity
	//   (the 26-arg overload), item in = SBIT_L|SBIT_U, out = SBIT_R, auto left/
	//   right, energy = SBIT_D, no parallel, no tanks. The Unboxinator map is the
	//   base-RecipeMap form — the upstream RecipeMapUnboxinator.java:43-89 loot
	//   runtime-synthesis arm is POOLED (the SHREDDER/CHISEL subclass-judgement
	//   precedent, documented on the GT6RecipeMaps.UNBOXINATOR field).
	// Fermenter 22003 (:1654): the SINGLE-VARIANT HU machine — StainlessSteel, NBT_
	//   INPUT 32 / NBT_INPUT_MIN 16 / NBT_INPUT_MAX 64, which is EXACTLY
	//   TIER_INPUTS[0] = {16, 32, 64} (the tier-0 row of the shared table, the
	//   upstream explicit window folds into the tier-0 assignment), RM.Fermenter,
	//   item+tank in = SBIT_B|SBIT_L, item out = SBIT_R, tank out = SBIT_U, item
	//   auto left/right, tank auto in back / out top, energy = SBIT_D (the burning
	//   box HU feed enters the bottom face — the p13 boiler adjacency form).
	//
	// DECLARED DEVIATION (decisions.p29-w1-split-rulings): the upstream Electric*
	// ladders are FIVE tiers (VN[1..5], the :1508/:1515/:1522/:1639/:1646 T5 rows
	// with NBT_INPUT 8192) — this repo runs the 4-ladder rule, so the five T5 rows
	// STAY POOLED (the Canner T5 :1383 missing-row precedent, same ledger); the
	// 5-tier MachineRow rollout rides the W2 Electrolyzer card.
	//
	// KJS face of this card: the registration rows (4 Mixer kinetic + 20 Electric
	// rows + 1 Fermenter) plus the datapack domain — the LOOM/BOXINATOR/UNBOXINATOR/
	// FERMENTER smoke rows ride the GT6RecipeMapJsonLoader direct-pour seam
	// (data/gt6/recipe_maps/<map>.json); MIXER and SIFTING reuse their existing
	// static rows (GT6RecipesMixer / GT6RecipesSifter). NO KubeJS surface.
	// ---------------------------------------------------------------------------

	/** The Mixer family unit word (the W1 trio one-slot key form; upstream "Mixer ("+aMat.getLocal()+")", Loader:1392-1395). */
	public static final String MACHINE_MIXER_UNIT_KEY = "gt6.row.machine.mixer";
	/** The Electric Mixer family display template (the voltage-word slot; upstream "Electric Mixer ("+VN[tier]+")", :1504-1508). */
	public static final String ELECTRIC_MIXER_DISPLAY_KEY = "gt6.row.electricmixer.display";
	/** The Electric Loom family display template (upstream "Electric Loom ("+VN[tier]+")", :1511-1515). */
	public static final String ELECTRIC_LOOM_DISPLAY_KEY = "gt6.row.electricloom.display";
	/** The Electric Sifter family display template (upstream "Electric Sifter ("+VN[tier]+")", :1518-1522). */
	public static final String ELECTRIC_SIFTER_DISPLAY_KEY = "gt6.row.electricsifter.display";
	/** The Boxinator family display template (upstream "Boxinator ("+VN[tier]+")", :1635-1639). */
	public static final String BOXINATOR_DISPLAY_KEY = "gt6.row.boxinator.display";
	/** The Unboxinator family display template (upstream "Unboxinator ("+VN[tier]+")", :1642-1646). */
	public static final String UNBOXINATOR_DISPLAY_KEY = "gt6.row.unboxinator.display";
	/** The Fermenter atomic display template — the single-variant row has NO slot (upstream name column "Fermenter", :1654). */
	public static final String FERMENTER_DISPLAY_KEY = "gt6.row.fermenter.display";

	/** The Fermenter housing material (upstream MT.StainlessSteel, :1654 — the lazy-supplier form, the GTWireSpecs:35 ruling). */
	public static final java.util.function.Supplier<OreDictMaterial> FERMENTER_MATERIAL = () -> gregapi.data.MT.StainlessSteel;

	/** The NBT_EFFICIENCY column of every Electric* row (:1504-1522) — the 5000 = half-speed/2×-energy face (the card-A erratum direction). */
	public static final int ELECTRIC_EFFICIENCY = 5000;

	/** The four Mixer rows, upstream line order :1392-1395 (T1-T4, the Kinetic_T ladder, RU). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> MIXER_ROWS = java.util.List.of(
			mixer("mixer"   , "bronze"       , "Bronze"       , 20181,  7.0F, 0, PARALLEL_4_32[0]),
			mixer("mixer_t2", "steel"        , "Steel"        , 20182,  6.0F, 1, PARALLEL_4_32[1]),
			mixer("mixer_t3", "titanium"     , "Titanium"     , 20183,  9.0F, 2, PARALLEL_4_32[2]),
			mixer("mixer_t4", "tungstensteel", "Tungstensteel", 20184, 12.5F, 3, PARALLEL_4_32[3]));

	/** The four Electric Mixer rows, upstream line order :1504-1507 (T1-T4, the Electric_T ladder, EU, efficiency 5000, the SHARED RM.Mixer map). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ELECTRIC_MIXER_ROWS = java.util.List.of(
			electricMixer("electricmixer"   , "lv", "LV", 20351, 0),
			electricMixer("electricmixer_t2", "mv", "MV", 20352, 1),
			electricMixer("electricmixer_t3", "hv", "HV", 20353, 2),
			electricMixer("electricmixer_t4", "ev", "EV", 20354, 3));

	/** The four Electric Loom rows, upstream line order :1511-1514 (T1-T4, EU, RM.Loom, efficiency 5000, no parallel). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ELECTRIC_LOOM_ROWS = java.util.List.of(
			electricLoom("electricloom"   , "lv", "LV", 20361, 0),
			electricLoom("electricloom_t2", "mv", "MV", 20362, 1),
			electricLoom("electricloom_t3", "hv", "HV", 20363, 2),
			electricLoom("electricloom_t4", "ev", "EV", 20364, 3));

	/** The four Electric Sifter rows, upstream line order :1518-1521 (T1-T4, EU, the SHARED RM.Sifting map, efficiency 5000, no parallel). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ELECTRIC_SIFTER_ROWS = java.util.List.of(
			electricSifter("electricsifter"   , "lv", "LV", 20371, 0),
			electricSifter("electricsifter_t2", "mv", "MV", 20372, 1),
			electricSifter("electricsifter_t3", "hv", "HV", 20373, 2),
			electricSifter("electricsifter_t4", "ev", "EV", 20374, 3));

	/** The four Boxinator rows, upstream line order :1635-1638 (T1-T4, EU, RM.Boxinator, NO efficiency key → the 26-arg overload). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> BOXINATOR_ROWS = java.util.List.of(
			boxinator("boxinator"   , "lv", "LV", 20581, 0),
			boxinator("boxinator_t2", "mv", "MV", 20582, 1),
			boxinator("boxinator_t3", "hv", "HV", 20583, 2),
			boxinator("boxinator_t4", "ev", "EV", 20584, 3));

	/** The four Unboxinator rows, upstream line order :1642-1645 (T1-T4, EU, RM.Unboxinator, NO efficiency key → the 26-arg overload). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> UNBOXINATOR_ROWS = java.util.List.of(
			unboxinator("unboxinator"   , "lv", "LV", 20591, 0),
			unboxinator("unboxinator_t2", "mv", "MV", 20592, 1),
			unboxinator("unboxinator_t3", "hv", "HV", 20593, 2),
			unboxinator("unboxinator_t4", "ev", "EV", 20594, 3));

	/** The ONE Fermenter row (upstream :1654 — the single-variant HU machine, the window 16/32/64 = TIER_INPUTS[0]). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> FERMENTER_ROWS = java.util.List.of(fermenter());

	/**
	 * One Mixer row factory — the differing columns (path/name/id/material/hardness/tier/
	 * parallel) plus the family constants: the SHARED RM.Mixer map, RU, the "mixer"
	 * texture, the :1392 masks (item+tank in left|top, out right|bottom, energy bottom,
	 * tank auto in top / out back), parallelDuration T (the NBT_PARALLEL_DURATION
	 * column), the null efficiency column (the 26-arg overload — no NBT_EFFICIENCY key,
	 * the :96 10000 identity) and the null menu supplier (zero new MenuType).
	 */
	private static GTBasicMachineBlock.MachineRow mixer(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_MIXER_UNIT_KEY, aMetaId, aHardness, aTier,
				aParallel, true /*NBT_PARALLEL_DURATION T :1392*/,
				() -> GT6RecipeMaps.MIXER, TD.Energy.RU, "mixer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D, the :151 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_B*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)5 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BACK*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/**
	 * One Electric Mixer row factory — the mixer() masks verbatim over the EU family
	 * constants: Electric_T material, the voltage-word slot, hardness 4.0 (the :1504
	 * column on all four rows), the SHARED RM.Mixer map, the "electricmixer" texture and
	 * the NBT_EFFICIENCY 5000 column (the 27-arg canonical form) — HALF SPEED, 2× the
	 * energy-time per process (the units() direction, the card-A erratum).
	 */
	private static GTBasicMachineBlock.MachineRow electricMixer(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, ELECTRIC_T_LADDER.get(aTier), ELECTRIC_MIXER_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				PARALLEL_4_32[aTier], true /*NBT_PARALLEL_DURATION T :1504*/,
				() -> GT6RecipeMaps.MIXER, TD.Energy.EU, "electricmixer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_B*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)5 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BACK*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false,
				ELECTRIC_EFFICIENCY /*NBT_EFFICIENCY 5000 :1504-1507 — half-speed face*/);
	}

	/**
	 * One Electric Loom row factory — the :1511 masks: item top-in/bottom-out (auto
	 * top/bottom), energy = SBIT_L|SBIT_R (BOTH side faces), ZERO tank keys → the
	 * 127/-1 no-tank face (the zero-fluid map, the Shredder precedent), no parallel,
	 * efficiency 5000, RM.Loom, the "electricloom" texture.
	 */
	private static GTBasicMachineBlock.MachineRow electricLoom(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, ELECTRIC_T_LADDER.get(aTier), ELECTRIC_LOOM_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1511-1514*/,
				() -> GT6RecipeMaps.LOOM, TD.Energy.EU, "electricloom",
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_L|SBIT_R — both sides*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U — top in*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D — bottom out*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				null /*the menu-less carrier*/, true, null, false,
				ELECTRIC_EFFICIENCY /*NBT_EFFICIENCY 5000 :1511-1514*/);
	}

	/** One Electric Sifter row factory — the electricLoom shape over the :1518 masks (energy SBIT_B) and the SHARED RM.Sifting map. */
	private static GTBasicMachineBlock.MachineRow electricSifter(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, ELECTRIC_T_LADDER.get(aTier), ELECTRIC_SIFTER_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1518-1521*/,
				() -> GT6RecipeMaps.SIFTING, TD.Energy.EU, "electricsifter",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U — top in*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D — bottom out*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				null /*the menu-less carrier*/, true, null, false,
				ELECTRIC_EFFICIENCY /*NBT_EFFICIENCY 5000 :1518-1521*/);
	}

	/**
	 * One Boxinator row factory — the :1635 masks: item in = left|top (auto left), out =
	 * right (auto right), energy = bottom, zero tank keys, no parallel, NO
	 * NBT_EFFICIENCY key → the 26-arg overload (the 10000 identity, the zero-drift face).
	 */
	private static GTBasicMachineBlock.MachineRow boxinator(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, ELECTRIC_T_LADDER.get(aTier), BOXINATOR_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1635-1638*/,
				() -> GT6RecipeMaps.BOXINATOR, TD.Energy.EU, "boxinator",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** One Unboxinator row factory — the boxinator shape verbatim over the :1642 masks and RM.Unboxinator. */
	private static GTBasicMachineBlock.MachineRow unboxinator(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, ELECTRIC_T_LADDER.get(aTier), UNBOXINATOR_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1642-1645*/,
				() -> GT6RecipeMaps.UNBOXINATOR, TD.Energy.EU, "unboxinator",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/**
	 * The Fermenter row factory — the SINGLE-variant HU machine (:1654): StainlessSteel
	 * housing, hardness 6.0, the tier-0 row of the SHARED TIER_INPUTS table (the upstream
	 * explicit NBT_INPUT 32 / MIN 16 / MAX 64 window IS TIER_INPUTS[0] = {16, 32, 64}),
	 * the :1654 masks (item+tank in back|left, item out right, tank out top, tank auto in
	 * back / out top, energy bottom), no parallel, no efficiency key → the 26-arg overload.
	 */
	private static GTBasicMachineBlock.MachineRow fermenter() {
		return new GTBasicMachineBlock.MachineRow("fermenter", "stainless_steel", "StainlessSteel", FERMENTER_MATERIAL, FERMENTER_DISPLAY_KEY, 22003, 6.0F,
				0, 1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1654*/,
				() -> GT6RecipeMaps.FERMENTER, TD.Energy.HU, "fermenter",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D — the burning box feeds the bottom face*/,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_B|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_B|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)5 /*NBT_TANK_SIDE_AUTO_IN SIDE_BACK*/, (byte)1 /*NBT_TANK_SIDE_AUTO_OUT SIDE_TOP*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/** The registered Mixer blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> MIXER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Mixer items, same keys as {@link #MIXER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> MIXER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Electric Mixer blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTRIC_MIXER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Electric Mixer items. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTRIC_MIXER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Electric Loom blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTRIC_LOOM_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Electric Loom items. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTRIC_LOOM_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Electric Sifter blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTRIC_SIFTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Electric Sifter items. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTRIC_SIFTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Boxinator blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> BOXINATOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Boxinator items. */
	public static final java.util.Map<String, RegistryObject<Item>> BOXINATOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Unboxinator blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> UNBOXINATOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Unboxinator items. */
	public static final java.util.Map<String, RegistryObject<Item>> UNBOXINATOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Fermenter blocks by path (the single-variant ladder). */
	public static final java.util.Map<String, RegistryObject<Block>> FERMENTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Fermenter items. */
	public static final java.util.Map<String, RegistryObject<Item>> FERMENTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : MIXER_ROWS) {
			MIXER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.MIXER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			MIXER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.MIXER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : ELECTRIC_MIXER_ROWS) {
			ELECTRIC_MIXER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ELECTRIC_MIXER_BE.get(), tRow)));
			ELECTRIC_MIXER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ELECTRIC_MIXER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : ELECTRIC_LOOM_ROWS) {
			ELECTRIC_LOOM_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ELECTRIC_LOOM_BE.get(), tRow)));
			ELECTRIC_LOOM_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ELECTRIC_LOOM_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : ELECTRIC_SIFTER_ROWS) {
			ELECTRIC_SIFTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ELECTRIC_SIFTER_BE.get(), tRow)));
			ELECTRIC_SIFTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ELECTRIC_SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : BOXINATOR_ROWS) {
			BOXINATOR_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.BOXINATOR_BE.get(), tRow)));
			BOXINATOR_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.BOXINATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : UNBOXINATOR_ROWS) {
			UNBOXINATOR_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.UNBOXINATOR_BE.get(), tRow)));
			UNBOXINATOR_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.UNBOXINATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : FERMENTER_ROWS) {
			FERMENTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.FERMENTER_BE.get(), tRow)));
			FERMENTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.FERMENTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Mixer block list in registration order (the loot/datagen walkers). */
	public static Block[] mixerBlockArray() {
		Block[] rBlocks = new Block[MIXER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : MIXER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Electric Mixer block list in registration order. */
	public static Block[] electricMixerBlockArray() {
		Block[] rBlocks = new Block[ELECTRIC_MIXER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ELECTRIC_MIXER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Electric Loom block list in registration order. */
	public static Block[] electricLoomBlockArray() {
		Block[] rBlocks = new Block[ELECTRIC_LOOM_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ELECTRIC_LOOM_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Electric Sifter block list in registration order. */
	public static Block[] electricSifterBlockArray() {
		Block[] rBlocks = new Block[ELECTRIC_SIFTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ELECTRIC_SIFTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Boxinator block list in registration order. */
	public static Block[] boxinatorBlockArray() {
		Block[] rBlocks = new Block[BOXINATOR_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : BOXINATOR_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Unboxinator block list in registration order. */
	public static Block[] unboxinatorBlockArray() {
		Block[] rBlocks = new Block[UNBOXINATOR_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : UNBOXINATOR_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Fermenter block list in registration order (the single-variant ladder). */
	public static Block[] fermenterBlockArray() {
		Block[] rBlocks = new Block[FERMENTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : FERMENTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine mixer — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block mixerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = MIXER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine electricmixer — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block electricMixerBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ELECTRIC_MIXER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine electricloom — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block electricLoomBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ELECTRIC_LOOM_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine electricsifter — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block electricSifterBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ELECTRIC_SIFTER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine boxinator — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block boxinatorBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = BOXINATOR_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine unboxinator — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block unboxinatorBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = UNBOXINATOR_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine fermenter — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block fermenterBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = FERMENTER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// the seven eu-hu family BETs: the W1-trio shape verbatim — ONE family BET, the four
	// (or one) tier blocks multi-attached, the row read off the placed block by the
	// SHARED kineticMachine factory body (the row carries every column these families
	// need; applyRow inside lands the masks AND the efficiency column on the BE).

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> MIXER_BE =
			BLOCK_ENTITY_TYPES.register("mixer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.MIXER_BE.get(), aPos, aState),
					mixerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ELECTRIC_MIXER_BE =
			BLOCK_ENTITY_TYPES.register("electricmixer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ELECTRIC_MIXER_BE.get(), aPos, aState),
					electricMixerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ELECTRIC_LOOM_BE =
			BLOCK_ENTITY_TYPES.register("electricloom", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ELECTRIC_LOOM_BE.get(), aPos, aState),
					electricLoomBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ELECTRIC_SIFTER_BE =
			BLOCK_ENTITY_TYPES.register("electricsifter", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ELECTRIC_SIFTER_BE.get(), aPos, aState),
					electricSifterBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> BOXINATOR_BE =
			BLOCK_ENTITY_TYPES.register("boxinator", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.BOXINATOR_BE.get(), aPos, aState),
					boxinatorBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> UNBOXINATOR_BE =
			BLOCK_ENTITY_TYPES.register("unboxinator", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.UNBOXINATOR_BE.get(), aPos, aState),
					unboxinatorBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> FERMENTER_BE =
			BLOCK_ENTITY_TYPES.register("fermenter", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.FERMENTER_BE.get(), aPos, aState),
					fermenterBlockArray()).build(null));

	// ---------------------------------------------------------------------------
	// the P29 W2 exotic-energy families (task p29-w2-exotic-energy) — six machine families
	// over the THREE exotic energy domains, the first machine consumers of MU/LU/CU in this
	// repo (the supply side is the /gt6energy source-block dial, GTEnergySourceBlockEntity
	// .resolveEnergyType :293-308 — the p29-w2-energy-types-5tier precedent chain; the
	// generators stay deferred, no upstream source exists, the S9 ruling):
	//
	//   Polarizer         20221-20225  MU  RM.Polarizer          :1418-1422  parallel 1
	//   MagneticSeparator 20301-20305  MU  RM.MagneticSeparator  :1470-1474  parallel 1
	//   LaserEngraver     20321-20325  LU  RM.LaserEngraver      :1483-1487  parallel 1
	//   LaserWelder       20331-20335  LU  RM.Welder             :1490-1494  parallel 1
	//   Freezer           20561-20565  CU  RM.Freezer            :1621-1625  parallel 1
	//   CryoMixer         20571-20575  CU  RM.CryoMixer          :1628-1632  CRYO_PARALLEL
	//
	// ALL rows are the plain MultiTileEntityBasicMachine base-class form (NOT the Electric
	// subclass) riding the Electric_T material ladder — the decisions.p29-w2-split-rulings
	// energy-domain/material-decoupling shape: NBT_INPUT 32/128/512/2048/8192 through the
	// :126 conversion = TIER_INPUTS[0..3] for the T1-T4 rungs and EV_TIER_INPUTS
	// {4096, 8192, 16384} for the T5 rung (the 5-tier 立行制 first CONSUMERS — card ①
	// declared both constants ahead of this card). Hardness 4.0 on every row (the
	// Electric_T column); cheap overclocking T (the :773 unconditional port face); menu =
	// null on every row (the zero-new-MenuType GUI clause); no NBT_EFFICIENCY key (the
	// plain base class carries none — the 26-arg MachineRow overload, the 10000 identity).
	//
	// The NAME columns split two ways (the upstream verbatim, the reason the row factories
	// take the display word pair instead of riding one ladder):
	//   Polarizer / Magnetic Separator ride the MATERIAL word
	//   ("Polarizer ("+aMat.getLocal()+")" :1418 — Electric_T[1..5] = Galvanized Steel /
	//   Aluminium / Stainless Steel / Chromium / Titanium, MT.java:3691 setLocal faces);
	//   Laser Engraver / Laser Welder / Freezer / Cryo Mixer ride the LITERAL TIER WORD
	//   "(T1)".."(T5)" (:1483/:1490/:1621/:1628 — NOT a voltage word; the declaration-
	//   fidelity column, the gt6.row.mat.t1..t5 units are the bare ordinals).
	//
	// The registry paths keep the snake-case family names while the NBT_TEXTURE tokens
	// stay the upstream camel-joined art tokens ("magneticseparator"/"laserengraver"/
	// "laserwelder"/"cryomixer" — the pressure_washer/debarker art-token-fidelity form).
	//
	// The connectivity masks are the upstream rows verbatim (the row bytes carry the
	// post-read values, the :137/:138/:143/:144/:151 reads OR SBIT_A onto every keyed
	// mask; unkeyed masks ride the field default 127 / SIDE_UNDEFINED -1). The energy
	// faces are the card's whole point: SBIT_U|SBIT_D (Polarizer), SBIT_U (Magnetic
	// Separator / both Laser machines), SBIT_B (Freezer, the back face), SBIT_D (CryoMixer)
	// — the LIVE :501 reference-equality type gate (isEnergyType, the :803 face) is what
	// makes a MU row refuse an EU/RU packet, the acceptance-① arms drive exactly that.
	//
	// The crafting-table recipe tails (the TwT/PMP/... housing frames, the Laser Welder's
	// DYE_OREDICTS_LENS[Yellow] lens, the Freezer/CryoMixer Si+StainlessSteel plate
	// ladder) are the unported crafting domain — data-only, no row column (the Buzzsaw
	// crafting-head precedent); the LENS ITEM FAMILY is pooled by the card ruling (the
	// smoke rows carry no lens dependency, the recipe rows are declarations).
	//
	// KJS face of this card (the wave-plan declaration): REGISTRATION face only — six
	// families, THIRTY MachineRow rows, the MU/LU/CU domains' first machine consumers —
	// plus the datapack face (the six recipe-map smoke rows under data/gt6/recipe_maps/
	// through the card-① POURABLE keys). No KubeJS surface. The low-temperature fluid
	// dependency stays ROW-LEVEL (the smoke rows ride in-registry fluids; the chemical
	// fluid batch F follows).
	// ---------------------------------------------------------------------------

	/** The Polarizer family unit word (the one-slot key form; the upstream name column "Polarizer ("+aMat.getLocal()+")", :1418-1422). */
	public static final String MACHINE_POLARIZER_UNIT_KEY = "gt6.row.machine.polarizer";

	/** The Magnetic Separator family unit word (the :1470-1474 name column "Magnetic Separator ("). */
	public static final String MACHINE_MAGNETIC_SEPARATOR_UNIT_KEY = "gt6.row.machine.magnetic_separator";

	/** The Laser Engraver family unit word (the :1483-1487 LITERAL tier-word column "Laser Engraver (T1..T5)" — declaration fidelity, no voltage word). */
	public static final String MACHINE_LASER_ENGRAVER_UNIT_KEY = "gt6.row.machine.laser_engraver";

	/** The Laser Welder family unit word (the :1490-1494 literal tier-word column "Laser Welder (T1..T5)"). */
	public static final String MACHINE_LASER_WELDER_UNIT_KEY = "gt6.row.machine.laser_welder";

	/** The Freezer family unit word (the :1621-1625 literal tier-word column "Freezer (T1..T5)"). */
	public static final String MACHINE_FREEZER_UNIT_KEY = "gt6.row.machine.freezer";

	/** The Cryo Mixer family unit word (the :1628-1632 literal tier-word column "Cryo Mixer (T1..T5)" — the two-word form verbatim). */
	public static final String MACHINE_CRYO_MIXER_UNIT_KEY = "gt6.row.machine.cryo_mixer";

	/** The literal tier-word rungs of the four T-named families (the "(T1)".."(T5)" name columns — the gt6.row.mat.t1..t5 units). */
	public static final java.util.List<String> TIER_WORD_SLUGS = java.util.List.of("t1", "t2", "t3", "t4", "t5");

	/** The display words of the {@link #TIER_WORD_SLUGS} rungs (the upstream "(T1)".."(T5)" verbatim). */
	public static final java.util.List<String> TIER_WORD_DISPLAYS = java.util.List.of("T1", "T2", "T3", "T4", "T5");

	/** The five Polarizer rows, upstream line order :1418-1422 (T1-T5, the Electric_T material word). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> POLARIZER_ROWS = java.util.List.of(
			polarizer("polarizer"   , "galvanized_steel", "Galvanized Steel", 20221, 0),
			polarizer("polarizer_t2", "aluminium"       , "Aluminium"       , 20222, 1),
			polarizer("polarizer_t3", "stainless_steel" , "Stainless Steel" , 20223, 2),
			polarizer("polarizer_t4", "chromium"        , "Chromium"        , 20224, 3),
			polarizer("polarizer_t5", "titanium"        , "Titanium"        , 20225, 4));

	/** The five Magnetic Separator rows, upstream line order :1470-1474 (T1-T5, the Electric_T material word). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> MAGNETIC_SEPARATOR_ROWS = java.util.List.of(
			magneticSeparator("magnetic_separator"   , "galvanized_steel", "Galvanized Steel", 20301, 0),
			magneticSeparator("magnetic_separator_t2", "aluminium"       , "Aluminium"       , 20302, 1),
			magneticSeparator("magnetic_separator_t3", "stainless_steel" , "Stainless Steel" , 20303, 2),
			magneticSeparator("magnetic_separator_t4", "chromium"        , "Chromium"        , 20304, 3),
			magneticSeparator("magnetic_separator_t5", "titanium"        , "Titanium"        , 20305, 4));

	/** The five Laser Engraver rows, upstream line order :1483-1487 (T1-T5, the LITERAL tier word; the Electric_T material stays the NBT_MATERIAL/tint column). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> LASER_ENGRAVER_ROWS = java.util.List.of(
			laserEngraver("laser_engraver"   , 20321, 0),
			laserEngraver("laser_engraver_t2", 20322, 1),
			laserEngraver("laser_engraver_t3", 20323, 2),
			laserEngraver("laser_engraver_t4", 20324, 3),
			laserEngraver("laser_engraver_t5", 20325, 4));

	/** The five Laser Welder rows, upstream line order :1490-1494 (T1-T5, the literal tier word; the RM.Welder map is the upstream recipe-map column verbatim). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> LASER_WELDER_ROWS = java.util.List.of(
			laserWelder("laser_welder"   , 20331, 0),
			laserWelder("laser_welder_t2", 20332, 1),
			laserWelder("laser_welder_t3", 20333, 2),
			laserWelder("laser_welder_t4", 20334, 3),
			laserWelder("laser_welder_t5", 20335, 4));

	/** The five Freezer rows, upstream line order :1621-1625 (T1-T5, the literal tier word). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> FREEZER_ROWS = java.util.List.of(
			freezer("freezer"   , 20561, 0),
			freezer("freezer_t2", 20562, 1),
			freezer("freezer_t3", 20563, 2),
			freezer("freezer_t4", 20564, 3),
			freezer("freezer_t5", 20565, 4));

	/** The five Cryo Mixer rows, upstream line order :1628-1632 (T1-T5; the CRYO_PARALLEL {4,8,16,32,64} + duration-T ladder, the card-① constant's first consumer). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CRYO_MIXER_ROWS = java.util.List.of(
			cryoMixer("cryo_mixer"   , 20571, 0),
			cryoMixer("cryo_mixer_t2", 20572, 1),
			cryoMixer("cryo_mixer_t3", 20573, 2),
			cryoMixer("cryo_mixer_t4", 20574, 3),
			cryoMixer("cryo_mixer_t5", 20575, 4));

	/**
	 * The material/word pair of one exotic row — the Electric_T rung by tier index (the
	 * T1-T4 rungs ride {@link #ELECTRIC_T_LADDER}, the T5 rung {@link #ELECTRIC_T5}); the
	 * material-word families pass the slug/display pair, the tier-word families the
	 * {@link #TIER_WORD_SLUGS}/{@link #TIER_WORD_DISPLAYS} entry.
	 */
	private static java.util.function.Supplier<OreDictMaterial> electricTRung(int aTier) {
		return aTier < ELECTRIC_T_LADDER.size() ? ELECTRIC_T_LADDER.get(aTier) : ELECTRIC_T5;
	}

	/**
	 * One Polarizer row factory — the differing columns (path/id/tier) plus the family
	 * constants: the material-word slot, MU (NBT_ENERGY_ACCEPTED :1418), the "polarizer"
	 * texture, the :1418 masks (item left-in/right-out, energy top|bottom, NO tank keys —
	 * the POLARIZER map is 0/0/0 fluids) and parallel 1 / duration F (no NBT keys).
	 */
	private static GTBasicMachineBlock.MachineRow polarizer(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, electricTRung(aTier), MACHINE_POLARIZER_UNIT_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1418-1422*/,
				() -> GT6RecipeMaps.POLARIZER, TD.Energy.MU, "polarizer",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U|SBIT_D, the :151 OR*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/**
	 * One Magnetic Separator row factory — the :1470 masks: item left-in/right|bottom-out,
	 * tank in LEFT (auto left) / out right|bottom (auto bottom), energy TOP.
	 */
	private static GTBasicMachineBlock.MachineRow magneticSeparator(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, electricTRung(aTier), MACHINE_MAGNETIC_SEPARATOR_UNIT_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1470-1474*/,
				() -> GT6RecipeMaps.MAGNETIC_SEPARATOR, TD.Energy.MU, "magneticseparator",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)2 /*NBT_TANK_SIDE_AUTO_IN SIDE_LEFT*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** One Laser Engraver row factory — the :1483 masks (item left-in/right-out, energy top, no tanks) over the LITERAL tier-word slot and the LU carrier. */
	private static GTBasicMachineBlock.MachineRow laserEngraver(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, TIER_WORD_SLUGS.get(aTier), TIER_WORD_DISPLAYS.get(aTier), electricTRung(aTier), MACHINE_LASER_ENGRAVER_UNIT_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1483-1487*/,
				() -> GT6RecipeMaps.LASER_ENGRAVER, TD.Energy.LU, "laserengraver",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** One Laser Welder row factory — the :1490 masks (tank in down|left auto BOTTOM, NO tank-out key — the WELDER map is 1/0/0 fluids) over the LU carrier and RM.Welder. */
	private static GTBasicMachineBlock.MachineRow laserWelder(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, TIER_WORD_SLUGS.get(aTier), TIER_WORD_DISPLAYS.get(aTier), electricTRung(aTier), MACHINE_LASER_WELDER_UNIT_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1490-1494*/,
				() -> GT6RecipeMaps.WELDER, TD.Energy.LU, "laserwelder",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_D|SBIT_L*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)0 /*NBT_TANK_SIDE_AUTO_IN SIDE_BOTTOM*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** One Freezer row factory — the :1621 masks (item+tank in up|left auto TOP, out right|bottom auto BOTTOM, energy BACK) over the CU carrier. */
	private static GTBasicMachineBlock.MachineRow freezer(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, TIER_WORD_SLUGS.get(aTier), TIER_WORD_DISPLAYS.get(aTier), electricTRung(aTier), MACHINE_FREEZER_UNIT_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1621-1625*/,
				() -> GT6RecipeMaps.FREEZER, TD.Energy.CU, "freezer",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B — the back face*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** One Cryo Mixer row factory — the :1628 masks (item+tank in left|top auto LEFT/TOP, out right|bottom auto RIGHT/BACK, energy BOTTOM) over the CRYO_PARALLEL duration-T ladder. */
	private static GTBasicMachineBlock.MachineRow cryoMixer(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, TIER_WORD_SLUGS.get(aTier), TIER_WORD_DISPLAYS.get(aTier), electricTRung(aTier), MACHINE_CRYO_MIXER_UNIT_KEY, aMetaId, 4.0F, aTier,
				CRYO_PARALLEL[aTier], true /*NBT_PARALLEL_DURATION T :1628-1632*/,
				() -> GT6RecipeMaps.CRYO_MIXER, TD.Energy.CU, "cryomixer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_B*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)5 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BACK*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** The registered Polarizer blocks by path (the BET/datagen/loot walkers). */
	public static final java.util.Map<String, RegistryObject<Block>> POLARIZER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Polarizer items. */
	public static final java.util.Map<String, RegistryObject<Item>> POLARIZER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Magnetic Separator blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> MAGNETIC_SEPARATOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Magnetic Separator items. */
	public static final java.util.Map<String, RegistryObject<Item>> MAGNETIC_SEPARATOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Laser Engraver blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> LASER_ENGRAVER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Laser Engraver items. */
	public static final java.util.Map<String, RegistryObject<Item>> LASER_ENGRAVER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Laser Welder blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> LASER_WELDER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Laser Welder items. */
	public static final java.util.Map<String, RegistryObject<Item>> LASER_WELDER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Freezer blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> FREEZER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Freezer items. */
	public static final java.util.Map<String, RegistryObject<Item>> FREEZER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Cryo Mixer blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> CRYO_MIXER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Cryo Mixer items. */
	public static final java.util.Map<String, RegistryObject<Item>> CRYO_MIXER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		registerExoticFamily(POLARIZER_ROWS, POLARIZER_BLOCKS_BY_PATH, POLARIZER_ITEMS_BY_PATH, () -> GTMachines.POLARIZER_BE);
		registerExoticFamily(MAGNETIC_SEPARATOR_ROWS, MAGNETIC_SEPARATOR_BLOCKS_BY_PATH, MAGNETIC_SEPARATOR_ITEMS_BY_PATH, () -> GTMachines.MAGNETIC_SEPARATOR_BE);
		registerExoticFamily(LASER_ENGRAVER_ROWS, LASER_ENGRAVER_BLOCKS_BY_PATH, LASER_ENGRAVER_ITEMS_BY_PATH, () -> GTMachines.LASER_ENGRAVER_BE);
		registerExoticFamily(LASER_WELDER_ROWS, LASER_WELDER_BLOCKS_BY_PATH, LASER_WELDER_ITEMS_BY_PATH, () -> GTMachines.LASER_WELDER_BE);
		registerExoticFamily(FREEZER_ROWS, FREEZER_BLOCKS_BY_PATH, FREEZER_ITEMS_BY_PATH, () -> GTMachines.FREEZER_BE);
		registerExoticFamily(CRYO_MIXER_ROWS, CRYO_MIXER_BLOCKS_BY_PATH, CRYO_MIXER_ITEMS_BY_PATH, () -> GTMachines.CRYO_MIXER_BE);
	}

	/**
	 * One exotic family's block+item registration walk — the W1 BY_PATH form (the
	 * qualified-read forward-reference lambda, the P6 lesson: the BET handle travels as a
	 * DEFERRED supplier because the static block textually precedes the BET fields).
	 */
	private static void registerExoticFamily(java.util.List<GTBasicMachineBlock.MachineRow> aRows,
			java.util.Map<String, RegistryObject<Block>> aBlocks, java.util.Map<String, RegistryObject<Item>> aItems,
			java.util.function.Supplier<RegistryObject<BlockEntityType<TileEntityBasicMachine>>> aBe) {
		for (GTBasicMachineBlock.MachineRow tRow : aRows) {
			aBlocks.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> aBe.get().get(), tRow)));
			aItems.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(aBlocks.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Polarizer block list in registration order (the loot/datagen walkers). */
	public static Block[] polarizerBlockArray() {
		return blockArrayOf(POLARIZER_BLOCKS_BY_PATH);
	}

	/** The Magnetic Separator block list in registration order. */
	public static Block[] magneticSeparatorBlockArray() {
		return blockArrayOf(MAGNETIC_SEPARATOR_BLOCKS_BY_PATH);
	}

	/** The Laser Engraver block list in registration order. */
	public static Block[] laserEngraverBlockArray() {
		return blockArrayOf(LASER_ENGRAVER_BLOCKS_BY_PATH);
	}

	/** The Laser Welder block list in registration order. */
	public static Block[] laserWelderBlockArray() {
		return blockArrayOf(LASER_WELDER_BLOCKS_BY_PATH);
	}

	/** The Freezer block list in registration order. */
	public static Block[] freezerBlockArray() {
		return blockArrayOf(FREEZER_BLOCKS_BY_PATH);
	}

	/** The Cryo Mixer block list in registration order. */
	public static Block[] cryoMixerBlockArray() {
		return blockArrayOf(CRYO_MIXER_BLOCKS_BY_PATH);
	}

	/** The BY_PATH walk behind every exotic block array. */
	private static Block[] blockArrayOf(java.util.Map<String, RegistryObject<Block>> aBlocks) {
		Block[] rBlocks = new Block[aBlocks.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : aBlocks.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	// the six exotic family BETs: the W1-trio shape over the ONE shared
	// {@link #exoticMachine} factory body — the row carries every column, the only
	// exotic-specific bit is the tier-4 window arm (EV_TIER_INPUTS).

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> POLARIZER_BE =
			BLOCK_ENTITY_TYPES.register("polarizer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> exoticMachine(GTMachines.POLARIZER_BE.get(), aPos, aState),
					polarizerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> MAGNETIC_SEPARATOR_BE =
			BLOCK_ENTITY_TYPES.register("magnetic_separator", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> exoticMachine(GTMachines.MAGNETIC_SEPARATOR_BE.get(), aPos, aState),
					magneticSeparatorBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LASER_ENGRAVER_BE =
			BLOCK_ENTITY_TYPES.register("laser_engraver", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> exoticMachine(GTMachines.LASER_ENGRAVER_BE.get(), aPos, aState),
					laserEngraverBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LASER_WELDER_BE =
			BLOCK_ENTITY_TYPES.register("laser_welder", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> exoticMachine(GTMachines.LASER_WELDER_BE.get(), aPos, aState),
					laserWelderBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> FREEZER_BE =
			BLOCK_ENTITY_TYPES.register("freezer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> exoticMachine(GTMachines.FREEZER_BE.get(), aPos, aState),
					freezerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CRYO_MIXER_BE =
			BLOCK_ENTITY_TYPES.register("cryo_mixer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> exoticMachine(GTMachines.CRYO_MIXER_BE.get(), aPos, aState),
					cryoMixerBlockArray()).build(null));

	/**
	 * The exotic BET factory body — the kineticMachine body verbatim except the WINDOW
	 * column: the 5-tier rows index BEYOND the 4-row {@link #TIER_INPUTS}, so the tier-4
	 * rung rides {@link #EV_TIER_INPUTS} (the 5-tier 立行制 window {4096, 8192, 16384},
	 * the :126 conversion over NBT_INPUT 8192). The static block above can reference the
	 * BET fields only because this body reads the row off the placed block at BE
	 * CREATION time (the DeferredRegister lazy-supplier order — the P6 lambda lesson).
	 */
	private static TileEntityBasicMachine exoticMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.menu());
		tMachine.mEnergyTypeAccepted = tRow.energyType();
		long[] tInputs = tRow.tier() < TIER_INPUTS.length ? TIER_INPUTS[tRow.tier()] : EV_TIER_INPUTS;
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return applyRow(tMachine, tRow);
	}

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
	 * the 204 machine-domain blocks the client paint BlockColor
	 * registers over — the oven ladder (4) + the shredder/crusher/lathe ladders (4 each = 12) + the
	 * dryer (4) + the distillery (4) + the canner (4) + the sifter/compressor/wiremill
	 * ladders (4 each = 12) + press (4) + extruder (4) + the ULV rows (5 + the rollingmill
	 * rung = 6) + the roll ladders (rollingmill RU t1-t4 + rollbender/rollformer/clustermill
	 * 4 each = 16, task p29-w1-kinetic-roll-ladder) + the six process families
	 * (buzzsaw/squeezer/centrifuge/sluice/sandingmachine/pressurewasher 4 each = 24,
	 * task p29-w1-kinetic-process-ladder) + the eu-hu families (mixer/electricmixer/
	 * electricloom/electricsifter/boxinator/unboxinator 4 each = 24 + the fermenter rung
	 * = 25, task p29-w1-eu-hu-families) + the eu-special families (autocrafter/lightning
	 * 5 each + laminator 4 = 14, task p29-w2-eu-special) + the six exotic-energy families
	 * (polarizer/magneticseparator/laserengraver/laserwelder/freezer/cryomixer 5 each
	 * = 30, task p29-w2-exotic-energy) + the five eu-core families (electrolyzer/injector/
	 * printer/scannervisuals/slicer 5 each = 25, task p29-w2-eu-core-5tier) + the hu-tu
	 * piggyback (steamcracker/catalyticcracker 4 each = 8 + coagulator/generifier/bath/
	 * autoclave 1 each = 4 + loom 4 = 16, task p29-w2-hu-tu-piggyback),
	 * the upstream {@code MultiTileEntityBasicMachine} render census (the getTexture2 :1014
	 * grayscale x mRGBa consumers). Card_A put the paint capability on the 03 base, so the
	 * whole 03 family can carry PAINT model data (barrels/pipes included) — but this card's
	 * v1 registers the tint over exactly this machine array; the family-wide extension
	 * (connectors/barrels/pipes rendering) stays pooled. Client-side call time only.
	 */
	public static Block[] paintableBlockArray() {
		java.util.List<Block> rBlocks = new java.util.ArrayList<>(209);
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
		java.util.Collections.addAll(rBlocks, rollbenderBlockArray()); // task p29-w1-kinetic-roll-ladder — the roll ladders join the paint census
		java.util.Collections.addAll(rBlocks, rollformerBlockArray());
		java.util.Collections.addAll(rBlocks, clustermillBlockArray());
		// task p29-w1-kinetic-process-ladder — the six process families, +24 (the
		// Buzzsaw/Squeezer/Centrifuge/Sluice/SandingMachine/PressureWasher ladders; the
		// census walks in the section order — the paint tint registration mirrors)
		java.util.Collections.addAll(rBlocks, buzzsawBlockArray());
		java.util.Collections.addAll(rBlocks, squeezerBlockArray());
		java.util.Collections.addAll(rBlocks, centrifugeBlockArray());
		java.util.Collections.addAll(rBlocks, sluiceBlockArray());
		java.util.Collections.addAll(rBlocks, sandingBlockArray());
		java.util.Collections.addAll(rBlocks, pressurewasherBlockArray());
		// task p29-w1-eu-hu-families — the seven eu-hu families, +25 blocks (mixer 4 +
		// electricmixer 4 + electricloom 4 + electricsifter 4 + boxinator 4 + unboxinator
		// 4 + fermenter 1), the census comment and the datagen-JVM half move together
		java.util.Collections.addAll(rBlocks, mixerBlockArray());
		java.util.Collections.addAll(rBlocks, electricMixerBlockArray());
		java.util.Collections.addAll(rBlocks, electricLoomBlockArray());
		java.util.Collections.addAll(rBlocks, electricSifterBlockArray());
		java.util.Collections.addAll(rBlocks, boxinatorBlockArray());
		java.util.Collections.addAll(rBlocks, unboxinatorBlockArray());
		java.util.Collections.addAll(rBlocks, fermenterBlockArray());
		// task p29-w2-eu-special — the eu-special families, +14 blocks (autocrafter 5 +
		// lightning 5 + laminator 4), the census comment and the datagen-JVM half move
		// together (the 119 → 133 machine-domain re-pin)
		java.util.Collections.addAll(rBlocks, autocrafterBlockArray());
		java.util.Collections.addAll(rBlocks, lightningBlockArray());
		java.util.Collections.addAll(rBlocks, laminatorBlockArray());
		// task p29-w2-exotic-energy — the six exotic-energy families, +30 blocks (Polarizer/
		// MagneticSeparator 5 each over MU + LaserEngraver/LaserWelder 5 each over LU +
		// Freezer/CryoMixer 5 each over CU), the census walk order mirrors the section
		// order. (Review-seat rebase fix: the polarizer addAll rode COMMENTED in the
		// af52df9a original — a splice artifact that silently dropped the 5 polarizer
		// blocks from the paint walk while the 149 census pins and this javadoc still
		// count them; uncommented to restore the branch's own declared census.)
		java.util.Collections.addAll(rBlocks, polarizerBlockArray());
		java.util.Collections.addAll(rBlocks, magneticSeparatorBlockArray());
		java.util.Collections.addAll(rBlocks, laserEngraverBlockArray());
		java.util.Collections.addAll(rBlocks, laserWelderBlockArray());
		java.util.Collections.addAll(rBlocks, freezerBlockArray());
		java.util.Collections.addAll(rBlocks, cryoMixerBlockArray());
		// task p29-w2-eu-core-5tier — the five eu-core families, +25 blocks (each the first
		// FIVE-tier ladder: electrolyzer 5 + injector 5 + printer 5 + scannervisuals 5 +
		// slicer 5), the census comment and the datagen-JVM half move together (119 → 133
		// → 163 → 188)
		java.util.Collections.addAll(rBlocks, electrolyzerBlockArray());
		java.util.Collections.addAll(rBlocks, injectorBlockArray());
		java.util.Collections.addAll(rBlocks, printerBlockArray());
		java.util.Collections.addAll(rBlocks, scannerVisualsBlockArray());
		java.util.Collections.addAll(rBlocks, slicerBlockArray());
		// task p29-w2-hu-tu-piggyback — the seven hu-tu families, +16 blocks (steamcracker
		// 4 + catalyticcracker 4 + coagulator 1 + generifier 1 + bath 1 + autoclave 1 +
		// loom 4), the census comment and the datagen-JVM half move together
		java.util.Collections.addAll(rBlocks, steamcrackerBlockArray());
		java.util.Collections.addAll(rBlocks, catalyticcrackerBlockArray());
		java.util.Collections.addAll(rBlocks, coagulatorBlockArray());
		java.util.Collections.addAll(rBlocks, generifierBlockArray());
		java.util.Collections.addAll(rBlocks, bathBlockArray());
		java.util.Collections.addAll(rBlocks, autoclaveBlockArray());
		java.util.Collections.addAll(rBlocks, loomBlockArray());
		// task p29-w3-heat-smelter — the Smelter 4-ladder + the Melter single, +5 blocks,
		// the census comment and the datagen-JVM half move together
		java.util.Collections.addAll(rBlocks, smelterBlockArray());
		java.util.Collections.addAll(rBlocks, melterBlockArray());
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

	// ---------------------------------------------------------------------------
	// the P29 W1 process families (task p29-w1-kinetic-process-ladder) — six Kinetic_T
	// 4-ladders over the CONSUMED card-A recipe maps, all MultiTileEntityBasicMachine
	// rows with MT.DATA.Kinetic_T[1..4] (the KINETIC_T_LADDER word set Bronze/Steel/
	// Titanium/Tungstensteel), NBT_INPUT 32/128/512/2048 through the TIER_INPUTS
	// conversion, hardness 7.0/6.0/9.0/12.5 (NBT_RESISTANCE == hardness), NBT_PARALLEL
	// absent unless named, cheap overclocking T (the :773 unconditional port face) and
	// menu = null on every row (the zero-new-MenuType GUI clause — use() stays inert,
	// the acceptance drives inject+check):
	//
	//   Buzzsaw        20061-20064  RU  RM.CUTTER          :1318-1321  parallel 1
	//   Squeezer       20071-20074  KU  RM.SQUEEZER        :1324-1327  PARALLEL_4_32 + duration T
	//   Centrifuge     20081-20084  RU  RM.CENTRIFUGE      :1330-1333  CENTRIFUGE_PARALLEL {1,2,4,8} + duration T (the NON-standard ladder)
	//   Sluice         20291-20294  RU  RM.SLUICE          :1464-1467  parallel 1 — the plain BasicMachine (NO world interaction /
	//                                                                    flowing-water dependency: the 流水 lives in the RECIPE domain,
	//                                                                    RM.SLUICE MIN 2 = every row brings its own fluid input)
	//   SandingMachine 20511-20514  RU  RM.SHARPENING      :1589-1592  parallel 1
	//   PressureWasher 20551-20554  RU  RM.PRESSURE_WASHER :1615-1618  parallel 1 (water consumption = the fluid-in
	//                                                                    mandatory leg; the NBT_TEXTURE "debarker" stays verbatim)
	//
	// ONE family BET per family over the four tier blocks — the DRYER_ROWS MachineRow
	// carrier shape, the shared {@link #kineticMachine} factory body (the row drives the
	// recipe map, energy type, parallel and masks; only the BlockEntityType argument
	// differs). The connectivity masks are the upstream rows verbatim, the row bytes
	// carrying the post-read values (the :137/:138/:143/:144/:151 reads OR SBIT_A onto
	// every keyed mask; unkeyed masks ride the field default 127 / SIDE_UNDEFINED -1).
	// The tier rides the displayKey unit word + the gt6.row.mat Kinetic material word
	// (the upstream name column "<Family> ("+aMat.getLocal()+")" per row).
	//
	// KJS face of this card (the wave-plan declaration): REGISTRATION face only — six
	// families, twenty-four MachineRow rows — plus the datapack face (the six recipe-map
	// smoke rows under data/gt6/recipe_maps/ through the GT6RecipeMapJsonLoader seam).
	// The recipe maps themselves are the card-A constants consumed verbatim (except the
	// SLUICE tail-append documented on the GT6RecipeMaps.SLUICE field); no KubeJS surface.
	// ---------------------------------------------------------------------------

	/** The Buzzsaw family unit word (the W1 trio :101-104 one-slot key form; the upstream name column "Buzzsaw ("+aMat.getLocal()+")", :1318-1321). */
	public static final String MACHINE_BUZZSAW_UNIT_KEY = "gt6.row.machine.buzzsaw";

	/** The Squeezer family unit word (the :1324-1327 name column). */
	public static final String MACHINE_SQUEEZER_UNIT_KEY = "gt6.row.machine.squeezer";

	/** The Centrifuge family unit word (the :1330-1333 name column). */
	public static final String MACHINE_CENTRIFUGE_UNIT_KEY = "gt6.row.machine.centrifuge";

	/** The Sluice family unit word (the :1464-1467 name column). */
	public static final String MACHINE_SLUICE_UNIT_KEY = "gt6.row.machine.sluice";

	/** The Sanding Machine family unit word (the :1589-1592 name column — the two-word form verbatim). */
	public static final String MACHINE_SANDING_UNIT_KEY = "gt6.row.machine.sanding_machine";

	/** The Pressure Washer family unit word (the :1615-1618 name column). */
	public static final String MACHINE_PRESSURE_WASHER_UNIT_KEY = "gt6.row.machine.pressure_washer";

	/** The four Buzzsaw rows, upstream line order :1318-1321 (T1-T4, the Kinetic_T ladder; the recipe-head column T1 toolHeadBuzzSaw(Steel) / T2-4 CobaltBrass stays the unported crafting-table domain). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> BUZZSAW_ROWS = java.util.List.of(
			buzzsaw("buzzsaw"   , "bronze"       , "Bronze"       , 20061,  7.0F, 0),
			buzzsaw("buzzsaw_t2", "steel"        , "Steel"        , 20062,  6.0F, 1),
			buzzsaw("buzzsaw_t3", "titanium"     , "Titanium"     , 20063,  9.0F, 2),
			buzzsaw("buzzsaw_t4", "tungstensteel", "Tungstensteel", 20064, 12.5F, 3));

	/** The four Squeezer rows, upstream line order :1324-1327 (T1-T4; the PARALLEL_4_32 + duration-T columns). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SQUEEZER_ROWS = java.util.List.of(
			squeezer("squeezer"   , "bronze"       , "Bronze"       , 20071,  7.0F, 0, PARALLEL_4_32[0]),
			squeezer("squeezer_t2", "steel"        , "Steel"        , 20072,  6.0F, 1, PARALLEL_4_32[1]),
			squeezer("squeezer_t3", "titanium"     , "Titanium"     , 20073,  9.0F, 2, PARALLEL_4_32[2]),
			squeezer("squeezer_t4", "tungstensteel", "Tungstensteel", 20074, 12.5F, 3, PARALLEL_4_32[3]));

	/** The four Centrifuge rows, upstream line order :1330-1333 (T1-T4; the NON-standard CENTRIFUGE_PARALLEL {1,2,4,8} + duration-T columns). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CENTRIFUGE_ROWS = java.util.List.of(
			centrifuge("centrifuge"   , "bronze"       , "Bronze"       , 20081,  7.0F, 0, CENTRIFUGE_PARALLEL[0]),
			centrifuge("centrifuge_t2", "steel"        , "Steel"        , 20082,  6.0F, 1, CENTRIFUGE_PARALLEL[1]),
			centrifuge("centrifuge_t3", "titanium"     , "Titanium"     , 20083,  9.0F, 2, CENTRIFUGE_PARALLEL[2]),
			centrifuge("centrifuge_t4", "tungstensteel", "Tungstensteel", 20084, 12.5F, 3, CENTRIFUGE_PARALLEL[3]));

	/** The four Sluice rows, upstream line order :1464-1467 (T1-T4; the plain-BasicMachine family — no world interaction, the fluid leg is the recipe's). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SLUICE_ROWS = java.util.List.of(
			sluice("sluice"   , "bronze"       , "Bronze"       , 20291,  7.0F, 0),
			sluice("sluice_t2", "steel"        , "Steel"        , 20292,  6.0F, 1),
			sluice("sluice_t3", "titanium"     , "Titanium"     , 20293,  9.0F, 2),
			sluice("sluice_t4", "tungstensteel", "Tungstensteel", 20294, 12.5F, 3));

	/** The four Sanding Machine rows, upstream line order :1589-1592 (T1-T4; no parallel key → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SANDING_ROWS = java.util.List.of(
			sanding("sanding_machine"   , "bronze"       , "Bronze"       , 20511,  7.0F, 0),
			sanding("sanding_machine_t2", "steel"        , "Steel"        , 20512,  6.0F, 1),
			sanding("sanding_machine_t3", "titanium"     , "Titanium"     , 20513,  9.0F, 2),
			sanding("sanding_machine_t4", "tungstensteel", "Tungstensteel", 20514, 12.5F, 3));

	/** The four Pressure Washer rows, upstream line order :1615-1618 (T1-T4; the NBT_TEXTURE "debarker" verbatim — the art-token fidelity over the registry path). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> PRESSURE_WASHER_ROWS = java.util.List.of(
			pressurewasher("pressure_washer"   , "bronze"       , "Bronze"       , 20551,  7.0F, 0),
			pressurewasher("pressure_washer_t2", "steel"        , "Steel"        , 20552,  6.0F, 1),
			pressurewasher("pressure_washer_t3", "titanium"     , "Titanium"     , 20553,  9.0F, 2),
			pressurewasher("pressure_washer_t4", "tungstensteel", "Tungstensteel", 20554, 12.5F, 3));

	/**
	 * One Buzzsaw row factory — the differing columns (path/name/id/material/hardness/
	 * tier) plus the family constants: RM.CUTTER through the supplier, RU, the "buzzsaw"
	 * texture, the :1318 masks (item left-in/right-out, tank IN over top|bottom with the
	 * SIDE_BOTTOM auto face, NO tank-out key → the 127 field default, energy back) and
	 * parallel 1 / parallelDuration F (no NBT_PARALLEL keys). The crafting-head column
	 * (T1 OP.toolHeadBuzzSaw.dat(ANY.Steel) vs T2-4 CobaltBrass, the :1318-1321 recipe
	 * tails) is the unported crafting-table domain — data-only, the row carries no column
	 * for it (the GTMachinesMaterialRowTest census form).
	 */
	private static GTBasicMachineBlock.MachineRow buzzsaw(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_BUZZSAW_UNIT_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.CUTTER, TD.Energy.RU, "buzzsaw",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B, the :151 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_D, the :143 read ORs SBIT_A — the coolant leg*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L, the :137 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R, the :138 read*/,
				(byte)0 /*NBT_TANK_SIDE_AUTO_IN SIDE_BOTTOM*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType (the card GUI clause)*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate on the legacy rows (p28-c-ulv-machine-ladder)*/, false);
	}

	/** One Squeezer row factory — the buzzsaw shape over the :1324 masks (tank OUT bottom, NO tank-in key, energy top) and RM.Squeezer/KU + the PARALLEL_4_32 duration-T ladder. */
	private static GTBasicMachineBlock.MachineRow squeezer(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_SQUEEZER_UNIT_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.SQUEEZER, TD.Energy.KU, "squeezer",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U, the :151 read*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_D, the :144 read — the juice leg*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*no NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/** One Centrifuge row factory — the :1330 masks (item top-in/right-out, fluid top-in/left-out, energy bottom) and RM.Centrifuge/RU + the NON-standard CENTRIFUGE_PARALLEL duration-T ladder. */
	private static GTBasicMachineBlock.MachineRow centrifuge(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier, int aParallel) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_CENTRIFUGE_UNIT_KEY, aMetaId, aHardness, aTier, aParallel, true,
				() -> GT6RecipeMaps.CENTRIFUGE, TD.Energy.RU, "centrifuge",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)2 /*NBT_TANK_SIDE_AUTO_OUT SIDE_LEFT*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/** One Sluice row factory — the :1464 masks (item+tank in left|top, item out right|bottom auto RIGHT, tank out right|bottom auto BOTTOM, energy back) and RM.Sluice/RU, parallel 1. */
	private static GTBasicMachineBlock.MachineRow sluice(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_SLUICE_UNIT_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.SLUICE, TD.Energy.RU, "sluice",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/** One Sanding Machine row factory — the wiremill shape over the :1589 masks (left-in/right-out, energy TOP, no tank keys at all) and RM.Sharpening/RU, parallel 1. */
	private static GTBasicMachineBlock.MachineRow sanding(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_SANDING_UNIT_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.SHARPENING, TD.Energy.RU, "sander",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U — the energy face rides the TOP*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/** One Pressure Washer row factory — the buzzsaw shape over the :1615 masks (tank IN top|bottom auto TOP, no tank-out key, energy back) and RM.PressureWasher/RU, parallel 1, the "debarker" texture verbatim. */
	private static GTBasicMachineBlock.MachineRow pressurewasher(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_PRESSURE_WASHER_UNIT_KEY, aMetaId, aHardness, aTier, 1, false,
				() -> GT6RecipeMaps.PRESSURE_WASHER, TD.Energy.RU, "debarker",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_D — the water-consumption leg*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key — the map is IN 1 / OUT 0 fluids*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/** The registered Buzzsaw blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> BUZZSAW_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Buzzsaw items, same keys as {@link #BUZZSAW_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> BUZZSAW_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Squeezer blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> SQUEEZER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Squeezer items, same keys as {@link #SQUEEZER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> SQUEEZER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Centrifuge blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> CENTRIFUGE_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Centrifuge items, same keys as {@link #CENTRIFUGE_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> CENTRIFUGE_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Sluice blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> SLUICE_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Sluice items, same keys as {@link #SLUICE_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> SLUICE_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Sanding Machine blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> SANDING_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Sanding Machine items, same keys as {@link #SANDING_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> SANDING_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Pressure Washer blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> PRESSURE_WASHER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Pressure Washer items, same keys as {@link #PRESSURE_WASHER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> PRESSURE_WASHER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : BUZZSAW_ROWS) {
			BUZZSAW_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.BUZZSAW_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			BUZZSAW_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.BUZZSAW_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : SQUEEZER_ROWS) {
			SQUEEZER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SQUEEZER_BE.get(), tRow)));
			SQUEEZER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SQUEEZER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : CENTRIFUGE_ROWS) {
			CENTRIFUGE_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.CENTRIFUGE_BE.get(), tRow)));
			CENTRIFUGE_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.CENTRIFUGE_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : SLUICE_ROWS) {
			SLUICE_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SLUICE_BE.get(), tRow)));
			SLUICE_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SLUICE_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : SANDING_ROWS) {
			SANDING_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SANDING_BE.get(), tRow)));
			SANDING_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SANDING_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : PRESSURE_WASHER_ROWS) {
			PRESSURE_WASHER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.PRESSURE_WASHER_BE.get(), tRow)));
			PRESSURE_WASHER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.PRESSURE_WASHER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Buzzsaw block list in registration order (the loot/datagen walkers). */
	public static Block[] buzzsawBlockArray() {
		Block[] rBlocks = new Block[BUZZSAW_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : BUZZSAW_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Squeezer block list in registration order. */
	public static Block[] squeezerBlockArray() {
		Block[] rBlocks = new Block[SQUEEZER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SQUEEZER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Centrifuge block list in registration order. */
	public static Block[] centrifugeBlockArray() {
		Block[] rBlocks = new Block[CENTRIFUGE_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : CENTRIFUGE_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Sluice block list in registration order. */
	public static Block[] sluiceBlockArray() {
		Block[] rBlocks = new Block[SLUICE_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SLUICE_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Sanding Machine block list in registration order. */
	public static Block[] sandingBlockArray() {
		Block[] rBlocks = new Block[SANDING_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SANDING_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Pressure Washer block list in registration order. */
	public static Block[] pressurewasherBlockArray() {
		Block[] rBlocks = new Block[PRESSURE_WASHER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : PRESSURE_WASHER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/**
	 * The ONE Buzzsaw family BET: the Dryer shape verbatim over the shared
	 * {@link #kineticMachine} factory (the row carries every column the family needs; no
	 * extra registration columns).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> BUZZSAW_BE =
			BLOCK_ENTITY_TYPES.register("buzzsaw", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.BUZZSAW_BE.get(), aPos, aState),
					buzzsawBlockArray()).build(null));

	/** The ONE Squeezer family BET — the KU first-new-consumer family (the PARALLEL_4_32 + duration-T ladder). */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SQUEEZER_BE =
			BLOCK_ENTITY_TYPES.register("squeezer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.SQUEEZER_BE.get(), aPos, aState),
					squeezerBlockArray()).build(null));

	/** The ONE Centrifuge family BET — the CENTRIFUGE_PARALLEL {1,2,4,8} non-standard ladder's first consumer (the card-A constant lands here). */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CENTRIFUGE_BE =
			BLOCK_ENTITY_TYPES.register("centrifuge", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.CENTRIFUGE_BE.get(), aPos, aState),
					centrifugeBlockArray()).build(null));

	/** The ONE Sluice family BET — the plain-BasicMachine family over the batch-C SLUICE map tail-append. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SLUICE_BE =
			BLOCK_ENTITY_TYPES.register("sluice", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.SLUICE_BE.get(), aPos, aState),
					sluiceBlockArray()).build(null));

	/** The ONE Sanding Machine family BET — the SHARPENING map's first machine consumer. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SANDING_BE =
			BLOCK_ENTITY_TYPES.register("sanding_machine", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.SANDING_BE.get(), aPos, aState),
					sandingBlockArray()).build(null));

	/** The ONE Pressure Washer family BET — the PRESSURE_WASHER map's first machine consumer (the "debarker" art family). */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> PRESSURE_WASHER_BE =
			BLOCK_ENTITY_TYPES.register("pressure_washer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.PRESSURE_WASHER_BE.get(), aPos, aState),
					pressurewasherBlockArray()).build(null));

	// ---------------------------------------------------------------------------
	// the P29 W2 eu-special families (task p29-w2-eu-special) — EU special 2 + HU 1,
	// 14 rows: the Autocrafter ladder (Loader_MultiTileEntities.java:1497-1501, EU,
	// VN[1..5] — the FIRST 5-tier family shape of the repo together with the W2 card-②
	// eu-core families) / the Lightning Processor ladder (:1582-1586, EU, VN[1..5],
	// NBT_USE_OUTPUT_TANK T — the first USE_OUTPUT_TANK-only consumer after the Canner,
	// with NO NBT_TANK_CAPACITY column) / the Laminator ladder (:1532-1535, HU 4梯
	// Heat_T[1..4]). All three maps are the card-① DECLARED-empty W2 maps, filled here
	// with the smoke rows only (data/gt6/recipe_maps/{autocrafter,lightning,laminator}
	// .json through the GT6RecipeMapJsonLoader direct-pour seam).
	//
	// KJS face of this card: the registration rows (3 families / 14 rows) + the datapack
	// domain (the three smoke rows). NO KubeJS surface.
	//
	// Declared deviations (all three declared by the arch card / the card ① map block):
	// - AUTOCRAFTER: upstream RM.Autocrafter is a RecipeMapAutocrafting subclass whose
	//   lookup reads the vanilla crafting grid into rows at runtime — the crafting-grid
	//   auto-fill arm STAYS POOLED (the UNBOXINATOR loot-arm judged form, card ① doc);
	//   the map here is the base-RecipeMap DECLARED-empty form and the machine consumes
	//   the smoke rows only. The upstream GUI word is "Crafting" (RM.java:63) — that is
	//   the MAP's GUI path word, not the machine's name column ("Autocrafter ("+VN[tier]
	//   +")", :1497-1501).
	// - LIGHTNING: NO lightning-strike mechanism — upstream the lightning-into-network
	//   face is the LightningRod MULTIBLOCK (18104, ported, task p24-lightning-rod);
	//   the Lightning Processor is a plain EU consumer (:1582-1586 registers
	//   MultiTileEntityBasicMachineElectric with NBT_ENERGY_ACCEPTED EU). Only the
	//   NBT_USE_OUTPUT_TANK T key (the :716-732 recipe-fallback arm, the Canner
	//   factory-column form WITHOUT the capacity column) is the porting point.
	// - LAMINATOR: the upstream rows carry NBT_GUI = RES_PATH_GUI + "machines/Laminator
	//   .png" (the custom GUI path) — the port's null-menu convention defers ALL machine
	//   GUIs (the menu-less carrier), so the path is declared HERE only (fidelity
	//   record); the HU energy face is the burning-box bottom-feed form (the Dryer/
	//   Extruder/Fermenter SBIT_D shape).
	// ---------------------------------------------------------------------------

	/** The Autocrafter family display template (the voltage-word slot; upstream "Autocrafter ("+VN[tier]+")", :1497-1501). */
	public static final String AUTOCRAFTER_DISPLAY_KEY = "gt6.row.autocrafter.display";
	/** The Lightning Processor family display template (upstream "Lightning Processor ("+VN[tier]+")", :1582-1586). */
	public static final String LIGHTNING_PROCESSOR_DISPLAY_KEY = "gt6.row.lightningprocessor.display";
	/** The Laminator family display template (the Heat_T material-word slot; upstream "Laminator ("+aMat.getLocal()+")", :1532-1535). */
	public static final String LAMINATOR_DISPLAY_KEY = "gt6.row.laminator.display";

	/**
	 * The 5-tier window resolver (the 立行制 carrier, task p29-w2-energy-types-5tier ②):
	 * tier 0..3 ride the SHARED {@link #TIER_INPUTS} table, tier 4 (the T5/IV rung) rides
	 * {@link #EV_TIER_INPUTS} — the :126 conversion over NBT_INPUT 8192 = min 4096 /
	 * rec 8192 / max 16384. The two 5-tier eu-special ladders (Autocrafter :1497-1501,
	 * Lightning :1582-1586, NBT_INPUT 32/128/512/2048/8192) consume this; the
	 * {@link #machine} helper stays byte-identical on the 4-row TIER_INPUTS dispatch.
	 * Public test seam (the {@link #applyRow} form): the offline row tests pin the
	 * resolver's tier arithmetic directly.
	 */
	public static long[] euFiveTierWindow(int aTier) {
		return aTier < TIER_INPUTS.length ? TIER_INPUTS[aTier] : EV_TIER_INPUTS;
	}

	/**
	 * The 5-tier BET factory body — the {@link #machine} body with the tier window read
	 * through {@link #euFiveTierWindow} (tier 4 = the EV_TIER_INPUTS row). Card ②'s
	 * eu-core families carry their own arm of the same shape; the merge dedup point is
	 * THIS resolver (the card-② equivalent folds onto it).
	 */
	private static TileEntityBasicMachine euFiveTierMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = new TileEntityBasicMachine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.menu());
		tMachine.mEnergyTypeAccepted = tRow.energyType();
		long[] tInputs = euFiveTierWindow(tRow.tier());
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return applyRow(tMachine, tRow);
	}

	/** The five Autocrafter rows, upstream line order :1497-1501 (T1-T5, the Electric_T ladder + the T5 rung, EU, RM.AUTOCRAFTER). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> AUTOCRAFTER_ROWS = java.util.List.of(
			autocrafter("autocrafter"   , "lv", "LV", 20341, 0),
			autocrafter("autocrafter_t2", "mv", "MV", 20342, 1),
			autocrafter("autocrafter_t3", "hv", "HV", 20343, 2),
			autocrafter("autocrafter_t4", "ev", "EV", 20344, 3),
			autocrafter("autocrafter_t5", "iv", "IV", 20345, 4));

	/**
	 * One Autocrafter row factory — the differing columns plus the family constants:
	 * Electric_T material (tier 4 rides {@link #ELECTRIC_T5}), the voltage-word slot,
	 * hardness 4.0 (the :1497 column on all five rows), the "autocrafter" texture,
	 * RM.AUTOCRAFTER, EU, NO parallel/efficiency keys (the 26-arg overload), and the
	 * :1497 masks VERBATIM — the WAVE'S FIRST DUAL ENERGY FACE: NBT_ENERGY_ACCEPTED_SIDES
	 * SBIT_U|SBIT_D (item in U|L auto LEFT, out R|D auto RIGHT; NO tank keys — the map is
	 * 9/12/1 items, 0/0/0 fluids, the zero-fluid 127/-1 face).
	 */
	private static GTBasicMachineBlock.MachineRow autocrafter(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, aTier < 4 ? ELECTRIC_T_LADDER.get(aTier) : ELECTRIC_T5, AUTOCRAFTER_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1497-1501*/,
				() -> GT6RecipeMaps.AUTOCRAFTER, TD.Energy.EU, "autocrafter",
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_U|SBIT_D — the dual-face energy, :1497 verbatim + the :151 OR*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/** The five Lightning Processor rows, upstream line order :1582-1586 (T1-T5, EU, RM.LIGHTNING, NBT_USE_OUTPUT_TANK T). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> LIGHTNING_ROWS = java.util.List.of(
			lightning("lightning"   , "lv", "LV", 20501, 0),
			lightning("lightning_t2", "mv", "MV", 20502, 1),
			lightning("lightning_t3", "hv", "HV", 20503, 2),
			lightning("lightning_t4", "ev", "EV", 20504, 3),
			lightning("lightning_t5", "iv", "IV", 20505, 4));

	/**
	 * One Lightning Processor row factory — the autocrafter shape over the :1582 masks:
	 * the "lightning" texture (the upstream NBT_TEXTURE token verbatim — the family path
	 * keeps the two-word display name), tank in U|L auto TOP / out R|D auto BOTTOM (the
	 * map is 6/6/0 items + 6/6/0 fluids), energy SBIT_B (bottom, the single face), the
	 * 26-arg overload. The NBT_USE_OUTPUT_TANK T column rides the {@link #lightningMachine}
	 * factory arm (the Canner two-extra-columns form, capacity-free).
	 */
	private static GTBasicMachineBlock.MachineRow lightning(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, aTier < 4 ? ELECTRIC_T_LADDER.get(aTier) : ELECTRIC_T5, LIGHTNING_PROCESSOR_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1582-1586*/,
				() -> GT6RecipeMaps.LIGHTNING, TD.Energy.EU, "lightning",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/** The four Laminator rows, upstream line order :1532-1535 (T1-T4, the Heat_T ladder, HU, RM.LAMINATOR). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> LAMINATOR_ROWS = java.util.List.of(
			laminator("laminator"   , "steel"           , "Steel"           , 20391,  6.0F, 0),
			laminator("laminator_t2", "invar"           , "Invar"           , 20392,  4.0F, 1),
			laminator("laminator_t3", "titanium"        , "Titanium"        , 20393,  9.0F, 2),
			laminator("laminator_t4", "tungsten_carbide", "Tungsten Carbide", 20394, 12.5F, 3));

	/**
	 * One Laminator row factory — the :1532 masks: item in L|U auto LEFT, out R auto
	 * RIGHT, energy SBIT_D (the burning-box bottom-feed form), zero tank keys (the map is
	 * 2/1/2 items, 0/0/0 fluids), the Heat_T material-word slot, hardness 6.0/4.0/9.0/12.5
	 * (NBT_RESISTANCE == hardness), no parallel/efficiency keys, the 26-arg overload. The
	 * upstream NBT_GUI "machines/Laminator.png" path is DECLARED on the section doc (the
	 * null-menu convention defers the GUI, zero new MenuType).
	 */
	private static GTBasicMachineBlock.MachineRow laminator(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, HEAT_T_LADDER.get(aTier), LAMINATOR_DISPLAY_KEY, aMetaId, aHardness, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1532-1535*/,
				() -> GT6RecipeMaps.LAMINATOR, TD.Energy.HU, "laminator",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D — the p13 bottom-feed form*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — the NBT_GUI path stays declared on the section doc*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/** The registered Autocrafter blocks by path (the BET/datagen/loot walkers). */
	public static final java.util.Map<String, RegistryObject<Block>> AUTOCRAFTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Autocrafter items, same keys as {@link #AUTOCRAFTER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> AUTOCRAFTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : AUTOCRAFTER_ROWS) {
			AUTOCRAFTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.AUTOCRAFTER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			AUTOCRAFTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.AUTOCRAFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Autocrafter block list in registration order (the loot/datagen walkers). */
	public static Block[] autocrafterBlockArray() {
		Block[] rBlocks = new Block[AUTOCRAFTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : AUTOCRAFTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for the placement/loot walkers — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block autocrafterBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = AUTOCRAFTER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The registered Lightning Processor blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> LIGHTNING_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Lightning Processor items, same keys as {@link #LIGHTNING_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> LIGHTNING_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : LIGHTNING_ROWS) {
			LIGHTNING_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.LIGHTNING_BE.get(), tRow)));
			LIGHTNING_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.LIGHTNING_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Lightning Processor block list in registration order. */
	public static Block[] lightningBlockArray() {
		Block[] rBlocks = new Block[LIGHTNING_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : LIGHTNING_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for the placement/loot walkers — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block lightningBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = LIGHTNING_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The registered Laminator blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> LAMINATOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Laminator items, same keys as {@link #LAMINATOR_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> LAMINATOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : LAMINATOR_ROWS) {
			LAMINATOR_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.LAMINATOR_BE.get(), tRow)));
			LAMINATOR_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.LAMINATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Laminator block list in registration order. */
	public static Block[] laminatorBlockArray() {
		Block[] rBlocks = new Block[LAMINATOR_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : LAMINATOR_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for the placement/loot walkers — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block laminatorBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = LAMINATOR_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The ONE Autocrafter family BET: the 5 validBlocks multi-attach (the first 5-block
	 * family shape) over the {@link #euFiveTierMachine} factory (tier 4 = EV_TIER_INPUTS).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> AUTOCRAFTER_BE =
			BLOCK_ENTITY_TYPES.register("autocrafter", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.AUTOCRAFTER_BE.get(), aPos, aState),
					autocrafterBlockArray()).build(null));

	/**
	 * The ONE Lightning Processor family BET — the euFiveTierMachine body plus the
	 * NBT_USE_OUTPUT_TANK T column (the {@code cannerMachine} two-extra-columns form,
	 * capacity-free: the :1582-1586 rows carry NO NBT_TANK_CAPACITY key).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LIGHTNING_BE =
			BLOCK_ENTITY_TYPES.register("lightning", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> lightningMachine(GTMachines.LIGHTNING_BE.get(), aPos, aState),
					lightningBlockArray()).build(null));

	/** The Lightning BET factory body — the euFiveTierMachine body plus the output-tank flag. */
	private static TileEntityBasicMachine lightningMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		TileEntityBasicMachine tMachine = euFiveTierMachine(aType, aPos, aState);
		tMachine.mCanUseOutputTanks = true; // NBT_USE_OUTPUT_TANK T (:1582-1586, the :716-732 fallback arm)
		return tMachine;
	}

	/** The ONE Laminator family BET — the HU family over the shared 4-row TIER_INPUTS dispatch. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LAMINATOR_BE =
			BLOCK_ENTITY_TYPES.register("laminator", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.LAMINATOR_BE.get(), aPos, aState),
					laminatorBlockArray()).build(null));

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

	// ---------------------------------------------------------------------------
	// the P29 W2 EU-core families (task p29-w2-eu-core-5tier) — the FIRST 5-TIER ladders
	// of the port (the 立行制 first consumer set, five MultiTileEntityBasicMachineElectric
	// families over MT.DATA.Electric_T[1..5] = SteelGalvanized/Al/StainlessSteel/Cr/Ti,
	// MT.java:3691 — tiers 0-3 ride {@link #ELECTRIC_T_LADDER}, the T5 rung
	// {@link #ELECTRIC_T_LADDER}'s {@link #ELECTRIC_T5} supplier), all TD.Energy.EU
	// (MultiTileEntityBasicMachineElectric), all NBT_INPUT 32/128/512/2048/8192 through
	// the :126 conversion ({@link #TIER_INPUTS}[0..3] + the tier-4 arm {@link
	// #EV_TIER_INPUTS} = {4096, 8192, 16384}), hardness 4.0F on EVERY row (NBT_RESISTANCE
	// == hardness), cheap overclocking T (the :773 unconditional port face), NO
	// NBT_EFFICIENCY key (the 26-arg overload — the :96 10000 identity) and menu = null on
	// every row (the zero-new-MenuType GUI clause):
	//
	//   Electrolyzer     20091-20095  EU  RM.ELECTROLYZER     :1336-1340  ELECTROLYZER_PARALLEL {1,2,4,8,16} + duration T
	//   Injector         20261-20265  EU  RM.INJECTOR         :1443-1447  parallel 1
	//   Printer          20271-20275  EU  RM.PRINTER          :1450-1454  parallel 1 (the base-form map — the NBT
	//                                                                     blueprint-copy face stays pooled, the card-①
	//                                                                     RecipeMapPrinter deviation)
	//   ScannerVisuals   20281-20285  EU  RM.SCANNER_VISUALS  :1457-1461  parallel 1 (same base-form pool on the
	//                                                                     RecipeMapScannerVisuals NBT scan face)
	//   Slicer           20381-20385  EU  RM.SLICER           :1525-1529  parallel 1
	//
	// ONE family BET per family over the FIVE tier blocks — the DRYER_ROWS MachineRow
	// carrier shape, the shared {@link #euFiveTierMachine} factory body (the card-③ landed resolver — the W2 single window source, the merge dedup point) (the row drives the
	// recipe map, energy type, parallel and masks; only the BlockEntityType argument
	// differs). The connectivity masks are the upstream rows verbatim, the row bytes
	// carrying the post-read values (the :137/:138/:143/:144/:151 reads OR SBIT_A onto
	// every keyed mask; unkeyed masks ride the field default 127 / SIDE_UNDEFINED -1).
	// The display words ride the VN VOLTAGE ladder per row (the upstream name column
	// "Electrolyzer ("+VN[tier]+")" form, CS.java:154) — LV/MV/HV/EV for tiers 1-4 and
	// the FIFTH word VN[5] = "IV" for the T5 rung (the S9 ruling: NOT "EV" — ev is
	// already T4's word since p24, the CS.java:154 array and the GTMachines.EV_TIER_INPUTS
	// doc both carry the erratum; the "iv" mat unit landed with card ①).
	//
	// KJS face of this card (the wave-plan declaration): REGISTRATION face only — five
	// families, twenty-five MachineRow rows over the 5-tier array TIER_INPUTS[0..3] +
	// EV_TIER_INPUTS (the constant name kept as the consumers' API anchor, its real
	// meaning VN[5] = IV) — plus the datapack face (the five recipe-map smoke rows under
	// data/gt6/recipe_maps/ through the GT6RecipeMapJsonLoader seam). NO KubeJS surface.
	// ---------------------------------------------------------------------------

	/** The Electrolyzer family display template (the voltage-word slot; upstream "Electrolyzer ("+VN[tier]+")", :1336-1340). */
	public static final String ELECTROLYZER_DISPLAY_KEY = "gt6.row.electrolyzer.display";
	/** The Injector family display template (upstream "Injector ("+VN[tier]+")", :1443-1447). */
	public static final String INJECTOR_DISPLAY_KEY = "gt6.row.injector.display";
	/** The Printer family display template (upstream "Printer ("+VN[tier]+")", :1450-1454). */
	public static final String PRINTER_DISPLAY_KEY = "gt6.row.printer.display";
	/** The Scanner (Visuals) family display template (the upstream name column "Scanner (Visuals, "+VN[tier]+")", :1457-1461 — the comma form verbatim). */
	public static final String SCANNER_VISUALS_DISPLAY_KEY = "gt6.row.scannervisuals.display";
	/** The Slicer family display template (upstream "Slicer ("+VN[tier]+")", :1525-1529). */
	public static final String SLICER_DISPLAY_KEY = "gt6.row.slicer.display";

	/** The voltage-word columns of the 5-tier rows — slugs for the gt6.row.mat units, displays for the census forms (VN[1..5] = LV/MV/HV/EV/IV, CS.java:154). */
	public static final String[][] VOLTAGE_WORDS = {{"lv", "LV"}, {"mv", "MV"}, {"hv", "HV"}, {"ev", "EV"}, {"iv", "IV"}};

	/** The housing material of one eu-core row — Electric_T[1..4] through the ladder, the T5 rung through {@link #ELECTRIC_T5} (MT.java:3691). */
	private static java.util.function.Supplier<OreDictMaterial> electricTier(int aTier) {
		return aTier < ELECTRIC_T_LADDER.size() ? ELECTRIC_T_LADDER.get(aTier) : ELECTRIC_T5;
	}

	/** The five Electrolyzer rows, upstream line order :1336-1340 (T1-T5, the VN ladder; the ELECTROLYZER_PARALLEL + duration-T columns). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ELECTROLYZER_ROWS = java.util.List.of(
			electrolyzer("electrolyzer"   , 20091, 0),
			electrolyzer("electrolyzer_t2", 20092, 1),
			electrolyzer("electrolyzer_t3", 20093, 2),
			electrolyzer("electrolyzer_t4", 20094, 3),
			electrolyzer("electrolyzer_t5", 20095, 4));

	/** The five Injector rows, upstream line order :1443-1447 (T1-T5; no NBT_PARALLEL → 1). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> INJECTOR_ROWS = java.util.List.of(
			injector("injector"   , 20261, 0),
			injector("injector_t2", 20262, 1),
			injector("injector_t3", 20263, 2),
			injector("injector_t4", 20264, 3),
			injector("injector_t5", 20265, 4));

	/** The five Printer rows, upstream line order :1450-1454 (T1-T5; the base-form PRINTER map — the NBT blueprint face pooled). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> PRINTER_ROWS = java.util.List.of(
			printer("printer"   , 20271, 0),
			printer("printer_t2", 20272, 1),
			printer("printer_t3", 20273, 2),
			printer("printer_t4", 20274, 3),
			printer("printer_t5", 20275, 4));

	/** The five Scanner (Visuals) rows, upstream line order :1457-1461 (T1-T5; the base-form SCANNER_VISUALS map — the NBT scan face pooled). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SCANNER_VISUALS_ROWS = java.util.List.of(
			scannerVisuals("scannervisuals"   , 20281, 0),
			scannerVisuals("scannervisuals_t2", 20282, 1),
			scannerVisuals("scannervisuals_t3", 20283, 2),
			scannerVisuals("scannervisuals_t4", 20284, 3),
			scannerVisuals("scannervisuals_t5", 20285, 4));

	/** The five Slicer rows, upstream line order :1525-1529 (T1-T5; no tank keys — the zero-fluid face, data-only). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SLICER_ROWS = java.util.List.of(
			slicer("slicer"   , 20381, 0),
			slicer("slicer_t2", 20382, 1),
			slicer("slicer_t3", 20383, 2),
			slicer("slicer_t4", 20384, 3),
			slicer("slicer_t5", 20385, 4));

	/**
	 * One Electrolyzer row factory — the differing columns (path/id/tier) plus the family
	 * constants: RM.ELECTROLYZER through the supplier, EU, the "electrolyzer" texture, the
	 * :1336 masks (item+tank in U|F|B with the SIDE_TOP auto face, out R|L with the item
	 * auto face SIDE_RIGHT and the tank auto face SIDE_LEFT — the tank-out auto face is
	 * the ONE divergence from the Canner's SIDE_BOTTOM), energy back-bottom SBIT_D, the
	 * ELECTROLYZER_PARALLEL {1,2,4,8,16} duration-T ladder, hardness 4.0 and the null
	 * menu supplier.
	 */
	private static GTBasicMachineBlock.MachineRow electrolyzer(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, VOLTAGE_WORDS[aTier][0], VOLTAGE_WORDS[aTier][1], electricTier(aTier), ELECTROLYZER_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				ELECTROLYZER_PARALLEL[aTier], true /*NBT_PARALLEL_DURATION T :1336*/,
				() -> GT6RecipeMaps.ELECTROLYZER, TD.Energy.EU, "electrolyzer",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D, the :151 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_F | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_F|SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_F | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_F|SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_L*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)2 /*NBT_TANK_SIDE_AUTO_OUT SIDE_LEFT*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/**
	 * One Injector row factory — the :1443 masks (item+tank in U|L auto LEFT, out R|D with
	 * the item auto face SIDE_RIGHT and the tank auto face SIDE_BOTTOM, energy back) and
	 * RM.Injector/EU + parallel 1 / duration F (no NBT_PARALLEL keys).
	 */
	private static GTBasicMachineBlock.MachineRow injector(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, VOLTAGE_WORDS[aTier][0], VOLTAGE_WORDS[aTier][1], electricTier(aTier), INJECTOR_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1443-1447*/,
				() -> GT6RecipeMaps.INJECTOR, TD.Energy.EU, "injector",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/**
	 * One Printer row factory — the :1450 masks (item in U|L auto LEFT, out R|D auto
	 * RIGHT, the tank IN over the same U|L with the SIDE_TOP auto face, NO tank-out key →
	 * the 127/-1 field defaults, energy back) and RM.Printer/EU + parallel 1 (the
	 * RecipeMapPrinter NBT blueprint face stays pooled, the card-① base-form deviation).
	 */
	private static GTBasicMachineBlock.MachineRow printer(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, VOLTAGE_WORDS[aTier][0], VOLTAGE_WORDS[aTier][1], electricTier(aTier), PRINTER_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL :1450-1454*/,
				() -> GT6RecipeMaps.PRINTER, TD.Energy.EU, "printer",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/**
	 * One Scanner (Visuals) row factory — the Printer shape minus every tank key (the
	 * :1457 rows carry NO NBT_TANK keys → the 127/-1 no-tank face, the ElectricLoom
	 * zero-fluid form) and RM.ScannerVisuals/EU + parallel 1.
	 */
	private static GTBasicMachineBlock.MachineRow scannerVisuals(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, VOLTAGE_WORDS[aTier][0], VOLTAGE_WORDS[aTier][1], electricTier(aTier), SCANNER_VISUALS_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL :1457-1461*/,
				() -> GT6RecipeMaps.SCANNER_VISUALS, TD.Energy.EU, "scannervisuals",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)-1 /*no NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/**
	 * One Slicer row factory — the :1525 masks (item in L|U auto LEFT, out R|D auto
	 * RIGHT, NO tank keys — the zero-fluid face, data-only, energy back) and
	 * RM.Slicer/EU + parallel 1.
	 */
	private static GTBasicMachineBlock.MachineRow slicer(String aPath, int aMetaId, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, VOLTAGE_WORDS[aTier][0], VOLTAGE_WORDS[aTier][1], electricTier(aTier), SLICER_DISPLAY_KEY, aMetaId, 4.0F, aTier,
				1, false /*no NBT_PARALLEL :1525-1529*/,
				() -> GT6RecipeMaps.SLICER, TD.Energy.EU, "slicer",
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_B*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_L|SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_D*/,
				(byte)-1 /*no NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true, null, false);
	}

	/** The registered eu-core blocks by path (the BET/datagen/loot walkers). */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTROLYZER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Block>> INJECTOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Block>> PRINTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Block>> SCANNER_VISUALS_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Block>> SLICER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered eu-core items, same keys as the block maps. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTROLYZER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Item>> INJECTOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Item>> PRINTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Item>> SCANNER_VISUALS_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Item>> SLICER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : ELECTROLYZER_ROWS) {
			ELECTROLYZER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ELECTROLYZER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			ELECTROLYZER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.ELECTROLYZER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : INJECTOR_ROWS) {
			INJECTOR_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.INJECTOR_BE.get(), tRow)));
			INJECTOR_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.INJECTOR_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : PRINTER_ROWS) {
			PRINTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.PRINTER_BE.get(), tRow)));
			PRINTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.PRINTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : SCANNER_VISUALS_ROWS) {
			SCANNER_VISUALS_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SCANNER_VISUALS_BE.get(), tRow)));
			SCANNER_VISUALS_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SCANNER_VISUALS_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : SLICER_ROWS) {
			SLICER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SLICER_BE.get(), tRow)));
			SLICER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SLICER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Electrolyzer block list in registration order (the loot/datagen walkers). */
	public static Block[] electrolyzerBlockArray() {
		Block[] rBlocks = new Block[ELECTROLYZER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : ELECTROLYZER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Injector block list in registration order. */
	public static Block[] injectorBlockArray() {
		Block[] rBlocks = new Block[INJECTOR_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : INJECTOR_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Printer block list in registration order. */
	public static Block[] printerBlockArray() {
		Block[] rBlocks = new Block[PRINTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : PRINTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Scanner (Visuals) block list in registration order. */
	public static Block[] scannerVisualsBlockArray() {
		Block[] rBlocks = new Block[SCANNER_VISUALS_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SCANNER_VISUALS_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Slicer block list in registration order. */
	public static Block[] slicerBlockArray() {
		Block[] rBlocks = new Block[SLICER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SLICER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	// the five eu-core family BETs: the eu-hu shape verbatim — ONE family BET, the FIVE
	// tier blocks multi-attached, the row read off the placed block by the SHARED
	// euFiveTierMachine factory body (the card-③ landed 5-tier resolver, the merge dedup point).

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ELECTROLYZER_BE =
			BLOCK_ENTITY_TYPES.register("electrolyzer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.ELECTROLYZER_BE.get(), aPos, aState),
					electrolyzerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> INJECTOR_BE =
			BLOCK_ENTITY_TYPES.register("injector", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.INJECTOR_BE.get(), aPos, aState),
					injectorBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> PRINTER_BE =
			BLOCK_ENTITY_TYPES.register("printer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.PRINTER_BE.get(), aPos, aState),
					printerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SCANNER_VISUALS_BE =
			BLOCK_ENTITY_TYPES.register("scannervisuals", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.SCANNER_VISUALS_BE.get(), aPos, aState),
					scannerVisualsBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SLICER_BE =
			BLOCK_ENTITY_TYPES.register("slicer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> euFiveTierMachine(GTMachines.SLICER_BE.get(), aPos, aState),
					slicerBlockArray()).build(null));


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
							// task p29-w1-kinetic-roll-ladder: the roll ladders, +16 rows (the
							// RU RollingMill ladder joins its ULV sibling's map walk above — the
							// ROLLINGMILL_ROWS walk covers the p28 ULV rung only)
							for (GTBasicMachineBlock.MachineRow tRow : ROLLINGMILL_RU_ROWS) {
								aOutput.accept(new ItemStack(ROLLINGMILL_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : ROLL_BENDER_ROWS) {
								aOutput.accept(new ItemStack(ROLLBENDER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : ROLL_FORMER_ROWS) {
								aOutput.accept(new ItemStack(ROLLFORMER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : CLUSTER_MILL_ROWS) {
								aOutput.accept(new ItemStack(CLUSTERMILL_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							// task p29-w1-kinetic-process-ladder: the six process families, +24 rows
							// (Buzzsaw/Squeezer/Centrifuge/Sluice/SandingMachine/PressureWasher,
							// upstream row order per family)
							for (GTBasicMachineBlock.MachineRow tRow : BUZZSAW_ROWS) {
								aOutput.accept(new ItemStack(BUZZSAW_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : SQUEEZER_ROWS) {
								aOutput.accept(new ItemStack(SQUEEZER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : CENTRIFUGE_ROWS) {
								aOutput.accept(new ItemStack(CENTRIFUGE_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : SLUICE_ROWS) {
								aOutput.accept(new ItemStack(SLUICE_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : SANDING_ROWS) {
								aOutput.accept(new ItemStack(SANDING_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : PRESSURE_WASHER_ROWS) {
								aOutput.accept(new ItemStack(PRESSURE_WASHER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							// task p29-w2-eu-special: the eu-special families, +14 rows (the
							// Autocrafter/Lightning 5-tier EU ladders + the Laminator HU ladder,
							// upstream row order per family)
							for (GTBasicMachineBlock.MachineRow tRow : AUTOCRAFTER_ROWS) {
								aOutput.accept(new ItemStack(AUTOCRAFTER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : LIGHTNING_ROWS) {
								aOutput.accept(new ItemStack(LIGHTNING_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : LAMINATOR_ROWS) {
								aOutput.accept(new ItemStack(LAMINATOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : ROLLINGMILL_ROWS) {
								aOutput.accept(new ItemStack(ROLLINGMILL_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							// task p29-w1-eu-hu-families: the seven eu-hu families, +25 rows
							for (GTBasicMachineBlock.MachineRow tRow : MIXER_ROWS) {
								aOutput.accept(new ItemStack(MIXER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : ELECTRIC_MIXER_ROWS) {
								aOutput.accept(new ItemStack(ELECTRIC_MIXER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : ELECTRIC_LOOM_ROWS) {
								aOutput.accept(new ItemStack(ELECTRIC_LOOM_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : ELECTRIC_SIFTER_ROWS) {
								aOutput.accept(new ItemStack(ELECTRIC_SIFTER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : BOXINATOR_ROWS) {
								aOutput.accept(new ItemStack(BOXINATOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : UNBOXINATOR_ROWS) {
								aOutput.accept(new ItemStack(UNBOXINATOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
						for (GTBasicMachineBlock.MachineRow tRow : FERMENTER_ROWS) {
							aOutput.accept(new ItemStack(FERMENTER_ITEMS_BY_PATH.get(tRow.path()).get()));
						}
							// task p29-w2-hu-tu-piggyback: the seven hu-tu families, +16 rows
							// (SteamCracker/CatalyticCracker 4-ladders, the TU four singles,
							// the kinetic Loom 4-ladder — upstream row order per family; the
							// GTMachines. qualification dodges the simple-name forward-reference
							// rule — the fields are declared in the tail section below)
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.STEAM_CRACKER_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.STEAM_CRACKER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CATALYTIC_CRACKER_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.CATALYTIC_CRACKER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.COAGULATOR_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.COAGULATOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.GENERIFIER_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.GENERIFIER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.BATH_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.BATH_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.AUTOCLAVE_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.AUTOCLAVE_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.LOOM_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.LOOM_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							// task p29-w2-exotic-energy: the six exotic-energy families, +30 rows
							// (Polarizer/MagneticSeparator/LaserEngraver/LaserWelder/Freezer/
							// CryoMixer, upstream row order per family)
							for (GTBasicMachineBlock.MachineRow tRow : POLARIZER_ROWS) {
								aOutput.accept(new ItemStack(POLARIZER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : MAGNETIC_SEPARATOR_ROWS) {
								aOutput.accept(new ItemStack(MAGNETIC_SEPARATOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : LASER_ENGRAVER_ROWS) {
								aOutput.accept(new ItemStack(LASER_ENGRAVER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : LASER_WELDER_ROWS) {
								aOutput.accept(new ItemStack(LASER_WELDER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : FREEZER_ROWS) {
								aOutput.accept(new ItemStack(FREEZER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : CRYO_MIXER_ROWS) {
								aOutput.accept(new ItemStack(CRYO_MIXER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							// task p29-w2-eu-core-5tier: the five eu-core families, +25 rows
							// (Electrolyzer/Injector/Printer/ScannerVisuals/Slicer, the first
							// 5-tier ladders — the T5 rung rides each family walk's tail)
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.ELECTROLYZER_ROWS) {
								aOutput.accept(new ItemStack(ELECTROLYZER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : INJECTOR_ROWS) {
								aOutput.accept(new ItemStack(INJECTOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : PRINTER_ROWS) {
								aOutput.accept(new ItemStack(PRINTER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : SCANNER_VISUALS_ROWS) {
								aOutput.accept(new ItemStack(SCANNER_VISUALS_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : SLICER_ROWS) {
								aOutput.accept(new ItemStack(SLICER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							// task p29-w4-eu-bridge: the three EU-bridge families + the Roasting
							// Oven ladder, +19 rows (upstream row order per family; the
							// GTMachines. qualification dodges the tail-section declaration order)
							for (gregtech6.registry.GTMachines.BridgeRow tRow : GTMachines.ELECTRIC_HEATER_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.ELECTRIC_HEATER_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (gregtech6.registry.GTMachines.BridgeRow tRow : GTMachines.ELECTRIC_ENGINE_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.ELECTRIC_ENGINE_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (gregtech6.registry.GTMachines.BridgeRow tRow : GTMachines.ELECTRIC_MOTOR_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.ELECTRIC_MOTOR_ITEMS_BY_PATH.get(tRow.path()).get()));
							}
							for (GTBasicMachineBlock.MachineRow tRow : GTMachines.ROASTING_ROWS) {
								aOutput.accept(new ItemStack(GTMachines.ROASTING_ITEMS_BY_PATH.get(tRow.path()).get()));
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

	// ---------------------------------------------------------------------------
	// the P29 W2 hu-tu piggyback (task p29-w2-hu-tu-piggyback) — seven families, sixteen
	// MachineRow rows, all MultiTileEntityBasicMachine carriers (the W1 eu-hu shape):
	//
	// SteamCracker 20491-94 (Loader_MultiTileEntities.java:1576-1579) and CatalyticCracker
	//   20481-84 (:1570-1573) are the HU 4-ladder PAIR (Heat_T[1..4], the hardness ladder
	//   6.0/4.0/9.0/12.5, NBT_INPUT 32/128/512/2048 through the TIER_INPUTS conversion,
	//   NO NBT_PARALLEL → 1, RM.SteamCracking / RM.CatalyticCracking) — the DOUBLE-VARIANT
	//   same-parameters pair: both maps carry the IDENTICAL RM.java constants row
	//   (RM.java:67/:68 — 1/3/0 items, 2/9/1 fluids, MIN 2) and both registrations carry
	//   the IDENTICAL masks (:1570 verbatim — item+tank in SBIT_U|SBIT_L auto TOP, out
	//   SBIT_R|SBIT_B auto BACK, tank auto LEFT/RIGHT, energy SBIT_D), split only by map
	//   and NBT_TEXTURE (the art tokens "steamcracker"/"catalyticcracker").
	// Coagulator 22000 (:1651), Generifier 22001 (:1652), Bath 22002 (:1653) and Autoclave
	//   22004 (:1655) are the TU FOUR SINGLES (StainlessSteel housing — the Fermenter
	//   :1654 material word, hardness 6.0) sharing the card-① dynamics semantics:
	//   NBT_INPUT 1 + NBT_INPUT_MIN 1 + NBT_INPUT_MAX 16 (the window {1, 1, 16} —
	//   {@link #TU_WINDOW}, NOT the TIER_INPUTS[0] fold the Fermenter rides: the upstream
	//   explicit MIN/MAX keys re-bind after the :126 conversion) + NBT_NO_CONSTANT_POWER T
	//   (the :123 bind — the :894 idle reset gate is SKIPPED, the progress RETAINS across
	//   a power gap) + NBT_ENERGY_ACCEPTED_SIDES 63 (the :151 read ORs SBIT_A → 127 = ALL
	//   SIX faces, the machine-side first carrier of the card-① type-guard assertions).
	//   Generifier carries NBT_PARALLEL 100 (NO NBT_PARALLEL_DURATION → the :770-771
	//   energy-scaling arm — and as a TU carrier mMinEnergy stays mEUt, the :770 TU half).
	//   Bath rides the P26 in-catalog RM.BATH map (only rows poured, never rebuilt — the
	//   map's static wood-oil ladder pours ZERO rows live, so this card pours ONE bath.json
	//   smoke row through the card-① POURABLE key); Loom rides the W1 in-catalog RM.LOOM
	//   map (the loom.json smoke row the ElectricLoom chain already drives — the kinetic
	//   rung is the SAME-map cross-proof).
	// Loom 20211-14 (:1412-1415) is the RU kinetic 4-ladder (Kinetic_T[1..4], the
	//   hardness ladder 7.0/6.0/9.0/12.5, TIER_INPUTS windows, NO parallel, RM.Loom,
	//   item top-in/bottom-out auto TOP/BOTTOM, energy SBIT_L|SBIT_R BOTH sides — the
	//   ElectricLoom face verbatim, zero tank keys).
	//
	// ONE family BET per family (the W1-trio shape): the crackers and the loom ride the
	// SHARED kineticMachine factory body (the row drives everything); the TU four ride the
	// {@link #tuMachine} body — the machine half + applyRow + the TU window override and
	// the mNoConstantEnergy bind (the Canner card's tier-indexed-constants precedent: the
	// row record stays OUT of this card's scope, the two TU-only columns are BET-factory
	// locals). The crafting-table tails (IwI/PMP/ICI, IPI/ZMZ/ICI, T T/hMw/TdT, ChC/CMC/
	// CwC, CwC/PMP/PPP, CwC/PMP/GPG, ShS/GMG/SwS) are the unported crafting-table domain —
	// data-only, the rows carry no column for them (the buzzsaw toolHead precedent).
	//
	// KJS face of this card (the wave-plan declaration): REGISTRATION face — seven
	// families, sixteen MachineRow rows (HU 8 + TU 4 + RU 4) — plus the datapack face
	// (the STEAM_CRACKING/CATALYTIC_CRACKING/COAGULATOR/GENERIFIER/AUTOCLAVE smoke rows
	// under data/gt6/recipe_maps/ through the card-① POURABLE keys, plus ONE bath.json
	// smoke row — the BATH/LOOM maps themselves are the P26/W1 in-catalog constants,
	// reused not rebuilt; the loom.json row is the W1 in-catalog row). NO KubeJS surface.
	// The live NO_CONSTANT_POWER resume arm (the card-①遗留 obligation, double-recorded on
	// the p29_w2_energy_types chain doc and the GT6EnergyDynamicsTest doc) rides the
	// p29_w2_coagulator chain: a real TU machine RETAINS its mid-flight progress across a
	// command-idle power gap, the constant-power shredder arm resets (the card-① live
	// half's machine-side completion).
	// ---------------------------------------------------------------------------

	/** The Steam Cracker family unit word (the W1 one-slot key form; the upstream name column "Steam Cracker ("+aMat.getLocal()+")", :1576-1579). */
	public static final String MACHINE_STEAM_CRACKER_UNIT_KEY = "gt6.row.machine.steam_cracker";

	/** The Catalytic Cracker family unit word (the :1570-1573 name column). */
	public static final String MACHINE_CATALYTIC_CRACKER_UNIT_KEY = "gt6.row.machine.catalytic_cracker";

	/** The Loom family unit word (the :1412-1415 name column "Loom ("+aMat.getLocal()+")"). */
	public static final String MACHINE_LOOM_UNIT_KEY = "gt6.row.machine.loom";

	/** The Coagulator atomic display template — the single-variant row has NO slot (upstream name column "Coagulator", :1651). */
	public static final String COAGULATOR_DISPLAY_KEY = "gt6.row.coagulator.display";

	/** The Generifier atomic display template (upstream "Generifier", :1652). */
	public static final String GENERIFIER_DISPLAY_KEY = "gt6.row.generifier.display";

	/** The Bath atomic display template (upstream "Bath", :1653). */
	public static final String BATH_DISPLAY_KEY = "gt6.row.bath.display";

	/** The Autoclave atomic display template (upstream "Autoclave", :1655). */
	public static final String AUTOCLAVE_DISPLAY_KEY = "gt6.row.autoclave.display";

	/** The TU four housing material — the Fermenter StainlessSteel word (the :1651-1655 MT.StainlessSteel column, the same lazy supplier). */
	public static final java.util.function.Supplier<OreDictMaterial> TU_FOUR_MATERIAL = FERMENTER_MATERIAL;

	/**
	 * The TU four explicit energy window (the :1651-1655 NBT_INPUT 1 / NBT_INPUT_MIN 1 /
	 * NBT_INPUT_MAX 16 columns — the card-① chain dials both edges live). NOT a
	 * TIER_INPUTS row: the upstream explicit MIN/MAX keys re-bind after the :126
	 * conversion, so the port overrides the tier-0 assignment in the BET factory.
	 */
	public static final long[] TU_WINDOW = {1, 1, 16};

	/** The Generifier NBT_PARALLEL column (:1652 — no NBT_PARALLEL_DURATION key, the :770-771 energy-scaling arm). */
	public static final int GENERIFIER_PARALLEL = 100;

	/** The four Steam Cracker rows, upstream line order :1576-1579 (T1-T4, the Heat_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> STEAM_CRACKER_ROWS = java.util.List.of(
			steamcracker("steamcracker"   , "steel"           , "Steel"           , 20491,  6.0F, 0),
			steamcracker("steamcracker_t2", "invar"           , "Invar"           , 20492,  4.0F, 1),
			steamcracker("steamcracker_t3", "titanium"        , "Titanium"        , 20493,  9.0F, 2),
			steamcracker("steamcracker_t4", "tungsten_carbide", "Tungsten Carbide", 20494, 12.5F, 3));

	/** The four Catalytic Cracker rows, upstream line order :1570-1573 (T1-T4, the Heat_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> CATALYTIC_CRACKER_ROWS = java.util.List.of(
			catalyticcracker("catalyticcracker"   , "steel"           , "Steel"           , 20481,  6.0F, 0),
			catalyticcracker("catalyticcracker_t2", "invar"           , "Invar"           , 20482,  4.0F, 1),
			catalyticcracker("catalyticcracker_t3", "titanium"        , "Titanium"        , 20483,  9.0F, 2),
			catalyticcracker("catalyticcracker_t4", "tungsten_carbide", "Tungsten Carbide", 20484, 12.5F, 3));

	/** The ONE Coagulator row (upstream :1651 — the TU single, NO NBT_INV_SIDE_IN key → the 127 field default). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> COAGULATOR_ROWS = java.util.List.of(coagulator());

	/** The ONE Generifier row (upstream :1652 — the TU single with the NBT_PARALLEL 100 column). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> GENERIFIER_ROWS = java.util.List.of(generifier());

	/** The ONE Bath row (upstream :1653 — the TU single over the P26 in-catalog RM.BATH map). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> BATH_ROWS = java.util.List.of(bath());

	/** The ONE Autoclave row (upstream :1655 — the TU single, the tank-in face rides BOTTOM|LEFT). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> AUTOCLAVE_ROWS = java.util.List.of(autoclave());

	/** The four Loom rows, upstream line order :1412-1415 (T1-T4, the Kinetic_T ladder, RU). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> LOOM_ROWS = java.util.List.of(
			loom("loom"   , "bronze"       , "Bronze"       , 20211,  7.0F, 0),
			loom("loom_t2", "steel"        , "Steel"        , 20212,  6.0F, 1),
			loom("loom_t3", "titanium"     , "Titanium"     , 20213,  9.0F, 2),
			loom("loom_t4", "tungstensteel", "Tungstensteel", 20214, 12.5F, 3));

	/**
	 * The :1570 masks — the DOUBLE-VARIANT pair shape shared by both crackers (item+tank
	 * in SBIT_U|SBIT_L auto TOP, out SBIT_R|SBIT_B auto BACK, tank auto LEFT/RIGHT,
	 * energy SBIT_D; the keyed masks carry the post-read OR-SBIT_A form).
	 */
	private static GTBasicMachineBlock.MachineRow crackerRow(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier,
			java.util.function.Supplier<gregtech6.recipes.RecipeMap> aRecipes, String aTexture, String aUnitKey) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, HEAT_T_LADDER.get(aTier), aUnitKey, aMetaId, aHardness, aTier, 1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1570-1579*/,
				aRecipes, TD.Energy.HU, aTexture,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D, the :151 read ORs SBIT_A*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L, the :143 read*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R|SBIT_B, the :144 read*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L, the :137 read*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R|SBIT_B, the :138 read*/,
				(byte)2 /*NBT_TANK_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_TANK_SIDE_AUTO_OUT SIDE_RIGHT*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)5 /*NBT_INV_SIDE_AUTO_OUT SIDE_BACK*/,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/** One Steam Cracker row factory — the crackerRow shape over RM.STEAM_CRACKING and the "steamcracker" texture. */
	private static GTBasicMachineBlock.MachineRow steamcracker(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return crackerRow(aPath, aMatSlug, aMatDisplay, aMetaId, aHardness, aTier, () -> GT6RecipeMaps.STEAM_CRACKING, "steamcracker", MACHINE_STEAM_CRACKER_UNIT_KEY);
	}

	/** One Catalytic Cracker row factory — the crackerRow shape over RM.CATALYTIC_CRACKING and the "catalyticcracker" texture. */
	private static GTBasicMachineBlock.MachineRow catalyticcracker(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return crackerRow(aPath, aMatSlug, aMatDisplay, aMetaId, aHardness, aTier, () -> GT6RecipeMaps.CATALYTIC_CRACKING, "catalyticcracker", MACHINE_CATALYTIC_CRACKER_UNIT_KEY);
	}

	/** The :1651-1655 energy column — NBT_ENERGY_ACCEPTED_SIDES 63 through the :151 read (63 | SBIT_A = 127 = ALL SIX faces, the TU four's distinctive trait). */
	private static final byte TU_ENERGY_SIDES = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L
			| GTBasicMachineBlock.SBIT_F | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);

	/**
	 * The ONE Coagulator row factory (upstream :1651): TU, the {1,1,16} window + the
	 * NO_CONSTANT_POWER bind ride the BET factory (not the row), the :1651 masks —
	 * NO NBT_INV_SIDE_IN key → the 127 field default, item out SBIT_D|SBIT_R auto BOTTOM,
	 * tank in SBIT_U|SBIT_L auto TOP, NO NBT_TANK_SIDE_OUT key → 127, NO parallel.
	 */
	private static GTBasicMachineBlock.MachineRow coagulator() {
		return new GTBasicMachineBlock.MachineRow("coagulator", "stainless_steel", "StainlessSteel", TU_FOUR_MATERIAL, COAGULATOR_DISPLAY_KEY, 22000, 6.0F,
				0, 1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1651*/,
				() -> GT6RecipeMaps.COAGULATOR, TD.Energy.TU, "coagulator",
				TU_ENERGY_SIDES /*NBT_ENERGY_ACCEPTED_SIDES 63, the :151 read → all six faces*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key → the upstream field default*/,
				(byte)127 /*no NBT_INV_SIDE_IN key → the upstream field default — the fluid-only map's zero-item face*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D|SBIT_R*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)-1 /*no NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED*/,
				(byte)-1 /*no NBT_INV_SIDE_AUTO_IN key → SIDE_UNDEFINED*/, (byte)0 /*NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/**
	 * The ONE Generifier row factory (upstream :1652): the coagulator energy face over the
	 * full :1652 masks (item+tank in SBIT_U|SBIT_L auto LEFT/TOP, out SBIT_D|SBIT_R auto
	 * RIGHT/BOTTOM) and the NBT_PARALLEL 100 column.
	 */
	private static GTBasicMachineBlock.MachineRow generifier() {
		return new GTBasicMachineBlock.MachineRow("generifier", "stainless_steel", "StainlessSteel", TU_FOUR_MATERIAL, GENERIFIER_DISPLAY_KEY, 22001, 6.0F,
				0, GENERIFIER_PARALLEL, false /*NBT_PARALLEL 100 with NO NBT_PARALLEL_DURATION key :1652 — the :770-771 arm*/,
				() -> GT6RecipeMaps.GENERIFIER, TD.Energy.TU, "generifier",
				TU_ENERGY_SIDES /*NBT_ENERGY_ACCEPTED_SIDES 63 → all six faces*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_D|SBIT_R*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D|SBIT_R*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/**
	 * The ONE Bath row factory (upstream :1653): the generifier masks verbatim over the
	 * P26 in-catalog RM.BATH map (reused, never rebuilt — this card pours the rows only)
	 * and NO parallel column.
	 */
	private static GTBasicMachineBlock.MachineRow bath() {
		return new GTBasicMachineBlock.MachineRow("bath", "stainless_steel", "StainlessSteel", TU_FOUR_MATERIAL, BATH_DISPLAY_KEY, 22002, 6.0F,
				0, 1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1653*/,
				() -> GT6RecipeMaps.BATH, TD.Energy.TU, "bath",
				TU_ENERGY_SIDES /*NBT_ENERGY_ACCEPTED_SIDES 63 → all six faces*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_D|SBIT_R*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D|SBIT_R*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/**
	 * The ONE Autoclave row factory (upstream :1655): the distinctive masks — item in
	 * SBIT_U|SBIT_L auto LEFT, item out SBIT_B|SBIT_R auto RIGHT, tank in SBIT_D|SBIT_L
	 * auto BOTTOM, tank out SBIT_B|SBIT_R auto BACK, all six energy faces.
	 */
	private static GTBasicMachineBlock.MachineRow autoclave() {
		return new GTBasicMachineBlock.MachineRow("autoclave", "stainless_steel", "StainlessSteel", TU_FOUR_MATERIAL, AUTOCLAVE_DISPLAY_KEY, 22004, 6.0F,
				0, 1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1655*/,
				() -> GT6RecipeMaps.AUTOCLAVE, TD.Energy.TU, "autoclave",
				TU_ENERGY_SIDES /*NBT_ENERGY_ACCEPTED_SIDES 63 → all six faces*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_D|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_B|SBIT_R*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_B|SBIT_R*/,
				(byte)0 /*NBT_TANK_SIDE_AUTO_IN SIDE_BOTTOM*/, (byte)5 /*NBT_TANK_SIDE_AUTO_OUT SIDE_BACK*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/**
	 * One Loom row factory (upstream :1412-1415): RU kinetic, the :1412 masks — item
	 * top-in/bottom-out (auto TOP/BOTTOM), energy SBIT_L|SBIT_R BOTH sides (the
	 * ElectricLoom face), ZERO tank keys → the 127 defaults, no parallel.
	 */
	private static GTBasicMachineBlock.MachineRow loom(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, KINETIC_T_LADDER.get(aTier), MACHINE_LOOM_UNIT_KEY, aMetaId, aHardness, aTier, 1, false /*no NBT_PARALLEL, no NBT_PARALLEL_DURATION :1412-1415*/,
				() -> GT6RecipeMaps.LOOM, TD.Energy.RU, "loom",
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_L|SBIT_R — both sides*/,
				(byte)127 /*no NBT_TANK_SIDE_IN key → the upstream field default*/,
				(byte)127 /*no NBT_TANK_SIDE_OUT key*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U — top in*/,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_D — bottom out*/,
				(byte)-1 /*SIDE_UNDEFINED*/, (byte)-1 /*SIDE_UNDEFINED*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)0 /*NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM*/,
				null /*the menu-less carrier*/, true /*NBT_CHEAP_OVERCLOCKING*/, null /*no melting gate*/, false);
	}

	/** The registered Steam Cracker blocks by path (the BET/datagen/loot walkers + /gt6machine place iterate this). */
	public static final java.util.Map<String, RegistryObject<Block>> STEAM_CRACKER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Steam Cracker items, same keys as {@link #STEAM_CRACKER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> STEAM_CRACKER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Catalytic Cracker blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> CATALYTIC_CRACKER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Catalytic Cracker items, same keys as {@link #CATALYTIC_CRACKER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> CATALYTIC_CRACKER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Coagulator blocks by path (the single-variant ladder). */
	public static final java.util.Map<String, RegistryObject<Block>> COAGULATOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Coagulator items. */
	public static final java.util.Map<String, RegistryObject<Item>> COAGULATOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Generifier blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> GENERIFIER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Generifier items. */
	public static final java.util.Map<String, RegistryObject<Item>> GENERIFIER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Bath blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> BATH_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Bath items. */
	public static final java.util.Map<String, RegistryObject<Item>> BATH_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Autoclave blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> AUTOCLAVE_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Autoclave items. */
	public static final java.util.Map<String, RegistryObject<Item>> AUTOCLAVE_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Loom blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> LOOM_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Loom items, same keys as {@link #LOOM_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> LOOM_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : STEAM_CRACKER_ROWS) {
			STEAM_CRACKER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.STEAM_CRACKER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			STEAM_CRACKER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.STEAM_CRACKER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : CATALYTIC_CRACKER_ROWS) {
			CATALYTIC_CRACKER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.CATALYTIC_CRACKER_BE.get(), tRow)));
			CATALYTIC_CRACKER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.CATALYTIC_CRACKER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : COAGULATOR_ROWS) {
			COAGULATOR_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.COAGULATOR_BE.get(), tRow)));
			COAGULATOR_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.COAGULATOR_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : GENERIFIER_ROWS) {
			GENERIFIER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.GENERIFIER_BE.get(), tRow)));
			GENERIFIER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.GENERIFIER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : BATH_ROWS) {
			BATH_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.BATH_BE.get(), tRow)));
			BATH_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.BATH_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : AUTOCLAVE_ROWS) {
			AUTOCLAVE_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.AUTOCLAVE_BE.get(), tRow)));
			AUTOCLAVE_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.AUTOCLAVE_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : LOOM_ROWS) {
			LOOM_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.LOOM_BE.get(), tRow)));
			LOOM_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.LOOM_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Steam Cracker block list in registration order (the loot/datagen walkers). */
	public static Block[] steamcrackerBlockArray() {
		Block[] rBlocks = new Block[STEAM_CRACKER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : STEAM_CRACKER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Catalytic Cracker block list in registration order. */
	public static Block[] catalyticcrackerBlockArray() {
		Block[] rBlocks = new Block[CATALYTIC_CRACKER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : CATALYTIC_CRACKER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Coagulator block list in registration order (the single-variant ladder). */
	public static Block[] coagulatorBlockArray() {
		Block[] rBlocks = new Block[COAGULATOR_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : COAGULATOR_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Generifier block list in registration order. */
	public static Block[] generifierBlockArray() {
		Block[] rBlocks = new Block[GENERIFIER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : GENERIFIER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Bath block list in registration order. */
	public static Block[] bathBlockArray() {
		Block[] rBlocks = new Block[BATH_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : BATH_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Autoclave block list in registration order. */
	public static Block[] autoclaveBlockArray() {
		Block[] rBlocks = new Block[AUTOCLAVE_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : AUTOCLAVE_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Loom block list in registration order. */
	public static Block[] loomBlockArray() {
		Block[] rBlocks = new Block[LOOM_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : LOOM_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	// the seven hu-tu family BETs: the W1-trio shape verbatim — the crackers and the loom
	// ride the SHARED kineticMachine factory body, the TU four the {@link #tuMachine} body
	// (the {1,1,16} window override + the NO_CONSTANT_POWER bind live there).

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> STEAM_CRACKER_BE =
			BLOCK_ENTITY_TYPES.register("steamcracker", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.STEAM_CRACKER_BE.get(), aPos, aState),
					steamcrackerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> CATALYTIC_CRACKER_BE =
			BLOCK_ENTITY_TYPES.register("catalyticcracker", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.CATALYTIC_CRACKER_BE.get(), aPos, aState),
					catalyticcrackerBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> COAGULATOR_BE =
			BLOCK_ENTITY_TYPES.register("coagulator", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> tuMachine(GTMachines.COAGULATOR_BE.get(), aPos, aState),
					coagulatorBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> GENERIFIER_BE =
			BLOCK_ENTITY_TYPES.register("generifier", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> tuMachine(GTMachines.GENERIFIER_BE.get(), aPos, aState),
					generifierBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> BATH_BE =
			BLOCK_ENTITY_TYPES.register("bath", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> tuMachine(GTMachines.BATH_BE.get(), aPos, aState),
					bathBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> AUTOCLAVE_BE =
			BLOCK_ENTITY_TYPES.register("autoclave", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> tuMachine(GTMachines.AUTOCLAVE_BE.get(), aPos, aState),
					autoclaveBlockArray()).build(null));

	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> LOOM_BE =
			BLOCK_ENTITY_TYPES.register("loom", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.LOOM_BE.get(), aPos, aState),
					loomBlockArray()).build(null));

	/**
	 * The TU four BET factory body (the Canner card's tier-indexed-constants precedent —
	 * the row record stays out of this card's scope, so the two TU-only columns are
	 * factory locals): the kineticMachine half verbatim, then the {@link #TU_WINDOW}
	 * {1, 1, 16} override (the upstream :1651-1655 explicit NBT_INPUT_MIN/NBT_INPUT_MAX
	 * re-bind after the :126 conversion — the Fermenter's 16/32/64 fold does NOT apply)
	 * and the NBT_NO_CONSTANT_POWER T bind on {@code mNoConstantEnergy} (the :123 key —
	 * the :894 idle reset gate is skipped, the progress RETAINS across a power gap).
	 */
	private static TileEntityBasicMachine tuMachine(BlockEntityType<TileEntityBasicMachine> aType, net.minecraft.core.BlockPos aPos,
			net.minecraft.world.level.block.state.BlockState aState) {
		GTBasicMachineBlock.MachineRow tRow = ((GTBasicMachineBlock)aState.getBlock()).row();
		TileEntityBasicMachine tMachine = machine(aType, aPos, aState, tRow.recipes().get(), tRow.parallel(), tRow.parallelDuration(), tRow.energyType(), tRow.tier(), tRow.menu());
		return applyTuRow(tMachine, tRow);
	}

	/**
	 * The TU-column assignment (public — the offline row test drives it against the fixture
	 * machine, the {@link #applyRow} seam shape): applyRow's masks + the two TU-only
	 * columns — {@code mNoConstantEnergy = true} (the :123 NBT_NO_CONSTANT_POWER T bind)
	 * and the {@link #TU_WINDOW} {1, 1, 16} override.
	 */
	public static TileEntityBasicMachine applyTuRow(TileEntityBasicMachine aMachine, GTBasicMachineBlock.MachineRow aRow) {
		applyRow(aMachine, aRow);
		aMachine.mNoConstantEnergy = true; // the :123 NBT_NO_CONSTANT_POWER T bind (:1651-1655)
		aMachine.mInputMin = TU_WINDOW[0];
		aMachine.mInput = TU_WINDOW[1];
		aMachine.mInputMax = TU_WINDOW[2];
		return aMachine;
	}

	// ---------------------------------------------------------------------------
	// the P29 W3 heat-smelter section (task p29-w3-heat-smelter — the GTMachines.java
	// EXCLUSIVE append of the wave, the structural conflict-elimination ruling): the
	// Smelter HU 4-ladder 20241-20244 (Loader_MultiTileEntities.java:1431-1434, the
	// Heat_T[1..4] ladder, NBT_TEXTURE "smelter", RM.Smelter, NBT_CHEAP_OVERCLOCKING T,
	// NBT_PARALLEL 1000 + NBT_PARALLEL_DURATION T) and the ONE Melter 22010 (:1657,
	// ANY.Iron, RM.Melter, the same CHEAP_OC + PARALLEL 1000 + DURATION T columns).
	// Both families share the :1431 masks verbatim — inv+tank in SBIT_U auto TOP,
	// inv out SBIT_L auto LEFT, tank out SBIT_R auto RIGHT, energy SBIT_D — and ride
	// the SHARED kineticMachine factory body (the row drives everything; the window is
	// the TIER_INPUTS[tier] assignment 32/128/512/2048 = the upstream NBT_INPUT column
	// exactly). The crafting-table tails ('wUh','PMP','BCB' with the furnace-class U
	// items 1024/1019/1019/1043 = getItem(1005) for the Melter) are the unported
	// crafting-table domain — data-only, the rows carry no column for them (the W2
	// cracker ruling).
	//
	// KJS face of this card (the wave-plan declaration): REGISTRATION face — five
	// MachineRow rows (Smelter 4 + Melter 1) plus the HEX controller (GT6HeatExchangers,
	// the self-contained registry) — plus the datapack face (the SMELTER/MELTER/FM.Hot
	// three smoke rows under data/gt6/recipe_maps/ through the C1 POURABLE keys). NO
	// KubeJS surface.
	// ---------------------------------------------------------------------------

	/** The Smelter family unit word (the W2 one-slot key form; the upstream name column "Smelter ("+aMat.getLocal()+")", :1431-1434). */
	public static final String MACHINE_SMELTER_UNIT_KEY = "gt6.row.machine.smelter";

	/** The Melter atomic display template — the single-variant row has NO slot (upstream name column "Melter", :1657). */
	public static final String MELTER_DISPLAY_KEY = "gt6.row.melter.display";

	/** The Melter housing material — the :1657 ANY.Iron column. */
	public static final java.util.function.Supplier<OreDictMaterial> MELTER_MATERIAL = () -> gregapi.data.ANY.Iron;

	/** The Smelter/Melter PARALLEL column (:1431-1434/:1657 — NBT_PARALLEL 1000 + NBT_PARALLEL_DURATION T on every row). */
	public static final int SMELTER_PARALLEL = 1000;

	/** The four Smelter rows, upstream line order :1431-1434 (T1-T4, the Heat_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> SMELTER_ROWS = java.util.List.of(
			smelter("smelter"   , "steel"           , "Steel"           , 20241,  6.0F, 0),
			smelter("smelter_t2", "invar"           , "Invar"           , 20242,  4.0F, 1),
			smelter("smelter_t3", "titanium"        , "Titanium"        , 20243,  9.0F, 2),
			smelter("smelter_t4", "tungsten_carbide", "Tungsten Carbide", 20244, 12.5F, 3));

	/** The ONE Melter row (upstream :1657 — the HU single over ANY.Iron). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> MELTER_ROWS = java.util.List.of(melter());

	/**
	 * The :1431 masks — the family shape shared by the Smelter ladder and the Melter
	 * (item+tank in SBIT_U auto TOP, inv out SBIT_L auto LEFT, tank out SBIT_R auto
	 * RIGHT, energy SBIT_D; PARALLEL 1000 + DURATION T on every row; the keyed masks
	 * carry the post-read OR-SBIT_A form).
	 */
	private static GTBasicMachineBlock.MachineRow heatSmelterRow(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier,
			java.util.function.Supplier<gregtech6.recipes.RecipeMap> aRecipes, TagData aEnergyType, String aTexture, String aDisplayKey,
			java.util.function.Supplier<OreDictMaterial> aMaterial) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, aMaterial, aDisplayKey, aMetaId, aHardness, aTier, SMELTER_PARALLEL, true /*NBT_PARALLEL_DURATION T*/,
				aRecipes, aEnergyType, aTexture,
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D, the :151 read*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_U, the :143 read*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_R, the :144 read*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_U, the :137 read*/,
				(byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_L, the :138 read*/,
				(byte)1 /*NBT_TANK_SIDE_AUTO_IN SIDE_TOP*/, (byte)4 /*NBT_TANK_SIDE_AUTO_OUT SIDE_RIGHT*/,
				(byte)1 /*NBT_INV_SIDE_AUTO_IN SIDE_TOP*/, (byte)2 /*NBT_INV_SIDE_AUTO_OUT SIDE_LEFT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING — :773 unconditional*/, null /*no melting gate*/, false);
	}

	/** One Smelter row factory — the :1431 shape over RM.Smelter and the "smelter" texture (the Heat_T material-word slot). */
	private static GTBasicMachineBlock.MachineRow smelter(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		// the TD.Energy.<KIND>, "<family>", literal order is the borrow_port_overlays census key
		return heatSmelterRow(aPath, aMatSlug, aMatDisplay, aMetaId, aHardness, aTier, () -> GT6RecipeMaps.SMELTER, TD.Energy.HU, "smelter", MACHINE_SMELTER_UNIT_KEY, HEAT_T_LADDER.get(aTier));
	}

	/** The ONE Melter row factory — the :1657 shape over RM.Melter and the "melter" texture (the atomic no-slot display). */
	private static GTBasicMachineBlock.MachineRow melter() {
		return heatSmelterRow("melter", "iron", "Iron", 22010, 6.0F, 0, () -> GT6RecipeMaps.MELTER, TD.Energy.HU, "melter", MELTER_DISPLAY_KEY, MELTER_MATERIAL);
	}

	/** The registered Smelter blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> SMELTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Smelter items, same keys as {@link #SMELTER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> SMELTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Melter blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> MELTER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Melter items, same keys as {@link #MELTER_BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> MELTER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : SMELTER_ROWS) {
			SMELTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.SMELTER_BE.get(), tRow)));
			// the GT6Boilers qualified-read forward-reference form (the P6 lambda lesson)
			SMELTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.SMELTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
		for (GTBasicMachineBlock.MachineRow tRow : MELTER_ROWS) {
			MELTER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.MELTER_BE.get(), tRow)));
			MELTER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new gregtech6.block.GTComposedNameItem(GTMachines.MELTER_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Smelter block list in registration order (the loot/datagen walkers). */
	public static Block[] smelterBlockArray() {
		Block[] rBlocks = new Block[SMELTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : SMELTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The Melter block list in registration order. */
	public static Block[] melterBlockArray() {
		Block[] rBlocks = new Block[MELTER_BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<Block> tBlock : MELTER_BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6machine smelter — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block smelterBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = SMELTER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The lookup for /gt6machine melter — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block melterBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = MELTER_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The Smelter family BET: the Wiremill shape verbatim — the shared kineticMachine factory, the four tier blocks multi-attached. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> SMELTER_BE =
			BLOCK_ENTITY_TYPES.register("smelter", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.SMELTER_BE.get(), aPos, aState),
					smelterBlockArray()).build(null));

	/** The Melter family BET: the same kineticMachine body over its one block. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> MELTER_BE =
			BLOCK_ENTITY_TYPES.register("melter", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.MELTER_BE.get(), aPos, aState),
					melterBlockArray()).build(null));

	// ---------------------------------------------------------------------------
	// the P29 W4 EU-bridge card (task p29-w4-eu-bridge) — the three EU→X converter
	// families (Loader_MultiTileEntities.java:815-821/:831-837/:847-853) + the Roasting
	// Oven 4-ladder (:1386-1389). The bridges are GT-INTERNAL energy converters, NOT
	// outbound bridges: EU never leaves the GT grid through them (the P28 ruling,
	// decisions.p28-cut-eu-fe-bridge — the emission face is the native TD.Energy.HU/KU/RU
	// push, consumed by the crucible/HEX heat face, the kinetic axle and the kinetic
	// machine faces that already live in this repo). KJS face of this card: REGISTRATION
	// face (15 converter rows + 4 Roasting rows) + datapack domain (RM.ROASTING rows via
	// the tier-b JSON seam); NO KubeJS surface.
	//
	// The bridges ride the GT6DynamoBlock carrier (the p28 dynamo family block class,
	// reused UNTOUCHED: FRONT = the emission face, BACK = the EU input face, the tier
	// index selects the ladder rung) over a SHARED conversion core — the nested
	// {@link ElectricBridgeBlockEntity} extends {@link GT6DynamoBlockEntity} (the
	// TileEntityBase10EnergyConverter :45-180 port core) and re-types the INPUT arm
	// RU → EU (the core's ONLY hardcoded family axis): capacitor = NBT_INPUT × 2
	// (Base10:75), the input band min = in/2 (Base10:76, in > 16 on every row here),
	// rec = in, max = in × 2; the output band min = out/2, rec = out, max = out × 2
	// (Base10:77). THE HALF-RATE MECHANISM (the units() semantics — doConversion :62):
	// {@code tOutput = units(mStorage, mInput, mOutput, F)} = stored × mOutput/mInput,
	// and every ladder row carries mOutput = mInput / 2 exactly (32/16, 128/64, 512/256,
	// 2048/1024, 8192/4096 — the Loader rows verbatim), so one tick converts stored/2:
	// "出恒半 = in×2 = out" is NOT a separate loss factor, it IS the units() conversion
	// between the row's own NBT_INPUT and NBT_OUTPUT columns. WASTE_ENERGY = T (the
	// :92 tail, aMode = 0): every tick the capacitor vents units(mInput × 2, 16, 16, T)
	// = 2 × NBT_INPUT REGARDLESS of output demand — the intake is never gated by the
	// consumer side, the bucket empties every tick (the dynamo-core waste=T semantics
	// verbatim; the emitted half is NOT deducted — the :81/:87 arms are skipped).
	//
	// The three upstream families collapse onto ONE BE class: the Heater is the
	// TileEntityBase10EnergyConverter core itself, the Motor rides TileEntityBase11Motor
	// (the same converter core plus the counter-clockwise toggle — pooled, the mMode
	// crop precedent) and the Engine carries its own 32-state screwdriver ladder
	// (MultiTileEntityEngineElectric :118-133, mState = 15 = the full-rate rung — the
	// state ladder crops to the constant full rate here, the same declared mMode=0 crop
	// the transformer/dynamo cores carry). All three rows share the identical
	// NBT_INPUT/NBT_OUTPUT ladders and WASTE_ENERGY = T, so the port carries ONE class
	// parameterized by the emitted type (HU / KU / RU) — the Flux/Electric dynamo
	// two-classes-one-core shape generalized.
	//
	// Emission signs: TD.Energy.ALL_NEGATIVE_ALLOWED = (AU, QU, MU, KU, RU, EU)
	// (TD.java:202) — HU is NOT in it, so the Heater emits positive-size HU packets
	// only; the KU/RU families ride the live ±sign (the aNegative conjunct:
	// the EU input IS negative-allowed, Base10:121).
	// ---------------------------------------------------------------------------

	/** One EU-bridge ladder row — the upstream-parity columns of one Loader aRegistry.add line (:817-821/:833-837/:849-853). */
	public record BridgeRow(String path, int metaId, int tier, String voltageWord, TagData outType) {}

	/** The shared NBT_INPUT ladder of all three families (EU in, the Loader rows verbatim). */
	public static final long[] BRIDGE_INPUTS = {32, 128, 512, 2048, 8192};

	/** The shared NBT_OUTPUT ladder of all three families (out = in/2 EXACTLY — the units() half-rate, the class-doc mechanism). */
	public static final long[] BRIDGE_OUTPUTS = {16, 64, 256, 1024, 4096};

	/** The voltage words of the five rungs (upstream VN[1..5], CS.java:154 — the name column "Electric Heater (LV)" form). */
	public static final java.util.List<String> BRIDGE_VOLTAGE_WORDS = java.util.List.of("LV", "MV", "HV", "EV", "IV");

	/** The Heater rows, upstream line order :817-821 (ids 10001-10005, the EU→HU family). */
	public static final java.util.List<BridgeRow> ELECTRIC_HEATER_ROWS;
	/** The Engine rows, upstream line order :833-837 (ids 10011-10015, the EU→KU family). */
	public static final java.util.List<BridgeRow> ELECTRIC_ENGINE_ROWS;
	/** The Motor rows, upstream line order :849-853 (ids 10021-10025, the EU→RU family). */
	public static final java.util.List<BridgeRow> ELECTRIC_MOTOR_ROWS;

	static {
		java.util.List<BridgeRow> tRows = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) tRows.add(new BridgeRow(bridgePath("electric_heater", i), 10001 + i, i, BRIDGE_VOLTAGE_WORDS.get(i), TD.Energy.HU));
		ELECTRIC_HEATER_ROWS = java.util.List.copyOf(tRows);
		tRows = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) tRows.add(new BridgeRow(bridgePath("electric_engine", i), 10011 + i, i, BRIDGE_VOLTAGE_WORDS.get(i), TD.Energy.KU));
		ELECTRIC_ENGINE_ROWS = java.util.List.copyOf(tRows);
		tRows = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) tRows.add(new BridgeRow(bridgePath("electric_motor", i), 10021 + i, i, BRIDGE_VOLTAGE_WORDS.get(i), TD.Energy.RU));
		ELECTRIC_MOTOR_ROWS = java.util.List.copyOf(tRows);
	}

	/** The rung path: T1 carries the bare family name, T2-T5 the _tN suffix (the W2 exotic five-rung convention). */
	private static String bridgePath(String aFamily, int aTier) {
		return aTier == 0 ? aFamily : aFamily + "_t" + (aTier + 1);
	}

	/** The registered Heater blocks by path (the BET/datagen/loot/command walkers). */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTRIC_HEATER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The registered Heater items, same keys. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTRIC_HEATER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The registered Engine blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTRIC_ENGINE_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The registered Engine items, same keys. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTRIC_ENGINE_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The registered Motor blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ELECTRIC_MOTOR_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The registered Motor items, same keys. */
	public static final java.util.Map<String, RegistryObject<Item>> ELECTRIC_MOTOR_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		registerBridgeFamily(ELECTRIC_HEATER_ROWS, ELECTRIC_HEATER_BLOCKS_BY_PATH, ELECTRIC_HEATER_ITEMS_BY_PATH, () -> GTMachines.ELECTRIC_HEATER_BE);
		registerBridgeFamily(ELECTRIC_ENGINE_ROWS, ELECTRIC_ENGINE_BLOCKS_BY_PATH, ELECTRIC_ENGINE_ITEMS_BY_PATH, () -> GTMachines.ELECTRIC_ENGINE_BE);
		registerBridgeFamily(ELECTRIC_MOTOR_ROWS, ELECTRIC_MOTOR_BLOCKS_BY_PATH, ELECTRIC_MOTOR_ITEMS_BY_PATH, () -> GTMachines.ELECTRIC_MOTOR_BE);
	}

	/**
	 * One bridge family's block+item registration walk — the W1 BY_PATH form over the
	 * REUSED {@link GT6DynamoBlock} carrier (the qualified-read forward-reference lambda,
	 * the P6 lesson). Hardness/resistance 4.0 (the NBT_HARDNESS column, every row), stack
	 * 16 (the upstream stack column).
	 */
	private static void registerBridgeFamily(java.util.List<BridgeRow> aRows,
			java.util.Map<String, RegistryObject<Block>> aBlocks, java.util.Map<String, RegistryObject<Item>> aItems,
			java.util.function.Supplier<RegistryObject<BlockEntityType<ElectricBridgeBlockEntity>>> aBe) {
		for (BridgeRow tRow : aRows) {
			aBlocks.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6DynamoBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
							.strength(4.0F, 4.0F).sound(SoundType.METAL), tRow.tier(), () -> aBe.get().get())));
			aItems.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(aBlocks.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	/** The Heater family BET — one BE class, the five ladder blocks multi-attached, the HU emission type captured per factory. */
	public static final RegistryObject<BlockEntityType<ElectricBridgeBlockEntity>> ELECTRIC_HEATER_BE =
			BLOCK_ENTITY_TYPES.register("electric_heater", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new ElectricBridgeBlockEntity(GTMachines.ELECTRIC_HEATER_BE.get(), TD.Energy.HU, aPos, aState),
					bridgeBlockArray(ELECTRIC_HEATER_ROWS, ELECTRIC_HEATER_BLOCKS_BY_PATH)).build(null));

	/** The Engine family BET (the KU emission type). */
	public static final RegistryObject<BlockEntityType<ElectricBridgeBlockEntity>> ELECTRIC_ENGINE_BE =
			BLOCK_ENTITY_TYPES.register("electric_engine", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new ElectricBridgeBlockEntity(GTMachines.ELECTRIC_ENGINE_BE.get(), TD.Energy.KU, aPos, aState),
					bridgeBlockArray(ELECTRIC_ENGINE_ROWS, ELECTRIC_ENGINE_BLOCKS_BY_PATH)).build(null));

	/** The Motor family BET (the RU emission type). */
	public static final RegistryObject<BlockEntityType<ElectricBridgeBlockEntity>> ELECTRIC_MOTOR_BE =
			BLOCK_ENTITY_TYPES.register("electric_motor", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new ElectricBridgeBlockEntity(GTMachines.ELECTRIC_MOTOR_BE.get(), TD.Energy.RU, aPos, aState),
					bridgeBlockArray(ELECTRIC_MOTOR_ROWS, ELECTRIC_MOTOR_BLOCKS_BY_PATH)).build(null));

	/** The block list of one bridge family in registration order (the BET varargs). */
	private static Block[] bridgeBlockArray(java.util.List<BridgeRow> aRows, java.util.Map<String, RegistryObject<Block>> aBlocks) {
		Block[] rBlocks = new Block[aRows.size()];
		for (int i = 0; i < rBlocks.length; i++) rBlocks[i] = aBlocks.get(aRows.get(i).path()).get();
		return rBlocks;
	}

	/** The lookup for /gt6bridge — null for an unknown family name. */
	@javax.annotation.Nullable
	public static TagData bridgeOutType(String aFamily) {
		return switch (aFamily) {
			case "heater" -> TD.Energy.HU;
			case "engine" -> TD.Energy.KU;
			case "motor" -> TD.Energy.RU;
			default -> null;
		};
	}

	/**
	 * The shared EU→X conversion core of the three bridge families — the
	 * {@link GT6DynamoBlockEntity} core (the TileEntityBase10EnergyConverter :45-180
	 * port) with the input arm re-typed RU → EU. EVERYTHING else is the core verbatim:
	 * the capacitor = in × 2, the min/rec/max bands, the per-tick units() conversion
	 * (the half-rate mechanism — see the segment doc), the WASTE_ENERGY = T vent, the
	 * BACK-in/FRONT-out faces, the 100-strike overload ladder. The accounting pair
	 * {@code gt.last_in}/{@code gt.last_out} (EU consumed / X emitted, cumulative,
	 * persisted) is the live RCON acceptance face — with the waste vent pinning the
	 * per-tick pairing, one dial tick contributes exactly in = rec EU and out = rec/2 X.
	 */
	public static class ElectricBridgeBlockEntity extends GT6DynamoBlockEntity {

		/** The persisted cumulative EU intake (EU units — consumed packet mass). */
		public static final String NBT_LAST_IN = "gt.last_in";
		/** The persisted cumulative emission (the emitted type's own units). */
		public static final String NBT_LAST_OUT = "gt.last_out";

		/** The family's emitted type (HU / KU / RU — the Loader NBT_ENERGY_EMITTED column). */
		private final TagData mOutType;

		/** Cumulative EU consumed (doInject whole packets). */
		public long mLastIn = 0;
		/** Cumulative emitted mass (whole accepted packets — the emission packet is atomic at size tOutput). */
		public long mLastOut = 0;

		/** The offline test seam — the core's {@code setAdjacencyOverride} is package-private to its home package. */
		private @javax.annotation.Nullable gregapi.tileentity.energy.IEnergyAdjacency mAdjacencyOverride = null;

		/** BET factory — resolves the shared type through the registry at runtime. */
		public ElectricBridgeBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			this(null, TD.Energy.HU, aPos, aState);
		}

		/** Full constructor — the type-capturing BET factories and the offline (test) entry point (the dual-constructor precedent). */
		public ElectricBridgeBlockEntity(@javax.annotation.Nullable BlockEntityType<?> aType, TagData aOutType,
				net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
			mOutType = aOutType;
			applyTier(aState, BRIDGE_INPUTS, BRIDGE_OUTPUTS);
		}

		@Override
		public String getTileEntityName() {
			return "electric_bridge"; // the shared-class name; the family lives in the BET id (electric_heater/electric_engine/electric_motor)
		}

		@Override
		public TagData outputType() {
			return mOutType; // the Loader NBT_ENERGY_EMITTED column; widened to public for the /gt6bridge stat arm
		}

		@Override
		protected boolean negativeOutputAllowed() {
			return TD.Energy.ALL_NEGATIVE_ALLOWED.contains(mOutType); // HU = false (positive packets only); KU/RU = true (the live ±sign)
		}

		@Override
		protected long emitConverted(long tOutput, boolean aNegative) {
			// the Converter :85 size-carrying branch: ONE packet, size = ±tOutput, amount 1;
			// the Util loops the sides, isEnergyEmittingTo gates FRONT-only (the core face)
			long tSign = aNegative ? -1 : 1;
			long tUsed = ITileEntityEnergy.Util.emitEnergyToNetwork(mOutType, tSign * tOutput, 1, this, adjacency());
			if (tUsed > 0) mLastOut += tOutput; // the packet is atomic: accepted = the whole tOutput landed
			return tUsed;
		}

		// --- the EU input arm: the core's ONLY family axis (RU) re-typed to EU ---

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			// upstream :150: (aEmitting ? mEnergyOUT : mEnergyIN).isType(aEnergyType)
			return aEmitting ? aEnergyType == outputType() : aEnergyType == TD.Energy.EU;
		}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
			if (aEnergyType != TD.Energy.EU) return 0;
			return mInput <= 16 ? 1 : mInput / 2; // Base10:76 verbatim (in > 16 on every row here)
		}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
			return aEnergyType == TD.Energy.EU ? mInput : 0;
		}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
			return aEnergyType == TD.Energy.EU ? mInput * 2 : 0;
		}

		@Override
		public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
			// upstream :159 — both converter halves
			return java.util.Arrays.asList(TD.Energy.EU, outputType());
		}

		@Override
		public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			long tConsumed = super.doInject(aEnergyType, aSide, aSize, aAmount, aDoInject);
			if (aDoInject && tConsumed > 0) mLastIn += tConsumed * Math.abs(aSize); // the Stats whole-packet mass
			return tConsumed;
		}

		/** The live accounting snapshot arm — resets both counters (the RCON phase determinism). */
		public void resetAccounting() {
			mLastIn = 0;
			mLastOut = 0;
		}

		/** The offline test seam for the emit side (the core's seam is package-private to gregtech6.tileentity.energy). */
		public void setAdjacencyOverrideForTest(@javax.annotation.Nullable gregapi.tileentity.energy.IEnergyAdjacency aAdjacency) {
			mAdjacencyOverride = aAdjacency;
		}

		@Override
		protected gregapi.tileentity.energy.IEnergyAdjacency adjacency() {
			if (mAdjacencyOverride != null) return mAdjacencyOverride;
			return super.adjacency();
		}

		@Override
		protected void saveAdditional(net.minecraft.nbt.CompoundTag aNBT) {
			super.saveAdditional(aNBT);
			aNBT.putLong(NBT_LAST_IN, mLastIn);
			aNBT.putLong(NBT_LAST_OUT, mLastOut);
		}

		@Override
		public void load(net.minecraft.nbt.CompoundTag aNBT) {
			super.load(aNBT);
			if (aNBT.contains(NBT_LAST_IN, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) mLastIn = Math.max(0, aNBT.getLong(NBT_LAST_IN));
			if (aNBT.contains(NBT_LAST_OUT, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) mLastOut = Math.max(0, aNBT.getLong(NBT_LAST_OUT));
		}
	}

	// ---------------------------------------------------------------------------
	// the Roasting Oven 4-ladder (task p29-w4-eu-bridge, Loader_MultiTileEntities.java
	// :1386-1389) — the HU recipe machine over RM.ROASTING, the Heat_T[1..4] material
	// ladder (the Oven/Dryer word set), the smelter-family row shape with the :1386
	// deltas: hardness 6.0/4.0/9.0/12.5, NBT_PARALLEL {1, 2, 4, 8} with NO
	// NBT_PARALLEL_DURATION key (parallelDuration = F — the parallel arm is the plain
	// mInput-bound count, NOT the duration-folding one), NBT_CHEAP_OVERCLOCKING T
	// (the :773 unconditional port face), NBT_TEXTURE "roaster", the display column
	// "Roasting Oven ("+aMat.getLocal()+")" (the material-word slot — the OVEN name
	// form). Masks (:1386 verbatim + the ORed SBIT_A): item+tank in SBIT_B|SBIT_L auto
	// LEFT/BACK, item out SBIT_R auto RIGHT, tank out SBIT_U auto TOP, energy SBIT_D.
	// ---------------------------------------------------------------------------

	/** The Roasting Oven family unit word (the one-slot key form; the upstream name column "Roasting Oven ("+aMat.getLocal()+")", :1386-1389). */
	public static final String MACHINE_ROASTING_OVEN_UNIT_KEY = "gt6.row.machine.roasting_oven";

	/** The :1386-1389 NBT_PARALLEL ladder — NON-standard (T1 = 1), the CENTRIFUGE_PARALLEL shape ({1, 2, 4, 8}). */
	public static final int[] ROASTING_PARALLEL = {1, 2, 4, 8};

	/** The four Roasting rows, upstream line order :1386-1389 (T1-T4, the Heat_T ladder). */
	public static final java.util.List<GTBasicMachineBlock.MachineRow> ROASTING_ROWS = java.util.List.of(
			roastingOven("roasting_oven"   , "steel"           , "Steel"           , 20171,  6.0F, 0),
			roastingOven("roasting_oven_t2", "invar"           , "Invar"           , 20172,  4.0F, 1),
			roastingOven("roasting_oven_t3", "titanium"        , "Titanium"        , 20173,  9.0F, 2),
			roastingOven("roasting_oven_t4", "tungsten_carbide", "Tungsten Carbide", 20174, 12.5F, 3));

	/** One Roasting row factory — the :1386 masks and the ROASTING_PARALLEL duration-F ladder over RM.ROASTING and the "roaster" texture. */
	private static GTBasicMachineBlock.MachineRow roastingOven(String aPath, String aMatSlug, String aMatDisplay, int aMetaId, float aHardness, int aTier) {
		return new GTBasicMachineBlock.MachineRow(aPath, aMatSlug, aMatDisplay, HEAT_T_LADDER.get(aTier), MACHINE_ROASTING_OVEN_UNIT_KEY, aMetaId, aHardness, aTier,
				ROASTING_PARALLEL[aTier], false /*NO NBT_PARALLEL_DURATION key :1386-1389*/,
				() -> GT6RecipeMaps.ROASTING, TD.Energy.HU, "roaster",
				(byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A) /*NBT_ENERGY_ACCEPTED_SIDES SBIT_D*/,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_IN SBIT_B|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A) /*NBT_TANK_SIDE_OUT SBIT_U*/,
				(byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_IN SBIT_B|SBIT_L*/,
				(byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A) /*NBT_INV_SIDE_OUT SBIT_R*/,
				(byte)5 /*NBT_TANK_SIDE_AUTO_IN SIDE_BACK*/, (byte)1 /*NBT_TANK_SIDE_AUTO_OUT SIDE_TOP*/,
				(byte)2 /*NBT_INV_SIDE_AUTO_IN SIDE_LEFT*/, (byte)4 /*NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT*/,
				null /*the menu-less carrier — zero new gt6:* MenuType*/, true /*NBT_CHEAP_OVERCLOCKING :1386*/, null /*no melting gate*/, false);
	}

	/** The registered Roasting blocks by path. */
	public static final java.util.Map<String, RegistryObject<Block>> ROASTING_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered Roasting items, same keys. */
	public static final java.util.Map<String, RegistryObject<Item>> ROASTING_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (GTBasicMachineBlock.MachineRow tRow : ROASTING_ROWS) {
			ROASTING_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBasicMachineBlock(tRow.properties(), () -> GTMachines.ROASTING_BE.get(), tRow)));
			ROASTING_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new gregtech6.block.GTComposedNameItem(GTMachines.ROASTING_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The Roasting block list in registration order (the loot/datagen walkers). */
	public static Block[] roastingBlockArray() {
		return blockArrayOf(ROASTING_BLOCKS_BY_PATH);
	}

	/** The lookup for /gt6machine roasting_oven — null for an unknown path. */
	@javax.annotation.Nullable
	public static Block roastingBlockByPath(String aPath) {
		RegistryObject<Block> tHandle = ROASTING_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The Roasting family BET: the smelter shape verbatim — the shared kineticMachine factory, the four tier blocks multi-attached. */
	public static final RegistryObject<BlockEntityType<TileEntityBasicMachine>> ROASTING_BE =
			BLOCK_ENTITY_TYPES.register("roasting_oven", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> kineticMachine(GTMachines.ROASTING_BE.get(), aPos, aState),
					roastingBlockArray()).build(null));

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
