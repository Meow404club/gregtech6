package gregtech6.block.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6FeBatteries;
import gregtech6.tileentity.energy.GT6FeSourceBlockEntity;

/**
 * The FE source test fixture block (task p28-b-fe-converter-machine) — the sink battery
 * block's extractable twin: a simple-cube BaseEntityBlock carrier over the fixture BET,
 * NO use interaction and NO onRemove override (the BaseEntityBlock kill+recreate lesson,
 * remember id59). Plain BaseEntityBlock, NOT the GTEntityBlock carrier — the fixture BE
 * is deliberately OUTSIDE the 01Root family (the foreign-source identity, the
 * GT6FeBatteryBlock doc), and a pure source needs no ticker.
 */
public class GT6FeSourceBlock extends BaseEntityBlock {

	public GT6FeSourceBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract — the simpleCodec representative-value
	// form, GTEnergySourceBlock precedent.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6FeSourceBlock> codec() {
		return simpleCodec(aProperties -> new GT6FeSourceBlock(aProperties));
	}
	*///?}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6FeBatteries.FE_SOURCE_BE.get().create(aPos, aState);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
