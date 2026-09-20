package gregtech6.client.render;

import javax.annotation.Nullable;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

/**
 * The machine paint tint (task p21-paintable-tint-render, card_B of the P21 paintable
 * split; the colour source re-based on the row material by task
 * p27-machine-material-tint-fidelity) — the client consumption half of the paint storage:
 * the machine-domain block models carry {@code tintindex 0} (the GT6BlockStates
 * machineModel element form) and the tint resolves from the BE's
 * {@link GTModelProperties#PAINT} model data (the 03 base supplies it while painted,
 * TileEntityBase03TicksAndSync.getModelData).
 *
 * <p>Since task p32-render-embeddium-tint this class is the PURE colour-decision seam
 * only: the WORLD half of the consumption moved to {@link GTMachineTintModel}, which
 * bakes {@link #tintARGB} into the quads' vertex colours at {@code getQuads} time (the
 * runtime {@code BlockColor} route rendered achromatic in the live client on both chunk
 * builders — the investigation record lives in the known_bugs
 * embeddium_tint_no_shader entry). {@link #blockColor} stays as the unregistered
 * reference form the tests drive; the ITEM half still rides
 * {@code GTItemPaintTint} ({@code ItemColor}), a different consumer that was never
 * implicated.
 *
 * <p>Upstream equivalence: the 1.7.10 machine renders {@code getTexture2 =
 * BlockTextureMulti(BlockTextureDefault(mTexturesMaterial[side], mRGBa), ...)}
 * (MultiTileEntityBasicMachine.java:1014) — the grayscale texture multiplied by the colour.
 * The DEFAULT colour is the row material, NOT white: the registration derives
 * {@code NBT_COLOR = getRGBInt(material.fRGBaSolid)} from every machine row's NBT_MATERIAL
 * (MultiTileEntityClassContainer.java:51), so an unpainted steel machine renders
 * gray-white and a copper one orange-red (the research.p27-machine-tint-reresearch
 * correction of the former "upstream default gray" reading — white was only the field
 * fallback, Paintable:50). This seam mirrors that two-level chain: a present PAINT value
 * wins (the spray-paint override, upstream Paintable:85); an absent one falls back to the
 * block's {@code NBT_MATERIAL} column through
 * {@link GTBasicMachineBlock#materialOf} — the same white {@code 0xFFFFFF} fallback the
 * material-less rows keep (upstream UNCOLORED, CS.java:327). The vanilla tint pipeline is
 * the same multiplication over the quad colour, so returning the row colour here IS the
 * upstream result, and full-alpha white stays numerically the vanilla {@code -1} no-tint
 * sentinel.
 *
 * <p>The material dispatch is domain-gated: only the block carriers that mirror upstream
 * NBT_MATERIAL rows resolve a material (the machine blocks, the Oven ladder, the burning
 * boxes) — every other block registered over this lambda (the P23 barrel co-registration)
 * resolves {@code null} and keeps the white identity BYTE-IDENTICAL, so P23's
 * "unpainted barrel = zero visual change" contract survives untouched. Every other tint
 * index returns {@code -1} (no tint).
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from GTClientHandlers under the
 * dist guard; the registration faces are unchanged, the P22 carrier ruling stands).
 */
@OnlyIn(Dist.CLIENT)
public final class GTMachinePaintTint {

	/**
	 * Upstream UNCOLORED = 0xFFFFFF (CS.java:327) — the MATERIAL-LESS fallback: white
	 * multiplies the grayscale texture unchanged (Paintable:50 field default; the row
	 * materials render their fRGBaSolid instead since task
	 * p27-machine-material-tint-fidelity).
	 */
	public static final int UNPAINTED = 0xFFFFFF;

	private GTMachinePaintTint() {
	}

	/**
	 * The pure seam the tests drive (the GTWireTint.tintARGB shape): the opaque ARGB for
	 * one tint index over one snapshot and one row material. Index 0 reads
	 * {@link GTModelProperties#PAINT} — painted = the stored colour (the spray override);
	 * unpainted = the row material's fRGBaSolid, or white when the material is null or
	 * {@code MT.NULL} (the no-tint identity). Every other index is no tint.
	 */
	public static int tintARGB(@Nullable ModelData aData, @Nullable OreDictMaterial aMaterial, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		Integer tPaint = aData == null ? null : aData.get(GTModelProperties.PAINT);
		if (tPaint != null) return 0xFF000000 | (tPaint.intValue() & UNPAINTED);
		return 0xFF000000 | GTBasicMachineBlock.materialColor(aMaterial);
	}

	/**
	 * The world-side half: registered over {@code GTMachines.paintableBlockArray()} and the
	 * barrel array (GTClientHandlers). The registration scope is unchanged — the material
	 * resolution rides the {@link GTBasicMachineBlock#materialOf} gate on the state's block,
	 * so only the NBT_MATERIAL carriers tint while unpainted. A null level/pos or a missing
	 * BE is the no-tint sentinel — which IS full-alpha white (see the class doc identity).
	 */
	public static BlockColor blockColor() {
		return (BlockState aState, @Nullable BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) -> {
			if (aTintIndex != 0 || aLevel == null || aPos == null) return -1;
			BlockEntity tBE = aLevel.getBlockEntity(aPos);
			OreDictMaterial tMaterial = aState == null ? null : GTBasicMachineBlock.materialOf(aState.getBlock());
			return tBE == null ? -1 : tintARGB(tBE.getModelData(), tMaterial, aTintIndex);
		};
	}
}
