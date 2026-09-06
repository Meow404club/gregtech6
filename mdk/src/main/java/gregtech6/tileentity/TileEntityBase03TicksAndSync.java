package gregtech6.tileentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
 */
public abstract class TileEntityBase03TicksAndSync extends TileEntityBase01Root {

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
