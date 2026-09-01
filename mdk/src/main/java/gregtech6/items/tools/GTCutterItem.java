package gregtech6.items.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolAction;

import gregtech6.covers.ICoverableTE;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.util.UT6;

/**
 * The formal GT6 wire cutter — task p10-tool-cutter spec ②. Upstream the tool mounts
 * {@code Behavior_Tool(TOOL_cutter, …)} (GT_Tool_WireCutter.java:101) and the wire MTEs
 * answer it through {@code getFacingTool() == TOOL_cutter}
 * (MultiTileEntityWireElectric.java:245) on the {@code onToolClick2} chain
 * (TileEntityBase09Connector.java:70-79). The 1.20.1 port flattens the pre-use hook to
 * a {@link #useOn} direct dispatch with two arms:
 * <ol>
 * <li><b>the wire toggle</b> — the target BE is a {@link GTWireBlockEntity}: resolve the
 *     nine-grid target side ({@link #targetSide}, the upstream :73
 *     {@code UT.Code.getSideWrenching} — the already-ported {@link UT6#getSideWrenching}),
 *     then {@code connected ? disconnect : connect} (:76), the ported base handshake
 *     (TileEntityBase09Connector.java:105/:129 — the toggle calls ONLY the existing
 *     connect/disconnect API, the BE files stay zero-diff). The {@code getFacingTool}
 *     gate (:72) is statically true here: every wire in this port answers the cutter.</li>
 * <li><b>the cover relay</b> — the target BE is an {@link ICoverableTE}:
 *     {@code onCoverToolClick("cutter", …)} travels the :275-276 relay into
 *     {@code ICover.onToolClick} — the live consumer is the redstone emitter's
 *     strong-gate toggle (CoverRedstoneEmitter.onToolClick, returning 1000).</li>
 * </ol>
 *
 * <p>Declared port-isms and cuts (card spec ②/⑥):
 * <ul>
 * <li>the upstream {@code allowInteraction} gate on the clicked neighbour (:75) is CUT —
 *     that is the upstream ownership domain, and the ported BEs carry no
 *     {@code allowInteraction} face; the cutter trusts every loaded neighbour.</li>
 * <li>the upstream {@code interceptConnect} cover interference (TileEntityBase09Connector
 *     :114-117, inside the connect handshake) is NOT ported — this repo's wires are not
 *     {@code ICoverableTE}s and the ported base handshake has no cover hook (card spec ⑥,
 *     declaration).</li>
 * <li>the client leg (:71 {@code isClientSide}) is the same-side claim pattern the
 *     crowbar item uses: claim SUCCESS on the client, the server decides PASS/CONSUME.</li>
 * <li>durability payment follows the crowbar 10000-units-→-1-point mapping
 *     (Behavior_Tool.doDamage units(…,10000,100)): every full 10000 units of returned
 *     tool damage costs one vanilla point. The emitter's 1000-unit relay return stays
 *     below one point and pays nothing — upstream's sub-point granularity cannot map
 *     onto integer vanilla durability (declared deviation).</li>
 * </ul>
 *
 * <p>Cut with the whole item shell (GT_Tool_WireCutter.java, numbers for the record):
 * the attack face (:53-55 200 units, :63-65 base damage 1.25F — the cutter is NOT a
 * weapon here, no attack attributes, card spec ③), the mining face (:80-87
 * isMinableBlock — the cable/wire class-name heuristic and the {@code TOOL_cutter}
 * harvest registry; this repo has no harvest layer and the GT wire family is not
 * minable-by-cutter in the port — pooled with the tool-family card), the tripwire
 * cutting behaviour (:102 Behavior_TripwireCutting) and the material tint (:95-97,
 * the same runtime-tint pool the crowbar declared). Durability 512, single steel tier
 * (upstream {@code 4*U} material-scaled, Loader_Tools.java:131 — the ladder is a pool
 * cut). The crafting recipe ({@code {"PfP","hPd","STS"}}, Loader_Tools.java:323 — needs
 * the h hammer + d tool PIECES) stays pooled with the crowbar recipe.
 */
public class GTCutterItem extends Item {

	/** The successful connection-toggle tool damage (upstream TileEntityBase09Connector:76 return). */
	public static final long TOOL_DAMAGE_PER_CUT = 10000;

	/** The vanilla durability points — single steel tier (upstream 4*U material-scaled, Loader_Tools:131). */
	public static final int DURABILITY_POINTS = 512;

