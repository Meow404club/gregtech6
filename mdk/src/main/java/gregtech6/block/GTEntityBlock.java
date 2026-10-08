package gregtech6.block;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.IItemHandler;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * Base for all GT6 blocks carrying a BlockEntity — the 1.20.1 block-side counterpart
 * of the GT6 MultiTileEntity system. Wires EntityBlock.newBlockEntity (EntityBlock.java:15,
 * mandatory) and the ticker (EntityBlock.getTicker :18, default null = notick chain
 * equivalent) onto the upstream canUpdate() semantics
 * (TileEntityBase01Root.java:440 = mIsTicking &amp;&amp; mShouldRefresh): the ticker runs
 * the 03 dispatcher only while canUpdate() holds, evaluated on the live instance every
 * tick, so a machine can stop and resume ticking by flipping mShouldRefresh.
 *
 * <p>GTCEu Modern precedent for the direct lambda without createTickerHelper:
 * ManagedSyncEntityBlock.getTicker :19-28. One ticker serves both sides — the upstream
 * updateEntity ran on client and server alike and branches on isServerSide internally.
 */
public abstract class GTEntityBlock extends BaseEntityBlock {

	protected GTEntityBlock(Properties aProperties) {
		super(aProperties);
	}

	/**
	 * The BlockEntityType this block mounts (ADR-P3-1: multiple blocks may return the
	 * same shared BET — the GT6 "one TE class, many material blocks" counterpart).
	 */
	protected abstract BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType();

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		// BlockEntityType.create -> factory (BlockEntityType.java:288-290)
		return tickerType().create(aPos, aState);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
		// identity gate flattened from BaseEntityBlock.createTickerHelper (:38-42)
		if (aType != tickerType()) {
			return null;
		}
		return (aTickerLevel, aPos, aTickerState, aTile) -> {
			if (aTile instanceof TileEntityBase03TicksAndSync tTile && tTile.canUpdate() && !tTile.isRemoved()) {
				tTile.updateEntity();
			}
		};
	}

	/**
	 * The content-drop fallback (GitHub #19). Vanilla folds "drop the container contents"
	 * into each block's own onRemove (ChestBlock.onRemove :230-241 — the :235
	 * Containers.dropContents over the still-registered BE), and the default
	 * BlockBehaviour.onRemove (:146-151) only removes the BlockEntity — so the port's BE
	 * blocks, none of which carried the walk, scattered nothing when broken. Upstream 1.7.10
	 * made it a final breakBlock → TE.breakBlock() duty (MultiTileEntityBlock.java:144-152)
	 * that loops mInventory server-side under the per-slot canDrop gate
	 * (TileEntityBase05Inventories.java:152-171). This override is the single walk for every
	 * family, the GTCEu Modern one-point precedent (MetaMachineBlock.java:256-277): a real
	 * block swap (the {@code !oldState.is(newState)} vanilla convention, server side only)
	 * pops the BE's exposed inventory — the vanilla Container face first (the Bumbliary BE
	 * implements Container), else the raw {@code getInventory()} census via
	 * {@link #dropInventory}. Families that expose no inventory (the barrel rides its item
	 * NBT, the hive's playerDestroy owns its own walk) simply answer null and stay put.
	 */
	@Override
	public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
		if (!aOldState.is(aNewState.getBlock()) && !aLevel.isClientSide) {
			BlockEntity tBE = aLevel.getBlockEntity(aPos);
			if (tBE instanceof TileEntityBase03TicksAndSync tTile) {
				if (tTile instanceof Container tContainer) {
					Containers.dropContents(aLevel, aPos, tContainer); // the ChestBlock :235 face
				} else {
					IItemHandler tInventory = dropInventory(tTile);
					if (tInventory != null) {
						for (int i = 0, l = tInventory.getSlots(); i < l; i++) {
							ItemStack tStack = tInventory.getStackInSlot(i);
							if (!tStack.isEmpty() && canDrop(i) && beCanDrop(tTile, i)) {
								Block.popResource(aLevel, aPos, tStack);
							}
						}
					}
				}
			}
		}
		super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
	}

	/**
	 * The block-side per-slot drop gate — the upstream canDrop(int) semantic
	 * (TileEntityBase05Inventories.java:152-171 loop), default = every slot drops. Block
	 * subclasses override for whole-block rulings: the bottlecrate kind answers F so its
	 * contents ride the dropped BlockEntityTag item instead (the shulker convention, the
	 * upstream :244-245 {@code canDrop = F} fold).
	 */
	protected boolean canDrop(int aInventorySlot) {
		return true;
	}

	/**
	 * The BE-declared upstream gate, honoured when the family carries one:
	 * TileEntityAdvancedCraftingTable.canDrop :770 keeps the holo pair (31/32) with the
	 * block self-drop. Undeclared = all slots (the sole census entry — no other BE declares
	 * a canDrop(int) — so the probe cannot mis-gate a family).
	 *
	 * <p>The probe must be {@linkplain #probeDeclared declared-only}: a server-side
	 * {@code getMethod} miss falls back to enumerating every interface default method, and
	 * building those Method objects resolves their signatures — IUIHolder.createScreen
	 * (ModularUI fork api/IUIHolder.java:30-31) names ModularScreen
	 * (screen/ModularScreen.java:63, {@code @OnlyIn(Dist.CLIENT)}), so reflecting over any
	 * GT6MuiMachine implementor detonates on a dedicated server (FML RuntimeDistCleaner
	 * "invalid dist DEDICATED_SERVER" — the /fill teardown crash, qu_machines both legs).
	 */
	private static boolean beCanDrop(TileEntityBase03TicksAndSync aTile, int aSlot) {
		try {
			Method tMethod = probeDeclared(aTile.getClass(), "canDrop", int.class);
			return tMethod == null || (Boolean) tMethod.invoke(aTile, aSlot);
		} catch (ReflectiveOperationException aE) {
			return true;
		}
	}

	/**
	 * The class-hierarchy probe behind both reflective gates ({@link #beCanDrop},
	 * {@link #dropInventory}): walks {@code getDeclaredMethod} up the superclass chain —
	 * public members only (the {@code getMethod} visibility parity). It never enumerates
	 * interface default methods, so no implementor signature — the GT6MuiMachine
	 * {@code createScreen → ModularScreen} seam included — is ever resolved on the server
	 * path. The census: canDrop(int) lives only on TileEntityAdvancedCraftingTable :774,
	 * getInventory() on the machines/storages/hoppers/chest families (TestMachineBlockEntity
	 * :91 … TileEntityBase10MultiBlockMachine :883) — both found inside the GT hierarchy,
	 * never via an interface.
	 */
	static Method probeDeclared(Class<?> aClass, String aName, Class<?>... aParams) {
		for (Class<?> tClass = aClass; tClass != null && tClass != Object.class; tClass = tClass.getSuperclass()) {
			try {
				Method tMethod = tClass.getDeclaredMethod(aName, aParams);
				if (Modifier.isPublic(tMethod.getModifiers())) {
					return tMethod;
				}
			} catch (NoSuchMethodException aE) { /* keep walking up */ }
		}
		return null;
	}

	/**
	 * The raw inventory the fallback walks, or null when the family exposes none — "can get
	 * → drop, can't → skip" (the inventory accessors are not uniform across families). The
	 * default rides the {@code getInventory()} census (TestMachineBlockEntity:91 over the
	 * machines/storages/hoppers/chest families to TileEntityBase10MultiBlockMachine:883)
	 * reflectively; a block bridges a differently-spelled accessor by overriding this
	 * (GTAnvilBlock hands over the BE's {@code inventory()}). Blocks whose BE answers null
	 * are untouched by the fallback. The probe is {@linkplain #probeDeclared declared-only}
	 * — the same dist seam as {@link #beCanDrop} (a {@code getMethod} miss here would
	 * enumerate the MUI interface defaults and detonate on a dedicated server for any
	 * family lacking the accessor).
	 *
	 * <p>ponytail: a typed seam interface would mean editing every BE file for the same
	 * join; the public accessor IS the existing duck-typed contract, and this card may not
	 * touch the BE tree.
	 */
	@Nullable
	protected IItemHandler dropInventory(BlockEntity aTile) {
		try {
			Method tMethod = probeDeclared(aTile.getClass(), "getInventory");
			return tMethod == null ? null : (IItemHandler) tMethod.invoke(aTile);
		} catch (ReflectiveOperationException aE) {
			return null; // the family exposes no inventory — nothing to drop
		}
	}
}
