package gregtech6.block.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 BALE block — the {@code gt.block.bale.grass} / {@code gt.block.bale.crop}
 * families (upstream BlockBaleGrass/BlockBaleCrop, Loader_Blocks.java:128-129, task
 * material-mc-g2-decor-misc). The upstream (variant 2 bits) x (pillar axis 2 bits) meta
 * ladder splits into FOUR per-variant blocks per family (the P8 ADR ④ per-variant item
 * identity ruling — the compact economy pours 9-grass-stack outputs per variant,
 * Loader_Recipes_Crops.java:127-140/:163-229) over a 1.20.1 {@link RotatedPillarBlock}
 * (the upstream PILLAR_RENDER = the vanilla log renderer, CS.java:764).
 *
 * <p>Numbers: the vanilla hay row verbatim (BlockBaseBale.java:50-52 — Blocks.hay_block
 * hardness/resistance/stack size), grass sound (both ctors soundTypeGrass), sword
 * harvest tool (BlockBaseBale.java:47 — the mineable/sword... 1.20.1 has no sword band,
 * the declared shears-shaped defer: the block joins NO mineable tag, hand-breakable at
 * hay hardness), flammability 150/150 (:57-58) on every variant.
 *
 * <p>THE TRANSFORM FACE (BlockBaleGrass.java:490-523, grass family only): the fresh and
 * moldy variants random-tick — in the Nether fresh turns DRY; in a wet environment fresh
 * turns MOLDY and moldy turns ROTTEN; elsewhere fresh turns DRY. The dry and rotten
 * variants are terminal. The upstream guard chain (nether, cold, sun-exposure, biome
 * wetness, adjacent water) maps onto: nether dimension, a temperature floor, a
 * day+sky+no-rain gate for the dry arm, a precipitation-biome or adjacent-water gate for
 * the wet arm — the 1.20.1 biome carries no rainfall field, so the wet gate is
 * {@code hasPrecipitation()} (the declared coarse proxy: the fresh/moldy/rotten ladder
 * still runs, the dry-biome edge widens; ponytail: the four-state economy over five
 * biome fields the modern registry no longer ships).
 */
public class GT6BaleBlock extends RotatedPillarBlock {

	/** The grass-family variant ordinal (the upstream meta&3 face). */
	public enum BaleVariant {
		FRESH, DRY, MOLDY, ROTTEN
	}

	/** The display names (the dump faces gt.block.bale.grass.0-3 / gt.block.bale.crop.0-3). */
	public static final String[] GRASS_NAMES = {"gt6.grass_bale.name", "gt6.dry_grass_bale.name",
			"gt6.moldy_grass_bale.name", "gt6.rotten_grass_bale.name"};
	/** The crop-family display names (Rye/Oats/Barley/Rice Bale). */
	public static final String[] CROP_NAMES = {"gt6.rye_bale.name", "gt6.oats_bale.name",
			"gt6.barley_bale.name", "gt6.rice_bale.name"};

	/** True for the grass family (the transform face owner; the crop family is inert). */
	public final boolean grass;
	/** The FIXED variant (the upstream meta&3). */
	public final BaleVariant variant;
	/** The registry snake id ({@code grass_bale} form). */
	public final String snake;

	public GT6BaleBlock(String aSnake, boolean aGrass, BaleVariant aVariant, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.grass = aGrass;
		this.variant = aVariant;
	}

	/** The vanilla hay row numbers (BlockBaseBale.java:50-52); flammability rides the overrides. */
	public static BlockBehaviour.Properties baleProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5F, 1.0F)
				.sound(SoundType.GRASS).randomTicks();
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(AXIS);
	}

	/** The composed display name — the per-variant dump face. */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(
				(this.grass ? GRASS_NAMES : CROP_NAMES)[this.variant.ordinal()]);
	}

	/** The wood flammability face (BlockBaseBale.java:57-58), the GT6TreePlankBlock route. */
	@Override
	public int getFlammability(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, net.minecraft.core.Direction aFace) {
		return 150;
	}

	@Override
	public int getFireSpreadSpeed(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, net.minecraft.core.Direction aFace) {
		return 150;
	}

	@Override
	public boolean isFireSource(BlockState aState, LevelReader aLevel, BlockPos aPos, net.minecraft.core.Direction aFace) {
		return false; // BlockBaseBale.java:56
	}

	/** The transform face (BlockBaleGrass.java:490-523) — grass family, fresh/moldy arms only. */
	@Override
	public void randomTick(BlockState aState, ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		if (!this.grass || this.variant == BaleVariant.DRY || this.variant == BaleVariant.ROTTEN) return;
		if (aRandom.nextInt(3) > 0) return; // the :500/:506 shared 1-in-3 gate
		Block tTarget = null;
		if (this.variant == BaleVariant.FRESH) {
			if (aLevel.dimension() == Level.NETHER) {
				tTarget = dry(); // the :501-504 nether arm
			} else {
				tTarget = tickFresh(aLevel, aPos, aRandom);
			}
		} else {
			tTarget = tickMoldy(aLevel, aPos, aRandom);
		}
		if (tTarget != null) {
			BlockState tState = tTarget.defaultBlockState().setValue(AXIS, aState.getValue(AXIS));
			aLevel.setBlock(aPos, tState, 3);
		}
	}

	/** The fresh arm (the :505-517 chain): dry in the cold/sunny-dry, moldy in the wet. */
	private Block tickFresh(ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		if (aLevel.getBiome(aPos).value().getBaseTemperature() < COLD_TEMP) return null; // ponytail: cold-floor proxy, the envTemp face
		if (aRandom.nextInt(3) > 0 && !(aLevel.isDay() && !aLevel.isRaining() && aLevel.canSeeSky(aPos.above(2)))) return null;
		if (isWet(aLevel, aPos, aRandom)) return moldy();
		return dry();
	}

	/** The moldy arm (:518-521): wet or slow-odds turns rotten. */
	private Block tickMoldy(ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		if (isWet(aLevel, aPos, aRandom) || aRandom.nextInt(42) == 0) return rotten();
		return null;
	}

	/** The wet gate: a precipitation biome or adjacent water (:508-509 + the rain arm). */
	private boolean isWet(ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		var tBiome = aLevel.getBiome(aPos).value();
		if (tBiome.hasPrecipitation()) return true; // ponytail: hasPrecipitation stands in for the rainfall>0.8 + raining arms
		for (net.minecraft.core.Direction tSide : net.minecraft.core.Direction.values()) {
			if (aLevel.getFluidState(aPos.relative(tSide)).is(net.minecraft.tags.FluidTags.WATER)) return true;
		}
		return false;
	}

	/** The 10 degrees-C cold floor (the envTemp < C+10 face), vanilla temperature proxy. */
	private static final float COLD_TEMP = 0.2F;

	private static Block dry()    { return gregtech6.registry.GT6DecorBlocks.GRASS_BALES.get(BaleVariant.DRY.ordinal()).get(); }
	private static Block moldy()  { return gregtech6.registry.GT6DecorBlocks.GRASS_BALES.get(BaleVariant.MOLDY.ordinal()).get(); }
	private static Block rotten() { return gregtech6.registry.GT6DecorBlocks.GRASS_BALES.get(BaleVariant.ROTTEN.ordinal()).get(); }
}
