package gregtech6.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;

/**
 * The formal GT6 wrench — task tool-hammer-wrench spec ①, the GT6FileItem form.
 * Upstream the tool is a crafting-domain meta id ({@code ToolsGT.WRENCH = 16},
 * CS.java:1736) mounted by the Loader_Tools.java:126 registration row (display name
 * "Wrench", the CS.java:1083 TOOL_LOCALISER row verbatim) over {@code GT_Tool_Wrench}
 * (gregtech/items/tools/machine/, GT_Tool_Wrench.java:46); the oredict crafting key is
 * {@code OreDictToolNames.wrench = "craftingToolWrench"} (CS.java:1876) — the snake
 * translation ruling names the ingredient tag {@code #gt6:tools/wrench} (GT6ItemTags).
 *
 * <p>The crafting-loss face: upstream the in-grid wrench rides the container-item
 * channel (MultiItemTool.getContainerItem :532-540) at
 * {@code getToolDamagePerContainerCraft() = 800} (GT_Tool_Wrench.java:59); the port
 * flattens that channel onto the {@link GT6FileItem#craftRemaining} static seam at the
 * shared one-point mapping (the same declared deviation as the hammer/file/saw trio,
 * decisions.p24-tool-system-damage-mapping), with the id410 has/get PAIRING iron law
 * ({@code hasCraftingRemainingItem} constant {@code true}).
 *
 * <p>Classification: the item performs {@link GT6ToolActions#WRENCH} and nothing else.
 * RED LINE (decisions.p25-tool-hammer-wrench-rulings ②, the WRENCH interaction pool
 * confirmation): ZERO {@code useOn} / world-interaction surface here — the upstream
 * {@code Behavior_Tool(TOOL_wrench, …)} arm (GT_Tool_Wrench.java:95) is the
 * machine-dismantle/rotation domain, a live POOL whose three vanilla-hoe substitute
 * predicates (GTOvenBlock.use:109 / GTFluidPipeBlock.use:104 /
 * GTWrenchHighlightListener:85) stay keyed on {@code ToolActions.HOE_DIG} untouched; a
 * wrench that classified as HOE_DIG would fire the wrench UI everywhere (the crowbar
 * card's regression wall, family-wide). The wrench enters recipes through the
 * {@code #gt6:tools/wrench} tag and nothing else until the interaction card lands.
 *
 * <p>MINING FACE (task wrench-mining-face, ruling A): the tag-driven dig surface —
 * {@link #MINEABLE_WITH_WRENCH} members dig at the ladder speed, everything else at
 * ZERO (the upstream {@code isMinableBlock ? 1 : 0} semantics); the monkey wrench
 * inherits the whole face (the upstream GT_Tool_MonkeyWrench extends GT_Tool_Wrench).
 * Ruling C: {@code requiresCorrectToolForDrops} deliberately NOT set (the punitive
 * no-drop is the defer pool — never requested).
 *
 * <p>MATERIAL LADDER (task machine-ladder): the stack's {@code GT.ToolStats} identity
 * scales durability (the {@link GT6ToolLadder} j/100 points), the composed display name
 * ("Wrench (Bronze)") and the head tint ride the same seam; the IDENTITY-LESS arm
 * reproduces Steel bit-exact (the upstream {@code getPrimaryMaterial(stack, MT.Steel)}
 * read, so the pre-ladder 512 constant IS the steel fallback).
 *
 * <p>Registration form: the file/saw row shape — {@code Item.Properties().durability(
 * DURABILITY_POINTS)}, single steel tier 512 (the pinned family value; upstream scales
 * per material via {@code 4*U}, Loader_Tools.java:126).
 */
public class GTWrenchItem extends Item implements GT6ToolLadder.LadderTool {

	/** The vanilla durability points — single steel tier (the crowbar/file/saw pinned family value). */
	public static final int DURABILITY_POINTS = 512;

	/** The form durability multiplier (upstream ToolStats.java:71 default 1.0). */
	public static final float DURABILITY_MULTIPLIER = 1.0F;

