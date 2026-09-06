package gregtech6.block.stone;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;

import gregapi.oredict.OreDictMaterial;

/**
 * One GT6 stone family block: 16 {@link StoneVariant} states behind ONE registered Block
 * (task p19-stoneblocks-registry). Upstream ships one meta-typed BlockStones per stone
 * (gregapi/block/metatype/BlockStones.java, 17 instances at Loader_Rocks.java:56-139) with
 * the 16 variants as block metadata; 1.20.1 has no metadata, so the variant rides an
 * EnumProperty — the P6 oven 16-variant blockstate shape (GTOvenBlock FACING/ACTIVE/RUNNING
 * precedent), giving exactly 16 blockstate variants per stone instead of 272 block classes
 * (the research card's veto against a per-variant split).
 *
 * <p>Numbers are the upstream ctor rows verbatim: vanilla hardness =
 * aHardnessMultiplier * 1.5 and blast resistance = aResistanceMultiplier * 10
 * (BlockMetaType.java:61-62), fed with the Loader_Rocks.java:56-139 columns; the vanilla
 * Material/sound stand-ins are rock/stone (BlockStones.java:92 defaults
 * {@code Material.rock} / {@code soundTypeStone} — no stone row overrides either).
 *
 * <p>Declared working state / cuts (ADR-P8 ④ intermediate-state precedent, this card is
 * W1 of the stone split): blockstate/model/loot JSONs are the render card's surface
 * (p19-stoneblocks-render); the harvest tool/level gating is not wired (the
 * GTMaterialPrefixBlock deviation — the level still rides {@link #harvestLevel} for the
 * later consumer); the wither-proof gate (BlockStonesGT.java:52-55, granite rows
 * witherProof=T) is not wired — the flag rides {@link #witherProof}; the creative tab the
 * upstream meta type joined (BlockMetaType.java:63 {@code CreativeTabs.tabBlock}) is not
 * joined — declared, no new tab and no vanilla-tab wiring this card.
 *
 * <p>The shared property instance (one constant for all 17 blocks — the ADR-P16-2
 * single-instance rule: 1.21.x looks property names up by identity inside StateHolder).
 */
public class GTStoneBlock extends Block {

	/** The single variant property instance every GTStoneBlock shares (ADR-P16-2). */
	public static final EnumProperty<StoneVariant> VARIANT = EnumProperty.create("variant", StoneVariant.class);

	/** The stone's material (e.g. MT.STONES.GraniteBlack) — consumer seam (recipes/tools cards). */
	public final OreDictMaterial material;
	/** The stone's registration snake id (the {@code gt6:<snake>} path and lang segment). */
	public final String stoneSnake;
	/** The upstream mHarvestLevel (Loader_Rocks.java:56-139 column 5) — unwired, data parity. */
	public final int harvestLevel;
	/** The upstream mWitherProof (BlockStonesGT.java:35; granite rows T) — unwired, data parity. */
	public final boolean witherProof;

	public GTStoneBlock(String aStoneSnake, OreDictMaterial aMaterial, float aHardnessMultiplier,
			float aResistanceMultiplier, int aHarvestLevel, boolean aWitherProof) {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(aHardnessMultiplier * 1.5F, aResistanceMultiplier * 10.0F) // BlockMetaType.java:61-62
				.sound(SoundType.STONE)); // BlockStones.java:92 default, no Loader_Rocks row overrides it
		this.stoneSnake = aStoneSnake;
		this.material = aMaterial;
		this.harvestLevel = aHarvestLevel;
		this.witherProof = aWitherProof;
		registerDefaultState(this.stateDefinition.any().setValue(VARIANT, StoneVariant.STONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(VARIANT);
	}

	/**
	 * The variant-0 key of this card's 272-key lang table ({@code block.gt6.<stone>.stone}) —
	 * both the block and its BlockItem (BlockItem.java:187-188 delegates to the block) resolve
	 * their display name through it; the other 15 variant keys are the state-dependent names.
	 * The vanilla default would consult the built-in registry (Block.java:365-369), which has
	 * no meaning offline.
	 */
	@Override
	public String getDescriptionId() {
		return "block.gt6." + this.stoneSnake + "." + StoneVariant.STONE.snake;
	}
}
