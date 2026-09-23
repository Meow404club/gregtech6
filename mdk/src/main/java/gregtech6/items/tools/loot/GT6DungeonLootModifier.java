package gregtech6.items.tools.loot;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

//? if forge {
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
//21.1: same simple names, neoforged package (the GT6ToolLootModifiers fork verbatim).
*///?}

/**
 * The dungeon-loot INJECTION seam — task p34-loot-injection. Upstream
 * {@code loaders/c/Loader_Loot.java:410-552} added weighted {@code WeightedRandomChestContent}
 * rows into the vanilla {@code ChestGenHooks} structure categories (dungeon, mineshaft,
 * stronghold x3, pyramid x2/dispenser, village blacksmith); the port re-arms those rows as a
 * Forge Global-Loot-Modifier over the LOOT-TABLE-ID seam (the {@code GT6SafeBlockEntity}
 * dungeonloot key already resolves the same vanilla table ids through {@code gt6:chests/safe_*}
 * — injections into the vanilla tables reach the safes for free).
 *
 * <p><b>Why the table id rides OUR codec</b> instead of the platform
 * {@code LootTableIdCondition}: the shared generated tree is ONE set of JSONs for both legs
 * (ADR-P17-1) and the platform condition is loader-branded ({@code forge:loot_table_id} vs
 * {@code neoforge:loot_table_id}) — a {@code gt6:}-namespaced modifier JSON must stay
 * byte-identical across legs, so the check reads
 * {@code LootContext.getQueriedLootTableId()} directly (the exact call both platform
 * conditions make, javap-verified on Forge 1.20.1 47.4.10 and NeoForge 21.1.209; the chain
 * itself fires from the Forge {@code LootTable} patch
 * {@code ForgeHooks.modifyLoot(getLootTableId(), ...)} which chest fills AND {@code /loot}
 * rolls go through — LootTable.java.patch m_230922_). The empty {@code conditions} array is
 * the platform base-class contract (it runs the array first).
 *
 * <p><b>The declared semantic shift</b>: upstream GT rows COMPETED with the vanilla entries for
 * the same chest rolls; a GLM sees only the generated stacks, so it rolls its own
 * {@code rolls} picks — {@code [1,3]}, the share-approximation of the upstream weight share
 * (~1-2 GT stacks per structure chest). // ponytail: fixed [1,3] everywhere; per-table tuning
 * is a datagen-row change (the codec already carries the field) when a live-dungeon census
 * exists.
 *
 * <p><b>Consumer contract</b> ({@link GT6ToolLootModifiers}): new modifier class + ONE
 * serializer row under the shared {@code SERIALIZERS} register, no new DeferredRegister; the
 * per-table instances are datagen-built ({@code GT6ToolLootModifiersDatagen}; the row mirror
 * lives in {@code gregtech6.datagen.GT6LootInjectionDatagen}). Existing tool modes stay
 * zero-diff.
 */
public class GT6DungeonLootModifier extends LootModifier {

	/**
	 * One upstream {@code addLoot(category, aChance, aMin, aMax, stack)} row
	 * ({@code Loader_Loot.java:565-574}): {@code weight} = the pool weight (aChance), the
	 * [min,max] stack range clamped to maxStackSize at roll time (the upstream :571 clamp).
	 * The optional {@code tag} is the task-p36 artifact lane — the ZPM dungeon face spawns
	 * the module 2/3 FULL ({@code DungeonData.zpm:306-310}, the {@code gt.active.energy}
	 * store-as-full key), the only upstream row that ever carried NBT.
	 */
	public record Entry(Item item, int weight, int min, int max, net.minecraft.nbt.CompoundTag tag) {

		/** The NBT-less form every Loader_Loot row takes. */
		public Entry(Item aItem, int aWeight, int aMin, int aMax) {
			this(aItem, aWeight, aMin, aMax, null);
		}
	}

