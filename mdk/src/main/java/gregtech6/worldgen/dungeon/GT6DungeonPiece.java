package gregtech6.worldgen.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.FluidState;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GT6Structures;
import gregtech6.registry.GTStoneBlocks;

/**
 * The shelter-dungeon PIECE (task p38-dungeon-framework) — one class over the shipped
 * room kinds, each an upstream {@code IDungeonChunk} port (tmp/gt6-1.7.10
 * gregapi/worldgen/dungeon/):
 * <ul>
 * <li>{@link Kind#ROOM_EMPTY} — {@code DungeonChunkRoomEmpty} (:204): the 16×16×8 shell,
 *     open boundary doorways on connected sides;</li>
 * <li>{@link Kind#CORRIDOR} — {@code DungeonChunkCorridor} (:190): the 6-wide arm piece,
 *     connection-count agnostic (the Corridor3/4 loot variants defer);</li>
 * <li>{@link Kind#ENTRANCE} — {@code DungeonChunkEntrance} (:338): the shell + the
 *     surface shaft with the two spiral slab staircases;</li>
 * <li>{@link Kind#STORAGE} — {@code DungeonChunkRoomStorage} (:215) over
 *     {@code DungeonChunkRoomVault} (:34): the vault shell (open boundary doorways) +
 *     the piston airlocks on every connected side + the loot chests. The upstream Vault
 *     class IS the shell+door base Storage extends, so one kind carries both ports; the
 *     DEAD_END pool this card ships is exactly this room (the five portal rooms are the
 *     mod-专属 never pool, unported by ruling).</li>
 * </ul>
 *
 * <p>Every block goes through world coordinates with the chunk-clip {@code BoundingBox}
 * of {@link #postProcess} — the native 1.20.1 replacement for the 1.7.10 cross-chunk
 * setBlock walk. Piece orientation stays null.
 *
 * <p>Block vocabulary mapping (upstream DungeonData helpers → modern): bricks/smooth/
 * tiles/smalltiles/chiseled/redstoned → the GT6 per-pair stone blocks of two random
 * stones (the y==2 LOCAL belt wears the secondary stone, DungeonData.set :353 verbatim);
 * randomBricks (meta 3..5) → BRICK/CRACK/MBRIK; glassglow → glowstone; lamp → a LIT
 * redstone lamp (the 1.7.10 lit_redstone_lamp face) over a redstone-brick; colored →
 * vanilla concrete of the dungeon accent color; the stair slabs → stone brick slabs
 * (the upstream GT slabs). The MTE faces defer with the room-batch cards: the corridor
 * shelves 32110 + coins + cups + safes (Corridor3's loot nooks), the storage crate/tank
 * stacks (replaced by the chests), the Hand Crank MTE 32111 (replaced by the airlock
 * lever) — declared boundaries, this javadoc is the record.
 *
 * <p>The piston airlock (upstream {@code DungeonChunkDoorPiston}, :348) is rebuilt as an
 * explicitly-correct 1.20.1 circuit at the upstream anchor coordinates: 4 sticky pistons
 * embedded in the door wall (the z6/z9 wall columns, facing the 7..8 gap), extended by
 * default (the upstream door-closed posture), dust on the piston bases at the upstream
 * wire ring coordinates (14,3,6)/(14,3,9) + the (14,4,7)/(14,4,8) ring — the bases never
 * move so the dust survives both door states — and a floor LEVER at the lintel where the
 * upstream Hand Crank MTE stood (the crank's redstone face is a manual power source; the
 * lever is the vanilla-native equivalent). Lever ON = door shut, OFF = open.
 */
public class GT6DungeonPiece extends StructurePiece {

    /** The shipped room kinds (the upstream IDungeonChunk classes, see the class javadoc). */
    public enum Kind {
        ROOM_EMPTY, CORRIDOR, ENTRANCE, STORAGE;

        static Kind of(String aName) {
            return valueOf(aName);
        }
    }

    /** The loot table of the storage-vault chests (the upstream DUNGEON_CHEST category face). */
    public static final String DUNGEON_CHEST_TABLE = "chests/dungeon_chest";

    private final Kind mKind;
    /** The door side bitfield — bit per horizontal direction (2D data value order E,W,S,N). */
    private final byte mDoors;
    private final String mPrimaryStone, mSecondaryStone;
    private final int mColor;
    /** The entrance-only aligned surface cap (WORLD Y). */
    private final int mShaftTop;

    /** The worldgen ctor. */
    public GT6DungeonPiece(Kind aKind, BoundingBox aBox, byte aDoors, String aPrimary, String aSecondary, int aColor, int aShaftTop) {
        super(GT6Structures.DUNGEON_PIECE_TYPE.get(), 0, aBox);
        mKind = aKind;
        mDoors = aDoors;
        mPrimaryStone = aPrimary;
        mSecondaryStone = aSecondary;
        mColor = aColor;
        mShaftTop = aShaftTop;
    }

