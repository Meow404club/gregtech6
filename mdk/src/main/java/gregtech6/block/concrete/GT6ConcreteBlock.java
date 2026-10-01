package gregtech6.block.concrete;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 CONCRETE block — the {@code gt.block.concrete} family (upstream
 * {@code BlockConcrete}, Loader_Blocks.java:63), the {@code BlockColored} 16-meta dye
 * ladder split into per-pair registrations (the P8 ADR ④ / P24 grass ruling: the dye
 * Bath economy pours per-colour OUTPUT stacks — Loader_Recipes_Other.java:452/:457 walk
 * {@code ST.make(block, 1, i)} for every colour incl. the slabs — which an EnumProperty
 * single block cannot express).
 *
 * <p>Numbers are the upstream ctor row verbatim (BlockConcrete.java:51): Material.rock /
 * stone sound, hardnessMultiplier 1.0 x 1.5 = 1.5 and resistanceMultiplier 2.0 x 10 = 20
 * (BlockMetaType.java:61-62), harvest level 1 (pickaxe — the tag band rides
 * {@code mineable/pickaxe}; {@code requiresCorrectToolForDrops} stays unset, the
 * stone-family red line). The RENDER face is the tint leg: upstream ships ONE grayscale
 * texture (Textures.BlockIcons.CONCRETES = {@code UT.Code.fill(CONCRETE, ..)},
 * Textures.java:704 — the same PNG for all 16 metas) coloured by
 * {@code DYES_INT[meta]} (BlockColored.java:63-73); the port keeps exactly that — the
 * shared {@code gt6:block/concrete} PNG + tintindex-0 models + the
 * {@code GT6ConcreteTintListener} BlockColor over this block's FIXED dye index. Declared
 * deviations: the upstream {@code MT.Concrete.mTextureSolid} icon copy (:52) is render
 * plumbing the datagen replaces; the drill-into-reinforced tool conversion
 * (BlockConcrete.java:67-83) and the spray recolour routing (BlockColored.recolourBlock)
 * are tool-card behaviour, deferred; the OM.data composition rows (:53) have no port
 * ItemStack->oredict surface (the P8 ADR ⑥ / GTStoneBlocks.oreDictMappings posture).
 *
 * <p>Display name: the composed template ({@link #BLOCK_NAME_KEY}) over the
 * {@code gt6.dye.<id>} small unit — the upstream dump face verbatim
 * ({@code gt.block.concrete.<meta>} = "Light Gray Concrete" / 淡灰色混凝土,
 * tmp/gregtech.lang:1736; the BlockColored.java:46 LH compose).
 */
public class GT6ConcreteBlock extends Block {

	/** The full-block display template (one dye-unit slot) — the GT6EnUs/GT6ZhCn band. */
	public static final String BLOCK_NAME_KEY = "gt6.concrete.block";

	/** The reinforced variant's display template. */
	public static final String REINFORCED_NAME_KEY = "gt6.concrete.block_reinforced";

	/** The dye small-unit key of an id ({@code gt6.dye.light_gray} form). */
	public static String dyeKey(int aDyeIndex) {
		return "gt6.dye." + gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}

	/** The registry snake id ({@code concrete_light_gray} form) — the GT6ConcreteBlocks.path source. */
	public final String snake;
	/** The FIXED dye index ({@code 0..15}, the CS.DYE_INDEX order 0=Black..15=White). */
	public final int dyeIndex;
	/** True for the reinforced family ({@code gt.block.concrete.reinforced}). */
	public final boolean reinforced;

	public GT6ConcreteBlock(String aSnake, int aDyeIndex, boolean aReinforced, BlockBehaviour.Properties aProperties) {
		super(aProperties);
		this.snake = aSnake;
		this.dyeIndex = aDyeIndex;
		this.reinforced = aReinforced;
	}

	/** The plain family's properties (hardness 1.5 / resistance 20, the BlockConcrete.java:51 row). */
	public static BlockBehaviour.Properties plainProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 20.0F).sound(SoundType.STONE);
	}

	/** The reinforced family's properties (hardness 6.0 / resistance 80, BlockConcreteReinforced.java:41). */
	public static BlockBehaviour.Properties reinforcedProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0F, 80.0F).sound(SoundType.STONE);
	}

	/** The composed display name — the dye unit over the family template (the GTStoneSlabBlock.getName form). */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(this.reinforced ? REINFORCED_NAME_KEY : BLOCK_NAME_KEY,
				net.minecraft.network.chat.Component.translatable(dyeKey(this.dyeIndex)));
	}
}
