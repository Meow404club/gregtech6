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
import gregtech6.tileentity.energy.GT6ElectricDynamoBlockEntity;

/**
 * The Electric Dynamo family registration (task p28-c-dynamo-family-be) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6FeConverters/GT6FluxDynamos shape (ADR-P3-4). The connected port (the wave-card
 * ruling c): the upstream Flux Dynamo recipe's middle key IS the same-tier Electric
 * Dynamo item ({@code getItem(10111..10115)}, Loader :953-957) and the Flux rows anchor
 * the Dynamos creative page on it, so this family rides the same card. Five material
 * rows verbatim (Loader_MultiTileEntities.java:946-950): NBT_INPUT 32/128/512/2048/8192
 * RU, NBT_OUTPUT 22/88/352/1408/5632 EU — each pair EXACTLY 0.6875 (the display words
 * ride the voltage ladder {@code VN[1..5]}, the materials the Electric_T[1..5] set) —
 * the meta ids 10111-10115 ride {@link ElectricRow#metaId} as the parity column.
 *
 * <p>THE Electric_T[5] EXISTENCE ASSERTION (the wave-card W1 gate): the modern
 * {@code GTMachines.ELECTRIC_T_LADDER} carries only the [1..4] members the canner rows
 * consumed; the dynamo needs the [5] member too — upstream Electric_T[5] = Ti
 * (upstream MT.java:3691: {TinAlloy, SteelGalvanized, Al, StainlessSteel, Cr, Ti, …}).
 * This class carries its OWN six-supplier {@link #ELECTRIC_T_LADDER} (GTMachines stays
 * zero-edited, ADR-P3-4), and the row-table test pins
 * {@code ELECTRIC_T_LADDER.get(5).get() == MT.Ti}.
 *
 * <p>THE T0 ULV ROW (task p28-c-ulv-dynamo-row — the water-wheel chain's last link,
 * research.p28-r-ulv-tier-design): upstream GT6 ships NO VN[0] machine at all (only the
 * Transformer :881 and the batteries :1009/:1033), so the T0 row is a DECLARED
 * tier-extension deviation on three axes. (1) RATIO: in 8 RU / out 8 EU / 1 A — exactly
 * 1:1, NOT the 0.6875 family ratio (0.6875 × 8 = 5.5, no integral packet exists below
 * 22 EU; the rounded 1:1 makes the water wheel's 8 RU packet come out as ONE 8 EU packet,
 * the exact V[0] the ULV machine window [4,16] — GTMachines.ULV_TIER_INPUTS — is centered
 * on). (2) MATERIAL: Electric_T[0] = TinAlloy (upstream MT.java:3691 member [0]). (3)
 * META ID: 10116 = the family base 10111 + 5, the p28-c-ulv-machine-ladder invented-id
 * convention (upstream has no ULV dynamo id). The tier index PREPENDS (ULV = 0, LV..IV
 * shift to 1..5) so the ladder index stays the VN ordinal; the input band rides the
 * Base10:76 ≤16 arm (min = 1, the GT6DynamoBlockEntity fix commit) and the output band
 * [4..16] IS the ULV machine input window.
 *
 * <p>The display-name face (the "Electric Dynamo (LV)" composed rows) and the creative
 * tab join are DECLARED DEFERRED to the W2 render card (lang datagen is a runData
 * product; 创造栏页签 W2 定). KJS surface: none (the registration face is deferred — the
 * KJS binding pool declaration).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ElectricDynamos {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One Electric ladder row — the upstream-parity columns of one Loader :946-950 aRegistry.add line. */
	public record ElectricRow(String path, int metaId, int tier, String voltageWord) {}

	/**
	 * The six rows — the T0 ULV extension row (the task p28-c-ulv-dynamo-row declared
	 * deviation, path suffix "_ulv" the p28 ULV ladder convention, invented id 族基+5)
	 * PREPENDED to the five upstream lines in their line order :946-950 (LV..IV; metaId
	 * 10111-10115; the VN[1..5] display words, CS.java:154; ULV = VN[0]). The tier field
	 * is the family ladder index = the VN ordinal.
	 */
	public static final java.util.List<ElectricRow> ROWS = java.util.List.of(
			new ElectricRow("electric_dynamo_ulv", 10116, 0, "ULV"),
			new ElectricRow("electric_dynamo"   , 10111, 1, "LV"),
			new ElectricRow("electric_dynamo_t2", 10112, 2, "MV"),
			new ElectricRow("electric_dynamo_t3", 10113, 3, "HV"),
			new ElectricRow("electric_dynamo_t4", 10114, 4, "EV"),
			new ElectricRow("electric_dynamo_t5", 10115, 5, "IV"));

	/**
	 * The Electric_T[0..5] ladder of THIS family (upstream MT.java:3691 members [0..5] =
	 * TinAlloy / SteelGalvanized / Al / StainlessSteel / Cr / Ti) — six suppliers, the [5]
	 * member the wave-card existence assertion, the [0] member the T0 row's declared
	 * extension (the upstream [0] slot exists in the material array but carries no
	 * machine). Lazy read (the GTWireSpecs:35 ruling).
	 */
	public static final java.util.List<java.util.function.Supplier<gregapi.oredict.OreDictMaterial>> ELECTRIC_T_LADDER = java.util.List.of(
			() -> gregapi.data.MT.TinAlloy, () -> gregapi.data.MT.SteelGalvanized, () -> gregapi.data.MT.Al,
			() -> gregapi.data.MT.StainlessSteel, () -> gregapi.data.MT.Cr, () -> gregapi.data.MT.Ti);

	public static final RegistryObject<Block> ELECTRIC_DYNAMO_ULV = BLOCKS.register("electric_dynamo_ulv",
			() -> dynamo(0));
	public static final RegistryObject<Block> ELECTRIC_DYNAMO = BLOCKS.register("electric_dynamo",
			() -> dynamo(1));
	public static final RegistryObject<Block> ELECTRIC_DYNAMO_T2 = BLOCKS.register("electric_dynamo_t2",
			() -> dynamo(2));
	public static final RegistryObject<Block> ELECTRIC_DYNAMO_T3 = BLOCKS.register("electric_dynamo_t3",
			() -> dynamo(3));
	public static final RegistryObject<Block> ELECTRIC_DYNAMO_T4 = BLOCKS.register("electric_dynamo_t4",
			() -> dynamo(4));
	public static final RegistryObject<Block> ELECTRIC_DYNAMO_T5 = BLOCKS.register("electric_dynamo_t5",
			() -> dynamo(5));

	/** The row block: hardness/resistance 4.0/4.0 (NBT_HARDNESS column), metal sounds, the family BET supplier. */
	private static GT6DynamoBlock dynamo(int aTier) {
		return new GT6DynamoBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(4.0F, 4.0F).sound(SoundType.METAL), aTier, () -> ELECTRIC_DYNAMO_BE.get());
	}

	/** The tier items — plain BlockItems, stack 16 (the upstream stack column; the name face is W2). */
	public static final RegistryObject<Item> ELECTRIC_DYNAMO_ULV_ITEM = ITEMS.register("electric_dynamo_ulv",
			() -> new BlockItem(ELECTRIC_DYNAMO_ULV.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> ELECTRIC_DYNAMO_ITEM = ITEMS.register("electric_dynamo",
			() -> new BlockItem(ELECTRIC_DYNAMO.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> ELECTRIC_DYNAMO_T2_ITEM = ITEMS.register("electric_dynamo_t2",
			() -> new BlockItem(ELECTRIC_DYNAMO_T2.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> ELECTRIC_DYNAMO_T3_ITEM = ITEMS.register("electric_dynamo_t3",
			() -> new BlockItem(ELECTRIC_DYNAMO_T3.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> ELECTRIC_DYNAMO_T4_ITEM = ITEMS.register("electric_dynamo_t4",
			() -> new BlockItem(ELECTRIC_DYNAMO_T4.get(), new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> ELECTRIC_DYNAMO_T5_ITEM = ITEMS.register("electric_dynamo_t5",
			() -> new BlockItem(ELECTRIC_DYNAMO_T5.get(), new Item.Properties().stacksTo(16)));

	/**
	 * The BET — one BE class over the six ladder blocks (the p8 family-BET Builder.of
	 * varargs shape). Registers AFTER the BLOCKS (vanilla registry order).
	 */
	public static final RegistryObject<BlockEntityType<GT6ElectricDynamoBlockEntity>> ELECTRIC_DYNAMO_BE =
			BLOCK_ENTITY_TYPES.register("electric_dynamo", () -> BlockEntityType.Builder.of(
					GT6ElectricDynamoBlockEntity::new,
					ELECTRIC_DYNAMO_ULV.get(), ELECTRIC_DYNAMO.get(), ELECTRIC_DYNAMO_T2.get(),
					ELECTRIC_DYNAMO_T3.get(), ELECTRIC_DYNAMO_T4.get(), ELECTRIC_DYNAMO_T5.get()).build(null));

	private GT6ElectricDynamos() {}

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
			gregtech6.GT6Mod.LOGGER.info("GT6 electric dynamos registered: 6 rows (ULV 1:1 declared + LV..IV 0.6875, ids 10111-10116, the p28 dynamo family)");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole six-item ladder joins the
	 * machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the class-level
	 * MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what delivers this
	 * handler). JEI 1.20.1 derives its item list from the tab display items, so
	 * registered-but-tab-less was invisible in both the creative menu and JEI. Pool-cut
	 * declaration: upstream gives the family its own "Dynamos" category (tab 10111,
	 * Loader_MultiTileEntities.java:946-950); this port pools the join into MACHINES_TAB
	 * (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(ELECTRIC_DYNAMO_ULV_ITEM.get()));
			aEvent.accept(new ItemStack(ELECTRIC_DYNAMO_ITEM.get()));
			aEvent.accept(new ItemStack(ELECTRIC_DYNAMO_T2_ITEM.get()));
			aEvent.accept(new ItemStack(ELECTRIC_DYNAMO_T3_ITEM.get()));
			aEvent.accept(new ItemStack(ELECTRIC_DYNAMO_T4_ITEM.get()));
			aEvent.accept(new ItemStack(ELECTRIC_DYNAMO_T5_ITEM.get()));
		}
	}
}
