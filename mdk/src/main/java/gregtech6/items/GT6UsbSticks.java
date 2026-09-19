package gregtech6.items;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * The USB Stick data-storage family (task p32-usb-data) — the four data sticks of the
 * upstream MultiItemTechnological registrations (MultiItemTechnological.java:791-794,
 * item ids 32001-32004, "USB 1.0 Stick" .. "USB 4.0 Stick", tooltip "Stores Data"), the
 * port counterpart of {@code Behavior_DataStorage} (gregtech/items/behaviors/Behavior_DataStorage.
 * java:33-48) and its material-data NBT carrier.
 *
 * <p><b>The NBT carrier</b> — the keys the upstream scanner/replicator/computer faces share
 * (CS.java:1276/:1277/:1281 verbatim):
 * <ul>
 * <li>{@code gt.usb.tier} (BYTE) — the stick's data-handling tier, the
 *     {@code OD_USB_STICKS[1..4]} axis. The Molecular Scanner writes tier 3
 *     (RecipeMapScannerMolecular.java:60); the replicator gate reads
 *     {@code OD_USB_STICKS[3]} sticks only (RecipeMapReplicator.java:63).</li>
 * <li>{@code gt.usb.data} (COMPOUND) — the data payload. The scanner writes
 *     {@code gt.replicator.data} (SHORT, CS.java:1281) = the scanned material's id
 *     (RecipeMapScannerMolecular.java:59); the replicator reads the same pair back
 *     (RecipeMapReplicator.java:66/:82-83). {@link #writeMaterialData} is that scanner
 *     write-shape; {@link #readMaterialId}/{@link #materialOf} are the read legs.</li>
 * </ul>
 * The 1.7.10 write faces live in the machine RecipeMaps' runtime synthesis (the W2
 * consumer card calls these helpers); this card ships the ITEM-side carrier + the
 * tooltip face so the data plane is testable before any machine exists.
 *
 * <p><b>The tooltip face</b> (Behavior_DataStorage.java:37-48 verbatim): an untagged stick
 * shows "This Stick is Empty" (the LH.Chat.CYAN line); a tagged one rides
 * {@code UT.NBT.getDataToolTip} — the material branch (UT.java:2246-2267, the
 * all-details form the behavior passes {@code T} to): "Material Data", the
 * "Can be Replicated using" header with the Neutral/Charged (Anti)Matter p/n amounts and
 * the QU energy line {@code (p+n)*65536} for {@code TD.Processing.UUM} materials, the
 * "(Not Replicatable)" line otherwise — plus the "Data: USB &lt;tier&gt;.0" tier line.</p>
 *
 * <p>The creative-tab face is CUT (the GT6LubricantBucket ruling: /give + the crafting
 * rows cover the live faces; the tab walk is the creative-tab card's exclusive surface).
 * The upstream crafting rows (:796-799) ride the crafting card pool.</p>
 *
 * <p>KJS surface (the card's declaration): REGISTRATION face only; deferred to the KJS
 * binding card.</p>
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6UsbSticks {

	/** The tier key (upstream CS.java:1276 NBT_USB_TIER = "gt.usb.tier", BYTE). */
	public static final String NBT_USB_TIER = "gt.usb.tier";
	/** The data-compound key (upstream CS.java:1277 NBT_USB_DATA = "gt.usb.data"). */
	public static final String NBT_USB_DATA = "gt.usb.data";
	/** The material-id short inside the data compound (upstream CS.java:1281 NBT_REPLICATOR_DATA). */
	public static final String NBT_REPLICATOR_DATA = "gt.replicator.data";

	/** The scanner's write tier (RecipeMapScannerMolecular.java:60 — {@code (byte)3}). */
	public static final byte TIER_SCANNER_WRITE = 3;

	/** The self-contained registration listener (the GT6LubricantBucket shape, ADR-P3-4). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	/** The USB 1.0 Stick — the :791 row (OD_USB_STICKS[1], Behavior_DataStorage.INSTANCE). */
	public static final RegistryObject<Item> USB_STICK_1 = ITEMS.register("usb_stick_1",
			() -> new GT6UsbStickItem(new Item.Properties(), (byte)1));
	/** The USB 2.0 Stick — the :792 row (OD_USB_STICKS[2]). */
	public static final RegistryObject<Item> USB_STICK_2 = ITEMS.register("usb_stick_2",
			() -> new GT6UsbStickItem(new Item.Properties(), (byte)2));
	/** The USB 3.0 Stick — the :793 row (OD_USB_STICKS[3], the scanner/replicator gate tier). */
	public static final RegistryObject<Item> USB_STICK_3 = ITEMS.register("usb_stick_3",
			() -> new GT6UsbStickItem(new Item.Properties(), (byte)3));
	/** The USB 4.0 Stick — the :794 row (OD_USB_STICKS[4], IL.java:415). */
	public static final RegistryObject<Item> USB_STICK_4 = ITEMS.register("usb_stick_4",
			() -> new GT6UsbStickItem(new Item.Properties(), (byte)4));

	/** The tooltip key of a stick ("Stores Data", the :791-794 description column). */
	public static String tooltipKey(byte aTier) {
		return "item.gt6.usb_stick_" + aTier + ".tooltip";
	}

	// ---------------------------------------------------------------------------
	// the data carrier — the per-leg fork (the GT6BatteryItem shape:
	// 1.20.1 freeform stack tag, 21.1 the opaque CUSTOM_DATA envelope — same keys)
	// ---------------------------------------------------------------------------

	/** The scanner write face (RecipeMapScannerMolecular.java:58-60 shape): the material id short into gt.usb.data + the tier-3 byte. */
	public static void writeMaterialData(ItemStack aStack, OreDictMaterial aMaterial) {
		if (aStack.isEmpty() || aMaterial == null || aMaterial.mID < 1) return;
		CompoundTag tData = new CompoundTag();
		tData.putShort(NBT_REPLICATOR_DATA, aMaterial.mID);
		//? if forge {
		aStack.getOrCreateTag().put(NBT_USB_DATA, tData);
		aStack.getOrCreateTag().putByte(NBT_USB_TIER, TIER_SCANNER_WRITE);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.put(NBT_USB_DATA, tData);
		tTag.putByte(NBT_USB_TIER, TIER_SCANNER_WRITE);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
	}

	/** The tier byte, 0 when absent (the Behavior_DataStorage.java:42 read leg). */
	public static byte readTier(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag() ? aStack.getTag().getByte(NBT_USB_TIER) : 0;
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getByte(NBT_USB_TIER);
		 *///?}
	}

	/** The raw data compound, or null when the key is absent (the RecipeMapReplicator.java:66/:80 read leg). */
	@Nullable
	public static CompoundTag readData(ItemStack aStack) {
		//? if forge {
		if (!aStack.hasTag() || !aStack.getTag().contains(NBT_USB_DATA, net.minecraft.nbt.Tag.TAG_COMPOUND)) return null;
		return aStack.getTag().getCompound(NBT_USB_DATA);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		return tTag.contains(NBT_USB_DATA, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tTag.getCompound(NBT_USB_DATA) : null;
		 *///?}
	}

	/** The material-id short, 0 when absent (the RecipeMapReplicator.java:82 read leg). */
	public static short readMaterialId(ItemStack aStack) {
		CompoundTag tData = readData(aStack);
		return tData == null ? 0 : tData.getShort(NBT_REPLICATOR_DATA);
	}

	/** The material behind the carried id, or null (the RecipeMapReplicator.java:82-83 {@code tID > 0 && UT.Code.exists} gate verbatim over the raw array). */
	@Nullable
	public static OreDictMaterial materialOf(ItemStack aStack) {
		short tID = readMaterialId(aStack);
		if (tID <= 0) return null;
		return MaterialRegistry.INSTANCE.byID(tID);
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6LubricantBucket shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	private GT6UsbSticks() {}

	/**
	 * The stick item: the static "Stores Data" line + the behavior's data tooltip face.
	 */
	public static final class GT6UsbStickItem extends Item {

		/** The stick's tier (the OD_USB_STICKS index, the "Data: USB &lt;tier&gt;.0" face). */
		public final byte mTier;

		public GT6UsbStickItem(Properties aProperties, byte aTier) {
			super(aProperties);
			mTier = aTier;
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
			//21.1: the hover signature carries the Item.TooltipContext (vanilla 1.21.1 Item.java:468)
			//— the GT6Circuits fork shape.
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			addTooltipFace(aStack, aTooltip);
		}
		*///?}

		/** The Behavior_DataStorage.java:37-48 face: the "Stores Data" line, then the data state. */
		private static void addTooltipFace(ItemStack aStack, List<Component> aTooltip) {
			GT6UsbStickItem tItem = aStack.getItem() instanceof GT6UsbStickItem tStick ? tStick : null;
			if (tItem != null) aTooltip.add(Component.translatable(tooltipKey(tItem.mTier)));
			// the Behavior_DataStorage gate: an untagged stack reads "This Stick is Empty" (LH.Chat.CYAN),
			// a tagged one rides the getDataToolTip material face + the tier line (LH.Chat.DGRAY).
			if (readData(aStack) == null && readTier(aStack) == 0) {
				aTooltip.add(Component.literal("This Stick is Empty").withStyle(ChatFormatting.AQUA));
				return;
			}
			// the getDataToolTip material branch (UT.java:2246-2267, the all-details form) — the
			// upstream lines are hardcoded en (the 1.7.10 dump carries zero zh rows for them),
			// so the port keeps the literals verbatim and the lang surface stays at the 8 item keys.
			OreDictMaterial tMaterial = materialOf(aStack);
			if (tMaterial != null) {
				if (tMaterial.contains(TD.Processing.UUM)) {
					aTooltip.add(Component.literal("Material Data: ").withStyle(ChatFormatting.AQUA)
							.append(Component.literal(tMaterial.getLocal()).withStyle(ChatFormatting.WHITE)));
					aTooltip.add(Component.literal("Can be Replicated using").withStyle(ChatFormatting.AQUA));
					String tNeutral = tMaterial.contains(TD.Atomic.ANTIMATTER) ? "Neutral Antimatter" : "Neutral Matter";
					String tCharged = tMaterial.contains(TD.Atomic.ANTIMATTER) ? "Charged Antimatter" : "Charged Matter";
					aTooltip.add(Component.literal(tNeutral + ": ").withStyle(ChatFormatting.WHITE)
							.append(Component.literal("" + tMaterial.mNeutrons).withStyle(ChatFormatting.YELLOW)));
					aTooltip.add(Component.literal(tCharged + ": ").withStyle(ChatFormatting.WHITE)
							.append(Component.literal("" + tMaterial.mProtons).withStyle(ChatFormatting.RED)));
					aTooltip.add(Component.literal("Energy: ").withStyle(ChatFormatting.WHITE)
							.append(Component.literal((tMaterial.mNeutrons + tMaterial.mProtons) * 65536 + " "
									+ TD.Energy.QU.getLocalisedNameShort()).withStyle(ChatFormatting.AQUA)));
				} else {
					aTooltip.add(Component.literal("Material Data: ").withStyle(ChatFormatting.AQUA)
							.append(Component.literal(tMaterial.getLocal()).withStyle(ChatFormatting.WHITE))
							.append(Component.literal(" (Not Replicatable)").withStyle(ChatFormatting.GOLD)));
				}
			}
			aTooltip.add(Component.literal("Data: USB " + readTier(aStack) + ".0").withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
