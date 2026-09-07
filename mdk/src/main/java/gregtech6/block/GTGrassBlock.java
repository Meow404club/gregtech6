package gregtech6.block;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * One GT6 grass VARIANT block (task p24-grass-block): the 1.20.1 counterpart of upstream
 * {@code BlockGrass extends BlockBaseMeta, maxMeta = 6} (BlockGrass.java:46-48) as ONE
 * class instantiated 6x per-pair (the P21 GTStoneBlock degenerate-pure-block precedent;
 * the EnumProperty form is ruled OUT by decisions.p24-grass-behavior-trim ④ — the dye
 * recipes and the spray-can targets need per-variant ITEM identities).
 *
 * <p><b>The behaviour face is deliberately EMPTY of overrides</b> — every upstream trait
 * is a cut against the vanilla GrassBlock feature set, and each cut is the DEFAULT face
 * here (upstream evidence, state research.p24-r-grass-block q2):
 * <ul>
 * <li><b>no spread, no death</b>: upstream has NO updateTick2/randomTick override at all
 *     (BlockGrass.java:46-113, BlockBase.java:123 empty stub + the :127-130 final
 *     delegate) and the tooltip says so ("Does not spread, get eaten, change color nor
 *     need light", :86). This class extends {@link Block} DIRECTLY — the vanilla spread
 *     tick lives in {@code SpreadingSnowyDirtBlock#randomTick}, which is therefore not
 *     inherited (decisions ④); {@code randomTicks()} is NOT set on the properties
 *     (vanilla Blocks.java:89 grass row sets it — the deliberate omission is the cut).</li>
 * <li><b>not flammable</b>: upstream getFlammability 0 (BlockBase.java:112-117
 *     unoverridden); the modern default fire face is the same zero.</li>
 * <li><b>zero tint</b>: upstream bakes the colour into the PNGs (GRASSES_TOP/SIDE
 *     pre-coloured, Textures.java:530-565); the models carry NO tintindex (the
 *     GT6BlockStates grass models; a GrassBlock-style biome tint is forbidden).</li>
 * <li><b>sheep never eat it</b>: EatBlockGoal.java:33/:71 hardcodes {@code Blocks.GRASS}
 *     — the GT blocks are simply not that identity (decisions ⑤, automatic parity).</li>
 * <li><b>no creature spawning</b>: upstream canCreatureSpawn = F (BlockGrass.java:107);
 *     the 1.20.1 animal spawn surface is tag-driven (Animal.java:109), so the six
 *     {@code *_spawnable_on} tags stay UNSET on these blocks — the tag-absence
 *     equivalence (decisions ①); hostile-mob suppression is the declared deviation
 *     (no 1.20.1 block-level surface). valid_spawn is likewise not joined (decision ②).</li>
 * <li><b>canSustainPlant = the DEFAULT face, declared deviation</b>: upstream returns
 *     Plains-type plants always-true and Beach-type only with adjacent water
 *     (BlockGrass.java:97-100). The direct translation is NOT portable: NeoForge 21.1
 *     REMOVED {@code IPlantable}/{@code PlantType} (the 21.1.249 universal jar carries
 *     neither class) and reshaped the hook to {@code TriState canSustainPlant(...,
 *     BlockState plant)} (IBlockExtension, javap-verified), so the plant-type switch has
 *     no 21.1 surface while the 1.20.1 face is {@code IForgeBlock.canSustainPlant(...,
 *     IPlantable)} — a per-leg fork for a half-reachable semantics. Ruled by the task
 *     card's blocked-arm: NO override; the block joins {@code #minecraft:dirt}, and both
 *     legs' default face (the forge 1.20.1 Block.java patch body:
 *     {@code state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND)}) sustains every
 *     plant type UNCONDITIONALLY — the Plains arm holds, the Beach-adjacent-water gate
 *     is the declared cut.</li>
 * <li><b>numbers are the vanilla grass registration values</b> (upstream copies them at
 *     runtime, BlockGrass.java:111-112; the modern face pins the vanilla Blocks.java:89
 *     constants): hardness 0.6 = blast resistance 0.6 ({@code strength(0.6F)}),
 *     {@link MapColor#GRASS}, {@link SoundType#GRASS}. Shovel-mined (upstream
 *     TOOL_shovel level 0, :109-110) — wired through the {@code mineable/shovel} TAG,
 *     not block code (the user tag-paradigm ruling).</li>
 * <li><b>loot is datagen-only</b> (dirt without fortune, silk touch self-drop, the
 *     upstream :105 getDrops shape): GT6LootTables.GT6GrassBlockLoot; the GT spade
 *     self-drop arm stays in the tool-system pool (GT_Tool_Spade.java:81-89).</li>
 * </ul>
 *
 * <p>The two tooltips ride the upstream LH keys verbatim (BlockGrass.java:86-87 static
 * block + the :92-93 CYAN/GRAY faces); the lang values are the GT6EnUs/GT6ZhCn rows.
 */
public class GTGrassBlock extends Block {

	/** The upstream tooltip key verbatim (BlockGrass.java:86). */
	public static final String TOOLTIP_KEY = "gt.grass.tooltip";

	/** The upstream spray tooltip key verbatim (BlockGrass.java:87). */
	public static final String TOOLTIP_SPRAY_KEY = "gt.grass.tooltip.spray";

	public GTGrassBlock() {
		// the vanilla grass registration row (Blocks.java:89) MINUS randomTicks() — the
		// no-spread cut; strength(0.6F) = hardness AND blast resistance 0.6 (upstream
		// copies both from the vanilla block at runtime, BlockGrass.java:111-112)
		super(grassProperties());
	}

	/**
	 * The registration properties, as the package-private seam the offline tests read
	 * (the block ITSELF is unconstructible in the frozen-registry test JVM — the mod-Item
	 * wall's block face, CrowbarTest.bootStrap NOTE — but a {@code Properties} object
	 * carries no registry state). The pinned numbers: the vanilla grass row
	 * (Blocks.java:89) MINUS {@code randomTicks()}. Named {@code grassProperties} — 21.1's
	 * {@code BlockBehaviour.properties()} accessor clashes with a bare {@code properties}.
	 */
	static BlockBehaviour.Properties grassProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.GRASS)
				.strength(0.6F)
				.sound(SoundType.GRASS);
	}

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable net.minecraft.world.level.BlockGetter aLevel,
			List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aTooltip);
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext,
			List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the Block hover signature carries the Item.TooltipContext (1.21.1 Block.class,
		// javap-verified) — the GTSprayCanItem appendHoverText fork shape
		tooltipLines(aTooltip);
	}
	*///?}

	/** The shared tooltip body — the upstream :92-93 order and chat colours verbatim (LH.Chat.CYAN = §b = the modern AQUA; GRAY = §7). */
	private static void tooltipLines(List<Component> aTooltip) {
		aTooltip.add(Component.translatable(TOOLTIP_KEY).withStyle(ChatFormatting.AQUA));
		aTooltip.add(Component.translatable(TOOLTIP_SPRAY_KEY).withStyle(ChatFormatting.GRAY));
	}
}
