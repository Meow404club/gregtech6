package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6TreeBlocks;

/**
 * The worldgen Feature registration home — the L1 large-vein pipeline skeleton (task
 * p26-worldgen-pipeline-skeleton), self-contained in the GT6Attachments/GT6Kinetics
 * shape: a card-owned {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event — GT6Mod.java / GTModBusListener.java stay untouched.
 *
 * <p>L0 registered ZERO entries (the 17 stone blobs are pure datagen over the vanilla
 * {@code Feature.ORE} + {@code OreConfiguration}). Task p30-w6-t1-trees-nine lands the
 * FIRST custom features: the nine {@link GT6TreeFeature} instances (one per
 * {@link GT6TreeKind}, the grow-semantics option-b ruling) — the sapling grower and the
 * placed feature run the same code through the configured-feature key
 * ({@code GT6Worldgen.treeConfiguredKey}). The L1 vein card still drops its rows into
 * this register.
 *
 * <p>KJS face (the card declaration): configured/placed/biome-modifier JSONs are the
 * tier-a datapack-native surface; the Feature/codec registration here is the registry
 * face declared out of KJS scope (worldgen scripting is not a KJS standard capability —
 * the id452 "不考虑亦合规" clause).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Features {

    /** The custom-feature register (vanilla {@code minecraft:feature} registry, the DeferredRegister.create(ResourceKey, modid) overload — both legs same shape). */
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "gt6");

    /**
     * The 9 tree feature INSTANCES, GT6TreeBlocks.KINDS order — registry ids {@code
     * tree_<snake>} (the GT6Worldgen.treeEntryPath scheme over the FEATURE registry: the
     * configured JSON's "type" field is this id). The list holds the plain instances (NOT
     * registry handles): datagen serializes the instance itself, and the handle face stays
     * inside the DeferredRegister (the stonecutter RegistryObject swap table is typed to
     * concrete element classes — the wildcard Feature<?> is the ponytail-avoided shape).
     */
    public static final List<Feature<?>> TREE_FEATURES = registerTreeFeatures();

    private static List<Feature<?>> registerTreeFeatures() {
        List<Feature<?>> rList = new ArrayList<>(GT6TreeBlocks.KINDS.size());
        for (GT6TreeKind tKind : GT6TreeBlocks.KINDS) {
            Feature<?> tFeature = new GT6TreeFeature(tKind);
            FEATURES.register(GT6Worldgen.treeEntryPath(tKind.snake()), () -> tFeature);
            rList.add(tFeature);
        }
        return List.copyOf(rList);
    }

    /**
     * The L1 large-vein feature (task p30-w6-t3-large-veins) — the reserved seam fulfilled:
     * ONE registration row {@code gt6:large_veins}, the single Feature over the 40-row JSON
     * vein table (the card spec ①/② — 40 separate configured features would lose the
     * weight-for-exactly-one draw semantics, GT6WorldGenerator.java:93-103). The
     * configured/placed/biome-modifier rows hang off it in GT6WorldgenDatagen.
     */
    public static final GT6LargeVeinFeature LARGE_VEINS = registerLargeVeinFeature();

    private static GT6LargeVeinFeature registerLargeVeinFeature() {
        GT6LargeVeinFeature tFeature = new GT6LargeVeinFeature();
        FEATURES.register("large_veins", () -> tFeature);
        return tFeature;
    }

    /** The feature instance of a kind (index-aligned with KINDS). */
    public static GT6TreeFeature treeFeature(GT6TreeKind aKind) {
        return (GT6TreeFeature) TREE_FEATURES.get(aKind.ordinal());
    }

    /**
     * The 4 fallen-log feature instances (task p30-w6-t2-surface-blocks), dry/rotten/mossy
     * /frozen — registry ids {@code log_<kind>} ({@link GT6Worldgen#FALLEN_LOG_PATHS}).
     * The blocks resolve through the supplier at worldgen/datagen use time (the
     * DeferredRegister entries are NOT up at this class's static init).
     */
    public static final List<Feature<?>> FALLEN_LOG_FEATURES = registerFallenLogFeatures();

    private static List<Feature<?>> registerFallenLogFeatures() {
        List<Feature<?>> rList = new ArrayList<>(GT6Worldgen.FALLEN_LOG_PATHS.size());
        GT6FallenLogFeature.Kind[] tKinds = GT6FallenLogFeature.Kind.values();
        for (int i = 0; i < GT6Worldgen.FALLEN_LOG_PATHS.size(); i++) {
            int tIndex = i;
            Feature<?> tFeature = new GT6FallenLogFeature(
                    () -> gregtech6.registry.GT6SurfaceBlocks.FALLEN_LOGS.get(tIndex).get(), tKinds[i]);
            FEATURES.register(GT6Worldgen.FALLEN_LOG_PATHS.get(i), () -> tFeature);
            rList.add(tFeature);
        }
        return List.copyOf(rList);
    }

    private GT6Features() {
    }

    /** The construct-event attach (GT6Attachments.onModConstruct verbatim shape). */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onModConstruct(FMLConstructModEvent aEvent) {
        //? if forge {
        IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
        //?} else {
        /*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
        //21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
        //listener reaches the mod bus through its mod container (GT6Attachments fork verbatim).
        *///?}
        FEATURES.register(tModBus);
    }
}
