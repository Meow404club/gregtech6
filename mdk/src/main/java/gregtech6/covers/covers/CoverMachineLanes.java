package gregtech6.covers.covers;

import gregtech6.covers.ICoverableTE;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.util.UT6;

/**
 * The coverable-machine state lane reads (task p35-covers-display-scale-6) — the
 * declared two-form mapping the P11 controller card established
 * (CoverControllerAutoRedstone.runsActivelyWithoutSuccess): the upstream
 * {@code ITileEntityRunningPossible/Passively/Actively/Successfully},
 * {@code ITileEntityProgress} and {@code ITileEntityEnergyDataCapacitor} interfaces have
 * no ported implementor to key on, but BOTH coverable machine forms carry the exact
 * field lanes verbatim — TileEntityOven.java:166-169 and TileEntityBasicMachine.java:243-246
 * ({@code mEnergy/mInputMin/mInput/mInputMax/mMinEnergy}, {@code mProgress/mMaxProgress},
 * {@code mSuccessful/mActive/mRunning}, {@code mCouldUseRecipe}).
 *
 * <p>The method-level mappings are the upstream MultiTileEntityBasicMachine answers:
 * <ul>
 * <li>{@code getStateRunningPossible()} :1023 — {@code mCouldUseRecipe || mActive ||
 *     mMaxProgress > 0 || ...}; the {@code mChargeRequirement} and
 *     {@code mIgnited && !mDisabledItemOutput && mOutputBlocked} arms read fields outside
 *     the ported lane set, the declared subset;</li>
 * <li>{@code getStateRunningPassively()} :1024 — {@code mRunning};
 *     {@code getStateRunningActively()} :1025 — {@code mActive};
 *     {@code getStateRunningSuccessfully()} :1026 — {@code mSuccessful};</li>
 * <li>{@code getProgressValue(aSide)} :1018 / {@code getProgressMax(aSide)} :1019 — the
 *     success-clamped, mMinEnergy-normalised progress pair (UT.Code.divup);</li>
 * <li>the capacitor face — {@code getEnergyStored}/{@code getEnergyCapacity} ride the
 *     machine's own internal buffer lane {@code mEnergy}/{@code mInputMax} (the :98 field
 *     block). Upstream the display/scale energy covers admit the
 *     ITileEntityEnergyDataCapacitor hosts (the battery boxes); the port admits the
 *     coverable machine forms whose energy lane is the read — the declared
 *     host-mapping deviation, no ported battery box is coverable today. Task p36 closes
 *     the deviation for the ZPM decharger (the BatBox family IS the upstream capacitor
 *     host): it joins the admission and its internal buffer is the read.</li>
 * </ul>
 *
 * <p>The upstream {@code canTick()} placement-gate half folds away: every ported machine
 * form ticks by construction.
 */
final class CoverMachineLanes {

	private CoverMachineLanes() {
	}

	/** True when the host is a coverable machine form or the task-p36 capacitor host (the lane carrier). */
	static boolean isMachineForm(ICoverableTE aHost) {
		return aHost instanceof TileEntityOven || aHost instanceof TileEntityBasicMachine
				|| aHost instanceof gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity; // p36 — the first coverable capacitor host (the upstream ITileEntityEnergyDataCapacitor admission restored)
	}

	/** Upstream :1023 — the running-possible lane (the declared subset, see the class doc). */
	static boolean runningPossible(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return tOven.mCouldUseRecipe || tOven.mActive || tOven.mMaxProgress > 0;
		if (aHost instanceof TileEntityBasicMachine tMachine) return tMachine.mCouldUseRecipe || tMachine.mActive || tMachine.mMaxProgress > 0;
		return false;
	}

	/** Upstream :1024. */
	static boolean runningPassively(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return tOven.mRunning;
		if (aHost instanceof TileEntityBasicMachine tMachine) return tMachine.mRunning;
		return false;
	}

	/** Upstream :1025. */
	static boolean runningActively(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return tOven.mActive;
		if (aHost instanceof TileEntityBasicMachine tMachine) return tMachine.mActive;
		return false;
	}

	/** Upstream :1018 — {@code mSuccessful ? getProgressMax : (mMinEnergy < 1 ? raw : divup(raw, mMinEnergy))}. */
	static long progressValue(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return progressValue(tOven.mSuccessful, tOven.mMaxProgress, tOven.mProgress, tOven.mMinEnergy);
		if (aHost instanceof TileEntityBasicMachine tMachine) return progressValue(tMachine.mSuccessful, tMachine.mMaxProgress, tMachine.mProgress, tMachine.mMinEnergy);
		return 0;
	}

	/** Upstream :1019 — {@code max(1, mMinEnergy < 1 ? raw : divup(raw, mMinEnergy))}. */
	static long progressMax(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return progressMax(tOven.mMaxProgress, tOven.mMinEnergy);
		if (aHost instanceof TileEntityBasicMachine tMachine) return progressMax(tMachine.mMaxProgress, tMachine.mMinEnergy);
		return 0;
	}

	/** The energy-buffer lane — the declared capacitor-face read (the class doc). */
	static long energyStored(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return tOven.mEnergy;
		if (aHost instanceof TileEntityBasicMachine tMachine) return tMachine.mEnergy;
		if (aHost instanceof gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity tDech) return tDech.mEnergy; // p36 — the internal buffer
		return 0;
	}

	/** The energy-buffer capacity lane — {@code mInputMax}, the :98 field block. */
	static long energyCapacity(ICoverableTE aHost) {
		if (aHost instanceof TileEntityOven tOven) return tOven.mInputMax;
		if (aHost instanceof TileEntityBasicMachine tMachine) return tMachine.mInputMax;
		if (aHost instanceof gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity tDech) return tDech.capacity(); // p36 — the :216 progress-max buffer cap
		return 0;
	}

	/** The :1018 body. */
	private static long progressValue(boolean aSuccessful, long aMaxProgress, long aRaw, long aMinEnergy) {
		return aSuccessful ? progressMax(aMaxProgress, aMinEnergy) : aMinEnergy < 1 ? aRaw : UT6.divup(aRaw, aMinEnergy);
	}

	/** The :1019 body. */
	private static long progressMax(long aRaw, long aMinEnergy) {
		return Math.max(1, aMinEnergy < 1 ? aRaw : UT6.divup(aRaw, aMinEnergy));
	}
}
