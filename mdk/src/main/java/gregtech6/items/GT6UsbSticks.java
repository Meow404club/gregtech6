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

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
import gregtech6.registry.GTMachines;

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
 * <p>The creative-tab face (task p38-tabfix-d-ruling, the user ruling): all four sticks
 * join MACHINES_TAB via {@link #onBuildTabContents} — supersedes the old CUT declaration
 * (the /give posture); the crafting rows (:796-799) still ride the crafting card pool.</p>
 *
 * <p>Task p37-usb-peripherals extends the family with the 8 USB peripheral rows of the
 * same upstream registration block: USB Cable 1-4 (:803-806, ids 32011-32014, NO
 * behavior — the static "Replaces USB Sticks when connected to USB Ports" line is the
 * whole face) and USB HDD 1-4 (:814-817, ids 32021-32024, the
 * {@code Behavior_DataStorage16} tooltip face — the 16-slot {@code gt.usb.drive}
 * compound). The HDD crafting rows' 'L' column pulls the Helium Laser Emitter in under
 * the coordinator's single-item exemption (GT6LaserGas.COMP_LASER_GAS_HE).</p>
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
	/** The 16-slot drive-compound key (upstream CS.java:1278 NBT_USB_DRIVE = "gt.usb.drive", the Behavior_DataStorage16 face). */
	public static final String NBT_USB_DRIVE = "gt.usb.drive";
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

	// ---------------------------------------------------------------------------
	// task p37-usb-peripherals — the USB Cable family, upstream
	// MultiItemTechnological.java:803-806 (item ids 32011-32014, "USB 1.0 Cable" ..
	// "USB 4.0 Cable", tooltip "Replaces USB Sticks when connected to USB Ports",
	// OD_USB_CABLES[1..4] = gt:usbcable1..4, CS.java:162). NO Behavior face — the
	// cables carry data BETWEEN ports (the upstream rows attach no behavior), so the
	// static tooltip line is the whole item face (the GT6LaserGasItem shape).
	// ---------------------------------------------------------------------------

	/** The USB 1.0 Cable — the :803 row (id 32011, OD_USB_CABLES[1]). */
	public static final RegistryObject<Item> USB_CABLE_1 = ITEMS.register("usb_cable_1",
			() -> new GT6LaserGas.GT6LaserGasItem(new Item.Properties(), cableTooltipKey((byte)1)));
	/** The USB 2.0 Cable — the :804 row (id 32012, OD_USB_CABLES[2]). */
	public static final RegistryObject<Item> USB_CABLE_2 = ITEMS.register("usb_cable_2",
			() -> new GT6LaserGas.GT6LaserGasItem(new Item.Properties(), cableTooltipKey((byte)2)));
	/** The USB 3.0 Cable — the :805 row (id 32013, OD_USB_CABLES[3]). */
	public static final RegistryObject<Item> USB_CABLE_3 = ITEMS.register("usb_cable_3",
			() -> new GT6LaserGas.GT6LaserGasItem(new Item.Properties(), cableTooltipKey((byte)3)));
	/** The USB 4.0 Cable — the :806 row (id 32014, OD_USB_CABLES[4]). */
	public static final RegistryObject<Item> USB_CABLE_4 = ITEMS.register("usb_cable_4",
			() -> new GT6LaserGas.GT6LaserGasItem(new Item.Properties(), cableTooltipKey((byte)4)));

	// ---------------------------------------------------------------------------
	// task p37-usb-peripherals — the USB HDD family, upstream
	// MultiItemTechnological.java:814-817 (item ids 32021-32024, "USB 1.0 HDD" ..
	// "USB 4.0 HDD", tooltip "Stores up to 16 Files at once", OD_USB_DRIVES[1..4] =
	// gt:usbdrive1..4, CS.java:164, Behavior_DataStorage16.INSTANCE).
	// ---------------------------------------------------------------------------

	/** The USB 1.0 HDD — the :814 row (id 32021, OD_USB_DRIVES[1]). */
	public static final RegistryObject<Item> USB_DRIVE_1 = ITEMS.register("usb_drive_1",
			() -> new GT6UsbDriveItem(new Item.Properties(), (byte)1));
	/** The USB 2.0 HDD — the :815 row (id 32022, OD_USB_DRIVES[2]). */
	public static final RegistryObject<Item> USB_DRIVE_2 = ITEMS.register("usb_drive_2",
			() -> new GT6UsbDriveItem(new Item.Properties(), (byte)2));
	/** The USB 3.0 HDD — the :816 row (id 32023, OD_USB_DRIVES[3]). */
	public static final RegistryObject<Item> USB_DRIVE_3 = ITEMS.register("usb_drive_3",
			() -> new GT6UsbDriveItem(new Item.Properties(), (byte)3));
	/** The USB 4.0 HDD — the :817 row (id 32024, OD_USB_DRIVES[4]). */
	public static final RegistryObject<Item> USB_DRIVE_4 = ITEMS.register("usb_drive_4",
			() -> new GT6UsbDriveItem(new Item.Properties(), (byte)4));

	/** The tooltip key of a stick ("Stores Data", the :791-794 description column). */
	public static String tooltipKey(byte aTier) {
		return "item.gt6.usb_stick_" + aTier + ".tooltip";
	}

	/** The tooltip key of a cable ("Replaces USB Sticks when connected to USB Ports", the :803-806 description column). */
	public static String cableTooltipKey(byte aTier) {
		return "item.gt6.usb_cable_" + aTier + ".tooltip";
	}

	/** The tooltip key of a drive ("Stores up to 16 Files at once", the :814-817 description column). */
	public static String driveTooltipKey(byte aTier) {
		return "item.gt6.usb_drive_" + aTier + ".tooltip";
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

	/**
	 * The 16-slot drive compound of a HDD stack, or null when the key is absent (the
	 * Behavior_DataStorage16.java:39-40 read leg over NBT_USB_DRIVE; the W2 consumer
	 * cards write slots {@code gt.usb.data0..15} into this compound).
	 */
	@Nullable
	public static CompoundTag readDrive(ItemStack aStack) {
		//? if forge {
		if (!aStack.hasTag() || !aStack.getTag().contains(NBT_USB_DRIVE, net.minecraft.nbt.Tag.TAG_COMPOUND)) return null;
		return aStack.getTag().getCompound(NBT_USB_DRIVE);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		return tTag.contains(NBT_USB_DRIVE, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tTag.getCompound(NBT_USB_DRIVE) : null;
		 *///?}
	}

	/** The drive write face: the whole 16-slot compound under NBT_USB_DRIVE (the consumer-side mirror of {@link #readDrive}). */
	public static void writeDrive(ItemStack aStack, CompoundTag aDrive) {
		//? if forge {
		aStack.getOrCreateTag().put(NBT_USB_DRIVE, aDrive);
		//?} else {
		/*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.put(NBT_USB_DRIVE, aDrive);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
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

	/**
	 * The tab walk (task p38-tabfix-d-ruling — all four sticks join the machines tab; the
	 * GT6BurningBoxes.onBuildTabContents verbatim form, the class-level MOD-bus
	 * {@code @Mod.EventBusSubscriber} at the class head is what delivers this handler).
	 * COUNT ERRATUM: the census/card said 3, the file registers 4 — the evidence window
	 * (:83-89) truncated at USB_STICK_3, USB_STICK_4 (:92) is the row it lost.
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(USB_STICK_1.get()));
			aEvent.accept(new ItemStack(USB_STICK_2.get()));
			aEvent.accept(new ItemStack(USB_STICK_3.get()));
			aEvent.accept(new ItemStack(USB_STICK_4.get()));
			// task p37-usb-peripherals — the 8 peripheral rows join the same tab (the
			// upstream rows all hang on the Technological items tab, the pooled join).
			aEvent.accept(new ItemStack(USB_CABLE_1.get()));
			aEvent.accept(new ItemStack(USB_CABLE_2.get()));
			aEvent.accept(new ItemStack(USB_CABLE_3.get()));
			aEvent.accept(new ItemStack(USB_CABLE_4.get()));
			aEvent.accept(new ItemStack(USB_DRIVE_1.get()));
			aEvent.accept(new ItemStack(USB_DRIVE_2.get()));
			aEvent.accept(new ItemStack(USB_DRIVE_3.get()));
			aEvent.accept(new ItemStack(USB_DRIVE_4.get()));
		}
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

	/**
	 * The drive item (task p37-usb-peripherals): the static "Stores up to 16 Files at
	 * once" line + the {@code Behavior_DataStorage16} face (upstream
	 * Behavior_DataStorage16.java:37-58, field-for-field):
	 *
	 * <ul>
	 * <li>no {@code gt.usb.drive} key → "Perfectly Formatted" (the LH.Chat.CYAN line,
	 *     :54);</li>
	 * <li>the drive compound present but empty → "Uncleanly Formatted" (:42);</li>
	 * <li>otherwise 16 slots, key {@code gt.usb.data}<i>i</i> (:45, the NBT_USB_DATA +
	 *     i concatenation): an empty slot reads "Data Slot i is Empty" (DGRAY, :47), a
	 *     filled one rides {@code UT.NBT.getDataToolTip(tUSB, aList, F)} (:49) — the
	 *     SHORT material form (UT.java:2260: "Mat Data: name (n/p/QU)" for UUM
	 *     materials, the "Material Data: name (Not Replicatable)" line otherwise) —
	 *     the SAME hardcoded-en posture as the stick face.</li>
	 * </ul>
	 */
	public static final class GT6UsbDriveItem extends Item {

		/** The drive's tier (the OD_USB_DRIVES index, the tooltip-key face). */
		public final byte mTier;

		public GT6UsbDriveItem(Properties aProperties, byte aTier) {
			super(aProperties);
			mTier = aTier;
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			addDriveTooltipFace(aStack, aTooltip);
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			//21.1: the hover signature carries the Item.TooltipContext (the GT6UsbStickItem fork shape).
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			addDriveTooltipFace(aStack, aTooltip);
		}
		*///?}

		/** The Behavior_DataStorage16.java:37-58 face: the static line, then the drive format state. */
		private static void addDriveTooltipFace(ItemStack aStack, List<Component> aTooltip) {
			GT6UsbDriveItem tItem = aStack.getItem() instanceof GT6UsbDriveItem tDrive ? tDrive : null;
			if (tItem != null) aTooltip.add(Component.translatable(driveTooltipKey(tItem.mTier)));
			CompoundTag tDrive = readDrive(aStack);
			if (tDrive == null) {
				// the :53-55 else branch — no drive key at all.
				aTooltip.add(Component.literal("Perfectly Formatted").withStyle(ChatFormatting.AQUA));
				return;
			}
			if (tDrive.isEmpty()) {
				// the :41-42 branch — the key exists but carries nothing.
				aTooltip.add(Component.literal("Uncleanly Formatted").withStyle(ChatFormatting.AQUA));
				return;
			}
			for (byte i = 0; i < 16; i++) {
				CompoundTag tSlot = tDrive.getCompound(NBT_USB_DATA + i);
				if (tSlot.isEmpty()) {
					aTooltip.add(Component.literal("Data Slot " + i + " is Empty").withStyle(ChatFormatting.DARK_GRAY));
				} else {
					addShortMaterialFace(tSlot, aTooltip);
				}
			}
		}

		/** The UT.NBT.getDataToolTip(.., F) short material form (UT.java:2260, the NBT_REPLICATOR_DATA branch). */
		private static void addShortMaterialFace(CompoundTag aSlot, List<Component> aTooltip) {
			short tID = aSlot.getShort(NBT_REPLICATOR_DATA);
			OreDictMaterial tMaterial = tID > 0 ? MaterialRegistry.INSTANCE.byID(tID) : null;
			if (tMaterial == null) return; // the Code.exists gate (UT.java:2258) — a non-material slot renders nothing
			if (tMaterial.contains(TD.Processing.UUM)) {
				aTooltip.add(Component.literal("Mat Data: ").withStyle(ChatFormatting.AQUA)
						.append(Component.literal(tMaterial.getLocal()).withStyle(ChatFormatting.WHITE))
						.append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
						.append(Component.literal("" + tMaterial.mNeutrons).withStyle(ChatFormatting.YELLOW))
						.append(Component.literal("/").withStyle(ChatFormatting.WHITE))
						.append(Component.literal("" + tMaterial.mProtons).withStyle(ChatFormatting.RED))
						.append(Component.literal("/").withStyle(ChatFormatting.WHITE))
						.append(Component.literal((tMaterial.mNeutrons + tMaterial.mProtons) * 65536 + " "
								+ TD.Energy.QU.getLocalisedNameShort()).withStyle(ChatFormatting.AQUA))
						.append(Component.literal(")").withStyle(ChatFormatting.WHITE)));
			} else {
				aTooltip.add(Component.literal("Material Data: ").withStyle(ChatFormatting.AQUA)
						.append(Component.literal(tMaterial.getLocal()).withStyle(ChatFormatting.WHITE))
						.append(Component.literal(" (Not Replicatable)").withStyle(ChatFormatting.GOLD)));
			}
		}
	}
}
