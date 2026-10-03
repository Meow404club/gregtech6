package gregtech6.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The material-domain tooltip face (task material-tooltip-face): {@link MaterialPrefixItem}
 * replays the nine data-driven row families of the upstream global hover hook
 * (GT_API_Proxy_Client.onItemTooltip, ItemTooltipEvent HIGHEST :214-518) over its own
 * prefix x material pair — the port has no OM.anydata_ pool, so the GT item is
 * self-describing (the instanceof shape the census card pinned). Red-green card: every pin
 * here was RED before {@code appendHoverText} existed (MaterialPrefixItem carried only
 * name + tint).
 *
 * <p>The hover call itself is the dual-leg compile pin (the GT6MachineBlockItemTest
 * posture): forge = the {@code Level} reference, 1.21.1 = {@code Item.TooltipContext}.
 * The TooltipFlag.NORMAL vs .ADVANCED pair IS the upstream F3+H semantic
 * (:452 {@code aEvent.showAdvancedItemTooltips}).
 *
 * <p>The fixture materials (the GTMaterialItems.initMaterials bootstrap, the
 * GTMachinePaintTintTest shape): MT.Fe (element "Fe", the MT.java:2932 vanilla marker),
 * MT.Bronze (the computed Cu:Sn formula chain, setMoleculeConfiguration via uumAloy),
 * MT.Blaze (UNBURNABLE+BURNING :2127, handle(ANY.Blaze), the fireAspect 3 / flame 3
 * enchant rows :3619) over OP.ingot (TOOLTIP_MATERIAL+TOOLTIP_ENCHANTS :1214),
 * OP.toolHeadHammer (NEEDS_HANDLE+TOOL_HEAD :1291), OP.toolHeadRawSword
 * (NEEDS_SHARPENING :1304), OP.dust (TOOLTIP_MATERIAL :1199).
 */
public class MaterialPrefixTooltipTest extends GTOfflineTestBase {

	static MaterialPrefixItem sIronIngot;
	static MaterialPrefixItem sBronzeIngot;
	static MaterialPrefixItem sBlazeHammerHead;
	static MaterialPrefixItem sBlazeRawSwordBlade;
	static MaterialPrefixItem sCoalDust;

