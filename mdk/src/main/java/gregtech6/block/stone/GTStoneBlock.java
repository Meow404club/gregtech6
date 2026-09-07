package gregtech6.block.stone;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import gregapi.oredict.OreDictMaterial;

/**
 * One GT6 stone VARIANT block: a degenerate pure {@link Block} with a FIXED
 * {@link StoneVariant} (task p21-stoneblocks-16item-registry-split) — one block per
 * (stone, variant) pair, 272 registrations over the 17 CS.java:1668 stones. This retires
 * the P19 intermediate shape (ONE Block per stone carrying the 16 variants behind the
 * {@code GTStoneBlock.VARIANT} EnumProperty): 1.20.1 has no metadata, and the upstream
 * per-meta item ids (the {@code ST.make(this, 1, aMeta)} universe of BlockStones — the
 * :731 getDrops swap, the GT_Tool_Chisel.java:73-77 CHISEL_MAPPINGS drop conversion) are
 * only faithful with one item id per variant — the P8 ADR ④ per-pair Block+BlockItem
 * precedent (GTMaterialBlocks/GTMaterialPrefixBlock), now applied here.
 *
 * <p>Numbers are the upstream ctor rows verbatim: vanilla hardness =
 * aHardnessMultiplier * 1.5 and blast resistance = aResistanceMultiplier * 10
 * (BlockMetaType.java:61-62), fed with the Loader_Rocks.java:56-139 columns; the vanilla
 * Material/sound stand-ins are rock/stone (BlockStones.java:92 defaults
 * {@code Material.rock} / {@code soundTypeStone} — no stone row overrides either).
 *
 * <p>Cuts carried over from the P19 card, unchanged in shape: the harvest tool/level
 * gating is not wired (the level still rides {@link #harvestLevel} for the later
 * consumer); the wither-proof gate (BlockStonesGT.java:52-55, granite rows witherProof=T)
 * is not wired — the flag rides {@link #witherProof}; the creative tab the upstream meta
 * type joined (BlockMetaType.java:63 {@code CreativeTabs.tabBlock}) is not joined —
 * declared, no tab (task p21 keeps the P19 no-tab status quo, the tab ruling stays
 * pooled).
 *
 * <p>Declared deviation (the P19 ledger continues): the retired EnumProperty means the
 * P19-era {@code variant=<snake>} blockstate property is GONE — already-placed blocks
 * from P19-era worlds lose their placed variant on migration (the variant-0 id keeps the
 * plain-stone look, the 15 new per-variant ids start empty). Project pre-release accepts
 * this — the 2026-09-07 ADR draft records the declaration.
 */
public class GTStoneBlock extends Block {

	/** The stone's material (e.g. MT.STONES.GraniteBlack) — consumer seam (recipes/tools cards). */
	public final OreDictMaterial material;
	/** The stone's registration snake id (the {@code gt6:<snake>} id prefix and lang segment). */
	public final String stoneSnake;
	/** The block's FIXED variant — the per-pair split's identity (was the P19 EnumProperty value). */
	public final StoneVariant variant;
	/** The upstream mHarvestLevel (Loader_Rocks.java:56-139 column 5) — unwired, data parity. */
	public final int harvestLevel;
	/** The upstream mWitherProof (BlockStonesGT.java:35; granite rows T) — unwired, data parity. */
	public final boolean witherProof;

	public GTStoneBlock(String aStoneSnake, StoneVariant aVariant, OreDictMaterial aMaterial,
			float aHardnessMultiplier, float aResistanceMultiplier, int aHarvestLevel, boolean aWitherProof) {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(aHardnessMultiplier * 1.5F, aResistanceMultiplier * 10.0F) // BlockMetaType.java:61-62
				.sound(SoundType.STONE)); // BlockStones.java:92 default, no Loader_Rocks row overrides it
		this.stoneSnake = aStoneSnake;
		this.variant = aVariant;
		this.material = aMaterial;
		this.harvestLevel = aHarvestLevel;
		this.witherProof = aWitherProof;
	}

	/**
	 * The composed display name (task p20-i18n-compose-rows, the B-wave lang ruling): the
	 * block composes ITS OWN variant's {@code gt6.stone.variant.<snake>} template (ONE
	 * position-param template per variant, unchanged by the p21 split — variant-0 blocks
	 * compose the exact same template the P19 single-block form composed) with the
	 * {@code gt6.material.<snake>} small-unit slot, so a zh client renders the dump's stone
	 * word for free (the A-wave material face). ZERO new lang keys: the 16 templates and
	 * the material units both predate this card (the GT6LangParityTest pins hold). The
	 * vanilla descriptionId default ({@code block.gt6.<path>}) is deliberately NOT backed
	 * by a lang key — every name path routes through this override (the GTWireBlock
	 * posture; the raw-key fallback only shows for exotic consumers of getDescriptionId).
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(this.variant.key(),
				net.minecraft.network.chat.Component.translatable(
						"gt6.material." + gregtech6.item.MaterialPrefixItem.snakeCase(this.material.mNameInternal)));
	}
}
