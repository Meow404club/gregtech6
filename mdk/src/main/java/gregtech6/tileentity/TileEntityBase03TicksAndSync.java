package gregtech6.tileentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.GT6Mod;

/**
 * 1.20.1 counterpart of gregapi/tileentity/base/TileEntityBase03TicksAndSync.java
 * (extends TileEntityBase02AdjacentTEBuffer, 166 lines).
 *
 * <p>Port scope:
 * <ul>
 * <li>the final tick dispatcher updateEntity (:111-141) with the eight onTick*
 *     phases (:144-165) verbatim, including the Throwable fallback with
 *     setError + onTickFailed (:130-139);</li>
 * <li>the self-built IPacket sync stack replaced by the vanilla two-channel sync:
 *     chunk data ({@link #getUpdateTag()} + IForgeBlockEntity.handleUpdateTag :68,
 *     default = load(tag)) and block updates ({@link #getUpdatePacket()} =
 *     ClientboundBlockEntityDataPacket over getUpdateTag, sent by ChunkHolder's
 *     blockChanged broadcast (:247); client lands in IForgeBlockEntity.onDataPacket
 *     :53, default = load(pkt.getTag())) — both channels converge on load(), so
 *     both are plain data producers with no packet classes of our own;</li>
 * <li>mOwner ownership routing (:40, :55-76, :106-108) and the mOwner-gated
 *     allowInteraction are dropped with the IPacket stack (vanilla channels
 *     broadcast to trackers; per-owner routing is a later feature need).</li>
 * </ul>
 *
 * <p>Omissions relative to upstream: the adjacency TileEntity buffer of the skipped
 * TileEntityBase02AdjacentTEBuffer layer (neighbor-cache surface, out of the minimal
 * face) — its tick-core semantics (mTimer counting and coordinate-change detection,
 * 02:41-49/:138-155) are folded in here so the dispatcher keeps upstream timing:
 * mTimer == 0 fires onTickFirst on the very first tick, the client sync gate opens
 * at mTimer &gt; 2 (upstream :123).
 *
 * <p>Paintable stratum (task p21-paintable-storage-sync): this class also carries the
 * upstream TileEntityBase07Paintable paint layer (the family-wide inheritance stratum,
 * upstream 07Paintable.java:49-52) folded into the 01-07 chain collapse point — the
 * {@link IPaintableTE} face ({@code paint/mixPaint/unpaint/isPainted/getPaint}, upstream
 * Paintable:83-86 + the 04:227-235 recolour routing), the {@code mRGBa}/{@code mIsPainted}
 * storage (direct 0xRRGGBB, ruling 3 of ADR 2026-09-07-p21-paintable-rulings), the NBT
 * keys {@code gt.color}/{@code gt.painted} (upstream CS.java:1161-1162, read from
 * readFromNBT2:57-58) and the {@code getModelData()} PAINT property supply.
 */
public abstract class TileEntityBase03TicksAndSync extends TileEntityBase01Root implements IPaintableTE {

	/** Variable for seeing if the Tick Function is called right now (upstream :43). */
	public boolean mIsRunningTick = false;

	/** Variable for updating Data to the Client (upstream :46, private there). */
	private boolean mSendClientData = false;

	/** Gets set to true when the Block received a Block Update (upstream :49; reset in onTickResetChecks :159). */
	public boolean mBlockUpdated = false;

	/** Timer Value (upstream TileEntityBase02AdjacentTEBuffer.java:41). */
	protected long mTimer = 0;

	/** Old Coordinates during the previous Tick (upstream TileEntityBase02AdjacentTEBuffer.java:43). */
	protected int oX = 0, oY = 0, oZ = 0;

	// ---------------------------------------------------------------------------
	// paint layer (upstream TileEntityBase07Paintable.java:49-52, task p21-paintable-storage-sync)
	// ---------------------------------------------------------------------------

	/** Upstream CS.UNCOLORED = 0x00FFFFFF (CS.java:327) — the unpainted colour, white = "no tint" (GTWireTint/GTMaterialPrefixBlock precedent). */
	public static final int UNCOLORED = 0xFFFFFF;

	/** Upstream NBT_COLOR = "gt.color" (CS.java:1161) — the key names are verbatim upstream (ADR ruling 4; no 1.7.10-world migration exists to preserve). */
	public static final String NBT_COLOR = "gt.color";

	/** Upstream NBT_PAINTED = "gt.painted" (CS.java:1162). */
	public static final String NBT_PAINTED = "gt.painted";

