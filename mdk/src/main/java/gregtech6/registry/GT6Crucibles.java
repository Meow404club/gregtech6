package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
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
import gregtech6.block.multiblock.GTCrucibleControllerBlock;
import gregtech6.block.multiblock.GTCrucibleWallBlock;
import gregtech6.tileentity.multiblocks.CrucibleWallBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;

/**
 * The LARGE-crucible family registration home (task p26-crucible-multiblock SPEC ⑤, the
 * ADR-P3-4 self-contained form — GT6BurningBoxes/GT6Boilers shape): block + item + BET
 * DeferredRegisters attached from the construct event, {@code GTMachines.java} and
 * {@code GTMultiBlocks.java} untouched.
 *
 * <p><b>Shared-file discipline</b>: this path is the A/B/C shared registration point
 * (tasks.p26-arch-crucible-chain ⑥). Card A's takeover registers the SMALL Smeltery rows
 * (smeltery_stone/bronze/steel) in the same-named class — the merge unions the two bodies
 * onto ONE DeferredRegister trio (same field names, one onModConstruct attach); this file
 * keeps only the C-scoped rows so the union is a verbatim append.
 *
 * <p><b>The rows</b> (the Loader_MultiTileEntities.java projection): the wall is the
 * upstream "Steel Wall" (part id 18009, :1145 — the metalwall family, hardness ==
 * resistance 6.0, the crucible's NBT_DESIGN :1270 wall reference becomes a Block
 * identity); the controller is the "Large Steel Crucible" (:1270 — MTE id 17309,
 * hardness == resistance 6.0, NBT_ACIDPROOF F). The 8-material ladder + NBT_DESIGN
 * wall swap is the defer pool (SPEC ⑤).
 *
 * <p><b>Two BETs, both family-scoped</b> (the GTMultiBlocks.HEAT_TRANSMITTER_BE precedent
 * — the shared part BET's valid list lives in a file this card does not own): the wall
 * BET mounts the relaying {@link CrucibleWallBlockEntity} over the wall block; the
 * controller BET mounts {@link TileEntityCrucible} over the controller block.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Crucibles {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One registration row — the Loader aRegistry.add projection (path + shell material + hardness + the NBT_DESIGN wall). */
	public record CrucibleRow(String path, OreDictMaterial material, String display, float hardness,
			String wallPath, boolean acidProof, int metaId) {}

	/**
	 * The single rung (SPEC ⑤): upstream :1270 verbatim — Steel shell, the 18009 Steel
	 * Wall design, NOT acidproof (the StainlessSteel row carries NBT_ACIDPROOF T, the
	 * rung would flip the wall row with the defer-pool ladder).
	 */
	public static final CrucibleRow STEEL_ROW =
			new CrucibleRow("crucible_steel", MT.Steel, "Large Steel Crucible", 6.0F, "crucible_steel_wall", false, 17309);

	/** The wall row (upstream :1145 "Steel Wall" — part id 18009, the single-rung wall ladder). */
	public static final CrucibleRow STEEL_WALL_ROW =
			new CrucibleRow("crucible_steel_wall", MT.Steel, "Steel Wall", 6.0F, "crucible_steel_wall", false, 18009);

	/** The controller rows (the datagen/lang/walkers iterate; the ladder grows here). */
	public static final List<CrucibleRow> CRUCIBLE_ROWS = List.of(STEEL_ROW);

	/** The registered controller blocks by path. */
	public static final Map<String, RegistryObject<GTCrucibleControllerBlock>> CRUCIBLE_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered controller items, same keys. */
	public static final Map<String, RegistryObject<Item>> CRUCIBLE_ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The wall block (the single-rung wall ladder rides one handle; the ladder grows into a map). */
	public static final RegistryObject<GTCrucibleWallBlock> CRUCIBLE_STEEL_WALL =
			BLOCKS.register(STEEL_WALL_ROW.path(), () -> new GTCrucibleWallBlock(partProperties(STEEL_WALL_ROW.hardness())));

	/** The wall item. */
	public static final RegistryObject<Item> CRUCIBLE_STEEL_WALL_ITEM =
			ITEMS.register(STEEL_WALL_ROW.path(), () -> new GTComposedNameItem(CRUCIBLE_STEEL_WALL.get(), new Item.Properties()));

	static {
		for (CrucibleRow tRow : CRUCIBLE_ROWS) {
			CRUCIBLE_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTCrucibleControllerBlock(tRow, partProperties(tRow.hardness()))));
			CRUCIBLE_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(CRUCIBLE_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The part properties (hardness == resistance on every row; the METAL sound, the machine-block convention). */
	public static BlockBehaviour.Properties partProperties(float aHardness) {
		return BlockBehaviour.Properties.of().strength(aHardness, aHardness).sound(SoundType.METAL);
	}

	/**
	 * The wall part BET: the relaying {@link CrucibleWallBlockEntity} over its one block
	 * (the HEAT_TRANSMITTER_BE degenerate shape — the SHARED part BET cannot mount it, the
	 * valid list lives in GTMultiBlocks.java which this card does not touch).
	 */
	public static final RegistryObject<BlockEntityType<CrucibleWallBlockEntity>> CRUCIBLE_WALL_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_crucible_wall", () -> BlockEntityType.Builder.of(
					CrucibleWallBlockEntity::new, CRUCIBLE_STEEL_WALL.get()).build(null));

	/**
	 * The controller BET: one crucible class over the registered controller blocks (the
	 * LARGE_BOILER_BE one-BET-many-blocks form; the row rides the block carrier).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityCrucible>> MULTIBLOCK_CRUCIBLE_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_crucible", () -> BlockEntityType.Builder.of(
					TileEntityCrucible::new, crucibleBlockArray()).build(null));

	/** The controller-block array for the BET (the same varargs shape). */
	private static Block[] crucibleBlockArray() {
		return CRUCIBLE_BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	/** The lookup for the command/datagen walkers — null for an unknown variant path. */
	@Nullable
	public static Block crucibleBlockByPath(String aPath) {
		RegistryObject<GTCrucibleControllerBlock> tHandle = CRUCIBLE_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The wall block of a controller row (the GTLargeBoilerBlock.wallBlock resolution, the
	 * production {@code getWallBlock} binding). The single-rung ladder resolves to the one
	 * Steel wall; the defer-pool ladder grows this into the row's own wallPath map.
	 */
	public static Block wallBlockOf(CrucibleRow aRow) {
		return CRUCIBLE_STEEL_WALL.get();
	}

	// ------------------------------------------------------------------------------------
	// the block-state sanity walk (the datagen/census helper, the GTMultiBlocks block-array shape)
	// ------------------------------------------------------------------------------------

	/** Every block of this family in registration order (the BET census + the loot walkers). */
	public static List<Block> familyBlocks() {
		List<Block> rBlocks = new ArrayList<>();
		rBlocks.add(CRUCIBLE_STEEL_WALL.get());
		for (RegistryObject<GTCrucibleControllerBlock> tHandle : CRUCIBLE_BLOCKS_BY_PATH.values()) rBlocks.add(tHandle.get());
		return rBlocks;
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
}
