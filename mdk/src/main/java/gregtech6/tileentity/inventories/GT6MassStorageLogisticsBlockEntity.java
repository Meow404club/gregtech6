package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.tileentity.logistics.ITileEntityLogisticsStorage;

/**
 * The logistics mass storage — 1.20.1 port of
 * {@code gregtech/tileentity/inventories/MultiTileEntityMassStorageLogistics.java} (159
 * lines): the base mass storage plus the {@link ITileEntityLogisticsStorage} endpoint
 * face (upstream :38) — the priority/filter answers ride the base (:501-505, the
 * semi-filtered item tier 2/1), the digit strip renders CYAN (the {@code digitARGB}
 * override; the RED face is the shared full state), and the block art is the black-body
 * logistics set (the loader row's NBT_MATERIAL = MT.Black, Loader_MultiTileEntities
 * .java:142). The upstream logistics-network join lands through the port's live
 * logistics domain (logistics-lv3): the core BFS walks
 * {@link ITileEntityLogisticsStorage} implementors through
 * {@code ITileEntityLogistics#canLogistics} sides.
 */
public class GT6MassStorageLogisticsBlockEntity extends GT6MassStorageBlockEntity implements ITileEntityLogisticsStorage {

	public GT6MassStorageLogisticsBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	/** The BET constructor (the vanilla two-arg face). */
	public GT6MassStorageLogisticsBlockEntity(BlockPos aPos, BlockState aState) {
		super(aPos, aState);
	}

	/** The BET registry path (the getTileEntityName convention). */
	@Override
	public String getTileEntityName() {
		return "mass_storage_logistics";
	}

	@Override
	public int digitARGB() {
		return 0xFF00FFFF; // cyan (upstream :126 CA_CYAN_255)
	}
}
