/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.components.OM;
import gregtech6.datagen.GT6ItemTags;

/**
 * The C-leg reload deriver pins (task component-derivation-reload; ADR
 * docs/adr/2026-10-06-components-subsystem.md §1 leg C + red lines 1/3/5).
 *
 * <p>Offline posture: recipes load as REAL shaped JSON through the
 * {@link GTRecipesOfflineTestBase.TestRecipeManager} bridge (the
 * RecipeMapFurnaceBridgeTest dialect — the vanilla serializer face parses, no ctor
 * doubles); the input cells read through {@link OM#anydata_} whose vanilla family-tag
 * arm answers under the injected membership stub (the GT6RecipeTagFallbackTest.java:53
 * seam discipline, the modern stand-in for the 1.7.10 registration-filled map). The
 * deriver's own offline seam {@link GT6ComponentDeriver#sCellMembers} stays
 * production-bound (a no-op) except in its dedicated pin.
 *
 * <p>Acceptance arms: reload idempotency (red line 3, the retire-and-reconcile form),
 * derivation↔A-leg transcription consistency sampling (the derived write reads
 * identical to an explicit declaration of the same aggregation through the same
 * central face), the namespace/manifest gates, the declared non-derivations, the
 * natural-count amortization and the shipped-tree over-derivation census (ADR §1
 * leg C: "交卡须 census 该集并钉数量").
 */
public class GT6ComponentDeriverTest extends GTRecipesOfflineTestBase {

	/** The frozen access handed to {@code getResultItem} — both legs decode the result at
	 * JSON-load time (1.20.1 CraftingHelper ItemStack form / 21.1 the ItemStack.CODEC object
	 * form), so the provider itself is inert; a frozen empty access is enough offline. */
	private static final RegistryAccess ACCESS =
			new RegistryAccess.ImmutableRegistryAccess(Map.of()).freeze();

	static final String ID_MACHINE_BAND = "gt6:derive_machine_band";
	static final String ID_FORCE = "minecraft:derive_force_row";
	static final String ID_AMORTIZE = "gt6:derive_amortize";

	/** The recyclable notification recorder — the duplicate-registration canary. */
	static final List<String> sNotifications = new ArrayList<>();

	@BeforeAll
	static void bootTheComponentDomain() {
		gregtech6.registry.GTMaterialItems.initMaterials(); // MT/OP + the reverse tag lookups
		OM.addListener(tEvent -> sNotifications.add(tEvent.mStack.getItem().toString()));
		sNotifications.clear(); // the addListener catch-up replayed earlier classes' leftovers
	}

