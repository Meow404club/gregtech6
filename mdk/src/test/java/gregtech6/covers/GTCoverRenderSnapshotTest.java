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

import gregtech6.covers.covers.CoverConveyor;
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerCovers;
import gregtech6.covers.covers.CoverControllerRedstone;
import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverRedstoneConductorIN;
import gregtech6.covers.covers.CoverRedstoneConductorOUT;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.covers.covers.CoverRobotArm;
import gregtech6.covers.covers.CoverShutter;

/**
 * The cover snapshot LAYER TABLE contract (task p11-render-cover-multilayer): the
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
