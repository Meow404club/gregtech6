package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.example.GTExampleChestBlockEntity;

//? if forge {
import net.minecraft.world.item.enchantment.Enchantments;
//?} else {
/*import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.Lifecycle;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
*///?}

/**
 * Task p19-nbtaccess-dynamic-rebind — the NBT_ACCESS rebind contract, offline.
 *
 * <p>Production shape (the {@code //? if neoforge} leg of TileEntityBase03TicksAndSync):
 * the public static NBT_ACCESS stays a FINAL field, its value a delegating Provider over an
 * AtomicReference whose initial delegate is the frozen builtin registry view (the P18
 * behaviour, byte for byte); the embedded ServerRegistryAccessBinder rebinds the delegate at
 * ServerAboutToStart to the server's composite RegistryAccess. These tests stand in for the
 * server composite with a synthetic Provider = frozen builtin + one data-driven enchantment
 * registry (exactly what the 1.21.1 composite adds over the builtin layer — enchantments are
 * WORLDGEN-data-driven there and BuiltInRegistries has no ENCHANTMENT field), so every
 * terminal state below is pinned without a server.
 *
 * <p>Measured terminal states over the frozen fallback (the pre-card behaviour, measured on
 * 1.21.1 + NeoForge 21.1.249, correcting the research card's prediction): the save face
 * THROWS (the component codec errors; Neo DataComponentUtil wrapEncodingExceptions
 * getOrThrow rethrows even a partial result) and the parse face PARTIALLY succeeds — the
 * item survives WITHOUT its enchantments (DataComponentPatch decode is per-entry tolerant;
 * ItemStack.parse resultOrPartial keeps the partial), i.e. a silent enchantment strip, not
 * a lost stack.
 */
