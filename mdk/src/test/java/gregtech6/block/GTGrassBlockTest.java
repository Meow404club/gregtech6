package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

import gregtech6.registry.GTGrassBlocks;

/**
 * The offline acceptance of task p24-grass-block over the committed generated tree (the
 * GT6TagsDatagenTest snapshot discipline): the 6-variant registry face (id paths, the
 * Behavior_Spray_Color.java:154-160 dye order, the borrow-code texture mapping), the
 * EMPTY behaviour face (no randomTick — the no-spread cut; no flammability override —
 * the vanilla-default face; no canSustainPlant override — the declared default-face
 * deviation, decisions.p24-grass-behavior-trim), the sheep identity difference, the loot
 * double-track snapshots (dirt without fortune + silk self-drop, the vanilla grass_block
 * alternatives form), the tag snapshots (the four joined tags exactly, the six animal
 * spawnable tags plus valid_spawn pinned ABSENT — the canCreatureSpawn = F equivalence),
 * the model snapshots (cube_bottom_top, ZERO tintindex), and the 12 recipe snapshots
 * (8 grass + 1 dye tag -> 8 variants; the reverse shapeless; zero bare dye items).
 */
class GTGrassBlockTest {

	/** The pinned variant-order id paths (variant 0 bare id, the GTStoneBlocks.path rule). */
	private static final List<String> PINNED_PATHS = List.of(
			"grass", "grass_lime", "grass_black", "grass_light_gray", "grass_yellow", "grass_brown");

	/**
	 * The pinned dye indexes in variant order — the Behavior_Spray_Color.java:154-160
	 * switch transposed by variant (Green←2, Lime←10, Black←0, LightGray←7, Yellow←11,
	 * Brown←3), the same order the dye recipes BlockGrass.java:75-80 ride.
	 */
	private static final byte[] PINNED_DYES = {2, 10, 0, 7, 11, 3};

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	// ------------------------------------------------------------------ the registry face

	@Test
	void variantOrderFollowsTheUpstreamMetaOrder() {
		assertEquals(PINNED_PATHS, GTGrassBlocks.PATHS, "the three same-sequence upstream tables");
		assertEquals(6, GTGrassBlocks.VARIANTS.size());
		byte[] tDyes = GTGrassBlocks.dyeIndexes();
		for (int i = 0; i < 6; i++) {
			assertEquals(PINNED_DYES[i], tDyes[i], "variant " + i + " dye index");
			assertEquals(PINNED_DYES[i], GTGrassBlocks.VARIANTS.get(i).dyeIndex(), "record row " + i);
		}
	}

	/**
	 * The borrow-code texture mapping (the research-card trap): meta 3 LightGray's own
	 * light_gray tail, meta 0 Green's green tail — the UPSTREAM file names were the trap
	 * (MEDIUM/NORMAL et al), this mapping is the code face the copies used.
	 */
	@Test
	void textureTailsFollowTheVariantSemantics() {
		assertEquals("green", GTGrassBlocks.textureOf("grass"));
		assertEquals("lime", GTGrassBlocks.textureOf("grass_lime"));
		assertEquals("black", GTGrassBlocks.textureOf("grass_black"));
		assertEquals("light_gray", GTGrassBlocks.textureOf("grass_light_gray"));
		assertEquals("yellow", GTGrassBlocks.textureOf("grass_yellow"));
		assertEquals("brown", GTGrassBlocks.textureOf("grass_brown"));
	}

	// ------------------------------------------------------------------ the behaviour face

	/**
	 * The no-spread cut: the block extends Block DIRECTLY (SpreadingSnowyDirtBlock's
	 * randomTick would spread/death), declares NO randomTick of its own, and the
	 * registration properties carry no randomTicks flag — against the vanilla control
	 * which IS (vanilla Blocks.java:89 {@code randomTicks()}).
	 */
	@Test
	void grassNeverSpreads() throws Exception {
		assertEquals(Block.class, GTGrassBlock.class.getSuperclass(), "extends Block directly — no spread tick inherited");
		for (var tMethod : GTGrassBlock.class.getDeclaredMethods()) {
			assertFalse(tMethod.getName().equals("randomTick"), "no randomTick override — the upstream empty stub face");
		}
		Object tProps = GTGrassBlock.properties();
		assertFalse(readBoolean(tProps, "isRandomlyTicking"), "randomTicks() deliberately unset");
		// the vanilla control, by CLASS SHAPE (the flag face is registry-timing-sensitive
		// in the test JVM): vanilla grass spreads THROUGH SpreadingSnowyDirtBlock.randomTick,
		// which the GT block does not inherit and does not declare
		assertTrue(net.minecraft.world.level.block.SpreadingSnowyDirtBlock.class
				.isInstance(Blocks.GRASS_BLOCK), "the vanilla spread carrier");
		assertFalse(net.minecraft.world.level.block.SpreadingSnowyDirtBlock.class
				.isAssignableFrom(GTGrassBlock.class), "the GT block inherits no spread tick");
	}

