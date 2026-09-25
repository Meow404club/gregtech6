/**
 * The structural coverage guard (task p38-c5-asset-coverage-guard, the C5 card of the
 * p38-render-gap-census split): EVERY block of the registration universe must have a
 * generated blockstate JSON, and EVERY registered item must have a generated item model
 * JSON. The census (state tasks.p38-render-gap-census) pinned the shared leak channel of
 * the three render issues — nothing gated "registered x generated", so the electric/flux
 * dynamo rows (GT6ElectricDynamos / GT6FluxDynamos) and the clay_bowl item shipped with
 * zero assets: placed blocks render the missing-model checkerboard, held items render
 * magenta. The datagen providers (GT6BlockStates / GT6ItemModels) are the PRODUCTION
 * side; this test is the CONSUMER side — it derives its universe from the REGISTRATION
 * classes alone and checks the committed generated tree on disk, so a provider segment
 * forgotten for a new registration row breaks this test instead of a player's screen.
 *
 * <p>The universe has two channels, both offline-safe (no registry event, no block
 * construction, nothing bound):</p>
 * <ol>
 * <li>{@code DeferredRegister} entries — the ~60 registration container classes each hold
 *     static {@code DeferredRegister<Block>/<Item>} fields; the entries map is populated
 *     by the class init (the register() calls fire before any registry event), and each
 *     entry key exposes {@code getId()} on both legs (forge RegistryObject.java:287 / neo
 *     DeferredHolder.getId — the CreativeTabJoinCensusTest reflection discipline, no
 *     import seam);</li>
 * <li>the {@code RegisterEvent} families whose registration is a raw event walk — each
 *     exposes its own offline-safe enumeration (the "census walk" methods, each the
 *     single definition site of its id scheme): GTMaterialItems / GTMaterialBlocks /
 *     GT6OreBlocks / GT6BedrockOreBlocks / GT6NetherOres / GTStoneBlocks.</li>
 * </ol>
 *
 * <p>Exemptions are EXPLICIT and each carries its reason:</p>
 * <ul>
 * <li>{@code GTFluids.BLOCKS} — every entry is a {@code LiquidBlock}; fluid-rendered
 *     blocks have no blockstate JSON by design (the fluid client renders them); the
 *     spring-lake worldgen bodies ride the same face.</li>
 * <li>{@link #KNOWN_GAP_BLOCK_ASSETS} / {@link #KNOWN_GAP_ITEM_ASSETS} — the booking
 *     point for live gaps: an entry is added only with a booked fix card, subtracted from
 *     the existence walks AND pinned still-missing by
 *     {@link #declaredKnownGapsAreStillMissing()}, so the waiver cannot outlive the gap:
 *     the moment the fix card lands the asset, that pin goes red and forces the list to
 *     shrink in the same change. No silent waivers. Currently EMPTY — the C1 card
 *     (p38-c1-dynamo-bowl-models) landed the dynamo blockstates/models and the clay_bowl
 *     item model, healing the census bookings.</li>
 * <li>{@link #GUARD_CAUGHT_UNBOOKED_ITEM_ASSETS} — the booking point for models this
 *     guard's own walks catch beyond the census list. The six first-walk catches (zpm,
 *     faucet_ceramic_raw, plow, branch_cutter, sense, hand_drill) were healed by the C1
 *     append. Same still-missing pin — the entry dies the moment its model lands.</li>
 * </ul>
 *
 * <p>The container-class list is hand-maintained like the CreativeTabJoinCensusTest
 * family list, BUT {@link #everyBlockItemContainerClassIsListed()} classpath-scans the
 * registration-bearing packages and fails when a class holding a Block/Item
 * {@code DeferredRegister} escapes the list — a new registration file joins the guard on
 * its first run, not after its first missing-asset bug.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.GT6Mod;
import gregtech6.covers.GT6Covers;
import gregtech6.fluid.GTFluids;
import gregtech6.item.GT6Circuits;
import gregtech6.item.GT6LubricantBucket;
import gregtech6.items.GT6LaserGas;
import gregtech6.items.GT6UsbSticks;
import gregtech6.items.bees.GT6Bumbles;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GT6BeeHives;
import gregtech6.registry.GT6Boilers;
import gregtech6.registry.GT6Books;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6CrystalChargers;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6ElectricDynamos;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.registry.GT6FeBatteries;
import gregtech6.registry.GT6FeConverters;
import gregtech6.registry.GT6FluxDynamos;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.registry.GT6FoamSprays;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6Lasers;
import gregtech6.registry.GT6Logistics;
import gregtech6.registry.GT6LongDistanceTransformers;
import gregtech6.registry.GT6LongDistPipes;
import gregtech6.registry.GT6LongDistWires;
import gregtech6.registry.GT6MagicAbsorbers;
import gregtech6.registry.GT6Molds;
import gregtech6.registry.GT6NetherOres;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6Placeables;
import gregtech6.registry.GT6Portals;
import gregtech6.registry.GT6QuantumEnergizers;
import gregtech6.registry.GT6Rails;
import gregtech6.registry.GT6Sensors;
import gregtech6.registry.GT6SlicerBlades;
import gregtech6.registry.GT6SprayCans;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GT6ZpmDechargers;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTEnergySources;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWires;
import gregtech6.registry.GTItemPipes;

public class GT6AssetCoverageGuardTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; the registries are usable by
			// now (the GTNoOcclusionCensusTest bracket).
		}
		// The channel-2 walks dereference the material axis (GT6OreBlocks.materialAxis /
		// GTMaterialBlocks.enumerate) — the census-test insurance init.
		GTMaterialItems.initMaterials();
	}

	// -------------------------------------------------------------------------
	// the declared exemptions
	// -------------------------------------------------------------------------

	/**
	 * The known blockstate gaps. Each entry is asserted STILL MISSING by
	 * {@link #declaredKnownGapsAreStillMissing()} — when the fix card lands the asset the
	 * pin goes red and the entry must be deleted in the same change. Currently EMPTY:
	 * the C1 card (p38-c1-dynamo-bowl-models) landed the ten dynamo blockstates the census
	 * booked and the entries were retired at that merge. Book new gaps back into this
	 * set, never waive silently.
	 */
	private static final Set<String> KNOWN_GAP_BLOCK_ASSETS = Set.of();

	/**
	 * The known item-model gaps. Same still-missing pin as the blockstate list. Currently
	 * EMPTY: the C1 card landed the ten dynamo BlockItem models and the clay_bowl item
	 * model (the census P1 third entry), retiring every entry at that merge.
	 */
	private static final Set<String> KNOWN_GAP_ITEM_ASSETS = Set.of();

	/**
	 * The gaps THIS GUARD caught on its first walk (2026-09-24, forge leg) — BEYOND the
	 * census list, each a plain registered Item with zero generated model (held = magenta):
	 * the ZPM battery row (GT6Batteries.ZPM_ITEM), the raw ceramic faucet
	 * (GT6Molds.FAUCET_CERAMIC_RAW), and the four field tools (GT6Tools PLOW/BRANCH_CUTTER/
	 * SENSE/HAND_DRILL). All six were fixed by the C1 append (p38-c1-dynamo-bowl-models)
	 * and the entries retired at that merge — the set stays as the booking point for the
	 * next unbooked catch.
	 */
	private static final Set<String> GUARD_CAUGHT_UNBOOKED_ITEM_ASSETS = Set.of();

	// -------------------------------------------------------------------------
	// the tests
	// -------------------------------------------------------------------------

	/** Every registered block (minus the declared exemptions) has a blockstate JSON on disk. */
	@Test
	public void everyRegisteredBlockHasABlockstate() throws Exception {
		Set<String> tUniverse = blockUniverse();
		Set<String> tOnDisk = blockstateIdsOnDisk();
		List<String> tMissing = new ArrayList<>();
		for (String tId : tUniverse) {
			if (KNOWN_GAP_BLOCK_ASSETS.contains(tId)) continue; // declared, pinned still-missing below
			if (!tOnDisk.contains(tId)) tMissing.add(tId);
		}
		assertTrue(tUniverse.size() >= 9000, "block universe implausibly small: " + tUniverse.size()
				+ " — the universe walk broke, this must never pass vacuously");
		assertTrue(tMissing.isEmpty(), missingReport("blockstate", tMissing));
	}

	/** Every registered item (minus the declared exemptions) has an item model JSON on disk. */
	@Test
	public void everyRegisteredItemHasAnItemModel() throws Exception {
		Set<String> tUniverse = itemUniverse();
		Set<String> tOnDisk = itemModelIdsOnDisk();
		List<String> tMissing = new ArrayList<>();
		for (String tId : tUniverse) {
			if (KNOWN_GAP_ITEM_ASSETS.contains(tId)
					|| GUARD_CAUGHT_UNBOOKED_ITEM_ASSETS.contains(tId)) continue; // declared, pinned still-missing below
			if (!tOnDisk.contains(tId)) tMissing.add(tId);
		}
		assertTrue(tUniverse.size() >= 60000, "item universe implausibly small: " + tUniverse.size()
				+ " — the universe walk broke, this must never pass vacuously");
		assertTrue(tMissing.isEmpty(), missingReport("item model", tMissing));
	}

	/**
	 * The anti-rot half of the known-gap waiver: every declared gap must STILL be missing.
	 * The moment the C1 card generates the asset, this test goes red and forces the
	 * exemption list to shrink in the same change.
	 */
	@Test
	public void declaredKnownGapsAreStillMissing() throws Exception {
		Set<String> tOnDiskStates = blockstateIdsOnDisk();
		Set<String> tOnDiskModels = itemModelIdsOnDisk();
		List<String> tHealed = new ArrayList<>();
		for (String tId : KNOWN_GAP_BLOCK_ASSETS) {
			if (tOnDiskStates.contains(tId)) tHealed.add("blockstate " + tId);
		}
		for (String tId : KNOWN_GAP_ITEM_ASSETS) {
			if (tOnDiskModels.contains(tId)) tHealed.add("item model " + tId);
		}
		for (String tId : GUARD_CAUGHT_UNBOOKED_ITEM_ASSETS) {
			if (tOnDiskModels.contains(tId)) tHealed.add("item model " + tId
					+ " (guard-caught set) — the fix card landed the model, delete the entry");
		}
		assertTrue(tHealed.isEmpty(), "C1 landed the asset(s) — delete the known-gap entries from "
				+ "GT6AssetCoverageGuardTest so the coverage walks re-claim them: " + tHealed);
	}

	/**
	 * The list-drift nail: every class in the registration-bearing packages that holds a
	 * static Block/Item {@code DeferredRegister} must appear in
	 * {@link #REGISTRY_CONTAINER_CLASSES} or be the declared GTFluids exemption. A new
	 * registration file therefore cannot silently leave the guard's universe.
	 */
	@Test
	public void everyBlockItemContainerClassIsListed() throws Exception {
		Set<String> tListed = new HashSet<>();
		for (Class<?> tClass : REGISTRY_CONTAINER_CLASSES) tListed.add(tClass.getName());
		tListed.add(GTFluids.class.getName()); // the declared LiquidBlock exemption
		List<String> tEscapees = new ArrayList<>();
		for (Class<?> tClass : scanRegistrationPackageClasses()) {
			if (tListed.contains(tClass.getName())) continue;
			if (holdsBlockOrItemRegister(tClass)) tEscapees.add(tClass.getName());
		}
		assertTrue(tEscapees.isEmpty(), "registration class(es) holding Block/Item DeferredRegister "
				+ "fields are missing from REGISTRY_CONTAINER_CLASSES — join them into the coverage guard: "
				+ tEscapees);
	}

	// -------------------------------------------------------------------------
	// the universe
	// -------------------------------------------------------------------------

	/**
	 * The registration container classes: every class holding a static
	 * {@code DeferredRegister<Block>} or {@code DeferredRegister<Item>} field (walked
	 * 2026-09-24 over gregtech6.registry + covers + fluid + item + items;
	 * {@link #everyBlockItemContainerClassIsListed()} re-walks the packages each run).
	 * Menu/feature/component registers are out of scope by generic type.
	 */
	private static final List<Class<?>> REGISTRY_CONTAINER_CLASSES = List.of(
			GT6Anvils.class, GT6Attachments.class, GT6Batteries.class, GT6BeeCombs.class,
			GT6BeeHives.class, GT6Boilers.class, GT6Books.class, GT6BurningBoxes.class, GT6Crucibles.class,
			GT6CrystalChargers.class, GT6Distillation.class, GT6DynamoHousings.class,
			GT6ElectricDynamos.class, GT6ElectricTransformers.class, GT6ExtruderMolds.class,
			GT6FeBatteries.class, GT6FeConverters.class, GT6FluxDynamos.class, GT6FoamBlocks.class,
			GT6FoamSprays.class, GT6FoodCans.class, GTGrassBlocks.class, GT6HeatExchangers.class,
			GT6Hoppers.class, GT6Kinetics.class, GT6Kitchen.class, GT6LargeMachines.class,
			GT6Lasers.class, GT6Logistics.class, GT6LongDistanceTransformers.class,
			GT6LongDistPipes.class, GT6LongDistWires.class, GT6MagicAbsorbers.class, GT6Molds.class,
			GT6Placeables.class, GT6Portals.class, GT6QuantumEnergizers.class, GT6Rails.class,
			GT6Sensors.class, GT6SlicerBlades.class, GT6SprayCans.class, GT6StaticStorages.class,
			GT6SurfaceBlocks.class, GT6Tanks.class, GT6Tools.class, GT6TreeBlocks.class,
			GT6Turbines.class, GT6ZpmDechargers.class, GTEnergySources.class, GTBarrels.class, GTBlockEntities.class,
			GTMachines.class, GTMultiBlocks.class, GTWires.class, GTFluidPipes.class,
			GTItemPipes.class, GT6Covers.class, GT6Circuits.class, GT6LubricantBucket.class,
			GT6LaserGas.class, GT6UsbSticks.class, GT6Bumbles.class, gregtech6.items.GT6Keys.class);

	/** Channel 1 + channel 2 block ids ("ns:path"). */
	private static Set<String> blockUniverse() throws Exception {
		Set<String> rIds = new LinkedHashSet<>();
		collectDeferredRegisters(rIds, null);
		collectRegisterEventFamilies(rIds, null);
		return rIds;
	}

	/** Channel 1 + channel 2 item ids ("ns:path"). */
	private static Set<String> itemUniverse() throws Exception {
		Set<String> rIds = new LinkedHashSet<>();
		collectDeferredRegisters(null, rIds);
		collectRegisterEventFamilies(null, rIds);
		return rIds;
	}

	/**
	 * Channel 1: the DeferredRegister entries of every listed container class. The entry
	 * holder type is matched BY NAME (forge = RegistryObject, neo = DeferredHolder) and
	 * {@code getId()} is invoked reflectively — same signature both legs. The field read
	 * triggers the class init that fills the entries map; the SUPPLIERS stay uninvoked
	 * (no block/item construction, nothing touches the frozen registries).
	 */
	private static void collectDeferredRegisters(Set<String> aBlocks, Set<String> aItems) throws Exception {
		for (Class<?> tContainer : REGISTRY_CONTAINER_CLASSES) {
			for (Field tField : tContainer.getDeclaredFields()) {
				if (!Modifier.isStatic(tField.getModifiers())) continue;
				if (!(tField.getGenericType() instanceof ParameterizedType tGeneric)
						|| !tField.getType().getName().endsWith(".DeferredRegister")) continue;
				if (!(tGeneric.getActualTypeArguments()[tGeneric.getActualTypeArguments().length - 1]
						instanceof Class<?> tElement)) continue; // BlockEntityType<?> is itself parameterized
				boolean tIsBlock = tElement == Block.class;
				boolean tIsItem = tElement == Item.class;
				if (!tIsBlock && !tIsItem) continue; // tabs / BETs / menus / fluids are other registries
				if (tContainer == GTFluids.class && tIsBlock) {
					// DECLARED EXEMPTION: every GTFluids BLOCKS entry is a LiquidBlock — the
					// fluid-rendered face has no blockstate JSON by design. The container is
					// skipped whole; a non-fluid block there must move out, not waives in.
					continue;
				}
				Object tRegister = tField.get(null); // initializes the container class
				// forge: Collection<RegistryObject<T>> (the entriesView key set) / neo:
				// Collection<DeferredHolder<T,T>> — both expose getId() on each element.
				@SuppressWarnings("unchecked")
				Iterable<Object> tEntries =
						(Iterable<Object>) tRegister.getClass().getMethod("getEntries").invoke(tRegister);
				for (Object tKey : tEntries) {
					ResourceLocation tId = (ResourceLocation) tKey.getClass().getMethod("getId").invoke(tKey);
					if (tIsBlock && aBlocks != null) aBlocks.add(tId.getNamespace() + ":" + tId.getPath());
					if (tIsItem && aItems != null) aItems.add(tId.getNamespace() + ":" + tId.getPath());
				}
			}
		}
	}

	/**
	 * Channel 2: the raw-RegisterEvent families, each over its own offline-safe census
	 * walk (the single definition site of its id scheme — the guard reuses them verbatim,
	 * so an id-scheme drift cannot fork). Blocks and items share the id path wherever the
	 * family registers a BlockItem under the block's path.
	 */
	private static void collectRegisterEventFamilies(Set<String> aBlocks, Set<String> aItems) {
		// the material item flood (the 468-prefix walk) — item face
		if (aItems != null) {
			for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
				aItems.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
			}
		}
		// the storage-block families — block face + the BlockItem under the same path
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			String tPath = "gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material());
			if (aBlocks != null) aBlocks.add(tPath);
			if (aItems != null) aItems.add(tPath);
		}
		// the ore universe (normal/broken/small over the family x material axis)
		for (GT6OreBlocks.OreKey tKey : GT6OreBlocks.registrationOrder()) {
			String tPath = "gt6:" + GT6OreBlocks.path(tKey);
			if (aBlocks != null) aBlocks.add(tPath);
			if (aItems != null) aItems.add(tPath);
		}
		// the bedrock ore pairs (large/small)
		for (GT6BedrockOreBlocks.BedrockKey tKey : GT6BedrockOreBlocks.registrationOrder()) {
			String tPath = "gt6:" + GT6BedrockOreBlocks.path(tKey.small(), tKey.material());
			if (aBlocks != null) aBlocks.add(tPath);
			if (aItems != null) aItems.add(tPath);
		}
		// the nether ore band — MINIMAL CARRIERS: no BlockItem (the class doc's declared defer)
		if (aBlocks != null) {
			for (GT6NetherOres.NetherOreKey tKey : GT6NetherOres.KEYS) {
				aBlocks.add("gt6:" + tKey.path());
			}
		}
		// the stone universe (17 stones x 16 variants)
		for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
			String tPath = "gt6:" + GTStoneBlocks.path(tKey.stone().snake(), tKey.variant());
			if (aBlocks != null) aBlocks.add(tPath);
			if (aItems != null) aItems.add(tPath);
		}
	}

	// -------------------------------------------------------------------------
	// the generated-tree face
	// -------------------------------------------------------------------------

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** The committed blockstate ids ("ns:path") over the static ∪ generated trees. */
	private static Set<String> blockstateIdsOnDisk() throws IOException {
		Set<String> rIds = new HashSet<>();
		for (String tTree : new String[] {"src/main/resources", "src/generated/resources"}) {
			Path tAssets = mdkRoot().resolve(tTree).resolve("assets");
			if (!Files.isDirectory(tAssets)) continue;
			try (Stream<Path> tWalk = Files.walk(tAssets, 3)) {
				tWalk.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
					Path tParent = p.getParent();
					if (tParent == null || !tParent.getFileName().toString().equals("blockstates")) return;
					Path tNs = tParent.getParent();
					String tName = p.getFileName().toString();
					rIds.add(tNs.getFileName().toString() + ":"
							+ tName.substring(0, tName.length() - ".json".length()));
				});
			}
		}
		assertTrue(!rIds.isEmpty(), "no blockstates found on disk — the tree walk broke");
		return rIds;
	}

	/** The committed item model ids ("ns:path") over the static ∪ generated trees. */
	private static Set<String> itemModelIdsOnDisk() throws IOException {
		Set<String> rIds = new HashSet<>();
		for (String tTree : new String[] {"src/main/resources", "src/generated/resources"}) {
			Path tAssets = mdkRoot().resolve(tTree).resolve("assets");
			if (!Files.isDirectory(tAssets)) continue;
			try (Stream<Path> tWalk = Files.walk(tAssets)) {
				tWalk.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
					// .../assets/<ns>/models/item/<path>.json
					Path tItem = p.getParent();
					if (tItem == null || !tItem.getFileName().toString().equals("item")) return;
					Path tModels = tItem.getParent();
					if (tModels == null || !tModels.getFileName().toString().equals("models")) return;
					Path tNs = tModels.getParent();
					String tRel = tModels.relativize(p).toString().replace('\\', '/');
					rIds.add(tNs.getFileName().toString() + ":"
							+ tRel.substring("item/".length(), tRel.length() - ".json".length()));
				});
			}
		}
		assertTrue(!rIds.isEmpty(), "no item models found on disk — the tree walk broke");
		return rIds;
	}

	/** Failure message for a missing list, capped to keep the report readable. */
	private static String missingReport(String aWhat, List<String> aMissing) {
		StringBuilder r = new StringBuilder("registered without a generated " + aWhat + ": "
				+ aMissing.size() + " — add the datagen segment, or (only with a booked fix card) "
				+ "declare a known-gap entry with its reason");
		for (String tId : aMissing.subList(0, Math.min(40, aMissing.size()))) {
			r.append("\n  - ").append(tId);
		}
		return r.toString();
	}

	// -------------------------------------------------------------------------
	// the container-class scanner (the list-drift nail)
	// -------------------------------------------------------------------------

	/** True when the class declares a static field whose type is a DeferredRegister of Block or Item. */
	private static boolean holdsBlockOrItemRegister(Class<?> aClass) {
		for (Field tField : aClass.getDeclaredFields()) {
			if (!Modifier.isStatic(tField.getModifiers())) continue;
			if (!(tField.getGenericType() instanceof ParameterizedType tGeneric)
					|| !tField.getType().getName().endsWith(".DeferredRegister")) continue;
			if (!(tGeneric.getActualTypeArguments()[tGeneric.getActualTypeArguments().length - 1]
					instanceof Class<?> tElement)) continue; // BlockEntityType<?> is itself parameterized
			if (tElement == Block.class || tElement == Item.class) return true;
		}
		return false;
	}

	/**
	 * Every top-level class under the registration-bearing packages (gregtech6.registry,
	 * .covers, .fluid, .item, .items) reachable from the main classpath root. Class.forName
	 * initializes them (the same init the universe walk triggers for the listed ones).
	 */
	private static List<Class<?>> scanRegistrationPackageClasses() throws Exception {
		Path tRoot = mainClassesRoot();
		List<Class<?>> rClasses = new ArrayList<>();
		for (String tPackage : new String[] {"registry", "covers", "fluid", "item", "items"}) {
			Path tDir = tRoot.resolve("gregtech6").resolve(tPackage);
			if (!Files.isDirectory(tDir)) continue;
			try (Stream<Path> tWalk = Files.walk(tDir)) {
				for (Path tFile : tWalk.filter(p -> p.getFileName().toString().endsWith(".class")).toList()) {
					if (tFile.getFileName().toString().contains("$")) continue; // nested classes ride their host
					String tRel = tRoot.relativize(tFile).toString().replace('\\', '/');
					rClasses.add(Class.forName(tRel.substring(0, tRel.length() - ".class".length()).replace('/', '.')));
				}
			}
		}
		assertTrue(!rClasses.isEmpty(), "the registration package scan found no classes — the "
				+ "classpath root broke, the list-drift nail must never pass vacuously");
		return rClasses;
	}

	private static Path mainClassesRoot() throws Exception {
		URI tLocation = GT6Mod.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		Path tPath = Path.of(tLocation);
		assertTrue(Files.isDirectory(tPath), "unexpected main classpath shape (not a directory): "
				+ tPath + " — the list-drift nail needs the classes dir");
		return tPath;
	}
}
