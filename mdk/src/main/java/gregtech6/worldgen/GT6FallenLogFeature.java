package gregtech6.worldgen;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The GT6 fallen-log worldgen Feature (task p30-w6-t2-surface-blocks) — the four
 * {@code WorldgenOnSurface} log classes (WorldgenLogDry/Rotten/Mossy/Frozen.java) folded
 * into ONE parametrised instance per wood, the shapes verbatim:
 * <ul>
 * <li>three shapes per placement roll (dry :58-79, rotten :59-83, mossy :60-89, frozen
 *     :58-79): the vertical pile {@code aY-1..aY+2} (+ the optional aY+3, the rotten
 *     water arm grows to aY+5 with depth), the X row {@code aY+1} {@code aX-2..aX+1}
 *     (+ the optional aX+2), the Z row likewise;</li>
 * <li>the mossy mushroom cap (setMushroom :94-102): red/brown vanilla mushrooms on the
 *     row tops, the Harvestcraft garden arm cut (mod absent);</li>
 * <li>the rotten water arm (:55 {@code anywater ? aY--}): in water the pile starts one
 *     below the surface and stacks up to above sea level;</li>
 * <li>the frozen snow arm (:56-57): the ground contact must carry a snow layer, the logs
 *     REPLACE the layer slot.</li>
 * </ul>
 *
 * <p>The anchor translation (the rocks/sticks live-scan finding): the modern placed
 * origin is the free slot ABOVE the ground contact (the heightmap ray stop), while the
 * upstream {@code tryPlaceStuff} receives the CONTACT itself — so {@code aY} maps to
 * {@code origin.below().getY()} (the dry/mossy/rotten-land face) or one deeper for the
 * snow/water contacts that own the surface slot.
 */
public final class GT6FallenLogFeature extends Feature<NoneFeatureConfiguration> {

    /** The four woods, {@link GT6Worldgen#FALLEN_LOG_PATHS} order. */
    public enum Kind { DRY, ROTTEN, MOSSY, FROZEN }

    private final Supplier<Block> mLog;
    private final Kind mKind;

