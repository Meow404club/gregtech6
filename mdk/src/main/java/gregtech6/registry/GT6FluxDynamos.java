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

import gregtech6.block.energy.GT6DynamoBlock;
import gregtech6.tileentity.energy.GT6FluxDynamoBlockEntity;

/**
 * The Flux Dynamo family registration (task p28-c-dynamo-family-be) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6FeConverters shape (ADR-P3-4; a separate class keeps the card scopes disjoint).
 * Five material rows, the upstream registration column pair verbatim
 * (Loader_MultiTileEntities.java:953-957): {@code aMat = MT.DATA.Flux_T[1..5]} — the root
 * {@code MT.FLUX_T} ladder this card's root commit materialized — with NBT_INPUT
 * 32/128/512/2048/8192 RU and NBT_OUTPUT 88/352/1408/5632/22528 FE, each pair EXACTLY
 * 2.75 (the ratio lives HERE, not in the bridge — the W0 pushPacketTrain ruling; the
 * meta ids 11111-11115 ride {@link FluxRow#metaId} as the upstream-parity column).
 *
 * <p>NBT_HARDNESS/NBT_RESISTANCE 4.0F and stack 16 per row (:953-957); NBT_WASTE_ENERGY
 * = T rides {@link GT6DynamoBlockEntity#WASTE_ENERGY}; the machine name row is the
 * composed "Flux Dynamo (&lt;material&gt;)" face upstream — the modern binding of the
 * display names (lang datagen, a runData product) is DECLARED DEFERRED to the W2 render
 * card, so the items ship plain BlockItems this card. The creative-tab join is likewise
 * W2 (the task card: 创造栏页签 W2 定). KJS surface: none (behavior + material data; the
 * registration face is deferred — the KJS binding pool declaration).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FluxDynamos {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One Flux ladder row — the upstream-parity columns of one Loader :953-957 aRegistry.add line. */
	public record FluxRow(String path, int metaId, int tier) {}

	/** The five rows, upstream line order :953-957 (T1-T5; metaId 11111-11115). */
	public static final java.util.List<FluxRow> ROWS = java.util.List.of(
			new FluxRow("flux_dynamo"   , 11111, 0),
			new FluxRow("flux_dynamo_t2", 11112, 1),
			new FluxRow("flux_dynamo_t3", 11113, 2),
			new FluxRow("flux_dynamo_t4", 11114, 3),
			new FluxRow("flux_dynamo_t5", 11115, 4));

	/**
	 * The ladder material suppliers — {@code MT.FLUX_T[1..5]} = Pb / Invar / Electrum /
	 * EnderiumBase / Enderium (upstream MT.java:3692), read LAZILY through the root array
	 * (the GTWireSpecs:35 ruling: registry classes load before {@code MT.init()}, and the
	 * array itself is late-bound at the end of init() — a direct read in a row initializer
	 * would resolve null/empty).
	 */
	public static final java.util.List<java.util.function.Supplier<gregapi.oredict.OreDictMaterial>> FLUX_T_LADDER = java.util.List.of(
			() -> gregapi.data.MT.FLUX_T[1], () -> gregapi.data.MT.FLUX_T[2], () -> gregapi.data.MT.FLUX_T[3],
			() -> gregapi.data.MT.FLUX_T[4], () -> gregapi.data.MT.FLUX_T[5]);

	public static final RegistryObject<Block> FLUX_DYNAMO = BLOCKS.register("flux_dynamo",
			() -> dynamo(0));
	public static final RegistryObject<Block> FLUX_DYNAMO_T2 = BLOCKS.register("flux_dynamo_t2",
			() -> dynamo(1));
	public static final RegistryObject<Block> FLUX_DYNAMO_T3 = BLOCKS.register("flux_dynamo_t3",
			() -> dynamo(2));
	public static final RegistryObject<Block> FLUX_DYNAMO_T4 = BLOCKS.register("flux_dynamo_t4",
			() -> dynamo(3));
	public static final RegistryObject<Block> FLUX_DYNAMO_T5 = BLOCKS.register("flux_dynamo_t5",
			() -> dynamo(4));

	/** The row block: hardness/resistance 4.0/4.0 (NBT_HARDNESS column), metal sounds, the family BET supplier. */
	private static GT6DynamoBlock dynamo(int aTier) {
		return new GT6DynamoBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(4.0F, 4.0F).sound(SoundType.METAL), aTier, () -> FLUX_DYNAMO_BE.get());
	}

	/** The tier items — plain BlockItems, stack 16 (the upstream stack column; the name face is W2). */
	public static final RegistryObject<Item> FLUX_DYNAMO_ITEM = ITEMS.register("flux_dynamo",
			() -> new BlockItem(FLUX_DYNAMO.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> FLUX_DYNAMO_T2_ITEM = ITEMS.register("flux_dynamo_t2",
			() -> new BlockItem(FLUX_DYNAMO_T2.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> FLUX_DYNAMO_T3_ITEM = ITEMS.register("flux_dynamo_t3",
			() -> new BlockItem(FLUX_DYNAMO_T3.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> FLUX_DYNAMO_T4_ITEM = ITEMS.register("flux_dynamo_t4",
			() -> new BlockItem(FLUX_DYNAMO_T4.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> FLUX_DYNAMO_T5_ITEM = ITEMS.register("flux_dynamo_t5",
			() -> new BlockItem(FLUX_DYNAMO_T5.get(), new Item.Properties().stacksTo(16)));

	/**
	 * The BET — one BE class over the five ladder blocks (the p8 family-BET Builder.of
	 * varargs shape). Registers AFTER the BLOCKS (vanilla registry order, the
	 * GT6FeBatteries doc).
	 */
	public static final RegistryObject<BlockEntityType<GT6FluxDynamoBlockEntity>> FLUX_DYNAMO_BE =
			BLOCK_ENTITY_TYPES.register("flux_dynamo", () -> BlockEntityType.Builder.of(
					GT6FluxDynamoBlockEntity::new,
					FLUX_DYNAMO.get(), FLUX_DYNAMO_T2.get(), FLUX_DYNAMO_T3.get(),
					FLUX_DYNAMO_T4.get(), FLUX_DYNAMO_T5.get()).build(null));

	private GT6FluxDynamos() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6FeConverters fork form). */
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

	/** Registration smoke evidence (the GT6FeConverters.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 flux dynamos registered: 5 rows, 2.75 FE/RU (ids 11111-11115, the p28 dynamo family)");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole five-item ladder joins the
	 * machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the class-level
	 * MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what delivers this
	 * handler). JEI 1.20.1 derives its item list from the tab display items, so
	 * registered-but-tab-less was invisible in both the creative menu and JEI. Pool-cut
	 * declaration: upstream gives the family its own "Dynamos" category (tab 10111,
	 * Loader_MultiTileEntities.java:953-957); this port pools the join into MACHINES_TAB
	 * (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(FLUX_DYNAMO_ITEM.get()));
			aEvent.accept(new ItemStack(FLUX_DYNAMO_T2_ITEM.get()));
			aEvent.accept(new ItemStack(FLUX_DYNAMO_T3_ITEM.get()));
			aEvent.accept(new ItemStack(FLUX_DYNAMO_T4_ITEM.get()));
			aEvent.accept(new ItemStack(FLUX_DYNAMO_T5_ITEM.get()));
		}
	}
}
