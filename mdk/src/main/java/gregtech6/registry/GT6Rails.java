package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.block.rail.GT6BoosterRailBlock;
import gregtech6.block.rail.GT6DetectorRailBlock;
import gregtech6.block.rail.GT6RailBlock;
import gregtech6.block.rail.GT6RoadRailBlock;

/**
 * The rail registration home (task p35-rails-31-blocks) — the GT6Sensors shape: a
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister pair attached from the
 * construct event. 31 rows = the Road Stripe + the 10 rail materials x 3 classes
 * (normal/booster/detector), the upstream Loader_Rails.java:39-72 registration order
 * verbatim (road, then Al..Ad normal, Al..Ad booster, Al..Ad detector). The per-row
 * columns are the loader lines: {@code aSpeed} (the 0.20F..4.00F ladder) and
 * {@code aExplosionResistance} (6..100); the hardness column is the vanilla rail 0.7F
 * (the upstream {@code getBlockHardness = Blocks.rail} fold, BlockBaseRail.java:97) and
 * the Road Stripe's half face (BlockRailRoad.java:137 = {@code / 2} → 0.35F).
 *
 * <p>Speed ladder: every class overrides the per-rail {@code getRailMaxSpeed} hook —
 * census-verified on BOTH legs (Forge 1.20.1 IForgeBaseRailBlock.java:62 default +
 * AbstractMinecart.patch:240 call site; NeoForge 21.1 IBaseRailBlockExtension
 * getRailMaxSpeed default + AbstractMinecart.getMaxSpeedWithRail call site), so the
 * upstream ladder (BlockBaseRail.java:278-289) is a DIRECT translation — the long-straight
 * run grants the full {@code aSpeed}, everything else (curves, slopes, isolated segments)
 * falls back to the vanilla 0.4F cap. The per-cart rail speed cap (1.2 default on both
 * legs, IAbstractMinecartExtension.getMaxCartSpeedOnRail) clamps on top exactly as the
 * 1.7.10 Forge stack did — Adamantium's 4.00F manifests as 1.2 on vanilla carts, a
 * platform constant, not a port deviation.
 *
 * <p>No creative tab (the sensors precedent — /give-reachable, the tab system is the pool
 * card); no harvest-level layer (the repo has none — the GTCrowbarItem javadoc ruling);
 * the crowbar mining arm reaches the family through the vanilla rails block tag
 * ({@code mineable/pickaxe} nests {@code #minecraft:rails} on both legs).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Rails {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The vanilla-rail fallback speed (upstream {@code Math.min(mSpeed, 0.4F)} tail). */
	public static final float VANILLA_RAIL_SPEED = 0.4F;

	/** The Road Stripe's top speed (Loader_Rails.java:39 {@code 0.50F}). */
	public static final float ROAD_SPEED = 0.50F;

	/** The vanilla rail hardness both classes ride (upstream Blocks.rail fold, BlockBaseRail.java:97). */
	public static final float RAIL_HARDNESS = 0.7F;

	/** The three material classes — the loader column pairs (T, F) / (T, T)... the boolean pair folded. */
	public enum RailKind {
		NORMAL, BOOSTER, DETECTOR
	}

	/**
	 * One loader line: the block-carrier projection of the :39-72 columns. The material is
	 * carried as the LADDER INDEX (0..9) and resolved at CALL time — the class loads from
	 * the FML automatic-subscriber scan (the GT6Sensors shape) BEFORE the gregapi material
	 * registry initialises, so an eager {@code MT.*} read in the clinit NPEs (the
	 * GTMaterialItems late-binding precedent).
	 */
	public record RailRow(String path, String display, int materialIndex, RailKind kind,
			float speed, float resistance) {

		/** The row material — resolved at call time, never in the clinit. */
		public OreDictMaterial material() {
			return materials().get(materialIndex);
		}

		/** The composed display key ({@code block.gt6.<path>}, the vanilla BlockItem naming). */
		public String displayKey() {
			return "block.gt6." + path();
		}
	}

	/**
	 * The 10 rail materials, the loader line order Al..Ad (Loader_Rails.java:41-50) — a
	 * METHOD: every read happens after the material registry initialisation (the clinit
	 * wall above).
	 */
	public static List<OreDictMaterial> materials() {
		return List.of(
				MT.Al, MT.Bronze, MT.Magnalium, MT.Steel, MT.StainlessSteel,
				MT.W, MT.Ti, MT.TungstenSteel, MT.TungstenCarbide, MT.Ad);
	}

	/** The {@code aSpeed} ladder, same index space (Loader_Rails.java:41-50). */
	public static final float[] SPEEDS = {0.20F, 0.30F, 0.60F, 0.60F, 0.80F, 1.00F, 1.20F, 1.40F, 1.60F, 4.00F};

	/** The {@code aExplosionResistance} column, same index space (Loader_Rails.java:41-50). */
	public static final float[] RESISTANCES = {6.0F, 8.0F, 12.0F, 12.0F, 10.0F, 20.0F, 16.0F, 20.0F, 24.0F, 100.0F};

	/** The registry-path slugs (the upstream ids gt.block.rail[.booster/.detector].&lt;slug&gt; snake-folded). */
	public static final String[] SLUGS = {"aluminium", "bronze", "magnalium", "steel", "stainlesssteel",
			"tungsten", "titanium", "tungstensteel", "tungstencarbide", "adamantium"};

	/** The display words (the upstream local names verbatim — the loader string column). */
	public static final String[] DISPLAY_WORDS = {"Aluminium", "Bronze", "Magnalium", "Steel", "Stainless Steel",
			"Tungsten", "Titanium", "Tungstensteel", "Tungstencarbide", "Adamantium"};

	/** The 30 material rows, in the upstream registration order: normals, then boosters, then detectors. */
	public static final List<RailRow> ROWS = buildRows();

	private static List<RailRow> buildRows() {
		List<RailRow> rRows = new ArrayList<>();
		for (RailKind tKind : RailKind.values()) {
			for (int i = 0; i < SLUGS.length; i++) {
				String tPrefix = tKind == RailKind.NORMAL ? "rail_" : "rail_" + tKind.name().toLowerCase() + "_";
				String tDisplay = tKind == RailKind.NORMAL
						? DISPLAY_WORDS[i] + " Track"
						: DISPLAY_WORDS[i] + " " + tKind.name().charAt(0) + tKind.name().substring(1).toLowerCase() + " Track";
				rRows.add(new RailRow(tPrefix + SLUGS[i], tDisplay, i, tKind, SPEEDS[i], RESISTANCES[i]));
			}
		}
		return rRows;
	}

	/** The Road Stripe row-equivalent (the :39 line — outside the material walk). */
	public static final String ROAD_PATH = "rail_road";
	public static final float ROAD_RESISTANCE = 20.0F;

	/** The blocks/BlockItems, one pair per row (the GT6Sensors static-block form). */
	public static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();
	public static final RegistryObject<Block> ROAD_BLOCK = BLOCKS.register(ROAD_PATH,
			() -> new GT6RoadRailBlock(BlockBehaviour.Properties.of()
					.strength(RAIL_HARDNESS / 2.0F, ROAD_RESISTANCE).sound(SoundType.METAL).noCollission()));
	public static final RegistryObject<Item> ROAD_ITEM = ITEMS.register(ROAD_PATH,
			() -> new BlockItem(ROAD_BLOCK.get(), new Item.Properties()));

	static {
		for (RailRow tRow : ROWS) {
			final RailRow fRow = tRow;
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(), () -> railBlockOf(fRow)));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GT6Rails.BLOCKS_BY_PATH.get(fRow.path()).get(), new Item.Properties())));
		}
	}

	/** The per-kind block construction (the loader boolean pair folded into the three classes). */
	private static Block railBlockOf(RailRow aRow) {
		BlockBehaviour.Properties tProps = BlockBehaviour.Properties.of()
				.strength(RAIL_HARDNESS, aRow.resistance()).sound(SoundType.METAL).noCollission();
		return switch (aRow.kind()) {
			case NORMAL -> new GT6RailBlock(aRow.speed(), tProps);
			case BOOSTER -> new GT6BoosterRailBlock(aRow.speed(), tProps);
			case DETECTOR -> new GT6DetectorRailBlock(aRow.speed(), tProps);
		};
	}

	private GT6Rails() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Sensors shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Attachments fork).
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Sensors.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 rails registered: {} material rows + the road stripe ({} / {} blocks valid)",
				ROWS.size(), BLOCKS_BY_PATH.size(), ITEMS_BY_PATH.size()));
	}

	// ------------------------------------------------- the speed ladder (the BlockBaseRail.java:278-289 direct translation)

	/**
	 * The pure ladder core — the offline pin seam. A long straight run of GT6 rails grants
	 * the full material speed; the chunk-loaded guard's cold arm holds the 1.0F bleed cap
	 * (upstream {@code doChunksNearChunkExist(x, y, z, 17)}); every other shape (curves,
	 * slopes, isolated segments) falls back to the vanilla {@link #VANILLA_RAIL_SPEED}.
	 */
	public static float ladderSpeed(float aSpeed, boolean aStraightRun, boolean aChunksLoaded) {
		if (aStraightRun) return aChunksLoaded ? aSpeed : Math.min(aSpeed, 1.0F);
		return Math.min(aSpeed, VANILLA_RAIL_SPEED);
	}

	/** The stateful half — shape + neighbours + the 17-chunk loaded guard, then the pure core. */
	public static float ladderSpeed(float aSpeed, BlockState aState, Level aLevel, BlockPos aPos) {
		return ladderSpeed(aSpeed, isLongStraightRun(aState, aLevel, aPos), hasLadderChunks(aLevel, aPos));
	}

	/**
	 * The straight-run test: this rail is flat straight AND both in-axis neighbours are GT6
	 * rails of the SAME straight shape (the upstream {@code instanceof BlockBaseRail &&
	 * (meta & 7) == shape} pair). Vanilla rails do NOT qualify — the ladder is the GT6
	 * family's own.
	 */
	public static boolean isLongStraightRun(BlockState aState, Level aLevel, BlockPos aPos) {
		RailShape tShape = ((BaseRailBlock) aState.getBlock()).getRailDirection(aState, aLevel, aPos, null);
		if (tShape == RailShape.NORTH_SOUTH) {
			return isSameStraightAt(aLevel, aPos.north(), tShape) && isSameStraightAt(aLevel, aPos.south(), tShape);
		}
		if (tShape == RailShape.EAST_WEST) {
			return isSameStraightAt(aLevel, aPos.east(), tShape) && isSameStraightAt(aLevel, aPos.west(), tShape);
		}
		return false;
	}

	/** One neighbour arm: a GT6 rail whose straight shape equals {@code aShape}. */
	private static boolean isSameStraightAt(Level aLevel, BlockPos aPos, RailShape aShape) {
		BlockState tState = aLevel.getBlockState(aPos);
		if (!isGT6Rail(tState.getBlock())) return false;
		return ((BaseRailBlock) tState.getBlock()).getRailDirection(tState, aLevel, aPos, null) == aShape;
	}

	/** The family test (the upstream {@code instanceof BlockBaseRail}): the four port classes, nothing else. */
	public static boolean isGT6Rail(Block aBlock) {
		return aBlock instanceof GT6RailBlock || aBlock instanceof GT6BoosterRailBlock
				|| aBlock instanceof GT6DetectorRailBlock || aBlock instanceof GT6RoadRailBlock;
	}

	/** The 17-radius loaded guard (the upstream {@code doChunksNearChunkExist(x, y, z, 17)}). */
	private static boolean hasLadderChunks(Level aLevel, BlockPos aPos) {
		return aLevel.hasChunksAt(aPos.offset(-17, -17, -17), aPos.offset(17, 17, 17));
	}
}
