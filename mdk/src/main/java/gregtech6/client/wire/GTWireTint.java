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
import gregtech6.registry.GTWireSpecs;

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
 * insulation jacket is a per-family FIXED colour (task p11-wire-brightness spec 3): the
 * electric family uses the upstream gray 64,64,64 (MultiTileEntityWireElectric.java:237-238,
 * {@code isPainted() ? mRGBa : getRGBInt(64,64,64)}), the redstone family the upstream
 * {@code 96,64,64} (MultiTileEntityWireRedstoneInsulated.java:184-185 — the same constant on
 * the INSULATION_FULL side jacket AND the diameter-tier caps) — both rendered in their
 * unpainted form (the painted-foam variant is a foam-feature deviation). Index 1 = the
 * jacket, bound where the model emits insulation quads. Every other index returns -1 (no
 * tint), as does index 0 on the material-less legacy pair.
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from GTClientHandlers under the
 * dist guard; per the Forge docs a BlockColor does NOT colour the BlockItem, so the ItemColor
 * twin is registered over the block items in the same handler).
 */
@OnlyIn(Dist.CLIENT)
public final class GTWireTint {

	/** The electric-family jacket, upstream {@code getRGBInt(64, 64, 64)} (WireElectric :237-238). */
	public static final int ELECTRIC_JACKET = 0xFF404040;

	/** The redstone-family jacket, upstream {@code getRGBInt(96, 64, 64)} (WireRedstoneInsulated :184-185). */
	public static final int REDSTONE_JACKET = 0xFF604040;

	private GTWireTint() {
	}

	/**
	 * The opaque ARGB for a tint index over one wire material (the pure seam the tests
	 * drive). The family picks the jacket: redstone {@value #REDSTONE_JACKET}, everything
	 * else (electric rows, the material-less legacy pair, the jacket-less laser form that
	 * never emits index 1) the electric constant.
	 */
	public static int tintARGB(@Nullable OreDictMaterial aMaterial, @Nullable GTWireSpecs.Row.Family aFamily, int aTintIndex) {
		if (aTintIndex == 1) {
			return aFamily == GTWireSpecs.Row.Family.REDSTONE ? REDSTONE_JACKET : ELECTRIC_JACKET;
		}
		if (aTintIndex == 0 && aMaterial != null) {
			short[] tRGBa = aMaterial.fRGBaSolid;
			return 0xFF000000 | (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]); // UT.Code.getRGBInt :1580-1582
		}
		return -1;
	}

	/** The world-side half: registered over {@code GTWires.wireBlockArray()} (GTClientHandlers). */
	public static BlockColor blockColor() {
		return (BlockState aState, BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) ->
				aState.getBlock() instanceof GTWireBlock tWire ? tintARGB(tWire.material(), tWire.family(), aTintIndex) : -1;
	}

	/** The inventory half: registered over the block items (GTClientHandlers). */
	public static ItemColor itemColor() {
		return (aStack, aTintIndex) -> {
			if (aStack.getItem() instanceof BlockItem tItem && tItem.getBlock() instanceof GTWireBlock tWire) {
				return tintARGB(tWire.material(), tWire.family(), aTintIndex);
			}
			return -1;
		};
	}

	/** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
	private static int bind8(long aValue) {
		return (int) Math.max(0, Math.min(255, aValue));
	}
}
