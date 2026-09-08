package gregtech6.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;

/**
 * The worldgen Feature registration home — the L1 large-vein pipeline skeleton (task
 * p26-worldgen-pipeline-skeleton), self-contained in the GT6Attachments/GT6Kinetics
 * shape: a card-owned {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event — GT6Mod.java / GTModBusListener.java stay untouched.
 *
 * <p>L0 (this card) registers ZERO entries: the 17 stone blobs are pure datagen over the
 * vanilla {@code Feature.ORE} + {@code OreConfiguration} (GT6WorldgenDatagen), no custom
 * Feature/codec exists yet. The register lives here so the L1 vein card (the single
 * custom {@code Feature<GTVeinConfig>} + JSON vein-table port of WorldgenOresLarge's
 * 4-material layering, WorldgenOresLarge.java:46-137) drops its
 * {@code FEATURES.register(...)} row into an existing wiring. A
 * {@code GT6WorldgenPlacements} DeferredRegister (custom PlacementModifier seam) is
 * deliberately NOT created until L1 needs one — an empty second register is dead weight
 * (the spec's "GT6WorldgenPlacements(若需)" clause).
 *
 * <p>KJS face (the card declaration): configured/placed/biome-modifier JSONs are the
 * tier-a datapack-native surface; the Feature/codec registration here is the registry
 * face declared out of KJS scope (worldgen scripting is not a KJS standard capability —
 * the id452 "不考虑亦合规" clause).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Features {

    /** The L1 custom-feature register (vanilla {@code minecraft:feature} registry, the DeferredRegister.create(ResourceKey, modid) overload — both legs same shape). */
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "gt6");

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
