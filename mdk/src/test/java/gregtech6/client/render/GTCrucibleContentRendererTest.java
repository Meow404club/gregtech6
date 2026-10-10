/**
 * Offline pins for task crucible-formed-vanish-probe — the "formed crucible renders
 * transparently invisible" report's renderer-side contract. The static faces (blockstate
 * variants, the formed design-4 model, the formation chain) were already cleared by the
 * f969e03a3 delimitation; what this card adds is the never-throw contract on the render
 * path and the material-scan pin that keeps it honest:
 * <ol>
 * <li>the FULL {@code MATERIAL_ARRAY} universe (exactly {@code GT6Crucibles.materialById}'s
 *     domain — the BER's only material source; the crucible row materials are all in it)
 *     answers the molten arm through the defensive wrap without a throw, the wrap is
 *     IDENTICAL to the raw dispatch on valid materials (belt-and-braces must not change
 *     the art), and every answer parses through {@link GT6CrucibleDatagen#loc} — the sprite
 *     face the BER, the Jade element (GT6ContentFaceElement's ResourceLocation ctor) and
 *     the bowl tint listener all consume;</li>
 * <li>a hostile input (null slipping past the materialById gate — impossible via render,
 *     which null-checks, but the wrap is the last line) degrades to the fixed gray
 *     {@link GT6CrucibleDatagen#SOLID_CONTENT} instead of propagating: vanilla gives a BER
 *     throw no silent-vanish path to hide in — tryRender rethrows as a ReportedException
 *     (BlockEntityRenderDispatcher.java:95-103), the LevelRenderer "blockentities" loop
 *     (:1224-1265) has no catch, so the frame crashes; the chunk-rebuild leg queues a
 *     delayed crash ("Batching chunks", ChunkRenderDispatcher.java:123-127) and keeps the
 *     previous mesh (:545-551, compiled set on success only). A crash report, never a
 *     transparent multiblock — which is why the wrap degrades the face instead.</li>
 * </ol>
 * The bake-wrap alternative suspect was cleared during the same census: GTRenderModelListener /
 * GTMachineTintModel / GTDynamicBakedModel carry zero catch clauses, so there is no
 * swallow-to-empty-model mechanism either.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6CrucibleDatagen;
import gregtech6.registry.GT6MaterialTestSupport;

public class GTCrucibleContentRendererTest extends GTOfflineRenderTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the scan walks the material universe (the solid-face-matrix shape)
		GT6MaterialTestSupport.materials();
	}

	/** The BER material domain scan: every material answers, wraps transparently, parses into the sprite path. */
	@Test
	void everyMaterialAnswersTheMoltenArmAndParsesIntoTheSpritePath() {
		int tCount = 0;
		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_ARRAY) {
			if (tMaterial == null) continue;
			GT6CrucibleDatagen.ContentFace tFace = assertDoesNotThrow(
					() -> GTCrucibleContentRenderer.contentFaceSafely(tMaterial),
					tMaterial.mNameInternal + " (#" + tMaterial.mID + "): the BER render path must never see a throw");
			assertEquals(GT6CrucibleDatagen.contentFace(tMaterial, true), tFace,
					tMaterial.mNameInternal + ": the defensive wrap must be transparent on valid materials");
			assertDoesNotThrow(() -> GT6CrucibleDatagen.loc(tFace.texture()),
					tMaterial.mNameInternal + ": the sprite face must parse (the BER/Jade-element consumption)");
			assertTrue(tFace.texture().startsWith("gt6:block/materialicons/") && tFace.texture().endsWith("/molten"),
					tMaterial.mNameInternal + ": the molten arm answers a materialicons molten sprite, got " + tFace.texture());
			tCount++;
		}
		assertTrue(tCount >= 300, "the universe walk broke — never pass vacuously (" + tCount + ")");
	}

	/** The fallback pin: a failed lookup degrades to the fixed gray face, never propagates (one log line — not asserted, the face is the contract). */
	@Test
	void aFailedLookupFallsBackToTheSolidFaceInsteadOfThrowing() {
		assertThrows(NullPointerException.class, () -> GT6CrucibleDatagen.contentFace(null, true),
				"precondition: the raw molten arm does throw on a hostile material — the wrap is what absorbs it");
		assertEquals(GT6CrucibleDatagen.SOLID_CONTENT, GTCrucibleContentRenderer.contentFaceSafely(null),
				"the wrap must answer the fixed gray face, never propagate");
	}
}
