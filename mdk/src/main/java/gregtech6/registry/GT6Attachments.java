package gregtech6.registry;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
 * recipe-system cards; the "Misc Tool Blocks" creative tab is upstream's MTE-registry
 * category and stays out (the crank precedent — the items are /give-reachable; the tab
 * system is the pool card).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Attachments {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One attachment row: the upstream aRegistry.add columns, the block-carrier projection. */
	public record AttachmentRow(
			String path,        // the registry path (also the blockstate/model/lang key tail)
			String displayName, // the upstream row display name, verbatim
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
			new AttachmentRow("tap_ceramic"                     , "Ceramic Tap"                     , GTAttachmentSmallBlock.Family.TAP   , false, 0.5F,   5.0F, SoundType.STONE ),
			new AttachmentRow("tap_plastic"                     , "Plastic Tap"                     , GTAttachmentSmallBlock.Family.TAP   , false, 0.5F,   3.0F, SoundType.WOOD  ),
			new AttachmentRow("tap_stainless_steel"             , "Stainless Tap"                   , GTAttachmentSmallBlock.Family.TAP   , true , 0.5F,   6.0F, SoundType.METAL ),
			new AttachmentRow("tap_tungsten"                    , "Tungsten Tap"                    , GTAttachmentSmallBlock.Family.TAP   , true , 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("tap_tantalum_hafnium_carbide"    , "Tantalum Hafnium Carbide Tap"    , GTAttachmentSmallBlock.Family.TAP   , false, 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("tap_adamantium"                  , "Adamantium Tap"                  , GTAttachmentSmallBlock.Family.TAP   , true , 0.5F, 100.0F, SoundType.METAL ),
			new AttachmentRow("funnel_ceramic"                  , "Ceramic Funnel"                  , GTAttachmentSmallBlock.Family.FUNNEL, false, 0.5F,   5.0F, SoundType.STONE ),
			new AttachmentRow("funnel_plastic"                  , "Plastic Funnel"                  , GTAttachmentSmallBlock.Family.FUNNEL, false, 0.5F,   3.0F, SoundType.WOOD  ),
			new AttachmentRow("funnel_stainless_steel"          , "Stainless Funnel"                , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F,   6.0F, SoundType.METAL ),
			new AttachmentRow("funnel_tungsten"                 , "Tungsten Funnel"                 , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("funnel_tantalum_hafnium_carbide" , "Tantalum Hafnium Carbide Funnel" , GTAttachmentSmallBlock.Family.FUNNEL, false, 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("funnel_adamantium"               , "Adamantium Funnel"               , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F, 100.0F, SoundType.METAL ));

	/** The blocks/BlockItems, one pair per row. The {@code GT6Attachments.}-qualified reference is the legal forward-reference form (the P6 lambda lesson). */
	public static final Map<String, RegistryObject<GTAttachmentSmallBlock>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	static {
		for (AttachmentRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTAttachmentSmallBlock(tRow.family(), tRow.acidProof(), tRow.tickerType(),
							BlockBehaviour.Properties.of()
									.strength(tRow.hardnessF(), tRow.resistanceF()).sound(tRow.sound()))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GT6Attachments.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
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
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GTBarrels.onCommonSetup log shape, acceptance ④). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 attachments registered: {} tap + {} funnel rows ({} / {} blocks valid)",
				ROWS.size() / 2, ROWS.size() / 2, tapBlockArray().length, funnelBlockArray().length));
	}
}
