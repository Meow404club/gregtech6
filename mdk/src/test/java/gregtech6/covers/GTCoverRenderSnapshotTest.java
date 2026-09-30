package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverAsphalt;
import gregtech6.covers.covers.CoverConveyor;
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerCovers;
import gregtech6.covers.covers.CoverControllerRedstone;
import gregtech6.covers.covers.CoverCrafting;
import gregtech6.covers.covers.CoverDrain;
import gregtech6.covers.covers.CoverFilterFluid;
import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverPressureValve;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverRedstoneConductorIN;
import gregtech6.covers.covers.CoverRedstoneConductorOUT;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.covers.covers.CoverRobotArm;
import gregtech6.covers.covers.CoverSelectorTag;
import gregtech6.covers.covers.CoverShutter;
import gregtech6.covers.covers.CoverVent;

/**
 * The cover snapshot LAYER TABLE contract (task render-cover-multilayer): the
 * census map (every port-registered surface sprite carries the upstream
 * {@code BlockTextureMulti} underlay — see the {@link GTCoverRenderSnapshot} class
 * doc), the layer order (background bottom, surface top), the record roundtrip over
 * both construction shapes and the freeze guarantee.
 */
public class GTCoverRenderSnapshotTest {

	private static final ResourceLocation UNMAPPED = new ResourceLocation("gt6", "block/cover/test_unmapped");

	/**
	 * The census test is the first suite member to class-load {@link GT6Covers} — whose
	 * static init builds the {@code DeferredRegister(ForgeRegistries.ITEMS)} and whose
	 * ForgeRegistries chain (ForgeRegistries.java:65 static {@code init()} over
	 * RegistryManager.ACTIVE) touches the vanilla registries. Bootstrap first, exactly
	 * the {@code GTCoverTestBase.buildCoverOvenFixture} pattern, or an early executor
	 * order poisons the shared JVM with a failed {@code BuiltInRegistries} init for
	 * every later class.
	 */
	@BeforeAll
	static void bootstrapRegistries() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the iron-plate census row walks the material DB (GT6Covers.ironPlateSprite → MT.Iron) —
		// initMaterials here (the GTWireDisplayNameTest boot shape) so the class is self-sufficient
		// instead of leaning on a sibling suite class to have initialized the materials first.
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	// ---------------------------------------------------------------------------
	// the census — every registered cover sprite maps, the cover controller maps to ITS OWN base
	// ---------------------------------------------------------------------------

