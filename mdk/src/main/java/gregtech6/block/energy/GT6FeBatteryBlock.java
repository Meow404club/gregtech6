package gregtech6.block.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6FeBatteries;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity;

/**
 * The FE battery test fixture block (task p26-eu-bridge-outbound) — a simple-cube
 * BaseEntityBlock carrier over the fixture BET. The RECEIVING end of the EU->FE outbound
 * bridge acceptance chain: the RCON chain (/gt6febattery place|stat|reset) drives it
 * headless, so there is NO {@code use} interaction and NO onRemove override (the
 * BaseEntityBlock kill+recreate lesson, remember id59).
 *
 * <p>Deliberately a plain BaseEntityBlock rather than the GTEntityBlock carrier: the
 * GTEntityBlock ticker wire types against the TileEntityBase03TicksAndSync family, and the
 * fixture BE is deliberately OUTSIDE that family (a foreign-receiver stand-in — see the
 * GT6FeBatteryBlockEntity class doc; the ticker is pointless on a pure sink anyway).
 */
public class GT6FeBatteryBlock extends BaseEntityBlock {

	public GT6FeBatteryBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch); the simpleCodec representative-value form, GTEnergySourceBlock precedent.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6FeBatteryBlock> codec() {
		return simpleCodec(aProperties -> new GT6FeBatteryBlock(aProperties));
	}
	*///?}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6FeBatteries.FE_BATTERY_BE.get().create(aPos, aState);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
