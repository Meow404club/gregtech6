package gregtech6.items.tools.loot;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 3x3x3 neighbourhood re-harvest helper — task p29-w5-t1-dig-six shared
 * infrastructure ② (the wave ruling decisions.p30-w5-split-rulings: t1 builds,
 * t2 Axe / t4 Plow+Sense consume). Upstream form: the {@code sIsHarvestingRightNow}
 * ThreadLocal instance field + the 26-neighbour {@code tryHarvestBlock} walk, verbatim
 * on GT_Tool_Plow.java:42/:67-75 and GT_Tool_Sense.java:47/:76-91 — the guard stops a
 * neighbour's own break flow (its loot conversion re-enters the sweep) from recursing
 * into an infinite collapse.
 *
 * <p><b>Consumer contract</b> (the t2/t4/t5/t6 seam — read before calling):
 * <ul>
 * <li>Call {@link #sweep(ServerPlayer, ItemStack, BlockPos)} ONCE per break, from the
 *     caller's loot-conversion arm, with the BREAKING player and the held stack. The
 *     guard means re-entrant calls (a neighbour break firing the same consumer again)
 *     return 0 — exactly the upstream {@code sIsHarvestingRightNow.get() == null} gate.</li>
 * <li>The per-neighbour filter is the upstream {@code getDigSpeed(...) > 0} test, modern
 *     {@code ItemStack.getDestroySpeed(state) > 0} — air is excluded (upstream never
 *     saw air through the test; the modern speed form would return the bare tool speed
 *     on it, so the guard is explicit here).</li>
 * <li>Each neighbour rides {@code ServerPlayerGameMode.destroyBlock} — the modern
 *     {@code tryHarvestBlock} face: INSTANT-break neighbours (plants, grass, torches)
 *     break inside this call; hardness-bearing neighbours only accrue progress across
 *     repeated sweeps. Consumers whose arm wants hard neighbours down must sweep again
 *     per tick or use a dedicated flow — this helper is the instant-plant form.</li>
 * <li>The caller OWNS the tool durability payment (upstream doDamage rode the
 *     behaviours, not the walk).</li>
 * </ul>
 *
 * <p>The ThreadLocal is a static sentinel (upstream used one instance field per tool —
 * the flat-item port has one shared walk, so one shared guard; a per-call-site guard
 * would let tool A re-enter tool B's collapse, the cross-tool case upstream never hit
 * because each ToolStats instance owned its field).
 */
public final class GT6ToolSweep {

	private static final Object INSIDE = new Object();
	private static final ThreadLocal<Object> sIsHarvestingRightNow = new ThreadLocal<>();

	private GT6ToolSweep() {
	}

	/** The upstream {@code sIsHarvestingRightNow.get() != null} read, exposed for tests and consumer prechecks. */
	public static boolean inside() {
		return sIsHarvestingRightNow.get() != null;
	}

	/**
	 * The upstream set-arm: claim the thread or report the re-entrance. {@code true} =
	 * caller proceeds with the walk; {@code false} = a sweep is already running on this
	 * thread, caller skips. ALWAYS pair with {@link #exit()} (the sweep method pairs it
	 * in a finally; direct consumers should too).
	 */
	public static boolean tryEnter() {
		if (sIsHarvestingRightNow.get() != null) return false;
		sIsHarvestingRightNow.set(INSIDE);
		return true;
	}

	/** The upstream clear-arm ({@code sIsHarvestingRightNow.set(null)}). */
	public static void exit() {
		sIsHarvestingRightNow.remove();
	}

	/**
	 * The 3x3x3 walk — the upstream Plow/Sense body :70-72 (Plow) / :80-84 (Sense): 26
	 * neighbours of {@code aCenter}, every block passing {@code aFilter} (upstream: the
	 * dig-speed test) goes through {@code destroyBlock}; the return is the upstream
	 * {@code rConversions} count of blocks actually broken. Re-entrant calls return 0
	 * (the guard).
	 *
	 * @param aExtraFilter additional consumer-side predicate ANDed with the dig-speed
	 *        test (upstream Plow/Sense needed none — pass {@code null}); kept so the
	 *        t2/t4 consumers can narrow the face without forking the walk.
	 */
	public static int sweep(ServerPlayer aPlayer, ItemStack aTool, BlockPos aCenter, Predicate<BlockState> aExtraFilter) {
		if (aPlayer == null || aPlayer.level().isClientSide()) return 0;
		if (!tryEnter()) return 0;
		try {
			int rConversions = 0;
			ServerLevel tLevel = aPlayer.serverLevel();
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) for (int k = -1; k < 2; k++) {
				if (i == 0 && j == 0 && k == 0) continue; // the centre block is the caller's own break
				BlockPos tNeighbour = aCenter.offset(i, j, k);
				BlockState tState = tLevel.getBlockState(tNeighbour);
				if (tState.isAir()) continue; // the modern dig-speed form is positive on air — the explicit guard
				if (aTool.getDestroySpeed(tState) <= 0) continue; // upstream getDigSpeed(...) > 0
				if (aExtraFilter != null && !aExtraFilter.test(tState)) continue;
				if (aPlayer.gameMode.destroyBlock(tNeighbour)) rConversions++;
			}
			return rConversions;
		} finally {
			exit();
		}
	}

	/** The no-extra-filter convenience overload (the upstream call shape). */
	public static int sweep(ServerPlayer aPlayer, ItemStack aTool, BlockPos aCenter) {
		return sweep(aPlayer, aTool, aCenter, null);
	}
}
