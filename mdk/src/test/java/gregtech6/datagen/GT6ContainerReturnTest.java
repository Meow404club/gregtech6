/**
 * The container-return pins (task material-prefix-container-return): {@link MaterialPrefixItem}
 * now replays the upstream PrefixItem.getContainerItem face (PrefixItem.java:160-165) — a
 * crafting that consumes a declared prefix's item hands back the container (chemtube → the
 * MT.Empty tube, Loader_Items.java:128), the container itself is consumed flat (:162).
 *
 * <p>The upstream prefix census is exactly three declarations — OP.bottle = the vanilla glass
 * bottle (OP.java:576), OP.cell = the IC2 empty cell (LoaderItemList.java:780), OP.chemtube =
 * Loader_Items.java:128 — and the port item path materializes ONLY chemtube, so the census pins
 * below assert the registration universe carries the chemtube(MT.Empty) pair and NO bottle/cell
 * pairs at all. The affected crafting face is the melt walk alone: every
 * {@code dust_tiny/from_chemtube} row (1068, the GT6CraftFromDatagenTest:812 ratchet) refunds,
 * while the {@code chemtube/from_dust_tiny} fill rows (1096) consume the EMPTY tube through the
 * same :162 cut upstream applies — no self-refund loop. Zero recipe-JSON involvement: the
 * refund rides the vanilla crafting-remainder channel (1.20.1 ResultSlot.java:59-82 /
 * 1.21.1 :61-93 both walk Recipe.getRemainingItems at take time), so the datagen tree stays
 * byte-identical.
 *
 * <p>Two resolution arms (the GT6DatagenWalkLegTest posture): the registry-live JVM (the neo
 * junit-fml boot) answers through the REAL registration INDEX; the offline JVM pins the same
 * semantics through fixtures plus the {@code GTMaterialItems.sLookup} seam stub (restore in
 * afterEach, always). The remainder methods live on the family class, constant-true gate —
 * the dead-gate reflection probe mirrors MachineFaceFourTest:141.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GT6ContainerReturnTest extends GTOfflineTestBase {

	private static final GT6CraftingRecipes RECIPES = new GT6CraftingRecipes(
			new net.minecraft.data.PackOutput(java.nio.file.Path.of("build", "test-container-return")),
			java.util.concurrent.CompletableFuture.completedFuture(null));

	/** Offline-JVM fixtures (the GTMaterialItemsRegistrationTest posture); null on the registry-live leg. */
	static MaterialPrefixItem sTubeEmpty;
	static MaterialPrefixItem sTubeFilled;
	static MaterialPrefixItem sIngotIron;

	/** True when this JVM actually registered the GT6 content (the neo junit-fml leg). */
	private static boolean registryLive() {
		return !GTMaterialItems.items().isEmpty();
	}

	@BeforeAll
	static void buildFixtures() {
		GTMaterialItems.initMaterials();
		if (registryLive()) return; // the live leg answers through the real INDEX, no fixtures
		sTubeEmpty = registerItemFixture("fixture_container_chemtube_empty",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.chemtube, MT.Empty));
		sTubeFilled = registerItemFixture("fixture_container_chemtube_filled",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.chemtube, MT.Fe));
		sIngotIron = registerItemFixture("fixture_container_ingot_iron",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Fe));
	}

	@AfterEach
	void restoreSeam() {
		GT6ContainerLookup.restore();
	}

	/** The first registered non-Empty chemtube material (the melt walk's consuming shape). */
	private static GTMaterialItems.PrefixMaterial filledTubePair() {
		return GTMaterialItems.registrationOrder().stream()
				.filter(tPair -> tPair.prefix() == OP.chemtube && tPair.material() != MT.Empty)
				.findFirst().orElseThrow();
	}

	/**
	 * THE behavior pin: crafting with a filled tube hands back the empty tube
	 * (Loader_Items.java:128). Registry-live arm: the real INDEX answers stackOf;
	 * offline arm: the fixture pair + the sLookup stub (installed before the call,
	 * restored in afterEach).
	 */
	@Test
	public void theMeltFaceRefundsTheEmptyTube() {
		Item tFilledItem;
		Item tEmptyItem;
		if (registryLive()) {
			GTMaterialItems.PrefixMaterial tPair = filledTubePair();
			tFilledItem = GTMaterialItems.get(tPair.prefix(), tPair.material()).get();
			tEmptyItem = GTMaterialItems.get(OP.chemtube, MT.Empty).get();
		} else {
			GT6ContainerLookup.stubEmptyTube(sTubeEmpty);
			tFilledItem = sTubeFilled;
			tEmptyItem = sTubeEmpty;
		}
		ItemStack tBack = tFilledItem.getCraftingRemainingItem(new ItemStack(tFilledItem));
		assertFalse(tBack.isEmpty(), "the melt face refunds (upstream PrefixItem.java:164)");
		assertEquals(tEmptyItem, tBack.getItem(), "the refund is the family's own MT.Empty tube (Loader_Items.java:128)");
		assertEquals(1, tBack.getCount(), "one tube back, PrefixItem.java:164 ST.amount(1, ...)");
		assertTrue(tFilledItem.hasCraftingRemainingItem(new ItemStack(tFilledItem)), "the gate is open on the refund path");
	}

	/** The :162 self-return cut — the empty tube consumed by the fill walk is gone, no loop. */
	@Test
	public void theEmptyTubeIsConsumedFlatNotRefundedForItself() {
		Item tEmptyItem = registryLive() ? GTMaterialItems.get(OP.chemtube, MT.Empty).get() : sTubeEmpty;
		assertTrue(tEmptyItem.getCraftingRemainingItem(new ItemStack(tEmptyItem)).isEmpty(),
				"PrefixItem.java:162 — the container item is never refunded for itself");
		assertTrue(tEmptyItem.hasCraftingRemainingItem(new ItemStack(tEmptyItem)), "the gate stays open (the GET face decides)");
	}

	/** Prefixes without an upstream declaration consume flat (the 109-prefix majority face). */
	@Test
	public void prefixesWithoutADeclarationConsumeFlat() {
		Item tIngotItem = registryLive() ? GTMaterialItems.get(OP.ingot, MT.Fe).get() : sIngotIron;
		assertTrue(tIngotItem.getCraftingRemainingItem(new ItemStack(tIngotItem)).isEmpty(),
				"no mContainerItem declaration on ingot (the upstream census: bottle/cell/chemtube only)");
	}

	/** The dead-gate reflection probe (MachineFaceFourTest:141 shape): both remainder methods are declared on the family, not inherited. */
	@Test
	public void theRemainderMethodsAreDeclaredOnTheFamilyClass() throws NoSuchMethodException {
		assertEquals(MaterialPrefixItem.class,
				MaterialPrefixItem.class.getMethod("hasCraftingRemainingItem", ItemStack.class).getDeclaringClass(),
				"an inherited constant-false would swallow the get override (id410/id413)");
		assertEquals(MaterialPrefixItem.class,
				MaterialPrefixItem.class.getMethod("getCraftingRemainingItem", ItemStack.class).getDeclaringClass(),
				"the refund face lives on the family");
	}

	/**
	 * The upstream prefix census vs the port universe: chemtube carries the MT.Empty pair,
	 * bottle/cell (the other two upstream declarations, OP.java:576 / LoaderItemList.java:780)
	 * land NO port prefix items — their mContainerItem rows have no port face.
	 */
	@Test
	public void thePrefixCensusMatchesTheUpstreamDeclarations() {
		assertTrue(GTMaterialItems.registrationOrder().stream().anyMatch(tPair -> tPair.prefix() == OP.chemtube && tPair.material() == MT.Empty),
				"the (chemtube, MT.Empty) pair is in the registration universe (the refund target)");
		assertFalse(GTMaterialItems.registrationOrder().stream().anyMatch(tPair -> tPair.prefix() == OP.bottle),
				"OP.bottle (OP.java:576 → vanilla glass_bottle) has no port prefix items");
		assertFalse(GTMaterialItems.registrationOrder().stream().anyMatch(tPair -> tPair.prefix() == OP.cell),
				"OP.cell (LoaderItemList.java:780 → the IC2 empty cell) has no port prefix items");
	}

	/**
	 * The census reconciliation: the affected crafting face is the melt walk EXACTLY —
	 * 1068 dust_tiny/from_chemtube rows consume a filled tube and refund; the 1096
	 * chemtube/from_dust_tiny fill rows consume the EMPTY tube (the :162 cut, unaffected).
	 * Both numbers are the GT6CraftFromDatagenTest ratchets (:811-812).
	 */
	@Test
	public void theAffectedCraftingFaceIsTheMeltWalkExactly() {
		long tMelt = 0, tFill = 0;
		for (GT6CraftingRecipes.ShapelessCraftFromMaterialRow tRow : GT6CraftingRecipes.shapelessCraftFromMaterialRows()) {
			if (tRow.aForm().aKey().equals("dust_tiny/from_chemtube")) tMelt++;
			if (tRow.aForm().aKey().equals("chemtube/from_dust_tiny")) tFill++;
		}
		assertEquals(1068, tMelt, "the affected rows: every melt row refunds through the new face");
		assertEquals(1096, tFill, "the fill rows stay flat (the self-return cut) — no recipe-JSON change either way");
		assertTrue(RECIPES != null, "the provider class resolves under the offline boot");
	}
}

