package gregtech6.tileentity.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GT6 Plant Pot — 1.20.1 counterpart of
 * {@code gregtech/tileentity/tools/MultiTileEntityPlantPot.java} (94 lines), the :2229
 * row ("Universal Plant Pot", meta 32065).
 *
 * <p>The BE exists for ONE face: the paint stratum (the upstream 07Paintable base —
 * mRGBa/mIsPainted + the spray-paint face, the 03 folded stratum supplies it). The pot
 * carries NO inventory (the upstream canDrop :92 override is vacuous — the 07Paintable
 * base has no slots) and NO tick (the upstream class overrides no tick work). Every
 * other semantic lives on the block carrier (the pot geometry/canSustainPlant/light
 * opacity) and the datagen (the two-element tinted model).
 */
public class GT6PlantPotBlockEntity extends TileEntityBase03TicksAndSync {

	/** BET factory for BlockEntityType.Builder.of. */
	public GT6PlantPotBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.PLANT_POT_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6PlantPotBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState); // the no-tick chain — no upstream tick work
	}

	@Override
	public String getTileEntityName() {
		return "plant_pot"; // upstream :90 "gt.multitileentity.plantpot" — the BET path mirrors it
	}
}
