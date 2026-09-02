package gregtech6.covers;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import gregtech6.client.render.GTRenderSnapshot;

/**
 * The immutable cover render snapshot — the {@link GTRenderSnapshot} record the host BE
 * hands to the render thread (task p4-cover-core ⑥). Carries the per-face cover sprite
 * id ({@code getCoverTextureSurface}, the ICover :194 hook) PLUS the per-face LAYER
 * TABLE (task p11-render-cover-multilayer): every face value is now an ordered
 * {@code List<ResourceLocation>} — the upstream {@code BlockTextureMulti} stack, bottom
 * first — instead of the single sprite that folded the double-layer covers flat.
 *
 * <p><b>Why the layer table lives here</b> (the census, task p11-render-cover-multilayer):
 * upstream renders each cover's 2px plate box with the cover's ATTACHMENT texture
 * (TileEntityBase06Covers.java:449-463 — the odd per-cover pass boxes
 * {@code BOXES_COVERS} and paints {@code getCoverTextureAttachment} on the plate's
 * front/back faces), and every port-registered cover returns a two-layer multi there:
 * {@code BlockTextureMulti.get(BACKGROUND_COVER, fg)} with
 * {@code BACKGROUND_COVER = "machines/covers/base"} (AbstractCoverDefault.java:111) —
 * controller :62, auto-controller :64, shutter :88, filter :140, conveyor :84,
 * robot arm :111, pump :88, conductors :32/:60, the iron plate via CoverTextureSimple
 * :50, the emitter via the attachment wrap :112 over its surface multi :111. The single
 * exception is the cover controller, whose own background is
 * {@code "machines/covers/coverswitch/base"} (CoverControllerCovers.java:101/:104), not
 * the shared base. The port's single-sprite snapshot folded that background away — the
 * {@link #UNDERLAYS} census table restores it as layer 0 beneath the surface sprite.
 * Pure single-layer faces (a sprite with no census entry) stay exactly one layer, so
 * the plate plan for them is byte-identical to the pre-p11 planner.
 *
 * <p>Layer order = upstream {@code BlockTextureMulti} order: the background first, the
 * surface sprite last (the top layer). {@link #sprite(Direction)} keeps returning the
 * surface (top) sprite the producer handed in, so the record's legacy view
 * ({@code coverSprites()} — the /gt6cover command surface) is unchanged; the plate
 * model reads {@link #layers(Direction)} and offsets each layer by
 * {@code epsilon * layerIndex} against z-fighting (CoverPlateModel.PLATE_EPSILON).
 * No tint carries in the census layers: every upstream layer here is an untinted
 * {@code BlockTextureDefault} (a tinted layer would need a {@code Layer(sprite, tint)}
 * pair — none exists in the registered set, so the plain sprite list is the honest shape).
 *
 * <p>Thread safety per the snapshot contract: {@link Direction} keys are JVM
 * singletons, {@link ResourceLocation} values and the layer lists are immutable, and
 * both maps are frozen by {@code Map.copyOf} before entering {@code ModelData} (the
 * ModelData.java:28 iron law).
 *
 * <p>GTCEu 对位：ICoverableRenderer.renderCovers (ICoverableRenderer.java:43-91) 从
 * COVER_MODEL_DATA 拿每面数据；我们把同形数据提前冻进快照，getQuads 只读不查世界。
 */
