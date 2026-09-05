package gregtech6.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBlockProperties;
import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTWires;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.util.UT6;

/**
 * The GT6 electric wire block (task p7-d2-cable spec ④) — the block side of the wire
 * family over the shared BET (ADR-P3-1), mirroring the GTFluidPipeBlock shape. The
 * carrier pattern (the GTBarrelBlock registration-carrier precedent): the block carries
 * the wire ratings upstream wrote into the MTE definition NBT — the {@code NBT_PIPESIZE}
 * voltage, the {@code NBT_PIPEBANDWIDTH} amperage and the {@code NBT_PIPELOSS} per-segment
 * loss (MultiTileEntityWireElectric.java:114-116 read them back in readFromNBT2). Since task
 * p9-wire-family-w1 the block additionally carries the row identity the W2 render card consumes:
 * the {@link OreDictMaterial} (may be null on the two material-less p7 legacy blocks), the
 * band size {@code n} (the "1x..16x" multiplier) and the {@code insulated} form flag — the
 * ratings (voltage/amperage/loss/diameter/max-stack) still come from the registration site
 * (GTWireSpecs.Variant, the direct addElectricWires :71-109 transcription).
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask as a BlockState property — the
 * visual counterpart of {@code TileEntityBase09Connector.mConnections}, written by the
 * wire BlockEntity on every connection change (GTWireBlockEntity.onConnectionChange,
 * the pipe twin). Since p9-wire-family-w1 the datagen blockstate maps every mask to the one
 * placeholder model through a single property-less variant (the ModelBakery.java:173 empty-key
 * wildcard) — the connection-aware model picking is the W2 card's BakedModel.
 *
 * <p>{@code use} is a SEMANTIC LOCK (task p9-wire-family-w1 spec ④, promoted from the
 * accidental base default to an explicit override): it always returns
 * {@link InteractionResult#PASS}. Upstream wires have no vanilla right-click action — the
 * connection-management tool is the cutter ({@code getFacingTool() = TOOL_cutter},
 * MultiTileEntityWireElectric.java:245), which travels the IBlockToolable chain
 * (gregapi/block/IBlockToolable.java:81), NOT the vanilla use chain; the vanilla right-click
 * with any held item must fall through ("nothing happens", the upstream
 * TileEntityBase06Covers.onBlockActivated2 :106-130 outcome for GUI-less wires). Never turn
 * this into a connect/disconnect toggle — that is the W3 cutter card's surface, and until it
 * lands {@code /gt6wire connect} is the driver. NO onRemove override — the BaseEntityBlock
 * kill+recreate lesson (remember id59).
 */
