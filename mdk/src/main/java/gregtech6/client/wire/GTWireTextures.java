package gregtech6.client.wire;

import javax.annotation.Nullable;

import gregapi.oredict.OreDictMaterial;

/**
 * The wire texture-set derivation (task wire-family-w2) — the MC-free single source for
 * "which borrowed {@code materialicons/<set>/wire.png} does this material render with".
 * Deliberately OUTSIDE the datagen class ({@code GT6BlockStates} pulls the Forge
 * BlockStateProvider statics, which bootstrap-gate the offline test JVM) and outside the
 * client listener (datagen consumes it too — {@code GT6BlockStates.addWireFamily} and
 * {@code addPrefixBlocks} share the exact same expression).
 *
 * <p>Semantics (upstream OreDictMaterial.java:252 + TextureSet.java:188): the first
 * {@code mTextureSetsBlock} entry, lower-snaked (the {@code GTMaterialItems.snakeCase}
 * form, the P8 borrow layout); an empty/blank list = {@code "none"} = upstream SET_NONE
 * (the setless Superconductor wire row lands there).
 */
public final class GTWireTextures {

	/** The SET_NONE fallback name (also a borrowed directory). */
	public static final String NONE_SET = "none";

	private GTWireTextures() {
	}

	/** The material's block texture-set name, lower-snaked; empty list → {@link #NONE_SET}. */
	public static String blockSetOf(@Nullable OreDictMaterial aMaterial) {
		if (aMaterial == null) return NONE_SET;
		java.util.List<String> tSets = aMaterial.mTextureSetsBlock;
		return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
				? NONE_SET
				: gregtech6.registry.GTMaterialItems.snakeCase(tSets.get(0));
	}

	/** The borrowed wire texture id of one set: {@code gt6:block/materialicons/<set>/wire}. */
	public static net.minecraft.resources.ResourceLocation wireSprite(String aSet) {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "block/materialicons/" + aSet + "/wire");
	}

	/**
	 * The pipeSide art folder of one material (task pipe-render-closeout) — the zero
	 * parallel-table SET derivation ({@link #blockSetOf}) collapsed by the upstream
	 * byte-identity census: {@code pipeSide.png} is the SAME grayscale art in every
	 * texture set except WOOD and RUBBER (upstream snapshot sha256: {@code 93307398…}
	 * shared, {@code eb13d1a4…} WOOD, {@code f7dc337a…} RUBBER — the set folders were
	 * populated by the {@code copy_into_all.bat}-style scripts), so the art folder
	 * resolves to {@code wood} / {@code rubber} / the shared {@code copper} copy — the
	 * STONE→brick bodyTexture byte-identity remap precedent
	 * ({@code GT6CrucibleDatagen.bodyTexture}). The material colour rides the tint
	 * chain (tintindex 0), the same mRGBa seat as every materialicons domain.
	 */
	public static String pipeArtSetOf(@Nullable OreDictMaterial aMaterial) {
		String tSet = blockSetOf(aMaterial);
		if (tSet.equals("wood")) return "wood";
		if (tSet.equals("rubber")) return "rubber";
		return "copper";
	}

	/** The borrowed pipe-side base id of one material: {@code gt6:block/materialicons/<art set>/pipe_side}. */
	public static net.minecraft.resources.ResourceLocation pipeSideSprite(@Nullable OreDictMaterial aMaterial) {
		String tPath = "block/materialicons/" + pipeArtSetOf(aMaterial) + "/pipe_side";
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tPath);
	}

	/**
	 * The borrowed connected-arm art id of one pipe row (task pipe-render-closeout):
	 * {@code gt6:block/materialicons/<art set>/pipe_<size>} — the upstream arm selector
	 * verbatim ({@code getIconIndexConnected}, TileEntityBase10ConnectorRendered.java:265:
	 * {@code <0.37 pipeTiny, <0.49 pipeSmall, <0.74 pipeMedium, <0.99 pipeLarge, else
	 * pipeHuge} over the diameter in blocks = px/16 — so the PX_P diameters 4/6/8/12/16
	 * map tiny/small/medium/large/huge and the quadruple/nonuple rows (PX_P[16]) ride
	 * huge like upstream). The per-diameter arts collapse by the same byte-identity
	 * census as {@link #pipeSideSprite} (each {@code pipe<Size>.png} hashes one art in
	 * every set except WOOD/LEAF and RUBBER — assets/README.md), so the art set is the
	 * shared {@link #pipeArtSetOf} dispatch.
	 */
	public static net.minecraft.resources.ResourceLocation pipeArmSprite(@Nullable OreDictMaterial aMaterial, int aDiameterPx) {
		String tSize = aDiameterPx < 5 ? "tiny" : aDiameterPx < 7 ? "small" : aDiameterPx < 9 ? "medium"
				: aDiameterPx < 13 ? "large" : "huge";
		String tPath = "block/materialicons/" + pipeArtSetOf(aMaterial) + "/pipe_" + tSize;
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tPath);
	}

	/**
	 * The untinted pipe outline band ({@code pipeSide_OVERLAY}): byte-identical
	 * (in fact empty — all 256 px alpha 0) across EVERY upstream set — and the
	 * per-diameter {@code pipe<Size>_OVERLAY.png} arts hash that SAME empty PNG
	 * (all 41 sets × all 8 sizes, assets/README.md) — so one id serves every base and
	 * arm quad; the rod model's inflated twins and the JSON decal bands ride it
	 * unchanged (the tex-pipe-textures two-pass form).
	 */
	public static final net.minecraft.resources.ResourceLocation PIPE_SIDE_OVERLAY_SPRITE =
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "block/materialicons/copper/pipe_side_overlay");

	/**
	 * The laser family's fixed texture pair (task wire-fiber-texture) — the borrowed
	 * FIBER_WIRE icons (MultiTileEntityWireLaser.java:121-122; upstream
	 * {@code textures/blocks/iconsets/FIBER_WIRE[_OVERLAY].png}, lowercased on borrow,
	 * assets/README.md). The base carries the dye through tint index 0, the overlay is
	 * untinted — {@code BlockTextureMulti(BlockTextureDefault(FIBER_WIRE, mRGBa),
	 * BlockTextureDefault(FIBER_WIRE_OVERLAY))}, no glow layer.
	 */
	public static net.minecraft.resources.ResourceLocation fiberSprite() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "block/iconsets/fiber_wire");
	}

	/** The untinted overlay half of the laser pair ({@code FIBER_WIRE_OVERLAY}). */
	public static net.minecraft.resources.ResourceLocation fiberOverlaySprite() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "block/iconsets/fiber_wire_overlay");
	}
}
