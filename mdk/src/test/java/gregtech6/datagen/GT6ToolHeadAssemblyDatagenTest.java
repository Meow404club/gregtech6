/**
 * The tool-head assembly pin test (task r7-39-toolhead-assembly, GitHub #39 — the
 * B-lite fix): the upstream tool acquisition splits into the arg-8 DIRECT rows
 * (Loader_Tools.java:453-455 {@code CR.shaped(tTool)} — the tool) and the arg-9 HEAD
 * rows (:514-516 {@code CR.shaped(tStack)} where tStack = the listening prefix item,
 * :451). The port previously emitted TOOLS from upstream HEAD-row shapes (the
 * inversion): this test pins the repaired faces —
 *
 * <ul>
 * <li>the inverted rows (dig :294-300, sword :321, machine :305/:306/:307/:327) emit
 * {@code gt6:tool_head_<mat>} through the vanilla crafting_shaped face (the head item
 * IS the identity — no stamp field);</li>
 * <li>the direct families (wrench :310 / monkey :311 / pincers :316 / crowbar :314 /
 * cutter :324 / knife :322 / butchery :323) STAY material_tool tool rows (the
 * zero-change guard);</li>
 * <li>the 17 head+handle assembly rows (the AdvancedCraftingTool :332-350 port) ride
 * the soft_hammer precedent's stamped material_tool face, one 1x2 "H"/"S" column per
 * (form x head item).</li>
 * </ul>
 *
 * <p>DECLARED DEVIATION (the card's): the upstream ACT matches gate binds the stick to
 * the head's mHandleMaterial (AdvancedCraftingTool.java:105); the port relaxes 'S' to
 * the platform wooden-rod tag — under the modern material set an iron head demanding an
 * iron-handle stick starves most rows.
 *
 * <p>Row-count pins are the measured item-truth numbers dead-written (the craftfrom
 * ruling-A caliber): the dig walk 202 and the assembly walk 7063.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6ToolHeadAssemblyDatagenTest extends GTOfflineTestBase {

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/" + aPath + ".json";
		try (InputStream tStream = GT6ToolHeadAssemblyDatagenTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonElement key(JsonObject aRow, char aChar) {
		return aRow.getAsJsonObject("key").get(String.valueOf(aChar));
	}

	/** The head item id of one inverted row (the GTMaterialItems composition + the iron anchor). */
	private static String headItem(OreDictPrefix aPrefix, String aSnake) {
		return "gt6:" + GTMaterialItems.snakeCase(aPrefix.mNameInternal) + "_" + aSnake;
	}

	// --------------------------------------------------------------- the inverted rows

	/** The six dig rows (:294-300, the arg-9 face) emit the HEAD through vanilla shaped — never the tool. */
	@Test
	public void theDigRowsEmitHeads() throws Exception {
		for (String[] tForm : GT6CraftingRecipes.DIG_LADDER_FORMS) {
			String tId = tForm[0];
			JsonObject tRow = generated("recipes/" + tId + "/iron");
			assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString(),
					tId + ": the head row rides the vanilla serializer (the head item IS the identity)");
			assertEquals(headItem(GT6CraftingRecipes.digLadderHeadPrefix(tId), "iron"),
					tRow.getAsJsonObject("result").get("item").getAsString(),
					tId + ": the result is the tool HEAD (Loader_Tools.java:451 tStack + :514-516), never the tool");
			assertFalse(tRow.has("material"), tId + ": no stamp field — the head needs no identity stamp");
			List<String> tPattern = tRow.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
			assertEquals(java.util.Arrays.asList(tForm).subList(1, tForm.length), tPattern,
					tId + ": the upstream P/I row shape verbatim (Loader_Tools.java:294-300)");
		}
	}

	/** The sword blade row (:321, the arg-9 face — NOT a direct row) emits the sword head. */
	@Test
	public void theSwordRowEmitsTheHead() throws Exception {
		JsonObject tRow = generated("recipes/sword/iron");
		assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString());
		assertEquals("gt6:tool_head_sword_iron", tRow.getAsJsonObject("result").get("item").getAsString(),
				"the :321 row is an mToolHeadRecipes row — the head, never the sword");
		List<String> tPattern = tRow.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
		assertEquals(List.of(" P ", "fPh"), tPattern, "the :321 P/I shape verbatim");
	}

	/** The machine head rows (:305 chisel / :306 screwdriver / :307 saw / :327 hammer) emit heads; Steel keeps its :327 row. */
	@Test
	public void theMachineHeadRowsEmitHeads() throws Exception {
		record MachinePin(String aForm, String aSnake, String aHead, List<String> aPattern, int aLine) {}
		for (MachinePin tPin : List.of(
				new MachinePin("chisel", "iron", "gt6:tool_head_chisel_iron", List.of("hPf", " S "), 305),
				new MachinePin("screwdriver", "iron", "gt6:tool_head_screwdriver_iron", List.of("hS", "Sf"), 306),
				new MachinePin("saw", "iron", "gt6:tool_head_saw_iron", List.of("PP", "fh"), 307),
				new MachinePin("hammer", "steel", "gt6:tool_head_hammer_steel", List.of("II ", "IIh", "II "), 327))) {
			JsonObject tRow = generated("recipes/" + tPin.aForm() + "/" + tPin.aSnake());
			assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString(), tPin.aForm() + " type");
			assertEquals(tPin.aHead(), tRow.getAsJsonObject("result").get("item").getAsString(),
					tPin.aForm() + ": the :" + tPin.aLine() + " head-row output");
			List<String> tPattern = tRow.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
			assertEquals(tPin.aPattern(), tPattern, tPin.aForm() + ": the :" + tPin.aLine() + " shape verbatim");
		}
	}

	// --------------------------------------------------------------- the zero-change guard

	/** The direct families (:310/:311/:316/:314/:324/:322/:323, the arg-8 face) STAY material_tool tool rows. */
	@Test
	public void theDirectFamiliesStayToolRows() throws Exception {
		for (String tForm : List.of("wrench", "monkey_wrench", "pincers", "crowbar", "cutter", "knife",
				"butchery_knife")) {
			JsonObject tRow = generated("recipes/" + tForm + "/iron");
			assertEquals("gt6:material_tool", tRow.get("type").getAsString(), tForm + ": the stamped serializer");
			assertEquals("gt6:" + tForm, tRow.getAsJsonObject("result").get("item").getAsString(),
					tForm + ": the direct row keeps the TOOL output (the arg-8 face, r7-39 untouched)");
			assertEquals("iron", tRow.get("material").getAsString(), tForm + ": the stamp material field");
		}
	}

	// --------------------------------------------------------------- the assembly band

	/** The 17-form ledger in upstream :332-350 order (minus the :332 lens row and the :334 soft hammer, both ported). */
	@Test
	public void theSeventeenAssemblyFormsMatchUpstream() {
		List<GT6CraftingRecipes.AssemblyForm> tForms = GT6CraftingRecipes.assemblyForms();
		assertEquals(17, tForms.size(), "the 19 ACT rows minus the lens (:332) and the soft hammer (:334)");
		record FormPin(String aId, OreDictPrefix aHead) {}
		FormPin[] tPins = {
				new FormPin("hard_hammer", OP.toolHeadHammer), // :333
				new FormPin("sword", OP.toolHeadSword), // :335
				new FormPin("builder_wand", OP.toolHeadBuilderwand), // :336
				new FormPin("pickaxe_construction", OP.toolHeadConstructionPickaxe), // :337
				new FormPin("pickaxe_gem", OP.toolHeadPickaxeGem), // :338
				new FormPin("pickaxe", OP.toolHeadPickaxe), // :339
				new FormPin("shovel", OP.toolHeadShovel), // :340
				new FormPin("spade", OP.toolHeadSpade), // :341
				new FormPin("axe", OP.toolHeadAxe), // :342
				new FormPin("axe_double", OP.toolHeadAxeDouble), // :343
				new FormPin("hoe", OP.toolHeadHoe), // :344
				new FormPin("sense", OP.toolHeadSense), // :345
				new FormPin("plow", OP.toolHeadPlow), // :346
				new FormPin("file", OP.toolHeadFile), // :347
				new FormPin("chisel", OP.toolHeadChisel), // :348
				new FormPin("screwdriver", OP.toolHeadScrewdriver), // :349
				new FormPin("saw", OP.toolHeadSaw)}; // :350
		for (int i = 0; i < tPins.length; i++) {
			assertSame(tPins[i].aId(), tForms.get(i).aId(), "form " + i + " keeps the upstream order");
			assertSame(tPins[i].aHead(), tForms.get(i).aHead(), tPins[i].aId() + " head prefix");
		}
		for (GT6CraftingRecipes.AssemblyForm tForm : tForms) {
			assertEquals(tForm.aId().equals("hard_hammer"), tForm.aNoSoftTag(),
					tForm.aId() + ": only the :333 Nor(WOOD, BOUNCY, STRETCHY) gate");
		}
	}

	/** The assembly walk = the head-item truth (7063 measured); the :333 gate excludes the soft materials. */
	@Test
	public void theAssemblyWalkIsTheHeadTruth() {
		List<GT6CraftingRecipes.AssemblyRow> tRows = GT6CraftingRecipes.toolAssemblyRows();
		assertEquals(7063, tRows.size(), "the measured item-truth total over the 17 forms");
		for (GT6CraftingRecipes.AssemblyRow tRow : tRows) {
			assertFalse(tRow.aSnake().isEmpty(), "every row carries its material snake");
			if (tRow.aForm().aNoSoftTag()) {
				assertFalse(tRow.aMaterial().contains(TD.Properties.WOOD)
						|| tRow.aMaterial().contains(TD.Properties.BOUNCY)
						|| tRow.aMaterial().contains(TD.Properties.STRETCHY),
						tRow.aForm().aId() + "/" + tRow.aSnake() + ": the :333 gate");
			}
		}
		assertTrue(tRows.stream().anyMatch(tRow -> tRow.aForm().aId().equals("pickaxe")
				&& tRow.aMaterial() == MT.Iron), "the iron pickaxe assembly rides the walk");
	}

	/** The representative assembly row: the head item + the wooden-rod tag (the declared deviation) through material_tool. */
	@Test
	public void theAssemblyRowCarriesTheHeadAndTheRod() throws Exception {
		JsonObject tRow = generated("recipes/pickaxe_from_head/iron");
		assertEquals("gt6:material_tool", tRow.get("type").getAsString(), "the stamp seam (the soft_hammer precedent)");
		assertEquals(List.of("H", "S"), tRow.getAsJsonArray("pattern").asList().stream()
				.map(JsonElement::getAsString).toList(), "the 1x2 head-over-rod column");
		assertEquals("gt6:tool_head_pickaxe_iron", key(tRow, 'H').getAsJsonObject().get("item").getAsString(),
				"'H' = the :339 head item the dig row above produces");
		assertTrue(key(tRow, 'S').getAsJsonObject().has("tag")
				&& key(tRow, 'S').getAsJsonObject().get("tag").getAsString().endsWith("rods/wooden"),
				"'S' = the platform wooden-rod tag (the DECLARED DEVIATION vs the :105 mHandleMaterial gate)");
		assertEquals("gt6:pickaxe", tRow.getAsJsonObject("result").get("item").getAsString());
		assertFalse(tRow.has("multiplier"), "the x1.0 form omits the multiplier field");
		assertEquals("iron", tRow.get("material").getAsString());
	}

	/** The gem pick's x0.25 form multiplier rides the row JSON (the serializer's pre-declared growth). */
	@Test
	public void theGemPickRowCarriesTheFormMultiplier() throws Exception {
		JsonObject tRow = generated("recipes/pickaxe_gem_from_head/amber");
		assertEquals("gt6:pickaxe_gem", tRow.getAsJsonObject("result").get("item").getAsString());
		assertEquals(0.25F, tRow.get("multiplier").getAsFloat(), "the LadderTool x0.25 budget face (MultiItemTool.java:182)");
	}

	/** The advancement rides the vanilla folder-name path — no orphaned recipes/tools advancement tree. */
	@Test
	public void theAdvancementRidesTheToolsFolder() throws Exception {
		JsonObject tAdvancement = generated("advancements/recipes/tools/pickaxe/iron");
		List<String> tRewards = tAdvancement.getAsJsonObject("rewards").getAsJsonArray("recipes").asList()
				.stream().map(JsonElement::getAsString).toList();
		assertEquals(List.of("gt6:pickaxe/iron"), tRewards, "the recipe reward keeps the row id");
	}
}
