package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * Offline tests for task p31-blade-ladder — the blade material ladder over the UNIFIED
 * {@link GT6ToolLadder} seam (the S31-3 dig/blade unification: the dig surface keeps,
 * the blade increments are the :392 attack fold, the secondary face and the family tint
 * dispatch). The {@link GT6ItemDataTest} boot shape; the mod-Item wall keeps the item
 * instances out of this JVM, so every arm rides the static seams and the constants.
 */
public class GT6BladeLadderTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		MT.init(); // the full material flood (the GT6ItemDataTest boot shape)
	}

	private static ItemStack identified(OreDictMaterial aPrimary, OreDictMaterial aSecondary, float aMultiplier) {
		ItemStack tStack = new ItemStack(Items.STICK);
		GT6ItemData.set(tStack, GT6ToolStats.KEY, GT6ToolStats.of(aPrimary, aSecondary, aMultiplier));
		return tStack;
	}

	// ------------------------------------------------------------- durability

	@Test
	public void durabilityIsThePayloadAtThePinnedRatio() {
		assertEquals(512, GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(new ItemStack(Items.STICK), 1.0F)),
				"the identity-less arm: the synthesized Steel stats — 512 (the legacy value, bit-exact)");
		assertEquals(MT.Steel.mToolDurability,
				GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(identified(MT.Steel, MT.Steel, 1.0F), 1.0F)),
				"Steel 512 → 512 (MultiItemTool.java:182)");
		assertEquals(MT.DamascusSteel.mToolDurability,
				GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(identified(MT.DamascusSteel, MT.DamascusSteel, 1.0F), 1.0F)),
				"DamascusSteel 1280 → 1280");
		assertEquals(MT.TungstenSteel.mToolDurability,
				GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(identified(MT.TungstenSteel, MT.TungstenSteel, 1.0F), 1.0F)),
				"TungstenSteel 5120 → 5120");
		assertEquals(MT.Bronze.mToolDurability,
				GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(identified(MT.Bronze, MT.Bronze, 1.0F), 1.0F)),
				"Bronze 448 → 448");
	}

	@Test
	public void theShapeMultiplierFoldsIntoThePayloadNotTheRead() {
		// the double-axe ×1.5 shape (GT_Tool_AxeDouble :34-37) is a CRAFT-TIME factor —
		// the blade family rows are all ×1.0 (the serializer carries no multiplier field)
		assertEquals((int) (MT.Steel.mToolDurability * 1.5F),
				GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(identified(MT.Steel, MT.Steel, 1.5F), 1.0F)),
				"a ×1.5 payload reads 768 (the legacy axe_double anchor)");
	}

	// ------------------------------------------------------------- attack fold

	@Test
	public void attackDamageIsBasePlusTheMaterialQuality() {
		assertEquals(4.0F + MT.Steel.mToolQuality,
				GT6ToolLadder.attackDamage(identified(MT.Steel, MT.Steel, 1.0F), GTSwordItem.ATTACK_DAMAGE),
				"a Steel sword = 4.0 + 2 = 6.0 (the :392 fold UNLOCKED)");
		assertEquals(2.0F + MT.Steel.mToolQuality,
				GT6ToolLadder.attackDamage(identified(MT.Steel, MT.Steel, 1.0F), GTKnifeItem.ATTACK_DAMAGE),
				"a Steel knife = 4.0");
		assertEquals(1.0F + MT.Steel.mToolQuality,
				GT6ToolLadder.attackDamage(identified(MT.Steel, MT.Steel, 1.0F), GTButcheryKnifeItem.ATTACK_DAMAGE),
				"a Steel butchery knife = 3.0");
		assertEquals(4.0F + MT.TungstenSteel.mToolQuality,
				GT6ToolLadder.attackDamage(identified(MT.TungstenSteel, MT.TungstenSteel, 1.0F), GTSwordItem.ATTACK_DAMAGE),
				"TungstenSteel quality 4 → 8.0");
	}

	@Test
	public void identitylessStacksKeepTheBareBaseDamage() {
		// the t1 pin: the mToolQuality fold rides WITH the identity only (the declared
		// blade face — unlike the dig faces the fallback is the bare constant)
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(4.0F, GT6ToolLadder.attackDamage(tLegacy, GTSwordItem.ATTACK_DAMAGE), "sword legacy = 4.0");
		assertEquals(2.0F, GT6ToolLadder.attackDamage(tLegacy, GTKnifeItem.ATTACK_DAMAGE), "knife legacy = 2.0");
		assertEquals(1.0F, GT6ToolLadder.attackDamage(tLegacy, GTButcheryKnifeItem.ATTACK_DAMAGE), "butchery legacy = 1.0");
	}

	// ------------------------------------------------------------- dig speed

	@Test
	public void theDigSpeedAnchorsAreTheSteelMaterialSpeeds() {
		// the :483 formula per shape = SPEED_MULTIPLIER × mToolSpeed; the Steel fallback
		// (the materialOf face) must equal the legacy MINING_SPEED anchors
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(GTSwordItem.MINING_SPEED, GT6ToolLadder.speed(GTSwordItem.SPEED_MULTIPLIER, GT6ToolLadder.materialOf(tLegacy)),
				"the sword ×1.0 at the Steel fallback = the 6.0F anchor");
		assertEquals(GTKnifeItem.MINING_SPEED, GT6ToolLadder.speed(GTKnifeItem.SPEED_MULTIPLIER, GT6ToolLadder.materialOf(tLegacy)),
				"the knife ×0.5 at the Steel fallback = the 3.0F anchor");
		assertEquals(0.5F * MT.Bronze.mToolSpeed, 2.75F, "the knife at Bronze (mToolSpeed 5.5)");
		assertEquals(1.0F * MT.DamascusSteel.mToolSpeed, 8.0F, "the sword at DamascusSteel (mToolSpeed 8.0)");
	}

	// ------------------------------------------------------------- looting

	@Test
	public void theLootingFormulaIsQualityOverTwoPlusOne() {
		// the ButcheryKnife :92-94 formula per material; the Steel fallback = LOOTING_LEVEL
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(GTButcheryKnifeItem.LOOTING_LEVEL, GT6ToolLadder.materialOf(tLegacy).mToolQuality / 2 + 1,
				"the Steel fallback → the constant 2");
		assertEquals(3, MT.TungstenSteel.mToolQuality / 2 + 1, "TungstenSteel quality 4 → 3");
	}

	// ------------------------------------------------------------- tint

	private static int argbOf(OreDictMaterial aMaterial) {
		return 0xFF000000 | (aMaterial.mRGBaSolid[0] << 16) | (aMaterial.mRGBaSolid[1] << 8) | aMaterial.mRGBaSolid[2];
	}

	@Test
	public void theTintLiteralsAreTheRconChainExpects() {
		// the /gt6blade stats chain asserts the packed literals — locked here so a
		// material-colour drift breaks cleanTest BEFORE the RCON run
		assertEquals(0xFF828282, argbOf(MT.Steel), "Steel 130/130/130");
		assertEquals(0xFF6E6E6E, argbOf(MT.DamascusSteel), "DamascusSteel 110/110/110");
		assertEquals(0xFF6464A0, argbOf(MT.TungstenSteel), "TungstenSteel 100/100/160");
		assertEquals(0xFFD2823C, argbOf(MT.Bronze), "Bronze 210/130/60");
	}

	@Test
	public void theTintLiteralsAreTheRconChainExpects() {
		// the /gt6blade stats chain asserts the packed literals — locked here so a
		// material-colour drift breaks cleanTest BEFORE the RCON run
		assertEquals(0xFF828282, argbOf(MT.Steel), "Steel 130/130/130");
		assertEquals(0xFF6E6E6E, argbOf(MT.DamascusSteel), "DamascusSteel 110/110/110");
		assertEquals(0xFF6464A0, argbOf(MT.TungstenSteel), "TungstenSteel 100/100/160");
		assertEquals(0xFFD2823C, argbOf(MT.Bronze), "Bronze 210/130/60");
	}

	@Test
	public void swordTintCoversTheHeadAndHandleLayers() {
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(argbOf(MT.Steel), GTSwordItem.tintARGB(tLegacy, 0), "index 0 → the Steel fallback (materialOf)");
		assertEquals(argbOf(MT.WOODS.Spruce), GTSwordItem.tintARGB(tLegacy, 2), "index 2 → the Spruce fallback (secondaryOf)");
		assertEquals(-1, GTSwordItem.tintARGB(tLegacy, 1), "the overlay layers stay un-tinted");
		assertEquals(-1, GTSwordItem.tintARGB(tLegacy, 3));

		ItemStack tIdentified = identified(MT.DamascusSteel, MT.Bronze, 1.0F);
		assertEquals(argbOf(MT.DamascusSteel), GTSwordItem.tintARGB(tIdentified, 0), "index 0 = the primary colour");
		assertEquals(argbOf(MT.Bronze), GTSwordItem.tintARGB(tIdentified, 2), "index 2 = the secondary colour");
		assertEquals(-1, GTSwordItem.tintARGB(tIdentified, 1));
	}

	@Test
	public void knifeTintIsTheSecondaryFace() {
		// upstream the knife's visible sprite rides the HANDLE pass (the head pass is
		// VOID, GT_Tool_Knife.getIcon :73-75) — the aIndex0IsSecondary dispatch arm
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(argbOf(MT.WOODS.Spruce), GTKnifeItem.tintARGB(tLegacy, 0), "identity-less → the Spruce fallback");
		ItemStack tIdentified = identified(MT.DamascusSteel, MT.Bronze, 1.0F);
		assertEquals(argbOf(MT.Bronze), GTKnifeItem.tintARGB(tIdentified, 0), "index 0 = the SECONDARY colour");
		assertEquals(-1, GTKnifeItem.tintARGB(tIdentified, 1), "the overlay stays un-tinted");
	}

	@Test
	public void butcheryTintIsThePrimaryFace() {
		// upstream getRGBa :97-99 — the visible sprite takes the PRIMARY colour
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(argbOf(MT.Steel), GTButcheryKnifeItem.tintARGB(tLegacy, 0), "identity-less → the Steel fallback");
		ItemStack tIdentified = identified(MT.DamascusSteel, MT.Bronze, 1.0F);
		assertEquals(argbOf(MT.DamascusSteel), GTButcheryKnifeItem.tintARGB(tIdentified, 0));
		assertEquals(-1, GTButcheryKnifeItem.tintARGB(tIdentified, 1));
	}

	// ----------------------------------------------- the generated recipe rows

	private static JsonObject generated(String aPath) throws Exception {
		try (InputStream tStream = GT6BladeLadderTest.class.getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	public void theGeneratedSwordRowIsTheSharedSerializerShape() throws Exception {
		// the 1.20.1 plural band: the shared gt6:material_tool row (the S31-3 seam
		// switch — the identity stamps at ASSEMBLE time, the JSON carries the snake)
		JsonObject tPlural = generated("/data/gt6/recipes/sword/steel.json");
		assertEquals("gt6:material_tool", tPlural.get("type").getAsString(), "the shared serializer");
		assertEquals("steel", tPlural.get("material").getAsString(), "the snake primary");
		JsonObject tResult = tPlural.getAsJsonObject("result");
		assertEquals("gt6:sword", tResult.get("item").getAsString());
		assertNull(tResult.get("nbt"), "the nbt carrier retired with the decorators");
		assertTrue(tResult.get("count").getAsInt() >= 1, "the count face");

		// the 1.21.1 singular mirror band: the bare {count, id} result (the components
		// carrier retired — the stamp rides the serializer on both legs)
		JsonObject tSingular = generated("/data/gt6/recipe/sword/steel.json").getAsJsonObject("result");
		assertEquals("gt6:sword", tSingular.get("id").getAsString());
		assertEquals(1, tSingular.get("count").getAsInt());
		assertNull(tSingular.get("components"), "the components carrier retired with the decorators");
	}

	@Test
	public void theRetiredSteelRouteRowsAreGoneAndTheClubRowStays() {
		assertNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/sword.json"),
				"the t1 sword steel-route placeholder retired with the ladder");
		assertNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/knife.json"));
		assertNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/butchery_knife.json"));
		assertNotNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/club.json"),
				"the club stays the single-tier convergence row (the single-tier-ruling card owns it)");
		// the ladder rows themselves: the three forms exist for Steel on the plate variant
		assertNotNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/sword/steel.json"));
		assertNotNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/knife/steel.json"));
		assertNotNull(GT6BladeLadderTest.class.getResourceAsStream("/data/gt6/recipes/butchery_knife/steel.json"));
	}
}
