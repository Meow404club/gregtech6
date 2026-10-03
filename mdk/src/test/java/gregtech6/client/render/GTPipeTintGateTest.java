/**
 * Task r11a-pipe-tint-gate-fix pins: the user-reported "every pipe renders white" — an
 * UNPAINTED plain pipe BE hands EMPTY ModelData in (TileEntityBase03TicksAndSync
 * .getModelData: unpainted → EMPTY; GTFluidPipeBlockEntity:812 plain → super), so the
 * rod model's pipe tint gate (the ModelData-emptiness test) never opened and the raw
 * grayscale pipe_side art rendered white. These pins drive the GTRodBakedModel pipe arm
 * with EMPTY ModelData and demand the row-material colour product (the
 * GTMachinePaintTint.tintARGB seam) — red while the gate stands, green once the arm is
 * unconditional (the axle-tint-arm :192 precedent and the GTMachineTintModel :100-102
 * wrap form). The painted-pipe PAINT override and the material-less wire identity are
 * the unchanged-behaviour controls.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.client.model.data.ModelData;

import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.pipe.GTItemPipeBlock;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

import gregapi.data.MT;

public class GTPipeTintGateTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the row materials resolve through the material system refill (the
		// GTMachinePaintTintTest shape — bare-JVM MT statics are null without it)
		GTMaterialItems.initMaterials();
	}

	// ------------------------------------------------------------------
	// fixtures (the GTAxleTintArmTest shapes)
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

	/** The fluid pipe fixture over the real ROWS table (properties irrelevant to the tint gate). */
	private static GTFluidPipeBlock fluidPipe(String aSlug) {
		GTFluidPipes.FluidPipeRow tRow = GTFluidPipes.ROWS.stream()
				.filter(tR -> tR.material().slug().equals(aSlug)).findFirst().orElseThrow();
		unfreezeBlockRegistry();
		return new GTFluidPipeBlock(tRow, BlockBehaviour.Properties.of());
	}

	/** The item pipe fixture over the real ROWS table. */
	private static GTItemPipeBlock itemPipe(String aSlug) {
		GTItemPipes.ItemPipeRow tRow = GTItemPipes.ROWS.stream()
				.filter(tR -> tR.material().slug().equals(aSlug)).findFirst().orElseThrow();
		unfreezeBlockRegistry();
		return new GTItemPipeBlock(tRow, BlockBehaviour.Properties.of());
	}

	/** The rod model over the identity sprite stub — carrier block seated, no overlays. */
	private static GTRodBakedModel model(Block aCarrier) {
		return new GTRodBakedModel(new GTDynamicBakedModelTest.StubFallback(),
				new GTRodBakedModel.Params(ResourceLocation.fromNamespaceAndPath("gt6", "block/pipe"), List.of(), 8),
				aCarrier, aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
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
	// the red nail: the unpainted pipe world arm
	// ------------------------------------------------------------------

	/** THE red nail: an unpainted pipe (EMPTY ModelData) must bake the row material colour. */
	@Test
	public void theUnpaintedPipeWorldArmCarriesTheRowMaterialColour() {
		GTFluidPipeBlock tWood = fluidPipe("wood");
		assertSame(MT.Wood, GTFluidPipeBlock.materialOf(tWood),
				"the wooden row rides MT.Wood (the 26000 loader line, GTFluidPipeBlock.materialOf)");
		int tWoodSlot = abgrOf(GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Wood, 0));
		List<BakedQuad> tQuads = model(tWood).getDynamicQuads(
				tWood.defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0),
				null, RandomSource.create(), ModelData.EMPTY, null);
		assertFalse(tQuads.isEmpty());
		for (BakedQuad tQuad : tQuads) {
			assertEquals(-1, tQuad.getTintIndex(), "the retinted copies carry tintIndex -1 (no double multiply)");
			assertEquals(tWoodSlot, vertexColour(tQuad),
					"the EMPTY-ModelData unpainted pipe bakes the row material colour (the white regression killer)");
		}
		// the warm pin: the wood tint keeps R > B in the ABGR slot (the #14 hue lesson)
		assertTrue((tWoodSlot & 255) > ((tWoodSlot >> 16) & 255), "the wood brown keeps R > B");
		// pairwise (the all-gray lesson): the copper row colours differently, same seam
		GTFluidPipeBlock tCopper = fluidPipe("copper");
		int tCopperSlot = vertexColour(model(tCopper).getDynamicQuads(
				tCopper.defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0),
				null, RandomSource.create(), ModelData.EMPTY, null).get(0));
		assertEquals(abgrOf(GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Cu, 0)), tCopperSlot,
				"the copper row rides the Cu fRGBaSolid product");
		assertNotEquals(tWoodSlot, tCopperSlot, "wood brown vs copper orange — pairwise distinct");
	}

	/** The item pipe BE hands EMPTY in too (the same 03 base) — the same gate covered it white. */
	@Test
	public void theItemPipeWorldArmRidesTheRowMaterialToo() {
		GTItemPipeBlock tBrass = itemPipe("brass");
		assertSame(MT.Brass, GTItemPipeBlock.materialOf(tBrass),
				"the brass item pipe row resolves MT.Brass (Loader :1823-1843 line)");
		int tBrassSlot = abgrOf(GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Brass, 0));
		List<BakedQuad> tQuads = model(tBrass).getDynamicQuads(
				tBrass.defaultBlockState().setValue(GTItemPipeBlock.CONNECTIONS, 0),
				null, RandomSource.create(), ModelData.EMPTY, null);
		assertFalse(tQuads.isEmpty());
		for (BakedQuad tQuad : tQuads) {
			assertEquals(-1, tQuad.getTintIndex(), "the retinted copies carry tintIndex -1 (no double multiply)");
			assertEquals(tBrassSlot, vertexColour(tQuad), "the item pipe bakes its row material colour");
		}
	}

	// ------------------------------------------------------------------
	// the unchanged-behaviour controls
	// ------------------------------------------------------------------

	/** CONTROL (green before and after): a painted pipe's PAINT snapshot still wins. */
	@Test
	public void thePaintedPipeWorldArmKeepsThePaintOverride() {
		GTFluidPipeBlock tWood = fluidPipe("wood");
		ModelData tPainted = GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(0xFF0000)).build();
		List<BakedQuad> tQuads = model(tWood).getDynamicQuads(
				tWood.defaultBlockState().setValue(GTFluidPipeBlock.CONNECTIONS, 0),
				null, RandomSource.create(), tPainted, null);
		assertFalse(tQuads.isEmpty());
		for (BakedQuad tQuad : tQuads) {
			assertEquals(-1, tQuad.getTintIndex(), "the retinted copies carry tintIndex -1 (no double multiply)");
			assertEquals(abgrOf(0xFFFF0000), vertexColour(tQuad), "the PAINT snapshot wins over the row colour");
		}
		// the paint face is not the unpainted identity — they are different products
		assertNotEquals(abgrOf(GTMachinePaintTint.tintARGB(ModelData.EMPTY, MT.Wood, 0)), abgrOf(0xFFFF0000),
				"paint red vs wood brown — the override is observable");
	}

	/** CONTROL: the material-less carrier (the logistics wire posture) stays the -1 white identity. */
	@Test
	public void theMateriallessCarrierStaysTheNoTintSentinel() {
		// the wire block extends GTEntityBlock with no carrier arm → tintMaterialOf answers
		// null → 0xFFFFFFFF IS the vanilla no-tint sentinel — the unconditional gate hands
		// it to tintQuads, which returns the input list unchanged (GTMachineTintModel :114)
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 0),
				"material null = the white identity — the wire rows must not start carrying a colour");
	}
}
