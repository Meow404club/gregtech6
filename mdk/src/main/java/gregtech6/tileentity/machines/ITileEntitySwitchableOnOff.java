package gregtech6.tileentity.machines;

/**
 * A machine whose ON/OFF state an external controller (the redstone machine switch
 * cover) may flip — 1.20.1 port of gregapi/tileentity/machines/
 * ITileEntitySwitchableOnOff.java (38 lines, task p10-cover-controller-redstone).
 *
 * <p>The upstream {@code ITileEntityUnloadable} super-interface folds away (the same
 * trim the multiblock controller interface recorded — every 1.20.1 BlockEntity carries
 * its level through {@code getLevel()}, there is no unloaded-state contract to port).
 *
 * <p>Consumer: {@code gregtech6.covers.covers.CoverControllerRedstone} — the five-arm
 * controller drives {@link #setStateOnOff(boolean)}; the machine keeps its own manual
 * stop latch separate (TileEntityOven mStopped, the upstream :1027/:1028 method pair).
 */
public interface ITileEntitySwitchableOnOff {

	/**
	 * Upstream :32.
	 *
	 * @param aOnOff true for ON false for OFF.
	 * @return the state of the Machine after it has switched, see getStateOnOff.
	 */
	boolean setStateOnOff(boolean aOnOff);

	/**
	 * Upstream :37.
	 *
	 * @return the ON/OFF State of the Machine. true for ON false for OFF.
	 */
	boolean getStateOnOff();
}
