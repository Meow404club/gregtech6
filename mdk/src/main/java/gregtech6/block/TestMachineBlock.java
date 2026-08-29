package gregtech6.block;

import javax.annotation.Nullable;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * Framework test block (task p3-be-framework): mounts the shared test BET
 * (ADR-P3-1 multi-mount) in a ticking and a passive variant — the passive one
 * overrides the ticker to null, the 1.20.1 equivalent of the upstream notick chain
 * (a TileEntityBase01Root constructed with mIsTicking=false never entering
 * updateEntity).
 *
 * <p>Blockstate/model/lang datagen belongs to the WAVE-2 example machine card — no
 * JSON is authored here (red line: all JSON comes from DataGen).
 */
public class TestMachineBlock extends GTEntityBlock {

	private final boolean mTicking;

	public TestMachineBlock(boolean aTicking, Properties aProperties) {
		super(aProperties);
		mTicking = aTicking;
	}

	public boolean isTicking() {
		return mTicking;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.TEST_MACHINE_BE.get();
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
		if (!mTicking) {
			return null; // notick chain equivalent (EntityBlock.getTicker default null, EntityBlock.java:18-20)
		}
		return super.getTicker(aLevel, aState, aType);
	}
}
