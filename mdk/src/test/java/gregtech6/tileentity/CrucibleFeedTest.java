package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialGraph;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CruciblePhysics;
import gregtech6.components.OM;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * The crucible feed ladder over the CENTRAL COMPONENT FACE (task
 * component-crucible-feed-resolve, MS-3's first domain-wide consumer): the shared
 * {@link CrucibleFeed} consults {@code OM.anydata} (upstream Crucible:209) and walks the
 * upstream arms (:213-232) over the READ DATA. Pin faces:
 * <ul>
 * <li><b>feed ↔ central-face consistency</b> — the ladder emits exactly what the read
 *     chain answers (the provider arm on prefix items, the map arm on plain data-carrying
 *     items, the family-tag arm on vanilla items), count-scaled once;</li>
 * <li><b>alloy-page row ↔ feed result consistency</b> — the CRUCIBLE_ALLOYING dust row's
 *     own inputs melt into exactly the row's component amounts, summing to the row's
 *     output (the page↔feed closure);</li>
 * <li><b>the declared deviations</b> — the whole-slot count scaling (uniform over the ore
 *     arms too, the r4-20b latent-loss fix), the vanilla-ore fallback bridge, the deleted
 *     ingot/nugget bridges riding the family-tag arm through a membership stub.</li>
 * </ul>
 */
public class CrucibleFeedTest {

	static MaterialPrefixItem DUST_IRON, ORE_IRON, RAW_IRON, BLOCKRAW_IRON, DENSE_IRON, DUST_SILVER, DUST_GOLD, INGOT_ELECTRUM;
	static Item PLAIN_ITEM;

	/** The saved production binding of the tag seam (restored in {@link #leaveTheProductionBindings()}). */
	static Function<ItemStack, Stream<TagKey<Item>>> sSavedTags;
	static Function<GT6RecipeMapCrucible.MatRequest, ItemStack> sSavedResolver;

	/**
	 * The family-tag membership stub for the vanilla ingot/nugget pins: the real tag
	 * manager never boots offline, so the read chain's family-tag arm
	 * ({@code OM.resolveByFamilyTags}) reads empty — the stub feeds it the ids the live
	 * VANILLA_INTERSECTION emission produces ({@code forge:ingots/iron =
	 * [minecraft:iron_ingot, gt6:ingot_iron]}, the ADR leg-B face). The "c" namespace
	 * rides {@code GT6ItemTags.COMMON_NAMESPACE} — the leg-neutral face (the forge leg's
	 * MATERIALS_NAMESPACE is "forge", the neo leg's is "c"; both accept "c").
	 *
	 * <p>Built in {@link #boot()}, NOT a static initializer: {@code Items.*} cannot be
	 * touched at class-init (before any {@code Bootstrap.bootStrap()} the registry classes
	 * fail to even load offline — the isolated-run initializationError).
	 */
	static Map<Item, TagKey<Item>> FAMILY_TAGS;

	static TagKey<Item> family(String aPath) {
		return TagKey.create(Registries.ITEM, new ResourceLocation("c", aPath));
	}

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GT6MaterialTestSupport.materials(); // the hermetic bracket: reset FIRST, then the full refill
		MaterialGraph.applyCrucibleAlloyReferences(); // the alloy-creation configs for the page pin
		DUST_IRON = probePrefix("feed_probe_dust_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Iron));
		ORE_IRON = probePrefix("feed_probe_ore_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.ore, MT.Iron));
		RAW_IRON = probePrefix("feed_probe_ore_raw_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.oreRaw, MT.Iron));
		BLOCKRAW_IRON = probePrefix("feed_probe_block_raw_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.blockRaw, MT.Iron));
		DENSE_IRON = probePrefix("feed_probe_ore_dense_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.oreDense, MT.Iron));
		DUST_SILVER = probePrefix("feed_probe_dust_silver", () -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Silver));
		DUST_GOLD = probePrefix("feed_probe_dust_gold", () -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Au));
		// the alloy-page row synthesis resolves the ROW OUTPUT through the ingot ladder too
		// (GT6RecipeMapCrucible.alloyConfigRows :478 — a null output kills the whole config);
		// the GT6RecipeMapCrucibleTest posture (its INGOT_ELECTRUM probe)
		INGOT_ELECTRUM = probePrefix("feed_probe_ingot_electrum", () -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Electrum));
		PLAIN_ITEM = probePlain("feed_probe_plain_item");

		FAMILY_TAGS = Map.of(
				Items.IRON_INGOT, family("ingots/iron"),
				Items.GOLD_INGOT, family("ingots/gold"),
				Items.COPPER_INGOT, family("ingots/copper"),
				Items.IRON_NUGGET, family("nuggets/iron"),
				Items.GOLD_NUGGET, family("nuggets/gold"));
		sSavedTags = OM.sStackTags;
		OM.sStackTags = aStack -> {
			TagKey<Item> tTag = FAMILY_TAGS.get(aStack.getItem());
			return tTag == null ? Stream.empty() : Stream.of(tTag);
		};
		sSavedResolver = GT6RecipeMapCrucible.sMatResolver;
		GT6RecipeMapCrucible.sMatResolver = r -> {
			MaterialPrefixItem tItem = null;
			if (r.prefix() == OP.dust && r.material() == MT.Silver) tItem = DUST_SILVER;
			if (r.prefix() == OP.dust && r.material() == MT.Au) tItem = DUST_GOLD;
			if (r.prefix() == OP.ingot && r.material() == MT.Electrum) tItem = INGOT_ELECTRUM;
			return tItem == null || r.count() < 1 ? null : new ItemStack(tItem, (int)Math.min(64, r.count()));
		};
	}

	@AfterAll
	static void leaveTheProductionBindings() {
		OM.sStackTags = sSavedTags;
		GT6RecipeMapCrucible.sMatResolver = sSavedResolver;
	}

	static MaterialPrefixItem probePrefix(String aProbeId, java.util.function.Supplier<MaterialPrefixItem> aCreator) {
		net.minecraft.core.Registry<Item> tRegistry = BuiltInRegistries.ITEM;
		openOffline(tRegistry);
		MaterialPrefixItem rItem = aCreator.get();
		net.minecraft.core.Registry.register(tRegistry, new ResourceLocation("gt6", aProbeId), rItem);
		return rItem;
	}

	static Item probePlain(String aProbeId) {
		net.minecraft.core.Registry<Item> tRegistry = BuiltInRegistries.ITEM;
		openOffline(tRegistry);
		Item rItem = new Item(new Item.Properties());
		net.minecraft.core.Registry.register(tRegistry, new ResourceLocation("gt6", aProbeId), rItem);
		return rItem;
	}

	/** The registry open, best-effort PER FACE (the TileEntitySmelteryOfflineTest posture). */
	@SuppressWarnings("unchecked")
	static void openOffline(net.minecraft.core.Registry<?> aRegistry) {
		net.minecraft.core.Registry<Object> tRegistry = (net.minecraft.core.Registry<Object>)aRegistry;
		try {
			Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline registry", aE);
		}
		try {
			Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
		} catch (NoSuchFieldException | NoSuchMethodException ignored) {
			// the 21.1 face: no forge delegate behind the vanilla registry
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline forge registry", aE);
		}
		try {
			Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (NoSuchFieldException ignored) {
			// the 21.1 face: nothing but the vanilla frozen flag to unlock
		} catch (Exception aE) {
			throw new IllegalStateException("could not clear the offline registry lock", aE);
		}
	}

	private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
			try {
				Field rField = tClass.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {}
		}
		throw new NoSuchFieldException(aName);
	}

	// -------------------------------------------------------------------------
	// feed ↔ central-face consistency (the MS-3 seam)
	// -------------------------------------------------------------------------

	/** The provider arm: the ladder emits exactly what OM.anydata answers, scaled once by the count. */
	@Test
	public void feedMatchesTheCentralFaceOnPrefixItems() {
		ItemStack tStack = new ItemStack(DUST_IRON, 3);
		OreDictItemData tData = OM.anydata(tStack);
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(tStack);
		assertNotNull(tData);
		assertNotNull(tFeed);
		assertEquals(1, tFeed.size());
		assertSame(tData.mMaterial.mMaterial, tFeed.get(0).mMaterial, "the melted material IS the central face's answer");
		assertEquals(tData.mMaterial.mAmount * tStack.getCount(), tFeed.get(0).mAmount, "the prefix amount per item, whole slot");
	}

	/** The map arm: a plain (non-prefix) data-carrying item feeds — the old {@code instanceof}
	 * gate fizzed exactly this stack. The generic emission carries main + byproducts (the
	 * upstream :213-216/:229-232 form the old ladder dropped), and the count scale stays out
	 * of the stored data (the clone discipline). */
	@Test
	public void mapArmItemsFeedThroughTheCentralFace() {
		ItemStack tProbe = new ItemStack(PLAIN_ITEM);
		assertTrue(OM.setItemData(tProbe, new OreDictItemData(new OreDictMaterialStack(MT.Copper, CS.U), new OreDictMaterialStack(MT.Au, CS.U9))));
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(tProbe);
		assertNotNull(tFeed, "a data-carrying non-prefix item feeds through the map arm");
		assertEquals(2, tFeed.size(), "main + byproduct — the upstream generic emission");
		assertSame(MT.Copper, tFeed.get(0).mMaterial);
		assertEquals(CS.U, tFeed.get(0).mAmount);
		assertSame(MT.Au, tFeed.get(1).mMaterial);
		assertEquals(CS.U9, tFeed.get(1).mAmount);
		// whole-slot scaling never mutates the stored data
		List<OreDictMaterialStack> tFeed3 = CrucibleFeed.feedStacks(new ItemStack(PLAIN_ITEM, 3));
		assertEquals(CS.U * 3, tFeed3.get(0).mAmount, "three items = threefold (whole-slot melt)");
		assertEquals(CS.U9 * 3, tFeed3.get(1).mAmount, "the byproduct scales with the slot too");
		assertEquals(CS.U, OM.data(tProbe).mMaterial.mAmount, "the stored map data keeps its per-item amount (cloned, not mutated)");
	}

	/** The trash+fizz arm: no read-chain data and no bridge entry → null (:210-212). */
	@Test
	public void unknownStackAnswersNull() {
		assertNull(OM.anydata(new ItemStack(Items.STICK)));
		assertNull(CrucibleFeed.feedStacks(new ItemStack(Items.STICK)));
	}

	// -------------------------------------------------------------------------
	// the prefix arms over the READ DATA (upstream :217-228)
	// -------------------------------------------------------------------------

	/** oreRaw identity arm and the STANDARD_ORE contains-arm: the direct-smelt projection ×1. */
	@Test
	public void rawOreAndStandardOreArmsFeedOreDirect() {
		assertOreArm(new ItemStack(RAW_IRON), 1, "oreRaw (:217-218)");
		assertOreArm(new ItemStack(ORE_IRON), 1, "STANDARD_ORE (:225-226 — OP.ore carries the tag)");
	}

	/** blockRaw ×9 (:219-220) and DENSE_ORE ×2 (:227-228). */
	@Test
	public void blockRawAndDenseOreArmsScaleTheProjection() {
		assertOreArm(new ItemStack(BLOCKRAW_IRON), 9, "blockRaw (:219-220)");
		assertOreArm(new ItemStack(DENSE_IRON), 2, "DENSE_ORE (:227-228)");
	}

	/** The whole-slot count scaling is uniform over the ORE arms too — the r4-20b ladder lost
	 * the count of a hopper-pushed ore stack (one ore's worth out of a cleared slot). */
	@Test
	public void oreArmsScaleWithTheWholeSlot() {
		ItemStack tFour = new ItemStack(ORE_IRON, 4);
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(tFour);
		assertEquals(1, tFeed.size());
		assertSame(MT.Fe.mTargetCrushing.mMaterial, tFeed.get(0).mMaterial);
		assertEquals(CruciblePhysics.oreDirect(MT.Fe, 1).mAmount * 4, tFeed.get(0).mAmount, "four ores = fourfold projection (whole-slot melt)");
	}

	static void assertOreArm(ItemStack aStack, long aFormFactor, String aArm) {
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(aStack);
		assertNotNull(tFeed, aArm + " must feed");
		assertEquals(1, tFeed.size(), aArm);
		assertSame(MT.Fe.mTargetCrushing.mMaterial, tFeed.get(0).mMaterial, aArm + " — the crushing target, not the refined metal");
		assertEquals(CruciblePhysics.oreDirect(MT.Fe, aFormFactor).mAmount, tFeed.get(0).mAmount, aArm + " — the projection amount");
	}

	// -------------------------------------------------------------------------
	// the vanilla faces (the deleted bridges' equivalence + the surviving fallback)
	// -------------------------------------------------------------------------

	/** The deleted ingot bridge's equivalence: the family-tag arm answers ingot data → the
	 * generic arm at the prefix amount per item (upstream fed the same map from the
	 * oredict registration stream; the port feeds it from the tag tree). */
	@Test
	public void vanillaIngotRidesTheFamilyTagArm() {
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(new ItemStack(Items.IRON_INGOT, 2));
		assertNotNull(tFeed, "the vanilla iron ingot feeds through the read chain");
		assertEquals(1, tFeed.size());
		assertSame(MT.Fe, tFeed.get(0).mMaterial);
		assertEquals(2 * OP.ingot.mAmount, tFeed.get(0).mAmount, "the prefix amount per item — the old bridge equivalence");
	}

	/** The nugget face: nine nuggets = one unit (the old bridge equivalence at OP.nugget). */
	@Test
	public void vanillaNuggetRidesTheFamilyTagArm() {
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(new ItemStack(Items.GOLD_NUGGET, 9));
		assertNotNull(tFeed);
		assertEquals(1, tFeed.size());
		assertSame(MT.Au, tFeed.get(0).mMaterial);
		assertEquals(9 * OP.nugget.mAmount, tFeed.get(0).mAmount);
	}

	/** The surviving vanilla-ORE fallback bridge: the family-tag arm has no ores family yet,
	 * so the read chain nulls and the declared backup answers (the class-doc upgrade path). */
	@Test
	public void vanillaOreRidesTheFallbackBridge() {
		assertNull(OM.anydata(new ItemStack(Items.IRON_ORE)), "no ores family in the tag arm yet — the bridge is the answer");
		List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(new ItemStack(Items.IRON_ORE, 2));
		assertNotNull(tFeed, "the vanilla iron ore feeds via the fallback bridge");
		assertEquals(1, tFeed.size());
		assertSame(MT.Fe.mTargetCrushing.mMaterial, tFeed.get(0).mMaterial);
		assertEquals(CruciblePhysics.oreDirect(MT.Fe, 1).mAmount * 2, tFeed.get(0).mAmount, "two ore items, whole-slot");
	}

	// -------------------------------------------------------------------------
	// the alloy-page row ↔ feed result consistency (CRUCIBLE_ALLOYING)
	// -------------------------------------------------------------------------

	/** The page↔feed closure: the CRUCIBLE_ALLOYING dust row's own inputs melt into exactly
	 * the row's component amounts, and the components sum to the row's output (Electrum =
	 * Ag 1U + Au 1U → commonDivider units of alloy — whatever the page says to put in, the
	 * crucible's feed face turns into exactly what the page promises out). */
	@Test
	public void alloyPageDustRowInputsFeedExactlyTheRowComposition() {
		List<Recipe> tRows = GT6RecipeMapCrucible.alloyingDisplayRows(MT.Electrum);
		assertFalse(tRows.isEmpty(), "Electrum builds display rows offline (the GT6RecipeMapCrucibleTest posture)");
		Recipe tDustRow = tRows.get(0); // the dust pair is the first row (:478 the dust row precedes the ingot row)
		List<OreDictMaterialStack> tComponents = MT.Electrum.mComponents.getUndividedComponents();
		assertEquals(tComponents.size(), tDustRow.mInputs.length, "one input per component");
		long tFedTotal = 0;
		for (int i = 0; i < tDustRow.mInputs.length; i++) {
			ItemStack tInput = tDustRow.mInputs[i];
			List<OreDictMaterialStack> tFeed = CrucibleFeed.feedStacks(tInput);
			assertNotNull(tFeed, "every alloy-page input feeds");
			assertEquals(1, tFeed.size(), "a component dust feeds exactly its own material");
			assertSame(tComponents.get(i).mMaterial, tFeed.get(0).mMaterial, "row input order == component order");
			assertEquals(tComponents.get(i).mAmount, tFeed.get(0).mAmount, "the page row input melts as exactly the component amount");
			tFedTotal += tFeed.get(0).mAmount;
		}
		assertEquals(tDustRow.mOutputs[0].getCount() * CS.U, tFedTotal, "the fed components sum to exactly the row's alloy output");
	}
}
