package gregtech6.block.decor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import gregapi.oredict.OreDictMaterial;

/**
 * One GT6 BARS block — the {@code gt.block.bars.*} family (upstream BlockBarsWood through
 * BlockBarsAdamantium, Loader_Blocks.java:106-111, task material-mc-g2-decor-misc). The
 * upstream 4-bit connection meta (Z-=1/Z+=2/X-=4/X+=8, BlockBaseBars.java:196) IS the
 * vanilla pane connection state — the 1.20.1 {@link IronBarsBlock} carries NORTH/EAST/
 * SOUTH/WEST booleans with native placement merging, so the block extends it unchanged.
 *
 * <p>Numbers are the upstream rows verbatim: BlockBaseBars.java:142-143 hardness 5 /
 * resistance 5; the per-family material/sound/explosion overrides — wood: Material.wood,
 * wood sound, resistance 3, flammability 150/150 (BlockBarsWood.java:44-48); brass: 5;
 * steel/titanium/tungstensteel/adamantium share the iron-material 5 default (the four
 * metal rows carry only LH names, BlockBarsSteel.java et al). Harvest tool: wood = axe,
 * metal = pickaxe (BlockBaseBars.java:136), quality = mMat.mToolQuality (the level face
 * stays unexpressed — requiresCorrectToolForDrops unset, the port red line).
 *
 * <p>Render: the material smooth face (BlockBaseBars.java:220 {@code aMat.getTextureSmooth()}
 * = the grayscale blockSolid PNG x mRGBaSolid — METALLIC and WOOD blockSolid.png are
 * byte-identical, sha-verified) tinted per FIXED material via GT6DecorTintListener over
 * the vanilla iron-bars geometry (the datagen-cloned post/cap/side models with tintindex 0).
 *
 * <p>Declared deviations: the onItemUseFirst placement-merge interaction (BlockBaseBars
 * .java:89-133 — clicking an existing bars expands its connection bits) is the vanilla
 * pane auto-connection's superseded face (placing against bars connects natively) and the
 * sub-pixel per-side selection box polish (:164-171); the drops — upstream
 * quantityDropped = FACE_CONNECTION_COUNT (BlockBaseBars.java:140, each connected segment
 * drops a unit) rides the datagen StatePropertiesCondition loot ladder (count per state);
 * the OM/stick composition has no port surface.
 */
public class GT6BarsBlock extends IronBarsBlock {

	/** The display template ({@code gt.block.bars.<mat>.0} dump faces, e.g. "Wood Bars"/木栏杆). */
	public static final String NAME_KEY = "gt6.bars.block";

	/** The registry snake id ({@code bars_wood} form) — the datagen/item source. */
	public final String snake;
	/** The row material (the tint + the smithing economy anchor). */
	public final OreDictMaterial material;
	/** True for the wood row (BlockBarsWood.java:47-48 flammability 150/150). */
	public final boolean flammable;

	public GT6BarsBlock(String aSnake, OreDictMaterial aMaterial, boolean aFlammable, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.material = aMaterial;
		this.flammable = aFlammable;
	}

	/** The metal rows' properties (hardness 5 / resistance 5, iron sound, piston-droppable). */
	public static BlockBehaviour.Properties metalProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 5.0F)
				.sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY);
	}

	/** The wood row's properties (resistance 3, wood sound — BlockBarsWood.java:44). */
	public static BlockBehaviour.Properties woodProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(5.0F, 3.0F)
				.sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY);
	}

	/** The composed display name — the material unit over the family template (the materialFill face). */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(NAME_KEY,
				gregtech6.item.MaterialPrefixItem.materialFill(this.material));
	}

	/** The wood row's flammability (BlockBarsWood.java:47-48, 150/150 — metal rows return 0). */
	@Override
	public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, @Nullable Direction aFace) {
		return this.flammable ? 150 : 0;
	}

	@Override
	public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aFace) {
		return this.flammable ? 150 : 0;
	}
}
