package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
 * <p>The display-name face and the creative tab join are DECLARED DEFERRED to the W2
 * render card (the GT6ElectricDynamos posture; the lang atomic key
 * {@code block.gt6.electric_transformer} is composed in the datagen lang face).
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
	public record TransformerRow(String path, int metaId, String voltagePair) {}

	/** The row, upstream :881 (the ULV-LV pair; the metaId rides as the parity column, the dynamo form). */
	public static final java.util.List<TransformerRow> ROWS = java.util.List.of(
			new TransformerRow("electric_transformer", 10040, "ULV-LV"));

	// ---------------------------------------------------------------------------
	// the recipe MATERIAL LOCK (decisions.p28-ulv-tier-rulings transformer_ruling — the
	// conditional entry: the carriers are the LV-era materials; the offline pin is
	// GT6ElectricTransformerBlockEntityTest#recipeMaterialLockIsLvEra)
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

	/** The wire column fold: wireGt01/wireGt04 (ANY.Cu) → the copper fine-wire tag (no 1x/4x wire item rows in the port; the count differential folds into the cells). */
	public static final String WIRE_TAG_PATH = "fine_wires/copper";

	/** The plate column, the :881 'I' key verbatim: plateDouble(ANY.Iron) → the iron double-plate tag. */
	public static final String PLATE_TAG_PATH = "double_plates/iron";

	/** The crafting shape — the :881 "WIW","XMx","WIW" with the upstream unbound 'm' dead cell folded to a space (CR has no 'm' tool letter). */
	public static final String[] RECIPE_PATTERN = {"WIW", "XM ", "WIW"};

	public static final RegistryObject<Block> ELECTRIC_TRANSFORMER = BLOCKS.register("electric_transformer",
			() -> transformer());

	/** The row block: hardness/resistance 4.0/4.0 (the NBT_HARDNESS/RESISTANCE columns), metal sounds, the family BET supplier. */
	private static GT6ElectricTransformerBlock transformer() {
		return new GT6ElectricTransformerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(4.0F, 4.0F).sound(SoundType.METAL), () -> ELECTRIC_TRANSFORMER_BE.get());
	}

	/** The tier item — a plain BlockItem, stack 16 (the upstream stack column; the name face is the datagen lang). */
	public static final RegistryObject<Item> ELECTRIC_TRANSFORMER_ITEM = ITEMS.register("electric_transformer",
			() -> new BlockItem(ELECTRIC_TRANSFORMER.get(), new Item.Properties().stacksTo(16)));

	/**
	 * The BET — the one BE class over the one ladder block (the p8 family-BET Builder.of
	 * shape). Registers AFTER the BLOCKS (vanilla registry order).
	 */
	public static final RegistryObject<BlockEntityType<GT6ElectricTransformerBlockEntity>> ELECTRIC_TRANSFORMER_BE =
			BLOCK_ENTITY_TYPES.register("electric_transformer", () -> BlockEntityType.Builder.of(
					GT6ElectricTransformerBlockEntity::new,
					ELECTRIC_TRANSFORMER.get()).build(null));

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
			gregtech6.GT6Mod.LOGGER.info("GT6 electric transformer registered: 1 row ULV-LV, x4/div4 EU packet math (id 10040, the p28 transformer card)");
		});
	}
}
