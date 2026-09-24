package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.tileentity.energy.GT6ElectricTransformerBlockEntity;

/**
 * The Electric Transformer registration (task p28-c-ulv-lv-transformer) — the
 * card-owned {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the
 * construct event, the GT6ElectricDynamos shape (ADR-P3-4). ONE row this card: the
 * ULV-LV pair, Loader_MultiTileEntities.java:881 VERBATIM — display "Transformer
 * (ULV-LV)", meta 10040 (the parity column), {@code NBT_INPUT, V[1], NBT_OUTPUT,
 * V[0], NBT_MULTIPLIER, V[1]/V[0], NBT_WASTE_ENERGY, F, EU/EU}. The higher pairs
 * (:882-889 LV-MV .. UV-UVm) stay OUT (the card SPEC: 其他档对不做 — the ladder rows
 * land when their tiers do; the row table below is the extension seat).
 *
 * <p>The CONDITIONAL-ENTRY ruling (decisions.p28-ulv-tier-rulings transformer_ruling):
 * the transformer enters the game ONLY gated by its LV-material recipe (the
 * {@code GT6CraftingRecipes#transformerBuilder} material lock — the galvanized-steel
 * casing + the LV-era copper wires + the iron double plates, deviating from the :881
 * TinAlloy casing = Electric_T[0] the lowest-price housing). The bridge is an
 * in-era convenience part, not a wall ladder: whoever can CRAFT it already commands
 * LV power. Zero behavior deviation — the :881 row's packet math (×4/÷4) ports
 * verbatim (the BE class doc).
 *
 * <p>The display-name face rides the datagen lang face (the atomic key
 * {@code block.gt6.electric_transformer}); the creative tab join landed in task
 * p38-tabfix-b-energy ({@link #onBuildTabContents} — the W2 deferral discharged).
 * KJS surface: none (the registration face is deferred — the KJS binding pool
 * declaration); recipe = the datapack domain (the crafting JSON), behavior = no
 * KubeJS face.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ElectricTransformers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One transformer row — the upstream-parity columns of one Loader :881-889 aRegistry.add line. */
	public record TransformerRow(String path, int metaId, int tier, String voltagePair, String wireToken) {}

	/**
	 * The nine rows, the full upstream declared ladder (task p35-energy-tail-machines —
	 * the :881-:889 line order, meta ids 10040-10048 as the parity column): every row
	 * NBT_INPUT V[i+1] / NBT_OUTPUT V[i] / NBT_MULTIPLIER 4 / WASTE F, wires Cu on the
	 * :881-:883 rows and AnnealedCopper on the :884-:889 rows (the upstream wireGt01/04
	 * dat(MT.AnnealedCopper) switch at :884). The tier is the row's ladder index i (the
	 * V-pair seat; the path suffix _t(i+1) for i≥1, the bridgePath convention).
	 */
	public static final java.util.List<TransformerRow> ROWS = java.util.List.of(
			new TransformerRow("electric_transformer"   , 10040, 0, "ULV-LV", "copper"),
			new TransformerRow("electric_transformer_t2", 10041, 1, "LV-MV" , "copper"),
			new TransformerRow("electric_transformer_t3", 10042, 2, "MV-HV" , "copper"),
			new TransformerRow("electric_transformer_t4", 10043, 3, "HV-EV" , "annealed_copper"),
			new TransformerRow("electric_transformer_t5", 10044, 4, "EV-IV" , "annealed_copper"),
			new TransformerRow("electric_transformer_t6", 10045, 5, "IV-LuV", "annealed_copper"),
			new TransformerRow("electric_transformer_t7", 10046, 6, "LuV-ZPM", "annealed_copper"),
			new TransformerRow("electric_transformer_t8", 10047, 7, "ZPM-UV", "annealed_copper"),
			new TransformerRow("electric_transformer_t9", 10048, 8, "UV-PUV1", "annealed_copper"));

	// ---------------------------------------------------------------------------
	// the recipe MATERIAL LOCK (decisions.p28-ulv-tier-rulings transformer_ruling — the
	// conditional entry: the carriers are the LV-era materials; the offline pin is
	// GT6ElectricTransformerBlockEntityTest#recipeMaterialLockIsLvEra. Row 0 only — the
	// :882-:889 rows craft over their VERBATIM Electric_T[i] casings, the CASING_LADDER
	// column, where the lock's SteelGalvanized IS the upstream material from tier 1 up.)
	// ---------------------------------------------------------------------------

	/**
	 * The casing lock = Electric_T[1] galvanized steel — the DECLARED DEVIATION from the
	 * :881 row's {@code aMat = MT.DATA.Electric_T[0]} (= TinAlloy, MT.java:3691, the
	 * lowest-price housing): whoever can craft this already commands LV power, so the
	 * bridge is an in-era convenience part, not a wall ladder. The prefix folds
	 * casingMachine → casingSmall (no casingMachine item row in the port — the static
	 * storage 'M' fold precedent).
	 */
	public static final java.util.function.Supplier<gregapi.oredict.OreDictMaterial> CASING_LOCK_MATERIAL = () -> gregapi.data.MT.SteelGalvanized;
	/** Lazy like every MT/OP field read (the GTWireSpecs:35 ruling — this class loads before {@code initMaterials()}). */
	public static final java.util.function.Supplier<gregapi.oredict.OreDictPrefix> CASING_LOCK_PREFIX = () -> gregapi.data.OP.casingSmall;

	/** The wire column fold: wireGt01/wireGt04 (ANY.Cu) → the copper fine-wire tag (no 1x/4x wire item rows in the port; the count differential folds into the cells). Row 0 carrier; the higher rows compose "fine_wires/" + row.wireToken(). */
	public static final String WIRE_TAG_PATH = "fine_wires/copper";

	/** The plate column, the :881 'I' key verbatim: plateDouble(ANY.Iron) → the iron double-plate tag. EVERY row. */
	public static final String PLATE_TAG_PATH = "double_plates/iron";

	/** The crafting shape — the :881-:889 "WIW","XMx","WIW" with the upstream unbound 'm' dead cell folded to a space (CR has no 'm' tool letter). EVERY row. */
	public static final String[] RECIPE_PATTERN = {"WIW", "XM ", "WIW"};

	/**
	 * The Electric_T[0..8] casing ladder of the :881-:889 rows (upstream MT.java:3691
	 * members [0..8] = TinAlloy / SteelGalvanized / Al / StainlessSteel / Cr / Ti / Ir /
	 * Os / Trinitanium) — the row's aMat column, lazy suppliers (the GTWireSpecs:35
	 * ruling). The dynamos carry their own [0..5] copy (GT6ElectricDynamos
	 * ELECTRIC_T_LADDER — the per-family local copy precedent); the [6..8] members are
	 * this card's extension.
	 */
	public static final java.util.List<java.util.function.Supplier<gregapi.oredict.OreDictMaterial>> CASING_LADDER = java.util.List.of(
			() -> gregapi.data.MT.TinAlloy, () -> gregapi.data.MT.SteelGalvanized, () -> gregapi.data.MT.Al,
			() -> gregapi.data.MT.StainlessSteel, () -> gregapi.data.MT.Cr, () -> gregapi.data.MT.Ti,
			() -> gregapi.data.MT.Ir, () -> gregapi.data.MT.Os, () -> gregapi.data.MT.Trinitanium);

	// the block/item registrations — one per row, the GT6Batteries map form (static-init
	// walk over ROWS; the BET supplier reads the map at registry time)
	/** The row blocks by registry path (the datagen/loot walk seat). */
	public static final java.util.Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The row items by registry path (the recipe result seat). */
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (TransformerRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(), () -> transformer(tRow.tier())));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new BlockItem(
					BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	// the row-0 anchors (the p28 consumers — the recipe host, the blockstates/loot walks,
	// the tests — keep compiling against the named constants; the row-0 pair of the maps)
	/** The :881 row block (the BLOCKS_BY_PATH row-0 entry). */
	public static final RegistryObject<Block> ELECTRIC_TRANSFORMER = BLOCKS_BY_PATH.get("electric_transformer");
	/** The :881 row item (the ITEMS_BY_PATH row-0 entry). */
	public static final RegistryObject<Item> ELECTRIC_TRANSFORMER_ITEM = ITEMS_BY_PATH.get("electric_transformer");

	/** The row block: hardness/resistance 4.0/4.0 (the NBT_HARDNESS/RESISTANCE columns), metal sounds, the family BET supplier. */
	private static GT6ElectricTransformerBlock transformer(int aTier) {
		return new GT6ElectricTransformerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(4.0F, 4.0F).sound(SoundType.METAL), () -> ELECTRIC_TRANSFORMER_BE.get(), aTier);
	}

	/**
	 * The BET — the one BE class over the nine ladder blocks (the p8 family-BET
	 * Builder.of shape; the BE resolves its V-pair off the block's tier column).
	 * Registers AFTER the BLOCKS (vanilla registry order).
	 */
	public static final RegistryObject<BlockEntityType<GT6ElectricTransformerBlockEntity>> ELECTRIC_TRANSFORMER_BE =
			BLOCK_ENTITY_TYPES.register("electric_transformer", () -> BlockEntityType.Builder.of(
					GT6ElectricTransformerBlockEntity::new,
					ROWS.stream().map(aRow -> BLOCKS_BY_PATH.get(aRow.path()).get()).toArray(Block[]::new)).build(null));

	/** The row item of a ladder tier (the upstream getItem(10040+tier) face — the large-BatteryBox 'M' column and the recipe hosts). */
	public static Item itemOfTier(int aTier) {
		return ITEMS_BY_PATH.get(ROWS.get(aTier).path()).get();
	}

	private GT6ElectricTransformers() {}

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

	/** Registration smoke evidence (the GT6ElectricDynamos.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 electric transformer registered: " + ROWS.size()
					+ " rows ULV-LV..UV-UVm, x4/div4 EU packet math per row (ids 10040-10048, the p28+p35 transformer cards)");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
	 * joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream gives the family its own "Transformers" category
	 * (tab 10041, Loader_MultiTileEntities.java:881-889); this port pools the join into
	 * MACHINES_TAB (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
