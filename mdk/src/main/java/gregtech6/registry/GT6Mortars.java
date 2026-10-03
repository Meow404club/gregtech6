package gregtech6.registry;

import java.util.List;
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

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.block.tools.GT6MortarBlock;
import gregtech6.tileentity.tools.GT6MortarBlockEntity;

/**
 * The mortar family registration (task mortar-family) — the
 * {@code @EventBusSubscriber(MOD)} DeferredRegister three-piece, the GT6Anvils shape
 * (a separate class keeps the card scopes disjoint). Ports the FIVE "Misc Tool Blocks"
 * mortar rows of Loader_MultiTileEntities.java:2179-2183 (IDs 32735/32094/32075/32076/
 * 32089): every row is {@code NBT_MATERIAL, MT.Ceramic} (the Ceramic bowl body — the
 * hardness 1.0 / resistance 5.0 / aUtilStone column folds onto the block properties, the
 * GT6Kitchen.kitchenProperties seam form) and differs ONLY in {@code NBT_DESIGN} 0-4 —
 * the pestle material of {@code MORTAR_MATERIALS} (MultiTileEntityMortar.java:59):
 * {@code ANY.Steel → MT.Netherite → ANY.Sapphire → ANY.Diamond → ANY.Amethyst} (the
 * port representative of each group is the row carrier's material supplier, the
 * single-tier family ruling).
 *
 * <p>GUI: none — the upstream tooltip is {@code LH.NO_GUI_CLICK_TO_INTERACT} +
 * {@code LH.FACE_TOP} (MultiTileEntityMortar.java:72): the top-face click with the held
 * item IS the working surface, the BE's {@link GT6MortarBlockEntity#activateChain}.
 * Creative tab: the {@link GTMachines#MACHINES_TAB} join (the GT6Anvils form).
 *
 * <p>KJS: the registration face (blocks/items/BE) is the declared defer — an addon
 * binding rides the kjs-bindings card (the GT6Cups family form). The recipe face is the
 * datapack domain ({@code data/gt6/recipe_maps/mortar.json} ships the row stock; the
 * crafting rows ride the standard datagen) — naturally modifiable, zero adaptation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Mortars {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The bowl body material — every upstream row is {@code NBT_MATERIAL, MT.Ceramic} (Loader :2179-2183). */
	public static final Supplier<OreDictMaterial> BODY_MATERIAL = () -> MT.Ceramic;

	/**
	 * One registration row — the block-carrier projection of the Loader mortar line (the
	 * GT6Anvils.AnvilRow form). The list order IS the upstream ID order (:2179-2183), and
	 * the index IS the upstream {@code NBT_DESIGN} value.
	 *
	 * @param path   the gt6 registry path (the blockstate/model/lang key tail)
	 * @param design the upstream {@code NBT_DESIGN} (0-4)
	 * @param pestle the row of {@code MORTAR_MATERIALS} (MultiTileEntityMortar.java:59) the
	 *               design selects — the pestle tint seat and the crafting ingredient
	 */
	public record MortarRow(String path, int design, Supplier<OreDictMaterial> pestle) {}

	/** The five rows in registration order (Loader :2179-2183; the pestle groups ride their port representatives). */
	public static final List<MortarRow> ROWS = List.of(
			new MortarRow("mortar_steel", 0, () -> MT.Steel), // :2179 — ID 32735, ANY.Steel, OP.ingot.dat(ANY.Iron)
			new MortarRow("mortar_netherite", 1, () -> MT.Netherite), // :2180 — ID 32094, OP.ingot.dat(MT.Netherite)
			new MortarRow("mortar_sapphire", 2, () -> MT.Sapphire), // :2181 — ID 32075, OP.gem.dat(ANY.Sapphire)
			new MortarRow("mortar_diamond", 3, () -> MT.Diamond), // :2182 — ID 32076, OP.gem.dat(ANY.Diamond)
			new MortarRow("mortar_amethyst", 4, () -> MT.Amethyst)); // :2183 — ID 32089, OP.gem.dat(ANY.Amethyst)

	/** The registered blocks by path (the datagen walkers + the BET multi-mount array). */
	public static final java.util.Map<String, RegistryObject<GT6MortarBlock>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (MortarRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6MortarBlock(tRow.pestle(), () -> GT6Mortars.MORTAR_BE.get(), mortarProperties())));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GT6Mortars.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The shared mortar-family properties seam (the GT6Kitchen.kitchenProperties per-file
	 * seam form): the upstream column is {@code NBT_HARDNESS 1.0 / NBT_RESISTANCE 5.0 /
	 * aUtilStone} on every row (Loader :2179-2183) and the sub-cube bowl model over a true
	 * {@code canOcclude} would cull neighbour faces against the cavity (the #9 X-ray) —
	 * so the chain carries {@code .noOcclusion().isViewBlocking(never)} exactly like the
	 * kitchen seam.
	 */
	static BlockBehaviour.Properties mortarProperties() {
		return BlockBehaviour.Properties.of()
				.strength(1.0F, 5.0F).sound(SoundType.STONE)
				.noOcclusion().isViewBlocking(GT6Mortars::never);
	}

	/** The fog-only rider (the GT6Kitchen seam body): the sub-cube bowl never blocks the view. */
	private static boolean never(net.minecraft.world.level.block.state.BlockState aState,
			net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos) {
		return false;
	}

	/**
	 * The mortar BET — the shared-BET multi-mount (ADR-P3-1): one BE class over all five
	 * rows (the rows differ ONLY in the registration design the carrier now holds).
	 */
	public static final RegistryObject<BlockEntityType<GT6MortarBlockEntity>> MORTAR_BE =
			BLOCK_ENTITY_TYPES.register("mortar", () -> BlockEntityType.Builder.of(
					GT6MortarBlockEntity::new, blocks()).build(null));

	/** The five registered blocks in row order (the BET multi-mount varargs). */
	private static GT6MortarBlock[] blocks() {
		return BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(GT6MortarBlock[]::new);
	}

	/** The MACHINES-TAB join (the GT6Anvils form — the mortar is a processing block). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (MortarRow tRow : ROWS) {
				aEvent.accept(new ItemStack(ITEMS_BY_PATH.get(tRow.path()).get()));
			}
		}
	}

	private GT6Mortars() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Anvils fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework — the GTBarrels fork form.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Anvils onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 mortar registered: 5 rows (RM.Mortar), head {}",
					ForgeRegistries.BLOCKS.getKey(BLOCKS_BY_PATH.get("mortar_steel").get()));
		});
	}
}
