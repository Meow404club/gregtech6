package gregtech6.tileentity.portals;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Portals;

/**
 * 1.20.1 counterpart of gregtech/tileentity/portals/MultiTileEntityMiniPortalNether.java
 * (138 lines, Loader_MultiTileEntities.java:2003 id 32766) — the Nether↔Overworld
 * miniature portal, the ×8 distance factor with a 128 m tolerance (Nether.java:71-80,
 * Y-proximity tie-break :78), the whole opposite table rescanned when either side gains
 * a portal (:103/:107). Dimension checks ride the level's ResourceKey
 * ({@code dimensionId == DIM_OVERWORLD/DIM_NETHER} ↔
 * {@code level.dimension() == Level.OVERWORLD/NETHER}).
 */
public class GTMiniPortalNetherBlockEntity extends GTMiniPortalBlockEntity {

	/** Upstream Nether.java:42-44 — the static session pair tables (server-only; the GT6Portals.ServerLifecycleLists listener clears them on server start/stop). */
	public static final List<GTMiniPortalBlockEntity>
	sListNetherSide = new java.util.ArrayList<>(),
	sListWorldSide  = new java.util.ArrayList<>();

	public GTMiniPortalNetherBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6Portals.PORTAL_NETHER_BE.get(), aPos, aState);
	}

	/** The fixture seam (the Bumbliary holder-array test form) — the production BET rides the public ctor. */
	protected GTMiniPortalNetherBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override protected List<GTMiniPortalBlockEntity> getPortalListA() {return sListWorldSide;}
	@Override protected List<GTMiniPortalBlockEntity> getPortalListB() {return sListNetherSide;}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.portal.nether"; // upstream Nether.java:137
	}

	// ---------------------------------------------------------------------------
	// pairing (upstream findTargetPortal :67-96 — the coordinate-arithmetic nearest
	// neighbour over the opposite table; the two dimension branches are mirror images)
	// ---------------------------------------------------------------------------

	@Override
	public void findTargetPortal() {
		mTarget = null;
		Level tLevel = getLevel();
		if (tLevel == null || tLevel.isClientSide()) return; // upstream :69 isServerSide guard
		if (tLevel.dimension() == Level.OVERWORLD) {
			mTarget = nearestPortal(sListNetherSide, getBlockPos(), 8, 128 * 128);
		} else if (tLevel.dimension() == Level.NETHER) {
			mTarget = nearestPortal(sListWorldSide, getBlockPos(), 8, 128 * 128);
		}
	}

	// ---------------------------------------------------------------------------
	// registration (upstream addThisPortalToLists :99-113 — join the own-dimension
	// table, rescan the whole opposite table, then self-scan; wrong dimension deactivates)
	// ---------------------------------------------------------------------------

	@Override
	public void addThisPortalToLists() {
		Level tLevel = getLevel();
		if (tLevel == null || tLevel.isClientSide()) return;
		if (tLevel.dimension() == Level.OVERWORLD) {
			if (!sListWorldSide.contains(this)) sListWorldSide.add(this);
			for (GTMiniPortalBlockEntity tPortal : sListNetherSide) tPortal.findTargetPortal(); // upstream :103
			findTargetPortal();
		} else if (tLevel.dimension() == Level.NETHER) {
			if (!sListNetherSide.contains(this)) sListNetherSide.add(this);
			for (GTMiniPortalBlockEntity tPortal : sListWorldSide) tPortal.findTargetPortal(); // upstream :107
			findTargetPortal();
		} else {
			setPortalInactive();
		}
	}

	// ---------------------------------------------------------------------------
	// activation (upstream onToolClick :116-128 — TOOL_igniter toggles, TOOL_extinguisher
	// douses; the modern arm lives on GTMiniPortalBlock.use (the flint-and-steel/tinder
	// item check) and /gt6portal (the RCON stand-in, the /gt6machine ignite precedent))
	// ---------------------------------------------------------------------------

	/** The upstream :119 igniter toggle — both arms. */
	public void igniteToggle() {
		if (mActive) setPortalInactive(); else setPortalActive();
	}

	// the client-ambient half (upstream IMTE_RandomDisplayTick :62-64) is cut with the
	// render face; the ACTIVE blockstate cube swap is the visual carrier (declared).
}
