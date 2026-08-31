package gregtech6.client.wire;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.wire.GTWireBlock;

/**
 * The wire family material tints (task p9-wire-family-w2) — the runtime half of the true-
 * material rendering, the {@code GTMaterialPrefixBlock.blockColor} shape applied to the
 * wires: the grayscale {@code materialicons/<set>/wire.png} carries the shape, tint index 0
 * carries the material colour.
 *
 * <p>Upstream colour picks: the wire TE renders {@code mRGBa}, whose unpainted default is
 * {@code UT.Code.getRGBInt(mMaterial.fRGBaSolid)} (TileEntityBase07Paintable.unpaint :83 —
 * the exact {@code fRGBaSolid} expression), so index 0 = {@code fRGBaSolid} bound to ARGB
 * (the UT.Code.getRGBInt :1580-1582 encoding, GTMaterialPrefixBlock.tintARGB form). The
 * insulation jacket is the upstream fixed gray 64,64,64
 * (MultiTileEntityWireElectric.java:237-238, {@code isPainted() ? mRGBa : getRGBInt(64,64,64)}
 * — the painted-foam variant is a foam-feature deviation, W2 renders the unpainted form),
 * bound to tint index 1 where the model emits insulation quads. Every other index returns
 * -1 (no tint), as does index 0 on the material-less legacy pair.
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from GTClientHandlers under the
 * dist guard; per the Forge docs a BlockColor does NOT colour the BlockItem, so the ItemColor
 * twin is registered over the block items in the same handler).
 */
@OnlyIn(Dist.CLIENT)
public final class GTWireTint {

	private GTWireTint() {
	}

	/** The opaque ARGB for a tint index over one wire material (the pure seam the tests drive). */
	public static int tintARGB(@Nullable OreDictMaterial aMaterial, int aTintIndex) {
		if (aTintIndex == 1) return 0xFF404040; // the upstream insulation jacket, getRGBInt(64, 64, 64)
		if (aTintIndex == 0 && aMaterial != null) {
			short[] tRGBa = aMaterial.fRGBaSolid;
			return 0xFF000000 | (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]); // UT.Code.getRGBInt :1580-1582
		}
		return -1;
	}

	/** The world-side half: registered over {@code GTWires.wireBlockArray()} (GTClientHandlers). */
	public static BlockColor blockColor() {
		return (BlockState aState, BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) ->
				aState.getBlock() instanceof GTWireBlock tWire ? tintARGB(tWire.material(), aTintIndex) : -1;
	}

	/** The inventory half: registered over the block items (GTClientHandlers). */
	public static ItemColor itemColor() {
		return (aStack, aTintIndex) -> {
			if (aStack.getItem() instanceof BlockItem tItem && tItem.getBlock() instanceof GTWireBlock tWire) {
				return tintARGB(tWire.material(), aTintIndex);
			}
			return -1;
		};
	}

	/** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
	private static int bind8(long aValue) {
		return (int) Math.max(0, Math.min(255, aValue));
	}
}
