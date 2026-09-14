package gregtech6.block.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.GTTankValveBlockEntity;

/**
 * The Tank Main Valve controller block — the concrete {@link GTMultiBlockControllerBlock}
 * of the 25 variants (upstream MultiTileEntityTank3x3x3{Wood,Metal} /
 * MultiTileEntityTank5x5x5Metal, Loader_MultiTileEntities.java:1195-1222 re-read verbatim).
 * Everything visual/behavioural is base-owned (FACING + FORMED); this class mounts the
 * shared {@link GT6Tanks#TANK_VALVE_BE} and carries the variant row (the BoilerTankBlock
 * form — the upstream NBT_DESIGN wall id + NBT_TANK_CAPACITY + the four proof flags
 * registration NBT become the row), the row data itself lives in
 * {@link GT6Tanks.TankValveRow}.
 *
 * <p>NO use override: the Tank has NO GUI by census (MultiTileEntityTank.addToolTips
 * :71-82 carries no GUI line, the funnel/tap toolchain is the interaction surface) — a
 * right-click does nothing.
 *
 * <p>The wood valve carries NBT_FLAMMABILITY 150 (:1195): the fire-face trio overrides on
 * the flammable row only (the WoodWallPartBlock shape — the same value drives isFlammable,
 * getFlammability and getFireSpreadSpeed, the TileEntityBase07Paintable :107-108 ruling).
 * The block sound follows the upstream block-material column (aWooden → WOOD, the wood
 * row; aMachine → METAL, the 24 metal rows).
 */
public class GTTankValveBlock extends GTMultiBlockControllerBlock {

	private final GT6Tanks.TankValveRow mRow;

	public GTTankValveBlock(GT6Tanks.TankValveRow aRow, Properties aProperties) {
		super(aProperties);
		mRow = aRow;
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the GTLargeBoilerBlock simpleCodec representative-value form.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTTankValveBlock> codec() {
		return simpleCodec(aProperties -> new GTTankValveBlock(GT6Tanks.ROWS.get(0), aProperties));
	}
	*///?}

	/** The registration row (the block carrier — capacity, the four proof flags, the NBT_DESIGN wall). */
	public GT6Tanks.TankValveRow row() {
		return mRow;
	}

	/** The gt6 registry path tail (the datagen/lang key). */
	public String path() {
		return mRow.path();
	}

	/** The composed tank name (the {@link GT6Tanks#displayOf} carrier — the wood/metal template pair). */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return GT6Tanks.displayOf(mRow);
	}

	/**
	 * The Tank Wall block this variant is built from (upstream mTankWalls 18002 default +
	 * the :1195-1222 NBT_DESIGN per row — the wall MTE id pair becomes the Block identity
	 * the row carries). The lookup spans every part registration map via the leg-neutral
	 * {@link GTMultiBlocks#anyPartBlock} (wood wall / machine walls / dense walls).
	 */
	public net.minecraft.world.level.block.Block wallBlock() {
		net.minecraft.world.level.block.Block tBlock = GTMultiBlocks.anyPartBlock(mRow.wallPath());
		if (tBlock != null) return tBlock;
		return GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(GTMultiBlocks.WALL_ROWS.get(0).path()).get(); // the SS Dense Wall defensive default (unreachable — every row path resolves)
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6Tanks.TANK_VALVE_BE.get();
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, net.minecraft.world.entity.LivingEntity aPlacer, net.minecraft.world.item.ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof net.minecraft.world.entity.player.Player tPlayer
				&& aLevel.getBlockEntity(aPos) instanceof GTTankValveBlockEntity tValve) {
			tValve.setFacingFromPlacement(tPlayer); // the FRONT mirrors the placement (the boiler shape)
		}
	}

	// ---------------------------------------------------------------------------
	// the wood fire faces (NBT_FLAMMABILITY 150, :1195 — the flammable row only)
	// ---------------------------------------------------------------------------

	@Override
	public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return mRow.flammable() ? 150 : 0; // NBT_FLAMMABILITY :1195
	}

	@Override
	public boolean isFlammable(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return mRow.flammable();
	}

	@Override
	public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return mRow.flammable() ? 150 : 0; // :107 — the same value drives both fire faces
	}
}
