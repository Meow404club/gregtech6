package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;

/**
 * The Steam Boiler Tank family registration home (task p13-boiler-tank spec ⑨, the
 * GT6BurningBoxes self-contained-DR form): 26 blocks/items over ONE shared BET row —
 * the single BE class {@link GTBoilerTankBlockEntity} over the block carrier.
 *
 * <p><b>The rows</b> (Loader_MultiTileEntities.java:551-579 transcribed verbatim, upstream
 * file order, every ID/NBT_OUTPUT_SU/NBT_HARDNESS/NBT_RESISTANCE column kept):
 * <ul>
 * <li>{@link #BOILER_ROWS} = 13 — "Steam Boiler Tank (Material)" (:553-565, IDs
 *     1200-1212 — NOTE the upstream line order is NOT id-ordered: 1200,1201,1202,1210,
 *     1211,1203,1204,1205,1206,1209,1207,1208,1212), NBT_OUTPUT_SU = 16/20/24/24/28/16/
 *     32/96/112/112/128/128/256 × STEAM_PER_EU (the loader's own CS.java:240 = 2, so the
 *     stored row value is 32..512);</li>
 * <li>{@link #STRONG_BOILER_ROWS} = 13 — "Strong Steam Boiler Tank (Material)"
 *     (:567-579, IDs 1250-1262, same non-monotonic id order 1250,1251,1252,1260,1261,
 *     1253,1254,1255,1256,1259,1257,1258,1262), 64..1024 × STEAM_PER_EU = 128..2048.</li>
 * </ul>
 * NO commented-out rows and NO gaps in this section (the upstream :552-580 block is dense
 * — the count is exactly 13+13 = 26, the card's arithmetic holds, unlike the burning-box
 * census). The HBM-conditional dense-plate item of :567 is a recipe-side detail (no recipe
 * layer in this port); every row else is byte-identical.
 *
 * <p>Material slugs follow the port conventions (GT6Kinetics/GTBarrels): MT.Pb → lead,
 * MT.Bi → bismuth, MT.Bronze → bronze, MT.ArsenicCopper → arsenic_copper, MT.ArsenicBronze
 * → arsenic_bronze, MT.Invar → invar, ANY.Steel → steel, MT.Cr → chromium, MT.Ti →
 * titanium, MT.Netherite → netherite, ANY.W → tungsten, MT.TungstenSteel → tungstensteel,
 * MT.Ultimet → ultimet. Hardness == resistance on every row (upstream NBT_HARDNESS ==
 * NBT_RESISTANCE); the METAL sound (the machine-block convention).
 *
 * <p>Creative tab (task p38-tabfix-a-multiblock): all 26 items join MULTIBLOCKS_TAB via
 * {@link #onBuildTabContents} — registered-but-tab-less is invisible in BOTH the creative
 * menu and JEI (the BurningBoxes issue-#10 form, superseding the old axle/diesel tab-less
 * precedent). Pool cut declared: upstream rode the per-family "Multiblock Machines"
 * creative tab (tab id 17101, Loader :553-579); this port pools the family into the
 * gt6:multiblocks tab. No GUI by census (:103 NO_GUI_FUNNEL_TO_TANK — the upstream
 * tooltip IS the contract, the funnel face is ported).
 *
 * <p>The block carrier is the SteamEngineBlock shape: FACING horizontal (the FRONT = the
 * barometer face, :246-247 getDefaultSide SIDE_FRONT over SIDES_HORIZONTAL), the row rides
 * the instance, and the two pressurised-removal faces of the explosion family live here —
 * {@code playerWillDestroy} (:202-205, the removedByPlayer counterpart — NOT an onRemove
 * override) and {@code onBlockExploded} (:208-211, the Forge explosion face
 * IForgeBlock.java:716-720, the BE consulted BEFORE the air swap removes it).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Boilers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One Loader material — slug + display name + the NBT_HARDNESS (== NBT_RESISTANCE) pair. */
	public record BoilerMaterial(String slug, String display, float hardness) {}

	/** The 13 boiler materials, the Loader :553-579 material column (Ultimet is boiler-only, :565/:579). */
	public static final BoilerMaterial
			MAT_LEAD        = new BoilerMaterial("lead"        , "Lead"          ,  4.0F),
			MAT_BISMUTH     = new BoilerMaterial("bismuth"     , "Bismuth"       ,  4.0F),
			MAT_BRONZE      = new BoilerMaterial("bronze"      , "Bronze"        ,  7.0F),
			MAT_ARSENIC_COPPER  = new BoilerMaterial("arsenic_copper", "Arsenic Copper", 7.0F),
			MAT_ARSENIC_BRONZE  = new BoilerMaterial("arsenic_bronze", "Arsenic Bronze", 7.0F),
			MAT_INVAR       = new BoilerMaterial("invar"       , "Invar"         ,  4.0F),
			MAT_STEEL       = new BoilerMaterial("steel"       , "Steel"         ,  6.0F),
			MAT_CHROMIUM    = new BoilerMaterial("chromium"    , "Chromium"      ,  4.0F),
			MAT_TITANIUM    = new BoilerMaterial("titanium"    , "Titanium"      ,  9.0F),
			MAT_NETHERITE   = new BoilerMaterial("netherite"   , "Netherite"     ,  9.0F),
			MAT_TUNGSTEN    = new BoilerMaterial("tungsten"    , "Tungsten"      , 10.0F),
			MAT_TUNGSTENSTEEL = new BoilerMaterial("tungstensteel", "Tungstensteel", 12.5F),
			MAT_ULTIMET     = new BoilerMaterial("ultimet"     , "Ultimet"       , 12.5F);

	/** The composed Steam Boiler Tank display template "{@code Steam Boiler Tank (%s)}" — one material slot (task p20-i18n-compose-rows). */
	public static final String DISPLAY_KEY = "gt6.row.boiler.display";
	/** The Strong template "{@code Strong Steam Boiler Tank (%s)}" — the Strong wording rides the template. */
	public static final String DISPLAY_STRONG_KEY = "gt6.row.boiler.display.strong";

	/** The row's material small-unit key. */
	public static String matUnitKeyOf(BoilerRow aRow) {
		return "gt6.row.mat." + aRow.material().slug();
	}

	/** The composed name of a boiler row (the pure compose seam). */
	public static net.minecraft.network.chat.MutableComponent displayOf(BoilerRow aRow) {
		return net.minecraft.network.chat.Component.translatable(aRow.strong() ? DISPLAY_STRONG_KEY : DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(matUnitKeyOf(aRow)));
	}

	/**
	 * One registration row — the block-carrier projection of one upstream aRegistry.add
	 * line. {@code metaId} is the upstream MultiTile id, kept for the zero-diff table test;
	 * {@code outputSteamPerTick} is the raw NBT_OUTPUT_SU value (the loader already multiplied
	 * by STEAM_PER_EU 2 — the BE stores it verbatim, :77).
	 *
	 * @param path               the gt6 registry path (the blockstate/model/lang key tail)
	 * @param displayName        the upstream row display name, verbatim
	 * @param metaId             the upstream MultiTileEntity id (1200-1212 / 1250-1262)
	 * @param outputSteamPerTick the upstream NBT_OUTPUT_SU (16*STEAM_PER_EU .. 1024*STEAM_PER_EU)
	 * @param material           the row material (slug/display/hardness)
	 * @param strong             the Strong ladder flag (the display prefix, no behavioural
	 *                           difference upstream — the SAME class and the same NBT set)
	 */
	public record BoilerRow(String path, int metaId, long outputSteamPerTick, BoilerMaterial material, boolean strong) {
		/** The block properties (hardness == resistance on every row; the METAL sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(material.hardness(), material.hardness())
					.sound(SoundType.METAL);
		}
	}

	/**
	 * The 13 standard rows (:553-565, the upstream line order with the non-monotonic ids —
	 * the display material column carries NBT_OUTPUT_SU 16/20/24/24/28/16/32/96/112/112/
	 * 128/128/256 × STEAM_PER_EU).
	 */
	public static final List<BoilerRow> BOILER_ROWS = List.of(
			row("steam_boiler_tank_", MAT_LEAD            , 1200,  16),
			row("steam_boiler_tank_", MAT_BISMUTH         , 1201,  20),
			row("steam_boiler_tank_", MAT_BRONZE          , 1202,  24),
			row("steam_boiler_tank_", MAT_ARSENIC_COPPER  , 1210,  24),
			row("steam_boiler_tank_", MAT_ARSENIC_BRONZE  , 1211,  28),
			row("steam_boiler_tank_", MAT_INVAR           , 1203,  16),
			row("steam_boiler_tank_", MAT_STEEL           , 1204,  32),
			row("steam_boiler_tank_", MAT_CHROMIUM        , 1205,  96),
			row("steam_boiler_tank_", MAT_TITANIUM        , 1206, 112),
			row("steam_boiler_tank_", MAT_NETHERITE       , 1209, 112),
			row("steam_boiler_tank_", MAT_TUNGSTEN        , 1207, 128),
			row("steam_boiler_tank_", MAT_TUNGSTENSTEEL   , 1208, 128),
			row("steam_boiler_tank_", MAT_ULTIMET         , 1212, 256));

	/** The 13 Strong rows (:567-579, 64..1024 × STEAM_PER_EU). */
	public static final List<BoilerRow> STRONG_BOILER_ROWS = List.of(
			row("strong_steam_boiler_tank_", MAT_LEAD            , 1250,  64),
			row("strong_steam_boiler_tank_", MAT_BISMUTH         , 1251,  80),
			row("strong_steam_boiler_tank_", MAT_BRONZE          , 1252,  96),
			row("strong_steam_boiler_tank_", MAT_ARSENIC_COPPER  , 1260,  96),
			row("strong_steam_boiler_tank_", MAT_ARSENIC_BRONZE  , 1261, 112),
			row("strong_steam_boiler_tank_", MAT_INVAR           , 1253,  64),
			row("strong_steam_boiler_tank_", MAT_STEEL           , 1254, 128),
			row("strong_steam_boiler_tank_", MAT_CHROMIUM        , 1255, 384),
			row("strong_steam_boiler_tank_", MAT_TITANIUM        , 1256, 448),
			row("strong_steam_boiler_tank_", MAT_NETHERITE       , 1259, 448),
			row("strong_steam_boiler_tank_", MAT_TUNGSTEN        , 1257, 512),
			row("strong_steam_boiler_tank_", MAT_TUNGSTENSTEEL   , 1258, 512),
			row("strong_steam_boiler_tank_", MAT_ULTIMET         , 1262, 1024));

	/** A row builder — the NBT_OUTPUT_SU value is {@code aRawOutput * STEAM_PER_EU} (the loader's CS.java:240 = 2). */
	private static BoilerRow row(String aPathPrefix, BoilerMaterial aMat, int aMetaId, long aRawOutput) {
		return new BoilerRow(aPathPrefix + aMat.slug(),
				aMetaId, aRawOutput * GTFluids.STEAM_PER_EU, aMat, aPathPrefix.startsWith("strong"));
	}

	/** All 26 rows in registration order: the standard 13, then the Strong 13. */
	public static List<BoilerRow> allRows() {
		List<BoilerRow> rRows = new ArrayList<>(BOILER_ROWS.size() + STRONG_BOILER_ROWS.size());
		rRows.addAll(BOILER_ROWS);
		rRows.addAll(STRONG_BOILER_ROWS);
		return rRows;
	}

	/** The registered blocks by path (the BET/datagen/loot walkers + /gt6boiler place iterate this). */
	public static final Map<String, RegistryObject<BoilerTankBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (BoilerRow tRow : allRows()) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new BoilerTankBlock(tRow, tRow.properties())));
			// the GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read forward-reference form (the P6 lambda lesson)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Boilers.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The block list in registration order (the BET multi-mount array + the loot/datagen walkers). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<BoilerTankBlock> tBlock : BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for /gt6boiler place — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<BoilerTankBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the block carrier — the SteamEngineBlock shape + the two removal faces
	// -------------------------------------------------------------------------

	/**
	 * The boiler block — the facing cube carrier over the shared BET: the FACING is the
	 * FRONT (the barometer face, :240/:246-247), the row (output) rides the instance, no
	 * click face (no GUI by census :103). The pressurised-removal faces of the explosion
	 * family live here: playerWillDestroy (the removedByPlayer :202-205 counterpart) and
	 * onBlockExploded (the :208-211 second-blast face). NO onRemove override (the card
	 * red line — the explosion goes through the block event faces).
	 */
	public static final class BoilerTankBlock extends GTEntityBlock {

		/** Facing property (horizontal — the FRONT = the barometer face). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		private final BoilerRow mRow;

		public BoilerTankBlock(BoilerRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends BoilerTankBlock> codec() {
			return simpleCodec(aProperties -> new BoilerTankBlock(BOILER_ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the GTBarrelBlock.capacityL carrier read). */
		public BoilerRow row() {
			return mRow;
		}

		/** The composed boiler name (task p20-i18n-compose-rows): the {@link GT6Boilers#displayOf} carrier. */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return displayOf(mRow);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			// the front (barometer face) TOWARDS the placer — the GT6PlacementFacing canon
			// (task p28-singleblock-facing-canon)
			return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GTBlockEntities.BOILER_TANK_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler) {
				tBoiler.setFacingFromPlacement(tPlayer); // the FRONT (barometer) mirror
			}
		}

		/**
		 * Upstream :202-205 removedByPlayer — the pressurised-dismantle trigger: barometer
		 * &gt; 4 while a NON-creative player breaks it → explode(T) (instant; the root
		 * explode body destroys the block and detonates). Creative players and sub-4
		 * pressure just break it. This is the pre-removal player face, NOT an onRemove
		 * override (the card red line).
		 */
		//? if forge {
		@Override
		public void playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
			super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
			if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler) {
				tBoiler.dismantle(aPlayer); // :203 — the creative check inside
			}
		}
		//?} else {
		/*@Override
		public BlockState playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
		//21.1: BlockBehaviour.playerWillDestroy returns BlockState (void on 1.20.1, javap
		//Block 21.1.249) — the override return type drifts; the tail hands back the state
		//untouched (vanilla TntBlock return-shape, no state swap on this path).
			super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
			if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler) {
				tBoiler.dismantle(aPlayer); // :203 — the creative check inside
			}
			return aState;
		}
		*///?}

		/**
		 * Upstream :208-211 onExploded — the caught-in-a-neighbour's-explosion second blast.
		 * The Forge explosion face (IForgeBlock.java:716-720): the default body swaps the air
		 * FIRST (removing the BE with it), so the BE consult happens here BEFORE the air
		 * swap. The chained explode(T) may cascade into further boilers — the upstream
		 * semantics verbatim.
		 */
		@Override
		public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
			if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof GTBoilerTankBlockEntity tBoiler) {
				tBoiler.onExploded(); // :210
			}
			aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3); // the IForgeBlock default body
		}
	}

	private GT6Boilers() {}

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
	}

	/**
	 * The tab walk (task p38-tabfix-a-multiblock — the whole {@link #ITEMS_BY_PATH} family
	 * joins the multiblocks tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what delivers
	 * this handler). JEI derives its item list from the tab display items.
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMultiBlocks.MULTIBLOCKS_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