    /** The chunk-NBT load ctor. */
    public GT6DungeonPiece(CompoundTag aTag) {
        super(GT6Structures.DUNGEON_PIECE_TYPE.get(), aTag);
        mKind = Kind.of(aTag.getString("gtKind"));
        mDoors = aTag.getByte("gtDoors");
        mPrimaryStone = aTag.getString("gtPrimary");
        mSecondaryStone = aTag.getString("gtSecondary");
        mColor = aTag.getInt("gtColor");
        mShaftTop = aTag.getInt("gtShaftTop");
    }

    /** The door bitfield for a layout cell. */
    public static byte doorsOf(byte[][] aLayout, int aI, int aJ) {
        byte rDoors = 0;
        for (Direction tSide : Direction.Plane.HORIZONTAL) {
            if (GT6DungeonLayout.connected(aLayout, aI, aJ, tSide)) {
                rDoors |= (byte) (1 << tSide.get2DDataValue());
            }
        }
        return rDoors;
    }

    private boolean doorAt(Direction aSide) {
        return (mDoors & (1 << aSide.get2DDataValue())) != 0;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext aContext, CompoundTag aTag) {
        aTag.putString("gtKind", mKind.name());
        aTag.putByte("gtDoors", mDoors);
        aTag.putString("gtPrimary", mPrimaryStone);
        aTag.putString("gtSecondary", mSecondaryStone);
        aTag.putInt("gtColor", mColor);
        aTag.putInt("gtShaftTop", mShaftTop);
    }

    @Override
    public void postProcess(
            WorldGenLevel aLevel, StructureManager aStructure, ChunkGenerator aGenerator, RandomSource aRandom,
            BoundingBox aClip, ChunkPos aChunkPos, BlockPos aAnchor) {
        switch (mKind) {
            case ENTRANCE -> buildEntrance(aLevel, aClip, aRandom);
            case CORRIDOR -> buildCorridor(aLevel, aClip, aRandom);
            case STORAGE -> {
                buildRoomShell(aLevel, aClip, aRandom);
                for (Direction tSide : Direction.Plane.HORIZONTAL) if (doorAt(tSide)) {
                    buildAirlock(aLevel, aClip, tSide);
                }
                buildChests(aLevel, aClip, aRandom);
            }
            default -> buildRoomShell(aLevel, aClip, aRandom);
        }
    }

    // ---------------------------------------------------------------- coordinates

    private int wx(int aLX) {
        return this.boundingBox.minX() + aLX;
    }

    private int wz(int aLZ) {
        return this.boundingBox.minZ() + aLZ;
    }

    /** LOCAL y 0 = DUNGEON_Y (20) — the upstream DungeonData frame. */
    private int wy(int aLY) {
        return GT6DungeonLayout.DUNGEON_Y + aLY;
    }

    private void set(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ, BlockState aState) {
        BlockPos tPos = new BlockPos(wx(aLX), wy(aLY), wz(aLZ));
        if (aClip.isInside(tPos)) {
            aLevel.setBlock(tPos, aState, 2);
            FluidState tFluid = aLevel.getFluidState(tPos);
            if (!tFluid.isEmpty()) {
                aLevel.scheduleTick(tPos, tFluid.getType(), 0);
            }
        }
    }

