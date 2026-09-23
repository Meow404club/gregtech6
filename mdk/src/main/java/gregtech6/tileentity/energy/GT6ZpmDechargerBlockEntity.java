package gregtech6.tileentity.energy;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GT6ZpmDechargerBlock;
import gregtech6.covers.CoverData;
import gregtech6.covers.ICoverableTE;
import gregtech6.item.energy.GT6ZpmItem;
import gregtech6.tileentity.GTItemStackHandler;

/**
 * The ZPM Decharger BE (task p36-energy-zpm-dechargers) — the thin shell over
 * {@link GT6BatteryBoxBlockEntity} that upstream files as
 * {@code MultiTileEntityZPMDechargerEU/QU} (gregtech/tileentity/energy/storage/:35-:66, the
 * texture-only subclasses of {@code TileEntityBase10EnergyBatBox} — the exact reuse posture
 * the Crystal Charger card declared, one step deeper): the BatteryBox transcription with
 * THREE upstream columns the EU/LU rows fold away, now restored.
 *
 * <h2>The split type lanes (the Base10 :54-:55/:67-:68/:202 restoration)</h2>
 * Both Loader rows (:1000-:1001) carry {@code NBT_ENERGY_ACCEPTED QU} (the in lane —
 * the packet language the ZPM speaks) and {@code NBT_ENERGY_EMITTED QU|EU} (the out lane —
 * the Base10 :55 {@code mEnergyTypeOut} seat). The port box rows are all EU/EU so the
 * BatteryBox folded the pair into one field (its :338 comment); this shell re-splits:
 * {@link #mEnergyType} stays the in/accepted/capacitor lane (resolved QU off the block),
 * {@link #mEnergyTypeOut} the emit lane (EU on the Electric decharger — the EU-ladder
 * feed, QU on the Quantum one), and {@link #isEnergyType} rides the :202 verbatim split
 * {@code aEnergyType == (aEmitting ? mEnergyTypeOut : mEnergyType)}.
 *
 * <h2>The ZPM slot gate (the :36-:37 verbatim)</h2>
 * {@code isItemValidForSlot/canInsertItem2 → IL.ZPM.equal(aStack, F, T)} — the ONLY
 * inventory carrier is the ZPM (the positive pin: it enters; the negative pins: the EU
 * and LU battery ladders are the same IItemEnergy language and still fail the gate).
 * The charge arms of the battery phase (:113-:114 push) call through to the ZPM's
 * {@code canEnergyInjection=F} and drain 0 — the box only ever DISCHARGES its ZPM into
 * the buffer, and the :179 intake guard ({@code mReceivablePower} = chargeable-count ×
 * size) reads 0, so the decharger refuses network intake exactly like upstream: a
 * one-way artifact→machine lane.
 *
 * <h2>The cover host (the capacitor-face arming, the card acceptance ⑤)</h2>
 * Upstream the display/scale energy covers admit the {@code ITileEntityEnergyDataCapacitor}
 * hosts — THE BATTERY BOXES. The port's covers admitted only the two machine forms (the
 * CoverMachineLanes declared deviation; the cover6_display_energy chain documented the
 * non-zero face as structurally unreachable "until a coverable capacitor host exists").
 * This BE IS that host: it carries the {@link ICoverableTE} composition (the mCovers
 * store, the :189-:221 tick dispatch, the covers NBT — the oven/pipe shape) and the
 * lane read maps {@code mEnergy}/{@code capacity()} (the :215-:216 progress pair —
 * the internal buffer, NOT the inventory sum).
 *
 * <p>NBT: the base carriers + the {@code covers} tag (the ICoverableTE write/read pair).
 * KJS surface: none (registration face deferred — the KJS binding pool).
 */
public class GT6ZpmDechargerBlockEntity extends GT6BatteryBoxBlockEntity implements ICoverableTE {

	/** The emit lane (the Base10 :55 seat; in = {@link #mEnergyType} off the block column). */
	public final TagData mEnergyTypeOut;

	/** The cover store — {@code null} while no face carries a cover (the oven :207 form). */
	public CoverData mCovers = null;

	@Override
	public CoverData getCovers() {
		return mCovers;
	}

	@Override
	public void setCovers(CoverData aCoverData) {
		mCovers = aCoverData;
	}

	/** The BET factory (the BlockEntityType.Builder.of seat; the row data resolves off the block state). */
	public GT6ZpmDechargerBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The runtime entry — the out lane resolves off the {@link GT6ZpmDechargerBlock} column. */
	public GT6ZpmDechargerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		this(aType, aPos, aState,
				aState.getBlock() instanceof GT6ZpmDechargerBlock tDech ? tDech.outEnergyType().get() : TD.Energy.QU);
	}

	/** The offline fixture entry — the :1000/:1001 row shape is fixed (V[7], 1 slot) + the explicit out lane. */
	public GT6ZpmDechargerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState, TagData aOutType) {
		super(aType, aPos, aState, 7, 1); // NBT_INPUT/NBT_OUTPUT V[7] = 131072, NBT_INV_SIZE 1
		mEnergyTypeOut = aOutType;
		// the :36-:37 slot gate — ONLY the ZPM (the base IItemEnergy-type gate is too wide here)
		setInventory(new GTItemStackHandler(slots()) {
			@Override
			public boolean isItemValid(int aSlot, ItemStack aStack) {
				return aStack.getItem() instanceof GT6ZpmItem;
			}

			@Override
			public int getSlotLimit(int aSlot) {
				return 1; // the :218 getInventoryStackLimit verbatim
			}
		});
	}

	@Override
	public String getTileEntityName() {
		return "zpm_decharger"; // the BET registry path
	}

	// ---------------------------------------------------------------------------
	// the split type face (the Base10 :202/:212 verbatim — the fold restoration)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEnergyType == (aEmitting ? mEnergyTypeOut : mEnergyType); // :202
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return List.of(mEnergyType, mEnergyTypeOut); // :212 (the two-lane pair)
	}

	/** The emit lane (the doEmit seat — the base folds to mEnergyType, the :145 verbatim reads the out lane). */
	@Override
	protected TagData emitType() {
		return mEnergyTypeOut;
	}

	// ---------------------------------------------------------------------------
	// the cover lifecycle (the pipe/oven dispatch shape, 06Covers :189-:221)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide) {
			// upstream 06Covers :191 — the validity sweep before the machine business
			checkCoverValidity();
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		// upstream 06Covers :200 — the cover tick precedes the machine business
		if (hasCovers()) getCovers().tickPre(aTimer, aIsServerSide, mBlockUpdated, false);
		super.onTick(aTimer, aIsServerSide);
		// upstream 06Covers :202 — the cover tick follows the machine business
		if (hasCovers()) getCovers().tickPost(aTimer, aIsServerSide, mBlockUpdated, false);
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// the cover visual sync window (the :184-186 pair)
		return (hasCovers() && getCovers().requiresSync()) || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickChecked(long aTimer) {
		super.onTickChecked(aTimer);
		if (hasCovers()) getCovers().resetSync();
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		writeCoversToNBT(aNBT); // the 06Covers :74 covers tag
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		readCoversFromNBT(aNBT); // the 06Covers :68 covers tag
	}
}
