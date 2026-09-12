package gregtech6.block.energy;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6FeConverters;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The FE→EU converter block carrier (task p28-b-fe-converter-machine) — a simple cube
 * over the shared BET, the GTEnergySourceBlock shape minus the command rig. SINGLE-TIER
 * family (the ULV balance ruling of decisions.p28-eu-inbound-converter): one block, the
 * tier voltage living as the constant in {@link GT6FeConverters} — the BE reads it from
 * there, no per-instance state to carry.
 *
 * <p>NO {@code use} interaction and NO onRemove override — the BaseEntityBlock
 * kill+recreate lesson (remember id59).
 */
public class GT6FeConverterBlock extends GTEntityBlock {

	public GT6FeConverterBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch); the simpleCodec representative-value form, GTEnergySourceBlock precedent.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6FeConverterBlock> codec() {
		return simpleCodec(GT6FeConverterBlock::new);
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6FeConverters.FE_CONVERTER_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