	public GTWrenchItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The dispatch GATE — the has/get pairing iron law (S1 review id410), same shape and
	 * rationale as {@link GT6FileItem#hasCraftingRemainingItem}: without it the vanilla
	 * field test short-circuits the channel and every craft swallows the wrench.
	 */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/**
	 * The container-item channel (upstream MultiItemTool.getContainerItem :532-540, the
	 * GT_Tool_Wrench.java:59 800-unit row) — the file's seam at the shared one-point
	 * mapping, see {@link GT6FileItem#craftRemaining}.
	 */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the file/saw static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.WRENCH == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}

	// ------------------------------ the mining face (task wrench-mining-face) ------------------------------

	/**
	 * The wrench-mineable block tag — the mining surface, the datagen {@code addWrenchBand}
	 * product: the whole GTMachines register + the twelve battery boxes (the reported
	 * symptom family) + the nine vanilla members unfolded from
	 * {@code GT_Tool_Wrench.isMinableBlock} (:72-81 — the piston material ×4, the
	 * redstoneLight lamp, the bars pane, hopper/dispenser/dropper). Machine blocks are a
	 * DOUBLE-tool set (ruling B: mineable/pickaxe kept AND this tag added, no yielding).
	 * Self-owned {@code gt6:} namespace — one name both legs, no ecosystem fork.
	 */
	public static final TagKey<Block> MINEABLE_WITH_WRENCH =
			TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("gt6", "mineable/wrench"));

	/** The form speed multiplier — the wrench carries none (ToolStats.java default 1.0), so the face speed is the material mToolSpeed. */
	public static final float FORM_SPEED_MULTIPLIER = 1.0F;

	/**
	 * The membership seam — the tag check (static so the offline tests pin it without
	 * constructing the item, the crowbar {@code mines()} family form).
	 */
	public static boolean mines(BlockState aState) {
		return aState.is(MINEABLE_WITH_WRENCH);
	}

	/**
	 * The dig-speed seam — the face digs at the ladder speed (MultiItemTool.getDigSpeed
	 * :472-484 = {@code isMinableBlock 1 × getSpeedMultiplier 1.0 × mToolSpeed}, the Steel
	 * anchor 6.0 — the battery box 4.0 hardness drops from the vanilla-divided 6 s to the
	 * upstream ~1 s), everything else at ZERO (ruling A: the upstream
	 * {@code isMinableBlock ? 1 : 0} semantics — the wrench cannot dig stone; NOT the
	 * crowbar's 1.0F hand-speed deviation).
	 */
	public static float destroySpeed(ItemStack aStack, BlockState aState) {
		return mines(aState) ? GT6ToolLadder.speed(FORM_SPEED_MULTIPLIER, GT6ToolLadder.materialOf(aStack)) : 0.0F;
	}

	/** The drop authorization (SwordItem.java:68 shape, the crowbar instance-override form). */
	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the crowbar leg fork); the face is a
	//pure BlockState check here, the identity rides getDestroySpeed only.
	*///?}
		return mines(aState);
	}

	/** The dig speed (SwordItem.java:44 shape). */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return destroySpeed(aStack, aState);
	}

	/**
	 * The per-block-break wear — the upstream 50 units (getToolDamagePerBlockBreak,
	 * GT_Tool_Wrench.java:58) fold into ONE point (the p24 damage-mapping), the
	 * DiggerItem.mineBlock verbatim shape (vanilla DiggerItem.java:50-55).
	 */
	@Override
	public boolean mineBlock(ItemStack aStack, Level aLevel, BlockState aState, BlockPos aPos, LivingEntity aEntity) {
		if (!aLevel.isClientSide && aState.getDestroySpeed(aLevel, aPos) != 0.0F) {
			aStack.hurtAndBreak(1, aEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	// ------------------------------ the GT6ToolLadder identity faces (task machine-ladder) ------------------------------

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — Steel fallback = 512). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The form durability multiplier (ToolStats.java:71 default 1.0). */
	@Override
	public float durabilityMultiplier() {
		return DURABILITY_MULTIPLIER;
	}

	/** The runtime tint (the head pass, the material mRGBaSolid with the steel fallback). */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.tintARGB(aStack, aTintIndex);
	}

	/** The composed display name — "Wrench (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}
}