/** The per-leg typed seam holder (the GT6MaterialItemsLookup shape): the stub answers ONLY the
 * (chemtube, MT.Empty) refund pair, with the offline fixture's own handle. */
//? if forge {
final class GT6ContainerLookup {
	private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, net.minecraftforge.registries.RegistryObject<Item>> sOriginal;

	static void stubEmptyTube(Item aEmptyTube) {
		sOriginal = GTMaterialItems.sLookup;
		net.minecraft.resources.ResourceLocation tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aEmptyTube);
		net.minecraftforge.registries.RegistryObject<Item> tHandle =
				net.minecraftforge.registries.RegistryObject.create(tKey, net.minecraft.core.registries.Registries.ITEM, tKey.getNamespace());
		GTMaterialItems.sLookup = (aPrefix, aMaterial) -> aPrefix == OP.chemtube && aMaterial == MT.Empty ? tHandle : null;
	}

	static void restore() {
		if (sOriginal != null) GTMaterialItems.sLookup = sOriginal;
		sOriginal = null;
	}
}
//?} else {
/*final class GT6ContainerLookup {
	private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, net.neoforged.neoforge.registries.DeferredHolder<Item, Item>> sOriginal;

	static void stubEmptyTube(Item aEmptyTube) {
		sOriginal = GTMaterialItems.sLookup;
		net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle =
				net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.core.registries.Registries.ITEM,
						net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aEmptyTube));
		GTMaterialItems.sLookup = (aPrefix, aMaterial) -> aPrefix == OP.chemtube && aMaterial == MT.Empty ? tHandle : null;
	}

	static void restore() {
		if (sOriginal != null) GTMaterialItems.sLookup = sOriginal;
		sOriginal = null;
	}
}
*///?}