public class GTWireBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — bit i = side i connected (GT6 side order) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final IntegerProperty CONNECTIONS = GTBlockProperties.CONNECTIONS;

	private final long mVoltage;
	private final long mAmperage;
	private final long mLoss;
	private final OreDictMaterial mMaterial;
	private final int mSize;
	private final boolean mInsulated;
	private final int mDiameter;
	private final Family mFamily;
	private final boolean mContactDamage;
	private final boolean mLuminous;

	/**
	 * The upstream :219 contact-damage collision box — the 2px inset
	 * {@code box(PX_P[2], PX_P[2], PX_P[2], PX_N[2], PX_N[2], PX_N[2])}
	 * (PX_P[2] = 2/16 = 0.125, PX_N[2] = 14/16 = 0.875). It is the ENABLER of the whole
	 * shock mechanic: 1.20.1 {@code Entity.checkInsideBlocks} (Entity.java:966-989) fires
	 * {@code entityInside} for every block volume the entity AABB overlaps, and an entity
	 * standing ON a full cube overlaps nothing — with the inset box the entity sinks the
	 * 2px into the wire cell, the overlap exists, and the hook runs (upstream feeds the
	 * same inset through IMTE_GetCollisionBoundingBoxFromPool for exactly that reason).
	 */
	private static final VoxelShape CONTACT_SHAPE = Shapes.box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);

	/**
	 * The p7 legacy form (the two material-less variants) — the vanilla-block fallback ratings
	 * (the upstream field defaults :64) with no row identity.
	 */
	public GTWireBlock(long aVoltage, long aAmperage, long aLoss, Properties aProperties) {
		this(aVoltage, aAmperage, aLoss, null, 1, false, 0, aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTWireBlock> codec() {
		return simpleCodec(aProperties -> new GTWireBlock(32, 1, 1, aProperties));
	}
	*///?}

	/**
	 * The full W1 carrier (task p9-wire-family-w1 spec ②) — the electric family form.
	 *
	 * @param aVoltage the packet size ceiling in EU (upstream NBT_PIPESIZE)
	 * @param aAmperage the packet count ceiling (upstream NBT_PIPEBANDWIDTH)
	 * @param aLoss the EU lost per segment (upstream NBT_PIPELOSS)
	 * @param aMaterial the row material (the W2 tint anchor; null on the legacy pair)
	 * @param aSize the band multiplier n of "nx" (upstream wireGt01..16 / cableGt01..12)
	 * @param aInsulated false = bare wire (renderType 0), true = insulated cable (renderType 1)
	 * @param aDiameter the upstream PX_P diameter index (NBT_DIAMETER, W2 geometry seed)
	 */
	public GTWireBlock(long aVoltage, long aAmperage, long aLoss, @Nullable OreDictMaterial aMaterial,
			int aSize, boolean aInsulated, int aDiameter, Properties aProperties) {
		this(aVoltage, aAmperage, aLoss, aMaterial, aSize, aInsulated, aDiameter, Family.ELECTRIC, aProperties);
	}

	/**
	 * The family carrier (task p10-wire-redstone-family): one extra column over the W1
	 * form. A REDSTONE-family block is the same visual carrier (the CONNECTIONS mask
	 * stays THE ONLY BlockState payload — spec 3 red line: the signal VALUE lives on the
	 * BlockEntity, a POWER-style state property would be the 64×16 variant explosion the
	 * research card bans) while its BE mounts the push-BFS redstone semantics instead of
	 * the EU pump. {@code aVoltage/aAmperage} sit at 0/1 on redstone rows (the EU face
	 * family is gated off at the BE); {@code aLoss} is the upstream NBT_PIPELOSS
	 * (MAX_RANGE/16|/64).
	 */
	public GTWireBlock(long aVoltage, long aAmperage, long aLoss, @Nullable OreDictMaterial aMaterial,
			int aSize, boolean aInsulated, int aDiameter, Family aFamily, Properties aProperties) {
		super(aProperties);
		mVoltage = aVoltage;
		mAmperage = aAmperage;
		mLoss = aLoss;
		mMaterial = aMaterial;
		mSize = aSize;
		mInsulated = aInsulated;
		mDiameter = aDiameter;
		mFamily = aFamily;
		mContactDamage = contactDamageOf(aFamily, aInsulated, aMaterial);
		mLuminous = luminousOf(aFamily, aMaterial);
		registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
	}

	/** The voltage ceiling in EU (upstream NBT_PIPESIZE). */
	public long voltageL() {
		return mVoltage;
	}

	/** The amperage ceiling (upstream NBT_PIPEBANDWIDTH). */
	public long amperageL() {
		return mAmperage;
	}

	/** The per-segment loss in EU (upstream NBT_PIPELOSS). */
	public long lossL() {
		return mLoss;
	}

	/** The row material (W2 tint anchor; null on the two material-less p7 legacy blocks). */
	@Nullable
	public OreDictMaterial material() {
		return mMaterial;
	}

	/** The band multiplier n of "nx" (upstream wireGt01..16 / cableGt01/02/04/08/12). */
	public int size() {
		return mSize;
	}

	/** False = bare wire (upstream NBT_PIPERENDER 0), true = insulated cable (NBT_PIPERENDER 1). */
	public boolean insulated() {
		return mInsulated;
	}

	/** The upstream PX_P diameter index (NBT_DIAMETER — the W2 geometry seed, W1 data-only). */
	public int diameter() {
		return mDiameter;
	}

	/** The family column (task p10): ELECTRIC = the EU pump rows, REDSTONE = the push-BFS signal rows. */
	public Family family() {
		return mFamily;
	}

	/**
	 * The spec contact-damage flag (the upstream {@code NBT_CONTACTDAMAGE} registration data,
	 * read into {@code TileEntityBase10ConnectorRendered.mContactDamage} :64). Derived at
	 * construction from the {@link GTWireSpecs} row the block carries (see
	 * {@link #contactDamageOf}); TRUE only on the BARE wires of the 28 shock-flagged rows —
	 * insulated cables ({@code contactDamageCable = F} on every row), the Graphene and
	 * Superconductor pure wires and every non-ELECTRIC row are inert.
	 */
	public boolean contactDamage() {
		return mContactDamage;
	}

	/**
	 * The {@code NBT_CONTACTDAMAGE} derivation. The registration carrier ({@code GTWires})
	 * builds every block from the {@link GTWireSpecs.Variant} record, which has no flag
	 * column — the flag is instead re-derived from the block's row identity here, the
	 * upstream mapping being: wires take {@code aContactDamageWire} (:72-87), cables take
	 * {@code aContactDamageCable} (:89-93), both F on the Graphene/Superconductor rows
	 * (Loader:1948/:1950) and F on every redstone row (no NBT_CONTACTDAMAGE exists on the
	 * Loader:1893-1902 registration at all). Blocks without row identity (the material-less
	 * p7 legacy pair) carry the upstream FIELD default F — upstream reads the flag only
	 * when present ({@code if (aNBT.hasKey(NBT_CONTACTDAMAGE))}, :64) and the field
	 * initialises to F (:57), so a registration without the flag is an inert wire.
	 */
	private static boolean contactDamageOf(Family aFamily, boolean aInsulated, @Nullable OreDictMaterial aMaterial) {
		if (aFamily != Family.ELECTRIC || aMaterial == null) return false;
		for (GTWireSpecs.Row tRow : GTWireSpecs.ROWS) {
			if (tRow.material().get() == aMaterial) return aInsulated ? tRow.contactDamageCable() : tRow.contactDamageWire();
		}
		return false;
	}

	/**
	 * The upstream material GLOWING attribute (the wirelamp data pin, task p10 spec 8):
	 * {@code mIsGlowing = material.contains(TD.Properties.GLOWING)} (TileEntityBase10ConnectorRendered
	 * :69) — only the Lumium row carries it (MT.java:1792; RedAlloy/Signalum do not), so only
	 * the "Lumium Wirelamp" registration (Loader:1901) is a light source. Derived from the
	 * {@link GTWireSpecs#REDSTONE_ROWS} table like {@link #contactDamageOf} (GTWires builds this
	 * block from the same rows — the constructor stays untouched, no new carrier column).
	 * The emission itself lives in {@link #getLightEmission}; the change chain lives on the BE
	 * ({@code GTWireBlockEntity.onTickCheck} → {@code refreshGlowLight}).
	 */
	public boolean luminous() {
		return mLuminous;
	}

	/** The {@code material GLOWING} derivation — redstone rows only, matched by material identity. */
	private static boolean luminousOf(Family aFamily, @Nullable OreDictMaterial aMaterial) {
		if (aFamily != Family.REDSTONE || aMaterial == null) return false;
		for (GTWireSpecs.Row tRow : GTWireSpecs.REDSTONE_ROWS) {
			if (tRow.material().get() == aMaterial) return tRow.luminous();
		}
		return false;
	}

	/**
	 * The upstream mBlockUpdated bridge (MultiTileEntityWireRedstoneInsulated :103 —
	 * {@code if (mBlockUpdated) updateConnectionStatus()}): vanilla delivers a neighbour
	 * change here and the BE consumes the flag in its next server tick (the vanilla-side
	 * trigger of the connection re-scan; the BFS triggers themselves are the connection
	 * change :94 and the per-tick convergence :104). The 1.7.10 MTE block fed the same
	 * flag from onNeighborBlockChange.
	 *
	 * <p>Since task p11-connector-stale-mask this hook ALSO runs the synchronous
	 * connection-mask rescan ({@link GTWireBlockEntity#validateConnections}) — the flag-1
	 * half of the two delivery channels (this one and {@link #updateShape}; see there for
	 * why both exist). Player break/place arrives through here; deferring the prune to the
	 * BE tick alone would strand the fix on any loaded-but-not-entity-ticking chunk.
	 */
	@Override
	public void neighborChanged(BlockState aState, Level aLevel, BlockPos aPos, Block aNeighborBlock, BlockPos aFromPos, boolean aIsMoving) {
		super.neighborChanged(aState, aLevel, aPos, aNeighborBlock, aFromPos, aIsMoving);
		if (aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire) {
			tWire.validateConnections(); // p11 — the stale-bit prune, synchronous
			tWire.markBlockUpdated();
		}
	}

	/**
	 * The second delivery channel of the p11 stale-mask prune — and the one that alone sees
	 * EVERY state-level neighbour change: vanilla {@code /setblock} (and {@code /fill})
	 * place with flags=2 ONLY (SetBlockCommand.java:97 → BlockInput.place(ServerLevel,
	 * BlockPos, 2)), so the flag-1 {@code neighborChanged} arm of markAndNotifyBlock
	 * (Level.java:230-233, {@code blockUpdated}) never fires — while the shape-update arm
	 * ({@code ($$2 & 16) == 0 && $$3 > 0}, Level.java:238-242) DOES. That arm lands here.
	 * The vanilla precedent for maintaining a connection property on exactly this seam is
	 * RedStoneWireBlock.updateShape :187-198 (recalculates its sides from the neighbour).
	 *
	 * <p>The override stays read-only itself and delegates the mask decision to the BE
	 * ({@link GTWireBlockEntity#validateConnections} — prune-only, never auto-reconnect, so
	 * a cutter/manual disconnect can never be resurrected by a neighbour change); the
	 * {@code markBlockUpdated} flag additionally arms the deferred per-tick re-check (the
	 * upstream MultiTileEntityWireRedstoneInsulated :103 consumption). The vanilla neighbour
	 * updater (1.19.3+ queue, the max-chained-neighbor-updates cap) contains any setBlock
	 * cascade the prune's own state write produces.
	 */
	@Override
	public BlockState updateShape(BlockState aState, Direction aDirection, BlockState aNeighborState,
			LevelAccessor aLevel, BlockPos aPos, BlockPos aNeighborPos) {
		if (!aLevel.isClientSide() && aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire) {
			tWire.validateConnections(); // p11 — fires for /setblock, where neighborChanged (flag 1) does not
			tWire.markBlockUpdated();
		}
		return super.updateShape(aState, aDirection, aNeighborState, aLevel, aPos, aNeighborPos);
	}

	/**
	 * The explicit PASS lock (spec ④): non-tool right-clicks are never consumed — the wire has
	 * no GUI and the connection tool is the cutter on the IBlockToolable chain, not use. See
	 * the class javadoc; do not override again.
	 */
	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer,
			InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer,
			BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(javap BlockBehaviour 21.1.249).
	*///?}
		return InteractionResult.PASS;
	}

	// ---------------------------------------------------------------------------
	// the redstone emission bridge (task p10-wire-redstone-family spec 4)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream isProvidingWeakPower :140-145 via the BE body (weak == strong, the :140-152
	 * ONE-body rule). The vanilla query direction is the side the RECEIVER sees the wire
	 * from (the GTOvenBlock.bridgeSignal :88-96 / ICoverableTE.redstoneOut :397-399
	 * convention — upstream getWeakPower's side parameter, verbatim); the BE flips it to
	 * the emission face with OPOS and applies the neighbour correction there. Electric
	 * rows never reach the redstone body ({@code isRedstone()} gates it to 0 — they have
	 * no redstone semantics upstream either, the vanilla Block default 0 rides in).
	 */
	@Override
	public int getSignal(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		if (aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire && tWire.isRedstone()) {
			return tWire.getRedstoneOut((byte)aDirection.get3DDataValue(), false);
		}
		return super.getSignal(aState, aLevel, aPos, aDirection);
	}

	/** Upstream isProvidingStrongPower :147-152 — the same bridge, strong form (same body upstream). */
	@Override
	public int getDirectSignal(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		if (aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire && tWire.isRedstone()) {
			return tWire.getRedstoneOut((byte)aDirection.get3DDataValue(), true);
		}
		return super.getDirectSignal(aState, aLevel, aPos, aDirection);
	}

	/**
	 * Upstream getComparatorInputOverride :155-157 — the comparator reads
	 * {@code bind4(mRedstone / MAX_RANGE)} (floor division, the upstream literal).
	 */
	@Override
	public int getAnalogOutputSignal(BlockState aState, Level aLevel, BlockPos aPos) {
		if (aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire && tWire.isRedstone()) {
			return tWire.getComparatorOut();
		}
		return super.getAnalogOutputSignal(aState, aLevel, aPos);
	}

	// ---------------------------------------------------------------------------
	// the contact-damage hook (task p10-wire-contact-damage spec 1)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream MultiTileEntityWireElectric.onEntityCollidedWithBlock :203 —
	 * {@code if (mContactDamage && !mFoamDried) UT.Entities.applyElectricityDamage(aEntity,
	 * mWattageLast)} — relocated from the TE to the 1.20.1 Block hook
	 * ({@code Entity.checkInsideBlocks} Entity.java:966-989 drives it for every overlapped
	 * block volume, both sides; the client guard lives below). The {@code mFoamDried}
	 * exemption is excised along with the foam system (the C-Foam ADR — foam is a declared pool item,
	 * so no dried-foam state can exist to gate on). The family gate rides
	 * {@link #mContactDamage} (false on every non-ELECTRIC row) plus the BE-side
	 * {@code isRedstone()} re-gate in {@link GTWireBlockEntity#applyElectricityDamage}.
	 */
	@Override
	public void entityInside(BlockState aState, Level aLevel, BlockPos aPos, Entity aEntity) {
		super.entityInside(aState, aLevel, aPos, aEntity);
		if (!mContactDamage || aLevel.isClientSide) return;
		if (aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire) {
			tWire.applyElectricityDamage(aEntity); // upstream :203 — the wattage gate lives on the BE
		}
	}

	/**
	 * Upstream TileEntityBase10ConnectorRendered.getCollisionBoundingBoxFromPool :219 —
	 * {@code mContactDamage ? box(PX_P[2]..PX_N[2]) : super} (the {@code !mFoamDried}
	 * half is excised along with foam). The 2px inset is what lets an entity sink into the wire cell
	 * so {@link #entityInside} actually fires while standing on the wire (see
	 * {@link #CONTACT_SHAPE}); inert wires keep the plain full cube.
	 */
	@Override
	public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return mContactDamage ? CONTACT_SHAPE : super.getCollisionShape(aState, aLevel, aPos, aContext);
	}

	// ---------------------------------------------------------------------------
	// the light emission (task p11-wire-brightness spec 1 — the wirelamp)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream MultiTileEntityWireRedstone :79 — {@code getLightValue() = mIsGlowing ? mState : 0}
	 * with {@code mState = bind4(divup(mRedstone, MAX_RANGE))} (:53), read through the BE the same
	 * way the 1.7.10 light engine polled it (MultiTileEntityBlock.java:212,
	 * {@code bind4(TE.getLightValue())}). The 1.20.1 landing is THIS override — the
	 * level-sensitive {@code IForgeBlock.getLightEmission(state, level, pos)} (IForgeBlock.java
	 * :113, the official {@code LevelSensitiveLightBlockTest} :82-89 form: the BlockState grows no
	 * property, the value lives on the BlockEntity) — and the CHANGE side moves from the upstream
	 * {@code updateLightValue} (TileEntityBase01Root :549-554, own cell + six neighbours re-lit) to
	 * {@code level.getLightEngine().checkBlock(pos)}, driven by the BE wherever {@code mState}
	 * changes ({@code GTWireBlockEntity.onTickCheck} :51-58 server, {@code load()} client — the
	 * upstream {@code setVisualData} :62-67 landing; the Forge patches route the whole light
	 * engine through the level-sensitive getter, BlockLightEngine.java.patch:8/:17).
	 *
	 * <p>WORKER-THREAD CONTRACT: the getter "may be called on a worker thread" (IForgeBlock.java
	 * :106-110 — the 1.20.1 server light tasks run on the ChunkMap workers), so the BE probe MUST
	 * go through {@code getExistingBlockEntity} (IForgeBlockGetter.java:30-50 — skips the
	 * {@code Level#getBlockEntity} promote-on-access path, exactly what the official sample's
	 * {@code level.getExistingBlockEntity(pos)} :84 does); an {@code ImposterProtoChunk} sampled
	 * at the light-engine boundary would otherwise race the chunk promotion.
	 *
	 * <p>The gates are the upstream CLASS split, verbatim: the bare wire class implements
	 * {@code IMTE_GetLightValue} (MultiTileEntityWireRedstone :35/:79) while its insulated parent
	 * (MultiTileEntityWireRedstoneInsulated) does NOT — a glowing MATERIAL on the cable form
	 * (the Lumium Cable, Loader:1902) stays dark upstream and stays dark here. So: REDSTONE
	 * family + {@link #luminous()} + bare form + a BE present, else the vanilla default.
	 *
	 * <p>DECLARED DEVIATION (render side, not here): the upstream bare wire also flips a
	 * TEXTURE-fullbright flag {@code mState > 0} (WireRedstone :81-82 — RedAlloy/Signalum wires
	 * render their quads at constant brightness with the signal up). That is a lightmap-side
	 * quad property with no world-light counterpart; it needs ModelData/BEWLR (the P9 ADR red
	 * line) and is declared unimplemented (see GTWireBakedModel).
	 */
	@Override
	public int getLightEmission(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		if (mFamily == Family.REDSTONE && mLuminous && !mInsulated
				//? if forge {
				&& aLevel.getExistingBlockEntity(aPos) instanceof GTWireBlockEntity tWire) {
				//?} else {
				/*&& aLevel.getBlockEntity(aPos) instanceof GTWireBlockEntity tWire) {
				//21.1: the forge getExistingBlockEntity patch is gone — BlockGetter.getBlockEntity
				//is already a null-safe read here.
				*///?}
			return UT6.bind4(UT6.divup(tWire.mRedstone, GTWireSpecs.MAX_RANGE)); // upstream :53/:79
		}
		return super.getLightEmission(aState, aLevel, aPos);
	}

	/**
	 * The per-family BET (task p10): redstone rows resolve GTWires.WIRE_REDSTONE_BE (the 6
	 * redstone blocks), laser rows resolve GTWires.WIRE_LASER_BE (task
	 * p10-wire-laser-placeholder — the 1 laser block, the WIRE_REDSTONE_BE precedent:
	 * the valid-block list MUST carry every block that stamps the BET, the
	 * promotePendingBlockEntity lesson), electric rows keep
	 * GTBlockEntities.WIRE_ELECTRIC_BE (the p7 pair + the 620 family) — the same BE CLASS
	 * mounts all three, the family gate lives inside it. (Task p10-wire-laser-placeholder
	 * NARROW touch: this ternary line is the registration-required branch for the laser
	 * BET mount; everything else in this file is untouched.)
	 */
	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mFamily == Family.REDSTONE ? GTWires.WIRE_REDSTONE_BE.get()
				: mFamily == Family.LASER ? GTWires.WIRE_LASER_BE.get()
				: GTBlockEntities.WIRE_ELECTRIC_BE.get();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		aBuilder.add(CONNECTIONS);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
