package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
//21.1: RegistryObject died with the class; DeferredHolder.create(key, id) is the same lazy
//handle. The explicit legs (not the swap table): RegistryObject<StructureType<?>> has the
//wildcard generic the table's per-element entries do not cover (the id258 to-串 lesson).
*///?}

import gregtech6.worldgen.dungeon.GT6DungeonPiece;
import gregtech6.worldgen.dungeon.GT6DungeonStructure;

/**
 * The structure registration home (task p38-dungeon-framework) — the GT6Features shape
 * (the card-owned {@code @EventBusSubscriber(MOD)} DeferredRegister pair attached from
 * the construct event): the dungeon {@code StructureType} (the codec dispatch face the
 * structure JSON's {@code "type": "gt6:dungeon"} resolves through) and the single
 * {@code StructurePieceType} all dungeon pieces serialize under (the chunk-NBT
 * {@code "id"} face, StructurePiece.createTag :92-100).
 *
 * <p>KJS face (the card declaration): the structure/structure_set/loot JSONs are the
 * tier-a datapack surface; these two registry rows are the registry face declared out
 * of KJS scope (worldgen scripting is not a KJS standard capability — the GT6Features
 * javadoc clause).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Structures {

    /** The vanilla {@code minecraft:structure_type} registry (DeferredRegister.create(ResourceKey, modid)). */
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, "gt6");

    /** The vanilla {@code minecraft:structure_piece} registry (the BuiltInRegistries.STRUCTURE_PIECE key). */
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, "gt6");

    /**
     * The typed constant (a wildcard-generic functional interface cannot be a lambda
     * target — the inference failure the first compile caught).
     */
    static final StructureType<GT6DungeonStructure> DUNGEON_TYPE = () -> GT6DungeonStructure.CODEC;

    /** The dungeon structure type — {@code gt6:dungeon}, the GT6DungeonStructure.CODEC dispatch. */
    //? if forge {
    public static final RegistryObject<StructureType<?>> DUNGEON_STRUCTURE_TYPE =
    //?} else {
    /*public static final DeferredHolder<StructureType<?>, StructureType<?>> DUNGEON_STRUCTURE_TYPE =
    *///?}
            STRUCTURE_TYPES.register("dungeon", () -> DUNGEON_TYPE);

    /**
     * The one dungeon piece type — every {@link GT6DungeonPiece} kind serializes under
     * this id (the kind rides the {@code gtKind} NBT string); the ContextlessType shape
     * (StructurePieceType.java:98-105, the load(CompoundTag) face).
     */
    static final StructurePieceType.ContextlessType DUNGEON_PIECE = GT6DungeonPiece::new;

    //? if forge {
    public static final RegistryObject<StructurePieceType> DUNGEON_PIECE_TYPE =
    //?} else {
    /*public static final DeferredHolder<StructurePieceType, StructurePieceType> DUNGEON_PIECE_TYPE =
    *///?}
            STRUCTURE_PIECES.register("dungeon_piece", () -> DUNGEON_PIECE);

    private GT6Structures() {
    }

    /** The construct-event attach (GT6Features.onModConstruct verbatim shape). */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onModConstruct(FMLConstructModEvent aEvent) {
        //? if forge {
        IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
        //?} else {
        /*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
        //21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
        //listener reaches the mod bus through its mod container (GT6Features fork verbatim).
        *///?}
        STRUCTURE_TYPES.register(tModBus);
        STRUCTURE_PIECES.register(tModBus);
    }
}
