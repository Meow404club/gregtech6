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
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Molds {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One registration row — the Loader aRegistry.add projection (path + shell material + the pre-carved shape). */
	public record MoldRow(String path, OreDictMaterial material, float hardness, int preCarvedShape) {}

	/** The stone rung (the :347 NBT_HARDNESS 1.0 / NBT_RESISTANCE 5.0 pair, pre-carved with the ingot bar). */
	public static final List<MoldRow> ROWS = List.of(
			new MoldRow("mold_stone", MT.Stone, 1.0F, TileEntityMold.ingotShape(0)));

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

	/** The block list of the family in registration order. */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[ROWS.size()];
		for (int i = 0; i < ROWS.size(); i++) rBlocks[i] = BLOCKS_BY_PATH.get(ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static MoldBlock blockByPath(String aPath) {
		RegistryObject<MoldBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
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
