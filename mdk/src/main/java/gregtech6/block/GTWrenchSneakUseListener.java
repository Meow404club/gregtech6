package gregtech6.block;

import net.minecraft.world.entity.player.Player;

//? if forge {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?}

import gregtech6.items.tools.GT6ToolActions;

/**
 * The sneak-use un-gate (task wrench-interaction-chain, symptom18 "扳手 shift+右键指定流向
 * 全断"): vanilla skips {@code blockstate.use} while sneaking with a non-empty hand
 * (1.20.1 ServerPlayerGameMode.useItemOn :312-315 {@code isSecondaryUseActive}; the same
 * gate existed in 1.7.10 — upstream rode the sneak-proof ITEM chain,
 * {@code Behavior_Tool.onItemUseFirst} gregapi/.../Behavior_Tool.java:57-68, feeding
 * {@code aPlayer.isSneaking()} into {@code IBlockToolable.Util.onToolClick},
 * IBlockToolable.java:60). The port's wrench arms live in {@code block.use}, so every
 * shift+click was dead at this gate before any branch could run. This listener lifts the
 * gate for exactly the marker blocks ({@link GTWrenchUsable}) — the Forge patch honours
 * {@code setUseBlock(ALLOW)} past the sneak check on BOTH legs
 * (patches/minecraft/net/minecraft/server/level/ServerPlayerGameMode.java.patch:134, and
 * the client twin MultiPlayerGameMode.java.patch:130 keeps the prediction symmetric, so
 * the {@code CONSUME} claims inside the pipe {@code use} arms stay ghost-free).
 *
 * <p>The predicate mirrors what {@code block.use} will re-check with the SAME hand:
 * the shared {@link GT6ToolActions#isWrenchInteractionKey} on the event's own stack
 * ({@code getItemStack()} = {@code player.getItemInHand(hand)}). Per-hand, not
 * both-hands: the main hand always gets the first packet, and an empty main with a
 * wrench off-hand reaches this listener again on the off-hand pass once the main hand
 * PASSes — the same item the in-block arm will read. Non-GT blocks stay vanilla
 * (sneak+wrench on a chest does nothing, the upstream ToolCompat no-op shape).
 *
 * <p>Both sides, both hands of the bus: the event fires on the client
 * (MultiPlayerGameMode) and the server (ServerPlayerGameMode); this listener is
 * registered un-distinct (the {@link GT6PlaceablePlacement} shape) so the one method
 * serves prediction and execution. Creative mode honours the right-click results (the
 * {@code setUseBlock has no effect} caveat in PlayerInteractEvent is the LEFT-click
 * subclass's javadoc only).
 */
//? if forge {
@Mod.EventBusSubscriber(modid = "gt6")
//?} else {
/*@EventBusSubscriber(modid = "gt6")
*///?}
public final class GTWrenchSneakUseListener {

	private GTWrenchSneakUseListener() {
	}

	//? if forge {
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock aEvent) {
	//?} else {
	/*@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock aEvent) {
	*///?}
		Player tPlayer = aEvent.getEntity();
		if (!tPlayer.isShiftKeyDown()) return; // only the sneaking leg is gated by vanilla
		if (!GT6ToolActions.isWrenchInteractionKey(aEvent.getItemStack())) return;
		if (!GTWrenchUsable.is(aEvent.getLevel().getBlockState(aEvent.getPos()))) return;
		//? if forge {
		aEvent.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
		//?} else {
		/*aEvent.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
		*///?}
	}
}
