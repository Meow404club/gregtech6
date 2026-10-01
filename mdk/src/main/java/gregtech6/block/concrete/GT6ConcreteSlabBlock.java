package gregtech6.block.concrete;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 CONCRETE SLAB — the {@code mSlabs[0]} bottom-slab face of the concrete family
 * (the debt-slab-gap ruling applied to the second {@code BlockColored} universe): every
 * BlockMetaType family auto-creates six directional half-blocks (BlockMetaType.java:74-81)
 * of which only {@code mSlabs[0]} is player-visible (:83-87 hide the rest; the sawing row
 * :92 emits {@code ST.make(mSlabs[0], 2, i)} and the Bath dye rows :457-458 recolour it).
 * ONE slab block per (family, colour) pair over the vanilla {@link SlabBlock} ruling (the
 * GT6CFoamSlabBlock/GTStoneSlabBlock shape): the vanilla {@code TYPE} property is a strict
 * superset of the visible face — the hidden mSlabs[1] (SIDE_UP) is the {@code top} state,
 * the upstream right-click merge (BlockMetaType.java:142-153) is the {@code double} state.
 * The four hidden directional slabs (N/S/W/E) have no vanilla shape and no ported consumer
 * — the GTStoneSlabBlock declared cut, carried verbatim.
 *
 * <p>Numbers: the upstream slab is created with the family multipliers HALVED
 * (BlockMetaType.java:75-80 {@code / 2}) then the same x1.5 / x10 conversion (:108-109) —
 * plain: hardness 0.75 / resistance 10; reinforced: hardness 3.0 / resistance 40. The
 * RENDER face is the shared grayscale PNG + tintindex-0 models over the FIXED dye index
 * (the {@link GT6ConcreteBlock} tint leg).
 *
 * <p>Display name: the slab template over the SAME dye unit — the upstream dump face
 * verbatim ({@code gt.block.concrete.slab.0.<meta>} = "Light Gray Concrete Slab" /
 * 淡灰色混凝土半砖, tmp/gregtech.lang:1872; the BlockColored.java:58 slab LH compose).
 */
public class GT6ConcreteSlabBlock extends SlabBlock {

	/** The slab display template (one dye-unit slot). */
	public static final String SLAB_NAME_KEY = "gt6.concrete.slab";

	/** The reinforced slab's display template. */
	public static final String REINFORCED_SLAB_NAME_KEY = "gt6.concrete.slab_reinforced";

	/** The registry snake id ({@code concrete_light_gray_slab} form). */
	public final String snake;
	/** The FIXED dye index ({@code 0..15}). */
	public final int dyeIndex;
	/** True for the reinforced family. */
	public final boolean reinforced;

	public GT6ConcreteSlabBlock(String aSnake, int aDyeIndex, boolean aReinforced, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.dyeIndex = aDyeIndex;
		this.reinforced = aReinforced;
	}

	/** The plain slab's properties (family numbers halved per BlockMetaType.java:75-80, then :108-109). */
	public static BlockBehaviour.Properties plainProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.75F, 10.0F).sound(SoundType.STONE);
	}

	/** The reinforced slab's properties (BlockConcreteReinforced.java:41 row halved the same way). */
	public static BlockBehaviour.Properties reinforcedProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 40.0F).sound(SoundType.STONE);
	}

	/** The composed display name — the dye unit over the slab template (the GTStoneSlabBlock.getName form). */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(
				this.reinforced ? REINFORCED_SLAB_NAME_KEY : SLAB_NAME_KEY,
				net.minecraft.network.chat.Component.translatable(GT6ConcreteBlock.dyeKey(this.dyeIndex)));
	}
}
