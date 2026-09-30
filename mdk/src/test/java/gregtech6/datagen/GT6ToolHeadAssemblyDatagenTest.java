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
 * ruling-A caliber): the dig walk 202, the assembly walk 7063 and the r9-39 completion
 * walk (the 9 missing head families + the dig/chisel/saw C variants, task
 * r9-39-toolhead-rows, pinned in {@code theCompletionWalkCarriesTheMaterialGates}).
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

	// --------------------------------------------------------------- the r9-39 completion band

	/**
	 * The spade row's lowercase 's' cell is the CR.java:211 SAW tool letter — the
	 * decisions.r9-toolhead-s-letter reform (r7-39 had emitted the wooden-rod tag).
	 */
	@Test
	public void theSpadeRowSawSlotRidesTheSawTag() throws Exception {
		JsonObject tRow = generated("recipes/spade/iron");
		assertTrue(key(tRow, 's').getAsJsonObject().has("tag")
				&& "gt6:tools/saw".equals(key(tRow, 's').getAsJsonObject().get("tag").getAsString()),
				"'s' = the saw tool tag (the tool-damage slot), never the wooden rod again");
	}

	/** The 9 completion families emit HEADS through vanilla shaped, upstream shapes verbatim (Loader_Tools :293-:310). */
	@Test
	public void theCompletionHeadRowsEmitHeads() throws Exception {
		record CompletionPin(String aId, String aSnake, String aHead, List<String> aPattern, int aLine) {}
		for (CompletionPin tPin : List.of(
				new CompletionPin("builder_wand", "iron", "gt6:tool_head_builderwand_iron", List.of(" P ", "f h", " s "), 293),
				new CompletionPin("axe_double", "iron", "gt6:tool_head_axe_double_iron", List.of("PIP", "P P", "f h"), 301),
				new CompletionPin("sense", "iron", "gt6:tool_head_sense_iron", List.of("PPI", "f h"), 302),
				new CompletionPin("plow", "iron", "gt6:tool_head_plow_iron", List.of("PPP", "PPP", "f h"), 303),
				new CompletionPin("file", "iron", "gt6:tool_head_file_iron", List.of(" P ", " Pk"), 304),
				new CompletionPin("chainsaw", "steel", "gt6:tool_head_chainsaw_steel", List.of("WVW", "XhX", "WVW"), 308),
				new CompletionPin("drill", "iron", "gt6:tool_head_drill_iron", List.of("PVP", "PVP", "VhV"), 309),
				new CompletionPin("wrench_head", "iron", "gt6:tool_head_wrench_iron", List.of("hPW", "PVP", "WPd"), 310))) {
			JsonObject tRow = generated("recipes/" + tPin.aId() + "/" + tPin.aSnake());
			assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString(), tPin.aId() + " type");
			assertEquals(tPin.aHead(), tRow.getAsJsonObject("result").get("item").getAsString(),
					tPin.aId() + ": the :" + tPin.aLine() + " head-row output");
			assertFalse(tRow.has("material"), tPin.aId() + ": no stamp field — the head item IS the identity");
			List<String> tPattern = tRow.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
			assertEquals(tPin.aPattern(), tPattern, tPin.aId() + ": the :" + tPin.aLine() + " shape verbatim");
		}
	}

	/** The chainsaw's fixed Steel letters + the material chain (the :308 arg-13/14 specials). */
	@Test
	public void theChainsawRowCarriesTheSteelSpecials() throws Exception {
		JsonObject tRow = generated("recipes/chainsaw/steel");
		assertEquals("gt6:plate_steel", key(tRow, 'W').getAsJsonObject().get("item").getAsString(), "'W' = plate(ANY.Steel)");
		assertEquals("gt6:ring_steel", key(tRow, 'V').getAsJsonObject().get("item").getAsString(), "'V' = ring(ANY.Steel)");
		assertEquals("gt6:chain_steel", key(tRow, 'X').getAsJsonObject().get("item").getAsString(), "'X' = the row material's chain");
		JsonObject tWrench = generated("recipes/wrench_head/iron");
		assertEquals("gt6:screw_steel", key(tWrench, 'W').getAsJsonObject().get("item").getAsString(), "wrench 'W' = screw(ANY.Steel)");
		assertEquals("gt6:ring_steel", key(tWrench, 'V').getAsJsonObject().get("item").getAsString(), "wrench 'V' = ring(ANY.Steel)");
		assertTrue(key(tWrench, 'd').getAsJsonObject().get("tag").getAsString().equals("gt6:tools/screwdriver"),
				"'d' = the screwdriver tool letter (CR.java:198)");
		JsonObject tFile = generated("recipes/file/iron");
		assertTrue(key(tFile, 'k').getAsJsonObject().get("tag").getAsString().equals("gt6:tools/knife"),
				"'k' = the knife tool letter (CR.java:204)");
	}

	/** The C/G second variants: the plateGem ITEM + the gems tag, the head of the SAME family. */
	@Test
	public void theGemVariantRowsRideThePlateGemItem() throws Exception {
		JsonObject tPickaxe = generated("recipes/pickaxe_gem/diamond");
		assertEquals("minecraft:crafting_shaped", tPickaxe.get("type").getAsString());
		assertEquals("gt6:tool_head_pickaxe_diamond", tPickaxe.getAsJsonObject("result").get("item").getAsString(),
				"the id _gem = the plateGem VARIANT of the pickaxe head row — the result is the plain pickaxe HEAD, "
						+ "never the gem-tipped pickaxe head (tool_head_pickaxe_gem stays the craftfrom :190 face)");
		assertEquals(List.of("CGG", "f  "), tPickaxe.getAsJsonArray("pattern").asList().stream()
				.map(JsonElement::getAsString).toList(), "the :295 second shape verbatim");
		assertEquals("gt6:plate_gem_diamond", key(tPickaxe, 'C').getAsJsonObject().get("item").getAsString(),
				"'C' = the plateGem ITEM (the bladeLadderIngredient 'C' precedent)");
		assertEquals("forge:gems/diamond", key(tPickaxe, 'G').getAsJsonObject().get("tag").getAsString(),
				"'G' = the gems tag");
		JsonObject tSpade = generated("recipes/spade_gem/diamond");
		assertEquals("gt6:tool_head_spade_diamond", tSpade.getAsJsonObject("result").get("item").getAsString());
		assertEquals("gt6:tools/saw", key(tSpade, 's').getAsJsonObject().get("tag").getAsString(),
				"the gem-variant spade carries the same 's' reform");
		JsonObject tPure = generated("recipes/builder_wand_pure_gem/diamond");
		assertEquals("gt6:tool_head_builderwand_diamond", tPure.getAsJsonObject("result").get("item").getAsString(),
				"the builderwand :293 third shape (the bare-gem 'G' variant) lands under its own id");
	}

	/**
	 * The completion walk: the census is the measured item-truth number; the typemin(2)/
	 * qualmax(2)/qualmin(1) gates ride the AXIS (not the item truth) — proven by the
	 * item-true-but-gate-cut materials (TungstenSteel quality 4, Cu quality 0).
	 */
	@Test
	public void theCompletionWalkCarriesTheMaterialGates() {
		List<GT6CraftingRecipes.ToolHeadRow> tRows = GT6CraftingRecipes.toolHeadRows();
		assertEquals(3397, tRows.size(), "the measured head-truth ∩ axis ∩ letter-truth total over the 23 forms");
		for (GT6CraftingRecipes.ToolHeadRow tRow : tRows) {
			assertTrue(tRow.aMaterial().mToolTypes >= Math.max(1, tRow.aForm().aTypeMin()),
					tRow.aForm().aId() + "/" + tRow.aSnake() + ": the :426 listener gate + typemin");
			if (tRow.aForm().aId().endsWith("_gem") || tRow.aForm().aId().equals("builder_wand_pure_gem")) {
				assertTrue(gregapi.data.OP.plateGem.isGeneratingItem(tRow.aMaterial()),
						tRow.aForm().aId() + "/" + tRow.aSnake() + ": the gem variants ride the plateGem truth");
			}
		}
		assertTrue(gregapi.data.OP.toolHeadFile.isGeneratingItem(MT.TungstenSteel),
				"the tungstensteel FILE head item is item-true (typemin(2), quality-agnostic)");
		assertFalse(tRows.stream().anyMatch(tRow -> tRow.aForm().aId().equals("file")
				&& tRow.aMaterial().mToolQuality > 2), "qualmax(2) cuts the quality-4 tungstensteel file row");
		assertTrue(gregapi.data.OP.toolHeadWrench.isGeneratingItem(MT.Cu),
				"the copper WRENCH head item is item-true (typemin(2))");
		assertFalse(tRows.stream().anyMatch(tRow -> tRow.aForm().aId().equals("wrench_head")
				&& tRow.aMaterial().mToolQuality < 1), "qualmin(1) cuts the quality-0 copper wrench-head row");
		assertTrue(tRows.stream().anyMatch(tRow -> tRow.aForm().aId().equals("chainsaw")
				&& tRow.aMaterial() == MT.Steel), "Steel keeps its chainsaw head row (no steel anchor owns this family)");
	}

	// --------------------------------------------------------------- the r10 gem sisters

	/**
	 * The machine-band gem sisters (task r10-debt-gem-sisters, the r9-39 declared
	 * remainder): the wrench :310 / monkey wrench :311 C (plateGem) variants stay DIRECT
	 * TOOL rows (the arg-8 face), the hammer :327 G (gem) variant is a HEAD row (the
	 * arg-9 face) — the gem variants swap the in-grid hammer for the file (the upstream
	 * letter choice verbatim: Loader_Tools.java "CfC"/"CCd"/"GGf").
	 */
	@Test
	public void theGemSisterRowsCarryTheUpstreamShapes() throws Exception {
		JsonObject tWrench = generated("recipes/wrench_gem/diamond");
		assertEquals("gt6:material_tool", tWrench.get("type").getAsString(), "the :310 C variant is a DIRECT tool row (the arg-8 face)");
		assertEquals("gt6:wrench", tWrench.getAsJsonObject("result").get("item").getAsString(),
				"the gem variant keeps the TOOL output");
		assertEquals(List.of("CfC", " C ", " C "), tWrench.getAsJsonArray("pattern").asList().stream()
				.map(JsonElement::getAsString).toList(), "the :310 second shape verbatim");
		assertEquals("gt6:plate_gem_diamond", key(tWrench, 'C').getAsJsonObject().get("item").getAsString(),
				"'C' = the plateGem ITEM (no tag family, the blade-family precedent)");
		assertEquals("gt6:tools/file", key(tWrench, 'f').getAsJsonObject().get("tag").getAsString(),
				"the gem variant files, never hammers ('f' not 'h' upstream)");
		JsonObject tMonkey = generated("recipes/monkey_wrench_gem/diamond");
		assertEquals("gt6:material_tool", tMonkey.get("type").getAsString());
		assertEquals("gt6:monkey_wrench", tMonkey.getAsJsonObject("result").get("item").getAsString());
		assertEquals(List.of("CCd", "fCT", " C "), tMonkey.getAsJsonArray("pattern").asList().stream()
				.map(JsonElement::getAsString).toList(), "the :311 second shape verbatim");
		assertEquals("gt6:screw_diamond", key(tMonkey, 'T').getAsJsonObject().get("item").getAsString(),
				"'T' = the row material's screw (the :463 letter resolution)");
		assertEquals("gt6:tools/screwdriver", key(tMonkey, 'd').getAsJsonObject().get("tag").getAsString(),
				"'d' = the screwdriver tool letter (CR.java)");
		JsonObject tHammer = generated("recipes/hammer_gem/diamond");
		assertEquals("minecraft:crafting_shaped", tHammer.get("type").getAsString(),
				"the :327 G variant is a HEAD row (the arg-9 face, the r7-39 inversion face)");
		assertEquals("gt6:tool_head_hammer_diamond", tHammer.getAsJsonObject("result").get("item").getAsString(),
				"the head item IS the identity — never the soft-hammer shape's :328 twin");
		assertEquals(List.of("GG ", "GGf", "GG "), tHammer.getAsJsonArray("pattern").asList().stream()
				.map(JsonElement::getAsString).toList(), "the :327 second shape verbatim");
		assertEquals("forge:gems/diamond", key(tHammer, 'G').getAsJsonObject().get("tag").getAsString(),
				"'G' = the gems tag (the completion-band 'G' letter precedent)");
	}

	/**
	 * The gem sister universes ride the SAME axis gates as their P/I siblings plus the
	 * plateGem/gem/screw letter item truth (the offline isGeneratingItem face — the
	 * toolHeadRowResolvable split; the emission's get() face is datagen-JVM-only). The
	 * counts are the measured dead numbers matching the generated trees; the negative
	 * gates prove the ghost rows constructibly zero.
	 */
	@Test
	public void theGemSisterUniversesRideTheTruthGates() {
		java.util.Map<String, Integer> tCounts = new java.util.HashMap<>();
		java.util.Set<String> tSeen = new java.util.HashSet<>();
		for (gregapi.oredict.OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			for (GT6CraftingRecipes.MachineLadderForm tForm : GT6CraftingRecipes.MACHINE_LADDER_FORMS) {
				if (!tForm.aId().endsWith("_gem")) continue; // this card's three forms
				if (!GT6CraftingRecipes.machineLadderAxis(tMaterial, tForm)) continue;
				// the hammer_gem HEAD-row emission gate (machineLadderHeadItem get() in the datagen
				// JVM; its offline isGeneratingItem face here — the Glass probe: gem-true but
				// headless, the tree carries no glass row)
				if (tForm.aId().equals("hammer_gem") && !gregapi.data.OP.toolHeadHammer.isGeneratingItem(tMaterial)) continue;
				boolean tResolvable = true;
				for (String tPatternRow : tForm.aPattern()) {
					for (char tChar : tPatternRow.toCharArray()) {
						if (tChar == ' ') continue;
						switch (tChar) {
							case 'C' -> tResolvable &= gregapi.data.OP.plateGem.isGeneratingItem(tMaterial);
							case 'G' -> tResolvable &= gregapi.data.OP.gem.isGeneratingItem(tMaterial);
							case 'T' -> tResolvable &= gregapi.data.OP.screw.isGeneratingItem(tMaterial);
							case 'f', 'd' -> {} // the tool tags carry no material truth
							default -> throw new IllegalArgumentException(tForm.aId() + ": unexpected letter " + tChar);
						}
					}
				}
				if (!tResolvable) continue;
				tCounts.merge(tForm.aId(), 1, Integer::sum);
				if (tForm.aQualMin() > 0) {
					assertTrue(tMaterial.mToolTypes >= 2 && tMaterial.mToolQuality >= tForm.aQualMin(),
							tForm.aId() + "/" + tMaterial.mNameInternal + ": the wrench pair's typemin(2)+qualmin(1)");
					assertTrue(gregapi.data.OP.plateGem.isGeneratingItem(tMaterial),
							tForm.aId() + "/" + tMaterial.mNameInternal + ": the plateGem letter truth");
				} else {
					assertFalse(tMaterial.contains(TD.Properties.WOOD) || tMaterial.contains(TD.Properties.BOUNCY)
							|| tMaterial.contains(TD.Properties.STRETCHY),
							tForm.aId() + "/" + tMaterial.mNameInternal + ": the :327 Nor(WOOD,BOUNCY,STRETCHY) gate");
					assertTrue(gregapi.data.OP.gem.isGeneratingItem(tMaterial)
							&& gregapi.data.OP.toolHeadHammer.isGeneratingItem(tMaterial),
							tForm.aId() + "/" + tMaterial.mNameInternal + ": the gem + hammer-head item truth");
				}
			}
		}
		assertEquals(114, tCounts.get("wrench_gem").intValue(), "the measured plateGem ∩ wrench-axis universe");
		assertEquals(114, tCounts.get("monkey_wrench_gem").intValue(), "every plateGem wrench material also carries the screw letter");
		assertEquals(130, tCounts.get("hammer_gem").intValue(), "the gem universe without the typemin/qualmin cuts (the quartz family lands here, not in the wrench pair)");
		assertTrue(tCounts.containsKey("wrench_gem") && tCounts.size() == 3, "exactly the three gem-sister forms walked");
	}
}
