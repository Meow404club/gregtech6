package gregtech6.block.decor;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * One GT6 GLASS / GLOW GLASS block — the {@code gt.block.glass} and
 * {@code gt.block.glass.glow} families (upstream BlockGlassClear/BlockGlassGlow,
 * Loader_Blocks.java:72-73, task material-mc-g2-decor-misc). The BlockColored 16-meta
 * dye ladders split into per-dye registrations (the GT6ConcreteBlock precedent — the
 * Mixer glass economy pours per-colour stacks, Loader_Recipes_Other.java:596-599; the
 * Injector glow walk :602-605 unblocks on this landing — its pour rows are the shipped
 * injector.json data face (the recipe-data card's domain, the B6 header keeps the
 * declaration until that card enumerates)).
 *
 * <p>Numbers are the upstream ctor rows verbatim (BlockGlassClear.java:288 /
 * BlockGlassGlow.java:370 x BlockMetaType.java:61-62): hardness 0.5 x 1.5 = 0.75,
 * resistance 0.5 x 10 = 5, glass sound, NO harvest tool (hand-breakable glass family,
 * instant via hardness alone — the vanilla glass posture; no mineable band), light
 * opacity NONE, non-opaque translucent render (the shared GLASS_CLEAR grayscale PNG is
 * alpha-48 translucent — byte-verified — so the vanilla {@code translucent} render type;
 * the concrete tint chain resolves the FIXED dye index via GT6DecorTintListener).
 * The GLOW arm carries the upstream {@code setLightLevel(1.0F)} = the 1.20.1 lightLevel(15).
 *
 * <p>Declared deviations: the drops — upstream {@code getDrops} emits 40/80 scrapGt
 * Glass (BlockGlassClear.java:310); the port has no scrap item surface (P8 ADR ⑥), so
 * the block drops NOTHING (the vanilla-glass posture, the scrap economy rides the
 * item-form card). The {@code BlocksGT.breakableGlass} breakable-by-tool registry, the
 * isSealable face, the sibling-glass culling {@code shouldSideBeRendered} arm and the
 * OM.data Glass/Glowstone composition rows have no port surface; the mSlabs[0] slab face
 * (and its Injector slab legs) ride the slab-sweep defer.
 */
public class GT6GlassBlock extends Block {

	/** The plain family's display template (one dye-unit slot). */
	public static final String GLASS_NAME_KEY = "gt6.glass.block";
	/** The glow family's display template (one dye-unit slot). */
	public static final String GLOW_NAME_KEY = "gt6.glass.glow_block";

	/** The dye small-unit key of an id ({@code gt6.dye.light_gray} form) — the concrete source. */
	public static String dyeKey(int aDyeIndex) {
		return "gt6.dye." + gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}

	/** The FIXED dye index ({@code 0..15}, the CS.DYE_INDEX order 0=Black..15=White). */
	public final int dyeIndex;
	/** True for the glow family ({@code gt.block.glass.glow}, lightLevel 15). */
	public final boolean glow;
	/** The registry snake id ({@code glass_light_gray} / {@code glow_glass_light_gray}). */
	public final String snake;

	public GT6GlassBlock(String aSnake, int aDyeIndex, boolean aGlow, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.dyeIndex = aDyeIndex;
		this.glow = aGlow;
	}

	/** The plain family's properties (hardness 0.75 / resistance 5, no light). */
	public static BlockBehaviour.Properties plainProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.75F, 5.0F)
				.sound(SoundType.GLASS).noOcclusion()
				.pushReaction(PushReaction.DESTROY);
	}

	/** The glow family's properties — the upstream {@code setLightLevel(1.0F)} face. */
	public static BlockBehaviour.Properties glowProperties() {
		return plainProperties().lightLevel(aState -> 15);
	}

	/** The composed display name — the dye unit over the family template. */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(this.glow ? GLOW_NAME_KEY : GLASS_NAME_KEY,
				net.minecraft.network.chat.Component.translatable(dyeKey(this.dyeIndex)));
	}
}
