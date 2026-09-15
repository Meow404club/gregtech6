package gregtech6.block.surface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * The GT6 surface stick (task p30-w6-rocks-sticks) — ONE block for the upstream MTE 32756
 * (WorldgenSticks.java:65 places it bare, MultiTileEntityStick: no NBT, hardness 0.25 :189,
 * no collision :177, fire 300/300 :190-191). Same attach/pickup/micro-slab behaviour as the
 * rock (the :72-99 rows are the MultiTileEntityRock rows re-run for sticks), so this is a
 * subclass carrying only the flammable face and the wood sound/map colour.
 *
 * <p>Declared deviation (research row "stick 群系名子串"): the upstream getDefaultStick
 * biome-substring ladder (MultiTileEntityStick.java:112-153, ~25 wood materials + the
 * 3/16 Dead/Mossy/Rotten roll) is NOT materialised — the first batch drops the vanilla
 * stick (research verdict "首批可收敛 vanilla stick"); the wood band lands as loot-table
 * biome conditions when the material-stick item face is consumed (no 25 blocks — the card
 * pin).
 */
public class GT6SurfaceStickBlock extends GT6SurfaceRockBlock {

	public GT6SurfaceStickBlock(Properties aProperties) {
		super(aProperties.sound(SoundType.WOOD).mapColor(MapColor.WOOD), null);
	}

	/** MultiTileEntityStick.java:190-191 verbatim — the IForgeBlock 4-arg faces (the GTTankValveBlock form, both legs). */
	@Override
	public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return 300;
	}

	@Override
	public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		return 300;
	}
}