	/** The current paint colour, 0xRRGGBB (upstream :50 {@code mRGBa = UNCOLORED}; direct storage, ADR ruling 3). */
	protected int mRGBa = UNCOLORED;

	/** Upstream :49 {@code mIsPainted = F}. */
	protected boolean mIsPainted = false;

	/** Upstream Paintable:85 verbatim — direct store + painted flag; the same-colour spray is the no-op. */
	@Override
	public boolean paint(int aRGB) {
		if (aRGB != mRGBa) {
			mRGBa = aRGB;
			mIsPainted = true;
			markPaintChanged();
			return true;
		}
		return false;
	}

	/**
	 * The recolour routing of upstream TileEntityBase04MultiTileEntities.java:227-235,
	 * {@code (isPainted ? mixRGBInt(color, getPaint()) : color) & ALL_NON_ALPHA_COLOR},
	 * with the dye-index complement folded away: the upstream caller sprayed
	 * {@code ~mColor&15} through the DYES_INT_INVERTED table, whose composition is
	 * identical to {@code DYES_INT[mColor]} — the port takes the final colour directly
	 * (ADR ruling 3, declared equivalence simplification).
	 */
	@Override
	public boolean mixPaint(int aRGB) {
		return paint((isPainted() ? mixRGBInt(aRGB, getPaint()) : aRGB) & 0xFFFFFF);
	}

	/**
	 * Upstream Paintable:83 shape ({@code if (mIsPainted) {mIsPainted=F; mRGBa=<colour>; ...}}).
	 * Declared deviation: upstream restores {@code mMaterial.fRGBaSolid} — the port's
	 * machines carry no material field (trimmed set), so unpaint returns UNCOLORED white,
	 * which renders as "no tint" (ADR ruling, IPaintableTE doc).
	 */
	@Override
	public boolean unpaint() {
		if (mIsPainted) {
			mIsPainted = false;
			mRGBa = UNCOLORED;
			markPaintChanged();
			return true;
		}
		return false;
	}

	/**
	 * Upstream Paintable:84 server half verbatim. Declared deviation: the client-side
	 * material-colour inference ({@code worldObj != null && isClientSide() && materialColor != mRGBa})
	 * is cut with the {@code mMaterial} field — the client knows {@code mIsPainted}
	 * through {@code gt.painted} riding the vanilla two-channel sync instead.
	 */
	@Override
	public boolean isPainted() {
		return mIsPainted;
	}

	/** Upstream Paintable:86. */
	@Override
	public int getPaint() {
		return mRGBa;
	}

	/**
	 * The paint write-point triple (ADR ruling 4): {@code setChanged()} (the chunk-dirty
	 * flag — the cover-chain resurrect lesson) + {@link #sendClientData()} (upstream
	 * :161-165 sendBlockUpdated chain, the immediate broadcast of the two sync channels)
	 * + {@code requestModelDataUpdate()} (the {@code getModelData()} refresh; Forge
	 * IForgeBlockEntity:153 / Neo IBlockEntityExtension:76 — both default methods,
	 * client-side-only and self-guarding, so the server-side write-point call is a no-op).
	 */
	private void markPaintChanged() {
		setChanged();
		sendClientData();
		requestModelDataUpdate();
	}

	/** Upstream UT.Code.mixRGBInt (UT.java:1576-1578) verbatim — the per-channel average. */
	private static int mixRGBInt(int aRGB1, int aRGB2) {
		return ((getR(aRGB1) + getR(aRGB2)) >> 1) << 16 | ((getG(aRGB1) + getG(aRGB2)) >> 1) << 8 | ((getB(aRGB1) + getB(aRGB2)) >> 1); // UT.Code.getRGBInt shape :1580-1582
	}

	/** Upstream UT.Code.getR (UT.java:1600). */
	private static int getR(int aRGB) {
		return (aRGB >>> 16) & 255;
	}

	/** Upstream UT.Code.getG (UT.java:1601). */
	private static int getG(int aRGB) {
		return (aRGB >>> 8) & 255;
	}

	/** Upstream UT.Code.getB (UT.java:1602). */
	private static int getB(int aRGB) {
		return aRGB & 255;
	}

