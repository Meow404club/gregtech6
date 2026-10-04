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
 * Registration home of the GT6 wood-beam universe (task beam-blocks-register +
 * beam-fireproof-closeout): <b>42 per-pair Block+BlockItem registrations</b> — beam x the
 * 21 {@link GT6BeamKind} rows (the FULL upstream LIST_BEAMS block set, Loader_Woods.java:
 * 48-61: Beam1 meta 0-3 LoaderWoodDictionary.java:51-54, Beam2 meta 0-1 :55-56 + the
 * IL.Beam default face :66 + the Rubber Wood face :175, Beam3 x4 :60 the mod-wood faces,
 * BeamA x4 :48 + BeamB x4 :50 + BeamC x1 :52 the GT-tree 0-8 faces) <b>+ the 21 FireProof
 * twins</b> (Loader_Woods.java:49/:51/:53/:57/:59/:61, {@code BlockTreeBeam*FireProof}
 * extends BlockBaseBeam — the same texture arrays, the flammability face off,
 * GT6BeamBlock's fireproof flag). The id scheme is the single-source snake composition
 * {@code <kind.snake>_beam} (+ {@code _beam_fireproof}, the upstream id tail
 * "gt.block.beam.1.fireproof"), and the dropped/gathered item faces the downstream
 * resolvers: the coke-oven beam rows (Loader_Recipes_Woods.java:197-201), the wash
 * debarking chain (:167) and the beam saw/lathe/pulverize rows (:190-205) stay their own
 * cards' domains — this card only seats the items (the 13 new kinds' consume walk is the
 * declared recipe-backfill unpoured face).
 *
 * <p>Creative tab: BUILDING_BLOCKS (the upstream tabBlock join, BlockBase.java:63), wired
 * through {@code BuildCreativeModeTabContentsEvent} (the GT6TreeBlocks.onBuildTabContents
 * shape). OM faces: upstream registers every beam meta of EVERY beam block (flammable AND
 * fireproof — the FireProof subclasses ride the same BlockBaseBeam constructor) into
 * OD.beamWood (BlockBaseBeam.java:48) and the generify target IL.Beam = Beam2 meta 3 —
 * the oredict/composition faces ride the prefix-registry seam, not this card.
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

    /** The 21 beam rows in upstream meta order (Beam1 0-3, Beam2 0-3, Beam3 0-3, BeamA 0-3, BeamB 0-3, BeamC 0). */
    public static final List<GT6BeamKind> KINDS = List.of(GT6BeamKind.values());

    /** The 21 flammable beam blocks, kind order. */
    public static final List<RegistryObject<Block>> BLOCKS = registerBlocks();

    /** The 21 FireProof twin blocks, kind order (Loader_Woods.java:49-61). */
    public static final List<RegistryObject<Block>> FIREPROOF_BLOCKS = registerBlocks(true);

    /** The 21 beam block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> ITEMS = registerItems(false);

    /** The 21 FireProof twin block items, kind order (the tab walk rides after the flammable set). */
    public static final List<RegistryObject<Item>> FIREPROOF_ITEMS = registerItems(true);

    /** The registry id of one beam: {@code <snake>_beam}. */
    public static String path(GT6BeamKind aKind) {
        return aKind.snake() + "_beam";
    }

    /** The registry id of one FireProof twin: {@code <snake>_beam_fireproof} (the upstream id tail "gt.block.beam.1.fireproof"). */
    public static String fireproofPath(GT6BeamKind aKind) {
        return path(aKind) + "_fireproof";
    }

    private static List<RegistryObject<Block>> registerBlocks() {
        return registerBlocks(false);
    }

    private static List<RegistryObject<Block>> registerBlocks(boolean aFireproof) {
        List<RegistryObject<Block>> rList = new ArrayList<>(KINDS.size());
        for (GT6BeamKind tKind : KINDS) {
            rList.add(aFireproof
                    ? BLOCKS_REG.register(fireproofPath(tKind), () -> new GT6BeamBlock(tKind, true))
                    : BLOCKS_REG.register(path(tKind), () -> new GT6BeamBlock(tKind)));
        }
        return List.copyOf(rList);
    }

    private static List<RegistryObject<Item>> registerItems(boolean aFireproof) {
        List<RegistryObject<Block>> tBlocks = aFireproof ? FIREPROOF_BLOCKS : BLOCKS;
        List<RegistryObject<Item>> rList = new ArrayList<>(tBlocks.size());
        for (int i = 0; i < tBlocks.size(); i++) {
            int tIndex = i;
            RegistryObject<Block> tBlock = tBlocks.get(tIndex);
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

    /** The tab join: beams = building (BlockBase.java:63 the upstream tabBlock face); the FireProof twins ride after the flammable set. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
        if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (RegistryObject<Item> tItem : ITEMS) {
                aEvent.accept(new ItemStack(tItem.get()));
            }
            for (RegistryObject<Item> tItem : FIREPROOF_ITEMS) {
                aEvent.accept(new ItemStack(tItem.get()));
            }
        }
    }
}
