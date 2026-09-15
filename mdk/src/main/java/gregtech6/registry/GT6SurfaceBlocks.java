package gregtech6.registry;

import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.block.surface.GT6SurfaceStickBlock;

/**
 * The surface deco block registrations (task p30-w6-rocks-sticks) — the card-owned
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister shape (the
 * {@link GT6FoamBlocks} precedent; GT6Mod/GTModBusListener untouched). Four blocks:
 * three per-material surface rocks (the first batch of the WorldgenRocks universe) and
 * the single zero-material surface stick (WorldgenSticks).
 *
 * <p>Behaviour numbers are the upstream MTE rows verbatim: hardness 0.25, blast
 * resistance 0 (MultiTileEntityRock.java:250/:249), no collision
 * (getCollisionBoundingBoxFromPool null :238), light-opaque 0 (:248) — the vanilla
 * stand-ins being {@code noCollission()} + {@code noOcclusion()} on a non-full micro
 * box. No creative tab, NO BlockItem: the upstream rock/stick is never obtainable as a
 * block (right-click collects the carried item, MultiTileEntityRock.java:143-148) — the
 * loot table is the only item path, so there is no item face to register.
 *
 * <p>Upstream first-batch transcription (WorldgenRocks.java:63, the NBT lottery into
 * per-pair blocks — the Feature weights in GT6WorldgenDatagen): half the placements carry
 * NO NBT = the default rock ({@code surface_rock_stone}, the overworld default MT.Stone,
 * MultiTileEntityRock.java:174); of the NBT half, 11/12 carry a flint item
 * ({@code surface_rock_flint}) and 1/12 carry MeteoricIron rockGt/oreRaw 3:1
 * ({@code surface_rock_meteorite}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6SurfaceBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");

	/** The default-rock block (upstream NBT-less placement, the MT.Stone overworld default). */
	public static final RegistryObject<Block> SURFACE_ROCK_STONE =
			BLOCKS.register("surface_rock_stone", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.STONE, SoundType.STONE), MT.Stone));

	/** The flint-carrying rock (upstream NBT = Items.flint, 11/12 of the NBT half). */
	public static final RegistryObject<Block> SURFACE_ROCK_FLINT =
			BLOCKS.register("surface_rock_flint", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.Flint));

	/** The meteoric-iron rock (upstream NBT = MeteoricIron rockGt/oreRaw, 1/12 of the NBT half). */
	public static final RegistryObject<Block> SURFACE_ROCK_METEORITE =
			BLOCKS.register("surface_rock_meteorite", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.MeteoricIron));

	/** The surface stick (MTE 32756, the single zero-material block). */
	public static final RegistryObject<Block> SURFACE_STICK =
			BLOCKS.register("surface_stick", () -> new GT6SurfaceStickBlock(surfaceProperties(MapColor.WOOD, SoundType.WOOD)));

	/** All four, registration order — the census/exemption walk unit (GT6LangParityTest, Jade name face). */
	public static final List<RegistryObject<Block>> ALL = List.of(
			SURFACE_ROCK_STONE, SURFACE_ROCK_FLINT, SURFACE_ROCK_METEORITE, SURFACE_STICK);

	/** The shared behaviour properties (MultiTileEntityRock.java:238/:248/:249/:250 verbatim). */
	private static BlockBehaviour.Properties surfaceProperties(MapColor aColor, SoundType aSound) {
		return BlockBehaviour.Properties.of()
				.mapColor(aColor)
				.strength(0.25F, 0.0F) // hardness 0.25 (getBlockHardness :250), blast 0 (getExplosionResistance2 :249)
				.sound(aSound)
				.noCollission() // the upstream collision box is null (:238)
				.noOcclusion() // light opacity none (:248); the micro box never occludes
				.pushReaction(PushReaction.DESTROY); // the deco walks like the vanilla flower row
	}

	private GT6SurfaceBlocks() {
	}

	/** The construct-event attach (the GT6FoamBlocks.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS.register(tModBus);
	}
}
