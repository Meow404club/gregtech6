package gregtech6.registry;

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

/**
 * The GT6 bee-hive registration home — task p32-bees-lv2. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GT6SprayCans/GT6BeeCombs precedent verbatim).
 *
 * <p>One block + one BE type: the MTE 32755 "Bumble Hive" port
 * (Loader_MultiTileEntities.java:2041, id row verbatim in the class javadocs). NO
 * BlockItem, NO creative tab — the worldgen-only loot shell is never obtainable as an
 * item (the GT6SurfaceBlocks rock/stick form; /setblock is the placement face, the
 * RCON chain drives it).
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
	}

	/**
	 * The world-half paint tint over the hive block — the GTMachinePaintTint lambda reads
	 * the BE's PAINT model data (the material fallback resolves null on the hive = the
	 * white identity). Item half not needed: no BlockItem exists. CLIENT-ONLY nested
	 * subscriber (the dist guard keeps the server classload clean).
	 */
	@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	private static final class ClientTint {
		@SubscribeEvent
		public static void onRegisterBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block aEvent) {
			aEvent.getBlockColors().register(GTMachinePaintTint.blockColor(), HIVE.get());
		}
	}
}