	@Test
	void censusCoversEveryRegisteredCoverSurfaceSprite() {
		// the shared BACKGROUND_COVER base (AbstractCoverDefault.java:111) — plain-fg covers
		ResourceLocation[][] tSharedBase = {
				{CoverControllerRedstone.sprite(), CoverControllerAutoRedstone.sprite()},
				{CoverShutter.SPRITE_NORMAL, CoverShutter.SPRITE_INVERTED},
				{CoverFilterItem.SPRITE_WHITELIST, CoverFilterItem.SPRITE_BLACKLIST},
				{CoverConveyor.CONVEYOR_OUT_SPRITE, CoverConveyor.CONVEYOR_IN_SPRITE},
				{CoverRobotArm.ROBOT_ARM_OUT_SPRITE, CoverRobotArm.ROBOT_ARM_IN_SPRITE},
				{CoverPump.PUMP_OUT_SPRITE, CoverPump.PUMP_IN_SPRITE},
				{CoverRedstoneConductorIN.sprite(), CoverRedstoneConductorOUT.sprite()},
				{GT6Covers.ironPlateSprite()},
		};
		for (ResourceLocation[] tRow : tSharedBase) {
			for (ResourceLocation tFg : tRow) {
				assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, GTCoverRenderSnapshot.underlayOf(tFg),
						"the shared plate base sits under " + tFg);
			}
		}
		// the emitter's 16 composed tier sprites (the tier PNGs already fold underlay+digit)
		for (int i = 0; i < 16; i++) {
			assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, GTCoverRenderSnapshot.underlayOf(CoverRedstoneEmitter.spriteForTier(i)),
					"the emitter tier " + i + " carries the plate base");
		}
		// the single exception: the cover controller's own background (CoverControllerCovers :101/:104)
		assertSame(GTCoverRenderSnapshot.SPRITE_COVER_SWITCH_BASE, GTCoverRenderSnapshot.underlayOf(CoverControllerCovers.sprite()),
				"the cover controller maps to coverswitch/base, NOT the shared base");
	}

	@Test
	void unmappedSpriteStaysSingleLayer() {
		assertNull(GTCoverRenderSnapshot.underlayOf(UNMAPPED), "no census entry → no underlay");
		assertEquals(List.of(UNMAPPED), GTCoverRenderSnapshot.layersOf(UNMAPPED), "the single-layer sprite keeps its flat stack");
	}

	@Test
	void layersOrderIsBaseBottomSurfaceTop() {
		assertEquals(List.of(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, CoverControllerRedstone.sprite()),
				GTCoverRenderSnapshot.layersOf(CoverControllerRedstone.sprite()),
				"the stack mirrors the upstream BlockTextureMulti.get(BACKGROUND_COVER, fg) order");
	}

	/**
	 * The cover-underlay-census widening (task cover-underlay-census): the seven gameplay
	 * covers join the census — the four true {@code BACKGROUND_COVER} wraps upstream
	 * (fluid filter CoverFilterFluid :132, tag selector CoverSelectorTag :60, crafting
	 * CoverCrafting → CoverTextureMulti :71, asphalt CoverTextureSimple :50) plus the
	 * three facet covers (vent :77 / drain :246 / pressure valve :80), which wrap no base
	 * upstream but pair the host wall under the art on the flush-host surface pass
	 * (TileEntityBase06Covers :453) — the port's plate replaces that host face, so the
	 * base stands in for it. Keys are anchored on the covers' own surface output (the
	 * class constants, or a fresh instance's hook where the sprite is built inline), so
	 * the census cannot drift from the classes.
	 *
	 * <p>The ⑤-B double-form check rides here: upstream 1.7.10 items are ALSO flat icons
	 * (the plate art as one plane), while the INSTALLED plate is the two-layer
	 * base+art stack — after this widening the port's item face (the untouched 2D
	 * {@code item/generated} model, pinned by GT6ItemFormCensusTest) and its installed
	 * face are naturally distinct, which is exactly the user-reported relief-valve
	 * "installed == item icon" defect closing.
	 */
	@Test
	void gameplaySevenJoinTheUnderlayCensus() {
		// the four attachment-wrap covers — every mode/variant sprite maps to the shared base
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(CoverFilterFluid.SPRITE_WHITELIST), "the fluid-filter whitelist carries the base");
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(CoverFilterFluid.SPRITE_BLACKLIST), "the fluid-filter blacklist carries the base");
		for (int i = 0; i < 16; i++) {
			assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
					GTCoverRenderSnapshot.underlayOf(new CoverSelectorTag((byte) i).getCoverTextureSurface((byte) 0, null)),
					"tag selector mode " + i + " carries the base (upstream :60 wraps the composed sprite in BACKGROUND_COVER)");
		}
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(CoverCrafting.CRAFTING_SPRITE), "the crafting plate carries the base");
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(CoverAsphalt.ASPHALT_SPRITE), "the asphalt plate carries the base");
		// the three facet covers — the surface hook output anchors the census key
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(new CoverVent().getCoverTextureSurface((byte) 0, null)), "the vent carries the base");
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(new CoverDrain().getCoverTextureSurface((byte) 0, null)), "the drain carries the base");
		assertSame(GTCoverRenderSnapshot.SPRITE_PLATE_BASE,
				GTCoverRenderSnapshot.underlayOf(new CoverPressureValve().getCoverTextureSurface((byte) 0, null)),
				"the pressure valve carries the base");
		// and the derived layer stack is the full two-layer plate
		assertEquals(List.of(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, CoverAsphalt.ASPHALT_SPRITE),
				GTCoverRenderSnapshot.layersOf(CoverAsphalt.ASPHALT_SPRITE));
	}

	/**
	 * The facet table (task cover-underlay-census): the vent is the one faceted family —
	 * its surface sprite maps to the upstream back/sides pair (CoverVent :78-79), every
	 * other census sprite stays facet-free (null). The back/rim sprites are the borrowed
	 * verbatim PNGs (assets README, the vent facet section).
	 */
	@Test
	void ventIsTheFacetedFamily() {
		ResourceLocation tVentFront = new CoverVent().getCoverTextureSurface((byte) 0, null);
		GTCoverRenderSnapshot.Facets tFacets = GTCoverRenderSnapshot.facetsOf(tVentFront);
		assertEquals(new ResourceLocation("gt6", "block/vent/back"), tFacets.back(), "upstream :78 — the attachment back face");
		assertEquals(new ResourceLocation("gt6", "block/vent/sides"), tFacets.rim(), "upstream :78/:79 — the rim/holder faces");
		assertNull(GTCoverRenderSnapshot.facetsOf(new CoverDrain().getCoverTextureSurface((byte) 0, null)),
				"the drain's facet dispatch is the declared follow-up, not this card");
		assertNull(GTCoverRenderSnapshot.facetsOf(CoverControllerRedstone.sprite()), "the flat-plate family stays facet-free");
	}

	// ---------------------------------------------------------------------------
	// the record roundtrip — producer shape, explicit shape, equality, freeze
	// ---------------------------------------------------------------------------

	@Test
	void snapshotValueRoundtripThroughBothConstructionShapes() {
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		tSprites.put(Direction.UP, CoverControllerRedstone.sprite());
		tSprites.put(Direction.NORTH, UNMAPPED);

		GTCoverRenderSnapshot tFromProducer = new GTCoverRenderSnapshot(tSprites);
		// layers: the census-expanded value, bottom first
		assertEquals(List.of(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, CoverControllerRedstone.sprite()), tFromProducer.layers(Direction.UP));
		assertEquals(List.of(UNMAPPED), tFromProducer.layers(Direction.NORTH), "the unmapped face stays single-layer");
		assertEquals(List.of(), tFromProducer.layers(Direction.DOWN), "an uncovered face reads as an empty layer table");
		// the legacy view stays the surface sprite (the /gt6cover command surface, GTCoverCommand.coverSprites)
		assertEquals(CoverControllerRedstone.sprite(), tFromProducer.sprite(Direction.UP));
		assertTrue(tFromProducer.hasCover(Direction.UP));
		assertEquals((byte) ((1 << Direction.UP.get3DDataValue()) | (1 << Direction.NORTH.get3DDataValue())), tFromProducer.mask());

		// the explicit canonical shape with a null layer table derives the same value
		GTCoverRenderSnapshot tFromCanonicalNull = new GTCoverRenderSnapshot(tSprites, null);
		assertEquals(tFromProducer, tFromCanonicalNull, "canonical(null) derives the census layers");
		// and with the layers spelled out equals the derived record
		GTCoverRenderSnapshot tFromCanonicalExplicit = new GTCoverRenderSnapshot(tSprites,
				Map.of(Direction.UP, List.of(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, CoverControllerRedstone.sprite()),
						Direction.NORTH, List.of(UNMAPPED)));
		assertEquals(tFromProducer, tFromCanonicalExplicit, "the explicit layer table matches the census derivation");
	}

	@Test
	void snapshotFreezesTheLayerTablesAgainstCallerMutation() {
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		tSprites.put(Direction.UP, CoverControllerRedstone.sprite());
		Map<Direction, List<ResourceLocation>> tLayers = new HashMap<>();
		tLayers.put(Direction.UP, new java.util.ArrayList<>(List.of(GTCoverRenderSnapshot.SPRITE_PLATE_BASE, CoverControllerRedstone.sprite())));

		GTCoverRenderSnapshot tSnapshot = new GTCoverRenderSnapshot(tSprites, tLayers);
		tSprites.put(Direction.DOWN, UNMAPPED); // the sprite map was copied at construction
		tLayers.get(Direction.UP).add(UNMAPPED); // the caller's layer list was copied too
		assertFalse(tSnapshot.hasCover(Direction.DOWN), "the record holds a frozen copy of the sprite map");
		assertEquals(2, tSnapshot.layers(Direction.UP).size(), "the record holds a frozen copy of the layer list");
		try {
			tSnapshot.layers(Direction.UP).add(UNMAPPED);
			throw new AssertionError("the layer list must be immutable");
		} catch (UnsupportedOperationException tExpected) {
			// List.of contract
		}
	}
}
