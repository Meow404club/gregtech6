package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * Offline tests for task p31-dig-ladder — the dig-family material ladder over the
 * {@link GT6ToolLadder} seam (the DigSixTest pure-static premise; the mod-Item wall
 * keeps the items out, the math rides vanilla stacks: the identity is item-agnostic).
 *
 * <p>Material anchors (MT.java, the qual(type, speed, durability, quality) rows):
 * Steel = qual(3, 6.0, 512, 2) :2459 (the identity-less fallback = the pre-ladder
 * constants), Bronze = qual(3, 5.5, 448, 2) :2453, Cu = qual(2, 4.0, 64, 0) :1438
 * (the quality-starved gate arm), TungstenSteel = qual(3, 10.0, 5120, 4) :2465 (the
 * only dig-family material above the diamond gate).
 */
public class DigLadderTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the full material flood — MT class-load only registers NULL (the GT6ItemDataTest boot shape)
		MT.init();
	}

	private static ItemStack stamped(OreDictMaterial aMaterial, float aMultiplier) {
		return GT6ToolLadder.stampIdentity(new ItemStack(Items.STICK), aMaterial, aMultiplier);
	}

	// ------------------------------------------------------------------ durability

	/** The identity-less arm = Steel bit-exact: 512 points at ×1.0, the gem pick's ×0.25 = 128. */
	@Test
	public void identityLessFallbackIsSteelBitExact() {
		ItemStack tBare = new ItemStack(Items.STICK);
		assertEquals(512, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(tBare, 1.0F)),
				"the pre-ladder family constant IS the steel fallback (MT.Steel 512)");
		assertEquals(128, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(tBare, GTPickaxeGemItem.DURABILITY_MULTIPLIER)),
				"the gem pick ×0.25 = the flat 128 (GTPickaxeGemItem pre-ladder constant)");
	}

	/** The ladder face: j = mToolDurability × 100 × multiplier → points = mToolDurability × multiplier (:182). */
	@Test
	public void durabilityScalesPerMaterial() {
		assertEquals(448, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(stamped(MT.Bronze, 1.0F), 1.0F)),
				"Bronze qual(3, 5.5, 448, 2) → 448 points");
		assertEquals(5120, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(stamped(MT.TungstenSteel, 1.0F), 1.0F)),
				"TungstenSteel qual(3, 10.0, 5120, 4) → 5120 points");
	}

	// ------------------------------------------------------------------ speed

	/** speed = formMultiplier × mToolSpeed (:483): the pre-ladder anchors at Steel, the ladder face at Bronze. */
	@Test
	public void speedIsFormMultiplierTimesMaterialSpeed() {
		assertEquals(6.0F, GT6ToolLadder.speed(1.0F, MT.Steel), 0.0F, "the pickaxe/shovel/hoe/axe anchor");
		assertEquals(12.0F, GT6ToolLadder.speed(GTPickaxeConstructionItem.SPEED_MULTIPLIER, MT.Steel), 0.0F, "×2 construction");
		assertEquals(9.0F, GT6ToolLadder.speed(GTSpadeItem.SPEED_MULTIPLIER, MT.Steel), 0.0F, "×1.5 spade");
		assertEquals(4.5F, GTUniversalSpadeItem.MINING_SPEED, 0.0F, "×0.75 universal (unchanged, unconverted — the pre-ladder constant stands)");
		assertEquals(5.5F, GT6ToolLadder.speed(1.0F, MT.Bronze), 0.0F, "a bronze pick mines at the bronze speed");
		assertEquals(10.0F, GT6ToolLadder.speed(1.0F, MT.TungstenSteel), 0.0F, "tungstensteel ×10.0");
	}

	// ------------------------------------------------------------------ the quality gate

	/**
	 * The :482 gate math — the level half is {@code baseQuality + mToolQuality} (the
	 * upstream :482/:495 formula). The BLOCK-requirement half (the vanilla needs_* tier
	 * tags) binds only with the datapack — offline every block answers requiredLevel 0,
	 * so the tag-arm denials (copper gated on iron ore, bronze on obsidian,
	 * tungstensteel passing the diamond tier) are pinned LIVE by the RCON material
	 * chain (tool_ladder_dig_materials, the speed=0.0 arms). Here the formula half:
	 */
	@Test
	public void miningLevelIsBaseQualityPlusMaterialQuality() {
		assertEquals(0, MT.Cu.mToolQuality, "copper is the quality-0 anchor");
		assertEquals(2, MT.Steel.mToolQuality, "steel is the pre-ladder anchor");
		assertEquals(4, MT.TungstenSteel.mToolQuality, "tungstensteel is the diamond-tier anchor");
		assertEquals(2, GT6ToolLadder.miningLevel(0, MT.Steel), "the dig family base quality = 0");
		assertEquals(0, GT6ToolLadder.miningLevel(0, MT.Cu));
		assertEquals(4, GT6ToolLadder.miningLevel(0, MT.TungstenSteel));
		// the offline tag-blind corollary: with the tags unbound every gate reads false
		assertFalse(GT6ToolLadder.qualityGate(stamped(MT.Cu, 1.0F), state(Blocks.STONE)),
				"requiredLevel offline = 0 — the denials bind live (the DigSixTest tag-arm premise)");
	}

	// ------------------------------------------------------------------ tint + name

	/** tintARGB: index 0 = the material mRGBaSolid with the steel fallback, other indices the -1 sentinel. */
	@Test
	public void tintFollowsTheMaterialWithTheSteelFallback() {
		int tBronze = 0xFF000000 | (MT.Bronze.mRGBaSolid[0] << 16) | (MT.Bronze.mRGBaSolid[1] << 8) | MT.Bronze.mRGBaSolid[2];
		assertEquals(tBronze, GT6ToolLadder.tintARGB(stamped(MT.Bronze, 1.0F), 0));
		assertEquals(-1, GT6ToolLadder.tintARGB(stamped(MT.Bronze, 1.0F), 1), "the overlay pass stays un-tinted");
		int tSteel = 0xFF000000 | (MT.Steel.mRGBaSolid[0] << 16) | (MT.Steel.mRGBaSolid[1] << 8) | MT.Steel.mRGBaSolid[2];
		assertEquals(tSteel, GT6ToolLadder.tintARGB(new ItemStack(Items.STICK), 0), "the identity-less arm = the steel fallback");
	}

	/** displayName: bare for identity-less, the (Material) suffix over the gt6.material.* family when stamped. */
	@Test
	public void displayNameComposesTheMaterialSuffix() {
		ItemStack tBare = new ItemStack(Items.STICK);
		assertEquals("item.gt6.pickaxe", GT6ToolLadder.displayName(tBare, "item.gt6.pickaxe").getString(),
				"identity-less keeps the bare form (the unresolved key offline)");
		String tStamped = GT6ToolLadder.displayName(stamped(MT.Bronze, 1.0F), "item.gt6.pickaxe").getString();
		assertTrue(tStamped.startsWith("item.gt6.pickaxe (") && tStamped.endsWith(")"), "the P7 paren convention: " + tStamped);
		assertTrue(tStamped.contains("gt6.material.bronze"), "the material slot rides the existing lang family: " + tStamped);
	}

	// ------------------------------------------------------------------ the identity payload face

	/** The stamp writes the upstream verbatim compound (short 'a', long 'j') readable through the seam. */
	@Test
	public void stampIdentityWritesTheUpstreamCompound() {
		ItemStack tStack = stamped(MT.Bronze, 1.0F);
		//? if forge {
		CompoundTag tRoot = tStack.getTag();
		assertTrue(tRoot != null && tRoot.contains("GT.ToolStats"), "the upstream compound name");
		CompoundTag tToolTag = tRoot.getCompound("GT.ToolStats");
		assertEquals((short) MT.Bronze.mID, tToolTag.getShort("a"), "the short-typed primary (GT6ToolStats 'a' discipline)");
		assertTrue(tToolTag.contains("j", Tag.TAG_LONG), "the long-typed budget");
		assertEquals(MT.Bronze.mToolDurability * 100L, tToolTag.getLong("j"), "j = mToolDurability * 100 * 1.0 (:182)");
		//?} else {
		/*GT6ToolStats tBack = GT6ItemData.get(tStack, GT6ToolStats.KEY);
		assertSame(MT.Bronze, tBack.primaryMaterial(), "the DC carrier round-trips the material");
		assertEquals(MT.Bronze.mToolDurability * 100L, tBack.maxDamage(), "j = mToolDurability * 100 * 1.0 (:182)");
		*///?}
	}

	/** The seam read half: the stamped identity resolves through find(). */
	@Test
	public void stampedIdentityRoundTrips() {
		GT6ToolStats tStats = GT6ItemData.get(stamped(MT.Bronze, 1.0F), GT6ToolStats.KEY);
		assertSame(MT.Bronze, tStats.primaryMaterial());
		assertTrue(GT6ItemData.find(new ItemStack(Items.STICK), GT6ToolStats.KEY).isEmpty(), "the bare arm stays explicit-missing");
	}

	// ------------------------------------------------------------------ the material-tool recipe face

	//? if forge {
	/** The serializer parse + the assemble-time stamp (the vanilla shaped JSON + the material field). */
	@Test
	public void materialToolSerializerParsesAndStamps() {
		com.google.gson.JsonObject tJson = new com.google.gson.JsonObject();
		tJson.addProperty("category", "equipment");
		com.google.gson.JsonArray tPattern = new com.google.gson.JsonArray();
		tPattern.add("P");
		tJson.add("pattern", tPattern);
		com.google.gson.JsonObject tKey = new com.google.gson.JsonObject();
		com.google.gson.JsonObject tPlate = new com.google.gson.JsonObject();
		tPlate.addProperty("tag", "c:plates/bronze");
		tKey.add("P", tPlate);
		tJson.add("key", tKey);
		com.google.gson.JsonObject tResult = new com.google.gson.JsonObject();
		tResult.addProperty("item", "minecraft:stick"); // the mod-Item wall: the vanilla item carries the stamp face
		tJson.add("result", tResult);
		tJson.addProperty("material", "bronze");

		GT6MaterialToolRecipe.Serializer tSerializer = new GT6MaterialToolRecipe.Serializer();
		GT6MaterialToolRecipe tRecipe = tSerializer.fromJson(new net.minecraft.resources.ResourceLocation("gt6", "pickaxe/bronze"), tJson);
		net.minecraft.world.inventory.CraftingContainer tContainer = new net.minecraft.world.inventory.TransientCraftingContainer(null, 1, 1);
		ItemStack tAssembled = tRecipe.assemble(tContainer, net.minecraft.core.RegistryAccess.EMPTY);
		OreDictMaterial tPrimary = GT6ItemData.get(tAssembled, GT6ToolStats.KEY).primaryMaterial();
		assertSame(MT.Bronze, tPrimary, "the assembled stack carries the row's material identity");

		assertThrows(com.google.gson.JsonSyntaxException.class,
				() -> tSerializer.fromJson(new net.minecraft.resources.ResourceLocation("gt6", "pickaxe/bogus"),
						tJsonDeepCopy(tJson, "not_a_material")),
				"an unknown material is a parse error, never a silent NULL");
	}

	private com.google.gson.JsonObject tJsonDeepCopy(com.google.gson.JsonObject aJson, String aMaterial) {
		com.google.gson.JsonObject tCopy = aJson.deepCopy();
		tCopy.addProperty("material", aMaterial);
		return tCopy;
	}
	//?} else {
	/*// The 1.21.1 stamp face: the recipe constructed directly (the codec parse face rides
	// the datagen emit + the runData idempotence, the FML JVM pins the serializer registration).
	@Test
	public void materialToolAssemblerStampsTheIdentity() {
		java.util.Map<Character, Ingredient> tKey = java.util.Map.of('P', Ingredient.of(Items.STICK));
		GT6MaterialToolRecipe tRecipe = new GT6MaterialToolRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
				net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, java.util.List.of("P")),
				new ItemStack(Items.STICK), true, "bronze");
		ItemStack tAssembled = tRecipe.assemble(net.minecraft.world.item.crafting.CraftingInput.of(1, 1, java.util.List.of(new ItemStack(Items.STICK))),
				net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
		OreDictMaterial tPrimary = GT6ItemData.get(tAssembled, GT6ToolStats.KEY).primaryMaterial();
		assertSame(MT.Bronze, tPrimary, "the assembled stack carries the row's material identity");
	}
	*///?}

	// ------------------------------------------------------------------ the raw block states

	private static BlockState state(net.minecraft.world.level.block.Block aBlock) {
		return aBlock.defaultBlockState();
	}
}