	/** The entry codec — {@code {"item": "gt6:ingot_steel", "weight": 12, "min": 1, "max": 6}} (+ the optional artifact tag). */
	static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(aInst -> aInst.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
			Codec.intRange(1, 4096).fieldOf("weight").forGetter(Entry::weight),
			Codec.intRange(1, 64).fieldOf("min").forGetter(Entry::min),
			Codec.intRange(1, 64).fieldOf("max").forGetter(Entry::max),
			net.minecraft.nbt.CompoundTag.CODEC.optionalFieldOf("tag").forGetter(aEntry -> java.util.Optional.ofNullable(aEntry.tag())))
			.apply(aInst, (aItem, aWeight, aMin, aMax, aTag) -> new Entry(aItem, aWeight, aMin, aMax, aTag.orElse(null))));

	/**
	 * The element type forks per leg — Codec (forge 1.20.1) vs MapCodec (neo 21.1), the
	 * {@code GT6ToolConvertModifier.CODEC} fork verbatim.
	 */
	//? if forge {
	public static final Codec<GT6DungeonLootModifier> CODEC =
			RecordCodecBuilder.create(aInst -> codecStart(aInst)
					.and(ResourceLocation.CODEC.fieldOf("table").forGetter(aModifier -> aModifier.mTable))
					.and(UniformInt.CODEC.fieldOf("rolls").forGetter(aModifier -> aModifier.mRolls))
					.and(ENTRY_CODEC.listOf().fieldOf("entries").forGetter(aModifier -> aModifier.mEntries))
					.apply(aInst, GT6DungeonLootModifier::new));
	//?} else {
	/*public static final com.mojang.serialization.MapCodec<GT6DungeonLootModifier> CODEC =
			RecordCodecBuilder.mapCodec(aInst -> codecStart(aInst)
					.and(ResourceLocation.CODEC.fieldOf("table").forGetter(aModifier -> aModifier.mTable))
					.and(UniformInt.CODEC.fieldOf("rolls").forGetter(aModifier -> aModifier.mRolls))
					.and(ENTRY_CODEC.listOf().fieldOf("entries").forGetter(aModifier -> aModifier.mEntries))
					.apply(aInst, GT6DungeonLootModifier::new));
	*///?}

	/** The vanilla loot table id this modifier injects into (the verified spec-④ mapping). */
	final ResourceLocation mTable;

	/** The per-application roll count (the share-approximation, class doc). */
	final UniformInt mRolls;

	/** The weighted entries — the upstream rows in upstream order (the pin-test face). */
	final List<Entry> mEntries;

	public GT6DungeonLootModifier(LootItemCondition[] aConditions, ResourceLocation aTable,
			UniformInt aRolls, List<Entry> aEntries) {
		super(aConditions);
		mTable = aTable;
		mRolls = aRolls;
		mEntries = List.copyOf(aEntries);
	}

	@Override
	//? if forge {
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
	//?} else {
	/*public com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
	*///?}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
		ResourceLocation tQueried = aContext.getQueriedLootTableId();
		if (tQueried == null || !mTable.equals(tQueried) || mEntries.isEmpty()) return aLoot;
		int tTotal = 0;
		for (Entry tEntry : mEntries) tTotal += tEntry.weight();
		if (tTotal <= 0) return aLoot;
		int tRolls = mRolls.sample(aContext.getRandom());
		for (int tRoll = 0; tRoll < tRolls; tRoll++) {
			int tPick = aContext.getRandom().nextInt(tTotal), tAcc = 0;
			for (Entry tEntry : mEntries) {
				tAcc += tEntry.weight();
				if (tAcc > tPick) {
					// 21.1 renamed the no-arg Item face getDefaultMaxStackSize (the
					// IItemExtension overload takes the stack — the default is the loot-entry face)
					//? if forge {
					int tCap = Math.min(tEntry.item().getMaxStackSize(), 64);
					//?} else {
					/*int tCap = Math.min(tEntry.item().getDefaultMaxStackSize(), 64);
					*///?}
					int tMin = Math.min(tEntry.min(), tCap);
					int tMax = Math.min(Math.max(tEntry.max(), tMin), tCap); // the upstream :571 clamp
					int tCount = tMin + (tMin >= tMax ? 0 : aContext.getRandom().nextInt(tMax - tMin + 1));
					//? if forge {
					ItemStack tStack = new ItemStack(tEntry.item(), tCount);
					if (tEntry.tag() != null) tStack.setTag(tEntry.tag().copy()); // the task-p36 artifact lane (the store-as-full key)
					aLoot.add(tStack);
					//?} else {
					/*ItemStack tStack = new ItemStack(tEntry.item(), tCount);
					if (tEntry.tag() != null) tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
							net.minecraft.world.item.component.CustomData.of(tEntry.tag().copy()));
					aLoot.add(tStack);
					*///?}
					break;
				}
			}
		}
		return aLoot;
	}
}