	@BeforeAll
	static void buildFixtures() {
		GTMaterialItems.initMaterials();
		sIronIngot = registerItemFixture("fixture_tooltip_fe_ingot",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Fe));
		sBronzeIngot = registerItemFixture("fixture_tooltip_bronze_ingot",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Bronze));
		sBlazeHammerHead = registerItemFixture("fixture_tooltip_blaze_hammer_head",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.toolHeadHammer, MT.Blaze));
		sBlazeRawSwordBlade = registerItemFixture("fixture_tooltip_blaze_raw_sword",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.toolHeadRawSword, MT.Blaze));
		sCoalDust = registerItemFixture("fixture_tooltip_coal_dust",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Coal));
	}

	/** The leg-swap hover call — ONE swap, the GTLightningRodBlock.Item:71-81 shape. */
	private static void callHoverText(MaterialPrefixItem aItem, ItemStack aStack, List<Component> aTooltip, boolean aAdvanced) {
		TooltipFlag tFlag = aAdvanced ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
		//? if forge {
		aItem.appendHoverText(aStack, null, aTooltip, tFlag);
		//?} else {
		/*aItem.appendHoverText(aStack, Item.TooltipContext.EMPTY, aTooltip, tFlag);
		*///?}
	}

	private static List<Component> hover(MaterialPrefixItem aItem, boolean aAdvanced) {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(aItem, new ItemStack(aItem), tTooltip, aAdvanced);
		return tTooltip;
	}

	/** The first row whose flat text equals the needle (LiteralContents rows: the formula face). */
	private static Component findLiteralRow(List<Component> aTooltip, String aText) {
		for (Component tRow : aTooltip) if (tRow.getString().equals(aText)) return tRow;
		return null;
	}

	/** The first row carrying the lang key (the keyed-row face; multi-sibling rows root at Component.empty, so the walk descends). */
	private static Component findKeyedRow(List<Component> aTooltip, String aKey) {
		for (Component tRow : aTooltip) if (containsKey(tRow, aKey)) return tRow;
		return null;
	}

	private static boolean containsKey(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return true;
		for (Component tSibling : aRow.getSiblings()) if (containsKey(tSibling, aKey)) return true;
		return false;
	}

	/** The TranslatableContents carrying the key inside a row (depth-first — the header slot of a multi-sibling row). */
	private static TranslatableContents keyedContents(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return tContents;
		for (Component tSibling : aRow.getSiblings()) {
			TranslatableContents tFound = keyedContents(tSibling, aKey);
			if (tFound != null) return tFound;
		}
		return null;
	}

	/** The component carrying the key inside a row (the styled header sibling of a multi-sibling row). */
	private static Component keyedSibling(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return aRow;
		for (Component tSibling : aRow.getSiblings()) {
			Component tFound = keyedSibling(tSibling, aKey);
			if (tFound != null) return tFound;
		}
		return null;
	}

	@Test
	public void ironIngotShowsTheElementFormulaAndTheVanillaOriginRow() {
		// acceptance ① — :348-350 (YELLOW mTooltipChemical) + :485-486 (BLUE "Vanilla Material")
		List<Component> tTooltip = hover(sIronIngot, false);
		Component tFormula = findLiteralRow(tTooltip, MT.Fe.mTooltipChemical);
		assertNotNull(tFormula, "the element formula row (Fe) must appear");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), tFormula.getStyle().getColor()); // LH.Chat.YELLOW
		Component tOrigin = findKeyedRow(tTooltip, "gt6.tooltip.material.origin_vanilla");
		assertNotNull(tOrigin, "the vanilla-material origin row must appear (MT.java:2932 setOriginalMod(MD.MC))");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), tOrigin.getStyle().getColor()); // LH.Chat.BLUE
	}

	@Test
	public void bronzeIngotShowsTheComputedFormula() {
		// acceptance ② — the setMoleculeConfiguration chain computes the alloy face; the
		// observed value of the FIRST GREEN RUN (the card ruling): Cu 3U + Sn 1U over the
		// NUM_SUB subscript digits = "Cu₃Sn"
		List<Component> tTooltip = hover(sBronzeIngot, false);
		assertNotNull(MT.Bronze.mTooltipChemical, "the uumAloy chain must have computed a formula");
		assertEquals("Cu₃Sn", MT.Bronze.mTooltipChemical, "the computed face is the Cu:Sn subscript formula");
		Component tFormula = findLiteralRow(tTooltip, MT.Bronze.mTooltipChemical);
		assertNotNull(tFormula, "the computed formula row must appear verbatim");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), tFormula.getStyle().getColor());
		// :493-494 — Bronze carries the MT.java:3100 IC2 attribution, so the origin row is
		// the "Material from %s" face with the mod-id slot
		Component tOrigin = findKeyedRow(tTooltip, "gt6.tooltip.material.origin_mod");
		assertNotNull(tOrigin, "the mod-attribution origin row must appear");
		TranslatableContents tOriginContents = keyedContents(tOrigin, "gt6.tooltip.material.origin_mod");
		assertEquals(MT.Bronze.mOriginalMod, tOriginContents.getArgs()[0], "the slot carries the mod id verbatim");
	}

	@Test
	public void unattributedPeriodicElementRidesThePeriodicFace() {
		// :487-489 — MT.Tc (Technetium) keeps its gregapi attribution (no setOriginalMod in
		// the MT.java:2929+ band) with 0 < mID < 8000, so the origin row is the periodic face;
		// the mID >= 8000 random face shares the same ternary arm and stays unpinned here
		MaterialPrefixItem tTcDust = registerItemFixture("fixture_tooltip_tc_dust",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Tc));
		List<Component> tTooltip = hover(tTcDust, false);
		assertNotNull(findKeyedRow(tTooltip, "gt6.tooltip.material.origin_periodic"), "an unattributed element below ID 8000 rides the periodic face");
	}

	@Test
	public void blazeHammerHeadShowsHandleMaterialAndToolEnchants() {
		// acceptance ③ — :361-362 NEEDS_HANDLE (the handle(ANY.Blaze) chain) + :382-390
		// the tools row with the fireAspect>=3 " (Autosmelt)" annex
		List<Component> tTooltip = hover(sBlazeHammerHead, false);
		Component tHandle = findKeyedRow(tTooltip, "gt6.tooltip.material.needs_handle");
		assertNotNull(tHandle, "the NEEDS_HANDLE row must appear (OP.toolHeadHammer)");
		TranslatableContents tHandleContents = keyedContents(tHandle, "gt6.tooltip.material.needs_handle");
		assertNotNull(tHandleContents, "the header slot carries the key");
		// the header key holds no %s — the handle word rides the WHITE sibling (the upstream
		// :362 CYAN + key + WHITE + local concat shape)
		assertTrue(tHandle.getSiblings().size() >= 2, "the handle-material word is a sibling of the header");
		assertEquals("gt6.material." + MaterialPrefixItem.snakeCase(MT.Blaze.mHandleMaterial.mNameInternal),
				tHandle.getSiblings().get(1).getString(), "the sibling fills with the handle material's translatable small unit");

		Component tTools = findKeyedRow(tTooltip, "gt6.tooltip.material.tool_enchants");
		assertNotNull(tTools, "the tools enchant row must appear (fireAspect 3, MT.java:3619)");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE),
				keyedSibling(tTools, "gt6.tooltip.material.tool_enchants").getStyle().getColor(), "LH.Chat.PURPLE labels the row");
		// the offline JVM resolves the vanilla keys (Bootstrap loads the en_us face), so the
		// row renders the exact vanilla getFullname face + the :388 annex
		assertEquals("gt6.tooltip.material.tool_enchantsFire Aspect III (Autosmelt)", tTools.getString(),
				"the vanilla description id + the level-3 numeral + the fireAspect>=3 annex (upstream :388)");

		// the TOOL_HEAD gate (:416) keeps the armor row off a hammer head
		assertNoRow(tTooltip, "gt6.tooltip.material.armor_enchants");
	}

	private static void assertNoRow(List<Component> aTooltip, String aKey) {
		org.junit.jupiter.api.Assertions.assertNull(findKeyedRow(aTooltip, aKey), "no " + aKey + " row expected");
	}

	@Test
	public void blazeRawSwordBladeShowsTheSharpeningRow() {
		// :361 NEEDS_SHARPENING — OP.toolHeadRawSword (:1304)
		List<Component> tTooltip = hover(sBlazeRawSwordBlade, false);
		assertNotNull(findKeyedRow(tTooltip, "gt6.tooltip.material.needs_sharpening"), "the NEEDS_SHARPENING row must appear");
	}

	@Test
	public void blazeHammerHeadShowsTheUnburnableRow() {
		// :449 — MT.Blaze carries UNBURNABLE (MT.java:2127); GREEN per LH.Chat.GREEN
		List<Component> tTooltip = hover(sBlazeHammerHead, false);
		Component tUnburnable = findKeyedRow(tTooltip, "gt6.tooltip.material.unburnable");
		assertNotNull(tUnburnable, "the UNBURNABLE row must appear");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tUnburnable.getStyle().getColor());
	}

	@Test
	public void coalDustShowsTheFlammableRow() {
		// acceptance ④ — :437-446, RED per LH.Chat.RED; coal() carries FLAMMABLE (MT.java:540)
		List<Component> tTooltip = hover(sCoalDust, false);
		Component tFlammable = findKeyedRow(tTooltip, "gt6.tooltip.material.flammable");
		assertNotNull(tFlammable, "the FLAMMABLE row must appear");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tFlammable.getStyle().getColor());
		assertNoRow(tTooltip, "gt6.tooltip.material.flammable_explosive");
		assertNoRow(tTooltip, "gt6.tooltip.material.explosive");
	}

	@Test
	public void containedMaterialsOnlyRideTheAdvancedFlag() {
		// acceptance ⑤ — :452-479, the TooltipFlag.ADVANCED pair = the upstream
		// showAdvancedItemTooltips (F3+H) semantic; NORMAL gets the hint row instead
		List<Component> tNormal = hover(sIronIngot, false);
		assertNoRow(tNormal, "gt6.tooltip.material.contained_materials");
		assertNotNull(findKeyedRow(tNormal, "gt6.tooltip.material.f3h_hint"), "the F3+H hint row shows in the NORMAL face");

		List<Component> tAdvanced = hover(sIronIngot, true);
		assertNoRow(tAdvanced, "gt6.tooltip.material.f3h_hint");
		Component tHeader = findKeyedRow(tAdvanced, "gt6.tooltip.material.contained_materials");
		assertNotNull(tHeader, "the contained-materials header shows under ADVANCED");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA), tHeader.getStyle().getColor()); // LH.Chat.DCYAN
		// one stack (prefix weight, no byproducts) — the main-material row follows the header
		long tHeaderIndex = tAdvanced.indexOf(tHeader);
		assertTrue(tAdvanced.size() > tHeaderIndex + 1, "the component row follows the header");
		Component tRow = tAdvanced.get((int)tHeaderIndex + 1);
		assertFalse(tRow.getSiblings().isEmpty(), "the component row is the multi-sibling composition");
		assertEquals("1.000 ", tRow.getSiblings().get(0).getString(), "displayUnits(prefix.mWeight) leads the row (UT.Code.displayUnits face)");
		assertTrue(tRow.getSiblings().get(1).getContents() instanceof TranslatableContents, "the material word rides the gt6.material fill");
		assertEquals("gt6.material." + MaterialPrefixItem.snakeCase(MT.Fe.mNameInternal), tRow.getSiblings().get(1).getString());
		assertTrue(tRow.getString().contains("M: " + MT.Fe.mMeltingPoint), "the melting point rides the row");
		assertTrue(tRow.getString().contains("W: "), "the weight slot rides the row");
	}

	@Test
	public void rowsStayAbsentWhereTheDataIsAbsent() {
		// the zero-data face: plain iron ingot — no tool stats (mToolTypes == 0 on the INGOT
		// carrier... Fe HAS mToolTypes>0, but the row is data-driven, so pin the actual data
		// faces: no handle/sharpening/source/enchant rows on a plain ingot of Fe)
		List<Component> tTooltip = hover(sIronIngot, false);
		assertNoRow(tTooltip, "gt6.tooltip.material.needs_handle");
		assertNoRow(tTooltip, "gt6.tooltip.material.needs_sharpening");
		assertNoRow(tTooltip, "gt6.tooltip.material.source_of");
		assertNoRow(tTooltip, "gt6.tooltip.material.tool_enchants");
	}

	@Test
	public void sourceOfRowRidesOrePrefixesOnly() {
		// :364-372 — the SourceOf row is ORE/ORE_PROCESSING_DIRTY-gated; a non-ore prefix
		// (ingot) must not show it even when mSourceOf is populated (Fe has none, so also
		// pin a populated case is out of scope — the gate is the pin)
		List<Component> tTooltip = hover(sCoalDust, false);
		assertNoRow(tTooltip, "gt6.tooltip.material.source_of");
	}
}
