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
import gregtech6.block.tools.GTAnvilBlock;
import gregtech6.tileentity.tools.GT6AnvilBlockEntity;

/**
 * The anvil family registration (task c-anvil + material-mc-e-tool-anvil-rows) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6Kitchen shape (a separate class keeps the card scopes disjoint). Ports the FULL
 * "Misc Tool Blocks" anvil ladder of Loader_MultiTileEntities.java:2185-2219 (35 rows):
 * <ul>
 * <li>Stone Anvil (:2185) — MT.Stone, NBT_DURABILITY 10000, hardness 1.0 / resistance
 *     6.0, aUtilStone; the recipe "RRR","hR ","RRR" over {@code Blocks.stone} rides the
 *     crafting datagen (GT6CraftingRecipes, the tier-a band).</li>
 * <li>Blackstone Anvil (:2186) — MT.STONES.Blackstone, NBT_DURABILITY 100000, same
 *     shape over {@code OP.stone.dat(Blackstone)} = the vanilla BLACKSTONE item.</li>
 * <li>the 33 material rows (:2187-2219) — task material-mc-e-tool-anvil-rows appended
 *     them verbatim (material + NBT_DURABILITY + the aUtil column): two more aUtilStone
 *     stone-carrier rows (Black/Red Granite), the aUtilMetal ingot-carrier ladder
 *     (Pb 800k → Infinity 1e15), and the single aUtilWood row (Ironwood 7.5M — the axe
 *     band). Every row keeps NBT_HARDNESS 1.0 / NBT_RESISTANCE 6.0.</li>
 * </ul>
 * The c-anvil card's "other 14 rows pool cut" is retired by this table (the BE reads the
 * carrier values, so the ladder was ROWS-only — the appended rows prove the form). The
 * durability ladder IS the gameplay: the Stone anvil breaks after ONE displayed point
 * (the wear floor = one point per working hit, MultiTileEntityAnvil :124-126), the
 * Blackstone one survives ten, the Infinity one a quadrillion.
 *
 * <p>GUI: none — the upstream tooltip is {@code LH.NO_GUI_CLICK_TO_INTERACT}
 * (MultiTileEntityAnvil :91), the menu-less kitchen ruling. Creative tab: the
 * {@link GTMachines#MACHINES_TAB} join (the p27 kitchen-tab retirement form —
 * {@link #onBuildTabContents}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Anvils {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One registration row — the block-carrier projection of the Loader anvil line
	 * (the GT6Hoppers.HopperRow form).
	 *
	 * @param path        the gt6 registry path (the blockstate/model/lang key tail)
	 * @param material    the upstream {@code NBT_MATERIAL} (the smash-target hop reads it)
	 * @param durability  the upstream {@code NBT_DURABILITY} (10000 stone … 1e15 infinity)
	 * @param sound       the aUtil column's sound family (STONE for aUtilStone, METAL for
	 *                    aUtilMetal, WOOD for the aUtilWood Ironwood row)
	 */
	public record AnvilRow(String path, Supplier<OreDictMaterial> material, long durability, SoundType sound) {}

	/**
	 * The 35 rows in registration order (:2185-2219 verbatim). The ANY.* rows resolve to
	 * their port identity material (ANY.Steel → Steel, ANY.BlackSteel → BlackSteel,
	 * ANY.W → Tungsten — the mc-D transformer-gearbox ANY resolution).
	 */
	public static final List<AnvilRow> ROWS = List.of(
			new AnvilRow("stone_anvil", () -> MT.Stone, 10000, SoundType.STONE),
			new AnvilRow("blackstone_anvil", () -> MT.STONES.Blackstone, 100000, SoundType.STONE),
			// :2187-2219 — the material ladder (task material-mc-e-tool-anvil-rows)
			new AnvilRow("granite_black_anvil", () -> MT.STONES.GraniteBlack, 100000, SoundType.STONE),
			new AnvilRow("granite_red_anvil", () -> MT.STONES.GraniteRed, 100000, SoundType.STONE),
			new AnvilRow("lead_anvil", () -> MT.Pb, 800000, SoundType.METAL),
			new AnvilRow("bronze_anvil", () -> MT.Bronze, 1000000, SoundType.METAL),
			new AnvilRow("arsenic_copper_anvil", () -> MT.ArsenicCopper, 1000000, SoundType.METAL),
			new AnvilRow("arsenic_bronze_anvil", () -> MT.ArsenicBronze, 2000000, SoundType.METAL),
			new AnvilRow("syrmorite_anvil", () -> MT.Syrmorite, 2000000, SoundType.METAL),
			new AnvilRow("iron_wood_anvil", () -> MT.IronWood, 7500000, SoundType.WOOD),
			new AnvilRow("steel_anvil", () -> MT.Steel, 10000000, SoundType.METAL),
			new AnvilRow("desh_anvil", () -> MT.Desh, 12500000, SoundType.METAL),
			new AnvilRow("efrine_anvil", () -> MT.Efrine, 20000000, SoundType.METAL),
			new AnvilRow("thaumium_anvil", () -> MT.Thaumium, 25000000, SoundType.METAL),
			new AnvilRow("manasteel_anvil", () -> MT.Manasteel, 25000000, SoundType.METAL),
			new AnvilRow("black_steel_anvil", () -> MT.BlackSteel, 30000000, SoundType.METAL),
			new AnvilRow("blue_steel_anvil", () -> MT.BlueSteel, 40000000, SoundType.METAL),
			new AnvilRow("red_steel_anvil", () -> MT.RedSteel, 50000000, SoundType.METAL),
			new AnvilRow("vanadium_steel_anvil", () -> MT.VanadiumSteel, 70000000, SoundType.METAL),
			new AnvilRow("octine_anvil", () -> MT.Octine, 80000000, SoundType.METAL),
			new AnvilRow("fiery_steel_anvil", () -> MT.FierySteel, 90000000, SoundType.METAL),
			new AnvilRow("tungsten_alloy_anvil", () -> MT.TungstenAlloy, 100000000, SoundType.METAL),
			new AnvilRow("titanium_anvil", () -> MT.Ti, 100000000, SoundType.METAL),
			new AnvilRow("netherite_anvil", () -> MT.Netherite, 150000000, SoundType.METAL),
			new AnvilRow("terrasteel_anvil", () -> MT.Terrasteel, 200000000, SoundType.METAL),
			new AnvilRow("void_metal_anvil", () -> MT.VoidMetal, 300000000, SoundType.METAL),
			new AnvilRow("titanium_gold_anvil", () -> MT.TitaniumGold, 400000000, SoundType.METAL),
			new AnvilRow("tungsten_steel_anvil", () -> MT.TungstenSteel, 1000000000, SoundType.METAL),
			new AnvilRow("tungsten_anvil", () -> MT.Tungsten, 2000000000, SoundType.METAL),
			new AnvilRow("iridium_anvil", () -> MT.Ir, 10000000000L, SoundType.METAL),
			new AnvilRow("gaia_spirit_anvil", () -> MT.GaiaSpirit, 100000000000L, SoundType.METAL),
			new AnvilRow("adamantium_anvil", () -> MT.Ad, 1000000000000L, SoundType.METAL),
			new AnvilRow("draconium_anvil", () -> MT.Draconium, 1000000000000L, SoundType.METAL),
			new AnvilRow("awakened_draconium_anvil", () -> MT.DraconiumAwakened, 2000000000000L, SoundType.METAL),
			new AnvilRow("infinity_anvil", () -> MT.Infinity, 1000000000000000L, SoundType.METAL));

	/** The aUtilWood rows (the axe mineable band; the harvest census :2185-2219 column walk). */
	public static boolean isWoodBand(AnvilRow aRow) {
		return aRow.sound() == SoundType.WOOD;
	}

	/** The registered blocks by path (the datagen walkers + the BET multi-mount array). */
	public static final java.util.Map<String, RegistryObject<GTAnvilBlock>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (AnvilRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTAnvilBlock(tRow.material(), tRow.durability(), () -> GT6Anvils.ANVIL_BE.get(),
							BlockBehaviour.Properties.of().strength(1.0F, 6.0F).sound(tRow.sound()))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GT6Anvils.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The stone anvil handle (the :2185 row). */
	public static final RegistryObject<GTAnvilBlock> STONE_ANVIL = BLOCKS_BY_PATH.get("stone_anvil");
	/** The blackstone anvil handle (the :2186 row). */
	public static final RegistryObject<GTAnvilBlock> BLACKSTONE_ANVIL = BLOCKS_BY_PATH.get("blackstone_anvil");

	/**
	 * The anvil BET — the shared-BET multi-mount (ADR-P3-1): one BE class over all 35 rows
	 * (the rows differ ONLY in the registration NBT the carrier now holds).
	 */
	public static final RegistryObject<BlockEntityType<GT6AnvilBlockEntity>> ANVIL_BE =
			BLOCK_ENTITY_TYPES.register("anvil", () -> BlockEntityType.Builder.of(
					GT6AnvilBlockEntity::new, BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	/** The MACHINES-TAB join (the p27 kitchen retirement form — the anvil is a processing block). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (AnvilRow tRow : ROWS) {
				aEvent.accept(new ItemStack(ITEMS_BY_PATH.get(tRow.path()).get()));
			}
		}
	}

	private GT6Anvils() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GTKitchen fork form). */
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

	/** Registration smoke evidence (the GTKitchen onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 anvil registered: {} rows ({} durability {} / {} durability {} at the ladder ends) (RM.Anvil + RM.AnvilBend)",
					BLOCKS_BY_PATH.size(), ForgeRegistries.BLOCKS.getKey(STONE_ANVIL.get()), STONE_ANVIL.get().durability(),
					ForgeRegistries.BLOCKS.getKey(BLOCKS_BY_PATH.get("infinity_anvil").get()),
					BLOCKS_BY_PATH.get("infinity_anvil").get().durability());
		});
	}
}
