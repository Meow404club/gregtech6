package gregtech6.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.common.ToolAction;

import javax.annotation.Nullable;

/**
 * The formal soft hammer — item id {@code gt6:soft_hammer} (task p29-w5-t3-machine-face-four
 * spec ①, the GTWrenchItem family form). Upstream the tool is meta id
 * {@code ToolsGT.SOFTHAMMER = 14} (CS.java:1736) mounted by the Loader_Tools.java:125
 * registration row (display name "Soft Hammer", tagline "Can rotate vanilla-ish things and
 * toggle Lamps/Rails") over {@code GT_Tool_SoftHammer} (gregtech/items/tools/machine/).
 *
 * <p>The stat pins (the card's test literals): {@code getSpeedMultiplier() = 0.1F}
 * (GT_Tool_SoftHammer.java:74-76) ported as the flat {@link #getDestroySpeed} — slower
 * than the bare hand, the {@code isMiningTool() = false} semantics; and
 * {@code getMaxDurabilityMultiplier() = 8.0F} (:79-81) — LIVE since the material ladder
 * (task p31-machine-ladder): the form multiplier rides the {@link GT6ToolLadder} j/100
 * budget (Steel 512 ×8 = 4096 points; the pre-ladder flat 512 was the pool-cut shell,
 * a declared behaviour change). The crafting-loss face rides the shared one-point
 * mapping (the upstream :49-51 800-unit row folded, the p25 ruling).
 *
 * <p>The vanilla-ish rotation face (the :125 tagline, upstream
 * gregapi/block/ToolCompat.java:260-297): right-click rotates/cycles the 1.20.1 blocks the
 * card cut — stairs, doors, pillars, curved rails (the {@code state.rotate} family), the
 * straight rails' flat N-S ↔ E-W flip (the upstream :273-284 {@code golden_rail} /
 * {@code activator_rail} {@code (aMeta + 8) % 16} two-flip), fence gates (the
 * {@code HORIZONTAL_FACING} cycle) and the redstone lamp ({@code LIT} cycle, the upstream
 * :261-272 lit/unlit pair toggle). Everything else — pistons, furnaces, chests, hoppers,
 * pumpkins (the upstream :285-297 arms) and the whole GT6 machine side — is the RED LINE
 * external pool (the card's cut ruling: outside the cut list, zero touch). A successful
 * rotation pays ONE durability point (the upstream Behavior_Tool 100-unit click folded to
 * the minimal vanilla point, the p25 one-point mapping family); the click-sound arm
 * (SFX.IC_TRAMPOLINE — an IC2-namespace sound with no vanilla counterpart) stays pooled
 * with the sounds.json card.
 *
 * <p>MATERIAL LADDER (task p31-machine-ladder): the stack's {@code GT.ToolStats} identity
 * scales durability at the form's ×8 multiplier, the composed display name
 * ("Soft Hammer (Bronze)") and the head tint ride the same seam; the IDENTITY-LESS arm
 * is Steel ×8 (the upstream {@code getPrimaryMaterial(stack, MT.Steel)} read over the
 * :79-81 multiplier — the pre-ladder flat 512 shell was the pool-cut value, the
 * declared behaviour change).
 */
public class GTSoftHammerItem extends Item implements GT6ToolLadder.LadderTool {

	/** The vanilla durability points — the pre-ladder shell value (the ladder arm is Steel ×8, see the class javadoc). */
	public static final int DURABILITY_POINTS = 512;

	/** The upstream getSpeedMultiplier literal (GT_Tool_SoftHammer.java:74-76) — the flat dig speed, slower than hand. */
	public static final float SPEED_MULTIPLIER = 0.1F;

	/** The upstream getMaxDurabilityMultiplier literal (:79-81) — LIVE over the {@link GT6ToolLadder} budget. */
	public static final float MAX_DURABILITY_MULTIPLIER = 8.0F;

	/** One durability point per successful rotation (the 100-unit click folded, see class doc). */
	public static final int DAMAGE_PER_ROTATION = 1;

	/** The stack-classification action — "gt6_softhammer", the self-owned action form of this wave (the family home GT6ToolActions stays untouched, the merge-order discipline). */
	public static final ToolAction ACTION = ToolAction.get("gt6_softhammer");

	/** The upstream {@code CS.TOOL_softhammer} dispatch id ("softhammer", CS.java:1063) — the reserved string, the HAMMER_ID shape. */
	public static final String ID = "softhammer";

	public GTSoftHammerItem(Properties aProperties) {
		super(aProperties);
	}

