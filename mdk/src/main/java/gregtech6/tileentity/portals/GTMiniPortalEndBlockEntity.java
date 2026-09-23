package gregtech6.tileentity.portals;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Portals;

/**
 * 1.20.1 counterpart of gregtech/tileentity/portals/MultiTileEntityMiniPortalEnd.java
 * (134 lines, Loader_MultiTileEntities.java:2004 id 32000) — the End↔Overworld
 * miniature portal, the ×128 distance factor with a 512 m tolerance (End.java:67-89),
 * activated by an Ender Eye (End.java:112-123 — right click consumes one unless the
 * player has infinite items).
 */
public class GTMiniPortalEndBlockEntity extends GTMiniPortalBlockEntity {

	/** Upstream End.java:42-44 — the static session pair tables (server-only). */
	public static final List<GTMiniPortalBlockEntity>
	sListEndSide   = new java.util.ArrayList<>(),
	sListWorldSide = new java.util.ArrayList<>();

	public GTMiniPortalEndBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6Portals.PORTAL_END_BE.get(), aPos, aState);
	}

	/** The fixture seam (the Bumbliary holder-array test form) — the production BET rides the public ctor. */
	protected GTMiniPortalEndBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override protected List<GTMiniPortalBlockEntity> getPortalListA() {return sListWorldSide;}
	@Override protected List<GTMiniPortalBlockEntity> getPortalListB() {return sListEndSide;}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.portal.end"; // upstream End.java:133
	}

	@Override
	public void findTargetPortal() {
		mTarget = null;
		Level tLevel = getLevel();
		if (tLevel == null || tLevel.isClientSide()) return;
		if (tLevel.dimension() == Level.OVERWORLD) {
			mTarget = nearestPortal(sListEndSide, getBlockPos(), 128, 512 * 512);
		} else if (tLevel.dimension() == Level.END) {
			mTarget = nearestPortal(sListWorldSide, getBlockPos(), 128, 512 * 512);
		}
	}

	@Override
	public void addThisPortalToLists() {
		Level tLevel = getLevel();
		if (tLevel == null || tLevel.isClientSide()) return;
		if (tLevel.dimension() == Level.OVERWORLD) {
			if (!sListWorldSide.contains(this)) sListWorldSide.add(this);
			for (GTMiniPortalBlockEntity tPortal : sListEndSide) tPortal.findTargetPortal(); // upstream :99
			findTargetPortal();
		} else if (tLevel.dimension() == Level.END) {
			if (!sListEndSide.contains(this)) sListEndSide.add(this);
			for (GTMiniPortalBlockEntity tPortal : sListWorldSide) tPortal.findTargetPortal(); // upstream :103
			findTargetPortal();
		} else {
			setPortalInactive();
		}
	}

	/**
	 * Upstream onBlockActivated2 :112-123 — the Ender Eye activation. The caller (the
	 * block's use, or /gt6portal eye) passes the interaction; returns true when the eye was
	 * consumed (the upstream {@code aStack.stackSize--} arm; the infinite-items check maps
	 * onto the creative flag, UT.Entities.hasInfiniteItems ↔ Player.getAbilities().instabuild).
	 * The target-coords chat readout (:117) rides the /gt6portal check command.
	 */
	public boolean activateWithEye(Player aPlayer, InteractionHand aHand) {
		if (getLevel() == null || getLevel().isClientSide()) return false;
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		if (tStack.is(Items.ENDER_EYE) && tStack.getCount() > 0) {
			setPortalActive();
			if (!aPlayer.getAbilities().instabuild) tStack.shrink(1); // upstream :118
			return true;
		}
		return false;
	}
}
