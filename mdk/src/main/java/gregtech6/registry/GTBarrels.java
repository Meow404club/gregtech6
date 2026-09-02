package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.item.GTBarrelBlockItem;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.GTBarrelLogisticsBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;
import gregtech6.tileentity.tank.GTBarrelPlasticBlockEntity;

/**
 * Fluid barrel registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched. Same shape as GTFluidPipes/GTFluids;
 * a separate class keeps the W3 card scopes disjoint.
 *
 * <p>The barrel family (task p4-fluid-barrel wood, p6-barrel-metal-plastic wood-carrier
 * extension + plastic/metal, p7-barrel-high-tier-melt-bridge the 128K→10B ladder)
 * ports the upstream tank rows (Loader_MultiTileEntities.java:2136-2170, category
 * "Fluid Containers"): each material row carries its own TE class and BET, the capacity
 * and melt-down ceiling ride the block (the W1 registration-NBT-carrier pattern).
 * Creative tab ownership (card note ④): all barrels belong to the "Fluid Containers"
 * category — the upstream MTE-registry category of the same rows — as this card's
 * minimal per-card tab.
 *
 * <p>Task p7-barrel-high-tier-melt-bridge: (a) the material melting-point bridge —
 * {@link #meltingPointK} is the verbatim {@code TileEntityBase08Barrel.readFromNBT2}
 * :66 pair of branches (explicit NBT_CAPACITY_HU wins, else
 * {@code (long)(mMaterial.mMeltingPoint * 1.25)}), which revokes the P6 declared
 * deviation "metal MAX_VALUE never melts" — the bronze drum now carries its real
 * dataset ceiling; (b) the high-tier metal drum rows :2159-2170 (128K and above —
 * the 64K non-bronze alloy variants stay a pool cut) as shared-BET multi-mounts
 * (ADR-P3-1): one {@link GTBarrelMetalBlockEntity}, twelve blocks, zero new BE classes.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBarrels {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/**
	 * Wood fluid barrel — 16000 L sticky-tank family, melts down at 340 K (upstream
	 * NBT_CAPACITY_HU row). The capacity/ceiling/ticker-type now ride the block carrier
	 * explicitly (task p6-barrel-metal-plastic): 16000 L was already the class default,
	 * so this row is a zero-behaviour-change re-statement.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL = BLOCKS.register("barrel_wood",
			() -> new GTBarrelBlock(16000, 340, () -> GTBarrels.BARREL_BE.get(), BlockBehaviour.Properties.of()
					.strength(1.0F, 5.0F).sound(SoundType.WOOD)));

	/** The barrel BET: one BlockEntityType over the wood barrel (registry order BLOCKS before BLOCK_ENTITY_TYPES). */
	public static final RegistryObject<BlockEntityType<GTBarrelBlockEntity>> BARREL_BE =
			BLOCK_ENTITY_TYPES.register("barrel_wood", () -> BlockEntityType.Builder.of(
					GTBarrelBlockEntity::new, BARREL.get()).build(null));

	/** The barrel item — the p12 carrier item: FLUID_HANDLER_ITEM capability + the :290 content-stacking rule (empty barrels stack to 16). */
	public static final RegistryObject<Item> BARREL_ITEM = ITEMS.register("barrel_wood",
			() -> new GTBarrelBlockItem(BARREL.get(), new Item.Properties().stacksTo(16)));

	/**
	 * Plastic canister — 32000 L, melts down at 370 K (upstream NBT_CAPACITY_HU row,
	 * Loader_MultiTileEntities.java:2150; the upstream GASPROOF flag is a P4 quartet pool
	 * cut with no consumer). The block properties are placeholders like every barrel
	 * texture here: WOOL is the closest vanilla stand-in for plastic.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL_PLASTIC = BLOCKS.register("barrel_plastic",
			() -> new GTBarrelBlock(32000, 370, () -> GTBarrels.BARREL_PLASTIC_BE.get(),
					BlockBehaviour.Properties.of()
							.strength(1.0F, 5.0F).sound(SoundType.WOOL)));

	/** The plastic canister BET: validity over the plastic barrel only (one TE class per material row). */
	public static final RegistryObject<BlockEntityType<GTBarrelPlasticBlockEntity>> BARREL_PLASTIC_BE =
			BLOCK_ENTITY_TYPES.register("barrel_plastic", () -> BlockEntityType.Builder.of(
					GTBarrelPlasticBlockEntity::new, BARREL_PLASTIC.get()).build(null));

	/** The plastic canister item — the p12 carrier item (same shape as the wood row). */
	public static final RegistryObject<Item> BARREL_PLASTIC_ITEM = ITEMS.register("barrel_plastic",
			() -> new GTBarrelBlockItem(BARREL_PLASTIC.get(), new Item.Properties().stacksTo(16)));

	/**
	 * Metal drum — 64000 L bronze tier (upstream row Loader_MultiTileEntities.java:2151,
	 * the lowest metal drum of the 64K→10B ladder). The P6 declared deviation (MAX_VALUE,
	 * never melts) is revoked by the material melting-point bridge (task
	 * p7-barrel-high-tier-melt-bridge spec ①): the ceiling is the verbatim :66 else-branch
	 * over the live dataset — Bronze carries Copper's 1357 K (MT.java:1705
	 * {@code heat(Cu.mMeltingPoint)}), so the drum melts at (long)(1357 * 1.25) = 1696 K.
	 * Copper sound for the bronze drum.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL_METAL = BLOCKS.register("barrel_metal",
			() -> new GTBarrelBlock(64000, meltingPointK(MT.Bronze), () -> GTBarrels.BARREL_METAL_BE.get(),
					BlockBehaviour.Properties.of()
							.strength(1.0F, 6.0F).sound(SoundType.COPPER)));

	/**
	 * One high-tier metal drum row — the upstream
	 * {@code aRegistry.add("<display>", "Fluid Containers", <id>, 32719,
	 * MultiTileEntityBarrelMetal.class, ..., NBT_TANK_CAPACITY, <capacity>, ...,
	 * [NBT_CAPACITY_HU, <hu>], ...)} lines (Loader_MultiTileEntities.java:2159-2170),
	 * the block-carrier projection.
	 *
	 * <p>Spec ③ discipline: a row whose MT constant is absent from the ported dataset is
	 * dropped at porting time and recorded on the card — all twelve constants are present
	 * (verified per-line against MT.java), so the table is complete and zero rows skip; a
	 * null resolution at registration would be a porting bug and fails loudly (never a
	 * guessed number).
	 *
	 * @param path        the registry path (also the blockstate/model/lang key tail)
	 * @param displayName the upstream row display name, verbatim
	 * @param material    the upstream {@code aMat} (resolved lazily — class-load precedes MT.init)
	 * @param capacityL   the upstream NBT_TANK_CAPACITY
	 * @param explicitHU  the upstream NBT_CAPACITY_HU when the row carries one, else -1
	 *                    (the :66 else-branch applies)
	 * @param resistanceF the upstream NBT_RESISTANCE (NBT_HARDNESS is 1.0F on every row)
	 */
	public record MetalDrumRow(String path, String displayName, Supplier<OreDictMaterial> material,
			long capacityL, long explicitHU, float resistanceF) {

		/** The block-carrier melting point: explicit NBT_CAPACITY_HU wins, else the :66 material formula. */
		public long meltingPointK() {
			return explicitHU >= 0 ? explicitHU : GTBarrels.meltingPointK(material.get());
		}
	}

	/**
	 * The 128K-and-above drum ladder (Loader_MultiTileEntities.java:2159-2170, upstream
	 * row order): 128K {TungstenAlloy, Ti, Netherite} → 256K {Tungstensteel,
	 * Tungsten(ANY.W), Voidmetal} → 512K {Ta4HfC5} → 1.024M {Gaia} → 4.096M {Adamantium,
	 * Draconium} → 8.192M {Awakened Draconium, NBT_CAPACITY_HU=10000} → 10B {Infinity,
	 * NBT_CAPACITY_HU=1000000000}. The two explicit-HU rows are transcribed verbatim —
	 * the :66 branch 1 overrides the material formula (the Infinity drum's ceiling is
	 * upstream's own 1e9 K, an effectively-never-melting figure; the card's "keep-MAX"
	 * clause covers materials whose dataset mMeltingPoint IS MAX_VALUE, see
	 * {@link #meltingPointK(long)}). The 64K non-bronze alloy variants (:2152-2158) stay
	 * a pool cut per the card boundary.
	 */
	public static final List<MetalDrumRow> HIGH_TIER_METAL_DRUMS = List.of(
			new MetalDrumRow("barrel_tungsten_alloy", "Tungsten Alloy Drum", () -> MT.TungstenAlloy, 128000, -1, 9.0F),
			new MetalDrumRow("barrel_titanium", "Titanium Drum", () -> MT.Ti, 128000, -1, 9.0F),
			new MetalDrumRow("barrel_netherite", "Netherite Drum", () -> MT.Netherite, 128000, -1, 9.0F),
			new MetalDrumRow("barrel_tungstensteel", "Tungstensteel Drum", () -> MT.TungstenSteel, 256000, -1, 12.5F),
			new MetalDrumRow("barrel_tungsten", "Tungsten Drum", () -> ANY.W, 256000, -1, 10.0F),
			new MetalDrumRow("barrel_void_metal", "Voidmetal Drum", () -> MT.VoidMetal, 256000, -1, 10.0F),
			new MetalDrumRow("barrel_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide Drum", () -> MT.Ta4HfC5, 512000, -1, 10.0F),
			new MetalDrumRow("barrel_gaia_spirit", "Gaia Drum", () -> MT.GaiaSpirit, 1024000, -1, 25.0F),
			new MetalDrumRow("barrel_adamantium", "Adamantium Drum", () -> MT.Ad, 4096000, -1, 100.0F),
			new MetalDrumRow("barrel_draconium", "Draconium Drum", () -> MT.Draconium, 4096000, -1, 100.0F),
			new MetalDrumRow("barrel_awakened_draconium", "Awakened Draconium Drum", () -> MT.DraconiumAwakened, 8192000, 10000, 100.0F),
			new MetalDrumRow("barrel_infinity", "Infinity Drum", () -> MT.Infinity, 10000000000L, 1000000000L, 100.0F));

	/**
	 * The high-tier blocks/BlockItems, one pair per row. The material suppliers resolve
	 * inside the registration lambdas (RegisterEvent — strictly after the ConstructMod
	 * enqueueWork that runs MT.init(), the GTMaterialItems.onRegister precedent), so the
	 * bridge reads live dataset values; the {@code GTBarrels.}-qualified BET reference is
	 * the legal forward-reference form (the P6 lambda lesson).
	 */
	public static final Map<String, RegistryObject<GTBarrelBlock>> METAL_DRUM_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> METAL_DRUM_ITEMS = new LinkedHashMap<>();
	static {
		for (MetalDrumRow tRow : HIGH_TIER_METAL_DRUMS) {
			METAL_DRUM_BLOCKS.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTBarrelBlock(tRow.capacityL(), tRow.meltingPointK(),
							() -> GTBarrels.BARREL_METAL_BE.get(), BlockBehaviour.Properties.of()
									.strength(1.0F, tRow.resistanceF()).sound(SoundType.COPPER))));
			METAL_DRUM_ITEMS.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTBarrelBlockItem(GTBarrels.METAL_DRUM_BLOCKS.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	/**
	 * The metal drum BET — the shared-BET multi-mount (ADR-P3-1, the GT6 "one TE class,
	 * many material blocks" counterpart): valid over the bronze drum AND every high-tier
	 * row; zero new BE classes.
	 */
	public static final RegistryObject<BlockEntityType<GTBarrelMetalBlockEntity>> BARREL_METAL_BE =
			BLOCK_ENTITY_TYPES.register("barrel_metal", () -> BlockEntityType.Builder.of(
					GTBarrelMetalBlockEntity::new,
					Stream.concat(Stream.of(GTBarrels.BARREL_METAL.get()),
							GTBarrels.METAL_DRUM_BLOCKS.values().stream().map(RegistryObject::get))
						.toArray(Block[]::new)).build(null));

	/** The metal drum item — the p12 carrier item (same shape as the wood row). */
	public static final RegistryObject<Item> BARREL_METAL_ITEM = ITEMS.register("barrel_metal",
			() -> new GTBarrelBlockItem(BARREL_METAL.get(), new Item.Properties().stacksTo(16)));

	/**
	 * Logistics Tank — the keepFilter barrel (task p12-barrel-keepfilter-logistics), the
	 * upstream row Loader_MultiTileEntities.java:2171 verbatim on the load-bearing numbers:
	 * {@code "Logistics Tank", MultiTileEntityBarrelLogistics, NBT_HARDNESS 1.0F,
	 * NBT_RESISTANCE 10.0F, NBT_TANK_CAPACITY 1000000L, NBT_CAPACITY_HU 100000} — the block
	 * carrier takes capacityL = 1000000 and the 100000 K melt ceiling; the block sound is
	 * the metal-drum COPPER stand-in (the row material is ANY.W = tungsten with the
	 * aUtilMetal tool set — NOT the wood set the task-card sketch guessed, upstream :2171
	 * read verbatim). Declared cuts: the row's four-proof flags (all T) are the P4 quartet
	 * pool with no port consumer; the upstream category is "Logistics" (id 17997) and the
	 * recipe references {@code IL.Cover_Logistics_Generic_Storage} +
	 * {@code IL.FIELD_GENERATORS} — no logistics pipe network exists in the port, so the
	 * barrel pools into this "Fluid Containers" tab below and stays /give-reachable, card
	 * spec ②.
	 */
	public static final RegistryObject<GTBarrelBlock> BARREL_LOGISTICS = BLOCKS.register("barrel_logistics",
			() -> new GTBarrelBlock(1000000, 100000, () -> GTBarrels.BARREL_LOGISTICS_BE.get(),
					BlockBehaviour.Properties.of()
							.strength(1.0F, 10.0F).sound(SoundType.COPPER)));

	/** The logistics tank BET: one BlockEntityType over the logistics barrel only (one TE class per material row, the wood/plastic shape). */
	public static final RegistryObject<BlockEntityType<GTBarrelLogisticsBlockEntity>> BARREL_LOGISTICS_BE =
			BLOCK_ENTITY_TYPES.register("barrel_logistics", () -> BlockEntityType.Builder.of(
					GTBarrelLogisticsBlockEntity::new, BARREL_LOGISTICS.get()).build(null));

	/** The logistics tank item — the p12 carrier item (same shape as the wood row). */
	public static final RegistryObject<Item> BARREL_LOGISTICS_ITEM = ITEMS.register("barrel_logistics",
			() -> new GTBarrelBlockItem(BARREL_LOGISTICS.get(), new Item.Properties().stacksTo(16)));

	/** The "Fluid Containers" category tab (upstream MTE category of the barrel row, :2136 column 2). */
	public static final RegistryObject<CreativeModeTab> FLUID_CONTAINERS_TAB = CREATIVE_MODE_TABS.register("fluid_containers",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.fluid_containers"))
					.icon(() -> new ItemStack(BARREL_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(BARREL_ITEM.get()));
						aOutput.accept(new ItemStack(BARREL_PLASTIC_ITEM.get()));
						aOutput.accept(new ItemStack(BARREL_METAL_ITEM.get()));
						aOutput.accept(new ItemStack(BARREL_LOGISTICS_ITEM.get())); // the :2171 row pools here — its upstream "Logistics" category is a port pool cut
						for (RegistryObject<Item> tItem : GTBarrels.METAL_DRUM_ITEMS.values()) aOutput.accept(new ItemStack(tItem.get()));
					})
					.build());

	private GTBarrels() {}

	/**
	 * The material melting-point bridge (task p7 spec ①) — the verbatim
	 * {@code TileEntityBase08Barrel.readFromNBT2} :66 pair of branches split over two
	 * overloads: the row NBT home is the block carrier, so the explicit-HU branch lives
	 * in {@link MetalDrumRow#meltingPointK()} and this helper is the else-branch
	 * {@code mMeltingPoint = (long)(mMaterial.mMeltingPoint * 1.25)} — the Java
	 * double-multiply + long cast, its truncation included.
	 */
	public static long meltingPointK(OreDictMaterial aMaterial) {
		return aMaterial == null ? Long.MAX_VALUE : meltingPointK(aMaterial.mMeltingPoint);
	}

	/**
	 * The raw :66 else-branch arithmetic. A MAX_VALUE material stays MAX_VALUE (keep-MAX —
	 * the upstream form reaches the same ceiling through the double-cast saturation,
	 * {@code (long)(Long.MAX_VALUE * 1.25)} clamps to Long.MAX_VALUE; the explicit branch
	 * just states it), so a never-melting material yields a never-melting drum.
	 */
	public static long meltingPointK(long aMaterialMeltingPointK) {
		if (aMaterialMeltingPointK >= Long.MAX_VALUE) return Long.MAX_VALUE;
		return (long)(aMaterialMeltingPointK * 1.25);
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTFluidPipes.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}

	/** Registration smoke evidence (the GTFluids.onCommonSetup log shape, acceptance ④). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 fluid barrels registered: {} {} L @ {} K / {} {} L @ {} K / {} {} L @ {} K",
					ForgeRegistries.BLOCKS.getKey(BARREL.get()), BARREL.get().capacityL(), BARREL.get().meltingPointK(),
					ForgeRegistries.BLOCKS.getKey(BARREL_PLASTIC.get()), BARREL_PLASTIC.get().capacityL(), BARREL_PLASTIC.get().meltingPointK(),
					ForgeRegistries.BLOCKS.getKey(BARREL_METAL.get()), BARREL_METAL.get().capacityL(), BARREL_METAL.get().meltingPointK());
			// the p12 keepFilter row (:2171) — one log line, same shape as the drum rows
			GT6Mod.LOGGER.info("GT6 logistics tank registered: {} \"Logistics Tank\" {} L @ {} K (keepsFilter)",
					BARREL_LOGISTICS.getId(), BARREL_LOGISTICS.get().capacityL(), BARREL_LOGISTICS.get().meltingPointK());
			// the p7 acceptance ④: one log row per drum, capacity + bridge melting point
			for (MetalDrumRow tRow : HIGH_TIER_METAL_DRUMS) {
				RegistryObject<GTBarrelBlock> tBlock = GTBarrels.METAL_DRUM_BLOCKS.get(tRow.path());
				GT6Mod.LOGGER.info("GT6 metal drum registered: {} \"{}\" {} L @ {} K",
						tBlock.getId(), tRow.displayName(), tRow.capacityL(), tBlock.get().meltingPointK());
			}
		});
	}
}
