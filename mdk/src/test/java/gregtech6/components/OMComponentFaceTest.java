package gregtech6.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.item.MaterialPrefixItem;

/**
 * The central component face pins (task component-central-face, the six acceptance arms).
 *
 * <p>Offline posture: the vanilla registries bootstrap, the GT material universe refills
 * ({@code GTMaterialItems.initMaterials()}, the GT6RecipeTagFallbackTest convention), and the
 * reverse-tag seam {@link OM#sStackTags} runs under an injected membership stub — the production
 * binding is {@code ItemStack::getTags}, which reads EMPTY offline (the tag manager never boots;
 * the GT6RecipeTagFallbackTest.java:53 seam discipline, the same-value ruling
 * decisions.p25-tag-input-fallback-rulings ②). The stub returns the platform-family tag ids the
 * datagen emits, so the positive verdict is the same one a live registry answers.
 *
 * <p>OM state is static: every test writes through its own probe Item instances so the
 * (item, damage) keys never collide, and the tag seam is restored to the production binding
 * after every test.
 */
class OMComponentFaceTest {

	/** The recyclable notification recorder — it records the registered item only (the wave2
	 * recycling card listens for the container itself). */
	static final List<String> sNotifications = new ArrayList<>();
	static final IOreDictListenerRecyclable sRecorder = tEvent -> sNotifications.add(tEvent.mStack.getItem().toString());