	/** The vanilla-default flammability face: no fire override declared (upstream BlockBase.java:112-117 unoverridden). */
	@Test
	void grassIsNotFlammableByDefault() {
		for (var tMethod : GTGrassBlock.class.getDeclaredMethods()) {
			assertFalse(tMethod.getName().equals("getFlammability"), "no flammability override");
			assertFalse(tMethod.getName().equals("getFireSpreadSpeed"), "no fire-spread override");
			assertFalse(tMethod.getName().equals("isFireSource"), "no fire-source override");
		}
	}

	/**
	 * The declared canSustainPlant deviation: NO override — the block rides the default
	 * face through #minecraft:dirt (the Plains arm holds; the Beach adjacent-water gate is
	 * the declared cut — NeoForge 21.1 removed IPlantable/PlantType, the direct
	 * translation is not portable, see the GTGrassBlock javadoc evidence).
	 */
	@Test
	void canSustainPlantStaysTheDefaultFace() {
		for (var tMethod : GTGrassBlock.class.getDeclaredMethods()) {
			assertFalse(tMethod.getName().equals("canSustainPlant"), "the default face is the declared deviation");
		}
	}

	/**
	 * The numbers are the vanilla grass registration constants (Blocks.java:89): strength
	 * 0.6 (hardness = blast resistance), the grass sound — pinned on the properties seam
	 * (the block instance is unconstructible in the frozen-registry test JVM),
	 * cross-checked against the vanilla block's live face; the mapColour rides the
	 * vanilla-pinned {@code MapColor.GRASS} (a Function-typed Properties field, not
	 * reflectable — the code review face). And NOT the vanilla identity:
	 * EatBlockGoal.java:33/:71 hardcodes Blocks.GRASS_BLOCK — the sheep never eat the GT
	 * block, the automatic parity of a different identity, decisions ⑤.
	 */
	@Test
	void numbersMatchTheVanillaRegistrationRow() throws Exception {
		Object tProps = GTGrassBlock.properties();
		float tStrength = readFloat(tProps, "destroyTime");
		assertEquals(0.6F, tStrength, "the vanilla 0.6 hardness");
		assertEquals(Blocks.GRASS_BLOCK.defaultBlockState().getDestroySpeed(null, null), tStrength,
				"the vanilla block's own live face, cross-checked");
		assertEquals(SoundType.GRASS, readField(tProps, "soundType"), "the vanilla grass sound");
		assertNotSame(Blocks.GRASS_BLOCK, GTGrassBlock.class, "a DIFFERENT identity — the sheep-never-eats parity");
	}

	// the BlockBehaviour.Properties reflection readers — the fields are package-private in vanilla

	private static Object readField(Object aProps, String aName) throws Exception {
		var tField = aProps.getClass().getDeclaredField(aName);
		tField.setAccessible(true);
		return tField.get(aProps);
	}

	private static boolean readBoolean(Object aProps, String aName) throws Exception {
		return (Boolean) readField(aProps, aName);
	}

	private static float readFloat(Object aProps, String aName) throws Exception {
		return (Float) readField(aProps, aName);
	}

	// ------------------------------------------------------------------ the generated-tree snapshots

