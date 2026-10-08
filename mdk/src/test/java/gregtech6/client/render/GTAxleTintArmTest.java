/**
 * Task axle-tint-arm pins: the 44 axle rows consume their row-material tint — the WORLD
 * arm inside {@link GTRodBakedModel#getDynamicQuads} (the consumer-side slug dispatch,
 * GTMachinePaintTint zero-touch) and the INVENTORY half
 * {@link GTRodBakedModel#axleRowTintARGB} registered over the full AXLE_ITEMS map
 * (GTClientHandlers). The value math itself is GTMachinePaintTintTest's face; the pins
 * here prove the axle family routes through it (Steel/wood pairwise sampling — the
 * all-gray regression killer, the #18 lesson) and that the census walks all 44 rows.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.client.model.data.ModelData;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.energy.GTAxleBlock;
import gregtech6.client.GTClientHandlers;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

import gregapi.data.ANY;
import gregapi.data.MT;

public class GTAxleTintArmTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the row materials resolve through the material system refill (the
		// GTMachinePaintTintTest shape — bare-JVM MT statics are null without it)
		GTMaterialItems.initMaterials();
	}

	// ------------------------------------------------------------------
	// fixtures (the GTMachinePaintTintTest.kitchenBlock / GTRodShapeTest shapes)
	// ------------------------------------------------------------------

	/** The registry write window for direct block construction (intrusive holders). */
	private static void unfreezeBlockRegistry() {
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/** The offline axle carrier over the real AXLE_SPECS row (properties irrelevant to the material gate). */
	private static GTAxleBlock axle(String aSlug, int aSizeIndex) {
		unfreezeBlockRegistry();
		return new GTAxleBlock(BlockBehaviour.Properties.of(),
				GT6Kinetics.AXLE_SPECS.stream().filter(tSpec -> tSpec.material().equals(aSlug)).findFirst().orElseThrow(),
				aSizeIndex);
	}

	/**
	 * The block-registration write window (the GT6MachineBlockItemTest.BlockLatch shape):
	 * a live BlockItem registration binds its block delegate, so the fixture axle must be
	 * a REGISTERED block. Unarmed = the fixture tests telemetry-skip (the known ItemLatch
	 * dual-leg asymmetry).
	 */
	private static final class BlockLatch {
		static final sun.misc.Unsafe UNSAFE;
		static final long LOCKED_OFFSET;
		static final long FROZEN_OFFSET;
		static final boolean ARMED;
		static {
			sun.misc.Unsafe tUnsafe = null;
			long tLocked = -1, tFrozen = -1;
			boolean tArmed = true;
			try {
				java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tUnsafeField.setAccessible(true);
				tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
				Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass();
				try {
					tLocked = tUnsafe.objectFieldOffset(GTOfflineTestBase.findNestedField(tClass, "locked"));
				} catch (NoSuchFieldException ignored) {
					// the 21.1 shape: no 'locked' gate anywhere on the chain — 'frozen' is the sole write guard
				}
				tFrozen = tUnsafe.objectFieldOffset(GTOfflineTestBase.findNestedField(tClass, "frozen"));
			} catch (Throwable ignored) {
				tArmed = false; // the fallback leg (no registered fixtures)
			}
			UNSAFE = tUnsafe;
			LOCKED_OFFSET = tLocked;
			FROZEN_OFFSET = tFrozen;
			ARMED = tArmed;
		}
	}

	/** The axle fixture under a UNIQUE fixture key (block first — the delegate binding — then the item). */
	private static GTAxleBlock registeredAxle(String aSlug, int aSizeIndex, String aFixtureKey) {
		org.junit.jupiter.api.Assumptions.assumeTrue(BlockLatch.ARMED,
				"the offline block-registry latch is unreachable on this JVM");
		GTAxleBlock tAxle;
		BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
				BlockLatch.FROZEN_OFFSET, false);
		if (BlockLatch.LOCKED_OFFSET != -1) BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
				BlockLatch.LOCKED_OFFSET, false);
		try {
			tAxle = new GTAxleBlock(BlockBehaviour.Properties.of(),
					GT6Kinetics.AXLE_SPECS.stream().filter(tSpec -> tSpec.material().equals(aSlug)).findFirst().orElseThrow(),
					aSizeIndex);
			net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					ResourceLocation.fromNamespaceAndPath("gt6", aFixtureKey), tAxle);
		} finally {
			BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					BlockLatch.FROZEN_OFFSET, true);
			if (BlockLatch.LOCKED_OFFSET != -1) BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
					BlockLatch.LOCKED_OFFSET, true);
		}
		return tAxle;
	}

	/** The X-rod state form (the maskOf contract arm). */
	private static BlockState axleX(GTAxleBlock aAxle) {
		return aAxle.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
	}

	/** The rod model over the identity sprite stub — a fresh instance per call (fresh retint table). */
	private static GTRodBakedModel model() {
		return new GTRodBakedModel(new GTDynamicBakedModelTest.StubFallback(),
				new GTRodBakedModel.Params(ResourceLocation.fromNamespaceAndPath("gt6", "block/axle"), List.of(), 6),
				null, aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
	}

	/** The ABGR view of an ARGB tint (the #14 slot convention — the baked COLOR slot layout). */
	private static int abgrOf(int aArgb) {
		return (aArgb & 0xFF00FF00) | ((aArgb & 0xFF) << 16) | ((aArgb >> 16) & 0xFF);
	}

	/** The baked COLOR slot of vertex 0 (stride 8, slot 3 — the GTMachineTintModelTest layout). */
	private static int vertexColour(BakedQuad aQuad) {
		return aQuad.getVertices()[3];
	}

	// ------------------------------------------------------------------
	// the consumer-side slug dispatch
	// ------------------------------------------------------------------

	/** The slug dispatch: the sampled bySlug rows, the three-row wood/alloy tail, and all 11 resolve. */
	@Test
	public void axleCarriersResolveTheRowMaterial() {
		assertSame(ANY.Steel, GTRodBakedModel.axleMaterialOf(axle("steel", 3)),
				"the steel row rides ANY.Steel (the GT6Boilers slug convention, GT6Kinetics :457)");
		assertSame(MT.Bronze, GTRodBakedModel.axleMaterialOf(axle("bronze", 0)),
				"the bySlug delegation covers the shared slugs");
		assertSame(MT.WoodTreated, GTRodBakedModel.axleMaterialOf(axle("wood_treated", 0)),
				"the wooden rows ride MT.WoodTreated (Loader :1662-1666, the axle-local tail)");
		assertSame(MT.Iritanium, GTRodBakedModel.axleMaterialOf(axle("titanium_iridium", 0)),
				"the Iritanium alloy row (the titanium_iridium slug)");
		assertSame(MT.Trinitanium, GTRodBakedModel.axleMaterialOf(axle("trinitanium", 0)),
				"the Trinitanium row (:1748-1752)");
		// every one of the 11 material rows resolves — a missing arm shows up gray
		for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) {
			assertNotNull(GTRodBakedModel.axleMaterialOf(axle(tSpec.material(), 0)),
					"the " + tSpec.material() + " row must resolve its material");
		}
	}

	// ------------------------------------------------------------------
	// the world consumption pin
	// ------------------------------------------------------------------

	/** THE consumption pin: the world arm routes the state's carrier through the single tintARGB decision site. */
	@Test
	public void theWorldArmConsumesTheRowMaterialTint() {
		List<BakedQuad> tSteelQuads = model().getDynamicQuads(axleX(axle("steel", 3)),
				null, RandomSource.create(), ModelData.EMPTY, null);
		assertFalse(tSteelQuads.isEmpty());
		int tSteelSlot = abgrOf(GTMachinePaintTint.tintARGB(ModelData.EMPTY, ANY.Steel, 0));
		for (BakedQuad tQuad : tSteelQuads) {
			assertEquals(-1, tQuad.getTintIndex(), "the retinted copies carry tintIndex -1 (no double multiply)");
			assertEquals(tSteelSlot, vertexColour(tQuad), "the baked vertex colour IS the steel row product");
		}
		// pairwise (the all-gray lesson): the wood row colours differently, through the same seam
		int tWoodSlot = vertexColour(model().getDynamicQuads(axleX(axle("wood_treated", 0)),
				null, RandomSource.create(), ModelData.EMPTY, null).get(0));
		assertEquals(abgrOf(GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.WoodTreated, 0)), tWoodSlot,
				"the wood row rides the WoodTreated fRGBaSolid product");
		assertTrue(tSteelSlot != tWoodSlot, "steel gray-white vs wood brown — pairwise distinct");
		// the warm pin: the wood tint keeps R > B in the ABGR slot (the #14 hue lesson)
		assertTrue((tWoodSlot & 255) > ((tWoodSlot >> 16) & 255), "the wood brown keeps R > B");
		// the spray-paint override: a painted axle's PAINT snapshot wins over the row colour
		ModelData tPainted = GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(0xFF0000)).build();
		assertEquals(abgrOf(0xFFFF0000), vertexColour(model().getDynamicQuads(axleX(axle("steel", 3)),
				null, RandomSource.create(), tPainted, null).get(0)), "painted wins over the steel row colour");
	}

	/** The item form stays raw: the stateless render path carries tintindex 0 for the ItemColor half. */
	@Test
	public void theItemFormStaysRawForTheItemColorHalf() {
		List<BakedQuad> tItem = model().getDynamicQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
		assertFalse(tItem.isEmpty());
		for (BakedQuad tQuad : tItem) {
			assertEquals(0, tQuad.getTintIndex(), "the item form carries raw tintindex 0 — the ItemColor tints exactly once");
		}
		assertEquals(GTRodBakedModel.ITEM_MASK, GTRodBakedModel.maskOf(null), "the item mask stays the N-S segment");
	}

	// ------------------------------------------------------------------
	// the inventory half + the census
	// ------------------------------------------------------------------

	/** The ItemColor face: the fixture stack tints its row colour; off-family/index arms stay the sentinel. */
	@Test
	public void theItemColorFaceAnswersTheRowColour() {
		// the lambda reads only the BlockItem's block — the fixture keys never collide with a real path
		GTComposedNameItem tSteel = registerItemFixture("axle_tint_fixture_steel",
				() -> new GTComposedNameItem(registeredAxle("steel", 3, "axle_tint_fixture_steel"), new Item.Properties()));
		assertEquals(GTMachinePaintTint.tintARGB(null, ANY.Steel, 0),
				GTRodBakedModel.axleRowTintARGB(new ItemStack(tSteel), 0), "the steel axle stack tints the row colour");
		GTComposedNameItem tWood = registerItemFixture("axle_tint_fixture_wood",
				() -> new GTComposedNameItem(registeredAxle("wood_treated", 0, "axle_tint_fixture_wood"), new Item.Properties()));
		assertEquals(GTMachinePaintTint.tintARGB(null, MT.WoodTreated, 0),
				GTRodBakedModel.axleRowTintARGB(new ItemStack(tWood), 0), "the wooden axle stack tints WoodTreated");
		// the guards: non-zero index and the off-family stack stay the no-tint sentinel
		assertEquals(-1, GTRodBakedModel.axleRowTintARGB(new ItemStack(tSteel), 1), "index 1 is the no-tint sentinel");
		assertEquals(-1, GTRodBakedModel.axleRowTintARGB(new ItemStack(Items.STICK), 0), "an off-family stack is the sentinel");
	}

	/** The row walk the registration seam must cover 1:1 (the GT6KineticsTabCensusTest spec-table posture). */
	private static List<String> rowWalk() {
		List<String> rWalk = new ArrayList<>();
		for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) {
			for (int tSize = 0; tSize < GT6Kinetics.AXLE_DIAMETERS.length; tSize++) {
				rWalk.add(GT6Kinetics.axleName(tSpec.material(), tSize));
			}
		}
		return rWalk;
	}

	/** The walk arithmetic: 11 materials x 4 diameters, unique paths (both legs, offline-safe). */
	@Test
	public void theRowWalkCoversFiftyTwoUniquePaths() {
		List<String> tWalk = rowWalk();
		assertEquals(52, tWalk.size(), "13 materials x 4 diameters (the AXLE_SPECS ruling + the Trinaquadalloy/Adamantium tail rows, task material-mc-d-powertrain-rows)");
		assertEquals(52, new HashSet<>(tWalk).size(), "the row paths are unique");
	}

	/** THE census: the handler seam IS the full axle item map, 1:1 with the walk — a missed row renders gray. */
	@Test
	public void theItemColorRegistrationCoversEveryAxleRow() {
		List<String> tWalk = rowWalk();
		org.junit.jupiter.api.Assumptions.assumeFalse(GT6Kinetics.AXLE_ITEMS.isEmpty(),
				"the axle item map never filled in this JVM (no mod construct) — the walk pin above still holds");
		List<String> tSeam = GTClientHandlers.axleRowTintItems().stream()
				.map(tHandle -> tHandle.getId().getPath()).toList();
		assertEquals(GT6Kinetics.AXLE_ITEMS.size(), tSeam.size(), "the seam is the full map, no hand-copied subset");
		assertEquals(new HashSet<>(tWalk), new HashSet<>(tSeam), "the ItemColor registration covers the 44 rows 1:1");
	}
}
