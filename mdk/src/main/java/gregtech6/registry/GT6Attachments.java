package gregtech6.registry;

import java.util.List;
import java.util.Map;
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

import gregtech6.GT6Mod;
import gregtech6.block.attachment.GTAttachmentSmallBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The attachment registration home (task p12-tap-funnel-attachment spec ⑤) — the
 * shared DeferredRegisters for the wall-attachment family, card-owned (ADR-P3-4):
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the
 * construct event — GT6Mod.java / GTModBusListener.java stay untouched. The GT6Kinetics
 * precedent shape; a separate class keeps GTBarrels from bloating (the card's explicit
 * ruling). The two BET type rows live in {@link GTBlockEntities} (TAP_BE / FUNNEL_BE,
 * the CRANK_BE cross-register form).
 *
 * <p>The 12 rows (Loader_MultiTileEntities.java:2108-2120, category "Misc Tool
 * Blocks") — 6 taps + 6 funnels, one record per row, EVERY live upstream column
 * transcribed: hardness 0.5F, resistance (5.0/3.0/6.0/10.0/10.0/100.0), the tool
 * material → sound (aUtilStone → STONE, aUtilWood → WOOD, aUtilMetal → METAL) and the
 * per-row {@code NBT_ACIDPROOF} (the block-carried flag the BE consumes). The rows'
 * {@code NBT_MAGICPROOF} column (Ceramic/Plastic/Stainless F, Tungsten/Ta4HfC5/
 * Adamantium T — both families, :2108-2120 verbatim) has NO upstream BE consumer (the
 * Tap/Funnel {@code readFromNBT2} pairs read NBT_ACIDPROOF only, MultiTileEntityFluidTap
 * .java:64-67 / MultiTileEntityFluidFunnel.java:55-58) — dead registration data, NOT
 * carried (the gasproof-quartet ruling: no flags into the BE). The row materials are
 * all present in the ported dataset (MT.java:2114 Ceramic / :2135 Plastic / :2489
 * StainlessSteel; ANY.W/Ta4HfC5/Ad ride the drum rows GTBarrels already resolves) —
 * zero rows dropped, per the p7 spec-③ discipline.
 *
 * <p>Registration-NBT trims (declared): the row ids (32728-32732/32080 taps,
 * 32723-32727/32081 funnels), the stack size (64) and the crafting recipes ride the
 * recipe-system cards; the upstream "Misc Tool Blocks" MTE-registry category (tab 32720)
 * is pooled into the MACHINES_TAB join (task p38-tabfix-b-energy,
 * {@link #onBuildTabContents}; the GTBarrels:257 pooling precedent — supersedes the old
 * stay-out sentence).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Attachments {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The composed tap display template "{@code %s Tap}" — one material slot (task p20-i18n-compose-rows). */
	public static final String TAP_DISPLAY_KEY = "gt6.row.tap.display";
	/** The composed funnel display template "{@code %s Funnel}". */
	public static final String FUNNEL_DISPLAY_KEY = "gt6.row.funnel.display";

	/** The row's material small-unit key (the attachment namespace: the slug is the path tail after tap_/funnel_). */
	public static String matUnitKeyOf(AttachmentRow aRow) {
		String tPath = aRow.path();
		String tSlug = tPath.startsWith("tap_") ? tPath.substring("tap_".length()) : tPath.substring("funnel_".length());
		return "gt6.row.attachment.mat." + tSlug;
	}

	/** The composed name of an attachment row (the pure compose seam). */
	public static net.minecraft.network.chat.MutableComponent displayOf(AttachmentRow aRow) {
		return net.minecraft.network.chat.Component.translatable(
				aRow.family() == GTAttachmentSmallBlock.Family.TAP ? TAP_DISPLAY_KEY : FUNNEL_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(matUnitKeyOf(aRow)));
	}

	/** One attachment row: the upstream aRegistry.add columns, the block-carrier projection. */
	public record AttachmentRow(
			String path,        // the registry path (also the blockstate/model/lang key tail)
			String matDisplay,  // the row material word, verbatim from the old display column
			GTAttachmentSmallBlock.Family family,
			boolean acidProof,  // the upstream NBT_ACIDPROOF
			float hardnessF,    // the upstream NBT_HARDNESS (0.5F on every row)
			float resistanceF,  // the upstream NBT_RESISTANCE
			SoundType sound) {  // the upstream aUtil tool material (Stone/Wood/Metal)

		/** The typed BET this row mounts (the GTBarrels Supplier-block form; resolves through GTBlockEntities at registration time). */
		public Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> tickerType() {
			return family() == GTAttachmentSmallBlock.Family.TAP
					? () -> GTBlockEntities.TAP_BE.get()
					: () -> GTBlockEntities.FUNNEL_BE.get();
		}
	}

	/**
	 * The 12 rows, upstream order: the six taps :2108-2113 then the six funnels
	 * :2115-2120. acidProof per row verbatim (F/F/T/T/F/T both families).
	 */
	public static final List<AttachmentRow> ROWS = List.of(
			new AttachmentRow("tap_ceramic"                     , "Ceramic"                     , GTAttachmentSmallBlock.Family.TAP   , false, 0.5F,   5.0F, SoundType.STONE ),
			new AttachmentRow("tap_plastic"                     , "Plastic"                     , GTAttachmentSmallBlock.Family.TAP   , false, 0.5F,   3.0F, SoundType.WOOD  ),
			new AttachmentRow("tap_stainless_steel"             , "Stainless"                   , GTAttachmentSmallBlock.Family.TAP   , true , 0.5F,   6.0F, SoundType.METAL ),
			new AttachmentRow("tap_tungsten"                    , "Tungsten"                    , GTAttachmentSmallBlock.Family.TAP   , true , 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("tap_tantalum_hafnium_carbide"    , "Tantalum Hafnium Carbide"    , GTAttachmentSmallBlock.Family.TAP   , false, 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("tap_adamantium"                  , "Adamantium"                  , GTAttachmentSmallBlock.Family.TAP   , true , 0.5F, 100.0F, SoundType.METAL ),
			new AttachmentRow("funnel_ceramic"                  , "Ceramic"                  , GTAttachmentSmallBlock.Family.FUNNEL, false, 0.5F,   5.0F, SoundType.STONE ),
			new AttachmentRow("funnel_plastic"                  , "Plastic"                  , GTAttachmentSmallBlock.Family.FUNNEL, false, 0.5F,   3.0F, SoundType.WOOD  ),
			new AttachmentRow("funnel_stainless_steel"          , "Stainless"                , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F,   6.0F, SoundType.METAL ),
			new AttachmentRow("funnel_tungsten"                 , "Tungsten"                 , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("funnel_tantalum_hafnium_carbide" , "Tantalum Hafnium Carbide" , GTAttachmentSmallBlock.Family.FUNNEL, false, 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("funnel_adamantium"               , "Adamantium"               , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F, 100.0F, SoundType.METAL ));

	/** The blocks/BlockItems, one pair per row. The {@code GT6Attachments.}-qualified reference is the legal forward-reference form (the P6 lambda lesson). */
	public static final Map<String, RegistryObject<GTAttachmentSmallBlock>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	static {
		for (AttachmentRow tRow : ROWS) {
			final AttachmentRow fRow = tRow;
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTAttachmentSmallBlock(fRow, fRow.acidProof(), fRow.tickerType(),
							BlockBehaviour.Properties.of()
									.strength(fRow.hardnessF(), fRow.resistanceF()).sound(fRow.sound()))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new gregtech6.block.GTComposedNameItem(GT6Attachments.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The tap blocks for the shared BET validity (GTBlockEntities.TAP_BE). */
	public static Block[] tapBlockArray() {
		return ROWS.stream().filter(r -> r.family() == GTAttachmentSmallBlock.Family.TAP)
				.map(r -> BLOCKS_BY_PATH.get(r.path()).get()).toArray(Block[]::new);
	}

	/** The funnel blocks for the shared BET validity (GTBlockEntities.FUNNEL_BE). */
	public static Block[] funnelBlockArray() {
		return ROWS.stream().filter(r -> r.family() == GTAttachmentSmallBlock.Family.FUNNEL)
				.map(r -> BLOCKS_BY_PATH.get(r.path()).get()).toArray(Block[]::new);
	}

	private GT6Attachments() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Kinetics.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GTMachines fork precedent.
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GTBarrels.onCommonSetup log shape, acceptance ④). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 attachments registered: {} tap + {} funnel rows ({} / {} blocks valid)",
				ROWS.size() / 2, ROWS.size() / 2, tapBlockArray().length, funnelBlockArray().length));
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
	 * joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream hangs the family on its "Misc Tool Blocks" category
	 * (tab 32720, Loader_MultiTileEntities.java:2108-2120); this port pools the join into
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
