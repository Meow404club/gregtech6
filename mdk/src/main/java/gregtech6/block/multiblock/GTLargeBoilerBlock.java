package gregtech6.block.multiblock;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

/**
 * The Large Boiler controller block — the concrete {@link GTMultiBlockControllerBlock} of
 * the five variants (upstream MultiTileEntityLargeBoiler, Loader_MultiTileEntities.java
 * :1248-1252 re-read verbatim). Everything visual/behavioural is base-owned (FACING +
 * FORMED); this class mounts the shared LARGE_BOILER_BE and carries the variant row (the
 * BoilerTankBlock form — the upstream NBT_DESIGN wall id + NBT_OUTPUT_SU registration NBT
 * become the row), the row data itself lives in {@link GTMultiBlocks.LargeBoilerRow}.
 *
 * <p>NO use override: the Large Boiler has NO GUI by census (upstream addToolTips
 * :150-166 carries no GUI line, unlike the CokeOven) — a right-click does nothing.
 *
 * <p>The two pressurised-removal faces of the explosion family live here (the W3
 * BoilerTankBlock form): {@code playerWillDestroy} (the removedByPlayer :313-316
 * counterpart — barometer &gt; 4 while a NON-creative player breaks the CONTROLLER
 * detonates; NOT an onRemove override) and {@code onBlockExploded} (the :318-322
 * onExploded second blast — the BE consulted BEFORE the air swap). Breaking any
 * STRUCTURE wall instead arms the tick's structural probe (the :262 arm on the part
 * block's playerWillDestroy → onStructureChange chain — the "拆任意结构墙即炸"
 * semantics, no extra block hook needed).
 */
public class GTLargeBoilerBlock extends GTMultiBlockControllerBlock {

	private final GTMultiBlocks.LargeBoilerRow mRow;

	public GTLargeBoilerBlock(GTMultiBlocks.LargeBoilerRow aRow, Properties aProperties) {
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
	protected com.mojang.serialization.MapCodec<? extends GTLargeBoilerBlock> codec() {
		return simpleCodec(aProperties -> new GTLargeBoilerBlock(GTMultiBlocks.LARGE_BOILER_ROWS.get(0), aProperties));
	}
	*///?}

	/** The registration row (the BoilerTankBlock.row carrier read — output + wall variant). */
	public GTMultiBlocks.LargeBoilerRow row() {
		return mRow;
	}

	/** The gt6 registry path tail (the datagen/lang key). */
	public String path() {
		return mRow.path();
	}

	/**
	 * The Boiler Wall block of this variant (upstream :66 mBoilerWalls + :77 NBT_DESIGN —
	 * the wall MTE id pair becomes the Block identity the row carries).
	 */
	public net.minecraft.world.level.block.Block wallBlock() {
		net.minecraftforge.registries.RegistryObject<GTMultiBlockPartBlock> tHandle =
				GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(mRow.wallPath());
		return tHandle == null ? GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(GTMultiBlocks.WALL_ROWS.get(0).path()).get() : tHandle.get();
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.LARGE_BOILER_BE.get();
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityLargeBoiler tBoiler) {
			tBoiler.setFacingFromPlacement(tPlayer); // the FRONT (barometer) mirror
		}
	}

	/**
	 * Upstream :313-316 removedByPlayer — the pressurised-dismantle trigger on the
	 * CONTROLLER block: barometer &gt; 4 while a NON-creative player breaks it → explode(T)
	 * (instant). Creative players and sub-4 pressure just break it (the W3 form).
	 */
	@Override
	public void playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
		super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
		if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof TileEntityLargeBoiler tBoiler) {
			tBoiler.dismantle(aPlayer); // :314 — the creative check inside
		}
	}

	/**
	 * Upstream :318-322 onExploded — the caught-in-a-neighbour's-explosion second blast
	 * (the IForgeBlock explosion face, the W3 form: the BE consult happens BEFORE the air
	 * swap; the chained explode(T) may cascade — the upstream semantics verbatim).
	 */
	@Override
	public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
		if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof TileEntityLargeBoiler tBoiler) {
			tBoiler.onExploded(); // :321
		}
		aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3); // the IForgeBlock default body
	}
}
