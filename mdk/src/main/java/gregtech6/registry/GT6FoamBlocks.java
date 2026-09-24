package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.block.foam.GT6CFoamBlock;
import gregtech6.block.foam.GT6CFoamFreshBlock;
import gregtech6.block.foam.GT6CFoamFreshSlabBlock;
import gregtech6.block.foam.GT6CFoamOwnedBlock;
import gregtech6.block.foam.GT6CFoamSlabBlock;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.tileentity.foam.GT6CFoamBlockEntity;

/**
 * The C-Foam block family registration — task p26-c-foam-block-family. The card-owned
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister shape (the
 * {@link GT6FoamSprays} precedent; GT6Mod/GTModBusListener untouched).
 *
 * <p>Five blocks over two plain states + the owned TE carrier (the spec_rulings
 * color_dim ruling — ONE block per state over a 16-step {@code color} property, NOT a
 * 16-instance ladder):
 * <ul>
 * <li>{@code gt6:cfoam_fresh} / {@code gt6:cfoam} — the wet/dried full blocks
 *     (upstream BlockCFoamFresh/BlockCFoam, BlocksGT.CFoamFresh/CFoam);</li>
 * <li>{@code gt6:cfoam_fresh_slab} / {@code gt6:cfoam_slab} — the mSlabs forms
 *     (the spec_rulings.ruling_slab vanilla SlabBlock ruling);</li>
 * <li>{@code gt6:cfoam_owned} — the owned TE carrier (upstream MTE 32765), NO BlockItem
 *     (upstream showInCreative false, MultiTileEntityCFoam.java:152);</li>
 * </ul>
 * BlockItems exist for the two DRIED forms only (the self-drop loot carriers; the fresh
 * blocks are spray-only intermediate states with empty loot tables). Declared deviation:
 * the upstream 16-meta ITEM ladder (one item per colour) collapses to one uncoloured item
 * per dried form — the placed colour persists while standing (the tint), the re-placed
 * item lands colour 0 (the per-pair item ladder is the P21 stone ruling's counter-form;
 * this card's files_scope asks for no 16-item band).
 *
 * <p>The registry paths deliberately do NOT overlap the fluid family ({@code cfoam_*}
 * fluids, task p26-c-foam-fluid-refill) at the STRING level even though the registries
 * are distinct — the block {@code cfoam} row is the dried block, the fluid {@code cfoam}
 * row is the base fluid (decisions.p26-cfoam-fluid-naming), both gt6-namespaced.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FoamBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	// The declaration order is the textual forward-reference order (the dried targets of the
	// fresh forms must be DECLARED above their consumers — the suppliers only RUN at
	// registration, but the JLS 8.3.3 simple-name check is textual).

	/** The dried full block (upstream BlockCFoam — BlocksGT.CFoam). */
	public static final RegistryObject<Block> CFOAM = BLOCKS.register("cfoam",
			() -> new GT6CFoamBlock(GT6CFoamBlock.driedProperties()));

	/** The dried slab (the upstream mSlabs dried form). */
	public static final RegistryObject<Block> CFOAM_SLAB = BLOCKS.register("cfoam_slab",
			() -> new GT6CFoamSlabBlock(GT6CFoamSlabBlock.driedSlabProperties()));

	/** The wet full block (upstream BlockCFoamFresh — BlocksGT.CFoamFresh). */
	public static final RegistryObject<Block> CFOAM_FRESH = BLOCKS.register("cfoam_fresh",
			() -> new GT6CFoamFreshBlock(GT6CFoamFreshBlock.freshProperties(), CFOAM::get));

	/** The wet slab (the upstream mSlabs fresh form). */
	public static final RegistryObject<Block> CFOAM_FRESH_SLAB = BLOCKS.register("cfoam_fresh_slab",
			() -> new GT6CFoamFreshSlabBlock(GT6CFoamFreshSlabBlock.freshSlabProperties(), () -> (GT6CFoamSlabBlock)CFOAM_SLAB.get()));

	/** The owned TE carrier (upstream MTE 32765) — no BlockItem, spray-only placement. */
	public static final RegistryObject<Block> CFOAM_OWNED = BLOCKS.register("cfoam_owned",
			() -> new GT6CFoamOwnedBlock(GT6CFoamOwnedBlock.ownedProperties()));

	/**
	 * The owned-foam BET (the TestMachineBlockEntity single-mount row shape; the registry
	 * path mirrors {@link GT6CFoamBlockEntity} like every row). BLOCK registers before
	 * BLOCK_ENTITY_TYPES (vanilla registry order), so the {@code .get()} is safe.
	 */
	public static final RegistryObject<BlockEntityType<GT6CFoamBlockEntity>> CFOAM_OWNED_BE =
			BLOCK_ENTITY_TYPES.register("cfoam_owned", () -> BlockEntityType.Builder.of(
					GT6CFoamBlockEntity::new, CFOAM_OWNED.get()).build(null));

	/** The dried full block's item — the self-drop loot carrier (uncoloured default). */
	public static final RegistryObject<Item> CFOAM_ITEM = ITEMS.register("cfoam",
			() -> new BlockItem(CFOAM.get(), new Item.Properties()));

	/** The dried slab's item — the self-drop loot carrier (uncoloured default). */
	public static final RegistryObject<Item> CFOAM_SLAB_ITEM = ITEMS.register("cfoam_slab",
			() -> new BlockItem(CFOAM_SLAB.get(), new Item.Properties()));

	/** The properties pass straight through — the ctors take the BlockBehaviour base type. */
	private GT6FoamBlocks() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6FoamSprays.onModConstruct shape). */
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

	/** Registration smoke evidence (the GT6FoamSprays.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 cfoam blocks registered: cfoam_fresh/cfoam + slabs + owned BE {}",
				ForgeRegistries.BLOCKS.getKey(CFOAM_OWNED.get())));
	}

	/**
	 * The vanilla BUILDING_BLOCKS join (task p38-tabfix-c-misc — the GTGrassBlocks
	 * .onBuildTabContents form; the census adjudicates the C-Foam family decorative, the
	 * GTGrassBlocks/GT6TreeBlocks decorative-block precedent — the upstream tab mount was
	 * not traced (BlocksGT.CFoam unverified), so the pool cut IS the census ruling).
	 * Exactly the two DRIED BlockItems: the fresh blocks are spray-only intermediates
	 * (empty loot) and the owned carrier has no BlockItem (upstream showInCreative
	 * false, MultiTileEntityCFoam.java:152).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			aEvent.accept(new ItemStack(CFOAM_ITEM.get()));
			aEvent.accept(new ItemStack(CFOAM_SLAB_ITEM.get()));
		}
	}

	/** The dye index as the GT6 colour name segment ({@link GTSprayCanItem#DYE_IDS} order) — the stat/stat-line face. */
	public static String colorName(int aDyeIndex) {
		return GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}
}