	protected TileEntityBase03TicksAndSync(boolean aIsTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aIsTicking, aType, aPos, aState);
	}

	/** Timer Value accessor (upstream TileEntityBase02AdjacentTEBuffer.getTimer :162-164; the 01Root ITileEntity form returned 0). */
	public long getTimer() {
		return mTimer;
	}

	/**
	 * The tick entry the EntityBlock ticker calls (upstream public final updateEntity,
	 * :111-141 — verbatim phase order and guards; {@code super.updateEntity()} :121 is the
	 * folded 01Root+02 tick core below; markDirty+WD.mark :116-117 is one 1.20.1
	 * {@code setChanged()}).
	 */
	public final void updateEntity() {
		mIsRunningTick = true;
		boolean tIsServerSide = isServerSide();
		try {
			if (mTimer == 0) {
				setChanged();
				onTickFirst(tIsServerSide);
			}
			if (!isDead()) onTickStart(mTimer, tIsServerSide);
			if (!isDead()) updateEntityBase();
			if (!isDead()) onTick(mTimer, tIsServerSide);
			if (!isDead() && tIsServerSide && mTimer > 2 && (mSendClientData || onTickCheck(mTimer))) {
				sendClientData();
				mSendClientData = false;
				onTickChecked(mTimer);
			}
			if (!isDead()) onTickResetChecks(mTimer, tIsServerSide);
			if (!isDead()) onTickEnd(mTimer, tIsServerSide);
		} catch (Throwable e1) {
			// upstream :130-138 verbatim
			GT6Mod.LOGGER.error("TileEntity tick threw", e1);
			setError((tIsServerSide ? "Serverside: " : "Clientside: ") + e1);
			try {
				onTickFailed(mTimer, tIsServerSide);
			} catch (Throwable e2) {
				GT6Mod.LOGGER.error("TileEntity onTickFailed threw", e2);
				setError((tIsServerSide ? "Serverside: " : "Clientside: ") + e2);
			}
		}
		mIsRunningTick = false;
	}

	/**
	 * The super.updateEntity() step (:121): the folded TileEntityBase01Root.updateEntity
	 * core (see {@link TileEntityBase01Root#updateEntityCore()}) plus the folded
	 * TileEntityBase02AdjacentTEBuffer tick core (:138-155): timer increment, first-tick
	 * coordinate snapshot and block-update arming (:150), coordinate-change detection.
	 */
	private void updateEntityBase() {
		updateEntityCore();
		if (!isDead()) {
			mTimer++;
			if (isServerSide()) {
				if (mTimer == 1) {
					oX = getBlockPos().getX();
					oY = getBlockPos().getY();
					oZ = getBlockPos().getZ();
					mDoesBlockUpdate = true; // upstream 02:150 — runs doBlockUpdate on the next tick
				}
				if (oX != getBlockPos().getX() || oY != getBlockPos().getY() || oZ != getBlockPos().getZ()) {
					onCoordinateChange();
					oX = getBlockPos().getX();
					oY = getBlockPos().getY();
					oZ = getBlockPos().getZ();
				}
			}
		}
	}

	// ---------------------------------------------------------------------------
	// sync layer (upstream IPacket stack -> vanilla two-channel sync)
	// ---------------------------------------------------------------------------

	/** Called to flag the next dispatcher sync window (upstream updateClientData :88). */
	public void updateClientData() {
		mSendClientData = true;
	}

	/** Marks the Block received a Block Update (upstream the mBlockUpdated writer; reset in onTickResetChecks :159). */
	public void markBlockUpdated() {
		mBlockUpdated = true;
	}

	/** Upstream :90 onCoordinateChange -> updateClientData; the adjacency-buffer clearing of the 1.7.10 super call is skipped with the buffer. */
	public void onCoordinateChange() {
		updateClientData();
	}

	/**
	 * Upstream sendClientData(boolean, EntityPlayerMP) (:55-76) with the hand-built IPacket
	 * routing replaced by the vanilla block-update channel: ServerLevel.sendBlockUpdated
	 * (:951) -> ServerChunkCache.blockChanged -> ChunkHolder broadcasts
	 * {@link #getUpdatePacket()} to tracking players (ChunkHolder.java:247), the client
	 * lands in IForgeBlockEntity.onDataPacket (:53) = load(updateTag).
	 */
	public void sendClientData() {
		if (hasLevel() && isServerSide()) {
			getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Chunk-data channel payload (vanilla BlockEntity.getUpdateTag :154): the full sync
	 * state, the content contract of the upstream getClientDataPacket(true) (:52). Client
	 * side arrives via IForgeBlockEntity.handleUpdateTag (:68, default = load(tag)).
	 */
	//? if forge {
	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}
	//?} else {
	/*@Override
	public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider aProvider) {
	//21.1: BlockEntity.getUpdateTag/saveWithoutMetadata take the registries (javap 21.1.249).
		return saveWithoutMetadata(aProvider);
	}
	*///?}

	// ---------------------------------------------------------------------------
	// paint NBT + ModelData supply (task p21-paintable-storage-sync)
	// ---------------------------------------------------------------------------

	/**
	 * The paint keys ride the persistence NBT (upstream readFromNBT2 :57-58 read /
	 * 04:120 write-back): {@code gt.color} (Integer) + {@code gt.painted} (Boolean),
	 * written only while painted so an unpainted BE carries no paint keys. Because
	 * {@link #getUpdateTag()} = {@code saveWithoutMetadata()}, both sync channels
	 * (chunk data + block update) carry the two keys for free — the client
	 * {@link #load} rehydrates them.
	 */
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mIsPainted) {
			aNBT.putInt(NBT_COLOR, mRGBa); // upstream CS.NBT_COLOR :1161
			aNBT.putBoolean(NBT_PAINTED, true); // upstream CS.NBT_PAINTED :1162
		}
	}

	/**
	 * The upstream readFromNBT2 :57-58 hasKey-guarded pair. The client arm: a paint
	 * change arriving through either sync channel refreshes the ModelDataManager
	 * (dirty-gated like the oven's mOvenVisualDirty — requestModelDataUpdate alone is
	 * client-side-only, IForgeBlockEntity:153 / IBlockEntityExtension:76).
	 */
	@Override
	public void load(CompoundTag aNBT) {
		boolean tWasPainted = mIsPainted;
		int tWasRGBa = mRGBa;
		super.load(aNBT);
		if (aNBT.contains(NBT_COLOR, Tag.TAG_ANY_NUMERIC)) mRGBa = aNBT.getInt(NBT_COLOR); // upstream :57
		if (aNBT.contains(NBT_PAINTED)) mIsPainted = aNBT.getBoolean(NBT_PAINTED); // upstream :58
		if (hasLevel() && isClientSide() && (mIsPainted != tWasPainted || mRGBa != tWasRGBa)) {
			requestModelDataUpdate();
		}
	}

	/**
	 * The PAINT supply (Forge IForgeBlockEntity:174 / Neo IBlockEntityExtension:96 — the
	 * dual-leg host-interface names differ, the member shape does not): unpainted = the
	 * super default ({@code ModelData.EMPTY} semantics — absent property, no tint);
	 * painted = the derived snapshot carrying {@link gregtech6.client.render.GTModelProperties#PAINT}
	 * with the immutable Integer colour (the ModelData iron law, GTModelProperties:21-28).
	 * Subclasses derive from THIS ({@code GTModelProperties.derive(super.getModelData())}),
	 * so the oven's OVEN_SNAPSHOT/RENDER_SNAPSHOT keys coexist with PAINT on the same
	 * snapshot (single-valued properties each on their own key).
	 */
	@Override
	public net.minecraftforge.client.model.data.ModelData getModelData() {
		if (!mIsPainted) return super.getModelData();
		return gregtech6.client.render.GTModelProperties.derive(super.getModelData())
				.with(gregtech6.client.render.GTModelProperties.PAINT, Integer.valueOf(mRGBa))
				.build();
	}

	//? if neoforge {
	/*// 21.1: the ItemStackHandler NBT face (serializeNBT/deserializeNBT) and the ItemStack
	//save/parse face take a HolderLookup.Provider. Subclasses reference NBT_ACCESS in their
	//forked save/load legs. The field stays FINAL: its value is a delegating Provider over the
	//AtomicReference below (P19 B' ruling — no bare non-final static rebind). The initial
	//delegate is the frozen builtin registry view, so offline tests, runData and every
	//no-server path keep the P18 behaviour byte for byte; the embedded listener
	//(ServerRegistryAccessBinder below) rebinds the delegate at ServerAboutToStart to the
	//server's composite RegistryAccess. That rebind closes the dynamic-registry gap: 1.21
	//enchantments are data-driven (WORLDGEN layer, BuiltInRegistries has no ENCHANTMENT
	//field), so over the frozen view every enchanted ItemStack degrades on the parse face
	//(RegistryFixedCodec misses the registry → ItemStack.parse → resultOrPartial →
	//parseOptional(EMPTY)) and throws on the save face (Neo DataComponentUtil
	//wrapEncodingExceptions rethrows). Timing (P19 research card, 1.21.1 decompile):
	//WorldLoader.load loads the WORLDGEN layer BEFORE the server ctor
	//(MinecraftServer.java:290 this.registries = WorldStem.registries()), and /reload only
	//replaces the RELOADABLE loot layer (ReloadableServerRegistries replaceFrom) — the
	//composite is stable for the server's lifetime. Chunk-load worker threads only consume
	//this face after worlds exist, i.e. after the AboutToStart rebind, so the
	//AtomicReference's volatile semantics are sufficient; a leftover view from a stopped
	//server stays a legal frozen snapshot.
	private static final java.util.concurrent.atomic.AtomicReference<net.minecraft.core.HolderLookup.Provider> NBT_ACCESS_DELEGATE =
			new java.util.concurrent.atomic.AtomicReference<>(
					net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));

	// Package-private seams (the GT6RecipeMaps.registerGenerationResetHook precedent): the
	// current delegate — the frozen builtin until the first server boot — and the rebind entry
	// the server listener and the offline tests share.
	static net.minecraft.core.HolderLookup.Provider nbtAccessDelegate() {
		return NBT_ACCESS_DELEGATE.get();
	}

	static void bindNbtAccess(net.minecraft.core.HolderLookup.Provider aProvider) {
		NBT_ACCESS_DELEGATE.set(aProvider);
	}

	// The tree-wide NBT view: forwards the two abstract Provider points to the current
	// delegate — the default createSerializationContext/asGetterLookup and the NeoForge
	// IHolderLookupProviderExtension helpers all funnel through lookup/listRegistries, so no
	// other override is needed.
	public static final net.minecraft.core.HolderLookup.Provider NBT_ACCESS = new net.minecraft.core.HolderLookup.Provider() {
		@Override
		public java.util.stream.Stream<net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>>> listRegistries() {
			return NBT_ACCESS_DELEGATE.get().listRegistries();
		}

		@Override
		public <T> java.util.Optional<net.minecraft.core.HolderLookup.RegistryLookup<T>> lookup(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> aKey) {
			return NBT_ACCESS_DELEGATE.get().lookup(aKey);
		}
	};

	// The embedded rebind listener (ADR-P3-4 self-contained @EventBusSubscriber, no bus
	// attribute — ServerAboutToStartEvent is a game-bus event routed by type, the
	// GT6CapabilityWiring precedent). Nested so the annotation scan's mod-construction
	// class-load binds only THIS nested class and never initializes the BE class (the p6
	// a9027ac lazy-capture lesson): the outer static init still first runs at the earliest
	// NBT_ACCESS touch, always after the registries exist.
	@net.neoforged.fml.common.EventBusSubscriber(modid = "gt6")
	public static final class ServerRegistryAccessBinder {
		@net.neoforged.bus.api.SubscribeEvent
		public static void onServerAboutToStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent aEvent) {
			bindNbtAccess(aEvent.getServer().registryAccess());
			GT6Mod.LOGGER.debug("NBT_ACCESS rebound to the server composite RegistryAccess");
		}
	}
	*///?}

	/**
	 * Block-update channel (vanilla BlockEntity.getUpdatePacket :150): vanilla
	 * ClientboundBlockEntityDataPacket.create(be) (:23-25) packs getUpdateTag(); the client
	 * default onDataPacket (:53) loads the tag. Replaces the upstream
	 * getDescriptionPacket final-null (:92) + IPacket stack.
	 */
	@Override
	@Nullable
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	// ---------------------------------------------------------------------------
	// the eight onTick phases (upstream :143-165 verbatim)
	// ---------------------------------------------------------------------------

	/** The very first Tick happening to this TileEntity (upstream :143-144). */
	public void onTickFirst(boolean aIsServerSide) {/**/}

	/** The first Part of the Tick (upstream :146-147). */
	public void onTickStart(long aTimer, boolean aIsServerSide) {/**/}

	/** The regular Tick (upstream :149-150). */
	public void onTick(long aTimer, boolean aIsServerSide) {/**/}

	/** Use this to check if it is required to send an update to the Clients (upstream :152-153). */
	public boolean onTickCheck(long aTimer) {return false;}

	/** Called when onTickCheck returns true. A super Call is important for this one! (upstream :155-156) */
	public void onTickChecked(long aTimer) {/**/}

	/** Used to reset all Variables which have something to do with the detection of Changes. A super Call is important for this one! (upstream :158-159) */
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {mBlockUpdated = false;}

	/** The absolutely last Part of the Tick (upstream :161-162). */
	public void onTickEnd(long aTimer, boolean aIsServerSide) {/**/}

	/** Gets called when there is an Exception happening during one of the Tick Functions (upstream :164-165). */
	public void onTickFailed(long aTimer, boolean aIsServerSide) {/**/}
}
