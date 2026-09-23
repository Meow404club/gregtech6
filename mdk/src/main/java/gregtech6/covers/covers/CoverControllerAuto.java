package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import gregtech6.covers.CoverData;

/**
 * The automatic machine switch cover — 1.20.1 port of gregapi/cover/covers/
 * CoverControllerAuto.java (:33-46, task p35-covers-display-scale-6; upstream
 * MultiItemTechnological.java:63 meta 1003 "Automatic Machine Switch", dump
 * 自动开关). "Automatically turns Machines ON/OFF when needed": the held state IS the
 * machine's own running-possible-or-active answer (:44) — a machine with something to do
 * runs, an idle machine is held OFF.
 *
 * <p>Host admission (:34): the running-possible lane carrier that is also switchable —
 * the {@code canTick()} half folds (the CoverMachineLanes ruling), the
 * running-possible face maps onto the two coverable machine forms (the declared
 * mapping), and the Controller base gate supplies the switchable half.
 *
 * <p>Texture (:40): upstream {@code machines/covers/autoswitch/circuit} path-maps to
 * {@code gt6:block/auto_switch/circuit} (the byte-identical borrow, assets/README.md).
 */
public class CoverControllerAuto extends AbstractCoverAttachmentController {

	/** The sprite path — upstream CoverControllerAuto.java:40, lowercased/underscored. */
	public static final String SPRITE_PATH = "block/auto_switch/circuit";

	/** The sprite id; static so the tables stay registry-free. */
	public static ResourceLocation sprite() {
		return new ResourceLocation("gt6", SPRITE_PATH);
	}

	/**
	 * Upstream :34 — the placement gate: refuse unless the host is a running-possible
	 * lane carrier AND switchable (the base gate answers the switchable half).
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !CoverMachineLanes.isMachineForm(aData.mTileEntity) || super.interceptCoverPlacement(aCoverSide, aData, aPlayer);
	}

	/**
	 * Upstream :44 verbatim — {@code (RunningPossible && getStateRunningPossible()) ||
	 * (RunningActively && getStateRunningActively())}. The ported machine forms carry
	 * both lanes, so the two-instanceof dance folds into two lane reads.
	 */
	@Override
	public boolean getStateOnOff(byte aCoverSide, CoverData aData) {
		return CoverMachineLanes.runningPossible(aData.mTileEntity) || CoverMachineLanes.runningActively(aData.mTileEntity);
	}

	/** Upstream :36/:40 — the switch art. */
	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return sprite();
	}
}
