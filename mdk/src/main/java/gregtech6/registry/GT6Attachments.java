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
 * The attachment registration home (task tap-funnel-attachment spec ⑤) — the
 * shared DeferredRegisters for the wall-attachment family, card-owned (ADR-P3-4):
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the
 * construct event — GT6Mod.java / GTModBusListener.java stay untouched. The GT6Kinetics
 * precedent shape; a separate class keeps GTBarrels from bloating (the card's explicit
 * ruling). The BET type rows live in {@link GTBlockEntities} (TAP_BE / FUNNEL_BE /
 * NOZZLE_BE / CAP_NOZZLE_BE, the CRANK_BE cross-register form).
 *
 * <p>The 24 rows (Loader_MultiTileEntities.java:2108-2134, category "Misc Tool
 * Blocks") — 6 taps + 6 funnels + (task material-mc-f-attachment-rows) 6 nozzles +
 * 6 cap nozzles, one record per row, EVERY live upstream column
 * transcribed: hardness 0.5F, resistance (3.0/5.0/6.0/10.0/10.0/100.0 per family
 * ladder), the tool
 * material → sound (aUtilStone → STONE, aUtilWood → WOOD, aUtilMetal → METAL) and the
 * per-row {@code NBT_ACIDPROOF} (the block-carried flag the BE consumes). The rows'
 * {@code NBT_MAGICPROOF} column has NO upstream BE consumer (the
 * Tap/Funnel/Nozzle/CapNozzle {@code readFromNBT2} pairs read NBT_ACIDPROOF only,
 * MultiTileEntityFluidTap.java:64-67 / MultiTileEntityFluidFunnel.java:55-58 /
 * MultiTileEntityFluidNozzle.java:60-63 / MultiTileEntityFluidCapNozzle.java:54-57)
 * — dead registration data, NOT
 * carried (the gasproof-quartet ruling: no flags into the BE). The row materials are
 * all present in the ported dataset (MT.java:2114 Ceramic / :2135 Plastic / :2489
 * StainlessSteel; ANY.W/Ta4HfC5/Ad ride the drum rows GTBarrels already resolves) —
 * zero rows dropped, per the p7 spec-③ discipline.
 *
 * <p>Task material-mc-f-attachment-rows declaration: the nozzle/cap-nozzle BE
 * activation chains (MultiTileEntityFluidNozzle onBlockActivated3 :80-153 gas-drain
 * half of the tap pair / MultiTileEntityFluidCapNozzle :64-88 gas-fill half) ride the
 * nozzle-function pool card — GTNozzleBlockEntity carries the declared cut (the
 * GTTapBlockEntity:126 "nozzleDrain rides the Nozzle pool card" sentence, now a
 * registered type). The row ids (32747-32750/32079 nozzles, 32058-32062/32082 cap
 * nozzles) join the registration-NBT trims below.
 *
 * <p>Registration-NBT trims (declared): the row ids (32728-32732/32080 taps,
 * 32723-32727/32081 funnels), the stack size (64) and the crafting recipes ride the
 * recipe-system cards; the upstream "Misc Tool Blocks" MTE-registry category (tab 32720)
 * is pooled into the MACHINES_TAB join (task tabfix-b-energy,
 * {@link #onBuildTabContents}; the GTBarrels:257 pooling precedent — supersedes the old
 * stay-out sentence).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Attachments {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The composed tap display template "{@code %s Tap}" — one material slot (task i18n-compose-rows). */
	public static final String TAP_DISPLAY_KEY = "gt6.row.tap.display";
	/** The composed funnel display template "{@code %s Funnel}". */
	public static final String FUNNEL_DISPLAY_KEY = "gt6.row.funnel.display";
	/** The composed nozzle display template "{@code %s Nozzle}" (task material-mc-f-attachment-rows, the Loader :2122-2127 display faces). */
	public static final String NOZZLE_DISPLAY_KEY = "gt6.row.nozzle.display";
	/** The composed cap-nozzle display template "{@code %s Cap Nozzle}" (the Loader :2129-2134 display faces). */
	public static final String CAP_NOZZLE_DISPLAY_KEY = "gt6.row.capnozzle.display";

	/** The row's material small-unit key (the attachment namespace: the slug is the path tail after the family prefix). */
	public static String matUnitKeyOf(AttachmentRow aRow) {
		String tPath = aRow.path();
		String tSlug = switch (aRow.family()) {
			case TAP -> tPath.substring("tap_".length());
			case FUNNEL -> tPath.substring("funnel_".length());
			case NOZZLE -> tPath.substring("nozzle_".length());
			case CAP_NOZZLE -> tPath.substring("cap_nozzle_".length());
		};
		return "gt6.row.attachment.mat." + tSlug;
	}

	/** The composed name of an attachment row (the pure compose seam, one template slot per family). */
	public static net.minecraft.network.chat.MutableComponent displayOf(AttachmentRow aRow) {
		return net.minecraft.network.chat.Component.translatable(
				switch (aRow.family()) {
					case TAP -> TAP_DISPLAY_KEY;
					case FUNNEL -> FUNNEL_DISPLAY_KEY;
					case NOZZLE -> NOZZLE_DISPLAY_KEY;
					case CAP_NOZZLE -> CAP_NOZZLE_DISPLAY_KEY;
				},
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
			return switch (family()) {
				case TAP -> () -> GTBlockEntities.TAP_BE.get();
				case FUNNEL -> () -> GTBlockEntities.FUNNEL_BE.get();
				case NOZZLE -> () -> GTBlockEntities.NOZZLE_BE.get();
				case CAP_NOZZLE -> () -> GTBlockEntities.CAP_NOZZLE_BE.get();
			};
		}
	}

	/**
	 * The 24 rows, upstream Loader order: the six taps :2108-2113, the six funnels
	 * :2115-2120, then (task material-mc-f-attachment-rows) the six nozzles :2122-2127
	 * and the six cap nozzles :2129-2134. acidProof per row verbatim (F/F/T/T/F/T all
	 * four families; the nozzle pair's Steel rows carry F/F). The nozzle material set
	 * differs from the tap/funnel set: Plastic/Steel/Stainless/W/Ta4HfC5/Ad — NO Ceramic
	 * row (so no STONE sound row), the Steel rows ride ANY.Steel (the sound column
	 * aUtilWood → WOOD on plastic, aUtilMetal → METAL elsewhere).
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
			new AttachmentRow("funnel_adamantium"               , "Adamantium"               , GTAttachmentSmallBlock.Family.FUNNEL, true , 0.5F, 100.0F, SoundType.METAL ),
			new AttachmentRow("nozzle_plastic"                  , "Plastic"                  , GTAttachmentSmallBlock.Family.NOZZLE, false, 0.5F,   3.0F, SoundType.WOOD  ),
			new AttachmentRow("nozzle_steel"                    , "Steel"                    , GTAttachmentSmallBlock.Family.NOZZLE, false, 0.5F,   5.0F, SoundType.METAL ),
			new AttachmentRow("nozzle_stainless_steel"          , "Stainless"                , GTAttachmentSmallBlock.Family.NOZZLE, true , 0.5F,   6.0F, SoundType.METAL ),
			new AttachmentRow("nozzle_tungsten"                 , "Tungsten"                 , GTAttachmentSmallBlock.Family.NOZZLE, true , 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("nozzle_tantalum_hafnium_carbide" , "Tantalum Hafnium Carbide" , GTAttachmentSmallBlock.Family.NOZZLE, false, 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("nozzle_adamantium"               , "Adamantium"               , GTAttachmentSmallBlock.Family.NOZZLE, true , 0.5F, 100.0F, SoundType.METAL ),
			new AttachmentRow("cap_nozzle_plastic"              , "Plastic"                  , GTAttachmentSmallBlock.Family.CAP_NOZZLE, false, 0.5F,   3.0F, SoundType.WOOD  ),
			new AttachmentRow("cap_nozzle_steel"                , "Steel"                    , GTAttachmentSmallBlock.Family.CAP_NOZZLE, false, 0.5F,   5.0F, SoundType.METAL ),
			new AttachmentRow("cap_nozzle_stainless_steel"      , "Stainless"                , GTAttachmentSmallBlock.Family.CAP_NOZZLE, true , 0.5F,   6.0F, SoundType.METAL ),
			new AttachmentRow("cap_nozzle_tungsten"             , "Tungsten"                 , GTAttachmentSmallBlock.Family.CAP_NOZZLE, true , 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("cap_nozzle_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide", GTAttachmentSmallBlock.Family.CAP_NOZZLE, false, 0.5F,  10.0F, SoundType.METAL ),
			new AttachmentRow("cap_nozzle_adamantium"           , "Adamantium"               , GTAttachmentSmallBlock.Family.CAP_NOZZLE, true , 0.5F, 100.0F, SoundType.METAL ));

	/**
	 * The row material (task tap-funnel-model-audit — the tint lane's pure seam): the
	 * upstream NBT_MATERIAL column verbatim (Loader_MultiTileEntities.java:2108-2113
	 * taps — MT.Ceramic/MT.Plastic/MT.StainlessSteel/ANY.W/MT.Ta4HfC5/MT.Ad — mirrored
	 * :2115-2120 funnels). Task material-mc-f-attachment-rows: the nozzle pair's slugs
	 * resolve the same words over the family prefix — the Steel rows' upstream
	 * {@code ANY.Steel} (:2123/:2130) folds to MT.Steel (the boiler-recipe
	 * ANY-to-primary fold convention; the tint needs a concrete material). Unknown
	 * slugs (the faucet's synthetic TAP-family rows)
	 * answer {@code null} = untinted (the GT6MoldTintListener -1 doctrine — the faucet
	 * rows dispatch through that listener instead).
	 */
	public static gregapi.oredict.OreDictMaterial materialOf(AttachmentRow aRow) {
		String tSlug = matUnitKeyOf(aRow).substring("gt6.row.attachment.mat.".length());
		return switch (tSlug) {
			case "ceramic" -> gregapi.data.MT.Ceramic;
			case "plastic" -> gregapi.data.MT.Plastic;
			case "steel" -> gregapi.data.MT.Steel;
			case "stainless_steel" -> gregapi.data.MT.StainlessSteel;
			case "tungsten" -> gregapi.data.ANY.W;
			case "tantalum_hafnium_carbide" -> gregapi.data.MT.Ta4HfC5;
			case "adamantium" -> gregapi.data.MT.Ad;
			default -> null;
		};
	}

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
		return familyBlockArray(GTAttachmentSmallBlock.Family.TAP);
	}

	/** The funnel blocks for the shared BET validity (GTBlockEntities.FUNNEL_BE). */
	public static Block[] funnelBlockArray() {
		return familyBlockArray(GTAttachmentSmallBlock.Family.FUNNEL);
	}

	/** The nozzle blocks for the shared BET validity (GTBlockEntities.NOZZLE_BE, task material-mc-f-attachment-rows). */
	public static Block[] nozzleBlockArray() {
		return familyBlockArray(GTAttachmentSmallBlock.Family.NOZZLE);
	}

	/** The cap-nozzle blocks for the shared BET validity (GTBlockEntities.CAP_NOZZLE_BE). */
	public static Block[] capNozzleBlockArray() {
		return familyBlockArray(GTAttachmentSmallBlock.Family.CAP_NOZZLE);
	}

	/** The per-family block walk behind the four BET validity arrays. */
	private static Block[] familyBlockArray(GTAttachmentSmallBlock.Family aFamily) {
		return ROWS.stream().filter(r -> r.family() == aFamily)
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
	 * The tab walk (task tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
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
