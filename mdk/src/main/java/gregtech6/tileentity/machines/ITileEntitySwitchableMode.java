package gregtech6.tileentity.machines;

/**
 * The switchable-mode host dial — 1.20.1 port of
 * gregapi/tileentity/machines/ITileEntitySwitchableMode.java:27-38 (task
 * p34-covers-gameplay-10; the {@code extends ITileEntityUnloadable} marker interface
 * rides the port's BE base, so this port is the plain two-method face). The four
 * selector covers (task p34-covers-gameplay-10) drive it: the host narrows the mode
 * it accepts, the cover asserts {@link #setStateMode} and reads back through the
 * return.
 *
 * <p>FIRST-HOST DECLARATION: no production BE implements this face yet — the selector
 * covers mount on switchable-mode hosts and the first live host is the declared
 * host-composition follow-up card (the GTWireBlockEntity mMode javadoc names the same
 * landing: "the semantics stay complete for the cover card"). The offline pins drive
 * the dial through a test probe implementing this interface.
 */
public interface ITileEntitySwitchableMode {

	/**
	 * Upstream :32 — {@code @param aMode} number between 0 and 15, usable with redstone.
	 *
	 * @return the state of the Machine after it has switched (the CoverSelectorManual
	 *         :88 and CoverSelectorRedstone :42 write-backs compose it into the visual lane).
	 */
	byte setStateMode(byte aMode);

	/** Upstream :37 — the Mode of the Machine. */
	byte getStateMode();
}
