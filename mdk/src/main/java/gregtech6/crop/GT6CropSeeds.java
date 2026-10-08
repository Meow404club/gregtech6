package gregtech6.crop;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The crop seed item — task cbc-3-crop-data-assets, the port of IC2's seed carrier
 * (1.12 decompiled ItemCropSeed.generateItemStackFromValues :110-121 via
 * TileEntityCrop.generateSeeds :895-897) as ONE self-owned item whose payload says which of the
 * {@link GT6CropCards} rows it grows and at which stats. {@link CropTileView#generateSeeds} and
 * {@link CropMath#pickSeed} route here (cbc-2's contract: "the seed item itself is cbc-3's face").
 *
 * <p>Payload keys — NBT on the 1.20.1 leg, the same keys inside the opaque vanilla
 * {@code DataComponents.CUSTOM_DATA} envelope on the 1.21.1 leg (the GT6DataComponents carrier
 * law: only the envelope changes, the keys stay byte-identical): the stat bytes
 * {@code G}/{@code Ga}/{@code Re}/{@code scanLevel} (the 1.7.10 task-card law) plus the identity
 * pair {@code cropOwner}/{@code cropId} (the 1.12 TileEntityCrop read face :96-97, the same keys
 * the cbc-1 crop-stick BE writes). Bytes, not ints, per ItemCropSeed :115-118.
 *
 * <p>The item model reuses the family crop-stick sprite (gt6:item/crop_stick — the same borrow
 * cbc-1 seated; IC2 itself renders ItemCrop with the crop_stick texture, ItemCrop.java:27);
 * the datagen model line lives in GT6ItemModels. Creative seat: the crop domain's
 * "GregTech: Crops" tab ({@code gt6:crops}, task crop-creative-tab — the tab walk emits
 * one fully-scanned representative seed per card).
 *
 * <p>KJS face: REGISTRATION face only, deferred to the KJS binding card.
 * RCON face: none. Viewer face: no machine diagram, zero JEI/EMI recipe surfaces — the
 * tooltip IS the scan-information disclosure ({@link #appendSeedTooltip}, the
 * ItemCropSeed.addInformation :63-75 port); Jade face: another card, not this wave.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CropSeeds {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The payload keys (see class doc for the line anchors). */
	public static final String TAG_OWNER = "cropOwner", TAG_CROP = "cropId",
			TAG_GROWTH = "G", TAG_GAIN = "Ga", TAG_RESISTANCE = "Re", TAG_SCAN = "scanLevel";

	/** The owner stamp — the single-owner port (the cbc-1 GT6CropBlockEntity cropOwner face). */
	public static final String OWNER = "gt6";

	/** The seed stack stats — one byte per ICropSeed stat face. */
	public record SeedData(String cropOwner, String cropId, int growth, int gain, int resistance, int scanLevel) {}

	/**
	 * The seed item, id gt6:crop_seed — the tooltip face rides it (the port of
	 * ItemCropSeed.addInformation :63-75: scanLevel &gt;= 4 discloses the three stat
	 * bytes, scanLevel &gt;= 1 names the crop through the cbc-3 {@code gt.crop.<name>}
	 * keys; below 1 the payload is undisclosed, scan with the Cropnalyzer); the creative
	 * seat rides the crop domain's "GregTech: Crops" tab ({@code gt6:crops}, task
	 * crop-creative-tab — the GT6CropSticks.walkDisplayItems emission).
	 */
	public static final RegistryObject<Item> CROP_SEED = ITEMS.register("crop_seed", () -> new Item(new Item.Properties()) {
		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aLevel,
			 java.util.List<net.minecraft.network.chat.Component> aTooltip, TooltipFlag aFlag) {
			appendSeedTooltip(aStack, aTooltip);
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext,
			 java.util.List<net.minecraft.network.chat.Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the hover signature carries the Item.TooltipContext (the GT6Foods.GT6FoodItem fork).
			appendSeedTooltip(aStack, aTooltip);
		}
		 *///?}
	});

	/**
	 * The scan disclosure — ItemCropSeed.addInformation :63-75: the stat lines only at
	 * scan 4+ (the Cropnalyzer face, cbc-4), the crop name line from scan 1 (the
	 * {@code gt.crop.<name>} key family, cbc-3's lang four-landing).
	 */
	static void appendSeedTooltip(ItemStack aStack, java.util.List<net.minecraft.network.chat.Component> aTooltip) {
		SeedData tData = readSeed(aStack);
		if (tData == null) return;
		if (tData.scanLevel() >= 1) {
			aTooltip.add(net.minecraft.network.chat.Component.translatable("gt.crop." + tData.cropId())
					.withStyle(net.minecraft.ChatFormatting.GRAY));
		}
		if (tData.scanLevel() >= 4) {
			aTooltip.add(net.minecraft.network.chat.Component.literal("Gr ")
					.withStyle(net.minecraft.ChatFormatting.DARK_GREEN)
					.append(net.minecraft.network.chat.Component.literal(String.valueOf(tData.growth()))
							.withStyle(net.minecraft.ChatFormatting.GRAY)));
			aTooltip.add(net.minecraft.network.chat.Component.literal("Ga ")
					.withStyle(net.minecraft.ChatFormatting.GOLD)
					.append(net.minecraft.network.chat.Component.literal(String.valueOf(tData.gain()))
							.withStyle(net.minecraft.ChatFormatting.GRAY)));
			aTooltip.add(net.minecraft.network.chat.Component.literal("Re ")
					.withStyle(net.minecraft.ChatFormatting.AQUA)
					.append(net.minecraft.network.chat.Component.literal(String.valueOf(tData.resistance()))
							.withStyle(net.minecraft.ChatFormatting.GRAY)));
		}
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Foods shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	private GT6CropSeeds() {}

	/**
	 * Port of TileEntityCrop.generateSeeds :895-897 → ItemCropSeed.generateItemStackFromValues
	 * :110-121 (the delegate body {@link CropTileView#generateSeeds} points at).
	 */
	public static ItemStack generateSeeds(GT6CropCard aCard, int aGrowth, int aGain, int aResistance, int aScan) {
		ItemStack rStack = new ItemStack(CROP_SEED.get());
		writeSeed(rStack, OWNER, aCard.name(), aGrowth, aGain, aResistance, aScan);
		return rStack;
	}

	/**
	 * The registry-tolerant face of {@link #generateSeeds} -- null while the gt6:crop_seed item
	 * is unbound (the offline legs, where the card's interim base-seed face answers instead).
	 * The merge-state wiring ({@link GT6CropCard#seedStack}) routes picked seeds here LIVE so a
	 * picked seed carries its G/Ga/Re/scan payload (the ItemCropSeed :110-121 law).
	 */
	@Nullable
	public static ItemStack tryGenerate(GT6CropCard aCard, int aGrowth, int aGain, int aResistance, int aScan) {
		// the bound check rides the holder face — RegistryObject.get()/DeferredHolder.get()
		// THROW on an unbound registry, so the former get()==null dead check never answered
		// the documented offline fallback (exposed by the cbc-6 end-to-end driver on the bare
		// forge JVM; the neo FML test JVM binds live and skips this arm).
		//? if forge {
		if (!CROP_SEED.isPresent()) return null;
		//?} else {
		/*if (!CROP_SEED.isBound()) return null;
		 *///?}
		Item tItem = CROP_SEED.get();
		ItemStack rStack = new ItemStack(tItem);
		writeSeed(rStack, OWNER, aCard.name(), aGrowth, aGain, aResistance, aScan);
		return rStack;
	}

	/** The stat writer — both legs share the put body, only the carrier envelope differs. */
	public static void writeSeed(ItemStack aStack, String aOwner, String aCropId,
			int aGrowth, int aGain, int aResistance, int aScan) {
		//? if forge {
		CompoundTag tTag = aStack.getOrCreateTag();
		putSeed(tTag, aOwner, aCropId, aGrowth, aGain, aResistance, aScan);
		//?} else {
		/*net.minecraft.world.item.component.CustomData tData = aStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		CompoundTag tTag = putSeed(tData == null ? new CompoundTag() : tData.copyTag(), aOwner, aCropId, aGrowth, aGain, aResistance, aScan);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
	}

	/** The shared put body (bytes per ItemCropSeed :115-118). */
	private static CompoundTag putSeed(CompoundTag aTag, String aOwner, String aCropId,
			int aGrowth, int aGain, int aResistance, int aScan) {
		aTag.putString(TAG_OWNER, aOwner);
		aTag.putString(TAG_CROP, aCropId);
		aTag.putByte(TAG_GROWTH, (byte)aGrowth);
		aTag.putByte(TAG_GAIN, (byte)aGain);
		aTag.putByte(TAG_RESISTANCE, (byte)aResistance);
		aTag.putByte(TAG_SCAN, (byte)aScan);
		return aTag;
	}

	/** The payload reader — null on an untagged stack; the upstream face guards the KEYS only, not the item type (ItemCropSeed.getCropFromStack :125-132). */
	@Nullable
	public static SeedData readSeed(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		//? if forge {
		CompoundTag tTag = aStack.getTag();
		//?} else {
		/*net.minecraft.world.item.component.CustomData tData = aStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		CompoundTag tTag = tData == null ? null : tData.copyTag();
		 *///?}
		if (tTag == null || !tTag.contains(TAG_OWNER) || !tTag.contains(TAG_CROP)) return null;
		return new SeedData(tTag.getString(TAG_OWNER), tTag.getString(TAG_CROP),
				tTag.getByte(TAG_GROWTH), tTag.getByte(TAG_GAIN), tTag.getByte(TAG_RESISTANCE), tTag.getByte(TAG_SCAN));
	}

	/** The card of a seed stack — the GT6Crops registration-order lookup (60 entries, linear is the honest walk). */
	@Nullable
	public static GT6CropCard cropOf(ItemStack aStack) {
		SeedData tData = readSeed(aStack);
		if (tData == null || !OWNER.equals(tData.cropOwner())) return null;
		for (GT6CropCard tCard : GT6Crops.crops()) {
			if (tCard.name().equals(tData.cropId())) return tCard;
		}
		return null;
	}
}