	/** The tag stub: the iron ingot sits in ingots/iron, the copper ingot in ingots/copper,
	 * everything else in nothing. */
	static final Function<ItemStack, Stream<TagKey<Item>>> sTagStub = aStack ->
			aStack.getItem() == Items.IRON_INGOT ? Stream.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, "iron"))
			: aStack.getItem() == Items.COPPER_INGOT ? Stream.of(GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, "copper"))
			: Stream.empty();

	@BeforeAll
	static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap(); // the Forge-patched boot throws offline at NetworkHooks — the registries are ready by then
		} catch (Throwable ignored) {}
		gregtech6.registry.GTMaterialItems.initMaterials(); // MT.init + OP.init, the offline material universe
		OM.addListener(sRecorder);
	}

	@BeforeEach
	void armTheStub() {
		OM.sStackTags = sTagStub;
	}

	@AfterEach
	void restoreProductionBinding() {
		OM.sStackTags = ItemStack::getTags;
		sNotifications.clear();
	}

	@AfterAll
	static void leaveTheProductionBindingInPlace() {
		OM.sStackTags = ItemStack::getTags;
	}

	// ------------------------------------------------------------- ① the read-chain priorities

	/** The provider arm covers the map and only fires on override reads — upstream :694-699:
	 * a map-only read (OM.data, upstream OM.java:167-169) never consults it, an override read
	 * (OM.anydata :174-176) lets the item's self-description win. */
	@Test
	void providerArmBeatsTheMapOnlyOnOverrideReads() {
		// the registered probe (the GT6RecipeTagFallbackTest posture — the Forge intrusive
		// holder makes a bare `new MaterialPrefixItem` throw while the registry is frozen)
		MaterialPrefixItem tIngotIron = probeItem("omface_probe_ingot_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Iron));
		ItemStack tProbe = new ItemStack(tIngotIron);
		// the map disagrees with the provider on purpose (prefix-less Copper data)
		assertTrue(OM.setItemData(tProbe, new OreDictItemData(new OreDictMaterialStack(MT.Copper, CS.U))));
		// map-only read: the map entry wins (the provider arm never runs)
		assertEquals(MT.Copper, OM.data(tProbe).mMaterial.mMaterial);
		// override read: the provider's own (ingot, Iron) beats the map
		OreDictItemData tData = OM.anydata(tProbe);
		assertEquals(OP.ingot, tData.mPrefix);
		assertEquals(MT.Iron, tData.mMaterial.mMaterial);
	}

	/** The map declaration beats the vanilla family-tag arm — the tag arm is the modern stand-in
	 * for the 1.7.10 registration fill of the SAME map, so explicit map entries keep priority
	 * (the declared arm slot, the OM javadoc). And the gate parity pin: a tag-resolved item
	 * REFUSES re-declaration (:651-652 through the getAssociation_ filter :726-729) — upstream
	 * showed the same refusal for its registration-filled map entries. */
	@Test
	void mapArmBeatsTheVanillaTagArm() {
		// write under the production binding (offline tags read empty — the gate sees no
		// association, the upstream pre-registration write shape)
		OM.sStackTags = ItemStack::getTags;
		assertTrue(OM.setItemData(new ItemStack(Items.COPPER_INGOT), new OreDictItemData(new OreDictMaterialStack(MT.Gold, CS.U))));
		// arm the tag face: the map declaration still wins
		OM.sStackTags = sTagStub;
		assertEquals(MT.Gold, OM.anydata(new ItemStack(Items.COPPER_INGOT)).mMaterial.mMaterial);
		// a tag-resolved item refuses the overwrite — even under the Wood name (Iron is not Wood)
		assertFalse(OM.setItemData(new ItemStack(Items.IRON_INGOT), new OreDictItemData(new OreDictMaterialStack(MT.Gold, CS.U))));
		assertFalse(OM.setItemData(new ItemStack(Items.IRON_INGOT), new OreDictItemData(OP.plate, MT.Wood)));
	}

	// ------------------------------------------------------------- ⑤ the vanilla family-tag arm

	/** Acceptance ⑤: the vanilla iron ingot resolves (ingot, Iron) through the
	 * {@code <platform>:ingots/iron} intersection tag — the GT6RecipeTagFallbackTest.java:53
	 * precedent shape: production binding offline-red, injected stub green, gold stays out. */
	@Test
	void vanillaIronIngotResolvesIronThroughTheFamilyTag() {
		// red under the production binding (the tag manager never boots offline)
		OM.sStackTags = ItemStack::getTags;
		assertNull(OM.anydata(new ItemStack(Items.IRON_INGOT)));
		// green under the injected stub: the intersection face parses back to the pair
		OM.sStackTags = sTagStub;
		OreDictItemData tData = OM.anydata(new ItemStack(Items.IRON_INGOT));
		assertEquals(OP.ingot, tData.mPrefix);
		assertEquals(MT.Iron, tData.mMaterial.mMaterial); // the Fe face (MT.Iron = the port's iron material)
		// the negative: gold carries no family face under this stub
		assertNull(OM.anydata(new ItemStack(Items.GOLD_INGOT)));
		// the ungated face: the map-only data() read answers too (the arm replaces the upstream
		// registration fill, which was never override-gated)
		assertEquals(OP.ingot, OM.data(new ItemStack(Items.IRON_INGOT)).mPrefix);
	}

	// ------------------------------------------------------------- ① (cont.) the damage arm

	/** Acceptance ①: the damageable proportional arm (:702-708) rescales Main Material and
	 * Byproducts by the REMAINING durability fraction (125 of 250 = one half of U for an iron
	 * pickaxe), only under mUseVanillaDamage; the stored entry never mutates. */
	@Test
	void damageableArmRescalesByRemainingDurability() {
		OreDictItemData tWritten = new OreDictItemData(MT.Iron, CS.U, MT.Copper, CS.U).setUseVanillaDamage();
		assertTrue(OM.setItemData(new ItemStack(Items.IRON_PICKAXE), tWritten));
		// half-worn: 125 of 250 durability left → exactly half the amounts
		ItemStack tHalfWorn = new ItemStack(Items.IRON_PICKAXE);
		tHalfWorn.setDamageValue(125);
		OreDictItemData tData = OM.anydata(tHalfWorn);
		assertEquals(CS.U / 2, tData.mMaterial.mAmount);
		assertEquals(1, tData.mByProducts.length);
		assertEquals(CS.U / 2, tData.mByProducts[0].mAmount);
		// the stored entry stayed whole (the rescale is local, upstream :705-707)
		assertEquals(CS.U, tWritten.mMaterial.mAmount);
		// undamaged reads full amounts (the exact (item, 0) hit, upstream :698)
		assertEquals(CS.U, OM.anydata(new ItemStack(Items.IRON_PICKAXE)).mMaterial.mAmount);
		// a zero-key entry WITHOUT the flag returns unscaled (:703-708)
		assertTrue(OM.setItemData(new ItemStack(Items.IRON_SHOVEL), new OreDictItemData(MT.Iron, CS.U)));
		ItemStack tWornShovel = new ItemStack(Items.IRON_SHOVEL);
		tWornShovel.setDamageValue(125);
		assertEquals(CS.U, OM.anydata(tWornShovel).mMaterial.mAmount);
	}

	/** The wildcard-damage key (:700; CS.W = OreDictionary.WILDCARD_VALUE): data written at
	 * damage W answers every damage of the item — proven with GOLD data so no other test's
	 * zero-key entry could produce the verdict.
	 *
	 * <p>Dual-leg split (platform-declared, probed 2026-10-07): 1.20.1 stores the raw wildcard
	 * on any stack, so the write keys at W and reads at other damages hit the wildcard arm.
	 * The 1.21.1 component axis CLAMPS setDamageValue to maxDamage (32767 → 250 on the axe,
	 * → 0 on a non-damageable stick), so no real stack can carry W and the arm is unreachable
	 * there — the pin rides the clamp itself: the write lands at the clamped damage, and the
	 * wildcard-registration isomorph on this axis is the family-tag arm (pinned above). */
	@Test
	void wildcardDamageKeyAnswersEveryDamage() {
		//? if forge {
		ItemStack tWriter = new ItemStack(Items.IRON_AXE);
		tWriter.setDamageValue(OM.W);
		assertEquals(OM.W, tWriter.getDamageValue()); // the raw wildcard survives on this axis
		assertTrue(OM.setItemData(tWriter, new OreDictItemData(new OreDictMaterialStack(MT.Gold, CS.U))));
		assertEquals(MT.Gold, OM.data(new ItemStack(Items.IRON_AXE)).mMaterial.mMaterial);
		ItemStack tDamaged = new ItemStack(Items.IRON_AXE);
		tDamaged.setDamageValue(10);
		assertEquals(MT.Gold, OM.data(tDamaged).mMaterial.mMaterial);
		//?} else {
		/*ItemStack tWriter = new ItemStack(Items.IRON_AXE);
		tWriter.setDamageValue(OM.W);
		assertEquals(tWriter.getMaxDamage(), tWriter.getDamageValue()); // the clamp: 32767 → 250
		assertTrue(OM.setItemData(tWriter, new OreDictItemData(new OreDictMaterialStack(MT.Gold, CS.U))));
		assertEquals(MT.Gold, OM.data(tWriter).mMaterial.mMaterial); // the exact key at the clamped damage
		assertNull(OM.data(new ItemStack(Items.IRON_AXE))); // a zero-damage read misses both map probes
		 *///?}
	}

	// ------------------------------------------------------------- ② add/set semantics + the Wood exception

	/** Acceptance ②: add writes only into absence (:636-644); set refuses a prefixed non-Wood
	 * overwrite and grants the Wood exception (:651-652 — wood items get re-declared across
	 * mods). The gate runs through the getAssociation_ filter (:726-729, {@code validData()}),
	 * so PREFIX-LESS existing data does not refuse a set — the upstream add/set asymmetry,
	 * pinned. */
	@Test
	void addWritesOnlyIntoAbsenceAndSetHonoursTheWoodException() {
		ItemStack tProbe = new ItemStack(Items.IRON_NUGGET); // an untouched item
		OreDictItemData tFirst = new OreDictItemData(OP.nugget, MT.Iron); // prefixed — the gate-visible shape
		assertTrue(OM.addItemData(tProbe, tFirst));
		// a second add declines and leaves the first data intact
		OreDictItemData tSecond = new OreDictItemData(OP.nugget, MT.Copper);
		assertFalse(OM.addItemData(tProbe, tSecond));
		assertSame(tFirst, OM.data(tProbe));
		// set on the prefixed non-Wood data refuses too (the :651-652 gate inside setItemData_)
		assertFalse(OM.setItemData(tProbe, tSecond));
		assertSame(tFirst, OM.data(tProbe));
		// the existing prefixed non-Wood data refuses even a PREFIX-LESS overwrite — the gate
		// reads the EXISTING data's shape (:651-652 through getAssociation_ :726-729), not the
		// incoming one
		OreDictItemData tPrefixless = new OreDictItemData(new OreDictMaterialStack(MT.Copper, CS.U9));
		assertFalse(OM.setItemData(tProbe, tPrefixless));
		assertSame(tFirst, OM.data(tProbe));
		// the asymmetry: PREFIX-LESS EXISTING data does NOT refuse a set (validData() filters
		// it out of the gate — upstream :726-729 semantics)
		ItemStack tLeather = new ItemStack(Items.LEATHER); // an untouched probe item (the map is static)
		assertTrue(OM.setItemData(tLeather, tPrefixless));
		OreDictItemData tOverPrefixless = new OreDictItemData(new OreDictMaterialStack(MT.Gold, CS.U9));
		assertTrue(OM.setItemData(tLeather, tOverPrefixless));
		assertSame(tOverPrefixless, OM.data(tLeather));
		// the Wood exception: existing Wood data overwrites
		ItemStack tWoodProbe = new ItemStack(Items.OAK_PLANKS);
		assertTrue(OM.setItemData(tWoodProbe, new OreDictItemData(OP.plate, MT.Wood)));
		OreDictItemData tReWood = new OreDictItemData(OP.plate, MT.WoodTreated);
		assertTrue(OM.setItemData(tWoodProbe, tReWood));
		assertSame(tReWood, OM.data(tWoodProbe));
		// add is stricter than set even on Wood: absence-only, so it declines
		assertFalse(OM.addItemData(tWoodProbe, new OreDictItemData(OP.plate, MT.Wood)));
		// set with a null data declines (upstream :647)
		assertFalse(OM.setItemData(new ItemStack(Items.STICK), null));
	}

	// ------------------------------------------------------------- ③ the stack-size amortization

	/** Acceptance ③: a multi-stack write amortizes Main Material and Byproducts by the count and
	 * the data keys per ONE item (:653-657, in-place mutation — upstream behavior). */
	@Test
	void stackSizeAmortizationDividesAmountsAndKeysPerItem() {
		// EGG: a probe item no other test writes (the map is static — the keys must not collide)
		ItemStack tFour = new ItemStack(Items.EGG, 4);
		OreDictItemData tWritten = new OreDictItemData(MT.Iron, CS.U, MT.Copper, CS.U4);
		assertTrue(OM.setItemData(tFour, tWritten));
		// the passed data MUTATED (upstream divides in place)
		assertEquals(CS.U / 4, tWritten.mMaterial.mAmount);
		assertEquals(CS.U4 / 4, tWritten.mByProducts[0].mAmount);
		// the read is per one item, regardless of the reading stack's size
		assertEquals(CS.U / 4, OM.data(new ItemStack(Items.EGG)).mMaterial.mAmount);
		assertEquals(CS.U / 4, OM.data(new ItemStack(Items.EGG, 64)).mMaterial.mAmount);
		// upstream :656 (ST.amount(1, aStack) = copy at size 1): the recyclable registration
		// carries a ONE-count copy even though the write went in with a 4-stack
		assertEquals(1, OM.recyclingRegistrations().stream().filter(tEvent -> tEvent.mStack.getItem() == Items.EGG)
				.findFirst().orElseThrow().mStack.getCount());
	}

	// ------------------------------------------------------------- ④ the recyclable notification gate

	/** Acceptance ④: only prefix-less data or a RECYCLABLE prefix notifies (:666-670); a plain
	 * prefix stays silent; a late listener replays the past registrations (:141-148). */
	@Test
	void recyclableNotificationGate() {
		// prefix-less data notifies
		assertTrue(OM.setItemData(new ItemStack(Items.BRICK), new OreDictItemData(new OreDictMaterialStack(MT.Iron, CS.U9))));
		assertTrue(notificationsFor(Items.BRICK) >= 1);
		// a non-RECYCLABLE prefix stays silent — ingot carries the flag (upstream OP.java:167,
		// port root OP.java:1214, verbatim), so the pin rides ore (upstream OP.java:55, no flag)
		int tBefore = sNotifications.size();
		assertTrue(OM.setItemData(new ItemStack(Items.GOLD_NUGGET), new OreDictItemData(OP.ore, MT.Gold)));
		assertEquals(tBefore, sNotifications.size());
		// a RECYCLABLE prefix notifies — crushed is the canonical one (upstream OP.java:135, port
		// OP.java:1175); ingot carries the flag too (upstream OP.java:167, port root OP.java:1214)
		assertTrue(OM.setItemData(new ItemStack(Items.RAW_IRON), new OreDictItemData(OP.crushed, MT.Iron)));
		assertTrue(notificationsFor(Items.RAW_IRON) >= 1);
		// a late listener catches up from the registration log (upstream :147 replay)
		List<String> tLateLog = new ArrayList<>();
		OM.addListener(tEvent -> tLateLog.add(tEvent.mStack.getItem().toString()));
		assertTrue(tLateLog.contains(Items.BRICK.toString()));
		assertTrue(tLateLog.contains(Items.RAW_IRON.toString()));
		// the registration log itself (upstream mRecyclableRegistrations :70)
		assertTrue(OM.recyclingRegistrations().stream().anyMatch(tEvent -> tEvent.mStack.getItem() == Items.BRICK));
	}

	private static long notificationsFor(Item aItem) {
		return sNotifications.stream().filter(aItem.toString()::equals).count();
	}

	// ------------------------------------------------------------- the mBlocked computation

	/** The :659 computation: a plain item stays unblocked, a block item blocks, the data's own
	 * mBlackListed flag blocks. (The fluid-container arm rides the live capability registry —
	 * see the OM javadoc; offline it stays unexercised on purpose.) */
	@Test
	void blockedComputation() {
		// PREFIXED data (prefix-less data carries mBlackListed=T by construction — the
		// upstream OreDictItemData.java:61 quirk — which would mask all three arms)
		ItemStack tPlain = new ItemStack(Items.STICK);
		assertTrue(OM.setItemData(tPlain, new OreDictItemData(OP.ingot, MT.Wood)));
		assertFalse(OM.data(tPlain).mBlocked);
		ItemStack tBlock = new ItemStack(Items.IRON_BLOCK);
		assertTrue(OM.setItemData(tBlock, new OreDictItemData(OP.ingot, MT.Iron)));
		assertTrue(OM.data(tBlock).mBlocked);
		OreDictItemData tBlacklisted = new OreDictItemData(OP.nugget, MT.Iron);
		tBlacklisted.mBlackListed = true;
		ItemStack tProbe = new ItemStack(Items.FLINT);
		assertTrue(OM.setItemData(tProbe, tBlacklisted));
		OreDictItemData tRead = OM.data(tProbe);
		assertTrue(tRead.mBlackListed);
		assertTrue(tRead.mBlocked);
	}

	// ------------------------------------------------------------- the unification-target seam

	/** The handed-over constraint: mUnificationTarget (:39) + getStack (:171-173) live on the
	 * face — the target arm wins, the cache invalidates without losing the map answer (:781). */
	@Test
	void unificationTargetSeamAndStack() {
		// (gem, Iron): unregistered on BOTH legs — the neo leg's FML boot fills the registration
		// index (gt6:ingot_iron is live there), so the degradation pin needs a pair the mod
		// never generates items for (gem items exist for gem materials only)
		OreDictItemData tData = new OreDictItemData(OP.gem, MT.Iron);
		// no target registered and the pair is not in the (live) registration index: EMPTY —
		// the upstream-faithful degradation of an unfilled sName2StackMap
		assertTrue(OM.getStack(tData, 1).isEmpty());
		// the target arm: a registered canonical stack wins and copies the count
		ItemStack tTarget = new ItemStack(Items.IRON_INGOT);
		OM.putUnificationTarget(tData.toString(), tTarget);
		assertSame(tTarget, OM.unificationTarget(tData)); // the lazy cache holds the same instance (upstream :632)
		ItemStack tStacked = OM.getStack(tData, 8);
		assertEquals(Items.IRON_INGOT, tStacked.getItem());
		assertEquals(8, tStacked.getCount());
		// the cache invalidates (:781) and the map re-answers (invalidation loses nothing)
		OM.clearUnificationTargets();
		assertSame(tTarget, OM.unificationTarget(tData));
		// prefix-less data answers EMPTY (upstream :172 would NPE — the port degrades, declared)
		assertTrue(OM.getStack(new OreDictItemData(new OreDictMaterialStack(MT.Iron, CS.U)), 1).isEmpty());
		// a null data answers EMPTY too
		assertTrue(OM.getStack(null, 1).isEmpty());
	}

	// ------------------------------------------------------------- the offline probe (the GT6RecipeTagFallbackTest helper, mirrored)

	/**
	 * The offline probe item (the FileSawTest/ScrewdriverTest precedent): the Forge
	 * intrusive holder makes {@code new MaterialPrefixItem(...)} throw while the vanilla
	 * item registry is frozen, so the probe item registers under a dedicated probe id.
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Supplier<I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// the Forge runtime shape: THREE locks must open (the vanilla frozen flag, the
			// delegate ForgeRegistry.isFrozen, the NamespacedWrapper.locked register gate)
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
			// the 21.1 runtime shape: a single frozen flag guards both the intrusive-holder
			// construction and Registry.register
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		I rItem = aCreator.get();
		net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
		return rItem;
	}

	/** getDeclaredField along the superclass chain (the FileSawTest helper, mirrored). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// walk up
			}
		}
		throw new NoSuchFieldException(aName);
	}
}
