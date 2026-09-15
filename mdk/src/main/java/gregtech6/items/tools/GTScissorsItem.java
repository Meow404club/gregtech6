package gregtech6.items.tools;

import net.minecraft.world.item.ShearsItem;

/**
 * The formal GT6 scissors — item id {@code gt6:scissors} (task p29-w5-t5-scene-six spec
 * ①, single steel tier ruling d). Upstream GT_Tool_Scissors.java:43 rides the
 * {@code TOOL_shears} harvest name (isMinableBlock :104-107) — the modern
 * {@link ShearsItem} base IS that surface, so the whole shear universe arrives by
 * inheritance (the forge patch face, ShearsItem.java.patch):
 * <ul>
 * <li><b>Mining face</b> (:104-107 cloth/web): {@code getDestroySpeed} = cobweb/leaves
 *     15.0, wool 5.0, vine/glow lichen 2.0 (ShearsItem.java:50-58); drop authorization =
 *     cobweb/redstone wire/tripwire (:45-47). The {@code TOOL_scissors} harvest-tool arm
 *     has no GT blocks in this universe yet (W6); TF/EBXL/BoP vines are mod arms (cut).</li>
 * <li><b>Behavior_Shears(20)</b> (:122): the sheep/mooshroom/snow-golem shear rides the
 *     patched {@code interactLivingEntity} (IForgeShearable walk); the upstream 20-unit
 *     cost folds onto ONE vanilla point (the 10000=1 mapping, ceil-to-one ruling). The
 *     patched {@code canPerformAction} = the DEFAULT_SHEARS_ACTIONS set, which also opens
 *     the BeehiveBlock honeycomb face (SHEARS_HARVEST, BeehiveBlock.java.patch:26) and the
 *     silent TripWireBlock disarm (SHEARS_DISARM, TripWireBlock.java.patch:8) for free.</li>
 * <li><b>Behavior_TripwireCutting(100)</b> (:123): the upstream cost never charged here —
 *     the vanilla disarm path pays nothing (the declared mapping deviation; the
 *     100-unit row folds to 0.01 points, the vanilla floor wins).</li>
 * <li><b>Vine self-drop</b> (convertBlockDrops :87-101): rides the
 *     {@code GT6ToolLootModifiers} loot seam, mode {@code PLANT_SELF_DROP}, gated by the
 *     {@code gt6:holds_tool} condition on this item (the t1 consumer contract; the vanilla
 *     match_tool predicate is the bare-shears item identity and cannot see this item).</li>
 * <li><b>CUT</b>: the {@code Behavior_Tool(TOOL_knife, …)} container-craft face (:121) —
 *     no {@code craftingToolKnife} recipe consumes this item in the port (the blade card's
 *     domain); revives with a consumer row.</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream scales per material via
 * {@code U*2+screw+2*ring}, Loader_Tools.java:149 — the ladder is the standing pool cut).
 * The {@code mineBlock} wear exemption for wool/leaves/cobweb/vine/tripwire/... rides the
 * vanilla base verbatim (ShearsItem.java:26-42).
 */
public class GTScissorsItem extends ShearsItem {

	/** The family value (the crowbar/file/saw pinned 512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	public GTScissorsItem(Properties aProperties) {
		super(aProperties);
	}
	// ponytail: no GT6ToolAction classifier yet — the machine-side TOOL_scissors
	// consumers are not ported; the patched vanilla shears set IS the action face.
	// Add a static classifies when the pocket/machine cards need it (the t7 handoff note).
}
