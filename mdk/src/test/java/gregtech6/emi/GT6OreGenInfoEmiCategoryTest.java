/**
 * Offline tests for task debt-ore-gen-display: the EMI leg's page surface — the category
 * id (the JEMI skip key twin), the row face (id stability, the no-tree ruling, the
 * text-band widget layout through the recording {@link dev.emi.emi.api.widget.WidgetHolder}
 * double — the batch1 GT6RecipeMapEmiCategoryTest form; {@code TextWidget.getBounds()} is
 * client-bound so only the COUNT is assertable, the coordinates ride the shared seam's
 * public constants), and the registration walk shape.
 */
package gregtech6.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregapi.data.MT;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.GT6OreGenInfoLayout;
import gregtech6.worldgen.OreDistributionInfo;

public class GT6OreGenInfoEmiCategoryTest {

	@BeforeAll
	static void bootVanillaOffline() {
		GTMaterialItems.initMaterials();
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@Test
	public void categoryIdMirrorsTheJeiUid() {
		assertEquals("gt6:ore_gen_info", GT6OreGenInfoEmiCategory.CATEGORY.getId().toString(),
				"the EMI category id must mirror the JEI uid one-to-one (the JEMI skip key)");
		// the title rides the translatable seam (task debt-oregen-title-i18n): the key face
		// is pinned, the en/zh values live in the generated lang faces (GT6LangParityTest)
		assertTrue(GT6OreGenInfoEmiCategory.CATEGORY.getName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents,
				"the page title must be translatable, not a literal");
		assertEquals(GT6OreGenInfoLayout.TITLE_KEY,
				((net.minecraft.network.chat.contents.TranslatableContents) GT6OreGenInfoEmiCategory.CATEGORY.getName().getContents()).getKey());
	}

	@Test
	public void rowFaceCarriesTheDeterministicIdAndNoTree() {
		OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
		GT6OreGenInfoEmiRecipe tFace = new GT6OreGenInfoEmiRecipe(tCassiterite, 7);
		assertEquals("gt6:ore_gen_info/7", tFace.getId().toString());
		assertTrue(tFace.getCategory() == GT6OreGenInfoEmiCategory.CATEGORY);
		assertTrue(tFace.getInputs().isEmpty(), "the page is information — no inputs");
		assertFalse(tFace.supportsRecipeTree(), "no tree face (the batch1 ruling)");
		assertEquals(GT6OreGenInfoLayout.WIDTH, tFace.getDisplayWidth());
		assertEquals(GT6OreGenInfoLayout.categoryHeight(), tFace.getDisplayHeight());
		// the outputs ride the leg's registry state (see the layout test): forge offline =
		// the EMPTY degenerate face, neoforge = the FULL 76-stack invisible mounting
		//? if forge {
		assertTrue(tFace.getOutputs().isEmpty(), "offline: no registries, the invisible mounting stays empty");
		//?} else {
		/*assertEquals(GT6OreGenInfoLayout.variantPaths(tCassiterite).size(), tFace.getOutputs().size(),
				"the invisible mounting resolves in full when the registries are live");
		 *///?}
	}

	/**
	 * The text band: Cassiterite's dims row + 6 face lines = 7 text widgets; the
	 * representative slot rides the leg's registry state (forge offline draws none — EMPTY
	 * stack; neoforge draws one at the shared SLOT_X/SLOT_Y, the JEI twin's geometry).
	 */
	@Test
	public void addWidgetsLaysDimsRowPlusFaceLines() {
		OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
		RecordingHolder tHolder = new RecordingHolder();
		new GT6OreGenInfoEmiRecipe(tCassiterite, 0).addWidgets(tHolder);
		//? if forge {
		assertEquals(0, tHolder.mSlots, "offline: the representative stack is EMPTY, no slot");
		//?} else {
		/*assertEquals(1, tHolder.mSlots, "the representative slot draws when the registries are live");
		 *///?}
		assertEquals(7, tHolder.mTexts, "dims row + 6 face lines (3 small + 2 vein + 1 bedrock)");
		// the dims row sits above the face band (21 vs 38) — the constants keep their roles
		assertTrue(GT6OreGenInfoLayout.DIMS_Y < GT6OreGenInfoLayout.FACE_BASE_Y);
	}

	@Test
	public void ferberiteRowIsDimsPlusOneBedrockLine() {
		OreDistributionInfo.Entry tFerberite = OreDistributionInfo.of(MT.OREMATS.Ferberite);
		RecordingHolder tHolder = new RecordingHolder();
		new GT6OreGenInfoEmiRecipe(tFerberite, 117).addWidgets(tHolder);
		//? if forge {
		assertEquals(0, tHolder.mSlots, "offline: no slot");
		//?} else {
		/*assertEquals(1, tHolder.mSlots, "the bedrock LARGE representative resolves on the live leg");
		 *///?}
		assertEquals(2, tHolder.mTexts, "dims row + the one bedrock line");
	}

	/** The registration walk: one recipe per entry, ids index-stable (the data layer's first-appearance order). */
	@Test
	public void registrationWalkShapeIsOneRowPerEntry() {
		List<OreDistributionInfo.Entry> tEntries = OreDistributionInfo.entries();
		assertEquals(118, tEntries.size());
		for (int i = 0; i < tEntries.size(); i++) {
			assertEquals("gt6:ore_gen_info/" + i,
					new GT6OreGenInfoEmiRecipe(tEntries.get(i), i).getId().toString());
		}
	}

	/** The recording double: {@code add} is the one funnel; slots vs texts split by widget kind, Bounds never read. */
	private static final class RecordingHolder implements WidgetHolder {
		int mSlots;
		int mTexts;

		@Override
		public int getWidth() {
			return GT6OreGenInfoLayout.WIDTH;
		}

		@Override
		public int getHeight() {
			return GT6OreGenInfoLayout.categoryHeight();
		}

		@Override
		public <T extends Widget> T add(T aWidget) {
			if (aWidget instanceof dev.emi.emi.api.widget.SlotWidget) {
				mSlots++;
			} else if (aWidget instanceof dev.emi.emi.api.widget.TextWidget) {
				mTexts++;
			} else {
				throw new AssertionError("unexpected widget kind: " + aWidget.getClass());
			}
			return aWidget;
		}
	}
}
