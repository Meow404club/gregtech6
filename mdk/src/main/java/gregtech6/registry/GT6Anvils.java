package gregtech6.registry;

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
 * The anvil family registration (task p28-c-anvil) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6Kitchen shape (a separate class keeps the card scopes disjoint). Ports the FIRST
 * TWO "Misc Tool Blocks" anvil rows of Loader_MultiTileEntities.java:2185-2186:
 * <ul>
 * <li>Stone Anvil (:2185) — MT.Stone, NBT_DURABILITY 10000, hardness 1.0 / resistance
 *     6.0, aUtilStone; the recipe "RRR","hR ","RRR" over {@code Blocks.stone} rides the
 *     crafting datagen (GT6CraftingRecipes, the tier-a band).</li>
 * <li>Blackstone Anvil (:2186) — MT.STONES.Blackstone, NBT_DURABILITY 100000, same
 *     shape over {@code OP.stone.dat(Blackstone)} = the vanilla BLACKSTONE item.</li>
 * </ul>
 * The other 14 material rows (:2187-2200+, GraniteBlack/GraniteRed/Pb/Bronze/... /
 * BlackSteel) are the material-ladder pool cut — the single-tier family ruling (the BE
 * reads BOTH carrier values, so a later ladder card appends block rows only). The
 * durability ladder IS the gameplay: the Stone anvil breaks after ONE displayed point
 * (the wear floor = one point per working hit), the Blackstone one survives ten.
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

	/** The stone anvil — MT.Stone, 10000 durability units = ONE working point (:2185). */
	public static final RegistryObject<GTAnvilBlock> STONE_ANVIL = BLOCKS.register("stone_anvil",
			() -> new GTAnvilBlock(() -> MT.Stone, 10000, () -> GT6Anvils.ANVIL_BE.get(),
					BlockBehaviour.Properties.of().strength(1.0F, 6.0F).sound(SoundType.STONE)));

	/** The blackstone anvil — MT.STONES.Blackstone, 100000 durability units = TEN points (:2186). */
	public static final RegistryObject<GTAnvilBlock> BLACKSTONE_ANVIL = BLOCKS.register("blackstone_anvil",
			() -> new GTAnvilBlock(() -> MT.STONES.Blackstone, 100000, () -> GT6Anvils.ANVIL_BE.get(),
					BlockBehaviour.Properties.of().strength(1.0F, 6.0F).sound(SoundType.STONE)));

	/**
	 * The anvil BET — the shared-BET multi-mount (ADR-P3-1): one BE class over both rows
	 * (the rows differ ONLY in the registration NBT the carrier now holds).
	 */
	public static final RegistryObject<BlockEntityType<GT6AnvilBlockEntity>> ANVIL_BE =
			BLOCK_ENTITY_TYPES.register("anvil", () -> BlockEntityType.Builder.of(
					GT6AnvilBlockEntity::new, STONE_ANVIL.get(), BLACKSTONE_ANVIL.get()).build(null));

	/** The anvil items (plain BlockItems — no item-capability face, the kitchen ruling). */
	public static final RegistryObject<Item> STONE_ANVIL_ITEM = ITEMS.register("stone_anvil",
			() -> new BlockItem(STONE_ANVIL.get(), new Item.Properties()));
	public static final RegistryObject<Item> BLACKSTONE_ANVIL_ITEM = ITEMS.register("blackstone_anvil",
			() -> new BlockItem(BLACKSTONE_ANVIL.get(), new Item.Properties()));

	/** The MACHINES-TAB join (the p27 kitchen retirement form — the anvil is a processing block). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(STONE_ANVIL_ITEM.get()));
			aEvent.accept(new ItemStack(BLACKSTONE_ANVIL_ITEM.get()));
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
			GT6Mod.LOGGER.info("GT6 anvil registered: {} durability {} / {} durability {} (RM.Anvil + RM.AnvilBend)",
					ForgeRegistries.BLOCKS.getKey(STONE_ANVIL.get()), STONE_ANVIL.get().durability(),
					ForgeRegistries.BLOCKS.getKey(BLACKSTONE_ANVIL.get()), BLACKSTONE_ANVIL.get().durability());
		});
	}
}