    private void air(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, Blocks.CAVE_AIR.defaultBlockState());
    }

    // ---------------------------------------------------------------- block faces

    /** The GT6 stone block of a (stone, variant) pair — the registry is live at worldgen time. */
    private BlockState stone(StoneVariant aVariant, boolean aPrimary) {
        String tSnake = aPrimary ? mPrimaryStone : mSecondaryStone;
        return GTStoneBlocks.block(GTStoneBlocks.path(tSnake, aVariant)).get().defaultBlockState();
    }

    /**
     * The upstream {@code DungeonData.set} face (:353): the y==2 LOCAL belt wears the
     * SECONDARY stone, everything else the primary.
     */
    private BlockState face(StoneVariant aVariant, int aLY, boolean aPrimary) {
        return stone(aVariant, aLY != 2 && aPrimary);
    }

    /** The upstream {@code bricks} face (randomBricks, meta 3..5): BRICK/CRACK/MBRIK. */
    private BlockState randBrick(RandomSource aRandom, int aLY) {
        return switch (aRandom.nextInt(3)) {
            case 0 -> face(StoneVariant.CRACK, aLY, true);
            case 1 -> face(StoneVariant.MBRIK, aLY, true);
            default -> face(StoneVariant.BRICK, aLY, true);
        };
    }

    /** The upstream {@code lamp(x, y, z, +1)} face: lit lamp + redstone-brick above. */
    private void lamp(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY + 1, aLZ, face(StoneVariant.RSTBR, aLY + 1, true));
        set(aLevel, aClip, aLX, aLY, aLZ, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
    }

    private void bricks(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, face(StoneVariant.BRICK, aLY, true));
    }

    private void smooth(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, face(StoneVariant.SMOTH, aLY, true));
    }

    private void chiseled(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, face(StoneVariant.CHISL, aLY, true));
    }

    private void tiles(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, face(StoneVariant.TILES, aLY, true));
    }

    private void smalltiles(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, face(StoneVariant.STILE, aLY, true));
    }

    private void redstoned(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, face(StoneVariant.RSTBR, aLY, true));
    }

    /** The vanilla concrete row — the 1.7.10 dye-meta order = the DyeColor enum order (meta 0 = white). */
    private static final Block[] CONCRETES = {
            Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLACK_CONCRETE};

    private void colored(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, CONCRETES[mColor & 15].defaultBlockState());
    }

    private void glowstone(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ) {
        set(aLevel, aClip, aLX, aLY, aLZ, Blocks.GLOWSTONE.defaultBlockState());
    }

    /** The upstream {@code WD.liquid || canBlockSeeTheSky} probe. */
    private boolean liquidOrSky(WorldGenLevel aLevel, int aWX, int aWY, int aWZ) {
        BlockPos tPos = new BlockPos(aWX, aWY, aWZ);
        return !aLevel.getFluidState(tPos).isEmpty() || aLevel.canSeeSky(tPos);
    }

    // ---------------------------------------------------------------- shared: pillar

    /**
     * The support column under a room/corridor (upstream DungeonChunkPillar :35-68): a
     * base plate when the 4×4 below is hollow, then alternating 4×4 fills and 6×6 plates
     * down to stable ground or y2 — the world reads are the upstream verbatim shape.
     */
    private void pillar(WorldGenLevel aLevel, BoundingBox aClip) {
        boolean tTemp = true;
        for (int tX = 6; tX <= 9 && tTemp; tX++) for (int tZ = 6; tZ <= 9 && tTemp; tZ++) {
            BlockPos tBelow = new BlockPos(wx(tX), wy(-1), wz(tZ));
            if (aLevel.getBlockState(tBelow).isSolidRender(aLevel, tBelow)) tTemp = false;
        }

        if (tTemp) for (int tX = 5; tX <= 10; tX++) for (int tZ = 5; tZ <= 10; tZ++) {
            smooth(aLevel, aClip, tX, -1, tZ);
            bricks(aLevel, aClip, tX, -2, tZ);
        }

        for (int tY = -3; wy(tY) >= 2 && tTemp; tY--) {
            tTemp = false;
            for (int tX = 6; tX <= 9 && !tTemp; tX++) for (int tZ = 6; tZ <= 9 && !tTemp; tZ++) {
                BlockState tState = aLevel.getBlockState(new BlockPos(wx(tX), wy(tY), wz(tZ)));
                if (tState.getBlock() instanceof FallingBlock || !tState.isSolidRender(aLevel, new BlockPos(wx(tX), wy(tY), wz(tZ)))) {
                    tTemp = true;
                }
            }
            if (tTemp) {
                for (int tX = 6; tX <= 9; tX++) for (int tZ = 6; tZ <= 9; tZ++) {
                    bricks(aLevel, aClip, tX, tY, tZ);
                }
            } else {
                for (int tX = 5; tX <= 10; tX++) for (int tZ = 5; tZ <= 10; tZ++) {
                    smooth(aLevel, aClip, tX, tY + 1, tZ);
                    bricks(aLevel, aClip, tX, tY, tZ);
                    bricks(aLevel, aClip, tX, tY - 1, tZ);
                    // the upstream bedrock-hardness gate (:61) — skip the under-plate at y2
                    if (wy(tY) > 2 || aLevel.getBlockState(new BlockPos(wx(tX), 2, wz(tZ))).getDestroySpeed(aLevel, new BlockPos(wx(tX), 2, wz(tZ))) >= 0) {
                        smooth(aLevel, aClip, tX, tY - 2, tZ);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- room shell

    /**
     * The 16×16×8 room shell (upstream DungeonChunkRoomEmpty :36-121): walls/floor/ceiling
     * in the primary stone, the deco-point lattice, the lit lamp band, the glow ceiling
     * probe, and the open boundary doorways on every connected side.
     */
    private void buildRoomShell(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        pillar(aLevel, aClip);

        for (int tX = 0; tX < 16; tX++) for (int tZ = 0; tZ < 16; tZ++) for (int tY = 0; tY <= 7; tY++) {
            if (tX == 0 || tX == 15 || tZ == 0 || tZ == 15 || tY == 0 || tY == 7) {
                if ((tX == 3 || tX == 6 || tX == 9 || tX == 12) && (tZ == 3 || tZ == 6 || tZ == 9 || tZ == 12)) {
                    if (tY == 0) {
                        chiseled(aLevel, aClip, tX, tY, tZ);
                    } else if (tY == 7) {
                        lamp(aLevel, aClip, tX, tY, tZ);
                    } else {
                        bricks(aLevel, aClip, tX, tY, tZ);
                    }
                } else {
                    if (tY == 0) {
                        tiles(aLevel, aClip, tX, tY, tZ);
                    } else if (tY == 7) {
                        smalltiles(aLevel, aClip, tX, tY, tZ);
                        tiles(aLevel, aClip, tX, tY + 1, tZ);
                    } else {
                        bricks(aLevel, aClip, tX, tY, tZ);
                    }
                }
            } else {
                air(aLevel, aClip, tX, tY, tZ);
            }
        }

        // the glow-ceiling probe (:126-134): liquid or sky above → the 6×6 glassglow band.
        if (liquidOrSky(aLevel, wx(8), wy(9), wz(8))) {
            for (int tX = 5; tX <= 10; tX++) for (int tZ = 5; tZ <= 10; tZ++) {
                if ((tX == 5 || tX == 10) && (tZ == 5 || tZ == 10)) {
                    chiseled(aLevel, aClip, tX, 7, tZ);
                    chiseled(aLevel, aClip, tX, 8, tZ);
                } else {
                    glowstone(aLevel, aClip, tX, 7, tZ);
                    glowstone(aLevel, aClip, tX, 8, tZ);
                }
            }
        }

        if (doorAt(Direction.EAST)) buildDoorway(aLevel, aClip, Direction.EAST);
        if (doorAt(Direction.WEST)) buildDoorway(aLevel, aClip, Direction.WEST);
        if (doorAt(Direction.SOUTH)) buildDoorway(aLevel, aClip, Direction.SOUTH);
        if (doorAt(Direction.NORTH)) buildDoorway(aLevel, aClip, Direction.NORTH);
    }

    /**
     * The side mapper: {@code u} = the wall-axis coordinate (15 = the positive-side
     * boundary, 0 = the negative-side boundary), {@code v} = the along-axis coordinate.
     * EAST (x=u, z=v), WEST (x=15-u, z=v), SOUTH (x=v, z=u), NORTH (x=v, z=15-u).
     */
    private int sideLX(Direction aSide, int aU, int aV) {
        return switch (aSide) {
            case EAST -> aU;
            case WEST -> 15 - aU;
            case SOUTH -> aV;
            default -> aV;
        };
    }

    private int sideLZ(Direction aSide, int aU, int aV) {
        return switch (aSide) {
            case EAST, WEST -> aV;
            case SOUTH -> aU;
            default -> 15 - aU;
        };
    }

    private void sideSet(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide, int aU, int aY, int aV, BlockState aState) {
        set(aLevel, aClip, sideLX(aSide, aU, aV), aY, sideLZ(aSide, aU, aV), aState);
    }

    /**
     * The open boundary doorway (upstream RoomEmpty :135-279, the four mirrored frames):
     * a parametric rewrite of the verbatim rows — frame edges v5/v10, gap v6..9, y0..4.
     */
    private void buildDoorway(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide) {
        int tWall = aSide == Direction.EAST || aSide == Direction.SOUTH ? 15 : 0;
        for (int tV = 5; tV <= 10; tV++) for (int tY = 0; tY <= 4; tY++) {
            if (tV == 5 || tV == 10) {
                if (tY == 0 || tY == 4) {
                    sideSet(aLevel, aClip, aSide, tWall, tY, tV, face(StoneVariant.CHISL, tY, true));
                } else {
                    sideSet(aLevel, aClip, aSide, tWall, tY, tV, face(StoneVariant.SMOTH, tY, true));
                }
            } else if (tY >= 1 && tY <= 3) {
                sideSet(aLevel, aClip, aSide, tWall, tY, tV, Blocks.CAVE_AIR.defaultBlockState());
            } else {
                sideSet(aLevel, aClip, aSide, tWall, tY, tV, face(StoneVariant.SMOTH, tY, true));
            }
        }
    }

    // ---------------------------------------------------------------- airlock

    /**
     * The piston airlock on one side (the {@code DungeonChunkDoorPiston} rebuild — see
     * the class javadoc for the circuit derivation and the lever-for-crank deviation).
     *
     * <p>Per side (depths rotate with the side; the numbers below are for the EAST side):
     * the narrowing rows at depth 13/14 (solid v5/v10 + the y3 seal row), the 4 sticky
     * pistons embedded at depth 14 (v6 facing +v, v9 facing -v, extended by default),
     * dust on the piston bases at the upstream wire coordinates (14,3,6)/(14,3,9) plus
     * the (14,4,7)/(14,4,8) lintel ring, the floor lever on the inner lintel (the Hand
     * Crank slot), the colored panel at depth 13 (the upstream (13,1..2,7..8) anchor),
     * and the boundary lamps over the gap (upstream lamp(15,4,7..8,+1)).
     */
    private void buildAirlock(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide) {
        // the along-axis push directions: E/W doors push along z, S/N doors along x.
        Direction tPlus = aSide == Direction.EAST || aSide == Direction.WEST ? Direction.SOUTH : Direction.EAST;
        Direction tMinus = tPlus.getOpposite();
        boolean tWestNorth = aSide == Direction.WEST || aSide == Direction.NORTH;
        int tWall = tWestNorth ? 1 : 14;  // the piston/wall depth
        int tInner = tWestNorth ? 2 : 13; // the inner depth (panel + lever + seal row)

        BlockState tPistonPlus = Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, tPlus).setValue(PistonBaseBlock.EXTENDED, Boolean.TRUE);
        BlockState tPistonMinus = Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, tMinus).setValue(PistonBaseBlock.EXTENDED, Boolean.TRUE);
        BlockState tDust = Blocks.REDSTONE_WIRE.defaultBlockState();
        BlockState tLever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACING, Direction.UP).setValue(LeverBlock.POWERED, Boolean.TRUE);

        // the narrowing rows: depth tWall (beside the pistons) and depth tInner, y1..2.
        for (int tY = 1; tY <= 2; tY++) for (int tV = 5; tV <= 10; tV++) {
            if (tV == 7 || tV == 8) continue;
            sideSet(aLevel, aClip, aSide, tInner, tY, tV, face(StoneVariant.SMOTH, tY, true));
            if (tV == 5 || tV == 10) {
                sideSet(aLevel, aClip, aSide, tWall, tY, tV, face(StoneVariant.SMOTH, tY, true));
            }
        }

        // the pistons: depth tWall, columns v6 (pushing +v) and v9 (pushing -v), extended.
        for (int tY = 1; tY <= 2; tY++) {
            sideSet(aLevel, aClip, aSide, tWall, tY, 6, tPistonPlus);
            sideSet(aLevel, aClip, aSide, tWall, tY, 9, tPistonMinus);
        }

        // the seal row + lintel: depth tInner y3 v5..10 (the upstream smooth(13,3,5..10)),
        // depth tWall y3 v7..8 (the lintel carrying the ring dust).
        for (int tV = 5; tV <= 10; tV++) {
            sideSet(aLevel, aClip, aSide, tInner, 3, tV, face(StoneVariant.SMOTH, 3, true));
        }
        for (int tV = 7; tV <= 8; tV++) {
            sideSet(aLevel, aClip, aSide, tWall, 3, tV, face(StoneVariant.SMOTH, 3, true));
        }

        // the dust: on the piston bases (upstream (14,3,6)/(14,3,9)) + the lintel ring
        // (upstream (14,4,7)/(14,4,8)).
        sideSet(aLevel, aClip, aSide, tWall, 3, 6, tDust);
        sideSet(aLevel, aClip, aSide, tWall, 3, 9, tDust);
        sideSet(aLevel, aClip, aSide, tWall, 4, 7, tDust);
        sideSet(aLevel, aClip, aSide, tWall, 4, 8, tDust);

        // the lever on the inner lintel (the Hand Crank slot) — ON by default = door shut.
        sideSet(aLevel, aClip, aSide, tInner, 4, 7, tLever);

        // the decorative colored panel in the passage (the upstream (13,1..2,7..8) anchor).
        for (int tY = 1; tY <= 2; tY++) for (int tV = 7; tV <= 8; tV++) {
            sideSet(aLevel, aClip, aSide, tInner, tY, tV, CONCRETES[mColor & 15].defaultBlockState());
        }

        // the boundary lamps over the gap (upstream lamp(15,4,7..8,+1)).
        for (int tV = 7; tV <= 8; tV++) {
            sideSet(aLevel, aClip, aSide, 15, 4, tV, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
            sideSet(aLevel, aClip, aSide, 15, 5, tV, face(StoneVariant.RSTBR, 5, true));
        }
    }

    // ---------------------------------------------------------------- corridor

    private void buildCorridor(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        // the pillar only when connected on both axes (upstream Corridor :37-41).
        boolean tX = doorAt(Direction.EAST) || doorAt(Direction.WEST);
        boolean tZ = doorAt(Direction.SOUTH) || doorAt(Direction.NORTH);
        if (tX && tZ) pillar(aLevel, aClip);

        // the core (:43-64).
        for (int tXc = 5; tXc <= 10; tXc++) for (int tZc = 5; tZc <= 10; tZc++) for (int tY = 0; tY <= 4; tY++) {
            if (tY == 0) {
                tiles(aLevel, aClip, tXc, tY, tZc);
            } else if (tXc == 5 || tXc == 10 || tZc == 5 || tZc == 10) {
                bricks(aLevel, aClip, tXc, tY, tZc);
            } else if (tY == 4) {
                if (liquidOrSky(aLevel, wx(tXc), wy(tY + 1), wz(tZc))) {
                    glowstone(aLevel, aClip, tXc, tY, tZc);
                } else {
                    bricks(aLevel, aClip, tXc, tY, tZc);
                }
            } else {
                air(aLevel, aClip, tXc, tY, tZc);
            }
        }

        // the core ceiling lamps (:105-116): glow probe over the four center columns.
        if (liquidOrSky(aLevel, wx(7), wy(4), wz(7)) || liquidOrSky(aLevel, wx(7), wy(4), wz(8))
                || liquidOrSky(aLevel, wx(8), wy(4), wz(7)) || liquidOrSky(aLevel, wx(8), wy(4), wz(8))) {
            glowstone(aLevel, aClip, 7, 4, 7);
            glowstone(aLevel, aClip, 7, 4, 8);
            glowstone(aLevel, aClip, 8, 4, 7);
            glowstone(aLevel, aClip, 8, 4, 8);
        } else {
            redstoned(aLevel, aClip, 7, 4, 7);
            set(aLevel, aClip, 7, 4, 8, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
            set(aLevel, aClip, 8, 4, 7, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
            redstoned(aLevel, aClip, 8, 4, 8);
        }

        // the four arms (upstream :118-180), parametric — the arm row + the end lamps.
        for (Direction tSide : Direction.Plane.HORIZONTAL) if (doorAt(tSide)) {
            buildCorridorArm(aLevel, aClip, tSide);
        }
    }

    /** One corridor arm to the chunk edge (the upstream per-side loops, mirrored). */
    private void buildCorridorArm(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide) {
        for (int tD = 10; tD <= 15; tD++) for (int tV = 5; tV <= 10; tV++) for (int tY = 0; tY <= 4; tY++) {
            boolean tWall = tV == 5 || tV == 10;
            if (tY == 0) {
                armSet(aLevel, aClip, aSide, tD, tV, tY, face(StoneVariant.TILES, tY, true));
            } else if (tWall) {
                armSet(aLevel, aClip, aSide, tD, tV, tY, face(StoneVariant.BRICK, tY, true));
            } else if (tY == 4) {
                if (liquidOrSky(aLevel, wx(armLX(aSide, tD, tV)), wy(tY + 1), wz(armLZ(aSide, tD, tV)))) {
                    armSet(aLevel, aClip, aSide, tD, tV, tY, Blocks.GLOWSTONE.defaultBlockState());
                } else {
                    armSet(aLevel, aClip, aSide, tD, tV, tY, face(StoneVariant.BRICK, tY, true));
                }
            } else {
                armSet(aLevel, aClip, aSide, tD, tV, tY, Blocks.CAVE_AIR.defaultBlockState());
            }
        }
        // the end-lamp row (upstream redstoned(13,4,6)/lamp(13,4,7..8)/redstoned(13,4,9)).
        armSet(aLevel, aClip, aSide, 13, 6, 4, face(StoneVariant.RSTBR, 4, true));
        armSet(aLevel, aClip, aSide, 13, 7, 4, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
        armSet(aLevel, aClip, aSide, 13, 8, 4, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
        armSet(aLevel, aClip, aSide, 13, 9, 4, face(StoneVariant.RSTBR, 4, true));
    }

    /** The arm loop coordinate for a side: EAST (x=d, z=v), WEST (x=15-d, z=v), SOUTH (x=v, z=d), NORTH (x=v, z=15-d). */
    private int armLX(Direction aSide, int aD, int aV) {
        return switch (aSide) {
            case EAST -> aD;
            case WEST -> 15 - aD;
            case SOUTH -> aV;
            default -> aV;
        };
    }

    private int armLZ(Direction aSide, int aD, int aV) {
        return switch (aSide) {
            case EAST, WEST -> aV;
            case SOUTH -> aD;
            default -> 15 - aD;
        };
    }

    private void armSet(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide, int aD, int aV, int aY, BlockState aState) {
        set(aLevel, aClip, armLX(aSide, aD, aV), aY, armLZ(aSide, aD, aV), aState);
    }

    // ---------------------------------------------------------------- entrance

    private void buildEntrance(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        pillar(aLevel, aClip);

        // the shell — the entrance deco lattice is {2,6,9,13} and the ceiling keeps the
        // shaft open at (6/9, 6/9) (upstream DungeonChunkEntrance :28-69).
        for (int tX = 0; tX < 16; tX++) for (int tZ = 0; tZ < 16; tZ++) for (int tY = 0; tY <= 7; tY++) {
            if (tX == 0 || tX == 15 || tZ == 0 || tZ == 15 || tY == 0 || tY == 7) {
                if ((tX == 2 || tX == 6 || tX == 9 || tX == 13) && (tZ == 2 || tZ == 6 || tZ == 9 || tZ == 13)) {
                    if (tY == 0) {
                        chiseled(aLevel, aClip, tX, tY, tZ);
                    } else if (tY == 7) {
                        if (!((tX == 6 || tX == 9) && (tZ == 6 || tZ == 9))) {
                            lamp(aLevel, aClip, tX, tY, tZ);
                        } else {
                            bricks(aLevel, aClip, tX, tY, tZ);
                            bricks(aLevel, aClip, tX, tY + 1, tZ);
                        }
                    } else {
                        bricks(aLevel, aClip, tX, tY, tZ);
                    }
                } else {
                    if (tY == 0) {
                        tiles(aLevel, aClip, tX, tY, tZ);
                    } else if (tY == 7) {
                        smalltiles(aLevel, aClip, tX, tY, tZ);
                    } else {
                        bricks(aLevel, aClip, tX, tY, tZ);
                    }
                }
            } else {
                air(aLevel, aClip, tX, tY, tZ);
            }
        }

        int tTop = mShaftTop - GT6DungeonLayout.DUNGEON_Y; // the LOCAL aligned cap

        // the shaft (:143-174): walls 3..12, the inner ring 6..9, the aligned cap.
        for (int tY = 7; tY <= tTop; tY++) for (int tX = 3; tX <= 12; tX++) for (int tZ = 3; tZ <= 12; tZ++) {
            if (tX >= 6 && tX <= 9 && tZ >= 6 && tZ <= 9 && (tX == 6 || tZ == 6 || tX == 9 || tZ == 9)) {
                if (tY % 4 == 0) {
                    colored(aLevel, aClip, tX, tY, tZ);
                } else {
                    smooth(aLevel, aClip, tX, tY, tZ);
                }
            } else if (tX == 3 || tZ == 3 || tX == 12 || tZ == 12) {
                if (tY == tTop - 1) {
                    tiles(aLevel, aClip, tX, tY, tZ);
                } else {
                    bricks(aLevel, aClip, tX, tY, tZ);
                }
            } else {
                air(aLevel, aClip, tX, tY, tZ);
            }
        }

        // the room-side core walls y1..6 (:176-185) + the shaft door holes (:187-194).
        for (int tY = 1; tY <= 6; tY++) for (int tX = 6; tX <= 9; tX++) for (int tZ = 6; tZ <= 9; tZ++) {
            if (tX == 6 || tZ == 6 || tX == 9 || tZ == 9) {
                if (tY % 4 == 0) {
                    colored(aLevel, aClip, tX, tY, tZ);
                } else {
                    smooth(aLevel, aClip, tX, tY, tZ);
                }
            }
        }
        for (int tY = 1; tY <= 2; tY++) for (int tX = 7; tX <= 8; tX++) {
            air(aLevel, aClip, tX, tY, 6);
            air(aLevel, aClip, tX, tY, 9);
        }

        // the two spiral slab staircases (:196-241), verbatim rows, 5 Y per revolution.
        BlockState tSlab = Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        int tOffsetY = -5;
        while (tOffsetY + 10 < tTop) {
            tOffsetY += 5;
            slab(aLevel, aClip, 10, tOffsetY + 1, 6, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 1, 6, tSlab);
            slab(aLevel, aClip, 10, tOffsetY + 1, 7, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 1, 7, tSlab);
            slab(aLevel, aClip, 10, tOffsetY + 2, 8, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 2, 8, tSlab);
            slab(aLevel, aClip, 10, tOffsetY + 2, 9, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 2, 9, tSlab);

            slab(aLevel, aClip, 10, tOffsetY + 3, 10, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 3, 10, tSlab);
            slab(aLevel, aClip, 10, tOffsetY + 3, 11, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 3, 11, tSlab);
            slab(aLevel, aClip, 9, tOffsetY + 3, 10, tSlab);
            slab(aLevel, aClip, 9, tOffsetY + 3, 11, tSlab);
            slab(aLevel, aClip, 8, tOffsetY + 4, 10, tSlab);
            slab(aLevel, aClip, 8, tOffsetY + 4, 11, tSlab);
            slab(aLevel, aClip, 7, tOffsetY + 4, 10, tSlab);
            slab(aLevel, aClip, 7, tOffsetY + 4, 11, tSlab);
            slab(aLevel, aClip, 6, tOffsetY + 5, 10, tSlab);
            slab(aLevel, aClip, 6, tOffsetY + 5, 11, tSlab);

            slab(aLevel, aClip, 5, tOffsetY + 5, 10, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 5, 11, tSlab);
            slab(aLevel, aClip, 4, tOffsetY + 5, 10, tSlab);
            slab(aLevel, aClip, 4, tOffsetY + 5, 11, tSlab);

            slab(aLevel, aClip, 4, tOffsetY + 1, 9, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 1, 9, tSlab);
            slab(aLevel, aClip, 4, tOffsetY + 1, 8, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 1, 8, tSlab);
            slab(aLevel, aClip, 4, tOffsetY + 2, 7, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 2, 7, tSlab);
            slab(aLevel, aClip, 4, tOffsetY + 2, 6, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 2, 6, tSlab);

            slab(aLevel, aClip, 4, tOffsetY + 3, 5, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 3, 5, tSlab);
            slab(aLevel, aClip, 4, tOffsetY + 3, 4, tSlab);
            slab(aLevel, aClip, 5, tOffsetY + 3, 4, tSlab);

            slab(aLevel, aClip, 6, tOffsetY + 3, 4, tSlab);
            slab(aLevel, aClip, 6, tOffsetY + 3, 5, tSlab);
            slab(aLevel, aClip, 7, tOffsetY + 4, 4, tSlab);
            slab(aLevel, aClip, 7, tOffsetY + 4, 5, tSlab);
            slab(aLevel, aClip, 8, tOffsetY + 4, 4, tSlab);
            slab(aLevel, aClip, 8, tOffsetY + 4, 5, tSlab);
            slab(aLevel, aClip, 9, tOffsetY + 5, 4, tSlab);
            slab(aLevel, aClip, 9, tOffsetY + 5, 5, tSlab);
            slab(aLevel, aClip, 10, tOffsetY + 5, 4, tSlab);
            slab(aLevel, aClip, 10, tOffsetY + 5, 5, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 5, 4, tSlab);
            slab(aLevel, aClip, 11, tOffsetY + 5, 5, tSlab);
        }

        // the top landing plates (:243-259).
        for (int tX = 4; tX <= 5; tX++) for (int tZ = 6; tZ <= 9; tZ++) {
            slab(aLevel, aClip, tX, tOffsetY + 5, tZ, tSlab);
        }
        for (int tX = 10; tX <= 11; tX++) for (int tZ = 6; tZ <= 9; tZ++) {
            slab(aLevel, aClip, tX, tOffsetY + 5, tZ, tSlab);
        }

        // the surface cap (:261-268): the 12×12 ring at the aligned top, interior air.
        for (int tY = Math.max(8, tTop - 2); tY <= tTop + 2; tY++) for (int tX = 2; tX <= 13; tX++) for (int tZ = 2; tZ <= 13; tZ++) {
            if (tX == 2 || tZ == 2 || tX == 13 || tZ == 13) {
                if (tY == tTop + 1) {
                    colored(aLevel, aClip, tX, tY, tZ);
                } else {
                    bricks(aLevel, aClip, tX, tY, tZ);
                }
            } else {
                if (tY >= tTop) air(aLevel, aClip, tX, tY, tZ);
            }
        }

        // the boundary doorways — the entrance wears COLORED frames (upstream :270-337).
        for (Direction tSide : Direction.Plane.HORIZONTAL) if (doorAt(tSide)) {
            buildColoredDoorway(aLevel, aClip, tSide);
        }
    }

    private void slab(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ, BlockState aSlab) {
        set(aLevel, aClip, aLX, aLY, aLZ, aSlab);
    }

    /** The colored-frame boundary doorway (the entrance variant of {@link #buildDoorway}). */
    private void buildColoredDoorway(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide) {
        int tWall = aSide == Direction.EAST ? 15 : aSide == Direction.WEST ? 0 : aSide == Direction.SOUTH ? 15 : 0;
        for (int tV = 5; tV <= 10; tV++) for (int tY = 0; tY <= 4; tY++) {
            int tLX = switch (aSide) {
                case EAST, WEST -> tWall;
                case SOUTH -> tV;
                default -> tV;
            };
            int tLZ = switch (aSide) {
                case EAST, WEST -> tV;
                case SOUTH -> tWall;
                default -> 15 - tWall;
            };
            if (tV == 5 || tV == 10) {
                colored(aLevel, aClip, tLX, tY, tLZ);
            } else if (tY >= 1 && tY <= 3) {
                air(aLevel, aClip, tLX, tY, tLZ);
            } else {
                colored(aLevel, aClip, tLX, tY, tLZ);
            }
        }
    }

    // ---------------------------------------------------------------- loot chests

    /**
     * The loot chests in the four corner nooks (upstream :272-336 crate stacks — the
     * MTE face defers, the chests carry the DUNGEON_CHEST 对位 table).
     */
    private void buildChests(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        int[][] tCorners = {{2, 2}, {2, 13}, {13, 2}, {13, 13}};
        for (int[] tCorner : tCorners) {
            if (aRandom.nextInt(2) == 0) {
                createDungeonChest(aLevel, aClip, aRandom, wx(tCorner[0]), wy(1), wz(tCorner[1]));
            }
        }
    }

    /** {@code createChest} bound to {@code gt6:chests/dungeon_chest} — the MineshaftPieces.java:270-282 face. */
    private void createDungeonChest(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom, int aWX, int aWY, int aWZ) {
        //? if forge {
        this.createChest(aLevel, aClip, aRandom, aWX, aWY, aWZ, new net.minecraft.resources.ResourceLocation("gt6", DUNGEON_CHEST_TABLE));
        //?} else {
        /*this.createChest(aLevel, aClip, aRandom, aWX, aWY, aWZ, net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", DUNGEON_CHEST_TABLE)));
        //21.1: createChest takes ResourceKey<LootTable> (StructurePiece.java:446-451, the 1.20.5 loot-key move).
        *///?}
    }

}
