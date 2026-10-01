package gregtech6.block.surface;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.registries.RegistryObject;

import gregtech6.registry.GT6CropFoods;

/**
 * The GT6 wild berry bush with its growth domain (task bush-growth-blockstate) — the
 * blockstate-form port of upstream MTE 32759 "Berry Bush" (MultiTileEntityBush.java:59,
 * Loader_MultiTileEntities.java:2030, hardness 0.5 / resistance 0.3): the vanilla
 * {@link SweetBerryBushBlockShaped} shape, i.e. a {@code SweetBerryBushBlock} homolog —
 * {@link #AGE} ({@code BlockStateProperties.AGE_3}) mirrors the upstream {@code mStage}
 * 0-3 (MultiTileEntityBush.java:61, the growth cap :131) and {@link #KIND} carries the
 * berry identity the upstream NBT {@code mBerry} ItemStack did. ZERO block entities.
 * The vanilla {@link BushBlock} base keeps the ground-attach face.
 *
 * <p>The ported shape is the vanilla {@code SweetBerryBushBlock} homolog the card pinned
 * (SweetBerryBushBlock.java:30-123): same growth roll, same bonemeal trio, same harvest
 * pop. DECLARED DEVIATIONS (card spec, five — none silent):
 * <ol>
 * <li><b>128t deterministic beat → randomTick</b>: the upstream tick domain
 * ({@code SERVER_TIME % 128 == 0} beat, MultiTileEntityBush.java:105/:131, the
 * {@code mGrowth} byte-overflow counter :134 and the {@code mSpeed} soil scalar :126-128
 * all ride it) becomes the vanilla random-tick roll {@code nextInt(5) == 0}
 * (SweetBerryBushBlock.java:64 form) — same shape the saplings ride. The light gate
 * collapses to the vanilla one-shot {@code getRawBrightness(pos.above(), 0) >= 9} (:64),
 * which covers BOTH upstream branches (:133 sky-exposed via getSkyOffset, :137
 * {@code light > 9} via getLightLevelOffset). The tick-domain side faces retire with the
 * beat: the oxygen-death check (:91-93/:106), the snow-above cleanup (:98/:108) and the
 * cluster side-sync (:110-124) are BE faces with no random-tick seat.</li>
 * <li><b>mGrowth overflow counter CUT</b>: upstream accumulates {@code mSpeed}
 * half-units into a byte and advances {@code mStage} on wraparound (:134) — the AGE
 * property advances one step per successful roll instead (the vanilla integer form).</li>
 * <li><b>Rain bonus → bone meal</b>: the upstream raining exposure grants extra growth
 * increments (MultiTileEntityBush.java:135) — remapped to the vanilla
 * {@link BonemealableBlock} trio (SweetBerryBushBlock.java:110-123 verbatim), i.e. the
 * "extra growth" function moves from the weather to the player's hand. Chosen over the
 * plain cut because the vanilla seat already exists in the pinned homolog and costs three
 * one-liners, and because a random-tick rain re-roll would compound with deviation ① into
 * invisible growth variance.</li>
 * <li><b>Cluster shape stays CUT</b> (the w6-t2 declaration retained, now as
 * "maintained"): the upstream 6-block cluster (core + 4 facing sides + top,
 * WorldgenBushes.java:71-76/:89-96, the side sync/popOff MultiTileEntityBush.java:110-124)
 * collapses to one independent block per placement — consistent with the port's existing
 * single-block registration face and avoiding a 6× block-state spread.</li>
 * <li><b>Generic plantGtBerry planting face CUT</b>: the upstream right-click-a-berry
 * -onto-a-bush face (MultiTileEntityBush.java:173-178, OP.plantGtBerry + BushesGT.get)
 * stays unported — the {@link #KIND} quartet is fixed at the four worldgen kinds and
 * berries cannot be replanted. The Aether enchanted-grass {@code mSpeed = 2} row (:127)
 * is cut with it (foreign-mod compat, no port seat).</li>
 * </ol>
 *
 * <p>The {@link #KIND} quartet is the card ruling: the tint research card recorded the
 * BushesGT colour table as "3 berries + the string cotton" (CS.java:1581-1590 +
 * MultiItemFood.java:397/:405/:409) and pinned the KIND count at 4 (no expansion).
 * Factual note, declared: the upstream worldgen set is actually WIDER — BushesGT.MAP
 * receives 8 berry rows (MultiItemFood.java:397-429) plus the string default (:1589), so
 * the quartet is a card ruling, not the upstream full set. The berry carriers are the
 * T5a {@code GT6CropFoods} registrations; the cotton bush drops the vanilla
 * {@link Items#STRING} (WorldgenBushes.java:66 — the {@code Items.string} default; the
 * HarvestCraft {@code cottonItem} branch :69 is the foreign-conditional TRUE NEGATIVE).
 *
 * <p>The world face renders the upstream grayscale {@code bush.png} through the
 * tintindex-0 seat, tinted per state by the kind's BUSH BODY colour — the body colour is
 * stage-constant upstream (MultiTileEntityBush.java:236-240, {@code tBerryColor[0]} at
 * every stage; the stage colours [1..3] ride the berry OVERLAY textures, which stay CUT
 * with the berry visual). {@code GT6BushTintListener} answers the per-state arm.
 */
