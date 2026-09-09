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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

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
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * The crucible family registration home (task p26-crucible-physics-smeltery spec ⑥, the
 * ADR-P3-4 self-contained form — GT6BurningBoxes/GTBarrels shape): block + item + BET
 * DeferredRegisters attached from the construct event, {@code GTMachines.java} untouched.
 *
 * <p><b>The rows</b> (the Loader_MultiTileEntities.java:250-289 Smeltery projection, the
 * card's Stone/Bronze/Steel minimal ladder): upstream the stone Smeltery is the id-1000
 * opening row ("Smeltery (Stone)", 8 cobblestone handcraft) with Bronze/Steel as the
 * next rungs — the card runs the three-rung ladder only (the full 8-material ladder is
 * the defer pool, tasks.p26-arch-crucible-chain defer_pool).
 *
 * <p><b>The liquid-level carrier</b> (spec ⑦): the rendered fill height rides the
 * {@link CrucibleBlock#LIQUID_LEVEL} int property (0..8 buckets) with datagen-native
 * blockstate variants — the mDisplayedHeight client sync (:82/:298/:558-571) collapses
 * into the blockstate, and the per-material molten tint (getTextureMolten, :587-591) plus
 * the MELTDOWN glow (:576-584) stay the declared card-internal faces/pool (no custom
 * atlas work this card; the tint comes free through the existing item-color idiom only
 * when a dedicated molten texture lands).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Crucibles {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/**
	 * One registration row — the Loader aRegistry.add projection (path + shell material + hardness pair).
	 * The material rides a {@link java.util.function.Supplier} (the GTWireSpecs.Row:81 form): the
	 * static ROWS initialize at class-load time, which on the mod bus runs before MT.init() assigns
	 * the OreDictMaterial statics (the P2 two-phase material reset) — a direct MT.Steel reference
	 * captured null and every shell read as the Stone fallback ceiling (live-probed: all three rungs
	 * answered max=1375K with row.material()==NULL).
	 */
	public record CrucibleRow(String path, java.util.function.Supplier<OreDictMaterial> material, String display, float hardness) {}

	/** The three rungs: Stone (the opening row, 6.0 hardness like the :250 NBT_HARDNESS family), Bronze (7.0), Steel (6.0). */
	public static final List<CrucibleRow> ROWS = List.of(
			new CrucibleRow("smeltery_stone", () -> MT.Stone, "Smeltery (Stone)", 6.0F),
			new CrucibleRow("smeltery_bronze", () -> MT.Bronze, "Smeltery (Bronze)", 7.0F),
			new CrucibleRow("smeltery_steel", () -> MT.Steel, "Smeltery (Steel)", 6.0F));

	/** The registered blocks by path (the BET/datagen/command walkers iterate this). */
	public static final Map<String, RegistryObject<CrucibleBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (CrucibleRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new CrucibleBlock(tRow, BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), tRow.hardness() * 2) // the upstream NBT_HARDNESS/NBT_RESISTANCE pair
							.sound(SoundType.STONE))));
			// the GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read forward-reference form (the P6 lambda lesson)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Crucibles.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The shared BET: one BlockEntityType over the three shell blocks (ADR-P3-1, the
	 * one-TE-class-many-material-blocks counterpart). Registry path "smeltery" mirrors
	 * {@link TileEntitySmeltery#getTileEntityName}. The blocks register before the BET
	 * (vanilla registry order across DeferredRegisters), so the .get() calls are safe.
	 */
	public static final RegistryObject<BlockEntityType<TileEntitySmeltery>> CRUCIBLE_BE =
			BLOCK_ENTITY_TYPES.register("smeltery", () -> BlockEntityType.Builder.of(
					TileEntitySmeltery::new, blockArray()).build(null));

	/** The block list of the family in registration order (the BET multi-mount form). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[ROWS.size()];
		for (int i = 0; i < ROWS.size(); i++) rBlocks[i] = BLOCKS_BY_PATH.get(ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static CrucibleBlock blockByPath(String aPath) {
		RegistryObject<CrucibleBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// ------------------------------------------------------------------------------------
	// the block carrier (the BurningBoxBlock form: the row rides the instance)
	// ------------------------------------------------------------------------------------

	/**
	 * The crucible block — a plain cube carrier over the shared BET; the shell material
	 * rides the row, the fill height rides {@link #LIQUID_LEVEL}, and the top-face click
	 * opens the NO_GUI world interaction (the onBlockActivated3 SIDES_TOP gate, :413).
	 */
	public static final class CrucibleBlock extends GTEntityBlock {

		/**
		 * The fill-height property (spec ⑦): 0 = empty, 8 = full — the mDisplayedHeight
		 * :298 {@code UT.Code.scale(tTotal, MAX_AMOUNT, 255, F)} census bucketed to 9
		 * datagen-native variants. Single-owner property (the P16 identity lesson).
		 */
		public static final IntegerProperty LIQUID_LEVEL = IntegerProperty.create("gt_liquid_level", 0, 8);

		private final CrucibleRow mRow;

		public CrucibleBlock(CrucibleRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(LIQUID_LEVEL, 0));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends CrucibleBlock> codec() {
			return simpleCodec(aProperties -> new CrucibleBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the GTBarrelBlock.capacityL carrier read). */
		public CrucibleRow row() {
			return mRow;
		}

		/** The composed display name (the row display column; composed keys ride the lang provider). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable("gt6.row.crucible.display." + mRow.path());
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(LIQUID_LEVEL);
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Crucibles.CRUCIBLE_BE.get();
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
			// the :413 SIDES_TOP gate — only the top face reacts (the NO_GUI contract)
			if (aHit.getDirection() != net.minecraft.core.Direction.UP) return net.minecraft.world.InteractionResult.PASS;
			if (aLevel.getBlockEntity(aPos) instanceof TileEntitySmeltery tCrucible) {
				if (!aLevel.isClientSide) tCrucible.useTop(aPlayer, aHand);
				return net.minecraft.world.InteractionResult.sidedSuccess(aLevel.isClientSide);
			}
			return net.minecraft.world.InteractionResult.PASS;
		}
	}

	private GT6Crucibles() {}

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

	/** (unused today) the ItemStack display helper for the /give-facing items. */
	public static Component displayOf(CrucibleRow aRow) {
		return Component.translatable("gt6.row.crucible.display." + aRow.path());
	}
}
