package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

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
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.energy.generators.GTGeneratorFluidBedBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorGasBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorLiquidBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorSolidBlockEntity;

/**
 * The Burning Box family registration home (task p13-burning-box-family spec ⑦, the
 * GT6Kinetics self-contained-DR form): 97 blocks/items over FOUR shared BET rows —
 * the upstream hierarchy Solid←(Brick/Metal) + Liquid←Gas + FluidBed collapsed into
 * four BE classes and one block carrier class parameterized by {@link Family}.
 *
 * <p><b>The rows</b> (the Loader_MultiTileEntities.java:517-704 projection, upstream
 * file order, every {@code NBT_EFFICIENCY}/{@code NBT_OUTPUT}/{@code NBT_HARDNESS}
 * transcribed verbatim):
 * <ul>
 * <li>{@link #SOLID_ROWS} = 27 — the Brick row (:519, eff 2500, out 16, ID 1199), the
 *     13 Burning Box (Solid) rows (:522-534, IDs 1100-1112, eff 4500-10000, out
 *     16-256) and the 13 Dense rows (:536-548, IDs 1150-1162, out 64-1024). COUNT
 *     ERRATUM (the p12-engine-steam 28-vs-26 census precedent): the task card's
 *     acceptance says "26" Solid rows, but 1 + 13 + 13 = 27 — the Brick row the card's
 *     own spec ① lists is the one the arithmetic lost; the no-upstream-subset ruling
 *     keeps all 27.</li>
 * <li>{@link #LIQUID_ROWS} = 22 — the 11 Burning Box (Liquid) rows (:619-629, IDs
 *     1402-1412) and the 11 Dense rows (:633-643, IDs 1452-1462); the Pb/Bi rows
 *     :617-618/:631-632 are commented out upstream and NOT ported (the card ruling).</li>
 * <li>{@link #GAS_ROWS} = 22 — the 11 Burning Box (Gas) rows (:649-659, IDs
 *     1602-1612) and the 11 Dense rows (:663-673, IDs 1652-1662); every row carries
 *     NBT_FUELMAP FM.Burn (NOT FM.Gas — the card/spec ruling, the Loader is the
 *     evidence), same eff/out ladders as the Liquid family.</li>
 * <li>{@link #FLUIDBED_ROWS} = 26 — the 13 Fluidized Bed rows (:678-690, IDs
 *     9000-9012) and the 13 Dense rows (:692-704, IDs 9050-9062; Dense = 4× the
 *     normal output, 256-4096).</li>
 * </ul>
 *
 * <p>Material slugs follow the port registry conventions (GT6Kinetics AXLE_SPECS /
 * GTBarrels rows): Pb → lead, Bi → bismuth, ANY.Steel → steel, MT.Cr → chromium,
 * MT.Ti → titanium, MT.Ta4HfC5 → tantalum_hafnium_carbide (the GTBarrels row slug).
 * Hardness/resistance ride the row (upstream NBT_HARDNESS == NBT_RESISTANCE on every
 * burning-box row); the Brick row is the STONE-sound carrier (:519 the aStone block),
 * every metal row the METAL sound (the machine-block convention).
 *
 * <p>Creative tab: all 97 items join MACHINES_TAB via {@link #onBuildTabContents}
 * (issue #10 — JEI 1.20.1 derives its item list from the tab display items, so the
 * registered-but-tab-less family was invisible in BOTH the creative menu and JEI;
 * supersedes the old axle/diesel tab-less precedent). No GUI by census
 * (LH.NO_GUI_CLICK_TO_INVENTORY, the Solid-class doc); the row
 * (efficiency/rate) rides the BLOCK carrier and the BE reads it off the placed state.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BurningBoxes {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The family of a registration row — the BE class selector and the block behaviour carrier. */
	public enum Family {
		/** The FM.Furnace dust/anything-burnable family (MultiTileEntityGeneratorSolid/Brick/Metal). */
		SOLID,
		/** The FM.Burn liquid-fuel family (MultiTileEntityGeneratorLiquid). */
		LIQUID,
		/** The FM.Burn gas-fuel family (MultiTileEntityGeneratorGas extends Liquid, :38). */
		GAS,
		/** The FM.FluidBed dust+calcite family (MultiTileEntityGeneratorFluidBed). */
		FLUIDBED
	}

	/** The composed Solid/Liquid/Gas display template "{@code Burning Box (%s, %s)}" — family + material slots (task p20-i18n-compose-rows). */
	public static final String DISPLAY_KEY = "gt6.row.burning_box.display";
	/** The Dense form of the Solid/Liquid/Gas template — the Dense wording rides the template. */
	public static final String DISPLAY_DENSE_KEY = "gt6.row.burning_box.display.dense";
	/** The Fluidized Bed template "{@code Fluidized Bed Burning Box (%s)}" — material slot only (the upstream bracket has no family word). */
	public static final String DISPLAY_FLUIDBED_KEY = "gt6.row.burning_box.display.fluidbed";
	/** The Dense Fluidized Bed template. */
	public static final String DISPLAY_FLUIDBED_DENSE_KEY = "gt6.row.burning_box.display.fluidbed_dense";
	/** The family-word unit keys (Solid/Liquid/Gas — slot-less nouns the templates consume). */
	public static final String FAMILY_SOLID_UNIT_KEY = "gt6.row.burning_box.family.solid";
	public static final String FAMILY_LIQUID_UNIT_KEY = "gt6.row.burning_box.family.liquid";
	public static final String FAMILY_GAS_UNIT_KEY = "gt6.row.burning_box.family.gas";

	/** The row's display template key (the family split; Dense rides the path prefix). */
	public static String displayKeyOf(BurningBoxRow aRow) {
		boolean tDense = aRow.path().startsWith("dense_");
		return switch (aRow.family()) {
			case FLUIDBED -> tDense ? DISPLAY_FLUIDBED_DENSE_KEY : DISPLAY_FLUIDBED_KEY;
			default -> tDense ? DISPLAY_DENSE_KEY : DISPLAY_KEY;
		};
	}

	/** The family-word unit key of a row (null-family rows — the Brick atom — never compose). */
	public static String familyUnitKeyOf(BurningBoxRow aRow) {
		return switch (aRow.family()) {
			case SOLID -> FAMILY_SOLID_UNIT_KEY;
			case LIQUID -> FAMILY_LIQUID_UNIT_KEY;
			case GAS -> FAMILY_GAS_UNIT_KEY;
			case FLUIDBED -> null;
		};
	}

	/** The row's material small-unit key. */
	public static String matUnitKeyOf(BurningBoxRow aRow) {
		return "gt6.row.mat." + aRow.material().slug();
	}

	/** The composed name of a burning-box row (the pure compose seam; the Brick atom keeps its atomic key). */
	public static net.minecraft.network.chat.MutableComponent displayOf(BurningBoxRow aRow) {
		net.minecraft.network.chat.Component tMat = net.minecraft.network.chat.Component.translatable(matUnitKeyOf(aRow));
		if (aRow.family() == Family.FLUIDBED) {
			return net.minecraft.network.chat.Component.translatable(displayKeyOf(aRow), tMat);
		}
		return net.minecraft.network.chat.Component.translatable(displayKeyOf(aRow),
				net.minecraft.network.chat.Component.translatable(familyUnitKeyOf(aRow)), tMat);
	}

	/**
	 * One Loader material — slug + display name + the NBT_HARDNESS (== NBT_RESISTANCE)
	 * pair + the row's NBT_MATERIAL (task p27-machine-material-tint-fidelity): every
	 * upstream burning-box row carries it (Loader_MultiTileEntities.java:519-548/:619-704)
	 * and the 1.7.10 registration derives the render colour from it
	 * (MultiTileEntityClassContainer.java:51, {@code getRGBInt(material.fRGBaSolid)}).
	 * A LAZY supplier per the GTBarrels MetalDrumRow convention (GTWireSpecs.java:35
	 * ruling — the registry classes load before {@code MT.init()}).
	 */
	public record BoxMaterial(String slug, String display, float hardness, java.util.function.Supplier<gregapi.oredict.OreDictMaterial> mat) {}

	/** The 13 burning-box materials, the GTBarrels/GT6Kinetics slug conventions (the mat column = the Loader {@code aMat} per row). */
	public static final BoxMaterial
			MAT_LEAD        = new BoxMaterial("lead"                   , "Lead"                  ,  4.0F, () -> gregapi.data.MT.Pb),
			MAT_BISMUTH     = new BoxMaterial("bismuth"                , "Bismuth"               ,  4.0F, () -> gregapi.data.MT.Bi),
			MAT_BRONZE      = new BoxMaterial("bronze"                 , "Bronze"                ,  7.0F, () -> gregapi.data.MT.Bronze),
			MAT_ARSENIC_COPPER  = new BoxMaterial("arsenic_copper"     , "Arsenic Copper"        ,  7.0F, () -> gregapi.data.MT.ArsenicCopper),
			MAT_ARSENIC_BRONZE  = new BoxMaterial("arsenic_bronze"     , "Arsenic Bronze"        ,  7.0F, () -> gregapi.data.MT.ArsenicBronze),
			MAT_INVAR       = new BoxMaterial("invar"                  , "Invar"                 ,  4.0F, () -> gregapi.data.MT.Invar),
			MAT_STEEL       = new BoxMaterial("steel"                  , "Steel"                 ,  6.0F, () -> gregapi.data.MT.Steel),
			MAT_CHROMIUM    = new BoxMaterial("chromium"               , "Chromium"              ,  4.0F, () -> gregapi.data.MT.Cr),
			MAT_TITANIUM    = new BoxMaterial("titanium"               , "Titanium"              ,  9.0F, () -> gregapi.data.MT.Ti),
			MAT_NETHERITE   = new BoxMaterial("netherite"              , "Netherite"             ,  9.0F, () -> gregapi.data.MT.Netherite),
			MAT_TUNGSTEN    = new BoxMaterial("tungsten"               , "Tungsten"              , 10.0F, () -> gregapi.data.MT.W),
			MAT_TUNGSTENSTEEL = new BoxMaterial("tungstensteel"        , "Tungstensteel"         , 12.5F, () -> gregapi.data.MT.TungstenSteel),
			MAT_TANTALUM_HAFNIUM_CARBIDE = new BoxMaterial("tantalum_hafnium_carbide", "Ta4HfC5", 12.5F, () -> gregapi.data.MT.Ta4HfC5);

	/** One registration row — the block-carrier projection of one upstream aRegistry.add line (the display column retired into the composed-name parameters, task p20-i18n-compose-rows). */
	public record BurningBoxRow(String path, short efficiency, long rate, Family family, BoxMaterial material, boolean stone) {
		/** The block properties (hardness == resistance on every row; the Brick row the STONE sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(material.hardness(), material.hardness())
					.sound(stone() ? SoundType.STONE : SoundType.METAL);
		}
	}

	// the row ladders — the Loader file order, values verbatim (class doc)

	/** The Brick row (:519, ID 1199) — eff 2500, out 16 HU/t, the aStone carrier; NBT_MATERIAL = MT.Brick (:519). */
	public static final BurningBoxRow BRICK_ROW = new BurningBoxRow("brick_burning_box", (short)2500, 16, Family.SOLID,
			new BoxMaterial("brick", "Brick", 6.0F, () -> gregapi.data.MT.Brick), true);

	/** The 13 Burning Box (Solid) rows (:522-534) + the 13 Dense rows (:536-548) — 27 with the Brick row. */
	public static final List<BurningBoxRow> SOLID_ROWS = buildLadder(Family.SOLID,
			// :522-534 — the normal ladder (IDs 1100-1112)
			row("burning_box_solid_", MAT_LEAD               , (short) 5000,  16),
			row("burning_box_solid_", MAT_BISMUTH            , (short) 4500,  20),
			row("burning_box_solid_", MAT_BRONZE             , (short) 7500,  24),
			row("burning_box_solid_", MAT_ARSENIC_COPPER    , (short) 8000,  24),
			row("burning_box_solid_", MAT_ARSENIC_BRONZE    , (short) 9000,  28),
			row("burning_box_solid_", MAT_INVAR             , (short)10000,  16),
			row("burning_box_solid_", MAT_STEEL             , (short) 7000,  32),
			row("burning_box_solid_", MAT_CHROMIUM          , (short) 8500, 112),
			row("burning_box_solid_", MAT_TITANIUM          , (short) 8500,  96),
			row("burning_box_solid_", MAT_NETHERITE         , (short) 9000,  96),
			row("burning_box_solid_", MAT_TUNGSTEN          , (short)10000, 128),
			row("burning_box_solid_", MAT_TUNGSTENSTEEL     , (short) 9000, 128),
			row("burning_box_solid_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000, 256),
			// :536-548 — the Dense ladder (IDs 1150-1162, the ×4 output)
			row("dense_burning_box_solid_", MAT_LEAD               , (short) 5000,  64),
			row("dense_burning_box_solid_", MAT_BISMUTH            , (short) 4500,  80),
			row("dense_burning_box_solid_", MAT_BRONZE             , (short) 7500,  96),
			row("dense_burning_box_solid_", MAT_ARSENIC_COPPER    , (short) 8000,  96),
			row("dense_burning_box_solid_", MAT_ARSENIC_BRONZE    , (short) 9000, 112),
			row("dense_burning_box_solid_", MAT_INVAR             , (short)10000,  64),
			row("dense_burning_box_solid_", MAT_STEEL             , (short) 7000, 128),
			row("dense_burning_box_solid_", MAT_CHROMIUM          , (short) 8500, 448),
			row("dense_burning_box_solid_", MAT_TITANIUM          , (short) 8500, 384),
			row("dense_burning_box_solid_", MAT_NETHERITE         , (short) 9000, 384),
			row("dense_burning_box_solid_", MAT_TUNGSTEN          , (short)10000, 512),
			row("dense_burning_box_solid_", MAT_TUNGSTENSTEEL     , (short) 9000, 512),
			row("dense_burning_box_solid_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000,1024));

	/** The 11+11 Liquid rows (:619-629 + :633-643); the Pb/Bi rows :617-618/:631-632 stay commented-out upstream. */
	public static final List<BurningBoxRow> LIQUID_ROWS = buildLadder(Family.LIQUID,
			row("burning_box_liquid_", MAT_BRONZE            , (short) 7500,  24),
			row("burning_box_liquid_", MAT_ARSENIC_COPPER    , (short) 8000,  24),
			row("burning_box_liquid_", MAT_ARSENIC_BRONZE    , (short) 9000,  28),
			row("burning_box_liquid_", MAT_INVAR             , (short)10000,  16),
			row("burning_box_liquid_", MAT_STEEL             , (short) 7000,  32),
			row("burning_box_liquid_", MAT_CHROMIUM          , (short) 8500, 112),
			row("burning_box_liquid_", MAT_TITANIUM          , (short) 8500,  96),
			row("burning_box_liquid_", MAT_NETHERITE         , (short) 9000,  96),
			row("burning_box_liquid_", MAT_TUNGSTEN          , (short)10000, 128),
			row("burning_box_liquid_", MAT_TUNGSTENSTEEL     , (short) 9000, 128),
			row("burning_box_liquid_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000, 256),
			row("dense_burning_box_liquid_", MAT_BRONZE            , (short) 7500,  96),
			row("dense_burning_box_liquid_", MAT_ARSENIC_COPPER    , (short) 8000,  96),
			row("dense_burning_box_liquid_", MAT_ARSENIC_BRONZE    , (short) 9000, 112),
			row("dense_burning_box_liquid_", MAT_INVAR             , (short)10000,  64),
			row("dense_burning_box_liquid_", MAT_STEEL             , (short) 7000, 128),
			row("dense_burning_box_liquid_", MAT_CHROMIUM          , (short) 8500, 448),
			row("dense_burning_box_liquid_", MAT_TITANIUM          , (short) 8500, 384),
			row("dense_burning_box_liquid_", MAT_NETHERITE         , (short) 9000, 384),
			row("dense_burning_box_liquid_", MAT_TUNGSTEN          , (short)10000, 512),
			row("dense_burning_box_liquid_", MAT_TUNGSTENSTEEL     , (short) 9000, 512),
			row("dense_burning_box_liquid_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000,1024));

	/** The 11+11 Gas rows (:649-659 + :663-673) — NBT_FUELMAP FM.Burn on EVERY row (the FM.Gas ruling). */
	public static final List<BurningBoxRow> GAS_ROWS = buildLadder(Family.GAS,
			row("burning_box_gas_", MAT_BRONZE            , (short) 7500,  24),
			row("burning_box_gas_", MAT_ARSENIC_COPPER    , (short) 8000,  24),
			row("burning_box_gas_", MAT_ARSENIC_BRONZE    , (short) 9000,  28),
			row("burning_box_gas_", MAT_INVAR             , (short)10000,  16),
			row("burning_box_gas_", MAT_STEEL             , (short) 7000,  32),
			row("burning_box_gas_", MAT_CHROMIUM          , (short) 8500, 112),
			row("burning_box_gas_", MAT_TITANIUM          , (short) 8500,  96),
			row("burning_box_gas_", MAT_NETHERITE         , (short) 9000,  96),
			row("burning_box_gas_", MAT_TUNGSTEN          , (short)10000, 128),
			row("burning_box_gas_", MAT_TUNGSTENSTEEL     , (short) 9000, 128),
			row("burning_box_gas_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000, 256),
			row("dense_burning_box_gas_", MAT_BRONZE            , (short) 7500,  96),
			row("dense_burning_box_gas_", MAT_ARSENIC_COPPER    , (short) 8000,  96),
			row("dense_burning_box_gas_", MAT_ARSENIC_BRONZE    , (short) 9000, 112),
			row("dense_burning_box_gas_", MAT_INVAR             , (short)10000,  64),
			row("dense_burning_box_gas_", MAT_STEEL             , (short) 7000, 128),
			row("dense_burning_box_gas_", MAT_CHROMIUM          , (short) 8500, 448),
			row("dense_burning_box_gas_", MAT_TITANIUM          , (short) 8500, 384),
			row("dense_burning_box_gas_", MAT_NETHERITE         , (short) 9000, 384),
			row("dense_burning_box_gas_", MAT_TUNGSTEN          , (short)10000, 512),
			row("dense_burning_box_gas_", MAT_TUNGSTENSTEEL     , (short) 9000, 512),
			row("dense_burning_box_gas_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000,1024));

	/** The 13+13 Fluidized Bed rows (:678-690 + :692-704); the Dense ladder is ×4 (256-4096). */
	public static final List<BurningBoxRow> FLUIDBED_ROWS = buildLadder(Family.FLUIDBED,
			row("burning_box_fluidbed_", MAT_LEAD               , (short) 5000,  64),
			row("burning_box_fluidbed_", MAT_BISMUTH            , (short) 4500,  80),
			row("burning_box_fluidbed_", MAT_BRONZE             , (short) 7500,  96),
			row("burning_box_fluidbed_", MAT_ARSENIC_COPPER    , (short) 8000,  96),
			row("burning_box_fluidbed_", MAT_ARSENIC_BRONZE    , (short) 9000, 112),
			row("burning_box_fluidbed_", MAT_INVAR             , (short)10000,  64),
			row("burning_box_fluidbed_", MAT_STEEL             , (short) 7000, 128),
			row("burning_box_fluidbed_", MAT_CHROMIUM          , (short) 8500, 448),
			row("burning_box_fluidbed_", MAT_TITANIUM          , (short) 8500, 384),
			row("burning_box_fluidbed_", MAT_NETHERITE         , (short) 9000, 384),
			row("burning_box_fluidbed_", MAT_TUNGSTEN          , (short)10000, 512),
			row("burning_box_fluidbed_", MAT_TUNGSTENSTEEL     , (short) 9000, 512),
			row("burning_box_fluidbed_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000,1024),
			row("dense_burning_box_fluidbed_", MAT_LEAD               , (short) 5000,  256),
			row("dense_burning_box_fluidbed_", MAT_BISMUTH            , (short) 4500,  320),
			row("dense_burning_box_fluidbed_", MAT_BRONZE             , (short) 7500,  384),
			row("dense_burning_box_fluidbed_", MAT_ARSENIC_COPPER    , (short) 8000,  384),
			row("dense_burning_box_fluidbed_", MAT_ARSENIC_BRONZE    , (short) 9000,  448),
			row("dense_burning_box_fluidbed_", MAT_INVAR             , (short)10000,  256),
			row("dense_burning_box_fluidbed_", MAT_STEEL             , (short) 7000,  512),
			row("dense_burning_box_fluidbed_", MAT_CHROMIUM          , (short) 8500, 1792),
			row("dense_burning_box_fluidbed_", MAT_TITANIUM          , (short) 8500, 1536),
			row("dense_burning_box_fluidbed_", MAT_NETHERITE         , (short) 9000, 1536),
			row("dense_burning_box_fluidbed_", MAT_TUNGSTEN          , (short)10000, 2048),
			row("dense_burning_box_fluidbed_", MAT_TUNGSTENSTEEL     , (short) 9000, 2048),
			row("dense_burning_box_fluidbed_", MAT_TANTALUM_HAFNIUM_CARBIDE, (short)10000, 4096));

	/** A normal-ladder row builder (the row path closes with the material slug; the display is composed at getName time). */
	private static BurningBoxRow row(String aPathPrefix, BoxMaterial aMat, short aEfficiency, long aRate) {
		return new BurningBoxRow(aPathPrefix + aMat.slug(), aEfficiency, aRate, null, aMat, false);
	}

	/** The four ladders with the family stamped (the record is family-immutable after the build). */
	private static List<BurningBoxRow> buildLadder(Family aFamily, BurningBoxRow... aRows) {
		List<BurningBoxRow> rRows = new ArrayList<>(aRows.length);
		for (BurningBoxRow tRow : aRows) rRows.add(new BurningBoxRow(tRow.path(), tRow.efficiency(), tRow.rate(), aFamily, tRow.material(), tRow.stone()));
		return List.of(rRows.toArray(new BurningBoxRow[0]));
	}

	static {}

	/** All 97 rows in registration order: Brick + Solid(27) + Liquid(22) + Gas(22) + FluidBed(26). */
	public static List<BurningBoxRow> allRows() {
		List<BurningBoxRow> rRows = new ArrayList<>(1 + SOLID_ROWS.size() + LIQUID_ROWS.size() + GAS_ROWS.size() + FLUIDBED_ROWS.size());
		rRows.add(BRICK_ROW);
		rRows.addAll(SOLID_ROWS);
		rRows.addAll(LIQUID_ROWS);
		rRows.addAll(GAS_ROWS);
		rRows.addAll(FLUIDBED_ROWS);
		return rRows;
	}

	/** The registered blocks by path (the BET/datagen/loot walkers + /gt6burner place iterate this). */
	public static final Map<String, RegistryObject<BurningBoxBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (BurningBoxRow tRow : allRows()) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new BurningBoxBlock(tRow, tRow.properties())));
			// the GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read forward-reference form (the P6 lambda lesson)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6BurningBoxes.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The block list of one family in registration order (the BET multi-mount array walkers). */
	public static Block[] blockArray(Family aFamily) {
		List<BurningBoxRow> tRows = switch (aFamily) {
			case SOLID -> withBrick(SOLID_ROWS);
			case LIQUID -> LIQUID_ROWS;
			case GAS -> GAS_ROWS;
			case FLUIDBED -> FLUIDBED_ROWS;
		};
		Block[] rBlocks = new Block[tRows.size()];
		for (int i = 0; i < tRows.size(); i++) rBlocks[i] = BLOCKS_BY_PATH.get(tRows.get(i).path()).get();
		return rBlocks;
	}

	/** The Brick row joins the SOLID family BET (the same BE class, upstream :518-548 one section). */
	private static List<BurningBoxRow> withBrick(List<BurningBoxRow> aSolidRows) {
		List<BurningBoxRow> rRows = new ArrayList<>(1 + aSolidRows.size());
		rRows.add(BRICK_ROW);
		rRows.addAll(aSolidRows);
		return rRows;
	}

	/** The lookup for /gt6burner place — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<BurningBoxBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the block carrier — ONE class over the four families (the SteamEngineBlock form)
	// -------------------------------------------------------------------------

	/**
	 * The burning-box block — the facing cube carrier over the family BET (the
	 * GT6Kinetics.SteamEngineBlock shape): the FACING is the FRONT (the fuel/ignite
	 * face), the row (efficiency/rate) rides the instance, and the SOLID/FLUIDBED
	 * families open the front-click transfer on use (the upstream onBlockActivated3;
	 * the Liquid/GAS families have NO click face upstream — fluids move through the
	 * tanks' faces).
	 */
	public static final class BurningBoxBlock extends GTEntityBlock {

		/** Facing property (horizontal — the FRONT = the fuel/ignite face). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		private final BurningBoxRow mRow;

		public BurningBoxBlock(BurningBoxRow aRow, Properties aProperties) {
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
		protected com.mojang.serialization.MapCodec<? extends BurningBoxBlock> codec() {
			return simpleCodec(aProperties -> new BurningBoxBlock(SOLID_ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the GTBarrelBlock.capacityL carrier read). */
		public BurningBoxRow row() {
			return mRow;
		}

		/**
		 * The row material (task p27-machine-material-tint-fidelity) — the NBT_MATERIAL the
		 * upstream burning-box rows carry (:519-548/:619-704); the colour source the common
		 * {@code GTBasicMachineBlock.materialOf} dispatch hands the paint tint and the 03
		 * base unpaint(). The lazy supplier resolves against {@code MT.init()} at call time.
		 */
		public gregapi.oredict.OreDictMaterial material() {
			return mRow.material().mat().get();
		}

		/** The family (the BE selector + the use-face gate). */
		public Family family() {
			return mRow.family();
		}

		/** The composed burning-box name (task p20-i18n-compose-rows): the {@link GT6BurningBoxes#displayOf} carrier. */
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
			// the front (fuel/ignite face) TOWARDS the placer — the GT6PlacementFacing canon
			// (task p28-singleblock-facing-canon)
			return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return switch (mRow.family()) {
				case SOLID -> GTBlockEntities.BURNING_BOX_SOLID_BE.get();
				case LIQUID -> GTBlockEntities.BURNING_BOX_LIQUID_BE.get();
				case GAS -> GTBlockEntities.BURNING_BOX_GAS_BE.get();
				case FLUIDBED -> GTBlockEntities.BURNING_BOX_FLUIDBED_BE.get();
			};
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		//? if forge {
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (javap 21.1.249) — the
		//InteractionHand param dropped from the signature; the game loop drives the hands
		//in order and MAIN_HAND is the canonical first entry.
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			// the onBlockActivated3 :181 front gate — only the FRONT face reacts, and only
			// the Solid/FluidBed families have a click face (the Liquid/GAS families move
			// fluids through the tanks)
			if (mRow.family() != Family.SOLID && mRow.family() != Family.FLUIDBED) return InteractionResult.PASS;
			if (aHit.getDirection() != aState.getValue(FACING)) return InteractionResult.PASS;
			if (aLevel.getBlockEntity(aPos) instanceof GTGeneratorSolidBlockEntity tBox) {
				if (!aLevel.isClientSide) tBox.useOnFront(aPlayer, aHand);
				return InteractionResult.sidedSuccess(aLevel.isClientSide);
			}
			return InteractionResult.PASS;
		}


	}

	private GT6BurningBoxes() {}

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
	 * The tab walk (issue #10 — the whole {@link #ITEMS_BY_PATH} family joins the
	 * machines tab; the GT6Covers.onBuildTabContents verbatim form, the class-level
	 * MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what delivers this
	 * handler). JEI 1.20.1 derives its item list from the tab display items, so
	 * registered-but-tab-less was invisible in both the creative menu and JEI.
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
