package gregtech6.registry;


import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import gregtech6.GT6Mod;
import gregtech6.block.portals.GTMiniPortalBlock;
import gregtech6.tileentity.portals.GTMiniPortalEndBlockEntity;
import gregtech6.tileentity.portals.GTMiniPortalNetherBlockEntity;

/**
 * The portal registration home (task p35-portals-mini-nether-end) — the self-contained
 * {@code @EventBusSubscriber} shape (the GT6HeatExchangers form: blocks, items, BETs and
 * the server-lifecycle listener in ONE card-owned file; the GTBlockEntities shared file
 * stays untouched — the W2 merge-order ratchet, energy→pipes→portals→rails).
 *
 * <p>The two live rows (Loader_MultiTileEntities.java:2003/:2004, category "Portals";
 * the 19 mod-gate rows :2005+ are the never_pool — Twilight=TF and friends stay out,
 * the card mandate):
 * <ul>
 * <li>Miniature Nether Portal — id 32766, hardness 3, resistance 16, host aStone → STONE
 *     sound (upstream :2003 columns);</li>
 * <li>Miniature End Portal — id 32000, hardness 1, resistance 16, aStone (upstream :2004
 *     columns).</li>
 * </ul>
 *
 * <p>The static pair lists of the portal BEs live through a whole server session — the
 * server-bus listener clears them on ServerStarted/ServerStopped (upstream
 * onServerStart/Stop, MultiTileEntityMiniPortal.java:209-210).
 *
 * <p>Creative tab: both portal items join MACHINES_TAB via {@link #onBuildTabContents}
 * (task p38-tabfix-d-ruling, the user ruling over the port-native tab-less state; the
 * GT6BurningBoxes join form).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Portals {

	private static final Logger LOGGER = LogUtils.getLogger();

	// the DeferredRegister trio (the GT6Boilers/GT6HeatExchangers form)
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** Upstream :2003 — "Miniature Nether Portal", 32766, 3/16, aStone. */
	public static final RegistryObject<Block> PORTAL_NETHER = BLOCKS.register("mini_portal_nether",
			() -> new GTMiniPortalBlock(GT6Portals::netherType, BlockBehaviour.Properties.of()
					.strength(3.0F, 16.0F).sound(SoundType.STONE)));

	/** Upstream :2004 — "Miniature End Portal", 32000, 1/16, aStone. */
	public static final RegistryObject<Block> PORTAL_END = BLOCKS.register("mini_portal_end",
			() -> new GTMiniPortalBlock(GT6Portals::endType, BlockBehaviour.Properties.of()
					.strength(1.0F, 16.0F).sound(SoundType.STONE)));

	/** The ticker-type resolvers — static METHODS break the block↔BET field forward-reference cycle (the solidBurningBoxFactory shape). */
	static net.minecraft.world.level.block.entity.BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> netherType() {
		return PORTAL_NETHER_BE.get();
	}

	static net.minecraft.world.level.block.entity.BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> endType() {
		return PORTAL_END_BE.get();
	}

	public static final RegistryObject<Item> PORTAL_NETHER_ITEM = ITEMS.register("mini_portal_nether",
			() -> new GT6PortalItem(PORTAL_NETHER.get(), new Item.Properties(),
					"gt.tileentity.portal.mini.tooltip.1", "gt.tileentity.portal.mini.tooltip.2",
					"gt.tileentity.portal.nether.tooltip.1", "gt.tileentity.portal.nether.tooltip.2",
					"gt.lang.requirement.ignite.fire"));
	public static final RegistryObject<Item> PORTAL_END_ITEM = ITEMS.register("mini_portal_end",
			() -> new GT6PortalItem(PORTAL_END.get(), new Item.Properties(),
					"gt.tileentity.portal.mini.tooltip.1", "gt.tileentity.portal.mini.tooltip.2",
					"gt.tileentity.portal.end.tooltip.1", "gt.tileentity.portal.end.tooltip.2",
					"gt.tileentity.portal.end.tooltip.3"));

	/**
	 * The portal BlockItem with the upstream addToolTips stack (MultiTileEntityMiniPortal.java:93-98
	 * — the two shared function lines, the per-family distance/activation lines and the orange
	 * requirement line; the Nether pair carries the ignite requirement, the End pair the
	 * Ender-Eye line). The chunk-loader warning (:97) rides BOTH items.
	 */
	public static final class GT6PortalItem extends BlockItem {
		private final String[] mTooltipKeys;

		public GT6PortalItem(Block aBlock, Item.Properties aProperties, String... aTooltipKeys) {
			super(aBlock, aProperties);
			mTooltipKeys = aTooltipKeys;
		}

		//? if forge {
		@Override
		public void appendHoverText(net.minecraft.world.item.ItemStack aStack, @javax.annotation.Nullable net.minecraft.world.level.Level aLevel,
				java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
			tooltipLines(aTooltip);
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		}
		//?} else {
		/*@Override
		public void appendHoverText(net.minecraft.world.item.ItemStack aStack, net.minecraft.world.item.Item.TooltipContext aContext,
				java.util.List<net.minecraft.network.chat.Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
		//21.1: the Level argument became Item.TooltipContext (javap 21.1.249).
			tooltipLines(aTooltip);
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		}
		*///?}

		private void tooltipLines(java.util.List<net.minecraft.network.chat.Component> aTooltip) {
			for (String tKey : mTooltipKeys) {
				aTooltip.add(net.minecraft.network.chat.Component.translatable(tKey));
			}
			aTooltip.add(net.minecraft.network.chat.Component.translatable("gt.lang.requirement.chunk.loader")); // upstream :97
		}
	}

	/** The Nether portal BET (the CRANK_BE single-mount shape; registry path mirrors getTileEntityName's tail). */
	public static final RegistryObject<BlockEntityType<GTMiniPortalNetherBlockEntity>> PORTAL_NETHER_BE =
			BLOCK_ENTITY_TYPES.register("mini_portal_nether", () -> BlockEntityType.Builder.of(
					GTMiniPortalNetherBlockEntity::new, PORTAL_NETHER.get()).build(null));

	/** The End portal BET — the same shape over its own block. */
	public static final RegistryObject<BlockEntityType<GTMiniPortalEndBlockEntity>> PORTAL_END_BE =
			BLOCK_ENTITY_TYPES.register("mini_portal_end", () -> BlockEntityType.Builder.of(
					GTMiniPortalEndBlockEntity::new, PORTAL_END.get()).build(null));

	/**
	 * FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Sensors form):
	 * the three DeferredRegisters join the mod bus, strictly before any RegisterEvent.
	 * The MOD-bus annotation rides the OUTER class (the sensors shape).
	 */
	@SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		net.minecraftforge.eventbus.api.IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*net.minecraftforge.eventbus.api.IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6HeatExchangers fork).
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/**
	 * The tab walk (task p38-tabfix-d-ruling — the two portal items join the machines tab;
	 * the GT6BurningBoxes.onBuildTabContents verbatim form, the class-level MOD-bus
	 * {@code @Mod.EventBusSubscriber} at the class head is what delivers this handler).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(PORTAL_NETHER_ITEM.get()));
			aEvent.accept(new ItemStack(PORTAL_END_ITEM.get()));
		}
	}

	/**
	 * The session pair-list wipe (upstream onServerStart/Stop :209-210) — public so the
	 * offline lifecycle test (a different package) drives it; idempotent and side-effect-free.
	 */
	public static void clearPairLists() {
		GTMiniPortalNetherBlockEntity.sListNetherSide.clear();
		GTMiniPortalNetherBlockEntity.sListWorldSide.clear();
		GTMiniPortalEndBlockEntity.sListEndSide.clear();
		GTMiniPortalEndBlockEntity.sListWorldSide.clear();
		LOGGER.info("GT6 portal pair lists cleared (upstream onServerStart/Stop, MiniPortal:209-210)");
	}

	/**
	 * The session pair-list wipe listener — the forge GAME bus (the
	 * ServerLifecycleEvents face, the GT6CapabilityWiring listener form).
	 */
	@Mod.EventBusSubscriber(modid = "gt6")
	private static final class ServerLifecycleLists {
		@SubscribeEvent
		public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent aEvent) {
			clearPairLists();
		}

		@SubscribeEvent
		public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent aEvent) {
			clearPairLists();
		}
	}

	private GT6Portals() {
	}
}
