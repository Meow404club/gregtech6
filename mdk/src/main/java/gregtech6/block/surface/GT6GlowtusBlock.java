package gregtech6.block.surface;

import net.minecraft.world.level.block.WaterlilyBlock;

/**
 * The GT6 glowtus (task p30-w6-t2-surface-blocks) — the upstream water plant is a
 * {@code BlockBaseLilyPad} (BlockGlowtus.java:33, light 15 :52, placed on the water
 * surface by WorldgenGlowtus.java:55 {@code anywater && set(aY+1)}), so the modern block
 * extends the vanilla {@link WaterlilyBlock} (the mayPlaceOn water-top idiom verbatim).
 *
 * <p>DECLARED DEVIATION: the upstream 16 dye-coloured meta variants
 * (BlockGlowtus.java:39 {@code DYE_NAMES[i] + " Glowtus"}, the WorldgenGlowtus
 * {@code nextInt(16)} colour lottery) collapse to ONE red-sample block — per-pair x16
 * colour rows would buy 16 blocks for one cosmetic dimension (the coordinator-approved
 * ruling); the WorldgenGlowtus colour lottery rides the default state only.
 * The light level 15 rides the registration properties ({@code lightLevel}, the vanilla
 * glowstone row), the biomass/mortarize recipe faces are the recipe-domain defer.
 */
public final class GT6GlowtusBlock extends WaterlilyBlock {

    public GT6GlowtusBlock(Properties aProperties) {
        super(aProperties);
    }
}
