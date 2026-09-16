package gregtech6.items.tools;

import com.google.common.collect.ImmutableSet;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The formal GT6 construction pickaxe — item id {@code gt6:pickaxe_construction} (task
 * p29-w5-t1-dig-six). Upstream GT_Tool_PickaxeConstruction.java:39-65 extends
 * GT_Tool_Pickaxe with three arms:
 * <ul>
 * <li><b>Speed ×2</b> (:41-43) → {@link #MINING_SPEED} = 12.0F (the pickaxe 6.0F anchor
 *     doubled — "Good for Brucks and alike", Loader_Tools.java:147).</li>
 * <li><b>Ore-stone penalty</b> (:62-65 {@code WD.ore_stone → default / 4}) →
 *     {@link #destroySpeedBonus} quarters the speed on {@link #ORE_STONE} (the vanilla
 *     ore set; the GT ore blocks of the W6 worldgen join the set when they land).</li>
 * <li><b>Ender-chest self-drop</b> (:53-59) → the loot seam
 *     ({@code GT6ToolLootModifiers} mode {@code ENDER_CHEST_SELF}, the per-tool GLM
 *     JSON) — NOT in this class: drop conversions ride the loot seam only (the card
 *     RED LINE).</li>
 * <li>{@code canCollect} (:45) → CUT: the auto-pickup arm rides the upstream entity-drop
 *     pipeline this port does not carry (the crowbar canBlock cut precedent).</li>
 * </ul>
 *
 * <p>Mining surface, torch arm, attack damage 3.0F, classifier (the gt6_pickaxe action —
 * upstream registers it as a {@code TOOL_pickaxe} variant, Loader_Tools.java:147): all
 * inherited from {@link GTPickaxeItem} verbatim.
 */
public class GTPickaxeConstructionItem extends GTPickaxeItem {

	/** The family value — the construction pick takes NO durability multiplier upstream. */
	public static final int DURABILITY_POINTS = GTPickaxeItem.DURABILITY_POINTS;

	/** Upstream getSpeedMultiplier :41-43 — the pickaxe anchor doubled (6.0 × 2). */
	public static final float MINING_SPEED = GTPickaxeItem.MINING_SPEED * 2.0F;

	/**
	 * The upstream {@code WD.ore_stone} test, vanilla members (GT_Tool_PickaxeConstruction
	 * .java:62-65; the 1.20.1 ore universe minus the deepslate-raw pairs that never existed
	 * in 1.7.10 — all 19 vanilla ore blocks, deepslate variants included). GT6 ore blocks
	 * (the W6 worldgen domain) join this set explicitly when they land.
	 */
	static final ImmutableSet<Block> ORE_STONE = ImmutableSet.of(
			Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
			Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
			Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
			Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS);

	public GTPickaxeConstructionItem(Properties aProperties) {
		super(aProperties);
	}

	/** Upstream :62-65 — the ore-stone quarter speed, the ×2 surface speed elsewhere. */
	public static float destroySpeedBonus(BlockState aState) {
		if (ORE_STONE.contains(aState.getBlock())) return MINING_SPEED / 4.0F; // aDefault / 4
		return GTPickaxeItem.mines(aState) ? MINING_SPEED : 1.0F;
	}

	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeedBonus(aState);
	}

	// The drop-authorization + mining surface stay the pickaxe's (upstream isMinableBlock
	// is inherited, not overridden); the ender-chest arm lives in the loot seam.
}
