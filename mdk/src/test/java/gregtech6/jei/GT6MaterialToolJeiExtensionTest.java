/**
 * Offline tests for task r3-jei-tool-output-tint (GitHub #6 round 3): the JEI crafting
 * category extension for {@code gt6:material_tool} rows. The JEI runtime itself cannot
 * run offline, so the assertions land on the exact seam the extension consumes —
 * {@link GT6MaterialToolRecipe#stampedDisplayResult()} (the output-slot stack) and the
 * shaped-layout accessors; the in-game JEI screenshot acceptance stays with the card's
 * visual gate (research.issues-r3-tool-jei).
 *
 * <p>Pinned values are the #6 round-3 acceptance colours: the iron row's head pass =
 * Iron (200,200,200) and handle pass = the Spruce secondary default (102,79,47); the
 * bare {@code getResultItem} face stays on the Steel fallback (130,130,130) — that
 * contrast IS the bug report, and the reason the extension exists.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;
import gregtech6.items.tools.GT6MaterialToolRecipe;
import gregtech6.items.tools.GT6ToolLadder;

public class GT6MaterialToolJeiExtensionTest {

	/**
	 * The output-slot face: the display stack carries the ROW's material identity (not
	 * the bare stack JEI rendered before), and the four-pass tint the output icon will
	 * now show follows it — two material rows, two head colours (the #6 report), handle
	 * passes on the Spruce default.
	 */
	@Test
	public void stampedDisplayResultCarriesTheRowIdentity() {
		GT6MaterialToolRecipe tIronRow = row("iron");
		GT6MaterialToolRecipe tBronzeRow = row("bronze");
		ItemStack tIronDisplay = tIronRow.stampedDisplayResult();
		ItemStack tBronzeDisplay = tBronzeRow.stampedDisplayResult();

		assertSame(MT.Iron, GT6ItemData.get(tIronDisplay, GT6ToolStats.KEY).primaryMaterial(),
				"the iron row's display stack carries the row identity (not bare)");
		assertSame(MT.Bronze, GT6ItemData.get(tBronzeDisplay, GT6ToolStats.KEY).primaryMaterial(),
				"the bronze row's display stack carries the row identity");

		assertEquals(0xFFC8C8C8, GT6ToolLadder.fourPassTintARGB(tIronDisplay, 0),
				"iron row head pass = Iron (200,200,200) — the round-3 acceptance value");
		assertEquals(0xFF664F2F, GT6ToolLadder.fourPassTintARGB(tIronDisplay, 2),
				"iron row handle pass = the Spruce default (102,79,47)");
		assertEquals(argb(MT.Bronze), GT6ToolLadder.fourPassTintARGB(tBronzeDisplay, 0),
				"bronze row head pass = Bronze");
		assertNotEquals(GT6ToolLadder.fourPassTintARGB(tIronDisplay, 0),
				GT6ToolLadder.fourPassTintARGB(tBronzeDisplay, 0),
				"two material rows must show two different head colours — the #6 report");
	}

	/**
	 * The stamp lives on a COPY and the crafting semantics stay untouched: the bare
	 * {@code getResultItem} face keeps the identity-less Steel fallback both before and
	 * after a display call — that face is what the vanilla recipe book still shows, and
	 * the exact pre-fix JEI symptom this extension supersedes.
	 */
	@Test
	public void bareResultFaceStaysIdentityLess() {
		GT6MaterialToolRecipe tIronRow = row("iron");
		tIronRow.stampedDisplayResult(); // the display call must not taint the bare face
		ItemStack tBare = bare(tIronRow);
		assertTrue(GT6ItemData.find(tBare, GT6ToolStats.KEY).isEmpty(),
				"the stamp is on a copy — the bare face stays identity-less");
		assertEquals(0xFF828282, GT6ToolLadder.fourPassTintARGB(tBare, 0),
				"the bare face keeps the Steel fallback (130,130,130) — the pre-fix JEI symptom");
	}

	/**
	 * The forge-leg layout fix: the extension (not the recipe class) carries the shaped
	 * dims, because JEI's Forge RecipeHelper reads width/height only off an
	 * IShapedRecipe (RecipeHelper.java:30-43) and this leg's wrapper deliberately does
	 * not implement it — the 2x2 grid instead of the shapeless single row.
	 */
	//? if forge {
	@Test
	public void extensionPinsTheShapedLayout() {
		GT6MaterialToolJeiExtension tExtension = new GT6MaterialToolJeiExtension(row("iron"));
		assertEquals(2, tExtension.getWidth(), "the 2x2 grid width — not the shapeless row");
		assertEquals(2, tExtension.getHeight(), "the 2x2 grid height — not the shapeless row");
		assertEquals(new net.minecraft.resources.ResourceLocation("gt6", "screwdriver/iron"),
				tExtension.getRegistryName(), "the advanced-tooltip recipe id face");
	}
	//?} else {
	/*// The neo-leg layout: nothing to override — the 19.x default dispatch reads the dims
	// off ShapedRecipe (CraftingCategoryExtension.java:36-45) and this leg's recipe
	// extends ShapedRecipe, so the superclass dims ARE the JEI layout.
	@Test
	public void shapedLayoutRidesTheShapedRecipeSuperclass() {
		GT6MaterialToolRecipe tRecipe = row("iron");
		assertNotNull(GT6MaterialToolJeiExtension.INSTANCE, "the 19.x singleton exists for registration");
		assertEquals(2, tRecipe.getWidth(), "the 2x2 dims the default dispatch reads off ShapedRecipe");
		assertEquals(2, tRecipe.getHeight(), "the 2x2 dims the default dispatch reads off ShapedRecipe");
	}
	*///?}

	private static int argb(OreDictMaterial aMaterial) {
		return 0xFF000000 | (aMaterial.mRGBaSolid[0] << 16) | (aMaterial.mRGBaSolid[1] << 8) | aMaterial.mRGBaSolid[2];
	}

	/** The 2x2 screwdriver-grid row (the screwdriver/iron.json shape) with a stick as the vanilla-item result carrier — the mod-Item wall, same as DigLadderTest. */
	//? if forge {
	private static GT6MaterialToolRecipe row(String aMaterial) {
		com.google.gson.JsonObject tJson = new com.google.gson.JsonObject();
		tJson.addProperty("category", "equipment");
		com.google.gson.JsonArray tPattern = new com.google.gson.JsonArray();
		tPattern.add("hS");
		tPattern.add("Sf");
		tJson.add("pattern", tPattern);
		com.google.gson.JsonObject tKey = new com.google.gson.JsonObject();
		com.google.gson.JsonObject tStick = new com.google.gson.JsonObject();
		tStick.addProperty("item", "minecraft:stick");
		tKey.add("S", tStick);
		tKey.add("h", tStick.deepCopy());
		tKey.add("f", tStick.deepCopy());
		tJson.add("key", tKey);
		com.google.gson.JsonObject tResult = new com.google.gson.JsonObject();
		tResult.addProperty("item", "minecraft:stick");
		tJson.add("result", tResult);
		tJson.addProperty("material", aMaterial);
		return new GT6MaterialToolRecipe.Serializer().fromJson(
				new net.minecraft.resources.ResourceLocation("gt6", "screwdriver/" + aMaterial), tJson);
	}

	private static ItemStack bare(GT6MaterialToolRecipe aRecipe) {
		return aRecipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
	}
	//?} else {
	/*private static GT6MaterialToolRecipe row(String aMaterial) {
		java.util.Map<Character, net.minecraft.world.item.crafting.Ingredient> tKey = java.util.Map.of(
				'S', net.minecraft.world.item.crafting.Ingredient.of(Items.STICK),
				'h', net.minecraft.world.item.crafting.Ingredient.of(Items.STICK),
				'f', net.minecraft.world.item.crafting.Ingredient.of(Items.STICK));
		return new GT6MaterialToolRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
				net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, java.util.List.of("hS", "Sf")),
				new ItemStack(Items.STICK), true, aMaterial);
	}

	private static ItemStack bare(GT6MaterialToolRecipe aRecipe) {
		return aRecipe.getResultItem(net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
				net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
	}
	*///?}
}
