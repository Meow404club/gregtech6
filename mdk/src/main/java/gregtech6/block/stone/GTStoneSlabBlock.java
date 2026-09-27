package gregtech6.block.stone;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import gregapi.oredict.OreDictMaterial;

/**
 * One GT6 stone VARIANT SLAB — the {@code mSlabs[0]} face of the upstream stone universe
 * (task debt-slab-gap): every {@link BlockMetaType} family auto-creates six directional
 * half-blocks (BlockMetaType.java:55 {@code mSlabs}, filled :74-81 over the six sides),
 * of which only the SIDE_DOWN entry is player-visible (:83 hides the other five —
 * {@code mIsPrimary} rides {@code aSlabType == 0}, :113) and craftable
 * (BlockStones.java:269/:327 reference {@code mSlabs[0]}). The port's shape is the
 * {@code spec_rulings.ruling_slab} vanilla {@link SlabBlock} ruling (the
 * GT6CFoamSlabBlock precedent): ONE slab block per (stone, variant) pair — 272
 * registrations over the same {@link GTStoneBlocks#registrationOrder()} walk — whose
 * vanilla {@code TYPE} property (BOTTOM/TOP/DOUBLE) is a strict superset of the upstream
 * player-visible surface: the hidden mSlabs[1] (SIDE_UP) is the vanilla {@code top}
 * state, and the upstream right-click merge into the FULL block (:142-153) is the
 * vanilla {@code double} state.
 *
 * <p>Numbers are the upstream slab ctor rows verbatim: the slab is created with the
 * family's multipliers HALVED (BlockMetaType.java:75-80 {@code aResistanceMultiplier / 2,
 * aHardnessMultiplier / 2}), then the same {@code * 1.5} / {@code * 10} conversion the
 * full block applies (:61-62, :108-109) — so hardness = aHardnessMultiplier * 0.75 and
 * resistance = aResistanceMultiplier * 5. Drops: one slab always (:178
 * {@code getItemDropped} returns {@code mBlock.mSlabs[0]}); the vanilla
 * {@code createSlabItemTable} loot idiom the datagen lands doubles that ONLY for the
 * {@code double} state (two slabs' worth of material — the upstream double is the full
 * block, a different block id upstream, same economy).
 *
 * <p>Cuts carried over from the stone family verbatim (the GTStoneBlock ledger): no
 * harvest tool/level gating, no wither-proof gate, no creative tab (upstream joined
 * {@code CreativeTabs.tabBlock}, :63 — the p21 no-tab status quo). The four HIDDEN
 * directional slabs (N/S/W/E, mSlabs[2..5]) have NO vanilla shape and no ported
 * consumer (upstream uses them for foam/glass worldgen, not stones) — declared cut.
 */
public class GTStoneSlabBlock extends SlabBlock {

	/** The slab display template (one position slot: the composed variant name) — the GT6EnUs/GT6ZhCn band. */
	public static final String SLAB_NAME_KEY = "gt6.stone.slab";

	/** The stone's material (e.g. MT.STONES.GraniteBlack) — consumer seam. */
	public final OreDictMaterial material;
	/** The stone's registration snake id (the {@code gt6:<snake>..._slab} id prefix). */
	public final String stoneSnake;
	/** The slab's FIXED variant — the per-pair split's identity. */
	public final StoneVariant variant;

	public GTStoneSlabBlock(String aStoneSnake, StoneVariant aVariant, OreDictMaterial aMaterial,
			float aHardnessMultiplier, float aResistanceMultiplier) {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(aHardnessMultiplier * 1.5F / 2.0F, aResistanceMultiplier * 10.0F / 2.0F) // BlockMetaType.java:75-80 halved, then :61-62
				.sound(SoundType.STONE)); // BlockStones.java:92 default, no Loader_Rocks row overrides it
		this.stoneSnake = aStoneSnake;
		this.variant = aVariant;
		this.material = aMaterial;
	}

	/**
	 * The composed display name (the p20 template family): the ONE slab template
	 * {@code gt6.stone.slab} over the SAME composed variant name the full block shows
	 * (the variant template over the {@code gt6.material.<snake>} small unit) — upstream
	 * wording is the dump's {@code gt.stone.<stone>.slab.0.<meta>} family
	 * (tmp/gregtech.lang:15977-15982: 花岗岩半砖 / 花岗岩圆石半砖 = the variant name + 半砖).
	 */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(SLAB_NAME_KEY,
				net.minecraft.network.chat.Component.translatable(this.variant.key(),
						net.minecraft.network.chat.Component.translatable(
								"gt6.material." + gregtech6.item.MaterialPrefixItem.snakeCase(this.material.mNameInternal))));
	}
}