	/** The 0.1x dig face — slower than the bare hand's 1.0 (the upstream multiplier semantics). */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return SPEED_MULTIPLIER;
	}

	/** The dispatch GATE — the has/get pairing iron law (S1 review id410, the GTWrenchItem shape). */
	@Override
	public boolean hasCraftingRemainingItem(ItemStack aStack) {
		return true;
	}

	/** The container-item channel — the shared one-point mapping (the upstream 800-unit row folded). */
	@Override
	public ItemStack getCraftingRemainingItem(ItemStack aStack) {
		return GT6FileItem.craftRemaining(aStack, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The stack-classification face (the GTWrenchItem static-seam shape). */
	public static boolean classifies(ToolAction aToolAction) {
		return ACTION == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}

	// ------------------------------ the GT6ToolLadder identity faces (task p31-machine-ladder) ------------------------------

	/** The per-material durability (the {@link GT6ToolLadder} j/100 points — the identity-less arm is Steel ×8 = 4096). */
	@Override
	public int getMaxDamage(ItemStack aStack) {
		return GT6ToolLadder.durabilityPoints(GT6ToolLadder.statsOf(aStack, durabilityMultiplier()));
	}

	/** The form durability multiplier — the upstream :79-81 ×8 (MultiItemTool.java:182). */
	@Override
	public float durabilityMultiplier() {
		return MAX_DURABILITY_MULTIPLIER;
	}

	/** The runtime tint (the head pass, the material mRGBaSolid with the steel fallback). */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		return GT6ToolLadder.tintARGB(aStack, aTintIndex);
	}

	/** The composed display name — "Soft Hammer (Bronze)"; bare for identity-less stacks. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		return GT6ToolLadder.displayName(aStack, getDescriptionId());
	}

	/**
	 * The vanilla-ish rotation face. Server-authoritative: the state change and the one-point
	 * payment happen server-side only; a no-op (outside the cut list) PASSes untouched.
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		BlockState tBefore = tLevel.getBlockState(tPos);
		BlockState tAfter = rotatedForm(tBefore);
		if (tAfter == tBefore) {
			return InteractionResult.PASS;
		}
		if (!tLevel.isClientSide) {
			tLevel.setBlock(tPos, tAfter, 3);
			ItemStack tStack = aContext.getItemInHand();
			if (tStack.getItem() instanceof GTSoftHammerItem) {
				payRotation(tStack, aContext.getPlayer());
			}
		}
		return InteractionResult.sidedSuccess(tLevel.isClientSide);
	}

	/** The one-point payment (the hurtAndBreak shape; a null player — the acceptance channel — pays nothing). */
	private static void payRotation(ItemStack aStack, @Nullable Player aPlayer) {
		if (aPlayer != null) {
			aStack.hurtAndBreak(DAMAGE_PER_ROTATION, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
	}

	/**
	 * The cut list — the card's 1.20.1 vanilla 对位 ruling, ONE dispatch, the same state in →
	 * the same state out when the block is outside the list (the static seam the offline test
	 * pins and the {@code /gt6machineface rotate} arm drives).
	 */
	public static BlockState rotatedForm(BlockState aState) {
		Block tBlock = aState.getBlock();
		// the state.rotate family — stairs (facing+shape), doors (facing), curved rails
		// (the RailShape rotation incl. the curve forms)
		if (tBlock instanceof StairBlock || tBlock instanceof DoorBlock || tBlock instanceof RailBlock) {
			return aState.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
		}
		// pillars — the vanilla CLOCKWISE_90 rotate is a NO-OP on the Y axis, while the
		// upstream BlockRotatedPillar arm (ToolCompat.java:285-287, (aMeta+4)&15) TOGGLES
		// the axis: the AXIS cycle is the faithful 1.20.1-reachable form
		if (tBlock instanceof RotatedPillarBlock) {
			return aState.cycle(BlockStateProperties.AXIS);
		}
		// the straight rails — powered + activator are BOTH PoweredRailBlock instances
		// (Blocks.java:708/:3115): the upstream flat N-S ↔ E-W two-flip, ascending forms stay
		if (tBlock instanceof PoweredRailBlock) {
			RailShape tShape = aState.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT);
			if (tShape == RailShape.NORTH_SOUTH) return aState.setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT, RailShape.EAST_WEST);
			if (tShape == RailShape.EAST_WEST) return aState.setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT, RailShape.NORTH_SOUTH);
			return aState;
		}
		// fence gates carry no rotate override — the facing cycle is the 1.20.1-reachable form
		if (tBlock instanceof FenceGateBlock) {
			return aState.cycle(BlockStateProperties.HORIZONTAL_FACING);
		}
		// the lamp — the upstream lit/unlit pair toggle flattened onto the LIT cycle
		if (tBlock instanceof RedstoneLampBlock) {
			return aState.cycle(RedstoneLampBlock.LIT);
		}
		return aState;
	}
}
