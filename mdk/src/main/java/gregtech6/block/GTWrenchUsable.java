package gregtech6.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * The wrench-usable block marker (task wrench-interaction-chain, symptom18): this
 * block's {@code use()} carries a wrench arm (the pipe connection/ioMask toggles, the
 * oven shift-rotation, the facing-machine wrenchRotate pick) — so the vanilla
 * sneak-use gate must not swallow its wrench clicks.
 *
 * <p>The breakpoint this marker fixes (the full-chain census, upstream-referenced):
 * vanilla 1.20.1 {@code ServerPlayerGameMode.useItemOn} (:312-315) skips
 * {@code blockstate.use} whenever the player is secondary-use-active (sneaking) with a
 * non-empty hand, and 1.7.10 had the same gate — which is why the upstream wrench never
 * noticed it: upstream the wrench dispatches from the ITEM chain
 * ({@code Behavior_Tool.onItemUseFirst}, gregapi/item/multiitem/behaviors/Behavior_Tool.java:57-68,
 * which runs BEFORE block activation on both sides and is sneak-proof, passing
 * {@code aPlayer.isSneaking()} into {@code IBlockToolable.Util.onToolClick},
 * gregapi/block/IBlockToolable.java:60/:73-80). The port put the dispatch inside
 * {@code block.use} instead — so every shift+right-click wrench interaction was dead
 * behind the gate ("点击毫无反应", field_test symptom18), no matter how correct the
 * in-block branches were. The repair keeps the single dispatch point: a FORGE-bus
 * {@code PlayerInteractEvent.RightClickBlock} listener ({@link GTWrenchSneakUseListener})
 * answers {@code setUseBlock(ALLOW)} for sneaking wrench-key clicks onto
 * {@link GTWrenchUsable} blocks — the Forge patch honours the result on BOTH legs
 * (patches/minecraft/net/minecraft/server/level/ServerPlayerGameMode.java.patch:134
 * {@code event.getUseBlock() == ALLOW || (!= DENY && !flag1)}; the twin
 * MultiPlayerGameMode.java.patch:130 keeps the client prediction symmetric), so the
 * existing in-block arms execute unmodified, sneak included.
 *
 * <p>Implementations: {@code GTFluidPipeBlock}, {@code GTItemPipeBlock},
 * {@code GTOvenBlock}, {@code GT6ElectricTransformerBlock} (and the
 * {@code wrenchRotate} family — dynamo, battery box incl. the ZPM decharger heir),
 * {@code GTSensorBlock}. A new wrench-clickable block implements this marker and the
 * sneak gate lifts itself — no listener edit (that is the drift-proof point of the
 * marker; the pure-class pin lives in the offline test).
 *
 * <p>Marker by design (zero members): the per-block predicates stay INSIDE each
 * {@code use()} — the substitute pool keys the shared
 * {@code GT6ToolActions.isWrenchInteractionKey} seam while the facing-machine family
 * keys {@code GT6ToolActions.WRENCH} (the upstream {@code getFacingTool()} shape,
 * TileEntityBase09Connector.java:72 — the monkey wrench is NOT a wrench there) — and
 * this marker carries none of that. It only answers "does a wrench-key sneak click on
 * this block deserve to reach {@code use()}"; the upstream vanilla chest/furnace
 * behaviour (sneak+wrench = nothing) is preserved by the marker's narrowness — the
 * item-side {@code doesSneakBypassUse} road was rejected precisely because it un-gates
 * EVERY block.
 */
public interface GTWrenchUsable {

	/** The static seam the listener and the offline pin read (no instance needed). */
	static boolean is(BlockState aState) {
		return aState.getBlock() instanceof GTWrenchUsable;
	}
}
