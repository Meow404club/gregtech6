package gregtech6.items;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity;

/**
 * The Key item family (task dungeon-keys) — the ten material keys of the upstream
 * {@code IL.KEYS} pool (IL.java:516 Brass/Bronze/Copper/Gold/Iron/Lead/Plastic/Platinum/
 * Silver/Tin, the registrations MultiItemRandomTools.java:589-598, item ids 30000-30009,
 * display names "Iron Key" .. "Platinum Key", zh 铁钥匙 .. 塑料钥匙). The shelter dungeon
 * draws its five key STACKS from this pool per dungeon (WorldgenDungeonGT.java:169-173:
 * {@code IL.KEYS[aRandom.nextInt(IL.KEYS.length)]}), so the pool ships complete — cutting
 * it to five would be an arbitrary truncation of the draw table.
 *
 * <p><b>The key id carrier</b> (Behavior_Key.java:44-63 verbatim semantics): the long in
 * {@code gt.key} (the upstream CS.NBT_KEY — {@link GT6SafeKeyLockedBlockEntity#NBT_KEY}
 * shares it). A right-click on a Key Locked Safe through the storage use arm
 * ({@link gregtech6.registry.GT6StaticStorages}, the SAFE_KEYLOCKED arm):
 * <ul>
 * <li>a keyed stack plays its id through {@link GT6SafeKeyLockedBlockEntity#useKey} —
 *     a matching id flips the latch (upstream :51, the lock useKey :83-95);</li>
 * <li>a BLANK stack on an UNCLAIMED lock (mID 0) generates its id
 *     ({@code 1 + max(nextInt(1000000), System.nanoTime())}, Behavior_Key.java:54 verbatim)
 *     and claims the lock;</li>
 * <li>a BLANK stack on an OPEN, claimed lock clones its id (canCloneKey :94-96 —
 *     {@code mOpened && mID != 0}).</li>
 * </ul>
 *
 * <p><b>The dungeon face</b> (WorldgenDungeonGT.java:170-173): per dungeon the first id is
 * {@code 1 + max(draw, unique-cell-tag)} and the rest DESCEND ({@code tKeyIDs[i] =
 * tKeyIDs[i-1] - 1}); the stacks carry the display name "Key #1".."Key #5" (upstream
 * {@code getWithNameAndNBT(1, "Key #"+(i+1), ...)} — a hardcoded-en runtime rename, the
 * GT6UsbSticks literal precedent). {@link #dungeonStack} builds that stack; the id roll
 * lives in GT6DungeonStructure.generatePieces, the hiding in the GT6DungeonPiece chests.
 *
 * <p>The tooltip face (Behavior_Key.java:66-73): the "Can open certain regular Locks" line
 * (translatable, the dump row gt.behaviour.key 可以打开特定的锁) + the id line, hardcoded en
 * upstream — kept literal (the GT6UsbSticks literal precedent).
 *
 * <p>The creative-tab face: all ten keys join {@link GT6Tools#TOOLS_TAB} via
 * {@link #onBuildTabContents} (upstream keys are creative-visible MultiItem rows AND
 * craftable — the tab posture, not the /give posture).
 *
 * <p>KJS surface (the card's declaration, corrected from 5 to the upstream pool size):
 * REGISTRATION face, 10 new items; datapack-domain recipes naturally modifiable — the
 * upstream plate → 3 keys crafting rows (MultiItemRandomTools.java:589-598) ride a
 * crafting-domain card. KJS bindings stay tier-c deferred.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Keys {

	/** The self-contained registration listener (the GT6UsbSticks shape, ADR-P3-4). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	// The ten keys in IL.KEYS order (IL.java:516) — the pool the dungeon draw walks.
	/** The :594 Brass Key row (30005). */
	public static final RegistryObject<Item> KEY_BRASS = ITEMS.register("key_brass", () -> new GT6KeyItem(new Item.Properties()));
	/** The :593 Bronze Key row (30004). */
	public static final RegistryObject<Item> KEY_BRONZE = ITEMS.register("key_bronze", () -> new GT6KeyItem(new Item.Properties()));
	/** The :591 Copper Key row (30002). */
	public static final RegistryObject<Item> KEY_COPPER = ITEMS.register("key_copper", () -> new GT6KeyItem(new Item.Properties()));
	/** The :590 Gold Key row (30001). */
	public static final RegistryObject<Item> KEY_GOLD = ITEMS.register("key_gold", () -> new GT6KeyItem(new Item.Properties()));
	/** The :589 Iron Key row (30000). */
	public static final RegistryObject<Item> KEY_IRON = ITEMS.register("key_iron", () -> new GT6KeyItem(new Item.Properties()));
	/** The :597 Lead Key row (30008). */
	public static final RegistryObject<Item> KEY_LEAD = ITEMS.register("key_lead", () -> new GT6KeyItem(new Item.Properties()));
	/** The :598 Plastic Key row (30009). */
	public static final RegistryObject<Item> KEY_PLASTIC = ITEMS.register("key_plastic", () -> new GT6KeyItem(new Item.Properties()));
	/** The :596 Platinum Key row (30007). */
	public static final RegistryObject<Item> KEY_PLATINUM = ITEMS.register("key_platinum", () -> new GT6KeyItem(new Item.Properties()));
	/** The :595 Silver Key row (30006). */
	public static final RegistryObject<Item> KEY_SILVER = ITEMS.register("key_silver", () -> new GT6KeyItem(new Item.Properties()));
	/** The :592 Tin Key row (30003). */
	public static final RegistryObject<Item> KEY_TIN = ITEMS.register("key_tin", () -> new GT6KeyItem(new Item.Properties()));

	/** The dungeon draw pool, the IL.KEYS order verbatim (WorldgenDungeonGT.java:173 walks this table). */
	public static final List<RegistryObject<Item>> KEYS = List.of(
			KEY_BRASS, KEY_BRONZE, KEY_COPPER, KEY_GOLD, KEY_IRON, KEY_LEAD, KEY_PLASTIC, KEY_PLATINUM, KEY_SILVER, KEY_TIN);

	/** The keys per dungeon (WorldgenDungeonGT.java:161 {@code new boolean[5]} — five stacks). */
	public static final int KEYS_PER_DUNGEON = 5;

	/** The tooltip key of the family (the Behavior_Key "Can open certain regular Locks" line). */
	public static final String TOOLTIP_KEY = "item.gt6.key.tooltip";

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6UsbSticks shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/** The tab walk (the GT6UsbSticks.onBuildTabContents form) — the ten keys join the tools tab. */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GT6Tools.TOOLS_TAB.getId())) {
			for (RegistryObject<Item> tKey : KEYS) {
				aEvent.accept(new ItemStack(tKey.get()));
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the id carrier — the per-leg fork (the GT6UsbStickItem shape)
	// ---------------------------------------------------------------------------

	/** The key id long, 0 when absent (the Behavior_Key.java:50 read leg). */
	public static long keyIdOf(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag() ? aStack.getTag().getLong(GT6SafeKeyLockedBlockEntity.NBT_KEY) : 0;
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong(GT6SafeKeyLockedBlockEntity.NBT_KEY);
		 *///?}
	}

	/** Writes the key id (the Behavior_Key.java:55/:59 write leg). */
	public static void setKeyId(ItemStack aStack, long aId) {
		//? if forge {
		aStack.getOrCreateTag().putLong(GT6SafeKeyLockedBlockEntity.NBT_KEY, aId);
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putLong(GT6SafeKeyLockedBlockEntity.NBT_KEY, aId);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
	}

	/** The fresh-key id roll (Behavior_Key.java:54 verbatim — {@code 1+max(RNGSUS.nextInt(1000000), System.nanoTime())}). */
	public static long newKeyId() {
		return 1 + Math.max(ThreadLocalRandom.current().nextInt(1000000), System.nanoTime());
	}

	/**
	 * The dungeon key stack (WorldgenDungeonGT.java:173): one item of the drawn type, the
	 * id NBT, the "Key #&lt;n&gt;" display name (the upstream hardcoded-en rename — kept
	 * literal, the GT6UsbSticks literal precedent).
	 */
	public static ItemStack dungeonStack(Item aType, int aIndex, long aKeyId) {
		ItemStack rStack = new ItemStack(aType);
		setKeyId(rStack, aKeyId);
		//? if forge {
		rStack.setHoverName(Component.literal("Key #" + (aIndex + 1)));
		//?} else {
		/*rStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Key #" + (aIndex + 1)));
		//21.1: the hover-name setter died with the 1.20.5 component move (ItemStack.java CUSTOM_NAME).
		*///?}
		return rStack;
	}

	/**
	 * The Behavior_Key.java:44-63 use face over the ported lock — a keyed stack plays its
	 * id through {@link GT6SafeKeyLockedBlockEntity#useKey}; a blank one CLAIMS an unclaimed
	 * lock with a fresh id (:53-57); a blank one CLONES the id of an open claimed lock
	 * (:58-61, the canCloneKey :94-96 gate). False when nothing applied.
	 */
	public static boolean useOnKeyLocked(GT6SafeKeyLockedBlockEntity aLock, ItemStack aKeyStack) {
		long tKeyId = keyIdOf(aKeyStack);
		if (tKeyId != 0) return aLock.useKey(tKeyId);
		tKeyId = aLock.getKeyID();
		if (tKeyId == 0) {
			tKeyId = newKeyId();
			setKeyId(aKeyStack, tKeyId);
			return aLock.useKey(tKeyId);
		}
		if (aLock.canCloneKey()) {
			setKeyId(aKeyStack, tKeyId);
			return true;
		}
		return false;
	}

	private GT6Keys() {}

	/**
	 * The key item: the behavior tooltip face (the Behavior_Key.getAdditionalToolTips
	 * :69-73 port).
	 */
	public static final class GT6KeyItem extends Item {

		public GT6KeyItem(Properties aProperties) {
			super(aProperties);
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			addTooltipFace(aStack, aTooltip);
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			addTooltipFace(aStack, aTooltip);
		}
		*///?}

		/** The Behavior_Key.java:69-73 face: the behavior line, then the id state. */
		private static void addTooltipFace(ItemStack aStack, List<Component> aTooltip) {
			aTooltip.add(Component.translatable(TOOLTIP_KEY));
			long tId = keyIdOf(aStack);
			if (tId != 0) {
				aTooltip.add(Component.literal("Key ID: " + tId));
			} else {
				aTooltip.add(Component.literal("*BLANK*").withStyle(ChatFormatting.GRAY));
			}
		}
	}
}
