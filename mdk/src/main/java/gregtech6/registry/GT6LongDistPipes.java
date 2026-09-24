package gregtech6.registry;

import java.util.List;

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
import gregtech6.block.tools.GT6LongDistPipeBlock;
import gregtech6.tileentity.inventories.GT6LongDistanceItemPipeBlockEntity;
import gregtech6.tileentity.tank.GT6LongDistanceFluidPipeBlockEntity;

/**
 * The Long Distance Pipe registration (task p35-long-distance-pipes) — the 16 metas of
 * the upstream {@code BlocksGT.LongDistPipe01} as 16 plain blocks over one class
 * (Loader_Blocks.java:179, the temperature row verbatim: meta 0 = -1 the ITEM pipeline,
 * metas 1..4 = the StainlessSteel/W/Adamantium/Draconium melting points the FLUID
 * pipelines, metas 5..15 = 0 the dead rows), plus the two endpoint MTEs of
 * Loader_MultiTileEntities :906-:907 (meta ids 10060/10061, "Long Distance Item/Fluid
 * Pipeline Endpoint") as the facing-cube carriers over the
 * {@link GT6ElectricTransformerBlock} form (the GT6LongDistanceTransformers reuse — the
 * FRONT/BACK convention is the same shape). The endpoints are the lazy delegates: no
 * transport tick, the sender pushes straight into the target's BACK-adjacent inventory/
 * tank through the wire-blob BFS (MultiTileEntityLongDistancePipelineItem :130-178 /
 * ...Fluid :138-186).
 *
 * <p>Display names: the BlockLongDistPipe :38-39 form "Long Distance Item Pipeline" /
 * "Long Distance Fluid Pipeline ("+temp+" K)" and the :906-:907 "… Pipeline Endpoint"
 * wording. The crafting rows (:181-:185 + :906-:907) are deferred — the pipeMedium and
 * casingMachine item carriers are absent from the registry (the OP families
 * unregistered), the obtainability rides the crafting pool (the crystal-charger
 * precedent, the energy tail card leftovers). KJS surface: none (the registration face
 * is deferred — the KJS binding pool).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6LongDistPipes {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One pipe row = one upstream meta: the registry-path seat + the temperature rating. */
	public record PipeRow(int meta, long temperatureK) {}

	/**
	 * The 16 metas — the Loader_Blocks.java:179 temperature row as LITERALS (the
	 * GT6LongDistWires tier-byte form): meta 0 = -1 (item), then the upstream computed
	 * SS/W/Ad/Draconium melting points {1943, 3695, 5425, 4500} (the zh dump
	 * gt.block.longdistpipe.01.1-4 faces verbatim; the material table populates after
	 * mod construct, so the eager MT.*.mMeltingPoint read is not available at class-init
	 * — the values are pinned against the MT table by the offline test), then the 11
	 * dead 0 rows.
	 */
	public static final List<PipeRow> ROWS = List.of(
			new PipeRow(0, -1),
			new PipeRow(1, 1943),
			new PipeRow(2, 3695),
			new PipeRow(3, 5425),
			new PipeRow(4, 4500),
			new PipeRow(5, 0), new PipeRow(6, 0), new PipeRow(7, 0), new PipeRow(8, 0),
			new PipeRow(9, 0), new PipeRow(10, 0), new PipeRow(11, 0), new PipeRow(12, 0),
			new PipeRow(13, 0), new PipeRow(14, 0), new PipeRow(15, 0));

	/** The registry path of a meta: long_dist_pipe_&lt;meta&gt; (the meta index stays visible — the GT6LongDistWires.pathOf convention). */
	public static String pathOf(int aMeta) {
		return "long_dist_pipe_" + aMeta;
	}

	/** The wire-pipe blocks by meta index (the BFS walks the instances, the datagen walks the map). */
	public static final java.util.Map<Integer, RegistryObject<Block>> WIRE_BLOCKS_BY_META = new java.util.LinkedHashMap<>();
	/** The wire-pipe items by meta index. */
	public static final java.util.Map<Integer, RegistryObject<Item>> WIRE_ITEMS_BY_META = new java.util.LinkedHashMap<>();

	static {
		for (PipeRow tRow : ROWS) {
			int tMeta = tRow.meta();
			WIRE_BLOCKS_BY_META.put(tMeta, BLOCKS.register(pathOf(tMeta), () -> new GT6LongDistPipeBlock(tRow.temperatureK())));
			WIRE_ITEMS_BY_META.put(tMeta, ITEMS.register(pathOf(tMeta),
					() -> new BlockItem(WIRE_BLOCKS_BY_META.get(tMeta).get(), new Item.Properties())));
		}
	}

	/** The row pipe block of a meta (the RCON/datagen selector). */
	public static Block wireBlockOf(int aMeta) {
		return WIRE_BLOCKS_BY_META.get(aMeta).get();
	}

	// the two endpoints (the :906-:907 MTE rows, 16.0F hardness/resistance, max stack 16)

	public static final RegistryObject<Block> ITEM_PIPE_BLOCK = BLOCKS.register("longdist_item_pipe",
			() -> new GT6ElectricTransformerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					// the qualified self-reference — the lambda defers past the textual forward reference
					.strength(16.0F, 16.0F).sound(SoundType.METAL), () -> GT6LongDistPipes.LONGDIST_ITEM_PIPE_BE.get()));
	public static final RegistryObject<Item> ITEM_PIPE_ITEM = ITEMS.register("longdist_item_pipe",
			() -> new BlockItem(ITEM_PIPE_BLOCK.get(), new Item.Properties().stacksTo(16)));

	public static final RegistryObject<Block> FLUID_PIPE_BLOCK = BLOCKS.register("longdist_fluid_pipe",
			() -> new GT6ElectricTransformerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(16.0F, 16.0F).sound(SoundType.METAL), () -> GT6LongDistPipes.LONGDIST_FLUID_PIPE_BE.get()));
	public static final RegistryObject<Item> FLUID_PIPE_ITEM = ITEMS.register("longdist_fluid_pipe",
			() -> new BlockItem(FLUID_PIPE_BLOCK.get(), new Item.Properties().stacksTo(16)));

	/** The item-endpoint BET — one BE class over its one block (the family-BET shape). */
	public static final RegistryObject<BlockEntityType<GT6LongDistanceItemPipeBlockEntity>> LONGDIST_ITEM_PIPE_BE =
			BLOCK_ENTITY_TYPES.register("longdist_item_pipe", () -> BlockEntityType.Builder.of(
					GT6LongDistanceItemPipeBlockEntity::new, ITEM_PIPE_BLOCK.get()).build(null));

	/** The fluid-endpoint BET. */
	public static final RegistryObject<BlockEntityType<GT6LongDistanceFluidPipeBlockEntity>> LONGDIST_FLUID_PIPE_BE =
			BLOCK_ENTITY_TYPES.register("longdist_fluid_pipe", () -> BlockEntityType.Builder.of(
					GT6LongDistanceFluidPipeBlockEntity::new, FLUID_PIPE_BLOCK.get()).build(null));

	private GT6LongDistPipes() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6LongDistWires fork form). */
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

	/** Registration smoke evidence (the GT6LongDistWires.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 long distance pipes registered: 16 metas (-1 item, 4 fluid ratings, 11 dead), 2 endpoints (ids 10060/10061, the p35 pipes card)");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the 16 {@link #WIRE_ITEMS_BY_META} metas
	 * plus the two endpoint items join the machines tab; the GT6BurningBoxes
	 * .onBuildTabContents verbatim form, the class-level MOD-bus
	 * {@code @Mod.EventBusSubscriber} at the class head is what delivers this handler).
	 * JEI 1.20.1 derives its item list from the tab display items, so registered-but-
	 * tab-less was invisible in both the creative menu and JEI. Pool-cut declaration:
	 * upstream hangs the LD pipe metas AND the two endpoint MTEs on the "Long Distance
	 * Transport" category (tab 10060, Loader_MultiTileEntities.java:906-907 endpoints,
	 * the block ride :915); this port pools the join into MACHINES_TAB (the GTBarrels:257
	 * pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : WIRE_ITEMS_BY_META.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
			aEvent.accept(new ItemStack(ITEM_PIPE_ITEM.get()));
			aEvent.accept(new ItemStack(FLUID_PIPE_ITEM.get()));
		}
	}
}
