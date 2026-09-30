/**
 * The gun family pin test (task pistol-family-items) — the R11-C row of the
 * research.tool-crafting-audit matrix: the pistol/carbine/rifle ITEMS were unregistered
 * (zero registration face, zero crafting rows). This test pins the repaired faces:
 *
 * <ul>
 * <li>the registration census (three GT6Tools rows, the TAB_TABLE 91-row band between
 *     the pocket forms and the armor tail);</li>
 * <li>the basic-attribute pins (durability 512, the bare getBaseDamage 1.0F, the vanilla
 *     sword attack-rate anchor, the :198-200 desc tooltips);</li>
 * <li>the no-behaviour census (isMiningTool=F/isWeapon=F → no useOn/mineBlock/
 *     canPerformAction override — the shooting chain is the DECLARED DEFERRED face);</li>
 * <li>the crafting walk (the upstream OreProcessing_Tool :317-319 arg-8 rows on the
 *     toolHeadWrench listener: the axis gates, the letter item truth, the shapes
 *     verbatim, Steel included) + the generated material_tool JSON spot check.</li>
 * </ul>
 *
 * <p>Row-count pins are the measured item-truth numbers dead-written (the craftfrom
 * ruling-A caliber).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.item.ItemStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.items.tools.GTPistolItem;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6GunFamilyTest extends GTOfflineTestBase {

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/" + aPath + ".json";
		try (InputStream tStream = GT6GunFamilyTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// --------------------------------------------------------------- the registration census

	/** The three GT6Tools rows ride between the pocket forms (63) and the armor tail (67..90) — 91 rows total. */
	@Test
	public void theGunRowsJoinTheTabBetweenPocketAndArmor() {
		assertEquals(91, GT6Tools.TAB_TABLE.size(), "the 88 prior rows + the three gun rows");
		assertSame(GT6Tools.PISTOL, GT6Tools.TAB_TABLE.get(64), "row 64 is the pistol");
		assertSame(GT6Tools.CARBINE, GT6Tools.TAB_TABLE.get(65), "row 65 is the carbine");
		assertSame(GT6Tools.RIFLE, GT6Tools.TAB_TABLE.get(66), "row 66 is the rifle");
		assertEquals(GT6Tools.ARMOR_ROWS.get(0), GT6Tools.TAB_TABLE.get(67), "the armor tail stays put (the ArmorSetTest window)");
		assertEquals(new net.minecraft.resources.ResourceLocation("gt6", "pistol"), GT6Tools.PISTOL.getId());
		assertEquals(new net.minecraft.resources.ResourceLocation("gt6", "carbine"), GT6Tools.CARBINE.getId());
		assertEquals(new net.minecraft.resources.ResourceLocation("gt6", "rifle"), GT6Tools.RIFLE.getId());
		assertEquals(3, GT6Tools.GUN_ROWS.size());
	}

	/** The basic attributes — the ToolStats.java:69 inherited getBaseDamage 1.0F + the family durability + the vanilla rate anchor. */
	@Test
	public void theBasicAttributesMatchTheUpstreamToolStats() {
		assertEquals(512, GTPistolItem.DURABILITY_POINTS, "the family value (the U9*19/U9*28 material scaling is the ladder card's face)");
		assertEquals(1.0F, GTPistolItem.ATTACK_DAMAGE, 0.001F,
				"ToolStats.java:69 getBaseDamage default — the pistol family has NO override (the bare-face pin)");
		assertEquals(-2.4F, GTPistolItem.ATTACK_SPEED, 0.001F, "the vanilla sword rate anchor (no 1.7.10 source)");
		assertEquals("item.gt6.pistol.tooltip", GTPistolItem.Kind.PISTOL.tooltipKey);
		assertEquals("item.gt6.carbine.tooltip", GTPistolItem.Kind.CARBINE.tooltipKey);
		assertEquals("item.gt6.rifle.tooltip", GTPistolItem.Kind.RIFLE.tooltipKey);
		assertEquals("pistol", GTPistolItem.pathOf(GTPistolItem.Kind.PISTOL));
		assertEquals("carbine", GTPistolItem.pathOf(GTPistolItem.Kind.CARBINE));
		assertEquals("rifle", GTPistolItem.pathOf(GTPistolItem.Kind.RIFLE));
	}

	/**
	 * The no-behaviour census — isMiningTool=F (GT_Tool_Pistol :42) and isWeapon=F (:40):
	 * no useOn, no mineBlock, no canPerformAction override. The shooting chain
	 * (Behavior_Gun BULLETS_SMALL/MEDIUM/LARGE) is the DECLARED DEFERRED face.
	 */
	@Test
	public void theBehaviourCensusIsOffAndShootingStaysDeferred() throws NoSuchMethodException {
		assertThrows(NoSuchMethodException.class,
				() -> GTPistolItem.class.getDeclaredMethod("useOn", net.minecraft.world.item.context.UseOnContext.class),
				"the gun must not override useOn (no world arm upstream)");
		assertThrows(NoSuchMethodException.class,
				() -> GTPistolItem.class.getDeclaredMethod("mineBlock", ItemStack.class, net.minecraft.world.level.Level.class,
						net.minecraft.world.level.block.state.BlockState.class, net.minecraft.core.BlockPos.class, net.minecraft.world.entity.LivingEntity.class),
				"the gun must not override mineBlock (isMiningTool=F)");
		assertThrows(NoSuchMethodException.class,
				() -> GTPistolItem.class.getDeclaredMethod("canPerformAction", ItemStack.class, net.minecraftforge.common.ToolAction.class),
				"the gun must not override canPerformAction (isWeapon=F, no TOOL_ behaviour row)");
		// the melee face EXISTS: the pistol-whip point-per-hit fold (getToolDamagePerEntityAttack :38, 200 units)
		assertNotNull(GTPistolItem.class.getDeclaredMethod("hurtEnemy", ItemStack.class,
				net.minecraft.world.entity.LivingEntity.class, net.minecraft.world.entity.LivingEntity.class),
				"the pistol-whip durability face is live");
	}

	// --------------------------------------------------------------- the crafting walk

	/** The shapes are the :317-319 rows verbatim. */
	@Test
	public void theGunShapesAreUpstreamVerbatim() {
		assertEquals(3, GT6GunRecipes.GUN_FORMS.size());
		assertEquals(List.of("XXV", " TH", "d h"), List.of(GT6GunRecipes.GUN_FORMS.get(0).aPattern()), "pistol :317");
		assertEquals(List.of("XXV", "THH", "d h"), List.of(GT6GunRecipes.GUN_FORMS.get(1).aPattern()), "carbine :318");
		assertEquals(List.of("XXX", "HHV", "dTh"), List.of(GT6GunRecipes.GUN_FORMS.get(2).aPattern()), "rifle :319");
	}

	/** The walk census — the measured item-truth number dead-written (the craftfrom ruling-A caliber): 203 materials × 3 forms. */
	@Test
	public void theWalkCensusIsTheMeasuredItemTruth() {
		assertEquals(609, GT6GunRecipes.gunRows().size(), "203 toolHeadWrench-item materials ∩ the :317-319 axis ∩ the letter truth × 3 forms");
		assertEquals(203, GT6GunRecipes.gunRows().stream().map(GT6GunRecipes.GunRow::aMaterial).distinct().count(), "the material universe");
	}

	/** Steel is INCLUDED (no steel anchor — the stamped row IS the steel row); Wood and the ghost rows are OUT. */
	@Test
	public void theWalkCarriesTheAxisGates() {
		List<OreDictMaterial> tWalked = GT6GunRecipes.gunRows().stream().map(GT6GunRecipes.GunRow::aMaterial).distinct().toList();
		assertTrue(tWalked.contains(MT.Steel), "Steel keeps its rows (the blade-family ruling)");
		assertFalse(tWalked.contains(MT.Wood), "MT.Wood.NOT (:317-319 axis verbatim)");
		for (OreDictMaterial tMaterial : tWalked) {
			assertTrue(tMaterial.mToolTypes >= 2, "typemin(2) + the :426 listener gate");
			assertFalse(tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER), "ANTIMATTER.NOT");
			assertFalse(tMaterial.contains(gregapi.data.TD.Properties.BOUNCY) || tMaterial.contains(gregapi.data.TD.Properties.STRETCHY),
					"BOUNCY/STRETCHY.NOT");
			// the ghost-row guard: every emitted row's material-carrying letters resolve
			assertTrue(OP.plateCurved.isGeneratingItem(tMaterial), "the X letter truth");
			assertTrue(OP.screw.isGeneratingItem(tMaterial), "the T letter truth");
		}
	}

	/** The generated iron pistol row — the stamped material_tool JSON (the blade/machine band face). */
	@Test
	public void theIronPistolRowIsTheStampedMaterialToolRow() throws Exception {
		JsonObject tRow = generated("recipes/pistol/iron");
		assertEquals("gt6:material_tool", tRow.get("type").getAsString(), "the stamped serializer");
		assertEquals("gt6:pistol", tRow.getAsJsonObject("result").get("item").getAsString(), "the arg-8 face outputs the TOOL");
		assertEquals("iron", tRow.get("material").getAsString(), "the identity stamp field");
		List<String> tPattern = tRow.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
		assertEquals(List.of("XXV", " TH", "d h"), tPattern, "the :317 shape verbatim");
		JsonObject tKey = tRow.getAsJsonObject("key");
		assertEquals("gt6:plate_curved_iron", tKey.get("X").getAsJsonObject().get("item").getAsString(), "X = the plateCurved item");
		assertEquals("gt6:screw_iron", tKey.get("T").getAsJsonObject().get("item").getAsString(), "T = the screw item");
		assertEquals("minecraft:flint", tKey.get("V").getAsJsonObject().get("item").getAsString(), "V = the :317 arg-13 striker");
		assertTrue(tKey.get("H").getAsJsonObject().has("tag"), "H = the wood-rod tag (the mHandleMaterial port face)");
		assertTrue(tKey.get("d").getAsJsonObject().has("tag"), "d = the screwdriver tool tag");
		assertTrue(tKey.get("h").getAsJsonObject().has("tag"), "h = the hammer tool tag");
		// the advancement pair rides recipes/tools/ (the MaterialToolRow form)
		JsonObject tAdvancement = generated("advancements/recipes/tools/pistol/iron");
		assertTrue(tAdvancement.has("criteria"), "the advancement lands beside the row");
	}
}
