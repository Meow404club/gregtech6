package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraft.core.Direction;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}

import gregtech6.registry.GT6FeBatteries;

/**
 * The FE battery test fixture BE (task p26-eu-bridge-outbound): a platform
 * {@code EnergyStorage} reference implementation inside a plain BlockEntity. Both legs ship
 * the same reference class shape — javap-verified constructor twins
 * {@code (int capacity, int maxReceive, int maxExtract)} and the same six IEnergyStorage
 * methods — differing only in package ({@code net.minecraftforge.energy} vs
 * {@code net.neoforged.neoforge.energy}), which is what the //? forks below carry.
 *
 * <p><b>Deliberately NOT a TileEntityBase01Root subclass — the fixture plays the FOREIGN
 * receiver.</b> The root dispatch (ITileEntityEnergy.Util.insertEnergyInto, the
 * EnergyCompat.insertEnergyInto:140 counterpart) routes GT-family receivers (everything
 * under 01Root, which implements ITileEntityEnergy) to doEnergyInjection and ONLY
 * non-GT receivers to the EnergyBridge — exactly like upstream, whose EnergyCompat:143
 * comment excludes the GT6 Root blocks from compat ("Obvious GT6 Blocks should not be
 * eligible for Compat"). A fixture that subclassed the Root family would be a GT receiver
 * by identity and the bridge would never fire; the first RCON pass proved exactly that
 * (three phases, stored 0, zero errors). A foreign FE machine is not a GT BE — so the
 * fixture extends BlockEntity directly, the same identity the CountingSink test double
 * uses (GTEnergySourceBlockEntityTest).
 *
 * <p>Capacity = maxReceive, maxExtract = 0: a pure sink, because the bridge is outbound-only
 * (the research.p26-r-eu-bridge ruling; the GTCEu nativeEUToFE path is strictly one-way the
 * same way, EUToFEProvider.outputsEnergy = false, :190-192).
 *
 * <p>The {@link MutableEnergyStorage} subclass exists because the INBTSerializable faces
 * DIVERGED across the legs — forge 1.20.1 serializes as {@code serializeNBT()}/
 * {@code deserializeNBT(Tag)} while neoforge 21.1.249 carries the HolderLookup.Provider
 * parameter pair (javap-verified) — but both keep {@code energy} protected. Direct protected
 * field access through a same-shape subclass dodges the fork entirely (and the RCON reset
 * dial needs the same write face).
 *
 * <p>Capability exposure, per leg (the P15 two-seam model):
 * <ul>
 * <li>1.20.1 forge: the {@link #getCapability} override answers
 *     {@code ForgeCapabilities.ENERGY} with a LazyOptional over the storage (the
 *     TileEntityBase01Root.java:439-446 shape; IEnergyStorage carries
 *     &#64;AutoRegisterCapability, forge-1.20.1 IEnergyStorage.java:19, so no registration
 *     event is needed on this leg).</li>
 * <li>1.21.1 neoforge: BlockEntity has no getCapability to override (21.1.249 javap) —
 *     the GT6CapabilityWiring registerFeBattery row (tail-appended by this card) hands the
 *     BET to {@code RegisterCapabilitiesEvent.registerBlockEntity} with a provider reading
 *     {@link #energyStorage()}.</li>
 * </ul>
 *
 * <p>The bridge handler (GT6EuToFeBridgeForge / GT6EuToFeBridgeNeoforge) finds this storage
 * through the LEVEL QUERY — the same face every foreign FE machine answers — and bills the
 * packet math in root {@code EnergyBridge.insertFe} (4 FE per 1 EU, packet-aligned).
 *
 * <p>Persistence: the stored FE rides "energy"; no tile-name lane (the fixture is not on
 * the 01Root NBT schema).
 */
public class GT6FeBatteryBlockEntity extends BlockEntity {

	/** The stored-FE NBT key. */
	public static final String NBT_ENERGY = "energy";

