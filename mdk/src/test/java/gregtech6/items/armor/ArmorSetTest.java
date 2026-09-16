package gregtech6.items.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ArmorItem;

import gregtech6.registry.GT6Tools;

/**
 * The offline armor pin (task p29-w5-t8-armor-24 acceptance): the TAB_TABLE tail parity
 * + the 24 id census + the {@link GT6HazardSets} judgment arms + the stat literals — all
 * PURE faces, because the mod-Item intrusive-holder wall makes the pieces themselves
 * unconstructible in this bootstrapped-and-frozen JVM (the GT6ToolsCreativeTabTest
 * posture). The committed generated tree is the second witness: the hazard TAG files
 * must equal the API membership (the tag+static-table SPEC translation) and the recipe
 * JSONs must carry the Loader_Tools grids.
 */
public class ArmorSetTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	// ------------------------------------------------------------------ the table faces

	/** The 24 armor rows are the TAB_TABLE tail, in the SUITS walk order. */
	@Test
	public void armorRowsAreTheTabTableTail() {
		assertEquals(GT6ArmorMaterials.SUITS.size() * 4, GT6Tools.ARMOR_ROWS.size());
		int tTail = GT6Tools.TAB_TABLE.size() - GT6Tools.ARMOR_ROWS.size();
		assertTrue(tTail >= 10, "the ten base tool rows must precede the armor tail");
		for (int i = 0; i < 24; i++) {
			assertEquals(GT6Tools.ARMOR_ROWS.get(i).getId(), GT6Tools.TAB_TABLE.get(tTail + i).getId(),
					"TAB_TABLE tail row " + i + " must be the armor row (append-only seam)");
		}
	}

	/** The 24 id census — every id is suit-word + "_" + slot-word, all distinct. */
	@Test
	public void censusIsTwentyFourDistinctSuitPieceIds() {
		Set<ResourceLocation> rIds = new LinkedHashSet<>();
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < 4; i++) {
				assertEquals(rl(tSuit.pieceId(i)), GT6Tools.armorRow(tSuit, i).getId());
				rIds.add(rl(tSuit.pieceId(i)));
			}
		}
		assertEquals(24, rIds.size());
	}

	/** The stat literals — the Loader_Tools.java:68 row verbatim (1/件, 128, 8), via the leg-neutral face. */
	@Test
	public void statLiteralsMatchTheUpstreamRow() {
		for (GT6ArmorMaterials tMaterial : GT6ArmorMaterials.values()) {
			// the four WEARABLE slots (the :68 int[]{1,1,1,1}); the 1.21 BODY slot (wolf
			// armor) carries no upstream analogue and reads the map default
			for (ArmorItem.Type tType : GT6ArmorMaterials.PIECE_TYPES) {
				assertEquals(1, tMaterial.defenseFor(tType), "defense 1/件 (" + tMaterial + ")");
				assertEquals(128, tMaterial.durabilityFor(tType), "durability 128 (" + tMaterial + ")");
			}
			assertEquals(8, tMaterial.enchantValue());
		}
	}

	/** The hazard coverage columns — the Loader_Tools.java:98-112 join semantics as data. */
	@Test
	public void suitCoverageColumnsMatchTheUpstreamJoins() {
		assertEquals(6, GT6ArmorMaterials.SUITS.size());
		assertTrue(GT6ArmorMaterials.rowOf(GT6ArmorMaterials.UNIVERSAL).hazards()
				.equals(java.util.EnumSet.allOf(GT6HazardSets.Hazard.class)), "universal joins ALL (the :106 LIGHTNING row included)");
		assertEquals(java.util.EnumSet.of(GT6HazardSets.Hazard.GAS, GT6HazardSets.Hazard.BIO, GT6HazardSets.Hazard.CHEM),
				GT6ArmorMaterials.rowOf(GT6ArmorMaterials.BIOCHEMGAS).hazards(), "biochemgas joins gas/bio/chem (the :108-112 loop)");
	}

	// ------------------------------------------------------------------ the judgment arms

	private static List<ResourceLocation> pieces(GT6ArmorMaterials aSuit) {
		List<ResourceLocation> rList = new ArrayList<>();
		for (int i = 0; i < 4; i++) rList.add(rl(GT6ArmorMaterials.rowOf(aSuit).pieceId(i)));
		return rList;
	}

	/** universal 全套 → every hazard set reads TRUE (the card's 7 arms + the :106 LIGHTNING row). */
	@Test
	public void fullUniversalSetReadsTrueOnEveryHazard() {
		List<ResourceLocation> tSet = pieces(GT6ArmorMaterials.UNIVERSAL);
		for (GT6HazardSets.Hazard tHazard : GT6HazardSets.Hazard.values()) {
			assertTrue(GT6HazardSets.isFullSet(tHazard, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)),
					"universal full set must satisfy " + tHazard);
		}
	}

	/** biochemgas 全套 → gas/bio/chem true, radiation (and the rest) false. */
	@Test
	public void fullBiochemgasSetIsThreeTrueFiveFalse() {
		List<ResourceLocation> tSet = pieces(GT6ArmorMaterials.BIOCHEMGAS);
		assertTrue(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.GAS, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertTrue(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.BIO, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertTrue(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.CHEM, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertFalse(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.RADIATION, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertFalse(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.INSECTS, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertFalse(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.FROST, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertFalse(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.HEAT, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
		assertFalse(GT6HazardSets.isFullSet(GT6HazardSets.Hazard.LIGHTNING, tSet.get(0), tSet.get(1), tSet.get(2), tSet.get(3)));
	}

	/** 混穿 → false everywhere (one piece per DIFFERENT suit is never a full set). */
	@Test
	public void mixedWearReadsFalseEverywhere() {
		List<ResourceLocation> tInsect = pieces(GT6ArmorMaterials.INSECTS);
		List<ResourceLocation> tFrost = pieces(GT6ArmorMaterials.FROST);
		List<ResourceLocation> tHeat = pieces(GT6ArmorMaterials.HEAT);
		List<ResourceLocation> tRadiation = pieces(GT6ArmorMaterials.RADIATION);
		for (GT6HazardSets.Hazard tHazard : GT6HazardSets.Hazard.values()) {
			assertFalse(GT6HazardSets.isFullSet(tHazard, tInsect.get(0), tFrost.get(1), tHeat.get(2), tRadiation.get(3)),
					"mixed suit wear must fail " + tHazard);
		}
	}

	/** An empty slot (null) is never a full set — the partial-set guard. */
	@Test
	public void emptySlotIsNeverAFullSet() {
		List<ResourceLocation> tSet = pieces(GT6ArmorMaterials.UNIVERSAL);
		for (GT6HazardSets.Hazard tHazard : GT6HazardSets.Hazard.values()) {
			assertFalse(GT6HazardSets.isFullSet(tHazard, null, tSet.get(1), tSet.get(2), tSet.get(3)));
			assertFalse(GT6HazardSets.isFullSet(tHazard, tSet.get(0), tSet.get(1), null, tSet.get(3)));
		}
	}

	/** The membership roster — base suit + universal per the join columns. */
	@Test
	public void membershipRostersMatchTheJoins() {
		for (GT6HazardSets.Hazard tHazard : GT6HazardSets.Hazard.values()) {
			if (tHazard == GT6HazardSets.Hazard.LIGHTNING) {
				assertEquals(pieces(GT6ArmorMaterials.UNIVERSAL), List.copyOf(GT6HazardSets.members(tHazard)),
						"lightning carries ONLY the universal set (the :106 single join)");
			} else {
				assertEquals(8, GT6HazardSets.members(tHazard).size(), tHazard + " = base suit + universal");
			}
		}
	}

	/** The texture seam — the HumanoidArmorLayer composition, gt6-namespaced, legs = layer 2. */
	@Test
	public void texturePathIsTheLayeredModelComposition() {
		assertEquals("gt6:textures/models/armor/hazmat_insect_layer_1.png",
				GT6ArmorItem.armorTexturePath("hazmat_insect", false));
		assertEquals("gt6:textures/models/armor/hazmat_insect_layer_2.png",
				GT6ArmorItem.armorTexturePath("hazmat_insect", true));
	}

	// ------------------------------------------------------------------ the generated witnesses

	/** The classpath text of one generated file (the GT6TagsDatagenTest face). */
	private static String generated(String aPath) throws Exception {
		try (InputStream tStream = ArmorSetTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated file must be committed: " + aPath);
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** The hazard TAG files equal the API membership — the tag+static-table parity. */
	@Test
	public void hazardTagsEqualTheApiMembership() throws Exception {
		for (GT6HazardSets.Hazard tHazard : GT6HazardSets.Hazard.values()) {
			Set<String> tTagged = new LinkedHashSet<>();
			var tArray = JsonParser.parseString(generated("data/gt6/tags/items/" + GT6HazardSets.tagPath(tHazard) + ".json"))
					.getAsJsonObject().getAsJsonArray("values");
			for (var tEntry : tArray) tTagged.add(tEntry.getAsString());
			Set<String> tApi = new LinkedHashSet<>();
			for (ResourceLocation tId : GT6HazardSets.members(tHazard)) tApi.add(tId.toString());
			assertEquals(tApi, tTagged, "the " + tHazard + " tag face must equal the API face");
		}
	}

	/** The 24 recipe JSONs exist, one per piece (the result-path convention). */
	@Test
	public void allTwentyFourRecipeFilesExist() throws Exception {
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (int i = 0; i < 4; i++) {
				assertNotNull(generated("data/gt6/recipes/" + tSuit.pieceId(i) + ".json"));
			}
		}
	}

	/** The base-suit grid pin — the insect helmet row (:68) minus the dropped tool letters. */
	@Test
	public void baseSuitHelmetGridPinsTheUpstreamRow() throws Exception {
		var tJson = JsonParser.parseString(generated("data/gt6/recipes/hazmat_insect_helmet.json")).getAsJsonObject();
		assertEquals("minecraft:crafting_shaped", tJson.get("type").getAsString());
		var tPattern = tJson.getAsJsonArray("pattern");
		assertEquals(2, tPattern.size(), "the third upstream row (q l) is the dropped-tool-letters row");
		assertEquals("MMM", tPattern.get(0).getAsString());
		assertEquals("MGM", tPattern.get(1).getAsString());
		assertEquals("gt6:foil_rubber", tJson.getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString(),
				"'R' = foil.dat(ANY.Rubber), flattened to the Rubber member");
		assertEquals("minecraft:black_stained_glass_pane", tJson.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
		assertEquals("gt6:hazmat_insect_helmet", tJson.getAsJsonObject("result").get("item").getAsString(),
				"the 1.20.1 result face (the item key; count omitted = 1)");
	}

	/** The universal grid pin — the :95 legs row: five same-slot pieces + the chainmail legs. */
	@Test
	public void universalLegsGridPinsTheUpstreamRow() throws Exception {
		var tJson = JsonParser.parseString(generated("data/gt6/recipes/hazmat_universal_leggings.json")).getAsJsonObject();
		var tPattern = tJson.getAsJsonArray("pattern");
		assertEquals(3, tPattern.size());
		assertEquals("A B", tPattern.get(0).getAsString());
		assertEquals("C D", tPattern.get(1).getAsString());
		assertEquals("E F", tPattern.get(2).getAsString());
		var tKey = tJson.getAsJsonObject("key");
		assertEquals("gt6:hazmat_biochemgas_leggings", tKey.getAsJsonObject("A").get("item").getAsString());
		assertEquals("gt6:hazmat_insect_leggings", tKey.getAsJsonObject("B").get("item").getAsString());
		assertEquals("gt6:hazmat_frost_leggings", tKey.getAsJsonObject("C").get("item").getAsString());
		assertEquals("gt6:hazmat_heat_leggings", tKey.getAsJsonObject("D").get("item").getAsString());
		assertEquals("gt6:hazmat_radiation_leggings", tKey.getAsJsonObject("E").get("item").getAsString());
		assertEquals("minecraft:chainmail_leggings", tKey.getAsJsonObject("F").get("item").getAsString());
		assertEquals("gt6:hazmat_universal_leggings", tJson.getAsJsonObject("result").get("item").getAsString());
		assertFalse(tJson.getAsJsonObject("result").has("count"), "count 1 serializes away (the 1.20.1 face)");
	}
}
