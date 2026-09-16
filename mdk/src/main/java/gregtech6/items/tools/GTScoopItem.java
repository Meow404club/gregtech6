package gregtech6.items.tools;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;

/**
 * The formal GT6 scoop — item id {@code gt6:scoop} (task p29-w5-t5-scene-six spec ②,
 * single steel tier ruling d). Upstream GT_Tool_Scoop.java:38 (registration :132 "Scoop",
 * {@code 3*U}); the port ruling per the research ammunition:
 * <ul>
 * <li><b>BeehiveBlock face</b> — the DECLARATIVE modern counterpart: the forge patch keys
 *     the honeycomb harvest on {@code canPerformAction(SHEARS_HARVEST)}
 *     (BeehiveBlock.java.patch:26), so the shears base opens it for free. The upstream
 *     scoop never saw the vanilla hive (1.7.10 had none) — this face is the port's
 *     mapping, not a verbatim arm. One-line complexity budget kept; the GT hive blocks
 *     stay W6.</li>
 * <li><b>Cobweb/vine full-drop</b> — the shears-class harvest rides the
 *     {@code GT6ToolLootModifiers} loot seam, mode {@code PLANT_SELF_DROP}, gated by the
 *     {@code gt6:holds_tool} condition (the vanilla match_tool predicate is the
 *     bare-shears item identity and cannot see this item). The getDestroySpeed faces
 *     (cobweb 15.0 / vine 2.0) ride the vanilla base.</li>
 * <li><b>CUT — {@code MaterialScoopable}</b> (Loader_Others.java:55): the 1.20.1 server
 *     has no block Material system (the t1 explicit-set ruling); the GT hive/hive-block
 *     harvest face waits for W6 with the hive blocks themselves.</li>
 * <li><b>CUT — {@code Behavior_Scoop}</b> (Behavior_Scoop.java:40-59): the Forestry
 *     IEntityButterfly catch arm (the reflection-loaded behaviour, GT_Tool_Scoop
 *     :102-107) — no Forestry in this universe; revives never (the dont list).</li>
 * <li><b>CUT — TOOL_scoop harvest-tool name</b> (isMinableBlock :84-86): no GT block in
 *     the port carries that harvest name yet (W6).</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream {@code 3*U}, the ladder pool cut). The
 * shear-creature face (upstream {@code canCollect} = true) rides the patched
 * {@code interactLivingEntity} base arm verbatim.
 */
public class GTScoopItem extends ShearsItem {

	/** The family value (the crowbar/file/saw pinned 512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	public GTScoopItem(Properties aProperties) {
		super(aProperties);
	}

	//? if neoforge {
	/**
	 * The 1.20.1-forge ShearsItem override the 21.1 base DROPPED (1.20.1 ShearsItem.java:45-47
	 * vs the 21.1 sources: no isCorrectToolForDrops face): cobweb requiresCorrectToolForDrops
	 * = true, so without this the game-mode destroy path skips the loot walk entirely
	 * (canHarvestBlock = false) and the drop-conversion seam never sees the block.
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
		return aState.is(net.minecraft.world.level.block.Blocks.COBWEB)
				|| aState.is(net.minecraft.world.level.block.Blocks.REDSTONE_WIRE)
				|| aState.is(net.minecraft.world.level.block.Blocks.TRIPWIRE)
				|| super.isCorrectToolForDrops(aStack, aState);
	}
	//?}
}
