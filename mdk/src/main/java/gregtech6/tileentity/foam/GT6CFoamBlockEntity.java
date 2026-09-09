package gregtech6.tileentity.foam;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.foam.GT6CFoamOwnedBlock;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The owned (player-placed) C-Foam TE — the 1.20.1 counterpart of upstream
 * {@code MultiTileEntityCFoam} (gregtech/tileentity/misc/MultiTileEntityCFoam.java:52-155,
 * MTE id 32765). The upstream class extends {@code TileEntityBase07Paintable}; the port's
 * {@link TileEntityBase03TicksAndSync} carries the SAME paintable stratum (the P21 fold),
 * so {@code gt.color}/{@code gt.painted} ride the base verbatim.
 *
 * <p>Upstream traits kept verbatim:
 * <ul>
 * <li>the state pair {@code mFoamDried}/{@code mOwnable} (:53) + the owner UUID;</li>
 * <li>NBT: {@code gt.foamdried}/{@code gt.ownable} written UNCONDITIONALLY (:66-67), the
 *     owner only while set (:68) — the four-key family with the base's paint keys. The
 *     owner VALUE is the vanilla {@code putUUID} int-array form (the pipe's declared
 *     deviation from the upstream String form). The ITEM NBT half (:72-76 — the first two
 *     keys riding the block item) has no carrier here: the owned block registers NO
 *     BlockItem (upstream {@code showInCreative} false, :152) — declared cut;</li>
 * <li>the drying ticker :97-104 — server side, matured ({@code aTimer >= 100}), a
 *     1/5900 per-tick roll ({@link #DRY_MIN_AGE}/{@link #DRY_RNG_BOUND}), then
 *     {@code updateClientData}; the roll rides the {@link #rng} injection seam (the
 *     pipe :279 form); the port additionally flips the block's DRIED property (the
 *     spec_rulings.ruling_color_dim dried-visual carrier);</li>
 * <li>the THREE-CLAUSE ownership gate :113-115 — {@code !mOwnable || !mFoamDried ||
 *     super}, the {@code super} core unwrapped to the UUID form exactly like the pipe
 *     {@code allowInteraction} (a wet or unowned foam passes everyone; only a DRIED owned
 *     foam arms the lock, and a null owner passes everyone);</li>
 * <li>{@code dryFoam} :118-123 — NO allowInteraction gate (the upstream asymmetry: anyone
 *     may harden, only the owner may remove);</li>
 * <li>{@code removeFoam} :126-130 — gated, then the position becomes air;</li>
 * <li>{@code applyFoam} :149 constant false (the foam does not accept more foam);</li>
 * <li>the dried light swap :134 ({@code LIGHT_OPACITY_MAX : LIGHT_OPACITY_WATER}) rides
 *     the block's {@code getLightBlock} over the DRIED property (no BE lookup needed);
 *     the hardness delegation :136-137 rides the block's {@code getDestroySpeed};</li>
 * <li>the four-state texture :132 rides the blockstate DRIED variant pair
 *     (fresh/hardened × the owned sprite pair) + the paint tint (the ruling_color_dim
 *     owned_render form — the PipeFoamSnapshot stays untouched, P25 frozen).</li>
 * </ul>
 *
 * <p>The upstream {@code onPlaced} :107-110 owner record folds into the spray write point:
 * the owned block has no BlockItem, so the ONLY placement route is the spray arm, whose
 * {@link #configureSpray} carries the NBT bundle of the upstream static {@code setBlock}
 * (:81-83 — colour/painted/ownable/owner in one write).
 */
public class GT6CFoamBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityFoamable {

	/** Upstream NBT_FOAMDRIED = "gt.foamdried" (read :58, written :66/:73). */
	public static final String NBT_FOAMDRIED = "gt.foamdried";

	/** Upstream NBT_OWNABLE = "gt.ownable" (read :59, written :67/:74). */
	public static final String NBT_OWNABLE = "gt.ownable";

	/** Upstream NBT_OWNER = "gt.owner" (read :60, written :68) — the vanilla putUUID value form. */
	public static final String NBT_OWNER = "gt.owner";

	/** The drying gate's minimum age (upstream :100 {@code aTimer >= 100}). */
	public static final int DRY_MIN_AGE = 100;

	/** The drying roll bound (upstream :100 {@code rng(5900) == 0} — mean ~295 s). */
	public static final int DRY_RNG_BOUND = 5900;

	/** Upstream :53 {@code mFoamDried = F}. */
	public boolean mFoamDried = false;

	/** Upstream :53 {@code mOwnable = F}. */
	public boolean mOwnable = false;

	/** Upstream :60/:68/:108 — null = unowned. */
	@Nullable
	public UUID mOwner = null;

	public GT6CFoamBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
	}

	/**
	 * The BET factory for {@code BlockEntityType.Builder.of} — resolves the type through the
	 * registry at runtime (no supplier self-reference; the TestMachineBlockEntity.java:28 shape).
	 */
	public GT6CFoamBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6FoamBlocks.CFOAM_OWNED_BE.get(), aPos, aState);
	}

	/** The BET registry path mirror (the TestMachineBlockEntity#getTileEntityName form — upstream :154 "id" write). */
	@Override
	public String getTileEntityName() {
		return "cfoam_owned";
	}

	// -------------------------------------------------------------------------
	// the spray write point (the upstream static setBlock :81-83 NBT bundle)
	// -------------------------------------------------------------------------

	/**
	 * The one write point the spray arm drives after landing the owned block: the paint pair
	 * (upstream {@code NBT_COLOR + NBT_PAINTED = T} of :82 — the base's {@code paint} face,
	 * which bundles setChanged + sync + model-data refresh) plus the ownable/owner pair
	 * ({@code NBT_OWNABLE = aOwned, NBT_OWNER = aOwned && aPlayer != null ? ... : null}).
	 */
	public void configureSpray(int aRGB, boolean aOwned, @Nullable UUID aSprayer) {
		paint(aRGB); // the NBT_COLOR + NBT_PAINTED pair of upstream :82
		mOwnable = aOwned;
		mOwner = aOwned && aSprayer != null ? aSprayer : null;
		setChanged();
		updateClientData();
	}

	// -------------------------------------------------------------------------
	// the drying ticker (upstream :97-104 verbatim + the DRIED property leg)
	// -------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (aIsServerSide && aTimer >= DRY_MIN_AGE && !mFoamDried && rng(DRY_RNG_BOUND) == 0) {
			setDried(); // upstream :101-102 mFoamDried = T + updateClientData (+ the port property leg)
		}
	}

	/**
	 * The uniform random roll (the pipe :279 form) — {@code protected} so the offline tests
	 * inject a deterministic roll; level-less BEs roll 0 (deterministic either way).
	 */
	protected int rng(int aBound) {
		return hasLevel() ? getLevel().random.nextInt(aBound) : 0;
	}

	/** The dried flip: the field + sync (upstream :101-102) + the block's DRIED property leg. */
	private void setDried() {
		mFoamDried = true;
		updateClientData();
		BlockState tState = getBlockState();
		if (hasLevel() && isServerSide() && tState.hasProperty(GT6CFoamOwnedBlock.DRIED)
				&& !tState.getValue(GT6CFoamOwnedBlock.DRIED)) {
			getLevel().setBlock(getBlockPos(), tState.setValue(GT6CFoamOwnedBlock.DRIED, true), 3);
		}
	}

	// -------------------------------------------------------------------------
	// the THREE-CLAUSE ownership gate (upstream :113-115, the pipe :145-147 form)
	// -------------------------------------------------------------------------

	/**
	 * {@code !mOwnable || !mFoamDried || super.allowInteraction} verbatim: a wet or unowned
	 * foam passes EVERYONE, a dried owned foam locks to the owner, a null owner keeps the
	 * position open to everyone. The UUID unwrap is the offline discipline (no Entity).
	 */
	public boolean allowInteraction(@Nullable UUID aUUID) {
		return !mOwnable || !mFoamDried || mOwner == null || (aUUID != null && mOwner.equals(aUUID));
	}

	// -------------------------------------------------------------------------
	// the ITileEntityFoamable face (upstream :118-130/:146-149)
	// -------------------------------------------------------------------------

	@Override
	public boolean applyFoam(byte aSide, @Nullable UUID aPlayer, int aRGB, boolean aOwnedFoam) {
		return false; // upstream :149 verbatim — the foam does not accept more foam
	}

	@Override
	public boolean dryFoam(byte aSide, @Nullable UUID aPlayer) {
		if (mFoamDried || isClientSide()) return false; // upstream :119 verbatim
		setDried(); // upstream :120-122 mFoamDried = T + updateClientData (+ the port property leg)
		return true;
	}

	@Override
	public boolean removeFoam(byte aSide, @Nullable UUID aPlayer) {
		if (isClientSide() || !allowInteraction(aPlayer)) return false; // upstream :127 verbatim
		getLevel().removeBlock(getBlockPos(), false); // upstream :128 setBlock NB, 3
		return true;
	}

	@Override
	public boolean hasFoam(byte aSide) {
		return true; // upstream :146
	}

	@Override
	public boolean driedFoam(byte aSide) {
		return mFoamDried; // upstream :147
	}

	@Override
	public boolean ownedFoam(byte aSide) {
		return mOwnable; // upstream :148
	}

	// -------------------------------------------------------------------------
	// the NBT persistence (upstream :56-69 — the four-key family)
	// -------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_FOAMDRIED, mFoamDried); // upstream :66 — unconditional (UT.NBT.setBoolean)
		aNBT.putBoolean(NBT_OWNABLE, mOwnable); // upstream :67 — unconditional
		if (mOwner != null) aNBT.putUUID(NBT_OWNER, mOwner); // upstream :68 — only while set; the UUID value form
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FOAMDRIED)) mFoamDried = aNBT.getBoolean(NBT_FOAMDRIED); // upstream :58
		if (aNBT.contains(NBT_OWNABLE)) mOwnable = aNBT.getBoolean(NBT_OWNABLE); // upstream :59
		if (aNBT.hasUUID(NBT_OWNER)) mOwner = aNBT.getUUID(NBT_OWNER); // upstream :60 (OWNERSHIP_RESET folded, the p24 deviation)
	}
}
