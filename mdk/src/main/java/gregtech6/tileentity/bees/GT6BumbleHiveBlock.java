package gregtech6.tileentity.bees;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.ToolActions;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6BeeHives;

/**
 * The Bumble Hive block (task p32-bees-lv2) — the block carrier of the MTE 32755 port
 * (Loader_MultiTileEntities.java:2041 over the {@code aHive} "rock" MTE block,
 * :111: {@code MaterialScoopable + soundTypeWood + TOOL_scoop}). The material/tool pair
 * maps onto the modern declarative face:
 *
 * <ul>
 * <li><b>THE SCOOP GATE</b> — {@link #canHarvestBlock} keys on the held tool's
 *     {@code SHEARS_HARVEST} action (the GTScoopItem javadoc's declared BeehiveBlock-face
 *     mapping: forge keys the vanilla-hive honeycomb harvest on the same action,
 *     BeehiveBlock.java.patch:26; the scoop is a ShearsItem, so it performs the action
 *     for free). A wrong-tool break destroys the block but drops NOTHING — the
 *     TOOL_scoop harvest semantics; creative passes (instabuild) and drops nothing as
 *     vanilla (playerDestroy never runs).</li>
 * <li><b>THE LOOT SHELL</b> — {@link #playerDestroy} drops the BE inventory (the
 *     upstream {@code mDroppable} collapse: onBlockHarvested + canDrop :97-98 fold into
 *     the harvest walk, since only a harvest reaches playerDestroy). noLootTable — the
 *     contents ARE the loot, the Drops_None convention (GTBedrockOreBlock precedent).</li>
 * <li><b>FIRE</b> — flammability/fire-spread 300/300 (MultiTileEntityBumbleHive.java:83-84
 *     verbatim).</li>
 * </ul>
 *
 * <p>Strength 1.0/1.0 (the upstream getBlockHardness/getExplosionResistance2 lit-pumpkin
 * pair, :80-82 = vanilla jack_o_lantern {@code strength(1.0F)}), wood sound (the aHive
 * row). Not obtainable as an item: no BlockItem, no creative tab — the worldgen-only
 * loot-shell convention (GT6SurfaceBlocks rock/stick form).
 */
public class GT6BumbleHiveBlock extends GTEntityBlock {

	public GT6BumbleHiveBlock(Properties aProperties) {
		super(aProperties);
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the GTSensorBlock fork shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6BumbleHiveBlock> codec() {
		return simpleCodec(aProperties -> new GT6BumbleHiveBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
		return GT6BeeHives.HIVE_BE.get();
	}

	/** The TOOL_scoop gate: shears-action tool (the scoop) or creative — else no drops. */
	@Override
	public boolean canHarvestBlock(BlockState aState, BlockGetter aLevel, BlockPos aPos, Player aPlayer) {
		return aPlayer.getAbilities().instabuild
				|| aPlayer.getMainHandItem().canPerformAction(ToolActions.SHEARS_HARVEST);
	}

	/** The mDroppable collapse: the whole BE inventory drops on a proper harvest (:97-98). */
	@Override
	public void playerDestroy(Level aLevel, Player aPlayer, BlockPos aPos, BlockState aState,
			@Nullable BlockEntity aBlockEntity, ItemStack aTool) {
		super.playerDestroy(aLevel, aPlayer, aPos, aState, aBlockEntity, aTool);
		if (aLevel.isClientSide || !(aBlockEntity instanceof GT6BumbleHiveBlockEntity tHive)) return;
		for (int i = 0; i < tHive.inventory().getSlots(); i++) {
			ItemStack tStack = tHive.inventory().getStackInSlot(i);
			if (!tStack.isEmpty()) {
				popResource(aLevel, aPos, tStack);
				tHive.inventory().setStackInSlot(i, ItemStack.EMPTY);
			}
		}
	}

	/** Upstream getFlammability :84 verbatim. */
	@Override
	public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, @Nullable Direction aDirection) {
		return 300;
	}

	/** Upstream getFireSpreadSpeed :83 verbatim. */
	@Override
	public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, @Nullable Direction aDirection) {
		return 300;
	}
}
