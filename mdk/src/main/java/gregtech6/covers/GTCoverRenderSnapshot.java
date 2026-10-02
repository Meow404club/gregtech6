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
 * hands to the render thread (task cover-core ⑥). Carries the per-face cover sprite
 * id ({@code getCoverTextureSurface}, the ICover :194 hook) PLUS the per-face LAYER
 * TABLE (task render-cover-multilayer): every face value is now an ordered
 * {@code List<ResourceLocation>} — the upstream {@code BlockTextureMulti} stack, bottom
 * first — instead of the single sprite that folded the double-layer covers flat.
 *
 * <p><b>Why the layer table lives here</b> (the census, task render-cover-multilayer;
 * widened by task cover-underlay-census): upstream renders each cover's 2px plate box
 * with the cover's ATTACHMENT texture (TileEntityBase06Covers.java:449-463 — the odd
 * per-cover pass boxes {@code BOXES_COVERS} and paints {@code getCoverTextureAttachment}
 * on the plate's front/back faces), and most port-registered covers return a two-layer
 * multi there: {@code BlockTextureMulti.get(BACKGROUND_COVER, fg)} with
 * {@code BACKGROUND_COVER = "machines/covers/base"} (AbstractCoverDefault.java:111) —
 * controller :62, auto-controller :64, shutter :88, filter :140, conveyor :84,
 * robot arm :111, pump :88, conductors :32/:60, the iron plate via CoverTextureSimple
 * :50, the emitter via the attachment wrap :112 over its surface multi :111, and (the
 * cover-underlay-census widening) the fluid filter CoverFilterFluid :132, the tag
 * selector CoverSelectorTag :60, the crafting table CoverCrafting via CoverTextureMulti
 * :71 and the asphalt plate via CoverTextureSimple :50. The single exception is the
 * cover controller, whose own background is
 * {@code "machines/covers/coverswitch/base"} (CoverControllerCovers.java:101/:104), not
 * the shared base. The three facet covers (vent :77-84, drain :246-253, pressure valve
 * :80-91) wrap NO base upstream — but their flush-host surface pass (TileEntityBase06Covers
 * :453) still paints {@code BlockTextureMulti.get(hostTexture, surface)}: the host wall
 * under the art. The port's plate replaces that host face, so the census entry stands in
 * for it — the same single-sprite-fold defect, same fix. The port's single-sprite
 * snapshot folded that background away — the
 * {@link #UNDERLAYS} census table restores it as layer 0 beneath the surface sprite.
 * Pure single-layer faces (a sprite with no census entry) stay exactly one layer, so
 * the plate plan for them is byte-identical to the pre-p11 planner.
 *
 * <p><b>The facet table</b> (task cover-underlay-census): upstream CoverVent :78-79
 * dispatches its attachment texture per texture side — the cover face gets
 * {@code vent/front}, the opposite face {@code vent/back}, the four rim faces
 * {@code vent/sides} (the holder :79 repeats the sides). The port folds all that into
 * the single surface sprite; {@link #FACETS} restores the back/rim pair, and
 * CoverPlateModel paints it on the null-pass back quad and the rim quads (single
 * sprites, upstream verbatim — no base stacks there; drain :247 shares the identical
 * three-texture shape and rides the same follow-up).
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
 * <p>GTCEu counterpart: ICoverableRenderer.renderCovers (ICoverableRenderer.java:43-91)
 * reads the per-face data out of COVER_MODEL_DATA; we freeze the same-shaped data into the
 * snapshot ahead of time, so getQuads stays read-only and never queries the world.
 */
public record GTCoverRenderSnapshot(Map<Direction, ResourceLocation> coverSprites,
		Map<Direction, List<ResourceLocation>> layers) implements GTRenderSnapshot {

	/** The plate background sprite — upstream {@code machines/covers/base.png} (AbstractCoverDefault.java:111 BACKGROUND_COVER), borrowed byte-identical (see assets README). */
	public static final ResourceLocation SPRITE_PLATE_BASE = new ResourceLocation("gt6", "block/covers/base");

	/** The cover controller's own background — upstream {@code machines/covers/coverswitch/base.png} (CoverControllerCovers.java:104 sTextureBackground), NOT the shared base. */
	public static final ResourceLocation SPRITE_COVER_SWITCH_BASE = new ResourceLocation("gt6", "block/cover_switch/base");

	/**
	 * The census table (task render-cover-multilayer): surface sprite id → the
	 * underlay sprite rendered beneath it on the plate, each entry pinned to its upstream
	 * {@code BlockTextureMulti} line in the class doc. Keys mirror the borrowed fg
	 * texture paths (assets README); the test suite pins the table against the cover
	 * classes' own sprite constants so the two cannot drift.
	 */
	private static final Map<ResourceLocation, ResourceLocation> UNDERLAYS = buildUnderlays();

	/** The facet pair of a faceted cover (upstream CoverVent.java:78-79): the attachment BACK face sprite + the rim/holder face sprite. */
	public record Facets(ResourceLocation back, ResourceLocation rim) {}

	/** The facet table — surface sprite id → the back/rim pair the plate model paints on the null/rim passes. */
	private static final Map<ResourceLocation, Facets> FACETS = buildFacets();

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

	/** @return the facet pair beneath that surface sprite or null (the plain flat-plate family). Pure and static for the offline pins. */
	public static Facets facetsOf(ResourceLocation aSurfaceSprite) {
		return FACETS.get(aSurfaceSprite);
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
		// — the cover-underlay-census widening — the attachment-wrap family (upstream wraps fg in BACKGROUND_COVER):
		rMap.put(new ResourceLocation("gt6", "block/filterfluid/normal"), SPRITE_PLATE_BASE);      // CoverFilterFluid :132 (fg :139)
		rMap.put(new ResourceLocation("gt6", "block/filterfluid/inverted"), SPRITE_PLATE_BASE);    // CoverFilterFluid :132 (fg :138)
		for (int i = 0; i < 16; i++) {
			// the tag-selector ladder (CoverSelectorTag :60 — surface = multi(selectortag/underlay, digit), wrapped in BACKGROUND_COVER;
			// the port's 16 shipped PNGs pre-composite underlay+digit, so the census base completes the 3-layer stack)
			rMap.put(new ResourceLocation("gt6", "block/selectortag/" + i), SPRITE_PLATE_BASE);
		}
		rMap.put(new ResourceLocation("gt6", "block/crafting/0"), SPRITE_PLATE_BASE);              // CoverCrafting → CoverTextureMulti :71 (fg folder :42)
		rMap.put(new ResourceLocation("gt6", "block/asphalt"), SPRITE_PLATE_BASE);                 // CoverAsphalt → CoverTextureSimple :50
		// the facet family — NO BACKGROUND_COVER wrap upstream (vent :77-84 / drain :246-253 / pressure valve :80-91 are
		// single-texture attachments), but the flush-host surface pass :453 still pairs the host wall under the art; the
		// port's plate replaces that host face, so the base stands in for it (the same single-sprite-fold defect):
		rMap.put(new ResourceLocation("gt6", "block/vent/front"), SPRITE_PLATE_BASE);              // CoverVent :77 (surface)
		rMap.put(new ResourceLocation("gt6", "block/drain/front"), SPRITE_PLATE_BASE);             // CoverDrain :246 (surface)
		rMap.put(new ResourceLocation("gt6", "block/pressurevalve/front"), SPRITE_PLATE_BASE);     // CoverPressureValve :80 (surface)
		return Map.copyOf(rMap);
	}

	/** The facet table — the one faceted family (upstream CoverVent :77-84; drain :247 shares the shape, same follow-up). */
	private static Map<ResourceLocation, Facets> buildFacets() {
		return Map.of(new ResourceLocation("gt6", "block/vent/front"),
				new Facets(new ResourceLocation("gt6", "block/vent/back"), new ResourceLocation("gt6", "block/vent/sides")));
	}
}