    public GT6FallenLogFeature(Supplier<Block> aLog, Kind aKind) {
        super(NoneFeatureConfiguration.CODEC);
        mLog = aLog;
        mKind = aKind;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aCtx) {
        WorldGenLevel tLevel = aCtx.level();
        BlockPos tOrigin = aCtx.origin();
        RandomSource tRandom = aCtx.random();
        // The contact = the ray stop (origin is the free slot above it).
        BlockPos tContact = tOrigin.below();
        BlockState tGround = tLevel.getBlockState(tContact);
        boolean tWaterArm = mKind == Kind.ROTTEN && !tGround.getFluidState().isEmpty();
        int tContactY = tContact.getY();
        int tBaseY; // the upstream aY after the per-kind contact adjustments
        boolean tSandOk = mKind != Kind.ROTTEN; // rotten requires plantableGreens ONLY (:56); dry/mossy add sand (:56-58)
        if (tWaterArm) {
            tBaseY = tContactY - 1; // WorldgenLogRotten.java:55 the water arm's aY--
        } else if (mKind == Kind.FROZEN) {
            // :56-57 the snow face is mandatory — TWO anchor shapes (the live-scan
            // finding: MOTION_BLOCKING skips the 2px layer, blocksMotion = isSolid):
            if (tGround.is(Blocks.SNOW)) {
                // the heightmap COUNTED the layer: contact = snow slot, soil below it
                BlockState tSoil = tLevel.getBlockState(tContact.below());
                if (!isSoil(tSoil)) return false;
                tBaseY = tContactY - 1; // the logs start at the ground top
            } else if (tLevel.getBlockState(tOrigin).is(Blocks.SNOW)) {
                // the layer owns the ORIGIN slot: contact = the ground itself, the
                // rows replace the layer slot (the upstream set(aY+1) face verbatim)
                if (!isSoil(tGround)) return false;
                tBaseY = tContactY;
            } else {
                return false;
            }
        } else {
            if (!tGround.is(BlockTags.DIRT)
                    && (!tSandOk || !(tGround.is(Blocks.SAND) || tGround.is(Blocks.RED_SAND)))) return false;
            if (!tLevel.getBlockState(tOrigin).isAir()) return false; // WD.air at aY+1
            tBaseY = tContactY;
        }
        BlockState tLog = mLog.get().defaultBlockState();
        switch (tRandom.nextInt(3)) {
            case 0 -> { // the vertical pile (:60-66, + the rotten water extensions :66-68)
                if (tRandom.nextBoolean()) tLevel.setBlock(tOrigin.atY(tBaseY - 1), tLog, 2);
                for (int tY = tBaseY; tY <= tBaseY + 2; tY++) tLevel.setBlock(tOrigin.atY(tY), tLog, 2);
                int tSea = tLevel.getSeaLevel();
                if (mKind == Kind.ROTTEN) {
                    if (tBaseY < tSea || tRandom.nextBoolean()) tLevel.setBlock(tOrigin.atY(tBaseY + 3), tLog, 2);
                    if (tBaseY < tSea - 1) tLevel.setBlock(tOrigin.atY(tBaseY + 4), tLog, 2);
                    if (tBaseY < tSea - 2 && tRandom.nextBoolean()) tLevel.setBlock(tOrigin.atY(tBaseY + 5), tLog, 2);
                } else if (tRandom.nextBoolean()) {
                    tLevel.setBlock(tOrigin.atY(tBaseY + 3), tLog, 2);
                }
            }
            case 1 -> { // the X row (:67-73)
                int tY = tBaseY + 1;
                placeRow(tLevel, tRandom, tLog, tY, tOrigin.getZ(), tOrigin.getX(), Direction.Axis.X);
                if (mKind == Kind.MOSSY) for (int tX = tOrigin.getX() - 1; tX <= tOrigin.getX() + 1; tX++)
                    placeMushroom(tLevel, tRandom, new BlockPos(tX, tY + 1, tOrigin.getZ())); // :75-77
            }
            case 2 -> { // the Z row (:74-79 + the mossy caps :86-88)
                int tY = tBaseY + 1;
                placeRow(tLevel, tRandom, tLog, tY, tOrigin.getX(), tOrigin.getZ(), Direction.Axis.Z);
                if (mKind == Kind.MOSSY) for (int tZ = tOrigin.getZ() - 1; tZ <= tOrigin.getZ() + 1; tZ++)
                    placeMushroom(tLevel, tRandom, new BlockPos(tOrigin.getX(), tY + 1, tZ));
            }
        }
        return true;
    }

    /** One horizontal row: optional far arm, four core blocks, optional near arm (dry :67-71 shape). */
    private void placeRow(WorldGenLevel aLevel, RandomSource aRandom, BlockState aLog, int aY,
            int aFixed, int aCenter, Direction.Axis aAxis) {
        int tFrom = aRandom.nextBoolean() ? aCenter - 2 : aCenter - 1;
        int tTo = aRandom.nextBoolean() ? aCenter + 2 : aCenter + 1;
        for (int tI = tFrom; tI <= tTo; tI++) {
            BlockPos tPos = aAxis == Direction.Axis.X ? new BlockPos(tI, aY, aFixed) : new BlockPos(aFixed, aY, tI);
            aLevel.setBlock(tPos, aLog.setValue(RotatedPillarBlock.AXIS, aAxis), 2);
        }
    }

    /** The soil face (plantableGreens/sand — the dirt-tag family plus the sand pair). */
    private static boolean isSoil(BlockState aState) {
        return aState.is(BlockTags.DIRT) || aState.is(Blocks.SAND) || aState.is(Blocks.RED_SAND);
    }

    /** The mossy cap (setMushroom :94-102, the HaC arm cut): red/brown on a free slot. */
    private void placeMushroom(WorldGenLevel aLevel, RandomSource aRandom, BlockPos aPos) {
        if (!aLevel.getBlockState(aPos).isAir()) return;
        aLevel.setBlock(aPos, (aRandom.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM)
                .defaultBlockState(), 2);
    }
}