	@BeforeEach
	void armTheTagStub() {
		OM.sStackTags = aStack ->
				aStack.getItem() == Items.IRON_INGOT ? Stream.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, "iron"))
				: aStack.getItem() == Items.COPPER_INGOT ? Stream.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, "copper"))
				: Stream.empty();
	}

	@AfterEach
	void cleanTheSharedStaticFace() {
		// the deriver's own removal face is also the test-hygiene face: retire every key these
		// fixtures wrote (map entry + recyclable registration), then the bookkeeping itself.
		for (Item tItem : List.of(Items.IRON_BARS, Items.CAULDRON, Items.IRON_NUGGET, Items.LADDER,
				Items.CLAY_BALL, Items.MINECART, Items.HOPPER)) {
			OM.removeItemData(new ItemStack(tItem));
		}
		GT6ComponentDeriver.sDerived.clear();
		GT6ComponentDeriver.sCellMembers = aCell -> List.of();
		OM.sStackTags = ItemStack::getTags;
		sNotifications.clear();
	}

	// ------------------------------------------------------------- the consistency sample

	/** The A-leg consistency sample: the derived write reads IDENTICAL to an explicit
	 * declaration of the same aggregation through the same central face — the sampled proof
	 * that C (derivation) and A (transcription) converge on one write semantics
	 * (upstream CR.java:368-410 → OM.data(aResult, new OreDictItemData(tData)), :409). */
	@Test
	public void derivedWriteMatchesTheALegTranscriptionOfTheSameRow() {
		TestRecipeManager tManager = managerWith(ID_MACHINE_BAND,
				shaped2x2("minecraft:iron_bars", "minecraft:iron_ingot", 1));
		GT6ComponentDeriver.apply(tManager, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);

		OreDictItemData tDerived = OM.anydata(new ItemStack(Items.IRON_BARS));
		assertNotNull(tDerived, "the gt6-namespace shaped row derived at reload time");
		// the cell read (upstream :386 OM.data_ via the modern read chain — the iron ingot
		// rides the vanilla family-tag arm, the registration-fill isomorph)
		OreDictItemData tCell = OM.anydata_(new ItemStack(Items.IRON_INGOT));
		assertNotNull(tCell, "the input cell resolved through the read chain");
		// the A-leg twin: the same aggregation declared explicitly (the aggregation ctor is
		// the data-model card's pinned CR.java:405-410 face)
		ItemStack tTwin = new ItemStack(Items.MINECART);
		assertTrue(OM.setItemData(tTwin, new OreDictItemData(tCell, tCell, tCell, tCell)));
		OreDictItemData tExpected = OM.data(tTwin);

		assertNull(tDerived.mPrefix, "aggregated data is prefixless (the ctor :102-104)");
		assertTrue(tDerived.mBlackListed);
		assertEquals(tExpected.mPrefix, tDerived.mPrefix);
		// the aggregation ctor re-targets through mTargetReversing (:109-110; ANY.java:233 wires
		// Fe → ANY.Iron "Any Iron Or Steel") — upstream verbatim, NOT the raw MT.Iron cell
		assertEquals(gregapi.data.ANY.Iron, tDerived.mMaterial.mMaterial);
		assertEquals(tExpected.mMaterial.mMaterial, tDerived.mMaterial.mMaterial);
		assertEquals(tExpected.mMaterial.mAmount, tDerived.mMaterial.mAmount,
				"the twin (A leg) and the derivation (C leg) store the same amount");
		assertEquals(4 * tCell.mMaterial.mAmount, tDerived.mMaterial.mAmount,
				"four ingot cells sum (the ctor :113-120)");
		assertEquals(0, tDerived.mByProducts.length);
		assertEquals(0, tExpected.mByProducts.length);
		// exactly one recyclable registration for the output (the central face gate :666-670;
		// prefixless data notifies)
		assertEquals(1, recyclingCount(Items.IRON_BARS));
		assertTrue(sNotifications.contains(Items.IRON_BARS.toString()));
	}

	// ------------------------------------------------------------- the gates

	/** The namespace default + the manifest gates: minecraft-namespace rows stay untouched
	 * without a force (the conservative carrier for the upstream vanilla-domain DEF_REV rows,
	 * Loader_Recipes_Vanilla.java:413-416), force derives them, suppress beats everything —
	 * and a re-gate RETIRES the previous derivation (red line 3: reconcile, not blind rebuild). */
	@Test
	public void theNamespaceAndManifestGatesDecideWhoDerives() {
		TestRecipeManager tManager = managerWith(ID_FORCE,
				shaped2x2("minecraft:cauldron", "minecraft:iron_ingot", 1));

		GT6ComponentDeriver.apply(tManager, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertNull(OM.anydata(new ItemStack(Items.CAULDRON)), "no manifest, no minecraft-namespace derivation");
		assertTrue(GT6ComponentDeriver.sDerived.isEmpty());

		GT6ComponentDeriver.apply(tManager, new GT6ComponentDeriver.Manifest(Set.of(rl(ID_FORCE)), Set.of()), ACCESS);
		assertNotNull(OM.anydata(new ItemStack(Items.CAULDRON)), "the force arm derives");

		GT6ComponentDeriver.apply(tManager, new GT6ComponentDeriver.Manifest(Set.of(rl(ID_FORCE)), Set.of(rl(ID_FORCE))), ACCESS);
		assertNull(OM.anydata(new ItemStack(Items.CAULDRON)), "suppress wins and the re-gate retired the old write");
		assertTrue(GT6ComponentDeriver.sDerived.isEmpty());
		assertEquals(0, recyclingCount(Items.CAULDRON), "the retire pruned the recyclable registration too");

		// the pure truth table: suppress > force > the gt6-namespace default > other namespaces
		GT6ComponentDeriver.Manifest tManifest = new GT6ComponentDeriver.Manifest(Set.of(rl("other:forced")), Set.of(rl("other:shut")));
		assertTrue(tManifest.shouldDerive(rl("gt6:any_row")));
		assertFalse(tManifest.shouldDerive(rl("minecraft:any_row")));
		assertTrue(tManifest.shouldDerive(rl("other:forced")));
		assertFalse(tManifest.shouldDerive(rl("other:shut")));
	}

	/** The reload idempotency pin (ADR red line 3): a second identical pass is mutation-free
	 * (no rewrite, no duplicate recyclable registration — the container has no equals, a blind
	 * rebuild would log copies forever), and a moved graph retires exactly its own keys. */
	@Test
	public void reloadReentryIsIdempotentAndRetiresOnlyMovedDerivations() {
		TestRecipeManager tManager = managerWith(ID_MACHINE_BAND,
				shaped2x2("minecraft:iron_bars", "minecraft:iron_ingot", 1));

		int tBaselineRegistrations = OM.recyclingRegistrations().size(); // review-seat seam: the shared
		// static registration set carries earlier classes' leftovers in a combined-JVM run — the
		// pin asserts THIS test's delta (exactly its own row), not the global absolute.
		GT6ComponentDeriver.apply(tManager, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		OreDictItemData tFirst = OM.data(new ItemStack(Items.IRON_BARS));
		assertNotNull(tFirst);
		int tNotificationsAfterFirst = sNotifications.size();
		assertEquals(tBaselineRegistrations + 1, OM.recyclingRegistrations().size(),
				"exactly the derivation's own registration lands");

		// the byte-identical second /reload
		OM.StackKey tKey1 = new ArrayList<>(GT6ComponentDeriver.sDerived.keySet()).get(0);
		GT6ComponentDeriver.apply(tManager, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertTrue(GT6ComponentDeriver.sDerived.containsKey(tKey1),
				"the (item, damage) key is content-stable across passes (forge ItemStack equality is not — caps compare)");
		assertSame(tFirst, OM.data(new ItemStack(Items.IRON_BARS)), "unchanged rows touch nothing — the stored instance survives");
		assertEquals(tNotificationsAfterFirst, sNotifications.size(), "no re-write fired a second registration");
		assertEquals(tBaselineRegistrations + 1, OM.recyclingRegistrations().size(), "no duplicate registration accumulated");

		// the graph moved under the derivation: the empty graph retires the key
		GT6ComponentDeriver.apply(new TestRecipeManager(), GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertNull(OM.data(new ItemStack(Items.IRON_BARS)));
		assertEquals(tBaselineRegistrations, OM.recyclingRegistrations().size(),
				"removeItemData pruned the map entry AND the registration — back to the pre-test baseline");
		assertTrue(GT6ComponentDeriver.sDerived.isEmpty());
	}

	/** The explicit-declaration shield: the add-only face declines under an A-leg declaration,
	 * the declined key is never tracked, and a later reconcile cannot retire another author's
	 * data (upstream add-only parity, OM.java:641-644). */
	@Test
	public void explicitDeclarationsAreUntouchableByDerivation() {
		ItemStack tHopper = new ItemStack(Items.HOPPER);
		assertTrue(OM.setItemData(tHopper, new OreDictItemData(new OreDictMaterialStack(MT.Obsidian, CS.U))));
		OreDictItemData tExplicit = OM.data(tHopper);

		TestRecipeManager tManager = managerWith("gt6:derive_shield",
				shaped2x2("minecraft:hopper", "minecraft:iron_ingot", 1));
		GT6ComponentDeriver.apply(tManager, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertSame(tExplicit, OM.data(tHopper), "the explicit declaration beat the derivation write");
		assertFalse(GT6ComponentDeriver.sDerived.containsKey(new OM.StackKey(Items.HOPPER, 0)),
				"a declined write is never tracked");

		GT6ComponentDeriver.apply(new TestRecipeManager(), GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertSame(tExplicit, OM.data(tHopper), "the reconcile never retires untracked (explicit) data");
	}

	// ------------------------------------------------------------- the declared non-derivations

	/** Rows with nothing to aggregate write nothing: shapeless v1 (the upstream default
	 * carried no REV, CR.java:454-456) and shaped rows whose cells all resolve null (the
	 * containsSomething gate, upstream :409 UT.Code.containsSomething). */
	@Test
	public void rowsWithNothingToAggregateWriteNothing() {
		TestRecipeManager tShapeless = managerWith("gt6:derive_shapeless",
				"{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:iron_ingot\"}],\"result\":"
						+ resultJson("minecraft:clay_ball", 1) + "}");
		GT6ComponentDeriver.apply(tShapeless, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertNull(OM.anydata(new ItemStack(Items.CLAY_BALL)));

		TestRecipeManager tNullGate = managerWith("gt6:derive_null_gate",
				"{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"S\"],\"key\":{\"S\":{\"item\":\"minecraft:stick\"}},\"result\":"
						+ resultJson("minecraft:ladder", 1) + "}");
		GT6ComponentDeriver.apply(tNullGate, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);
		assertNull(OM.anydata(new ItemStack(Items.LADDER)));
		assertTrue(GT6ComponentDeriver.sDerived.isEmpty());
	}

	/** The tag-cell arm: live, tag members come from {@code Ingredient#getItems()}; when that
	 * enumerates empty (offline the tag manager may not bind) the engine consults
	 * {@link GT6ComponentDeriver#sCellMembers} — production-bound to a no-op, tests inject
	 * membership (the GT6RecipeTagFallbackTest.java:53 seam discipline). The seam fires ONLY
	 * on the empty enumeration — a non-empty live enumeration wins even when it resolves
	 * nothing (the loop never falls through to the seam). */
	@Test
	public void tagCellsResolveThroughTheSeamAndTheProductionBindingIsANoOp() {
		Ingredient tEmpty = Ingredient.of();
		assertEquals(0, tEmpty.getItems().length, "an empty ingredient enumerates empty on both legs");

		assertNull(GT6ComponentDeriver.resolveCell(tEmpty), "the production binding answers empty");

		IdentityHashMap<Ingredient, List<ItemStack>> tMembers = new IdentityHashMap<>();
		tMembers.put(tEmpty, List.of(new ItemStack(Items.COPPER_INGOT)));
		Function<Ingredient, java.util.Collection<ItemStack>> tProduction = GT6ComponentDeriver.sCellMembers;
		GT6ComponentDeriver.sCellMembers = tMembers::get;
		try {
			OreDictItemData tResolved = GT6ComponentDeriver.resolveCell(tEmpty);
			assertNotNull(tResolved);
			assertEquals(MT.Copper, tResolved.mMaterial.mMaterial, "the injected member's data wins on the empty enumeration");
			assertEquals(OP.ingot, tResolved.mPrefix);

			// a non-empty live enumeration beats the seam even when it resolves to nothing
			Ingredient tLive = Ingredient.of(new ItemStack(Items.STICK));
			assertTrue(tLive.getItems().length > 0);
			assertNull(GT6ComponentDeriver.resolveCell(tLive),
					"the seam is never consulted behind a non-empty live enumeration");
		} finally {
			GT6ComponentDeriver.sCellMembers = tProduction;
		}
	}

	/** The natural-count amortization: the engine passes the result stack at its own count
	 * (upstream :409) and the central face divides by it (:653-657) — one craft of a 4-count
	 * output carries a quarter of the input each. */
	@Test
	public void derivationAmortizesByTheNaturalOutputCount() {
		TestRecipeManager tManager = managerWith(ID_AMORTIZE,
				"{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"I\"],\"key\":{\"I\":{\"item\":\"minecraft:iron_ingot\"}},\"result\":"
						+ resultJson("minecraft:iron_nugget", 4) + "}");
		GT6ComponentDeriver.apply(tManager, GT6ComponentDeriver.Manifest.EMPTY, ACCESS);

		OreDictItemData tDerived = OM.anydata(new ItemStack(Items.IRON_NUGGET));
		assertNotNull(tDerived);
		OreDictItemData tCell = OM.anydata_(new ItemStack(Items.IRON_INGOT));
		assertEquals(tCell.mMaterial.mAmount / 4, tDerived.mMaterial.mAmount,
				"the aggregation (1 cell) divided by the output count");

		// the A-leg twin computes the same fraction through the same face
		ItemStack tTwin = new ItemStack(Items.MINECART, 4);
		assertTrue(OM.setItemData(tTwin, new OreDictItemData(tCell)));
		assertEquals(OM.data(tTwin).mMaterial.mAmount, tDerived.mMaterial.mAmount);
	}

	/** The reconcile content probe: structural equality on prefix/material/byproducts — the
	 * pin that keeps a changed recipe graph retiring its stale rows. */
	@Test
	public void theReconcileDiffersProbeIsContentWise() {
		OreDictItemData tBase = new OreDictItemData(new OreDictMaterialStack(MT.Iron, CS.U));
		assertFalse(GT6ComponentDeriver.differs(tBase, new OreDictItemData(new OreDictMaterialStack(MT.Iron, CS.U))),
				"equal content = unchanged row");
		assertTrue(GT6ComponentDeriver.differs(tBase, new OreDictItemData(new OreDictMaterialStack(MT.Copper, CS.U))),
				"material identity differs");
		assertTrue(GT6ComponentDeriver.differs(tBase, new OreDictItemData(new OreDictMaterialStack(MT.Iron, 2 * CS.U))),
				"amount differs");
		assertTrue(GT6ComponentDeriver.differs(tBase,
				new OreDictItemData(new OreDictMaterialStack(MT.Iron, CS.U), new OreDictMaterialStack(MT.Gold, CS.U / 4))),
				"byproduct structure differs");
	}

	// ------------------------------------------------------------- the shipped-tree census

	/**
	 * The over-derivation census (ADR §1 leg C acceptance clause: the gt6-namespace default
	 * derives the WHOLE authored universe, upstream rows without REV included — the set gets
	 * pinned, the harm is bounded by the no-MELTING/no-recycling-row gate,
	 * Loader_OreProcessing.java:252/:257). Counts walk the canonical generated tree
	 * (data/gt6/recipe — the singular 1.21.1 face; the plural recipes/ twin mirrors it,
	 * GT6DualDirectoryFacesTest discipline). EXACT ratchet: recipe PRs bump these numbers.
	 */
	@Test
	public void theShippedTreeCensusPinsTheDerivationUniverse() throws Exception {
		Map<String, Integer> tTypes = new TreeMap<>();
		int tTotal = 0;
		Path tRecipe = mdkRoot().resolve("src/generated/resources/data/gt6/recipe");
		try (Stream<Path> tWalk = Files.walk(tRecipe)) {
			for (Path tFile : tWalk.filter(Files::isRegularFile).toList()) {
				try (InputStream tStream = Files.newInputStream(tFile)) {
					JsonObject tJson = JsonParser.parseReader(new InputStreamReader(tStream, StandardCharsets.UTF_8)).getAsJsonObject();
					JsonElement tType = tJson.get("type");
					tTotal++;
					tTypes.merge(tType == null ? "<none>" : tType.getAsString(), 1, Integer::sum);
				}
			}
		}
		// 41124/23484: 41103/23463 was the mc-E anvil-ladder + kitchen-table re-pin (merge
		// 0af79a237f, +34 crafting_shaped); since then the 16 Asphalt Panel dyeing recipes
		// (merge of 70a3beb773, again no census in that gate domain) and the 5 Lightning
		// Processor rungs (task lightning-processor-pour, +5 crafting_shaped) landed —
		// the review-seat rebase seam re-pins the EXACT ratchet again (shapeless/
		// material_tool/smelting/circuit_program moved by zero; tree walk recomputed
		// byte-identical on both main 41119/23479 and the branch 41124/23484).
		assertEquals(41124, tTotal, "the gt6-namespace shipped recipe universe");
		assertEquals(23484, tTypes.get("minecraft:crafting_shaped"),
				"the derivation candidate set — the over-derivation upper bound (bump-on-change ratchet)");
		assertEquals(4594, tTypes.get("minecraft:crafting_shapeless"),
				"declared skip v1 (CR.java:454 — the shapeless default had no REV)");
		Map<String, Integer> tExpectedOthers = new TreeMap<>();
		tExpectedOthers.put("gt6:material_tool", 12959); // the serializer face owns these
		tExpectedOthers.put("minecraft:smelting", 61);
		tExpectedOthers.put("gt6:circuit_program", 26);
		Map<String, Integer> tActualOthers = new TreeMap<>(tTypes);
		tActualOthers.keySet().removeAll(Set.of("minecraft:crafting_shaped", "minecraft:crafting_shapeless"));
		assertEquals(tExpectedOthers, tActualOthers, "the non-ShapedRecipe skips: serializers + smelting");
		assertEquals(tTotal, tTypes.values().stream().mapToInt(Integer::intValue).sum(), "the census partitions the tree");

		// the minecraft-namespace rows the mod ships stay in the unforced conservative set
		int tMcShaped = 0;
		Path tMc = mdkRoot().resolve("src/generated/resources/data/minecraft/recipe");
		try (Stream<Path> tWalk = Files.walk(tMc)) {
			for (Path tFile : tWalk.filter(Files::isRegularFile).toList()) {
				try (InputStream tStream = Files.newInputStream(tFile)) {
					if (JsonParser.parseReader(new InputStreamReader(tStream, StandardCharsets.UTF_8)).getAsJsonObject()
							.get("type").getAsString().equals("minecraft:crafting_shaped")) tMcShaped++;
				}
			}
		}
		assertEquals(4, tMcShaped, "the shipped minecraft-namespace shaped rows derive only via manifest force");
	}

	/** The manifest load degrades to EMPTY on a missing file (an opt-in knob, not a
	 * load-bearing input) — exercised against an empty ResourceManager double. */
	@Test
	public void theManifestLoadDegradesToEmptyOnAMissingFile() {
		ResourceManager tNoFiles = (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
				new Class<?>[] {ResourceManager.class},
				(aProxy, aMethod, aArgs) -> {
					if (aMethod.getName().equals("getResource")) return java.util.Optional.empty();
					throw new UnsupportedOperationException(aMethod.getName());
				});
		assertSame(GT6ComponentDeriver.Manifest.EMPTY, GT6ComponentDeriver.Manifest.load(tNoFiles));
		assertFalse(GT6ComponentDeriver.Manifest.EMPTY.shouldDerive(rl("minecraft:any_row")));
		assertTrue(GT6ComponentDeriver.Manifest.EMPTY.shouldDerive(rl("gt6:any_row")));
	}

	// ------------------------------------------------------------- fixtures

	private static long recyclingCount(Item aItem) {
		return OM.recyclingRegistrations().stream().filter(tContainer -> tContainer.mStack.getItem() == aItem).count();
	}

	private static ResourceLocation rl(String aId) {
		//? if forge {
		return new ResourceLocation(aId);
		//?} else {
		/*return ResourceLocation.parse(aId); // the full "namespace:path" string parses itself
		*///?}
	}

	/** Loads one recipe JSON through the real vanilla serializer face (the
	 * RecipeMapFurnaceBridgeTest dialect). */
	private static TestRecipeManager managerWith(String aId, String aJson) {
		TestRecipeManager tManager = new TestRecipeManager();
		Map<ResourceLocation, JsonElement> tMap = new HashMap<>();
		tMap.put(rl(aId), JsonParser.parseString(aJson));
		tManager.load(tMap);
		return tManager;
	}

	private static String shaped2x2(String aResult, String aInputItem, int aResultCount) {
		return "{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"II\",\"II\"],"
				+ "\"key\":{\"I\":{\"item\":\"" + aInputItem + "\"}},"
				+ "\"result\":" + resultJson(aResult, aResultCount) + "}";
	}

	/** The result object form: 1.20.1 keys the item, 1.20.5+ (the 21.1 leg) keys the id
	 * (RecipeMapFurnaceBridgeTest's vanilla glass.json precedent). */
	private static String resultJson(String aResultItem, int aCount) {
		//? if forge {
		return "{\"item\":\"" + aResultItem + "\",\"count\":" + aCount + "}";
		//?} else {
		/*return "{\"id\":\"" + aResultItem + "\",\"count\":" + aCount + "}";
		*///?}
	}

	/** Location of the mdk project root (the GT6WoodButtonCraftingJsonTest.mdkRoot shape). */
	private static Path mdkRoot() {
		for (Path tPath = Path.of("").toAbsolutePath(); tPath != null; tPath = tPath.getParent()) {
			if (Files.isRegularFile(tPath.resolve("tools").resolve("gen_textures.py"))) return tPath;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from " + Path.of("").toAbsolutePath());
	}
}
