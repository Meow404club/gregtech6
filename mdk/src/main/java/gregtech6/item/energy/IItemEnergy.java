package gregtech6.item.energy;

import java.util.Collection;

import net.minecraft.world.item.ItemStack;

import gregapi.code.TagData;

/**
 * The item energy seam (task p29-w4-battery-storage ①) — a literal translation of upstream
 * {@code gregapi/item/IItemEnergy.java:41-190}, NARROWED to the minimum face the card's
 * consumers need (the port archaeology: ZERO prior IItemEnergy in this repo — the seam is
 * pure-new, the sym_query zero-hit census 2026-09-15):
 * <ol>
 * <li>{@link #isEnergyType} / {@link #getEnergyTypes} — the type-identity faces (upstream
 *     :56/:63);</li>
 * <li>{@link #doEnergyInjection} / {@link #canEnergyInjection} /
 *     {@link #doEnergyExtraction} / {@link #canEnergyExtraction} — the push/pull packet
 *     faces (upstream :73-:86; the BatteryBox exchange drives exactly these,
 *     TileEntityBase10EnergyBatBox.java:111-114/:132-133);</li>
 * <li>{@link #useEnergy} — the canUse+use-in-one simulate form (upstream :88-95, the
 *     combined face the electric tools consume — the W5 tool card is the consumer);</li>
 * <li>{@link #setEnergyStored} / {@link #getEnergyStored} / {@link #getEnergyCapacity} —
 *     the storage faces (upstream :101-:113; the W5 capacity-sum face
 *     Loader_Tools.java:427-451 rides {@link #getEnergyCapacity}).</li>
 * </ol>
 *
 * <p><b>The declared narrowing</b> (the "直译收窄" of the card SPEC): the upstream
 * signatures carry {@code IInventory aInventory, World aWorld, int aX/aY/aZ} on the
 * injection/extraction/use faces "in case something should happen when tampering with the
 * Item" (upstream :42-48 — the remote-rechargeable hook) plus the
 * {@code EntityLivingBase aPlayer} on use (the creative-mode bypass + the armor-recharge
 * arm, TileEntityBase08Battery.java:176-208). The port drops those parameters: no port
 * consumer reads them, the only upstream reader is the COMPAT_EU_ITEM armor arm that has
 * no port counterpart, and the W5 tool card can widen the face when its player-facing
 * consumers actually land. The TagData energy-type parameter is KEPT on every method —
 * the TD.Energy.EU / TD.Energy.LU dual domain is the seam's whole point (the LU domain
 * proven live by the W2 source-block short codes).
 *
 * <p><b>Cut with declaration</b>: the six packet-size band faces
 * ({@code getEnergySizeInputMin/Recommended/Max} and the output triple, upstream
 * :116-:158) do NOT enter the interface — the band lives in the implementer
 * ({@link GT6BatteryItem} carries the Base08 :62-66 mSizeMin/Rec/Max semantics
 * internally and the can* faces enforce it), and no port consumer needs to READ the
 * band off an item. The {@code Utility} static class (upstream :160-189) is also cut —
 * it existed to bridge the IC2 COMPAT_EU_ITEM fallback which this port does not have;
 * the BatteryBox dispatches {@code instanceof IItemEnergy} directly (the upstream
 * :111/:131 branches, the only branches the port keeps).
 *
 * <p>KJS surface: none — this is a port-internal interface with zero datapack or
 * KubeJS face (the card's KJS declaration).
 */
public interface IItemEnergy {

	/**
	 * Upstream :50-56 verbatim (doc): you do not have to check for this Function, this is
	 * only for things like Slot insertion Conditions and similar.
	 *
	 * @param aEnergyType the Type of Energy
	 * @param aEmitting   if it is asked to emit this Energy Type, otherwise it is asked to
	 *                    accept this Energy Type
	 * @return if this Item has anything to do with this Type of Energy. The returning
	 *         Value must be constant for this Item.
	 */
	boolean isEnergyType(TagData aEnergyType, ItemStack aStack, boolean aEmitting);

	/**
	 * Upstream :58-63: all the Types of Energy which are relevant to this Item. Use
	 * {@link java.util.Collections#EMPTY_LIST} if you don't have any.
	 */
	Collection<TagData> getEnergyTypes(ItemStack aStack);

	/**
	 * Upstream :65-73: charges an Energy Storage.
	 *
	 * @param aDoInject if this is supposed to increase the internal Energy; false = just a
	 *                  simulation
	 * @return amount of used aAmount
	 */
	long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoInject);

	/** Upstream :75 (canEnergyInjection). */
	boolean canEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize);

	/**
	 * Upstream :77-86: decharges an Energy Storage.
	 *
	 * @param aDoExtract if this is supposed to decrease the internal Energy; false = just
	 *                   a simulation
	 * @return amount of taken aAmount
	 */
	long doEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoExtract);

	/** Upstream :86 (canEnergyExtraction). */
	boolean canEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize);

	/**
	 * Upstream :88-95, the "canUse and use together in one Function by using a Simulate
	 * Parameter" form.
	 *
	 * @param aDoUse if this is supposed to decrease the internal Energy; false = just a
	 *               simulation
	 * @return true if this can use that much Energy
	 */
	boolean useEnergy(TagData aEnergyType, ItemStack aStack, long aEnergyAmount, boolean aDoUse);

	/**
	 * Upstream :97-101: stores an amount of Energy, returns the ItemStack you used as a
	 * Parameter (for convenience).
	 */
	ItemStack setEnergyStored(TagData aEnergyType, ItemStack aStack, long aAmount);

	/** Upstream :103-107: amount of Energy stored. */
	long getEnergyStored(TagData aEnergyType, ItemStack aStack);

	/** Upstream :109-113: amount of Energy that can be stored; 0 if not accepting this Energy. */
	long getEnergyCapacity(TagData aEnergyType, ItemStack aStack);
}
