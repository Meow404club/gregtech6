package gregtech6.tileentity.attachment;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * 1.20.1 counterpart of gregtech/tileentity/tools/MultiTileEntityFluidFunnel.java
 * (:68-93 onBlockActivated3, task p12-tap-funnel-attachment spec ③) — the wall funnel:
 * right-clicking POURS the held fluid content INTO the container it is mounted on. The
 * chain, translated:
 *
 * <ol>
 * <li>the held item must carry fluid (:70-72 — {@code FL.getFluid(ST.amount(1, aStack), T)},
 *     ONE item's content: the 1.20.1 probe is {@link FluidUtil#getFluidContained});</li>
 * <li>the content is refused for gases and, on a non-acid-proof funnel, acids (:73 —
 *     the same verdict pair the tap runs);</li>
 * <li>the mounted container must be {@link FunnelAccessible} (:74-75);</li>
 * <li>SIMULATE first (:76): {@code funnelFill(side, fluid, doFill=false)} must accept
 *     the WHOLE content, then the executed pass (:77) must land {@code > 0} — on success
 *     the spent container item is consumed and its EMPTY container form given back
 *     (:79-80, the upstream {@code ST.container} — the 1.20.1 idiom is the crafting
 *     remaining item, water bucket → bucket);</li>
 * <li>otherwise, an {@link IFluidHandlerItem} held item with {@code count == 1}
 *     (:83-87) pours by the drain form: the executed funnelFill lands first, the held
 *     container drains by EXACTLY what landed (upstream nests
 *     {@code drain(aStack, funnelFill(side, fluid, T), T)}).</li>
 * </ol>
 *
 * <p>A null player is the RCON acceptance channel ({@code /gt6tank funnel <pos>} runs
 * the chain with a virtual water bucket — the deterministic counterfactual of "a player
 * holding a water bucket clicks"; the returned container is reported instead of given,
 * DECLARED deviation, the card's acceptance (b)).
 */
public class GTFunnelBlockEntity extends GTAttachmentSmallBlockEntity {

	public GTFunnelBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public GTFunnelBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : gregtech6.registry.GTBlockEntities.FUNNEL_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "funnel"; // BET registry path mirrors it (GTBlockEntities.FUNNEL_BE)
	}

	/**
	 * The funnel interface the mounted container implements (upstream
	 * {@code ITileEntityFunnelAccessible} :27-29 minus the capnozzle half — the
	 * CapNozzle chain is the pool card): {@code aDoFill=false} is the probe,
	 * {@code true} the executed pour.
	 */
	public interface FunnelAccessible {
		int funnelFill(byte aSide, FluidStack aFluid, boolean aDoFill);
	}

	// ---------------------------------------------------------------------------
	// the activation chain (upstream onBlockActivated3 :68-93)
	// ---------------------------------------------------------------------------

	/**
	 * The server-side activation, the whole :68-93 chain. {@code aHeld} is the held
	 * stack (the player's main hand, or the virtual stack of the acceptance channel).
	 * Returns a human-readable action report for the RCON channel (the player path
	 * ignores it).
	 */
	@Override
	protected String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld) {
		if (!isServerSide()) return "client side";
		if (aHeld == null || aHeld.isEmpty()) return "nothing held";

		// :72 — ONE item's fluid content
		FluidStack tFluid = probeHeldFluid(aHeld);
		if (tFluid == null || tFluid.getAmount() <= 0) return "the held item carries no fluid";
		// :73 — gases and (non-acid-proof) acids are refused
		if (isGas(tFluid)) return "refused a gas";
		if (!isAcidProof() && isAcid(tFluid)) return "refused an acid (funnel is not acid proof)";

		// :74-75 — the mounted container
		BlockEntity tTarget = adjacent();
		if (!(tTarget instanceof FunnelAccessible tAccessible)) return "no funnel-accessible container on the facing side";
		byte tSide = sideFacingBack(mFacing);

		// :76-81 — the bucket-form pour: the probe must accept it ALL, the executed pass pays
		int tProbed = tAccessible.funnelFill(tSide, tFluid, false);
		if (tProbed >= tFluid.getAmount() && tAccessible.funnelFill(tSide, tFluid, true) > 0) {
			giveEmptyContainer(aPlayer, aHeld);
			return "poured " + tFluid.getAmount() + " L of " + fluidKey(tFluid) + ", empty container returned";
		}

		// :83-87 — the container-handler pour (count == 1): land first, drain the held by what landed
		IFluidHandlerItem tHandler = heldItemHandler(aHeld);
		if (tHandler != null && aHeld.getCount() == 1) {
			int tLanded = tAccessible.funnelFill(tSide, tFluid, true);
			if (tLanded > 0) tHandler.drain(tLanded, FluidAction.EXECUTE);
			return tLanded > 0 ? "drained " + tLanded + " L from the held container" : "the container refused the pour";
		}
		return "the container would not take the whole content";
	}

	/**
	 * The :72 probe seam — overridable so the offline tests can inject the held fluid
	 * without the capability dispatch (the live form is {@link #heldFluid}).
	 */
	protected FluidStack probeHeldFluid(ItemStack aHeld) {
		return heldFluid(aHeld);
	}

	/**
	 * The :72 {@code FL.getFluid(ST.amount(1, aStack), T)} port — ONE item's content.
	 * The 1.20.1 probe is {@link FluidUtil#getFluidContained} (the Forge bucket wrapper
	 * answers a water bucket with 1000 L of water).
	 */
	public static FluidStack heldFluid(ItemStack aHeld) {
		return FluidUtil.getFluidContained(aHeld).orElse(null);
	}

	/**
	 * The :79-80 spent-item half: {@code aStack.stackSize--} + {@code ST.give(aPlayer,
	 * ST.container(aStack, T), T)} — the 1.20.1 empty-container identity is the crafting
	 * remaining item (water bucket → bucket); an item with no container form is consumed
	 * outright (the upstream null-container form), and without a player (the RCON
	 * channel) the empty container is reported instead of given.
	 */
	public static void giveEmptyContainer(@Nullable Player aPlayer, ItemStack aHeld) {
		aHeld.shrink(1); // :79
		ItemStack tEmpty = aHeld.getCraftingRemainingItem(); // :80 ST.container
		if (aPlayer != null && tEmpty != null && !tEmpty.isEmpty()) {
			aPlayer.getInventory().placeItemBackInInventory(tEmpty);
		}
	}
}
