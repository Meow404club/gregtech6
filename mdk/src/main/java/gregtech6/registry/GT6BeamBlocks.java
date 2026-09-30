package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.tree.GT6BeamBlock;
import gregtech6.block.tree.GT6BeamKind;

/**
 * Registration home of the GT6 wood-beam universe (task beam-blocks-register): <b>8
 * per-pair Block+BlockItem registrations</b> — beam x the 8 {@link GT6BeamKind} rows (the
 * vanilla subset of the upstream LIST_BEAMS walk: Beam1 meta 0-3 LoaderWoodDictionary.java:51-54,
 * Beam2 meta 0-1 :55-56 + the IL.Beam default face :66 + the Rubber Wood face :175; the
 * FireProof twins and the modded-wood BeamA/B/C/3 rows stay unported). The id scheme is
 * the single-source snake composition {@code <kind.snake>_beam} (the GT6TreeBlocks
 * {@code <kind>_log} rule), and the dropped/gathered item faces the downstream resolvers:
 * the coke-oven beam rows (Loader_Recipes_Woods.java:197-201), the wash debarking chain
 * (:167) and the beam saw/lathe/pulverize rows (:190-205) stay their own cards' domains —
 * this card only seats the items.
 *
 * <p>Creative tab: BUILDING_BLOCKS (the upstream tabBlock join, BlockBase.java:63), wired
 * through {@code BuildCreativeModeTabContentsEvent} (the GT6TreeBlocks.onBuildTabContents
 * shape). OM faces: upstream registers every beam meta into OD.beamWood
 * (BlockBaseBeam.java:48) and the generify target IL.Beam = Beam2 meta 3 — the
 * oredict/composition faces ride the prefix-registry seam, not this card.
 *
 * <p>Self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event (GT6TreeBlocks.onModConstruct shape; ADR-P3-4: GT6Mod stays untouched).
 *
 * <p>KJS surface (the card's declaration): REGISTRATION face only — the KubeJS typings
 * defer to the kjs binding card (the GT6Bumbles.java:71-72 precedent).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BeamBlocks {

    public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
    public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

    /** The 8 beam rows in upstream meta order (Beam1 0-3, then Beam2 0-3). */
    public static final List<GT6BeamKind> KINDS = List.of(GT6BeamKind.values());

    /** The 8 beam blocks, kind order. */
    public static final List<RegistryObject<Block>> BLOCKS = registerBlocks();

    /** The 8 beam block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> ITEMS = registerItems();

    /** The registry id of one beam: {@code <snake>_beam}. */
    public static String path(GT6BeamKind aKind) {
        return aKind.snake() + "_beam";
    }

    private static List<RegistryObject<Block>> registerBlocks() {
        List<RegistryObject<Block>> rList = new ArrayList<>(KINDS.size());
        for (GT6BeamKind tKind : KINDS) {
            rList.add(BLOCKS_REG.register(path(tKind), () -> new GT6BeamBlock(tKind)));
        }
        return List.copyOf(rList);
    }

    private static List<RegistryObject<Item>> registerItems() {
        List<RegistryObject<Item>> rList = new ArrayList<>(KINDS.size());
        for (int i = 0; i < BLOCKS.size(); i++) {
            int tIndex = i;
            RegistryObject<Block> tBlock = BLOCKS.get(tIndex);
            rList.add(ITEMS_REG.register(tBlock.getId().getPath(),
                    () -> new BlockItem(tBlock.get(), new Item.Properties())));
        }
        return List.copyOf(rList);
    }

    // ------------------------------------------------------------------ lifecycle

    private GT6BeamBlocks() {
    }

    /** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6TreeBlocks.onModConstruct shape). */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
        //? if forge {
        net.minecraftforge.eventbus.api.IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
        //?} else {
        /*net.minecraftforge.eventbus.api.IEventBus tModBus =
                net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
         *///?}
        BLOCKS_REG.register(tModBus);
        ITEMS_REG.register(tModBus);
    }

    /** The tab join: beams = building (BlockBase.java:63 the upstream tabBlock face). */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
        if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (RegistryObject<Item> tItem : ITEMS) {
                aEvent.accept(new ItemStack(tItem.get()));
            }
        }
    }
}
