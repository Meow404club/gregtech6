package gregtech6.block.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 ASPHALT block — the {@code gt.block.asphalt} family (upstream BlockAsphalt,
 * Loader_Blocks.java:59, task material-mc-g2-decor-misc). The upstream BlockColored
 * 16-meta dye ladder split into per-dye registrations (the GT6ConcreteBlock precedent —
 * the dye Bath economy pours per-colour outputs, Loader_Recipes_Other.java:448-449).
 *
 * <p>Numbers are the upstream ctor row verbatim (BlockAsphalt.java:44 → BlockMetaType
 * :61-62): hardnessMultiplier 1.0 x 1.5 = 1.5, resistanceMultiplier 1.0 x 10 = 10,
 * stone sound, harvest level 1 pickaxe (the mineable/pickaxe band, no
 * requiresCorrectToolForDrops — the stone-family red line).
 *
 * <p>THE WALK FACE (BlockAsphalt.java:62-67 verbatim): entities standing on the block
 * get their horizontal motion multiplied x1.3 each move tick while walking (x1.05 when
 * a half-block-of-the-same-family sits directly below — the stacked-path slipperiness
 * interaction; the port reads only the vanilla block-below slipperiness >= 0.8 arm, the
 * stacked-asphalt slab arm rides the slab defer). doesWalkSpeed = T for every meta.
 * The onWalkOver move-tick cadence maps to the vanilla {@code stepOn} hook (the GT6
 * walk-speed family face, the vanilla slime-block multiplication idiom).
 *
 * <p>Render: the ONE grayscale upstream PNG ({@code Textures.BlockIcons.ASPHALTS =
 * UT.Code.fill(ASPHALT, ..)}, Textures.java:701) + DYES_INT tint per FIXED dye index
 * (the GT6ConcreteTintListener route, {@code GT6DecorTintListener}).
 *
 * <p>Declared deviations: the Thaumcraft aspect rows (Loader_Blocks.java:60) and the
 * {@code MT.Asphalt.mTextureSolid} icon copy (:47) have no port surface; the OM.data
 * composition rows (:46/:58) keep the P8 ADR ⑥ posture; the mSlabs[0] slab face and
 * its Bath/crafting economy ride the slab-sweep defer.
 */
public class GT6AsphaltBlock extends Block {

	/** The full-block display template (one dye-unit slot) — the GT6EnUs/GT6ZhCn band. */
	public static final String BLOCK_NAME_KEY = "gt6.asphalt.block";

	/** The dye small-unit key of an id ({@code gt6.dye.light_gray} form) — the concrete source. */
	public static String dyeKey(int aDyeIndex) {
		return "gt6.dye." + gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}

	/** The FIXED dye index ({@code 0..15}, the CS.DYE_INDEX order 0=Black..15=White). */
	public final int dyeIndex;
	/** The registry snake id ({@code asphalt_light_gray} form) — the datagen/item source. */
	public final String snake;

	public GT6AsphaltBlock(String aSnake, int aDyeIndex, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.dyeIndex = aDyeIndex;
	}

	/** The upstream row numbers (BlockAsphalt.java:44 x BlockMetaType.java:61-62). */
	public static BlockBehaviour.Properties decorProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 10.0F).sound(SoundType.STONE);
	}

	/** The composed display name — the dye unit over the family template. */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(BLOCK_NAME_KEY,
				net.minecraft.network.chat.Component.translatable(dyeKey(this.dyeIndex)));
	}

	/** The upstream walk face (BlockAsphalt.java:62-67): x1.3 motion boost, x1.05 over ice. */
	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		if ((aEntity.getDeltaMovement().x != 0 || aEntity.getDeltaMovement().z != 0)
				&& !aEntity.isInWater() && !aEntity.isShiftKeyDown()) {
			double tSpeed = aLevel.getBlockState(aPos.below()).getBlock().getFriction() >= 0.8 ? 1.05 : 1.3;
			aEntity.setDeltaMovement(aEntity.getDeltaMovement().x * tSpeed, aEntity.getDeltaMovement().y,
					aEntity.getDeltaMovement().z * tSpeed);
		}
		super.stepOn(aLevel, aPos, aState, aEntity);
	}
}
