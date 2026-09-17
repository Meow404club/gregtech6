package gregtech6.itemdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Optional;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.items.tools.GTCrowbarItem;

/**
 * The GT6ItemData keyed access seam — task p31-identity-seam acceptance: the
 * same-key set/get round trip, the raw-tag passthrough keeping payloads verbatim
 * (battery Base08 / bucket keepFilter / still shapes), and the fail-visible
 * missing/corrupt semantics (an exception or the explicit empty, NEVER a silent
 * default). The crowbar first-consumer arms ride {@link GTCrowbarItem}'s static
 * seams (the mod-Item offline wall keeps the instance out of this JVM — the
 * CrowbarTest premise).
 */
public class GT6ItemDataTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the full material flood — MT class-load only registers NULL (MT.java:1366-1371);
		// Steel & co. exist after init() (the GT6CrucibleLadderCensusTest boot shape)
		MT.init();
	}

	// ------------------------------------------------------------- round trip

	@Test
	void sameKeyRoundTrip() {
		ItemStack tStack = new ItemStack(Items.STICK);
		GT6ToolStats tStats = GT6ToolStats.of(MT.Steel, MT.Brass, 1.0F);

		GT6ItemData.set(tStack, GT6ToolStats.KEY, tStats);

		Optional<GT6ToolStats> tBack = GT6ItemData.find(tStack, GT6ToolStats.KEY);
		assertTrue(tBack.isPresent(), "the payload is present after set");
		assertSame(MT.Steel, tBack.get().primaryMaterial(), "the primary material round-trips by identity");
		assertSame(MT.Brass, tBack.get().secondaryMaterial(), "the secondary material round-trips by identity");
		assertEquals(MT.Steel.mToolDurability * 100, tBack.get().maxDamage(), "the :182 j snapshot (512 * 100 * 1.0)");
	}

	@Test
	void firstKeyRidesTheUpstreamCompoundShape() {
		// guardrail ①: the 1.20.1 storage form is the upstream verbatim compound —
		// "GT.ToolStats" with the short-typed "a" (the MaterialStackNBT "i" precedent)
		// and the long-typed "j" (MultiItemTool.java:181-192)
		ItemStack tStack = new ItemStack(Items.STICK);
		GT6ItemData.set(tStack, GT6ToolStats.KEY, GT6ToolStats.of(MT.Steel, null, 1.0F));

		//? if forge {
		CompoundTag tRoot = tStack.getTag();
		assertTrue(tRoot != null && tRoot.contains("GT.ToolStats"), "the upstream compound name (:192)");
		CompoundTag tToolTag = tRoot.getCompound("GT.ToolStats");
		assertTrue(tToolTag.contains("a", Tag.TAG_SHORT), "the 'a' key must be stored as a short");
		assertFalse(tToolTag.contains("a", Tag.TAG_INT), "the 'a' key must not widen to an int");
		assertEquals((short) MT.Steel.mID, tToolTag.getShort("a"));
		assertTrue(tToolTag.contains("j", Tag.TAG_LONG), "the 'j' key is the long max-damage snapshot");
		assertFalse(tToolTag.contains("c"), "no secondary material, no c/d keys (:184 absent stays absent)");
		//?} else {
		/*// the 1.21.1 carrier is the registered tool_stats DataComponentType; the compound
		//shape is asserted at the codec level in codecRoundTripsBothMaterialArms.
		assertTrue(GT6ItemData.find(tStack, GT6ToolStats.KEY).isPresent(), "the component carries the payload");
		assertSame(MT.Steel, GT6ItemData.get(tStack, GT6ToolStats.KEY).primaryMaterial());
		*///?}
	}

	@Test
	void codecRoundTripsBothMaterialArms() {
		// the id arm (mID > 0 → short "a") and the name arm (else string "b", :181)
		CompoundTag tById = GT6ToolStats.of(MT.Steel, null, 1.0F).toTag();
		GT6ToolStats tBackById = GT6ToolStats.CODEC.parse(NbtOps.INSTANCE, tById).resultOrPartial(tMsg -> {}).orElseThrow();
		assertSame(MT.Steel, tBackById.primaryMaterial());

		CompoundTag tByName = new CompoundTag();
		tByName.putString("b", MT.Steel.toString());
		tByName.putLong("j", 51200L);
		GT6ToolStats tBackByName = GT6ToolStats.CODEC.parse(NbtOps.INSTANCE, tByName).resultOrPartial(tMsg -> {}).orElseThrow();
		assertSame(MT.Steel, tBackByName.primaryMaterial(), "the string 'b' arm resolves by internal name");
		assertEquals(51200L, tBackByName.maxDamage());
	}

	// ------------------------------------------------------------ fail visible

	@Test
	void missingKeyIsExplicitNotSilent() {
		ItemStack tStack = new ItemStack(Items.STICK);

		assertTrue(GT6ItemData.find(tStack, GT6ToolStats.KEY).isEmpty(), "absent = explicit empty");
		assertThrows(NoSuchElementException.class, () -> GT6ItemData.get(tStack, GT6ToolStats.KEY),
				"fail-visible: the unqualified get throws instead of fabricating a default");

		GT6ToolStats tFallback = GT6ToolStats.of(MT.Steel, null, 1.0F);
		assertSame(tFallback, GT6ItemData.get(tStack, GT6ToolStats.KEY, tFallback),
				"the three-arg form is the CALLER declaring its fallback at the call site");
	}

	@Test
	void corruptPayloadFailsVisibly() {
		// a payload present but under-specified: decode ERROR, never a silent MT.NULL fallback
		CompoundTag tNoMaterial = new CompoundTag();
		tNoMaterial.putLong("j", 51200L);
		assertThrows(RuntimeException.class, () -> GT6ToolStats.CODEC.parse(NbtOps.INSTANCE, tNoMaterial).resultOrPartial(tMsg -> {}).orElseThrow(),
				"a/b both missing → decode error");

		// an unresolvable material name → the registry miss lands on MT.NULL → decode error
		CompoundTag tUnknown = new CompoundTag();
		tUnknown.putString("b", "NoSuchMaterialAnyMore");
		tUnknown.putLong("j", 51200L);
		assertThrows(RuntimeException.class, () -> GT6ToolStats.CODEC.parse(NbtOps.INSTANCE, tUnknown).resultOrPartial(tMsg -> {}).orElseThrow(),
				"unknown material → decode error, not the silent MT.NULL of the upstream :362 default");

		// missing j
		CompoundTag tNoJ = new CompoundTag();
		tNoJ.putShort("a", MT.Steel.mID);
		assertThrows(RuntimeException.class, () -> GT6ToolStats.CODEC.parse(NbtOps.INSTANCE, tNoJ).resultOrPartial(tMsg -> {}).orElseThrow(),
				"j missing → decode error");

		// garbage IN fails the encode face identically on both legs (on 1.21.1 the in-memory
		// set is a typed component store — the encode is the disk/sync boundary where the DC
		// fail-fast lands)
		GT6ToolStats tBroken = new GT6ToolStats(null, null, 51200L);
		assertThrows(RuntimeException.class,
				() -> GT6ToolStats.CODEC.encodeStart(NbtOps.INSTANCE, tBroken).resultOrPartial(tMsg -> {}).orElseThrow(),
				"encoding a null-primary payload throws — the fail-visible parity of the 1.21.1 DC fail-fast");

		//? if forge {
		// the storage-corruption arm on the leg where the payload IS the root tag: a foreign
		// compound under the key dies on read (the 1.21.1 counterpart is the DC load fail-fast,
		// the platform's own visible failure — not reproducible through an in-memory component)
		ItemStack tStack = new ItemStack(Items.STICK);
		tStack.getOrCreateTag().put("GT.ToolStats", tNoJ);
		assertThrows(RuntimeException.class, () -> GT6ItemData.get(tStack, GT6ToolStats.KEY),
				"a corrupt stored payload throws on read");
		//?}
	}

	// -------------------------------------------------------------- raw bypass

	@Test
	void rawTagPassthroughStaysVerbatim() {
		// guardrail ②: keyed writes never disturb the verbatim payload carrier
		ItemStack tStack = new ItemStack(Items.STICK);
		byte[] tBase08 = {1, 2, 3, -4};
		GT6ItemData.updateRaw(tStack, tTag -> {
			tTag.putByteArray("Base08", tBase08); // the battery Base08 shape
			tTag.putBoolean("keepFilter", true); // the bucket keepFilter shape
			CompoundTag tStill = new CompoundTag(); // the still payload shape
			tStill.putLong("gt.materials", 77L);
			tTag.put("gt.still_payload", tStill);
		});

		CompoundTag tSnapshot = GT6ItemData.rawTag(tStack);
		assertTrue(tSnapshot != null && tSnapshot.contains("Base08"), "the raw bypass reads the payload");

		GT6ItemData.set(tStack, GT6ToolStats.KEY, GT6ToolStats.of(MT.DamascusSteel, null, 1.0F));

		// every PRE-EXISTING raw key survives the keyed write untouched, value-identical.
		// On the 1.20.1 leg the root tag IS the keyed storage (the upstream shape — the
		// GT.ToolStats key lands alongside the payload, MultiItemTool.java:193), so the
		// assertion is per-key, not whole-compound; on the 1.21.1 leg the carriers are
		// separate components and the raw envelope is untouched entirely.
		CompoundTag tAfter = GT6ItemData.rawTag(tStack);
		for (String tKey : tSnapshot.getAllKeys()) {
			assertEquals(tSnapshot.get(tKey), tAfter.get(tKey), "raw key \"" + tKey + "\" stays verbatim");
		}
		assertSame(MT.DamascusSteel, GT6ItemData.get(tStack, GT6ToolStats.KEY).primaryMaterial(),
				"the keyed payload coexists with the raw one");
	}

	// ----------------------------------------------- the crowbar first consumer

	@Test
	void crowbarPerMaterialDurability() {
		// the legacy arm: identity-less stacks keep the ADR-pinned 512
		assertEquals(512, GTCrowbarItem.DURABILITY_POINTS);
		assertEquals(512, GTCrowbarItem.durabilityPoints(null));

		// the identity arms at the pinned 100 units = 1 point ratio (multiplier 1.0F, GT_Tool_Crowbar :83-85)
		assertEquals(512, GTCrowbarItem.durabilityPoints(GT6ToolStats.of(MT.Steel, null, 1.0F)),
				"Steel stays at the pinned ADR value");
		assertEquals(MT.TungstenSteel.mToolDurability, GTCrowbarItem.durabilityPoints(GT6ToolStats.of(MT.TungstenSteel, null, 1.0F)),
				"TungstenSteel 5120 → 5120 points (:182)");
		assertEquals(MT.Bronze.mToolDurability, GTCrowbarItem.durabilityPoints(GT6ToolStats.of(MT.Bronze, null, 1.0F)),
				"Bronze 448 → 448 points");
		assertEquals(MT.DamascusSteel.mToolDurability, GTCrowbarItem.durabilityPoints(GT6ToolStats.of(MT.DamascusSteel, null, 1.0F)),
				"DamascusSteel 1280 → 1280 points");
	}

	@Test
	void crowbarRuntimeTint() {
		// the upstream :148 steel fallback for identity-less stacks
		ItemStack tLegacy = new ItemStack(Items.STICK);
		assertEquals(steelARGB(), GTCrowbarItem.tintARGB(tLegacy, 0), "no identity → the verbatim MT.Steel fallback");

		// the identity arm: the primary material mRGBaSolid, packed ARGB
		ItemStack tIdentified = new ItemStack(Items.STICK);
		GT6ItemData.set(tIdentified, GT6ToolStats.KEY, GT6ToolStats.of(MT.DamascusSteel, null, 1.0F));
		assertEquals(damascusARGB(), GTCrowbarItem.tintARGB(tIdentified, 0), "tint index 0 = the material colour");

		// the overlay pass (model layer1 = tint index 1) stays un-tinted — the -1 sentinel
		assertEquals(-1, GTCrowbarItem.tintARGB(tIdentified, 1));
	}

	private static int steelARGB() {
		return 0xFF000000 | (MT.Steel.mRGBaSolid[0] << 16) | (MT.Steel.mRGBaSolid[1] << 8) | MT.Steel.mRGBaSolid[2];
	}

	private static int damascusARGB() {
		return 0xFF000000 | (MT.DamascusSteel.mRGBaSolid[0] << 16) | (MT.DamascusSteel.mRGBaSolid[1] << 8) | MT.DamascusSteel.mRGBaSolid[2];
	}

	@Test
	void keyRegistryIsNameUnique() {
		assertTrue(GT6DataKey.registry().containsKey("GT.ToolStats"), "the first key is registered");
		assertSame(GT6ToolStats.KEY, GT6DataKey.registry().get("GT.ToolStats"));
		assertThrows(IllegalStateException.class, () -> new GT6DataKey<>("GT.ToolStats", "another_path", GT6ToolStats.CODEC),
				"a duplicate key name fails loudly — never a silent second slot");
		assertSame(GT6ToolStats.KEY, GT6DataKey.registry().get("GT.ToolStats"), "the original key keeps its slot");
	}
}