	/** One committed generated file, parsed (the classpath face of the shared tree). */
	private static JsonObject generated(String aPath) throws Exception {
		try (InputStream tStream = GTGrassBlockTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated file must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** The loot double-track: alternatives [silk self-drop, survives_explosion dirt] per variant (BlockGrass.java:105 shape). */
	@Test
	void lootIsTheDirtSilkDoubleTrack() throws Exception {
		for (String tPath : GTGrassBlocks.PATHS) {
			JsonObject tTable = generated("data/gt6/loot_tables/blocks/" + tPath + ".json");
			JsonArray tPools = tTable.getAsJsonArray("pools");
			assertEquals(1, tPools.size(), "one pool: " + tPath);
			JsonObject tEntry = tPools.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
			assertEquals("minecraft:alternatives", tEntry.get("type").getAsString(), tPath);
			JsonArray tChildren = tEntry.getAsJsonArray("children");
			assertEquals(2, tChildren.size(), "silk arm + dirt arm: " + tPath);
			JsonObject tSilk = tChildren.get(0).getAsJsonObject();
			assertEquals("gt6:" + tPath, tSilk.get("name").getAsString(), "silk touch self-drop: " + tPath);
			assertEquals("minecraft:match_tool", tSilk.getAsJsonArray("conditions").get(0).getAsJsonObject()
					.get("condition").getAsString(), "the silk gate: " + tPath);
			JsonObject tDirt = tChildren.get(1).getAsJsonObject();
			assertEquals("minecraft:dirt", tDirt.get("name").getAsString(), "the constant dirt drop: " + tPath);
			assertEquals("minecraft:survives_explosion", tDirt.getAsJsonArray("conditions").get(0).getAsJsonObject()
					.get("condition").getAsString(), tPath);
			// fortune-immune by construction: no fortune condition anywhere in the table
			assertFalse(tTable.toString().contains("fortune"), "no fortune arm (the upstream :105 ignore): " + tPath);
			assertFalse(tTable.toString().contains("minecraft:grass_block\""), "the vanilla block is not a drop: " + tPath);
		}
	}

	/** The four joined block/item tags contain exactly the 6 paths; the animal spawnables + valid_spawn are ABSENT. */
	@Test
	void tagMembershipIsTheCreatureSpawnEquivalence() throws Exception {
		List<String> tIds = new ArrayList<>();
		for (String tPath : GTGrassBlocks.PATHS) tIds.add("gt6:" + tPath);

		// the joined block tags
		for (String tTag : List.of("minecraft/tags/blocks/dirt.json",
				"minecraft/tags/blocks/mineable/shovel.json",
				"minecraft/tags/blocks/sniffer_diggable_block.json")) {
			List<String> tValues = tagValues(tTag);
			for (String tId : tIds) assertTrue(tValues.contains(tId), tId + " must ride " + tTag);
		}
		// the item-side dirt tag (the per-pair item identity)
		List<String> tItemDirt = tagValues("minecraft/tags/items/dirt.json");
		for (String tId : tIds) assertTrue(tItemDirt.contains(tId), tId + " must ride the items dirt tag");

		// the six animal spawnable tags + valid_spawn: the mod contributes NOTHING
		// (classpath absence here = no gt6 member; vanilla never lists a gt6 block) —
		// the canCreatureSpawn = F equivalence face + the valid_spawn cut
		for (String tTag : List.of("animals_spawnable_on", "wolves_spawnable_on", "foxes_spawnable_on",
				"rabbits_spawnable_on", "parrots_spawnable_on", "frogs_spawnable_on", "valid_spawn")) {
			try (InputStream tStream = GTGrassBlockTest.class.getClassLoader()
					.getResourceAsStream("data/minecraft/tags/blocks/" + tTag + ".json")) {
				if (tStream == null) continue; // the mod produced no file — the absence face
				String tBody = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
				assertFalse(tBody.contains("gt6:"), "the spawnable tag must carry no gt6 member: " + tTag);
			}
		}
	}

	/** One generated tag JSON's values array (the GT6TagsDatagenTest reader shape). */
	private static List<String> tagValues(String aDataPath) throws Exception {
		JsonObject tObject = generated("data/" + aDataPath);
		List<String> rValues = new ArrayList<>();
		for (var tEntry : tObject.getAsJsonArray("values")) rValues.add(tEntry.getAsString());
		return rValues;
	}

	/** The models: cube_bottom_top over the borrowed PNGs + the vanilla dirt bottom, ZERO tintindex (the no-tint ruling). */
	@Test
	void modelsCarryNoTintAndReferenceTheBorrowedTextures() throws Exception {
		for (int i = 0; i < GTGrassBlocks.PATHS.size(); i++) {
			String tPath = GTGrassBlocks.PATHS.get(i);
			String tColour = GTGrassBlocks.textureOf(tPath);
			JsonObject tModel = generated("assets/gt6/models/block/" + tPath + ".json");
			assertFalse(tModel.toString().contains("tintindex"), "zero tint on the pre-coloured PNGs: " + tPath);
			assertEquals("minecraft:block/cube_bottom_top", tModel.get("parent").getAsString(), tPath);
			JsonObject tTextures = tModel.getAsJsonObject("textures");
			assertEquals("minecraft:block/dirt", tTextures.get("bottom").getAsString(),
					"the vanilla dirt bottom (the copied-icon face): " + tPath);
			assertEquals("gt6:block/grass/top_" + tColour, tTextures.get("top").getAsString(), tPath);
			assertEquals("gt6:block/grass/side_" + tColour, tTextures.get("side").getAsString(), tPath);
			// the blockstate is the single property-free variant (the per-pair form)
			JsonObject tState = generated("assets/gt6/blockstates/" + tPath + ".json");
			assertEquals(1, tState.getAsJsonObject("variants").entrySet().size(), "one wildcard variant: " + tPath);
			// and the item model parents the block model
			JsonObject tItem = generated("assets/gt6/models/item/" + tPath + ".json");
			assertEquals("gt6:block/" + tPath, tItem.get("parent").getAsString(), tPath);
		}
	}

	/**
	 * The 12 recipe snapshots: 6 forward (8 vanilla grass + 1 dye TAG -> 8 variants, zero
	 * bare dye items) + 6 reverse (1 variant -> 1 vanilla grass, the BlockGrass.java:72-73
	 * shape). The dye tag namespace is the leg face ({@code forge:} / {@code c:}) — the
	 * //? fork below is the ONLY leg-dependent assertion.
	 */
	@Test
	void recipesAreTheSixForwardSixReverseBand() throws Exception {
		for (int i = 0; i < GTGrassBlocks.PATHS.size(); i++) {
			String tPath = GTGrassBlocks.PATHS.get(i);
			// forward
			JsonObject tForward = generated("data/gt6/recipes/" + tPath + ".json");
			assertEquals("minecraft:crafting_shapeless", tForward.get("type").getAsString(), tPath);
			JsonObject tResult = tForward.getAsJsonObject("result");
			assertEquals("gt6:" + tPath, tResult.get("item").getAsString(), "the variant result: " + tPath);
			assertEquals(8, tResult.get("count").getAsInt(), "the upstream count 8: " + tPath);
			JsonArray tIngredients = tForward.getAsJsonArray("ingredients");
			assertEquals(9, tIngredients.size(), "8 grass + 1 dye: " + tPath);
			int tGrassCount = 0, tDyeCount = 0;
			List<String> tTagBodies = new ArrayList<>();
			for (JsonElement tElement : tIngredients) {
				JsonObject tIngredient = tElement.getAsJsonObject();
				if (tIngredient.has("item")) {
					assertEquals("minecraft:grass_block", tIngredient.get("item").getAsString(),
							"the literal vanilla grass block: " + tPath);
					tGrassCount++;
				} else if (tIngredient.has("tag")) {
					tTagBodies.add(tIngredient.get("tag").getAsString());
					tDyeCount++;
				}
			}
			assertEquals(8, tGrassCount, "exactly eight vanilla grass blocks: " + tPath);
			assertEquals(1, tDyeCount, "exactly one dye TAG (never a bare item): " + tPath);
			assertEquals(dyeTag(i), tTagBodies.get(0), "the dye tag of variant " + i);
			// reverse
			JsonObject tReverse = generated("data/gt6/recipes/" + tPath + "_reverse.json");
			assertEquals("minecraft:crafting_shapeless", tReverse.get("type").getAsString(), tPath);
			JsonObject tReverseResult = tReverse.getAsJsonObject("result");
			assertEquals("minecraft:grass_block", tReverseResult.get("item").getAsString(), "the generify face: " + tPath);
			assertFalse(tReverseResult.has("count") && tReverseResult.get("count").getAsInt() != 1, "count 1: " + tPath);
			JsonArray tReverseIngredients = tReverse.getAsJsonArray("ingredients");
			assertEquals(1, tReverseIngredients.size(), "the single variant item: " + tPath);
			assertEquals("gt6:" + tPath, tReverseIngredients.get(0).getAsJsonObject().get("item").getAsString(), tPath);
		}
	}

	/** The platform dye-tag id of variant i — forge {@code forge:dyes/<color>} vs neo {@code c:dyes/<color>}. */
	private static String dyeTag(int aVariant) {
		String[] tColors = {"green", "lime", "black", "light_gray", "yellow", "brown"};
		//? if forge {
		return "forge:dyes/" + tColors[aVariant];
		//?} else {
		/*return "c:dyes/" + tColors[aVariant];
		 *///?}
	}
}