public final class GT6WildBushBlock extends BushBlock implements BonemealableBlock {

    /** The upstream stage cap (MultiTileEntityBush.java:131 {@code mStage < 3}; :61 {@code mStage}). */
    public static final int MAX_AGE = 3;
    /** The vanilla AGE_3 seat ↔ the upstream mStage 0-3 (SweetBerryBushBlock.java:33 form). */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    /** The vanilla light gate (SweetBerryBushBlock.java:64 {@code getRawBrightness(pos.above(), 0) >= 9}). */
    public static final int GROWTH_LIGHT = 9;

    /** The four worldgen kinds — the card ruling quartet (see the class javadoc factual note). */
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);

    /** The inset leaf-ball box (the non-full plant shape; MTE had a full-interact box). */
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public GT6WildBushBlock(Properties aProperties) {
        super(aProperties);
        // The default state = the cotton bush: the BushesGT.DEFAULT row (CS.java:1588) the
        // un-tinted item face has rendered since bushesgt-tint-color — zero visual drift.
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, Integer.valueOf(0)).setValue(KIND, Kind.COTTON));
    }

    //? if neoforge {
    /*
    // 21.1 made BlockBehaviour.codec() abstract (the vanilla 1.21 block-state codec
    // dispatch). The simpleCodec representative-value form is the vanilla StairBlock
    // precedent (the TestMachineBlock fork shape) — world save/load never runs through
    // this codec (the registry-id + property mapper does).
    @Override
    protected com.mojang.serialization.MapCodec<? extends GT6WildBushBlock> codec() {
        return simpleCodec(GT6WildBushBlock::new);
    }
    *///?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
        aBuilder.add(AGE, KIND);
    }

    @Override
    public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState aState) {
        // the mature bush leaves the tick roll entirely (SweetBerryBushBlock.java:57-59 form)
        return aState.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState aState, ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
        if (growthRoll(aState.getValue(AGE), aLevel.getRawBrightness(aPos.above(), 0), aRandom)) {
            aLevel.setBlock(aPos, aState.setValue(AGE, Integer.valueOf(aState.getValue(AGE) + 1)), 2);
        }
    }

    /**
     * The growth roll, the SweetBerryBushBlock.java:64 line split for the offline pin:
     * {@code age < 3 && nextInt(5) == 0 && rawBrightness(above) >= 9} — evaluation order
     * matters (the roll precedes the light read; a mature bush consumes no rng draw), so
     * the seam keeps the vanilla operand order verbatim.
     */
    static boolean growthRoll(int aAge, int aRawBrightnessAbove, RandomSource aRandom) {
        return aAge < MAX_AGE && aRandom.nextInt(5) == 0 && aRawBrightnessAbove >= GROWTH_LIGHT;
    }

    /**
     * The upstream harvest amount (MultiTileEntityBush.java:169 {@code ST.amount(1+rng(2), mBerry)})
     * — the seam the seeded distribution pin drives.
     */
    static int harvestAmount(RandomSource aRandom) {
        return 1 + aRandom.nextInt(2);
    }

    /**
     * The harvest face (the upstream onBlockActivated3 :165-171 — stage 3 pays
     * {@code 1 + rng(2)} berries and resets {@code mStage} to 0, NOT the vanilla reset-to-1;
     * the under-ripe bush passes through, :168 {@code return F}). The drop pops at the
     * block (the vanilla :93 popResource shape) instead of the upstream direct
     * {@code ST.give} into the inventory — the block-idiom deviation of the vanilla form.
     */
    //? if forge {
    @Override
    public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
        //?} else {
        /*@Override
        public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
        //21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
        //(javap BlockBehaviour 21.1.209); the game loop drives MAIN_HAND first.
        InteractionHand aHand = InteractionHand.MAIN_HAND;
        *///?}
        if (aState.getValue(AGE) != MAX_AGE) return InteractionResult.PASS; // the :168 under-ripe pass
        if (!aLevel.isClientSide()) {
            popResource(aLevel, aPos, new ItemStack(aState.getValue(KIND).berry(), harvestAmount(aLevel.random)));
            aLevel.setBlock(aPos, aState.setValue(AGE, Integer.valueOf(0)), 2); // the :170 reset-to-zero
        }
        return InteractionResult.sidedSuccess(aLevel.isClientSide());
    }

    /**
     * The bone-meal remap of the rain bonus (deviation ③) — the SweetBerryBushBlock.java:
     * 110-123 trio verbatim (target = under-ripe, always a success, one AGE step).
     */
    //? if forge {
    @Override
    public boolean isValidBonemealTarget(LevelReader aLevel, BlockPos aPos, BlockState aState, boolean aClient) {
        //?} else {
        /*@Override
        public boolean isValidBonemealTarget(LevelReader aLevel, BlockPos aPos, BlockState aState) {
        //21.1: the isClientSide param dropped (javap BonemealableBlock 21.1.209).
        *///?}
        return aState.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level aLevel, RandomSource aRandom, BlockPos aPos, BlockState aState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel aLevel, RandomSource aRandom, BlockPos aPos, BlockState aState) {
        aLevel.setBlock(aPos, aState.setValue(AGE, Integer.valueOf(Math.min(MAX_AGE, aState.getValue(AGE) + 1))), 2);
    }

    /**
     * One bush identity — the KIND quartet row. The colour is the upstream WORLD-render
     * BUSH BODY colour (MultiTileEntityBush.java:236-240 {@code tBerryColor[0]}, the
     * stage-constant face); the berry carrier is the T5a {@code GT6CropFoods} row id, or
     * {@code null} for the vanilla string carrier (WorldgenBushes.java:66).
     */
    public enum Kind implements net.minecraft.util.StringRepresentable {

        /** MultiItemFood.java:397 (BushesGT.put 0x22ff22 body). */
        BLUEBERRY(0x22ff22, "food_blueberry"),
        /** MultiItemFood.java:405 (BushesGT.put 0x44ff44 body). */
        CANDLEBERRY(0x44ff44, "food_candleberry"),
        /** MultiItemFood.java:409 (BushesGT.put 0x00dd00 body). */
        CRANBERRY(0x00dd00, "food_cranberry"),
        /** The string-cotton default bush (CS.java:1588/:1589 — the BushesGT.DEFAULT row). */
        COTTON(0x22cc22, null);

        /** The body colour, 0x00RRGGBB (the {@code bush.png} tint, stage-constant). */
        private final int mBodyColorRGB;
        /** The T5a food-row id, null = the vanilla string carrier. */
        private final String mFoodPath;
        private final String mSerializedName = name().toLowerCase(Locale.ROOT);

        Kind(int aBodyColorRGB, String aFoodPath) {
            mBodyColorRGB = aBodyColorRGB;
            mFoodPath = aFoodPath;
        }

        /** The world-face tint, 0xFFRRGGBB (the GT6BushTintListener per-state arm). */
        public int bodyColorARGB() {
            return 0xFF000000 | mBodyColorRGB;
        }

        /** The T5a food-row id, null on the cotton/string kind (the offline path-pin face). */
        public String foodPath() {
            return mFoodPath;
        }

        /**
         * The berry carrier item: the T5a {@code GT6CropFoods} registration, or
         * {@link Items#STRING} on the cotton kind. The FOODS scan is the strong tie to the
         * T5a band — a renamed row fails LOUD here (a silent registry null would NPE the
         * harvest far from the cause), and the GT6WildBushBlockTest pins the paths so the
         * drift can never ship.
         */
        public Item berry() {
            if (mFoodPath == null) return Items.STRING;
            for (RegistryObject<Item> tFood : GT6CropFoods.FOODS) {
                if (tFood.getId().getPath().equals(mFoodPath)) return tFood.get();
            }
            throw new IllegalStateException("the GT6CropFoods band lost the bush row: gt6:" + mFoodPath);
        }

        @Override
        public String getSerializedName() {
            return mSerializedName;
        }
    }
}
