package gregtech6.block.energy;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import gregapi.code.TagData;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The BatteryBox block (task w4-battery-storage ④) — the facing-cube carrier of
 * {@link gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity}, the
 * GT6ElectricTransformerBlock shape with the BatteryBox convention (Base10EnergyBatBox
 * :232-233): the FRONT face ({@code mFacing}) is the OUTPUT face (the emit side), ALL
 * OTHER faces are input. Row :894-:895 hardness/resistance 4.0/4.0, stack 16.
 *
 * <p>Placement = the GT6PlacementFacing canon (issue #18, task 18-converter-tex-facing:
 * SIX-WAY now — upstream Base10EnergyBatBox rides Base09 whose SIDES_VALID = all six,
 * CS.java:699 — the front TOWARDS the placer over the full look, the monkey wrench
 * re-faces to the clicked sub-face). The BE mirror re-syncs from the state each tick
 * (the state is the authority). The block
 * carries the family's slot count (4 or 16 — the NBT_INV_SIZE column) as data; the two
 * sizes share the BE class over two BETs. {@code use()} is the two-arm dispatch (task
 * batterybox-gui): the wrench sub-face pick first, then the GUI open (upstream
 * onBlockActivated3 :99-102 {@code openGUI} — the Base10 getGUIServer2 :195-196
 * ContainerCommonDefault chest-grid, re-expressed as the
 * {@link gregtech6.gui.machines.GT6BatteryBoxMUI} panel through the BE's
 * {@code GT6MuiMachine} face; the MUI factory chain, zero MenuType). NO onRemove
 * override (the BaseEntityBlock kill+recreate lesson).
 *
 * <p>Since task tex-composite-family the block carries the ACTIVITY and MATERIAL
 * columns of the upstream rows: the ACTIVE property drives the overlay_active texture
 * shell (upstream {@code getTexture2 sOverlays[mActiveState & 3]} — MultiTileEntityBatteryBox
 * :31-:33 / CrystalCharger :33-:36 / ZPMDechargerEU :39-:44, the trinary collapsed to the
 * boolean, 0=overlay / 1=overlay_active, the blinking third state the #18 defer), and
 * the material supplier feeds the tint seat (upstream {@code NBT_MATERIAL, MT.DATA.
 * Electric_T[i]} per row — Loader_MultiTileEntities :894-:895/:970-:971, the ZPM rows
 * MT.Osmiridium :1000-:1001 — the {@link GT6ElectricTransformerBlock} lazy-Supplier
 * form). The former "no activity layer upstream" doc claim was a misreading — every
 * energystorages family borrows its overlay_active trio.
 */
public class GT6BatteryBoxBlock extends GTEntityBlock implements gregtech6.block.GTWrenchUsable {

	/** Facing property (six-way, issue #18 — FRONT is the output face, ALL-BUT-FRONT the input; upstream SIDES_VALID = all six). */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** The activity visual property (the overlay layer selector; the BE drives it off {@code mActive}) — the GTBlockProperties shared singleton. */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE = gregtech6.block.GTBlockProperties.ACTIVE;

	/** The NBT_INV_SIZE column: 4 (the small box, :894) or 16 (the Large box, :895). */
	private final int mSlots;

	/** The tier ladder index (the VN ordinal — V[tier] rides mInput/mOutput, the NBT_INPUT/OUTPUT columns). */
	private final int mTier;

	/** The family BET (the GT6ElectricTransformers registry object supplier). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/** The energy domain of the box (task p35: EU = the battery boxes, LU = the Crystal Chargers — the NBT_ENERGY_ACCEPTED/EMITTED columns). */
	private final Supplier<TagData> mEnergyType;

	/**
	 * The block's upstream {@code NBT_MATERIAL} column (task tex-composite-family —
	 * the tint colour source, the {@link GT6ElectricTransformerBlock} lazy-Supplier
	 * form): the row's Electric_T[i] casing (Loader :894-:895/:970-:971) or Osmiridium
	 * (the ZPM rows :1000-:1001).
	 */
	@Nullable
	private final Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	public GT6BatteryBoxBlock(Properties aProperties, int aTier, int aSlots,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		this(aProperties, aTier, aSlots, aTickerType, () -> gregapi.data.TD.Energy.EU, null);
	}

	/** The typed constructor (task p35 — the Crystal Charger LU family). */
	public GT6BatteryBoxBlock(Properties aProperties, int aTier, int aSlots,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Supplier<TagData> aEnergyType) {
		this(aProperties, aTier, aSlots, aTickerType, aEnergyType, null);
	}

	/** The material-carrier form (task tex-composite-family): the row feeds the tint colour source. */
	public GT6BatteryBoxBlock(Properties aProperties, int aTier, int aSlots,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Supplier<TagData> aEnergyType,
			@Nullable Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties);
		mTier = aTier;
		mSlots = aSlots;
		mTickerType = aTickerType;
		mEnergyType = aEnergyType;
		mMaterial = aMaterial;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
	}

	/**
	 * The block's upstream {@code NBT_MATERIAL}, resolved lazily through the Supplier;
	 * null = the white no-tint identity (the offline codec representative).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The battery-box material dispatch (task tex-composite-family, the
	 * {@link GT6ElectricTransformerBlock#materialOf} mirror shape): the carrier blocks
	 * (battery boxes, crystal chargers, ZPM dechargers) resolve their row material —
	 * every other block is null here.
	 */
	@Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6BatteryBoxBlock tBox ? tBox.material() : null;
	}

	/** The box's energy domain (the BE resolveEnergyType seat; lazy like every MT/TD read). */
	public Supplier<TagData> energyType() {
		return mEnergyType;
	}

	/** The block's own family BET supplier (the BE resolveBet seat — the charger blocks resolve THEIR BET, not the battery-box one). */
	public Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> betSupplier() {
		return mTickerType;
	}

	/** The family slot count (the BE reads it off its block state — the NBT_INV_SIZE column). */
	public int slots() {
		return mSlots;
	}

	/** The tier ladder index (the BE reads V[tier] off it — the NBT_INPUT/NBT_OUTPUT columns). */
	public int tier() {
		return mTier;
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract — the GT6ElectricTransformerBlock
	// simpleCodec representative-value form (slot count 4, the family/material suppliers
	// dropped).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6BatteryBoxBlock> codec() {
		return simpleCodec(aProperties -> new GT6BatteryBoxBlock(aProperties, 0, 4, () -> null));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, ACTIVE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer over the full look — the FRONT is the OUTPUT face
		// here (the consumer side; the canon, the vertical fold per issue #18)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getNearestLookingDirection()));
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem (the GTOvenBlock fork).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// the wrench arm (issue #18 — upstream Base09 onToolClick2 :67): the clicked
		// wrench-grid sub-face becomes the FRONT (the emit face); the state is the
		// authority, the BE tick mirror follows. PASS when no wrench is held — the
		// GUI arm answers then (upstream onBlockActivated3 :99-102 ran after the
		// tool click the same way).
		InteractionResult tWrench = GT6ElectricTransformerBlock.wrenchRotate(aState, aLevel, aPos, aPlayer, aHand, aHit, FACING);
		if (tWrench != InteractionResult.PASS) return tWrench;
		// the GUI open arm (task batterybox-gui — the Base10 getGUIServer2 :195-196
		// chest-grid): the MUI factory chain (the GTBasicMachineBlock :404 shape, zero
		// MenuType). The family inherits this arm (GT6ZpmDechargerBlock overrides nothing).
		if (aLevel.getBlockEntity(aPos) instanceof gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity tBox
				&& aPlayer instanceof net.minecraft.server.level.ServerPlayer tServerPlayer) {
			gregtech6.gui.machines.GT6MuiMachine.tryOpen(tServerPlayer, tBox);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		BlockEntityType<? extends TileEntityBase03TicksAndSync> tType = tickerType();
		return tType == null ? null : tType.create(aPos, aState); // the GTEntityBlock form
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
		BlockEntityType<? extends TileEntityBase03TicksAndSync> tType = tickerType();
		if (tType == null || aType != tType) {
			return null;
		}
		return (aTickerLevel, aPos, aTickerState, aTile) -> {
			if (aTile instanceof TileEntityBase03TicksAndSync tTile && tTile.canUpdate() && !tTile.isRemoved()) {
				tTile.updateEntity();
			}
		};
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL;
	}
}
