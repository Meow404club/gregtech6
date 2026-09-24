package gregtech6.covers.covers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import gregtech6.covers.CoverData;

/**
 * The asphalt cover — 1.20.1 port of gregapi/cover/covers/CoverAsphalt.java:32-42 (task
 * p37-covers-crafting-asphalt; upstream the Asphalt Panel items, Loader_MultiTileEntities
 * .java:2053-2055, one CoverAsphalt per DYES[i] plate). Walking over the covered TOP face
 * accelerates the walker: horizontal motion ×1.3 while moving, not in water and not
 * sneaking (upstream :39 verbatim) — the pavement speed face the vanilla friction system
 * never expresses, so it rides the walk-over hook (the 1.20.1 counterpart of the upstream
 * {@code onEntityWalking} chain, MultiTileEntityBlock.java:306 → TileEntityBase06Covers
 * :428).
 *
 * <p>Declared folds: the 16 dye variants collapse into the one plate over the plain
 * {@code ASPHALT} icon (the worldgen streets ride the DYE_INDEX_Gray face — the single
 * sprite the plate renderer paints, the p35 declared-fold posture); {@code SFX.MC_DIG_ROCK}
 * maps onto {@link SoundEvents#STONE_PLACE} (the p4 vanilla-sound-mapping posture — one
 * sound for the place and crowbar-removal hooks).
 */
public class CoverAsphalt extends CoverTextureSimple {

	//? if forge {
	public static final ResourceLocation ASPHALT_SPRITE = new ResourceLocation("gt6", "block/asphalt");
	//?} else {
	/*public static final ResourceLocation ASPHALT_SPRITE = ResourceLocation.fromNamespaceAndPath("gt6", "block/asphalt");
	 *///?}

	/** The upstream :39 horizontal boost factor. */
	public static final double WALK_BOOST = 1.3;

	public CoverAsphalt() {
		super(ASPHALT_SPRITE, SoundEvents.STONE_PLACE); // SFX.MC_DIG_ROCK — see the class doc mapping
	}

	/** Upstream :38-41 verbatim — the moving/dry/not-sneaking gate, then the ×1.3 boost. */
	@Override
	public boolean onWalkOver(byte aCoverSide, CoverData aData, Entity aEntity) {
		Vec3 tMotion = aEntity.getDeltaMovement();
		if ((tMotion.x != 0 || tMotion.z != 0) && !aEntity.isInWater() && !aEntity.isShiftKeyDown()) {
			aEntity.setDeltaMovement(tMotion.x * WALK_BOOST, tMotion.y, tMotion.z * WALK_BOOST);
		}
		return true; // :40 — the walk event is consumed (the 06Covers :428 shape)
	}
}
