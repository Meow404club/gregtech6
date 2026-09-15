package gregtech6.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
//21.1: same simple names, neoforged package (the loot-subtree lesson — the fluids
//subtree is NOT on the stonecutter swap table; the BLOCK capability lookup returns
//the handler directly, null = absent).
*///?}

/**
 * The formal GT6 plunger — item id {@code gt6:plunger} (task p29-w5-t5-scene-six spec ③,
 * single steel tier ruling d). Upstream GT_Tool_Plunger.java:39 (registration :140
 * "Plunger", tooltip "Not as good at cleaning Pipes as flaming Flowers", mAmount 0):
 * <ul>
 * <li><b>Fluid drain arm</b> (Behavior_Plunger_Fluid.java:48-62 verbatim): a right-click
 *     on any block entity exposing the fluid-handler capability DRAINS 1000 L (voids it —
 *     the upstream {@code drain(…, true)} do-drain), pays one durability point (the
 *     upstream {@code getToolDamagePerDropConversion()} = 100 units through the 10000=1
 *     mapping) and plays the flush sound. The port consumes the platform capability seam
 *     ({@code SideFluidHandler} exposes the GT pipes/tanks/machines through it), so the
 *     pipe domain needs no new seam. The IC2 trampoline sound has no vanilla carrier —
 *     the bucket-empty face stands in (the t3 soft-hammer trampoline ruling).</li>
 * <li><b>Item-clear arm</b> (Behavior_Plunger_Item.java:33-77): CUT — the upstream body
 *     is COMMENTED OUT (:42-64, the whole {@code onItemUseFirst} is dead code returning
 *     F), so there is no behaviour to port; the item-pipe domain card may revive it.</li>
 * <li><b>Thaumcraft essentia arm</b> (:88-91, the reflection try): CUT (the dont list).</li>
 * <li><b>Mining face</b> (isMinableBlock :68-70): the {@code Material.dragonEgg} arm →
 *     {@link Blocks#DRAGON_EGG} (the t1 explicit-set ruling); the TOOL_plunger
 *     harvest-name arm has no carrier in the port (cut).</li>
 * <li><b>Behavior_Tool(TOOL_plunger …)</b> machine face (:85): CUT — no ported machine
 *     answers the plunger tool click yet (the consumers ride the machine cards).</li>
 * </ul>
 *
 * <p>Durability 512 (the family value; upstream material amount 0 with the ×0.25
 * durability multiplier NOT carried — the single-tier pinned value wins, the same ruling
 * as the flint-and-tinder's explicit ×0.25 note except there it folds the number in).
 */
public class GTPlungerItem extends Item {

	/** The family value (the crowbar/file/saw pinned 512; 10000 upstream units = 1 point). */
	public static final int DURABILITY_POINTS = 512;

	/** Behavior_Plunger_Fluid.java:53/:55 — the 1000 L do-drain per click. */
	public static final int DRAIN_MILLIBUCKETS = 1000;

	/** The crowbar MINING_SPEED anchor (the dragon-egg arm rides the same iron-tier scale). */
	public static final float MINING_SPEED = 6.0F;

	public GTPlungerItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The drain arm — resolve the fluid capability on the clicked block entity and void
	 * 1000 L (upstream Behavior_Plunger_Fluid.java:51-58; the empty-capability/empty-drain
	 * arms PASS so the click stays a no-op like the upstream F return).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		//? if forge {
		LazyOptional<IFluidHandler> tCapability =
				tLevel.getCapability(ForgeCapabilities.FLUID_HANDLER_CAPABILITY, tPos, aContext.getClickedFace());
		if (!tCapability.isPresent()) return InteractionResult.PASS;
		IFluidHandler tHandler = tCapability.orElse(null);
		//?} else {
		/*IFluidHandler tHandler =
				tLevel.getCapability(Capabilities.FluidHandler.BLOCK, tPos, aContext.getClickedFace());
		//21.1: the block-capability lookup returns the handler DIRECTLY (null = absent) —
		//the GT6LargeMachines getCapability fork shape, read side.
		if (tHandler == null) return InteractionResult.PASS;
		*///?}
		// var: the FluidStack simple name is NOT on the stonecutter swap table — the
		// inferred local keeps the shared drain line leg-neutral (the t1 loot-subtree lesson).
		var tDrained = tHandler.drain(DRAIN_MILLIBUCKETS, IFluidHandler.FluidAction.EXECUTE);
		if (tDrained == null || tDrained.isEmpty() || tDrained.getAmount() <= 0) return InteractionResult.PASS; // upstream :53 the null-drain guard
		if (!tLevel.isClientSide) {
			tLevel.playSound(null, tPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			Player tPlayer = aContext.getPlayer();
			if (tPlayer != null) {
				aContext.getItemInHand().hurtAndBreak(1, tPlayer, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
			}
		}
		return InteractionResult.sidedSuccess(tLevel.isClientSide);
	}

	/** The dragon-egg arm (isMinableBlock :68-70) — the dig-speed half. */
	@Override
	public float getDestroySpeed(ItemStack aStack, BlockState aState) {
		return aState.is(Blocks.DRAGON_EGG) ? MINING_SPEED : super.getDestroySpeed(aStack, aState);
	}

	/** The dragon-egg arm — the drop-authorization half (the upstream material face mined it bare-handed too). */
	@Override
	//? if forge {
	public boolean isCorrectToolForDrops(BlockState aState) {
	//?} else {
	/*public boolean isCorrectToolForDrops(ItemStack aStack, BlockState aState) {
	//21.1: the stack parameter joined the signature (the GTCrowbarItem fork).
	*///?}
		return aState.is(Blocks.DRAGON_EGG);
	}

	/** The upstream getBaseDamage :42 — 1.25F, kept as the parity read only (the flat-item ruling: no attack attribute pipeline). */
	public static float upstreamBaseDamage() {
		return 1.25F;
	}
}
