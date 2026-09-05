package gregtech6.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
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

/**
 * The Integrated Circuit carrier (task p16-distillery-family ①) — the port counterpart of
 * upstream {@code ItemIntegratedCircuit} (gregapi/item/ItemIntegratedCircuit.java:48-52,
 * registered as {@code IL.Circuit_Selector}, GT_API.java:731), the "Selector Tag" item every
 * sub-recipe-selecting row carries in its input slot ({@code ST.tag(n)},
 * ST.java:779-781 = {@code IL.Circuit_Selector.getWithDamage(0, n)}).
 *
 * <p><b>Carrier shape</b> (the p14 research verdict, docs/TODO.md:218): ONE item, the
 * configuration number riding the vanilla 1.20.1 {@code Damage} NBT key —
 * {@code ItemStack.getDamageValue()} reads {@code tag.getInt("Damage")} and
 * {@code setDamageValue} writes it on ANY item regardless of damageability
 * (vanilla 1.20.1 ItemStack.java:291-297), which is the mechanical map of the upstream
 * {@code setHasSubtypes(T) + setMaxDamage(0)} "damage is free data" idiom (1.7.10 has no
 * meta axis in 1.20.1, the item is NOT damageable — maxDamage stays at the default 0 so the
 * stack keeps upstream setMaxDamage(0) semantics: stackable, no durability bar, no vanilla
 * damage predicates).
 *
 * <p><b>Recipe semantics</b> (upstream ST.java:779-781 + Recipe.java:780-781): the upstream
 * recipe input is the circuit at STACK SIZE 0 — never consumed ({@code stackSize -= 0}).
 * Size-0 is NOT portable to 1.20.1 ({@code ItemStack.isEmpty()} is {@code count <= 0}, so a
 * size-0 stack cannot live in an IItemHandler slot and is stripped from recipe inputs), so
 * the port carries recipe inputs at count 1 and the consume step skips the circuit stack BY
 * ITEM IDENTITY (Recipe.checkStacksEqual, the research-card required deviation) — the net
 * effect equals the upstream minus-zero. Matching is exact tag equality through
 * {@code Recipe.isSameItemAndTag} (the recipe input carries the {@code Damage} tag, so the
 * configuration number routes the linear scan the way the upstream item-hash buckets do —
 * the Chem.java:333-vs-:346 selector distinction).
 *
 * <p><b>Cut against upstream (declared)</b>: the 25 crafting rows
 * (ItemIntegratedCircuit.java:58-85) and the CoverSelectorTag binding (:87) ride the
 * circuit-programming and cover pools; the 256-icon damage ladder (:90-118) is cut to ONE
 * model + the configuration tooltip (the research-card icon deviation); the creative-tab
 * surface is the machines tab (upstream files the Selector Tag under its general item
 * category — the port has no gregapi items tab, the machines tab is the nearest live
 * category, GTMachines.MACHINES_TAB).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Circuits {

	/** The vanilla ItemStack damage key — the configuration number's carrier (vanilla 1.20.1 ItemStack.java:292). */
	public static final String TAG_CONFIGURATION = "Damage";

	/**
	 * The item registration, id {@code gt6:integrated_circuit} (upstream
	 * {@code "gt.integrated_circuit"}, ItemIntegratedCircuit.java:50 — the lowercase
	 * registry-path convention, the 1.20.1 ResourceLocation constraint). Self-contained
	 * listener shape (ADR-P3-4, the GT6Tools.java:57-88 precedent).
	 */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	public static final RegistryObject<Item> INTEGRATED_CIRCUIT = ITEMS.register("integrated_circuit",
			() -> new IntegratedCircuitItem(new Item.Properties()));

	/** The tooltip lang key (GT6EnUs pins "Configuration: %s" — the upstream LH "Configuration: " line, :54/:100). */
	public static final String TOOLTIP_KEY = "item.gt6.integrated_circuit.configuration";

	/**
	 * The circuit item body: the configuration tooltip replaces the upstream 256-icon
	 * damage ladder (ItemIntegratedCircuit.java:98-101 addAdditionalToolTips —
	 * "Configuration: " + the number; the icon layers are the declared cut).
	 */
	public static final class IntegratedCircuitItem extends Item {

		public IntegratedCircuitItem(Properties aProperties) {
			super(aProperties);
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			aTooltip.add(Component.translatable(GT6Circuits.TOOLTIP_KEY, configurationOf(aStack))); // upstream :100
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			//21.1: the hover signature carries the Item.TooltipContext (vanilla 1.21.1 Item.java:468)
			//— the GTBasicMachineBlock.use fork shape.
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			aTooltip.add(Component.translatable(GT6Circuits.TOOLTIP_KEY, configurationOf(aStack))); // upstream :100
		}
		*///?}
	}

	/**
	 * The upstream {@code ST.tag(n)} shape (ST.java:779-781) — a count-1 circuit stack with
	 * configuration {@code aConfig} riding the {@code Damage} key. The item travels as a
	 * parameter so the offline tests can drive the helper against a fixture instance (the
	 * live callers pass {@link #INTEGRATED_CIRCUIT}{{@code .get()}}).
	 */
	public static ItemStack selector(Item aCircuit, int aConfig) {
		ItemStack rStack = new ItemStack(aCircuit, 1); // count 1 — the size-0 upstream form is not portable, see the class doc
		applyConfiguration(rStack, Math.max(0, aConfig));
		return rStack;
	}

	/** The live-item form of {@link #selector} (config on the registered circuit). */
	public static ItemStack selector(int aConfig) {
		return selector(INTEGRATED_CIRCUIT.get(), aConfig);
	}

	/** Writes the configuration payload (see {@link #configurationOf} for the two carriers). */
	private static void applyConfiguration(ItemStack aStack, int aConfig) {
		//? if forge {
		aStack.setDamageValue(aConfig); // vanilla :295-297 — getOrCreateTag().putInt("Damage", max(0, n))
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putInt(TAG_CONFIGURATION, aConfig);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		*///?}
	}

	/**
	 * The configuration number of a circuit stack, default 0 for a payload-less stack (the
	 * vanilla :291-293 read form). The 1.20.1 carrier is the {@code Damage} NBT key — the
	 * vanilla getDamageValue/setDamageValue pair works on ANY item regardless of
	 * damageability. 1.20.5+ CLAMPS getDamageValue to [0, maxDamage] (0 on this carrier —
	 * the research card's single-source risk note, docs/TODO.md:218), so the 21.1 leg moves
	 * the SAME key into the opaque {@code CUSTOM_DATA} envelope: the payload key keeps its
	 * exact shape byte-for-byte inside the envelope (the GT6DataComponents payload
	 * convention) and the component compare ({@code isSameItemSameComponents}) routes the
	 * configuration number exactly like the NBT tag compare does.
	 */
	public static int configurationOf(ItemStack aStack) {
		//? if forge {
		return aStack.getDamageValue();
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt(TAG_CONFIGURATION);
		*///?}
	}

	/**
	 * Whether a stack carries the configuration payload — the exact-tag matching premise
	 * (a tagged recipe input matches ONLY stacks carrying the tag). The 1.20.1 carrier is
	 * the {@code Damage} NBT key (vanilla ItemStack.java:291-297); 1.20.5+ re-expresses the
	 * same value as the {@code minecraft:damage} data component — the helper forks on the
	 * node (the Recipe.isSameItemAndTag fork shape).
	 */
	public static boolean hasConfigurationTag(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag() && aStack.getTag().contains(TAG_CONFIGURATION);
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(TAG_CONFIGURATION);
		*///?}
	}

	/** The item-identity probe the recipe consume step uses to skip the circuit stack (Recipe.checkStacksEqual). */
	public static boolean isSelector(ItemStack aStack) {
		return !aStack.isEmpty() && aStack.getItem() instanceof IntegratedCircuitItem;
	}

	private GT6Circuits() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Tools.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: the GTMachines fork precedent.
		*///?}
		ITEMS.register(tModBus);
	}
}
