package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GT6LongDistWireBlock;

/**
 * The Long Distance Electric Wire registration (task p35-energy-tail-machines) — the
 * 16 metas of the upstream {@code BlocksGT.LongDistWire01} as 16 plain blocks over one
 * class (Loader_Blocks.java:160, the tier byte table {4,4,5,6,6,6,6,6,7,7,7,7,8,8,8,8}
 * verbatim). The wire carries NO BE and NO energy face of its own: it is the passive
 * scan medium of the Long Distance Transformer (the BFS matches the block instance),
 * throughput {@code VMAX[tier]} (GTWireSpecs.VMAX), loss rides the transformer's
 * doInject. The crafting rows (Loader_Blocks.java:162-177: "RSR","PWP","RSR" over
 * wireGt16 of the 16 stepped materials) live in the GT6CraftingRecipes datagen face.
 *
 * <p>Display names: the upstream LH.add form "Long Distance Electric Wire ("+VN[tier]+")"
 * (BlockLongDistWire :44) — the atomic-key lang face composes the same words.
 * KJS surface: none (the registration face is deferred — the KJS binding pool).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6LongDistWires {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One wire row = one upstream meta: the index (the registry-path seat) + the tier byte. */
	public record WireRow(int meta, int tier) {}

	/** The 16 metas, verbatim from Loader_Blocks.java:160 ({@code new byte[] {4,4,5,6,6,6,6,6,7,7,7,7,8,8,8,8}}). */
	public static final List<WireRow> ROWS = List.of(
			new WireRow(0, 4), new WireRow(1, 4), new WireRow(2, 5),
			new WireRow(3, 6), new WireRow(4, 6), new WireRow(5, 6), new WireRow(6, 6), new WireRow(7, 6),
			new WireRow(8, 7), new WireRow(9, 7), new WireRow(10, 7), new WireRow(11, 7),
			new WireRow(12, 8), new WireRow(13, 8), new WireRow(14, 8), new WireRow(15, 8));

	/** The registry path of a meta: long_dist_wire_&lt;meta&gt; (the meta index stays visible — the crafting rows and the RCON selector address it). */
	public static String pathOf(int aMeta) {
		return "long_dist_wire_" + aMeta;
	}

	/** The wire blocks by meta index (the transformer BFS walks the instances, the datagen walks the map). */
	public static final java.util.Map<Integer, RegistryObject<Block>> BLOCKS_BY_META = new java.util.LinkedHashMap<>();
	/** The wire items by meta index. */
	public static final java.util.Map<Integer, RegistryObject<Item>> ITEMS_BY_META = new java.util.LinkedHashMap<>();

	static {
		for (WireRow tRow : ROWS) {
			int tMeta = tRow.meta(), tTier = tRow.tier();
			BLOCKS_BY_META.put(tMeta, BLOCKS.register(pathOf(tMeta), () -> new GT6LongDistWireBlock(tTier)));
			ITEMS_BY_META.put(tMeta, ITEMS.register(pathOf(tMeta),
					() -> new net.minecraft.world.item.BlockItem(BLOCKS_BY_META.get(tMeta).get(), new Item.Properties())));
		}
	}

	/** The wire block of a meta (the RCON/recipe selector). */
	public static Block blockOf(int aMeta) {
		return BLOCKS_BY_META.get(aMeta).get();
	}

	private GT6LongDistWires() {}

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
	}

	/** Registration smoke evidence (the GT6ElectricDynamos.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 long distance wires registered: 16 metas, tiers EV..UV, VMAX throughput (Loader_Blocks:160, the p35 energy tail card)");
		});
	}
}
