package gregtech6.block.energy;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6FeBatteries;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The FE battery test fixture block (task p26-eu-bridge-outbound) — the GTEnergySourceBlock
 * shape (the simple-cube GTEntityBlock carrier minus the CONNECTIONS property) over its own
 * BET. The RECEIVING end of the EU->FE outbound bridge acceptance chain: the RCON chain
 * (/gt6febattery place|stat|reset) drives it headless, so there is NO {@code use} interaction
 * and NO onRemove override (the BaseEntityBlock kill+recreate lesson, remember id59).
 */
public class GT6FeBatteryBlock extends GTEntityBlock {

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
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6FeBatteries.FE_BATTERY_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
