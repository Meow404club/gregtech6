package gregtech6.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * Registration home of the GT6 NETHER surface-form block band (task p31-nether-lens-end-yield
 * spec ①, the coordinator-approved option A): 14 plain blocks carrying the three upstream
 * nether forms whose 1.7.10 carriers have no modern port —
 *
 * <ul>
 * <li>{@code gt6:dense_nether_quartz_ore} — the WorldgenNetherQuartz payload, upstream
 *     BlocksGT.RockOres meta 8 = OP.oreDense NetherQuartz (BlockRockOres.java:50
 *     ORE_MATERIALS[8], the oreDense prefix registration in the ctor);</li>
 * <li>{@code gt6:crystal_<material>} x12 — the WorldgenNetherCrystals payload, upstream
 *     BlocksGT.CrystalOres metas 0..11 in BlockCrystalOres.java:43 order (the 12 sulfides,
 *     the noise-picked identity);</li>
 * <li>{@code gt6:nether_red_clay} — the WorldgenNetherClay payload, upstream
 *     BlocksGT.Diggables meta 3 (Loader_Worldgen.java:599; the :594 overworld pit row is
 *     the same meta, disabled because "it's supposed to be only in the Nether").</li>
 * </ul>
 *
 * <p>MINIMAL CARRIERS (the GT6BedrockOreBlocks posture): plain {@link Block}, NO
 * BlockItem, NO lang, NO loot table ({@code noLootTable}), NO creative tab. DECLARED
 * DEFER (the acquisition-faces clause, the bedrock-card wording): these blocks are the
 * identity/scan carriers — the player-facing acquisition faces (drops, items, real
 * textures) ride a future nether-surface card; until then mining yields nothing and the
 * textures are vanilla stand-ins ({@link NetherOreKey#vanillaTexture}, the
 * shared-model band in GT6OreBlockStates). NO oredict registration either: the OP.oreDense/OP.crystal
 * prefix universe does not exist in the port yet — it is part of the same deferred face.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6NetherOres {

    /** One band row: the registry path tail + the carrying material (supplier — this class loads pre-OP.init, the GT6BedrockOreBlocks.MaterialSlot lesson) + the vanilla stand-in texture. */
    public record NetherOreKey(String path, java.util.function.Supplier<OreDictMaterial> material, String vanillaTexture) {

        /** The resolved material (the census/test/runtime face). */
        public OreDictMaterial resolve() {
            return material.get();
        }
    }

    /** The band, upstream identity order: quartz first, then the 12 crystal metas (BlockCrystalOres.java:43 order), then the red clay. */
    public static final List<NetherOreKey> KEYS = List.of(
            new NetherOreKey("dense_nether_quartz_ore", () -> MT.NetherQuartz, "block/nether_quartz_ore"),
            new NetherOreKey("crystal_arsenopyrite" , () -> MT.OREMATS.Arsenopyrite, "block/amethyst_block"), // :43 meta 0
            new NetherOreKey("crystal_chalcopyrite" , () -> MT.OREMATS.Chalcopyrite, "block/amethyst_block"), // :43 meta 1
            new NetherOreKey("crystal_cinnabar"     , () -> MT.OREMATS.Cinnabar, "block/amethyst_block"), // :43 meta 2
            new NetherOreKey("crystal_cobaltite"    , () -> MT.OREMATS.Cobaltite, "block/amethyst_block"), // :43 meta 3
            new NetherOreKey("crystal_galena"       , () -> MT.OREMATS.Galena, "block/amethyst_block"), // :43 meta 4
            new NetherOreKey("crystal_kesterite"    , () -> MT.OREMATS.Kesterite, "block/amethyst_block"), // :43 meta 5
            new NetherOreKey("crystal_molybdenite"  , () -> MT.OREMATS.Molybdenite, "block/amethyst_block"), // :43 meta 6
            new NetherOreKey("crystal_pyrite"       , () -> MT.Pyrite, "block/amethyst_block"), // :43 meta 7
            new NetherOreKey("crystal_sphalerite"   , () -> MT.OREMATS.Sphalerite, "block/amethyst_block"), // :43 meta 8
            new NetherOreKey("crystal_stannite"     , () -> MT.OREMATS.Stannite, "block/amethyst_block"), // :43 meta 9
            new NetherOreKey("crystal_stibnite"     , () -> MT.OREMATS.Stibnite, "block/amethyst_block"), // :43 meta 10
            new NetherOreKey("crystal_tetrahedrite" , () -> MT.OREMATS.Tetrahedrite, "block/amethyst_block"), // :43 meta 11
            new NetherOreKey("nether_red_clay"      , () -> MT.Clay, "block/packed_mud"));

    /** The runtime handle map, KEYS order. */
    //? if forge {
    private static final java.util.Map<String, net.minecraftforge.registries.RegistryObject<Block>> BLOCKS = new java.util.LinkedHashMap<>();
    //?} else {
    /*private static final java.util.Map<String, net.neoforged.neoforge.registries.DeferredHolder<Block, Block>> BLOCKS = new java.util.LinkedHashMap<>();
     *///?}
    /** Defensive dedup across re-fired RegisterEvents (ADR-P2-2 fix 1). */
    private static final Set<ResourceLocation> REGISTERED_BLOCK_IDS = new HashSet<>();

    private GT6NetherOres() {
    }

    /** The registered block of a band path, or null (the invalid-row posture). */
    public static Block block(String aPath) {
        var tHandle = BLOCKS.get(aPath);
        return tHandle == null ? null : tHandle.get();
    }

    /** RegisterEvent, LOW priority, the BLOCK segment only (GT6BedrockOreBlocks.onRegister shape; no items, no tab). */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void onRegister(net.minecraftforge.registries.RegisterEvent event) {
        if (event.getRegistryKey() != Registries.BLOCK) return;
        for (NetherOreKey tKey : KEYS) {
            ResourceLocation tLoc = ResourceLocation.fromNamespaceAndPath("gt6", tKey.path());
            if (!REGISTERED_BLOCK_IDS.add(tLoc)) continue; // defensive dedup, ADR-P2-2 fix 1
            //? if forge {
            net.minecraftforge.registries.RegistryObject<Block> tHandle =
                    net.minecraftforge.registries.RegistryObject.create(tLoc, Registries.BLOCK, "gt6");
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Block, Block> tHandle =
                    net.neoforged.neoforge.registries.DeferredHolder.create(Registries.BLOCK, tLoc);
             *///?}
            event.register(Registries.BLOCK, tLoc, () -> new Block(properties(tKey)));
            BLOCKS.put(tKey.path(), tHandle);
        }
    }

    /**
     * The stand-in block properties: the upstream carriers' feel at zero oredict/tool
     * surface — RockOres meta8 pickaxe-stone (BlockRockOres.java:50 HARDNESS 1.0), the
     * crystals glass-brittle (BlockCrystalOres Material.glass), the diggable ground-soft
     * (BlockDiggable.java:50 Material.ground/soundTypeGravel).
     */
    private static Properties properties(NetherOreKey aKey) {
        if (aKey.path().startsWith("crystal_")) return Properties.of()
                .mapColor(MapColor.COLOR_PURPLE).strength(0.3F).sound(SoundType.AMETHYST).noLootTable();
        if (aKey.path().equals("nether_red_clay")) return Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(0.6F).sound(SoundType.GRAVEL).noLootTable();
        return Properties.of()
                .mapColor(MapColor.STONE).strength(1.0F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops().noLootTable();
    }
}
