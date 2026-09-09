package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.TileEntityFaucet;
import gregtech6.tileentity.tools.TileEntityMold;

/**
 * The mold family registration home (task p26-crucible-physics-smeltery spec ⑤⑥, the
 * ADR-P3-4 self-contained form — GT6Crucibles/GT6BurningBoxes shape). Card A ships the
 * STONE rung only (the Loader_MultiTileEntities.java:347 opening row: "Mold (Stone)",
 * the 7-cobblestone handcraft of the card face); the Bronze/Invar/Steel/Ceramic rungs
 * (:361-366/:352) and the 30 ceramic molds are the card-B surface.
 *
 * <p>The stone mold carries its {@code preCarvedShape} = the ingot bar (the declared
 * minimal-face deviation on {@link TileEntityMold}): the row0 chain needs no chisel
 * gymnastics to cast an ingot.
 *
 * <p><b>Card B append (p26-crucible-mold-faucet)</b>: the CERAMIC rung — the blank mold
 * (Loader:352, one row, no pre-carve) plus the 30 pre-carved shape molds (:391-420, each
 * upstream a one-item-NBT variant; the 1.20.1 vanilla smelting JSON cannot emit item
 * NBT — SimpleCookingSerializer.fromJson reads a bare item id — so each shape is its
 * own block row, the declared port deviation). Each row pairs with a RAW clay item
 * ({@code *_raw}, the MultiItemRandomTools.java:127+ clay chain) that the VANILLA
 * furnace hardens into the formed block item (the :391-420 RM.add_smelting family,
 * datagen-native smelting JSON). The Crucible Faucet rows (:300 Stone / :305 Ceramic)
 * mount {@link gregtech6.tileentity.tools.TileEntityFaucet} over
 * {@link #FAUCET_BE} — the p12 tap/funnel attachment form. All card-B registration
 * rides a SECOND static block + the same DeferredRegisters (the append-only shared-file
 * discipline); {@link #blockArray()} walks the registration map so the shared BET
 * covers every family block in insertion order.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Molds {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/**
	 * One registration row — the Loader aRegistry.add projection (path + shell material + the pre-carved shape).
	 * The material rides a {@link java.util.function.Supplier} (the GTWireSpecs.Row:81 form — the
	 * class-load-time static rows initialize before MT.init(); a direct MT.Stone captured null).
	 */
	public record MoldRow(String path, java.util.function.Supplier<OreDictMaterial> material, float hardness, int preCarvedShape) {}

	/** The stone rung (the :347 NBT_HARDNESS 1.0 / NBT_RESISTANCE 5.0 pair, pre-carved with the ingot bar). */
	public static final List<MoldRow> ROWS = List.of(
			new MoldRow("mold_stone", () -> MT.Stone, 1.0F, TileEntityMold.ingotShape(0)));

	/** The registered blocks by path. */
	public static final Map<String, RegistryObject<MoldBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (MoldRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new MoldBlock(tRow, BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), 5.0F) // the :347 NBT_HARDNESS/NBT_RESISTANCE pair
							.sound(SoundType.STONE))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The mold BET: one BlockEntityType over the family blocks (ADR-P3-1). Registry path
	 * "mold" mirrors {@link TileEntityMold#getTileEntityName}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityMold>> MOLD_BE =
			BLOCK_ENTITY_TYPES.register("mold", () -> BlockEntityType.Builder.of(
					TileEntityMold::new, blockArray()).build(null));

	/** The block list of the family in registration order (stone first, then the card-B ceramic rung — map insertion order). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<MoldBlock> tHandle : BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static MoldBlock blockByPath(String aPath) {
		RegistryObject<MoldBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// ------------------------------------------------------------------------------------
	// card B (p26-crucible-mold-faucet): the ceramic rung + raw clay items + the faucet
	// ------------------------------------------------------------------------------------

	/** The ceramic blank (Loader:352 — the one row without a crafting recipe, shape 0 until carved). */
	public static final MoldRow CERAMIC_BLANK_ROW = new MoldRow("mold_ceramic", () -> MT.Ceramic, 1.0F, 0);

	/**
	 * The 30 pre-carved ceramic molds (Loader_MultiTileEntities.java:391-420, upstream
	 * order). Each mask is the {@code gt.mold} NBT the upstream smelting row wrote onto
	 * the one Ceramic Mold item; here it is the block row's {@code preCarvedShape}. The
	 * Nugget row's single-center-bit mask is deliberately NOT in {@link TileEntityMold
	 * #MOLD_RECIPES} — it answers through the {@code OP.nugget} fallback (:79-83).
	 */
	public static final List<MoldRow> CERAMIC_ROWS = List.of(
			new MoldRow("mold_ceramic_ingot", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01110_01110_01110), // -> ingot
			new MoldRow("mold_ceramic_billet", () -> MT.Ceramic, 1.0F, 0b0_01100_11110_11110_01100_00000), // -> billet
			new MoldRow("mold_ceramic_chunk", () -> MT.Ceramic, 1.0F, 0b0_11000_11000_00000_00000_00000), // -> chunkGt
			new MoldRow("mold_ceramic_plate", () -> MT.Ceramic, 1.0F, 0b0_11111_11111_11111_11111_11111), // -> plate
			new MoldRow("mold_ceramic_tiny_plate", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_01110_01110_00000), // -> plateTiny
			new MoldRow("mold_ceramic_bolt", () -> MT.Ceramic, 1.0F, 0b0_00000_00000_00100_00100_00000), // -> bolt
			new MoldRow("mold_ceramic_rod", () -> MT.Ceramic, 1.0F, 0b0_00000_00000_11111_00000_00000), // -> stick
			new MoldRow("mold_ceramic_long_rod", () -> MT.Ceramic, 1.0F, 0b0_10000_01000_00100_00010_00001), // -> stickLong
			new MoldRow("mold_ceramic_item_casing", () -> MT.Ceramic, 1.0F, 0b0_11101_11101_11101_00001_11100), // -> casingSmall
			new MoldRow("mold_ceramic_ring", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_01010_01110_00000), // -> ring
			new MoldRow("mold_ceramic_gear", () -> MT.Ceramic, 1.0F, 0b0_10101_01110_11011_01110_10101), // -> gearGt
			new MoldRow("mold_ceramic_small_gear", () -> MT.Ceramic, 1.0F, 0b0_01010_11111_01010_11111_01010), // -> gearGtSmall
			new MoldRow("mold_ceramic_sword", () -> MT.Ceramic, 1.0F, 0b0_00100_01110_01110_01110_01110), // -> toolHeadRawSword
			new MoldRow("mold_ceramic_pickaxe", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_10001_00000_00000), // -> toolHeadRawPickaxe
			new MoldRow("mold_ceramic_spade", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01110_01010_00000), // -> toolHeadRawSpade
			new MoldRow("mold_ceramic_shovel", () -> MT.Ceramic, 1.0F, 0b0_00100_01110_01110_01110_00000), // -> toolHeadRawShovel
			new MoldRow("mold_ceramic_universal_spade", () -> MT.Ceramic, 1.0F, 0b0_00100_01110_01100_01110_00000), // -> toolHeadRawUniversalSpade
			new MoldRow("mold_ceramic_axe", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_01110_01000_00000), // -> toolHeadRawAxe
			new MoldRow("mold_ceramic_double_axe", () -> MT.Ceramic, 1.0F, 0b0_00000_11111_11111_10001_00000), // -> toolHeadRawAxeDouble
			new MoldRow("mold_ceramic_saw", () -> MT.Ceramic, 1.0F, 0b0_00000_11111_11111_00000_00000), // -> toolHeadRawSaw
			new MoldRow("mold_ceramic_hammer", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01010_01110_01110), // -> toolHeadHammer
			new MoldRow("mold_ceramic_file", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01110_00100_00100), // -> toolHeadFile
			new MoldRow("mold_ceramic_screwdriver", () -> MT.Ceramic, 1.0F, 0b0_00000_00100_00100_00100_00100), // -> toolHeadScrewdriver
			new MoldRow("mold_ceramic_chisel", () -> MT.Ceramic, 1.0F, 0b0_01110_00100_00100_00100_00100), // -> toolHeadRawChisel
			new MoldRow("mold_ceramic_arrow", () -> MT.Ceramic, 1.0F, 0b0_00000_00100_00100_01110_00000), // -> toolHeadRawArrow
			new MoldRow("mold_ceramic_hoe", () -> MT.Ceramic, 1.0F, 0b0_00000_00110_01110_00000_00000), // -> toolHeadRawHoe
			new MoldRow("mold_ceramic_sense", () -> MT.Ceramic, 1.0F, 0b0_00000_01111_11111_00000_00000), // -> toolHeadRawSense
			new MoldRow("mold_ceramic_plow", () -> MT.Ceramic, 1.0F, 0b0_11111_11111_11111_11111_00100), // -> toolHeadRawPlow
			new MoldRow("mold_ceramic_builderwand", () -> MT.Ceramic, 1.0F, 0b0_00000_00100_11111_01110_01010), // -> toolHeadBuilderwand
			new MoldRow("mold_ceramic_nugget", () -> MT.Ceramic, 1.0F, 0b0_00000_00000_00100_00000_00000)  // -> nugget (the :82 fallback)
			);

	/** The raw clay items, one per ceramic row (the {@code *_raw} tail; the MultiItemRandomTools :127+ chain hardens in a vanilla furnace). */
	public static final Map<String, RegistryObject<Item>> RAW_ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The raw item of a formed row path ({@code mold_ceramic_plate} → the {@code *_raw} item) — null for an unknown path. */
	@Nullable
	public static Item rawItemByPath(String aFormedPath) {
		RegistryObject<Item> tHandle = RAW_ITEMS_BY_PATH.get(aFormedPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * One faucet registration row (the Loader aRegistry.add projection :300/:305 — the
	 * material drives the {@code getMoldMaxTemperature} heat verdict, the acid-proof
	 * column rides the block carrier like every attachment).
	 */
	/**
	 * One faucet registration row (the Loader aRegistry.add projection :300/:305 — the
	 * material drives the {@code getMoldMaxTemperature} heat verdict, the acid-proof
	 * column rides the block carrier like every attachment). The material rides the same
	 * {@link java.util.function.Supplier} as {@link MoldRow}: the class-load-time static
	 * rows initialize before MT.init() (the P2 two-phase reset) — a direct MT.Ceramic
	 * captured null and the faucet stat NPE'd at {@code material().mMeltingPoint}
	 * (live-probed: "An unexpected error occurred trying to execute that command").
	 */
	public record FaucetRow(String path, java.util.function.Supplier<OreDictMaterial> material, String matDisplay, boolean acidProof, SoundType sound) {}

	/** The two card-B faucet rungs: Stone (:300, the row0 craft) and Ceramic (:305, the raw→furnace pair). */
	public static final List<FaucetRow> FAUCET_ROWS = List.of(
			new FaucetRow("faucet_stone"  , () -> MT.Stone  , "Stone"  , false, SoundType.STONE),
			new FaucetRow("faucet_ceramic", () -> MT.Ceramic, "Ceramic", false, SoundType.STONE));

	/**
	 * The registered faucet blocks by path. The value type is the {@link java.util.function.Supplier}
	 * face (RegistryObject and DeferredHolder both implement it): the nested
	 * {@code TileEntityFaucet.FaucetBlock} type argument sits outside the stonecutter
	 * RegistryObject→DeferredHolder swap's simple-name census (the dotted name defeats the
	 * {@code [A-Za-z0-9]+} catch-all), so the supertype keeps the shared source leg-neutral.
	 */
	public static final Map<String, java.util.function.Supplier<TileEntityFaucet.FaucetBlock>> FAUCET_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered faucet items, same keys as {@link #FAUCET_BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> FAUCET_ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The composed faucet display template "{@code %s Crucible Faucet}" (the Loader display column word order). */
	public static final String FAUCET_DISPLAY_KEY = "gt6.row.faucet.display";

	/** The faucet row's material small-unit key (the attachment namespace over the {@code faucet_} slug). */
	public static String faucetMatUnitKeyOf(FaucetRow aRow) {
		return "gt6.row.faucet.mat." + aRow.path().substring("faucet_".length());
	}

	/** The faucet BET: one BlockEntityType over the family blocks; registry path "faucet" mirrors the BE name. */
	public static final RegistryObject<BlockEntityType<TileEntityFaucet>> FAUCET_BE =
			BLOCK_ENTITY_TYPES.register("faucet", () -> BlockEntityType.Builder.of(
					TileEntityFaucet::new, faucetBlockArray()).build(null));

	/** The faucet block list of the family in registration order. */
	public static Block[] faucetBlockArray() {
		Block[] rBlocks = new Block[FAUCET_ROWS.size()];
		for (int i = 0; i < FAUCET_ROWS.size(); i++) rBlocks[i] = FAUCET_BLOCKS_BY_PATH.get(FAUCET_ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The faucet lookup for the command/RCON walkers — null for an unknown path. */
	@Nullable
	public static TileEntityFaucet.FaucetBlock faucetBlockByPath(String aPath) {
		java.util.function.Supplier<TileEntityFaucet.FaucetBlock> tHandle = FAUCET_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The raw clay faucet item (Loader:305 — only the Ceramic rung rides the raw→furnace pair). */
	public static final RegistryObject<Item> FAUCET_CERAMIC_RAW = ITEMS.register("faucet_ceramic_raw",
			() -> new Item(new Item.Properties()));

	/**
	 * The card-B registration pass — runs AFTER the card-A static block (multiple static
	 * initializers execute in declaration order), registering the ceramic rows into the
	 * SAME maps the stone rung uses (the BET/datagen/command walkers see one family) and
	 * the faucet family beside them. The GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read
	 * forward-reference form against the P6 lambda lesson.
	 */
	static {
		// the family walk: the carvable blank FIRST (Loader:352), then the 30 pre-carved
		// rows — its block/item/raw trio backs the datagen band (the blank clay handcraft,
		// the smelt_mold_ceramic hardening row) and the command walkers
		for (MoldRow tRow : withBlank(CERAMIC_ROWS)) {
			final MoldRow fRow = tRow;
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new MoldBlock(fRow, BlockBehaviour.Properties.of()
							.strength(fRow.hardness(), 5.0F) // the :352 NBT_HARDNESS/NBT_RESISTANCE pair
							.sound(SoundType.STONE))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
			RAW_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path() + "_raw",
					() -> new Item(new Item.Properties())));
		}
		for (FaucetRow tRow : FAUCET_ROWS) {
			final FaucetRow fRow = tRow;
			FAUCET_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new TileEntityFaucet.FaucetBlock(fRow, () -> GT6Molds.FAUCET_BE.get(),
							BlockBehaviour.Properties.of()
									.strength(1.0F, tRow.acidProof() ? 6.0F : 5.0F) // the :300/:305 NBT pair
									.sound(tRow.sound())))); // collision shape: the attachment base answers empty (TileEntityBase10Attachment:35)
			FAUCET_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Molds.FAUCET_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The family walk order: the carvable blank (Loader:352) leads, the 30 pre-carved rows follow. */
	private static java.util.List<MoldRow> withBlank(java.util.List<MoldRow> aRows) {
		java.util.List<MoldRow> rRows = new java.util.ArrayList<>(aRows.size() + 1);
		rRows.add(CERAMIC_BLANK_ROW);
		rRows.addAll(aRows);
		return rRows;
	}


	// ------------------------------------------------------------------------------------
	// the block carrier (the CrucibleBlock form)
	// ------------------------------------------------------------------------------------

	/**
	 * The mold block — a plain cube carrier over the shared BET; the top-face click IS
	 * the NO_GUI interface (the onBlockActivated3 SIDES_TOP gate, :268).
	 */
	public static final class MoldBlock extends GTEntityBlock {

		private final MoldRow mRow;

		public MoldBlock(MoldRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends MoldBlock> codec() {
			return simpleCodec(aProperties -> new MoldBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row. */
		public MoldRow row() {
			return mRow;
		}

		/** The composed display name (the lang provider key). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable("gt6.row.mold.display." + mRow.path());
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Molds.MOLD_BE.get();
		}

		@Override
		public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
			return net.minecraft.world.level.block.RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		//? if forge {
		public net.minecraft.world.InteractionResult use(BlockState aState, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		//?} else {
		/*public net.minecraft.world.InteractionResult useWithoutItem(BlockState aState, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (javap 21.1.249) — the
		//InteractionHand param dropped from the signature; the game loop drives the hands
		//in order and MAIN_HAND is the canonical first entry.
		net.minecraft.world.InteractionHand aHand = net.minecraft.world.InteractionHand.MAIN_HAND;
		*///?}
			// the :268 SIDES_TOP gate — only the top face reacts
			if (aHit.getDirection() != net.minecraft.core.Direction.UP) return net.minecraft.world.InteractionResult.PASS;
			if (aLevel.getBlockEntity(aPos) instanceof TileEntityMold tMold) {
				if (!aLevel.isClientSide) tMold.useTop(aPlayer, aHand);
				return net.minecraft.world.InteractionResult.sidedSuccess(aLevel.isClientSide);
			}
			return net.minecraft.world.InteractionResult.PASS;
		}
	}

	private GT6Molds() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
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
		BLOCK_ENTITY_TYPES.register(tModBus);
	}
}
