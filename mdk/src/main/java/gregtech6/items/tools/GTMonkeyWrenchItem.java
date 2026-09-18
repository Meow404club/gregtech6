package gregtech6.items.tools;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

/**
 * The formal monkey wrench — item id {@code gt6:monkey_wrench} (task
 * p29-w5-t3-machine-face-four spec ②, the {@link GTWrenchItem} subclass, the card's
 * "GT_Tool_Wrench 子类" ruling). Upstream meta id {@code ToolsGT.MONKEY_WRENCH = 52}
 * (CS.java:1736), mounted by the Loader_Tools.java:144 registration row (display name
 * "Monkey Wrench") over {@code GT_Tool_MonkeyWrench extends GT_Tool_Wrench}
 * (machine/GT_Tool_MonkeyWrench.java:33).
 *
 * <p>Classification: the item performs {@link #ACTION} and NOTHING else — the upstream
 * {@code Behavior_Tool(TOOL_monkeywrench, SFX.GT_WRENCH, …)} (:47) machine-dismantle face
 * is the machine-interaction POOL (the P25 RED LINE, decisions.p25-tool-hammer-wrench-rulings
 * ② / the wave ruling c). The {@code GT_WRENCH} click sound is a gregapi-namespace sound
 * with no vanilla counterpart — pooled with the sounds.json card. The upstream oredict
 * double name (OreDictToolNames.monkeywrench + wrench, the :144 row) folds onto the ONE
 * ingredient tag {@code #gt6:tools/monkey_wrench} (the card's tag ruling — no wrench
 * substitution in recipes).
 *
 * <p>The subclass keeps the wrench's crafting-loss face inherited (the has/get pair on
 * {@link GT6FileItem#craftRemaining} — the upstream 800-unit wrench row, the shared
 * one-point mapping) and the material-ladder faces (task p31-machine-ladder, the
 * {@link GT6ToolLadder} seam over the {@code GT.ToolStats} identity — durability,
 * the composed "Monkey Wrench (Bronze)" name, the head tint). Zero {@code useOn} by the
 * RED LINE — the classifies pin is the same reflection wall the hammer/wrench pair carries.
 */
public class GTMonkeyWrenchItem extends GTWrenchItem {

	/** The stack-classification action — "gt6_monkeywrench", the self-owned wave form. */
	public static final ToolAction ACTION = ToolAction.get("gt6_monkeywrench");

	/** The upstream {@code CS.TOOL_monkeywrench} dispatch id ("monkeywrench", CS.java:1039). */
	public static final String ID = "monkeywrench";

	public GTMonkeyWrenchItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The subclass classification — the monkey wrench is its OWN action, never the plain
	 * wrench (the machine-side wrench consumers stay keyed on {@link GT6ToolActions#WRENCH}
	 * untouched) and never {@code ToolActions.HOE_DIG} (the three wrench-substitute
	 * predicates must not see it, the P25 red line).
	 */
	public static boolean classifies(ToolAction aToolAction) {
		return ACTION == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
