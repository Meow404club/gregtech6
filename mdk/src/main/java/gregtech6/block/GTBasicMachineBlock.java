package gregtech6.block;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}

import gregtech6.gui.machines.GT6MuiMachine;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The basic-machine block (task p7-basicmachine-family ③) — the GTOvenBlock shape with the
 * cover machinery and the wrench-rotation layer cut (the machine family registers no cover
 * surface and the side-configuration stays a carrier concern): BlockState carries the whole
 * visual payload — FACING (the upstream byte mFacing, horizontal subset) plus ACTIVE/RUNNING,
 * the two visual bits of upstream getVisualData :1010-1011 re-expressed as properties
 * (16 datagen blockstate variants per machine, the A-tier furnace idiom). use() is the
 * pure open-GUI chain (upstream onBlockActivated3 :483-486 → openGUI), setPlacedBy mirrors
 * onPlaced (:128-131). No onRemove override (id59).
 *
 * <p>One class, many registered blocks (Shredder/Crusher/Lathe + the Dryer ladder) — each
 * instance resolves its own BlockEntityType through the supplier captured at registration
 * (ADR-P3-1: the ticker asks the live instance every tick). Since task p14-dryer-family a
 * block may carry a {@link MachineRow} — the block-carrier projection of one upstream
 * aRegistry.add line (the GT6Boilers BoilerRow shape): the tier ladder is then data on the
 * placed BlockState's block (the factory reads {@link #row()}), not a tierOf dispatch. The
 * legacy three families stay row-less (the 2-arg constructor); since task
 * p26-mui-a-open-chain their use() dispatches the ModularUI chain
 * ({@link GT6MuiMachine#tryOpen}) — since task p26-mui-row-menu-null-dispatch the dispatch
 * key is the MenuType supplier itself ({@link #opensModularUi}): every machine WITHOUT a
 * bound gt6:* menu opens ModularUI (the batch-A ruling "new machines default to
 * ModularUI", the W1 row form menu = null), the machines WITH one keep the vanilla menu
 * path (the dryer/canner chains, byte-identical).
 */
public class GTBasicMachineBlock extends GTEntityBlock {

	/** Facing property (horizontal — the upstream SIDES_HORIZONTAL valid sides). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	// the CS.java:612 connectivity bit values (the port carries no SBIT constant copy — the
	// masks are row data; bit order == the machine-relative side order of GTSideTables).
	public static final byte SBIT_D = 1, SBIT_U = 2, SBIT_L = 4, SBIT_F = 8, SBIT_R = 16, SBIT_B = 32, SBIT_A = 64;

	/**
	 * One registration row — the block-carrier projection of one upstream aRegistry.add
	 * line (task p14-dryer-family, the GT6Boilers BoilerRow record precedent). Every column
	 * of the Dryer rows (Loader_MultiTileEntities.java:1477-1480) has a named field:
	 *
	 * @param path               the gt6 registry path (the blockstate/model/lang key tail)
	 * @param matSlug            the row material slug (the gt6.row.mat small-unit key tail)
	 * @param matDisplay         the row material local name (MT.DATA.Heat_T[1..4] = Steel/Invar/
	 *                           Titanium/Tungsten Carbide, MT.java:3689), verbatim
	 * @param material           the row material — the upstream NBT_MATERIAL column (task
	 *                           p27-machine-material-tint-fidelity): every upstream basic-machine
	 *                           row carries it and the 1.7.10 registration derives the render
	 *                           colour from it (MultiTileEntityClassContainer.java:51 —
	 *                           {@code hasKey(NBT_MATERIAL) && !hasKey(NBT_COLOR) → NBT_COLOR =
	 *                           getRGBInt(material.fRGBaSolid)}), so the port default colour of a
	 *                           row IS {@code material.fRGBaSolid}. A LAZY supplier per the
	 *                           GTBarrels MetalDrumRow convention (GTWireSpecs.java:35 ruling:
	 *                           the registry classes load before {@code MT.init()}, a direct
	 *                           {@code MT.X} field read in a row initializer would resolve null)
	 * @param displayKey         the family display template key ({@code gt6.row.dryer.display} /
	 *                           {@code gt6.row.distillery.display}) the composed name fills
	 * @param metaId             the upstream MultiTileEntity id (20311-20314), the zero-diff yardstick
	 * @param hardness           the NBT_HARDNESS column (6/4/9/12.5 — NBT_RESISTANCE == hardness)
	 * @param tier               the tier index 0..3 (the TIER_INPUTS selector — the NBT_INPUT
	 *                           column 32/128/512/2048 through the :126 conversion lives in
	 *                           GTMachines.TIER_INPUTS). MATERIAL tier, not voltage: the
	 *                           four variants pick the Kinetic/Heat_T material words
	 *                           (MT.DATA.Heat_T[1..4] here, MT.java:3689-3690); voltage
	 *                           names (LV/MV/HV/EV) belong to the Electric_T motor classes
	 *                           only (task p27-machine-energy-display-fix javadoc ruling)
	 * @param parallel           the NBT_PARALLEL column (8/16/32/64 — clamped at :130)
	 * @param parallelDuration   the NBT_PARALLEL_DURATION column (T on all four rows)
	 * @param recipes            the NBT_RECIPEMAP column as a supplier — RM.Drying is the
	 *                           volatile GT6RecipeMaps.DRYING, read at BE creation time
	 * @param energyType         the NBT_ENERGY_ACCEPTED column (TD.Energy.HU)
	 * @param texture            the NBT_TEXTURE column ("dryer", all four rows — the ladder
	 *                           shares the T1 front textures, the p8 ruling)
	 * @param energySides        NBT_ENERGY_ACCEPTED_SIDES (SBIT_D — the :151 read ORs SBIT_A)
	 * @param fluidIn            NBT_TANK_SIDE_IN (SBIT_B|SBIT_L — the :143 read ORs SBIT_A)
	 * @param fluidOut           NBT_TANK_SIDE_OUT (SBIT_U — the :144 read ORs SBIT_A)
	 * @param itemIn             NBT_INV_SIDE_IN (SBIT_B|SBIT_L — the :137 read ORs SBIT_A);
	 *                           data-only for now, the item-face gate rides the item-IO pool
	 * @param itemOut            NBT_INV_SIDE_OUT (SBIT_R — the :138 read ORs SBIT_A); data-only, same pool
	 * @param fluidAutoIn        NBT_TANK_SIDE_AUTO_IN (SIDE_BACK); data-only, the auto-IO pool
	 * @param fluidAutoOut       NBT_TANK_SIDE_AUTO_OUT (SIDE_TOP); data-only, the auto-IO pool
	 * @param itemAutoIn         NBT_INV_SIDE_AUTO_IN (SIDE_LEFT); data-only, the auto-IO pool
	 * @param itemAutoOut        NBT_INV_SIDE_AUTO_OUT (SIDE_RIGHT); data-only, the auto-IO pool
	 * @param menu               the port-owned MenuType supplier (null = the menu-less
	 *                           carrier: use() dispatches the ModularUI chain instead of
	 *                           constructing a vanilla menu — task
	 *                           p26-mui-row-menu-null-dispatch generalized the
	 *                           p26-mui-a-open-chain inert gate into the MUI dispatch; a
	 *                           non-null supplier keeps the vanilla menu path, the
	 *                           dryer/canner registration is the live example)
	 * @param cheapOverclocking  the NBT_CHEAP_OVERCLOCKING column (T on all four rows — the
	 *                           port overclock loop :773 runs unconditionally, "no config
	 *                           source, always T")
	 * @param maxMeltingPointK   the port-owned melting-gate column (task
	 *                           p28-c-ulv-machine-ladder, NO upstream NBT key — the ULV
	 *                           tier extension is the declared-deviation face): non-null
	 *                           arms the {@code TileEntityBasicMachine.checkRecipe} input
	 *                           melting-point hook (any input material stack with
	 *                           {@code mMeltingPoint} above the ceiling refuses the
	 *                           recipe), null = no gate. Every ULV row carries 1375 K —
	 *                           the stone-crucible ceiling (GT6Crucibles.java:86), the
	 *                           Smeltery :194 / Mold :189 container semantics
	 *                           re-expressed as a machine gate. Rides the BE through
	 *                           {@code GTMachines.applyRow} (the mask-carrier seam).
	 * @param ulvVoltage         the voltage-ladder marker (task p28-c-ulv-machine-ladder,
	 *                           the row-level counterpart of the upstream NBT_INPUT
	 *                           column — the port folds NBT_INPUT into {@code tier}
	 *                           through TIER_INPUTS, so the ULV rows need the explicit
	 *                           selector): true = the BET factory feeds the BE the ULV
	 *                           window GTMachines.ULV_TIER_INPUTS = {4, 8, 16} (8 EU × 1 A,
	 *                           the V[0] packet lands mid-window) instead of
	 *                           TIER_INPUTS[tier]; false = the legacy material-ladder
	 *                           behaviour, byte-identical.
	 */
	public record MachineRow(String path, String matSlug, String matDisplay,
			java.util.function.Supplier<gregapi.oredict.OreDictMaterial> material,
			String displayKey, int metaId, float hardness,
			int tier, int parallel, boolean parallelDuration,
			java.util.function.Supplier<gregtech6.recipes.RecipeMap> recipes, gregapi.code.TagData energyType,
			String texture,
			byte energySides, byte fluidIn, byte fluidOut, byte itemIn, byte itemOut,
			byte fluidAutoIn, byte fluidAutoOut, byte itemAutoIn, byte itemAutoOut,
			@Nullable java.util.function.Supplier<net.minecraft.world.inventory.MenuType<gregtech6.gui.machines.GTBasicMachineMenu>> menu,
			boolean cheapOverclocking, @Nullable Long maxMeltingPointK, boolean ulvVoltage) {

		/** The block properties (hardness == resistance on every row; the METAL machine sound). */
		public net.minecraft.world.level.block.state.BlockBehaviour.Properties properties() {
			return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(hardness, hardness)
					.sound(net.minecraft.world.level.block.SoundType.METAL);
		}
	}

	/**
	 * The carried registration row (null = the legacy row-less families — Shredder/Crusher/
	 * Lathe keep the 2-arg constructor and the tierOf dispatch of the p8 shape).
	 */
	private final MachineRow mRow;

	/**
	 * The composed-name carrier of the row-less tier blocks (task p20-i18n-compose-rows):
	 * a pre-composed name Component the tier registrations supply — the MachineRow carrier
	 * would drag the MenuType-supplier dispatch semantics onto the tier ladders, so they
	 * ride this instead (and keep the plain composed name).
	 */
	@Nullable
	private final java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> mComposedName;

	/**
	 * The material carrier of the legacy row-less tier blocks (task
	 * p27-machine-material-tint-fidelity): the Shredder/Crusher/Lathe ladders keep the
	 * tierOf dispatch and the 4-arg constructor for their NAMES, but their upstream rows
	 * carry NBT_MATERIAL like every other family (Loader_MultiTileEntities.java:1294-1309,
	 * Kinetic_T[1..4]), so the tier registrations hand the row colour source through this
	 * supplier. Null = genuinely material-less (no such registration today — the white
	 * fallback of {@link #materialColor} stays the identity).
	 */
	@Nullable
	private final java.util.function.Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		this(aProperties, aTickerType, null, null, null);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTBasicMachineBlock> codec() {
		return simpleCodec(aProperties -> new GTBasicMachineBlock(aProperties, () -> gregtech6.registry.GTMachines.OVEN_BE.get()));
	}
	*///?}

	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, @Nullable MachineRow aRow) {
		this(aProperties, aTickerType, aRow, null);
	}

	/** The tier-ladder form (task p20-i18n-compose-rows): a row-less block whose name is the pre-composed supplier. */
	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			@Nullable MachineRow aRow, @Nullable java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> aComposedName) {
		this(aProperties, aTickerType, aRow, aComposedName, null);
	}

	/**
	 * The material-carrying tier-ladder form (task p27-machine-material-tint-fidelity): the
	 * legacy row-less ladders (Shredder/Crusher/Lathe) hand the tier's Kinetic_T material
	 * through {@code aMaterial} — the block identity carries the render colour source the
	 * same way the row carriers do through {@link MachineRow#material}.
	 */
	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			@Nullable MachineRow aRow, @Nullable java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> aComposedName,
			@Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties);
		mTickerType = aTickerType;
		mRow = aRow;
		mComposedName = aComposedName;
		mMaterial = aMaterial;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false).setValue(RUNNING, false));
	}

	/**
	 * The composed name (task p20-i18n-compose-rows): a row carrier fills its family template
	 * over the gt6.row.mat small unit, a tier carrier hands back its pre-composed name, the
	 * legacy families keep the vanilla atomic-key lookup.
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		if (mRow != null) {
			return net.minecraft.network.chat.Component.translatable(mRow.displayKey(),
					net.minecraft.network.chat.Component.translatable("gt6.row.mat." + mRow.matSlug()));
		}
		if (mComposedName != null) return mComposedName.get();
		return super.getName();
	}

	/** The carried row, or null for the legacy row-less families. */
	@Nullable
	public MachineRow row() {
		return mRow;
	}

	/**
	 * The block's row material (task p27-machine-material-tint-fidelity) — the colour
	 * source the paint tint falls back to while unpainted and the unpaint() write-back
	 * restores: the block-side mirror of the upstream NBT_MATERIAL → NBT_COLOR derivation
	 * (MultiTileEntityClassContainer.java:51). Row carriers read the row column, the
	 * legacy tier ladders the constructor supplier. Null = material-less (the white
	 * identity of {@link #materialColor}).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		if (mRow != null && mRow.material() != null) return mRow.material().get();
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The machine-domain material dispatch (task p27-machine-material-tint-fidelity): the
	 * ONE common-code seam the paint tint (world + inventory halves) and the 03 base
	 * unpaint() consult, over the three block carriers that mirror upstream NBT_MATERIAL
	 * rows — {@link GTBasicMachineBlock} (the MachineRow families + the Kinetic legacy
	 * ladders), {@link GTOvenBlock} (the Heat_T Oven ladder) and the
	 * {@code GT6BurningBoxes.BurningBoxBlock} rows (Loader :519-548/:619-704). Everything
	 * else (barrels, pipes, vanilla states) is material-less: null → the white identity,
	 * which keeps the P23 barrel registration byte-identical.
	 */
	@Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		if (aBlock instanceof GTBasicMachineBlock tMachine) return tMachine.material();
		if (aBlock instanceof GTOvenBlock tOven) return tOven.material();
		if (aBlock instanceof gregtech6.registry.GT6BurningBoxes.BurningBoxBlock tBox) return tBox.material();
		return null;
	}

	/**
	 * The machine default colour of one material (task p27-machine-material-tint-fidelity):
	 * {@code UT.Code.getRGBInt(mMaterial.fRGBaSolid)} (UT.java:1580-1582 over
	 * OreDictMaterial.java:111) — the exact 1.7.10 registration expression
	 * (MultiTileEntityClassContainer.java:51), the 0xRRGGBB form the {@code mRGBa} storage
	 * keeps. Null material = upstream UNCOLORED 0xFFFFFF (CS.java:327, the Paintable:50
	 * field default) — full-alpha bound that IS the vanilla {@code -1} no-tint sentinel.
	 */
	public static int materialColor(@Nullable gregapi.oredict.OreDictMaterial aMaterial) {
		if (aMaterial == null) return 0xFFFFFF;
		short[] tRGBa = aMaterial.fRGBaSolid;
		return (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]); // UT.Code.getRGBInt :1580-1582
	}

	/** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
	private static int bind8(long aValue) {
		return (int) Math.max(0, Math.min(255, aValue));
	}

	/**
	 * Actually processing (upstream mActive, visual bit 0) — the GTBlockProperties single
	 * instance (ADR-P16-2), so this IS the same object as GTOvenBlock.ACTIVE by
	 * construction; the shared datagen helper reads either interchangeably. (The old
	 * "interning" note was a wrong theory: vanilla never interned by name — 1.20.1 only
	 * worked because StateHolder looked properties up by value equality, and 1.21.x looks
	 * them up by identity, where distinct instances crash.)
	 */
	public static final BooleanProperty ACTIVE = GTBlockProperties.ACTIVE;

	/** Powered / has work (upstream mRunning, visual bit 1) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final BooleanProperty RUNNING = GTBlockProperties.RUNNING;

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, ACTIVE, RUNNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer (the GT6PlacementFacing canon, task
		// p28-singleblock-facing-canon) — the same side setFacingFromPlacement writes,
		// so the client prediction and the server pair-write agree (UseOnContext.java:70
		// getHorizontalDirection = the VIEW direction; the canon is its opposite)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(javap BlockBehaviour 21.1.249); the game loop drives MAIN_HAND first.
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// AbstractFurnaceBlock.use :39-45 shape, upstream onBlockActivated3 :483-486
		// (the cover consumption and the wrench rotation of GTOvenBlock.use are cut —
		// the machine family registers no covers)
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof TileEntityBasicMachine tMachine && aPlayer instanceof ServerPlayer tServerPlayer) {
			if (opensModularUi(mRow)) {
				// no gt6:* MenuType bound — the ModularUI chain (the ACT :91 shape, the
				// BlockEntityUIFactory's own network): the row-less families since task
				// p26-mui-a-open-chain, the menu-less row carriers since task
				// p26-mui-row-menu-null-dispatch (the batch-A ruling "new machines default
				// to ModularUI" — the W1 five families register MachineRow + menu = null)
				GT6MuiMachine.tryOpen(tServerPlayer, tMachine);
				return InteractionResult.CONSUME;
			}
			// the vanilla menu path (a bound supplier — the dryer/canner chains, unchanged):
			//? if forge {
			NetworkHooks.openScreen(tServerPlayer, tMachine, aPos); // upstream openGUI
			//?} else {
			/*tServerPlayer.openMenu(tMachine, tBuf -> tBuf.writeBlockPos(aPos)); // 21.1: NetworkHooks deleted — ServerPlayer.openMenu(MenuProvider, buf) carries the pos payload (the command-file precedent)
			*///?}
		}
		return InteractionResult.CONSUME;
	}

	/**
	 * The MUI dispatch key of {@code use()} (task p26-mui-row-menu-null-dispatch — the
	 * generalization of the p26-mui-a-open-chain {@code mRow == null} gate): a machine
	 * with NO MenuType supplier opens the {@link GT6MuiMachine#tryOpen} chain. That is the
	 * row-less families (null row) AND the menu-less row carriers (the batch-A machine
	 * form: {@code MachineRow + menu = null}, the Distillery today, the W1 five families
	 * the wave card registers next). A row WITH a supplier keeps the vanilla menu path —
	 * the truth table is two nulls deep: (row null) → MUI, (menu null) → MUI, (menu
	 * bound) → NetworkHooks/openMenu, byte-identical for the dryer/canner chains.
	 */
	static boolean opensModularUi(@Nullable MachineRow aRow) {
		return aRow == null || aRow.menu() == null;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityBasicMachine tMachine) {
			tMachine.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131
		}
	}
}
