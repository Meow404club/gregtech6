package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.wire.GTWireBlock;
import gregtech6.block.wire.GTWireBlockItem;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * Electric wire registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * GT6Mod.java / GTModBusListener.java stay untouched (the frozen P2 form is never
 * extended). Same shape as GTFluidPipes (the GTFluidPipes.java:48 Supplier block
 * precedent); the shared BET lives in {@link GTBlockEntities#WIRE_ELECTRIC_BE} per the
 * task card (the BET type row in the shared registry file), and its supplier resolves the
 * blocks here — safe because the vanilla registry order fires the Block registration
 * event before the BlockEntityType one, across DeferredRegisters (GTBlockEntities doc).
 *
 * <p>The p7 pair (spec ⑤ of that card) stays: 1x = 32 EU / 1 A / 1 loss (the upstream field
 * defaults, MultiTileEntityWireElectric.java:64) and 2x = 32 EU / 2 A / 1 loss (the upstream
 * "2x" bandwidth doubling :73) — they are the material-less legacy anchors the P8 RCON
 * chain and the gen→wire→oven e2e regression drive on, untouched.
 *
 * <p>Task p9-wire-family-w1 (spec ①/②) adds the FULL spectrum over the {@link GTWireSpecs}
 * table — the direct addElectricWires :71-109 / Loader_MultiTileEntities.java:1914-1950
 * transcription: 620 per-pair Block+BlockItem registrations (16 bare wires per material,
 * +5 insulated cables on the 28 cable rows; 620 = 28×21 + 2×16). One static loop registers
 * every {@link GTWireSpecs.Variant}; the shared BET's valid-block list is
 * {@link #wireBlockArray()} (GTBlockEntities). Material dereference happens inside the
 * registration suppliers (post MT.init, the GTBarrels lazy-row discipline); the registry
 * names come from the pre-init {@link GTWireSpecs.Row#token} field.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTWires {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** 1x electric wire — 32 EU / 1 A / 1 loss per segment (upstream :64 defaults). */
	public static final RegistryObject<GTWireBlock> WIRE_ELECTRIC_1X = BLOCKS.register("wire_electric_1x",
			() -> new GTWireBlock(32, 1, 1, BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	/** 2x electric wire — 32 EU / 2 A / 1 loss per segment (upstream :73 bandwidth doubling). */
	public static final RegistryObject<GTWireBlock> WIRE_ELECTRIC_2X = BLOCKS.register("wire_electric_2x",
			() -> new GTWireBlock(32, 2, 1, BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	public static final RegistryObject<Item> WIRE_ELECTRIC_1X_ITEM = ITEMS.register("wire_electric_1x",
			() -> new GTWireBlockItem(WIRE_ELECTRIC_1X.get(), new Item.Properties()));

	public static final RegistryObject<Item> WIRE_ELECTRIC_2X_ITEM = ITEMS.register("wire_electric_2x",
			() -> new GTWireBlockItem(WIRE_ELECTRIC_2X.get(), new Item.Properties()));

	// -------------------------------------------------------------------------
	// the p9-wire-family-w1 spectrum: 620 per-(row, form, size) pairs over GTWireSpecs
	// -------------------------------------------------------------------------

	/** The family blocks, GTWireSpecs order (upstream Loader row order, 16 wires then 5 cables per row). */
	public static final List<RegistryObject<GTWireBlock>> FAMILY_BLOCKS = new ArrayList<>();

	/** The family BlockItems, same order (stacksTo = the upstream literal 64/n ladder). */
	public static final List<RegistryObject<Item>> FAMILY_ITEMS = new ArrayList<>();

	/**
	 * The selector index: registry path -> family block (GTWireCommand / tests). Task
	 * p10-wire-laser-placeholder: the laser path ({@code wire_laser}) ALSO keys in here —
	 * this map is the {@code /gt6wire place <registry-path>} resolution channel, and keying
	 * the laser block into it lets the EXISTING command branch place and read the laser
	 * wire with ZERO command-surface change (the card's "reuse the existing family path,
	 * zero special logic"). The lists stay family-pure: {@link #FAMILY_BLOCKS} remains the
	 * ELECTRIC-only block list (the WIRE_ELECTRIC_BE valid-block source), the laser block
	 * lives in {@link #LASER_BLOCKS}.
	 */
	public static final Map<String, RegistryObject<GTWireBlock>> FAMILY_BY_NAME = new LinkedHashMap<>();

	static {
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
			String tName = GTWireSpecs.registryName(tVariant);
			if (FAMILY_BY_NAME.containsKey(tName)) throw new IllegalStateException("gt6 wire family: duplicate registry name " + tName);
			RegistryObject<GTWireBlock> tBlock = BLOCKS.register(tName,
					() -> new GTWireBlock(tVariant.voltage(), tVariant.amperage(), tVariant.loss(),
							tVariant.row().material().get(), tVariant.size(), tVariant.insulated(),
							tVariant.diameter(), BlockBehaviour.Properties.of()
									.strength(1.0F, 2.0F).sound(SoundType.COPPER))); // upstream NBT_HARDNESS 1.0 / NBT_RESISTANCE 2.0
			RegistryObject<Item> tItem = ITEMS.register(tName,
					() -> new GTWireBlockItem(tBlock.get(), new Item.Properties().stacksTo(tVariant.maxStack())));
			FAMILY_BLOCKS.add(tBlock);
			FAMILY_ITEMS.add(tItem);
			FAMILY_BY_NAME.put(tName, tBlock);
		}
	}

	// -------------------------------------------------------------------------
	// the p10-wire-redstone-family: 6 per-(row, form) pairs over GTWireSpecs.REDSTONE_ROWS
	// -------------------------------------------------------------------------

	/** The redstone-family blocks, GTWireSpecs.redstoneVariants() order (Loader:1893-1902 row order). */
	public static final List<RegistryObject<GTWireBlock>> REDSTONE_BLOCKS = new ArrayList<>();

	/** The redstone-family BlockItems, same order (upstream maxStack 64 on both forms). */
	public static final List<RegistryObject<Item>> REDSTONE_ITEMS = new ArrayList<>();

	/** The redstone selector index: registry path -> family block (GTWireCommand / datagen). */
	public static final Map<String, RegistryObject<GTWireBlock>> REDSTONE_BY_NAME = new LinkedHashMap<>();

	static {
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
			String tName = GTWireSpecs.registryName(tVariant);
			if (REDSTONE_BY_NAME.containsKey(tName)) throw new IllegalStateException("gt6 redstone wire family: duplicate registry name " + tName);
			// task p10 — the redstone block carries the family column (the push-BFS BE mount);
			// voltage 0 / amperage 1 (the EU face family is gated off at the BE), loss = the
			// upstream NBT_PIPELOSS (MAX_RANGE/16|/64), diameter PX_P[2] wire / PX_P[4] cable.
			RegistryObject<GTWireBlock> tBlock = BLOCKS.register(tName,
					() -> new GTWireBlock(0, 1, tVariant.loss(),
							tVariant.row().material().get(), tVariant.size(), tVariant.insulated(),
							tVariant.diameter(), GTWireSpecs.Row.Family.REDSTONE, BlockBehaviour.Properties.of()
									.strength(1.0F, 2.0F).sound(SoundType.COPPER))); // upstream NBT_HARDNESS 1.0 / NBT_RESISTANCE 2.0 (:1893-1902)
			RegistryObject<Item> tItem = ITEMS.register(tName,
					() -> new GTWireBlockItem(tBlock.get(), new Item.Properties().stacksTo(tVariant.maxStack())));
			REDSTONE_BLOCKS.add(tBlock);
			REDSTONE_ITEMS.add(tItem);
			REDSTONE_BY_NAME.put(tName, tBlock);
		}
	}

	/** Every redstone-wire block this registry owns — the WIRE_REDSTONE_BE valid-block list. */
	public static Block[] redstoneBlockArray() {
		Block[] rBlocks = new Block[REDSTONE_BLOCKS.size()];
		for (int i = 0; i < REDSTONE_BLOCKS.size(); i++) rBlocks[i] = REDSTONE_BLOCKS.get(i).get();
		return rBlocks;
	}

	// -------------------------------------------------------------------------
	// the p10-wire-laser-placeholder: 1 per-pair registration over GTWireSpecs.laserVariants()
	// -------------------------------------------------------------------------

	/** The laser-family blocks (Loader:1814-1815 — exactly one, the bare fiber wire). */
	public static final List<RegistryObject<GTWireBlock>> LASER_BLOCKS = new ArrayList<>();

	/** The laser-family BlockItems, same order. */
	public static final List<RegistryObject<Item>> LASER_ITEMS = new ArrayList<>();

	/** The laser selector index: registry path -> family block (tests / the FAMILY_BY_NAME channel). */
	public static final Map<String, RegistryObject<GTWireBlock>> LASER_BY_NAME = new LinkedHashMap<>();

	static {
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) {
			String tName = GTWireSpecs.registryName(tVariant);
			if (LASER_BY_NAME.containsKey(tName)) throw new IllegalStateException("gt6 laser wire family: duplicate registry name " + tName);
			// task p10-wire-laser-placeholder — the laser block carries the LASER family column;
			// voltage 0 / amperage 1 / loss 0 (upstream getEnergyLossPerMeter :114 = 0 — the
			// LOSSLESS wire; the Long.MAX_VALUE LU ratings stay the GTWireSpecs.LASER_CAPACITY
			// data pin, the EU face family is gated off at the BE), diameter PX_P[6] (:1815
			// NBT_DIAMETER), maxStack 64 (:1815), CONTACTDAMAGE F (:1815 — the inert family).
			RegistryObject<GTWireBlock> tBlock = BLOCKS.register(tName,
					() -> new GTWireBlock(0, 1, tVariant.loss(),
							tVariant.row().material().get(), tVariant.size(), tVariant.insulated(),
							tVariant.diameter(), GTWireSpecs.Row.Family.LASER, BlockBehaviour.Properties.of()
									.strength(1.0F, 2.0F).sound(SoundType.COPPER))); // upstream NBT_HARDNESS 1.0 / NBT_RESISTANCE 2.0 (:1815)
			RegistryObject<Item> tItem = ITEMS.register(tName,
					() -> new GTWireBlockItem(tBlock.get(), new Item.Properties().stacksTo(tVariant.maxStack())));
			LASER_BLOCKS.add(tBlock);
			LASER_ITEMS.add(tItem);
			LASER_BY_NAME.put(tName, tBlock);
			// the /gt6wire place channel (see FAMILY_BY_NAME) — the existing command branch
			// resolves the laser path from here, zero GTWireCommand surface.
			FAMILY_BY_NAME.put(tName, tBlock);
		}
	}

	/** Every laser-wire block this registry owns — the WIRE_LASER_BE valid-block list. */
	public static Block[] laserBlockArray() {
		Block[] rBlocks = new Block[LASER_BLOCKS.size()];
		for (int i = 0; i < LASER_BLOCKS.size(); i++) rBlocks[i] = LASER_BLOCKS.get(i).get();
		return rBlocks;
	}

	/**
	 * The shared redstone-wire BET (task p10): one BlockEntityType over the 6 family blocks,
	 * same BET class as the electric wire (the shared-carrier ruling — the family gate lives
	 * on the BE itself). Owns its own DeferredRegister so GTBlockEntities stays untouched
	 * (the W1 card surface); the Block event fires before the BlockEntityType event across
	 * DeferredRegisters of the same listener (the GTBlockEntities doc guarantee — this file
	 * registers BLOCKS before BETS in onModConstruct).
	 */
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	public static final RegistryObject<BlockEntityType<GTWireBlockEntity>> WIRE_REDSTONE_BE =
			BLOCK_ENTITY_TYPES.register("wire_redstone", () -> BlockEntityType.Builder.of(
					GTWireBlockEntity::new, redstoneBlockArray()).build(null));

	/**
	 * The shared laser-wire BET (task p10-wire-laser-placeholder): one BlockEntityType over
	 * the 1 laser family block, same BET class as the electric/redstone wires (the
	 * shared-carrier ruling — the family gate lives on the BE itself), the WIRE_REDSTONE_BE
	 * precedent verbatim. Owns its own DeferredRegister so GTBlockEntities stays untouched;
	 * the Block event fires before the BlockEntityType event across DeferredRegisters of
	 * the same listener (this file registers BLOCKS before BLOCK_ENTITY_TYPES in
	 * onModConstruct — the GTBlockEntities doc guarantee).
	 */
	public static final RegistryObject<BlockEntityType<GTWireBlockEntity>> WIRE_LASER_BE =
			BLOCK_ENTITY_TYPES.register("wire_laser", () -> BlockEntityType.Builder.of(
					GTWireBlockEntity::new, laserBlockArray()).build(null));

	/**
	 * Every electric-wire block this registry owns (the p7 legacy pair + the 620 family) —
	 * the shared BET's valid-block list (GTBlockEntities.WIRE_ELECTRIC_BE, one line per the card).
	 */
	public static Block[] wireBlockArray() {
		Block[] rBlocks = new Block[2 + FAMILY_BLOCKS.size()];
		rBlocks[0] = WIRE_ELECTRIC_1X.get();
		rBlocks[1] = WIRE_ELECTRIC_2X.get();
		for (int i = 0; i < FAMILY_BLOCKS.size(); i++) rBlocks[2 + i] = FAMILY_BLOCKS.get(i).get();
		return rBlocks;
	}

	/**
	 * The "Electric Wires" category tab — the upstream MTE-registry category
	 * ("Electric Wires", MultiTileEntityWireElectric.java:72 addElectricWires
	 * aCreativeTabID) as the minimal per-card tab, the GTFluidPipes.FLUID_PIPES_TAB shape.
	 *
	 * <p>Task p9-wire-family-w2 (the W1 review handoff): the displayItems are TABLE-DRIVEN
	 * over the full spectrum — upstream registers every addElectricWires row into this
	 * category (the whole 16-wire + 5-cable ladder per material), so the legacy pair is
	 * followed by all 620 family items in registration order (the W1 loop order = the
	 * upstream Loader row order).
	 */
	public static final RegistryObject<CreativeModeTab> ELECTRIC_WIRES_TAB = CREATIVE_MODE_TABS.register("electric_wires",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.electric_wires"))
					.icon(() -> new ItemStack(WIRE_ELECTRIC_2X_ITEM.get()))
					.displayItems((aParameters, aOutput) -> {
						aOutput.accept(new ItemStack(WIRE_ELECTRIC_1X_ITEM.get()));
						aOutput.accept(new ItemStack(WIRE_ELECTRIC_2X_ITEM.get()));
						for (RegistryObject<Item> tFamilyItem : FAMILY_ITEMS) {
							aOutput.accept(new ItemStack(tFamilyItem.get()));
						}
						// task p10-wire-redstone-family: the 6 redstone items ride the same flat
						// tab (upstream "Redstone Wires" is its own MTE category, Loader:1893-1902;
						// the per-category tab split stays the standing P9 observation item).
						for (RegistryObject<Item> tRedstoneItem : REDSTONE_ITEMS) {
							aOutput.accept(new ItemStack(tRedstoneItem.get()));
						}
						// task p10-wire-laser-placeholder: the laser item rides the same flat tab
						// (upstream "Laser Wires" is its own MTE category, Loader:1815 — the same
						// standing observation item as the redstone rows above).
						for (RegistryObject<Item> tLaserItem : LASER_ITEMS) {
							aOutput.accept(new ItemStack(tLaserItem.get()));
						}
					})
					.build());

	private GTWires() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus); // before BLOCK_ENTITY_TYPES — the valid-block resolution guarantee
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}
}