public record GTCoverRenderSnapshot(Map<Direction, ResourceLocation> coverSprites,
		Map<Direction, List<ResourceLocation>> layers) implements GTRenderSnapshot {

	/** The plate background sprite — upstream {@code machines/covers/base.png} (AbstractCoverDefault.java:111 BACKGROUND_COVER), borrowed byte-identical (see assets README). */
	public static final ResourceLocation SPRITE_PLATE_BASE = new ResourceLocation("gt6", "block/covers/base");

	/** The cover controller's own background — upstream {@code machines/covers/coverswitch/base.png} (CoverControllerCovers.java:104 sTextureBackground), NOT the shared base. */
	public static final ResourceLocation SPRITE_COVER_SWITCH_BASE = new ResourceLocation("gt6", "block/cover_switch/base");

	/**
	 * The census table (task p11-render-cover-multilayer): surface sprite id → the
	 * underlay sprite rendered beneath it on the plate, each entry pinned to its upstream
	 * {@code BlockTextureMulti} line in the class doc. Keys mirror the borrowed fg
	 * texture paths (assets README); the test suite pins the table against the cover
	 * classes' own sprite constants so the two cannot drift.
	 */
	private static final Map<ResourceLocation, ResourceLocation> UNDERLAYS = buildUnderlays();

	public GTCoverRenderSnapshot {
		coverSprites = Map.copyOf(coverSprites); // second freeze line of defense beside the builder
		if (layers == null) {
			// the producer shape: derive the layer table from the census
			Map<Direction, List<ResourceLocation>> tDerived = new EnumMap<>(Direction.class);
			for (Map.Entry<Direction, ResourceLocation> tEntry : coverSprites.entrySet()) {
				tDerived.put(tEntry.getKey(), layersOf(tEntry.getValue()));
			}
			layers = Map.copyOf(tDerived);
		} else {
			// the explicit shape (test/future-producer seam): freeze the caller's lists too
			Map<Direction, List<ResourceLocation>> tFrozen = new EnumMap<>(Direction.class);
			for (Map.Entry<Direction, List<ResourceLocation>> tEntry : layers.entrySet()) {
				tFrozen.put(tEntry.getKey(), List.copyOf(tEntry.getValue()));
			}
			layers = Map.copyOf(tFrozen);
		}
	}

	/** The producer shape (TileEntityOven.getModelData / TileEntityBase08Barrel) — the census derives the layers. */
	public GTCoverRenderSnapshot(Map<Direction, ResourceLocation> aCoverSprites) {
		this(aCoverSprites, null);
	}

	/** Whether the given face carries a cover sprite. */
	public boolean hasCover(Direction aFace) {
		return coverSprites.containsKey(aFace);
	}

	/** The GT6 side byte ({@code Direction.get3DDataValue()}) order mask of the covered faces. */
	public byte mask() {
		byte rMask = 0;
		for (Direction tFace : coverSprites.keySet()) rMask |= (byte) (1 << tFace.get3DDataValue());
		return rMask;
	}

	/** @return the surface (top-layer) cover sprite on that face or null — the value the producer handed in. */
	public ResourceLocation sprite(Direction aFace) {
		return coverSprites.get(aFace);
	}

	/**
	 * The face's layer table, bottom first (upstream BlockTextureMulti order), or an
	 * empty list when the face carries no cover. Immutable.
	 */
	public List<ResourceLocation> layers(Direction aFace) {
		return layers.getOrDefault(aFace, List.of());
	}

	/**
	 * The census lookup: the layer stack of one surface sprite — {@code [underlay, sprite]}
	 * for a census hit, {@code [sprite]} (unchanged single layer) otherwise. Pure and
	 * static so the offline tests can pin it against the cover classes' constants.
	 */
	public static List<ResourceLocation> layersOf(ResourceLocation aSurfaceSprite) {
		ResourceLocation tUnderlay = UNDERLAYS.get(aSurfaceSprite);
		return tUnderlay == null ? List.of(aSurfaceSprite) : List.of(tUnderlay, aSurfaceSprite);
	}

	/** @return the underlay beneath that surface sprite or null (single-layer sprite). */
	public static ResourceLocation underlayOf(ResourceLocation aSurfaceSprite) {
		return UNDERLAYS.get(aSurfaceSprite);
	}

	/** The census table — each entry cites its upstream BlockTextureMulti line (see the class doc census). */
	private static Map<ResourceLocation, ResourceLocation> buildUnderlays() {
		Map<ResourceLocation, ResourceLocation> rMap = new HashMap<>();
		// the shared BACKGROUND_COVER base (AbstractCoverDefault.java:111) under every plain-fg cover:
		rMap.put(new ResourceLocation("gt6", "block/redstone_switch/circuit"), SPRITE_PLATE_BASE);      // CoverControllerRedstone :62 (fg :65)
		rMap.put(new ResourceLocation("gt6", "block/auto_redstone_switch/circuit"), SPRITE_PLATE_BASE);  // CoverControllerAutoRedstone :64 (fg :67)
		rMap.put(new ResourceLocation("gt6", "block/shutter/normal"), SPRITE_PLATE_BASE);                // CoverShutter :88 (fg :93)
		rMap.put(new ResourceLocation("gt6", "block/shutter/inverted"), SPRITE_PLATE_BASE);              // CoverShutter :88 (fg :94)
		rMap.put(new ResourceLocation("gt6", "block/filteritem/normal"), SPRITE_PLATE_BASE);             // CoverFilterItem :140 (fg :145)
		rMap.put(new ResourceLocation("gt6", "block/filteritem/inverted"), SPRITE_PLATE_BASE);           // CoverFilterItem :140 (fg :146)
		rMap.put(new ResourceLocation("gt6", "block/conveyor/in"), SPRITE_PLATE_BASE);                   // CoverConveyor :84 (fg :93)
		rMap.put(new ResourceLocation("gt6", "block/conveyor/out"), SPRITE_PLATE_BASE);                  // CoverConveyor :84 (fg :94)
		rMap.put(new ResourceLocation("gt6", "block/robotarm/in"), SPRITE_PLATE_BASE);                   // CoverRobotArm :111 (fg :119)
		rMap.put(new ResourceLocation("gt6", "block/robotarm/out"), SPRITE_PLATE_BASE);                  // CoverRobotArm :111 (fg :120)
		rMap.put(new ResourceLocation("gt6", "block/cover_pump_in"), SPRITE_PLATE_BASE);                 // CoverPump :88 (fg :96/:97)
		rMap.put(new ResourceLocation("gt6", "block/cover_pump_out"), SPRITE_PLATE_BASE);                // CoverPump :88 (fg :96/:97)
		rMap.put(new ResourceLocation("gt6", "block/redstone_conductor/in"), SPRITE_PLATE_BASE);         // CoverRedstoneConductorIN :32 (fg :35)
		rMap.put(new ResourceLocation("gt6", "block/redstone_conductor/out"), SPRITE_PLATE_BASE);        // CoverRedstoneConductorOUT :60 (fg :63)
		rMap.put(new ResourceLocation("gt6", "item/material_sets/metallic/plate"), SPRITE_PLATE_BASE);   // the iron plate cover (CoverTextureSimple :50; GT6Covers.ironPlateSprite)
		for (int i = 0; i < 16; i++) {
			// the emitter's 16 composed tier sprites (CoverRedstoneEmitter :112 wraps the surface multi in BACKGROUND_COVER; the tier PNGs already fold underlay+digit)
			rMap.put(new ResourceLocation("gt6", "block/redstone_emitter/" + i), SPRITE_PLATE_BASE);
		}
		// the cover controller's OWN background (CoverControllerCovers :101 with sTextureBackground :104):
		rMap.put(new ResourceLocation("gt6", "block/cover_switch/circuit"), SPRITE_COVER_SWITCH_BASE);
		return Map.copyOf(rMap);
	}
}
