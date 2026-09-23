package gregtech6.covers.covers;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.machines.ITileEntitySwitchableOnOff;

/**
 * The auto reboot switch cover — 1.20.1 port of gregapi/cover/covers/
 * CoverControllerAutoTimer.java (:34-56, task p35-covers-display-scale-6; upstream
 * MultiItemTechnological.java:68-72 metas 1009-1013, the five durations 1200..36000
 * ticks = 1..30 minutes, dump 自动重启开关 (N分钟)). "Attempts to Reboot a Machine every
 * Minute": every {@code mTime} ticks the switch pulses the machine OFF for the last 10
 * ticks of the cycle (:49 — {@code aTimer % mTime >= mTime - 10} holds ON, so OFF is the
 * 10-tick window) and lets the tick poll release it again — a periodically rebooting
 * machine never wedges. An actively running machine is never pulsed (the
 * {@code getStateRunningActively()} first arm).
 *
 * <p>Upstream binds one art per duration
 * ({@code machines/covers/autotimerswitch/<time>/circuit}); the upstream tree ships only
 * the 6000 face — the port borrows that single circuit sprite for the whole ladder
 * ({@code gt6:block/auto_timer_switch/circuit}, the declared art fold, assets/README.md).
 */
public class CoverControllerAutoTimer extends AbstractCoverAttachmentController {

	/** The sprite path — the shared ladder art (the declared fold, see the class doc). */
	public static final String SPRITE_PATH = "block/auto_timer_switch/circuit";

	/** The five ladder durations — upstream MultiItemTechnological.java:68-72 verbatim. */
	public static final int[] TIMER_TIMES = {1200, 6000, 12000, 24000, 36000};

	/** Upstream :35 — the cycle length, floored at 11 ticks. */
	public final int mTime;

	public CoverControllerAutoTimer(int aTime) {
		mTime = Math.max(11, aTime);
	}

	/** The sprite id; static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/**
	 * Upstream :48-50 — the timer poll: an actively running machine stays ON; otherwise
	 * the state is OFF except the last 10 ticks of each cycle, which pulse ON (the
	 * reboot attempt the tick poll then re-evaluates).
	 */
	@Override
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide && aData.mTileEntity instanceof ITileEntitySwitchableOnOff tTE) {
			tTE.setStateOnOff(CoverMachineLanes.runningActively(aData.mTileEntity) || aTimer % mTime >= mTime - 10);
		}
	}

	/** Upstream :53-55 — the load/mount arms always re-arm the machine ON (the timer owns the cycle from there). */
	@Override
	public boolean getStateOnOff(byte aCoverSide, CoverData aData) {
		return true;
	}

	/** Upstream :43 — the timer circuit art (the shared ladder face). */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