	public GTCutterItem(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The flattened onToolClick2 — direct dispatch into the two arms. PASS on everything
	 * that is neither a wire nor a covered host; claims on the client only when the
	 * target is one (the server side executes and decides PASS).
	 */
	@Override
	public InteractionResult useOn(UseOnContext aContext) {
		BlockEntity tBE = aContext.getLevel().getBlockEntity(aContext.getClickedPos());
		if (!(tBE instanceof GTWireBlockEntity) && !(tBE instanceof ICoverableTE)) {
			return InteractionResult.PASS;
		}
		if (aContext.getLevel().isClientSide) {
			return InteractionResult.SUCCESS; // claim, the server side executes
		}
		return cutterToolClick(aContext) > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	/**
	 * The nine-grid seam (upstream :73) — the clicked-face hit coordinates are the
	 * 1.20.1 caller form of {@link UT6#getSideWrenching}: the world-space hit location
	 * minus the clicked block's origin. Static pure function so the offline tests pin
	 * the wiring without constructing the item (the mod-Item wall, CrowbarTest NOTE).
	 */
	public static byte targetSide(byte aClickedSide, Vec3 aHitLocation, BlockPos aPos) {
		return UT6.getSideWrenching(aClickedSide,
				(float) (aHitLocation.x - aPos.getX()),
				(float) (aHitLocation.y - aPos.getY()),
				(float) (aHitLocation.z - aPos.getZ()));
	}

	/**
	 * The single dispatch + payment surface over a context, shared by {@link #useOn} and
	 * the {@code /gt6tool cut} acceptance command (single-source semantics, the
	 * {@code /gt6tool dismantle} shape).
	 *
	 * @return the upstream tool damage (10000 per wire toggle, 1000 per emitter relay) or 0.
	 */
	public static long cutterToolClick(UseOnContext aContext) {
		Level tLevel = aContext.getLevel();
		BlockPos tPos = aContext.getClickedPos();
		BlockEntity tBE = tLevel.getBlockEntity(tPos);
		byte tSide = (byte) aContext.getClickedFace().get3DDataValue();
		long tDamage;
		if (tBE instanceof GTWireBlockEntity tWire) {
			tDamage = cutterToolClick(tWire, targetSide(tSide, aContext.getClickLocation(), tPos));
		} else if (tBE instanceof ICoverableTE tHost) {
			tDamage = cutterToolClick(tHost, aContext.getPlayer(), aContext.getItemInHand(), tSide,
					aContext.isSecondaryUseActive());
		} else {
			return 0;
		}
		payPerPoint(aContext.getItemInHand(), aContext.getPlayer(), tDamage);
		return tDamage;
	}

	/**
	 * The wire arm — the upstream :70-79 core with the statically-true getFacingTool gate.
	 * Toggle-only: this calls the existing base {@code connect}/{@code disconnect}
	 * handshake and NOTHING else (the GTWireBlockEntity zero-diff red line).
	 */
	public static long cutterToolClick(GTWireBlockEntity aWire, byte aTargetSide) {
		return (aWire.connected(aTargetSide) ? aWire.disconnect(aTargetSide, true) // :76 disconnect half
				: aWire.connect(aTargetSide, true)) // :76 connect half
				? TOOL_DAMAGE_PER_CUT : 0; // :76 — 10000 on success, 0 on a failed handshake
	}

	/**
	 * The cover arm — the ICoverableTE :275-276 relay with the reserved
	 * {@link GT6ToolActions#CUTTER_ID}; the command and the offline doubles share it.
	 */
	public static long cutterToolClick(ICoverableTE aHost, @Nullable Player aPlayer, ItemStack aStack, byte aSide, boolean aSneaking) {
		long tDamage = aHost.onCoverToolClick(GT6ToolActions.CUTTER_ID, aPlayer, aStack, aSide, aSneaking);
		payPerPoint(aStack, aPlayer, tDamage);
		return tDamage;
	}

	/**
	 * The durability mapping — one vanilla point per full {@link #TOOL_DAMAGE_PER_CUT}
	 * units, on a non-null player (ruling in the class javadoc; the hurtAndBreak shape is
	 * the GTCrowbarItem.crowbarToolClick payment verbatim).
	 */
	private static void payPerPoint(ItemStack aStack, @Nullable Player aPlayer, long aDamage) {
		long tPoints = aDamage / TOOL_DAMAGE_PER_CUT;
		if (tPoints > 0 && aPlayer != null) {
			aStack.hurtAndBreak((int) tPoints, aPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
	}

	/**
	 * The stack classifier — the ONLY action this item performs is
	 * {@link GT6ToolActions#CUTTER}; never {@code ToolActions.HOE_DIG} (the three
	 * wrench-substitute predicates must not see the cutter) and never the crowbar action.
	 * Static seam for the offline tests (the mod-Item wall).
	 */
	public static boolean classifies(ToolAction aToolAction) {
		return GT6ToolActions.CUTTER == aToolAction;
	}

	@Override
	public boolean canPerformAction(ItemStack aStack, ToolAction aToolAction) {
		return classifies(aToolAction);
	}
}
