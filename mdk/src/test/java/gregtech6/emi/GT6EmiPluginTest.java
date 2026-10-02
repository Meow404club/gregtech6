/**
 * Offline guard tests for task debt-emi-tier-b: the EMI plugin's detection contract, the
 * shared info-text seam (the copy-paste red line), and the material_tool replacement
 * semantics — the EMI runtime itself cannot run offline, so the assertions land on the
 * exact seams the plugin consumes, the same layer strategy as GT6JeiPluginTest /
 * GT6MaterialToolJeiExtensionTest.
 *
 * <p>The JEMI red line context (why completeness is guarded): once this plugin ships,
 * EMI's JEMI bridge skips every gt6 JEI face (handledNamespaces), so the two JEI faces
 * MUST live here natively — the info key share test pins face 1's text seam, the
 * invalidator/output tests pin face 2's replacement. In-game EMI-only / EMI+JEI
 * verification stays with the card's field_test gate (runClient with EMI, no JEI
 * installed — the offline limit declared by the acceptance).
 */
package gregtech6.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;

import gregapi.data.MT;
import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;
import gregtech6.items.tools.GT6MaterialToolRecipe;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.jei.GT6RecipeViewerText;

public class GT6EmiPluginTest {

	/**
	 * Minimal vanilla offline bootstrap (the GTRecipesOfflineTestBase.bootVanillaOffline
	 * shape): the direct-construction rows touch {@code Items}, whose clinit walks the
	 * vanilla registry chain that needs the version/registry bootstrap. No JSON is
	 * parsed here, so the CraftingHelper ingredient-serializer registration the recipes
	 * base carries is deliberately NOT copied.
	 */
	@org.junit.jupiter.api.BeforeAll
	static void bootVanillaOffline() {
		// the material table must exist before any row() dereference (the same
		// "MT/OP must exist before any field dereference" order the ore/tint tests pin —
		// materialBySnake returns NULL without it and the stamp lands identity-less)
		gregtech6.registry.GTMaterialItems.initMaterials();
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	@Test
	public void noArgInstantiation() {
		// the EMI detection contract: a bare no-arg construction must succeed
		GT6EmiPlugin tPlugin = new GT6EmiPlugin();
		assertNotNull(tPlugin);
	}

	@Test
	public void declaredConstructorIsNoArg() throws Exception {
		// reflection face — what EMI's annotation scan instantiation requires
		assertEquals(0, GT6EmiPlugin.class.getDeclaredConstructor().getParameterCount());
	}

	@Test
	public void implementsEmiPlugin() {
		assertTrue(EmiPlugin.class.isAssignableFrom(GT6EmiPlugin.class));
	}

	@Test
	public void emiEntrypointAnnotationInBytecode() throws Exception {
		// @EmiEntrypoint carries NO @Retention meta-annotation = default CLASS retention:
		// invisible to runtime reflection — assert at the layer EMI's ModList scan reads,
		// the compiled bytecode (the same strategy as the JEI twin test).
		try (java.io.InputStream in = GT6EmiPlugin.class.getResourceAsStream("GT6EmiPlugin.class")) {
			assertNotNull(in, "plugin class resource not found on the test classpath");
			String tBytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertTrue(tBytes.contains("Ldev/emi/emi/api/EmiEntrypoint;"),
					"@EmiEntrypoint annotation missing from the class file — EMI would never detect the plugin");
		}
	}

	@Test
	public void infoKeySharedBetweenTheTwoViewers() throws Exception {
		// Face 1's seam: the info page text must be the ONE shared key (holder), not an
		// EMI-side copy — a silent rename on either side would orphan the other's page.
		// Since task multiblock-preview-infra the key's consumer is the preview widget's
		// description line (the text-info pages are gone from both plugins); the literal
		// share pin stays, and the EMI plugin's face is the preview category instead.
		assertEquals("gt6.jei.info.multiblock_coke_oven", GT6RecipeViewerText.INFO_KEY_COKE_OVEN);
		assertEquals(GT6JeiPlugin.INFO_KEY_COKE_OVEN, GT6RecipeViewerText.INFO_KEY_COKE_OVEN,
				"the JEI forwarding constant and the holder literal must stay one seam");
		// and the plugin bytecode carries the modern replacement face; the RETIRED face is
		// the coke-oven text page specifically — since task viewer-energy-jump-gear the
		// EmiInfoRecipe API is back for the energy-carrier pages (the seat-IX rebase
		// adjudication: the retirement targeted the replaced face, not the API):
		try (java.io.InputStream in = GT6EmiPlugin.class.getResourceAsStream("GT6EmiPlugin.class")) {
			String tBytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertFalse(tBytes.contains("registerCokeOvenInfo"),
					"the EMI coke-oven text-info face must be gone (task multiblock-preview-infra)");
			assertTrue(tBytes.contains("GT6MultiblockPreviewEmiCategory"),
					"the EMI plugin must register the preview category twin");
		}
	}

	/**
	 * Face 2's output slot: the replacement row carries the ROW's stamped identity — the
	 * exact face the JEI extension pins (GT6MaterialToolJeiExtensionTest), so both viewers
	 * show the same per-material colour instead of the bare Steel fallback.
	 */
	@Test
	public void replacementRowCarriesTheStampedOutput() {
		GT6MaterialToolRecipe tIronRow = row("iron");
		ResourceLocation tId = id("screwdriver/iron");
		GT6MaterialToolEmiRecipe tFace = new GT6MaterialToolEmiRecipe(tIronRow, tId);

		assertEquals(tId, tFace.getId(), "the replacement keeps the recipe id (search/lookup identity)");
		List<EmiStack> tOutputs = tFace.getOutputs();
		assertEquals(1, tOutputs.size(), "exactly one output stack");
		assertEquals(MT.Iron,
				GT6ItemData.get(tOutputs.get(0).getItemStack(), GT6ToolStats.KEY).primaryMaterial(),
				"the output slot carries the row's stamped identity — not the bare Steel fallback");
	}

	/**
	 * The replacement count semantics: every material_tool id gets exactly one replacement
	 * — the invalidator must kill every FOREIGN recipe carrying those ids (both auto-
	 * wrapper shapes) while sparing this plugin's own faces and unrelated recipes. Two
	 * rows in, two strangers out, two own faces and the bystander survive.
	 */
	@Test
	public void invalidatorReplacesExactlyTheMaterialToolRows() {
		GT6MaterialToolRecipe tIronRow = row("iron");
		GT6MaterialToolRecipe tBronzeRow = row("bronze");
		ResourceLocation tIronId = id("screwdriver/iron");
		ResourceLocation tBronzeId = id("screwdriver/bronze");

		GT6MaterialToolEmiRecipe tIronFace = new GT6MaterialToolEmiRecipe(tIronRow, tIronId);
		GT6MaterialToolEmiRecipe tBronzeFace = new GT6MaterialToolEmiRecipe(tBronzeRow, tBronzeId);
		EmiRecipe tBystander = stranger(id("screwdriver/unrelated"));

		Predicate<EmiRecipe> tInvalidator = GT6EmiPlugin.materialToolInvalidator(Set.of(tIronId, tBronzeId));

		// the auto-wrappers EMI's VanillaPlugin builds for the two rows — must die
		assertTrue(tInvalidator.test(stranger(tIronId)), "the iron row's auto-wrapper is invalidated");
		assertTrue(tInvalidator.test(stranger(tBronzeId)), "the bronze row's auto-wrapper is invalidated");
		// this plugin's own replacements — must survive (same ids as the strangers!)
		assertFalse(tInvalidator.test(tIronFace), "the iron replacement face survives its own invalidator");
		assertFalse(tInvalidator.test(tBronzeFace), "the bronze replacement face survives its own invalidator");
		// anything else — untouched
		assertFalse(tInvalidator.test(tBystander), "an unrelated recipe is none of this plugin's business");
	}

	/** The wrapper shape EMI's VanillaPlugin auto-builds for the row on this leg. */
	//? if forge {
	private static EmiRecipe stranger(ResourceLocation aId) {
		// the forge-leg generic tail: a plain EmiCraftingRecipe (material_tool is not a
		// ShapedRecipe on this leg, so the shaped dispatch misses)
		return new dev.emi.emi.api.recipe.EmiCraftingRecipe(
				List.of(dev.emi.emi.api.stack.EmiIngredient.of(
						net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK))),
				EmiStack.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)),
				aId);
	}
	//?} else {
	/*private static EmiRecipe stranger(ResourceLocation aId) {
		// The neoforge-leg wrapper is EmiShapedRecipe — but its ctor is client-bound
		// (EmiPort.getOutput reads Minecraft.getInstance().level.registryAccess(), NPE
		// offline), so the offline stranger is a minimal EmiRecipe double carrying just
		// the id: the invalidator's contract is exactly "(any non-own recipe, id match)",
		// which the double exercises at the same layer the forge leg's real
		// EmiCraftingRecipe stranger does (that ctor IS offline-safe there).
		return new EmiRecipe() {
			@Override public dev.emi.emi.api.recipe.EmiRecipeCategory getCategory() { return null; }
			@Override public ResourceLocation getId() { return aId; }
			@Override public List<dev.emi.emi.api.stack.EmiIngredient> getInputs() { return List.of(); }
			@Override public List<EmiStack> getOutputs() { return List.of(); }
			@Override public int getDisplayWidth() { return 0; }
			@Override public int getDisplayHeight() { return 0; }
			@Override public void addWidgets(dev.emi.emi.api.widget.WidgetHolder aWidgets) { }
		};
	}
	*///?}

	private static ResourceLocation id(String aPath) {
		//? if forge {
		return new ResourceLocation("gt6", aPath);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
		*///?}
	}

	/** The 2x2 screwdriver-grid row (the screwdriver/iron.json shape) — built by DIRECT construction on this leg: no JSON parse means no CraftingHelper ingredient-serializer bootstrap (the hidden fork-order dependency the JEI twin test rides on), this class stays self-contained. */
	//? if forge {
	private static GT6MaterialToolRecipe row(String aMaterial) {
		net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> tPattern =
				net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK),
						net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK),
						net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK),
						net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK));
		net.minecraft.world.item.crafting.ShapedRecipe tShaped =
				new net.minecraft.world.item.crafting.ShapedRecipe(id("screwdriver/" + aMaterial), "",
						net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
						2, 2, tPattern, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
		return new GT6MaterialToolRecipe(tShaped, gregtech6.items.tools.GT6ToolLadder.materialBySnake(aMaterial));
	}
	//?} else {
	/*private static GT6MaterialToolRecipe row(String aMaterial) {
		java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = java.util.Map.of(
				'S', net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK),
				'h', net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK),
				'f', net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK));
		return new GT6MaterialToolRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
				net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, java.util.List.of("hS", "Sf")),
				new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK), true, aMaterial);
	}
	*///?}
}
