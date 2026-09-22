package gregtech6.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.client.render.GTMachinePaintTint;
import gregtech6.tileentity.bees.GT6BumbleHiveBlock;
import gregtech6.tileentity.bees.GT6BumbleHiveBlockEntity;
import gregtech6.tileentity.bees.GT6BumbliaryBlock;
import gregtech6.tileentity.bees.GT6BumbliaryBlockEntity;

/**
 * The GT6 bee-hive registration home — task p32-bees-lv2. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GT6SprayCans/GT6BeeCombs precedent verbatim).
 *
 * <p>One block + one BE type: the MTE 32755 "Bumble Hive" port
 * (Loader_MultiTileEntities.java:2041, id row verbatim in the class javadocs).
 *
 * <p><b>THE R2 CONTAINMENT-CONTRACT REVISION (task p34-bumbliary-recipes)</b> — the
 * p32 ruling "the hive never gains an item face" is BROKEN ONCE, declared: the MTE 32755
 * HAS an item form upstream ({@code aRegistry.getItem(32755)} is the 32741 Bumbliary
 * recipe's 'B' key, Loader_MultiTileEntities.java:2222; the base getDrops
 * TileEntityBase04MultiTileEntities.java:166-171 puts the block item into EVERY break's
 * drop list) — so a scooped wild hive is carryable, and the recipe chain needs the item
 * to exist. The {@code gt6:bumble_hive} BlockItem lands (creative tab: the gt6:bee tab,
 * the GT6BeeCombs home). Placing it gives an empty hive (the worldgen fill is the only
 * content writer — the same face the upstream item placement gives).
 *
 * <p>Block properties: strength 1.0 (the upstream getBlockHardness/getExplosionResistance2
 * lit-pumpkin pair, MultiTileEntityBumbleHive.java:80-82 = vanilla jack_o_lantern
 * {@code strength(1.0F)}), wood sound (the aHive MTE block, :111), orange map colour
 * (the hive-body tint), noLootTable (the contents are the loot — the block's
 * playerDestroy walk). The tint registration is the hive row of the machine-paint face
 * (task p21-paintable-tint-render): the same {@link GTMachinePaintTint} lambda resolves
 * the BE's PAINT model data — worldgen paints the family colour at placement and the
 * spray cans recolour it; the unpainted fallback resolves white (the material-less
 * identity, byte-identical no-tint).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BeeHives {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** The hive block — id {@code gt6:bumble_hive} (the upstream "Bumble Hive" :2041 flattened). */
	public static final RegistryObject<Block> HIVE = BLOCKS.register("bumble_hive",
			() -> new GT6BumbleHiveBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_ORANGE)
					.strength(1.0F)
					.sound(SoundType.WOOD)
					.noLootTable()));

	/** The hive BET: one class, its one block (the ADR-P3-1 single-block form). */
	public static final RegistryObject<BlockEntityType<GT6BumbleHiveBlockEntity>> HIVE_BE =
			BLOCK_ENTITY_TYPES.register("bumble_hive",
					() -> BlockEntityType.Builder.of(GT6BumbleHiveBlockEntity::new, HIVE.get()).build(null));

	// ---------------------------------------------------------------------------
	// the Bumbliary pair (task p33-bees-lv3-b-bumbliary) — the MTE 32741/32007 ports,
	// obtainable machines (the BlockItem face the worldgen-only hive never needed).
	// ---------------------------------------------------------------------------

	/** The Bumbliary ITEMS register (the machine pair + the R2 hive BlockItem). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	/** The hive BlockItem — the R2 ruling's carryable-wild-hive face (the class javadoc's
	 *  containment-contract revision; the 32741 recipe's 'B' key upstream, :2222). The
	 *  display name rides the BLOCK's key (the vanilla BlockItem.getDescriptionId walk). */
	public static final RegistryObject<Item> HIVE_ITEM =
			ITEMS.register("bumble_hive", () -> new BlockItem(HIVE.get(), new Item.Properties()));

	/** The Bumbliary block — id {@code gt6:bumbliary} (the MTE 32741 "Bumbliary" flattened,
	 *  Loader_MultiTileEntities.java:2222; the wooden hardness/resistance 5.0/5.0 row). */
	public static final RegistryObject<Block> BUMBLIARY = BLOCKS.register("bumbliary",
			() -> new GT6BumbliaryBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOD)
					.strength(5.0F, 5.0F)
					.sound(SoundType.WOOD), false));

	/** The Bumbliary BlockItem — the obtainable-machine face. */
	public static final RegistryObject<Item> BUMBLIARY_ITEM =
			ITEMS.register("bumbliary", () -> new BlockItem(BUMBLIARY.get(), new Item.Properties()));

	/** The Advanced Bumbliary block — id {@code gt6:bumbliary_advanced} (the MTE 32007
	 *  "Advanced Bumbliary", :2223; the stainless 6.0/6.0 row). */
	public static final RegistryObject<Block> BUMBLIARY_ADVANCED = BLOCKS.register("bumbliary_advanced",
			() -> new GT6BumbliaryBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_GRAY)
					.strength(6.0F, 6.0F)
					.sound(SoundType.WOOD), true));

	/** The Advanced BlockItem. */
	public static final RegistryObject<Item> BUMBLIARY_ADVANCED_ITEM =
			ITEMS.register("bumbliary_advanced", () -> new BlockItem(BUMBLIARY_ADVANCED.get(), new Item.Properties()));

	/** The Bumbliary BET: one class, the flag-picked layout (the ADR-P3-1 form). */
	public static final RegistryObject<BlockEntityType<GT6BumbliaryBlockEntity>> BUMBLIARY_BE =
			BLOCK_ENTITY_TYPES.register("bumbliary",
					() -> BlockEntityType.Builder.of(GT6BumbliaryBlockEntity::new, BUMBLIARY.get()).build(null));

	/** The Advanced BET: the same class over the advanced layout. The factory needs its own
	 *  BET (the TE carries the type), so the holder routes through an array seam (the
	 *  GT6BumbleHiveBlockEntityTest fixture shape — the lambda defers the read past init). */
	public static final RegistryObject<BlockEntityType<GT6BumbliaryBlockEntity>> BUMBLIARY_ADVANCED_BE = registerAdvancedBet();

	private static RegistryObject<BlockEntityType<GT6BumbliaryBlockEntity>> registerAdvancedBet() {
		final BlockEntityType.BlockEntitySupplier<GT6BumbliaryBlockEntity>[] tFactory =
				new BlockEntityType.BlockEntitySupplier[1];
		RegistryObject<BlockEntityType<GT6BumbliaryBlockEntity>> rType = BLOCK_ENTITY_TYPES.register("bumbliary_advanced",
				() -> BlockEntityType.Builder.<GT6BumbliaryBlockEntity>of(tFactory[0], BUMBLIARY_ADVANCED.get()).build(null));
		tFactory[0] = (aPos, aState) -> new GT6BumbliaryBlockEntity(true, rType.get(), aPos, aState);
		return rType;
	}

	private GT6BeeHives() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6SprayCans.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/**
	 * The world-half paint tint over the hive block — the GTMachinePaintTint lambda reads
	 * the BE's PAINT model data (the material fallback resolves null on the hive = the
	 * white identity). Item half not needed: the BlockItem's inventory model bakes the
	 * unpainted-white face (the paint data lives on the BE, an item stack carries none —
	 * the same face the Bumbliary BlockItems ride). CLIENT-ONLY nested
	 * subscriber (the dist guard keeps the server classload clean).
	 */
	@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	private static final class ClientTint {
		@SubscribeEvent
		public static void onRegisterBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block aEvent) {
			aEvent.getBlockColors().register(GTMachinePaintTint.blockColor(), HIVE.get());
			aEvent.getBlockColors().register(GTMachinePaintTint.blockColor(), BUMBLIARY.get());
			aEvent.getBlockColors().register(GTMachinePaintTint.blockColor(), BUMBLIARY_ADVANCED.get());
		}
	}
}