	/**
	 * 100k FE at 4:1 = 25k EU of sink room: enough to soak the RCON chain's default rig
	 * (32 EU x 1 A = 128 FE/t) for ~13 minutes of continuous emit without saturating, so
	 * acceptance assertions stay in the linear fill region.
	 */
	public static final int CAPACITY = 100000;

	/**
	 * The leg's own reference implementation (the javap-verified twin both legs ship), plus
	 * the one write face the NBT load and the RCON reset dial need — {@code energy} is
	 * protected on BOTH legs (javap), so the subclass is byte-identical per leg.
	 */
	//? if forge {
	private static final class MutableEnergyStorage extends net.minecraftforge.energy.EnergyStorage {
		private MutableEnergyStorage(int aCapacity) { super(aCapacity, aCapacity, 0); }
		private void setStoredFe(int aEnergy) { this.energy = Math.max(0, Math.min(capacity, aEnergy)); }
	}
	//?} else {
	/*private static final class MutableEnergyStorage extends net.neoforged.neoforge.energy.EnergyStorage {
		private MutableEnergyStorage(int aCapacity) { super(aCapacity, aCapacity, 0); }
		private void setStoredFe(int aEnergy) { this.energy = Math.max(0, Math.min(capacity, aEnergy)); }
	}
	 *///?}

	private final MutableEnergyStorage mStorage = new MutableEnergyStorage(CAPACITY);
	//? if forge {
	private final LazyOptional<net.minecraftforge.energy.IEnergyStorage> mCap = LazyOptional.of(() -> mStorage);
	//?}

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6FeBatteryBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET (the
	 * GTEnergySourceBlockEntity dual-constructor precedent).
	 */
	public GT6FeBatteryBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6FeBatteries.FE_BATTERY_BE.get(), aPos, aState);
	}

	/** The stored FE, straight from the reference implementation (the /gt6febattery stat face). */
	public int storedFe() {
		return mStorage.getEnergyStored();
	}

	/** The RCON reset dial's write face (the protected-field subclass, the class doc). */
	public void resetStoredFe(int aEnergy) {
		mStorage.setStoredFe(aEnergy);
		setChanged();
	}

	/** The storage face the 21.1 wiring provider and the offline tests read. */
	//? if forge {
	public net.minecraftforge.energy.EnergyStorage energyStorage() {
		return mStorage;
	}
	//?} else {
	/*public net.neoforged.neoforge.energy.EnergyStorage energyStorage() {
		return mStorage;
	}
	 *///?}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		// IEnergyStorage carries @AutoRegisterCapability (forge-1.20.1 IEnergyStorage.java:19) —
		// no RegisterCapabilitiesEvent wiring needed on this leg; the invalidation cycle is
		// driven by the Forge-patched setRemoved() inserting invalidateCaps().
		if (aCapability == ForgeCapabilities.ENERGY) {
			return mCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mCap.invalidate();
	}
	//?}

	// ---------------------------------------------------------------------------
	// NBT (the stored FE; the 100k capacity and the 0 maxExtract are compile-time constants)
	// The vanilla hook signatures diverged across the legs (1.21.1 carries the
	// HolderLookup.Provider pair and no load(CompoundTag) — the 01Root:131 fork record);
	// the fixture forks the same way the whole 01Root tree does.
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putInt(NBT_ENERGY, mStorage.getEnergyStored());
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) {
			// the raw write face (setChanged is pointless during the load window)
			mStorage.setStoredFe(aNBT.getInt(NBT_ENERGY));
		}
	}
	//?} else {
	/* @Override
	protected void saveAdditional(CompoundTag aNBT, net.minecraft.core.HolderLookup.Provider aProvider) {
		super.saveAdditional(aNBT, aProvider);
		aNBT.putInt(NBT_ENERGY, mStorage.getEnergyStored());
	}

	@Override
	protected void loadAdditional(CompoundTag aNBT, net.minecraft.core.HolderLookup.Provider aProvider) {
		super.loadAdditional(aNBT, aProvider);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) {
			mStorage.setStoredFe(aNBT.getInt(NBT_ENERGY));
		}
	}
	 *///?}
}
