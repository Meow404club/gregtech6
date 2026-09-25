package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.multiblock.GTTankValveBlock;
import gregtech6.tileentity.multiblocks.GTTankValveBlockEntity;

/**
 * The Tank Main Valve family registration home — task p29-w3-tank-valves, the ADR-P3-4
 * self-contained listener form (GT6BurningBoxes/GT6Crucibles shape): block + item + BET
 * DeferredRegisters attached from the construct event, {@code GTMultiBlocks.java} untouched.
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (25 valve Blocks/BlockEntities/Items) and the STRUCTURE face (the
 * hollow 3x3x3 / 5x5x5 ONLY_FLUID wall semantics); the valve crafting is plain vanilla
 * shaped-recipe JSON (tier-a, naturally editable), and there is NO datapack row data and
 * NO KubeJS-specific seam in this family.
 *
 * <p><b>The rows</b> (Loader_MultiTileEntities.java:1195-1222 re-read VERBATIM, the
 * registration order — NOT id-ordered: 17002/17007/17006/17003/17004/17005): one wood
 * 3x3x3 (432000 L, the wood wall 18001, onlySimple, flammable 150, all four proof flags
 * false) + six plain small 3x3x3 + six dense small 3x3x3 (capacity x4) + six plain large
 * 5x5x5 (8M..512M) + six dense large 5x5x5 (32M..2048M) = 25 valves (the census "26"
 * correction, decisions.p29-w3-split-rulings). Every upstream registration-NBT column
 * maps onto the row: NBT_TANK_CAPACITY → {@link TankValveRow#capacity()}, the four
 * NBT_*PROOF flags, NBT_HARDNESS == NBT_RESISTANCE, NBT_MATERIAL → the deferred material
 * supplier, and NBT_DESIGN (the SINGULAR form — the controller row's wall choice, the
 * decisions.p29-w3-split-rulings design-seam ruling) → {@link TankValveRow#wallPath()}
 * as the part BLOCK identity (the checkAndSetTarget design argument stays 0 verbatim,
 * upstream Tank3x3x3.java:69).
 *
 * <p><b>The material supplier</b> (the GT6Crucibles.SmelteryRow / GTWireSpecs.Row form):
 * the static ROWS initialize at class-load time, which runs before MT.init() assigns the
 * OreDictMaterial statics (the P2 two-phase reset) — a direct MT.StainlessSteel reference
 * would capture null. The BE resolves the supplier at RUNTIME (tick time, melting point
 * reads) and guards the null generation with the class-default melting point.
 *
	 * <p><b>The creative tab</b> (task p38-tabfix-a-multiblock, superseding the "NOT joined
	 * this card" ruling): all 25 valve items join MULTIBLOCKS_TAB via
	 * {@link #onBuildTabContents} — registered-but-tab-less is invisible in BOTH the creative
	 * menu and JEI (the BurningBoxes issue-#10 form). Pool cut declared: upstream rode the
	 * per-family "Multiblock Machines" creative tab (tab id 17101, Loader :1195-1222); this
	 * port pools the family into the gt6:multiblocks tab.
	 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Tanks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/**
	 * One Tank Main Valve row — the Loader :1195-1222 line columns. {@code material} is the
	 * deferred MT reference (the class-load-order lesson above); {@code size} is 3 (the
	 * 3x3x3 hollow) or 5 (the 5x5x5 hollow); {@code wallPath} is the NBT_DESIGN wall as the
	 * part-block registry path; {@code flammable} carries NBT_FLAMMABILITY 150 (the wood
	 * valve only — the WoodWallPartBlock fire-face trio); {@code onlySimple} is the
	 * upstream {@code onlySimple()} gate (MultiTileEntityTank.java:105 — the wood valve
	 * accepts SIMPLE fluids only).
	 */
	public record TankValveRow(String path, int metaId, Supplier<OreDictMaterial> material, int size, String wallPath,
			long capacity, boolean gasProof, boolean acidProof, boolean plasmaProof, boolean magicProof,
			boolean flammable, boolean onlySimple, float hardness) {}

	/** The composed standard display template "{@code <size> <mat> Tank Main Valve}" — two slots. */
	public static final String TANK_VALVE_DISPLAY_KEY = "gt6.row.tank_valve.display";
	/** The composed wood display template "{@code <wood> Tank Main Valve}" — one slot (the upstream :1195 name column shape). */
	public static final String TANK_VALVE_WOOD_DISPLAY_KEY = "gt6.row.tank_valve.wood.display";
	/** The wood material small-unit key (the size slot of the wood template — no wall-row word exists for it). */
	public static final String WOOD_UNIT_KEY = "gt6.row.mat.wood";
	/** The size-word unit keys (the first slot of the standard template). */
	public static final String SIZE_SMALL = "gt6.row.tank_size.small";
	public static final String SIZE_SMALL_DENSE = "gt6.row.tank_size.small_dense";
	public static final String SIZE_LARGE = "gt6.row.tank_size.large";
	public static final String SIZE_LARGE_DENSE = "gt6.row.tank_size.large_dense";

	/** The 25 valve rows (:1195-1222, the registration order). */
	public static final List<TankValveRow> ROWS = List.of(
			// wood — :1195 (432000 L, the wood wall, onlySimple, flammable 150, no proofs)
			row("tank_wood", 17001, () -> gregapi.data.MT.WoodTreated, 3, "wood_wall",
					432000, false, false, false, false, true, true, 5.0F),
			// the plain small 3x3x3 six — :1196-1201
			row("tank_small_stainless_steel", 17002, () -> gregapi.data.MT.StainlessSteel, 3, "machine_wall_stainless_steel",
					1728000, true, true, false, false, false, false, 6.0F),
			row("tank_small_invar", 17007, () -> gregapi.data.MT.Invar, 3, "machine_wall_invar",
					1728000, true, false, false, false, false, false, 6.0F),
			row("tank_small_titanium", 17006, () -> gregapi.data.MT.Ti, 3, "machine_wall_titanium",
					3456000, true, false, false, false, false, false, 9.0F),
			row("tank_small_tungstensteel", 17003, () -> gregapi.data.MT.TungstenSteel, 3, "machine_wall_tungstensteel",
					6912000, true, false, false, true, false, false, 12.5F),
			row("tank_small_tungsten", 17004, () -> gregapi.data.MT.W, 3, "machine_wall_tungsten",
					6912000, true, true, false, true, false, false, 10.0F),
			row("tank_small_adamantium", 17005, () -> gregapi.data.MT.Ad, 3, "machine_wall_adamantium",
					110592000, true, true, true, true, false, false, 100.0F),
			// the dense small 3x3x3 six — :1203-1208 (capacity x4 of the plain rungs)
			row("tank_small_dense_stainless_steel", 17022, () -> gregapi.data.MT.StainlessSteel, 3, "dense_wall_stainless_steel",
					6912000, true, true, false, false, false, false, 6.0F),
			row("tank_small_dense_invar", 17027, () -> gregapi.data.MT.Invar, 3, "dense_wall_invar",
					6912000, true, false, false, false, false, false, 6.0F),
			row("tank_small_dense_titanium", 17026, () -> gregapi.data.MT.Ti, 3, "dense_wall_titanium",
					13824000, true, false, false, false, false, false, 9.0F),
			row("tank_small_dense_tungstensteel", 17023, () -> gregapi.data.MT.TungstenSteel, 3, "dense_wall_tungstensteel",
					27648000, true, false, false, true, false, false, 12.5F),
			row("tank_small_dense_tungsten", 17024, () -> gregapi.data.MT.W, 3, "dense_wall_tungsten",
					27648000, true, true, false, true, false, false, 10.0F),
			row("tank_small_dense_adamantium", 17025, () -> gregapi.data.MT.Ad, 3, "dense_wall_adamantium",
					442368000, true, true, true, true, false, false, 100.0F),
			// the plain large 5x5x5 six — :1210-1215
			row("tank_large_stainless_steel", 17042, () -> gregapi.data.MT.StainlessSteel, 5, "machine_wall_stainless_steel",
					8000000, true, true, false, false, false, false, 6.0F),
			row("tank_large_invar", 17047, () -> gregapi.data.MT.Invar, 5, "machine_wall_invar",
					8000000, true, false, false, false, false, false, 6.0F),
			row("tank_large_titanium", 17046, () -> gregapi.data.MT.Ti, 5, "machine_wall_titanium",
					16000000, true, false, false, false, false, false, 9.0F),
			row("tank_large_tungstensteel", 17043, () -> gregapi.data.MT.TungstenSteel, 5, "machine_wall_tungstensteel",
					32000000, true, false, false, true, false, false, 12.5F),
			row("tank_large_tungsten", 17044, () -> gregapi.data.MT.W, 5, "machine_wall_tungsten",
					32000000, true, true, false, true, false, false, 10.0F),
			row("tank_large_adamantium", 17045, () -> gregapi.data.MT.Ad, 5, "machine_wall_adamantium",
					512000000, true, true, true, true, false, false, 100.0F),
			// the dense large 5x5x5 six — :1217-1222 (capacity x4 of the plain larges)
			row("tank_large_dense_stainless_steel", 17062, () -> gregapi.data.MT.StainlessSteel, 5, "dense_wall_stainless_steel",
					32000000, true, true, false, false, false, false, 6.0F),
			row("tank_large_dense_invar", 17067, () -> gregapi.data.MT.Invar, 5, "dense_wall_invar",
					32000000, true, false, false, false, false, false, 6.0F),
			row("tank_large_dense_titanium", 17066, () -> gregapi.data.MT.Ti, 5, "dense_wall_titanium",
					64000000, true, false, false, false, false, false, 9.0F),
			row("tank_large_dense_tungstensteel", 17063, () -> gregapi.data.MT.TungstenSteel, 5, "dense_wall_tungstensteel",
					128000000, true, false, false, true, false, false, 12.5F),
			row("tank_large_dense_tungsten", 17064, () -> gregapi.data.MT.W, 5, "dense_wall_tungsten",
					128000000, true, true, false, true, false, false, 10.0F),
			row("tank_large_dense_adamantium", 17065, () -> gregapi.data.MT.Ad, 5, "dense_wall_adamantium",
					2048000000, true, true, true, true, false, false, 100.0F));

	/** One row builder — the path is verbatim, the columns are the Loader line's. */
	private static TankValveRow row(String aPath, int aMetaId, Supplier<OreDictMaterial> aMaterial, int aSize, String aWallPath,
			long aCapacity, boolean aGas, boolean aAcid, boolean aPlasma, boolean aMagic,
			boolean aFlammable, boolean aOnlySimple, float aHardness) {
		return new TankValveRow(aPath, aMetaId, aMaterial, aSize, aWallPath, aCapacity, aGas, aAcid, aPlasma, aMagic,
				aFlammable, aOnlySimple, aHardness);
	}

	/** The registered valve blocks by path (the BET valid list + the datagen walk + the command lookup). */
	public static final Map<String, RegistryObject<GTTankValveBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered valve items, same keys (the crafting results + the creative-less reach). */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		// the 25 valve blocks + items (the forward-reference lambda form — the BET builder
		// resolves these handles at REGISTER time, after every static field is initialized,
		// the WALL_ROWS lesson comment)
		for (TankValveRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTTankValveBlock(tRow, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), tRow.hardness())
							.sound(tRow.flammable() ? SoundType.WOOD : SoundType.METAL))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The Tank Valve BET: one controller class over the 25 variant blocks (the one-BET-many-
	 * blocks GT6Boilers form; the variant config rides the block carrier row). Registry path
	 * mirrors {@link GTTankValveBlockEntity#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<GTTankValveBlockEntity>> TANK_VALVE_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_tank_valve", () -> BlockEntityType.Builder.of(
					GTTankValveBlockEntity::new, valveBlockArray()).build(null));

	/** The valve-variant array for the controller BET (the boilerBlockArray shape). */
	private static Block[] valveBlockArray() {
		return GT6Tanks.BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	/**
	 * The tank-valve paint-tint walker (task issue8-residual, the
	 * {@code GT6Kitchen.paintableBlockArray} census convention): the 25 valve controllers
	 * whose datagen models carry tintindex 0 on every face (the borrowed grayscale
	 * woodwall/metalwall colored textures multiply the row's NBT_MATERIAL — the upstream
	 * {@code getTexture2} colored×mRGBa form). Feeding BOTH consumption halves — the baked
	 * world tint ({@code GTMachineTintModel}, the p32 route) and the inventory
	 * {@code ItemColor} — the row materials resolve through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} controller gate. Client call time only.
	 */
	public static Block[] paintableBlockArray() {
		return valveBlockArray();
	}

	/** The row lookup by block — null for a foreign block (the offline fixtures). */
	@Nullable
	public static TankValveRow rowOf(Block aBlock) {
		if (aBlock instanceof GTTankValveBlock tValve) return tValve.row();
		return null;
	}

	/** The valve block by registry path — null when unknown (the command lookup form). */
	@Nullable
	public static Block valveBlockByPath(String aPath) {
		RegistryObject<GTTankValveBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The composed display of a row (the {@code largeBoilerDisplayOf} carrier shape): the
	 * wood valve takes the ONE-slot template over the wood unit word (the :1195 "Wood Tank
	 * Main Valve" name column), the 24 metal valves the TWO-slot size+material template
	 * over the EXISTING gt6.row.mat.* wall-row words (all six metal materials already have
	 * unit keys — zero new material words).
	 */
	public static MutableComponent displayOf(TankValveRow aRow) {
		if (aRow.flammable()) { // the wood valve is the only flammable row (NBT_FLAMMABILITY 150, :1195)
			return Component.translatable(TANK_VALVE_WOOD_DISPLAY_KEY, Component.translatable(WOOD_UNIT_KEY));
		}
		return Component.translatable(TANK_VALVE_DISPLAY_KEY,
				Component.translatable(sizeUnitKeyOf(aRow)),
				Component.translatable(matUnitKeyOf(aRow)));
	}

	/** The size-word unit key: wood (never reached in the standard template), small, small_dense, large, large_dense. */
	public static String sizeUnitKeyOf(TankValveRow aRow) {
		if (aRow.flammable()) return WOOD_UNIT_KEY;
		if (aRow.size() == 5) return aRow.wallPath().startsWith("dense_wall_") ? SIZE_LARGE_DENSE : SIZE_LARGE;
		return aRow.wallPath().startsWith("dense_wall_") ? SIZE_SMALL_DENSE : SIZE_SMALL;
	}

	/** The material small-unit key — the SAME words the dense-wall rows registered (the path tail after the size prefix). */
	public static String matUnitKeyOf(TankValveRow aRow) {
		String tPath = aRow.path();
		if (tPath.startsWith("tank_small_dense_")) return "gt6.row.mat." + tPath.substring("tank_small_dense_".length());
		if (tPath.startsWith("tank_large_dense_")) return "gt6.row.mat." + tPath.substring("tank_large_dense_".length());
		if (tPath.startsWith("tank_small_")) return "gt6.row.mat." + tPath.substring("tank_small_".length());
		if (tPath.startsWith("tank_large_")) return "gt6.row.mat." + tPath.substring("tank_large_".length());
		return WOOD_UNIT_KEY;
	}

	/** Every row in registration order as a list (the census/datagen walk convenience). */
	public static List<TankValveRow> allRows() {
		return new ArrayList<>(ROWS);
	}

	private GT6Tanks() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
	 * (the GTMultiBlocks onModConstruct form).
	 */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: the GTMultiBlocks fork precedent (the annotation rework killed the bus enum).
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/**
	 * The tab walk (task p38-tabfix-a-multiblock — the whole {@link #ITEMS_BY_PATH} valve
	 * family joins the multiblocks tab; the GT6BurningBoxes.onBuildTabContents verbatim
	 * form, the class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is
	 * what delivers this handler). JEI derives its item list from the tab display items.
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMultiBlocks.MULTIBLOCKS_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
