package gregtech6.block.energy;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import gregapi.code.TagData;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import gregtech6.covers.ICoverableTE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The ZPM Decharger block (task p36-energy-zpm-dechargers) — the
 * {@link GT6BatteryBoxBlock} shape with the SECOND energy column: upstream files the two
 * rows (Loader_MultiTileEntities.java:1000-:1001) as
 * {@code NBT_ENERGY_ACCEPTED QU, NBT_ENERGY_EMITTED QU|EU} — the in lane rides the
 * parent's {@code energyType()} column, the out lane (the Base10 :55
 * {@code mEnergyTypeOut} seat) rides this class. Row :1000-:1001
 * hardness/resistance 4.0/50.0, 1 slot, tier 7 (V[7] both directions).
 *
 * <p>The FRONT face is the OUTPUT (the emit side, the BatteryBox :232-:233 convention
 * inherited), ALL-BUT-FRONT the intake face (dead in practice on this host: the ZPM
 * never registers chargeable, so the :179 guard refuses every network packet — the
 * one-way artifact→machine lane). The per-face zpm_electricity/zpm_quantum art is the
 * render pool (p36-render-texture-bake): this card ships the battery-box stand-in cube.
 */
public class GT6ZpmDechargerBlock extends GT6BatteryBoxBlock {

	/** The NBT_ENERGY_EMITTED column (the Base10 :55 mEnergyTypeOut seat). */
	private final Supplier<TagData> mOutEnergyType;

	public GT6ZpmDechargerBlock(BlockBehaviour.Properties aProperties, int aTier, int aSlots,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			Supplier<TagData> aInEnergyType, Supplier<TagData> aOutEnergyType) {
		super(aProperties, aTier, aSlots, aTickerType, aInEnergyType);
		mOutEnergyType = aOutEnergyType;
	}

	/** The emit lane (the BE's out-lane seat, resolved once at construction). */
	public Supplier<TagData> outEnergyType() {
		return mOutEnergyType;
	}

	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		super.stepOn(aLevel, aPos, aState, aEntity);
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable) tCoverable.onCoverWalkOver(aEntity); // MultiTileEntityBlock.java:306 -> 06Covers:428 (p37-covers-crafting-asphalt)
	}
}
