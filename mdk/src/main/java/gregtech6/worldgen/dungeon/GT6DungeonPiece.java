package gregtech6.worldgen.dungeon;

import java.util.function.Consumer;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
import gregtech6.registry.GT6Books;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6Structures;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.tileentity.inventories.GT6SafeBlockEntity;
import gregtech6.tileentity.inventories.GT6StaticStorageBaseBlockEntity;

/**
 * The shelter-dungeon PIECE (task p38-dungeon-framework) — one class over the shipped
 * room kinds, each an upstream {@code IDungeonChunk} port (tmp/gt6-1.7.10
 * gregapi/worldgen/dungeon/):
 * <ul>
 * <li>{@link Kind#ROOM_EMPTY} — {@code DungeonChunkRoomEmpty} (:204): the 16×16×8 shell,
 *     open boundary doorways on connected sides;</li>
 * <li>{@link Kind#CORRIDOR} — {@code DungeonChunkCorridor} (:190): the 6-wide arm piece,
 *     connection-count agnostic;</li>
 * <li>{@link Kind#ENTRANCE} — {@code DungeonChunkEntrance} (:338): the shell + the
 *     surface shaft with the two spiral slab staircases;</li>
 * <li>{@link Kind#STORAGE} — {@code DungeonChunkRoomStorage} (:215) over
 *     {@code DungeonChunkRoomVault} (:34): the vault shell (open boundary doorways) +
 *     the piston airlocks on every connected side + the loot chests. The upstream Vault
 *     class IS the shell+door base Storage extends, so one kind carries both ports; the
 *     DEAD_END pool this card ships is exactly this room (the five portal rooms are the
 *     mod-专属 never pool, unported by ruling).</li>
 * <li>{@link Kind#CORRIDOR3} — {@code DungeonChunkCorridor3} (:34, the
 *     dungeon-rooms-batch card): the 3-way corridor plus ONE alcove on its free side —
 *     the loot nook (roll 0), the breakable cobble wall with the KEY-LOCKED safe (roll 2
 *     always; roll 1 only when key #3 exists — the upstream :58/:99 gate), or the plain
 *     crossing (roll 3). The coin piles and drink cups omit (no shell face).</li>
 * <li>{@link Kind#CORRIDOR4} — {@code DungeonChunkCorridor4} (:30): the 4-way crossing
 *     hall, 12×12×7 over the {4,7,8,11} deco lattice with the four arm stubs.</li>
 * <li>{@link Kind#BARRACKS} — {@code DungeonChunkBarracks} (:38): the four corner
 *     quarters over the slab partition walls (carpets, iron doors, beds, crafting
 *     tables) + the shelf/safe loot pairs with the key face (the key-locked safes, the
 *     quarter-key shelf hides, the hint cobble walls). The shelf-front loot marker stays
 *     unset (the ported bookshelf BE ships
 *     the seam but the storage card shipped no trigger); the drink cups, hexorium
 *     monoliths, Sky Stone rock pile and coin piles omit; the metal bookshelves 7110
 *     fold to the wooden row family.</li>
 * <li>{@link Kind#LIBRARY} — {@code DungeonChunkRoomLibrary} (:197) +
 *     {@code DungeonChunkRoomLibraryNormal} (:106), task dungeon-library-zpm: the
 *     reading room (carpet, plank ceiling, bookshelf wall band, the four reading
 *     tables) plus the Normal variant's per-solid-wall display row (the TAG_LIBRARY_
 *     NORMAL once-per-dungeon room — the upstream Normal class IS the base room + the
 *     display rows, its super.generate :39). The Thaumcraft/Mystcraft variants are the
 *     mod-专属 never pool (unported by ruling).</li>
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
 * (the upstream GT slabs). The room-batch card (dungeon-rooms-batch) replaces the
 * framework-card deferral: the loot faces ride vanilla chests bound to the p34-injected
 * vanilla tables + the ported storage BEs (safe/bookshelf) — the remaining MTE faces
 * (crucibles/molds/tanks/pipes/spikes/tool racks/ingot piles/coins/cups) are empty-shell
 * placeholders or omissions, declared per room in the room javadocs and the card report.
 *
 * <p>Library deviations (task dungeon-library-zpm, the same record): the GT shelf MTE
 * (7000+tPlank / 7839, the per-seat 9-category ChestGenHooks loot roll,
 * {@code DungeonData.shelf :293-304} + the key-NBT shelf :122-128) ports as the vanilla
 * bookshelf — the loot face collapses onto the four reading-table chests bound to the
 * GT6 dungeon carrier table (the storage-room chest precedent), and the key shelf
 * defers with the keys card; the potion-cup MTE 32739 and coin-pile MTE 32700 seats
 * take declared air (the Hexorium display block resolves null in this universe, so the
 * upstream cup branch always took the cup); the ZPM trophy seat ({@code zpm :306-311},
 * 1/16 per seat 2/3 FULL) migrated to the dungeon loot face (the
 * {@code dungeon_inject_gt6_dungeon_chest} row — no ZPM block exists to seat), so the
 * trophy seats take the vanilla flower-pot shell of the upstream pot face.
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

    /**
     * The shipped room kinds (the upstream IDungeonChunk classes, see the class javadoc).
     * The room-batch card (dungeon-rooms-batch) appends the corridor/barracks special
     * cells and the ROOMS-pool entries; the pool itself lives on
     * {@link GT6DungeonStructure}.
     */
    public enum Kind {
        ROOM_EMPTY, CORRIDOR, ENTRANCE, STORAGE,
        /** {@code DungeonChunkCorridor3} — the 3-way corridor with one loot nook / breakable wall. */
        CORRIDOR3,
        /** {@code DungeonChunkCorridor4} — the 4-way crossing hall (:30-103). */
        CORRIDOR4,
        /** {@code DungeonChunkBarracks} (:38-162) — the four corner quarters. */
        BARRACKS,
        /** {@code DungeonChunkRoomWorkshop} (:43-208) — the smithy + the manual cabinet. */
        WORKSHOP,
        /** {@code DungeonChunkRoomMiningBedrock} (:34-139) — the pit down to the bedrock vein. */
        MINING_BEDROCK,
        /** {@code DungeonChunkRoomLibrary} + {@code DungeonChunkRoomLibraryNormal} — the reading room + display rows. */
        LIBRARY,
        /** {@code DungeonChunkRoomFarmCrop} (:35-230) — the irrigated crop quarters. */
        FARM_CROP,
        /** {@code DungeonChunkRoomFarmMobs} (:34-205) — the mob-drop tower, spilling over free neighbors. */
        FARM_MOBS,
        /** {@code DungeonChunkRoomFarmFish} (:38-83) — the fish pond. */
        FARM_FISH;

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
    /**
     * The per-dungeon key ids (WorldgenDungeonGT.java:169-171 — five descending ids,
     * the first {@code 1 + max(draw, unique-tag)}); null on non-key pieces. Shared by
     * every piece of one dungeon so a key hidden in one room opens a lock placed by
     * another (the upstream DungeonData mKeyStacks pass-by-reference face).
     */
    private final long[] mKeyIds;
    /**
     * The key index this piece carries from the dungeon's Workshop draw (upstream
     * DungeonChunkRoomWorkshop.java:119-123, {@code next(keys * 2) < keys → hide
     * key[tKeyIndex]}): the WORKSHOP hides key[draw] in its manual cabinet; the
     * CORRIDOR3 reads it for the :58/:99 breakable-wall gate (key #3 exists → the safe
     * locks to key #3, else the side's quarter key); -1 on every other kind. The
     * dungeon-keys STORAGE proxy consumer is GONE — the keys hide in the real rooms
     * (the upstream positions; the Library rows stay the library card's seam).
     */
    private final int mKeyIndex;
    /**
     * The BARRACKS quarter shelf slots (upstream :115/:126/:137/:148, the {@code
     * next(28)} draws): slot[q] for quarter q ∈ {0,1,3,4} = hide key[quarter] at that
     * shelf slot; -1 = the Workshop already hid that key (the shared mGeneratedKeys
     * guard). Slot >= 14 = the shelf back face → the hint cobble wall (:118 etc.).
     * Null on every other kind.
     */
    private final int[] mKeySlots;
    /**
     * The FARM_MOBS diagonal build-over mask (bit0 NW, 1 NE, 2 SW, 3 SE — layout i = the
     * x axis, j = the z axis): the bit is set when the diagonal cell AND both adjacent
     * ortho cells are rock-or-corridor (upstream :41-56), licensing the platform spill
     * into the neighboring chunks. Zero for every other kind.
     */
    private final byte mFree;

    /** The worldgen ctor (non-key kinds). */
    public GT6DungeonPiece(Kind aKind, BoundingBox aBox, byte aDoors, String aPrimary, String aSecondary, int aColor, int aShaftTop) {
        this(aKind, aBox, aDoors, aPrimary, aSecondary, aColor, aShaftTop, null, -1, null, (byte) 0);
    }

    /**
     * The full worldgen ctor: the key roll (keyIds + the Workshop draw — WORKSHOP hides
     * it, CORRIDOR3 reads it) + the barracks quarter slots + the FARM_MOBS spill mask.
     */
    public GT6DungeonPiece(Kind aKind, BoundingBox aBox, byte aDoors, String aPrimary, String aSecondary,
            int aColor, int aShaftTop, long[] aKeyIds, int aKeyIndex, int[] aKeySlots, byte aFree) {
        super(GT6Structures.DUNGEON_PIECE_TYPE.get(), 0, aBox);
        mKind = aKind;
        mDoors = aDoors;
        mPrimaryStone = aPrimary;
        mSecondaryStone = aSecondary;
        mColor = aColor;
        mShaftTop = aShaftTop;
        mKeyIds = aKeyIds;
        mKeyIndex = aKeyIndex;
        mKeySlots = aKeySlots;
        mFree = aFree;
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
        mKeyIds = aTag.contains("gtKeys") ? aTag.getLongArray("gtKeys") : null;
        mKeyIndex = aTag.contains("gtKeys") ? aTag.getInt("gtKeyIndex") : -1;
        mKeySlots = aTag.contains("gtKeySlots") ? aTag.getIntArray("gtKeySlots") : null;
        mFree = aTag.getByte("gtFree");
    }

    /** One {@link #mFree} diagonal bit (0 NW, 1 NE, 2 SW, 3 SE). */
    private boolean freeDiag(int aBit) {
        return (mFree & (1 << aBit)) != 0;
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
        if (mKeyIds != null) {
            aTag.putLongArray("gtKeys", mKeyIds);
            aTag.putInt("gtKeyIndex", mKeyIndex);
        }
        if (mKeySlots != null) {
            aTag.putIntArray("gtKeySlots", mKeySlots);
        }
        aTag.putByte("gtFree", mFree);
    }

    @Override
    public void postProcess(
            WorldGenLevel aLevel, StructureManager aStructure, ChunkGenerator aGenerator, RandomSource aRandom,
            BoundingBox aClip, ChunkPos aChunkPos, BlockPos aAnchor) {
        switch (mKind) {
            case ENTRANCE -> buildEntrance(aLevel, aClip, aRandom);
            case CORRIDOR -> buildCorridor(aLevel, aClip, aRandom);
            case CORRIDOR3 -> {
                buildCorridor(aLevel, aClip, aRandom);
                buildCorridor3Alcove(aLevel, aClip, aRandom);
            }
            case CORRIDOR4 -> buildCorridor4(aLevel, aClip, aRandom);
            case BARRACKS -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildBarracks(aLevel, aClip, aRandom);
            }
            case WORKSHOP -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildWorkshop(aLevel, aClip, aRandom);
            }
            case MINING_BEDROCK -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildMiningBedrock(aLevel, aClip, aRandom);
            }
            case FARM_CROP -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildFarmCrop(aLevel, aClip, aRandom);
            }
            case FARM_MOBS -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildFarmMobs(aLevel, aClip, aRandom);
            }
            case FARM_FISH -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildFarmFish(aLevel, aClip, aRandom);
            }
            case STORAGE -> {
                buildRoomShell(aLevel, aClip, aRandom);
                for (Direction tSide : Direction.Plane.HORIZONTAL) if (doorAt(tSide)) {
                    buildAirlock(aLevel, aClip, tSide);
                }
                buildChests(aLevel, aClip, aRandom);
            }
            case LIBRARY -> {
                buildRoomShell(aLevel, aClip, aRandom);
                buildLibrary(aLevel, aClip, aRandom);
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

    /**
     * The Library wood vocabulary (task dungeon-library-zpm) — the 1.7.10 plank meta
     * order (0 oak .. 5 dark oak) mapped onto the modern rows; the slab of the same
     * meta carries both halves (the 1.7.10 top-slab meta {@code tPlank + 8} = the TOP
     * slab state). Public final for the offline vocabulary pins.
     */
    public static final Block[] LIBRARY_PLANKS = {
            Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
            Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS};
    public static final Block[] LIBRARY_SLABS = {
            Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB,
            Blocks.JUNGLE_SLAB, Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB};


    /**
     * The shelf-run seats along one wall (upstream :130-156): the two 3-wide runs leave
     * the doorway zone v5..10 open; 6 seats × 4 walls × the 4 shelf levels = the 96
     * bookshelves of the wall band.
     */
    public static final int[] LIBRARY_SHELF_RUN = {2, 3, 4, 11, 12, 13};
    /** The Normal-variant display-row trophy seats (upstream :57/:59 — the ZPM seats). */
    public static final int[] LIBRARY_DISPLAY_SEATS = {6, 9};
    /** The four furniture seats of the reading nooks (upstream :164-193). */
    public static final int[][] LIBRARY_NOOK_SEATS = {{3, 3}, {3, 12}, {12, 3}, {12, 12}};

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
        // floor lever: LeverBlock.FACING is HORIZONTAL-only (LeverBlock.java:52 — a
        // Direction.UP write throws and takes the whole chunk-decoration task down, the
        // live scan boot#1 evidence); the attach face is the FACE property.
        BlockState tLever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, Boolean.TRUE);

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

    // ---------------------------------------------------------------- room batch: shared helpers

    /**
     * The vanilla chest of a loot nook, bound to a vanilla loot-table path (the p34
     * {@code ChestGenHooks → vanilla table} mapping face — GT6 loot rides the p34
     * injections on those tables). The FACING is the front.
     */
    private void createVanillaChest(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom,
            int aLX, int aLY, int aLZ, Direction aFacing, String aVanillaPath) {
        BlockPos tPos = new BlockPos(wx(aLX), wy(aLY), wz(aLZ));
        if (!aClip.isInside(tPos)) return;
        //? if forge {
        this.createChest(aLevel, aClip, aRandom, tPos,
                new net.minecraft.resources.ResourceLocation("minecraft", aVanillaPath),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, aFacing));
        //?} else {
        /*this.createChest(aLevel, aClip, aRandom, tPos,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", aVanillaPath)),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, aFacing));
        //21.1: createChest takes ResourceKey<LootTable> (StructurePiece.java:446-451, the 1.20.5 loot-key move).
        *///?}
    }

    /**
     * A GT6 static-storage block facing {@code aFacing} (the front), the BE handed to the
     * seeder (inventory fill / the dungeon-loot marker) — the worldgen counterpart of the
     * block carrier's {@code getStateForPlacement} + {@code setPlacedBy} pair.
     */
    private void placeStorage(WorldGenLevel aLevel, BoundingBox aClip, int aLX, int aLY, int aLZ,
            Block aBlock, Direction aFacing, Consumer<BlockEntity> aSeed) {
        BlockPos tPos = new BlockPos(wx(aLX), wy(aLY), wz(aLZ));
        if (!aClip.isInside(tPos) || !(aBlock instanceof GT6StaticStorages.GT6StorageBlock)) return;
        aLevel.setBlock(tPos, aBlock.defaultBlockState()
                .setValue(GT6StaticStorages.GT6StorageBlock.FACING, aFacing), 2);
        BlockEntity tBE = aLevel.getBlockEntity(tPos);
        if (tBE != null) aSeed.accept(tBE);
    }

    /** The safe loot marker — the full loot-table id the BE resolves on first open. */
    private void seedSafe(net.minecraft.world.level.block.entity.BlockEntity aBE, String aLootTableId) {
        if (aBE instanceof GT6SafeBlockEntity tSafe) {
            tSafe.mDungeonLootName = aLootTableId;
        }
    }

    /** The dye-meta block rows (white = meta 0 — the {@link #CONCRETES} order). */
    private static final Block[] CARPETS = {
            Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET, Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET, Blocks.LIME_CARPET, Blocks.PINK_CARPET, Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET, Blocks.CYAN_CARPET, Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.BLACK_CARPET};

    /**
     * The dungeon-loot categories the barracks safes draw (upstream :108 tLoots, the
     * p34 {@code ChestGenHooks → vanilla table} mapping verbatim; DUNGEON_CHEST rides
     * the gt6 carrier). Full ids — the safe marker face.
     */
    public static final String[] BARRACKS_SAFE_LOOTS = {
            "minecraft:chests/stronghold_library", "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing", "minecraft:chests/desert_pyramid",
            "minecraft:chests/jungle_temple", "minecraft:chests/village/village_weaponsmith",
            "minecraft:chests/abandoned_mineshaft", "gt6:chests/dungeon_chest",
            "minecraft:chests/spawn_bonus_chest"};

    // ---------------------------------------------------------------- corridor 3/4

    /**
     * The 4-way crossing hall (upstream {@code DungeonChunkCorridor4} :30-103): the
     * pillar gate, the 12×12×7 hall over the {4,7,8,11} deco lattice, and the four
     * 3-block arm stubs to the chunk edges.
     */
    private void buildCorridor4(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        boolean tX = doorAt(Direction.EAST) || doorAt(Direction.WEST);
        boolean tZ = doorAt(Direction.SOUTH) || doorAt(Direction.NORTH);
        if (tX && tZ) pillar(aLevel, aClip); // :30-32 — the same both-axes gate as the base corridor

        for (int tXc = 2; tXc <= 13; tXc++) for (int tZc = 2; tZc <= 13; tZc++) for (int tY = 0; tY <= 6; tY++) {
            if (tXc == 2 || tXc == 13 || tZc == 2 || tZc == 13 || tY == 0 || tY == 6) {
                if ((tXc == 4 || tXc == 7 || tXc == 8 || tXc == 11) && (tZc == 4 || tZc == 7 || tZc == 8 || tZc == 11)) {
                    if (tY == 0) {
                        chiseled(aLevel, aClip, tXc, tY, tZc);
                    } else if (tY == 6) {
                        lamp(aLevel, aClip, tXc, tY, tZc);
                    } else {
                        bricks(aLevel, aClip, tXc, tY, tZc);
                    }
                } else {
                    if (tY == 0) {
                        tiles(aLevel, aClip, tXc, tY, tZc);
                    } else if (tY == 6) {
                        smalltiles(aLevel, aClip, tXc, tY, tZc);
                    } else {
                        bricks(aLevel, aClip, tXc, tY, tZc);
                    }
                }
            } else {
                air(aLevel, aClip, tXc, tY, tZc);
            }
        }

        // the four arm stubs (:58-101) — one parametric walk over the connected sides.
        for (Direction tSide : Direction.Plane.HORIZONTAL) {
            for (int tD = 13; tD <= 15; tD++) for (int tV = 5; tV <= 10; tV++) for (int tY = 0; tY <= 4; tY++) {
                int tLX = switch (tSide) {
                    case EAST -> tD;
                    case WEST -> 15 - tD;
                    case SOUTH -> tV;
                    default -> tV;
                };
                int tLZ = switch (tSide) {
                    case EAST, WEST -> tV;
                    case SOUTH -> tD;
                    default -> 15 - tD;
                };
                if (tY == 0) {
                    set(aLevel, aClip, tLX, tY, tLZ, face(StoneVariant.TILES, tY, true));
                } else if (tY == 4) {
                    set(aLevel, aClip, tLX, tY, tLZ, face(StoneVariant.STILE, tY, true));
                } else if (tV == 5 || tV == 10) {
                    set(aLevel, aClip, tLX, tY, tLZ, face(StoneVariant.BRICK, tY, true));
                } else {
                    air(aLevel, aClip, tLX, tY, tLZ);
                }
            }
        }
    }

    /**
     * The 3-way corridor's one alcove (upstream {@code DungeonChunkCorridor3} :34-205):
     * the FIRST free side (E,W,S,N order — the upstream x+1/x-1/z+1/z-1 gate order)
     * draws the nook (roll 0), the breakable wall with the safe (roll 2 always; roll 1
     * ONLY when key #3 exists — the upstream :58/:99/:140/:181 fall-through gate), or
     * the default crossing (roll 3).
     *
     * <p>Declared MTE folds: the coin piles and the drink cup are omitted (no shell
     * face); the safe is the ported KEY-LOCKED safe (the upstream 3010) with the lock
     * id from the dungeon's key state (:69/:109/:151/:191) — openable either way, since
     * the fallback quarter key is always hidden by the Barracks.
     */
    private void buildCorridor3Alcove(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        Direction tSide = null;
        for (Direction tCandidate : new Direction[] {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            if (!doorAt(tCandidate)) {
                tSide = tCandidate;
                break;
            }
        }
        if (tSide == null) return;
        int tRoll = aRandom.nextInt(4);
        boolean tKey2 = mKeyIndex == 2 && mKeyIds != null; // the Workshop hid key #3
        if (tRoll == 0) buildLootNook(aLevel, aClip, aRandom, tSide);
        else if (tRoll <= 2 && (tRoll == 2 || tKey2)) buildBreakableWall(aLevel, aClip, aRandom, tSide, tKey2);
    }

    /** The upstream case 0 — the crafting station + loot chest sealed behind a wall (EAST shape :43-55). */
    private void buildLootNook(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom, Direction aSide) {
        boolean tPositive = aSide == Direction.EAST || aSide == Direction.SOUTH;
        int tSeal = tPositive ? 11 : 4;  // the sealing wall depth (u)
        int tOpen = tPositive ? 10 : 5;  // the opened furniture row depth
        for (int tY = 0; tY <= 4; tY++) for (int tV = 5; tV <= 10; tV++) {
            sideSet(aLevel, aClip, aSide, tSeal, tY, tV, face(StoneVariant.SMOTH, tY, true));
        }
        for (int tY = 1; tY <= 3; tY++) for (int tV = 6; tV <= 9; tV++) {
            sideSet(aLevel, aClip, aSide, tOpen, tY, tV, Blocks.CAVE_AIR.defaultBlockState());
        }
        sideSet(aLevel, aClip, aSide, tOpen, 1, 6, face(StoneVariant.SMOTH, 1, true));
        sideSet(aLevel, aClip, aSide, tOpen, 1, 9, face(StoneVariant.SMOTH, 1, true));
        // (A, B) swap with the side — the upstream E/W/S/N hand-mirrors.
        boolean tTableFirst = tPositive;
        int tVTable = tTableFirst ? 7 : 8, tVChest = tTableFirst ? 8 : 7;
        sideSet(aLevel, aClip, aSide, tOpen, 1, tVTable, Blocks.CRAFTING_TABLE.defaultBlockState());
        createVanillaChest(aLevel, aClip, aRandom, sideLX(aSide, tOpen, tVChest), 1, sideLZ(aSide, tOpen, tVChest),
                aSide.getOpposite(), "chests/stronghold_corridor");
    }

    /**
     * The upstream case 2 — the cobble shell with the safe pocket (EAST shape :61-73).
     * The safe is KEY-LOCKED (upstream 3010, :69/:109/:151/:191): key #3 when the
     * Workshop hid it, else the side's quarter key (EAST→0, WEST→1, SOUTH→3, NORTH→4).
     */
    private void buildBreakableWall(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom, Direction aSide, boolean aKey2) {
        boolean tPositive = aSide == Direction.EAST || aSide == Direction.SOUTH;
        int tOutermost = tPositive ? 13 : 2, tMid = tPositive ? 12 : 3, tInner = tPositive ? 11 : 4;
        int tPartial = tPositive ? 10 : 5, tPocket = tPositive ? 11 : 4, tNest = tPositive ? 12 : 3;
        for (int tDepth : new int[] {tOutermost, tMid, tInner}) {
            for (int tY = 0; tY <= 4; tY++) for (int tV = 5; tV <= 10; tV++) {
                sideSet(aLevel, aClip, aSide, tDepth, tY, tV,
                        face(aRandom.nextInt(2) == 0 ? StoneVariant.COBBL : StoneVariant.MCOBL, tY, true));
            }
        }
        for (int tY = 1; tY <= 3; tY++) for (int tV = 6; tV <= 9; tV++) {
            sideSet(aLevel, aClip, aSide, tPartial, tY, tV,
                    face(aRandom.nextInt(2) == 0 ? StoneVariant.COBBL : StoneVariant.MCOBL, tY, true));
        }
        for (int tY = 1; tY <= 3; tY++) for (int tV = 6; tV <= 9; tV++) {
            sideSet(aLevel, aClip, aSide, tPocket, tY, tV, Blocks.CAVE_AIR.defaultBlockState());
        }
        for (int tY = 1; tY <= 2; tY++) for (int tV = 7; tV <= 8; tV++) {
            sideSet(aLevel, aClip, aSide, tNest, tY, tV, Blocks.CAVE_AIR.defaultBlockState());
        }
        int tVSafe = tPositive ? 8 : 7;
        int tKeyIndex = GT6DungeonStructure.corridor3LockKeyIndex(mKeyIndex,
                switch (aSide) {
                    case EAST -> 0;
                    case WEST -> 1;
                    case SOUTH -> 3;
                    default -> 4;
                });
        Block tSafe = GT6StaticStorages.blockByPath("safe_keylocked_steel");
        if (tSafe != null) {
            placeStorage(aLevel, aClip, sideLX(aSide, tNest, tVSafe), 1, sideLZ(aSide, tNest, tVSafe),
                    tSafe, aSide.getOpposite(), tBE -> {
                        if (tBE instanceof gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity tLock && mKeyIds != null) {
                            tLock.mID = mKeyIds[tKeyIndex];
                        }
                        seedSafe(tBE, "minecraft:chests/stronghold_corridor");
                    });
        }
    }

    // ---------------------------------------------------------------- barracks

    /**
     * The barracks interior (upstream {@code DungeonChunkBarracks} :43-159): corner
     * carpets, the slab partition walls with the four iron-door quarters, beds/crafting
     * tables, and the shelf+safe loot pairs with the KEY face (the key-locked safes +
     * the quarter-key shelf hides + the hint cobble walls, :108-154).
     *
     * <p>Declared folds: the shelf-front loot marker stays unset (the ported bookshelf BE
     * has the seam but the storage card shipped no trigger); the drink cups, the hexorium
     * monoliths and the Sky Stone rock pile are omitted (no shell face); the metal
     * bookshelves 7110 fold to the wooden row family; the key ITEM rolls per placement
     * (the lock reads the NBT id, the cosmetic item face only).
     */
    private void buildBarracks(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        // the corner carpets (:43-45), the dye-inversed color (the 15-meta inverse).
        BlockState tCarpet = CARPETS[(15 - mColor) & 15].defaultBlockState();
        for (int tX = 1; tX <= 14; tX++) for (int tZ = 1; tZ <= 14; tZ++) {
            if ((tX <= 4 || tX >= 11) && (tZ <= 4 || tZ >= 11)) {
                set(aLevel, aClip, tX, 1, tZ, tCarpet);
            }
        }

        // the slab partition walls (:46-52): four segments per axis, y1..6; the 12 corner
        // anchors (:54-65).
        BlockState tSlab = Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int tY = 1; tY <= 6; tY++) {
            for (int tCoord = 1; tCoord <= 14; tCoord++) {
                if (tCoord <= 3 || tCoord >= 12) {
                    set(aLevel, aClip, tCoord, tY, 5, tSlab);
                    set(aLevel, aClip, tCoord, tY, 10, tSlab);
                    set(aLevel, aClip, 5, tY, tCoord, tSlab);
                    set(aLevel, aClip, 10, tY, tCoord, tSlab);
                }
            }
            for (int[] tAnchor : BARRACKS_CORNER_ANCHORS) {
                smooth(aLevel, aClip, tAnchor[0], tY, tAnchor[1]);
            }
        }

        // the four iron-door quarters (:68-75) + the buttons and plates (:76-83).
        for (int[] tDoor : new int[][] {{3, 5, 0}, {12, 5, 0}, {3, 10, 1}, {12, 10, 1}}) {
            Direction tFacing = tDoor[2] == 0 ? Direction.NORTH : Direction.SOUTH;
            set(aLevel, aClip, tDoor[0], 1, tDoor[1], Blocks.IRON_DOOR.defaultBlockState()
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(DoorBlock.FACING, tFacing));
            set(aLevel, aClip, tDoor[0], 2, tDoor[1], Blocks.IRON_DOOR.defaultBlockState()
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                    .setValue(DoorBlock.FACING, tFacing));
        }
        for (int[] tButton : new int[][] {{4, 6, 0}, {11, 6, 0}, {4, 9, 1}, {11, 9, 1}}) {
            set(aLevel, aClip, tButton[0], 2, tButton[1], Blocks.STONE_BUTTON.defaultBlockState()
                    .setValue(ButtonBlock.FACE, AttachFace.WALL)
                    .setValue(ButtonBlock.FACING,
                            tButton[2] == 0 ? Direction.NORTH : Direction.SOUTH));
        }
        for (int[] tPlate : new int[][] {{3, 4}, {12, 4}, {3, 11}, {12, 11}}) {
            set(aLevel, aClip, tPlate[0], 1, tPlate[1], Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        }

        // the beds (:84-91) and crafting tables (:92-95).
        BlockState tBed = Blocks.RED_BED.defaultBlockState();
        for (int tX : new int[] {1, 14}) {
            setBedPair(aLevel, aClip, tX, 2, Direction.NORTH, tBed);  // head at z1, foot at z2
            setBedPair(aLevel, aClip, tX, 13, Direction.SOUTH, tBed); // foot at z13, head at z14
            set(aLevel, aClip, tX, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState());
            set(aLevel, aClip, tX, 1, 11, Blocks.CRAFTING_TABLE.defaultBlockState());
        }

        // the shelf + safe loot pairs (:108-154): each quarter's safe is KEY-LOCKED to
        // the quarter's key id (:120/:131/:142/:153, the unconditional NBT_KEY) with its
        // OWN loot draw (the per-call UT.Code.select); the shelf hides the quarter key
        // UNLESS the Workshop already hid it (mKeySlots[q] == -1, the shared
        // mGeneratedKeys guard); slot >= 14 = the shelf back face → the hint cobble wall
        // behind the shelf (:118/:129/:140/:151).
        Block tShelf = GT6StaticStorages.blockByPath("bookshelf_oak");
        Block tSafe = GT6StaticStorages.blockByPath("safe_keylocked_steel");
        int[][] tQuarters = {{4, 1, 0}, {4, 14, 1}, {11, 1, 0}, {11, 14, 1}};
        for (int tQ = 0; tQ < tQuarters.length; tQ++) {
            int tSafeX = tQuarters[tQ][0], tZ = tQuarters[tQ][1];
            Direction tFacing = tQuarters[tQ][2] == 0 ? Direction.SOUTH : Direction.NORTH;
            int tShelfX = tSafeX == 4 ? 3 : 12;
            int tQuarterKey = GT6DungeonStructure.BARRACKS_QUARTER_KEYS[tQ];
            int tKeySlot = mKeySlots != null ? mKeySlots[tQ] : -1;
            if (tSafe != null) {
                String tLoot = BARRACKS_SAFE_LOOTS[aRandom.nextInt(BARRACKS_SAFE_LOOTS.length)];
                placeStorage(aLevel, aClip, tSafeX, 1, tZ, tSafe, tFacing, tBE -> {
                    if (tBE instanceof gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity tLock && mKeyIds != null) {
                        tLock.mID = mKeyIds[tQuarterKey];
                    }
                    seedSafe(tBE, tLoot);
                });
            }
            if (tShelf != null) {
                placeStorage(aLevel, aClip, tShelfX, 1, tZ, tShelf, tFacing, tBE -> {
                    if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage && tKeySlot >= 0 && mKeyIds != null) {
                        // the quarter key stack (upstream :116 — the mKeyStacks[i] member
                        // with its :173 item draw; the item rolls here per placement, the
                        // lock reads the NBT id, not the item face)
                        tStorage.getInventory().setStackInSlot(tKeySlot, gregtech6.items.GT6Keys.dungeonStack(
                                gregtech6.items.GT6Keys.KEYS.get(aRandom.nextInt(gregtech6.items.GT6Keys.KEYS.size())).get(),
                                tQuarterKey, mKeyIds[tQuarterKey]));
                    }
                });
                if (tKeySlot >= gregtech6.tileentity.inventories.GT6BookShelfBlockEntity.INVENTORY_SIZE / 2) {
                    // the "something behind the Shelf" hint (upstream :118 etc.) — the
                    // back face slot → the cobble column behind the shelf.
                    int tHintZ = tZ == 1 ? 0 : 15;
                    for (int tY = 1; tY <= 3; tY++) {
                        set(aLevel, aClip, tShelfX, tY, tHintZ, face(StoneVariant.COBBL, tY, true));
                    }
                }
            }
        }
    }

    /** One vanilla bed pair: the FOOT at {@code (aX, 1, aZ)}, the HEAD one block along {@code aFacing}. */
    private void setBedPair(WorldGenLevel aLevel, BoundingBox aClip, int aX, int aZ, Direction aFacing, BlockState aBed) {
        set(aLevel, aClip, aX, 1, aZ, aBed.setValue(BedBlock.FACING, aFacing)
                .setValue(BedBlock.PART, BedPart.FOOT));
        set(aLevel, aClip, aX + aFacing.getStepX(), 1, aZ + aFacing.getStepZ(),
                aBed.setValue(BedBlock.FACING, aFacing).setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** The 12 partition-wall corner anchors (upstream :54-65), as (x, z) rows. */
    private static final int[][] BARRACKS_CORNER_ANCHORS = {
            {4, 5}, {5, 4}, {5, 5}, {4, 10}, {5, 10}, {5, 11},
            {10, 4}, {10, 5}, {11, 5}, {10, 10}, {10, 11}, {11, 10}};

    // ---------------------------------------------------------------- workshop

    /**
     * The workshop drawer/ingot materials (upstream sMetals :44) — a METHOD body, not a
     * static initializer: the registry classes load before MT.init() (the
     * GT6RegistryStaticInitGuardTest ruling, the Supplier form of GTMachines.java:87);
     * worldgen runs long after init, so the per-call read is the lazy face.
     */
    private static OreDictMaterial[] workshopMetals() {
        return new OreDictMaterial[] {
                MT.DamascusSteel, MT.DamascusSteel, MT.DamascusSteel, MT.BlackSteel, MT.RedSteel, MT.BlueSteel,
                MT.VanadiumSteel, MT.Steel, MT.Fe, MT.Brass, MT.Bronze, MT.BismuthBronze, MT.BlackBronze};
    }

    /**
     * The manual cabinet contents — the upstream 8-book row (:118) minus the two CUT
     * dynamic manuals (Manual_Elements/Manual_Alloys generate their pages from the
     * material registry, the p35-books ruling), so the port fills the 6 shipped rows.
     */
    private static final String[] WORKSHOP_MANUALS = {
            "manual_smeltery", "manual_random", "manual_extenders", "manual_steam", "manual_tools", "manual_printer"};

    /**
     * The workshop interior (upstream {@code DungeonChunkRoomWorkshop} :51-206): the
     * smithy wall (chests, anvil, grindstone, ingot piles), the drawer + safe column,
     * the manual cabinet, and the drinks wall.
     *
     * <p>The dedup face (upstream TAG_WORKSHOP :48) IS the dispatch pool draw
     * (draw-without-replacement) — see {@link GT6DungeonStructure}.
     *
     * <p>Declared MTE folds (the ~30-ID dependency gate): chest MTE 11 → the vanilla
     * chest on the p34-mapped tables; the mechanical safe 2010 → the ported safe BE +
     * marker; the drawer 4011 → the ported drawer BE + the 4×8 material fill; the ACT
     * 5011 → the ported {@code gt6:advanced_crafting_table}; the bookshelf 7111 → the
     * ported bookshelf + the 6 shipped manuals; the bottle shelf 8762 → the ported
     * bottlecrate + glass bottles (the GT fluid bottles omit); ingot/plate piles
     * 32084/32085 → the material blockIngot/blockPlate (smooth shell fallback); EMPTY
     * SHELLS (smooth stone) for the gas cylinder 32055, the mortar 32735, the measuring
     * pot 32738, the mass storages 6011, the crucible 1102+t, the tool rack 32034+t, the
     * molds 1070/1020+t, the taps 32730, the drums 32716/32714/32734, the funnel 32725,
     * the mixing bowl 32705, the bathing pot 32707; OMITS (no shell face): the coins.
     * The KEY STASH rides the manual cabinet (:119-123 verbatim — the piece's Workshop
     * draw; the duct-tape rows and the two CUT manuals leave slots 6..9 empty).
     */
    private void buildWorkshop(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        // the west bench column (:51-60).
        smooth(aLevel, aClip, 5, 1, 1); // the propane gas cylinder → shell
        createVanillaChest(aLevel, aClip, aRandom, 3, 1, 1, Direction.SOUTH, "chests/abandoned_mineshaft");
        createVanillaChest(aLevel, aClip, aRandom, 2, 1, 1, Direction.SOUTH, "chests/stronghold_crossing");
        set(aLevel, aClip, 1, 1, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
        smooth(aLevel, aClip, 1, 2, 1); // the mortar → shell
        createGtChest(aLevel, aClip, aRandom, 1, 1, 2, Direction.EAST, DUNGEON_CHEST_TABLE);

        // the drawer + safe column (:63-99).
        Block tDrawer = GT6StaticStorages.blockByPath("drawer_steel");
        if (tDrawer != null) {
            placeStorage(aLevel, aClip, 1, 1, 3, tDrawer, Direction.EAST, tBE -> {
                if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage) {
                    int tSlot = 0;
                    for (OreDictMaterial tMat : new OreDictMaterial[] {MT.StainlessSteel, MT.Bronze, MT.Invar, MT.Brass}) {
                        // the upstream per-prefix counts (:65-96): stick/ingot/plate
                        // 32+33, curved/screw 16+49, ring/small-gear 8+25, gear 1+4.
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 32 + aRandom.nextInt(33), OP.stick);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 32 + aRandom.nextInt(33), OP.ingot);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 32 + aRandom.nextInt(33), OP.plate);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 16 + aRandom.nextInt(49), OP.plateCurved);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 16 + aRandom.nextInt(49), OP.screw);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 8 + aRandom.nextInt(25), OP.ring);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 1 + aRandom.nextInt(4), OP.gearGt);
                        tSlot = fillDrawer(tStorage, tSlot, aRandom, tMat, 8 + aRandom.nextInt(25), OP.gearGtSmall);
                    }
                }
            });
        }
        Block tSafe = GT6StaticStorages.blockByPath("safe_mechanical_steel");
        if (tSafe != null) {
            placeStorage(aLevel, aClip, 1, 2, 3, tSafe, Direction.EAST,
                    tBE -> seedSafe(tBE, "minecraft:chests/jungle_temple"));
        }

        // the ACT + the manual cabinet row (:112-124).
        set(aLevel, aClip, 1, 1, 4, gregtech6.registry.GTMachines.ADVANCED_CRAFTING_TABLE.get().defaultBlockState()
                .setValue(gregtech6.block.GTAdvancedCraftingTableBlock.FACING, Direction.EAST));
        smooth(aLevel, aClip, 1, 2, 4); // the measuring pot → shell
        smooth(aLevel, aClip, 4, 1, 1); // the primary mass storage → shell
        smooth(aLevel, aClip, 4, 2, 1); // the secondary mass storage → shell
        Block tShelf = GT6StaticStorages.blockByPath("bookshelf_oak");
        if (tShelf != null) {
            placeStorage(aLevel, aClip, 4, 3, 1, tShelf, Direction.SOUTH, tBE -> {
                if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage) {
                    int tSlot = 0;
                    for (String tPath : WORKSHOP_MANUALS) {
                        var tItem = gregtech6.registry.GT6Books.ITEMS_BY_PATH.get(tPath);
                        if (tItem != null) tStorage.getInventory().setStackInSlot(tSlot++, new ItemStack(tItem.get()));
                    }
                    // the key stash (:119-123): the piece carries the dungeon's Workshop
                    // draw; a passing draw marks key[draw] generated and hides it at the
                    // cabinet slot 10+next(18) ∈ [10,27] (the upstream :122 "s" slot draw
                    // — rows 10..27 of the 28-slot cabinet). The two CUT manuals and the
                    // duct-tape rows (:118) leave 6..9 empty; the key range is untouched.
                    if (mKeyIndex >= 0 && mKeyIds != null) {
                        tStorage.getInventory().setStackInSlot(10 + aRandom.nextInt(18),
                                gregtech6.items.GT6Keys.dungeonStack(
                                        gregtech6.items.GT6Keys.KEYS.get(aRandom.nextInt(gregtech6.items.GT6Keys.KEYS.size())).get(),
                                        mKeyIndex, mKeyIds[mKeyIndex]));
                    }
                }
            });
        }

        // the smithy wall (:132-145).
        createVanillaChest(aLevel, aClip, aRandom, 14, 1, 1, Direction.WEST, "chests/village/village_weaponsmith");
        smooth(aLevel, aClip, 14, 1, 2);
        smooth(aLevel, aClip, 14, 1, 3); // the crucible → shell
        smooth(aLevel, aClip, 14, 1, 4);
        ingotOrPlate(aLevel, aClip, aRandom, 14, 1, 5);
        ingotOrPlate(aLevel, aClip, aRandom, 10, 1, 1);
        set(aLevel, aClip, 11, 1, 1, Blocks.ANVIL.defaultBlockState()
                .setValue(AnvilBlock.FACING, Direction.from2DDataValue(aRandom.nextInt(4))));
        set(aLevel, aClip, 12, 1, 1, Blocks.GRINDSTONE.defaultBlockState()
                .setValue(GrindstoneBlock.FACING, Direction.SOUTH).setValue(GrindstoneBlock.FACE, AttachFace.FLOOR));
        smooth(aLevel, aClip, 11, 1, 4); // the tool rack (with the hammer) → shell
        ingotOrPlate(aLevel, aClip, aRandom, 11, 1, 5);
        smooth(aLevel, aClip, 14, 2, 2); // the mold → shell
        smooth(aLevel, aClip, 14, 2, 3); // the mold holder → shell
        smooth(aLevel, aClip, 14, 2, 4); // the mold → shell

        // the brew corner (:149-170).
        Block tCrate = GT6StaticStorages.blockByPath("bottlecrate_oak");
        if (tCrate != null) {
            placeStorage(aLevel, aClip, 11, 1, 14, tCrate, Direction.from2DDataValue(aRandom.nextInt(4)), tBE -> {
                if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage) {
                    // the mercury/glue/lube/ink bottle rows omit (the GT fluid bottles are
                    // unported); the glass-bottle fill carries the crate face.
                    for (int i = 0, n = 1 + aRandom.nextInt(8); i < n; i++) {
                        tStorage.getInventory().setStackInSlot(i, new ItemStack(Items.GLASS_BOTTLE));
                    }
                }
            });
        }
        smooth(aLevel, aClip, 13, 1, 14); // the mixing bowl → shell
        smooth(aLevel, aClip, 14, 1, 14);
        set(aLevel, aClip, 14, 1, 13, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 1 + aRandom.nextInt(3)));
        smooth(aLevel, aClip, 14, 1, 11); // the bathing pot → shell
        smooth(aLevel, aClip, 13, 2, 14); // the tap → shell
        smooth(aLevel, aClip, 14, 2, 14); // the water drum → shell
        smooth(aLevel, aClip, 14, 2, 13); // the tap → shell
        smooth(aLevel, aClip, 14, 3, 14); // the funnel → shell

        // the drinks wall (:174-205) — the barrel/cylinder columns → shells, one random
        // bottle crate break.
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
            if (aRandom.nextInt(3) < 2) {
                for (int tY = 1; tY <= 3; tY++) {
                    if (tCrate != null && aRandom.nextInt(3) == 0) {
                        placeStorage(aLevel, aClip, 1 + i, tY, 12 + j, tCrate,
                                Direction.from2DDataValue(aRandom.nextInt(4)), tBE -> {
                                    if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage) {
                                        for (int b = 0, n = 1 + aRandom.nextInt(8); b < n; b++) {
                                            tStorage.getInventory().setStackInSlot(b, new ItemStack(Items.GLASS_BOTTLE));
                                        }
                                    }
                                });
                        break;
                    }
                    smooth(aLevel, aClip, 1 + i, tY, 12 + j); // the drink barrel → shell
                    if (aRandom.nextInt(3) == 0) break;
                }
            } else if (aRandom.nextInt(3) < 2) {
                smooth(aLevel, aClip, 1 + i, 1, 12 + j); // the gas cylinder → shell
            }
        }
    }

    /** One drawer fill row — the first {@code aCount} of the prefix, skipped when unregistered. */
    private int fillDrawer(GT6StaticStorageBaseBlockEntity aStorage, int aSlot, RandomSource aRandom,
            OreDictMaterial aMaterial, int aCount, OreDictPrefix aPrefix) {
        var tHandle = GTMaterialItems.get(aPrefix, aMaterial);
        if (tHandle == null || aSlot >= aStorage.getInventory().getSlots()) return aSlot;
        aStorage.getInventory().setStackInSlot(aSlot, new ItemStack(tHandle.get(), aCount));
        return aSlot + 1;
    }

    /** The ingot/plate pile (upstream {@code ingots_or_plates} :259) → the material block, smooth fallback. */
    private void ingotOrPlate(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom, int aLX, int aLY, int aLZ) {
        OreDictMaterial[] tMetals = workshopMetals();
        OreDictMaterial tMat = tMetals[aRandom.nextInt(tMetals.length)];
        Block tIngot = materialBlock(OP.blockIngot, tMat), tPlate = materialBlock(OP.blockPlate, tMat);
        Block tUse = tIngot != null && tPlate != null ? (aRandom.nextBoolean() ? tPlate : tIngot)
                : tIngot != null ? tIngot : tPlate;
        if (tUse != null) set(aLevel, aClip, aLX, aLY, aLZ, tUse.defaultBlockState());
        else smooth(aLevel, aClip, aLX, aLY, aLZ);
    }

    /** The registered gt6 material block of a (prefix, material) pair — null when unregistered. */
    private Block materialBlock(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
        String tPath = GTMaterialItems.itemIdOf(aPrefix, aMaterial);
        //? if forge {
        Block tBlock = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                new net.minecraft.resources.ResourceLocation("gt6", tPath));
        //?} else {
        /*Block tBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tPath));
        *///?}
        return tBlock == null || tBlock == Blocks.AIR ? null : tBlock;
    }

    /**
     * The gt6-carrier loot chest at a LOCAL position with a facing — the workshop's
     * DUNGEON_CHEST row (:60, the {@code createDungeonChest} face with a direction).
     */
    private void createGtChest(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom,
            int aLX, int aLY, int aLZ, Direction aFacing, String aTablePath) {
        BlockPos tPos = new BlockPos(wx(aLX), wy(aLY), wz(aLZ));
        if (!aClip.isInside(tPos)) return;
        //? if forge {
        this.createChest(aLevel, aClip, aRandom, tPos,
                new net.minecraft.resources.ResourceLocation("gt6", aTablePath),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, aFacing));
        //?} else {
        /*this.createChest(aLevel, aClip, aRandom, tPos,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", aTablePath)),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, aFacing));
        *///?}
    }

    // ---------------------------------------------------------------- farms

    /** The farm crop roster — the vanilla rows of the upstream list (:59-124); the ~50
     * Pam/HaC rows are the mod-专属 never pool. Ages roll per placement. */
    private static final Block[] FARM_CROPS = {Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT, Blocks.BEETROOTS};

    /** The vanilla flower roster (the upstream GT flower rolls fold). */
    private static final Block[] FARM_FLOWERS = {
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
            Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY};

    /** The vanilla sapling roster (the upstream GT Saplings_AB/CD rolls fold). */
    private static final Block[] FARM_SAPLINGS = {
            Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING};

    /**
     * The crop farm interior (upstream {@code DungeonChunkRoomFarmCrop} :41-226): the
     * slab ring walls at y1/5/6, the four irrigated corner plots, the four planter
     * columns, and the log-and-cocoa gardens on the free sides.
     *
     * <p>Declared folds: the Glowtus MTE omits (unported); the plant-pot MTE 32065 → the
     * smooth shell; the GT saplings/flowers → the vanilla rosters; the Pam crop rows →
     * the vanilla four; the melon/pumpkin quadrant rolls stems vs ripe fruit.
     */
    private void buildFarmCrop(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        // the ring walls (:41-56): the corner-square borders on both axes, y1/5/6.
        BlockState tSlab = Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int tCoord = 1; tCoord <= 14; tCoord++) if (tCoord <= 4 || tCoord >= 11) {
            for (int tY : new int[] {1, 5, 6}) {
                set(aLevel, aClip, tCoord, tY, 5, tSlab);
                set(aLevel, aClip, tCoord, tY, 10, tSlab);
                set(aLevel, aClip, 5, tY, tCoord, tSlab);
                set(aLevel, aClip, 10, tY, tCoord, tSlab);
            }
        }

        // the corner plots (:126-144).
        for (int tX = 1; tX <= 14; tX++) for (int tZ = 1; tZ <= 14; tZ++) {
            if (!((tX <= 4 || tX >= 11) && (tZ <= 4 || tZ >= 11))) continue;
            lamp(aLevel, aClip, tX, 5, tZ);
            if (tX >= 4 && tX <= 11 && tZ >= 4 && tZ <= 11) {
                set(aLevel, aClip, tX, 1, tZ, Blocks.WATER.defaultBlockState());
            } else if (tX >= 8 && tZ >= 8) {
                if (aRandom.nextBoolean()) {
                    set(aLevel, aClip, tX, 2, tZ, (aRandom.nextBoolean() ? Blocks.MELON_STEM : Blocks.PUMPKIN_STEM)
                            .defaultBlockState().setValue(StemBlock.AGE, aRandom.nextInt(8)));
                } else {
                    set(aLevel, aClip, tX, 2, tZ, (aRandom.nextBoolean() ? Blocks.MELON : Blocks.PUMPKIN).defaultBlockState());
                }
            } else {
                // moisture 7 = the vanilla MAX (the property is 0..7 — 15 is the upstream
                // GT fertilized-face read, and setting 15 on the vanilla block CRASHES the
                // chunk decoration task, the LeverBlock.FACING crash class)
                set(aLevel, aClip, tX, 1, tZ, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                set(aLevel, aClip, tX, 2, tZ, cropStack(aRandom));
            }
        }

        // the four planter columns (:146-149): the pot MTE → shell + the reed/cactus columns.
        planterColumn(aLevel, aClip, 5, 5, true);
        planterColumn(aLevel, aClip, 5, 10, false);
        planterColumn(aLevel, aClip, 10, 5, false);
        planterColumn(aLevel, aClip, 10, 10, true);

        // the side gardens on the FREE sides (:152-226, the mRoomLayout[+1]==0 gate).
        for (Direction tSide : Direction.Plane.HORIZONTAL) if (!doorAt(tSide)) {
            buildCropGarden(aLevel, aClip, aRandom, tSide);
        }
    }

    /** One random crop with a random age (the beetroot's 4-step ladder folded per block). */
    private BlockState cropStack(RandomSource aRandom) {
        Block tCrop = FARM_CROPS[aRandom.nextInt(FARM_CROPS.length)];
        if (tCrop == Blocks.BEETROOTS) return tCrop.defaultBlockState().setValue(BeetrootBlock.AGE, aRandom.nextInt(4));
        return tCrop.defaultBlockState().setValue(CropBlock.AGE, aRandom.nextInt(8));
    }

    /** One planter column (:146-149): the shell + three sugar cane / cactus blocks. */
    private void planterColumn(WorldGenLevel aLevel, BoundingBox aClip, int aX, int aZ, boolean aReeds) {
        Block tPlant = aReeds ? Blocks.SUGAR_CANE : Blocks.CACTUS;
        smooth(aLevel, aClip, aX, 1, aZ);
        for (int tY = 2; tY <= 4; tY++) set(aLevel, aClip, aX, tY, aZ, tPlant.defaultBlockState());
    }

    /**
     * One side garden (the EAST shape :152-169, parametric): the slab bookends, the
     * jungle-log row with cocoa, the sapling planters, and the flower row with the one
     * double plant.
     */
    private void buildCropGarden(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom, Direction aSide) {
        boolean tPositive = aSide == Direction.EAST || aSide == Direction.SOUTH;
        int tWall = tPositive ? 14 : 1, tGarden = tPositive ? 13 : 2;
        for (int tV = 6; tV <= 9; tV++) {
            // the slab bookends at v5/v10, the log row, the cocoa row.
            if (tV == 6) {
                sideSet(aLevel, aClip, aSide, tWall, 3, 5, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                sideSet(aLevel, aClip, aSide, tWall, 3, 10, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
            sideSet(aLevel, aClip, aSide, tWall, 3, tV, Blocks.JUNGLE_LOG.defaultBlockState());
            sideSet(aLevel, aClip, aSide, tGarden, 3, tV, Blocks.COCOA.defaultBlockState()
                    .setValue(CocoaBlock.FACING, aSide.getOpposite()).setValue(CocoaBlock.AGE, aRandom.nextInt(3)));
            // the planter + sapling (:160-163).
            sideSet(aLevel, aClip, aSide, tWall, 1, tV, face(StoneVariant.SMOTH, 1, true));
            sideSet(aLevel, aClip, aSide, tWall, 2, tV, FARM_SAPLINGS[aRandom.nextInt(FARM_SAPLINGS.length)].defaultBlockState());
            // the planter + flower row (:165-168).
            sideSet(aLevel, aClip, aSide, tWall, 4, tV, face(StoneVariant.SMOTH, 4, true));
            sideSet(aLevel, aClip, aSide, tWall, 5, tV, FARM_FLOWERS[aRandom.nextInt(FARM_FLOWERS.length)].defaultBlockState());
        }
        // the double plant (:166/:204/:224) — lilac/sunflower/rose bush/peony by side.
        int tVDouble = tPositive ? 7 : 8;
        Block tDouble = switch (aSide) {
            case EAST -> Blocks.LILAC;
            case WEST -> Blocks.SUNFLOWER;
            case SOUTH -> Blocks.ROSE_BUSH;
            default -> Blocks.PEONY;
        };
        sideSet(aLevel, aClip, aSide, tWall, 5, tVDouble, tDouble.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        sideSet(aLevel, aClip, aSide, tWall, 6, tVDouble, tDouble.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * The fish pond (upstream {@code DungeonChunkRoomFarmFish} :44-80): the accent-brick
     * pond dug below the floor, the water fill, and the four bonus-crystal loot crates.
     * The HaC fish trap omits (mod-专属 never); the Glowtus omits.
     */
    private void buildFarmFish(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        for (int tX = 3; tX <= 12; tX++) for (int tZ = 3; tZ <= 12; tZ++) {
            if (tX == 3 || tX == 12 || tZ == 3 || tZ == 12) {
                colored(aLevel, aClip, tX, 0, tZ);
                colored(aLevel, aClip, tX, -1, tZ);
                bricks(aLevel, aClip, tX, -2, tZ);
            } else {
                bricks(aLevel, aClip, tX, -3, tZ);
                colored(aLevel, aClip, tX, -2, tZ);
                set(aLevel, aClip, tX, 0, tZ, Blocks.WATER.defaultBlockState());
                set(aLevel, aClip, tX, -1, tZ, Blocks.WATER.defaultBlockState());
            }
        }
        // the bonus crates (:77-80), the facing toward the room center.
        if (aRandom.nextBoolean()) createVanillaChest(aLevel, aClip, aRandom, 1, 1, 1, Direction.SOUTH, "chests/spawn_bonus_chest");
        if (aRandom.nextBoolean()) createVanillaChest(aLevel, aClip, aRandom, 14, 1, 1, Direction.WEST, "chests/spawn_bonus_chest");
        if (aRandom.nextBoolean()) createVanillaChest(aLevel, aClip, aRandom, 1, 1, 14, Direction.EAST, "chests/spawn_bonus_chest");
        if (aRandom.nextBoolean()) createVanillaChest(aLevel, aClip, aRandom, 14, 1, 14, Direction.NORTH, "chests/spawn_bonus_chest");
    }

    /** The upstream platform boolean row (:149) — index 0..15, rows/cols 1..14. */
    private static final boolean[] MOB_PLATFORM_MASK = {
            false, true, true, false, false, false, true, true, true, true, false, false, false, true, true, false};

    /**
     * The mob-drop tower interior (upstream {@code DungeonChunkRoomFarmMobs} :59-123):
     * the central pillar, the spill platforms (licensed by {@link #mFree}), the lamp
     * replacements, and the drop-collection center.
     *
     * <p>Declared folds: the Omni-Spikes omit (unported — the kill face of the farm is
     * the successor MTE card's seam); the item pipes 25377 → smooth shells; the mass
     * storages 6009 → the ported drawer BE carrying the vanilla mob-drop rows; the
     * hoppers 8010 → the ported gt6 hopper; the GT wood arrow → the vanilla arrow.
     */
    private void buildFarmMobs(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        // the solid pillar (:59-60).
        for (int tY = 1; tY <= 6; tY++) for (int tX = 6; tX <= 9; tX++) for (int tZ = 6; tZ <= 9; tZ++) {
            bricks(aLevel, aClip, tX, tY, tZ);
        }
        for (int tY = 7; tY <= 8; tY++) for (int tX = 5; tX <= 10; tX++) for (int tZ = 5; tZ <= 10; tZ++) {
            smalltiles(aLevel, aClip, tX, tY, tZ);
        }

        // the platforms (:63-71) — the in-chunk build plus the licensed spills.
        buildMobPlatforms(aLevel, aClip, 0, 0);
        if (freeDiag(0) || freeDiag(2)) buildMobPlatforms(aLevel, aClip, -16, 0);
        if (freeDiag(1) || freeDiag(3)) buildMobPlatforms(aLevel, aClip, 16, 0);
        if (freeDiag(0) || freeDiag(1)) buildMobPlatforms(aLevel, aClip, 0, -16);
        if (freeDiag(2) || freeDiag(3)) buildMobPlatforms(aLevel, aClip, 0, 16);
        if (freeDiag(0)) buildMobPlatforms(aLevel, aClip, -16, -16);
        if (freeDiag(1)) buildMobPlatforms(aLevel, aClip, 16, -16);
        if (freeDiag(2)) buildMobPlatforms(aLevel, aClip, -16, 16);
        if (freeDiag(3)) buildMobPlatforms(aLevel, aClip, 16, 16);

        // the lamp replacements (:74-85).
        for (int[] tLamp : new int[][] {{3, 3}, {3, 6}, {3, 9}, {3, 12}, {6, 3}, {9, 3}, {6, 12}, {9, 12},
                {12, 3}, {12, 6}, {12, 9}, {12, 12}}) {
            lamp(aLevel, aClip, tLamp[0], 6, tLamp[1]);
        }

        // the drop-collection center (:88-123): the down-pipes → shells, the mass
        // storages → drawer BEs with the mob drops, the restrictor seats stay stone.
        smooth(aLevel, aClip, 8, 6, 8);
        smooth(aLevel, aClip, 8, 5, 8);
        smooth(aLevel, aClip, 8, 4, 8);
        Block tDrawer = GT6StaticStorages.blockByPath("drawer_bronze");
        for (int[] tCell : new int[][] {
                {6, 3, 7, 0}, {6, 3, 8, 1}, {7, 3, 6, 2}, {7, 3, 9, 3}, {8, 3, 6, 4}, {8, 3, 9, 5},
                {9, 3, 7, 6}, {9, 3, 8, 7},
                {6, 2, 8, 8}, {7, 2, 9, 9}, {8, 2, 6, 10}, {8, 2, 9, 11}, {9, 2, 7, 12}, {9, 2, 8, 13}}) {
            int tX = tCell[0], tY = tCell[1], tZ = tCell[2];
            if (tDrawer != null) {
                Item tDrop = MOB_DROPS[tCell[3]];
                placeStorage(aLevel, aClip, tX, tY, tZ, tDrawer, Direction.from2DDataValue((tX + tZ) & 3), tBE -> {
                    if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage) {
                        tStorage.getInventory().setStackInSlot(0, new ItemStack(tDrop, 1 + aRandom.nextInt(8)));
                    }
                });
            } else {
                smooth(aLevel, aClip, tX, tY, tZ);
            }
        }
        // the empty drawer + the restrictor pipe seats (:108-123).
        Block tEmptyDrawer = GT6StaticStorages.blockByPath("drawer_bronze");
        if (tEmptyDrawer != null) {
            placeStorage(aLevel, aClip, 7, 2, 6, tEmptyDrawer, Direction.WEST, tBE -> {
            });
        } else {
            smooth(aLevel, aClip, 7, 2, 6);
        }
        for (int[] tSeat : new int[][] {{6, 2, 7}, {7, 3, 7}, {7, 3, 8}, {7, 2, 7}, {7, 2, 8}, {8, 3, 7}, {8, 3, 8},
                {8, 2, 7}, {8, 2, 8}}) {
            smooth(aLevel, aClip, tSeat[0], tSeat[1], tSeat[2]); // the item pipes → shells
        }
        for (int[] tAnchor : new int[][] {{6, 3, 6}, {6, 3, 9}, {9, 3, 6}, {9, 3, 9},
                {6, 2, 6}, {6, 2, 9}, {9, 2, 6}, {9, 2, 9}}) {
            chiseled(aLevel, aClip, tAnchor[0], tAnchor[1], tAnchor[2]);
        }
        smooth(aLevel, aClip, 6, 2, 7); // the restrictor keep-out (the upstream :109 comment)
    }

    /** The vanilla mob-drop roster of the collection drawers (:92-122, the GT rows fold). */
    private static final Item[] MOB_DROPS = {
            Items.GLASS_BOTTLE, Items.SLIME_BALL, Items.STRING, Items.REDSTONE,
            Items.SPIDER_EYE, Items.GLOWSTONE_DUST, Items.BONE, Items.STICK,
            Items.FEATHER, Items.GUNPOWDER, Items.ROTTEN_FLESH, Items.SUGAR,
            Items.ARROW, Items.ARROW};

    /**
     * One spill platform (upstream {@code makePlatForms} :128-203) at the LOCAL offset:
     * the roof, the water floor, the hollow walls, the mossy spawn platforms, the hopper
     * pit (the spikes → shells), and the corner water feeds.
     */
    private void buildMobPlatforms(WorldGenLevel aLevel, BoundingBox aClip, int aOX, int aOZ) {
        for (int tX = aOX; tX <= aOX + 15; tX++) for (int tZ = aOZ; tZ <= aOZ + 15; tZ++) {
            // the roof, two blocks thick (:131-133).
            tiles(aLevel, aClip, tX, 43, tZ);
            smalltiles(aLevel, aClip, tX, 42, tZ);
            // the water floor (:134-136).
            smalltiles(aLevel, aClip, tX, 8, tZ);
            tiles(aLevel, aClip, tX, 7, tZ);
            tiles(aLevel, aClip, tX, 6, tZ);
            // the hollow walls (:137-146).
            for (int tY = 9; tY < 42; tY++) {
                if (tX == aOX || tX == aOX + 15 || tZ == aOZ || tZ == aOZ + 15) {
                    bricks(aLevel, aClip, tX, tY, tZ);
                } else {
                    air(aLevel, aClip, tX, tY, tZ);
                }
            }
        }

        // the mossy spawn platforms (:149-152).
        for (int tY = 12; tY < 42; tY++) if (tY % 3 == 0) {
            for (int i = 1; i <= 14; i++) for (int j = 1; j <= 14; j++) {
                if (MOB_PLATFORM_MASK[i] || MOB_PLATFORM_MASK[j]) {
                    set(aLevel, aClip, aOX + i, tY, aOZ + j, face(StoneVariant.MCOBL, tY, true));
                }
            }
        }

        // the spike pit → shells (:154-159).
        for (int tX = aOX + 7; tX <= aOX + 8; tX++) for (int tZ = aOZ + 7; tZ <= aOZ + 8; tZ++) {
            smooth(aLevel, aClip, tX, 9, tZ);
        }

        // the hoppers (:161-165) → the ported gt6 hoppers with the upstream out-faces.
        Block tHopper = GT6Hoppers.BLOCKS_BY_PATH.get("hopper_steel").get();
        set(aLevel, aClip, aOX + 7, 8, aOZ + 7, tHopper.defaultBlockState().setValue(GT6Hoppers.GT6HopperBlock.FACING, Direction.EAST));
        set(aLevel, aClip, aOX + 7, 8, aOZ + 8, tHopper.defaultBlockState().setValue(GT6Hoppers.GT6HopperBlock.FACING, Direction.EAST));
        set(aLevel, aClip, aOX + 8, 8, aOZ + 7, tHopper.defaultBlockState().setValue(GT6Hoppers.GT6HopperBlock.FACING, Direction.SOUTH));
        set(aLevel, aClip, aOX + 8, 8, aOZ + 8, tHopper.defaultBlockState().setValue(GT6Hoppers.GT6HopperBlock.FACING, Direction.DOWN));

        // the item-pipe rows → shells (:167-179).
        set(aLevel, aClip, aOX + 7, 7, aOZ + 7, face(StoneVariant.CHISL, 7, true));
        set(aLevel, aClip, aOX + 7, 7, aOZ + 8, face(StoneVariant.CHISL, 7, true));
        set(aLevel, aClip, aOX + 8, 7, aOZ + 7, face(StoneVariant.CHISL, 7, true));
        smooth(aLevel, aClip, aOX + 8, 7, aOZ + 8);
        if (aOX > 0) for (int tX = aOX - 7; tX <= aOX + 7; tX++) smooth(aLevel, aClip, tX, 7, aOZ + 8);
        if (aOX < 0) for (int tX = aOX + 9; tX <= aOX + 23; tX++) smooth(aLevel, aClip, tX, 7, aOZ + 8);
        if (aOZ > 0) for (int tZ = aOZ - 7; tZ <= aOZ + 7; tZ++) smooth(aLevel, aClip, aOX + 8, 7, tZ);
        if (aOZ < 0) for (int tZ = aOZ + 9; tZ <= aOZ + 23; tZ++) smooth(aLevel, aClip, aOX + 8, 7, tZ);

        // the mossy spill pads + the corner water feeds (:182-201).
        int[][] tPads = {{1, 1}, {2, 1}, {3, 1}, {4, 1}, {1, 2}, {2, 2}, {3, 2}, {1, 3}, {2, 3}, {1, 4},
                {14, 1}, {13, 1}, {12, 1}, {11, 1}, {14, 2}, {13, 2}, {12, 2}, {14, 3}, {13, 3}, {14, 4},
                {1, 14}, {2, 14}, {3, 14}, {4, 14}, {1, 13}, {2, 13}, {3, 13}, {1, 12}, {2, 12}, {1, 11},
                {14, 14}, {13, 14}, {12, 14}, {11, 14}, {14, 13}, {13, 13}, {12, 13}, {14, 12}, {13, 12}, {14, 11}};
        for (int[] tPad : tPads) {
            set(aLevel, aClip, aOX + tPad[0], 9, aOZ + tPad[1], face(StoneVariant.MCOBL, 9, true));
        }
        for (int[] tCorner : new int[][] {{1, 1}, {1, 14}, {14, 1}, {14, 14}}) {
            set(aLevel, aClip, aOX + tCorner[0], 10, aOZ + tCorner[1], Blocks.WATER.defaultBlockState());
        }
    }

    // ---------------------------------------------------------------- mining bedrock

    /**
     * The mining-room material roster (upstream :39 verbatim) — method-body lazy, the
     * {@link #workshopMetals()} static-init-guard form.
     */
    private static OreDictMaterial[] miningMaterials() {
        return new OreDictMaterial[] {
                MT.Redstone, MT.S, MT.Fe2O3, MT.MnO2, MT.Apatite,
                MT.OREMATS.Molybdenite, MT.OREMATS.Bauxite, MT.OREMATS.Sphalerite,
                MT.OREMATS.Tetrahedrite, MT.OREMATS.Cassiterite, MT.OREMATS.Garnierite, MT.OREMATS.Galena};
    }

    /**
     * The mining pit (upstream {@code DungeonChunkRoomMiningBedrock} :44-135): the
     * 16x16 shaft dug 15 blocks below the floor, the ladders and bars, the pit bottom
     * with the dynamite charges, and the raw-ore piles of the rolled vein material.
     *
     * <p>Declared deviation — the forced vein carve (upstream :40 {@code
     * WorldgenOresBedrock.generateVein}) folds: the p31 bedrock-ore Feature already rolls
     * veins per chunk independently (this chunk included), and the forcing call needs the
     * package-private {@code GT6BedrockOreGenerator.BedrockSink}, outside this card's
     * files scope. The raw-ore piles carry the mining face; the upstream vein-failure
     * re-draw (return F -> another room) folds with the piece-architecture dispatch.
     *
     * <p>Declared MTE folds: the scaffolds 8408/8410 → the vanilla ladder; the bars
     * Bars_Brass/Steel → the vanilla iron bars; the dynamites 32104/32713/32712 → the
     * vanilla TNT; the raw-ore piles {@code BlocksGT.blockRaw} → the material blockRaw.
     */
    private void buildMiningBedrock(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        OreDictMaterial[] tMats = miningMaterials();
        OreDictMaterial tMat = tMats[aRandom.nextInt(tMats.length)];

        // the shaft (:44-68): LOCAL y -15..-1 = world 5..19; the border stays brick and
        // the four wall ladders sit only on the CONNECTED sides (the doorAt gates).
        for (int tY = -15; tY < 0; tY++) for (int tX = 0; tX <= 15; tX++) for (int tZ = 0; tZ <= 15; tZ++) {
            boolean tBorder = tX == 0 || tX == 15 || tZ == 0 || tZ == 15;
            BlockState tLadder = null;
            if (tZ == 7 && tX == 2 && doorAt(Direction.WEST)) tLadder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
            else if (tZ == 7 && tX == 13 && doorAt(Direction.EAST)) tLadder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
            else if (tX == 7 && tZ == 2 && doorAt(Direction.NORTH)) tLadder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            else if (tX == 7 && tZ == 13 && doorAt(Direction.SOUTH)) tLadder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
            if (tBorder) {
                bricks(aLevel, aClip, tX, tY, tZ);
            } else if (tLadder != null) {
                set(aLevel, aClip, tX, tY, tZ, tLadder);
            } else if (tY == -1 && (tX == 2 || tX == 13 || tZ == 2 || tZ == 13)) {
                bricks(aLevel, aClip, tX, tY, tZ);
            } else {
                air(aLevel, aClip, tX, tY, tZ);
            }
        }

        // the floor ring (:70-94): the bars fence + the ladder tops + the corner lamps.
        for (int tX = 2; tX <= 13; tX++) for (int tZ = 2; tZ <= 13; tZ++) {
            boolean tCorner = (tX == 2 || tX == 13) && (tZ == 2 || tZ == 13);
            if (tX == 2 || tX == 13 || tZ == 2 || tZ == 13) {
                if (tCorner) {
                    set(aLevel, aClip, tX, -1, tZ, face(StoneVariant.RSTBR, -1, true));
                    set(aLevel, aClip, tX, 0, tZ, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, Boolean.TRUE));
                } else {
                    set(aLevel, aClip, tX, 1, tZ, Blocks.IRON_BARS.defaultBlockState());
                    Direction tFace = tX == 2 ? Direction.EAST : tX == 13 ? Direction.WEST
                            : tZ == 2 ? Direction.SOUTH : Direction.NORTH;
                    set(aLevel, aClip, tX, 0, tZ, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, tFace));
                }
            } else {
                air(aLevel, aClip, tX, 0, tZ);
            }
        }

        // the crossing walkways over the pit (:96-110), only between OPPOSITE doors.
        if (doorAt(Direction.EAST) && doorAt(Direction.WEST)) {
            air(aLevel, aClip, 2, 1, 8);
            air(aLevel, aClip, 13, 1, 8);
            for (int tX = 3; tX <= 12; tX++) {
                set(aLevel, aClip, tX, 0, 8, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
                set(aLevel, aClip, tX, 1, 8, Blocks.IRON_BARS.defaultBlockState());
            }
        } else if (doorAt(Direction.SOUTH) && doorAt(Direction.NORTH)) {
            air(aLevel, aClip, 8, 1, 2);
            air(aLevel, aClip, 8, 1, 13);
            for (int tZ = 3; tZ <= 12; tZ++) {
                set(aLevel, aClip, 8, 0, tZ, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
                set(aLevel, aClip, 8, 1, tZ, Blocks.IRON_BARS.defaultBlockState());
            }
        }

        // the pit bottom (:112-125): the widened openings + the dynamite charges.
        for (int tX = 5; tX <= 10; tX++) for (int tZ = 5; tZ <= 10; tZ++) {
            if ((tX != 5 && tX != 10) || (tZ != 5 && tZ != 10)) air(aLevel, aClip, tX, -16, tZ);
        }
        for (int tX = 6; tX <= 9; tX++) for (int tZ = 6; tZ <= 9; tZ++) air(aLevel, aClip, tX, -17, tZ);
        set(aLevel, aClip, 6, -17, 6, Blocks.TNT.defaultBlockState());
        set(aLevel, aClip, 6, -17, 9, Blocks.TNT.defaultBlockState());
        set(aLevel, aClip, 9, -17, 6, Blocks.TNT.defaultBlockState());
        set(aLevel, aClip, 9, -17, 9, Blocks.TNT.defaultBlockState());

        // the raw-ore piles (:127-135): the four corner quadrants, the 3/4-2/3-1/2 stack.
        Block tRaw = materialBlock(OP.blockRaw, tMat);
        int[] tStart = {1, 11}, tEnd = {4, 14};
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
            for (int i = tStart[a]; i <= tEnd[a]; i++) for (int j = tStart[b]; j <= tEnd[b]; j++) {
                if (aRandom.nextInt(4) >= 3) continue;
                if (tRaw != null) set(aLevel, aClip, i, -15, j, tRaw.defaultBlockState());
                else smooth(aLevel, aClip, i, -15, j);
                if (aRandom.nextInt(3) >= 2) {
                    if (tRaw != null) set(aLevel, aClip, i, -14, j, tRaw.defaultBlockState());
                    else smooth(aLevel, aClip, i, -14, j);
                    if (aRandom.nextBoolean()) {
                        if (tRaw != null) set(aLevel, aClip, i, -13, j, tRaw.defaultBlockState());
                        else smooth(aLevel, aClip, i, -13, j);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- library

    /**
     * The Library room (task dungeon-library-zpm) — {@code DungeonChunkRoomLibrary}
     * (:46-193) + the Normal variant's display rows ({@code DungeonChunkRoomLibraryNormal}
     * :48-103), the interior over the shared room shell.
     *
     * <p>Verbatim faces: the 12×12 carpet at y1 in the INVERSED accent color
     * ({@code mColorInversed = bind4(15 - aColor)}, DungeonData :84); the plank ceiling
     * y7 + the bottom slab y8; the boundary rim y6 planks over the top-slab y5; the
     * interior slab y6 broken by the lamp lattice (the {@code lamp(x,6,z,+1)} face); the
     * four reading tables (the plank core + top-slab wings at y3); the corner pillars
     * y1..5; the wall band — planks at y3, the bookshelf seats y1/2/4/5; the four
     * furniture nooks in one of four rotations ({@code next(4)}, the cup always riding
     * the crafting-table seat); the display row per SOLID side (the {@code !doorAt}
     * mapping of the upstream {@code mRoomLayout[..] == 0} face) — the bottom-slab run
     * y1 v5..10 with the two bookshelf seats, the flower pots at the y2 trophy seats.
     *
     * <p>Deviations live on the class javadoc (the shelf/cup/coin MTE faces, the key
     * shelf, the ZPM seat → the loot-face migration). The room's loot chests — four
     * seats over the reading-table cores, the storage-room {@code nextInt(2)} roll —
     * close the method.
     */
    private void buildLibrary(WorldGenLevel aLevel, BoundingBox aClip, RandomSource aRandom) {
        int tPlank = aRandom.nextInt(6);
        BlockState tPlanks = LIBRARY_PLANKS[tPlank].defaultBlockState();
        BlockState tSlabTop = LIBRARY_SLABS[tPlank].defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState tSlabBottom = LIBRARY_SLABS[tPlank].defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState tShelf = Blocks.BOOKSHELF.defaultBlockState();

        // the reading carpet (:46-48) — the inversed accent color.
        for (int tX = 2; tX <= 13; tX++) for (int tZ = 2; tZ <= 13; tZ++) {
            set(aLevel, aClip, tX, 1, tZ, CARPETS[(15 - mColor) & 15].defaultBlockState());
        }

        // the ceiling, the rim and the lamp lattice (:50-61).
        for (int tX = 1; tX <= 14; tX++) for (int tZ = 1; tZ <= 14; tZ++) {
            set(aLevel, aClip, tX, 7, tZ, tPlanks);
            set(aLevel, aClip, tX, 8, tZ, tSlabBottom);
            if (tX == 1 || tX == 14 || tZ == 1 || tZ == 14) {
                set(aLevel, aClip, tX, 6, tZ, tPlanks);
                set(aLevel, aClip, tX, 5, tZ, tSlabTop);
            } else if ((tX == 3 || tX == 6 || tX == 9 || tX == 12) && (tZ == 3 || tZ == 6 || tZ == 9 || tZ == 12)) {
                lamp(aLevel, aClip, tX, 6, tZ);
            } else {
                set(aLevel, aClip, tX, 6, tZ, tSlabTop);
            }
        }

        // the four reading tables (:63-85): plank core at the wall corner + the 4 wings.
        int[][] tTables = {{2, 2, 2, 3, 2, 4, 3, 2, 4, 2}, {13, 2, 13, 3, 13, 4, 12, 2, 11, 2},
                {2, 13, 2, 12, 2, 11, 3, 13, 4, 13}, {13, 13, 13, 12, 13, 11, 12, 13, 11, 13}};
        for (int[] tTable : tTables) {
            set(aLevel, aClip, tTable[0], 3, tTable[1], tPlanks);
            for (int tW = 2; tW < 10; tW += 2) {
                set(aLevel, aClip, tTable[tW], 3, tTable[tW + 1], tSlabTop);
            }
        }

        // the corner pillars (:87-91) and the wall band (:93-157): planks at y3, the
        // bookshelf seats at y1/2/4/5 (the GT shelf MTE face — the class-javadoc shell).
        for (int tY = 1; tY <= 5; tY++) {
            set(aLevel, aClip, 1, tY, 1, tPlanks);
            set(aLevel, aClip, 14, tY, 1, tPlanks);
            set(aLevel, aClip, 1, tY, 14, tPlanks);
            set(aLevel, aClip, 14, tY, 14, tPlanks);
            BlockState tBand = tY == 3 ? tPlanks : tShelf;
            for (int tV : LIBRARY_SHELF_RUN) {
                set(aLevel, aClip, tV, tY, 1, tBand);
                set(aLevel, aClip, 14, tY, tV, tBand);
                set(aLevel, aClip, tV, tY, 14, tBand);
                set(aLevel, aClip, 1, tY, tV, tBand);
            }
        }

        // the four furniture nooks (:164-193): one rotation over the four seats, the
        // cup riding the crafting-table seat (the cup MTE face = the declared air).
        Block[] tFurniture = {Blocks.ENCHANTING_TABLE, Blocks.CRAFTING_TABLE, Blocks.JUKEBOX, Blocks.ENDER_CHEST};
        int tRoll = aRandom.nextInt(4);
        for (int tI = 0; tI < 4; tI++) {
            int[] tSeat = LIBRARY_NOOK_SEATS[(tI + tRoll) & 3];
            set(aLevel, aClip, tSeat[0], 1, tSeat[1], tFurniture[tI].defaultBlockState());
        }

        // the Normal-variant display row per solid side (LibraryNormal :48-103 — the
        // upstream mRoomLayout[..] == 0 face).
        for (Direction tSide : Direction.Plane.HORIZONTAL) if (!doorAt(tSide)) {
            buildLibraryDisplayRow(aLevel, aClip, tSide, tSlabBottom);
        }

        // the loot face: four chests over the reading-table cores (the storage-room
        // nextInt(2) roll), bound to the GT6 dungeon carrier — the migrated ZPM row
        // rides the same table (dungeon_inject_gt6_dungeon_chest).
        int[][] tChestSeats = {{2, 2}, {13, 2}, {2, 13}, {13, 13}};
        for (int[] tSeat : tChestSeats) {
            if (aRandom.nextInt(2) == 0) {
                createDungeonChest(aLevel, aClip, aRandom, wx(tSeat[0]), wy(4), wz(tSeat[1]));
            }
        }
    }

    /**
     * One display row against a solid side (LibraryNormal :48-103, mirrored): the
     * bottom-slab run v5..10 at y1 with the two bookshelf seats, the flower pots at the
     * y2 trophy seats. The coin seats (v5/v10, the 1-in-4 roll), the potion cup
     * (v7+rand(2)) and the ZPM roll take their declared shells (air / pot / the loot
     * face) — the class javadoc record.
     */
    private void buildLibraryDisplayRow(WorldGenLevel aLevel, BoundingBox aClip, Direction aSide, BlockState aSlab) {
        for (int tV = 5; tV <= 10; tV++) {
            sideSet(aLevel, aClip, aSide, 14, 1, tV,
                    tV == 6 || tV == 9 ? Blocks.BOOKSHELF.defaultBlockState() : aSlab);
        }
        for (int tV : LIBRARY_DISPLAY_SEATS) {
            sideSet(aLevel, aClip, aSide, 14, 2, tV, Blocks.FLOWER_POT.defaultBlockState());
        }
    }

    // ---------------------------------------------------------------- loot chests

    /**
     * The loot chests in the four corner nooks (upstream :272-336 crate stacks — the
     * MTE face defers, the chests carry the DUNGEON_CHEST 对位 table). The dungeon-keys
     * STORAGE proxy hide is GONE — the keys hide in the real rooms now (the Workshop
     * cabinet :119-123 + the Barracks shelves :113-152, the upstream positions).
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