public class TileEntityBase03TicksAndSyncNbtAccessTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(2, 3, 4);

	static BlockEntityType<GTExampleChestBlockEntity> sType;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildOfflineFixture() {
		BlockEntityType<GTExampleChestBlockEntity>[] tHolder =
				(BlockEntityType<GTExampleChestBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTExampleChestBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	// -----------------------------------------------------------------------
	// 21.1-only fixture: the synthetic composite (frozen builtin + one
	// data-driven enchantment registry, the 1.21.1 composite shape) and a
	// registered test enchantment inside it.
	// -----------------------------------------------------------------------

	//? if neoforge {
	/*static Holder<Enchantment> sTestEnchantment;

	// Per-phase enchantment ids: NeoForge's EncoderCache (net.minecraft.util.EncoderCache)
	// memoizes component-encode DataResults under a Key that compares the VALUE by equals —
	// ItemEnchantments.equals is holder-KEY based and ResourceKeys are interned, so two
	// synthetic registries holding the SAME id produce equal values and a prior encode under
	// ANOTHER view is reused. Distinct ids per test keep every phase order-independent.
	static HolderLookup.Provider buildComposite(String aEnchantmentId) {
		// the data-driven registry: a fresh MappedRegistry on the vanilla ENCHANTMENT key
		// (validateWrite only guards its own frozen flag — a fresh registry is writable,
		// exactly how WorldLoader's WORLDGEN layer builds them)
		MappedRegistry<Enchantment> tRegistry = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
		Enchantment tEnchantment = new Enchantment(Component.literal("test enchantment"),
				new Enchantment.EnchantmentDefinition(
						HolderSet.direct(BuiltInRegistries.ITEM.wrapAsHolder(Items.DIAMOND_SWORD)),
						Optional.empty(), 10, 5,
						new Enchantment.Cost(1, 1), new Enchantment.Cost(5, 5), 2,
						java.util.List.of(EquipmentSlotGroup.MAINHAND)),
				HolderSet.empty(), DataComponentMap.EMPTY);
		sTestEnchantment = Registry.registerForHolder(tRegistry,
				ResourceKey.create(Registries.ENCHANTMENT, new ResourceLocation(aEnchantmentId)),
				tEnchantment);
		// the composite: builtin lookups forward to the frozen view, ENCHANTMENT resolves
		// into the registry above — the exact delta the server composite adds (frozen
		// builtin + the WORLDGEN layer registries)
		return new HolderLookup.Provider() {
			@Override
			public Stream<ResourceKey<? extends net.minecraft.core.Registry<?>>> listRegistries() {
				return Stream.concat(
						RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).listRegistries(),
						Stream.of(Registries.ENCHANTMENT));
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends net.minecraft.core.Registry<? extends T>> aKey) {
				if (Registries.ENCHANTMENT.equals(aKey)) {
					return Optional.of((HolderLookup.RegistryLookup<T>) tRegistry.asLookup());
				}
				return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).lookup(aKey);
			}
		};
	}
	*///?}

	// -----------------------------------------------------------------------
	// the forge leg: 1.20.1 enchantments are static (BuiltInRegistries.ENCHANTMENT
	// bootstrapped) — the provider-less round trip is the correct leg form and the
	// parity baseline the 21.1 rebind restores.
	// -----------------------------------------------------------------------

	//? if forge {
	/**
	 * The enchanted ItemStack round trip through the BE inventory NBT face without any
	 * provider (1.20.1: the codec face rides the static bootstrapped registries). This is
	 * the behaviour the 21.1 leg must match after the ServerAboutToStart rebind.
	 */
	@Test
	public void enchantedItemStackRoundTripsWithoutAnyProvider() {
		GTItemStackHandler tHandler = new GTItemStackHandler(2);
		ItemStack tSword = new ItemStack(Items.DIAMOND_SWORD);
		tSword.enchant(Enchantments.SHARPNESS, 5);
		tHandler.setStackInSlot(0, tSword);

		CompoundTag tSaved = tHandler.serializeNBT();
		GTItemStackHandler tBack = new GTItemStackHandler(2);
		tBack.deserializeNBT(tSaved);

		ItemStack tRestored = tBack.getStackInSlot(0);
		assertFalse(tRestored.isEmpty(), "the enchanted sword survives the save/parse");
		assertEquals(5, tRestored.getEnchantmentLevel(Enchantments.SHARPNESS),
				"the enchantment level survives the save/parse round trip");
	}
	//?}

	// -----------------------------------------------------------------------
	// 21.1 legs: the delegate lifecycle (the listener's contract), the fallback
	// degradation (P18 frozen view — the measured terminal states), the rebound
	// preservation (the fix), and the plain-content snapshot equivalence (P18
	// method). Every rebind restores the entry delegate in a finally.
	// -----------------------------------------------------------------------

	//? if neoforge {
	/*// The rebind contract (the listener's job, exercised through the package-private seam):
	// the delegate starts as the frozen builtin fallback, a bind swaps it for the composite
	// view, and both abstract Provider points forward (lookup + listRegistries — the funnel
	// the default createSerializationContext and the NeoForge holder extensions ride).
	@Test
	public void delegateStartsFrozenAndRebindSwapsTheView() {
		assertTrue(TileEntityBase03TicksAndSync.nbtAccessDelegate() instanceof RegistryAccess.Frozen,
				"the initial delegate is the frozen builtin fallback view");
		assertTrue(TileEntityBase03TicksAndSync.NBT_ACCESS.lookup(Registries.ENCHANTMENT).isEmpty(),
				"the frozen fallback misses the data-driven enchantment registry (the P19 gap)");
		assertFalse(TileEntityBase03TicksAndSync.NBT_ACCESS.listRegistries()
				.anyMatch(aKey -> Registries.ENCHANTMENT.equals(aKey)),
				"listRegistries forwards: no enchantment registry before the rebind");

		HolderLookup.Provider tComposite = buildComposite("gt6:test_lifecycle_enchant");
		HolderLookup.Provider tPrevious = TileEntityBase03TicksAndSync.nbtAccessDelegate();
		TileEntityBase03TicksAndSync.bindNbtAccess(tComposite);
		try {
			assertTrue(TileEntityBase03TicksAndSync.nbtAccessDelegate() == tComposite,
					"the bind swaps the delegate");
			assertTrue(TileEntityBase03TicksAndSync.NBT_ACCESS.lookup(Registries.ENCHANTMENT).isPresent(),
					"the rebound view resolves the data-driven enchantment registry (the fix)");
			assertTrue(TileEntityBase03TicksAndSync.NBT_ACCESS.listRegistries()
					.anyMatch(aKey -> Registries.ENCHANTMENT.equals(aKey)),
					"listRegistries forwards: the enchantment registry is listed after the rebind");
		} finally {
			TileEntityBase03TicksAndSync.bindNbtAccess(tPrevious);
		}
	}

	// The degradation terminal states over the untouched frozen fallback (the pre-card
	// behaviour, pinned as measured — see the class javadoc): the save face THROWS and the
	// parse face silently strips the enchantments while keeping the item. The parse arm
	// hand-writes the 1.21.1 item tag a /give'd enchanted sword produces
	// ({id, count, components:{enchantments:{...}}}).
	@Test
	public void frozenFallbackViewDegradesEnchantedStacks() {
		HolderLookup.Provider tPrevious = TileEntityBase03TicksAndSync.nbtAccessDelegate();
		HolderLookup.Provider tComposite = buildComposite("gt6:test_frozen_enchant");
		TileEntityBase03TicksAndSync.bindNbtAccess(tComposite);
		// build the enchanted stack under the composite view, then judge the fallback faces
		ItemStack tSword = new ItemStack(Items.DIAMOND_SWORD);
		tSword.enchant(sTestEnchantment, 5);
		TileEntityBase03TicksAndSync.bindNbtAccess(tPrevious);
		try {
			// the save face: the codec cannot resolve the enchantment holder's registry → throw
			assertThrows(RuntimeException.class,
					() -> tSword.save(TileEntityBase03TicksAndSync.NBT_ACCESS, new CompoundTag()),
					"the frozen fallback cannot encode an enchanted stack (measured: throws)");

			// the parse face: the hand-written tag decodes PARTIALLY — the item survives
			// without its enchantments (the silent strip, the real chunk-reload regression)
			CompoundTag tTag = new CompoundTag();
			tTag.putString("id", "minecraft:diamond_sword");
			tTag.putInt("count", 1);
			CompoundTag tEnchantments = new CompoundTag();
			tEnchantments.putInt("gt6:test_frozen_enchant", 5);
			CompoundTag tComponents = new CompoundTag();
			tComponents.put("minecraft:enchantments", tEnchantments);
			tTag.put("components", tComponents);
			assertTrue(TileEntityBase03TicksAndSync.NBT_ACCESS
					.lookup(Registries.ENCHANTMENT).isEmpty(),
					"precondition: the judge view is the untouched frozen fallback");
			ItemStack tBack = ItemStack.parseOptional(TileEntityBase03TicksAndSync.NBT_ACCESS, tTag);
			assertFalse(tBack.isEmpty(), "the frozen fallback parse keeps the item (measured: partial decode)");
			assertTrue(tBack.is(Items.DIAMOND_SWORD), "the parsed item is the same item");
			assertEquals(1, tBack.getCount(), "the parsed stack keeps its count");
			assertFalse(tBack.isEnchanted(), "the enchantments are silently stripped (measured)");
		} finally {
			TileEntityBase03TicksAndSync.bindNbtAccess(tPrevious);
		}
	}

	// The fix: over the rebound (composite) view the enchanted ItemStack round-trips through
	// the BE NBT faces — the ItemStack save/parse face and the GTItemStackHandler inventory
	// face (the exact faces the BE load/save legs ride) — matching the 1.20.1 baseline test
	// above.
	@Test
	public void reboundViewRoundTripsEnchantedStacks() {
		HolderLookup.Provider tPrevious = TileEntityBase03TicksAndSync.nbtAccessDelegate();
		HolderLookup.Provider tComposite = buildComposite("gt6:test_rebound_enchant");
		TileEntityBase03TicksAndSync.bindNbtAccess(tComposite);
		try {
			// the ItemStack save/parse face
			ItemStack tSword = new ItemStack(Items.DIAMOND_SWORD);
			tSword.enchant(sTestEnchantment, 5);
			CompoundTag tSaved = (CompoundTag) tSword.save(TileEntityBase03TicksAndSync.NBT_ACCESS, new CompoundTag());
			ItemStack tBack = ItemStack.parseOptional(TileEntityBase03TicksAndSync.NBT_ACCESS, tSaved);
			assertFalse(tBack.isEmpty(), "the enchanted sword survives the codec save/parse over the rebound view");
			assertTrue(tBack.isEnchanted(), "the enchantment component survives");
			assertEquals(1, tBack.getEnchantments().size(), "exactly one enchantment entry");
			assertEquals(5, tBack.getEnchantments().getLevel(sTestEnchantment),
					"the enchantment level survives the round trip");

			// the BE inventory face (GTItemStackHandler serialize/deserializeNBT(provider, tag))
			GTItemStackHandler tHandler = new GTItemStackHandler(2);
			tHandler.setStackInSlot(0, tSword);
			CompoundTag tInv = tHandler.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS);
			GTItemStackHandler tRestored = new GTItemStackHandler(2);
			tRestored.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, tInv);
			ItemStack tFromSlot = tRestored.getStackInSlot(0);
			assertFalse(tFromSlot.isEmpty(), "the enchanted sword survives the inventory face");
			assertTrue(tFromSlot.isEnchanted(), "the inventory face preserves the enchantment");
			assertEquals(5, tFromSlot.getEnchantments().getLevel(sTestEnchantment),
					"the inventory face preserves the level (the BE slot round trip closes)");
		} finally {
			TileEntityBase03TicksAndSync.bindNbtAccess(tPrevious);
		}
	}

	// Acceptance: the plain (non-enchanted) BE NBT face is byte-identical under the frozen
	// fallback and the rebound composite view (the P18 snapshot method, mechanized) — the
	// rebind must not perturb a single existing save. A real BE (the example chest) saves
	// under both delegates; the trees must be deep-equal AND compress to identical bytes.
	@Test
	public void plainBlockEntityNbtIsByteIdenticalAcrossBothViews() throws Exception {
		GTExampleChestBlockEntity tChest = sType.create(POS, Blocks.STONE.defaultBlockState());
		tChest.getInventory().setStackInSlot(3, new ItemStack(Items.IRON_INGOT, 7));
		tChest.getInventory().setStackInSlot(4, new ItemStack(Items.DIAMOND_SWORD, 1));

		CompoundTag tFrozen = tChest.saveWithoutMetadata(TileEntityBase03TicksAndSync.nbtAccessDelegate());

		HolderLookup.Provider tPrevious = TileEntityBase03TicksAndSync.nbtAccessDelegate();
		HolderLookup.Provider tComposite = buildComposite("gt6:test_plain_enchant");
		TileEntityBase03TicksAndSync.bindNbtAccess(tComposite);
		try {
			CompoundTag tRebound = tChest.saveWithoutMetadata(TileEntityBase03TicksAndSync.NBT_ACCESS);
			assertEquals(tFrozen, tRebound, "the plain BE NBT tree is deep-equal across both views");
			assertTrue(Arrays.equals(nbtBytes(tFrozen), nbtBytes(tRebound)),
					"the plain BE NBT compresses to identical bytes across both views (P18 snapshot)");
		} finally {
			TileEntityBase03TicksAndSync.bindNbtAccess(tPrevious);
		}
	}

	// The gzip-NBT byte face of a tag (the P18 snapshot comparison, in-process).
	private static byte[] nbtBytes(CompoundTag aTag) throws Exception {
		java.io.ByteArrayOutputStream tBytes = new java.io.ByteArrayOutputStream();
		NbtIo.writeCompressed(aTag, tBytes);
		return tBytes.toByteArray();
	}
	*///?}
}
