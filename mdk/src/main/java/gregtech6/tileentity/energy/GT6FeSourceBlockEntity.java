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
 * The FE source test fixture BE (task p28-b-fe-converter-machine) — the EXTRACTABLE twin
 * of the p26 sink battery: the same platform {@code EnergyStorage} reference shape, but
 * (capacity, maxReceive=0, maxExtract=capacity) — a PURE SOURCE. The existing fe_battery
 * is a pure sink (maxExtract = 0), which is exactly right for the OUTBOUND bridge and
 * exactly wrong for the INBOUND converter's pull face; this fixture gives the p28 RCON
 * chain a source the converter can pull whole packet trains out of (through
 * EnergyBridge.extractFe over the adapted {@code extractEnergy}, the IFESource lambda
 * shape).
 *
 * <p>Deliberately NOT a TileEntityBase01Root subclass — the fixture plays the FOREIGN
 * source, the same identity ruling as the sink battery (GT6FeBatteryBlockEntity class
 * doc: a 01Root-family BE would be a GT receiver by identity). Also deliberately NOT on
 * the creative tab and WITHOUT a loot table — the fixture domain, RCON-driven only
 * (/gt6fesource place|stat|set|reset; place pre-fills to capacity, set dials the exact
 * amount the pull-math arms need, reset drains).
 *
 * <p>Capability exposure, per leg (the two-seam model, verbatim the sink battery):
 * 1.20.1 forge answers through the getCapability override; 1.21.1 neoforge rides the
 * GT6CapabilityWiring registerFeSource row.
 */
public class GT6FeSourceBlockEntity extends BlockEntity {

	/** The stored-FE NBT key (the battery's key — the fixtures share the schema). */
	public static final String NBT_ENERGY = "energy";

	/**
	 * 100k FE at 4:1 = 25k EU of supply: the RCON pull rig drains whole 32 FE packets
	 * (8 EU x 4, the ULV packet — the 128 FE wording was the pre-ruling ladder band), so
	 * the capacity covers any arm many times over while staying in the linear region.
	 */
	public static final int CAPACITY = 100000;

	/**
	 * The leg's reference implementation, the PURE-SOURCE parameterization
	 * {@code (capacity, 0, capacity)} (the sink battery is {@code (capacity, capacity, 0)}
	 * — javap-verified constructor twins on both legs). The {@code energy} field is
	 * protected on BOTH legs, so the setStoredFe write face is byte-identical per leg.
	 */
	//? if forge {
	private static final class MutableEnergyStorage extends net.minecraftforge.energy.EnergyStorage {
		private MutableEnergyStorage(int aCapacity) { super(aCapacity, 0, aCapacity); }
		private void setStoredFe(int aEnergy) { this.energy = Math.max(0, Math.min(capacity, aEnergy)); }
	}
	//?} else {
	/*private static final class MutableEnergyStorage extends net.neoforged.neoforge.energy.EnergyStorage {
		private MutableEnergyStorage(int aCapacity) { super(aCapacity, 0, aCapacity); }
		private void setStoredFe(int aEnergy) { this.energy = Math.max(0, Math.min(capacity, aEnergy)); }
	}
	 *///?}

	private final MutableEnergyStorage mStorage = new MutableEnergyStorage(CAPACITY);
	//? if forge {
	private final LazyOptional<net.minecraftforge.energy.IEnergyStorage> mCap = LazyOptional.of(() -> mStorage);
	//?}

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6FeSourceBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GT6FeSourceBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6FeBatteries.FE_SOURCE_BE.get(), aPos, aState);
	}

	/** The stored FE, straight from the reference implementation (the /gt6fesource stat face). */
	public int storedFe() {
		return mStorage.getEnergyStored();
	}

	/** The set/reset write face (the protected-field subclass, the sink battery's resetStoredFe twin). */
	public void setStoredFe(int aEnergy) {
		mStorage.setStoredFe(aEnergy);
		setChanged();
	}

	/** The storage face the 21.1 wiring provider and the offline tests read. */
	//? if forge {
	public net.minecraftforge.energy.EnergyStorage energyStorage() {
		return mStorage;
	}
	//?} else {
	/*public net.neoforged.neoforge.energy.IEnergyStorage energyStorage() {
		return mStorage;
	}
	 *///?}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		// the @AutoRegisterCapability face — no registration event on this leg (the sink
		// battery doc); invalidation rides the Forge-patched setRemoved().
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
	// NBT (the stored FE; the hook signature fork, the sink battery record shape)
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
