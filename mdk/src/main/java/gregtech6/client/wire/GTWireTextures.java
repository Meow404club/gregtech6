package gregtech6.client.wire;

import javax.annotation.Nullable;

import gregapi.oredict.OreDictMaterial;

/**
 * The wire texture-set derivation (task p9-wire-family-w2) — the MC-free single source for
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
		return new net.minecraft.resources.ResourceLocation("gt6", "block/materialicons/" + aSet + "/wire");
	}

	/**
	 * The laser family's fixed texture pair (task p11-wire-fiber-texture) — the borrowed
	 * FIBER_WIRE icons (MultiTileEntityWireLaser.java:121-122; upstream
	 * {@code textures/blocks/iconsets/FIBER_WIRE[_OVERLAY].png}, lowercased on borrow,
	 * assets/README.md). The base carries the dye through tint index 0, the overlay is
	 * untinted — {@code BlockTextureMulti(BlockTextureDefault(FIBER_WIRE, mRGBa),
	 * BlockTextureDefault(FIBER_WIRE_OVERLAY))}, no glow layer.
	 */
	public static net.minecraft.resources.ResourceLocation fiberSprite() {
		return new net.minecraft.resources.ResourceLocation("gt6", "block/iconsets/fiber_wire");
	}

	/** The untinted overlay half of the laser pair ({@code FIBER_WIRE_OVERLAY}). */
	public static net.minecraft.resources.ResourceLocation fiberOverlaySprite() {
		return new net.minecraft.resources.ResourceLocation("gt6", "block/iconsets/fiber_wire_overlay");
	}

	/** The legacy p7 placeholder texture (the material-less anchors). */
	public static net.minecraft.resources.ResourceLocation legacySprite() {
		return new net.minecraft.resources.ResourceLocation("gt6", "block/wire_electric");
	}
}
