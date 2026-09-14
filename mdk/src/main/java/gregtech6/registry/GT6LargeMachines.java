package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.fluid.FluidTankGT;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMapFurnace;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregtech6.tileentity.multiblocks.MultiBlockFluidHandler;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * The twelve W3 large machines (task p29-w3-large-12) — the
 * {@code gregtech/tileentity/multiblocks/MultiTileEntity<Centrifuge|Electrolyzer|...>}
 * family, Loader_MultiTileEntities.java:1229-1240, ported as ONE self-registration file
 * (the ADR-P3-4 GT6Boilers/GT6Tanks shape): one row record + one controller Block class +
 * one BlockEntityType mounting all twelve blocks (the MULTIBLOCK_PART_BE ADR-P3-1 form —
 * "one BET + per-type Blocks; new part blocks append to the valid list").
 *
 * <p><b>KJS surface declaration (card contract):</b> this card produces the REGISTRATION
 * face (12 controllers) + the STRUCTURE face (pattern-bound forming). ZERO new RecipeMaps —
 * all twelve rows reuse the W1/W2 in-catalogue maps (GT6RecipeMaps CENTRIFUGE/ELECTROLYZER/
 * COAGULATOR/AUTOCLAVE/BATH/MIXER/FERMENTER/FURNACE/SLUICE/CRUSHER/SHREDDER/SQUEEZER), so
 * tier-b has no new map_key; the controller crafting rows are CUT (the card-①
 * part-family precedent: every Loader :1229-1240 recipe needs 'R' IL.Processor_Crystal_Ruby
 * and 'C' OD_CIRCUITS[6], both ABSENT from the port item path — the absent-input pool,
 * GT6CraftingRecipes.java:880-886); no KubeJS special face.
 *
 * <p><b>The row table (Loader :1229-1240 verbatim columns):</b> path / display / metaId /
 * hardness / energy type / recipe map / the NBT_INPUT triple (in, min, max — every row
 * carries all three explicitly: the derived-form 512-window or the 1..16 TU window) /
 * NBT_PARALLEL / NBT_PARALLEL_DURATION / NBT_CHEAP_OVERCLOCKING / NBT_NO_CONSTANT_POWER /
 * NBT_EFFICIENCY / the auto-out face (SIDE_BOTTOM for eleven, SIDE_BACK for the
 * Fermenter) / the part-block paths (wall + inner + the Fermenter's transmitter base) /
 * the {@link StructureKind} (the upstream checkStructure2 geometry).
 *
 * <p><b>The six NO_CONSTANT_POWER rows</b> (Coagulator / Autoclave / Bath / Crusher /
 * Shredder / Squeezer) keep their progress through a power gap ({@code mNoConstantEnergy}
 * gates the doInactive :894 reset); the other six are constant-power and reset. The
 * efficiency rows (Centrifuge/Electrolyzer/Oven 2500 / Sluice/Crusher/Shredder/Squeezer
 * 5000) ride {@code units(minEnergy * duration, mEfficiency, 10000, T)} — 5000 = 2x the
 * required progress = half speed (the W1 units() ruling).
 *
 * <p><b>GUI face:</b> none — the machines run headless (the W2 rows' menu-null precedent;
 * GTMultiBlockControllerBlock has no use-face, so no MenuType is registered and the MUI
 * machine-GUI wave owns the eventual surface).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6LargeMachines {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The lazy recipe-map carrier — the maps register at mod construct, rows never touch them at class-init. */
	@FunctionalInterface
	public interface RecipeMapSupplier { RecipeMap get(); }

	/**
	 * One registration row — the projection of one Loader :1229-1240 line. The energy
	 * triple feeds {@link TileEntityBase10MultiBlockMachine.EnergyRowSpec} (the card-①
	 * carrier; the explicit MIN/MAX form, applied in the BE constructor — registration
	 * config, NOT persisted, the loadKeepsTheConstructorInjectedConfig contract).
	 *
	 * @param wallPath  the ring/wall part-block path ({@link GTMultiBlocks#anyPartBlock})
	 * @param innerPath the interior part block (wheels/blades/sluice parts/coils/parts); null = walls only
	 * @param basePath  the bottom-layer block (the Fermenter's heat transmitters); null = none
	 */
	public record LargeMachineRow(String path, String display, int metaId, float hardness, String texture,
			gregapi.code.TagData energyType, RecipeMapSupplier recipes,
			long nbtInput, long nbtInputMin, long nbtInputMax,
			int parallel, boolean parallelDuration, boolean cheapOverclocking,
			boolean noConstantPower, int efficiency, boolean autoOutBack,
			String wallPath, @Nullable String innerPath, @Nullable String basePath,
			StructureKind structure) {

		/** The card-① EnergyRowSpec this row rides (applied once in the BE constructor). */
		public TileEntityBase10MultiBlockMachine.EnergyRowSpec energySpec() {
			return new TileEntityBase10MultiBlockMachine.EnergyRowSpec(nbtInput, nbtInputMin, nbtInputMax, parallel, cheapOverclocking, parallelDuration);
		}

		/** The block properties (hardness == resistance per the NBT pair; the METAL sound). */
		public net.minecraft.world.level.block.state.BlockBehaviour.Properties properties() {
			return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(hardness, hardness).sound(SoundType.METAL);
		}
	}

	// -------------------------------------------------------------------------
	// the twelve rows (Loader :1229-1240, the line order)
	// -------------------------------------------------------------------------

	/** The VanillasGT-convention singleton-row offsets — none; every machine carries a hand row. */
	public static final List<LargeMachineRow> ROWS = List.of(
			// :1229 — aMat = MT.TungstenSteel; "CMC","RCR" crafting (CUT, the absent-input pool)
			new LargeMachineRow("large_centrifuge", "Large Centrifuge", 17100, 12.5F, "largecentrifuge",
					gregapi.data.TD.Energy.RU, () -> GT6RecipeMaps.CENTRIFUGE,
					512, 512, 4096, 16, true, true, false, 5000, false,
					"centrifuge_part", null, null, StructureKind.CENTRIFUGE),
			// :1230
			new LargeMachineRow("large_electrolyzer", "Large Electrolyzer", 17103, 6.0F, "largeelectrolyzer",
					gregapi.data.TD.Energy.EU, () -> GT6RecipeMaps.ELECTROLYZER,
					512, 512, 4096, 16, true, true, false, 5000, false,
					"electrolyzer_part", null, null, StructureKind.ELECTROLYZER),
			// :1231 — "Large Coagulator Array"
			new LargeMachineRow("large_coagulator", "Large Coagulator Array", 17105, 6.0F, "largecoagulator",
					gregapi.data.TD.Energy.TU, () -> GT6RecipeMaps.COAGULATOR,
					1, 1, 16, 64, false, false, true, 10000, false,
					"machine_wall_stainless_steel", null, null, StructureKind.BOX_5X5X2),
			// :1232 — the hollow sits ABOVE the controller (the shell centre one up)
			new LargeMachineRow("large_autoclave", "Large Autoclave", 17112, 6.0F, "largeautoclave",
					gregapi.data.TD.Energy.TU, () -> GT6RecipeMaps.AUTOCLAVE,
					1, 1, 16, 16, false, false, true, 10000, false,
					"dense_wall_stainless_steel", null, null, StructureKind.HOLLOW_3X3X3),
			// :1233 — "Large Bathing Vat"
			new LargeMachineRow("large_bath", "Large Bathing Vat", 17104, 6.0F, "largebath",
					gregapi.data.TD.Energy.TU, () -> GT6RecipeMaps.BATH,
					1, 1, 16, 64, false, false, true, 10000, false,
					"machine_wall_stainless_steel", null, null, StructureKind.BOX_5X5X2),
			// :1234 — "Large Batch Mixer"
			new LargeMachineRow("large_batch_mixer", "Large Batch Mixer", 17102, 6.0F, "largemixer",
					gregapi.data.TD.Energy.RU, () -> GT6RecipeMaps.MIXER,
					512, 512, 4096, 256, true, true, false, 10000, false,
					"machine_wall_stainless_steel", null, null, StructureKind.MIXER),
			// :1235 — the SIDE_BACK auto-out row; the 512/1/4096 window (MIN overridden to 1)
			new LargeMachineRow("large_fermenter", "Large Fermenter", 17113, 6.0F, "largefermenter",
					gregapi.data.TD.Energy.HU, () -> GT6RecipeMaps.FERMENTER,
					512, 1, 4096, 256, true, true, false, 10000, true,
					"machine_wall_stainless_steel", null, "heat_transmitter", StructureKind.FERMENTER),
			// :1236 — "Large Electric Oven" (MultiTileEntityOven; the two-coil middle ring)
			new LargeMachineRow("large_electric_oven", "Large Electric Oven", 17106, 6.0F, "largeoven",
					gregapi.data.TD.Energy.EU, () -> GT6RecipeMaps.FURNACE,
					512, 512, 4096, 64, true, true, false, 2500, false,
					"machine_wall_invar", "large_nichrome_coil", null, StructureKind.OVEN),
			// :1237
			new LargeMachineRow("large_sluice", "Large Sluice", 17107, 9.0F, "largesluice",
					gregapi.data.TD.Energy.RU, () -> GT6RecipeMaps.SLUICE,
					512, 512, 4096, 64, true, true, false, 5000, false,
					"machine_wall_titanium", "sluice_part", null, StructureKind.SLUICE),
			// :1238
			new LargeMachineRow("large_crusher", "Large Crusher", 17108, 12.5F, "largecrusher",
					gregapi.data.TD.Energy.RU, () -> GT6RecipeMaps.CRUSHER,
					512, 512, 4096, 64, true, true, true, 5000, false,
					"machine_wall_tungstensteel", "crusher_wheels", null, StructureKind.BASIN_WHEELS),
			// :1239
			new LargeMachineRow("large_shredder", "Large Shredder", 17109, 12.5F, "largeshredder",
					gregapi.data.TD.Energy.RU, () -> GT6RecipeMaps.SHREDDER,
					512, 512, 4096, 64, true, true, true, 5000, false,
					"machine_wall_tungstensteel", "shredder_blades", null, StructureKind.BASIN_WHEELS),
			// :1240 — aMat = ANY.Steel
			new LargeMachineRow("large_squeezer", "Large Squeezer", 17114, 6.0F, "largesqueezer",
					gregapi.data.TD.Energy.RU, () -> GT6RecipeMaps.SQUEEZER,
					512, 512, 4096, 64, true, true, true, 5000, false,
					"machine_wall_steel", null, null, StructureKind.BASIN_OPEN));

	public static final Map<String, LargeMachineRow> ROWS_BY_PATH;

	static {
		Map<String, LargeMachineRow> tMap = new LinkedHashMap<>();
		for (LargeMachineRow tRow : ROWS) tMap.put(tRow.path(), tRow);
		ROWS_BY_PATH = Map.copyOf(tMap);
	}

	/** The registered controller blocks by path (the BET valid list + the datagen/loot walkers + the chains). */
	public static final Map<String, RegistryObject<GTLargeMachineBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (LargeMachineRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTLargeMachineBlock(tRow, tRow.properties())));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The shared large-machine BET — one class mounting all twelve controller blocks
	 * (the MULTIBLOCK_PART_BE ADR-P3-1 form). Registry path mirrors
	 * {@code getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<GTLargeMachineBlockEntity>> LARGE_MACHINE_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_large_machine", () -> BlockEntityType.Builder.of(
					GTLargeMachineBlockEntity::new, blockArray()).build(null));

	/** The block list in registration order (the BET multi-mount array). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<GTLargeMachineBlock> tBlock : BLOCKS_BY_PATH.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/** The lookup for the chains/diagnostics — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GTLargeMachineBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent
	 * (the GT6Boilers/GTMultiBlocks precedent).
	 */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (the GTMachines fork precedent).
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	// -------------------------------------------------------------------------
	// the structure kinds — one constant per upstream checkStructure2 geometry
	// -------------------------------------------------------------------------

	/**
	 * The twelve structures collapse to NINE geometries (Centrifuge/Electrolyzer share the
	 * 3x3x2 shape, Coagulator/Bath the 5x5x2 box, Crusher/Shredder the wheel basin). Every
	 * {@code build} declares cells CONTROLLER-RELATIVE in WORLD axes — the upstream loops
	 * are world-axis tables conditioned on mFacing, and the facing enters only through the
	 * side-offset anchor ({@link GTMultiBlockPattern#anchorOffset}: the structure centre
	 * sits {@code -OFF[facing]} behind the controller, "Main Block centered on Side and
	 * facing outwards"); the BE walks them with {@code patternWalkFacing() == 0} (the
	 * crucible zero-offset form, TileEntityBase10MultiBlockBase javadoc), so declared
	 * offset == world offset from the controller and no per-cell rotation ever happens.
	 *
	 * <p>Facing-conditional structures (Fermenter vent, Sluice trough, Basin energy walls,
	 * Oven coil type) are per-facing PATTERN INSTANCES cached on the BE — the sanctioned
	 * "caller regenerates the pattern per evaluation" seam (GTMultiBlockPattern javadoc,
	 * the runtime-interpolation bullet). The mActive/mRunning design swings
	 * (Electrolyzer {@code mActive?2+rng(6):0}, Sluice/Basin {@code tD}) are FOLDED to
	 * their idle/static values: the ACTIVE design selection is the render wave's surface
	 * (the card exclusion), the design write is non-blocking for the check either way.
	 */
	public enum StructureKind {
		/** MultiTileEntityCentrifuge :49-76 — 3x3x2, per-position ring designs 1..8, the centre column ONLY_ENERGY_IN. */
		CENTRIFUGE {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				byte[][] tDesigns = {{1,2,3},{4,0,5},{6,7,8}}; // the (i,k) -> design table; 0 = the centre (energy)
				for (int j = 0; j <= 1; j++) for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					int tDesign = tDesigns[i + 1][k + 1];
					b.formingPart(i + anch[0], j + anch[1], k + anch[2], aWall,
							tDesign == 0 ? MultiBlockPartBlockEntity.ONLY_ENERGY_IN : MultiBlockPartBlockEntity.ONLY_ITEM_FLUID,
							tDesign);
				}
				return b.build();
			}
		},
		/** MultiTileEntityElectrolyzer :49-73 — 3x3x2, the bottom layer design 1 item+fluid+energy in, the top layer design 0 out. */
		ELECTROLYZER {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					b.formingPart(i + anch[0], anch[1], k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY_IN, 1);
					b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT, 0);
				}
				return b.build();
			}
		},
		/** MultiTileEntityMixer :46-77 — 3x3x2, the bottom layer OUT, the top layer IN, the centre column design 3 ONLY_ENERGY_IN. */
		MIXER {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int j = 0; j <= 1; j++) for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					boolean tCentre = i == 0 && k == 0;
					b.formingPart(i + anch[0], j + anch[1], k + anch[2], aWall,
							tCentre ? MultiBlockPartBlockEntity.ONLY_ENERGY_IN : j == 0 ? MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT : MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN,
							tCentre ? 3 : 0);
				}
				return b.build();
			}
		},
		/** MultiTileEntityAutoclave :47-63 — 3x3x3 hollow one ABOVE the controller, dense walls, item+fluid+energy. */
		HOLLOW_3X3X3 {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					if (i == 0 && j == 0 && k == 0) {
						b.hollow(i + anch[0], j + anch[1] + 1, k + anch[2], GTMultiBlockPattern.AIR);
					} else {
						b.formingPart(i + anch[0], j + anch[1] + 1, k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
					}
				}
				return b.build();
			}
		},
		/** MultiTileEntityCoagulator/Bath :47-79 — the solid 5x5x2 wall box. */
		BOX_5X5X2 {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int j = 0; j <= 1; j++) for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					b.formingPart(i + anch[0], j + anch[1], k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, 0);
				}
				return b.build();
			}
		},
		/** MultiTileEntityFermenter :48-129 — the 5x5 transmitter slab one BELOW the mid layer, the 5x5x2 wall box above it, the back mid-edge cells design 7 (the vent hole). */
		FERMENTER {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				int tVentI = -gt6APIOffX(aFacing) * 2, tVentK = -gt6APIOffZ(aFacing) * 2; // the back mid-edge (see the class doc table)
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int j = -1; j <= 1; j++) for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					if (j == -1) {
						b.formingPart(i + anch[0], j + anch[1], k + anch[2], aBase, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 0);
					} else {
						boolean tVent = i == tVentI && k == tVentK;
						b.formingPart(i + anch[0], j + anch[1], k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, tVent ? 7 : 0);
					}
				}
				return b.build();
			}
		},
		/** MultiTileEntityOven :47-91 — the 3x3x3: walls / the 8-coil ring around an air centre / walls; the coil type is the standing-block probe (the :62-63 try-Nichrome-then-Carborundum two-step). */
		OVEN {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					b.formingPart(i + anch[0], anch[1], k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
				}
				for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					if (i == 0 && k == 0) {
						b.hollow(i + anch[0], anch[1] + 1, k + anch[2], GTMultiBlockPattern.AIR);
					} else {
						b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aCoil, MultiBlockPartBlockEntity.NOTHING, 0);
					}
				}
				for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++) {
					b.formingPart(i + anch[0], anch[1] + 2, k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, 0);
				}
				return b.build();
			}
		},
		/** MultiTileEntitySluice :50-83 — the 7x3x3 trough controller-relative ("Slim-Side-Bottom"): walls down, the far-row energy wall at the middle layer, sluice parts on top. */
		SLUICE {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				// the trough extends -OFF (behind the facing) 6 cells: the controller-relative axis step
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing); // OFF_Y = 0 on the horizontal domain
				int tStepI = -gt6APIOffX(aFacing), tStepK = -gt6APIOffZ(aFacing);
				int tDesign = aFacing - 2; // the idle tD = mFacing-2 (the ACTIVE swing is the render wave's)
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int d = 0; d <= 6; d++) for (int s = -1; s <= 1; s++) {
					// (tI, tK): d along the trough axis, s across it
					int tI = tStepI * d + tStepK * s, tK = tStepK * d + tStepI * s;
					boolean tCentreLine = s == 0;
					b.formingPart(tI, anch[1], tK, aWall,
							tCentreLine ? MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT : MultiBlockPartBlockEntity.NOTHING,
							tCentreLine ? 1 : 0);
					boolean tFarRow = d == 5;
					b.formingPart(tI, anch[1] + 1, tK, aWall,
							tFarRow ? MultiBlockPartBlockEntity.ONLY_ENERGY_IN : MultiBlockPartBlockEntity.NOTHING,
							tFarRow ? 3 : 0);
					boolean tFarEnd = d == 6;
					b.formingPart(tI, anch[1] + 2, tK, aInner,
							tFarEnd ? MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN : MultiBlockPartBlockEntity.NOTHING,
							tDesign);
				}
				return b.build();
			}
		},
		/** MultiTileEntityCrusher/Shredder :49-140 — the 5x5x3 basin: the full wall floor OUT, the wall ring with the two perpendicular mid-edge ENERGY_IN walls + the 9-wheel filling, the wall ring + the 9-wheel input top. */
		BASIN_WHEELS {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				boolean tAxisX = gt6APIOffX(aFacing) != 0;
				int tDesign = tAxisX ? 2 : 0; // the idle tD: axis-Z -> 0, axis-X -> 2
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					b.formingPart(i + anch[0], anch[1], k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT, 0);
				}
				for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					if (Math.abs(i) <= 1 && Math.abs(k) <= 1) {
						// the 3x3 interior — the wheel filling (the :89-96/:126-134 ring)
						b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aInner, MultiBlockPartBlockEntity.NOTHING, tDesign);
					} else {
						boolean tMidEdge = (i == 0 && Math.abs(k) == 2) || (k == 0 && Math.abs(i) == 2);
						if (tMidEdge) {
							// the mid-edge PAIR perpendicular to the facing axis takes the energy in (:89/:120 conditionals)
							boolean tEnergy = tAxisX ? (i == 0) : (k == 0);
							b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aWall,
									tEnergy ? MultiBlockPartBlockEntity.ONLY_ENERGY_IN : MultiBlockPartBlockEntity.NOTHING,
									tEnergy ? 3 : 0);
						} else {
							b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aWall, MultiBlockPartBlockEntity.NOTHING, 0);
						}
					}
				}
				for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					if (Math.abs(i) <= 1 && Math.abs(k) <= 1) {
						b.formingPart(i + anch[0], anch[1] + 2, k + anch[2], aInner, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN, tDesign);
					} else {
						b.formingPart(i + anch[0], anch[1] + 2, k + anch[2], aWall, MultiBlockPartBlockEntity.NOTHING, 0);
					}
				}
				return b.build();
			}
		},
		/** MultiTileEntitySqueezer :48-107 — the 5x5x3 open basin: the full floor OUT, the 12-wall corner+edge ring (the interior and mid-edges OPEN), the full top IN. */
		BASIN_OPEN {
			@Override
			public GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase) {
				int[] anch = GTMultiBlockPattern.anchorOffset(aFacing);
				GTMultiBlockPattern.Builder b = GTMultiBlockPattern.builder();
				for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					b.formingPart(i + anch[0], anch[1], k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT, 0);
				}
				for (int i = -2; i <= 2; i++) for (int k = -2; k <= 2; k++) {
					if (Math.abs(i) == 2 && Math.abs(k) == 2) {
						b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aWall, MultiBlockPartBlockEntity.NOTHING, 0);
					} else if (Math.abs(i) == 2 && Math.abs(k) == 1 || Math.abs(k) == 2 && Math.abs(i) == 1) {
						// the edge walls only — the four mid-edges stay OPEN (the :57-67 ring census)
						b.formingPart(i + anch[0], anch[1] + 1, k + anch[2], aWall, MultiBlockPartBlockEntity.NOTHING, 0);
					}
					b.formingPart(i + anch[0], anch[1] + 2, k + anch[2], aWall, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN, 0);
				}
				return b.build();
			}
		};

		/**
		 * The declared pattern for one facing. {@code aWall} = the ring/wall block, {@code
		 * aInner} = the interior block (wheels/blades/sluice parts/coils; may equal the
		 * wall), {@code aCoil} = the resolved Oven coil type, {@code aBase} = the bottom
		 * slab block (null-safe: only the Fermenter carries one).
		 */
		public abstract GTMultiBlockPattern build(LargeMachineRow aRow, byte aFacing, Block aWall, Block aInner, Block aCoil, Block aBase);

		/** The GT6 OFF_X entry (GTMultiBlockPattern.OFF_X mirror — the horizontal 2..5 domain). */
		static int gt6APIOffX(byte aFacing) { return switch (aFacing) { case 4 -> -1; case 5 -> 1; default -> 0; }; }

		/** The GT6 OFF_Z entry. */
		static int gt6APIOffZ(byte aFacing) { return switch (aFacing) { case 2 -> -1; case 3 -> 1; default -> 0; }; }
	}

	// -------------------------------------------------------------------------
	// the controller block
	// -------------------------------------------------------------------------

	/**
	 * The large-machine controller block — the concrete
	 * {@link GTMultiBlockControllerBlock} over the shared BET; the row rides the instance
	 * (the BoilerTankBlock carrier read). No use-face: the machines run headless (the
	 * menu-null precedent — the class doc).
	 */
	public static final class GTLargeMachineBlock extends GTMultiBlockControllerBlock {

		private final LargeMachineRow mRow;

		public GTLargeMachineBlock(LargeMachineRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch) — the simpleCodec representative-value form (the BoilerTankBlock precedent).
		@Override
		protected com.mojang.serialization.MapCodec<? extends GTLargeMachineBlock> codec() {
			return simpleCodec(aProperties -> new GTLargeMachineBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the energy/structure config carrier). */
		public LargeMachineRow row() {
			return mRow;
		}

		/** The vanilla block name key — the atomic display ({@code block.gt6.<path>}). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable(getDescriptionId());
		}

		@Override
		protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
			return LARGE_MACHINE_BE.get();
		}
	}

	// -------------------------------------------------------------------------
	// the controller block entity
	// -------------------------------------------------------------------------

	/**
	 * The large-machine controller BE — the twelve-row carrier over
	 * {@link TileEntityBase10MultiBlockMachine}. What this class ADDS to the base (the
	 * base carries the Coke-Oven-shape trims; this card is the first fluid-consuming and
	 * first energy-fed family on the base):
	 * <ul>
	 * <li>the row config applied in the constructor (the EnergyRowSpec + energy type +
	 *     NO_CONSTANT_POWER + the recipe map — registration config, never persisted);</li>
	 * <li>the :455 TU gate RESTORED on the self-generation (the base's Coke-Oven-only
	 *     fold made the +1 unconditional; the onTick body is re-inlined because Java
	 *     cannot skip a superclass level — the 03 super call it replaces is the empty
	 *     hook it is there too);</li>
	 * <li>the fluid-input half of checkRecipe (:689-732) RESTORED over
	 *     {@link #mTanksInput} plus the :768/:771 efficiency divisor (the base folded
	 *     both to the Coke-Oven shape) — the verbatim upstream body with the two trims
	 *     undone, everything else untouched;</li>
	 * <li>the energy-intake face (upstream :489-519 verbatim minus the charging branch
	 *     the port cut): {@link #doInject} over the :501-505 math, the window trio, the
	 *     stopped-gated accepting arm;</li>
	 * <li>the declared structure pattern per facing ({@link StructureKind}) walked through
	 *     the shared checker (the Coke Oven shape), {@code patternWalkFacing() == 0}.</li>
	 * </ul>
	 */
	public static final class GTLargeMachineBlockEntity extends TileEntityBase10MultiBlockMachine {

		/** The in-repo plain key of the input tank (the base's "output_tank" mirror). */
		public static final String NBT_INPUT_TANK = "input_tank";

		/** The row (resolved from the block state; the offline fixtures inject it directly). */
		@Nullable
		private final LargeMachineRow mRow;

		/**
		 * The input tank — the upstream mTanksInput[0], the default FluidTankGT capacity
		 * (Long.MAX_VALUE, the base output-tank precedent; the twelve rows carry no
		 * NBT_TANK_CAPACITY). Filled through the fluid capability (the all-open face —
		 * the upstream mFluidInputs 127 default), drained by the recipe consume.
		 */
		public final FluidTankGT[] mTanksInput = {new FluidTankGT()};

		/**
		 * Upstream :96 mEfficiency — the efficiency divisor of the :768/:771 rows (the
		 * single-block carrier doc carries the full ruling; 10000 = identity, 5000 =
		 * half speed). Registration config, never persisted.
		 */
		public int mEfficiency = 10000;

		/** The per-facing pattern cache (the facing-conditional structures regenerate per facing). */
		private final GTMultiBlockPattern[] mPatternCache = new GTMultiBlockPattern[6];
		/** The Oven's per-coil-type variant selector (0 = Nichrome primary, 1 = Carborundum — the :62-63 two-step). */
		private int mOvenCoilVariant = 0;

		/** The registration factory — the row comes from the block. */
		public GTLargeMachineBlockEntity(BlockPos aPos, BlockState aState) {
			this(LARGE_MACHINE_BE.get(), aPos, aState, aState.getBlock() instanceof GTLargeMachineBlock tBlock ? tBlock.row() : null);
		}

		/** The test seam: offline fixtures build their own BET and inject the row (the TileEntityCokeOven precedent). */
		public GTLargeMachineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState, @Nullable LargeMachineRow aRow) {
			super(aType, aPos, aState);
			mRow = aRow;
			if (aRow != null) {
				applyEnergyRowSpec(aRow.energySpec());
				mEnergyTypeAccepted = aRow.energyType();
				mNoConstantEnergy = aRow.noConstantPower();
				mEfficiency = aRow.efficiency();
				// none of the twelve Loader :1229-1240 rows carries NBT_NEEDS_IGNITION — the
				// base TRUE default is the Coke-Oven-only fold (its :1193 row wrote the key T),
				// the large machines start on their own
				mRequiresIgnition = false;
			}
		}

		/** The row accessor — null on mis-registered offline fixtures (they inject the config instead). */
		@Nullable
		public LargeMachineRow row() {
			return mRow;
		}

		@Override
		public String getTileEntityName() {
			return "multiblock_large_machine";
		}

		/** The lazy row-keyed recipe map (the base's COKE_OVEN default overridden). */
		@Override
		public RecipeMap recipes() {
			RecipeMap tMap = mRecipes;
			if (tMap == null) {
				tMap = mRow != null ? mRow.recipes().get() : GT6RecipeMaps.COKE_OVEN;
				mRecipes = tMap;
			}
			return tMap;
		}

		// ---------------------------------------------------------------------
		// the tick chain — the :455 TU gate restored (see the class doc)
		// ---------------------------------------------------------------------

		@Override
		public void onTick(long aTimer, boolean aIsServerSide) {
			if (aIsServerSide) {
				// == TileEntityBase10MultiBlockBase.onTick: the 600-tick poll (:121-124)
				if (mTimer % 600 == 5) {
					if (!checkStructure(false)) checkStructure(true);
					doDefaultStructuralChecks();
				}
				// == :454-455 — the self-generation is TU-ONLY (upstream verbatim; the
				// base folded the gate away when the Coke Oven was the only subclass)
				if (!mStopped && mEnergyTypeAccepted == gregapi.data.TD.Energy.TU) mEnergy++;
				// == :459 + :461
				doOutputFluids();
				doWork(aTimer);
			}
		}

		// ---------------------------------------------------------------------
		// checkRecipe (:683-778) — the fluid-input half and the efficiency divisor
		// restored (see the class doc)
		// ---------------------------------------------------------------------

		@Override
		public int checkRecipe(boolean aApplyRecipe, boolean aUseAutoIO) {
			mCouldUseRecipe = false; // :684
			RecipeMap tRecipes = recipes();
			if (tRecipes == null) return DID_NOT_FIND_RECIPE; // :685

			int tInputItemsCount = 0; // :689
			ItemStack[] tInputs = new ItemStack[tRecipes.mInputItemsCount];
			for (int i = 0; i < tRecipes.mInputItemsCount; i++) {
				tInputs[i] = slot(i);
				if (tInputs[i] != null && !tInputs[i].isEmpty()) tInputItemsCount++;
			}

			// :689-706 — the fluid half the base cut: the tank contents ARE the recipe fluids
			// (the :696-705 auto-input pull is cut with the auto-IO surface, its absence is
			// the all-face capability fill this machine family carries instead)
			FluidStack[] tFluids = new FluidStack[mTanksInput.length];
			long[] tFluidsBefore = new long[mTanksInput.length]; // the pre-consume levels (the mirror's delta source)
			int tInputFluidCount = 0;
			for (int i = 0; i < mTanksInput.length; i++) {
				tFluids[i] = mTanksInput[i].fluid();
				tFluidsBefore[i] = tFluids[i] == null ? 0 : tFluids[i].getAmount();
				if (tFluids[i] != null && !tFluids[i].isEmpty()) tInputFluidCount++;
			}

			if (tInputItemsCount                     < tRecipes.mMinimalInputItems ) return DID_NOT_FIND_RECIPE; // :708
			if (tInputFluidCount                     < tRecipes.mMinimalInputFluids) return DID_NOT_FIND_RECIPE; // :709
			if (tInputItemsCount + tInputFluidCount  < tRecipes.mMinimalInputs     ) return DID_NOT_FIND_RECIPE; // :710

			// :712 — mInputMax is the voltage; the FURNACE bridge takes the Level (the
			// vanilla smelting lookup synthesises the row: eUt 16, dur 16, mCanBeBuffered F)
			Recipe tRecipe;
			if (tRecipes instanceof RecipeMapFurnace tFurnace) {
				tRecipe = hasLevel() ? tFurnace.findRecipe(getLevel(), mLastRecipe, mInputMax, slot(SLOT_SPECIAL), tFluids, tInputs) : null;
			} else {
				tRecipe = tRecipes.findRecipe(mLastRecipe, mInputMax, slot(SLOT_SPECIAL), tFluids, tInputs);
			}
			if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719

			if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734
			int tMaxProcessCount = canOutput(tRecipe); // :735
			if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736

			// :737 — the ignition gate, verbatim
			if (aApplyRecipe) aApplyRecipe = !mRequiresIgnition || mIgnited > 0 || mActive;
			// :738 — the two-stage consume, the FLUIDS array riding (the snapshots shrink;
			// the mirror into the tanks happens below on the apply path)
			if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, tFluids, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
			mCouldUseRecipe = true; // :739
			if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

			if (tMaxProcessCount > 1) {
				// :742-745 verbatim — the energy bind skips TU and parallelDuration rows
				if (!mParallelDuration && mEnergyTypeAccepted != gregapi.data.TD.Energy.TU) {
					tMaxProcessCount = (int) gregapi.util.UT.Code.bind(1, tMaxProcessCount, mInput / Math.max(1, tRecipe.mEUt));
				}
				int tExtra = 0;
				while (tExtra < tMaxProcessCount - 1 && tRecipe.isRecipeInputEqual(true, false, tFluids, tInputs)) tExtra++;
				tMaxProcessCount = 1 + tExtra;
			}

			mCurrentRecipe = tRecipe; // :757
			mOutputItems = tRecipe.getOutputs(tMaxProcessCount); // :758
			mOutputFluids = tRecipe.getFluidOutputs(tMaxProcessCount); // :759

			if (tRecipe.mEUt < 0) { // :761-764 — generator recipes
				mMaxProgress = tRecipe.mDuration;
				mMinEnergy = 0;
			} else {
				if (mParallelDuration) {
					// :766-768 — the duration carries the parallels, the divisor is mEfficiency
					mMinEnergy = Math.max(1, tRecipe.mEUt);
					mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration) * tMaxProcessCount, mEfficiency, 10000, true));
				} else {
					// :770-771 — the energy carries the parallels (the TU half keeps a
					// constant per-process energy)
					mMinEnergy = Math.max(1, mEnergyTypeAccepted == gregapi.data.TD.Energy.TU ? tRecipe.mEUt : tRecipe.mEUt * tMaxProcessCount);
					mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration), mEfficiency, 10000, true));
				}
				// :773 — overclocking: 4x energy, 2x speed; the CHEAP rows refuse the fold
				if (!mCheapOverclocking) {
					while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {
						mMinEnergy *= 4;
						mMaxProgress *= 2;
					}
				}
			}

			if (aApplyRecipe) mirrorFluidConsume(tFluids, tFluidsBefore); // the snapshot shrink lands on the tanks
			removeEmptyInputStacks(); // :776
			return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
		}

		/**
		 * The consume mirror: the :738/:745 two-stage shrank the SNAPSHOT array (the
		 * port Recipe mutates the passed stacks); the per-index delta drains the owning
		 * tank. One tank index per snapshot index — the arrays are the same length.
		 */
		private void mirrorFluidConsume(FluidStack[] aSnapshots, long[] aBefore) {
			for (int i = 0; i < mTanksInput.length && i < aSnapshots.length; i++) {
				// the :812-815 consume shrank the SNAPSHOT copy; the delta lands on the tank
				long tSnapNow = aSnapshots[i] == null ? 0 : aSnapshots[i].getAmount();
				if (tSnapNow < aBefore[i]) mTanksInput[i].drain((int)(aBefore[i] - tSnapNow), FluidAction.EXECUTE);
			}
		}

		// ---------------------------------------------------------------------
		// the energy-intake face (upstream :489-519, the charging branch cut)
		// ---------------------------------------------------------------------

		@Override
		public long doInject(gregapi.code.TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (mStopped) return 0; // :490
			aSize = Math.abs(aSize); // :492 (the :491 sign latch rides the cut :815 arm — no reader on this base)
			if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) { // :493
				if (aDoInject) overcharge(aSize, aEnergyType); // :494 — the Root D3 body
				return aAmount; // :495
			}
			// :497-500 charging branch cut (declared deviation, the single-block carrier note)
			if (aEnergyType == mEnergyTypeAccepted) { // :501
				long tInput = Math.min(mInputMax - mEnergy, aSize * aAmount), tConsumed = Math.min(aAmount, (tInput / aSize) + (tInput % aSize != 0 ? 1 : 0)); // :503
				if (aDoInject) mEnergy += tConsumed * aSize; // :504
				return tConsumed; // :505
			}
			return 0; // :507
		}

		/** Upstream :510 — the receiving arm only (the family never emits). */
		@Override public boolean isEnergyType(gregapi.code.TagData aEnergyType, byte aSide, boolean aEmitting) {return !aEmitting && aEnergyType == mEnergyTypeAccepted;}

		/** Upstream :511 with the default all-face mask (mEnergyInputs 127) — the stopped gate kept. */
		@Override
		public boolean isEnergyAcceptingFrom(gregapi.code.TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return (aTheoretical || !mStopped) && super.isEnergyAcceptingFrom(aEnergyType, aSide, aTheoretical);
		}

		@Override public long getEnergySizeInputMin(gregapi.code.TagData aEnergyType, byte aSide) {return mInputMin;} // :513
		@Override public long getEnergySizeInputRecommended(gregapi.code.TagData aEnergyType, byte aSide) {return mInput;} // :514
		@Override public long getEnergySizeInputMax(gregapi.code.TagData aEnergyType, byte aSide) {return mInputMax;} // :515
		@Override public java.util.Collection<gregapi.code.TagData> getEnergyTypes(byte aSide) {return mEnergyTypeAccepted.AS_LIST;} // :519

		// ---------------------------------------------------------------------
		// the declared structure (per-facing patterns over the shared checker)
		// ---------------------------------------------------------------------

		@Override
		public byte patternWalkFacing() {
			return 0; // the cells are CONTROLLER-relative (the crucible zero-offset form)
		}

		@Override
		@Nullable
		public GTMultiBlockPattern getStructurePattern() {
			if (mRow == null) return null;
			byte tFacing = (byte)(mFacing & 7);
			if (tFacing < 2 || tFacing > 5) tFacing = 2; // the horizontal canon guard
			GTMultiBlockPattern tCached = mPatternCache[tFacing];
			if (tCached != null && mRow.structure() != StructureKind.OVEN) return tCached; // the Oven re-probes its coil type every walk (the :62-63 two-step)
			Block tWall = wallBlock();
			if (tWall == null) return null;
			Block tInner = tWall;
			if (mRow.innerPath() != null) {
				Block tResolved = GTMultiBlocks.anyPartBlock(mRow.innerPath());
				if (tResolved == null) return null;
				tInner = tResolved;
			}
			Block tBase = mRow.basePath() != null ? GTMultiBlocks.HEAT_TRANSMITTER.get() : null;
			Block tCoil = tInner;
			if (mRow.structure() == StructureKind.OVEN) {
				// the :62-63 two-step re-expressed as pattern selection: the probe cell is
				// the first coil ring cell (centre-relative (-1, +1, -1)); a standing
				// Carborundum ring selects the Carborundum pattern, everything else
				// (including the fresh air pre-form) selects the Nichrome primary
				mOvenCoilVariant = probeOvenCoilVariant();
				Block tCarborundum = GTMultiBlocks.anyPartBlock("large_carborundum_coil");
				if (mOvenCoilVariant == 1 && tCarborundum != null) tCoil = tCarborundum;
			}
			GTMultiBlockPattern tPattern = mRow.structure().build(mRow, tFacing, tWall, tInner, tCoil, tBase);
			if (mRow.structure() != StructureKind.OVEN) mPatternCache[tFacing] = tPattern;
			return tPattern;
		}

		/** The Oven coil probe: the standing block at the first coil cell (0 = primary Nichrome, 1 = Carborundum). */
		private int probeOvenCoilVariant() {
			if (!hasLevel()) return 0;
			int[] anch = GTMultiBlockPattern.anchorOffset((byte)(mFacing & 7));
			BlockPos tProbe = getBlockPos().offset(anch[0] - 1, anch[1] + 1, anch[2] - 1);
			Block tStanding = getLevel().getBlockState(tProbe).getBlock();
			Block tCarborundum = GTMultiBlocks.anyPartBlock("large_carborundum_coil");
			return tCarborundum != null && tStanding == tCarborundum ? 1 : 0;
		}

		@Nullable
		private Block wallBlock() {
			return mRow != null ? GTMultiBlocks.anyPartBlock(mRow.wallPath()) : null;
		}

		/**
		 * Upstream :52-59 — the checker walk with the last-verdict keep on unloaded cells
		 * (the Coke Oven shape, the walkFacing-0 controller-relative declaration).
		 */
		@Override
		public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
			if (!hasLevel()) return mStructureOkay;
			GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
					this, patternWalkFacing(), aCoordinates, aPlayer, aInventory);
			if (tVerdict.unloaded) return mStructureOkay;
			return tVerdict.formed;
		}

		/** Upstream :94-97 per family — the declared bounding box, controller-relative. */
		@Override
		public boolean isInsideStructure(int aX, int aY, int aZ) {
			GTMultiBlockPattern tPattern = getStructurePattern();
			if (tPattern == null || !hasLevel()) return false;
			BlockPos tPos = getBlockPos();
			return aX >= tPos.getX() + tPattern.minX() && aY >= tPos.getY() + tPattern.minY() && aZ >= tPos.getZ() + tPattern.minZ()
					&& aX <= tPos.getX() + tPattern.maxX() && aY <= tPos.getY() + tPattern.maxY() && aZ <= tPos.getZ() + tPattern.maxZ();
		}

		// ---------------------------------------------------------------------
		// the fluid output target (:113-120 per family — the SIDE_BOTTOM push, the
		// Fermenter's SIDE_BACK 5-behind cell) and the fluid capability face
		// ---------------------------------------------------------------------

		@Override
		protected IFluidHandler getFluidOutputTarget(Fluid aOutput) {
			if (!hasLevel() || isClientSide()) return null;
			BlockPos tTarget;
			Direction tFace;
			if (mRow != null && mRow.autoOutBack()) {
				// MultiTileEntityFermenter :159-161 — the cell 5 OUT of the back face at
				// the controller layer, accessed at its back-pointing face
				Direction tBack = Direction.from3DDataValue(mFacing).getOpposite();
				tTarget = getBlockPos().relative(tBack, 5);
				tFace = Direction.from3DDataValue(mFacing);
			} else {
				// the getAdjacentTank(SIDE_BOTTOM) family form — the below cell at its top face
				tTarget = getBlockPos().below();
				tFace = Direction.UP;
			}
			BlockEntity tNeighbor = getLevel().getBlockEntity(tTarget);
			if (tNeighbor == null) return null;
			//? if forge {
			return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, tFace).resolve().orElse(null);
			//?} else {
			/*// 21.1: the query goes through the level (the TileEntityCokeOven seam form).
			return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tTarget, tFace);
			*///?}
		}

		/** The large-machine fluid face: FILL into the input tank (all faces — the upstream mFluidInputs 127 default), DRAIN from the output tank. */
		//? if forge {
		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
			if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
				return LazyOptional.of(() -> new LargeMachineFluidHandler(this, aSide)).cast();
			}
			return super.getCapability(aCapability, aSide); // the gated item surface + the rest of the base face
		}

		//?} else {
		/*// 21.1 seam member (the kitchen-pot shape): the RegisterCapabilitiesEvent provider
		// in GT6CapabilityWiring delegates here (registerLargeMachineFaces).
		public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
			if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
				return (T) new LargeMachineFluidHandler(this, aSide);
			}
			return null; // the item face rides the base's own seam member through its wiring row
		}
		*///?}

		/**
		 * The fill+drain face over {@link #mTanksInput} / {@link #mTanksOutput}: the fill
		 * half is the upstream :564-571 getFluidTankFillable2 with the DEFAULT all-open
		 * mask (mFluidInputs 127 — the twelve rows register no NBT_TANK_SIDE_IN), the
		 * drain half mirrors the base MultiBlockFluidHandler output rules (the rotated 61
		 * mask, the relative top refuses).
		 */
		public static final class LargeMachineFluidHandler implements IFluidHandler {

			private final GTLargeMachineBlockEntity mMachine;
			@Nullable
			private final Direction mSide;

			public LargeMachineFluidHandler(GTLargeMachineBlockEntity aMachine, @Nullable Direction aSide) {
				mMachine = aMachine;
				mSide = aSide;
			}

			@Override
			public int getTanks() {
				return 2; // the input tank + the output tank
			}

			@Override
			public FluidStack getFluidInTank(int aTank) {
				FluidTankGT tTank = tank(aTank);
				if (tTank == null) return FluidStack.EMPTY;
				FluidStack tFluid = tTank.fluid();
				return tFluid == null ? FluidStack.EMPTY : tFluid;
			}

			@Override
			public int getTankCapacity(int aTank) {
				FluidTankGT tTank = tank(aTank);
				return tTank == null ? 0 : FluidTankGT.bindInt(tTank.getCapacity());
			}

			@Override
			public boolean isFluidValid(int aTank, FluidStack aStack) {
				return aTank == 0 && aStack != null && !aStack.isEmpty();
			}

			/** The all-open fill (mask 127) into the input tank. */
			@Override
			public int fill(FluidStack aResource, FluidAction aAction) {
				if (aResource == null || aResource.isEmpty()) return 0;
				return mMachine.mTanksInput[0].fill(aResource, aAction);
			}

			/** The output-tank drain (the base drain rules; the input tank never drains through the face). */
			@Override
			public FluidStack drain(int aMaxDrain, FluidAction aAction) {
				if (aMaxDrain <= 0) return FluidStack.EMPTY;
				if (mSide != null && MultiBlockFluidHandler.relativeSide(mMachine.mFacing, (byte)mSide.get3DDataValue()) == 1) return FluidStack.EMPTY; // the relative top refuses
				FluidTankGT tTank = mMachine.mTanksOutput[0];
				if (tTank.isEmpty()) return FluidStack.EMPTY;
				FluidStack rDrained = tTank.drain(aMaxDrain, aAction);
				if (aAction.execute() && !rDrained.isEmpty()) mMachine.setChanged();
				return rDrained;
			}

			@Override
			public FluidStack drain(FluidStack aResource, FluidAction aAction) {
				if (aResource == null || aResource.isEmpty() || !mMachine.mTanksOutput[0].contains(aResource)) return FluidStack.EMPTY;
				return drain(FluidTankGT.bindInt(Math.min(mMachine.mTanksOutput[0].amount(), aResource.getAmount())), aAction);
			}

			@Nullable
			private FluidTankGT tank(int aTank) {
				return aTank == 0 ? mMachine.mTanksInput[0] : aTank == 1 ? mMachine.mTanksOutput[0] : null;
			}
		}

		// ---------------------------------------------------------------------
		// NBT — the input tank rides the base save/load chain
		// ---------------------------------------------------------------------

	// the tank NBT rides the shared chain — no provider-needing IO on this face (the
	// FluidTankGT readFromNBT/writeToNBT pair is the base output-tank form verbatim)
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mTanksInput[0].writeToNBT(aNBT, NBT_INPUT_TANK);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_INPUT_TANK, Tag.TAG_COMPOUND)) mTanksInput[0].readFromNBT(aNBT, NBT_INPUT_TANK);
	}
	}
}
