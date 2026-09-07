package gregtech6.client.render;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

/**
 * The machine paint tint (task p21-paintable-tint-render, card_B of the P21 paintable split)
 * — the client consumption half of card_A's paint storage: the 21 machine-domain block
 * models carry {@code tintindex 0} (the GT6BlockStates machineModel element form) and this
 * {@link BlockColor} resolves that index from the BE's
 * {@link GTModelProperties#PAINT} model data (the 03 base supplies it while painted,
 * TileEntityBase03TicksAndSync.getModelData).
 *
 * <p>Upstream equivalence: the 1.7.10 machine renders {@code getTexture2 =
 * BlockTextureMulti(BlockTextureDefault(mTexturesMaterial[side], mRGBa), ...)}
 * (MultiTileEntityBasicMachine.java:1014) — the grayscale texture multiplied by the paint
 * colour, white = unchanged. The vanilla tint pipeline is the same multiplication over the
 * quad colour, so returning {@code mRGBa} here IS the upstream result. Unpainted (the PAINT
 * property absent, the {@code ModelData.EMPTY} contract of the 03 base) falls back to
 * {@link #UNPAINTED} white 0xFFFFFF — bound with full alpha the white result is
 * {@code 0xFFFFFFFF}, numerically the vanilla {@code -1} no-tint sentinel, i.e. the exact
 * "no visual change" both routes share. Every other tint index returns {@code -1} (no tint).
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from GTClientHandlers under the
 * dist guard). Per the Forge docs a BlockColor does NOT colour its BlockItem: the inventory
 * half stays unregistered (the painted-look item form is the pooled item-domain card,
 * upstream TileEntityBase04MultiTileEntities.java:131 writeItemNBT semantics).
 */
@OnlyIn(Dist.CLIENT)
public final class GTMachinePaintTint {

	/** Upstream UNCOLORED = 0xFFFFFF (CS.java:327) — white multiplies the grayscale texture unchanged. */
	public static final int UNPAINTED = 0xFFFFFF;

	private GTMachinePaintTint() {
	}

	/**
	 * The pure seam the tests drive (the GTWireTint.tintARGB shape): the opaque ARGB for one
	 * tint index over one snapshot. Index 0 reads {@link GTModelProperties#PAINT}, defaulting
	 * to white when the property is absent (unpainted); every other index is no tint.
	 */
	public static int tintARGB(@Nullable ModelData aData, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		Integer tPaint = aData == null ? null : aData.get(GTModelProperties.PAINT);
		return 0xFF000000 | (tPaint == null ? UNPAINTED : (tPaint.intValue() & UNPAINTED));
	}

	/**
	 * The world-side half: registered over {@code GTMachines.paintableBlockArray()}
	 * (GTClientHandlers). The registration scope is the pinned 21-block machine census, so
	 * the lambda needs no block-type gate — the PAINT property lookup is the gate (a BE
	 * without the property resolves to the white default). A null level/pos or a missing BE
	 * is the no-tint sentinel — which IS full-alpha white (see the class doc identity).
	 */
	public static BlockColor blockColor() {
		return (BlockState aState, @Nullable BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) -> {
			if (aTintIndex != 0 || aLevel == null || aPos == null) return -1;
			BlockEntity tBE = aLevel.getBlockEntity(aPos);
			return tBE == null ? -1 : tintARGB(tBE.getModelData(), aTintIndex);
		};
	}
}
